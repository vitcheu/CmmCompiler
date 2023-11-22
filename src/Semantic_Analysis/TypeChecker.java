package Semantic_Analysis;

import AST.DEFINE.*;
import AST.EXPR.*;
import AST.STMT.BlockNode;
import AST.STMT.JumpNode;
import AST.STMT.ReturnExprNode;
import AST.STMT.StmtNode;
import AST.TYPE.ComposedType;
import AST.TYPE.TypeNode;
import CompileException.*;
import AST.Node;
import AST.AST;
import AST.Callee;
import IR.Constants.TypeWidth;
import Semantic_Analysis.SymbolTable.GlobalTable;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;

import static AST.TYPE.BaseType.*;
import static AST.TYPE.BaseType.CHAR;

public class TypeChecker extends Visitor {
    //主要任务:
    //处理赋值和解引用,成员访问呢,取地址相关的问题
    //赋值和取地址需要所操作对象为左值类型
    //而解引用和成员访问则需要指针类型(包括数组);

    private final errorHandler handler;
    private PrintWriter pw;
    private final HashMap<String, definedUserType> typeTable;
    private final GlobalTable globalTable;
    private DefinedFunction curFunction = null;
    private boolean hasReturnStmt = false;
    private boolean isGloble=false;

    public TypeChecker(errorHandler handler, PrintWriter pw, HashMap<String, definedUserType> typeTable,
                       GlobalTable globalTable) {
        this.handler = handler;
        this.pw = pw;
        this.typeTable = typeTable;
        this.globalTable=globalTable;
    }

    public void checkAst(AST ast) {
        isGloble=true;
        for (DefinedVariable var:ast.getVariables()) {
            try {
                check(var);
            } catch (CompileError e) {
//                //(e);
            }
        }
        isGloble=false;

        for(DefinedFunction fun:ast.getFunctions()){
            try {
                check(fun);
            } catch (CompileError e) {
//                //(e);
            }
        }
        Node.setDumpWriter(pw);
        pw.println("\n\nAST:");
        ast.dump(0);
    }

//    public void printLine() {
//        //("-----------------------------\n");
//    }

    private void check(Node node) throws CompileError {
//        printLine();
//        //("check:" + node);
        node.accept(this);
    }



    public void visit(BlockNode node) {
        for (DefinedVariable var : node.getDefVars()) {
            try {
                check(var);
            } catch (CompileError e) {
                pw.println("\n-----------------------\n################\ncaught in DefinedVariable" + var);
                handler.handle(e);
                e.printStackTrace(pw);
            }
        }
        for (StmtNode stmtNode : node.getStmts()) {
            try {
                check(stmtNode);
            } catch (CompileError e) {
                pw.println("\n-----------------------\n" +
                        "####################\ncaught in Statement:" + stmtNode);
                handler.handle(e);
                e.printStackTrace(pw);
            }
        }

    }

    public void visit(DefinedVariable node) throws CompileError {
        super.visit(node);
        TypeNode type = node.getType();
        ExprNode ini = node.getInit();
        List<ExprNode> iniList=node.getIniExprList();
        if (ini != null){
            testDefinedVariableIniExpr(node,ini);
        }
        if(iniList!=null){
            if(node.getType().isComposedType()){
                int i=0;
                ComposedType ct=(ComposedType) (node.getType().getBase());
                for(ExprNode expr:iniList){
                    if(i>=ct.getMemberNum()){
                        break;
                    }

                    TypeNode lt=ct.getNthMemberType(i);
                    ExprNode newExpr= testDefinedVariableIniExpr(lt,expr);
                    node.setNthIniExpr(i,newExpr);
                    i++;
                }
            }else if(node.getType().isArray()){
                TypeNode lt=node.getType().getArrayBaseType();
                int len=node.getType().getArrayLen();
                int i=0;
                for(ExprNode expr:iniList){
                    /*尽量保证数组不越界*/
                    if(len!=0&&i>=len){
                        handler.warning(String.format("数组越界:index=%d,数组长度=",i,len)
                                ,expr.getLocation());
                        break;
                    }
                    ExprNode newExpr= testDefinedVariableIniExpr(lt,expr);
                    node.setNthIniExpr(i,newExpr);
                    i++;
                }
            }
            //普通变量的赋值
            else{
                if(!iniList.isEmpty())
                    testDefinedVariableIniExpr(node,iniList.get(0));
            }
        }

        node.setType(type);
    }

    private ExprNode testDefinedVariableIniExpr(DefinedVariable node, ExprNode ini)throws CompileError {
        if(node.isPriv()&&!(ini.isLiteralExpr())){
            throw new CompileError("静态变量的初始化表达式须由常量组成",node.getLocation());
        }
        else if(node instanceof DefinedConst &&!(ini.isConstExpr())){
            throw new CompileError("const变量的初始化表达式须为常量表达式",node.getLocation());
        }
        return testAssign(node,node, ini,false);
    }

