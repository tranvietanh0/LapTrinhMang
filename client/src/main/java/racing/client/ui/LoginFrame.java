package racing.client.ui;

import racing.client.ClientApp;
import racing.client.net.NetworkClient;
import racing.common.GameConfig;
import racing.common.dto.LoginRequest;
import racing.common.dto.LoginResult;
import racing.common.net.Message;
import racing.common.net.MessageType;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/** Game entry screen. Connection setup runs off the Swing event thread. */
public final class LoginFrame extends JFrame {
    private final ClientApp app;
    private final JTextField serverField;
    private final JTextField userField = new JTextField(18);
    private final JPasswordField passField = new JPasswordField(18);
    private final JLabel errorLabel = new JLabel();
    private final JButton loginButton = GameTheme.button("VÀO SẢNH  →", true);
    private final JButton registerButton = GameTheme.button("Tạo tài khoản", false);
    private Runnable offLoginResult;

    public LoginFrame(ClientApp app, String defaultHost, int defaultPort) {
        super("Đua xe online – Đăng nhập");
        this.app=app;
        serverField=new JTextField(defaultHost+":"+defaultPort,18);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        JPanel root=new JPanel(new BorderLayout()); root.setBackground(GameTheme.BG);
        root.add(new GaragePanel(),BorderLayout.CENTER);
        JPanel form=GameTheme.column();
        form.setBorder(BorderFactory.createEmptyBorder(48,40,32,40));
        form.setPreferredSize(new Dimension(440,680));
        form.add(GameTheme.eyebrow("PIT ENTRY / ĐĂNG NHẬP")); GameTheme.gap(form,16);
        form.add(GameTheme.heading("Sẵn sàng xuất phát?")); GameTheme.gap(form,8);
        form.add(GameTheme.label("Vào sảnh và tìm đối thủ của bạn.",GameTheme.BODY,GameTheme.MUTED));
        GameTheme.gap(form,32);
        GameTheme.formField(form,"SERVER / HOST:PORT",serverField);
        GameTheme.formField(form,"TÀI KHOẢN",userField);
        GameTheme.formField(form,"MẬT KHẨU",passField);
        GameTheme.fullWidth(errorLabel,48);
        GameTheme.status(errorLabel,"Nhập địa chỉ server để chơi cùng bạn bè.",GameTheme.MUTED);
        form.add(errorLabel); GameTheme.gap(form,8);
        GameTheme.fullWidth(loginButton,GameTheme.CONTROL_H); form.add(loginButton); GameTheme.gap(form,16);
        GameTheme.fullWidth(registerButton,GameTheme.CONTROL_H); form.add(registerButton); GameTheme.gap(form,24);
        form.add(GameTheme.label("W / ↑  Tăng tốc     A / D  Đổi làn",GameTheme.SMALL,GameTheme.MUTED));
        root.add(form,BorderLayout.EAST); setContentPane(root); GameTheme.decorate(this);
        getRootPane().setDefaultButton(loginButton);
        loginButton.addActionListener(e->login()); registerButton.addActionListener(e->register());
        addWindowListener(new WindowAdapter() { public void windowClosed(WindowEvent e) { unregister(); } });
        pack(); setMinimumSize(new Dimension(980,720)); setLocationRelativeTo(null);
    }
    public void setUsername(String username) { userField.setText(username); passField.requestFocusInWindow(); }
    private void login() {
        String username=userField.getText().trim(), password=new String(passField.getPassword());
        if(username.isEmpty() || password.isEmpty()) { showError("Nhập tài khoản và mật khẩu."); return; }
        connectAsync(net->{
            unregister(); offLoginResult=net.on(MessageType.LOGIN_RESULT,this::onLoginResult);
            net.send(new Message(MessageType.LOGIN,new LoginRequest(username,password)));
            showInfo("Đang xác thực tài khoản…");
        });
    }
    private void onLoginResult(Message m) {
        LoginResult r=m.getPayload(); setBusy(false); unregister();
        if(r!=null && r.ok()) app.onLoggedIn(r.me(),this);
        else { showError(r==null?"Đăng nhập thất bại.":r.message()); passField.selectAll(); passField.requestFocusInWindow(); }
    }
    private void register() {
        connectAsync(net->{
            setBusy(false);
            RegisterDialog dialog=new RegisterDialog(this,net,userField.getText().trim()); dialog.setVisible(true);
            if(dialog.registeredUsername()!=null) {
                userField.setText(dialog.registeredUsername()); passField.setText(""); passField.requestFocusInWindow();
                showInfo("Đăng ký thành công, hãy đăng nhập.");
            }
        });
    }
    private void connectAsync(Consumer<NetworkClient> connected) {
        String[] parts=serverField.getText().trim().split(":",-1);
        String host=parts[0].isEmpty()?"localhost":parts[0];
        final int port;
        try {
            port=parts.length>1?Integer.parseInt(parts[1].trim()):GameConfig.PORT;
            if(parts.length>2 || port<1 || port>65535) throw new NumberFormatException();
        } catch(NumberFormatException e) { showError("Cổng phải là số từ 1 đến 65535."); return; }
        setBusy(true);
        new SwingWorker<NetworkClient,Void>() {
            protected NetworkClient doInBackground() throws IOException { return app.ensureConnected(host,port); }
            protected void done() {
                if(!isDisplayable()) return;
                try {
                    NetworkClient client=get();
                    if(!client.isConnected()) { setBusy(false); showError("Server đã ngắt kết nối. Hãy thử lại."); return; }
                    connected.accept(client);
                }
                catch(InterruptedException e) { Thread.currentThread().interrupt(); setBusy(false); showError("Đã hủy kết nối."); }
                catch(ExecutionException e) { setBusy(false); showError("Không kết nối được tới "+host+":"+port+". Kiểm tra server và mạng."); }
            }
        }.execute();
    }
    private void setBusy(boolean busy) {
        loginButton.setEnabled(!busy); registerButton.setEnabled(!busy);
        serverField.setEnabled(!busy); userField.setEnabled(!busy); passField.setEnabled(!busy);
        loginButton.setText(busy?"ĐANG KẾT NỐI…":"VÀO SẢNH  →");
        if(busy) showInfo("Đang kết nối…");
    }
    private void showError(String text) { GameTheme.status(errorLabel,text,GameTheme.ERROR); }
    private void showInfo(String text) { GameTheme.status(errorLabel,text,GameTheme.SUCCESS); }
    private void unregister() { if(offLoginResult!=null) { offLoginResult.run(); offLoginResult=null; } }
}
