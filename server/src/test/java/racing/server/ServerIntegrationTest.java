package racing.server;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import racing.common.GameConfig;
import racing.common.dto.CarState;
import racing.common.dto.InviteInfo;
import racing.common.dto.InviteReply;
import racing.common.dto.LoginRequest;
import racing.common.dto.LoginResult;
import racing.common.dto.MatchOutcome;
import racing.common.dto.MatchResult;
import racing.common.dto.MatchStart;
import racing.common.dto.RaceState;
import racing.common.dto.RematchReply;
import racing.common.net.Message;
import racing.common.net.MessageType;
import racing.server.db.DbConnection;
import racing.server.db.PlayerDAO;
import racing.server.db.PlayerRecord;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Test đầu cuối qua socket với MySQL thật (chỉ chạy khi có RACING_TEST_DB_URL, như trên CI):
 * alice mời bob, bob nhận, alice chạy 200 km/h về đích, bob đứng yên; kiểm tra kết quả và
 * điểm trong DB; bob từ chối thi đấu tiếp; cả hai nhận ROOM_CLOSED.
 */
class ServerIntegrationTest {

    private static GameServer server;

    /**
     * Client thô: luồng nền đọc liên tục vào hàng đợi như client thật (client ngừng đọc sẽ bị
     * server ngắt khi hàng đợi gửi đầy); await bỏ qua các loại không cần cho tới khi gặp loại mong đợi.
     */
    private static final class RawClient implements AutoCloseable {
        final Socket socket;
        final ObjectOutputStream out;
        final ObjectInputStream in;
        final BlockingQueue<Message> inbox = new LinkedBlockingQueue<>();

        RawClient(int port) throws IOException {
            socket = new Socket("127.0.0.1", port);
            socket.setSoTimeout(60_000);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
            // PING định kỳ như client thật, nếu không server ngắt sau DISCONNECT_TIMEOUT_S khi client chỉ đọc
            Thread heartbeat = new Thread(() -> {
                try {
                    while (!socket.isClosed()) {
                        Thread.sleep(GameConfig.HEARTBEAT_S * 1000L);
                        send(Message.of(MessageType.PING));
                    }
                } catch (Exception ignored) {
                    // socket đã đóng hoặc test kết thúc
                }
            }, "heartbeat");
            heartbeat.setDaemon(true);
            heartbeat.start();
            Thread reader = new Thread(() -> {
                try {
                    while (true) {
                        inbox.add((Message) in.readObject());
                    }
                } catch (Exception ignored) {
                    // socket đã đóng hoặc test kết thúc
                }
            }, "reader");
            reader.setDaemon(true);
            reader.start();
        }

        synchronized void send(Message m) throws IOException {
            out.writeObject(m);
            out.reset();
            out.flush();
        }

        Message await(MessageType type) throws InterruptedException {
            while (true) {
                Message m = inbox.poll(60, TimeUnit.SECONDS);
                if (m == null) {
                    throw new AssertionError("không nhận được " + type + " trong 60 s");
                }
                if (m.getType() == type) {
                    return m;
                }
            }
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }

    @BeforeAll
    static void boot() throws IOException {
        String url = System.getenv("RACING_TEST_DB_URL");
        assumeTrue(url != null && !url.isBlank(), "Không có RACING_TEST_DB_URL, bỏ qua test tích hợp server");
        DbConnection.configure(url, System.getenv().getOrDefault("RACING_TEST_DB_USER", "racing"),
                System.getenv().getOrDefault("RACING_TEST_DB_PASSWORD", "racing"));
        server = GameServer.withMysql(0);
        server.start();
    }

    @AfterAll
    static void shutdown() {
        if (server != null) {
            server.stop();
        }
    }

    private static PlayerRecord player(String name) throws SQLException {
        return new PlayerDAO().findByUsername(name).orElseThrow();
    }

