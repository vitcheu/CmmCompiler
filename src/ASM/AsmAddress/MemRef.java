package ASM.AsmAddress;

import ASM.Register.Register;
import IR.Constants.Type;
import IR.Constants.TypeWidth;
import IR.Value;
import IR.Var;

import java.util.*;

import static ASM.Register.Register.RBP;
import static ASM.Register.Register.RSP;

public class MemRef implements MemoryAddress {
    //偏移量
    protected int offset;
    //基地址
    protected List<Register> base=new LinkedList<>();
    protected Var var=null;
    protected Type type;
    //判断是否作为数组基址
    protected boolean leftValue =false;
    //所储存的字面量值
    protected String lxrValue=null;
    private MemRefAddress memRefAddress =null;
    /**
     * 保存基于此内存引用创建出来的其他地址,使得在此内存引用变化时(如更改基址寄存器,更改偏移量),其他地址也能随之变化.
     */
    private HashSet<MemRefAddress> createdMemAddr=null;

    public MemRef(){}

    public MemRef(int offset, Register base,Type type) {
        this.offset = offset;
        this.base.add(base);
        this.type=type;
    }

    public MemRef(int offset, Register base,Var var) {
        this.offset = offset;
        this.base.add(base);
        this.var=var;
        this.type=var.getIRType();
        base.addMemref(this);
    }

    /**
     * 返回以寄存器r为基地址的内存访问方式
     * 此函数用于形成以base寄存器所储存的地址为基址的内存访问方式
     */
    public static MemRef getMemRefBaseOnR(Register base,Var var){
        MemRef memRef=  new MemRef(0,base,var);
        memRef.setLeftValue(true);
        return memRef;
    }

    /**
     * 用于生成对该内存引用进行取地址操作时得到的地址
     */
    protected MemoryAddress getLeaAddress(){
        MemoryAddress addr=new MemRefAddress(this);
        addr.setLeftValue(true);
        return addr;
    }

    /**
     *基址寄存器是否为RBP或RSP
     */
    protected boolean isBasedOnStackAddr(){
        return getBase()==RSP|| getBase()==RBP;
    }

    public MemoryAddress getAddressAdditionAddr(int bias){
        MemoryAddress addr= new MemRefAddress(this,bias);
        return addr;
    }

    public MemoryAddress getAddressOpResult(){
        if(isLeftValue()){
            return this;
        }else{
            return getLeaAddress();
        }
    }

    protected void  addMemrefAddr(MemRefAddress address){
        if(createdMemAddr==null){
            createdMemAddr=new HashSet<>();
        }
        createdMemAddr.add(address);
    }

    @Override
    public MemoryAddress getDerefAddr(Var result) {
        return this ;
    }

