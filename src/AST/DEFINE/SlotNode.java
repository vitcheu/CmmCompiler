package AST.DEFINE;

import AST.Node;
import AST.TYPE.TypeNode;
import Semantic_Analysis.ASTVisitor;
import Parser.Entity.Location;

import java.util.Objects;
import CompileException.*;

public class SlotNode extends Node {
    private TypeNode type;
    private String name;

    public SlotNode(Location location, TypeNode type, String name) {
        super(location);
        this.type = type;
        this.name = name;
    }

    @Override
    public void accept(ASTVisitor ASTVisitor) throws CompileError {
        ASTVisitor.visit(this);
    }

    @Override
    public void dump(int level) {
        ident(level, toString());
    }

    @Override
    public String toString() {
        return type + ": " + ((name != null) ? name : "") + " ";
    }

    public TypeNode getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SlotNode slotNode = (SlotNode) o;
        return Objects.equals(type, slotNode.type) && Objects.equals(name, slotNode.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name);
    }
}
