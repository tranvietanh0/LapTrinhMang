package racing.server.db;

import racing.common.dto.RankRow;
import racing.server.core.PlayerRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Cài đặt {@link PlayerRepository} bằng {@link PlayerDAO} (MySQL). */
public final class JdbcPlayerRepository implements PlayerRepository {

    private final PlayerDAO dao = new PlayerDAO();

    @Override
    public Optional<PlayerRecord> login(String username, String password) throws SQLException {
        return dao.login(username, password);
    }

    @Override
    public Optional<PlayerRecord> findById(int playerId) throws SQLException {
        return dao.findById(playerId);
    }

    @Override
    public int create(String username, String password) throws SQLException {
        return dao.create(username, password);
    }

    @Override
    public List<RankRow> leaderboard() throws SQLException {
        return dao.getLeaderboard();
    }
}
