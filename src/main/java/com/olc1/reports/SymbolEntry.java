package com.olc1.reports;

public class SymbolEntry {
    private final String id;
    private final String tipoSimbolo;
    private final String tipoDato;
    private final String ambito;
    private final int line;
    private final int column;

    public SymbolEntry(String id, String tipoSimbolo, String tipoDato, String ambito, int line, int column) {
        this.id = id;
        this.tipoSimbolo = tipoSimbolo;
        this.tipoDato = tipoDato;
        this.ambito = ambito;
        this.line = line;
        this.column = column;
    }

    public String getId() { return id; }
    public String getTipoSimbolo() { return tipoSimbolo; }
    public String getTipoDato() { return tipoDato; }
    public String getAmbito() { return ambito; }
    public int getLine() { return line; }
    public int getColumn() { return column; }

    /**
     * Returns a formatted row for the symbol table report.
     */
    public String toTableRow() {
        return String.format("%-15s %-15s %-15s %-15s %-6d %d",
                id, tipoSimbolo, tipoDato, ambito, line, column);
    }

    /**
     * Returns the header for the symbol table.
     */
    public static String tableHeader() {
        return String.format("%-15s %-15s %-15s %-15s %-6s %s",
                "ID", "Tipo símbolo", "Tipo dato", "Ámbito", "Línea", "Columna");
    }

    /**
     * Returns a separator line for the table.
     */
    public static String tableSeparator() {
        return "---------------------------------------------------------------------------------";
    }
}
