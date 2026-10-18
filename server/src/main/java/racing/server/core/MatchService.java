package racing.server.core;

import racing.common.dto.EndReason;
import racing.server.Log;
import racing.server.db.PlayerRecord;

import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * Ghi trận vào DB và cập nhật điểm vào phiên: tạo bản ghi khi ván bắt đầu, chốt kết quả
 * (transaction trong DAO) khi kết thúc, ghi diễn biến (match_events). Lỗi ghi diễn biến chỉ
 * log, không làm hỏng trận; lỗi tạo / chốt trận được ném lên để Room xử lý.
 */
public final class MatchService {

    private final MatchRepository matches;
    private final PlayerRepository players;
    private final Executor eventWriter;

    public MatchService(MatchRepository matches, PlayerRepository players) {
        this(matches,players,Runnable::run);
    }

    public MatchService(MatchRepository matches, PlayerRepository players, Executor eventWriter) {
        this.matches = matches;
        this.players = players;
        this.eventWriter=eventWriter;
    }

    public int createMatch(String roomCode, Session a, Session b) throws SQLException {
        return matches.createMatch(roomCode, a.playerId(), b.playerId());
    }

    /**
     * Chốt kết quả và nạp lại điểm mới của hai người vào Session.
     *
     * @param winner null khi hòa (DRAW) hoặc hủy (ABORTED)
     */
    public void saveResult(int matchId, Session a, Session b, Session winner, EndReason reason) throws SQLException {
        matches.saveResult(matchId, a.playerId(), b.playerId(), winner == null ? null : winner.playerId(), reason);
        refresh(a);
        refresh(b);
    }

    public void event(int matchId, Session s, String type, String json) {
        int playerId=s.playerId();
        try {
            eventWriter.execute(()->{
                try { matches.addEvent(matchId,playerId,type,json); }
                catch(SQLException e) { Log.warn("không ghi được match_events "+type+" cho trận "+matchId,e); }
            });
        } catch (RejectedExecutionException e) {
            Log.warn("hàng đợi ghi match_events đã dừng, sự kiện "+type+" trận "+matchId,e);
        }
    }

    private void refresh(Session s) throws SQLException {
        Optional<PlayerRecord> r = players.findById(s.playerId());
        if (r.isPresent()) {
            s.updateStats(r.get().points(), r.get().wins());
        }
    }
}
