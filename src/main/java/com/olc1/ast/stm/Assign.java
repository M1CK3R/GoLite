package com.olc1.ast.stm;

import com.olc1.ast.ASTNode;
import com.olc1.visitor.Visitor;

public class Assign implements ASTNode {
    private final ASTNode target;
    private final ASTNode value;
    private final int line;
    private final int column;

    public Assign(ASTNode target, ASTNode value, int line, int column) {
        this.target = target;
        this.value = value;
        this.line = line;
        this.column = column;
    }

    public static class Context {
        public final ASTNode target;
        public final ASTNode value;
        public final int line;
        public final int column;

        public Context(Assign node) {
            this.target = node.target;
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
