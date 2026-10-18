package racing.client.ui.theme;

import java.awt.geom.Path2D;

/** Angled-corner ("chamfered") shapes: the visual signature of the NFS theme. */
public final class Chamfer {
    private Chamfer() { }

    /** Rectangle with cut corners, clockwise from top-left: tl, tr, br, bl cut sizes (0 = square). */
    public static Path2D.Float shape(float x, float y, float w, float h, float tl, float tr, float br, float bl) {
        Path2D.Float p = new Path2D.Float();
        p.moveTo(x + tl, y);
        p.lineTo(x + w - tr, y);
        p.lineTo(x + w, y + tr);
        p.lineTo(x + w, y + h - br);
        p.lineTo(x + w - br, y + h);
        p.lineTo(x + bl, y + h);
        p.lineTo(x, y + h - bl);
        p.lineTo(x, y + tl);
        p.closePath();
        return p;
    }

    /** The default NFS cut: top-left and bottom-right corners angled. */
    public static Path2D.Float slash(float x, float y, float w, float h, float cut) {
        return shape(x, y, w, h, cut, 0, cut, 0);
    }
}
