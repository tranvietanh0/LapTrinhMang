package racing.server.core;

import racing.common.GameConfig;
import racing.common.dto.LoginRequest;
import racing.common.dto.LoginResult;
import racing.common.dto.MatchRow;
import racing.common.dto.RankRow;
import racing.server.Log;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.regex.Pattern;

/** Đăng ký tài khoản, bảng xếp hạng, lịch sử trận: các thao tác đọc/ghi DB không cần phòng. */
public final class AccountService {

    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_]{3,50}");
    private static final int MIN_PASSWORD = 4;

    private final PlayerRepository players;
    private final MatchRepository matches;

    public AccountService(PlayerRepository players, MatchRepository matches) {
        this.players = players;
        this.matches = matches;
    }

    public LoginResult register(LoginRequest req) {
        if (req == null || req.username() == null || req.password() == null) {
            return new LoginResult(false, "Thiếu tên tài khoản hoặc mật khẩu", null);
        }
        String username = req.username().trim();
        if (!USERNAME.matcher(username).matches()) {
            return new LoginResult(false, "Tên tài khoản 3–50 ký tự, chỉ gồm chữ, số và dấu gạch dưới", null);
        }
        if (req.password().length() < MIN_PASSWORD) {
            return new LoginResult(false, "Mật khẩu phải có ít nhất " + MIN_PASSWORD + " ký tự", null);
        }
        try {
            players.create(username, req.password());
            Log.info("đăng ký tài khoản mới: " + username);
            return new LoginResult(true, "Đăng ký thành công, hãy đăng nhập", null);
        } catch (SQLIntegrityConstraintViolationException e) {
            return new LoginResult(false, "Tên tài khoản đã tồn tại", null);
        } catch (SQLException e) {
            Log.warn("lỗi DB khi đăng ký " + username, e);
            return new LoginResult(false, "Lỗi máy chủ khi tạo tài khoản", null);
        }
    }

    public List<RankRow> leaderboard() throws SQLException {
        return players.leaderboard();
    }

    public List<MatchRow> history(Session s) throws SQLException {
        return matches.findRecentByPlayer(s.playerId(), GameConfig.HISTORY_LIMIT);
    }
}
