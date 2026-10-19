package racing.client.ui.theme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

/**
 * Segmented, slanted countdown bar: amber while time remains, red for the last few segments. In the
 * urgent range every tick flashes the lit segments and lets the flash decay, like a heartbeat.
 */
public final class MenuCountdownBar extends JComponent {
    private static final int URGENT = 5, PULSE_MS = 700;
    private final int max;
    private final Path2D.Float segment = new Path2D.Float();
    private int value;
    private float pulse; // 1 right after an urgent tick, decays to 0
    private Motion.Tween beat;

    public MenuCountdownBar(int max) {
        this.max = Math.max(1, max);
        this.value = this.max;
        setAlignmentX(LEFT_ALIGNMENT);
        setOpaque(false);
    }

    public void setValue(int value) {
        int v = Math.max(0, Math.min(max, value));
        if (v == this.value) return;
        this.value = v;
        if (v > 0 && v <= URGENT && isDisplayable()) {
            if (beat != null) beat.stop();
            beat = Motion.play(this, PULSE_MS, Motion.OUT_CUBIC, e -> { pulse = 1f - e; repaint(); });
        }
        repaint();
    }

    @Override
    public void removeNotify() {
        if (beat != null) beat.stop();
        pulse = 0f;
        super.removeNotify();
    }

    private int p(int base) { return Math.round(base * Responsive.scaleOf(this)); }

    @Override public Dimension getPreferredSize() { return new Dimension(p(300), p(18)); }
    @Override public Dimension getMinimumSize() { return new Dimension(0, p(18)); }
    @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, p(18)); }

    @Override
    protected void paintComponent(Graphics graphics) {
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        Graphics2D g = (Graphics2D) graphics.create();
        MenuPaint.quality(g);
        int gap = Math.max(2, p(3)), skew = Math.max(2, p(5));
        float segW = (w - skew - gap * (max - 1)) / (float) max;
        Color on = value <= URGENT ? NfsPalette.RED : NfsPalette.AMBER;
        if (pulse > 0f) on = NfsPalette.mix(on, NfsPalette.AMBER_HOT, 0.55f * pulse);
        for (int i = 0; i < max; i++) {
            float x = i * (segW + gap);
            segment.reset();
            segment.moveTo(x + skew, 0);
            segment.lineTo(x + segW + skew, 0);
            segment.lineTo(x + segW, h);
            segment.lineTo(x, h);
            segment.closePath();
            g.setColor(i < value ? on : NfsPalette.alpha(NfsPalette.LINE, 0.8f));
            g.fill(segment);
            if (pulse > 0f && i < value) {
                g.setStroke(new BasicStroke(1f + 2f * pulse));
                g.setColor(NfsPalette.alpha(NfsPalette.RED, 0.5f * pulse));
                g.draw(segment);
            }
        }
        g.dispose();
    }
}
