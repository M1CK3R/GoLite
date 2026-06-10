package com.olc1;

import javax.swing.SwingUtilities;

import com.olc1.gui.GoliteFrame;

// Hello world!

public class App 
{
    public static void main( String[] args )
    {
        SwingUtilities.invokeLater(() -> {
            new GoliteFrame();
        });
    }
}

