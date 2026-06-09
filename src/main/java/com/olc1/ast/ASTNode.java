package com.olc1.ast;

import com.olc1.visitor.Visitor;

public interface ASTNode {
    <T> T accept(Visitor<T> visitor);
}
