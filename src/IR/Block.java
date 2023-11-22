package IR;

import ASM.Register.Register;
import AST.DEFINE.DefinedFunction;
import IR.Constants.Const;
import IR.Constants.OP;
import IR.Optimise.DataFlowCalculator;
import IR.Optimise.Loop;
import IR.instruction.CallInstruction;
import IR.instruction.ConditionJump;
import IR.instruction.Instruction;
import IR.instruction.ParamInstruction;
import Parser.Entity.Location;

import java.io.PrintWriter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static IR.Optimise.DataFlowCalculator.forwardDataFlowCalculate;
import static IR.Optimise.DataFlowCalculator.interSection;

/**
 * 中间指令的基本块
 */
public class Block {
    public static boolean verbose = false;
    public static final int ENTER = 0;
    public static final int EXIT = 1;
    public static final int NORMAL = 2;
    public static final int DEFAULT_NEXT_USED_POS=-1;
    private static int bid = 0;
    private final int id;
    private final int TAG;

    private List<Instruction> instructions;
    //收集基本块中的符号表项
    private HashSet<Var> usedVars = new HashSet<>();
    private HashMap<Var, Boolean> activeMap = new HashMap<>();
    //在出口处活跃的变量集
    private HashSet<Var> activeVarsOnExit = new HashSet<>();
    //在定值前使用的变量
    private HashSet<Var> usedBeforeAssignedVars=new HashSet<>();
    private HashSet<Var> newAssignedVars=new HashSet<>();
    //可用表达式集

    private Set<Expr> availableExprs=new HashSet<>();

    //前驱节点
    private List<Block> pre = new LinkedList<>();
    //后继节点
    private List<Block> next = new LinkedList<>();
    protected Label blockLabel;
    //基本块结尾的寄存器分配状态
    private HashMap<Register, Register.RegisterContent> contents = null;

    //基本块入口处需要保存的变量
    private Map<Var,Register> needStoreVarsTable;
    //基本块出口处需要加载的变量及其被分配的寄存器
    private Map<Var,Register> needLoadVarsTable;
    //已经加载至寄存器的变量
    private Map<Var,Register> allocationTable;

    //所从属的循环
    private Loop affiliatedLoop;

    public Block(List<Instruction> instructions) {
        this.instructions = instructions;
        this.id = bid++;
        this.TAG = NORMAL;
    }

    private Block(List<Instruction> instructions, int tag) {
        this.instructions = instructions;
        this.id = bid++;
        this.TAG = tag;
    }

    public static Block getNewEnterBlock() {
        Block block = new Block(new ArrayList<>(), ENTER);
        return block;
    }

    public static Block getNewExitBlock() {
        Block block = new Block(new ArrayList<>(), EXIT);
        return block;
    }


    private void addPreBlock(Block block) {
        if (!pre.contains(block))
            this.pre.add(block);
    }

    public void addNextBlock(Block block) {
        if (!next.contains(block))
            next.add(block);
        if (!block.getPreBlocks().contains(this)) {
            block.addPreBlock(this);
        }
    }

    public Optional<Instruction> getLastInstruction() {
        if(instructions.isEmpty())
            return Optional.empty();
        return Optional.ofNullable( instructions.get(instructions.size() - 1));
    }

    public Label getBlockLabel() {
        return blockLabel;
    }

    public void setBlockLabel(Label blockLabel) {
        this.blockLabel = blockLabel;
    }

    public int getId() {
        return id;
    }

    public Location getLocationOfEnter(){
        if(instructions==null||instructions.isEmpty())
            return null;
        return instructions.get(0).getLocation();
    }

    public Location getLocationOfExit(){
        if(instructions==null||instructions.isEmpty())
            return null;
        return instructions.get(instructions.size()-1).getLocation();
    }


    public boolean isLastInstr(Instruction instr){
        if(instructions==null||instructions.isEmpty())
            return false;
        return instructions.get(instructions.size()-1).equals(instr);
    }

    public boolean isFirstInstr(Instruction instr){
        if(instructions==null||instructions.isEmpty())
            return false;
        return instructions.get(0).equals(instr);
    }

