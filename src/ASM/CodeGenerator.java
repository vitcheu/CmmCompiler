package ASM;

import ASM.AsmAddress.*;
import ASM.Constants.*;
import ASM.Register.*;
import ASM.Util.AsmOpUtil;
import ASM.Util.LiteralProcessor;
import AST.Callee;
import AST.DEFINE.DeclaredFunction;
import AST.DEFINE.DefinedFunction;
import AST.DEFINE.DefinedVariable;
import AST.DEFINE.Entity;
import AST.EXPR.ExprNode;
import AST.TYPE.BaseType;
import AST.TYPE.TypeNode;
import CompileException.UnImplementedException;
import IR.*;
import IR.Constants.Const;
import IR.Constants.OP;
import IR.Constants.Type;
import IR.Constants.TypeWidth;
import IR.Label;
import IR.Optimise.Loop;
import IR.instruction.CallInstruction;
import IR.instruction.ConditionJump;
import IR.instruction.Instruction;
import IR.instruction.ParamInstruction;
import Parser.Entity.Location;
import Semantic_Analysis.SymbolTable.GlobalTable;
import Semantic_Analysis.SymbolTable.LocalTable;
import Semantic_Analysis.SymbolTable.SymbolTable;
import compile.Compiler;
import compile.CompilerOption;
import utils.Align;
import utils.CalculateTypeWidth;

import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static ASM.Constants.OpBase.*;
import static ASM.Constants.RegisterState.*;
import static ASM.Register.Register.*;
import static compile.CompilerOption.*;
import static compile.Constants.*;
import static IR.Constants.OP.*;
import static IR.Constants.Type.*;

public class CodeGenerator {
    public static final String EXTERNDEF = "externdef";
    public static final String EXTRN = "extrn";
    //比较码反转
    private static final HashMap<OP, OP> relOpMap = new HashMap<>();
    //中间操作码至目标操作吗
    private static final HashMap<OP, OpBase> opMap = new HashMap<>();
    //比较码至跳转码
    private static final HashMap<OP, OpBase> relToJump = new HashMap<>();
    //比较码至指令后缀
    private static final HashMap<OP, OpPostfix> relToPostfix = new HashMap<>();
    private static final HashMap<OpPostfix, OpPostfix> toSignedPostfix = new HashMap<>();
    //用于无符号数和浮点数的比较
    private static final HashMap<OpBase, OpBase> toUnsignedJump = new HashMap<>();
    //当前函数调用的参数列表
    private static List<Value> curParams = new LinkedList<>();

    static {
        opMap.put(add, ADD);
        opMap.put(sub, SUB);
        opMap.put(mul, IMUL);
        opMap.put(div, DIV);

        relOpMap.put(gt, le);
        relOpMap.put(ge, lt);
        relOpMap.put(eq, ne);

        relOpMap.put(ne, eq);
        relOpMap.put(le, gt);
        relOpMap.put(lt, ge);

        relToJump.put(gt, JG);
        relToJump.put(ge, JGE);
        relToJump.put(eq, JZ);
        relToJump.put(ne, JNZ);
        relToJump.put(le, JLE);
        relToJump.put(lt, JL);

        toUnsignedJump.put(JG, JA);
        toUnsignedJump.put(JGE, JAE);
        toUnsignedJump.put(JZ, JZ);
        toUnsignedJump.put(JNZ, JNZ);
        toUnsignedJump.put(JLE, JBE);
        toUnsignedJump.put(JL, JB);

        relToPostfix.put(gt, OpPostfix.a);
        relToPostfix.put(ge, OpPostfix.ae);
        relToPostfix.put(eq, OpPostfix.z);
        relToPostfix.put(ne, OpPostfix.nz);
        relToPostfix.put(le, OpPostfix.b);
        relToPostfix.put(lt, OpPostfix.be);

        toSignedPostfix.put(OpPostfix.a, OpPostfix.g);
        toSignedPostfix.put(OpPostfix.ae, OpPostfix.ge);
        toSignedPostfix.put(OpPostfix.z, OpPostfix.z);
        toSignedPostfix.put(OpPostfix.nz, OpPostfix.nz);
        toSignedPostfix.put(OpPostfix.b, OpPostfix.l);
        toSignedPostfix.put(OpPostfix.be, OpPostfix.le);

    }

    private final GlobalTable globalTable;
    private List<Block> blocks;
    private Address desAddr;
    private Address srcAddr;
    private PrintWriter pw;
    //当前优先级较高的寄存器
    private List<Register> preferRegs = new LinkedList<>();
    private Label curLabel;
    //当前中间指令号
    private int curIRid;
    private Location curLocation;
    private Block curBlock;
    private Loop curLoop;
    private List<Register> curLoadedRegs;
    private boolean hasMemOperand = false;
    private List<Value> paramList = new LinkedList<>();
    private Map<Value, List<Instruction>> storeInstructions = new HashMap<>();
    private Map<Value, ParamInstruction> findParamInstr = new HashMap<>();
    private Map<Var, Boolean> activeMap = new HashMap<>();
    private List<ASM> codes = new LinkedList<>();
    private List<ASM> dataDeclarations = new LinkedList<>();
    private List<ASM> constDeclarations = new LinkedList<>();
    private ProcessInfo processInfo = new ProcessInfo();

    public CodeGenerator(PrintWriter pw, List<Block> blocks, GlobalTable globalTable) {
        this.pw = pw;
        Register.pw = pw;
        this.blocks = blocks;
        this.globalTable = globalTable;
    }

    /**
     * 判断操作数是否均为无符号类型
     */
    private static boolean isUnsigned(Value x, Value y) {
        return !x.isSigned() || !y.isSigned();
    }

    /**
     * 判断value是否是当前函数调用的参数
     */
    public static boolean isInCurParamList(Value value) {
        return curParams.contains(value);
    }

    public void generate() {
        allocConstSegment();
        allocDataSegment();

        gen("\t\t.code");
        generateExternDefAndPublicInfo();

        for (Block block : blocks) {
            generateBlock(block);
        }
        gen("END");
        //输出汇编代码文件
        generateAsmFile();
    }

    private void generateBlock(Block block) {
        if (block.isEnd())
            return;
        curBlock = block;
        curLabel=block.getBlockLabel();
        curLoop=curBlock.getAffiliatedLoop();
        pw.println("curLoop="+curLoop);

        block.printSep(pw, true);
        Register.clearAllRegs();
        /*重新设置变量的活跃状态*/
        activeMap.entrySet().stream()
                .forEach(e -> {
                    Var var = e.getKey();
                    var.setActive(false);
                    e.setValue(false);
                });
//        activeMap.clear();
        block.setActiveWhenEnter();

        if (block.getPreBlocks().size() >= 2 || block.isStart()) {
            pw.println("#清除所有寄存器的内容");
        } else {
            if (!block.getPreBlocks().isEmpty()) {
                pw.println("#延续父基本块的内容");
                Block parent = block.getPreBlocks().get(0);
                /*延续父基本块执行内容*/
                HashMap<Register, RegisterContent> contents = parent.getContents();
                if (contents != null)
                    Register.restoreAllRegisters(contents);
            }
        }

        if(Debug)
            genComment(block.toString());
        storeVarsWhenEnter(block);

        pw.println("#Loaded vars:" + block.getAllocationTable());
        RegisterLoader.setContentOfRegisters(block);

        if(curLoop!=null&&curLoop.getAllocationTable()!=null)
            curLoadedRegs =new ArrayList<>( curLoop.getAllocationTable().values().stream().toList());

        pw.println("#加载后,寄存器的内容:");
        printRegister();

        if(block.getInstructions().isEmpty()){
            loadVarsWhenExit(block);
        }
        for (Instruction instr : block.getInstructions().stream().
                filter(Instruction::isTranslateImmediately).toList()) {
            if (curBlock.isLastInstr(instr)) {
                if (instr.jumpToOtherBlock()) {
                    loadVarsWhenExit(block);
                    translate(instr);
                } else {
                    translate(instr);
                    loadVarsWhenExit(block);
                }
            } else {
                translate(instr);
            }
        }

        /*保存寄存器内容*/
        HashMap<Register, RegisterContent> contents = Register.storeAllRegisters();
        block.setContents(contents);

        curLoadedRegs = null;
    }

    /**
     * 在作为循环出口的基本块开头处溢出变量到内存
     */
    private void storeVarsWhenEnter(Block block) {
        List<Register> storeLoadedRegs=curLoadedRegs;
        if(curLoadedRegs!=null){
            curLoadedRegs=null;
        }
        var storeTable=block.getNeedStoreVarsTable();
        if (storeTable!= null) {
            if(Debug)
                genComment("Store Vars when EXIT of Loop");
            else
                gen("");
            storeVars(storeTable);
        }
        curLoadedRegs=storeLoadedRegs;
    }

    private void storeVars(Map<Var, Register> table) {
        pw.println("#StoreVars, allocRegs=" + table);
        table.entrySet().stream()
                .forEach(entry -> {
                    Var var = entry.getKey();
                    Register r = entry.getValue();
                    tryStoreToMem(var, r);
                });
    }

    private void loadVars(Map<Var,Register> table) {
        List<Register> regs=table.values().stream().toList();
        table.keySet().stream()
                .forEach(var -> {
                    Register r = table.get(var);
                    if (var.getMemRef() != null) {
                        moveValueToSpecificReg(var, r,true);
                        r.setUsing(true);
                    }
                    if(curLoadedRegs!=null)
                        curLoadedRegs.add(r);
                });
        regs.forEach(r->r.setUsing(false));
    }

    /**
     * 在作为循环入口的基本块结尾处加载变量到寄存器
     */
    private void loadVarsWhenExit(Block block) {
        reLoadVarsWhenExit(block);
        var allocationTable = block.getNeedLoadVarsTable();
        var table = allocationTable;
        if (allocationTable != null) {
            if(Debug)
                genComment("Loading, table=" + table);
            else
                gen("");
            loadVars(table);
        }
    }

    private void reLoadVarsWhenExit(Block block) {
        if(curLoop==null)
            return;

        var table=block.getAllocationTable();
        if (table!= null) {
            Map<Var,Register> map;
            var filterLst = table.keySet().stream()
                    .filter(var -> {
                        var allocated = getCurAllocatedReg(var);
                        return allocated != null && !allocated.containValue(var);
                    })
                    .toList();
            map = filterLst.stream()
                    .collect(Collectors.toMap(var -> var, table::get, (a, b) -> b));
            if(curLoadedRegs!=null)
                curLoadedRegs.removeAll(map.values());
            filterLst.stream().
                    filter(var -> var.getMemRef() == null && var.isTemp())
                    .map(var -> (Temp) var).
                    forEachOrdered(temp -> {
                       MemoryAddress mem=  allocMemAddrForTemp(temp);
                       temp.addAddress(mem);
                       temp.setMemRef((MemRef) mem);
                    });
            if(!map.isEmpty())
                if(Debug)
                    genComment("Reload Vars when exit");
            loadVars(map);
        }
    }

    private void generateExternDefAndPublicInfo() {
        List<Entity> entities = globalTable.getDeclaredEntities();
        entities.stream().filter(DeclaredFunction.class::isInstance)
                .map(DeclaredFunction.class::cast)
                //如果是外部函数,只有在本编译单元被引用过才生成声明
                .filter(fun->
                        fun.isDefined() || fun.getReferenceCnt() > 0)
                .forEach(fun->
                        gen(String.format("\t\t%s\t" + "%s:proc", EXTERNDEF, fun.getName())));

        entities.stream().filter(e -> e instanceof DefinedVariable
                                    && e.getReferenceCnt() > 0)
                .map(e -> (DefinedVariable) e)
                .forEach(var ->
                    declareVar(var, var.isConst(), true));

        List<Entity> localEntities =globalTable.getLocalEntities();

        //公共函数的声明
        localEntities.stream().filter(e -> !e.isPriv() && (e instanceof DefinedFunction))
                .map(DefinedFunction.class::cast)
                .map(DefinedFunction::getName)
                .forEach(name -> gen("\t\tpublic\t\t%s".formatted(name)));
    }

    public void setDesAddr(Address desAddr) {
        releaseAddr(desAddr);
        this.desAddr = desAddr;
        setUsing(desAddr, true);
    }

    public void setSrcAddr(Address srcAddr) {
        releaseAddr(srcAddr);
        this.srcAddr = srcAddr;
        setUsing(srcAddr, true);
    }

    public void setUsing(Address addr, boolean using) {
        if (addr instanceof Register r)
            r.setUsing(using);
        else if (addr instanceof MemoryAddress mem) {
            List<Register> dependents = mem.getDependentRegs();
            if (dependents != null) {
                dependents.forEach(r -> r.setUsing(using));
            }
        }
    }

