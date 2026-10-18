package racing.client.ui.race;

import racing.client.model.CarModel;
import racing.common.GameConfig;
import racing.common.dto.Obstacle;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Một nửa màn hình đua: camera cuộn dọc bám theo một xe trên phố đêm (vỉa hè, nóc nhà, đèn đường,
 * mặt đường có vạch làn, xe cộ, xe người chơi với đèn pha, vụ nổ, vệt tốc độ). Toạ độ thế giới
 * tính bằng mét dọc đường đua; xe của khung nhìn đứng yên ở {@link #ANCHOR} chiều cao.
 * Mọi lớp tĩnh là ảnh dựng sẵn theo cỡ ({@link RaceSprites}); mỗi khung hình chỉ blit, không cấp phát.
 * Giữ trạng thái hiển thị riêng (quãng đường nội suy, làn trượt mượt, xe cộ đã bị tông).
 */
final class TrackView {

    /** Điểm ảnh logic trên một mét: xe dài 20 m cao 60 px, đúng bằng hình xe. */
    static final double PPM = PixelArt.PLAYER.getHeight() / GameConfig.CAR_LENGTH;
    static final int LANE_W = 84;
    static final int ROAD_W = LANE_W * GameConfig.LANES;
    /** Đuôi xe người chơi nằm ở 88 % chiều cao khung nhìn, thân xe quanh mốc 3/4. */
    private static final double ANCHOR = 0.88;
    private static final long EXPLOSION_MS = 650;
    static final int SKIP = Integer.getInteger("race.skip", 0);

    private record Explosion(double worldM, int lane, long startMillis) {
    }

    private final CarModel car;
    private final boolean rival;
    private final RaceDecor decor;
    private final RaceFx fx = new RaceFx();
    private final Set<Integer> destroyed = new HashSet<>();
    private final List<Explosion> explosions = new ArrayList<>();

    private double renderDist;
    private double lastTarget = -1;
    private long targetStampNanos;
    private double laneX = 1;
    private long lastFrameNanos;

    /** Giữ chữ ký cũ: {@code carSprite == PixelArt.OPPONENT} nghĩa là khung nhìn của đối thủ. */
    TrackView(CarModel car, BufferedImage carSprite, int decorSeed) {
        this(car, carSprite == PixelArt.OPPONENT, decorSeed);
    }

    TrackView(CarModel car, boolean rival, int decorSeed) {
        this.car = car;
        this.rival = rival;
        this.decor = new RaceDecor(decorSeed);
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
        targetStampNanos = 0;
        lastFrameNanos = 0;
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
        // Render the camera continuously rather than stepping to every arriving 20 Hz snapshot.
        // Only resetRound may move the camera backwards.
        if (desired > renderDist) renderDist += (desired-renderDist)*(1-Math.exp(-dt*24));
        laneX += (car.lane() - laneX) * Math.min(1, dt * 14);
        if (Math.abs(car.lane() - laneX) < 0.01) {
            laneX = car.lane();
        }
    }

    /** Cỡ ô 28 m (px) phù hợp với khung nhìn này: vừa chiều cao và chiều rộng, lượng tử theo {@link RaceSprites#STEP}. */
    static int tileSizeFor(int w, int h) {
        double byH = h / 560.0, byW = w / (ROAD_W + 90.0);
        return RaceSprites.stepFor(Math.min(byH, byW));
    }

    /** Vẽ khung nhìn vào vùng (x, y, w, h) với cỡ tự chọn; clip do người gọi đặt. */
    void paint(Graphics2D g, int x, int y, int w, int h, List<Obstacle> obstacles, double tickF, long nowMillis) {
        paint(g, x, y, w, h, obstacles, tickF, nowMillis, tileSizeFor(w, h));
    }

    /** Vẽ khung nhìn vào vùng (x, y, w, h) với ô 28 m = {@code n} px; clip do người gọi đặt. */
    void paint(Graphics2D g, int x, int y, int w, int h, List<Obstacle> obstacles, double tickF, long nowMillis, int n) {
        RaceSprites sp = RaceSprites.at(n);
        double pxPerM = n / 28.0;
        int anchorY = y + (int) (h * ANCHOR);
        double camPx = renderDist * pxPerM;
        int roadX = x + (w - sp.roadW) / 2;
        double bottomM = renderDist - (y + h - anchorY) / pxPerM;
        double topM = renderDist + (anchorY - y) / pxPerM;
        double speedRatio = Math.min(1, car.speed() / GameConfig.MAX_SPEED);

        BufferedImage ground = sp.ground();
        if ((SKIP & 1) == 0) tileVertical(g, ground, x, y, w, h, anchorY, camPx, ground.getWidth());
        if ((SKIP & 2) == 0) decor.drawCity(g, sp, x, w, roadX, y, h, anchorY, renderDist);
        BufferedImage road = sp.road();
        if ((SKIP & 4) == 0) tileVertical(g, road, roadX - sp.kerbW, y, road.getWidth(), h, anchorY, camPx, road.getWidth());
        if ((SKIP & 8) == 0) decor.drawPools(g, sp, roadX, y, h, anchorY, renderDist);
        drawBanners(g, sp, roadX, anchorY, bottomM, topM, pxPerM);

        // xe cộ trong khung nhìn
        int padY = sp.px(RaceCarArt.PAD_Y);
        for (int i = 0; i < obstacles.size(); i++) {
            if (destroyed.contains(i)) {
                continue;
            }
            Obstacle o = obstacles.get(i);
            double p = trafficPos(o, tickF);
            if (p > topM + 6 || p + GameConfig.OBSTACLE_LENGTH < bottomM - 6) {
                continue;
            }
            BufferedImage img = sp.traffic(o.kind());
            int cx = roadX + o.lane() * sp.laneW + sp.laneW / 2;
            g.drawImage(img, cx - img.getWidth() / 2, screenY(p + GameConfig.OBSTACLE_LENGTH, anchorY, pxPerM) - padY, null);
        }

        if ((SKIP & 32) == 0) fx.drawStreaks(g, sp, x, y, w, h, roadX, camPx, speedRatio);

        // xe của khung nhìn, nhấp nháy khi đang bị choáng
        boolean blinkOff = car.stunned() && (nowMillis / 90) % 2 == 0;
        if (!blinkOff) {
            BufferedImage img = sp.racer(rival);
            int cx = roadX + (int) Math.round(laneX * sp.laneW) + sp.laneW / 2;
            int noseY = screenY(renderDist + GameConfig.CAR_LENGTH, anchorY, pxPerM);
            if (!car.stunned() && !car.finished()) {
                BufferedImage beam = sp.beam();
                g.setComposite(RaceGfx.alpha(0.55 + 0.45 * speedRatio));
                g.drawImage(beam, cx - beam.getWidth() / 2, noseY - beam.getHeight() + sp.px(3), null);
                g.setComposite(RaceGfx.ALPHA[16]);
            }
            g.drawImage(img, cx - img.getWidth() / 2, noseY - padY, null);
        }

        drawExplosions(g, sp, roadX, anchorY, pxPerM, nowMillis);
        if ((SKIP & 256) == 0) decor.drawLamps(g, sp, roadX, y, h, anchorY, renderDist);
        if ((SKIP & 128) == 0) fx.drawVignette(g, x, y, w, h);
    }

    private void drawBanners(Graphics2D g, RaceSprites sp, int roadX, int anchorY, double bottomM, double topM, double pxPerM) {
        int left = roadX - sp.kerbW - sp.px(18);
        if (-24 <= topM && 12 >= bottomM) {
            g.drawImage(sp.startBanner(), left, screenY(0, anchorY, pxPerM) - sp.px(RaceScenery.BANNER_LINE_Y), null);
        }
        double fin = GameConfig.TRACK_LENGTH;
        if (fin - 24 <= topM && fin + 12 >= bottomM) {
            g.drawImage(sp.finishBanner(), left, screenY(fin, anchorY, pxPerM) - sp.px(RaceScenery.BANNER_LINE_Y), null);
        }
    }

    private void drawExplosions(Graphics2D g, RaceSprites sp, int roadX, int anchorY, double pxPerM, long nowMillis) {
        for (int i = explosions.size() - 1; i >= 0; i--) {
            if (nowMillis - explosions.get(i).startMillis() > EXPLOSION_MS) {
                explosions.remove(i);
            }
        }
        for (int i = 0; i < explosions.size(); i++) {
            Explosion e = explosions.get(i);
            long age = nowMillis - e.startMillis();
            BufferedImage img = sp.explosion(age >= 60 && age <= 500);
            int cx = roadX + e.lane() * sp.laneW + sp.laneW / 2;
            int cy = screenY(e.worldM(), anchorY, pxPerM);
            g.setComposite(RaceGfx.alpha(1.0 - Math.max(0, age - 350) / (double) (EXPLOSION_MS - 350)));
            g.drawImage(img, cx - img.getWidth() / 2, cy - img.getHeight() / 2, null);
        }
        g.setComposite(RaceGfx.ALPHA[16]);
    }

    private int screenY(double worldM, int anchorY, double pxPerM) {
        return anchorY - (int) Math.round((worldM - renderDist) * pxPerM);
    }

    /** Lát ảnh theo chiều dọc, neo theo toạ độ thế giới để cuộn cùng tốc độ với đường. */
    private static void tileVertical(Graphics2D g, BufferedImage tile, int x, int y, int w, int h,
                                     int anchorY, double camPx, int tw) {
        int th = tile.getHeight();
        double topWorld = camPx + (anchorY - y);
        double bottomWorld = camPx - (y + h - anchorY);
        long kFirst = (long) Math.floor(bottomWorld / th);
        long kLast = (long) Math.floor(topWorld / th);
        for (long k = kFirst; k <= kLast; k++) {
            int sy = anchorY - (int) Math.round((k + 1) * th - camPx);
            for (int sx = x; sx < x + w; sx += tw) {
                g.drawImage(tile, sx, sy, null);
            }
        }
    }
}
