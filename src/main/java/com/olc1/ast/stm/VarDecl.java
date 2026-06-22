package com.olc1.ast.stm;

import com.olc1.ast.ASTNode;
import com.olc1.visitor.Visitor;

public class VarDecl implements ASTNode {
    private final String name;
    private final String type;       // "int", "float64", "bool", "string", "rune" or struct name
    private final ASTNode value;     // puede ser null si no se inicializa
    private final int line;
    private final int column;
    
    private final boolean isAmbiguous;
    private final String first;
    private final String second;

    public VarDecl(String name, String type, ASTNode value, int line, int column) {
        this.name = name;
        this.type = type;
        this.value = value;
        this.line = line;
        this.column = column;
        this.isAmbiguous = false;
        this.first = null;
        this.second = null;
    }

    public VarDecl(String first, String second, ASTNode value, boolean isAmbiguous, int line, int column) {
        this.name = second; // Placeholder
        this.type = first;  // Placeholder
        this.value = value;
        this.line = line;
        this.column = column;
        this.isAmbiguous = isAmbiguous;
        this.first = first;
        this.second = second;
    }

    public boolean isAmbiguous() { return isAmbiguous; }
    public String first() { return first; }
    public String second() { return second; }

    public static class Context {
        public final String name;
        public final String type;
        public final ASTNode value;
        public final int line;
        public final int column;
        
        public final boolean isAmbiguous;
        public final String first;
        public final String second;

        public Context(VarDecl node) {
            this.name = node.name;
            this.type = node.type;
            this.value = node.value;
            this.line = node.line;
            this.column = node.column;
            this.isAmbiguous = node.isAmbiguous;
            this.first = node.first;
            this.second = node.second;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}
