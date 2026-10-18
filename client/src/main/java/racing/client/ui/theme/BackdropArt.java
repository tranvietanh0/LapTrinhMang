package racing.client.ui.theme;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Random;

/** Paints the static night-city backdrop (sky, skyline, wet road, vignette) into an image once per size. */
final class BackdropArt {
    private static final float[] SKY_AT = {0f, 0.5f, 1f};
    private static final Color[] SKY = {new Color(0x03050A), new Color(0x0D1122), new Color(0x2B1A1E)};

    private BackdropArt() { }

    static BufferedImage render(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int horizon = Math.round(h * 0.60f);
        double vpx = w * 0.60;
        sky(g, w, h, horizon);
        skyline(g, w, horizon, 0.46f, new Color(0x10162A), 1L, h);
        skyline(g, w, horizon, 0.30f, new Color(0x080B15), 2L, h);
        road(g, w, h, horizon, vpx);
        slashes(g, w, h);
        scanlines(g, w, h);
        vignette(g, w, h);
        g.dispose();
        return img;
    }

    private static void sky(Graphics2D g, int w, int h, int horizon) {
        g.setPaint(new LinearGradientPaint(0, 0, 0, horizon, SKY_AT, SKY));
        g.fillRect(0, 0, w, horizon);
        glow(g, w * 0.60, horizon, w * 0.75, h * 0.34, NfsPalette.alpha(NfsPalette.AMBER, 0.34f));
        glow(g, w * 0.88, horizon, w * 0.35, h * 0.22, NfsPalette.alpha(NfsPalette.RED, 0.26f));
        glow(g, w * 0.12, horizon, w * 0.30, h * 0.18, NfsPalette.alpha(new Color(0x2F6BFF), 0.16f));
    }

    /** Elliptical radial glow, centre fully tinted fading to transparent. */
    private static void glow(Graphics2D g, double cx, double cy, double rx, double ry, Color c) {
        Graphics2D gg = (Graphics2D) g.create();
        gg.translate(cx, cy);
        gg.scale(1, ry / rx);
        gg.setPaint(new RadialGradientPaint(new Point2D.Double(0, 0), (float) rx, new float[]{0f, 1f},
                new Color[]{c, NfsPalette.alpha(c, 0f)}));
        gg.fill(new java.awt.geom.Ellipse2D.Double(-rx, -rx, rx * 2, rx * 2));
        gg.dispose();
    }

    private static void skyline(Graphics2D g, int w, int horizon, float heightRatio, Color body, long seed, int h) {
        Random rnd = new Random(seed);
        double maxH = h * heightRatio;
        for (int x = -10; x < w; ) {
            int bw = (int) (w * (0.018 + rnd.nextDouble() * 0.04));
            int bh = (int) (maxH * (0.25 + rnd.nextDouble() * 0.75));
            g.setColor(body);
            g.fillRect(x, horizon - bh, bw, bh + 2);
            if (rnd.nextInt(4) == 0) g.fillRect(x + bw / 3, horizon - bh - (int) (maxH * 0.1), Math.max(2, bw / 8), (int) (maxH * 0.1));
            int cell = Math.max(4, h / 120);
            for (int wy = horizon - bh + cell; wy < horizon - cell; wy += cell * 2) {
                for (int wx = x + cell; wx < x + bw - cell; wx += cell * 2) {
                    if (rnd.nextInt(7) != 0) continue;
                    Color lit = rnd.nextInt(5) == 0 ? new Color(0xFFD9A0) : (rnd.nextInt(3) == 0 ? new Color(0x7FB5FF) : NfsPalette.AMBER);
                    g.setColor(NfsPalette.alpha(lit, 0.35f + rnd.nextFloat() * 0.5f));
                    g.fillRect(wx, wy, cell - 1, cell - 1);
                }
            }
            x += bw + rnd.nextInt(3);
        }
    }

    private static void road(Graphics2D g, int w, int h, int horizon, double vpx) {
        g.setPaint(new GradientPaint(0, horizon, new Color(0x10141C), 0, h, new Color(0x05070B)));
        g.fillRect(0, horizon, w, h - horizon);
        double span = h - horizon;
        // wet reflections of the horizon glow
        g.setPaint(new GradientPaint(0, horizon, NfsPalette.alpha(NfsPalette.AMBER, 0.20f), 0, h, NfsPalette.alpha(NfsPalette.AMBER, 0f)));
        g.fill(poly(vpx - w * 0.10, horizon, vpx + w * 0.10, horizon, vpx + w * 0.55, h, vpx - w * 0.55, h));
        // lane dashes in perspective: depth t in (0,1], position scales with t^1.6
        for (int lane = -2; lane <= 2; lane++) {
            if (lane == 0) continue;
            boolean solid = Math.abs(lane) == 2;
            for (double t = 0.04; t < 1.0; t += solid ? 0.5 : 0.13) {
                double t2 = solid ? 1.0 : t + 0.07;
                double y1 = horizon + span * Math.pow(Math.max(0.02, solid ? 0.02 : t), 1.7);
                double y2 = horizon + span * Math.pow(Math.min(1.0, t2), 1.7);
                double x1 = vpx + lane * w * 0.20 * Math.pow(Math.max(0.02, solid ? 0.02 : t), 1.7);
                double x2 = vpx + lane * w * 0.20 * Math.pow(Math.min(1.0, t2), 1.7);
                double t1w = Math.max(0.4, 5 * Math.pow(solid ? 0.02 : t, 1.7) * (w / 1280.0));
                double t2w = Math.max(0.6, 5 * Math.pow(Math.min(1.0, t2), 1.7) * (w / 1280.0) * 1.4);
                g.setColor(solid ? NfsPalette.alpha(NfsPalette.AMBER, 0.55f) : NfsPalette.alpha(NfsPalette.TEXT, 0.28f));
                g.fill(poly(x1 - t1w, y1, x1 + t1w, y1, x2 + t2w, y2, x2 - t2w, y2));
            }
        }
        glow(g, vpx, horizon + 2, w * 0.45, h * 0.035, NfsPalette.alpha(NfsPalette.AMBER_HOT, 0.45f));
    }

    private static Path2D poly(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4) {
        Path2D p = new Path2D.Double();
        p.moveTo(x1, y1);
        p.lineTo(x2, y2);
        p.lineTo(x3, y3);
        p.lineTo(x4, y4);
        p.closePath();
        return p;
    }

    /** Big translucent angled bars, the NFS menu chrome. */
    private static void slashes(Graphics2D g, int w, int h) {
        AffineTransform keep = g.getTransform();
        g.shear(-0.45, 0);
        g.translate(h * 0.45, 0);
        g.setColor(NfsPalette.alpha(NfsPalette.AMBER, 0.07f));
        g.fillRect(-(int) (w * 0.02), 0, (int) (w * 0.05), h);
        g.setColor(NfsPalette.alpha(NfsPalette.RED, 0.07f));
        g.fillRect((int) (w * 0.04), 0, (int) (w * 0.012), h);
        g.setTransform(keep);
    }

    private static void scanlines(Graphics2D g, int w, int h) {
        g.setColor(new Color(255, 255, 255, 6));
        for (int y = 0; y < h; y += 3) g.drawLine(0, y, w, y);
    }

    private static void vignette(Graphics2D g, int w, int h) {
        double r = Math.hypot(w, h) * 0.62;
        g.setPaint(new RadialGradientPaint(new Point2D.Double(w * 0.55, h * 0.5), (float) r,
                new float[]{0.45f, 1f}, new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 215)}));
        g.fillRect(0, 0, w, h);
    }
}
