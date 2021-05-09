package com.arcsinw.nesemulator.mapper;

public class MMC3 extends AbstractMapper{

    public MMC3(byte[] prg, byte[] chr) {
        super(prg, chr);
        id = 4;
    }

    public MMC3() {
        id = 4;
    }

    @Override
    public void write(int address, int data) {

    }

    @Override
    public byte read(int address) {
        return 0;
    }
}
