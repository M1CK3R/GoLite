package com.olc1.visitor;

import com.olc1.ast.exp.*;
import com.olc1.ast.stm.*;
import com.olc1.visitor.graphviz.*;

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

    T visit(Equ.Context ctx);

    T visit(Nequ.Context ctx);

    T visit(Lesser.Context ctx);

    T visit(LesserEqu.Context ctx);

    T visit(Greater.Context ctx);

    T visit(GreaterEqu.Context ctx);

    T visit(And.Context ctx);

    T visit(Or.Context ctx);

    T visit(Not.Context ctx);

    T visit(NilLiteral.Context ctx);

    T visit(VarDecl.Context ctx);

    T visit(ShortDecl.Context ctx);

    T visit(PlusAssign.Context ctx);

    T visit(MinusAssign.Context ctx);

    T visit(ElseIfPart.Context ctx);

    T visit(ElsePart.Context ctx);

    T visit(ForNode.Context ctx);

    T visit(ForRangeNode.Context ctx);

    T visit(ForWhileNode.Context ctx);

    T visit(BreakNode.Context ctx);

    T visit(ContinueNode.Context ctx);

    T visit(ExprStatment.Context ctx);

    T visit(FmtPrintln.Context ctx);

    T visit(StrconvAtoi.Context ctx);

    T visit(StrconvParseFloat.Context ctx);

    T visit(ReflectTypeOf.Context ctx);

    T visit(RuneLiteral.Context ctx);

    T visit(SwitchNode.Context ctx);

    T visit(CaseNode.Context ctx);

    T visit(FuncDeclNode.Context ctx);

    T visit(MethodDeclNode.Context ctx);

    T visit(ReturnNode.Context ctx);

    T visit(StructDeclNode.Context ctx);

    T visit(FuncCallNode.Context ctx);

    T visit(MethodCallNode.Context ctx);

    T visit(FieldAccessNode.Context ctx);

    T visit(SliceAccessNode.Context ctx);

    T visit(SliceLiteralNode.Context ctx);

    T visit(BraceLiteralNode.Context ctx);

    T visit(StructLiteralNode.Context ctx);

    T visit(AppendNode.Context ctx);

    T visit(SlicesIndexNode.Context ctx);

    T visit(StringsJoinNode.Context ctx);

    T visit(LenNode.Context ctx);

    T visit(FieldInit.Context ctx);
}
