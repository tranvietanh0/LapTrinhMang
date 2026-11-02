package racing.client.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.text.JTextComponent;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.prefs.Preferences;

/**
 * Cửa ngõ âm thanh của client: hiệu ứng ({@link Sfx}), nhạc nền ({@link Bgm}) và tiếng động cơ.
 * Gọi được từ mọi luồng, không bao giờ chặn luồng Swing: mọi việc mở thiết bị và dựng âm chạy
 * trên một luồng nền riêng. Máy không có thiết bị âm thanh (CI, máy chủ) thì báo một lần ra
 * stderr rồi chạy im lặng; game vẫn chơi bình thường.
 */
public final class Audio {
    private static final AudioFormat FORMAT = new AudioFormat(Synth.RATE, 16, 1, true, false);
    private static final double SFX_GAIN = 1.0, MUSIC_GAIN = 0.75;
    private static final Preferences PREFS = Preferences.userNodeForPackage(Audio.class);
    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "racing-audio");
        t.setDaemon(true);
        return t;
    });

    private static volatile boolean muted = PREFS.getBoolean("muted", false);
    private static volatile boolean unavailable;
    // chỉ dùng trên luồng EXEC
    private static final Map<Sfx, Clip> CLIPS = new EnumMap<>(Sfx.class);
    private static final Map<Bgm, byte[]> SCORES = new EnumMap<>(Bgm.class);
    private static final EngineHum ENGINE = new EngineHum();
    private static Clip musicClip;
    private static Bgm playing, wanted;

    private Audio() { }

    public static void play(Sfx sfx) {
        if (muted || unavailable) return;
        run(() -> {
            Clip c = CLIPS.get(sfx);
            if (c == null) {
                c = open(Synth.toPcm(sfx.render(), SFX_GAIN));
                CLIPS.put(sfx, c);
            }
            c.stop();
            c.setFramePosition(0);
            c.start();
        });
    }

    /** Chuyển nhạc nền; null = tắt nhạc. Gọi lại với bản đang phát thì không phát lại từ đầu. */
    public static void music(Bgm bgm) {
        run(() -> {
            wanted = bgm;
            applyMusic();
        });
    }

    /** Động cơ chạy với tốc độ speedFraction (0..1); gọi mỗi khung hình khi đang đua. */
    public static void engine(double speedFraction) {
        if (muted || unavailable) return;
        run(() -> ENGINE.set(speedFraction, FORMAT));
    }

    public static void engineOff() {
        ENGINE.off();
    }

    public static boolean muted() {
        return muted;
    }

    /** Bật / tắt toàn bộ âm thanh, nhớ lựa chọn cho lần chạy sau. */
    public static void toggleMute() {
        muted = !muted;
        PREFS.putBoolean("muted", muted);
        if (muted) ENGINE.off();
        else play(Sfx.CLICK);
        run(Audio::applyMusic);
    }

    /** Phím M ở mọi cửa sổ (trừ khi đang gõ trong ô nhập) để bật / tắt tiếng. */
    public static void installMuteKey() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_M && e.getModifiersEx() == 0
                    && !(e.getComponent() instanceof JTextComponent)) {
                toggleMute();
            }
            return false;
        });
    }

    private static void applyMusic() throws Exception {
        Bgm target = muted ? null : wanted;
        if (target == playing && (target == null || musicClip.isRunning())) return;
        if (musicClip != null) {
            musicClip.close();
            musicClip = null;
        }
        playing = target;
        if (target == null) return;
        byte[] pcm = SCORES.computeIfAbsent(target, b -> Synth.toPcm(b.render(), MUSIC_GAIN));
        musicClip = open(pcm);
        musicClip.loop(Clip.LOOP_CONTINUOUSLY);
    }

    private static Clip open(byte[] pcm) throws Exception {
        Clip c = AudioSystem.getClip();
        c.open(FORMAT, pcm, 0, pcm.length);
        return c;
    }

    private interface Job { void run() throws Exception; }

    private static void run(Job job) {
        if (unavailable) return;
        EXEC.execute(() -> {
            if (unavailable) return;
            try {
                job.run();
            } catch (Exception | LinkageError e) {
                unavailable = true;
                System.err.println("[audio] Không mở được thiết bị âm thanh, game chạy không tiếng: " + e);
            }
        });
    }
}
