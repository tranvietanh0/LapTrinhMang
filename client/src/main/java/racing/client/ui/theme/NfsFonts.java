package racing.client.ui.theme;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.InputStream;
import java.util.Map;
import java.awt.font.TextAttribute;

/** Bundled Barlow fonts (OFL) with graceful fallback to system fonts. Never throws. */
public final class NfsFonts {
    private static final String VIET = "ẠẮỆỒỮ";
    private static final Font BLACK_ITALIC = load("BarlowCondensed-BlackItalic.ttf", "Bahnschrift", Font.BOLD | Font.ITALIC);
    private static final Font XBOLD_ITALIC = load("BarlowCondensed-ExtraBoldItalic.ttf", "Bahnschrift", Font.BOLD | Font.ITALIC);
    private static final Font MEDIUM = load("Barlow-Medium.ttf", "Segoe UI", Font.PLAIN);
    private static final Font SEMIBOLD = load("Barlow-SemiBold.ttf", "Segoe UI", Font.BOLD);

    private NfsFonts() { }

    /** Heaviest italic display face, for the logo and hero. */
    public static Font hero(float size) { return BLACK_ITALIC.deriveFont(size); }
    /** Italic display face for headings and buttons. */
    public static Font display(float size) { return XBOLD_ITALIC.deriveFont(size); }
    /** Regular UI text. */
    public static Font body(float size) { return MEDIUM.deriveFont(size); }
    /** Emphasised UI text. */
    public static Font strong(float size) { return SEMIBOLD.deriveFont(size); }

    /**
     * Swaps glyphs the bundled fonts lack (arrows, bullets: no fallback exists for createFont fonts)
     * for ones they have, so UI text never renders as boxes.
     */
    public static String safe(String text) {
        if (text == null) return null;
        StringBuilder out = null;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            String repl = switch (c) {
                case '→' -> "»";
                case '←' -> "«";
                case '↑' -> "LÊN";
                case '↓' -> "XUỐNG";
                case '●' -> "•";
                default -> null;
            };
            if (repl != null && out == null) out = new StringBuilder(text.length() + 8).append(text, 0, i);
            if (out != null) out.append(repl != null ? repl : String.valueOf(c));
        }
        return out == null ? text : out.toString();
    }

    /** Adds letter spacing (em fraction, e.g. 0.12) to a font. */
    public static Font tracked(Font font, float tracking) {
        return font.deriveFont(Map.of(TextAttribute.TRACKING, tracking));
    }

    private static Font load(String file, String fallbackFamily, int fallbackStyle) {
        try (InputStream in = NfsFonts.class.getResourceAsStream("/fonts/" + file)) {
            if (in != null) {
                Font font = Font.createFont(Font.TRUETYPE_FONT, in);
                if (font.canDisplayUpTo(VIET) < 0) {
                    GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
                    return font;
                }
            }
        } catch (Exception | LinkageError e) {
            // fall through to the system font; a missing font must never stop the game from starting
        }
        return new Font(fallbackFamily, fallbackStyle, 12);
    }
}
