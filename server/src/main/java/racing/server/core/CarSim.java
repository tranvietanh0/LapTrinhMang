package racing.server.core;

import racing.common.GameConfig;
import racing.common.dto.CarSnapshot;
import racing.common.dto.CarState;
import racing.common.dto.Obstacle;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Trọng tài cho một xe. Server là nguồn sự thật: client chỉ gửi tốc độ và làn, quãng
 * đường do server tích phân mỗi tick từ tốc độ đã cắt ngưỡng (nên gửi 999 km/h hay
 * quãng đường giả đều vô nghĩa, T17). Không dùng đồng hồ hệ thống: mọi hàm nhận
 * {@code now} (mili giây) để test chạy tick-by-tick.
 */
public final class CarSim {

    /** Quãng đường xe đi được trong một tick ở tốc độ tối đa (m). */
    public static final double MAX_STEP = GameConfig.MAX_SPEED * GameConfig.KMH_TO_MS * GameConfig.TICK_MS / 1000.0;

    private final String username;
    private double distance;
    private int lane;
    private double speed;
    private long stunUntil;
    private boolean finished;
    private long finishTick = -1;
    private final Set<Integer> hitObstacles = new HashSet<>();

    public CarSim(String username, int startLane) {
        this.username = username;
        reset(startLane);
    }

    public void reset(int startLane) {
        distance = 0;
        lane = clampLane(startLane);
        speed = 0;
        stunUntil = 0;
        finished = false;
        finishTick = -1;
        hitObstacles.clear();
    }

    /** Nhận CAR_STATE từ client: làn và tốc độ (cắt ngưỡng); quãng đường của client bị bỏ qua. */
    public void applyClient(CarState s, long now) {
        if (s == null || finished) {
            return;
        }
        lane = clampLane(s.lane());
        if (now < stunUntil) {
            return;                                   // đang stun: tốc độ vẫn 0
        }
        speed = Math.max(0, Math.min(GameConfig.MAX_SPEED, s.speed()));
    }

    /**
     * Một tick của server: tiến xe, xét về đích, xét va chạm.
     *
     * @return chướng ngại vật vừa đâm phải, hoặc null
     */
    public Obstacle advance(long now, long tick, List<Obstacle> obstacles) {
        if (finished) {
            return null;
        }
        if (now < stunUntil) {
            speed = 0;
            return null;
        }
        distance += speed * GameConfig.KMH_TO_MS * GameConfig.TICK_MS / 1000.0;
        if (distance >= GameConfig.TRACK_LENGTH) {
            distance = GameConfig.TRACK_LENGTH;
            finished = true;
            finishTick = tick;
            return null;
        }
        for (int i = 0; i < obstacles.size(); i++) {
            Obstacle o = obstacles.get(i);
            if (o.lane() == lane && !hitObstacles.contains(i) && overlaps(o.positionAt(tick))) {
                hitObstacles.add(i);
                stunUntil = now + GameConfig.COLLISION_STUN_MS;
                speed = 0;
                return o;
            }
        }
        return null;
    }

    /** Xe mình [distance, distance + CAR_LENGTH) chồng lên xe cộ đang ở {@code obsStart}. */
    private boolean overlaps(double obsStart) {
        double carEnd = distance + GameConfig.CAR_LENGTH;
        double obsEnd = obsStart + GameConfig.OBSTACLE_LENGTH;
        return distance < obsEnd && obsStart < carEnd;
    }

    public CarSnapshot snapshot(long now) {
        return new CarSnapshot(username, distance, lane, speed, now < stunUntil, finished);
    }

    public String username() {
        return username;
    }

    public double distance() {
        return distance;
    }

    public int lane() {
        return lane;
    }

    public double speed() {
        return speed;
    }

    public boolean finished() {
        return finished;
    }

    public long finishTick() {
        return finishTick;
    }

    public boolean stunned(long now) {
        return now < stunUntil;
    }

    private static int clampLane(int l) {
        return Math.max(0, Math.min(GameConfig.LANES - 1, l));
    }
}
