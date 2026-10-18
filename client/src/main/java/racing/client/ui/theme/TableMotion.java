package racing.client.ui.theme;

import javax.swing.JTable;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Row motion for a menu table: on first load the rows fade and slide in one after another, and the
 * row under the mouse gets a highlight that fades in and out. {@link MenuCells} renderers read
 * {@link #reveal} / {@link #hover} through {@link #of}; with no motion running both are neutral
 * (1 and 0), so a settled table paints exactly as before.
 */
public final class TableMotion {
    private static final String KEY = "nfs.tableMotion";
    private static final int ROW_STAGGER = 45, MAX_STAGGERED = 12;

    private final JTable table;
    private boolean revealed;
    private float clock = -1f; // ms into the row entrance; < 0 when idle
    private int inRow = -1, outRow = -1;
    private float inLevel, outLevel;
    private Motion.Tween hoverTween;

    private TableMotion(JTable table) { this.table = table; }

    /** Installs row motion on {@code table} (hover tracking starts at once). */
    public static TableMotion attach(JTable table) {
        TableMotion m = new TableMotion(table);
        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) { m.hoverTo(table.rowAtPoint(e.getPoint())); }
            @Override public void mouseDragged(MouseEvent e) { mouseMoved(e); }
            @Override public void mouseExited(MouseEvent e) { m.hoverTo(-1); }
        };
        table.addMouseListener(mouse);
        table.addMouseMotionListener(mouse);
        table.putClientProperty(KEY, m);
        return m;
    }

    /** Motion installed on {@code table}, or null. */
    public static TableMotion of(JTable table) {
        return table.getClientProperty(KEY) instanceof TableMotion m ? m : null;
    }

    /** Plays the one-by-one row entrance the first time the table has rows; later calls do nothing. */
    public void revealFirstLoad() {
        if (revealed || table.getRowCount() == 0) return;
        revealed = true;
        clock = 0f;
        int total = ROW_STAGGER * (Math.min(table.getRowCount(), MAX_STAGGERED) - 1) + Motion.NORMAL;
        table.repaint();
        WhenShown.run(table, () -> Motion.play(table, total, Motion.LINEAR, e -> { clock = e * total; table.repaint(); })
                .then(() -> { clock = -1f; table.repaint(); }));
    }

    /** Entrance progress of a view row: 0 hidden, 1 settled. */
    public float reveal(int row) {
        if (clock < 0f) return 1f;
        float t = (clock - Math.min(row, MAX_STAGGERED) * ROW_STAGGER) / Motion.NORMAL;
        return t <= 0f ? 0f : t >= 1f ? 1f : Motion.OUT_CUBIC.apply(t);
    }

    /** Hover highlight of a view row, 0..1. */
    public float hover(int row) {
        return row < 0 ? 0f : row == inRow ? inLevel : row == outRow ? outLevel : 0f;
    }

    private void hoverTo(int row) {
        if (row == inRow) return;
        if (outRow >= 0 && outRow != row) repaintRow(outRow); // a third row drops straight to 0
        float startIn = row == outRow ? outLevel : 0f, startOut = inLevel;
        outRow = inRow;
        inRow = row;
        if (hoverTween != null) hoverTween.stop();
        hoverTween = Motion.play(table, Motion.FAST, Motion.OUT_CUBIC, e -> {
            inLevel = startIn + (1f - startIn) * e;
            outLevel = startOut * (1f - e);
            repaintRow(inRow);
            repaintRow(outRow);
        });
    }

    private void repaintRow(int row) {
        if (row < 0 || row >= table.getRowCount()) return;
        Rectangle r = table.getCellRect(row, 0, true);
        table.repaint(0, r.y, table.getWidth(), r.height);
    }
}
