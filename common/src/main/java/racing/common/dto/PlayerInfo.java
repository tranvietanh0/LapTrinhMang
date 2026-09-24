package racing.common.dto;

import java.io.Serializable;

/**
 * Một dòng trong danh sách online (S→C ONLINE_LIST) và thông tin người chơi hiện tại.
 *
 * @param playerId id trong bảng players
 * @param username tên đăng nhập
 * @param points   tổng điểm
 * @param wins     số trận thắng
 * @param status   Rảnh hay Đang thi đấu
 */
public record PlayerInfo(int playerId, String username, int points, int wins, PlayerStatus status)
        implements Serializable {
    private static final long serialVersionUID = 1L;

    public PlayerInfo withStatus(PlayerStatus newStatus) {
        return new PlayerInfo(playerId, username, points, wins, newStatus);
    }

    public PlayerInfo withStats(int newPoints, int newWins) {
        return new PlayerInfo(playerId, username, newPoints, newWins, status);
    }
}
