package Semantic_Analysis;

import AST.AST;
import AST.DEFINE.*;
import AST.EXPR.*;
import AST.STMT.*;
import AST.TYPE.*;
import CompileException.*;

/**
 * 访问抽象语法树(AST)各节点的接口
 * 具体访问操作由实现决定
 */
public interface ASTVisitor {


    /**
     *
     *
     *访问程序整体
     *
     *
     */
    void visit(AST node) throws CompileError;

    /**
     *
     *
     *访问表达式
     *
     *
     */

    void visit(AddressNode node) throws CompileError;

    void visit(sizeOfExprNode node) throws CompileError;

//    void visit(LHSNode node);

    void visit(DerefrenceNode node)throws CompileError;

    void visit(VariableNode node)throws CompileError;

    void visit(MemberNode node)throws CompileError;

    void visit(ArrayNode node)throws CompileError;

    void visit(CallNode node)throws CompileError;

    void visit(ArgNode node)throws CompileError;

    void visit(AssignNode node)throws CompileError;

    void visit(OpAssignNode node)throws CompileError;

    void visit(ConditionNode node)throws CompileError;

    void visit(UnaryOpNode node)throws CompileError;

//    void visit(PrefixOpNode node);
//
//    void visit(PostfixOpNode node);

    void visit(LiteralNode node)throws CompileError;

//    void visit(FloatNode node);
//
//    void visit(CharNode node);
//
//    void visit(IntegerNode node);
//
//    void visit(StringNode node);

    void visit(sizeOfTypeNode node)throws CompileError;

    void visit(BinaryOpNode node)throws CompileError;

    void visit(CastNode node)throws CompileError;
    void visit(ExprNode node)throws CompileError;

    /**
     *
     *
     * 访问语句
     *
     *
     */

    void visit(StmtNode node) throws CompileError;

    void visit(SwitchNode node)throws CompileError;

    void visit(JumpNode node)throws CompileError;

    void visit(GotoNode node)throws CompileError;

    void visit(ReturnExprNode node)throws CompileError;

    void visit(LabelNode node)throws CompileError;

    void visit(ExprStmt node)throws CompileError;

    void visit(WhileNode node)throws CompileError;

    void visit(DoWhileNode node)throws CompileError;

    void visit(IfNode node)throws CompileError;

    void visit(ForNode node)throws CompileError;

    void visit(BlockNode node)throws CompileError;

    void visit(ImportNode node)throws CompileError;

    /**
     *
     *
     * 访问定义语句
     *
     *
     */

    void visit(DeclararedNode node)throws CompileError;

//    void visit(DefinedConst node)throws CompileError;

    void visit(DefinedVariable node) throws CompileError;

    void visit(TypedefNode node)throws CompileError;

    void visit(DefinedFunction node)throws CompileError;

    void visit(definedComposedType node) throws CompileError;


    void visit(ParamNode node)throws CompileError;

    void visit(SlotNode node)throws CompileError;


    /**
     *
     *
     * 访问类型
     *
     */

    void visit(TypeNode node)throws CompileError;

    void visit(BaseType node);

    void visit(StructType node);

    void visit(UnionType node);

    void visit(ComposedType node);

    void visit(PostfixNode node);

    void visit(ParamPofix node);

    void visit(PtrPofix node);



}
