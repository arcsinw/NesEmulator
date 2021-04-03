package com.arcsinw.nesemulator;

/**
 * 2A03的CPU模拟
 */
public class CPU {

    Bus bus;

    int fetched = 0x00;

    int absoluteAddress = 0x0000;
    int relativeAddress = 0x00;
    int cycles = 0;
    int operationCode = 0x00;

    // region debug only

    int clockCount = 0;

    // endregion

    void setBus(Bus b) {
        this.bus = b;
    }

    byte read(int address) {
        return bus.read(address);
    }

    int read16(int address) {
        byte lo = read(address);
        byte hi = read(address + 1);
        return (hi << 8) | lo;
    }

    void write(int address, byte data) {
        bus.write(address, data);
    }

    final int STACK_BASE_ADDRESS = 0x0100;

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
     * 指令的周期
     */
    private final int[] INSTRUCTION_CYCLE = {
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

    /**
     * 寻址模式索引表
     */
    private final AddressingMode[] ADDRESSING_MODE_TABLE = new AddressingMode[] {
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

        private final static AddressingMode[] values = AddressingMode.values();

        public static AddressingMode fromIndex(int k) {
            return AddressingMode.values()[k];
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
     * Stack pointer, 1 byte
     */
    private byte S = 0x00;

    /**
     * Status Register, 1 byte
     */
    private byte P = 0x00;

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
        S = (byte) 0xFD;

        flagN = flagV = flagZ = flagC = flagD = 0;
        flagB = flagI = 1;

        absoluteAddress = 0xFFFC;
        PC = read16(absoluteAddress);

        absoluteAddress = 0x0000;
        relativeAddress = 0x0000;
        fetched = 0x00;

        cycles = 8;
    }

    /**
     * 不可屏蔽中断 Non-Maskable Interrupt
     * 中断向量为0xFFFA
     */
    void nmi() {
        // 将PC（2字节）写入栈（先写入高8位）
        write(STACK_BASE_ADDRESS + S--, (byte)((PC >> 8) & 0x00FF));
        write(STACK_BASE_ADDRESS + S--, (byte)(PC & 0x00FF));

        // 设置状态寄存器（发生中断），将状态寄存器写入栈
        flagB = 0;
        flagU = 1;
        flagI = 1;
        byte status = (byte)(flagC | (flagZ << 1) | (flagI << 2) | (flagD << 3) |
                (flagB << 4) | (flagU << 5) | (flagV << 6) | (flagN << 7));
        write(STACK_BASE_ADDRESS + S--, status);

        // 重置PC
        absoluteAddress = 0xFFFA;
        PC = read16(absoluteAddress);

        cycles = 8;
    }

    /**
     * 可屏蔽中断 Interrupt Request
     * 中断向量为0xFFFE
     */
    void irq() {
        // 标志寄存器中未设置 中断禁用
        // 将当前的现场写入栈
        if (flagI == 0) {
            // 将PC（2字节）写入栈（先写入高8位）
            write(STACK_BASE_ADDRESS + S--, (byte)((PC >> 8) & 0x00FF));
            write(STACK_BASE_ADDRESS + S--, (byte)(PC & 0x00FF));

            // 设置状态寄存器（发生中断），将状态寄存器写入栈
            flagB = 0;
            flagU = 1;
            flagI = 1;
            byte status = (byte)(flagC | (flagZ << 1) | (flagI << 2) | (flagD << 3) |
                    (flagB << 4) | (flagU << 5) | (flagV << 6) | (flagN << 7));
            write(STACK_BASE_ADDRESS + S--, status);

            // 重置PC
            absoluteAddress = 0xFFFE;
            PC = read16(absoluteAddress);

            cycles = 7;
        }
    }

    // endregion

    // region 7种CPU Flags

    /**
     * Carry，进位标志
     * 1 << 0
     */
    public byte flagC = 0;

    /**
     * Zero
     * 1 << 1
     */
    public byte flagZ = 0;

    /**
     * Interrupt Disable，中断禁用标志
     * 1 << 2
     */
    public byte flagI = 0;

    /**
     * Decimal Mode，十进制模式标志
     * 1 << 3
     */
    public byte flagD = 0;

    /**
     * B flag, 1 bit
     * 1 << 4
     */
    public byte flagB = 0;

    /**
     * Unused
     * 1 << 5
     */
    public byte flagU = 0;

    /**
     * Overflow，溢出标志
     * 1 << 6
     */
    public byte flagV = 0;

    /**
     * Negative
     * 1 << 7
     */
    public byte flagN = 0;

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

    // region 56种指令模拟
    // http://obelisk.me.uk/6502/reference.html

    /**
     * Add with Carry
     * 带进位加法
     */
    public void ADC() {
        fetch();

        int tmp = A + fetched + flagC;
        flagC = (byte)(tmp > 255 ? 1 : 0);
        flagZ = (byte)((tmp & 0x00FF) == 0 ? 1 : 0);
        flagN = (byte)((tmp & 0x80) == 0 ? 0 : 1);
        // A + M + C = R
        // FLAG_V = (A ^ M) & (~(A ^ R)) & 0x80 只看符号位
        flagV = (byte)(((A ^ fetched) & (~(A ^ tmp)) & 0x80) == 0 ? 0: 1);

        A = (byte)(tmp & 0x00FF);
    }

    /**
     * Logical AND
     */
    public void AND(int cycles, byte address) {
        fetch();
        A = (byte)(A & read(address));

        flagZ = (byte) ((A & 0x00FF) == 0 ? 1 : 0);
        flagN = (byte) ((A & 0x80) == 0 ? 0 : 1);
    }

    /**
     * Arithmetic Shift Left，算数左移
     * 根据寻址模式的不同，从累加器A或者内存中取值，并相应地将结果写回
     */
    public void ASL() {
        fetch();
        int tmp = fetched << 1;

        flagZ = (byte) ((A & 0x00FF) == 0 ? 1 : 0);
        flagC = (byte) ((fetched & 0x80) == 0 ? 0 : 1);
        flagN = (byte) ((tmp & 0x80) == 0 ? 0 : 1);

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
        if (flagC == 0) {
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
        if (flagC == 1) {
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
        if (flagZ == 1) {
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
     * Bit Test
     * 测试目标内存位置中是否设置了一个或多个位，mask存储于A中，设置标志位，不保留结果
     */
    public void BIT() {
        fetch();
        int tmp = fetched & A;

        flagZ = (byte)((tmp & 0x00FF) == 0 ? 1 : 0);
        flagV = (byte) ((fetched & 0x80) == 0 ? 0 : 1);
        flagV = (byte) ((fetched & 0x40) == 0 ? 0 : 1);
    }

    /**
     * Branch if Minus
     * 如果设置了 标志FLAG_N，改变当前的PC
     */
    public void BMI() {
        if (flagN == 1) {
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
        if (flagZ == 0) {
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
        if (flagN == 0) {
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
        flagI = 1;

        // 保存现场
        // 将PC（2字节）写入栈（先写入高8位）
        write(STACK_BASE_ADDRESS + S--, (byte)((PC >> 8) & 0x00FF));
        write(STACK_BASE_ADDRESS + S--, (byte)(PC & 0x00FF));

        // 设置状态寄存器（发生中断），将状态寄存器写入栈
        flagB = 1;
        byte status = (byte)(flagC | (flagZ << 1) | (flagI << 2) | (flagD << 3) |
                (flagB << 4) | (flagU << 5) | (flagV << 6) | (flagN << 7));
        write(STACK_BASE_ADDRESS + S--, status);
        flagB = 0;

        PC = read16(0xFFFE);
    }

    /**
     * Branch if Overflow Clear
     * 如果未设置 溢出标志FLAG_V，改变当前的PC
     */
    public void BVC() {
        if (flagV == 0) {
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
        if (flagV == 1) {
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
        flagC = 0;
    }

    /**
     * Clear Decimal Mode
     * 清除十进制标志位 FLAG_D
     */
    public void CLD() {
        flagD = 0;
    }

    /**
     * Clear Interrupt Disable
     * 清除中断禁用标志，允许正常中断
     */
    public void CLI() {
        flagI = 0;
    }

    /**
     * Clear Overflow Flag
     * 清除溢出标志位
     */
    public void CLV() {
        flagV = 0;
    }

    /**
     * Compare，Z,C,N = A-M
     * 比较A和内存中的值，并设置标志位
     */
    public void CMP() {
        fetch();
        int result = A - fetched;

        flagC = (byte)(result >= 0 ? 1 : 0);
        flagZ = (byte)(result == 0 ? 1 : 0);

        flagN = (byte)((result & 0x80) == 0 ? 0 : 1);

        // TODO 处理return 1
        cycles++;
    }

    /**
     * Compare X Register，Z,C,N = X-M
     * 比较X和内存中的值，并设置标志位
     */
    public void CPX() {
        fetch();
        int result = X - fetched;

        flagC = (byte)(result >= 0 ? 1 : 0);
        flagZ = (byte)(result == 0 ? 1 : 0);

        flagN = (byte)((result & 0x80) == 0 ? 0 : 1);
    }

    /**
     * Compare Y Register，Z,C,N = Y-M
     * 比较Y和内存中的值，并设置标志位
     */
    public void CPY() {
        fetch();
        int result = Y - fetched;

        flagC = (byte)(result >= 0 ? 1 : 0);
        flagZ = (byte)(result == 0 ? 1 : 0);

        flagN = (byte)((result & 0x80) == 0 ? 0 : 1);
    }

    /**
     * Decrement Memory, M,Z,N = M-1
     * 将内存中的值 -1，并设置标志位
     */
    public void DEC() {
        fetch();

        int result = fetched - 1;
        write(absoluteAddress, (byte)(result & 0x00FF));
        flagZ = (byte) ((result & 0x00FF) == 0 ? 1 : 0);
        flagN = (byte) ((result & 0x80) == 0 ? 0 : 1);
    }

    /**
     * Decrement X Register
     * X,Z,N = X-1
     * 将X的值 -1，并设置标志位
     */
    public void DEX() {
        X--;

        flagZ = (byte) (X == 0 ? 1 : 0);
        flagN = (byte) ((X & 0x80) == 0 ? 0 : 1);
    }

    /**
     * Decrement Y Register
     * Y,Z,N = Y-1
     * 将Y的值 -1，并设置标志位
     */
    public void DEY() {
        Y--;

        flagZ = (byte) (Y == 0 ? 1 : 0);
        flagN = (byte) ((Y & 0x80) == 0 ? 0 : 1);
    }

    /**
     * Exclusive OR
     * A,Z,N = A^M
     * 使用一字节内存的内容对A执行异或
     */
    public void EOR() {
        fetch();

        A ^= fetched;
        flagZ = (byte) (A == 0 ? 1 : 0);
        flagN = (byte) ((A & 0x80) == 0 ? 0 : 1);

        cycles++;
    }

    /**
     * Increment Memory
     * M,Z,N = M+1
     * 将内存中的值 +1，并设置标志位
     */
    public void INC() {
        fetch();

        int result = fetched + 1;
        write(absoluteAddress, (byte)(result & 0x00FF));
        flagZ = (byte) ((result & 0x00FF) == 0 ? 1 : 0);
        flagN = (byte) ((result & 0x80) == 0 ? 0 : 1);
    }

    /**
     * Increment X Register
     * X,Z,N = X+1
     * 将X的值 +1，并设置标志位
     */
    public void INX() {
        X++;

        flagZ = (byte) (X == 0 ? 1 : 0);
        flagN = (byte) ((X & 0x80) == 0 ? 0 : 1);
    }

    /**
     * Increment Y Register
     * Y,Z,N = Y+1
     * 将Y的值 +1，并设置标志位
     */
    public void INY() {
        Y++;

        flagZ = (byte) (Y == 0 ? 1 : 0);
        flagN = (byte) ((Y & 0x80) == 0 ? 0 : 1);
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

        write(STACK_BASE_ADDRESS + S--, (byte)((PC >> 8) & 0x00FF));
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

        A = (byte)fetched;

        flagZ = (byte) (A == 0 ? 1 : 0);
        flagN = (byte) ((A & 0x80) == 0 ? 0 : 1);
    }

    /**
     * Load X Register
     * X,Z,N = M
     * 将1字节的内存数据 加载到 X 中
     */
    public void LDX() {
        fetch();

        X = (byte)fetched;

        flagZ = (byte) (X == 0 ? 1 : 0);
        flagN = (byte) ((X & 0x80) == 0 ? 0 : 1);
    }

    /**
     * Load Y Register
     * Y,Z,N = M
     * 将1字节的内存数据 加载到 Y 中
     */
    public void LDY() {
        fetch();

        Y = (byte)fetched;

        flagZ = (byte) (Y == 0 ? 1 : 0);
        flagN = (byte) ((Y & 0x80) == 0 ? 0 : 1);
    }

    /**
     * Logical Shift Right
     * A,C,Z,N = A/2 or M,C,Z,N = M/2
     * A或M 逻辑右移，左边补0
     */
    public void LSR() {
        fetch();
        int tmp = fetched >> 1;

        flagZ = (byte) ((tmp & 0x00FF) == 0 ? 1 : 0);
        flagC = (byte) ((fetched & 0x01) == 0 ? 0 : 1);
        flagN = (byte) ((tmp & 0x80) == 0 ? 0 : 1);

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

        A |= fetched;
        flagZ = (byte) (A == 0 ? 1 : 0);
        flagN = (byte) ((A & 0x80) == 0 ? 0 : 1);

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
        flagB = flagU = 1;
        byte status = (byte)(flagC | (flagZ << 1) | (flagI << 2) | (flagD << 3) |
                (flagB << 4) | (flagU << 5) | (flagV << 6) | (flagN << 7));
        write(STACK_BASE_ADDRESS + S--, status);
        flagB = flagU = 0;
    }

    /**
     * Pull Accumulator
     * 从栈中pull 8bit的值 写入累加器A中
     */
    public void PLA() {
        S++;
        A = read(STACK_BASE_ADDRESS + S);

        flagZ = (byte)(A == 0x00 ? 1 : 0);
        flagN = (byte)((A & 0x80) == 0 ? 0 : 1);
    }

    /**
     * Pull Processor Status
     * 从栈取出 1字节 设置为标志位
     */
    public void PLP() {
        byte status = read(STACK_BASE_ADDRESS + S++);

        flagC = (byte)((status & 0x01) == 0 ? 0 : 1);
        flagZ = (byte)((status & 0x02) == 0 ? 0 : 1);
        flagI = (byte)((status & 0x04) == 0 ? 0 : 1);
        flagD = (byte)((status & 0x08) == 0 ? 0 : 1);
        flagB = (byte)((status & 0x10 & ~flagB) == 0 ? 0 : 1);
        flagU = (byte)((status & 0x20 & ~flagU) == 0 ? 0 : 1);
        flagV = (byte)((status & 0x40) == 0 ? 0 : 1);
        flagN = (byte)((status & 0x80) == 0 ? 0 : 1);

        flagU = 1;
    }

    /**
     * Rotate Left
     * 将 A 或 M 中的每个位向左移动一处
     * 右边补0，符号位作为 flagC
     */
    public void ROL() {
        fetch();

        int result = (fetched << 1) | flagC;
        flagC = (byte)((result & 0xFF00) > 0 ? 1 : 0);

        // TODO A or result ?
        flagZ = (byte) ((result & 0x00FF) == 0 ? 1 : 0);
        flagN = (byte) ((result & 0x80) == 0 ? 0 : 1);

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

        int result = (fetched >> 1) | (flagC << 7);
        flagC = (byte)((fetched & 0x0001) == 0 ? 0 : 1);

        // TODO A or result ?
        flagZ = (byte) ((result & 0x00FF) == 0 ? 1 : 0);
        flagN = (byte) ((result & 0x80) == 0 ? 0 : 1);

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
        byte status = read(STACK_BASE_ADDRESS + S++);

        flagC = (byte)((status & 0x01) == 0 ? 0 : 1);
        flagZ = (byte)((status & 0x02) == 0 ? 0 : 1);
        flagI = (byte)((status & 0x04) == 0 ? 0 : 1);
        flagD = (byte)((status & 0x08) == 0 ? 0 : 1);
        flagB = (byte)((status & 0x10 & ~flagB) == 0 ? 0 : 1);
        flagU = (byte)((status & 0x20 & ~flagU) == 0 ? 0 : 1);
        flagV = (byte)((status & 0x40) == 0 ? 0 : 1);
        flagN = (byte)((status & 0x80) == 0 ? 0 : 1);

        PC = read16(STACK_BASE_ADDRESS + S++);
        S++;
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
        int value = fetched ^ 0x00FF;

        int tmp = A + value + flagC;

        flagC = (byte)((tmp & 0xFF00) == 0 ? 0 : 1);
        flagZ = (byte)((tmp & 0x00FF) == 0 ? 0 : 1);
        flagV = (byte)(((tmp ^ A) & (tmp ^ value) & 0x0080) == 0 ? 0 : 1);
        flagN = (byte)((tmp & 0x80) == 0 ? 0 : 1);

        A = (byte)(tmp & 0x00FF);
    }

    /**
     * Set Carry Flag
     * C = 1
     */
    public void SEC() {
        flagC = 1;
    }

    /**
     * Set Decimal Flag
     * D = 1
     */
    public void SED() {
        flagD = 1;
    }

    /**
     * Set Interrupt Disable
     * I = 1
     */
    public void SEI() {
        flagI = 1;
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
        flagZ = (byte)(X == 0 ? 1 : 0);
        flagN = (byte)((X & 0x80) == 0 ? 0 : 1);
    }

    /**
     * Transfer Accumulator to Y
     * Y = A
     */
    public void TAY() {
        Y = A;
        flagZ = (byte)(Y == 0 ? 1 : 0);
        flagN = (byte)((Y & 0x80) == 0 ? 0 : 1);
    }

    /**
     * Transfer Stack Pointer to X
     * X = S
     */
    public void TSX() {
        X = S;
        flagZ = (byte)(X == 0 ? 1 : 0);
        flagN = (byte)((X & 0x80) == 0 ? 0 : 1);
    }

    /**
     *  Transfer X to Accumulator
     * A = X
     */
    public void TXA() {
        A = X;
        flagZ = (byte)(A == 0 ? 1 : 0);
        flagN = (byte)((A & 0x80) == 0 ? 0 : 1);
    }

    /**
     * Transfer X to Stack Pointer
     * S = X
     */
    public void TXS() {
        S = X;
    }

    /**
     * Transfer Y to Accumulator
     * A = Y
     */
    public void TYA() {
        A = Y;
        flagZ = (byte)(A == 0 ? 1 : 0);
        flagN = (byte)((A & 0x80) == 0 ? 0 : 1);
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

    void clock() {
        if (cycles == 0) {
            operationCode = read(PC++);
            flagU = 1;

            // 获取指令的周期数
            cycles = INSTRUCTION_CYCLE[operationCode];

            // 获取指令的寻址模式
            int addressingModeCode = INSTRUCTION_ADDRESSING_MODE[operationCode];
            AddressingMode addressingMode = AddressingMode.fromIndex(addressingModeCode);


            // 执行指令


            flagU = 1;
        }

        clockCount++;
        cycles--;
    }
}
