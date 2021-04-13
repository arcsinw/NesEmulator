package com.arcsinw.nesemulator.ui;


import com.arcsinw.nesemulator.ColorPalette;
import com.arcsinw.nesemulator.PPU;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;


public class PatternTableFrame extends Frame {
    private static final int SCREEN_WIDTH = 256;
    private static final int SCREEN_HEIGHT = 128;
    private static final int SCREEN_RATIO = 3;

    private static final int PATTERN_TABLE_WIDTH = 128;
    private static final int PATTERN_TABLE_HEIGHT = 128;
    private static final int PATTERN_TABLE_RATIO = 3;

    private PPU ppu;

    private Panel patternTablePanel = new Panel() {
        @Override
        public void paint(Graphics g) {
            displayPatternTable();
        }
    };

    public PatternTableFrame(PPU ppu) {
        this.ppu = ppu;

        setTitle("Pattern Table");
        setBackground(Color.black);

        patternTablePanel.setPreferredSize(new Dimension(SCREEN_WIDTH * SCREEN_RATIO, SCREEN_HEIGHT * SCREEN_RATIO));
        add(patternTablePanel);
        pack();

        this.addWindowListener(new WindowAdapter(){
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

    /**
     *
     * @return [2][256][64]
     */
    public byte[][][] getColorMap() {
        /**
         * [2][4096]
         */
        byte[][] patternTable = ppu.getPatternTable();
        byte[][][] result = new byte[2][256][64];

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 256; j++) { // 每个tile使用Pattern Table的16字节

                for (int k = 0; k < 64; k++) { // 像素
                    byte low0 = patternTable[i][j * 16 + k / 8];
                    byte low1 = patternTable[i][j * 16 + k / 8 + 8];

                    int offset = k % 8;

                    byte l = (byte)((low0 >> (7 - offset)) & 1);
                    byte h = (byte)(((low1 >> (7 - offset)) & 1) << 1);
                    result[i][j][k] = (byte) (l | h);
                }
            }
        }

        return result;
    }

    public void displayPatternTable() {
        BufferedImage image = new BufferedImage(16*8, 16*8, BufferedImage.TYPE_3BYTE_BGR);

        byte[][][] colorMap = getColorMap();
        byte[] palette = ppu.getPalette();

        palette = new byte[] {
            0x22, 0x29, 0x1A, 0x0F, 0x0F, 0x36, 0x17, 0x0F, 0x0F, 0x30, 0x21, 0x0F, 0x0F, 0x17, 0x17, 0x0F,
            0x22, 0x16, 0x27, 0x18, 0x0F, 0x1A, 0x30, 0x27, 0x0F, 0x16, 0x30, 0x27, 0x0F, 0x0F, 0x36, 0x17
        };

        for (int p = 0; p < 2; p++) {
            int startRow = 0, startCol = 0;
            for (int k = 0; k < 256; k++) {
                for (int i = 0; i < 64; i++) {
                    image.setRGB(startCol + i % 8, startRow + i / 8, fromRGB(ColorPalette.COLOR_PALETTE[palette[colorMap[p][k][i]]]).getRGB());
                }

                if (k != 0 && ((k & 0x0F) == 0x0F)) {
                    startCol = 0;
                    startRow += 8;
                } else {
                    startCol += 8;
                }
            }

            Graphics graphics = this.patternTablePanel.getGraphics();
            graphics.drawImage(image,
                     PATTERN_TABLE_WIDTH * PATTERN_TABLE_RATIO * p, 0,
                    PATTERN_TABLE_WIDTH * PATTERN_TABLE_RATIO,
                    PATTERN_TABLE_HEIGHT * PATTERN_TABLE_RATIO,
                    this.patternTablePanel);
        }
    }
}
