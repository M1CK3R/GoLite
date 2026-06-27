package com.olc1.gui;

import java.awt.Desktop;
import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.olc1.Lexer;
import com.olc1.ast.ASTNode;
import com.olc1.parser;
import com.olc1.reports.*;
import com.olc1.visitor.interpreter.InterpreterVisitor;

public class GoliteFrame extends JFrame {
    private final EditorPanel editorPanel;
    private final JTextArea consoleTextArea;
    private Lexer lexer;
    private parser parser;
    private InterpreterVisitor interpreter;
    private ASTNode currentAst;

    public GoliteFrame() {
        setTitle("Golite");
        setMinimumSize(new Dimension(600, 400));
        setSize(new Dimension(1200, 675));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        editorPanel = new EditorPanel();
        consoleTextArea = new JTextArea();
        cleanConsole();

        GoliteMenuBar menuBar = new GoliteMenuBar();
        setJMenuBar(menuBar);
        add(new MainPanel(editorPanel, consoleTextArea));

        wireActions(menuBar);

        setVisible(true);
        editorPanel.getTextArea().requestFocus();
    }

    private void wireActions(GoliteMenuBar menuBar) {
        menuBar.onRun(e -> run());
        menuBar.onClean(e -> cleanConsole());
        menuBar.onNew(e -> editorPanel.setText(""));
        menuBar.onOpen(e -> openFile());
        menuBar.onSave(e -> saveFile());
        menuBar.onExit(e -> System.exit(0));
        menuBar.onTokens(e -> {
            tokens();
        });
        menuBar.onAst(e -> generarReporteAST());
        menuBar.onSymbolTable(e -> {
            symbolTable();
        });
        menuBar.onErrors(e -> {
            errors();
        });
        menuBar.onAbout(e -> JOptionPane.showMessageDialog(
                this,
                "GolLite\nVersión 1.0.0\nLaboratorio OLC1",
                "Acerca de",
                JOptionPane.INFORMATION_MESSAGE));
    }

