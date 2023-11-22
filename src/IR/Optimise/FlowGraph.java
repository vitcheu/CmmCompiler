package IR.Optimise;

import IR.Block;

import java.io.PrintWriter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 以基本块为结点的数据流图
 */
public class FlowGraph {
   private static final  int VISITING=0;
   private static final  int VISITED=1;

   static PrintWriter logger;
   private static GraphManager finder;
   private static int fid=0;
   private final int id;
   private LinkedHashSet<Block> blocks;
   private Set<Edge> edges;
   /*从后代指向祖先的回退边*/
   private Set<Edge> retreatingEdges;
   /*互相不是对方后代的交叉边*/
   private Set<Edge> crossEdges;

   /*记录各block的深度优先访问顺序*/
   HashMap<Block,Integer> Dfn=new HashMap<>();

   private Set<Loop> loops;

   public FlowGraph(LinkedHashSet<Block> blocks){
      this.blocks=blocks;
      edges=new LinkedHashSet<>();
      retreatingEdges=new HashSet<>();
      crossEdges=new HashSet<>();
      this.id=fid++;
   }

   public static void setFinder(GraphManager finder) {
      FlowGraph.finder = finder;
   }

   public int getId() {
      return id;
   }

   @Override
   public String toString() {
      return "FG"+id;
   }

   public boolean containNode(Block block){
      return blocks.contains(block);
   }

   public List<Block> getBlocks() {
      return blocks.stream().sorted((b1, b2) -> -Integer.compare(Dfn.get(b1),Dfn.get(b2))).toList();
   }

   void addNode(Block block){
      blocks.add(block);
   }

   /**
    * @param type 边的类型
    */
   private void addEdge(Block from,Block to,EdgeType type){
      Set<Edge> edgeSet=edges;
      if(type==EdgeType.RETREATING){
         edgeSet=retreatingEdges;
      }
      else if(type==EdgeType.CROSS) {
         edgeSet=crossEdges;
      }
      Edge edge=new Edge(from,to,type);
      logger.println("加入边: "+edge);
      edgeSet.add(edge);
   }

   void addEdge(Block from,Block to){
       addEdge(from,to,EdgeType.FORWARD);
   }

   private void addRetreatingEdge(Block from,Block to){
      addEdge(from,to,EdgeType.RETREATING);
   }

   private void addCrossEdge(Block from,Block to){
      addEdge(from,to,EdgeType.CROSS);
   }

   void setDfn(){
      int n=blocks.size();
      int i=0;
      for (Block next : blocks) {
         Dfn.put(next, n - i);
         i++;
      }
      logger.println("Dfn: "+Dfn+"\n");
   }

   public void buildGraph(Block block){
      HashMap<Block,Integer> visageMap=new HashMap<>();
      forwardSearch(block,visageMap);
      logger.println("正向边:\t"+edges+"\n");
      logger.println("回退边:\t"+retreatingEdges+"\n");
      logger.println("交叉边:\t"+crossEdges+"\n");
   }

   private <E>  void search(E node, HashMap<E,Integer> visageMap, Function<E, Void> visitor){
      visageMap.put(node,VISITING);
      visitor.apply(node);
      visageMap.put(node,VISITED);
   }

   private void forwardSearch(Block block, HashMap<Block,Integer> visageMap){
      search(block, visageMap, block1 -> {
         addNode(block);
         for (Block next : block1.getNextBlocks()) {
            Integer visage = visageMap.get(next);
            if (visage == null) {
               addEdge(block1, next);
               forwardSearch(next, visageMap);
            } else if (visage == VISITING) {
               addRetreatingEdge(block1, next);
            } else {
               addCrossEdge(block1, next);
            }
         }
         return null;
      });
   }

   private void backwardSearch(Block block,HashMap<Block,Integer> visageMap,Set<Block> loop){
      search(block, visageMap, block1 -> {
         block.printSep(logger,true);
         for (Block next : block1.getPreBlocks()) {
            Integer visage = visageMap.get(next);
            if(loop.add(next)){
               logger.println("循环加入"+next);
            }
            if (visage == null) {
               backwardSearch(next, visageMap,loop);
            }
         }
         block.printSep(logger,false);
         return null;
      });
   }

