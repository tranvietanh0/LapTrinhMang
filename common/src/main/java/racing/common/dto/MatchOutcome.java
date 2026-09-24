package racing.common.dto;

/** Kết quả trận đấu nhìn từ phía người nhận thông điệp. */
public enum MatchOutcome {
    WIN,
    LOSE,
    DRAW,
    /** Trận bị hủy (ví dụ cả hai cùng mất kết nối), không ai được điểm. */
    ABORTED
}
