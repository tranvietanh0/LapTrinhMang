package racing.server.core;

import racing.common.GameConfig;
import racing.common.dto.CarState;
import racing.common.dto.PlayerStatus;
import racing.common.dto.RematchReply;
import racing.common.net.Message;
import racing.common.net.MessageType;
import racing.server.Log;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

/**
 * Tạo / đóng phòng và hẹn giờ cho phòng: đếm ngược 3-2-1-0 mỗi giây sau MATCH_START,
 * tick chung {@link GameConfig#TICK_MS} cho mọi phòng (GameServer gọi {@link #tickAll()}),
 * quét hết hạn rematch ({@link #sweep()}). Scheduler có thể null trong test (không hẹn giờ).
 */
public final class RoomManager implements RoomListener {

    private final SessionManager sessions;
    private final MatchService matches;
    private final ScheduledExecutorService scheduler;
    private final LongSupplier clock;
    private final Map<Integer, Room> rooms = new ConcurrentHashMap<>();
    private final AtomicInteger nextRoomId = new AtomicInteger(1);

    public RoomManager(SessionManager sessions, MatchService matches, ScheduledExecutorService scheduler,
                       LongSupplier clock) {
        this.sessions = sessions;
        this.matches = matches;
        this.scheduler = scheduler;
        this.clock = clock;
    }

    /** Mở phòng cho hai người vừa chấp nhận lời mời và bắt đầu ván đầu tiên. */
    public Room createRoom(Session a, Session b) {
        Room room = new Room(nextRoomId.getAndIncrement(), a, b, matches, this, clock);
        rooms.put(room.roomId(), room);
        a.setRoom(room);
        b.setRoom(room);
        sessions.setStatus(PlayerStatus.IN_MATCH, a, b);
        room.startRound();
        return room;
    }

    @Override
    public void roundStarted(Room room) {
        if (scheduler == null) {
            return;
        }
        for (int i = 0; i <= GameConfig.COUNTDOWN_S; i++) {
            int value = GameConfig.COUNTDOWN_S - i;
            scheduler.schedule(() -> safe(() -> room.countdown(value)), 1000L * (i + 1), TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void roomClosed(Room room) {
        rooms.remove(room.roomId());
        Session[] ss = room.sessions();
        for (Session s : ss) {
            if (s.room() == room) {
                s.setRoom(null);
            }
            if (sessions.isOnline(s.username())) {
                s.setStatus(PlayerStatus.FREE);
            }
        }
        sessions.broadcastOnlineList();
    }

    public void tickAll() {
        for (Room r : rooms.values()) {
            safe(r::tick);
        }
    }

    public void sweep() {
        for (Room r : rooms.values()) {
            safe(r::sweep);
        }
    }

    // ------------------------------------------------------------------ thông điệp từ ClientHandler

    public void onCarState(Session s, CarState cs) {
        Room r = s.room();
        if (r != null) {
            r.onCarState(s, cs);
        }
    }

    public void onFinish(Session s) {
        Room r = s.room();
        if (r != null) {
            r.onFinish(s);
        }
    }

    public void onQuit(Session s, Integer roomId) {
        Room r = s.room();
        if (r == null) {
            s.send(new Message(MessageType.ERROR, "Bạn không ở trong phòng nào"));
            return;
        }
        if (roomId != null && roomId != r.roomId()) {
            Log.warn(s + " gửi QUIT_MATCH sai phòng " + roomId + " (đang ở " + r.roomId() + ")");
        }
        r.onQuit(s);
    }

    public void onRematchReply(Session s, RematchReply reply) {
        Room r = s.room();
        if (r != null && reply != null) {
            r.onRematchReply(s, reply.agree());
        }
    }

    /** Người chơi rời (đăng xuất / mất kết nối) khi đang trong phòng. */
    public void onDisconnect(Session s) {
        Room r = s.room();
        if (r != null) {
            r.onDisconnect(s);
        }
    }

    public int roomCount() {
        return rooms.size();
    }

    private static void safe(Runnable r) {
        try {
            r.run();
        } catch (RuntimeException e) {
            Log.warn("lỗi trong tác vụ phòng", e);
        }
    }
}
