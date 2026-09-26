package racing.client.ui.race;

import racing.client.model.CarModel;
import racing.common.GameConfig;
import racing.common.dto.Obstacle;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Một nửa màn hình đua: camera cuộn dọc bám theo một xe, vẽ cỏ, cây, nhà, lề đường, mặt đường,
 * vạch làn, vạch xuất phát / đích, xe cộ, xe người chơi và vụ nổ. Toạ độ thế giới tính bằng mét
 * dọc đường đua; xe người chơi đứng yên trên màn hình ở {@link #ANCHOR} chiều cao khung nhìn.
 * Giữ trạng thái hiển thị riêng (quãng đường nội suy, làn trượt mượt, xe cộ đã bị tông).
 */
final class TrackView {

    /** Điểm ảnh trên một mét: xe dài 20 m cao 60 px, đúng bằng hình xe. */
    static final double PPM = PixelArt.PLAYER.getHeight() / GameConfig.CAR_LENGTH;
    static final int LANE_W = 84;
    static final int ROAD_W = LANE_W * GameConfig.LANES;
    private static final int KERB_W = 8;
    /** Đuôi xe người chơi nằm ở 88 % chiều cao khung nhìn, thân xe quanh mốc 3/4. */
    private static final double ANCHOR = 0.88;
    private static final long EXPLOSION_MS = 650;
    private static final double DECOR_SLOT_M = 14;

    private static final Color KERB = new Color(0x9A9A9A);
    private static final Color KERB_DARK = new Color(0x6E6E6E);
    private static final Color EDGE_LINE = new Color(0xEDEDED);
    private static final Color LANE_DASH = new Color(0xF2C12E);
    private static final BufferedImage GRASS = tile(256, 128, new Color(0x3FAA3A),
            new Color(0x369832), new Color(0x4FC24A), 11);
    private static final BufferedImage ASPHALT = tile(ROAD_W, 128, new Color(0x55575C),
            new Color(0x4A4C50), new Color(0x63656A), 29);

    private record Explosion(double worldM, int lane, long startMillis) {
    }

    private final CarModel car;
    private final BufferedImage carSprite;
    private final int decorSeed;
    private final Set<Integer> destroyed = new HashSet<>();
    private final List<Explosion> explosions = new ArrayList<>();

    private double renderDist;
    private double lastTarget = -1;
    private long targetStampNanos;
    private double laneX = 1;
    private long lastFrameNanos;

    TrackView(CarModel car, BufferedImage carSprite, int decorSeed) {
        this.car = car;
        this.carSprite = carSprite;
        this.decorSeed = decorSeed;
    }

    CarModel car() {
        return car;
    }

    /** Quãng đường đang hiển thị (đã nội suy mượt giữa các lần cập nhật 20 Hz). */
    double renderDist() {
        return renderDist;
    }

    void resetRound() {
        destroyed.clear();
        explosions.clear();
        renderDist = car.distance();
        lastTarget = -1;
        laneX = car.lane();
    }

    /** Xe vừa bị va chạm: nổ ở đầu xe, xe cộ gần nhất cùng làn biến mất. */
    void onHit(List<Obstacle> obstacles, double tickF, long nowMillis) {
        double d = car.distance();
        int best = -1;
        double bestGap = Double.MAX_VALUE;
        for (int i = 0; i < obstacles.size(); i++) {
            Obstacle o = obstacles.get(i);
            if (o.lane() != car.lane() || destroyed.contains(i)) {
                continue;
            }
            double p = trafficPos(o, tickF);
            // chồng lấn, nới 8 m cho sai lệch giữa dự đoán cục bộ và server
            if (d < p + GameConfig.OBSTACLE_LENGTH + 8 && p < d + GameConfig.CAR_LENGTH + 8) {
                double gap = Math.abs(p - d);
                if (gap < bestGap) {
                    bestGap = gap;
                    best = i;
                }
            }
        }
        if (best >= 0) {
            destroyed.add(best);
        }
        explosions.add(new Explosion(d + GameConfig.CAR_LENGTH * 0.8, car.lane(), nowMillis));
    }

    static double trafficPos(Obstacle o, double tickF) {
        return o.position() + o.speed() * GameConfig.KMH_TO_MS * tickF * GameConfig.TICK_MS / 1000.0;
    }

    /** Cập nhật trạng thái hiển thị một khung hình. */
    void update(long nowNanos) {
        double dt = lastFrameNanos == 0 ? 0 : Math.min(0.1, (nowNanos - lastFrameNanos) / 1e9);
        lastFrameNanos = nowNanos;

        double target = car.distance();
        if (target != lastTarget) {
            lastTarget = target;
            targetStampNanos = nowNanos;
        }
        double extra = car.stunned() || car.finished() ? 0
                : car.speed() * GameConfig.KMH_TO_MS * Math.min(0.12, (nowNanos - targetStampNanos) / 1e9);
        double desired = Math.min(GameConfig.TRACK_LENGTH, target + extra);
        // không lùi vì sai số nhỏ khi server hiệu chỉnh; lệch lớn (ván mới) thì nhảy luôn
        if (desired >= renderDist || renderDist - desired > 3) {
            renderDist = desired;
        }
        laneX += (car.lane() - laneX) * Math.min(1, dt * 14);
        if (Math.abs(car.lane() - laneX) < 0.01) {
            laneX = car.lane();
        }
    }

    /** Vẽ khung nhìn của đường đua vào vùng (x, y, w, h); clip do người gọi đặt. */
    void paint(Graphics2D g, int x, int y, int w, int h, List<Obstacle> obstacles, double tickF, long nowMillis) {
        int anchorY = y + (int) (h * ANCHOR);
        double camPx = renderDist * PPM;
        int roadX = x + (w - ROAD_W) / 2;

        tileVertical(g, GRASS, x, y, w, h, anchorY, camPx);
        drawDecor(g, x, roadX, y, h, anchorY);
        tileVertical(g, ASPHALT, roadX, y, ROAD_W, h, anchorY, camPx);

        double bottomM = renderDist - (y + h - anchorY) / PPM;
        double topM = renderDist + (anchorY - y) / PPM;

        // lề đường xám có khối sẫm cuộn theo
        g.setColor(KERB);
        g.fillRect(roadX - KERB_W, y, KERB_W, h);
        g.fillRect(roadX + ROAD_W, y, KERB_W, h);
        g.setColor(KERB_DARK);
        for (double m = Math.floor(bottomM / 6) * 6; m < topM + 6; m += 6) {
            int sy = screenY(m + 3, anchorY);
            g.fillRect(roadX - KERB_W, sy, KERB_W, (int) (3 * PPM));
            g.fillRect(roadX + ROAD_W, sy, KERB_W, (int) (3 * PPM));
        }
        g.setColor(EDGE_LINE);
        g.fillRect(roadX + 3, y, 3, h);
        g.fillRect(roadX + ROAD_W - 6, y, 3, h);

        // vạch làn vàng nét đứt
        g.setColor(LANE_DASH);
        for (double m = Math.floor(bottomM / 14) * 14; m < topM + 14; m += 14) {
            int sy = screenY(m + 7, anchorY);
            for (int i = 1; i < GameConfig.LANES; i++) {
                g.fillRect(roadX + i * LANE_W - 3, sy, 6, (int) (7 * PPM));
            }
        }

        drawStartFinish(g, roadX, anchorY, bottomM, topM);

        // xe cộ trong khung nhìn
        for (int i = 0; i < obstacles.size(); i++) {
            if (destroyed.contains(i)) {
                continue;
            }
            Obstacle o = obstacles.get(i);
            double p = trafficPos(o, tickF);
            if (p > topM || p + GameConfig.OBSTACLE_LENGTH < bottomM) {
                continue;
            }
            BufferedImage img = PixelArt.traffic(o.kind());
            int sx = roadX + o.lane() * LANE_W + (LANE_W - img.getWidth()) / 2;
            g.drawImage(img, sx, screenY(p + GameConfig.OBSTACLE_LENGTH, anchorY), null);
        }

        // xe người chơi, nhấp nháy khi đang bị choáng
        boolean blinkOff = car.stunned() && (nowMillis / 90) % 2 == 0;
        if (!blinkOff) {
            int cx = roadX + (int) Math.round(laneX * LANE_W) + (LANE_W - carSprite.getWidth()) / 2;
            g.drawImage(carSprite, cx, screenY(renderDist + GameConfig.CAR_LENGTH, anchorY), null);
        }

        drawExplosions(g, roadX, anchorY, nowMillis);
    }

    private void drawStartFinish(Graphics2D g, int roadX, int anchorY, double bottomM, double topM) {
        if (0 >= bottomM - 5 && 0 <= topM + 5) {
            int sy = screenY(0, anchorY);
            g.setColor(Color.WHITE);
            g.fillRect(roadX, sy - 4, ROAD_W, 8);
            PixelArt.drawText(g, "START", roadX + ROAD_W / 2, sy + 10, 3, Color.WHITE, 0);
        }
        double fin = GameConfig.TRACK_LENGTH;
        if (fin >= bottomM - 10 && fin <= topM + 10) {
            int sy = screenY(fin, anchorY);
            int cell = 12;
            for (int row = 0; row < 3; row++) {
                for (int cx = 0; cx * cell < ROAD_W; cx++) {
                    g.setColor((cx + row) % 2 == 0 ? Color.WHITE : Color.BLACK);
                    g.fillRect(roadX + cx * cell, sy - 18 + row * cell, Math.min(cell, ROAD_W - cx * cell), cell);
                }
            }
            PixelArt.drawText(g, "FINISH", roadX + ROAD_W / 2, sy - 58, 3, Color.WHITE, 0);
        }
    }

    private void drawExplosions(Graphics2D g, int roadX, int anchorY, long nowMillis) {
        explosions.removeIf(e -> nowMillis - e.startMillis() > EXPLOSION_MS);
        for (Explosion e : explosions) {
            long age = nowMillis - e.startMillis();
            BufferedImage img = age < 60 || age > 500 ? PixelArt.EXPLOSION_SMALL : PixelArt.EXPLOSION_BIG;
            int cx = roadX + e.lane() * LANE_W + LANE_W / 2;
            int cy = screenY(e.worldM(), anchorY);
            g.drawImage(img, cx - img.getWidth() / 2, cy - img.getHeight() / 2, null);
        }
    }

    /** Cây, bụi, ao, nhà hai bên đường; chọn theo hàm băm vị trí nên cố định, không nhấp nháy. */
    private void drawDecor(Graphics2D g, int x, int roadX, int y, int h, int anchorY) {
        int leftW = roadX - KERB_W - x;
        double bottomM = renderDist - (y + h - anchorY) / PPM - 20;
        double topM = renderDist + (anchorY - y) / PPM;
        long first = (long) Math.floor(bottomM / DECOR_SLOT_M);
        long last = (long) Math.ceil(topM / DECOR_SLOT_M);
        for (long slot = first; slot <= last; slot++) {
            for (int side = 0; side < 2; side++) {
                int hash = hash(slot, side);
                int kind = Math.floorMod(hash, 100);
                BufferedImage img;
                if (kind < 36) {
                    img = PixelArt.PINE_TREE;
                } else if (kind < 52) {
                    img = PixelArt.ROUND_BUSH;
                } else if (kind < 56) {
                    img = PixelArt.SMALL_POND;
                } else if (kind < 63) {
                    img = PixelArt.RED_HOUSE;
                } else {
                    continue;
                }
                int room = Math.max(1, leftW - img.getWidth() - 12);
                int off = 6 + Math.floorMod(hash >>> 8, room);
                int sx = side == 0 ? roadX - KERB_W - img.getWidth() - off
                        :roadX + ROAD_W + KERB_W + off;
                double m = slot * DECOR_SLOT_M + Math.floorMod(hash >>> 16, 8);
                g.drawImage(img, sx, screenY(m, anchorY) - img.getHeight(), null);
            }
        }
    }

    private int hash(long slot, int side) {
        long v = slot * 0x9E3779B97F4A7C15L + side * 0xC2B2AE3D27D4EB4FL + decorSeed;
        v ^= v >>> 31;
        v *= 0xBF58476D1CE4E5B9L;
        v ^= v >>> 29;
        return (int) v;
    }

    private int screenY(double worldM, int anchorY) {
        return anchorY - (int) Math.round((worldM - renderDist) * PPM);
    }

    /** Lát ảnh theo chiều dọc, neo theo toạ độ thế giới để cuộn cùng tốc độ với đường. */
    private static void tileVertical(Graphics2D g, BufferedImage tile, int x, int y, int w, int h,
                                     int anchorY, double camPx) {
        int th = tile.getHeight();
        int tw = tile.getWidth();
        double topWorld = camPx + (anchorY - y);
        double bottomWorld = camPx - (y + h - anchorY);
        long kFirst = (long) Math.floor(bottomWorld / th);
        long kLast = (long) Math.floor(topWorld / th);
        for (long k = kFirst; k <= kLast; k++) {
            int sy = anchorY - (int) Math.round((k + 1) * th - camPx);
            for (int sx = x; sx < x + w; sx += tw) {
                g.drawImage(tile, sx, sy, Math.min(tw, x + w - sx) + sx, sy + th, 0, 0, Math.min(tw, x + w - sx), th, null);
            }
        }
    }

    /** Ảnh nền có lốm đốm hai màu, lặp liền mạch theo chiều dọc. */
    private static BufferedImage tile(int w, int h, Color base, Color dark, Color light, long seed) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(base);
        g.fillRect(0, 0, w, h);
        Random rnd = new Random(seed);
        for (int i = 0; i < w * h / 40; i++) {
            g.setColor(rnd.nextBoolean() ? dark : light);
            g.fillRect(rnd.nextInt(w / 3) * 3, rnd.nextInt(h / 3) * 3, 3, 3);
        }
        g.dispose();
        return img;
    }
}
