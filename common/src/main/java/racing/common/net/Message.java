package racing.common.net;

import java.io.Serializable;

/**
 * Gói thông điệp duy nhất đi qua ObjectOutputStream / ObjectInputStream.
 * Payload là DTO trong racing.common.dto hoặc kiểu cơ bản, xem chú thích ở {@link MessageType}.
 */
public final class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    private final MessageType type;
    private final Object payload;
    private final long timestamp;

    public Message(MessageType type, Object payload) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        this.type = type;
        this.payload = payload;
        this.timestamp = System.currentTimeMillis();
    }

    /** Tiện ích cho thông điệp không có dữ liệu kèm theo (PING, PONG, LOGOUT, LEADERBOARD_REQ). */
    public static Message of(MessageType type) {
        return new Message(type, null);
    }

    public MessageType getType() {
        return type;
    }

    /** Ép kiểu payload theo loại thông điệp. Ném ClassCastException nếu gửi sai kiểu. */
    @SuppressWarnings("unchecked")
    public <T> T getPayload() {
        return (T) payload;
    }

    public Object rawPayload() {
        return payload;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "Message{" + type + ", payload=" + payload + "}";
    }
}
