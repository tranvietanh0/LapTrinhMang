package racing.client.ui.theme;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/** Wide "challenger" banner: kicker, big opponent name, amber sub line and a speeding car. Cached per size. */
public final class MenuChallengeBanner extends JComponent {
    private final String kicker, name, sub;
    private BufferedImage cache;
    private int cacheW, cacheH;
    private float cacheScale;

    public MenuChallengeBanner(String kicker, String name, String sub) {
        this.kicker = kicker;
        this.name = name;
        this.sub = sub;
        setAlignmentX(LEFT_ALIGNMENT);
        setOpaque(false);
    }

    private int p(int base) { return Math.round(base * Responsive.scaleOf(this)); }

    @Override public Dimension getPreferredSize() { return new Dimension(p(400), p(150)); }
    @Override public Dimension getMinimumSize() { return new Dimension(0, p(150)); }
    @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, p(150)); }

    @Override
    protected void paintComponent(Graphics graphics) {
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        float s = Responsive.scaleOf(this);
        if (cache == null || cacheW != w || cacheH != h || cacheScale != s) {
            cache = render(w, h, s);
            cacheW = w;
            cacheH = h;
            cacheScale = s;
        }
        graphics.drawImage(cache, 0, 0, null);
    }

    private BufferedImage render(int w, int h, float s) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        MenuPaint.quality(g);
        Shape frame = Chamfer.shape(1, 1, w - 2, h - 2, 18 * s, 0, 18 * s, 0);
        g.setPaint(new GradientPaint(0, 0, new Color(0x241A10), w, h, NfsPalette.VOID));
        g.fill(frame);
        g.setClip(frame);
        g.setColor(NfsPalette.alpha(NfsPalette.AMBER, 0.14f));
        for (int i = 0; i < 4; i++) {
            float x = w * 0.50f + i * 34 * s;
            g.fillPolygon(new int[]{Math.round(x + 40 * s), Math.round(x + 62 * s), Math.round(x + 22 * s), Math.round(x)},
                    new int[]{0, 0, h, h}, 4);
        }
        double cw = Math.min(w * 0.62, h * 1.9);
        CarArt.draw(g, w - cw * 0.98, h - cw * 0.3 - 6 * s, cw);
        g.setClip(null);
        g.setStroke(new BasicStroke(1.4f));
        g.setColor(NfsPalette.alpha(NfsPalette.AMBER, 0.6f));
        g.draw(frame);
        g.setColor(NfsPalette.RED);
        g.fillRect(Math.round(20 * s), 1, Math.round(26 * s), Math.round(4 * s));
        g.setColor(NfsPalette.AMBER);
        g.fillRect(Math.round(50 * s), 1, Math.round(70 * s), Math.round(4 * s));
        int textW = Math.round(w * 0.58f);
        g.setFont(NfsFonts.tracked(NfsFonts.strong(13f * s), 0.2f));
        g.setColor(NfsPalette.MUTED);
        g.drawString(NfsFonts.safe(kicker), 24 * s, 40 * s);
        g.setFont(NfsFonts.hero(46f * s));
        g.setColor(NfsPalette.TEXT);
        g.drawString(MenuPaint.ellipsize(g.getFontMetrics(), NfsFonts.safe(name.toUpperCase()), textW), 24 * s, 90 * s);
        g.setFont(NfsFonts.display(24f * s));
        g.setColor(NfsPalette.AMBER_HOT);
        g.drawString(NfsFonts.safe(sub), 24 * s, 125 * s);
        g.dispose();
        return img;
    }
}
