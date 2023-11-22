package AST.EXPR;

import AST.TYPE.TypeNode;
import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;
import CompileException.*;

public class CastNode extends ExprNode implements Assignable {
   private   ExprNode expr;
   private TypeNode typeNode;

    public CastNode(Location location, ExprNode expr, TypeNode typeNode) {
        super(location);
        this.expr = expr;
        this.typeNode = typeNode;
        setType(typeNode);
    }

    @Override
    public void accept(ASTVisitor ASTVisitor)throws CompileError {
        ASTVisitor.visit(this);
    }

    @Override
    public void dump(int level) {
        dumpHead("Cast", level);
        expr.dump(level + 1);
        if (typeNode == null) {
            ident(level + 1, "暂无类型信息");
        } else {
            typeNode.dump(level + 1);
        }
        dumpEnd("Cast", level);
    }

    public ExprNode getRightHandSideExpr() {
        return expr;
    }

    @Override
    public TypeNode getTypeOfLeftHandSide() {
        return getType();
    }

    @Override
    public TypeNode getType() {
        return typeNode;
    }

    @Override
    public void setRightHandSideExpr(ExprNode node) {
        //类型转化节点不必转换转换表达式
        return ;
    }


    public TypeNode getTypeNode() {
        return typeNode;
    }

    @Override
    public String toString() {
        return "CastNode{" +
                "type=" + type +
                ", location=" + location +
                '}';
    }

    @Override
    public boolean isLiteralExpr() {
        return expr.isLiteralExpr();
    }

    @Override
    public boolean isConstExpr() {
        return expr.isConstExpr();
    }

    @Override
    public boolean isCastNode() {
        return true;
    }
}
