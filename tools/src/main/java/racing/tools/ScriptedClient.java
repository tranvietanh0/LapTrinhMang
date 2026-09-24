package racing.tools;

import racing.common.GameConfig;
import racing.common.dto.CarState;
import racing.common.dto.InviteInfo;
import racing.common.dto.InviteReply;
import racing.common.dto.LoginRequest;
import racing.common.dto.MatchStart;
import racing.common.dto.RematchReply;
import racing.common.net.Message;
import racing.common.net.MessageType;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Client dòng lệnh nói đúng giao thức {@link Message} qua TCP, không có giao diện.
 * Dùng để:
 * <ul>
 *   <li>Phương test server trước khi client Swing xong (đăng nhập, mời, đua, xem RACE_UPDATE).</li>
 *   <li>Kịch bản kiểm thử T10 (hai client cùng về đích một tick) và T17 (gửi tốc độ 999).</li>
 *   <li>Chạy nhiều client song song để test tải (T15).</li>
 * </ul>
 * Mọi thông điệp nhận được đều in ra stdout kèm thời gian.
 *
 * <pre>
 * java -jar racing-tools.jar [--host localhost] [--port 5000] [--script file.txt] [--auto-accept]
 *      [--auto-rematch yes|no] [lệnh...]
 * </pre>
 * Không có --script và không có lệnh trên dòng lệnh thì đọc lệnh từ stdin (chế độ tương tác).
 * Cú pháp lệnh: xem {@link ScriptCommand}.
 */
public final class ScriptedClient implements AutoCloseable {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private final Thread reader;
    private final String label;
    private final boolean autoAccept;
    private final Boolean autoRematch;

    private volatile boolean running = true;
    private final AtomicReference<InviteInfo> lastInvite = new AtomicReference<>();
    private final AtomicReference<MatchStart> currentMatch = new AtomicReference<>();
    private volatile boolean matchOver = false;
    private volatile long myDistanceOnServer = 0;

    private final Object waitLock = new Object();
    private final List<MessageType> received = new ArrayList<>();
    /** Vị trí đã duyệt tới trong {@link #received}: wait/expect xét tiếp từ đây nên không bỏ lỡ thông điệp đến trong lúc drive/sleep. */
    private int cursor = 0;

    public ScriptedClient(String host, int port, String label, boolean autoAccept, Boolean autoRematch)
            throws IOException {
        this.label = label;
        this.autoAccept = autoAccept;
        this.autoRematch = autoRematch;
        this.socket = new Socket(host, port);
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.out.flush();
        this.in = new ObjectInputStream(socket.getInputStream());
        this.reader = new Thread(this::readLoop, "reader-" + label);
        this.reader.setDaemon(true);
        this.reader.start();
        Thread heartbeat = new Thread(this::heartbeatLoop, "ping-" + label);
        heartbeat.setDaemon(true);
        heartbeat.start();
        log("kết nối " + host + ":" + port);
    }

    // ------------------------------------------------------------------ nhận

    /** PING mỗi HEARTBEAT_S giây như client thật, để server không coi là mất kết nối khi script đang chờ. */
    private void heartbeatLoop() {
        try {
            while (running) {
                Thread.sleep(GameConfig.HEARTBEAT_S * 1000L);
                if (running) {
                    send(Message.of(MessageType.PING));
                }
            }
        } catch (InterruptedException | IOException ignored) {
            // đang tắt hoặc mất kết nối; readLoop sẽ báo
        }
    }

    private void readLoop() {
        try {
            while (running) {
                Message m = (Message) in.readObject();
                onMessage(m);
            }
        } catch (EOFException e) {
            log("server đóng kết nối");
        } catch (IOException | ClassNotFoundException e) {
            if (running) {
                log("lỗi đọc: " + e);
            }
        }
        running = false;
        synchronized (waitLock) {
            waitLock.notifyAll();
        }
    }

