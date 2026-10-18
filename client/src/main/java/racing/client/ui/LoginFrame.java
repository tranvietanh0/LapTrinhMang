package racing.client.ui;

import racing.client.ClientApp;
import racing.client.net.NetworkClient;
import racing.common.GameConfig;
import racing.common.dto.LoginRequest;
import racing.common.dto.LoginResult;
import racing.common.net.Message;
import racing.common.net.MessageType;
import racing.client.ui.theme.*;
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
    private final ChamferButton loginButton = new ChamferButton("VÀO SẢNH  →", true);
    private final JButton registerButton = GameTheme.button("Tạo tài khoản", false);
    private final RevealPanel stage = new RevealPanel(null);
    private final JPanel card;
    private Runnable offLoginResult;

    public LoginFrame(ClientApp app, String defaultHost, int defaultPort) {
        super("Đua xe online – Đăng nhập");
        this.app=app;
        serverField=new JTextField(defaultHost+":"+defaultPort,18);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        RevealPanel form=RevealPanel.column();
        form.add(GameTheme.eyebrow("PIT ENTRY / ĐĂNG NHẬP")); GameTheme.gap(form,6);
        form.add(RevealPanel.style(GameTheme.heading("SẴN SÀNG XUẤT PHÁT?"),RevealPanel.Style.LAND)); GameTheme.gap(form,4);
        form.add(GameTheme.label("Vào sảnh và tìm đối thủ của bạn.",GameTheme.BODY,GameTheme.MUTED));
        GameTheme.gap(form,22);
        GameTheme.formField(form,"SERVER / HOST:PORT",serverField);
        GameTheme.formField(form,"TÀI KHOẢN",userField);
        GameTheme.formField(form,"MẬT KHẨU",passField);
        GameTheme.fullWidth(errorLabel,40);
        GameTheme.status(errorLabel,"Nhập địa chỉ server để chơi cùng bạn bè.",GameTheme.MUTED);
        form.add(errorLabel); GameTheme.gap(form,4);
        GameTheme.fullWidth(loginButton,GameTheme.CONTROL_H); form.add(loginButton); GameTheme.gap(form,8);
        GameTheme.fullWidth(registerButton,GameTheme.CONTROL_H); form.add(registerButton); GameTheme.gap(form,14);
        form.add(GameTheme.label("W  Tăng tốc     A / D  Đổi làn     F11  Toàn màn hình",GameTheme.SMALL,GameTheme.MUTED));
        card=GameTheme.card(new BorderLayout(),26); card.add(form,BorderLayout.CENTER);
        HeroPanel hero=new HeroPanel("STREET RACING NETWORK","ĐƯỜNG ĐUA","CỦA BẠN.","Chọn đối thủ. Giữ tay lái. Vượt lên phía trước.",
                new String[][]{{"1 VS 1","ĐẤU TRỰC TIẾP"},{"2.500 M","ĐƯỜNG ĐUA"},{"3 LÀN","NÉ XE CỘ"}});
        RevealPanel.style(hero,RevealPanel.Style.SLIDE);
        stage.add(hero); stage.add(card);
        stage.setLayout(new HeroCardLayout(hero,card,440,1000,28));
        NfsBackdrop root=new NfsBackdrop();
        root.setLayout(new BorderLayout()); root.add(stage,BorderLayout.CENTER);
        GameTheme.decorate(this);
        ScreenFrame.fit(this,1280,800); ScreenFrame.install(this,root,new Dimension(900,600)); setLocationRelativeTo(null);
        getRootPane().setDefaultButton(loginButton);
        loginButton.addActionListener(e->login()); registerButton.addActionListener(e->register());
        addWindowListener(new WindowAdapter() { public void windowClosed(WindowEvent e) { unregister(); } });
        // entrance: hero slides in, card rises, then the form lines follow one by one; idle light sweep on the CTA
        stage.reveal(0,90,hero,card);
        form.reveal(200,RevealPanel.STAGGER);
        loginButton.setSheen(true);
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
        loginButton.setBusy(busy);
        if(busy) showInfo("Đang kết nối…");
    }
    private void showError(String text) { GameTheme.status(errorLabel,text,GameTheme.ERROR); stage.shake(card); }
    private void showInfo(String text) { GameTheme.status(errorLabel,text,GameTheme.SUCCESS); }
    private void unregister() { if(offLoginResult!=null) { offLoginResult.run(); offLoginResult=null; } }
}
