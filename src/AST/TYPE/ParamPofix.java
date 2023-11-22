package AST.TYPE;

import AST.DEFINE.ParamNode;
import AST.NodeList.ListNode;
import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;

import java.util.List;

public class ParamPofix extends PostfixNode {
    private List<ParamNode> params;

    public ParamPofix(Location location, List<ParamNode> params) {
        super(location);
        this.params = params;
    }

    public static ParamPofix copyParamPostfix(ParamPofix postfix){
       return new ParamPofix(postfix.location,postfix.getParams());
    }


    @Override
    public String toString() {
        return getDescription(true);
    }

    @Override
    public void accept(ASTVisitor ASTVisitor) {
        ASTVisitor.visit(this);
    }

    private String getDescription(boolean needName){
        if(params==null|| params.isEmpty()){
            return "(void)";
        }
        StringBuilder sb=new StringBuilder("(");
        for(ParamNode paramNode :params){
            String str=needName?paramNode.toString():
                    paramNode.getType().getDescription();
            sb.append(str+",");
        }
        sb.delete(sb.length()-1,sb.length());
        sb.append(")");
        return sb.toString();
    }

    @Override
    public String getDescription() {
       return getDescription(false);
    }

    public List<ParamNode> getParams() {
        return params;
    }
}
