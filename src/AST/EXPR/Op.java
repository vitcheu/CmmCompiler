package AST.EXPR;

import AST.Node;
import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;

public class Op extends Node {
    String op;

    public Op(Location location, String op) {
        super(location);
        this.op = op;
    }

    public String getOp() {
        return op;
    }

    @Override
    public void accept(ASTVisitor ASTVisitor) {

    }
}
