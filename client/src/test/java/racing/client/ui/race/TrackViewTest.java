package racing.client.ui.race;

import org.junit.jupiter.api.Test;
import racing.common.dto.Obstacle;
import racing.common.dto.CarSnapshot;
import racing.client.model.CarModel;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrackViewTest {

    @Test
    void cameraAbsorbsSnapshotStepsInsteadOfSnappingEveryTick() {
        CarModel car=new CarModel("alice",1);
        car.applyServer(new CarSnapshot("alice",100,1,300,false,false));
        TrackView view=new TrackView(car,PixelArt.PLAYER,17);
        view.resetRound();
        view.update(1_000_000_000L);
        car.applyServer(new CarSnapshot("alice",106,1,300,false,false));
        view.update(1_016_000_000L);
        assertTrue(view.renderDist()>100 && view.renderDist()<106,"camera must smooth the 20 Hz position step");
    }

    @Test
    void trafficPositionMatchesServerFormulaAtWholeTicks() {
        Obstacle o = new Obstacle(2, 150, 100, 3);
        for (long tick : new long[] {0, 1, 20, 347}) {
            assertEquals(o.positionAt(tick), TrackView.trafficPos(o, tick), 1e-9);
        }
    }

    @Test
    void everyTrafficKindHasSpriteOfCarLength() {
        for (int kind = -1; kind <= 6; kind++) {
            assertNotNull(PixelArt.traffic(kind));
            assertEquals(PixelArt.PLAYER.getHeight(), PixelArt.traffic(kind).getHeight());
        }
    }

    @Test
    void spriteGridRejectsRaggedRowsAndUnknownColours() {
        assertThrows(IllegalArgumentException.class,
                () -> PixelArt.build(new String[] {"ab", "a"}, 1, Map.of('a', java.awt.Color.RED, 'b', java.awt.Color.RED)));
        assertThrows(IllegalArgumentException.class,
                () -> PixelArt.build(new String[] {"az"}, 1, Map.of('a', java.awt.Color.RED)));
    }
}
