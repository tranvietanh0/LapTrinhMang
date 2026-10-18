package racing.client.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import racing.client.ui.theme.ChamferButton;
import racing.client.ui.theme.ChamferPanel;
import racing.client.ui.theme.NfsFieldUI;
import racing.client.ui.theme.NfsFonts;
import racing.client.ui.theme.NfsPalette;
import racing.client.ui.theme.Responsive;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;

/**
 * Entry point of the Need-for-Speed street design system. The visual pieces live in
 * {@code racing.client.ui.theme}; this facade keeps the original menu API stable.
 */
public final class GameTheme {
    private static final Color TABLE_SELECTION = new Color(0x3A2D12);
    private static final Color TABLE_ALT = new Color(0x161B24);

    public static final Color BG = NfsPalette.BG;
    public static final Color SURFACE = NfsPalette.SURFACE;
    public static final Color INPUT = NfsPalette.INPUT;
    public static final Color LINE = NfsPalette.LINE;
    public static final Color TEXT = NfsPalette.TEXT;
    public static final Color MUTED = NfsPalette.MUTED;
    public static final Color ACCENT = NfsPalette.AMBER;
    public static final Color SUCCESS = NfsPalette.SUCCESS;
    public static final Color ERROR = NfsPalette.ERROR;
    public static final int SPACE = 8;
    /** Height of buttons and inputs, including the glow margin reserved around them. */
    public static final int CONTROL_H = 52;
    public static final Font BODY = NfsFonts.body(15f);
    public static final Font SMALL = NfsFonts.strong(13.5f);
    public static final Font STRONG = NfsFonts.strong(15f);
    public static final Font TITLE = NfsFonts.display(34f);

    private GameTheme() { }

