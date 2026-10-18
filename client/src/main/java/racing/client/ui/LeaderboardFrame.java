package racing.client.ui;

import racing.client.net.NetworkClient;
import racing.client.ui.theme.*;
import racing.common.dto.RankRow;
import racing.common.net.Message;
import racing.common.net.MessageType;

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.function.IntFunction;

/** Bảng xếp hạng: gửi LEADERBOARD_REQ khi mở, hiển thị LEADERBOARD (bục top 3 + bảng), nút Làm mới. */
public final class LeaderboardFrame extends JFrame {

    private final NetworkClient net;
    private final LeaderboardTableModel model = new LeaderboardTableModel();
    private final MenuPodium podium = new MenuPodium();
    private final JLabel statusLabel = new JLabel("Đang tải…");
    private Runnable off;

    public LeaderboardFrame(NetworkClient net) {
        super("Bảng xếp hạng");
        this.net = net;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JTable table = new JTable(model);
        GameTheme.table(table);
        Responsive.rowHeight(table, 52);
        table.setEnabled(false);
        TableMotion rowMotion = TableMotion.attach(table);
        IntFunction<Color> medalStripe = row -> row < 3 ? MenuPaint.medal(row + 1)[0] : null;
        TableColumnModel columns = table.getColumnModel();
        columns.getColumn(0).setCellRenderer(MenuCells.rank(medalStripe));
        columns.getColumn(1).setCellRenderer(MenuCells.text(NfsFonts.display(22f), GameTheme.TEXT, null));
        columns.getColumn(2).setCellRenderer(MenuCells.text(NfsFonts.display(22f), GameTheme.ACCENT, null));
        columns.getColumn(3).setCellRenderer(MenuCells.text(NfsFonts.strong(17f), GameTheme.SUCCESS, null));
        columns.getColumn(4).setCellRenderer(MenuCells.text(NfsFonts.strong(17f), GameTheme.ERROR, null));
        columns.getColumn(5).setCellRenderer(MenuCells.text(NfsFonts.strong(17f), GameTheme.MUTED, null));
        TableColumn rank = columns.getColumn(0);
        rank.setMinWidth(96); rank.setMaxWidth(96);
        columns.getColumn(1).setPreferredWidth(320);
        JScrollPane scroll = GameTheme.scroll(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        JPanel heading = GameTheme.column();
        heading.add(GameTheme.eyebrow("DRIVER STANDINGS / TOP TAY ĐUA")); GameTheme.gap(heading, 4);
        heading.add(GameTheme.heading("BẢNG XẾP HẠNG")); GameTheme.gap(heading, 4);
        heading.add(GameTheme.label("Điểm cao hơn xếp trước; cùng điểm xét số trận thắng.", GameTheme.SMALL, GameTheme.MUTED));

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
        shell.add(podium);
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

        off = net.on(MessageType.LEADERBOARD, m -> {
            List<RankRow> rows = m.getPayload();
            model.setRows(rows);
            podium.setRows(rows);
            rowMotion.revealFirstLoad();
            statusLabel.setText(model.getRowCount() + " người chơi");
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

    private void request() {
        statusLabel.setText("Đang tải…");
        net.send(Message.of(MessageType.LEADERBOARD_REQ));
    }
}
