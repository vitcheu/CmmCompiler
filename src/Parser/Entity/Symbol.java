package Parser.Entity;

import java.util.Objects;

public abstract class Symbol {
    private String value;
    private int id;

    Location location;

    public Symbol(String value){
        this.value=value;
        location=new Location(0,0);
    }

    public Symbol(String value,Location l){
        this.value=value;
        this.location=l;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getId() {
        return id;
    }

    public String getValue() {
        return value;
    }

    protected void setId(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return value;
    }

    public abstract boolean isTerminal();

    public Location getLocation() {
        return location;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Symbol symbols = (Symbol) o;
        return id == symbols.id && Objects.equals(value, symbols.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, id);
    }
}
