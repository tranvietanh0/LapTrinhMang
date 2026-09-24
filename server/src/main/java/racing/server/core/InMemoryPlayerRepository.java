package racing.server.core;

import racing.common.dto.RankRow;
import racing.server.db.PlayerRecord;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Bảng players trong bộ nhớ, mật khẩu lưu thô. Dùng cho test lõi và cho chế độ
 * {@code --memory} của GameServer (chạy thử không cần MySQL, dữ liệu mất khi tắt server).
 */
public final class InMemoryPlayerRepository implements PlayerRepository {

    final Map<Integer, PlayerRecord> byId = new LinkedHashMap<>();
    final Map<Integer, String> passwords = new LinkedHashMap<>();
    private int nextId = 1;

    public synchronized PlayerRecord add(String username, String password) {
        PlayerRecord r = new PlayerRecord(nextId++, username, "plain", 0, 0, 0, 0);
        byId.put(r.playerId(), r);
        passwords.put(r.playerId(), password);
        return r;
    }

    @Override
    public synchronized Optional<PlayerRecord> login(String username, String password) {
        for (PlayerRecord r : byId.values()) {
            if (r.username().equals(username) && passwords.get(r.playerId()).equals(password)) {
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }

    @Override
    public synchronized Optional<PlayerRecord> findById(int playerId) {
        return Optional.ofNullable(byId.get(playerId));
    }

    @Override
    public synchronized int create(String username, String password) throws SQLException {
        for (PlayerRecord r : byId.values()) {
            if (r.username().equals(username)) {
                throw new SQLIntegrityConstraintViolationException("Duplicate entry '" + username + "'");
            }
        }
        return add(username, password).playerId();
    }

    @Override
    public synchronized List<RankRow> leaderboard() {
        List<PlayerRecord> all = new ArrayList<>(byId.values());
        all.sort(Comparator.comparingInt(PlayerRecord::points).reversed()
                .thenComparing(Comparator.comparingInt(PlayerRecord::wins).reversed())
                .thenComparing(PlayerRecord::username));
        List<RankRow> rows = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) {
            PlayerRecord r = all.get(i);
            rows.add(new RankRow(i + 1, r.username(), r.points(), r.wins(), r.losses(), r.draws()));
        }
        return rows;
    }

    /** Cộng điểm như MatchDAO.saveResult. */
    synchronized void apply(int playerId, int points, int wins, int losses, int draws) {
        PlayerRecord r = byId.get(playerId);
        byId.put(playerId, new PlayerRecord(r.playerId(), r.username(), r.passwordHash(),
                r.points() + points, r.wins() + wins, r.losses() + losses, r.draws() + draws));
    }
}
