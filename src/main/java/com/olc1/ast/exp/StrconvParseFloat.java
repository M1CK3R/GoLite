package com.olc1.ast.exp;

import com.olc1.ast.ASTNode;
import com.olc1.visitor.Visitor;

public class StrconvParseFloat implements ASTNode{
    private final ASTNode argument;
    private final int line;
    private final int column;

    public StrconvParseFloat(ASTNode argument, int line, int column) {
        this.argument = argument;
        this.line = line;
        this.column = column;
    }

    public static class Context {
        public final ASTNode argument;
        public final int line;
        public final int column;

        public Context(StrconvParseFloat node) {
            this.argument = node.argument;
            this.line = node.line;
            this.column = node.column;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}
