package AST.DEFINE;

import AST.EXPR.VariableNode;
import AST.TYPE.ComposedType;
import AST.TYPE.TypeNode;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

//结构体或联合体的变量,保存有成员变量的引用
public class definedComposedVariable extends DefinedVariable {
    //储存成员变量的符号表
    private HashMap<String,VariableNode> memberVariables=new LinkedHashMap<>();

//    public definedComposedVariable(Location location, String id, definedComposedType definedComposedVariable) {
//        super(location, id);
//        setMemberVariables(definedComposedVariable);
//    }
//
//    public definedComposedVariable(VariableNode var){
//        super(var.getLocation(),var.getId());
//    }


//    public definedComposedVariable(Location location, boolean priv, TypeNode type, String name) {
//        super(location, priv, type, name, null);
//    }

    public definedComposedVariable(DefinedVariable defvar){
        super(defvar.getLocation(),defvar.priv,defvar.type,defvar.name,null);
    }

    public void setMemberVariables(definedComposedType definedComposedType) {
        List<SlotNode> slotNodes=definedComposedType.getMembers();
        if(slotNodes!=null){
            for(SlotNode member:slotNodes){
                VariableNode var=new VariableNode(member.getType(),member.getName());
                memberVariables.put(var.getId(),var);
            }
        }
    }

    @Override
    public String toString() {
//        StringBuilder sb=new StringBuilder ("DefVar@" +name+"#"+type+location+"{\n");
//        for(String str:memberVariables.keySet()){
//            VariableNode var=memberVariables.get(str);
//            sb.append(var.getType()+":"+var.getId()+"\n");
//        }
//        sb.append("}\n");
//        return sb.toString();
        return super.toString();
    }

    /**
     * 根据id查找成员
     * @return 给定id的成员变量,无该成员则返回null
     */
    public VariableNode getMember(String id){
        return memberVariables.get(id);
    }

    /**
     * 查找第n个成员的类型
     */
    public TypeNode getNthMemberType(int n){
        ComposedType composedType=(ComposedType) (getType().getBase());
        definedComposedType definedComposedType=composedType.getTypeEntry();
        return definedComposedType.getNthMember(n).getType();
    }
}