    public List<Instruction> getInstructions() {
        return instructions;
    }

    public void setInstructions(List<Instruction> instructions) {
        this.instructions = instructions;
    }

    public List<Block> getPreBlocks() {
        return pre;
    }

    public List<Block> getNextBlocks() {
        return next;
    }

    @Override
    public String toString() {
        String tagStr = "";
        if (TAG == ENTER) tagStr = "ENTER";
        else if (TAG == EXIT) tagStr = "EXIT";
        return String.format("%sB%d", TAG == NORMAL ? "" : ("(" + tagStr + ")"), id);
    }


    private void collectVars() {
        usedVars = new LinkedHashSet<>();
        for (Instruction instr : instructions) {
            collect(instr.getArg1());
            collect(instr.getArg2());
            collect(instr.getResult());
        }
    }

    private void collectVar(Var var) {
        usedVars.add(var);
    }

    private void collect(Object v) {
        if (v != null && (v instanceof Var var)) {
            collectVar(var);
        }
    }

    public boolean hasCallInstr(){
       return   instructions.stream().anyMatch(CallInstruction.class::isInstance);
    }

    public HashSet<Var> getUsedVars() {
        if (usedVars.isEmpty())
            collectVars();
        return usedVars;
    }

    public Map<Var, Register> getNeedStoreVarsTable() {
        return needStoreVarsTable;
    }

    public void setNeedStoreVarsTable(Map<Var, Register> needStoreVarsTable) {
        this.needStoreVarsTable = needStoreVarsTable;
    }

    public Map<Var, Register> getNeedLoadVarsTable() {
        return needLoadVarsTable;
    }

    public Set<Var> getLoadedVar(){
        return allocationTable.keySet();
    }

    public Map<Var,Register> getAllocationTable(){
        return allocationTable;
    }

    public void setNeedLoadVarsTable(Map<Var, Register> needLoadVarsTable) {
        this.needLoadVarsTable = needLoadVarsTable;
    }

    public boolean isLoaded(Var var){
        return allocationTable !=null&& allocationTable.containsKey(var);
    }

    public Loop getAffiliatedLoop() {
        return affiliatedLoop;
    }

    public void setAffiliatedLoop(Loop affiliatedLoop) {
        if(this.affiliatedLoop==null||this.affiliatedLoop.size()>affiliatedLoop.size()){
             this.affiliatedLoop=affiliatedLoop;
        }
    }

    public boolean isActiveOnExit(Var var){
        return activeVarsOnExit.contains(var);
    }

    public void setAllocationTable(Map<Var, Register> allocationTable) {
        this.allocationTable = allocationTable;
    }

    public Set<Var> getUsedBeforeAssignedVars(){
        return getUsedVars().stream()
                .filter(v-> activeMap.get(v))
                .collect(Collectors.toSet());
    }

    private Set<Var> getNewAssignedVars(){
       return newAssignedVars;
    }

    public Set<Var> getNewAssignedAndActiveVars(){
        return newAssignedVars.stream().
                filter(v-> activeVarsOnExit.contains(v))
                .collect(Collectors.toSet());
    }


    public HashMap<Register, Register.RegisterContent> getContents() {
        return contents;
    }

    public void setContents(HashMap<Register, Register.RegisterContent> contents) {
        this.contents = contents;
    }

    public boolean isNormal() {
        return TAG == NORMAL;
    }

    public boolean isStart() {
        return TAG == ENTER;
    }

    public boolean isEnd() {
        return TAG == EXIT;
    }

    public List<Var> getActiveVars() {
        return activeMap.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .toList();
    }

    public Set<Var> getActiveOnExitVars(){
        return activeVarsOnExit;
    }

    private void iniActiveInfo() {
        if (activeVarsOnExit == null) {
            activeVarsOnExit = new HashSet<>();
        } else {
            activeVarsOnExit.clear();
        }
        activeMap.clear();
        usedVars.stream().forEach(v -> {
            v.resetUsedNum();
            //全局变量默认在出口处都是活跃的
            boolean active = v.isGlobal();
            activeMap.put(v, active);
            /*使此时的activeMap的信息和activeVarsInExit的同步*/
            if (active)
                activeVarsOnExit.add(v);
        });
    }

