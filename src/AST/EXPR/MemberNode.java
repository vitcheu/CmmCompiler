package AST.EXPR;

import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;import CompileException.*;

public class MemberNode extends LHSNode{
    ExprNode expr;
    String name;
    String op;

    public MemberNode(Location location,String op, ExprNode expr, String name) {
        super(location);
        this.expr = expr;
        this.name = name;
        this.op=op;
    }

    @Override
    public void accept(ASTVisitor ASTVisitor)throws CompileError {
        ASTVisitor.visit(this);
    }

    @Override
    public void dump(int level) {
       dumpHead("Member op",level);
        expr.dump(level+1);
        ident(level+1,"op:"+op);
        ident(level+1,"name:"+name);
       dumpEnd("Member op",level);
    }

    public ExprNode getExpr() {
        return expr;
    }

    public String getName() {
        return name;
    }

    public String getOp() {
        return op;
    }

    @Override
    public String toString() {
        return "MemberNode{" +
                "location=" + location +
                '}';
    }

    @Override
    public boolean isConstExpr() {
        return expr.isConstExpr();
    }
}
