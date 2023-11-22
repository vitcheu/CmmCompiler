package ASM.Register;

import ASM.AsmAddress.Address;
import ASM.AsmAddress.MemRef;
import ASM.AsmAddress.RegisterAddress;
import ASM.Constants.RegisterState;
import IR.Constants.Type;
import IR.Constants.TypeWidth;
import IR.Literal;
import IR.Value;
import IR.Var;

import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

import static ASM.Constants.RegisterState.*;

public class Register implements Address {

    public enum RegType {
        Caller,
        Callee,
        Specific,
    }

    public static class RegisterContent {
        public RegisterContent(Register r){
            this.regState=r.regState;
            this.preState=r.preState;
            this.registerDecorator=new LinkedList<>(r.getRegisterDecorator());
            this.memRefs=new LinkedList<>(r.getMemRefs());
//            this.stackAddr=r.getStackAddr();
            this.storeLiteral=r.storeLiteral;
            this.dirtyPos=Arrays.copyOfRange(r.dirtyPos,0,4);
            this.HBPart=r.HBPart;

            this.Using=r.Using;
            this.usedAsParamReg=r.usedAsParamReg;
        }

        private RegisterState regState= EMPTY;
        private RegisterState preState=null;
        //寄存器描述符
        private List<Var> registerDecorator = new LinkedList<>();
        private List<MemRef> memRefs = new LinkedList<>();
        private Literal storeLiteral = null;
        //记录寄存器的脏位
        protected boolean[] dirtyPos;
        //第一个字的高字节部分
        private HBRegister HBPart=null;
//        保存在栈中的地址
//        private MemRef stackAddr=null;

        /*      标记      */
        private boolean Using = false;
        private boolean usedAsParamReg=false;
    }

    /**
     * 保存寄存器内容
     */
    public RegisterContent storeContent(){
        content=new RegisterContent(this );
        return content;
    }

    /**
     * 加载寄存器内容
     */
    public void loadContent(RegisterContent content){
        this.regState= content.regState;
        this.preState= content.preState;

        //恢复寄存器描述符
        this.registerDecorator=new LinkedList<>(content.registerDecorator);
        //在变量的地址描述符中加入此寄存器
        for(Var var:registerDecorator){
            var.addAddress(this);
        }

        //恢复内存访问方式
        this.memRefs=new LinkedList<>(content.memRefs);
        //在内存访问方式的基址寄存器描述符中加入此寄存器
        for(MemRef mem:memRefs){
            mem.addBase(this);
        }

        this.storeLiteral=content.storeLiteral;
//        this.stackAddr=content.stackAddr;
        this.HBPart=content.HBPart;
        /*恢复脏位*/
        this.dirtyPos=Arrays.copyOfRange(content.dirtyPos,0,4);

        this.Using=content.Using;
        this.usedAsParamReg=content.usedAsParamReg;
    }

    public void loadContent(){
        loadContent(content);
    }


    /**
     *         <内容区>
     **/
    private RegisterState regState= EMPTY;
    private RegisterState preState=null;
    //寄存器描述符
    protected List<Var> registerDecorator = new LinkedList<>();
    //当用此寄存器储存地址时,该字段用来储存地址所表示的变量
//    private Set<Var> addressOfVars = new LinkedHashSet<>();
    private List<MemRef> memRefs = new LinkedList<>();
    protected Literal storeLiteral = null;
    //第一个字的高字节部分
    private HBRegister HBPart=null;
    //保存在栈中的地址
    protected MemRef stackAddr=null;
    //记录寄存器的脏位
    protected boolean[] dirtyPos=new boolean[]{true,true,true,true};

    /*      标记      */
    protected boolean Using = false;
    protected boolean usedAsParamReg=false;

    protected RegisterContent content=null;

    /**
     *          </内容区>
     */

    //描述符栈
    protected LinkedList<List<Var>> decoratorStack = new LinkedList<>();
    protected LinkedList<Literal> literalStack = new LinkedList<>();
    protected LinkedList<RegisterState> stateStack =new LinkedList<>();
    public static PrintWriter pw;
    protected RegType type;
    //寄存器名
    private String name;


    public Register(String name) {
        this.name = name;
        this.type = RegType.Caller;
    }

    public Register(String name,RegisterState iniState) {
        this.name = name;
        this.type = RegType.Caller;
        this.regState=iniState;
    }


