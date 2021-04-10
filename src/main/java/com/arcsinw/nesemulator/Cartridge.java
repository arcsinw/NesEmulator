package com.arcsinw.nesemulator;

import java.io.IOException;
import java.io.InputStream;

/**
 * 卡带
 * http://fms.komkon.org/EMUL8/NES.html
 */
public class Cartridge implements CPUBusDevice{

    private final String INVALID_NES_ROM_MESSAGE = "非法的nes文件";

    public enum Mirror {
        Horizontal(0),
        Vertical(1);

        private int key;

        Mirror(int k) {
            key = k;
        }
    }

    /**
     * 文件头，16字节
     */
    class Header {
        private final byte[] NES_MAGIC_STRING = new byte[] {0x4E, 0x45, 0x53, 0x1A};

        /**
         * Number of 16kB ROM banks, also named PRG
         */
        int prgBanksCount;

        /**
         * Number of 8kB VROM banks, also named CHR
         */
        int chrBanksCount;

        /**
         * Id of mapper
         */
        int mapperNo;

        boolean trainerFlag;

        /**
         * Mirroring: 0: horizontal (vertical arrangement) (CIRAM A10 = PPU A11)
         *            1: vertical (horizontal arrangement) (CIRAM A10 = PPU A10)
         */
        Mirror mirror;



        public Header(byte[] bytes) {
            int length = bytes.length;
            if (length != 16) return;

            // 检查文件头是否为NES_MAGIC_STRING
            for (int i = 0; i < 4; i++) {
                check(bytes[i] == NES_MAGIC_STRING[i], INVALID_NES_ROM_MESSAGE);
            }

            prgBanksCount = bytes[4] & 0xFF;
            chrBanksCount = bytes[5] & 0xFF;

            mapperNo = ((bytes[7] & 0xF0) | (bytes[6] >>> 4)) & 0x0FFF; // 确保是正数

            mirror = (bytes[6] & 0x01) != 0 ? Mirror.Vertical : Mirror.Horizontal;
            trainerFlag = (bytes[6] & 0x04) != 0;
        }

        @Override
        public String toString() {
            return "Header{ " +
                    "romBanksCount=" + prgBanksCount +
                    ", vromBanksCount=" + chrBanksCount +
                    ", mapperNo=" + mapperNo +
                    ", trainerFlag=" + trainerFlag +
                    ", mirrorFlag=" + mirror.key +
                    " }";
        }
    }

    Header header;

    byte[] trainer;

    /**
     * Program
     */
    byte[] prg;

    /**
     * Character
     */
    public byte[] chr;

    /**
     * SRAM
     * 0x6000 - 0x7FFF
     */
    byte[] sram;

    void check(boolean result, String errorMessage) {
        if (!result) {
            throw new Error(errorMessage);
        }
    }

    @Override
    public void cpuWrite(int address, byte data) {
        if (address >= 0x8000 && address <= 0xFFFF) {
            prg[address & (header.prgBanksCount == 1 ? 0x3FFF : 0x7FFF)] = data;
        }
    }

    /**
     * CPU 读写 的是 RPG部分
     * @param address 数据地址
     * @return
     */
    @Override
    public byte cpuRead(int address) {
        // if PRGROM is 16KB
        //     CPU Address Bus          PRG ROM
        //     0x8000 -> 0xBFFF: Map    0x0000 -> 0x3FFF
        //     0xC000 -> 0xFFFF: Mirror 0x0000 -> 0x3FFF
        // if PRGROM is 32KB
        //     CPU Address Bus          PRG ROM
        //     0x8000 -> 0xFFFF: Map    0x0000 -> 0x7FFF
        if (address >= 0x8000 && address <= 0xFFFF) {
            return prg[address & (header.prgBanksCount == 1 ? 0x3FFF : 0x7FFF)];
        }

        return 0x00;
    }

    /**
     *
     * @param address
     * @param data
     */
    public void ppuWrite(int address, byte data) {
        // Pattern Table  0x0000 - 0x1FFF
        if (address >= 0x0000 && address <= 0x1FFF) {
            chr[address] = data;
        }
    }

    /**
     *
     * @param address
     * @return
     */
    public byte ppuRead(int address) {
        if (address >= 0x0000 && address <= 0x1FFF) {
            return chr[address];
        }

        return 0x00;
    }

    public Cartridge(InputStream inputStream) throws IOException {
        byte[] headerBytes = new byte[16];

        inputStream.read(headerBytes, 0, 16);
        header = new Header(headerBytes);

        if (header.trainerFlag) {
            trainer = new byte[512];
            inputStream.read(trainer, 0, 512);
        }

        int prgCount = header.prgBanksCount;
        prg = new byte[prgCount * 16384];
        for (int i = 0; i < prgCount; i++) {
            inputStream.read(prg, i * 16384, 16384);
        }

        int chrCount = header.chrBanksCount;
        chr = new byte[chrCount * 8192];
        for (int i = 0; i < chrCount; i++) {
            inputStream.read(chr, i * 8192, 8192);
        }
    }

    public Cartridge(String filePath) throws IOException {
        InputStream inputStream = Cartridge.class.getResourceAsStream(filePath);
//        URL url = Cartridge.class.getResource("/");
//        System.out.println(url.getPath());
        byte[] headerBytes = new byte[16];

        inputStream.read(headerBytes, 0, 16);
        header = new Header(headerBytes);

        if (header.trainerFlag) {
            trainer = new byte[512];
            inputStream.read(trainer, 0, 512);
        }

        int prgCount = header.prgBanksCount;
        prg = new byte[prgCount * 16384];
        for (int i = 0; i < prgCount; i++) {
            inputStream.read(prg, i * 16384, 16384);
        }

        int chrCount = header.chrBanksCount;
        chr = new byte[chrCount * 8192];
        for (int i = 0; i < chrCount; i++) {
            inputStream.read(chr, i * 8192, 8192);
        }
    }
}


