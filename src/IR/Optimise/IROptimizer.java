package IR.Optimise;

import IR.Block;
import IR.Literal;
import IR.instruction.Instruction;
import IR.instruction.ParamInstruction;
import compile.Constants;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static IR.Optimise.DataFlowCalculator.visitBlocks;

/**
 *
 */
public class IROptimizer {
    public static boolean verbose=true;
//    private static final Logger log = LoggerFactory.getLogger({SimpleClassName}.getClass());
    private static final PrintWriter logger;
    static {
        try {
            logger=new PrintWriter(Constants.optimizeLog);
            DAG.logger=logger;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private GraphManager graphManager;

    public IROptimizer(){};

    public void optimize(List<Block> blocks){
        verbose=false;
        /*获得活跃结点信息*/
        setGlobalActiveInfo(blocks);
        optimizeInBlock(blocks);

        try(PrintWriter blockWriter =new PrintWriter(Constants.optimizePath+"blocks.txt")) {
            verbose=true;
            blockWriter.println("\n######################原基本块############################");
            Block.printAllBlocks(blocks,blockWriter);
            collectCommonExprOfBlocks(blocks);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        /*重新设置活跃结点信息*/
        verbose=false;
        setGlobalActiveInfo(blocks);

        this.graphManager =new GraphManager();
        graphManager.findLoops(blocks);


        logger.close();
    }

    private void collectCommonExprOfBlocks(List<Block> blocks) {
        try (PrintWriter pw = new PrintWriter(Constants.optimizePath + "CommonExpr.txt")) {
            Block.verbose = true;
            collectGlobalExprs(blocks,pw);
            pw.println("\n\n\n######################全局表达式优化后############################");
            Block.printAllBlocks(blocks,pw);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void optimize(Block block){
        if(!block.isNormal())
            return;

        block.setActiveWhenExit();
        DAG dag=new DAG() ;
        /*保留指令旧id到其所属的param语句的映射*/
        HashMap<Integer,Integer> ori_idToPInstr=new HashMap<>();
        Optional<Instruction> LastInstr=block.getLastInstruction();
        boolean isLastRet=LastInstr.isPresent()&&LastInstr.get().isLastRet();

        for(Instruction instr:block.getInstructions()){
            if (instr instanceof ParamInstruction PInstr) {
                int pid=PInstr.getId();
                List<Instruction> instructionList = PInstr.getStoreInstructions();
                instructionList.stream()
                        .sorted()
                        .forEach(i -> ori_idToPInstr.put(i.getId(), pid));
                PInstr.clearStoring();
            }
            dag.buildNode(instr);
        }
        try {
            var Instrs= dag.rebuiltInstrs();
            block.setInstructions(Instrs);
        }catch (Exception e){
            logger.close();
            throw new RuntimeException(e);
        }

        List<Instruction> newInstrs=block.getInstructions();
        /*还原最后ret语句信息*/
        Optional<Instruction> newLastInstr=block.getLastInstruction();
        newLastInstr.ifPresent(last->last.setLastRet(isLastRet));
        /*设置标签*/
        if(!newInstrs.isEmpty()){
            block.setLabelToFirstInstr();
            newInstrs.forEach(i->{
                int oriId=i.getOri_id();
                var belong_pid=ori_idToPInstr.get(oriId);
                if(belong_pid!=null){
                    i.setTranslateImmediately(false);
                    ParamInstruction pInstr=(ParamInstruction) dag.getInstrOfOri_id(belong_pid);
                    pInstr.addInstr(i);
                }
            });
        }

        block.resetArgsOfCallInstrs();

        block.getUsedVars().forEach(var -> {
            var.setAssignedDag(null);
            var.setRecentDag(null);
        });
        Literal.clearAllDag();
        dag.clear();
    }

    private void optimizeInBlock(List<Block> blocks){
        for(Block block:blocks){
            logger.println("\n\n\n--------------------------<"+block+">------------------------------");
            logger.printf("%-6s\t%-20s\n","Entry",block.getActiveVars());
            logger.printf("%-6s\t%-20s\n","Exit",block.getActiveVarsOnExit());
            optimize(block);
            logger.println("\n--------------------------</"+block+">-----------------------------");
        }
    }

    private void setGlobalActiveInfo(List<Block> blocks){
        logger.println("\n\n\n-------------------------" +
                "全局活跃性分析-------------------------");
        visitBlocks(blocks, block -> {
            boolean updated= block.updateActiveInfo(logger);
            logger.println(String.format("%-6s\t%-20s","Use:",
                    block.getActiveVars().stream().sorted(Comparator.comparing(Object::toString)).toList()));
            return updated;
        },logger);
    }

    private void collectGlobalExprs(List<Block> blocks,PrintWriter pw){
        pw.println("\n\n\n-------------------------" +
                "全局公共子表达式提取-------------------------");
        visitBlocks(blocks,block -> {
            pw.println("更新前:\n"+block.getAvailableExprStr()+"\n");
            boolean updated=block.updateCommonExprs(pw);
            pw.println("更新后:\n"+block.getAvailableExprStr()+"\n\n");
            return updated;
        },pw);
    }

    public List<Block> getBlock(){
      return graphManager.getBlocks();
    }
}
