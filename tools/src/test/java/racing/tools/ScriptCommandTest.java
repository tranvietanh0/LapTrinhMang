package racing.tools;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScriptCommandTest {

    @Test
    void parsesCommandAndArgs() {
        ScriptCommand c = ScriptCommand.parse("  LOGIN alice 123456 ");
        assertEquals("login", c.name());
        assertEquals(List.of("alice", "123456"), c.args());
        assertEquals(1, ScriptCommand.parse("drive 120").intArg(1, 1));
        assertEquals(999.0, ScriptCommand.parse("car 10 1 999").doubleArg(2));
    }

    @Test
    void skipsBlankAndComments() {
        assertNull(ScriptCommand.parse(""));
        assertNull(ScriptCommand.parse("   "));
        assertNull(ScriptCommand.parse("# ghi chú"));
        assertNull(ScriptCommand.parse(null));
    }

    @Test
    void rejectsUnknownCommandAndMissingArgs() {
        assertThrows(IllegalArgumentException.class, () -> ScriptCommand.parse("fly"));
        assertThrows(IllegalArgumentException.class, () -> ScriptCommand.parse("login alice"));
        assertThrows(IllegalArgumentException.class, () -> ScriptCommand.parse("car 1 2"));
    }
}
