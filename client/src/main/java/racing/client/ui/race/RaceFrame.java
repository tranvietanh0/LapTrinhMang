package racing.client.ui.race;

import racing.client.model.CarModel;
import racing.common.GameConfig;
import racing.common.dto.MatchResult;
import racing.common.dto.MatchStart;
import racing.common.dto.RaceState;
import racing.common.dto.RematchReply;
import racing.common.net.Message;
import racing.common.net.MessageType;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;

/**
 * Cửa sổ màn hình đua. Không phụ thuộc NetworkClient: nhận một {@code sender} để gửi Message và
 * cung cấp {@link #handle(Message)} để lớp mạng đẩy thông điệp vào (gọi trên luồng Swing).
 *
 * <p>Cách dùng từ sảnh (Hiếu):
 * <pre>
 *   RaceFrame f = new RaceFrame(matchStart, net::send, () -> lobby.setVisible(true));
 *   net.on(COUNTDOWN | RACE_UPDATE | MATCH_RESULT | REMATCH_ASK | MATCH_START | ROOM_CLOSED, f::handle);
 *   f.setVisible(true);
 * </pre>
 */
public final class RaceFrame extends JFrame {

    private enum Phase { WAITING, COUNTDOWN, RACING, FINISHED }

    private final Consumer<Message> sender;
    private final Runnable onClosed;
    private final CarModel myCar;
    private final CarModel opponentCar;
    private final RacePanel panel;
    private final Timer tick;

    private MatchStart match;
    private Phase phase = Phase.WAITING;
    private boolean finishSent;
    private boolean rematchPending;
    private boolean resultShowing;
    private long raceStartMillis;

