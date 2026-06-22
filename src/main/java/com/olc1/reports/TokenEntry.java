package com.olc1.reports;

/**
 * Represents a single token found during lexical analysis.
 */
public class TokenEntry {
    private final String tipo;
    private final String lexema;
    private final int linea;
    private final int columna;

    public TokenEntry(String tipo, String lexema, int linea, int columna) {
        this.tipo = tipo;
        this.lexema = lexema;
        this.linea = linea;
        this.columna = columna;
    }

    public String getTipo() { return tipo; }
    public String getLexema() { return lexema; }
    public int getLinea() { return linea; }
    public int getColumna() { return columna; }

    /**
     * Returns a formatted row for the token report.
     */
    public String toTableRow() {
        // Escape special characters for display
        String displayLexema = lexema.replace("\n", "\\n")
                                     .replace("\r", "\\r")
                                     .replace("\t", "\\t");
        return String.format("%-20s %-30s %-8d %d",
                tipo, displayLexema, linea, columna);
    }

    /**
     * Returns the header for the token table.
     */
    public static String tableHeader() {
        return String.format("%-20s %-30s %-8s %s",
                "Tipo", "Lexema", "Línea", "Columna");
    }

    /**
     * Returns a separator line for the table.
     */
    public static String tableSeparator() {
        return "------------------------------------------------------------------------";
    }
}
