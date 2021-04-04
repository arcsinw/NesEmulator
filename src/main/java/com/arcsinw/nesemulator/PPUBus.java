package com.arcsinw.nesemulator;

/**
 * PPU总线
 * 16bit, 0x0000 ~ 0xFFFF, 共 64KB
 */
public class PPUBus implements Bus{
    /**
     * 总线是16bit的，可使用的内存范围为 64KB
     * 0x0000 - 0xFFFF
     */
    public byte[] ram = new byte[64 * 1024];


    /**
     * 向总线写入数据
     * @param address 写入地址 16bit
     * @param data 要写入的数据 8bit
     */
    public void write(int address, byte data) {
        if (address >= 0x0000 && address <= 0xFFFF) {
            ram[address] = data;
        }
    }

    /**
     * 从总线读取数据
     * @param address 数据地址
     * @return 8bit 数据
     */
    public byte read(int address) {
        if (address >= 0x0000 && address <= 0xFFFF) {
            return ram[address];
        }

        return 0x00;
    }
}
