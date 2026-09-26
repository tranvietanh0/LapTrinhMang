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
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Chạy thử màn hình đua KHÔNG cần server: một "server giả" trong tiến trình đếm ngược, nhận
 * CAR_STATE, cắt ngưỡng tốc độ, xét va chạm với xe cộ đang chạy, điều khiển một xe đối thủ tự động, xét về đích và
 * hỏi thi đấu tiếp. Luật ở đây chỉ để xem giao diện; luật thật nằm ở server của Phương.
 *
 * <pre>./mvnw -pl client -am exec:java -Dexec.mainClass=racing.client.ui.race.RaceDemo</pre>
 */
public final class RaceDemo {

    private static final Random RNG = new Random();
    /** Số xe cộ và tốc độ mỗi làn (km/h) chỉ cho bản demo; luật thật do server sinh. */
    private static final int TRAFFIC_COUNT = 18;
    private static final double[] LANE_SPEEDS = {70, 100, 130};

    private RaceFrame frame;
    private int roomId = 1;
    private int matchId = 100;
    private List<Obstacle> obstacles;
    private double myDist, mySpeed;
    private int myLane = 1;
    private long myStunUntil;
    private final Set<Integer> myHits = new HashSet<>();
    private final Set<Integer> oppHits = new HashSet<>();
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
        myHits.clear(); oppHits.clear();
        return new MatchStart(roomId, matchId++,
                new PlayerInfo(1, "alice", myPoints, 10, PlayerStatus.IN_MATCH),
                new PlayerInfo(2, "bob", oppPoints, 12, PlayerStatus.IN_MATCH),
                GameConfig.TRACK_LENGTH, obstacles);
    }

    /** Xe cộ chạy cùng chiều; mỗi làn một tốc độ nên xe cùng làn không đè nhau. */
    static List<Obstacle> randomObstacles() {
        double[] speeds = LANE_SPEEDS.clone();
        for (int i = speeds.length - 1; i > 0; i--) {
            int j = RNG.nextInt(i + 1);
            double t = speeds[i]; speeds[i] = speeds[j]; speeds[j] = t;
        }
        List<Obstacle> list = new ArrayList<>();
        double step = (GameConfig.TRACK_LENGTH - 150) / TRAFFIC_COUNT;
        for (int i = 0; i < TRAFFIC_COUNT; i++) {
            int lane = RNG.nextInt(GameConfig.LANES);
            list.add(new Obstacle(lane, 60 + i * step + RNG.nextInt(15), speeds[lane], RNG.nextInt(6)));
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
                if (hits(myLane, myDist, myHits)) {
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

    /** Va chạm với xe cộ ở vị trí hiện tại (positionAt(tick)); mỗi xe cộ chỉ bị tông một lần. */
    private boolean hits(int lane, double dist, Set<Integer> alreadyHit) {
        for (int i = 0; i < obstacles.size(); i++) {
            Obstacle o = obstacles.get(i);
            double p = o.positionAt(tickNo);
            if (o.lane() == lane && !alreadyHit.contains(i) && dist + GameConfig.CAR_LENGTH > p
                    && dist < p + GameConfig.OBSTACLE_LENGTH) {
                alreadyHit.add(i);
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
            // né xe cộ phía trước trong cùng làn (thỉnh thoảng né hụt cho có va chạm)
            for (int i = 0; i < obstacles.size(); i++) {
                Obstacle o = obstacles.get(i);
                double gap = o.positionAt(tickNo) - oppDist;
                if (o.lane() == oppLane && !oppHits.contains(i) && gap < 45 && gap > 0 && RNG.nextInt(6) != 0) {
                    oppLane = oppLane == 0 ? 1 : (oppLane == 2 ? 1 : (RNG.nextBoolean() ? 0 : 2));
                    break;
                }
            }
            oppDist += oppSpeed * GameConfig.KMH_TO_MS * GameConfig.TICK_MS / 1000.0;
            if (hits(oppLane, oppDist, oppHits)) {
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