    /**
     * 更新基本块出口处的活跃变量集
     *
     * @return 如果有更新, 返回true
     */
    public boolean updateActiveInfo(PrintWriter pw) {
        return DataFlowCalculator.updateDataFlowValue(block -> block.getActiveVars().size(),
                block -> {
                    block.setActives(pw, this::getActiveInfoOfExit);
                    return null;
        }, this);
    }

    private List<Var> getActiveInfoOfExit() {
        List<Var> vars = next.stream()
                .flatMap(block -> block.getActiveVars().stream())
                .distinct()
                .toList();
        /*与获取后续结点的入口处活跃变量的顺序不能颠倒,否则在自身循环的情况下会出错*/
        iniActiveInfo();
        activeVarsOnExit.addAll(vars);
        for (Var v : vars) {
            activeMap.put(v, true);
        }
        return activeVarsOnExit.stream().toList();
    }

    public HashSet<Var> getActiveVarsOnExit() {
        return activeVarsOnExit;
    }

    interface activeInfoOnExitSetter{
        void set();
    }

    private void getCommonExprsFromPreBlocks(PrintWriter pw){
        forwardDataFlowCalculate(block -> {
                    block.availableExprs.clear();
                    return null;
                }, (block, block2) -> {
                    Set<Expr> exprs = block.availableExprs;
                    getCommonExpr(block2.availableExprs, exprs);
                    return null;
                }, block -> {
                    pw.println("after:" + block.availableExprs);
                    return null;
                }
                , this);
    }
    
    public boolean updateCommonExprs(PrintWriter pw){
        return DataFlowCalculator.updateDataFlowValue(
                block -> block.getActiveVars().size(),
                block -> {
                    block.getCommonExprsFromPreBlocks(pw);
                    block.collectCommonExpr(pw);
                    return null;
                }, this);
    }



    /**
     * 找出两者的公共子表达式,并用结果覆盖set1
     * 若两者均为空,返回空,
     * 若其一为空,返回另一个
     */
    private static void getCommonExpr(Set<Expr> set1,Set<Expr> set2){
        interSection(set1,set2);
    }

    /**
     * 设置变量的活跃度为在出口处的活跃度
     */
    public void setActiveWhenExit() {
        usedVars.stream().forEach(var -> {
            boolean b= activeVarsOnExit.contains(var);
            var.setActive(b);
            if(var instanceof Temp temp){
                temp.setActiveOnExit(b);
            }
        });
    }

    /**
     * 设置变量的入口处活跃信息和临时变量的出口处活跃信息
     */
    public void setActiveWhenEnter(){
        if(needStoreVarsTable!=null){
           setActiveWhenEnter(needStoreVarsTable.keySet(),(activeVarsOnExit::contains));
        }

        setActiveWhenEnter(usedVars, activeMap::get);

        if(allocationTable !=null){
            setActiveWhenEnter(allocationTable.keySet(), var->true);
        }

        if(needLoadVarsTable!=null){
           setActiveWhenEnter(needLoadVarsTable.keySet(), var -> true);
        }
    }

    private void setActiveWhenEnter( Collection<Var> varSet, Function<Var,Boolean> activeInfoGetter){
        varSet.forEach(var -> {
            boolean b =activeInfoGetter.apply(var);
            var.setActive(b);
            setActiveOnExitOfTemp(var);
        });
    }

    private void setActiveOnExitOfTemp(Var var){
        if (var instanceof Temp temp) {
            temp.setActiveOnExit(activeVarsOnExit.contains(var));
        }
    }

