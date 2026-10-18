package racing.client.ui.theme;

import javax.swing.*;
import java.awt.*;

/**
 * Row of angled stat tiles (small tracked label over a big italic number). Whole-number values
 * count up (tile by tile) from their previous value whenever they change and the strip is shown.
 */
public final class MenuStatStrip extends JComponent {
    private static final Font LABEL = NfsFonts.tracked(NfsFonts.strong(12f), 0.14f);
    private static final int TILE_STAGGER = 70;
    private String[] labels = {}, values = {};
    private Color[] colors = {};
    private long[] from = {}, to = {};
    private float clock = -1f; // ms into the count-up; < 0 when idle
    private boolean waiting;
    private Motion.Tween count;

    public MenuStatStrip() {
        setAlignmentX(LEFT_ALIGNMENT);
        setOpaque(false);
    }

    public void setStats(String[] labels, String[] values, Color[] colors) {
        long[] start = new long[values.length], end = new long[values.length];
        boolean changed = false;
        for (int i = 0; i < values.length; i++) {
            end[i] = number(values[i]);
            start[i] = i < this.values.length && end[i] >= 0 ? Math.max(0, shownNumber(i)) : 0;
            changed |= end[i] >= 0 && start[i] != end[i];
        }
        this.labels = labels.clone();
        this.values = values.clone();
        this.colors = colors.clone();
        from = start;
        to = end;
        if (changed) countUp();
        revalidate();
        repaint();
    }

    private void countUp() {
        if (count != null) count.stop();
        clock = 0f;
        if (waiting) return;
        waiting = true;
        WhenShown.run(this, () -> {
            waiting = false;
            int total = Motion.SLOW + TILE_STAGGER * Math.max(0, values.length - 1);
            count = Motion.play(this, total, Motion.LINEAR, e -> { clock = e * total; repaint(); })
                    .then(() -> { clock = -1f; repaint(); });
        });
    }

    /** Non-negative whole number in the text, or -1 when it is not one (shown as-is, no count-up). */
    private static long number(String text) {
        if (text == null || text.isEmpty() || text.length() > 12 || !text.chars().allMatch(Character::isDigit)) return -1;
        return Long.parseLong(text);
    }

    /** Number currently on screen for tile i (mid count-up or settled), or -1. */
    private long shownNumber(int i) {
        if (i >= to.length || to[i] < 0) return number(values[i]);
        if (clock < 0f) return to[i];
        float t = Math.max(0f, Math.min(1f, (clock - i * TILE_STAGGER) / Motion.SLOW));
        return Math.round(from[i] + (to[i] - from[i]) * (double) Motion.OUT_CUBIC.apply(t));
    }

    private String shown(int i) {
        return clock < 0f || i >= to.length || to[i] < 0 ? values[i] : String.valueOf(shownNumber(i));
    }

    @Override
    public void removeNotify() {
        if (count != null) count.stop();
        clock = -1f;
        super.removeNotify();
    }

    private int p(int base) { return Math.round(base * Responsive.scaleOf(this)); }

    @Override public Dimension getPreferredSize() { return new Dimension(p(120) * Math.max(1, labels.length), p(84)); }
    @Override public Dimension getMinimumSize() { return new Dimension(0, p(84)); }
    @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, p(84)); }

    @Override
    protected void paintComponent(Graphics graphics) {
        int n = labels.length, w = getWidth(), h = getHeight();
        if (n == 0 || w <= 0) return;
        float s = Responsive.scaleOf(this);
        Graphics2D g = (Graphics2D) graphics.create();
        MenuPaint.quality(g);
        int gap = p(10), tw = (w - gap * (n - 1)) / n;
        for (int i = 0; i < n; i++) {
            int x = i * (tw + gap);
            Shape tile = Chamfer.slash(x + 0.5f, 0.5f, tw - 1, h - 1, p(12));
            g.setPaint(new GradientPaint(0, 0, NfsPalette.alpha(NfsPalette.SURFACE_HI, 0.95f), 0, h, NfsPalette.alpha(NfsPalette.INPUT, 0.95f)));
            g.fill(tile);
            g.setColor(NfsPalette.LINE);
            g.setStroke(new BasicStroke(1.2f));
            g.draw(tile);
            g.setColor(colors[i]);
            g.fillRect(x + p(16), 1, p(30), p(3));
            g.setFont(LABEL.deriveFont(LABEL.getSize2D() * s));
            g.setColor(NfsPalette.MUTED);
            g.drawString(NfsFonts.safe(labels[i]), x + p(16), p(28));
            float size = 38f * s;
            Font font = NfsFonts.display(size);
            String value = NfsFonts.safe(values[i]); // size is fitted to the final value so it never jumps
            while (size > 14f && g.getFontMetrics(font).stringWidth(value) > tw - p(28)) {
                size *= 0.92f;
                font = NfsFonts.display(size);
            }
            g.setFont(font);
            g.setColor(colors[i]);
            g.drawString(NfsFonts.safe(shown(i)), x + p(16), h - p(14));
        }
        g.dispose();
    }
}
