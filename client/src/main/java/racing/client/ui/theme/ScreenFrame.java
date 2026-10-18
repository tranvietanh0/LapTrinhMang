package racing.client.ui.theme;

import javax.swing.*;
import java.awt.*;

/** One-call setup for a themed, resizable, F11-capable window. */
public final class ScreenFrame {
    private ScreenFrame() { }

    /**
     * Makes the (already populated) backdrop the content pane and adds responsive scaling and F11.
     * Set the window size BEFORE calling so the first scale is right.
     */
    public static void install(JFrame frame, NfsBackdrop backdrop, Dimension minimum) {
        frame.setContentPane(backdrop);
        frame.setMinimumSize(minimum);
        frame.setResizable(true);
        Responsive.attach(frame, frame.getRootPane());
        Fullscreen.bind(frame);
    }

    /** Dialog variant: no fullscreen toggle; its scale is pinned to the owner window's scale. */
    public static void install(JDialog dialog, NfsBackdrop backdrop, Window owner) {
        dialog.setContentPane(backdrop);
        dialog.setResizable(true);
        Responsive.pin(dialog.getRootPane(), Responsive.scaleOf(owner));
    }

    /** Sets the window size, clamped to 94% of the usable screen so it never opens off-screen. */
    public static void fit(Window window, int width, int height) {
        Rectangle screen = window.getGraphicsConfiguration() != null
                ? window.getGraphicsConfiguration().getBounds() : new Rectangle(0, 0, 1920, 1080);
        window.setSize(Math.min(width, (int) (screen.width * 0.94)), Math.min(height, (int) (screen.height * 0.92)));
    }
}
