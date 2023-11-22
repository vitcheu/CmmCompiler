package IR.Optimise;

import AST.Callee;
import AST.Node;
import IR.*;
import IR.Constants.OP;
import Parser.Entity.Location;
import com.sun.source.tree.LambdaExpressionTree;

import javax.swing.text.html.HTML;
import java.util.*;
import java.util.function.Supplier;

import static IR.Optimise.DagNode.ConstantTag.Array_Assigning;
import static compile.CompilerOption.*;

/**
 *
 */
public class DagNode {
    enum ConstantTag implements DagTag {
        INI,
        //表示一个具名变量的地址
        Address,
        Array_Assigned,
        Array_Assigning,
        Pointer_Assigned,
        Pointer_Assigning;

        @Override
        public String getDescriptionStr() {
            return switch (this) {
                case Array_Assigned -> "[]=";
                case Array_Assigning -> "=[]";
                case Pointer_Assigned -> "*=";
                case Pointer_Assigning -> "=*";
                case Address -> "Addr";
                default -> toString();
            };
        }
    }

    protected static int id = 0;
    private final int did;
    private static final int default_ori_id = -1;

    protected class SymbolInfo {
        Value symbol;
        int ori_id;
        Location location;
        /*用于判断被杀死后是否还需要保留该结点的值*/
        boolean needStore = false;
        //是否为参数
        boolean argument = false;


        public SymbolInfo(Value symbol, int ori_id, Location location) {
            this.symbol = symbol;
            this.ori_id = ori_id;
            this.location = location;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SymbolInfo that = (SymbolInfo) o;
            return ori_id == that.ori_id && Objects.equals(symbol, that.symbol) && Objects.equals(location, that.location);
        }

        @Override
        public int hashCode() {
            return Objects.hash(symbol, ori_id, location);
        }

        boolean used() {
//            return true;
            return (symbol instanceof Var var) &&
                    ((var.isActive()) || needStore || addrUsedMap.containsKey(symbol) || var.isGlobal() || var.isStatic());
        }
    }

    protected List<SymbolInfo> symbolInfos = new ArrayList<>();

    //父节点数量
    private int parentCnt;
    private Set<DagNode> parents = new HashSet<>();

    List<DagNode> children;
    //依赖的数组地址
    private HashSet<Value> dependentsAddr = new LinkedHashSet<>();
    DagTag tag;
    boolean alive = true;
    boolean built = false;

    Location location;
    int oriId;

    //是否引用了地址
    private static HashMap<Value, DagNode> addrUsedMap = new HashMap<>();

    protected DagNode(DagTag tag, Value symbol, Location location, int ori_id) {
        did = id++;
        this.tag = tag;
        parentCnt = 0;
        this.location = location;
        this.oriId = ori_id;
        if (symbol != null)
            addSymbol(symbol, ori_id, location);
    }

    public static DagNode getInstance(DagTag tag, Value symbol, Location location, int ori_id) {
        DagNode node;
        if (tag == ConstantTag.INI) {
            node = new IniNode(symbol, location, ori_id);
        } else if (tag == OP.call) {
            node = new CallNode(OP.call, symbol, location, ori_id);
        } else {
            node = new DagNode(tag, symbol, location, ori_id);
        }
        return node;
    }

    public static DagNode getAddress(Var var, Location location, int ori_id) {
        DagNode addr = addrUsedMap.get(var);
        if (addr == null) {
            addr = new DagNode(ConstantTag.Address, var, location, ori_id);
            addrUsedMap.put(var, addr);
        }
        return addr;
    }

    DagTag getTag() {
        return tag;
    }

    public List<Value> getSymbols() {
        List<Value> symbols = new ArrayList<>();
        symbolInfos.forEach(info -> symbols.add(info.symbol));
        return symbols;
    }

    public boolean isOperandOfLeaNode() {
        return parents.stream()
                .anyMatch(p -> p.tag == OP.lea)
                && (tag == Array_Assigning);
    }

    String getSymbolStr() {
        var symbols = getSymbols();
        return symbols.toString();
    }

