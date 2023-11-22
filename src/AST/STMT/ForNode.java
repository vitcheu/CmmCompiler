package AST.STMT;

import AST.DEFINE.DefinedVariable;
import AST.EXPR.ExprNode;
import AST.NodeList.ListNode;
import Semantic_Analysis.ASTVisitor;
import CompileException.*;
import Semantic_Analysis.SymbolTable.LocalTable;

import java.util.List;

public class ForNode extends StmtNode{
    private List<DefinedVariable> definedVariables;
    private List<ExprNode> expr1;
    private ExprNode expr2;
    private  List<ExprNode> expr3;
    private StmtNode stmt;
    private LocalTable table=null;

//    public ForNode(Location location, List<ExprNode> expr1, ExprNode expr2, ExprNode expr3, StmtNode stmt) {
//        super(location);
//        this.expr1 = expr1;
//        this.expr2 = expr2;
//        this.expr3 = expr3;
//        this.stmt = stmt;
//    }

    public ForNode(ForInitializer initializer, ExprNode expr2, ListNode<ExprNode> expr3,StmtNode stmt ){
        super(initializer.getLocation());
        if(initializer.isVars()){
            definedVariables=initializer.getDefinedVariableList();
        }else{
            expr1=initializer.getExprNodeList();
        }

        this.expr2=expr2;
        this.expr3=expr3.getNodeList();
        this.stmt=stmt;
    }

    @Override
    public void accept(ASTVisitor ASTVisitor)throws CompileError {
        ASTVisitor.visit(this);
    }

    public void dump(int level) {
        dumpHead("For",level);
            ident(level+1,"Init Expr:");
                if(expr1!=null){
                    for(ExprNode expr:expr1){
                        expr.dump(level+2);
                    }
                }
                if(definedVariables!=null){
                    for(DefinedVariable definedVariable:definedVariables){
                        definedVariable.dump(level+2);
                    }
                }

            ident(level+1,"Condition Expr:");
                if(expr2!=null)
                    expr2.dump(level+2);
            ident(level+1,"End Expr:");
                if(expr3!=null){
                    for(ExprNode expr:expr3){
                        expr.dump(level+2);
                    }
                }
            ident(level+1,"Stmt:");
                stmt.dump(level+2);
        dumpEnd("For",level);
    }

    public List<ExprNode> getExpr1() {
        return expr1;
    }

    public List<DefinedVariable> getDefinedVariables() {
        return definedVariables;
    }

    public ExprNode getExpr2() {
        return expr2;
    }

    public List<ExprNode> getExpr3() {
        return expr3;
    }

    public StmtNode getStmt() {
        return stmt;
    }

    @Override
    public boolean isControlStmt() {
        return true;
    }

    public LocalTable getTable() {
        return table;
    }

    public void setTable(LocalTable table) {
        this.table = table;
    }
}
