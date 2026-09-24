package racing.server.core;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import racing.common.dto.EndReason;
import racing.common.dto.InviteInfo;
import racing.common.dto.InviteReply;
import racing.common.dto.LoginRequest;
import racing.common.dto.LoginResult;
import racing.common.dto.MatchOutcome;
import racing.common.dto.MatchResult;
import racing.common.dto.MatchStart;
import racing.common.dto.PlayerInfo;
import racing.common.dto.RankRow;
import racing.common.net.Message;
import racing.common.net.MessageType;
import racing.server.GameServer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Chạy GameServer thật (socket, ClientHandler, scheduler) trên kho dữ liệu trong bộ nhớ,
 * không cần MySQL: kiểm tra đường đi của thông điệp qua mạng, không kiểm tra luật đua
 * (đã có RoomTest).
 */
class SocketSmokeTest {

    private static InMemoryPlayerRepository players;
    private static GameServer server;

    private static final class RawClient implements AutoCloseable {
        final Socket socket;
        final ObjectOutputStream out;
        final ObjectInputStream in;

        RawClient() throws IOException {
            socket = new Socket("127.0.0.1", server.port());
            socket.setSoTimeout(10_000);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
        }

        void send(Message m) throws IOException {
            out.writeObject(m);
            out.reset();
            out.flush();
        }

        Message await(MessageType type) throws IOException, ClassNotFoundException {
            while (true) {
                Message m = (Message) in.readObject();
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
        players = new InMemoryPlayerRepository();
        players.add("alice", "123456");
        players.add("bob", "123456");
        server = new GameServer(0, players, new InMemoryMatchRepository(players));
        server.start();
    }

    @AfterAll
    static void shutdown() {
        server.stop();
    }

    @Test
    void loginInviteQuitAndLeaderboardOverSockets() throws Exception {
        try (RawClient a = new RawClient(); RawClient b = new RawClient()) {
            a.send(Message.of(MessageType.PING));
            a.await(MessageType.PONG);

            a.send(Message.of(MessageType.LEADERBOARD_REQ));
            assertEquals("Bạn chưa đăng nhập", a.await(MessageType.ERROR).getPayload());

            a.send(new Message(MessageType.LOGIN, new LoginRequest("alice", "sai")));
            assertFalse(((LoginResult) a.await(MessageType.LOGIN_RESULT).getPayload()).ok());
            a.send(new Message(MessageType.LOGIN, new LoginRequest("alice", "123456")));
            LoginResult la = a.await(MessageType.LOGIN_RESULT).getPayload();
            assertTrue(la.ok());
            assertEquals("alice", la.me().username());

            b.send(new Message(MessageType.LOGIN, new LoginRequest("bob", "123456")));
            assertTrue(((LoginResult) b.await(MessageType.LOGIN_RESULT).getPayload()).ok());
            List<PlayerInfo> list = a.await(MessageType.ONLINE_LIST).getPayload();
            while (list.size() < 2) {
                list = a.await(MessageType.ONLINE_LIST).getPayload();
            }
            assertEquals(List.of("alice", "bob"), list.stream().map(PlayerInfo::username).toList());

            a.send(new Message(MessageType.REGISTER, new LoginRequest("carol", "abcd")));
            assertTrue(((LoginResult) a.await(MessageType.REGISTER_RESULT).getPayload()).ok());
            a.send(new Message(MessageType.REGISTER, new LoginRequest("carol", "abcd")));
            assertEquals("Tên tài khoản đã tồn tại",
                    ((LoginResult) a.await(MessageType.REGISTER_RESULT).getPayload()).message());

            a.send(new Message(MessageType.INVITE, "bob"));
            InviteInfo inv = b.await(MessageType.INVITE_RECEIVED).getPayload();
            b.send(new Message(MessageType.INVITE_REPLY, new InviteReply(inv.inviteId(), true)));
            MatchStart ms = a.await(MessageType.MATCH_START).getPayload();
            b.await(MessageType.MATCH_START);
            assertEquals(3, (Integer) a.await(MessageType.COUNTDOWN).getPayload());

            a.send(new Message(MessageType.QUIT_MATCH, ms.roomId()));
            MatchResult ra = a.await(MessageType.MATCH_RESULT).getPayload();
            MatchResult rb = b.await(MessageType.MATCH_RESULT).getPayload();
            assertEquals(MatchOutcome.LOSE, ra.outcome());
            assertEquals(MatchOutcome.WIN, rb.outcome());
            assertEquals(EndReason.QUIT, rb.reason());
            a.await(MessageType.ROOM_CLOSED);
            b.await(MessageType.ROOM_CLOSED);

            b.send(Message.of(MessageType.LEADERBOARD_REQ));
            List<RankRow> ranks = b.await(MessageType.LEADERBOARD).getPayload();
            assertEquals("bob", ranks.get(0).username());
            assertEquals(1, ranks.get(0).points());

            b.send(Message.of(MessageType.MATCH_HISTORY_REQ));
            List<racing.common.dto.MatchRow> history = b.await(MessageType.MATCH_HISTORY).getPayload();
            assertEquals(1, history.size());
            assertEquals("alice", history.get(0).opponentUsername());
            assertEquals(MatchOutcome.WIN, history.get(0).outcome());

            // đăng xuất: bob thấy danh sách rút xuống
            a.send(Message.of(MessageType.LOGOUT));
            list = b.await(MessageType.ONLINE_LIST).getPayload();
            while (list.size() > 1) {
                list = b.await(MessageType.ONLINE_LIST).getPayload();
            }
            assertEquals(List.of("bob"), list.stream().map(PlayerInfo::username).toList());
        }
    }
}
