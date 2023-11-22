package IR.Optimise;

import ASM.Register.Register;
import IR.Block;
import IR.Constants.Type;
import IR.Var;

import java.io.PrintWriter;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 为循环中频繁使用的变量分配寄存器
 */
public class RegisterAssignor {
    private static final int INNER_HEIGHT=10;
    private  List<Block> blocks;
    private static  int MAX_ALLOC_NUM=6;
    private static List<Register> integerRegs =Register.getRegs().stream()
            .filter(r->!r.isFloatRegister()&&r.isGeneral())
            .toList();
    private static List<Register> floatRegs=Register.getRegs().stream()
            .filter(Register::isFloatRegister)
            .toList();

    public static PrintWriter logger;
     static{
//         try {
//             logger=new PrintWriter(Constants.optimizePath+"Register Assignment.txt");
//         } catch (FileNotFoundException e) {
//             throw new RuntimeException(e);
//         }
         logger=FlowGraph.logger;
     }

     public RegisterAssignor(List<Block>  blocks){
         this.blocks=blocks;
     }

    /**
     *在相互不嵌套且相邻的循环间增加缓冲中间块,
     *使得加载/保存变量不必在循环体中进行
     */
    private void addIntermediateBlocks(FlowGraph graph){
        for(Loop loop:graph.getLoops()){
            var enters=loop.getEnters();
            for (Block block1:enters) {
                Loop affilliatedLoop=block1.getAffiliatedLoop();
                /*入口基本块也处于某一循环*/
                if(affilliatedLoop!=null
                        && !affilliatedLoop.getInnerLoops().contains(loop)){
                    //在入口基本块和本循环头之间增加一个缓冲基本块
                    Block interBlock=Block.createIntermediateBlock(block1,loop.getHead());
                    blocks.add(interBlock);
                    logger.println("#增加中间块:"+interBlock);
                    interBlock.printAllInstructions(logger);
                    interBlock.setActives(logger);

                    var  outers=loop.getOuterLoops(graph.getLoops().stream().toList());
                    outers.forEach(loop1 -> loop1.addComponents(interBlock));
                }
            }
//            logger.println("\n\n\n######################插入中间块后############################");
//            Block.printAllBlocks(blocks,logger);
        }
    }

    public void processFlowGraph(FlowGraph graph){
//        addIntermediateBlocks(graph);

         for(Loop loop:graph.getLoops()){
             logger.println("\n#Processing "+loop);
             Set<Var> usedVars=new HashSet<>();
             for(Block block:loop.getComponents()){
                 /*不直接处理内存循环的基本块*/
                 if(graph.isPartOfInnerLoop(block,loop)){
                     continue;
                 }

                 Set<Var> used=block.getActiveOnExitVars().stream()
                         .filter(var -> block.getUsedVars().contains(var))
                         .collect(Collectors.toSet());
                 usedVars.addAll(used);

                 block.printSep(logger,true);
                 logger.printf("Use:%s%n",used);
                 logger.println("Used Before Assigned:%s".formatted(block.getUsedBeforeAssignedVars()));
                 logger.printf("Active on Exit: %s%n",block.getActiveVarsOnExit());
                 logger.printf("Assigned & Active:%s%n", block.getNewAssignedAndActiveVars());
                 block.printSep(logger,false);
             }

             List<Loop> innerLoops=loop.getInnerLoops();
             Set<Var> innerUsedVars=new HashSet<>();
             for(Loop innerLoop:innerLoops){
                 Set<Var> used=innerLoop.getActiveVars();
                 /*求交集*/
                 DataFlowCalculator.interSection(innerUsedVars,used);
//                 usedVars.addAll(innerLoop.getActiveVars());
             }
             logger.println("#innerUsed: "+innerUsedVars);
             usedVars.addAll(innerUsedVars);
//             DataFlowCalculator.interSection(usedVars,innerUsedVars);

             loop.setLoadingInfoOfVars(usedVars,innerLoops);

             logger.printf("#Used:%s%n", loop.getActiveVars());
             logger.printf("#Enters :%s%n", loop.getEnters());
             logger.printf("#Exits: %s%n", loop.getExits());
             logger.printf("#Volatiles: %s%n", loop.getVolatileVars());
         }

        var loops = graph.getLoops().stream().toList();
        HashSet<Loop> visited=new HashSet<>();
        IntStream.iterate(loops.size() - 1, i -> i >= 0, i -> i - 1)
                .mapToObj(i -> loops.get(i)).
                forEach(loop -> {
                    if (!visited.contains(loop)) {
                        allocateRegsForLoop( loop, visited);
                        loop.setAllocationTables();
                        loop.getInnerLoops().forEach(loop1 -> visited.add(loop1));
                        visited.add(loop);
                    }
                });
    }

