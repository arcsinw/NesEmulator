package com.arcsinw.nesemulator;

/**
 * Picture Process Unit
 * 型号是 2C02
 * 0x0000 ~ 0x3FFF
 * 0x4000 - 0xFFFF Mirrors
 */
public class PPU {

    // region 内存

    /**
     * Pattern Table, 图案表 [2][4096]
     * 0x0000 ~ 0x1FFF
     * 0x0000 - 0x0FFF Pattern Table 0
     * 0x1000 - 0x1FFF Pattern Table 1
     */
    private byte[][] patternTable = new byte[2][256 * 16];

    /**
     * Name Table, 命名表 (VRAM, CIRAM)
     * 0x2000 ~ 0x3EFF
     * 0x2000 - 0x2FFF Name Tables + Attribute Tables  共 4KB 分为4块
     * 0x3000 - 0x3EFF Mirrors
     */
    private byte[][] nameTable = new byte[2][1024];

    /**
     * 调色板，共 32 种颜色的索引，指向NES的全部64种颜色
     * 背景 和 精灵 各使用 16 字节
     * 0x3F00 - 0x3F0F Image Palette
     * 0x3F10 - 0x3F1F Sprite Palette
     * 0x3F20 - 0x3FFF Mirrors
     */
    private byte[] palette = new byte[32];

    // endregion

    // region 基于内存的寄存器 0x2000 - 0x2007, 0x4014

    public enum PPUCtrl {
        /**
         * 	Generate an NMI at the start of the
         *  vertical blanking interval (0: off; 1: on)
         */
        NmiEnable(1 << 7),
        /**
         * PPU master/slave select
         * (0: read backdrop from EXT pins; 1: output color on EXT pins)
         */
        SlaveMode(1 << 6),
        /**
         * Sprite size (0: 8x8 pixels; 1: 8x16 pixels)
         */
        SpriteSize(1 << 5),
        /**
         * Background pattern table address (0: $0000; 1: $1000)
         */
        BackgroundSelect(1 << 4),
        /**
         * Sprite pattern table address for 8x8 sprites
         * (0: $0000; 1: $1000; ignored in 8x16 mode)
         */
        SpriteSelect(1 << 3),
        /**
         * VRAM address increment per CPU read/write of PPUDATA
         * (0: add 1, going across; 1: add 32, going down)
         */
        IncrementMode(1 << 2),
        /**
         * Base nametable address 2bit
         * (0 = $2000; 1 = $2400; 2 = $2800; 3 = $2C00)
         */
        NameTableSelectX(1 << 1),
        NameTableSelectY(1 << 0);

        private int mask;

        private PPUCtrl(int m) {
            this.mask = m;
        }
    }

    enum PPUMask {
        /**
         * color emphasis B
         */
        ColorEmphasisB(1 << 7),
        /**
         * color emphasis G
         */
        ColorEmphasisR(1 << 6),
        /**
         * color emphasis R
         */
        ColorEmphasisG(1 << 5),
        /**
         * sprite enable
         */
        SpriteEnable(1 << 4),
        /**
         * background enable
         */
        BackgroundEnable(1 << 3),
        /**
         * sprite left column enable
         */
        SpriteLeftEnable(1 << 2),
        /**
         * background left column enable
         */
        BackgroundLeftEnable(1 << 1),
        /**
         * greyscale
         */
        GreyScale(1 << 0);

        private int mask;

        private PPUMask(int m) {
            this.mask = m;
        }
    }

    /**
     * 只使用了其中的 3 bit
     */
    enum PPUStatus {
        /**
         * vblank
         */
        VBlank(1 << 7),
        /**
         * sprite 0 hit
         */
        SpriteZeroHit(1 << 6),
        /**
         * sprite overflow
         */
        SpriteOverflow(1 << 5);

        private int mask;

        private PPUStatus(int m) {
            this.mask = m;
        }
    }

    /**
     * $2000 Write only
     */
    private byte ppuCtrl = 0x00;

    /**
     * $2001 Write only
     */
    private byte ppuMask = 0x00;

    /**
     * $2002 Read only
     */
    private byte ppuStatus = 0x00;

    /**
     * OAM 的读写地址 无符号型 $2003
     */
    private int oamAddress = 0x00;

    /**
     * 要读出或写入的数据 $2004
     */
    private byte oamData = 0x00;

