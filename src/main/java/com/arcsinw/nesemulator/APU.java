package com.arcsinw.nesemulator;


/**
 * Audio Processing Unit
 *
 */
public class APU {
    private PulseChannel pulseChannel1 = new PulseChannel();
    private PulseChannel pulseChannel2 = new PulseChannel();
    private TriangleChannel triangleChannel = new TriangleChannel();

    private boolean pulseChannel1Enabled = false;
    private boolean pulseChannel2Enabled = false;

    public int read(int address) {
        return 0;
    }

    public void write(int address, int data) {
        byte byteData = (byte) (data & 0x00FF);

        switch(address) {
            /**
             * 控制各个通道的开启状态 (0-关闭)
             * ---D NT21
             * |||| ||++-------------- Pulse channel 2, 1
             * |||| |+---------------- Triangle channel
             * |||| +----------------- Noise channel
             * |||+ ------------------ DMC channel
             */
            case 0x4015:
                pulseChannel1Enabled = (byteData & 0x01) != 0;
                break;
            default:
                break;
        }
    }

    /**
     * 获取混合后的音频输出
     */
    public void getMixedSampleOuput() {

    }

    public void clock() {

    }

    public void reset() {

    }
}
