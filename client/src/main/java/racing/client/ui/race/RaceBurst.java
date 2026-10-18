package racing.client.ui.race;

import racing.client.ui.theme.NfsPalette;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * One-shot effects of one road view, driven by the race render loop (no timers): a short camera
 * jolt plus a spark / smoke burst on collision, and a flash plus a checkered-flag burst when the
 * car finishes. Particle parameters live in flat arrays filled when the effect fires; every frame
 * evaluates closed-form motion and blits pre-baked sprites ({@link RaceSprites}), so painting
 * allocates nothing.
 */
final class RaceBurst {
    static final long SHAKE_MS = 300, IMPACT_MS = 900, FINISH_MS = 1500, FLASH_MS = 480;
    private static final long NONE = Long.MIN_VALUE / 4;
    private static final int SPARKS = 18, PUFFS = 6, FLAGS = 18;
    /** Peak jolt in logical px (about 4-9 screen px depending on the view scale). */
    private static final double SHAKE_PX = 4;
    private static BufferedImage flash;

    private final Random rnd;
    private final float[] spVx = new float[SPARKS], spVy = new float[SPARKS], spLife = new float[SPARKS];
    private final float[] puVx = new float[PUFFS], puVy = new float[PUFFS], puGrow = new float[PUFFS];
    private final float[] flVx = new float[FLAGS], flVy = new float[FLAGS], flSpin = new float[FLAGS],
            flSize = new float[FLAGS];
    private long hitAt = NONE, finishAt = NONE;

    RaceBurst(int seed) {
        this.rnd = new Random(seed);
    }

    void reset() {
        hitAt = NONE;
        finishAt = NONE;
    }

    /** Collision: jolt the camera and throw sparks and smoke from the hit point. */
    void hit(long nowMillis) {
        hitAt = nowMillis;
        for (int i = 0; i < SPARKS; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, v = 260 + rnd.nextDouble() * 360;
            spVx[i] = (float) (Math.cos(a) * v);
            spVy[i] = (float) (Math.sin(a) * v * 0.8 - 90);   // biased forward (up the screen)
            spLife[i] = 0.28f + rnd.nextFloat() * 0.34f;
        }
        for (int i = 0; i < PUFFS; i++) {
            double a = rnd.nextDouble() * Math.PI * 2;
            puVx[i] = (float) (Math.cos(a) * (18 + rnd.nextDouble() * 34));
            puVy[i] = (float) (Math.sin(a) * 22 - 26);
            puGrow[i] = 0.7f + rnd.nextFloat() * 0.6f;
        }
    }

    /** The car crossed the line: flash the view and burst checkered flags from the car. */
    void finish(long nowMillis) {
        finishAt = nowMillis;
        for (int i = 0; i < FLAGS; i++) {
            double a = -Math.PI / 2 + (rnd.nextDouble() - 0.5) * Math.PI * 1.5, v = 240 + rnd.nextDouble() * 420;
            flVx[i] = (float) (Math.cos(a) * v);
            flVy[i] = (float) (Math.sin(a) * v);
            flSpin[i] = (float) ((rnd.nextBoolean() ? 1 : -1) * (5 + rnd.nextDouble() * 7));
            flSize[i] = 0.75f + rnd.nextFloat() * 0.5f;
        }
    }

    /** Horizontal camera offset in screen px for this frame (0 when idle). */
    int shakeX(long nowMillis, double s) {
        double amp = shakeAmp(nowMillis, s);
        return amp == 0 ? 0 : (int) Math.round(amp * Math.sin((nowMillis - hitAt) * 0.11 + 1.2));
    }

    int shakeY(long nowMillis, double s) {
        double amp = shakeAmp(nowMillis, s);
        return amp == 0 ? 0 : (int) Math.round(amp * 0.6 * Math.cos((nowMillis - hitAt) * 0.083));
    }

    private double shakeAmp(long nowMillis, double s) {
        long age = nowMillis - hitAt;
        if (age < 0 || age >= SHAKE_MS) {
            return 0;
        }
        double k = 1 - age / (double) SHAKE_MS;
        return SHAKE_PX * s * k * k;
    }

    /** Sparks and smoke around the hit point ({@code cx}, {@code cy} in screen px). */
    void drawImpact(Graphics2D g, RaceSprites sp, int cx, int cy, long nowMillis) {
        long age = nowMillis - hitAt;
        if (age < 0 || age >= IMPACT_MS) {
            return;
        }
        double t = age / 1000.0;
        smooth(g, true);
        BufferedImage puff = sp.puff();
        for (int i = 0; i < PUFFS; i++) {
            double life = 0.85, k = t / life;
            if (k >= 1) continue;
            int size = (int) Math.round(puff.getWidth() * (0.45 + 0.75 * puGrow[i] * Math.sqrt(k)));
            int px = cx + (int) Math.round(puVx[i] * t * sp.s), py = cy + (int) Math.round(puVy[i] * t * sp.s);
            g.setComposite(RaceGfx.alpha(0.6 * (1 - k)));
            g.drawImage(puff, px - size / 2, py - size / 2, size, size, null);
        }
        BufferedImage spark = sp.spark();
        int half = spark.getWidth() / 2;
        for (int i = 0; i < SPARKS; i++) {
            double k = t / spLife[i];
            if (k >= 1) continue;
            double d = (1 - Math.exp(-t * 5)) / 5;   // drag: fast out, then hangs
            int px = cx + (int) Math.round(spVx[i] * d * sp.s), py = cy + (int) Math.round(spVy[i] * d * sp.s);
            g.setComposite(RaceGfx.alpha(1 - k * k));
            g.drawImage(spark, px - half, py - half, null);
        }
        g.setComposite(RaceGfx.ALPHA[16]);
        smooth(g, false);
    }

