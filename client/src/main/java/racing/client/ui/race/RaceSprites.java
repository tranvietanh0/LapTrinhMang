package racing.client.ui.race;

import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Every race sprite for one view scale, built lazily and kept (a few scales are retained, so a
 * resize that settles back on a known size is free). The scale is quantised so that one 28 m tile
 * is exactly {@code n} pixels: lane width, road width and kerb are then whole pixels and the
 * scrolling layers never drift against the cars.
 */
final class RaceSprites {
    static final int STEP = 8, MIN_N = 56, MAX_N = 192;
    private static final int KEEP = 4;
    private static final Map<Integer, RaceSprites> CACHE = new LinkedHashMap<>(8, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, RaceSprites> e) {
            return size() > KEEP;
        }
    };

    /** Tile height in px (== lane width == one 28 m slice of road). */
    final int n;
    /** Logical-to-screen scale. */
    final double s;
    final int laneW, roadW, kerbW;

    private BufferedImage road, ground, beam, pool, streak, start, finish;
    private final BufferedImage[] own = new BufferedImage[2];
    private final BufferedImage[] traffic = new BufferedImage[RaceCarArt.TRAFFIC.length];
    private final BufferedImage[] explosion = new BufferedImage[2];
    private final BufferedImage[][][] roofs = new BufferedImage[RaceScenery.ROOF_VARIANTS][2][2];
    private final BufferedImage[] trees = new BufferedImage[2];
    private final BufferedImage[] lamps = new BufferedImage[2];

    private RaceSprites(int n) {
        this.n = n;
        this.s = n / (double) RaceScenery.TILE;
        this.laneW = n;
        this.roadW = 3 * n;
        this.kerbW = n / 8;
    }

    static synchronized RaceSprites at(int n) {
        return CACHE.computeIfAbsent(n, RaceSprites::new);
    }

    /** Quantised tile size for a raw scale (1.0 == the 84 px design tile). */
    static int stepFor(double rawScale) {
        int n = (int) Math.round(rawScale * RaceScenery.TILE / STEP) * STEP;
        return Math.max(MIN_N, Math.min(MAX_N, n));
    }

    BufferedImage road() {
        if (road == null) road = RaceScenery.roadTile(s);
        return road;
    }

    BufferedImage ground() {
        if (ground == null) ground = RaceScenery.groundTile(s);
        return ground;
    }

    /** Racer sprite: {@code rival} false = own hot-red car, true = ice-blue rival. */
    BufferedImage racer(boolean rival) {
        int i = rival ? 1 : 0;
        if (own[i] == null) own[i] = RaceCarArt.car(s, rival ? RaceCarArt.RIVAL : RaceCarArt.OWN);
        return own[i];
    }

    BufferedImage traffic(int kind) {
        int k = Math.floorMod(kind, traffic.length);
        if (traffic[k] == null) traffic[k] = RaceCarArt.car(s, RaceCarArt.TRAFFIC[k]);
        return traffic[k];
    }

    BufferedImage beam() {
        if (beam == null) beam = RaceCarArt.beam(s);
        return beam;
    }

    BufferedImage explosion(boolean big) {
        int i = big ? 1 : 0;
        if (explosion[i] == null) explosion[i] = RaceCarArt.explosion(s, big);
        return explosion[i];
    }

    BufferedImage roof(int variant, boolean neonOnRight, boolean far) {
        int v = Math.floorMod(variant, RaceScenery.ROOF_VARIANTS), a = neonOnRight ? 1 : 0, b = far ? 1 : 0;
        if (roofs[v][a][b] == null) roofs[v][a][b] = RaceScenery.roof(s, v, neonOnRight, far ? 0.55 : 1.0);
        return roofs[v][a][b];
    }

    BufferedImage tree(int variant) {
        int v = Math.floorMod(variant, trees.length);
        if (trees[v] == null) trees[v] = RaceScenery.tree(s, v);
        return trees[v];
    }

    BufferedImage lamp(boolean armToRight) {
        int i = armToRight ? 1 : 0;
        if (lamps[i] == null) lamps[i] = RaceScenery.lamp(s, armToRight);
        return lamps[i];
    }

    BufferedImage lampPool() {
        if (pool == null) pool = RaceScenery.lampPool(s);
        return pool;
    }

    BufferedImage streak() {
        if (streak == null) streak = RaceScenery.streak(s);
        return streak;
    }

    BufferedImage startBanner() {
        if (start == null) start = RaceScenery.banner(s, "START", false);
        return start;
    }

    BufferedImage finishBanner() {
        if (finish == null) finish = RaceScenery.banner(s, "FINISH", true);
        return finish;
    }

    int px(double logical) {
        return (int) Math.round(logical * s);
    }
}
