package Semantic_Analysis;

import AST.AST;
import AST.DEFINE.*;
import AST.TYPE.*;
import CompileException.CompileError;
import CompileException.errorHandler;
import AST.Node;
import utils.CalculateTypeWidth;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;

public class DefinedTypeChecker extends Visitor {
    //主要任务:
    //处理类型定义,检查循环依赖问题和变量定义,函数定义中类型的有效性
    //处理自定义类型,包括struct,union,typedef的类型引用问题,建立类型引用到类型定义的关系

    private errorHandler handler;
    private PrintWriter pw;
    private HashMap<String, definedUserType> typeTable;

    public static final int checked = 0;
    private static final int checking = 1;
//    private static final int uncheck=2;

    public DefinedTypeChecker(errorHandler handler, PrintWriter pw, HashMap<String, definedUserType> typeTable) {
        this.handler = handler;
        this.pw = pw;
        this.typeTable = typeTable;
    }

    public void checkDefinedType(AST ast) {
        HashMap<definedComposedType, Integer> flags = new HashMap<>();
        //处理自定义变量
        for (definedUserType type : ast.getTypes()) {
            if (type instanceof definedComposedType defCom) {
                try {
                    //检查是否有重复的成员或void类型的成员
                    checkMember(type);
                    //检查类型是否循环依赖
                    if (flags.get(type) == null)
                        checkRecursiveTypeDefinition(defCom, flags);
                } catch (CompileError e) {
                    handler.handle(e);
                }
            }
        }

        //处理函数或变量的类型声明
        for (definedUserType composedType : ast.getTypes()) {
            if (composedType instanceof definedComposedType) {
                definedComposedType ct = (definedComposedType) composedType;
                try {
                    super.visit(ct);
                } catch (CompileError e) {
                   pw.println("caught in processing definedComposedType:"+ct);
                   e.printStackTrace(pw);
                }
            }
        }
        for (Entity entity : ast.getEntities()) {
            check(entity);
        }

        /*输出结构体的内存布局*/
        CalculateTypeWidth.typeTable=typeTable;
        for(definedUserType type:ast.getTypes()){
            if(type instanceof definedComposedType defCom){
                if(defCom.isStruct()){
                    pw.println("\n"+defCom.getMemoryArragingDescription());
                }
            }
        }
    }

    /**
     *检查成员是否定义重复
     */
    private void checkMember(definedUserType type) {
        if (type instanceof definedComposedType) {
            HashSet<SlotNode> marks = new HashSet();
            for (SlotNode member : ((definedComposedType) type).getMembers()) {
                try {
                    if (marks.contains(member))
                        throw new CompileError("成员" + member + "定义重复", member.getLocation());
                    marks.add(member);
                } catch (CompileError e) {
                    handler.handle(e);
                }
            }
        }
    }


    //用DFS遍历方式的递归版本
    public void checkRecursiveTypeDefinition(definedComposedType type, HashMap<definedComposedType, Integer> flags) throws CompileError {
        if (type == null) return;
        Integer flag = flags.get(type);
        pw.println("\n---------------------------\n" +
                "检查" + type + "的循环依赖问题");
        if (flag != null) {
            pw.println("flag=" + flag);
            if (flag == checked) return;
            else if (flag == checking) {
                throw new CompileError(type + "循环定义", type.getLocation());
            }
        }

        flags.put(type, checking);
        for (SlotNode member : type.getMembers()) {
            TypeNode memType = member.getType();
            BaseType base = memType.getBase();

            //访问类型前缀的依赖
            //而类型后缀为指针类型或函数类型,不存在循环依赖问题
            if (!memType.isPointerOrArr()&& base instanceof ComposedType) {
                ComposedType ct = (ComposedType) base;
                try {
                    checkRecursiveTypeDefinition((definedComposedType) typeTable.get(ct.getId()), flags);
                } catch (CompileError e) {
                    handler.handle(e);
                }
            }
        }

        flags.put(type, checked);
        pw.println("\n---------------------------\n" +
                "检查" + type + "结束");
    }

    public void visit(DefinedFunction function) {
        //函数的返回类型不能是结构体或联合体,或者不支持的类型
        //检查返回类型
        TypeNode ret = function.getTypeOfLeftHandSide();
        BaseType base = ret.getBase();
        PostfixNode postfix = ret.getPostfix();

        if (!ret.isValidated()) {
            handler.error("函数返回类型错误" + ret, ret.getLocation());
            return;
        }

        if (base instanceof ComposedType && postfix == null) {
            handler.error("函数不能返回结构体或联合体", ret.getLocation());
            return;
        }

        //检查函数参数
        if(!function.hasNoParam()){
            for (ParamNode param : function.getParams()) {
                if(param.isKwargs())continue;
                if (!TypeNode.isValidateDeclaration(param.getType())) {
                    handler.error("参数" + param.getName() + "的类型:" +
                            param.getTypeOfLeftHandSide() + "不合法", function.getLocation());
                }
            }
        }

        //检查函数体
        try {
            if(function.getBody()!=null){
                super.visit(function.getBody());
            }
        }catch (CompileError e){
            pw.println("caught in Function body:");
            e.printStackTrace(pw);
        }
    }

    public void visit(DefinedVariable node) {
        TypeNode type = node.getType();
        if (!TypeNode.isValidateDeclaration(type)) {
            handler.error("变量" + node.getName() + "的类型:" +
                    type + "不合法", node.getLocation());
        }
    }

    public void visit(SlotNode node) {
        if (!TypeNode.isValidateDeclaration(node.getType())) {
            handler.error("成员" + node + "的类型不合法",
                    node.getLocation());
        }
    }

    public void check(Node node) {
        try {
            node.accept(this);
        } catch (CompileError e) {
            e.printStackTrace();
        }
    }
}
