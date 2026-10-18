package racing.client.ui.theme;

import java.awt.*;

/** Small painting helpers shared by the menu screens (lobby, leaderboard, history, invite). */
public final class MenuPaint {
    public static final Color[] GOLD = {new Color(0xFFD04A), new Color(0xD98200)};
    public static final Color[] SILVER = {new Color(0xE3E8EF), new Color(0x7C8696)};
    public static final Color[] BRONZE = {new Color(0xE0A06A), new Color(0x93552D)};

    private MenuPaint() { }

    /** Top/bottom gradient colours of a podium medal, or null for rank 4+. */
    public static Color[] medal(int rank) {
        return rank == 1 ? GOLD : rank == 2 ? SILVER : rank == 3 ? BRONZE : null;
    }

    public static void quality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
    }

    /** Shortens the text with an ellipsis until it fits maxW pixels. */
    public static String ellipsize(FontMetrics fm, String text, int maxW) {
        if (text == null) return "";
        if (fm.stringWidth(text) <= maxW) return text;
        int end = text.length();
        while (end > 1 && fm.stringWidth(text.substring(0, end) + "…") > maxW) end--;
        return text.substring(0, end) + "…";
    }

    /** Tinted, angled status chip. */
    public static void chip(Graphics2D g, int x, int y, int w, int h, Color color) {
        Shape shape = Chamfer.slash(x + 0.5f, y + 0.5f, w - 1, h - 1, Math.min(h * 0.3f, 8f));
        g.setColor(NfsPalette.alpha(color, 0.16f));
        g.fill(shape);
        g.setStroke(new BasicStroke(1.2f));
        g.setColor(NfsPalette.alpha(color, 0.85f));
        g.draw(shape);
    }

    public static void centered(Graphics2D g, String text, int cx, int baseline) {
        g.drawString(text, cx - g.getFontMetrics().stringWidth(text) / 2, baseline);
    }
}
