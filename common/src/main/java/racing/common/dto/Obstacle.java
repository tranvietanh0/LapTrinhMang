package racing.common.dto;

import racing.common.GameConfig;

import java.io.Serializable;

/**
 * Một xe cộ (chướng ngại vật) trên đường đua, chạy cùng chiều với tốc độ không đổi. Hai đường
 * đua nhận cùng danh sách nên giống nhau. Vị trí tại một tick tính bằng {@link #positionAt(long)}
 * ở cả server (xét va chạm) và client (vẽ), nên hai bên luôn khớp.
 *
 * @param lane     làn 0..GameConfig.LANES-1
 * @param position vị trí đầu xe lúc GO (tick 0), tính từ vạch xuất phát (mét)
 * @param speed    tốc độ chạy (km/h); 0 = đứng yên
 * @param kind     kiểu xe để client chọn hình vẽ (không ảnh hưởng luật)
 */
public record Obstacle(int lane, double position, double speed, int kind) implements Serializable {
    private static final long serialVersionUID = 2L;

    /** Vật đứng yên, kiểu 0. */
    public Obstacle(int lane, double position) {
        this(lane, position, 0, 0);
    }

    /** Vị trí đầu xe (mét) sau {@code tick} tick kể từ GO. */
    public double positionAt(long tick) {
        return position + speed * GameConfig.KMH_TO_MS * tick * GameConfig.TICK_MS / 1000.0;
    }
}
