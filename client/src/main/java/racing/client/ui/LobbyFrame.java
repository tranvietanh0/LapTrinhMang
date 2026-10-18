package racing.client.ui;

import racing.client.ClientApp;
import racing.client.model.ClientState;
import racing.client.net.NetworkClient;
import racing.client.ui.theme.*;
import racing.common.GameConfig;
import racing.common.dto.InviteInfo;
import racing.common.dto.InviteResult;
import racing.common.dto.MatchStart;
import racing.common.dto.PlayerInfo;
import racing.common.dto.PlayerStatus;
import racing.common.net.Message;
import racing.common.net.MessageType;

import javax.swing.*;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Sảnh: bảng người online tự cập nhật từ ONLINE_LIST, thách đấu, nhận lời mời,
 * mở bảng xếp hạng / lịch sử, đăng xuất. Nhận MATCH_START thì ẩn sảnh và mở màn hình đua.
 * Giao diện: main menu / garage kiểu Need for Speed (hồ sơ + xe trưng bày + danh sách đối thủ).
 */
public final class LobbyFrame extends JFrame {

    private final ClientApp app;
    private final NetworkClient net;
    private final ClientState state;
    private final OnlineTableModel model = new OnlineTableModel();
    private final JTable table = new JTable(model);
    private final JLabel meLabel = new JLabel();
    private final MenuStatStrip stats = new MenuStatStrip();
    private final MenuShowcase showcase = new MenuShowcase("GARAGE / XE CỦA BẠN", "STREET 01",
            new String[][]{{"1 VS 1", "ĐẤU TRỰC TIẾP"}, {"2.500 M", "ĐƯỜNG ĐUA"}, {"3 LÀN", "NÉ XE CỘ"}});
    private final JLabel statusLabel = new JLabel(" ");
    private final JButton inviteButton = GameTheme.button("THÁCH ĐẤU  →",true);
    private final JLabel onlineLabel = GameTheme.label("",GameTheme.SMALL,GameTheme.MUTED);
    private final JLabel selectionLabel = GameTheme.label("Chọn một đối thủ đang rảnh để bắt đầu.",GameTheme.BODY,GameTheme.MUTED);
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

        JButton leaderboard=navButton("Bảng xếp hạng");
        JButton history=navButton("Lịch sử trận");
        JButton logout=navButton("Đăng xuất");
        JPanel nav=GameTheme.panel(new FlowLayout(FlowLayout.RIGHT,10,0));
        nav.add(leaderboard); nav.add(history); nav.add(logout);
        JPanel navHolder=GameTheme.panel(new GridBagLayout()); navHolder.add(nav);
        JPanel titles=GameTheme.column();
        titles.add(GameTheme.eyebrow("RACING / ONLINE   //   SẢNH CHÍNH")); GameTheme.gap(titles,2);
        titles.add(GameTheme.heading("SẢNH THÁCH ĐẤU"));
        JPanel header=GameTheme.panel(new BorderLayout(16,0));
        header.add(titles,BorderLayout.WEST); header.add(navHolder,BorderLayout.EAST);

        // left: driver card + garage showcase
        Responsive.font(meLabel,NfsFonts.display(40f)); meLabel.setForeground(GameTheme.TEXT);
        meLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel profile=GameTheme.column();
        profile.add(GameTheme.eyebrow("HỒ SƠ TAY ĐUA / DRIVER")); GameTheme.gap(profile,6);
        profile.add(meLabel); GameTheme.gap(profile,14);
        profile.add(stats);
        JPanel profileCard=GameTheme.card(new BorderLayout(),22); profileCard.add(profile,BorderLayout.CENTER);
        JPanel left=new MenuPanel(new MenuStackLayout(16),0,0);
        left.add(profileCard); left.add(showcase);

