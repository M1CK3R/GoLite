package com.olc1.ast.exp;

import java.util.List;
import com.olc1.ast.ASTNode;
import com.olc1.visitor.Visitor;

public class MethodCallNode implements ASTNode {
    private final ASTNode receiver;
    private final String name;
    private final List<ASTNode> args;
    private final int line;
    private final int column;

    public MethodCallNode(ASTNode receiver, String name, List<ASTNode> args, int line, int column) {
        this.receiver = receiver;
        this.name = name;
        this.args = args;
        this.line = line;
        this.column = column;
    }

    public static class Context {
        public final ASTNode receiver;
        public final String name;
        public final List<ASTNode> args;
        public final int line;
        public final int column;

        public Context(MethodCallNode node) {
            this.receiver = node.receiver;
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
