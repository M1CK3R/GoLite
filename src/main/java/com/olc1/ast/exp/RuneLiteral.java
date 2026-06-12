package com.olc1.ast.exp;

import com.olc1.ast.ASTNode;
import com.olc1.visitor.Visitor;

public class RuneLiteral implements ASTNode {
    private final char value;
    private final int line;
    private final int column;

    public RuneLiteral(String raw, int line, int column) {
        this.line = line;
        this.column = column;
        // raw llega como 'a' o '\n', extraemos el char
        if (raw.length() == 3) {
            this.value = raw.charAt(1);
        } else {
            this.value = switch (raw.charAt(2)) {
                case 'n'  -> '\n';
                case 't'  -> '\t';
                case 'r'  -> '\r';
                case '\\' -> '\\';
                case '\'' -> '\'';
                default   -> raw.charAt(2);
            };
        }
    }

    public static class Context {
        public final char value;
        public final int line;
        public final int column;

        public Context(RuneLiteral node) {
            this.value = node.value;
            this.line = node.line;
            this.column = node.column;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}
