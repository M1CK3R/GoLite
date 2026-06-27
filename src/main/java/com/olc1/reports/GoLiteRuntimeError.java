package com.olc1.reports;

public class GoLiteRuntimeError extends RuntimeException {
    private final int line;
    private final int column;

    public GoLiteRuntimeError(String message, int line, int column) {
        super(message);
        this.line = line;
        this.column = column;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }
}
