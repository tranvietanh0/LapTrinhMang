package racing.server.core;

import org.junit.jupiter.api.Test;
import racing.common.dto.LoginRequest;
import racing.common.dto.LoginResult;
import racing.common.dto.PlayerInfo;
import racing.common.dto.PlayerStatus;
import racing.common.net.MessageType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionManagerTest {

    private final TestWorld w = new TestWorld();

    @Test
    void wrongPasswordIsRejected() {
        w.players.add("alice", "pw");
        LoginResult r = w.sessions.login(new LoginRequest("alice", "xxx"), new FakeConnection());
        assertFalse(r.ok());
        assertNull(r.me());
        assertFalse(w.sessions.isOnline("alice"));
    }

    @Test
    void loginSendsOnlineListToEveryone() {
        FakeConnection ca = new FakeConnection();
        FakeConnection cb = new FakeConnection();
        Session alice = w.login("alice", ca);
        assertEquals(PlayerStatus.FREE, alice.status());
        List<PlayerInfo> first = ca.last(MessageType.ONLINE_LIST).getPayload();
        assertEquals(List.of("alice"), first.stream().map(PlayerInfo::username).toList());

        w.login("bob", cb);
        List<PlayerInfo> a = ca.last(MessageType.ONLINE_LIST).getPayload();
        List<PlayerInfo> b = cb.last(MessageType.ONLINE_LIST).getPayload();
        assertEquals(List.of("alice", "bob"), a.stream().map(PlayerInfo::username).toList());
        assertEquals(a.size(), b.size());
    }

    @Test
    void duplicateLoginIsRejectedAndFirstSessionKept() {
        FakeConnection first = new FakeConnection();
        Session s1 = w.login("alice", first);
        LoginResult r = w.sessions.login(new LoginRequest("alice", "pw"), new FakeConnection());
        assertFalse(r.ok());
        assertTrue(r.message().contains("nơi khác"));
        assertEquals(s1, w.sessions.find("alice").orElseThrow());
        assertEquals(1, w.sessions.sessions().size());
    }

    @Test
    void logoutBroadcastsShrunkList() {
        FakeConnection ca = new FakeConnection();
        FakeConnection cb = new FakeConnection();
        Session alice = w.login("alice", ca);
        w.login("bob", cb);
        w.sessions.logout(alice);
        assertFalse(w.sessions.isOnline("alice"));
        List<PlayerInfo> b = cb.last(MessageType.ONLINE_LIST).getPayload();
        assertEquals(List.of("bob"), b.stream().map(PlayerInfo::username).toList());
        // đăng nhập lại được sau khi đăng xuất
        assertTrue(w.sessions.login(new LoginRequest("alice", "pw"), new FakeConnection()).ok());
    }

    @Test
    void statusChangeIsBroadcast() {
        FakeConnection ca = new FakeConnection();
        FakeConnection cb = new FakeConnection();
        Session alice = w.login("alice", ca);
        w.login("bob", cb);
        w.sessions.setStatus(PlayerStatus.IN_MATCH, alice);
        List<PlayerInfo> b = cb.last(MessageType.ONLINE_LIST).getPayload();
        assertEquals(PlayerStatus.IN_MATCH, b.stream().filter(p -> p.username().equals("alice")).findFirst().orElseThrow().status());
    }
}
