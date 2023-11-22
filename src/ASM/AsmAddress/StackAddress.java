package ASM.AsmAddress;

import IR.Constants.Type;
import IR.Constants.TypeWidth;

/**
 * 形如ST(n)的相对于栈顶的地址,
 * 表示一个浮点寄存器
 */
public class StackAddress implements Address {
    int offset;
    public StackAddress(int offset){
        this.offset=offset;
    }

    public int getOffset() {
        return offset;
    }

    public boolean isTop(){
        return offset==0;
    }

    @Override
    public String toString() {
       return "ST("+offset+")";
    }


    @Override
    public boolean isFloat() {
        return true;
    }

    @Override
    public boolean isLeftValue() {
        return false;
    }

    @Override
    public Type getIRType() {
        return null;
    }

    @Override
    public int getWidth() {
        return TypeWidth.doubleWidth;
    }
}
