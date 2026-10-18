package racing.client.ui.theme;

import java.awt.Color;

/** Need-for-Speed street palette: night asphalt, amber headlights, red tail lights. */
public final class NfsPalette {
    public static final Color VOID = new Color(0x05070B);
    public static final Color BG = new Color(0x0A0D13);
    public static final Color SURFACE = new Color(0x131820);
    public static final Color SURFACE_HI = new Color(0x1C232E);
    public static final Color INPUT = new Color(0x090C11);
    public static final Color LINE = new Color(0x2B3340);
    public static final Color TEXT = new Color(0xF5F2EA);
    public static final Color MUTED = new Color(0x9EA8B8);
    public static final Color AMBER = new Color(0xFFB000);
    public static final Color AMBER_HOT = new Color(0xFFD04A);
    public static final Color AMBER_DEEP = new Color(0xD98200);
    public static final Color RED = new Color(0xE5202E);
    public static final Color SUCCESS = new Color(0x6FE3A8);
    public static final Color ERROR = new Color(0xFF7B7B);

    private NfsPalette() { }

    /** Same colour with an alpha channel (0..1). */
    public static Color alpha(Color c, float a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.round(Math.max(0f, Math.min(1f, a)) * 255));
    }

    /** Linear blend of two colours, t=0 gives a, t=1 gives b. */
    public static Color mix(Color a, Color b, float t) {
        return new Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }
}
