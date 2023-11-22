package utils;

import AST.DEFINE.definedComposedType;
import AST.DEFINE.definedUserType;
import AST.TYPE.ComposedType;
import AST.TYPE.TypeNode;
import IR.Constants.Type;
import IR.Constants.TypeWidth;
import IR.Value;
import IR.Var;

import java.util.HashMap;

/**
 *
 */
public class CalculateTypeWidth {
    public static  HashMap<String, definedUserType> typeTable;

    /**
     * 计算类型宽度(字节)
     */
    public static int getTypeWidth(TypeNode typeNode) {
        if(typeNode==null){
            throw new RuntimeException();
        }
        if (typeNode.isArray()) {
            TypeNode baseType = typeNode.getArrayBaseType();
            return typeNode.getTotalLen() * getTypeWidth(baseType);
        }
        if (typeNode.isPointerType()) {
            return TypeWidth.ptrWidth;
        }
        if(typeNode.isComposedType()){
            if(typeTable==null){
                throw  new RuntimeException("未填充类型表");
            }
            ComposedType composedType=(ComposedType) typeNode.getBase();
            definedComposedType def=(definedComposedType) typeTable.get(composedType.getId());
            if(def==null){
                throw new RuntimeException("未定义类型:"+composedType.getId());
            }
            return def.getTypeWidth();
        }

        //void类型
        if(typeNode.isVoid())
            return 0;

        //其他基本类型
        return Type.TypeNodeToIRType(typeNode).getWidth();
    }

    public static int getValueWidth(Value v){
        Type type;
        if(v instanceof Var){
            Var var=(Var) v;
             type=var.getIRType();
        }else{
            type=v.getIRType();
        }
        return type.getWidth();
    }
}
