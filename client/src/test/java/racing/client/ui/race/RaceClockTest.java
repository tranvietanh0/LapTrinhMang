package racing.client.ui.race;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RaceClockTest {
    @Test void delayedPacketsNeverMoveTrafficBackwards() {
        RaceClock clock=new RaceClock();
        clock.accept(0,1_000_000_000L);
        double before=clock.sample(1_080_000_000L);
        clock.accept(1,1_080_000_000L);
        double after=clock.sample(1_081_000_000L);
        assertTrue(after>=before);
        assertTrue(after-before<0.1,"arrival correction should be gradual");
    }
    @Test void stopsAfterThreeTicksWhenNetworkStallsAndResetsForNewRound() {
        RaceClock clock=new RaceClock(); clock.accept(40,1_000_000_000L);
        assertEquals(43,clock.sample(2_000_000_000L),1e-9);
        assertEquals(43,clock.sample(3_000_000_000L),1e-9);
        clock.reset(); assertEquals(0,clock.sample(4_000_000_000L));
        clock.accept(0,4_000_000_000L); assertEquals(1,clock.sample(4_050_000_000L),1e-9);
    }
    @Test void steadyPacketsAdvanceAtSimulationRate() {
        RaceClock clock=new RaceClock(); long start=1_000_000_000L; clock.accept(0,start);
        for(int tick=1;tick<=100;tick++) {
            long now=start+tick*50_000_000L;
            clock.accept(tick,now);
            assertEquals(tick,clock.sample(now),1e-8);
        }
    }
}
