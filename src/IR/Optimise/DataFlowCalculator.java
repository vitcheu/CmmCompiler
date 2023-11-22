package IR.Optimise;

import IR.Block;

import java.io.PrintWriter;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 *数据流分析框架
 */
public class DataFlowCalculator {
    /**
     * 数组流更新通用框架
     * @return 本次计算是否更新了数据流值
     */
    public static boolean updateDataFlowValue(Function<Block,Integer> dataValueGetter,
                                               Function<Block,Void> updater, Block block){
        int oriValue=dataValueGetter.apply(block);
        updater.apply(block);
        int curValue=dataValueGetter.apply(block);
        return oriValue!=curValue;
    }

    /**
     *数据流值(集合形式)的交汇运算
     */
    public static <E> void interSection(Set<E> set1, Set<E>set2) {
        if (set2.isEmpty())
            return ;
        if (set1.isEmpty()){
            set1.addAll(set2);
        }else{
            set1.removeIf(e -> !set2.contains(e));
        }
    }

    /**
     * 前向数据流问题通用框架
     */
    public static void forwardDataFlowCalculate(Function<Block,Void> preLoopFunction, BiFunction<Block,Block,Void> loopBody,
                                                Function<Block,Void> afterLoopFunction, Block block){
        if(preLoopFunction!=null) {
            preLoopFunction.apply(block);
        }
        for (Block preBlock :block.getPreBlocks()) {
            loopBody.apply(preBlock,block);
        }
        if(afterLoopFunction!=null){
            afterLoopFunction.apply(block);
        }
    }

    /**
     * 数据流分析框架
     * @param updateFunction 每个block的处理函数,若到达不动点,返回false
     */
    public static void visitBlocks(List<Block> blocks , Function<Block,Boolean> updateFunction, PrintWriter logger){
        boolean changed=true;
        int turn=0;
        while (changed){
            logger.println("\nTurn "+turn);
            changed=false;
            for(Block block:blocks){
                logger.println("\n"+block);
                if(updateFunction.apply(block)){
                    changed=true;
                }
            }
            turn++;
        }
    }
}
