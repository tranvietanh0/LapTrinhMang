package racing.server.core;

import racing.common.net.Message;

/**
 * Kênh gửi thông điệp tới một client. {@code ClientHandler} cài đặt bằng socket;
 * test cài đặt bằng danh sách trong bộ nhớ. Không bao giờ ném exception: lỗi gửi được
 * ghi log và kết nối tự dọn.
 */
public interface PlayerConnection {

    void send(Message message);
}
