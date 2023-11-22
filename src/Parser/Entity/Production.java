package Parser.Entity;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Production {
    private static int id=0;
    private Nonterminal left;
    private List<Symbol> right;
    private  int pid;

    public Production(Nonterminal left, List<Symbol> right){
        this.left=left;
        this.right=right;
        this.pid=id++;
    }

    public String[] getRightStr(){
        StringBuilder stringBuilder=new StringBuilder();
        for(Symbol t:right){
            stringBuilder.append(t.toString()+" ");
        }
        String[] result=stringBuilder.toString().split(" ");
        return result;
    }

    public Nonterminal getLeft() {
        return left;
    }

    public List<Symbol> subList(int begin, int end){
        List<Symbol> result=new LinkedList<>();
        if(end<begin) throw new IndexOutOfBoundsException("end<begin!end="+end+",begin="+begin);
        for(int i=begin;i<end;i++){
            result.add(right.get(i));
        }
        return result;
    }

    public boolean isEpsilon(){
        return right.size()==0;
    }

    public Symbol getHeader(){
        if(right.size()>=1)
        return right.get(0);
        else return null;//为空产生式
    }

    public List<Symbol> getAlpha(){
        return new ArrayList<>(right.subList(1,right.size()));
    }


    public List<Symbol> getRight() {
        return right;
    }

    public int getPid() {
        return pid;
    }

    @Override
    public String toString() {
        StringBuilder sb=new StringBuilder(left+"\t->\t");
        for(Symbol t:right){
            sb.append(t.toString()+"\t");
        }
        return sb.toString();
    }
    public String toCsvString(){
        StringBuilder sb=new StringBuilder(left+"\t->\t");
        for(Symbol t:right){
            sb.append(t.toString()+" ");
        }
        return sb.toString();
    }
    public String toNotapString(){
        StringBuilder sb=new StringBuilder(left+"-> ");
        for(Symbol t:right){
            sb.append(t.toString()+" ");
        }
        return sb.toString();
    }


}
