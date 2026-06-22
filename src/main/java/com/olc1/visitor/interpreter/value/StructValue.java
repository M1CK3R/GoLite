package com.olc1.visitor.interpreter.value;

import java.util.Map;
import java.util.LinkedHashMap;

public final class StructValue implements ValueWrapper {
    private final String typeName;
    private final Map<String, ValueWrapper> fields = new LinkedHashMap<>();
    private final int line;
    private final int column;

    public StructValue(String typeName, int line, int column) {
        this.typeName = typeName;
        this.line = line;
        this.column = column;
    }

    public Map<String, ValueWrapper> fields() {
        return fields;
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
        return typeName;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        int i = 0;
        for (Map.Entry<String, ValueWrapper> entry : fields.entrySet()) {
            if (i > 0) sb.append(", ");
            sb.append(entry.getKey()).append(":").append(entry.getValue());
            i++;
        }
        sb.append("}");
        return sb.toString();
    }
}
