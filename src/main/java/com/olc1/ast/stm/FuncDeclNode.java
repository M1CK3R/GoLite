package com.olc1.ast.stm;

import java.util.List;
import com.olc1.ast.ASTNode;
import com.olc1.visitor.Visitor;

public class FuncDeclNode implements ASTNode {
    private final String name;
    private final List<Param> params;
    private final String returnType;
    private final ASTNode body;
    private final int line;
    private final int column;

    public FuncDeclNode(String name, List<Param> params, String returnType, ASTNode body, int line, int column) {
        this.name = name;
        this.params = params;
        this.returnType = returnType;
        this.body = body;
        this.line = line;
        this.column = column;
    }

    public static class Context {
        public final String name;
        public final List<Param> params;
        public final String returnType;
        public final ASTNode body;
        public final int line;
        public final int column;

        public Context(FuncDeclNode node) {
            this.name = node.name;
            this.params = node.params;
            this.returnType = node.returnType;
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