    private static void  allocateRegsForLoop(Loop loop,Set<Loop> visited){
         if(visited.contains(loop))
             return;

        logger.println("\n#为循环%s分配寄存器".formatted(loop));
        Set<Var> vars=new HashSet<>();
        List<Loop> innerLoops=loop.getInnerLoops();
        Map<Var, Register> allocatedMap=null;

        var scores=calculateBenefitOfVars(loop);
        /*根据分数筛选变量*/
        List<Var> selected = loop.getActiveVars().stream()
                .sorted(Comparator.comparing(var ->-scores.get(var)))
                .limit(MAX_ALLOC_NUM)
                .toList();
        logger.println("Selected:"+selected);

//        /*先分配内层循环间共有的变量*/
//        for(Loop innerLoop:innerLoops){
//            Set<Var> innerActiveVars=innerLoop.getActiveVars();
//            DataFlowCalculator.interSection(vars,innerActiveVars);
//        }
//        logger.println("#内层循环共有变量:"+vars);
//        allocatedMap=allocateRegsForLoop(loop,vars);

        /*分配本层循环*/
        vars.addAll(selected);
        allocatedMap= allocateRegsForLoop(loop,selected);

        /*分配内层循环*/
        for(Loop innerLoop:innerLoops){
            innerLoop.setAllocationTable(new HashMap<>( allocatedMap));
//            allocateRegsForLoop(graph,innerLoop,visited);
            visited.add(innerLoop);
        }

        logger.println("\n#循环%s分配寄存器结束".formatted(loop));
        visited.add(loop);
    }

    /**
     * 计算在循环中把变量保留在寄存器中的收益
     */
    private static HashMap<Var,Integer> calculateBenefitOfVars(Loop loop){
        var  vars=loop.getActiveVars();


        HashMap<Var,Integer> scores= vars.stream()
                .collect(Collectors.toMap(var -> var, var -> 0, (a, b) -> b, HashMap::new));
        logger.println("\n#计算循环%s的收益".formatted(loop));

        //计算内部循环的收益
        for(Loop innerLoop:loop.getInnerLoops()){
            var innerScores=calculateBenefitOfVars(innerLoop);
            for(Var innerVar:innerLoop.getActiveVars()){
                int innerScore=innerScores.get(innerVar)*INNER_HEIGHT;
                Integer ori=scores.get(innerVar);
                if(ori!=null){
                    ori+=innerScore;
                    scores.put(innerVar,ori);
                }else{
                    scores.put(innerVar,innerScore);
                }
            }
            logger.println("在内部循环中%s中的收益:\n%s".formatted(innerLoop,scores));
        }

        for(Var var:vars){
            int score=scores.get(var);
            logger.println("@计算%s的收益".formatted(var));

            for(Block block:loop.getPureComponents()){
                var used=block.getUsedBeforeAssignedVars();
                var assigned=block.getNewAssignedAndActiveVars();
                boolean hasCall=block.hasCallInstr();

                if(used.contains(var))
                    score++;
                if(assigned.contains(var)&&!hasCall)
                    score++;
                logger.println("$%-5s:\t[%-3d]".formatted(block,score));
            }

            scores.put(var,score);
//            logger.println("$%-5s的收益:\t%-3d\n".formatted(var,score));
            logger.println("\n");
        }
        return scores;
    }

    private static Map<Var,Register>  allocateRegsForLoop(Loop loop,Collection<Var> unAllocatedVars){
        Map<Var, Register> allocatedMap=loop.getAllocationTable();
        if(allocatedMap==null){
            allocatedMap=new HashMap<>();
        }

        Set<Register> allocatedRegs=allocatedMap.values().stream().collect(Collectors.toSet());
        Map<Var, Register> finalAllocatedMap = allocatedMap;
        unAllocatedVars= unAllocatedVars.stream()
                .filter(var -> finalAllocatedMap.get(var) == null)
                .sorted(Comparator.comparing(Object::toString))
                .toList();

        Iterator<Register> intRegItr=integerRegs.iterator();
        Iterator<Register> floatRegItr=floatRegs.iterator();
        Iterator<Var> varIterator=unAllocatedVars.iterator();

        boolean foundAReg=true;
        Var var=null;
        while (intRegItr.hasNext()&&floatRegItr.hasNext()&&
                    varIterator.hasNext()){
            if(foundAReg)
               var=varIterator.next();

            foundAReg=false;
            Register r=allocateRegForVar(var,intRegItr,floatRegItr);
            if(!allocatedRegs.contains(r)){
                allocatedMap.put(var,r);
                logger.println("分配 %-5s -> %-5s".formatted(r,var));
                foundAReg=true;
            }
        }

        loop.setAllocationTable(allocatedMap);
        return allocatedMap;
    }

    private static Register allocateRegForVar(Var var, Iterator<Register> intRegItr,
                                          Iterator<Register> floatRegItr){
        Type type=var.getIRType();
        Register r=type.isFloatType()?floatRegItr.next():intRegItr.next();
        return r;
    }
}
