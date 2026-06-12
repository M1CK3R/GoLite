package com.olc1.visitor.interpreter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.olc1.ast.ASTNode;
import com.olc1.ast.exp.Add;
import com.olc1.ast.exp.And;
import com.olc1.ast.exp.BoolLiteral;
import com.olc1.ast.exp.Decimal;
import com.olc1.ast.exp.Div;
import com.olc1.ast.exp.Equ;
import com.olc1.ast.exp.FmtPrintln;
import com.olc1.ast.exp.Greater;
import com.olc1.ast.exp.GreaterEqu;
import com.olc1.ast.exp.Integers;
import com.olc1.ast.exp.Lesser;
import com.olc1.ast.exp.LesserEqu;
import com.olc1.ast.exp.Mod;
import com.olc1.ast.exp.Mul;
import com.olc1.ast.exp.Negate;
import com.olc1.ast.exp.Nequ;
import com.olc1.ast.exp.NilLiteral;
import com.olc1.ast.exp.Not;
import com.olc1.ast.exp.Or;
import com.olc1.ast.exp.Paren;
import com.olc1.ast.exp.ReflectTypeOf;
import com.olc1.ast.exp.RuneLiteral;
import com.olc1.ast.exp.StrconvAtoi;
import com.olc1.ast.exp.StrconvParseFloat;
import com.olc1.ast.exp.StringLiteral;
import com.olc1.ast.exp.Sub;
import com.olc1.ast.exp.VarRef;
import com.olc1.ast.stm.Assign;
import com.olc1.ast.stm.BreakNode;
import com.olc1.ast.stm.ContinueNode;
import com.olc1.ast.stm.ElseIfPart;
import com.olc1.ast.stm.ElsePart;
import com.olc1.ast.stm.ExprStatment;
import com.olc1.ast.stm.ForNode;
import com.olc1.ast.stm.ForWhileNode;
import com.olc1.ast.stm.IfNode;
import com.olc1.ast.stm.Imprimir;
import com.olc1.ast.stm.MinusAssign;
import com.olc1.ast.stm.PlusAssign;
import com.olc1.ast.stm.ShortDecl;
import com.olc1.ast.stm.Statments;
import com.olc1.ast.stm.VarDecl;
import com.olc1.visitor.Visitor;
import com.olc1.visitor.interpreter.value.BoolValue;
import com.olc1.visitor.interpreter.value.DecimalValue;
import com.olc1.visitor.interpreter.value.IntValue;
import com.olc1.visitor.interpreter.value.RuneValue;
import com.olc1.visitor.interpreter.value.StringValue;
import com.olc1.visitor.interpreter.value.ValueWrapper;
import com.olc1.reports.SymbolEntry;
import com.olc1.visitor.interpreter.value.VoidValue;

public class InterpreterVisitor implements Visitor<ValueWrapper>{
    public String output = "";
    public final List<SymbolEntry> symbolTable = new ArrayList<>();
    private final ValueWrapper defaultVoid = new VoidValue(-1, -1);
    private final Map<String, ValueWrapper> variables = new HashMap<>();

    public ValueWrapper Visit(ASTNode node) {
        return node.accept(this);
    }


    // Para las literales
    @Override
    public ValueWrapper visit(Integers.Context ctx) {
        return new IntValue(ctx.value, ctx.line, ctx.column);
    }

