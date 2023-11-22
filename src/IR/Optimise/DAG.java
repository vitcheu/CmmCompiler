package IR.Optimise;

import AST.Callee;
import IR.*;
import IR.Constants.OP;
import IR.Constants.Type;
import IR.instruction.CallInstruction;
import IR.instruction.ConditionJump;
import IR.instruction.Instruction;
import IR.instruction.ParamInstruction;
import Parser.Entity.Location;

import java.io.PrintWriter;
import java.util.*;

import static IR.Optimise.DagNode.ConstantTag.*;

/**
 *
 */
public class DAG {
    public static PrintWriter logger;
    private  List<DagNode> builtNodes=new ArrayList<>();
    private  List<Instruction> storeInstrs=new ArrayList<>();
    private  HashMap<Value,DagNode> iniNodes=new HashMap<>();

    private  HashMap<Value, List<DagNode>> array_refNodes=new HashMap<>();
    private  HashMap<Var,Var> clearPointers =new HashMap<>();
    //保存原跳转语句
    private  HashMap<Integer,Instruction> jumpMap =new HashMap<>();
    //保存指令的标签
    private HashMap<Integer,Label> ori_label=new HashMap<>();

    //旧id和新Instr的关系
    private  HashMap<Integer,Instruction> ori_idTONewInstr=new HashMap<>();
    public DAG(){}

    private DagNode buildNode(DagTag tag, Location location, int ori_id, Value symbol, Value... values) {
        List<DagNode> children = new ArrayList<>();

        /*获取子节点*/
        Arrays.stream(values).toList().forEach(v -> {
            DagNode node = getDagNode(tag, v, location, ori_id);
            children.add(node);
        });
        Value NewV = checkValues(tag, children);
        int idx;
        DagNode sameNode = null,newNode=null,result=null;

        /*常量折叠*/
        if (NewV != null) {
            DagNode builtNode = buildAssignNode((Var) symbol, NewV, location, ori_id);
            builtNodes.add(builtNode);
            logger.println("#生成常量折叠节点:" + builtNode);
            return builtNode;
        }
        /*代数不变式优化*/
        else if ((idx = unchangedExpression(tag, values)) != -1&&
                !(symbol instanceof Var var&&var.isLeftValue())) {
            sameNode = children.get(idx);
        }
        else {
            DagNode node = DagNode.createInnerNode(tag, location, ori_id, children, symbol);
            /*寻找公共子表达式*/
            if (tag != OP.param
                    && !(symbol instanceof Temp t && tag == Array_Assigning && (t.isArrayAddr()))) {
                sameNode = findNodeWithSameExpr(node);
            }
            if(sameNode==null){
                logger.println("$buildNode:" + node);
                //由于结构原因,[]=类型的节点还没有形成
                if (node.getTag() != Array_Assigned && node.getTag() != Pointer_Assigned)
                    builtNodes.add(node);
                updateChildrenInfos(node, values);
                result=node;
            }
        }

        if (sameNode != null) {
            logger.println("$useNode:" + sameNode);
            sameNode.addSymbol(symbol, ori_id, location);
            result= sameNode;
        }

        return result;
    }

    /**
     * @return 不为零或一的子结点的标号,若都不为零或一,返回-1;
     */
    private int unchangedExpression(DagTag tag,Value[] values){
        if(values.length<2)
            return -1;
        Value v1=values[0],v2=values[1];
        if((tag== OP.add||tag== OP.sub)){
            if(v1 instanceof Literal l1&&l1.isZero()){
                return 1;
            }else if(v2 instanceof Literal l2&&l2.isZero()){
                return 0;
            }
        }else if(tag==OP.mul||tag== OP.div){
            if(v1 instanceof Literal l1&&l1.isOne()){
                return 1;
            }else if(v2 instanceof Literal l2&&l2.isOne()){
                return 0;
            }
        }
        return -1;
    }

