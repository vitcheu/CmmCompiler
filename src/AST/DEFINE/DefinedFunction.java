package AST.DEFINE;

import ASM.AsmAddress.Address;
import AST.Callee;
import AST.NodeList.ListNode;
import AST.STMT.BlockNode;
import AST.TYPE.ParamPofix;
import AST.TYPE.PostfixNode;
import AST.TYPE.TypeNode;
import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;
import CompileException.*;

import java.util.LinkedList;
import java.util.List;

public class DefinedFunction extends Entity implements Address, Callee {
    private ListNode<ParamNode> params;
    private BlockNode body;

    public DefinedFunction(Location location, boolean priv, TypeNode ret, String name, ListNode<ParamNode> params, BlockNode body) {
        super(location,priv,ret,name);
        this.params = params;
        this.body = body;
        entityType=Entity.FUNCTION;
    }

    public DefinedFunction(Location location, boolean priv, TypeNode type, String name, ListNode<ParamNode> params) {
        super(location, priv, type, name);
        this.params = params;
    }

    @Override
    public void accept(ASTVisitor ASTVisitor)throws CompileError {
        ASTVisitor.visit(this);
    }

    @Override
    public String toString() {
//        return "DefFun@" +name+"#"+type+"#"+params+location;
        return "@"+name;
    }

    @Override
    public void dump(int level) {
        dumpHead("Function",level);
            if(!isDefined()){
                ident(level+1,"extern: true");
            }
            ident(level+1,"static:"+priv);
            ident(level+1,"return:"+type);
            ident(level+1,"Name:"+name);
            ident(level+1,"Parms:");
                if(params==null||params.getNodeList().isEmpty()){
                    ident(level+2,"void");
                }else{
                    params.dump(level+2);}
            if(body!=null){
            ident(level+1,"Body:");
                body.dump(level+2);}
        dumpEnd("Function",level);
    }

    public List<ParamNode> getParams() {
        if(params==null||isVoidParam(params.getNodeList())) return new LinkedList<>();
        return params.getNodeList();
    }

    @Override
    public boolean isFunction() {
        return true;
    }

    public BlockNode getBody() {
        return body;
    }

    @Override
    public boolean isFloat() {
        return false;
    }

    public String getNthParamName(int n){
        ParamNode paramNode=getNthParam(n);
        String ret= paramNode.getName();
        return ret==null?"":ret;
    }

    public boolean hasNoParam(){
       return getParams().isEmpty();
    }

    @Override
    public int getWidth() {
        return 0;
    }

    public TypeNode toFunctionType(){
        ParamPofix postfix=new ParamPofix(getLocation(),params.getNodeList());
        return TypeNode.createFunctionType(type,postfix);
    }
}