   public void findAllLoops(){
      Set<Loop> loops=new HashSet<>();
      for(Edge edge:retreatingEdges){
         logger.println("\n寻找回边"+edge+"所确定的循环......");
         /* n->d ,d为循环头,位于循环结点集合的第一个元素 */
         Block n=edge.from,d=edge.to;
         Set<Block> components=new LinkedHashSet<>();
         components.add(d);
         components.add(n);

         if (d!=n) {
            HashMap<Block,Integer> visageMap=new HashMap<>();
            visageMap.put(d,VISITED);

            backwardSearch(n,visageMap,components);
         }
         logger.println("\n回边"+edge+"所确定的循环为: "+components);

         Loop loop1=new Loop(components,d);
         loops.add(loop1);
      }
      this.loops=loops;
      combineLoops();

      logger.println("\n\n循环列表:");
      loops.forEach(l->logger.println(l));
      findAllInnerLoops();
      setOrderOfLoops();
      logger.println("\n\n排序后, 循环列表:");
      this.loops.forEach(l->logger.println(l));
   }

   /**
    * 简单起见,将拥有公共循环头的循环合并
    */
   private void combineLoops(){
      Set<Loop> visited=new HashSet<>();
      Set<Loop> combinedLoops=new HashSet<>();
      for(Loop loop:loops){
         loops.stream().filter(loop1 -> {
            return !visited.contains(loop1) && !loop1.equals(loop)
                    &&loop.getHead().equals(loop1.getHead());
         }).forEach(loop1 -> {
            combinedLoops.add(Loop.combine(loop,loop1));
            visited.add(loop1);
         });
         visited.add(loop);
      }
//      for(Loop loop:combinedLoops){
//         loops.remove(loop);
//      }
   }


   private boolean isInnerLoopOf(Loop l1,Loop l2){
      return !l1.equals(l2)&&l2.contains(l1.getHead())&&l2.size()>l1.size();
   }

   /**
    * @return 判断block是否为l1的内层循环的一部分
    */
   public boolean isPartOfInnerLoop(Block block,Loop l1){
      return findInnerLoops(l1).contains(block);
   }

   /**
    * 找到嵌套于循环loop的所有内层循环
    */
   public List<Loop> findInnerLoops(Loop loop){
      var result= loops.stream().filter(loop1 ->
             isInnerLoopOf(loop1,loop))
              .toList();
      logger.printf("嵌套于%d的循环: %s\n%n",
              loop.id, result.stream().map(loop1 -> loop1.id).toList());
      loop.setInnerLoops(result);
      return result;
   }

   private void setOrderOfLoops(){
      HashMap<Loop,Integer> visageMap=new LinkedHashMap<>();
      for (Loop loop : loops.stream().sorted(this::compare).toList()) {
         if (visageMap.get(loop) == null)
            setOrderOfLoop(loop, visageMap);
      }

      var keyset=visageMap.keySet();
      var loopLst=keyset.stream()
              .sorted(this::compare)
              .toList();
      this.loops=new LinkedHashSet<>(loopLst);
      logger.println("顺序:"+loops);
   }

   private void setOrderOfLoop(Loop loop, HashMap<Loop,Integer> visageMap){
      search(loop, visageMap,
              loop1 -> {
                 var innerLoops = findInnerLoops(loop1);
                 for (Loop loop2 : innerLoops) {
                    if(visageMap.get(loop2)==null)
                        setOrderOfLoop(loop2, visageMap);
                 }
                 return null;
              });
   }

   public void findAllInnerLoops(){
      loops.forEach(this::findInnerLoops);
   }

   public Set<Loop> getLoops() {
      return loops;
   }

   private  int compare(Loop l1, Loop l2) {
      if(isInnerLoopOf(l1,l2)){
         return -1;
      }else if(isInnerLoopOf(l2,l1)){
         return 1;
      }else{
         return -Integer.compare(Dfn.get(l1.getHead()), Dfn.get(l2.getHead()));
      }
   }
}

enum EdgeType{
   FORWARD,
   RETREATING,
   CROSS,
}


class Edge{
   EdgeType type;
   Block from;
   Block to;

   public Edge(Block from, Block to,EdgeType type) {
      this.from = from;
      this.to = to;
      this.type=type;
   }

   @Override
   public String toString() {
      return String.format("${%s->%s}",from,to);
   }
}
