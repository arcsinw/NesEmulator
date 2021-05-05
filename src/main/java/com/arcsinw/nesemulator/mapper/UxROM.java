package com.arcsinw.nesemulator.mapper;

/**
 * 包括两个Banks
 * CPU $8000-$BFFF: 16 KB switchable PRG ROM bank
 * CPU $C000-$FFFF: 16 KB PRG ROM bank, fixed to the last bank
 */
public class UxROM extends AbstractMapper {

    public UxROM(byte[] prg, byte[] chr) {
        super(prg, chr);
        id = 2;
    }

    public UxROM() {
        id = 2;
    }

    private int bank0Offset = 0;
    private int bank1Offset = 0;

    @Override
    public void write(int address, int data) {
        if (address < 0x2000) {
            // Pattern Table
            chr[address] = (byte) (data & 0x00FF);
        } else if (address >= 0x8000 && address <= 0xFFFF) {
            // 向这个地址范围写入数据视为切换PRG Banks
            writeBankSelect(data);
        }
    }

    @Override
    public byte read(int address) {
        byte data = 0;
        if (address < 0x2000) {
            // Pattern Table
            data = chr[address];
        } else if (address >= 0x8000 && address <= 0xBFFF) {
            // PRG Bank 0
            data = prg[(address - 0x8000) + bank0Offset];
        } else if (address >= 0xC000 && address <= 0xFFFF) {
            // PRG Bank 1
            data = prg[(address - 0xC000) + bank1Offset];
        }

        return data;
    }

    @Override
    public void reset() {
        bank0Offset = 0;
        bank1Offset = ((prg.length / 16384) - 1) * 0x4000;
    }

    /**
     * 切换$8000 ~ $BFFF指向的Bank
     * 7  bit  0
     * ---- ----
     * xxxx pPPP
     *      ||||
     *      ++++- Select 16 KB PRG ROM bank for CPU $8000-$BFFF
     *           (UNROM uses bits 2-0; UOROM uses bits 3-0)
     * @param data
     */
    public void writeBankSelect(int data) {
        bank0Offset = (data & 0x0F) * 0x4000;
    }
}
