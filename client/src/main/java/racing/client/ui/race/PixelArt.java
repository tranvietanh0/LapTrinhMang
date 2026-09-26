package racing.client.ui.race;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Hình pixel-art vẽ bằng code (không cần file ảnh) theo phong cách game đua xe 8-bit: xe, cây,
 * nhà, ao, vụ nổ. Mỗi hình là một lưới ký tự, mỗi ký tự là một điểm ảnh theo bảng màu; ảnh được
 * dựng một lần rồi phóng to bằng nội suy láng giềng gần nhất để giữ nét răng cưa kiểu 8-bit.
 * Chữ HUD cũng vẽ nhỏ không khử răng cưa rồi phóng to, có bộ nhớ đệm để không cấp phát mỗi khung hình.
 */
final class PixelArt {

    private PixelArt() {
    }

    /** Phóng to mọi hình xe, cây, nhà. */
    static final int SCALE = 3;

    // ------------------------------------------------------------------ lưới hình

    private static final String[] RACE_CAR = {
            "...yBBBBy...",
            "..BBBWWBBB..",
            "kkBBBWWBBBkk",
            "kkBBBWWBBBkk",
            "kkBBBWWBBBkk",
            "..BBggggBB..",
            "..BggggggB..",
            "..BggggggB..",
            "..BBBWWBBB..",
            "..BBBWWBBB..",
            "..BdBWWBdB..",
            "..BdBWWBdB..",
            "..BBBWWBBB..",
            "..BBggggBB..",
            "kkBBBWWBBBkk",
            "kkBBBWWBBBkk",
            "kkBBBWWBBBkk",
            "..BBBWWBBB..",
            ".kkkkkkkkkk.",
            ".rrBBBBBBrr.",
    };

    private static final String[] SEDAN = {
            "...yCCCCy...",
            "..CCCCCCCC..",
            ".kCCCCCCCCk.",
            ".kCCCCCCCCk.",
            "..CCCCCCCC..",
            "..CggggggC..",
            "..CggggggC..",
            "..CCggggCC..",
            "..CRRRRRRC..",
            "..CRRRRRRC..",
            "..CRRRRRRC..",
            "..CRRRRRRC..",
            "..CCggggCC..",
            "..CggggggC..",
            "..CCCCCCCC..",
            ".kCCCCCCCCk.",
            ".kCCCCCCCCk.",
            "..CCCCCCCC..",
            "..CCCCCCCC..",
            "...rCCCCr...",
    };

    private static final String[] VAN = {
            "...yCCCCy...",
            "..CCCCCCCC..",
            ".kCggggggCk.",
            ".kCggggggCk.",
            "..CCCCCCCC..",
            "..CRRRRRRC..",
            "..CRRRRRRC..",
            "..CRRRRRRC..",
            "..CRRRRRRC..",
            "..CRRRRRRC..",
            "..CRRRRRRC..",
            "..CRRRRRRC..",
            "..CRRRRRRC..",
            "..CRRRRRRC..",
            ".kCRRRRRRCk.",
            ".kCRRRRRRCk.",
            "..CRRRRRRC..",
            "..CCCCCCCC..",
            "..CCCkkCCC..",
            "...rCCCCr...",
    };

    private static final String[] TRUCK = {
            "...yCCCCy...",
            "..CCCCCCCC..",
            ".kCggggggCk.",
            ".kCCCCCCCCk.",
            "..CCCCCCCC..",
            "..kkkkkkkk..",
            ".XXXXXXXXXX.",
            ".XxxxxxxxxX.",
            "kXXXXXXXXXXk",
            "kXxxxxxxxxXk",
            ".XXXXXXXXXX.",
            ".XxxxxxxxxX.",
            ".XXXXXXXXXX.",
            ".XxxxxxxxxX.",
            ".XXXXXXXXXX.",
            "kXxxxxxxxxXk",
            "kXXXXXXXXXXk",
            ".XxxxxxxxxX.",
            ".XXXXkkXXXX.",
            ".rXXXXXXXXr.",
    };

