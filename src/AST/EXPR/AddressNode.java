package AST.EXPR;

import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;
import CompileException.*;

public class AddressNode extends ExprNode{
    ExprNode expr;

    public AddressNode(Location location, ExprNode expr) {
        super(location);
        this.expr = expr;
    }

    @Override
    public void accept(ASTVisitor ASTVisitor) throws CompileError {
        ASTVisitor.visit(this);
    }

    @Override
    public void dump(int level) {
        dumpHead("Address",level);
        expr.dump(level+1);
        dumpEnd("Address",level);
    }

    public ExprNode getExpr() {
        return expr;
    }

    @Override
    public String toString() {
       return "&"+"("+expr+")";
    }

    @Override
    public boolean isPointer() {
        return true;
    }

    @Override
    public boolean isCallable() {
       return expr.isCallable();
    }
}
