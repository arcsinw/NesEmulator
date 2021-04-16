package com.arcsinw.nesemulator.ui;

import com.arcsinw.nesemulator.CPU;
import com.arcsinw.nesemulator.CPUBus;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * 一个6502小游戏（可简单用于测试CPU是否正常运行）
 * https://skilldrick.github.io/easy6502/#snake
 * https://github.com/bugzmanov/nes_ebook/blob/master/src/chapter_3_4.md
 */
public class SnakeTestFrame extends Frame {
    private static final int[] GAME_CODE = new int[] {
            0x20, 0x06, 0x06, 0x20, 0x38, 0x06, 0x20, 0x0d, 0x06, 0x20, 0x2a, 0x06, 0x60, 0xa9, 0x02, 0x85,
            0x02, 0xa9, 0x04, 0x85, 0x03, 0xa9, 0x11, 0x85, 0x10, 0xa9, 0x10, 0x85, 0x12, 0xa9, 0x0f, 0x85,
            0x14, 0xa9, 0x04, 0x85, 0x11, 0x85, 0x13, 0x85, 0x15, 0x60, 0xa5, 0xfe, 0x85, 0x00, 0xa5, 0xfe,
            0x29, 0x03, 0x18, 0x69, 0x02, 0x85, 0x01, 0x60, 0x20, 0x4d, 0x06, 0x20, 0x8d, 0x06, 0x20, 0xc3,
            0x06, 0x20, 0x19, 0x07, 0x20, 0x20, 0x07, 0x20, 0x2d, 0x07, 0x4c, 0x38, 0x06, 0xa5, 0xff, 0xc9,
            0x77, 0xf0, 0x0d, 0xc9, 0x64, 0xf0, 0x14, 0xc9, 0x73, 0xf0, 0x1b, 0xc9, 0x61, 0xf0, 0x22, 0x60,
            0xa9, 0x04, 0x24, 0x02, 0xd0, 0x26, 0xa9, 0x01, 0x85, 0x02, 0x60, 0xa9, 0x08, 0x24, 0x02, 0xd0,
            0x1b, 0xa9, 0x02, 0x85, 0x02, 0x60, 0xa9, 0x01, 0x24, 0x02, 0xd0, 0x10, 0xa9, 0x04, 0x85, 0x02,
            0x60, 0xa9, 0x02, 0x24, 0x02, 0xd0, 0x05, 0xa9, 0x08, 0x85, 0x02, 0x60, 0x60, 0x20, 0x94, 0x06,
            0x20, 0xa8, 0x06, 0x60, 0xa5, 0x00, 0xc5, 0x10, 0xd0, 0x0d, 0xa5, 0x01, 0xc5, 0x11, 0xd0, 0x07,
            0xe6, 0x03, 0xe6, 0x03, 0x20, 0x2a, 0x06, 0x60, 0xa2, 0x02, 0xb5, 0x10, 0xc5, 0x10, 0xd0, 0x06,
            0xb5, 0x11, 0xc5, 0x11, 0xf0, 0x09, 0xe8, 0xe8, 0xe4, 0x03, 0xf0, 0x06, 0x4c, 0xaa, 0x06, 0x4c,
            0x35, 0x07, 0x60, 0xa6, 0x03, 0xca, 0x8a, 0xb5, 0x10, 0x95, 0x12, 0xca, 0x10, 0xf9, 0xa5, 0x02,
            0x4a, 0xb0, 0x09, 0x4a, 0xb0, 0x19, 0x4a, 0xb0, 0x1f, 0x4a, 0xb0, 0x2f, 0xa5, 0x10, 0x38, 0xe9,
            0x20, 0x85, 0x10, 0x90, 0x01, 0x60, 0xc6, 0x11, 0xa9, 0x01, 0xc5, 0x11, 0xf0, 0x28, 0x60, 0xe6,
            0x10, 0xa9, 0x1f, 0x24, 0x10, 0xf0, 0x1f, 0x60, 0xa5, 0x10, 0x18, 0x69, 0x20, 0x85, 0x10, 0xb0,
            0x01, 0x60, 0xe6, 0x11, 0xa9, 0x06, 0xc5, 0x11, 0xf0, 0x0c, 0x60, 0xc6, 0x10, 0xa5, 0x10, 0x29,
            0x1f, 0xc9, 0x1f, 0xf0, 0x01, 0x60, 0x4c, 0x35, 0x07, 0xa0, 0x00, 0xa5, 0xfe, 0x91, 0x00, 0x60,
            0xa6, 0x03, 0xa9, 0x00, 0x81, 0x10, 0xa2, 0x00, 0xa9, 0x01, 0x81, 0x10, 0x60, 0xa2, 0x00, 0xea,
            0xea, 0xca, 0xd0, 0xfb, 0x60
    };

