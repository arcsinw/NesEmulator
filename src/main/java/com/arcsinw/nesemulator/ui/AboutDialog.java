package com.arcsinw.nesemulator.ui;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;


public class AboutDialog extends Dialog {
    public AboutDialog(Frame owner) {
        super(owner);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });

        setTitle("关于");
    }
}
