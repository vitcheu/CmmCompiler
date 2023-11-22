package AST.DEFINE;

import AST.NodeList.ListNode;
import AST.STMT.BlockNode;
import AST.TYPE.TypeNode;
import Parser.Entity.Location;

import java.util.List;

/**
 *
 */
public class DeclaredFunction extends DefinedFunction{
    public DeclaredFunction(Location location, boolean priv, TypeNode ret, String name, ListNode<ParamNode> params) {
        super(location, priv, ret, name, params);
        setDefined(false);
    }

}
