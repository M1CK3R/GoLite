package com.olc1.ast.exp;

import com.olc1.ast.ASTNode;
import com.olc1.visitor.Visitor;

public class BoolLiteral implements ASTNode {
    private final boolean value;
    private final int line;
    private final int column;

    public BoolLiteral(boolean value, int line, int column) {
        this.value = value;
        this.line = line;
        this.column = column;
    }

    public static class Context {
        public final boolean value;
        public final int line;
        public final int column;

        public Context(BoolLiteral node) {
            this.value = node.value;
            this.line = node.line;
            this.column = node.column;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}
