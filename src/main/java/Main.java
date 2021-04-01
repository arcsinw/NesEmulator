import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class Main {
    public static void main(String[] args) throws IOException {

        MainWindow mainWindow = new MainWindow();


        InputStream inputStream = Main.class.getResourceAsStream("nestest.nes");
        URL url = Main.class.getClassLoader().getResource("nestest.nes");
        System.out.println(url.getPath());


        NesRom nesRom = new NesRom(inputStream);
        System.out.println(nesRom.header.toString());

        CPU cpu = new CPU();
        cpu.diasm(nesRom.prg);
    }
}
