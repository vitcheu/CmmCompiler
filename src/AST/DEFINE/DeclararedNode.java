package AST.DEFINE;

import AST.Node;
import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;
import CompileException.*;
public class DeclararedNode extends Node {
    private boolean defined=true;
    public DeclararedNode(Location location) {
        super(location);
    }

    @Override
    public void accept(ASTVisitor ASTVisitor)throws CompileError {
        ASTVisitor.visit(this);
    }

    public boolean isDefined(){
        return defined;
    }

    public void setDefined(boolean defined){
        this.defined=defined;
    }

    @Override
    public String toString() {
        return "DeclararedNode{" +
                "defined=" + defined +
                ", location=" + location +
                '}';
    }
}
