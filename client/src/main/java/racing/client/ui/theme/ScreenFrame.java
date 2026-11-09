package racing.client.ui.theme;

import javax.swing.*;
import java.awt.*;

/**
 * One-call setup for a themed, resizable, F11-capable window. Every helper keeps windows inside the
 * usable screen area (screen minus taskbar), so no button can end up below the taskbar or off-screen
 * on a small display or with a high Windows scaling setting.
 */
public final class ScreenFrame {
    private ScreenFrame() { }

    /**
     * Makes the (already populated) backdrop the content pane and adds responsive scaling and F11.
     * Set the window size BEFORE calling so the first scale is right.
     */
    public static void install(JFrame frame, NfsBackdrop backdrop, Dimension minimum) {
        frame.setContentPane(backdrop);
        minimumSize(frame, minimum);
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

    /** Sets the window size, clamped to the usable screen area so it never opens under the taskbar or off-screen. */
    public static void fit(Window window, int width, int height) {
        Rectangle area = workArea(window);
        window.setSize(Math.min(width, area.width), Math.min(height, area.height));
    }

    /** Minimum size, but never larger than the usable screen area (a small screen must still fit the window). */
    public static void minimumSize(Window window, Dimension minimum) {
        Rectangle area = workArea(window);
        window.setMinimumSize(new Dimension(Math.min(minimum.width, area.width), Math.min(minimum.height, area.height)));
    }

    /**
     * Sizes a card dialog so the WHOLE card, buttons included, is visible: the content pane gets the card's
     * height plus margins at the owner's scale and {@code pack()} adds the title bar and borders. When that
     * would not fit the usable screen, the dialog's scale steps down until it does. Then centres it on the owner.
     *
     * @param baseContentWidth content width at scale 1
     * @param baseMargin       margin above and below the card at scale 1 (the card layout's margin)
     */
    public static void fitCard(JDialog dialog, JComponent content, Component card, int baseContentWidth, int baseMargin, Window owner) {
        Rectangle area = workArea(owner != null ? owner : dialog);
        float scale = Responsive.scaleOf(owner);
        while (true) {
            Responsive.pin(dialog.getRootPane(), scale);
            // +2 px: with fractional Windows scaling (e.g. 150%) the window borders round and can steal a pixel
            content.setPreferredSize(new Dimension(Math.round(baseContentWidth * scale),
                    card.getPreferredSize().height + 2 * Math.round(baseMargin * scale) + 2));
            dialog.pack();
            boolean fits = dialog.getWidth() <= area.width && dialog.getHeight() <= area.height;
            if (fits || scale <= Responsive.MIN_SCALE) {
                break;
            }
            scale = Math.max(Responsive.MIN_SCALE, scale - 0.05f);
        }
        dialog.setMinimumSize(new Dimension(Math.min(dialog.getWidth(), 420), Math.min(dialog.getHeight(), 380)));
        fit(dialog, dialog.getWidth(), dialog.getHeight());
        center(dialog, owner);
    }

    /** Centres the window on {@code owner} (or the screen when null), then nudges it fully into the usable area. */
    public static void center(Window window, Component owner) {
        window.setLocationRelativeTo(owner);
        Rectangle area = workArea(window);
        int x = Math.max(area.x, Math.min(window.getX(), area.x + area.width - window.getWidth()));
        int y = Math.max(area.y, Math.min(window.getY(), area.y + area.height - window.getHeight()));
        window.setLocation(x, y);
    }

    /** The usable area (screen bounds minus taskbar and other reserved insets) of the window's screen. */
    public static Rectangle workArea(Window window) {
        GraphicsConfiguration gc = window != null && window.getGraphicsConfiguration() != null
                ? window.getGraphicsConfiguration()
                : GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        Rectangle b = gc.getBounds();
        Insets i = Toolkit.getDefaultToolkit().getScreenInsets(gc);
        return new Rectangle(b.x + i.left, b.y + i.top, b.width - i.left - i.right, b.height - i.top - i.bottom);
    }
}
