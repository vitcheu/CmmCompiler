package AST.STMT;

import AST.EXPR.Assignable;
import AST.EXPR.ExprNode;
import AST.TYPE.TypeNode;
import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;import CompileException.*;

public class ReturnExprNode extends JumpNode implements Assignable {
    private  ExprNode ret;
    private boolean  lastRet=false;


    public ReturnExprNode(Location location,  ExprNode ret) {
        super(location, JumpNode.RETURN);
        this.ret = ret;
    }

    @Override
    public void accept(ASTVisitor ASTVisitor)throws CompileError {
        ASTVisitor.visit(this);
    }

    @Override
    public void dump(int level) {
        dumpHead("Return",level);
            ident(level+1,"expr:");
            ret.dump(level+1);
        dumpEnd("Return",level);
    }

    public ExprNode getRet() {
        return ret;
    }

    @Override
    public ExprNode getRightHandSideExpr() {
        return ret;
    }

    @Override
    public void setRightHandSideExpr(ExprNode node) {
        this.ret=node;
    }

    @Override
    public TypeNode getTypeOfLeftHandSide() {
        return null;
    }

    public boolean isLastRet() {
        return lastRet;
    }

    public void setLastRet(boolean lastRet) {
        this.lastRet = lastRet;
    }
}
