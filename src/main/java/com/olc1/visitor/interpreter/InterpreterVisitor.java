package com.olc1.visitor.interpreter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.olc1.ast.ASTNode;
import com.olc1.ast.exp.*;
import com.olc1.ast.stm.*;
import com.olc1.visitor.Visitor;
import com.olc1.visitor.interpreter.value.*;
import com.olc1.reports.SymbolEntry;
import com.olc1.reports.ErrorCollector;

public class InterpreterVisitor implements Visitor<ValueWrapper> {
    public String output = "";
    public final List<SymbolEntry> symbolTable = new ArrayList<>();
    private final ValueWrapper defaultVoid = new VoidValue(-1, -1);

    public static class Environment {
        private final Environment parent;
        private final Map<String, ValueWrapper> variables = new HashMap<>();

        public Environment(Environment parent) {
            this.parent = parent;
        }

        public void define(String name, ValueWrapper value) {
            variables.put(name, value);
        }

        public ValueWrapper get(String name) {
            if (variables.containsKey(name)) {
                return variables.get(name);
            }
            if (parent != null) {
                return parent.get(name);
            }
            return null;
        }

        public boolean assign(String name, ValueWrapper value) {
            if (variables.containsKey(name)) {
                variables.put(name, value);
                return true;
            }
            if (parent != null) {
                return parent.assign(name, value);
            }
            return false;
        }
    }

    private Environment currentEnvironment = new Environment(null);
    private final Environment globalEnvironment = currentEnvironment;
    private final Map<String, StructDeclNode.Context> structsMap = new HashMap<>();
    private final Map<String, FuncDeclNode.Context> funcsMap = new HashMap<>();
    private final Map<String, MethodDeclNode.Context> methodsMap = new HashMap<>();
    private String expectedType = null;

    private static class ReturnException extends RuntimeException {
        public final ValueWrapper value;

        public ReturnException(ValueWrapper value) {
            this.value = value;
        }
    }

    public ValueWrapper Visit(ASTNode node) {
        return node.accept(this);
    }

    private ValueWrapper createDefaultValue(String type, int line, int column) {
        if (type == null)
            return defaultVoid;
        if (type.startsWith("[]")) {
            String elemType = type.substring(2);
            return new SliceValue(elemType, line, column);
        }
        return switch (type) {
            case "int" -> new IntValue(0, line, column);
            case "float64" -> new DecimalValue(0.0, line, column);
            case "bool" -> new BoolValue(false, line, column);
            case "string" -> new StringValue("", line, column);
            case "rune" -> new IntValue(0, line, column);
            default -> {
                StructDeclNode.Context structDecl = structsMap.get(type);
                if (structDecl == null) {
                    throw new RuntimeException("Tipo desconocido: " + type);
                }
                StructValue structVal = new StructValue(type, line, column);
                for (FieldDecl field : structDecl.fields) {
                    structVal.fields().put(field.name(), createDefaultValue(field.type(), line, column));
                }
                yield structVal;
            }
        };
    }

