package com.arcsinw.nesemulator.utils;

public class IdeaDebugUtils {
    public static String get2DArrayPrint(byte[] matrix) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < (matrix.length / 16); i++) {
            sb.append(String.format("%04X : ", 0x2000 + i * 16));
            for (int j = 0; j < 16; j++) {
                sb.append(String.format("%02X  ", matrix[i* 16 + j]));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public static String get2DArrayPrint(int[][] matrix) {
        StringBuilder sb = new StringBuilder();
        int count = 0;

        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                if (count % 16 == 0) {
                    sb.append(String.format("%04X : ", 0x2000 + count));
                }

                sb.append(String.format("%02X  ", matrix[i][j]));
                if (++count % 16 == 0) {
                    sb.append("\n");
                }
            }
        }

        return sb.toString();
    }
}
