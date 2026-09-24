package racing.server.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import racing.common.GameConfig;
import racing.common.dto.InviteInfo;
import racing.common.dto.InviteReply;
import racing.common.dto.InviteResult;
import racing.common.dto.InviteStatus;
import racing.common.dto.PlayerStatus;
import racing.common.net.MessageType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InviteManagerTest {

    private final TestWorld w = new TestWorld();
    private final FakeConnection ca = new FakeConnection();
    private final FakeConnection cb = new FakeConnection();
    private final FakeConnection cc = new FakeConnection();
    private Session alice;
    private Session bob;
    private Session carol;

    @BeforeEach
    void login() {
        alice = w.login("alice", ca);
        bob = w.login("bob", cb);
        carol = w.login("carol", cc);
    }

    private InviteInfo inviteAliceToBob() {
        w.invites.invite(alice, "bob");
        InviteInfo info = cb.last(MessageType.INVITE_RECEIVED).getPayload();
        assertNotNull(info);
        return info;
    }

    @Test
    void targetReceivesInviteWithExpiry() {
        InviteInfo info = inviteAliceToBob();
        assertEquals("alice", info.fromUsername());
        assertEquals(w.now.get() + GameConfig.INVITE_TIMEOUT_S * 1000L, info.expiresAtMillis());
        assertTrue(w.invites.hasPending("alice"));
        assertTrue(w.invites.hasPending("bob"));
    }

    @Test
    void offlineTargetGetsOffline() {
        w.invites.invite(alice, "nobody");
        InviteResult r = ca.last(MessageType.INVITE_RESULT).getPayload();
        assertEquals(InviteStatus.OFFLINE, r.status());
        assertEquals("nobody", r.targetUsername());
    }

    @Test
    void busyTargetGetsBusy() {
        inviteAliceToBob();                                  // bob đang có lời mời chờ
        w.invites.invite(carol, "bob");
        InviteResult r = cc.last(MessageType.INVITE_RESULT).getPayload();
        assertEquals(InviteStatus.BUSY, r.status());
        assertEquals(1, cb.count(MessageType.INVITE_RECEIVED), "bob không nhận hộp thoại thứ hai");

        bob.setStatus(PlayerStatus.IN_MATCH);
        w.invites.reply(bob, new InviteReply(1, false));   // dọn lời mời cũ
        w.invites.invite(carol, "bob");
        assertEquals(InviteStatus.BUSY, ((InviteResult) cc.last(MessageType.INVITE_RESULT).getPayload()).status());
    }

    @Test
    void inviterWithPendingInviteGetsError() {
        inviteAliceToBob();
        w.invites.invite(alice, "carol");
        assertEquals(1, ca.count(MessageType.ERROR));
        assertEquals(0, cc.count(MessageType.INVITE_RECEIVED));
    }

    @Test
    void selfInviteIsError() {
        w.invites.invite(alice, "alice");
        assertEquals(1, ca.count(MessageType.ERROR));
    }

    @Test
    void rejectNotifiesInviterAndClearsPending() {
        InviteInfo info = inviteAliceToBob();
        w.invites.reply(bob, new InviteReply(info.inviteId(), false));
        InviteResult r = ca.last(MessageType.INVITE_RESULT).getPayload();
        assertEquals(InviteStatus.REJECTED, r.status());
        assertEquals(info.inviteId(), r.inviteId());
        assertFalse(w.invites.hasPending("alice"));
        assertFalse(w.invites.hasPending("bob"));
        assertEquals(0, w.rooms.roomCount());
    }

    @Test
    void acceptCreatesRoomAndBothBecomeInMatch() {
        InviteInfo info = inviteAliceToBob();
        w.invites.reply(bob, new InviteReply(info.inviteId(), true));
        assertEquals(1, w.rooms.roomCount());
        assertEquals(PlayerStatus.IN_MATCH, alice.status());
        assertEquals(PlayerStatus.IN_MATCH, bob.status());
        assertNotNull(alice.room());
        assertEquals(1, ca.count(MessageType.MATCH_START));
        assertEquals(1, cb.count(MessageType.MATCH_START));
        // carol thấy cả hai chuyển trạng thái trong ONLINE_LIST cuối cùng
        java.util.List<racing.common.dto.PlayerInfo> list = cc.last(MessageType.ONLINE_LIST).getPayload();
        long inMatch = list.stream().filter(p -> p.status() == PlayerStatus.IN_MATCH).count();
        assertEquals(2, inMatch);
        assertFalse(w.invites.hasPending("alice"));
    }

    @Test
    void timeoutNotifiesInviterOnly() {
        InviteInfo info = inviteAliceToBob();
        w.advance(GameConfig.INVITE_TIMEOUT_S * 1000L - 1);
        w.invites.sweep();
        assertNull(ca.last(MessageType.INVITE_RESULT));
        w.advance(1);
        w.invites.sweep();
        InviteResult r = ca.last(MessageType.INVITE_RESULT).getPayload();
        assertEquals(InviteStatus.TIMEOUT, r.status());
        assertEquals(info.inviteId(), r.inviteId());
        assertFalse(w.invites.hasPending("bob"));
        assertEquals(PlayerStatus.FREE, alice.status());
        assertEquals(PlayerStatus.FREE, bob.status());
        // trả lời muộn bị từ chối
        w.invites.reply(bob, new InviteReply(info.inviteId(), true));
        assertEquals(1, cb.count(MessageType.ERROR));
        assertEquals(0, w.rooms.roomCount());
    }

    @Test
    void replyFromWrongUserIsRejected() {
        InviteInfo info = inviteAliceToBob();
        w.invites.reply(carol, new InviteReply(info.inviteId(), true));
        assertEquals(1, cc.count(MessageType.ERROR));
        assertTrue(w.invites.hasPending("bob"));
    }

    @Test
    void targetDisconnectSendsOfflineToInviter() {
        inviteAliceToBob();
        w.invites.onDisconnect(bob);
        w.sessions.logout(bob);
        InviteResult r = ca.last(MessageType.INVITE_RESULT).getPayload();
        assertEquals(InviteStatus.OFFLINE, r.status());
        assertFalse(w.invites.hasPending("alice"));
    }

    @Test
    void inviterDisconnectJustClearsInvite() {
        inviteAliceToBob();
        int before = cb.sent.size();
        w.invites.onDisconnect(alice);
        assertEquals(before, cb.sent.size(), "người được mời không nhận gì, hộp thoại tự hết giờ");
        assertFalse(w.invites.hasPending("bob"));
        w.invites.invite(carol, "bob");
        assertEquals(2, cb.count(MessageType.INVITE_RECEIVED), "bob lại có thể được mời");
    }
}
