package IR.instruction;

import Parser.Entity.Location;
import IR.Constants.OP;
import IR.Result;
import IR.Value;

public class ConditionJump extends Instruction{
    private OP jumpType;
    private boolean trueJump=true;


    public ConditionJump(OP op, Value arg1, Value arg2, Result result, Location location, OP jumpType, boolean tranImmediately) {
        super(op, arg1, arg2, result, location,tranImmediately);
        this.jumpType=jumpType;
        if(op.equals(OP.ifFalse_jump)){
            trueJump=false;
        }
    }


    public OP getJumpType() {
        return jumpType;
    }

    public boolean isTrueJump() {
        return trueJump;
    }
}
