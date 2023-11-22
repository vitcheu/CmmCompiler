package AST.STMT;

import AST.EXPR.ExprNode;
import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;import CompileException.*;

public class DoWhileNode extends StmtNode{
    ExprNode condition;
    StmtNode stmt;

    public DoWhileNode(Location location, ExprNode condition, StmtNode stmt) {
        super(location);
        this.condition = condition;
        this.stmt = stmt;
    }

    @Override
    public void accept(ASTVisitor ASTVisitor)throws CompileError {
        ASTVisitor.visit(this);
    }

    public void dump(int level) {
        dumpHead("DoWhile",level);
            ident(level+1,"Stmt:");
            stmt.dump(level+1);
            ident(level+1,"condition:");
            condition.dump(level+1);
        dumpEnd("DoWhile",level);
    }

    public ExprNode getCondition() {
        return condition;
    }

    public StmtNode getStmt() {
        return stmt;
    }

    @Override
    public boolean isControlStmt() {
        return true;
    }
}
