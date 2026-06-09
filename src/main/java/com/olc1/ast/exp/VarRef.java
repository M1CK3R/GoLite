package com.olc1.ast.exp;

import com.olc1.ast.ASTNode;
import com.olc1.visitor.Visitor;

public class VarRef implements ASTNode {
    private final String name;
    private final int line;
    private final int column;

    public VarRef(String name, int line, int column) {
        this.name = name;
        this.line = line;
        this.column = column;
    }

    public static class Context {
        public final String name;
        public final int line;
        public final int column;

        public Context(VarRef node) {
            this.name = node.name;
            this.line = node.line;
            this.column = node.column;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}
