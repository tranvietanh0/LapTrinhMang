package racing.client.ui;

import racing.client.ClientApp;
import racing.client.net.NetworkClient;
import racing.common.GameConfig;
import racing.common.dto.LoginRequest;
import racing.common.dto.LoginResult;
import racing.common.net.Message;
import racing.common.net.MessageType;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

/** Màn hình đăng nhập: địa chỉ server, tài khoản, mật khẩu; nút Đăng ký mở {@link RegisterDialog}. */
public final class LoginFrame extends JFrame {

    private final ClientApp app;
    private final JTextField serverField;
    private final JTextField userField = new JTextField(18);
    private final JPasswordField passField = new JPasswordField(18);
    private final JLabel errorLabel = new JLabel(" ");
    private final JButton loginButton = new JButton("Đăng nhập");
    private final JButton registerButton = new JButton("Đăng ký");
    private Runnable offLoginResult;

    public LoginFrame(ClientApp app, String defaultHost, int defaultPort) {
        super("Đua xe online – Đăng nhập");
        this.app = app;
        this.serverField = new JTextField(defaultHost + ":" + defaultPort, 18);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(16, 24, 12, 24));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;

        JLabel title = new JLabel("GAME ĐUA XE ĐỐI KHÁNG");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2; c.anchor = GridBagConstraints.CENTER;
        form.add(title, c);
        c.gridwidth = 1; c.anchor = GridBagConstraints.WEST;

        addRow(form, c, 1, "Server (host:port):", serverField);
        addRow(form, c, 2, "Tài khoản:", userField);
        addRow(form, c, 3, "Mật khẩu:", passField);

        errorLabel.setForeground(new Color(0xB00020));
        c.gridx = 0; c.gridy = 4; c.gridwidth = 2;
        form.add(errorLabel, c);

        JPanel buttons = new JPanel();
        buttons.add(loginButton);
        buttons.add(registerButton);
        c.gridy = 5; c.anchor = GridBagConstraints.CENTER;
        form.add(buttons, c);

        setContentPane(form);
        getRootPane().setDefaultButton(loginButton);   // Enter = Đăng nhập
        loginButton.addActionListener(e -> login());
        registerButton.addActionListener(e -> register());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                unregister();
            }
        });
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }

    public void setUsername(String username) {
        userField.setText(username);
        passField.requestFocusInWindow();
    }

    private static void addRow(JPanel p, GridBagConstraints c, int row, String label, JTextField field) {
        c.gridx = 0; c.gridy = row;
        p.add(new JLabel(label), c);
        c.gridx = 1;
        p.add(field, c);
    }

    // ------------------------------------------------------------------ hành động

    private void login() {
        String username = userField.getText().trim();
        String password = new String(passField.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            showError("Nhập tài khoản và mật khẩu.");
            return;
        }
        NetworkClient net = connect();
        if (net == null) {
            return;
        }
        setBusy(true);
        unregister();
        offLoginResult = net.on(MessageType.LOGIN_RESULT, this::onLoginResult);
        net.send(new Message(MessageType.LOGIN, new LoginRequest(username, password)));
    }

    private void onLoginResult(Message m) {
        LoginResult r = m.getPayload();
        setBusy(false);
        unregister();
        if (r != null && r.ok()) {
            app.onLoggedIn(r.me(), this);
        } else {
            showError(r == null ? "Đăng nhập thất bại." : r.message());
            passField.selectAll();
            passField.requestFocusInWindow();
        }
    }

    private void register() {
        NetworkClient net = connect();
        if (net == null) {
            return;
        }
        RegisterDialog dialog = new RegisterDialog(this, net, userField.getText().trim());
        dialog.setVisible(true);
        if (dialog.registeredUsername() != null) {
            userField.setText(dialog.registeredUsername());
            passField.setText("");
            passField.requestFocusInWindow();
            showInfo("Đăng ký thành công, hãy đăng nhập.");
        }
    }

    /** Kết nối theo ô địa chỉ; trả về null (và hiện lỗi) khi không nối được. */
    private NetworkClient connect() {
        String[] parts = serverField.getText().trim().split(":");
        String host = parts[0].isEmpty() ? "localhost" : parts[0];
        int port;
        try {
            port = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : GameConfig.PORT;
        } catch (NumberFormatException e) {
            showError("Cổng không hợp lệ.");
            return null;
        }
        try {
            return app.ensureConnected(host, port);
        } catch (IOException e) {
            showError("Không kết nối được tới " + host + ":" + port + " (" + e.getMessage() + ")");
            return null;
        }
    }

    private void setBusy(boolean busy) {
        loginButton.setEnabled(!busy);
        registerButton.setEnabled(!busy);
        if (busy) {
            showInfo("Đang đăng nhập…");
        }
    }

    private void showError(String text) {
        errorLabel.setForeground(new Color(0xB00020));
        errorLabel.setText(text);
    }

    private void showInfo(String text) {
        errorLabel.setForeground(new Color(0x2E7D32));
        errorLabel.setText(text);
    }

    private void unregister() {
        if (offLoginResult != null) {
            offLoginResult.run();
            offLoginResult = null;
        }
    }
}
