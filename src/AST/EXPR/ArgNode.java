package AST.EXPR;

import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;
import CompileException.*;

import java.util.LinkedList;
import java.util.List;

public class ArgNode extends ExprNode{
    List<ExprNode> args=new LinkedList<>();

    public ArgNode(Location location) {
        super(location);
    }

    @Override
    public void accept(ASTVisitor ASTVisitor)throws CompileError {
        ASTVisitor.visit(this);
    }

    public void addArgument(ExprNode arg){
        args.add(arg);
    }


    @Override
    public void dump(int level) {
        if(args.isEmpty()){
            ident(level,"void");
        }
        for(ExprNode arg:args){
            dumpHead("argument",level);
                arg.dump(level+1);
            dumpEnd("argument",level);
        }
    }

    public List<ExprNode> getArgs() {
        return args;
    }
}