    /**
     * 将中间代码翻译为机器代码
     */
    public void translate(Instruction instr) {
        //可能某条中间语句被省略,而它的标签需要保留
        if (curLabel != null && instr.getLabel() == null) {
            ;
        }
        else if (curLabel == null) {
            //不在基本块的开头取label
            if(!curBlock.isFirstInstr(instr))
                curLabel = instr.getLabel();
        }
        //两个标签冲突!
        else if(!curLabel.equals(instr.getLabel())){
            pw.println("两个标签冲突!:" + curLabel + " vs " + instr.getLabel());
            Label newLbl = instr.getLabel();
            newLbl.setImage(curLabel);
        }
        curIRid = instr.getId();
        curLocation = instr.getLocation();

        OP op = instr.getOp();
        Value y = instr.getArg1(),
                z = instr.getArg2();
        Result result = instr.getResult();
        setActiveInfo(y, z, result, instr.getActiveInfo());
        hasMemOperand = false;
        printLine();
        pw.println(String.format("翻译指令:%-25s\t@@%-6s", instr,
                instr.getLocation().getLine()));

        switch (op) {
            case assign -> {
                translateAssign(y, (Var) result);
            }
            case mul, add, sub -> {
                translateArithmetic(y, z, result, op);
            }
            case mod, div -> {
                if ((y instanceof Literal) && (z instanceof Literal)) {
                    throw new RuntimeException("现在还不能处理操作数都是字面量的中间指令," +
                            "y=" + y + ",z=" + z);
                }
                Type ty = y.getIRType(), tz = z.getIRType();
                //整数除法
                if ((ty.isIntType()) && (tz.isIntType())) {
                    translateIntDivOrMul(y, z, result, op);
                }
                //浮点数除法
                else {
                    translateArithmetic(y, z, result, op);
                }
            }
            case cast -> {
                Type type = (Type) z;
                translateCast(y, type, result);
            }
            case lea -> {
                translateLea(y, (Var) result);
            }
            case deref -> {
                Var x = (Var) result;
                MemoryAddress mem = null;
                Address addrY = chooseAddrForValue(y, true);
                setSrcAddr(addrY);
                /*函数指针或函数名的解引用,实际上不需要进行间接内存访问,只需获取指针的右值*/
                if(y instanceof Var vy&&vy.isFunctionPointer()||(y instanceof DefinedFunction)) {
                    x.addAddress(addrY);
                    if(addrY instanceof Register r){
                        r.addVariable(x);
                    }
                } else  {
                    boolean indirectAccess = true;
                    //y为一个临时变量,则表示此时解引用的对象是一个中间结果
                    //中间结果的pointingAddr存放了所需的内存访问
                    if (y instanceof Temp) {
                        mem = ((Temp) y).getPointingAddr();
                    }
                    //mem为直接地址,解引用为本身,类型为结果类型
                    if (mem instanceof DirectAddress) {
                        mem = mem.getDerefAddr(x);
                        indirectAccess = false;
                    } else {
                        if (!(srcAddr instanceof Register)) {
                            //强制将y的右值(一个地址)装入一个寄存器
                            Register r = loadVar((Var) y);
                            changDesOrSrcAddr(srcAddr, r, false);
                        }
                    }
//                else if(!mem.isLeftValue()&&!(srcAddr instanceof R)){
//                    Register genReg=findAvailableReg(pointer);
//                    changDesOrSrcAddr(srcAddr,genReg,false);
//                    genMove(genReg,mem);
//                }
                    //用寄存器间接访问
                    if (indirectAccess)
                        mem = new MemRef(0, (Register) srcAddr, x);

                    setPointingAddr(x, mem);
                }
            }
            case array -> {
                Var x = (Var) result;
                setSrcAddr(chooseAddrForValue(z, true));
                /*强制加载至寄存器*/
                if (srcAddr instanceof MemoryAddress mem) {
                    Register tempR = loadValueToTempR(z);
                    changDesOrSrcAddr(srcAddr, tempR, false);
                }

                Address baseAddr = getArrayBaseAddr(y);
                pw.println(String.format("#array,src=%s,baseAddr=%s\n#isLeftValue?: %s, isArrayAddr?: %s", srcAddr, baseAddr,
                        x.isLeftValue(), x.isArrayAddr()));

                /*形成需要访问的地址*/
                /**
                 *  由gen(ADD,...)调用优化此情况,直接形成新的内存地址,避免生成add指令
                 *  当数组访问是实际是指针访问时,不能进行这种优化
                 */
                boolean canBeOptimized =
                        (baseAddr instanceof MemoryAddress) && (srcAddr instanceof Literal)
                                && ((y instanceof Var varY) && (varY.isArrayAddr()));
                if (canBeOptimized) {
                    var baseMem = (MemoryAddress) baseAddr;
                    //如果数组基址是一个右值,获取它的左值
                    baseAddr = baseMem.getAddressOpResult();
                    setDesAddr(baseAddr);
                } else if (baseAddr instanceof MemoryAddress baseMem && (!baseMem.canBeArrayBase() ||
                        !((Var) y).isArrayAddr())) {
//                    //左值化
//                    baseAddr=baseMem.getAddressOpResult();
                    /*pr储存数组基址*/
                    Register pr = findAvailableReg(pointer);
                    genMove(pr, baseAddr);
                    ((Var) y).addAddress(pr);
                    pr.addVariable((Var) y);
                    Address des = pr;
                    /*当源操作数时字面量时,可以优化内存访问*/
                    if (srcAddr instanceof Literal) {
                        //形成内存操作数
                        des = MemRef.getMemRefBaseOnR(pr, x);
                    }
                    setDesAddr(des);
                } else {
                    setDesAddr(baseAddr);
                }
                /*防止baseAddr被溢出*/
                if (baseAddr instanceof Register baseR)
                    baseR.setUsing(true);

                //需要清空偏移量寄存器的高32位
                if (srcAddr instanceof Register srcR) {
                    if((curLoadedRegs==null||!curLoadedRegs.contains(srcR)))
                        /*零扩展至指针宽度*/
                         zeroOrSignExtend(srcR, pointer, false, false);
                    srcR.shiftToState(pointer);
                }

                //地址值加常量,优化成基址加偏移量的寻址
                if ((desAddr instanceof MemoryAddress) && (srcAddr instanceof Literal)) {
                    //desAddr代表一个右值,不能进行地址直接相加
                    if (!desAddr.isLeftValue()) {
                        pw.println("#" + desAddr + " is left value");
                        throw new RuntimeException("should not happend");
                    }

                    //直接在该地址上加上偏移量
                    MemoryAddress mem = (MemoryAddress) desAddr;
                    int bias = Integer.parseInt(((Literal) srcAddr).getLxrValue());
                    mem = mem.getAddressAdditionAddr(bias);
                    pw.println("#mem=" + mem + ",isLeftValue?" + mem.isLeftValue() + "\n#class="
                            + mem.getClass().getSimpleName());
                    changDesOrSrcAddr(desAddr, mem, true);
                }
                
                //PC相对地址加寄存器,此种间接寻址方式在64位长模式下不可用,故需要转为取址和相加指令
                else if(desAddr instanceof DirectAddress direct && srcAddr instanceof Register srcR
                        && !LARGE_ADDR){
                    Register tempR=findAvailableReg(pointer);
                    leaAddr(tempR,direct);
                    gen(ADD,tempR,srcR);
                    MemRef mem=new MemRef(0,tempR,x.getIRType());
                    changDesOrSrcAddr(desAddr,mem,true);
                }

                else {
                    MemoryAddress mem;
                    if (srcAddr instanceof Register srcR) {
                        int scale = 1;
                        if (srcR.isIndex()) {
                            Temp t = (Temp) srcR.getIndex();
                            scale = t.getScale();
                        }
                        mem = MemRefAddress.getAddressAdditionAddr(desAddr, srcR, scale, x.getIRType());
                        /*返回原模式*/
                        srcR.shiftToState(int32);
                    } else if (srcAddr instanceof Literal literal) {
                        mem = MemRefAddress.getAddressAdditionAddr(desAddr, srcAddr, 1, x.getIRType());
                    } else {
                        throw new RuntimeException(String.format("should not happen,desAddr=%s,srcAddr=%s,curId=%d",
                                desAddr, srcAddr, curIRid));
                    }
                    changDesOrSrcAddr(desAddr, mem, true);
                }

                pw.println("#array,desAddr=" + desAddr);

                //尽快地加载到寄存器
                setPointingAddr(x, (MemoryAddress) desAddr);
                if (baseAddr instanceof Register baseR)
                    baseR.setUsing(false);
            }
            case minus -> {
                setSrcAddr(chooseAddrForValue(y, true));
                if (srcAddr instanceof Register) {
                    setSrcAddr(moveReg((Register) srcAddr, (Var) y, true));
                } else {
                    Register tempR = findAvailableReg(result.getIRType());
                    genMove(tempR, srcAddr);
                    setSrcAddr(tempR);
                }
                gen(NEG, srcAddr, null);
                setDesAddr(srcAddr);
                loadResultVar(desAddr, result);
            }


            /**
             * 移动指令
             */
            case shift_left, shift_right -> {
                translateShift(op, y, z, result);
            }


            /**
             * 跳转指令
             */
            case jump -> {
                Label label = (Label) result;
                gen(JMP, label, null);
            }
            case if_jump, ifFalse_jump -> {
                boolean operandReversed = false;
                if ((y instanceof Literal) && (z instanceof Literal)) {
                    throw new RuntimeException("should not happend");
                } else if ((y instanceof Literal) && z != null) {
                    Value t = y;
                    y = z;
                    z = t;
                    operandReversed = true;
                }

                OP jumpOp = null;
                if (instr instanceof ConditionJump conJump) {
                    jumpOp = conJump.getJumpType();
                }
                //将ifFalse中的比较符反转
                if (op == ifFalse_jump && jumpOp != null) {
                    jumpOp = relOpMap.get(jumpOp);
                }
                if (operandReversed) {
                    assert jumpOp != null;
                    jumpOp = switch (jumpOp) {
                        case ge -> le;
                        case le -> ge;
                        case gt -> lt;
                        case lt -> gt;
                        case eq -> eq;
                        case ne -> ne;
                        default -> null;
                    };
                }
                //生成指令
                if (jumpOp != null) {
                    Address addrY = chooseAddrForValue(y, true, false, false),
                            addrZ;
                    setSrcAddr(addrY);
                    addrZ = chooseAddrForValue(z, true, false, false);
                    setDesAddr(addrZ);

                    genCmp(y, addrY, addrZ);
                    OpBase asmOP = relToJump.get(jumpOp);
                    if (y.getIRType().isFloatType() || !y.isSigned()) {
                        asmOP = toUnsignedJump.get(asmOP);
                    }
                    gen(asmOP, (Label) result, null);
                }
                //形如if(a)这样的条件表达式
                else {
                    OpBase asmOP = (op == if_jump) ? JNZ : JZ;
                    //若是立即数,进行编译器计算并直接跳转
                    if (y instanceof Literal) {
                        String lxr = ((Literal) y).getLxrValue();
                        if (lxr.equals("0") && asmOP == JZ || !lxr.equals("0") && asmOP == JNZ) {
                            gen(JMP, (Label) result, null);
                        }
                        //不生成语句,保留标签
                        ;
                    } else {
                        Address addrY = chooseAddrForValue(y, true);
                        //如果上条指令已经设置了条件码,则不必生成比较指令
                        if (!flagUsable()) {
                            genCmp(y, addrY, new Literal("0"));
                        }
                        gen(asmOP, (Label) result, null);
                    }
                }
            }


            /**
             * 比较指令
             */
            case gt, lt, ge, le, eq, ne -> {
                if ((y instanceof Literal) && (z instanceof Literal)) {
                    throw new RuntimeException("现在还不能处理操作数都是字面量的中间指令");
                } else if ((y instanceof Literal) && z != null) {
                    Value t = y;
                    y = z;
                    z = t;
                }

                Address addrY = chooseAddrForValue(y, true, false, false),
                        addrZ, addrX;
                setSrcAddr(addrY);
                addrZ = chooseAddrForValue(z, true, false, false);
                setDesAddr(addrZ);

                translateDirectRel(y, z, addrY, addrZ, (Var) result, op);
            }


            /**
             * 过程和块
             */
            case enter -> {
                translateEnter(instr);
            }
            case leave -> {
                translateLeave();
            }
            case ret -> {
                translateRet(y, instr.isLastRet());
            }
            case param -> {
                translateParam((ParamInstruction) instr);
            }
            case call -> {
                translateCall((CallInstruction) instr);
            }
            default -> {
            }
        }

        releaseAddr(srcAddr);
        releaseAddr(desAddr);
        if (result instanceof Var) {
            tryStoreToMem((Var) result);
        }
        resetOperandInfo(y);
        resetOperandInfo(z);
        if (result instanceof Var)
            resetOperandInfo(result);
//        if (!values.isEmpty()) {
//            for (Value v : values) {
//                tryStoreToMem((Var) v);
//            }
//        }
        Register.printRegister();
        pw.println("变量地址:");
        printAddr(y);
        printAddr(z);
        printAddr(result);
    }

    private void releaseAddr(Address address) {
        setUsing(address, false);
    }

    private void changDesOrSrcAddr(Address oriAddr, Address newAddr, boolean isDes) {
        releaseAddr(oriAddr);
        if (isDes) {
            setDesAddr(newAddr);
        } else {
            setSrcAddr(newAddr);
        }
    }

    private void printAddr(Value v) {
        if (v instanceof Var var) {
            String s = "";
            if (!var.getAddrDecorator().isEmpty())
                s += var.getAddrDecorator().toString();
            if ((var instanceof Temp) && (((Temp) var).getPointingAddr() != null)) {
                if (s.length() > 2) {
                    s = s.substring(0, s.length() - 1);
                    s += ",";
                } else {
                    s += "[";
                }
                s += ((Temp) var).getPointingAddr().toString() + "]";
                Temp t = (Temp) var;
                if (t.getWriteBackAddr() != null) {
                    s += "\tneedWriteBack?: " + t.isNeedWriteBack();
                    if (t.isNeedWriteBack()) {
                        s += "\t" + t.getWriteBackAddr();
                    }
                }
            }
            pw.println("Addr of " + v + ":\t" + s);
        }
    }

    /**
     * 翻译语句x=y relop z
     */
    private void translateDirectRel(Value y, Value z, Address addrY, Address addrZ, Var x, OP op) {
        Address addrX = chooseAddrForValue(x, false);
        if (y instanceof Literal ly && z instanceof Literal lz) {
            calLiteralRel(ly, lz, addrX, op);
        } else {
            //弄干净寄存器,同时默认置0
            if (addrX instanceof Register) {
                cleanRegister((Register) addrX);
            } else {
                throw new RuntimeException("目标操作数需为寄存器");
            }

            genCmp(y, addrY, addrZ);
            OpPostfix postfix = relToPostfix.get(op);
            //float使用无符号跳转
            if (!y.getIRType().isFloatType() && !isUnsigned(y, z))
                postfix = toSignedPostfix.get(postfix);
            gen(set, postfix, addrX, null);
        }

        loadResultVar(addrX, x);
    }

    private void calLiteralRel(Literal ly, Literal lz, Address addrX, OP op) {
        Type yt = ly.getIRType(), zt = lz.getIRType();
        if (yt.isFloatType() && !zt.isFloatType()
                || !yt.isFloatType() && zt.isFloatType()) {
            throw new RuntimeException("should not happen,yt=" + yt + ",zt=" + zt);
        }

        boolean result = false;
        if (yt.isFloatType() && zt.isFloatType()) {
            String s1 = SymbolTable.trimFloat(ly.getLxrValue(),yt.isSinglePrecision()),
                    s2 = SymbolTable.trimFloat( lz.getLxrValue(),zt.isSinglePrecision());
            double dy = Double.parseDouble(s1), dz = Double.parseDouble(s2);
            result = switch (op) {
                case gt -> dy > dz;
                case ge -> dy >= dz;
                case eq -> dy == dz;
                case ne -> dy != dz;
                case le -> dy <= dz;
                case lt -> dy < dz;
                default -> false;
            };
        } else if (!yt.isFloatType() && !zt.isFloatType()) {
            long y = Long.parseLong(ly.getLxrValue()), z = Long.parseLong(lz.getLxrValue());
            result = switch (op) {
                case gt -> y > z;
                case ge -> y >= z;
                case eq -> y == z;
                case ne -> y != z;
                case le -> y <= z;
                case lt -> y < z;
                default -> false;
            };
        }
        if (!(addrX instanceof Register)) {
            Register tempR = findAvailableReg(addrX.getIRType());
            addrX = tempR;
        }
        genMove(addrX, new Literal(result ? "1" : "0", boolType));
    }

    private Address getArrayBaseAddr(Value array) {
        //加载数组基址
        Address baseAddr = null;
        if (array instanceof Literal)
            throw new UnImplementedException("未实现常量地址为基址的数组访问");
        Var varray = (Var) array;
        if (array instanceof Temp ty && ty.getPointingAddr() != null && ty.getRegisters().isEmpty()) {
            //array是一个可修改的左值,为deref或成员访问或数组访问的结果值
            baseAddr = ty.getPointingAddr();
        }
        /*优先选择非寄存器作为基址*/
        else if (varray.getMemAddr() instanceof DirectAddress) {
            baseAddr = varray.getMemAddr();
        } else if (!varray.isTemp() && varray.getRegisters().isEmpty()) {
            baseAddr = varray.getMemAddr();
        } else {
            baseAddr = chooseAddrForValue(array, varray.isArrayAddr(), false, false);
        }
        if (baseAddr == null) {
            throw new RuntimeException("array=" + array);
        }
        return baseAddr;
    }

