package com.arcsinw.nesemulator;


import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * Audio Processing Unit
 */
public class APU {
    private PulseChannel pulseChannel1 = new PulseChannel();
    private PulseChannel pulseChannel2 = new PulseChannel();
    private TriangleChannel triangleChannel = new TriangleChannel();
    private NoiseChannel noiseChannel = new NoiseChannel();
    private DmcChannel dmcChannel = new DmcChannel();

    private boolean pulseChannel1Enabled = false;
    private boolean pulseChannel2Enabled = false;
    private boolean triangleChannelEnabled = false;
    private boolean noiseChannelEnabled = false;
    private boolean dmcChannelEnabled = false;

    private int frameCounter = 0;

    public int read(int address) {
        return 0;
    }

    public void write(int address, int data) {
        byte byteData = (byte) (data & 0x00FF);

        switch (address) {
            case 0x4000:
            case 0x4001:
            case 0x4002:
            case 0x4003:
                pulseChannel1.write(address, data);
                break;
            case 0x4004:
            case 0x4005:
            case 0x4006:
            case 0x4007:
                pulseChannel2.write(address, data);
                break;
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
                pulseChannel2Enabled = (byteData & 0x02) != 0;
                triangleChannelEnabled = (byteData & 0x04) != 0;
                noiseChannelEnabled = (byteData & 0x08) != 0;
                dmcChannelEnabled = (byteData & 0x10) != 0;
                break;
            default:
                break;
        }
    }

    /**
     * 获取混合后的音频输出
     * APU音频输出信号来自两个单独的组件。脉冲通道在一个引脚上输出，三角形/噪声/ DMC在另一个引脚上输出
     * @return 混合后的音频 0.0 ~ 1.0
     */
    public double getMixedSampleOuput() {
        double pulse = 95.88 / (8128 / (pulseChannel1.getOutput() + pulseChannel2.getOutput()) + 100);
        double tnd = 159.79 / (1 / (triangleChannel.getOutput() / 8227 + noiseChannel.getOutput() / 12241 + dmcChannel.getOutput() / 22638) + 100);

        double mixed = pulse + tnd;
        return mixed;
    }

    public void clock() {
        double output = getMixedSampleOuput();

        AudioFormat audioFormat = new AudioFormat(44100, 8, 1, true, true);
        SourceDataLine sourceDataLine = null;

        try {
            sourceDataLine = AudioSystem.getSourceDataLine(audioFormat);
            sourceDataLine.open();
            sourceDataLine.start();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        } finally {
            if (sourceDataLine != null) {
                sourceDataLine.drain();
                sourceDataLine.close();
            }
        }
    }

    public void reset() {

    }
}
