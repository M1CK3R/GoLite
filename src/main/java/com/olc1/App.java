package com.olc1;

import javax.swing.SwingUtilities;

import com.formdev.flatlaf.FlatDarkLaf;
import com.olc1.gui.GoliteFrame;

// Hello world!

public class App 
{
    public static void main( String[] args )
    {
        // Configurar Look & Feel Oscuro
        FlatDarkLaf.setup();
        
        SwingUtilities.invokeLater(() -> {
            new GoliteFrame();
        });
    }
}

