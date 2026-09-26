package racing.client.ui.race;

import org.junit.jupiter.api.Test;
import racing.common.dto.Obstacle;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TrackViewTest {

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
