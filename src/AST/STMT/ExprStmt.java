package AST.STMT;

import AST.EXPR.ExprNode;
import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;
import CompileException.*;

public class ExprStmt extends StmtNode{
    ExprNode expr;

    public ExprStmt(Location location, ExprNode expr) {
        super(location);
        this.expr = expr;
    }

    @Override
    public void accept(ASTVisitor ASTVisitor)throws CompileError {
        ASTVisitor.visit(this);
    }

    @Override
    public String toString() {
        return "ExprStmt:" +expr;
    }

    @Override
    public void dump(int level) {
        dumpHead("Expr Stmt",level);
            expr.dump(level+1);
        dumpEnd("Expr Stmt",level);
    }

    public ExprNode getExpr() {
        return expr;
    }
}