    private void translateAssign(Value y, Var x) {
        Address addrX = null, addrY = null;
        //字符串字面量对字符串数组的赋值
        if (y.getIRType() == StringLiteral && x.isArrayAddr()) {
            Literal ly = (Literal) y;
            DirectAddress directAddr = globalTable.getStringLiteral(ly);
            addrX = getArrayBaseAddr(x);
            int length = LiteralProcessor.calculateLen(ly.getLxrValue());
            strAssign(directAddr, addrX, length);
        } else {
            addrY = chooseAddrForValue(y, true);
            pw.println("#addrY=" + addrY);
            setSrcAddr(addrY);
            //此情况可以直接在y的寄存器描述符中增加x
            if ((addrY instanceof Register) && (x.isActive()) && !x.isLeftValue()) {
                //x活跃,y的寄存器转入x
                Register ry = (Register) addrY;
                ry.addVariable(x);
                addrX = ry;
            } else {
                //优先选择内存地址
                if (!x.isActive() && !x.isLeftValue() && x.getMemAddr() != null) {
                    addrX = x.getMemAddr();
                } else {
                    addrX = chooseAddrForValue(x, false, false, false);
                }
                setDesAddr(addrX);
                pw.println("#addrX=" + addrX);
                //addrY为内存引用或pc相对寻址
                if (addrY instanceof MemoryAddress) {
                    if (!(addrX instanceof Register)) {
                        //先把y的值复制到中间寄存器
                        Register tempR = findAvailableReg(y.getIRType());
                        loadValue(tempR, y, addrY);
                        pw.println("move addrY to temp :" + tempR);
                        addrY = tempR;
                    }
                } else if (addrY instanceof Register ry) {
                    ry.addVariable(x);
                }
                genMove(addrX, addrY);

                //修该目标寄存器的描述符
                if ((addrX instanceof Register rx)) {
                    rx.setUniqueVar(x);
                    if (y instanceof Var)
                        rx.addVariable((Var) y);
                }
//            loadNewValue(addrX,x);
            }

            //x表示普通变量
            if (!x.isLeftValue()) {
                x.setUniqueAddress(addrX);
                if (addrY instanceof Register)
                    x.addAddress(addrY);
            }
            if (addrY instanceof Register) {
                ((Register) addrY).addVariable(x);
            }
        }
    }

    /**
     * 翻译字符串赋值语句
     */
    private void strAssign(DirectAddress strAddr, Address desAddr, int length) {
//        Address des=chooseAddrForValue(x,true,true,RDI);
        moveAddr(RSI);
        moveAddr(RDI);
        moveAddr(RCX);
        gen(LEA, RSI, strAddr);
        gen(LEA, RDI, desAddr);
        Literal lenLiteral = new Literal(length);
        genMove(RCX, lenLiteral);
        genStrInstr(MOVS, OpPostfix.B);
    }

    private void translateCastToBool(Value y, Address addrY, Var x) {
        //注意不必移动addrY,这是调用方的责任
        if (!y.getIRType().isFloatType()) {
            Literal zero = Literal.getZero(y.getIRType());
            translateDirectRel(y, zero, addrY, zero, x, ne);
        } else {
            //将零加入到浮点字面量表
            DirectAddress zero = addZeroLiteral(y.getIRType().isSinglePrecision());
            Literal zeroLxr = zero.getIRType().isSinglePrecision() ? Literal.ZERO_IN_FLOAT : Literal.ZERO_IN_FLOAT;

            //maybe should load zero to reg
            if (y instanceof Var) {

            } else {
                pw.println("y=" + y);
            }
            translateDirectRel(y, zeroLxr, addrY, zero, x, ne);
        }
    }

    private void translateCast(Value y, Type type, Result result) {
        pw.println("#y=" + y + ",src type=" + y.getIRType() + ",target type=" + type);
        //yt -> xt
        Var x = (Var) result;
        Type yt = y.getIRType(), xt = x.getIRType();


        boolean isSameKind = !yt.isFloatType() && !xt.isFloatType()
                || yt.isFloatType() && xt.isFloatType();
        boolean needMove = (isSameKind && yt.getWidth() != xt.getWidth())
                && !(type == boolType && y instanceof Literal);//当字面量转换至bool时,不需要转载至寄存器

        //valueChanging为false
        Address addrY = chooseAddrForValue(y, true, needMove, false),
                addrX;

        /*转换为布尔值需要特殊翻译*/
        if (type == boolType) {
            //addrY转换为bool类型
            translateCastToBool(y, addrY, x);
//            setDesAddr(addrY);
        } else {
            //尝试原地寄存器扩展
            if (addrY instanceof Register ry) {
                //同类型的寄存器相互转换
                if (isSameKind) {
                    //高位字节不能直接原地转换
                    if (ry.isHB() && yt.getWidth() < xt.getWidth()) {
                        addrX = findAvailableReg(x.getIRType());
                        Register rx = (Register) addrX;
                        genExtensionMove(rx, ry, MOVSX);
                        loadResultVar(rx, x);
                    } else {
                        locallyConvert(ry, y, x);
                    }

                    return;
                }
            }
            setSrcAddr(addrY);
            addrX = chooseAddrForValue(x, false, false, false);
            setDesAddr(addrX);

            pw.println("addrY=" + addrY + ",addrX=" + addrX);
            genConvert(y.getIRType());
            loadResultVar(desAddr, x);
        }
    }

    /**
     * 原地寄存器进行类型转换
     */
    private void locallyConvert(Register ry, Value y, Var x) {
        Type xt = x.getIRType(), yt = y.getIRType();
        //宽度一致
        if (yt == xt) {
            ry.addVariable(x);
        } else {
            //浮点数之间转换
            if (xt.isFloatType()) {
                OpBase base = CVT;
                OpPostfix postfix;
                //单精度->双精度
                if (yt.isSinglePrecision()) {
                    postfix = OpPostfix.SS2SD;
                } else {
                    postfix = OpPostfix.SD2SS;
                }
                gen(base, postfix, ry, ry);
            }
            //整数之间转换
            else {
                //宽化,需保证高位是干净的
                if (yt.getWidth() < xt.getWidth()) {
                    zeroOrSignExtend(ry, xt, y.isSigned(), x.isSigned());
                }
            }

            ry.setUniqueVar(x);
            ry.shiftToState(xt);
        }

        x.setUniqueAddress(ry);
    }

    private void translateLea(Value y, Var result) {
        MemoryAddress mem=null;

        if (y instanceof Var vy) {
            if (y instanceof Temp ty) {
                Register ry = null;
                if (ty.getPointingAddr() != null) {
                    MemoryAddress memoryAddress = ty.getPointingAddr();
                    leaMemRef(memoryAddress, result);
                    return;

                } else if (vy.isArrayAddr()) {
                    ry = (Register) chooseAddrForValue(vy, true, false, false);
                }

                result.addAddress(ry);
                ry.addVariable(result);
                return;
            }
            if (vy.getMemAddr() == null) {
                throw new RuntimeException("无法找到" + vy + "的地址," + vy);
            }

            //加载非临时变量的地址
            mem = vy.getMemAddr();

            //继续寻找y的地址
            if (mem == null) {
                if (vy.getAddrDecorator().isEmpty()) {
                    throw new RuntimeException("无法找到变量" + vy + "的值");
                }

                Address addrY = chooseAddrForValue(vy, true, false, false);
                if (addrY instanceof Register ry) {
                    //这里假设y是由指针引用,数组访问或成员访问所得到的地址
                    //属于数组名,数组名左值和右值都是其基地址
                    if (vy.isArrayAddr()) {
                        //不用生成任何语句
                        if (result instanceof Var) {
                            result.addAddress(ry);
                            ry.addVariable(result);
                        }
                        return;
                    }
                } else {
                    throw new RuntimeException("addrY应该为寄存器!");
                }
            }
        }else if( y instanceof DefinedFunction function){
            mem=new  DirectAddress(function.getName());
        }else{
            throw new RuntimeException();
        }

        leaMemRef(mem, result);
    }

    /**
     * 对内存地址操作数取引用
     */
    private void leaMemRef(MemoryAddress memoryAddress, Var result) {
//        gen(LEA,desAddr,mem);
//         loadResultVar(desAddr,result);

        //不生成lea指令,直接返回该地址,返回结果是一个左值

        MemoryAddress mem = memoryAddress.getAddressOpResult();
        pw.println("#lea,src addr=" + memoryAddress);
        pw.println("#result addr=" + mem);
        loadResultVar(mem, result);
    }

    private void translateShift(OP op, Value y, Value z, Result result) {
        Address addrZ, addrY, srcR;
        addrZ = chooseAddrForValue(z, true, false, false);
        srcR = addrZ;

        addrY = chooseAddrForValue(y, true, true, false);

        OpBase base;
        if (op == shift_left) {
            base = (isUnsigned(y, z)) ? SHL : SAL;
        } else {
            base = (isUnsigned(y, z)) ? SHR : SAR;
        }

        if (!(addrZ instanceof Literal)) {
            //将z的值装入cl,z的值小于8位由语义分析阶段保证
            Register reg = RCX;
            pw.printf("#must move rcx, content:%s%n", RCX.getDescription());
            Register newR= moveReg(reg, (Var) z, false);
            if(addrY.equals(reg)){
                addrY=newR;
            }
            setDesAddr(addrY);

            if (reg.getRegState() != B) {
                reg.shiftToState(byteType);
            }
            if (addrZ != reg) {
                pw.printf("#%s load to cl,z.type=%s%n", z, z.getIRType());
                genMove(reg, addrZ);
                srcR = reg;
            }
            setSrcAddr(reg);
        }else{
            setDesAddr(addrY);
            setSrcAddr(addrZ);
        }

        gen(base, desAddr, srcR);

        loadResultVar(desAddr, result);
    }

    /**
     * 以单指令的形式翻译整数除法,乘法和取模运算
     */
    private void translateIntDivOrMul(Value y, Value z, Result result, OP op) {
        //交换y和z
        if (y instanceof Literal && (z instanceof Var)) {
            Value temp = y;
            y = z;
            z = temp;
        }
        //如果z是2的正整数次幂,用更为简捷的移动指令
        if (y instanceof Var && z instanceof Literal && (op == div)) {
            int intZ = Integer.parseInt(((Literal) z).getLxrValue());
            if (intZ > 0 && Integer.bitCount(intZ) == 1) {
                int n = (int) (Math.log(intZ) / Math.log(2));
                translateShift(shift_right, y, new Literal(n), result);
                return;
            }
        }

        //设置目标寄存器
        Register resultReg = ((op == div || op == mul) ? RAX : RDX);
        setDesAddr(resultReg);
        Var x = (Var) result;
        boolean isUnsigned = isUnsigned(y, z);
        boolean containingValue = RAX.containValue(y);

        //构造被除数/乘数
        RDX.setUsing(true);
        moveReg(RDX);
        RDX.clear();
        RDX.shiftToState(x.getIRType());
        zeroOrSignExtendRDX(x.getIRType(), isUnsigned);


        //迁移AX原值
        pw.println("\n###迁移RAX的值");
        RAX.setUsing(true);
        moveReg(RAX, (y instanceof Var) ? (Var) y : null, true);
        RAX.clear();
        pw.println("###迁移RAx的值完毕\n");

        //RAX装入y的值
        if (!containingValue)
            moveValueToReg(y, RAX);

        //迁移结果寄存器的值
        if (y instanceof Var) {
            //为了不影响moveReg的决策
            ((Var) y).setIsOperand(false);
        }
        setDesAddr(moveReg((Register) desAddr, x, true));

        //将结果与目标寄存器绑定
        loadResultVar(desAddr, result);

        //构造除数
        Address addrZ = chooseAddrForValue(z, true, false, false);
        //此类指令的操作数不能为立即数,所以需要临时寄存器
        if (addrZ instanceof Literal) {
            Register tempR = findAvailableReg(addrZ.getIRType());
            genMove(tempR, addrZ);
            setSrcAddr(tempR);
        } else {
            setSrcAddr(addrZ);
        }

        //生成指令
        OpBase base;
        if (op == mul) {
            base = (isUnsigned) ? MUL : IMUL;
        } else {
            base = (isUnsigned) ? DIV : IDIV;
        }
        gen(base, null, srcAddr);
        /*设置另一个寄存器的值*/
        if (y instanceof Var vy) {
            Register otherReg = desAddr.equals(RAX) ? RDX : RAX;
            otherReg.removeVar(vy);
            vy.removeAddress(otherReg);
        }

//        if(op==div)
//            RDX.clear();
//        else RAX.clear();
        RDX.setUsing(false);
        RAX.setUsing(false);
    }

    private void zeroOrSignExtendRDX(Type resultType, boolean isUnsigned) {
        if (isUnsigned) {
            gen(XOR, RDX, RDX);
        } else {
            OpBase base = null;
            switch (resultType.getWidth()) {
                case TypeWidth.wordWidth -> {
                    base = CWD;
                }
                case TypeWidth.dwordWidth -> {
                    base = CDQ;
                }
                case TypeWidth.qwordWidth -> {
                    base = CQO;
                }
            }
            gen(base, null, null);
        }
    }

    private void moveValueToReg(Value y, Register r) {
        r.shiftToState(y.getIRType());
        if (y instanceof Var vy) {
            if (r.containValue(y)) {
                ;
            }
            //如果y的值在某一寄存器中,将它转移到r
            else if (vy.hasLoaded()) {
                Register Ry = chooseFromAddrDecorator(vy, false);
                if (Ry != r)
                    genMove(r, Ry);
            }
            //否则将y加载到r
            else {
                loadVar(r, vy);
            }
        } else {
            genMove(r, (Literal) y);
            r.setUniqueVar(y);
        }
    }

    private void translateArithmetic(Value y, Value z, Result result, OP op) {
        OpBase base = opMap.get(op);
        boolean changeable = op == add || op == mul;

        //将字面量的位置放到最后
        if ((y instanceof Literal) && (z instanceof Literal)) {
            Register r1 = findAvailableReg(y.getIRType());
            setDesAddr(r1);
            genMove(r1, (Literal) y);
            gen(base, desAddr, (Literal) z);
            loadResultVar(r1, result);
            return;

        } else if (changeable && (y instanceof Literal)) {
            Value t = y;
            y = z;
            z = t;
        }

        getReg(y, z, (Var) result, op, changeable);

        //要求目标操作数需为寄存器,故将内存操作数转换为寄存器操作数
        if (base == IMUL) {
            /*选择合适的乘法指令
             * 如果是8位乘法,选择8086风格的单操作数指令
             * 否则选择通用的imul指令
             */
            if (result.getIRType().getWidth() == TypeWidth.byteWidth) {
                translateIntDivOrMul(y, z, result, mul);
                return;
            } else {
                if (desAddr instanceof MemRef mem) {
                    Register temp = findAvailableReg((Var) result);
                    loadValue(temp, y, mem);
                    changDesOrSrcAddr(desAddr, temp, true);
                }
            }
        }

        /*如果是索引值乘元素大小,可不生成这种指令*/
        if (!isIndexMulScale(y, z, result, op)) {
            genArithmeticInstr(base);
        }
        //更改目的寄存器内容
        if (op == mul && (result instanceof Var x && x.isIndex())) {
            Register desR = (Register) desAddr;
            desR.addVariable(x);
            x.setUniqueAddress(desR);
        } else {
            loadResultVar(desAddr, result);
        }
    }

    private boolean isIndexMulScale(Value y, Value z, Result result, OP op) {
        return (op == mul) && (result instanceof Var v && v.isIndex())
                && (z instanceof Literal || y instanceof Literal);
    }

    private void genConvert(Type type) {
        if (desAddr instanceof Register desR && !(desAddr instanceof HBRegister)
                && !(srcAddr instanceof Literal)//源操作数为立即数时,不需要转换寄存器状态
                && !type.isFloatType())//浮点操作数需要生成额外的转换指令,不能简单地转换状态
        {
            //保存原状态
//            RegisterState oriState=desR.getRegState();
            desR.shiftToState(type);

            genMove(desAddr, srcAddr);

            //恢复状态
//            desR.setState(oriState);
        } else {
            genMove(desAddr, srcAddr);
        }
    }

    public LocalTable curTable() {
        return processInfo.processTable;
    }

