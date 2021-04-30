package com.arcsinw.nesemulator;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * 脉冲波
 */
public class PulseChannel {
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

    /**
     * write $4000 or $4004
     * Control Register (write only)
     * @param data
     */
    public void writeRegister0(int data) {
        byte byteData = (byte) (data & 0xFFFF);
        duty = DUTY_CYCLES_SEQUENCES[(byteData & 0xC0) >> 6];
    }

    /**
     * write $4001 or $4005
     * Ramp Control Register (write only)
     * @param data
     */
    public void writeRegister1(int data) {
    }

    /**
     * write $4002 or $4006
     * Fine Tune Register (write only)
     * PPPP PPPP
     * ++++ ++++-------------- period low
     * @param data period low, 8 bit
     */
    public void writeRegister2(int data) {
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
    public void writeRegister3(int data) {
        byte byteData = getUnsignedByte(data);
        reload = ((byteData & 0x07) << 8) | (reload & 0x00FF);
        timer = reload;
    }

    public void getOutput() {

    }

    private byte getUnsignedByte(int data) {
        return (byte) (data & 0x00FF);
    }

    public static void main(String[] args) {
//        byte[] buf = new byte[1];
//        AudioFormat af = new AudioFormat((float) 44100, 8, 1, true, false);
//        SourceDataLine sdl = null;
//        try {
//            sdl = AudioSystem.getSourceDataLine(af);
//            sdl.open();
//            sdl.start();
//            for (int i = 0; i < 1000 * (float) 44100 / 1000; i++) {
//                double angle = i / ((float) 44100 / 440) * 2.0 * Math.PI;
//                buf[0] = (byte) (Math.sin(angle) * 100);
//                sdl.write(buf, 0, 1);
//            }
//            sdl.drain();
//            sdl.stop();
//        } catch (LineUnavailableException e) {
//            e.printStackTrace();
//        }

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
