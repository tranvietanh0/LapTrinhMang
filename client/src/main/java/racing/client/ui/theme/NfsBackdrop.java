package racing.client.ui.theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Night-city street backdrop for any window. The scene is cached per size (re-rendered 150 ms after
 * a resize settles); the only per-frame work is a few pre-baked speed streaks and bokeh sprites,
 * driven by ONE timer that runs only while the component is showing.
 */
public class NfsBackdrop extends JPanel {
    private static final int FRAME_MS = 33;
    private static final int STREAKS = 16, BOKEH = 9;
    private static final boolean ANIMATE = !Boolean.getBoolean("racing.noanim");

    private final BufferedImage[] streakSprite = {
            streakSprite(NfsPalette.AMBER_HOT), streakSprite(NfsPalette.RED), streakSprite(Color.WHITE)};
    private final BufferedImage bokehSprite = bokehSprite();
    // per streak: x, y (0..1 of size), length (fraction of width), speed (widths/sec), thickness px, sprite
    private final float[] sx = new float[STREAKS], sy = new float[STREAKS], sl = new float[STREAKS], sv = new float[STREAKS];
    private final int[] sth = new int[STREAKS], sk = new int[STREAKS];
    private final float[] bx = new float[BOKEH], by = new float[BOKEH], bs = new float[BOKEH], bv = new float[BOKEH];
    private final Timer ticker = new Timer(FRAME_MS, e -> tick());
    private final Timer settle = new Timer(150, e -> rerender());
    private BufferedImage scene;
    private long lastNanos;

    public NfsBackdrop() {
        super(null);
        setOpaque(true);
        setBackground(NfsPalette.VOID);
        settle.setRepeats(false);
        Random rnd = new Random(7);
        for (int i = 0; i < STREAKS; i++) {
            sx[i] = rnd.nextFloat();
            sy[i] = 0.64f + rnd.nextFloat() * 0.34f;
            sl[i] = 0.05f + rnd.nextFloat() * 0.18f;
            sv[i] = 0.10f + rnd.nextFloat() * 0.45f;
            sth[i] = 1 + rnd.nextInt(3);
            sk[i] = rnd.nextInt(5) == 0 ? 1 : (rnd.nextInt(4) == 0 ? 2 : 0);
        }
        for (int i = 0; i < BOKEH; i++) {
            bx[i] = rnd.nextFloat();
            by[i] = 0.35f + rnd.nextFloat() * 0.6f;
            bs[i] = 0.02f + rnd.nextFloat() * 0.05f;
            bv[i] = 0.004f + rnd.nextFloat() * 0.01f;
        }
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0) return;
            if (isShowing() && ANIMATE) { lastNanos = System.nanoTime(); ticker.start(); }
            else ticker.stop();
        });
    }

    private void tick() {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNanos) / 1e9f);
        lastNanos = now;
        for (int i = 0; i < STREAKS; i++) {
            sx[i] -= sv[i] * dt;
            if (sx[i] + sl[i] < 0) sx[i] = 1f + sl[i];
        }
        for (int i = 0; i < BOKEH; i++) {
            by[i] -= bv[i] * dt;
            if (by[i] < 0.25f) by[i] = 0.98f;
        }
        repaint();
    }

    private void rerender() {
        if (getWidth() > 0 && getHeight() > 0) { scene = BackdropArt.render(getWidth(), getHeight()); repaint(); }
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        Graphics2D g = (Graphics2D) graphics;
        if (scene == null) scene = BackdropArt.render(w, h);
        if (scene.getWidth() != w || scene.getHeight() != h) {
            g.drawImage(scene, 0, 0, w, h, null); // stale but cheap while the user drags the edge
            settle.restart();
        } else {
            g.drawImage(scene, 0, 0, null);
        }
        if (!ANIMATE) return;
        for (int i = 0; i < BOKEH; i++) {
            int d = Math.round(bs[i] * w);
            g.drawImage(bokehSprite, Math.round(bx[i] * w) - d / 2, Math.round(by[i] * h), d, d, null);
        }
        for (int i = 0; i < STREAKS; i++) {
            g.drawImage(streakSprite[sk[i]], Math.round(sx[i] * w), Math.round(sy[i] * h), Math.round(sl[i] * w), sth[i], null);
        }
    }

    private static BufferedImage streakSprite(Color c) {
        BufferedImage img = new BufferedImage(128, 4, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setPaint(new LinearGradientPaint(0, 0, 128, 0, new float[]{0f, 0.85f, 1f},
                new Color[]{NfsPalette.alpha(c, 0f), NfsPalette.alpha(c, 0.55f), NfsPalette.alpha(Color.WHITE, 0.95f)}));
        g.fillRect(0, 0, 128, 4);
        g.dispose();
        return img;
    }

    private static BufferedImage bokehSprite() {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new RadialGradientPaint(32, 32, 31, new float[]{0f, 0.8f, 0.92f, 1f},
                new Color[]{NfsPalette.alpha(NfsPalette.AMBER, 0.05f), NfsPalette.alpha(NfsPalette.AMBER, 0.10f),
                        NfsPalette.alpha(NfsPalette.AMBER_HOT, 0.22f), NfsPalette.alpha(NfsPalette.AMBER, 0f)}));
        g.fillOval(0, 0, 64, 64);
        g.dispose();
        return img;
    }
}
