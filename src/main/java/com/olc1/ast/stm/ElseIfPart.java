package com.olc1.ast.stm;

import com.olc1.ast.ASTNode;
import com.olc1.visitor.Visitor;

public class ElseIfPart implements ASTNode {
    private final ASTNode condition;
    private final ASTNode body;
    private final ASTNode elsePart;  // siguiente ElseIfPart, ElsePart o null
    private final int line;
    private final int column;

    public ElseIfPart(ASTNode condition, ASTNode body, ASTNode elsePart, int line, int column) {
        this.condition = condition;
        this.body = body;
        this.elsePart = elsePart;
        this.line = line;
        this.column = column;
    }

    public static class Context {
        public final ASTNode condition;
        public final ASTNode body;
        public final ASTNode elsePart;
        public final int line;
        public final int column;

        public Context(ElseIfPart node) {
            this.condition = node.condition;
            this.body = node.body;
            this.elsePart = node.elsePart;
            this.line = node.line;
            this.column = node.column;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}
