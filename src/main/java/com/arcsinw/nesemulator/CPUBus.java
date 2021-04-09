package com.arcsinw.nesemulator;


/**
 * CPU总线
 * 16bit 0x0000 ~ 0xFFFF 共 64KB
 * 0x0000 - 0x1FFF RAM
 * 0x2000 - 0x401F I/O Registers
 * 0x4020 - 0x5FFF Expansion ROM
 * 0x6000 - 0x7FFF SRAM
 * 0x8000 - 0xFFFF PRG-ROM 卡带中的程序数据
 */
public class CPUBus {

    // region 总线上的设备

    private CPU cpu;

    public PPU ppu;

    private APU apu;

    private Cartridge cartridge;

    /**
     * CPU RAM
     */
    public byte[] cpuRAM = new byte[2 * 1024];

    /**
     * 两个控制器
     */
    public byte[] controller = new byte[2];

    // endregion

    public CPUBus() {

    }

    /**
     * 向总线写入数据
     * @param address 写入地址 16bit
     * @param data 要写入的数据 8bit
     */
    public void write(int address, byte data) {
        if (address >= 0x0000 && address <= 0x1FFF) {
            // 0x0000 - 0x1FFF 共8k，但实际CPU RAM只有2K大小，其余都是mirror
            cpuRAM[address & 0x07FF] = data;
        } else if (address >= 0x2000 && address <= 0x3FFF) {
            // 0x2000 - 0x2007 是PPU的8个寄存器，其余是mirror
            ppu.cpuWrite(address & 0x0007, data);
        } else if (address >= 0x4016 && address <= 0x4017) {
//            controller[address & 0x0001] = data;
        } else if (address >= 0x8000 && address <= 0xFFFF) {
            cartridge.cpuWrite(address, data);
        }
    }

    /**
     * 从总线读取数据
     * @param address 数据地址
     * @return 8bit 数据
     */
    public byte read(int address) {
        byte data = 0x00;
        if (address >= 0x0000 && address <= 0x1FFF) {
            data = cpuRAM[address & 0x07FF];
        } else if (address >= 0x2000 && address <= 0x3FFF) {
            // 0x2000 - 0x2007 是PPU的寄存器，其余是mirror
            data = ppu.cpuRead(address & 0x0007);
        } else if (address >= 0x4016 && address <= 0x4017) {

        } else if (address >= 0x8000 && address <= 0xFFFF) {
            data = cartridge.cpuRead(address);
        }

        return data;
    }

    public void setCartridge(Cartridge cartridge) {
        this.cartridge = cartridge;
        this.ppu.setCartridge(cartridge);
    }

    public void setPpu(PPU ppu) {
        this.ppu = ppu;
    }

    private long cycles;

    public void clock() {
        ppu.clock();

        // PPU 的运行速度是 CPU 的3倍， 同步时钟周期
        if (cycles % 3 == 0) {
            cpu.clock();
        }

        cycles++;
    }
}
