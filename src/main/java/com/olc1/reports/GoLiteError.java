package com.olc1.reports;

public class GoLiteError {
    private final String type;
    private final String description;
    private final int line;
    private final int column;

    public GoLiteError(String type, String description, int line, int column) {
        this.type = type;
        this.description = description;
        this.line = line;
        this.column = column;
    }

    public String getType()        { return type; }
    public String getDescription() { return description; }
    public int    getLine()        { return line; }
    public int    getColumn()      { return column; }

    @Override
    public String toString() {
        return String.format("Error %s: %s en la linea %d, columna %d", type, description, line, column);
    }
}
