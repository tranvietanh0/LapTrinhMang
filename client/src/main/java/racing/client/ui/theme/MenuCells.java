package racing.client.ui.theme;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;

/**
 * Table cell renderers for the menu screens: roster names, rank badges, status / result chips and
 * plain numbers. All of them follow the window scale and share the zebra + amber selection look.
 */
public final class MenuCells {
    private static final Color ALT = new Color(0x161B24);
    private static final Font CHIP_FONT = NfsFonts.tracked(NfsFonts.strong(12.5f), 0.12f);

    /** Text and colour of one chip. */
    public record Chip(String text, Color color) { }

    private MenuCells() { }

    private abstract static class Cell extends JComponent implements TableCellRenderer {
        private final IntFunction<Color> stripe;
        Object value;
        String text = "";
        int row, col;
        boolean selected;
        float scale = 1f;
        private Color tint;
        private float reveal = 1f, hover;

        Cell(IntFunction<Color> stripe) {
            this.stripe = stripe;
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int r, int c) {
            value = v;
            text = v == null ? "" : NfsFonts.safe(v.toString());
            selected = sel;
            row = r;
            col = c;
            scale = Responsive.scaleOf(t);
            tint = stripe == null ? null : stripe.apply(r);
            TableMotion motion = TableMotion.of(t);
            reveal = motion == null ? 1f : motion.reveal(r);
            hover = motion == null ? 0f : motion.hover(r);
            return this;
        }

        int p(int base) { return Math.round(base * scale); }

