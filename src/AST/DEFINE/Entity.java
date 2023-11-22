package AST.DEFINE;

import AST.TYPE.TypeNode;
import Parser.Entity.Location;

import java.util.Objects;

import IR.Constants.Type;
import IR.Value;

//变量和函数的定义
public class Entity extends DeclararedNode implements Value {
    protected boolean priv = false;
    protected TypeNode type = null;
    protected String name;
    //实体类别,函数或变量
    protected int entityType;

    public final static int FUNCTION = 0;
    public final static int VARIABLE = 1;

    //记录引用次数
    protected int referenceCnt = 0;

    public Entity(Location location, boolean priv, TypeNode type, String name) {
        super(location);
        this.priv = priv;
        this.type = type;
        this.name = name;
    }

    public Entity(Location location, String name) {
        super(location);
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entity entity = (Entity) o;
        return entityType == entity.entityType && Objects.equals(name, entity.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, entityType);
    }

    public boolean isFunction() {
        return false;
    }

    public boolean isPriv() {
        return priv;
    }

    public TypeNode getTypeOfLeftHandSide() {
        return type;
    }

    public TypeNode getType(){
        return type;
    }

    public String getName() {
        return name;
    }

    public void setType(TypeNode type) {
        this.type = type;
    }

    public void setPriv(boolean priv) {
        this.priv = priv;
    }

    public int getEntityType() {
        return entityType;
    }

    public int getReferenceCnt() {
        return referenceCnt;
    }

    public boolean isReferenced() {
        return referenceCnt != 0;
    }

    public void addReferencedCnt() {
        referenceCnt++;
    }

    @Override
    public Type getIRType() {
        return null;
    }

    @Override
    public boolean isSigned() {
        return false;
    }

//    @Override
//    public Type getIRType() {
//
//    }

}
