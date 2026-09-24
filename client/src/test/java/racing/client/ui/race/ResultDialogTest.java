package racing.client.ui.race;

import org.junit.jupiter.api.Test;
import racing.common.dto.EndReason;
import racing.common.dto.MatchOutcome;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResultDialogTest {

    @Test
    void reasonTextDependsOnOutcome() {
        assertEquals("bạn về đích trước", ResultDialog.reasonText(EndReason.FINISH, MatchOutcome.WIN));
        assertEquals("đối thủ về đích trước", ResultDialog.reasonText(EndReason.FINISH, MatchOutcome.LOSE));
        assertEquals("hai xe về đích cùng lúc", ResultDialog.reasonText(EndReason.DRAW, MatchOutcome.DRAW));
        assertEquals("đối thủ đã thoát trận", ResultDialog.reasonText(EndReason.QUIT, MatchOutcome.WIN));
        assertEquals("bạn mất kết nối", ResultDialog.reasonText(EndReason.DISCONNECT, MatchOutcome.LOSE));
        assertEquals("không rõ", ResultDialog.reasonText(null, MatchOutcome.ABORTED));
    }
}
