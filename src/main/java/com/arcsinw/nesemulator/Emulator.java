package com.arcsinw.nesemulator;

import com.arcsinw.nesemulator.ui.AboutDialog;
import com.arcsinw.nesemulator.ui.NameTableFrame;
import com.arcsinw.nesemulator.ui.PatternTableFrame;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;

public class Emulator extends Frame {
    private static CPU cpu = new CPU();
    private static PPU ppu = new PPU();
    private static CPUBus cpuBus = new CPUBus();

    private static Cartridge cartridge;

    private static final int SCREEN_WIDTH = 256;
    private static final int SCREEN_HEIGHT = 240;
    private static final int SCREEN_RATIO = 3;

    private static final int PATTERN_TABLE_WIDTH = 128;
    private static final int PATTERN_TABLE_HEIGHT = 128;
    private static final int PATTERN_TABLE_RATIO = 6;

    private void addMenuBar() {
        MenuBar menuBar = new MenuBar();

        {
            Menu fileMenu = new Menu("文件");
            fileMenu.add(new MenuItem("打开") {

            });
            menuBar.add(fileMenu);
        }

        {
            Menu debugMenu = new Menu("调试");
            {
                MenuItem ptMenuItem = new MenuItem("Pattern Table");
                ptMenuItem.addActionListener(e -> showPatternTableFrame());
                debugMenu.add(ptMenuItem);
            }
            {
                MenuItem ntMenuItem = new MenuItem("Name Table");
                ntMenuItem.addActionListener(e -> showNameTableFrame());
                debugMenu.add(ntMenuItem);
            }
            menuBar.add(debugMenu);
        }

        {
            Menu helpMenu = new Menu("帮助");
            {
                MenuItem aboutMenuItem = new MenuItem("关于");

                aboutMenuItem.addActionListener(e -> {
                    AboutDialog aboutDialog = new AboutDialog(this);
                    aboutDialog.setVisible(true);
                });
                helpMenu.add(aboutMenuItem);
            }

            menuBar.add(helpMenu);
        }

        setMenuBar(menuBar);
    }

    private Panel panel = new Panel() {
        @Override
        public void paint(Graphics g) {
            displayPatternTable();
        }
    };

    public Emulator() {
        setTitle("NesEmulator");
//        setSize(SCREEN_WIDTH * SCREEN_RATIO, SCREEN_HEIGHT * SCREEN_RATIO);
        setBackground(Color.black);
        setLayout(new FlowLayout());
        addMenuBar();

        panel.setPreferredSize(new Dimension(SCREEN_WIDTH * SCREEN_RATIO, SCREEN_HEIGHT * SCREEN_RATIO));
        add(panel);
        pack();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                dispose();
            }
        });
        setVisible(true);
    }

    public static void main(String[] args) throws IOException {
        Emulator emulator = new Emulator();
        emulator.setVisible(true);

        String romPath = "/nestest.nes";
//        String romPath = "/Donkey Kong.nes";
//        String romPath = "/896.nes";
        cartridge = new Cartridge(romPath);
        System.out.println(cartridge.header.toString());

        cpu.setBus(cpuBus);
        cpuBus.setPpu(ppu);
        cpuBus.setCartridge(cartridge);

        int line = 0;
        cpu.reset();

        while (line++ < 400000) {
            cpu.clock();
        }

        emulator.displayPatternTable();
    }

    public static String get2DArrayPrint(byte[] matrix) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < (matrix.length / 16); i++) {
            sb.append(String.format("%04X : ", 0x2000 + i * 16));
            for (int j = 0; j < 16; j++) {
                sb.append(String.format("%02X  ", matrix[i* 16 + j]));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public void showPatternTableFrame() {
        if (cartridge != null) {
            PatternTableFrame patternTableFrame = new PatternTableFrame(ppu);
            patternTableFrame.displayPatternTable();
        }
    }

    public void showNameTableFrame() {
        if (cartridge != null) {
            NameTableFrame patternTableFrame = new NameTableFrame(ppu);
            patternTableFrame.displayNameTable();
        }
    }

    private byte[] palette = new byte[]{
            0x22, 0x29, 0x1A, 0x0F, 0x0F, 0x36, 0x17, 0x0F, 0x0F, 0x30, 0x21, 0x0F, 0x0F, 0x17, 0x17, 0x0F, // Image Palette
            0x22, 0x16, 0x27, 0x18, 0x0F, 0x1A, 0x30, 0x27, 0x0F, 0x16, 0x30, 0x27, 0x0F, 0x0F, 0x36, 0x17  // Sprite Palette
    };

    public void displayPatternTable() {
        BufferedImage image = new BufferedImage(16*8, 16*8, BufferedImage.TYPE_3BYTE_BGR);

        byte[][] table = ppu.getPatternTable();

        /**
         * 2 Background 和 Sprite
         * 256 每个Pattern Table有 256个 Tile
         * 64 每个 Tile 8x8 共 64个像素点
         * 每个像素点的颜色 使用 4bit来表示（实际上是索引了Palettes）
         * 每个Tile使用了Pattern table的 16字节
         */
        byte[][][] imageColor = new byte[2][256][64];

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 256; j++) {
                // 读 16 字节
                byte[] tileData = Arrays.copyOfRange(table[i], 16 * j, 16 * (j + 1));

                // 从Attribute table读
                for (int k = 0; k < 64; k++) {

                    // bit 0
                    byte lo = (byte)((tileData[k / 8] >> (7 - k % 8)) & 0x01);

                    // bit 1
                    byte hi = (byte)(((tileData[k / 8 + 8] >> (7 - k % 8)) & 0x01) << 1);

                    byte color = (byte)(lo | hi);

                    imageColor[i][j][k] = color;
                }
            }
        }


        byte[][] background = imageColor[0];
        byte[][] sprite = imageColor[1];

        int startRow = 0, startCol = 0;

        for (int k = 0; k < 256; k++) {
            for (int i = 0; i < 64; i++) { // 每个图案64个像素点
                // 这里只有颜色的后两位
                switch (background[k][i]) {
                    case 1:
                        image.setRGB(startCol + i % 8, startRow + i / 8, Color.cyan.getRGB());
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
                0, 0,
                PATTERN_TABLE_WIDTH * PATTERN_TABLE_RATIO,
                PATTERN_TABLE_HEIGHT * PATTERN_TABLE_RATIO,
                this.panel);
    }
}
