package com.olc1.ast.stm;

import com.olc1.ast.ASTNode;
import com.olc1.visitor.Visitor;

public class ShortDecl implements ASTNode {
    private final String name;
    private final ASTNode value;
    private final int line;
    private final int column;

    public ShortDecl(String name, ASTNode value, int line, int column) {
        this.name = name;
        this.value = value;
        this.line = line;
        this.column = column;
    }

    public static class Context {
        public final String name;
        public final ASTNode value;
        public final int line;
        public final int column;

        public Context(ShortDecl node) {
            this.name = node.name;
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
