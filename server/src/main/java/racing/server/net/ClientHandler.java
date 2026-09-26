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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Một luồng cho một client: vòng {@code readObject}, phân loại theo {@link MessageType} và
 * giao cho các manager. {@link #send} chỉ đưa thông điệp vào hàng đợi, một luồng ghi riêng
 * cho mỗi client ghi ra socket và gọi {@code reset()} sau mỗi lần ghi để ObjectOutputStream
 * không gửi lại bản cũ của DTO. Nhờ vậy luồng tick của phòng không bao giờ bị chặn bởi một
 * client ngừng đọc (treo, rút mạng); hàng đợi đầy thì ngắt client đó. Không nhận gì trong
 * {@link GameConfig#DISCONNECT_TIMEOUT_S} giây (client PING mỗi 5 s) thì coi là mất kết nối.
 */
public final class ClientHandler implements Runnable, PlayerConnection {

    /** Khoảng 12 s RACE_UPDATE (20 tin/giây); client chậm hơn mức này coi như mất kết nối. */
    static final int OUTBOX_CAPACITY = 256;

    private final Socket socket;
    private final BlockingQueue<Message> outbox = new LinkedBlockingQueue<>(OUTBOX_CAPACITY);
    private final Thread writer;
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
        this.writer = new Thread(this::writeLoop, "writer-" + remote);
        this.writer.setDaemon(true);
    }

    @Override
    public void run() {
        Log.info("kết nối mới " + remote);
        writer.start();
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
        if (r.ok()) {
            // ONLINE_LIST phát trong login() tới trước LOGIN_RESULT, lúc client chưa mở sảnh nên bỏ qua;
            // gửi lại sau LOGIN_RESULT để sảnh của người vừa vào có danh sách ngay
            send(new Message(MessageType.ONLINE_LIST, services.sessions().onlineList()));
        }
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
        closeSocket();
        writer.interrupt();
        Log.info("đóng " + remote);
    }

    /** Không chặn: gọi được từ luồng tick, luồng hẹn giờ lời mời hay luồng của client khác. */
    @Override
    public void send(Message message) {
        if (closed || socket.isClosed()) {
            return;
        }
        if (!outbox.offer(message)) {
            Log.info(who() + " không nhận kịp, hàng đợi gửi đầy " + OUTBOX_CAPACITY + " tin, ngắt kết nối");
            closeSocket();
        }
    }

    private void writeLoop() {
        try {
            while (!socket.isClosed()) {
                Message m = outbox.take();
                out.writeObject(m);
                out.reset();
                out.flush();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            if (!closed) {
                Log.info("không gửi được tới " + who() + ": " + e.getMessage());
            }
            closeSocket();
        }
    }

    /** Đóng socket làm vòng readObject thoát và gọi cleanup. */
    private void closeSocket() {
        try {
            socket.close();
        } catch (IOException ignored) {
            // đã đóng
        }
    }

    private String who() {
        Session s = session;
        return s == null ? remote : s.username() + "@" + remote;
    }
}
