package ASM;

import ASM.Register.Register;
import IR.Block;
import IR.Constants.Type;
import IR.Optimise.Loop;
import IR.Var;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理基本块中预先加载和循环结束后保存寄存器的操作
 * 主要负责在基本块凯斯或结尾处设置变量的地址描述符和寄存器的寄存器描述符
 */
public class RegisterLoader {

    private static void setContent(Map<Var,Register> allocationTable){
        allocationTable.keySet().forEach(var -> {
            Register r = allocationTable.get(var);
            Type type = var.getIRType();
            r.setUniqueVar(var);
            r.shiftToState(type);
            var.setUniqueAddress(r);
        });
    }

    public static void setContentOfRegisters(Block block){
        Loop loop=block.getAffiliatedLoop();
        if(loop==null||loop.getAllocationTable()==null)
            return;
        setContent(loop.getAllocationTable());
    }
}
