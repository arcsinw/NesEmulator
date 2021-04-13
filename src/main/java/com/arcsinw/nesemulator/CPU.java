package com.arcsinw.nesemulator;

import java.util.Arrays;
import java.util.Optional;

/**
 * 2A03的CPU模拟
 */
public class CPU {

    // region 字段
    private int fetched = 0x00;

    private int absoluteAddress = 0x0000;
    private byte relativeAddress = 0x00;
    private int cycles = 0;
    private int operationCode = 0x00;

    private CPUBus bus;

    /**
     * 卡带
     */
    private Cartridge cartridge;

    // debug only
    int clockCount = 0;
    boolean logging = true;

    // endregion

    void setBus(CPUBus b) {
        this.bus = b;
    }

    byte read(int address) {
        return bus.read(address);
    }

    int read16(int address) {
        byte lo = read(address);
        byte hi = read(address + 1);
        return ((hi & 0x00FF) << 8) | (lo & 0x00FF);
    }

    void write(int address, byte data) {
        bus.write(address, data);
    }

    private final int STACK_BASE_ADDRESS = 0x0100;

    // region 6502 Instruction set
    // https://pastraiser.com/cpu/6502/6502_opcodes.html

    /**
     * 指令表，为16x16的矩阵，横坐标和纵坐标为0-F，这里使用一维数组表示
     * 例如， 0x24是BIT，0x24的十进制为36，在数组中的下标也是36
     */
    public static final String[] INSTRUCTION_SET = {
            "BRK", "ORA", "UNK", "UNK", "UNK", "ORA", "ASL", "UNK", "PHP", "ORA", "ASL", "UNK", "UNK", "ORA", "ASL", "UNK",
            "BPL", "ORA", "UNK", "UNK", "UNK", "ORA", "ASL", "UNK", "CLC", "ORA", "UNK", "UNK", "UNK", "ORA", "ASL", "UNK",
            "JSR", "AND", "UNK", "UNK", "BIT", "AND", "ROL", "UNK", "PLP", "AND", "ROL", "UNK", "BIT", "AND", "ROL", "UNK",
            "BMI", "AND", "UNK", "UNK", "UNK", "AND", "ROL", "UNK", "SEC", "AND", "UNK", "UNK", "UNK", "AND", "ROL", "UNK",
            "RTI", "EOR", "UNK", "UNK", "UNK", "EOR", "LSR", "UNK", "PHA", "EOR", "LSR", "UNK", "JMP", "EOR", "LSR", "UNK",
            "BVC", "EOR", "UNK", "UNK", "UNK", "EOR", "LSR", "UNK", "CLI", "EOR", "UNK", "UNK", "UNK", "EOR", "LSR", "UNK",
            "RTS", "ADC", "UNK", "UNK", "UNK", "ADC", "ROR", "UNK", "PLA", "ADC", "ROR", "UNK", "JMP", "ADC", "ROR", "UNK",
            "BVS", "ADC", "UNK", "UNK", "UNK", "ADC", "ROR", "UNK", "SEI", "ADC", "UNK", "UNK", "UNK", "ADC", "ROR", "UNK",
            "UNK", "STA", "UNK", "UNK", "STY", "STA", "STX", "UNK", "DEY", "UNK", "TXA", "UNK", "STY", "STA", "STX", "UNK",
            "BCC", "STA", "UNK", "UNK", "STY", "STA", "STX", "UNK", "TYA", "STA", "TXS", "UNK", "UNK", "STA", "UNK", "UNK",
            "LDY", "LDA", "LDX", "UNK", "LDY", "LDA", "LDX", "UNK", "TAY", "LDA", "TAX", "UNK", "LDY", "LDA", "LDX", "UNK",
            "BCS", "LDA", "UNK", "UNK", "LDY", "LDA", "LDX", "UNK", "CLV", "LDA", "TSX", "UNK", "LDY", "LDA", "LDX", "UNK",
            "CPY", "CMP", "UNK", "UNK", "CPY", "CMP", "DEC", "UNK", "INY", "CMP", "DEX", "UNK", "CPY", "CMP", "DEC", "UNK",
            "BNE", "CMP", "UNK", "UNK", "UNK", "CMP", "DEC", "UNK", "CLD", "CMP", "UNK", "UNK", "UNK", "CMP", "DEC", "UNK",
            "CPX", "SBC", "UNK", "UNK", "CPX", "SBC", "INC", "UNK", "INX", "SBC", "NOP", "UNK", "CPX", "SBC", "INC", "UNK",
            "BEQ", "SBC", "UNK", "UNK", "UNK", "SBC", "INC", "UNK", "SED", "SBC", "UNK", "UNK", "UNK", "SBC", "INC", "UNK",
    };

    /**
     * 指令的长度，与上面的指令表一一对应
     */
    public static final int[] INSTRUCTION_LENGTH = {
            1, 2, 1, 1, 1, 2, 2, 1, 1, 2, 1, 1, 1, 3, 3, 1,
            2, 2, 1, 1, 1, 2, 2, 1, 1, 3, 1, 1, 1, 3, 3, 1,
            3, 2, 1, 1, 2, 2, 2, 1, 1, 2, 1, 1, 3, 3, 3, 1,
            2, 2, 1, 1, 1, 2, 2, 1, 1, 3, 1, 1, 1, 3, 3, 1,
            1, 2, 1, 1, 1, 2, 2, 1, 1, 2, 1, 1, 3, 3, 3, 1,
            2, 2, 1, 1, 1, 2, 2, 1, 1, 3, 1, 1, 1, 3, 3, 1,
            1, 2, 1, 1, 1, 2, 2, 1, 1, 2, 1, 1, 3, 3, 3, 1,
            2, 2, 1, 1, 1, 2, 2, 1, 1, 3, 1, 1, 1, 3, 3, 1,
            1, 2, 1, 1, 2, 2, 2, 1, 1, 1, 1, 1, 3, 3, 3, 1,
            2, 2, 1, 1, 2, 2, 2, 1, 1, 3, 1, 1, 1, 3, 1, 1,
            2, 2, 2, 1, 2, 2, 2, 1, 1, 2, 1, 1, 3, 3, 3, 1,
            2, 2, 1, 1, 2, 2, 2, 1, 1, 3, 1, 1, 3, 3, 3, 1,
            2, 2, 1, 1, 2, 2, 2, 1, 1, 2, 1, 1, 3, 3, 3, 1,
            2, 2, 1, 1, 1, 2, 2, 1, 1, 3, 1, 1, 1, 3, 3, 1,
            2, 2, 1, 1, 2, 2, 2, 1, 1, 2, 1, 1, 3, 3, 3, 1,
            2, 2, 1, 1, 1, 2, 2, 1, 1, 3, 1, 1, 1, 3, 3, 1
    };

    /**
     * 指令的周期
     */
    public static final int[] INSTRUCTION_CYCLE = {
         // 0  1  2  3  4  5  6  7  8  9  A  B  C  D  E  F
            7, 6, 2, 8, 3, 3, 5, 5, 3, 2, 2, 2, 4, 4, 6, 6,
            2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,
            6, 6, 2, 8, 3, 3, 5, 5, 4, 2, 2, 2, 4, 4, 6, 6,
            2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,
            6, 6, 2, 8, 3, 3, 5, 5, 3, 2, 2, 2, 3, 4, 6, 6,
            2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,
            6, 6, 2, 8, 3, 3, 5, 5, 4, 2, 2, 2, 5, 4, 6, 6,
            2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,
            2, 6, 2, 6, 3, 3, 3, 3, 2, 2, 2, 2, 4, 4, 4, 4,
            2, 6, 2, 6, 4, 4, 4, 4, 2, 5, 2, 5, 5, 5, 5, 5,
            2, 6, 2, 6, 3, 3, 3, 3, 2, 2, 2, 2, 4, 4, 4, 4,
            2, 5, 2, 5, 4, 4, 4, 4, 2, 4, 2, 4, 4, 4, 4, 4,
            2, 6, 2, 8, 3, 3, 5, 5, 2, 2, 2, 2, 4, 4, 6, 6,
            2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,
            2, 6, 2, 8, 3, 3, 5, 5, 2, 2, 2, 2, 4, 4, 6, 6,
            2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7
    };

    /**
     * 指令的寻址模式
     * 0表示隐含寻址 或 该指令不存在
     */
    public static final int[] INSTRUCTION_ADDRESSING_MODE = {
         // 0  1   2  3  4  5  6  7  8  9  A  B  C  D  E  F
            0, 11, 0, 0, 0, 3, 3, 0, 0, 2, 1, 0, 0, 7, 7, 0,
            6, 12, 0, 0, 0, 4, 4, 0, 0, 9, 0, 0, 0, 8, 8, 0,
            7, 11, 0, 0, 3, 3, 3, 0, 0, 2, 1, 0, 7, 7, 7, 0,
            6, 12, 0, 0, 0, 4, 4, 0, 0, 9, 0, 0, 0, 8, 8, 0,
            0, 11, 0, 0, 0, 3, 3, 0, 0, 2, 1, 0, 7, 7, 7, 0,
            6, 12, 0, 0, 0, 4, 4, 0, 0, 9, 0, 0, 0, 8, 8, 0,
            0, 11, 0, 0, 0, 3, 3, 0, 0, 2, 1, 0,10, 7, 7, 0,
            6, 12, 0, 0, 0, 4, 4, 0, 0, 9, 0, 0, 0, 8, 8, 0,
            0, 11, 0, 0, 3, 3, 3, 0, 0, 0, 0, 0, 7, 7, 7, 0,
            6, 12, 0, 0, 4, 4, 5, 0, 0, 9, 0, 0, 0, 8, 0, 0,
            2, 11, 2, 0, 3, 3, 3, 0, 0, 2, 0, 0, 7, 7, 7, 0,
            6, 12, 0, 0, 4, 4, 5, 0, 0, 9, 0, 0, 8, 8, 9, 0,
            2, 11, 0, 0, 3, 3, 3, 0, 0, 2, 0, 0, 7, 7, 7, 0,
            6, 12, 0, 0, 0, 4, 4, 0, 0, 9, 0, 0, 0, 8, 8, 0,
            2, 11, 0, 0, 3, 3, 3, 0, 0, 2, 0, 0, 7, 7, 7, 0,
            6, 12, 0, 0, 0, 4, 4, 0, 0, 9, 0, 0, 0, 8, 8, 0,
    };

