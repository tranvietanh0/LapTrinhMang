package racing.server.db;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    @Test
    void verifyAcceptsCorrectPassword() {
        String stored = PasswordHasher.hash("123456");
        assertTrue(stored.startsWith("sha256$"));
        assertTrue(PasswordHasher.verify("123456", stored));
    }

    @Test
    void verifyRejectsWrongPassword() {
        String stored = PasswordHasher.hash("123456");
        assertFalse(PasswordHasher.verify("1234567", stored));
        assertFalse(PasswordHasher.verify("", stored));
        assertFalse(PasswordHasher.verify(null, stored));
    }

    @Test
    void verifyRejectsMalformedStoredValue() {
        assertFalse(PasswordHasher.verify("123456", null));
        assertFalse(PasswordHasher.verify("123456", "plaintext"));
        assertFalse(PasswordHasher.verify("123456", "md5$ab$cd"));
        assertFalse(PasswordHasher.verify("123456", "sha256$zz$zz"));
    }

    @Test
    void saltDiffersBetweenHashesOfSamePassword() {
        assertNotEquals(PasswordHasher.hash("123456"), PasswordHasher.hash("123456"));
    }

    @Test
    void seedHashMatchesPythonGeneratedValue() {
        // Giá trị của tài khoản alice trong db/seed.sql
        String seed = "sha256$f615b44b33fc7839436ddc8cb92cb6f1$"
                + "d6e93e38440e52de8d6eaf53341b35444d9a4e65dc22b2c3a388002743024ab9";
        assertTrue(PasswordHasher.verify("123456", seed));
        assertFalse(PasswordHasher.verify("654321", seed));
    }
}
