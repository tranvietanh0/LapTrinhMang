package racing.client.ui;

import racing.client.net.NetworkClient;
import racing.client.ui.theme.*;
import racing.common.GameConfig;
import racing.common.dto.MatchOutcome;
import racing.common.dto.MatchRow;
import racing.common.net.Message;
import racing.common.net.MessageType;

import javax.swing.*;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.function.IntFunction;

/** Lịch sử trận của chính mình: gửi MATCH_HISTORY_REQ khi mở, hiển thị MATCH_HISTORY (tổng kết + bảng chip thắng/thua). */
public final class HistoryFrame extends JFrame {

    private final NetworkClient net;
    private final HistoryTableModel model = new HistoryTableModel();
    private final MenuStatStrip summary = new MenuStatStrip();
    private final JLabel statusLabel = new JLabel("Đang tải…");
    private Runnable off;

    public HistoryFrame(NetworkClient net) {
        super("Lịch sử trận");
        this.net = net;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JTable table = new JTable(model);
        GameTheme.table(table);
        Responsive.rowHeight(table, 52);
        table.setEnabled(false);
        TableMotion rowMotion = TableMotion.attach(table);
        IntFunction<Color> resultStripe = row -> outcomeColor(String.valueOf(table.getValueAt(row, 2)));
        TableColumnModel columns = table.getColumnModel();
        columns.getColumn(0).setPreferredWidth(220); columns.getColumn(0).setCellRenderer(MenuCells.text(NfsFonts.strong(16f), GameTheme.MUTED, resultStripe));
        columns.getColumn(1).setPreferredWidth(300); columns.getColumn(1).setCellRenderer(MenuCells.text(NfsFonts.display(22f), GameTheme.TEXT, null));
        columns.getColumn(2).setPreferredWidth(160); columns.getColumn(2).setCellRenderer(MenuCells.chip(HistoryFrame::outcomeChip, null));
        columns.getColumn(3).setPreferredWidth(200); columns.getColumn(3).setCellRenderer(MenuCells.text(NfsFonts.body(16f), GameTheme.MUTED, null));
        JScrollPane scroll = GameTheme.scroll(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        JPanel heading = GameTheme.column();
        heading.add(GameTheme.eyebrow("RACE ARCHIVE / NHẬT KÝ ĐƯỜNG ĐUA")); GameTheme.gap(heading, 4);
        heading.add(GameTheme.heading("LỊCH SỬ ĐƯỜNG ĐUA")); GameTheme.gap(heading, 4);
        heading.add(GameTheme.label(GameConfig.HISTORY_LIMIT + " trận gần nhất của bạn, mới nhất xếp trước.", GameTheme.SMALL, GameTheme.MUTED));
        showSummary(List.of());

        JButton refresh = GameTheme.button("Làm mới", false);
        Responsive.font(statusLabel, GameTheme.SMALL);
        statusLabel.setForeground(GameTheme.MUTED);
        refresh.addActionListener(e -> request());
        JPanel footer = GameTheme.panel(new BorderLayout(16, 0));
        footer.add(statusLabel, BorderLayout.CENTER);
        footer.add(refresh, BorderLayout.EAST);

        JPanel card = GameTheme.card(new BorderLayout(0, 12), 14);
        card.add(scroll, BorderLayout.CENTER);
        card.add(footer, BorderLayout.SOUTH);

        MenuPanel shell = new MenuPanel(new MenuStackLayout(14), 22, 32);
        shell.add(heading);
        shell.add(summary);
        shell.add(card);
        RevealPanel.style(heading, RevealPanel.Style.SLIDE);
        shell.reveal(0, 90);
        NfsBackdrop root = new NfsBackdrop();
        root.setLayout(new BorderLayout());
        root.add(shell, BorderLayout.CENTER);
        GameTheme.decorate(this);
        ScreenFrame.fit(this, 1120, 800);
        ScreenFrame.install(this, root, new Dimension(860, 580));
        setLocationRelativeTo(null);

        off = net.on(MessageType.MATCH_HISTORY, m -> {
            List<MatchRow> rows = m.getPayload();
            model.setRows(rows);
            showSummary(rows);
            rowMotion.revealFirstLoad();
            statusLabel.setText(model.getRowCount() == 0 ? "Chưa có trận nào." : model.getRowCount() + " trận");
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (off != null) {
                    off.run();
                    off = null;
                }
            }
        });
        request();
    }

    private void showSummary(List<MatchRow> rows) {
        int total = rows == null ? 0 : rows.size();
        long wins = rows == null ? 0 : rows.stream().filter(r -> r.outcome() == MatchOutcome.WIN).count();
        long losses = rows == null ? 0 : rows.stream().filter(r -> r.outcome() == MatchOutcome.LOSE).count();
        long draws = rows == null ? 0 : rows.stream().filter(r -> r.outcome() == MatchOutcome.DRAW).count();
        summary.setStats(new String[]{"TỔNG TRẬN", "THẮNG", "THUA", "HÒA"},
                new String[]{String.valueOf(total), String.valueOf(wins), String.valueOf(losses), String.valueOf(draws)},
                new Color[]{GameTheme.TEXT, GameTheme.SUCCESS, NfsPalette.RED, GameTheme.ACCENT});
    }

    private static Color outcomeColor(String text) {
        if (HistoryTableModel.outcomeText(MatchOutcome.WIN).equals(text)) return GameTheme.SUCCESS;
        if (HistoryTableModel.outcomeText(MatchOutcome.LOSE).equals(text)) return NfsPalette.RED;
        if (HistoryTableModel.outcomeText(MatchOutcome.DRAW).equals(text)) return GameTheme.ACCENT;
        return GameTheme.LINE;
    }

    private static MenuCells.Chip outcomeChip(String text) {
        Color color = outcomeColor(text);
        return new MenuCells.Chip(text.toUpperCase(java.util.Locale.ROOT), color == GameTheme.LINE ? GameTheme.MUTED : color);
    }

    private void request() {
        statusLabel.setText("Đang tải…");
        net.send(Message.of(MessageType.MATCH_HISTORY_REQ));
    }
}
