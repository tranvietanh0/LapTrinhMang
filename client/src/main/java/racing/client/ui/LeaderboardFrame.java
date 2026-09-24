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
        table.setRowHeight(22);
        table.setEnabled(false);
        table.getColumnModel().getColumn(0).setMaxWidth(60);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(520, 360));

        JPanel root = new JPanel(new BorderLayout(6, 6));
        root.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        root.add(new JLabel("Xếp theo điểm giảm dần, cùng điểm thì nhiều trận thắng hơn xếp trên."),
                BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refresh = new JButton("Làm mới");
        refresh.addActionListener(e -> request());
        south.add(refresh);
        south.add(statusLabel);
        root.add(south, BorderLayout.SOUTH);
        setContentPane(root);

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