    /**
     * OAM DMA high address $4014
     * for fast copying of 256 bytes from CPU RAM to OAM
     */
    private byte oamDMA = 0x00;

    /**
     * fine scroll position (two writes: X scroll, Y scroll)
     * $2005 Write only
     */
    private byte PpuScroll = 0x00;

    /**
     * $2006
     */
    private byte ppuAddress = 0x00;

    /**
     * $2007
     */
    private byte ppuData = 0x00;

    public void setPpuCtrlValue(byte v) {
        ppuCtrl = v;
    }

    public void setPpuCtrl(PPUCtrl flag, int value) {
        if (value == 0) {
            ppuCtrl &= (~flag.mask);
        } else {
            ppuCtrl |= flag.mask;
        }
    }

    public byte getPpuCtrl(PPUCtrl p) {
        return (byte) ((ppuCtrl & p.mask) == 0 ? 0 : 1);
    }

    public void setPpuMaskValue(byte v) {
        ppuMask = v;
    }

    public void setPpuMask(PPUMask flag, int value) {
        if (value == 0) {
            ppuMask &= (~flag.mask);
        } else {
            ppuMask |= flag.mask;
        }
    }

    public int getPpuMask(PPUMask p) {
        return (ppuMask & p.mask) == 0 ? 0 : 1;
    }

    public void setPpuStatusValue(byte v) {
        ppuStatus = v;
    }

    public void setPpuStatus(PPUStatus flag, int value) {
        if (value == 0) {
            ppuStatus &= (~flag.mask);
        } else {
            ppuStatus |= flag.mask;
        }
    }

    public int getPpuStatus(PPUStatus p) {
        return (ppuStatus & p.mask) == 0 ? 0 : 1;
    }

    // endregion

    // region 字段
    StringBuilder tmp = new StringBuilder();

    private boolean isFirstPpuAddress = true;
    private byte ppuDataBuffer = 0x00;
    private boolean logging = true;

    // endregion

    /**
     * NES屏幕分辨率 256x240
     * 每个像素点使用 4 bit 作为颜色索引
     */
    byte[][] screen = new byte[240][256];

    public static class OAMEntry {
        /**
         * Y position of top of sprite
         */
        public byte y;

        /**
         * Tile index number
         */
        public byte id;

        /**
         * Attributes
         * 定义Sprite渲染的方式
         */
        public byte attribute;

        /**
         * X position of left side of sprite.
         */
        public byte x;

        public OAMEntry(byte y, byte id, byte attribute, byte x) {
            this.y = y;
            this.id = id;
            this.attribute = attribute;
            this.x = x;
        }

        @Override
        public String toString() {
            return String.format("(%d, %d)      ID: %02X       AT: %02X", x, y, id, attribute);
        }
    }

    /**
     * OAM (Object Attribute Memory)
     *
     */
    public byte[] oam = new byte[256];
//    private OAMEntry[] oam = new OAMEntry[64];


    /**
     * 为方便实现 卷轴滚动 虚拟的寄存器
     * yyy NN YYYYY XXXXX   15 bit
     * ||| || ||||| +++++-- coarse X scroll
     * ||| || +++++-------- coarse Y scroll
     * ||| ++-------------- nametable select
     * +++----------------- fine Y scroll
     */
    private class LoopyRegister {
        /**
         * 5 bit
         */
        byte coarseX = 0;
        /**
         * 5 bit
         */
        byte coarseY = 0;
        /**
         * 1 bit
         */
        byte nameTableX = 0;
        /**
         * 1 bit
         */
        byte nameTableY = 0;
        /**
         * 3 bit
         */
        byte fineY = 0;

        public void setValue(int value) {
            coarseX = (byte) (value & 0x1F);
            coarseY = (byte) ((value >>> 5) & 0x1F);
            nameTableX = (byte) ((value >> 10) & 0x01);
            nameTableY = (byte) ((value >> 11) & 0x01);
            fineY = (byte) ((value >> 12) & 0x07);
        }

        public int getValue() {
            return (coarseX & 0x1F) |
                    ((coarseY & 0x1F) << 5) |
                    ((nameTableX & 0x01) << 10) |
                    ((nameTableY & 0x01) << 11) |
                    ((fineY & 0x07) << 12);
        }
    }

    private LoopyRegister vramAddress = new LoopyRegister();
    private LoopyRegister tmpVramAddress = new LoopyRegister();
    private byte fineX = 0x00;