    private String getChildrenStr() {
        if (children == null) return "none";
        StringBuilder builder = new StringBuilder();
        children.forEach(child -> builder.append("^" + child.getTag() + child.getSymbolStr() + ","));
        if (builder.length() >= 1) builder.delete(builder.length() - 1, builder.length());
        return builder.toString();
    }

    public Location getLocation(int n) {
        return symbolInfos.get(n).location;
    }

    public Location getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return String.format("$%d(%s)@%s{%s}<%s>",
                getDid(), getSymbolStr(), tag, getChildrenStr(),
                (symbolInfos.isEmpty() || getLocation(0) == null) ? "" : getLocation(0).getLine());
    }

    /**
     * 判断是否拥有公共子表达式
     */
    boolean hasCommonExprWith(DagNode node) {
        if (this.tag != node.tag)
            return false;
        if (this.children == null && node.children != null
                || this.children != null && node.children == null)
            return false;
        else {
            /*叶子结点*/
            if (this.children == null && node.children == null) {
                Value sym = node.getSymbols().get(0);
                return this.getSymbols().contains(sym);
            }
            /*内部结点*/
            else {
                if (this.children.size() != node.children.size())
                    return false;
                if (tag instanceof OP op && OP.exchangeable(op)) {
                    DagNode c1 = children.get(0), c2 = children.get(1),
                            c3 = node.children.get(0), c4 = node.children.get(1);
                    if (c1.hasCommonExprWith(c3) && c2.hasCommonExprWith(c4)
                            || c2.hasCommonExprWith(c3) && c1.hasCommonExprWith(c4))
                        return true;
                }

                /*比较各个子节点*/
                for (int i = 0; i < children.size(); i++) {
                    DagNode child = children.get(i), nodeChild = node.children.get(i);
                    if (!child.hasCommonExprWith(nodeChild)) {
                        return false;
                    }
                }

                return true;
            }
        }
    }

    /**
     * 获取作为代表本层运算结果的变量,其他附着变量直接由该变量赋值得到
     * 优先选择非临时变量
     */
    protected Value getFirstSymbol(boolean tempFirst) {
        //如果是常量节点,直接返回常量
        if (isConstNode()) {
            return getConstValue();
        }
        if (tempFirst) {
            var optional = symbolInfos.stream().map(info -> info.symbol)
                    .filter(Temp.class::isInstance)
                    .findFirst();
            if (optional.isPresent())
                return optional.get();
        }
        return getFirstInfo().symbol;
    }

    public Value offerFirstSymbol(){
        return getFirstSymbol(false);
    }

    Value getFirstSymbol() {
        return getFirstSymbol(false);
    }

    Value getFirstAssignedSymbol() {
        return getFirstSymbol(false);
    }

    private SymbolInfo getFirstInfo() {
        return symbolInfos.get(0);
    }


    /**
     * 增加附着变量
     */
    void addSymbol(Value v, int oriIid, Location location) {
        SymbolInfo info = new SymbolInfo(v, oriIid, location);
        if (!symbolInfos.contains(info)) {
            symbolInfos.add(info);
            /*切断与最近定值节点的联系*/
            //如果节点是一个代表变量左值的节点(tag==Address),则不用修改
            if (this.tag != ConstantTag.Address) {
                DagNode lastAssigned = v.getAssignedDag(),
                        recentDag = v.getRecentDag();
                if (lastAssigned != null)
                    lastAssigned.removeSymbol(v);
                else {
//                    //设置另一个子图上的节点中v的活跃信息
//                    if (recentDag != null) {
//                        recentDag.setActive(v, true);
//                    }
                }
//
                v.setAssignedDag(this);
            }
        }
    }

    /**
     * @return 获取单目运算的运算节点
     */
    DagNode getUnaryOPNode() {
        return getChildren().get(0);
    }

    /**
     * @return 是否为取地址节点
     */
    boolean isAddressNode() {
        return tag == OP.lea || tag == ConstantTag.Address;
    }

    /**
     * @return 是否装载边变量的左值
     */
    boolean loadLeftValue() {
        return isAddressNode() || (isIniNode() &&
                getSymbols().stream().allMatch(
                        sym -> (sym instanceof Var) && (((Var) sym).isArrayAddr())
                ));
    }

    boolean isDerefNode() {
        return tag == OP.deref || tag == ConstantTag.Pointer_Assigning
                || tag == ConstantTag.Pointer_Assigned;
    }

    /**
     * 获取取地址操作的对象节点
     */
    DagNode getAddressOperandNode() {
        DagNode child = getChildren().get(0);
        return child;
    }


    void removeSymbol(Value v) {
        //如果本节点为v的ini节点则不必删除
        if (isIniNodeOf(v)) {
            IniNode iniNode = (IniNode) this;
//            iniNode.setValid(false);
        } else {
            SymbolInfo info = getInfo(v);
            symbolInfos.remove(info);
        }
        v.setAssignedDag(null);
    }


    private SymbolInfo getInfo(Value v) {
        for (SymbolInfo info : symbolInfos) {
            if (info.symbol.equals(v)) {
                return info;
            }
        }
        return null;
    }

    public void setArgument(Value v, boolean argument) {
        SymbolInfo info = getInfo(v);
        info.argument = argument;
    }


    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setNeedStore(Value sym, boolean needStore) {
        SymbolInfo info = getInfo(sym);
        info.needStore = needStore;
    }

    public void setTag(DagTag tag) {
        this.tag = tag;
    }

    /**
     * @return 是否值得生成IR语句
     */
    public boolean deserveRebuilt() {
        return !isBuilt() && (mustBeReBuilt(tag) ||
                !isRoot() || (hasVariable()));
    }

    private boolean mustBeReBuilt(DagTag tag) {
        return tag == OP.param || tag == OP.ret || tag == ConstantTag.Array_Assigned
                || tag == OP.call
                || tag == ConstantTag.Pointer_Assigned || tag == OP.if_jump || tag == OP.ifFalse_jump;
    }

    /**
     * @return 是否值得生成赋值语句
     */
    public boolean deserveAssign() {
        return hasVariable()
                ||
                symbolInfos.stream().anyMatch(info -> (info instanceof Var var && !var.isTemp()));
    }

    /**
     * @return 是否有需要使用的变量
     */
    private boolean hasVariable() {
        return symbolInfos.stream().anyMatch(info -> info.used());
    }

    List<DagNode> getChildren() {
        return children;
    }

    DagNode getNthChild(int n) {
        return children.get(n);
    }

    public boolean needStore(Value sym) {
        SymbolInfo info = getInfo(sym);
        return info.needStore;
    }

    public boolean isJumpNode() {
        return tag == OP.if_jump || tag == OP.ifFalse_jump;
    }

    public boolean isRoot() {
        return parentCnt == 0;
    }

    public boolean isUsed(Value v) {
        SymbolInfo info = getInfo(v);
        return info.used();
    }

    public boolean canBeAssigned(Value v) {
        if (v instanceof Var var) {
            return isUsed(v);
        }
        return false;
    }