    private void openFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Abrir archivo GoLite");
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Archivos de GoLite (*.glt)", "glt");
        chooser.setFileFilter(filter);
        chooser.setAcceptAllFileFilterUsed(true);

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                String content = new String(Files.readAllBytes(file.toPath()));
                editorPanel.setText(content);
                setTitle("Golite — " + file.getName());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "No se pudo leer el archivo:\n" + ex.getMessage(),
                        "Error al abrir", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar archivo GoLite");
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Archivos GoLite (*.glt)", "glt");
        chooser.setFileFilter(filter);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setSelectedFile(new File("programa.glt"));
        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            // Asegurar extensión .glt
            if (!file.getName().toLowerCase().endsWith(".glt")) {
                file = new File(file.getParentFile(), file.getName() + ".glt");
            }
            try {
                Files.write(file.toPath(), editorPanel.getText().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                setTitle("Golite — " + file.getName());
                JOptionPane.showMessageDialog(this,
                        "Archivo guardado en:\n" + file.getAbsolutePath(),
                        "Guardado", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "No se pudo guardar el archivo:\n" + ex.getMessage(),
                        "Error al guardar", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void generarReporteAST() {
        // Asegurarnos de que tengamos un AST disponible
        if (currentAst == null) {
            consoleTextArea.append("No hay un AST disponible. Ejecuta el código primero.\n");
            return;
        }

        // Opcional: si hay errores, advertir pero igual generar (puede estar parcial)
        if (!ErrorCollector.getErrors().isEmpty()) {
            consoleTextArea.append("Advertencia: existen errores semánticos, el AST puede estar incompleto.\n");
        }

        try {
            // Generar el reporte usando el AST guardado
            String filePath = AstReportGenerator.generate(currentAst, null);

            consoleTextArea.append("Reporte AST generado exitosamente: " + filePath + "\n");

            try {
                AstReportGenerator.openReport(filePath);
            } catch (IOException e) {
                consoleTextArea.append("No se pudo abrir el archivo automáticamente: " + e.getMessage() + "\n");
            }

        } catch (AstReportException e) {
            consoleTextArea.append("Error al generar el AST: " + e.getMessage() + "\n");
        } catch (Exception e) {
            consoleTextArea.append("Error inesperado: " + e.getMessage() + "\n");
            e.printStackTrace();
        }
    }

    private void run() {
        cleanConsole();
        ErrorCollector.clear();

        currentAst = parsearCodigo(editorPanel.getText());

        // Re-parsear para tener lexer y parser frescos para el reporte de errores
        try {
            lexer = new Lexer(new BufferedReader(new StringReader(editorPanel.getText())));
            parser = new parser(lexer);
            parser.parse();

            if (parser != null && !parser.errors.isEmpty()) {
                consoleTextArea.append("--- Errores sintácticos ---\n");
                for (GoLiteError err : parser.errors) {
                    consoleTextArea.append(err.getDescription() + " (línea " + err.getLine() + ")\n");
                }
            }
            if (lexer != null && !lexer.errors.isEmpty()) {
                consoleTextArea.append("--- Errores léxicos ---\n");
                for (GoLiteError err : lexer.errors) {
                    consoleTextArea.append(err.getDescription() + " (línea " + err.getLine() + ")\n");
                }
            }
        } catch (Exception e) {
            // El parser ya registró los errores en parser.errors
        }

        // Si se logró construir un AST, interpretar
        if (currentAst != null) {
            interpreter = new InterpreterVisitor();

            try {
                interpreter.Visit(currentAst);
            } catch (Exception e) {
                // Solo llega aquí si hay un error que el modo pánico no atrapó
                ErrorCollector.addError("Semántico",
                        "Error crítico: " + e.getMessage(), 0, 0);
            } finally {
                // SIEMPRE mostrar el output generado hasta donde llegó
                if (!interpreter.output.isEmpty()) {
                    consoleTextArea.append(interpreter.output);
                }

                // Pasar todos los errores del intérprete al ErrorCollector
                for (GoLiteError error : interpreter.errors) {
                    ErrorCollector.addError(
                            error.getType(),
                            error.getDescription(),
                            error.getLine(),
                            error.getColumn());
                }
            }
        }

        consoleTextArea.setCaretPosition(consoleTextArea.getDocument().getLength());
        editorPanel.getTextArea().requestFocus();
    }

    private void errors() {
        if (lexer == null || parser == null) {
            JOptionPane.showMessageDialog(this,
                    "Ejecuta el código primero antes de ver el reporte de errores.",
                    "Sin ejecución", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<GoLiteError> lexicErrors = new ArrayList<>(lexer.errors);
        List<GoLiteError> syntaxErrors = new ArrayList<>(parser.errors);
        List<GoLiteError> semanticErrors = new ArrayList<>(ErrorCollector.getErrors());

        try {
            File htmlFile = ErrorReportGenerator.generate(lexicErrors, syntaxErrors, semanticErrors);

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(htmlFile.toURI());
                cleanConsole();
                int total = lexicErrors.size() + syntaxErrors.size() + semanticErrors.size();
                consoleTextArea.append("Reporte de errores generado.\n");
                consoleTextArea.append("  Léxicos:    " + lexicErrors.size() + "\n");
                consoleTextArea.append("  Sintácticos: " + syntaxErrors.size() + "\n");
                consoleTextArea.append("  Semánticos: " + semanticErrors.size() + "\n");
                consoleTextArea.append("  Total:       " + total + "\n");
                consoleTextArea.append("\nEl reporte HTML se abrió en el navegador.\n");
            } else {
                cleanConsole();
                consoleTextArea.append("Reporte generado en: " + htmlFile.getAbsolutePath() + "\n");
                consoleTextArea.append("(Abre ese archivo manualmente en tu navegador)\n");
            }
        } catch (IOException ex) {
            cleanConsole();
            consoleTextArea.append("Error al generar el reporte HTML: " + ex.getMessage() + "\n");
        }
    }

    private ASTNode parsearCodigo(String codigo) {
        parser tempParser = null;
        try {
            Lexer tempLexer = new Lexer(new BufferedReader(new StringReader(codigo)));
            tempParser = new parser(tempLexer);
            ASTNode result = (ASTNode) tempParser.parse().value;
            if (result != null)
                return result;
        } catch (Exception e) {
            // intento de recuperar el AST parcial construido hasta el error
        }
        // si parse() falló, intentar retornar lo que alcanzó a construir
        return (tempParser != null) ? tempParser.partialAst : null;
    }

    private void symbolTable() {
        cleanConsole();

        if (interpreter == null) {
            consoleTextArea.append("Aún no se ha ejecutado nada.\n");
            return;
        }

        consoleTextArea.append("=== Reporte de Tabla de Símbolos ===\n\n");
        consoleTextArea.append(SymbolEntry.tableHeader() + "\n");
        consoleTextArea.append(SymbolEntry.tableSeparator() + "\n");

        if (interpreter.symbolTable.isEmpty()) {
            consoleTextArea.append("(No se encontraron símbolos declarados)\n");
        } else {
            for (SymbolEntry entry : interpreter.symbolTable) {
                consoleTextArea.append(entry.toTableRow() + "\n");
            }
        }

        consoleTextArea.append("\nTotal de símbolos: " + interpreter.symbolTable.size() + "\n");
    }

    private void tokens() {
        cleanConsole();

        if (lexer == null) {
            consoleTextArea.append("Ejecuta el código primero antes de ver el reporte de tokens.\n");
            return;
        }

        consoleTextArea.append("=== Reporte de Tokens ===\n\n");
        consoleTextArea.append(TokenEntry.tableHeader() + "\n");
        consoleTextArea.append(TokenEntry.tableSeparator() + "\n");

        if (lexer.tokens.isEmpty()) {
            consoleTextArea.append("(No se encontraron tokens)\n");
        } else {
            for (TokenEntry entry : lexer.tokens) {
                consoleTextArea.append(entry.toTableRow() + "\n");
            }
        }

        consoleTextArea.append("\nTotal de tokens: " + lexer.tokens.size() + "\n");
    }

    private void cleanConsole() {
        consoleTextArea.setText("CONSOLA  -  LABORATORIO DE ORGANIZACION DE LENGUAJES Y COMPILADORES 1\n\n");
    }

    public EditorPanel getEditorPanel() {
        return editorPanel;
    }

    public JTextArea getConsoleTextArea() {
        return consoleTextArea;
    }
}
