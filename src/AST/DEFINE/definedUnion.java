package AST.DEFINE;

import AST.NodeList.ListNode;
import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;
import CompileException.*;

public class definedUnion extends definedComposedType {
    public definedUnion(Location location, String name, ListNode<SlotNode> members) {
        super(location,name,members);
    }

    @Override
    public void accept(ASTVisitor ASTVisitor)throws CompileError {
        ASTVisitor.visit(this);
    }

    @Override
    public void dump(int level) {
        dumpHead("Union Definition",level+1);
        ident(level+1,"Name:"+getTypeName());
        ident(level+1,"Members:");
        members.dump(level+1);
        dumpEnd("Union Definition",level+1);
    }
}
