package racing.client.ui.theme;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Transparent panel that animates its direct children by painting them with an alpha, an offset or
 * a scale: a staggered entrance ({@link #reveal}) and a short horizontal shake ({@link #shake}).
 * Bounds, layout, focus and input are never touched, and while no motion runs it paints exactly like
 * a plain JPanel, so the settled state is the static design.
 */
public class RevealPanel extends JPanel {
    /** How a child enters: rise from below, slide in from the left, land from the left with overshoot, or pop (scale up). */
    public enum Style { RISE, SLIDE, LAND, POP }

    /** Default delay (ms) between two consecutive children of one entrance. */
    public static final int STAGGER = 40;
    private static final String STYLE = "nfs.reveal.style";
    private static final int RISE_PX = 22, SLIDE_PX = 48, LAND_PX = 30, SHAKE_PX = 10, SHAKE_MS = 300;

    private final List<Component> items = new ArrayList<>();
    private int delay, stagger;
    private float clock = -1f; // ms into the entrance; < 0 when idle
    private boolean waiting;
    private Motion.Tween entrance, shake;
    private Component shaken;
    private float shakeDx;

    public RevealPanel(LayoutManager layout) {
        super(layout);
        setOpaque(false);
    }

    /** Vertical BoxLayout variant, a drop-in for a form column. */
    public static RevealPanel column() {
        RevealPanel panel = new RevealPanel(null);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    /** Tags how {@code c} enters (default {@link Style#RISE}). */
    public static <T extends JComponent> T style(T c, Style style) {
        c.putClientProperty(STYLE, style);
        return c;
    }

    /**
     * Plays the entrance of {@code only} (every non-spacer child when empty): the first starts after
     * {@code delayMs}, each next one {@code staggerMs} later. Starts when the panel is first shown.
     */
    public void reveal(int delayMs, int staggerMs, Component... only) {
        items.clear();
        for (Component c : only.length > 0 ? only : getComponents()) if (!(c instanceof Box.Filler)) items.add(c);
        delay = delayMs;
        stagger = staggerMs;
        clock = 0f;
        if (entrance != null) entrance.stop();
        repaint();
        if (waiting) return;
        waiting = true;
        WhenShown.run(this, this::startEntrance);
    }

    private void startEntrance() {
        waiting = false;
        if (clock < 0f) return;
        int total = delay + stagger * Math.max(0, items.size() - 1) + Motion.NORMAL;
        entrance = Motion.play(this, total, Motion.LINEAR, e -> { clock = e * total; repaint(); })
                .then(() -> { clock = -1f; items.clear(); repaint(); });
    }

    /** Short decaying horizontal shake of one child, e.g. the form card after a failed submit. */
    public void shake(Component child) {
        if (!isShowing()) return;
        if (shake != null) shake.stop();
        shaken = child;
        float amp = SHAKE_PX * Responsive.scaleOf(this);
        shake = Motion.play(this, SHAKE_MS, Motion.LINEAR, t -> {
            shakeDx = (float) (Math.sin(t * Math.PI * 6) * (1f - t) * amp);
            repaint();
        }).then(() -> { shaken = null; shakeDx = 0f; repaint(); });
    }

    private boolean animating() { return clock >= 0f || shaken != null; }

    @Override
    public boolean isOptimizedDrawingEnabled() { return !animating() && super.isOptimizedDrawingEnabled(); }

    @Override
    public void removeNotify() {
        if (entrance != null) entrance.stop();
        if (shake != null) shake.stop();
        clock = -1f; // never leave children half-faded if the panel comes back
        items.clear();
        shaken = null;
        shakeDx = 0f;
        super.removeNotify();
    }

    @Override
    protected void paintChildren(Graphics g) {
        if (!animating()) { super.paintChildren(g); return; }
        float s = Responsive.scaleOf(this);
        for (Component c : getComponents()) {
            if (!c.isVisible() || c.getWidth() <= 0 || c.getHeight() <= 0) continue;
            float alpha = 1f, dx = c == shaken ? shakeDx : 0f, dy = 0f, scale = 1f;
            int idx = clock >= 0f ? items.indexOf(c) : -1;
            if (idx >= 0) {
                float t = Math.max(0f, Math.min(1f, (clock - delay - idx * stagger) / Motion.NORMAL));
                if (t <= 0f) continue;
                float out = Motion.OUT_CUBIC.apply(t), back = Motion.OUT_BACK.apply(t);
                switch (styleOf(c)) {
                    case SLIDE -> { alpha = out; dx -= (1f - out) * SLIDE_PX * s; }
                    case LAND -> { alpha = Math.min(1f, t * 2f); dx -= (1f - back) * LAND_PX * s; }
                    case POP -> { alpha = Math.min(1f, t * 2.2f); scale = 0.86f + 0.14f * back; }
                    default -> { alpha = out; dy = (1f - out) * RISE_PX * s; }
                }
            }
            paintChild((Graphics2D) g, c.getBounds(), alpha, dx, dy, scale);
        }
    }

    /**
     * Paints the one child inside {@code b} through Swing's own {@code paintChildren} (clipped to
     * that child) so nested double buffering keeps working, with the transform and alpha applied.
     */
    private void paintChild(Graphics2D g, Rectangle b, float alpha, float dx, float dy, float scale) {
        Graphics2D cg = (Graphics2D) g.create();
        try {
            cg.translate(Math.round(dx), Math.round(dy)); // whole pixels keep text crisp
            if (scale != 1f) {
                cg.translate(b.getCenterX(), b.getCenterY());
                cg.scale(scale, scale);
                cg.translate(-b.getCenterX(), -b.getCenterY());
            }
            if (alpha < 1f) {
                Composite cur = cg.getComposite();
                float base = cur instanceof AlphaComposite ac && ac.getRule() == AlphaComposite.SRC_OVER ? ac.getAlpha() : 1f;
                cg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, base * alpha));
            }
            cg.clipRect(b.x, b.y, b.width, b.height);
            super.paintChildren(cg);
        } finally {
            cg.dispose();
        }
    }

    private static Style styleOf(Component c) {
        return c instanceof JComponent jc && jc.getClientProperty(STYLE) instanceof Style st ? st : Style.RISE;
    }
}
