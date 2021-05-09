package com.arcsinw.nesemulator.mapper;

public class AxROM extends AbstractMapper {
    public AxROM(byte[] prg, byte[] chr) {
        super(prg, chr);
        id = 7;
    }

    public AxROM() {
        id = 7;
    }
    @Override
    public void write(int address, int data) {

    }

    @Override
    public byte read(int address) {
        return 0;
    }
}
