package com.arcsinw.nesemulator;

/**
 * 总线上的设备
 */
public interface CPUBusDevice {
    /**
     * 写入数据
     * @param address 写入地址 16bit
     * @param data 要写入的数据 8bit
     */
    public void cpuWrite(int address, byte data);

    /**
     * 读取数据
     * @param address 数据地址
     * @return 8bit 数据
     */
    public byte cpuRead(int address);
}
