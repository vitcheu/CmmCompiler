package IR.instruction;

import AST.Callee;
import AST.DEFINE.DefinedFunction;
import AST.TYPE.TypeNode;
import CompileException.CompileError;
import IR.Literal;
import Parser.Entity.Location;
import IR.Constants.OP;
import IR.Result;
import IR.Value;
import IR.Var;
import Semantic_Analysis.SymbolTable.GlobalTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class CallInstruction extends Instruction {
    Callee callee;
    private List<Value> args;
    private HashMap<Var,Boolean> afterCallActiveInfo=new HashMap<>();
    public CallInstruction(Value arg1, Value arg2, Result result ,List<Value> args,Callee callee, Location location, boolean tranImmediately) {
        super( OP.call,arg1, arg2, result,location,tranImmediately);
        if(args!=null){
            this.args=new LinkedList<>(args);
        }
        this.callee=callee;
    }


    public List<Value> getArgs() {
        return args;
    }

    public void setArgs(List<Value> args) {
        this.args = args;
    }

    public HashMap<Var, Boolean> getAfterCallActiveInfo() {
        return afterCallActiveInfo;
    }

    public void restoreActiveINfo(){
        for(Value value:args){
            restoreActiveINfo(value);
        }
    }

    public void restoreActiveINfo(Value value){
        if(value instanceof Var){
            Var var=(Var) value;
            boolean active=afterCallActiveInfo.get(var);
            var.setActive(active);
        }
    }

    public String getFuncName(){
        DefinedFunction DefinedFunction =(DefinedFunction) arg1;
        return DefinedFunction.getName();
    }

    public Callee getCallee(){
        return callee;
    }

    public void collectArgs( LinkedList<ParamInstruction> paramInstructions){
        List<Value> args=new ArrayList<>();
        int num=Integer.parseInt(arg2.toString());
        for(int j=0;j<num;j++){
            ParamInstruction pi=paramInstructions.removeLast();
            Value arg=pi.getArg1();
            args.add(arg);
        }
        setArgs(args);
    }

    public boolean isDirectCall(){
        return arg1  instanceof DefinedFunction;
    }
}
