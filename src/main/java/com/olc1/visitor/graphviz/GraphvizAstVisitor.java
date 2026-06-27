package com.olc1.visitor.graphviz;

import com.olc1.ast.ASTNode;
import com.olc1.ast.exp.*;
import com.olc1.ast.stm.*;
import com.olc1.visitor.Visitor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Genera un árbol AST en formato Graphviz (DOT) y lo exporta a SVG.
 * Implementa el patrón Visitor recorriendo todos los nodos del AST.
 */
public class GraphvizAstVisitor implements Visitor<String> {

    private final StringBuilder dot = new StringBuilder();
    private final AtomicInteger counter = new AtomicInteger(0);

    public GraphvizAstVisitor() {
        dot.append("digraph AST {\n");
        dot.append("    node [shape=box, style=filled, fillcolor=lightyellow, fontname=\"Arial\"];\n");
        dot.append("    edge [arrowhead=vee];\n");
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private String nextId() {
        return "n" + counter.incrementAndGet();
    }

    private String createNode(String label) {
        String id = nextId();
        String escaped = label.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
        dot.append("    ").append(id).append(" [label=\"").append(escaped).append("\"];\n");
        return id;
    }

    private void addEdge(String parentId, String childId) {
        if (childId != null) {
            dot.append("    ").append(parentId).append(" -> ").append(childId).append(";\n");
        }
    }

    private void addEdges(String parentId, List<? extends ASTNode> children) {
        if (children == null)
            return;
        for (ASTNode child : children) {
            if (child != null) {
                String childId = child.accept(this);
                addEdge(parentId, childId);
            }
        }
    }

    // ==================== MÉTODOS DE GENERACIÓN ====================

    /**
     * Genera el SVG usando la librería graphviz-java (requiere dependencia).
     * Si no la tienes, usa generateSVGWithDot.
     */
    public void generateSVG(String outputPath) throws IOException {
        dot.append("}\n");
        // Usar guru.nidi.graphviz
        guru.nidi.graphviz.engine.Graphviz.fromString(dot.toString())
                .render(guru.nidi.graphviz.engine.Format.SVG)
                .toFile(new java.io.File(outputPath));
    }

    /**
     * Alternativa: usa el comando "dot" del sistema (Graphviz instalado).
     * Escribe el archivo .dot y luego ejecuta dot -Tsvg.
     */
    public void generateSVGWithDot(String dotPath, String outputPath) throws IOException, InterruptedException {
        Files.write(Paths.get(dotPath), dot.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        ProcessBuilder pb = new ProcessBuilder("dot", "-Tsvg", dotPath, "-o", outputPath);
        Process p = pb.start();
        int exitCode = p.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Error al ejecutar dot, código: " + exitCode);
        }
    }

    // ==================== VISITAS: NODOS EXPRESIÓN ====================

    @Override
    public String visit(Add.Context ctx) {
        String id = createNode("+");
        String leftId = ctx.left.accept(this);
        String rightId = ctx.right.accept(this);
        addEdge(id, leftId);
        addEdge(id, rightId);
        return id;
    }

    @Override
    public String visit(Sub.Context ctx) {
        String id = createNode("-");
        String leftId = ctx.left.accept(this);
        String rightId = ctx.right.accept(this);
        addEdge(id, leftId);
        addEdge(id, rightId);
        return id;
    }

    @Override
    public String visit(Mul.Context ctx) {
        String id = createNode("*");
        String leftId = ctx.left.accept(this);
        String rightId = ctx.right.accept(this);
        addEdge(id, leftId);
        addEdge(id, rightId);
        return id;
    }

    @Override
    public String visit(Div.Context ctx) {
        String id = createNode("/");
        String leftId = ctx.left.accept(this);
        String rightId = ctx.right.accept(this);
        addEdge(id, leftId);
        addEdge(id, rightId);
        return id;
    }

    @Override
    public String visit(Mod.Context ctx) {
        String id = createNode("%");
        String leftId = ctx.left.accept(this);
        String rightId = ctx.right.accept(this);
        addEdge(id, leftId);
        addEdge(id, rightId);
        return id;
    }

    @Override
    public String visit(Negate.Context ctx) {
        String id = createNode("- (unario)");
        String childId = ctx.expression.accept(this);
        addEdge(id, childId);
        return id;
    }

    @Override
    public String visit(And.Context ctx) {
        String id = createNode("&&");
        String leftId = ctx.left.accept(this);
        String rightId = ctx.right.accept(this);
        addEdge(id, leftId);
        addEdge(id, rightId);
        return id;
    }

    @Override
    public String visit(Or.Context ctx) {
        String id = createNode("||");
        String leftId = ctx.left.accept(this);
        String rightId = ctx.right.accept(this);
        addEdge(id, leftId);
        addEdge(id, rightId);
        return id;
    }

    @Override
    public String visit(Not.Context ctx) {
        String id = createNode("!");
        String childId = ctx.expression.accept(this);
        addEdge(id, childId);
        return id;
    }

    @Override
    public String visit(Equ.Context ctx) {
        String id = createNode("==");
        String leftId = ctx.left.accept(this);
        String rightId = ctx.right.accept(this);
        addEdge(id, leftId);
        addEdge(id, rightId);
        return id;
    }

    @Override
    public String visit(Nequ.Context ctx) {
        String id = createNode("!=");
        String leftId = ctx.left.accept(this);
        String rightId = ctx.right.accept(this);
        addEdge(id, leftId);
        addEdge(id, rightId);
        return id;
    }

    @Override
    public String visit(Greater.Context ctx) {
        String id = createNode(">");
        String leftId = ctx.left.accept(this);
        String rightId = ctx.right.accept(this);
        addEdge(id, leftId);
        addEdge(id, rightId);
        return id;
    }

    @Override
    public String visit(GreaterEqu.Context ctx) {
        String id = createNode(">=");
        String leftId = ctx.left.accept(this);
        String rightId = ctx.right.accept(this);
        addEdge(id, leftId);
        addEdge(id, rightId);
        return id;
    }

    @Override
    public String visit(Lesser.Context ctx) {
        String id = createNode("<");
        String leftId = ctx.left.accept(this);
        String rightId = ctx.right.accept(this);
        addEdge(id, leftId);
        addEdge(id, rightId);
        return id;
    }

    @Override
    public String visit(LesserEqu.Context ctx) {
        String id = createNode("<=");
        String leftId = ctx.left.accept(this);
        String rightId = ctx.right.accept(this);
        addEdge(id, leftId);
        addEdge(id, rightId);
        return id;
    }

    @Override
    public String visit(Integers.Context ctx) {
        return createNode("int: " + ctx.value);
    }

    @Override
    public String visit(Decimal.Context ctx) {
        return createNode("float: " + ctx.value);
    }

    @Override
    public String visit(StringLiteral.Context ctx) {
        return createNode("string: \"" + ctx.value + "\"");
    }

    @Override
    public String visit(BoolLiteral.Context ctx) {
        return createNode("bool: " + ctx.value);
    }

    @Override
    public String visit(RuneLiteral.Context ctx) {
        // Mostrar el carácter escapado si es necesario
        String display = (ctx.value == '\n') ? "\\n"
                : (ctx.value == '\t') ? "\\t"
                        : (ctx.value == '\r') ? "\\r"
                                : (ctx.value == '\\') ? "\\\\"
                                        : (ctx.value == '\'') ? "\\'" : String.valueOf(ctx.value);
        return createNode("rune: '" + display + "'");
    }

    @Override
    public String visit(NilLiteral.Context ctx) {
        return createNode("nil");
    }

    @Override
    public String visit(VarRef.Context ctx) {
        return createNode("var: " + ctx.name);
    }

    @Override
    public String visit(Paren.Context ctx) {
        // Los paréntesis no agregan estructura, solo pasamos al hijo
        return ctx.expression.accept(this);
    }

    @Override
    public String visit(FmtPrintln.Context ctx) {
        String id = createNode("fmt.Println");
        addEdges(id, ctx.args);
        return id;
    }

    @Override
    public String visit(StrconvAtoi.Context ctx) {
        String id = createNode("strconv.Atoi");
        String argId = ctx.argument.accept(this);
        addEdge(id, argId);
        return id;
    }

    @Override
    public String visit(StrconvParseFloat.Context ctx) {
        String id = createNode("strconv.ParseFloat");
        String argId = ctx.argument.accept(this);
        addEdge(id, argId);
        return id;
    }

    @Override
    public String visit(ReflectTypeOf.Context ctx) {
        String id = createNode("reflect.TypeOf");
        String argId = ctx.argument.accept(this);
        addEdge(id, argId);
        return id;
    }

    @Override
    public String visit(AppendNode.Context ctx) {
        String id = createNode("append");
        String sliceId = ctx.slice.accept(this);
        addEdge(id, sliceId);
        String valId = ctx.value.accept(this);
        addEdge(id, valId);
        return id;
    }

    @Override
    public String visit(LenNode.Context ctx) {
        String id = createNode("len");
        String argId = ctx.expression.accept(this);
        addEdge(id, argId);
        return id;
    }

    @Override
    public String visit(SlicesIndexNode.Context ctx) {
        String id = createNode("slices.Index");
        String sliceId = ctx.slice.accept(this);
        String valId = ctx.value.accept(this);
        addEdge(id, sliceId);
        addEdge(id, valId);
        return id;
    }

    @Override
    public String visit(StringsJoinNode.Context ctx) {
        String id = createNode("strings.Join");
        String sliceId = ctx.slice.accept(this);
        String sepId = ctx.sep.accept(this);
        addEdge(id, sliceId);
        addEdge(id, sepId);
        return id;
    }

    @Override
    public String visit(SliceAccessNode.Context ctx) {
        String id = createNode("slice access");
        String targetId = ctx.target.accept(this);
        String indexId = ctx.index.accept(this);
        addEdge(id, targetId);
        addEdge(id, indexId);
        return id;
    }

    @Override
    public String visit(SliceLiteralNode.Context ctx) {
        String id = createNode("slice literal [" + ctx.type + "]");
        addEdges(id, ctx.elements);
        return id;
    }

    @Override
    public String visit(StructLiteralNode.Context ctx) {
        String id = createNode("struct literal " + ctx.type);
        addEdges(id, ctx.elements);
        return id;
    }

    @Override
    public String visit(FieldAccessNode.Context ctx) {
        String id = createNode("field access ." + ctx.field);
        String targetId = ctx.target.accept(this);
        addEdge(id, targetId);
        return id;
    }

    @Override
    public String visit(FieldInit.Context ctx) {
        // FieldInit es un record que implementa ASTNode
        // Su accept retorna null, así que lo manejamos aquí
        String id = createNode("field: " + ctx.name);
        String valId = ctx.value.accept(this);
        addEdge(id, valId);
        return id;
    }

    @Override
    public String visit(MethodCallNode.Context ctx) {
        String id = createNode("method call: " + ctx.name);
        String recId = ctx.receiver.accept(this);
        addEdge(id, recId);
        addEdges(id, ctx.args);
        return id;
    }

    @Override
    public String visit(FuncCallNode.Context ctx) {
        String id = createNode("call: " + ctx.name);
        addEdges(id, ctx.args);
        return id;
    }

    @Override
    public String visit(BraceLiteralNode.Context ctx) {
        // Se usa para llaves en inicializaciones de structs/slices
        String id = createNode("braced list");
        addEdges(id, ctx.elements);
        return id;
    }

    // ==================== VISITAS: NODOS INSTRUCCIÓN ====================

    @Override
    public String visit(VarDecl.Context ctx) {
        String typeLabel = ctx.type != null ? ctx.type : "inferido";
        String id = createNode("var " + ctx.name + " : " + typeLabel);
        if (ctx.value != null) {
            String valId = ctx.value.accept(this);
            addEdge(id, valId);
        }
        return id;
    }

    @Override
    public String visit(ShortDecl.Context ctx) {
        String id = createNode(":= " + ctx.name);
        String valId = ctx.value.accept(this);
        addEdge(id, valId);
        return id;
    }

    @Override
    public String visit(Assign.Context ctx) {
        String id = createNode("=");
        String targetId = ctx.target.accept(this);
        String valId = ctx.value.accept(this);
        addEdge(id, targetId);
        addEdge(id, valId);
        return id;
    }

    @Override
    public String visit(PlusAssign.Context ctx) {
        String id = createNode("+=");
        String targetId = ctx.target.accept(this);
        String valId = ctx.value.accept(this);
        addEdge(id, targetId);
        addEdge(id, valId);
        return id;
    }

    @Override
    public String visit(MinusAssign.Context ctx) {
        String id = createNode("-=");
        String targetId = ctx.target.accept(this);
        String valId = ctx.value.accept(this);
        addEdge(id, targetId);
        addEdge(id, valId);
        return id;
    }

    @Override
    public String visit(IfNode.Context ctx) {
        String id = createNode("if");
        String condId = ctx.condition.accept(this);
        addEdge(id, condId);

        if (ctx.body != null) {
            String bodyId = ctx.body.accept(this);
            addEdge(id, bodyId);
        }

        if (ctx.elsePart != null) {
            String elseId = ctx.elsePart.accept(this);
            addEdge(id, elseId);
        }
        return id;
    }

    @Override
    public String visit(ElseIfPart.Context ctx) {
        String id = createNode("else if");
        String condId = ctx.condition.accept(this);
        addEdge(id, condId);
        if (ctx.body != null) {
            String bodyId = ctx.body.accept(this);
            addEdge(id, bodyId);
        }
        // elsePart se conecta al if principal, no aquí
        return id;
    }

    @Override
    public String visit(ElsePart.Context ctx) {
        String id = createNode("else");
        if (ctx.body != null) {
            String bodyId = ctx.body.accept(this);
            addEdge(id, bodyId);
        }
        return id;
    }

    @Override
    public String visit(ForNode.Context ctx) {
        String id = createNode("for");
        if (ctx.init != null) {
            String initId = ctx.init.accept(this);
            addEdge(id, initId);
        }
        if (ctx.condition != null) {
            String condId = ctx.condition.accept(this);
            addEdge(id, condId);
        }
        if (ctx.increment != null) {
            String incId = ctx.increment.accept(this);
            addEdge(id, incId);
        }
        if (ctx.body != null) {
            String bodyId = ctx.body.accept(this);
            addEdge(id, bodyId);
        }
        return id;
    }

    @Override
    public String visit(ForWhileNode.Context ctx) {
        String id = createNode("for while");
        String condId = ctx.condition.accept(this);
        addEdge(id, condId);
        if (ctx.body != null) {
            String bodyId = ctx.body.accept(this);
            addEdge(id, bodyId);
        }
        return id;
    }

    @Override
    public String visit(ForRangeNode.Context ctx) {
        String label = "for range";
        if (ctx.indexId != null && ctx.valueId != null) {
            label += " (" + ctx.indexId + ", " + ctx.valueId + ")";
        } else if (ctx.indexId != null) {
            label += " (" + ctx.indexId + ")";
        }
        String id = createNode(label);
        String collId = ctx.collection.accept(this);
        addEdge(id, collId);
        if (ctx.body != null) {
            String bodyId = ctx.body.accept(this);
            addEdge(id, bodyId);
        }
        return id;
    }

    @Override
    public String visit(BreakNode.Context ctx) {
        return createNode("break");
    }

    @Override
    public String visit(ContinueNode.Context ctx) {
        return createNode("continue");
    }

    @Override
    public String visit(ReturnNode.Context ctx) {
        String id = createNode("return");
        if (ctx.value != null) {
            String valId = ctx.value.accept(this);
            addEdge(id, valId);
        }
        return id;
    }

    @Override
    public String visit(SwitchNode.Context ctx) {
        String id = createNode("switch");
        String exprId = ctx.expression.accept(this);
        addEdge(id, exprId);
        addEdges(id, ctx.cases); // casos son CaseNode
        return id;
    }

    @Override
    public String visit(CaseNode.Context ctx) {
        String label = (ctx.expression == null) ? "default" : "case";
        String id = createNode(label);
        if (ctx.expression != null) {
            String exprId = ctx.expression.accept(this);
            addEdge(id, exprId);
        }
        if (ctx.body != null) {
            String bodyId = ctx.body.accept(this);
            addEdge(id, bodyId);
        }
        return id;
    }

    @Override
    public String visit(ExprStatment.Context ctx) {
        // Envoltorio de expresión: simplemente pasamos al hijo
        return ctx.expression.accept(this);
    }

    @Override
    public String visit(Statments.Context ctx) {
        String id = createNode("bloque");
        addEdges(id, ctx.statements);
        return id;
    }

    @Override
    public String visit(FuncDeclNode.Context ctx) {
        String label = "func " + ctx.name;
        if (ctx.returnType != null && !ctx.returnType.isEmpty()) {
            label += " : " + ctx.returnType;
        }
        String id = createNode(label);

        // Parámetros: Param no es ASTNode, los creamos manualmente
        if (ctx.params != null && !ctx.params.isEmpty()) {
            String paramsId = createNode("parámetros");
            addEdge(id, paramsId);
            for (Param p : ctx.params) {
                String pId = createNode(p.name() + " : " + p.type());
                addEdge(paramsId, pId);
            }
        }

        if (ctx.body != null) {
            String bodyId = ctx.body.accept(this);
            addEdge(id, bodyId);
        }
        return id;
    }

    @Override
    public String visit(MethodDeclNode.Context ctx) {
        String label = "method " + ctx.name + " (receiver: " + ctx.receiver.type() + " " + ctx.receiver.name() + ")";
        if (ctx.returnType != null && !ctx.returnType.isEmpty()) {
            label += " : " + ctx.returnType;
        }
        String id = createNode(label);

        // Parámetros (excluyendo receiver)
        if (ctx.params != null && !ctx.params.isEmpty()) {
            String paramsId = createNode("parámetros");
            addEdge(id, paramsId);
            for (Param p : ctx.params) {
                String pId = createNode(p.name() + " : " + p.type());
                addEdge(paramsId, pId);
            }
        }

        if (ctx.body != null) {
            String bodyId = ctx.body.accept(this);
            addEdge(id, bodyId);
        }
        return id;
    }

    // Param no implementa ASTNode, pero lo manejamos en FuncDecl y MethodDecl

    @Override
    public String visit(StructDeclNode.Context ctx) {
        String id = createNode("struct " + ctx.name);
        // FieldDecl no es ASTNode, los creamos manualmente
        if (ctx.fields != null) {
            for (FieldDecl f : ctx.fields) {
                String fId = createNode(f.name() + " : " + f.type());
                addEdge(id, fId);
            }
        }
        return id;
    }

    // FieldDecl no implementa ASTNode, manejado en StructDeclNode

    @Override
    public String visit(Imprimir.Context ctx) {
        // Similar a fmt.Println pero con nombre "imprimir"
        String id = createNode("imprimir");
        String exprId = ctx.expression.accept(this);
        addEdge(id, exprId);
        return id;
    }
}