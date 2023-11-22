package IR;

import ASM.AsmAddress.Address;
import ASM.AsmAddress.MemRef;
import ASM.AsmAddress.MemoryAddress;
import ASM.Register.Register;
import IR.Constants.Type;
import IR.Optimise.DagNode;

import java.util.List;

public interface Var extends Result {
    int UNUSED=-2;
    int UNDEFINED=-1;
    Type getIRType();

    boolean isActive();

    boolean isGlobal();

    boolean isStatic();

    void setActive(Boolean active);

    int getNextUsed();

    void setNextUsed(int nextUsed);

    String getDescription();

    List<Address> getAddrDecorator();

    List<Register> getRegisters();

    boolean needStore();

    void addAddress(Address address);
    void setUniqueAddress(Address address);

    void removeAddress(Address address);

    MemRef getMemRef();
    void setMemRef(MemRef memRef);
    void setMemoryAddr(MemoryAddress memoryAddr);
    MemoryAddress getMemAddr();

    boolean isLeftValue();

    boolean hasLoaded();

    boolean isOperand();
    void setIsOperand(boolean operand);

    int getUsedNum();
    void incUsedNum();

    boolean isParam();

    /**
     * 获取写回地址
     */
    Address getWriteBackAddr();

    default boolean isRegParam(){
        return  false;
    }
    //表示正处于参数传递中
    boolean isPassingValueAsArg();
    void  setPassingValueAsArg(boolean passingValueAsArg);

    boolean isStillUsed();

     boolean isStillUsedInReg(Register r);

    boolean hasOtherRegisterAddr(Register theReg);

    boolean isArrayAddr();

    default void setPosDecorator(int level,int nth){
        ;
    }
    default String  getPosDecorator(){
        return "";
    }

    /**
     * @return 是否在函数调用前被保存进栈
     */
   default   boolean isStoreInStack(){
       return false;
   }

   default void setStoreInStack(boolean storeInStack){
       ;
   }

   boolean mustStoreBeforeCall();

   int getOffset();

   /**
    * 判断变量的值是否使用过
    */
   default boolean hasUsed(){
       return getUsedNum()>0;
   }

    /**
     * 获取变量所需的空间
     */
    default int getWidth(){
        return 0;
    };

    default boolean isTemp(){
        return false;
    }

    /**
     * 判断是否为偏移量,可提供该信息给代码生成器进行代码压缩
     */
    default boolean isIndex(){
        return false;
    }
     void resetUsedNum();

    boolean isFunctionPointer();
}
