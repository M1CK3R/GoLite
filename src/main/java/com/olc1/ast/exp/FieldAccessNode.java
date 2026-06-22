package com.olc1.ast.exp;

import com.olc1.ast.ASTNode;
import com.olc1.visitor.Visitor;

public class FieldAccessNode implements ASTNode {
    private final ASTNode target;
    private final String field;
    private final int line;
    private final int column;

    public FieldAccessNode(ASTNode target, String field, int line, int column) {
        this.target = target;
        this.field = field;
        this.line = line;
        this.column = column;
    }

    public ASTNode getTarget() {
        return target;
    }

    public String getField() {
        return field;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public static class Context {
        public final ASTNode target;
        public final String field;
        public final int line;
        public final int column;

        public Context(FieldAccessNode node) {
            this.target = node.target;
            this.field = node.field;
            this.line = node.line;
            this.column = node.column;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}
