package com.olc1.ast.stm;

import java.util.List;
import com.olc1.ast.ASTNode;
import com.olc1.visitor.Visitor;

public class StructDeclNode implements ASTNode {
    private final String name;
    private final List<FieldDecl> fields;
    private final int line;
    private final int column;

    public StructDeclNode(String name, List<FieldDecl> fields, int line, int column) {
        this.name = name;
        this.fields = fields;
        this.line = line;
        this.column = column;
    }

    public static class Context {
        public final String name;
        public final List<FieldDecl> fields;
        public final int line;
        public final int column;

        public Context(StructDeclNode node) {
            this.name = node.name;
            this.fields = node.fields;
            this.line = node.line;
            this.column = node.column;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}