    /**
     *类型转换后的赋值运算右侧节点
     */
    private ExprNode testDefinedVariableIniExpr(TypeNode lt,ExprNode ini) throws CompileError {
        AssignNode assignNode=new AssignNode(lt,ini);
        if(lt==null){
            throw  new RuntimeException();
        }
        testAssign(assignNode,assignNode, ini,false);
        return assignNode.getRightHandSideExpr();
    }


    public void visit(AssignNode node) throws CompileError {
        super.visit(node);
        ExprNode left = node.getLhs();
        ExprNode right=node.getRightHandSideExpr();
        //检查左侧是否为左值
        checkLeftHandSide(left);
        //检查右侧是否可赋值给左侧
        testAssign(node,left,right,false);

        //设置类型为赋值左侧的类型
        node.setType(left.getType());
    }

    public void visit(CallNode node) throws CompileError {
        super.visit(node);
        ExprNode expr = node.getExpr();
        if (!expr.isCallable()) {
            throw new CompileError(expr + "不可调用", expr.getLocation());
        }

        //检查参数是否与声明匹配
        Callee callee=null;
        if(expr instanceof VariableNode variableNode){
            callee = (DefinedFunction) variableNode.getEntry();
        }else if(expr instanceof DerefrenceNode derefrenceNode){
            TypeNode typeNode=derefrenceNode.getType();
            callee=typeNode;
            pw.println("call  node="+node+",caller="+callee+",funcType="+typeNode);
        }

        List<ParamNode> params = callee.getParams();
        List<ExprNode> args = node.getArgNode().getArgs();

        if(callee.containParamAfterKwargs()){
            throw new CompileError("可变长参数的后面不能有其他参数");
        }
        if (params.size() != args.size()&&!callee.isKwarg()) {
            throw new CompileError("参数的个数不匹配,需要" + params.size() + "个,实为" + args.size() + "个", node.getLocation());
        }
        if(callee.isKwarg()&&params.size()-1>args.size()){
            throw new CompileError("实参的数量不够,需要至少"+params.size()+"个,提供了"+args.size()+"个",node.getLocation());
        }

        //实参可以比形参数量少一个,因为可以不给可变参数传值
        for (int i = 0; i <args.size(); i++) {
            ParamNode param=null;
            ExprNode argNode=null;
            try {
                 param = callee.getNthParam(i+1);
                 argNode = args.get(i);

                //单精度参数传参至可变长参数函数时,需要转换为双精度
                if (callee.isKwarg(i+1) && argNode.getType().isType(FLOAT)) {
                    argNode = typeConversion(argNode, DOUBLE);
                }
                AssignNode assignNode=param.toAssignNode(argNode);
                if(!param.isKwargs())
                    testAssign(assignNode,param.getTypeOfLeftHandSide(), argNode,true);

                param.setRightHandSideExpr(assignNode.getRightHandSideExpr());
                args.set(i,assignNode.getRightHandSideExpr());
            } catch (CompileError e) {
                throw new CompileError("参数类型不匹配" +
                        "应为：" + param.getType() +
                        "实为：" +argNode.getType(), node.getLocation());
            }
        }
        //设置类型
        node.setType(callee.getType());
    }

    public void visit(DerefrenceNode node) throws CompileError {
        super.visit(node);
        ExprNode expr = node.getExpr();
        TypeNode type = expr.getType();
        pw.println("解引用:" + expr);
        if (!type.isPointerOrArr()) {
            throw new CompileError("解引用的对象需为指针类型:" + expr, node.getLocation());
        }
        else if(type.isVoidPtr()){
            throw new CompileError("不能对void*指针解引用",node.getLocation());
        }

        TypeNode base = type.getDerefType();
        pw.println("Ptr Base=" + base);
        node.setType(base);
    }

    public void visit(ArrayNode node) throws CompileError {
        super.visit(node);
        ExprNode arr = node.getArray();
        ExprNode idx = node.getIdx();
        TypeNode type = arr.getType();
        if (!type.isPointerOrArr()) {
            throw new CompileError("需为数组或指针类型:" + arr, node.getLocation());
        }
        if (type.isFunctionType()) {
            throw new CompileError("函数指针不能用于数组访问" + arr, node.getLocation());
        }
        //检查idx的类型是否为整数
        if (!idx.getType().isIntType()) {
            throw new CompileError("数组偏移量需为整数:" + idx, node.getLocation());
        }

        //设置类型
        TypeNode arrBase=type.getDirectReferenceType();
        node.setType(arrBase);
    }

