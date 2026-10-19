package racing.client.ui;

import racing.client.net.NetworkClient;
import racing.client.ui.theme.*;
import racing.common.GameConfig;
import racing.common.dto.InviteInfo;
import racing.common.dto.InviteReply;
import racing.common.net.Message;
import racing.common.net.MessageType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Hộp thoại nhận lời mời: đếm ngược 30 → 0 theo {@code expiresAtMillis} của server,
 * thanh tiến trình, Chấp nhận / Từ chối gửi INVITE_REPLY. Hết giờ thì tự đóng, không gửi gì
 * (server tự báo TIMEOUT cho người mời). Không modal để sảnh vẫn cập nhật.
 */
public final class InviteDialog extends JDialog {

    private static final int URGENT_SECONDS = 5;

    private final NetworkClient net;
    private final InviteInfo invite;
    private final JLabel countLabel = new JLabel("");
    private final MenuCountdownBar bar = new MenuCountdownBar(GameConfig.INVITE_TIMEOUT_S);
    private final Timer timer;
    private boolean replied;

    public InviteDialog(JFrame owner, NetworkClient net, InviteInfo invite) {
        super(owner, "Lời mời thách đấu", false);
        this.net = net;
        this.invite = invite;

        Responsive.font(countLabel, NfsFonts.hero(46f));
        countLabel.setForeground(GameTheme.ACCENT);
        countLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton accept = GameTheme.button("CHẤP NHẬN  →", true);
        JButton reject = GameTheme.button("Từ chối", false);
        GameTheme.fullWidth(accept, GameTheme.CONTROL_H);
        GameTheme.fullWidth(reject, GameTheme.CONTROL_H);
        JPanel buttons = GameTheme.panel(new GridLayout(1, 2, 12, 0));
        buttons.add(accept);
        buttons.add(reject);
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);

        RevealPanel col = RevealPanel.column();
        col.add(GameTheme.eyebrow("INCOMING CHALLENGE / LỜI MỜI")); GameTheme.gap(col, 2);
        col.add(GameTheme.label("THÁCH ĐẤU!", NfsFonts.hero(58f), GameTheme.TEXT)); GameTheme.gap(col, 12);
        col.add(new MenuChallengeBanner("ĐỐI THỦ MỜI BẠN", invite.fromUsername(), invite.fromPoints() + " ĐIỂM")); GameTheme.gap(col, 10);
        col.add(GameTheme.label("Một trận 1 vs 1 trên đường đua 2.500 m.", GameTheme.BODY, GameTheme.MUTED)); GameTheme.gap(col, 12);
        col.add(countLabel); GameTheme.gap(col, 4);
        col.add(bar); GameTheme.gap(col, 20);
        col.add(buttons);

        JPanel card = GameTheme.card(new BorderLayout(), 26);
        card.add(col, BorderLayout.CENTER);
        RevealPanel stage = new RevealPanel(null);
        stage.add(RevealPanel.style(card, RevealPanel.Style.POP));
        stage.setLayout(new HeroCardLayout(null, card, 520, 0, 24));
        NfsBackdrop root = new NfsBackdrop();
        root.setLayout(new BorderLayout());
        root.add(stage, BorderLayout.CENTER);
        // entrance: the card pops in with a small overshoot, its lines follow quickly
        stage.reveal(0, 0, card);
        col.reveal(140, 30);
        GameTheme.decorate(this);
        float scale = Responsive.scaleOf(owner);
        ScreenFrame.fit(this, Math.round(660 * scale), Math.round(760 * scale));
        ScreenFrame.install(this, root, owner);
        setMinimumSize(new Dimension(420, 420));
        accept.addActionListener(e -> reply(true));
        reject.addActionListener(e -> reply(false));

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                reply(false);
            }

            @Override
            public void windowClosed(WindowEvent e) {
                timer.stop();
            }
        });
        timer = new Timer(250, e -> tick());
        tick();
        timer.start();
        validate();
        setSize(getWidth(), Math.min(getHeight(), card.getPreferredSize().height + Math.round(48 * Responsive.scaleOf(this))));
        setLocationRelativeTo(owner);
    }

    public long inviteId() {
        return invite.inviteId();
    }

    private void tick() {
        long remainMs = invite.expiresAtMillis() - System.currentTimeMillis();
        int remain = (int) Math.max(0, Math.ceil(remainMs / 1000.0));
        countLabel.setText(remain + " giây");
        countLabel.setForeground(remain <= URGENT_SECONDS ? NfsPalette.RED : GameTheme.ACCENT);
        bar.setValue(Math.min(GameConfig.INVITE_TIMEOUT_S, remain));
        if (remainMs <= 0) {
            dispose();   // hết giờ: không gửi gì
        }
    }

    private void reply(boolean accept) {
        if (replied) {
            return;
        }
        replied = true;
        net.send(new Message(MessageType.INVITE_REPLY, new InviteReply(invite.inviteId(), accept)));
        dispose();
    }
}
