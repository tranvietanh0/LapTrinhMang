package racing.client.ui.theme;

import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * Shared UI motion: easing curves and a one-shot {@link Tween} on the EDT. Every animated widget
 * uses this instead of its own ad-hoc Timer so durations and curves stay consistent.
 * A tween stops by itself when done or when its owner leaves the window, so nothing ticks off-screen.
 */
public final class Motion {
    /** ~60 FPS frame step for UI tweens. */
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

    /** One-shot animation; restartable with {@link #start()}. The final frame always receives exactly 1. */
    public static final class Tween {
        private final JComponent owner;
        private final int ms;
        private final Easing easing;
        private final Frame frame;
        private final Timer timer;
        private long startNanos;
        private Runnable onDone;

        Tween(JComponent owner, int ms, Easing easing, Frame frame) {
            this.owner = owner;
            this.ms = Math.max(1, ms);
            this.easing = easing;
            this.frame = frame;
            this.timer = new Timer(FRAME_MS, e -> step());
            this.timer.setCoalesce(true);
        }

        public Tween then(Runnable done) { this.onDone = done; return this; }

        public void start() {
            startNanos = System.nanoTime();
            frame.at(easing.apply(0f));
            timer.restart();
        }

        public void stop() { timer.stop(); }

        public boolean running() { return timer.isRunning(); }

        private void step() {
            if (owner != null && !owner.isDisplayable()) { timer.stop(); return; }
            float t = Math.min(1f, (System.nanoTime() - startNanos) / 1_000_000f / ms);
            frame.at(t >= 1f ? 1f : easing.apply(t));
            if (t >= 1f) {
                timer.stop();
                if (onDone != null) onDone.run();
            }
        }
    }
}
