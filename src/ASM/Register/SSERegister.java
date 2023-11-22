package ASM.Register;

import IR.Constants.Type;
import IR.Var;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class SSERegister extends Register{
    private static int sseId=0;

    public SSERegister(String name) {
        super(name);
    }

    public SSERegister(){
        super("Xmm"+sseId++);
    }

    public SSERegister(RegType type){
        super("Xmm"+sseId++,type);
    }

    public static List<SSERegister> sseRegs=new ArrayList<>();
    static {
        //Xmm0~Xmm15,其中Xmm0~Xmm5为易失性寄存器
        int n=16,callerRegNum=6;
        for(int i=0;i<n;i++){
            RegType type=(i>=callerRegNum)?RegType.Callee:RegType.Caller;
            SSERegister sseReg=new SSERegister(type);
            sseRegs.add(sseReg);
        }
    }

    /**
     * 分配空余的寄存器
     */
    public static Register getAvailableReg(Type type,List<Register> preferRegs){
        Register result=null;
        for(Register r:preferRegs){
            if(r.isFloat() &&r.isAvailable()){
                result=r;
                break;
            }
        }
        if (result == null) {
            for (Register r : sseRegs) {
                if (r.isAvailable()) {
                    result = r;
                    break;
                }
            }
        }
        result.shiftToState(type);
        return result;
    }

    public static List<Register> getRegs(){
       List<Register> regs=new LinkedList<>();
       regs.addAll(sseRegs);
       return regs;
    }

    @Override
    public boolean isAvailable() {
        if(isUsing()||usedAsParamReg)
            return false;
        for(Var v:registerDecorator){
            if(v.isActive()||v.isOperand()){
                return false;
            }
        }
        return true;
    }

    /**
     * 获取用于传递参数的sse寄存器
     */
    public static SSERegister[] getParamRegister(){
        int paramRegNum=4;
        SSERegister[] regs=new SSERegister[paramRegNum];
        for(int i=0;i<paramRegNum;i++){
            regs[i]=get(i);
        }
        return regs;
    }

    /**
     *@param idx 寄存器编号
     * @return 对应的寄存器,如果idx=0,那么返回寄存器Xmm0
     */
    public static SSERegister get(int idx){
        return sseRegs.get(idx);
    }
}
