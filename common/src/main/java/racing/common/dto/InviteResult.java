package racing.common.dto;

import java.io.Serializable;

/** S→C INVITE_RESULT, gửi tới người mời khi lời mời KHÔNG dẫn tới trận đấu. Khi được chấp nhận server gửi MATCH_START thay vì thông điệp này. */
public record InviteResult(long inviteId, String targetUsername, InviteStatus status) implements Serializable {
    private static final long serialVersionUID = 1L;
}
