package AST;

import AST.DEFINE.*;
import AST.NodeList.ListNode;
import AST.STMT.ImportNode;
import Parser.LibraryLoader;
import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;
import Semantic_Analysis.SymbolTable.GlobalTable;

import java.io.FileNotFoundException;
import java.util.*;

import CompileException.*;

public class AST extends Node{
    private List<ImportNode> imports=new LinkedList<>();

    private Set<DeclararedNode> declaredNodes =new LinkedHashSet<>();

    private GlobalTable globalTable;

    public AST(Location location, ListNode<ImportNode> impts, ListNode<DeclararedNode> decls,
                 errorHandler handler){
        super(location);
        imports.addAll(impts.getNodeList());
        declaredNodes.addAll(decls.getNodeList());
        resolveImportStmts(handler);
    }

    public List<Entity> getEntities() {
        List<Entity> entities=new LinkedList<>();
        for(DeclararedNode def: declaredNodes){
            if(def instanceof Entity){
                entities.add((Entity) def);
            }
        }
        return entities;
    }

    public List<DefinedVariable> getVariables(){
        List<DefinedVariable> variables=new LinkedList<>();
        for(DeclararedNode def: declaredNodes){
            if(def instanceof DefinedVariable){
                variables.add((DefinedVariable) def);
            }
        }
        return variables;
    }

    public List<DefinedFunction> getFunctions(){
        List<DefinedFunction> functions=new LinkedList<>();
        for(DeclararedNode def: declaredNodes){
            if(def instanceof DefinedFunction){
                functions.add((DefinedFunction) def);
            }
        }
        return functions;
    }

    public List<definedUserType> getTypes(){
        List<definedUserType> types=new LinkedList<>();
        for(DeclararedNode def: declaredNodes){
            if(def instanceof definedUserType){
                types.add((definedUserType) def);
            }
        }
        return types;
    }

    /**
     * 获取声明列表
     */
    public List<DeclararedNode> getDeclarations(){
          return declaredNodes.stream().filter(decl ->
                !(decl instanceof Entity) || !decl.isDefined() || (decl instanceof DefinedConst)).toList();
    }

    public List<DeclararedNode> getDefinedNode(){
        return declaredNodes.stream().filter(declararedNode -> declararedNode.isDefined()).toList();
    }


    @Override
    public void accept(ASTVisitor ASTVisitor) throws CompileError {
        ASTVisitor.visit(this);
    }

    @Override
    public void dump(int level) {
        ident(0,"******************************************");
        ident(level,"AST:");

        ident(level+1,"Imports:");
        for(ImportNode importNode:imports){
            importNode.dump(level+1);
        }

        ident(level+1,"Top Defines:");
        for(DeclararedNode def: declaredNodes){
            def.dump(level+1);
        }
        ident(0,"******************************************");
    }

    public void setGlobalTable(GlobalTable globalTable) {
        this.globalTable = globalTable;
    }

    public GlobalTable getGlobalTable() {
        return globalTable;
    }

    /**
     * 处理import语句
     */
    public void resolveImportStmts(errorHandler handler){
        for(ImportNode impt:imports){
            try{
                List<DeclararedNode> nodes=LibraryLoader.resolveImport(impt);
                declaredNodes.addAll(nodes);
            } catch (CompileException e) {
                handler.warning(e.getMessage(),e.getLocation());
                handler.warning(String.format("导入库%s失败",impt.getPath()),impt.getLocation());
            } catch (FileNotFoundException e) {
                handler.warning(e.getMessage(),impt.getLocation());
            }
        }
    }
}