    protected Register(String name, RegType type) {
        this.name = name;
        this.type = type;
    }

    public HBRegister getHBPart() {
        return HBPart;
    }

    public void setHBPart(HBRegister HBPart) {
        this.HBPart = HBPart;
    }

    public boolean isHB(){
        return false;
    }

    //64位寄存器组
    public static final Register RAX = new Register("RAX");
    public static final Register RBX = new Register("RBX", RegType.Callee);
    public static final Register RCX = new Register("RCX");
    public static final Register RDX = new Register("RDX");
    public static final Register RIP = new Register("RIP", RegType.Specific);

    public static final Register R8 = new Register("R8");
    public static final Register R9 = new Register("R9");
    public static final Register R10 = new Register("R10");
    public static final Register R11 = new Register("R11");
    public static final Register R12 = new Register("R12", RegType.Callee);
    public static final Register R13 = new Register("R13", RegType.Callee);
    public static final Register R14 = new Register("R14", RegType.Callee);
    public static final Register R15 = new Register("R15", RegType.Callee);

    public static final Register RBP = new Register("RBP", RegType.Specific);
    public static final Register RSP = new Register("RSP", RegType.Specific);
    public static final Register RDI = new Register("RDI", RegType.Callee);
    public static final Register RSI = new Register("RSI", RegType.Callee);

    //传递参数的寄存器
    public static Register[] GeneralParamRegs = {RCX, RDX, R8, R9};
    //Xmm0~Xmm3用于前四个传递参数,且与相同位置的通用寄存器一一对应
    public static SSERegister[] FloatParamRegs=SSERegister.getParamRegister();


    public static HashMap<String, Register> registerMap = new LinkedHashMap<>();

    static {
        registerMap.put("RAX", RAX);
        registerMap.put("RCX", RCX);
        registerMap.put("RDX", RDX);
        registerMap.put("RBP", RBP);

        registerMap.put("R8", R8);
        registerMap.put("R9", R9);
        registerMap.put("R10", R10);
        registerMap.put("R11", R11);
        registerMap.put("RBX", RBX);
        registerMap.put("R12", R12);
        registerMap.put("R13", R13);
        registerMap.put("R14", R14);
        registerMap.put("R15", R15);

        registerMap.put("RSP", RSP);
        registerMap.put("RIP", RIP);
        registerMap.put("RDI", RDI);
        registerMap.put("RSI", RSI);

        RAX.setHBPart(HBRegister.AH);
        RBX.setHBPart(HBRegister.BH);
        RCX.setHBPart(HBRegister.CH);
        RDX.setHBPart(HBRegister.DH);
    }

    /**
     * 判断是否可分配该寄存器给类型为type的值
     * 当type不为null且返回为true时,自动更改寄存器状态以适应类型type
     */
    public boolean isAvailableForType(Type type) {
        if (!isGeneral()||isUsedAsParamReg()) return false;
        int byteWidth = 1,typeWidth=-1;
        if(type!=null)
            typeWidth=type.getWidth();
        boolean canOfferHB= (regState == B)//可分配高字节空间
                && (typeWidth== byteWidth)&&hasHighByte();

        //低字节不可用且高字节可分配
        if(canOfferHB&& !HBPart.isActive()){
            if(type!=null)
                setStateFromValueType(type,isActive(),false);
            return true;
        }
        if(Using){
            return false;
        }

        if (storeLiteral != null
                || regState == EMPTY){
            if(type!=null)
                setStateFromValueType(type);
            return true;
        }

        //表示高低字节是否活跃
        boolean lowByteActive=isActive(),highByteActive=true;
        if(regState==HB||regState==HH||regState==B&&hasHighByte()){
            highByteActive= HBPart.isActive();
        }

        //都活跃或者分配一个字节以上的值且低字节或高字节活跃时,不能分配
        if(lowByteActive&&highByteActive){
            return false;
        } else if ((regState == HB || regState == HH || regState == B)
                && (lowByteActive || highByteActive)
                && (hasHighByte())//必须有这个条件,因为处于B状态的寄存器可能没有高字节
        ) {
            return false;
        }
        else if(lowByteActive){
            return false;
        }

        //其他情况,可以分配
        if(type!=null)
            setStateFromValueType(type,lowByteActive,highByteActive);
        return true;
    }

