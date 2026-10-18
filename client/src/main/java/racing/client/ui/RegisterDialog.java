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
    private final JButton okButton = GameTheme.button("TẠO TÀI KHOẢN  →",true);
    private Runnable offResult;
    private String registeredUsername;

    public RegisterDialog(JFrame owner, NetworkClient net, String prefillUsername) {
        super(owner, "Đăng ký tài khoản", true);
        this.net = net;
        userField.setText(prefillUsername == null ? "" : prefillUsername);

        JPanel form = GameTheme.column();
        form.setOpaque(true); form.setBackground(GameTheme.BG);
        form.setBorder(BorderFactory.createEmptyBorder(32,40,32,40));
        form.add(GameTheme.eyebrow("NEW DRIVER / ĐĂNG KÝ")); GameTheme.gap(form,16);
        form.add(GameTheme.label("Tên bạn trên đường đua.",GameTheme.TITLE.deriveFont(28f),GameTheme.TEXT)); GameTheme.gap(form,8);
        form.add(GameTheme.label("Tạo tài khoản để lưu điểm và lịch sử trận.",GameTheme.BODY,GameTheme.MUTED));
        GameTheme.gap(form,24);
        GameTheme.formField(form,"TÀI KHOẢN",userField);
        form.add(GameTheme.label("3–50 ký tự: chữ không dấu, số hoặc _",GameTheme.SMALL,GameTheme.MUTED));
        GameTheme.gap(form,16);
        GameTheme.formField(form,"MẬT KHẨU / ÍT NHẤT 4 KÝ TỰ",passField);
        GameTheme.formField(form,"NHẬP LẠI MẬT KHẨU",confirmField);
        GameTheme.fullWidth(errorLabel,48); GameTheme.status(errorLabel,"",GameTheme.ERROR); form.add(errorLabel);
        GameTheme.fullWidth(okButton,GameTheme.CONTROL_H); form.add(okButton); GameTheme.gap(form,12);
        JButton cancel = GameTheme.button("Quay lại đăng nhập",false);
        GameTheme.fullWidth(cancel,GameTheme.CONTROL_H); form.add(cancel);

        setContentPane(form);
        GameTheme.decorate(this);
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

    private void submit() {
        String username = userField.getText().trim();
        char[] pass = passField.getPassword();
        char[] confirm = confirmField.getPassword();
        String problem = validate(username, pass, confirm);
        if (problem != null) {
            GameTheme.status(errorLabel,problem,GameTheme.ERROR);
            Arrays.fill(pass,'\0'); Arrays.fill(confirm,'\0');
            return;
        }
        okButton.setEnabled(false);
        userField.setEnabled(false); passField.setEnabled(false); confirmField.setEnabled(false);
        okButton.setText("ĐANG TẠO TÀI KHOẢN…");
        GameTheme.status(errorLabel,"Đang tạo tài khoản…",GameTheme.MUTED);
        unregister();
        offResult = net.on(MessageType.REGISTER_RESULT, this::onResult);
        net.send(new Message(MessageType.REGISTER, new LoginRequest(username, new String(pass))));
        Arrays.fill(pass,'\0'); Arrays.fill(confirm,'\0');
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
        userField.setEnabled(true); passField.setEnabled(true); confirmField.setEnabled(true);
        okButton.setText("TẠO TÀI KHOẢN  →");
        unregister();
        if (r != null && r.ok()) {
            registeredUsername = userField.getText().trim();
            dispose();
        } else {
            GameTheme.status(errorLabel,r == null ? "Đăng ký thất bại." : r.message(),GameTheme.ERROR);
        }
    }

    private void unregister() {
        if (offResult != null) {
            offResult.run();
            offResult = null;
        }
    }
}