    /**
     * @return 内存访问的前缀,如byte ptr
     */
    protected String  getPrefix(){
        String prefix;
        prefix=type.getDeclareStr();
        prefix+=" ptr";
        return prefix;
    }


    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
        toMemAddress().setOffset(offset);
        setOffsetOfCreatedAddrs(offset);
    }

    protected void setOffsetOfCreatedAddrs(int offset){
        if(createdMemAddr!=null){
            for(MemRefAddress addr:createdMemAddr){
                addr.setOffset(offset);
            }
        }
    }

    public Register getBase() {
        if(base.isEmpty())return null;
        return base.get(0);
    }

    public void setBase(Register base) {
        if(!this.base.isEmpty()){
            this.base.clear();
            this.base.add(base);
        }
    }

    /**
     *更改相对内存访问的基址基址寄存器
     */
    protected void changeBase(Register base){
        setBase(base);
        MemRefAddress address=toMemAddress();
        address.setBase(base);
        changeCreatedMemBase(base);
    }

    protected void changeCreatedMemBase(Register base){
        if(createdMemAddr!=null){
            for(MemRefAddress addr:createdMemAddr){
                addr.setBase(base);
            }
        }
    }

    public void addBase(Register r){
        if(!base.contains(r)){
            base.add(r);
        }
    }

    /**
     *将以rbp为基址的间接寻址更换为以rsp为基址
     */
    public void shiftToRspBase(int rspDifferFromRbp){
        if(getBase()==RSP)
            return;
        setOffset(offset+rspDifferFromRbp);
        changeBase(RSP);
    }

    public void removeBase(Register r){
        base.remove(r);
    }

    public Var getVar() {
        return var;
    }

    @Override
    public String toString() {
       return getDescription(true);
    }

    protected String getDescription(boolean needPrefix){
        String p=(needPrefix)?getPrefix():"";
        String offsetDescription=getOffsetDescription();

        if (base != null) {
            String s = "";
            if (lxrValue != null) {
                s = " (" + lxrValue + ")";
            } else if (var != null && !var.isLeftValue()) {
                s = " (" + var + ")";
            }
            //形如    byte ptr arr$[rsp+32]   ;
            return p + getPrefixOffsetDescription()
                    + "[" + getBaseDescription() + offsetDescription + "]";
        } else return "";
    }

    /**
     * 判断是否有形如arr$[rsp]中的$arr的地址修饰符
     */
    protected boolean hasPosDecorator(){
        return var!=null&&var.getPosDecorator()!=null;
    }

    protected String  getPrefixOffsetDescription(){
        if(hasPosDecorator()
                &&isBasedOnStackAddr()
        ) {
            return " "+var.getPosDecorator();
        }else{
            return "";
        }
    }

    protected String getOffsetDescription(){
        String offsetDescription;
        if(hasPosDecorator()){
            offsetDescription="";
        }else{
            offsetDescription =getOffsetDescription(this.offset);
        }

        return offsetDescription;
    }

    protected   String getOffsetDescription(int offset){
        if(offset==0)
            return "";
        else if(offset<0){
            return ""+offset;
        }else{
            return "+"+offset;
        }
    }

    protected String getBaseDescription(){
        Register r=getBase();
        return (r==null)?"":r.getName();
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MemRef memRef = (MemRef) o;
        return offset == memRef.offset && Objects.equals(base, memRef.base);
    }

    public boolean totallyEquals(Object o){
        if(!this.equals(o)) return false;
        MemRef mem=(MemRef) o;
        return this.getVar().equals(mem.getVar());
    }

    @Override
    public int hashCode() {
        return Objects.hash(offset, base);
    }

    public void setVar(Var var) {
        this.var = var;
    }

    /**
     * 由于在翻译的过程中保存了临时变量或非易失性寄存器,
     * 局部变量和保存到栈中的易失性寄存器的内存位置需要调整,
     * 已经用该内存位置生成的汇编指令也需要修改
     */
    public void addOffset(int n){
        offset-=n;
        MemRefAddress address=toMemAddress();
        address.addOffset(n);
    }

    @Override
    public boolean isFloat() {
       return type.isFloatType();
    }

    public Type getIRType() {
        return type;
    }

    public void setIRType(Type type) {
        this.type = type;
    }

    public String getLxrValue() {
        return lxrValue;
    }

    public void setLxrValue(String lxrValue) {
        this.lxrValue = lxrValue;
    }

    public boolean  isActive(){
        /*var已经不在寄存器中但它的值仍需要使用*/
        return  var!=null&&(var.isStillUsed())&&(var.getRegisters().isEmpty());
    }
    public MemRefAddress toMemAddress(){
        if(memRefAddress ==null){
            memRefAddress =new MemRefAddress(this);
        }

        return memRefAddress;
    }

    public MemRefAddress getMemAddress() {
        return memRefAddress;
    }

    public List<Register> getBaseList(){
        return base;
    }

    public boolean isLeftValue() {
        return leftValue;
    }

    public void setLeftValue(boolean leftValue) {
        this.leftValue = leftValue;
    }

    @Override
    public boolean isDoublePrecision() {
        return this.type.isDoublePrecision();
    }

    @Override
    public boolean isSinglePrecision() {
        return this.type.isSinglePrecision();
    }

    @Override
    public boolean isIndex() {
       return var!=null&&var.isIndex();
    }

    @Override
    public List<Register> getDependentRegs() {
       Register base=getBase();
       if(base!=null&& base.isGeneral())
           return Arrays.asList(base);
       return null;
    }
}
