package Semantic_Analysis.SymbolTable;

import AST.DEFINE.DefinedVariable;
import AST.DEFINE.Entity;
import AST.EXPR.VariableNode;
import CompileException.CompileError;
import CompileException.errorHandler;
import IR.Constants.Type;
import IR.Value;
import IR.Var;
import utils.Align;

import java.util.*;

/**
 * 局部符号表
 */
public class LocalTable extends SymbolTable implements Value {
    protected static int tableId=0;
    protected final int id;
    protected SymbolTable parent;
    private boolean processTable=false;
    private int paramNum=0;
    private String name;
    //嵌套的级数
    private int level=-1;
    //总共需要分配的空间
    private int allocSize=-1;
    private List<List<List<Var>>> locals=null;

    protected Map<String, DefinedVariable> variables=new LinkedHashMap<>();
    protected Map<String, DefinedVariable> staticVars=new LinkedHashMap<>();

    public LocalTable(SymbolTable parent,int n,String name){
        this.parent=parent;
        parent.addChild(this);
        this.name=name;
        paramNum=n;
        id=tableId++;
    }

    public SymbolTable getParent() {
        return parent;
    }

    public Entity get(String name) throws CompileError {
        DefinedVariable variable=variables.get(name);
        if(variable!=null){
            return variable;
        }else{
            return parent.get(name);
        }
    }



    public boolean isDefinedLocally(String name){
        return variables.get(name)!=null||staticVars.get(name)!=null;
    }

    public void defineVariable(DefinedVariable v){
        if (v.isPriv()) {
            staticVars.put(v.getName(),v);
        } else {
            variables.put(v.getName(),v);
        }
        v.setTableId(getId());
    }

    public void addVariable(DefinedVariable var, GlobalTable globalTable, errorHandler handler){
        try {
            if (isDefinedLocally(var.getName())) {
                throw new CompileError("作用域中已定义变量" + var.getName(), var.getLocation());
            } else {
                defineVariable(var);
                if(var.isPriv()){
                    globalTable.definedStaticVar(var);
                }
            }
        } catch (CompileError e) {
            handler.handle(e);
        }
    }

    public Map<String, DefinedVariable> getVariables() {
        return variables;
    }

    public Map<String, DefinedVariable> getStaticVars(){
        return staticVars;
    }

    public void setOffset(){
        curOffset=0;
        int oriOffset=curOffset,paramOffset=16;
        int i=0,pIdx=0;
        List<DefinedVariable> vars=new ArrayList<>(variables.values());
        vars.sort((o1,o2)-> Integer.compare(o2.getWidth(),o1.getWidth()));
        for(DefinedVariable var:vars){
            int width= var.getWidth();
            if(i<paramNum){
                pIdx++;
                if(pIdx>4){
                    var.setOffset(paramOffset);
                    paramOffset+=width;
                }
            }else{
                curOffset+=width;
                int align_size=Align.get_natural_align_size(var);
                //对齐到自然边界
                curOffset= Align.align(curOffset,align_size);//8为返回地址占用的8B栈空间
                var.setOffset(curOffset);
            }
            i++;
        }
        setSize(curOffset-oriOffset);
    }

    @Override
    public String toString() {
//        StringBuilder sb=new StringBuilder();
//        sb.append("isProcessTable?:"+processTable+"\tsize="+size);
//        sb.append("变量\t"+"类型\t"+"偏移量"+"\n");
//        for(String s:variables.keySet()){
//            DefinedVariable defv=variables.get(s);
//            sb.append(defv.getName()+"\t"+defv.getType()+"\t"
//            +defv.getOffset()+"\n");
//        }
        return getName();
    }

    public List<Var> getLocalVars(){
        List<Var> result=new LinkedList<>();
        int i=0;
        for(String s:variables.keySet()){
            if(i<paramNum) {
                i++;
                continue;
            }
            DefinedVariable defv=variables.get(s);
            result.add(defv);
            i++;
        }
        result.sort((o1,o2)-> Integer.compare(o2.getWidth(),o1.getWidth()));
        return result;
    }

    public boolean hasNoLocalOrParamVar(){
        return getAllLocals().isEmpty()&& getParams().isEmpty();
    }


    /**
     * 判断是否为过程的变量
     */
    public boolean isLocalInProcess(DefinedVariable variable){
        return getAllLocals().contains(variable)||getParamVars().contains(variable);
    }

    /**
     * 获取本层及所有后代符号表中的局部变量
     */
    public List<Var> getAllLocals(){
        List<Var> result = new LinkedList<>();

        result.addAll(getLocalVars());

        List<LocalTable> tables = getChildren();
        for (LocalTable table : tables) {
            //递归调用子节点的方法
            result.addAll(table.getAllLocals());
        }

        return result;
    }

    public List<DefinedVariable> getParamVars(){
        List<DefinedVariable> result=new LinkedList<>();
        int i=0;
        for(String s:variables.keySet()){
            if(i>=paramNum) break;
            DefinedVariable defv=variables.get(s);
            result.add(defv);
            i++;
        }
        return result;
    }

    public List<Var> getParams(){
        return getParamVars().stream().map(param->(Var)param).toList();
    }

    public boolean isEmpty(){
        return getAllLocals().isEmpty();
    }


    public boolean isProcessTable() {
        return processTable;
    }

    public void setProcessTable(boolean processTable) {
        this.processTable = processTable;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Type getIRType() {
        return null;
    }


    public int getId() {
        return id;
    }

    public int getLevel() {
        return level;
    }

    public int getAllocSize() {
        return allocSize;
    }

    public void setAllocSize(int allocSize) {
        this.allocSize = allocSize;
    }

}
