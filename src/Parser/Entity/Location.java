package Parser.Entity;

import java.util.Comparator;

public class Location implements Comparable<Location>{
    private  int line;
    private int column;

    public Location(int line, int column) {
        this.line = line;
        this.column = column;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    @Override
    public String toString() {
        return "("+line+","+column+")";
    }

    @Override
    public int compareTo(Location b) {
        int result;
        if (b == null) {
            result = 0;
        } else {
            int la = this.getLine(), lb = b.getLine();
            int ca = this.getColumn(), cb = b.getColumn();
            if (la == lb) {
                result = Integer.compare(ca, cb);
            } else {
                result = Integer.compare(la, lb);
            }
        }
        return result;
    }
}
