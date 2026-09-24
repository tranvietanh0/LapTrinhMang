package racing.server.core;

import racing.common.dto.LoginRequest;
import racing.common.dto.LoginResult;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Dựng toàn bộ lõi server với kho dữ liệu trong bộ nhớ, đồng hồ giả và không có scheduler. */
final class TestWorld {

    final AtomicLong now = new AtomicLong(1_000_000);
    final InMemoryPlayerRepository players = new InMemoryPlayerRepository();
    final InMemoryMatchRepository matches = new InMemoryMatchRepository(players);
    final SessionManager sessions = new SessionManager(players);
    final MatchService matchService = new MatchService(matches, players);
    final RoomManager rooms = new RoomManager(sessions, matchService, null, now::get);
    final InviteManager invites = new InviteManager(sessions, rooms, now::get);
    final AccountService accounts = new AccountService(players, matches);

    /** Tạo tài khoản và đăng nhập, trả về phiên. */
    Session login(String username, FakeConnection conn) {
        if (players.login(username, "pw").isEmpty()) {
            players.add(username, "pw");
        }
        LoginResult r = sessions.login(new LoginRequest(username, "pw"), conn);
        assertTrue(r.ok(), r.message());
        return sessions.find(username).orElseThrow();
    }

    void advance(long millis) {
        now.addAndGet(millis);
    }
}
