package racing.client.ui.theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;

/**
 * Angled-corner button with animated hover glow and press feedback. Primary = amber fill,
 * secondary = dark glass with an outline. Reserves {@link #GLOW} px around the shape for the glow.
 * Optional motion (all via {@link Motion}): a light sweep every few seconds ({@link #setSheen}) and
 * marching stripes while waiting on the network ({@link #setBusy}).
 */
public final class ChamferButton extends JButton {
    public static final int GLOW = 4;
    private static final int SHEEN_MS = 900, SHEEN_EVERY_MS = 4200, BUSY_LOOP_MS = 900;
    private final boolean primary;
    private float hover;      // 0..1 animated
    private boolean sheen, busy;
    private float sheenPos = -1f, busyPhase; // sweep position 0..1 (< 0 = none), stripe phase 0..1
    private Motion.Tween hoverTween, sheenTween, busyTween;
    private Path2D.Float shape;
    private int shapeW = -1, shapeH = -1;
    private Paint fillPaint;
    private int paintH = -1;

    public ChamferButton(String text, boolean primary) {
        super(NfsFonts.safe(text));
        this.primary = primary;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setBorder(BorderFactory.createEmptyBorder(8, 22, 8, 22));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Responsive.font(this, NfsFonts.tracked(NfsFonts.display(19f), 0.04f));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hoverTo(isEnabled() ? 1f : 0f); }
            @Override public void mouseExited(MouseEvent e) { hoverTo(0f); }
        });
        getModel().addChangeListener(e -> repaint());
    }

    @Override
    public void setText(String text) { super.setText(NfsFonts.safe(text)); }

    private void hoverTo(float target) {
        if (hoverTween != null) hoverTween.stop();
        float from = hover;
        hoverTween = Motion.play(this, Motion.FAST, Motion.OUT_CUBIC, e -> { hover = from + (target - from) * e; repaint(); });
    }

    /** Sweeps a soft light band across the button every few seconds while it is on screen and enabled. */
    public void setSheen(boolean on) {
        sheen = on;
        if (on && isDisplayable()) startSheen();
        else if (!on && sheenTween != null) { sheenTween.stop(); sheenPos = -1f; repaint(); }
    }

    private void startSheen() {
        if (sheenTween != null && sheenTween.running()) return;
        float idle = SHEEN_EVERY_MS - SHEEN_MS;
        sheenTween = Motion.play(this, SHEEN_EVERY_MS, Motion.LINEAR, e -> {
            float t = (e * SHEEN_EVERY_MS - idle) / SHEEN_MS;
            if (t < 0f) return; // idle part of the loop: no repaint at all
            sheenPos = Motion.IN_OUT_SINE.apply(Math.min(1f, t));
            repaint();
        }).then(() -> {
            sheenPos = -1f;
            repaint();
            if (sheen && isDisplayable()) sheenTween.start();
        });
    }

    /** Busy look (marching stripes) while a request is in flight; callers still disable the button. */
    public void setBusy(boolean on) {
        if (busy == on) return;
        busy = on;
        if (on && isDisplayable()) startBusy();
        else if (!on && busyTween != null) busyTween.stop();
        repaint();
    }

    private void startBusy() {
        busyTween = Motion.play(this, BUSY_LOOP_MS, Motion.LINEAR, e -> { busyPhase = e; repaint(); })
                .then(() -> { if (busy && isDisplayable()) busyTween.start(); });
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) return super.getPreferredSize();
        FontMetrics fm = getFontMetrics(getFont());
        Insets in = getInsets();
        return new Dimension(fm.stringWidth(getText()) + in.left + in.right + GLOW * 2, fm.getHeight() + in.top + in.bottom + GLOW * 2);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (sheen) startSheen();
        if (busy) startBusy();
    }

    @Override
    public void removeNotify() {
        for (Motion.Tween t : new Motion.Tween[]{hoverTween, sheenTween, busyTween}) if (t != null) t.stop();
        hover = 0f;
        sheenPos = -1f;
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        int w = getWidth(), h = getHeight();
        if (w <= 2 * GLOW || h <= 2 * GLOW) return;
        if (shape == null || shapeW != w || shapeH != h) {
            shapeW = w; shapeH = h;
            shape = Chamfer.slash(GLOW, GLOW, w - 2 * GLOW, h - 2 * GLOW, Math.min(14f, (h - 2 * GLOW) * 0.32f));
        }
        if (fillPaint == null || paintH != h) {
            paintH = h;
            fillPaint = primary
                    ? new GradientPaint(0, GLOW, NfsPalette.AMBER_HOT, 0, h - GLOW, NfsPalette.AMBER_DEEP)
                    : new GradientPaint(0, GLOW, NfsPalette.SURFACE_HI, 0, h - GLOW, NfsPalette.SURFACE);
        }
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
        boolean on = isEnabled();
        boolean pressed = on && getModel().isPressed();
        float glowLevel = on ? Math.max(hover, hasFocus() ? 0.6f : 0f) : 0f;
        Color glowColor = primary ? NfsPalette.AMBER_HOT : NfsPalette.AMBER;
        if (glowLevel > 0f) {
            for (int i = 3; i >= 1; i--) {
                g.setColor(NfsPalette.alpha(glowColor, glowLevel * (0.30f - i * 0.07f)));
                g.setStroke(new BasicStroke(i * 2f + 1f));
                g.draw(shape);
            }
        }
        g.translate(0, pressed ? 1 : 0);
        if (busy) {
            ButtonFx.busy(g, shape, w, h, busyPhase);
            paintLabel(g, w, h, NfsPalette.TEXT);
            g.dispose();
            return;
        }
        g.setPaint(on ? fillPaint : NfsPalette.SURFACE);
        g.fill(shape);
        if (on && hover > 0f) {
            g.setColor(NfsPalette.alpha(Color.WHITE, hover * (primary ? 0.16f : 0.06f)));
            g.fill(shape);
        }
        if (!primary || !on) {
            g.setStroke(new BasicStroke(1.4f));
            g.setColor(on ? NfsPalette.mix(NfsPalette.LINE, NfsPalette.AMBER, hover) : NfsPalette.LINE);
            g.draw(shape);
        }
        if (primary && on) { // hot top edge highlight
            g.setColor(NfsPalette.alpha(Color.WHITE, 0.35f));
            g.fillRect(GLOW + 14, GLOW, w - 2 * GLOW - 14, 1);
        }
        if (on && sheenPos >= 0f) ButtonFx.sheen(g, shape, w, h, sheenPos);
        paintLabel(g, w, h, !on ? NfsPalette.MUTED : primary ? NfsPalette.VOID : NfsPalette.TEXT);
        g.dispose();
    }

    private void paintLabel(Graphics2D g, int w, int h, Color color) {
        g.setFont(getFont());
        FontMetrics fm = g.getFontMetrics();
        g.setColor(color);
        g.drawString(getText(), (w - fm.stringWidth(getText())) / 2, (h - fm.getHeight()) / 2 + fm.getAscent());
    }
}
