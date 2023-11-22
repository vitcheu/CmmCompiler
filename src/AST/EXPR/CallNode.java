package AST.EXPR;

import AST.DEFINE.DefinedFunction;
import IR.Value;
import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;
import CompileException.*;

import java.util.LinkedList;
import java.util.List;

public class CallNode extends ExprNode{
    private ExprNode expr;
    private ArgNode argNode;
    private DefinedFunction entry;
    private List<Value> args=new LinkedList<>();

    public CallNode(Location location, ExprNode expr, ArgNode argNode) {
        super(location);
        this.expr = expr;
        this.argNode = argNode;
    }

    @Override
    public void accept(ASTVisitor ASTVisitor) throws CompileError {
        ASTVisitor.visit(this);
    }

    @Override
    public void dump(int level) {
        dumpHead("Call",level);
            ident(level+1,"Caller:");
                expr.dump(level+2);

            ident(level+1,"ArgumentList:");
                argNode.dump(level+2);
        dumpEnd("Call",level);
    }

    public ExprNode getExpr() {
        return expr;
    }

    public ArgNode getArgNode() {
        return argNode;
    }

    @Override
    public String toString() {
        return "CallNode{" +
                "location=" + location +
                '}';
    }

    public DefinedFunction getEntry() {
        return entry;
    }

    public void setEntry(DefinedFunction entry) {
        this.entry = entry;
    }

    public List<Value> getArgs() {
        return args;
    }

    public void addArg(Value value){
        args.add(value);
    }
}
