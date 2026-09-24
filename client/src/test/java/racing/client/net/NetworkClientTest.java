package racing.client.net;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import racing.common.dto.LoginRequest;
import racing.common.dto.PlayerInfo;
import racing.common.dto.PlayerStatus;
import racing.common.net.Message;
import racing.common.net.MessageType;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Server giả trên cổng ngẫu nhiên; không cần màn hình (EDT chạy được ở chế độ headless). */
class NetworkClientTest {

    private ServerSocket serverSocket;
    private Socket accepted;
    private ObjectOutputStream serverOut;
    private ObjectInputStream serverIn;
    private NetworkClient client;

    @BeforeEach
    void setUp() throws Exception {
        serverSocket = new ServerSocket(0);
        client = new NetworkClient();
        Thread acceptor = new Thread(() -> {
            try {
                accepted = serverSocket.accept();
                serverOut = new ObjectOutputStream(accepted.getOutputStream());
                serverOut.flush();
                serverIn = new ObjectInputStream(accepted.getInputStream());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        acceptor.start();
        client.connect("127.0.0.1", serverSocket.getLocalPort());
        acceptor.join(5000);
        assertTrue(client.isConnected());
    }

    @AfterEach
    void tearDown() throws Exception {
        client.close();
        if (accepted != null) {
            accepted.close();
        }
        serverSocket.close();
    }

    @Test
    void sendReachesServer() throws Exception {
        client.send(new Message(MessageType.LOGIN, new LoginRequest("alice", "123456")));
        Message got = (Message) serverIn.readObject();
        assertEquals(MessageType.LOGIN, got.getType());
        LoginRequest req = got.getPayload();
        assertEquals("alice", req.username());
    }

    @Test
    void listenerFiresOnEdtWithPayload() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean onEdt = new AtomicBoolean(false);
        AtomicReference<List<PlayerInfo>> payload = new AtomicReference<>();
        client.on(MessageType.ONLINE_LIST, m -> {
            onEdt.set(SwingUtilities.isEventDispatchThread());
            payload.set(m.getPayload());
            latch.countDown();
        });

        serverOut.writeObject(new Message(MessageType.ONLINE_LIST,
                List.of(new PlayerInfo(1, "alice", 3, 2, PlayerStatus.FREE))));
        serverOut.flush();

        assertTrue(latch.await(5, TimeUnit.SECONDS), "listener không được gọi");
        assertTrue(onEdt.get(), "listener phải chạy trên EDT");
        assertEquals("alice", payload.get().get(0).username());
    }

    @Test
    void unregisterStopsDelivery() throws Exception {
        CountDownLatch first = new CountDownLatch(1);
        AtomicBoolean calledAgain = new AtomicBoolean(false);
        Runnable off = client.on(MessageType.PONG, m -> {
            if (first.getCount() > 0) {
                first.countDown();
            } else {
                calledAgain.set(true);
            }
        });
        serverOut.writeObject(Message.of(MessageType.PONG));
        serverOut.flush();
        assertTrue(first.await(5, TimeUnit.SECONDS));

        off.run();
        CountDownLatch second = new CountDownLatch(1);
        client.onAny(m -> second.countDown());   // để biết thông điệp thứ hai đã được phát
        serverOut.writeObject(Message.of(MessageType.PONG));
        serverOut.flush();
        assertTrue(second.await(5, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> { });     // xả hàng đợi EDT
        assertFalse(calledAgain.get(), "listener đã huỷ vẫn được gọi");
    }

    @Test
    void closingServerSocketTriggersDisconnect() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        client.onDisconnect(latch::countDown);
        accepted.close();
        assertTrue(latch.await(5, TimeUnit.SECONDS), "onDisconnect không được gọi");
        assertFalse(client.isConnected());
    }

    @Test
    void explicitCloseDoesNotTriggerDisconnect() throws Exception {
        AtomicBoolean fired = new AtomicBoolean(false);
        client.onDisconnect(() -> fired.set(true));
        client.close();
        Thread.sleep(200);
        SwingUtilities.invokeAndWait(() -> { });
        assertFalse(fired.get());
        assertFalse(client.isConnected());
    }
}