    public boolean isAvailable(){
        return isAvailableForType(null);
    }

    /**
     *  判断该寄存器是否活跃,
     *  不考虑高字节部分
     */
    protected boolean isActive(){
        List<Var> vars=registerDecorator;
        for (Var v : vars) {
            if(v.isStillUsedInReg(this))
                return true;
        }
        for (MemRef mem : memRefs) {
            if (mem.isActive()) {
                return true;
            }
        }
        return false;
    }

    private static boolean checkActive(List<Var> vars, List<MemRef> memRefs){
        for (Var v : vars) {
            if(v.isStillUsed())
                return true;
        }
        for (MemRef mem : memRefs) {
            if (mem.isActive()) {
                return true;
            }
        }
        return false;
    }

    private boolean HighOrLowByteActive(){
        List<Var> vars=new LinkedList<>(registerDecorator);
        List<MemRef> memRefs=new LinkedList<>(getMemRefs());
        if(regState==HH||regState==HB){
            vars.addAll(HBPart.getRegisterDecorator());
            memRefs.addAll(HBPart.getMemRefs());
        }
        return checkActive(vars,memRefs);
    }



    /**
     * @return 若当前状态为H,返回高字节寄存器,否则返回本寄存器
     */
    public Register getReg(){
        if(regState==HB){
            return HBPart;
        }else
            return this;
    }

    public boolean canBeDestination() {
       return HighOrLowByteActive();
    }

//    /**
//     * 返回一个空闲的寄存器
//     */
//    public static Register getReg() {
//        for (String name : registerMap.keySet()) {
//            Register r = registerMap.get(name);
//            if (r.isAvailable() && r != RBP && r != RIP && r != RSP) {
//                return r;
//            }
//        }
//        return null;
//    }

    public boolean isEmpty() {
        return storeLiteral == null
                && registerDecorator.isEmpty()
                && memRefs.isEmpty();
    }


    public void addVariable(Var var) {
        if (!registerDecorator.contains(var)) {
            registerDecorator.add(var);
            pw.println(name + "添加" + var + "的值,类型:"+var.getIRType()+",当前状态:"+regState);
            pw.println("寄存器描述符:" + registerDecorator);
        }
    }

    public void setUniqueVar(Value v) {
        clear(false);
        shiftToState(v.getIRType());
        if (v instanceof Var) {
            Var var = (Var) v;
            registerDecorator.add(var);
            pw.println(name + "装入" + var);
            storeLiteral = null;
            var.addAddress(this);
        } else {
            registerDecorator.clear();
            storeLiteral = (Literal) v;
        }
//        Type t=v.getIRType();
//        setStateFromValueType(t);
    }

    public List<Var> getRegisterDecorator() {
        List<Var>  vars;
        if(HighByteAvailable()){
          vars=new LinkedList<>(registerDecorator);
          vars.addAll(HBPart.getRegisterDecorator());
        }else{
            vars=registerDecorator;
        }
        return vars;
    }


    public int getLen() {
        int len;
        if(regState==B||regState==H){
            len=8;
        }else if(regState==W)
            len=16;
        else if(regState==D)
            len=32;
        else
            len=64;
        return len;
    }

    /**
     * 返回以字节为单位的寄存器宽度
     */
    public int getWidth(){
        RegisterState state=regState;
        if(state==B||state==H)
            return 1;
        if(state==HH||state==HB||state==W)
            return 2;
        if(state==D)
            return 4;
        if(state==W)
            return 2;

        if(state==SS)
            return 4;
        else
            return 8;
    }

    @Override
    public String toString() {
        return getName().toLowerCase();
    }

    public String getName(){
        String base,result;
        boolean b=this==RSI||this==RDI||this==RSP||this==RBP;
        if(hasHighByte()){
            //AX~DX
            base=name.substring(1,2);
        }else if(b){
            base=name.substring(1,3);
        }else{
            //RB~R15
            base=name;
        }

        if(regState==B||regState==HB||regState==HH){
            if(hasHighByte()||b)
                result=base+'L';
            else
                result=base+"B";
        }else if(regState==H){
            result=base;
        }else if(regState==W){
            if(hasHighByte())
                result=base+"X";
            else if(b)
                result=base;
            else
                result=base+"W";
        }else if(regState==D){
            if(hasHighByte())
                result="E"+base+"X";
            else if(b)
                result="E"+base;
            else
                result=base+"D";
        }else{
            result=name;
        }

        return result;
    }

