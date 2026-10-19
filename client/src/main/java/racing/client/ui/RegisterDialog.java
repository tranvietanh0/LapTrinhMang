package racing.client.ui;

import racing.client.net.NetworkClient;
import racing.common.dto.LoginRequest;
import racing.common.dto.LoginResult;
import racing.common.net.Message;
import racing.common.net.MessageType;

import racing.client.ui.theme.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
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
    private final ChamferButton okButton = new ChamferButton("TẠO TÀI KHOẢN  →",true);
    private final RevealPanel stage = new RevealPanel(null);
    private final JPanel card;
    private Runnable offResult;
    private String registeredUsername;

    public RegisterDialog(JFrame owner, NetworkClient net, String prefillUsername) {
        super(owner, "Đăng ký tài khoản", true);
        this.net = net;
        userField.setText(prefillUsername == null ? "" : prefillUsername);

        RevealPanel form = RevealPanel.column();
        form.add(GameTheme.eyebrow("NEW DRIVER / ĐĂNG KÝ")); GameTheme.gap(form,6);
        form.add(RevealPanel.style(GameTheme.heading("TÊN BẠN TRÊN ĐƯỜNG ĐUA"),RevealPanel.Style.LAND)); GameTheme.gap(form,4);
        form.add(GameTheme.label("Tạo tài khoản để lưu điểm và lịch sử trận.",GameTheme.BODY,GameTheme.MUTED));
        GameTheme.gap(form,18);
        GameTheme.formField(form,"TÀI KHOẢN",userField);
        form.add(GameTheme.label("3–50 ký tự: chữ không dấu, số hoặc _",GameTheme.SMALL,GameTheme.MUTED));
        GameTheme.gap(form,14);
        GameTheme.formField(form,"MẬT KHẨU / ÍT NHẤT 4 KÝ TỰ",passField);
        GameTheme.formField(form,"NHẬP LẠI MẬT KHẨU",confirmField);
        GameTheme.fullWidth(errorLabel,36); GameTheme.status(errorLabel,"",GameTheme.ERROR); form.add(errorLabel);
        GameTheme.fullWidth(okButton,GameTheme.CONTROL_H); form.add(okButton); GameTheme.gap(form,8);
        JButton cancel = GameTheme.button("Quay lại đăng nhập",false);
        GameTheme.fullWidth(cancel,GameTheme.CONTROL_H); form.add(cancel);

        card = GameTheme.card(new BorderLayout(),26); card.add(form,BorderLayout.CENTER);
        stage.add(card);
        stage.setLayout(new HeroCardLayout(null,card,460,0,24));
        NfsBackdrop root = new NfsBackdrop();
        root.setLayout(new BorderLayout()); root.add(stage,BorderLayout.CENTER);
        GameTheme.decorate(this);
        ScreenFrame.fit(this,Math.round(620*Responsive.scaleOf(owner)),Math.round(880*Responsive.scaleOf(owner)));
        ScreenFrame.install(this,root,owner);
        setMinimumSize(new Dimension(420,420));
        getRootPane().setDefaultButton(okButton);
        okButton.addActionListener(e -> submit());
        cancel.addActionListener(e -> dispose());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                unregister();
            }
        });
        validate();
        setSize(getWidth(),Math.min(getHeight(),card.getPreferredSize().height+Math.round(48*Responsive.scaleOf(this))));
        setLocationRelativeTo(owner);
        // entrance: card rises, form lines follow one by one; idle light sweep on the CTA
        stage.reveal(0,0,card);
        form.reveal(140,RevealPanel.STAGGER);
        okButton.setSheen(true);
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
            showError(problem);
            Arrays.fill(pass,'\0'); Arrays.fill(confirm,'\0');
            return;
        }
        okButton.setEnabled(false);
        userField.setEnabled(false); passField.setEnabled(false); confirmField.setEnabled(false);
        okButton.setText("ĐANG TẠO TÀI KHOẢN…");
        okButton.setBusy(true);
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
        okButton.setBusy(false);
        unregister();
        if (r != null && r.ok()) {
            registeredUsername = userField.getText().trim();
            dispose();
        } else {
            showError(r == null ? "Đăng ký thất bại." : r.message());
        }
    }

    private void showError(String text) {
        GameTheme.status(errorLabel,text,GameTheme.ERROR);
        stage.shake(card);
    }

    private void unregister() {
        if (offResult != null) {
            offResult.run();
            offResult = null;
        }
    }
}