    public void visit(MemberNode node) throws CompileError {
        super.visit(node);
        String operator = node.getOp();
        String name = node.getName();
        TypeNode type = node.getExpr().getType();
        SlotNode member;

        pw.println("\n***************************");
        pw.println("取成员运算:,var=" + node.getExpr() + ",name of member=" + name);
//        if(type==null){
//            throw new CompileError("这里暂时还没发处理",node.getLocation());
//        }

        if (operator.equals(".")) {
            if (!type.isComposedType()) {
                throw new CompileError("需为结构体或联合体:" + type, node.getLocation());
            }
            //获取成员
            member=getMember((ComposedType) type.getBase(), name);
        }

        //->
        else {
            TypeNode baseType = type.getPointerBaseType();
            if (baseType == null || !baseType.isComposedType()) {
                throw new CompileError("需为结构体或联合体的指针:" + type, node.getLocation());
            }
            pw.println("\n*****************\n" + type);
            pw.println("baseType:" + baseType);
            member=getMember((ComposedType) baseType.getBase(), name);
        }
        if (member==null) {
            throw new CompileError("类型" +node.getExpr() + "无成员:" + name, node.getLocation());
        }

        //设置类型
        pw.println("获得的成员为:"+member);
        node.setType(member.getType());
    }

    public void visit(AddressNode node) throws CompileError {
        pw.println("\n---------------------\n检查&运算");
        super.visit(node);
        ExprNode expr = node.getExpr();
        pw.println("expr=" + expr);
        pw.println("type=" + expr.getType());

        //检查是否可取地址
        if (!(expr instanceof LHSNode)) {
            throw new CompileError("取地址运算的对象需为左值:" + expr, node.getLocation());
        }

        //设置类型
        TypeNode pointer = getLeaType(expr);
        pw.println("Pointer=" + pointer);
        node.setType(pointer);

    }

    private TypeNode getLeaType(ExprNode node){
        if(node.isFunction()){
            VariableNode variable=(VariableNode)node;
            DefinedFunction function=(DefinedFunction) (variable.getEntry());
            return function.toFunctionType().getAddressOpType();
        }else{
            return node.getType().getAddressOpType();
        }
    }

    public void visit(UnaryOpNode node) throws CompileError {
        //+,-,++,--,~,!
        super.visit(node);

        String op = node.getOp();
        TypeNode type = node.getExpr().getType();

        switch (op) {
            case "+":
            case "-":
                expectsNumOperand(op,node.getExpr());
                if (!type.isFloatType()&&!type.isIntType()&&op.equals("-"))
                    convertToInt(node);
                setUnaryOpType(node);
                break;
            case "!":
                expectsBoolOperand(node);
                node.setType(new TypeNode(BOOL));
                break;
            case "~":
                expectsIntCompatibleOperand(op,node.getExpr());
                convertToInt(node);
                node.setType(node.getExpr().getType());
                break;

            //++,--
            default:
                ExprNode expr = node.getExpr();
                if (!(expr instanceof LHSNode)) {
                    throw new CompileError("自增/自减的操作数需为左值" + expr, expr.getLocation());
                }
                if(expr.getType().isVoidPtr()){
                    throw new CompileError("void*,未知的大小", node.getLocation());
                }
                expectsSelfOpOperand(node);
                //类型不变
                node.setType(node.getExpr().getType());
                break;
        }
    }

    public void visit(VariableNode node) throws CompileError {
        if (node.getEntry() == null)
            throw new CompileError("找不到变量" + node.getId() + "的定义,"+node.getLocation(), node.getLocation());
        //设置变量类型
        node.setType(node.getEntry().getTypeOfLeftHandSide());
    }

    public void visit(BinaryOpNode node) throws CompileError {
        checkBinaryOpNode(node,true);
    }

