package racing.server.db;

import racing.common.dto.EndReason;
import racing.common.dto.MatchRow;
import racing.server.core.MatchRepository;

import java.sql.SQLException;
import java.util.List;

/** Cài đặt {@link MatchRepository} bằng {@link MatchDAO} (MySQL). */
public final class JdbcMatchRepository implements MatchRepository {

    private final MatchDAO dao = new MatchDAO();

    @Override
    public int createMatch(String roomCode, int player1Id, int player2Id) throws SQLException {
        return dao.createMatch(roomCode, player1Id, player2Id);
    }

    @Override
    public void saveResult(int matchId, int player1Id, int player2Id, Integer winnerId, EndReason reason)
            throws SQLException {
        dao.saveResult(matchId, player1Id, player2Id, winnerId, reason);
    }

    @Override
    public void addEvent(int matchId, int playerId, String eventType, String payloadJson) throws SQLException {
        dao.addEvent(matchId, playerId, eventType, payloadJson);
    }

    @Override
    public List<MatchRow> findRecentByPlayer(int playerId, int limit) throws SQLException {
        return dao.findRecentByPlayer(playerId, limit);
    }
}
