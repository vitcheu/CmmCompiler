package AST.STMT;

import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;
import CompileException.*;

public class GotoNode extends JumpNode{
    String id;

    public GotoNode(Location location, String id) {
        super(location, JumpNode.GOTO);
        this.id = id;
    }

    @Override
    public void accept(ASTVisitor ASTVisitor)throws CompileError {
        ASTVisitor.visit(this);
    }

    @Override
    public void dump(int level) {
        ident(level,"Goto "+id);
    }

    public String getId() {
        return id;
    }
}
