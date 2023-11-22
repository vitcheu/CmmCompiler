package AST.TYPE;

import AST.Callee;
import AST.DEFINE.ParamNode;
import AST.Node;
import IR.Constants.Type;
import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;
import CompileException.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class TypeNode extends Node implements Callee {
    private BaseType base;
    private PostfixNode postfix;
    private boolean signed;
    @Override
    public void accept(ASTVisitor ASTVisitor) throws CompileError {
        ASTVisitor.visit(this);
    }

    public TypeNode(Location location, BaseType base, PostfixNode postfix) {
        super(location);
        this.base = base;
        this.signed=base.signed();
        this.postfix = postfix;
    }

    public TypeNode(BaseType baseType) {
        super(baseType.getLocation());
        this.base = baseType;
        this.signed=base.signed();
        this.postfix = null;
    }


    public TypeNode(String s){
        super(null);
        this.base=new BaseType(s);
        this.signed=base.signed();
        this.postfix=null;
    }

    public TypeNode(String s,boolean signed){
        super(null);
        BaseType baseType =new BaseType(s,signed);
        iniBase(baseType,signed);
    }

    public TypeNode(BaseType base,List<PostfixNode> postfixNodes,boolean signed){
        super(null);
        iniBase(base,signed);
        this.postfix=createPostfixChain(postfixNodes);
    }

    /**
     * 创建后缀结点链表
     * @return 链表的头节点
     */
    private PostfixNode createPostfixChain(final List<PostfixNode> postfixNodes){
        PostfixNode head=null;
        if(!postfixNodes.isEmpty()){
            PostfixNode pre=null;
            for(PostfixNode node:postfixNodes) {
                PostfixNode newNode=PostfixNode.createPostfixNode(node);
                if(pre!=null){
                    pre.setNextPostfix(newNode);
                    pre.setHead(newNode.getHead());
                }else{
                    //为第一个节点
                    head=newNode;
                    newNode.setHead(newNode);
                }

                pre=newNode;
            }
        }

        return head;
    }

    protected TypeNode clone(){
        TypeNode type=new TypeNode(this.base,this.getPostfixes(),this.signed);
        return type;
    }

    private void iniBase(BaseType base, boolean signed){
        this.base=base;
        this.signed=signed;
    }

    @Override
    public void dump(int level) {
//        dumpHead("Type",level);
        ident(level, this.toString());
//        dumpEnd("Type",level);
    }

    /**
     * 判断类型是否为支持的类型
     * 不支持的类型有:void的指针或数组,返回结构体或联合体的函数
     */
    public  boolean isValidated() {
        //基本类型总是有效的
        if (postfix == null) return true;
        //void的数组类型无效
        if (base == BaseType.voidType
                &&(postfix instanceof PtrPofix ptrPofix)&&ptrPofix.isArrayPostfix()) {
            return false;
        }
        //为不合法的函数类型
        if (base instanceof ComposedType && postfix instanceof ParamPofix) {
            return false;
        }
        return true;
    }

    /**
     * 判断类型是否可用于变量的类型声明
     */
    public static boolean isValidateDeclaration(TypeNode type) {
        //有效且非void
        return type.isValidated() &&
                !type.isType(BaseType.VOID);
    }

    public boolean isBaseTYpe() {
        return postfix == null;
    }

    public boolean isPrimaryType(){
        return postfix==null&&!(isComposedType());
    }

    public BaseType getBase() {
        return base;
    }

    public PostfixNode getPostfix() {
        return postfix;
    }

    private String getPostfixStr() {
        if(postfix==null){
            return "";
        }
        StringBuilder sb = new StringBuilder();
        PostfixNode curNode = postfix;
        while (curNode != null) {
            sb.append(curNode + " ");
            curNode = curNode.getNextPostfix();
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        String signedStr=(signed?"":"u");
        return signedStr+ base + getPostfixStr();
    }

    /**
     * 判断该类型是否为指针或数组
     */
    public boolean isPointerOrArr() {
        return !isBaseTYpe();
    }

    public boolean isPointerType(){
        PostfixNode last=getLastPostfix();
        return !isBaseTYpe()&&
                last.isPtrPostfix()&& !last.isArrayPostfix();
    }

    public boolean isComposedType() {
        return isBaseTYpe() && (base instanceof ComposedType);
    }

    public boolean isArrayType(){
        PostfixNode last=getLastPostfix();
        return !isBaseTYpe()&&
                last.isPtrPostfix()&&last.isArrayPostfix();
    }

    /**
     *增加新类型后缀,使得间接层次增加一级
     */
    public void addNewPostfix(){
        PostfixNode last=getLastPostfix();
        PostfixNode newPostfix=new PtrPofix();
        if(last!=null){
            last.setNextPostfix(newPostfix);
            newPostfix.setHead(last.getHead());
        }else{
            newPostfix.setHead(newPostfix);
            setPostfix(newPostfix);
        }
    }


    /**
     * 判断是否为函数指针
     */
    public boolean isFunctionType() {
        PostfixNode cur = postfix;
        while (cur != null) {
            if (cur instanceof ParamPofix)
                return true;
            cur=cur.getNextPostfix();
        }
        return false;
    }

    /**
     * 获得指针或数组指向的类型
     * 不是指针类型则返回null
     * 如果是void*指针,返回char
     * 如int[3]* 返回int[3]
     */
    public TypeNode getPointerBaseType() {
        LinkedList<PostfixNode> postfixNodes=new LinkedList<>(getPostfixes());
        postfixNodes.removeLast();
        TypeNode result=new  TypeNode(this.base,postfixNodes,signed);
        if(result.isType(BaseType.VOID)){
            return new TypeNode(BaseType.charType);
        }else{
            return result;
        }
    }

    /**
     * 返回数组直接索引的类型
     * 如int****返回int***,int[2][3]*[4][5]返回int[2][3]*[5],
     */
    public TypeNode getArrayDirectIndex() {
        if (isFunctionType()) return this;
        List<PostfixNode> postfixNodes = getPostfixes();
        LinkedList<PostfixNode> stack = new LinkedList<>();
        boolean flag = false;
        for (int i = postfixNodes.size() - 1; i >= 0; i--) {
            PostfixNode node = getNthPostfix(i);
            //当flag为true或者处于刚开始的连续的数组后缀块时,照搬节点
            if (flag|| node.isArrayPostfix()) {
                stack.addLast(node);
            }
            //设置flag为false,表示以后可以照搬节点,并去除栈中上一个数组后缀
            else{
                if(!stack.isEmpty())
                    stack.removeLast();
                stack.addLast(node);
                flag=true;
            }
        }
        List<PostfixNode> nodes=new LinkedList<>();
        if(!flag){
            stack.removeLast();
        }
        while (!stack.isEmpty()){
            nodes.add(stack.removeLast());
        }
        return new TypeNode(getBase(),nodes,signed);
    }

    /**
     * @return 指针或数组类型所直接索引的对象类型
     */
    public TypeNode getDirectReferenceType(){
        if(isArray())return getArrayDirectIndex();
        else return getPointerBaseType();
    }

    /**
     *获取多维数组的元素类型
     * 如int[3][4]*[5][6]的输出为int[3][4]*,是一个指针类型
     */
    public TypeNode getArrayBaseType(){
        if (!isArrayType())
            throw  new RuntimeException("访问对象非法! "+this+"不是数组类型");
        List<PostfixNode> postfixNodes = getPostfixes();
        LinkedList<PostfixNode> stack = new LinkedList<>();
        boolean flag = false;
        for (int i = postfixNodes.size() - 1; i >= 0; i--) {
            PostfixNode node = getNthPostfix(i);
            //当flag为true,照搬节点
            if (flag) {
                stack.addLast(node);
            }
            //过滤掉末尾的数组式后缀
            else if(node.isArrayPostfix()){
                ;
            }
            //设置flag为false,表示以后可以照搬节点
            else{
                if(!stack.isEmpty())
                    stack.removeLast();
                stack.addLast(node);
                flag=true;
            }
        }
        List<PostfixNode> nodes=new LinkedList<>();
        while (!stack.isEmpty()){
            nodes.add(stack.removeLast());
        }
        return new TypeNode(getBase(),nodes,signed);
    }

    /**
     * 判断该节点是否是数类型的
     * 若属于int,float,bool,char,返回true
     * 否则返回false
     */
    public boolean isNum(){
        if(isPointerOrArr()) return false;
        if(base.isIntType()||base.isFloatType()||base.equals(BaseType.charType)
            ||base.equals(BaseType.boolType)){
            return true;
        }
        return false;
    }

    /**
     * 获取解引用类型,亦即一维数组访问的类型
     */
    public TypeNode getDerefType(){
        //指针
        if(isPointerType()){
            return getPointerBaseType();
        }else{
            return getArrayDirectIndex();
        }
    }

    /**
     * 创建对该类型变量取地址运算后的类型
     */
    public TypeNode getAddressOpType(){
        return createPointer();
    }

    /**
     * 判断是否为函数指针
     */
    public boolean isFunctionPointer(){
        return  isPointerType()&& getPointerBaseType().isFunctionType();
    }


    /**
     * 判断该类型是否是int兼容类
     */
    public boolean isIntCompatible(){
        if(isPointerOrArr()) return false;
        if(base.isIntType()||isType(BaseType.CHAR)
                ||isType(BaseType.BOOL)){
            return true;
        }
        return false;
    }

    /**
     * 判断是否为指定的基本类型
     */
    public boolean isType(String s){
        String baseStr=base.getType();
        return !isPointerOrArr()&&
           baseStr.equals(s)
                || baseStr.equals(BaseType.LONG_LONG)&&s.equals(BaseType.LONG)
                || baseStr.equals(BaseType.LONG)&&s.equals(BaseType.LONG_LONG);
    }

    /**
     *判断是否为字符串类型(char *,char[])
     */
    public boolean isStrType(){
        return isPointerOrArr()&& getPointerBaseType().isType(BaseType.CHAR);
    }


    public boolean isType(String s,boolean signed){
        return isType(s)&&signed==this.getBase().signed();
    }

    /**
     * 创建指向本类型的指针
     */
    public final TypeNode createPointer(){
        //指向函数的指针依然为函数
        if(isFunctionPointer()) return this;
        TypeNode type=this.clone();
        type.addNewPostfix();
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TypeNode typeNode = (TypeNode) o;
        boolean bastTypeEqual=this.getBase().equals(typeNode.getBase());
        if(!bastTypeEqual)
            return false;
        if(isBaseTYpe()&&typeNode.isBaseTYpe()&&bastTypeEqual){
            return true;
        }
        return Objects.equals(this.getDescription(), typeNode.getDescription());
    }

    public String getDescription(){
        String str=" ";
        if(postfix!=null){
            str= postfix.getDescription();
        }
        return base.getDescription()+str;
    }

    @Override
    public int hashCode() {
        return Objects.hash(base, postfix);
    }

    /**
     * 判断是否为void类型
     */
    public boolean isVoid(){
        return postfix==null&&base.equals(BaseType.voidType);
    }

    /**
     *计算数组的平展长度,即数组可储存的元素长度
     */
    public int getTotalLen(){
        if(isBaseTYpe()) {
           return 1;
        }
        int product=1;
        PostfixNode cur=postfix;
        while (cur!=null){
           PtrPofix arr=(PtrPofix) cur;
            //表示一个指针
           if(!arr.isArrayPostfix()){
            //乘积重置为1
               product=1;
           }else{
               product*=arr.getArrayLen();
           }

           cur=cur.getNextPostfix();
        }
        return product;
    }

    /**
     * 获取数组的长度
     */
    public int getArrayLen(){
        PtrPofix ptrPofix= (PtrPofix) getLastPostfix();
        if(!isArray()||ptrPofix==null){
            throw new RuntimeException("should not happened");
        }
        int len=ptrPofix.getArrayLen();
        return len>0?len:0;
    }


    /**
     * @return 判断是否为数组类型
     */
    public boolean isArray(){
        PostfixNode lastPostfix=getLastPostfix();
        if (lastPostfix == null                //为基本类型
                || !lastPostfix.isPtrPostfix()   //为函数参数后缀
                || !((lastPostfix).isArrayPostfix()) //为指针后缀
        )
            return false;
        return true;
    }

    private void setPostfix(PostfixNode postfix) {
        this.postfix = postfix;
    }

    /**
     *数组转换为指针
     * int[3][4]的输出为int[4]*
     */
    public TypeNode arrToPointer(){
        if(!isArray()) return this;
        TypeNode base=getArrayDirectIndex();
        base.setLocation(location);
        return base.createPointer();
    }


    /**
     *获取从左到右的第n个(从零开始)后缀
     */
    private PostfixNode getNthPostfix(int n){
        if(n>=getPostfixNum()){
            throw new IndexOutOfBoundsException();
        }
        PostfixNode cur=postfix;
        int i=0;
        while (cur!=null){
            if(i==n){
                return cur;
            }
            i++;
            cur=cur.getNextPostfix();
        }
        return null;
    }

    private List<PostfixNode> getPostfixes(){
        PostfixNode cur=postfix;
        List<PostfixNode> postfixNodes=new LinkedList<>();
        while (cur!=null){
            postfixNodes.add(cur);
            cur=cur.getNextPostfix();
        }
        return postfixNodes;
    }

    private int getPostfixNum(){
        PostfixNode cur=postfix;
        int n=0;
        while (cur!=null){
            n++;
            cur=cur.getNextPostfix();
        }
        return n;
    }

    /**
     * 返回最后一个后缀节点,如果没有后缀节点,返回null
     */
    private PostfixNode getLastPostfix(){
        int n=getPostfixNum();
        if(n==0){
            return null;
        }else{
            return getNthPostfix(n-1);
        }
    }

   public static TypeNode createFunctionType(TypeNode ret,ParamPofix param){
        List<PostfixNode> postfixNodes=ret.getPostfixes();
        postfixNodes.add(param);
        return new TypeNode(ret.base,postfixNodes,ret.base.signed());
    }

    /**
     * 设置最后的后缀节点
     */
    public void setLastPostfix(PostfixNode node){
        PostfixNode cur=node,parent=null;
        while (cur!=null){
            parent=cur;
            cur=cur.getNextPostfix();
        }
        if(parent!=null){
            parent.setNextPostfix(node);
            node.setHead(parent.getHead());
        }else{
            this.postfix=node;
            node.setHead(node);
        }
    }

    /**
     * 形参从数组转换为指针
     */
    public void  paramArrTransformToPointer(){
        PostfixNode last=getLastPostfix();
        if(last!=null||!last.isPtrPostfix()){
            PtrPofix pofix=(PtrPofix) last;
            pofix.shiftToPtrPostfix();
        }
    }

    public int getWidth(){
        return Type.TypeNodeToIRType(this).getWidth();
    }

    public boolean isFloatType(){
        return isPrimaryType()&& getBase().isFloatType();
    }

    public boolean isIntType(){
        return isPrimaryType()&& getBase().isIntType();
    }

    /**
     * 判断整型类型的优先级
     */
    public int compare(TypeNode type){
        BaseType base=type.getBase();
        if(this.getWidth()!=type.getWidth()){
            return Integer.compare(this.getWidth(),type.getWidth());
        }else{
            if(!this.base.signed()&&base.signed()) return 1;
            else if(this.base.signed()&&!base.signed()) return -1;
            return 0;
        }
    }

    public boolean isSigned() {
        return signed;
    }

    public void setSigned(boolean signed) {
        this.signed = signed;
    }

    /**
     * 判断是否是void*类型的指针
     */
    public boolean isVoidPtr(){
        return base.equals(BaseType.voidType)&&(postfix!=null)
                &&(postfix.getNextPostfix()==null)
                &&(postfix.isPtrPostfix());
    }

    /**
     * 获取指针或数组类型的间接级别
     */
    public int getIndirectLevel(){
        if(postfix==null) return 0;
        return getPostfixes().size();
    }

    private static boolean isCompatible(TypeNode lt ,TypeNode rt){
        if (lt.isIntCompatible() && !rt.isIntCompatible()
                || !lt.isIntCompatible() && rt.isIntCompatible())
            return false;
        if(lt.isFloatType()&&!rt.isFloatType()
            ||!lt.isFloatType()&&rt.isFloatType())
            return false;
        if(lt.isPointerOrArr()&& !rt.isPointerOrArr()
            ||!lt.isPointerOrArr()&&rt.isPointerOrArr())
            return false;
        return true;
    }

    /**
     * 判断基类型是否兼容,分为浮点数,整数,指针三个等价类
     */
    public boolean isCompatibleWith(TypeNode typeNode){
        TypeNode lt=new TypeNode(this.base.toString(),this.signed);
        TypeNode rt=new TypeNode(typeNode.getBase().toString(),typeNode.signed);
        return isCompatible(lt,rt);
    }

    /**
     * 将自定义类型更改为实际类型
     * @param realBaseType 该类型实际引用的基类型
     */
    public void shiftToRealType(TypeNode realBaseType) {
        /*修改baseType*/
        copyBase(realBaseType);

        LinkedList<PostfixNode> postfixes;
        /*需要改变数组后缀的位置*/
        if(realBaseType.isArray()&&this.getNthPostfix(0).isArrayPostfix()){
            postfixes=new LinkedList<>();
            LinkedList<PtrPofix> arr1=new LinkedList<>(),arr2=new LinkedList<>();
            PostfixNode curPostfix;
            int n=realBaseType.getPostfixNum(),
                    i=n-1;
            boolean finding=true;

            /*从右到扫描自定义类型的实际类型,找出后面所有的数组后缀,直至遇到非数组后缀或结束*/
            while (i>=0){
                curPostfix=realBaseType.getNthPostfix(i);
                if(!finding){
                    postfixes.push(curPostfix);
                }
                else if (curPostfix.isArrayPostfix()) {
                    arr1.push((PtrPofix) curPostfix);
                }else{
                    finding=false;
                }
                i--;
            }

            n=this.getPostfixNum();
            /*从左到右扫描自定义类型后的后缀结点,找出前面的所有直接数组后缀*/
            i=0;
            finding=true;
            while (i < n) {
                curPostfix =this.getNthPostfix(i);
                if (finding && curPostfix.isArrayPostfix()) {
                    arr2.add((PtrPofix) curPostfix);
                } else if (finding && !curPostfix.isArrayPostfix()) {
                    finding = false;
                    /*调换数组后缀的顺序*/
                    postfixes.addAll(arr2);
                    postfixes.addAll(arr1);
                    /*加上本层节点*/
                    postfixes.addLast(curPostfix);
                } else {
                    postfixes.addLast(curPostfix);
                }

                i++;
            }
        }
        /*直接将两者的后缀相加*/
        else{
            postfixes=new LinkedList<>(realBaseType.getPostfixes());
            postfixes.addAll(this.getPostfixes());
        }

        this.postfix=createPostfixChain(postfixes);
    }

    private void copyBase(TypeNode typeNode){
        this.base= typeNode.getBase();
        this.signed= typeNode.signed;
    }

    public void copyFrom(TypeNode typeNode){
        copyBase(typeNode);
        List<PostfixNode> postfixNodes=typeNode.getPostfixes();
        this.postfix=createPostfixChain(postfixNodes);
    }


    @Override
    public List<ParamNode> getParams() {
        ParamPofix paramPofix=(ParamPofix) postfix;
        final List<ParamNode> params = paramPofix.getParams();
        if(paramPofix==null||isVoidParam(params))
            return new LinkedList<>();
        return params;
    }

    @Override
    public TypeNode getType() {
        return new TypeNode(base);
    }
}
