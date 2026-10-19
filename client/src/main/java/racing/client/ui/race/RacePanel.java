package racing.client.ui.race;

import racing.client.model.CarModel;
import racing.client.ui.theme.Chamfer;
import racing.client.ui.theme.Motion;
import racing.client.ui.theme.NfsFonts;
import racing.client.ui.theme.NfsPalette;
import racing.common.dto.Obstacle;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Màn hình đua kiểu Need for Speed đường phố, chia đôi: trái là đường của mình, phải là đường của
 * đối thủ, mỗi bên một camera cuộn dọc bám theo xe ({@link TrackView}). Thanh trên cùng có tên hai
 * tay đua và tiến độ về đích, dưới mỗi đường là bảng đồng hồ (tốc độ, vị trí, quãng đường).
 * Vẽ lại khoảng 60 lần / giây; xe và xe cộ được nội suy giữa các lần cập nhật 20 Hz của server.
 * Mọi lớp tĩnh là ảnh dựng sẵn theo cỡ cửa sổ; chỉ vẽ, không chứa logic mạng.
 */
public final class RacePanel extends JPanel {

    private static final int FRAME_MS = 16;
    private static final int MAX_NAME = 14;
    private static final Color STATUS_DOT = NfsPalette.AMBER;
    /** Rank / status text pop when it changes (1ST to 2ND, VA CHẠM!, VỀ ĐÍCH!). */
    private static final long RANK_POP_NANOS = 380_000_000L;

    private final TrackView mine;
    private final TrackView opponent;
    private final RaceHud hud = new RaceHud();
    private final RaceCountdown countdown = new RaceCountdown();
    private ScheduledExecutorService frameClock;
    private List<Obstacle> obstacles = List.of();

    private String myName = "BẠN";
    private String opponentName = "ĐỐI THỦ";
    private String statusText = "";
    private BufferedImage statusChip;
    private String statusChipText;
    private float statusChipScale;
    private final RaceClock raceClock = new RaceClock();
    private final String[] shownRank = new String[2];
    private final long[] rankChangedAt = new long[2];

