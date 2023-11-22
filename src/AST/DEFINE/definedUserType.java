package AST.DEFINE;

import Parser.Entity.Location;

public class definedUserType extends DeclararedNode {
    protected String typeName;

    public definedUserType(Location location, String name) {
        super(location);
        this.typeName=name;
    }

    public String getTypeName() {
        return typeName;
    }

    @Override
    public String toString() {
        return "Defined type:#"+typeName;
    }
}
