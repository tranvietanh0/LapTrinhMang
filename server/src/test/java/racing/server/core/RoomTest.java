package racing.server.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import racing.common.GameConfig;
import racing.common.dto.CarState;
import racing.common.dto.EndReason;
import racing.common.dto.MatchOutcome;
import racing.common.dto.MatchResult;
import racing.common.dto.MatchStart;
import racing.common.dto.Obstacle;
import racing.common.dto.PlayerStatus;
import racing.common.dto.RaceState;
import racing.common.net.Message;
import racing.common.net.MessageType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomTest {

    private final TestWorld w = new TestWorld();
    private final FakeConnection ca = new FakeConnection();
    private final FakeConnection cb = new FakeConnection();
    private Session alice;
    private Session bob;
    private Room room;

    @BeforeEach
    void openRoom() {
        alice = w.login("alice", ca);
        bob = w.login("bob", cb);
        room = w.rooms.createRoom(alice, bob);
    }

    private void go() {
        for (int v = GameConfig.COUNTDOWN_S; v >= 0; v--) {
            w.advance(1000);
            room.countdown(v);
        }
    }

    private final java.util.Map<Session, CarState> driving = new java.util.LinkedHashMap<>();

    /** Như client thật: ghi nhớ trạng thái và gửi lại mỗi tick (sau va chạm client vẫn nhấn ga). */
    private void drive(Session s, CarState state) {
        driving.put(s, state);
        room.onCarState(s, state);
    }

    /** Chạy tick cho tới khi cả hai nhận MATCH_RESULT hoặc hết maxTicks. */
    private void raceUntilResult(int maxTicks) {
        for (int i = 0; i < maxTicks && ca.count(MessageType.MATCH_RESULT) == 0; i++) {
            w.advance(GameConfig.TICK_MS);
            driving.forEach(room::onCarState);
            room.tick();
        }
    }

    @Test
    void matchStartIsPerRecipientWithSameObstacles() {
        MatchStart a = ca.last(MessageType.MATCH_START).getPayload();
        MatchStart b = cb.last(MessageType.MATCH_START).getPayload();
        assertEquals("alice", a.me().username());
        assertEquals("bob", a.opponent().username());
        assertEquals("bob", b.me().username());
        assertEquals(a.obstacles(), b.obstacles());
        assertEquals(GameConfig.OBSTACLE_COUNT, a.obstacles().size());
        assertEquals(PlayerStatus.IN_MATCH, a.me().status());
        assertEquals(1, w.matches.matches.size());
        assertEquals(2, w.matches.eventsOf("START").size());
    }

    @Test
    void countdownSequenceThenRacing() {
        assertEquals(Room.State.WAITING, room.state());
        go();
        List<Message> cds = ca.of(MessageType.COUNTDOWN);
        assertEquals(List.of(3, 2, 1, 0), cds.stream().map(m -> (Integer) m.getPayload()).toList());
        assertEquals(Room.State.RACING, room.state());
    }

    @Test
    void carStateBeforeGoIsIgnored() {
        room.onCarState(alice, new CarState(0, 1, 200));
        go();
        w.advance(GameConfig.TICK_MS);
        room.tick();
        RaceState s = ca.last(MessageType.RACE_UPDATE).getPayload();
        assertEquals(0, s.me().distance());
    }

    @Test
    void raceUpdateIsPerRecipientAndClampsSpeed() {
        go();
        room.onCarState(alice, new CarState(0, 2, 999));
        w.advance(GameConfig.TICK_MS);
        room.tick();
        RaceState sa = ca.last(MessageType.RACE_UPDATE).getPayload();
        RaceState sb = cb.last(MessageType.RACE_UPDATE).getPayload();
        assertEquals("alice", sa.me().username());
        assertEquals("bob", sa.opponent().username());
        assertEquals("bob", sb.me().username());
        assertEquals(GameConfig.MAX_SPEED, sa.me().speed());
        assertEquals(2, sa.me().lane());
        assertEquals(room.roomId(), sa.roomId());
    }

    @Test
    void firstToFinishWinsAndPointsAreUpdated() {
        go();
        drive(alice, new CarState(0, 1, 200));
        raceUntilResult(1000);
        MatchResult ra = ca.last(MessageType.MATCH_RESULT).getPayload();
        MatchResult rb = cb.last(MessageType.MATCH_RESULT).getPayload();
        assertEquals(MatchOutcome.WIN, ra.outcome());
        assertEquals(MatchOutcome.LOSE, rb.outcome());
        assertEquals(EndReason.FINISH, ra.reason());
        assertEquals("alice", ra.winnerUsername());
        assertEquals(1, ra.myPoints());
        assertEquals(0, ra.opponentPoints());
        assertEquals(0, rb.myPoints());
        assertEquals(1, rb.opponentPoints());
        assertEquals(1, alice.points());
        assertEquals(EndReason.FINISH, w.matches.matches.get(room.matchId()).reason());
        assertEquals(1, w.matches.eventsOf("FINISH").size());
        assertEquals(Room.State.AWAIT_REMATCH, room.state());
        assertEquals(1, ca.count(MessageType.REMATCH_ASK));
        assertEquals(1, cb.count(MessageType.REMATCH_ASK));
    }

    @Test
    void sameTickFinishIsDraw() {
        go();
        drive(alice, new CarState(0, 1, 200));
        drive(bob, new CarState(0, 1, 200));
        raceUntilResult(1000);
        MatchResult ra = ca.last(MessageType.MATCH_RESULT).getPayload();
        MatchResult rb = cb.last(MessageType.MATCH_RESULT).getPayload();
        assertEquals(MatchOutcome.DRAW, ra.outcome());
        assertEquals(MatchOutcome.DRAW, rb.outcome());
        assertEquals(EndReason.DRAW, ra.reason());
        assertNull(ra.winnerUsername());
        assertEquals(1, ra.myPoints());
        assertEquals(1, ra.opponentPoints());
        assertNull(w.matches.matches.get(room.matchId()).winner());
    }

    @Test
    void oneTickApartIsNotDraw() {
        go();
        drive(alice, new CarState(0, 1, 200));
        w.advance(GameConfig.TICK_MS);
        room.tick();                                   // alice đi trước một tick
        drive(bob, new CarState(0, 1, 200));
        raceUntilResult(1000);
        MatchResult ra = ca.last(MessageType.MATCH_RESULT).getPayload();
        assertEquals(MatchOutcome.WIN, ra.outcome());
    }

    @Test
    void collisionIsReportedAsStunnedAndLogged() {
        go();
        Obstacle first = room.obstacles().get(0);
        room.onCarState(alice, new CarState(0, first.lane(), 200));
        boolean stunnedSeen = false;
        for (int i = 0; i < 200 && !stunnedSeen; i++) {
            w.advance(GameConfig.TICK_MS);
            room.tick();
            RaceState s = ca.last(MessageType.RACE_UPDATE).getPayload();
            stunnedSeen = s.me().stunned();
            RaceState sb = cb.last(MessageType.RACE_UPDATE).getPayload();
            assertEquals(s.me().stunned(), sb.opponent().stunned(), "hai bên thấy cùng trạng thái va chạm");
        }
        assertTrue(stunnedSeen);
        assertEquals(1, w.matches.eventsOf("COLLISION").size());
    }

    @Test
    void quitDuringRaceLosesAndClosesRoomWithoutRematch() {
        go();
        room.onQuit(alice);
        MatchResult ra = ca.last(MessageType.MATCH_RESULT).getPayload();
        MatchResult rb = cb.last(MessageType.MATCH_RESULT).getPayload();
        assertEquals(MatchOutcome.LOSE, ra.outcome());
        assertEquals(MatchOutcome.WIN, rb.outcome());
        assertEquals(EndReason.QUIT, rb.reason());
        assertEquals(1, bob.points());
        assertEquals(0, ca.count(MessageType.REMATCH_ASK));
        assertEquals(1, ca.count(MessageType.ROOM_CLOSED));
        assertEquals(1, cb.count(MessageType.ROOM_CLOSED));
        assertEquals(Room.State.CLOSED, room.state());
        assertEquals(PlayerStatus.FREE, alice.status());
        assertEquals(PlayerStatus.FREE, bob.status());
        assertNull(alice.room());
        assertEquals(0, w.rooms.roomCount());
        assertEquals(1, w.matches.eventsOf("QUIT").size());
    }

    @Test
    void disconnectDuringCountdownIsRecordedAsDisconnect() {
        w.advance(1000);
        room.countdown(3);
        w.rooms.onDisconnect(bob);
        MatchResult ra = ca.last(MessageType.MATCH_RESULT).getPayload();
        assertEquals(MatchOutcome.WIN, ra.outcome());
        assertEquals(EndReason.DISCONNECT, ra.reason());
        assertEquals(EndReason.DISCONNECT, w.matches.matches.get(room.matchId()).reason());
        assertEquals(1, ca.count(MessageType.ROOM_CLOSED));
    }

    @Test
    void rematchWhenBothAgreeStartsNewMatchWithNewObstacles() {
        go();
        drive(alice, new CarState(0, 1, 200));
        raceUntilResult(1000);
        int firstMatch = room.matchId();
        List<Obstacle> firstObstacles = room.obstacles();
        ca.clear();
        cb.clear();
        w.advance(1234);
        room.onRematchReply(alice, true);
        assertEquals(Room.State.AWAIT_REMATCH, room.state(), "chờ người thứ hai");
        room.onRematchReply(bob, true);
        assertEquals(Room.State.WAITING, room.state());
        assertNotEquals(firstMatch, room.matchId());
        assertNotEquals(firstObstacles, room.obstacles());
        MatchStart ms = ca.last(MessageType.MATCH_START).getPayload();
        assertEquals(room.matchId(), ms.matchId());
        assertEquals(2, w.matches.eventsOf("REMATCH").size());
        assertEquals(2, w.matches.matches.size());
        go();
        w.advance(GameConfig.TICK_MS);
        room.tick();
        RaceState s = ca.last(MessageType.RACE_UPDATE).getPayload();
        assertEquals(0, s.me().distance(), "quãng đường về 0 ở ván mới");
    }

    @Test
    void rematchDeclinedClosesRoom() {
        go();
        drive(alice, new CarState(0, 1, 200));
        raceUntilResult(1000);
        room.onRematchReply(alice, true);
        room.onRematchReply(bob, false);
        assertEquals(Room.State.CLOSED, room.state());
        assertEquals(1, ca.count(MessageType.ROOM_CLOSED));
        assertEquals(PlayerStatus.FREE, alice.status());
        assertNotNull(w.sessions.find("alice").orElse(null));
    }

    @Test
    void rematchTimeoutClosesRoom() {
        go();
        drive(alice, new CarState(0, 1, 200));
        raceUntilResult(1000);
        w.advance(GameConfig.INVITE_TIMEOUT_S * 1000L - 1);
        w.rooms.sweep();
        assertEquals(Room.State.AWAIT_REMATCH, room.state());
        w.advance(2);
        w.rooms.sweep();
        assertEquals(Room.State.CLOSED, room.state());
    }

    @Test
    void twoRoomsDoNotMixUpdates() {
        FakeConnection cc = new FakeConnection();
        FakeConnection cd = new FakeConnection();
        Session carol = w.login("carol", cc);
        Session dave = w.login("dave", cd);
        Room room2 = w.rooms.createRoom(carol, dave);
        go();
        for (int v = GameConfig.COUNTDOWN_S; v >= 0; v--) {
            room2.countdown(v);
        }
        room.onCarState(alice, new CarState(0, 1, 200));
        room2.onCarState(carol, new CarState(0, 1, 100));
        w.advance(GameConfig.TICK_MS);
        w.rooms.tickAll();
        RaceState sa = ca.last(MessageType.RACE_UPDATE).getPayload();
        RaceState sc = cc.last(MessageType.RACE_UPDATE).getPayload();
        assertEquals(room.roomId(), sa.roomId());
        assertEquals(room2.roomId(), sc.roomId());
        assertEquals("carol", sc.me().username());
        assertEquals(100, sc.me().speed());
        assertEquals(200, sa.me().speed());
        assertEquals(0, cc.count(MessageType.MATCH_START) - 1);
    }
}
