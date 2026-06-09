package com.olc1.visitor;

import com.olc1.ast.exp.*;
import com.olc1.ast.stm.*;

public interface Visitor<T> {
    T visit(Integers.Context ctx);
    T visit(Decimal.Context ctx);
    T visit(Paren.Context ctx);
    T visit(Add.Context ctx);
    T visit(Sub.Context ctx);
    T visit(Mul.Context ctx);
    T visit(Div.Context ctx);
    T visit(Negate.Context ctx);
    T visit(BoolLiteral.Context ctx);
    T visit(StringLiteral.Context ctx);
    T visit(VarRef.Context ctx);
    T visit(Imprimir.Context ctx);
    T visit(Assign.Context ctx);
    T visit(IfNode.Context ctx);
    T visit(Statments.Context ctx);
    T visit(Mod.Context ctx);
    T visit(Eq.Context ctx);
    T visit(Neq.Context ctx);
    T visit(Lt.Context ctx);
    T visit(Le.Context ctx);
    T visit(Gt.Context ctx);
    T visit(Ge.Context ctx);
    T visit(And.Context ctx);
    T visit(Or.Context ctx);
    T visit(Not.Context ctx);
    T visit(NilLiteral.Context ctx);
    T visit(VarDecl.Context ctx);
    T visit(ShortDecl.Context ctx);
    T visit(PlusAssign.Context ctx);
    T visit(MinusAssign.Context ctx);
    T visit(ElseIfPart.Context ctx);   // Si es necesario
    T visit(ElsePart.Context ctx);
    T visit(ForNode.Context ctx);
    T visit(ForWhileNode.Context ctx);
    T visit(BreakNode.Context ctx);
    T visit(ContinueNode.Context ctx);
    T visit(ExprStmt.Context ctx);
    T visit(FmtPrintln.Context ctx);
    T visit(StrconvAtoi.Context ctx);
    T visit(StrconvParseFloat.Context ctx);
    T visit(ReflectTypeOf.Context ctx);
}
