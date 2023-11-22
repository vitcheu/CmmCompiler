package IR.instruction;

import Parser.Entity.Location;
import IR.*;
import IR.Constants.OP;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static IR.Constants.OP.*;

public class Instruction implements Comparable<Instruction>{
    public static final int XPOS=2;
    public static final int YPOS=0;
    public static final int ZPOS=1;
    public static final Comparator<Instruction> comparator= Instruction::compareTo;
    protected static int iId=0;
    protected final  int id;
    protected int ori_id;
    protected Label label;
    protected OP op;
    protected Value arg1;
    protected Value arg2;
    protected Result result;
    protected Location location;
    //记录指令所用的内存地址的值是否在指令所在处活跃
    private boolean[] activeInfo=new boolean[4];
    private int[] nextUsedOfOperands=new int[4];
    //指令计算的值下一次使用的位置
    protected int nextUsed=-1;

    //判断是否属于一个过程最后的ret语句
    protected boolean lastRet=false;
    //是否应该马上翻译成目标代码
    protected boolean translateImmediately=true;
    protected boolean hasSideEffect=false;


    public Instruction(Label label, OP op, Value arg1, Value arg2, Result result) {
        this.op = op;
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.result = result;
        id=iId++;
    }

    public Instruction(OP op, Value arg1, Value arg2, Result result, Location location, boolean b,boolean translateImmediately) {
        this.op = op;
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.result = result;
        this.location=location;
        id=-1;
        this.translateImmediately=translateImmediately;
    }

    public Location getLocation() {
        if(location==null)
            this.location=new Location(-2,-2);
        return location;
    }

    public Instruction(OP op, Value arg1, Value arg2, Result result, Location location,boolean translateImmediately) {
        this.op = op;
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.result = result;
        this.location=location;
        id=iId++;
        this.translateImmediately=translateImmediately;
    }

    public Instruction(OP op, Value arg1, Value arg2) {
        this.op = op;
        this.arg1 = arg1;
        this.arg2 = arg2;
        result=new Temp(this);
        id=iId++;
    }

    public void setOp(OP op) {
        this.op = op;
    }

    public int getOri_id() {
        return ori_id;
    }

    public void setOri_id(int ori_id) {
        this.ori_id = ori_id;
    }

    public Result getResult() {
        return result;
    }

    @Override
    public String toString() {
        if (OP.isBinary(op)) {
            return String.format("%s %s = %s %s %s", getIdAndLabelStr(),
                    result,
                    (arg1),
                    OP.getStr(op),
                    (arg2));
        }else if(op==OP.cast){
            return String.format("%s %s = (%s) %s",getIdAndLabelStr(),
                    result,arg2,arg1);

        } else if (OP.isUnary(op)) {
            return String.format("%s %s%s %s", getIdAndLabelStr(),
                    result == null ? "" :result+ " = ",
                    OP.getStr(op),
                    (arg1));
        } else if (OP.isNoneOperand(op)) {
            return String.format("%s %s", getIdAndLabelStr(),
                    OP.getStr(op));

        }else if(op== ret){
            return String.format("%s %s%s", getIdAndLabelStr(),
                    OP.getStr(op),(arg1==null)?"":" "+arg1);
        }
        else if(op==OP.assign){
            return String.format("%s %s = %s",getIdAndLabelStr(),
                    result,arg1);

        } else if (op == OP.param) {
            return String.format("%s %s %s", getIdAndLabelStr(),
                    OP.getStr(op), arg1);

        } else if (this instanceof ConditionJump conditionJump) {
            return String.format("%s %s %s %s %s   %s", getIdAndLabelStr(), OP.getStr(op),
                    arg1, OP.getStr(conditionJump.getJumpType()),
                    arg2, result);

        }else if(op== if_jump||op== ifFalse_jump){
            return String.format("%s %s %s %s",getIdAndLabelStr(),OP.getStr(op),
                    arg1,result);

        } else if (op == jump || op == ifFalse_jump || op == if_jump) {
            return String.format("%s %s %s", getIdAndLabelStr(),
                    OP.getStr(op), result);

        } else if (op == OP.array) {
            return String.format("%s %s =%s%s[%s]",getIdAndLabelStr(),
                    result,((result instanceof Var var&&(var.isLeftValue()))?" offset ":" "),
                    arg1,arg2);

        }else{
            throw new RuntimeException("未支持的指令类型:"+op);
        }
    }

    private String getIdAndLabelStr(){
        return String.format("%-5sI%-3d:\t",label==null?"":"("+label+") ",id);
    }

    public int getId() {
        return id;
    }

    public OP getOp() {
        return op;
    }


    public Value getArg1() {
        return arg1;
    }

    public Value getArg2() {
        return arg2;
    }

    public boolean isActive(){
        if(result instanceof Label) return  false;
        return ((Var)result).isActive();
    }

    public boolean jumpToOtherBlock(){
        return  isJumpInstr();
//                &&((Label)getResult()).isBlockEdge();
    }

    public boolean isJumpInstr(){
        return (op == if_jump || op == ifFalse_jump|| op==jump);
    }

    public boolean[] getActiveInfo() {
        return activeInfo;
    }

    public int[] getNextUsedOfOperands() {
        return nextUsedOfOperands;
    }

    public int getNextUsed() {
        return nextUsed;
    }

    public void setNextUsed(int nextUsed) {
        this.nextUsed = nextUsed;
    }

    public Label getLabel() {
        return label;
    }

    public void setLabel(Label label) {
        this.label = label;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public boolean isLastRet() {
        return lastRet;
    }

    public void setLastRet(boolean lastRet) {
        this.lastRet = lastRet;
    }

    public boolean isTranslateImmediately() {
        return translateImmediately;
    }

    public void setTranslateImmediately(boolean translateImmediately) {
        this.translateImmediately = translateImmediately;
    }

    public boolean isHasSideEffect() {
        return hasSideEffect;
    }

    public void setHasSideEffect(boolean hasSideEffect) {
        this.hasSideEffect = hasSideEffect;
    }

    @Override
    public int compareTo(Instruction o2) {
        if (this.getOri_id() == o2.getOri_id()){
            if(this.getLocation().getLine()==o2.getLocation().getLine())
                return Integer.compare(this.getId(),o2.getId());
            return this.getLocation().compareTo(o2.getLocation());
        }
        else
            return Integer.compare(this.getOri_id(), o2.getOri_id());
    }

    /**
     * 转换至赋值语句
     */
    public void transformToAssignInstr(Value right){
        this.op=assign;
        this.arg1=right;
        this.arg2=null;
    }

    public void setArg(int pos,Value newValue){
        if(pos==Instruction.YPOS){
            this.arg1=newValue;
        }else if(pos==Instruction.ZPOS){
            this.arg2=newValue;
        }else{
            throw new RuntimeException("should not happen,pos="+pos+",newValue"+newValue);
        }
    }

    public List<Value> getOperands(){
        List<Value> ret=new ArrayList<>();
        if(arg1!=null)
            ret.add(arg1);
        if(arg2!=null)
            ret.add(arg2);
        return ret;
    }

    public int getPosOfOperand(Value value){
        int pos=-1;
        if(arg1!=null&&arg1.equals(value))
            pos=YPOS;
        else if(arg2!=null&&arg2.equals(value)){
            pos=ZPOS;
        }else if(result!=null&&result.equals(value)){
            pos=XPOS;
        }
        return pos;
    }
}
