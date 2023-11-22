package AST.EXPR;

import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;
import CompileException.*;

public class sizeOfExprNode extends ExprNode{
    private ExprNode expr;

    public sizeOfExprNode(Location location, ExprNode expr) {
        super(location);
        this.expr = expr;
    }

    @Override
    public void accept(ASTVisitor ASTVisitor) throws CompileError {
        ASTVisitor.visit(this);
    }

    @Override
    public void dump(int level) {
        dumpHead("sizeOfExpr",level);
        expr.dump(level+1);
        dumpEnd("sizeOfExpr",level);
    }

    public ExprNode getExpr() {
        return expr;
    }

    @Override
    public String toString() {
        return "sizeOfExprNode{" +
                "location=" + location +
                '}';
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
