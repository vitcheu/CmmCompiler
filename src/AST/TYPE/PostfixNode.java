package AST.TYPE;

import AST.Node;
import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;

abstract public class PostfixNode extends Node {
    protected PostfixNode nextPostfix=null;
    protected PostfixNode head=null;

    public PostfixNode(Location location) {
        super(location);
    }

    @Override
    public void accept(ASTVisitor ASTVisitor) {
        ASTVisitor.visit(this);
    }

    public PostfixNode(Location location, PostfixNode nextPostfix) {
        super(location);
        this.nextPostfix = nextPostfix;
    }

    public static PostfixNode createPostfixNode(PostfixNode p){
        if(p instanceof PtrPofix){
            return new PtrPofix(null,((PtrPofix) p).getArrayLen());
        }else{
            return ParamPofix.copyParamPostfix((ParamPofix)p);
        }
    }

    public boolean isPtrPostfix(){return false;}


    public void setNextPostfix(PostfixNode nextPostfix) {
        this.nextPostfix = nextPostfix;
    }

    public void setHead(PostfixNode head) {
        this.head = head;
    }

    public PostfixNode getNextPostfix() {
        return nextPostfix;
    }


    public PostfixNode getHead() {
        return head;
    }

    public boolean isArrayPostfix(){
        return false;
    }

    abstract public String getDescription();
}
