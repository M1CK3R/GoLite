package com.olc1.gui;

import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

public class GoliteMenuBar extends JMenuBar {
    private final JMenuItem newItem;
    private final JMenuItem openItem;
    private final JMenuItem saveItem;
    private final JMenuItem exitItem;
    private final JButton runButton;
    private final JButton cleanButton;
    private final JMenuItem tokensItem;
    private final JMenuItem symbolTableItem;
    private final JMenuItem errorsItem;
    private final JMenuItem astItem;
    private final JMenuItem aboutItem;

    public GoliteMenuBar() {
        JMenu fileMenu = new JMenu("Archivo");
        runButton = createButton("Ejecutar");
        JMenu reportMenu = new JMenu("Reportes");
        cleanButton = createButton("Limpiar consola");
        JMenu helpMenu = new JMenu("Ayuda");

        newItem = new JMenuItem("Nuevo");
        openItem = new JMenuItem("Abrir archivo");
        saveItem = new JMenuItem("Guardar archivo");
        exitItem = new JMenuItem("Salir");
        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        tokensItem = new JMenuItem("Reporte de tokens");
        symbolTableItem = new JMenuItem("Reporte de tabla de símbolos");
        errorsItem = new JMenuItem("Reporte de errores");
        astItem = new JMenuItem("Reporte AST");
        reportMenu.add(tokensItem);
        reportMenu.add(symbolTableItem);
        reportMenu.add(errorsItem);
        reportMenu.add(astItem);

        aboutItem = new JMenuItem("Acerca de");
        helpMenu.add(aboutItem);

        add(fileMenu);
        add(runButton);
        add(reportMenu);
        add(cleanButton);
        add(helpMenu);
    }

    public void onRun(ActionListener l) {
        runButton.addActionListener(l);
    }

    public void onClean(ActionListener l) {
        cleanButton.addActionListener(l);
    }

    public void onNew(ActionListener l) {
        newItem.addActionListener(l);
    }

    public void onOpen(ActionListener l) {
        openItem.addActionListener(l);
    }

    public void onSave(ActionListener l) {
        saveItem.addActionListener(l);
    }

    public void onExit(ActionListener l) {
        exitItem.addActionListener(l);
    }

    public void onTokens(ActionListener l) {
        tokensItem.addActionListener(l);
    }

    public void onErrors(ActionListener l) {
        errorsItem.addActionListener(l);
    }

    public void onSymbolTable(ActionListener l) {
        symbolTableItem.addActionListener(l);
    }

    public void onAbout(ActionListener l) {
        aboutItem.addActionListener(l);
    }

    public void onAst(ActionListener l) {
        astItem.addActionListener(l);
    }

    private static JButton createButton(String text) {
        JButton btn = new JButton(text);
        btn.putClientProperty("JButton.buttonType", "toolBarButton");
        return btn;
    }
}