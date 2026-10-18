package racing.client.ui.theme;

import java.awt.*;

/** Two full-height columns: the left takes a fraction of the width (never below a scaled minimum). */
public final class MenuColumnsLayout implements LayoutManager {
    private final Component left, right;
    private final float fraction;
    private final int leftMin, gap;

    public MenuColumnsLayout(Component left, Component right, float fraction, int baseLeftMin, int baseGap) {
        this.left = left;
        this.right = right;
        this.fraction = fraction;
        this.leftMin = baseLeftMin;
        this.gap = baseGap;
    }

    @Override public void layoutContainer(Container parent) {
        Insets in = parent.getInsets();
        float s = Responsive.scaleOf(parent);
        int w = parent.getWidth() - in.left - in.right, h = parent.getHeight() - in.top - in.bottom;
        int g = Math.round(gap * s);
        int lw = Math.min(Math.max(Math.round(w * fraction), Math.round(leftMin * s)), w - g);
        left.setBounds(in.left, in.top, lw, h);
        right.setBounds(in.left + lw + g, in.top, Math.max(0, w - lw - g), h);
    }

    @Override public Dimension preferredLayoutSize(Container parent) {
        Insets in = parent.getInsets();
        Dimension l = left.getPreferredSize(), r = right.getPreferredSize();
        return new Dimension(l.width + r.width + gap + in.left + in.right, Math.max(l.height, r.height) + in.top + in.bottom);
    }

    @Override public Dimension minimumLayoutSize(Container parent) { return new Dimension(600, 300); }
    @Override public void addLayoutComponent(String name, Component comp) { }
    @Override public void removeLayoutComponent(Component comp) { }
}
