package AST.EXPR;

import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;import CompileException.*;

public class UnaryOpNode extends ExprNode{
    protected String op;
    protected ExprNode expr;
    public UnaryOpNode(Location location,String op,ExprNode expr) {
        super(location);
        this.op=op;
        this.expr=expr;
    }

    public String getOp() {
        return op;
    }

    @Override
    public void accept(ASTVisitor ASTVisitor)throws CompileError {
        ASTVisitor.visit(this);
    }

    @Override
    public void dump(int level) {
        dumpHead(" "+op+" ",level);
            ident (level+1,"expression:");
            expr.dump(level+1);
        dumpEnd(" "+op+" ",level);
    }

    public ExprNode getExpr() {
        return expr;
    }

    @Override
    public String toString() {
        return   op+"("+expr+")";
    }

    @Override
    public boolean isBoolExpr() {
        return op.equals("!");
    }

    public void setExpr(ExprNode expr) {
        this.expr = expr;
    }

    @Override
    public boolean isLiteralExpr() {
        return expr.isLiteralExpr();
    }

    @Override
    public boolean isConstExpr() {
        return expr.isConstExpr();
    }
}
