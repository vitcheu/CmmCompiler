package AST.EXPR;

import AST.Node;
import AST.TYPE.TypeNode;
import IR.Label;
import IR.Value;
import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;
import CompileException.*;


public abstract class ExprNode extends Node {
    protected TypeNode type;
    //存放表达式的值的地址
    private Value value;
    //表达式为真时的跳转地址
    private Label trueLabel;
    //表达式为假时的跳转地址
    private Label falseLabel;

    public ExprNode(Location location) {
        super(location);
    }

    @Override
    public void accept(ASTVisitor ASTVisitor)throws CompileError {
        ASTVisitor.visit(this);
    }

    public TypeNode getType(){return type;}

    public void setType(TypeNode type) {
        this.type = type;
    }

    public boolean isPointer(){return false;}

    public boolean isComposedType(){return false;}

    public boolean isCallable(){return false;}

    /**
     *判断是否为左值
     */
    public boolean isLeftValueExpr(){
        return false;
    }

    /**
     * 判断是否为const变量
     */
    public boolean isConst(){
        return false;
    }

    public Value getValue() {
        return value;
    }

    public void setValue(Value value) {
        this.value = value;
    }

    /**
     * 判断是否是布尔表达式
     */
    public boolean isBoolExpr(){
        return false;
    }

    public Label getTrueLabel() {
        return trueLabel;
    }

    public void setTrueLabel(Label trueLabel) {
        this.trueLabel = trueLabel;
    }

    public Label getFalseLabel() {
        return falseLabel;
    }

    public void setFalseLabel(Label falseLabel) {
        this.falseLabel = falseLabel;
    }

    @Override
    public void dump(int level) {
        dumpWriter.println("@Expr$type="+((type==null)?"null":type.toString()));
    }

    /**
     * 判断是否为常量表达式
     */
    public boolean isConstExpr(){
        return false;
    }

    public boolean isLiteralExpr(){
        return false;
    }

    public boolean isFunction(){
        return (this instanceof VariableNode variable)&&variable.isFunctionVariable();
    }
}
