package com.arcsinw.nesemulator.mapper;

/**
 * 将地址映射到正确的物理内存
 */
public abstract class AbstractMapper {
    protected int id;

    public String getName() {
        return String.format("Mapper%03d", id);
    }

    /**
     * Program Bank
     * 16KB per bank
     */
    protected byte[] prg;

    /**
     * Character Bank
     * 8KB per bank
     */
    protected byte[] chr;

    protected byte[] sram;

    public AbstractMapper(byte[] prg, byte[] chr) {
        this.prg = prg;
        this.chr = chr;
    }

    public AbstractMapper(byte[] prg, byte[] chr, byte[] sram) {
        this.prg = prg;
        this.chr = chr;
        this.sram = sram;
    }

    public AbstractMapper() { }

    public void setPrg(byte[] prg) {
        this.prg = prg;
    }

    public void setChr(byte[] chr) {
        this.chr = chr;
    }

    public void setSram(byte[] sram) {
        this.sram = sram;
    }

    public abstract void write(int address, int data);

    public abstract byte read(int address);

    public void reset() {};
}
