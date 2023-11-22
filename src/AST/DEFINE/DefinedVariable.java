package AST.DEFINE;

import ASM.AsmAddress.Address;
import ASM.AsmAddress.MemRef;
import ASM.AsmAddress.MemoryAddress;
import ASM.CodeGenerator;
import ASM.Register.Register;
import AST.EXPR.Assignable;
import AST.EXPR.ExprNode;
import AST.EXPR.VariableNode;
import AST.Node;
import AST.NodeList.ListNode;
import AST.TYPE.TypeNode;
import IR.Optimise.DagNode;
import IR.instruction.Instruction;
import IR.Value;
import IR.Var;
import IR.Constants.Type;
import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;
import CompileException.*;
import utils.CalculateTypeWidth;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class DefinedVariable extends Entity implements Var, Assignable {
    /*      标志区     */
    protected boolean operand;
    protected boolean passingValueAsArg=false;
    protected boolean active=false;
    //是否是形参
    private boolean isParam=false;
    //表示变量的值是否在加载后改变,即脏位
    private boolean valueChanged=false;
    protected boolean storeInStack=false;

    protected boolean global;
    protected boolean isConst=false;

    protected boolean Static=false;

    /*      标志区结束   */

    protected String posDecorator=null;

    protected ExprNode init=null;
    protected List<ExprNode> iniExprList=null;
    //假设在开始时所有非临时变量都是活跃的
    protected int nextUsed;
    //最近定值位置
    protected DagNode lastDagNode=null;
    protected DagNode recentDagNode=null;
//    protected MemRef memRef=null;
    protected MemoryAddress memoryAddress=null;
//    protected boolean firstUsed =false;
    private Value iniValue;
    //在局部符号表中的位置
    protected int offset=-1;
    private  int usedNum=0;
    private int tableId;

    private int width=-1;

    //地址描述符
    protected List<Address> addrDecorator=new LinkedList<>();

    public DefinedVariable(Location location, boolean priv, TypeNode type, String name, Node init) {
        super(location,priv,type,name);
        if(init!=null){
            if(init instanceof ExprNode){
                this.init=(ExprNode) init;
            }else if(init instanceof ListNode){
                this.iniExprList=((ListNode)init).getNodeList();
            }else {
                throw new RuntimeException("错误的对象,类型为"+init.getClass()+",位置:"+init.getLocation());
            }
        }else{
            this.init = null;
            this.iniExprList=null;
        }
        entityType=Entity.VARIABLE;
    }

    public DefinedVariable(Location location, String name, ExprNode init) {
        super(location,name);
        this.init = init;
        entityType=Entity.VARIABLE;
    }

    public VariableNode toVariableInstance(){
        return new VariableNode(location,getName(),this);
    }

    public void setPriv(boolean priv) {
        this.priv = priv;
    }

    public void setType(TypeNode type) {
        this.type = type;
    }

    @Override
    public void accept(ASTVisitor ASTVisitor)throws CompileError {
        ASTVisitor.visit(this);
    }

    @Override
    public void dump(int level) {
        dumpHead("Variable Definition",level);
            if(!isDefined()){
                ident(level+1,"extern: true");
            }
            ident(level+1,"static:"+priv);
            ident(level+1,"Type:"+type);
            ident(level+1,"Name:"+name);
            if(init!=null){
                ident(level+1,"Init Expr:");
                init.dump(level+2);
            }
        dumpEnd("Variable Definition",level);
    }

    public ExprNode getInit() {
        return init;
    }

    public List<ExprNode> getIniExprList() {
        return iniExprList;
    }

    /**
     * 设置初始化列表中的第n个表达式
     */
    public void setNthIniExpr(int n, ExprNode expr){
        iniExprList.set(n,expr);
    }

    @Override
    public String toString() {
//        return "DefVar@" +name+"#"+type+location;
        return "$"+name;
    }

    public void setInit(ExprNode init) {
        this.init = init;
    }

    @Override
    public Type getIRType() {
        if(getType()==null){
            throw new RuntimeException();
        }
        return Type.TypeNodeToIRType(getType());
    }

    public boolean isActive() {
        return active;
    }

    public boolean isStillUsed(){
        //活跃或者充当当前指令的操作数,表示仍在使用
        return  active||isOperand();
    }

    /**
     *查看变量是否仍要需要在r中使用,若存在不同于r的寄存器储存此变量的值
     *那么r可以挤出此变量的值
     */
    public boolean isStillUsedInReg(Register r){
        boolean b=isStillUsed();
        //如果b仍要使用,且没有其他寄存器保存变量的值,
        //那么该变量需要保留在r中
        return  b&&!hasOtherRegisterAddr(r);
    }

    @Override
    public boolean hasOtherRegisterAddr(Register theReg) {
        if(theReg==null) return false;
        return addrDecorator.stream()
                .anyMatch(addr -> addr instanceof Register
                     && !addr.equals(theReg));
    }

    @Override
    public void setActive(Boolean active) {
        this.active=active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getNextUsed() {
        return nextUsed;
    }

    public void setNextUsed(int nextUsed) {
        this.nextUsed = nextUsed;
    }

    @Override
    public DagNode getAssignedDag() {
        return lastDagNode;
    }

    public void setAssignedDag(DagNode lastDagNode) {
        this.lastDagNode = lastDagNode;
    }

    @Override
    public DagNode getRecentDag() {
        return recentDagNode;
    }

    @Override
    public void setRecentDag(DagNode dag) {
          this.recentDagNode=dag;
    }

    public String getDescription(){
        return name+"\t"+"active?"+active+"\t";
    }

    public List<Address> getAddrDecorator() {
        return addrDecorator;
    }


    public List<Register> getRegisters(){
        List<Register> result= addrDecorator.stream()
                .filter(address -> address instanceof Register)
                .map(address -> (Register) address)
                .collect(Collectors.toCollection(LinkedList::new));
        return result;
    }

    @Override
    public void setMemoryAddr(MemoryAddress memoryAddr) {
        this.memoryAddress = memoryAddr;
        if(memoryAddress instanceof MemRef memRef){
            Var var=memRef.getVar();
            if(var==null)
               memRef.setVar(this);
        }
    }

    @Override
    public boolean isLeftValue() {
        return false;
    }

    /**
     *判断是否需要将当前值存入内存
     */
    public boolean needStore(){
        MemoryAddress mem=getMemAddr();
        return !isArrayAddr()  &&
            !addrDecorator.contains(mem)&&(isActive()||isGlobal()||isStatic());
    }

    @Override
    public void addAddress(Address address) {
        if(!addrDecorator.contains(address)){
            addrDecorator.add(address);
            if(address instanceof MemRef memRef&&memoryAddress==null){
                setMemRef(memRef);
            }
        }
        if(address==getWriteBackAddr()){
            valueChanged=false;
        }
    }

    public void setUniqueAddress(Address address){
        //切断其他地址与该变量的联系
        for(Address addr:addrDecorator){
            if(addr instanceof Register&&!addr.equals(address)){
                Register r=(Register) addr;
                r.removeVar(this);
            }
        }
        if(!addrDecorator.isEmpty())
            addrDecorator=new LinkedList<>();

        addrDecorator.add(address);

        MemRef memRef=getMemRef();
        //设置脏位
        if((memRef==null)//临时变量和寄存器形参
                ||(address!=memRef)//普通变量
        ){
            valueChanged=true;
        }
    }



    @Override
    public void removeAddress(Address address) {
       addrDecorator.remove(address);
    }

    @Override
    public MemRef getMemRef() {
        if(memoryAddress instanceof MemRef){
            return (MemRef) memoryAddress;
        }
        return null;
    }

    @Override
    public void setMemRef(MemRef memRef) {
        this.memoryAddress=memRef;
    }

    @Override
    public boolean hasLoaded() {
        return !getRegisters().isEmpty();
    }

    @Override
    public boolean isOperand() {
        return operand;
    }

    public void setIsOperand(boolean operand) {
        this.operand = operand;
    }

    @Override
    public int getUsedNum() {
        return usedNum;
    }

    @Override
    public void incUsedNum(){
        usedNum++;
    }

    public void resetUsedNum(){usedNum=0;}

    @Override
    public boolean isFunctionPointer() {
        return type.isPointerType()&&
                type.getPointerBaseType().isFunctionPointer();
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public boolean isConst() {
        return isConst;
    }

    public boolean isStatic(){
        return isPriv();
    }

    public Value getIniValue() {
        return iniValue;
    }

    public void setIniValue(Value iniValue) {
        this.iniValue = iniValue;
    }


    public boolean isParam() {
        return isParam;
    }

    public void setParam(boolean param) {
        isParam = param;
    }

    @Override
    public ExprNode getRightHandSideExpr() {
        return init;
    }

    @Override
    public TypeNode getType() {
        return type;
    }


    @Override
    public void setRightHandSideExpr(ExprNode node) {
        this.init=node;
    }

    public Address getWriteBackAddr(){
        return getMemAddr();
    }

    public boolean isRegParam() {
        return isParam&&(getMemRef()==null);
    }

    @Override
    public boolean isPassingValueAsArg() {
        return passingValueAsArg;
    }

    public void setPassingValueAsArg(boolean passingValueAsArg) {
        this.passingValueAsArg = passingValueAsArg;
    }

    public boolean isArrayAddr(){
        return getType().isArrayType()||getType().isComposedType();
    }

    @Override
    public String getPosDecorator() {
//        return "$"+name;
        return posDecorator;
    }

    /**
     * @return 是否在函数调用前被保存进栈
     */
    @Override
    public boolean isStoreInStack() {
        return storeInStack;
    }

    public void setStoreInStack(boolean storeInStack) {
        this.storeInStack = storeInStack;
    }

    @Override
    public boolean mustStoreBeforeCall() {
        return (memoryAddress == null||!addrDecorator.contains(memoryAddress))
                && ((!isPassingValueAsArg() && isStillUsed())
                        || (isPassingValueAsArg() && !CodeGenerator.isInCurParamList(this)));
    }

    @Override
    public void setPosDecorator(int level, int nth) {
        this.posDecorator= String.format("%s$%s%s", getName(),
                ((level > 0) ? (level) : ""), nth > 0 ? ("@"+nth ): "");
    }

    @Override
    public MemoryAddress getMemAddr() {
        return memoryAddress;
    }

    public int getTableId() {
        return tableId;
    }

    public void setTableId(int tableId) {
        this.tableId = tableId;
    }

    /**
     * 在生成变量的直接地址时用于唯一标识该变量
     */
    public String getUniqueName() {
        if(this instanceof DefinedConst){
            return getName();
        }
        else if(isPriv()){
            if(isGlobal()){
                return getName()+"_$_";
            }else{
                return getName()+"$_"+tableId;
            }
        } else{
            return getName();
        }
    }

    @Override
    public boolean isSigned() {
        if(type!=null){
            return getType().isSigned();
        }else{
            return getIRType().isSigned();
        }
    }

    public boolean isGlobal() {
        return global;
    }


    public void setGlobal(boolean global) {
        this.global = global;
    }

    /**
     * 判断是否支持列表初始化
     */
    public boolean supportListInitialize(){
        return type.isArray()||type.isComposedType();
    }

    //    public void setPosDecorator(){
//        if(this.posDecorator==null){
//            setPosDecorator("$"+getName());
//        }
//    }


    public int getWidth() {
        if(width==-1){
            width= CalculateTypeWidth.getTypeWidth(type);
        }
        return width;
    }
}