    private static final String[] EXPLOSION = {
            "....R..R....",
            ".R..RooR..R.",
            "..RooyyooR..",
            "...oyyyyo...",
            "RRoyyWWyyoRR",
            "..oyyWWyyo..",
            "..oyyWWyyo..",
            "RRoyyyyyyoRR",
            "...ooyyoo...",
            "..R.oRRo.R..",
            ".R...RR...R.",
            "....R..R....",
    };

    private static final String[] PINE = {
            "....GG....",
            "...GGGG...",
            "...GgGG...",
            "..GGGGGG..",
            "..GgGGgG..",
            ".GGGGGGGG.",
            ".GgGGGGgG.",
            "GGGGGGGGGG",
            "GgGGgGGGgG",
            "GGGGGGGGGG",
            "....tt....",
            "....tt....",
    };

    private static final String[] BUSH = {
            "..GGGG..",
            ".GGgGGG.",
            "GGgggGGG",
            "GGggGGGG",
            "GGGGGGgG",
            ".GGGGGG.",
            "..GGGG..",
    };

    private static final String[] HOUSE = {
            "RRRRRRRRRRRR",
            "RrRrRrRrRrRr",
            "RRRRRRRRRRRR",
            "rRrRrRrRrRrR",
            "RRRRRRRRRRRR",
            "kkkkkkkkkkkk",
            "RRRRRRRRRRRR",
            "RrRrRrRrRrRr",
            "RRRRRRRRRRRR",
            "rRrRrRrRrRrR",
            "RRRRRRRRRRRR",
            "wwwwwwwwwwww",
    };

    private static final String[] POND = {
            "....bbbbbb....",
            "..bbBBBBBBbb..",
            ".bBBBBBBBBBBb.",
            "bBBBBwwBBBBBBb",
            "bBBBBBBBBwwBBb",
            ".bBBBBBBBBBBb.",
            "..bbBBBBBBbb..",
            "....bbbbbb....",
    };

    // ------------------------------------------------------------------ ảnh dựng sẵn

    private static final Color TIRE = new Color(0x151515);
    private static final Color GLASS = new Color(0x1C3550);
    private static final Color HEADLIGHT = new Color(0xFFE66B);
    private static final Color TAILLIGHT = new Color(0xFF3B30);

    static final BufferedImage PLAYER = raceCar(new Color(0xD7261E));
    static final BufferedImage OPPONENT = raceCar(new Color(0x1F6FD6));

    /** Xe cộ theo {@code kind} 0..5; kind ngoài khoảng lấy theo modulo. */
    private static final BufferedImage[] TRAFFIC = {
            car(SEDAN, new Color(0x19B5C8), new Color(0x7FE3EE)),     // 0 xanh ngọc
            car(SEDAN, new Color(0xF2C12E), new Color(0xFFE38A)),     // 1 vàng
            car(SEDAN, new Color(0xC8322A), new Color(0xF07A70)),     // 2 đỏ
            car(SEDAN, new Color(0xF4F4F4), new Color(0x2E9E4A)),     // 3 trắng mui xanh
            car(TRUCK, new Color(0xF2C12E), null),                    // 4 xe tải
            car(VAN, new Color(0x3A5FCD), new Color(0x8FA8F0)),       // 5 xe van
    };

    static final BufferedImage EXPLOSION_SMALL = build(EXPLOSION, SCALE, Map.of(
            'R', new Color(0xE0301E), 'o', new Color(0xFF8A1E), 'y', new Color(0xFFE14D), 'W', Color.WHITE));
    static final BufferedImage EXPLOSION_BIG = build(EXPLOSION, 5, Map.of(
            'R', new Color(0xE0301E), 'o', new Color(0xFF8A1E), 'y', new Color(0xFFE14D), 'W', Color.WHITE));

    static final BufferedImage PINE_TREE = build(PINE, SCALE, Map.of(
            'G', new Color(0x1E6B2E), 'g', new Color(0x3D9A4C), 't', new Color(0x6B4423)));
    static final BufferedImage ROUND_BUSH = build(BUSH, SCALE, Map.of(
            'G', new Color(0x2A7F32), 'g', new Color(0x4DB356)));
    static final BufferedImage RED_HOUSE = build(HOUSE, SCALE, Map.of(
            'R', new Color(0xB8322B), 'r', new Color(0x8E2420), 'k', new Color(0x5A5A5A), 'w', new Color(0xDDDDDD)));
    static final BufferedImage SMALL_POND = build(POND, SCALE, Map.of(
            'b', new Color(0x9FD3F5), 'B', new Color(0x2F7FD0), 'w', Color.WHITE));

