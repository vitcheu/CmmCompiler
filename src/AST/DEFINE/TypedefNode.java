package AST.DEFINE;

import AST.TYPE.TypeNode;
import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;
import CompileException.*;

public class TypedefNode extends definedUserType {
    private TypeNode type;

    public TypedefNode(Location location, TypeNode type, String typeName) {
        super(location,typeName);
        this.type = type;
    }

    @Override
    public void accept(ASTVisitor ASTVisitor)throws CompileError {
        ASTVisitor.visit(this);
    }

    @Override
    public void dump(int level) {
       dumpHead("Typedef",level);
         ident(level+1,"Type Name:"+typeName);
         ident(level+1,"Type:");
            type.dump(level+2);
       dumpHead("Typedef",level);
    }

    public TypeNode getType() {
        return type;
    }

}
