package racing.client.ui.race;

import racing.client.ui.GameTheme;
import racing.client.ui.theme.Chamfer;
import racing.client.ui.theme.NfsFonts;
import racing.client.ui.theme.NfsPalette;
import racing.common.GameConfig;
import racing.common.dto.EndReason;
import racing.common.dto.MatchOutcome;
import racing.common.dto.MatchResult;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.Locale;

/**
 * The dramatic VICTORY / DEFEAT card: outcome glow and speed lines, a huge italic title that pops in,
 * the reason and three stat plates (time, average speed, points). Chrome and title are baked per size;
 * a short intro timer animates the title and plates, then stops.
 */
final class RaceResultCard extends JPanel {
    static final int BASE_W = 680, BASE_H = 470;
    private static final int INTRO_MS = 620;

    private final MatchResult result;
    private final String me, opponent;
    private final float u;
    private final JButton close;
    private final Color accent, accent2;
    private final String title, subtitle, reason;
    private final Timer intro = new Timer(16, e -> repaint());
    private long openedAt;
    private BufferedImage chrome, titleImage;
    private int builtW = -1, builtH = -1;

    RaceResultCard(MatchResult result, String me, String opponent, float scale, Runnable onClose) {
        super(null);
        this.result = result;
        this.me = me == null ? "" : me;
        this.opponent = opponent == null ? "" : opponent;
        this.u = scale;
        setOpaque(false);
        setPreferredSize(new Dimension(Math.round(BASE_W * u), Math.round(BASE_H * u)));
        MatchOutcome o = result.outcome();
        accent = switch (o) {
            case WIN -> NfsPalette.AMBER;
            case LOSE -> NfsPalette.RED;
            case DRAW -> new Color(0x35B6FF);
            case ABORTED -> NfsPalette.MUTED;
        };
        accent2 = switch (o) {
            case WIN -> NfsPalette.AMBER_HOT;
            case LOSE -> new Color(0xFF6A5A);
            case DRAW -> new Color(0x9BE0FF);
            case ABORTED -> NfsPalette.TEXT;
        };
        title = switch (o) {
            case WIN -> "VICTORY";
            case LOSE -> "DEFEAT";
            case DRAW -> "DRAW";
            case ABORTED -> "ABORTED";
        };
        subtitle = switch (o) {
            case WIN -> "Bạn thắng!";
            case LOSE -> "Bạn thua";
            case DRAW -> "Hòa";
            case ABORTED -> "Trận bị hủy";
        };
        reason = "Lý do: " + ResultDialog.reasonText(result.reason(), o);
        close = GameTheme.button("TIẾP TỤC", true);
        close.addActionListener(e -> onClose.run());
        add(close);
    }

    JButton closeButton() {
        return close;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        openedAt = System.nanoTime();
        intro.start();
    }

    @Override
    public void removeNotify() {
        intro.stop();
        super.removeNotify();
    }

    @Override
    public void doLayout() {
        int bw = Math.round(250 * u), bh = Math.round(60 * u);
        close.setBounds((getWidth() - bw) / 2, getHeight() - bh - Math.round(24 * u), bw, bh);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        if (chrome == null || builtW != w || builtH != h) {
            chrome = buildChrome(w, h);
            titleImage = RaceCountdown.glowText(title, NfsFonts.hero(112 * u), accent, Color.WHITE, accent2, u, 0, false);
            builtW = w;
            builtH = h;
        }
        // off-screen painting (printing, screenshots) always shows the finished card
        double t = isShowing() ? Math.max(0, Math.min(1, (System.nanoTime() - openedAt) / (INTRO_MS * 1e6))) : 1;
        if (t >= 1) {
            intro.stop();
        }
        double ease = 1 - Math.pow(1 - t, 3);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(chrome, 0, 0, null);

        double scale = 1 + 0.45 * (1 - ease);
        int dw = (int) Math.round(titleImage.getWidth() * scale), dh = (int) Math.round(titleImage.getHeight() * scale);
        g.setComposite(RaceGfx.alpha(Math.min(1, t * 2.2)));
        g.drawImage(titleImage, (w - dw) / 2, Math.round(122 * u) - dh / 2, dw, dh, null);
        g.setComposite(RaceGfx.alpha(Math.max(0, (t - 0.35) / 0.65)));

        g.setFont(NfsFonts.tracked(NfsFonts.display(23 * u), 0.06f));
        g.setColor(accent2);
        centered(g, NfsFonts.safe(subtitle).toUpperCase(Locale.ROOT), w / 2, Math.round(212 * u));
        g.setFont(NfsFonts.body(16 * u));
        g.setColor(NfsPalette.MUTED);
        centered(g, NfsFonts.safe(reason), w / 2, Math.round(240 * u));
        paintStats(g, w);
        g.dispose();
    }

    private void paintStats(Graphics2D g, int w) {
        int pad = Math.round(36 * u), gap = Math.round(14 * u);
        int pw = (w - 2 * pad - 2 * gap) / 3, ph = Math.round(84 * u), py = Math.round(264 * u);
        boolean paced = result.elapsedMillis() > 0;
        boolean ranked = result.reason() == EndReason.FINISH || result.reason() == EndReason.DRAW;
        String time = paced ? String.format(Locale.ROOT, "%.1f S", result.elapsedMillis() / 1000.0) : "—";
        String avg = paced && ranked
                ? Math.round(GameConfig.TRACK_LENGTH / (result.elapsedMillis() / 1000.0) * 3.6) + " KM/H" : "—";
        plate(g, pad, py, pw, ph, "THỜI GIAN", time, null, null);
        plate(g, pad + pw + gap, py, pw, ph, "TỐC ĐỘ TB", avg, null, null);
        plate(g, pad + 2 * (pw + gap), py, pw, ph, "ĐIỂM", result.myPoints() + " – " + result.opponentPoints(),
                clip(g, me + " · " + opponent, pw - Math.round(24 * u), NfsFonts.strong(12 * u)), result.myPoints());
    }

