package IR;

import ASM.ASM;
import ASM.AsmAddress.Address;
import IR.Constants.Type;

public class Label implements Result, Address, ASM {
    private static int lid=-1;
    private final int id;
    private int irPos;//标签在中间代码的位置
    private int asmPos;//标签在汇编代码的位置

    private boolean blockEdge=true;
    private String image;

    public static final Label Fall=new Label();

    public Label() {
        id=lid++;
        image="L"+id;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return image;
    }

    public void setImage(Label label) {
        this.image =label.image;
    }

    public int getIrPos() {
        return irPos;
    }

    public void setIrPos(int irPos) {
        this.irPos = irPos;
    }

    public int getAsmPos() {
        return asmPos;
    }

    public void setAsmPos(int asmPos) {
        this.asmPos = asmPos;
    }

    @Override
    public Type getIRType() {
        return null;
    }

    @Override
    public boolean isSigned() {
        return false;
    }

    @Override
    public boolean isFloat() {
        return false;
    }


    public boolean isBlockEdge() {
        return blockEdge;
    }

    public void setBlockEdge(boolean blockEdge) {
        this.blockEdge = blockEdge;
    }

    @Override
    public int getWidth() {
        return 0;
    }
}