    /**
     * 寻址模式索引表
     */
    public static final AddressingMode[] ADDRESSING_MODE_TABLE = new AddressingMode[] {
            AddressingMode.Implied,             // 0
            AddressingMode.Accumulator,         // 1
            AddressingMode.Immediate,           // 2
            AddressingMode.ZeroPage,            // 3
            AddressingMode.ZeroPageX,           // 4
            AddressingMode.ZeroPageY,           // 5
            AddressingMode.Relative,            // 6
            AddressingMode.Absolute,            // 7
            AddressingMode.AbsoluteX,           // 8
            AddressingMode.AbsoluteY,           // 9
            AddressingMode.Indirect,            // 10
            AddressingMode.IndexedIndirectX,    // 11
            AddressingMode.IndirectIndexedY     // 12
    };

    public enum AddressingMode {
        Implied(0),         // 隐含寻址，无需明确指出操作地址，如CLC，RTS
        Accumulator(1),     // 累加器寻址
        Immediate(2),
        ZeroPage(3),
        ZeroPageX(4),
        ZeroPageY(5),
        Relative(6),
        Absolute(7),
        AbsoluteX(8),
        AbsoluteY(9),
        Indirect(10),
        IndexedIndirectX(11),
        IndirectIndexedY(12);

        private int key;

        private AddressingMode(int k) {
            key = k;
        }

        public static AddressingMode fromIndex(int k) {
            return ADDRESSING_MODE_TABLE[k];
        }

        public String getFullName() {
            return "AddressingMode." + this.name();
        }
    }

