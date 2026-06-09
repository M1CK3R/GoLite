package com.olc1.ast.exp;

import com.olc1.ast.ASTNode;
import com.olc1.visitor.Visitor;

public class Or implements ASTNode {
    private final ASTNode left;
    private final ASTNode right;

    public Or(ASTNode left, ASTNode right) {
        this.left = left;
        this.right = right;
    }

    public static class Context {
        public final ASTNode left;
        public final ASTNode right;

        public Context(Or node) {
            this.left = node.left;
            this.right = node.right;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}
