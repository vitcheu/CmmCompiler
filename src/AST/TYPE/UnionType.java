package AST.TYPE;

import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;

public class UnionType extends ComposedType{

    public UnionType(Location location, String id) {
        super(location,id);
        this.type=UNION;
    }

    @Override
    public void accept(ASTVisitor ASTVisitor) {
        ASTVisitor.visit(this);
    }

    @Override
    public String toString() {
        return "UnionType{" +
                "id='" + getId() + '\'' +
                ", location=" + location +
                '}';
    }
}
