package racing.server.core;

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
import racing.server.Log;

import java.sql.SQLException;
import java.util.List;
import java.util.function.LongSupplier;

/**
 * Phòng đua của hai người chơi: máy trạng thái WAITING → COUNTDOWN → RACING → AWAIT_REMATCH
 * (→ WAITING khi cả hai đồng ý) → CLOSED. Không có timer bên trong: {@link RoomManager} gọi
 * {@link #countdown(int)}, {@link #tick()}, {@link #sweep()} theo lịch; test gọi trực tiếp.
 * Mọi phương thức public đều {@code synchronized} trên phòng. Thứ tự khóa: Room → khóa gửi
 * của ClientHandler; không lớp nào gọi ngược vào Room khi đang giữ khóa khác.
 */
public final class Room {

    public enum State { WAITING, COUNTDOWN, RACING, AWAIT_REMATCH, CLOSED }

    private static final int START_LANE = 1;

    private final int roomId;
    private final RoomPlayer[] players = new RoomPlayer[2];
    private final MatchService matches;
    private final RoomListener listener;
    private final LongSupplier clock;

    private State state = State.CLOSED;
    private int matchId;
    private List<Obstacle> obstacles = List.of();
    private long tick;
    private long raceStartMillis;
    private long rematchDeadline;