    private void translateEnter(Instruction instr) {
        LocalTable table = (LocalTable) instr.getArg1();
        //生成函数序言
        processInfo.processTable = table;
        processInfo.macroPos = codes.size();
        gen(curTable().getName() + " PROC ");
        if (!table.getLocalVars().isEmpty()) {
            genComment("function prologue");
        }
        processInfo.ini(0, curIRid, curLocation, codes.size());
        //为局部变量分配地址
        setAddrForLocalAndParamVars(table, 0);
        Register.getRegs().stream().filter(r -> r.isParamRegister() && !r.getRegisterDecorator().isEmpty())
                .forEach(this::storeRegToShadowSpace);
        if (!table.getLocalVars().isEmpty()) {
            genComment("function body");
        }
    }

    private void translateLeave() {
        //将未保存的变量写至内存
        for (String s : curTable().getVariables().keySet()) {
            DefinedVariable var = curTable().getVariables().get(s);
            tryStoreToMem(var);
        }
        //生成尾声
        if (curTable().isProcessTable()) {
            epilogue();
        }
    }

    private void translateRet(Value y, boolean isLastRet) {
        //获取结果寄存器
        Register retReg = RAX;
        if ((y != null)
                && (y.getIRType().isFloatType())) {
            retReg = SSERegister.get(0);//Xmm0
        }
        if (y != null)
            moveValueToSpecificReg(y, retReg,false);

        if (isLastRet) {
            if (processInfo.hasOtherRet) {
                Label l = bindLabel();
                processInfo.setEpilogueLbl(l);
            }
            translateLeave();
            gen(RET, null, null);
            gen(curTable().getName() + " ENDP ");
            if (!curTable().hasNoLocalOrParamVar() || !processInfo.storeTemps.isEmpty()) {
                gen("_TEXT\tENDS");
                processInfo.insertMacroOfVarPosition();
            }
        } else {
            //跳转到函数尾声
            //跳转目标尚未确定,等待回填
            AsmInstr retInstr = gen(JMP, null, null);
            processInfo.addRetInstr(retInstr);
            processInfo.hasOtherRet = true;
        }
    }


    private void translateParam(ParamInstruction instr) {
        Value y = instr.getArg1();
        paramList.add(y);
        instr.setPassingAsArg(true);
        pw.println("#" + instr + ",storeInstrs:");

        List<Instruction> instructions = storeInstructions.get(y);
        if (instructions == null)
            instructions = new ArrayList<>();
        for (Instruction i : instr.getStoreInstructions()) {
            pw.println("#" + i);
        }
        instructions.addAll(instr.getStoreInstructions());

        storeInstructions.put(y, instructions);
        findParamInstr.put(y, instr);
    }


    private void translatePreInstr(Value v) {
//        if(CompilerOption.IROptimized)
//            return;
        List<Instruction> ins = storeInstructions.get(v);
        ins.stream().sorted().forEach(this::translate);
    }

    private void translateCall(CallInstruction callInstr) {
        Value y = callInstr.getArg1(), z = callInstr.getArg2();
        Var x = (Var) callInstr.getResult();
        pw.println("#call,paramList=" + paramList);
        int argNum = Integer.parseInt(((Literal) z).getLxrValue());
        int paramListSize = paramList.size();
//        String functionName = callInstr.getFuncName();
//        globalTable.getFunction(functionName);

        //更新当前传递参数
        updateCurParams(argNum,callInstr);
        //迁移结果寄存器的值,避免将其保存进栈
        Register retReg = RAX;

        List<Register> consideringRegs = Register.getCallerRegister();
        //如果返回类型不为void
        if (x != null) {
            if (x.getIRType().isFloatType()) {
                retReg = SSERegister.get(0);//Xmm0
            }
            consideringRegs.add(retReg);
        }

        /*清空所有的装载变量寄存器*/
        List<Register> storeLoadedRegs=curLoadedRegs;
        if(curLoadedRegs!=null)
            curLoadedRegs=null;

        //保存易失性寄存器
        //先恢复参数变量的活跃性信息,
        //因为是否需要保存此类寄存器参考的是调用后变量的活跃性
        callInstr.restoreActiveINfo();
        LinkedList<Register> storeCallerRegs = new LinkedList<>();//记录要保存的调用方寄存器
        int storeRegsSize = 0;
        pw.println("RSP=" + processInfo.RSPDifferFromRBP);
        for (Register r : consideringRegs) {
                storeRegisterToMem(r);
                r.wash();
        }
        pw.println("storeRegsize=" + storeRegsSize + ",RSP=" + processInfo.RSPDifferFromRBP);

        /*      预处理步骤     */
        //设置传参数时的活跃性信息
        //此处之所以要设置活跃性信息是为了在传参时的寄存器选择环节中
        //避免溢出尚未传递的变量
        for (int i = 0; i < argNum; i++) {
            int idx = paramListSize - 1 - i;
            Value v = paramList.get(idx);
            if (v instanceof Var var) {
                var.setActive(true);
                var.setPassingValueAsArg(true);
            }
        }
        for (Var var : curBlock.getUsedVars()) {
            List<Value> args = callInstr.getArgs();
            if (!args.contains(var) && !(var instanceof Temp)) {
                var.setActive(false);
                var.setNextUsed(Var.UNDEFINED);
            }
        }

        //计算栈参数的空间
        int passedByRegNum = Integer.min(GeneralParamRegs.length, argNum);
        int shadowSpaceSize = 32, callerAllocedStackSize = shadowSpaceSize; //预留影子空间
        int passedByStack = argNum - passedByRegNum;
        if (passedByStack > 0) {
            //一律按8字节大小传栈
            callerAllocedStackSize += TypeWidth.ptrWidth * (passedByStack);
        }
        pw.println("$stackSize=" + callerAllocedStackSize);
        //栈指针(虚拟)向下移动,表示分配空间给栈变量
        processInfo.addRspDiff(callerAllocedStackSize);

        //更新最大传参和保存寄存器所用空间
        processInfo.maxCallerAllocedStackSize = Integer.max(processInfo.maxCallerAllocedStackSize,
                callerAllocedStackSize + storeRegsSize);
        pw.println("$maxSize=" + processInfo.maxCallerAllocedStackSize);

        /*  将保存的易失性寄存器的地址更改为以RSP为基址  */
        for (Register r : storeCallerRegs) {
            MemRef mem = r.getStackAddr();
            mem.shiftToRspBase(processInfo.RSPDifferFromRBP);
        }

//        gen("");
        /*      传递参数进栈      */
        //以参数的地址以RSP为基准
        if (argNum > passedByRegNum) {
            int offset = callerAllocedStackSize;
            //从右至左依次入栈
            for (int i = argNum - 1; i >= argNum - passedByStack; i--) {
                Value v = getNthArg(i);
                pw.println("\n---------传递" + v + "-------------");
                genComment("passing the " + (i + 1) + "th argument");
                translatePreInstr(v);
                offset -= TypeWidth.ptrWidth;
                MemRef mem = new MemRef(offset, RSP, v.getIRType());
                param(v, mem, callInstr, i + 1);
            }
        }

        /*      传递参数至寄存器    */
        //实际上,按照64位MS ABI,所有的寄存器传参都是64位的
        //按照从右向左的顺序
        List<Register> paramRegs = new LinkedList<>();
        for (int i = passedByRegNum - 1; i >= 0; i--) {
            Value v = getNthArg(i);
            pw.println("\n---------传递" + v + "-------------");
            genComment("passing the " + (i + 1) + "th argument");
            Register r = Register.getParamReg(v, i);
            //翻译计算此实参的中间语句
            translatePreInstr(v);

            param(v, r, callInstr, i + 1);
            paramRegs.add(r);
            //表示该寄存器正在进行参数传递
            r.setUsedAsParamReg(true);
        }
        //清空寄存器的参数传递标志
        for (Register r : paramRegs) {
            r.setUsedAsParamReg(false);
        }

        //清空参数列表
        paramList.subList(paramListSize - argNum, paramListSize).clear();
        curParams.clear();

        /*      调用函数    */

        Address funcAddr=getAddrOfCalledFunc(callInstr);
        gen(CALL,funcAddr, null);
        gen("");

        /*      清空易失性寄存器    */
        for (Register r : Register.getCallerRegister()) {
            if (!storeCallerRegs.contains(r)) {
                r.clear();
            }
        }

        /* 撤销间接调用变量的特殊状态 */
        if(callInstr.isDirectCall()){
            Value value=callInstr.getArg1();
            if(value instanceof Var var){
                var.setPassingValueAsArg(false);
            }
        }

        //撤销栈空间
//        gen(ADD,RSP,new Literal(callerAllocedStackSize));
        processInfo.RSPDifferFromRBP -= callerAllocedStackSize;
        //恢复寄存器
        while (!storeCallerRegs.isEmpty()) {
            Register r = storeCallerRegs.removeLast();
            virtualPop(r);
        }
        pw.println("RSP=" + processInfo.RSPDifferFromRBP);

        /*  返回寄存器装入函数结果    */
        if (x != null) {
            loadResultVar(retReg, x);
        }

        pw.println("清空后,paramList=" + paramList);
    }

    /**
     * @return Call指令所需的地址
     */
    private Address getAddrOfCalledFunc(CallInstruction callInstr){
        Value calledFunc=callInstr.getArg1();
        if(calledFunc instanceof DefinedFunction){
            return new Literal(callInstr.getFuncName());
        }else{
            Address addr=chooseAddrForValue(calledFunc,true,false,false);
            return addr;
        }
    }

    /**
     * 获取从左到右的第n个参数(从0开始)
     */
    private Value getNthArg(int n) {
        return paramList.get(paramList.size() - 1 - n);
    }

    /**
     * 更新当前传递的参数
     */
    private void updateCurParams(int numOfParams, CallInstruction callInstr) {
        curParams.clear();
        for (int i = 0; i < numOfParams; i++) {
            Value value = paramList.get(paramList.size() - 1 - i);
            addCurParam(value);
            List<Instruction> instructions = storeInstructions.get(value);
            for (Instruction instr : instructions) {
                addCurParam(instr.getArg1());
                addCurParam(instr.getArg2());
                if (instr.getResult() instanceof Value)
                    addCurParam(instr.getResult());
            }
        }
        if(!callInstr.isDirectCall()){
            Value value=callInstr.getArg1();
            addCurParam(value);
            if(value instanceof Var var){
                var.setPassingValueAsArg(true);
            }
        }
    }

    private void addCurParam(Value v) {
        if (!curParams.contains(v))
            curParams.add(v);
    }

    /**
     * 将地址addr的值压栈.
     * 用MOV指令代替PUSH指令,跟踪栈指针的相对位置
     */
    private void virtualPush(Address addr) {
        if (addr instanceof Register r) {
            //保存寄存器内容
            r.storeContent();

            //分配栈空间
            processInfo.RSPDifferFromRBP += TypeWidth.ptrWidth;
            Type memType = r.getIRType();
            MemRef mem = new MemRef(-processInfo.RSPDifferFromRBP, RBP, memType);

            //寄存器进栈时,保存完整的寄存器
            r.shiftToState(memType);
            genComment("push " + r);
            pw.println("#push " + r + ",r.content=" + r.getDescription());
            genMove(mem, r);
            r.setStackAddr(mem);
            r.loadPreState();

        } else {
            throw new RuntimeException("待实现...");
        }
    }

    private void virtualPop(Address addr) {
        if (addr instanceof Register) {
            Register r = (Register) addr;
            //清洗寄存器
            r.wash();

            MemRef mem = r.getStackAddr();
            r.shiftToState(mem.getIRType());
            genComment("pop " + r);
            genMove(r, mem);
            r.loadPreState();

            r.setStackAddr(null);

            //撤销栈空间
            processInfo.RSPDifferFromRBP -= TypeWidth.ptrWidth;

            //恢复寄存器内容
            r.loadContent();

        } else {
            throw new RuntimeException("待实现...");
        }
    }

    /**
     * 将临时变量或寄存器参数的值保存至内存,
     */
    private MemoryAddress storeTempToMem(Temp temp, Register r) {
        MemoryAddress mem = null;
        if (temp.isIndex() && temp.getDependency() != null) {
            Var dependency = temp.getDependency();
            if (dependency.getMemAddr() != null) {
                mem = dependency.getMemAddr();
            }
        } else {
            mem=allocMemAddrForTemp(temp);
            genMove(mem, r);
        }
        temp.setMemoryAddr(mem);
        temp.addAddress(mem);
        return mem;
    }

    private MemoryAddress allocMemAddrForTemp(Temp temp){
        processInfo.addTemp(temp);
        temp.setPosDecorator(0, 0);
        MemRef mem = new MemRef(-1, RSP, temp);
        return mem;
    }

    /**
     * @param r 将寄存器保存进内存
     */
    private MemoryAddress storeRegisterToMem(Register r) {
        Optional<Var> optional = r.getRegisterDecorator().stream()
                .filter(var ->
                                !(!var.isTemp()&&var.isArrayAddr()) &&//数组名
//                                getCurVolatileVars().contains(var) &&
                                var.mustStoreBeforeCall()
//                        //常驻内存的变量不必在调用前保存,因为他们的值在有函数调用的情况下已经保证了和内存同步?
//                        &&curLoadedRegs!=null&&!curLoadedRegs.contains(r)
                ).findFirst();
        if(optional.isPresent()){
            pw.println("#Store before call");
            storeVarToMem(optional.get(),r);
        }
        return null;
    }

    /**
     * 将参数传到addr指定的地址
     *
     * @param n 第几个参数
     */
    private void param(Value v, Address addr, CallInstruction callInstruction, int n) {
        //恢复活跃性信息
        if (v instanceof Var var) {
            callInstruction.restoreActiveINfo(v);
            var.setIsOperand(true);
        }
        //传递至寄存器
        if (addr instanceof Register r) {
            loadParamToReg(v, r);

            Callee callee=callInstruction.getCallee();
            //浮点参数传递至可变长参数的函数时需要同时传递到通用寄存器上
            if (callee.isKwarg(n) && addr.isFloat()) {
                Register genReg = Register.getGenParamReg(n - 1);
                moveReg(genReg);
                //转换通用寄存器的状态
                Type type = addr.isSinglePrecision() ? int32 : int64;
                genReg.shiftToState(type);
                genSSEMove(genReg, r);
            }
        }
        //传递至栈
        else {
            Address addrV = chooseAddrForValue(v, true, false, false);
            if (!(addrV instanceof Register)) {
                Register R = findAvailableReg(v.getIRType());
                genMove(R, addrV);
                addrV = R;
            }
            genMove(addr, addrV);
            if (v instanceof Var) {
                ((Var) v).addAddress(addr);
            }
        }

        if (v instanceof Var) {
            ((Var) v).setIsOperand(false);
        }

        storeInstructions.get(v).clear();
        ParamInstruction paramInstruction = findParamInstr.get(v);
        paramInstruction.setPassingAsArg(false);
    }

