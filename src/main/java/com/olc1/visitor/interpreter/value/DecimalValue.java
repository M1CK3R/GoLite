package com.olc1.visitor.interpreter.value;

public record DecimalValue(double value, int line, int column) implements ValueWrapper {

    @Override
    public String getTypeName() {
        return "float64";
    }

    @Override
    public String toString() {
        if (value == Math.floor(value)) {
            return String.format("%.1f", value);
        }
        return String.valueOf(value);
    }
}