//    /*判断是否被使用过*/
//    private boolean hasBeenUsed(){
//        return !isRoot()||
//    }
//    private void getFirst

    public void setPropertiesOfChildren(boolean addParentCnt, boolean active) {
        if (children == null)
            return;
        boolean b = false;
        for (DagNode child : children) {/*更新父节点个数*/
            child.updateParentCnt(addParentCnt);
            if (addParentCnt) {
                if ((child instanceof Var var) && !b) {
                    b = true;
                }
            }
        }
    }

    /***
     * @param symbol 代表节点的值的value,可以为null
     */
    public static DagNode createInnerNode(DagTag tag, Location location, int ori_id, List<DagNode> children, Value symbol) {
        DagNode node = DagNode.getInstance(tag, symbol, location, ori_id);
        node.setChildren(children);
        node.location = location;
        return node;
    }

    public void updateParentCnt(boolean increase) {
        parentCnt += (increase ? 1 : -1);
        if (parentCnt < 0) {
            throw new RuntimeException("Should not happen!");
        }
    }

    public void addChild(DagNode child) {
        if (!children.contains(child))
            children.add(child);
    }


    int getDid() {
        return did;
    }

    int getOriIid(int n) {
        return symbolInfos.get(n).ori_id;
    }

    int getOriIid() {
        return oriId;
    }

    void setChildren(List<DagNode> children) {
        this.children = children;
        for (DagNode child : children) {
            child.parents.add(this);
//            if(tag==OP.lea){
//                child.getSymbols().stream()
//                        .filter(Var.class::isInstance)
//                        .map(Var.class::cast)
//                        .forEach(var -> addrUsedMap.put(var,child));
//            }
        }
    }

    public boolean isBuilt() {
        return built;
    }

    public boolean isIniNode() {
        return (this instanceof IniNode);
    }

    public boolean isConstNode() {
        return isIniNode() && (getSymbols().get(0) instanceof Literal);
    }

    public Literal getConstValue() {
        return (Literal) getSymbols().get(0);
    }

    public void setBuilt(boolean built) {
        this.built = built;
    }

    Value getArg(int n) {
        DagNode child = children.get(n);
        if (child.getSymbols().isEmpty()) {
            throw new RuntimeException();
        }
        return children.get(n).getFirstSymbol();
    }

    Value offerArg(int n){
        DagNode child = children.get(n);
        if (child.getSymbols().isEmpty()) {
            throw new RuntimeException();
        }
        return children.get(n).offerFirstSymbol();
    }

    public int getArgNum() {
        return children.size();
    }

    boolean addDependentsAddr(Value value) {
        return dependentsAddr.add(value);
    }

    public HashSet<Value> getDependentsAddr() {
        return dependentsAddr;
    }

    public void clear() {
        dependentsAddr.clear();
        setAlive(false);
//        removeAllSymbols();
    }

    /**
     * 设置附着在其上的所有顶点的最后定值节点为空
     */
    public void removeAllSymbols() {
        for (SymbolInfo info : symbolInfos) {
            var sym = info.symbol;
            sym.setAssignedDag(null);
            if (sym instanceof Var var) {
                var.setRecentDag(this);
            }
        }
    }


    /**
     * @return 是否为间接读取内存的节点
     */
    public boolean isMemReadNode() {
        return tag == Array_Assigning;
    }

    /**
     * @return 是否为间接写入内存的节点
     */
    public boolean isMemWriteNode() {
        return tag == ConstantTag.Array_Assigned;
    }

    protected boolean isIniNodeOf(Value v) {
        return false;
    }

    protected boolean isLastNodeOf(Value v) {
        DagNode last = v.getAssignedDag();
        return last != null && (this.equals(last));
    }


}

