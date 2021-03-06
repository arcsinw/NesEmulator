package com.arcsinw.nesemulator;

import com.arcsinw.nesemulator.input.Joypad;
import com.arcsinw.nesemulator.input.XboxController;
import com.arcsinw.nesemulator.ui.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

public class Emulator extends JFrame implements PPU.FrameRenderCompletedEventListener {
    private static CPU cpu = new CPU();
    private static PPU ppu = new PPU();
    private static CPUBus cpuBus = new CPUBus();
    private static XboxController controller = new XboxController();

    private static Cartridge cartridge = null;
    private BufferedImage image = new BufferedImage(256, 240, BufferedImage.TYPE_INT_RGB);

    private Panel panel = new Panel();

    // region 常量
    private static final int SCREEN_WIDTH = 256;
    private static final int SCREEN_HEIGHT = 240;
    private static final int SCREEN_RATIO = 3;

    private static final int CONTENT_WIDTH = 256;
    private static final int CONTENT_HEIGHT = 240;
    private static final int CONTENT_RATIO = 3;

    private static final int FPS = 60;

    // endregion

    private void addMenuBar() {
        MenuBar menuBar = new MenuBar();

        {
            Menu fileMenu = new Menu("文件");
            fileMenu.add(new MenuItem("打开"));
            fileMenu.addActionListener(e -> openFilePicker());
            menuBar.add(fileMenu);
        }

        {
            Menu debugMenu = new Menu("调试");
            {
                MenuItem patternTableMenuItem = new MenuItem("Pattern Table");
                patternTableMenuItem.addActionListener(e -> showPatternTableFrame());
                debugMenu.add(patternTableMenuItem);
            }
            {
                MenuItem nameTableMenuItem = new MenuItem("Name Table");
                nameTableMenuItem.addActionListener(e -> showNameTableFrame());
                debugMenu.add(nameTableMenuItem);
            }
            {
                MenuItem oamMenuItem = new MenuItem("Object Attribute Memory");
                oamMenuItem.addActionListener(e -> showOAMFrame());
                debugMenu.add(oamMenuItem);
            }
            {
                MenuItem hexEditMenuItem = new MenuItem("十六进制编辑器");
                hexEditMenuItem.addActionListener(e -> showHexViewerFrame());
                debugMenu.add(hexEditMenuItem);
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

    private final HashMap<Integer, Joypad.ButtonFlag> KEYBOARD_MAPPING = new HashMap() {
        {
            put(KeyEvent.VK_J, Joypad.ButtonFlag.A);
            put(KeyEvent.VK_K, Joypad.ButtonFlag.B);
            put(KeyEvent.VK_1, Joypad.ButtonFlag.Select);
            put(KeyEvent.VK_ENTER, Joypad.ButtonFlag.Start);
            put(KeyEvent.VK_W, Joypad.ButtonFlag.Up);
            put(KeyEvent.VK_S, Joypad.ButtonFlag.Down);
            put(KeyEvent.VK_A, Joypad.ButtonFlag.Left);
            put(KeyEvent.VK_D, Joypad.ButtonFlag.Right);
        }
    };

    public Emulator() {
        setTitle("NesEmulator");
        setBackground(Color.black);
        addMenuBar();

        panel.setPreferredSize(new Dimension(SCREEN_WIDTH * SCREEN_RATIO, SCREEN_HEIGHT * SCREEN_RATIO));
        add(panel);
        pack();

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        panel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();

                if (KEYBOARD_MAPPING.containsKey(keyCode)) {
                    cpuBus.joypad1.setButton(KEYBOARD_MAPPING.get(keyCode), 1);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                int keyCode = e.getKeyCode();

                if (KEYBOARD_MAPPING.containsKey(keyCode)) {
                    cpuBus.joypad1.setButton(KEYBOARD_MAPPING.get(keyCode), 0);
                }
            }
        });

        controller.addListener((button, pressed) -> {
            if (XboxController.XBOXCONTROLLER_MAPPING.containsKey(button)) {
                cpuBus.joypad2.setButton(XboxController.XBOXCONTROLLER_MAPPING.get(button), pressed ? 1 : 0);
            }
        });

        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        setVisible(true);
    }

    public static void main(String[] args) throws IOException {
        Emulator emulator = new Emulator();
//        String romPath = "/nestest.nes";
//        String romPath = "/Pac-Man.nes";
//        String romPath = "/Donkey Kong.nes";
        String romPath = "/896.nes";
//        String romPath = "/Contra.nes";
//        String romPath = "/Contra (USA).nes";
//        String romPath = "/LoZ.nes";
//        String romPath = "/cpu_dummy_writes_ppumem.nes";
//        String romPath = "/Mega Man 2.nes";
//        String romPath = "/Metroid.nes";
//        String romPath = "/cpu_dummy_writes_oam.nes";
//        String romPath = "/palette_pal.nes";
//        String romPath = "/ppu_2000_glitch.nes";
//        String romPath = "/IceClimber.nes";
//        String romPath = "/BattleCity.nes";
        cartridge = new Cartridge(romPath);
        System.out.println(cartridge.header);

        ppu.addFrameRenderCompletedEventListener(emulator);

        cpuBus.setCpu(cpu);
        cpuBus.setPpu(ppu);
        cpuBus.setCartridge(cartridge);
        cpuBus.reset();

        while (true) {
            long start = System.currentTimeMillis();
            if (!emulator.frameRenderCompleted) {
                cpuBus.clock();
            } else {
                long elapsed = System.currentTimeMillis() - start;
                long wait = 1000 / FPS - elapsed;
                if (wait > 0) {
                    try {
                        Thread.sleep(wait);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                emulator.frameRenderCompleted = false;
            }
        }
    }

    public void loop() {
        while (true) {
            long start = System.currentTimeMillis();
            if (!frameRenderCompleted) {
                cpuBus.clock();
            } else {
                long elapsed = System.currentTimeMillis() - start;
                long wait = 1000 / FPS - elapsed;
                if (wait > 0) {
                    try {
                        Thread.sleep(wait);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                frameRenderCompleted = false;
            }
        }
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

    public void showOAMFrame() {
        if (cartridge != null) {
            ObjectAttributeMemoryFrame oamFrame = new ObjectAttributeMemoryFrame(ppu);
        }
    }

    public void showHexViewerFrame() {
        if (cartridge != null) {
            new MemoryViewerFrame("CPU RAM", cpuBus.cpuRAM);
            new MemoryViewerFrame("PPU RAM", ppu.getPalette());
            new MemoryViewerFrame("OAM", ppu.oam);
        }
    }

    public void loadRom(String romPath) throws IOException {
//        cartridge = new Cartridge(romPath);
        cartridge.loadRom(romPath);
        System.out.println(cartridge.header);

//        ppu.addFrameRenderCompletedEventListener(emulator);

        cpuBus.setCpu(cpu);
        cpuBus.setPpu(ppu);
        cpuBus.setCartridge(cartridge);
        cpuBus.reset();

        loop();
    }

    public void openFilePicker()  {
        FileDialog fileDialog = new FileDialog(this, "选择文件", FileDialog.LOAD);
        fileDialog.setVisible(true);

        try {
            loadRom(fileDialog.getDirectory() + fileDialog.getFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
                CONTENT_WIDTH * CONTENT_RATIO,
                CONTENT_HEIGHT * CONTENT_RATIO,
                this.panel);
    }

    private Color fromRGB(int[] c) {
        return new Color(c[0], c[1], c[2]);
    }

    private Color fromIndex(int index) {
        return fromRGB(ColorPalette.COLOR_PALETTE[index]);
    }

    public void display() {
        byte[][] screen = ppu.getScreen();
        int[] imageData = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

        for (int row = 0; row < 240; row++) {
            for (int col = 0; col < 256; col++) {
                imageData[row * 256 + col] = fromIndex(screen[row][col]).getRGB();
            }
        }

        Graphics graphics = this.panel.getGraphics();
        graphics.drawImage(image,
                0, 0,
                CONTENT_WIDTH * CONTENT_RATIO,
                CONTENT_HEIGHT * CONTENT_RATIO,
                this.panel);
    }

    @Override
    public void notifyFrameRenderCompleted() {
        display();
        frameRenderCompleted = true;
    }

    private boolean frameRenderCompleted = false;
}
