package AST.STMT;

import AST.EXPR.ExprNode;
import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;import CompileException.*;

public class IfNode extends StmtNode {
    ExprNode condition;
    StmtNode thenStmt;
    StmtNode elseStmt;

    public IfNode(Location location, ExprNode condition, StmtNode thenStmt,
                  StmtNode elseStmt) {
        super(location);
        this.condition = condition;
        this.thenStmt = thenStmt;
        this.elseStmt = elseStmt;
    }

    @Override
    public void accept(ASTVisitor ASTVisitor)throws CompileError {
        ASTVisitor.visit(this);
    }

    public void dump(int level) {
        dumpHead("IF", level);
        ident(level + 1, "condition:");
        condition.dump(level + 1);
        ident(level + 1, "True  Body:");
        thenStmt.dump(level + 1);
        if (elseStmt != null) {
            ident(level + 1, "False Body:");
            elseStmt.dump(level + 1);
        }
        dumpEnd("IF", level);
    }

    public ExprNode getCondition() {
        return condition;
    }

    public StmtNode getThenStmt() {
        return thenStmt;
    }

    public StmtNode getElseStmt() {
        return elseStmt;
    }
}
