package racing.client.model;

import org.junit.jupiter.api.Test;
import racing.common.dto.PlayerInfo;
import racing.common.dto.PlayerStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientStateTest {

    private static final PlayerInfo ALICE = new PlayerInfo(1, "alice", 3, 2, PlayerStatus.FREE);
    private static final PlayerInfo BOB = new PlayerInfo(2, "bob", 1, 1, PlayerStatus.FREE);
    private static final PlayerInfo CAROL = new PlayerInfo(3, "carol", 5, 4, PlayerStatus.IN_MATCH);

    @Test
    void onlineListRefreshesSelf() {
        ClientState s = new ClientState();
        s.setMe(ALICE);
        s.setOnline(List.of(new PlayerInfo(1, "alice", 4, 3, PlayerStatus.FREE), BOB));
        assertEquals(4, s.me().points());
        assertTrue(s.isSelf(s.find("alice").orElseThrow()));
        assertFalse(s.isSelf(BOB));
    }

    @Test
    void canInviteOnlyFreeOthers() {
        ClientState s = new ClientState();
        s.setMe(ALICE);
        s.setOnline(List.of(ALICE, BOB, CAROL));
        assertTrue(s.canInvite(BOB));
        assertFalse(s.canInvite(CAROL), "đang thi đấu");
        assertFalse(s.canInvite(ALICE), "không tự mời mình");
        assertFalse(s.canInvite(null));
    }

    @Test
    void pendingInviteBlocksAnotherInvite() {
        ClientState s = new ClientState();
        s.setMe(ALICE);
        s.setOnline(List.of(ALICE, BOB));
        s.markInviteSent("bob");
        assertTrue(s.hasPendingInvite());
        assertFalse(s.canInvite(BOB));
        s.clearPendingInvite();
        assertTrue(s.canInvite(BOB));
    }

    @Test
    void nullListBecomesEmpty() {
        ClientState s = new ClientState();
        s.setOnline(null);
        assertTrue(s.online().isEmpty());
    }
}