    public boolean isStillUsedInBlock(int instrId,Var var){
        Optional<Instruction> optional=instructions.stream().filter(i->i.getId()==instrId)
                .findFirst();
        if(!optional.isPresent()||instructions.isEmpty()){
            throw new RuntimeException("should not happen,instrId="+instrId+",var="+var);
        }else{
            Instruction instr=optional.get();
            int pos=instr.getPosOfOperand(var);
            if(pos!=-1){
                int[] nextUsedInfo=instr.getNextUsedOfOperands();
                int nextUsedPos=nextUsedInfo[pos];
                return nextUsedPos!=Block.DEFAULT_NEXT_USED_POS
                        && instructions.stream()
                        .anyMatch(i->i.getId()==nextUsedPos);
            }
        }

        return false;
    }

    /**
     * 在设置活跃度的初始化过程中,
     * 只让新赋值且在后续基本块使用的变量在出口处活跃,
     * 为代码生成器所需
     */
    public void setActives(PrintWriter pw){
        setActives(pw, () -> {
             getActiveInfoOfExit();
            usedVars.forEach(v->{
                boolean active =
                        activeVarsOnExit.contains(v) &&
                                (needLoadVarsTable != null && needLoadVarsTable.keySet().contains(v)
                        || allocationTable != null && allocationTable.keySet().contains(v)
//                        || newAssignedVars != null && newAssignedVars.contains(v)
                        );
                activeMap.put(v,active);
            });
        });
    }

    /**
     * 标记各指令是否活跃
     */
    private void setActives(PrintWriter pw,activeInfoOnExitSetter setter) {
        collectVars();
//        getActiveInfoOfExit();
        setter.set();
        /*此时activeMap为基本块出口处的活跃变量集*/
        pw.println("Exit:" + getActiveVars());
        usedVars.forEach(v->v.setNextUsed(DEFAULT_NEXT_USED_POS));

        for (int i = instructions.size() - 1; i >= 0; i--) {
            Instruction ins = instructions.get(i);
            OP op = ins.getOp();
            verbose("\n--------------------------------------", pw);
            verbose("指令:" + ins, pw);

            if (op == OP.call) {
                CallInstruction callInstruction = (CallInstruction) ins;
                HashMap<Var, Boolean> PostCallActiveInfo = callInstruction.getAfterCallActiveInfo();
                //保存变量在此指令后的活跃信息
                List<Value> args = callInstruction.getArgs();
                for (Value arg : args) {
                    if (arg instanceof Var) {
                        Var varg = (Var) arg;
                        //保存当前变量的活跃状态
                        PostCallActiveInfo.put(varg, varg.isActive());
                        //变量在此指令之前是活跃的
                        varg.setActive(true);
                        activeMap.put(varg, true);
                    }
                }

                if(callInstruction.isDirectCall()){
                    setVisited(ins,ins.getArg1(),Const.Ypos,true,pw);
                }
                //设置结果的活跃信息
                setVisited(ins, ins.getResult(), Const.Xpos, false, pw);
            } else {
                setVisited(ins, ins.getResult(), Const.Xpos, false, pw);
                setVisited(ins, ins.getArg1(), Const.Ypos, true, pw);
                //不能重复设置
                if (ins.getArg2() != ins.getArg1())
                    setVisited(ins, ins.getArg2(), Const.Zpos, true, pw);
//            verbose();("填充后,活跃信息:"+Arrays.toString(ins.getActiveInfo()));
            }
        }

        for (Instruction ins : instructions) {
            verbose("\n--------------------------------------", pw);
            verbose("指令:" + ins, pw);
            Value arg1 = ins.getArg1(),
                    arg2 = ins.getArg2();
            Result result = ins.getResult();
            if (arg1 != null && arg1 instanceof Var)
                verbose("arg1:" + arg1 + ",active:" + ins.getActiveInfo()[Const.Ypos], pw);
            if (arg2 != null && arg2 instanceof Var)
                verbose("arg2:" + arg2 + ",active:" + ins.getActiveInfo()[Const.Zpos], pw);
            if (result != null)
                verbose("result:" + result + ",active:" + ins.getActiveInfo()[Const.Xpos], pw);
            verbose("nextUsed:" + ins.getNextUsed(), pw);
        }

    }

