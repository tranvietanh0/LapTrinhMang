package racing.client.ui.race;

import racing.client.model.CarModel;
import racing.common.GameConfig;
import racing.common.dto.Obstacle;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * Vẽ hai đường đua song song (trái: xe của mình, phải: xe đối thủ), chướng ngại vật giống nhau,
 * HUD tốc độ / quãng đường và overlay đếm ngược. Chỉ vẽ, không chứa logic mạng.
 * Toàn bộ 1000 m được vẽ vừa chiều cao đường đua; xuất phát ở dưới, đích ở trên.
 */
public final class RacePanel extends JPanel {

    private static final Color ROAD = new Color(0x4A4A4A);
    private static final Color ROAD_EDGE = new Color(0x222222);
    private static final Color LANE_LINE = Color.WHITE;
    private static final Color OBSTACLE = new Color(0xE67E22);
    private static final Color OBSTACLE_EDGE = new Color(0x7A3E00);
    private static final Color MY_CAR = new Color(0x2F6DB5);
    private static final Color OPP_CAR = new Color(0xC0392B);
    private static final Color HEADER_BG = new Color(0xE9EEF5);
    private static final Color HUD_BG = new Color(0xF7F7F7);
    private static final Color FLASH = new Color(255, 0, 0, 90);
    private static final Color OVERLAY = new Color(0, 0, 0, 120);

    private static final int HEADER_H = 56;
    private static final int HUD_H = 64;
    private static final int MARGIN = 16;
    private static final int GAP = 90;

    private final CarModel myCar;
    private final CarModel opponentCar;
    private List<Obstacle> obstacles = List.of();

    private String myTitle = "XE CỦA BẠN";
    private String opponentTitle = "XE ĐỐI THỦ";
    private String countdownText = null;   // "3", "2", "1", "GO!" hoặc null
    private String statusText = "";        // dòng trạng thái giữa hai đường đua
    private long myFlashUntil;
    private long oppFlashUntil;

    public RacePanel(CarModel myCar, CarModel opponentCar) {
        this.myCar = myCar;
        this.opponentCar = opponentCar;
        setPreferredSize(new Dimension(1200, 800));
        setBackground(Color.WHITE);
        setDoubleBuffered(true);
        setFocusable(true);
    }

    public void setObstacles(List<Obstacle> obstacles) {
        this.obstacles = obstacles == null ? List.of() : List.copyOf(obstacles);
        repaint();
    }

    public void setTitles(String mine, String opponent) {
        this.myTitle = mine;
        this.opponentTitle = opponent;
    }

    public void setCountdownText(String text) {
        this.countdownText = text;
        repaint();
    }

    public void setStatusText(String text) {
        this.statusText = text == null ? "" : text;
    }

    public void flashMine() {
        myFlashUntil = System.currentTimeMillis() + 500;
    }

    public void flashOpponent() {
        oppFlashUntil = System.currentTimeMillis() + 500;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int trackW = (w - 2 * MARGIN - GAP) / 2;
        int trackTop = MARGIN + HEADER_H;
        int trackH = h - trackTop - HUD_H - 2 * MARGIN;
        int leftX = MARGIN;
        int rightX = MARGIN + trackW + GAP;

        drawTrack(g, leftX, trackTop, trackW, trackH, myTitle,
                "W/↑ tăng tốc   S/↓ phanh   A/D hoặc ←/→ đổi làn", myCar, MY_CAR,
                System.currentTimeMillis() < myFlashUntil);
        drawTrack(g, rightX, trackTop, trackW, trackH, opponentTitle,
                "Vị trí, tốc độ, quãng đường do server đồng bộ", opponentCar, OPP_CAR,
                System.currentTimeMillis() < oppFlashUntil);

        drawHud(g, leftX, trackTop + trackH + MARGIN, trackW, myCar, MY_CAR);
        drawHud(g, rightX, trackTop + trackH + MARGIN, trackW, opponentCar, OPP_CAR);

        // dòng trạng thái giữa hai đường
        if (!statusText.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.setColor(new Color(0x555555));
            drawCentered(g, statusText, w / 2, trackTop + trackH + MARGIN + HUD_H / 2);
        }

        if (countdownText != null) {
            g.setColor(OVERLAY);
            g.fillRect(0, 0, w, h);
            g.setFont(new Font("SansSerif", Font.BOLD, 120));
            g.setColor(Color.WHITE);
            drawCentered(g, countdownText, w / 2, h / 2);
        }
        g.dispose();
    }

    private void drawTrack(Graphics2D g, int x, int y, int w, int h, String title, String subtitle,
                           CarModel car, Color carColor, boolean flash) {
        // header
        g.setColor(HEADER_BG);
        g.fillRect(x, y - HEADER_H, w, HEADER_H);
        g.setColor(ROAD_EDGE);
        g.drawRect(x, y - HEADER_H, w, HEADER_H);
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.setColor(Color.BLACK);
        drawCentered(g, title, x + w / 2, y - HEADER_H + 22);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(0x444444));
        drawCentered(g, subtitle, x + w / 2, y - HEADER_H + 42);

        // road
        g.setColor(ROAD);
        g.fillRect(x, y, w, h);
        g.setColor(ROAD_EDGE);
        g.setStroke(new BasicStroke(2f));
        g.drawRect(x, y, w, h);

