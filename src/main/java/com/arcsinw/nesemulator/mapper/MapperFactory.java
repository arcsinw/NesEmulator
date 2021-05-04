package com.arcsinw.nesemulator.mapper;

public class MapperFactory {
    public static AbstractMapper getMapper(int id) {
        switch (id) {
            case 0:
                return new NROM();
            case 2:
                return new UxROM();
            default:
                return null;
        }
    }
}
