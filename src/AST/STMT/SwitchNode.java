package AST.STMT;

import AST.EXPR.ExprNode;
import AST.NodeList.ListNode;
import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;

import java.util.List;
import CompileException.*;
public class SwitchNode extends StmtNode{
    ExprNode condition;
    ListNode<LabelNode> labelStmts;

    public SwitchNode(Location location, ExprNode condition, ListNode<LabelNode> labelStmts) {
        super(location);
        this.condition = condition;
        this.labelStmts = labelStmts;
    }

    @Override
    public void accept(ASTVisitor ASTVisitor)throws CompileError {
        ASTVisitor.visit(this);
    }

    @Override
    public void dump(int level) {
        dumpHead("Switch",level);
            ident(level+1,"condition:");
            condition.dump(level+1);
            ident(level+1,"cases:");
            labelStmts.dump(level+1);
        dumpEnd("Switch",level);
    }

    public ExprNode getCondition() {
        return condition;
    }

    public List<LabelNode> getLabelStmts() {
        return labelStmts.getNodeList();
    }

    @Override
    public boolean isControlStmt() {
        return true;
    }
}