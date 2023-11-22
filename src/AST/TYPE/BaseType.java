package AST.TYPE;

import AST.Node;
import Parser.Entity.Location;
import Semantic_Analysis.ASTVisitor;

import java.util.LinkedHashMap;
import java.util.Objects;

public class BaseType extends Node {
    public final static String INT= "int";
    public final static String SHORT="short";
    public final static String LONG="long";
    public final static String LONG_LONG="long long";
    public final static String Int64="Int64";
    public final static String Int8="int8";
    public final static String FLOAT="float";
    public final static String DOUBLE="double";
    public final static String CHAR= "char";
    public final static String BOOL="bool";
    public final static String STRING= "string";
    public final static String VOID="void";
    public final static String STRUCT="struct";
    public final static String UNION="union";
    public final static BaseType intType=new BaseType(null,INT);
    public final static BaseType int8Type=new BaseType(null,Int8);
    public final static BaseType shortType=new BaseType(null,SHORT);
    public final static BaseType longType=new BaseType(null,LONG);
    public final static BaseType longLongType=new BaseType(null,LONG_LONG);
    public final static BaseType floatType=new BaseType(null,FLOAT);
    public final static BaseType doubleType=new BaseType(null,DOUBLE);
    public final static BaseType charType=new BaseType(null,CHAR);
    public final static BaseType  boolType=new BaseType(null,BOOL);
    public final static BaseType stringType=new BaseType(null,STRING);
    public final static BaseType voidType=new BaseType(null,VOID);
    public final static BaseType int64Type=new BaseType(null,Int64);
    public static LinkedHashMap<String,BaseType> typeMap;

    static {
        typeMap=new LinkedHashMap<>();
        typeMap.put(INT,intType);
        typeMap.put(SHORT,shortType);
        typeMap.put(LONG,longType);
        typeMap.put(LONG_LONG,longLongType);

        typeMap.put(FLOAT,floatType);
        typeMap.put(DOUBLE,doubleType);

        typeMap.put(VOID,voidType);
        typeMap.put(CHAR,charType);
        typeMap.put(BOOL,boolType);

        typeMap.put(Int64,int64Type);
        typeMap.put(Int8,int8Type);
    }

    private boolean signed=true;
    protected String type;

    public BaseType(Location location, String type) {
        super(location);
        this.type = type;
    }

    public BaseType(String s,boolean signed){
        super(null);
        this.type=s;
        this.signed=signed;
    }

    public BaseType(String s){
        super(null);
        this.type=s;
    }

    public String getId(){
        return "";
    }

    public BaseType(Location location){
        super(location);
    }

    @Override
    public void accept(ASTVisitor ASTVisitor) {
        ASTVisitor.visit(this);
    }


    @Override
    public String toString() {
        String s=type;
        if(this instanceof StructType ||this instanceof UnionType){
            s+=" "+getId();
        }
        return s;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseType baseType = (BaseType) o;
        if(this.type.equals(LONG)&&baseType.type.equals(LONG_LONG)
            ||this.type.equals(LONG_LONG)&&baseType.type.equals(LONG))
            return true;
        return Objects.equals(type, baseType.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }

    public String getType() {
        return type;
    }

    public boolean signed() {
        return signed;
    }

    public void setSigned(boolean signed) {
        this.signed = signed;
    }

    public boolean isFloatType(){
        return this.equals(floatType)||this.equals(doubleType);
    }

    public boolean isIntType(){
        return this.equals(intType)||this.equals(int8Type)||this.equals(shortType)||this.equals(longType)
                ||this.equals(longLongType)||this.equals(int64Type);
    }

    public String getDescription(){
        return toString();
    }
}
