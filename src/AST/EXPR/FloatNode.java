package AST.EXPR;

import AST.TYPE.BaseType;
import AST.TYPE.TypeNode;
import CompileException.CompileError;
import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;

public class FloatNode extends LiteralNode{
    public FloatNode(Location l,String lxrv){
        super(l);
        literal= lxrv;
        setType(new TypeNode( BaseType.floatType));
    }

    @Override
    public void accept(ASTVisitor ASTVisitor)throws CompileError {
        ASTVisitor.visit(this);
    }
}
