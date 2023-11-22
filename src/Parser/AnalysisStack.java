package Parser;

import AST.Node;
import Parser.Entity.*;
import compile.Constants;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

public class AnalysisStack {
    private LinkedList<Integer> stack = new LinkedList<>();
    private LinkedList<Symbol> content = new LinkedList<>();
    private AnalysisTable table;
    private PrintWriter err;
    private Reducer reducer;
    private Node preNode;

    public AnalysisStack(AnalysisTable table, PrintWriter err) throws FileNotFoundException {
        this.table = table;
        this.err = err;
    }

    public void setReducer(Reducer reducer) {
        this.reducer = reducer;
    }

    /**
     * 使记录栈的内容的content能跟踪栈的变化,当移进一个终结符号时,content增加该符号
     *
     * @param i 分析栈中压入的状态号
     */
    public void push(int i, Symbol s) {
        stack.push(i);
        content.addLast(s);
    }

    /**
     * 根据p规约,修改栈中的内容
     */
    public void reduced(Production p) {
        int colOffSet = Constants.numOfTerminal;
        //执行对应的动作,获得抽象语法树节点
         Node node;
         try{
             node = reducer.reduce(p);
         }catch (ClassCastException e){
             throw new RuntimeException("\n"+p+" 归结错误,产生式号:"+p.getPid());
         }
        try {
//            if (node != null &&
//                    p.getPid() < 90) {
//                reducer.pw.println("\n------------------------------------------------------");
//                reducer.pw.println("规约项:\t#" + p.getPid() + "\t$" + p);
//                reducer.pw.println("得到节点:" + node);
//                if (node instanceof StmtNode &&
//                        (preNode == null || !preNode.equals(node))) {
//                    Node.dumpWriter.println("\n------------------------------------------------------");
//
//                    StmtNode SNode = (StmtNode) node;
//                    SNode.dump(0);
//
//                    Node.dumpWriter.println("------------------------------------------------------");
//                }
//            }
//            reducer.pw.println("\n获得新节点:" + node);
            preNode = node;
            //弹出栈的内容
            for (int i = 0; i < p.getRight().size(); i++) {
                pop();
            }

            //获取规约所得的非终结符
            Nonterminal nt = new Nonterminal(p.getLeft());
            //保存生成的新node类
            nt.setNode(node);
            //转到新状态
            int curState = stack.peek();
            int newState = table.getAction(curState, nt);
            push(newState, nt);
        } catch (Exception e) {
            Node.closeDumping();
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    /**
     * 语法分析栈的弹出
     */
    public void pop() {
        if (stack.isEmpty()) {
            err.println("语法错误,分析栈已空!");
        }
        stack.pop();
        content.removeLast();
    }

    public int peek() {
        return stack.peek();
    }

    public Symbol getSymbol(int offset) {
        if (stack.size() < offset) {
            throw new RuntimeException("访问栈的位置错误,size=" + stack.size() + "," +
                    "offset=" + offset);
        }
        return content.get(stack.size() - 1 - offset);
    }

    public Node getNode(int offset) {
        Nonterminal nt = (Nonterminal) getSymbol(offset);
        return nt.getNode();
    }


    public int size() {
        return stack.size();
    }

    @Override
    public String toString() {
        return content.toString();
    }
}
