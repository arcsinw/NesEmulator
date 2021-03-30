import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MainWindow  extends Frame {
    private static final int SCREEN_WIDTH = 256;
    private static final int SCREEN_HEIGHT = 240;
    private static final int SCREEN_RATIO = 3;

    public MainWindow() {
        setTitle("NesEmulator");
        setSize(SCREEN_WIDTH * SCREEN_RATIO, SCREEN_HEIGHT * SCREEN_RATIO);

        addMenuBar();

        setVisible(true);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                dispose();
            }
        });

    }

    public void addMenuBar() {
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("文件");
        fileMenu.add(new MenuItem("打开") {

        });

        Menu helpMenu = new Menu("帮助");
        helpMenu.add(new MenuItem("关于") {

        });

        menuBar.add(fileMenu);
        menuBar.add(helpMenu);

        setMenuBar(menuBar);
    }
}
