package com.olc1.ast.exp;

import com.olc1.ast.ASTNode;
import com.olc1.visitor.Visitor;

public class Decimal implements ASTNode {
    private final double value;
    private final int line;
    private final int column;

    public Decimal(double value, int line, int column) {
        this.value = value;
        this.line  = line;
        this.column = column;
    }

    public static class Context {
        public final Double value;
        public final int line;
        public final int column;

        public Context(Decimal node) {
            this.value = node.value;
            this.line = node.line;
            this.column = node.column;
        }
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visit(new Context(this));
    }
}
