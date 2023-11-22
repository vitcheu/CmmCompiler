package ASM.Register;

import ASM.Constants.RegisterState;

/**
 * 高字节寄存器,如AH,BH...
 */
public class HBRegister extends Register{

    public HBRegister(String name) {
        super(name);
        setState(RegisterState.H);
    }

    public boolean isHB(){
        return true;
    }

    public static HBRegister AH=new HBRegister("AH");
    public static HBRegister BH=new HBRegister("BH");
    public static HBRegister CH=new HBRegister("CH");
    public static HBRegister DH=new HBRegister("DH");

}