    /** Installs FlatLaf (dark) plus the NFS overrides. Call once, before creating any window. */
    public static void install() {
        UIManager.put("defaultFont", BODY);
        FlatLaf.setGlobalExtraDefaults(Map.of("@accentColor", "#FFB000", "@background", "#0A0D13", "@foreground", "#F5F2EA"));
        if (!FlatDarkLaf.setup()) throw new IllegalStateException("Không khởi tạo được giao diện game");
        for (String key : new String[]{"Component.arc", "Button.arc", "TextComponent.arc", "ProgressBar.arc", "CheckBox.arc"}) UIManager.put(key, 0);
        for (String key : new String[]{"Panel.background", "OptionPane.background", "Viewport.background", "RootPane.background"}) UIManager.put(key, BG);
        UIManager.put("Component.focusColor", ACCENT);
        UIManager.put("Component.focusedBorderColor", ACCENT);
        UIManager.put("Button.background", SURFACE);
        UIManager.put("Button.default.background", ACCENT);
        UIManager.put("Button.default.foreground", NfsPalette.VOID);
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("ScrollBar.thumbArc", 0);
        UIManager.put("ScrollBar.showButtons", false);
        UIManager.put("ScrollBar.thumb", NfsPalette.mix(LINE, ACCENT, 0.25f));
        UIManager.put("ScrollBar.hoverThumbColor", ACCENT);
        UIManager.put("ScrollBar.track", NfsPalette.VOID);
        UIManager.put("Table.background", SURFACE);
        UIManager.put("Table.selectionBackground", TABLE_SELECTION);
        UIManager.put("Table.selectionForeground", TEXT);
        UIManager.put("TableHeader.background", INPUT);
        UIManager.put("TableHeader.foreground", MUTED);
        UIManager.put("TableHeader.font", SMALL);
        UIManager.put("ProgressBar.foreground", ACCENT);
        UIManager.put("ProgressBar.background", INPUT);
        UIManager.put("ToolTip.background", NfsPalette.SURFACE_HI);
        UIManager.put("ToolTip.foreground", TEXT);
        UIManager.put("ToolTip.border", BorderFactory.createLineBorder(ACCENT));
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("OptionPane.messageFont", BODY);
        UIManager.put("OptionPane.buttonFont", STRONG);
        UIManager.put("OptionPane.border", BorderFactory.createEmptyBorder(16, 20, 16, 20));
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

    /** Label whose font follows the window scale (see {@link Responsive}). */
    public static JLabel label(String text, Font font, Color color) {
        JLabel label = new JLabel(NfsFonts.safe(text));
        Responsive.font(label, font);
        label.setForeground(color);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    public static JLabel heading(String text) {
        return label(text, TITLE, TEXT);
    }

    /** Small tracked amber kicker above a heading. */
    public static JLabel eyebrow(String text) {
        return label(text, NfsFonts.tracked(NfsFonts.strong(13f), 0.2f), ACCENT);
    }

    /** Vertical space that scales with the window. */
    public static void gap(JPanel panel, int size) {
        Box.Filler filler = new Box.Filler(new Dimension(0, size), new Dimension(0, size), new Dimension(Short.MAX_VALUE, size));
        panel.add(Responsive.height(filler, size));
    }

    public static void fullWidth(JComponent c, int height) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setMinimumSize(new Dimension(0, height));
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        c.setPreferredSize(new Dimension(320, height));
        Responsive.height(c, height);
    }

    public static JPanel card(LayoutManager layout, int padding) {
        ChamferPanel panel = new ChamferPanel(layout, 16);
        panel.setBorder(BorderFactory.createEmptyBorder(padding, padding, padding, padding));
        return panel;
    }

    public static void field(JTextComponent field) {
        NfsFieldUI.apply(field);
        Responsive.font(field, NfsFonts.body(17f));
        fullWidth(field, CONTROL_H);
    }

    public static void formField(JPanel panel, String title, JTextComponent field) {
        JLabel label = label(title, NfsFonts.tracked(NfsFonts.strong(13.5f), 0.14f), MUTED);
        label.setBorder(BorderFactory.createEmptyBorder(0, ChamferButton.GLOW, 0, 0));
        label.setLabelFor(field);
        panel.add(label);
        gap(panel, 2);
        field(field);
        panel.add(field);
        gap(panel, SPACE * 2);
    }

    public static JButton button(String text, boolean primary) {
        return new ChamferButton(text, primary);
    }

    public static void decorate(Window window) {
        BufferedImage icon = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = icon.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(NfsPalette.VOID);
        g.fillPolygon(new int[]{0, 64, 64, 14}, new int[]{0, 0, 64, 64}, 4);
        g.setColor(ACCENT);
        g.fillPolygon(new int[]{12, 30, 52, 34}, new int[]{52, 12, 12, 52}, 4);
        g.setColor(NfsPalette.RED);
        g.fillPolygon(new int[]{38, 48, 58, 48}, new int[]{52, 28, 28, 52}, 4);
        g.dispose();
        window.setIconImage(icon);
    }

    public static String html(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    public static void status(JLabel label, String text, Color color) {
        label.setForeground(color);
        Responsive.font(label, SMALL);
        label.setText("<html>" + html(NfsFonts.safe(text)) + "</html>");
    }

    public static void table(JTable t) {
        t.setFont(BODY);
        t.setForeground(TEXT);
        t.setBackground(SURFACE);
        Responsive.rowHeight(t, 48);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setSelectionBackground(TABLE_SELECTION);
        t.setSelectionForeground(TEXT);
        t.setFillsViewportHeight(true);
        t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setPreferredSize(new Dimension(100, 40));
        t.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(table, value, selected, focus, row, col);
                setBackground(INPUT);
                setForeground(ACCENT);
                setFont(NfsFonts.tracked(NfsFonts.strong(12.5f), 0.14f));
                setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, ACCENT),
                        BorderFactory.createEmptyBorder(0, 16, 0, 16)));
                return this;
            }
        });
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(table, value, selected, focus, row, col);
                setFont(col == 0 ? STRONG : BODY);
                setBackground(selected ? TABLE_SELECTION : (row % 2 == 0 ? SURFACE : TABLE_ALT));
                setForeground(TEXT);
                setHorizontalAlignment(value instanceof Number ? RIGHT : LEFT);
                setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, LINE),
                        BorderFactory.createEmptyBorder(0, 16, 0, 16)));
                if (focus) setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ACCENT), BorderFactory.createEmptyBorder(0, 15, 0, 15)));
                return this;
            }
        };
        t.setDefaultRenderer(Object.class, renderer);
        t.setDefaultRenderer(Integer.class, renderer);
    }

    public static JScrollPane scroll(JTable table) {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(LINE));
        scrollPane.getViewport().setBackground(SURFACE);
        scrollPane.setBackground(SURFACE);
        return scrollPane;
    }
}
