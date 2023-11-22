package AST.EXPR;

import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;import CompileException.*;

public class OpAssignNode extends AssignNode{
    private String op;

    public OpAssignNode(Location location, ExprNode lhs, ExprNode expr, String op) {
        super(location, lhs, expr);
        this.op = op;
    }

    public String getOp() {
        return op;
    }

    @Override
    public void accept(ASTVisitor ASTVisitor)throws CompileError {
        ASTVisitor.visit(this);
    }

    @Override
    public String toString() {
        return "OpAssignNode{" +
                "op='" + op + '\'' +
                ", location=" + location +
                '}';
    }

    public BinaryOpNode toBinaryOpNode(){
        BinaryOpNode bin= new  BinaryOpNode(getLocation(),lhs,expr,mulOpToOp(op));
        bin.setType(lhs.getType());
        return bin;
    }

    /**
     * 将复合赋值运算符转换为相应的二元运算符
     */
    public static String mulOpToOp(String mulOp){
        return mulOp.substring(0,1);
    }

    @Override
    public boolean isConstExpr() {
        return false;
    }

    @Override
    public boolean isLiteralExpr() {
        return false;
    }
}
