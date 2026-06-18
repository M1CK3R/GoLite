package com.olc1.ast.stm;

import com.olc1.ast.ASTNode;
import com.olc1.visitor.Visitor;

public class CaseNode implements ASTNode {
    private final ASTNode expression; // null for default case
    private final Statments body;
    private final int line;
    private final int column;

    public CaseNode(ASTNode expression, Statments body, int line, int column) {
        this.expression = expression;
        this.body = body;
        this.line = line;
        this.column = column;
    }

    public ASTNode getExpression() {
        return expression;
    }

    public Statments getBody() {
        return body;
    }

    public static class Context {
        public final ASTNode expression;
        public final Statments body;
        public final int line;
        public final int column;

        public Context(CaseNode node) {
            this.expression = node.expression;
            this.body = node.body;
            this.line = node.line;
            this.column = node.column;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}
