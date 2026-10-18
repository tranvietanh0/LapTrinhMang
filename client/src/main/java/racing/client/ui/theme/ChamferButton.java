package racing.client.ui.theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;

/**
 * Angled-corner button with animated hover glow and press feedback. Primary = amber fill,
 * secondary = dark glass with an outline. Reserves {@link #GLOW} px around the shape for the glow.
 */
public final class ChamferButton extends JButton {
    public static final int GLOW = 4;
    private final boolean primary;
    private final Timer anim = new Timer(16, e -> step());
    private float hover;      // 0..1 animated
    private boolean hovered;
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
            @Override public void mouseEntered(MouseEvent e) { hovered = true; anim.start(); }
            @Override public void mouseExited(MouseEvent e) { hovered = false; anim.start(); }
        });
        getModel().addChangeListener(e -> repaint());
    }

    @Override
    public void setText(String text) { super.setText(NfsFonts.safe(text)); }

    private void step() {
        float target = hovered && isEnabled() ? 1f : 0f;
        hover += Math.signum(target - hover) * 0.18f;
        if (Math.abs(target - hover) < 0.18f) { hover = target; anim.stop(); }
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) return super.getPreferredSize();
        FontMetrics fm = getFontMetrics(getFont());
        Insets in = getInsets();
        return new Dimension(fm.stringWidth(getText()) + in.left + in.right + GLOW * 2, fm.getHeight() + in.top + in.bottom + GLOW * 2);
    }

    @Override
    public void removeNotify() { anim.stop(); super.removeNotify(); }

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
        g.setFont(getFont());
        FontMetrics fm = g.getFontMetrics();
        int tx = (w - fm.stringWidth(getText())) / 2;
        int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
        g.setColor(!on ? NfsPalette.MUTED : primary ? NfsPalette.VOID : NfsPalette.TEXT);
        g.drawString(getText(), tx, ty);
        g.dispose();
    }
}
