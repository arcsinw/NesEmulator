package com.arcsinw.nesemulator.mapper;

public class MapperFactory {
    public static AbstractMapper getMapper(int id) {
        switch (id) {
            case 0:
                return new NROM();
            case 1:
                return new MMC1();
            case 2:
                return new UxROM();
            case 4:
                return new MMC3();
            case 7:
                return new AxROM();
            default:
                return null;
        }
    }
}
