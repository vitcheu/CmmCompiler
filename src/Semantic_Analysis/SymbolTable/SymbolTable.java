package Semantic_Analysis.SymbolTable;

import ASM.AsmAddress.DirectAddress;
import AST.DEFINE.Entity;
import CompileException.CompileError;
import IR.Constants.Type;
import IR.Literal;

import java.util.*;

/**
 * 保存变量,常量和函数的定义信息和引用信息的符号表
 */
public abstract class SymbolTable {
    private static int fid=0;
    public static final int globalTableId=-1;
    public static final int externTableId=-2;
    protected List<LocalTable>  children=new LinkedList<>();
    protected HashMap<floatEntry, DirectAddress> floatLiterals=new HashMap<>();

    protected int curOffset=0;
    protected int size;

    public void addChild(LocalTable table){
        children.add(table);
    }

    public List<LocalTable> getChildren() {
        return children;
    }

    abstract public Entity get(String name) throws CompileError;

    public int getCurOffset() {
        return curOffset;
    }

    public int size() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

//    public abstract Object get(VariableNode variable);


    public static class floatEntry {
        private    boolean isReal4Type;
        private  String literal;

        public floatEntry(boolean isReal4Type,String literal) {
            literal=trimFloat(literal,isReal4Type);
            this.isReal4Type = isReal4Type;
            this.literal=literal;
        }

        public static floatEntry newFloatEntry(Type type,String literal){
            return new floatEntry(type.isSinglePrecision(),literal);
        }


        public void setReal4Type(boolean real4Type) {
            isReal4Type = real4Type;
        }

        public boolean isReal4Type() {
            return isReal4Type;
        }

        public String getLiteral() {
            return literal;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            floatEntry entry = (floatEntry) o;
            return isReal4Type == entry.isReal4Type && Objects.equals(literal, entry.literal);
        }

        @Override
        public int hashCode() {
            return Objects.hash(isReal4Type, literal);
        }

        @Override
        public String toString() {
            return String.format("%s@%s",isReal4Type?"float":"double",literal);
        }
    }

    private floatEntry getFloatEntry(String lxr,boolean isReal4Type){
        return new floatEntry(isReal4Type,lxr);
    }

    private static DirectAddress newDirectAddress(Type type){
        return DirectAddress.createDirectAddress("__f"+(fid++),type);
    }

    public void addFloatLiteral(String lxr,boolean isReal4Type){
        lxr= trimFloat(lxr,isReal4Type);
        floatEntry entry=new floatEntry(isReal4Type,lxr);
        DirectAddress directAddress=newDirectAddress(isReal4Type?Type.floatLiteral:Type.doubleLiteral);
        floatLiterals.put(entry,directAddress);
    }

    public void addFloatLiteral(String lxr, Type type,DirectAddress directAddress){
        lxr= trimFloat(lxr,type.isSinglePrecision());
        floatEntry entry=floatEntry.newFloatEntry(type,lxr);
        floatLiterals.put(entry,directAddress);
    }

    public boolean containFloatLiteral(String lxr,Type type){
        lxr= trimFloat(lxr,type.isSinglePrecision());
        floatEntry entry=floatEntry.newFloatEntry(type,lxr);
        return floatLiterals.containsKey(entry);
    }

    /**
     *去掉浮点字面量后面冗余的'0'
     */
    public static String trimFloat(String lxr,boolean isReal4Type){
       if (lxr.matches("[0-9a-z]*[a-z]*[0-9a-z]*"))
           return lxr;
       if(isReal4Type){
           float f=Float.parseFloat(lxr);
           return String.valueOf(f);
       }else{
           double d=Double.parseDouble(lxr);
           return String.valueOf(d);
       }
    }

    private void setDirectAddress(String lxr,Type type,DirectAddress addr){
        floatEntry entry=floatEntry.newFloatEntry(type,lxr);
        floatLiterals.put(entry,addr);
    }

    public void setFloatDirectAddr(String lxr,DirectAddress addr){
         setDirectAddress(lxr,Type.floatLiteral,addr);
    }

    public void setDoubleDirectAddr(String lxr,DirectAddress addr){
        setDirectAddress(lxr,Type.doubleLiteral,addr);
    }

    public void setFloatLiteralType(String lxr,boolean isReal4Type){
        floatEntry oriEntry=getFloatEntry(lxr,!isReal4Type);
        DirectAddress address=floatLiterals.get(oriEntry);
        DirectAddress newAddr=DirectAddress.createDirectAddress(address,isReal4Type?Type.floatType:Type.doubleLiteral);
        if(address ==null)
            throw  new RuntimeException("should not happen,"+oriEntry);
        floatEntry entry=getFloatEntry(lxr,isReal4Type);

//        floatLiterals.remove(oriEntry);
        floatLiterals.put(entry,newAddr);
    }

    public Set<floatEntry> getFloatLiterals() {
        return floatLiterals.keySet();
    }

    public DirectAddress getAddrOfLiteral(Literal literal){
        DirectAddress directAddress= getDirectAddr(literal.getLxrValue(),
                literal.getIRType().isSinglePrecision());
        if(directAddress==null){
            throw new RuntimeException("未能找到"+literal+"的符号表项");
        }
        return directAddress;
    }

    public DirectAddress getDirectAddr(String lxr,boolean isReal4Type){
        floatEntry entry=getFloatEntry(lxr,isReal4Type);
        return floatLiterals.get(entry);
    }

    public DirectAddress getDirectAddr(floatEntry entry){
        return floatLiterals.get(entry);
    }
}