    /**
     * @return 与node拥有相同公共表达式的节点
     */
    private DagNode findNodeWithSameExpr(DagNode node) {
        for (DagNode node2 : builtNodes) {
            if (node2.isAlive()) {
                if (node2.hasCommonExprWith(node))
                    return node2;
                else if (isSamePosAddrRef(node, node2)) {
                    /*如果是数组读操作,返回相同位置的写操作的写入项*/
                    return node2.getNthChild(2);
                }else if(isSamePosPointerRef(node,node2)){
                    return node2.getNthChild(1);
                }
            }
        }
        return null;
    }

    /**
     * @return 判断是否读写数组的相同位置
     */
    private static boolean isSamePosAddrRef(DagNode reader, DagNode writer) {
        if (reader.isMemReadNode() && writer.isMemWriteNode()) {
            DagNode child1 = reader.getNthChild(0),
                    child2 = reader.getNthChild(1);
            DagNode child3 = writer.getNthChild(0),
                    child4 = writer.getNthChild(1);
            return child1.hasCommonExprWith(child3)
                    && child2.hasCommonExprWith(child4);
        }
        return false;
    }

    /**
     * @return 判断是否解引用相同位置
     */
    private static boolean isSamePosPointerRef(DagNode reader,DagNode writer){
        if(reader.getTag()==Pointer_Assigning&&writer.getTag()==Pointer_Assigned){
            DagNode c1=reader.getNthChild(0),
                    c2=writer.getNthChild(0);
            return c1.hasCommonExprWith(c2);
        }
        return false;
    }

    private static boolean ReadAndWriteConflict(DagNode reader, DagNode writer){
        if (reader.isMemReadNode() && writer.isMemWriteNode()) {
            DagNode child1 = reader.getNthChild(0),
                    child2 = reader.getNthChild(1);
            DagNode child3 = writer.getNthChild(0),
                    child4 = writer.getNthChild(1);
            logger.println("#testConflict\n#reader="+reader+"\n#writer="+writer+"\n");
            return (!child1.hasCommonExprWith(child3)) ||
                    ( child2.hasCommonExprWith(child4) && (!child3.isConstNode() || !child4.isConstNode()));
        }
        return false;
    }

    /**
     *检查是否可进行常量折叠
     * @return 若可以,返回折叠后的字面量,否则返回null
     */
    private Value checkValues(DagTag tag,List<DagNode> children){
        if(tag instanceof OP op){
            if (children.size()== 2 && OP.isBinary(op)) {
                DagNode child1=children.get(0),child2=children.get(1);
                if (child1.isConstNode()&&child2.isConstNode()) {
                    Literal l=child1.getConstValue(),r=child2.getConstValue();
                    Literal literal = ConstExprCalculator.calculate(l, r, op);
                    return literal;
                }
            }
            else if(children.size()==1&&OP.isUnary(op)){
                DagNode child1=children.get(0);
                if(child1.isConstNode()&&(tag!= OP.lea)){
                    Literal l=child1.getConstValue();
                    Literal literal=ConstExprCalculator.calculateUnary(l,op);
                    return literal;
                }
            }
        }
        return null;
    }


    /**
     * 更新@param node的子节点的信息,包括是否为根节点,是否活跃,对地址的依赖关系
     */
    private void updateChildrenInfos(DagNode node,Value... values){
        /*设定子节点的根属性和活跃关系*/
        node.setPropertiesOfChildren(true,true);
        /*更新地址依赖关系*/
        addRefOfNewNode(node,values);
    }

    private    DagNode getIniNode(Value value, Location location, int ori_id) {
        if(iniNodes.containsKey(value)){
            return iniNodes.get(value);
        }else{
            DagNode node=DagNode.getInstance (DagNode.ConstantTag.INI,value,location,ori_id);
            logger.println("#create: "+node);
            iniNodes.put(value,node);
            builtNodes.add(node);
            return node;
        }
    }