    public String getRegisterName(){
        return  name;
    }

    public String getDescription() {
        String s=null;
        if (!registerDecorator.isEmpty()) {
            s = registerDecorator.toString();
        }
        if (!memRefs.isEmpty()) {
            if (s != null && s.length() > 1) {
                s = s.substring(0, s.length() - 1);
                s += ",";
            }
            else if (s == null)
                s = "[";
            for (MemRef mem : memRefs) {
                Var v = mem.getVar();
                if (v != null) {
                    s += "&" + v + ",";
                }
            }
            if (s.length() > 1)
                s = s.substring(0, s.length() - 1);
            s += "]";
        }
        if(storeLiteral!=null){
            if (s != null && s.contains("]")) {
                s = s.substring(0, s.length() - 1);
                s += ","+storeLiteral.getLxrValue()+"]";
            }else{
                s="["+storeLiteral.getLxrValue()+"]";
            }
        }

        return getName() + ":\t" + s;
    }

    public boolean isGeneral() {
        return (this != RBP) && (this != RIP) && (this != RSP);
    }

    public static void printRegister() {
        pw.println("寄存器内容:");
        List<Register> registers = getRegs();
        for (Register r : registers) {
            if (!r.getRegisterDecorator().isEmpty()
                ||!r.getMemRefs().isEmpty()
                ||r.getStoreLiteral()!=null) {
                pw.println(r.getDescription());
                //输出高位部分
                if(r.HighByteAvailable()){
                    HBRegister hb=r.getHBPart();
                    if(!hb.getRegisterDecorator().isEmpty())
                        pw.println(hb.getDescription());
                }
            }
        }
//        pw.println("$Top=" + FloatRegister.peek());

        pw.println("暂时不可分配寄存器:");
        for (Register r : registers) {
            if (!r.isAvailable() && r.isGeneral()) {
                pw.println(r.getDescription());
                //输出高位部分
                if(r.HighByteAvailable()){
                    HBRegister hb=r.getHBPart();
                    if(!hb.isAvailable())
                        pw.println(hb.getDescription());
                }
            }
        }
    }


    public boolean isUsing() {
        return Using;
    }

    public void setUsing(boolean using) {
        Using = using;
    }

    public void clear() {
        clear(true);
    }

    /**
     * 清除与变量的连接,清除字面量和内存访问方式
     * 不清除原状态
     */
    private void clear(boolean removeFromVarDecorator){
        for (Var v : registerDecorator) {
//            if(removeFromVarDecorator)
                v.removeAddress(this);
        }
        if(!registerDecorator.isEmpty())
            registerDecorator = new LinkedList<>();
        if(!memRefs.isEmpty()){
            //内存访问将不能使用该寄存器
            for(MemRef mem:memRefs){
                mem.removeBase(this);
            }
            memRefs = new LinkedList<>();
        }
        storeLiteral = null;
//        setState(EMPTY);
    }

    public void removeVar(Var var) {
        registerDecorator.remove(var);
        if(isEmpty())
            setState(EMPTY);
    }

    public void storeDecorator() {
        decoratorStack.addLast(registerDecorator);
        literalStack.addLast(storeLiteral);
        stateStack.addLast(regState);
        clear();
    }

    public void loadDecorator() {
        registerDecorator = decoratorStack.removeLast();
        storeLiteral = literalStack.removeLast();
        setState(stateStack.removeLast());
    }

    /**
     * @return 寄存器的类型,表示寄存器是否易变
     */
    public RegType getType() {
        return type;
    }

    public static List<Register> getCallerRegister() {
        List<Register> result =new LinkedList<>();
        for (String s : registerMap.keySet()) {
            Register r = registerMap.get(s);
            if (r.getType() == RegType.Caller) {
                result.add(r);
            }
        }
        List<Register> sseRegisters=new LinkedList<>();
        for(Register r:SSERegister.getRegs()){
            if(r.getType()==RegType.Caller){
                sseRegisters.add(r);
            }
        }
        result.addAll(sseRegisters);
        return result;
    }

