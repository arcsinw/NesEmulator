package com.arcsinw.nesemulator;

import com.arcsinw.nesemulator.ui.*;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class Emulator extends Frame {
    private static CPU cpu = new CPU();
    private static PPU ppu = new PPU();
    private static NesRom nesRom;

    private static final int SCREEN_WIDTH = 256;
    private static final int SCREEN_HEIGHT = 240;
    private static final int SCREEN_RATIO = 3;

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

    public Emulator() {
        setTitle("NesEmulator");
        setSize(SCREEN_WIDTH * SCREEN_RATIO, SCREEN_HEIGHT * SCREEN_RATIO);
        setBackground(Color.black);
        setLayout(new FlowLayout());

        addMenuBar();

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

        InputStream inputStream = Emulator.class.getResourceAsStream("/896.nes");
        URL url = Emulator.class.getResource("/");
        System.out.println(url.getPath());
        nesRom = new NesRom(inputStream);
        System.out.println(nesRom.header.toString());

        cpu.diasm(nesRom.prg);

        emulator.displayPatternTable(ppu.getPatternTable(nesRom.chr));
    }

    public void showPatternTableFrame() {
        if (nesRom != null) {
            PatternTableFrame patternTableFrame = new PatternTableFrame(ppu.getPatternTable(nesRom.chr));
            patternTableFrame.displayPatternTable();
        }
    }


    public void displayPatternTable(byte[][][] table) {
        BufferedImage bgImage = new BufferedImage(256*8,
                256*8, BufferedImage.TYPE_3BYTE_BGR);

        byte[][] background = table[0];
        byte[][] sprite = table[1];

        int startRow = 0, startCol = 0;

        for (int k = 0; k < 256; k++) {
            for (int i = 0; i < 64; i++) {
                switch (background[k][i]) {
                    case 1:
                        bgImage.setRGB(startCol + i % 8, startRow + i / 8, Color.cyan.getRGB());
                        break;
                    case 2:
                        bgImage.setRGB(startCol + i % 8, startRow + i / 8, Color.orange.getRGB());
                        break;
                    case 3:
                        bgImage.setRGB(startCol + i % 8, startRow + i / 8, Color.green.getRGB());
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

        Graphics graphics = this.getGraphics();
        int left = this.getInsets().left;
        int right = this.getInsets().right;

        graphics.drawImage(bgImage, 150, 150, 256*16*2, 256*16*2,this);
    }
}
