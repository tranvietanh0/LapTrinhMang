package racing.client.audio;

import racing.client.audio.Synth.Env;
import racing.client.audio.Synth.Wave;

import static racing.client.audio.Synth.midi;
import static racing.client.audio.Synth.tone;

/**
 * Nhạc nền chiptune, dựng bằng {@link Synth} thành một vòng lặp 8 ô nhịp nối liền (đuôi nốt cuối
 * được cộng về đầu vòng). MENU nhẹ cho sảnh / đăng nhập, RACE dồn dập cho lúc đua.
 */
public enum Bgm {
    MENU(100, new int[]{45, 41, 48, 43}, new boolean[]{true, false, false, false}),
    RACE(150, new int[]{40, 36, 43, 38}, new boolean[]{true, false, false, false});

    private static final int STEPS = 16, BARS = 8;
    /** (ô 16 phần, chỉ số nốt trong hợp âm, độ dài theo ô 16 phần) của câu giai điệu nửa sau. */
    private static final int[][] RIFF = {{0, 2, 2}, {2, 1, 2}, {4, 0, 2}, {6, 1, 1}, {7, 2, 1}, {8, 3, 4}, {12, 2, 2}, {14, 1, 2}};
    private static final int[][] MELODY = {{0, 2, 3}, {3, 1, 3}, {6, 0, 2}, {8, 1, 4}, {12, 2, 4}};
    private static final int[] ARP = {0, 1, 2, 3, 2, 1, 0, 1, 2, 3, 2, 1, 0, 2, 1, 3};

    private final int bpm;
    private final int[] roots;      // nốt gốc mỗi ô nhịp (octave 2), lặp lại
    private final boolean[] minor;

    Bgm(int bpm, int[] roots, boolean[] minor) {
        this.bpm = bpm;
        this.roots = roots;
        this.minor = minor;
    }

    double stepSeconds() {
        return 60.0 / bpm / 4;
    }

    double loopSeconds() {
        return stepSeconds() * STEPS * BARS;
    }

    float[] render() {
        double step = stepSeconds();
        float[] b = Synth.buffer(loopSeconds() + 1.0);
        for (int bar = 0; bar < BARS; bar++) {
            int chord = bar % roots.length;
            int root = roots[chord];
            int[] tones = minor[chord] ? new int[]{0, 3, 7, 12} : new int[]{0, 4, 7, 12};
            double t0 = bar * STEPS * step;
            boolean second = bar >= BARS / 2;
            if (this == RACE) {
                race(b, t0, step, root, tones, second, bar);
            } else {
                menu(b, t0, step, root, tones, second, bar);
            }
        }
        return Synth.foldLoop(b, loopSeconds());
    }

    private static void menu(float[] b, double t0, double step, int root, int[] tones, boolean second, int bar) {
        for (int s = 0; s < STEPS; s++) {
            double t = t0 + s * step;
            if (s % 4 == 0) tone(b, t, step * 3.5, midi(root), Wave.TRIANGLE, 0.34, Env.NOTE);
            if (s == 6 || s == 14) tone(b, t, step * 1.8, midi(root + 7), Wave.TRIANGLE, 0.26, Env.NOTE);
            tone(b, t, step * 0.9, midi(root + 36 + tones[ARP[s]]), Wave.PULSE, 0.055, Env.PLUCK);
            if (s == 0 || s == 8 || s == 10) Synth.kick(b, t, 0.42);
            if (s == 4 || s == 12) Synth.snare(b, t, 0.14, bar * 31L + s);
            if (s % 2 == 0) Synth.hat(b, t, s % 4 == 2 ? 0.07 : 0.04, bar * 97L + s);
        }
        if (second) {
            for (int[] n : MELODY) {
                tone(b, t0 + n[0] * step, n[2] * step, midi(root + 24 + tones[n[1]]), Wave.SQUARE, 0.10, Env.NOTE);
            }
        }
    }

    private static void race(float[] b, double t0, double step, int root, int[] tones, boolean second, int bar) {
        for (int s = 0; s < STEPS; s++) {
            double t = t0 + s * step;
            if (s % 2 == 0) {
                int note = root + (s % 4 == 2 ? 12 : 0);
                tone(b, t, step * 1.7, midi(note), Wave.SQUARE, 0.13, Env.PLUCK);
                tone(b, t, step * 1.7, midi(note), Wave.TRIANGLE, 0.26, Env.PLUCK);
            }
            if (!second) tone(b, t, step * 0.8, midi(root + 24 + tones[ARP[s]]), Wave.PULSE, 0.06, Env.PLUCK);
            if (s % 4 == 0) Synth.kick(b, t, 0.55);
            if (s == 4 || s == 12) Synth.snare(b, t, 0.30, bar * 31L + s);
            if (bar % 4 == 3 && s >= 12) Synth.snare(b, t, 0.18, bar * 53L + s);   // dồn trống cuối câu
            Synth.hat(b, t, s % 2 == 1 ? 0.06 : 0.03, bar * 97L + s);
        }
        if (second) {
            for (int[] n : RIFF) {
                tone(b, t0 + n[0] * step, n[2] * step, midi(root + 24 + tones[n[1]]), Wave.SQUARE, 0.12, Env.NOTE);
            }
        }
    }
}
