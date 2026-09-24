package racing.common.dto;

import java.io.Serializable;

/** C→S INVITE_REPLY. accept = true là Chấp nhận (OK), false là Từ chối (Reject). */
public record InviteReply(long inviteId, boolean accept) implements Serializable {
    private static final long serialVersionUID = 1L;
}
