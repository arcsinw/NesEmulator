package com.arcsinw.nesemulator.mapper;

/**
 *
 * CPU $6000-$7FFF: 8 KB PRG RAM bank, (optional)
 * CPU $8000-$BFFF: 16 KB PRG ROM bank, either switchable or fixed to the first bank
 * CPU $C000-$FFFF: 16 KB PRG ROM bank, either fixed to the last bank or switchable
 * PPU $0000-$0FFF: 4 KB switchable CHR bank
 * PPU $1000-$1FFF: 4 KB switchable CHR bank
 * https://wiki.nesdev.com/w/index.php/MMC1
 */
public class MMC1 extends AbstractMapper{

    public MMC1(byte[] prg, byte[] chr) {
        super(prg, chr);
        id = 1;
    }

    public MMC1() {
        id = 1;
    }


    // region 内部寄存器

    /**
     * $8000-$9FFF
     * xxxCPPMM
     * ||||||||
     * ||||||++- Mirroring (0: one-screen, lower bank; 1: one-screen, upper bank;
     * ||||||               2: vertical; 3: horizontal)
     * ||||++--- PRG ROM bank mode (0, 1: switch 32 KB at $8000, ignoring low bit of bank number;
     * ||||                         2: fix first bank at $8000 and switch 16 KB bank at $C000;
     * ||||                         3: fix last bank at $C000 and switch 16 KB bank at $8000)
     * |||+----- CHR ROM bank mode (0: switch 8 KB at a time; 1: switch two separate 4 KB banks)
     */
    private byte controlRegister;

    /**
     * $A000-$BFFF
     * xxxCCCCC
     * ||||||||
     * |||+++++- Select 4 KB or 8 KB CHR bank at PPU $0000 (low bit ignored in 8 KB mode)
     */
    private byte chrBank0Register;

    /**
     * $A000-$BFFF
     * xxxCCCCC
     * ||||||||
     * |||+++++- Select 4 KB or 8 KB CHR bank at PPU $0000 (low bit ignored in 8 KB mode)
     */
    private byte chrBank1Register;

    /**
     * $E000-$FFFF
     * xxxRPPPP
     * ||||||||
     * ||||++++- Select 16 KB PRG ROM bank (low bit ignored in 32 KB mode)
     * |||+----- PRG RAM chip enable (0: enabled; 1: disabled; ignored on MMC1A)
     */
    private byte prgBankRegister;

    // endregion

    // region 字段

    /**
     * Mirroring (0: one-screen, lower bank; 1: one-screen, upper bank;
     *            2: vertical; 3: horizontal)
     */
    private int mirroring;

    /**
     * 0, 1: switch 32 KB at $8000, ignoring low bit of bank number;
     * 2: fix first bank at $8000 and switch 16 KB bank at $C000;
     * 3: fix last bank at $C000 and switch 16 KB bank at $8000
     */
    private int prgRomBankMode;

    /**
     * 0: switch 8 KB at a time;
     * 1: switch two separate 4 KB banks
     */
    private int chrRomBankMode;

    /**
     * CHR的bank每个4KB
     */
    private int chrBank0Offset;
    private int chrBank1Offset;

    /**
     * PRG的每个bank 16KB
     */
    private int prgBank0Offset;
    private int prgBank1Offset;

    // endregion

    private byte shiftRegister;
    private byte shiftCount;
    private boolean prgRamEnable = false;

    @Override
    public void write(int address, int data) {
        byte byteData = (byte) (data & 0x00FF);
        if (address <= 0x1FFF) {
            // write chr
            int bank = (address / 0x1000) == 0 ? chrBank0Offset : chrBank1Offset;
            chr[bank + (address % 0x1000)] = byteData;
        } else if (address >= 0x6000 && address <= 0x7FFF) {
            sram[address - 0x6000] = byteData;
        } else if (address >= 0x8000 && address <= 0xFFFF) {
            // write common shifter register
            loadRegister(address, data);
        }else
        {
            throw new Error("Invalid Mapper read at address " + String.format("%04X", address));
        }
    }