        // right: roster + challenge action
        JPanel rosterTitles=GameTheme.column();
        rosterTitles.add(GameTheme.eyebrow("GARAGE ROSTER / ĐỐI THỦ ONLINE")); GameTheme.gap(rosterTitles,4);
        rosterTitles.add(GameTheme.heading("TÌM ĐỐI THỦ. VÀO ĐƯỜNG ĐUA.")); GameTheme.gap(rosterTitles,6);
        rosterTitles.add(onlineLabel);
        JLabel connected=GameTheme.label("●  ĐÃ KẾT NỐI",NfsFonts.tracked(NfsFonts.strong(13f),0.12f),GameTheme.SUCCESS);
        JPanel connectedHolder=GameTheme.panel(new FlowLayout(FlowLayout.RIGHT,0,0)); connectedHolder.add(connected);
        JPanel rosterHeader=GameTheme.panel(new BorderLayout(12,0));
        rosterHeader.add(rosterTitles,BorderLayout.CENTER); rosterHeader.add(connectedHolder,BorderLayout.EAST);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        GameTheme.table(table);
        Responsive.rowHeight(table,56);
        table.setAutoCreateRowSorter(false);
        table.getSelectionModel().addListSelectionListener(e -> refreshInviteButton());
        TableColumnModel columns=table.getColumnModel();
        columns.getColumn(0).setPreferredWidth(280); columns.getColumn(0).setCellRenderer(MenuCells.player(model::isSelfRow));
        columns.getColumn(1).setPreferredWidth(100); columns.getColumn(1).setCellRenderer(MenuCells.text(NfsFonts.display(22f),GameTheme.ACCENT,null));
        columns.getColumn(2).setPreferredWidth(100); columns.getColumn(2).setCellRenderer(MenuCells.text(NfsFonts.strong(17f),GameTheme.TEXT,null));
        columns.getColumn(3).setPreferredWidth(170); columns.getColumn(3).setCellRenderer(MenuCells.chip(LobbyFrame::statusChip,null));
        JScrollPane scroll = GameTheme.scroll(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        Responsive.font(selectionLabel,NfsFonts.display(24f));
        GameTheme.fullWidth(inviteButton,64);
        Responsive.font(inviteButton,NfsFonts.tracked(NfsFonts.display(26f),0.04f));
        JPanel action=GameTheme.panel(new BorderLayout(16,0));
        action.add(selectionLabel,BorderLayout.CENTER); action.add(inviteButton,BorderLayout.EAST);
        GameTheme.fullWidth(statusLabel,40);
        GameTheme.status(statusLabel,"Sẵn sàng. Chọn đối thủ để bắt đầu.",GameTheme.MUTED);
        JPanel south=GameTheme.panel(new BorderLayout(0,4));
        south.add(action,BorderLayout.NORTH); south.add(statusLabel,BorderLayout.CENTER);
        south.add(GameTheme.label("W  Tăng tốc     S  Phanh     A / D  Đổi làn     F11  Toàn màn hình",GameTheme.SMALL,GameTheme.MUTED),BorderLayout.SOUTH);

        JPanel rosterCard=GameTheme.card(new BorderLayout(0,16),22);
        rosterCard.add(rosterHeader,BorderLayout.NORTH); rosterCard.add(scroll,BorderLayout.CENTER); rosterCard.add(south,BorderLayout.SOUTH);

        JPanel body=new MenuPanel(new MenuColumnsLayout(left,rosterCard,0.34f,330,20),0,0);
        body.add(left); body.add(rosterCard);
        JPanel shell=new MenuPanel(new MenuStackLayout(18),22,32);
        shell.add(header); shell.add(body);
        NfsBackdrop root=new NfsBackdrop();
        root.setLayout(new BorderLayout()); root.add(shell,BorderLayout.CENTER);
        GameTheme.decorate(this);
        ScreenFrame.fit(this,1280,800); ScreenFrame.install(this,root,new Dimension(900,600)); setLocationRelativeTo(null);

        inviteButton.addActionListener(e -> invite());
        leaderboard.addActionListener(e -> new LeaderboardFrame(net).setVisible(true));
        history.addActionListener(e -> new HistoryFrame(net).setVisible(true));
        logout.addActionListener(e -> app.logout());

        registerListeners();
        onOnlineList(state.online());
        updateMe();
        refreshInviteButton();
    }

    private static JButton navButton(String text) {
        JButton button=GameTheme.button(text,false);
        Responsive.font(button,NfsFonts.tracked(NfsFonts.display(16f),0.04f));
        return button;
    }

    private static MenuCells.Chip statusChip(String text) {
        return OnlineTableModel.statusText(PlayerStatus.FREE).equals(text)
                ? new MenuCells.Chip("RẢNH",GameTheme.SUCCESS) : new MenuCells.Chip("ĐANG ĐUA",NfsPalette.RED);
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
        int others=(int)state.online().stream().filter(p->state.me()==null || !p.username().equals(state.me().username())).count();
        onlineLabel.setText(others==0 ? "Chưa có đối thủ online. Mời bạn bè kết nối vào cùng server." : others+" đối thủ online • Chọn một người chơi để thách đấu.");
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
            inviteButton.setText("THÁCH ĐẤU  →");
        }
        selectionLabel.setText(target==null ? "Chọn đối thủ rảnh." : "Đối thủ: "+target.username());
        selectionLabel.setForeground(target==null ? GameTheme.MUTED : GameTheme.TEXT);
        selectionLabel.setToolTipText(target==null ? null : target.username());
    }

    private void updateMe() {
        PlayerInfo me = state.me();
        if (me != null) {
            meLabel.setText(NfsFonts.safe(me.username()));
            meLabel.setToolTipText(me.username());
            stats.setStats(new String[]{"ĐIỂM", "TRẬN THẮNG"}, new String[]{String.valueOf(me.points()), String.valueOf(me.wins())},
                    new Color[]{GameTheme.ACCENT, GameTheme.SUCCESS});
            showcase.setPlate(me.username());
        }
    }

    private String selectedUsername() {
        PlayerInfo p = model.playerAt(table.getSelectedRow());
        return p == null ? null : p.username();
    }

    private void setStatus(String text, boolean isError) {
        GameTheme.status(statusLabel,text,isError?GameTheme.ERROR:GameTheme.SUCCESS);
    }
}
