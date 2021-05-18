package com.arcsinw.nesemulator;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * 脉冲波
 */
public class PulseChannel implements Channel {
    /**
     * 占空比序列
     */
    private final int[] DUTY_CYCLES_SEQUENCES = {
            0b01000000,     // 12.5%
            0b01100000,     // 25%
            0b01111000,     // 50%
            0b10011111      // 25% negated
    };

    /**
     * 占空比序号 0~3
     */
    private int duty = 0;

    private int reload = 0;

    private int timer = 0;

    private int lengthCounter = 0;

    private boolean enabled = false;

    /**
     * write $4000 or $4004
     * Control Register (write only)
     * DDLC VVVV
     * |||| ++++-------------- volume/envelope (V)
     * |||+ ------------------ constant volume，音量控制（固定音量 或 envelope（包络，描述声音振幅的变化，ADSR - Attack, Decay, Sustain, Release））
     * ||+- ------------------ envelope loop / length counter halt (L)
     * ++-- ------------------ duty 占空比
     * @param data
     */
    private void writeRegister0(int data) {
        byte byteData = (byte) (data & 0xFFFF);
        duty = DUTY_CYCLES_SEQUENCES[(byteData & 0xC0) >> 6];
    }

    /**
     * write $4001 or $4005
     * Ramp Control Register (write only)
     * EPPP NSSS
     * |||| |+++-------------- shift
     * |||| +----------------- negate
     * |+++ ------------------ period
     * +--- ------------------ enabled
     * @param data
     */
    private void writeRegister1(int data) {

    }

    /**
     * write $4002 or $4006
     * Fine Tune Register (write only)
     * PPPP PPPP
     * ++++ ++++-------------- period low
     * @param data period low, 8 bit
     */
    private void writeRegister2(int data) {
        reload = (reload & 0xFF00) | (data & 0x00FF);
    }

    /**
     * write $4003 or $4007
     * Coarse Tune / Length Register (write only)
     * LLLL LPPP
     * |||| |+++-------------- period high
     * ++++ +----------------- length index
     * @param data
     */
    private void writeRegister3(int data) {
        byte byteData = getUnsignedByte(data);
        reload = ((byteData & 0x07) << 8) | (reload & 0x00FF);
        timer = reload;
    }

    @Override
    public double getOutput() {
        return 0.0d;
    }

    @Override
    public void write(int address, int data) {
        switch (address) {
            case 0x4000:
            case 0x4004:
                writeRegister0(data);
                break;
            case 0x4001:
            case 0x4005:
                writeRegister1(data);
                break;
            case 0x4002:
            case 0x4006:
                writeRegister2(data);
                break;
            case 0x4003:
            case 0x4007:
                writeRegister3(data);
                break;
            default:
                break;
        }
    }

    private byte getUnsignedByte(int data) {
        return (byte) (data & 0x00FF);
    }

    public static void main(String[] args) {
        try {
            byte[] buffer = new byte[2];
            int frequency = 44100; //44100 sample points per 1 second
            AudioFormat audioFormat = new AudioFormat((float) frequency, 16, 1, true, false);
            SourceDataLine sourceDataLine = null;
            sourceDataLine = AudioSystem.getSourceDataLine(audioFormat);
            sourceDataLine.open();
            sourceDataLine.start();
            int durationMs = 5000;
            int numberOfTimesFullSinFuncPerSec = 441; //number of times in 1sec sin function repeats
            for (int i = 0; i < durationMs * (float) 44100 / 1000; i++) { //1000 ms in 1 second
                float numberOfSamplesToRepresentFullSin= (float) frequency / numberOfTimesFullSinFuncPerSec;
                double angle = i / (numberOfSamplesToRepresentFullSin/ 2.0) * Math.PI;  // /divide with 2 since sin goes 0PI to 2PI
                short a = (short) (Math.sin(angle) * 32767);  //32767 - max value for sample to take (-32767 to 32767)
                buffer[0] = (byte) (a & 0xFF); //write 8bits ________WWWWWWWW out of 16
                buffer[1] = (byte) (a >> 8); //write 8bits WWWWWWWW________ out of 16
                sourceDataLine.write(buffer, 0, 2);
            }
            sourceDataLine.drain();
            sourceDataLine.stop();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }

    }
}
