package racing.client.ui.theme;

import javax.swing.*;
import java.awt.*;

/** Row of angled stat tiles (small tracked label over a big italic number). */
public final class MenuStatStrip extends JComponent {
    private static final Font LABEL = NfsFonts.tracked(NfsFonts.strong(12f), 0.14f);
    private String[] labels = {}, values = {};
    private Color[] colors = {};

    public MenuStatStrip() {
        setAlignmentX(LEFT_ALIGNMENT);
        setOpaque(false);
    }

    public void setStats(String[] labels, String[] values, Color[] colors) {
        this.labels = labels.clone();
        this.values = values.clone();
        this.colors = colors.clone();
        revalidate();
        repaint();
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
            String value = NfsFonts.safe(values[i]);
            while (size > 14f && g.getFontMetrics(font).stringWidth(value) > tw - p(28)) {
                size *= 0.92f;
                font = NfsFonts.display(size);
            }
            g.setFont(font);
            g.setColor(colors[i]);
            g.drawString(value, x + p(16), h - p(14));
        }
        g.dispose();
    }
}
