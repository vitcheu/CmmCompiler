package AST.STMT;

import AST.DEFINE.DefinedVariable;
import AST.Node;
import Semantic_Analysis.ASTVisitor;
import Semantic_Analysis.SymbolTable.LocalTable;
import CompileException.*;

import java.util.List;

public class BlockNode extends StmtNode{
    private List<DefinedVariable> defVars;
    private List<StmtNode> stmts;
    private LocalTable localTable;
    List<Node> blockComponents=null;

    public LocalTable getLocalTable() {
        return localTable;
    }

    public void setLocalTable(LocalTable localTable) {
        this.localTable = localTable;
    }

//    public BlockNode(Location location, ListNode<DefinedVariable> defVars, ListNode<StmtNode> stmts) {
//        super(location);
//        this.defVars = defVars;
//        this.stmts = stmts;
//    }
    public BlockNode(BlockComponents blockComponents){
        super(blockComponents.getLocation());
        defVars= blockComponents.getVariables();
        stmts= blockComponents.getStmts();
        this.blockComponents=blockComponents.getNodes();
    }


    @Override
    public void accept(ASTVisitor ASTVisitor) throws CompileError {
        ASTVisitor.visit(this);
    }

    public void dump(int level) {
        dumpHead("Block",level);
            if(defVars!=null){
            ident(level+1,"Defined Variables:");
            for(DefinedVariable var:defVars){
                var.dump(level+1);
            }
            }
            if(stmts!=null){
            for(StmtNode stmt:stmts){
                stmt.dump(level+1);
            }
            }
        dumpEnd("Block",level);
    }

    public void addVariables(List<DefinedVariable> variables){
        defVars.addAll(variables);
    }

    public List<DefinedVariable> getDefVars() {
        return defVars;
    }


    public List<StmtNode> getStmts() {
        return stmts;
    }

    public List<Node> getBlockComponents() {
        return blockComponents;
    }
}
