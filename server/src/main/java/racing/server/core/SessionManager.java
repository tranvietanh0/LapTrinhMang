package racing.server.core;

import racing.common.dto.LoginRequest;
import racing.common.dto.LoginResult;
import racing.common.dto.PlayerInfo;
import racing.common.dto.PlayerStatus;
import racing.common.net.Message;
import racing.common.net.MessageType;
import racing.server.Log;
import racing.server.db.PlayerRecord;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý người chơi đang online: đăng nhập (từ chối đăng nhập trùng), đăng xuất,
 * trạng thái Rảnh / Đang thi đấu và phát ONLINE_LIST mỗi khi danh sách đổi.
 * Chỉ dùng ConcurrentHashMap, không giữ khóa nào khi gửi ra socket.
 */
public final class SessionManager {

    private final PlayerRepository players;
    private final Map<String, Session> online = new ConcurrentHashMap<>();

    public SessionManager(PlayerRepository players) {
        this.players = players;
    }

    /** Đăng nhập. Trả về kết quả gửi cho client; khi ok, phiên đã được thêm vào danh sách online. */
    public synchronized LoginResult login(LoginRequest req, PlayerConnection conn) {
        if (req == null || req.username() == null || req.password() == null) {
            return new LoginResult(false, "Thiếu tên tài khoản hoặc mật khẩu", null);
        }
        Optional<PlayerRecord> found;
        try {
            found = players.login(req.username(), req.password());
        } catch (SQLException e) {
            Log.warn("lỗi DB khi đăng nhập " + req.username(), e);
            return new LoginResult(false, "Lỗi máy chủ khi kiểm tra tài khoản", null);
        }
        if (found.isEmpty()) {
            return new LoginResult(false, "Sai tên tài khoản hoặc mật khẩu", null);
        }
        PlayerRecord record = found.get();
        if (online.containsKey(record.username())) {
            return new LoginResult(false, "Tài khoản đang đăng nhập ở nơi khác", null);
        }
        Session s = new Session(record, conn);
        online.put(s.username(), s);
        Log.info("đăng nhập: " + s + " (online " + online.size() + ")");
        broadcastOnlineList();
        return new LoginResult(true, "Đăng nhập thành công", s.info());
    }

    public void logout(Session s) {
        if (s != null && online.remove(s.username(), s)) {
            Log.info("đăng xuất: " + s + " (online " + online.size() + ")");
            broadcastOnlineList();
        }
    }

    public Optional<Session> find(String username) {
        return username == null ? Optional.empty() : Optional.ofNullable(online.get(username));
    }

    public boolean isOnline(String username) {
        return username != null && online.containsKey(username);
    }

    public Collection<Session> sessions() {
        return online.values();
    }

    /** Đổi trạng thái nhiều người rồi phát danh sách một lần. */
    public void setStatus(PlayerStatus status, Session... sessions) {
        for (Session s : sessions) {
            s.setStatus(status);
        }
        broadcastOnlineList();
    }

    /** Danh sách mới tạo mỗi lần gọi (ObjectOutputStream cache đối tượng). */
    public List<PlayerInfo> onlineList() {
        List<PlayerInfo> list = new ArrayList<>(online.size());
        for (Session s : online.values()) {
            list.add(s.info());
        }
        list.sort((a, b) -> a.username().compareToIgnoreCase(b.username()));
        return list;
    }

    public void broadcastOnlineList() {
        for (Session s : online.values()) {
            s.send(new Message(MessageType.ONLINE_LIST, onlineList()));
        }
    }
}
