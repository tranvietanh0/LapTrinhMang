package racing.client.ui.race;

import racing.client.model.CarModel;
import racing.common.GameConfig;
import racing.common.dto.Obstacle;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.List;
import java.util.Locale;

/**
 * Màn hình đua kiểu game đua xe 8-bit cổ điển, chia đôi: trái là đường của mình, phải là đường
 * của đối thủ, mỗi bên một camera cuộn dọc bám theo xe ({@link TrackView}). Trên mỗi bên là thanh
 * HUD đen (tên, km/h, quãng đường, thứ hạng), mép ngoài có bản đồ nhỏ tiến độ của cả hai xe.
 * Vẽ lại khoảng 60 lần / giây; xe và xe cộ được nội suy giữa các lần cập nhật 20 Hz của server.
 * Chỉ vẽ, không chứa logic mạng.
 */
public final class RacePanel extends JPanel {

    private static final int HUD_H = 56;
    private static final int STATUS_H = 26;
    private static final int DIVIDER = 6;
    private static final int MAP_W = 10;
    private static final int FRAME_MS = 16;

    private static final Color BG = Color.BLACK;
    private static final Color HUD_TEXT = Color.WHITE;
    private static final Color HUD_ACCENT = new Color(0xFFD23F);
    private static final Color HUD_DIM = new Color(0x9A9A9A);
    private static final Color MAP_BG = new Color(0x2B2B2B);
    private static final Color MAP_ROAD = new Color(0x77797E);
    private static final Color MY_COLOR = new Color(0xE8392F);
    private static final Color OPP_COLOR = new Color(0x3C8CF0);
    private static final Color BOX_FILL = new Color(58, 58, 58, 248);
    private static final Color STATUS_TEXT = new Color(0xD8D8D8);
    private static final Font STATUS_FONT = new Font(Font.DIALOG, Font.PLAIN, 13);
    private static final Stroke BOX_STROKE = new BasicStroke(4f);

    private final TrackView mine;
    private final TrackView opponent;
    private final Timer frameTimer = new Timer(FRAME_MS, e -> repaint());
    private List<Obstacle> obstacles = List.of();

    private String myName = "YOU";
    private String opponentName = "RIVAL";
    private String countdownText = null;   // "3", "2", "1", "GO!" hoặc null
    private String statusText = "";
    private long raceTick;
    private long raceTickStampNanos;       // 0 = chưa nhận tick nào trong ván

    public RacePanel(CarModel myCar, CarModel opponentCar) {
        this.mine = new TrackView(myCar, PixelArt.PLAYER, 17);
        this.opponent = new TrackView(opponentCar, PixelArt.OPPONENT, 91);
        setPreferredSize(new Dimension(1200, 800));
        setBackground(BG);
        setDoubleBuffered(true);
        setFocusable(true);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        frameTimer.start();
    }

    @Override
    public void removeNotify() {
        frameTimer.stop();
        super.removeNotify();
    }

    /** Ván mới: danh sách xe cộ (giống nhau hai bên), xoá xe cộ đã tông và vụ nổ, tick về 0. */
    public void setObstacles(List<Obstacle> obstacles) {
        this.obstacles = obstacles == null ? List.of() : List.copyOf(obstacles);
        raceTick = 0;
        raceTickStampNanos = 0;
        mine.resetRound();
        opponent.resetRound();
        repaint();
    }

    /** Tên hiển thị trên HUD của hai bên. */
    public void setTitles(String myName, String opponentName) {
        this.myName = hudName(myName);
        this.opponentName = hudName(opponentName);
    }

    /** Tick server mới nhất (RaceState.tick): vị trí xe cộ = obstacle.positionAt(tick). */
    public void setRaceTick(long tick) {
        raceTick = tick;
        raceTickStampNanos = System.nanoTime();
    }

    public void setCountdownText(String text) {
        this.countdownText = text;
        repaint();
    }

    public void setStatusText(String text) {
        this.statusText = text == null ? "" : text;
    }

    public void flashMine() {
        mine.onHit(obstacles, tickF(System.nanoTime()), System.currentTimeMillis());
    }

    public void flashOpponent() {
        opponent.onHit(obstacles, tickF(System.nanoTime()), System.currentTimeMillis());
    }

    /** Tick có phần lẻ: tick server + thời gian cục bộ từ lúc nhận, tối đa 3 tick để không chạy quá xa. */
    private double tickF(long nowNanos) {
        if (raceTickStampNanos == 0) {
            return raceTick;
        }
        double ahead = (nowNanos - raceTickStampNanos) / (GameConfig.TICK_MS * 1e6);
        return raceTick + Math.max(0, Math.min(3, ahead));
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        long nowNanos = System.nanoTime();
        long nowMillis = System.currentTimeMillis();
        double tickF = tickF(nowNanos);
        mine.update(nowNanos);
        opponent.update(nowNanos);

        int w = getWidth();
        int h = getHeight();
        int sideW = (w - DIVIDER) / 2;
        int viewH = h - HUD_H - STATUS_H;

        paintSide(g, mine, opponent, 0, sideW, viewH, "1P", myName, MY_COLOR, true, tickF, nowMillis);
        paintSide(g, opponent, mine, sideW + DIVIDER, w - sideW - DIVIDER, viewH, "2P", opponentName, OPP_COLOR,
                false, tickF, nowMillis);

        g.setColor(BG);
        g.fillRect(sideW, 0, DIVIDER, h);
        paintStatus(g, w, h);
        if (countdownText != null) {
            paintCountdown(g, w, HUD_H + viewH / 2);
        }
        g.dispose();
    }

