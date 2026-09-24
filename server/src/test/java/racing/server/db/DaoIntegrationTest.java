package racing.server.db;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import racing.common.dto.EndReason;
import racing.common.dto.RankRow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Test tích hợp với MySQL thật. Chỉ chạy khi có biến môi trường RACING_TEST_DB_URL,
 * ví dụ (sau docker compose up -d):
 * <pre>
 *   RACING_TEST_DB_URL=jdbc:mysql://localhost:3306/racing RACING_TEST_DB_USER=racing RACING_TEST_DB_PASSWORD=racing mvn test
 * </pre>
 * Test tự dọn dữ liệu mình tạo (tài khoản có tiền tố "t_").
 */
class DaoIntegrationTest {

    private static final String PREFIX = "t_";
    private final PlayerDAO players = new PlayerDAO();
    private final MatchDAO matches = new MatchDAO();

    @BeforeAll
    static void configure() {
        String url = System.getenv("RACING_TEST_DB_URL");
        assumeTrue(url != null && !url.isBlank(), "RACING_TEST_DB_URL chưa đặt, bỏ qua test tích hợp");
        DbConnection.configure(url,
                System.getenv().getOrDefault("RACING_TEST_DB_USER", "racing"),
                System.getenv().getOrDefault("RACING_TEST_DB_PASSWORD", "racing"));
    }

    @BeforeEach
    void cleanTestRows() throws SQLException {
        try (Connection c = DbConnection.get(); Statement st = c.createStatement()) {
            st.executeUpdate("DELETE me FROM match_events me JOIN players p ON p.player_id = me.player_id "
                    + "WHERE p.username LIKE '" + PREFIX + "%'");
            st.executeUpdate("DELETE m FROM matches m JOIN players p ON p.player_id = m.player1_id "
                    + "WHERE p.username LIKE '" + PREFIX + "%'");
            st.executeUpdate("DELETE FROM players WHERE username LIKE '" + PREFIX + "%'");
        }
    }

    @Test
    void loginSucceedsWithCorrectPasswordAndFailsOtherwise() throws SQLException {
        int id = players.create(PREFIX + "login", "secret");
        Optional<PlayerRecord> ok = players.login(PREFIX + "login", "secret");
        assertTrue(ok.isPresent());
        assertEquals(id, ok.get().playerId());
        assertFalse(players.login(PREFIX + "login", "wrong").isPresent());
        assertFalse(players.login(PREFIX + "nobody", "secret").isPresent());
    }

    @Test
    void winnerGetsOnePointLoserGetsZero() throws SQLException {
        int a = players.create(PREFIX + "a", "x");
        int b = players.create(PREFIX + "b", "x");
        int matchId = matches.createMatch("R1", a, b);

        matches.saveResult(matchId, a, b, a, EndReason.FINISH);

        assertStats(a, 1, 1, 0, 0);
        assertStats(b, 0, 0, 1, 0);
        assertMatch(matchId, a, "FINISHED", "FINISH");
    }

    @Test
    void drawGivesOnePointEach() throws SQLException {
        int a = players.create(PREFIX + "a", "x");
        int b = players.create(PREFIX + "b", "x");
        int matchId = matches.createMatch("R2", a, b);

        matches.saveResult(matchId, a, b, null, EndReason.DRAW);

        assertStats(a, 1, 0, 0, 1);
        assertStats(b, 1, 0, 0, 1);
        assertMatch(matchId, null, "FINISHED", "DRAW");
    }

    @Test
    void quitAndDisconnectCountAsLossForThatPlayer() throws SQLException {
        int a = players.create(PREFIX + "a", "x");
        int b = players.create(PREFIX + "b", "x");
        int m1 = matches.createMatch("R3", a, b);
        matches.saveResult(m1, a, b, b, EndReason.QUIT);        // a thoát → b thắng
        int m2 = matches.createMatch("R4", a, b);
        matches.saveResult(m2, a, b, a, EndReason.DISCONNECT);  // b mất kết nối → a thắng

        assertStats(a, 1, 1, 1, 0);
        assertStats(b, 1, 1, 1, 0);
    }

    @Test
    void abortedMatchChangesNoPoints() throws SQLException {
        int a = players.create(PREFIX + "a", "x");
        int b = players.create(PREFIX + "b", "x");
        int matchId = matches.createMatch("R5", a, b);

        matches.saveResult(matchId, a, b, null, EndReason.ABORTED);

        assertStats(a, 0, 0, 0, 0);
        assertStats(b, 0, 0, 0, 0);
        assertMatch(matchId, null, "ABORTED", "ABORTED");
    }

