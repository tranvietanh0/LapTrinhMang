package racing.client.ui.race;

import racing.client.ui.GameTheme;
import racing.client.ui.theme.HeroCardLayout;
import racing.client.ui.theme.NfsBackdrop;
import racing.client.ui.theme.Responsive;
import racing.client.ui.theme.ScreenFrame;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/** Hỏi có thi đấu tiếp với đối thủ không, cùng phong cách với đăng nhập. Trả về true nếu Đồng ý. */
public final class RematchDialog {

    private RematchDialog() {
    }

    public static boolean ask(Window owner, String opponent) {
        boolean[] agreed = {false};
        JDialog dlg = create(owner, opponent, agreed);
        dlg.setVisible(true);
        dlg.dispose();
        return agreed[0];
    }

    /** Dựng hộp thoại chưa hiển thị; {@code agreed[0]} thành true khi bấm Đồng ý. Đóng cửa sổ / Esc = từ chối. */
    public static JDialog create(Window owner, String opponent, boolean[] agreed) {
        float scale = owner != null ? Responsive.scaleOf(owner) : 1f;
        JDialog dlg = new JDialog(owner, "Thi đấu tiếp", Dialog.ModalityType.APPLICATION_MODAL);
        JButton yes = GameTheme.button("ĐỒNG Ý  »", true);
        JButton no = GameTheme.button("Từ chối", false);

        JPanel form = GameTheme.column();
        form.add(GameTheme.eyebrow("REMATCH / ĐẤU LẠI"));
        GameTheme.gap(form, 6);
        form.add(GameTheme.heading("THI ĐẤU TIẾP?"));
        GameTheme.gap(form, 10);
        form.add(GameTheme.label("Thi đấu tiếp với " + opponent + "?", GameTheme.STRONG, GameTheme.TEXT));
        GameTheme.gap(form, 4);
        form.add(GameTheme.label("Ván mới chỉ bắt đầu khi cả hai cùng đồng ý.", GameTheme.BODY, GameTheme.MUTED));
        GameTheme.gap(form, 22);
        GameTheme.fullWidth(yes, GameTheme.CONTROL_H);
        form.add(yes);
        GameTheme.gap(form, 8);
        GameTheme.fullWidth(no, GameTheme.CONTROL_H);
        form.add(no);

        JPanel card = GameTheme.card(new BorderLayout(), 26);
        card.add(form, BorderLayout.CENTER);
        RematchCard animated = new RematchCard(card, yes);   // pops in, then the primary button glows gently
        NfsBackdrop root = new NfsBackdrop();
        root.add(animated);
        root.setLayout(new HeroCardLayout(null, animated, 460, 0, 24));
        GameTheme.decorate(dlg);
        ScreenFrame.fit(dlg, Math.round(620 * scale), Math.round(520 * scale));
        ScreenFrame.install(dlg, root, owner);
        dlg.setMinimumSize(new Dimension(420, 380));
        dlg.getRootPane().setDefaultButton(yes);
        yes.addActionListener(e -> {
            agreed[0] = true;
            dlg.dispose();
        });
        no.addActionListener(e -> dlg.dispose());
        dlg.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "decline");
        dlg.getRootPane().getActionMap().put("decline", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dlg.dispose();
            }
        });
        dlg.validate();
        dlg.setSize(dlg.getWidth(), Math.min(dlg.getHeight(), card.getPreferredSize().height + Math.round(48 * Responsive.scaleOf(dlg))));
        dlg.setLocationRelativeTo(owner);
        return dlg;
    }
}
