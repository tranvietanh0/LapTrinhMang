package racing.client.ui.race;

import racing.client.ui.theme.NfsPalette;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

/**
 * Segmented arc speedometer. The dim and lit rings are rendered once per size; each frame only a
 * pie-shaped clip (one reused Arc2D) reveals the lit ring up to the current speed. The clip edge
 * always lands in the gap between two segments, so it never shows a hard cut.
 */
final class RaceGauge {
    static final int SEGMENTS = 36;
    private static final double START = 225, SWEEP = 270, STEP = SWEEP / SEGMENTS, SEG = STEP * 0.76;

    private final int radius, pad, size;
    private final BufferedImage dim, lit;
    private final Arc2D.Double clip = new Arc2D.Double(Arc2D.PIE);

    RaceGauge(int radius, int thickness, Color[] ramp) {
        this.radius = radius;
        this.pad = Math.max(6, thickness);
        this.size = 2 * (radius + pad);
        this.dim = ring(thickness, null, ramp);
        this.lit = ring(thickness, ramp, ramp);
    }

    /** Full image edge in px (the gauge is drawn centred in it). */
    int size() {
        return size;
    }

    void paint(Graphics2D g, int cx, int cy, double ratio) {
        int x = cx - size / 2, y = cy - size / 2;
        g.drawImage(dim, x, y, null);
        int k = (int) Math.round(Math.max(0, Math.min(1, ratio)) * SEGMENTS);
        if (k <= 0) {
            return;
        }
        java.awt.Shape old = g.getClip();
        clip.setArc(x, y, size, size, START + 1, -(k * STEP - STEP * 0.12 + 1), Arc2D.PIE);
        g.clip(clip);
        g.drawImage(lit, x, y, null);
        g.setClip(old);
    }

    private BufferedImage ring(int thickness, Color[] litRamp, Color[] ramp) {
        BufferedImage img = RaceGfx.image(size, size);
        Graphics2D g = RaceGfx.gfx(img, 1);
        double c = size / 2.0, r = radius - thickness / 2.0;
        if (litRamp == null) {
            g.setColor(RaceGfx.a(NfsPalette.LINE, 0.55));
            g.setStroke(new BasicStroke(1.2f));
            g.draw(new Ellipse2D.Double(c - radius - 3, c - radius - 3, 2 * radius + 6, 2 * radius + 6));
            g.setColor(RaceGfx.a(NfsPalette.VOID, 0.55));
            g.fill(new Ellipse2D.Double(c - radius + thickness + 2, c - radius + thickness + 2,
                    2 * (radius - thickness - 2), 2 * (radius - thickness - 2)));
        }
        g.setStroke(new BasicStroke(thickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        for (int i = 0; i < SEGMENTS; i++) {
            float t = i / (float) (SEGMENTS - 1);
            Color seg;
            if (litRamp == null) {
                seg = RaceGfx.a(NfsPalette.LINE, 0.55);
            } else {
                float f = t * (ramp.length - 1);
                int a = Math.min(ramp.length - 2, (int) f);
                seg = NfsPalette.mix(ramp[a], ramp[a + 1], f - a);
            }
            g.setColor(seg);
            g.draw(new Arc2D.Double(c - r, c - r, 2 * r, 2 * r, START - STEP * i, -SEG, Arc2D.OPEN));
            if (litRamp != null) {   // hot core line inside each segment
                g.setColor(RaceGfx.a(Color.WHITE, 0.30));
                g.setStroke(new BasicStroke(Math.max(1f, thickness * 0.22f), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
                g.draw(new Arc2D.Double(c - r - thickness * 0.22, c - r - thickness * 0.22,
                        2 * (r + thickness * 0.22), 2 * (r + thickness * 0.22), START - STEP * i, -SEG, Arc2D.OPEN));
                g.setStroke(new BasicStroke(thickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
            }
        }
        g.dispose();
        return img;
    }
}
