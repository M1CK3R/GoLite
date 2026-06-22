package com.olc1.ast.exp;

import com.olc1.ast.ASTNode;
import com.olc1.visitor.Visitor;

public class SliceAccessNode implements ASTNode {
    private final ASTNode target;
    private final ASTNode index;
    private final int line;
    private final int column;

    public SliceAccessNode(ASTNode target, ASTNode index, int line, int column) {
        this.target = target;
        this.index = index;
        this.line = line;
        this.column = column;
    }

    public ASTNode getTarget() {
        return target;
    }

    public ASTNode getIndex() {
        return index;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public static class Context {
        public final ASTNode target;
        public final ASTNode index;
        public final int line;
        public final int column;

        public Context(SliceAccessNode node) {
            this.target = node.target;
            this.index = node.index;
            this.line = node.line;
            this.column = node.column;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}
