package AST.STMT;

import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;import CompileException.*;

public class LabelNode extends StmtNode{
    int  type;
    public static  final int GOTO=0;
    public static final int SWITCH=1;

    String label;
    StmtNode stmt;

    public LabelNode(Location location, int type, String label,StmtNode stmtNode) {
        super(location);
        this.type = type;
        this.label = label;
        this.stmt=stmtNode;
    }

    @Override
    public void accept(ASTVisitor ASTVisitor)throws CompileError {
        ASTVisitor.visit(this);
    }

    @Override
    public void dump(int level) {
        dumpHead("Label Stmt",level);
            ident(level+1,"label:"+label);
            ident(level+1,"stmt:");
            stmt.dump(level+1);
        dumpEnd("Label Stmt",level);
    }

    public int getType() {
        return type;
    }

    public String getLabel() {
        return label;
    }

    public StmtNode getStmt() {
        return stmt;
    }
}