    public void setVisited(Instruction ins, Value operand, int pos, boolean isSrc
            , PrintWriter pw) {
        if (operand == null || !(operand instanceof Var))
            return;
        Var var = (Var) operand;

        boolean[] info = ins.getActiveInfo();
        int[] nextUsedPos=ins.getNextUsedOfOperands();
        //填入当前变量的活跃信息
        boolean newActiveValue;
        if (activeMap.get(var) == null) {
            newActiveValue = false;
        } else
            newActiveValue = activeMap.get(var);
        info[pos] = newActiveValue;
        nextUsedPos[pos]=var.getNextUsed();
        verbose("#info[" + pos + "]填入" + var + "的活跃信息:" + newActiveValue, pw);

        //更改变量活跃状态
        boolean active = isSrc;
        //当变量代表一个左值且位于赋值运算左侧时(不包括形成时的指令),它在该指令前是活跃的
        if (var instanceof Temp && var.isLeftValue()) {
            active = !var.isActive();
        }

        if(!isSrc){
            newAssignedVars.add(var);
        }

        var.setActive(active);
        activeMap.put(var, active);
        verbose("#flag=" + active + "," + var + "在此指令前是否活跃?:" + activeMap.get(var), pw);

        //设置后续使用信息
        if (isSrc) {
            var.setNextUsed(ins.getId());
            var.incUsedNum();
            verbose("#" + var + "的下一次使用位置:" + ins.getId(), pw);
        } else {
            ins.setNextUsed(var.getNextUsed());
            var.setNextUsed(DEFAULT_NEXT_USED_POS);
            verbose("#指令" + ins + "的下一次使用:" + var.getNextUsed(), pw);
        }
    }

    /*收集基本内可用的*/
    public void collectCommonExpr(PrintWriter pw) {
        verbose("\n\n\n--------------------------<" + this + (this.getBlockLabel() == null ? "" :
                " (" + this.getBlockLabel() + ")") + ">------------------------------", pw);
        Set<Expr> exprSet =availableExprs;
        HashMap<Var, List<Expr>> exprsOfVar = new HashMap<>();
        /*可以用值value代替键var的每次出现*/
        HashMap<Var, Value> replacement = new HashMap<>();

        for (Instruction instr : instructions) {
            OP op = instr.getOp();
            Result result = instr.getResult();
            Var x = (result != null && result instanceof Var var) ? var : null;
            verbose("\n--------------------------------------", pw);
            verbose("指令:" + instr, pw);

            if (op == OP.call
//                        ||op==OP.assign&&x.isLeftValue()
            ) {
                exprSet.clear();
                exprsOfVar.clear();
                replacement.clear();
            }
            else {
                replaceOperandOfInstr(instr,replacement);
                if (op == OP.assign || op == OP.param || op == OP.ret ||
                        ((op == OP.jump || op == OP.if_jump || op == OP.ifFalse_jump) && !(instr instanceof ConditionJump))
                        || (op == OP.array && x.isLeftValue())) {
                    if(op==OP.assign){
                        replacement.put(x,instr.getArg1());
                        removeAllExprOf(exprsOfVar,exprSet,replacement,(Var) instr.getResult());
                    }
                    ;
                } else {
                    Expr newExpr = Expr.getExprOfInstr(instr);
                    /*用可用表达式替换此语句,使其变成赋值语句*/
                    if (exprSet.contains(newExpr)) {
                        if (x != null) {
                            Value replacementBy = exprSet.stream().
                                    filter(e->e.equals(newExpr)).
                                    findFirst().get().getResult();
                            instr.transformToAssignInstr(replacementBy);
                            replacement.put(x, replacementBy);
                            verbose("#transform to assign instruction:\n#" + instr, pw);
                        }
                    }
                    /*加入新的可用表达式*/
                    else {
                        exprSet.add(newExpr);
                        addExprOf(exprsOfVar,newExpr.arg1,newExpr);
                        addExprOf(exprsOfVar,newExpr.arg2,newExpr);
                        if (x != null) {
                            removeAllExprOf(exprsOfVar,exprSet,replacement,x);
                        }
                    }
                }

                String str=getAvailableExprStr();
                verbose(str,pw);
            }
        }

        verbose("\n--------------------------</" +
                this + (this.getBlockLabel() == null ? "" : " (" + this.getBlockLabel() + ")") + ">------------------------------", pw);
        eliminateUnusedAssignInstr(pw);
        setLabelToFirstInstr();
        printAllInstructions(pw);
    }

