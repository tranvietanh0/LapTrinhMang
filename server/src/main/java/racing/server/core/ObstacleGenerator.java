package racing.server.core;

import racing.common.GameConfig;
import racing.common.dto.Obstacle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Sinh chướng ngại vật cho một ván từ seed: OBSTACLE_COUNT vật, vị trí tăng dần trong
 * [120, TRACK_LENGTH - 80], cách nhau ít nhất 60 m, làn ngẫu nhiên. Cùng seed cho cùng
 * danh sách, nên hai client nhận đúng một bộ giống nhau.
 */
public final class ObstacleGenerator {

    private static final double FIRST = 120.0;
    private static final double LAST = GameConfig.TRACK_LENGTH - 80.0;

    private ObstacleGenerator() {
    }

    public static List<Obstacle> generate(long seed) {
        Random rnd = new Random(seed);
        int count = GameConfig.OBSTACLE_COUNT;
        double slot = (LAST - FIRST) / count;                 // 100 m mỗi ô với cấu hình mặc định
        double jitter = Math.max(0, slot - 60.0);             // ô 100 m, lệch tối đa 40 m → cách nhau ≥ 60 m
        List<Obstacle> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double position = FIRST + i * slot + rnd.nextDouble() * jitter;
            list.add(new Obstacle(rnd.nextInt(GameConfig.LANES), Math.round(position)));
        }
        return list;
    }
}
