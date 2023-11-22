package AST.EXPR;

import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;import CompileException.*;

public class PostfixOpNode extends UnaryOpNode{
    public PostfixOpNode(Location location, String op, ExprNode expr) {
        super(location, op, expr);
    }

    @Override
    public void accept(ASTVisitor ASTVisitor)throws CompileError {
        ASTVisitor.visit(this);
    }

    public void dump(int level) {
        dumpHead("Unary OP",level);
        ident(level+1,"Postfix Op:"+op);
        ident (level+1,"expression:");
        expr.dump(level+1);
        dumpEnd("Unary OP",level);
    }

//    @Override
//    public boolean isLeftValueExpr() {
//        return true;
//    }
}