    /**
     * 加载值v到指定的寄存器r
     * r在方法调用前已清空,且r用作不更改值的源寄存器
     */
    private void loadParamToReg(Value v, Register r) {
        pw.println("\n#logdToReg,Value v=" + v + ",Register r=" + r + "\n" +
                "#r.decorator=" + r.getRegisterDecorator() + ",v.decorator=" + ((v instanceof Var) ? ((Var) v).getAddrDecorator()
                : "null"));
        Var var;

        if (v instanceof Literal literal) {
            //转换寄存器状态
            r.shiftToState(v.getIRType());
            moveReg(r);
            if (v.getIRType() == StringLiteral) {
                DirectAddress strAddr = globalTable.getStringLiteral(literal);
                genMove(r, strAddr);
                r.setUniqueVar(v);
            } else {
                Literal literalR = r.getStoreLiteral();
                //r中没有装载v的值
                if (literalR == null || !literalR.equals(v)) {
                    Address addrV = (Literal) v;
                    if (v.getIRType() == floatLiteral) {
                        addrV = globalTable.getAddrOfLiteral(literal);
                    }
                    r.setUniqueVar(v);
                    genMove(r, addrV);
                }
            }
        } else {
            assert v instanceof Var;
            moveValueToSpecificReg(v,r,false);
//            //r中不包含v的值
//            if (!r.getDecorator().contains(var)) {
//                //迁移r的原值
//                moveReg(r);
//                r.shiftToState(v.getIRType());
//                Register Rv = chooseFromAddrDecorator(var, false);
//                //没有寄存器有v的值,生成取数指令
//                if (Rv == null) {
//                    loadVar(r, var);
//                } else {
//                    Rv.shiftToState(var.getIRType());
//                    genMove(r, Rv);
//                    var.addAddress(r);
//                    r.setUniqueVar(var);
//                }
//            }
        }
    }

    /**
     * 将寄存器r保存到影子空间,
     * 其中r中只保存着没有内存地址的形参变量,
     * 本方法为这些变量设置内存地址.
     */
    private void storeRegToShadowSpace(Register r) {
        Register[] regs = (r.isFloat()) ? FloatParamRegs : GeneralParamRegs;
        List<Register> registers = Arrays.asList(regs);
        int idxOfR = registers.indexOf(r);
        //相对于RBP的地址
        int offset = (idxOfR + 1) * TypeWidth.ptrWidth;//现在不保存rbp
        Var var = r.getRegisterDecorator().get(0);
        MemRef mem = new MemRef(offset, RBP, var);
        for (Var v : r.getRegisterDecorator()) {
            if (v.getMemRef() != null || v instanceof Temp) {
            } else {
                v.setPosDecorator(0, 0);
                v.setMemRef(mem);
                v.addAddress(mem);
            }
        }
        pw.println("#将" + r + "保存到影子空间");
        pw.println("$ " + r.getDescription());
        processInfo.shadowSpaceAddMem(r, mem);
    }

    /**
     * 为符号表中的变量设置内存地址
     */
    private void setAddrForLocalAndParamVars(LocalTable table, int base) {
        pw.println("##setAddrForLocalVars," + "base=" + base + ",tableSize=" + table.size());
        //清除寄存器内容
        Register.clearAllRegs();

        //设置形参的地址
        List<DefinedVariable> params = table.getParamVars();
        for (int i = 0; i < params.size(); i++) {
            DefinedVariable param = params.get(i);
            if (i < GeneralParamRegs.length) {
                Register r = Register.getParamReg(param, i);
                setRegAddrForParam(r, param);
            } else {
                int offset = param.getOffset();
                MemRef mem = new MemRef(offset, RBP, param);
                param.setMemoryAddr(mem);
                param.setUniqueAddress(mem);
            }
            pw.println(param.getName() + "\t" + param.getAddrDecorator());
        }

        //设置局部变量的地址
        int totalSize = setAddrForAllLocalsInTable(table, base, 0, 0);
        table.setAllocSize(totalSize);

        //为局部变量分配栈空间
        if (table.getAllocSize() != 0) {
            pw.println("RSP=" + processInfo.RSPDifferFromRBP);
//            int n=align(table.size(), TypeWidth.X86_RSP_align_size);
            processInfo.RSPDifferFromRBP += table.getAllocSize();
        }
        //记录分配语句的插入位置,因为栈指针移动的距离仍未确定
//        allocInstr= gen(SUB,RSP,null);
        processInfo.posOfAllocInstr = codes.size();
    }

    /**
     * 从偏移量base开始,为table中的的本层局部变量设置地址
     *
     * @return 所有变量设置地址后的偏移量
     */
    public int setAddrOfDirectLocalInTable(LocalTable table, int base, int level, int nth) {
        int offset = base;
        List<Var> locals = table.getLocalVars();
        for (Var var : locals) {
            offset = base + var.getOffset();
            var.setPosDecorator(level, nth);
            pw.println(String.format("%s相对于rbp的偏移量:%d", var, -offset));

            MemRef mem = new MemRef(-offset, RBP, var);
            //设置数组基址标志
            mem.setLeftValue(var.isArrayAddr());
            var.setMemRef(mem);
            var.setUniqueAddress(mem);
        }

        pw.println("total size=" + (offset - base));
        return offset;
    }

    /**
     * @param level 嵌套深度.
     * @param nth   属于父符号表的第几个子代
     * @return 设置table所包含所有局部变量后的偏移量
     */
    public int setAddrForAllLocalsInTable(LocalTable table, int base, int level, int nth) {
        //设置本层局部变量的地址
        int newBase = setAddrOfDirectLocalInTable(table, base, level, nth);
        int totalSize = newBase;

        int n = 0;
        for (LocalTable child : table.getChildren()) {
            int s = setAddrForAllLocalsInTable(child, newBase, level + 1, n);
            totalSize = Integer.max(totalSize, s);
            n++;
        }

        return totalSize;
    }

    /**
     * 为指令的变量设定活跃信息
     */
    public Queue<Var> setActiveInfo(Value y, Value z, Result x, boolean[] info) {
        Queue<Var> storeVars = new LinkedList<>();
        setActiveInfo(y, info[Const.Ypos], storeVars);
        if(y!=z)
            setActiveInfo(z, info[Const.Zpos], storeVars);
        setActiveInfo(x, info[Const.Xpos], storeVars);
        return storeVars;
    }

    public void setActiveInfo(Value v, boolean active, Queue<Var> storeVars) {
        if (!(v instanceof Var))
            return;
        Var var = (Var) v;
        boolean ori = var.isActive();

        var.setActive(active);
        activeMap.put(var, active);
        var.setIsOperand(true);

        if (ori && !active) {
            storeVars.add(var);
        }
    }

    private void resetOperandInfo(Value v) {
        if (v instanceof Var) {
            ((Var) v).setIsOperand(false);
        }
    }

    /**
     * 为形如"x=y OP z"的指令寻找寄存器
     * 机器指令形如"OP,Rxy,Rz"
     * 通过分别设置字段selectedXY,selectedZ返回选择的寄存器
     *
     * @param changeable 指令的两个操作数位置是否可交换
     */
    private void getReg(Value y, Value z, Var x, OP op, boolean changeable) {
        pw.printf("##getReg,addrOf%s=%s,addrOf%s=%s%n",
                y, (y instanceof Literal) ? "null" : ((Var) y).getAddrDecorator(),
                z, (z instanceof Literal) ? "null" : ((Var) z).getAddrDecorator());
        Var vz = null;
        //当z和x相等时或者y活跃而z不活跃时,交换y和z
        if ((z instanceof Var)
                && (z == x ||
                changeable &&
                        (y instanceof Var) && (((Var) y).isActive()) && !(((Var) z).isActive()))) {
            vz = (Var) z;
            Var temp = (Var) y;
            y = vz;
            vz = temp;
        }
        setSrcAddr(chooseAddrForValue((vz == null) ? z : vz, true));
        boolean desChanging = !(op == mul && (x.isIndex()));
        setDesAddr(chooseAddrForValue(y, true, desChanging, y == x));
        pw.println("srcR=" + srcAddr + "\n" + "desAddr=" + desAddr);
    }

    private void moveAddr(Address address) {
        if (!(address instanceof Register))
            return;
        Register r = (Register) address;
        moveReg(r);
    }

    /**
     * 将编译器保存的除var之外的值迁移到另一寄存器
     *
     * @param var           R希望装入或保留的变量
     * @param ValueChanging 如果该值为真,var的值同样需要迁移
     */
    private Register moveReg(Register R, Var var, boolean ValueChanging) {

        List<Var> vars = new LinkedList<>(R.getRegisterDecorator());
        Register newR = null;
        boolean needMove = false;
        pw.println("##moveReg," + R.getDescription() + ",the var=" + var);
        //检查变量
        for (Var v : vars) {
            //不需要迁移var的值
            if (var != null && v == var
                    && !ValueChanging) {
                continue;
            }
            /**
             * 这里并没有删除v的地址描符中的R项,防止产生一些不必要的寄存器移动
             *R中装入新值或清空应该由使用者保证
             */
            //当v活跃且只有该寄存器保存v的值
            if (v.isActive() && (v.getRegisters().size() == 1)
                    && !v.isPassingValueAsArg()) {
                needMove =true;
            }
            //正在传递给当前的函数调用
            else if ((v.isPassingValueAsArg() && (isInCurParamList(v)
                    && v.isActive() && !v.isOperand()))
                    || (v.isOperand() && (var == null || var != v)))
            {
                pw.println("#%s need to be moved".formatted(v));
                needMove = true;
            }
        }
        //检查是否作为地址访问
        for (MemRef mem : R.getMemRefs()) {
            if (mem.isActive() && (var != null && mem.getVar() != var)) {
                pw.println(mem + "is still used");
                needMove = true;
                break;
            }
        }

        tryStoreReg(R);
        //不需要迁移寄存器内容
        if (!needMove || vars.isEmpty()) {
//            R.clear();
            return R;
        }

        Type irType = null;
        if (newR == null) {
            if (!vars.isEmpty()) {
                irType = vars.get(0).getIRType();
            } else if (R.getStoreLiteral() != null) {
                irType = R.getStoreLiteral().getIRType();
            } else if (!R.getMemRefs().isEmpty()) {
                MemRef mem = R.getMemRefs().get(0);
                if (mem.getVar() != null) {
                    irType = mem.getVar().getIRType();
                }
            }
            newR = findAvailableReg(irType);
        }
        pw.println("##newR chosen, newR=" + newR);

        //新寄存器复制就寄存器的内容
        RegisterState oriStateOfR = R.getRegState();
        R.shiftToState(newR.getRegState());
        newR.copyContent(R);
//        R.clear();
        genMove(newR, R);
        R.setState(oriStateOfR);
        return newR;
    }

    private Register moveReg(Register R) {
        return moveReg(R, null, false);
    }

    /**
     * 选择变量v的地址
     *
     * @param needLoad      true表示变量v不存在任一寄存器中时从内存中加载变量
     * @param valueChanging 表示寄存器的值是否在运算后改变,同时也判断v是否作为某些运算的目标操作数
     */
    private Address chooseAddrForValue(Value value, boolean needLoad, boolean valueChanging,
                                      boolean desEquSrc, Register preferReg) {
        Type type = value.getIRType();
        if (value instanceof Literal literal) {
            //加载浮点字面量
            if (value.getIRType().isFloatType()) {
                DirectAddress direct = globalTable.getAddrOfLiteral(literal);
                return direct;
            }
            //从静态数据区中获得字符串的地址
            else if (value.getIRType() == StringLiteral) {
                DirectAddress strAddr = globalTable.getStringLiteral((Literal) value);
                return strAddr;
            } else {
                if (!valueChanging)
                    return (Literal) value;
                else {
                    //R装载作为目的操作数的字面量值
                    Register R = chooseAddrForValue(type);
                    genMove(R, (Literal) value);
                    return R;
                }
            }
        }
        //函数名
        else if(value instanceof DefinedFunction definedFunction){
            return  new Literal(definedFunction.getName());
        }

        Var v = (Var) value;
        /*避免重复的寄存器移动*/
        boolean moved = false;
        //从寄存器描述符中寻找
        Register R = chooseFromAddrDecorator(v, valueChanging && !desEquSrc);
        //优先分配偏好的寄存器
        if (R != null && preferReg != null) {
            genMove(preferReg, R);
            preferReg.copyContent(R);
            R = preferReg;
        }

        if (R == null) {
            //v是一个代表内存中的值的匿名变量
            if ((v instanceof Temp t)
                    && ((Temp) v).getPointingAddr() != null && (v.isLeftValue())) {
                hasMemOperand = true;
                Address PointingAddr = t.getPointingAddr();
                if (!valueChanging || PointingAddr.isLeftValue())
                    return PointingAddr;
                    //选择的是目标寄存器,需要用寄存器装载结果值
                else {
                    R = findAvailableReg(t.getIRType());
                }
            }
            //可以选择变量的内存引用的情况:当前指令没有其他内存操作数且(变量不活跃,不作为目标操作数
            //  或者作为目标操作数且有一个源操作数和目标操作数相等)
            else if (!hasMemOperand
                    && (v.getMemAddr() != null)
                    && ((!v.isActive() && !valueChanging) || (valueChanging && desEquSrc))) {
                hasMemOperand = true;
                return v.getMemAddr();
            }
            pw.println("#" + value + "的类型:" + type);

            //分配一个空闲寄存器
            if (R == null) {
                R = chooseAddrForValue(type);
            }
            //从内存中加载v到R
            if (needLoad)
                loadVar(R, v);
        }

        if (valueChanging && !desEquSrc && !v.hasOtherRegisterAddr(R))
            R = moveReg(R, v, true);
        return R;
    }

    private Address chooseAddrForValue(Value value, boolean needLoad, boolean valueChanging,
                                      boolean desEquSrc) {
        return chooseAddrForValue(value, needLoad, valueChanging, desEquSrc, null);
    }

    private Address chooseAddrForValue(Value value, boolean needLoad) {
        return chooseAddrForValue(value, needLoad, false, false);
    }

    /**
     * 从变量的地址描述符中选择寄存器
     */
    private Register chooseFromAddrDecorator(Var v, boolean ValueChanging) {
        Var var = v;
        if (v instanceof Temp t && t.isIndex()) {
            var = t.getDependency();
            for (Address addr : var.getAddrDecorator()) {
                t.addAddress(addr);
                if (addr instanceof Register r) {
                    r.addVariable(t);
                } else if (addr instanceof MemoryAddress mem && t.getMemAddr() == null) {
                    t.setMemoryAddr(mem);
                }
            }
        }
        List<Register> registers = var.getRegisters();
        if (registers.isEmpty()) return null;
        Register r = registers.get(0);
        if (ValueChanging) {
            r = moveReg(r, var, ValueChanging);
        }
        return r;
    }

    /**
     * 生成取数指令
     */
    private void loadVar(Register R, Var v) {
        if (R instanceof FloatRegister) {
            gen(FLD, v.getMemRef(), null);
        } else {
            MemoryAddress mem = null;
            if (v.getMemAddr() != null)
                mem = v.getMemAddr();
                //v表示一个地址,需要内存访问
            else if ((v instanceof Temp) &&
                    (((Temp) v).getPointingAddr() != null)) {
                mem = ((Temp) v).getPointingAddr();
            }
            if (mem == null) {
                throw new RuntimeException("无法找到变量:%s的内存地址,curIrId=%d".formatted(v, curIRid));
            }
            /*使用扩展宽度传输*/
            if (v.isIndex()) {
                R.shiftToState(pointer);
                gen(MOV, OpPostfix.SXD, R, mem);
                R.cleanWhenExtension(int32,pointer);
            } else {
                genMove(R, mem);
            }
        }
        v.addAddress(R);
        R.setUniqueVar(v);
    }

    private void moveValueToSpecificReg(Value value, Register r,boolean needClean) {
        pw.printf("\n#传递 %s 至%s%n", value, r);
        Var var = null;
        if (value instanceof Var var1)
            var = var1;

        List<Var> decorator = r.getRegisterDecorator();
        r.shiftToState(value.getIRType());
        if (!decorator.contains(value)) {
            moveReg(r, var, false);
            r.clear();
            Address addr = null;
            //简化地址选择步骤
            preferRegs.add(r);
            addr = chooseAddrForValue(value, true, false, false);

            loadValue(r, value, addr,needClean);
            preferRegs.remove(r);
        } else if(needClean &&r.getWidth()!=TypeWidth.ptrWidth){
            zeroOrSignExtend(r, pointer,true,true);
        }

        pw.printf("#传递 %s 至%s结束%n", value, r);
    }

