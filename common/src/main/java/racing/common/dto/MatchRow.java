package racing.common.dto;

import java.io.Serializable;

/**
 * Một dòng lịch sử trận của người chơi (S→C MATCH_HISTORY), nhìn từ phía người xem.
 *
 * @param matchId          id trận
 * @param opponentUsername tên đối thủ
 * @param outcome          WIN / LOSE / DRAW / ABORTED của người xem
 * @param reason           lý do kết thúc (null nếu trận chưa kết thúc)
 * @param startedAtMillis  thời điểm bắt đầu (epoch ms)
 * @param endedAtMillis    thời điểm kết thúc (0 nếu chưa kết thúc)
 */
public record MatchRow(int matchId, String opponentUsername, MatchOutcome outcome, EndReason reason,
                       long startedAtMillis, long endedAtMillis) implements Serializable {
    private static final long serialVersionUID = 1L;
}
