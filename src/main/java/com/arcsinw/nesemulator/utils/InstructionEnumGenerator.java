package com.arcsinw.nesemulator.utils;

import com.arcsinw.nesemulator.CPU;
import com.sun.org.apache.bcel.internal.generic.Instruction;

import java.util.HashMap;

public class InstructionEnumGenerator {
    /**
     * BRK_Implied("BRK", 1, 7, 0x00, AddressingMode.Implied) {
     *             @Override
     *             public void operation(CPU cpu) {
     *                 cpu.IMP();
     *                 cpu.BRK();
     *             }
     *         },
     */
    private static final String template = "%s_%s(\"%s\", %d, %d, 0x%02X, %s) {\n" +
            "    @Override\n" +
            "    public void operation(CPU cpu) {\n" +
            "        cpu.%s();\n" +
            "        cpu.%s();\n" +
            "    }\n" +
            "},\n";

    private static HashMap<String, String> addressingMode2FunctionMap = new HashMap() {
        {
            put("Implied", "IMP");
            put("Accumulator", "IMP");
            put("Immediate", "IMM");
            put("ZeroPage", "ZP0");
            put("ZeroPageX", "ZPX");
            put("ZeroPageY", "ZPY");
            put("Relative", "REL");
            put("Absolute", "ABS");
            put("AbsoluteX", "ABX");
            put("AbsoluteY", "ABY");
            put("Indirect", "IND");
            put("IndexedIndirectX", "IZX");
            put("IndirectIndexedY", "IZY");
        }
    };


    private static HashMap<Integer, Instruction> code2InstructionMap = new HashMap<>();

    public static void main(String[] args) {
        String[] instructionSet = CPU.INSTRUCTION_SET;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < instructionSet.length; i++) {
            String instructionName = instructionSet[i];

            if (instructionName.equals("UNK")) continue;

            CPU.AddressingMode addressingMode = CPU.ADDRESSING_MODE_TABLE[CPU.INSTRUCTION_ADDRESSING_MODE[i]];

            String tmp = String.format(template, instructionName, addressingMode.toString(),
                    instructionName, CPU.INSTRUCTION_LENGTH[i], CPU.INSTRUCTION_CYCLE[i],
                    i, addressingMode.getFullName(), addressingMode2FunctionMap.get(addressingMode.toString()), instructionName);

            sb.append(tmp);
        }

        System.out.println("OK");
    }
}
