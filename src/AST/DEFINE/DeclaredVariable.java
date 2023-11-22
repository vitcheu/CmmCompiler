package AST.DEFINE;

import AST.TYPE.TypeNode;
import Parser.Entity.Location;

/**
 *
 */
public class DeclaredVariable extends DefinedVariable{
    //是否被引用过
    private boolean referenced=false;

    public DeclaredVariable(Location location, boolean priv, TypeNode type, String name) {
        super(location, priv, type, name,null);
        setDefined(false);
    }

    public DeclaredVariable(Location location, String name) {
        super(location, name,null);
        setDefined(false);
    }
}
