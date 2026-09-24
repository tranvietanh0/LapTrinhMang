package racing.client.ui;

import racing.client.ClientApp;
import racing.client.model.ClientState;
import racing.client.net.NetworkClient;
import racing.common.GameConfig;
import racing.common.dto.InviteInfo;
import racing.common.dto.InviteResult;
import racing.common.dto.MatchStart;
import racing.common.dto.PlayerInfo;
import racing.common.net.Message;
import racing.common.net.MessageType;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Sảnh: bảng người online tự cập nhật từ ONLINE_LIST, thách đấu, nhận lời mời,
 * mở bảng xếp hạng / lịch sử, đăng xuất. Nhận MATCH_START thì ẩn sảnh và mở màn hình đua.
 */
public final class LobbyFrame extends JFrame {

    private final ClientApp app;
    private final NetworkClient net;
    private final ClientState state;
    private final OnlineTableModel model = new OnlineTableModel();
    private final JTable table = new JTable(model);
    private final JLabel meLabel = new JLabel();
    private final JLabel statusLabel = new JLabel(" ");
    private final JButton inviteButton = new JButton("Thách đấu");
    private final RaceLauncher launcher;
    private final List<Runnable> offs = new ArrayList<>();
    private InviteDialog inviteDialog;

    public LobbyFrame(ClientApp app) {
        super("Đua xe online – Sảnh");
        this.app = app;
        this.net = app.net();
        this.state = app.state();
        this.launcher = new RaceLauncher(net, this::onRaceClosed);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                app.logout();
                System.exit(0);
            }

            @Override
            public void windowClosed(WindowEvent e) {
                offs.forEach(Runnable::run);
                offs.clear();
            }
        });

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        meLabel.setFont(meLabel.getFont().deriveFont(Font.BOLD, 14f));
        root.add(meLabel, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);
        table.setAutoCreateRowSorter(false);
        table.getSelectionModel().addListSelectionListener(e -> refreshInviteButton());
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(520, 300));
        root.add(scroll, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(4, 4));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton leaderboard = new JButton("Bảng xếp hạng");
        JButton history = new JButton("Lịch sử trận");
        JButton logout = new JButton("Đăng xuất");
        buttons.add(inviteButton);
        buttons.add(leaderboard);
        buttons.add(history);
        buttons.add(logout);
        south.add(buttons, BorderLayout.NORTH);
        south.add(statusLabel, BorderLayout.SOUTH);
        root.add(south, BorderLayout.SOUTH);
        setContentPane(root);

        inviteButton.addActionListener(e -> invite());
        leaderboard.addActionListener(e -> new LeaderboardFrame(net).setVisible(true));
        history.addActionListener(e -> new HistoryFrame(net).setVisible(true));
        logout.addActionListener(e -> app.logout());

        registerListeners();
        updateMe();
        refreshInviteButton();
        pack();
        setLocationRelativeTo(null);
    }

    // ------------------------------------------------------------------ thông điệp từ server

    private void registerListeners() {
        offs.add(net.on(MessageType.ONLINE_LIST, m -> onOnlineList(m.getPayload())));
        offs.add(net.on(MessageType.INVITE_RECEIVED, m -> onInviteReceived(m.getPayload())));
        offs.add(net.on(MessageType.INVITE_RESULT, m -> onInviteResult(m.getPayload())));
        offs.add(net.on(MessageType.MATCH_START, m -> onMatchStart(m.getPayload())));
        offs.add(net.on(MessageType.ERROR, m -> setStatus(String.valueOf(m.rawPayload()), true)));
    }

    private void onOnlineList(List<PlayerInfo> list) {
        String selected = selectedUsername();
        state.setOnline(list);
        model.setRows(state.online(), state.me() == null ? null : state.me().username());
        if (selected != null) {
            int row = model.rowOf(selected);
            if (row >= 0) {
                table.setRowSelectionInterval(row, row);
            }
        }
        updateMe();
        refreshInviteButton();
    }

    private void onInviteReceived(InviteInfo info) {
        if (inviteDialog != null && inviteDialog.isDisplayable()) {
            inviteDialog.dispose();
        }
        inviteDialog = new InviteDialog(this, net, info);
        inviteDialog.setVisible(true);
    }

    private void onInviteResult(InviteResult r) {
        state.clearPendingInvite();
        String who = r.targetUsername();
        String text = switch (r.status()) {
            case REJECTED -> who + " đã từ chối lời mời.";
            case TIMEOUT -> who + " không trả lời trong " + GameConfig.INVITE_TIMEOUT_S + " giây.";
            case BUSY -> who + " đang bận (đang thi đấu hoặc đang có lời mời khác).";
            case OFFLINE -> who + " đã thoát.";
        };
        setStatus(text, true);
        refreshInviteButton();
    }

    private void onMatchStart(MatchStart start) {
        if (launcher.isOpen()) {
            return;   // ván tiếp theo của cùng phòng: RaceFrame tự xử lý
        }
        state.clearPendingInvite();
        if (inviteDialog != null && inviteDialog.isDisplayable()) {
            inviteDialog.dispose();
        }
        setStatus("Đang thi đấu với " + start.opponent().username(), false);
        setVisible(false);
        launcher.open(start);
    }

    private void onRaceClosed() {
        setStatus("Đã về sảnh.", false);
        refreshInviteButton();
        setVisible(true);
        toFront();
    }

    // ------------------------------------------------------------------ hành động

    private void invite() {
        PlayerInfo target = model.playerAt(table.getSelectedRow());
        if (!state.canInvite(target)) {
            return;
        }
        state.markInviteSent(target.username());
        net.send(new Message(MessageType.INVITE, target.username()));
        setStatus("Đã mời " + target.username() + ", đang chờ trả lời…", false);
        refreshInviteButton();
    }

    private void refreshInviteButton() {
        PlayerInfo target = model.playerAt(table.getSelectedRow());
        inviteButton.setEnabled(state.canInvite(target));
        if (state.hasPendingInvite()) {
            inviteButton.setText("Đang chờ trả lời…");
        } else {
            inviteButton.setText("Thách đấu");
        }
    }

    private void updateMe() {
        PlayerInfo me = state.me();
        if (me != null) {
            meLabel.setText("Xin chào " + me.username() + "  |  Điểm: " + me.points()
                    + "  |  Thắng: " + me.wins());
        }
    }

    private String selectedUsername() {
        PlayerInfo p = model.playerAt(table.getSelectedRow());
        return p == null ? null : p.username();
    }

    private void setStatus(String text, boolean isError) {
        statusLabel.setText(text == null ? " " : text);
        statusLabel.setForeground(isError ? new Color(0xB00020) : new Color(0x333333));
    }
}
