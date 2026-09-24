package racing.common.dto;

/** Lý do kết thúc trận, trùng với ENUM end_reason trong bảng matches. */
public enum EndReason {
    /** Một xe về đích trước. */
    FINISH,
    /** Hai xe về đích trong cùng một tick. */
    DRAW,
    /** Một người bấm Thoát trận. */
    QUIT,
    /** Một người mất kết nối quá DISCONNECT_TIMEOUT_S giây. */
    DISCONNECT,
    /** Cả hai cùng mất kết nối hoặc server dừng. */
    ABORTED
}