    private DagNode getDagNode(DagTag tag,Value v,Location location,int ori_id){
        //取左值
        if(tag==OP.lea){
            //这种情况只能取非临时变量的左值,而不能取非具名变量的值
            if (v instanceof Var var &&!var.isTemp()){
                return  DagNode.getAddress(var,location,ori_id);
            }
        }
        //取右值
        return getDagNode(v,location,ori_id);
    }

    /**
     * @return 储存v的右值的节点
     */
    private  DagNode getDagNode(Value v, Location location, int ori_id){
        if(v.getAssignedDag()==null){
            /*被杀死的上一个赋值结点,需要声明其需要被构建*/
            if(v.getRecentDag()!=null){
                DagNode recentNode=v.getRecentDag();
                recentNode.setNeedStore(v,true);
            }
            return getIniNode(v,location,ori_id);
        }
        return v.getAssignedDag();
    }

    public DagNode  buildNode(Instruction instr){
        OP op=instr.getOp();
        Value arg1=instr.getArg1(),arg2=instr.getArg2();
        Result result=instr.getResult();
        int ori_id=instr.getId();
        Location location1=instr.getLocation();
//        //(instr);
        logger.println("\n"+instr+"\t@@"+instr.getLocation().getLine());
        if(instr.isJumpInstr()){
            jumpMap.put(ori_id,instr);
        }
        if(instr.getLabel()!=null){
            Label lbl=instr.getLabel();
            ori_label.put(ori_id,lbl);
        }

        switch (op){
            case assign -> {
               return buildAssignNode((Var)result,arg1,location1,ori_id );
            }

            case add,mul,sub,div,mod,shift_left,shift_right ,
                    and,or,xor,cast,
                    gt,ge,eq,ne,lt,le,
                    minus,neg,not,param-> {
                if(arg2!=null)
                    return buildNode(op,location1,ori_id,result,arg1,arg2);
                else
                    return buildNode(op,location1,ori_id,result,arg1);
            }
            case ret->{
                if(arg1==null){
                    storeInstrs.add(instr);
                    return null;
                }else{
                    return buildNode(op,location1,ori_id,result,arg1);
                }
            }

            case if_jump,ifFalse_jump -> {
                if(instr instanceof ConditionJump){
                    return buildNode(op,location1,ori_id,result,arg1,arg2);
                } else{
                    return buildNode(op,location1,ori_id,result,arg1);
//                    storeInstrs.add(instr);
//                    return null;
                }
            }

            case array -> {
                Var var = (Var) result;
                DagTag tag=var.isLeftValue()? Array_Assigned:DagNode.ConstantTag.Array_Assigning;
                DagNode node=buildNode(tag,location1,ori_id,result,arg1,arg2);
                /* []= */
                if (var.isLeftValue()) {
                    /*数组变量的数组访问*/
                    if(arg1 instanceof Var arr &&arr.isArrayAddr()){
                        removeArray_ref(arg1,node);
                    }
                }
                /* =[] */
                else {
                    createAddrRefEntry(arg1,node);
                }
                return node;
            }

            case lea ->{
                return buildLeaNode((Var) result,arg1,location1,ori_id);
            }

            case deref -> {
                return buildDerefNode((Var) result,(Var)arg1,location1,ori_id);
            }

            case call -> {
                CallInstruction callInstruction=(CallInstruction) instr;
                if(callInstruction.isDirectCall()){
                    killAllNodes();
                    storeInstrs.add(instr);
                    return null;
                }else{
                    CallNode  node=(CallNode) buildNode(op,location1,ori_id,result,arg1,arg2);
                    node.setCallee(callInstruction.getCallee());
                    return node;
                }
            }


            default -> {
                /*不需要优化*/
                storeInstrs.add(instr);
                return null;
//                throw new  UnImplementedException("未支持的操作:"+op);
            }
        }
    }

