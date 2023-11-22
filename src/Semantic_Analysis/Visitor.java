package Semantic_Analysis;

import AST.DEFINE.*;
import AST.EXPR.*;
import AST.Node;
import AST.AST;
import AST.STMT.*;
import AST.TYPE.*;
import CompileException.*;

import java.util.List;

/**
 * 各visitor类的基类
 * 子类只需按需重载特定的方法
 */
public class Visitor implements ASTVisitor {
    public void visitNode(Node node) throws CompileError {
//        //("--------------\n访问" + node);
            node.accept(this);
//        //("访问" + node+"结束");
    }

    public void visitStmts(List<StmtNode> nodes) throws CompileError {
        for (Node node : nodes) {
            visitNode(node);
        }
    }

    /**
     * 访问程序整体
     */
    @Override
    public void visit(AST node) throws CompileError {
        for (DeclararedNode def : node.getDefinedNode()) {
            visitNode(def);
        }
    }

    /**
     * 访问表达式
     */

    @Override
    public void visit(AddressNode node) throws CompileError {
        visitNode(node.getExpr());
    }

    @Override
    public void visit(sizeOfExprNode node) throws CompileError {
        visitNode(node.getExpr());
    }


    @Override
    public void visit(DerefrenceNode node) throws CompileError {
        visitNode(node.getExpr());
    }

    @Override
    public void visit(VariableNode node) throws CompileError {
        if (node.getType() != null)
            visitNode(node.getType());
    }

    @Override
    public void visit(MemberNode node) throws CompileError {
        visitNode(node.getExpr());
        if (node.getType() != null)
            visitNode(node.getType());
    }

    @Override
    public void visit(ArrayNode node) throws CompileError {
        visitNode(node.getArray());
        visitNode(node.getIdx());
    }

    @Override
    public void visit(CallNode node) throws CompileError {
        visitNode(node.getExpr());
        visitNode(node.getArgNode());
    }

    @Override
    public void visit(ArgNode node) throws CompileError {
        for (ExprNode expr : node.getArgs()) {
            visitNode(expr);
        }
    }

    @Override
    public void visit(AssignNode node) throws CompileError {
        visitNode(node.getLhs());
        visitNode(node.getRightHandSideExpr());
    }

    @Override
    public void visit(OpAssignNode node) throws CompileError {
        visitNode(node.getLhs());
        visitNode(node.getRightHandSideExpr());
    }

    @Override
    public void visit(ConditionNode node) throws CompileError {
        visitNode(node.getCondition());
        visitNode(node.getExpr1());
        visitNode(node.getExpr2());
    }

    @Override
    public void visit(UnaryOpNode node) throws CompileError {
        visitNode(node.getExpr());
    }


    @Override
    public void visit(LiteralNode node) throws CompileError {
//        visitNode(node);
    }

//    @Override
//    public void visit(FloatNode node) {
//
//    }
//
//    @Override
//    public void visit(CharNode node) {
//
//    }
//
//    @Override
//    public void visit(IntegerNode node) {
//
//    }
//
//    @Override
//    public void visit(StringNode node) {
//
//    }

    @Override
    public void visit(sizeOfTypeNode node) throws CompileError {
        visitNode(node.getTypeNode());
    }

    @Override
    public void visit(BinaryOpNode node) throws CompileError {
        visitNode(node.getLeft());
        visitNode(node.getRight());
    }

    @Override
    public void visit(CastNode node) throws CompileError {
        visitNode(node.getTypeNode());
        visitNode(node.getRightHandSideExpr());
    }

    /**
     * 访问语句
     */
    @Override
    public void visit(StmtNode node) throws CompileError {

    }


    @Override
    public void visit(SwitchNode node) throws CompileError {
        visitNode(node.getCondition());
        for (StmtNode stmtNode : node.getLabelStmts()) {
            visitNode(stmtNode);
        }
    }

    @Override
    public void visit(JumpNode node) {

    }

