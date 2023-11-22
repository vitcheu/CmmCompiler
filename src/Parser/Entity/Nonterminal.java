package Parser.Entity;

import AST.Node;

public class Nonterminal extends Symbol {
    private Node node;
    public static  int nid=0;
    public Nonterminal(String value) {
        super(value);
        setId(nid++);
    }

    /**
     *赋值构造函数
     */
    public Nonterminal(Nonterminal nt){
        super(nt.getValue());
        setId(nt.getId());
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public void setValue(String v){
        super.setValue(v);
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

}
