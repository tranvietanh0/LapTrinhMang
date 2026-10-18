package racing.client.ui.race;

import racing.client.ui.theme.Chamfer;
import racing.client.ui.theme.NfsFonts;
import racing.client.ui.theme.NfsPalette;
import racing.common.GameConfig;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

/**
 * HUD for the split race screen: a top bar (both drivers, race progress with a marker each) and a
 * dashboard under each road (arc speedometer, big italic speed, position, distance). Static chrome
 * is baked per size/scale; per frame it only blits and draws a handful of cached strings.
 */
final class RaceHud {
    static final Color ME = NfsPalette.AMBER_HOT;
    static final Color RIVAL = new Color(0x35B6FF);
    static final String STATUS_CRASH = "VA CHẠM!";
    static final String STATUS_FINISH = "VỀ ĐÍCH!";
    private static final Color[] RAMP_ME = {NfsPalette.AMBER, NfsPalette.AMBER_HOT, new Color(0xFF7A21), NfsPalette.RED};
    private static final Color[] RAMP_RIVAL = {new Color(0x1E7FD6), new Color(0x35B6FF), new Color(0x9BE0FF), Color.WHITE};

    private static final String[] SPEED = new String[(int) GameConfig.MAX_SPEED + 1];
    private static final String[] DIST = new String[(int) GameConfig.TRACK_LENGTH + 1];
    static {
        for (int i = 0; i < SPEED.length; i++) SPEED[i] = Integer.toString(i);
    }

    private record Txt(Font font, FontMetrics fm) {
        int width(String s) {
            return fm.stringWidth(s);
        }
    }

    private float u;
    private int w = -1, h = -1;
    private String myName = "", rivalName = "";
    int topH, dashH;
    private BufferedImage topBar, markerMe, markerRival, fillMe, fillRival;
    private final BufferedImage[] dash = new BufferedImage[2];
    private final int[] dashW = {-1, -1};
    private final int[] barX0 = new int[2], barX1 = new int[2];
    private final BufferedImage[] barLit = new BufferedImage[2];
    private static final String[] PCT = new String[101];
    private static final int BAR_SEGMENTS = 28;
    private RaceGauge gaugeMe, gaugeRival;
    private Txt speed, rank, rankSmall, dist, eyebrow, cap, strong, pct;
    private static final String TOTAL = " / " + (int) GameConfig.TRACK_LENGTH + " M";
    private int trackX0, trackX1, trackY, trackH, gaugeR;

    /** Rebuilds the cached chrome when the window size, scale or driver names change. */
    void layout(int width, int height, float scale, String me, String rival) {
        if (width == w && height == h && scale == u && me.equals(myName) && rival.equals(rivalName)) {
            return;
        }
        boolean rescale = scale != u;
        w = width; h = height; u = scale; myName = me; rivalName = rival;
        topH = Math.round(78 * u);
        dashH = Math.round(112 * u);
        gaugeR = Math.round(46 * u);
        BufferedImage probe = RaceGfx.image(4, 4);
        Graphics2D pg = probe.createGraphics();
        speed = txt(pg, NfsFonts.hero(46 * u));
        rank = txt(pg, NfsFonts.hero(54 * u));
        rankSmall = fitStatus(pg, rank.width("2ND") + Math.round(24 * u));
        dist = txt(pg, NfsFonts.display(28 * u));
        eyebrow = txt(pg, NfsFonts.tracked(NfsFonts.strong(12.5f * u), 0.12f));
        cap = txt(pg, NfsFonts.tracked(NfsFonts.display(15 * u), 0.08f));
        strong = txt(pg, NfsFonts.display(21 * u));
        pct = txt(pg, NfsFonts.display(17 * u));
        pg.dispose();
        if (rescale || gaugeMe == null) {
            gaugeMe = new RaceGauge(gaugeR, Math.max(5, Math.round(8.5f * u)), RAMP_ME);
            gaugeRival = new RaceGauge(gaugeR, Math.max(5, Math.round(8.5f * u)), RAMP_RIVAL);
        }
        buildTop();
        dashW[0] = dashW[1] = -1;
        barLit[0] = barLit[1] = null;
    }