    @Override
    public byte read(int address) {
        byte data = 0;
        if (address <= 0x1FFF) {
            // ppu chr bank 0,1
            int bank = (address / 0x1000) == 0 ? chrBank0Offset : chrBank1Offset;
            data = chr[bank + (address % 0x1000)];
        } else if (address >= 0x6000 && address <= 0x7FFF) {
            // 8KB PRG RAM bank
            data = sram[address - 0x6000];
        } else if (address >= 0x8000 && address <= 0xFFFF) {
            // cpu prg rom bank 0,1
            int bank = ((address - 0x8000) / 0x4000) == 0 ? prgBank0Offset : prgBank1Offset;
            if ((bank + (address % 0x4000)) > prg.length) {
                int a = 0;
            }

            data = prg[bank + (address % 0x4000)];
        } else
        {
            throw new Error("Invalid Mapper read at address " + String.format("%04X", address));
        }

        return data;
    }


    public void loadRegister(int address, int data) {
        if ((data & 0x80) != 0) {
            // clear the shifter register to its initial state
            writeRegister(address, (shiftRegister & 0x00FF) | 0x0C);
            shiftRegister = 0;
            shiftCount = 0;
        } else {
            // 连续写入5次
            shiftRegister |= (byte) ((data & 0x01) << shiftCount);
            shiftCount++;

            if (shiftCount == 5) {
                // 根据第5次写入的地址将5bit的数据写入内部的寄存器中
                writeRegister(address, shiftRegister);
                shiftRegister = 0;
                shiftCount = 0;
            }
        }
    }

    public void writeControlRegister(byte data) {
        controlRegister = data;
        mirroring = (data & 0x03);
        prgRomBankMode = ((data >>> 2) & 0x03);
        chrRomBankMode = ((data >>> 4) & 0x01);
        updateBankOffset();
    }

    public void writeRegister(int address, int data) {
        byte byteData = (byte) (data & 0x00FF);
        if (address >= 0x8000 && address <= 0x9FFF) {
            // control
            writeControlRegister(byteData);
        } else if (address >= 0xA000 && address <= 0xBFFF) {
            // chr bank 0 (pattern table 0 $0000)
            chrBank0Register = byteData;
            updateBankOffset();
        } else if (address >= 0xC000 && address <= 0xDFFF) {
            // chr bank 1 (pattern table 1 $1000)
            chrBank1Register = byteData;
            updateBankOffset();
        } else if (address >= 0xE000 && address <= 0xFFFF) {
            // prg bank
            prgBankRegister = byteData;
            prgRamEnable = ((data >>> 4) & 0x01) != 0;
            updateBankOffset();
        }
    }

    public void updateBankOffset() {
        switch (prgRomBankMode) {
            case 0:
            case 1:
                // switch 32 KB at $8000, ignoring low bit of bank number
                prgBank0Offset = ((prgBankRegister & 0x0E) >>> 1) * 0x4000;
                prgBank1Offset = prgBank0Offset + 0x4000;
                break;
            case 2:
                // fix first bank at $8000 and switch 16 KB bank at $C000
                prgBank0Offset = 0;
                prgBank1Offset = (prgBankRegister & 0x0F) * 0x4000;
                break;
            case 3:
                // fix last bank at $C000 and switch 16 KB bank at $8000
                prgBank0Offset = (prgBankRegister & 0x0F) * 0x4000;
                prgBank1Offset = (prg.length / 16384 - 1) * 0x4000;
                break;
            default:
                break;
        }

//        System.out.println(prgBank0Offset + " " + prgBank1Offset);

        switch (chrRomBankMode) {
            case 0:
                // switch 8 KB at a time
                chrBank0Offset = ((chrBank0Register & 0x1E) >>> 1) * 0x1000;
                chrBank1Offset = chrBank0Offset + 0x1000;
                break;
            case 1:
                // switch two separate 4 KB banks
                chrBank0Offset = chrBank0Register * 0x1000;
                chrBank1Offset = chrBank1Register * 0x1000;
                break;
            default:
                break;
        }
    }

    @Override
    public void reset() {
        super.reset();
        shiftRegister = 0x0C;
        controlRegister = 0;
        chrBank0Register = 0;
        chrBank1Register = 0;
        prgBankRegister = 0;

        shiftCount = 0;

        prgBank0Offset = 0;
        prgBank1Offset = ((prg.length / 16384) - 1) * 0x4000;
    }
}
