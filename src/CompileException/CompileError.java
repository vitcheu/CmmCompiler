package CompileException;

import Parser.Entity.Location;
public class CompileError extends CompileException {


    public CompileError() {
        super();
        tag =CompileException.ERROR;
    }

    public CompileError(String err) {
        super(err);
        tag =CompileException.ERROR;
    }

    public CompileError(String err, Location location) {
        super(err, location);
        tag =CompileException.ERROR;
    }


}
