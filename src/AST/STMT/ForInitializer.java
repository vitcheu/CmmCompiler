package AST.STMT;

import AST.DEFINE.DefinedVariable;
import AST.EXPR.ExprNode;
import AST.Node;
import AST.NodeList.ListNode;
import Parser.Entity.Location;
import CompileException.CompileError;
import Semantic_Analysis.ASTVisitor;

import java.util.List;

/**
 *
 */
public class ForInitializer extends Node {
    private boolean isVars=true;
    private List<DefinedVariable> definedVariableList;
    private List<ExprNode> exprNodeList;
    public ForInitializer(Location location, ListNode<DefinedVariable> defVars){
        super(location);
        this.definedVariableList=defVars.getNodeList();
        this.isVars=true;
    }

    public ForInitializer(Location location,ListNode<ExprNode> exprNodeListNode,boolean isVars){
        super(location);
        this.exprNodeList=exprNodeListNode.getNodeList();
        this.isVars=false;
    }

    public boolean isVars() {
        return isVars;
    }

    public List<DefinedVariable> getDefinedVariableList() {
        return definedVariableList;
    }

    public List<ExprNode> getExprNodeList() {
        return exprNodeList;
    }

    @Override
    public void accept(ASTVisitor ASTVisitor) throws CompileError {

    }
}
