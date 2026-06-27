package com.olc1.reports;

import com.olc1.ast.ASTNode;
import com.olc1.visitor.graphviz.GraphvizAstVisitor;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Genera el reporte del Árbol de Sintaxis Abstracta (AST) en formato SVG.
 */
public class AstReportGenerator {

    // Directorio por defecto donde se guardarán los reportes
    private static final String DEFAULT_REPORT_DIR = "reportes";

    /**
     * Genera el reporte AST en SVG y retorna la ruta absoluta del archivo generado.
     *
     * @param ast            Nodo raíz del AST (normalmente una instancia de
     *                       Statments).
     * @param outputFileName Nombre del archivo de salida (sin extensión). Si es
     *                       null, se genera uno automático con timestamp.
     * @return Ruta absoluta del archivo SVG generado.
     * @throws AstReportException Si ocurre un error durante la generación (AST
     *                            nulo, error de escritura, etc.).
     */
    public static String generate(ASTNode ast, String outputFileName) throws AstReportException {
        // 1. Validar que el AST no sea nulo
        if (ast == null) {
            throw new AstReportException("No se puede generar el AST: el árbol es nulo.");
        }

        // 2. Verificar que no haya errores léxicos/sintácticos previos que invaliden el
        // AST
        if (!ErrorCollector.getErrors().isEmpty()) {
            // Puedes decidir si lanzar excepción o solo advertir.
            // Es mejor lanzar excepción para no generar un AST incompleto.
            throw new AstReportException("No se puede generar el AST: existen errores léxicos o sintácticos. "
                    + "Corrígelos primero.");
        }

        // 3. Crear el directorio de reportes si no existe
        File reportDir = new File(DEFAULT_REPORT_DIR);
        if (!reportDir.exists()) {
            boolean created = reportDir.mkdirs();
            if (!created) {
                throw new AstReportException("No se pudo crear el directorio de reportes: " + DEFAULT_REPORT_DIR);
            }
        }

        // 4. Definir el nombre del archivo de salida
        if (outputFileName == null || outputFileName.trim().isEmpty()) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            outputFileName = "ast_" + timestamp;
        }
        // Asegurar que tenga extensión .svg
        if (!outputFileName.endsWith(".svg") && !outputFileName.endsWith(".SVG")) {
            outputFileName = outputFileName + ".svg";
        }

        // 5. Ruta completa del archivo
        String outputPath = reportDir.getAbsolutePath() + File.separator + outputFileName;

        try {
            // 6. Instanciar el visitor de Graphviz
            GraphvizAstVisitor visitor = new GraphvizAstVisitor();

            // 7. Recorrer el AST (esto construye el contenido DOT internamente en el
            // visitor)
            ast.accept(visitor);

            // 8. Generar el archivo SVG
            // Puedes elegir aquí qué método de generación usar.
            // Si tienes la dependencia "graphviz-java", usa visitor.generateSVG(outputPath)
            // Si prefieres usar el comando "dot" del sistema, usa
            // visitor.generateSVGWithDot(dotPath, outputPath)

            // *** OPCIÓN RECOMENDADA (dependencia graphviz-java) ***
            visitor.generateSVG(outputPath);

            // *** OPCIÓN ALTERNATIVA (requiere Graphviz instalado en el sistema) ***
            // String dotPath = reportDir.getAbsolutePath() + File.separator +
            // outputFileName.replace(".svg", ".dot");
            // visitor.generateSVGWithDot(dotPath, outputPath);

            // 9. Retornar la ruta absoluta para que la GUI pueda abrir el archivo
            return new File(outputPath).getAbsolutePath();

        } catch (IOException e) {
            throw new AstReportException("Error al escribir el archivo SVG: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new AstReportException("Error inesperado al generar el AST: " + e.getMessage(), e);
        }
    }

    /**
     * Sobrecarga: genera el AST con un nombre automático basado en timestamp.
     *
     * @param ast Nodo raíz del AST.
     * @return Ruta absoluta del archivo SVG generado.
     * @throws AstReportException Si ocurre un error.
     */
    public static String generate(ASTNode ast) throws AstReportException {
        return generate(ast, null);
    }

    /**
     * Método de utilidad para abrir el archivo generado en el navegador o visor
     * predeterminado del sistema.
     * (Opcional: puede ser llamado desde la GUI después de generate).
     *
     * @param filePath Ruta del archivo a abrir.
     * @throws IOException Si no se puede abrir el archivo.
     */
    public static void openReport(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("El archivo no existe: " + filePath);
        }
        java.awt.Desktop.getDesktop().open(file);
    }
}