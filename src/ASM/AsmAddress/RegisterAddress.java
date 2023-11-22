package ASM.AsmAddress;

import ASM.Register.Register;
import IR.Constants.Type;

/**
 * 形成指令时实际运用的寄存器名,如AX,EAX
 */
public class RegisterAddress implements Address {
    private Type type;
    private String  name;
    private Register register;
    @Override
    public boolean isFloat() {
        return register.isFloatRegister();
    }

    public RegisterAddress(Register register){
        this.register=register;
        this.name=register.getName();
        this.type=register.getIRType();
    }

    public String getName() {
        return name;
    }

    public String getRegisterName(){
        return register.getRegisterName();
    }

    public Register getRegister() {
        return register;
    }

    @Override
    public String toString() {
        return name.toLowerCase();
    }

    @Override
    public boolean isLeftValue() {
        return false;
    }

    @Override
    public Type getIRType() {
        return type;
    }

    @Override
    public boolean isRegister() {
        return true;
    }

    @Override
    public int getWidth() {
        return type.getWidth();
    }
}
