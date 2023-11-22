package AST.TYPE;

import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;

public class PtrPofix extends PostfixNode{
    public final int pointerArrayLen=-1;//表示为指针后缀时arrayLen的值
    private int arrayLen=pointerArrayLen;
    //形如int[n]的类型
    public PtrPofix(Location location, int arrayLen) {
        super(location);
        this.arrayLen = arrayLen;
    }


    //形如int[]和int*的类型
    public PtrPofix(Location location) {
        super(location);
    }

    //复制构造
    public PtrPofix(PtrPofix p){
        super(p.location);
        this.arrayLen=p.arrayLen;
    }

    @Override
    public void accept(ASTVisitor ASTVisitor) {
        ASTVisitor.visit(this);
    }

    public PtrPofix(Location location, PostfixNode nextPostfix, int arrayLen) {
        super(location, nextPostfix);
        this.arrayLen = arrayLen;
    }

    public PtrPofix(){
        super(null);
    }

    public boolean isPtrPostfix(){return true;}
    public PtrPofix(Location location, PostfixNode nextPostfix) {
        super(location, nextPostfix);
    }


    @Override

    public String toString() {
        if(arrayLen==-1)
        return "*";
        else return "["+arrayLen+"]";
    }

    public int getArrayLen() {
        return arrayLen;
    }

    /**
     * 判断是否为数组后缀
     */
    public boolean isArrayPostfix(){
        return arrayLen!=pointerArrayLen;
    }

    @Override
    public String getDescription() {
        return toString();
    }

    /**
     * 转换为指针后缀
     */
    public void shiftToPtrPostfix(){
        arrayLen=pointerArrayLen;
    }

}