    public RacePanel(CarModel myCar, CarModel opponentCar) {
        this.mine = new TrackView(myCar, false, 17);
        this.opponent = new TrackView(opponentCar, true, 91);
        setPreferredSize(new Dimension(1200, 780));
        setBackground(NfsPalette.VOID);
        setOpaque(true);
        setDoubleBuffered(true);
        setFocusable(true);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if(frameClock==null) {
            frameClock=Executors.newSingleThreadScheduledExecutor(r->{
                Thread thread=new Thread(r,"race-render-clock");thread.setDaemon(true);return thread;
            });
            // Fixed deadlines avoid the ~31 ms cadence of a repeating 16 ms Swing Timer on Windows.
            // repaint is thread-safe and RepaintManager coalesces requests if the EDT is busy.
            frameClock.scheduleAtFixedRate(this::repaint,0,FRAME_MS,TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void removeNotify() {
        if(frameClock!=null) { frameClock.shutdownNow();frameClock=null; }
        super.removeNotify();
    }

    /** Ván mới: danh sách xe cộ (giống nhau hai bên), xoá xe cộ đã tông và vụ nổ, tick về 0. */
    public void setObstacles(List<Obstacle> obstacles) {
        this.obstacles = obstacles == null ? List.of() : List.copyOf(obstacles);
        raceClock.reset();
        mine.resetRound();
        opponent.resetRound();
        shownRank[0] = shownRank[1] = null;
        repaint();
    }

    /** Tên hiển thị trên HUD của hai bên. */
    public void setTitles(String myName, String opponentName) {
        this.myName = hudName(myName);
        this.opponentName = hudName(opponentName);
    }

    /** Tick server mới nhất (RaceState.tick): vị trí xe cộ = obstacle.positionAt(tick). */
    public void setRaceTick(long tick) {
        raceClock.accept(tick,System.nanoTime());
    }

    public void setCountdownText(String text) {
        countdown.set(text, System.nanoTime());
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
        return raceClock.sample(nowNanos);
    }

    /** Hệ số cỡ HUD theo cửa sổ, bước 0,05 để kéo cửa sổ không làm dựng lại liên tục. */
    static float hudScale(int w, int h) {
        double raw = Math.min(w / 1280.0, h / 760.0);
        return Math.max(0.8f, Math.min(1.9f, Math.round(raw * 20) / 20f));
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        long nowNanos = System.nanoTime();
        long nowMillis = System.currentTimeMillis();
        double tickF = tickF(nowNanos);
        mine.update(nowNanos);
        opponent.update(nowNanos);

        float u = hudScale(w, h);
        hud.layout(w, h, u, myName, opponentName);
        int divider = Math.max(4, Math.round(8 * u));
        int sideW = (w - divider) / 2;
        int rightW = w - sideW - divider;
        int viewH = Math.max(40, h - hud.topH - hud.dashH);
        int n = Math.min(TrackView.tileSizeFor(sideW, viewH), TrackView.tileSizeFor(rightW, viewH));

        g.setColor(NfsPalette.VOID);
        g.fillRect(0, 0, w, h);
        Graphics2D left = (Graphics2D) g.create(0, hud.topH, sideW, viewH);
        mine.paint(left, 0, 0, sideW, viewH, obstacles, tickF, nowMillis, n);
        left.dispose();
        Graphics2D right = (Graphics2D) g.create(sideW + divider, hud.topH, rightW, viewH);
        opponent.paint(right, 0, 0, rightW, viewH, obstacles, tickF, nowMillis, n);
        right.dispose();
        paintDivider(g, sideW, divider, viewH);

        hud.paintTop(g, mine.renderDist(), opponent.renderDist());
        paintDash(g, mine, opponent, true, 0, sideW, viewH, nowNanos);
        paintDash(g, opponent, mine, false, sideW + divider, rightW, viewH, nowNanos);
        g.setColor(NfsPalette.VOID);
        g.fillRect(sideW, hud.topH + viewH, divider, hud.dashH);

        paintStatus(g, w, hud.topH + Math.round(14 * u), u);
        countdown.paint(g, w, hud.topH + viewH / 2, u, nowNanos);
        g.dispose();
    }

    private void paintDivider(Graphics2D g, int x, int width, int viewH) {
        g.setColor(NfsPalette.VOID);
        g.fillRect(x, hud.topH, width, viewH);
        g.setColor(NfsPalette.AMBER_DEEP);
        g.fillRect(x + width / 2 - 1, hud.topH, 2, viewH);
        g.setColor(NfsPalette.LINE);
        g.fillRect(x, hud.topH, 1, viewH);
        g.fillRect(x + width - 1, hud.topH, 1, viewH);
    }

    private void paintDash(Graphics2D g, TrackView view, TrackView other, boolean isMine, int x, int width, int viewH,
                           long nowNanos) {
        CarModel car = view.car();
        Color accent = isMine ? RaceHud.ME : RaceHud.RIVAL;
        String rank;
        Color rankColor;
        boolean status = false;
        if (car.finished()) {
            rank = RaceHud.STATUS_FINISH;
            rankColor = accent;
            status = true;
        } else if (car.stunned()) {
            rank = RaceHud.STATUS_CRASH;
            rankColor = NfsPalette.RED;
            status = true;
        } else if (isMine ? view.renderDist() >= other.renderDist() : view.renderDist() > other.renderDist()) {
            rank = "1ST";
            rankColor = accent;
        } else {
            rank = "2ND";
            rankColor = NfsPalette.MUTED;
        }
        hud.paintDash(g, x, hud.topH + viewH, width, isMine, view.shownSpeed(), view.renderDist(), rank, rankColor, status,
                rankPop(isMine ? 0 : 1, rank, nowNanos));
    }

    /** Scale of the rank text: pops in (OUT_BACK) for a moment after it changes, otherwise exactly 1. */
    private float rankPop(int side, String rank, long nowNanos) {
        if (!rank.equals(shownRank[side])) {
            rankChangedAt[side] = shownRank[side] == null ? nowNanos - RANK_POP_NANOS : nowNanos;
            shownRank[side] = rank;
        }
        long age = nowNanos - rankChangedAt[side];
        if (age >= RANK_POP_NANOS) {
            return 1f;
        }
        return 0.45f + 0.55f * Motion.OUT_BACK.apply(age / (float) RANK_POP_NANOS);
    }

    /** Dòng trạng thái (đang đếm ngược, đang chờ đối thủ...) dạng viên thuốc ở giữa, chỉ dựng lại khi đổi chữ. */
    private void paintStatus(Graphics2D g, int w, int y, float u) {
        if (statusText.isEmpty()) {
            return;
        }
        if (statusChip == null || !statusText.equals(statusChipText) || statusChipScale != u) {
            statusChip = buildStatusChip(statusText, u);
            statusChipText = statusText;
            statusChipScale = u;
        }
        g.drawImage(statusChip, (w - statusChip.getWidth()) / 2, y, null);
    }

    private static BufferedImage buildStatusChip(String text, float u) {
        var font = NfsFonts.tracked(NfsFonts.display(16 * u), 0.06f);
        BufferedImage probe = RaceGfx.image(4, 4);
        Graphics2D pg = probe.createGraphics();
        FontMetrics fm = pg.getFontMetrics(font);
        pg.dispose();
        String shown = NfsFonts.safe(text).toUpperCase(Locale.ROOT);
        int pad = Math.round(16 * u), dot = Math.round(8 * u);
        int cw = fm.stringWidth(shown) + 2 * pad + dot + Math.round(8 * u), ch = Math.round(32 * u);
        BufferedImage img = RaceGfx.image(cw + 4, ch + 4);
        Graphics2D g = RaceGfx.gfx(img, 1);
        var shape = Chamfer.shape(2, 2, cw, ch, 9 * u, 0, 9 * u, 0);
        g.setColor(RaceGfx.a(NfsPalette.VOID, 0.82));
        g.fill(shape);
        g.setColor(RaceGfx.a(NfsPalette.AMBER, 0.75));
        g.setStroke(new BasicStroke(1.3f));
        g.draw(shape);
        g.setColor(STATUS_DOT);
        g.fillOval(2 + pad, 2 + (ch - dot) / 2, dot, dot);
        g.setFont(font);
        g.setColor(NfsPalette.TEXT);
        g.drawString(shown, 2 + pad + dot + Math.round(8 * u), 2 + (ch + fm.getAscent() - fm.getDescent()) / 2);
        g.dispose();
        return img;
    }

    /** Tên trên HUD: chữ hoa (giữ dấu tiếng Việt), tối đa {@value #MAX_NAME} ký tự. */
    private static String hudName(String s) {
        if (s == null || s.isBlank()) {
            return "?";
        }
        String n = NfsFonts.safe(java.text.Normalizer.normalize(s.trim(), java.text.Normalizer.Form.NFC))
                .toUpperCase(Locale.ROOT);
        return n.length() > MAX_NAME ? n.substring(0, MAX_NAME) : n;
    }
}
