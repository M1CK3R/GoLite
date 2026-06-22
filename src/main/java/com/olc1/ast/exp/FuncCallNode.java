package com.olc1.ast.exp;

import java.util.List;
import com.olc1.ast.ASTNode;
import com.olc1.visitor.Visitor;

public class FuncCallNode implements ASTNode {
    private final String name;
    private final List<ASTNode> args;
    private final int line;
    private final int column;

    public FuncCallNode(String name, List<ASTNode> args, int line, int column) {
        this.name = name;
        this.args = args;
        this.line = line;
        this.column = column;
    }

    public static class Context {
        public final String name;
        public final List<ASTNode> args;
        public final int line;
        public final int column;

        public Context(FuncCallNode node) {
            this.name = node.name;
            this.args = node.args;
            this.line = node.line;
            this.column = node.column;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}
