package racing.server;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Log ra console kèm thời gian và tên luồng. Đủ dùng cho bài tập, không cần framework. */
public final class Log {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private Log() {
    }

    public static void info(String msg) {
        System.out.println(LocalTime.now().format(TIME) + " [" + Thread.currentThread().getName() + "] " + msg);
    }

    public static void warn(String msg, Throwable t) {
        System.err.println(LocalTime.now().format(TIME) + " [" + Thread.currentThread().getName() + "] CẢNH BÁO: "
                + msg + (t == null ? "" : " (" + t + ")"));
    }

    public static void warn(String msg) {
        warn(msg, null);
    }
}
