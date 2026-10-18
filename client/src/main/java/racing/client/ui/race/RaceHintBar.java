package racing.client.ui.race;

import racing.client.ui.GameTheme;
import racing.client.ui.theme.Chamfer;
import racing.client.ui.theme.NfsFonts;
import racing.client.ui.theme.NfsPalette;
import racing.client.ui.theme.Responsive;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * Slim bar under the race: keycap chips for the controls on the left, the quit button on the right.
 * Repainted only on resize, so it paints directly instead of caching. Hints that do not fit are dropped
 * from the end, most important first.
 */
final class RaceHintBar extends JPanel {
    private static final int BASE_H = 56;
    /** keycap text, label; a keycap with a space is drawn as several caps. */
    private static final String[][] HINTS = {
            {"W", "TĂNG TỐC"}, {"S", "PHANH"}, {"A D", "ĐỔI LÀN"}, {"F11", "TOÀN MÀN HÌNH"}, {"ESC", "THOÁT"}};

    private final JButton quit;

    RaceHintBar(Runnable onQuit) {
        super(new BorderLayout());
        setOpaque(true);
        setBackground(NfsPalette.VOID);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 16));
        quit = GameTheme.button("THOÁT TRẬN", false);
        quit.setFocusable(false);
        quit.addActionListener(e -> onQuit.run());
        JPanel east = new JPanel(new java.awt.GridBagLayout());
        east.setOpaque(false);
        east.add(quit);
        add(east, BorderLayout.EAST);
        Responsive.height(this, BASE_H);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        int w = getWidth(), h = getHeight();
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        float u = Responsive.scaleOf(this);
        g.setPaint(new GradientPaint(0, 0, NfsPalette.SURFACE, 0, h, NfsPalette.VOID));
        g.fillRect(0, 0, w, h);
        g.setColor(NfsPalette.LINE);
        g.fillRect(0, 0, w, 1);

        Font capFont = NfsFonts.tracked(NfsFonts.display(15 * u), 0.04f);
        Font labelFont = NfsFonts.tracked(NfsFonts.strong(13.5f * u), 0.1f);
        FontMetrics cfm = g.getFontMetrics(capFont), lfm = g.getFontMetrics(labelFont);
        int x = Math.round(20 * u), limit = w - quit.getPreferredSize().width - Math.round(60 * u);
        int ch = Math.round(28 * u), cy = (h - ch) / 2;
        for (String[] hint : HINTS) {
            String[] caps = hint[0].split(" ");
            String label = NfsFonts.safe(hint[1]);
            int need = 0;
            for (String cap : caps) need += capWidth(cfm, cap, u) + Math.round(5 * u);
            need += Math.round(8 * u) + lfm.stringWidth(label) + Math.round(28 * u);
            if (x + need > limit) {
                break;
            }
            for (String cap : caps) {
                int cw = capWidth(cfm, cap, u);
                keycap(g, x, cy, cw, ch, u);
                g.setFont(capFont);
                g.setColor(NfsPalette.AMBER_HOT);
                g.drawString(cap, x + (cw - cfm.stringWidth(cap)) / 2, cy + (ch + cfm.getAscent() - cfm.getDescent()) / 2 - 1);
                x += cw + Math.round(5 * u);
            }
            x += Math.round(8 * u);
            g.setFont(labelFont);
            g.setColor(NfsPalette.MUTED);
            g.drawString(label, x, cy + (ch + lfm.getAscent() - lfm.getDescent()) / 2);
            x += lfm.stringWidth(label) + Math.round(28 * u);
        }
        g.dispose();
    }

    private static int capWidth(FontMetrics fm, String cap, float u) {
        return Math.max(Math.round(28 * u), fm.stringWidth(cap) + Math.round(16 * u));
    }

    private static void keycap(Graphics2D g, int x, int y, int w, int h, float u) {
        var shape = Chamfer.shape(x, y, w, h, 7 * u, 0, 7 * u, 0);
        g.setPaint(new GradientPaint(0, y, NfsPalette.SURFACE_HI, 0, y + h, NfsPalette.BG));
        g.fill(shape);
        g.setColor(NfsPalette.LINE);
        g.setStroke(new BasicStroke(1.2f));
        g.draw(shape);
        g.setColor(new Color(255, 176, 0, 90));
        g.fill(new RoundRectangle2D.Float(x + 7 * u, y + h - 3 * u, Math.max(2, w - 14 * u), 2 * u, 2, 2));
    }
}
