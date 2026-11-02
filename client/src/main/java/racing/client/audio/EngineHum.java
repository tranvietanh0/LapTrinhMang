package racing.client.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.util.Random;

/**
 * Tiếng động cơ liên tục: một luồng nền sinh mẫu theo tốc độ hiện tại và ghi thẳng ra
 * SourceDataLine (độ trễ ~45 ms). Cao độ và độ sáng tăng theo tốc độ, chuyển mượt để không giật.
 * Tắt thì nhỏ dần rồi đóng line, luồng tự kết thúc.
 */
final class EngineHum {
    private static final int CHUNK = 512;

    private volatile double target;      // tốc độ 0..1
    private volatile boolean on;
    private Thread thread;

    /** Đặt tốc độ (0..1) và bật động cơ nếu đang tắt. Gọi từ luồng âm thanh. */
    void set(double speedFraction, AudioFormat format) throws Exception {
        target = Math.max(0, Math.min(1, speedFraction));
        on = true;
        if (thread == null || !thread.isAlive()) {
            SourceDataLine line = AudioSystem.getSourceDataLine(format);
            line.open(format, CHUNK * 2 * 4);
            line.start();
            thread = new Thread(() -> run(line), "racing-engine");
            thread.setDaemon(true);
            thread.start();
        }
    }

    void off() {
        on = false;
    }

    private void run(SourceDataLine line) {
        byte[] buf = new byte[CHUNK * 2];
        Random rnd = new Random(3);
        double speed = 0, amp = 0, phase = 0, sub = 0, lp = 0, noise = 0;
        try {
            while (on || amp > 0.001) {
                for (int i = 0; i < CHUNK; i++) {
                    speed += (target - speed) * 0.0008;
                    double wantAmp = on ? 0.16 + 0.10 * speed : 0;
                    amp += (wantAmp - amp) * 0.0015;
                    double f = 34 + 135 * speed + 6 * Math.sin(phase * 0.05);   // khẽ rung như máy nổ
                    phase += f / Synth.RATE;
                    phase -= Math.floor(phase);
                    sub += f / 2 / Synth.RATE;
                    sub -= Math.floor(sub);
                    noise += 0.2 * ((rnd.nextDouble() * 2 - 1) - noise);
                    double raw = (2 * phase - 1) * 0.6 + (sub < 0.5 ? 0.45 : -0.45) + noise * 0.25;
                    lp += (0.08 + 0.30 * speed) * (raw - lp);           // lọc thông thấp mở dần khi nhanh
                    int s = (int) Math.round(Math.tanh(lp * amp * 2.2) * 32000);
                    buf[2 * i] = (byte) s;
                    buf[2 * i + 1] = (byte) (s >> 8);
                }
                line.write(buf, 0, buf.length);
            }
            line.drain();
        } finally {
            line.close();
        }
    }
}
