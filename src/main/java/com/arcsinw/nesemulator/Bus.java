package com.arcsinw.nesemulator;

public interface Bus {

    /**
     * 向总线写入数据
     * @param address 写入地址 16bit
     * @param data 要写入的数据 8bit
     */
    public void write(int address, byte data);

    /**
     * 从总线读取数据
     * @param address 数据地址
     * @return 8bit 数据
     */
    public byte read(int address);


}
