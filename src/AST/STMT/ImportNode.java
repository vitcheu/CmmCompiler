package AST.STMT;

import AST.EXPR.NameNode;
import AST.Node;
import AST.NodeList.ListNode;
import compile.Constants;
import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;
import CompileException.*;

import java.io.File;

public class ImportNode extends Node {
    ListNode<NameNode> path;

    public ImportNode(Location location, ListNode<NameNode> path) {
        super(location);
        this.path = path;
    }

    @Override
    public void accept(ASTVisitor ASTVisitor)throws CompileError {
        ASTVisitor.visit(this);
    }

    @Override
    public void dump(int level) {
        StringBuilder sb=new StringBuilder("import ");
        for(NameNode n:path.getNodeList()){
            sb.append(n.toString()+".");
        }
        sb.delete(sb.length()-1,sb.length());
        ident(level,sb.toString());
    }

    public String getPath() {
        StringBuilder builder=new StringBuilder();
        path.getNodeList().forEach(nameNode -> {
            String name =nameNode.getName();
            name.replace(".", File.separator);
            builder.append((builder.isEmpty() ? "" : File.separator) + name);
        });
        return builder+ Constants.HEADER_FILE_POSTFIX;
    }
}
