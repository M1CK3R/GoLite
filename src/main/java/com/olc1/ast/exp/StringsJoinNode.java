package com.olc1.ast.exp;

import com.olc1.ast.ASTNode;
import com.olc1.visitor.Visitor;

public class StringsJoinNode implements ASTNode {
    private final ASTNode slice;
    private final ASTNode sep;
    private final int line;
    private final int column;

    public StringsJoinNode(ASTNode slice, ASTNode sep, int line, int column) {
        this.slice = slice;
        this.sep = sep;
        this.line = line;
        this.column = column;
    }

    public static class Context {
        public final ASTNode slice;
        public final ASTNode sep;
        public final int line;
        public final int column;

        public Context(StringsJoinNode node) {
            this.slice = node.slice;
            this.sep = node.sep;
            this.line = node.line;
            this.column = node.column;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}