    /**
     * 卡带
     */
    private Cartridge cartridge;

    public void setCartridge(Cartridge cartridge) {
        this.cartridge = cartridge;
        System.arraycopy(this.cartridge.chr, 0, patternTable[0], 0, 4096);
        System.arraycopy(this.cartridge.chr, 4096, patternTable[1], 0, 4096);
    }


    /**
     * 获取图案表
     * 每个图案8x8个像素，使用16字节
     * 图案表分为 背景图案表 和 精灵图案表，各256个图案
     * @return [2][4096]
     */
    public byte[][] getPatternTable() {
        return this.patternTable;
    }

    public byte[][] getScreen() {
        return this.screen;
    }

    /**
     * 获取命名表
     * 0x2000 - 0x2FFF  共 4KB
     * 分为4块，NES主机提供 2KB，卡带提供另外2KB或者不提供
     * @return
     */
    public byte[][] getNameTable() {
        return nameTable;
    }

    public byte[] getPalette() {
        return palette;
    }

    public byte getColorFromPalette(int paletteId, int pixelId) {
        return ppuRead(0x3F00 + (paletteId << 2) + pixelId);
    }

    private boolean isScrollFirstWrite = true;

    /**
     * CPU 与 PPU之间的通信通过 0x2000 - 0x2007的 8个 寄存器 来实现
     * @param address 写入地址 16bit
     * @param data 要写入的数据 8bit
     */
    public void cpuWrite(int address, byte data) {
        switch (address) {
            // PPU Control
            case 0x0000:
                setPpuCtrlValue(data);
                tmpVramAddress.nameTableX = getPpuCtrl(PPUCtrl.NameTableSelectX);
                tmpVramAddress.nameTableY = getPpuCtrl(PPUCtrl.NameTableSelectY);
                break;
            // PPU Mask
            case 0x0001:
                setPpuMaskValue(data);
                break;
            // PPU Status (not writeable)
            case 0x0002:
                break;
            // OAM Address
            case 0x0003:
                oamAddress = data;
                break;
            case 0x0004:
                oam[oamAddress] = data;
                break;
            // PPU SCROLL
            case 0x0005:
                if (isFirstPpuAddress) {
                    fineX = (byte) (data & 0x07);
                    tmpVramAddress.coarseX = (byte) (data >>> 3);
                    isFirstPpuAddress = false;
                } else {
                    tmpVramAddress.fineY = (byte) (data & 0x07);
                    tmpVramAddress.coarseY = (byte) (data >>> 3);
                    isFirstPpuAddress = true;
                }
                break;
            // PPU Address
            case 0x0006:
                // CPU 和 PPU在不同的总线上，无法直接访问内存，通过 0x0006和0x0007来实现内存的读写
                // PPU的地址为16位，PPU Data寄存器是8位，需要进行两次写入才能设置地址
                // 先写入高地址 再写入低地址
                if (isFirstPpuAddress) {
                    if (logging) {
                        tmp.append(String.format(" %02X ", data));
                    }
//                    tmpPpuAddress = (tmpPpuAddress & 0x00FF) | ((data << 8) & 0x0FFFF);
                    tmpVramAddress.setValue((tmpVramAddress.getValue() & 0x00FF) | (((data & 0x3F) << 8) & 0x0FFFF));
                    isFirstPpuAddress = false;
                } else {
                    if (logging) {
                        tmp.append(String.format(" %02X ", data));
                    }
//                    tmpPpuAddress = (tmpPpuAddress & 0xFF00) | (data & 0x00FF);
                    tmpVramAddress.setValue((tmpVramAddress.getValue() & 0xFF00) + (data & 0x00FF));

                    {
                        vramAddress.fineY = tmpVramAddress.fineY;
                        vramAddress.nameTableX = tmpVramAddress.nameTableX;
                        vramAddress.nameTableY = tmpVramAddress.nameTableY;
                        vramAddress.coarseX = tmpVramAddress.coarseX;
                        vramAddress.coarseY = tmpVramAddress.coarseY;
                    }

                    isFirstPpuAddress = true;
                }
                break;
            // PPU Data
            case 0x0007:
//                ppuWrite(tmpPpuAddress, data);
                ppuWrite(vramAddress.getValue(), data);

                if (logging) {
                    System.out.println(String.format("%S %04X %X", tmp.toString(), vramAddress.getValue(), data));
                    tmp.delete(0, tmp.length());
                }
//                tmpPpuAddress += (getPpuCtrl(PPUCtrl.IncrementMode) == 0 ? 1 : 32);
                vramAddress.setValue(vramAddress.getValue() + (getPpuCtrl(PPUCtrl.IncrementMode) == 0 ? 1 : 32));
                break;
            default:
                break;
        }

        // 改变PPUSTATUS的后5位
//        ppuStatus = (byte) ((ppuStatus & 0xE0) | (data & 0x1F));
    }

