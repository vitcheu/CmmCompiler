package AST.DEFINE;

import AST.Node;
import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;

public class StaticNode extends Node {

    public StaticNode(Location location) {
        super(location);
    }

    @Override
    public void accept(ASTVisitor ASTVisitor) {
    }
}
