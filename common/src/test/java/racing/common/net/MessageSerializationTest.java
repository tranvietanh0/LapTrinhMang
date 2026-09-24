package racing.common.net;

import org.junit.jupiter.api.Test;
import racing.common.GameConfig;
import racing.common.dto.CarSnapshot;
import racing.common.dto.CarState;
import racing.common.dto.EndReason;
import racing.common.dto.InviteInfo;
import racing.common.dto.LoginRequest;
import racing.common.dto.LoginResult;
import racing.common.dto.MatchOutcome;
import racing.common.dto.MatchResult;
import racing.common.dto.MatchRow;
import racing.common.dto.MatchStart;
import racing.common.dto.Obstacle;
import racing.common.dto.PlayerInfo;
import racing.common.dto.PlayerStatus;
import racing.common.dto.RaceState;
import racing.common.dto.RankRow;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Mọi DTO phải đi qua ObjectOutputStream / ObjectInputStream nguyên vẹn, vì đó là cách client và server nói chuyện. */
class MessageSerializationTest {

    private static Message roundTrip(Message m) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(m);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (Message) in.readObject();
        }
    }

    @Test
    void loginRoundTrip() throws Exception {
        Message m = roundTrip(new Message(MessageType.LOGIN, new LoginRequest("alice", "123456")));
        assertEquals(MessageType.LOGIN, m.getType());
        LoginRequest req = m.getPayload();
        assertEquals("alice", req.username());
        assertEquals("123456", req.password());
    }

    @Test
    void loginResultAndPlayerInfoRoundTrip() throws Exception {
        PlayerInfo me = new PlayerInfo(1, "alice", 12, 10, PlayerStatus.FREE);
        Message m = roundTrip(new Message(MessageType.LOGIN_RESULT, LoginResult.success(me)));
        LoginResult r = m.getPayload();
        assertEquals(true, r.ok());
        assertEquals(me, r.me());
        assertEquals(PlayerStatus.IN_MATCH, r.me().withStatus(PlayerStatus.IN_MATCH).status());
    }

    @Test
    void onlineListRoundTrip() throws Exception {
        List<PlayerInfo> list = List.of(
                new PlayerInfo(1, "alice", 12, 10, PlayerStatus.FREE),
                new PlayerInfo(2, "bob", 15, 12, PlayerStatus.IN_MATCH));
        Message m = roundTrip(new Message(MessageType.ONLINE_LIST, list));
        List<PlayerInfo> back = m.getPayload();
        assertEquals(list, back);
    }

    @Test
    void matchStartWithObstaclesRoundTrip() throws Exception {
        MatchStart start = new MatchStart(7, 42,
                new PlayerInfo(1, "alice", 12, 10, PlayerStatus.IN_MATCH),
                new PlayerInfo(2, "bob", 15, 12, PlayerStatus.IN_MATCH),
                GameConfig.TRACK_LENGTH,
                List.of(new Obstacle(0, 120.0), new Obstacle(2, 340.5)));
        MatchStart back = roundTrip(new Message(MessageType.MATCH_START, start)).getPayload();
        assertEquals(start, back);
        assertEquals(2, back.obstacles().size());
    }

    @Test
    void raceStateRoundTrip() throws Exception {
        RaceState s = new RaceState(7, 120L,
                new CarSnapshot("alice", 380.0, 1, 120.0, false, false),
                new CarSnapshot("bob", 310.0, 2, 105.0, true, false), 6000L);
        RaceState back = roundTrip(new Message(MessageType.RACE_UPDATE, s)).getPayload();
        assertEquals(s, back);
    }

    @Test
    void matchResultRoundTrip() throws Exception {
        MatchResult r = new MatchResult(7, 42, MatchOutcome.DRAW, EndReason.DRAW, null, 13, 16, 41000L);
        MatchResult back = roundTrip(new Message(MessageType.MATCH_RESULT, r)).getPayload();
        assertEquals(r, back);
        assertNull(back.winnerUsername());
    }

    @Test
    void primitivePayloadsRoundTrip() throws Exception {
        assertEquals(3, (Integer) roundTrip(new Message(MessageType.COUNTDOWN, 3)).getPayload());
        assertEquals("bob", (String) roundTrip(new Message(MessageType.INVITE, "bob")).getPayload());
        assertNull(roundTrip(Message.of(MessageType.PING)).rawPayload());
        CarState cs = roundTrip(new Message(MessageType.CAR_STATE, new CarState(10.5, 1, 80))).getPayload();
        assertEquals(1, cs.lane());
        InviteInfo inv = roundTrip(new Message(MessageType.INVITE_RECEIVED,
                new InviteInfo(99L, "bob", 15, 123456789L))).getPayload();
        assertEquals(99L, inv.inviteId());
        RankRow row = roundTrip(new Message(MessageType.LEADERBOARD, List.of(new RankRow(1, "bob", 15, 12, 4, 3))))
                .<List<RankRow>>getPayload().get(0);
        assertEquals(1, row.rank());
    }

    @Test
    void registerAndHistoryRoundTrip() throws Exception {
        LoginRequest reg = roundTrip(new Message(MessageType.REGISTER, new LoginRequest("new_user", "pw"))).getPayload();
        assertEquals("new_user", reg.username());
        LoginResult res = roundTrip(new Message(MessageType.REGISTER_RESULT, LoginResult.fail("Tên đã tồn tại"))).getPayload();
        assertEquals(false, res.ok());
        List<MatchRow> history = roundTrip(new Message(MessageType.MATCH_HISTORY, List.of(
                new MatchRow(42, "bob", MatchOutcome.WIN, EndReason.FINISH, 1_700_000_000_000L, 1_700_000_041_000L),
                new MatchRow(43, "carol", MatchOutcome.ABORTED, null, 1_700_000_100_000L, 0L)))).getPayload();
        assertEquals(2, history.size());
        assertEquals("bob", history.get(0).opponentUsername());
        assertNull(history.get(1).reason());
    }

    @Test
    void nullTypeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Message(null, "x"));
    }
}
