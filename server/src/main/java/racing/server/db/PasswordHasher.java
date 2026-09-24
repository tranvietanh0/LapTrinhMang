package racing.server.db;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Băm mật khẩu dạng {@code sha256$<salt hex>$<hash hex>} với hash = SHA-256(salt || password).
 * Salt 16 byte ngẫu nhiên cho mỗi tài khoản. So sánh bằng thời gian không đổi.
 *
 * <p>Chạy {@code java -cp ... racing.server.db.PasswordHasher <matkhau>} để in ra hash cho seed.sql.
 */
public final class PasswordHasher {

    private static final String PREFIX = "sha256";
    private static final int SALT_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final HexFormat HEX = HexFormat.of();

    private PasswordHasher() {
    }

    public static String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return PREFIX + "$" + HEX.formatHex(salt) + "$" + HEX.formatHex(digest(salt, password));
    }

    public static boolean verify(String password, String stored) {
        if (password == null || stored == null) {
            return false;
        }
        String[] parts = stored.split("\\$");
        if (parts.length != 3 || !PREFIX.equals(parts[0])) {
            return false;
        }
        byte[] salt;
        byte[] expected;
        try {
            salt = HEX.parseHex(parts[1]);
            expected = HEX.parseHex(parts[2]);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return MessageDigest.isEqual(expected, digest(salt, password));
    }

    private static byte[] digest(byte[] salt, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            md.update(password.getBytes(StandardCharsets.UTF_8));
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 không khả dụng", e);
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Cách dùng: PasswordHasher <matkhau> [<matkhau> ...]");
            System.exit(1);
        }
        for (String pw : args) {
            System.out.println(hash(pw));
        }
    }
}
