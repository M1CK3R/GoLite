package com.olc1.visitor.interpreter;

import java.util.HashMap;
import java.util.Map;

import com.olc1.ast.ASTNode;
import com.olc1.visitor.Visitor;
import com.olc1.ast.exp.*;
import com.olc1.ast.stm.*;
import com.olc1.visitor.interpreter.value.*;

public class InterpreterVisitor implements Visitor<ValueWrapper>{
    public String output = "";
    private final ValueWrapper defaultVoid = new VoidValue(-1, -1);
    private final Map<String, ValueWrapper> variables = new HashMap<>();

    public ValueWrapper Visit(ASTNode node) {
        return node.accept(this);
    }

    @Override
    public ValueWrapper visit(Integers.Context ctx) {
        return new IntValue(ctx.value, ctx.line, ctx.column);
    }

    @Override
    public ValueWrapper visit(Decimal.Context ctx) {
        return new DecimalValue(ctx.value, ctx.line, ctx.column);
    }

    @Override
    public ValueWrapper visit(Add.Context ctx) {
        ValueWrapper left  = Visit(ctx.left);
        ValueWrapper right = Visit(ctx.right);

        return switch (left) {
            case IntValue     l when right instanceof IntValue     r -> new IntValue(l.value() + r.value(), l.line(), l.column());
            case IntValue     l when right instanceof DecimalValue r -> new DecimalValue(l.value() + r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof IntValue     r -> new DecimalValue(l.value() + r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof DecimalValue r -> new DecimalValue(l.value() + r.value(), l.line(), l.column());
            default -> throw new RuntimeException("Operacion invalida: " + left.getTypeName() + " + " + right.getTypeName());
        };
    }

    @Override
    public ValueWrapper visit(Sub.Context ctx) {
        ValueWrapper left  = Visit(ctx.left);
        ValueWrapper right = Visit(ctx.right);
        return switch (left) {
            case IntValue     l when right instanceof IntValue     r -> new IntValue((int)(l.value() - r.value()), l.line(), l.column());
            case IntValue     l when right instanceof DecimalValue r -> new DecimalValue(l.value() - r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof IntValue     r -> new DecimalValue(l.value() - r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof DecimalValue r -> new DecimalValue(l.value() - r.value(), l.line(), l.column());
            default -> throw new RuntimeException("Operacion invalida: " + left.getTypeName() + " - " + right.getTypeName());
        };
    }

    @Override
    public ValueWrapper visit(Mul.Context ctx) {
        ValueWrapper left  = Visit(ctx.left);
        ValueWrapper right = Visit(ctx.right);
        return switch (left) {
            case IntValue     l when right instanceof IntValue     r -> new IntValue(l.value() * r.value(), l.line(), l.column());
            case IntValue     l when right instanceof DecimalValue r -> new DecimalValue(l.value() * r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof IntValue     r -> new DecimalValue(l.value() * r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof DecimalValue r -> new DecimalValue(l.value() * r.value(), l.line(), l.column());
            default -> throw new RuntimeException("Operacion invalida: " + left.getTypeName() + " * " + right.getTypeName());
        };
    }

    @Override
    public ValueWrapper visit(Div.Context ctx) {
        ValueWrapper left  = Visit(ctx.left);
        ValueWrapper right = Visit(ctx.right);
        return switch (left) {
            case IntValue     l when right instanceof IntValue     r -> new IntValue(l.value() / r.value(), l.line(), l.column());
            case IntValue     l when right instanceof DecimalValue r -> new DecimalValue(l.value() / r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof IntValue     r -> new DecimalValue(l.value() / r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof DecimalValue r -> new DecimalValue(l.value() / r.value(), l.line(), l.column());
            default -> throw new RuntimeException("Operacion invalida: " + left.getTypeName() + " / " + right.getTypeName());
        };
    }

    @Override
    public ValueWrapper visit(Negate.Context ctx) {
        ValueWrapper operand = Visit(ctx.expression);
        return switch (operand) {
            case IntValue     v -> new IntValue(-v.value(), v.line(), v.column());
            case DecimalValue v -> new DecimalValue(-v.value(), v.line(), v.column());
            default -> throw new RuntimeException("Operacion invalida: -" + operand.getTypeName());
        };
    }

    @Override
    public ValueWrapper visit(Imprimir.Context ctx) {
        ValueWrapper value = Visit(ctx.expression);
        output += value.toString() + "\n";
        return defaultVoid;
    }

    @Override
    public ValueWrapper visit(Statments.Context ctx) {
        for (ASTNode statment : ctx.statements) {
            Visit(statment);
        }

        return defaultVoid;
    }

    @Override
    public ValueWrapper visit(Paren.Context ctx) {
        return Visit(ctx.expression);
    }

    @Override
    public ValueWrapper visit(BoolLiteral.Context ctx) {
        return new BoolValue(ctx.value, ctx.line, ctx.column);
    }

    @Override
    public ValueWrapper visit(StringLiteral.Context ctx) {
        return new StringValue(ctx.value, ctx.line, ctx.column);
    }

    @Override
    public ValueWrapper visit(VarRef.Context ctx) {
        ValueWrapper val = variables.get(ctx.name);
        if (val == null) throw new RuntimeException("Variable no definida: " + ctx.name);
        return val;
    }

    @Override
    public ValueWrapper visit(Assign.Context ctx) {
        ValueWrapper val = Visit(ctx.value);
        variables.put(ctx.name, val);
        return defaultVoid;
    }

    @Override
    public ValueWrapper visit(IfNode.Context ctx) {
        ValueWrapper cond = Visit(ctx.condition);
        if (cond instanceof BoolValue b && b.value()) {
            Visit(ctx.body);
        }
        return defaultVoid;
    }
    
    @Override
    public ValueWrapper visit(Mod.Context ctx) {
        throw new UnsupportedOperationException("Operación % no implementada aún");
    }

    @Override
    public ValueWrapper visit(Eq.Context ctx) {
        throw new UnsupportedOperationException("Operación == no implementada aún");
    }

    @Override
    public ValueWrapper visit(Neq.Context ctx) {
        throw new UnsupportedOperationException("Operación != no implementada aún");
    }

    @Override
    public ValueWrapper visit(Lt.Context ctx) {
        throw new UnsupportedOperationException("Operación < no implementada aún");
    }

    @Override
    public ValueWrapper visit(Le.Context ctx) {
        throw new UnsupportedOperationException("Operación <= no implementada aún");
    }

    @Override
    public ValueWrapper visit(Gt.Context ctx) {
        throw new UnsupportedOperationException("Operación > no implementada aún");
    }

    @Override
    public ValueWrapper visit(Ge.Context ctx) {
        throw new UnsupportedOperationException("Operación >= no implementada aún");
    }

    @Override
    public ValueWrapper visit(And.Context ctx) {
        throw new UnsupportedOperationException("Operación && no implementada aún");
    }

    @Override
    public ValueWrapper visit(Or.Context ctx) {
        throw new UnsupportedOperationException("Operación || no implementada aún");
    }

    @Override
    public ValueWrapper visit(Not.Context ctx) {
        throw new UnsupportedOperationException("Operación ! no implementada aún");
    }

    @Override
    public ValueWrapper visit(NilLiteral.Context ctx) {
        return new VoidValue(ctx.line, ctx.column); // o crea NilValue si lo tienes
    }

    @Override
    public ValueWrapper visit(VarDecl.Context ctx) {
        // Implementar declaración de variables
        ValueWrapper value = ctx.expression != null ? Visit(ctx.expression) : defaultVoid;
        variables.put(ctx.name, value);
        return defaultVoid;
    }

    @Override
    public ValueWrapper visit(ShortDecl.Context ctx) {
        return visit((VarDecl.Context) ctx); // usualmente similar a VarDecl
    }

    @Override
    public ValueWrapper visit(PlusAssign.Context ctx) {
        throw new UnsupportedOperationException("+= no implementado aún");
    }

    @Override
    public ValueWrapper visit(MinusAssign.Context ctx) {
        throw new UnsupportedOperationException("-= no implementado aún");
    }

    @Override
    public ValueWrapper visit(ElseIfPart.Context ctx) {
        throw new UnsupportedOperationException("else if no implementado aún");
    }

    @Override
    public ValueWrapper visit(ElsePart.Context ctx) {
        throw new UnsupportedOperationException("else no implementado aún");
    }

    @Override
    public ValueWrapper visit(ForNode.Context ctx) {
        throw new UnsupportedOperationException("for no implementado aún");
    }

    @Override
    public ValueWrapper visit(ForWhileNode.Context ctx) {
        throw new UnsupportedOperationException("for while no implementado aún");
    }

    @Override
    public ValueWrapper visit(BreakNode.Context ctx) {
        throw new UnsupportedOperationException("break no implementado aún");
    }

    @Override
    public ValueWrapper visit(ContinueNode.Context ctx) {
        throw new UnsupportedOperationException("continue no implementado aún");
    }

    @Override
    public ValueWrapper visit(ExprStmt.Context ctx) {
        return Visit(ctx.expression);
    }

    @Override
    public ValueWrapper visit(FmtPrintln.Context ctx) {
        ValueWrapper value = Visit(ctx.expression);
        output += value.toString() + "\n";
        return defaultVoid;
    }

    @Override
    public ValueWrapper visit(StrconvAtoi.Context ctx) {
        throw new UnsupportedOperationException("strconv.Atoi no implementado aún");
    }

    @Override
    public ValueWrapper visit(StrconvParseFloat.Context ctx) {
        throw new UnsupportedOperationException("strconv.ParseFloat no implementado aún");
    }

    @Override
    public ValueWrapper visit(ReflectTypeOf.Context ctx) {
        throw new UnsupportedOperationException("reflect.TypeOf no implementado aún");
    }
}