    @Test
    void fullMatchOverSockets() throws Exception {
        PlayerRecord aliceBefore = player("alice");
        PlayerRecord bobBefore = player("bob");

        try (RawClient a = new RawClient(server.port()); RawClient b = new RawClient(server.port())) {
            a.send(new Message(MessageType.LOGIN, new LoginRequest("alice", "123456")));
            LoginResult la = a.await(MessageType.LOGIN_RESULT).getPayload();
            assertTrue(la.ok(), la.message());
            b.send(new Message(MessageType.LOGIN, new LoginRequest("bob", "123456")));
            LoginResult lb = b.await(MessageType.LOGIN_RESULT).getPayload();
            assertTrue(lb.ok(), lb.message());

            a.send(new Message(MessageType.INVITE, "bob"));
            InviteInfo inv = b.await(MessageType.INVITE_RECEIVED).getPayload();
            assertEquals("alice", inv.fromUsername());
            b.send(new Message(MessageType.INVITE_REPLY, new InviteReply(inv.inviteId(), true)));

            MatchStart msA = a.await(MessageType.MATCH_START).getPayload();
            MatchStart msB = b.await(MessageType.MATCH_START).getPayload();
            assertEquals(msA.roomId(), msB.roomId());
            assertEquals(msA.obstacles(), msB.obstacles());
            assertEquals("bob", msA.opponent().username());

            // chờ GO
            while (true) {
                Integer v = a.await(MessageType.COUNTDOWN).getPayload();
                if (v == 0) {
                    break;
                }
            }
            // alice chạy 200 km/h, gửi CAR_STATE mỗi tick; bob chỉ đọc
            Thread driver = new Thread(() -> {
                try {
                    for (int i = 0; i < 2000; i++) {
                        a.send(new Message(MessageType.CAR_STATE, new CarState(0, 1, 200)));
                        Thread.sleep(GameConfig.TICK_MS);
                    }
                } catch (Exception ignored) {
                    // kết thúc khi socket đóng hoặc test xong
                }
            }, "driver");
            driver.setDaemon(true);
            driver.start();

            RaceState first = b.await(MessageType.RACE_UPDATE).getPayload();
            assertEquals("bob", first.me().username());
            assertEquals("alice", first.opponent().username());

            MatchResult ra = a.await(MessageType.MATCH_RESULT).getPayload();
            MatchResult rb = b.await(MessageType.MATCH_RESULT).getPayload();
            driver.interrupt();
            assertEquals(MatchOutcome.WIN, ra.outcome());
            assertEquals(MatchOutcome.LOSE, rb.outcome());
            assertEquals("alice", ra.winnerUsername());
            assertEquals(aliceBefore.points() + GameConfig.POINTS_WIN, ra.myPoints());
            assertEquals(bobBefore.points(), ra.opponentPoints());

            PlayerRecord aliceAfter = player("alice");
            PlayerRecord bobAfter = player("bob");
            assertEquals(aliceBefore.points() + GameConfig.POINTS_WIN, aliceAfter.points());
            assertEquals(aliceBefore.wins() + 1, aliceAfter.wins());
            assertEquals(bobBefore.losses() + 1, bobAfter.losses());

            assertNotNull(a.await(MessageType.REMATCH_ASK));
            assertNotNull(b.await(MessageType.REMATCH_ASK));
            b.send(new Message(MessageType.REMATCH_REPLY, new RematchReply(msB.roomId(), false)));
            assertEquals(msA.roomId(), (Integer) a.await(MessageType.ROOM_CLOSED).getPayload());
            assertEquals(msB.roomId(), (Integer) b.await(MessageType.ROOM_CLOSED).getPayload());

            a.send(Message.of(MessageType.MATCH_HISTORY_REQ));
            java.util.List<racing.common.dto.MatchRow> history = a.await(MessageType.MATCH_HISTORY).getPayload();
            assertEquals(ra.matchId(), history.get(0).matchId());
            assertEquals("bob", history.get(0).opponentUsername());
            assertEquals(MatchOutcome.WIN, history.get(0).outcome());
        }
    }
}
