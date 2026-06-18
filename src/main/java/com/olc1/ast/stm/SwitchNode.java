package com.olc1.ast.stm;

import java.util.List;
import com.olc1.ast.ASTNode;
import com.olc1.visitor.Visitor;

public class SwitchNode implements ASTNode {
    private final ASTNode expression;
    private final List<ASTNode> cases; // contains CaseNode objects
    private final int line;
    private final int column;

    public SwitchNode(ASTNode expression, List<ASTNode> cases, int line, int column) {
        this.expression = expression;
        this.cases = cases;
        this.line = line;
        this.column = column;
    }

    public static class Context {
        public final ASTNode expression;
        public final List<ASTNode> cases;
        public final int line;
        public final int column;

        public Context(SwitchNode node) {
            this.expression = node.expression;
            this.cases = node.cases;
            this.line = node.line;
            this.column = node.column;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}
