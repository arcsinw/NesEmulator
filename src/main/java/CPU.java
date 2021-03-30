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
    void reset() {}

    /**
     * 不可屏蔽中断 Non-Maskable Interrupt
     */
    void nmi() {}

    /**
     * 可屏蔽中断 Interrupt Request
     */
    void irq() {}

    // endregion

    // region 7种CPU Flags

    /**
     * Carry，进位标志
     */
    public static final int FLAG_C = (1 << 0);

    /**
     * Zero
     */
    public static final int FLAG_Z = (1 << 1);

    /**
     * Interrupt Disable
     */
    public static final int FLAG_I = (1 << 2);

    /**
     * Decimal Mode
     */
    public static final int FLAG_D = (1 << 3);

    /**
     * B flag, 2 bit
     */
    public static final int FLAG_B = (1 << 4);

    /**
     * Overflow
     */
    public static final int FLAG_V = (1 << 6);

    /**
     * Negative
     */
    public static final int FLAG_N = (1 << 7);

    // endregion

    // region 12种取地址方式 Addressing Modes https://zhuanlan.zhihu.com/p/44051504

    /**
     * 隐含寻址 Implied Addressing 单字节指令
     */
    public byte IMP() {
        fetched = A;
        return 0;
    }

    /**
     * 立即寻址 Immediate Addressing 双字节
     *
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
    public void ADC() {}
    public void AND() {}
    public void ASL() {}
    public void BCC() {}
    public void BCS() {}
    public void BEQ() {}
    public void BIT() {}
    public void BMI() {}
    public void BNE() {}
    public void BPL() {}
    public void BRK() {}
    public void BVC() {}
    public void BVS() {}
    public void CLC() {}
    public void CLD() {}
    public void CLI() {}
    public void CLV() {}
    public void CMP() {}
    public void CPX() {}
    public void CPY() {}
    public void DEC() {}
    public void DEX() {}
    public void DEY() {}
    public void EOR() {}
    public void INC() {}
    public void INX() {}
    public void INY() {}
    public void JMP() {}
    public void JSR() {}
    public void LDA() {}
    public void LDX() {}
    public void LDY() {}
    public void LSR() {}
    public void NOP() {}
    public void ORA() {}
    public void PHA() {}
    public void PHP() {}
    public void PLA() {}
    public void PLP() {}
    public void ROL() {}
    public void ROR() {}
    public void RTI() {}
    public void RTS() {}
    public void SBC() {}
    public void SEC() {}
    public void SED() {}
    public void SEI() {}
    public void STA() {}
    public void STX() {}
    public void STY() {}
    public void TAX() {}
    public void TAY() {}
    public void TSX() {}
    public void TXA() {}
    public void TXS() {}
    public void TYA() {}
    // endregion

    void fetch() {}
    int fetched = 0x00;

    int absoluteAddress = 0x0000;
    int relativeAddress = 0x00;
    int cycles = 0;
    int operationCode = 0x00;
}
