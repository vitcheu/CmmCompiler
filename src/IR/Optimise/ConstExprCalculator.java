package IR.Optimise;

import IR.Constants.OP;
import IR.Constants.Type;
import IR.Literal;

import static IR.Constants.OP.*;

/**
 *
 */
public class ConstExprCalculator {
    private static Number parse(String lxr,boolean isFloat){
        if (isFloat)
            return (Float) Float.parseFloat(lxr);
        return (Integer) Integer.parseInt(lxr);
    }

    public static Literal calculate(Literal left, Literal right, OP op){
        boolean isFloat=left.isFloat()||right.isFloat();
        String l=left.getLxrValue(),r=right.getLxrValue();
        Number n1=parse(l,isFloat),n2=parse(r,isFloat);
        int i1=0,i2=0;
        float f1=0,f2=0;
        if(!isFloat){
            i1=(Integer)n1;
            i2=(Integer)n2;
        }else{
            f1=(Float) n1;
            f2=(Float)n2;
        }
        Number n3;

        n3= switch (op){
            case add -> {
                if (isFloat) yield f1 + f2;
                yield ( i1 + i2);
            }

            case sub -> {
                if (isFloat) yield f1 - f2;
                yield (i1 - i2);
            }

            case mul -> {
                if (isFloat) yield f1 * f2;
                yield  (i1 * i2);
            }

            case div -> {
                if (isFloat) yield f1 / f2;
                yield  ( i1 / i2);
            }

            case mod ->(  i1 % i2);

            case shift_left -> ( i1 << i2);

            case shift_right ->( i1 >> i2);

            case and ->( (i1!=0&&i2!=0)?1:0);

            case or -> ( (i1 != 0 || i2 != 0)?1:0);

            case xor ->( (i1 == 0 && i2 != 0 || i1 != 0 && i2 == 0)?1:0);

            case eq ->(  (n1==n2)?1:0);
            case ne -> ( (n1!=n2)?1:0);
            case ge -> {
                if (isFloat) {
                    yield (f1 >= f2) ? 1 : 0;
                } else {
                    yield (  (i1 >= i2) ? 1 : 0);
                }
            }
            case le -> {
                if (isFloat) {
                    yield (f1 <= f2) ? 1 : 0;
                } else {
                    yield ( (i1 <= i2) ? 1 : 0);
                }
            }
            case gt -> {
                if (isFloat) {
                    yield (f1 > f2) ? 1 : 0;
                } else {
                    yield ( (i1 > i2) ? 1 : 0);
                }
            }
            case lt -> {
                if (isFloat) {
                    yield (f1 < f2) ? 1 : 0;
                } else {
                    yield ( (i1 < i2) ? 1 : 0);
                }
            }


            default->{
                throw new RuntimeException(String.format("不支持的操作 %s %s %s",left,op,right));
            }
        };

        return new Literal(n3.toString(),left.getIRType());
    }

    public static Literal calculateUnary(Literal operand, OP op) {
        String l = operand.getLxrValue();
        boolean isFloat = operand.isFloat();
        Number n1 = parse(l, isFloat);
        int i1 = 0;
        float f1 = 0;
        Number n3;
        if (!isFloat) {
            i1 = (Integer) n1;
        } else {
            f1 = (Float) n1;
        }

        n3 = switch (op) {
            case neg -> {
                if (isFloat) yield -f1 - 1;
                yield  ((-i1 - 1));
            }

            case minus -> {
                if (isFloat) yield -f1;
                yield  (-i1);
            }

            case not  -> {
                if (isFloat) {
                    yield (f1 == 0) ? 1 : 0;
                } else {
                    yield  ((i1 == 0) ? 1 : 0);
                }
            }


            default -> {
                throw new RuntimeException(String.format("不支持的操作 %s %s ",op, operand));
            }
        };

        return new Literal(n3.toString(), operand.getIRType());
    }
}
