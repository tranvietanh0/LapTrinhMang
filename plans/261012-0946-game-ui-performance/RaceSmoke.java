import racing.client.net.NetworkClient;
import racing.common.GameConfig;
import racing.common.dto.*;
import racing.common.net.*;
import racing.server.db.DbConnection;
import racing.client.ui.GameTheme;
import racing.client.ui.race.RaceFrame;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Local integration probe using existing sample accounts; backs up the DB before the match. */
class RaceSmoke {
    static void backup() throws Exception {
        Path destination=Path.of("dist/private-backups/racing-before-ui-smoke-"+System.currentTimeMillis()+".sql");
        try(Connection c=DbConnection.get(); BufferedWriter out=Files.newBufferedWriter(destination,StandardCharsets.UTF_8)) {
            c.setAutoCommit(false);
            out.write("CREATE DATABASE IF NOT EXISTS racing; USE racing; SET FOREIGN_KEY_CHECKS=0;\n");
            for(String table:List.of("players","matches","match_events")) {
                try(Statement s=c.createStatement(); ResultSet r=s.executeQuery("SHOW CREATE TABLE "+table)) {
                    r.next(); out.write("DROP TABLE IF EXISTS "+table+";\n"+r.getString(2)+";\n");
                }
                try(Statement s=c.createStatement(); ResultSet r=s.executeQuery("SELECT * FROM "+table)) {
                    int count=r.getMetaData().getColumnCount();
                    while(r.next()) {
                        out.write("INSERT INTO "+table+" VALUES (");
                        for(int col=1;col<=count;col++) {
                            if(col>1)out.write(",");
                            String value=r.getString(col);
                            out.write(value==null ? "NULL" : "CONVERT(X'"+HexFormat.of().formatHex(value.getBytes(StandardCharsets.UTF_8))+"' USING utf8mb4)");
                        }
                        out.write(");\n");
                    }
                }
            }
            out.write("SET FOREIGN_KEY_CHECKS=1;\n"); c.commit();
        }
        if(Files.size(destination)==0)throw new IllegalStateException("Empty backup");
        System.out.println("Backup="+destination);
    }
    static void login(NetworkClient net,String user) throws Exception {
        CountDownLatch ready=new CountDownLatch(1); AtomicReference<LoginResult> result=new AtomicReference<>();
        Runnable off=net.on(MessageType.LOGIN_RESULT,m->{ result.set(m.getPayload());ready.countDown(); });
        net.send(new Message(MessageType.LOGIN,new LoginRequest(user,"123456")));
        if(!ready.await(10,TimeUnit.SECONDS)||!result.get().ok())throw new IllegalStateException("Login failed: "+user);
        off.run();
    }
    public static void main(String[] args) throws Exception {
        backup();
        boolean ui=args.length>0 && args[0].equals("ui");
        AtomicReference<RaceFrame> frame=new AtomicReference<>();
        if(ui)SwingUtilities.invokeAndWait(GameTheme::install);
        NetworkClient a=new NetworkClient(), b=new NetworkClient();
        AtomicReference<MatchStart> match=new AtomicReference<>();
        CountDownLatch started=new CountDownLatch(1),go=new CountDownLatch(1),ended=new CountDownLatch(1);
        List<Double> intervals=new CopyOnWriteArrayList<>(); AtomicLong previous=new AtomicLong();
        AtomicInteger collisions=new AtomicInteger(); AtomicBoolean wasStunned=new AtomicBoolean();
        try {
            a.connect("127.0.0.1",5000); b.connect("127.0.0.1",5000);
            a.on(MessageType.MATCH_START,m->{
                match.set(m.getPayload());
                if(ui){RaceFrame f=new RaceFrame(match.get(),a::send,()->{});frame.set(f);f.setVisible(true);}
                started.countDown();
            });
            if(ui)a.onAny(m->{if(frame.get()!=null && m.getType()!=MessageType.MATCH_START)frame.get().handle(m);});
            a.on(MessageType.COUNTDOWN,m->{Integer value=m.getPayload();if(value==0)go.countDown();});
            a.on(MessageType.RACE_UPDATE,m->{
                long now=System.nanoTime(), old=previous.getAndSet(now);if(old!=0)intervals.add((now-old)/1e6);
                RaceState state=m.getPayload();
                if(state.me().stunned()&&!wasStunned.getAndSet(state.me().stunned()))collisions.incrementAndGet();
                else wasStunned.set(state.me().stunned());
            });
            a.on(MessageType.MATCH_RESULT,m->ended.countDown());
            b.on(MessageType.INVITE_RECEIVED,m->{InviteInfo invite=m.getPayload();b.send(new Message(MessageType.INVITE_REPLY,new InviteReply(invite.inviteId(),true)));});
            login(a,"carol"); login(b,"dave");
            a.send(new Message(MessageType.INVITE,"dave"));
            if(!started.await(10,TimeUnit.SECONDS)||!go.await(10,TimeUnit.SECONDS))throw new IllegalStateException("Match did not start");
            if(ui)SwingUtilities.invokeAndWait(()->frame.get().getRootPane().getActionMap().get("W-down").actionPerformed(null));
            for(int tick=0;tick<240;tick++) {
                if(!ui)a.send(new Message(MessageType.CAR_STATE,new CarState(tick*5,1,360)));
                if(ui && tick==100)SwingUtilities.invokeAndWait(()->{
                    try {
                        var content=frame.get().getContentPane();
                        BufferedImage picture=new BufferedImage(content.getWidth(),content.getHeight(),BufferedImage.TYPE_INT_RGB);
                        var graphics=picture.createGraphics();content.printAll(graphics);graphics.dispose();
                        ImageIO.write(picture,"png",new File("plans/261012-0946-game-ui-performance/screenshots/race.png"));
                    } catch(IOException e){throw new RuntimeException(e);}
                });
                b.send(new Message(MessageType.CAR_STATE,new CarState(tick*4,2,300)));
                Thread.sleep(GameConfig.TICK_MS);
            }
            a.send(new Message(MessageType.QUIT_MATCH,match.get().roomId()));
            if(!ended.await(10,TimeUnit.SECONDS))throw new IllegalStateException("No match result");
            if(ui)SwingUtilities.invokeAndWait(()->{for(var window:java.awt.Window.getWindows())window.dispose();});
            Thread.sleep(500);
            Collections.sort(intervals);
            if(intervals.size()<100)throw new IllegalStateException("Too few race updates");
            System.out.printf("updates=%d median=%.2fms p95=%.2fms max=%.2fms collisions=%d%n",intervals.size(),intervals.get(intervals.size()/2),intervals.get((int)(intervals.size()*0.95)),intervals.get(intervals.size()-1),collisions.get());
            try(Connection c=DbConnection.get();PreparedStatement s=c.prepareStatement("SELECT status,end_reason,(SELECT COUNT(*) FROM match_events e WHERE e.match_id=m.match_id) events FROM matches m WHERE match_id=?")) {
                s.setInt(1,match.get().matchId());try(ResultSet r=s.executeQuery()){r.next();System.out.println("DB match="+match.get().matchId()+" status="+r.getString(1)+" reason="+r.getString(2)+" events="+r.getInt(3));if(!"FINISHED".equals(r.getString(1))||r.getInt(3)<3)throw new IllegalStateException("DB verification failed");}
            }
        } finally {
            a.close();b.close();
            if(ui)SwingUtilities.invokeAndWait(()->{for(var window:java.awt.Window.getWindows())window.dispose();});
        }
        System.exit(0);
    }
}
