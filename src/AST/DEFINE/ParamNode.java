package AST.DEFINE;

import AST.EXPR.AssignNode;
import AST.EXPR.Assignable;
import AST.EXPR.ExprNode;
import AST.Node;
import AST.TYPE.TypeNode;
import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;
import CompileException.*;

public class ParamNode extends Node implements Assignable {
    private TypeNode type;
    private String name=null;
    //对应的实参
    private ExprNode value=null;
    //该形参转换得到的局部变量
    private DefinedVariable localVariable=null;

//    public final static String KWARGS="kwargs";

    public  static ParamNode KwargsNode=new ParamNode(null,null,"Kwargs");

    public ParamNode(Location location, TypeNode type, String name) {
        super(location);
        this.type = type;
        this.name = name;
        localVariable=toLocalVariable();
    }

    public ParamNode(Location location, TypeNode type) {
        super(location);
        this.type = type;
        localVariable=toLocalVariable();
    }

    /**
     *将该节点以及赋值右侧的right节点一起组成赋值节点
     * 以共检查参数传递中的赋值
     */
    public AssignNode toAssignNode(ExprNode right){
        if(isKwargs())
            return new AssignNode(null,right);
        else
            return new AssignNode(type,right);
    }

    public void setType(TypeNode type) {
        this.type = type;
    }

    @Override
    public void accept(ASTVisitor ASTVisitor) throws CompileError {
        ASTVisitor.visit(this);
    }

    @Override
    public void dump(int level) {
        dumpHead("Parm",level);
            ident(level+1,toString());
        dumpEnd("Parm",level);
    }

    @Override
    public String toString() {
        if(this.equals(KwargsNode)) return "...";
        return "%s %s".formatted(type, (name != null) ? name : "");
    }

    private DefinedVariable toLocalVariable(){
        return new DefinedVariable(getLocation(),false,type,name,null);
    }

    public DefinedVariable getLocalVariable() {
        return localVariable;
    }

    public void setLocalVariable(DefinedVariable localVariable) {
        this.localVariable = localVariable;
    }

    public TypeNode getTypeOfLeftHandSide() {
        return type;
    }

    public TypeNode getType(){
        return type;
    }

    public String getName() {
        return name;
    }

    @Override
    public ExprNode getRightHandSideExpr() {
        return value;
    }


    @Override
    public void setRightHandSideExpr(ExprNode node) {
        this.value=node;
    }

    public boolean isKwargs(){
        return  this.equals(KwargsNode);
    }
}
