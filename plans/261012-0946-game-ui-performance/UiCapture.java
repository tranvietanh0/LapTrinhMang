import racing.client.ClientApp;
import racing.client.ui.*;
import racing.client.net.NetworkClient;
import racing.common.dto.LoginRequest;
import racing.common.dto.LoginResult;
import racing.common.net.*;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.*;

class UiCapture {
    static final File DIR=new File("plans/261012-0946-game-ui-performance/screenshots");
    static void capture(JFrame f,String name) throws Exception { capture(f.getContentPane(),name); }
    static void capture(Container component,String name) throws Exception {
        component.validate();
        checkControls(component);
        BufferedImage image=new BufferedImage(component.getWidth(),component.getHeight(),BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics(); component.printAll(g); g.dispose();
        ImageIO.write(image,"png",new File(DIR,name+".png"));
        System.out.println(name+" "+image.getWidth()+"x"+image.getHeight());
    }
    static void checkControls(Container root) {
        for(Component c:root.getComponents()) {
            if(c instanceof JButton b && b.getText()!=null && !b.getText().isBlank()) {
                int text=b.getFontMetrics(b.getFont()).stringWidth(b.getText());
                Insets insets=b.getInsets();
                if(text>b.getWidth()-insets.left-insets.right) throw new IllegalStateException("Clipped button: "+b.getText());
                if(b.getHeight()<44)throw new IllegalStateException("Small button target: "+b.getText());
            }
            if(c instanceof Container child)checkControls(child);
        }
    }
    static JButton button(Container root,String text) {
        for(Component c:root.getComponents()) {
            if(c instanceof JButton b && b.getText().contains(text)) return b;
            if(c instanceof Container child) { JButton result=button(child,text); if(result!=null)return result; }
        }
        return null;
    }
    public static void main(String[] args) throws Exception {
        try { run(args); }
        catch(Throwable error) { error.printStackTrace(); System.exit(1); }
    }
    static void run(String[] args) throws Exception {
        DIR.mkdirs();
        ClientApp app=new ClientApp("127.0.0.1",5000);
        SwingUtilities.invokeAndWait(()->{
            try {
                GameTheme.install();
                LoginFrame f=new LoginFrame(app,"127.0.0.1",5000); f.validate();
                capture(f,"login");
                JButton entry=button(f.getContentPane(),"VÀO SẢNH");
                entry.getModel().setRollover(true); capture(f,"login-hover"); entry.getModel().setRollover(false);
                f.setSize(1280,800);f.validate();capture(f,"login-wide");
                f.pack();f.validate();
                button(f.getContentPane(),"VÀO SẢNH").doClick(); capture(f,"login-error");
                RegisterDialog reg=new RegisterDialog(f,new NetworkClient(),""); reg.validate();
                capture(reg.getContentPane(),"register");
                button(reg.getContentPane(),"TẠO TÀI KHOẢN").doClick(); capture(reg.getContentPane(),"register-error");
                reg.dispose(); f.dispose();
                JPanel footer=new JPanel(new FlowLayout(FlowLayout.RIGHT));footer.setBackground(GameTheme.BG);
                footer.add(GameTheme.button("Thoát trận (Esc)",false));footer.setSize(500,64);footer.doLayout();capture(footer,"race-footer");
            } catch(Exception e) { throw new RuntimeException(e); }
        });
        NetworkClient net=app.ensureConnected("127.0.0.1",5000);
        CountDownLatch loggedIn=new CountDownLatch(1);
        net.on(MessageType.LOGIN_RESULT,m->{
            LoginResult result=m.getPayload();
            if(!result.ok()) throw new IllegalStateException(result.message());
            app.state().setMe(result.me()); loggedIn.countDown();
        });
        net.send(new Message(MessageType.LOGIN,new LoginRequest("carol","123456")));
        if(!loggedIn.await(10,TimeUnit.SECONDS)) throw new IllegalStateException("No login response");
        NetworkClient peer=new NetworkClient();peer.connect("127.0.0.1",5000);
        CountDownLatch peerLogin=new CountDownLatch(1);
        peer.on(MessageType.LOGIN_RESULT,m->{LoginResult result=m.getPayload();if(!result.ok())throw new IllegalStateException(result.message());peerLogin.countDown();});
        peer.send(new Message(MessageType.LOGIN,new LoginRequest("dave","123456")));
        if(!peerLogin.await(10,TimeUnit.SECONDS))throw new IllegalStateException("Peer login failed");
        Thread.sleep(300);
        SwingUtilities.invokeAndWait(()->{
            try {
                LobbyFrame lobby=new LobbyFrame(app); lobby.validate(); capture(lobby,"lobby");
                LeaderboardFrame ranks=new LeaderboardFrame(net); ranks.validate(); capture(ranks,"leaderboard-loading");
                ranks.dispose(); lobby.dispose();
            } catch(Exception e) { throw new RuntimeException(e); }
        });
        net.close(); peer.close(); System.exit(0);
    }
}
