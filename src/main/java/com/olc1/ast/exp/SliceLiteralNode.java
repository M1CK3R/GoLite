package com.olc1.ast.exp;

import java.util.List;
import com.olc1.ast.ASTNode;
import com.olc1.visitor.Visitor;

public class SliceLiteralNode implements ASTNode {
    private final String type;
    private final List<ASTNode> elements;
    private final int line;
    private final int column;

    public SliceLiteralNode(String type, List<ASTNode> elements, int line, int column) {
        this.type = type;
        this.elements = elements;
        this.line = line;
        this.column = column;
    }

    public static class Context {
        public final String type;
        public final List<ASTNode> elements;
        public final int line;
        public final int column;

        public Context(SliceLiteralNode node) {
            this.type = node.type;
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
