package racing.common.dto;

/** Kết quả một lời mời thách đấu, gửi về cho người mời. */
public enum InviteStatus {
    /** Người được mời bấm Từ chối. */
    REJECTED,
    /** Quá INVITE_TIMEOUT_S giây không phản hồi. */
    TIMEOUT,
    /** Người được mời đang thi đấu hoặc đang có lời mời khác. */
    BUSY,
    /** Người được mời đã thoát khỏi hệ thống. */
    OFFLINE
}
