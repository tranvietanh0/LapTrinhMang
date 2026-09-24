package racing.tools;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Một dòng lệnh của ScriptedClient đã được phân tích. Cú pháp mỗi dòng: {@code <lệnh> [tham số...]},
 * dòng trống hoặc bắt đầu bằng {@code #} bị bỏ qua.
 *
 * <pre>
 * login &lt;user&gt; &lt;pass&gt;        gửi LOGIN
 * logout                     gửi LOGOUT
 * invite &lt;user&gt;              thách đấu
 * accept | reject            trả lời lời mời gần nhất
 * car &lt;distance&gt; &lt;lane&gt; &lt;speed&gt;   gửi một CAR_STATE
 * drive &lt;speed&gt; [lane] [ticks]     gửi CAR_STATE liên tục mỗi 50 ms, quãng đường tự tăng theo tốc độ;
 *                                  bỏ ticks thì chạy đến khi về đích (distance &gt;= 1000) hoặc trận kết thúc
 * finish                     gửi FINISH
 * quit                       gửi QUIT_MATCH cho phòng hiện tại
 * rematch yes|no             trả lời REMATCH_ASK
 * leaderboard                gửi LEADERBOARD_REQ
 * ping                       gửi PING
 * sleep &lt;ms&gt;                 chờ
 * wait &lt;MESSAGE_TYPE&gt; [ms]   chờ đến khi nhận loại thông điệp đó (mặc định 10 000 ms), quá hạn thì báo lỗi
 * expect &lt;MESSAGE_TYPE&gt; [ms] như wait nhưng thoát mã 2 nếu quá hạn (dùng cho kịch bản tự động)
 * exit                       đóng kết nối và thoát
 * </pre>
 */
public record ScriptCommand(String name, List<String> args) {

    public static ScriptCommand parse(String line) {
        if (line == null) {
            return null;
        }
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return null;
        }
        String[] parts = trimmed.split("\\s+");
        String name = parts[0].toLowerCase(Locale.ROOT);
        List<String> args = Arrays.asList(parts).subList(1, parts.length);
        int min = minArgs(name);
        if (min < 0) {
            throw new IllegalArgumentException("Lệnh không hợp lệ: " + parts[0]);
        }
        if (args.size() < min) {
            throw new IllegalArgumentException("Lệnh " + name + " cần ít nhất " + min + " tham số");
        }
        return new ScriptCommand(name, args);
    }

    /** Số tham số tối thiểu, hoặc -1 nếu lệnh không tồn tại. */
    static int minArgs(String name) {
        return switch (name) {
            case "login" -> 2;
            case "invite", "rematch", "sleep", "wait", "expect" -> 1;
            case "car" -> 3;
            case "drive" -> 1;
            case "logout", "accept", "reject", "finish", "quit", "leaderboard", "ping", "exit" -> 0;
            default -> -1;
        };
    }

    public String arg(int i) {
        return args.get(i);
    }

    public int intArg(int i, int defaultValue) {
        return i < args.size() ? Integer.parseInt(args.get(i)) : defaultValue;
    }

    public double doubleArg(int i) {
        return Double.parseDouble(args.get(i));
    }
}
