package racing.server.db;

import racing.common.dto.PlayerInfo;
import racing.common.dto.PlayerStatus;

/** Một dòng bảng players (phía server, có password_hash). Không gửi ra client; dùng {@link #toInfo()}. */
public record PlayerRecord(int playerId, String username, String passwordHash,
                           int points, int wins, int losses, int draws) {

    public PlayerInfo toInfo(PlayerStatus status) {
        return new PlayerInfo(playerId, username, points, wins, status);
    }

    public PlayerInfo toInfo() {
        return toInfo(PlayerStatus.FREE);
    }
}