    public static List<Register> getIntegerRegs() {
        List<Register> registers = new LinkedList<>();
        for (String name : registerMap.keySet()) {
            Register r = registerMap.get(name);
            registers.add(r);
        }
        return registers;
    }

    public static List<Register> getRegs() {
        List<Register> registers = getIntegerRegs();
        List<Register> floatRegs = SSERegister.getRegs();
//        registers.addAll(FloatRegister.getFloatRegs());
        registers.addAll(floatRegs);
        return registers;
    }

    /**
     * 清空所有寄存器
     */
    public static void clearAllRegs(){
        for(Register r:Register.getRegs()){
            r.clear();
            r.setState(EMPTY);
        }
    }

    public static HashMap<Register,RegisterContent> storeAllRegisters(){
        HashMap<Register,RegisterContent> contentHashMap=new HashMap<>();
        for(Register r:Register.getRegs()){
            RegisterContent content=r.storeContent();
            contentHashMap.put(r,content);
        }
        return contentHashMap;
    }

    public static void restoreAllRegisters( HashMap<Register,RegisterContent> contentHashMap){
        contentHashMap.entrySet().stream()
                .forEach(entry->{
                    Register r=entry.getKey();
                    RegisterContent content=entry.getValue();
                    r.loadContent(content);
                });
    }

    public static List<Register> getCalleeRegister() {
        List<Register> result = registerMap.keySet().stream()
                .map(s -> registerMap.get(s))
                .filter(r -> r.getType() == RegType.Callee)
                .collect(Collectors.toCollection(LinkedList::new));
        return result;
    }

    public boolean isFloatRegister() {
        return (this instanceof FloatRegister) || (this instanceof SSERegister);
    }

    @Override
    public boolean isFloat() {
        return isFloatRegister();
    }

//    public void addVarAddr(Var v) {
//        if (!isEmpty()) {
//            clear();
//        }
//        addressOfVars.add(v);
//    }
//
//    public Set<Var> getAddressOfVars() {
//        return addressOfVars;
//    }

    public void addMemref(MemRef memRef) {
        if(!isGeneral())
            return;
//        if (!isEmpty()) {
//            clear();
//        }
//        MemRef m1=getMemRef(memRef);
//        if(m1==null)
            memRefs.add(memRef);
        //表示当前寄存器存储的是地址
//        setState(M);
    }

    public List<MemRef> getMemRefs() {
        return memRefs;
    }

    public RegisterState getRegState() {
        return regState;
    }

    public void setState(RegisterState state){
//        if(pw!=null)
//            pw.println("@oriState="+regState+",@curState="+state);
        preState=this.regState;
        this.regState=state;
    }

    public void setDirtyPos(Type type){
        int width=type.getWidth();
        dirtyPos[3]=dirtyPos[3]||width>=TypeWidth.ptrWidth;
        dirtyPos[2]=dirtyPos[2]||width>=TypeWidth.dwordWidth;
        dirtyPos[1]=dirtyPos[1]||width>=TypeWidth.wordWidth;
        dirtyPos[0]=dirtyPos[0]||width>=TypeWidth.byteWidth;
    }

    public RegisterState getPreState() {
        return preState;
    }

    public void loadPreState(){
        setState(preState);
    }

    /**
     * 判断本寄存器是否有高字节寻址空间
     */
    public boolean hasHighByte(){
        return this==RAX||this==RBX||this==RCX||this==RDX;
    }

    public boolean HighByteAvailable(){
        return regState==HH||regState==HB;
    }

    /**
     *根据转入的值的类型设置装入后寄存器的储存状态
     */
    public void setStateFromValueType(Type type){
        setStateFromValueType(type,false,false);
    }

    private void setStateFromValueType(Type type, boolean lowByteActive, boolean highByteActive) {
        RegisterState state;
        if(type.getWidth()== TypeWidth.byteWidth){
            //低位字节有值时而高位未分配
            if(regState==B&&hasHighByte()||regState==HB
                ||regState==HH){
                //分配低字节
                if(!lowByteActive){
                    if(regState==B){
                        state=B;
                    }else
                        state=HH;
                }
                //分配高字节
                else state=HB;
            }else if(regState==H){
                return;
            }
            else
                state=B;
        } else {
            state=typeToState(type);
        }
        setState(state);
    }