    private Register loadVar(Var v) {
        Register r = findAvailableReg(v);
        loadVar(r, v);
        return r;
    }

    /**
     * 必要时创建将变量装入内存的指令
     */
    private void tryStoreToMem(Var v) {
        Register R = chooseFromAddrDecorator(v, false);
        tryStoreToMem(v, R);
    }

    /**
     * 将r的储存的变量的值保存至内存
     */
    private void tryStoreToMem(Register r) {
        r.getRegisterDecorator().forEach(var -> {
            tryStoreToMem(var, r);
        });
    }

    private boolean needStore(Var v) {
        //注意函数调用后,寄存器中的值被清空
        if (!curBlock.isLoaded(v)||curLoadedRegs==null) {
            return v.needStore();
        }
        /*需要常驻寄存器的变量*/
        else {
            return curBlock.hasCallInstr()
                    && !v.getAddrDecorator().contains(v.getMemRef());
        }
//        return false;
    }

    private Register getCurAllocatedReg(Var var){
        Map<Var,Register> table= curBlock.getAllocationTable();
        if(table!=null){
            return table.get(var);
        }
        return null;
    }
    
    /**
     * 返回在本基本块内被定值的变量
     */
    private  Set<Var> getCurVolatileVars(){
        return curBlock.getNewAssignedAndActiveVars();
    }

    private void tryStoreToMem(Var v, Register R) {
        if (R == null)
            return;
        if (needStore(v)) {
            storeVarToMem(v,R);
        }
    }

    private void storeVarToMem(Var v,Register R){
        //保存R的状态
        RegisterState oriState = R.getRegState();

        R.setStateFromValueType(v.getIRType());
        Address writeBackAddr = v.getWriteBackAddr();
        if (writeBackAddr != null && (!v.isTemp() || v.isLeftValue())) {
            genMove(writeBackAddr, R);
            //溢出完毕
            //此时假设临时变量的溢出都是由于往该临时变量所代表的地址写了新值
            if (v instanceof Temp temp) {
                //清空需要写回标志
                temp.setNeedWriteBack(false);
            }
        } else if (writeBackAddr == null && !v.isParam() && !(v instanceof Temp)) {
            throw new RuntimeException("error!找不到" + v + "的地址!");
        }
        //该变量没有地址,属于普通临时变量或用寄存器传递的参数
        //将变量保存到栈上,位于局部变量之后参数空间之前
        else {
            writeBackAddr = storeTempToMem((Temp) v, R);
        }
        pw.println("将" + v + "溢出到内存:" + writeBackAddr);
        v.addAddress(writeBackAddr);

        //恢复R的状态
        R.setState(oriState);
    }

    /**
     * 将r所贮存的变量溢出
     */
    private void tryClearReg(Register r) {
        for (Var v : r.getRegisterDecorator()) {
            tryStoreToMem(v);
        }
    }

    /**
     * 寄存器r保存新值
     * 一般用作修改代表运算结果的变量的地址描述符,
     * 以及转载该变量的寄存器的寄存器描述符
     */
    private void loadResultVar(Address address, Value v) {
//        pw.println(r + "中装入值" + var);
        if (v instanceof Var var) {
            var.setUniqueAddress(address);
        }
        if (address instanceof Register r) {
            r.setUniqueVar(v);
        }
    }

    /**
     * 寄存器r装入value的值
     * 此时因为value的内容不变,所以不必更改它的描述符
     * 作用:生成mov传输指令,修改r的描述符,value的描述符或增加r的表项
     */
    private void loadValue(Register r, Value value, Address addrOfValue,boolean needClean) {
        if (!r.equals(addrOfValue)) {
            genMove(r, addrOfValue,needClean);
            r.setUniqueVar(value);
        }else if(needClean){
            zeroOrSignExtend(r,pointer,true,true);
        }

        if (value instanceof Var) {
            ((Var) value).addAddress(r);
        }
    }

    private void loadValue(Register r, Value value, Address addrOfValue){
        loadValue(r,value,addrOfValue,false);
    }

    /**
     * 寻找空闲的寄存器装载value的值,
     * 目前一般用为加载字面量或变量到寄存器
     * 此寄存器作为临时寄存器
     *
     * @return 选择的寄存器
     */
    private Register loadValueToTempR(Value value) {
        Register tempR = findAvailableReg(value.getIRType());
        if (value instanceof Literal) {
            loadValue(tempR, value, (Literal) value);
        } else {
            Var var = (Var) value;
            loadVar(tempR, var);
        }
        return tempR;
    }

    /**
     * 加载地址到寄存器
     * 作用:生成LEA指令,清空desReg的描述符等内容
     */
    private void leaAddr(Register desReg, Address addr) {
        gen(LEA, desReg, addr);
        desReg.clear();
    }

    /**
     * 设置形参的寄存器地址
     */
    private void setRegAddrForParam(Register r, Var v) {
        //之所以不调用v的setUniqueAddress()函数是因为此情况下该函数会设置脏位
        v.addAddress(r);
        r.setUniqueVar(v);

    }

    /**
     * 选择一个寄存器
     * 先选择空闲的,若无则选择一个寄存器溢出
     *
     * @return 选择的寄存器
     */
    private Register chooseAddrForValue(Type type) {
        Register r = findAvailableReg(type);
        if (r != null) return r;

        //首先选择装载不活跃变量的寄存器,
        //然后是只含非临时变量的
        //其次是活跃变量较少的
        //再选择下一次引用位置较远的.
//        throw new RuntimeException("寄存器已满...");
        pw.println("选择要溢出的寄存器..........");

        r = R8;
        List<Var> vars = r.getRegisterDecorator();
        //将寄存器r的内容保存进内存
        //如果只保存一个非临时变量
        if (vars.size() == 1
                && (vars.get(0) instanceof DefinedVariable)) {
            tryStoreToMem(vars.get(0));
            vars.get(0).removeAddress(r);
        }

        //如果保存有临时变量或多于一个非临时变量
        else {
            for (Var var : vars) {
                tryStoreToMem(var);
                var.removeAddress(r);
            }
        }

        return r;
    }

    /**
     * 从空闲寄存器中选择一个整数或浮点寄存器
     */
    private Register findAvailableReg(Type type) {
        if (type.isFloatType())
            return findAvailableSSEReg(type);
        else
            return findGeneralReg(type);
    }

    private Register findAvailableReg(Register register) {
        if (register instanceof SSERegister sseRegister) {
            return findAvailableSSEReg(sseRegister.isSinglePrecision() ? floatType : doubleType);
        } else {
            RegisterState state = register.getRegState();
            Type type;
            if (state == B || state == H) {
                type = charType;
            } else if (state == W || state == HB || state == HH) {
                //这里没有十六位的类型,暂时用三十二位代替
                type = int32;
            } else if (state == D) {
                type = int32;
            } else {
                type = pointer;
            }
            return findAvailableReg(type);
        }
    }

    private Register findAvailableReg(Var v) {
        return findAvailableReg(v.getIRType());
    }

    private Register findGeneralReg(Type type) {
        Register ret = null;
        pw.println("#findGeneralReg,prefer="+preferRegs);
        for (Register r : preferRegs) {
            if (!r.isFloat() && testAvailable(r, type)) {
                ret = r.getReg();
                break;
            }
        }
        if(ret==null){
            for (Register reg : Register.getIntegerRegs()) {
                if (testAvailable(reg, type)) {
                    ret = reg.getReg();
                    break;
                }
            }
        }
        ret.setDirtyPos(type);
        return ret;
    }

    private Register findCompatibleReg(Register r) {
        boolean isHighByteReg = r.isHB();
        for (Register reg : Register.getIntegerRegs()) {
            if (testAvailable(reg, byteType)) {
                Register register = reg.getReg();
                if (isHighByteReg && register.isHB()
                        || !isHighByteReg && !register.isHB()) {
                    return register;
                }
            }
        }
        return null;
    }

    private boolean testAvailable(Register reg, Type type) {
        if (reg.isAvailableForType(type) && reg.isGeneral()
                && !isLoadedRegs(reg)) {
            //选择实际的寄存器,可能会产生AH
            reg = reg.getReg();
            pw.println("选择空闲寄存器" + reg.getName() + ",PreState=" + reg.getPreState() +
                    ", state=" + reg.getRegState() + ",content=" + reg.getDescription());
            checkReg(reg);
            return true;
        }
        return false;
    }

    private boolean isLoadedRegs(Register r) {
        return curLoadedRegs != null && curLoadedRegs.contains(r);
    }

    /**
     * 选择一个可用的浮点寄存器
     */
    private Register findAvailableFloatReg() {
        for (Register r : preferRegs) {
            if (r.isFloat() && testAvailable(r, floatType)) {
                return r;
            }
        }

        Register r = FloatRegister.getAvailableReg();
        pw.println("选择栈顶浮点寄存器:" + r);
        return r;
    }

    /**
     * 选择一个可用的SSE寄存器
     */
    private Register findAvailableSSEReg(Type type) {
        Register r = SSERegister.getAvailableReg(type, preferRegs);
        checkReg(r);
        return r;
    }

    /**
     * 将寄存器reg保存的一些变量存入内存或压入堆栈
     */
    private void checkReg(Register reg) {
//        if (reg == null) return;
//        List<Var> vars = reg.getRegisterDecorator();
//
//        for (Var v : vars) {
//            if (isInScope(v)) {
//                v.removeAddress(reg);
//            } else {
//                break;
//            }
//        }
//        tryStoreReg(reg);
        return;
//        reg.clear();
    }

    /**
     * 判断变量是否属于该作用域
     */
    private boolean isInScope(Var v) {
        if (!(v instanceof DefinedVariable def))
            throw new RuntimeException("此方法未有Var的其他实现的版本...");
        return (v instanceof Temp)
                || processInfo.processTable.isLocalInProcess(def)
                || globalTable.isGlobalOrStatic(def);
    }

    private void tryStoreReg(Register reg) {
        if (reg.getType() == RegType.Callee) {
            reg.storeDecorator();
            processInfo.addCalleeReg(reg);
            pw.println("%被调用方保存的寄存器" + processInfo.CalleeUsedRegs + " 加入 " + reg);
        }
    }

    /**
     * 设置解引用结果的寻址
     * 如果x表示一个赋值地址,即非变量形式的左值,
     * 如成员访问,数组访问,指针解引用等,
     * 此时,不需要加载其值到寄存器
     */
    private void setPointingAddr(Var x, MemoryAddress mem) {
        Temp t = (Temp) x;
        //设置内存操作数的类型
        mem.setIRType(x.getIRType());
        //设置mem的左值属性
        mem.setLeftValue(x.isLeftValue());

        //将活跃的右值置于寄存器中
        if (!x.isLeftValue() && !x.isArrayAddr()) {
            if (mem.getIRType() == null) {
                throw new RuntimeException("Error,无法获取mem的类型:   " + mem);
            }
            //加载内存的值到寄存器
            Register r = findAvailableReg(mem.getIRType());
            genMove(r, mem);

            loadResultVar(r,x);
//            x.addAddress(r);

        }

        t.setPointingAddr(mem);
    }

    /**
     * 生成函数尾声
     */
    private void epilogue() {
        Set<Register> calleeRegs = processInfo.CalleeUsedRegs;
        int prologuePos = processInfo.prologuePos;
        int irPos = processInfo.IRPos;
        Location prologueLocation = processInfo.irLocation;

        int calleeRegSize = calleeRegs.size() * TypeWidth.ptrWidth;
        //函数序言处需要分配的栈空间大小,等于 局部变量空间 + 临时变量空间 + 传参空间
        pw.println(String.format("$maxCallerAllocedStackSize=%d,temp variable size=%d,local variable size=%d",
                processInfo.maxCallerAllocedStackSize, processInfo.tempVarSize, curTable().getAllocSize()));
        int allocSize = processInfo.maxCallerAllocedStackSize + curTable().getAllocSize()
                + processInfo.tempVarSize + calleeRegSize;
        //栈指针对齐
        allocSize = Align.alignStack(allocSize);

        if (!curTable().getAllLocals().isEmpty()) {
            genComment("function epilogue");
            /*  更改局部变量的偏移量,变成以RSP为基准   */
            for (Var var : curTable().getAllLocals()) {
                var.getMemRef().shiftToRspBase(allocSize);
//                processInfo.insertMacroOfVarPosition(var);
            }
        }

        //需要更改部分指令的内存操作数
        if (!calleeRegs.isEmpty() || !processInfo.storeTemps.isEmpty()
                || !processInfo.shadowSpace.isEmpty()) {
            //将非易失性寄存器保存在局部变量之后,以防局部变量的位置发生变化
            int offset = -curTable().getAllocSize() + allocSize;//当以RSP为基准时,需要加上RSP和RBP的差
            for (Register r : calleeRegs) {
                offset -= TypeWidth.ptrWidth;
                //此实现在保存寄存器时避免移动栈指针
                MemRef mem = new MemRef(offset, RSP, pointer);
                r.setStackAddr(mem);

                //插入保存非易失性寄存器的指令
                //需要保存完整的寄存器
                RegisterState regState = r.getRegState();
                r.shiftToState(pointer);
                AsmInstr instr = genMove(mem, r, false, irPos, prologueLocation,false);
                insertAsm(instr, prologuePos);
                r.setState(regState);
            }

            /* 设置临时变量的偏移量 */
            int baseOffsetOfTemps = -(curTable().getAllocSize() + calleeRegSize) + allocSize;
            processInfo.setMemRefForTemps(baseOffsetOfTemps);

            /*  回填保存寄存器到影子空间的指令  */
            for (RegisterAddress r : processInfo.shadowSpace.keySet()) {
                MemRef mem = processInfo.shadowSpace.get(r);
                /*  更改影子空间的寻址 */
                mem.shiftToRspBase(allocSize);
                Comment comment = genComment("store " + r + " to Shadow Space", false);
                AsmInstr instr = genMove(mem, r, false, curIRid, prologueLocation,false);
                insertAsm(instr, prologuePos);
                insertAsm(comment, prologuePos);
            }
            processInfo.shadowSpace.clear();

            //恢复寄存器的值
            for (Register r : calleeRegs) {
//                gen(POP,r,null);
                MemRef mem = r.getStackAddr();
                r.setStackAddr(null);
                r.shiftToState(pointer);
                genMove(r, mem);

                //恢复寄存器的描述符
                pw.println("$恢复寄存器" + r + "的描述符,恢复前:");
                pw.println("\t" + r.getRegisterDecorator());
                r.loadDecorator();
                pw.println("$恢复后:");
                pw.println("\t" + r.getRegisterDecorator());
            }
        }

        allocSize = Align.alignStack(allocSize);
        if (allocSize > 0) {
            //回填栈分配的指令
            insertAsm(SUB, RSP, new Literal(allocSize),
                    processInfo.posOfAllocInstr, irPos, prologueLocation);

            //栈指针和帧指针复位
            gen(ADD, RSP, new Literal(allocSize));

            //使用rbp
//            gen(POP,RBP,null);
        }

        //统一出口
        processInfo.bindLabelForRetInstr();
        pw.println("#popRegs,maxAllocSize=" + processInfo.maxCallerAllocedStackSize + ",tableSize=" + curTable().getAllocSize()
                + "allocSize=" + allocSize);
    }

    private void printLine() {
        pw.println("\n-------------------------------------------------------" +
                "------------------------");
    }

