package AST.EXPR;

import AST.Node;
import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;

public class NameNode extends Node {
    String name;

    public NameNode(Location location, String name) {
        super(location);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public void accept(ASTVisitor ASTVisitor) {

    }
}
