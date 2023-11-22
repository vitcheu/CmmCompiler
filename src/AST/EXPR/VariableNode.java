package AST.EXPR;

import AST.DEFINE.DefinedConst;
import AST.DEFINE.Entity;
import AST.TYPE.TypeNode;
import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;import CompileException.*;

public class VariableNode extends LHSNode {
    protected String id;
    protected Entity entry;
    public VariableNode(Location location,String id,Entity entry){
        super(location);
        this.id=id;
        this.entry=entry;
    }

    @Override
    public void accept(ASTVisitor ASTVisitor)throws CompileError {
        ASTVisitor.visit(this);
    }

    public VariableNode(Location location, String id) {
        super(location);
        this.id = id;
    }

    //用于创建成员变量
    public VariableNode(TypeNode type,String id){
        super(null);
        this.id=id;
        this.entry=null;
        setType(type);
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public void dump(int level) {
        dumpHead("Variable", level);
            ident(level + 1, "id=" + id);
            if (type != null) {
                type.dump(level + 1);
            }
        dumpEnd("Variable", level);
    }

    @Override
    public String toString() {
        return "Var $"+id+"{"+entry+"}";
    }

    public void setEntry(Entity entry) {
        this.entry = entry;
    }

    public Entity getEntry() {
        return entry;
    }

    /**
     * 如果该变量是一个函数,或者为数组或指针,则该方法返回true
     */
    @Override
    public boolean isPointer() {
       TypeNode type=entry.getTypeOfLeftHandSide();
       return type.isPointerOrArr();
    }

    @Override
    public boolean isComposedType() {
        if(entry==null) return false;
        return entry.getTypeOfLeftHandSide().isComposedType();
    }

    @Override
    public boolean isCallable() {
//        if(entry==null) throw new RuntimeException("无法找到变量"+this+"的引用");
        return entry.isFunction()||entry.getTypeOfLeftHandSide().isFunctionType();
    }

    @Override
    public boolean isConst() {
        if(entry==null) return false;
        return entry instanceof DefinedConst;
    }

    @Override
    public boolean isLiteralExpr() {
        return false;
    }

    @Override
    public boolean isConstExpr() {
        return   entry instanceof DefinedConst;
    }

    public boolean isFunctionVariable(){
        return entry.isFunction();
    }
}
