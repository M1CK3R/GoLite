package com.olc1.ast.stm;

import com.olc1.ast.ASTNode;
import com.olc1.visitor.Visitor;

public class ReturnNode implements ASTNode {
    private final ASTNode value; // can be null
    private final int line;
    private final int column;

    public ReturnNode(ASTNode value, int line, int column) {
        this.value = value;
        this.line = line;
        this.column = column;
    }

    public static class Context {
        public final ASTNode value;
        public final int line;
        public final int column;

        public Context(ReturnNode node) {
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
