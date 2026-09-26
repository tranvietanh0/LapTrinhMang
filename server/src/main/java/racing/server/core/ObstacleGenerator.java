package racing.server.core;

import racing.common.GameConfig;
import racing.common.dto.Obstacle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Sinh xe cộ cho một ván từ seed. Mỗi làn nhận một tốc độ trong {@link GameConfig#TRAFFIC_SPEEDS}
 * (xáo ngẫu nhiên) và OBSTACLE_COUNT / LANES xe, vị trí lúc GO trải từ FIRST tới cuối đường,
 * hai xe cùng làn cách nhau ít nhất MIN_GAP. Cùng làn cùng tốc độ nên khoảng cách này giữ nguyên
 * suốt ván. Cùng seed cho cùng danh sách, nên hai client nhận đúng một bộ giống nhau.
 */
public final class ObstacleGenerator {

    static final double FIRST = 120.0;
    static final double MIN_GAP = 2 * GameConfig.CAR_LENGTH;

    private ObstacleGenerator() {
    }

    public static List<Obstacle> generate(long seed) {
        if (GameConfig.TRAFFIC_SPEEDS.size() != GameConfig.LANES) {
            throw new IllegalStateException("TRAFFIC_SPEEDS phải có đúng LANES phần tử");
        }
        Random rnd = new Random(seed);
        List<Double> speeds = new ArrayList<>(GameConfig.TRAFFIC_SPEEDS);
        Collections.shuffle(speeds, rnd);

        int perLane = GameConfig.OBSTACLE_COUNT / GameConfig.LANES;
        double slot = (GameConfig.TRACK_LENGTH - FIRST) / perLane;   // ~147 m với cấu hình mặc định
        double jitter = Math.max(0, slot - MIN_GAP);                  // lệch trong ô, vẫn cách nhau ≥ MIN_GAP
        List<Obstacle> list = new ArrayList<>(perLane * GameConfig.LANES);
        for (int lane = 0; lane < GameConfig.LANES; lane++) {
            for (int i = 0; i < perLane; i++) {
                double position = Math.round(FIRST + i * slot + rnd.nextDouble() * jitter);
                list.add(new Obstacle(lane, position, speeds.get(lane), rnd.nextInt(GameConfig.TRAFFIC_KINDS)));
            }
        }
        list.sort(Comparator.comparingDouble(Obstacle::position));
        return list;
    }
}
