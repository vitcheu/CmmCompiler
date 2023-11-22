package Semantic_Analysis;

import AST.AST;
import AST.Callee;
import AST.DEFINE.*;
import AST.EXPR.*;
import AST.Node;
import AST.STMT.*;
import AST.TYPE.BaseType;
import AST.TYPE.ComposedType;
import AST.TYPE.TypeNode;
import Parser.Entity.Location;
import IR.*;
import CompileException.*;
import IR.Constants.OP;
import IR.Constants.Type;
import IR.instruction.CallInstruction;
import IR.instruction.ConditionJump;
import IR.instruction.Instruction;
import IR.instruction.ParamInstruction;
import Semantic_Analysis.SymbolTable.LocalTable;
import Semantic_Analysis.SymbolTable.SymbolTable;
import utils.CalculateTypeWidth;

import java.io.PrintWriter;
import java.util.*;

import static IR.Constants.OP.*;
import static IR.Constants.Type.*;

public class IRGenerator extends Visitor {
    private final PrintWriter pw;
    private final errorHandler handler;
    public static  HashMap<String, definedUserType> typeTable;
    private SymbolTable curTable;
    private final LinkedList<Instruction> IRs;
    private final List<Block> blocks = new LinkedList<>();
    //当前指令号
    private int curInstrId = 0;
    private Label curRetLabel;

    //指示代码生成器是否应该立即翻译此中间语句
    private boolean translateImmediately=true;
    private final LinkedList<Instruction> storeInstrOfCall=new LinkedList<>();

    //表示当前翻译的表达式是否处于if,while等控制流
    private boolean InControlFlow = false;

    //判断具有副作用的表达式处于单独的语句中还是作为更大的表达式的一部分
    private boolean InStatement = false;
    //表示当前翻译的是左值还是右值,true表示左值,false表示右值
    private boolean LeftSide = false;
    private boolean needAddress=false;
    //判断是否有return,break,continue等跳转语句
    private boolean hasJumpStmt=false;
    private ExprNode needBoolValue;
    //记录break语句的跳转标签
    private final LinkedList<Label> breakStack = new LinkedList<>();
    //记录continue语句的跳转标签
    private final LinkedList<Label> continueStack = new LinkedList<>();
    //跳转标签与指令号的映射关系
    private final HashMap<Label, Integer> labelMap = new HashMap<>();
    /*记录某位置上的标签集*/
    private final HashMap<Integer, List<Label>> posMap = new HashMap<>();
    /*记录跳转到某标签的指令集*/
    private final HashMap<Label,List<Instruction>> jumpMap=new HashMap<>();

    private static final HashMap<String, OP> opMap = new HashMap<>();

    static {
        opMap.put("+", add);
        opMap.put("-", sub);
        opMap.put("*", mul);
        opMap.put("/", div);
        opMap.put("%", mod);

        opMap.put(">>", shift_right);
        opMap.put("<<", shift_left);

        opMap.put(">=", ge);
        opMap.put("<=", le);
        opMap.put("!=", ne);
        opMap.put("==", eq);
        opMap.put(">", gt);
        opMap.put("<", lt);

        opMap.put("&&",and);
        opMap.put("||",or);

        opMap.put("~", neg);
    }

    public IRGenerator(errorHandler handler, PrintWriter pw) {
        this.pw = pw;
        this.handler = handler;
        IRs = new LinkedList<>();
    }

    public LinkedList<Instruction> getIRs() {
        return IRs;
    }

    private void generate(Node node) throws CompileError {
        node.accept(this);
//        pw.println("\n*******************");
//        pw.println("visiting:"+node);
    }

    private void generate(List<Node> nodes)throws CompileError {
        for(Node node:nodes){
            node.accept(this);
        }
    }

    public void generate(AST ast) {
        List<DefinedVariable> globalAndStaticVars=ast.getGlobalTable().getGlobalAndStaticVars(true);
        for(DefinedVariable var:globalAndStaticVars){
            try {
               generate(var);
            } catch (CompileError e) {
                throw new RuntimeException(e);
            }
        }

        for (DefinedFunction fun : ast.getFunctions()) {
            if(!fun.isDefined())
                continue;
            try {
                curRetLabel=genLabel();
                genEnter(fun.getBody().getLocalTable(),fun.getLocation());
                if(fun.getBody()!=null){
                    visit(fun.getBody());
                }
                bindLabel(curRetLabel);
                if(fun.getType().getBase()==BaseType.voidType){
                    gen(ret,null,null,null,null);
                }
            } catch (CompileError e) {
                pw.println("\n------------------------------------");
                pw.println("Caught in Visiting function body...");
                e.printStackTrace(pw);
            }
        }
        genExit();
        splitIntoBlocks();
    }