    /**
     * 检查二元运算的类型
     * @param leftExprChangeable true:左操作数需要转换,false:左操作数需要转换
     */
    private void checkBinaryOpNode(BinaryOpNode node,boolean leftExprChangeable) throws CompileError {
        super.visit(node);
        String op = node.getOp();
        ExprNode lexp = node.getLeft(),
                rexp = node.getRight();
        TypeNode ltype = lexp.getType(),
                rtype = rexp.getType();
        if (ltype == null) throw new CompileError("无法获知"+lexp+"的类型");
////        //("Binary OP:");
////        //("op=" + op);
////        //("left=" + lexp + ",right=" + rexp);
        if (op.equals("+") || op.equals("-")) {
            //指针与整数相加
            if (op.equals("+") &&
                    (ltype.isPointerOrArr())&& rtype.isIntType() ||
                            rtype.isPointerOrArr() && ltype.isIntType()) {
                if(ltype.isVoidPtr()||rtype.isVoidPtr()){
                    throw new CompileError("void*,未知的大小", node.getLocation());
                }
                //类型依然为指针
                node.setType(ltype.isPointerOrArr() ? ltype : rtype);
            }

            //指针减整数
            else if (op.equals("-") && (ltype.isPointerOrArr() && rtype.isIntType())) {
                if(ltype.isVoidPtr()||rtype.isVoidPtr()){
                    throw new CompileError("void*,未知的大小", node.getLocation());
                }
                node.setType(ltype);
            }

            //指针减指针,结果为64位整数
            else if (op.equals("-") && (ltype.isPointerOrArr() && rtype.isPointerOrArr())) {
                if(ltype.isVoidPtr()||rtype.isVoidPtr()){
                    throw new CompileError("void*,未知的大小", node.getLocation());
                }
                node.setType(new TypeNode(int64Type));
            }

            //可加数相加减
            //float可以与char,bool类型相加减
            else {
                expectsNumOperand(op,lexp,rexp);
                //类型转换
                //若有浮点数参与,统一转换为浮点数
                convertToFloat(node,leftExprChangeable);
                //其他类型参与运算,需转换为整数
                convertToInt(node,leftExprChangeable);
                //设置类型
                setBinaryOpType(node);
            }
        }

        else if (op.equals("*") || op.equals("/")) {
            expectsMultipleOperand(op,lexp,rexp);
            //int与float运算时转换为浮点数
            convertToFloat(node,leftExprChangeable);
            convertToInt(node,leftExprChangeable);
            //设置类型
            setBinaryOpType(node);
        }

        else if (op.equals("%")) {
            //要求操作数均为int,结果为int
            expectsIntOperand(op,lexp,rexp);
            convertToInt(node,leftExprChangeable);
           setBinaryOpType(node);
        }

        else if (op.equals("&") || op.equals("|") || node.getOp().equals("^")||
        op.equals("<<")||op.equals(">>")) {
            //要求操作数为int,bool,char
            expectsIntCompatibleOperand(op,lexp,rexp);

            convertToInt(node,leftExprChangeable);
            //如果是移动指令,转换类型为8位整数
            if(op.equals(">>")||op.equals("<<")){
                convertToInt8(node);
            }
           setBinaryOpType(node);
        }

        else if (isRelOP(op)) {
            expectsRelOperand(op,lexp,rexp);
            convertToPointer(node,leftExprChangeable);
            convertToFloat(node,leftExprChangeable);
            convertToInt(node,leftExprChangeable);
            node.setType(new TypeNode(BOOL));
        }

        else if (op.equals("&&") || op.equals("||")) {
            //运算对象可为数或者指针
            //结果类型为char,表示布尔值
            //不用进行类型转换
            expectsBoolOperand(op,lexp,rexp);
            node.setType(new TypeNode(BOOL));
        }
    }


    public void visit(CastNode node) throws CompileError {

        super.visit(node);
        ExprNode expr = node.getRightHandSideExpr();
        TypeNode desType = node.getTypeNode();
        testCast(node);
        node.setType(desType);
    }

    public void visit(ConditionNode node) throws CompileError {
        super.visit(node);
        ExprNode condition = node.getCondition(),
                trueCase = node.getExpr1(),
                falseCase = node.getExpr2();
        //测试condition是否是bool类型的
        if (!isBoolOperand(condition.getType())) {
            throw new CompileError(condition + "需为布尔表达式", condition.getLocation());
        }
        //使左右类型相容
        testAssign(node,trueCase.getType(), falseCase,false);
        node.setType(trueCase.getType());
    }

    public void visit(sizeOfExprNode node) throws CompileError {
        super.visit(node);
        if (!node.getExpr().isLeftValueExpr()) {
            throw new CompileError("sizeof运算对象需为左值", node.getExpr().getLocation());
        }
        node.setType(new TypeNode(intType));
    }

    public void visit(sizeOfTypeNode node) throws CompileError {
        super.visit(node);
        if (node.getTypeNode().isVoid())
            throw new CompileError("sizeof运算对象不能为void", node.getLocation());
        node.setType(new TypeNode(intType));
    }

    public void visit(OpAssignNode node) throws CompileError {
        String op=node.getOp();
        BinaryOpNode bin=node.toBinaryOpNode();
        ExprNode lhs= node.getLhs();
        //检查左侧是否为左值
        checkLeftHandSide(lhs);
        //将复合赋值当作二元运算节点进行类型检查,且左操作数不需要转换
        checkBinaryOpNode(bin,false);
        //获取结果类型
        TypeNode binType=bin.getType();
        //如果左侧类型和结果类型不一致,则必然导致宽类型向窄类型隐式转换的错误
        //如int i;  i+=1.2; (int)=(float)
        if(!lhs.getType().equals(binType)){
            throw new CompileError("不相容的类型:"+lhs.getType()+"和"+binType,node.getLocation());
        }
        //重新设置类型转换后的右节点
        node.setRightHandSideExpr(bin.getRight());
        //设置类型
        node.setType(lhs.getType());
    }


    /**
     * 检查函数定义,判断函数中的返回类型是否与return语句的类型相容
     */
    public void visit(DefinedFunction node) throws CompileError {
        curFunction = node;
        hasReturnStmt = false;
        //转换形参的类型,如将数组转换为指针
        for(ParamNode param:node.getParams()){
            if(param.isKwargs()) continue;

            TypeNode realType=param.getTypeOfLeftHandSide();
            if(realType.isArray()){
                param.getTypeOfLeftHandSide().paramArrTransformToPointer();
                pw.println("形参"+param.getName()+"的类型从"+realType+"转换至"+param.getTypeOfLeftHandSide());
            }
            param.setType(realType);
        }
        super.visit(node);

        if(node.isDefined()){
            //函数无返回语句
            if (!hasReturnStmt && !curFunction.getTypeOfLeftHandSide().isVoid()) {
                handler.error("函数" + curFunction.getName() + "无返回语句", node.getLocation());
            }
        }
    }

