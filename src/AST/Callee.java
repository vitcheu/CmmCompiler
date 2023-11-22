package AST;

import AST.DEFINE.ParamNode;
import AST.TYPE.TypeNode;
import IR.Value;

import java.util.List;

public interface Callee  {
    default boolean containParamAfterKwargs(){
        List<ParamNode>paramNodes=getParams();
        return !paramNodes.isEmpty() &&
                (isKwarg() && (paramNodes.indexOf(ParamNode.KwargsNode) != paramNodes.size() - 1));
    };

    /**
     * 获取第n个参数(从1开始),当函数包含可变长参数且i大于定长参数的数量时,
     * 返回KwargsNode
     */
    default ParamNode getNthParam(int n){
        List<ParamNode> paramNodes=getParams();
        if(n<=paramNodes.size()){
            return paramNodes.get(n-1);
        }else{
            ParamNode last= null;
            try {
                last = paramNodes.get(paramNodes.size()-1);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if(last.isKwargs()){
                return last;
            }else{
                throw null;
            }
        }
    };

    default boolean isKwarg(int n){
        return getNthParam(n).isKwargs();
    }

    default boolean isKwarg(){
        return getParams().contains(ParamNode.KwargsNode);
    }


    List<ParamNode> getParams();

    default boolean isVoidParam(List<ParamNode> paramNodes){
        return  paramNodes.size()==1&&
               paramNodes.get(0).getType().isVoid();
    }

    TypeNode getType();
}
