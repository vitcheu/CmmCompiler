package AST.TYPE;

import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;

public class StructType extends ComposedType{

    public StructType(Location location, String id) {
        super(location,id);
        this.type=STRUCT;
    }

    @Override
    public void accept(ASTVisitor ASTVisitor) {
        ASTVisitor.visit(this);
    }


    @Override
    public String toString() {
        return "Struct "+ getId();
    }


}
