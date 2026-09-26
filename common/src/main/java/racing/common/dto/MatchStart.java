package racing.common.dto;

import java.io.Serializable;
import java.util.List;

/**
 * S→C MATCH_START, gửi tới cả hai người chơi khi lời mời được chấp nhận (hoặc khi bắt đầu ván rematch).
 *
 * @param roomId      id phòng
 * @param matchId     id bản ghi trong bảng matches
 * @param me          thông tin người nhận
 * @param opponent    thông tin đối thủ
 * @param trackLength chiều dài đường đua (mét), bằng GameConfig.TRACK_LENGTH
 * @param obstacles   danh sách xe cộ (chướng ngại vật chạy cùng chiều), giống nhau cho cả hai
 */
public record MatchStart(int roomId, int matchId, PlayerInfo me, PlayerInfo opponent,
                         double trackLength, List<Obstacle> obstacles) implements Serializable {
    private static final long serialVersionUID = 1L;
}
