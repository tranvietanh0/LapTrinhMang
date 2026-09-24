package racing.common.dto;

import java.io.Serializable;

/**
 * S→C RACE_UPDATE, gửi tới cả hai người chơi mỗi tick. Server tạo riêng một RaceState cho từng
 * người để "me" luôn là xe của người nhận.
 *
 * @param roomId         id phòng
 * @param tick           số thứ tự tick kể từ GO, tăng dần
 * @param me             xe của người nhận
 * @param opponent       xe đối thủ
 * @param elapsedMillis  thời gian đã đua (ms)
 */
public record RaceState(int roomId, long tick, CarSnapshot me, CarSnapshot opponent, long elapsedMillis)
        implements Serializable {
    private static final long serialVersionUID = 1L;
}