    /**
     * 将字符串字面量设置为char*类型
     */
    public void visit(StringNode node) {
        TypeNode strType = (new TypeNode(charType)).createPointer();
        node.setType(strType);
    }

    public void visit(JumpNode node) {
        super.visit(node);
        //return ;语句
        if (node.getOp() == JumpNode.RETURN) {
            TypeNode ret = curFunction.getType();
            pw.println("当前函数" + curFunction.getName() + ",返回类型为:" + ret);
            if (!ret.isVoid()) {
                handler.error("函数" + curFunction.getName() + "的返回类型为:" + curFunction.getTypeOfLeftHandSide() + ",不为void",
                        node.getLocation());
            }
            hasReturnStmt = true;
        }
    }

    public void visit(ReturnExprNode node) throws CompileError {
        check(node.getRet());
        //return expr;语句
        TypeNode ret = curFunction.getTypeOfLeftHandSide();
        pw.println("当前函数的返回类型为:" + ret + ",返回语句为" + node.getRet() + ",类型" + node.getRet().getType());
        //测试是否相容
        try {
            if(node.getRet()!=null){
                testAssign(node,ret, node.getRet(),false);
            }
        } catch (CompileError e) {
            handler.error("返回值与函数签名不符,需要" + ret + ",实为" + node.getRet().getType(),
                    node.getLocation());
        }
        hasReturnStmt = true;
    }


    private SlotNode getMember(ComposedType cp, String name) throws CompileError {
        definedComposedType ct = (definedComposedType) typeTable.get(cp.getId());
        if(ct==null){
           throw new CompileError();
        }
        return ct.getMember(name);
    }

    /**
     * 判断操作数类型是否为int,char,bool,float等数字类型
     */
    private void expectsNumOperand(String op,ExprNode left,ExprNode right) throws CompileError {
        expectsNumOperand(op,left);
        expectsNumOperand(op,right);
    }

    private void expectsRelOperand(String op,ExprNode left,ExprNode right) throws CompileError{
        TypeNode lt=left.getType(),rt=right.getType();
        if(!lt.isNum()&&!lt.isPointerType()){
            unMatchedOpError(op,left);
        }else if(!rt.isNum()&&!rt.isPointerType()){
            unMatchedOpError(op,right);
        }
    }

    private void expectsNumOperand(String op,ExprNode expr) throws CompileError {
        if (!expr.getType().isNum()) {
            unMatchedOpError(op, expr);
        }
    }


    /**
     * 判断是否为int,bool,char等与int相等或兼容的类型
     */
    private void expectsIntCompatibleOperand(String op,ExprNode left,ExprNode right) throws CompileError {
       expectsIntCompatibleOperand(op,left);
       expectsIntCompatibleOperand(op,right);
    }

    private void expectsIntCompatibleOperand(String op,ExprNode expr) throws CompileError {
        if (!expr.getType().isIntCompatible()) {
            unMatchedOpError(op,expr);
        }
    }

    /**
     * 判断操作数是否可乘除
     */
    private void expectsMultipleOperand(String op,ExprNode left,ExprNode right) throws CompileError {
        TypeNode lt = left.getType(),
                rt = right.getType();

        if (lt.isPointerOrArr() || rt.isPointerOrArr()
                || lt.isType(BOOL) || rt.isType(BOOL)
                || lt.isType(CHAR) || rt.isType(CHAR)
                || lt.isType(VOID) || rt.isType(VOID)) {
            unMatchedOpError(op, left);
        }
    }

    private void expectsIntOperand(String op,ExprNode left,ExprNode right) throws CompileError {
        TypeNode lt =left.getType(),
                rt =right.getType();
        if (!lt.isIntType()|| !rt.isIntType()) {
            unMatchedOpError(op,left);
        }
    }

    private void expectsBoolOperand(String op,ExprNode left,ExprNode right) throws CompileError {
        TypeNode lt =left.getType(),
                rt =right.getType();
        if (isBoolOperand(lt) && isBoolOperand(rt)) {
            return;
        }
        unMatchedOpError(op,left);
    }

    private void expectsBoolOperand(UnaryOpNode node) throws CompileError {
        if (isBoolOperand(node.getExpr().getType())) {
            return;
        }
        unMatchedOpError(node.getOp(), node);
    }

    /**
     * 判断是否为布尔类型,布尔类型包括一切可以表示为数的类型和指针
     */
    private boolean isBoolOperand(TypeNode type) {
        return type.isPointerOrArr() || type.isNum();
    }

