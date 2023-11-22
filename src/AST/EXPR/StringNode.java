package AST.EXPR;

import AST.TYPE.BaseType;
import AST.TYPE.TypeNode;
import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;import CompileException.*;

public class StringNode extends LiteralNode {
    public StringNode(Location l, String lxrValue){
        super(l);
        literal=lxrValue;
        setType(new TypeNode(BaseType.stringType));
    }


    @Override
    public void accept(ASTVisitor ASTVisitor)throws CompileError {
        ASTVisitor.visit(this);
    }

    @Override
    public boolean isString() {
        return super.isString();
    }
}