    public Room(int roomId, Session a, Session b, MatchService matches, RoomListener listener, LongSupplier clock) {
        this.roomId = roomId;
        this.players[0] = new RoomPlayer(a);
        this.players[1] = new RoomPlayer(b);
        this.matches = matches;
        this.listener = listener;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ vòng đời ván

    /** Tạo bản ghi trận, sinh chướng ngại vật, gửi MATCH_START. Gọi khi mở phòng và khi cả hai rematch. */
    public synchronized void startRound() {
        try {
            matchId = matches.createMatch(roomCode(), players[0].session, players[1].session);
        } catch (SQLException e) {
            Log.warn("không tạo được trận cho phòng " + roomId, e);
            broadcast(new Message(MessageType.ERROR, "Lỗi máy chủ khi tạo trận"));
            close();
            return;
        }
        obstacles = ObstacleGenerator.generate(clock.getAsLong() ^ ((long) roomId << 32) ^ matchId);
        tick = 0;
        for (RoomPlayer p : players) {
            p.car.reset(START_LANE);
            p.rematchAgree = false;
        }
        state = State.WAITING;
        Log.info("phòng " + roomId + " trận " + matchId + " bắt đầu: " + players[0].session + " vs " + players[1].session);
        for (int i = 0; i < 2; i++) {
            players[i].session.send(new Message(MessageType.MATCH_START, new MatchStart(roomId, matchId,
                    players[i].session.info(), players[1 - i].session.info(), GameConfig.TRACK_LENGTH, obstacles)));
            matches.event(matchId, players[i].session, "START", "{\"lane\":" + START_LANE + "}");
        }
        listener.roundStarted(this);
    }

    /** Gửi COUNTDOWN value; 0 = GO, mở khóa đua. */
    public synchronized void countdown(int value) {
        if (state != State.WAITING && state != State.COUNTDOWN) {
            return;
        }
        if (value > 0) {
            state = State.COUNTDOWN;
        } else {
            state = State.RACING;
            raceStartMillis = clock.getAsLong();
        }
        broadcast(new Message(MessageType.COUNTDOWN, value));
    }

    /** Một tick trọng tài: tiến hai xe, xét va chạm, phát RACE_UPDATE, xét về đích. */
    public synchronized void tick() {
        if (state != State.RACING) {
            return;
        }
        long now = clock.getAsLong();
        tick++;
        for (RoomPlayer p : players) {
            Obstacle hit = p.car.advance(now, tick, obstacles);
            if (hit != null) {
                matches.event(matchId, p.session, "COLLISION", "{\"lane\":" + hit.lane() + ",\"distance\":"
                        + Math.round(p.car.distance()) + ",\"obstacle\":" + Math.round(hit.position()) + "}");
            }
        }
        long elapsed = now - raceStartMillis;
        for (int i = 0; i < 2; i++) {
            players[i].session.send(new Message(MessageType.RACE_UPDATE, new RaceState(roomId, tick,
                    players[i].car.snapshot(now), players[1 - i].car.snapshot(now), elapsed)));
        }
        boolean f0 = players[0].car.finished();
        boolean f1 = players[1].car.finished();
        if (f0 && f1 && players[0].car.finishTick() == players[1].car.finishTick()) {
            logFinish(players[0], elapsed);
            logFinish(players[1], elapsed);
            endRound(EndReason.DRAW, null, elapsed, true);
        } else if (f0 || f1) {
            RoomPlayer winner = f0 ? players[0] : players[1];
            logFinish(winner, elapsed);
            endRound(EndReason.FINISH, winner, elapsed, true);
        }
    }

    /** Hết hạn chờ trả lời thi đấu tiếp. Gọi định kỳ. */
    public synchronized void sweep() {
        if (state == State.AWAIT_REMATCH && clock.getAsLong() >= rematchDeadline) {
            Log.info("phòng " + roomId + ": hết hạn chờ thi đấu tiếp");
            close();
        }
    }

    // ------------------------------------------------------------------ thông điệp từ client

    public synchronized void onCarState(Session s, CarState cs) {
        RoomPlayer p = player(s);
        if (p != null && state == State.RACING) {
            p.car.applyClient(cs, clock.getAsLong());
        }
    }

    /** FINISH chỉ là gợi ý của client; server tự xét trong tick nên không cần làm gì. */
    public synchronized void onFinish(Session s) {
        // giữ để log nếu cần
    }

    public synchronized void onQuit(Session s) {
        leave(s, EndReason.QUIT);
    }

    public synchronized void onDisconnect(Session s) {
        leave(s, EndReason.DISCONNECT);
    }

    public synchronized void onRematchReply(Session s, boolean agree) {
        RoomPlayer p = player(s);
        if (p == null || state != State.AWAIT_REMATCH) {
            return;
        }
        if (!agree) {
            Log.info("phòng " + roomId + ": " + s + " từ chối thi đấu tiếp");
            close();
            return;
        }
        p.rematchAgree = true;
        if (players[0].rematchAgree && players[1].rematchAgree) {
            for (RoomPlayer rp : players) {
                matches.event(matchId, rp.session, "REMATCH", null);
            }
            startRound();
        }
    }

    // ------------------------------------------------------------------ nội bộ

    private void leave(Session s, EndReason reason) {
        RoomPlayer p = player(s);
        if (p == null || state == State.CLOSED) {
            return;
        }
        if (state == State.AWAIT_REMATCH) {
            close();
            return;
        }
        RoomPlayer other = players[0] == p ? players[1] : players[0];
        matches.event(matchId, s, reason.name(), "{\"distance\":" + Math.round(p.car.distance()) + "}");
        long elapsed = state == State.RACING ? clock.getAsLong() - raceStartMillis : 0;
        endRound(reason, other, elapsed, false);
        close();
    }

    private void endRound(EndReason reason, RoomPlayer winner, long elapsed, boolean askRematch) {
        state = State.AWAIT_REMATCH;
        Session a = players[0].session;
        Session b = players[1].session;
        try {
            matches.saveResult(matchId, a, b, winner == null ? null : winner.session, reason);
        } catch (SQLException e) {
            Log.warn("không lưu được kết quả trận " + matchId, e);
        }
        String winnerName = winner == null ? null : winner.session.username();
        for (int i = 0; i < 2; i++) {
            RoomPlayer me = players[i];
            RoomPlayer opp = players[1 - i];
            MatchOutcome outcome = winner == null ? MatchOutcome.DRAW : (winner == me ? MatchOutcome.WIN : MatchOutcome.LOSE);
            me.session.send(new Message(MessageType.MATCH_RESULT, new MatchResult(roomId, matchId, outcome, reason,
                    winnerName, me.session.points(), opp.session.points(), elapsed)));
        }
        Log.info("phòng " + roomId + " trận " + matchId + " kết thúc: " + reason
                + (winnerName == null ? "" : ", thắng " + winnerName));
        if (askRematch) {
            rematchDeadline = clock.getAsLong() + GameConfig.INVITE_TIMEOUT_S * 1000L;
            broadcast(new Message(MessageType.REMATCH_ASK, roomId));
        }
    }

    private void close() {
        if (state == State.CLOSED) {
            return;
        }
        state = State.CLOSED;
        broadcast(new Message(MessageType.ROOM_CLOSED, roomId));
        Log.info("phòng " + roomId + " đóng");
        listener.roomClosed(this);
    }

    private void logFinish(RoomPlayer p, long elapsed) {
        matches.event(matchId, p.session, "FINISH",
                "{\"distance\":" + Math.round(p.car.distance()) + ",\"elapsedMillis\":" + elapsed + "}");
    }

    private void broadcast(Message m) {
        for (RoomPlayer p : players) {
            p.session.send(m);
        }
    }

    private RoomPlayer player(Session s) {
        for (RoomPlayer p : players) {
            if (p.session == s) {
                return p;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ truy vấn

    public int roomId() {
        return roomId;
    }

    public String roomCode() {
        return "R" + roomId;
    }

    public synchronized State state() {
        return state;
    }

    public synchronized int matchId() {
        return matchId;
    }

    public synchronized List<Obstacle> obstacles() {
        return obstacles;
    }

    public Session[] sessions() {
        return new Session[] {players[0].session, players[1].session};
    }

    public boolean contains(Session s) {
        return player(s) != null;
    }

    private static final class RoomPlayer {
        final Session session;
        final CarSim car;
        boolean rematchAgree;

        RoomPlayer(Session session) {
            this.session = session;
            this.car = new CarSim(session.username(), START_LANE);
            session.setStatus(PlayerStatus.IN_MATCH);
        }
    }
}