    private DagNode buildDerefNode(Var result,Var operand,Location location,int ori_id){
        DagTag tag=result.isLeftValue()? Pointer_Assigned
                : Pointer_Assigning;
        /* *= */
        if(result.isLeftValue()){
            return buildDerefAssignedNode(result,operand,location,ori_id);
        }else{
            //所解引用的节点
            DagNode node=getDagNode(operand,location,ori_id);
            /*如果节点代表一个地址运算的结果,直接该返回地址运算的对象,不需要生成新的解引用节点*/
            if(node.isAddressNode()){
                /*地址运算所实际作用的节点*/
                DagNode  child=node.getAddressOperandNode();
                //注意取地址操作可能作用于函数名
                if (child.getFirstSymbol() instanceof Var var
                        && !var.isArrayAddr()) {
                    DagNode derefResult = getDagNode(var, location, ori_id);
                    if (derefResult == null) {
                        logger.println("#buidDerefNode,无法找到 " + operand + "所指向变量 的最后定值节点");
                        throw new RuntimeException("should not happen,node=" + node + ",var=" + var);
                    }
                    derefResult.addSymbol(result, ori_id, location);
                    logger.println("用#" + child + "作为解引用结果");
                    return derefResult;
                }
            }

            return buildNode(tag,location,ori_id,result,operand);
        }
    }


    /**
     *   处理*=类型的结点
     */
    private DagNode buildDerefAssignedNode(Var result,Var operand,Location location,int ori_id){
        DagNode node= buildNode(Pointer_Assigned,location,ori_id,result,operand);
        Var pointingVar=clearPointers.get(operand);
        if(pointingVar!=null){
//                killVar(pointingVar);
            //此处增加此关系是为了在生成接下来的赋值节点的时候让变量的最后定值位置确定为赋值右侧节点
            createClearPointersEntry(result,pointingVar);
        }
        else{
            logger.println("暂无法找到"+operand+"的明确指向");
            killAllNodes();
//                throw new RuntimeException("operand="+operand+",node="+node);
        }
        return node;
    }


    private DagNode buildLeaNode(Var result,Value operand,Location location,int ori_id){
        DagNode node=getDagNode(operand,location,ori_id);
        DagNode ret=node;
//        if(operand instanceof DefinedFunction){
//            ;
//        }
        /*不需要生成新的节点*/
            if(node.isDerefNode()){
            DagNode operandNode=node.getUnaryOPNode();
            operandNode.addSymbol(result,ori_id,location);
            logger.println("用#"+operandNode+"作为取地址结果");
        }else{
            if(operand instanceof Var var &&!var.isTemp())
                createClearPointersEntry(result,var);
            ret = buildNode(OP.lea, location, ori_id, result, operand);
        }
        /*增加对取地址的变量及其后代的依赖*/
//        createAddrRefEntry(operand,ret);
        return ret;
    }

    private DagNode buildAssignNode(Var result,Value right,Location location,int ori_id){
        DagNode expr=getDagNode(right,location,ori_id);
        DagNode resultNode=result.getAssignedDag();
        if(resultNode!=null){
            DagTag tag=resultNode.getTag();
            /*需要对此种类的结点变形*/
            if ((tag == Array_Assigned
                    ||tag==Pointer_Assigned)) {
                return reshapeNode(result,resultNode,expr,location,ori_id);
            }
            /*成员访问得到的数组或结构体被赋值*/
            else{
                if(result.isArrayAddr() && tag== OP.add){
                    logger.println("resultNode="+resultNode+",right="+right);
//                    //转换成数组写节点
                    return shiftToArrayAssignedNode(resultNode, expr);
                }
            }
        }

        //旧的初始结点不再有效
        if(iniNodes.containsKey(result)){
            IniNode iniNode=(IniNode) iniNodes.get(result);
            iniNode.setValid(false);
            iniNodes.remove(result);
        }

        if(pointingToVariable(right,expr)){
            Var pointingVar=clearPointers.get((Var) right);
            addPointer(result,pointingVar);
        }
        expr.addSymbol(result,ori_id,location);
        logger.println(expr);
        return expr;
    }

