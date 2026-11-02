package racing.client.audio;

import java.util.Random;

/**
 * Bộ tổng hợp âm thanh tối giản kiểu máy chơi game 8-bit: cộng các nốt (sóng vuông, tam giác,
 * răng cưa, sin) và tiếng ồn vào một mảng float, rồi đổi sang PCM 16-bit. Thuần tính toán, không
 * chạm thiết bị âm thanh, nên kiểm thử được trên máy không có loa.
 */
final class Synth {
    static final float RATE = 22050f;

    enum Wave { SQUARE, PULSE, TRIANGLE, SAW, SINE }

    /** Đường bao âm lượng: lên trong attack giây, tắt dần theo hệ số decay (1/s), nhả trong release giây cuối. */
    record Env(double attack, double decay, double release) {
        static final Env PLUCK = new Env(0.004, 6, 0.02);
        static final Env NOTE = new Env(0.005, 1.5, 0.03);
        static final Env HOLD = new Env(0.01, 0, 0.05);
    }

    private Synth() { }

    static float[] buffer(double seconds) {
        return new float[(int) Math.ceil(seconds * RATE)];
    }

    static double midi(double note) {
        return 440.0 * Math.pow(2, (note - 69) / 12.0);
    }

    /** Một nốt từ start, dài dur giây, tần số trượt mũ từ f0 tới f1. */
    static void tone(float[] out, double start, double dur, double f0, double f1, Wave wave, double gain, Env env) {
        int from = (int) (start * RATE), n = (int) (dur * RATE);
        double phase = 0, ratio = f1 / f0;
        for (int i = 0; i < n && from + i < out.length; i++) {
            double t = i / RATE;
            double f = f0 * Math.pow(ratio, (double) i / n);
            phase += f / RATE;
            phase -= Math.floor(phase);
            out[from + i] += (float) (shape(wave, phase) * gain * envelope(env, t, dur));
        }
    }

    static void tone(float[] out, double start, double dur, double freq, Wave wave, double gain, Env env) {
        tone(out, start, dur, freq, freq, wave, gain, env);
    }

    /**
     * Tiếng ồn trắng qua bộ lọc thông thấp một cực; brightness 0..1 (1 = không lọc).
     * Dùng cho trống, tiếng nổ, tiếng gió. Seed cố định để mỗi lần dựng ra cùng một âm.
     */
    static void noise(float[] out, double start, double dur, double gain, Env env, double brightness, long seed) {
        Random rnd = new Random(seed);
        int from = (int) (start * RATE), n = (int) (dur * RATE);
        double y = 0, a = Math.max(0.01, Math.min(1, brightness));
        for (int i = 0; i < n && from + i < out.length; i++) {
            y += a * ((rnd.nextDouble() * 2 - 1) - y);
            out[from + i] += (float) (y * gain * envelope(env, i / RATE, dur));
        }
    }

    /** Trống kick: sin trượt nhanh từ cao xuống trầm. */
    static void kick(float[] out, double start, double gain) {
        tone(out, start, 0.18, 150, 42, Wave.SINE, gain, new Env(0.001, 18, 0.02));
    }

    static void snare(float[] out, double start, double gain, long seed) {
        noise(out, start, 0.16, gain, new Env(0.001, 22, 0.02), 0.55, seed);
        tone(out, start, 0.08, 230, 170, Wave.TRIANGLE, gain * 0.5, new Env(0.001, 30, 0.01));
    }

    static void hat(float[] out, double start, double gain, long seed) {
        noise(out, start, 0.04, gain, new Env(0.001, 70, 0.005), 1, seed);
    }

    static double shape(Wave wave, double phase) {
        return switch (wave) {
            case SQUARE -> phase < 0.5 ? 1 : -1;
            case PULSE -> phase < 0.25 ? 1 : -1;
            case TRIANGLE -> 4 * Math.abs(phase - 0.5) - 1;
            case SAW -> 2 * phase - 1;
            case SINE -> Math.sin(2 * Math.PI * phase);
        };
    }

    static double envelope(Env e, double t, double dur) {
        double a = e.attack() <= 0 ? 1 : Math.min(1, t / e.attack());
        double r = e.release() <= 0 ? 1 : Math.min(1, Math.max(0, (dur - t) / e.release()));
        return a * r * Math.exp(-e.decay() * t);
    }

    /** Cộng phần đuôi vượt quá {@code loopSeconds} về đầu mảng để vòng lặp nối liền, trả về mảng đã cắt. */
    static float[] foldLoop(float[] mix, double loopSeconds) {
        int len = (int) Math.round(loopSeconds * RATE);
        float[] out = new float[len];
        for (int i = 0; i < mix.length; i++) {
            out[i % len] += mix[i];
        }
        return out;
    }

    /** Đổi sang PCM 16-bit little-endian mono, nén mềm bằng tanh để không bị vỡ tiếng khi cộng nhiều lớp. */
    static byte[] toPcm(float[] mix, double gain) {
        byte[] pcm = new byte[mix.length * 2];
        for (int i = 0; i < mix.length; i++) {
            int s = (int) Math.round(Math.tanh(mix[i] * gain) * 32000);
            pcm[2 * i] = (byte) s;
            pcm[2 * i + 1] = (byte) (s >> 8);
        }
        return pcm;
    }
}
