package racing.server.db;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Cấp Connection JDBC tới MySQL. Cấu hình đọc theo thứ tự ưu tiên:
 * <ol>
 *   <li>biến môi trường RACING_DB_URL / RACING_DB_USER / RACING_DB_PASSWORD (dùng cho CI, docker)</li>
 *   <li>file do system property -Dracing.db.config=/duong/dan/db.properties</li>
 *   <li>file db.properties trong thư mục làm việc</li>
 *   <li>db.properties trên classpath (server/src/main/resources/db.properties)</li>
 * </ol>
 * Mỗi lần gọi {@link #get()} là một kết nối mới; caller đóng bằng try-with-resources.
 * Số kết nối đồng thời của game nhỏ (chỉ lúc đăng nhập và lưu kết quả) nên chưa cần pool.
 */
public final class DbConnection {

    private static volatile Properties config;

    private DbConnection() {
    }

    public static Connection get() throws SQLException {
        Properties p = config();
        return DriverManager.getConnection(p.getProperty("db.url"), p.getProperty("db.user"),
                p.getProperty("db.password"));
    }

    /** Cho phép test hoặc server nạp cấu hình sẵn thay vì đọc file. */
    public static void configure(String url, String user, String password) {
        Properties p = new Properties();
        p.setProperty("db.url", url);
        p.setProperty("db.user", user);
        p.setProperty("db.password", password);
        config = p;
    }

    private static Properties config() {
        Properties p = config;
        if (p != null) {
            return p;
        }
        synchronized (DbConnection.class) {
            if (config == null) {
                config = load();
            }
            return config;
        }
    }

    private static Properties load() {
        String envUrl = System.getenv("RACING_DB_URL");
        if (envUrl != null && !envUrl.isBlank()) {
            Properties p = new Properties();
            p.setProperty("db.url", envUrl);
            p.setProperty("db.user", System.getenv().getOrDefault("RACING_DB_USER", "racing"));
            p.setProperty("db.password", System.getenv().getOrDefault("RACING_DB_PASSWORD", "racing"));
            return p;
        }
        String explicit = System.getProperty("racing.db.config");
        if (explicit != null) {
            return readFile(Path.of(explicit));
        }
        Path local = Path.of("db.properties");
        if (Files.exists(local)) {
            return readFile(local);
        }
        try (InputStream in = DbConnection.class.getResourceAsStream("/db.properties")) {
            if (in == null) {
                throw new IllegalStateException("Không tìm thấy cấu hình DB. Sao chép "
                        + "server/src/main/resources/db.properties.example thành db.properties, "
                        + "hoặc đặt biến môi trường RACING_DB_URL.");
            }
            Properties p = new Properties();
            p.load(in);
            return require(p);
        } catch (IOException e) {
            throw new IllegalStateException("Không đọc được db.properties trên classpath", e);
        }
    }

    private static Properties readFile(Path path) {
        try (FileInputStream in = new FileInputStream(path.toFile())) {
            Properties p = new Properties();
            p.load(in);
            return require(p);
        } catch (IOException e) {
            throw new IllegalStateException("Không đọc được " + path, e);
        }
    }

    private static Properties require(Properties p) {
        for (String key : new String[] {"db.url", "db.user", "db.password"}) {
            if (p.getProperty(key) == null) {
                throw new IllegalStateException("db.properties thiếu khóa " + key);
            }
        }
        return p;
    }
}