    /**
     * @return 包括高位字节在内的寄存器描述符的所有内容
     */
    private List<Var> getAllRegisterDecorator(){
        List<Var>result=new LinkedList<>(registerDecorator);
        if(hasHighByte()&&HighByteAvailable()){
            result.addAll(HBPart.registerDecorator);
        }
        return result;
    }

    private List<MemRef> getAllMemRefs(){
        List<MemRef>result=new LinkedList<>(memRefs);
        if(hasHighByte()&&HighByteAvailable()){
            result.addAll(HBPart.getMemRefs());
        }
        return result;
    }

    /**
     * 清洗易失性寄存器,
     * 使其剔除非传参变量,这些值或者已经保存在栈上,
     * 或者调用后不需要,可以直接把它们剔除
     */
    public void wash(){
        //清除其他非传参变量
        //因为若其他非传参变量仍需要在函数调用后使用,那么此前已经把它们的值压栈
        //因此将它们挤出寄存器是安全的
//        pw.println("#清除前:\t"+this+",r.decorator="+this.getRegisterDecorator()+"\n" +
//                "r.memrefs="+this.getMemRefs());
        List<Var> vars=getDecorator().stream().toList();
        for(Var var1:vars){
            if(!var1.isPassingValueAsArg()){
                this.removeVar(var1);
                var1.removeAddress(this);
            }
        }
        List<MemRef> memRefs=getMemRefs().stream().toList();
        for(MemRef mem:memRefs){
            Var var1=mem.getVar();
            if(var1!=null&&!var1.isPassingValueAsArg()){
                mem.removeBase(this);
                this.removeMemRef(mem);
            }
        }
        if(hasHighByte()&&HighByteAvailable()){
            HBPart.wash();
        }
//        pw.println("#清除后:\t"+this+",r.decorator="+this.getRegisterDecorator()+"\n" +
//                "#r.memrefs="+this.getMemRefs());
    }

    /**
     *赋值寄存器reg的内容
     */
    public void copyContent(Register reg){
        clear();
        //复制寄存器状态
//        RegisterState state= reg.getRegState();
//        if (!hasHighByte() && hasHighByte()
//                && (state == HH || state == HB)) {
//            setState(RegisterState.W);
//        } else setState(state);

        //复制寄存器描述符
        List<Var> varList=new LinkedList<>(reg.getDecorator());
        for(Var v:varList){
//            v.removeAddress(reg);
//            reg.removeVar(v);
            v.addAddress(this);
            addVariable(v);
        }

        //建立内存访问与此寄存器的连接
        List<MemRef> memRefs=reg.getMemRefs();
        for(MemRef mem:memRefs){
            mem.addBase(this);
            this.memRefs.add(mem);
        }
        //复制字面量
        this.storeLiteral=reg.getStoreLiteral();
        //复制高字节寄存器
        if(hasHighByte()&&reg.hasHighByte()){
            HBRegister hb1=this.HBPart,hb2=reg.getHBPart();
            List<Var> vars=new LinkedList<>(hb2.getDecorator());
            hb1.setRegisterDecorator(vars);
        }
    }

    public Literal getStoreLiteral() {
        return storeLiteral;
    }

    /**
     * 返回寄存器描述符的内容,不包括高字节寄存器
     */
    public List<Var> getDecorator(){
        return registerDecorator;
    }

    /**
     *根据type的宽度扩展或截断寄存器
     */
    public void shiftToState(Type type){
        if(isFloat()&&type.isIntType()){
            return;
        }
        else if(regState==H&&type.getWidth()==TypeWidth.byteWidth){
            return;
        }else{
            RegisterState state=typeToState(type);
            setState(state);   
        }
    }
    
    private static RegisterState typeToState(Type type){
        RegisterState state;
        if(type==null)
            state=Q;
        else {
            int width=type.getWidth();
            if(type.isFloatType()){
                if(width==TypeWidth.floatWidth){
                    state=SS;
                }else{
                    state=SD;
                }
            }
            else if (width == TypeWidth.byteWidth) {
                state = B;
            } else if (width== TypeWidth.wordWidth) {
                state=W;
            } else if (width== TypeWidth.dwordWidth) {
                state = D;
            } else {
                state = Q;
            }
        }
        return state;
    }