    static BufferedImage traffic(int kind) {
        return TRAFFIC[Math.floorMod(kind, TRAFFIC.length)];
    }

    private static BufferedImage raceCar(Color body) {
        return build(RACE_CAR, SCALE, Map.of('B', body, 'd', body.darker(), 'W', Color.WHITE, 'g', GLASS,
                'k', TIRE, 'y', HEADLIGHT, 'r', TAILLIGHT));
    }

    private static BufferedImage car(String[] grid, Color body, Color roof) {
        return build(grid, SCALE, Map.of('C', body, 'R', roof == null ? body : roof, 'g', GLASS, 'k', TIRE,
                'y', HEADLIGHT, 'r', TAILLIGHT, 'X', new Color(0xB9BEC6), 'x', new Color(0x9097A1)));
    }

    /** Dựng ảnh từ lưới ký tự: '.' trong suốt, ký tự khác lấy màu từ bảng; thiếu màu là lỗi lập trình. */
    static BufferedImage build(String[] grid, int scale, Map<Character, Color> palette) {
        int w = grid[0].length();
        BufferedImage img = new BufferedImage(w * scale, grid.length * scale, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < grid.length; y++) {
            if (grid[y].length() != w) {
                throw new IllegalArgumentException("Hàng " + y + " dài " + grid[y].length() + ", cần " + w);
            }
            for (int x = 0; x < w; x++) {
                char c = grid[y].charAt(x);
                if (c == '.') {
                    continue;
                }
                Color col = palette.get(c);
                if (col == null) {
                    throw new IllegalArgumentException("Thiếu màu cho ký tự '" + c + "'");
                }
                int argb = col.getRGB();
                for (int dy = 0; dy < scale; dy++) {
                    for (int dx = 0; dx < scale; dx++) {
                        img.setRGB(x * scale + dx, y * scale + dy, argb);
                    }
                }
            }
        }
        return img;
    }

    // ------------------------------------------------------------------ chữ pixel

    private static final Font BASE_FONT = new Font(Font.MONOSPACED, Font.BOLD, 11);
    private static final int TEXT_CACHE = 800;
    private static final Map<String, BufferedImage> TEXT = new LinkedHashMap<>(TEXT_CACHE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, BufferedImage> e) {
            return size() > TEXT_CACHE;
        }
    };

    /** Ảnh chữ cỡ gốc (chưa phóng), vẽ không khử răng cưa; có bộ đệm theo (chuỗi, màu). */
    static BufferedImage text(String s, Color color) {
        String key = color.getRGB() + "|" + s;
        BufferedImage img = TEXT.get(key);
        if (img == null) {
            BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            Graphics2D pg = probe.createGraphics();
            FontMetrics fm = pg.getFontMetrics(BASE_FONT);
            pg.dispose();
            img = new BufferedImage(Math.max(1, fm.stringWidth(s)), fm.getAscent() + fm.getDescent(),
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            g.setFont(BASE_FONT);
            g.setColor(color);
            g.drawString(s, 0, fm.getAscent());
            g.dispose();
            TEXT.put(key, img);
        }
        return img;
    }

    /** Vẽ chữ pixel phóng {@code scale} lần; {@code align} -1 trái, 0 giữa, 1 phải theo x. */
    static void drawText(Graphics2D g, String s, int x, int y, int scale, Color color, int align) {
        BufferedImage img = text(s, color);
        int w = img.getWidth() * scale;
        int dx = align < 0 ? x : (align == 0 ? x - w / 2 : x - w);
        g.drawImage(img, dx, y, w, img.getHeight() * scale, null);
    }

    static int textHeight(int scale) {
        return text("0", Color.WHITE).getHeight() * scale;
    }
}
