package racing.common.dto;

/** Trạng thái người chơi trong sảnh. */
public enum PlayerStatus {
    /** Rảnh, có thể được thách đấu. */
    FREE,
    /** Đang trong phòng đua (đếm ngược, đang đua hoặc đang chờ rematch). */
    IN_MATCH
}