    private void onMessage(Message m) throws IOException {
        Object p = m.rawPayload();
        log("<< " + m.getType() + (p == null ? "" : " " + p));
        switch (m.getType()) {
            case INVITE_RECEIVED -> {
                lastInvite.set(m.getPayload());
                if (autoAccept) {
                    send(new Message(MessageType.INVITE_REPLY, new InviteReply(lastInvite.get().inviteId(), true)));
                }
            }
            case MATCH_START -> {
                currentMatch.set(m.getPayload());
                matchOver = false;
                myDistanceOnServer = 0;
            }
            case RACE_UPDATE -> {
                racing.common.dto.RaceState s = m.getPayload();
                myDistanceOnServer = (long) s.me().distance();
            }
            case MATCH_RESULT -> matchOver = true;
            case REMATCH_ASK -> {
                if (autoRematch != null) {
                    send(new Message(MessageType.REMATCH_REPLY,
                            new RematchReply(currentMatchId(), autoRematch)));
                }
            }
            case ROOM_CLOSED -> currentMatch.set(null);
            default -> { }
        }
        synchronized (waitLock) {
            received.add(m.getType());
            waitLock.notifyAll();
        }
    }

    /**
     * Chờ đến khi nhận loại thông điệp đó, xét từ sau thông điệp mà lần wait/expect trước đã khớp
     * (nên thông điệp đến trong lúc drive/sleep vẫn được tính). Trả về false nếu quá hạn hoặc mất kết nối.
     */
    public boolean waitFor(MessageType type, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        synchronized (waitLock) {
            while (running) {
                for (int i = cursor; i < received.size(); i++) {
                    if (received.get(i) == type) {
                        cursor = i + 1;
                        return true;
                    }
                }
                long left = deadline - System.currentTimeMillis();
                if (left <= 0) {
                    return false;
                }
                waitLock.wait(left);
            }
            return false;
        }
    }

    // ------------------------------------------------------------------ gửi

    public synchronized void send(Message m) throws IOException {
        out.writeObject(m);
        out.reset();   // tránh ObjectOutputStream cache đối tượng cũ
        out.flush();
        log(">> " + m.getType() + (m.rawPayload() == null ? "" : " " + m.rawPayload()));
    }

    private int currentMatchId() {
        MatchStart ms = currentMatch.get();
        return ms == null ? -1 : ms.roomId();
    }

    // ------------------------------------------------------------------ thực thi lệnh

    /** Thực thi một lệnh. Trả về false khi gặp exit hoặc mất kết nối. */
    public boolean execute(ScriptCommand c) throws IOException, InterruptedException {
        if (!running) {
            log("không còn kết nối, bỏ qua: " + c.name());
            return false;
        }
        switch (c.name()) {
            case "login" -> send(new Message(MessageType.LOGIN, new LoginRequest(c.arg(0), c.arg(1))));
            case "logout" -> send(Message.of(MessageType.LOGOUT));
            case "invite" -> send(new Message(MessageType.INVITE, c.arg(0)));
            case "accept", "reject" -> {
                InviteInfo inv = lastInvite.get();
                if (inv == null) {
                    log("chưa nhận lời mời nào");
                } else {
                    send(new Message(MessageType.INVITE_REPLY, new InviteReply(inv.inviteId(), c.name().equals("accept"))));
                }
            }
            case "car" -> send(new Message(MessageType.CAR_STATE,
                    new CarState(c.doubleArg(0), (int) c.doubleArg(1), c.doubleArg(2))));
            case "drive" -> drive(c.doubleArg(0), c.intArg(1, 1), c.intArg(2, Integer.MAX_VALUE));
            case "finish" -> send(new Message(MessageType.FINISH, System.currentTimeMillis()));
            case "quit" -> send(new Message(MessageType.QUIT_MATCH, currentMatchId()));
            case "rematch" -> send(new Message(MessageType.REMATCH_REPLY,
                    new RematchReply(currentMatchId(), c.arg(0).equalsIgnoreCase("yes"))));
            case "leaderboard" -> send(Message.of(MessageType.LEADERBOARD_REQ));
            case "ping" -> send(Message.of(MessageType.PING));
            case "sleep" -> Thread.sleep(c.intArg(0, 0));
            case "wait", "expect" -> {
                MessageType t = MessageType.valueOf(c.arg(0).toUpperCase());
                boolean ok = waitFor(t, c.intArg(1, 10_000));
                log((ok ? "đã nhận " : "QUÁ HẠN chờ ") + t);
                if (!ok && c.name().equals("expect")) {
                    System.exit(2);
                }
            }
            case "exit" -> {
                return false;
            }
            default -> throw new IllegalStateException(c.name());
        }
        return running;
    }

