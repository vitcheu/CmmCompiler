package ASM.Constants;

import java.util.LinkedList;
import java.util.List;

public class AsmOP {
    private  OpBase base;
    private List<OpPostfix> opPostfixes=null;
    private OpPrefix prefix=null;

    public AsmOP(OpBase base, List<OpPostfix> opPostfixes){
        this.base=base;
        if(opPostfixes==null){
            this.opPostfixes=new LinkedList<>();
        }else{
            this.opPostfixes=opPostfixes;
        }
    }

    public AsmOP(OpBase base){
        this.base=base;
        if(opPostfixes==null){
            this.opPostfixes=new LinkedList<>();
        }
    }

    public AsmOP(OpBase base, List<OpPostfix> opPostfixes,OpPrefix prefix){
        this.base=base;
        if(opPostfixes==null){
            this.opPostfixes=new LinkedList<>();
        }else{
            this.opPostfixes=opPostfixes;
        }
        this.prefix=prefix;
    }

    public AsmOP(OpBase base, OpPrefix prefix){
        this.base=base;
        opPostfixes=new LinkedList<>();
        this.prefix=prefix;
    }

    public void addPostfix(OpPostfix postfix){
        if(!opPostfixes.contains(postfix)){
            opPostfixes.add(postfix);
        }
    }

    public OpBase getBase() {
        return base;
    }

    public void setBase(OpBase base) {
        this.base = base;
    }

    public List<OpPostfix> getOpPostfixes() {
        return opPostfixes;
    }


    public void setOpPostfixes(List<OpPostfix> opPostfixes) {
        this.opPostfixes = opPostfixes;
    }

    @Override
    public String toString() {
        String ps=(prefix==null)?"":prefix+" ";
        String s;
        if(opPostfixes.isEmpty())
            s="";
        else if(opPostfixes.size()==1){
            s=opPostfixes.get(0).toString();
        }else{
            StringBuilder sb=new StringBuilder();
            for(OpPostfix postfix:opPostfixes){
                sb.append(postfix.toString());
            }
            s=sb.toString();
        }
        return (ps+base+s).toLowerCase();
    }

    public OpPrefix getPrefix() {
        return prefix;
    }

    public void setPrefix(OpPrefix prefix) {
        this.prefix = prefix;
    }
}
