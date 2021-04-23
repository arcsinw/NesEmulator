package com.arcsinw.nesemulator.ui;

import javax.swing.*;
import java.awt.*;

public class MemoryViewerFrame extends JPanel {

    private static final int FONT_SIZE = 16;

    byte[] mem;
    int offset = 0;
    int lines;

    static JFrame frame;
    static JTabbedPane tabbed;
    JScrollBar scroll;


    static void addViewer(String title, MemoryViewerFrame mv, JScrollBar bar){
        if(frame == null){
            frame = new JFrame("MemoryViewer");
            frame.add(tabbed = new JTabbedPane());
            frame.setBounds(600, 0, 450, 400);
            frame.setVisible(true);
        }
        JPanel temp = new JPanel();
        temp.setLayout(new BorderLayout());
        temp.add(mv);
        temp.add(bar, BorderLayout.EAST);
        tabbed.add(title, temp);
    }


    public MemoryViewerFrame(String title, byte[] mem) {
        this.mem = mem;
        lines = mem.length/16;
        scroll = new JScrollBar(JScrollBar.VERTICAL, 0, Math.min(16, lines), 0, lines);
        scroll.addAdjustmentListener(e -> {
            offset = e.getValue();
            repaint();
        });
        addViewer(title, this, scroll);
    }

    Font font = new Font(Font.MONOSPACED, Font.PLAIN, 14);

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        g.setColor(Color.black);
        g.setFont(font);

        for (int i = 0; i < lines; i++) {
            StringBuffer sb = new StringBuffer();
            int addr = (offset + i) << 4;
            if (addr < this.mem.length) {
                sb.append(String.format("%04X|  ", addr));

                for (int j = 0; j < 16 && addr + j < mem.length; j++) {
                    sb.append(String.format("%02X", mem[addr + j]));
                    sb.append(j % 4 == 3 ? " | " : " ");
                }
                if (((offset + i) & 0xF) == 0) {
                    g.drawLine(0, FONT_SIZE * i + 4, this.getWidth(), FONT_SIZE * i + 4);
                }
                g.drawString(sb.toString(), 0, FONT_SIZE * i + FONT_SIZE);
            }
        }
    }
}