    /**
     * 判断是否为适合自增,自减的类型
     */
    private void expectsSelfOpOperand(UnaryOpNode node) throws CompileError {
        TypeNode type = node.getExpr().getType();
        if (!type.isPointerOrArr() && !type.isIntCompatible()
                ||type.isType(BOOL)
                ||type.isArray()) //数组名不能自增/自减
        {
            unMatchedOpError(node.getOp(), node);
        }
    }


    /**
     * 设置二元运算的类型
     */
    private void setBinaryOpType(BinaryOpNode node) {
        ExprNode lexp = node.getLeft(),
                rexp = node.getRight();
        TypeNode ltype = lexp.getType(),
                rtype = rexp.getType();
        String op=node.getOp();
        if (ltype.isFloatType() || rtype.isFloatType())
            setFloatType(node,ltype,rtype);
        //字符相加,升级为32位整型
        else if(op.equals("+")&&(ltype.isType(CHAR)||rtype.isType(CHAR))){
            node.setType(new TypeNode(intType));
        }
        else setIntType(node,ltype,rtype);
    }

    /**
     *选择合适宽度的浮点类型类型
     */
    private void setFloatType(ExprNode node,TypeNode lt,TypeNode rt){
        if(lt.getWidth()>TypeWidth.floatWidth
                ||rt.getWidth()>TypeWidth.floatWidth){
            node.setType(new TypeNode(DOUBLE));
        }
        else node.setType(new TypeNode(FLOAT));
    }

    /**
     * 选择合适的整数类型
     */
    private void setIntType(ExprNode node,TypeNode lt ,TypeNode rt){
        //这里假设左右表达式的类型已经一致
        node.setType(lt);
    }


    private void setUnaryOpType(UnaryOpNode node) {
        node.setType(node.getExpr().getType());
    }

    /**
     * 将node转换为s指定的类型
     * @return 一个类型转换节点
     */
    private static ExprNode typeConversion(ExprNode node, String s) {
        TypeNode typeNode=new TypeNode(s);
        return typeConversion(node,typeNode);
    }

    private static ExprNode typeConversion(ExprNode node,TypeNode target){
        TypeNode type = node.getType();
        /*判断是否是整数间或浮点数间的转换
        * 若是,可不产生新的cast结点
        **/
        boolean f2fOrI2I = (type.isFloatType() && target.isFloatType()
                            || (!type.isFloatType() && !target.isFloatType()));
        if (node instanceof LiteralNode && f2fOrI2I 
                && !(node instanceof FloatNode)) {
            node.setType(target);
            return node;
        } else if (node instanceof CastNode castNode && f2fOrI2I) {
            castNode.setType(target);
            return castNode;
        }
//        throw new RuntimeException();
        return new CastNode(node.getLocation(), node, target);
    }

    private static ExprNode convertToFloatType(TypeNode target, ExprNode node){
        return typeConversion(node,target);
    }

    /**
     * 检查赋值操作中的左侧
     */
    private void checkLeftHandSide(ExprNode left) throws CompileError {
        // 检查赋值左侧是否为左值
        if (!(left.isLeftValueExpr()) || left.isComposedType()) {
            throw new CompileError("赋值运算的左侧需为左值:" + left, left.getLocation());
        }
        //这里简单地令const变量不能再被赋值
        if(left.isConst()){
            throw new CompileError("const变量不能被赋值:"+left,left.getLocation());
        }
    }

    /**
     * 检查运算的操作数,有必要时转换为整数类型
     * @param leftExpCanChange true:左操作数需要转换,false则不需要转换.
     */
    private void convertToInt(BinaryOpNode node,boolean leftExpCanChange) {
        ExprNode  lexp=node.getLeft(),
                rexp=node.getRight();
        TypeNode ltype = lexp.getType(),
                rtype = rexp.getType();
        String op=node.getOp();

        if(ltype.isFloatType()&&rtype.isFloatType()){
            return;
        }else  if(ltype.isPointerType()||rtype.isPointerType())
            return;

        //先提升到32位整型
        TypeNode int32=new TypeNode(INT);
        if(!ltype.isFloatType()&&ltype.compare(int32)<0){
            node.setLeft(typeConversion(lexp,INT));
        }
        if (!rtype.isFloatType()&&rtype.compare(int32)<0) {
            node.setRight(typeConversion(rexp, INT));
        }

        TypeNode target= getArithmeticType(ltype,rtype);
        if(!node.getLeft().getType().equals(target)){
            node.setLeft(typeConversion(node.getLeft(),target));
        }
        if(!node.getRight().getType().equals(target)){
            node.setRight(typeConversion(node.getRight(),target));
        }
    }

    private void convertToInt8(BinaryOpNode node){
        node.setRight(typeConversion(node.getRight(), Int8));
    }

    private void convertToInt(UnaryOpNode node) {
        ExprNode exp = node.getExpr();
        TypeNode type = exp.getType();
        //char,bool等类型
        if (!type.isFloatType()&&!type.isIntType()) {
            node.setExpr(typeConversion(exp, INT));
        }
    }