    /**
     * Gửi CAR_STATE mỗi TICK_MS với tốc độ cố định, quãng đường tính như client thật.
     * Tốc độ vượt MAX_SPEED được gửi nguyên (để test server cắt ngưỡng, T17).
     */
    private void drive(double speedKmh, int lane, int maxTicks) throws IOException, InterruptedException {
        double distance = 0;
        for (int tick = 0; tick < maxTicks && running && !matchOver; tick++) {
            distance += speedKmh * GameConfig.KMH_TO_MS * (GameConfig.TICK_MS / 1000.0);
            send(new Message(MessageType.CAR_STATE, new CarState(distance, lane, speedKmh)));
            if (distance >= GameConfig.TRACK_LENGTH) {
                send(new Message(MessageType.FINISH, System.currentTimeMillis()));
                return;
            }
            Thread.sleep(GameConfig.TICK_MS);
        }
    }

    private void log(String s) {
        System.out.println(LocalTime.now().format(TIME) + " [" + label + "] " + s);
    }

    @Override
    public void close() throws IOException {
        running = false;
        socket.close();
    }

    // ------------------------------------------------------------------ main

    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int port = GameConfig.PORT;
        Path script = null;
        boolean autoAccept = false;
        Boolean autoRematch = null;
        String label = "client";
        List<String> inline = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--host" -> host = args[++i];
                case "--port" -> port = Integer.parseInt(args[++i]);
                case "--script" -> script = Path.of(args[++i]);
                case "--label" -> label = args[++i];
                case "--auto-accept" -> autoAccept = true;
                case "--auto-rematch" -> autoRematch = args[++i].equalsIgnoreCase("yes");
                case "--help", "-h" -> {
                    System.out.println("java -jar racing-tools.jar [--host H] [--port P] [--label L] [--script FILE]"
                            + " [--auto-accept] [--auto-rematch yes|no] [\"lệnh; lệnh; ...\"]");
                    System.out.println("Lệnh: login u p | logout | invite u | accept | reject | car d lane v | drive v [lane] [ticks]"
                            + " | finish | quit | rematch yes|no | leaderboard | ping | sleep ms | wait TYPE [ms] | expect TYPE [ms] | exit");
                    return;
                }
                default -> inline.add(args[i]);
            }
        }

        List<String> lines = new ArrayList<>();
        if (script != null) {
            lines.addAll(Files.readAllLines(script, StandardCharsets.UTF_8));
        } else if (!inline.isEmpty()) {
            for (String chunk : String.join(" ", inline).split(";")) {
                lines.add(chunk);
            }
        }

        ScriptedClient connected;
        try {
            connected = new ScriptedClient(host, port, label, autoAccept, autoRematch);
        } catch (IOException e) {
            System.err.println("Không kết nối được tới " + host + ":" + port + " (" + e.getMessage()
                    + "). Server đã chạy chưa?");
            System.exit(1);
            return;
        }
        try (ScriptedClient client = connected) {
            if (!lines.isEmpty()) {
                for (String line : lines) {
                    ScriptCommand c = ScriptCommand.parse(line);
                    if (c != null && !client.execute(c)) {
                        break;
                    }
                }
            } else {
                BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
                System.out.println("Chế độ tương tác. Gõ lệnh, 'exit' để thoát.");
                String line;
                while ((line = stdin.readLine()) != null) {
                    try {
                        ScriptCommand c = ScriptCommand.parse(line);
                        if (c != null && !client.execute(c)) {
                            break;
                        }
                    } catch (IllegalArgumentException e) {
                        System.out.println("  ! " + e.getMessage());
                    }
                }
            }
            // giữ kết nối thêm một chút để nhận nốt phản hồi cuối
            CountDownLatch l = new CountDownLatch(1);
            l.await(300, TimeUnit.MILLISECONDS);
        }
    }
}
