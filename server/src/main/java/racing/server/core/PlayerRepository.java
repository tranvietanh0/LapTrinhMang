package racing.server.core;

import racing.common.dto.RankRow;
import racing.server.db.PlayerRecord;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Cổng truy cập bảng players mà phần lõi server dùng. Cài đặt thật là
 * {@code JdbcPlayerRepository} (gọi PlayerDAO); test dùng bản trong bộ nhớ.
 */
public interface PlayerRepository {

    Optional<PlayerRecord> login(String username, String password) throws SQLException;

    Optional<PlayerRecord> findById(int playerId) throws SQLException;

    /** Trả về player_id; ném SQLIntegrityConstraintViolationException khi trùng tên. */
    int create(String username, String password) throws SQLException;

    List<RankRow> leaderboard() throws SQLException;
}
