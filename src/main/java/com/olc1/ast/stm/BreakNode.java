package com.olc1.ast.stm;

import com.olc1.ast.ASTNode;
import com.olc1.visitor.Visitor;

public class BreakNode implements ASTNode {
    private final int line;
    private final int column;

    public BreakNode(int line, int column) {
        this.line = line;
        this.column = column;
    }

    public static class Context {
        public final int line;
        public final int column;

        public Context(BreakNode node) {
            this.line = node.line;
            this.column = node.column;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}
