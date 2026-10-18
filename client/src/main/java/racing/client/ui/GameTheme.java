package racing.client.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicTextFieldUI;
import javax.swing.plaf.basic.BasicPasswordFieldUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.image.BufferedImage;

/** Shared visual language for the game menus. All painting stays static and lightweight. */
public final class GameTheme {
    private static final Color TABLE_SELECTION = new Color(0x443124);

    public static final Color BG = new Color(0x101318);
    public static final Color SURFACE = new Color(0x191E26);
    public static final Color INPUT = new Color(0x11161D);
    public static final Color LINE = new Color(0x343D4B);
    public static final Color TEXT = new Color(0xF3F4F6);
    public static final Color MUTED = new Color(0xAAB4C3);
    public static final Color ACCENT = new Color(0xFF7842);
    public static final Color SUCCESS = new Color(0x76DEB2);
    public static final Color ERROR = new Color(0xFF9696);
    public static final int SPACE = 8;
    public static final int CONTROL_H = 46;
    public static final Font BODY = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font SMALL = BODY.deriveFont(12f);
    public static final Font STRONG = BODY.deriveFont(Font.BOLD);
    public static final Font TITLE = new Font("Bahnschrift", Font.BOLD, 32);
    private GameTheme() { }

    public static void install() {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception e) { throw new IllegalStateException("Không khởi tạo được giao diện game", e); }
        for (String key : new String[]{"Panel.background", "OptionPane.background", "Viewport.background"}) UIManager.put(key, BG);
        for (String key : new String[]{"Label.foreground", "OptionPane.messageForeground", "Table.foreground", "Button.foreground"}) UIManager.put(key, TEXT);
        for (String key : new String[]{"Label.font", "Button.font", "TextField.font", "PasswordField.font", "Table.font", "OptionPane.messageFont"}) UIManager.put(key, BODY);
        UIManager.put("Button.background", SURFACE);
        UIManager.put("Table.background", SURFACE);
        UIManager.put("Table.selectionBackground", TABLE_SELECTION);
        UIManager.put("Table.selectionForeground", TEXT);
        UIManager.put("TableHeader.background", INPUT);
        UIManager.put("TableHeader.foreground", MUTED);
        UIManager.put("TableHeader.font", STRONG);
        UIManager.put("ScrollBar.thumb", LINE);
        UIManager.put("ScrollBar.track", BG);
        UIManager.put("ProgressBar.foreground", ACCENT);
        UIManager.put("ProgressBar.background", INPUT);
    }

    public static JPanel panel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    public static JPanel column() {
        JPanel panel = panel(null);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    public static JLabel label(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    public static JLabel heading(String text) {
        return label(text, TITLE, TEXT);
    }

    public static JLabel eyebrow(String text) {
        return label(text, STRONG.deriveFont(12f), ACCENT);
    }

    public static void gap(JPanel panel, int size) {
        panel.add(Box.createVerticalStrut(size));
    }
    public static void fullWidth(JComponent c, int height) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setMinimumSize(new Dimension(0, height));
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        c.setPreferredSize(new Dimension(320, height));
    }
    public static JPanel card(LayoutManager layout, int padding) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE),
                BorderFactory.createEmptyBorder(padding, padding, padding, padding)));
        return panel;
    }
    public static void field(JTextComponent field) {
        if (field instanceof JPasswordField passwordField) {
            passwordField.setUI(new BasicPasswordFieldUI());
        } else if (field instanceof JTextField textField) {
            textField.setUI(new BasicTextFieldUI());
        }
        field.setFont(BODY);
        field.setForeground(TEXT);
        field.setBackground(INPUT);
        field.setCaretColor(ACCENT);
        field.setSelectionColor(new Color(0x634332));
        field.setDisabledTextColor(MUTED);
        field.setBorder(fieldBorder(LINE));
        fullWidth(field, CONTROL_H);
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                field.setBorder(fieldBorder(ACCENT));
            }

            @Override
            public void focusLost(FocusEvent event) {
                field.setBorder(fieldBorder(LINE));
            }
        });
    }
    private static Border fieldBorder(Color color) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color),
                BorderFactory.createEmptyBorder(10, 12, 10, 12));
    }

    public static void formField(JPanel panel, String title, JTextComponent field) {
        JLabel label = label(title, SMALL, MUTED);
        label.setLabelFor(field);
        panel.add(label);
        gap(panel, SPACE);
        field(field);
        panel.add(field);
        gap(panel, SPACE * 2);
    }

    public static JButton button(String text, boolean primary) {
        return new GameButton(text, primary);
    }
    public static void decorate(Window window) {
        BufferedImage icon = new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = icon.createGraphics(); g.setColor(BG); g.fillRect(0,0,32,32);
        g.setColor(ACCENT); g.fillPolygon(new int[]{6,14,23,15},new int[]{26,6,6,26},4);
        g.setColor(TEXT); g.fillPolygon(new int[]{18,23,28,23},new int[]{26,14,14,26},4); g.dispose();
        window.setIconImage(icon);
    }
    public static String html(String s) {
        return s == null ? "" : s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");
    }
    public static void status(JLabel label, String text, Color color) {
        label.setForeground(color); label.setFont(SMALL);
        label.setText("<html>" + html(text) + "</html>");
    }
    public static void table(JTable t) {
        t.setFont(BODY); t.setForeground(TEXT); t.setBackground(SURFACE);
        t.setRowHeight(48); t.setShowGrid(false); t.setIntercellSpacing(new Dimension(0,0));
        t.setSelectionBackground(TABLE_SELECTION); t.setSelectionForeground(TEXT);
        t.setFillsViewportHeight(true); t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setPreferredSize(new Dimension(100,40));
        t.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table,Object value,boolean selected,boolean focus,int row,int col) {
                super.getTableCellRendererComponent(table,value,selected,focus,row,col);
                setBackground(INPUT); setForeground(MUTED); setFont(STRONG.deriveFont(12f));
                setBorder(BorderFactory.createEmptyBorder(0,16,0,16)); return this;
            }
        });
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table,Object value,boolean selected,boolean focus,int row,int col) {
                super.getTableCellRendererComponent(table,value,selected,focus,row,col);
                setFont(col==0 ? STRONG : BODY);
                setBackground(selected ? TABLE_SELECTION : (row%2==0 ? SURFACE : new Color(0x1D232D)));
                setForeground(TEXT);
                setHorizontalAlignment(value instanceof Number ? RIGHT : LEFT);
                setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,0,1,0,LINE),BorderFactory.createEmptyBorder(0,16,0,16)));
                if (focus) setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ACCENT),BorderFactory.createEmptyBorder(0,15,0,15)));
                return this;
            }
        };
        t.setDefaultRenderer(Object.class,renderer); t.setDefaultRenderer(Integer.class,renderer);
    }
    public static JScrollPane scroll(JTable table) {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(LINE));
        scrollPane.getViewport().setBackground(SURFACE);
        scrollPane.setBackground(SURFACE);
        return scrollPane;
    }
    private static final class GameButton extends JButton {
        private final boolean primary;

        GameButton(String text, boolean primary) {
            super(text);
            this.primary = primary;
            setFont(STRONG);
            setForeground(primary ? BG : TEXT);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            Insets insets = getInsets();
            int textWidth = getFontMetrics(getFont()).stringWidth(text);
            setPreferredSize(new Dimension(textWidth + insets.left + insets.right + SPACE * 2, CONTROL_H));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = primary ? ACCENT : SURFACE;
            if (!isEnabled()) {
                fill = LINE;
            } else if (getModel().isPressed()) {
                fill = primary ? new Color(0xDA5927) : LINE;
            } else if (getModel().isRollover()) {
                fill = primary ? new Color(0xFF966A) : new Color(0x293342);
            }
            g.setColor(fill);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            if (!primary || hasFocus()) {
                g.setColor(hasFocus() ? ACCENT : LINE);
                g.setStroke(new BasicStroke(hasFocus() ? 2 : 1));
                g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 8, 8);
            }
            if (hasFocus() && primary) {
                g.setColor(BG);
                g.drawRoundRect(4, 4, getWidth() - 9, getHeight() - 9, 6, 6);
            }
            g.dispose();
            super.paintComponent(graphics);
        }
    }
}
