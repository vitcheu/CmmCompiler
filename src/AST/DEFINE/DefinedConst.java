package AST.DEFINE;

import AST.EXPR.ExprNode;
import AST.TYPE.TypeNode;
import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;
import CompileException.*;
public class DefinedConst extends DefinedVariable {

    public DefinedConst(Location location, TypeNode type, String id, ExprNode expr) {
        super(location,true,type,id,expr);
        init=expr;
        isConst=true;
    }

    @Override
    public void accept(ASTVisitor ASTVisitor)throws CompileError {
        ASTVisitor.visit(this);
    }

    @Override
    public String toString() {
        return "DefConst@" +name+"#"+type+location;
    }

    @Override
    public void dump(int level) {
        dumpHead("CONST",level);
            ident(level+1,"Type:"+type);
            ident(level+1,"Name:"+name);
            ident(level+1,"Init Expr:");
                init.dump(level+2);
        dumpEnd("CONSt",level);
    }


}
