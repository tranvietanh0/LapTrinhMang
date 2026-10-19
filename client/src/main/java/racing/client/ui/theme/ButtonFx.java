package racing.client.ui.theme;

import java.awt.*;

/** Paint-only effects of {@link ChamferButton}: the periodic light sweep and the busy stripes. */
final class ButtonFx {
    private ButtonFx() { }

    /** Slanted white band crossing the shape; {@code pos} 0 = just left of it, 1 = just right of it. */
    static void sheen(Graphics2D g, Shape shape, int w, int h, float pos) {
        float band = Math.max(28f, h * 0.9f);
        float cx = -band + pos * (w + band * 2f);
        Graphics2D s = (Graphics2D) g.create();
        s.clip(shape);
        s.setPaint(new LinearGradientPaint(cx - band, 0f, cx + band, h * 0.6f, new float[]{0f, 0.5f, 1f},
                new Color[]{NfsPalette.alpha(Color.WHITE, 0f), NfsPalette.alpha(Color.WHITE, 0.34f), NfsPalette.alpha(Color.WHITE, 0f)}));
        s.fill(shape);
        s.dispose();
    }

    /** Dim amber body with diagonal stripes marching right; {@code phase} 0..1 is one stripe period. */
    static void busy(Graphics2D g, Shape shape, int w, int h, float phase) {
        Graphics2D s = (Graphics2D) g.create();
        s.setPaint(new GradientPaint(0, 0, NfsPalette.mix(NfsPalette.SURFACE_HI, NfsPalette.AMBER_DEEP, 0.35f),
                0, h, NfsPalette.mix(NfsPalette.SURFACE, NfsPalette.AMBER_DEEP, 0.22f)));
        s.fill(shape);
        s.clip(shape);
        int period = Math.max(18, Math.round(h * 0.7f)), stripe = period / 2;
        s.setColor(NfsPalette.alpha(NfsPalette.AMBER_HOT, 0.16f));
        int offset = Math.round(phase * period);
        for (int x = -h - period + offset; x < w + h; x += period) {
            s.fillPolygon(new int[]{x, x + stripe, x + stripe + h, x + h}, new int[]{h, h, 0, 0}, 4);
        }
        s.dispose();
        g.setStroke(new BasicStroke(1.4f));
        g.setColor(NfsPalette.alpha(NfsPalette.AMBER, 0.7f));
        g.draw(shape);
    }
}
