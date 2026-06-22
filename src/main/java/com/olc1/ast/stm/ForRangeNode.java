package com.olc1.ast.stm;

import com.olc1.ast.ASTNode;
import com.olc1.visitor.Visitor;

public class ForRangeNode implements ASTNode {
    private final String indexId;       // can be null
    private final String valueId;       // can be null
    private final boolean isShortDecl;
    private final ASTNode collection;
    private final ASTNode body;
    private final int line;
    private final int column;

    public ForRangeNode(String indexId, String valueId, boolean isShortDecl, ASTNode collection, ASTNode body, int line, int column) {
        this.indexId = indexId;
        this.valueId = valueId;
        this.isShortDecl = isShortDecl;
        this.collection = collection;
        this.body = body;
        this.line = line;
        this.column = column;
    }

    public static class Context {
        public final String indexId;
        public final String valueId;
        public final boolean isShortDecl;
        public final ASTNode collection;
        public final ASTNode body;
        public final int line;
        public final int column;

        public Context(ForRangeNode node) {
            this.indexId = node.indexId;
            this.valueId = node.valueId;
            this.isShortDecl = node.isShortDecl;
            this.collection = node.collection;
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
