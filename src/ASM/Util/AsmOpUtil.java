package ASM.Util;

import ASM.CodeGenerator;
import ASM.Constants.OpBase;
import IR.Literal;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static ASM.Constants.OpBase.*;

/**
 * 2023/10/9
 */
public class AsmOpUtil {
    private static Set<OpBase> setFlagOp=new HashSet<>();
    static {
        setFlagOp.add(ADD);
        setFlagOp.add(SUB);
        setFlagOp.add(MUL);
        //IMUL不设置除了C和Z的其他位
//        setFlagOp.add(IMUL);
        setFlagOp.add(DIV);
        setFlagOp.add(IDIV);
        setFlagOp.add(INC);
        setFlagOp.add(DEC);
        setFlagOp.add(NEG);
        setFlagOp.add(XOR);


        setFlagOp.add(SHL);
        setFlagOp.add(SHR);
        setFlagOp.add(ROL);
        setFlagOp.add(ROR);
        setFlagOp.add(RCL);
        setFlagOp.add(RCR);
        setFlagOp.add(SAL);
        setFlagOp.add(SAR);


        setFlagOp.add(AND);
        setFlagOp.add(OR);
        setFlagOp.add(NEG);
        setFlagOp.add(TEST);
        setFlagOp.add(CMP);

        setFlagOp.add(COMI);

        setFlagOp.add(FADD);
        setFlagOp.add(FSUB);
        setFlagOp.add(FSUBR);
        setFlagOp.add(FMUL);
        setFlagOp.add(FDIV);
        setFlagOp.add(FDIVR);
    }
    public static boolean hasSetFlag(OpBase base){
       return setFlagOp.contains(base);
    }
}
