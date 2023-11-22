package AST.DEFINE;

import AST.NodeList.ListNode;
import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;
import CompileException.*;

public class definedStruct extends definedComposedType {

    public definedStruct(Location location, String name, ListNode<SlotNode> members) {
        super(location,name,members);
    }

    @Override
    public void accept(ASTVisitor ASTVisitor)throws CompileError {
        ASTVisitor.visit(this);
    }

    @Override
    public void dump(int level) {
        dumpHead("Struct Definition",level);
            ident(level+1,"Name:"+getTypeName());
            ident(level+1,"Members:");
            members.dump(level+2);
        dumpEnd("Struct Definition",level);
    }

}
