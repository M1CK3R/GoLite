package com.olc1.ast.exp;

import com.olc1.ast.ASTNode;
import com.olc1.visitor.Visitor;

public record FieldInit(String name, ASTNode value) implements ASTNode {
    @Override
    public <T> T accept(Visitor<T> visitor) {
        return null;
    }
}
