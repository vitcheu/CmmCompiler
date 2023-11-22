package AST.EXPR;

import AST.TYPE.TypeNode;
import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;import CompileException.*;

public class ConditionNode extends ExprNode implements Assignable{
    ExprNode condition;
    ExprNode expr1;

    ExprNode expr2;

    public ConditionNode(Location location, ExprNode condition, ExprNode e1, ExprNode e2) {
        super(location);
        this.condition = condition;
        this.expr1 = e1;
        this.expr2 = e2;
    }

    @Override
    public void accept(ASTVisitor ASTVisitor)throws CompileError {
        ASTVisitor.visit(this);
    }

    @Override
    public void dump(int level) {
        dumpHead("Condition Expression",level);
            ident(level+1,"Bool Expression:");
                condition.dump(level+2);
            ident(level+1,"True Case Expression:");
                expr1.dump(level+2);
            ident(level+1,"False Case Expression:");
                expr2.dump(level+2);
        dumpEnd("Condition Expression",level);
    }

    public ExprNode getCondition() {
        return condition;
    }

    public ExprNode getExpr1() {
        return expr1;
    }

    public ExprNode getExpr2() {
        return expr2;
    }

    @Override
    public ExprNode getRightHandSideExpr() {
        return expr2;
    }

    @Override
    public TypeNode getTypeOfLeftHandSide() {
        return condition.getType();
    }

    @Override
    public TypeNode getType() {
        return expr1.type;
    }

    @Override
    public void setRightHandSideExpr(ExprNode node) {
        this.expr2=node;
    }

    @Override
    public boolean isLiteralExpr() {
        return condition.isLiteralExpr()&&expr1.isLiteralExpr()&&expr2.isLiteralExpr();
    }

    @Override
    public boolean isConstExpr() {
        return condition.isConstExpr()&&expr1.isConstExpr()&&expr2.isConstExpr();
    }
}
