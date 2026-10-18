package racing.client.ui.theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowListener;

/** F11 borderless-fullscreen toggle for a JFrame (no exclusive mode, so modal dialogs still work). */
public final class Fullscreen {
    private static final String KEY = "nfs.fullscreen";

    private Fullscreen() { }

    /** Binds F11 on the frame's root pane. */
    public static void bind(JFrame frame) {
        JRootPane root = frame.getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), KEY);
        root.getActionMap().put(KEY, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { toggle(frame); }
        });
    }

    public static boolean isFullscreen(JFrame frame) {
        return frame.getRootPane().getClientProperty(KEY) instanceof Rectangle;
    }

    public static void toggle(JFrame frame) {
        boolean entering = !isFullscreen(frame);
        Rectangle restore = entering ? frame.getBounds() : (Rectangle) frame.getRootPane().getClientProperty(KEY);
        Rectangle screen = frame.getGraphicsConfiguration().getBounds();
        // Changing the decoration needs dispose(); mute listeners so windowClosed handlers do not fire.
        WindowListener[] listeners = frame.getWindowListeners();
        for (WindowListener l : listeners) frame.removeWindowListener(l);
        int state = frame.getExtendedState();
        frame.dispose();
        frame.setUndecorated(entering);
        frame.getRootPane().putClientProperty(KEY, entering ? restore : null);
        frame.setExtendedState(JFrame.NORMAL);
        frame.setBounds(entering ? screen : restore);
        frame.setVisible(true);
        if (!entering && (state & JFrame.MAXIMIZED_BOTH) != 0) frame.setExtendedState(state);
        for (WindowListener l : listeners) frame.addWindowListener(l);
        frame.toFront();
        frame.requestFocus();
    }
}