class IniNode extends DagNode {
    //本初始节点对应的value
    private Value value;
    //此结点的值是否仍有效,当变量被重新赋值时为无效
    private boolean valid = true;

    IniNode(Value symbol, Location location, int ori_id) {
        super(ConstantTag.INI, symbol, location, ori_id);
        this.value = symbol;
    }

    public Value getValue() {
        return value;
    }

    boolean containOtherSymbol() {
        return symbolInfos.stream()
                .map(info -> info.symbol)
                .anyMatch(sym -> !sym.equals(value));
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public boolean isValid() {
        return valid;
    }

    @Override
    protected boolean isIniNodeOf(Value v) {
        return value.equals(v);
    }

    @Override
    public boolean deserveAssign() {
        return super.deserveAssign()
                || containOtherSymbol();
    }

    @Override
    public boolean canBeAssigned(Value v) {
        return v != value;
    }


    public Value offerFirstSymbol(){
        return super.getFirstSymbol(true);
    }
}

class CallNode extends DagNode {
    private Callee callee;

    public CallNode(DagTag tag, Value symbol, Location location, int ori_id) {
        super(tag, symbol, location, ori_id);
        this.alive = false;
    }

    public void setCallee(Callee callee) {
        this.callee = callee;
    }

    public Callee getCallee() {
        return callee;
    }
}
