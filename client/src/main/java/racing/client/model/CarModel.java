package racing.client.model;

import racing.common.GameConfig;
import racing.common.dto.CarSnapshot;
import racing.common.dto.CarState;

/**
 * Trạng thái một xe phía client. Xe của mình được dự đoán cục bộ mỗi tick rồi hiệu chỉnh theo
 * server ({@link #applyServer}); xe đối thủ chỉ nhận từ server.
 */
public final class CarModel {

    private final String username;
    private double distance;
    private int lane;
    private double speed;
    private boolean stunned;
    private boolean finished;
    private boolean throttle;   // đang giữ W / ↑
    private boolean braking;    // đang giữ S / ↓

    public CarModel(String username, int startLane) {
        this.username = username;
        this.lane = clampLane(startLane);
    }

    // ---- điều khiển (chỉ dùng cho xe của mình)

    /** Giữ / nhả phím tăng tốc. Tốc độ đổi dần trong {@link #advance}, không theo số lần lặp phím. */
    public void setThrottle(boolean on) {
        throttle = on;
    }

    public void setBrake(boolean on) {
        braking = on;
    }

    public void laneLeft() {
        if (!finished) {
            lane = clampLane(lane - 1);
        }
    }

    public void laneRight() {
        if (!finished) {
            lane = clampLane(lane + 1);
        }
    }

    /** Dự đoán cục bộ: đổi tốc độ theo phím đang giữ rồi tăng quãng đường trong dtSeconds. */
    public void advance(double dtSeconds) {
        if (finished || stunned) {
            return;
        }
        double rate = braking ? -GameConfig.BRAKE_KMH_PER_S
                : throttle ? GameConfig.ACCEL_KMH_PER_S
                : -GameConfig.COAST_KMH_PER_S;
        speed = Math.max(0, Math.min(GameConfig.MAX_SPEED, speed + rate * dtSeconds));
        distance = Math.min(GameConfig.TRACK_LENGTH, distance + speed * GameConfig.KMH_TO_MS * dtSeconds);
        if (distance >= GameConfig.TRACK_LENGTH) {
            finished = true;
        }
    }

    /** Ghi đè bằng trạng thái server xác nhận. Trả về true nếu server báo vừa bị va chạm (để nháy đỏ). */
    public boolean applyServer(CarSnapshot s) {
        boolean newlyStunned = s.stunned() && !stunned;
        distance = s.distance();
        lane = clampLane(s.lane());
        speed = s.speed();
        stunned = s.stunned();
        finished = s.finished();
        return newlyStunned;
    }

    public void reset(int startLane) {
        distance = 0;
        lane = clampLane(startLane);
        speed = 0;
        stunned = false;
        finished = false;
        throttle = false;
        braking = false;
    }

    public CarState toState() {
        return new CarState(distance, lane, speed);
    }

    private static int clampLane(int l) {
        return Math.max(0, Math.min(GameConfig.LANES - 1, l));
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

    public boolean stunned() {
        return stunned;
    }

    public boolean finished() {
        return finished;
    }

    /** Phần trăm quãng đường đã đi, 0..100. */
    public int progressPercent() {
        return (int) Math.round(100.0 * distance / GameConfig.TRACK_LENGTH);
    }
}
