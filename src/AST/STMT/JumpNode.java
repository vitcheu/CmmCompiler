package AST.STMT;

import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;
import CompileException.*;
public class JumpNode extends StmtNode{
    int  op;
    public static final int BREAK=0;
    public static final int GOTO=1;
    public static final int CONTINUE=2;
    public static final int RETURN=3;

    public JumpNode(Location location, int op) {
        super(location);
        this.op = op;
    }

    @Override
    public void accept(ASTVisitor ASTVisitor)throws CompileError {
        ASTVisitor.visit(this);
    }

    @Override
    public void dump(int level) {
        String s="";
        if(op==BREAK) s="BREAK";
        if(op==GOTO) s="GOTO";
        if(op==CONTINUE) s="CONTINUE";
        if(op==RETURN) s="RETURN";
        ident(level,s);
    }

    public int getOp() {
        return op;
    }
}
