package racing.client.model;

import org.junit.jupiter.api.Test;
import racing.common.GameConfig;
import racing.common.dto.CarSnapshot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CarModelTest {

    @Test
    void speedIsClampedBetweenZeroAndMax() {
        CarModel car = new CarModel("alice", 1);
        for (int i = 0; i < 100; i++) {
            car.accelerate();
        }
        assertEquals(GameConfig.MAX_SPEED, car.speed());
        for (int i = 0; i < 100; i++) {
            car.brake();
        }
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
        for (int i = 0; i < 20; i++) {
            car.accelerate();          // 200 km/h
        }
        car.advance(GameConfig.TICK_MS / 1000.0);
        double expected = GameConfig.MAX_SPEED * GameConfig.KMH_TO_MS * GameConfig.TICK_MS / 1000.0;
        assertEquals(expected, car.distance(), 1e-9);
        assertFalse(car.finished());

        for (int i = 0; i < 100_000 && !car.finished(); i++) {
            car.advance(0.05);
        }
        assertTrue(car.finished());
        assertEquals(GameConfig.TRACK_LENGTH, car.distance());
        car.accelerate();
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
        assertEquals(13, car.progressPercent());
    }

    @Test
    void stunnedCarDoesNotMoveOrAccelerate() {
        CarModel car = new CarModel("alice", 1);
        car.applyServer(new CarSnapshot("alice", 50, 1, 0, true, false));
        car.accelerate();
        car.advance(1);
        assertEquals(0, car.speed());
        assertEquals(50, car.distance());
    }
}
