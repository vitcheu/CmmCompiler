package AST.STMT;

import AST.Node;
import IR.Label;
import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;
import CompileException.*;
public class StmtNode extends Node {
    //语句所生成的中间代码的下一条中间代码标签
    private Label next;
    public StmtNode(Location location) {
        super(location);
    }

    @Override
    public void accept(ASTVisitor ASTVisitor) throws CompileError {
        ASTVisitor.visit(this);
    }

    public Label getNext() {
        return next;
    }

    public void setNext(Label next) {
        this.next = next;
    }

    /**
     * 判断是否为为控制语句
     */
    public boolean isControlStmt(){
        return (this instanceof IfNode)||(this instanceof ForNode) ||
                (this  instanceof WhileNode) || (this instanceof DoWhileNode);
    }
}
