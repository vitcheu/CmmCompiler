package AST.EXPR;

import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;import CompileException.*;

public class LiteralNode extends ExprNode{

    protected String literal =null;
    public LiteralNode(Location location) {
        super(location);
    }

    @Override
    public void accept(ASTVisitor ASTVisitor)throws CompileError {
        ASTVisitor.visit(this);
    }

    public String getLiteralValue(){
        return literal;
    }

    @Override
    public String toString() {
        return "$LiteralNode"+getClass().getSimpleName()+",#"+ getLiteralValue();
    }

    @Override
    public void dump(int level) {
        dumpHead("Literal",level);
            ident(level+1,"Literal value:"+getLiteralValue());
            ident(level+1,"type:"+ getType());
        dumpEnd("Literal",level);
    }

    public boolean isString(){
        return false;
    }

    public void setLxrValue(String lxr){
        this.literal =lxr;
    }

    @Override
    public boolean isLiteralExpr() {
        return true;
    }

    @Override
    public boolean isConstExpr() {
        return true;
    }
}
