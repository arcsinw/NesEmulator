package com.arcsinw.nesemulator;

/**
 * Picture Process Unit
 * 型号是 2C02
 * 0x0000 ~ 0x3FFF
 * 0x4000 - 0xFFFF Mirrors
 */
public class PPU implements CPUBusDevice {

    // region 内存

    /**
     * Pattern Table, 图案表
     * 0x0000 ~ 0x1FFF
     * 0x0000 - 0x0FFF Pattern Table 0
     * 0x1000 - 0x1FFF Pattern Table 1
     */
    private byte[][][] patternTable = new byte[2][256][64];

    /**
     * Name Table, 命名表 (VRAM, CIRAM)
     * 0x2000 ~ 0x3EFF
     * 0x2000 - 0x2FFF Name Tables + Attribute Tables
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
//    private byte[] palette = new byte[2 * 16];
    private byte[] palette = new byte[]{
            0x22, 0x29, 0x1A, 0x0F, 0x0F, 0x36, 0x17, 0x0F, 0x0F, 0x30, 0x21, 0x0F, 0x0F, 0x17, 0x17, 0x0F, // Image Palette
            0x22, 0x16, 0x27, 0x18, 0x0F, 0x1A, 0x30, 0x27, 0x0F, 0x16, 0x30, 0x27, 0x0F, 0x0F, 0x36, 0x17  // Sprite Palette
    };

    // endregion

    // region 基于内存的寄存器 0x2000 - 0x2007

    enum PPUCtrl {
        /**
         * 	NMI enable
         */
        NmiEnable(1 << 7),
        /**
         * PPU master/slave
         */
        SlaveMode(1 << 6),
        /**
         * sprite height
         */
        SpriteHeight(1 << 5),
        /**
         * background tile select
         */
        BackgroundSelect(1 << 4),
        /**
         * sprite tile select
         */
        SpriteSelect(1 << 3),
        /**
         * increment mode
         */
        IncrementMode(1 << 2),
        /**
         * nametable select x
         */
        NameTableSelectX(1 << 1),
        /**
         * nametable select y
         */
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
     * $2000
     */
    private byte ppuCtrl = 0x00;

    /**
     * $2001
     */
    private byte ppuMask = 0x00;

    /**
     * $2002
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
     */
    private byte oamDMA = 0x00;

    /**
     * fine scroll position (two writes: X scroll, Y scroll)
     * $2005
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

    /**
     * 卡带
     */
    private Cartridge cartridge;

    public void setCartridge(Cartridge cartridge) {
        this.cartridge = cartridge;
    }

    /**
     * 获取图案表
     * 每个图案8x8个像素，使用16字节
     * 图案表分为 背景图案表 和 精灵图案表，各256个图案
     * @return
     */
    public byte[][][] getPatternTable() {
        byte[] data = cartridge.chr;
        byte[][][] result = new byte[2][256][64];
        int len = data.length;

        int index = 0;
        int start = 0;

        while (start < len - 1 && index < 256) {
            // 读8个字节作为图案的 0位
            // 读8个字节作为图案的 1位
            for (int i = 0; i < 8; i++) {
                byte low0 = data[start + i];
                byte low1 = data[start + i + 8];

                // 处理字节的每一位
                for (int j = 0; j < 8; j++) {
                    byte l = (byte)((low0 >> (7 - j)) & 1);
                    byte h = (byte)(((low1 >> (7 - j)) & 1) << 1);
                    result[0][index][i*8+j] = (byte)(l | h);
                }
            }

            index++;
            start += 16;
        }

        index = 0;

        while (start < len - 1 && index < 256) {
            // 读8个字节作为图案的 0位
            // 读8个字节作为图案的 1位
            for (int i = 0; i < 8; i++) {
                byte low0 = data[start + i];
                byte low1 = data[start + i + 8];

                // 处理字节的每一位
                for (int j = 0; j < 8; j++) {
                    byte l = (byte)((low0 >> (7 - j)) & 1);
                    byte h = (byte)(((low1 >> (7 - j)) & 1) << 1);
                    result[1][index][i*8+j] = (byte)(l | h);
                }
            }

            index++;
            start += 16;
        }

        return result;
    }

