package racing.client.ui;

import javax.swing.JPanel;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

/** Resolution-independent garage artwork, cached until resized; no animation timer. */
public final class GaragePanel extends JPanel {
    private static final int ARTWORK_WIDTH = 540;
    private static final int ARTWORK_HEIGHT = 640;

    private BufferedImage artwork;

    public GaragePanel() {
        setBackground(GameTheme.BG);
        setPreferredSize(new Dimension(ARTWORK_WIDTH, ARTWORK_HEIGHT));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        if (artwork == null || artwork.getWidth() != getWidth() || artwork.getHeight() != getHeight()) {
            artwork = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D artworkGraphics = artwork.createGraphics();
            artworkGraphics.scale(
                    getWidth() / (double) ARTWORK_WIDTH,
                    getHeight() / (double) ARTWORK_HEIGHT);
            draw(artworkGraphics);
            artworkGraphics.dispose();
        }
        graphics.drawImage(artwork, 0, 0, null);
    }

    private void draw(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(
                0, 0, new Color(0x232B36),
                ARTWORK_WIDTH, ARTWORK_HEIGHT, new Color(0x0C1016)));
        g.fillRect(0, 0, ARTWORK_WIDTH, ARTWORK_HEIGHT);
        g.setColor(new Color(0x303945));
        for (int x = -280; x < 800; x += 80) {
            g.drawLine(x, ARTWORK_HEIGHT, x + 420, 260);
        }
        for (int y = 320; y < ARTWORK_HEIGHT; y += 48) {
            g.drawLine(0, y, ARTWORK_WIDTH, y);
        }
        g.setColor(GameTheme.ACCENT);
        g.fillPolygon(new int[]{394, 446, 154, 102}, new int[]{0, 0, 640, 640}, 4);
        g.setColor(new Color(0x17202A));
        g.fillPolygon(new int[]{446, 488, 196, 154}, new int[]{0, 0, 640, 640}, 4);
        g.setColor(GameTheme.ACCENT); g.fillRect(40,40,8,18); g.fillRect(52,40,4,18);
        g.setFont(GameTheme.STRONG); g.setColor(GameTheme.TEXT); g.drawString("RACING / ONLINE",68,55);
        g.setFont(GameTheme.SMALL); g.setColor(GameTheme.MUTED); g.drawString("ĐUA XE ĐỐI KHÁNG",40,108);
        g.setFont(GameTheme.TITLE.deriveFont(54f)); g.setColor(GameTheme.TEXT);
        g.drawString("ĐƯỜNG ĐUA.",38,173); g.drawString("CỦA BẠN.",38,234);
        g.setFont(GameTheme.BODY); g.setColor(GameTheme.MUTED);
        g.drawString("Chọn đối thủ. Giữ tay lái. Vượt lên phía trước.",40,272);
        drawCar(g);
        g.setColor(new Color(0x19212C)); g.fillRect(0,535,540,105);
        g.setColor(GameTheme.LINE); g.drawLine(40,535,500,535);
        g.setFont(GameTheme.STRONG.deriveFont(18f)); g.setColor(GameTheme.TEXT);
        g.drawString("1 VS 1",40,578); g.drawString("2.500 M",213,578); g.drawString("3 LÀN",401,578);
        g.setFont(GameTheme.SMALL); g.setColor(GameTheme.MUTED);
        g.drawString("ĐẤU TRỰC TIẾP",40,602); g.drawString("ĐƯỜNG ĐUA",213,602); g.drawString("NÉ XE CỘ",401,602);
    }
    private static void drawCar(Graphics2D g) {
        g.setColor(new Color(0,0,0,100)); g.fillOval(41,452,463,38);
        // Lower racing body and roof, deliberately rendered as a single silhouette.
        Path2D body=new Path2D.Double(); body.moveTo(45,411); body.lineTo(75,392); body.lineTo(160,376);
        body.lineTo(221,331); body.lineTo(326,334); body.lineTo(389,375); body.lineTo(467,390);
        body.lineTo(496,421); body.lineTo(492,450); body.lineTo(45,450); body.closePath();
        g.setPaint(new GradientPaint(0,335,new Color(0xFFAA77),0,454,new Color(0xD95326))); g.fill(body);
        Path2D window=new Path2D.Double(); window.moveTo(181,375); window.lineTo(230,343); window.lineTo(320,345);
        window.lineTo(365,377); window.closePath(); g.setColor(new Color(0x1B2938));g.fill(window);
        g.setColor(new Color(0x5A7187)); g.drawLine(232,347,201,369); g.drawLine(295,346,295,376);
        g.setColor(new Color(0x12202D));g.fillRect(64,429,407,16);
        g.setColor(new Color(0xFFE6D1));g.fillPolygon(new int[]{458,481,488,458},new int[]{395,405,413,410},4);
        g.setColor(new Color(0xB22724));g.fillRect(49,410,21,10);
        g.setColor(GameTheme.TEXT); g.fillPolygon(new int[]{248,286,315,277},new int[]{382,382,425,425},4);
        g.setFont(GameTheme.TITLE.deriveFont(23f));g.setColor(GameTheme.BG);g.drawString("01",258,412);
        g.setColor(new Color(0x0B1016));g.fillRect(55,378,43,8);g.fillRect(86,384,6,17);
        for (int centerX : new int[]{137, 410}) {
            g.setColor(new Color(0x0B0D12));
            g.fillOval(centerX - 39, 413, 78, 78);
            g.setColor(new Color(0x465364));
            g.fillOval(centerX - 26, 426, 52, 52);
            g.setColor(new Color(0xBAC7D5));
            g.setStroke(new BasicStroke(3));
            for (int spoke = 0; spoke < 5; spoke++) {
                double angle = spoke * Math.PI * 2 / 5;
                g.drawLine(
                        centerX,
                        452,
                        centerX + (int) (20 * Math.cos(angle)),
                        452 + (int) (20 * Math.sin(angle)));
            }
            g.setColor(GameTheme.ACCENT);
            g.fillOval(centerX - 5, 447, 10, 10);
        }
    }
}