    private void plate(Graphics2D g, int x, int y, int w, int h, String caption, String value, String sub, Integer mine) {
        var shape = Chamfer.slash(x, y, w, h, 12 * u);
        g.setPaint(new GradientPaint(0, y, RaceGfx.a(NfsPalette.SURFACE_HI, 0.95), 0, y + h, RaceGfx.a(NfsPalette.BG, 0.95)));
        g.fill(shape);
        g.setColor(NfsPalette.LINE);
        g.setStroke(new BasicStroke(1.2f));
        g.draw(shape);
        g.setColor(accent);
        g.fillRect(x + Math.round(12 * u), y, Math.round(34 * u), Math.max(2, Math.round(3 * u)));
        g.setFont(NfsFonts.tracked(NfsFonts.strong(12 * u), 0.14f));
        g.setColor(NfsPalette.MUTED);
        g.drawString(caption, x + Math.round(14 * u), y + Math.round(24 * u));
        g.setFont(NfsFonts.display(34 * u));
        g.setColor(mine == null ? NfsPalette.TEXT : accent2);
        g.drawString(NfsFonts.safe(value), x + Math.round(14 * u), y + Math.round(62 * u));
        if (sub != null) {
            g.setFont(NfsFonts.strong(12 * u));
            g.setColor(NfsPalette.MUTED);
            g.drawString(sub, x + Math.round(14 * u), y + h - Math.round(5 * u));
        }
    }

    private static String clip(Graphics2D g, String text, int max, Font font) {
        FontMetrics fm = g.getFontMetrics(font);
        if (fm.stringWidth(text) <= max) {
            return text;
        }
        String s = text;
        while (s.length() > 1 && fm.stringWidth(s + "…") > max) {
            s = s.substring(0, s.length() - 1);
        }
        return s + "…";
    }

    private static void centered(Graphics2D g, String s, int cx, int baseline) {
        g.drawString(s, cx - g.getFontMetrics().stringWidth(s) / 2, baseline);
    }

    private BufferedImage buildChrome(int w, int h) {
        BufferedImage img = RaceGfx.image(w, h);
        Graphics2D g = RaceGfx.gfx(img, 1);
        float cut = 30 * u;
        var body = Chamfer.shape(1, 1, w - 2, h - 2, cut, 0, cut, 0);
        g.setPaint(new GradientPaint(0, 0, NfsPalette.SURFACE_HI, 0, h, NfsPalette.VOID));
        g.fill(body);
        g.setPaint(carbon());
        g.fill(body);
        java.awt.Shape oldClip = g.getClip();
        g.clip(body);
        g.setPaint(new RadialGradientPaint(w / 2f, Math.round(120 * u), w * 0.62f, new float[]{0f, 0.55f, 1f},
                new Color[]{RaceGfx.a(accent, 0.42), RaceGfx.a(accent, 0.12), RaceGfx.a(accent, 0)}));
        g.fillRect(0, 0, w, h);
        g.setStroke(new BasicStroke(Math.max(1.5f, 2.2f * u)));
        for (int i = 0; i < 16; i++) {
            float y = Math.round(40 * u) + i * 14 * u;
            float len = (90 + (i * 53) % 190) * u, x0 = ((i * 97) % 420) * u;
            g.setPaint(new GradientPaint(x0, y, RaceGfx.a(accent2, 0), x0 + len, y, RaceGfx.a(accent2, i % 3 == 0 ? 0.30 : 0.14)));
            g.draw(new Line2D.Float(x0, y, x0 + len, y - 26 * u));
        }
        int cell = Math.round(11 * u);
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c * cell < w; c++) {
                g.setColor((r + c) % 2 == 0 ? RaceGfx.a(Color.WHITE, 0.16) : RaceGfx.a(Color.BLACK, 0.4));
                g.fillRect(c * cell, h - Math.round(8 * u) - cell * 2 + r * cell, cell, cell);
            }
        }
        g.setClip(oldClip);
        g.setColor(NfsPalette.LINE);
        g.setStroke(new BasicStroke(1.4f));
        g.draw(body);
        float bar = Math.min(w * 0.42f, 300 * u);
        g.setColor(accent);
        g.fill(Chamfer.shape(1, 1, bar, 5 * u, cut * 0.9f, 0, 0, 0));
        g.setColor(NfsPalette.RED);
        g.fillRect(Math.round(bar) + 8, 1, Math.round(26 * u), Math.round(5 * u));
        g.setColor(RaceGfx.a(accent, 0.9));
        g.setStroke(new BasicStroke(3f));
        g.drawLine(w - 2, h - 2 - Math.round(cut) - 18, w - 2, h - 2 - Math.round(cut));
        g.dispose();
        return img;
    }

    private static TexturePaint carbon() {
        BufferedImage weave = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = weave.createGraphics();
        Color dark = new Color(0, 0, 0, 70), light = new Color(255, 255, 255, 14);
        for (int y = 0; y < 8; y += 4) {
            for (int x = 0; x < 8; x += 4) {
                boolean alt = ((x + y) / 4) % 2 == 0;
                g.setColor(alt ? dark : light);
                g.fillRect(x, y, 4, 2);
                g.setColor(alt ? light : dark);
                g.fillRect(x, y + 2, 4, 2);
            }
        }
        g.dispose();
        return new TexturePaint(weave, new Rectangle(0, 0, 8, 8));
    }
}
