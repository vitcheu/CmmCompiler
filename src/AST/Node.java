package AST;

import compile.Constants;
import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;
import CompileException.*;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

abstract public class Node implements Dumpable{
    protected Location location;
    public static final String INDENTATION="    ";

    public  static PrintWriter dumpWriter;
    private static boolean closed=false;

    static {
        try {
            dumpWriter = new PrintWriter(Constants.outputPath + "AST Dump.html");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public Node(Location location){
        this.location=location;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    abstract  public void accept (ASTVisitor ASTVisitor) throws CompileError;

    public void dump(int level) {

    }

    public void ident(int n,String s){
        if(closed)
            return;;
        for(int i=0;i<n;i++){
            dumpWriter.print(INDENTATION);
        }
        dumpWriter.println(s);
    }

    public static void closeDumping(){
        dumpWriter.close();
        closed=true;
    }


    public void dumpHead(String s,int level){
        ident(level,"<"+s+">"+" "+location);
    }

    public void dumpEnd(String s,int level){
        ident(level,"</"+s+">");
    }

    public static void setDumpWriter(PrintWriter dumpWriter) {
        Node.dumpWriter = dumpWriter;
    }
}
