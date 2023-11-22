package Semantic_Analysis;

import AST.AST;
import AST.DEFINE.*;
import AST.DEFINE.definedComposedVariable;
import AST.TYPE.BaseType;
import AST.TYPE.ComposedType;
import AST.TYPE.TypeNode;
import AST.TYPE.UserType;
import CompileException.*;
import AST.Node;

import java.io.PrintWriter;
import java.util.HashMap;

/**
 * 处理自定义类型变量的引用
 */
public class TypeResolver extends Visitor {
    //根据类型名找到对应的类型定义
    private HashMap<String, definedUserType> typeTable = new HashMap<>();
    private HashMap<String,TypeNode> definedTypeCache =new HashMap<>();
    private errorHandler handler;
    private PrintWriter pw;

    public TypeResolver(errorHandler handler, PrintWriter pw) {
        this.handler = handler;
        this.pw = pw;
    }

    private void resolve(Node node) {
        try {
            node.accept(this);
        } catch (CompileError e) {
            handler.handle(e);
            e.printStackTrace();
        }
    }

    public void resolve(AST ast) {
        pw.println("\n-------------------@Type Resolver start");
        //先自定义构造变量表
        for (definedUserType type : ast.getTypes()) {
            typeTable.put(type.getTypeName(), type);
        }

        //访问所有可能引用自定义变量的地方
        for (definedUserType type : ast.getTypes()) {
            resolveDefinedType(type);
        }

        for (DefinedVariable var : ast.getVariables()) {
            resolve(var);
        }

        for (DefinedFunction fun : ast.getFunctions()) {
            resolveFunction(fun);
        }
    }

    private void resolveDefinedType(definedUserType type) {
        //访问成员的类型节点
        if (type instanceof definedComposedType definedCt) {
            for (SlotNode slot : (definedCt.getMembers())) {
                resolve(slot.getType());
            }
        }else{
            TypedefNode typedef=(TypedefNode) type;
            resolve(typedef);
        }
    }

    private void resolveFunction(DefinedFunction fun) {
        try{
          super.visit(fun);
        } catch (CompileError e) {
            pw.println("Caught in visiting Function body:" + fun.getBody());
            e.printStackTrace(pw);
        }
    }

    @Override
    public void visit(TypeNode node) throws CompileError {
        if(node==null||node.getBase()==null){
            throw new RuntimeException();
        }
        BaseType baseType = node.getBase();
        if (baseType instanceof ComposedType) {
            pw.println("\n-----------------------\n" +
                    "处理" + baseType + " " + node.getLocation() + "的引用问题");
            ComposedType ct = (ComposedType) baseType;
            definedComposedType entry = (definedComposedType) typeTable.get(ct.getId());
            if (entry != null) {
                ct.setTypeEntry(entry);
                pw.println(ct + "的类型为:\n" + entry);
            } else {
                handler.handle(new CompileError("无法找到类型" + ct.getId() + "的定义", ct.getLocation()));
            }
        }
        /*将自定义类型更改为实际类型*/
        else if(baseType instanceof UserType userType){
            userType=(UserType)baseType;
            TypedefNode typedefNode=(TypedefNode) (typeTable.get(userType.getTypeName()));
            if(typedefNode==null){
                throw new CompileError("找不到类型"+userType.getTypeName()+"的定义",node.getLocation());
            }

            TypeNode realType=typedefNode.getType();
            if(realType.getBase() instanceof UserType){
                throw new CompileError("类型"+realType.getBase()+"尚未定义",realType.getLocation());
            }

            if (definedTypeCache.containsKey(node.toString())) {
                TypeNode typeNode = definedTypeCache.get(node.toString());
                node.copyFrom(typeNode);
            } else {
                definedTypeCache.put(node.toString(), node);
                pw.println("#原类型:" + node);
                node.shiftToRealType(realType);
                pw.println("#转换后:" + node);
            }
        }
    }

    public void visit(DefinedVariable defvar) {
        try {
            //先消解变量的类型
            super.visit(defvar);
            if(defvar instanceof definedComposedVariable){
                definedComposedVariable cv=(definedComposedVariable) defvar;
                //将它转换为更加具体的变量类型
                pw.println("cv=" +cv);
                pw.println("转换为复合类型变量....");
                pw.println("Defined Variable=" + cv);
                pw.println("Type=" +cv.getType());

                //根据类型表的类型定义初始化成员变量
                String typeName=cv.getType().getBase().getId();
                if(typeTable.get(typeName)==null){
                    handler.handle(new CompileError("无法找到类型" + typeName + "的定义", defvar.getLocation()));
                }
                cv.setMemberVariables((definedComposedType) typeTable.get(typeName));

                pw.println("cv=" +cv);
            }
        } catch (CompileError e) {
            throw new RuntimeException(e);
        }
    }

    public HashMap<String, definedUserType> getTypeTable() {
        return typeTable;
    }
}
