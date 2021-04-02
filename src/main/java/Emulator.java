import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class Emulator {
    private static CPU cpu = new CPU();
    private static PPU ppu = new PPU();

    public static void main(String[] args) throws IOException {
        MainWindow mainWindow = new MainWindow();

        InputStream inputStream = Emulator.class.getResourceAsStream("896.nes");
        URL url = Emulator.class.getClassLoader().getResource("896.nes");
        System.out.println(url.getPath());


        NesRom nesRom = new NesRom(inputStream);
        System.out.println(nesRom.header.toString());

        cpu.diasm(nesRom.prg);

        byte[][][] patternTable = ppu.getPatternTable(nesRom.chr);

        mainWindow.displayPatternTable(patternTable);
    }
}
