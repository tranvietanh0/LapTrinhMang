package racing.common.dto;

import java.io.Serializable;

/**
 * S→C MATCH_RESULT, gửi tới cả hai người chơi khi trận kết thúc. Server tạo riêng cho từng người
 * nên outcome là kết quả của người nhận.
 *
 * @param roomId          id phòng
 * @param matchId         id bản ghi matches
 * @param outcome         WIN / LOSE / DRAW / ABORTED của người nhận
 * @param reason          lý do kết thúc
 * @param winnerUsername  người thắng, null khi hòa hoặc hủy
 * @param myPoints        tổng điểm mới của người nhận
 * @param opponentPoints  tổng điểm mới của đối thủ
 * @param elapsedMillis   thời gian trận (ms)
 */
public record MatchResult(int roomId, int matchId, MatchOutcome outcome, EndReason reason,
                          String winnerUsername, int myPoints, int opponentPoints, long elapsedMillis)
        implements Serializable {
    private static final long serialVersionUID = 1L;
}
