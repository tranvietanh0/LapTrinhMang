package racing.client.ui.theme;

import javax.swing.*;
import javax.swing.plaf.basic.BasicPasswordFieldUI;
import javax.swing.plaf.basic.BasicTextFieldUI;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.Path2D;

/** Chamfered dark input chrome with an amber focus glow, for JTextField and JPasswordField. */
public final class NfsFieldUI {
    private NfsFieldUI() { }

    /** Restyles an existing field in place (UI, colours, padding, focus repaint). */
    public static void apply(JTextComponent field) {
        field.setUI(field instanceof JPasswordField ? new Password() : new Text());
        field.setOpaque(false);
        field.setForeground(NfsPalette.TEXT);
        field.setCaretColor(NfsPalette.AMBER_HOT);
        field.setSelectionColor(NfsPalette.alpha(NfsPalette.AMBER, 0.45f));
        field.setSelectedTextColor(NfsPalette.TEXT);
        field.setDisabledTextColor(NfsPalette.MUTED);
        int pad = ChamferButton.GLOW;
        field.setBorder(BorderFactory.createEmptyBorder(pad + 6, pad + 16, pad + 6, pad + 12));
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { field.repaint(); }
            @Override public void focusLost(FocusEvent e) { field.repaint(); }
        });
    }

    private static void paintChrome(Graphics graphics, JTextComponent c) {
        int g4 = ChamferButton.GLOW, w = c.getWidth(), h = c.getHeight();
        if (w <= 2 * g4 || h <= 2 * g4) return;
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        boolean focus = c.hasFocus() && c.isEnabled();
        Path2D.Float shape = Chamfer.slash(g4, g4, w - 2 * g4, h - 2 * g4, Math.min(12f, (h - 2 * g4) * 0.28f));
        if (focus) {
            for (int i = 3; i >= 1; i--) {
                g.setColor(NfsPalette.alpha(NfsPalette.AMBER, 0.20f - i * 0.04f));
                g.setStroke(new BasicStroke(i * 2f + 1f));
                g.draw(shape);
            }
        }
        g.setPaint(c.isEnabled()
                ? new GradientPaint(0, g4, new Color(0x05070A), 0, h - g4, new Color(0x0D1218))
                : new GradientPaint(0, 0, new Color(0x0C0F14), 0, h, new Color(0x0C0F14)));
        g.fill(shape);
        g.setStroke(new BasicStroke(focus ? 1.8f : 1.2f));
        g.setColor(focus ? NfsPalette.AMBER : NfsPalette.LINE);
        g.draw(shape);
        if (focus) { // amber tick on the left edge, like a selected menu row
            g.setColor(NfsPalette.AMBER);
            g.fillRect(g4, g4 + 8, 3, h - 2 * g4 - 16);
        }
        g.dispose();
    }

    private static final class Text extends BasicTextFieldUI {
        @Override protected void paintSafely(Graphics g) {
            paintChrome(g, getComponent());
            super.paintSafely(g);
        }
    }

    private static final class Password extends BasicPasswordFieldUI {
        @Override protected void paintSafely(Graphics g) {
            paintChrome(g, getComponent());
            super.paintSafely(g);
        }
    }
}
