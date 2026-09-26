package racing.client.model;

import org.junit.jupiter.api.Test;
import racing.common.GameConfig;
import racing.common.dto.CarSnapshot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CarModelTest {

    private static void run(CarModel car, double seconds) {
        for (int i = 0; i < Math.round(seconds * 1000 / GameConfig.TICK_MS); i++) {
            car.advance(GameConfig.TICK_MS / 1000.0);
        }
    }

    @Test
    void holdingThrottleReachesMaxInAboutThreeSecondsAndIsClamped() {
        CarModel car = new CarModel("alice", 1);
        car.setThrottle(true);
        run(car, 1);
        assertEquals(GameConfig.ACCEL_KMH_PER_S, car.speed(), 1e-6);
        run(car, 10);
        assertEquals(GameConfig.MAX_SPEED, car.speed());
    }

    @Test
    void releasingThrottleCoastsAndBrakeStopsAtZero() {
        CarModel car = new CarModel("alice", 1);
        car.setThrottle(true);
        run(car, 10);
        car.setThrottle(false);
        run(car, 1);
        assertEquals(GameConfig.MAX_SPEED - GameConfig.COAST_KMH_PER_S, car.speed(), 1e-6);
        car.setBrake(true);
        run(car, 10);
        assertEquals(0, car.speed());
    }

    @Test
    void laneStaysInsideTrack() {
        CarModel car = new CarModel("alice", 1);
        car.laneLeft();
        car.laneLeft();
        assertEquals(0, car.lane());
        for (int i = 0; i < 5; i++) {
            car.laneRight();
        }
        assertEquals(GameConfig.LANES - 1, car.lane());
        assertEquals(GameConfig.LANES - 1, new CarModel("x", 99).lane());
    }

    @Test
    void advanceUsesSpeedAndTickAndStopsAtFinish() {
        CarModel car = new CarModel("alice", 1);
        car.setThrottle(true);
        run(car, 10);                  // đạt MAX_SPEED
        double before = car.distance();
        car.advance(GameConfig.TICK_MS / 1000.0);
        double expected = GameConfig.MAX_SPEED * GameConfig.KMH_TO_MS * GameConfig.TICK_MS / 1000.0;
        assertEquals(expected, car.distance() - before, 1e-9);
        assertFalse(car.finished());

        for (int i = 0; i < 100_000 && !car.finished(); i++) {
            car.advance(0.05);
        }
        assertTrue(car.finished());
        assertEquals(GameConfig.TRACK_LENGTH, car.distance());
        car.advance(1);
        assertEquals(GameConfig.TRACK_LENGTH, car.distance(), "không đi tiếp sau khi về đích");
    }

    @Test
    void applyServerOverridesAndReportsNewStun() {
        CarModel car = new CarModel("alice", 1);
        assertTrue(car.applyServer(new CarSnapshot("alice", 120.5, 2, 0, true, false)));
        assertEquals(120.5, car.distance());
        assertEquals(2, car.lane());
        assertTrue(car.stunned());
        assertFalse(car.applyServer(new CarSnapshot("alice", 121, 2, 0, true, false)), "đang choáng sẵn thì không báo lại");
        assertFalse(car.applyServer(new CarSnapshot("alice", 130, 2, 90, false, false)));
        car.advance(0.05);
        assertTrue(car.distance() > 130);
        assertEquals(5, car.progressPercent());   // ~130 m / 2500 m
    }

    @Test
    void stunnedCarDoesNotMoveOrAccelerate() {
        CarModel car = new CarModel("alice", 1);
        car.applyServer(new CarSnapshot("alice", 50, 1, 0, true, false));
        car.setThrottle(true);
        car.advance(1);
        assertEquals(0, car.speed());
        assertEquals(50, car.distance());
    }
}
