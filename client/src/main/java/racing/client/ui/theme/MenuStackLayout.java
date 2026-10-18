package racing.client.ui.theme;

import java.awt.*;

/**
 * Vertical stack: every visible child gets its preferred height, the LAST visible child takes the
 * remaining space. The gap between children follows the window scale.
 */
public final class MenuStackLayout implements LayoutManager {
    private final int gap;

    public MenuStackLayout(int baseGap) { this.gap = baseGap; }

    @Override public void layoutContainer(Container parent) {
        Insets in = parent.getInsets();
        int g = Math.round(gap * Responsive.scaleOf(parent));
        int x = in.left, w = parent.getWidth() - in.left - in.right;
        int y = in.top, bottom = parent.getHeight() - in.bottom;
        Component last = lastVisible(parent);
        for (Component c : parent.getComponents()) {
            if (!c.isVisible()) continue;
            int h = c == last ? Math.max(0, bottom - y) : Math.min(c.getPreferredSize().height, Math.max(0, bottom - y));
            c.setBounds(x, y, w, h);
            y += h + g;
        }
    }

    private static Component lastVisible(Container parent) {
        Component last = null;
        for (Component c : parent.getComponents()) if (c.isVisible()) last = c;
        return last;
    }

    @Override public Dimension preferredLayoutSize(Container parent) {
        Insets in = parent.getInsets();
        int g = Math.round(gap * Responsive.scaleOf(parent));
        int w = 0, h = 0, n = 0;
        for (Component c : parent.getComponents()) {
            if (!c.isVisible()) continue;
            Dimension d = c.getPreferredSize();
            w = Math.max(w, d.width);
            h += d.height;
            n++;
        }
        return new Dimension(w + in.left + in.right, h + Math.max(0, n - 1) * g + in.top + in.bottom);
    }

    @Override public Dimension minimumLayoutSize(Container parent) { return preferredLayoutSize(parent); }
    @Override public void addLayoutComponent(String name, Component comp) { }
    @Override public void removeLayoutComponent(Component comp) { }
}
