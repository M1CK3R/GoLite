package com.olc1.ast.stm;

public class FieldDecl {
    private String name;
    private String type;
    private final boolean isAmbiguous;
    private final String first;
    private final String second;

    public FieldDecl(String name, String type) {
        this.name = name;
        this.type = type;
        this.isAmbiguous = false;
        this.first = null;
        this.second = null;
    }

    public FieldDecl(String first, String second, boolean isAmbiguous) {
        this.first = first;
        this.second = second;
        this.isAmbiguous = isAmbiguous;
        // Placeholders resolved at runtime
        this.name = second;
        this.type = first;
    }

    public String name() { return name; }
    public String type() { return type; }
    public boolean isAmbiguous() { return isAmbiguous; }
    public String first() { return first; }
    public String second() { return second; }

    public void resolve(String name, String type) {
        this.name = name;
        this.type = type;
    }
}