    // TODO 使用两个Switch代替枚举
    private enum Instruction {
        BRK_Implied("BRK", 1, 7, 0x00, AddressingMode.Implied) {
            @Override
            public void operation(CPU cpu) {
                cpu.IMP();
                cpu.BRK();
            }
        },
        ORA_IndexedIndirectX("ORA", 2, 6, 0x01, AddressingMode.IndexedIndirectX) {
            @Override
            public void operation(CPU cpu) {
                cpu.IZX();
                cpu.ORA();
            }
        },
        ORA_ZeroPage("ORA", 2, 3, 0x05, AddressingMode.ZeroPage) {
            @Override
            public void operation(CPU cpu) {
                cpu.ZP0();
                cpu.ORA();
            }
        },
        ASL_ZeroPage("ASL", 2, 5, 0x06, AddressingMode.ZeroPage) {
            @Override
            public void operation(CPU cpu) {
                cpu.ZP0();
                cpu.ASL();
            }
        },
        PHP_Implied("PHP", 1, 3, 0x08, AddressingMode.Implied) {
            @Override
            public void operation(CPU cpu) {
                cpu.IMP();
                cpu.PHP();
            }
        },
        ORA_Immediate("ORA", 2, 2, 0x09, AddressingMode.Immediate) {
            @Override
            public void operation(CPU cpu) {
                cpu.IMM();
                cpu.ORA();
            }
        },
        ASL_Accumulator("ASL", 1, 2, 0x0A, AddressingMode.Accumulator) {
            @Override
            public void operation(CPU cpu) {
                cpu.ACM();
                cpu.ASL();
            }
        },
        ORA_Absolute("ORA", 3, 4, 0x0D, AddressingMode.Absolute) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABS();
                cpu.ORA();
            }
        },
        ASL_Absolute("ASL", 3, 6, 0x0E, AddressingMode.Absolute) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABS();
                cpu.ASL();
            }
        },
        BPL_Relative("BPL", 2, 2, 0x10, AddressingMode.Relative) {
            @Override
            public void operation(CPU cpu) {
                cpu.REL();
                cpu.BPL();
            }
        },
        ORA_IndirectIndexedY("ORA", 2, 5, 0x11, AddressingMode.IndirectIndexedY) {
            @Override
            public void operation(CPU cpu) {
                cpu.IZY();
                cpu.ORA();
            }
        },
        ORA_ZeroPageX("ORA", 2, 4, 0x15, AddressingMode.ZeroPageX) {
            @Override
            public void operation(CPU cpu) {
                cpu.ZPX();
                cpu.ORA();
            }
        },
        ASL_ZeroPageX("ASL", 2, 6, 0x16, AddressingMode.ZeroPageX) {
            @Override
            public void operation(CPU cpu) {
                cpu.ZPX();
                cpu.ASL();
            }
        },
        CLC_Implied("CLC", 1, 2, 0x18, AddressingMode.Implied) {
            @Override
            public void operation(CPU cpu) {
                cpu.IMP();
                cpu.CLC();
            }
        },
        ORA_AbsoluteY("ORA", 3, 4, 0x19, AddressingMode.AbsoluteY) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABY();
                cpu.ORA();
            }
        },
        ORA_AbsoluteX("ORA", 3, 4, 0x1D, AddressingMode.AbsoluteX) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABX();
                cpu.ORA();
            }
        },
        ASL_AbsoluteX("ASL", 3, 7, 0x1E, AddressingMode.AbsoluteX) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABX();
                cpu.ASL();
            }
        },
        JSR_Absolute("JSR", 3, 6, 0x20, AddressingMode.Absolute) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABS();
                cpu.JSR();
            }
        },
        AND_IndexedIndirectX("AND", 2, 6, 0x21, AddressingMode.IndexedIndirectX) {
            @Override
            public void operation(CPU cpu) {
                cpu.IZX();
                cpu.AND();
            }
        },
        BIT_ZeroPage("BIT", 2, 3, 0x24, AddressingMode.ZeroPage) {
            @Override
            public void operation(CPU cpu) {
                cpu.ZP0();
                cpu.BIT();
            }
        },
        AND_ZeroPage("AND", 2, 3, 0x25, AddressingMode.ZeroPage) {
            @Override
            public void operation(CPU cpu) {
                cpu.ZP0();
                cpu.AND();
            }
        },
        ROL_ZeroPage("ROL", 2, 5, 0x26, AddressingMode.ZeroPage) {
            @Override
            public void operation(CPU cpu) {
                cpu.ZP0();
                cpu.ROL();
            }
        },
        PLP_Implied("PLP", 1, 4, 0x28, AddressingMode.Implied) {
            @Override
            public void operation(CPU cpu) {
                cpu.IMP();
                cpu.PLP();
            }
        },
        AND_Immediate("AND", 2, 2, 0x29, AddressingMode.Immediate) {
            @Override
            public void operation(CPU cpu) {
                cpu.IMM();
                cpu.AND();
            }
        },
        ROL_Accumulator("ROL", 1, 2, 0x2A, AddressingMode.Accumulator) {
            @Override
            public void operation(CPU cpu) {
                cpu.ACM();
                cpu.ROL();
            }
        },
        BIT_Absolute("BIT", 3, 4, 0x2C, AddressingMode.Absolute) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABS();
                cpu.BIT();
            }
        },
        AND_Absolute("AND", 3, 4, 0x2D, AddressingMode.Absolute) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABS();
                cpu.AND();
            }
        },
        ROL_Absolute("ROL", 3, 6, 0x2E, AddressingMode.Absolute) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABS();
                cpu.ROL();
            }
        },
        BMI_Relative("BMI", 2, 2, 0x30, AddressingMode.Relative) {
            @Override
            public void operation(CPU cpu) {
                cpu.REL();
                cpu.BMI();
            }
        },
        AND_IndirectIndexedY("AND", 2, 5, 0x31, AddressingMode.IndirectIndexedY) {
            @Override
            public void operation(CPU cpu) {
                cpu.IZY();
                cpu.AND();
            }
        },
        AND_ZeroPageX("AND", 2, 4, 0x35, AddressingMode.ZeroPageX) {
            @Override
            public void operation(CPU cpu) {
                cpu.ZPX();
                cpu.AND();
            }
        },
        ROL_ZeroPageX("ROL", 2, 6, 0x36, AddressingMode.ZeroPageX) {
            @Override
            public void operation(CPU cpu) {
                cpu.ZPX();
                cpu.ROL();
            }
        },
        SEC_Implied("SEC", 1, 2, 0x38, AddressingMode.Implied) {
            @Override
            public void operation(CPU cpu) {
                cpu.IMP();
                cpu.SEC();
            }
        },
        AND_AbsoluteY("AND", 3, 4, 0x39, AddressingMode.AbsoluteY) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABY();
                cpu.AND();
            }
        },
        AND_AbsoluteX("AND", 3, 4, 0x3D, AddressingMode.AbsoluteX) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABX();
                cpu.AND();
            }
        },
        ROL_AbsoluteX("ROL", 3, 7, 0x3E, AddressingMode.AbsoluteX) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABX();
                cpu.ROL();
            }
        },
        RTI_Implied("RTI", 1, 6, 0x40, AddressingMode.Implied) {
            @Override
            public void operation(CPU cpu) {
                cpu.IMP();
                cpu.RTI();
            }
        },
        EOR_IndexedIndirectX("EOR", 2, 6, 0x41, AddressingMode.IndexedIndirectX) {
            @Override
            public void operation(CPU cpu) {
                cpu.IZX();
                cpu.EOR();
            }
        },
        EOR_ZeroPage("EOR", 2, 3, 0x45, AddressingMode.ZeroPage) {
            @Override
            public void operation(CPU cpu) {
                cpu.ZP0();
                cpu.EOR();
            }
        },
        LSR_ZeroPage("LSR", 2, 5, 0x46, AddressingMode.ZeroPage) {
            @Override
            public void operation(CPU cpu) {
                cpu.ZP0();
                cpu.LSR();
            }
        },
        PHA_Implied("PHA", 1, 3, 0x48, AddressingMode.Implied) {
            @Override
            public void operation(CPU cpu) {
                cpu.IMP();
                cpu.PHA();
            }
        },
        EOR_Immediate("EOR", 2, 2, 0x49, AddressingMode.Immediate) {
            @Override
            public void operation(CPU cpu) {
                cpu.IMM();
                cpu.EOR();
            }
        },
        LSR_Accumulator("LSR", 1, 2, 0x4A, AddressingMode.Accumulator) {
            @Override
            public void operation(CPU cpu) {
                cpu.ACM();
                cpu.LSR();
            }
        },
        JMP_Absolute("JMP", 3, 3, 0x4C, AddressingMode.Absolute) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABS();
                cpu.JMP();
            }
        },
        EOR_Absolute("EOR", 3, 4, 0x4D, AddressingMode.Absolute) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABS();
                cpu.EOR();
            }
        },
        LSR_Absolute("LSR", 3, 6, 0x4E, AddressingMode.Absolute) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABS();
                cpu.LSR();
            }
        },
        BVC_Relative("BVC", 2, 2, 0x50, AddressingMode.Relative) {
            @Override
            public void operation(CPU cpu) {
                cpu.REL();
                cpu.BVC();
            }
        },
        EOR_IndirectIndexedY("EOR", 2, 5, 0x51, AddressingMode.IndirectIndexedY) {
            @Override
            public void operation(CPU cpu) {
                cpu.IZY();
                cpu.EOR();
            }
        },
        EOR_ZeroPageX("EOR", 2, 4, 0x55, AddressingMode.ZeroPageX) {
            @Override
            public void operation(CPU cpu) {
                cpu.ZPX();
                cpu.EOR();
            }
        },
        LSR_ZeroPageX("LSR", 2, 6, 0x56, AddressingMode.ZeroPageX) {
            @Override
            public void operation(CPU cpu) {
                cpu.ZPX();
                cpu.LSR();
            }
        },
        CLI_Implied("CLI", 1, 2, 0x58, AddressingMode.Implied) {
            @Override
            public void operation(CPU cpu) {
                cpu.IMP();
                cpu.CLI();
            }
        },
        EOR_AbsoluteY("EOR", 3, 4, 0x59, AddressingMode.AbsoluteY) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABY();
                cpu.EOR();
            }
        },
        EOR_AbsoluteX("EOR", 3, 4, 0x5D, AddressingMode.AbsoluteX) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABX();
                cpu.EOR();
            }
        },
        LSR_AbsoluteX("LSR", 3, 7, 0x5E, AddressingMode.AbsoluteX) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABX();
                cpu.LSR();
            }
        },
        RTS_Implied("RTS", 1, 6, 0x60, AddressingMode.Implied) {
            @Override
            public void operation(CPU cpu) {
                cpu.IMP();
                cpu.RTS();
            }
        },
        ADC_IndexedIndirectX("ADC", 2, 6, 0x61, AddressingMode.IndexedIndirectX) {
            @Override
            public void operation(CPU cpu) {
                cpu.IZX();
                cpu.ADC();
            }
        },
        ADC_ZeroPage("ADC", 2, 3, 0x65, AddressingMode.ZeroPage) {
            @Override
            public void operation(CPU cpu) {
                cpu.ZP0();
                cpu.ADC();
            }
        },
        ROR_ZeroPage("ROR", 2, 5, 0x66, AddressingMode.ZeroPage) {
            @Override
            public void operation(CPU cpu) {
                cpu.ZP0();
                cpu.ROR();
            }
        },
        PLA_Implied("PLA", 1, 4, 0x68, AddressingMode.Implied) {
            @Override
            public void operation(CPU cpu) {
                cpu.IMP();
                cpu.PLA();
            }
        },
        ADC_Immediate("ADC", 2, 2, 0x69, AddressingMode.Immediate) {
            @Override
            public void operation(CPU cpu) {
                cpu.IMM();
                cpu.ADC();
            }
        },
        ROR_Accumulator("ROR", 1, 2, 0x6A, AddressingMode.Accumulator) {
            @Override
            public void operation(CPU cpu) {
                cpu.ACM();
                cpu.ROR();
            }
        },
        JMP_Indirect("JMP", 3, 5, 0x6C, AddressingMode.Indirect) {
            @Override
            public void operation(CPU cpu) {
                cpu.IND();
                cpu.JMP();
            }
        },
        ADC_Absolute("ADC", 3, 4, 0x6D, AddressingMode.Absolute) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABS();
                cpu.ADC();
            }
        },
        ROR_Absolute("ROR", 3, 6, 0x6E, AddressingMode.Absolute) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABS();
                cpu.ROR();
            }
        },
        BVS_Relative("BVS", 2, 2, 0x70, AddressingMode.Relative) {
            @Override
            public void operation(CPU cpu) {
                cpu.REL();
                cpu.BVS();
            }
        },
        ADC_IndirectIndexedY("ADC", 2, 5, 0x71, AddressingMode.IndirectIndexedY) {
            @Override
            public void operation(CPU cpu) {
                cpu.IZY();
                cpu.ADC();
            }
        },
        ADC_ZeroPageX("ADC", 2, 4, 0x75, AddressingMode.ZeroPageX) {
            @Override
            public void operation(CPU cpu) {
                cpu.ZPX();
                cpu.ADC();
            }
        },
        ROR_ZeroPageX("ROR", 2, 6, 0x76, AddressingMode.ZeroPageX) {
            @Override
            public void operation(CPU cpu) {
                cpu.ZPX();
                cpu.ROR();
            }
        },
        SEI_Implied("SEI", 1, 2, 0x78, AddressingMode.Implied) {
            @Override
            public void operation(CPU cpu) {
                cpu.IMP();
                cpu.SEI();
            }
        },
        ADC_AbsoluteY("ADC", 3, 4, 0x79, AddressingMode.AbsoluteY) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABY();
                cpu.ADC();
            }
        },
        ADC_AbsoluteX("ADC", 3, 4, 0x7D, AddressingMode.AbsoluteX) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABX();
                cpu.ADC();
            }
        },
        ROR_AbsoluteX("ROR", 3, 7, 0x7E, AddressingMode.AbsoluteX) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABX();
                cpu.ROR();
            }
        },
        STA_IndexedIndirectX("STA", 2, 6, 0x81, AddressingMode.IndexedIndirectX) {
            @Override
            public void operation(CPU cpu) {
                cpu.IZX();
                cpu.STA();
            }
        },
        STY_ZeroPage("STY", 2, 3, 0x84, AddressingMode.ZeroPage) {
            @Override
            public void operation(CPU cpu) {
                cpu.ZP0();
                cpu.STY();
            }
        },
        STA_ZeroPage("STA", 2, 3, 0x85, AddressingMode.ZeroPage) {
            @Override
            public void operation(CPU cpu) {
                cpu.ZP0();
                cpu.STA();
            }
        },
        STX_ZeroPage("STX", 2, 3, 0x86, AddressingMode.ZeroPage) {
            @Override
            public void operation(CPU cpu) {
                cpu.ZP0();
                cpu.STX();
            }
        },
        DEY_Implied("DEY", 1, 2, 0x88, AddressingMode.Implied) {
            @Override
            public void operation(CPU cpu) {
                cpu.IMP();
                cpu.DEY();
            }
        },
        TXA_Implied("TXA", 1, 2, 0x8A, AddressingMode.Implied) {
            @Override
            public void operation(CPU cpu) {
                cpu.IMP();
                cpu.TXA();
            }
        },
        STY_Absolute("STY", 3, 4, 0x8C, AddressingMode.Absolute) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABS();
                cpu.STY();
            }
        },
        STA_Absolute("STA", 3, 4, 0x8D, AddressingMode.Absolute) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABS();
                cpu.STA();
            }
        },
        STX_Absolute("STX", 3, 4, 0x8E, AddressingMode.Absolute) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABS();
                cpu.STX();
            }
        },
        BCC_Relative("BCC", 2, 2, 0x90, AddressingMode.Relative) {
            @Override
            public void operation(CPU cpu) {
                cpu.REL();
                cpu.BCC();
            }
        },
        STA_IndirectIndexedY("STA", 2, 6, 0x91, AddressingMode.IndirectIndexedY) {
            @Override
            public void operation(CPU cpu) {
                cpu.IZY();
                cpu.STA();
            }
        },
        STY_ZeroPageX("STY", 2, 4, 0x94, AddressingMode.ZeroPageX) {
            @Override
            public void operation(CPU cpu) {
                cpu.ZPX();
                cpu.STY();
            }
        },
        STA_ZeroPageX("STA", 2, 4, 0x95, AddressingMode.ZeroPageX) {
            @Override
            public void operation(CPU cpu) {
                cpu.ZPX();
                cpu.STA();
            }
        },
        STX_ZeroPageY("STX", 2, 4, 0x96, AddressingMode.ZeroPageY) {
            @Override
            public void operation(CPU cpu) {
                cpu.ZPY();
                cpu.STX();
            }
        },
        TYA_Implied("TYA", 1, 2, 0x98, AddressingMode.Implied) {
            @Override
            public void operation(CPU cpu) {
                cpu.IMP();
                cpu.TYA();
            }
        },
        STA_AbsoluteY("STA", 3, 5, 0x99, AddressingMode.AbsoluteY) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABY();
                cpu.STA();
            }
        },
        TXS_Implied("TXS", 1, 2, 0x9A, AddressingMode.Implied) {
            @Override
            public void operation(CPU cpu) {
                cpu.IMP();
                cpu.TXS();
            }
        },
        STA_AbsoluteX("STA", 3, 5, 0x9D, AddressingMode.AbsoluteX) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABX();
                cpu.STA();
            }
        },
        LDY_Immediate("LDY", 2, 2, 0xA0, AddressingMode.Immediate) {
            @Override
            public void operation(CPU cpu) {
                cpu.IMM();
                cpu.LDY();
            }
        },
        LDA_IndexedIndirectX("LDA", 2, 6, 0xA1, AddressingMode.IndexedIndirectX) {
            @Override
            public void operation(CPU cpu) {
                cpu.IZX();
                cpu.LDA();
            }
        },
        LDX_Immediate("LDX", 2, 2, 0xA2, AddressingMode.Immediate) {
            @Override
            public void operation(CPU cpu) {
                cpu.IMM();
                cpu.LDX();
            }
        },
        LDY_ZeroPage("LDY", 2, 3, 0xA4, AddressingMode.ZeroPage) {
            @Override
            public void operation(CPU cpu) {
                cpu.ZP0();
                cpu.LDY();
            }
        },
        LDA_ZeroPage("LDA", 2, 3, 0xA5, AddressingMode.ZeroPage) {
            @Override
            public void operation(CPU cpu) {
                cpu.ZP0();
                cpu.LDA();
            }
        },
        LDX_ZeroPage("LDX", 2, 3, 0xA6, AddressingMode.ZeroPage) {
            @Override
            public void operation(CPU cpu) {
                cpu.ZP0();
                cpu.LDX();
            }
        },
        TAY_Implied("TAY", 1, 2, 0xA8, AddressingMode.Implied) {
            @Override
            public void operation(CPU cpu) {
                cpu.IMP();
                cpu.TAY();
            }
        },
        LDA_Immediate("LDA", 2, 2, 0xA9, AddressingMode.Immediate) {
            @Override
            public void operation(CPU cpu) {
                cpu.IMM();
                cpu.LDA();
            }
        },
        TAX_Implied("TAX", 1, 2, 0xAA, AddressingMode.Implied) {
            @Override
            public void operation(CPU cpu) {
                cpu.IMP();
                cpu.TAX();
            }
        },
        LDY_Absolute("LDY", 3, 4, 0xAC, AddressingMode.Absolute) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABS();
                cpu.LDY();
            }
        },
        LDA_Absolute("LDA", 3, 4, 0xAD, AddressingMode.Absolute) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABS();
                cpu.LDA();
            }
        },
        LDX_Absolute("LDX", 3, 4, 0xAE, AddressingMode.Absolute) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABS();
                cpu.LDX();
            }
        },
        BCS_Relative("BCS", 2, 2, 0xB0, AddressingMode.Relative) {
            @Override
            public void operation(CPU cpu) {
                cpu.REL();
                cpu.BCS();
            }
        },
        LDA_IndirectIndexedY("LDA", 2, 5, 0xB1, AddressingMode.IndirectIndexedY) {
            @Override
            public void operation(CPU cpu) {
                cpu.IZY();
                cpu.LDA();
            }
        },
        LDY_ZeroPageX("LDY", 2, 4, 0xB4, AddressingMode.ZeroPageX) {
            @Override
            public void operation(CPU cpu) {
                cpu.ZPX();
                cpu.LDY();
            }
        },
        LDA_ZeroPageX("LDA", 2, 4, 0xB5, AddressingMode.ZeroPageX) {
            @Override
            public void operation(CPU cpu) {
                cpu.ZPX();
                cpu.LDA();
            }
        },
        LDX_ZeroPageY("LDX", 2, 4, 0xB6, AddressingMode.ZeroPageY) {
            @Override
            public void operation(CPU cpu) {
                cpu.ZPY();
                cpu.LDX();
            }
        },
        CLV_Implied("CLV", 1, 2, 0xB8, AddressingMode.Implied) {
            @Override
            public void operation(CPU cpu) {
                cpu.IMP();
                cpu.CLV();
            }
        },
        LDA_AbsoluteY("LDA", 3, 4, 0xB9, AddressingMode.AbsoluteY) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABY();
                cpu.LDA();
            }
        },
        TSX_Implied("TSX", 1, 2, 0xBA, AddressingMode.Implied) {
            @Override
            public void operation(CPU cpu) {
                cpu.IMP();
                cpu.TSX();
            }
        },
        LDY_AbsoluteX("LDY", 3, 4, 0xBC, AddressingMode.AbsoluteX) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABX();
                cpu.LDY();
            }
        },
        LDA_AbsoluteX("LDA", 3, 4, 0xBD, AddressingMode.AbsoluteX) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABX();
                cpu.LDA();
            }
        },
        LDX_AbsoluteY("LDX", 3, 4, 0xBE, AddressingMode.AbsoluteY) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABY();
                cpu.LDX();
            }
        },
        CPY_Immediate("CPY", 2, 2, 0xC0, AddressingMode.Immediate) {
            @Override
            public void operation(CPU cpu) {
                cpu.IMM();
                cpu.CPY();
            }
        },
        CMP_IndexedIndirectX("CMP", 2, 6, 0xC1, AddressingMode.IndexedIndirectX) {
            @Override
            public void operation(CPU cpu) {
                cpu.IZX();
                cpu.CMP();
            }
        },
        CPY_ZeroPage("CPY", 2, 3, 0xC4, AddressingMode.ZeroPage) {
            @Override
            public void operation(CPU cpu) {
                cpu.ZP0();
                cpu.CPY();
            }
        },
        CMP_ZeroPage("CMP", 2, 3, 0xC5, AddressingMode.ZeroPage) {
            @Override
            public void operation(CPU cpu) {
                cpu.ZP0();
                cpu.CMP();
            }
        },
        DEC_ZeroPage("DEC", 2, 5, 0xC6, AddressingMode.ZeroPage) {
            @Override
            public void operation(CPU cpu) {
                cpu.ZP0();
                cpu.DEC();
            }
        },
        INY_Implied("INY", 1, 2, 0xC8, AddressingMode.Implied) {
            @Override
            public void operation(CPU cpu) {
                cpu.IMP();
                cpu.INY();
            }
        },
        CMP_Immediate("CMP", 2, 2, 0xC9, AddressingMode.Immediate) {
            @Override
            public void operation(CPU cpu) {
                cpu.IMM();
                cpu.CMP();
            }
        },
        DEX_Implied("DEX", 1, 2, 0xCA, AddressingMode.Implied) {
            @Override
            public void operation(CPU cpu) {
                cpu.IMP();
                cpu.DEX();
            }
        },
        CPY_Absolute("CPY", 3, 4, 0xCC, AddressingMode.Absolute) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABS();
                cpu.CPY();
            }
        },
        CMP_Absolute("CMP", 3, 4, 0xCD, AddressingMode.Absolute) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABS();
                cpu.CMP();
            }
        },
        DEC_Absolute("DEC", 3, 6, 0xCE, AddressingMode.Absolute) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABS();
                cpu.DEC();
            }
        },
        BNE_Relative("BNE", 2, 2, 0xD0, AddressingMode.Relative) {
            @Override
            public void operation(CPU cpu) {
                cpu.REL();
                cpu.BNE();
            }
        },
        CMP_IndirectIndexedY("CMP", 2, 5, 0xD1, AddressingMode.IndirectIndexedY) {
            @Override
            public void operation(CPU cpu) {
                cpu.IZY();
                cpu.CMP();
            }
        },
        CMP_ZeroPageX("CMP", 2, 4, 0xD5, AddressingMode.ZeroPageX) {
            @Override
            public void operation(CPU cpu) {
                cpu.ZPX();
                cpu.CMP();
            }
        },
        DEC_ZeroPageX("DEC", 2, 6, 0xD6, AddressingMode.ZeroPageX) {
            @Override
            public void operation(CPU cpu) {
                cpu.ZPX();
                cpu.DEC();
            }
        },
        CLD_Implied("CLD", 1, 2, 0xD8, AddressingMode.Implied) {
            @Override
            public void operation(CPU cpu) {
                cpu.IMP();
                cpu.CLD();
            }
        },
        CMP_AbsoluteY("CMP", 3, 4, 0xD9, AddressingMode.AbsoluteY) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABY();
                cpu.CMP();
            }
        },
        CMP_AbsoluteX("CMP", 3, 4, 0xDD, AddressingMode.AbsoluteX) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABX();
                cpu.CMP();
            }
        },
        DEC_AbsoluteX("DEC", 3, 7, 0xDE, AddressingMode.AbsoluteX) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABX();
                cpu.DEC();
            }
        },
        CPX_Immediate("CPX", 2, 2, 0xE0, AddressingMode.Immediate) {
            @Override
            public void operation(CPU cpu) {
                cpu.IMM();
                cpu.CPX();
            }
        },
        SBC_IndexedIndirectX("SBC", 2, 6, 0xE1, AddressingMode.IndexedIndirectX) {
            @Override
            public void operation(CPU cpu) {
                cpu.IZX();
                cpu.SBC();
            }
        },
        CPX_ZeroPage("CPX", 2, 3, 0xE4, AddressingMode.ZeroPage) {
            @Override
            public void operation(CPU cpu) {
                cpu.ZP0();
                cpu.CPX();
            }
        },
        SBC_ZeroPage("SBC", 2, 3, 0xE5, AddressingMode.ZeroPage) {
            @Override
            public void operation(CPU cpu) {
                cpu.ZP0();
                cpu.SBC();
            }
        },
        INC_ZeroPage("INC", 2, 5, 0xE6, AddressingMode.ZeroPage) {
            @Override
            public void operation(CPU cpu) {
                cpu.ZP0();
                cpu.INC();
            }
        },
        INX_Implied("INX", 1, 2, 0xE8, AddressingMode.Implied) {
            @Override
            public void operation(CPU cpu) {
                cpu.IMP();
                cpu.INX();
            }
        },
        SBC_Immediate("SBC", 2, 2, 0xE9, AddressingMode.Immediate) {
            @Override
            public void operation(CPU cpu) {
                cpu.IMM();
                cpu.SBC();
            }
        },
        NOP_Implied("NOP", 1, 2, 0xEA, AddressingMode.Implied) {
            @Override
            public void operation(CPU cpu) {
                cpu.IMP();
                cpu.NOP();
            }
        },
        CPX_Absolute("CPX", 3, 4, 0xEC, AddressingMode.Absolute) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABS();
                cpu.CPX();
            }
        },
        SBC_Absolute("SBC", 3, 4, 0xED, AddressingMode.Absolute) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABS();
                cpu.SBC();
            }
        },
        INC_Absolute("INC", 3, 6, 0xEE, AddressingMode.Absolute) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABS();
                cpu.INC();
            }
        },
        BEQ_Relative("BEQ", 2, 2, 0xF0, AddressingMode.Relative) {
            @Override
            public void operation(CPU cpu) {
                cpu.REL();
                cpu.BEQ();
            }
        },
        SBC_IndirectIndexedY("SBC", 2, 5, 0xF1, AddressingMode.IndirectIndexedY) {
            @Override
            public void operation(CPU cpu) {
                cpu.IZY();
                cpu.SBC();
            }
        },
        SBC_ZeroPageX("SBC", 2, 4, 0xF5, AddressingMode.ZeroPageX) {
            @Override
            public void operation(CPU cpu) {
                cpu.ZPX();
                cpu.SBC();
            }
        },
        INC_ZeroPageX("INC", 2, 6, 0xF6, AddressingMode.ZeroPageX) {
            @Override
            public void operation(CPU cpu) {
                cpu.ZPX();
                cpu.INC();
            }
        },
        SED_Implied("SED", 1, 2, 0xF8, AddressingMode.Implied) {
            @Override
            public void operation(CPU cpu) {
                cpu.IMP();
                cpu.SED();
            }
        },
        SBC_AbsoluteY("SBC", 3, 4, 0xF9, AddressingMode.AbsoluteY) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABY();
                cpu.SBC();
            }
        },
        SBC_AbsoluteX("SBC", 3, 4, 0xFD, AddressingMode.AbsoluteX) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABX();
                cpu.SBC();
            }
        },
        INC_AbsoluteX("INC", 3, 7, 0xFE, AddressingMode.AbsoluteX) {
            @Override
            public void operation(CPU cpu) {
                cpu.ABX();
                cpu.INC();
            }
        },
        ;

        private String name;
        private int length;
        private int cycles;
        private int operationCode;
        private AddressingMode addressingMode;
        public void operation(CPU cpu) {

        }

        private Instruction(String name, int len, int cycles, int code, AddressingMode addressingMode) {
            this.name = name;
            this.length = len;
            this.cycles = cycles;
            this.operationCode = code;
            this.addressingMode = addressingMode;
        }


        private final static Instruction[] values = Instruction.values();

        public static Instruction fromCode(int code) {
            Optional<Instruction> instruction = Arrays.stream(values).filter(x -> x.operationCode == code).parallel().findFirst();
            return instruction.isPresent() ? instruction.get() : NOP_Implied;
        }
    }
    // endregion

    // region 寄存器
    // http://wiki.nesdev.com/w/index.php/CPU_registers

    /**
     * Accumulator, 1 byte
     */
    private byte A = 0x00;

    /**
     * Index Register, 1 byte
     */
    private byte X = 0x00, Y = 0x00;

    /**
     * Program Counter，程序计数器，指示下一条指令的地址, 2byte
     */
    private int PC = 0x0000;

    /**
     * Stack pointer, 1 byte, unsigned
     * Java中没有无符号的byte，这里用int代替
     */
    private int S = 0x00;

    /**
     * Status Register, 1 byte
     */
    private byte P = 0x00;

    enum StatusFlag {
        /**
         * Carry，进位标志
         */
        C(1 << 0),
        /**
         * Zero
         */
        Z(1 << 1),
        /**
         * Interrupt Disable，中断禁用标志
         */
        I(1 << 2),
        /**
         * Decimal Mode，十进制模式标志
         */
        D(1 << 3),
        /**
         * Break flag, 1 bit
         */
        B(1 << 4),
        /**
         * Unused
         */
        U(1 << 5),
        /**
         * Overflow，溢出标志
         */
        V(1 << 6),
        /**
         * Negative
         */
        N(1 << 7);

        private int mask;

        private StatusFlag(int m) {
            this.mask = m;
        }
    }

    public void setFlag(StatusFlag flag, int value) {
        if (value == 0) {
            P &= (~flag.mask);
        } else {
            P |= flag.mask;
        }
    }

    public int getFlag(StatusFlag flag) {
        return (P & flag.mask) == 0 ? 0 : 1;
    }

    // endregion

    // region 3种中断

    /**
     * 复位
     * 中断向量为0xFFFC
     */
    void reset() {
        A = 0x00;
        X = 0x00;
        Y = 0x00;
        S = 0x00FD;

        P = 0x00;

//        setFlag(StatusFlag.U, 1);
        setFlag(StatusFlag.I, 1); // 测试用

        PC = read16(0xFFFC);
//        PC = 0xC000; // 调试nestest.nes

        absoluteAddress = 0x0000;
        relativeAddress = 0x00;
        fetched = 0x00;

        cycles += 7;
    }

    /**
     * 不可屏蔽中断 Non-Maskable Interrupt
     * 中断向量为0xFFFA
     */
    void nmi() {
        // 将PC（2字节）写入栈（先写入高8位）
        write(STACK_BASE_ADDRESS + S--, (byte)((PC >>> 8) & 0x00FF));
        write(STACK_BASE_ADDRESS + S--, (byte)(PC & 0x00FF));

        // 设置状态寄存器（发生中断），将状态寄存器写入栈
        setFlag(StatusFlag.B, 0);
        setFlag(StatusFlag.U, 1);
        setFlag(StatusFlag.I, 1);

        write(STACK_BASE_ADDRESS + S--, P);

        // 重置PC
        absoluteAddress = 0xFFFA;
        PC = read16(absoluteAddress);

        cycles += 8;
    }

    /**
     * 可屏蔽中断 Interrupt Request
     * 中断向量为0xFFFE
     */
    void irq() {
        // 标志寄存器中未设置 中断禁用
        // 将当前的现场写入栈
        if (getFlag(StatusFlag.I) == 0) {
            // 将PC（2字节）写入栈（先写入高8位）
            write(STACK_BASE_ADDRESS + S--, (byte)((PC >>> 8) & 0x00FF));
            write(STACK_BASE_ADDRESS + S--, (byte)(PC & 0x00FF));

            // 设置状态寄存器（发生中断），将状态寄存器写入栈
            setFlag(StatusFlag.B, 0);
            setFlag(StatusFlag.U, 1);
            setFlag(StatusFlag.I, 1);

            write(STACK_BASE_ADDRESS + S--, P);

            // 重置PC
            absoluteAddress = 0xFFFE;
            PC = read16(absoluteAddress);

            cycles += 7;
        }
    }

    // endregion

    // region 12种寻址模式 Addressing Modes
    // https://zhuanlan.zhihu.com/p/44051504

    /**
     * 隐含寻址 Implied Addressing 单字节指令
     * 指令隐含了操作地址
     */
    public byte IMP() {
        return 0;
    }

    /**
     * 寄存器寻址 Accumulator Addressing
     * 操作对象为 累加器A
     * @return
     */
    public byte ACM() {
        fetched = (A & 0x00FF);
        return 0;
    }

    /**
     * 立即寻址 Immediate Addressing 双字节
     */
    public byte IMM() {
        absoluteAddress = PC++;
        return 0;
    }

    /**
     * 零页寻址 Zero-page Absolute Addressing 双字节
     * $00 - $FF 页号为0，可省略，使指令变为2字节
     */
    public byte ZP0() {
        absoluteAddress = read(PC++);
        absoluteAddress &= 0x00FF;
        return 0;
    }

    /**
     * 零页X变址 Zero-page X Indexed Addressing 双字节
     */
    public byte ZPX() {
        absoluteAddress = read(PC++) + X;
        absoluteAddress &= 0x00FF;
        return 0;
    }

    /**
     * 零页Y变址 Zero-page Y Indexed Addressing 双字节
     */
    public byte ZPY() {
        absoluteAddress = read(PC++) + Y;
        absoluteAddress &= 0x00FF;
        return 0;
    }

    /**
     * 相对寻址: Relative Addressing 双字节
     * 相对地址是1字节 跳转范围是 -128 ~ 127
     */
    public byte REL() {
        relativeAddress = read(PC++);
//        if ((relativeAddress & 0x80) != 0) { // 检查相对地址是否为负数
//            relativeAddress |= 0xFF00; // 相对地址是1字节，但内存的地址是2字节，将地址的前面置为1表示负数
//        }

        return 0;
    }

    /**
     * 绝对寻址 Absolute Addressing 三字节
     * $DA $12 $F4 将地址为$F412的值加载到寄存器A中
     * 地址是小端序 高位在右
     */
    public byte ABS() {
        absoluteAddress = read16(PC);
        PC += 2;
        return 0;
    }

    /**
     * 绝对X变址 Absolute X Indexed Addressing 三字节
     */
    public byte ABX() {
        int tmp = read16(PC);
        PC += 2;

//        int lowAddress = read(PC++);
//        int highAddress = read(PC++);
//        absoluteAddress = (highAddress << 8) | lowAddress;

        absoluteAddress = (tmp + (X & 0xFF)) & 0xFFFF;

        // 发生内存换页
//        if ((absoluteAddress & 0xFF00) != (highAddress << 8)) {
        if ((absoluteAddress & 0xFF00) != (tmp & 0xFF00)) {
            cycles += 1;
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * 绝对Y变址 Absolute Y Indexed Addressing 三字节
     */
    public byte ABY() {
        int tmp = read16(PC);
        PC += 2;

//        int lowAddress = read(PC++);
//        int highAddress = read(PC++);
//        absoluteAddress = (highAddress << 8) | lowAddress;

        absoluteAddress = (tmp + (Y & 0xFF)) & 0xFFFF;

        // 发生内存换页
        //        if ((absoluteAddress & 0xFF00) != (highAddress << 8)) {
        if ((absoluteAddress & 0xFF00) != (tmp & 0xFF00)) {
            cycles += 1;
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * 间接寻址 Indirect Addressing 三字节
     * $6C $5F $21
     * JMP ($215F) 跳转到 $215F的值指向的地址
     * 需要模拟跨页的硬件bug
     */
    public byte IND() {
        int address = read16(PC);
        PC += 2;

        // 地址跨页一定发生在下一个字节上
        int address2 = (address & 0xFF00) | ((address + 1) & 0x00FF);

        absoluteAddress = ((read(address2) & 0x00FF) << 8) | (read(address) & 0x00FF);
        return 0;
    }

    /**
     * 间接X变址: Pre-indexed Indirect Addressing 双字节
     * $A1 $3E
     * 先与寄存器X变址 $3E + X 的两个字节作为新的地址
     * 再使用这个新地址进行间接寻址
     */
    public byte IZX() {
        int address = read(PC++);

        int lo = read((address + X) & 0x00FF);
        int hi = read((address + X + 1) & 0x00FF);

        absoluteAddress = (((hi & 0x00FF) << 8) | (lo & 0x00FF));

        return 0;
    }

    /**
     * 间接Y变址: Post-indexed Indirect Addressing 双字节
     * 先间接寻址后于寄存器Y变址
     */
    public byte IZY() {
        int address = read(PC++);

        int lo = read(address & 0x00FF); // 零页地址
        int hi = read((address + 1) & 0x00FF);
        absoluteAddress = ((((hi & 0x00FF) << 8) | (lo & 0x00FF)) + (Y & 0xFF)) & 0xFFFF; // 保证absoluteAddress是16位

        if ((absoluteAddress & 0xFF00) != (hi << 8)) {
            cycles += 1;
            return 1;
        } else {
            return 0;
        }
    }

    // endregion

    // region 56种指令模拟
    // http://obelisk.me.uk/6502/reference.html

    /**
     * Add with Carry
     * 带进位加法
     */
    public void ADC() {
        fetch();

        int tmp = (A & 0x00FF) + (fetched & 0x00FF) + getFlag(StatusFlag.C);
        setFlag(StatusFlag.C, tmp > 255 ? 1 : 0);
        setFlag(StatusFlag.Z, (tmp & 0x00FF) == 0 ? 1 : 0);
        setFlag(StatusFlag.N, (tmp & 0x80) == 0 ? 0 : 1);
        // A + M + C = R
        // FLAG_V = (~(A ^ M)) & (A ^ R) & 0x80 只看符号位
        setFlag(StatusFlag.V, ((~(A ^ (fetched & 0x00FF)) & (A ^ (tmp & 0x00FF))) & 0x0080) == 0 ? 0: 1);

        A = (byte)(tmp & 0x00FF);
    }

    /**
     * Logical AND
     */
    public void AND() {
        fetch();
        A &= (fetched & 0x00FF);

        setFlag(StatusFlag.Z, (A & 0x00FF) == 0 ? 1 : 0);
        setFlag(StatusFlag.N, (A & 0x80) == 0 ? 0 : 1);

        cycles++;
    }

    /**
     * Arithmetic Shift Left，算数左移
     * 根据寻址模式的不同，从累加器A或者内存中取值，并相应地将结果写回
     */
    public void ASL() {
        fetch();
        int tmp = (fetched << 1) & 0x0FFF;

        setFlag(StatusFlag.Z, (tmp & 0x00FF) == 0 ? 1 : 0);
        setFlag(StatusFlag.C, (tmp & 0xFF00) > 0 ? 1 : 0);
        setFlag(StatusFlag.N, (tmp & 0x80) == 0 ? 0 : 1);

        if (INSTRUCTION_ADDRESSING_MODE[operationCode] ==
                AddressingMode.Accumulator.key) {
            A = (byte)(tmp & 0x00FF);
        } else {
            write(absoluteAddress, (byte)(tmp & 0x00FF));
        }
    }

    /**
     * Branch if Carry Clear
     * 如果未设置 进位标志FLAG_C，改变当前的PC
     */
    public void BCC() {
        if (getFlag(StatusFlag.C) == 0) {
            cycles++;
            absoluteAddress = PC + relativeAddress;

            // 如果跨页则增加一个时钟周期
            if ((absoluteAddress & 0xFF00) != (PC & 0xFF00)) {
                cycles++;
            }

            PC = absoluteAddress;
        }
    }

    /**
     * Branch if Carry Set
     * 如果设置了 进位标志FLAG_C，改变当前的PC
     */
    public void BCS() {
        if (getFlag(StatusFlag.C) == 1) {
            cycles++;
            absoluteAddress = PC + relativeAddress;

            // 如果跨页则增加一个时钟周期
            if ((absoluteAddress & 0xFF00) != (PC & 0xFF00)) {
                cycles++;
            }

            PC = absoluteAddress;
        }
    }

    /**
     * Branch if Equal
     * 如果设置了 零标志FLAG_Z，改变当前的PC
     */
    public void BEQ() {
        if (getFlag(StatusFlag.Z) == 1) {
            cycles++;
            absoluteAddress = (PC + relativeAddress) & 0xFFFF;

            // 如果跨页则增加一个时钟周期
            if ((absoluteAddress & 0xFF00) != (PC & 0xFF00)) {
                cycles++;
            }

            PC = absoluteAddress;
        }
    }

    /**
     * Bit Test
     * A & M, N = M7, V = M6
     * 测试目标内存位置中是否设置了一个或多个位，mask存储于A中，设置标志位，不保留结果
     */
    public void BIT() {
        fetch();
        int tmp = (fetched & 0x00FF) & A;

        setFlag(StatusFlag.Z, (tmp & 0x00FF) == 0 ? 1 : 0);
        setFlag(StatusFlag.N, (fetched & 0x80) == 0 ? 0 : 1);
        setFlag(StatusFlag.V, (fetched & 0x40) == 0 ? 0 : 1);
    }

    /**N
     * Branch if Minus
     * 如果设置了 标志FLAG_N，改变当前的PC
     */
    public void BMI() {
        if (getFlag(StatusFlag.N) == 1) {
            cycles++;
            absoluteAddress = PC + relativeAddress;

            // 如果跨页则增加一个时钟周期
            if ((absoluteAddress & 0xFF00) != (PC & 0xFF00)) {
                cycles++;
            }

            PC = absoluteAddress;
        }
    }

    /**
     * Branch if Not Equal
     * 如果未设置 零标志FLAG_Z，改变当前的PC
     */
    public void BNE() {
        if (getFlag(StatusFlag.Z) == 0) {
            cycles++;
            absoluteAddress = PC + relativeAddress;

            // 如果跨页则增加一个时钟周期
            if ((absoluteAddress & 0xFF00) != (PC & 0xFF00)) {
                cycles++;
            }

            PC = absoluteAddress;
        }
    }

    /**
     * Branch if Positive
     * 如果未设置 标志FLAG_N，改变当前的PC
     */
    public void BPL() {
        if (getFlag(StatusFlag.N) == 0) {
            cycles++;
            absoluteAddress = PC + relativeAddress;

            // 如果跨页则增加一个时钟周期
            if ((absoluteAddress & 0xFF00) != (PC & 0xFF00)) {
                cycles++;
            }

            PC = absoluteAddress;
        }
    }

    /**
     * Force Interrupt，强制生成中断请求
     * 保存现场到栈中，设置PC为中断向量
     */
    public void BRK() {
        PC++;
        setFlag(StatusFlag.I, 1);

        // 保存现场
        // 将PC（2字节）写入栈（先写入高8位）
        write(STACK_BASE_ADDRESS + S--, (byte)((PC >>> 8) & 0x00FF));
        write(STACK_BASE_ADDRESS + S--, (byte)(PC & 0x00FF));

        // 设置状态寄存器（发生中断），将状态寄存器写入栈
        setFlag(StatusFlag.B, 1);
        write(STACK_BASE_ADDRESS + S--, P);
        setFlag(StatusFlag.B, 0);

        PC = read16(0xFFFE);
    }

    /**
     * Branch if Overflow Clear
     * 如果未设置 溢出标志FLAG_V，改变当前的PC
     */
    public void BVC() {
        if (getFlag(StatusFlag.V) == 0) {
            cycles++;
            absoluteAddress = PC + relativeAddress;

            // 如果跨页则增加一个时钟周期
            if ((absoluteAddress & 0xFF00) != (PC & 0xFF00)) {
                cycles++;
            }

            PC = absoluteAddress;
        }
    }

    /**
     * Branch if Overflow Set
     * 如果设置了 溢出标志FLAG_V，改变当前的PC
     */
    public void BVS() {
        if (getFlag(StatusFlag.V) == 1) {
            cycles++;
            absoluteAddress = PC + relativeAddress;

            // 如果跨页则增加一个时钟周期
            if ((absoluteAddress & 0xFF00) != (PC & 0xFF00)) {
                cycles++;
            }

            PC = absoluteAddress;
        }
    }

    /**
     * Clear Carry Flag
     * 清除进位标志
     */
    public void CLC() {
        setFlag(StatusFlag.C, 0);
    }

    /**
     * Clear Decimal Mode
     * 清除十进制标志位 FLAG_D
     */
    public void CLD() {
        setFlag(StatusFlag.D, 0);
    }

    /**
     * Clear Interrupt Disable
     * 清除中断禁用标志，允许正常中断
     */
    public void CLI() {
        setFlag(StatusFlag.I, 0);
    }

    /**
     * Clear Overflow Flag
     * 清除溢出标志位
     */
    public void CLV() {
        setFlag(StatusFlag.V, 0);
    }

    /**
     * Compare，Z,C,N = A-M
     * 比较A和内存中的值，并设置标志位
     */
    public void CMP() {
        fetch();
        int result = (A & 0x00FF) - (fetched & 0x00FF);

        setFlag(StatusFlag.C, result >= 0 ? 1 : 0);
        setFlag(StatusFlag.Z, result == 0 ? 1 : 0);

        setFlag(StatusFlag.N, (result & 0x80) == 0 ? 0 : 1);

        cycles++;
    }

    /**
     * Compare X Register，Z,C,N = X-M
     * 比较X和内存中的值，并设置标志位
     */
    public void CPX() {
        fetch();
        int result = (X & 0x00FF) - (fetched & 0x00FF);

        setFlag(StatusFlag.C, result >= 0 ? 1 : 0);
        setFlag(StatusFlag.Z, result == 0 ? 1 : 0);

        setFlag(StatusFlag.N, (result & 0x80) == 0 ? 0 : 1);
    }

    /**
     * Compare Y Register，Z,C,N = Y-M
     * 比较Y和内存中的值，并设置标志位
     */
    public void CPY() {
        fetch();
        int result = (Y & 0x00FF) - (fetched & 0x00FF);

        setFlag(StatusFlag.C, result >= 0 ? 1 : 0);
        setFlag(StatusFlag.Z, result == 0 ? 1 : 0);

        setFlag(StatusFlag.N, (result & 0x80) == 0 ? 0 : 1);
    }

    /**
     * Decrement Memory, M,Z,N = M-1
     * 将内存中的值 -1，并设置标志位
     */
    public void DEC() {
        fetch();

        int result = (fetched & 0x00FF) - 1;
        write(absoluteAddress, (byte)(result & 0x00FF));
        setFlag(StatusFlag.Z, (result & 0x00FF) == 0 ? 1 : 0);
        setFlag(StatusFlag.N, (result & 0x80) == 0 ? 0 : 1);
    }

    /**
     * Decrement X Register
     * X,Z,N = X-1
     * 将X的值 -1，并设置标志位
     */
    public void DEX() {
        X--;

        setFlag(StatusFlag.Z, X == 0 ? 1 : 0);
        setFlag(StatusFlag.N, (X & 0x80) == 0 ? 0 : 1);
    }

    /**
     * Decrement Y Register
     * Y,Z,N = Y-1
     * 将Y的值 -1，并设置标志位
     */
    public void DEY() {
        Y--;

        setFlag(StatusFlag.Z, Y == 0 ? 1 : 0);
        setFlag(StatusFlag.N, (Y & 0x80) == 0 ? 0 : 1);
    }

    /**
     * Exclusive OR
     * A,Z,N = A^M
     * 使用一字节内存的内容对A执行异或
     */
    public void EOR() {
        fetch();

        A ^= (fetched & 0x00FF);
        setFlag(StatusFlag.Z, A == 0 ? 1 : 0);
        setFlag(StatusFlag.N, (A & 0x80) == 0 ? 0 : 1);

        cycles++;
    }

    /**
     * Increment Memory
     * M,Z,N = M+1
     * 将内存中的值 +1，并设置标志位
     */
    public void INC() {
        fetch();

        int result = (fetched & 0x00FF) + 1;
        write(absoluteAddress, (byte)(result & 0x00FF));
        setFlag(StatusFlag.Z, (result & 0x00FF) == 0 ? 1 : 0);
        setFlag(StatusFlag.N, (result & 0x80) == 0 ? 0 : 1);
    }

    /**
     * Increment X Register
     * X,Z,N = X+1
     * 将X的值 +1，并设置标志位
     */
    public void INX() {
        X++;

        setFlag(StatusFlag.Z, X == 0 ? 1 : 0);
        setFlag(StatusFlag.N, (X & 0x80) == 0 ? 0 : 1);
    }

    /**
     * Increment Y Register
     * Y,Z,N = Y+1
     * 将Y的值 +1，并设置标志位
     */
    public void INY() {
        Y++;

        setFlag(StatusFlag.Z, Y == 0 ? 1 : 0);
        setFlag(StatusFlag.N, (Y & 0x80) == 0 ? 0 : 1);
    }

    /**
     * Jump
     * 跳转指令
     */
    public void JMP() {
        PC = absoluteAddress;
    }

    /**
     * Jump to Subroutine
     * Push current PC to stack, PC = address
     */
    public void JSR() {
        // PC指向的是下一条指令的地址
        PC--;

        write(STACK_BASE_ADDRESS + S--, (byte)((PC >>> 8) & 0x00FF));
        write(STACK_BASE_ADDRESS + S--, (byte)(PC & 0x00FF));

        PC = absoluteAddress;
    }

    /**
     * Load Accumulator
     * A,Z,N = M
     * 将1字节的内存数据 加载到 A 中
     */
    public void LDA() {
        fetch();

        A = (byte) (fetched & 0x00FF);

        setFlag(StatusFlag.Z, A == 0 ? 1 : 0);
        setFlag(StatusFlag.N, (A & 0x80) == 0 ? 0 : 1);
    }

    /**
     * Load X Register
     * X,Z,N = M
     * 将1字节的内存数据 加载到 X 中
     */
    public void LDX() {
        fetch();

        X = (byte) (fetched & 0x00FF);

        setFlag(StatusFlag.Z, X == 0 ? 1 : 0);
        setFlag(StatusFlag.N, (X & 0x80) == 0 ? 0 : 1);

        cycles++;
    }

    /**
     * Load Y Register
     * Y,Z,N = M
     * 将1字节的内存数据 加载到 Y 中
     */
    public void LDY() {
        fetch();

        Y = (byte)(fetched & 0x00FF);

        setFlag(StatusFlag.Z, Y == 0 ? 1 : 0);
        setFlag(StatusFlag.N, (Y & 0x80) == 0 ? 0 : 1);
    }

    /**
     * Logical Shift Right
     * A,C,Z,N = A/2 or M,C,Z,N = M/2
     * A或M 逻辑右移，左边补0
     */
    public void LSR() {
        fetch();

        setFlag(StatusFlag.C, (fetched & 0x0001) == 0 ? 0 : 1);
        int tmp = (fetched & 0x00FF) >>> 1;
        setFlag(StatusFlag.Z, (tmp & 0x00FF) == 0 ? 1 : 0);
        setFlag(StatusFlag.N, (tmp & 0x0080) == 0 ? 0 : 1);

        if (INSTRUCTION_ADDRESSING_MODE[operationCode] ==
                AddressingMode.Accumulator.key) {
            A = (byte)(tmp & 0x00FF);
        } else {
            write(absoluteAddress, (byte)(tmp & 0x00FF));
        }
    }

    /**
     * No Operation
     */
    public void NOP() {
        switch (operationCode) {
            case 0x1C:
            case 0x3C:
            case 0x5C:
            case 0x7C:
            case 0xDC:
            case 0xFC:
                cycles++;
            break;
        }
    }

    /**
     * Logical Inclusive OR
     * A,Z,N = A|M
     * 使用1字节内存的值 对 A 执行或
     */
    public void ORA() {
        fetch();

        A = (byte)(A | (fetched & 0x00FF));
        setFlag(StatusFlag.Z, A == 0 ? 1 : 0);
        setFlag(StatusFlag.N, (A & 0x80) == 0 ? 0 : 1);

        cycles++;
    }

    /**
     * Push Accumulator
     * 将 累加器A的值push到栈中
     */
    public void PHA() {
        write(STACK_BASE_ADDRESS + S--, A);
    }

    /**
     * Push Processor Status
     * 将 状态寄存器 的值push到栈中
     */
    public void PHP() {
        setFlag(StatusFlag.B, 1);
        setFlag(StatusFlag.U, 1);

        write(STACK_BASE_ADDRESS + S--, P);
        setFlag(StatusFlag.B, 0);
        setFlag(StatusFlag.U, 0);
    }

    /**
     * Pull Accumulator
     * 从栈中pull 8bit的值 写入累加器A中
     */
    public void PLA() {
        S++;
        A = read(STACK_BASE_ADDRESS + S);

        setFlag(StatusFlag.Z, A == 0x00 ? 1 : 0);
        setFlag(StatusFlag.N, (A & 0x80) == 0 ? 0 : 1);
    }

    /**
     * Pull Processor Status
     * 从栈取出 1字节 设置为标志位
     */
    public void PLP() {
        S++;
        P = read(STACK_BASE_ADDRESS + S);
        setFlag(StatusFlag.U, 1);
    }

    /**
     * Rotate Left
     * 将 A 或 M 中的每个位向左移动一处
     * 右边补0，符号位作为 flagC
     */
    public void ROL() {
        fetch();

        int result = ((fetched << 1) & 0x0FFF) | getFlag(StatusFlag.C); // 保证fetched << 1是正数
        setFlag(StatusFlag.C, (result & 0xFF00) > 0 ? 1 : 0);
        setFlag(StatusFlag.Z, (result & 0x00FF) == 0 ? 1 : 0);
        setFlag(StatusFlag.N, (result & 0x80) == 0 ? 0 : 1);

        if (INSTRUCTION_ADDRESSING_MODE[operationCode] ==
                AddressingMode.Accumulator.key) {
            A = (byte)(result & 0x00FF);
        } else {
            write(absoluteAddress, (byte)(result & 0x00FF));
        }
    }

    /**
     * Rotate Right
     * 将 A 或 M 右移一位，左边填充进位标志的当前值，而旧位0成为新进位标志值
     */
    public void ROR() {
        fetch();

        int result = (fetched >>> 1) | ((getFlag(StatusFlag.C) << 7) & 0x0FFF);
        setFlag(StatusFlag.C, (fetched & 0x01) == 0 ? 0 : 1);
        setFlag(StatusFlag.Z, (result & 0x00FF) == 0 ? 1 : 0);
        setFlag(StatusFlag.N, (result & 0x80) == 0 ? 0 : 1);

        if (INSTRUCTION_ADDRESSING_MODE[operationCode] ==
                AddressingMode.Accumulator.key) {
            A = (byte)(result & 0x00FF);
        } else {
            write(absoluteAddress, (byte)(result & 0x00FF));
        }
    }

    /**
     * Return from Interrupt
     * 恢复中断后的现场
     */
    public void RTI() {
        S++;
        P = read(STACK_BASE_ADDRESS + S);
        setFlag(StatusFlag.B, 0);
        setFlag(StatusFlag.U, 0);

        S++;
        PC = read16(STACK_BASE_ADDRESS + S++);
    }

    /**
     * Return from Subroutine
     * 在子例程的末尾返回到调用例程。它从堆栈中提取 PC-1
     */
    public void RTS() {
        S++;
        PC = read16(STACK_BASE_ADDRESS + S++);

        PC++;
    }

    /**
     * Subtract with Carry，A = A-M-(1-C)
     * -M是M按位取反再+1，原式 = A + (M按位取反) + C
     * 带进位减法
     */
    public void SBC() {
        fetch();

        // M按位取反
        int value = (fetched & 0x00FF) ^ 0x00FF;

        int tmp = (A & 0x00FF) + value + getFlag(StatusFlag.C);

        setFlag(StatusFlag.C, (tmp & 0xFF00) == 0 ? 0 : 1);
        setFlag(StatusFlag.Z, (tmp & 0x00FF) == 0 ? 1 : 0);
        setFlag(StatusFlag.V, ((tmp ^ (A & 0x00FF)) & (tmp ^ value) & 0x0080) == 0 ? 0 : 1);
        setFlag(StatusFlag.N, (tmp & 0x80) == 0 ? 0 : 1);

        A = (byte)(tmp & 0x00FF);

        cycles += 1;
    }

    /**
     * Set Carry Flag
     * C = 1
     */
    public void SEC() {
        setFlag(StatusFlag.C, 1);
    }

    /**
     * Set Decimal Flag
     * D = 1
     */
    public void SED() {
        setFlag(StatusFlag.D, 1);
    }

    /**
     * Set Interrupt Disable
     * I = 1
     */
    public void SEI() {
        setFlag(StatusFlag.I, 1);
    }

    /**
     * Store Accumulator
     * M = A
     * 将A 写入 内存
     */
    public void STA() {
        write(absoluteAddress, A);
    }

    /**
     * Store X Register
     * M = X
     */
    public void STX() {
        write(absoluteAddress, X);
    }

    /**
     * Store Y Register
     * M = Y
     */
    public void STY() {
        write(absoluteAddress, Y);
    }

    /**
     * Transfer Accumulator to X
     * X = A
     * 将 A 赋值给 X，并设置标志位
     */
    public void TAX() {
        X = A;
        setFlag(StatusFlag.Z, X == 0 ? 1 : 0);
        setFlag(StatusFlag.N, (X & 0x80) == 0 ? 0 : 1);
    }

    /**
     * Transfer Accumulator to Y
     * Y = A
     */
    public void TAY() {
        Y = A;
        setFlag(StatusFlag.Z, Y == 0 ? 1 : 0);
        setFlag(StatusFlag.N, (Y & 0x80) == 0 ? 0 : 1);
    }

    /**
     * Transfer Stack Pointer to X
     * X = S
     */
    public void TSX() {
        X = (byte)(S & 0x00FF);
        setFlag(StatusFlag.Z, X == 0 ? 1 : 0);
        setFlag(StatusFlag.N, (X & 0x80) == 0 ? 0 : 1);
    }

    /**
     *  Transfer X to Accumulator
     * A = X
     */
    public void TXA() {
        A = X;
        setFlag(StatusFlag.Z, A == 0 ? 1 : 0);
        setFlag(StatusFlag.N, (A & 0x80) == 0 ? 0 : 1);
    }

    /**
     * Transfer X to Stack Pointer
     * S = X
     */
    public void TXS() {
        S = (0x00FF & X);
    }

    /**
     * Transfer Y to Accumulator
     * A = Y
     */
    public void TYA() {
        A = Y;
        setFlag(StatusFlag.Z, A == 0 ? 1 : 0);
        setFlag(StatusFlag.N, (A & 0x80) == 0 ? 0 : 1);
    }

    // endregion

    // region 非官方指令

    /**
     * Load A then transfer to X
     */
    public void LAX() {
        LDA();
        X = A;

        setFlag(StatusFlag.Z, X == 0 ? 1 : 0);
        setFlag(StatusFlag.N, (X & 0x80) == 0 ? 0 : 1);
    }

    // endregion

    void diasm(byte[] codes) {
        int len = codes.length;

        int start = 0;

        while (start < len - 1) {
            byte operationCode = codes[start];

            String operationName = INSTRUCTION_SET[operationCode & 0xFF];
            int operationLength = INSTRUCTION_LENGTH[operationCode & 0xFF];

            String operationString = String.format("%02X", operationCode);
            String parameters = "";

            for (int i = 1; i < operationLength; i++) {
                parameters = String.format("%02X", codes[start+i]) + parameters;
                operationString += " " + String.format("%02X", codes[start+i]);
            }

            if (!parameters.isEmpty()) {
                parameters = "$" + parameters;
            }

            System.out.println(String.format("%04X: %-8s \t%s %s", start, operationString, operationName, parameters));
            start += operationLength;
        }
    }

    int fetch() {
        if (INSTRUCTION_ADDRESSING_MODE[operationCode] != AddressingMode.Implied.key &&
                INSTRUCTION_ADDRESSING_MODE[operationCode] != AddressingMode.Accumulator.key) {
            fetched = read(absoluteAddress);
        }

        return fetched;
    }

    void clock() {
        int tmpPC = PC;
        operationCode = read(PC++) & 0x00FF;
        setFlag(StatusFlag.U, 1);

        // 获取指令
        Instruction instruction = Instruction.fromCode(operationCode);

        if (logging) {
            String operationName = INSTRUCTION_SET[operationCode & 0xFF];
            int operationLength = INSTRUCTION_LENGTH[operationCode & 0xFF];

            StringBuilder operationCodeString = new StringBuilder(String.format("%02X", operationCode));
            StringBuilder parameters = new StringBuilder();
            if (INSTRUCTION_ADDRESSING_MODE[operationCode & 0xFF] == AddressingMode.Accumulator.key) {
                parameters.insert(0, "A");
            } else {
                for (int i = 1; i < operationLength; i++) {
                    parameters.insert(0, String.format("%02X", read(tmpPC + i)));
                    operationCodeString.append(" " + String.format("%02X", read(tmpPC + i)));
                }

                if (operationLength != 1) {
                    parameters.insert(0,  "$");
//                    parameters.append(String.format(" = %02X", fetched));
                }

                if (instruction.addressingMode == AddressingMode.Immediate) {
                    parameters.insert(0, "#");
                }
            }

//            System.out.println(String.format("%04X  %-8s \t%s %-26s  A:%02X X:%02X Y:%02X P:%02X SP:%02X PPU: %04X CYC: %d",
//                    tmpPC, operationCodeString.toString(), operation, parameters, A, X, Y, P, (byte)S, bus.ppu.getAddress(), cycles));

            System.out.println(String.format("%04X  %-8s  %s %-26s  A:%02X X:%02X Y:%02X P:%02X SP:%02X CYC:%d",
                    tmpPC, operationCodeString.toString(), operationName, parameters.toString(), A, X, Y, P, (byte)S, cycles));
        }

        // 执行指令（包含了寻址过程）
        instruction.operation(CPU.this);
        cycles += instruction.cycles;

        setFlag(StatusFlag.U, 1);

        clockCount++;
    }
}
