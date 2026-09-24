package racing.client.ui.race;

import racing.common.GameConfig;
import racing.common.dto.CarSnapshot;
import racing.common.dto.CarState;
import racing.common.dto.EndReason;
import racing.common.dto.MatchOutcome;
import racing.common.dto.MatchResult;
import racing.common.dto.MatchStart;
import racing.common.dto.Obstacle;
import racing.common.dto.PlayerInfo;
import racing.common.dto.PlayerStatus;
import racing.common.dto.RaceState;
import racing.common.dto.RematchReply;
import racing.common.net.Message;
import racing.common.net.MessageType;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Chạy thử màn hình đua KHÔNG cần server: một "server giả" trong tiến trình đếm ngược, nhận
 * CAR_STATE, cắt ngưỡng tốc độ, xét va chạm, điều khiển một xe đối thủ tự động, xét về đích và
 * hỏi thi đấu tiếp. Luật ở đây chỉ để xem giao diện; luật thật nằm ở server của Phương.
 *
 * <pre>./mvnw -pl client -am exec:java -Dexec.mainClass=racing.client.ui.race.RaceDemo</pre>
 */
public final class RaceDemo {

    private static final Random RNG = new Random();

    private RaceFrame frame;
    private int roomId = 1;
    private int matchId = 100;
    private List<Obstacle> obstacles;
    private double myDist, mySpeed;
    private int myLane = 1;
    private long myStunUntil;
    private boolean myFinished, oppFinished, over;
    private double oppDist;
    private int oppLane = 1;
    private long oppStunUntil;
    private long tickNo;
    private long startAt;
    private Timer serverTick;
    private int myPoints = 12, oppPoints = 15;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RaceDemo().start());
    }

    private void start() {
        MatchStart ms = newMatch();
        frame = new RaceFrame(ms, this::onClientMessage, () -> System.exit(0));
        frame.setVisible(true);
        countdownThenRace();
    }

    private MatchStart newMatch() {
        obstacles = randomObstacles();
        myDist = 0; mySpeed = 0; myLane = 1; myStunUntil = 0; myFinished = false;
        oppDist = 0; oppLane = 1; oppStunUntil = 0; oppFinished = false;
        over = false; tickNo = 0;
        return new MatchStart(roomId, matchId++,
                new PlayerInfo(1, "alice", myPoints, 10, PlayerStatus.IN_MATCH),
                new PlayerInfo(2, "bob", oppPoints, 12, PlayerStatus.IN_MATCH),
                GameConfig.TRACK_LENGTH, obstacles);
    }

    static List<Obstacle> randomObstacles() {
        List<Obstacle> list = new ArrayList<>();
        double step = (GameConfig.TRACK_LENGTH - 200) / GameConfig.OBSTACLE_COUNT;
        for (int i = 0; i < GameConfig.OBSTACLE_COUNT; i++) {
            list.add(new Obstacle(RNG.nextInt(GameConfig.LANES), 120 + i * step + RNG.nextInt(30)));
        }
        return list;
    }

    private void countdownThenRace() {
        int[] n = {GameConfig.COUNTDOWN_S};
        Timer t = new Timer(1000, null);
        t.addActionListener(e -> {
            frame.handle(new Message(MessageType.COUNTDOWN, n[0]));
            if (n[0] == 0) {
                t.stop();
                startAt = System.currentTimeMillis();
                serverTick = new Timer(GameConfig.TICK_MS, ev -> serverTick());
                serverTick.start();
            }
            n[0]--;
        });
        t.setInitialDelay(500);
        t.start();
    }

    // ---- client -> "server"

    private void onClientMessage(Message m) {
        switch (m.getType()) {
            case CAR_STATE -> {
                CarState s = m.getPayload();
                long now = System.currentTimeMillis();
                if (now < myStunUntil || myFinished) {
                    return;
                }
                mySpeed = Math.max(0, Math.min(GameConfig.MAX_SPEED, s.speed()));
                myLane = Math.max(0, Math.min(GameConfig.LANES - 1, s.lane()));
                double maxStep = GameConfig.MAX_SPEED * GameConfig.KMH_TO_MS * GameConfig.TICK_MS / 1000.0 * 1.5;
                myDist = Math.min(s.distance(), myDist + maxStep);
                if (hits(myLane, myDist)) {
                    myStunUntil = now + GameConfig.COLLISION_STUN_MS;
                    mySpeed = 0;
                }
            }
            case FINISH -> { }
            case QUIT_MATCH -> finish(false, EndReason.QUIT);
            case REMATCH_REPLY -> {
                RematchReply r = m.getPayload();
                if (r.agree()) {
                    Timer t = new Timer(800, e -> {
                        frame.handle(new Message(MessageType.MATCH_START, newMatch()));
                        countdownThenRace();
                    });
                    t.setRepeats(false);
                    t.start();
                } else {
                    Timer t = new Timer(500, e -> frame.handle(new Message(MessageType.ROOM_CLOSED, roomId)));
                    t.setRepeats(false);
                    t.start();
                }
            }
            default -> { }
        }
    }

    private boolean hits(int lane, double dist) {
        for (Obstacle o : obstacles) {
            if (o.lane() == lane && dist + GameConfig.CAR_LENGTH > o.position()
                    && dist < o.position() + GameConfig.OBSTACLE_LENGTH) {
                return true;
            }
        }
        return false;
    }

    // ---- "server" tick: xe đối thủ tự lái, đồng bộ, xét về đích

    private void serverTick() {
        if (over) {
            return;
        }
        long now = System.currentTimeMillis();
        tickNo++;
        double oppSpeed = 0;
        if (!oppFinished && now >= oppStunUntil) {
            oppSpeed = 140 + 30 * Math.sin(tickNo / 40.0);
            // né chướng ngại vật phía trước trong cùng làn
            for (Obstacle o : obstacles) {
                if (o.lane() == oppLane && o.position() - oppDist < 80 && o.position() - oppDist > 0) {
                    oppLane = oppLane == 0 ? 1 : (oppLane == 2 ? 1 : (RNG.nextBoolean() ? 0 : 2));
                    break;
                }
            }
            oppDist += oppSpeed * GameConfig.KMH_TO_MS * GameConfig.TICK_MS / 1000.0;
            if (RNG.nextInt(400) == 0 && hits(oppLane, oppDist)) {
                oppStunUntil = now + GameConfig.COLLISION_STUN_MS;
            }
            if (oppDist >= GameConfig.TRACK_LENGTH) {
                oppDist = GameConfig.TRACK_LENGTH;
                oppFinished = true;
            }
        }
        if (myDist >= GameConfig.TRACK_LENGTH) {
            myFinished = true;
        }
        frame.handle(new Message(MessageType.RACE_UPDATE, new RaceState(roomId, tickNo,
                new CarSnapshot("alice", myDist, myLane, now < myStunUntil ? 0 : mySpeed, now < myStunUntil, myFinished),
                new CarSnapshot("bob", oppDist, oppLane, now < oppStunUntil ? 0 : oppSpeed, now < oppStunUntil, oppFinished),
                now - startAt)));
        if (myFinished && oppFinished) {
            finish(true, EndReason.DRAW);
        } else if (myFinished) {
            finish(true, EndReason.FINISH);
        } else if (oppFinished) {
            finish(false, EndReason.FINISH);
        }
    }

    private void finish(boolean iWin, EndReason reason) {
        if (over) {
            return;
        }
        over = true;
        serverTick.stop();
        MatchOutcome outcome;
        if (reason == EndReason.DRAW) {
            outcome = MatchOutcome.DRAW;
            myPoints += GameConfig.POINTS_DRAW;
            oppPoints += GameConfig.POINTS_DRAW;
        } else if (iWin) {
            outcome = MatchOutcome.WIN;
            myPoints += GameConfig.POINTS_WIN;
        } else {
            outcome = MatchOutcome.LOSE;
            oppPoints += GameConfig.POINTS_WIN;
        }
        String winner = outcome == MatchOutcome.DRAW ? null : (iWin ? "alice" : "bob");
        frame.handle(new Message(MessageType.MATCH_RESULT, new MatchResult(roomId, matchId - 1, outcome, reason,
                winner, myPoints, oppPoints, System.currentTimeMillis() - startAt)));
        if (reason != EndReason.QUIT) {
            frame.handle(new Message(MessageType.REMATCH_ASK, roomId));
        } else {
            frame.handle(new Message(MessageType.ROOM_CLOSED, roomId));
        }
    }
}
