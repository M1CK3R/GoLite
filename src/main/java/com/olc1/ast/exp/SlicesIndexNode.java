package com.olc1.ast.exp;

import com.olc1.ast.ASTNode;
import com.olc1.visitor.Visitor;

public class SlicesIndexNode implements ASTNode {
    private final ASTNode slice;
    private final ASTNode value;
    private final int line;
    private final int column;

    public SlicesIndexNode(ASTNode slice, ASTNode value, int line, int column) {
        this.slice = slice;
        this.value = value;
        this.line = line;
        this.column = column;
    }

    public static class Context {
        public final ASTNode slice;
        public final ASTNode value;
        public final int line;
        public final int column;

        public Context(SlicesIndexNode node) {
            this.slice = node.slice;
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
