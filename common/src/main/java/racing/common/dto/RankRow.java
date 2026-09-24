package racing.common.dto;

import java.io.Serializable;

/** Một dòng bảng xếp hạng (S→C LEADERBOARD). Sắp theo points DESC rồi wins DESC. */
public record RankRow(int rank, String username, int points, int wins, int losses, int draws)
        implements Serializable {
    private static final long serialVersionUID = 1L;
}
