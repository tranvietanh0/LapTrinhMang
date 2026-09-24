package racing.server.net;

import racing.common.GameConfig;
import racing.common.dto.LoginResult;
import racing.common.net.Message;
import racing.common.net.MessageType;
import racing.server.Log;
import racing.server.core.PlayerConnection;
import racing.server.core.ServerServices;
import racing.server.core.Session;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.sql.SQLException;

/**
 * Một luồng cho một client: vòng {@code readObject}, phân loại theo {@link MessageType} và
 * giao cho các manager. {@link #send} đồng bộ trên handler và gọi {@code reset()} sau mỗi
 * lần ghi để ObjectOutputStream không gửi lại bản cũ của DTO. Không nhận gì trong
 * {@link GameConfig#DISCONNECT_TIMEOUT_S} giây (client PING mỗi 5 s) thì coi là mất kết nối.
 */
public final class ClientHandler implements Runnable, PlayerConnection {

    private final Socket socket;
    private final ServerServices services;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private final String remote;
    private volatile Session session;
    private volatile boolean closed;

    public ClientHandler(Socket socket, ServerServices services) throws IOException {
        this.socket = socket;
        this.services = services;
        this.remote = socket.getRemoteSocketAddress().toString();
        socket.setSoTimeout(GameConfig.DISCONNECT_TIMEOUT_S * 1000);
        socket.setTcpNoDelay(true);
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.out.flush();
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    @Override
    public void run() {
        Log.info("kết nối mới " + remote);
        try {
            while (!closed) {
                Object o = in.readObject();
                if (o instanceof Message m) {
                    dispatch(m);
                } else {
                    send(new Message(MessageType.ERROR, "Thông điệp không hợp lệ"));
                }
            }
        } catch (SocketTimeoutException e) {
            Log.info(who() + " không phản hồi " + GameConfig.DISCONNECT_TIMEOUT_S + " s, coi là mất kết nối");
        } catch (EOFException e) {
            Log.info(who() + " đóng kết nối");
        } catch (IOException | ClassNotFoundException e) {
            if (!closed) {
                Log.info(who() + " mất kết nối: " + e);
            }
        } catch (RuntimeException e) {
            Log.warn("lỗi xử lý thông điệp của " + who(), e);
        } finally {
            cleanup();
        }
    }

    private void dispatch(Message m) {
        MessageType t = m.getType();
        switch (t) {
            case PING -> send(Message.of(MessageType.PONG));
            case LOGIN -> login(m);
            case REGISTER -> send(new Message(MessageType.REGISTER_RESULT, services.accounts().register(m.getPayload())));
            case LOGOUT -> {
                leaveEverything();
                session = null;
            }
            default -> {
                if (session == null) {
                    send(new Message(MessageType.ERROR, "Bạn chưa đăng nhập"));
                } else {
                    dispatchLoggedIn(session, m);
                }
            }
        }
    }

    private void dispatchLoggedIn(Session s, Message m) {
        switch (m.getType()) {
            case INVITE -> services.invites().invite(s, m.getPayload());
            case INVITE_REPLY -> services.invites().reply(s, m.getPayload());
            case CAR_STATE -> services.rooms().onCarState(s, m.getPayload());
            case FINISH -> services.rooms().onFinish(s);
            case QUIT_MATCH -> services.rooms().onQuit(s, m.getPayload());
            case REMATCH_REPLY -> services.rooms().onRematchReply(s, m.getPayload());
            case LEADERBOARD_REQ -> query(MessageType.LEADERBOARD, "bảng xếp hạng", () -> services.accounts().leaderboard());
            case MATCH_HISTORY_REQ -> query(MessageType.MATCH_HISTORY, "lịch sử trận", () -> services.accounts().history(s));
            default -> send(new Message(MessageType.ERROR, "Server không xử lý thông điệp " + m.getType()));
        }
    }

    private void login(Message m) {
        if (session != null) {
            send(new Message(MessageType.LOGIN_RESULT, new LoginResult(false, "Bạn đã đăng nhập rồi", session.info())));
            return;
        }
        LoginResult r = services.sessions().login(m.getPayload(), this);
        if (r.ok()) {
            session = services.sessions().find(r.me().username()).orElse(null);
        }
        send(new Message(MessageType.LOGIN_RESULT, r));
    }

    private interface DbQuery {
        Object run() throws SQLException;
    }

    private void query(MessageType reply, String what, DbQuery q) {
        try {
            send(new Message(reply, q.run()));
        } catch (SQLException e) {
            Log.warn("lỗi DB khi lấy " + what + " cho " + who(), e);
            send(new Message(MessageType.ERROR, "Lỗi máy chủ khi lấy " + what));
        }
    }

    /** Dọn phòng, lời mời, phiên (thứ tự này để đối thủ nhận kết quả trước khi danh sách online đổi). */
    private void leaveEverything() {
        Session s = session;
        if (s == null) {
            return;
        }
        services.rooms().onDisconnect(s);
        services.invites().onDisconnect(s);
        services.sessions().logout(s);
    }

    private void cleanup() {
        if (closed) {
            return;
        }
        closed = true;
        leaveEverything();
        session = null;
        try {
            socket.close();
        } catch (IOException ignored) {
            // socket đã đóng
        }
        Log.info("đóng " + remote);
    }

    @Override
    public void send(Message message) {
        if (closed) {
            return;
        }
        synchronized (out) {
            try {
                out.writeObject(message);
                out.reset();
                out.flush();
            } catch (IOException e) {
                Log.info("không gửi được tới " + who() + ": " + e.getMessage());
                try {
                    socket.close();          // làm vòng readObject thoát và cleanup
                } catch (IOException ignored) {
                    // đã đóng
                }
            }
        }
    }

    private String who() {
        Session s = session;
        return s == null ? remote : s.username() + "@" + remote;
    }
}
