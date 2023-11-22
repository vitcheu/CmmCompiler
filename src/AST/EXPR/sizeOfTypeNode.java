package AST.EXPR;

import AST.TYPE.TypeNode;
import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;import CompileException.*;

public class sizeOfTypeNode extends ExprNode {
    private TypeNode type;
    public sizeOfTypeNode(Location location, TypeNode type) {
        super(location);
        this.type=type;
    }

    @Override
    public void accept(ASTVisitor ASTVisitor)throws CompileError {
        ASTVisitor.visit(this);
    }

    public void dump(int level) {
        dumpHead("sizeOfExpr",level);
        type.dump(level+1);
        dumpEnd("sizeOfExpr",level);
    }

    public TypeNode getTypeNode() {
        return type;
    }

    @Override
    public String toString() {
        return "sizeOfTypeNode{" +
                "type=" + type +
                ", location=" + location +
                '}';
    }

    @Override
    public boolean isLiteralExpr() {
        return true;
    }

    @Override
    public boolean isConstExpr() {
        return true;
    }
}
