package racing.client.net;

import racing.common.GameConfig;
import racing.common.net.Message;
import racing.common.net.MessageType;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Lớp mạng phía client: một socket TCP, luồng đọc {@code readObject}, gửi có khóa,
 * listener theo {@link MessageType}. Mọi listener đều được gọi trên luồng Swing (EDT)
 * nên các màn hình cập nhật giao diện trực tiếp, không cần {@code invokeLater} nữa.
 *
 * <pre>
 *   NetworkClient net = new NetworkClient();
 *   net.connect("localhost", GameConfig.PORT);
 *   Runnable off = net.on(MessageType.ONLINE_LIST, m -> table.setRows(m.getPayload()));
 *   net.send(new Message(MessageType.LOGIN, new LoginRequest("alice", "123456")));
 *   off.run();   // huỷ đăng ký
 * </pre>
 */
public final class NetworkClient implements AutoCloseable {

    private static final int CONNECT_TIMEOUT_MS = 5000;

    private final Map<MessageType, List<Consumer<Message>>> listeners = new ConcurrentHashMap<>();
    private final List<Consumer<Message>> anyListeners = new CopyOnWriteArrayList<>();
    private final List<Runnable> disconnectListeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Object sendLock = new Object();

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private ScheduledExecutorService heartbeat;

    /** Kết nối tới server. Ném IOException nếu không nối được trong 5 giây. */
    public void connect(String host, int port) throws IOException {
        if (socket != null) {
            throw new IllegalStateException("đã kết nối");
        }
        Socket s = new Socket();
        s.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
        socket = s;
        out = new ObjectOutputStream(s.getOutputStream());
        out.flush();
        in = new ObjectInputStream(s.getInputStream());

        Thread reader = new Thread(this::readLoop, "net-reader");
        reader.setDaemon(true);
        reader.start();

        heartbeat = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "net-heartbeat");
            t.setDaemon(true);
            return t;
        });
        heartbeat.scheduleAtFixedRate(() -> send(Message.of(MessageType.PING)),
                GameConfig.HEARTBEAT_S, GameConfig.HEARTBEAT_S, TimeUnit.SECONDS);
    }

    public boolean isConnected() {
        return socket != null && !closed.get();
    }

    /** Gửi một thông điệp. An toàn khi gọi từ mọi luồng. Lỗi ghi được coi là mất kết nối. */
    public void send(Message m) {
        if (!isConnected()) {
            return;
        }
        synchronized (sendLock) {
            try {
                out.writeObject(m);
                out.reset();          // tránh ObjectOutputStream cache đối tượng cũ
                out.flush();
            } catch (IOException e) {
                handleDisconnect();
            }
        }
    }

    /** Đăng ký listener cho một loại thông điệp. Trả về hàm huỷ đăng ký. */
    public Runnable on(MessageType type, Consumer<Message> listener) {
        List<Consumer<Message>> list = listeners.computeIfAbsent(type, t -> new CopyOnWriteArrayList<>());
        list.add(listener);
        return () -> list.remove(listener);
    }

    /** Đăng ký listener nhận mọi thông điệp (dùng để log). */
    public Runnable onAny(Consumer<Message> listener) {
        anyListeners.add(listener);
        return () -> anyListeners.remove(listener);
    }

    /** Gọi (trên EDT) khi socket bị đóng hoặc lỗi đọc, không gọi khi tự {@link #close()}. */
    public Runnable onDisconnect(Runnable listener) {
        disconnectListeners.add(listener);
        return () -> disconnectListeners.remove(listener);
    }

    /** Đóng chủ động (đăng xuất). Không phát sự kiện mất kết nối. */
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            shutdownQuietly();
        }
    }

    // ------------------------------------------------------------------ nội bộ

    private void readLoop() {
        try {
            while (!closed.get()) {
                Object o = in.readObject();
                if (o instanceof Message m) {
                    dispatch(m);
                }
            }
        } catch (IOException | ClassNotFoundException | RuntimeException e) {
            handleDisconnect();
        }
    }

    private void dispatch(Message m) {
        SwingUtilities.invokeLater(() -> {
            List<Consumer<Message>> list = listeners.get(m.getType());
            if (list != null) {
                for (Consumer<Message> l : list) {
                    l.accept(m);
                }
            }
            for (Consumer<Message> l : anyListeners) {
                l.accept(m);
            }
        });
    }

    private void handleDisconnect() {
        if (closed.compareAndSet(false, true)) {
            shutdownQuietly();
            SwingUtilities.invokeLater(() -> disconnectListeners.forEach(Runnable::run));
        }
    }

    private void shutdownQuietly() {
        if (heartbeat != null) {
            heartbeat.shutdownNow();
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
            // đang đóng, không còn gì để làm với lỗi này
        }
    }
}
