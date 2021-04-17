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

    public int getPpuCtrl(PPUCtrl p) {
        return (ppuCtrl & p.mask) == 0 ? 0 : 1;
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

    private int tmpPpuAddress = 0x00;
    private boolean isFirstPpuAddress = true;
    private int ppuDataBuffer = 0x00;
    private boolean logging = false;

    // endregion

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
     */
    private class LoopyRegister {
        byte coarseX = 0;
        byte coarseY = 0;
        byte nameTableX = 0;
        byte nameTableY = 0;
        byte fineY = 0;
    }

    private LoopyRegister loopyRegister = new LoopyRegister();

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

    /**
     * CPU 与 PPU之间的通信通过 0x2000 - 0x2007的 8个 寄存器 来实现
     * @param address 写入地址 16bit
     * @param data 要写入的数据 8bit
     */
    public void cpuWrite(int address, byte data) {
        switch (address) {
            case 0x0000:    // PPU Control
                setPpuCtrlValue(data);
                break;
            case 0x0001:    // PPU Mask
                setPpuMaskValue(data);
                break;
            case 0x0002:    // PPU Status (not writeable)
                break;
            case 0x0003:    // OAM Address
                oamAddress = data;
                break;
            case 0x0004:
                oam[oamAddress] = data;
                break;
            case 0x0006: // PPU Address
                // CPu 和 PPU在不同的总线上，无法直接访问内存，通过 0x0006和0x0007来实现内存的读写
                // PPU的地址为16位，PPU Data寄存器是8位，需要进行两次写入才能设置地址
                // 先写入高地址 再写入低地址
                if (isFirstPpuAddress) {
                    if (logging) {
                        tmp.append(String.format(" %02X ", data));
                    }
                    tmpPpuAddress = (tmpPpuAddress & 0x00FF) | ((data << 8) & 0x0FFFF);
                    isFirstPpuAddress = false;
                } else {
                    if (logging) {
                        tmp.append(String.format(" %02X ", data));
                    }
                    tmpPpuAddress = (tmpPpuAddress & 0xFF00) | (data & 0x00FF);
                    isFirstPpuAddress = true;
                }
                break;
            case 0x0007: // PPU Data
                ppuWrite(tmpPpuAddress, data);

                if (logging) {
                    System.out.println(String.format("%S %04X %X", tmp.toString(), tmpPpuAddress, data));
                    tmp.delete(0, tmp.length());
                }
                tmpPpuAddress += (getPpuCtrl(PPUCtrl.IncrementMode) == 0 ? 1 : 32);
                break;
        }

        // 改变PPUSTATUS的后5位
        ppuStatus = (byte) ((ppuStatus & 0xE0) | (data & 0x1F));
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
                case 0x0000:    // PPU Control
                    data = ppuCtrl;
                    break;
                case 0x0001:    // PPU Mask
                    break;
                case 0x0002:    // PPU Status
                    // 暂时写死 使程序能往下运行
//                    setPpuStatus(PPUStatus.VBlank, 1);
                    // ppu status 只有前三位有效
//                    data = (byte) ((ppuStatus & 0xE0) | (ppuDataBuffer & 0x1F));
                    data = ppuStatus;
                    setPpuStatus(PPUStatus.VBlank, 0);
                    isFirstPpuAddress = true;
                    break;
                case 0x0003:    // OAM Address
                    break;
                case 0x0004:
                    data = oam[oamAddress];
                    break;
                case 0x0006:    // PPU Address
                    break;
                case 0x0007:    // PPU Data
                    // CPU 从 PPU读取数据 要慢一个 ppuRead
                    data = (byte) ppuDataBuffer;
                    ppuDataBuffer = ppuRead(tmpPpuAddress);

                    // 读取Palettes没有延迟
                    if (tmpPpuAddress >= 0x3F00) {
                        data = (byte) ppuDataBuffer;
                    }

                    tmpPpuAddress += (getPpuCtrl(PPUCtrl.IncrementMode) == 0 ? 1 : 32);
                    break;
            }
        }
        return data;
    }

    public void ppuWrite(int address, byte data) {
        address &= 0x3FFF;

        if (address >= 0x0000 && address <= 0x1FFF) {
            // Pattern table
//            cartridge.ppuWrite(address, data);
            patternTable[(address & 0x1000) >>> 12][address & 0x0FFF] = data;
        }
        if (address >= 0x2000 && address <= 0x3EFF) {
            // Name Tables 实际地址 0x2000 - 0x2FFF 其余是Mirror（只Mirror了0x2000 - 0x2EFF）
            // 最多有4个Name Table
            address &= 0x0FFF;
            if (this.cartridge.header.mirror == Cartridge.Mirror.Vertical) // vertical mirror
            {
                // Vertical
                if (address >= 0x0000 && address <= 0x03FF)
                    nameTable[0][address & 0x03FF] = data;
                if (address >= 0x0400 && address <= 0x07FF)
                    nameTable[1][address & 0x03FF] = data;
                if (address >= 0x0800 && address <= 0x0BFF)
                    nameTable[0][address & 0x03FF] = data;
                if (address >= 0x0C00 && address <= 0x0FFF)
                    nameTable[1][address & 0x03FF] = data;
            }
            else // horizontal mirror
            {
                // Horizontal
                if (address >= 0x0000 && address <= 0x03FF)
                    nameTable[0][address & 0x03FF] = data;
                if (address >= 0x0400 && address <= 0x07FF)
                    nameTable[0][address & 0x03FF] = data;
                if (address >= 0x0800 && address <= 0x0BFF)
                    nameTable[1][address & 0x03FF] = data;
                if (address >= 0x0C00 && address <= 0x0FFF)
                    nameTable[1][address & 0x03FF] = data;
            }
        }
        else if (address >= 0x3F00 && address <= 0x3FFF) {
            // Palettes 真实地址 0x3F00 - 0x3F1F 剩下的是Mirrors
            address &= 0x001F;  // % 32
//            if (address == 0x0010) address = 0x0000;
//            if (address == 0x0014) address = 0x0004;
//            if (address == 0x0018) address = 0x0008;
//            if (address == 0x001C) address = 0x000C;
//
            if (address == 0x10 || address == 0x0014 || address == 0x0018 || address == 0x001C) {
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
//            data = cartridge.ppuRead(address);
            data = patternTable[(address & 0x1000) >> 12][address & 0x0FFF];
        }
        if (address >= 0x2000 && address <= 0x3EFF) {
            // Name Tables 实际地址 0x2000 - 0x2FFF 其余是Mirror
            // 最多有4个Name Table
            address &= 0x0FFF;
            if (cartridge.header.mirror == Cartridge.Mirror.Vertical) {
                // Vertical mirror
                if (address >= 0x0000 && address <= 0x03FF)
                    data = nameTable[0][address & 0x03FF];
                if (address >= 0x0400 && address <= 0x07FF)
                    data = nameTable[1][address & 0x03FF];
                if (address >= 0x0800 && address <= 0x0BFF)
                    data = nameTable[0][address & 0x03FF];
                if (address >= 0x0C00 && address <= 0x0FFF)
                    data = nameTable[1][address & 0x03FF];
            }
            else if (cartridge.header.mirror == Cartridge.Mirror.Horizontal) {
                // Horizontal mirror
                if (address >= 0x0000 && address <= 0x03FF)
                    data = nameTable[0][address & 0x03FF];
                if (address >= 0x0400 && address <= 0x07FF)
                    data = nameTable[0][address & 0x03FF];
                if (address >= 0x0800 && address <= 0x0BFF)
                    data = nameTable[1][address & 0x03FF];
                if (address >= 0x0C00 && address <= 0x0FFF)
                    data = nameTable[1][address & 0x03FF];
            }
        }
        else if (address >= 0x3F00 && address <= 0x3FFF) {
            // Palettes 真实地址 0x3F00 - 0x3F1F 剩下的是Mirrors
            address &= 0x001F;  // % 32
            if (address % 4 == 0) {
                data = (byte) (palette[0] & (getPpuMask(PPUMask.GreyScale) == 1 ? 0x30 : 0x3F));
            } else {
                if (address == 0x0010) address = 0x0000;
                if (address == 0x0014) address = 0x0004;
                if (address == 0x0018) address = 0x0008;
                if (address == 0x001C) address = 0x000C;
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

    public void clock() {
        if (scanLine == -1 && cycles == 0) {
            setPpuStatus(PPUStatus.VBlank, 0);
        }

        if (scanLine == 241 && cycles == 0) {
            setPpuStatus(PPUStatus.VBlank, 1);

            // 在VBlank时 触发CPU中断
            if (getPpuCtrl(PPUCtrl.NmiEnable) == 1) {
                nmi = true;
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
        tmpPpuAddress = 0x00;
        isFirstPpuAddress = true;
        ppuDataBuffer = 0x00;
        scanLine = 0;
        cycles = 0;

        ppuStatus = 0;
        ppuCtrl = 0;
        ppuMask = 0;
    }

    public int getAddress() {
        return tmpPpuAddress;
    }
}
