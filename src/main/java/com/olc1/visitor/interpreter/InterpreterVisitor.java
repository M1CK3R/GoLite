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
import com.olc1.reports.GoLiteRuntimeError;
import com.olc1.reports.GoLiteError;

public class InterpreterVisitor implements Visitor<ValueWrapper> {
    public String output = "";
    public final List<SymbolEntry> symbolTable = new ArrayList<>();
    private final ValueWrapper defaultVoid = new VoidValue(-1, -1);
    public final List<GoLiteError> errors = new ArrayList<>();
    private int loopDepth = 0;

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
    private String currentScope = "Global";

    private void addSymbol(String id, String tipoSimbolo, String tipoDato, String ambito, int line, int column) {
        String normalizedTipoDato = tipoDato;
        if (tipoDato != null && tipoDato.startsWith("[]")) {
            normalizedTipoDato = "Slice";
        }
        for (SymbolEntry entry : symbolTable) {
            if (entry.getId().equals(id) && entry.getAmbito().equals(ambito) && entry.getLine() == line
                    && entry.getColumn() == column) {
                return;
            }
        }
        symbolTable.add(new SymbolEntry(id, tipoSimbolo, normalizedTipoDato, ambito, line, column));
    }

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
        switch (type) {
            case "int":
                return new IntValue(0, line, column);
            case "float64":
                return new DecimalValue(0.0, line, column);
            case "string":
                return new StringValue("", line, column);
            case "bool":
                return new BoolValue(false, line, column);
            case "rune":
                return new RuneValue((char) 0, line, column);
            default:
                // Si el tipo existe en tu mapa de structs (es un struct personalizado)
                if (structsMap.containsKey(type)) {
                    StructDeclNode.Context structDecl = structsMap.get(type);
                    StructValue structVal = new StructValue(type, line, column);

                    for (var field : structDecl.fields) {
                        if (structsMap.containsKey(field.type())) {
                            structVal.fields().put(field.name(), defaultVoid);
                        } else {
                            structVal.fields().put(field.name(), createDefaultValue(field.type(), line, column));
                        }
                    }
                    return structVal;
                }
                return defaultVoid;
        }
    }

    private ValueWrapper evaluateBraceLiteral(List<ASTNode> elements, String type, int line, int column) {
        if (type == null) {
            boolean isStruct = elements.stream().anyMatch(e -> e instanceof FieldInit);
            if (isStruct) {
                throw new GoLiteRuntimeError("No se puede inferir el tipo del struct literal sin contexto de tipo",
                        line, column);
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
            throw new GoLiteRuntimeError("Struct no definido: " + type, line, column);
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
                        .orElseThrow(
                                () -> new GoLiteRuntimeError("Campo " + fi.name() + " no existe en struct " + type,
                                        line, column));
                String prevExpected = this.expectedType;
                this.expectedType = fDecl.type();
                try {
                    structVal.fields().put(fi.name(), Visit(fi.value()));
                } finally {
                    this.expectedType = prevExpected;
                }
            } else {
                if (i >= structDecl.fields.size()) {
                    throw new GoLiteRuntimeError("Demasiados valores para inicializar struct " + type, line, column);
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
                throw new GoLiteRuntimeError("Variable no declarada: " + vr.getName(), vr.getLine(), vr.getColumn());
            }
        } else if (target instanceof FieldAccessNode fa) {
            ValueWrapper structVal = Visit(fa.getTarget());
            if (structVal instanceof StructValue sv) {
                if (!sv.fields().containsKey(fa.getField())) {
                    throw new GoLiteRuntimeError("Campo " + fa.getField() + " no existe en struct " + sv.getTypeName(),
                            fa.getLine(), fa.getColumn());
                }
                sv.fields().put(fa.getField(), value);
            } else {
                throw new GoLiteRuntimeError(
                        "El objetivo del acceso a campo no es un struct: " + structVal.getTypeName(), fa.getLine(),
                        fa.getColumn());
            }
        } else if (target instanceof SliceAccessNode sa) {
            ValueWrapper sliceVal = Visit(sa.getTarget());
            if (sliceVal instanceof SliceValue sv) {
                ValueWrapper idxVal = Visit(sa.getIndex());
                if (!(idxVal instanceof IntValue iv)) {
                    throw new GoLiteRuntimeError("Índice de slice debe ser entero", sa.getLine(), sa.getColumn());
                }
                int idx = iv.value();
                if (idx < 0 || idx >= sv.getElements().size()) {
                    throw new GoLiteRuntimeError(
                            "Índice fuera de rango: " + idx + " (tamaño " + sv.getElements().size() + ")", sa.getLine(),
                            sa.getColumn());
                }
                sv.getElements().set(idx, value);
            } else {
                throw new GoLiteRuntimeError("El objetivo de la indexación no es un slice: " + sliceVal.getTypeName(),
                        sa.getLine(), sa.getColumn());
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
        String oldScope = currentScope;
        currentScope = func.name;
        for (int i = 0; i < func.params.size(); i++) {
            Param param = func.params.get(i);
            ValueWrapper arg = args.get(i);
            funcEnv.define(param.name(), arg);
            addSymbol(param.name(), "Variable", param.type(), currentScope, func.line, func.column);
        }
        Environment oldEnv = currentEnvironment;
        currentEnvironment = funcEnv;
        try {
            Visit(func.body);
        } catch (ReturnException e) {
            return e.value;
        } finally {
            currentEnvironment = oldEnv;
            currentScope = oldScope;
        }
        return defaultVoid;
    }

    private ValueWrapper executeMethod(MethodDeclNode.Context method, ValueWrapper receiverVal,
            List<ValueWrapper> args) {
        Environment methodEnv = new Environment(globalEnvironment);
        String oldScope = currentScope;
        currentScope = method.name;
        methodEnv.define(method.receiver.name(), receiverVal);
        addSymbol(method.receiver.name(), "Variable", method.receiver.type(), currentScope, method.line, method.column);
        for (int i = 0; i < method.params.size(); i++) {
            Param param = method.params.get(i);
            ValueWrapper arg = args.get(i);
            methodEnv.define(param.name(), arg);
            addSymbol(param.name(), "Variable", param.type(), currentScope, method.line, method.column);
        }
        Environment oldEnv = currentEnvironment;
        currentEnvironment = methodEnv;
        try {
            Visit(method.body);
        } catch (ReturnException e) {
            return e.value;
        } finally {
            currentEnvironment = oldEnv;
            currentScope = oldScope;
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
        String processed = processEscapes(ctx.value);
        return new StringValue(processed, ctx.line, ctx.column);
    }

    private String processEscapes(String s) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            if (s.charAt(i) == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                switch (next) {
                    case 't':
                        sb.append('\t');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case '"':
                        sb.append('"');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    default:
                        sb.append('\\');
                        sb.append(next);
                        break;
                }
                i += 2;
            } else {
                sb.append(s.charAt(i));
                i++;
            }
        }
        return sb.toString();
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
            default -> throw new GoLiteRuntimeError("Operacion invalida: -" + operand.getTypeName(), operand.line(),
                    operand.column());
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
        throw new RuntimeException(
                "Operacion invalida: " + left.getTypeName() + " " + op + " " + right.getTypeName());
    }

    private boolean isEqual(ValueWrapper left, ValueWrapper right) {
        if (left instanceof VoidValue && right instanceof VoidValue) {
            return true;
        }
        if (left instanceof VoidValue || right instanceof VoidValue) {
            return false;
        }
        if (left instanceof IntValue l && right instanceof IntValue r)
            return l.value() == r.value();
        if (left instanceof DecimalValue l && right instanceof DecimalValue r)
            return l.value() == r.value();
        if (left instanceof BoolValue l && right instanceof BoolValue r)
            return l.value() == r.value();
        if (left instanceof StringValue l && right instanceof StringValue r)
            return l.value().equals(r.value());
        if (left instanceof RuneValue l && right instanceof RuneValue r)
            return l.value() == r.value();
        if (left instanceof StructValue l && right instanceof StructValue r) {
            return l == r;
        }
        throw new RuntimeException("Operacion invalida: " + left.getTypeName() + " == " + right.getTypeName());
    }

    @Override
    public ValueWrapper visit(Equ.Context ctx) {
        ValueWrapper left = Visit(ctx.left);
        ValueWrapper right = Visit(ctx.right);

        // Manejar nil (VoidValue)
        if (left instanceof VoidValue && right instanceof VoidValue) {
            return new BoolValue(true, left.line(), left.column());
        }
        if (left instanceof VoidValue || right instanceof VoidValue) {
            return new BoolValue(false, left.line(), left.column());
        }

        return switch (left) {
            // int == int
            case IntValue l when right instanceof IntValue r ->
                new BoolValue(l.value() == r.value(), l.line(), l.column());
            // int == float64 ← este es el que te faltaba
            case IntValue l when right instanceof DecimalValue r ->
                new BoolValue(l.value() == r.value(), l.line(), l.column());
            // float64 == int ← y este
            case DecimalValue l when right instanceof IntValue r ->
                new BoolValue(l.value() == r.value(), l.line(), l.column());
            // float64 == float64
            case DecimalValue l when right instanceof DecimalValue r ->
                new BoolValue(l.value() == r.value(), l.line(), l.column());
            // bool == bool
            case BoolValue l when right instanceof BoolValue r ->
                new BoolValue(l.value() == r.value(), l.line(), l.column());
            // string == string
            case StringValue l when right instanceof StringValue r ->
                new BoolValue(l.value().equals(r.value()), l.line(), l.column());
            // rune == rune
            case RuneValue l when right instanceof RuneValue r ->
                new BoolValue(l.value() == r.value(), l.line(), l.column());
            default ->
                throw new RuntimeException("Operacion invalida: "
                        + left.getTypeName() + " == " + right.getTypeName());
        };
    }

    @Override
    public ValueWrapper visit(Nequ.Context ctx) {
        ValueWrapper left = Visit(ctx.left);
        ValueWrapper right = Visit(ctx.right);

        // Manejar nil (VoidValue)
        if (left instanceof VoidValue && right instanceof VoidValue) {
            return new BoolValue(false, left.line(), left.column()); // nil != nil -> false
        }
        if (left instanceof VoidValue || right instanceof VoidValue) {
            return new BoolValue(true, left.line(), left.column()); // nil != valor -> true
        }
        return switch (left) {
            case IntValue l when right instanceof IntValue r ->
                new BoolValue(l.value() != r.value(), l.line(), l.column());
            // int != float64
            case IntValue l when right instanceof DecimalValue r ->
                new BoolValue(l.value() != r.value(), l.line(), l.column());
            // float64 != int
            case DecimalValue l when right instanceof IntValue r ->
                new BoolValue(l.value() != r.value(), l.line(), l.column());
            case DecimalValue l when right instanceof DecimalValue r ->
                new BoolValue(l.value() != r.value(), l.line(), l.column());
            case BoolValue l when right instanceof BoolValue r ->
                new BoolValue(l.value() != r.value(), l.line(), l.column());
            case StringValue l when right instanceof StringValue r ->
                new BoolValue(!l.value().equals(r.value()), l.line(), l.column());
            case RuneValue l when right instanceof RuneValue r ->
                new BoolValue(l.value() != r.value(), l.line(), l.column());
            default ->
                throw new RuntimeException("Operacion invalida: "
                        + left.getTypeName() + " != " + right.getTypeName());
        };
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
        throw new GoLiteRuntimeError("Operacion ! requiere booleano", val.line(), val.column());
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
            throw new GoLiteRuntimeError("Variable no definida: " + ctx.name, ctx.line, ctx.column);
        return val;
    }

    @Override
    public ValueWrapper visit(Imprimir.Context ctx) {
        ValueWrapper value = Visit(ctx.expression);
        output += value.toString() + "\n";
        return defaultVoid;
    }

    @Override
    public ValueWrapper visit(FieldInit.Context ctx) {
        return Visit(ctx.value);
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
        addSymbol(ctx.name, "Variable", ctx.type, currentScope, ctx.line, ctx.column);
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
        addSymbol(ctx.name, "Variable", val.getTypeName(), currentScope, ctx.line, ctx.column);
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
            default -> throw new GoLiteRuntimeError(
                    "Operacion += invalida: " + current.getTypeName() + " += " + increment.getTypeName(),
                    current.line(), current.column());
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
            default -> throw new GoLiteRuntimeError(
                    "Operacion -= invalida: " + current.getTypeName() + " -= " + decrement.getTypeName(),
                    current.line(), current.column());
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

    @Override
    public ValueWrapper visit(ForRangeNode.Context ctx) {
        ValueWrapper collVal = Visit(ctx.collection);
        String elemType;
        int size = 0;
        List<ValueWrapper> elements = null;
        String strVal = null;

        if (collVal instanceof SliceValue sv) {
            elemType = sv.getElementType();
            elements = sv.getElements();
            size = elements.size();
        } else if (collVal instanceof StringValue sv) {
            elemType = "rune";
            strVal = sv.value();
            size = strVal.length();
        } else {
            throw new GoLiteRuntimeError("Se requiere un slice o string para usar range", ctx.line, ctx.column);
        }

        Environment oldEnv = currentEnvironment;
        currentEnvironment = new Environment(oldEnv);
        try {
            if (ctx.isShortDecl) {
                if (ctx.indexId != null && !ctx.indexId.equals("_")) {
                    currentEnvironment.define(ctx.indexId, new IntValue(0, ctx.line, ctx.column));
                    addSymbol(ctx.indexId, "Variable", "int", currentScope, ctx.line, ctx.column);
                }
                if (ctx.valueId != null && !ctx.valueId.equals("_")) {
                    ValueWrapper defVal = createDefaultValue(elemType, ctx.line, ctx.column);
                    currentEnvironment.define(ctx.valueId, defVal);
                    addSymbol(ctx.valueId, "Variable", elemType, currentScope, ctx.line, ctx.column);
                }
            }

            for (int i = 0; i < size; i++) {
                ValueWrapper idxVal = new IntValue(i, ctx.line, ctx.column);
                ValueWrapper valVal;
                if (elements != null) {
                    valVal = elements.get(i);
                } else {
                    valVal = new RuneValue(strVal.charAt(i), ctx.line, ctx.column);
                }

                if (ctx.indexId != null && !ctx.indexId.equals("_")) {
                    if (ctx.isShortDecl) {
                        currentEnvironment.define(ctx.indexId, idxVal);
                    } else {
                        if (!currentEnvironment.assign(ctx.indexId, idxVal)) {
                            throw new GoLiteRuntimeError("Variable no definida: " + ctx.indexId, ctx.line, ctx.column);
                        }
                    }
                }
                if (ctx.valueId != null && !ctx.valueId.equals("_")) {
                    if (ctx.isShortDecl) {
                        currentEnvironment.define(ctx.valueId, valVal);
                    } else {
                        if (!currentEnvironment.assign(ctx.valueId, valVal)) {
                            throw new GoLiteRuntimeError("Variable no definida: " + ctx.valueId, ctx.line, ctx.column);
                        }
                    }
                }

                try {
                    Visit(ctx.body);
                } catch (BreakException e) {
                    break;
                } catch (ContinueException e) {
                    // continuar
                }
            }
        } finally {
            currentEnvironment = oldEnv;
        }
        return defaultVoid;
    }

    // Bucles

    @Override
    public ValueWrapper visit(ForNode.Context ctx) {
        Environment oldEnv = currentEnvironment;
        currentEnvironment = new Environment(oldEnv);
        loopDepth++;
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
            loopDepth--;
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
        if (loopDepth == 0) {
            ErrorCollector.addError("semántico", "break fuera de bucle", ctx.line, ctx.column);
            return defaultVoid;
        }
        throw new BreakException();
    }

    @Override
    public ValueWrapper visit(ContinueNode.Context ctx) {
        if (loopDepth == 0) {
            ErrorCollector.addError("semántico", "continue fuera de bucle", ctx.line, ctx.column);
            return defaultVoid;
        }
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
            if (i > 0)
                sb.append(" ");
            ValueWrapper val = Visit(ctx.args.get(i));
            if (val instanceof VoidValue) {
                sb.append("nil");
            } else {
                sb.append(val.toString());
            }
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
                throw new GoLiteRuntimeError("strconv.Atoi: no se pudo convertir '" + s.value() + "' a entero",
                        s.line(), s.column());
            }
        }
        throw new GoLiteRuntimeError("strconv.Atoi espera un string", arg.line(), arg.column());
    }

    @Override
    public ValueWrapper visit(StrconvParseFloat.Context ctx) {
        ValueWrapper arg = Visit(ctx.argument);
        if (arg instanceof StringValue s) {
            try {
                double num = Double.parseDouble(s.value());
                return new DecimalValue(num, s.line(), s.column());
            } catch (NumberFormatException e) {
                throw new GoLiteRuntimeError("strconv.ParseFloat: no se pudo convertir '" + s.value() + "' a float64",
                        s.line(), s.column());
            }
        }
        throw new GoLiteRuntimeError("strconv.ParseFloat espera un string", arg.line(), arg.column());
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
        addSymbol(ctx.name, ctx.returnType == null ? "Procedimiento" : "Función",
                ctx.returnType == null ? "void" : ctx.returnType, "Global", ctx.line, ctx.column);
        return defaultVoid;
    }

    @Override
    public ValueWrapper visit(MethodDeclNode.Context ctx) {
        methodsMap.put(ctx.receiver.type() + "#" + ctx.name, ctx);
        addSymbol(ctx.name, ctx.returnType == null ? "Procedimiento" : "Función",
                ctx.returnType == null ? "void" : ctx.returnType, "Global", ctx.line, ctx.column);
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
        addSymbol(ctx.name, "Struct", ctx.name, "Global", ctx.line, ctx.column);
        return defaultVoid;
    }

    @Override
    public ValueWrapper visit(FuncCallNode.Context ctx) {
        FuncDeclNode.Context func = funcsMap.get(ctx.name);
        if (func == null) {
            throw new GoLiteRuntimeError("Función no definida: " + ctx.name, ctx.line, ctx.column);
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
            throw new GoLiteRuntimeError("Método " + ctx.name + " no definido para tipo " + recType, ctx.line,
                    ctx.column);
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
                throw new GoLiteRuntimeError("Campo " + ctx.field + " no existe en struct " + sv.getTypeName(),
                        ctx.line, ctx.column);
            }
            return sv.fields().get(ctx.field);
        }
        throw new GoLiteRuntimeError("Acceso a campo inválido en tipo " + targetVal.getTypeName(), ctx.line,
                ctx.column);
    }

    @Override
    public ValueWrapper visit(SliceAccessNode.Context ctx) {
        ValueWrapper targetVal = Visit(ctx.target);
        if (targetVal instanceof SliceValue sv) {
            ValueWrapper idxVal = Visit(ctx.index);
            if (!(idxVal instanceof IntValue iv)) {
                throw new GoLiteRuntimeError("Índice de slice debe ser entero", ctx.line, ctx.column);
            }
            int idx = iv.value();
            if (idx < 0 || idx >= sv.getElements().size()) {
                throw new GoLiteRuntimeError(
                        "Índice fuera de rango: " + idx + " (tamaño " + sv.getElements().size() + ")", ctx.line,
                        ctx.column);
            }
            return sv.getElements().get(idx);
        }
        throw new GoLiteRuntimeError("Indexación inválida en tipo " + targetVal.getTypeName(), ctx.line, ctx.column);
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
        throw new GoLiteRuntimeError("append requiere un slice", ctx.line, ctx.column);
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
        throw new GoLiteRuntimeError("slices.Index requiere un slice", ctx.line, ctx.column);
    }

    @Override
    public ValueWrapper visit(StringsJoinNode.Context ctx) {
        ValueWrapper sliceVal = Visit(ctx.slice);
        if (sliceVal instanceof SliceValue sv) {
            if (!sv.getElementType().equals("string")) {
                throw new GoLiteRuntimeError("strings.Join requiere un slice de strings", ctx.line, ctx.column);
            }
            ValueWrapper sepVal = Visit(ctx.sep);
            if (!(sepVal instanceof StringValue sev)) {
                throw new GoLiteRuntimeError("strings.Join requiere un separador de tipo string", ctx.line, ctx.column);
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
        throw new GoLiteRuntimeError("strings.Join requiere un slice", ctx.line, ctx.column);
    }

    @Override
    public ValueWrapper visit(LenNode.Context ctx) {
        ValueWrapper val = Visit(ctx.expression);
        if (val instanceof SliceValue sv) {
            return new IntValue(sv.getElements().size(), ctx.line, ctx.column);
        } else if (val instanceof StringValue sv) {
            return new IntValue(sv.value().length(), ctx.line, ctx.column);
        }
        throw new GoLiteRuntimeError("len requiere un slice o string", ctx.line, ctx.column);
    }
}
