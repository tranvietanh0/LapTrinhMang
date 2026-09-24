package racing.client.ui;

import racing.client.net.NetworkClient;
import racing.common.GameConfig;
import racing.common.dto.InviteInfo;
import racing.common.dto.InviteReply;
import racing.common.net.Message;
import racing.common.net.MessageType;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Hộp thoại nhận lời mời: đếm ngược 30 → 0 theo {@code expiresAtMillis} của server,
 * thanh tiến trình, Chấp nhận / Từ chối gửi INVITE_REPLY. Hết giờ thì tự đóng, không gửi gì
 * (server tự báo TIMEOUT cho người mời). Không modal để sảnh vẫn cập nhật.
 */
public final class InviteDialog extends JDialog {

    private final NetworkClient net;
    private final InviteInfo invite;
    private final JLabel countLabel = new JLabel("", SwingConstants.CENTER);
    private final JProgressBar bar = new JProgressBar(0, GameConfig.INVITE_TIMEOUT_S);
    private final Timer timer;
    private boolean replied;

    public InviteDialog(JFrame owner, NetworkClient net, InviteInfo invite) {
        super(owner, "Lời mời thách đấu", false);
        this.net = net;
        this.invite = invite;

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(16, 20, 12, 20));
        JLabel text = new JLabel("<html><b>" + invite.fromUsername() + "</b> (" + invite.fromPoints()
                + " điểm) mời bạn thi đấu.</html>", SwingConstants.CENTER);
        text.setFont(text.getFont().deriveFont(14f));
        root.add(text, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(4, 4));
        countLabel.setFont(countLabel.getFont().deriveFont(Font.BOLD, 20f));
        bar.setStringPainted(false);
        center.add(countLabel, BorderLayout.NORTH);
        center.add(bar, BorderLayout.SOUTH);
        root.add(center, BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        JButton accept = new JButton("Chấp nhận");
        JButton reject = new JButton("Từ chối");
        accept.addActionListener(e -> reply(true));
        reject.addActionListener(e -> reply(false));
        buttons.add(accept);
        buttons.add(reject);
        root.add(buttons, BorderLayout.SOUTH);

        setContentPane(root);
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
        pack();
        setSize(Math.max(getWidth(), 360), getHeight());
        setLocationRelativeTo(owner);
    }

    public long inviteId() {
        return invite.inviteId();
    }

    private void tick() {
        long remainMs = invite.expiresAtMillis() - System.currentTimeMillis();
        int remain = (int) Math.max(0, Math.ceil(remainMs / 1000.0));
        countLabel.setText(remain + " giây");
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
