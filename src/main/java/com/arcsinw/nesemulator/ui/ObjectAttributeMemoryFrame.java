package com.arcsinw.nesemulator.ui;

import com.arcsinw.nesemulator.PPU;

import javax.swing.*;
import java.awt.*;
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

        setVisible(true);
        setAutoRequestFocus(false);

        Timer timer = new Timer(20, arg -> displayOAM());
        timer.start();
    }

    private void displayOAM() {
        if (ppu != null) {
            defaultListModel.removeAllElements();

            IntStream.range(0, 64).forEach(i -> {
                defaultListModel.addElement(ppu.oam[i].toString());
            });

            scroll.updateUI();
            revalidate();
        }
    }
}