    /**
     * 获得寄存器所储存值的类型
     */
    private   Type getRegisterIRType(){
        return switch (regState) {
            case B -> Type.byteType;
            case HB -> Type.int16;
            case HH -> Type.int16;
            case H -> Type.byteType;
            case W -> Type.int16;
            case D -> Type.int32;
            case Q -> Type.pointer;
            case SS -> Type.floatType;
            case SD -> Type.doubleType;
            default -> Type.pointer;
        };
    }

    @Override
    public Type getIRType() {
        return getRegisterIRType();
    }

    public void shiftToState(RegisterState regState) {
        RegisterState curState=this.regState,newState;
        if(regState==H&&curState!=H){
            newState=B;
        }else if(regState==B&&curState==H){
            newState=H;
        }
        else{
            newState=regState;
        }
        setState(newState);
    }


    /**
     * 将寄存器转换为不可变的寄存器地址
     */
    public static Address toRegAddress(Address address){
        Address a=address;
        if(a instanceof Register){
            a=new RegisterAddress((Register) a);
        }
        return a;
    }

    public RegisterAddress toRegAddress(){
        return(RegisterAddress) toRegAddress(this);
    }

    public void setRegisterDecorator(List<Var> registerDecorator) {
        this.registerDecorator = registerDecorator;
    }

    public MemRef getMemRef(MemRef memRef){
        for(MemRef mem:memRefs){
            if(mem.totallyEquals(memRef))
                return mem;
        }
        return null;
    }

    public void removeMemRef(MemRef mem){
       MemRef memRef=  getMemRef(mem);
       if(memRef!=null){
           this.memRefs.remove(mem);
       }
    }

    /**
     * 判断本寄存器是否为易失性寄存器
     */
    public boolean isViolate(){
        return type==RegType.Caller;
    }

    /**
     * 根据v的类型获取传递实参v的值所需的寄存器
     * @param  i 第几个参数
     */
    public static Register getParamReg(Value v,int i){
        Type vType=v.getIRType();
        if(vType.isFloatType()){
            return FloatParamRegs[i];
        }else{
            return GeneralParamRegs[i];
        }
    }

    public static Register getGenParamReg(int i){
        return GeneralParamRegs[i];
    }

    public static List<Register> getAllParamRegs(){
        List<Register>regs=new LinkedList<>(Arrays.asList(GeneralParamRegs));
        regs.addAll(Arrays.asList(FloatParamRegs));
        return  regs;
    }

    /**
     * 判断是否包含value的值
     */
    public boolean containValue(Value value){
        if(value instanceof Literal){
            return storeLiteral.equals(value);
        }else {
            return getRegisterDecorator().contains(value);
        }
    }

    public boolean isUsedAsParamReg() {
        return usedAsParamReg;
    }

    public void setUsedAsParamReg(boolean usedAsParamReg) {
        this.usedAsParamReg = usedAsParamReg;
    }

    @Override
    public boolean isLeftValue() {
        return false;
    }

    public MemRef getStackAddr() {
        return stackAddr;
    }

    public void setStackAddr(MemRef stackAddr) {
        if((this.stackAddr!=null)&&(stackAddr!=null)&&
                !stackAddr.equals(this.stackAddr)){
            throw new RuntimeException();
        }
        this.stackAddr = stackAddr;
        //修改储存的变量的地址描述符
        for(Var var:getRegisterDecorator()){
            if(stackAddr==null&&this.stackAddr!=null){
                var.removeAddress(this.stackAddr);
                var.setStoreInStack(false);
            }else{
                var.addAddress(stackAddr);
                var.setStoreInStack(true);
            }
        }
    }

    public void setCalleeStackAddr(MemRef stackAddr){
        if((this.stackAddr!=null)&&(stackAddr!=null)&&
                !stackAddr.equals(this.stackAddr)){
            throw new RuntimeException();
        }
        this.stackAddr = stackAddr;
        //修改储存的变量的地址描述符
        for(Var var:getRegisterDecorator()){
            if(stackAddr==null&&this.stackAddr!=null){
                var.removeAddress(this.stackAddr);
            }else
                var.addAddress(stackAddr);
        }
    }

