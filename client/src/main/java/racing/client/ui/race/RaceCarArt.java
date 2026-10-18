package racing.client.ui.race;

import racing.client.ui.theme.NfsPalette;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * Top-down vector cars for the night street: racers (own = hot red/amber, rival = ice blue) with
 * headlight and tail-light glow, plus muted traffic (sedans, truck, van). Built once per scale.
 */
final class RaceCarArt {
    /** Car body box in logical px (matches PPM: 20 m == 60 px long). */
    static final int BODY_W = 34, BODY_H = 60;
    /** Transparent padding around the body that holds glows. */
    static final int PAD_X = 28, PAD_Y = 24;
    static final int CANVAS_W = BODY_W + 2 * PAD_X, CANVAS_H = BODY_H + 2 * PAD_Y;

    static final Color OWN_BODY = new Color(0xD81F2B);
    static final Color RIVAL_BODY = new Color(0x2C9BF0);
    private static final Color GLASS_TOP = new Color(0x1B2B40), GLASS_BOT = new Color(0x05080D);
    private static final Color HEAD = new Color(0xFFF1C4), TAIL = new Color(0xFF2A33);

    private RaceCarArt() { }

    enum Shape { SEDAN, TRUCK, VAN }

    record Style(Shape shape, Color body, Color accent, Color underglow, boolean racer) { }

    static final Style OWN = new Style(Shape.SEDAN, OWN_BODY, NfsPalette.AMBER, NfsPalette.AMBER, true);
    static final Style RIVAL = new Style(Shape.SEDAN, RIVAL_BODY, Color.WHITE, new Color(0x35B6FF), true);
    static final Style[] TRAFFIC = {
            new Style(Shape.SEDAN, new Color(0x1C8C9E), new Color(0x0F4E59), null, false),
            new Style(Shape.SEDAN, new Color(0xC8A22A), new Color(0x6A5612), null, false),
            new Style(Shape.SEDAN, new Color(0x962A2E), new Color(0x4C1417), null, false),
            new Style(Shape.SEDAN, new Color(0xD2D6DC), new Color(0x68707C), null, false),
            new Style(Shape.TRUCK, new Color(0xC9781C), new Color(0xB9BFC9), null, false),
            new Style(Shape.VAN, new Color(0x3A5FCD), new Color(0x8FA8F0), null, false),
    };

    static BufferedImage car(double s, Style st) {
        BufferedImage img = RaceGfx.image((int) Math.ceil(CANVAS_W * s), (int) Math.ceil(CANVAS_H * s));
        Graphics2D g = RaceGfx.gfx(img, s);
        g.translate(PAD_X, PAD_Y);
        if (st.underglow() != null) underglow(g, st.underglow());
        g.setColor(new Color(0, 0, 0, 120));
        g.fill(new Ellipse2D.Double(-1, 3, BODY_W + 2, BODY_H + 2));
        wheels(g);
        switch (st.shape()) {
            case SEDAN -> sedan(g, st);
            case TRUCK -> truck(g, st);
            case VAN -> van(g, st);
        }
        lights(g, st);
        g.dispose();
        return img;
    }

    private static void underglow(Graphics2D g, Color c) {
        g.setPaint(new RadialGradientPaint(17, 31, 38, new float[]{0f, 0.55f, 1f},
                new Color[]{RaceGfx.a(c, 0.0), RaceGfx.a(c, 0.42), RaceGfx.a(c, 0.0)}));
        g.fill(new Ellipse2D.Double(-24, -14, 82, 88));
    }

    private static void wheels(Graphics2D g) {
        g.setColor(new Color(0x07080A));
        for (double y : new double[]{9, 41}) {
            g.fill(new RoundRectangle2D.Double(-1.6, y, 4.4, 11, 2, 2));
            g.fill(new RoundRectangle2D.Double(31.2, y, 4.4, 11, 2, 2));
        }
    }

    private static LinearGradientPaint tube(Color body) {
        return new LinearGradientPaint(0, 0, BODY_W, 0, new float[]{0f, 0.28f, 0.5f, 0.72f, 1f},
                new Color[]{RaceGfx.shade(body, 0.42), body, RaceGfx.shade(body, 1.28), body, RaceGfx.shade(body, 0.42)});
    }

