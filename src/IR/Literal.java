package IR;

import ASM.AsmAddress.Address;
import ASM.AsmAddress.DirectAddress;
import IR.Constants.Type;
import IR.Optimise.DagNode;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Objects;

public class Literal implements Value , Address {
    private static HashMap<Literal, DagNode> lastDagNodes=new HashMap<>();
    private String lxrValue;
    private Type type;
    public static  final Literal TRUE=new Literal("1",Type.int8);
    public static final Literal FALSE=new Literal("0",Type.int8);

    /**
     * 浮点数的"0.0"
     */
    public static final Literal ZERO_IN_FLOAT=new Literal("0.0",Type.floatType);//"000000000r"
    public static final Literal ZERO_IN_DOUBLE=new Literal("0.0",Type.doubleType);//"00000000000000000r"


    public Literal(String lxrValue, Type type) {
        this.lxrValue = lxrValue;
        this.type = type;
    }

    //支持更宽的字面量
    public Literal(int i){
        lxrValue=String.valueOf(i);
        type=Type.int32;
    }

    public Literal(String s) {
        lxrValue=s;
        type=Type.StringLiteral;
    }

    public String getLxrValue() {
        return lxrValue;
    }

    public void setLxrValue(String lxrValue) {
        this.lxrValue = lxrValue;
    }

    public Type getIRType() {
        return type;
    }

    @Override
    public boolean isSigned() {
        return (type==Type.int32 ||type==Type.floatLiteral)&&lxrValue.startsWith("-");
    }

    @Override
    public String toString() {
        String quote="\"";
        if(getIRType()==Type.StringLiteral)
            return quote+lxrValue+quote;
         return lxrValue;
    }

    @Override
    public String getOutputStr() {
        if(this.type==Type.StringLiteral)
            return toString().substring(1,toString().length()-1);
        return toString();
    }

    @Override
    public boolean isFloat() {
        return type.isFloatType();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Literal literal = (Literal) o;
        return Objects.equals(lxrValue, literal.lxrValue) && Objects.equals(type, literal.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lxrValue, type);
    }

    @Override
    public boolean isLeftValue() {
        return false;
    }

    /**
     * 构造指定位数的掩码
     */
    public static Literal getMask(int srcWidth, int targetWidth,boolean isZeroExtension){
        StringBuilder lxr=new StringBuilder();
        lxr.append("h");
        for(int i=1;i<=targetWidth;i++){
            //保留原内容
            if(i<=srcWidth)
                lxr.append("ff");
            //高位零扩展或符号扩展
            else
                lxr.append("00");
        }
        char last=lxr.charAt(lxr.length()-1);
        if(last=='f'){
            lxr.append("0");
        }
        lxr.reverse();
        return  new Literal(lxr.toString());
    }

    /**
     * 构造指定位数的"1"
     */
    public static Literal getOne(int width){
        StringBuilder lxr=new StringBuilder();
        for(int i=0;i<width-1;i++){
            lxr.append("00");
        }
        lxr.append("01");
        lxr.append("h");
        return new Literal(lxr.toString());
    }

    public static Literal getZero(Type type){
        return new Literal("0",type);
    }


    @Override
    public void setIRType(Type type) {
         this.type=type;
    }

    public boolean isZero(){
        return getLxrValue().equals("0");
    }

    public boolean isOne(){
        return getLxrValue().equals("1");
    }

    @Override
    public int getWidth() {
        return type.getWidth();
    }

    @Override
    public DagNode getAssignedDag() {
        return lastDagNodes.get(this);
    }

    @Override
    public void setAssignedDag(DagNode node) {
        lastDagNodes.put(this,node);
    }

    public static void clearAllDag(){
        lastDagNodes.clear();
    }
}
