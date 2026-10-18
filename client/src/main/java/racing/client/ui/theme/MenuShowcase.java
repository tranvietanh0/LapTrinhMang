package racing.client.ui.theme;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

/**
 * Garage turntable: spot-lit car on an amber platform with a number plate and spec row. The scene
 * is rendered once per size / scale / plate and blitted afterwards; there is no animation.
 */
public final class MenuShowcase extends ChamferPanel {
    private final String kicker, model;
    private final String[][] specs;
    private String plate = "";
    private BufferedImage scene;
    private int sceneW, sceneH;
    private float sceneScale;
    private String scenePlate;

    public MenuShowcase(String kicker, String model, String[][] specs) {
        super(null, 20);
        this.kicker = kicker;
        this.model = model;
        this.specs = specs;
    }

    public void setPlate(String plate) {
        this.plate = plate == null ? "" : plate;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        float s = Responsive.scaleOf(this);
        if (scene == null || sceneW != w || sceneH != h || sceneScale != s || !plate.equals(scenePlate)) {
            scene = render(w, h, s);
            sceneW = w;
            sceneH = h;
            sceneScale = s;
            scenePlate = plate;
        }
        graphics.drawImage(scene, 0, 0, null);
    }

    private BufferedImage render(int w, int h, float s) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        MenuPaint.quality(g);
        g.clip(Chamfer.shape(2, 2, w - 4, h - 4, Math.max(8, 20 * s), 0, Math.max(8, 20 * s), 0));
        int cx = w / 2, floorY = Math.round(h * 0.57f);
        float rx = w * 0.43f, ry = rx * 0.17f;
        Path2D.Float cone = new Path2D.Float();
        cone.moveTo(cx - w * 0.10f, 0);
        cone.lineTo(cx + w * 0.10f, 0);
        cone.lineTo(cx + w * 0.46f, floorY);
        cone.lineTo(cx - w * 0.46f, floorY);
        cone.closePath();
        g.setPaint(new GradientPaint(0, 0, NfsPalette.alpha(Color.WHITE, 0.14f), 0, floorY, NfsPalette.alpha(Color.WHITE, 0f)));
        g.fill(cone);
        g.setPaint(new GradientPaint(0, floorY - ry, NfsPalette.alpha(NfsPalette.AMBER, 0.05f), 0, floorY + ry, NfsPalette.alpha(NfsPalette.AMBER, 0.30f)));
        g.fill(new Ellipse2D.Float(cx - rx, floorY - ry, rx * 2, ry * 2));
        g.setStroke(new BasicStroke(2f * s));
        g.setColor(NfsPalette.alpha(NfsPalette.AMBER, 0.75f));
        g.draw(new Ellipse2D.Float(cx - rx, floorY - ry, rx * 2, ry * 2));
        g.setStroke(new BasicStroke(1.2f * s));
        g.setColor(NfsPalette.alpha(NfsPalette.AMBER, 0.35f));
        g.draw(new Ellipse2D.Float(cx - rx * 0.76f, floorY - ry * 0.76f, rx * 1.52f, ry * 1.52f));
        double cw = Math.min(w * 0.9, h * 0.46 / 0.29);
        CarArt.draw(g, cx - cw / 2, floorY - cw * 0.285, cw);
        g.setFont(NfsFonts.tracked(NfsFonts.strong(13f * s), 0.2f));
        g.setColor(NfsPalette.AMBER);
        g.drawString(NfsFonts.safe(kicker), 24 * s, 34 * s);
        g.setFont(NfsFonts.display(34f * s));
        g.setColor(NfsPalette.TEXT);
        g.drawString(NfsFonts.safe(model), 24 * s, 70 * s);
        int plateW = Math.round(Math.min(w * 0.6f, 200 * s)), plateH = Math.round(40 * s);
        int plateY = Math.round(floorY + ry + 16 * s);
        Shape plateShape = Chamfer.slash(cx - plateW / 2f, plateY, plateW, plateH, 8 * s);
        g.setPaint(new GradientPaint(0, plateY, new Color(0xF5F2EA), 0, plateY + plateH, new Color(0xC9C2AE)));
        g.fill(plateShape);
        g.setColor(NfsPalette.VOID);
        g.setStroke(new BasicStroke(1.5f));
        g.draw(plateShape);
        g.setFont(NfsFonts.tracked(NfsFonts.display(25f * s), 0.06f));
        FontMetrics fm = g.getFontMetrics();
        String text = MenuPaint.ellipsize(fm, NfsFonts.safe(plate.toUpperCase()), plateW - Math.round(24 * s));
        g.drawString(text, cx - fm.stringWidth(text) / 2, plateY + (plateH - fm.getHeight()) / 2 + fm.getAscent());
        int specTop = h - Math.round(80 * s);
        if (specTop > plateY + plateH + 8 * s) {
            g.setColor(NfsPalette.LINE);
            g.fillRect(Math.round(24 * s), specTop, w - Math.round(48 * s), 1);
            for (int i = 0; i < specs.length; i++) {
                int x = Math.round(w * (2 * i + 1) / (2f * specs.length));
                g.setFont(NfsFonts.display(21f * s));
                g.setColor(NfsPalette.TEXT);
                MenuPaint.centered(g, NfsFonts.safe(specs[i][0]), x, specTop + Math.round(32 * s));
                g.setFont(NfsFonts.tracked(NfsFonts.strong(11.5f * s), 0.12f));
                g.setColor(NfsPalette.MUTED);
                MenuPaint.centered(g, NfsFonts.safe(specs[i][1]), x, specTop + Math.round(54 * s));
            }
        }
        g.dispose();
        return img;
    }
}
