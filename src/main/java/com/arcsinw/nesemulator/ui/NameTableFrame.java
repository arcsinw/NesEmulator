package com.arcsinw.nesemulator.ui;

import com.arcsinw.nesemulator.ColorPalette;
import com.arcsinw.nesemulator.PPU;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.Arrays;

public class NameTableFrame extends Frame {
    private static final int PANEL_WIDTH = 256 * 2;
    private static final int PANEL_HEIGHT = 240 * 2;
    private static final int PANEL_RATIO = 1;

    private static final int IMAGE_WIDTH = 256;
    private static final int IMAGE_HEIGHT = 240;
    private static final int IMAGE_RATIO = 1;

    private PPU ppu;

    private Panel panel = new Panel() {
        @Override
        public void paint(Graphics g) {
            displayNameTable();
        }
    };

    public NameTableFrame(PPU ppu) {
        this.ppu = ppu;

        setTitle("Name Table");
        setBackground(Color.black);

        panel.setPreferredSize(new Dimension(PANEL_WIDTH * PANEL_RATIO, PANEL_HEIGHT * PANEL_RATIO));
        add(panel);
        pack();

        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                dispose();
            }
        });

        setVisible(true);

    }

    private Color fromRGB(int[] c) {
        return new Color(c[0], c[1], c[2]);
    }

    private Color fromIndex(byte index) {
//        palette = new byte[] {
//                0x22, 0x29, 0x1A, 0x0F, 0x0F, 0x36, 0x17, 0x0F, 0x0F, 0x30, 0x21, 0x0F, 0x0F, 0x17, 0x17, 0x0F,
//                0x22, 0x16, 0x27, 0x18, 0x0F, 0x1A, 0x30, 0x27, 0x0F, 0x16, 0x30, 0x27, 0x0F, 0x0F, 0x36, 0x17
//        };
        return fromRGB(ColorPalette.COLOR_PALETTE[index]);
    }

    public void displayNameTable() {
        BufferedImage image = new BufferedImage(32 * 8, 30 * 8, BufferedImage.TYPE_3BYTE_BGR);

        /**
         * 调色板 共32种颜色
         */
        byte[] palette = ppu.getPalette();

        /**
         * 2KB [2][1024]
         * 每个 8x8的 Tile 使用1字节来索引，共 32*30=960个Tile 使用960字节
         * 剩下的64字节是Attribute Table 每个字节控制16个Tile的颜色，每4个田字格Tile 共用 2bit 作为颜色的前两位
         */
//        byte[][] nameTable = ppu.getNameTable();

//        int[] sampleData = new int[] {0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x02, 0x24, 0x19, 0x15, 0x0A, 0x22, 0x0E, 0x1B, 0x24, 0x10, 0x0A, 0x16, 0x0E, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x31, 0x32, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x26, 0x34, 0x33, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x26, 0x26, 0x26, 0x33, 0x24, 0x24, 0x24, 0x24, 0x1D, 0x18, 0x19, 0x28, 0x24, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x26, 0x26, 0x34, 0x26, 0x33, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x36, 0x37, 0x36, 0x37, 0x36, 0x37, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x26, 0x26, 0x26, 0x26, 0x26, 0x33, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x35, 0x25, 0x25, 0x25, 0x25, 0x25, 0x25, 0x38, 0x24, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xAA, 0xAA, 0xEA, 0xAA, 0xAA, 0xAA, 0xAA, 0xAA, 0x00, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x00, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x00, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x00, 0x00, 0x00, 0x99, 0xAA, 0xAA, 0xAA, 0x00, 0x00, 0x00, 0x00, 0x99, 0xAA, 0xAA, 0xAA, 0x00, 0x00, 0x50, 0x50, 0x50, 0x50, 0x50, 0x50, 0x50, 0x50, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x16, 0x0A, 0x1B, 0x12, 0x18, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x20, 0x18, 0x1B, 0x15, 0x0D, 0x24, 0x24, 0x1D, 0x12, 0x16, 0x0E, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x24, 0x24, 0x2E, 0x29, 0x00, 0x00, 0x24, 0x24, 0x24, 0x24, 0x01, 0x28, 0x01, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x44, 0x48, 0x48, 0x48, 0x48, 0x48, 0x48, 0x48, 0x48, 0x48, 0x48, 0x48, 0x48, 0x48, 0x48, 0x48, 0x48, 0x48, 0x48, 0x48, 0x48, 0x49, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x46, 0xD0, 0xD1, 0xD8, 0xD8, 0xDE, 0xD1, 0xD0, 0xDA, 0xDE, 0xD1, 0x26, 0x26, 0x26, 0x26, 0x26, 0x26, 0x26, 0x26, 0x26, 0x26, 0x4A, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x46, 0xD2, 0xD3, 0xDB, 0xDB, 0xDB, 0xD9, 0xDB, 0xDC, 0xDB, 0xDF, 0x26, 0x26, 0x26, 0x26, 0x26, 0x26, 0x26, 0x26, 0x26, 0x26, 0x4A, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x46, 0xD4, 0xD5, 0xD4, 0xD9, 0xDB, 0xE2, 0xD4, 0xDA, 0xDB, 0xE0, 0x26, 0x26, 0x26, 0x26, 0x26, 0x26, 0x26, 0x26, 0x26, 0x26, 0x4A, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x46, 0xD6, 0xD7, 0xD6, 0xD7, 0xE1, 0x26, 0xD6, 0xDD, 0xE1, 0xE1, 0x26, 0x26, 0x26, 0x26, 0x26, 0x26, 0x26, 0x26, 0x26, 0x26, 0x4A, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x46, 0xD0, 0xE8, 0xD1, 0xD0, 0xD1, 0xDE, 0xD1, 0xD8, 0xD0, 0xD1, 0x26, 0xDE, 0xD1, 0xDE, 0xD1, 0xD0, 0xD1, 0xD0, 0xD1, 0x26, 0x4A, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x46, 0xDB, 0x42, 0x42, 0xDB, 0x42, 0xDB, 0x42, 0xDB, 0xDB, 0x42, 0x26, 0xDB, 0x42, 0xDB, 0x42, 0xDB, 0x42, 0xDB, 0x42, 0x26, 0x4A, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x46, 0xDB, 0xDB, 0xDB, 0xDB, 0xDB, 0xDB, 0xDF, 0xDB, 0xDB, 0xDB, 0x26, 0xDB, 0xDF, 0xDB, 0xDF, 0xDB, 0xDB, 0xE4, 0xE5, 0x26, 0x4A, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x46, 0xDB, 0xDB, 0xDB, 0xDE, 0x43, 0xDB, 0xE0, 0xDB, 0xDB, 0xDB, 0x26, 0xDB, 0xE3, 0xDB, 0xE0, 0xDB, 0xDB, 0xE6, 0xE3, 0x26, 0x4A, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x46, 0xDB, 0xDB, 0xDB, 0xDB, 0x42, 0xDB, 0xDB, 0xDB, 0xD4, 0xD9, 0x26, 0xDB, 0xD9, 0xDB, 0xDB, 0xD4, 0xD9, 0xD4, 0xD9, 0xE7, 0x4A, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x5F, 0x95, 0x95, 0x95, 0x95, 0x95, 0x95, 0x95, 0x95, 0x97, 0x98, 0x78, 0x95, 0x96, 0x95, 0x95, 0x97, 0x98, 0x97, 0x98, 0x95, 0x7A, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0xCF, 0x01, 0x09, 0x08, 0x05, 0x24, 0x17, 0x12, 0x17, 0x1D, 0x0E, 0x17, 0x0D, 0x18, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0xCE, 0x24, 0x01, 0x24, 0x19, 0x15, 0x0A, 0x22, 0x0E, 0x1B, 0x24, 0x10, 0x0A, 0x16, 0x0E, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24};
//        int[] sampleData1 = new int[] {0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x36, 0x37, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x35, 0x25, 0x25, 0x38, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x39, 0x3A, 0x3B, 0x3C, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x36, 0x37, 0x36, 0x37, 0x36, 0x37, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x35, 0x25, 0x25, 0x25, 0x25, 0x25, 0x25, 0x38, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x39, 0x3A, 0x3B, 0x3A, 0x3B, 0x3A, 0x3B, 0x3C, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x53, 0x54, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x55, 0x56, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x53, 0x54, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x45, 0x45, 0x53, 0x54, 0x45, 0x45, 0x53, 0x54, 0x45, 0x45, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x55, 0x56, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x47, 0x47, 0x55, 0x56, 0x47, 0x47, 0x55, 0x56, 0x47, 0x47, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x60, 0x61, 0x62, 0x63, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x31, 0x32, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x64, 0x65, 0x66, 0x67, 0x24, 0x24, 0x24, 0x24, 0x24, 0x30, 0x26, 0x34, 0x33, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x36, 0x37, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x68, 0x69, 0x26, 0x6A, 0x24, 0x24, 0x24, 0x24, 0x30, 0x26, 0x26, 0x26, 0x26, 0x33, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x35, 0x25, 0x25, 0x38, 0x24, 0x24, 0x24, 0x24, 0x24, 0x68, 0x69, 0x26, 0x6A, 0x24, 0x24, 0x24, 0x24, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB4, 0xB5, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0xB6, 0xB7, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x88, 0xAA, 0x00, 0x00, 0x80, 0xA0, 0xA0, 0x00, 0x00, 0x00, 0x30, 0x00, 0x08, 0x0A, 0x0A, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x30, 0x00, 0xD0, 0xD0, 0x10, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x50, 0x50, 0x50, 0x50, 0x50, 0x50, 0x50, 0x50, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05};
//        for (int i = 0; i < sampleData.length; i++) {
//            nameTable[0][i] = (byte) sampleData[i];
//            nameTable[1][i] = (byte) sampleData1[i];
//        }

        /**
         * 共8K
         * [2][256 * 16]
         * 每个图案占 16 字节（前8字节 bit 0，后8字节 bit 1）
         */
        byte[][] patternTable = ppu.getPatternTable();

        /**
         * [2][960 * 16]
         * 960 个 tile，每个tile 占 16 字节
         */
        byte[][] nameTableColorMap = new byte[2][960 * 16];

        // 填充 nameTableColorMap
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 960; j++) {
                int s = (ppu.ppuRead(0x2000 + 0x0400 * i + j) & 0x00FF) * 16;
//                int s = (nameTable[i][j] & 0x00FF) * 16;
                int backgroundAddress = ppu.getPpuCtrl(PPU.PPUCtrl.BackgroundSelect);
                System.arraycopy(patternTable[backgroundAddress], s, nameTableColorMap[i], j * 16, 16);
            }
        }

        /**
         * 2 Background 和 Sprite
         * 960 每个Name Table有 960个 tile
         * 64 每个 Tile 8x8 共 64个像素点
         * 每个像素点的颜色 使用 4bit来表示（实际上是索引了Palettes）
         * 每个Tile使用了Pattern table的 16字节
         */
        byte[][][] imageColor = new byte[2][960][64];

        for (int i = 0; i < 2; i++) {
            // 读最后64字节作为Attribute Table，1字节控制16个tile
//            byte[] attributeTable = Arrays.copyOfRange(nameTable[i], 960, 1024);

            for (int j = 0; j < 960; j++) { // tile
                // 读 16 字节
                byte[] tileData = Arrays.copyOfRange(nameTableColorMap[i], 16 * j, 16 * (j + 1));

                int row = j / 32;
                int col = j % 30;

                // 4tile x 4tile 的大Tile id
                int bigTileId = (row / 4) * 8 + col / 4;

                byte colorByte = ppu.ppuRead(0x2000 + 0x0400 * i + 960 + bigTileId);
//                byte colorByte =  attributeTable[bigTileId];

                int bigTileLeftTopRow = (bigTileId / 8) * 4;
                int bigTileLeftTopCol = (bigTileId % 8) * 4;

                // 小tile的offset id (0, 1, 2, 3)
                int tileOffset = ((row - bigTileLeftTopRow) / 2) * 2 + (col - bigTileLeftTopCol) / 2;

                for (int k = 0; k < 64; k++) {  // 像素点
                    // bit 0
                    byte lo = (byte)((tileData[k / 8] >> (7 - k % 8)) & 0x01);

                    // bit 1
                    byte hi = (byte)(((tileData[k / 8 + 8] >> (7 - k % 8)) & 0x01) << 1);

                    byte color = (byte)(lo | hi | (((colorByte >>> ((tileOffset) * 2) & 0x03) << 2)));

                    imageColor[i][j][k] = color;
                }
            }
        }

        for (int p = 0; p < 2; p++) {
            for (int k = 0; k < 960; k++) { // tile
                int startRow = (k / 32) * 8;
                int startCol = (k % 32) * 8;
                for (int i = 0; i < 64; i++) { // 像素
                    // 256x240的图片
//                    System.out.println(String.format("% 3d % 3d % 3d", k, startCol + i % 8, startRow + i / 8));
                    image.setRGB(startCol + i % 8, startRow + i / 8, fromIndex(palette[imageColor[p][k][i]]).getRGB());
                }
            }

            Graphics graphics = this.panel.getGraphics();
            graphics.drawImage(image,
                    IMAGE_WIDTH * IMAGE_RATIO * p, 0,
                    IMAGE_WIDTH * IMAGE_RATIO,
                    IMAGE_HEIGHT * IMAGE_RATIO,
                    this.panel);
        }
    }
}