        // lane lines
        int laneW = w / GameConfig.LANES;
        g.setColor(LANE_LINE);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f,
                new float[] {14f, 12f}, 0f));
        for (int i = 1; i < GameConfig.LANES; i++) {
            int lx = x + laneW * i;
            g.drawLine(lx, y + 4, lx, y + h - 4);
        }
        g.setStroke(new BasicStroke(1f));

        // start / finish
        int startY = yFor(0, y, h);
        int finishY = yFor(GameConfig.TRACK_LENGTH, y, h);
        drawCheckerLine(g, x, startY, w);
        drawCheckerLine(g, x, finishY, w);
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.setColor(Color.WHITE);
        g.drawString("XUẤT PHÁT", x + 6, startY - 6);
        g.drawString("ĐÍCH", x + 6, finishY + 16);

        // obstacles
        // Kích thước vẽ tối thiểu để xe và chướng ngại vật dễ nhìn dù cả 1000 m nằm gọn trong panel
        double metersPerPx = GameConfig.TRACK_LENGTH / h;
        int obsH = Math.max(22, (int) (GameConfig.OBSTACLE_LENGTH / metersPerPx));
        int obsW = laneW * 3 / 5;
        for (Obstacle o : obstacles) {
            int ox = x + laneW * o.lane() + (laneW - obsW) / 2;
            int oy = yFor(o.position() + GameConfig.OBSTACLE_LENGTH, y, h);
            g.setColor(OBSTACLE);
            g.fillRect(ox, oy, obsW, obsH);
            g.setColor(OBSTACLE_EDGE);
            g.drawRect(ox, oy, obsW, obsH);
            g.drawLine(ox, oy, ox + obsW, oy + obsH);
            g.drawLine(ox, oy + obsH, ox + obsW, oy);
        }

        // car
        int carH = Math.max(40, (int) (GameConfig.CAR_LENGTH / metersPerPx));
        int carW = laneW * 2 / 5;
        int cx = x + laneW * car.lane() + (laneW - carW) / 2;
        int cy = yFor(car.distance() + GameConfig.CAR_LENGTH, y, h);
        cy = Math.max(y, Math.min(y + h - carH, cy));
        g.setColor(car.stunned() ? carColor.darker() : carColor);
        g.fill(new RoundRectangle2D.Float(cx, cy, carW, carH, 8, 8));
        g.setColor(ROAD_EDGE);
        g.draw(new RoundRectangle2D.Float(cx, cy, carW, carH, 8, 8));
        g.setColor(new Color(0xBFE3FF));
        g.fillRect(cx + carW / 5, cy + carH / 6, carW * 3 / 5, Math.max(4, carH / 5));   // kính trước
        g.setColor(ROAD_EDGE);
        g.fillRect(cx - 3, cy + 4, 4, carH / 4);                                          // bánh xe
        g.fillRect(cx + carW - 1, cy + 4, 4, carH / 4);
        g.fillRect(cx - 3, cy + carH - carH / 4 - 4, 4, carH / 4);
        g.fillRect(cx + carW - 1, cy + carH - carH / 4 - 4, 4, carH / 4);

        if (flash) {
            g.setColor(FLASH);
            g.fillRect(x, y, w, h);
        }
    }

    private void drawHud(Graphics2D g, int x, int y, int w, CarModel car, Color color) {
        g.setColor(HUD_BG);
        g.fillRect(x, y, w, HUD_H);
        g.setColor(ROAD_EDGE);
        g.drawRect(x, y, w, HUD_H);
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(Color.BLACK);
        g.drawString(String.format("Tốc độ: %.0f km/h%s", car.speed(), car.stunned() ? "   (va chạm!)" : ""),
                x + 10, y + 24);
        g.drawString(String.format("Quãng đường: %.0f / %.0f m", car.distance(), GameConfig.TRACK_LENGTH),
                x + 10, y + 46);
        // progress bar
        int barX = x + w * 55 / 100;
        int barW = w * 40 / 100;
        int barY = y + 22;
        g.setColor(new Color(0xDDDDDD));
        g.fillRect(barX, barY, barW, 20);
        g.setColor(color);
        g.fillRect(barX, barY, barW * car.progressPercent() / 100, 20);
        g.setColor(new Color(0x999999));
        g.drawRect(barX, barY, barW, 20);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        drawCentered(g, car.progressPercent() + "%", barX + barW / 2, barY + 10);
        if (car.finished()) {
            g.setColor(color);
            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            g.drawString("ĐÃ VỀ ĐÍCH", barX, y + 58);
        }
    }

    private static void drawCheckerLine(Graphics2D g, int x, int y, int w) {
        int n = 12;
        int cell = w / n;
        for (int i = 0; i < n; i++) {
            g.setColor(i % 2 == 0 ? Color.WHITE : ROAD_EDGE);
            g.fillRect(x + i * cell, y - 4, cell, 8);
        }
    }

    /** Quy đổi quãng đường (m) sang toạ độ y: 0 m ở đáy, TRACK_LENGTH ở đỉnh đường đua. */
    private static int yFor(double meters, int trackY, int trackH) {
        double ratio = Math.max(0, Math.min(1, meters / GameConfig.TRACK_LENGTH));
        return trackY + trackH - (int) Math.round(ratio * trackH);
    }

    private static void drawCentered(Graphics2D g, String s, int cx, int cy) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, cx - fm.stringWidth(s) / 2, cy + fm.getAscent() / 2 - 2);
    }
}
