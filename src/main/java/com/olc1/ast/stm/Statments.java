package com.olc1.ast.stm;

import java.util.ArrayList;
import java.util.List;

import com.olc1.ast.ASTNode;
import com.olc1.visitor.Visitor;

public class Statments implements ASTNode {
    private final List<ASTNode> statements;

    public Statments() {
        this.statements = new ArrayList<>();
    }

    public Statments(ASTNode statement) {
         this.statements = new ArrayList<>();
         this.statements.add(statement);
    }

    public void add(ASTNode statement) {
        this.statements.add(statement);
    }

    public class Context {
        public final List<ASTNode> statements;

        public Context(ASTNode node) {
            this.statements = ((Statments) node).statements;
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(new Context(this));
    }
}
