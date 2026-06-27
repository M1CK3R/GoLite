package com.olc1.ast.exp;

import com.olc1.ast.ASTNode;
import com.olc1.visitor.Visitor;

public record FieldInit(String name, ASTNode value) implements ASTNode {
    public static class Context {
        public final String name;
        public final ASTNode value;

        public Context(FieldInit node) {
            this.name = node.name;
            this.value = node.value;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}