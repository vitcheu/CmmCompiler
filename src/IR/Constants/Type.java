package IR.Constants;

import AST.EXPR.LiteralNode;
import AST.TYPE.BaseType;
import AST.TYPE.TypeNode;
import CompileException.UnImplementedException;
import IR.Value;

import java.util.HashMap;
import java.util.Map;

import static AST.TYPE.BaseType.intType;
import static IR.Constants.TypeWidth.*;

public class Type implements Value {
    private String name;
    private int width=-1;
    private boolean signed=true;

    public Type(String name,int width){
        this.name=name;
        this.width=width;
    }

    public Type(String name,int width,boolean signed){
        this.name=name;
        this.width=width;
        this.signed=signed;
    }

    public static Type int8=new Type("int8",byteWidth);
    public static Type int16=new Type("int16",wordWidth);
    public static Type int32=new Type("int32",dwordWidth);
    public static Type int64=new Type("int64",qwordWidth);

    public static Type uInt8=new Type("uint8",byteWidth,false);
    public static Type uInt16=new Type("uint16",wordWidth,false);
    public static Type uInt32=new Type("uint32",dwordWidth,false);
    public static Type uInt64=new Type("uint64",qwordWidth,false);

    public static Type floatType=new Type("float",floatWidth);
    public static Type doubleType=new Type("double",doubleWidth);

    public static Type charType=new Type("char",byteWidth);
    public static Type pointer=new Type("pointer",ptrWidth,false);

    public static Type boolType=new Type("bool",byteWidth);

    public static Type StringLiteral=new Type("unknown",UNKNOWN);
    public static Type floatLiteral=new Type("floatLiteral",floatWidth);
    public static Type doubleLiteral=new Type("doubleLiteral",doubleWidth);

    public static Type byteType=new Type( "byte",byteWidth);
    public static Map<BaseType,Type> typeMap=new HashMap<>();
    static {
        typeMap.put(intType, int32);
        typeMap.put(BaseType.floatType,floatType);
        typeMap.put(BaseType.doubleType,doubleType);
        typeMap.put(BaseType.charType,charType);
        typeMap.put(BaseType.stringType,StringLiteral);
        typeMap.put(BaseType.boolType,boolType);
        typeMap.put(BaseType.int64Type, int64);
        typeMap.put(BaseType.int8Type,int8);
        typeMap.put(BaseType.shortType,int16);
        typeMap.put(BaseType.longType, int64);
        typeMap.put(BaseType.longLongType, int64);
    }

    public String getDeclareStr(){
        int width=getWidth();
       if(isFloatType()){
           if(width==8){
               return "real8";
           }else if(width==4){
               return "real4";
           }
           throw  new RuntimeException("不支持浮点的类型:"+this);
       }else{
          switch (width){
              case 1:return "byte";
              case 2:return "word";
              case 4:return "dword";
              case 8:return "qword";
              default:{
                  throw  new RuntimeException("不支持浮点的类型:"+this);
              }
          }
       }
    }

    public static String getDeclareStr(TypeNode typeNode){
        return TypeNodeToIRType(typeNode).getDeclareStr();
    }

    public int getWidth() {
        return width;
    }

    @Override
    public Type getIRType() {
        return Type.int32;
    }

    @Override
    public boolean isSigned() {
        return signed;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return  getName();
    }

    public static Type TypeNodeToIRType(TypeNode typeNode){
        if(typeNode.isVoid()){
            throw new RuntimeException("不支持void类型:");
        }
        if(typeNode.isPrimaryType()){
            BaseType base=typeNode.getBase();
            Type type= typeMap.get(base);
            if(!typeNode.isSigned()){
                type=getUnsignedType(type);
//                base.isSigned();
            }
            if(type==null){
                throw new RuntimeException("未支持的类型:"+typeNode);
            }
            return type;
        }
        else if(typeNode.isComposedType()){
            return pointer;
        }
        else {
            return pointer;
        }
    }

    /**
     * 获取对用的无符号整数类型
     */
    private static Type getUnsignedType(Type type){
        switch (type.getWidth()){
            case byteWidth:return uInt8;
            case wordWidth:return uInt16;
            case dwordWidth:return uInt32;
            case qwordWidth:return uInt64;
            default:{
                throw new RuntimeException();
            }
        }
    }

    public boolean isIntType(){
        return this.equals(int32)||this.equals(int64)||this.equals(int8)||this.equals(int64)
                ||this.equals(uInt8) ||this.equals(uInt16) ||this.equals(uInt32) ||this.equals(uInt64);
    }

    public boolean isFloatType(){
        return this.equals(floatLiteral)||this.equals(floatType)
                ||this.equals(doubleType)||this.equals(doubleLiteral);
    }

    public boolean isDoublePrecision(){
        return this.equals(doubleLiteral)||this.equals(doubleType);
    }

    public boolean isSinglePrecision(){
        return this.equals(floatLiteral)||this.equals(floatType);
    }

    /**
     * @return 宽度with的类型
     */
    private static Type getSpecificWidthType(int width,boolean isFloat){
        if(isFloat){
            return switch (width){
                case floatWidth-> floatType;
                default -> doubleType;
            };
        }else{
            return switch (width){
                case byteWidth -> int8;
                case wordWidth -> int16;
                case dwordWidth -> int32;
                default -> int64;
            };
        }
    }

    /**
     * 返回可直接转换(不必进行整数和浮点数之间的转换)的更宽的类型
     * @param isFloat 是否返回浮点类型
     */
    public static Type  getWiderType(Type t1,Type t2,boolean isFloat){
        if(t1.getWidth()>=t2.getWidth()){
            return getSpecificWidthType(t1.getWidth(),isFloat);
        }else{
            return getSpecificWidthType(t2.getWidth(), isFloat);
        }
    }

    public static Type getLiteralType(LiteralNode node){
        TypeNode  typeNode=node.getType();
        if(typeNode.isType(BaseType.FLOAT)){
            return floatLiteral;
        }else if(typeNode.isType(BaseType.DOUBLE)){
            return doubleLiteral;
        }else if(typeNode.isType(BaseType.INT)){
            return int32;
        }else if(typeNode.isType(BaseType.CHAR)){
            return charType;
        }else if(typeNode.isType(BaseType.STRING)){
            return StringLiteral;
        }else  if(typeNode.isType(BaseType.BOOL)){
            return boolType;
        }
        else {
            throw new UnImplementedException("不支持的字面量类型:"+typeNode);
        }
    }
}
