package AST.EXPR;

import AST.TYPE.TypeNode;

public interface Assignable {
     ExprNode getRightHandSideExpr();

     /**
      * 设置赋值运算右侧的表达式
      */
     void setRightHandSideExpr(ExprNode node);

     default boolean isCastNode(){
          return false;
     }

     TypeNode getTypeOfLeftHandSide();
}
