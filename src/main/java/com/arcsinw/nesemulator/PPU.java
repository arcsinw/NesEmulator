package com.arcsinw.nesemulator;

public class PPU {



    private byte[][][] patternTable = new byte[2][256][64];

    /**
     * 获取图案表
     * 每个图案8x8个像素，使用16字节
     * 图案表分为 背景图案表 和 精灵图案表，各256个图案
     * @return
     */
    public byte[][][] getPatternTable(byte[] data) {
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
}
