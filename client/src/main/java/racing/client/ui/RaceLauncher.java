package racing.client.ui;

import racing.client.net.NetworkClient;
import racing.client.ui.race.RaceFrame;
import racing.common.dto.MatchStart;
import racing.common.net.MessageType;

import java.util.ArrayList;
import java.util.List;

/**
 * Mở {@link RaceFrame} khi nhận MATCH_START, nối thông điệp trận vào {@code RaceFrame.handle}
 * và gỡ mọi listener khi cửa sổ đua đóng. Một sảnh giữ một launcher.
 */
public final class RaceLauncher {

    private static final MessageType[] RACE_TYPES = {
        MessageType.COUNTDOWN, MessageType.RACE_UPDATE, MessageType.MATCH_RESULT,
        MessageType.REMATCH_ASK, MessageType.MATCH_START, MessageType.ROOM_CLOSED
    };

    private final NetworkClient net;
    private final Runnable onClosed;
    private RaceFrame frame;
    private final List<Runnable> offs = new ArrayList<>();

    public RaceLauncher(NetworkClient net, Runnable onClosed) {
        this.net = net;
        this.onClosed = onClosed;
    }

    public boolean isOpen() {
        return frame != null;
    }

    /** Mở cửa sổ đua cho ván đầu của phòng. Ván tiếp theo (rematch) đi thẳng vào RaceFrame.handle. */
    public void open(MatchStart start) {
        if (frame != null) {
            return;
        }
        RaceFrame f = new RaceFrame(start, net::send, this::closed);
        frame = f;
        for (MessageType t : RACE_TYPES) {
            offs.add(net.on(t, f::handle));
        }
        f.setVisible(true);
    }

    private void closed() {
        offs.forEach(Runnable::run);
        offs.clear();
        frame = null;
        onClosed.run();
    }
}
