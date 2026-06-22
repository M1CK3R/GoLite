package com.olc1.visitor.interpreter.value;

import java.util.List;
import java.util.ArrayList;

public final class SliceValue implements ValueWrapper {
    private final String elementType;
    private final List<ValueWrapper> elements;
    private final int line;
    private final int column;

    public SliceValue(String elementType, int line, int column) {
        this.elementType = elementType;
        this.elements = new ArrayList<>();
        this.line = line;
        this.column = column;
    }

    public SliceValue(String elementType, List<ValueWrapper> elements, int line, int column) {
        this.elementType = elementType;
        this.elements = new ArrayList<>(elements);
        this.line = line;
        this.column = column;
    }

    public String getElementType() {
        return elementType;
    }

    public List<ValueWrapper> getElements() {
        return elements;
    }

    @Override
    public int line() {
        return line;
    }

    @Override
    public int column() {
        return column;
    }

    @Override
    public String getTypeName() {
        return "[]" + elementType;
    }

    @Override
    public String toString() {
        return elements.toString();
    }
}