    private void convertToPointer(BinaryOpNode node,boolean  leftNodeChangeable){
        ExprNode lexp = node.getLeft(),
                rexp = node.getRight();
        TypeNode ltype = lexp.getType(),
                rtype = rexp.getType();

        if(ltype.isPointerType()&&!rtype.isPointerType()){
            if(rtype.isIntCompatible()){
                node.setRight(typeConversion(rexp, Int64));
            }
        }else if(leftNodeChangeable&& rtype.isPointerType()&&!ltype.isPointerType()){
            if(ltype.isIntCompatible()){
                node.setLeft(typeConversion(lexp, Int64));
            }
        }
    }

    private void convertToFloat(BinaryOpNode node,boolean leftNodeChangeable) {
        ExprNode lexp = node.getLeft(),
                rexp = node.getRight();
        TypeNode ltype = lexp.getType(),
                rtype = rexp.getType();
        if(ltype.isPointerType()||rtype.isPointerType())
            return;
        if (ltype.isFloatType() && !rtype.isFloatType()) {
            node.setRight(convertToFloatType(ltype,node.getRight()));
        }
        else if (leftNodeChangeable && rtype.isFloatType() && !ltype.isFloatType()) {
            node.setLeft(convertToFloatType(rtype,node.getLeft()));
        }
        else if (ltype.isFloatType() && rtype.isFloatType()) {
            TypeNode target = getArithmeticType(ltype, rtype);

            if (!ltype.equals(target) && leftNodeChangeable) {
                node.setLeft(typeConversion(lexp, target));
            }
            if (!rtype.equals(target)){
                node.setRight(typeConversion(rexp,target));
            }
        }
    }

    /**
     * @return 运算后的结果类型
     */
    private TypeNode getArithmeticType(TypeNode lt, TypeNode rt){
        if(lt.isFloatType()&&rt.isFloatType()){
            /*浮点类型之间的比较*/
            if(lt.isType(DOUBLE)||rt.isType(DOUBLE)){
                return new TypeNode(DOUBLE);
            }else{
                return new TypeNode(FLOAT);
            }
        }else{
            if(lt.isType(LONG,false)||rt.isType(LONG,false)){
                return new TypeNode(LONG,false);
            }
            else if(lt.isType(LONG,true)||rt.isType(LONG,true)){
                return new TypeNode(LONG,true);
            }
            else if(lt.isType(INT,false)||rt.isType(INT,false)){
                return new TypeNode(INT,false);
            }
            else return new TypeNode(INT,true);
        }
    }

    private void unMatchedOpError(String op, Node node) throws CompileError {
        throw new CompileError(op + "运算的操作对象类型不正确：" + node.getLocation(), node.getLocation());
    }


    private ExprNode testAssign(Assignable node, Node left, ExprNode right, boolean isParam) throws CompileError {
        TypeNode type=(left instanceof DefinedVariable)?((DefinedVariable) left).getType()
                :((ExprNode)left).getType();

        testArrayAssign(node);
        //将赋值给double变量的浮点数字面量设置为double类型
        if(type.isType(DOUBLE)&&right instanceof FloatNode floatNode
                &&(right.getType().isType(FLOAT))){
            floatNode.setType(new TypeNode(DOUBLE));
//            Value v= right.getValue();
//            v.setIRType(Type.doubleType);
            globalTable.setFloatLiteralType(floatNode.getLiteralValue(),false);
        }
        testAssign(node,type,right,isParam);
        return node.getRightHandSideExpr();
    }

    private boolean  testArrayAssign(Assignable node){
        TypeNode type=node.getTypeOfLeftHandSide();
        ExprNode right=node.getRightHandSideExpr();

        //不能赋值给数组(不可变的左值)
        if (type.isArray()) {
            if(type.isStrType()&&(right instanceof StringNode)){
                ;
            } else{
                handler.error (right.getType()+"类型的变量不能直接赋值给数组", right.getLocation());
                return false;
            }
        }
        return true;
    }