    private void paintSide(Graphics2D g, TrackView view, TrackView other, int x, int w, int viewH, String tag,
                           String name, Color color, boolean mapOnLeft, double tickF, long nowMillis) {
        Graphics2D clip = (Graphics2D) g.create(x, HUD_H, w, viewH);
        view.paint(clip, 0, 0, w, viewH, obstacles, tickF, nowMillis);
        clip.dispose();

        int mapX = mapOnLeft ? x + 8 : x + w - 8 - MAP_W;
        Color otherColor = color == MY_COLOR ? OPP_COLOR : MY_COLOR;
        paintMinimap(g, mapX, HUD_H + 14, viewH - 28, view.renderDist(), color, other.renderDist(), otherColor);
        paintHud(g, view, other, x, w, tag, name, color);
    }

    private void paintHud(Graphics2D g, TrackView view, TrackView other, int x, int w, String tag, String name,
                          Color color) {
        CarModel car = view.car();
        g.setColor(BG);
        g.fillRect(x, 0, w, HUD_H);
        int line1 = 4;
        int line2 = 4 + PixelArt.textHeight(2) + 2;
        int pad = 22;
        PixelArt.drawText(g, tag, x + pad, line1, 2, color, -1);
        PixelArt.drawText(g, name, x + pad + 42, line1, 2, HUD_TEXT, -1);
        PixelArt.drawText(g, String.format(Locale.ROOT, "%03d KM/H", Math.round(car.speed())), x + pad, line2, 2,
                HUD_TEXT, -1);

        String dist = String.format(Locale.ROOT, "%04d/%04d M", Math.round(view.renderDist()),
                Math.round(GameConfig.TRACK_LENGTH));
        PixelArt.drawText(g, dist, x + w - pad, line1, 2, HUD_TEXT, 1);
        String rank;
        Color rankColor;
        if (car.finished()) {
            rank = "FINISH!";
            rankColor = HUD_ACCENT;
        } else if (car.stunned()) {
            rank = "CRASH!";
            rankColor = MY_COLOR;
        } else if (view.renderDist() >= other.renderDist()) {
            rank = "1ST";
            rankColor = HUD_ACCENT;
        } else {
            rank = "2ND";
            rankColor = HUD_DIM;
        }
        PixelArt.drawText(g, rank, x + w - pad, line2, 2, rankColor, 1);
    }

    private static void paintMinimap(Graphics2D g, int x, int y, int h, double self, Color selfColor,
                                     double other, Color otherColor) {
        g.setColor(MAP_BG);
        g.fillRect(x - 2, y - 2, MAP_W + 4, h + 4);
        g.setColor(MAP_ROAD);
        g.fillRect(x + MAP_W / 2 - 2, y, 4, h);
        for (int i = 0; i < 2; i++) {   // ô cờ đích ở đỉnh
            g.setColor(i % 2 == 0 ? Color.WHITE : Color.BLACK);
            g.fillRect(x + i * MAP_W / 2, y, MAP_W / 2, 4);
        }
        g.setColor(Color.WHITE);
        g.fillRect(x, y + h - 2, MAP_W, 2);
        dot(g, x, y, h, other, otherColor, 3);
        dot(g, x, y, h, self, selfColor, 5);
    }

    private static void dot(Graphics2D g, int x, int y, int h, double meters, Color c, int r) {
        double ratio = Math.max(0, Math.min(1, meters / GameConfig.TRACK_LENGTH));
        int cy = y + h - (int) Math.round(ratio * h);
        g.setColor(Color.BLACK);
        g.fillRect(x + MAP_W / 2 - r - 1, cy - r - 1, 2 * r + 2, 2 * r + 2);
        g.setColor(c);
        g.fillRect(x + MAP_W / 2 - r, cy - r, 2 * r, 2 * r);
    }

    private void paintStatus(Graphics2D g, int w, int h) {
        g.setColor(BG);
        g.fillRect(0, h - STATUS_H, w, STATUS_H);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(STATUS_FONT);
        FontMetrics fm = g.getFontMetrics();
        int baseline = h - STATUS_H + (STATUS_H + fm.getAscent()) / 2 - 2;
        g.setColor(HUD_DIM);
        g.drawString("W/↑ tăng tốc   S/↓ phanh   A/D, ←/→ đổi làn   Esc thoát", 12, baseline);
        g.setColor(STATUS_TEXT);
        g.drawString(statusText, w - 12 - fm.stringWidth(statusText), baseline);
    }

    private void paintCountdown(Graphics2D g, int w, int cy) {
        String text = countdownText.toUpperCase(Locale.ROOT);
        int scale = text.length() <= 1 ? 9 : 6;
        int textW = PixelArt.text(text, Color.WHITE).getWidth() * scale;
        int textH = PixelArt.textHeight(scale);
        int boxW = Math.max(260, textW + 80);
        int boxH = textH + 40;
        int bx = (w - boxW) / 2;
        int by = cy - boxH / 2;
        g.setColor(BOX_FILL);
        g.fillRect(bx, by, boxW, boxH);
        Stroke old = g.getStroke();
        g.setStroke(BOX_STROKE);
        g.setColor(Color.WHITE);
        g.drawRect(bx + 6, by + 6, boxW - 12, boxH - 12);
        g.setStroke(old);
        PixelArt.drawText(g, text, w / 2, by + 20, scale, Color.WHITE, 0);
    }

    /** Tên trên HUD: chữ hoa, bỏ dấu ngoài bảng ASCII của font pixel, tối đa 10 ký tự. */
    private static String hudName(String s) {
        if (s == null || s.isBlank()) {
            return "?";
        }
        String n = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replace('đ', 'd').replace('Đ', 'D').toUpperCase(Locale.ROOT);
        return n.length() > 10 ? n.substring(0, 10) : n;
    }
}
