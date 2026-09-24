package racing.client;

import racing.common.GameConfig;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Điểm vào client Swing.
 * <pre>java -jar racing-client.jar [host] [port]</pre>
 * Mặc định localhost:5000; người dùng vẫn có thể sửa địa chỉ trên màn hình đăng nhập.
 */
public final class ClientMain {

    private ClientMain() {
    }

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? parsePort(args[1]) : GameConfig.PORT;
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // giữ look and feel mặc định
        }
        SwingUtilities.invokeLater(() -> new ClientApp(host, port).start());
    }

    private static int parsePort(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            System.err.println("Cổng không hợp lệ: " + s + ", dùng " + GameConfig.PORT);
            return GameConfig.PORT;
        }
    }
}
