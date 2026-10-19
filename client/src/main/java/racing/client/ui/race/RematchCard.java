package racing.client.ui.race;

import racing.client.ui.theme.Chamfer;
import racing.client.ui.theme.ChamferButton;
import racing.client.ui.theme.Motion;
import racing.client.ui.theme.NfsPalette;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * Wraps the rematch card: it pops in (scale + fade, {@link Motion#OUT_BACK}) on the first on-screen
 * paint, then a soft amber glow breathes around the primary button. The glow ring is baked once per
 * button size; each pulse frame only blits it at a new alpha over the button's own rectangle.
 */
final class RematchCard extends JPanel {
    private static final int POP_MS = Motion.NORMAL + 60;
    private static final double PULSE_PERIOD_MS = 1600, GLOW_PX = 9;

    private final JButton primary;
    private final Motion.Loop pulse = Motion.loop(this::repaintGlow);
    private Motion.Tween pop;
    private float popT = 1f;
    private boolean frozen;
    private long pulseStart;
    private double frozenPulse = -1;
    private BufferedImage glow;
    private final Rectangle glowAt = new Rectangle();

    RematchCard(JComponent card, JButton primary) {
        super(new BorderLayout());
        this.primary = primary;
        setOpaque(false);
        add(card, BorderLayout.CENTER);
    }

    /** Pins the pop at {@code t} (0..1) and the glow at {@code pulse01} (0..1) for snapshots. */
    void freezeAt(float t, double pulse01) {
        frozen = true;
        popT = Math.max(0f, Math.min(1f, t));
        frozenPulse = pulse01;
        repaint();
    }

    @Override
    public void removeNotify() {
        if (pop != null) pop.stop();
        pulse.stop();
        super.removeNotify();
    }

    @Override
    public void paint(Graphics graphics) {
        if (!frozen && pop == null && isShowing()) {
            popT = 0f;
            pop = Motion.play(this, POP_MS, Motion.LINEAR, e -> {
                popT = e;
                repaint();
            }).then(() -> {
                pulseStart = System.currentTimeMillis();
                pulse.start();
            });
        }
        if (popT >= 1f) {
            super.paint(graphics);
            paintGlow((Graphics2D) graphics);
            return;
        }
        float k = Motion.OUT_BACK.apply(popT);
        double scale = 0.82 + 0.18 * k;
        Graphics2D g = (Graphics2D) graphics.create();
        g.setComposite(RaceGfx.alpha(Math.min(1, popT * 2.2)));
        g.translate(getWidth() / 2.0, getHeight() / 2.0);
        g.scale(scale, scale);
        g.translate(-getWidth() / 2.0, -getHeight() / 2.0);
        super.paint(g);
        g.dispose();
    }

    /** Glow level 0..1: a slow sine that rests at a faint baseline, never fully off. */
    private double pulseLevel() {
        if (frozenPulse >= 0) return frozenPulse;
        if (!pulse.running()) return 0;
        double ph = (System.currentTimeMillis() - pulseStart) / PULSE_PERIOD_MS * Math.PI * 2;
        return 0.5 - 0.5 * Math.cos(ph);
    }

    private void paintGlow(Graphics2D g) {
        if (!primary.isShowing() && !frozen) return;
        double level = pulseLevel();
        if (level <= 0 || !locateGlow()) return;
        g.setComposite(RaceGfx.alpha(0.15 + 0.6 * level));
        g.drawImage(glow, glowAt.x, glowAt.y, null);
        g.setComposite(RaceGfx.ALPHA[16]);
    }

    private void repaintGlow() {
        if (!isDisplayable()) {
            pulse.stop();
            return;
        }
        if (locateGlow()) repaint(glowAt.x, glowAt.y, glowAt.width, glowAt.height);
    }

    /** Places the (re)baked glow ring around the button, in this panel's coordinates. */
    private boolean locateGlow() {
        int bw = primary.getWidth(), bh = primary.getHeight();
        if (bw <= 2 * ChamferButton.GLOW || bh <= 2 * ChamferButton.GLOW) return false;
        int m = (int) Math.ceil(GLOW_PX);
        if (glow == null || glow.getWidth() != bw + 2 * m || glow.getHeight() != bh + 2 * m) {
            glow = bakeGlow(bw, bh, m);
        }
        Rectangle b = SwingUtilities.convertRectangle(primary.getParent(), primary.getBounds(), this);
        glowAt.setBounds(b.x - m, b.y - m, glow.getWidth(), glow.getHeight());
        return true;
    }

    /** Soft amber ring outside the button's chamfered shape (same geometry as {@link ChamferButton}). */
    private static BufferedImage bakeGlow(int bw, int bh, int m) {
        BufferedImage img = RaceGfx.image(bw + 2 * m, bh + 2 * m);
        Graphics2D g = RaceGfx.gfx(img, 1);
        int pad = ChamferButton.GLOW;
        float ih = bh - 2 * pad;
        var shape = Chamfer.slash(m + pad, m + pad, bw - 2 * pad, ih, Math.min(14f, ih * 0.32f));
        for (int i = 6; i >= 1; i--) {
            g.setColor(RaceGfx.a(NfsPalette.AMBER_HOT, 0.05 + 0.05 * (6 - i)));
            g.setStroke(new BasicStroke(i * 3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(shape);
        }
        g.setComposite(java.awt.AlphaComposite.Clear);   // keep the button face itself untouched
        g.fill(shape);
        g.dispose();
        return img;
    }
}
