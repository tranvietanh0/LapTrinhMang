package racing.client.ui.theme;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Shared UI motion: easing curves, a one-shot {@link Tween} and a continuous {@link Loop}, all stepped
 * on the EDT by ONE frame pulse. The pulse runs on fixed deadlines because a repeating 16 ms Swing
 * Timer fires about every 31 ms on Windows (same fix as RacePanel's frame clock); sharing one pulse
 * also keeps staggered animations in step. The pulse thread idles when nothing is animating.
 * A tween stops by itself when done or when its owner leaves the window, so nothing ticks off-screen.
 */
public final class Motion {
    /** ~60 FPS frame step for UI animation. */
    public static final int FRAME_MS = 16;
    /** Standard durations (ms): quick feedback, panel entrance, emphasis. */
    public static final int FAST = 160, NORMAL = 320, SLOW = 600;

    private Motion() { }

    @FunctionalInterface
    public interface Easing { float apply(float t); }

    @FunctionalInterface
    public interface Frame { void at(float eased); }

    public static final Easing LINEAR = t -> t;
    public static final Easing OUT_CUBIC = t -> 1f - (float) Math.pow(1f - t, 3);
    public static final Easing IN_OUT_SINE = t -> (float) (-(Math.cos(Math.PI * t) - 1) / 2);
    /** Overshoots ~10% then settles: for things that "land" (titles, cards, badges). */
    public static final Easing OUT_BACK = t -> {
        float c1 = 1.70158f, c3 = c1 + 1f, u = t - 1f;
        return 1f + c3 * u * u * u + c1 * u * u;
    };

    /** Starts a tween of {@code ms} on {@code owner}; {@code frame} receives eased progress 0..1. */
    public static Tween play(JComponent owner, int ms, Easing easing, Frame frame) {
        Tween t = new Tween(owner, ms, easing, frame);
        t.start();
        return t;
    }

    /** A not-yet-started loop calling {@code onFrame} once per frame on the EDT until stopped. */
    public static Loop loop(Runnable onFrame) {
        return new Loop(onFrame);
    }

    // ------------------------------------------------------------------ frame pulse (EDT-owned set)

    private static final Set<Runnable> ACTIVE = new LinkedHashSet<>();
    private static final AtomicBoolean POSTED = new AtomicBoolean();
    private static final Runnable RUN_FRAME = Motion::runFrame;
    private static ScheduledExecutorService pulse;
    private static ScheduledFuture<?> pulseTask;

    private static void subscribe(Runnable step) {
        ACTIVE.add(step);
        synchronized (Motion.class) {
            if (pulseTask != null) return;
            if (pulse == null) {
                pulse = Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread t = new Thread(r, "ui-motion-pulse");
                    t.setDaemon(true);
                    return t;
                });
            }
            pulseTask = pulse.scheduleAtFixedRate(() -> {
                // At most one frame queued: a busy EDT drops frames instead of piling them up.
                if (POSTED.compareAndSet(false, true)) SwingUtilities.invokeLater(RUN_FRAME);
            }, FRAME_MS, FRAME_MS, TimeUnit.MILLISECONDS);
        }
    }

    private static void unsubscribe(Runnable step) {
        ACTIVE.remove(step);
        if (!ACTIVE.isEmpty()) return;
        synchronized (Motion.class) {
            if (pulseTask != null) { pulseTask.cancel(false); pulseTask = null; }
        }
    }

    private static void runFrame() {
        POSTED.set(false);
        List<Runnable> steps = new ArrayList<>(ACTIVE);   // steps may (un)subscribe while running
        for (Runnable step : steps) step.run();
    }

    // ------------------------------------------------------------------ animations

    /** One-shot animation; restartable with {@link #start()}. The final frame always receives exactly 1. */
    public static final class Tween {
        private final JComponent owner;
        private final int ms;
        private final Easing easing;
        private final Frame frame;
        private final Runnable step = this::step;
        private boolean running;
        private long startNanos;
        private Runnable onDone;

        Tween(JComponent owner, int ms, Easing easing, Frame frame) {
            this.owner = owner;
            this.ms = Math.max(1, ms);
            this.easing = easing;
            this.frame = frame;
        }

        public Tween then(Runnable done) { this.onDone = done; return this; }

        public void start() {
            startNanos = 0L;   // clock starts on the first pulse, so a stall before it skips nothing
            frame.at(easing.apply(0f));
            running = true;
            subscribe(step);
        }

        public void stop() {
            running = false;
            unsubscribe(step);
        }

        public boolean running() { return running; }

        private void step() {
            if (owner != null && !owner.isDisplayable()) { stop(); return; }
            long now = System.nanoTime();
            if (startNanos == 0L) startNanos = now;
            float t = Math.min(1f, (now - startNanos) / 1_000_000f / ms);
            frame.at(t >= 1f ? 1f : easing.apply(t));
            if (t >= 1f) {
                stop();
                if (onDone != null) onDone.run();
            }
        }
    }

    /** Continuous per-frame callback for ambient motion (backdrop streaks, glow pulses). */
    public static final class Loop {
        private final Runnable step;
        private boolean running;

        Loop(Runnable onFrame) { this.step = onFrame; }

        public void start() {
            if (running) return;
            running = true;
            subscribe(step);
        }

        public void stop() {
            running = false;
            unsubscribe(step);
        }

        public boolean running() { return running; }
    }
}
