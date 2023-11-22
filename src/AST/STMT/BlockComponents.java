package AST.STMT;

import AST.DEFINE.DefinedVariable;
import AST.Node;
import AST.NodeList.ListNode;
import Parser.Entity.Location;
import CompileException.CompileError;
import Semantic_Analysis.ASTVisitor;

import java.util.LinkedList;
import java.util.List;

/**
 *
 */
public class BlockComponents extends Node {
    private LinkedList<StmtNode> stmts=new LinkedList<>();
    private LinkedList<DefinedVariable> variables=new LinkedList<>();
    private List<Node> nodes=new LinkedList<>();
    public BlockComponents(Location location){
        super(location);
    }

    public LinkedList<StmtNode> getStmts() {
        return stmts;
    }

    public LinkedList<DefinedVariable> getVariables() {
        return variables;
    }

    public void addStmt(StmtNode stmtNode){
        stmts.add(stmtNode);
    }

    private void addVar(DefinedVariable variable){
        variables.add(variable);
    }

    public void addNode(Node node){
        if(node==null){
            return;
        }
        if(node.getLocation()!=null&&this.location==null){
            this.location=node.getLocation();
        }
        if(node instanceof StmtNode){
            addStmt((StmtNode) node);
            nodes.add(node);
        }else if(node  instanceof DefinedVariable){
            addVar((DefinedVariable) node);
            nodes.add(node);
        }else if(node instanceof ListNode) {
            ListNode<DefinedVariable> listNode=(ListNode<DefinedVariable>) node;
            for(DefinedVariable var:listNode.getNodeList()){
                addVar(var);
                nodes.add(var);
            }
        } else{
            throw new RuntimeException("不支持的类型对象:"+node+","+node.getLocation());
        }
    }

    public List<Node> getNodes() {
        return nodes;
    }

    @Override
    public void accept(ASTVisitor ASTVisitor) throws CompileError {

    }
}