    /** Status rank font, shrunk from 34u so every status label stays clear of the progress bar. */
    private Txt fitStatus(Graphics2D g, int maxWidth) {
        Txt t = txt(g, NfsFonts.hero(34 * u));
        for (float size = 34; size > 16 && (t.width(STATUS_CRASH) > maxWidth || t.width(STATUS_FINISH) > maxWidth); size -= 2) {
            t = txt(g, NfsFonts.hero(size * u));
        }
        return t;
    }

    private static Txt txt(Graphics2D g, Font f) {
        return new Txt(f, g.getFontMetrics(f));
    }

    // ------------------------------------------------------------------ top bar

    private void buildTop() {
        topBar = RaceGfx.image(w, topH);
        Graphics2D g = RaceGfx.gfx(topBar, 1);
        g.setPaint(new GradientPaint(0, 0, RaceGfx.a(NfsPalette.SURFACE_HI, 0.98), 0, topH, RaceGfx.a(NfsPalette.VOID, 0.98)));
        g.fillRect(0, 0, w, topH);
        g.setPaint(carbon());
        g.fillRect(0, 0, w, topH);
        g.setPaint(new LinearGradientPaint(0, topH - 3, w, topH - 3, new float[]{0f, 0.2f, 0.5f, 0.8f, 1f},
                new Color[]{RaceGfx.a(ME, 0), ME, NfsPalette.RED, RIVAL, RaceGfx.a(RIVAL, 0)}));
        g.fillRect(0, topH - Math.max(2, Math.round(3 * u)), w, Math.max(2, Math.round(3 * u)));
        chip(g, true);
        chip(g, false);
        trackX0 = Math.round(w * 0.31f);
        trackX1 = Math.round(w * 0.69f);
        trackH = Math.max(6, Math.round(8 * u));
        trackY = Math.round(topH * 0.68f);
        g.setColor(RaceGfx.a(Color.BLACK, 0.7));
        g.fill(Chamfer.shape(trackX0 - 2, trackY - 2, trackX1 - trackX0 + 4, trackH + 4, 5, 0, 5, 0));
        g.setColor(RaceGfx.a(NfsPalette.LINE, 0.9));
        g.fill(Chamfer.shape(trackX0, trackY, trackX1 - trackX0, trackH, 4, 0, 4, 0));
        g.setColor(RaceGfx.a(Color.WHITE, 0.22));
        for (int i = 1; i < 10; i++) {
            int tx = trackX0 + (trackX1 - trackX0) * i / 10;
            g.fillRect(tx, trackY + trackH, 1, Math.max(3, Math.round(4 * u)));
        }
        int cell = Math.max(3, Math.round(4 * u));
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 4; c++) {
                g.setColor((r + c) % 2 == 0 ? new Color(0xF2EFE6) : new Color(0x0A0C10));
                g.fillRect(trackX1 + Math.round(6 * u) + c * cell, trackY + trackH / 2 - cell * 3 / 2 + r * cell, cell, cell);
            }
        }
        g.setFont(cap.font());
        g.setColor(NfsPalette.MUTED);
        g.drawString("START", trackX0 - cap.width("START") - Math.round(10 * u), trackY + trackH);
        g.dispose();
        fillMe = fill(ME);
        fillRival = fill(RIVAL);
        markerMe = marker(ME, true);
        markerRival = marker(RIVAL, false);
    }

    private BufferedImage fill(Color c) {
        BufferedImage img = RaceGfx.image(trackX1 - trackX0, trackH);
        Graphics2D g = RaceGfx.gfx(img, 1);
        g.setPaint(new GradientPaint(0, 0, RaceGfx.shade(c, 1.15), 0, trackH, RaceGfx.shade(c, 0.65)));
        g.fillRect(0, 0, img.getWidth(), trackH);
        g.setColor(RaceGfx.a(Color.WHITE, 0.4));
        g.fillRect(0, 0, img.getWidth(), 1);
        g.dispose();
        return img;
    }

    private BufferedImage marker(Color c, boolean pointsDown) {
        int mw = Math.round(22 * u), mh = Math.round(15 * u), pad = Math.round(10 * u);
        BufferedImage img = RaceGfx.image(mw + 2 * pad, mh + 2 * pad);
        Graphics2D g = RaceGfx.gfx(img, 1);
        Path2D.Float tri = new Path2D.Float();
        float cx = pad + mw / 2f;
        if (pointsDown) {
            tri.moveTo(pad, pad); tri.lineTo(pad + mw, pad); tri.lineTo(cx, pad + mh);
        } else {
            tri.moveTo(pad, pad + mh); tri.lineTo(pad + mw, pad + mh); tri.lineTo(cx, pad);
        }
        tri.closePath();
        for (int i = 4; i >= 1; i--) {
            g.setColor(RaceGfx.a(c, 0.07 * (5 - i)));
            g.setStroke(new BasicStroke(i * 3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(tri);
        }
        g.setPaint(new GradientPaint(0, pad, RaceGfx.shade(c, 1.2), 0, pad + mh, RaceGfx.shade(c, 0.8)));
        g.fill(tri);
        g.setColor(RaceGfx.a(Color.WHITE, 0.55));
        g.setStroke(new BasicStroke(1f));
        g.draw(tri);
        g.dispose();
        return img;
    }

    /** Driver name plate: accent slash, small caption, name. */
    private void chip(Graphics2D g, boolean mine) {
        String name = mine ? myName : rivalName;
        Color c = mine ? ME : RIVAL;
        int ph = Math.round(54 * u), py = (topH - ph) / 2 - Math.round(2 * u);
        int tw = Math.max(strong.width(name), cap.width("ĐỐI THỦ")) + Math.round(46 * u);
        int px = mine ? Math.round(16 * u) : w - Math.round(16 * u) - tw;
        float cut = 12 * u;
        var shape = mine ? Chamfer.shape(px, py, tw, ph, cut, 0, cut, 0) : Chamfer.shape(px, py, tw, ph, 0, cut, 0, cut);
        g.setPaint(new GradientPaint(0, py, RaceGfx.a(NfsPalette.SURFACE_HI, 0.95), 0, py + ph, RaceGfx.a(NfsPalette.BG, 0.95)));
        g.fill(shape);
        g.setColor(NfsPalette.LINE);
        g.setStroke(new BasicStroke(1.2f));
        g.draw(shape);
        int bar = Math.round(7 * u);
        g.setColor(c);
        g.fill(mine ? Chamfer.shape(px, py, bar, ph, cut, 0, 0, 0) : Chamfer.shape(px + tw - bar, py, bar, ph, 0, cut, 0, 0));
        g.setColor(RaceGfx.a(c, 0.9));
        g.setFont(cap.font());
        int tx = mine ? px + bar + Math.round(14 * u) : px + Math.round(14 * u);
        String capText = mine ? "BẠN" : "ĐỐI THỦ";
        g.drawString(capText, tx, py + Math.round(17 * u));
        g.setFont(strong.font());
        g.setColor(NfsPalette.TEXT);
        g.drawString(name, tx, py + ph - Math.round(11 * u));
    }

    /** Race progress: both markers, filled bars and the distance read-out. */
    void paintTop(Graphics2D g, double myDist, double rivalDist) {
        g.drawImage(topBar, 0, 0, null);
        int len = trackX1 - trackX0;
        int mx = trackX0 + (int) Math.round(len * ratio(myDist));
        int rx = trackX0 + (int) Math.round(len * ratio(rivalDist));
        g.drawImage(fillRival, trackX0, trackY + trackH - Math.max(2, trackH / 3), trackX0 + (rx - trackX0), trackY + trackH,
                0, 0, Math.max(1, rx - trackX0), Math.max(2, trackH / 3), null);
        g.drawImage(fillMe, trackX0, trackY, trackX0 + (mx - trackX0), trackY + trackH - Math.max(2, trackH / 3),
                0, 0, Math.max(1, mx - trackX0), trackH - Math.max(2, trackH / 3), null);
        int pad = Math.round(10 * u);
        g.drawImage(markerMe, mx - markerMe.getWidth() / 2, trackY - markerMe.getHeight() + pad - Math.round(1 * u), null);
        g.drawImage(markerRival, rx - markerRival.getWidth() / 2, trackY + trackH - pad + Math.round(1 * u), null);
        String d = distText((int) Math.round(myDist));
        g.setFont(dist.font());
        int base = Math.round(topH * 0.42f);
        int full = dist.width(d) + cap.width(TOTAL);
        int sx = (trackX0 + trackX1) / 2 - full / 2;
        g.setColor(ME);
        g.drawString(d, sx, base);
        g.setFont(cap.font());
        g.setColor(NfsPalette.MUTED);
        g.drawString(TOTAL, sx + dist.width(d), base);
    }

    private static double ratio(double meters) {
        return Math.max(0, Math.min(1, meters / GameConfig.TRACK_LENGTH));
    }

    private static String distText(int m) {
        int i = Math.max(0, Math.min(DIST.length - 1, m));
        if (DIST[i] == null) DIST[i] = i + " M";
        return DIST[i];
    }

    // ------------------------------------------------------------------ dashboard

    /** One dashboard under a road. {@code mine} picks the amber (own) or blue (rival) styling. */
    void paintDash(Graphics2D g, int x, int y, int width, boolean mine, double speedKmh, double meters,
                   String rankText, Color rankColor, boolean statusRank, float rankScale) {
        int side = mine ? 0 : 1;
        if (dash[side] == null || dashW[side] != width) {
            dash[side] = buildDash(width, mine);
            dashW[side] = width;
        }
        g.drawImage(dash[side], x, y, null);
        Color accent = mine ? ME : RIVAL;
        int gcx = x + Math.round(18 * u) + gaugeR + Math.round(8 * u), gcy = y + dashH / 2 + Math.round(3 * u);
        (mine ? gaugeMe : gaugeRival).paint(g, gcx, gcy, speedKmh / GameConfig.MAX_SPEED);
        String sp = SPEED[Math.max(0, Math.min(SPEED.length - 1, (int) Math.round(speedKmh)))];
        g.setFont(speed.font());
        int sx = gcx - speed.width(sp) / 2, sy = gcy + Math.round(8 * u);
        g.setColor(RaceGfx.a(Color.BLACK, 0.6));
        g.drawString(sp, sx + 2, sy + 2);
        g.setColor(NfsPalette.TEXT);
        g.drawString(sp, sx, sy);
        g.setFont(cap.font());
        g.setColor(accent);
        g.drawString("KM/H", gcx - cap.width("KM/H") / 2, gcy + Math.round(30 * u));

        int col1 = gcx + gaugeR + Math.round(30 * u);
        g.setFont(eyebrow.font());
        g.setColor(NfsPalette.MUTED);
        g.drawString("VỊ TRÍ", col1, y + Math.round(36 * u));
        Txt rt = statusRank ? rankSmall : rank;
        int ry = y + dashH - Math.round(24 * u);
        double ax = col1 + rt.width(rankText) / 2.0, ay = ry - rt.fm().getAscent() * 0.35;
        if (rankScale != 1f) {   // pop around the text centre, undone below without allocating a transform
            g.translate(ax, ay);
            g.scale(rankScale, rankScale);
            g.translate(-ax, -ay);
        }
        g.setFont(rt.font());
        g.setColor(RaceGfx.a(Color.BLACK, 0.6));
        g.drawString(rankText, col1 + 2, ry + 2);
        g.setColor(rankColor);
        g.drawString(rankText, col1, ry);
        if (rankScale != 1f) {
            g.translate(ax, ay);
            g.scale(1 / rankScale, 1 / rankScale);
            g.translate(-ax, -ay);
        }

        paintProgress(g, x, y, side, meters);
        String d = distText((int) Math.round(meters));
        int right = x + width - Math.round(26 * u);
        g.setFont(eyebrow.font());
        g.setColor(NfsPalette.MUTED);
        String lab = "QUÃNG ĐƯỜNG";
        if (right - eyebrow.width(lab) > col1 + rank.width("2ND") + Math.round(20 * u)) {
            g.drawString(lab, right - eyebrow.width(lab), y + Math.round(36 * u));
            g.setFont(dist.font());
            g.setColor(NfsPalette.TEXT);
            g.drawString(d, right - dist.width(d), y + dashH - Math.round(24 * u));
        }
    }

    /** Segmented race-progress bar in the dashboard centre (skipped when the dash is too narrow). */
    private void paintProgress(Graphics2D g, int x, int y, int side, double meters) {
        if (barX1[side] - barX0[side] < Math.round(80 * u)) {
            return;
        }
        int percent = (int) Math.round(ratio(meters) * 100);
        String p = PCT[percent];
        if (p == null) p = PCT[percent] = percent + "%";
        int len = barX1[side] - barX0[side];
        int lit = (int) Math.round(ratio(meters) * BAR_SEGMENTS);
        if (lit > 0) {
            int litW = Math.min(len, (int) Math.ceil(len * lit / (double) BAR_SEGMENTS));
            g.drawImage(barLit[side], x + barX0[side], y + barY(), x + barX0[side] + litW, y + barY() + barLit[side].getHeight(),
                    0, 0, litW, barLit[side].getHeight(), null);
        }
        g.setFont(pct.font());
        g.setColor(NfsPalette.TEXT);
        g.drawString(p, x + barX1[side] - pct.width(p), y + barY() - Math.round(8 * u));
    }

    private int barY() {
        return dashH - Math.round(36 * u);
    }

    private void bakeBar(Graphics2D g, int side, int width, boolean mine) {
        int col1 = Math.round(18 * u) + gaugeR + Math.round(8 * u) + gaugeR + Math.round(30 * u);
        barX0[side] = col1 + rank.width("2ND") + Math.round(34 * u);
        barX1[side] = width - Math.round(26 * u) - dist.width("2500 M") - Math.round(34 * u);
        int len = barX1[side] - barX0[side];
        if (len < Math.round(80 * u)) {
            return;
        }
        int bh = Math.max(6, Math.round(9 * u)), skew = Math.max(2, Math.round(3 * u));
        g.setFont(eyebrow.font());
        g.setColor(NfsPalette.MUTED);
        g.drawString("TIẾN ĐỘ", barX0[side], barY() - Math.round(8 * u));
        BufferedImage lit = RaceGfx.image(len, bh);
        Graphics2D lg = RaceGfx.gfx(lit, 1);
        Color[] ramp = mine ? RAMP_ME : RAMP_RIVAL;
        for (int i = 0; i < BAR_SEGMENTS; i++) {
            int x0 = Math.round(i * len / (float) BAR_SEGMENTS), x1 = Math.round((i + 1) * len / (float) BAR_SEGMENTS) - 2;
            Path2D.Float seg = new Path2D.Float();
            seg.moveTo(x0 + skew, 0); seg.lineTo(x1 + skew, 0); seg.lineTo(x1, bh); seg.lineTo(x0, bh); seg.closePath();
            g.setColor(RaceGfx.a(NfsPalette.LINE, 0.7));
            seg.transform(java.awt.geom.AffineTransform.getTranslateInstance(barX0[side], barY()));
            g.fill(seg);
            lg.setPaint(new GradientPaint(0, 0, ramp[1], 0, bh, ramp[0]));
            lg.fill(new Path2D.Float(seg, java.awt.geom.AffineTransform.getTranslateInstance(-barX0[side], -barY())));
        }
        lg.dispose();
        barLit[side] = lit;
    }

    private BufferedImage buildDash(int width, boolean mine) {
        BufferedImage img = RaceGfx.image(width, dashH);
        Graphics2D g = RaceGfx.gfx(img, 1);
        Color accent = mine ? ME : RIVAL;
        float cut = 18 * u;
        var shape = mine ? Chamfer.shape(0.5f, 0.5f, width - 1, dashH - 1, 0, cut, 0, 0)
                : Chamfer.shape(0.5f, 0.5f, width - 1, dashH - 1, cut, 0, 0, 0);
        g.setPaint(new GradientPaint(0, 0, RaceGfx.a(NfsPalette.SURFACE_HI, 0.98), 0, dashH, RaceGfx.a(NfsPalette.VOID, 0.99)));
        g.fill(shape);
        g.setPaint(carbon());
        g.fill(shape);
        g.setColor(NfsPalette.LINE);
        g.setStroke(new BasicStroke(1.2f));
        g.draw(shape);
        int bar = Math.max(3, Math.round(4 * u));
        g.setPaint(new LinearGradientPaint(mine ? 0 : width, 0, mine ? width * 0.7f : width * 0.3f, 0,
                new float[]{0f, 1f}, new Color[]{accent, RaceGfx.a(accent, 0)}));
        g.fillRect(mine ? 0 : (int) (width * 0.3), 0, (int) (width * 0.7), bar);
        g.setColor(RaceGfx.a(NfsPalette.RED, 0.9));
        g.fillRect(mine ? Math.round(cut) + 4 : width - Math.round(cut) - 4 - Math.round(20 * u), 0, Math.round(20 * u), bar);
        g.setPaint(new GradientPaint(0, 0, RaceGfx.a(Color.WHITE, 0.07), 0, dashH * 0.4f, RaceGfx.a(Color.WHITE, 0)));
        g.fill(shape);
        bakeBar(g, mine ? 0 : 1, width, mine);
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

    float scale() {
        return u;
    }
}
