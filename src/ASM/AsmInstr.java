package ASM;

import ASM.AsmAddress.Address;
import ASM.AsmAddress.MemRef;
import ASM.Constants.AsmOP;
import ASM.Constants.OpBase;
import ASM.Register.Register;
import ASM.Util.LiteralProcessor;
import IR.Label;
import IR.Literal;
import Parser.Entity.Location;

//汇编语言指令
public class AsmInstr implements ASM{
    private static int asmId=0;
    private final int id;
    private Label label;
    private AsmOP op;
    private Address des;
    private Address src;
    //插入位置
    private int  insertPos=-1;
    private Location location;
//    private Location location;
    private int pos;
    public AsmInstr(AsmOP op, Address des, Address src,int pos,Location location) {
        this.op = op;
        this.des =toAsmAddress(des);
        this.src =toAsmAddress(src);
//        this.des=des;
//        this.src=src;
        this.pos=pos;
        this.id=asmId++;
        this.location=location;
    }

    private Address toAsmAddress(Address address){
        if(address instanceof Register){
            address=  Register.toRegAddress(address);
        }else if(address instanceof MemRef){
            address= ((MemRef) address).toMemAddress();
        }else if(address instanceof Literal literal){
            address= LiteralProcessor.transformLiteral(literal);
        }

        return address;
    }

    public Label getLabel() {
        return label;
    }

    public AsmOP getOp() {
        return op;
    }

    public OpBase getOpBase(){
        return op.getBase();
    }

    public Address getDes() {
        return des;
    }

    public Address getSrc() {
        return src;
    }

    public void setLabel(Label label) {
        this.label = label;
    }

    public int getPos() {
        return pos;
    }

    @Override
    public String toString() {
        return ""+
                ((label==null)?"":label)+
                op+"\t"+
                des+"\t"+
                src+"\t"+
                "("+pos+")";
    }

    public Location getLocation() {
        return location;
    }

    public int getId() {
        return id;
    }

    public void setDes(Address des) {
        this.des =toAsmAddress(des);
    }

    public void setSrc(Address src) {
        this.src =toAsmAddress(src);
    }

    public int getInsertPos() {
        return insertPos;
    }

    public void setInsertPos(int insertPos) {
        this.insertPos = insertPos;
    }
}
