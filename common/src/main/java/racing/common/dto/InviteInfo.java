package racing.common.dto;

import java.io.Serializable;

/**
 * S→C INVITE_RECEIVED, gửi tới người được mời.
 *
 * @param inviteId       id lời mời do server cấp, dùng lại trong InviteReply
 * @param fromUsername   người thách đấu
 * @param fromPoints     điểm của người thách đấu (hiển thị trong hộp thoại)
 * @param expiresAtMillis thời điểm hết hạn theo đồng hồ server (client chỉ dùng để hiển thị đếm ngược)
 */
public record InviteInfo(long inviteId, String fromUsername, int fromPoints, long expiresAtMillis)
        implements Serializable {
    private static final long serialVersionUID = 1L;
}