    /**
     * 成员访问或数组访问得到的左值(数组或联合体)的赋值结点你转换为数组访问结点
     */
    private DagNode shiftToArrayAssignedNode(DagNode resultNode,DagNode exprNode){
//         DagNode base=resultNode.getNthChild(0),index=resultNode.getNthChild(1);
         resultNode.setTag(Array_Assigned);
         resultNode.addChild(exprNode);
         return resultNode;
    }

    private DagNode reshapeNode(Var symbolOfNode, DagNode node, DagNode expr
            , Location location, int ori_id) {
//        resultNode.removeSymbol(result);
        node.addChild(expr);
        DagTag tag = node.getTag();
        DagNode node1 = null;

        if (tag == Array_Assigned)
            findNodeWithSameExpr(node);
        else if (tag == Pointer_Assigned) {
            Var pointingVar = clearPointers.get(symbolOfNode);
            if (pointingVar != null) {
                expr.addSymbol(pointingVar, ori_id, location);
            }
        }

        /*找到数组写操作的公共子表达式,不必生成新节点*/
        if (node1 != null) {
            builtNodes.remove(node);
            return node1;
        } else {
            Var base=(Var) node.getNthChild(0).getFirstSymbol();
            /*如果是指针变量的数组访问,需要当作指针与整数相加并解引用处理*/
            if (!base.isArrayAddr()) {
                logger.println("#symbolOfNode="+symbolOfNode);
                killAllNodes();
            }

            /*增加父节点计数*/
            expr.updateParentCnt(true);
            //此时[]*节点才真正形成
            builtNodes.add(node);
            return node;
        }
    }


    private boolean pointingToVariable(Value right,DagNode node){
       return node.isAddressNode() &&(clearPointers.containsKey(right));
    }

    /**
     * @param newNode 在方法#buildNode中新创建的节点
     * @param values 子节点所代表的值
     */
    private void addRefOfNewNode(DagNode newNode, Value... values){
        /*增加从子节点后代获得的依赖关系*/
        List<DagNode> children=newNode.getChildren();
        if(children!=null){
            Set<Value> dependents=new HashSet<>();
            children.forEach(child->{
                if(child.getDependentsAddr()!=null){
                    dependents.addAll(child.getDependentsAddr());
                }
            });
            dependents.forEach(depend -> addRef(depend, newNode));
        }

//       /*增加对子节点的依赖关系*/
        for(int i=0;i<values.length;i++){
            Value v=values[i];
            if (v instanceof Var var && (var.isArrayAddr() || var.getIRType() == Type.pointer)) {
                createAddrRefEntry(v,newNode);
            }
        }
    }

    /**
     * 增加newNode对地址array的依赖
     */
    private void addRef(Value array, DagNode newNode){
        List<DagNode> nodes=array_refNodes.get(array);
        boolean added=false;

        if(nodes==null){
            throw new RuntimeException("should not happen");
            //只有是非临时变量(具有地址)才新建依赖项
//            if(array instanceof Var var
////                    &&var.isArrayAddr())
//                      &&!var.isTemp())
//            {
//                nodes=createAddrRefEntry(array,newNode);
//                added=true;
//            }
        }else{
            if(!nodes.contains(newNode)){
                nodes.add(newNode);
                added=true;
            }
        }

        boolean b=false;
        if(added){
            logger.println("#addArray_ref,array:"+array+",newNode:"+newNode);
            b= newNode.addDependentsAddr(array);
        }

        if(b||added){
            logger.println("#依赖于"+array+"的结点:");
            nodes.forEach(n->logger.println(n));
            logger.println("\n"+newNode+"所依赖的值: "+newNode.getDependentsAddr());
            logger.println();
        }
    }

    private void addPointer(Var pointer,Var var){
        logger.println(String.format("#addClearPointer %s -> %s",pointer,var));
        clearPointers.put(pointer,var);
    }

