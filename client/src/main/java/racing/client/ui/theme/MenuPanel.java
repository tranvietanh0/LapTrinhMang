package racing.client.ui.theme;

import javax.swing.*;
import java.awt.*;

/** Transparent container whose outer margin follows the window scale. */
public class MenuPanel extends JPanel {
    private final int vertical, horizontal;

    public MenuPanel(LayoutManager layout, int vertical, int horizontal) {
        super(layout);
        this.vertical = vertical;
        this.horizontal = horizontal;
        setOpaque(false);
    }

    @Override
    public Insets getInsets() {
        float s = Responsive.scaleOf(this);
        int v = Math.round(vertical * s), h = Math.round(horizontal * s);
        return new Insets(v, h, v, h);
    }
}
