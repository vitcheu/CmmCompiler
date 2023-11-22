package IR.Optimise;

import ASM.Register.Register;
import ASM.RegisterLoader;
import IR.Block;
import compile.Constants;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

import static IR.Optimise.DataFlowCalculator.*;

/**
 * 2023-11-15 16:26:57
 * author: Vitcheu
 * 找出基本块之间的循环区域
 */
class GraphManager {
    public static PrintWriter logger;

    static {
        try {
            logger=new PrintWriter(Constants.optimizePath+"Loops Finder.txt");
            FlowGraph.logger=logger;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    private HashMap<Block, Set<Block>> domainRelationship=new HashMap<>();
    private List <FlowGraph> flowGraphs=new ArrayList<>();

    public GraphManager(){
        FlowGraph.setFinder(this);
    };

    public void findLoops(List<Block> blocks){
        /*计算支配节点*/
        getAllDomains(blocks);

        blocks.stream().filter(Block::isStart)
                .forEach(this::findLoop);

        RegisterAssignor assignor=new RegisterAssignor(blocks);
        for (FlowGraph graph:flowGraphs) {
            assignor.processFlowGraph(graph);
        }

        logger.close();
    }

    private void findLoop(Block block){
        LinkedHashSet<Block> visitedBlocks=new LinkedHashSet<>();
        FlowGraph graph=new FlowGraph(visitedBlocks);
        graph.buildGraph(block);
        flowGraphs.add(graph);
        logger.println("访问顺序: "+visitedBlocks+"\n");
        /*设置Dfn*/
        graph.setDfn();
        graph.findAllLoops();
    }
    
    private void getAllDomains(List<Block> blocks){
        iniDomain(blocks);
        visitBlocks(blocks, this::updateDomains, logger);
        logger.println("\n\nDomains of blocks:");
        blocks.forEach(block -> {
            block.printSep(logger, true);
            logger.println(domainRelationship.get(block).stream()
                    .sorted(Comparator.comparing(Block::getId)
                    ).toList()
            );
            block.printSep(logger, false);
            logger.println("\n\n");
        });
    }

    private void iniDomain(List<Block> blocks){
        blocks.forEach(block->{
            Set<Block> domains=new HashSet<>();
            if(block.isStart()){
                domains.add(block);
            }
            domainRelationship.put(block,domains);
        });
    }

    private void getDomains(Block block){
        forwardDataFlowCalculate(
                block1 -> null,
                (block1, block2) -> {
                    Set<Block> set1 = domainRelationship.get(block1);
                    Set<Block> set2=domainRelationship.get(block2);
                    interSection(set2,set1);
                    set2.add(block2);
                    return null;
                },
                block1 -> null,
                block);
    }

    private boolean updateDomains(Block block) {
        return updateDataFlowValue(
                block1 -> domainRelationship.get(block1).size(),
                block1 -> {
                    getDomains(block1);
                    return null;
                }, block
        );
    }

    public List<Block> getBlocks(){
        return flowGraphs.stream()
                .flatMap(graph -> graph.getBlocks().stream())
                .distinct()
                .toList();
    }

    /**
     * @return b1 是否支配b2
     */
    boolean  isDomain(Block b1,Block b2){
        return domainRelationship.get(b2).contains(b1);
    }
}
