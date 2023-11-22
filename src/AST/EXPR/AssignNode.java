package AST.EXPR;

import AST.TYPE.TypeNode;
import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;
import CompileException.*;

public class AssignNode extends ExprNode implements Assignable{
    protected   ExprNode lhs=null;
    protected    ExprNode expr=null;

    public AssignNode(Location location, ExprNode lhs, ExprNode expr) {
        super(location);
        this.lhs = lhs;
        this.expr = expr;
    }

    public AssignNode(TypeNode type, ExprNode right) {
        super(right.getLocation());
        setType(type);
        expr=right;
    }

    @Override
    public void accept(ASTVisitor ASTVisitor)throws CompileError {
        ASTVisitor.visit(this);
    }

    @Override
    public void dump(int level) {
        dumpHead("Assign",level);
            ident(level+1,"Left Hand Side:");
                lhs.dump(level+2);
            ident(level+1,"Right Hand Side:");
                expr.dump(level+2);
            if(this instanceof OpAssignNode){
                OpAssignNode opAssignNode=(OpAssignNode) this;
                ident(level+1,"op:"+opAssignNode.getOp());
            }
        dumpEnd("Assign",level);
    }

    public ExprNode getLhs() {
        return lhs;
    }

    public ExprNode getRightHandSideExpr() {
        return expr;
    }

    @Override
    public TypeNode getTypeOfLeftHandSide() {
        return getType();
    }

    @Override
    public TypeNode getType() {
        if(lhs!=null)
            return lhs.type;
        else
            return type;
    }

    @Override
    public String toString() {
        return "AssignNode{" +
                "location=" + location +
                '}';
    }

    public void setRightHandSideExpr(ExprNode expr) {
        this.expr = expr;
    }

    public void setLhs(ExprNode lhs) {
        this.lhs = lhs;
    }

    @Override
    public boolean isConstExpr() {
        return expr.isConstExpr();
    }

    @Override
    public boolean isLiteralExpr() {
        return expr.isLiteralExpr();
    }
}
