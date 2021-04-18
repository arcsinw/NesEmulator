package com.arcsinw.nesemulator.ui;

import com.arcsinw.nesemulator.ColorPalette;
import com.arcsinw.nesemulator.PPU;

import javax.swing.*;
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


        Timer timer = new Timer(1000 / 30, arg -> displayNameTable());
        timer.start();

        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                timer.stop();
                dispose();
            }
        });

        setVisible(true);

    }

    private Color fromRGB(int[] c) {
        return new Color(c[0], c[1], c[2]);
    }

    private Color fromIndex(byte index) {
        return fromRGB(ColorPalette.COLOR_PALETTE[index]);
    }

    public void displayNameTable() {
        BufferedImage image = new BufferedImage(32 * 8, 30 * 8, BufferedImage.TYPE_3BYTE_BGR);
        /**
         * 2KB [2][1024]
         * 每个 8x8的 Tile 使用1字节来索引，共 32*30=960个Tile 使用960字节
         * 剩下的64字节是Attribute Table 每个字节控制16个Tile的颜色，每4个田字格Tile 共用 2bit 作为颜色的前两位
         */
        byte[][] nameTable = ppu.getNameTable();


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
        byte[][] nameTableColorMap = new byte[4][960 * 16];

        // 填充 nameTableColorMap
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 960; j++) {
                int s = (ppu.ppuRead(0x2000 + 0x0400 * i + j) & 0x00FF) * 16;
//                int s = (nameTable[i][j] & 0x00FF) * 16;
                int backgroundAddress = ppu.getPpuCtrl(PPU.PPUCtrl.BackgroundSelect);
                System.arraycopy(patternTable[backgroundAddress], s, nameTableColorMap[i], j * 16, 16);
            }
        }

        /**
         * 2 个 Name table
         * 960 每个Name Table有 960个 tile
         * 64 每个 Tile 8x8 共 64个像素点
         * 每个像素点的颜色 使用 4bit来表示（实际上是索引了Palettes）
         * 每个Tile使用了Pattern table的 16字节
         */
        byte[][][] imageColor = new byte[4][960][64];

        for (int i = 0; i < 4; i++) {
            // 读最后64字节作为Attribute Table，1字节控制16个tile
//            byte[] attributeTable = Arrays.copyOfRange(nameTable[i], 960, 1024);

            for (int j = 0; j < 960; j++) { // tile
                // 读 16 字节
                byte[] tileData = Arrays.copyOfRange(nameTableColorMap[i], 16 * j, 16 * (j + 1));

                int row = j >> 5;
                int col = j % 32;

                // 4tile x 4tile 的大Tile id
                int bigTileId = ((row >> 2) << 3) + (col >> 2);

                byte colorByte = ppu.ppuRead(0x2000 + 0x0400 * i + 960 + bigTileId);
//                byte colorByte =  attributeTable[bigTileId];

                int bigTileLeftTopRow = (bigTileId >> 3) << 2;
                int bigTileLeftTopCol = (bigTileId & 0x07) << 2;

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

        for (int p = 0; p < 4; p++) {
            for (int k = 0; k < 960; k++) { // tile
                int startRow = (k / 32) * 8;
                int startCol = (k % 32) * 8;
                for (int i = 0; i < 64; i++) { // 像素
                    byte color = ppu.ppuRead(0x3F00 + imageColor[p][k][i]);
                    image.setRGB(startCol + i % 8, startRow + i / 8, fromIndex(color).getRGB());
                }
            }

            int x = (p % 2) * IMAGE_WIDTH * IMAGE_RATIO;
            int y = (p / 2) * IMAGE_HEIGHT * IMAGE_RATIO;

            Graphics graphics = this.panel.getGraphics();
            graphics.drawImage(image,
                    x, y,
                    IMAGE_WIDTH * IMAGE_RATIO,
                    IMAGE_HEIGHT * IMAGE_RATIO,
                    this.panel);
        }
    }
}