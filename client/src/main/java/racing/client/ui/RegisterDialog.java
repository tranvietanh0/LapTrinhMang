package racing.client.ui;

import racing.client.net.NetworkClient;
import racing.common.dto.LoginRequest;
import racing.common.dto.LoginResult;
import racing.common.net.Message;
import racing.common.net.MessageType;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;

/** Hộp thoại đăng ký tài khoản mới: gửi REGISTER, chờ REGISTER_RESULT. Modal. */
public final class RegisterDialog extends JDialog {

    private static final int MIN_USERNAME = 3;
    private static final int MAX_USERNAME = 50;
    private static final int MIN_PASSWORD = 4;

    private final NetworkClient net;
    private final JTextField userField = new JTextField(18);
    private final JPasswordField passField = new JPasswordField(18);
    private final JPasswordField confirmField = new JPasswordField(18);
    private final JLabel errorLabel = new JLabel(" ");
    private final JButton okButton = new JButton("Đăng ký");
    private Runnable offResult;
    private String registeredUsername;

    public RegisterDialog(JFrame owner, NetworkClient net, String prefillUsername) {
        super(owner, "Đăng ký tài khoản", true);
        this.net = net;
        userField.setText(prefillUsername == null ? "" : prefillUsername);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        addRow(form, c, 0, "Tài khoản:", userField);
        addRow(form, c, 1, "Mật khẩu:", passField);
        addRow(form, c, 2, "Nhập lại mật khẩu:", confirmField);
        errorLabel.setForeground(new Color(0xB00020));
        c.gridx = 0; c.gridy = 3; c.gridwidth = 2;
        form.add(errorLabel, c);

        JPanel buttons = new JPanel();
        JButton cancel = new JButton("Huỷ");
        buttons.add(okButton);
        buttons.add(cancel);
        c.gridy = 4; c.anchor = GridBagConstraints.CENTER;
        form.add(buttons, c);

        setContentPane(form);
        getRootPane().setDefaultButton(okButton);
        okButton.addActionListener(e -> submit());
        cancel.addActionListener(e -> dispose());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                unregister();
            }
        });
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    /** Tên tài khoản vừa tạo, hoặc null nếu huỷ / thất bại. */
    public String registeredUsername() {
        return registeredUsername;
    }

    private static void addRow(JPanel p, GridBagConstraints c, int row, String label, JTextField field) {
        c.gridx = 0; c.gridy = row;
        p.add(new JLabel(label), c);
        c.gridx = 1;
        p.add(field, c);
    }

    private void submit() {
        String username = userField.getText().trim();
        char[] pass = passField.getPassword();
        char[] confirm = confirmField.getPassword();
        String problem = validate(username, pass, confirm);
        if (problem != null) {
            errorLabel.setText(problem);
            return;
        }
        okButton.setEnabled(false);
        errorLabel.setText("Đang gửi…");
        unregister();
        offResult = net.on(MessageType.REGISTER_RESULT, this::onResult);
        net.send(new Message(MessageType.REGISTER, new LoginRequest(username, new String(pass))));
    }

    /** Kiểm tra cục bộ trước khi gửi; server kiểm tra lại. Trả về thông báo lỗi hoặc null. */
    static String validate(String username, char[] pass, char[] confirm) {
        if (username.length() < MIN_USERNAME || username.length() > MAX_USERNAME) {
            return "Tên tài khoản phải từ " + MIN_USERNAME + " đến " + MAX_USERNAME + " ký tự.";
        }
        if (!username.matches("[A-Za-z0-9_]+")) {
            return "Tên tài khoản chỉ gồm chữ, số và dấu gạch dưới.";
        }
        if (pass.length < MIN_PASSWORD) {
            return "Mật khẩu phải có ít nhất " + MIN_PASSWORD + " ký tự.";
        }
        if (!Arrays.equals(pass, confirm)) {
            return "Mật khẩu nhập lại không khớp.";
        }
        return null;
    }

    private void onResult(Message m) {
        LoginResult r = m.getPayload();
        okButton.setEnabled(true);
        unregister();
        if (r != null && r.ok()) {
            registeredUsername = userField.getText().trim();
            dispose();
        } else {
            errorLabel.setText(r == null ? "Đăng ký thất bại." : r.message());
        }
    }

    private void unregister() {
        if (offResult != null) {
            offResult.run();
            offResult = null;
        }
    }
}