    /**
     *
     * 读取寄存器时可能会改变它们的值
     * @param address 数据地址
     * @return
     */
    public byte cpuRead(int address, boolean readOnly) {
        byte data = 0x00;

        if (readOnly) // for test
        {
            // Reading from PPU registers can affect their contents
            // so this read only option is used for examining the
            // state of the PPU without changing its state. This is
            // really only used in debug mode.
            switch (address)
            {
                case 0x0000:    // PPUCtrl
                    data = ppuCtrl;
                    break;
                case 0x0001:    // PPUMask
                    data = ppuMask;
                    break;
                case 0x0002:    // PPU Status
                    // ppu status 只有前三位有效
                    data = ppuStatus;
                    break;
                case 0x0003:    // OAM Address
                    break;
                case 0x0006:    // PPU Address
                    break;
                case 0x0007:    // PPU Data
                    break;
            }
        }
        else {
            switch (address) {
                // PPU Control
                case 0x0000:
                    break;
                // PPU Mask
                case 0x0001:
                    break;
                // PPU Status
                case 0x0002:
                    // ppu status 只有前三位有效
                    data = (byte) ((ppuStatus & 0xE0) | (ppuDataBuffer & 0x1F));
                    setPpuStatus(PPUStatus.VBlank, 0);
                    isFirstPpuAddress = true;
                    break;
                // OAM Address
                case 0x0003:
                    break;
                // OAM Data
                case 0x0004:
                    data = oam[oamAddress];
                    break;
                // PPU Address
                case 0x0006:
                    break;
                // PPU Data
                case 0x0007:
                    // CPU 从 PPU读取数据 要慢一个 ppuRead
                    data = ppuDataBuffer;
//                    ppuDataBuffer = ppuRead(tmpPpuAddress);
                    ppuDataBuffer = ppuRead(vramAddress.getValue());

                    // 读取Palettes没有延迟
//                    if (tmpPpuAddress >= 0x3F00) {
                    if (vramAddress.getValue() >= 0x3F00) {
                        data = ppuDataBuffer;
                    }

//                    tmpPpuAddress += (getPpuCtrl(PPUCtrl.IncrementMode) == 0 ? 1 : 32);
                    vramAddress.setValue(vramAddress.getValue() + (getPpuCtrl(PPUCtrl.IncrementMode) == 0 ? 1 : 32));
                    break;
            }
        }
        return data;
    }

    public void ppuWrite(int address, byte data) {
        address &= 0x3FFF;

        if (address >= 0x0000 && address <= 0x1FFF) {
            // Pattern table
            patternTable[(address & 0x1000) >>> 12][address & 0x0FFF] = data;
        }
        if (address >= 0x2000 && address <= 0x3EFF) {
            // Name Tables 实际地址 0x2000 - 0x2FFF 其余是Mirror（只Mirror了0x2000 - 0x2EFF）
            // 最多有4个Name Table
            address &= 0x0FFF;
            if (this.cartridge.header.mirror == Cartridge.Mirror.Vertical) // vertical mirror
            {
                // Vertical
                if (address >= 0x0000 && address <= 0x03FF) {
                    nameTable[0][address & 0x03FF] = data;
                }
                if (address >= 0x0400 && address <= 0x07FF) {
                    nameTable[1][address & 0x03FF] = data;
                }
                if (address >= 0x0800 && address <= 0x0BFF) {
                    nameTable[0][address & 0x03FF] = data;
                }
                if (address >= 0x0C00 && address <= 0x0FFF) {
                    nameTable[1][address & 0x03FF] = data;
                }
            }
            else // horizontal mirror
            {
                // Horizontal
                if (address >= 0x0000 && address <= 0x03FF) {
                    nameTable[0][address & 0x03FF] = data;
                }
                if (address >= 0x0400 && address <= 0x07FF) {
                    nameTable[0][address & 0x03FF] = data;
                }
                if (address >= 0x0800 && address <= 0x0BFF) {
                    nameTable[1][address & 0x03FF] = data;
                }
                if (address >= 0x0C00 && address <= 0x0FFF) {
                    nameTable[1][address & 0x03FF] = data;
                }
            }
        }
        else if (address >= 0x3F00 && address <= 0x3FFF) {
            // Palettes 真实地址 0x3F00 - 0x3F1F 剩下的是Mirrors
            address &= 0x001F;  // % 32

            if (address == 0x0010 || address == 0x0014 || address == 0x0018 || address == 0x001C) {
                palette[address - 0x10] = data;
            }

            palette[address] = data;
        }
    }

