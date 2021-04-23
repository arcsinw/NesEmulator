package com.arcsinw.nesemulator;

import com.arcsinw.nesemulator.ui.*;
import com.arcsinw.nesemulator.utils.IdeaDebugUtils;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Emulator extends Frame implements PPU.FrameRenderCompletedEventListener {
    private static CPU cpu = new CPU();
    private static PPU ppu = new PPU();
    private static CPUBus cpuBus = new CPUBus();

    private static Cartridge cartridge;
    private BufferedImage image = new BufferedImage(256, 240, BufferedImage.TYPE_INT_RGB);

    private static final int SCREEN_WIDTH = 256;
    private static final int SCREEN_HEIGHT = 240;
    private static final int SCREEN_RATIO = 3;

    private static final int CONTENT_WIDTH = 256;
    private static final int CONTENT_HEIGHT = 240;
    private static final int CONTENT_RATIO = 3;

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

    private Panel panel = new Panel();

    private final HashMap<Integer, Joypad.ButtonFlag> KEY_MAPPING = new HashMap() {
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
                int keyCode = e.getKeyCode();

                if (KEY_MAPPING.containsKey(keyCode)) {
                    cpuBus.joypad1.setButton(KEY_MAPPING.get(keyCode), 1);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                int keyCode = e.getKeyCode();

                if (KEY_MAPPING.containsKey(keyCode)) {
                    cpuBus.joypad1.setButton(KEY_MAPPING.get(keyCode), 0);
                }
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
            cpuBus.clock();
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
        }
    }

    public void loadRom(String romPath) throws IOException {
        cartridge = new Cartridge(romPath);
        System.out.println(cartridge.header);

        System.out.println(cartridge.header);
        cpuBus.setCartridge(cartridge);

        int line = 0;
        cpuBus.reset();

        while (line++ >= 0 && true) {
            cpuBus.clock();

            if (line >= 400000) {
//                emulator.displayNameTable();
            }
        }
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
                CONTENT_WIDTH * CONTENT_RATIO,
                CONTENT_HEIGHT * CONTENT_RATIO,
                this.panel);
    }

    private Color fromRGB(int[] c) {
        return new Color(c[0], c[1], c[2]);
    }

    private Color fromIndex(byte index) {
        return fromRGB(ColorPalette.COLOR_PALETTE[index]);
    }

    public void displayNameTable() {
        int[] imageData = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

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
        byte[][] nameTableColorMap = new byte[2][960 * 16];

        // 填充 nameTableColorMap
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 960; j++) {
//                int s = (ppu.ppuRead(0x2000 + 0x0400 * i + j) & 0x00FF) * 16;
                int backgroundAddress = ppu.getPpuCtrl(PPU.PPUCtrl.BackgroundSelect);
                int s = (nameTable[i][j] & 0x00FF) * 16;
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
            byte[] attributeTable = Arrays.copyOfRange(nameTable[i], 960, 1024);

            for (int j = 0; j < 960; j++) { // tile
                // 读 16 字节
                byte[] tileData = Arrays.copyOfRange(nameTableColorMap[i], 16 * j, 16 * (j + 1));

                int row = j / 32;
                int col = j % 32;

                // 4tile x 4tile 的大Tile id
                int bigTileId = (row / 4) * 8 + col / 4;

//                byte colorByte = ppu.ppuRead(0x2000 + 0x0400 * i + 960 + bigTileId);
                byte colorByte =  attributeTable[bigTileId];

                int bigTileLeftTopRow = (bigTileId >>> 3) << 2;
                int bigTileLeftTopCol = (bigTileId & 0x07) << 2;

                // 小tile的offset id (0, 1, 2, 3)
                int tileOffset = ((row - bigTileLeftTopRow) / 2) * 2 + (col - bigTileLeftTopCol) / 2;

                for (int k = 0; k < 64; k++) {  // 像素点
                    // bit 0
                    byte lo = (byte) ((tileData[k / 8] >> (7 - k % 8)) & 0x01);

                    // bit 1
                    byte hi = (byte) (((tileData[k / 8 + 8] >> (7 - k % 8)) & 0x01) << 1);

                    byte color = (byte) (lo | hi | (((colorByte >>> ((tileOffset) * 2) & 0x03) << 2)));

                    imageColor[i][j][k] = color;
                }
            }
        }

        for (int k = 0; k < 960; k++) { // tile
            int startRow = (k / 32) * 8;
            int startCol = (k % 32) * 8;
            for (int i = 0; i < 64; i++) { // 像素
                int x = startCol + i % 8;
                int y = startRow + i / 8;
                byte color = ppu.ppuRead(0x3F00 + imageColor[0][k][i]);
                imageData[y * 256 + x] = fromIndex(color).getRGB();
            }
        }

        Graphics graphics = this.panel.getGraphics();
        graphics.drawImage(image,
                0, 0,
                CONTENT_WIDTH * CONTENT_RATIO,
                CONTENT_HEIGHT * CONTENT_RATIO,
                this.panel);
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
    }
}
