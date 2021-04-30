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

    private APU apu = new APU();

    private Cartridge cartridge;

    /**
     * CPU RAM
     * 0x0000 - 0x0800 2K
     */
    public byte[] cpuRAM = new byte[2048];

    /**
     * 两个控制器
     * 8位 每位代表一个按键的状态（1 按下）A B Select Start Up Down Left Right
     */
    public byte[] controller = new byte[2];

    private byte[] controllerState = new byte[2];

    public Joypad joypad1 = new Joypad();
    public Joypad joypad2 = new Joypad();

    // endregion


    // region DMA

    private byte dmaPage = 0x00;
    private byte dmaOffset = 0x00;
    private byte dmaData = 0x00;

    private boolean isDMAStart = false;

    /**
     * DMA 可能需要等待1或2个CPU时钟周期才能开始
     */
    private boolean isDMACanStart = false;
    // endregion

    private long cycles = 0;

    public CPUBus() {
    }

    /**
     * 向总线写入数据
     * @param address 写入地址 16bit
     * @param data 要写入的数据 8bit
     */
    public void write(int address, int data) {
        byte byteData = (byte) (data & 0x00FF);
        if (address >= 0x0000 && address <= 0x1FFF) {
            // 0x0000 - 0x1FFF 共8k，但实际CPU RAM只有2K大小，其余都是mirror
            cpuRAM[address & 0x07FF] = byteData;
        } else if (address >= 0x2000 && address <= 0x3FFF) {
            // 0x2000 - 0x2007 是PPU的8个寄存器，其余是mirror
            // CPU通过寄存器读写PPU
            ppu.cpuWrite(address & 0x0007, byteData);
        } else if ((address >= 0x4000 && address <= 0x4013) || address == 0x4015 || address == 0x4017) {
            apu.write(address, data);
        }
        else if (address == 0x4014) {
            // 执行DMA操作
            dmaPage = byteData;
            dmaOffset = 0x00;

            isDMAStart = true;
        } else if (address == 0x4016) {
            joypad1.write(0x4016, data);
            controllerState[address & 0x0001] = controller[address & 0x0001];
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
            // 手柄
            if (address == 0x4016) {
                data = joypad1.read();
            } else if (address == 0x4017) {
                data = joypad2.read();
            }
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

    public void setCpu(CPU cpu) {
        this.cpu = cpu;
        cpu.setBus(this);
    }

    public void clock() {
        ppu.clock();
        apu.clock();

        // PPU 的运行速度是 CPU 的3倍， 同步时钟周期
        if (cycles % 3 == 0) {
            if (!isDMAStart) {
                cpu.clock();
            } else {
                // DMA必须从CPU的偶数始终周期开始
                if (isDMACanStart) {
                    // 开始DMA
                    if (cycles % 2 == 0) {
                        // 偶数时钟周期读
                        dmaData = read((dmaPage & 0x00FF) << 8 | (dmaOffset & 0x00FF));
                    } else {
                        // 奇数时钟周期写
                       ppu.setOAMEntry(dmaOffset & 0x00FF, dmaData);
//                       ppu.oam[dmaOffset & 0x00FF] = dmaData;
                       dmaOffset++;

                       if (dmaOffset == 0x00) {
                           isDMAStart = false;
                           isDMACanStart = false;
                       }
                    }
                } else {
                    // 等待CPU的始终周期，下一周期为偶数时DMA才能开始
                    if (cycles % 2 == 1) {
                        isDMACanStart = true;
                    }
                }
            }
        }


        if (ppu.nmi) {
            ppu.nmi = false;
            cpu.nmi();
        }

        cycles++;
    }

    public void reset() {
        cpu.reset();
        ppu.reset();
//        Arrays.fill(cpuRAM, (byte)0xFF);
        cycles = 0;
    }
}
