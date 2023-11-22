package IR;

import IR.Constants.Type;
import IR.Optimise.DagNode;

public interface Value {
    Type getIRType();

    default boolean isSigned(){
        return true;
    }

    default void setIRType(Type type){
        ;
    }

    /**
     * 获得最近定值的语句对应的DAG结点
     */
    default DagNode getAssignedDag(){
        return null;
    }

    default void setAssignedDag(DagNode node){}

    default DagNode getRecentDag(){
        return null;
    }

    default void setRecentDag(DagNode dag){}
}
