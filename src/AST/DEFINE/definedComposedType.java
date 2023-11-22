package AST.DEFINE;

import AST.NodeList.ListNode;
import AST.TYPE.TypeNode;
import Parser.Entity.Location;
import utils.Align;
import utils.CalculateTypeWidth;

import java.util.LinkedList;
import java.util.List;

public class definedComposedType extends definedUserType {
    private static final int default_maxField_size=-1;
    private static final int default_width=-1;
    private List<Integer> offsets=new LinkedList<>();
    protected ListNode<SlotNode> members;
    private int width=default_width;
    int maxFieldSize=default_maxField_size;

    public definedComposedType(Location location, String name, ListNode<SlotNode> members) {
        super(location,name);
        this.members = members;
    }


    public List<SlotNode> getMembers() {
        if(members.getNodeList()==null) return new LinkedList<>();
        else
            return members.getNodeList();
    }

    @Override
    public String toString() {
        String s;
        if(this  instanceof definedStruct){
            s="Struct";
        }else s="Union";
        return  s+" "+typeName+
                location+
                "{" +
                "members=" + members +
                '}';
    }

//    public boolean hasMember(String name){
//        for(SlotNode member:members.getNodeList()){
//            if(member.getName().equals(name)){
//                return true;
//            }
//        }
//        return false;
//    }

    public SlotNode getMember(String name){
        for(SlotNode member:members.getNodeList()){
            if(member.getName().equals(name)){
                return member;
            }
        }
        return null;
    }

    public SlotNode getNthMember(int n){
        return members.getNodeList().get(n);
    }

    public int getTypeWidth(){
        if(width==default_width){
            fillOffsets();
        }
        return width;
    }

    private void fillOffsets(){
        if(width==default_width) {
            int offset=0;
            int max=Integer.MIN_VALUE;
            for(SlotNode slot:members.getNodeList()){
                TypeNode type=slot.getType();
                int width=CalculateTypeWidth.getTypeWidth(type);
                /*MS ABI规定,成员起始地址到结构体起始地址的长度应为成员类型宽度的整数倍*/
                offset= Align.align(offset,Integer.min(16,width));
                offsets.add(offset);
                offset+=width;

                max=Integer.max(max,width);
            }
            if(maxFieldSize==default_maxField_size&&(max!=Integer.MIN_VALUE)){
                /*MS ABI要求,最大字段宽度至多为16B*/
                maxFieldSize=Integer.min(16,max);
            }
            /*MS ABI规定,结构体的宽度应为最宽字段的整数倍*/
            if(maxFieldSize!=default_maxField_size){
                offset=Align.align(offset,maxFieldSize);
            }

            width=isStruct()?offset:max;
        }
    }

    public int getOffsetOfMember(SlotNode member){
        int n=members.getNodeList().indexOf(member);
        if(n==-1){
            throw  new RuntimeException("should not happened");
        }
        return getOffsetOfMember(n);
    }


    public int getOffsetOfMember(int n){
        if(width==default_width){
            //填充
           fillOffsets();
        }
        return getOffset(n);
    }

    public int getMaxFieldSize() {
        return maxFieldSize;
    }

    private int  getOffset(int n){
        return isStruct()?offsets.get(n):0;
    }

    public boolean isStruct(){
       return this instanceof definedStruct;
    }

    public String getMemoryArragingDescription(){
        if(width==default_width){
            fillOffsets();
        }
        StringBuilder builder=new StringBuilder();
        builder.append(String.format("----------------<%s的内存布局>-------------------\n",typeName));
        builder.append("定义位置:"+getLocation()+"\n");
        builder.append(String.format("%-8s\t%-8s\t%-6s\n","字段名","类型","偏移量"));
        int w=-1,preOffset=-1;
        for(int i=0;i<members.getNodeList().size();i++){
            int offset=getOffset(i);

            if(preOffset!=-1&&(preOffset+w)!=offset){
                builder.append(String.format("#\t\t\tpadding-(form %d)\n",preOffset+w));
            }

            SlotNode slot=members.getNodeList().get(i);
            builder.append(String.format("%-10s\t%-10s\t%-8d\n",slot.getName(),slot.getType(),offset));

            preOffset=offset;
            w=CalculateTypeWidth.getTypeWidth(slot.getType());
        }
        builder.append("总宽度:"+width+"\n");
        builder.append(String.format("----------------</%s的内存布局>------------------\n",typeName));
        return builder.toString();
    }
}
