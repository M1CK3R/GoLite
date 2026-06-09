package com.olc1.ast.stm;

import com.olc1.ast.ASTNode;
import com.olc1.visitor.Visitor;

public class ExprStatment implements ASTNode{
    private final ASTNode expression;
    private final int line;
    private final int column;

    public ExprStatment(ASTNode expression, int line, int column) {
        this.expression = expression;
        this.line = line;
        this.column = column;
    }

    public static class Context {
        public final ASTNode expression;
        public final int line;
        public final int column;

        public Context(ExprStatment node) {
            this.expression = node.expression;
            this.line = node.line;
            this.column = node.column;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }

}
