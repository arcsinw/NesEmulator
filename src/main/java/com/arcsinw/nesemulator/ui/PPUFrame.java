package com.arcsinw.nesemulator.ui;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class PPUFrame extends Frame {
    public PPUFrame() {
        setTitle("");
        this.addWindowListener(new WindowAdapter(){
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });
    }
}
