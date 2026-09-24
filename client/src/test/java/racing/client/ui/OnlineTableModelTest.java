package racing.client.ui;

import org.junit.jupiter.api.Test;
import racing.common.dto.EndReason;
import racing.common.dto.MatchOutcome;
import racing.common.dto.PlayerInfo;
import racing.common.dto.PlayerStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OnlineTableModelTest {

    @Test
    void selfFirstThenAlphabetical() {
        OnlineTableModel m = new OnlineTableModel();
        m.setRows(List.of(
                new PlayerInfo(2, "bob", 1, 1, PlayerStatus.FREE),
                new PlayerInfo(3, "carol", 5, 4, PlayerStatus.IN_MATCH),
                new PlayerInfo(1, "alice", 3, 2, PlayerStatus.FREE)), "carol");
        assertEquals(3, m.getRowCount());
        assertEquals("carol (bạn)", m.getValueAt(0, 0));
        assertTrue(m.isSelfRow(0));
        assertEquals("alice", m.getValueAt(1, 0));
        assertEquals("bob", m.getValueAt(2, 0));
        assertFalse(m.isSelfRow(2));
        assertEquals("Đang thi đấu", m.getValueAt(0, 3));
        assertEquals("Rảnh", m.getValueAt(1, 3));
        assertEquals(3, m.getValueAt(1, 1));
        assertEquals(2, m.getValueAt(1, 2));
    }

    @Test
    void rowOfAndPlayerAt() {
        OnlineTableModel m = new OnlineTableModel();
        m.setRows(List.of(new PlayerInfo(1, "alice", 0, 0, PlayerStatus.FREE)), "zed");
        assertEquals(0, m.rowOf("alice"));
        assertEquals(-1, m.rowOf("nobody"));
        assertEquals("alice", m.playerAt(0).username());
        assertNull(m.playerAt(-1));
        assertNull(m.playerAt(5));
    }

    @Test
    void historyTextsAreVietnamese() {
        assertEquals("Thắng", HistoryTableModel.outcomeText(MatchOutcome.WIN));
        assertEquals("Hòa", HistoryTableModel.outcomeText(MatchOutcome.DRAW));
        assertEquals("Mất kết nối", HistoryTableModel.reasonText(EndReason.DISCONNECT));
        assertEquals("", HistoryTableModel.timeText(0));
        assertFalse(HistoryTableModel.timeText(1_700_000_000_000L).isEmpty());
    }

    @Test
    void registerValidation() {
        assertNull(RegisterDialog.validate("alice_1", "1234".toCharArray(), "1234".toCharArray()));
        assertTrue(RegisterDialog.validate("ab", "1234".toCharArray(), "1234".toCharArray())
                .contains("3"));
        assertTrue(RegisterDialog.validate("a b", "1234".toCharArray(), "1234".toCharArray())
                .contains("chữ, số"));
        assertTrue(RegisterDialog.validate("alice", "123".toCharArray(), "123".toCharArray())
                .contains("4"));
        assertTrue(RegisterDialog.validate("alice", "1234".toCharArray(), "12345".toCharArray())
                .contains("không khớp"));
    }
}
