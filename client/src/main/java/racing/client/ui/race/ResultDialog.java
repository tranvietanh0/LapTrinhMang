package racing.client.ui.race;

import racing.common.dto.EndReason;
import racing.common.dto.MatchOutcome;
import racing.common.dto.MatchResult;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import java.awt.Window;

/** Hộp kết quả trận: thắng / thua / hòa, lý do và điểm mới của hai bên. Modal, đóng bằng OK. */
public final class ResultDialog {

    private ResultDialog() {
    }

    public static void show(Window owner, MatchResult r, String me, String opponent) {
        String title = switch (r.outcome()) {
            case WIN -> "Bạn THẮNG!";
            case LOSE -> "Bạn thua";
            case DRAW -> "Hòa";
            case ABORTED -> "Trận bị hủy";
        };
        String body = "<html><div style='width:320px;font-size:12pt'>"
                + "<p><b>" + title + "</b></p>"
                + "<p>Lý do: " + reasonText(r.reason(), r.outcome()) + "</p>"
                + "<p>Điểm hiện tại: " + me + " <b>" + r.myPoints() + "</b> · "
                + opponent + " <b>" + r.opponentPoints() + "</b></p>"
                + (r.elapsedMillis() > 0
                        ? "<p>Thời gian đua: " + String.format("%.1f", r.elapsedMillis() / 1000.0) + " s</p>" : "")
                + "</div></html>";
        int type = switch (r.outcome()) {
            case WIN -> JOptionPane.INFORMATION_MESSAGE;
            case LOSE -> JOptionPane.WARNING_MESSAGE;
            default -> JOptionPane.PLAIN_MESSAGE;
        };
        JOptionPane pane = new JOptionPane(body, type, JOptionPane.DEFAULT_OPTION);
        JDialog dlg = pane.createDialog(owner, "Kết quả trận đấu");
        dlg.setModal(true);
        dlg.setVisible(true);
        dlg.dispose();
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
