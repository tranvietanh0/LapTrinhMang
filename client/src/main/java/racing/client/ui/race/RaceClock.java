package racing.client.ui.race;

import racing.common.GameConfig;

/** A monotonic visual clock that absorbs arrival jitter without changing the simulation tick. */
final class RaceClock {
    private long latestTick;
    private long receivedAt;
    private long sampledAt;
    private double visualTick;
    private boolean initialized;

    void reset() { initialized=false; latestTick=0; visualTick=0; receivedAt=0; sampledAt=0; }
    void accept(long tick,long now) {
        if(!initialized) { initialized=true; visualTick=tick; sampledAt=now; }
        if(tick<latestTick) return;
        latestTick=tick; receivedAt=now;
    }
    double sample(long now) {
        if(!initialized) return 0;
        double elapsed=Math.max(0,(now-sampledAt)/1e9);
        sampledAt=now;
        double ahead=Math.max(0,Math.min(3,(now-receivedAt)/(GameConfig.TICK_MS*1e6)));
        double target=latestTick+ahead;
        double projected=visualTick+elapsed*1000/GameConfig.TICK_MS;
        double corrected=projected+(target-projected)*(1-Math.exp(-elapsed*5));
        visualTick=Math.max(visualTick,Math.min(latestTick+3,corrected));
        return visualTick;
    }
}
