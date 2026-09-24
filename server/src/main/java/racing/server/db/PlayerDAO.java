package racing.server.db;

import racing.common.GameConfig;
import racing.common.dto.RankRow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Truy cập bảng players: đăng nhập, tra cứu, bảng xếp hạng, tạo tài khoản. */
public class PlayerDAO {

    private static final String SELECT_COLUMNS =
            "SELECT player_id, username, password_hash, points, wins, losses, draws FROM players ";

    public Optional<PlayerRecord> findByUsername(String username) throws SQLException {
        try (Connection c = DbConnection.get();
             PreparedStatement ps = c.prepareStatement(SELECT_COLUMNS + "WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public Optional<PlayerRecord> findById(int playerId) throws SQLException {
        try (Connection c = DbConnection.get();
             PreparedStatement ps = c.prepareStatement(SELECT_COLUMNS + "WHERE player_id = ?")) {
            ps.setInt(1, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    /**
     * Kiểm tra đăng nhập. Trả về bản ghi người chơi nếu đúng tài khoản và mật khẩu,
     * Optional.empty() nếu sai một trong hai (không phân biệt để tránh dò tên tài khoản).
     */
    public Optional<PlayerRecord> login(String username, String password) throws SQLException {
        if (username == null || username.isBlank() || password == null) {
            return Optional.empty();
        }
        Optional<PlayerRecord> found = findByUsername(username.trim());
        if (found.isPresent() && PasswordHasher.verify(password, found.get().passwordHash())) {
            return found;
        }
        return Optional.empty();
    }

    /** Tạo tài khoản mới. Trả về player_id. Ném SQLException (SQLIntegrityConstraintViolation) nếu trùng tên. */
    public int create(String username, String password) throws SQLException {
        try (Connection c = DbConnection.get();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO players (username, password_hash) VALUES (?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username.trim());
            ps.setString(2, PasswordHasher.hash(password));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    /** Bảng xếp hạng: points DESC, wins DESC, cùng điểm cùng thắng thì theo tên. */
    public List<RankRow> getLeaderboard() throws SQLException {
        return getLeaderboard(GameConfig.LEADERBOARD_LIMIT);
    }

    public List<RankRow> getLeaderboard(int limit) throws SQLException {
        String sql = "SELECT username, points, wins, losses, draws FROM players "
                + "ORDER BY points DESC, wins DESC, username ASC LIMIT ?";
        List<RankRow> rows = new ArrayList<>();
        try (Connection c = DbConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                int rank = 0;
                while (rs.next()) {
                    rank++;
                    rows.add(new RankRow(rank, rs.getString("username"), rs.getInt("points"),
                            rs.getInt("wins"), rs.getInt("losses"), rs.getInt("draws")));
                }
            }
        }
        return rows;
    }

    static PlayerRecord map(ResultSet rs) throws SQLException {
        return new PlayerRecord(rs.getInt("player_id"), rs.getString("username"),
                rs.getString("password_hash"), rs.getInt("points"), rs.getInt("wins"),
                rs.getInt("losses"), rs.getInt("draws"));
    }
}
