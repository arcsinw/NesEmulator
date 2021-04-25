package com.arcsinw.nesemulator.ui;

import com.arcsinw.nesemulator.PPU;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
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


    static JTabbedPane tabbed = new JTabbedPane();

    public void addTab(String title, JPanel panel, PPU.OAMEntry[] data) {
        JPanel temp = new JPanel() {
            @Override
            public void paint(Graphics g) {
                displayOAM();
            }
        };
        temp.setLayout(new BorderLayout());
        temp.add(panel);
        tabbed.add(title, temp);
    }

    public void addTab(String title, JPanel panel, ArrayList<PPU.OAMEntry> data) {
        JPanel temp = new JPanel() {
            @Override
            public void paint(Graphics g) {
                displayOAM();
            }
        };
        temp.setLayout(new BorderLayout());
        temp.add(panel);
        tabbed.add(title, temp);
    }

    private void initFrame() {
        setTitle("Object Attribute Memory");
        setBackground(Color.black);
        add(tabbed);
        pack();
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setVisible(true);
        setAutoRequestFocus(false);
    }

    public ObjectAttributeMemoryFrame(PPU ppu) {
        initFrame();

        this.ppu = ppu;
        oamList.setVisibleRowCount(16);
        oamList.setPreferredSize(new Dimension(SCREEN_WIDTH * SCREEN_RATIO, SCREEN_HEIGHT * SCREEN_RATIO));

        scroll = new JScrollPane(
                oamList,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );
        setLayout(new BorderLayout());
        add(BorderLayout.LINE_END,scroll);
        add(oamList);

//        addTab("OAM", this, ppu.oam);
//        addTab("ScanLine OAM", this, ppu.scanLineSprite);
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
