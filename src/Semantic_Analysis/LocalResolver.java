package Semantic_Analysis;

import AST.AST;
import AST.DEFINE.*;
import AST.EXPR.*;
import AST.STMT.BlockNode;
import AST.STMT.ForNode;
import AST.TYPE.BaseType;
import CompileException.CompileError;
import Semantic_Analysis.SymbolTable.GlobalTable;
import Semantic_Analysis.SymbolTable.LocalTable;
import Semantic_Analysis.SymbolTable.SymbolTable;
import AST.Node;
import CompileException.errorHandler;

import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

/**
 * 建立符号表
 * 解决各个变量和函数的引用,找到它们对应的定义
 */
public class LocalResolver extends Visitor {
    private LinkedList<SymbolTable> tableStack;

    private errorHandler handler;
    private PrintWriter log;
    private GlobalTable globalTable;

    private boolean InFunction = false;

    public LocalResolver(errorHandler handler, PrintWriter log) {
        this.handler = handler;
        this.log = log;
        tableStack = new LinkedList<>();
    }

    public LinkedList<SymbolTable> getTableStack() {
        return tableStack;
    }


    public void resolve(AST ast) {
        globalTable = new GlobalTable();
        tableStack.push(globalTable);

        List<Entity> entityList=ast.getEntities();
        for (int i=0;i<entityList.size();i++) {
            Entity entity = entityList.get(i);
            try {
                if (entity instanceof DefinedVariable) {
                    entityList.set(i, transform((DefinedVariable) entity));
                }
                globalTable.defineEntity(entity);
            } catch (CompileError e) {
                throw new RuntimeException(e);
            }
        }

        resolveGlobalInitializer(ast.getVariables());
        resolveFunctions(ast.getFunctions());
        //最后
        ast.setGlobalTable(globalTable);
    }

    private SymbolTable getCurrentTable() {
        return tableStack.getLast();
    }

    private SymbolTable popTable() {
        return tableStack.removeLast();
    }

    private void pushTable(List<DefinedVariable> variables, int n, String name) {
        LocalTable localTable = new LocalTable(getCurrentTable(),n,name);
        for (DefinedVariable var : variables) {
            localTable.addVariable(var,getGlobalTable(),handler);
        }
        localTable.setProcessTable(InFunction);
        tableStack.addLast(localTable);
    }

    private LocalTable pushNewEmptyTable(){
        LocalTable localTable = new LocalTable(getCurrentTable(),0,null);
        localTable.setProcessTable(InFunction);
        tableStack.addLast(localTable);
        return localTable;
    }

    /**
     * 处理全局变量,常量的初始化表达式中的变量引用
     */
    private void resolveGlobalInitializer(List<DefinedVariable> vars) {
        for (DefinedVariable var : vars) {
            if (var.getInit() != null)
                resolve(var.getInit());
//            visit(var.getInit());
//            super.visit(var.getInit());
        }
    }

    /**
     * 处理函数
     */
    private void resolveFunctions(List<DefinedFunction> functions) {
        for (DefinedFunction function : functions) {
            if(!function.isDefined())
                continue;

            //构建符号表
            List<DefinedVariable> vars = new LinkedList<>();
            //将形参添加进局部变量表
            if (function.getParams() != null) {
                for (int i = 0; i < function.getParams().size(); i++) {
                        ParamNode parm = function.getParams().get(i);
                        if(!parm.isKwargs()){
                            parm.setLocalVariable(transform(parm.getLocalVariable()));
                            DefinedVariable var=parm.getLocalVariable();
                            var.setParam(true);
                            vars.add(var);
                        }
                }
            }
//            transformVarList(body.getDefVars());
//            vars.addAll(body.getDefVars());

//            List<DefinedVariable> defvars=body.getDefVars();
//            for(int i=0;i<defvars.size();i++){
//                defvars.set(i,transform(defvars.get(i)));
//                vars.add(defvars.get(i));
//            }
            InFunction = true;
            pushTable(vars,function.getParams().size(),function.getName());

            //访问函数体
            if(function.getBody()!=null){
                BlockNode body = function.getBody();
                resolve(body);
            }
        }
    }


