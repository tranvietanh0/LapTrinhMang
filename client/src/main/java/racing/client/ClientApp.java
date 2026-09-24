package racing.client;

import racing.client.model.ClientState;
import racing.client.net.NetworkClient;
import racing.client.ui.LobbyFrame;
import racing.client.ui.LoginFrame;
import racing.common.dto.PlayerInfo;
import racing.common.net.Message;
import racing.common.net.MessageType;

import javax.swing.JOptionPane;
import java.awt.Window;
import java.io.IOException;

/**
 * Điều phối vòng đời client: giữ kết nối mạng và trạng thái, chuyển giữa màn hình đăng nhập
 * và sảnh, xử lý đăng xuất và mất kết nối. Mọi phương thức gọi trên luồng Swing.
 */
public final class ClientApp {

    private final ClientState state = new ClientState();
    private final String defaultHost;
    private final int defaultPort;

    private NetworkClient net;
    private boolean loggingOut;

    public ClientApp(String defaultHost, int defaultPort) {
        this.defaultHost = defaultHost;
        this.defaultPort = defaultPort;
    }

    public ClientState state() {
        return state;
    }

    public NetworkClient net() {
        return net;
    }

    public void start() {
        showLogin(null);
    }

    /** Mở kết nối nếu chưa có (hoặc kết nối cũ đã đứt). Ném IOException khi không nối được. */
    public NetworkClient ensureConnected(String host, int port) throws IOException {
        if (net != null && net.isConnected()) {
            return net;
        }
        NetworkClient n = new NetworkClient();
        n.connect(host, port);
        n.onDisconnect(this::onDisconnected);
        net = n;
        loggingOut = false;
        return n;
    }

    /** Gọi khi LOGIN_RESULT ok: đóng màn hình đăng nhập, mở sảnh. */
    public void onLoggedIn(PlayerInfo me, Window loginWindow) {
        state.setMe(me);
        loginWindow.dispose();
        new LobbyFrame(this).setVisible(true);
    }

    /** Đăng xuất chủ động: báo server, đóng mọi cửa sổ, về màn hình đăng nhập. */
    public void logout() {
        loggingOut = true;
        if (net != null) {
            net.send(Message.of(MessageType.LOGOUT));
            net.close();
            net = null;
        }
        String username = state.me() == null ? null : state.me().username();
        resetState();
        disposeAllWindows();
        showLogin(username);
    }

    private void onDisconnected() {
        if (loggingOut) {
            return;
        }
        net = null;
        String username = state.me() == null ? null : state.me().username();
        resetState();
        disposeAllWindows();
        JOptionPane.showMessageDialog(null, "Mất kết nối tới server.", "Mất kết nối",
                JOptionPane.ERROR_MESSAGE);
        showLogin(username);
    }

    private void showLogin(String prefillUsername) {
        LoginFrame f = new LoginFrame(this, defaultHost, defaultPort);
        if (prefillUsername != null) {
            f.setUsername(prefillUsername);
        }
        f.setVisible(true);
    }

    private void resetState() {
        state.setMe(null);
        state.setOnline(null);
        state.clearPendingInvite();
    }

    private static void disposeAllWindows() {
        for (Window w : Window.getWindows()) {
            if (w.isDisplayable()) {
                w.dispose();
            }
        }
    }
}
