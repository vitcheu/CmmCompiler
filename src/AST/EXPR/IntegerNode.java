package AST.EXPR;

import AST.TYPE.BaseType;
import AST.TYPE.TypeNode;
import CompileException.CompileError;
import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;

public class IntegerNode extends LiteralNode {
    public IntegerNode(Location location,String lxrValue) {
        super(location);
        this.literal = lxrValue;
        setType(new TypeNode(BaseType.intType));
    }

    @Override
    public void accept(ASTVisitor ASTVisitor)throws CompileError {
        ASTVisitor.visit(this);
    }
}