    private static void sedan(Graphics2D g, Style st) {
        Path2D.Double b = new Path2D.Double();
        b.moveTo(9, 1);
        b.quadTo(17, -1.5, 25, 1);
        b.curveTo(31, 5, 33, 13, 33, 24);
        b.lineTo(33, 45);
        b.curveTo(33, 52, 31, 57, 27, 59);
        b.quadTo(17, 61.5, 7, 59);
        b.curveTo(3, 57, 1, 52, 1, 45);
        b.lineTo(1, 24);
        b.curveTo(1, 13, 3, 5, 9, 1);
        b.closePath();
        g.setPaint(tube(st.body()));
        g.fill(b);
        g.setColor(RaceGfx.a(Color.BLACK, 0.55));
        g.setStroke(new BasicStroke(0.8f));
        g.draw(b);
        Path2D.Double ws = new Path2D.Double();
        ws.moveTo(7.5, 27); ws.lineTo(11, 17); ws.lineTo(23, 17); ws.lineTo(26.5, 27); ws.closePath();
        g.setPaint(new GradientPaint(0, 17, GLASS_TOP, 0, 27, GLASS_BOT));
        g.fill(ws);
        Path2D.Double rw = new Path2D.Double();
        rw.moveTo(8.5, 40); rw.lineTo(25.5, 40); rw.lineTo(23, 47); rw.lineTo(11, 47); rw.closePath();
        g.setPaint(new GradientPaint(0, 40, GLASS_BOT, 0, 47, GLASS_TOP));
        g.fill(rw);
        Color roof = st.racer() ? new Color(0x14161C) : st.body();
        g.setPaint(new LinearGradientPaint(0, 0, BODY_W, 0, new float[]{0f, 0.5f, 1f},
                new Color[]{RaceGfx.shade(roof, 0.85), st.racer() ? new Color(0x2A2E38) : RaceGfx.shade(roof, 1.15),
                        RaceGfx.shade(roof, 0.85)}));
        g.fill(new RoundRectangle2D.Double(8, 27, 18, 13, 3, 3));
        g.setColor(RaceGfx.a(Color.WHITE, 0.22));
        g.setStroke(new BasicStroke(0.9f));
        g.drawLine(12, 18, 17, 18);
        if (st.racer()) {
            g.setColor(st.accent());
            g.fill(new RoundRectangle2D.Double(14.2, 1.5, 1.7, 15.5, 1, 1));
            g.fill(new RoundRectangle2D.Double(18.1, 1.5, 1.7, 15.5, 1, 1));
            g.fill(new RoundRectangle2D.Double(14.2, 47.5, 1.7, 9.5, 1, 1));
            g.fill(new RoundRectangle2D.Double(18.1, 47.5, 1.7, 9.5, 1, 1));
            g.setColor(new Color(0x0A0C10));
            g.fill(new RoundRectangle2D.Double(2.5, 51.5, 29, 5.2, 1.8, 1.8));
            g.setColor(RaceGfx.a(st.accent(), 0.9));
            g.fillRect(3, 51, 28, 1);
            g.setColor(new Color(0x0A0C10));
            g.fill(new RoundRectangle2D.Double(-1.6, 25, 3.2, 4.5, 1, 1));
            g.fill(new RoundRectangle2D.Double(32.4, 25, 3.2, 4.5, 1, 1));
        } else {
            g.setColor(RaceGfx.a(st.accent(), 0.8));
            g.fill(new RoundRectangle2D.Double(4, 48, 26, 3, 1, 1));
        }
    }

    private static void truck(Graphics2D g, Style st) {
        g.setPaint(tube(st.body()));
        g.fill(new RoundRectangle2D.Double(3, 0.5, 28, 17, 7, 7));
        g.setPaint(new GradientPaint(0, 4, GLASS_TOP, 0, 12, GLASS_BOT));
        g.fill(new RoundRectangle2D.Double(7, 4, 20, 7, 3, 3));
        g.setPaint(tube(st.accent()));
        g.fill(new RoundRectangle2D.Double(0.5, 19, 33, 40.5, 3, 3));
        g.setColor(RaceGfx.a(Color.BLACK, 0.28));
        g.setStroke(new BasicStroke(0.8f));
        for (int y = 27; y < 58; y += 8) g.drawLine(2, y, 32, y);
        g.setColor(RaceGfx.a(Color.BLACK, 0.5));
        g.draw(new RoundRectangle2D.Double(0.5, 19, 33, 40.5, 3, 3));
    }

    private static void van(Graphics2D g, Style st) {
        Path2D.Double b = new Path2D.Double();
        b.moveTo(6, 1); b.quadTo(17, -1, 28, 1);
        b.curveTo(32, 3, 33, 8, 33, 14); b.lineTo(33, 54);
        b.curveTo(33, 58, 30, 60, 26, 60); b.lineTo(8, 60);
        b.curveTo(4, 60, 1, 58, 1, 54); b.lineTo(1, 14);
        b.curveTo(1, 8, 2, 3, 6, 1); b.closePath();
        g.setPaint(tube(st.body()));
        g.fill(b);
        g.setPaint(new GradientPaint(0, 5, GLASS_TOP, 0, 15, GLASS_BOT));
        Path2D.Double ws = new Path2D.Double();
        ws.moveTo(6, 15); ws.lineTo(9, 5); ws.lineTo(25, 5); ws.lineTo(28, 15); ws.closePath();
        g.fill(ws);
        g.setPaint(tube(st.accent()));
        g.fill(new RoundRectangle2D.Double(5, 18, 24, 38, 3, 3));
        g.setColor(RaceGfx.a(Color.BLACK, 0.25));
        g.setStroke(new BasicStroke(0.8f));
        for (int y = 26; y < 56; y += 8) g.drawLine(6, y, 28, y);
    }