    public RaceFrame(MatchStart start, Consumer<Message> sender, Runnable onClosed) {
        super("Đua xe – " + start.me().username() + " vs " + start.opponent().username());
        this.sender = sender;
        this.onClosed = onClosed == null ? () -> { } : onClosed;
        this.myCar = new CarModel(start.me().username(), 1);
        this.opponentCar = new CarModel(start.opponent().username(), 1);
        this.panel = new RacePanel(myCar, opponentCar);
        this.tick = new Timer(GameConfig.TICK_MS, e -> onTick());

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmQuit();
            }
        });

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottom.setBorder(BorderFactory.createEmptyBorder(4, 0, 8, 0));
        JButton quit = new JButton("Thoát trận (Esc)");
        quit.setFocusable(false);
        quit.addActionListener(e -> confirmQuit());
        bottom.add(quit);

        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        bindKeys();
        pack();
        setLocationRelativeTo(null);

        startRound(start);
    }

    // ------------------------------------------------------------------ vòng đời ván

    private void startRound(MatchStart start) {
        this.match = start;
        myCar.reset(1);
        opponentCar.reset(1);
        finishSent = false;
        rematchPending = false;
        phase = Phase.WAITING;
        panel.setObstacles(start.obstacles());
        panel.setTitles(start.me().username(), start.opponent().username());
        panel.setStatusText("Đang chờ server đếm ngược…");
        panel.setCountdownText(null);
        panel.repaint();
    }

    /** Lớp mạng gọi hàm này với mọi thông điệp liên quan tới trận. Phải gọi trên luồng Swing. */
    public void handle(Message m) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> handle(m));
            return;
        }
        switch (m.getType()) {
            case COUNTDOWN -> onCountdown(m.getPayload());
            case RACE_UPDATE -> onRaceUpdate(m.getPayload());
            case MATCH_RESULT -> onResult(m.getPayload());
            case REMATCH_ASK -> onRematchAsk();
            case MATCH_START -> startRound(m.getPayload());
            case ROOM_CLOSED -> closeFrame();
            default -> { }
        }
    }

    private void onCountdown(Integer value) {
        if (value == null) {
            return;
        }
        if (value > 0) {
            phase = Phase.COUNTDOWN;
            panel.setStatusText("Sẵn sàng…");
            panel.setCountdownText(String.valueOf(value));
        } else {
            phase = Phase.RACING;
            raceStartMillis = System.currentTimeMillis();
            panel.setStatusText("Đang đua");
            panel.setCountdownText("GO!");
            new Timer(600, e -> panel.setCountdownText(null)) {{ setRepeats(false); }}.start();
            tick.start();
        }
    }

    private void onTick() {
        if (phase != Phase.RACING) {
            return;
        }
        myCar.advance(GameConfig.TICK_MS / 1000.0);
        sender.accept(new Message(MessageType.CAR_STATE, myCar.toState()));
        if (myCar.finished() && !finishSent) {
            finishSent = true;
            sender.accept(new Message(MessageType.FINISH, System.currentTimeMillis()));
        }
        panel.repaint();
    }

    private void onRaceUpdate(RaceState s) {
        if (s == null || match == null || s.roomId() != match.roomId()) {
            return;
        }
        panel.setRaceTick(s.tick());
        if (myCar.applyServer(s.me())) {
            panel.flashMine();
        }
        if (opponentCar.applyServer(s.opponent())) {
            panel.flashOpponent();
        }
        if (myCar.finished() && !finishSent) {
            finishSent = true;
            sender.accept(new Message(MessageType.FINISH, System.currentTimeMillis()));
        }
        panel.repaint();
    }

    private void onResult(MatchResult r) {
        phase = Phase.FINISHED;
        tick.stop();
        panel.setCountdownText(null);
        panel.setStatusText("Trận đấu kết thúc");
        panel.repaint();
        resultShowing = true;
        ResultDialog.show(this, r, myCar.username(), opponentCar.username());
        resultShowing = false;
        if (rematchPending) {
            rematchPending = false;
            askRematch();
        }
    }

    private void onRematchAsk() {
        if (resultShowing) {
            rematchPending = true;   // hỏi sau khi người chơi đóng hộp kết quả
        } else {
            askRematch();
        }
    }

    private void askRematch() {
        boolean agree = RematchDialog.ask(this, opponentCar.username());
        sender.accept(new Message(MessageType.REMATCH_REPLY, new RematchReply(match.roomId(), agree)));
        if (!agree) {
            panel.setStatusText("Đã từ chối thi đấu tiếp, đang về sảnh…");
        } else {
            panel.setStatusText("Đang chờ đối thủ trả lời…");
        }
        panel.repaint();
    }

    private void confirmQuit() {
        if (phase == Phase.FINISHED) {
            closeFrame();
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "Thoát trận bây giờ sẽ bị xử thua. Bạn chắc chứ?", "Thoát trận",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            tick.stop();
            phase = Phase.FINISHED;
            sender.accept(new Message(MessageType.QUIT_MATCH, match.roomId()));
            // server sẽ trả MATCH_RESULT (thua) rồi ROOM_CLOSED; nếu không nhận được thì vẫn đóng sau 3 s
            new Timer(3000, e -> closeFrame()) {{ setRepeats(false); }}.start();
        }
    }

    private void closeFrame() {
        if (!isDisplayable()) {
            return;
        }
        tick.stop();
        dispose();
        onClosed.run();
    }

    // ------------------------------------------------------------------ phím

    private void bindKeys() {
        JComponent root = getRootPane();
        bindHold(root, "W", myCar::setThrottle);
        bindHold(root, "UP", myCar::setThrottle);
        bindHold(root, "S", myCar::setBrake);
        bindHold(root, "DOWN", myCar::setBrake);
        bind(root, "A", myCar::laneLeft);
        bind(root, "LEFT", myCar::laneLeft);
        bind(root, "D", myCar::laneRight);
        bind(root, "RIGHT", myCar::laneRight);
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "quit");
        root.getActionMap().put("quit", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmQuit();
            }
        });
    }

    private void bind(JComponent c, String key, Runnable action) {
        c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), key);
        c.getActionMap().put(key, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (phase == Phase.RACING) {   // khóa phím trước GO và sau khi kết thúc
                    action.run();
                    panel.repaint();
                }
            }
        });
    }

    /** Phím giữ: gửi true khi nhấn, false khi nhả (bỏ qua lặp phím của hệ điều hành). */
    private void bindHold(JComponent c, String key, java.util.function.Consumer<Boolean> action) {
        for (boolean released : new boolean[] {false, true}) {
            String name = key + (released ? "-up" : "-down");
            c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .put(KeyStroke.getKeyStroke((released ? "released " : "pressed ") + key), name);
            c.getActionMap().put(name, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // nhả phím luôn được nhận, kể cả trước GO, để không kẹt ga
                    if (released || phase == Phase.RACING) {
                        action.accept(!released);
                    }
                }
            });
        }
    }

    public long elapsedMillis() {
        return phase == Phase.RACING ? System.currentTimeMillis() - raceStartMillis : 0;
    }
}
