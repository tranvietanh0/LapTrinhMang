package racing.server.net;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import racing.common.net.Message;
import racing.common.net.MessageType;

import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientHandlerTest {

    /**
     * Client ngừng đọc (treo, rút mạng): bộ đệm TCP đầy. send() được gọi từ luồng tick của phòng
     * nên không được chặn; hàng đợi đầy thì server ngắt client đó thay vì làm cả phòng đứng hình.
     */
    @Test
    @Timeout(20)
    void sendNeverBlocksOnClientThatStopsReading() throws Exception {
        try (ServerSocket ss = new ServerSocket(0);
             Socket client = new Socket("127.0.0.1", ss.getLocalPort());
             Socket accepted = ss.accept()) {
            new ObjectOutputStream(client.getOutputStream()).flush();   // header cho ObjectInputStream của handler
            ClientHandler handler = new ClientHandler(accepted, null);
            Thread reader = new Thread(handler, "reader");
            reader.setDaemon(true);
            reader.start();

            String big = "x".repeat(64 * 1024);
            long start = System.nanoTime();
            for (int i = 0; i < 2000; i++) {                           // ~128 MB, vượt mọi bộ đệm TCP
                handler.send(new Message(MessageType.ERROR, big));
            }
            long ms = (System.nanoTime() - start) / 1_000_000;

            assertTrue(ms < 2000, "send() bị chặn " + ms + " ms khi client không đọc");
            reader.join(5000);
            assertTrue(accepted.isClosed(), "hàng đợi gửi đầy phải ngắt client");
        }
    }
}
