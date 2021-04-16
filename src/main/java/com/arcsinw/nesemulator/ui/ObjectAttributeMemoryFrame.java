package com.arcsinw.nesemulator.ui;

import com.arcsinw.nesemulator.PPU;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.stream.IntStream;

public class ObjectAttributeMemoryFrame extends JFrame {
    private static final int SCREEN_WIDTH = 128;
    private static final int SCREEN_HEIGHT = 128;
    private static final int SCREEN_RATIO = 3;

    private DefaultListModel defaultListModel = new DefaultListModel();
    private JList<String> oamList = new JList<String>(defaultListModel);
    private JScrollPane scroll;

    private Panel patternTablePanel = new Panel();
    private PPU ppu;

    public ObjectAttributeMemoryFrame(PPU ppu) {
        this.ppu = ppu;

        setTitle("Object Attribute Memory");
        setBackground(Color.black);
        setLayout(new BorderLayout());

        oamList.setPreferredSize(new Dimension(SCREEN_WIDTH * SCREEN_RATIO, SCREEN_HEIGHT * SCREEN_RATIO));
        oamList.setVisibleRowCount(16);
        scroll = new JScrollPane(
                oamList,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );
        add(BorderLayout.LINE_END,scroll);

        add(oamList);
        pack();

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        displayOAM();

        setVisible(true);

        setAutoRequestFocus(false);

        Timer timer = new Timer(1000, arg -> displayOAM());
        timer.start();
    }

    private void displayOAM() {
        if (ppu != null) {
            PPU.OAMEntry[] oam = new PPU.OAMEntry[64];
            IntStream.range(0, 64).forEach(i -> {
                oam[i] = new PPU.OAMEntry(ppu.oam[i * 4], ppu.oam[i * 4 + 1], ppu.oam[i * 4 + 2], ppu.oam[i * 4 + 3]);
            });

            defaultListModel.removeAllElements();

            IntStream.range(0, 64).forEach(i -> {
                defaultListModel.addElement(oam[i].toString());
            });

            scroll.updateUI();
//            revalidate();
        }
    }
}
