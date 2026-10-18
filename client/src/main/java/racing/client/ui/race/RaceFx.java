package racing.client.ui.race;

import racing.client.ui.theme.NfsPalette;
import racing.common.GameConfig;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.LinearGradientPaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Per-view screen effects: motion-blur speed streaks that grow with speed, and a cached vignette
 * with a distance fog band across the top. Streak geometry lives in flat arrays (no per-frame objects).
 */
final class RaceFx {
    private static final int STREAKS = 34;
    /** Edge rush starts at this speed (km/h) and is full at MAX_SPEED. */
    private static final double RUSH_FROM = 200;
    private static final int RUSH = 16;
    private static final float[] XF = new float[STREAKS], PHASE = new float[STREAKS], PAR = new float[STREAKS],
            LEN = new float[STREAKS], GAIN = new float[STREAKS];
    private static final int[] ZONE = new int[STREAKS];

    static {
        Random rnd = new Random(77);
        for (int i = 0; i < STREAKS; i++) {
            XF[i] = rnd.nextFloat();
            PHASE[i] = rnd.nextFloat();
            PAR[i] = 1.25f + rnd.nextFloat() * 1.6f;
            LEN[i] = 0.5f + rnd.nextFloat();
            GAIN[i] = 0.45f + rnd.nextFloat() * 0.55f;
            ZONE[i] = i % 5 == 4 ? 2 : i % 2;   // 0 left of road, 1 right of road, 2 over the road (faint)
        }
    }

    private BufferedImage vignette;

    /** Streaks only appear once the car is really moving; length and brightness follow speed. */
    void drawStreaks(Graphics2D g, RaceSprites sp, int x, int y, int w, int h, int roadX, double camPx, double speedRatio) {
        if (speedRatio < 0.18) {
            return;
        }
        BufferedImage img = sp.streak();
        double power = (speedRatio - 0.18) / 0.82;
        int count = Math.max(6, (int) Math.round(STREAKS * (0.35 + 0.65 * power)));
        int leftW = roadX - sp.kerbW - x, rightX = roadX + sp.roadW + sp.kerbW, rightW = x + w - rightX;
        for (int i = 0; i < count; i++) {
            int len = sp.px(26 + 150 * power * LEN[i]);
            double span = h + len;
            double yy = (PHASE[i] * span + camPx * PAR[i]) % span - len;
            int sx;
            double a = power * GAIN[i] * 0.9;
            if (ZONE[i] == 0) {
                sx = x + (int) (XF[i] * leftW);
            } else if (ZONE[i] == 1) {
                sx = rightX + (int) (XF[i] * rightW);
            } else {
                sx = roadX + (int) (XF[i] * sp.roadW);
                a *= 0.35;
            }
            g.setComposite(RaceGfx.alpha(a));
            g.drawImage(img, sx, y + (int) yy, img.getWidth(), len, null);
        }
        g.setComposite(RaceGfx.ALPHA[16]);
    }

    /**
     * Bright, fast streaks hugging both road edges (kerbs) above {@value #RUSH_FROM} km/h; count,
     * length and brightness scale with how far past that speed the car is.
     */
    void drawRush(Graphics2D g, RaceSprites sp, int y, int h, int roadX, double camPx, double kmh) {
        double p = (kmh - RUSH_FROM) / (GameConfig.MAX_SPEED - RUSH_FROM);
        if (p <= 0) {
            return;
        }
        p = Math.min(1, p);
        BufferedImage img = sp.streak();
        int sw = img.getWidth() * 2, band = sp.kerbW + sp.px(14);
        int count = (int) Math.round(RUSH * (0.4 + 0.6 * p));
        for (int i = 0; i < count; i++) {
            int len = sp.px(70 + 170 * p * LEN[i]);
            double span = h + len;
            double yy = (PHASE[i] * span + camPx * (2.4 + PAR[i] * 0.5)) % span - len;
            int off = (int) (XF[i] * band);
            int sx = i % 2 == 0 ? roadX - sp.kerbW - sp.px(4) + off - band / 2 : roadX + sp.roadW + sp.kerbW + sp.px(4) - off + band / 2 - sw;
            g.setComposite(RaceGfx.alpha(p * (0.45 + 0.5 * GAIN[i])));
            g.drawImage(img, sx, y + (int) yy, sw, len, null);
        }
        g.setComposite(RaceGfx.ALPHA[16]);
    }

    /** Dark edges and a fog band across the top of the view (cached for the view size). */
    void drawVignette(Graphics2D g, int x, int y, int w, int h) {
        if (vignette == null || vignette.getWidth() != w || vignette.getHeight() != h) {
            vignette = buildVignette(w, h);
        }
        g.drawImage(vignette, x, y, null);
    }

    private static BufferedImage buildVignette(int w, int h) {
        BufferedImage img = RaceGfx.image(w, h);
        Graphics2D g = img.createGraphics();
        AffineTransform keep = g.getTransform();
        g.translate(w / 2.0, h * 0.58);
        g.scale(1, h / (double) w * 1.15);
        g.setPaint(new RadialGradientPaint(0, 0, (float) (w * 0.78), new float[]{0f, 0.62f, 1f},
                new Color[]{new Color(0, 0, 0, 0), new Color(3, 4, 8, 40), new Color(3, 4, 8, 215)}));
        g.fill(new Rectangle2D.Double(-w, -w, 2 * w, 2 * w));
        g.setTransform(keep);
        g.setPaint(new LinearGradientPaint(0, 0, 0, (float) (h * 0.30), new float[]{0f, 1f},
                new Color[]{RaceGfx.a(NfsPalette.VOID, 0.92), RaceGfx.a(NfsPalette.VOID, 0)}));
        g.fill(new Rectangle2D.Double(0, 0, w, h * 0.30));
        g.dispose();
        return img;
    }
}
