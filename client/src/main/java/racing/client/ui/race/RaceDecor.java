package racing.client.ui.race;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Roadside city for one view: rooftops and trees in up to three columns per side, plus street
 * lamps and the light pools they throw on the road. Placement is a pure function of the 28 m slot
 * index, so it is stable while scrolling. Only cached sprites are blitted; nothing is allocated.
 */
final class RaceDecor {
    private static final int SLOT = 28, MAX_COLUMNS = 3;

    private final int seed;

    RaceDecor(int seed) {
        this.seed = seed;
    }

    /** Rooftops and trees, drawn under the road strip. */
    void drawCity(Graphics2D g, RaceSprites sp, int x, int w, int roadX, int viewTop, int viewH, int anchorY,
                  double renderDist) {
        double pxPerM = sp.n / (double) SLOT;
        long first = (long) Math.floor((renderDist - (viewTop + viewH - anchorY) / pxPerM) / SLOT) - 1;
        long last = (long) Math.ceil((renderDist + (anchorY - viewTop) / pxPerM) / SLOT) + 1;
        int leftEdge = roadX - sp.kerbW, rightEdge = roadX + sp.roadW + sp.kerbW;
        for (long slot = first; slot <= last; slot++) {
            for (int side = 0; side < 2; side++) {
                int cursor = side == 0 ? leftEdge : rightEdge;
                for (int col = 0; col < MAX_COLUMNS; col++) {
                    int hash = hash(slot, side, col);
                    int kind = Math.floorMod(hash, 100);
                    int gap = sp.px(col == 0 ? 8 + Math.floorMod(hash >>> 9, 10) : 6 + Math.floorMod(hash >>> 9, 18));
                    BufferedImage img;
                    if (kind < 66) {
                        img = sp.roof(Math.floorMod(hash >>> 4, RaceScenery.ROOF_VARIANTS), side == 0, col > 0);
                    } else if (kind < 84) {
                        img = sp.tree(Math.floorMod(hash >>> 12, 2));
                    } else {
                        img = null;
                    }
                    int iw = img == null ? sp.px(52) : img.getWidth();
                    int sx = side == 0 ? cursor - gap - iw : cursor + gap;
                    cursor = side == 0 ? sx : sx + iw;
                    if (img != null) {
                        double m = slot * SLOT + Math.floorMod(hash >>> 16, 3);
                        int sy = anchorY - (int) Math.round((m - renderDist) * pxPerM) - img.getHeight();
                        g.drawImage(img, sx, sy, null);
                    }
                    if ((side == 0 && cursor < x) || (side == 1 && cursor > x + w)) {
                        break;
                    }
                }
            }
        }
    }

    /** Warm pools on the asphalt under each lamp. Draw over the road, under the cars. */
    void drawPools(Graphics2D g, RaceSprites sp, int roadX, int viewTop, int viewH, int anchorY, double renderDist) {
        BufferedImage pool = sp.lampPool();
        int cx = roadX + sp.roadW / 2 - pool.getWidth() / 2;
        double pxPerM = sp.n / (double) SLOT;
        long first = firstSlot(pxPerM, viewTop, viewH, anchorY, renderDist), last = lastSlot(pxPerM, viewTop, anchorY, renderDist);
        for (long slot = first; slot <= last; slot++) {
            int sy = lampY(slot, pxPerM, anchorY, renderDist);
            g.drawImage(pool, cx, sy - pool.getHeight() / 2, null);
        }
    }

    /** Lamp posts with glowing heads on both kerbs. Draw last so they hang over the cars. */
    void drawLamps(Graphics2D g, RaceSprites sp, int roadX, int viewTop, int viewH, int anchorY, double renderDist) {
        BufferedImage left = sp.lamp(true), right = sp.lamp(false);
        int xl = roadX - sp.kerbW - sp.px(7) - sp.px(14), xr = roadX + sp.roadW + sp.kerbW + sp.px(7) - sp.px(50);
        int ay = sp.px(32);
        double pxPerM = sp.n / (double) SLOT;
        long first = firstSlot(pxPerM, viewTop, viewH, anchorY, renderDist), last = lastSlot(pxPerM, viewTop, anchorY, renderDist);
        for (long slot = first; slot <= last; slot++) {
            int sy = lampY(slot, pxPerM, anchorY, renderDist) - ay;
            g.drawImage(left, xl, sy, null);
            g.drawImage(right, xr, sy, null);
        }
    }

    private static long firstSlot(double pxPerM, int viewTop, int viewH, int anchorY, double renderDist) {
        return (long) Math.floor((renderDist - (viewTop + viewH - anchorY) / pxPerM) / SLOT) - 1;
    }

    private static long lastSlot(double pxPerM, int viewTop, int anchorY, double renderDist) {
        return (long) Math.ceil((renderDist + (anchorY - viewTop) / pxPerM) / SLOT) + 1;
    }

    private static int lampY(long slot, double pxPerM, int anchorY, double renderDist) {
        return anchorY - (int) Math.round((slot * SLOT + SLOT / 2.0 - renderDist) * pxPerM);
    }

    private int hash(long slot, int side, int col) {
        long v = slot * 0x9E3779B97F4A7C15L + side * 0xC2B2AE3D27D4EB4FL + col * 0x165667B19E3779F9L + seed;
        v ^= v >>> 31;
        v *= 0xBF58476D1CE4E5B9L;
        v ^= v >>> 29;
        return (int) v;
    }
}