    @Test
    void invalidWinnerIsRejectedAndNothingChanges() throws SQLException {
        int a = players.create(PREFIX + "a", "x");
        int b = players.create(PREFIX + "b", "x");
        int c = players.create(PREFIX + "c", "x");
        int matchId = matches.createMatch("R6", a, b);

        assertThrows(IllegalArgumentException.class,
                () -> matches.saveResult(matchId, a, b, c, EndReason.FINISH));
        assertThrows(IllegalArgumentException.class,
                () -> matches.saveResult(matchId, a, b, null, EndReason.FINISH));
        assertThrows(IllegalArgumentException.class,
                () -> matches.saveResult(matchId, a, b, a, EndReason.DRAW));

        assertStats(a, 0, 0, 0, 0);
        assertMatch(matchId, null, "PLAYING", null);
    }

    @Test
    void unknownMatchRollsBack() throws SQLException {
        int a = players.create(PREFIX + "a", "x");
        int b = players.create(PREFIX + "b", "x");

        assertThrows(SQLException.class, () -> matches.saveResult(-1, a, b, a, EndReason.FINISH));

        assertStats(a, 0, 0, 0, 0);
        assertStats(b, 0, 0, 0, 0);
    }

    @Test
    void leaderboardOrdersByPointsThenWins() throws SQLException {
        int a = players.create(PREFIX + "lb_a", "x");
        int b = players.create(PREFIX + "lb_b", "x");
        int c = players.create(PREFIX + "lb_c", "x");
        // a: 2 điểm 2 thắng; b: 2 điểm 1 thắng 1 hòa; c: 1 điểm 1 hòa
        matches.saveResult(matches.createMatch("L1", a, c), a, c, a, EndReason.FINISH);
        matches.saveResult(matches.createMatch("L2", a, b), a, b, a, EndReason.FINISH);
        matches.saveResult(matches.createMatch("L3", b, c), b, c, null, EndReason.DRAW);
        matches.saveResult(matches.createMatch("L4", b, c), b, c, b, EndReason.FINISH);

        List<RankRow> rows = players.getLeaderboard(1000).stream()
                .filter(r -> r.username().startsWith(PREFIX + "lb_")).toList();
        assertEquals(List.of(PREFIX + "lb_a", PREFIX + "lb_b", PREFIX + "lb_c"),
                rows.stream().map(RankRow::username).toList());
        assertEquals(2, rows.get(0).points());
        assertEquals(2, rows.get(1).points());
        assertEquals(1, rows.get(1).wins());
        assertEquals(1, rows.get(2).points());
    }

    @Test
    void addEventStoresJsonPayload() throws SQLException {
        int a = players.create(PREFIX + "a", "x");
        int b = players.create(PREFIX + "b", "x");
        int matchId = matches.createMatch("E1", a, b);
        matches.addEvent(matchId, a, "COLLISION", "{\"lane\":1,\"distance\":420.5}");
        matches.addEvent(matchId, b, "FINISH", null);

        try (Connection c = DbConnection.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT COUNT(*) FROM match_events WHERE match_id = ?")) {
            ps.setInt(1, matchId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertEquals(2, rs.getInt(1));
            }
        }
    }

    private void assertStats(int playerId, int points, int wins, int losses, int draws) throws SQLException {
        PlayerRecord r = players.findById(playerId).orElseThrow();
        assertEquals(points, r.points(), "points");
        assertEquals(wins, r.wins(), "wins");
        assertEquals(losses, r.losses(), "losses");
        assertEquals(draws, r.draws(), "draws");
    }

    private void assertMatch(int matchId, Integer winnerId, String status, String reason) throws SQLException {
        try (Connection c = DbConnection.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT winner_id, status, end_reason FROM matches WHERE match_id = ?")) {
            ps.setInt(1, matchId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                Integer w = rs.getObject("winner_id", Integer.class);
                if (winnerId == null) {
                    assertNull(w);
                } else {
                    assertEquals(winnerId, w);
                }
                assertEquals(status, rs.getString("status"));
                assertEquals(reason, rs.getString("end_reason"));
            }
        }
    }
}
