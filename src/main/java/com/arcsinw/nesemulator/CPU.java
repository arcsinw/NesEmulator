package com.arcsinw.nesemulator;

/**
 * 2A03的CPU模拟
 */
public class CPU {

    Bus bus;

    void setBus(Bus b) {
        this.bus = b;
    }

    byte read(int address) {
        return bus.read(address);
    }

    void write(int address, byte data) {
        bus.write(address, data);
    }

    class Instruction {
        String name;
        int length;
        int cycle;
    }

    // region 6502 Instruction set
    // https://pastraiser.com/cpu/6502/6502_opcodes.html

    /**
     * 指令表，为16x16的矩阵，横坐标和纵坐标为0-F，这里使用一维数组表示
     * 例如， 0x24是BIT，0x24的十进制为36，在数组中的下标也是36
     */
    private final String[] INSTRUCTION_SET = {
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
    private final int[] INSTRUCTION_LENGTH = {
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
     * 指令的寻址模式
     */
    private final int[] INSTRUCTION_ADDRESSING_MODE = {
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


    // endregion

    // region 寄存器

    /**
     * Accumulator, 1 byte
     */
    private int A = 0x00;

    /**
     * Index Register, 1 byte
     */
    private int X = 0x00, Y = 0x00;

    /**
     * Program Counter，程序计数器，指示下一条指令的地址, 2byte
     */
    private int PC = 0x0000;

    /**
     * Stack pointer, 1 byte
     */
    private int S = 0x00;

    /**
     * Status Register, 1 byte
     */
    private int P = 0x00;

    // endregion

    // region 3种中断

    /**
     * 复位
     */
    void reset() {
    }

    /**
     * 不可屏蔽中断 Non-Maskable Interrupt
     */
    void nmi() {
    }

    /**
     * 可屏蔽中断 Interrupt Request
     */
    void irq() {
    }

    // endregion

    // region 7种CPU Flags

    /**
     * Carry，进位标志
     */
    public static byte FLAG_C = 0;

    /**
     * Zero
     */
    public static byte FLAG_Z = 0;

    /**
     * Interrupt Disable
     */
    public static byte FLAG_I = 0;

    /**
     * Decimal Mode
     */
    public static byte FLAG_D = 0;

    /**
     * B flag, 2 bit
     */
    public static byte FLAG_B = 0;

    /**
     * Overflow
     */
    public static byte FLAG_V = 0;

    /**
     * Negative
     */
    public static byte FLAG_N = 0;

    // endregion

    // region 12种寻址模式 Addressing Modes
    // https://zhuanlan.zhihu.com/p/44051504

    /**
     * 隐含寻址 Implied Addressing 单字节指令
     */
    public byte IMP() {
        fetched = A;
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
        absoluteAddress = read(PC);
        PC++;
        absoluteAddress &= 0x00FF;
        return 0;
    }

    /**
     * 零页X变址 Zero-page X Indexed Addressing 双字节
     */
    public byte ZPX() {
        absoluteAddress = read(PC) + X;
        PC++;
        absoluteAddress &= 0x00FF;
        return 0;
    }

    /**
     * 零页Y变址 Zero-page Y Indexed Addressing 双字节
     */
    public byte ZPY() {
        absoluteAddress = read(PC) + Y;
        PC++;
        absoluteAddress &= 0x00FF;
        return 0;
    }

    /**
     * 相对寻址: Relative Addressing 双字节
     * 相对地址是1字节 跳转范围是 -128 ~ 127
     */
    public byte REL() {
        relativeAddress = read(PC++);
        if ((relativeAddress & 0x80) != 0) { // 检查相对地址是否为负数
            relativeAddress |= 0xFF00; // 相对地址是1字节，但内存的地址是2字节，将地址的前面置为1表示负数
        }

        return 0;
    }

    /**
     * 绝对寻址 Absolute Addressing 三字节
     * $DA $12 $F4 将地址为$F412的值加载到寄存器A中
     * 地址是小端序 高位在右
     */
    public byte ABS() {
        int lowAddress = read(PC++);
        int highAddress = read(PC++);
        absoluteAddress = (highAddress << 8) | lowAddress;
        return 0;
    }

    /**
     * 绝对X变址 Absolute X Indexed Addressing 三字节
     */
    public byte ABX() {
        int lowAddress = read(PC++);
        int highAddress = read(PC++);

        absoluteAddress = (highAddress << 8) | lowAddress;
        absoluteAddress += X;

        // 发生内存换页
        if ((absoluteAddress & 0xFF00) != (highAddress << 8)) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * 绝对Y变址 Absolute Y Indexed Addressing 三字节
     */
    public byte ABY() {
        int lowAddress = read(PC++);
        int highAddress = read(PC++);

        absoluteAddress = (highAddress << 8) | lowAddress;
        absoluteAddress += Y;

        // 发生内存换页
        if ((absoluteAddress & 0xFF00) != (highAddress << 8)) {
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
        int low = read(PC++);
        int high = read(PC++);
        int address = (high << 8) | low;

        // 地址跨页一定发生在下一个字节上
        int address2 = (address & 0xFF00) | ((address + 1) & 0x00FF);

        absoluteAddress = (read(address2) << 8) | read(address);
        return 0;
    }

    /**
     * 间接X变址: Pre-indexed Indirect Addressing 双字节
     * $A1 $3E
     * 先与寄存器X变址 $3E + X 的两个字节作为新的地址
     * 再使用这个新地址进行间接寻址
     */
    public byte IZX() {
        int address = read(PC++) + X;
        absoluteAddress = read(address) | read(address + 1) << 8;
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
        absoluteAddress = ((hi << 8) | lo) + Y;

        if ((absoluteAddress & 0xFF00) != (hi << 8)) {
            return 1;
        } else {
            return 0;
        }
    }

    // endregion

    private AddressingMode[] tb = new AddressingMode[] {
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

    enum AddressingMode {
        Implied(0),
        Accumulator(1),
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
    }

    // region 56种指令模拟

    /**
     * Add with Carry
     */
    public void ADC() {
    }

    /**
     * Logical AND
     */
    public void AND(int cycles, byte address) {

        A = A & read(address);

        FLAG_Z = (byte) (A == 0x00 ? 1 : 0);
        FLAG_N = (byte) (A & 0x80);
    }

    public void ASL() {
    }

    public void BCC() {
    }

    public void BCS() {
    }

    public void BEQ() {
    }

    public void BIT() {
    }

    public void BMI() {
    }

    public void BNE() {
    }

    public void BPL() {
    }

    public void BRK() {
    }

    public void BVC() {
    }

    public void BVS() {
    }

    public void CLC() {
    }

    public void CLD() {
    }

    public void CLI() {
    }

    public void CLV() {
    }

    public void CMP() {
    }

    public void CPX() {
    }

    public void CPY() {
    }

    public void DEC() {
    }

    public void DEX() {
    }

    public void DEY() {
    }

    public void EOR() {
    }

    public void INC() {
    }

    public void INX() {
    }

    public void INY() {
    }

    public void JMP() {
    }

    public void JSR() {
    }

    public void LDA() {
    }

    public void LDX() {
    }

    public void LDY() {
    }

    public void LSR() {
    }

    public void NOP() {
    }

    public void ORA() {
    }

    public void PHA() {
    }

    public void PHP() {
    }

    public void PLA() {
    }

    public void PLP() {
    }

    public void ROL() {
    }

    public void ROR() {
    }

    public void RTI() {
    }

    public void RTS() {
    }

    public void SBC() {
    }

    public void SEC() {
    }

    public void SED() {
    }

    public void SEI() {
    }

    public void STA() {
    }

    public void STX() {
    }

    public void STY() {
    }

    public void TAX() {
    }

    public void TAY() {
    }

    public void TSX() {
    }

    public void TXA() {
    }

    public void TXS() {
    }

    public void TYA() {
    }
    // endregion

    void diasm(byte[] codes) {
        int len = codes.length;

        int start = 0;

        while (start < len - 1) {
            byte operationCode = codes[start];

            String operation = INSTRUCTION_SET[operationCode & 0xFF];
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

            System.out.println(String.format("%04X: %-8s \t%s %s", start, operationString, operation, parameters));
            start += operationLength;
        }
    }

    void fetch() {
    }

    int fetched = 0x00;

    int absoluteAddress = 0x0000;
    int relativeAddress = 0x00;
    int cycles = 0;
    int operationCode = 0x00;
}
