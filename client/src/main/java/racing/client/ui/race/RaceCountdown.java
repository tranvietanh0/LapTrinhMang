package racing.client.ui.race;

import racing.client.ui.theme.Chamfer;
import racing.client.ui.theme.Motion;
import racing.client.ui.theme.NfsFonts;
import racing.client.ui.theme.NfsPalette;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.HashMap;
import java.util.Map;

/**
 * Big centred "3 - 2 - 1 - GO!" overlay. Each word is baked once per scale into a glowing image;
 * the per-frame work is one scaled blit with a pop-in / fade-out animation and, on GO, a flash.
 */
final class RaceCountdown {
    private static final double PUNCH_S = 0.30;
    private final Map<String, BufferedImage> words = new HashMap<>();
    private float builtFor = -1;
    private BufferedImage flash, band;
    private int bandW = -1;
    private String text;
    private long stampNanos;

    void set(String value, long nowNanos) {
        if (value == null) {
            text = null;
            return;
        }
        if (!value.equals(text)) {
            text = value;
            stampNanos = nowNanos;
        }
    }

    boolean active() {
        return text != null;
    }

    void paint(Graphics2D g, int w, int cy, float u, long nowNanos) {
        if (text == null) {
            return;
        }
        if (u != builtFor) {
            words.clear();
            builtFor = u;
            flash = null;
            band = null;
        }
        BufferedImage img = words.get(text);
        if (img == null) {
            img = word(text, u);
            words.put(text, img);
            nowNanos = System.nanoTime();   // the bake takes a while: start the punch-in from its first visible frame
            stampNanos = nowNanos;
        }
        boolean go = text.startsWith("GO");
        double age = Math.max(0, (nowNanos - stampNanos) / 1e9);
        // numbers punch in: big and transparent, slam to a slight undershoot (OUT_BACK), settle at 1
        double scale = go ? 1 + age * 0.55 : 1.75 - 0.75 * Motion.OUT_BACK.apply((float) Math.min(1, age / PUNCH_S));
        double alpha = go ? 1 - Math.max(0, age - 0.28) / 0.32
                : age < 0.12 ? age / 0.12 : (age < 0.72 ? 1 : Math.max(0.1, 1 - (age - 0.72) / 0.28));
        if (band == null || bandW != w) {
            band = band(w, Math.round(img.getHeight() * 0.8f));
            bandW = w;
        }
        g.setComposite(RaceGfx.alpha(alpha * 0.9));
        g.drawImage(band, 0, cy - band.getHeight() / 2, null);
        if (go && age < 0.4) {
            if (flash == null) flash = flash();
            g.setComposite(RaceGfx.alpha(0.55 * (1 - age / 0.4)));
            g.drawImage(flash, 0, 0, w, cy * 2, null);
        }
        g.setComposite(RaceGfx.alpha(alpha));
        Object old = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        int dw = (int) Math.round(img.getWidth() * scale), dh = (int) Math.round(img.getHeight() * scale);
        g.drawImage(img, w / 2 - dw / 2, cy - dh / 2, dw, dh, null);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, old == null ? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR : old);
        g.setComposite(RaceGfx.ALPHA[16]);
    }

    private static BufferedImage band(int w, int h) {
        BufferedImage img = RaceGfx.image(w, Math.max(4, h));
        Graphics2D g = img.createGraphics();
        g.setPaint(new LinearGradientPaint(0, 0, 0, img.getHeight(), new float[]{0f, 0.25f, 0.75f, 1f}, new Color[]{
                new Color(5, 7, 11, 0), new Color(5, 7, 11, 150), new Color(5, 7, 11, 150), new Color(5, 7, 11, 0)}));
        g.fillRect(0, 0, w, img.getHeight());
        g.dispose();
        return img;
    }

    private static BufferedImage flash() {
        BufferedImage img = RaceGfx.image(64, 64);
        Graphics2D g = img.createGraphics();
        g.setPaint(new RadialGradientPaint(32, 32, 32, new float[]{0f, 0.6f, 1f}, new Color[]{
                RaceGfx.a(NfsPalette.AMBER_HOT, 0.95), RaceGfx.a(NfsPalette.AMBER, 0.35), RaceGfx.a(NfsPalette.RED, 0)}));
        g.fill(new Rectangle2D.Double(0, 0, 64, 64));
        g.dispose();
        return img;
    }

    private static BufferedImage word(String word, float u) {
        boolean go = word.startsWith("GO");
        return glowText(NfsFonts.safe(word), NfsFonts.hero((go ? 170 : 230) * u),
                go ? NfsPalette.AMBER : new Color(0xFF8A1E), Color.WHITE, go ? NfsPalette.AMBER_HOT : new Color(0xFFD9A0),
                u, Math.round(150 * u), true);
    }

    /**
     * Heavy italic text with a blurred halo, a hot outline and a vertical fill gradient, baked once.
     * {@code sidePad} reserves room left and right of the glyphs; {@code bars} adds the amber/red slash bars there.
     */
    static BufferedImage glowText(String shown, Font font, Color glow, Color top, Color bottom, float u, int sidePad,
                                  boolean bars) {
        BufferedImage probe = RaceGfx.image(4, 4);
        Graphics2D pg = probe.createGraphics();
        FontMetrics fm = pg.getFontMetrics(font);
        pg.dispose();
        int tw = fm.stringWidth(shown), th = fm.getAscent() + fm.getDescent();
        int pad = Math.round(font.getSize2D() * 0.24f);
        int w = tw + 2 * (sidePad + pad), h = th + 2 * pad;
        BufferedImage mask = RaceGfx.image(w, h);
        Graphics2D mg = RaceGfx.gfx(mask, 1);
        mg.setFont(font);
        mg.setColor(Color.WHITE);
        int tx = (w - tw) / 2, ty = pad + fm.getAscent();
        mg.drawString(shown, tx, ty);
        mg.dispose();
        BufferedImage halo = tint(blur(mask, Math.max(5, Math.round(font.getSize2D() * 0.06f))), glow);

        BufferedImage out = RaceGfx.image(w, h);
        Graphics2D g = RaceGfx.gfx(out, 1);
        g.drawImage(halo, 0, 0, null);
        g.drawImage(halo, 0, 0, null);
        g.drawImage(halo, 0, 0, null);
        if (bars) {
            int by = h / 2 - Math.round(7 * u), bh = Math.round(14 * u), bw = sidePad;
            for (int side = 0; side < 2; side++) {
                int bx = side == 0 ? pad / 2 : w - pad / 2 - bw;
                Color c = side == 0 ? NfsPalette.AMBER : NfsPalette.RED;
                g.setPaint(new GradientPaint(side == 0 ? bx : bx + bw, 0, RaceGfx.a(c, 0), side == 0 ? bx + bw : bx, 0, c));
                g.fill(Chamfer.shape(bx, by, bw, bh, 0, bh * 0.9f, 0, bh * 0.9f));
            }
        }
        g.setFont(font);
        g.setColor(RaceGfx.a(Color.BLACK, 0.7));
        g.drawString(shown, tx + Math.round(5 * u), ty + Math.round(6 * u));
        g.setColor(glow);
        g.setStroke(new java.awt.BasicStroke(Math.max(3f, font.getSize2D() * 0.03f), java.awt.BasicStroke.CAP_ROUND,
                java.awt.BasicStroke.JOIN_ROUND));
        g.draw(font.createGlyphVector(g.getFontRenderContext(), shown).getOutline(tx, ty));
        g.setPaint(new GradientPaint(0, ty - fm.getAscent() * 0.75f, top, 0, ty, bottom));
        g.drawString(shown, tx, ty);
        g.dispose();
        return out;
    }

    private static BufferedImage blur(BufferedImage src, int radius) {
        int k = radius * 2 + 1;
        float[] data = new float[k * k];
        java.util.Arrays.fill(data, 1f / (k * k));
        return new ConvolveOp(new Kernel(k, k, data), ConvolveOp.EDGE_NO_OP, null).filter(src, null);
    }

    private static BufferedImage tint(BufferedImage mask, Color c) {
        BufferedImage out = RaceGfx.image(mask.getWidth(), mask.getHeight());
        Graphics2D g = out.createGraphics();
        g.drawImage(mask, 0, 0, null);
        g.setComposite(java.awt.AlphaComposite.SrcIn);
        g.setColor(c);
        g.fillRect(0, 0, out.getWidth(), out.getHeight());
        g.dispose();
        return out;
    }
}
