package com.arcsinw.nesemulator.ui;

import com.arcsinw.nesemulator.PPU;

import javax.swing.*;
import java.awt.*;

public class MemoryViewerFrame extends JPanel {

    private static final int FONT_SIZE = 16;

    private byte[] memory;
    private byte[][] memory2D;
    private PPU.OAMEntry[] oam;
    private int offset = 0;
    private int lines;

    static JFrame frame;
    static JTabbedPane tabbed;
    JScrollBar scroll;

    Font font = new Font(Font.MONOSPACED, Font.PLAIN, 12);

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


    public MemoryViewerFrame(String title, byte[] memory) {
        this.memory = memory;
        lines = memory.length/16;
        scroll = new JScrollBar(JScrollBar.VERTICAL, 0, Math.min(16, lines), 0, lines);
        scroll.addAdjustmentListener(e -> {
            offset = e.getValue();
            repaint();
        });
        addViewer(title, this, scroll);

        refresh();
    }

    public MemoryViewerFrame(String title, PPU.OAMEntry[] oam) {
        this.oam = oam;
        byte[] data = new byte[256];
        for (int i = 0; i < oam.length; i++) {
            data[i] = (byte) oam[i].y;
            data[i + 1] = (byte) oam[i].id;
            data[i + 2] = oam[i].attribute;
            data[i + 3] = (byte) oam[i].x;
        }

        this.memory = data;
        lines = memory.length/16;
        scroll = new JScrollBar(JScrollBar.VERTICAL, 0, Math.min(16, lines), 0, lines);
        scroll.addAdjustmentListener(e -> {
            offset = e.getValue();
            repaint();
        });
        addViewer(title, this, scroll);

        refresh();
    }

    public MemoryViewerFrame(String title, byte[][] memory) {
        this.memory2D = memory;
        lines = (memory2D.length * memory2D[0].length) / 16;
        scroll = new JScrollBar(JScrollBar.VERTICAL, 0, Math.min(16, lines), 0, lines);
        scroll.addAdjustmentListener(e -> {
            offset = e.getValue();
            repaint();
        });
        addViewer(title, this, scroll);
    }

    private void refresh() {
        Timer timer = new Timer(100, args -> {
            if (this.oam != null) {
                for (int i = 0; i < oam.length; i++) {
                    this.memory[i] = (byte) oam[i].y;
                    this.memory[i + 1] = (byte) oam[i].id;
                    this.memory[i + 2] = oam[i].attribute;
                    this.memory[i + 3] = (byte) oam[i].x;
                }
            }
            this.repaint();
        });
        timer.start();
    }


    @Override
    public void paint(Graphics g) {
        super.paint(g);
        g.setColor(Color.black);
        g.setFont(font);

        for (int i = 0; i < lines; i++) {
            StringBuffer sb = new StringBuffer();
            int addr = (offset + i) << 4;
            if (addr < this.memory.length) {
                sb.append(String.format("%04X|  ", addr));

                for (int j = 0; j < 16 && addr + j < memory.length; j++) {
                    sb.append(String.format("%02X", memory[addr + j]));
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