    @Override
    public ValueWrapper visit(Decimal.Context ctx) {
        return new DecimalValue(ctx.value, ctx.line, ctx.column);
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
    public ValueWrapper visit(NilLiteral.Context ctx) {
        return new VoidValue(ctx.line, ctx.column); // o crea NilValue si lo tienes
    }

    @Override
    public ValueWrapper visit(RuneLiteral.Context ctx) {
        return new RuneValue(ctx.value, ctx.line, ctx.column);
    }

    // Para las expresiones aritmeticas
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
    public ValueWrapper visit(Mod.Context ctx) {
        ValueWrapper left  = Visit(ctx.left);
        ValueWrapper right = Visit(ctx.right);
        // Solo int % int
        if (left instanceof IntValue l && right instanceof IntValue r) {
            return new IntValue(l.value() % r.value(), l.line(), l.column());
        }
        throw new RuntimeException("Operacion invalida: " + left.getTypeName() + " % " + right.getTypeName());
    }

    // Para las expresiones logicas y relacionales

    private ValueWrapper compare(ASTNode leftNode, ASTNode rightNode, String op,
                                java.util.function.BiPredicate<Integer,Integer> intCmp,
                                java.util.function.BiPredicate<Double,Double> floatCmp) {
        ValueWrapper left  = Visit(leftNode);
        ValueWrapper right = Visit(rightNode);
        if (left instanceof IntValue l && right instanceof IntValue r)
            return new BoolValue(intCmp.test(l.value(), r.value()), l.line(), l.column());
        if (left instanceof IntValue l && right instanceof DecimalValue r)
            return new BoolValue(floatCmp.test((double)l.value(), r.value()), l.line(), l.column());
        if (left instanceof DecimalValue l && right instanceof IntValue r)
            return new BoolValue(floatCmp.test(l.value(), (double)r.value()), l.line(), l.column());
        if (left instanceof DecimalValue l && right instanceof DecimalValue r)
            return new BoolValue(floatCmp.test(l.value(), r.value()), l.line(), l.column());
        throw new RuntimeException("Operacion invalida: " + left.getTypeName() + " " + op + " " + right.getTypeName());
    }

    @Override
    public ValueWrapper visit(Equ.Context ctx) {
        ValueWrapper left  = Visit(ctx.left);
        ValueWrapper right = Visit(ctx.right);
        // conversiones implícitas según el enunciado
        if (left instanceof IntValue l && right instanceof IntValue r)
            return new BoolValue(l.value() == r.value(), l.line(), l.column());
        if (left instanceof IntValue l && right instanceof DecimalValue r)
            return new BoolValue(l.value() == r.value(), l.line(), l.column());
        if (left instanceof DecimalValue l && right instanceof IntValue r)
            return new BoolValue(l.value() == r.value(), l.line(), l.column());
        if (left instanceof DecimalValue l && right instanceof DecimalValue r)
            return new BoolValue(l.value() == r.value(), l.line(), l.column());
        if (left instanceof BoolValue l && right instanceof BoolValue r)
            return new BoolValue(l.value() == r.value(), l.line(), l.column());
        if (left instanceof StringValue l && right instanceof StringValue r)
            return new BoolValue(l.value().equals(r.value()), l.line(), l.column());
        throw new RuntimeException("Operacion invalida: " + left.getTypeName() + " == " + right.getTypeName());
    }

    @Override
    public ValueWrapper visit(Nequ.Context ctx) {
        ValueWrapper left  = Visit(ctx.left);
        ValueWrapper right = Visit(ctx.right);
        // similar a Eq pero negado
        ValueWrapper eq = visit(new Equ.Context(new Equ(ctx.left, ctx.right)));
        if (eq instanceof BoolValue b) {
            return new BoolValue(!b.value(), b.line(), b.column());
        }
        throw new RuntimeException("Error en comparación !=");
    }

    @Override
    public ValueWrapper visit(Lesser.Context ctx) {
        return compare(ctx.left, ctx.right, "<", (a,b) -> a < b, (a,b) -> a < b);
    }

    @Override
    public ValueWrapper visit(LesserEqu.Context ctx) {
        return compare(ctx.left, ctx.right, "<=", (a,b) -> a <= b, (a,b) -> a <= b);
    }

    @Override
    public ValueWrapper visit(Greater.Context ctx) {
        return compare(ctx.left, ctx.right, ">", (a,b) -> a > b, (a,b) -> a > b);
    }

    @Override
    public ValueWrapper visit(GreaterEqu.Context ctx) {
        return compare(ctx.left, ctx.right, ">=", (a,b) -> a >= b, (a,b) -> a >= b);
    }

    @Override
    public ValueWrapper visit(And.Context ctx) {
        ValueWrapper left = Visit(ctx.left);
        if (left instanceof BoolValue l && !l.value()) return new BoolValue(false, l.line(), l.column());
        ValueWrapper right = Visit(ctx.right);
        if (left instanceof BoolValue l && right instanceof BoolValue r)
            return new BoolValue(l.value() && r.value(), l.line(), l.column());
        throw new RuntimeException("Operacion && requiere booleanos");
    }

    @Override
    public ValueWrapper visit(Or.Context ctx) {
        ValueWrapper left = Visit(ctx.left);
        if (left instanceof BoolValue l && l.value()) return new BoolValue(true, l.line(), l.column());
        ValueWrapper right = Visit(ctx.right);
        if (left instanceof BoolValue l && right instanceof BoolValue r)
            return new BoolValue(l.value() || r.value(), l.line(), l.column());
        throw new RuntimeException("Operacion || requiere booleanos");    
    }

    @Override
    public ValueWrapper visit(Not.Context ctx) {
        ValueWrapper val = Visit(ctx.expression);
        if (val instanceof BoolValue b)
            return new BoolValue(!b.value(), b.line(), b.column());
        throw new RuntimeException("Operacion ! requiere booleano");
    }

    // Expresion de agrupacion

    @Override
    public ValueWrapper visit(Paren.Context ctx) {
        return Visit(ctx.expression);
    }


    // Referencia a variables

    @Override
    public ValueWrapper visit(VarRef.Context ctx) {
        ValueWrapper val = variables.get(ctx.name);
        if (val == null) throw new RuntimeException("Variable no definida: " + ctx.name);
        return val;
    }


    @Override
    public ValueWrapper visit(Imprimir.Context ctx) {
        ValueWrapper value = Visit(ctx.expression);
        output += value.toString() + "\n";
        return defaultVoid;
    }

    // Sentencias
    @Override
    public ValueWrapper visit(Statments.Context ctx) {
        for (ASTNode statment : ctx.statements) {
            Visit(statment);
        }

        return defaultVoid;
    }

    // Declaraciones y asignaciones
    @Override
    public ValueWrapper visit(VarDecl.Context ctx) {
        ValueWrapper val;
        if (ctx.value != null) {                  // ← CORREGIDO: value en vez de expression
            val = Visit(ctx.value);
        } else {
            // Valor por defecto según tipo
            switch (ctx.type) {
                case "int":     val = new IntValue(0, ctx.line, ctx.column); break;
                case "float64": val = new DecimalValue(0.0, ctx.line, ctx.column); break;
                case "bool":    val = new BoolValue(false, ctx.line, ctx.column); break;
                case "string":  val = new StringValue("", ctx.line, ctx.column); break;
                case "rune":    val = new IntValue(0, ctx.line, ctx.column); break;  // rune como entero
                default: throw new RuntimeException("Tipo desconocido: " + ctx.type);
            }
        }
        variables.put(ctx.name, val);
        symbolTable.add(new SymbolEntry(symbolTable.size() + 1, ctx.name, ctx.type, ctx.line, ctx.column));
        return defaultVoid;
    }

    @Override
    public ValueWrapper visit(ShortDecl.Context ctx) {
        ValueWrapper val = Visit(ctx.value);
        variables.put(ctx.name, val);
        symbolTable.add(new SymbolEntry(symbolTable.size() + 1, ctx.name, val.getTypeName(), ctx.line, ctx.column));
        return defaultVoid;    
    }

    @Override
    public ValueWrapper visit(Assign.Context ctx) {
        ValueWrapper val = Visit(ctx.value);
        if (!variables.containsKey(ctx.name))
            throw new RuntimeException("Variable no declarada: " + ctx.name);
        variables.put(ctx.name, val);
        return defaultVoid;
    }

    @Override
    public ValueWrapper visit(PlusAssign.Context ctx) {
        ValueWrapper current = variables.get(ctx.name);
        ValueWrapper increment = Visit(ctx.value);
        ValueWrapper result;
        if (current instanceof IntValue i && increment instanceof IntValue inc) {
            result = new IntValue(i.value() + inc.value(), i.line(), i.column());
        } else if (current instanceof DecimalValue d && increment instanceof IntValue inc) {
            result = new DecimalValue(d.value() + inc.value(), d.line(), d.column());
        } else if (current instanceof DecimalValue d && increment instanceof DecimalValue inc) {
            result = new DecimalValue(d.value() + inc.value(), d.line(), d.column());
        } else if (current instanceof StringValue s && increment instanceof StringValue inc) {
            result = new StringValue(s.value() + inc.value(), s.line(), s.column());
        } else {
            throw new RuntimeException("Operacion += invalida: " + current.getTypeName() + " += " + increment.getTypeName());
        }
        variables.put(ctx.name, result);
        return defaultVoid;    
    }

    @Override
    public ValueWrapper visit(MinusAssign.Context ctx) {
        ValueWrapper current = variables.get(ctx.name);
        ValueWrapper decrement = Visit(ctx.value);
        ValueWrapper result;
        if (current instanceof IntValue i && decrement instanceof IntValue dec) {
            result = new IntValue(i.value() - dec.value(), i.line(), i.column());
        } else if (current instanceof DecimalValue d && decrement instanceof IntValue dec) {
            result = new DecimalValue(d.value() - dec.value(), d.line(), d.column());
        } else if (current instanceof DecimalValue d && decrement instanceof DecimalValue dec) {
            result = new DecimalValue(d.value() - dec.value(), d.line(), d.column());
        } else {
            throw new RuntimeException("Operacion -= invalida: " + current.getTypeName() + " -= " + decrement.getTypeName());
        }
        variables.put(ctx.name, result);
        return defaultVoid;    
    }

    // ifs y else

    @Override
    public ValueWrapper visit(IfNode.Context ctx) {
        ValueWrapper cond = Visit(ctx.condition);
        if (cond instanceof BoolValue b && b.value()) {
            Visit(ctx.body);
        } else if (ctx.elsePart != null) {
            Visit(ctx.elsePart);   // elsePart puede ser ElseIfPart o ElsePart
        }
        return defaultVoid;
    }
    
    @Override
    public ValueWrapper visit(ElseIfPart.Context ctx) {
        ValueWrapper cond = Visit(ctx.condition);
        if (cond instanceof BoolValue b && b.value()) {
            Visit(ctx.body);
        } else if (ctx.elsePart != null) {
            Visit(ctx.elsePart);
        }
        return defaultVoid;    
    }

    @Override
    public ValueWrapper visit(ElsePart.Context ctx) {
        return Visit(ctx.body);
    }

    //Bucles

    @Override
    public ValueWrapper visit(ForNode.Context ctx) {
        if (ctx.init != null) Visit(ctx.init);
        while (true) {
            ValueWrapper cond = Visit(ctx.condition);
            if (cond instanceof BoolValue b && !b.value()) break;
            try {
                Visit(ctx.body);
            } catch (BreakException e) {
                break;
            } catch (ContinueException e) {
                // saltar al incremento
            }
            if (ctx.increment != null) Visit(ctx.increment);
        }
        return defaultVoid;    }

    @Override
    public ValueWrapper visit(ForWhileNode.Context ctx) {
        while (true) {
            ValueWrapper cond = Visit(ctx.condition);
            if (cond instanceof BoolValue b && !b.value()) break;
            try {
                Visit(ctx.body);
            } catch (BreakException e) {
                break;
            } catch (ContinueException e) {
                // continuar
            }
        }
        return defaultVoid;    }

    @Override
    public ValueWrapper visit(BreakNode.Context ctx) {
        throw new BreakException();
    }

    @Override
    public ValueWrapper visit(ContinueNode.Context ctx) {
        throw new ContinueException();
    }

    private static class BreakException extends RuntimeException{}
    private static class ContinueException extends RuntimeException{}


    //Funciones embebidas
    @Override
    public ValueWrapper visit(FmtPrintln.Context ctx) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ctx.args.size(); i++) {
            ValueWrapper val = Visit(ctx.args.get(i));
            if (i > 0) sb.append(" ");
            sb.append(val.toString());
        }
        output += sb.toString() + "\n";
        return defaultVoid;
    }

    @Override
    public ValueWrapper visit(StrconvAtoi.Context ctx) {
        ValueWrapper arg = Visit(ctx.argument);
        if (arg instanceof StringValue s) {
            try {
                int num = Integer.parseInt(s.value());
                return new IntValue(num, s.line(), s.column());
            } catch (NumberFormatException e) {
                throw new RuntimeException("strconv.Atoi: no se pudo convertir '" + s.value() + "' a entero");
            }
        }
        throw new RuntimeException("strconv.Atoi espera un string");
    }

    @Override
    public ValueWrapper visit(StrconvParseFloat.Context ctx) {
        ValueWrapper arg = Visit(ctx.argument);
        if (arg instanceof StringValue s) {
            try {
                double num = Double.parseDouble(s.value());
                return new DecimalValue(num, s.line(), s.column());
            } catch (NumberFormatException e) {
                throw new RuntimeException("strconv.ParseFloat: no se pudo convertir '" + s.value() + "' a float64");
            }
        }
        throw new RuntimeException("strconv.ParseFloat espera un string");
    }

    @Override
    public ValueWrapper visit(ReflectTypeOf.Context ctx) {
        ValueWrapper arg = Visit(ctx.argument);
        return new StringValue(arg.getTypeName(), arg.line(), arg.column());
    }

    // Expresion como sentencia
    @Override
    public ValueWrapper visit(ExprStatment.Context ctx) {
        return Visit(ctx.expression);
    }



}

