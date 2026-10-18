package racing.client.ui;

import racing.client.net.NetworkClient;
import racing.common.net.Message;
import racing.common.net.MessageType;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/** Bảng xếp hạng: gửi LEADERBOARD_REQ khi mở, hiển thị LEADERBOARD, nút Làm mới. */
public final class LeaderboardFrame extends JFrame {

    private final NetworkClient net;
    private final LeaderboardTableModel model = new LeaderboardTableModel();
    private final JLabel statusLabel = new JLabel("Đang tải…");
    private Runnable off;

    public LeaderboardFrame(NetworkClient net) {
        super("Bảng xếp hạng");
        this.net = net;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JTable table = new JTable(model);
        GameTheme.table(table);
        table.setEnabled(false);
        table.getColumnModel().getColumn(0).setMaxWidth(60);
        JScrollPane scroll = GameTheme.scroll(table);
        scroll.setPreferredSize(new Dimension(720, 360));

        JPanel root = new JPanel(new BorderLayout(6, 6));
        root.setBackground(GameTheme.BG); root.setBorder(BorderFactory.createEmptyBorder(24,32,24,32));
        JPanel heading=GameTheme.column(); heading.add(GameTheme.eyebrow("DRIVER STANDINGS"));
        GameTheme.gap(heading,12); heading.add(GameTheme.heading("Bảng xếp hạng")); GameTheme.gap(heading,8);
        heading.add(GameTheme.label("Điểm cao hơn xếp trước; cùng điểm xét số trận thắng.",GameTheme.SMALL,GameTheme.MUTED));
        GameTheme.gap(heading,16); root.add(heading,BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        JPanel south = GameTheme.panel(new FlowLayout(FlowLayout.LEFT,16,16));
        JButton refresh = GameTheme.button("Làm mới",false);
        statusLabel.setFont(GameTheme.SMALL); statusLabel.setForeground(GameTheme.MUTED);
        refresh.addActionListener(e -> request());
        south.add(refresh);
        south.add(statusLabel);
        root.add(south, BorderLayout.SOUTH);
        setContentPane(root);
        GameTheme.decorate(this);

        off = net.on(MessageType.LEADERBOARD, m -> {
            model.setRows(m.getPayload());
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
        pack();
        setLocationRelativeTo(null);
        request();
    }

    private void request() {
        statusLabel.setText("Đang tải…");
        net.send(Message.of(MessageType.LEADERBOARD_REQ));
    }
}