    public byte ppuRead(int address) {
        address &= 0x3FFF;
        byte data = 0x00;

        if (address >= 0x0000 && address <= 0x1FFF) {
            // Pattern table
            data = patternTable[(address & 0x1000) >> 12][address & 0x0FFF];
        }
        if (address >= 0x2000 && address <= 0x3EFF) {
            // Name Tables 实际地址 0x2000 - 0x2FFF 其余是Mirror
            // 最多有4个Name Table
            address &= 0x0FFF;
            if (cartridge.header.mirror == Cartridge.Mirror.Vertical) {
                // Vertical mirror
                if (address >= 0x0000 && address <= 0x03FF) {
                    data = nameTable[0][address & 0x03FF];
                }
                if (address >= 0x0400 && address <= 0x07FF) {
                    data = nameTable[1][address & 0x03FF];
                }
                if (address >= 0x0800 && address <= 0x0BFF) {
                    data = nameTable[0][address & 0x03FF];
                }
                if (address >= 0x0C00 && address <= 0x0FFF) {
                    data = nameTable[1][address & 0x03FF];
                }
            }
            else if (cartridge.header.mirror == Cartridge.Mirror.Horizontal) {
                // Horizontal mirror
                if (address >= 0x0000 && address <= 0x03FF) {
                    data = nameTable[0][address & 0x03FF];
                }
                if (address >= 0x0400 && address <= 0x07FF) {
                    data = nameTable[0][address & 0x03FF];
                }
                if (address >= 0x0800 && address <= 0x0BFF) {
                    data = nameTable[1][address & 0x03FF];
                }
                if (address >= 0x0C00 && address <= 0x0FFF) {
                    data = nameTable[1][address & 0x03FF];
                }
            }
        }
        else if (address >= 0x3F00 && address <= 0x3FFF) {
            // Palettes 真实地址 0x3F00 - 0x3F1F 剩下的是Mirrors
            address &= 0x001F;  // % 32
            if (address % 4 == 0) {
                data = (byte) (palette[0] & (getPpuMask(PPUMask.GreyScale) == 1 ? 0x30 : 0x3F));
            } else {
                if (address == 0x0010) {
                    address = 0x0000;
                }
                if (address == 0x0014) {
                    address = 0x0004;
                }
                if (address == 0x0018) {
                    address = 0x0008;
                }
                if (address == 0x001C) {
                    address = 0x000C;
                }
                data = (byte) (palette[address] & (getPpuMask(PPUMask.GreyScale) == 1 ? 0x30 : 0x3F));
            }
        }

        return data;
    }

    /**
     * 扫描线编号
     * -1 - 261
     */
    private int scanLine = 0;

    /**
     * 每根扫描线有 341 cycles
     * 每个cycle 渲染一个像素
     */
    private int cycles = 0;

    public boolean nmi = false;

    private byte nextBackgroundTileId = 0x00;
    private byte nextBackgroundTileAttribute = 0x00;

    /**
     * 下一个背景tile的Pattern 高8位
     */
    private byte nextBackgroundTilePatternHi = 0x00;

    /**
     * 下一个背景tile的Pattern 低8位
     */
    private byte nextBackgroundTilePatternLo = 0x00;

    private short backgroundPatternLoShifter = 0x0000;
    private short backgroundPatternHiShifter = 0x0000;
    private short backgroundAttributeLoShifter = 0x0000;
    private short backgroundAttributeHiShifter = 0x0000;

