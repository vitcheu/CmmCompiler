package IR;

import ASM.AsmAddress.Address;
import ASM.AsmAddress.MemRef;
import ASM.AsmAddress.MemRefAddress;
import ASM.AsmAddress.MemoryAddress;
import ASM.CodeGenerator;
import ASM.Register.Register;
import AST.DEFINE.DefinedVariable;
import AST.TYPE.TypeNode;
import IR.Constants.Type;
import IR.instruction.Instruction;

public class Temp extends DefinedVariable {
    private static  int tid=0;
    private int id;
    private boolean leftValue;
    private Type irType=null;
    //如果为一个指针变量,该字段保存所指向的地址,即该变量的右值
    private MemoryAddress pointingAddr=null;
    private boolean needWriteBack =false;
    //表示t的值是否是一个数组地址
    private boolean arrayAddr=false;
    private boolean isFunctionPointer =false;
    private boolean index=false;
    private Var dependency=null;
    /*是否在基本块出口处活跃*/
    private boolean activeOnExit=false;
    private int scale;

    private Instruction instruction;

    public Temp(){
        super(null,"t"+tid,null);
        this.id=tid++;
        active=false;
    }

    public Temp(Instruction instruction){
        super(null,"t"+tid,null);
        this.id=tid++;
        this.instruction=instruction;
        active=false;
    }

    public Temp(TypeNode type){
        super(null,"t"+tid,null);
        super.setType(type);
        setArrayAddr(type.isArrayType());
        this.id=tid++;
        active=false;
    }

    public Temp(Type type){
        super(null,"t"+tid,null);
        this.id=tid++;
        this.irType=type;
    }

    @Override
    public boolean isFunctionPointer() {
        return isFunctionPointer;
    }

    public void setFunctionPointer(boolean functionPointer) {
        isFunctionPointer = functionPointer;
    }

    @Override
    public Type getIRType() {
        if(irType!=null)
            return irType;
        return super.getIRType();
    }

    public void setIRType(Type type){
        this.irType=type;
    }

    public int getId() {
        return id;
    }

    public Instruction getInstruction() {
        return instruction;
    }

    public void setInstruction(Instruction instruction) {
        this.instruction = instruction;
    }

    @Override
    public String toString() {
        return "t"+id;
    }

    /**
     *当该临时变量表示一个左值且值已经改变,
     * 或者正在传参但不是当前函数的参数时,需要写回内存
     */
    @Override
    public boolean needStore() {
        return needWriteBack||(activeOnExit&&!addrDecorator.contains(memoryAddress))
                ||
                ((getMemRef() == null) && isStillUsed()
                        && passingValueAsArg
                        && !CodeGenerator.isInCurParamList(this));
    }

    @Override
    public boolean isLeftValue() {
        return leftValue;
    }

    @Override
    public boolean isTemp() {
        return true;
    }

    @Override
    public boolean isIndex() {
        return index;
    }

    public void setIndex(boolean index) {
        this.index = index;
    }

    public int getScale() {
        return scale;
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    public void setLeftValue(boolean leftValue) {
        this.leftValue = leftValue;
    }

    /**
     * 获取该临时变量的内存访问方式
     * 当临时变量的值需要写回内存时,不能再通过此内存访问方式访问该变量,此时返回null
     */
    public MemoryAddress getPointingAddr() {
        if(needWriteBack)
           return   null;
        else{
            return pointingAddr;
        }
    }

    public void setPointingAddr(MemoryAddress pointingAddr) {
        this.pointingAddr = pointingAddr;
    }

    /**
     * 此方法用于给该临时变量分配唯一的地址,
     * 一般用于两处:1.    给变量分配初始的内存地址;
     *            2.    此临时变量充当某计算的结果,使得只有address的值才是其正确值
     * 当address不同于本身的内存位置时,需要将该值写回内存
     */
    @Override
    public void setUniqueAddress(Address address) {
        //清空内存访问方式
        MemoryAddress mem=pointingAddr;
        if (mem != null && !mem.equals(address)&&this.isLeftValue()) {
            if(mem instanceof MemRef memRef &&!(mem instanceof MemRefAddress)){
                //删除与此内存访问间的关系
                Register base=memRef.getBase();
                base.removeMemRef(memRef);
            }
            //此时该临时变量表示一个内存地址的值(因为pointingAddr!=null)
            //当该值作为运算结果而 变化 时,需要将该变化写回内存
            setNeedWriteBack(true);
        }

        if(address instanceof Register){
            super.setUniqueAddress(address);
        }else if(address instanceof MemoryAddress){
            setPointingAddr((MemoryAddress) address);
        }else {
            throw new RuntimeException();
        }
    }

    public void setNeedWriteBack(boolean needWriteBack) {
        this.needWriteBack = needWriteBack;
    }

    /**
     * 获取将此变量的值写回内存所需的地址
     */
    public Address getWriteBackAddr(){
        return pointingAddr;
    }

    public boolean isArrayAddr() {
        return arrayAddr;
    }

    public void setArrayAddr(boolean arrayAddr) {
        this.arrayAddr = arrayAddr;
    }

    public boolean isNeedWriteBack() {
        return needWriteBack;
    }

    public void setActiveOnExit(boolean activeOnExit) {
        this.activeOnExit = activeOnExit;
    }

    public Var getDependency() {
        return dependency;
    }

    public void setDependency(Var dependency) {
        this.dependency = dependency;
    }
}

///**
// * 表示写入数组操作时所写入内存位置的变量
// * 拥有两个依赖值,为数组基址和偏移量
// */
//class ArrayAddrTemp{
//
//}