    /**
     * 依赖关系表中增加addr的表项,初始依赖节点为node
     */
    private List<DagNode> createAddrRefEntry(Value addr, DagNode node){
        List<DagNode> nodes=array_refNodes.get(addr);
        if(nodes==null){
            logger.println("creating new entry:"+addr);
            nodes=new ArrayList<>();
            nodes.add(node);
            array_refNodes.put(addr,nodes);
        }
        addRef(addr,node);
        return nodes;
    }

    private void createClearPointersEntry(Var pointer,Var var){
        Var pointingVar=clearPointers.get(pointer);
        if(pointingVar==null){
            logger.println("#creating new Pointer entry:<"+pointer+","+var+">");
            clearPointers.put(pointer,var);
        }
    }

    /**
     *删除DAG结点,使其不能成为公共子表达式的一部分
     */
    private void removeNode(DagNode node){
        node.clear();
    }

    /**
     * @param array 数组访问的基址
     */
    private void removeArray_ref(Value array,DagNode writer) {
        DagNode lastAssignedNode = array.getAssignedDag();
        if (lastAssignedNode == null)
            return;

        List<Value> dependents = new ArrayList<>(lastAssignedNode.getDependentsAddr());
        dependents.add(array);
        logger.println("#removeArray_ref,dependents=" + dependents);
        for (Value dependent : dependents) {
            removeDependent(dependent, writer);
        }
    }

    /**
     * 去除所有节点对dependent的依赖
     */
    private void removeDependent(Value dependent,DagNode writer){
        List<DagNode> nodes = array_refNodes.get(dependent);//所有值依赖于dependent的节点
        logger.println(String.format("#移除依赖于%s的节点:",dependent));
        if(nodes==null){
            return;
        }
        List<DagNode>  nodesLst=new ArrayList<>(nodes);
        nodesLst.forEach(node -> {
            if(node.isMemReadNode()
                && ReadAndWriteConflict(node,writer)){
                removeNode(node);
                nodes.remove(node);
                logger.println(node);
            }
        });
//        nodes.clear();
    }


    /**
     * DAG到基本块的重组
     */
    public  List<Instruction> rebuiltInstrs(){
//        logger.println("\nOriginal instructions:");
//        block.getInstructions().forEach(i->logger.println(i));
        logger.println("\n----------------------\nRebuilding:");

        List<Instruction> instructions = new ArrayList<>();
        final List<DagNode> roots =findDeserveRebuiltRootNodes();
        for (DagNode node : roots) {
            List<Instruction> instrs = rebuildRootNode(node);
            if (instrs != null)
                instructions.addAll(instrs);
        }

        storeInstrs.forEach(instr->instr.setOri_id(instr.getId()));
        instructions.addAll(storeInstrs);
        instructions.sort(Instruction.comparator);

        logger.println("\n----------------------\nNew Instructions:");
        int preLine=-1;
        for (Instruction i : instructions) {
            if(i.getLocation()!=null&&i.getLocation().getLine()!=preLine){
                logger.println();
            }
            logger.println(String.format("%-48s\t%-10s", i,
                            i.getLocation() == null ? "" : "@"+(i.getLocation().getLine()))
//               +String.format("     %-6s\t%-3s\n",i.getLocation(),i.getOri_id())
            );
            preLine=i.getLocation()==null?-1: i.getLocation().getLine();
        }
        logger.println("指令数:"+instructions.size());

        return instructions;
    }

