package racing.common.net;

/**
 * Loại thông điệp trao đổi giữa client và server. Hướng C→S là client gửi,
 * S→C là server gửi. Kiểu payload là lớp trong gói racing.common.dto (hoặc
 * kiểu cơ bản) được ghi ở từng hằng.
 */
public enum MessageType {

    // ---- Đăng nhập / phiên
    /** C→S  LoginRequest */
    LOGIN,
    /** S→C  LoginResult */
    LOGIN_RESULT,
    /** C→S  null */
    LOGOUT,
    /** S→C  List&lt;PlayerInfo&gt; – gửi lại mỗi khi danh sách online thay đổi */
    ONLINE_LIST,

    // ---- Thách đấu
    /** C→S  String targetUsername */
    INVITE,
    /** S→C  InviteInfo – tới người được mời */
    INVITE_RECEIVED,
    /** C→S  InviteReply */
    INVITE_REPLY,
    /** S→C  InviteResult – tới người mời (REJECTED / TIMEOUT / BUSY / OFFLINE) */
    INVITE_RESULT,

    // ---- Trận đấu
    /** S→C  MatchStart – tới cả hai người chơi */
    MATCH_START,
    /** S→C  Integer 3, 2, 1, 0 (0 = GO) */
    COUNTDOWN,
    /** C→S  CarState – 20 lần / giây */
    CAR_STATE,
    /** S→C  RaceState – vị trí hai xe, tới cả hai người chơi */
    RACE_UPDATE,
    /** C→S  Long clientTimeMillis – client báo đã qua vạch đích (server đối chiếu lại) */
    FINISH,
    /** S→C  MatchResult – tới cả hai người chơi */
    MATCH_RESULT,
    /** S→C  Integer roomId – hỏi có thi đấu tiếp không */
    REMATCH_ASK,
    /** C→S  RematchReply */
    REMATCH_REPLY,
    /** C→S  Integer roomId – chủ động thoát trận */
    QUIT_MATCH,
    /** S→C  Integer roomId – phòng đã đóng, client quay về sảnh */
    ROOM_CLOSED,

    // ---- Tài khoản
    /** C→S  LoginRequest – đăng ký tài khoản mới (username, password) */
    REGISTER,
    /** S→C  LoginResult – ok = true khi tạo được; message báo lỗi khi trùng tên hoặc dữ liệu không hợp lệ */
    REGISTER_RESULT,

    // ---- Bảng xếp hạng và lịch sử
    /** C→S  null */
    LEADERBOARD_REQ,
    /** S→C  List&lt;RankRow&gt; */
    LEADERBOARD,
    /** C→S  null – lịch sử trận của chính người gửi */
    MATCH_HISTORY_REQ,
    /** S→C  List&lt;MatchRow&gt; – mới nhất trước, tối đa GameConfig.HISTORY_LIMIT dòng */
    MATCH_HISTORY,

    // ---- Heartbeat
    /** C→S  null – mỗi HEARTBEAT_S giây */
    PING,
    /** S→C  null */
    PONG,

    // ---- Lỗi chung
    /** S→C  String message – lỗi không thuộc loại nào ở trên */
    ERROR
}
