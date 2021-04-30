package com.arcsinw.nesemulator.ui;

import com.arcsinw.nesemulator.PPU;

import javax.swing.*;
import java.awt.*;
import java.util.stream.IntStream;

public class ObjectAttributeMemoryFrame extends JFrame {
    private class ObjectAttributeMemoryPanel extends JPanel {
        private DefaultListModel defaultListModel = new DefaultListModel();
        private JList<String> oamList = new JList<String>(defaultListModel);
        private JScrollPane scrollPanel;

        public ObjectAttributeMemoryPanel(PPU.OAMEntry[] data) {
            scrollPanel = new JScrollPane(
                    oamList,
                    JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
            );
            scrollPanel.setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
            scrollPanel.setWheelScrollingEnabled(true);

            setLayout(new BorderLayout());
            add(scrollPanel);
            pack();

            setVisible(true);
            Timer timer = new Timer(50, arg -> {
                if (ppu != null) {
                    defaultListModel.removeAllElements();

                    IntStream.range(0, 64).forEach(i -> {
                        defaultListModel.addElement(ppu.oam[i].toString());
                    });

                    scrollPanel.updateUI();
                    revalidate();
                }
            });
            timer.start();
        }

        public ObjectAttributeMemoryPanel() {
            scrollPanel = new JScrollPane(
                    oamList,
                    JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
            );

            scrollPanel.setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
            scrollPanel.setWheelScrollingEnabled(true);

            setLayout(new BorderLayout());
            add(scrollPanel);
            pack();

            Timer timer = new Timer(10, arg -> {
                if (ppu != null) {
                    defaultListModel.removeAllElements();

                    // TODO 打印ppu.scanLineSprite (ppu.scanLineSprite会快速变化，目前的思路是进行深拷贝)
//                    PPU.OAMEntry[] dataCopy = ppu.scanLineSprite.toArray(new PPU.OAMEntry[0]);
////                    List<PPU.OAMEntry> dataCopy = new ArrayList<>(ppu.scanLineSprite);
//
//                    Arrays.stream(dataCopy).forEach(x -> {
//                        defaultListModel.addElement(x.toString());
//                    });

                    scrollPanel.updateUI();
                    revalidate();
                }
            });
            timer.start();
        }
    }

    private static final int SCREEN_WIDTH = 128;
    private static final int SCREEN_HEIGHT = 128;
    private static final int SCREEN_RATIO = 3;


    private PPU ppu;
    static JTabbedPane tabbed = new JTabbedPane();

    public void addTab(String title, JPanel panel) {
        JPanel temp = new JPanel();
        temp.setLayout(new BorderLayout());
        temp.add(panel);
        tabbed.add(title, temp);
    }

    private void initFrame() {
        setTitle("Object Attribute Memory");
        setBackground(Color.black);
        add(tabbed);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setAutoRequestFocus(false);
        setPreferredSize(new Dimension(SCREEN_WIDTH * SCREEN_RATIO, SCREEN_HEIGHT * SCREEN_RATIO));
        pack();
        setVisible(true);
    }

    public ObjectAttributeMemoryFrame(PPU ppu) {
        initFrame();
        this.ppu = ppu;

        addTab("OAM", new ObjectAttributeMemoryPanel(ppu.oam));
        addTab("ScanLine OAM", new ObjectAttributeMemoryPanel());
    }
}
