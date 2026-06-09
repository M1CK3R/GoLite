package com.olc1.ast.stm;

import com.olc1.ast.ASTNode;
import com.olc1.visitor.Visitor;


public class IfNode implements ASTNode {
    private final ASTNode condition;
    private final ASTNode body;

    public IfNode(ASTNode condition, ASTNode body) {
        this.condition = condition;
        this.body = body;
    }

    public static class Context {
        public final ASTNode condition;
        public final ASTNode body;
        public final ASTNode elsePart;
        public Context(IfNode node) {
            this.condition = node.condition;
            this.body = node.body;
            this.elsePart = node.elsePart;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}
