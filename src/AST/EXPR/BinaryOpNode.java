package AST.EXPR;

import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;
import CompileException.*;

public class BinaryOpNode extends ExprNode{
    private ExprNode left;
    private ExprNode right;
    private   String op;


    public BinaryOpNode(Location location, ExprNode left, ExprNode right, String op) {
        super(location);
        this.left = left;
        this.right = right;
        this.op = op;
    }

    @Override
    public void accept(ASTVisitor ASTVisitor)throws CompileError {
        ASTVisitor.visit(this);
    }

    @Override
    public void dump(int level) {
        dumpHead(" "+op+" ",level);
        ident(level+1,"Left Value:");
                left.dump(level+2);

        ident(level+1,"Right Value:");
                right.dump(level+2);
        dumpEnd(" "+op+" ",level);
    }

    public ExprNode getLeft() {
        return left;
    }

    public ExprNode getRight() {
        return right;
    }

    public String getOp() {
        return op;
    }

    @Override
    public boolean isBoolExpr() {
        return op.equals("&&")||op.equals("||");
    }

    @Override
    public String toString() {
        return "BinaryOpNode{" +
                "op='" + op + '\'' +
                '}';
    }

    /**
     * 判断是否为比较运算
     */
    public boolean isRelOP(){
        switch (op){
            case ">=":
            case "==":
            case "<=":
            case "!=":
            case ">":
            case "<": return true;
            default: return false;
        }
    }

    public void setLeft(ExprNode left) {
        this.left = left;
    }

    public void setRight(ExprNode right) {
        this.right = right;
    }

    @Override
    public boolean isConstExpr() {
        return left.isConstExpr()&&right.isConstExpr();
    }

    @Override
    public boolean isLiteralExpr() {
        return left.isLiteralExpr()&&right.isLiteralExpr();
    }
}