    private static BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);
    private static CPU cpu = new CPU();
    private static CPUBus cpuBus = new CPUBus();

    private static final int SCREEN_WIDTH = 32;
    private static final int SCREEN_HEIGHT = 32;
    private static final int SCREEN_RATIO = 8;

    private static final int CONTENT_WIDTH = 32;
    private static final int CONTENT_HEIGHT = 32;
    private static final int CONTENT_RATIO = 8;

    private Panel panel = new Panel() {
        @Override
        public void paint(Graphics g) {
            display();
        }
    };

    public SnakeTestFrame() {
        setTitle("SnakeTest");
        setBackground(Color.white);
        setLayout(new FlowLayout());

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

        panel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_W:
                        cpuBus.write(0xFF, (byte) 0x77);
                        break;
                    case KeyEvent.VK_S:
                        cpuBus.write(0xFF, (byte) 0x73);
                        break;
                    case KeyEvent.VK_A:
                        cpuBus.write(0xFF, (byte) 0x61);
                        break;
                    case KeyEvent.VK_D:
                        cpuBus.write(0xFF, (byte) 0x64);
                        break;
                }
            }
        });

        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        setVisible(true);
    }

    public static void main(String[] args) throws InterruptedException {
        SnakeTestFrame snakeTest = new SnakeTestFrame();
        cpuBus.setCpu(cpu);

//        byte[] codes = new byte[GAME_CODE.length];
//        IntStream.range(0, GAME_CODE.length).forEach(i -> codes[i] = (byte)GAME_CODE[i]);
//
//        cpu.diasm(codes);

        // 将代码加载到CPU内存中 0x600 ~ 0x733
        IntStream.range(0, GAME_CODE.length).forEach(i -> cpuBus.cpuRAM[0x600 + i] = (byte)GAME_CODE[i]);

        // 为了使游戏正常运行，手动将PC设置为0x600，并且修改BRK指令，设置PC = 0x600;
        cpu.PC = 0x600;

        long time = 0;

        do {
            cpu.clock();

            // 写入随机数
            cpuBus.cpuRAM[0xFE] = random();

            // 读 0x0200 ~ 0x0600 渲染画面 32x32
            snakeTest.display();

            Thread.sleep(10);
        } while (time++ < 10e9);
    }

    public static byte random() {
        return (byte) new Random().nextInt(16);
    }

    public void display() {
        int[] imageData = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
        Arrays.fill(imageData, 0);
        IntStream.range(0, 1024).forEach(i -> {
            int rgb = Color.black.getRGB();
            switch (cpuBus.cpuRAM[0x200 + i]) {
                case 0:
                    break;
                case 1:
                    rgb = Color.white.getRGB();
                    break;
                case 2:
                case 9:
                    rgb = Color.gray.getRGB();
                    break;
                case 3:
                case 10:
                    rgb = Color.red.getRGB();
                    break;
                case 4:
                case 11:
                    rgb = Color.green.getRGB();
                    break;
                case 5:
                case 12:
                    rgb = Color.blue.getRGB();
                    break;
                case 6:
                case 13:
                    rgb = Color.magenta.getRGB();
                    break;
                case 7:
                case 14:
                    rgb = Color.yellow.getRGB();
                    break;
            }
            imageData[i] = rgb;
        });

        Graphics graphics = panel.getGraphics();
        graphics.drawImage(image,
                0, 0,
                CONTENT_WIDTH * CONTENT_RATIO,
                CONTENT_HEIGHT * CONTENT_RATIO,
                this.panel);
    }
}
