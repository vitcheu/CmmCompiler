package AST.EXPR;

import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;
import CompileException.*;

public class LHSNode extends ExprNode{
    public LHSNode(Location location) {
        super(location);
    }

    @Override
    public void accept(ASTVisitor ASTVisitor)throws CompileError {
        ASTVisitor.visit(this);
    }

    @Override
    public boolean isLeftValueExpr() {
        return true;
    }
}
