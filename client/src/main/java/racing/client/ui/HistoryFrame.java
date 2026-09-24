package racing.client.ui;

import racing.client.net.NetworkClient;
import racing.common.GameConfig;
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

/** Lịch sử trận của chính mình: gửi MATCH_HISTORY_REQ khi mở, hiển thị MATCH_HISTORY. */
public final class HistoryFrame extends JFrame {

    private final NetworkClient net;
    private final HistoryTableModel model = new HistoryTableModel();
    private final JLabel statusLabel = new JLabel("Đang tải…");
    private Runnable off;

    public HistoryFrame(NetworkClient net) {
        super("Lịch sử trận");
        this.net = net;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JTable table = new JTable(model);
        table.setRowHeight(22);
        table.setEnabled(false);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(560, 340));

        JPanel root = new JPanel(new BorderLayout(6, 6));
        root.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        root.add(new JLabel(GameConfig.HISTORY_LIMIT + " trận gần nhất, mới nhất xếp trên."), BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refresh = new JButton("Làm mới");
        refresh.addActionListener(e -> request());
        south.add(refresh);
        south.add(statusLabel);
        root.add(south, BorderLayout.SOUTH);
        setContentPane(root);

        off = net.on(MessageType.MATCH_HISTORY, m -> {
            model.setRows(m.getPayload());
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
        pack();
        setLocationRelativeTo(null);
        request();
    }

    private void request() {
        statusLabel.setText("Đang tải…");
        net.send(Message.of(MessageType.MATCH_HISTORY_REQ));
    }
}
