package racing.common;

import java.util.List;

/**
 * Hằng số dùng chung cho client và server. Đây là "hợp đồng chung" của nhóm:
 * đổi giá trị ở đây phải báo cả nhóm vì ảnh hưởng cả ba module.
 */
public final class GameConfig {

    private GameConfig() {
    }

    /** Cổng TCP server lắng nghe. */
    public static final int PORT = 5000;

    /** Chiều dài đường đua (mét). Xe về đích khi distance >= TRACK_LENGTH. */
    public static final double TRACK_LENGTH = 2500.0;

    /** Số làn trên mỗi đường đua (0, 1, 2). */
    public static final int LANES = 3;

    /** Chu kỳ tick của server và client (mili giây). 20 lần / giây. */
    public static final int TICK_MS = 50;

    /** Thời gian chờ phản hồi lời mời thách đấu (giây). */
    public static final int INVITE_TIMEOUT_S = 30;

    /** Không nhận được gì từ client trong khoảng này thì coi là mất kết nối (giây). */
    public static final int DISCONNECT_TIMEOUT_S = 15;

    /** Client gửi PING định kỳ (giây). Phải nhỏ hơn DISCONNECT_TIMEOUT_S. */
    public static final int HEARTBEAT_S = 5;

    /** Số giây đếm ngược trước khi xuất phát. */
    public static final int COUNTDOWN_S = 3;

    /** Tốc độ tối đa (km/h). Server cắt mọi giá trị lớn hơn về ngưỡng này. */
    public static final double MAX_SPEED = 360.0;

    /** Giữ W: tốc độ tăng đều (km/h mỗi giây), từ 0 lên MAX_SPEED trong 3 giây. */
    public static final double ACCEL_KMH_PER_S = 120.0;

    /** Giữ S: tốc độ giảm (km/h mỗi giây). */
    public static final double BRAKE_KMH_PER_S = 300.0;

    /** Không giữ W: xe tự giảm tốc chậm (km/h mỗi giây). */
    public static final double COAST_KMH_PER_S = 40.0;

    /** Sau va chạm tốc độ về 0 trong khoảng này (mili giây). */
    public static final int COLLISION_STUN_MS = 1500;

    /**
     * Số xe cộ (chướng ngại vật) trên mỗi đường đua, chia đều cho các làn. Xe cộ chạy cùng chiều
     * nên cần nhiều hơn vật đứng yên để người chơi luôn phải né.
     */
    public static final int OBSTACLE_COUNT = 48;

    /**
     * Tốc độ (km/h) của xe cộ, mỗi làn nhận một giá trị (xáo theo seed). Cùng làn cùng tốc độ nên
     * xe cộ trong một làn không bao giờ chồng lên nhau. Số phần tử phải bằng LANES, nhỏ hơn MAX_SPEED.
     */
    public static final List<Double> TRAFFIC_SPEEDS = List.of(110.0, 160.0, 210.0);

    /** Số kiểu xe cộ để client chọn hình vẽ (kind 0..TRAFFIC_KINDS-1). */
    public static final int TRAFFIC_KINDS = 6;

    /** Chiều dài (mét) của một chướng ngại vật và của xe, dùng khi xét va chạm. */
    public static final double OBSTACLE_LENGTH = 20.0;
    public static final double CAR_LENGTH = 20.0;

    /** Điểm cộng khi thắng, khi hòa. Thua được 0. */
    public static final int POINTS_WIN = 1;
    public static final int POINTS_DRAW = 1;

    /** Số dòng tối đa của bảng xếp hạng gửi về client. */
    public static final int LEADERBOARD_LIMIT = 100;

    /** Số trận gần nhất trong lịch sử trận gửi về client. */
    public static final int HISTORY_LIMIT = 20;

    /**
     * Quy đổi km/h sang m/s để tính quãng đường đi được trong một tick:
     * distance += speedKmh * KMH_TO_MS * (TICK_MS / 1000.0)
     */
    public static final double KMH_TO_MS = 1000.0 / 3600.0;
}