    /**
     * 判断是否只储存一个变量,且变量的值已和内存同步或者有其他地址.
     * 如果为真,那么在函数调用前便可 [不保存该寄存器] ,节省进栈和压栈导致的两次内存访问,
     * 增加加载变量的一次内存访问,可至少节省一次内存访问和一条指令;
     */
    public boolean needPushedToStack(){
        if(isAvailable()){
            return false;
        }

        int num=0;//统计已同步的变量的个数
        //还要考虑高位字节
        List<Var> vars=new LinkedList<>(registerDecorator);
        if(hasHighByte()){
            vars.addAll(getHBPart().getRegisterDecorator());
        }
        for(Var var:vars){
            //如果有这类变量,直接返回true,因为这类变量没有内存地址,溢出后将无法找到变量的值,因此必须将保存它们的寄存器进栈
            //一般为用寄存器传递的参数或者临时变量
            if (var.mustStoreBeforeCall()){
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否能分配到影子空间.
     * 当储存的变量含有函数传参的形参时,返回true
     * 否则返回false
     */
    public boolean canStoredToShadowSpace(){
        //不是传参寄存器或者没有储存变量
        if(!isParamRegister()|| registerDecorator.isEmpty())
            return false;
        for(Var var:registerDecorator){
            if((var.isParam()&&var.getMemRef()==null)){
                return true;
            }
        }
        return false;
    }

    public  boolean isParamRegister(){
        Register[] regs=(isFloat())?FloatParamRegs:GeneralParamRegs;
        List<Register> registers= Arrays.asList(regs);
        return  registers.contains(this);
    }

    /**
     * 在基本块末尾处判断能保存至影子空间
     */
    public boolean canStoreToShadowSpaceBeforeBlockEnd(){
        if(!isParamRegister()) return false;

        boolean hasUsedVar=false;
        for(Var var:registerDecorator){
            if(!(var.isParam()&&var.getMemRef()==null)){
                return false;
            }
            if(!hasUsedVar){
                hasUsedVar=var.hasUsed();
            }
        }
        return hasUsedVar;
    }

    public void clean() {
        clear();
        storeLiteral=new Literal("0");
        for(int i=0;i<dirtyPos.length;i++)
            dirtyPos[i]=false;
    }

    public void cleanWhenExtension(Type oriType, Type targetType){
        int start=getStartIndexOfDirtyPos(oriType),
                end=getStartIndexOfDirtyPos(targetType);
        for(int i=start+1;i<=end;i++){
            dirtyPos[i]=false;
        }
    }

    public boolean isClean(){
        return storeLiteral!=null&&storeLiteral.getLxrValue().equals("0");
    }

    public boolean isClean(Type oriType,Type targetType){
        int start=getStartIndexOfDirtyPos(oriType),
                end=getStartIndexOfDirtyPos(targetType);
        boolean ret=true;
        for(int i=start+1;i<=end;i++){
            ret=ret&&!dirtyPos[i];
        }
        return ret;
    }

    private int getStartIndexOfDirtyPos(Type type){
        int width=type.getWidth();
        return switch (width){
            case TypeWidth.ptrWidth -> 3;
            case TypeWidth.dwordWidth -> 2;
            case TypeWidth.wordWidth -> 1;
            case TypeWidth.byteWidth -> 0;
            default -> 0;
        };
    }

    /**
     * 判断两寄存器是否可同时使用,
     * AH~DH的R9B~R15B不能同时使用
     */
    public static boolean isCompatible(Register r1,Register r2){
        if(r1==r2){
            return true;
        }
        return !(r1.isHB()&&!r2.isHB()&&r2.is64BitNewRegister()
                ||r2.isHB()&&!r1.isHB()&&r1.is64BitNewRegister());
    }

    /**
     * 判断寄存器是否属于R9~R15
     */
    private  boolean is64BitNewRegister(){
        String s=getRegisterName();
        char ch=s.charAt(1);
        return  Character.isDigit(ch)&&isGeneral();
    }

    @Override
    public boolean isDoublePrecision() {
        return regState==SD;
    }

    @Override
    public boolean isSinglePrecision() {
        return regState==SS;
    }

    public boolean HOPartNeedToBeMoved(Type targetType){
        return hasHighByte()&&regState==HB&&targetType.getWidth()>=TypeWidth.wordWidth;
    }

    @Override
    public boolean isRegister() {
        return true;
    }

    @Override
    public boolean isIndex(){
        return registerDecorator.stream()
                .anyMatch(Var::isIndex);
    }

    public Var getIndex(){
        return registerDecorator.stream()
                .filter(Var::isIndex)
                .findFirst().get();
    }

}