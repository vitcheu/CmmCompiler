package ASM;

//定义指令
public class AsmDirective implements ASM{
    String str;

    public AsmDirective(String s){
        this.str=s;
    }

    @Override
    public String toString() {
        return str;
    }
}