    /** Finish flash over the whole view and the flag burst from the car ({@code cx}, {@code cy}). */
    void drawFinish(Graphics2D g, RaceSprites sp, int x, int y, int w, int h, int cx, int cy, long nowMillis) {
        long age = nowMillis - finishAt;
        if (age < 0 || age >= FINISH_MS) {
            return;
        }
        smooth(g, true);
        if (age < FLASH_MS) {
            if (flash == null) flash = bakeFlash();
            double k = age / (double) FLASH_MS;
            int fw = (int) (w * 1.8), fh = (int) (h * 1.4);
            g.setComposite(RaceGfx.alpha(0.6 * (1 - k) * (1 - k)));
            g.drawImage(flash, cx - fw / 2, cy - fh / 2, fw, fh, null);
        }
        double t = age / 1000.0, life = FINISH_MS / 1000.0;
        double d = (1 - Math.exp(-t * 2.4)) / 2.4;
        double fade = Math.min(1, (life - t) / (life * 0.35));
        BufferedImage flag = sp.flag();
        for (int i = 0; i < FLAGS; i++) {
            int size = (int) Math.round(flag.getWidth() * flSize[i]);
            int fw = Math.max(1, (int) Math.round(size * Math.abs(Math.cos(flSpin[i] * t + i))));
            int px = cx + (int) Math.round(flVx[i] * d * sp.s);
            int py = cy + (int) Math.round((flVy[i] * d + 70 * t * t) * sp.s);
            g.setComposite(RaceGfx.alpha(fade));
            g.drawImage(flag, px - fw / 2, py - size / 2, fw, size, null);
        }
        g.setComposite(RaceGfx.ALPHA[16]);
        smooth(g, false);
    }

    /** Scaled soft sprites need bilinear filtering; the race view otherwise draws nearest-neighbour. */
    private static void smooth(Graphics2D g, boolean on) {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, on ? RenderingHints.VALUE_INTERPOLATION_BILINEAR
                : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    }

    // ------------------------------------------------------------------ sprites (build time only)

    static BufferedImage bakeSpark(double s) {
        int px = Math.max(6, (int) Math.ceil(14 * s));
        BufferedImage img = RaceGfx.image(px, px);
        Graphics2D g = RaceGfx.gfx(img, 1);
        float c = px / 2f;
        g.setPaint(new RadialGradientPaint(c, c, c, new float[]{0f, 0.3f, 1f}, new Color[]{
                Color.WHITE, RaceGfx.a(NfsPalette.AMBER_HOT, 0.85), RaceGfx.a(NfsPalette.AMBER_DEEP, 0)}));
        g.fill(new Rectangle2D.Float(0, 0, px, px));
        g.dispose();
        return img;
    }

    static BufferedImage bakePuff(double s) {
        int px = Math.max(12, (int) Math.ceil(72 * s));
        BufferedImage img = RaceGfx.image(px, px);
        Graphics2D g = RaceGfx.gfx(img, 1);
        float c = px / 2f;
        g.setPaint(new RadialGradientPaint(c, c, c, new float[]{0f, 0.55f, 1f}, new Color[]{
                new Color(120, 122, 132, 210), new Color(78, 80, 90, 130), new Color(40, 42, 50, 0)}));
        g.fill(new Ellipse2D.Float(0, 0, px, px));
        g.dispose();
        return img;
    }

    /** A small 3x3 checkered flag square with a light rim. */
    static BufferedImage bakeFlag(double s) {
        int cell = Math.max(2, (int) Math.round(5 * s)), px = cell * 3 + 2;
        BufferedImage img = RaceGfx.image(px, px);
        Graphics2D g = img.createGraphics();
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                g.setColor((r + c) % 2 == 0 ? new Color(0xF2EFE6) : new Color(0x0A0C10));
                g.fillRect(1 + c * cell, 1 + r * cell, cell, cell);
            }
        }
        g.setColor(RaceGfx.a(NfsPalette.AMBER_HOT, 0.8));
        g.setStroke(new BasicStroke(1f));
        g.drawRect(0, 0, px - 1, px - 1);
        g.dispose();
        return img;
    }

    private static BufferedImage bakeFlash() {
        BufferedImage img = RaceGfx.image(64, 64);
        Graphics2D g = img.createGraphics();
        g.setPaint(new RadialGradientPaint(32, 32, 32, new float[]{0f, 0.45f, 1f}, new Color[]{
                RaceGfx.a(Color.WHITE, 0.95), RaceGfx.a(NfsPalette.AMBER_HOT, 0.45), RaceGfx.a(NfsPalette.AMBER, 0)}));
        g.fill(new Rectangle2D.Double(0, 0, 64, 64));
        g.dispose();
        return img;
    }
}
