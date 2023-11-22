package AST.NodeList;

import AST.Node;
import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;

import java.util.LinkedList;
import java.util.List;

public class ListNode<E  extends  Node> extends Node{
    LinkedList<E> nodeList=new LinkedList<>();

    public ListNode(Location location) {
        super(location);
    }

    @Override
    public void accept(ASTVisitor ASTVisitor) {

    }

    public List<E> getNodeList() {
        return nodeList;
    }

    public void addNode(E node){
        nodeList.add(node);
        if(getLocation()==null&&node.getLocation()!=null){
            setLocation(node.getLocation());
        }
    }

    public void push(E node){
        nodeList.push(node);
    }

    public void addFirst(E node){
        nodeList.add(0,node);
        if(getLocation()==null&&node.getLocation()!=null){
            setLocation(node.getLocation());
        }
    }

    public void addAllNode(ListNode<E> nlist){
        for(E n:nlist.getNodeList()){
            addNode(n);
        }
    }

    @Override
    public void dump(int level) {
       for(E node:nodeList){
           node.dump(level);
       }
    }

    @Override
    public String toString() {
        return nodeList.toString();
    }
}
