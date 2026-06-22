package com.olc1.ast.exp;

import java.util.List;
import com.olc1.ast.ASTNode;
import com.olc1.visitor.Visitor;

public class BraceLiteralNode implements ASTNode {
    private final List<ASTNode> elements;
    private final int line;
    private final int column;

    public BraceLiteralNode(List<ASTNode> elements, int line, int column) {
        this.elements = elements;
        this.line = line;
        this.column = column;
    }

    public static class Context {
        public final List<ASTNode> elements;
        public final int line;
        public final int column;

        public Context(BraceLiteralNode node) {
            this.elements = node.elements;
            this.line = node.line;
            this.column = node.column;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}
