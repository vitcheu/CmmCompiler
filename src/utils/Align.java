package utils;

import AST.DEFINE.definedComposedType;
import AST.DEFINE.DefinedVariable;
import AST.TYPE.ComposedType;
import AST.TYPE.TypeNode;

import static IR.Constants.TypeWidth.*;

/**
 *
 */
public class Align {
    /**
     * 计算按一定规格对齐的地址字节量
     */
    public static int align(int size,int align_size){
        if(align_size==X86_RSP_align_size){
           return   alignStack(size);
        }else if(align_size==1){
            return size;
        }
        else
            return (((size+align_size-1)/align_size))*align_size;
    }

    /**
     * 计算栈指针对齐所需的字节数
     * 假设栈按十六字节对齐,且考虑已经压入栈的8字节返回地址
     */
    public static int alignStack(int size){
        if(size==0)
            return 0;
        return ((size+X86_RSP_align_size/2-1)/X86_RSP_align_size)* X86_RSP_align_size +X86_RSP_align_size/2;
    }

    /**
     * 获取需要自然对齐的字节边界
     */
    public static int get_natural_align_size(DefinedVariable var){
        TypeNode type = var.getType();
        return get_natural_align_size(type);
    }

    private static int get_natural_align_size(TypeNode type){
        if(type.isArray()){
            /*数组类型对齐到其基本类型*/
            return get_natural_align_size(type.getArrayBaseType());
        }else if(type.isComposedType()){
            /*复合类型需要对齐到最大字段的宽度*/
            ComposedType ct=(ComposedType) (type.getBase());
            definedComposedType def=ct.getTypeEntry();
            return def.getMaxFieldSize();
        }else{
            return CalculateTypeWidth.getTypeWidth(type);
        }
    }
}
