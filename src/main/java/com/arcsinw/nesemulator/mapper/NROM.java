package com.arcsinw.nesemulator.mapper;

/**
 * Mapper000
 */
public class NROM extends AbstractMapper {
    public NROM(byte[] prg, byte[] chr) {
        super(prg, chr);
        id = 0;
    }

    public NROM() {
        id = 0;
    }

    @Override
    public void write(int address, int data) {
        if (address < 0x2000) {
            // Pattern Table
            chr[address] = (byte) (data & 0x00FF);
        }
    }

    @Override
    public byte read(int address) {
        byte data = 0;
        if (address < 0x2000) {
            // Pattern Table
            data = chr[address];
        } else if (address >= 0x8000 && address <= 0xFFFF) {
            // Program
            data = prg[address & (prg.length > 16384 ? 0x7FFF : 0x3FFF)];
        }

        return data;
    }
}