    private List<DagNode> findDeserveRebuiltRootNodes() {
        List<DagNode> roots = new ArrayList<>();
        HashSet<DagNode> visitedRootNodes=new HashSet<>();
        boolean updated;
        int turn=1;
        do {
            logger.println("***Turn:"+turn);
            updated=false;
            for (DagNode node : builtNodes) {
                if(visitedRootNodes.contains(node))
                    continue;

//                //("visiting :"+node+",isRoot?"+node.isRoot()+"\n");
                logger.println("visiting :"+node+",isRoot?"+node.isRoot()+"\n");
                if (node.isRoot()) {
                    if (node.deserveRebuilt()) {
                        roots.add(node);
                        logger.println("#add "+node+"\n");
                    } else {
                        try {
                            node.setPropertiesOfChildren(false, false);
                        }catch (Exception e){
                            throw new RuntimeException(e);
                        }
                    }
                    visitedRootNodes.add(node);
                    updated =updated||!node.isIniNode();
                }
            }
            turn++;
        } while (updated);

        return roots;
    }

    private List<Instruction> rebuildRootNode(DagNode node){
        logger.println("\n------------------------\n重构根节点:"+node);
        List<Instruction> instrs =new ArrayList<>();

        /*重构子节点*/
        if (node.getChildren() == null || node.getChildren().isEmpty()) {
           ;
        } else {
            List<Instruction> childInstrs;
            for (DagNode child : node.getChildren()) {
                childInstrs = rebuildRootNode (child);
                instrs.addAll(childInstrs);
            }
        }

        /*重构本层节点*/
        instrs.addAll(rebuilt(node));

        logger.println("重构根节点 "+node+" 结束\n------------------------\n");
        return instrs;
    }

    /**
     * 重构节点对应的中间语句
     * @return 如果不需要生成任何语句,返回null
     */
    private List<Instruction> rebuilt(DagNode node){
        boolean deserve=node.deserveRebuilt();
        List<Instruction> instructions=new ArrayList<>();

        if (deserve) {
            List<DagNode>children=node.getChildren();
            if(children!=null) {
                DagTag tag= node.tag;
                OP op;
                if(tag== Array_Assigned||tag==Array_Assigning){
                    op=OP.array;
                    setIndexNode(node);
                }else if(tag==Pointer_Assigned||tag==Pointer_Assigning) {
                    op = OP.deref;
                }
                else{
                    op=(OP)tag;
                }

                Var result = (node.getSymbols().isEmpty()||node.isJumpNode())?null:(Var) node.getFirstSymbol();
                Instruction instr ;
                if(tag== OP.param){
                    instr = genParamInstr(node.offerArg(0), node.getLocation(), node.getOriIid());
                }else if((tag==OP.if_jump||tag==OP.ifFalse_jump)
                        && node.getArgNum()==2){
                    instr=genConditionJump( tag,node.offerArg(0),node.offerArg(1) ,node.getLocation(),node.getOriIid());
                }else if(node instanceof CallNode callNode){
                    instr=genCallInstr(node.offerArg(0),node.offerArg(1),result,((CallNode) node).getCallee(),
                            node.getLocation(),node.getOriIid());
                }
                else {
                    instr = gen(op, children.isEmpty() ? null : node.offerArg(0),
                            children.size() < 2 ? null : node.offerArg(1),
                            result, node.getLocation(), node.getOriIid());
                }
                instructions.add(instr);

                if(tag==Array_Assigned||tag==Pointer_Assigned){
                    //("#"+node);
                    Value right=node.offerArg(tag==Array_Assigned?2:1);
                    Instruction instr2=genAssignInstr(result,right,node.getLocation(),node.getOriIid());
                    instructions.add(instr2);
                }
            }
            //生成赋值语句
            if(node.deserveAssign())
                instructions.addAll(getAssign(node));
        }

        node.setBuilt(true);
        return instructions;
    }

    /**
     * 设置索引属性
     */
    private void setIndexNode(DagNode node){
        Value index=node.getArg(1);
        DagNode indexNode=node.getNthChild(1);
        if(index instanceof Temp t && indexNode.getTag()==OP.mul){
            t.setIndex(true);
            int scale = Integer.parseInt(
                    ((Literal) (indexNode.getArg(1))).getLxrValue());
            t.setScale(scale);

            /*设置依赖变量,如t=n*4中的n */
            if(indexNode.getTag()==OP.mul){
                DagNode dependencyNode=indexNode.getNthChild(0);
                Value dependency=dependencyNode.getFirstSymbol();
                if(dependency instanceof  Var var){
                    t.setDependency(var);
                }
            }
        }
    }



