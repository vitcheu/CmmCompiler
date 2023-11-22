package AST.TYPE;

import AST.DEFINE.definedComposedType;
import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;

import java.util.Objects;

public class ComposedType extends BaseType{
    private String id;
    private definedComposedType typeEntry=null;


    public ComposedType(Location location, String id) {
        super(location);
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void accept(ASTVisitor ASTVisitor) {
        ASTVisitor.visit(this);
    }

    public void setId(String id) {
        this.id = id;
    }

    public definedComposedType getTypeEntry() {
        return typeEntry;
    }

    public void setTypeEntry(definedComposedType typeEntry) {
        this.typeEntry = typeEntry;
    }

    public TypeNode getNthMemberType(int n){
        definedComposedType definedComposedType=getTypeEntry();
        return definedComposedType.getNthMember(n).getType();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ComposedType that = (ComposedType) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, typeEntry);
    }

    public boolean isStruct(){
        return typeEntry.isStruct();
    }

    public int getMemberNum(){
        return typeEntry.getMembers().size();
    }

    @Override
    public String getDescription() {
        return ((this instanceof StructType)?"struct": "union")+ " "+id;
    }
}
