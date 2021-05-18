package com.arcsinw.nesemulator;

public class NoiseChannel implements Channel {

    private int lengthCounter = 0;

    @Override
    public double getOutput() {
        return 0;
    }

    @Override
    public void write(int address, int data) {

    }
}
