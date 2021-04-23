package com.arcsinw.nesemulator;

import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * 手柄控制器
 */
public class Joypad {

    /**
     * 8位 每位代表一个按键的状态（1 按下）
     */
    private byte controller = 0;

    private int index = 0;

    private boolean logging = false;

    enum ButtonFlag {
        Right(1 << 7),
        Left(1 << 6),
        Down(1 << 5),
        Up(1 << 4),
        Start(1 << 3),
        Select(1 << 2),
        B(1 << 1),
        A(1 << 0),
        ;

        private int mask;
        ButtonFlag(int mask) {
            this.mask = mask;
        }
    }

    public void setButton(ButtonFlag flag, int value) {
        if (logging) {
            System.out.println(String.format("Set button %s to %d", flag.toString(), value));
        }

        if (value == 0) {
            controller &= (~flag.mask);
        } else {
            controller |= flag.mask;
        }
    }

    public void write(int address, int data) {
        if (address == 0x4016) {
            if ((data & 0x01) != 0) {
                // 向0x4016写入0x01，将手柄设置为 选通
                index = 0;
            } else {
                // 写入0x00

            }
        }

        if (logging) {
            System.out.println(String.format("Write 0x4016 to %02X", data));
        }
    }

    /**
     * 按键读出的顺序 A B Select Start Up Down Left Right
     * @return
     */
    public byte read() {
        byte data =  (byte) (0x40 | ((controller & (1 << index)) != 0 ? 1 : 0));
        index = (index + 1) % 8;

        if (logging) {
            System.out.println(String.format("Read 0x4016  data: %02X index: %d", data, index));
        }

        return data;
    }
}
