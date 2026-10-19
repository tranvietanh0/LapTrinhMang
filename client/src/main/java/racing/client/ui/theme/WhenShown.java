package racing.client.ui.theme;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;

/**
 * Defers an action until a component is on screen. Entrance motion is requested while a window is
 * still being built (not displayable yet, so a {@link Motion.Tween} would stop at once); this starts
 * it once the window shows and its first, expensive paints (backdrop scene, glyph caches; ~100-200 ms
 * measured on login) are done, so those stalls do not eat the start of the animation.
 */
final class WhenShown {
    /** Grace after the window first shows before entrance motion starts. */
    static final int FIRST_PAINT_GRACE_MS = 200;

    private WhenShown() { }

    /** Runs {@code action} now if {@code c} is showing, otherwise once, the first time it shows. */
    static void run(JComponent c, Runnable action) {
        if (c.isShowing()) {
            action.run();
            return;
        }
        HierarchyListener[] self = new HierarchyListener[1];
        self[0] = e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0 || !c.isShowing()) return;
            c.removeHierarchyListener(self[0]);
            Timer later = new Timer(FIRST_PAINT_GRACE_MS, ev -> { if (c.isShowing()) action.run(); });
            later.setRepeats(false);
            later.start();
        };
        c.addHierarchyListener(self[0]);
    }
}
