import racing.client.model.CarModel;
import racing.client.ui.race.RacePanel;
import racing.common.dto.CarSnapshot;
import racing.server.core.ObstacleGenerator;
import racing.server.db.DbConnection;
import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.util.Arrays;

class RaceRenderProbe {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CarModel a = new CarModel("alice", 1), b = new CarModel("bob", 2);
            RacePanel panel = new RacePanel(a, b);
            panel.setSize(1200, 800);
            panel.setObstacles(ObstacleGenerator.generate(42));
            BufferedImage image = new BufferedImage(1200, 800, BufferedImage.TYPE_INT_RGB);
            long[] times = new long[300];
            for (int i = 0; i < 400; i++) {
                a.applyServer(new CarSnapshot("alice", i * 4, 1, 300, false, false));
                b.applyServer(new CarSnapshot("bob", i * 4 + 2, 2, 300, false, false));
                panel.setRaceTick(i);
                Graphics2D g = image.createGraphics();
                long start = System.nanoTime();
                panel.paint(g);
                long elapsed = System.nanoTime() - start;
                g.dispose();
                if (i >= 100) times[i - 100] = elapsed;
            }
            Arrays.sort(times);
            System.out.printf("render median=%.3f ms p95=%.3f ms p99=%.3f ms%n", times[150]/1e6, times[285]/1e6, times[297]/1e6);
        });
        long[] connect = new long[12];
        for (int i = 0; i < connect.length; i++) {
            long start = System.nanoTime();
            try (var c = DbConnection.get(); var s = c.createStatement(); var r = s.executeQuery("SELECT 1")) { r.next(); }
            connect[i] = System.nanoTime() - start;
        }
        Arrays.sort(connect);
        System.out.printf("JDBC fresh connection + SELECT 1 median=%.3f ms p95=%.3f ms%n", connect[6]/1e6, connect[11]/1e6);
        System.exit(0);
    }
}
