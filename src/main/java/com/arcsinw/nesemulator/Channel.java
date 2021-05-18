package com.arcsinw.nesemulator;

public interface Channel {
    double getOutput();

    void write(int address, int data);
}
