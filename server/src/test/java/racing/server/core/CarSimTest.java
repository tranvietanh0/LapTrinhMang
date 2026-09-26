package racing.server.core;

import org.junit.jupiter.api.Test;
import racing.common.GameConfig;
import racing.common.dto.CarState;
import racing.common.dto.Obstacle;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CarSimTest {

    private static final List<Obstacle> NONE = List.of();

    @Test
    void speedAboveMaxIsClampedTo200() {
        CarSim car = new CarSim("alice", 1);
        car.applyClient(new CarState(0, 1, 999), 0);
        assertEquals(GameConfig.MAX_SPEED, car.speed());
        car.applyClient(new CarState(0, 1, -50), 0);
        assertEquals(0, car.speed());
    }

    @Test
    void laneIsClampedToTrack() {
        CarSim car = new CarSim("alice", 1);
        car.applyClient(new CarState(0, 7, 10), 0);
        assertEquals(GameConfig.LANES - 1, car.lane());
        car.applyClient(new CarState(0, -3, 10), 0);
        assertEquals(0, car.lane());
    }

    @Test
    void clientDistanceIsIgnoredServerIntegratesFromSpeed() {
        CarSim car = new CarSim("alice", 1);
        car.applyClient(new CarState(900, 1, 999), 0);      // báo đã đi 900 m với 999 km/h
        car.advance(50, 1, NONE);
        assertEquals(CarSim.MAX_STEP, car.distance(), 1e-9);  // chỉ tiến đúng một bước ở 200 km/h
        assertFalse(car.finished());
    }

    @Test
    void reachesFinishAfterExpectedTicks() {
        CarSim car = new CarSim("alice", 1);
        car.applyClient(new CarState(0, 1, 200), 0);
        long now = 0;
        long tick = 0;
        while (!car.finished()) {
            now += GameConfig.TICK_MS;
            car.advance(now, ++tick, NONE);
        }
        assertEquals((long) Math.ceil(GameConfig.TRACK_LENGTH / CarSim.MAX_STEP), tick);
        assertEquals(GameConfig.TRACK_LENGTH, car.distance());
        assertEquals(tick, car.finishTick());
    }

    @Test
    void collisionStunsForOneSecondAndOnlyOncePerObstacle() {
        CarSim car = new CarSim("alice", 0);
        List<Obstacle> obs = List.of(new Obstacle(0, 30));
        car.applyClient(new CarState(0, 0, 200), 0);
        long now = 0;
        long tick = 0;
        Obstacle hit = null;
        while (hit == null && tick < 100) {
            now += GameConfig.TICK_MS;
            hit = car.advance(now, ++tick, obs);
        }
        assertNotNull(hit, "phải đâm chướng ngại vật ở làn 0");
        assertEquals(0, car.speed());
        assertTrue(car.stunned(now));
        double stuckAt = car.distance();

        // trong 1 s: tốc độ client bị bỏ qua, xe đứng yên
        car.applyClient(new CarState(500, 0, 200), now + 10);
        for (int i = 0; i < GameConfig.COLLISION_STUN_MS / GameConfig.TICK_MS - 1; i++) {
            now += GameConfig.TICK_MS;
            assertNull(car.advance(now, ++tick, obs));
            assertEquals(stuckAt, car.distance());
        }
        now += GameConfig.COLLISION_STUN_MS;
        assertFalse(car.stunned(now));

        // hết stun, nhấn ga lại: đi tiếp qua chính chướng ngại vật đó mà không bị đâm lần hai
        car.applyClient(new CarState(0, 0, 200), now);
        for (int i = 0; i < 40; i++) {
            now += GameConfig.TICK_MS;
            assertNull(car.advance(now, ++tick, obs), "chướng ngại vật chỉ gây va chạm một lần");
        }
        assertTrue(car.distance() > stuckAt + 50);
    }

    @Test
    void movingTrafficIsHitAtItsMovedPositionNotItsStartPosition() {
        // xe cộ làn 0 bắt đầu ở 30 m, chạy 100 km/h; xe mình đứng yên 40 tick (2 s) ở cùng làn
        Obstacle traffic = new Obstacle(0, 30, 100, 0);
        List<Obstacle> obs = List.of(traffic);
        CarSim car = new CarSim("alice", 0);
        long now = 0;
        long tick = 0;
        for (int i = 0; i < 40; i++) {
            now += GameConfig.TICK_MS;
            assertNull(car.advance(now, ++tick, obs));
        }
        assertTrue(traffic.positionAt(tick) > 80, "xe cộ đã chạy khỏi vị trí 30 m");

        // tăng tốc 200 km/h: chạy xuyên qua vùng 30..50 m (vị trí lúc GO) mà không va chạm
        car.applyClient(new CarState(0, 0, 200), now);
        Obstacle hit = null;
        while (hit == null && car.distance() < 60) {
            now += GameConfig.TICK_MS;
            hit = car.advance(now, ++tick, obs);
        }
        assertNull(hit, "không va chạm ở vị trí lúc GO của xe cộ");

        // đuổi kịp (200 > 100 km/h): va chạm xảy ra đúng lúc hai xe chồng nhau ở vị trí đã di chuyển
        while (hit == null && tick < 400) {
            now += GameConfig.TICK_MS;
            hit = car.advance(now, ++tick, obs);
        }
        assertNotNull(hit, "phải đuổi kịp và đâm xe cộ");
        double at = traffic.positionAt(tick);
        assertTrue(car.distance() < at + GameConfig.OBSTACLE_LENGTH && at < car.distance() + GameConfig.CAR_LENGTH);
        assertTrue(at > 100, "va chạm ở vị trí đã di chuyển, không phải 30 m");
    }

    @Test
    void noCollisionOnDifferentLane() {
        CarSim car = new CarSim("alice", 2);
        List<Obstacle> obs = List.of(new Obstacle(0, 30), new Obstacle(1, 60));
        car.applyClient(new CarState(0, 2, 200), 0);
        long now = 0;
        for (long tick = 1; tick <= 60; tick++) {
            now += GameConfig.TICK_MS;
            assertNull(car.advance(now, tick, obs));
        }
    }

    @Test
    void resetClearsEverything() {
        CarSim car = new CarSim("alice", 0);
        car.applyClient(new CarState(0, 0, 200), 0);
        car.advance(50, 1, List.of(new Obstacle(0, 0)));
        car.reset(1);
        assertEquals(0, car.distance());
        assertEquals(1, car.lane());
        assertEquals(0, car.speed());
        assertFalse(car.stunned(50));
        assertFalse(car.finished());
    }
}
