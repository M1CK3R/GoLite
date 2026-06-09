package com.olc1.ast.exp;

import com.olc1.ast.ASTNode;
import com.olc1.visitor.Visitor;

public class Integers implements ASTNode{
    private final int value;
    private final int line;
    private final int column;

    public Integers(int value, int line, int column) {
        this.value = value;
        this.line = line;
        this.column = column;
    }

    public static class Context {
        public final Integer value;
        public final int line;
        public final int column;

        public Context(Integers node) {
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
