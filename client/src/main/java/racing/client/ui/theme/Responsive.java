package racing.client.ui.theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * Window-size driven scaling. A component opts in by storing a BASE font / height (design size at a
 * 1280x800 window); {@link #apply} re-derives them whenever the window's scale step changes.
 * Opting in never changes behaviour when no window installed the listener (scale stays 1.0).
 */
public final class Responsive {
    public static final double DESIGN_W = 1280, DESIGN_H = 800;
    public static final float MIN_SCALE = 0.8f, MAX_SCALE = 1.8f;
    private static final String SCALE = "nfs.scale", FONT = "nfs.font", HEIGHT = "nfs.h", ROW = "nfs.rowH";

    private Responsive() { }

    /** Scale factor for a window of this size, quantised to 0.05 steps so resizing does not thrash layout. */
    public static float scaleFor(int width, int height) {
        double raw = Math.min(width / DESIGN_W, height / DESIGN_H);
        float stepped = Math.round(raw * 20) / 20f;
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, stepped));
    }

    /** Current scale of the window that hosts this component (1.0 before it is attached). */
    public static float scaleOf(Component c) {
        JRootPane root = c == null ? null : SwingUtilities.getRootPane(c);
        return root != null && root.getClientProperty(SCALE) instanceof Float f ? f : 1f;
    }

    public static int px(Component c, int base) { return Math.round(base * scaleOf(c)); }

    /** Sets the BASE font of a component; the live font follows the window scale. */
    public static <T extends JComponent> T font(T c, Font base) {
        c.putClientProperty(FONT, base);
        c.setFont(scaled(base, scaleOf(c)));
        return c;
    }

    /** Fixes a BASE height (min/pref/max) that follows the window scale; widths are untouched. */
    public static <T extends JComponent> T height(T c, int base) {
        c.putClientProperty(HEIGHT, base);
        setHeight(c, Math.round(base * scaleOf(c)));
        return c;
    }

    /** Table row height that follows the window scale. */
    public static void rowHeight(JTable table, int base) {
        table.putClientProperty(ROW, base);
        table.setRowHeight(Math.round(base * scaleOf(table)));
    }

    /** Re-derives every opted-in descendant for the given scale. */
    public static void apply(Container root, float scale) {
        for (Component child : root.getComponents()) {
            if (child instanceof JComponent jc) {
                if (jc.getClientProperty(FONT) instanceof Font base) jc.setFont(scaled(base, scale));
                if (jc.getClientProperty(HEIGHT) instanceof Integer h) setHeight(jc, Math.round(h * scale));
                if (jc instanceof JTable t && t.getClientProperty(ROW) instanceof Integer r) t.setRowHeight(Math.round(r * scale));
            }
            if (child instanceof Container c) apply(c, scale);
        }
    }

    /** Hooks a window: keeps the scale property current and re-applies it on resize. */
    public static void attach(Window window, JRootPane rootPane) {
        java.util.function.Consumer<Boolean> update = force -> {
            if (window.getWidth() <= 0 || window.getHeight() <= 0) return;
            float scale = scaleFor(window.getWidth(), window.getHeight());
            if (!force && rootPane.getClientProperty(SCALE) instanceof Float old && old == scale) return;
            rootPane.putClientProperty(SCALE, scale);
            apply(rootPane, scale);
            rootPane.revalidate();
            rootPane.repaint();
        };
        window.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { update.accept(false); }
            @Override public void componentShown(ComponentEvent e) { update.accept(true); }
        });
        update.accept(true);
    }

    /** Fixes the scale of a root pane (e.g. a dialog that should match its owner window). */
    public static void pin(JRootPane rootPane, float scale) {
        rootPane.putClientProperty(SCALE, scale);
        apply(rootPane, scale);
    }

    private static Font scaled(Font base, float scale) { return base.deriveFont(base.getSize2D() * scale); }

    private static void setHeight(JComponent c, int h) {
        c.setMinimumSize(new Dimension(c.getMinimumSize().width, h));
        c.setPreferredSize(new Dimension(c.getPreferredSize().width, h));
        c.setMaximumSize(new Dimension(c.getMaximumSize().width, h));
    }
}