    private ValueWrapper evaluateBraceLiteral(List<ASTNode> elements, String type, int line, int column) {
        if (type == null) {
            boolean isStruct = elements.stream().anyMatch(e -> e instanceof FieldInit);
            if (isStruct) {
                throw new RuntimeException("No se puede inferir el tipo del struct literal sin contexto de tipo");
            } else {
                String elemType = "int";
                if (!elements.isEmpty()) {
                    ValueWrapper first = Visit(elements.get(0));
                    elemType = first.getTypeName();
                }
                SliceValue slice = new SliceValue(elemType, line, column);
                for (ASTNode elem : elements) {
                    slice.getElements().add(Visit(elem));
                }
                return slice;
            }
        }

        if (type.startsWith("[]")) {
            String elemType = type.substring(2);
            SliceValue slice = new SliceValue(elemType, line, column);
            String prevExpected = this.expectedType;
            this.expectedType = elemType;
            try {
                for (ASTNode elem : elements) {
                    slice.getElements().add(Visit(elem));
                }
            } finally {
                this.expectedType = prevExpected;
            }
            return slice;
        }

        StructDeclNode.Context structDecl = structsMap.get(type);
        if (structDecl == null) {
            throw new RuntimeException("Struct no definido: " + type);
        }
        StructValue structVal = new StructValue(type, line, column);
        for (FieldDecl f : structDecl.fields) {
            structVal.fields().put(f.name(), createDefaultValue(f.type(), line, column));
        }

        for (int i = 0; i < elements.size(); i++) {
            ASTNode elem = elements.get(i);
            if (elem instanceof FieldInit fi) {
                FieldDecl fDecl = structDecl.fields.stream()
                        .filter(f -> f.name().equals(fi.name()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Campo " + fi.name() + " no existe en struct " + type));
                String prevExpected = this.expectedType;
                this.expectedType = fDecl.type();
                try {
                    structVal.fields().put(fi.name(), Visit(fi.value()));
                } finally {
                    this.expectedType = prevExpected;
                }
            } else {
                if (i >= structDecl.fields.size()) {
                    throw new RuntimeException("Demasiados valores para inicializar struct " + type);
                }
                FieldDecl fDecl = structDecl.fields.get(i);
                String prevExpected = this.expectedType;
                this.expectedType = fDecl.type();
                try {
                    structVal.fields().put(fDecl.name(), Visit(elem));
                } finally {
                    this.expectedType = prevExpected;
                }
            }
        }
        return structVal;
    }

    private void assignToTarget(ASTNode target, ValueWrapper value) {
        if (target instanceof VarRef vr) {
            if (!currentEnvironment.assign(vr.getName(), value)) {
                throw new RuntimeException("Variable no declarada: " + vr.getName());
            }
        } else if (target instanceof FieldAccessNode fa) {
            ValueWrapper structVal = Visit(fa.getTarget());
            if (structVal instanceof StructValue sv) {
                if (!sv.fields().containsKey(fa.getField())) {
                    throw new RuntimeException("Campo " + fa.getField() + " no existe en struct " + sv.getTypeName());
                }
                sv.fields().put(fa.getField(), value);
            } else {
                throw new RuntimeException(
                        "El objetivo del acceso a campo no es un struct: " + structVal.getTypeName());
            }
        } else if (target instanceof SliceAccessNode sa) {
            ValueWrapper sliceVal = Visit(sa.getTarget());
            if (sliceVal instanceof SliceValue sv) {
                ValueWrapper idxVal = Visit(sa.getIndex());
                if (!(idxVal instanceof IntValue iv)) {
                    throw new RuntimeException("Índice de slice debe ser entero");
                }
                int idx = iv.value();
                if (idx < 0 || idx >= sv.getElements().size()) {
                    throw new RuntimeException(
                            "Índice fuera de rango: " + idx + " (tamaño " + sv.getElements().size() + ")");
                }
                sv.getElements().set(idx, value);
            } else {
                throw new RuntimeException("El objetivo de la indexación no es un slice: " + sliceVal.getTypeName());
            }
        } else {
            throw new RuntimeException("LHS de asignación inválido");
        }
    }

    private String getTargetType(ASTNode target) {
        if (target instanceof VarRef vr) {
            ValueWrapper val = currentEnvironment.get(vr.getName());
            return val != null ? val.getTypeName() : null;
        } else if (target instanceof FieldAccessNode fa) {
            ValueWrapper structVal = Visit(fa.getTarget());
            if (structVal instanceof StructValue sv) {
                ValueWrapper fieldVal = sv.fields().get(fa.getField());
                return fieldVal != null ? fieldVal.getTypeName() : null;
            }
        } else if (target instanceof SliceAccessNode sa) {
            ValueWrapper sliceVal = Visit(sa.getTarget());
            if (sliceVal instanceof SliceValue sv) {
                return sv.getElementType();
            }
        }
        return null;
    }

    private ValueWrapper executeFunction(FuncDeclNode.Context func, List<ValueWrapper> args) {
        Environment funcEnv = new Environment(globalEnvironment);
        for (int i = 0; i < func.params.size(); i++) {
            Param param = func.params.get(i);
            ValueWrapper arg = args.get(i);
            funcEnv.define(param.name(), arg);
        }
        Environment oldEnv = currentEnvironment;
        currentEnvironment = funcEnv;
        try {
            Visit(func.body);
        } catch (ReturnException e) {
            return e.value;
        } finally {
            currentEnvironment = oldEnv;
        }
        return defaultVoid;
    }

    private ValueWrapper executeMethod(MethodDeclNode.Context method, ValueWrapper receiverVal,
            List<ValueWrapper> args) {
        Environment methodEnv = new Environment(globalEnvironment);
        methodEnv.define(method.receiver.name(), receiverVal);
        for (int i = 0; i < method.params.size(); i++) {
            Param param = method.params.get(i);
            ValueWrapper arg = args.get(i);
            methodEnv.define(param.name(), arg);
        }
        Environment oldEnv = currentEnvironment;
        currentEnvironment = methodEnv;
        try {
            Visit(method.body);
        } catch (ReturnException e) {
            return e.value;
        } finally {
            currentEnvironment = oldEnv;
        }
        return defaultVoid;
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
        ValueWrapper left = Visit(ctx.left);
        ValueWrapper right = Visit(ctx.right);

        return switch (left) {
            case IntValue l when right instanceof IntValue r ->
                new IntValue(l.value() + r.value(), l.line(), l.column());
            case IntValue l when right instanceof DecimalValue r ->
                new DecimalValue(l.value() + r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof IntValue r ->
                new DecimalValue(l.value() + r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof DecimalValue r ->
                new DecimalValue(l.value() + r.value(), l.line(), l.column());
            case StringValue l when right instanceof StringValue r ->
                new StringValue(l.value() + r.value(), l.line(), l.column());
            default ->
                throw new RuntimeException("Operacion invalida: " + left.getTypeName() + " + " + right.getTypeName());
        };
    }

    @Override
    public ValueWrapper visit(Sub.Context ctx) {
        ValueWrapper left = Visit(ctx.left);
        ValueWrapper right = Visit(ctx.right);
        return switch (left) {
            case IntValue l when right instanceof IntValue r ->
                new IntValue((int) (l.value() - r.value()), l.line(), l.column());
            case IntValue l when right instanceof DecimalValue r ->
                new DecimalValue(l.value() - r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof IntValue r ->
                new DecimalValue(l.value() - r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof DecimalValue r ->
                new DecimalValue(l.value() - r.value(), l.line(), l.column());
            default ->
                throw new RuntimeException("Operacion invalida: " + left.getTypeName() + " - " + right.getTypeName());
        };
    }

    @Override
    public ValueWrapper visit(Mul.Context ctx) {
        ValueWrapper left = Visit(ctx.left);
        ValueWrapper right = Visit(ctx.right);
        return switch (left) {
            case IntValue l when right instanceof IntValue r ->
                new IntValue(l.value() * r.value(), l.line(), l.column());
            case IntValue l when right instanceof DecimalValue r ->
                new DecimalValue(l.value() * r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof IntValue r ->
                new DecimalValue(l.value() * r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof DecimalValue r ->
                new DecimalValue(l.value() * r.value(), l.line(), l.column());
            default ->
                throw new RuntimeException("Operacion invalida: " + left.getTypeName() + " * " + right.getTypeName());
        };
    }

    @Override
    public ValueWrapper visit(Div.Context ctx) {
        ValueWrapper left = Visit(ctx.left);
        ValueWrapper right = Visit(ctx.right);
        return switch (left) {
            case IntValue l when right instanceof IntValue r ->
                new IntValue(l.value() / r.value(), l.line(), l.column());
            case IntValue l when right instanceof DecimalValue r ->
                new DecimalValue(l.value() / r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof IntValue r ->
                new DecimalValue(l.value() / r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof DecimalValue r ->
                new DecimalValue(l.value() / r.value(), l.line(), l.column());
            default ->
                throw new RuntimeException("Operacion invalida: " + left.getTypeName() + " / " + right.getTypeName());
        };
    }

    @Override
    public ValueWrapper visit(Negate.Context ctx) {
        ValueWrapper operand = Visit(ctx.expression);
        return switch (operand) {
            case IntValue v -> new IntValue(-v.value(), v.line(), v.column());
            case DecimalValue v -> new DecimalValue(-v.value(), v.line(), v.column());
            default -> throw new RuntimeException("Operacion invalida: -" + operand.getTypeName());
        };
    }

    @Override
    public ValueWrapper visit(Mod.Context ctx) {
        ValueWrapper left = Visit(ctx.left);
        ValueWrapper right = Visit(ctx.right);
        // Solo int % int
        if (left instanceof IntValue l && right instanceof IntValue r) {
            return new IntValue(l.value() % r.value(), l.line(), l.column());
        }
        throw new RuntimeException("Operacion invalida: " + left.getTypeName() + " % " + right.getTypeName());
    }

    // Para las expresiones logicas y relacionales

    private ValueWrapper compare(ASTNode leftNode, ASTNode rightNode, String op,
            java.util.function.BiPredicate<Integer, Integer> intCmp,
            java.util.function.BiPredicate<Double, Double> floatCmp) {
        ValueWrapper left = Visit(leftNode);
        ValueWrapper right = Visit(rightNode);
        if (left instanceof IntValue l && right instanceof IntValue r)
            return new BoolValue(intCmp.test(l.value(), r.value()), l.line(), l.column());
        if (left instanceof IntValue l && right instanceof DecimalValue r)
            return new BoolValue(floatCmp.test((double) l.value(), r.value()), l.line(), l.column());
        if (left instanceof DecimalValue l && right instanceof IntValue r)
            return new BoolValue(floatCmp.test(l.value(), (double) r.value()), l.line(), l.column());
        if (left instanceof DecimalValue l && right instanceof DecimalValue r)
            return new BoolValue(floatCmp.test(l.value(), r.value()), l.line(), l.column());
        if (left instanceof RuneValue l && right instanceof RuneValue r)
            return new BoolValue(intCmp.test((int) l.value(), (int) r.value()), l.line(), l.column());
        if (left instanceof RuneValue l && right instanceof IntValue r)
            return new BoolValue(intCmp.test((int) l.value(), r.value()), l.line(), l.column());
        if (left instanceof IntValue l && right instanceof RuneValue r)
            return new BoolValue(intCmp.test(l.value(), (int) r.value()), l.line(), l.column());
        throw new RuntimeException("Operacion invalida: " + left.getTypeName() + " " + op + " " + right.getTypeName());
    }

    private boolean isEqual(ValueWrapper left, ValueWrapper right) {
        if (left instanceof IntValue l && right instanceof IntValue r)
            return l.value() == r.value();
        if (left instanceof IntValue l && right instanceof DecimalValue r)
            return l.value() == r.value();
        if (left instanceof DecimalValue l && right instanceof IntValue r)
            return l.value() == r.value();
        if (left instanceof DecimalValue l && right instanceof DecimalValue r)
            return l.value() == r.value();
        if (left instanceof BoolValue l && right instanceof BoolValue r)
            return l.value() == r.value();
        if (left instanceof StringValue l && right instanceof StringValue r)
            return l.value().equals(r.value());
        if (left instanceof RuneValue l && right instanceof RuneValue r)
            return l.value() == r.value();
        if (left instanceof RuneValue l && right instanceof IntValue r)
            return (int) l.value() == r.value();
        if (left instanceof IntValue l && right instanceof RuneValue r)
            return l.value() == (int) r.value();
        throw new RuntimeException("Operacion invalida: " + left.getTypeName() + " == " + right.getTypeName());
    }

    @Override
    public ValueWrapper visit(Equ.Context ctx) {
        ValueWrapper left = Visit(ctx.left);
        ValueWrapper right = Visit(ctx.right);
        return new BoolValue(isEqual(left, right), left.line(), left.column());
    }

    @Override
    public ValueWrapper visit(Nequ.Context ctx) {
        ValueWrapper left = Visit(ctx.left);
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
        return compare(ctx.left, ctx.right, "<", (a, b) -> a < b, (a, b) -> a < b);
    }

    @Override
    public ValueWrapper visit(LesserEqu.Context ctx) {
        return compare(ctx.left, ctx.right, "<=", (a, b) -> a <= b, (a, b) -> a <= b);
    }

    @Override
    public ValueWrapper visit(Greater.Context ctx) {
        return compare(ctx.left, ctx.right, ">", (a, b) -> a > b, (a, b) -> a > b);
    }

    @Override
    public ValueWrapper visit(GreaterEqu.Context ctx) {
        return compare(ctx.left, ctx.right, ">=", (a, b) -> a >= b, (a, b) -> a >= b);
    }

    @Override
    public ValueWrapper visit(And.Context ctx) {
        ValueWrapper left = Visit(ctx.left);
        if (left instanceof BoolValue l && !l.value())
            return new BoolValue(false, l.line(), l.column());
        ValueWrapper right = Visit(ctx.right);
        if (left instanceof BoolValue l && right instanceof BoolValue r)
            return new BoolValue(l.value() && r.value(), l.line(), l.column());
        throw new RuntimeException("Operacion && requiere booleanos");
    }

    @Override
    public ValueWrapper visit(Or.Context ctx) {
        ValueWrapper left = Visit(ctx.left);
        if (left instanceof BoolValue l && l.value())
            return new BoolValue(true, l.line(), l.column());
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
        ValueWrapper val = currentEnvironment.get(ctx.name);
        if (val == null)
            throw new RuntimeException("Variable no definida: " + ctx.name);
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
        Environment oldEnv = currentEnvironment;
        currentEnvironment = new Environment(oldEnv);
        try {
            for (ASTNode statment : ctx.statements) {
                try {
                    Visit(statment);
                } catch (BreakException | ContinueException | ReturnException e) {
                    throw e;
                } catch (Exception e) {
                    int[] pos = getLineCol(statment);
                    ErrorCollector.addError("semántico", e.getMessage(), pos[0], pos[1]);
                }
            }
        } finally {
            currentEnvironment = oldEnv;
        }

        if (oldEnv.parent == null) {
            FuncDeclNode.Context mainFunc = funcsMap.get("main");
            if (mainFunc != null) {
                funcsMap.remove("main"); // prevent recursion/loops if called inside main
                try {
                    executeFunction(mainFunc, new ArrayList<>());
                } finally {
                    funcsMap.put("main", mainFunc);
                }
            }
        }

        return defaultVoid;
    }

    private int[] getLineCol(ASTNode node) {
        try {
            java.lang.reflect.Field lineField = node.getClass().getDeclaredField("line");
            java.lang.reflect.Field colField = node.getClass().getDeclaredField("column");
            lineField.setAccessible(true);
            colField.setAccessible(true);
            return new int[] { (int) lineField.get(node), (int) colField.get(node) };
        } catch (Exception e) {
            return new int[] { 0, 0 };
        }
    }

    // Declaraciones y asignaciones
    @Override
    public ValueWrapper visit(VarDecl.Context ctx) {
        ValueWrapper val;
        if (ctx.value != null) {
            String prev = this.expectedType;
            this.expectedType = ctx.type;
            try {
                val = Visit(ctx.value);
            } finally {
                this.expectedType = prev;
            }
        } else {
            val = createDefaultValue(ctx.type, ctx.line, ctx.column);
        }
        currentEnvironment.define(ctx.name, val);
        symbolTable.add(new SymbolEntry(symbolTable.size() + 1, ctx.name, ctx.type, ctx.line, ctx.column));
        return defaultVoid;
    }

    @Override
    public ValueWrapper visit(ShortDecl.Context ctx) {
        String prev = this.expectedType;
        this.expectedType = null;
        ValueWrapper val;
        try {
            val = Visit(ctx.value);
        } finally {
            this.expectedType = prev;
        }
        currentEnvironment.define(ctx.name, val);
        symbolTable.add(new SymbolEntry(symbolTable.size() + 1, ctx.name, val.getTypeName(), ctx.line, ctx.column));
        return defaultVoid;
    }

    @Override
    public ValueWrapper visit(Assign.Context ctx) {
        String type = getTargetType(ctx.target);
        String prev = this.expectedType;
        this.expectedType = type;
        ValueWrapper val;
        try {
            val = Visit(ctx.value);
        } finally {
            this.expectedType = prev;
        }
        assignToTarget(ctx.target, val);
        return defaultVoid;
    }

    @Override
    public ValueWrapper visit(PlusAssign.Context ctx) {
        ValueWrapper current = Visit(ctx.target);
        String type = getTargetType(ctx.target);
        String prev = this.expectedType;
        this.expectedType = type;
        ValueWrapper increment;
        try {
            increment = Visit(ctx.value);
        } finally {
            this.expectedType = prev;
        }
        ValueWrapper result = switch (current) {
            case IntValue i when increment instanceof IntValue inc ->
                new IntValue(i.value() + inc.value(), i.line(), i.column());
            case DecimalValue d when increment instanceof IntValue inc ->
                new DecimalValue(d.value() + inc.value(), d.line(), d.column());
            case DecimalValue d when increment instanceof DecimalValue inc ->
                new DecimalValue(d.value() + inc.value(), d.line(), d.column());
            case StringValue s when increment instanceof StringValue inc ->
                new StringValue(s.value() + inc.value(), s.line(), s.column());
            default -> throw new RuntimeException(
                    "Operacion += invalida: " + current.getTypeName() + " += " + increment.getTypeName());
        };
        assignToTarget(ctx.target, result);
        return defaultVoid;
    }

    @Override
    public ValueWrapper visit(MinusAssign.Context ctx) {
        ValueWrapper current = Visit(ctx.target);
        String type = getTargetType(ctx.target);
        String prev = this.expectedType;
        this.expectedType = type;
        ValueWrapper decrement;
        try {
            decrement = Visit(ctx.value);
        } finally {
            this.expectedType = prev;
        }
        ValueWrapper result = switch (current) {
            case IntValue i when decrement instanceof IntValue dec ->
                new IntValue(i.value() - dec.value(), i.line(), i.column());
            case DecimalValue d when decrement instanceof IntValue dec ->
                new DecimalValue(d.value() - dec.value(), d.line(), d.column());
            case DecimalValue d when decrement instanceof DecimalValue dec ->
                new DecimalValue(d.value() - dec.value(), d.line(), d.column());
            default -> throw new RuntimeException(
                    "Operacion -= invalida: " + current.getTypeName() + " -= " + decrement.getTypeName());
        };
        assignToTarget(ctx.target, result);
        return defaultVoid;
    }

    // ifs y else

    @Override
    public ValueWrapper visit(IfNode.Context ctx) {
        ValueWrapper cond = Visit(ctx.condition);
        if (cond instanceof BoolValue b && b.value()) {
            Visit(ctx.body);
        } else if (ctx.elsePart != null) {
            Visit(ctx.elsePart); // elsePart puede ser ElseIfPart o ElsePart
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

    // Bucles

    @Override
    public ValueWrapper visit(ForNode.Context ctx) {
        Environment oldEnv = currentEnvironment;
        currentEnvironment = new Environment(oldEnv);
        try {
            if (ctx.init != null)
                Visit(ctx.init);
            while (true) {
                ValueWrapper cond = Visit(ctx.condition);
                if (cond instanceof BoolValue b && !b.value())
                    break;
                try {
                    Visit(ctx.body);
                } catch (BreakException e) {
                    break;
                } catch (ContinueException e) {
                    // saltar al incremento
                }
                if (ctx.increment != null)
                    Visit(ctx.increment);
            }
        } finally {
            currentEnvironment = oldEnv;
        }
        return defaultVoid;
    }

    @Override
    public ValueWrapper visit(ForWhileNode.Context ctx) {
        while (true) {
            ValueWrapper cond = Visit(ctx.condition);
            if (cond instanceof BoolValue b && !b.value())
                break;
            try {
                Visit(ctx.body);
            } catch (BreakException e) {
                break;
            } catch (ContinueException e) {
                // continuar
            }
        }
        return defaultVoid;
    }

    @Override
    public ValueWrapper visit(BreakNode.Context ctx) {
        throw new BreakException();
    }

    @Override
    public ValueWrapper visit(ContinueNode.Context ctx) {
        throw new ContinueException();
    }

    private static class BreakException extends RuntimeException {
    }

    private static class ContinueException extends RuntimeException {
    }

    // Funciones embebidas
    @Override
    public ValueWrapper visit(FmtPrintln.Context ctx) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ctx.args.size(); i++) {
            ValueWrapper val = Visit(ctx.args.get(i));
            if (i > 0)
                sb.append(" ");
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

    @Override
    public ValueWrapper visit(SwitchNode.Context ctx) {
        ValueWrapper switchVal = Visit(ctx.expression);
        CaseNode defaultCase = null;
        boolean matched = false;

        try {
            for (ASTNode caseNodeAST : ctx.cases) {
                CaseNode caseNode = (CaseNode) caseNodeAST;
                if (caseNode.getExpression() == null) {
                    defaultCase = caseNode;
                    continue;
                }

                ValueWrapper caseVal = Visit(caseNode.getExpression());
                if (isEqual(switchVal, caseVal)) {
                    Visit(caseNode.getBody());
                    matched = true;
                    break;
                }
            }

            if (!matched && defaultCase != null) {
                Visit(defaultCase.getBody());
            }
        } catch (BreakException e) {
            // Un break dentro de un switch termina la ejecución del switch
        }

        return defaultVoid;
    }

    @Override
    public ValueWrapper visit(CaseNode.Context ctx) {
        return defaultVoid;
    }

    @Override
    public ValueWrapper visit(FuncDeclNode.Context ctx) {
        funcsMap.put(ctx.name, ctx);
        return defaultVoid;
    }

    @Override
    public ValueWrapper visit(MethodDeclNode.Context ctx) {
        methodsMap.put(ctx.receiver.type() + "#" + ctx.name, ctx);
        return defaultVoid;
    }

    @Override
    public ValueWrapper visit(ReturnNode.Context ctx) {
        ValueWrapper val = ctx.value != null ? Visit(ctx.value) : defaultVoid;
        throw new ReturnException(val);
    }

    @Override
    public ValueWrapper visit(StructDeclNode.Context ctx) {
        structsMap.put(ctx.name, ctx);
        return defaultVoid;
    }

    @Override
    public ValueWrapper visit(FuncCallNode.Context ctx) {
        FuncDeclNode.Context func = funcsMap.get(ctx.name);
        if (func == null) {
            throw new RuntimeException("Función no definida: " + ctx.name);
        }
        List<ValueWrapper> argsVal = new ArrayList<>();
        for (ASTNode arg : ctx.args) {
            argsVal.add(Visit(arg));
        }
        return executeFunction(func, argsVal);
    }

    @Override
    public ValueWrapper visit(MethodCallNode.Context ctx) {
        ValueWrapper receiverVal = Visit(ctx.receiver);
        String recType = receiverVal.getTypeName();
        MethodDeclNode.Context method = methodsMap.get(recType + "#" + ctx.name);
        if (method == null) {
            throw new RuntimeException("Método " + ctx.name + " no definido para tipo " + recType);
        }
        List<ValueWrapper> argsVal = new ArrayList<>();
        for (ASTNode arg : ctx.args) {
            argsVal.add(Visit(arg));
        }
        return executeMethod(method, receiverVal, argsVal);
    }

    @Override
    public ValueWrapper visit(FieldAccessNode.Context ctx) {
        ValueWrapper targetVal = Visit(ctx.target);
        if (targetVal instanceof StructValue sv) {
            if (!sv.fields().containsKey(ctx.field)) {
                throw new RuntimeException("Campo " + ctx.field + " no existe en struct " + sv.getTypeName());
            }
            return sv.fields().get(ctx.field);
        }
        throw new RuntimeException("Acceso a campo inválido en tipo " + targetVal.getTypeName());
    }

    @Override
    public ValueWrapper visit(SliceAccessNode.Context ctx) {
        ValueWrapper targetVal = Visit(ctx.target);
        if (targetVal instanceof SliceValue sv) {
            ValueWrapper idxVal = Visit(ctx.index);
            if (!(idxVal instanceof IntValue iv)) {
                throw new RuntimeException("Índice de slice debe ser entero");
            }
            int idx = iv.value();
            if (idx < 0 || idx >= sv.getElements().size()) {
                throw new RuntimeException(
                        "Índice fuera de rango: " + idx + " (tamaño " + sv.getElements().size() + ")");
            }
            return sv.getElements().get(idx);
        }
        throw new RuntimeException("Indexación inválida en tipo " + targetVal.getTypeName());
    }

    @Override
    public ValueWrapper visit(SliceLiteralNode.Context ctx) {
        return evaluateBraceLiteral(ctx.elements, ctx.type, ctx.line, ctx.column);
    }

    @Override
    public ValueWrapper visit(BraceLiteralNode.Context ctx) {
        return evaluateBraceLiteral(ctx.elements, expectedType, ctx.line, ctx.column);
    }

    @Override
    public ValueWrapper visit(StructLiteralNode.Context ctx) {
        return evaluateBraceLiteral(ctx.elements, ctx.type, ctx.line, ctx.column);
    }

    @Override
    public ValueWrapper visit(AppendNode.Context ctx) {
        ValueWrapper sliceVal = Visit(ctx.slice);
        if (sliceVal instanceof SliceValue sv) {
            ValueWrapper val = Visit(ctx.value);
            List<ValueWrapper> newElems = new ArrayList<>(sv.getElements());
            newElems.add(val);
            return new SliceValue(sv.getElementType(), newElems, ctx.line, ctx.column);
        }
        throw new RuntimeException("append requiere un slice");
    }

    @Override
    public ValueWrapper visit(SlicesIndexNode.Context ctx) {
        ValueWrapper sliceVal = Visit(ctx.slice);
        if (sliceVal instanceof SliceValue sv) {
            ValueWrapper val = Visit(ctx.value);
            int idx = -1;
            for (int i = 0; i < sv.getElements().size(); i++) {
                if (isEqual(sv.getElements().get(i), val)) {
                    idx = i;
                    break;
                }
            }
            return new IntValue(idx, ctx.line, ctx.column);
        }
        throw new RuntimeException("slices.Index requiere un slice");
    }

    @Override
    public ValueWrapper visit(StringsJoinNode.Context ctx) {
        ValueWrapper sliceVal = Visit(ctx.slice);
        if (sliceVal instanceof SliceValue sv) {
            if (!sv.getElementType().equals("string")) {
                throw new RuntimeException("strings.Join requiere un slice de strings");
            }
            ValueWrapper sepVal = Visit(ctx.sep);
            if (!(sepVal instanceof StringValue sev)) {
                throw new RuntimeException("strings.Join requiere un separador de tipo string");
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < sv.getElements().size(); i++) {
                if (i > 0)
                    sb.append(sev.value());
                ValueWrapper item = sv.getElements().get(i);
                if (item instanceof StringValue s) {
                    sb.append(s.value());
                } else {
                    sb.append(item.toString());
                }
            }
            return new StringValue(sb.toString(), ctx.line, ctx.column);
        }
        throw new RuntimeException("strings.Join requiere un slice");
    }

    @Override
    public ValueWrapper visit(LenNode.Context ctx) {
        ValueWrapper val = Visit(ctx.expression);
        if (val instanceof SliceValue sv) {
            return new IntValue(sv.getElements().size(), ctx.line, ctx.column);
        } else if (val instanceof StringValue sv) {
            return new IntValue(sv.value().length(), ctx.line, ctx.column);
        }
        throw new RuntimeException("len requiere un slice o string");
    }
}
