package ASM.AsmAddress;

import ASM.Register.Register;
import IR.Constants.Type;
import IR.Literal;
import IR.Var;

import java.util.ArrayList;
import java.util.List;

public interface MemoryAddress extends Address{
    /**
     * 返回所表示的变量
     */
    Var getVar();

    Type getIRType();

    void setIRType(Type type);

    /**
     *获取增加偏移量后的内存地址
     */
    MemoryAddress getAddressAdditionAddr(int bias);

    /**
     * 设置是否为左值标志
     */
    void setLeftValue(boolean leftValue);

    /**
     * 获取对该内存变量进行地址操作之后的地址
     */
    MemoryAddress getAddressOpResult();

    /**
     * 获取解引用所得的地址
     */
    MemoryAddress getDerefAddr(Var result);

    @Override
    default int getWidth(){
        return getIRType().getWidth();
    }

    default List<Register> getDependentRegs(){
        return  null;
    }

    /**
     * 能否作为数组访问的基址
     */
    default boolean canBeArrayBase(){
        return true;
    }
}