    /**
     * 将变量定义转换为更加具体的类型,如联合体和结构体变量
     */
    private DefinedVariable transform(DefinedVariable var) {
        if (var.getType().isComposedType()) {
            return new definedComposedVariable(var);
        }
        return var;
    }

    private void transformVarList(List<DefinedVariable> variableList) {
        for (int i = 0; i < variableList.size(); i++) {
            variableList.set(i, transform(variableList.get(i)));
        }
    }

    public void visit(VariableNode var) {
        SymbolTable table = getCurrentTable();
        try {
            Entity entity = table.get(var.getId());
            log.println("\n------------------");
//            if (entity instanceof DefinedFunction) {
//                throw new CompileError("变量名" + var.getId() + "与函数名冲突",var.getLocation());
//            }

            var.setEntry(entity);
            entity.addReferencedCnt();

            log.println("变量" + var.getLocation() + " " + var + "的定义为:");
            log.println(entity);
        } catch (CompileError e) {
            handler.handle(e);
        }
    }

    public void visit(LiteralNode node) {
//        SymbolTable table=getCurrentTable();
        if(node.getType().isType(BaseType.FLOAT)){
            globalTable.addFloatLiteral(node.getLiteralValue(),node.getType().isType(BaseType.FLOAT));
            log.println("加入浮点字面量:"+node.getLiteralValue());
        }else if(node.getType().isType(BaseType.STRING)){
            globalTable.addStr(node.getLiteralValue());
            log.println("加入字符串字面量:"+node.getLiteralValue());
        }
    }

    public void visit(UnaryOpNode node) throws CompileError {
        String op=node.getOp();
        if(op.equals("-")&&(node.getExpr() instanceof LiteralNode)){
            LiteralNode lNode=(LiteralNode) node.getExpr();
            if(!lNode.getType().isType(BaseType.STRING)){
                String lxr=lNode.getLiteralValue();
                if(lxr.equals("0")){
                    ;
                } else {
                    if (lxr.contains("-")) {
                        lxr.replace("-", "");
                    } else if (lxr.contains("+")) {
                        lxr.replace("+", "-");
                    } else {
                        lxr = "-" + lxr;
                    }
                }
                lNode.setLxrValue(lxr);
            }
        }

        super.visit(node);
    }
    public void visit(ForNode node) throws CompileError {
        if(node.getDefinedVariables()!=null){
            //如果有子作用域,将初始化语句的变量置于子作用域中
               transformVarList(node.getDefinedVariables());
               pushTable(node.getDefinedVariables(),0,null);
               node.setTable((LocalTable)getCurrentTable());
        }
        super.visit(node);
//        else if(node.getExpr1()!=null){
//            for(ExprNode expr:node.getExpr1()){
//                resolve(expr);
//            }
//        }
//        resolve(node.getExpr2());
//        resolve(node.getStmt());
//        if(node.getExpr3()!=null){
//            for(ExprNode expr:node.getExpr3()){
//                resolve(expr);
//            }

        //弹出初始化语句中的符号表
        if(node.getDefinedVariables()!=null){
            popTable();
        }
    }

    public void visit(BlockNode block) {
        LocalTable table;
        //如果不是函数体,创建新符号表
        if (!InFunction) {
            transformVarList(block.getDefVars());
            table=pushNewEmptyTable();
        }else{
            table=(LocalTable) getCurrentTable();
        }
        InFunction = false;

        for(Node n:block.getBlockComponents()){
            resolve(n);
            if(n instanceof DefinedVariable){
                table.addVariable((DefinedVariable) n,getGlobalTable(),handler);
            }
        }

        //给该作用域设置符号表
        block.setLocalTable((LocalTable) popTable());
    }

    private void resolve(Node node) {
        try {
            node.accept(this);
        } catch (CompileError e) {
            e.printStackTrace();
        }
    }

    public GlobalTable getGlobalTable() {
        return globalTable;
    }
}
