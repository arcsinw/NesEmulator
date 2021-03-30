import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class Main {
    public static void main(String[] args) throws IOException {

        MainWindow mainWindow = new MainWindow();


        InputStream inputStream = Main.class.getResourceAsStream("896.nes");
        URL url = Main.class.getClassLoader().getResource("896.nes");
        System.out.println(url.getPath());


        NesRom nesRom = new NesRom(inputStream);
        System.out.println(nesRom.header.toString());
    }
}
