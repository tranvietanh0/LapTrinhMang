package racing.server.core;

import racing.common.dto.PlayerInfo;
import racing.common.dto.PlayerStatus;
import racing.common.net.Message;
import racing.server.db.PlayerRecord;

/** Một người chơi đã đăng nhập: thông tin tài khoản, trạng thái và kênh gửi. */
public final class Session {

    private final int playerId;
    private final String username;
    private final PlayerConnection connection;
    private volatile int points;
    private volatile int wins;
    private volatile PlayerStatus status = PlayerStatus.FREE;
    private volatile Room room;

    public Session(PlayerRecord record, PlayerConnection connection) {
        this.playerId = record.playerId();
        this.username = record.username();
        this.points = record.points();
        this.wins = record.wins();
        this.connection = connection;
    }

    public int playerId() {
        return playerId;
    }

    public String username() {
        return username;
    }

    public int points() {
        return points;
    }

    public PlayerStatus status() {
        return status;
    }

    public void setStatus(PlayerStatus status) {
        this.status = status;
    }

    public Room room() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    /** Cập nhật điểm sau khi DB đã chốt kết quả. */
    public void updateStats(int points, int wins) {
        this.points = points;
        this.wins = wins;
    }

    /** Tạo PlayerInfo MỚI mỗi lần gọi để ObjectOutputStream không gửi lại bản cũ. */
    public PlayerInfo info() {
        return new PlayerInfo(playerId, username, points, wins, status);
    }

    public void send(Message m) {
        connection.send(m);
    }

    @Override
    public String toString() {
        return username + "#" + playerId;
    }
}
