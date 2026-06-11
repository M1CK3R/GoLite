package com.olc1.reports;

public class SymbolEntry {
    private final int number;
    private final String lexema;
    private final String tipo;
    private final int line;
    private final int column;

    public SymbolEntry(int number, String lexema, String tipo, int line, int column) {
        this.number = number;
        this.lexema = lexema;
        this.tipo = tipo;
        this.line = line;
        this.column = column;
    }

    public int getNumber() { return number; }
    public String getLexema() { return lexema; }
    public String getTipo() { return tipo; }
    public int getLine() { return line; }
    public int getColumn() { return column; }

    /**
     * Returns a formatted row for the symbol table report.
     */
    public String toTableRow() {
        return String.format("%-5d| %-20s| %-12s| %-7d| %d",
                number, lexema, tipo, line, column);
    }

    /**
     * Returns the header for the symbol table.
     */
    public static String tableHeader() {
        return String.format("%-5s| %-20s| %-12s| %-7s| %s",
                "No.", "Lexema", "Tipo", "Línea", "Columna");
    }

    /**
     * Returns a separator line for the table.
     */
    public static String tableSeparator() {
        return "-----+---------------------+-------------+--------+--------";
    }
}
