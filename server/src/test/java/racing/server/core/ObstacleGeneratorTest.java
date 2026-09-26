package racing.server.core;

import org.junit.jupiter.api.Test;
import racing.common.GameConfig;
import racing.common.dto.Obstacle;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObstacleGeneratorTest {

    @Test
    void sameSeedSameTraffic() {
        assertEquals(ObstacleGenerator.generate(42), ObstacleGenerator.generate(42));
        assertNotEquals(ObstacleGenerator.generate(42), ObstacleGenerator.generate(43));
    }

    @Test
    void eachLaneHasOneSpeedAndCarsKeepTheirGap() {
        for (long seed = 0; seed < 200; seed++) {
            List<Obstacle> list = ObstacleGenerator.generate(seed);
            assertEquals(GameConfig.OBSTACLE_COUNT, list.size());
            Set<Double> laneSpeeds = new HashSet<>();
            for (int lane = 0; lane < GameConfig.LANES; lane++) {
                final int l = lane;
                List<Obstacle> inLane = list.stream().filter(o -> o.lane() == l).toList();
                assertEquals(GameConfig.OBSTACLE_COUNT / GameConfig.LANES, inLane.size());
                Set<Double> speeds = new HashSet<>();
                inLane.forEach(o -> speeds.add(o.speed()));
                assertEquals(1, speeds.size(), "một làn một tốc độ, seed " + seed);
                laneSpeeds.addAll(speeds);
                for (int i = 1; i < inLane.size(); i++) {
                    double gap = inLane.get(i).position() - inLane.get(i - 1).position();
                    assertTrue(gap >= ObstacleGenerator.MIN_GAP, "khoảng cách cùng làn " + gap + ", seed " + seed);
                }
            }
            assertEquals(new HashSet<>(GameConfig.TRAFFIC_SPEEDS), laneSpeeds, "ba làn ba tốc độ khác nhau");
            for (Obstacle o : list) {
                assertTrue(o.speed() > 0 && o.speed() < GameConfig.MAX_SPEED);
                assertTrue(o.kind() >= 0 && o.kind() < GameConfig.TRAFFIC_KINDS);
                assertTrue(o.position() >= ObstacleGenerator.FIRST && o.position() <= GameConfig.TRACK_LENGTH);
            }
        }
    }
}