    /**
     * 将寄存器的内存赋值到栈顶寄存器
     */
    private Register moveFloatRegToTop(Register r) {
        Register top = findAvailableFloatReg();
        gen(FLD, r, null);
        List<Var> decorator = new LinkedList<>(r.getRegisterDecorator());
        for (Var v : decorator) {
            v.addAddress(top);
            top.addVariable(v);
        }
        return top;
    }

    /**
     * 将栈顶寄存器的浮点数保存到内存
     */
    private void storeFloatToMem(MemRef memRef) {
        gen(FST, memRef, null);
        Var v = memRef.getVar();
        v.addAddress(memRef);
    }

    private void storeSDFPTOMem(MemRef memRef, Register r) {
        genMove(r, memRef);
        Var v = memRef.getVar();
        v.addAddress(memRef);
    }

    private void allocConstSegment() {
        genDeclaration("\t\t.const", true);
        for (Map.Entry<String, DirectAddress> entry : globalTable.getStrings().entrySet()) {
            String s = entry.getKey();
            s = LiteralProcessor.spilitString(s);
            DirectAddress direct = entry.getValue();
            genDeclaration(direct + "\tbyte\t" +
                    s, true);
        }

        globalTable.getFloatLiterals().stream().sorted((f1, f2) ->
                globalTable.getDirectAddr(f1).toString().compareTo(globalTable.getDirectAddr(f2).toString())).
                toList().forEach(floatEntry -> {
            String lxr = floatEntry.getLiteral();
            DirectAddress directAddress = globalTable.getDirectAddr(floatEntry);
            genDeclaration(directAddress.getDeclaration(lxr), true);
        });

        for (DefinedVariable var : globalTable.getConstants()) {
            declareVar(var, true, false);
        }
    }

    private void allocDataSegment() {
        genDeclaration("\t\t.data", false);
        for (DefinedVariable var : globalTable.getGlobalAndStaticVars(false)) {
            if (var.isDefined())
                declareVar(var, false, false);
        }
    }

    private void declareVar(DefinedVariable var, boolean isConst, boolean extern) {
        String name = var.getUniqueName(),
                typeDecorator = null;
        DirectAddress direct = new DirectAddress(var, name);
        String declaration;
        String ini = null;

        //数组类型变量
        if (var.getType().isArray()) {
            TypeNode base = var.getType().getArrayBaseType();
            int arraySize = var.getType().getTotalLen();
            ExprNode iniNode = var.getInit();
            typeDecorator = transformTypeDecorator(Type.getDeclareStr(base), extern);
            //字符串字面量赋值给字符数组
            if (base.isType(BaseType.CHAR) &&
                    iniNode != null && (iniNode.getValue().getIRType() == StringLiteral)) {
                Literal iniValue = (Literal) iniNode.getValue();
                int len = iniValue.getLxrValue().length();
                String left = "";
                if (arraySize - 1 - len > 0) {
                    left = "," + (arraySize - 1 - len) + getDefaultIniStr();
                }
                declaration = String.format("%s\t", direct, typeDecorator);
                ini = String.format("\"%s\",0%s", iniValue.getLxrValue(), left);
            } else {
                declaration = direct + "\t" + typeDecorator;
                ini = arraySize + getDefaultIniStr();//显示初始化为0
            }
        }

        //基本类型或指针变量
        else if (var.getType().isPrimaryType() || var.getType().isPointerType()) {
            typeDecorator = var.getIRType().getDeclareStr();
            if (var.getInit() != null) {
                if (var.getInit().getValue() != null && (var.getInit().getValue() instanceof Literal)) {
                    Value iniValue = var.getInit().getValue();
                    ini = ((Literal) iniValue).getLxrValue();
                    if (iniValue.getIRType() == charType) {
                        ini = "'" + ini + "'";
                    } else if (iniValue.getIRType() == StringLiteral) {
                        ini = "\"" + ini + "\"";
                    }
                } else {
                    throw new RuntimeException("未实现...var=" + var + "var.ini=" + var.getInit());
                }
            } else {
                ini = "?";
            }
            declaration = direct + "\t" + transformTypeDecorator(typeDecorator, extern);
        }

        /*复合类型变量*/
        else if (var.getType().isComposedType()) {
            int num = CalculateTypeWidth.getTypeWidth(var.getType());
            typeDecorator = "byte";
            declaration = direct + "\t" + transformTypeDecorator(typeDecorator, extern);
            ini = num + getDefaultIniStr();
        } else {
            throw new UnImplementedException("未支持的类型:" + var.getType());
        }

        if (extern) {
            /*在代码段中创建*/
            gen(String.format("\t\t%s\t%s", EXTERNDEF, declaration));
        } else {
            declaration = declaration + ini;
            genDeclaration(declaration, isConst);
        }
        var.setMemoryAddr(direct);
        var.addAddress(direct);
    }

    private String transformTypeDecorator(String decorator, boolean extern) {
        decorator = ((extern) ? ":" + decorator : decorator + "\t");
        return decorator;
    }

    /**
     * @return 若符号表中存在常量零, 返回它, 否则加入符号表并返回新创建的常量
     */
    private DirectAddress addZeroLiteral(boolean isSinglePrecision) {
        DirectAddress zero;
        Literal zeroLxr;
        if (isSinglePrecision) {
            zero = DirectAddress.ZERO_IN_FLOAT;
            zeroLxr = Literal.ZERO_IN_FLOAT;
        } else {
            zero = DirectAddress.ZERO_IN_DOUBLE;
            zeroLxr = Literal.ZERO_IN_DOUBLE;
        }

        return addFloatLiteral(zeroLxr.getLxrValue(), zero);
    }

    private DirectAddress addFloatLiteral(String lxr, DirectAddress directAddress) {
        Type type = directAddress.getIRType();
        if (!globalTable.containFloatLiteral(lxr, type)) {
            globalTable.addFloatLiteral(lxr, type, directAddress);
            genDeclaration(directAddress.getDeclaration(lxr), true);
            return directAddress;
        } else {
            return globalTable.getDirectAddr(lxr, type.isSinglePrecision());
        }
    }

    private String getDefaultIniStr() {
        int iniValue = 0;
        return String.format(" dup (%d)", iniValue);
    }

    /**
     * @return 是否生成了新指令
     */
    private boolean genArithmeticInstr(OpBase base) {
//        if((srcAddr instanceof FloatRegister)||desAddr instanceof FloatRegister){
//           genFPUInstr(base);
//        }
        if (arithmeticOptimize(base, desAddr, srcAddr)) {
            return false;
        }
        if ((srcAddr instanceof SSERegister) || desAddr instanceof SSERegister) {
            genSPSSEInstr(base);
        }
        //整数指令
        else {
            gen(base, desAddr, srcAddr);
        }

        return true;
    }

    private boolean arithmeticOptimize(OpBase base, Address desAddr, Address srcAddr) {

        if (base == ADD || base == SUB) {
            //加减0
            return srcAddr instanceof Literal && ((Literal) srcAddr).isZero();
        } else if (base == MUL || base == DIV || base == IMUL || base == IDIV) {
            //乘除1
            return srcAddr instanceof Literal && ((Literal) srcAddr).isOne();
        }
        return false;
    }

    /**
     * 创建FPU指令
     */
    private  void genFPUInstr(OpBase base) {
        pw.println("##genFPUInstr");
        MemRef storeAddr = null;
        List<OpPostfix> opPostfixes = new LinkedList<>();
        releaseAddr(desAddr);
        swapSrcAndDes();

        //op R,M
        if (!(srcAddr instanceof FloatRegister)) {
            FloatRegister fr = (FloatRegister) desAddr;
            if (!fr.isTop()) {
                setSrcAddr(loadVar(((MemRef) srcAddr).getVar()));
            }
        }
        //op M,R
        else if (!(desAddr instanceof FloatRegister fdes)) {
            storeAddr = (MemRef) desAddr;
            setDesAddr(loadVar(((MemRef) desAddr).getVar()));
        }
        //op R,R
        else {
            FloatRegister fsrc = (FloatRegister) srcAddr;
            if (!fsrc.isTop() && !fdes.isTop()) {
                setDesAddr(moveFloatRegToTop((Register) desAddr));
            }
        }

        //形成指令
        HashMap<OpBase, OpBase> toFloatOP = new HashMap<>();
        toFloatOP.put(ADD, FADD);
        toFloatOP.put(SUB, FSUB);
        toFloatOP.put(MUL, FMUL);
        OpBase op = toFloatOP.get(base);
        Register r = FloatRegister.peek();
        if (!r.isAvailable()) {
            opPostfixes.add(OpPostfix.P);
        }

        Address src = srcAddr, des = desAddr;
        if (isTop(desAddr) && (srcAddr instanceof MemRef)) {
            des = null;
        }
        if ((des instanceof FloatRegister) && (((FloatRegister) des).getPos() == 1)
                && (isTop(src))) {
            src = des = null;
        }
        if (src instanceof FloatRegister)
            src = new StackAddress(((FloatRegister) src).getPos());
        if (des instanceof FloatRegister)
            des = new StackAddress(((FloatRegister) des).getPos());
        gen(op, opPostfixes, des, src);
        if (storeAddr != null) {
            storeFloatToMem(storeAddr);
        }
    }

    /**
     * 创建标量浮点指令
     */
    private void genSPSSEInstr(OpBase base) {
        pw.println("##genFPUInstr");
        if (base == IMUL)
            base = MUL;
        List<OpPostfix> opPostfixes = new LinkedList<>();
        MemRef storeAddr = null;
        releaseAddr(desAddr);
        swapSrcAndDes();

        //形成指令
        opPostfixes.add(getSSEInstrPostfix(desAddr, srcAddr, base, false));
        if (desAddr instanceof MemRef) {
            setDesAddr(loadVar(((MemRef) desAddr).getVar()));
        }
        gen(base, opPostfixes, desAddr, srcAddr);
        if (storeAddr != null) {
            storeSDFPTOMem(storeAddr, (Register) desAddr);
        }
    }

    private boolean isTop(Address address) {
        return (address instanceof FloatRegister)
                && (((FloatRegister) address).isTop());
    }

    /**
     * 交换源和目的寄存器
     */
    private void swapSrcAndDes() {
        if (srcAddr instanceof Register && (((Register) srcAddr).canBeDestination()) &&
                !(desAddr instanceof Register)) {
            pw.println("交换:" + srcAddr + "和" + desAddr);
            Address addr = srcAddr;
            srcAddr = desAddr;
            desAddr = addr;
        }
    }

    /**
     * 创建比较指令
     */
    private void genCmp(Value desV, Address des, Address src) {
        if (des.isFloat() && (src.isFloat()
                || ((src instanceof Literal literal) && (literal.getLxrValue().equals("0"))))
        ) {
            OpBase base = COMI;
            OpPostfix postfix = getSSEInstrPostfix(des, src, base, false);
            //目标操作数只能能为寄存器
            if (des instanceof MemRef) {
                Register r = loadVar((Var) desV);
                des = r;
            }
            //src为字面量0
            if (src instanceof Literal) {
                Register tr = loadValueToTempR((Literal) src);
                //类型转换
                Register fr = findAvailableReg(des.getIRType());
                genMove(fr, tr);
                src = fr;
            }
            gen(base, postfix, des, src);
        } else if (!des.isFloat() && !src.isFloat()) {
            gen(CMP, des, src);
        } else {
            throw new RuntimeException("des=" + des + ",src=" + src);
        }
    }

    private OpPostfix getSSEInstrPostfix(Address des, Address src, OpBase base, boolean isMov) {
        //2023-10-18 19:52:02直接使用movaps在sse寄存器之间传输
        if (isMov && des.isRegister() && src.isRegister())
            return OpPostfix.APS;
        else {
            if (des.isDoublePrecision() || src.isDoublePrecision()) {
                return OpPostfix.SD;
            } else {
                return OpPostfix.SS;
            }
        }
    }

    /**
     * 创建字符窜指令
     */
    private void genStrInstr(OpBase base, OpPostfix postfix) {
        LinkedList<OpPostfix> postfixes = new LinkedList<>();
        postfixes.add(postfix);
        gen(base, postfixes, OpPrefix.rep, null, null, curIRid, curLocation, true);
    }

    /**
     * 创建指令
     */
    public AsmInstr gen(OpBase base, List<OpPostfix> postfixes, OpPrefix prefix, Address des, Address src, int id,
                        Location location, boolean addImmediately) {
        AsmInstr instr;
        if (arithmeticOptimize(base, des, src)) {
            return null;
        }
        if (src instanceof Literal literal && (literal.isOne())
                && (base == ADD || base == SUB)
                && (!des.isFloat())) {
            base = (base == ADD) ? INC : DEC;
            instr = new AsmInstr(new AsmOP(base), des, null, id, location);
        } else {
            AsmOP op = new AsmOP(base, postfixes, prefix);
            instr = new AsmInstr(op, des, src, id, location);
        }

        if (curLabel != null && addImmediately) {
            instr.setLabel(curLabel);
            curLabel = null;
        }
        if (addImmediately) {
            addCode(instr);
            pw.println("$创建指令:" + instr);
        }
        if (postfixes != null && postfixes.contains(OpPostfix.P)) {
            FloatRegister.pop();
        }
        return instr;
    }

    public AsmInstr gen(OpBase base, List<OpPostfix> postfixes, Address des, Address src, int id,
                        Location location, boolean addImmediately) {
        return gen(base, postfixes, null, des, src, id, location, addImmediately);
    }

    public AsmInstr gen(OpBase base, Address des, Address src) {
        return gen(base, null, des, src, curIRid, curLocation, true);
    }

    public AsmInstr gen(OpBase base, List<OpPostfix> postfixes, Address des, Address src) {
        return gen(base, postfixes, des, src, curIRid, curLocation, true);
    }

    public AsmInstr gen(OpBase base, OpPostfix postfix, Address des, Address src) {
        List<OpPostfix> postfixes = new LinkedList<>();
        postfixes.add(postfix);
        return gen(base, postfixes, des, src, curIRid, curLocation, true);
    }

    /**
     * 创建指令并插到目标指令号为id的位置
     */
    public AsmInstr insertAsm(OpBase base, Address des, Address src, int insertPos, int irPos, Location irLocation) {
        AsmInstr instr = genWithoutAdd(base, des, src, irPos, irLocation);
        addCode(insertPos, instr);
        return instr;
    }

    public void insertAsm(ASM code, int pos) {
        addCode(pos, code);
    }

    public AsmInstr genWithoutAdd(OpBase base, Address des, Address src, int id, Location location) {
        return gen(base, null, des, src, id, location, false);
    }

    public AsmDirective gen(String s) {
        return gen(s, true);
    }

    public AsmDirective genDeclaration(String s, boolean isConst) {
        AsmDirective asmDirective = new AsmDirective(s);
        addDeclaration(asmDirective, isConst);
        return asmDirective;
    }

    public AsmDirective gen(String s, boolean addImmediately) {
        AsmDirective d = new AsmDirective(s);
        if (addImmediately)
            addCode(d);
        return d;
    }

