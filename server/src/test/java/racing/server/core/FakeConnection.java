package racing.server.core;

import racing.common.net.Message;
import racing.common.net.MessageType;

import java.util.ArrayList;
import java.util.List;

/** Kênh gửi trong bộ nhớ: ghi lại mọi thông điệp server gửi cho client giả. */
final class FakeConnection implements PlayerConnection {

    final List<Message> sent = new ArrayList<>();

    @Override
    public synchronized void send(Message message) {
        sent.add(message);
    }

    synchronized List<Message> of(MessageType type) {
        List<Message> out = new ArrayList<>();
        for (Message m : sent) {
            if (m.getType() == type) {
                out.add(m);
            }
        }
        return out;
    }

    synchronized Message last(MessageType type) {
        List<Message> l = of(type);
        return l.isEmpty() ? null : l.get(l.size() - 1);
    }

    synchronized int count(MessageType type) {
        return of(type).size();
    }

    synchronized void clear() {
        sent.clear();
    }
}
