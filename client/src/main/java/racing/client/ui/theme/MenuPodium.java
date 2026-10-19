package racing.client.ui.theme;

import racing.common.dto.RankRow;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

/**
 * Top-3 podium for the leaderboard (2nd - 1st - 3rd). Hides itself when there are no rows. When new
 * standings arrive the plinths rise (3rd, then 2nd, then 1st) and the points count up.
 */
public final class MenuPodium extends JComponent {
    private static final float[] HEIGHT = {1f, 0.80f, 0.64f};
    private static final int[] SLOT = {1, 0, 2};
    private static final int RISE_STAGGER = 110;
    private List<RankRow> rows = List.of();
    private float clock = -1f; // ms into the rise; < 0 when idle
    private boolean waiting;
    private Motion.Tween rise;

    public MenuPodium() {
        setOpaque(false);
        setVisible(false);
    }

    public void setRows(List<RankRow> rows) {
        List<RankRow> top = rows == null ? List.of() : List.copyOf(rows.subList(0, Math.min(3, rows.size())));
        boolean changed = !top.equals(this.rows);
        this.rows = top;
        setVisible(!top.isEmpty());
        if (changed && !top.isEmpty()) animate();
        revalidate();
        repaint();
    }

    private void animate() {
        if (rise != null) rise.stop();
        clock = 0f;
        if (waiting) return;
        waiting = true;
        WhenShown.run(this, () -> {
            waiting = false;
            int total = Motion.SLOW + RISE_STAGGER * 2;
            rise = Motion.play(this, total, Motion.LINEAR, e -> { clock = e * total; repaint(); })
                    .then(() -> { clock = -1f; repaint(); });
        });
    }

    /** Rise progress of podium place idx (0 = 1st), 0..1; 1st goes last for a little suspense. */
    private float progress(int idx) {
        if (clock < 0f) return 1f;
        float t = (clock - (2 - idx) * RISE_STAGGER) / Motion.SLOW;
        return t <= 0f ? 0f : t >= 1f ? 1f : t;
    }

    @Override
    public void removeNotify() {
        if (rise != null) rise.stop();
        clock = -1f;
        super.removeNotify();
    }

    private int p(int base) { return Math.round(base * Responsive.scaleOf(this)); }

    @Override public Dimension getPreferredSize() { return new Dimension(p(480), p(190)); }
    @Override public Dimension getMinimumSize() { return new Dimension(0, p(190)); }
    @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, p(190)); }

    @Override
    protected void paintComponent(Graphics graphics) {
        int w = getWidth(), h = getHeight(), n = rows.size();
        if (n == 0 || w <= 0) return;
        float s = Responsive.scaleOf(this);
        Graphics2D g = (Graphics2D) graphics.create();
        MenuPaint.quality(g);
        int gap = p(14), colW = Math.min((w - gap * 2) / 3, p(280));
        int[] order = Arrays.stream(SLOT).filter(i -> i < n).toArray();
        int total = order.length * colW + (order.length - 1) * gap, x0 = (w - total) / 2;
        int plinthMax = h - p(62);
        for (int k = 0; k < order.length; k++) {
            int idx = order[k];
            RankRow r = rows.get(idx);
            float t = progress(idx);
            if (t <= 0f) continue;
            float up = t >= 1f ? 1f : Motion.OUT_BACK.apply(t);
            int x = x0 + k * (colW + gap), ph = Math.round(plinthMax * HEIGHT[idx] * up), top = h - ph;
            Color[] medal = MenuPaint.medal(idx + 1);
            Graphics2D slot = g;
            if (t < 1f) {
                slot = (Graphics2D) g.create();
                slot.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, t * 2.5f)));
            }
            paintSlot(slot, r, idx, x, top, ph, colW, h, s, medal, t >= 1f ? 1f : Motion.OUT_CUBIC.apply(t));
            if (slot != g) slot.dispose();
        }
        g.dispose();
    }

    private void paintSlot(Graphics2D g, RankRow r, int idx, int x, int top, int ph, int colW, int h, float s, Color[] medal, float count) {
        if (idx == 0) {
            float cy = top + ph * 0.3f, radius = Math.min(colW * 0.75f, cy);
            g.setPaint(new RadialGradientPaint(x + colW / 2f, cy, radius, new float[]{0f, 1f},
                    new Color[]{NfsPalette.alpha(NfsPalette.AMBER, 0.30f), NfsPalette.alpha(NfsPalette.AMBER, 0f)}));
            g.fillRect(Math.round(x + colW / 2f - radius), Math.round(cy - radius), Math.round(radius * 2), Math.round(radius * 2));
        }
        Shape plinth = Chamfer.slash(x, top, colW, ph, p(18));
        g.setPaint(new GradientPaint(0, top, medal[0], 0, h, medal[1]));
        g.fill(plinth);
        g.setPaint(new GradientPaint(0, top, NfsPalette.alpha(Color.WHITE, 0.28f), 0, top + ph * 0.5f, NfsPalette.alpha(Color.WHITE, 0f)));
        g.fill(plinth);
        g.setFont(NfsFonts.hero(Math.max(1f, Math.min(ph * 0.85f, 88f * s))));
        FontMetrics big = g.getFontMetrics();
        g.setColor(NfsPalette.alpha(NfsPalette.VOID, 0.82f));
        MenuPaint.centered(g, String.valueOf(r.rank()), x + colW / 2, top + (ph + big.getAscent() - big.getDescent()) / 2);
        g.setFont(NfsFonts.display((idx == 0 ? 30f : 25f) * s));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(idx == 0 ? NfsPalette.AMBER_HOT : NfsPalette.TEXT);
        MenuPaint.centered(g, MenuPaint.ellipsize(fm, NfsFonts.safe(r.username()), colW - p(12)), x + colW / 2, top - p(26));
        g.setFont(NfsFonts.tracked(NfsFonts.strong(13.5f * s), 0.1f));
        g.setColor(NfsPalette.MUTED);
        String points = String.valueOf(count >= 1f ? r.points() : Math.round(r.points() * count));
        MenuPaint.centered(g, points + " ĐIỂM  •  " + r.wins() + " THẮNG", x + colW / 2, top - p(7));
    }
}
