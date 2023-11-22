package IR.instruction;

import Parser.Entity.Location;
import IR.Constants.OP;
import IR.Result;
import IR.Value;
import IR.Var;

import java.util.LinkedList;

public class ParamInstruction extends Instruction{
    LinkedList<Instruction> storeInstructions=new LinkedList<>();
    public ParamInstruction(Value arg1, Location location, boolean tranImmediately) {
        super( OP.param, arg1, null, null,location,tranImmediately);
    }

    public void addInstr(Instruction instr){
        storeInstructions.addLast(instr);
    }

    public void removeInstr(Instruction instr){
        storeInstructions.remove(instr);
    }

    public LinkedList<Instruction> getStoreInstructions() {
        return storeInstructions;
    }

    public LinkedList<Instruction> getReverseOrderInstructions(){
        var lst=new LinkedList(storeInstructions);
        lst.sort(comparator.reversed());
        return lst;
    }

    public void setPassingAsArg(boolean b){
        setPassingAsArg( this.arg1,b);
        for(Instruction instr:storeInstructions){
            Value arg1=instr.getArg1(),
                    arg2=instr.getArg2();
            Result result=instr.getResult();
            setPassingAsArg(arg1,b);
            setPassingAsArg(arg2,b);
            if(result!=null&&result instanceof Value)
                setPassingAsArg(result,b);
        }
    }

    private void setPassingAsArg(Value v,boolean b){
        if(v!=null&& v instanceof Var){
            ((Var) v).setPassingValueAsArg(b);
        }
    }

    public void clearStoring(){
        storeInstructions.clear();
    }
}
