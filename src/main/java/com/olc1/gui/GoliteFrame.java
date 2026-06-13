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
import com.olc1.reports.ErrorCollector;
import com.olc1.reports.ErrorReportGenerator;
import com.olc1.reports.GoLiteError;
import com.olc1.reports.SymbolEntry;
import com.olc1.visitor.interpreter.InterpreterVisitor;

public class GoliteFrame extends JFrame {
    private final EditorPanel editorPanel;
    private final JTextArea consoleTextArea;
    private Lexer lexer;
    private parser parser;
    private InterpreterVisitor interpreter;

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

    private void run() {
        cleanConsole(); // limpiamos antes de empezar
        ErrorCollector.clear(); // limpiar errores semánticos previos

        ASTNode ast = null;
        try {
            lexer = new Lexer(new BufferedReader(new StringReader(editorPanel.getText())));
            parser = new parser(lexer);
            ast = (ASTNode) parser.parse().value;
        } catch (Exception e) {
            // El parser ya registró los errores en parser.errors
            // Si no pudo recuperarse, ast queda null
        }

        // Si se logró construir un AST (parcial o completo), intentar interpretar
        if (ast != null) {
            try {
                interpreter = new InterpreterVisitor();
                interpreter.Visit(ast);
                consoleTextArea.append(interpreter.output);
            } catch (Exception e) {
                // Capturar errores semánticos/runtime y agregarlos al reporte silenciosamente
                ErrorCollector.addError("semántico", e.getMessage(), 0, 0);
                if (interpreter != null && !interpreter.output.isEmpty()) {
                    consoleTextArea.append(interpreter.output);
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
