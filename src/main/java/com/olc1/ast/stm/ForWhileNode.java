package com.olc1.ast.stm;

import com.olc1.ast.ASTNode;
import com.olc1.visitor.Visitor;

public class ForWhileNode implements ASTNode {
    private final ASTNode condition;
    private final ASTNode body;
    private final int line;
    private final int column;

    public ForWhileNode(ASTNode condition, ASTNode body, int line, int column) {
        this.condition = condition;
        this.body = body;
        this.line = line;
        this.column = column;
    }

    public static class Context {
        public final ASTNode condition;
        public final ASTNode body;
        public final int line;
        public final int column;

        public Context(ForWhileNode node) {
            this.condition = node.condition;
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
