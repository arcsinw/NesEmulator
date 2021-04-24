package com.arcsinw.nesemulator;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;
import java.util.stream.IntStream;

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

        PPUCtrl(int m) {
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

        PPUMask(int m) {
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

        PPUStatus(int m) {
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
     * OAM 的读写地址 无符号型byte $2003
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

    public void setPpuCtrlValue(int v) {
        ppuCtrl = (byte) (v & 0x00FF);
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

    public void setPpuMaskValue(int v) {
        ppuMask = (byte) (v & 0x00FF);
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

    public void setPpuStatusValue(int v) {
        ppuStatus = (byte) (v & 0x00FF);
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
    private boolean logging = false;

    // endregion

    // region 帧渲染完成事件

    public interface FrameRenderCompletedEventListener extends EventListener {
        void notifyFrameRenderCompleted();
    }

    private ArrayList<FrameRenderCompletedEventListener> frameRenderCompletedEventListeners = new ArrayList<>();

    public void addFrameRenderCompletedEventListener(FrameRenderCompletedEventListener listener) {
        frameRenderCompletedEventListeners.add(listener);
    }

    public void notifyFrameRenderCompleted() {
        for (FrameRenderCompletedEventListener l : frameRenderCompletedEventListeners) {
            l.notifyFrameRenderCompleted();
        }
    }

    // endregion

    public PPU() {
        IntStream.range(0, 64).forEach(i -> oam[i] = new OAMEntry());
    }

    /**
     * NES屏幕分辨率 256x240
     * 每个像素点使用 4 bit 作为颜色索引
     */
    byte[][] screen = new byte[240][256];

    public static class OAMEntry {
        /**
         * Y position of top of sprite
         * 1 字节（无符号）
         */
        public int y;

        /**
         * Tile index number
         * 8x8模式             id为tile的id
         * 8x16模式(两个tile)
         * xxxxxxx y          8 bit
         * ||||||| +--------- Pattern table的地址，1 $1000, 0 $0000
         * +++++++----------- Sprite上半部分tile id
         * 1 字节（无符号）
         */
        public int id;

        /**
         * Attributes
         * 定义Sprite渲染的方式
         * yyy uuu xx         8 bit
         * ||| ||| ++-------- Sprite颜色的高2位
         * ||| +++----------- unused
         * ||+--------------- Sprite优先级，0 显示在背景前面，1显示在背景后面
         * |+---------------- 为1时水平翻转Sprite
         * +----------------- 为1时垂直翻转Sprite
         */
        public byte attribute;

        /**
         * X position of left side of sprite.
         * 1 字节（无符号）
         */
        public int x;

        public OAMEntry(byte y, byte id, byte attribute, byte x) {
            this.y = y & 0x00FF;
            this.id = id & 0x00FF;
            this.attribute = attribute;
            this.x = x & 0x00FF;
        }

        public OAMEntry() {
            x = 0xFF;
            y = 0xFF;
            id = 0xFF;
            attribute = (byte) 0xFF;
        }

        public OAMEntry(OAMEntry entry) {
            this.y = entry.y;
            this.id = entry.id;
            this.attribute = entry.attribute;
            this.x = entry.x;
        }

        @Override
        public String toString() {
            return String.format("(% 4d, % 4d)      ID: %02X       AT: %02X", x, y, id, attribute);
        }
    }

    /**
     * OAM (Object Attribute Memory)
     * 64 * 4 = 256字节
     */
    public OAMEntry[] oam = new OAMEntry[64];

    /**
     * 按字节设置 OAMEntry对象数组
     * @param address 0 ~ 255
     * @param data
     */
    public void setOAMEntry(int address, int data) {
        switch(address % 4) {
            case 0:
                oam[address / 4].y = data & 0x00FF;
                break;
            case 1:
                oam[address / 4].id = data & 0x00FF;
                break;
            case 2:
                oam[address / 4].attribute = getUnsignedByte(data);
                break;
            case 3:
                oam[address / 4].x = data & 0x00FF;
                break;
            default:
                break;
        }
    }

    /**
     * 按字节获取 OAMEntry对象数组的属性
     * @param address 0 ~ 255
     */
    public byte getOAMEntry(int address) {
        byte data = 0x00;
        switch(address % 4) {
            case 0:
                data = (byte) oam[address / 4].y;
                break;
            case 1:
                data = (byte) oam[address / 4].id;
                break;
            case 2:
                data = oam[address / 4].attribute;
                break;
            case 3:
                data = (byte) oam[address / 4].x;
                break;
            default:
                break;
        }

        return data;
    }

    /**
     * 一行扫描线上的Sprite
     */
    private List<OAMEntry> scanLineSprite = new ArrayList<>(8);
//    private OAMEntry[] scaneLineSprite = new OAMEntry[8];
    private byte spriteCount = 0;

    private byte[] spritePatternShifterLo = new byte[8];

    private byte[] spritePatternShifterHi = new byte[8];

    /**
     * 为方便实现 卷轴滚动 虚拟的寄存器
     * yyy NN YYYYY XXXXX   15 bit
     * ||| || ||||| +++++-- coarse X scroll
     * ||| || +++++-------- coarse Y scroll
     * ||| ++-------------- name table select
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
         * 1 bit, 1 << 10
         */
        byte nameTableX = 0;
        /**
         * 1 bit, 1 << 11
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

//    private LoopyRegister vramAddress = new LoopyRegister();
//    private LoopyRegister tmpVramAddress = new LoopyRegister();

    /**
     * Fine X scroll (3 bits)
     */
    private int fineX = 0x00;

    /**
     * yyy NN YYYYY XXXXX
     * ||| || ||||| +++++-- coarse X scroll (8x8 tile)
     * ||| || +++++-------- coarse Y scroll
     * ||| ++-------------- nametable select
     * +++----------------- fine Y scroll
     */
    private int v = 0;

    /**
     * yyy NN YYYYY XXXXX
     * ||| || ||||| +++++-- coarse X scroll
     * ||| || +++++-------- coarse Y scroll
     * ||| ++-------------- nametable select
     * +++----------------- fine Y scroll
     */
    private int t = 0;

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


    private int getTileAddress(int v) {
        return 0x2000 | (v & 0x0FFF);
    }

    /**
     *  NN 1111 YYY XXX
     *  || |||| ||| +++-- high 3 bits of coarse X (x/4)
     *  || |||| +++------ high 3 bits of coarse Y (y/4)
     *  || ++++---------- attribute offset (960 bytes)
     *  ++--------------- nametable select
     * @param v
     * @return
     */
    private int getAttributeAddress(int v) {
        return 0x23C0 | (v & 0x0C00) | ((v >> 4) & 0x38) | ((v >> 2) & 0x07);
    }

    private int getFineY(int v) {
        return v >>> 12;
    }

    /**
     * PPU的地址总线是14bit，v的最高位未使用
     * @param v
     * @return
     */
    private int getPPUAddress(int v) {
        return v & 0x3FFF;
    }

    /**
     * CPU 与 PPU之间的通信通过 0x2000 - 0x2007的 8个 寄存器 来实现
     * @param address 写入地址 16bit
     * @param data 要写入的数据 8bit
     */
    public void cpuWrite(int address, int data) {
        switch (address) {
            // PPU Control
            case 0x0000:
                setPpuCtrlValue(data);
                t = (t & ~0x0C00) | ((data & 0x03) << 10);
                break;
            // PPU Mask
            case 0x0001:
                setPpuMaskValue(data);
                break;
            // OAM Address
            case 0x0003:
                oamAddress = (data & 0x00FF);
                break;
            // OAM Data
            case 0x0004:
                setOAMEntry(oamAddress, data);
                break;
            // PPU SCROLL
            case 0x0005:
                if (isFirstPpuAddress) {
                    t = (t & ~0x001F) | ((data & 0x00FF) >>> 3);
                    fineX = data & 0x07;
                    isFirstPpuAddress = false;
                } else {
                    t = (t & ~0x7000) | ((data & 0x07) << 12);
//                    t = (t & ~0x03E0) | (((data & 0x00FF) >>> 3) << 5);
                    t = (t & ~0x3E0) | (((data & 0x00FF) & ~7) << 2);
                    isFirstPpuAddress = true;
                }
                break;
            // PPU Address
            case 0x0006:
                /**
                 * CPU 和 PPU在不同的总线上，无法直接访问内存，通过 0x0006和0x0007来实现内存的读写
                 * PPU的地址为16位，PPU Data寄存器是8位，需要进行两次写入才能设置地址
                 * 先写入高地址 再写入低地址
                 */
                if (isFirstPpuAddress) {
                    t = (t & ~0x7F00) | ((data & 0x3F) << 8);
                    isFirstPpuAddress = false;
                } else {
                    if (logging) {
                        tmp.append(String.format(" %02X ", data));
                    }

                    t = (t & ~0xFF) | (data & 0xFF);
                    v = t;
                    isFirstPpuAddress = true;
                }
                break;
            // PPU Data
            case 0x0007:
                ppuWrite(getPPUAddress(v), data);
                v = (v + (getPpuCtrl(PPUCtrl.IncrementMode) == 0 ? 1 : 32)) & 0x3FFF;
                break;
            default:
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
    public byte cpuRead(int address) {
        byte data = 0x00;

        switch (address) {
            // PPU Status
            case 0x0002:
                // ppu status 只有前三位有效
                data = (byte) ((ppuStatus & 0xE0) | (ppuDataBuffer & 0x1F));
                setPpuStatus(PPUStatus.VBlank, 0);
                isFirstPpuAddress = true;
                break;
            // OAM Data
            case 0x0004:
                data = getOAMEntry(oamAddress);
                break;
            // PPU Data
            case 0x0007:
                // CPU 从 PPU读取数据 要慢一个 ppuRead
                data = ppuDataBuffer;
//                    ppuDataBuffer = ppuRead(tmpPpuAddress);
//                ppuDataBuffer = ppuRead(vramAddress.getValue());
                ppuDataBuffer = ppuRead(getPPUAddress(v));

                // 读取Palettes没有延迟
//                    if (tmpPpuAddress >= 0x3F00) {
//                if (vramAddress.getValue() >= 0x3F00) {
//                    data = ppuDataBuffer;
//                }
                if (getPPUAddress(v) >= 0x3F00) {
                    data = ppuDataBuffer;
                }

//                    tmpPpuAddress += (getPpuCtrl(PPUCtrl.IncrementMode) == 0 ? 1 : 32);
                v = (v + (getPpuCtrl(PPUCtrl.IncrementMode) == 0 ? 1 : 32)) & 0x3FFF;
//                vramAddress.setValue(vramAddress.getValue() + (getPpuCtrl(PPUCtrl.IncrementMode) == 0 ? 1 : 32));
                break;
            default:
                break;
        }

        return data;
    }

    public void ppuWrite(int address, int data) {
        address &= 0x3FFF;

        if (address >= 0x0000 && address <= 0x1FFF) {
            // Pattern table
            patternTable[(address & 0x1000) >>> 12][address & 0x0FFF] = getUnsignedByte(data);
        }
        if (address >= 0x2000 && address <= 0x3EFF) {
            // Name Tables 实际地址 0x2000 - 0x2FFF 其余是Mirror（只Mirror了0x2000 - 0x2EFF）
            // 最多有4个Name Table
            address &= 0x0FFF;
            if (this.cartridge.header.mirror == Cartridge.Mirror.Vertical) // vertical mirror
            {
                // Vertical
                if (address >= 0x0000 && address <= 0x03FF) {
                    nameTable[0][address & 0x03FF] = getUnsignedByte(data);
                }
                if (address >= 0x0400 && address <= 0x07FF) {
                    nameTable[1][address & 0x03FF] = getUnsignedByte(data);
                }
                if (address >= 0x0800 && address <= 0x0BFF) {
                    nameTable[0][address & 0x03FF] = getUnsignedByte(data);
                }
                if (address >= 0x0C00 && address <= 0x0FFF) {
                    nameTable[1][address & 0x03FF] = getUnsignedByte(data);
                }
            }
            else // horizontal mirror
            {
                // Horizontal
                if (address >= 0x0000 && address <= 0x03FF) {
                    nameTable[0][address & 0x03FF] = getUnsignedByte(data);
                }
                if (address >= 0x0400 && address <= 0x07FF) {
                    nameTable[0][address & 0x03FF] = getUnsignedByte(data);
                }
                if (address >= 0x0800 && address <= 0x0BFF) {
                    nameTable[1][address & 0x03FF] = getUnsignedByte(data);
                }
                if (address >= 0x0C00 && address <= 0x0FFF) {
                    nameTable[1][address & 0x03FF] = getUnsignedByte(data);
                }
            }
        }
        else if (address >= 0x3F00 && address <= 0x3FFF) {
            // Palettes 真实地址 0x3F00 - 0x3F1F 剩下的是Mirrors
            address &= 0x001F;  // % 32

            if (address == 0x0010 || address == 0x0014 || address == 0x0018 || address == 0x001C) {
                palette[address - 0x10] = getUnsignedByte(data);
            }

            palette[address] = getUnsignedByte(data);
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

    // region 背景滚动相关字段

    /**
     * 扫描线编号,1帧有262条扫描线(0 - 261)
     * 0 - 239 可见扫描线
     * 240 post-render line
     * 241 - 260 vertical blank lines
     * 261 pre-render line
     */
    private int scanLine = 0;

    /**
     * 每根扫描线有 341 cycles, 0 - 340
     * 每个cycle 渲染一个像素
     * 0         空闲
     * 1   - 256 加载tile数据
     * 257 - 320 加载下一条扫描线的Sprite数据
     * 321 - 336 加载下一条扫描线的前2个tile
     * 337 - 340 加载两字节的Name table数据
     */
    private int cycles = 0;

    /**
     * 已渲染的帧数
     */
    private int frames = 0;

    public boolean nmi = false;

    private int nextBackgroundTileId = 0x00;
    private byte nextBackgroundTileAttribute = 0x00;

    /**
     * 暂存下一个背景tile的Pattern 高8位
     */
    private byte nextBackgroundTilePatternHi = 0x00;

    /**
     * 暂存下一个背景tile的Pattern 低8位
     */
    private byte nextBackgroundTilePatternLo = 0x00;


    /**
     * 存储下一个背景tile的Pattern 高8位，每经过一个cycle，寄存器左移1位
     */
    private short backgroundPatternShifterHi = 0x0000;

    /**
     * 存储下一个背景tile的Pattern 低8位，每经过一个cycle，寄存器左移1位
     */
    private short backgroundPatternShifterLo = 0x0000;


    private short backgroundAttributeLoShifter = 0x0000;
    private short backgroundAttributeHiShifter = 0x0000;

    // endregion

    /**
     * 将扫描线上下一个tile的数据加载到 Shifter中
     */
    public void loadBackgroundShifters() {
        backgroundPatternShifterHi = (short) ((backgroundPatternShifterHi & 0xFF00) | nextBackgroundTilePatternHi);
        backgroundPatternShifterLo = (short) ((backgroundPatternShifterLo & 0xFF00) | nextBackgroundTilePatternLo);

        backgroundAttributeHiShifter = (short) ((backgroundAttributeHiShifter & 0xFF00) | ((nextBackgroundTileAttribute & 0x02) != 0 ? 0xFF : 0x00));
        backgroundAttributeLoShifter = (short) ((backgroundAttributeLoShifter & 0xFF00) | ((nextBackgroundTileAttribute & 0x01) != 0 ? 0xFF : 0x00));
    }

    /**
     * 将所有的Shifter寄存器左移1位
     */
    public void updateShifters() {
        if ((getPpuMask(PPUMask.BackgroundEnable) != 0) || scanLine == 261) {
            backgroundAttributeHiShifter <<= 1;
            backgroundAttributeLoShifter <<= 1;

            backgroundPatternShifterHi <<= 1;
            backgroundPatternShifterLo <<= 1;
        }

        // 更新Sprite相关Shifter
        if ((getPpuMask(PPUMask.SpriteEnable) != 0) && cycles >= 1 && cycles < 258) {
            for (int i = 0; i < scanLineSprite.size(); i++) {
                if (scanLineSprite.get(i).x > 0) {
                    scanLineSprite.get(i).x--;
                } else {
                    spritePatternShifterHi[i] <<= 1;
                    spritePatternShifterLo[i] <<= 1;
                }
            }
        }
    }

    public void incrementScrollX() {
        if ((getPpuMask(PPUMask.BackgroundEnable)) != 0 || (getPpuMask(PPUMask.SpriteEnable) != 0)) {
            if ((v & 0x001F) == 31) { // if coarse X == 31
                v &= ~0x001F;          // coarse X = 0
                v ^= 0x0400;           // switch horizontal nametable
            } else {
                v += 1;                // increment coarse X
            }
        }
    }

    public void incrementScrollY() {
        if ((getPpuMask(PPUMask.BackgroundEnable)) != 0 || (getPpuMask(PPUMask.SpriteEnable) != 0)) {
            if ((v & 0x7000) != 0x7000) {       // if fine Y < 7
                v += 0x1000;                      // increment fine Y
            } else {
                v &= ~0x7000;                     // fine Y = 0
                int y = (v & 0x03E0) >> 5;        // let y = coarse Y
                if (y == 29) {
                    y = 0;                          // coarse Y = 0
                    v ^= 0x0800;                    // switch vertical nametable
                } else if (y == 31) {
                    y = 0;                          // coarse Y = 0, nametable not switched
                } else {
                    y += 1; // increment coarse Y
                }
                v = (v & ~0x03E0) | (y << 5);     // put coarse Y back into v
            }
        }
    }

    public void transferAddressX() {
        if ((getPpuMask(PPUMask.BackgroundEnable)) != 0 || (getPpuMask(PPUMask.SpriteEnable) != 0)) {
            v = (v & ~0x041F) | (t & 0x041F);
        }
    }

    public void transferAddressY() {
        if ((getPpuMask(PPUMask.BackgroundEnable)) != 0 || (getPpuMask(PPUMask.SpriteEnable) != 0)) {
            v = (v & ~0x7BE0) | (t & 0x7BE0);
        }
    }

    /**
     * 反转字节的比特位
     * @param b
     * @return
     */
    public byte reverseByte(byte b) {
        b = (byte) (((b & 0xF0) >> 4) | ((b & 0x0F) << 4));
        b = (byte) (((b & 0xCC) >> 2) | ((b & 0x33) << 2));
        b = (byte) (((b & 0xAA) >> 1) | ((b & 0x55) << 1));
        return b;
    }

    private boolean spriteZeroHitPossible = false;
    private boolean spriteZeroRendering = false;


    /**
     * 参考http://wiki.nesdev.com/w/images/4/4f/Ppu.svg
     * 262条扫描线 （0~261）  (0~239)是可见扫描线
     * 每条扫描线341 cycles (0~340)
     */
    public void clock() {
        // 可视区域，每个cycle更新1个像素
        // 扫描线(0, 239) cycles(1, 256)
        if (scanLine < 240 && cycles >= 1 && cycles <= 256) {
            // background render
            byte backgroundPixel = 0x00, backgroundPalette = 0x00;

            if (getPpuMask(PPUMask.BackgroundEnable) != 0) {
                // 二进制中的 1 表明当前渲染的像素位置
                short shifterMask = (short) (0x8000 >>> fineX);

                byte colorBit0 = (byte) ((backgroundPatternShifterLo & shifterMask) != 0 ? 1 : 0);
                byte colorBit1 = (byte) ((backgroundPatternShifterHi & shifterMask) != 0 ? 1 : 0);

                backgroundPixel = (byte) (colorBit0 | (colorBit1 << 1));

                byte colorBit2 = (byte) ((backgroundAttributeLoShifter & shifterMask) != 0 ? 1 : 0);
                byte colorBit3 = (byte) ((backgroundAttributeHiShifter & shifterMask) != 0 ? 1 : 0);

                backgroundPalette = (byte) (colorBit2 | (colorBit3 << 1));
            }

            // foreground render
            byte foregroundPixel = 0x00, foregroundPalette = 0x00, foregroundPriority = 0x00;

            if (getPpuMask(PPUMask.SpriteEnable) != 0) {
                spriteZeroRendering = false;

                for (int i = 0; i < scanLineSprite.size(); i++) {
                    if (scanLineSprite.get(i).x == 0) {
                        byte pixelLo = (byte) ((spritePatternShifterLo[i] & 0x80) != 0 ? 1 : 0);
                        byte pixelHi = (byte) ((spritePatternShifterHi[i] & 0x80) != 0 ? 1 : 0);

                        foregroundPixel = (byte) ((pixelHi << 1) | pixelLo);

                        foregroundPalette = (byte) ((scanLineSprite.get(i).attribute & 0x03) + 0x04);
                        foregroundPriority = (byte) ((scanLineSprite.get(i).attribute & 0x20) != 0 ? 1 : 0);

                        if (foregroundPixel != 0) {
                            if (i == 0) {
                                spriteZeroRendering = true;
                            }

                            // 在scanLineSprite中，靠前的Sprite优先级高
                            break;
                        }
                    }
                }
            }

            // 混合前景色和背景色
            byte pixel = backgroundPixel, palette = backgroundPalette;
//
//            pixel = foregroundPixel;
//            palette = foregroundPalette;

            if (foregroundPixel != 0 && foregroundPriority == 0) {
                pixel = foregroundPixel;
                palette = foregroundPalette;
            }

            // Sprite Zero Hit Detection
            if (spriteZeroRendering && spriteZeroHitPossible) {
                if (getPpuMask(PPUMask.BackgroundEnable) != 0 && getPpuMask(PPUMask.SpriteEnable) != 0) {
                    if (getPpuMask(PPUMask.BackgroundLeftEnable) == 0 && getPpuMask(PPUMask.SpriteLeftEnable) == 0) {
                        if (cycles >= 9 && cycles < 258) {
                            setPpuStatus(PPUStatus.SpriteZeroHit, 1);
                        }
                    } else {
                        if (cycles >= 1 && cycles < 258) {
                            setPpuStatus(PPUStatus.SpriteZeroHit, 1);
                        }
                    }
                }
            }

            screen[scanLine][cycles - 1] = getColorFromPalette(palette, pixel);
        }

        // 处理可视扫描线和pre-render扫描线中的 8个cycle的循环
        if ((scanLine < 240) || (scanLine == 261)) {
            // Background Render
            if (scanLine == 0 && cycles == 0) {
                cycles = 1;
            }

            if ((cycles >= 1 && cycles <= 256) || (cycles >= 321 && cycles <= 336)) {
//            if ((cycles >= 2 && cycles < 258) || (cycles >= 321 && cycles < 338)) {
                updateShifters();

                // 8个cycle一个周期
                // |NT byte|AT byte|   Lo  |hi  inc hori(v)|
                // | 1 | 2 | 3 | 4 | 5 | 6 |   7   |   8   |
                switch (cycles % 8) {
//                switch ((cycles - 1) % 8) {
                    case 1:
                        loadBackgroundShifters();

                        // 加载Name table
                        nextBackgroundTileId = ppuRead(getTileAddress(v)) & 0x00FF;

                        if (nextBackgroundTileId > 256) System.out.println(nextBackgroundTileId);
//                        nextBackgroundTileId = ppuRead(0x2000 + (vramAddress.getValue() & 0x0FFF));
                        break;
                    case 3:
                        // 读取Attribute table中的1字节（Attribute table中1字节控制一个4x4tile 的大Tile的颜色）
                        // 通过nameTableX,Y 计算tile所在的Name table
                        // 通过coarseX,Y 计算tile所在的 4x4tile 的大Tile id
                        // 使用大Tile id获取attribute
//                        nextBackgroundTileAttribute = ppuRead(0x23C0 | (vramAddress.nameTableY << 11) |
//                                (vramAddress.nameTableX << 10) |
//                                (vramAddress.coarseX >>> 2) |
//                                ((vramAddress.coarseY >>> 2) << 3));

                        nextBackgroundTileAttribute = ppuRead(getAttributeAddress(v));

                        if (((v >>> 5) & 0x02) != 0) {
                            nextBackgroundTileAttribute >>>= 4;
                        }

                        if ((v & 0x02) != 0) {
                            nextBackgroundTileAttribute >>>= 2;
                        }

                        // 从Attribute Table的1字节中选出2 bit

//                        if ((vramAddress.coarseY & 0x02) != 0) {
//                            nextBackgroundTileAttribute >>>= 4;
//                        }
//
//                        if ((vramAddress.coarseX & 0x02) != 0) {
//                            nextBackgroundTileAttribute >>>= 2;
//                        }

                        nextBackgroundTileAttribute &= 0x03;
                        break;
                    case 5:
                        // 读取Pattern table低字节
                        nextBackgroundTilePatternLo = ppuRead((getPpuCtrl(PPUCtrl.BackgroundSelect) << 12) +
                                (nextBackgroundTileId << 4) + getFineY(v));
                        break;
                    case 7:
                        // 读取Pattern table高字节
                        nextBackgroundTilePatternHi = ppuRead((getPpuCtrl(PPUCtrl.BackgroundSelect) << 12) +
                                (nextBackgroundTileId << 4) + getFineY(v) + 8);
                        break;
                    case 0:
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

            // Foreground Render

            // evaluation

            if (scanLine < 240 && cycles == 257) {
                // clear Sprite OAM
                scanLineSprite.clear();
                spriteCount = 0;

                for (int i = 0; i < 8; i++) {
                    spritePatternShifterLo[i] = 0;
                    spritePatternShifterHi[i] = 0;
                }

                // 获取当前扫描线会经过的Sprite (一条扫描线上最多能渲染8个Sprite，超过8时设置SpriteOverflow)
                int index = 0;
                spriteZeroHitPossible = false;
                while (index < 64 && spriteCount < 9) {
                    int diff = scanLine - oam[index].y;
                    if (diff >= 0 && diff < (getPpuCtrl(PPUCtrl.SpriteSize) == 1 ? 16 : 8)) {
                        if (spriteCount < 8) {
                            if (index == 0) {
                                spriteZeroHitPossible = true;
                            }

                            scanLineSprite.add(new OAMEntry(oam[index]));
                            spriteCount++;
                        }
                    }

                    index++;
                }

                setPpuStatus(PPUStatus.SpriteOverflow, (spriteCount > 8 ? 1 : 0));
            }

            if (cycles == 340) {
                for (int i = 0; i < spriteCount; i++) {
                    byte spritePatternBitsLo = 0, spritePatternBitsHi = 0;
                    int spritePatternAddressLo = 0, spritePatternAddressHi = 0;

                    // 8x8
                    if (getPpuCtrl(PPUCtrl.SpriteSize) == 0) {
                        // 垂直翻转Sprite
                        if ((scanLineSprite.get(i).attribute & 0x80) != 0) {
                            spritePatternAddressLo = (getPpuCtrl(PPUCtrl.SpriteSelect) << 12)
                                    | (scanLineSprite.get(i).id << 4)
                                    | (7 - (scanLine - scanLineSprite.get(i).y));
                        } else {
                            spritePatternAddressLo = (getPpuCtrl(PPUCtrl.SpriteSelect) << 12)
                                    | (scanLineSprite.get(i).id << 4)
                                    | (scanLine - scanLineSprite.get(i).y);
                        }
                    } else { // 8x16，由两个编号连续的tile组成，上半部分tile id为x，则下半部分tile id为x+1
                        // 垂直翻转Sprite
                        if ((scanLineSprite.get(i).attribute & 0x80) != 0) {
                            // Sprite的上半部分
                            if (scanLine - scanLineSprite.get(i).y < 8) {
                                spritePatternAddressLo = ((scanLineSprite.get(i).id & 0x01) << 12)
                                        | (((scanLineSprite.get(i).id & 0xFE) + 1) << 4)
                                        | (7 - ((scanLine - scanLineSprite.get(i).y) & 0x07));
                            } else {
                                spritePatternAddressLo = ((scanLineSprite.get(i).id & 0x01) << 12)
                                        | ((scanLineSprite.get(i).id & 0xFE) << 4)
                                        | (7 - ((scanLine - scanLineSprite.get(i).y) & 0x07));
                            }
                        } else {
                            // 不垂直翻转
                            if (scanLine - scanLineSprite.get(i).y < 8) {
                                spritePatternAddressLo = ((scanLineSprite.get(i).id & 0x01) << 12)
                                        | ((scanLineSprite.get(i).id & 0xFE) << 4)
                                        | ((scanLine - scanLineSprite.get(i).y) & 0x07);

                            } else {
                                spritePatternAddressLo = ((scanLineSprite.get(i).id & 0x01) << 12)
                                        | (((scanLineSprite.get(i).id & 0xFE) + 1) << 4)
                                        | ((scanLine - scanLineSprite.get(i).y) & 0x07);
                            }
                        }
                    }

                    spritePatternAddressHi = spritePatternAddressLo + 8;
                    spritePatternBitsHi = ppuRead(spritePatternAddressHi);
                    spritePatternBitsLo = ppuRead(spritePatternAddressLo);

                    // 水平翻转
                    if ((scanLineSprite.get(i).attribute & 0x40) != 0) {
                        spritePatternBitsHi = reverseByte(spritePatternBitsHi);
                        spritePatternBitsLo = reverseByte(spritePatternBitsLo);
                    }

                    spritePatternShifterLo[i] = spritePatternBitsLo;
                    spritePatternShifterHi[i] = spritePatternBitsHi;
                }
            }
        }


        // Post-render line, do nothing
        if (scanLine == 240) {}

        if (scanLine == 241 && cycles == 1) {
            setPpuStatus(PPUStatus.VBlank, 1);

            // 在VBlank时 触发CPU中断
            if (getPpuCtrl(PPUCtrl.NmiEnable) == 1) {
                nmi = true;
            }
        }

        // pre-render scaneline, end of vblank
        if (scanLine == 261) {
            if (cycles == 1) {
                setPpuStatus(PPUStatus.VBlank, 0);
                setPpuStatus(PPUStatus.SpriteOverflow, 0);
                setPpuStatus(PPUStatus.SpriteZeroHit, 0);

                IntStream.range(0, 8).forEach(i -> {
                    spritePatternShifterLo[i] = 0;
                    spritePatternShifterHi[i] = 0;
                });
            }
            else if (cycles >= 280 && cycles <= 304) {
                transferAddressY();
            }
        }

        if (cycles == 338 || cycles == 340) {
//            nextBackgroundTileId = ppuRead(0x2000 | (vramAddress.getValue() & 0x0FFF));
            nextBackgroundTileId = ppuRead(0x2000 | (getPPUAddress(v) & 0x0FFF));
        }

        cycles++;
        if (cycles > 340) {
            cycles = 0;
            scanLine++;

            if (scanLine > 261) {
                scanLine = 0;
                frames++;

                notifyFrameRenderCompleted();
            }
        }
    }

    public void reset() {
        isFirstPpuAddress = true;
        ppuDataBuffer = 0x00;
        scanLine = 240;
        cycles = 340;
        frames = 0;

        fineX = 0;
        nextBackgroundTileAttribute = 0x00;
        nextBackgroundTilePatternHi = 0x00;
        nextBackgroundTilePatternLo = 0x00;
        nextBackgroundTileId = 0x00;

        backgroundAttributeHiShifter = 0x00;
        backgroundAttributeLoShifter = 0x00;
        backgroundPatternShifterHi = 0x00;
        backgroundPatternShifterLo = 0x00;

        ppuStatus = 0;
        ppuCtrl = 0;
        ppuMask = 0;

        v = 0;
        t = 0;
    }

    private byte getUnsignedByte(int value) {
        return (byte) (value & 0x00FF);
    }
}