    public void loadBackgroundShifters() {
        backgroundPatternHiShifter = (short) ((backgroundPatternHiShifter & 0xFF00) | nextBackgroundTilePatternHi);
        backgroundPatternLoShifter = (short) ((backgroundPatternLoShifter & 0xFF00) | nextBackgroundTilePatternLo);

        backgroundAttributeHiShifter = (short) ((backgroundAttributeHiShifter & 0xFF00) | ((nextBackgroundTileAttribute & 0x02) != 0 ? 0xFF : 0x00));
        backgroundAttributeLoShifter = (short) ((backgroundAttributeLoShifter & 0xFF00) | ((nextBackgroundTileAttribute & 0x01) != 0 ? 0xFF : 0x00));
    }

    public void updateShifters() {
        if (getPpuMask(PPUMask.BackgroundEnable) != 0) {
            backgroundAttributeHiShifter <<= 1;
            backgroundAttributeLoShifter <<= 1;

            backgroundPatternHiShifter <<= 1;
            backgroundPatternLoShifter <<= 1;
        }
    }

    public void incrementScrollX() {
        if ((getPpuMask(PPUMask.BackgroundEnable)) != 0 || (getPpuMask(PPUMask.SpriteEnable) != 0)) {
            if (vramAddress.coarseX == 31) {
                vramAddress.coarseX = 0;

                // 修改当前的Name table
                vramAddress.nameTableX = (byte) (1 - vramAddress.nameTableX);
            } else {
                vramAddress.coarseX++;
            }
        }
    }

    public void incrementScrollY() {
        if ((getPpuMask(PPUMask.BackgroundEnable)) != 0 || (getPpuMask(PPUMask.SpriteEnable) != 0)) {
            if (vramAddress.fineY < 7) {
                vramAddress.fineY++;
            } else {
                vramAddress.fineY = 0;

                if (vramAddress.coarseY == 29) {
                    vramAddress.coarseY = 0;

                    // 修改当前的Name table
                    vramAddress.nameTableY = (byte) (1 - vramAddress.nameTableY);
                } else if (vramAddress.coarseY == 31) {
                    vramAddress.coarseY = 0;
                } else {
                    vramAddress.coarseY++;
                }
            }
        }
    }

    public void transferAddressX() {
        if ((getPpuMask(PPUMask.BackgroundEnable)) != 0 || (getPpuMask(PPUMask.SpriteEnable) != 0)) {
            vramAddress.coarseX = tmpVramAddress.coarseX;
            vramAddress.nameTableX = tmpVramAddress.nameTableX;
        }
    }

    public void transferAddressY() {
        if ((getPpuMask(PPUMask.BackgroundEnable)) != 0 || (getPpuMask(PPUMask.SpriteEnable) != 0)) {
            vramAddress.coarseY = tmpVramAddress.coarseY;
            vramAddress.nameTableY = tmpVramAddress.nameTableY;
            vramAddress.fineY = tmpVramAddress.fineY;
        }
    }

