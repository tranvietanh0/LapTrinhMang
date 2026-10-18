package racing.client.ui.race;

import racing.client.ui.theme.NfsPalette;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** Small drawing helpers shared by the race sprite builders (build time only, never per frame). */
final class RaceGfx {
    /** Pre-built alpha levels so paint code never allocates an AlphaComposite (index 0..16 maps to 0..1). */
    static final AlphaComposite[] ALPHA = new AlphaComposite[17];
    static {
        for (int i = 0; i < ALPHA.length; i++) ALPHA[i] = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, i / 16f);
    }

    private RaceGfx() { }

    static AlphaComposite alpha(double a) {
        return ALPHA[(int) Math.round(Math.max(0, Math.min(1, a)) * 16)];
    }

    static BufferedImage image(int w, int h) {
        return new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_ARGB_PRE);
    }

    /** Anti-aliased graphics for an image, already scaled so callers draw in logical units. */
    static Graphics2D gfx(BufferedImage img, double scale) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.scale(scale, scale);
        return g;
    }

    static Color a(Color c, double alpha) {
        return NfsPalette.alpha(c, (float) alpha);
    }

    static Color shade(Color c, double f) {
        return new Color(clamp(c.getRed() * f), clamp(c.getGreen() * f), clamp(c.getBlue() * f), c.getAlpha());
    }

    private static int clamp(double v) {
        return (int) Math.max(0, Math.min(255, Math.round(v)));
    }
}
