package com.olc1;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;
import java.io.StringReader;
import java.io.BufferedReader;
import com.olc1.reports.GoLiteError;
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
        assertTrue("Should have no parser errors", p.errors.isEmpty());

        InterpreterVisitor interpreter = new InterpreterVisitor();
        interpreter.Visit(ast);
        String output = interpreter.output;
        System.out.println("Output of execution:\n" + output);

        String expected = "0\n1\n2\n3\n4\n5\n4\n";
        assertEquals(expected, output.replace("\r\n", "\n"));
    }
}