    private static void lights(Graphics2D g, Style st) {
        double hx1 = st.shape() == Shape.SEDAN ? 6 : 4.5;
        double hx2 = st.shape() == Shape.SEDAN ? 22 : 24.5;
        for (double hx : new double[]{hx1, hx2}) {
            g.setPaint(new RadialGradientPaint((float) (hx + 3), 3f, 9f, new float[]{0f, 1f},
                    new Color[]{RaceGfx.a(HEAD, st.racer() ? 0.8 : 0.3), RaceGfx.a(HEAD, 0)}));
            g.fill(new Ellipse2D.Double(hx - 6, -6, 18, 18));
            g.setColor(HEAD);
            g.fill(new RoundRectangle2D.Double(hx, 1.2, 6, 2.6, 1.5, 1.5));
        }
        for (double tx : new double[]{4.5, 20.5}) {
            g.setPaint(new RadialGradientPaint((float) (tx + 4.5), 59f, 13f, new float[]{0f, 0.45f, 1f},
                    new Color[]{RaceGfx.a(TAIL, st.racer() ? 0.85 : 0.6), RaceGfx.a(TAIL, 0.25), RaceGfx.a(TAIL, 0)}));
            g.fill(new Ellipse2D.Double(tx - 8.5, 46, 26, 26));
            g.setColor(new Color(0xFF4A52));
            g.fill(new RoundRectangle2D.Double(tx, 57.2, 9, 2.4, 1.2, 1.2));
        }
    }

    /** Twin headlight cone that lights the road ahead of a racer; its bottom edge sits at the nose. */
    static BufferedImage beam(double s) {
        int w = 96, h = 170;
        BufferedImage img = RaceGfx.image((int) Math.ceil(w * s), (int) Math.ceil(h * s));
        Graphics2D g = RaceGfx.gfx(img, s);
        Path2D.Double cone = new Path2D.Double();
        cone.moveTo(48 - 15, h); cone.lineTo(48 + 15, h); cone.lineTo(w, 0); cone.lineTo(0, 0); cone.closePath();
        g.setPaint(new LinearGradientPaint(0, h, 0, 0, new float[]{0f, 0.55f, 1f},
                new Color[]{RaceGfx.a(NfsPalette.AMBER_HOT, 0.34), RaceGfx.a(NfsPalette.AMBER_HOT, 0.10),
                        RaceGfx.a(NfsPalette.AMBER_HOT, 0)}));
        g.fill(cone);
        g.dispose();
        return img;
    }

    /** Square sprite centred on the impact: small spark burst or full fireball. */
    static BufferedImage explosion(double s, boolean big) {
        double size = big ? 150 : 96;
        int px = (int) Math.ceil(size * s);
        BufferedImage img = RaceGfx.image(px, px);
        Graphics2D g = RaceGfx.gfx(img, s);
        double c = size / 2;
        g.setPaint(new RadialGradientPaint((float) c, (float) c, (float) c, new float[]{0f, 0.2f, 0.5f, 1f}, new Color[]{
                RaceGfx.a(Color.WHITE, 0.98), RaceGfx.a(NfsPalette.AMBER_HOT, 0.92),
                RaceGfx.a(NfsPalette.RED, 0.62), RaceGfx.a(NfsPalette.RED, 0)}));
        g.fill(new Ellipse2D.Double(0, 0, size, size));
        g.setStroke(new BasicStroke(big ? 5.5f : 3.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(RaceGfx.a(NfsPalette.AMBER_HOT, 0.95));
        for (int i = 0; i < 10; i++) {
            double ang = i * Math.PI * 2 / 10 + (i % 2) * 0.18;
            double r0 = size * 0.17, r1 = size * (i % 2 == 0 ? 0.43 : 0.31);
            g.drawLine((int) (c + Math.cos(ang) * r0), (int) (c + Math.sin(ang) * r0),
                    (int) (c + Math.cos(ang) * r1), (int) (c + Math.sin(ang) * r1));
        }
        g.setStroke(new BasicStroke(big ? 3f : 2f));
        g.setColor(RaceGfx.a(Color.WHITE, 0.7));
        g.draw(new Ellipse2D.Double(c - size * 0.28, c - size * 0.28, size * 0.56, size * 0.56));
        g.dispose();
        return img;
    }
}