    /**
     * 表达式的中间代码
     */
    public void visit(BinaryOpNode node) throws CompileError {
        String op = node.getOp();
        ExprNode left = node.getLeft(),
                right = node.getRight();
        //创建储存表达式值的临时地址
        Temp t;
        switch (op) {
            case "+":
            case "-":
            case "*":
            case "/":
            case "%":

            case "<<":
            case ">>":

            //比较运算的直接赋值实现
            case ">=":
            case "<=":
            case "==":
            case "!=":
            case ">":
            case "<":{
                super.visit(node);
                Type resultType;
                OP OP = opMap.get(op);
                Value arg1 = left.getValue(),
                        arg2 = right.getValue();
                TypeNode lt = left.getType(), rt = right.getType(),
                        nodeType=node.getType();

                if(nodeType==null){
                    throw new RuntimeException("位置:"+node.getLocation()+","+node);
                }
                if(nodeType.isPointerOrArr())
                    resultType=pointer;
                else{
                    resultType=Type.TypeNodeToIRType(nodeType);
                }
                t = new Temp(resultType);

                Literal intLxr;
                //指针与整数加法,需要将整数乘以类型宽度
                if ((op.equals("+")||op.equals("-"))
                        && (lt.isPointerType() && rt.isIntCompatible()
                        || rt.isPointerType() && lt.isIntCompatible())) {
                    Temp t1 = new Temp(int32),t2;
                    if (lt.isPointerType()) {
                        //获取指针所指向的类型的宽度
                        int pWidth= CalculateTypeWidth.getTypeWidth(lt.getPointerBaseType());
                        if(arg2 instanceof Literal){
                            int i2= Integer.parseInt((((Literal)arg2).getLxrValue()));
                            int i3=i2*pWidth;
                            intLxr=new Literal(i3);
                            arg2=intLxr;
                        }else{
                            intLxr = new Literal(pWidth);
                            gen(mul, arg2, intLxr, t1, node.getLocation());
                            t2=new Temp(pointer);
                            gen(cast,t1,pointer,t2,node.getLocation());
                            //加法的操作数重定向
                            arg2 = t2;
                        }
                    } else {
                        int pWidth = CalculateTypeWidth.getTypeWidth(rt.getPointerBaseType());
                        if (arg1 instanceof Literal) {
                            int i1 = Integer.parseInt((((Literal) arg1).getLxrValue()));
                            int i3 = i1 * pWidth;
                            intLxr = new Literal(i3);
                            arg1 = intLxr;
                        } else {
                            intLxr = new Literal(pWidth);
                            gen(mul, arg1, intLxr, t1, node.getLocation());
                            t2 = new Temp(pointer);
                            gen(cast, t1, pointer, t2, node.getLocation());
                            arg1 = t2;
                        }
                    }
                }

                gen(OP, arg1, arg2, t, node.getLocation());

                //指针相减,需将结果除以类型宽度
                if (op.equals("-") && lt.isPointerType() && rt.isPointerType()) {
                    intLxr = new Literal(CalculateTypeWidth.getTypeWidth(lt.getPointerBaseType()));
                    Temp t2 = new Temp(int64);
                    gen(div, t, intLxr, t2, node.getLocation());
                    //结果重定向
                    t = t2;
                }
                setValue(node, t);
                break;
            }

            //比较运算的间接赋值实现
//            case ">=":
//            case "<=":
//            case "==":
//            case "!=":
//            case ">":
//            case "<":{
//                Label fLbl,nextLbl=genLabel();
//                t=new Temp();
//                fLbl=(node.getFalseLabel()==null)?genLabel():node.getFalseLabel();
//                node.setFalseLabel(fLbl);
//                node.setTrueLabel(Label.Fall);
//                //生成跳转语句
//                genBaseJump(node);
//                //生成赋值语句
//                gen(assign,Literal.TRUE,null,t,node.getLocation());
//                gen(jump,null,null,nextLbl,node.getLocation());
//                bindLabel(fLbl);
//                gen(assign,Literal.FALSE,null,t,node.getLocation());
//                setValue(node,t);
//                bindLabel(nextLbl);
//                break;
//            }

            case "&&":
            case "||":{
                if (!InControlFlow && needBoolValue == null) {
                    needBoolValue = node;
                }

                Label fLbl, tLbl;
                if (InControlFlow) {
                    fLbl = node.getFalseLabel();
                    tLbl = node.getTrueLabel();
                } else {
                    fLbl = (node.getFalseLabel() == null) ? genLabel() : node.getFalseLabel();
                    tLbl = (node.getTrueLabel() == null) ? Label.Fall : node.getTrueLabel();
                }
                pw.println("\n^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
                pw.println("InFlow=" + InControlFlow);
                pw.println("node=" + node + node.getLocation());
                pw.println("node.true=" + node.getTrueLabel());
                pw.println("node.false=" + node.getFalseLabel());

                //设定子节点的跳转目标
                if (op.equals("&&")) {
                    left.setTrueLabel(Label.Fall);
                    left.setFalseLabel((fLbl == Label.Fall) ? genLabel() : fLbl);
                } else {
                    left.setTrueLabel((tLbl == Label.Fall) ? genLabel() : tLbl);
                    left.setFalseLabel(Label.Fall);
                }
                right.setTrueLabel(tLbl);
                right.setFalseLabel(fLbl);

                //翻译左右子表达式
                generateBoolExpr(left);
                generateBoolExpr(right);

                //放置标签
                if (op.equals("&&") && fLbl == Label.Fall) {
                    bindLabel(left.getFalseLabel());
                }
                if (op.equals("||") && tLbl == Label.Fall) {
                    bindLabel(left.getTrueLabel());
                }

                //需要求本节点的布尔值
                if (needBoolValue == node) {
                    genBoolValue(node, fLbl);
                }

                //表示本层布尔表达式翻译完毕
                InControlFlow = false;
                pw.println("\n^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
                pw.println("翻译表达式"+node+"完毕");
                break;
            }
        }

        //设置地址
    }


    /**
     * 以控制流的形式翻译布尔表达式
     */
    public void generateBoolExpr(ExprNode node) throws CompileError {
        boolean flag = InControlFlow;
        //当且仅当此节点为布尔运算时,以控制流形式翻译语句
        InControlFlow =
                ((node.isBoolExpr()
                        ||(node instanceof BinaryOpNode binaryOpNode)&&(binaryOpNode.isRelOP())));
//        pw.println("\n--------------------------------------------------");
//        pw.println("以控制流形式翻译" + node + ",InControlFlow=" + InControlFlow);
        if (!node.isBoolExpr()) {
            pw.println("\n           翻译基本语句" + node);
            genBaseJump(node);
            pw.println("\n           翻译基本语句"+node+"完毕");
        } else {
            generate(node);
        }
        InControlFlow = flag;
    }

    /**
     * 根据布尔表达式的控制流跳转方向设置布尔表达式的值
     */
    public void genBoolValue(ExprNode node, Label falseLabel) {
        Temp t = new Temp(boolType);
        Label L2 = genLabel();
        //t=true
        gen(assign, Literal.TRUE, null, t, node.getLocation());
        //jump L2
        gen(jump, null, null, L2, node.getLocation());
        //L1:t=false
        bindLabel(falseLabel);
        gen(assign, Literal.FALSE, null, t, node.getLocation());
        //L2:
        bindLabel(L2);

        setValue(node, t);

        falseLabel.setBlockEdge(false);
        L2.setBlockEdge(false);

        needBoolValue = null;
    }

    /**
     * 将基本布尔表达式翻译成基本跳转语句
     */
    private void genBaseJump(ExprNode node) throws CompileError {
        //判断是否为比较语句,是则直接生成条件跳转语句
        //否则根据表达式的值进行跳转
        boolean isRelOp = (node instanceof BinaryOpNode) && (((BinaryOpNode) node).isRelOP());
        pw.println("isRelOp=" + isRelOp + ",node=" + node + node.getLocation());

        Label fLbl = node.getFalseLabel(),
                tLbl = node.getTrueLabel();

        if (isRelOp) {
            //根据布尔表达式的值间接地赋值
            //翻译左右子表达式
            if (InControlFlow) {
                BinaryOpNode relNode = (BinaryOpNode) node;
                super.visit(relNode);
                OP op =opMap.get(relNode.getOp());
                Value lv = relNode.getLeft().getValue(),
                        rv = relNode.getRight().getValue();
                if (lv == null || rv == null) {
                    throw new RuntimeException("翻译" + node + "的子表达式失败: lv=" + lv + ",rv=" + rv);
                }

                if (tLbl != Label.Fall && fLbl != Label.Fall) {
                    genConditionJump(if_jump, op, lv, rv, tLbl, node.getLocation());
                    gen(jump, null, null, fLbl, node.getLocation());
                } else if (tLbl != Label.Fall) {
                    genConditionJump(if_jump, op, lv, rv, tLbl, node.getLocation());

                } else if (fLbl != Label.Fall) {
                    genConditionJump(ifFalse_jump, op, lv, rv, fLbl, node.getLocation());
                }
            }else{
                //直接比较并设置值
                generate(node);
                Value boolvalue=node.getValue();
                genConditionJump(boolvalue,tLbl,fLbl,node.getLocation());
            }
        } else {
            generate(node);
            Value boolvalue = node.getValue();
            genConditionJump(boolvalue,tLbl,fLbl,node.getLocation());
        }
    }

    public void genConditionJump(Value boolvalue,Label tLbl,Label fLbl,Location location){
        OP op;
        if(boolvalue instanceof Literal  literal){
            boolean isZero=literal.isZero();
            Label des;
            if (tLbl != Label.Fall && fLbl != Label.Fall) {
                 des=isZero?fLbl:tLbl;
                 gen(jump,null,null,des,location);
            } else if (tLbl != Label.Fall) {
                 if(!isZero){
                     des=tLbl;
                     gen(jump,null,null,fLbl,location);
                 }
            } else if (fLbl != Label.Fall) {
                if(isZero){
                    des=fLbl;
                    gen(jump,null,null,fLbl,location);
                }
            }
        }else {
            if (tLbl != Label.Fall && fLbl != Label.Fall) {
                gen(if_jump, boolvalue, null, tLbl, location);
                gen(jump, null, null, fLbl, location);
            } else if (tLbl != Label.Fall) {
                gen(if_jump, boolvalue, null, tLbl, location);
            } else if (fLbl != Label.Fall) {
                gen(ifFalse_jump, boolvalue, null, fLbl, location);
            }
        }
    }


    public void visit(VariableNode node) {
        if (node.getEntry() == null) {
            throw new RuntimeException("无法找到变量" + node + "的符号表项,位置:"+node.getLocation());
        }
        //变量表达式的值地址就是变量对应的符号表项的引用
        node.setValue(node.getEntry());
    }

    public void visit(CallNode node) throws CompileError {
        ExprNode function = node.getExpr();
        ArgNode argumentNode = node.getArgNode();
        List<ExprNode> args = argumentNode.getArgs();
        List<Value> argValues=new LinkedList<>();

        //从右到左传参
        for (int i =args.size()-1; i>=0; i--) {
            //更改翻译模式
            translateImmediately=false;

            boolean flag=InStatement;
            InStatement=false;
            int oriNum=storeInstrOfCall.size();
            ExprNode exp = args.get(i);
            generate(exp);
            Value p = exp.getValue();
            InStatement=flag;

            translateImmediately=true;
            ParamInstruction paramInstr= genParam(p,exp.getLocation());
            while (storeInstrOfCall.size()>oriNum){
                paramInstr.addInstr(storeInstrOfCall.removeLast());
            }
            argValues.add(p);
        }

        Literal n = new Literal(args.size());
        generate(function);
        Value functionValue = function.getValue();
        TypeNode resultType=function.getType();

        Callee callee;
        if (functionValue instanceof DefinedFunction) {
            callee=(DefinedFunction)functionValue;
        } else {
            DerefrenceNode derefrenceNode=(DerefrenceNode) function;
            callee=derefrenceNode.getExpr().getType();
        }

        if(!resultType.isVoid()){
            Temp t=new Temp(function.getType());
            //重置翻译模式
            translateImmediately=true;
            genCall( functionValue,n,t,callee,argValues,node.getLocation());
            node.setValue(t);
        }else{
            //重置翻译模式
            translateImmediately=true;
            genCall(functionValue,n,null,callee,argValues,node.getLocation());
        }
    }

    public void visit(ConditionNode node) throws CompileError {
        boolean flag = InControlFlow;
        InControlFlow = true;
        Label L1 = genLabel(), L2 = genLabel();
        L1.setBlockEdge(false);
        L2.setBlockEdge(false);
        ExprNode condition = node.getCondition(),
                trueExp = node.getExpr1(),
                falseExp = node.getExpr2();
        Temp t = new Temp(trueExp.getType());
        //设置跳转目标
        condition.setTrueLabel(Label.Fall);
        condition.setFalseLabel(L1);
        //生成布尔表达式
        generateBoolExpr(condition);
        //恢复InControl标志
        InControlFlow = flag;
        //生成定值语句
        generate(trueExp);
        gen(assign, trueExp.getValue(), null, t, trueExp.getLocation());
        gen(jump, null, null, L2, trueExp.getLocation());

        bindLabel(condition.getFalseLabel());
        generate(falseExp);
        gen(assign, falseExp.getValue(), null, t, falseExp.getLocation());
        bindLabel(L2);
        //设置值
        node.setValue(t);
    }

    public void visit(MemberNode node) throws CompileError {
        ExprNode baseAddr = node.getExpr();
        String name = node.getName();

        //获取复合类型节点
        ComposedType entityType = (node.getOp().equals(".")) ? (ComposedType) baseAddr.getType().getBase()
                : (ComposedType) baseAddr.getType().getPointerBaseType().getBase();
        //获取复合类型的定义节点
        definedComposedType defType = entityType.getTypeEntry();
        if (defType == null) {
            throw new RuntimeException("无法找到" + entityType + "的类型定义");
        }
        pw.println("\n\n--------------------------\n" +
                "memberNode");
        pw.println("entity=" + baseAddr + ",name=" + name + ",op=" + node.getOp());
        pw.println("entityTYpe=" + entityType);
        pw.println("defType=" + defType);

        //计算变量所在的内存基地址
        Boolean flag = LeftSide;
        LeftSide = false;
        generate(baseAddr);
        LeftSide = flag;

        //寻找对应的字段
        SlotNode member=defType.getMember(name);
        int offset=defType.getOffsetOfMember(member);

        generateMemberRef(baseAddr,offset,node.getOp().equals("."),member.getType(),node);
    }

    /**
     * 创建成员访问指令
     * @return 表示成员访问结果的变量
     */
    public Var generateMemberRef(Node baseAddr,int memberOffset,boolean dotAccess,
                                  TypeNode derefType,ExprNode memberNode){
        Value base=null;
        Temp memberAddr = new Temp(pointer);
        Literal offsetLxr = new Literal(memberOffset);

        if(baseAddr instanceof DefinedVariable){
            base=(DefinedVariable) baseAddr;
        }else if(baseAddr instanceof ExprNode){
            base=((ExprNode)baseAddr).getValue();
        }

        /*统一转换为数组访问*/
        Location location=(memberNode==null)?baseAddr.getLocation(): memberNode.getLocation();
        Temp result=new Temp(derefType);
        setArrayRefResult(base,offsetLxr,derefType,result,location);

        //设置成员访问节点的值
        if(memberNode !=null){
            setValue(memberNode,result);
        }

        return result;
    }


    public void visit(sizeOfTypeNode node) {
        TypeNode type = node.getTypeNode();
        int width = CalculateTypeWidth.getTypeWidth(type);
        Literal lxr = new Literal(width);
        node.setValue(lxr);
    }

    public void visit(sizeOfExprNode node) {
        TypeNode type = node.getType();
        int width = CalculateTypeWidth.getTypeWidth(type);
        Literal lxr = new Literal(width);
        node.setValue(lxr);
    }

    /**
     * 具有副作用的表达式翻译
     * 包括赋值,后置自增/自减
     */
    public void visit(AssignNode node) throws CompileError {
        boolean flag = InStatement, f2 = LeftSide;
        InStatement = false;
        LeftSide=false;
        generate(node.getRightHandSideExpr());
        LeftSide = true;
        generate(node.getLhs());

        LeftSide = f2;
        InStatement = flag;

        Value left = node.getLhs().getValue(),
                right = node.getRightHandSideExpr().getValue();

        //在单独的语句中,无副作用
        if (InStatement) {
            gen(assign, right, null, (Var) left, node.getLocation());
            node.setValue(left);
        } else {
            Temp t = new Temp(right.getIRType());
            //t=rexp;
            gen(assign, right, null, t, node.getLocation());
            //lexp=t;
            gen(assign, t, null, (Var) left, node.getLocation());
            //node.value=t;
            node.setValue(t);
        }
    }

    public void visit(OpAssignNode node) throws CompileError {

        BinaryOpNode bin = node.toBinaryOpNode();
        pw.println("\n*******************************");
        pw.println("bin=" + bin + ",left=" + bin.getLeft() + ",right=" + bin.getRight());
        //以二元运算的形式得到右侧表达
        generate(bin);
        //翻译左侧
        Boolean flag = LeftSide;
        LeftSide = true;
        generate(node.getLhs());
        LeftSide = flag;
        Value v = bin.getValue(), lv = node.getLhs().getValue();
        //赋值给左侧
        gen(assign, v, null, (Var) lv, node.getLocation());
        node.setValue(v);
    }

    public void visit(PostfixOpNode node) throws CompileError {
        String op = node.getOp();
        ExprNode expr = node.getExpr();
        //翻译表达式
        boolean flag = InStatement;
        InStatement = false;
        generate(expr);
        InStatement = flag;

        Literal lxr;
        Value v = expr.getValue();

        int n;
        if (expr.getType().isPointerType()) {
            TypeNode base = expr.getType().getPointerBaseType();
            n = CalculateTypeWidth.getTypeWidth(base);
        } else
            n = 1;

        lxr = new Literal(n);
        OP op1 = op.equals("++") ? add : sub;

        //不在单独的语句中,其原值需要作为计算的一部分被使用
        if (!InStatement) {
            //保存原来的值
            //t=v;
            Temp t = new Temp(v.getIRType()),t2=new Temp(v.getIRType());
            gen(assign, v, null, t, node.getLocation());
            //v=v+1;
            gen(op1, v, lxr, t2, node.getLocation());
            gen(assign,t2,null,(Var)v,node.getLocation());
            node.setValue(t);
        } else {
            Temp t=new Temp(v.getIRType());
            gen(op1,v,lxr,t,node.getLocation());
            gen(assign, t, null,(Var) v, node.getLocation());
            node.setValue(v);
        }
    }

    public void visit(PrefixOpNode node) throws CompileError {
        String op = node.getOp();
        ExprNode expr = node.getExpr();
        generate(expr);
        Literal lxr;
        Value v = expr.getValue();
        Temp newValue = new Temp(expr.getType());

        int n;
        if (expr.getType().isPointerType()) {
            TypeNode base = expr.getType().getPointerBaseType();
            n = CalculateTypeWidth.getTypeWidth(base);
        } else
            n = 1;

        lxr = new Literal(n);
        OP op1 = op.equals("++") ? add : sub;
        gen(op1, v, lxr, newValue, node.getLocation());
        gen(assign, newValue, null, (Var) v, node.getLocation());

        node.setValue(newValue);
    }

    public void visit(UnaryOpNode node) throws CompileError {
        String op = node.getOp();
        ExprNode expr = node.getExpr();
        Value value;
        Temp t;

        switch (op) {
            case "-":
                super.visit(node);
                value = expr.getValue();
                if(value instanceof Var){
                    t = new Temp(value.getIRType());
                    gen(minus, value, null, t, node.getLocation());
                    setValue(node, t);
                }else{
                    Literal l=(Literal) value;
                    setValue(node,l);
                }
                break;
            case "~":
                super.visit(node);
                value = expr.getValue();
                t = new Temp(value.getIRType());
                gen(neg, value, null, t, node.getLocation());
                setValue(node, t);
                break;
            case "++":
            case "--":
                if (node instanceof PostfixOpNode)
                    visit((PostfixOpNode) node);
                else visit((PrefixOpNode) node);
                break;
            case "!":
                if (!InControlFlow && needBoolValue == null) {
                    needBoolValue = node;
                }

                Label tLbl, fLbl;
                if (InControlFlow && node.getTrueLabel() == null && node.getFalseLabel() == null
                        || needBoolValue != node && node.getTrueLabel() == null && node.getFalseLabel() == null) {
                    throw new RuntimeException("不该出现的情况:" + node + "," + node.getLocation());
                }
                fLbl = (node.getFalseLabel() != null) ? node.getFalseLabel() : genLabel();
                tLbl = (node.getTrueLabel() != null) ? node.getTrueLabel() : Label.Fall;

                //设定跳转目标
                expr.setTrueLabel(fLbl);
                expr.setFalseLabel(tLbl);
                //翻译子节点
                generateBoolExpr(expr);

                //根据出口定值
                if (needBoolValue == node) {
                    genBoolValue(node, fLbl);
                }

                //表示本层布尔表达式翻译完毕
                InControlFlow = false;
                break;
            case "+":
                super.visit(node);
                value = expr.getValue();
                setValue(node, value);
        }
    }

    public void visit(AddressNode node) throws CompileError {
        boolean flag=needAddress;
        needAddress=true;

        super.visit(node);
        ExprNode expr=node.getExpr();
        Value value=expr.getValue();
        Temp t = new Temp(pointer);
        gen(lea, value, null, t, node.getLocation());

        if(value instanceof Temp temp){
            temp.setArrayAddr(true);
        }

        if(expr instanceof VariableNode variable&&(variable.isFunctionVariable())){
            t.setFunctionPointer(true);
        }
        setValue(node, t);

        needAddress=flag;
    }


    public void visit(DerefrenceNode node) throws CompileError {
        boolean b=LeftSide;
        LeftSide=false;
        super.visit(node);
        LeftSide=b;

        Value v=node.getExpr().getValue();
        TypeNode type=node.getExpr().getType();
        Var result= setDerefValue(node.getLocation(),v,type.getDerefType());
        setValue(node,result);
    }

    public void visit(CastNode node) throws CompileError {
        super.visit(node);
        TypeNode des = node.getTypeNode();
        Value arg = node.getRightHandSideExpr().getValue();
        Temp t = new Temp(des);

        if (des.isPointerType()) {
            gen(cast, arg, Type.pointer, t, node.getLocation());
        }else if(des.isType(BaseType.BOOL)&&arg instanceof Literal literal){
            Value result=literal.isZero()?Literal.getZero(t.getIRType()):
                    new Literal("1",t.getIRType());
            setValue(node,result);
            return;
        }
        else {
            if (des.isComposedType())
                throw new RuntimeException("should not happen:不能转换为复合类型");
            gen(cast, arg,Type.TypeNodeToIRType(des), t, node.getLocation());
        }

        setValue(node, t);
    }

    public void visit(LiteralNode node) {
//        pw.println("##################\n进入LiteralNode");
        String value = node.getLiteralValue();
        Type type=Type.getLiteralType(node);
        node.setValue(new Literal(value, type));
    }

    /**
     * 数组访问
     */
    public void visit(ArrayNode node) throws CompileError {
        ExprNode array = node.getArray();
        ExprNode idx = node.getIdx();
        Boolean flag = LeftSide;
        //生成计算数组基地址的代码
        LeftSide =false;
        generate(array);
        LeftSide = flag;

        generate(idx);
        Value index=idx.getValue();
        boolean indexIsLiteral=index instanceof Literal;
        if(indexIsLiteral)
            index.setIRType(int64);

        Value curOffset;
        TypeNode element=node.getElementType();
        int width = CalculateTypeWidth.getTypeWidth(element);

        //计算本层级的偏移量
        if(index instanceof Literal){
            int product=width*Integer.parseInt(((Literal) index).getLxrValue());
            curOffset=new Literal(String.valueOf(product), int64);
        }else{
            Temp offsetOfCurLevel = new Temp(indexIsLiteral? int64 : int32);
            gen(mul, idx.getValue(), new Literal(String.valueOf(width), int64),
                    offsetOfCurLevel, node.getLocation());
            curOffset=offsetOfCurLevel;
        }

        if (array instanceof ArrayNode
                &&(((ArrayNode) array).getOffset()!=null)) {
            //将本级偏移量与累加和相加
            Value childOffset=((ArrayNode) array).getOffset();
            if(curOffset instanceof Literal&&((Literal) curOffset).isZero()){
                node.setOffset(childOffset);
            }else{
                Type sumType=(childOffset.getIRType()== int64 &&curOffset.getIRType()== int64)?
                        int64 : int32;
                Temp sumOfOffset=new Temp(sumType);
                gen(add, childOffset, curOffset, sumOfOffset, node.getLocation());
                node.setOffset(sumOfOffset);
            }
        } else {
            node.setOffset(curOffset);
            //如果是一个变量,设定其为基地址
            if(!(array instanceof ArrayNode))
                node.setBaseAddr(array.getValue());
        }

        TypeNode derefType=array.getType().getDerefType(),arrayType=array.getType();
        //当相邻两次访问的不是同一个数组时,生成array指令
        if (!derefType.isArrayType() ||!arrayType.isArrayType()) {
            //获取数组基址
            Value arrayBase=node.getBaseAddr();
            Temp derefValue=new Temp(derefType);
            Value offsetOfNode=node.getOffset();

            setArrayRefResult(arrayBase,offsetOfNode,derefType,derefValue,node.getLocation());
            setValue(node,derefValue);

            //置偏移量为解引用后的值,若紧跟着对其的指针式数组访问,这是必须的
            //累积的offset不再起效
            node.setOffset(null);
            //设置新的基地址
            //注意,该新地址不是供上层节点使用
            node.setBaseAddr(node.getValue());
        }
    }

    private void setArrayRefResult(Value arrayBase, Value arrayOffset,TypeNode resultType, Temp arrayResult, Location location){
        arrayResult.setArrayAddr(resultType.isArray()
                ||resultType.isComposedType());
        gen(OP.array, arrayBase,arrayOffset, arrayResult, location);
        arrayResult.setLeftValue(LeftSide);
    }

    /**
     * 在条件循环中生成条件表达式的中间代码
     */
    private void genConditionExpr(ExprNode node) throws CompileError {
        setInStatement(node);
        generateBoolExpr(node);
        InStatement=false;
        InControlFlow = false;
    }

    /**
     * 控制流语句的中间代码
     */

    public void visit(IfNode node) throws CompileError {
        Label next = node.getNext();
        ExprNode boolExp = node.getCondition();
        StmtNode thenStmt = node.getThenStmt(),
                elseStmt = node.getElseStmt();

        //指示条件部分的表达式应该翻译为控制流
        InControlFlow = boolExp.isBoolExpr();
        //Then语句开始标签
        Label L2 = null;

        //设定跳转目标
        boolExp.setTrueLabel(Label.Fall);
        thenStmt.setNext(next);
        if (elseStmt == null) {
            boolExp.setFalseLabel(next);
        } else {
            //else语句开始标签
            L2 = genLabel();
            boolExp.setFalseLabel(L2);
            elseStmt.setNext(next);
        }

        //生成中间代码
        genConditionExpr(boolExp);
        hasJumpStmt=false;
        generate(thenStmt);

        if (elseStmt != null) {
            if(!hasJumpStmt)
                gen(jump, null, null, next, node.getLocation());
            bindLabel(L2);
            generate(elseStmt);
        }
    }

    public void visit(WhileNode node) throws CompileError {
        Label conditionBegin = genLabel();
        Label end =genLabel();
        ExprNode boolExp = node.getCondition();
        StmtNode stmt = node.getStmt();

        InControlFlow = boolExp.isBoolExpr();

        //设定跳转目标
        boolExp.setTrueLabel(Label.Fall);
        boolExp.setFalseLabel(end);
        stmt.setNext(end);

        //翻译条件部分
        bindLabel(conditionBegin);
        genConditionExpr(boolExp);

        //翻译循环体
        recordJumpLabel(end, conditionBegin);
        generate(stmt);

        gen(jump, null, null, conditionBegin, node.getLocation());
        popLabel();

        bindLabel(end);
//        gen(leave,null,null,null,node.getLocation());
    }

    public void visit(DoWhileNode node) throws CompileError {
        Label end= genLabel();
        ExprNode boolExp = node.getCondition();
        StmtNode stmt = node.getStmt();
        InControlFlow = boolExp.isBoolExpr();
        //设定跳转目标
        Label conditionBegin = genLabel(),
                loopBegin = genLabel();
        boolExp.setTrueLabel(loopBegin);
        boolExp.setFalseLabel(Label.Fall);
        stmt.setNext(Label.Fall);

        //翻译循环体
        bindLabel(loopBegin);
        recordJumpLabel(end, conditionBegin);
        generate(stmt);
        popLabel();

        //翻译条件部分
        bindLabel(conditionBegin);
        genConditionExpr(boolExp);
        bindLabel(end);
    }

    public void visit(ForNode node) throws CompileError {
        if(node.getTable()!=null){
            curTable=node.getTable();
            ((LocalTable)curTable).setOffset();
        }

        Label end= genLabel();
        ExprNode boolExp = node.getExpr2();

        List<ExprNode> iniExp=node.getExpr1(),
                postExp=node.getExpr3();
        StmtNode stmt = node.getStmt();
        InControlFlow = boolExp.isBoolExpr();
        //翻译初始化语句
        if(node.getDefinedVariables()!=null){
           for(DefinedVariable definedVariable:node.getDefinedVariables()){
               generate(definedVariable);
           }
        }
        if(iniExp!=null)
            generateForExpr(iniExp);

        //设定跳转目标
        Label beginLabel = genLabel(),
                conditionBegin = genLabel(),
                postLabel = genLabel();
        boolExp.setTrueLabel(beginLabel);
        boolExp.setFalseLabel(Label.Fall);
        stmt.setNext(postLabel);

        gen(jump,null,null,conditionBegin,node.getLocation());

        //翻译循环体
        bindLabel(beginLabel);
        recordJumpLabel(end, conditionBegin);
        generate(stmt);
        popLabel();

        //翻译迭代表达式
        if(postExp!=null){
            bindLabel(postLabel);
            generateForExpr(postExp);
        }
        //翻译条件部分
        bindLabel(conditionBegin);
        genConditionExpr(boolExp);

        bindLabel(end);
        if(node.getTable()!=null){
            curTable = ((LocalTable) curTable).getParent();
        }
    }


    public void generateForExpr(List<ExprNode>  nodes)throws CompileError {
        for(ExprNode node:nodes){
            generateForExpr(node);
        }
    }

    public void generateForExpr(ExprNode node) throws CompileError {
        setInStatement(node);
        generate(node);
        InStatement=false;
    }

    public void genEnter(LocalTable table,Location location){
        setLastRet();
        gen(enter,table,null,null,location);
    }

    public void genExit(){
        setLastRet();
        gen(exit,null,null,null,new Location(-2,-2));
    }

    public void setLastRet(){
        if(!IRs.isEmpty()&&IRs.getLast().getOp()==ret){
            Instruction lastInstr=IRs.getLast();
            lastInstr.setLastRet(true);
        }
    }

    /**
     * 函数返回值的语句,实际实现过程与机器本身强相关
     * 故中间代码能做的只是简单地表明有这个过程
     */
    public void visit(ReturnExprNode node) throws CompileError {
        hasJumpStmt=true;
        generate(node.getRet());
        genReturn(node.getRet().getValue(),node.getLocation());
    }

    public void visit(JumpNode node) {
        int op = node.getOp();
        hasJumpStmt=true;
        switch (op) {
            case JumpNode.BREAK:
            case JumpNode.CONTINUE:
                LinkedList<Label> stack = (op == JumpNode.BREAK) ? breakStack : continueStack;
                pw.println("\n***********************************");
                pw.println("当前栈内容:" + stack);
                Label des = stack.getLast();
                gen(jump, null, null, des, node.getLocation());

                break;
            case JumpNode.RETURN:
                genReturn(null,node.getLocation());
        }
    }

    public void genReturn(Value retValue,Location location){
//        gen(leave,null,null,null,location);
        if(retValue==null){
            /*等待后续回填*/
            gen(jump,null,null,curRetLabel,location);
        }else{
            gen(ret, retValue, null, null, location);
        }
    }

    /**
     * 普通语句的中间代码
     */
    public void visit(BlockNode node) throws CompileError {
        //设置当前符号表
        curTable = node.getLocalTable();
        ((LocalTable)curTable).setOffset();
        pw.println("\n\n--------------------------------------------" +
                "--------------------------");
        pw.println(node+"的符号表:");
        pw.println(curTable);
        //进入块
//        if(!InLoop)
//            gen(enter,(LocalTable)curTable,null,null,node.getLocation());
        for(Node n:node.getBlockComponents()){
            if(n instanceof DefinedVariable var){
                if(!var.isPriv())
                    generate(var);
            }else if(n instanceof StmtNode stmt){
                //控制流语句需要下一个语句的标签
                if(stmt.isControlStmt()){
                    Label next=genLabel();
                    stmt.setNext(next);
                    generate(stmt);
                    bindLabel(next);
                }else {
                    generate(stmt);
                }
            }else{
                throw new RuntimeException("不应该出现的节点:"+n+"位置:"+n.getLocation()+",类型:"+n.getClass());
            }
        }

        //离开块
//        if(!InLoop&&
//                ((curTable instanceof GlobalTable)||
//                        !((LocalTable) curTable).isProcessTable()))
//            gen(leave,null,null,null,node.getLocation());

        //恢复符号表
        if(curTable!=null)
            curTable = ((LocalTable) curTable).getParent();
    }


    public void visit(DefinedVariable node) throws CompileError {
        ExprNode expr=node.getInit();
        visitIni(expr,node);
        if (node.getIniExprList() != null) {
            List<ExprNode> exprs = node.getIniExprList();
            if (node.supportListInitialize()) {
                //数组的列表初始化
                if(node.getType().isArray()){
                    genArrayIni(node, exprs);
                }
                //复合类型的列表初始化
                else {
                    genComposedVarIni(node,exprs);
                }
            } else {
                visitIni(exprs.get(0), node);
            }
        }
    }

    public void visitIni(ExprNode expr, DefinedVariable node) throws CompileError {
        if(expr!=null){
            InStatement=false;
            generate(expr);
            Value v=expr.getValue();
            if(!((node.isGlobal()||node.isStatic())&&v instanceof Literal)){
                gen(assign,v,null,node,expr.getLocation());
            }
            node.setIniValue(v);
        }
    }

    /**
     * 创建数组的列表初始化语句
     */
    private void genArrayIni(DefinedVariable node, List<ExprNode> iniList){
        int offset=0;
        boolean isArray=node.getType().isArray();

        for(ExprNode expr:iniList){
            TypeNode derefType;
            int width;

            derefType = node.getType().getArrayBaseType();
            width = derefType.getWidth();

            try {
                InStatement=false;
                generate(expr);
                Temp derefResult=new Temp(derefType);

                //t=a[i].addr
                gen(array,node,new Literal(String.valueOf(offset), int64),
                        derefResult,expr.getLocation());
                derefResult.setLeftValue(true);
                //a[i](t)=expr
                Value value=expr.getValue();
                gen(assign, value,null, derefResult ,expr.getLocation());

            } catch (CompileError e) {
                throw new RuntimeException(e);
            }finally {
               offset+=width;
            }
        }
    }

    /**
     *创建复合类型的列表初始化语句
     */
    public void genComposedVarIni(DefinedVariable node, List<ExprNode> iniList){
        int offset=0;
        int i=0;
        ComposedType composedType = (ComposedType) (node.getType().getBase());
        definedComposedType defType=composedType.getTypeEntry();

        TypeNode derefType;
        if(defType.isStruct()){
            for(ExprNode expr:iniList){

                derefType = composedType.getNthMemberType(i);
                offset=defType.getOffsetOfMember(i);

                generateMemberAssign(expr,node,offset,true,derefType);

                i++;
            }
        }
        /*union类型,只需要赋值给最后的字段或用最后的表达式赋值*/
        else{
            ExprNode expr;
            int exprNum=iniList.size(),memberNum=defType.getMembers().size();
            if(memberNum!=0){
                int index=(exprNum>=memberNum)?memberNum-1:exprNum-1;
                expr = iniList.get(index);
                derefType = composedType.getNthMemberType(index);

                generateMemberAssign(expr,node,offset,true,derefType);
            }
        }

        LeftSide=false;
    }

    private void generateMemberAssign(ExprNode expr, DefinedVariable node, int offset, boolean dotAccess,
                                      TypeNode derefType){
        LeftSide=false;
        try {
            generate(expr);
        } catch (CompileError e) {
            throw new RuntimeException(e);
        }
        LeftSide=true;

        Var derefResult = generateMemberRef(node, offset, true, derefType,null);
        Value value = expr.getValue();
        gen(assign, value, null, derefResult, expr.getLocation());
    }

    public void visit(ExprStmt node) throws CompileError {
        ExprNode expr = node.getExpr();
        setInStatement(expr);
        generate(expr);
        InStatement = false;

    }

    public void setInStatement(ExprNode expr){
        if ((expr instanceof AssignNode) || (expr instanceof PostfixOpNode))
            InStatement = true;
    }


    public void setValue(ExprNode node, Value value) {
        if(value instanceof Temp){
            ((Temp)value).setType(node.getType());
        }
        node.setValue(value);
    }

    /**
     * 当变量是一个左值时,如数组,成员访问,指针的解引用
     * 翻译结果需根据在赋值运算的位置而定
     * @param addr 计算的地址值,可能需要其解引用
     * @param derefType 结果类型
     */
    public Var setDerefValue(Location location, Value addr, TypeNode derefType) {
        Var result;
        //如果结果是一个数组或结构体,联合体,不需要解引用
        if(derefType.isArray()||derefType.isComposedType()){
            result= (Var) addr;
        }else{
            Temp t=new Temp(derefType);
            gen(deref, addr, null, t,location);
            result= t;
        }

        //在赋值运算的左边,设置左值属性
        if(result instanceof Temp temp)
            temp.setLeftValue(LeftSide);

        return result;
    }

    /**
     * 生成指令标签
     * 生成指令标签
     */
    private Label genLabel() {
        Label l = new Label();
        return l;
    }

    /**
     * 将标签与下一条指令的标号绑定
     */
    private void bindLabel(Label l) {
        labelMap.put(l, curInstrId);
        List<Label> labels = posMap.computeIfAbsent(curInstrId, k -> new LinkedList<>());
        labels.add(l);
        l.setIrPos(curInstrId);
    }

    /**
     * 记录break和continue语句的跳转目标
     */
    private void recordJumpLabel(Label breakLabel, Label continueLabel) {
        breakStack.addLast(breakLabel);
        continueStack.addLast(continueLabel);
    }

    /**
     * 将当前循环记录的break和continue跳转目标弹出栈
     */
    private void popLabel() {
        breakStack.removeLast();
        continueStack.removeLast();
    }

    /**
     * 生成中间代码指令
     */
    private Instruction gen(OP op, Value arg1, Value arg2, Result result, Location location) {
        Instruction instruction=new Instruction(op, arg1, arg2, result, location,
                translateImmediately);
        addInstr(instruction);
        return instruction;
    }

    private ParamInstruction genParam(Value arg1,Location location){
        ParamInstruction paramInstruction=new ParamInstruction(arg1,location,translateImmediately);
        addInstr(paramInstruction);
        return paramInstruction;
    }

    private Instruction genCall(Value arg1, Value arg2, Result result,Callee callee, List<Value> args, Location location){
        Instruction Callinstr=new CallInstruction(arg1,arg2,result,args,callee,location,translateImmediately);
        addInstr(Callinstr);
        return Callinstr;
    }

    private void addInstr(Instruction instruction){
        IRs.add(instruction);
        //有副作用的不要增加进待翻译列表
        if(!instruction.isTranslateImmediately()){
            storeInstrOfCall.addLast(instruction);
        }
        OP op=instruction.getOp();
        //将跳转到某标签的指令与该标签绑定
        if(op==jump||op==if_jump||op==ifFalse_jump){
            bindInstruction(instruction);
        }
        //递增指令号
        curInstrId++;
    }


    private void genConditionJump(OP op, OP jumpType, Value arg1, Value arg2, Result result, Location location) {
        Instruction instruction=new ConditionJump(op, arg1, arg2, result, location, jumpType,translateImmediately);
        IRs.add(instruction);
        bindInstruction(instruction);
        //递增指令号
        curInstrId++;
    }

    private void bindInstruction(Instruction instruction){
        Label l=(Label) instruction.getResult();
        List<Instruction> instructions = jumpMap.computeIfAbsent(l, k -> new LinkedList<>());
        instructions.add(instruction);
    }

    /**
     * 合并位置相同的标签
     */
    private void combineLabels(){
        for(int i:posMap.keySet()){
            List<Label> labels=posMap.get(i);
            Label l=labels.get(0);

            //跳转到其他标签的指令改为跳转到l
            for(int j=1;j<labels.size();j++){
                Label lj=labels.get(j);
                List<Instruction> instructions=jumpMap.get(lj);
                if(instructions==null)
                    continue;
                for(Instruction ins:instructions){
                    ins.setResult(l);
                }
            }
            //如果有指令跳转到l所在的位置,将l绑定到l所在位置的指令
            if(labels.stream().anyMatch(lbl->jumpMap.get(lbl)!=null)){
                Instruction ins=IRs.get(l.getIrPos());
                ins.setLabel(l);
            }
            //删去该位置的其他标签
            labels.clear();
            labels.add(l);
        }
        /*优化跳转语句,主要是地址连续的两条的跳转语句*/
        boolean optimized=true;
        while (optimized){
            optimized=false;
            List<Instruction> instructions=new ArrayList<>(IRs) ;
            List<Instruction> removeInstr=new ArrayList<>();
            for (int i=0;i<instructions.size();i++ ) {
                Instruction instr=instructions.get(i);
                if ((instr.getOp()==if_jump || instr.getOp()==ifFalse_jump) && i<IRs.size()-2) {
                    if(conJumpInstrOptimizable(instr,i)){
                        removeInstr.add(instructions.get(i+1));
                        optimized=true;
                    }
                }
            }
            removeInstr.forEach(i->IRs.remove(i));
        }
        /*更新标签的位置*/
        for (int i=0;i<IRs.size();i++ ) {
            Instruction instr=IRs.get(i);
            if (instr.getLabel()!=null) {
                Label label=instr.getLabel();
                label.setIrPos(i);
            }
        }
    }

    public boolean conJumpInstrOptimizable(Instruction instr,int i){
        Instruction next =IRs.get(i+1);
        if(next.getOp()==jump){
            Instruction desInstr=IRs.get(i+2);
            Label desLabel=desInstr.getLabel();
            if(desLabel!=null&&desLabel.equals(instr.getResult())){
                instr.setOp(instr.getOp()==if_jump?ifFalse_jump:if_jump);
                instr.setResult(next.getResult());
               return true;
            }
        }
        return false;
    }

    /**
     * 划分基本快
     */
    public void splitIntoBlocks() {
        combineLabels();
        //各基本块的开始节点
        List<Integer> starts = new LinkedList<>();
        HashMap<Label,Block> labelToBlock=new HashMap<>();

        //加入第一条语句
        starts.add(0);
        Block startBlock=Block.getNewEnterBlock();
        Block exitBlock=Block.getNewExitBlock();
        //找到开始语句列表
        for (int i = 1; i < IRs.size(); i++) {
            Instruction ins = IRs.get(i);
            OP op = ins.getOp();
            if (op == jump || op == if_jump || op == ifFalse_jump) {
                Label l = (Label) ins.getResult();
//                if(!l.isBlockEdge())
//                    continue;

                int des = l.getIrPos();
                starts.add(des);
                /*条件传输语句不作为划分基本块的依据*/
                if (i < IRs.size() - 1 && !starts.contains(i + 1))
                    starts.add(i + 1);
            }else if(op==ret){
                if (i < IRs.size() - 1 && !starts.contains(i + 1))
                    starts.add(i + 1);
            }
        }

        pw.println("\n-----------------------------------\n" +
                "starts=" + starts + "\nsize=" + starts.size());
        /* 构建基本快集合 */
        starts.sort(Integer::compare);
        //记录自然流动到本基本块的前基本块
        Block preBlock=null;
        for (int i = 0; i < starts.size(); i++) {
            int s = starts.get(i),
                    e = (i < starts.size() - 1) ? starts.get(i + 1) : IRs.size();
            if (s != e) {
                List<Instruction> instructions = IRs.subList(s, e);
                pw.println("\n\n--------------------------------------" +
                        "\ncreating new Block:start=" + s + ",end=" + e);
                Block block= new Block(instructions);
                blocks.add(block);

                /*设置基本块的标签*/
                Instruction firstInstr=instructions.get(0);
                Label startLbl=firstInstr.getLabel();
                if(startLbl!=null){
                    block.setBlockLabel(startLbl);
                    labelToBlock.put(startLbl,block);
                }
                if(firstInstr.getOp()==enter){
                    startBlock.addNextBlock(block);
                }

                Instruction lastInstr=instructions.get(instructions.size()-1);
                if(preBlock!=null){
                    preBlock.addNextBlock(block);
                }
                /*无条件跳转不能自然流动到下一基本块*/
                if(lastInstr.getOp()== jump||lastInstr.getOp()== ret){
                    preBlock=null;
                    if(lastInstr.getOp()==ret){
                        block.addNextBlock(exitBlock);
                        blocks.add(startBlock);
                        blocks.add(exitBlock);

                        startBlock=Block.getNewEnterBlock();
                        exitBlock=Block.getNewExitBlock();
                    }
                }else{
                    preBlock=block;
                }
            }
        }

        /*设定基本块之间的前驱和后继关系*/
        for(Block block:blocks){
            block.getInstructions().stream()
                    .filter(i -> i.jumpToOtherBlock())
                    .forEach(instr -> {
                        Label des=(Label) instr.getResult();
                        Block desBlock=labelToBlock.get(des);
                        if(desBlock==null){
                            throw new RuntimeException();
                        }
                        block.addNextBlock(desBlock);
                    });
        }

        for(Block block:blocks){
            block.printSep(pw,true);
            block.printAllInstructions(pw);
            block.printSep(pw,false);
        }
    }

    public List<Block> getBlocks() {
        return blocks;
    }
}
