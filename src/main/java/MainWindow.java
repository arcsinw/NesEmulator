import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

public class MainWindow  extends Frame {
    private static final int SCREEN_WIDTH = 256;
    private static final int SCREEN_HEIGHT = 240;
    private static final int SCREEN_RATIO = 3;

    private BufferedImage image = new BufferedImage(SCREEN_WIDTH,
            SCREEN_HEIGHT, BufferedImage.TYPE_3BYTE_BGR);

    public MainWindow() {
        setTitle("NesEmulator");
        setSize(SCREEN_WIDTH * SCREEN_RATIO, SCREEN_HEIGHT * SCREEN_RATIO);
        setBackground(Color.black);
        setLayout(new FlowLayout());
        addMenuBar();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                dispose();
            }
        });

        Button b = new Button("paint");
        b.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                paint();
            }
        });
        add(b);
        setVisible(true);
    }

    int k = 1;
    public void paint() {
        Graphics graphics = this.getGraphics();

        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 100; j++) {
                image.setRGB(i*k, j*k, Color.white.getRGB());
            }
        }

        k++;
        graphics.drawImage(image, 0, 0, 100, 200, this);
    }

    private void addMenuBar() {
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

    public void displayPatternTable(byte[][][] table) {
        BufferedImage bgImage = new BufferedImage(256*8,
                256*8, BufferedImage.TYPE_3BYTE_BGR);

        byte[][] background = table[1];
        byte[][] sprite = table[1];

        int startRow = 0, startCol = 0;

        for (int k = 0; k < 256; k++) {
            for (int i = 0; i < 64; i++) {
                switch (background[k][i]) {
                    case 1:
                        bgImage.setRGB(startCol + i % 8, startRow + i / 8, Color.cyan.getRGB());
                        break;
                    case 2:
                        bgImage.setRGB(startCol + i % 8, startRow + i / 8, Color.orange.getRGB());
                        break;
                    case 3:
                        bgImage.setRGB(startCol + i % 8, startRow + i / 8, Color.green.getRGB());
                        break;
                }
            }

            if (k != 0 && ((k & 0x0F) == 0x0F)) {
                startCol = 0;
                startRow += 8;
            } else {
                startCol += 8;
            }
        }

        Graphics graphics = this.getGraphics();
        int left = this.getInsets().left;
        int right = this.getInsets().right;

        graphics.drawImage(bgImage, 150, 150, 256*16*2, 256*16*2,this);
    }
}
