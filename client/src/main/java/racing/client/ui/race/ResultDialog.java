package racing.client.ui.race;

import racing.common.dto.EndReason;
import racing.common.dto.MatchOutcome;
import racing.common.dto.MatchResult;

import racing.client.ui.theme.Responsive;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/** Thẻ kết quả trận kiểu NFS: VICTORY / DEFEAT / DRAW, lý do, thời gian, tốc độ trung bình và điểm. Modal, đóng bằng TIẾP TỤC / Enter / Esc. */
public final class ResultDialog {

    private ResultDialog() {
    }

    public static void show(Window owner, MatchResult r, String me, String opponent) {
        JDialog dlg = create(owner, r, me, opponent);
        dlg.setVisible(true);
        dlg.dispose();
    }

    /** Dựng hộp thoại chưa hiển thị (dùng cho {@link #show} và để chụp ảnh kiểm tra giao diện). */
    public static JDialog create(Window owner, MatchResult r, String me, String opponent) {
        float scale = owner != null ? Responsive.scaleOf(owner) : 1f;
        JDialog dlg = new JDialog(owner, "Kết quả trận đấu", Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setUndecorated(true);
        try {
            dlg.setBackground(new Color(0, 0, 0, 0));   // góc vát trong suốt khi nền tảng cho phép
        } catch (UnsupportedOperationException ignored) {
            // nền đen phía sau góc vát: chấp nhận được
        }
        RaceResultCard card = new RaceResultCard(r, me, opponent, scale, dlg::dispose);
        dlg.setContentPane(card);
        Responsive.pin(dlg.getRootPane(), scale);
        dlg.getRootPane().setDefaultButton(card.closeButton());
        dlg.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        dlg.getRootPane().getActionMap().put("close", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dlg.dispose();
            }
        });
        dlg.pack();
        dlg.setLocationRelativeTo(owner);
        return dlg;
    }

    static String reasonText(EndReason reason, MatchOutcome outcome) {
        if (reason == null) {
            return "không rõ";
        }
        return switch (reason) {
            case FINISH -> outcome == MatchOutcome.WIN ? "bạn về đích trước" : "đối thủ về đích trước";
            case DRAW -> "hai xe về đích cùng lúc";
            case QUIT -> outcome == MatchOutcome.WIN ? "đối thủ đã thoát trận" : "bạn đã thoát trận";
            case DISCONNECT -> outcome == MatchOutcome.WIN ? "đối thủ mất kết nối" : "bạn mất kết nối";
            case ABORTED -> "trận bị hủy, không tính điểm";
        };
    }
}
