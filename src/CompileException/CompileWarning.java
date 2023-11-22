package CompileException;

import Parser.Entity.Location;

/**
 *
 */
public class CompileWarning extends CompileException {
    public CompileWarning(){
        super();
        tag="Warning";
    }

    public CompileWarning(String err){
        super(err);
        tag="Warning";
    }

    public CompileWarning(String err, Location location){
        super(err,location);
        tag="Warning";
    }

}
