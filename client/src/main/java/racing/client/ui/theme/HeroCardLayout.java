package racing.client.ui.theme;

import java.awt.*;

/**
 * Two-child layout (hero may be null for a card-only screen): a hero on the left and a card on the right. Below {@link #hideBelow} px of
 * width the hero is hidden and the card is centred. The card width follows the window scale.
 */
public final class HeroCardLayout implements LayoutManager {
    private final Component hero, card;
    private final int baseCardWidth, hideBelow, margin;

    public HeroCardLayout(Component hero, Component card, int baseCardWidth, int hideBelow, int margin) {
        this.hero = hero;
        this.card = card;
        this.baseCardWidth = baseCardWidth;
        this.hideBelow = hideBelow;
        this.margin = margin;
    }

    @Override public void layoutContainer(Container parent) {
        int w = parent.getWidth(), h = parent.getHeight();
        float s = Responsive.scaleOf(parent);
        int m = Math.round(margin * s);
        boolean wide = hero != null && w >= hideBelow;
        if (hero != null) hero.setVisible(wide);
        int cw = Math.min(Math.round(baseCardWidth * s), w - 2 * m);
        int ch = Math.min(card.getPreferredSize().height, h - 2 * m);
        int cx = wide ? w - cw - m * 2 : (w - cw) / 2;
        card.setBounds(cx, Math.max(m, (h - ch) / 2), cw, ch);
        if (wide) hero.setBounds(0, 0, cx - m, h);
    }

    @Override public Dimension preferredLayoutSize(Container parent) {
        Dimension c = card.getPreferredSize();
        return new Dimension(Math.round(baseCardWidth * 2.2f), c.height + 2 * margin);
    }

    @Override public Dimension minimumLayoutSize(Container parent) { return new Dimension(baseCardWidth, 300); }
    @Override public void addLayoutComponent(String name, Component comp) { }
    @Override public void removeLayoutComponent(Component comp) { }
}
