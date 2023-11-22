package Semantic_Analysis.SymbolTable;

import ASM.AsmAddress.DirectAddress;
import AST.DEFINE.*;
import CompileException.CompileError;
import IR.Literal;

import java.util.*;

/**
 * 全局符号表,保存全局变量,函数,常量和静态变量定义
 */
public class GlobalTable extends SymbolTable{
    private static int strId=0;
    protected Map<String, Entity> entities=new LinkedHashMap<>();
    //外部声明实体
    protected Map<String,Entity> declaredEntities=new LinkedHashMap<>();
    //静态变量
    protected Map<String , DefinedVariable> staticVariables=new LinkedHashMap<>();
    //const
    private Map<String, DefinedConst> constants =new LinkedHashMap<>();
    //字符串字面量
    private Map<String,DirectAddress> strings=new LinkedHashMap<>();


    public GlobalTable(){

    }

    public void defineEntity(Entity entity) throws CompileError {
        Entity e=entities.get(entity.getName());
        if(e!=null){
            throw new CompileError(entity.getName()+ "重复定义或声明!",entity.getLocation());
        }else {
            entities.put(entity.getName(),entity);
            if(!entity.isDefined()){
                declaredEntities.put(entity.getName(),entity);
            }
        }

        if(entity instanceof DefinedVariable defVar){
            defVar.setTableId(defVar.isDefined()? SymbolTable.globalTableId
                                : SymbolTable.externTableId);
            defVar.setGlobal(true);
        }
        if(entity instanceof DefinedConst definedConst){
            constants.put(entity.getName(),definedConst);
        }
    }

    public List<Entity> getLocalEntities() {
        return entities.values()
                .stream().filter(e->!declaredEntities.containsValue(e))
                .toList();
    }

    @Override
    public Entity get(String name) throws CompileError {
        Entity env=entities.get(name);
        if(env==null){
            env=staticVariables.get(name);
            if(env==null)
                throw new CompileError(name+"未定义");
        }
        return env;
    }

    public DefinedFunction getFunction(String name){
        try {
            Entity entity=get(name);
            return (DefinedFunction) entity;
        } catch (CompileError e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 查询变量是否具有静态生存期
     */
    public boolean isGlobalOrStatic(DefinedVariable var){
        return entities.containsValue(var)||staticVariables.containsValue(var);
    }

    public void definedStaticVar(DefinedVariable var) throws CompileError {
        DefinedVariable v=staticVariables.get(var);
        if(v!=null){
            throw new CompileError(var.getName()+ "重复定义!",var.getLocation());
        }else {
            entities.put(var.getName(),var);
        }
    }

    public void addStr(String str){
        DirectAddress direct=new DirectAddress("__str"+(strId++));
        strings.put(str,direct);
    }

    public List<DefinedVariable> getGlobalVars(){
        List<DefinedVariable> result=new LinkedList<>();
        for(Map.Entry<String,Entity> entry:entities.entrySet()){
            Entity entity=entry.getValue();
            if(entity instanceof DefinedVariable defVar){
                result.add(defVar);
            }
        }
        return result;
    }

    public List<Entity> getDeclaredEntities() {
        return declaredEntities.values().stream().toList();
    }

    public List<DefinedConst> getConstants() {
         return constants.values().stream().toList();
    }

    /**
     * 返回所有的全局变量
     * @param constIncluded 是否包含const
     */
    public List<DefinedVariable> getGlobalAndStaticVars(boolean constIncluded){
        List<DefinedVariable> vars=new LinkedList<>(getGlobalVars());
        vars.addAll(getStaticVars());
        if(constIncluded)
            return vars;
        return vars.stream().filter(definedVariable -> !definedVariable.isConst()).toList();
    }

    public Map<String, DefinedVariable> getStaticVariables() {
        return staticVariables;
    }

    public List<DefinedVariable> getStaticVars(){
        List<DefinedVariable>vars=new LinkedList<>();
        for(Map.Entry<String, DefinedVariable> entry:staticVariables.entrySet()){
            vars.add(entry.getValue());
        }
        return vars;
    }

    public Map<String, DirectAddress> getStrings() {
        return strings;
    }

    public DirectAddress getStringLiteral(String s){
        return strings.get(s);
    }

    public DirectAddress getStringLiteral(Literal l){
        return strings.get(l.getLxrValue());
    }

    /**
     *判断第n(从1开始)个参数时候是可变长参数
     */
    public boolean isKwarg(String functionName,int n){
        DefinedFunction func=(DefinedFunction) entities.get(functionName);
        ParamNode paramNode=func.getNthParam(n);
        return paramNode.isKwargs();
    }



}
