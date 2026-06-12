package com.olc1.visitor.interpreter.value;

public record RuneValue(char value, int line, int column) implements ValueWrapper {
    @Override
    public String getTypeName() {
        return "rune";
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