    /**
     * 为同一个DagNode的附着变量生成赋值语句
     */
    private List<Instruction> getAssign(DagNode node){
        List<Instruction> instructions=new ArrayList<>();
        Value first=node.getFirstAssignedSymbol();
        List<Value> symbols=node.getSymbols();

        for(int i=0;i<symbols.size();i++){
            Value s=symbols.get(i);
            if (!s.equals(first) && (node.canBeAssigned(s))) {
                Instruction instr = genAssignInstr((Var) s, first, node.getLocation(i), node.getOriIid(i));
                instructions.add(instr);
            }
        }
        return instructions;
    }

    public  void clear(){
        builtNodes.clear();
        storeInstrs.clear();
        iniNodes.clear();
        array_refNodes.clear();
        ori_idTONewInstr.clear();
        clearPointers.clear();
    }

    private void killAllNodes(){
        logger.println("#$killAllNodes");
        builtNodes.stream()
//                .filter(node ->! node.loadLeftValue())
                .forEach(node -> {
            node.clear();
            node.removeAllSymbols();
        });
        array_refNodes.clear();
        clearPointers.clear();
    }

    private void killVar(Var var) {
        logger.println("#kill Node of " + var);
        DagNode node = var.getAssignedDag();
        if (node == null) {
            logger.println("#KillVar,Node is null");
        }
        var.setAssignedDag(null);
    }

    private  Instruction genAssignInstr(Var left,Value right,Location location,int ori_id){
        Instruction instr=gen(OP.assign,right,null,left,location,ori_id);
        return instr;
    }

    private  Instruction gen(OP op,Value arg1,Value arg2,Var result,Location location,int ori_id){
        Instruction instr=new Instruction(op,arg1,arg2,result,location,true);
        recordInstr(instr,ori_id);
        return instr;
    }


    private ParamInstruction genParamInstr(Value arg,Location location,int ori_id){
        ParamInstruction instr =new ParamInstruction(arg,location,true);
        recordInstr(instr,ori_id);
        return instr;
    }

    private ConditionJump genConditionJump(DagTag tag, Value arg1, Value arg2, Location location, int ori_id){
        ConditionJump last=(ConditionJump) jumpMap.get(ori_id);
        Label des=(Label) last.getResult();
        OP jumpType=last.getJumpType();
        ConditionJump conJump=new ConditionJump((OP) tag,arg1,arg2,
                       des,location,
                        jumpType,true);
        recordInstr(conJump,ori_id);
        return conJump;
    }

    private CallInstruction genCallInstr(Value arg1, Value arg2, Result result, Callee callee,
            Location location,int ori_id){
        CallInstruction callInstruction=new CallInstruction(arg1,arg2,result,null,callee,
                location,true);
        recordInstr(callInstruction,ori_id);
        return callInstruction;
    }

    private void recordInstr(Instruction instr,int ori_id){
        instr.setOri_id(ori_id);
        Label  label=ori_label.get(ori_id);
        if(label !=null){
            instr.setLabel(label);
        }
        Instruction  jumpInstr =jumpMap.get(ori_id);
        if(jumpInstr!=null&&jumpInstr.getResult() instanceof Label){
            Label label1=(Label)(jumpInstr.getResult());
            instr.setResult(label1);
        }
        ori_idTONewInstr.put(ori_id,instr);
        logger.println(instr);
    }

    public Instruction getInstrOfOri_id(int ori_id){
        if(!ori_idTONewInstr.containsKey(ori_id))
            throw new RuntimeException("should not happen!");
        return ori_idTONewInstr.get(ori_id);
    }
}
