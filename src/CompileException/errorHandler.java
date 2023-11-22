package CompileException;

import Parser.Entity.Location;

import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

public class errorHandler {
    protected PrintWriter err;
    protected List<CompileException> exceptions=new LinkedList();
    boolean errorOccurred=false;
    public errorHandler(PrintWriter printWriter){
        err=printWriter;
    }

    public void handle(CompileException e){
        if(e.toString()==null) return;
        if(e.isError())
            errorOccurred=true;
        exceptions.add(e);
    }

    public void error(String s, Location location){
        handle(new CompileError(s,location));
    }

    public void warning(String s,Location location){
        handle(new CompileWarning(s,location));
    }

    public boolean errorOccurred(){
        return errorOccurred;
    }

    public void close(){
        err.close();
    }

    public void flush() {
        exceptions.sort((o1, o2) -> {
            //优先输出错误信息
            if(o1.isError()&&!o2.isError()) return -1;
            else if(!o1.isError()&&o2.isError()) return 1;
            if(o1.getLocation()==null||o2.getLocation()==null) return 0;
            if(o1.getLocation().getLine()<o2.getLocation().getLine()) return -1;
            if(o1.getLocation().getLine()>o2.getLocation().getLine()) return 1;
            return Integer.compare(o1.getLocation().getColumn(),o2.getLocation().getColumn());
        });

        for(CompileException e:exceptions){
            err.println (e.getTag()+":  " +
                    "位置"+e.getLocation()+","
                    +e+"\n");
        }
    }
}
