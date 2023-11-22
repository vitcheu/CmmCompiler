package ASM.AsmAddress;

import ASM.Register.Register;
import AST.DEFINE.DefinedVariable;
import IR.Constants.Type;
import IR.Literal;
import IR.Var;

public class DirectAddress implements MemoryAddress {
    public static final int defaultOffset=-1;
    protected String symbolName;
    protected  Type type;
    protected   Var var=null;
    //判断是否充当左值,即代表一个地址还是代表该地址的值
    protected boolean leftValue=true;

    private Register indexReg=null;
    private String registerIndexDescriptor=null;
    private boolean canBeArrayBase=true;

    /**
     * 内存的类型是否与定义时相悖
     */
    protected  boolean typeRedefined=false;

    int offset=defaultOffset;

    public static DirectAddress  ZERO_IN_FLOAT= createDirectAddress("real4@ZERO",Type.floatLiteral);
    public static DirectAddress  ZERO_IN_DOUBLE= createDirectAddress("real8@ZERO",Type.doubleLiteral);

    /**
     * 构造静态或全局变量地址
     */
    public DirectAddress(DefinedVariable var, String name) {
        this.symbolName=name;
        this.var=var;
        this.type=var.getIRType();
        setLeftValue(var.getType().isArray());
    }

    /**
     * 构造字面量地址
     */
    private DirectAddress(String lxrValue, Type type){
        this.symbolName=lxrValue;
        this.type=type;
        setLeftValue(type==Type.StringLiteral);
    }

    /**
     * 构造字符串字面量或函数名地址
     */
    public DirectAddress(String lxr){
       this.symbolName=lxr;
       this.type=Type.StringLiteral;
        setLeftValue(true);
    }

    /**
     * 直接地址加偏移量的内存访问方式
     */
    public DirectAddress(DirectAddress base,int offset,boolean leftValue){
        this.symbolName=base.symbolName;
        this.type=base.type;
        this.offset=offset;
        this.leftValue=leftValue;
        typeRedefined=true;
    }

    public DirectAddress(DirectAddress base, Register indexReg,int scale,Type type){
        this.symbolName=base.symbolName;
        this.type=type;
        typeRedefined=true;
        this.offset=0;
        this.indexReg=indexReg;
        this.registerIndexDescriptor=indexReg.toString()+"*"+scale;
    }

    /**
     *用于对一个直接地址进行取地址操作
     */
    public DirectAddress(DirectAddress direct){
        this.symbolName=direct.symbolName;
        this.type=Type.pointer;
        this.offset=direct.offset;
        this.leftValue=true;
        typeRedefined=true;
    }

    /**
     * 对一个地址重新解释为其他类型
     */
    public DirectAddress(DirectAddress base,Type type,boolean leftValue){
        this.symbolName=base.symbolName;
        this.type=type;
        this.offset=base.offset;
        typeRedefined=true;
        this.leftValue=leftValue;
    }

    public static DirectAddress createDirectAddress(String lxrValue, Type type) {
        return new DirectAddress(lxrValue, type);
    }

    public static DirectAddress createDirectAddress(DirectAddress address,Type type) {
        return new DirectAddress(address.symbolName+"1",type);
    }

    public MemoryAddress getAddressOpResult(){
        if(isLeftValue()){
            return this;
        }else{
            return new DirectAddress(this);
        }
    }

    public static MemoryAddress getAddressAdditionAddr(DirectAddress base, Address index, int scale, Type type){
        if(index instanceof Register r){
            return new DirectAddress(base,r,scale,type);
        }else{
            Literal literal=(Literal) index;
            return base.getAddressAdditionAddr(Integer.parseInt(literal.getLxrValue()));
        }
    }

    @Override
    public boolean isLeftValue() {
       return leftValue;
    }

    @Override
    public Var getVar() {
        return null;
    }

    @Override
    public Type getIRType() {
        return type;
    }

    @Override
    public MemoryAddress getAddressAdditionAddr(int bias) {
        return new DirectAddress(this, bias, true);
    }

    public void setIRType(Type type) {
        if(!typeRedefined&&this.type!=type){
            typeRedefined=true;
        }
        this.type = type;
    }

    public boolean isStringAddr(){
       return type.equals(Type.StringLiteral);
    }

    @Override
    public boolean isFloat() {
        return type.isFloatType();
    }

    @Override
    public boolean isSinglePrecision() {
        return type.isSinglePrecision();
    }

    @Override
    public boolean isDoublePrecision() {
       return type.isDoublePrecision();
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    @Override
    public String toString() {
        if((offset==defaultOffset)){
            return getVarDeclaration()+symbolName;
        }else{
            return getVarDeclaration()+ "["+symbolName+getOffsetDescription()+"]";
        }
    }

    public String  getOffsetDescription(){
        String s="";
        if(registerIndexDescriptor!=null){
            s="+"+registerIndexDescriptor;
        }
        if(offset!=0){
            s+="+"+offset;
        }
        return s;
    }

    public String getVarDeclaration(){
        if(!typeRedefined){
            return "";
        }

        if(var!=null&& var.isArrayAddr()) return "byte ptr ";
        else{
            return type.getDeclareStr()+" ptr ";
        }
    }

    @Override
    public MemoryAddress getDerefAddr(Var result) {
        return new DirectAddress(this, result.getIRType(),false);
    }

    public boolean isTypeRedefined() {
        return typeRedefined;
    }


    public void setLeftValue(boolean leftValue) {
        this.leftValue = leftValue;
    }

    /**
     * 获取浮点字面量的声明语句
     * @param lxr 浮点字面量字符串形式的值
     */
    public String getDeclaration(String lxr){
        if(!this.type.isFloatType()||isLeftValue()||var!=null||isTypeRedefined()){
            throw new RuntimeException("should not happen: "+this);
        }
        return String.format("%s\t%s\t%s",this,type.getDeclareStr(),lxr);
    }
}
