package racing.server;

import racing.common.GameConfig;
import racing.server.core.AccountService;
import racing.server.core.InMemoryMatchRepository;
import racing.server.core.InMemoryPlayerRepository;
import racing.server.core.InviteManager;
import racing.server.core.MatchRepository;
import racing.server.core.MatchService;
import racing.server.core.PlayerRepository;
import racing.server.core.RoomManager;
import racing.server.core.ServerServices;
import racing.server.core.SessionManager;
import racing.server.db.JdbcMatchRepository;
import racing.server.db.JdbcPlayerRepository;
import racing.server.net.ClientHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Điểm vào server: mở ServerSocket, mỗi client một luồng trong pool, một scheduler chung
 * cho tick phòng (50 ms), đếm ngược và quét hết hạn lời mời / rematch (1 s).
 * <pre>java -jar racing-server.jar [port] [--memory]</pre>
 * {@code --memory}: không cần MySQL, dữ liệu trong bộ nhớ với 6 tài khoản mẫu (mật khẩu 123456),
 * mất hết khi tắt server. Chỉ để chạy thử / demo nhanh.
 */
public final class GameServer {

    private final int requestedPort;
    private final ServerServices services;
    private final RoomManager rooms;
    private final InviteManager invites;
    private final ScheduledExecutorService scheduler;
    private final ExecutorService pool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "client");
        t.setDaemon(true);
        return t;
    });
    private ServerSocket serverSocket;
    private Thread acceptor;
    private volatile boolean running;

    public GameServer(int port, PlayerRepository players, MatchRepository matches) {
        this.requestedPort = port;
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "timer");
            t.setDaemon(true);
            return t;
        });
        SessionManager sessions = new SessionManager(players);
        MatchService matchService = new MatchService(matches, players);
        this.rooms = new RoomManager(sessions, matchService, scheduler, System::currentTimeMillis);
        this.invites = new InviteManager(sessions, rooms, System::currentTimeMillis);
        AccountService accounts = new AccountService(players, matches);
        this.services = new ServerServices(sessions, invites, rooms, accounts);
    }

    /** Server dùng MySQL thật (cấu hình qua DbConnection). */
    public static GameServer withMysql(int port) {
        return new GameServer(port, new JdbcPlayerRepository(), new JdbcMatchRepository());
    }

    /** Server không cần MySQL: kho dữ liệu trong bộ nhớ, sẵn 6 tài khoản như db/seed.sql. */
    public static GameServer withMemory(int port) {
        InMemoryPlayerRepository players = new InMemoryPlayerRepository();
        for (String name : List.of("alice", "bob", "carol", "dave", "erin", "frank")) {
            players.add(name, "123456");
        }
        return new GameServer(port, players, new InMemoryMatchRepository(players));
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(requestedPort);
        running = true;
        scheduler.scheduleAtFixedRate(rooms::tickAll, GameConfig.TICK_MS, GameConfig.TICK_MS, TimeUnit.MILLISECONDS);
        scheduler.scheduleAtFixedRate(() -> {
            invites.sweep();
            rooms.sweep();
        }, 1, 1, TimeUnit.SECONDS);
        acceptor = new Thread(this::acceptLoop, "acceptor");
        acceptor.setDaemon(true);
        acceptor.start();
        Log.info("server lắng nghe cổng " + port());
    }

    public int port() {
        return serverSocket == null ? requestedPort : serverSocket.getLocalPort();
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket s = serverSocket.accept();
                pool.execute(new ClientHandler(s, services));
            } catch (IOException e) {
                if (running) {
                    Log.warn("lỗi accept", e);
                }
            }
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
            // đang tắt
        }
        scheduler.shutdownNow();
        pool.shutdownNow();
        Log.info("server đã dừng");
    }

    public ServerServices services() {
        return services;
    }

    public static void main(String[] args) throws Exception {
        boolean memory = false;
        int port = GameConfig.PORT;
        for (String a : args) {
            if (a.equals("--memory")) {
                memory = true;
            } else {
                port = Integer.parseInt(a);
            }
        }
        GameServer server;
        if (memory) {
            Log.warn("CHẾ ĐỘ --memory: không dùng MySQL, mọi điểm số và lịch sử sẽ mất khi tắt server");
            server = withMemory(port);
        } else {
            server = withMysql(port);
        }
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        Thread.currentThread().join();
    }
}
