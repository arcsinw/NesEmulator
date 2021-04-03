package com.arcsinw.nesemulator;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 *
 * @See http://fms.komkon.org/EMUL8/NES.html#LABM
 */
public class NesRom {

    private final String INVALID_NES_ROM_MESSAGE = "非法的nes文件";

    void check(boolean result, String errorMessage) {
        if (!result) {
            throw new Error(errorMessage);
        }
    }

    /**
     * 16字节
     */
    class Header {
        private final byte[] NES_MAGIC_STRING = new byte[] {0x4E, 0x45, 0x53, 0x1A};

        /**
         * Number of 16kB ROM banks, also named PRG
         */
        int romBanksCount;

        /**
         * Number of 8kB VROM banks, also named CHR
         */
        int vromBanksCount;

        /**
         * Number of mapper
         */
        int mapperNo;

        boolean trainerFlag;

        boolean mirrorFlag;

        public Header(byte[] bytes) {
            int length = bytes.length;
            if (length != 16) return;

            // 检查文件头是否为NES_MAGIC_STRING
            for (int i = 0; i < 4; i++) {
                check(bytes[i] == NES_MAGIC_STRING[i], INVALID_NES_ROM_MESSAGE);
            }

            romBanksCount = bytes[4] & 0xFF;
            vromBanksCount = bytes[5] & 0xFF;

            mapperNo = (bytes[7] & 0xF0) | (bytes[6] >> 4);

            mirrorFlag = (bytes[6] & 0x01) != 0;
            trainerFlag = (bytes[6] & 0x04) != 0;
        }

        @Override
        public String toString() {
            return "Header{ " +
                    "romBanksCount=" + romBanksCount +
                    ", vromBanksCount=" + vromBanksCount +
                    ", mapperNo=" + mapperNo +
                    ", trainerFlag=" + trainerFlag +
                    ", mirrorFlag=" + mirrorFlag +
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
    byte[] chr;

    public NesRom(InputStream inputStream) throws IOException {
        byte[] headerBytes = new byte[16];

        inputStream.read(headerBytes, 0, 16);
        header = new Header(headerBytes);

        if (header.trainerFlag) {
            trainer = new byte[512];
            inputStream.read(trainer, 0, 512);
        }

        int prgCount = header.romBanksCount;
        prg = new byte[prgCount * 16384];
        for (int i = 0; i < prgCount; i++) {
            inputStream.read(prg, i * 16384, 16384);
        }

        int chrCount = header.vromBanksCount;
        chr = new byte[chrCount * 8192];
        for (int i = 0; i < chrCount; i++) {
            inputStream.read(chr, i * 8192, 8192);
        }
    }
}