    /**
     * 参考http://wiki.nesdev.com/w/images/4/4f/Ppu.svg
     * 262条扫描线 （0~261）或（-1~260）  (0~239)是可见扫描线
     * 每条扫描线341 cycles (0~340)
     */
    public void clock() {
        // visible frame
        if (scanLine >= -1 && scanLine < 240) {
            if (scanLine == 0 && cycles == 0) {
                cycles = 1;
            }

            if (scanLine == -1 && cycles == 1) {
                setPpuStatus(PPUStatus.VBlank, 0);
            }

            // visible cycles
//            if ((cycles >= 1 && cycles <= 256) || (cycles >= 321 && cycles <= 336)) {
            if ((cycles >= 2 && cycles < 258) || (cycles >= 321 && cycles < 338)) {
                updateShifters();

                // |NT byte|AT byte|   Lo  |hi  inc hori(v)|
                // | 1 | 2 | 3 | 4 | 5 | 6 |   7   |   8   |
//                switch (cycles % 8) {
                switch ((cycles - 1) % 8) {
                    case 0:
                        // 读取后续8个像素的信息
                        loadBackgroundShifters();

                        // 读取Name table
                        nextBackgroundTileId = ppuRead(0x2000 + (vramAddress.getValue() & 0x0FFF));
                        break;
                    case 2:
                        // 读取Attribute table中的1字节（Attribute table中1字节控制一个4x4tile 的大Tile的颜色）
                        // 通过nameTableX,Y 计算tile所在的Name table
                        // 通过coarseX,Y 计算tile所在的 4x4tile 的大Tile id
                        // 使用大Tile id获取attribute
                        nextBackgroundTileAttribute = ppuRead(0x23C0 + (vramAddress.nameTableY << 11) |
                                (vramAddress.nameTableX << 10) |
                                (vramAddress.coarseX >>> 2) |
                                ((vramAddress.coarseY >>> 2) << 3));

                        // 从Attribute Table的1字节中选出2 bit
                        if ((vramAddress.coarseY & 0x02) != 0) {
                            nextBackgroundTileAttribute >>>= 4;
                        }

                        if ((vramAddress.coarseX & 0x02) != 0) {
                            nextBackgroundTileAttribute >>>= 2;
                        }

                        nextBackgroundTileAttribute &= 0x03;
                        break;
                    case 4:
                        // 读取Pattern table低字节
                        nextBackgroundTilePatternLo = ppuRead((getPpuCtrl(PPUCtrl.BackgroundSelect) * 0x1000) +
                                nextBackgroundTileId * 16 + vramAddress.fineY);
                        break;
                    case 6:
                        // 读取Pattern table高字节
                        nextBackgroundTilePatternHi = ppuRead((getPpuCtrl(PPUCtrl.BackgroundSelect) * 0x1000) +
                                nextBackgroundTileId * 16 + vramAddress.fineY + 8);
                        break;

                    case 7:
                        // increment horizontal of v
                        incrementScrollX();
                        break;
                    default:
                        break;
                }
            }

            if (cycles == 256) {
                incrementScrollY();
            }

            if (cycles == 257) {
                loadBackgroundShifters();
                transferAddressX();
            }

            if (cycles == 338 || cycles == 340)
            {
                nextBackgroundTileId = ppuRead(0x2000 + (vramAddress.getValue() & 0x0FFF));
            }

            if (scanLine == -1 && cycles >= 280 && cycles <= 304) {
                transferAddressY();
            }
        }

        // Post-render line, do nothing
        if (scanLine == 240) {        }

        if (scanLine == 241 && cycles == 1) {
            setPpuStatus(PPUStatus.VBlank, 1);

            // 在VBlank时 触发CPU中断
            if (getPpuCtrl(PPUCtrl.NmiEnable) == 1) {
                nmi = true;
            }
        }

        if (getPpuMask(PPUMask.BackgroundEnable) != 0) {
            // 二进制中的 1 表明当前渲染的像素位置
            short shifterMask = (short) (0x8000 >>> fineX);

            byte colorBit0 = (byte) ((backgroundPatternLoShifter & shifterMask) != 0 ? 1 : 0);
            byte colorBit1 = (byte) ((backgroundPatternHiShifter & shifterMask) != 0 ? 1 : 0);

            byte colorId = (byte) (colorBit0 | (colorBit1 << 1));

            byte colorBit2 = (byte) ((backgroundAttributeLoShifter & shifterMask) != 0 ? 1 : 0);
            byte colorBit3 = (byte) ((backgroundAttributeHiShifter & shifterMask) != 0 ? 1 : 0);

            byte paletteId = (byte) (colorBit2 | (colorBit3 << 1));

            if (scanLine >= 0 && scanLine < 240 && cycles >= 1 && cycles < 256) {
                screen[scanLine][cycles-1] = getColorFromPalette(paletteId, colorId);
            }
        }

        cycles++;
        if (cycles >= 341) {
            cycles = 0;
            scanLine++;

            if (scanLine >= 261) {
                scanLine = -1;
            }
        }
    }

    public void reset() {
        isFirstPpuAddress = true;
        ppuDataBuffer = 0x00;
        scanLine = 0;
        cycles = 0;

        fineX = 0;
        nextBackgroundTileAttribute = 0x00;
        nextBackgroundTilePatternHi = 0x00;
        nextBackgroundTilePatternLo = 0x00;
        nextBackgroundTileId = 0x00;

        backgroundAttributeHiShifter = 0x00;
        backgroundAttributeLoShifter = 0x00;
        backgroundPatternHiShifter = 0x00;
        backgroundPatternLoShifter = 0x00;

        ppuStatus = 0;
        ppuCtrl = 0;
        ppuMask = 0;

        vramAddress.setValue(0);
        tmpVramAddress.setValue(0);
    }
}
