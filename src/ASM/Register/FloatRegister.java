package ASM.Register;

import IR.Value;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class FloatRegister extends Register{
    private static  int frId=0;
    public static int FRegNum=8;

    public FloatRegister(){
        super("R"+frId++);
    }

    public FloatRegister(RegType type){
        super(  "R"+frId++,type);
    }

    private static LinkedList<FloatRegister> FloatRegisterStack=new LinkedList<>();
    public static final FloatRegister  R0=new FloatRegister();
    public static final FloatRegister R1=new FloatRegister();
    public static final FloatRegister R2=new FloatRegister();
    public static final FloatRegister R3=new FloatRegister();
    public static final FloatRegister R4=new FloatRegister();
    public static final FloatRegister R5=new FloatRegister();
    public static final FloatRegister R6=new FloatRegister();
    public static final FloatRegister R7=new FloatRegister();

    //栈顶指针
    private static int  TOP=7;
    static {
        FloatRegisterStack.addAll(Arrays.asList(R7,R6,R5,R4,R3,R2,R1,R0));
        pw.println("$Top="+FloatRegisterStack.get(TOP));
    }

    public static void push(Value v){
        Register topReg=peek();
        topReg.setUniqueVar(v);
        TOP=(TOP+1)%FRegNum;
    }

    /**
     * 分配空余的寄存器
     */
    public static Register getAvailableReg(){
        Register top=peek();
        if(top.isAvailable()) return top;
        TOP=(TOP+1)%FRegNum;
        return peek();
    }

    public static Register pop(){
        Register r=peek();
        pw.println("弹出"+r+":"+r.getRegisterDecorator());
        r.clear();
        TOP=(TOP+7)%FRegNum;
        return r;
    }

    /**
     * 返回距离栈顶index个偏移量的寄存器
     */
    public static Register get(int index){
        int n=(TOP+FRegNum-index)%FRegNum;
        return FloatRegisterStack.get(n);
    }

    public static List<Register> getFloatRegs(){
        int idx=FRegNum-1;
        List<Register> result=new LinkedList<>();
        while (idx!=-1){
            Register r=get(idx);
//            pw.println(idx+":"+r);
            result.add(r);
            idx--;
        }
        return result;
    }

    /**
     *返回相对栈顶寄存器的便宜量,不存在则返回-8;
     */
    public  int getPos(){
        int pos= FloatRegisterStack.indexOf(this);
        if(pos ==-1) return -8;
        pos= (TOP-pos+FRegNum)%FRegNum;
        return pos;
    }

    public boolean isTop(){
        return this==peek();
    }

    public static Register peek(){
        return FloatRegisterStack.get(TOP);
    }

    @Override
    public String toString() {
        return "ST("+getPos()+"):["+getName()+"]".toLowerCase();
    }
}
