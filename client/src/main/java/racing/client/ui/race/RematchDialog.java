package racing.client.ui.race;

import javax.swing.JOptionPane;
import java.awt.Window;

/** Hỏi có thi đấu tiếp với đối thủ không. Trả về true nếu Đồng ý. */
public final class RematchDialog {

    private RematchDialog() {
    }

    public static boolean ask(Window owner, String opponent) {
        Object[] options = {"Đồng ý", "Từ chối"};
        int choice = JOptionPane.showOptionDialog(owner,
                "Thi đấu tiếp với " + opponent + "?\nVán mới chỉ bắt đầu khi cả hai cùng đồng ý.",
                "Thi đấu tiếp", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);
        return choice == JOptionPane.YES_OPTION;
    }
}
