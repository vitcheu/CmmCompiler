package AST.EXPR;

import AST.TYPE.BaseType;
import AST.TYPE.TypeNode;
import CompileException.CompileError;
import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;


public class CharNode extends LiteralNode{
    public CharNode(Location l,String lxrValue){
        super(l);
        literal =lxrValue;
        setType(new TypeNode(BaseType.charType));
    }

    @Override
    public void accept(ASTVisitor ASTVisitor)throws CompileError {
        ASTVisitor.visit(this);
    }
}
