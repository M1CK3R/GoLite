package com.olc1.ast.stm;

import com.olc1.ast.ASTNode;
import com.olc1.visitor.Visitor;

public class ForNode implements ASTNode {
    private final ASTNode init;       // puede ser null (si no hay init)
    private final ASTNode condition;  // puede ser null (bucle infinito, pero en Fase1 no se pide)
    private final ASTNode increment;  // puede ser null
    private final ASTNode body;
    private final int line;
    private final int column;

    public ForNode(ASTNode init, ASTNode condition, ASTNode increment, ASTNode body, int line, int column) {
        this.init = init;
        this.condition = condition;
        this.increment = increment;
        this.body = body;
        this.line = line;
        this.column = column;
    }

    public static class Context {
        public final ASTNode init;
        public final ASTNode condition;
        public final ASTNode increment;
        public final ASTNode body;
        public final int line;
        public final int column;

        public Context(ForNode node) {
            this.init = node.init;
            this.condition = node.condition;
            this.increment = node.increment;
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
