package IR.Optimise;

import ASM.Register.Register;
import IR.Block;
import IR.Var;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 */
public class Loop {
    private static int lid = 0;
    public final int id;
    private final Block head;
    private Set<Block> components;
    /*在整个循环范围内活跃的变量*/
    private Set<Var> activeVars;
    //循环范围内易变的变量
    private Set<Var> volatileVars;

    private Map<Var, Register> allocationTable;

    private List<Loop> innerLoops;

    Loop(Set<Block> components, Block head) {
        this.components = components;
        this.head = head;
        id = lid++;
        setAffiliatedLoop();
    }

    private void setAffiliatedLoop(){
        for(Block block:components){
            block.setAffiliatedLoop(this);
        }
    }

    boolean contains(Block block) {
        return components.contains(block);
    }

    public void addComponents(Block block){
        components.add(block);
        block.setAffiliatedLoop(this);
    }

    public List<Loop> getOuterLoops(List<Loop> loops){
        return loops.stream()
                .filter(loop -> loop.getInnerLoops().contains(this))
                .toList();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Loop loop = (Loop) o;
        return Objects.equals(components, loop.components);
    }

    @Override
    public int hashCode() {
        return Objects.hash(components);
    }

    @Override
    public String toString() {
        return "@%d%s".formatted(id, components.toString());
    }

    public Set<Block> getComponents() {
        return components;
    }

    public Block getHead() {
        return head;
    }

    public Set<Var> getActiveVars() {
        return activeVars;
    }

    public Set<Var> getVolatileVars() {
        return volatileVars;
    }

    public Map<Var, Register> getAllocationTable() {
        return allocationTable;
    }

    public List<Loop> getInnerLoops() {
        return innerLoops;
    }

    public void setInnerLoops(List<Loop> innerLoops) {
        this.innerLoops = innerLoops;
    }

    public void setAllocationTable(Map<Var, Register> allocationTable) {
        this.allocationTable = allocationTable;
    }

    public void setActiveVars(Set<Var> activeVars) {
        this.activeVars = activeVars;
    }

    public void setLoadingInfoOfVars(Set<Var> activeVars, List<Loop> innerLoops){
        /*现在暂时先过滤一下*/
        if(activeVars!=null){
            activeVars=activeVars.stream()
//                    .filter(var -> !var.isTemp())
                    .filter(var -> !var.isIndex())
                    .collect(Collectors.toSet());
        }
        /*需要保留在寄存器中的变量*/
        setActiveVars(activeVars);

        List<Var> varList=activeVars.stream().toList();
        List<Var> writtenVars = components.stream()
                .flatMap(block -> block.getNewAssignedAndActiveVars().stream())
                .distinct().toList();
        volatileVars= varList.stream()
                .filter(v -> writtenVars.contains(v))
                .collect(Collectors.toSet());
    }

    /*设置分配表,包括循环入口和循环出口所需的分配表*/
    public   void setAllocationTables(){
        if(innerLoops==null)
            innerLoops=new ArrayList<>();

        /*需要在循环入口处提前加载的变量*/
        for (Block block1 : getEnters()) {
            block1.setNeedLoadVarsTable(allocationTable);
        }

        /*需要在循环出口处保存的变量*/
        Map<Var,Register> map= allocationTable.entrySet().stream()
                .filter(e -> isVolatile(e.getKey()) )
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b));
        getExits().forEach(block ->{
            var m = map.entrySet().stream()
                    //只保存基本块中不使用或不定值的变量
                    .filter(e -> !block.getActiveVars().contains(e.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b));
                  block.setNeedStoreVarsTable(m);
        });

//        final List<Block> innerBlcoks = innerLoops.stream()
//                .flatMap(loop -> loop.getComponents().stream())
//                .distinct()
//                .toList();
        getComponents().stream()
//                .filter(block ->!innerBlcoks.contains(block))
                .forEach(block -> block.setAllocationTable(allocationTable)
                );
    }

    public List<Block> getPureComponents(){
        return components.stream()
                .filter(block -> ! innerLoops .stream()
                        .anyMatch(l->l.components.contains(block)))
                .toList();
    }

    /**
     * @return 循环的出口基本块
     */
    public Set<Block> getExits() {
        return components.stream()
                .flatMap(block -> block.getNextBlocks().stream())
                .filter(block -> !contains(block))
                .collect(Collectors.toSet());
    }

    /**
     * @return 进入循环的基本块
     */
    public Set<Block> getEnters() {
        return head.getPreBlocks()
                .stream().filter(block -> !contains(block))
                .collect(Collectors.toSet());
    }

    /**
     *合并循环
     * @retrun 被合并的循环
     */
    static Loop combine(Loop l1,Loop l2){
        if(l1.size()>l2.size()){
            l1.components.addAll(l2.components);
            return l2;
        }else{
            l2.components.addAll(l1.components);
            return l1;
        }
    }

    public boolean isVolatile(Var var){
        return volatileVars.contains(var);
    }

    public int size(){
        return components.size();
    }
}
