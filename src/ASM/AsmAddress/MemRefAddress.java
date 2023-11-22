package ASM.AsmAddress;

import ASM.Register.Register;
import IR.Constants.Type;
import IR.Literal;

import java.util.*;

public class MemRefAddress extends MemRef {
    private MemRef srcMemRef;
    private static final int DEFAULT_BIAS =0;
    private List<RegisterAddress> RegBases=new LinkedList<>();
    private int bias = DEFAULT_BIAS;
    private Register indexReg=null;
    private String registerIndexDescriptor=null;
    private boolean canBeArrayBase=true;


    public MemRefAddress(MemRef memRef){
        super();
        ini(memRef);
    }

    /**
     *用于基址-变址-偏移量寻址
     * @param bias 相对于基址base的偏移量
     */
    public MemRefAddress(MemRefAddress base,int bias){
        super();
        ini(base);
        this.bias = bias;
    }

    public MemRefAddress(Address base,Address index,int scale,Type type){
        super();
        setIRType(type);

        if(base instanceof Register baseR){
            setRegBases(Arrays.asList(new Register[]{baseR}));
            baseR.addMemref(this);
        }else if (base instanceof MemRef mem){
            ini(mem);
        }

        if(index instanceof Register r){
            this.indexReg=r;
            this.registerIndexDescriptor = "%s%s".formatted(index.toString(), (scale == 1) ? "" : ("*" + scale));
            r.addMemref(this);
        }else if(index instanceof Literal literal){
            this.bias=Integer.parseInt (literal.getLxrValue());
        }

        canBeArrayBase=false;
    }


    public MemRefAddress(MemRef base,int bias){
        super();
        setSrcMemRef(base);
        setOffset(base.getOffset());
        setRegBases(base.getBaseList());

        setVar(base.getVar());
        setIRType(base.getIRType());
        setLxrValue(base.getLxrValue());

        this.bias = bias;
    }

    public MemRefAddress(MemRefAddress base){
        super();
        ini(base);
        this.bias=base.bias;
    }

    private void ini(MemRefAddress base){
        setSrcMemRef(base.srcMemRef);
        setOffset(base.getOffset());
//        setRegBases(base.getBaseList());
        this.RegBases=new LinkedList<>(base.RegBases);
        setVar(base.getVar());
        setIRType(base.getIRType());
        setLxrValue(base.getLxrValue());
    }

    private void ini(MemRef memRef){
        setSrcMemRef(memRef);
        setOffset(memRef.getOffset());
        setRegBases(memRef.getBaseList());
        setVar(memRef.getVar());
        setIRType(memRef.getIRType());
        setLxrValue(memRef.getLxrValue());
    }


    /**
     *
     *用于地址相加时形成新地址
     */
    public MemRefAddress getAddressAdditionAddr(int bias){
//        throw new RuntimeException();
        MemRefAddress addr= new MemRefAddress(this,this.bias+bias);
        //地址相加后形成的值是一个左值
        addr.setLeftValue(true);
        return addr;
    }

    public static MemoryAddress getAddressAdditionAddr(Address base, Address index, int scale, Type type){
        MemoryAddress addr;
        if(base instanceof DirectAddress  dircet){
            addr=DirectAddress.getAddressAdditionAddr(dircet,index,scale,type);
        }else{
            addr=new MemRefAddress(base,index,scale,type);
//            addr.setLeftValue(true);
        }

        return addr;
    }

    @Override
    protected MemoryAddress getLeaAddress() {
        MemoryAddress addr=new MemRefAddress(this);
        addr.setLeftValue(true);
        return addr;
    }


    public void setSrcMemRef(MemRef srcMemRef) {
        this.srcMemRef = srcMemRef;
        if(srcMemRef!=null)
            srcMemRef.addMemrefAddr(this);
    }

    @Override
    protected String getOffsetDescription() {
        String offsetDescription;
        if (hasPosDecorator()) {
            offsetDescription = getOffsetDescription(bias);
        } else {
            int off =offset + bias;
            offsetDescription=getOffsetDescription(off);
        }
        if(registerIndexDescriptor !=null){
            offsetDescription="+"+ registerIndexDescriptor +offsetDescription;
        }
        return offsetDescription;
    }

    public void setRegBases(List<Register> bases) {
        for(Register r:bases){
            RegisterAddress regAddr=new RegisterAddress(r);
            this.RegBases.add(regAddr);
            r.addMemref(this);
        }
    }


    public RegisterAddress getBaseAddr(){
        if(RegBases.isEmpty()) return null;
        return RegBases.get(0);
    }

    @Override
    public Register getBase(){
        RegisterAddress registerAddress=getBaseAddr();
        if(registerAddress==null)
            return null;
        return registerAddress.getRegister();
    }

    @Override
    public String getBaseDescription() {
        RegisterAddress baseA=getBaseAddr();
        return (baseA==null)?"":baseA.getRegisterName().toLowerCase();
    }

    @Override
    public void addOffset(int n) {
        offset-=n;
    }

    @Override
    public void setOffset(int offset) {
        this.offset=offset;
    }

    @Override
    public void setBase(Register base) {
        if (!RegBases.isEmpty()) {
            RegBases.clear();
            RegBases.add(new RegisterAddress(base));
        }
    }

    @Override
    public MemRefAddress toMemAddress(){
       return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        MemRefAddress that = (MemRefAddress) o;
        return bias == that.bias && Objects.equals(RegBases, that.RegBases);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), RegBases, bias);
    }

    @Override
    public List<Register> getDependentRegs() {

        List<Register> regs=new ArrayList<>();
        var superRegs =super.getDependentRegs();
        if(superRegs!=null){
            regs.addAll(superRegs);
        }
        if(RegBases!=null)
            try {
                regs.addAll(RegBases.stream().
                        map(RegisterAddress::getRegister)
                        .toList());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        if(indexReg!=null)
            regs.add(indexReg);
        return regs;
    }

    @Override
    public boolean canBeArrayBase() {
        return canBeArrayBase;
    }
}
