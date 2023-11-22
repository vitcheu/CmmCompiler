package AST.EXPR;

import AST.TYPE.TypeNode;
import IR.Value;
import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;
import CompileException.*;

public class ArrayNode extends LHSNode{
    public ExprNode idx;
    public ExprNode array;
    private Value offset;
    private ArrayNode parent;
    private Value baseAddr;

    public ArrayNode(Location location, ExprNode idx) {
        super(location);
        this.idx = idx;
    }

    public ArrayNode(Location location, ExprNode array, ExprNode idx) {
        super(location);
        this.array=array;
        if(array instanceof ArrayNode)
        ((ArrayNode)array).parent=this;
        this.idx=idx;
    }

    @Override
    public void accept(ASTVisitor ASTVisitor)throws CompileError {
        ASTVisitor.visit(this);
    }

    @Override
    public void dump(int level) {
        dumpHead("Array",level);
            ident(level+1,"base:");
                array.dump(level+2);
            ident(level+1,"index:");
                idx.dump(level+2);
        dumpEnd("Array",level);
    }

    public ExprNode getIdx() {
        return idx;
    }

    public ExprNode getArray() {
        return array;
    }



    @Override
    public String toString() {
        return "ArrayNode{" +
                "location=" + location +
                '}';
    }

    @Override
    public boolean isPointer() {
        return true;
    }


    /**
     * @return 储存数组的基地址的变量
     */
    public ExprNode getBase(){
        if(array instanceof ArrayNode){
            return ((ArrayNode) array).getBase();
        }else{
           return array;
        }
    }

    public Value getBaseAddr(){
        if(baseAddr!=null){
            return baseAddr;
        }else if(!(array instanceof ArrayNode)){
            //array为一个变量
            return array.getValue();
        }else{
            //向下寻找
            return ((ArrayNode) array).getBaseAddr();
        }
    }

    public void setBaseAddr(Value baseAddr) {
        this.baseAddr = baseAddr;
    }

    /**
     * 获取储存数组的基地址的变量的类型
     */
    public TypeNode getBaseType(){
       return getBase().getType();
    }

    /**
     * 获取idx实际索引的数组的元素类型
     * 如int[2][3]中int[a][b]中的a实际索引的元素类型为int[3]类型,b索引的元素为int类型
     * 但节点被组织成array(a,array(int[2][3],b)的形式
     */
    public TypeNode getElementType(){
        if(array instanceof ArrayNode){
            return ((ArrayNode) array).getElementType().getDerefType();
        }else{
            return array.getType().getDerefType();
        }
    }

    /**
     *返回元素的宽度
     */
    public int getElementWidth(){
        TypeNode elementType=getElementType();
        return elementType.getTotalLen();
    }

    public Value getOffset() {
        return offset;
    }

    public void setOffset(Value offset) {
        this.offset = offset;
    }

    public ArrayNode getParent() {
        return parent;
    }

    public void setParent(ArrayNode parent) {
        this.parent = parent;
    }
}
