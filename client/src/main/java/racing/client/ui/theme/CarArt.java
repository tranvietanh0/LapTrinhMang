package racing.client.ui.theme;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

/** Vector street-racer silhouette (side view, facing right) drawn into a 1000x300 design box. */
public final class CarArt {
    public static final double BOX_W = 1000, BOX_H = 300;
    private static final int[] BODY_X = {64, 52, 118, 300, 392, 478, 588, 700, 808, 936, 982, 974, 64};
    private static final int[] BODY_Y = {236, 198, 168, 150, 90, 62, 58, 108, 140, 164, 192, 232, 236};

    private CarArt() { }

    /** Draws the car with its left edge at x, top at y, scaled so the box is `width` pixels wide. */
    public static void draw(Graphics2D g0, double x, double y, double width) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.translate(x, y);
        double s = width / BOX_W;
        g.scale(s, s);
        underglow(g);
        headlightBeam(g);
        Path2D.Float body = new Path2D.Float();
        body.moveTo(BODY_X[0], BODY_Y[0]);
        for (int i = 1; i < BODY_X.length; i++) body.lineTo(BODY_X[i], BODY_Y[i]);
        body.closePath();
        g.setPaint(new GradientPaint(0, 58, new Color(0x2A3240), 0, 236, new Color(0x07090D)));
        g.fill(body);
        window(g);
        trim(g, body);
        wheel(g, 244, 232);
        wheel(g, 790, 232);
        lights(g);
        g.dispose();
    }

    private static void underglow(Graphics2D g) {
        g.setPaint(new RadialGradientPaint(520, 262, 470, new float[]{0f, 1f},
                new Color[]{NfsPalette.alpha(NfsPalette.AMBER, 0.38f), NfsPalette.alpha(NfsPalette.AMBER, 0f)}));
        AffineTransform keep = g.getTransform();
        g.translate(0, 262);
        g.scale(1, 0.12);
        g.translate(0, -262);
        g.fill(new Ellipse2D.Double(50, -208, 940, 940));
        g.setTransform(keep);
        g.setColor(new Color(0, 0, 0, 150));
        g.fill(new Ellipse2D.Double(70, 270, 880, 22));
    }

    private static void headlightBeam(Graphics2D g) {
        Path2D.Float beam = new Path2D.Float();
        beam.moveTo(972, 176);
        beam.lineTo(1500, 70);
        beam.lineTo(1500, 270);
        beam.lineTo(972, 206);
        beam.closePath();
        g.setPaint(new LinearGradientPaint(972, 0, 1500, 0, new float[]{0f, 1f},
                new Color[]{NfsPalette.alpha(NfsPalette.AMBER_HOT, 0.42f), NfsPalette.alpha(NfsPalette.AMBER_HOT, 0f)}));
        g.fill(beam);
    }

    private static void window(Graphics2D g) {
        Path2D.Float w = new Path2D.Float();
        w.moveTo(330, 146);
        w.lineTo(402, 98);
        w.lineTo(480, 76);
        w.lineTo(580, 74);
        w.lineTo(676, 116);
        w.lineTo(740, 142);
        w.closePath();
        g.setPaint(new GradientPaint(0, 74, new Color(0x41607F), 0, 146, new Color(0x0B1220)));
        g.fill(w);
        g.setColor(new Color(0x05070B));
        g.fillRect(498, 76, 7, 68);
    }

    private static void trim(Graphics2D g, Path2D.Float body) {
        // amber rim light on the roof and hood, red belt-line stripe, dark skirt
        g.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D.Float rim = new Path2D.Float();
        rim.moveTo(BODY_X[3], BODY_Y[3]);
        for (int i = 4; i <= 10; i++) rim.lineTo(BODY_X[i], BODY_Y[i]);
        g.setColor(NfsPalette.alpha(NfsPalette.AMBER_HOT, 0.95f));
        g.draw(rim);
        g.setColor(NfsPalette.RED);
        g.setStroke(new BasicStroke(5f));
        g.drawLine(120, 182, 936, 182);
        g.setColor(new Color(0x030406));
        g.fillRect(150, 214, 700, 12);
        g.setColor(NfsPalette.alpha(Color.WHITE, 0.12f));
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(330, 160, 740, 160);
        // rear wing
        g.setColor(new Color(0x0B0E14));
        g.fillRect(44, 150, 96, 8);
        g.fillRect(70, 158, 8, 16);
        g.setColor(NfsPalette.alpha(NfsPalette.AMBER, 0.8f));
        g.fillRect(44, 150, 96, 2);
    }

    private static void wheel(Graphics2D g, double cx, double cy) {
        g.setColor(new Color(0x020304));
        g.fill(new Ellipse2D.Double(cx - 64, cy - 64, 128, 128)); // arch
        g.setColor(new Color(0x0A0C10));
        g.fill(new Ellipse2D.Double(cx - 56, cy - 56, 112, 112)); // tyre
        g.setPaint(new GradientPaint((float) cx, (float) cy - 40, new Color(0xE3E8EF), (float) cx, (float) cy + 40, new Color(0x4A5361)));
        g.fill(new Ellipse2D.Double(cx - 40, cy - 40, 80, 80)); // rim
        g.setColor(new Color(0x0B0E14));
        g.setStroke(new BasicStroke(7f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        for (int i = 0; i < 6; i++) {
            double a = Math.toRadians(i * 60 + 15);
            g.drawLine(Math.round((float) cx), Math.round((float) cy), Math.round((float) (cx + 38 * Math.cos(a))), Math.round((float) (cy + 38 * Math.sin(a))));
        }
        g.setColor(NfsPalette.AMBER);
        g.fill(new Ellipse2D.Double(cx - 9, cy - 9, 18, 18));
        g.setColor(NfsPalette.alpha(NfsPalette.RED, 0.9f));
        g.setStroke(new BasicStroke(2.5f));
        g.draw(new Ellipse2D.Double(cx - 44, cy - 44, 88, 88)); // brake caliper ring
    }

    private static void lights(Graphics2D g) {
        // headlight
        g.setPaint(new RadialGradientPaint(962, 184, 40, new float[]{0f, 1f},
                new Color[]{new Color(255, 245, 210, 255), new Color(255, 208, 74, 0)}));
        g.fill(new Ellipse2D.Double(922, 144, 80, 80));
        g.setColor(new Color(0xFFF4D0));
        g.fillPolygon(new int[]{930, 976, 978, 936}, new int[]{172, 178, 190, 188}, 4);
        // tail light bar + glow
        g.setPaint(new RadialGradientPaint(70, 190, 60, new float[]{0f, 1f},
                new Color[]{NfsPalette.alpha(NfsPalette.RED, 0.85f), NfsPalette.alpha(NfsPalette.RED, 0f)}));
        g.fill(new Ellipse2D.Double(10, 130, 120, 120));
        g.setColor(new Color(0xFF3A3A));
        g.fillPolygon(new int[]{54, 118, 118, 56}, new int[]{190, 176, 184, 200}, 4);
    }
}
