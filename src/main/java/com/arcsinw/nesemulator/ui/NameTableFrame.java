package com.arcsinw.nesemulator.ui;

import com.arcsinw.nesemulator.ColorPalette;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

public class NameTableFrame extends Frame {
    private static final int SCREEN_WIDTH = 256;
    private static final int SCREEN_HEIGHT = 128;
    private static final int SCREEN_RATIO = 3;

    private static final int PATTERN_TABLE_WIDTH = 128;
    private static final int PATTERN_TABLE_HEIGHT = 128;
    private static final int PATTERN_TABLE_RATIO = 3;

    private byte[][][] table;

    private Panel panel = new Panel() {
        @Override
        public void paint(Graphics g) {
            displayPatternTable();
        }
    };

    public NameTableFrame(byte[][][] table) {
        this.table = table;

        setTitle("Pattern Table");
        setBackground(Color.black);

        panel.setPreferredSize(new Dimension(SCREEN_WIDTH * SCREEN_RATIO, SCREEN_HEIGHT * SCREEN_RATIO));
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

    public void displayPatternTable() {
        BufferedImage image = new BufferedImage(16 * 8, 16 * 8, BufferedImage.TYPE_3BYTE_BGR);

        for (int p = 0; p < 2; p++) {
            int startRow = 0, startCol = 0;
            for (int k = 0; k < 256; k++) {
                for (int i = 0; i < 64; i++) {
                    switch (table[p][k][i]) {
                        case 0:
                            image.setRGB(startCol + i % 8, startRow + i / 8, fromRGB(ColorPalette.COLOR_PALETTE[0x22]).getRGB());
                            break;
                        case 1:
                            image.setRGB(startCol + i % 8, startRow + i / 8, Color.white.getRGB());
                            break;
                        case 2:
                            image.setRGB(startCol + i % 8, startRow + i / 8, Color.orange.getRGB());
                            break;
                        case 3:
                            image.setRGB(startCol + i % 8, startRow + i / 8, Color.green.getRGB());
                            break;
                    }
                }

                if (k != 0 && ((k & 0x0F) == 0x0F)) {
                    startCol = 0;
                    startRow += 8;
                } else {
                    startCol += 8;
                }
            }

            Graphics graphics = this.panel.getGraphics();
            graphics.drawImage(image,
                    PATTERN_TABLE_WIDTH * PATTERN_TABLE_RATIO * p, 0,
                    PATTERN_TABLE_WIDTH * PATTERN_TABLE_RATIO,
                    PATTERN_TABLE_HEIGHT * PATTERN_TABLE_RATIO,
                    this.panel);
        }
    }
}