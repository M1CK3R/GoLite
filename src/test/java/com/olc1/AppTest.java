package com.olc1;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;
import java.io.StringReader;
import java.io.BufferedReader;
import com.olc1.reports.GoLiteError;
import com.olc1.reports.SymbolEntry;
import com.olc1.ast.ASTNode;
import com.olc1.visitor.interpreter.InterpreterVisitor;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue()
    {
        assertTrue( true );
    }

    @Test
    public void testLexicalAndSyntacticErrorsAreCaptured() {
        // Input with a lexical error (invalid symbol '#') and syntactic error
        String input = "var i int = #;\n";
        Lexer lexer = new Lexer(new BufferedReader(new StringReader(input)));
        parser p = new parser(lexer);
        
        try {
            p.parse();
        } catch (Exception e) {
            // Expected parsing exception due to syntax error
        }

        // Verify lexical errors
        System.out.println("Lexical errors size: " + lexer.errors.size());
        for (GoLiteError err : lexer.errors) {
            System.out.println("Lexical error: " + err);
        }
        assertFalse("Lexical errors list should not be empty", lexer.errors.isEmpty());
        GoLiteError lexError = lexer.errors.get(0);
        assertTrue("Lexical error description should contain '#'", lexError.toString().contains("#"));

        // Verify syntactic errors
        System.out.println("Syntactic errors size: " + p.errors.size());
        for (GoLiteError err : p.errors) {
            System.out.println("Syntactic error: " + err);
        }
        assertFalse("Syntactic errors list should not be empty", p.errors.isEmpty());
    }

    @Test
    public void testForLoopsAndIncrementDecrement() throws Exception {
        String input = "n := 0\n" +
                       "for n < 5 {\n" +
                       "    fmt.Println(n)\n" +
                       "    n++\n" +
                       "}\n" +
                       "\n" +
                       "for {\n" +
                       "    fmt.Println(n)\n" +
                       "    n--\n" +
                       "    if n < 4 {\n" +
                       "        break\n" +
                       "    }\n" +
                       "}\n";
        Lexer lexer = new Lexer(new BufferedReader(new StringReader(input)));
        parser p = new parser(lexer);
        ASTNode ast = (ASTNode) p.parse().value;

        // Ensure no syntax errors
        assertTrue("Should have no lexer errors", lexer.errors.isEmpty());
        for (GoLiteError err : p.errors) {
            System.out.println("PARSER ERROR in testForLoopsAndIncrementDecrement: " + err.getDescription() + " line " + err.getLine() + " col " + err.getColumn());
        }
        assertTrue("Should have no parser errors", p.errors.isEmpty());

        InterpreterVisitor interpreter = new InterpreterVisitor();
        interpreter.Visit(ast);
        String output = interpreter.output;
        System.out.println("Output of execution:\n" + output);

        String expected = "0\n1\n2\n3\n4\n5\n4\n";
        assertEquals(expected, output.replace("\r\n", "\n"));
    }

    @Test
    public void testSyntaxErrorRecoveryAndContinuation() throws Exception {
        // Here, the second line has a syntax error (incomplete expression after '+')
        String input = "var x int = 5;\n" +
                       "var y int = 10 + ;\n" + // Syntax error here!
                       "fmt.Println(x);\n";
        Lexer lexer = new Lexer(new BufferedReader(new StringReader(input)));
        parser p = new parser(lexer);
        
        ASTNode ast = (ASTNode) p.parse().value;
        
        // We expect exactly 1 syntax error
        assertEquals("Should have registered 1 syntax error", 1, p.errors.size());
        GoLiteError err = p.errors.get(0);
        assertTrue("Error should be syntactic", err.getType().equals("sintáctico"));
        
        // Run interpreter to see if 'x' was declared and printed, skipping the invalid 'y' statement
        InterpreterVisitor interpreter = new InterpreterVisitor();
        interpreter.Visit(ast);
        String output = interpreter.output;
        
        assertEquals("5\n", output.replace("\r\n", "\n"));
    }

    @Test
    public void testSyntaxErrorRecoveryInsideLoop() throws Exception {
        String input = "x := 0;\n" +
                       "for x < 2 {\n" +
                       "    fmt.Println(x);\n" +
                       "    bad_statement + ;\n" + // Syntax error inside loop body
                       "    x++;\n" +
                       "}\n";
        Lexer lexer = new Lexer(new BufferedReader(new StringReader(input)));
        parser p = new parser(lexer);
        
        ASTNode ast = (ASTNode) p.parse().value;
        
        // We expect exactly 1 syntax error
        assertEquals("Should have registered 1 syntax error", 1, p.errors.size());
        
        // Run interpreter to see if the loop executed twice and skipped the bad statement
        InterpreterVisitor interpreter = new InterpreterVisitor();
        interpreter.Visit(ast);
        String output = interpreter.output;
        
        assertEquals("0\n1\n", output.replace("\r\n", "\n"));
    }

    @Test
    public void testSemanticErrorRecoveryAndContinuation() throws Exception {
        com.olc1.reports.ErrorCollector.clear();
        String input = "fmt.Println(5+5);\n" +
                       "x := 7 / 0;\n" + // Semantic error: division by zero
                       "fmt.Println(\"hola\");\n";
        Lexer lexer = new Lexer(new BufferedReader(new StringReader(input)));
        parser p = new parser(lexer);
        
        ASTNode ast = (ASTNode) p.parse().value;
        
        assertTrue("Should have no parser errors", p.errors.isEmpty());
        
        InterpreterVisitor interpreter = new InterpreterVisitor();
        interpreter.Visit(ast);
        String output = interpreter.output;
        
        assertEquals("10\nhola\n", output.replace("\r\n", "\n"));
        assertEquals("Should have registered 1 semantic error", 1, com.olc1.reports.ErrorCollector.getErrors().size());
    }

    @Test
    public void testVariableShadowingInNestedScopes() throws Exception {
        String input = "x := 1;\n" +
                       "{\n" +
                       "    x := 2;\n" +
                       "    {\n" +
                       "        x := 3;\n" +
                       "        {\n" +
                       "            x := 4;\n" +
                       "            fmt.Println(x);\n" +
                       "        }\n" +
                       "        fmt.Println(x);\n" +
                       "    }\n" +
                       "    fmt.Println(x);\n" +
                       "}\n" +
                       "fmt.Println(x);\n";
        Lexer lexer = new Lexer(new BufferedReader(new StringReader(input)));
        parser p = new parser(lexer);
        
        ASTNode ast = (ASTNode) p.parse().value;
        
        assertTrue("Should have no parser errors", p.errors.isEmpty());
        
        InterpreterVisitor interpreter = new InterpreterVisitor();
        interpreter.Visit(ast);
        String output = interpreter.output;
        
        assertEquals("4\n3\n2\n1\n", output.replace("\r\n", "\n"));
    }

    @Test
    public void testRuneAndStringOperations() throws Exception {
        com.olc1.reports.ErrorCollector.clear();
        String input = "ptos1a := 0\n" +
                       "var caracter rune = 'A'\n" +
                       "if caracter == 'A' {\n" +
                       "    ptos1a += 1\n" +
                       "}\n" +
                       "fmt.Println(ptos1a)\n" +
                       "strGo := \"Go\"\n" +
                       "strLite := \"Lite\"\n" +
                       "concat1 := strGo + strLite\n" +
                       "concat2 := \"Lenguaje: \" + strGo + strLite\n" +
                       "fmt.Println(concat1)\n" +
                       "fmt.Println(concat2)\n";
        Lexer lexer = new Lexer(new BufferedReader(new StringReader(input)));
        parser p = new parser(lexer);
        
        ASTNode ast = (ASTNode) p.parse().value;
        
        assertTrue("Should have no parser errors", p.errors.isEmpty());
        
        InterpreterVisitor interpreter = new InterpreterVisitor();
        interpreter.Visit(ast);
        String output = interpreter.output;
        
        assertEquals("1\nGoLite\nLenguaje: GoLite\n", output.replace("\r\n", "\n"));
        assertTrue("Should have no semantic errors", com.olc1.reports.ErrorCollector.getErrors().isEmpty());
    }

    @Test
    public void testSwitchCaseStatements() throws Exception {
        com.olc1.reports.ErrorCollector.clear();
        String input = 
            "numero := 2\n" +
            "switch numero {\n" +
            "    case 1:\n" +
            "        fmt.Println(\"Uno\")\n" +
            "    case 2:\n" +
            "        fmt.Println(\"Dos\")\n" +
            "    case 3:\n" +
            "        fmt.Println(\"Tres\")\n" +
            "    default:\n" +
            "        fmt.Println(\"default\")\n" +
            "}\n" +
            "\n" +
            "switch 5 {\n" +
            "    case 1:\n" +
            "        fmt.Println(\"Uno\")\n" +
            "    default:\n" +
            "        fmt.Println(\"Default para 5\")\n" +
            "}\n" +
            "\n" +
            "switch 10 {\n" +
            "    case 1:\n" +
            "        fmt.Println(\"No se imprime\")\n" +
            "}\n" +
            "\n" +
            "switch 1 {\n" +
            "    case 1:\n" +
            "        fmt.Println(\"Inicio case 1\")\n" +
            "        break\n" +
            "        fmt.Println(\"No se debe imprimir tras break\")\n" +
            "}\n";
            
        Lexer lexer = new Lexer(new BufferedReader(new StringReader(input)));
        parser p = new parser(lexer);
        
        ASTNode ast = (ASTNode) p.parse().value;
        
        assertTrue("Should have no parser errors", p.errors.isEmpty());
        
        InterpreterVisitor interpreter = new InterpreterVisitor();
        interpreter.Visit(ast);
        String output = interpreter.output;
        
        String expected = "Dos\nDefault para 5\nInicio case 1\n";
        assertEquals(expected, output.replace("\r\n", "\n"));
        assertTrue("Should have no semantic errors", com.olc1.reports.ErrorCollector.getErrors().isEmpty());
    }

    @Test
    public void testFunctionsSlicesAndStructsExample() throws Exception {
        com.olc1.reports.ErrorCollector.clear();
        String input =
            "type Producto struct {\n" +
            "    nombre string\n" +
            "    precio float64\n" +
            "}\n" +
            "\n" +
            "func actualizarProducto(p Producto, nuevoPrecio float64) {\n" +
            "    p.precio = nuevoPrecio\n" +
            "}\n" +
            "\n" +
            "func agregarElemento(s []int, elem int) []int {\n" +
            "    return append(s, elem)\n" +
            "}\n" +
            "\n" +
            "func main() {\n" +
            "    prod := Producto{nombre: \"Laptop\", precio: 999.99}\n" +
            "    fmt.Println(prod.nombre, prod.precio)\n" +
            "    actualizarProducto(prod, 899.99)\n" +
            "    fmt.Println(prod.precio)\n" +
            "\n" +
            "    numbers := []int{1, 2, 3}\n" +
            "    fmt.Println(len(numbers))\n" +
            "    numbers2 := agregarElemento(numbers, 4)\n" +
            "    fmt.Println(len(numbers), len(numbers2))\n" +
            "}\n";

        Lexer lexer = new Lexer(new BufferedReader(new StringReader(input)));
        parser p = new parser(lexer);
        ASTNode ast = (ASTNode) p.parse().value;

        assertTrue("Should have no parser errors", p.errors.isEmpty());

        InterpreterVisitor interpreter = new InterpreterVisitor();
        interpreter.Visit(ast);
        String output = interpreter.output;

        String expected = "Laptop 999.99\n899.99\n3\n3 4\n";
        assertEquals(expected, output.replace("\r\n", "\n"));
    }

    @Test
    public void testStructMethodsByReference() throws Exception {
        com.olc1.reports.ErrorCollector.clear();
        String input =
            "type Persona struct {\n" +
            "    nombre string\n" +
            "    edad int\n" +
            "}\n" +
            "\n" +
            "func (p Persona) CumplirAnios() {\n" +
            "    p.edad = p.edad + 1\n" +
            "}\n" +
            "\n" +
            "func main() {\n" +
            "    pers := Persona{nombre: \"Juan\", edad: 25}\n" +
            "    pers.CumplirAnios()\n" +
            "    fmt.Println(pers.nombre, pers.edad)\n" +
            "}\n";

        Lexer lexer = new Lexer(new BufferedReader(new StringReader(input)));
        parser p = new parser(lexer);
        ASTNode ast = (ASTNode) p.parse().value;

        assertTrue("Should have no parser errors", p.errors.isEmpty());

        InterpreterVisitor interpreter = new InterpreterVisitor();
        interpreter.Visit(ast);
        String output = interpreter.output;

        assertEquals("Juan 26\n", output.replace("\r\n", "\n"));
    }

    @Test
    public void testSlicesAndMatrices() throws Exception {
        com.olc1.reports.ErrorCollector.clear();
        String input =
            "func main() {\n" +
            "    var matrix [][]int\n" +
            "    row1 := []int{1, 2}\n" +
            "    row2 := []int{3, 4}\n" +
            "    matrix = [][]int{row1, row2}\n" +
            "    fmt.Println(matrix[0][0], matrix[0][1])\n" +
            "    fmt.Println(matrix[1][0], matrix[1][1])\n" +
            "    matrix[1][1] = 5\n" +
            "    fmt.Println(matrix[1][1])\n" +
            "}\n";

        Lexer lexer = new Lexer(new BufferedReader(new StringReader(input)));
        parser p = new parser(lexer);
        ASTNode ast = (ASTNode) p.parse().value;

        assertTrue("Should have no parser errors", p.errors.isEmpty());

        InterpreterVisitor interpreter = new InterpreterVisitor();
        interpreter.Visit(ast);
        String output = interpreter.output;

        assertEquals("1 2\n3 4\n5\n", output.replace("\r\n", "\n"));
    }

    @Test
    public void testBuiltInFunctions() throws Exception {
        com.olc1.reports.ErrorCollector.clear();
        String input =
            "func main() {\n" +
            "    names := []string{\"Alice\", \"Bob\", \"Charlie\"}\n" +
            "    fmt.Println(len(names))\n" +
            "    fmt.Println(slices.Index(names, \"Bob\"))\n" +
            "    fmt.Println(slices.Index(names, \"Dave\"))\n" +
            "    joined := strings.Join(names, \"-\")\n" +
            "    fmt.Println(joined)\n" +
            "}\n";

        Lexer lexer = new Lexer(new BufferedReader(new StringReader(input)));
        parser p = new parser(lexer);
        ASTNode ast = (ASTNode) p.parse().value;

        assertTrue("Should have no parser errors", p.errors.isEmpty());

        InterpreterVisitor interpreter = new InterpreterVisitor();
        interpreter.Visit(ast);
        String output = interpreter.output;

        assertEquals("3\n1\n-1\nAlice-Bob-Charlie\n", output.replace("\r\n", "\n"));
    }

    @Test
    public void testCStyleStructSyntax() throws Exception {
        com.olc1.reports.ErrorCollector.clear();
        String input =
            "struct Persona {\n" +
            "    string Nombre;\n" +
            "    int Edad;\n" +
            "    bool EsEstudiante;\n" +
            "}\n" +
            "Persona miInstancia = { Nombre: \"Alice\", Edad: 25, EsEstudiante: false };\n" +
            "string nombre = miInstancia.Nombre;\n" +
            "miInstancia.Nombre = \"Bob\";\n" +
            "miInstancia.Edad = 30;\n" +
            "fmt.Println(miInstancia.Edad)\n" +
            "fmt.Println(miInstancia.Nombre)\n" +
            "fmt.Println(nombre)\n";

        Lexer lexer = new Lexer(new BufferedReader(new StringReader(input)));
        parser p = new parser(lexer);
        ASTNode ast = (ASTNode) p.parse().value;

        for (GoLiteError err : p.errors) {
            System.out.println("PARSER ERROR in testCStyleStructSyntax: " + err.getDescription());
        }
        assertTrue("Should have no parser errors", p.errors.isEmpty());

        InterpreterVisitor interpreter = new InterpreterVisitor();
        interpreter.Visit(ast);
        String output = interpreter.output;

        for (GoLiteError err : com.olc1.reports.ErrorCollector.getErrors()) {
            System.out.println("SEMANTIC ERROR in testCStyleStructSyntax: " + err.getDescription());
        }
        System.out.println("Output: [" + output + "]");
        assertEquals("30\nBob\nAlice\n", output.replace("\r\n", "\n"));
        assertTrue("Should have no semantic errors", com.olc1.reports.ErrorCollector.getErrors().isEmpty());
    }

    @Test
    public void testMultiDimensionalSliceLiteral() throws Exception {
        com.olc1.reports.ErrorCollector.clear();
        String input =
            "mtx2 := [][]int{\n" +
            "    {0, 0, 0},\n" +
            "    {0, 0, 0},\n" +
            "    {0, 0, 0},\n" +
            "}\n" +
            "mtx2[0][0] = 7\n" +
            "mtx2[0][1] = 6\n" +
            "mtx2[0][2] = 5\n" +
            "fmt.Println(mtx2[0][1])\n";

        Lexer lexer = new Lexer(new BufferedReader(new StringReader(input)));
        parser p = new parser(lexer);
        ASTNode ast = (ASTNode) p.parse().value;

        for (GoLiteError err : p.errors) {
            System.out.println("PARSER ERROR in testMultiDimensionalSliceLiteral: " + err.getDescription());
        }
        assertTrue("Should have no parser errors", p.errors.isEmpty());

        InterpreterVisitor interpreter = new InterpreterVisitor();
        interpreter.Visit(ast);
        String output = interpreter.output;

        for (GoLiteError err : com.olc1.reports.ErrorCollector.getErrors()) {
            System.out.println("SEMANTIC ERROR in testMultiDimensionalSliceLiteral: " + err.getDescription());
        }
        assertTrue("Should have no semantic errors", com.olc1.reports.ErrorCollector.getErrors().isEmpty());
        assertEquals("6\n", output.replace("\r\n", "\n"));
    }

    @Test
    public void testUserCodeWithFuncCorrected() throws Exception {
        com.olc1.reports.ErrorCollector.clear();
        String input =
            "struct Producto {\n" +
            "    int id;\n" +
            "    string nombre;\n" +
            "}\n" +
            "// Función que devuelve un valor entero func\n" +
            "func obtenerNumero() int {\n" +
            "    return 42;\n" +
            "}\n" +
            "func imprimirMensaje() {\n" +
            "    fmt.Println(\"Hola, GoLite!\");\n" +
            "}\n" +
            "func sumar(a int, b int) int {\n" +
            "    return a + b;\n" +
            "}\n" +
            "func actualizarProducto(p Producto, nuevoNombre string) {\n" +
            "    p.nombre = nuevoNombre;\n" +
            "}\n" +
            "func agregarElemento(slice []int, valor int) []int {\n" +
            "    return append(slice, valor);\n" +
            "}\n" +
            "func main() {\n" +
            "    fmt.Println(obtenerNumero());\n" +
            "    imprimirMensaje();\n" +
            "    fmt.Println(sumar(5, 10));\n" +
            "    Producto p = { id: 1, nombre: \"Producto A\" };\n" +
            "    actualizarProducto(p, \"Producto B\");\n" +
            "    fmt.Println(p.nombre);\n" +
            "    numeros := []int{1, 2, 3};\n" +
            "    numeros = agregarElemento(numeros, 4);\n" +
            "    fmt.Println(numeros);\n" +
            "}\n";

        Lexer lexer = new Lexer(new BufferedReader(new StringReader(input)));
        parser p = new parser(lexer);
        ASTNode ast = (ASTNode) p.parse().value;

        for (GoLiteError err : p.errors) {
            System.out.println("PARSER ERROR in testUserCodeWithFuncCorrected: " + err.getDescription() + " at line " + err.getLine() + ", column " + err.getColumn());
        }
        assertTrue("Should have no parser errors", p.errors.isEmpty());

        InterpreterVisitor interpreter = new InterpreterVisitor();
        interpreter.Visit(ast);
        String output = interpreter.output;

        for (GoLiteError err : com.olc1.reports.ErrorCollector.getErrors()) {
            System.out.println("SEMANTIC ERROR in testUserCodeWithFuncCorrected: " + err.getDescription());
        }
        assertTrue("Should have no semantic errors", com.olc1.reports.ErrorCollector.getErrors().isEmpty());
        assertEquals("42\nHola, GoLite!\n15\nProducto B\n[1, 2, 3, 4]\n", output.replace("\r\n", "\n"));

        // Verify some symbol table entries
        boolean foundProductoStruct = false;
        boolean foundObtenerNumeroFunc = false;
        boolean foundImprimirMensajeProc = false;
        boolean foundPLocalVarInMain = false;
        boolean foundNumerosSliceInMain = false;

        for (SymbolEntry entry : interpreter.symbolTable) {
            System.out.println("SYMBOL: " + entry.toTableRow());
            if (entry.getId().equals("Producto") && entry.getTipoSimbolo().equals("Struct") && entry.getAmbito().equals("Global")) {
                foundProductoStruct = true;
            }
            if (entry.getId().equals("obtenerNumero") && entry.getTipoSimbolo().equals("Función") && entry.getTipoDato().equals("int") && entry.getAmbito().equals("Global")) {
                foundObtenerNumeroFunc = true;
            }
            if (entry.getId().equals("imprimirMensaje") && entry.getTipoSimbolo().equals("Procedimiento") && entry.getTipoDato().equals("void") && entry.getAmbito().equals("Global")) {
                foundImprimirMensajeProc = true;
            }
            if (entry.getId().equals("p") && entry.getTipoSimbolo().equals("Variable") && entry.getTipoDato().equals("Producto") && entry.getAmbito().equals("main")) {
                foundPLocalVarInMain = true;
            }
            if (entry.getId().equals("numeros") && entry.getTipoSimbolo().equals("Variable") && entry.getTipoDato().equals("Slice") && entry.getAmbito().equals("main")) {
                foundNumerosSliceInMain = true;
            }
        }

        assertTrue("Should find Producto struct in symbol table", foundProductoStruct);
        assertTrue("Should find obtenerNumero function in symbol table", foundObtenerNumeroFunc);
        assertTrue("Should find imprimirMensaje procedure in symbol table", foundImprimirMensajeProc);
        assertTrue("Should find local variable p inside main", foundPLocalVarInMain);
        assertTrue("Should find local variable numeros slice inside main", foundNumerosSliceInMain);
    }
}



