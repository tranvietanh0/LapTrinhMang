package racing.server.core;

import racing.common.dto.EndReason;
import racing.common.dto.MatchRow;

import java.sql.SQLException;
import java.util.List;

/**
 * Cổng truy cập bảng matches / match_events mà phần lõi server dùng. Cài đặt thật là
 * {@code JdbcMatchRepository} (gọi MatchDAO); test dùng bản trong bộ nhớ.
 */
public interface MatchRepository {

    int createMatch(String roomCode, int player1Id, int player2Id) throws SQLException;

    void saveResult(int matchId, int player1Id, int player2Id, Integer winnerId, EndReason reason)
            throws SQLException;

    void addEvent(int matchId, int playerId, String eventType, String payloadJson) throws SQLException;

    List<MatchRow> findRecentByPlayer(int playerId, int limit) throws SQLException;
}
