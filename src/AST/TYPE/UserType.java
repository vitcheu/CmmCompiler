package AST.TYPE;

import Parser.Entity.Location;

/**
 *
 */
public class UserType extends BaseType{
    String typeName;
    public UserType(Location location,String typeName) {
        super(location);
        this.typeName=typeName;
    }

    public String getTypeName() {
        return typeName;
    }

    @Override
    public String toString() {
        return typeName;
    }
}
