package com.olc1.visitor.interpreter.value;

public record StringValue(String value, int line, int column) implements ValueWrapper {
    @Override
    public String getTypeName() {
        return "string";
    }

    @Override
    public String toString() {
        return value.substring(1, value.length() - 1);
    }
}
