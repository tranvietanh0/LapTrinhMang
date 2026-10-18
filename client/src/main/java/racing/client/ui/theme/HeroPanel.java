package racing.client.ui.theme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

/**
 * Left-hand hero art for splash screens: logo, huge italic slogan, street racer and stat chips.
 * Everything is composed into one cached image per size, so repaint cost is a single drawImage.
 */
public final class HeroPanel extends JComponent {
    private final String eyebrow, line1, line2, tagline;
    private final String[][] stats; // {value, caption}
    private BufferedImage art;

    public HeroPanel(String eyebrow, String line1, String line2, String tagline, String[][] stats) {
        this.eyebrow = eyebrow;
        this.line1 = line1;
        this.line2 = line2;
        this.tagline = tagline;
        this.stats = stats;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        if (art == null || art.getWidth() != w || art.getHeight() != h) art = render(w, h);
        g.drawImage(art, 0, 0, null);
    }

    private BufferedImage render(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        float u = Math.max(0.45f, Math.min(w / 700f, h / 640f));
        int x0 = Math.round(Math.max(24, w * 0.07f));
        // slogan size shrinks until the longest line fits
        float titleSize = 116f * u;
        Font title = NfsFonts.hero(titleSize);
        int longest = Math.max(g.getFontMetrics(title).stringWidth(line1), g.getFontMetrics(title).stringWidth(line2));
        if (longest > w - x0 - 16) title = NfsFonts.hero(titleSize * (w - x0 - 16) / longest);
        FontMetrics tf = g.getFontMetrics(title);

        logo(g, x0, Math.round(h * 0.06f), u);
        int y = Math.round(h * 0.06f + 100 * u);
        g.setFont(NfsFonts.tracked(NfsFonts.strong(15f * u), 0.28f));
        g.setColor(NfsPalette.AMBER);
        g.fillRect(x0, y - Math.round(10 * u), Math.round(34 * u), Math.max(2, Math.round(3 * u)));
        g.drawString(eyebrow, x0 + Math.round(46 * u), y);
        y += Math.round(tf.getAscent() * 0.92f) + Math.round(8 * u);
        headline(g, line1, title, x0, y, NfsPalette.TEXT);
        y += Math.round(tf.getAscent() * 0.86f);
        headline(g, line2, title, x0, y, NfsPalette.AMBER);
        y += tf.getDescent() + Math.round(30 * u);
        g.setFont(NfsFonts.body(19f * u));
        g.setColor(NfsPalette.MUTED);
        g.drawString(tagline, x0, y);

        int chipsH = Math.round(66 * u);
        int chipsY = h - chipsH - Math.round(h * 0.07f);
        double carW = Math.min(w * 0.92, (chipsY - y - 24 * u) * CarArt.BOX_W / CarArt.BOX_H * 1.05);
        if (carW > 160) CarArt.draw(g, x0 - 10 * u, chipsY - carW * CarArt.BOX_H / CarArt.BOX_W - 8 * u, carW);
        chips(g, x0, chipsY, chipsH, u, w - x0 * 2);
        g.dispose();
        return img;
    }

    private static void logo(Graphics2D g, int x, int y, float u) {
        float s = 34 * u;
        g.setColor(NfsPalette.AMBER);
        g.fill(Chamfer.shape(x, y, s * 0.42f, s, 0, 0, 0, 0));
        Path2D.Float slash = new Path2D.Float();
        slash.moveTo(x + s * 0.55f, y);
        slash.lineTo(x + s * 0.80f, y);
        slash.lineTo(x + s * 0.40f + s * 0.25f, y + s);
        slash.lineTo(x + s * 0.55f + 0f - s * 0.15f, y + s);
        slash.closePath();
        g.setColor(NfsPalette.RED);
        g.fill(slash);
        g.setFont(NfsFonts.tracked(NfsFonts.hero(30f * u), 0.06f));
        FontMetrics fm = g.getFontMetrics();
        int tx = x + Math.round(s * 1.15f), ty = y + (Math.round(s) + fm.getAscent() - fm.getDescent()) / 2;
        g.setColor(NfsPalette.TEXT);
        g.drawString("RACING", tx, ty);
        g.setColor(NfsPalette.AMBER);
        g.drawString("ONLINE", tx + fm.stringWidth("RACING ") + 2, ty);
    }

    private static void headline(Graphics2D g, String text, Font font, int x, int baseline, Color color) {
        g.setFont(font);
        for (int i = 6; i >= 1; i -= 2) { // cheap bloom behind the glyphs
            g.setColor(NfsPalette.alpha(color == NfsPalette.TEXT ? NfsPalette.AMBER : color, 0.035f));
            g.drawString(text, x - i, baseline);
            g.drawString(text, x + i, baseline);
        }
        g.setColor(new Color(0, 0, 0, 140));
        g.drawString(text, x + 3, baseline + 4);
        g.setColor(color);
        g.drawString(text, x, baseline);
    }

    private void chips(Graphics2D g, int x, int y, int h, float u, int totalW) {
        int gap = Math.round(14 * u);
        int cw = Math.min(Math.round(200 * u), (totalW - gap * (stats.length - 1)) / stats.length);
        for (int i = 0; i < stats.length; i++) {
            int cx = x + i * (cw + gap);
            Path2D.Float chip = Chamfer.slash(cx, y, cw, h, 12 * u);
            g.setColor(NfsPalette.alpha(NfsPalette.BG, 0.78f));
            g.fill(chip);
            g.setColor(NfsPalette.LINE);
            g.setStroke(new BasicStroke(1.2f));
            g.draw(chip);
            g.setColor(NfsPalette.AMBER);
            g.fillRect(cx, y + Math.round(12 * u), Math.max(3, Math.round(4 * u)), h - Math.round(24 * u));
            g.setFont(NfsFonts.display(30f * u));
            g.setColor(NfsPalette.TEXT);
            g.drawString(stats[i][0], cx + Math.round(18 * u), y + Math.round(33 * u));
            g.setFont(NfsFonts.tracked(NfsFonts.strong(12f * u), 0.14f));
            g.setColor(NfsPalette.MUTED);
            g.drawString(stats[i][1], cx + Math.round(18 * u), y + Math.round(53 * u));
        }
    }
}
