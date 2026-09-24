package racing.server.core;

import racing.common.GameConfig;
import racing.common.dto.InviteInfo;
import racing.common.dto.InviteReply;
import racing.common.dto.InviteResult;
import racing.common.dto.InviteStatus;
import racing.common.dto.PlayerStatus;
import racing.common.net.Message;
import racing.common.net.MessageType;
import racing.server.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/**
 * Lời mời thách đấu: mỗi người chỉ có tối đa một lời mời đang chờ (gửi hoặc nhận).
 * Hết {@link GameConfig#INVITE_TIMEOUT_S} giây không trả lời thì {@link #sweep()} (được
 * GameServer gọi mỗi giây) báo TIMEOUT cho người mời. Đồng hồ được tiêm vào để test.
 */
public final class InviteManager {

    private record Invite(long id, Session from, Session to, long expiresAt) {
    }

    private final SessionManager sessions;
    private final RoomManager rooms;
    private final LongSupplier clock;
    private final AtomicLong nextId = new AtomicLong(1);
    private final Map<Long, Invite> pending = new ConcurrentHashMap<>();
    private final Map<String, Long> byUser = new ConcurrentHashMap<>();   // cả người mời lẫn người được mời

    public InviteManager(SessionManager sessions, RoomManager rooms, LongSupplier clock) {
        this.sessions = sessions;
        this.rooms = rooms;
        this.clock = clock;
    }

    /** Xử lý INVITE từ {@code from} tới {@code targetUsername}. Mọi phản hồi được gửi ngay trong hàm. */
    public synchronized void invite(Session from, String targetUsername) {
        if (targetUsername == null || targetUsername.equals(from.username())) {
            from.send(new Message(MessageType.ERROR, "Không thể tự mời chính mình"));
            return;
        }
        if (from.status() == PlayerStatus.IN_MATCH) {
            from.send(new Message(MessageType.ERROR, "Bạn đang trong trận, không thể mời"));
            return;
        }
        if (byUser.containsKey(from.username())) {
            from.send(new Message(MessageType.ERROR, "Bạn đang chờ trả lời một lời mời khác"));
            return;
        }
        Session to = sessions.find(targetUsername).orElse(null);
        long id = nextId.getAndIncrement();
        if (to == null) {
            from.send(new Message(MessageType.INVITE_RESULT, new InviteResult(id, targetUsername, InviteStatus.OFFLINE)));
            return;
        }
        if (to.status() == PlayerStatus.IN_MATCH || byUser.containsKey(to.username())) {
            from.send(new Message(MessageType.INVITE_RESULT, new InviteResult(id, targetUsername, InviteStatus.BUSY)));
            return;
        }
        long expiresAt = clock.getAsLong() + GameConfig.INVITE_TIMEOUT_S * 1000L;
        Invite inv = new Invite(id, from, to, expiresAt);
        pending.put(id, inv);
        byUser.put(from.username(), id);
        byUser.put(to.username(), id);
        Log.info("lời mời #" + id + ": " + from + " -> " + to);
        to.send(new Message(MessageType.INVITE_RECEIVED,
                new InviteInfo(id, from.username(), from.points(), expiresAt)));
    }

    /** Xử lý INVITE_REPLY từ người được mời. */
    public synchronized void reply(Session replier, InviteReply reply) {
        Invite inv = reply == null ? null : pending.get(reply.inviteId());
        if (inv == null || !inv.to().username().equals(replier.username())) {
            replier.send(new Message(MessageType.ERROR, "Lời mời đã hết hạn hoặc không tồn tại"));
            return;
        }
        remove(inv);
        if (!reply.accept()) {
            Log.info("lời mời #" + inv.id() + " bị từ chối");
            inv.from().send(new Message(MessageType.INVITE_RESULT,
                    new InviteResult(inv.id(), inv.to().username(), InviteStatus.REJECTED)));
            return;
        }
        if (!sessions.isOnline(inv.from().username())) {
            replier.send(new Message(MessageType.ERROR, "Người mời đã thoát"));
            return;
        }
        Log.info("lời mời #" + inv.id() + " được chấp nhận, tạo phòng");
        rooms.createRoom(inv.from(), inv.to());
    }

    /** Báo TIMEOUT cho các lời mời quá hạn. Gọi định kỳ. */
    public synchronized void sweep() {
        long now = clock.getAsLong();
        List<Invite> expired = new ArrayList<>();
        for (Invite inv : pending.values()) {
            if (inv.expiresAt() <= now) {
                expired.add(inv);
            }
        }
        for (Invite inv : expired) {
            remove(inv);
            Log.info("lời mời #" + inv.id() + " hết hạn");
            inv.from().send(new Message(MessageType.INVITE_RESULT,
                    new InviteResult(inv.id(), inv.to().username(), InviteStatus.TIMEOUT)));
        }
    }

    /** Người chơi rời (đăng xuất / mất kết nối): huỷ lời mời liên quan. */
    public synchronized void onDisconnect(Session s) {
        Long id = byUser.get(s.username());
        if (id == null) {
            return;
        }
        Invite inv = pending.get(id);
        if (inv == null) {
            byUser.remove(s.username());
            return;
        }
        remove(inv);
        if (inv.to() == s) {
            inv.from().send(new Message(MessageType.INVITE_RESULT,
                    new InviteResult(inv.id(), inv.to().username(), InviteStatus.OFFLINE)));
        }
        // người mời rớt: hộp thoại của người được mời tự hết giờ, không cần gửi gì
    }

    public boolean hasPending(String username) {
        return byUser.containsKey(username);
    }

    private void remove(Invite inv) {
        pending.remove(inv.id());
        byUser.remove(inv.from().username(), inv.id());
        byUser.remove(inv.to().username(), inv.id());
    }
}