    public AsmInstr genMove(Address des, Address src, boolean addImmediately, int irId, Location location,
                            boolean needClean) {

        List<OpPostfix> postfixes = null;
        Type st = src.getIRType(), dt = des.getIRType();
        OpPostfix postfix;
        OpBase base;
        boolean srcIsFloat = src.isFloat(),
                desIsFloat = des.isFloat();
        RegisterState desState = null, srcState = null;

        //加载地址
        if (src.isLeftValue()) {
            base = LEA;
        }
        //整数传送
        else if (!srcIsFloat && !desIsFloat) {
            base = MOV;
            eliminateUnSupportedMoveInstr(des, src);
        }
        //浮点数传送
        else if (srcIsFloat && desIsFloat) {
            postfixes = new LinkedList<>();
            /*浮点数之间的转换*/
            if (st.getWidth() != dt.getWidth()) {
                base = CVT;
                //单精度->双精度
                if (st.isSinglePrecision()) {
                    postfix = OpPostfix.SS2SD;
                }
                //双精度->单精度
                else {
                    postfix = OpPostfix.SD2SS;
                }
            } else {
                base = MOV;
                postfix = getSSEInstrPostfix(des, src, base, true);
            }
            postfixes.add(postfix);
        }
        //整数与浮点数转换
        else {
            base = CVT;
            postfixes = new LinkedList<>();
            //此指令的整数操作数不能为8位或16位
            int desWidth = dt.getWidth(), srcWidth = st.getWidth();
            if ((des instanceof Register desR) &&
                    (desWidth == TypeWidth.byteWidth || desWidth == TypeWidth.wordWidth)) {
                desState = desR.getRegState();
                desR.shiftToState(int32);
            }
            if (src instanceof Register srcR
                    && (srcWidth == TypeWidth.byteWidth || srcWidth == TypeWidth.wordWidth)) {
                srcState = srcR.getRegState();
                srcR.shiftToState(int32);
            }

            //整数转换为浮点数
            if (!srcIsFloat) {
                postfix = (des.isSinglePrecision()) ? OpPostfix.SI2SS
                        : OpPostfix.SI2SD;
                //需要先将整数字面量加载到寄存器
                if (src instanceof Literal) {
                    Register tempR = loadValueToTempR((Literal) src);
                    src = tempR;
                }
            }
            //浮点数转换为整数(截断)
            else {
                postfix = (srcAddr.isSinglePrecision()) ? OpPostfix.TSS2SI : OpPostfix.TSD2SI;
            }
            postfixes.add(postfix);
        }

        //将mov r,0转换为xor r,r
        if ((src instanceof Literal) && (((Literal) src).getLxrValue().equals("0")))
            if (base == MOV && (des instanceof Register)) {
                return gen(XOR, null, des, des, curIRid, curLocation, addImmediately);
            }

        Address desAddr = des;
        //转换操作的目的操作数不能为内存操作数
        boolean needToFindDesReg = !(des instanceof Register) && base == CVT;
        if (needToFindDesReg) {
            Register tempR = findAvailableReg(des.getIRType());
            releaseAddr(des);
            desAddr = tempR;
        }

        //用于形成扩展传输指令
        if (base == MOV && (postfixes == null || postfixes.isEmpty())
                && needClean && src.getWidth()!=TypeWidth.ptrWidth ) {
            if(des instanceof Register desR){
                desR.shiftToState(pointer);
            }
            base= MOVSXD;
        }
        AsmInstr instr = gen(base, postfixes, desAddr, src, irId, location, addImmediately);

        if (!des.equals(desAddr)) {
            genMove(des, desAddr);
        }

        /*恢复寄存器状态*/
        if (desState != null) {
            ((Register) des).shiftToState(desState);
        }
        if (srcState != null) {
            ((Register) src).shiftToState(srcState);
        }

        return instr;
    }

    private AsmInstr genExtensionMove(Address des, Address src, OpBase base) {
        src = eliminateUnSupportedMoveInstr(des, src);
        AsmInstr instr = gen(base, des, src);
        return instr;
    }

    private Address eliminateUnSupportedMoveInstr(Address des, Address src) {
        //消除体系结构不支持的移动指令
        if ((des instanceof Register) && (src instanceof Register srcR)
                && !Register.isCompatible((Register) des, (Register) src)) {
            Register desR = (Register) des;
            boolean b1 = srcR.isUsing(), b2 = desR.isUsing();

            srcR.setUsing(true);
            desR.setUsing(true);
            Register tempR = findCompatibleReg(desR);
            genMove(tempR, srcR);

            srcR.setUsing(b1);
            desR.setUsing(b2);
            src = tempR;
        }
        return src;
    }

    private AsmInstr genMove(Address des, Address src) {
        return genMove(des, src, true, curIRid, curLocation,false);
    }

    private AsmInstr genMove(Address des,Address src,boolean needClean){
        return genMove(des, src, true, curIRid, curLocation,needClean);
    }

    /**
     * 用于生成sse指令集的传输指令,如movd,movq等
     */
    private AsmInstr genSSEMove(Address des, Address src) {
        int desW = des.getIRType().getWidth(), srcW = src.getIRType().getWidth();
        OpBase base = MOV;
        OpPostfix postfix;

        if (desW == TypeWidth.doubleWidth && srcW == TypeWidth.doubleWidth) {
            postfix = OpPostfix.Q;
        } else if (desW == TypeWidth.floatWidth && srcW == TypeWidth.floatWidth) {
            postfix = OpPostfix.D;
        } else {
            throw new UnImplementedException("未实现其他宽度的sse传输指令");
        }

        return gen(base, postfix, des, src);
    }

    private void cleanRegister(Register r) {
        if (!r.isClean()) {
            r.shiftToState(pointer);
            gen(XOR, r, r);
            r.clean();
            r.loadPreState();
        }
    }

    /**
     * 对寄存器r进行零扩展或符号扩展
     * 除了有符号类型向有符号类型转换进行符号扩展,
     * 其余情况一律进行零扩展
     */
    private void zeroOrSignExtend(Register r, Type targetType,
                                  boolean srcIsSigned, boolean desIsSigned) {
        if (r.getIRType().getWidth() == targetType.getWidth())
            return;
        if (!r.isClean(r.getIRType(), targetType)) {
            //移动ah之类的寄存器中的内容
            if (r.HOPartNeedToBeMoved(targetType)) {
                moveReg(r.getHBPart());
            }
            boolean zeroExtension = !(srcIsSigned && desIsSigned);
            r.cleanWhenExtension(r.getIRType(), targetType);
            OpBase base = zeroExtension ? MOVZX : MOVSX;
            //32-64bit
            if (targetType.getWidth() == TypeWidth.qwordWidth &&
                    r.getWidth() == TypeWidth.dwordWidth) {
                if (zeroExtension) {
                    base = MOV;
                    gen(base, r, r);
                    r.shiftToState(targetType);
                    return;
                } else {
                    base = MOVSXD;
                }
            }
            setSrcAddr(r.toRegAddress());
            r.shiftToState(targetType);
            setDesAddr(r);
            genExtensionMove(desAddr, srcAddr, base);
        }
    }

    /**
     * 生成汇编注释
     */
    public Comment genComment(String comment) {
        return genComment(comment, true);
    }

    public Comment genComment(String comment, boolean addImmediately) {
        Comment com = new Comment(comment);
        if (addImmediately)
            addCode(com);
        return com;
    }

    public void addCode(ASM asm) {
        codes.add(asm);

    }

    public void addDeclaration(AsmDirective asmDirective, boolean isConst) {
        if (isConst) {
            constDeclarations.add(asmDirective);
        } else {
            dataDeclarations.add(asmDirective);
        }
    }

    public void addCode(int index, ASM asm) {
        codes.add(index, asm);
        pw.println("#位置" + index + ",加入" + asm);
    }

    private AsmInstr getLastCode() {
        for (int i = codes.size() - 1; i >= 0; i--) {
            ASM asm = codes.get(i);
            if (asm instanceof AsmInstr) {
                return (AsmInstr) asm;
            }
        }
        return null;
    }

    private boolean flagUsable() {
        AsmInstr instr = getLastCode();
        return AsmOpUtil.hasSetFlag(instr.getOpBase());
    }

    /**
     * 绑定标签
     *
     * @return 如果该位置已经有标签了, 返回此标签;
     * 否则返回新创建的标签
     */
    private Label bindLabel() {
        if (curLabel == null) {
            curLabel = new Label();
        }
        return curLabel;
    }

    /**
     * 生成汇编文件
     */
    public void generateAsmFile() {
        BufferedReader sourceFileReader = null;
        String line;
        int lastReadLine = 0;

        try (PrintWriter asmWriter = new PrintWriter(outputCodePath + Compiler.sourceFile + ASM_FILE_POSTFIX)) {
            if (OutputSourceFileLine) {
                sourceFileReader = new BufferedReader(new FileReader(sourceCodesPath + Compiler.sourceFile + SOURCE_FILE_POSTFIX));
            }
            List<ASM> declarations = new ArrayList<>(constDeclarations);
            declarations.addAll(dataDeclarations);
//            declarations.sort(Comparator.comparing(Object::toString));
            for (ASM dcl : declarations) {
                outputDirectiveAndComment(asmWriter, (AsmDirective) dcl);
            }

            int preLine = -1, curLine = -1;
            for (ASM asm : codes) {
                if (asm instanceof AsmDirective) {
                    outputDirectiveAndComment(asmWriter, (AsmDirective) asm);
                } else {
                    AsmInstr instr = (AsmInstr) asm;
                    curLine = instr.getLocation() == null ? -1 : instr.getLocation().getLine();

                    if (OutputSourceFileLine
                            && (curLine > preLine)) {
                        line = getSourceFileLine(sourceFileReader, lastReadLine, curLine);
                        Comment comment = genComment(String.format("Line %-4d:%-40s",
                                        curLine, line),
                                false);
                        outputDirectiveAndComment(asmWriter, comment);
                        lastReadLine = curLine;
                    }
                    outputInstr(asmWriter, instr);

                    preLine = curLine;
                }
            }

            if (sourceFileReader != null)
                sourceFileReader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param lastReadLineCnt 上次读取的行的行号
     * @param desLineCnt      本次需要读取的行的行号
     */
    private String getSourceFileLine(BufferedReader reader, int lastReadLineCnt, int desLineCnt) throws IOException {
        String line = null;
//        //("start:"+lastReadLineCnt+",desLineCnt:"+desLineCnt);
        for (int i = lastReadLineCnt; i < desLineCnt; i++) {
            line = reader.readLine();
        }
        if (line != null) {
            line = LiteralProcessor.removeLineComment(line);
            return line.trim();
        } else {
            return "";
        }
    }

    /**
     * 输出汇编指令
     */
    private void outputInstr(PrintWriter writer, AsmInstr instr) {
        Address des = instr.getDes(), src = instr.getSrc();
        String commaSplit = "    ,";
        String desStr = "",
                srcStr = "";
        if (des == null && src != null) {
            desStr = src.getOutputStr();
        } else {
            if (des != null) {
                desStr = des.getOutputStr();
            }
            if (src != null)
                srcStr = src.getOutputStr();
        }
        if (!srcStr.equals("")) {
            desStr += commaSplit;
        }
        Label label = instr.getLabel();
        writer.printf("%-6s %-8s %-8s %-8s \n",
                (label == null) ? "" : label + ":",
                instr.getOp(),
                desStr,
                srcStr);
//        writer.println();
    }

    /**
     * 输出处汇编指令外的汇编文件内容
     */
    private void outputDirectiveAndComment(PrintWriter writer, AsmDirective asm) {
        if (asm.toString().contains("SEGMENT") || asm.toString().contains("\t\t.")
//                ||(asm instanceof Comment&&!(getLastCode() instanceof Comment))
        )
            writer.println();
        writer.println(asm);
        if (asm.toString().contains("ENDS"))
            writer.println();
    }

    /**
     * 用于记录翻译一个过程中的全局信息,
     * 如参数空间大小,临时空间大小,需要保存的寄存器等
     */
    class ProcessInfo {
        //函数序言的位置
        private int prologuePos;
        //函数序言对应的中间代码号
        private int IRPos;
        private Location irLocation;
        //记录RSP相对于RBP的位置
        private int RSPDifferFromRBP;
        //被调用方需要保存的寄存器
        private Set<Register> CalleeUsedRegs = new HashSet<>();
        //记录影子空间上有效的间接寻址方式
        private HashMap<RegisterAddress, MemRef> shadowSpace = new LinkedHashMap<>();
        //保存的临时变量
        private List<Temp> storeTemps = new LinkedList<>();
        //记录用以传递参数的所需的栈空间大小
        private int maxCallerAllocedStackSize = 0;

        //记录所需保存的临时变量的空间大小
        private int tempVarSize;
        //记录栈分配语句的位置
        private int posOfAllocInstr;
        //函数的符号表
        private LocalTable processTable;
        //函数尾声位置的标签
        private Label epilogueLbl = null;
        //非尾部的ret语句
        private List<AsmInstr> retInstructions = new LinkedList<>();
        //插入宏的位置
        private int macroPos;
        //判断是否有其他的,处于函数体中间的ret语句
        private boolean hasOtherRet;

        public ProcessInfo() {
        }

        public void ini() {
            CalleeUsedRegs.clear();
            shadowSpace.clear();
            maxCallerAllocedStackSize = 0;
            tempVarSize = 0;
            posOfAllocInstr = -1;
            storeTemps.clear();
            epilogueLbl = null;
            retInstructions.clear();
            hasOtherRet = false;
            //macroPos已经初始化
//            macroPos=-1;
        }

        public void ini(int differ, int curIRid, Location curLocation, int codePos) {
            RSPDifferFromRBP = differ;
            IRPos = curIRid;
            irLocation = curLocation;
            prologuePos = codePos;

            ini();
        }

        public void addCalleeReg(Register r) {
            CalleeUsedRegs.add(r);
        }

        public void addTemp(Temp temp) {
            if (!storeTemps.contains(temp)) {
                storeTemps.add(temp);
                tempVarSize += CalculateTypeWidth.getValueWidth(temp);
            }
        }

        public void setMemRefForTemps(int offsetBase) {
            int offset = offsetBase;
            for (Temp temp : storeTemps) {
                MemRef mem = temp.getMemRef();
                offset -= CalculateTypeWidth.getValueWidth(temp);
                mem.setOffset(offset);

//                insertMacroOfVarPosition(temp);
            }
        }

        public void addRspDiff(int n) {
            RSPDifferFromRBP += n;
        }

        public void shadowSpaceAddMem(Register r, MemRef memRef) {
            if (!shadowSpace.containsKey(r.toRegAddress())) {
                shadowSpace.put(r.toRegAddress(), memRef);
            }
        }

        public void setEpilogueLbl(Label epilogueLbl) {
            this.epilogueLbl = epilogueLbl;
        }

        public void addRetInstr(AsmInstr instr) {
            retInstructions.add(instr);
        }

        /**
         * 令在函数体中间的ret语句跳转到统一的出口
         */
        public void bindLabelForRetInstr() {
            if (hasOtherRet) {
                for (AsmInstr instr : retInstructions) {
                    instr.setDes(epilogueLbl);
                }
            }
        }

        /**
         * 插入_text伪指令
         */
        private void insertDotTextDirective() {
            ASM asm = gen("_TEXT\tSEGMENT", false);
            insertAsm(asm, macroPos);
        }

        public void insertMacroOfVarPosition() {
            List<Var> vars = new LinkedList<>(processTable.getAllLocals());
            vars.addAll(storeTemps);
            vars.addAll(processTable.getParams());
            for (Var var : vars) {
                insertMacroOfVarPosition(var);
            }

            insertDotTextDirective();
        }

        /**
         * 在函数体前面插入访问变量位置的宏
         */
        private void insertMacroOfVarPosition(Var var) {
            int insertPoint = macroPos;
            if(var.getPosDecorator()!=null){
                ASM asm = gen(var.getPosDecorator() + " = " + var.getMemRef().getOffset(), false);
                insertAsm(asm, insertPoint);
                prologuePos++;
            }
        }
    }

}