        Font f(Font base) { return base.deriveFont(base.getSize2D() * scale); }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            MenuPaint.quality(g);
            int w = getWidth(), h = getHeight();
            Color base = row % 2 == 0 ? NfsPalette.SURFACE : ALT;
            g.setColor(selected ? NfsPalette.mix(NfsPalette.SURFACE, NfsPalette.AMBER, 0.14f)
                    : hover > 0f ? NfsPalette.mix(base, NfsPalette.AMBER, 0.08f * hover) : base);
            g.fillRect(0, 0, w, h);
            Graphics2D fx = (Graphics2D) g.create();
            if (reveal < 1f) { // row entrance: content fades in while sliding from the left
                fx.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, reveal));
                fx.translate(-Math.round((1f - reveal) * p(36)), 0);
            }
            Color bar = tint != null ? tint : (selected ? NfsPalette.AMBER : hover > 0f ? NfsPalette.alpha(NfsPalette.AMBER, 0.55f * hover) : null);
            if (col == 0 && bar != null) {
                fx.setColor(bar);
                fx.fillRect(0, 0, p(4), h);
            }
            g.setColor(NfsPalette.LINE);
            g.fillRect(0, h - 1, w, 1);
            paintCell(fx, w, h);
            fx.dispose();
            g.dispose();
        }

        abstract void paintCell(Graphics2D g, int w, int h);

        /** Draws text vertically centred, shortened to fit; returns the drawn width. */
        int text(Graphics2D g, String s, Font font, Color color, int x, int maxW, int h) {
            g.setFont(font);
            FontMetrics fm = g.getFontMetrics();
            String shown = MenuPaint.ellipsize(fm, s, Math.max(0, maxW));
            g.setColor(color);
            g.drawString(shown, x, (h - fm.getHeight()) / 2 + fm.getAscent());
            return fm.stringWidth(shown);
        }
    }

    /** Left-aligned text cell. {@code stripe} (nullable) tints the row's left edge. */
    public static TableCellRenderer text(Font base, Color color, IntFunction<Color> stripe) {
        return new Cell(stripe) {
            @Override void paintCell(Graphics2D g, int w, int h) {
                text(g, text, f(base), color, p(16), w - p(28), h);
            }
        };
    }

    /** Roster name: initial badge, name and an amber "BẠN" tag on the player's own row. */
    public static TableCellRenderer player(IntPredicate selfRow) {
        return new Cell(null) {
            @Override void paintCell(Graphics2D g, int w, int h) {
                boolean self = selfRow.test(row);
                String name = text.endsWith(" (bạn)") ? text.substring(0, text.length() - 6) : text;
                int size = Math.min(p(34), h - p(12)), x = p(16), y = (h - size) / 2;
                Shape badge = Chamfer.slash(x, y, size, size, size * 0.3f);
                g.setPaint(self ? new GradientPaint(0, y, NfsPalette.AMBER_HOT, 0, y + size, NfsPalette.AMBER_DEEP)
                        : new GradientPaint(0, y, NfsPalette.SURFACE_HI, 0, y + size, NfsPalette.INPUT));
                g.fill(badge);
                g.setColor(self ? NfsPalette.AMBER : NfsPalette.LINE);
                g.draw(badge);
                g.setFont(f(NfsFonts.display(19f)));
                g.setColor(self ? NfsPalette.VOID : NfsPalette.MUTED);
                String initial = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
                MenuPaint.centered(g, initial, x + size / 2, y + (size - g.getFontMetrics().getHeight()) / 2 + g.getFontMetrics().getAscent());
                int tagW = self ? p(52) : 0;
                int used = text(g, name, f(NfsFonts.strong(17f)), NfsPalette.TEXT, x + size + p(14), w - x - size - p(30) - tagW, h);
                if (self) {
                    g.setFont(f(CHIP_FONT));
                    FontMetrics fm = g.getFontMetrics();
                    int cx = x + size + p(14) + used + p(10), ch = p(22);
                    MenuPaint.chip(g, cx, (h - ch) / 2, fm.stringWidth("BẠN") + p(18), ch, NfsPalette.AMBER);
                    g.setColor(NfsPalette.AMBER_HOT);
                    g.drawString("BẠN", cx + p(9), (h - fm.getHeight()) / 2 + fm.getAscent());
                }
            }
        };
    }

    /** Coloured chip chosen from the cell text (status, match result). */
    public static TableCellRenderer chip(Function<String, Chip> map, IntFunction<Color> stripe) {
        return new Cell(stripe) {
            @Override void paintCell(Graphics2D g, int w, int h) {
                Chip chip = map.apply(text);
                g.setFont(f(CHIP_FONT));
                FontMetrics fm = g.getFontMetrics();
                int ch = p(26), cw = Math.min(w - p(24), fm.stringWidth(chip.text()) + p(24));
                MenuPaint.chip(g, p(16), (h - ch) / 2, cw, ch, chip.color());
                g.setColor(chip.color());
                g.drawString(MenuPaint.ellipsize(fm, chip.text(), cw - p(16)), p(28), (h - fm.getHeight()) / 2 + fm.getAscent());
            }
        };
    }

    /** Rank badge: gold / silver / bronze for the podium, outlined for everyone else. */
    public static TableCellRenderer rank(IntFunction<Color> stripe) {
        return new Cell(stripe) {
            @Override void paintCell(Graphics2D g, int w, int h) {
                int rank = value instanceof Number n ? n.intValue() : 0;
                Color[] medal = MenuPaint.medal(rank);
                int bw = p(40), bh = Math.min(p(30), h - p(10)), x = p(16), y = (h - bh) / 2;
                Shape badge = Chamfer.slash(x, y, bw, bh, bh * 0.3f);
                if (medal != null) {
                    g.setPaint(new GradientPaint(0, y, medal[0], 0, y + bh, medal[1]));
                    g.fill(badge);
                } else {
                    g.setColor(NfsPalette.LINE);
                    g.draw(badge);
                }
                g.setFont(f(NfsFonts.display(18f)));
                g.setColor(medal != null ? NfsPalette.VOID : NfsPalette.MUTED);
                MenuPaint.centered(g, text, x + bw / 2, y + (bh - g.getFontMetrics().getHeight()) / 2 + g.getFontMetrics().getAscent());
            }
        };
    }
}
