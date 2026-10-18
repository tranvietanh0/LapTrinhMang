package racing.client.ui.theme;

import javax.swing.JComponent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;

/**
 * Defers an action until a component is on screen. Entrance motion is requested while a window is
 * still being built (not displayable yet, so a {@link Motion.Tween} would stop at once); this starts
 * it the moment the window actually shows, so the user sees the whole animation.
 */
final class WhenShown {
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
            action.run();
        };
        c.addHierarchyListener(self[0]);
    }
}