    /**
     * 获取命名表
     * 0x2000 - 0x2FFF  共 4KB
     * 分为4块，NES主机提供 2KB，卡带提供另外2KB或者不提供
     * @return
     */
    public byte[][] getNameTable() {
        byte[][] result = new byte[2][1024];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 1024; j++) {
                result[i][j] = ppuRead(0x2000 + 0x400 * i + j);
            }
        }
        return result;
    }

    public byte[] getPalette() {
        byte[] result = new byte[2 * 16];
        for (int i = 0; i < 32; i++) {
            result[i] = ppuRead(0x3F00 + i);
        }

        return result;
    }

    /**
     * CPU 与 PPU之间的通信通过 0x2000 - 0x2007的 8个 寄存器 来实现
     * @param address 写入地址 16bit
     * @param data 要写入的数据 8bit
     */
    @Override
    public void cpuWrite(int address, byte data) {
        switch (address) {
            case 0x0000:
                setPpuCtrlValue(data);
                break;
            case 0x0001:
                setPpuMaskValue(data);
                break;
            case 0x0002:
                setPpuStatusValue(data);
                break;
            case 0x0003:
                oamAddress = data;
                break;
            case 0x0006: // PPU Address
                // CPu 和 PPU在不同的总线上，无法直接访问内存，通过 0x0006和0x0007来实现内存的读写
                // PPU的地址为16位，PPU Data寄存器是8位，需要进行两次写入才能设置地址
                // 先写入高地址 再写入低地址
                if (isFirstPpuAddress) {
                    tmpPpuAddress = (tmpPpuAddress & 0x00FF) | (data << 8);
                    isFirstPpuAddress = false;
                } else {
                    tmpPpuAddress = (tmpPpuAddress & 0xFF00) | data;
                    isFirstPpuAddress = true;
                }
                break;
            case 0x0007: // PPU Data
                ppuWrite(tmpPpuAddress, data);
                tmpPpuAddress++;
                break;
        }
    }

    private int tmpPpuAddress = 0x00;
    private boolean isFirstPpuAddress = true;
    private byte ppuDataBuffer = 0x00;

    @Override
    public byte cpuRead(int address) {
        byte data = 0x00;

        switch (address) {
            case 0x0000:
                break;
            case 0x0001:
                break;
            case 0x0002:
                // 暂时写死 使程序能往下运行
                setPpuStatus(PPUStatus.VBlank, 1);

                // ppu status 只有前三位有效
                data = (byte)((ppuStatus & 0xE0) | (ppuDataBuffer & 0x1F));
                setPpuStatus(PPUStatus.VBlank, 0);
                isFirstPpuAddress = true;
                break;
            case 0x0003:
                break;
            case 0x0006: // PPU Address
                break;
            case 0x0007: // PPU Data
                // CPU 从 PPU读取数据 要慢一个 ppuRead
                data = ppuDataBuffer;

                ppuDataBuffer = ppuRead(tmpPpuAddress);

                // 读取Palettes没有延迟
                if (tmpPpuAddress > 0x3F00) {
                    data = ppuDataBuffer;
                }

                tmpPpuAddress++;
                break;
        }

        return data;
    }

    public void ppuWrite(int address, byte data) {
        address &= 0x3FFF;

        if (address >= 0x2000 && address <= 0x3EFF) {
            // Name Tables 实际地址 0x2000 - 0x2FFF 其余是Mirror
            // 最多有4个Name Table
            address &= 0x0FFF;
            if (this.cartridge.header.mirrorFlag) // vertical mirror
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
            if (address == 0x0010) address = 0x0000;
            if (address == 0x0014) address = 0x0004;
            if (address == 0x0018) address = 0x0008;
            if (address == 0x001C) address = 0x000C;
            palette[address] = data;
        }
    }

    public byte ppuRead(int address) {
        address &= 0x3FFF;

        return 0;
    }
}
