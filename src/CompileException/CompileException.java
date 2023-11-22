package CompileException;

import Parser.Entity.Location;

public class CompileException extends Exception {
    protected static final String ERROR="error";
    protected  static final String WARNING="warning";
    protected String tag;
    private final  String errMsg;
    private final Location location;

    public CompileException(){
        errMsg="";
        location=null;
        tag=ERROR;
    }

    public CompileException(String err){
        errMsg=err;
        location=null;
    }

    public CompileException(String err,Location location){
        errMsg=err;
        this.location=location;
    }

    public Location getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return errMsg;
    }

    public String getTag() {
        return tag;
    }

    public boolean isError(){
        return tag.equals(ERROR);
    }
}

