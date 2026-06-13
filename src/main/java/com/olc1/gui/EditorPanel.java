package com.olc1.gui;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;


import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import java.awt.*;


public class EditorPanel extends JPanel {
    private final RSyntaxTextArea textArea;
    private final JLabel statusLabel;

    public EditorPanel() {
        setLayout(new BorderLayout());

        textArea = new RSyntaxTextArea();
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_GO);
        textArea.setCodeFoldingEnabled(true);
        textArea.setAntiAliasingEnabled(true);
        textArea.setAutoIndentEnabled(true);
        textArea.setTabSize(4);

        try {
            org.fife.ui.rsyntaxtextarea.Theme theme = org.fife.ui.rsyntaxtextarea.Theme.load(
                getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/dark.xml")
            );
            theme.apply(textArea);
        } catch (Exception e) {
            textArea.setBackground(new Color(0x1e, 0x1e, 0x1e));
            textArea.setForeground(Color.LIGHT_GRAY);
            textArea.setCaretColor(Color.WHITE);
        }

        RTextScrollPane scrollPane = new RTextScrollPane(textArea);
        add(scrollPane, BorderLayout.CENTER);

        statusLabel = new JLabel("Lín: 1, Col: 1");
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        statusBar.setBorder(BorderFactory.createEtchedBorder());
        statusBar.add(statusLabel);
        add(statusBar, BorderLayout.SOUTH);

        textArea.addCaretListener(new CaretListener() {
            @Override
            public void caretUpdate(CaretEvent e) {
                try {
                    int line = textArea.getCaretLineNumber() + 1;
                    int offset = textArea.getCaretPosition();
                    int lineStart = textArea.getLineStartOffset(line - 1);
                    int col = offset - lineStart + 1;
                    statusLabel.setText("Lín: " + line + ", Col: " + col);
                } catch (Exception ex) {
                    statusLabel.setText("Lín: --, Col: --");
                }
            }
        });
    }

    public RSyntaxTextArea getTextArea() {
        return textArea;
    }

    public String getText() {
        return textArea.getText();
    }

    public void setText(String text) {
        textArea.setText(text);
    }
}