    /**f
     * 测试右表达式是否能赋值给左侧
     * @param isParam 测试参数传递的合法性
     */
    private void testAssign(Assignable node,TypeNode lt, ExprNode right,
                           boolean isParam) throws CompileError {
        TypeNode rt;
//        if(right instanceof VariableNode variable&&variable.isFunctionVariable()){
//            rt
//        }
        rt = right.getType();
        boolean isCastNode=node.isCastNode();

        if((lt.isVoid()||rt.isVoid())&&!isParam){
            throw new CompileError("void类型不能赋值或被赋值");
        }
        //函数指针的赋值
        else if(lt.isFunctionPointer()
                &&!(rt.isFunctionPointer())){
           throw  new CompileError(right+"不能赋值给函数指针"+lt+",rt="+rt);
        }

        //指针与非指针之间赋值,只能将指针赋给bool类型变量
        if (lt.isPointerType() && !rt.isPointerType()
                || !lt.isPointerType() && rt.isPointerType() && !lt.isType(BOOL)) {
            //字符串字面量可以赋值给字符数组或指针
            if(lt.isStrType()&&(right instanceof StringNode)){
                ;
            }
            //字面量可以赋值给指针
            else   if(lt.isPointerType()&&right.isLiteralExpr()&&rt.isIntCompatible()){
                ;
            }
            else if(lt.isPointerType()&&rt.isArray()){
                //可以将同类型的数组赋值给指针
                if(lt.getPointerBaseType().equals(rt.getArrayBaseType())){
                    ;
                }
            }
            else{
                unMatchedOpError("=", right);
            }
        }

        //指针和数组之间的赋值
        if (lt.isPointerOrArr() && rt.isPointerOrArr()) {
            //不应该出现赋值给数组变量的情况
            if (lt.isArray()) {
                String errMsg = String.format("%s,lt=%s,rt=%s",
                        "数组类型不能被赋值", lt, rt);
//                throw new RuntimeException(errMsg+",位置"+right.getLocation());
                throw new CompileError(errMsg, right.getLocation());
            }
//            if (lt.isFunctionType() || rt.isFunctionType()) {
//                throw new CompileError("函数类型不能赋值或被赋值", right.getLocation());
//            }


            TypeNode l = lt, r = rt;
            //参数传递时,数组自动转换为指针,而形参的数组也自动转换为指针
            //故此时数组与指针等价
            if (isParam) {
                l = lt.arrToPointer();
                r = rt.arrToPointer();
            }

            /*当间接层次不一致时,输出警告*/
            if(l.getIndirectLevel()!=r.getIndirectLevel()){
                handler.warning(String.format("%s与%s的间接层次不一样",r,l),right.getLocation());
                return;
            }

            /* 当数组类型赋值给指针时,
             * 数组可以是指针所指向类型的数组,
             * 如int[2][3]可以赋值给int[3]*,因为它们的基类型相同
             */
            if (l.isPointerType() && r.isArrayType()) {
               r=r.arrToPointer();
            }

            /*可以正常编译,因为指针的相互赋值总是可行的*/
            if (!l.equals(r)) {
                //都不是void*指针
                if (!l.isVoidPtr() && !r.isVoidPtr()) {
                    String msg;
                    if(!l.isCompatibleWith(r)){
                        msg=String.format("类型%s与类型%s不兼容",l,r);
                    }else{
                        msg= "类型:%s与类型:%s不匹配".formatted(l.getDescription(), r.getDescription());
                    }
                    handler.warning(msg,
                            right.getLocation());
                    return;
                }
            }
        }
        /*赋值给float类型*/
        else if (lt.isFloatType()) {
            if (!rt.isFloatType()) {
                if(node!=null){
                    node.setRightHandSideExpr(new CastNode(right.getLocation(), right, new TypeNode(floatType)));
                }
            }else if(!lt.equals(rt)){
                if(node!=null){
                    node.setRightHandSideExpr(new CastNode(right.getLocation(),right,lt));
                }
            }
        }
        /*赋值给int类型*/
        else if (lt.isIntType()) {
            if (rt.isFloatType()&&!isCastNode) {
                throw new CompileError("无法将float类型赋值给int类型", right.getLocation());
            //转换为int类型
            } else if (!rt.isIntType()) {
                if(node!=null){
                    //转化为int32类型后与左侧类型比较
                    TypeNode target= getArithmeticType(new TypeNode(intType),lt);
                    node.setRightHandSideExpr(new CastNode(right.getLocation(), right,target));
                }
            }
        }
        /*赋值给char类型*/
        else if (lt.isType(CHAR) && !rt.isType(CHAR)) {
            //只有整数类型才能赋值给char类型
            if(!rt.isIntCompatible()){
                throw new CompileError("无法将类型" + rt+ "赋值给char类型", right.getLocation());
            }
        }
        /*赋值给bool类型*/
        else if(lt.isType(BOOL)){
            if(node!=null){
                /*需要生成显式类型转换*/
                node.setRightHandSideExpr(typeConversion(node.getRightHandSideExpr(),BOOL));
            }
        }
        /*赋值给复合类型变量*/
        else if(lt.isComposedType()&&!rt.isComposedType()){
            throw new CompileError("无法将类型"+rt+"赋值给复合类型",right.getLocation());
        }

        //测试函数返回值
        else if ((lt.isVoid() && !rt.isVoid())&&(isParam)) {
            handler.warning(String.format("void函数:%s返回值",curFunction.getName()), right.getLocation());
        }
    }


    private static boolean isRelOP(String op){
        return op.equals(">")||op.equals("<")||op.equals("==")
                ||op.equals("!=")||op.equals(">=")||op.equals("<=");
    }



    /**
     * 测试是否能类型转换
     */

    private void testCast(CastNode node){
        var lt=node.getTypeNode();
        try {
            testAssign(node,lt,node.getRightHandSideExpr(),false);
        } catch (CompileError e){
            handler.error(String.format("不能将类型%s转换到类型%s",node.getRightHandSideExpr().getType(),lt),
                    e.getLocation());
        }
    }
}