    /**
     * 设置第一条语句为本基本快的标签
     * 此方法一般为基本块的某些指令被删除时调用
     */
    public void setLabelToFirstInstr(){
        if(isNormal()&&!instructions.isEmpty()){
            Instruction firstInstr=instructions.get(0);
            firstInstr.setLabel(blockLabel);
        }
    }

    /**
     * 去除生成的所有以x为操作数的表达式
     * 删除使用x的替换关系
     * @param x
     */
    private void removeAllExprOf(HashMap<Var, List<Expr>> exprsOfVar,Set<Expr> availableExprs,
                                 HashMap<Var, Value> replacement ,Var x){
        List<Expr> exprs = exprsOfVar.get(x);
        if (exprs != null) {
            exprs.forEach(e->availableExprs.remove(e));
            exprs.clear();
        }
        var replacedVars=new ArrayList<>( replacement.keySet());
        replacedVars.stream().filter(var -> replacement.get(var).equals(x))
                .forEach(replacement::remove);
    }

    private void addExprOf(HashMap<Var, List<Expr>> exprsOfVar,Value operand,Expr expr){
        if(operand!=null && operand instanceof Var var){
            List<Expr> exprs=exprsOfVar.get(var);
            if(exprs==null)
                exprs=new ArrayList<>();
            exprs.add(expr);
            exprsOfVar.put(var,exprs);
        }
    }

    public String getAvailableExprStr(){
        StringBuilder builder = new StringBuilder("[");
        availableExprs.stream().forEach(e -> builder.append(e + "\n"));
        if (builder.length() > 0) builder.replace(builder.length() - 1, builder.length(), "");
        if (builder.length() > 0) builder.append("]");
        return"exprSet:\n" + builder;
    }

    private void replaceOperandOfInstr(Instruction instr,HashMap<Var,Value> replacementMap){
        replace(instr,Instruction.YPOS,instr.getArg1(),replacementMap);
        replace(instr,Instruction.ZPOS,instr.getArg2(),replacementMap);
    }

    private void replace(Instruction instr,int posOfInstr,Value oriValue,HashMap<Var,Value> replacementMap){
        if(oriValue!=null&&oriValue instanceof Var oriVar){
            //对具名变量的取地址不应该被替换
            if(instr.getOp()== OP.lea&&!oriVar.isTemp()){
                ;
            }
            else if(replacementMap.containsKey(oriVar)){
                Value replaceBy=replacementMap.get(oriVar);
                instr.setArg(posOfInstr,replaceBy);
            }
        }
    }

    /**
     * 剔除赋值给不活跃变量的无用的语句
     */
    private void eliminateUnusedAssignInstr(PrintWriter pw){
        /*记录变量的定值是否使用过*/
        HashMap<Var,Boolean> used=new HashMap<>();
        List<Instruction> removedInstrs=new ArrayList<>();
        HashMap<Instruction,ParamInstruction> paramDependency=new HashMap<>();

        for (int i=instructions.size()-1;i>=0;i--) {
            Instruction instr=instructions.get(i);
            instr.getOperands().stream()
                    .forEach(value -> setUsedOfOperand(value, used));
            Result result = instr.getResult();
            if(instr instanceof ParamInstruction pi){
                List<Instruction> paramInstructions=pi.getStoreInstructions();
                if(paramInstructions!=null){
                    paramInstructions.forEach(instruction -> paramDependency.put(instruction,pi));
                }
            }

            if (result != null && result instanceof Var x
                    &&canBeEliminated(x,instr.getOp())
            ) {
                if (used.get(x) == null) {
                    removedInstrs.add(instr);
                }
            }
        }
        verbose("removed Instructions:",pw);
        removedInstrs.forEach(i->{
            pw.println(i);
            instructions.remove(i);
            ParamInstruction pi=paramDependency.get(i);
            if(pi!=null){
                pi.removeInstr(i);
            }
        });

    }

