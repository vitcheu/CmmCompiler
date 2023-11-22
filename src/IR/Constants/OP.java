package IR.Constants;

import IR.Optimise.DagTag;

import java.util.*;

public enum OP implements DagTag {
    //取反
    minus,
    //按位取反
    neg,
    //加
    add,
    //减
    sub,
    //乘
    mul,
    //除
    div,
    //取模
    mod,
    //左移
    shift_left,
    //右移
    shift_right,
    //与
    and,
    //或
    or,
    //异或
    xor,
    //非
    not,
    //无条件跳转
    jump,
    //取地址
    lea,
    //解引用
    deref,
    //赋值
    assign,
    //操作数为真则跳转
    if_jump,
    //操作数为假则跳转
    ifFalse_jump,
    //过程返回指令
    ret,
    //过程调用传参指令
    param,
    //函数调用指令
    call,
    //带下标的赋值指令,如t2=a[t1]
    array,
    //类型转换
    cast,
    //进入和离开函数
    enter,
    leave,
    //退出程序
    exit,
    /**
     * 布尔运算
     */
    //相等
    eq,
    //不相等
    ne,
    //大于等于
    ge,
    //小于等于
    le,
    //大于
    gt,
    //小于
    lt;

    private static List<OP> binary= Arrays.asList(new OP[]{add, mul, sub, div, mod, shift_left, shift_right,
            and, or, xor,eq,ne,ge,le,gt,lt});
    private static List<OP> unary=Arrays.asList(new OP[]{cast,minus,neg,not,lea,deref,call,enter});
    private static List<OP> noneOperand=Arrays.asList(new OP[]{exit,leave});

    private static HashMap<OP,String> strings=new LinkedHashMap<>();
    static {
        strings.put(minus,"-");
        strings.put(neg,"~");
        strings.put(add,"+");
        strings.put(sub,"-");
        strings.put(mul,"*");
        strings.put(div,"/");
        strings.put(mod,"mode");
        strings.put(shift_left,"<<");
        strings.put(shift_right,">>");
        strings.put(and,"and");
        strings.put(and,"or");
        strings.put(xor,"xor");
        strings.put(eq,"==");
        strings.put(ne,"!=");
        strings.put(ge,">=");
        strings.put(le,"<=");
        strings.put(gt,">");
        strings.put(lt,"<");
        strings.put(deref,"*");
        strings.put(lea,"&");

    }

    public static boolean isBinary(OP op){
            return binary.contains(op);
    }

    public static boolean isUnary(OP op){
        return unary.contains(op);
    }

    public static boolean isNoneOperand(OP op){return noneOperand.contains(op);}

    public static String getStr(OP op){
        if(strings.containsKey(op))
            return strings.get(op);
        else return op.toString();
    }

    public static boolean exchangeable(OP op){
        return op==add||op==mul||op==eq||op==ne||op==and||op==or||op==xor;
    }

    @Override
    public String getDescriptionStr() {
        return getStr(this);
    }
}
