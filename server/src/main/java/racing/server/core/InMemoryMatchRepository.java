package racing.server.core;

import racing.common.GameConfig;
import racing.common.dto.EndReason;
import racing.common.dto.MatchOutcome;
import racing.common.dto.MatchRow;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Bảng matches / match_events trong bộ nhớ; cộng điểm vào {@link InMemoryPlayerRepository} như DAO thật. */
public final class InMemoryMatchRepository implements MatchRepository {

    record Match(int id, String roomCode, int p1, int p2, Integer winner, EndReason reason) {
    }

    record Event(int matchId, int playerId, String type, String json) {
    }

    final Map<Integer, Match> matches = new LinkedHashMap<>();
    final List<Event> events = new ArrayList<>();
    private final InMemoryPlayerRepository players;
    private int nextId = 1;

    public InMemoryMatchRepository(InMemoryPlayerRepository players) {
        this.players = players;
    }

    @Override
    public synchronized int createMatch(String roomCode, int player1Id, int player2Id) {
        int id = nextId++;
        matches.put(id, new Match(id, roomCode, player1Id, player2Id, null, null));
        return id;
    }

    @Override
    public synchronized void saveResult(int matchId, int player1Id, int player2Id, Integer winnerId, EndReason reason) {
        Match m = matches.get(matchId);
        matches.put(matchId, new Match(matchId, m.roomCode(), player1Id, player2Id, winnerId, reason));
        if (reason == EndReason.ABORTED) {
            return;
        }
        for (int pid : new int[] {player1Id, player2Id}) {
            if (reason == EndReason.DRAW) {
                players.apply(pid, GameConfig.POINTS_DRAW, 0, 0, 1);
            } else if (pid == winnerId) {
                players.apply(pid, GameConfig.POINTS_WIN, 1, 0, 0);
            } else {
                players.apply(pid, 0, 0, 1, 0);
            }
        }
    }

    @Override
    public synchronized void addEvent(int matchId, int playerId, String eventType, String payloadJson) {
        events.add(new Event(matchId, playerId, eventType, payloadJson));
    }

    @Override
    public synchronized List<MatchRow> findRecentByPlayer(int playerId, int limit) {
        List<MatchRow> rows = new ArrayList<>();
        List<Match> all = new ArrayList<>(matches.values());
        for (int i = all.size() - 1; i >= 0 && rows.size() < limit; i--) {
            Match m = all.get(i);
            if (m.reason() == null || (m.p1() != playerId && m.p2() != playerId)) {
                continue;
            }
            int oppId = m.p1() == playerId ? m.p2() : m.p1();
            MatchOutcome outcome = m.winner() == null
                    ? (m.reason() == EndReason.DRAW ? MatchOutcome.DRAW : MatchOutcome.ABORTED)
                    : (m.winner() == playerId ? MatchOutcome.WIN : MatchOutcome.LOSE);
            rows.add(new MatchRow(m.id(), players.byId.get(oppId).username(), outcome, m.reason(), 0, 0));
        }
        return rows;
    }

    synchronized List<Event> eventsOf(String type) {
        List<Event> out = new ArrayList<>();
        for (Event e : events) {
            if (e.type().equals(type)) {
                out.add(e);
            }
        }
        return out;
    }
}
