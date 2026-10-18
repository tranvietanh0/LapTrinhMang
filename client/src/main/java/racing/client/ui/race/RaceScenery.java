package racing.client.ui.race;

import racing.client.ui.theme.Chamfer;
import racing.client.ui.theme.NfsFonts;
import racing.client.ui.theme.NfsPalette;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Night-street scenery builders: road tile (kerbs, lane dashes, wear), pavement tile, rooftops,
 * trees, lamps and the START / FINISH gantry. Everything is drawn once per scale into images.
 * Logical units: one tile is 28 m = {@link #TILE} logical px tall; the road is 252 px wide.
 */
final class RaceScenery {
    static final int TILE = 84;
    static final double ROAD = 252;
    static final double KERB = 10.5;
    /** Logical width of road plus both kerbs. */
    static final double STRIP = ROAD + 2 * KERB;

    static final Color ASPHALT = new Color(0x161A21);
    static final Color KERB_RED = new Color(0xC21F2D);
    static final Color KERB_WHITE = new Color(0xDAD6CB);
    static final Color LAMP = new Color(0xFFD27A);
    static final Color[] NEON = {NfsPalette.AMBER, NfsPalette.RED, new Color(0x35B6FF), NfsPalette.AMBER_HOT, new Color(0xFF5A8A)};

    private RaceScenery() { }

    /** Seamless-in-Y road with kerbs, edge lines, dashes and tyre wear. Size: STRIP*s by TILE*s. */
    static BufferedImage roadTile(double s) {
        BufferedImage img = RaceGfx.image((int) Math.round(STRIP * s), (int) Math.round(TILE * s));
        Graphics2D g = RaceGfx.gfx(img, s);
        double x0 = KERB, x1 = KERB + ROAD;
        g.setPaint(new LinearGradientPaint((float) x0, 0, (float) x1, 0, new float[]{0f, 0.12f, 0.5f, 0.88f, 1f},
                new Color[]{RaceGfx.shade(ASPHALT, 0.7), ASPHALT, RaceGfx.shade(ASPHALT, 1.22), ASPHALT, RaceGfx.shade(ASPHALT, 0.7)}));
        g.fill(new java.awt.geom.Rectangle2D.Double(x0, 0, ROAD, TILE));
        Random rnd = new Random(29);
        for (int i = 0; i < 2600; i++) {
            g.setColor(rnd.nextBoolean() ? new Color(255, 255, 255, 9 + rnd.nextInt(10)) : new Color(0, 0, 0, 40 + rnd.nextInt(40)));
            g.fill(new java.awt.geom.Rectangle2D.Double(x0 + rnd.nextDouble() * ROAD, rnd.nextDouble() * (TILE - 1), 1 + rnd.nextInt(2), 1));
        }
        g.setColor(new Color(0, 0, 0, 40));
        for (int lane = 0; lane < 3; lane++) {
            double cx = x0 + lane * 84 + 42;
            g.fill(new RoundRectangle2D.Double(cx - 18, 6, 5, 44, 5, 5));
            g.fill(new RoundRectangle2D.Double(cx + 13, 22, 5, 52, 5, 5));
        }
        // lane dashes: 7 m on, 7 m off
        for (int i = 1; i < 3; i++) {
            double x = x0 + i * 84;
            for (double y : new double[]{10, 52}) {
                g.setColor(RaceGfx.a(KERB_WHITE, 0.10));
                g.fill(new RoundRectangle2D.Double(x - 3.5, y - 1.5, 7, 24, 3, 3));
                g.setColor(RaceGfx.a(KERB_WHITE, 0.88));
                g.fill(new RoundRectangle2D.Double(x - 1.6, y, 3.2, 21, 1.4, 1.4));
            }
        }
        // amber edge lines
        for (double x : new double[]{x0 + 4, x1 - 6.5}) {
            g.setColor(RaceGfx.a(NfsPalette.AMBER, 0.14));
            g.fill(new java.awt.geom.Rectangle2D.Double(x - 1.5, 0, 5.5, TILE));
            g.setColor(RaceGfx.a(NfsPalette.AMBER_HOT, 0.92));
            g.fill(new java.awt.geom.Rectangle2D.Double(x, 0, 2.5, TILE));
        }
        // kerbs
        for (double kx : new double[]{0, x1}) {
            for (int seg = 0; seg < 4; seg++) {
                g.setPaint(new GradientPaint((float) kx, 0, seg % 2 == 0 ? KERB_RED : KERB_WHITE,
                        (float) (kx + KERB), 0, RaceGfx.shade(seg % 2 == 0 ? KERB_RED : KERB_WHITE, 0.62)));
                g.fill(new java.awt.geom.Rectangle2D.Double(kx, seg * 21, KERB, 21));
            }
            g.setColor(RaceGfx.a(Color.BLACK, 0.55));
            g.fill(new java.awt.geom.Rectangle2D.Double(kx == 0 ? 0 : kx + KERB - 1.2, 0, 1.2, TILE));
        }
        g.dispose();
        return img;
    }

    /** Dark pavement slabs, 3 lanes wide, seamless in both axes. Size: ROAD*s by TILE*s. */
    static BufferedImage groundTile(double s) {
        BufferedImage img = RaceGfx.image((int) Math.round(ROAD * s), (int) Math.round(TILE * s));
        Graphics2D g = RaceGfx.gfx(img, s);
        g.setColor(new Color(0x0D1016));
        g.fill(new java.awt.geom.Rectangle2D.Double(0, 0, ROAD, TILE));
        Random rnd = new Random(41);
        for (int i = 0; i < 1500; i++) {
            g.setColor(rnd.nextBoolean() ? new Color(255, 255, 255, 7) : new Color(0, 0, 0, 55));
            g.fill(new java.awt.geom.Rectangle2D.Double(rnd.nextDouble() * (ROAD - 2), rnd.nextDouble() * (TILE - 1), 1 + rnd.nextInt(3), 1));
        }
        g.setStroke(new BasicStroke(1f));
        g.setColor(new Color(0, 0, 0, 120));
        for (double x = 0; x < ROAD; x += 42) g.draw(new java.awt.geom.Line2D.Double(x + 0.5, 0, x + 0.5, TILE));
        for (double y = 0; y < TILE; y += 42) g.draw(new java.awt.geom.Line2D.Double(0, y + 0.5, ROAD, y + 0.5));
        g.setColor(new Color(255, 255, 255, 8));
        for (double x = 0; x < ROAD; x += 42) g.draw(new java.awt.geom.Line2D.Double(x + 1.5, 0, x + 1.5, TILE));
        g.dispose();
        return img;
    }

    private static final int[][] ROOF_SIZES = {{84, 70}, {104, 80}, {66, 76}, {120, 64}, {76, 52}, {92, 82}};
    static final int ROOF_VARIANTS = ROOF_SIZES.length;
    static final int ROOF_PAD = 16;

    /** Rooftop with a neon edge on the side that faces the road; sprite includes ROOF_PAD of spill glow. */
    static BufferedImage roof(double s, int variant, boolean neonOnRight, double depthDim) {
        int bw = ROOF_SIZES[variant][0], bh = ROOF_SIZES[variant][1];
        int cw = bw + 2 * ROOF_PAD, ch = bh + 2 * ROOF_PAD;
        BufferedImage img = RaceGfx.image((int) Math.ceil(cw * s), (int) Math.ceil(ch * s));
        Graphics2D g = RaceGfx.gfx(img, s);
        if (!neonOnRight) {
            g.translate(cw, 0);
            g.scale(-1, 1);
        }
        Color neon = NEON[variant % NEON.length];
        Random rnd = new Random(variant * 977L + 5);
        // spill glow on the ground, toward the road
        g.setPaint(new LinearGradientPaint(ROOF_PAD + bw, 0, cw, 0, new float[]{0f, 1f},
                new Color[]{RaceGfx.a(neon, 0.28 * depthDim), RaceGfx.a(neon, 0)}));
        g.fill(new java.awt.geom.Rectangle2D.Double(ROOF_PAD + bw, ROOF_PAD + 4, ROOF_PAD, bh - 8));
        g.translate(ROOF_PAD, ROOF_PAD);
        g.setColor(new Color(0, 0, 0, 150));
        g.fill(Chamfer.shape(3, 4, bw, bh, 9, 0, 9, 0));
        var body = Chamfer.shape(0, 0, bw, bh, 9, 0, 9, 0);
        g.setPaint(new GradientPaint(0, 0, RaceGfx.shade(new Color(0x232B38), depthDim), 0, bh, RaceGfx.shade(new Color(0x10151D), depthDim)));
        g.fill(body);
        g.setColor(RaceGfx.shade(new Color(0x3A4558), depthDim));
        g.setStroke(new BasicStroke(1.4f));
        g.draw(body);
        g.setColor(RaceGfx.shade(new Color(0x181E29), depthDim));
        g.fill(new java.awt.geom.Rectangle2D.Double(6, 6, bw - 12, bh - 12));
        for (int i = 0; i < 4 + rnd.nextInt(3); i++) {
            double w = 8 + rnd.nextInt(14), h = 7 + rnd.nextInt(12);
            double x = 9 + rnd.nextDouble() * (bw - 18 - w), y = 9 + rnd.nextDouble() * (bh - 18 - h);
            g.setColor(RaceGfx.shade(new Color(0x2C3646), depthDim));
            g.fill(new RoundRectangle2D.Double(x, y, w, h, 2, 2));
            g.setColor(RaceGfx.a(Color.WHITE, 0.07));
            g.fill(new java.awt.geom.Rectangle2D.Double(x, y, w, 1.2));
        }
        // neon edge strip
        g.setColor(RaceGfx.a(neon, 0.25 * depthDim));
        g.fill(new java.awt.geom.Rectangle2D.Double(bw - 6, 10, 6, bh - 20));
        g.setColor(RaceGfx.a(neon, 0.95 * depthDim));
        g.fill(new java.awt.geom.Rectangle2D.Double(bw - 3, 11, 2.4, bh - 22));
        g.dispose();
        return img;
    }

    static final int TREE_PAD = 8;

    static BufferedImage tree(double s, int variant) {
        double r = variant == 0 ? 17 : 23;
        double size = 2 * (r + TREE_PAD);
        BufferedImage img = RaceGfx.image((int) Math.ceil(size * s), (int) Math.ceil(size * s));
        Graphics2D g = RaceGfx.gfx(img, s);
        double c = size / 2;
        g.setColor(new Color(0, 0, 0, 110));
        g.fill(new Ellipse2D.Double(c - r + 3, c - r + 4, 2 * r, 2 * r));
        for (int i = 0; i < 4; i++) {
            double ox = (i % 2 == 0 ? -1 : 1) * r * 0.28 * (i / 2 + 0.6), oy = (i < 2 ? -1 : 1) * r * 0.24;
            double rr = r * (0.72 + 0.06 * i % 3);
            g.setPaint(new RadialGradientPaint((float) (c + ox - rr * 0.25), (float) (c + oy - rr * 0.3), (float) rr,
                    new float[]{0f, 1f}, new Color[]{new Color(0x2E5C46), new Color(0x0E241C)}));
            g.fill(new Ellipse2D.Double(c + ox - rr, c + oy - rr, 2 * rr, 2 * rr));
        }
        g.dispose();
        return img;
    }

    /** Street-lamp head (pole top and arm reaching the road) with a hot glow, drawn for a lamp on the left. */
    static BufferedImage lamp(double s, boolean armToRight) {
        int w = 64, h = 64;
        BufferedImage img = RaceGfx.image((int) Math.ceil(w * s), (int) Math.ceil(h * s));
        Graphics2D g = RaceGfx.gfx(img, s);
        if (!armToRight) {
            g.translate(w, 0);
            g.scale(-1, 1);
        }
        g.setPaint(new RadialGradientPaint(44, 32, 30, new float[]{0f, 0.3f, 1f},
                new Color[]{RaceGfx.a(LAMP, 0.85), RaceGfx.a(LAMP, 0.25), RaceGfx.a(LAMP, 0)}));
        g.fill(new Ellipse2D.Double(14, 2, 60, 60));
        g.setColor(new Color(0x0A0C10));
        g.fill(new Ellipse2D.Double(10, 28, 8, 8));
        g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new java.awt.geom.Line2D.Double(14, 32, 40, 32));
        g.setColor(new Color(0xFFF3D6));
        g.fill(new RoundRectangle2D.Double(38, 28.5, 9, 7, 3, 3));
        g.dispose();
        return img;
    }

    /** Warm light pool a lamp throws over the road (centred; drawn over the strip). */
    static BufferedImage lampPool(double s) {
        double w = STRIP + 40, h = 86;
        BufferedImage img = RaceGfx.image((int) Math.ceil(w * s), (int) Math.ceil(h * s));
        Graphics2D g = RaceGfx.gfx(img, s);
        g.translate(w / 2, h / 2);
        g.scale(1, h / w);
        g.setPaint(new RadialGradientPaint(0, 0, (float) (w / 2), new float[]{0f, 0.55f, 1f},
                new Color[]{RaceGfx.a(LAMP, 0.20), RaceGfx.a(LAMP, 0.08), RaceGfx.a(LAMP, 0)}));
        g.fill(new Ellipse2D.Double(-w / 2, -w / 2, w, w));
        g.dispose();
        return img;
    }

    /** Vertical speed streak, faded at both ends (logical 3 x 128). */
    static BufferedImage streak(double s) {
        BufferedImage img = RaceGfx.image((int) Math.max(2, Math.round(3 * s)), (int) Math.ceil(128 * s));
        Graphics2D g = RaceGfx.gfx(img, s);
        g.setPaint(new LinearGradientPaint(0, 0, 0, 128, new float[]{0f, 0.35f, 1f},
                new Color[]{RaceGfx.a(Color.WHITE, 0), RaceGfx.a(new Color(0xFFE2A8), 0.8), RaceGfx.a(Color.WHITE, 0)}));
        g.fill(new java.awt.geom.Rectangle2D.Double(0, 0, 3, 128));
        g.dispose();
        return img;
    }

    /** Height of the gantry sprite in logical px; the checker band's centre is {@link #BANNER_LINE_Y} from the top. */
    static final int BANNER_H = 92;
    static final double BANNER_LINE_Y = 30;
    static final double BANNER_W = STRIP + 36;

    /** START / FINISH marking: checker (or solid) band across the road, glowing pylons and a name plate behind it. */
    static BufferedImage banner(double s, String label, boolean checker) {
        BufferedImage img = RaceGfx.image((int) Math.ceil(BANNER_W * s), (int) Math.ceil(BANNER_H * s));
        Graphics2D g = RaceGfx.gfx(img, s);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        double x0 = 18 + KERB, rows = checker ? 3 : 1, cell = ROAD / 20;
        double bandTop = BANNER_LINE_Y - rows * cell / 2;
        g.setPaint(new LinearGradientPaint(0, (float) bandTop - 14, 0, (float) bandTop,
                new float[]{0f, 1f}, new Color[]{RaceGfx.a(NfsPalette.AMBER, 0), RaceGfx.a(NfsPalette.AMBER, 0.28)}));
        g.fill(new java.awt.geom.Rectangle2D.Double(x0, bandTop - 14, ROAD, 14));
        if (checker) {
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 20; c++) {
                    g.setColor((r + c) % 2 == 0 ? new Color(0xF2EFE6) : new Color(0x0A0C10));
                    g.fill(new java.awt.geom.Rectangle2D.Double(x0 + c * cell, bandTop + r * cell, cell, cell));
                }
            }
        } else {
            g.setPaint(new LinearGradientPaint((float) x0, 0, (float) (x0 + ROAD), 0, new float[]{0f, 0.5f, 1f},
                    new Color[]{NfsPalette.RED, new Color(0xF2EFE6), NfsPalette.RED}));
            g.fill(new java.awt.geom.Rectangle2D.Double(x0, bandTop, ROAD, cell));
        }
        for (double px : new double[]{2, 18 + STRIP - 2}) {
            g.setColor(new Color(0x0A0C10));
            g.fill(new RoundRectangle2D.Double(px, bandTop - 6, 14, rows * cell + 12, 4, 4));
            g.setColor(NfsPalette.AMBER_HOT);
            g.fill(new java.awt.geom.Rectangle2D.Double(px + 5, bandTop - 3, 4, rows * cell + 6));
            g.setPaint(new RadialGradientPaint((float) px + 7, (float) BANNER_LINE_Y, 26f, new float[]{0f, 1f},
                    new Color[]{RaceGfx.a(NfsPalette.AMBER, 0.45), RaceGfx.a(NfsPalette.AMBER, 0)}));
            g.fill(new Ellipse2D.Double(px - 19, BANNER_LINE_Y - 26, 52, 52));
        }
        // name plate on the road behind the line
        double pw = 150, ph = 30, px = 18 + STRIP / 2 - pw / 2, py = bandTop + rows * cell + 12;
        var plate = Chamfer.shape((float) px, (float) py, (float) pw, (float) ph, 9, 0, 9, 0);
        g.setColor(RaceGfx.a(new Color(0x05070B), 0.82));
        g.fill(plate);
        g.setStroke(new BasicStroke(1.6f));
        g.setColor(NfsPalette.AMBER);
        g.draw(plate);
        g.setFont(NfsFonts.tracked(NfsFonts.hero(23f), 0.08f));
        FontMetrics fm = g.getFontMetrics();
        String text = NfsFonts.safe(label);
        g.setColor(checker ? NfsPalette.TEXT : NfsPalette.AMBER_HOT);
        g.drawString(text, (float) (px + (pw - fm.stringWidth(text)) / 2), (float) (py + (ph + fm.getAscent() - fm.getDescent()) / 2));
        g.dispose();
        return img;
    }
}