    /**
     * 重新设置call指令所需的参数
     */
    public void resetArgsOfCallInstrs(){
        LinkedList<ParamInstruction> paramInstructions=new LinkedList<>();
        instructions.stream().filter(i->(i instanceof ParamInstruction)||i instanceof CallInstruction)
                .forEach(i->{
                    if(i instanceof ParamInstruction pi){
                        paramInstructions.addLast(pi);
                    }else{
                        CallInstruction ci=(CallInstruction)i;
                        ci.collectArgs(paramInstructions);
                    }
                });
    }

    private boolean canBeEliminated(Var result,OP op ){
        return  !result.isLeftValue() &&  op!= OP.call
                && !(activeVarsOnExit.contains(result)||result.isGlobal()||result.isStatic());  //剔除在基本块出口处不活跃的变量
    }

    /**
     * 设置变量的使用标志
     */
    private void setUsedOfOperand(Value value,  HashMap<Var,Boolean> usedMap){
        if(value instanceof Var var)
            usedMap.put(var,true);
    }


    private void verbose(String msg, PrintWriter printWriter) {
        if (verbose) {
            printWriter.println(msg);
        }
    }

    public void printAllInstructions(PrintWriter blockWriter) {
        printSep(blockWriter,true);
        blockWriter.println("前驱:" + this.getPreBlocks());
        int preLine=-2;
        for (Instruction i : this.getInstructions()) {
            int curLine=i.getLocation().getLine();
            if(curLine!=preLine)
                blockWriter.println();
            blockWriter.println((String.format("%-48s\t%-10s", i,
                    curLine)));
            preLine=curLine;
        }
        blockWriter.println("后继:" + this.getNextBlocks());
        printSep(blockWriter,false);
    }

    public  void printSep(PrintWriter printWriter,boolean start){
        if(start)
            printWriter.println("\n--------------------------<" + this + (this.getBlockLabel() == null ? "" : " (" + this.getBlockLabel() + ")")
                    + ">------------------------------");
        else
            printWriter.println("\n--------------------------</" + this + ">-----------------------------");
    }

    public static Block createIntermediateBlock(Block from,Block to){
        if(!from.next.contains(to)||!to.pre.contains(from))
            throw new RuntimeException();
        Block block=new Block(new ArrayList<>());

        from.next.remove(to);
        to.pre.remove(from);

        block.addPreBlock(from);
        block.addNextBlock(to);

        from.addNextBlock(block);
        to.addPreBlock(block);

        return block;
    }

    public static void printAllBlocks(List<Block> blocks,PrintWriter pw){
        blocks.forEach(block -> {
            block.printAllInstructions(pw);
            block.resetArgsOfCallInstrs();
        });
    }
}

class Expr {
    Var result;
    OP op;
    Value arg1;
    Value arg2;

    private Expr(OP op, Value arg1, Value arg2, Result result) {
        this.op = op;
        this.arg1 = arg1;
        this.arg2 = arg2;
        if (result != null && result instanceof Var var) {
            this.result = var;
        }
    }

    public static Expr getExprOfInstr(Instruction instr) {
        OP op = instr.getOp();
        return new Expr(instr.getOp(), instr.getArg1(), instr.getArg2(), instr.getResult());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Expr expr = (Expr) o;
        return op == expr.op && Objects.equals(arg1, expr.arg1) && Objects.equals(arg2, expr.arg2)
                || OP.exchangeable(op) && (Objects.equals(arg1, expr.arg2) && Objects.equals(arg2, expr.arg1));
    }

    @Override
    public int hashCode() {
        return Objects.hash(op, arg1, arg2);
    }

    public String toString() {
        String s1 = (arg1 == null) ? "" : arg1.toString();
        String s2 = (arg2 == null) ? "" : arg2.toString();
        String s3 = (result == null) ? "" : result.toString();
        return String.format("%-5s\t%-8s\t%-8s ->%-4s", op.getDescriptionStr(), s1, s2, s3);
    }

    public Var getResult() {
        return result;
    }
}

