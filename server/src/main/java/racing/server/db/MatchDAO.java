package racing.server.db;

import racing.common.GameConfig;
import racing.common.dto.EndReason;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Truy cập bảng matches và match_events. {@link #saveResult} chạy trong một transaction:
 * cập nhật trận và cộng điểm cho cả hai người chơi cùng lúc, lỗi giữa chừng thì rollback.
 */
public class MatchDAO {

    /** Tạo bản ghi trận ở trạng thái PLAYING khi phòng bắt đầu đua. Trả về match_id. */
    public int createMatch(String roomCode, int player1Id, int player2Id) throws SQLException {
        try (Connection c = DbConnection.get();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO matches (room_code, player1_id, player2_id) VALUES (?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, roomCode);
            ps.setInt(2, player1Id);
            ps.setInt(3, player2Id);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    /**
     * Chốt kết quả trận và cộng điểm trong MỘT transaction.
     *
     * @param matchId   trận cần chốt
     * @param player1Id người chơi 1
     * @param player2Id người chơi 2
     * @param winnerId  id người thắng; null khi hòa (reason DRAW) hoặc hủy (reason ABORTED)
     * @param reason    lý do kết thúc, quyết định cách cộng điểm
     */
    public void saveResult(int matchId, int player1Id, int player2Id, Integer winnerId, EndReason reason)
            throws SQLException {
        if (reason == null) {
            throw new IllegalArgumentException("reason must not be null");
        }
        boolean aborted = reason == EndReason.ABORTED;
        boolean draw = reason == EndReason.DRAW;
        if (!aborted && !draw && winnerId == null) {
            throw new IllegalArgumentException("winnerId is required for reason " + reason);
        }
        if ((aborted || draw) && winnerId != null) {
            throw new IllegalArgumentException("winnerId must be null for reason " + reason);
        }
        if (winnerId != null && winnerId != player1Id && winnerId != player2Id) {
            throw new IllegalArgumentException("winnerId " + winnerId + " is not a player of this match");
        }

        String updateMatch = "UPDATE matches SET winner_id = ?, status = ?, end_reason = ?, ended_at = NOW() "
                + "WHERE match_id = ?";
        String updatePlayer = "UPDATE players SET points = points + ?, wins = wins + ?, "
                + "losses = losses + ?, draws = draws + ? WHERE player_id = ?";

        Connection c = DbConnection.get();
        try {
            c.setAutoCommit(false);
            try (PreparedStatement m = c.prepareStatement(updateMatch);
                 PreparedStatement p = c.prepareStatement(updatePlayer)) {
                if (winnerId == null) {
                    m.setNull(1, java.sql.Types.INTEGER);
                } else {
                    m.setInt(1, winnerId);
                }
                m.setString(2, aborted ? "ABORTED" : "FINISHED");
                m.setString(3, reason.name());
                m.setInt(4, matchId);
                if (m.executeUpdate() != 1) {
                    throw new SQLException("matches không có match_id = " + matchId);
                }

                if (!aborted) {
                    for (int pid : new int[] {player1Id, player2Id}) {
                        if (draw) {
                            bindDelta(p, GameConfig.POINTS_DRAW, 0, 0, 1, pid);
                        } else if (pid == winnerId) {
                            bindDelta(p, GameConfig.POINTS_WIN, 1, 0, 0, pid);
                        } else {
                            bindDelta(p, 0, 0, 1, 0, pid);
                        }
                        p.addBatch();
                    }
                    p.executeBatch();
                }
            }
            c.commit();
        } catch (SQLException | RuntimeException e) {
            try {
                c.rollback();
            } catch (SQLException ignored) {
                // lỗi rollback không che lỗi gốc
            }
            throw e;
        } finally {
            try {
                c.setAutoCommit(true);
            } finally {
                c.close();
            }
        }
    }

    private static void bindDelta(PreparedStatement p, int points, int win, int loss, int draw, int playerId)
            throws SQLException {
        p.setInt(1, points);
        p.setInt(2, win);
        p.setInt(3, loss);
        p.setInt(4, draw);
        p.setInt(5, playerId);
    }

    /** Ghi một sự kiện trong trận. payloadJson có thể null; nếu có phải là JSON hợp lệ (MySQL kiểm tra). */
    public void addEvent(int matchId, int playerId, String eventType, String payloadJson) throws SQLException {
        try (Connection c = DbConnection.get();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO match_events (match_id, player_id, event_type, payload) VALUES (?, ?, ?, ?)")) {
            ps.setInt(1, matchId);
            ps.setInt(2, playerId);
            ps.setString(3, eventType);
            if (payloadJson == null) {
                ps.setNull(4, java.sql.Types.VARCHAR);
            } else {
                ps.setString(4, payloadJson);
            }
            ps.executeUpdate();
        }
    }
}
