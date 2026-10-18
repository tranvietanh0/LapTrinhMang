package racing.client.ui.theme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

/**
 * Card with a brushed-carbon surface, angled corners and an amber edge. The chrome is rendered
 * once per size into a cached image; children are laid out by the layout passed in.
 */
public class ChamferPanel extends JPanel {
    private static final BufferedImage WEAVE = weave();
    private static final TexturePaint CARBON = new TexturePaint(WEAVE, new Rectangle(0, 0, WEAVE.getWidth(), WEAVE.getHeight()));
    private final int cut;
    private BufferedImage chrome;

    public ChamferPanel(LayoutManager layout, int cut) {
        super(layout);
        this.cut = cut;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        if (chrome == null || chrome.getWidth() != w || chrome.getHeight() != h) chrome = render(w, h);
        graphics.drawImage(chrome, 0, 0, null);
    }

    private BufferedImage render(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float c = Math.max(8, Math.round(cut * Responsive.scaleOf(this)));
        Path2D.Float body = Chamfer.shape(1, 1, w - 2, h - 2, c, 0, c, 0);
        g.setPaint(new GradientPaint(0, 0, NfsPalette.alpha(NfsPalette.SURFACE_HI, 0.94f), 0, h, NfsPalette.alpha(NfsPalette.BG, 0.96f)));
        g.fill(body);
        g.setPaint(CARBON);
        g.fill(body);
        g.setPaint(new GradientPaint(0, 0, NfsPalette.alpha(Color.WHITE, 0.10f), 0, h * 0.35f, NfsPalette.alpha(Color.WHITE, 0f)));
        g.fill(body);
        g.setStroke(new BasicStroke(1.2f));
        g.setColor(NfsPalette.LINE);
        g.draw(body);
        // amber accent edge along the top plus a short slash at the cut corner
        float bar = Math.min(w * 0.38f, 220f);
        g.setColor(NfsPalette.AMBER);
        g.fill(Chamfer.shape(1, 1, bar, 4, c * 0.9f, 0, 0, 0));
        g.setColor(NfsPalette.RED);
        g.fillRect(Math.round(bar) + 6, 1, 18, 4);
        g.setColor(NfsPalette.alpha(NfsPalette.AMBER, 0.85f));
        g.setStroke(new BasicStroke(2.4f));
        g.drawLine(w - 2, h - 2 - Math.round(c) - 14, w - 2, h - 2 - Math.round(c));
        g.dispose();
        return img;
    }

    private static BufferedImage weave() {
        BufferedImage img = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        Color dark = new Color(0, 0, 0, 70), light = new Color(255, 255, 255, 14);
        for (int y = 0; y < 8; y += 4) {
            for (int x = 0; x < 8; x += 4) {
                boolean alt = ((x + y) / 4) % 2 == 0;
                g.setColor(alt ? dark : light);
                g.fillRect(x, y, 4, 2);
                g.setColor(alt ? light : dark);
                g.fillRect(x, y + 2, 4, 2);
            }
        }
        g.dispose();
        return img;
    }
}