    @Override
    public void visit(GotoNode node) {

    }

    @Override
    public void visit(ReturnExprNode node) throws CompileError {
        visitNode(node.getRet());
    }

    @Override
    public void visit(LabelNode node) throws CompileError {
        visitNode(node.getStmt());
    }

    @Override
    public void visit(ExprStmt node) throws CompileError {
        visitNode(node.getExpr());
    }

    @Override
    public void visit(WhileNode node) throws CompileError {
        visitNode(node.getCondition());
        if(node.getStmt()!=null)
        visitNode(node.getStmt());
    }

    @Override
    public void visit(DoWhileNode node) throws CompileError {
        if(node.getStmt()!=null)
        visitNode(node.getStmt());
        visitNode(node.getCondition());
    }

    @Override
    public void visit(IfNode node) throws CompileError {
        visitNode(node.getCondition());
        visitNode(node.getThenStmt());
        if(node.getElseStmt()!=null)
        visitNode(node.getElseStmt());
    }

    @Override
    public void visit(ForNode node) throws CompileError {
        if(node.getExpr1()!=null){
           for(ExprNode expr:node.getExpr1()){
               visitNode(expr);
           }
        }
        if(node.getDefinedVariables()!=null){
            for(DefinedVariable var: node.getDefinedVariables()){
                visitNode(var);
            }
        }
        if(node.getExpr2()!=null)
             visitNode(node.getExpr2());
        if(node.getExpr3()!=null){
            for(ExprNode expr: node.getExpr3()){
                visitNode(expr);
            }
        }

        if(node.getStmt()!=null)
            visitNode(node.getStmt());
    }

    @Override
    public void visit(BlockNode node) throws CompileError {
        for(Node n:node.getBlockComponents()){
            visitNode(n);
        }
    }

    @Override
    public void visit(ImportNode node) {
    }

    public void visit(ExprNode node) throws CompileError {
    }

    /**
     * 访问定义语句
     */
    @Override
    public void visit(DeclararedNode node) throws CompileError {

    }

//    @Override
//    public void visit(DefinedConst node) throws CompileError{
//
//    }

    @Override
    public void visit(DefinedVariable node) throws CompileError {
        visitNode(node.getType());
        if (node.getInit() != null)
            visitNode(node.getInit());
        if(node.getIniExprList()!=null){
            if(node.supportListInitialize()){
                for(ExprNode e:node.getIniExprList()){
                    visitNode(e);
                }
            }else{
                visitNode(node.getIniExprList().get(0));
            }
        }
    }

    @Override
    public void visit(TypedefNode node) throws CompileError {
        visitNode(node.getType());
    }

    @Override
    public void visit(DefinedFunction node) throws CompileError {
        visitNode(node.getTypeOfLeftHandSide());
        if (node.getParams() != null) {
            for (ParamNode parm : node.getParams()) {
                visitNode(parm);
            }
        }
        if(node.getBody()!=null){
            visitNode(node.getBody());
        }
    }

    public void visit(definedComposedType node) throws CompileError {
        for (SlotNode slot : node.getMembers()) {
            visitNode(slot);
        }
    }


    @Override
    public void visit(ParamNode node) throws CompileError {
        if(!node.isKwargs())
            visitNode(node.getType());
    }

    @Override
    public void visit(SlotNode node) throws CompileError {
        visitNode(node.getType());
    }

    /**
     * 访问类型
     */
    @Override
    public void visit(TypeNode node) throws CompileError {
        node.getBase().accept(this);
        if (node.getPostfix() != null)
            node.getPostfix().accept(this);
    }

    @Override
    public void visit(BaseType node){

    }

    @Override
    public void visit(StructType node){

    }

    @Override
    public void visit(UnionType node) {

    }

    @Override
    public void visit(ComposedType node) {

    }

    @Override
    public void visit(PostfixNode node) {

    }

    @Override
    public void visit(ParamPofix node) {

    }

    @Override
    public void visit(PtrPofix node) {

    }

}
