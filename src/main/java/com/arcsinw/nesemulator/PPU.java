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
     * Name Table, 命名表
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
                result[i][j] = cpuRead(0x2000 + 0x400 * i + j);
            }
        }
        return result;
    }

    @Override
    public void cpuWrite(int address, byte data) {
        switch (address) {

        }
    }

    @Override
    public byte cpuRead(int address) {
        return 0;
    }

    public void ppuWrite(int address, byte data) {

    }

    public byte ppuRead(int address) {
        return 0;
    }
}
