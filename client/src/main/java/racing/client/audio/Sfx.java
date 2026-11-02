package racing.client.audio;

import racing.client.audio.Synth.Env;
import racing.client.audio.Synth.Wave;

import static racing.client.audio.Synth.midi;
import static racing.client.audio.Synth.noise;
import static racing.client.audio.Synth.tone;

/** Các hiệu ứng âm thanh ngắn của game. Mỗi hiệu ứng được dựng một lần bằng {@link Synth} khi phát lần đầu. */
public enum Sfx {
    /** Rê chuột lên nút. */
    HOVER,
    /** Bấm nút. */
    CLICK,
    /** Mỗi nhịp đếm ngược 3-2-1. */
    COUNT,
    /** Xuất phát. */
    GO,
    /** Đổi làn. */
    LANE,
    /** Xe mình va chạm. */
    CRASH,
    /** Xe đối thủ va chạm (nhỏ hơn). */
    CRASH_FAR,
    /** Xe mình về đích. */
    FINISH,
    WIN,
    LOSE,
    DRAW,
    /** Có lời mời thách đấu / đối thủ hỏi đấu tiếp. */
    INVITE,
    /** Bị từ chối, hết giờ, lỗi từ server. */
    ERROR;

    float[] render() {
        return switch (this) {
            case HOVER -> one(0.03, b -> tone(b, 0, 0.03, 1900, 2300, Wave.TRIANGLE, 0.10, Env.PLUCK));
            case CLICK -> one(0.09, b -> {
                tone(b, 0, 0.05, 1050, 1500, Wave.SQUARE, 0.16, Env.PLUCK);
                tone(b, 0.035, 0.05, 1600, Wave.SQUARE, 0.10, Env.PLUCK);
            });
            case COUNT -> one(0.28, b -> tone(b, 0, 0.26, midi(69), Wave.SQUARE, 0.30, new Env(0.003, 4, 0.04)));
            case GO -> one(0.75, b -> {
                tone(b, 0, 0.7, midi(81), Wave.SQUARE, 0.30, new Env(0.003, 2.2, 0.08));
                tone(b, 0, 0.7, midi(76), Wave.PULSE, 0.14, new Env(0.003, 2.2, 0.08));
            });
            case LANE -> one(0.16, b -> {
                noise(b, 0, 0.16, 0.30, new Env(0.03, 10, 0.05), 0.35, 11);
                tone(b, 0, 0.12, 260, 420, Wave.TRIANGLE, 0.10, Env.PLUCK);
            });
            case CRASH -> crash(1.0);
            case CRASH_FAR -> crash(0.4);
            case FINISH -> one(0.6, b -> arpeggio(b, new int[]{72, 76, 79, 84}, 0.07, 0.30, Wave.SQUARE, 0.20));
            case WIN -> one(1.6, b -> {
                arpeggio(b, new int[]{67, 72, 76}, 0.12, 0.22, Wave.SQUARE, 0.20);
                tone(b, 0.38, 1.1, midi(79), Wave.SQUARE, 0.22, new Env(0.005, 1.2, 0.2));
                tone(b, 0.38, 1.1, midi(84), Wave.PULSE, 0.12, new Env(0.005, 1.2, 0.2));
                tone(b, 0.38, 1.1, midi(48), Wave.TRIANGLE, 0.30, new Env(0.005, 1.2, 0.2));
            });
            case LOSE -> one(1.5, b -> {
                int[] notes = {67, 66, 65};
                for (int i = 0; i < notes.length; i++) {
                    tone(b, i * 0.26, 0.26, midi(notes[i]), Wave.SQUARE, 0.20, Env.NOTE);
                }
                tone(b, 0.78, 0.7, midi(64), midi(62.5), Wave.SQUARE, 0.20, new Env(0.005, 1.5, 0.2));
                tone(b, 0.78, 0.7, midi(40), Wave.TRIANGLE, 0.30, new Env(0.005, 1.5, 0.2));
            });
            case DRAW -> one(0.9, b -> {
                tone(b, 0, 0.3, midi(72), Wave.SQUARE, 0.18, Env.NOTE);
                tone(b, 0.3, 0.55, midi(74), Wave.SQUARE, 0.18, new Env(0.005, 2, 0.15));
            });
            case INVITE -> one(0.55, b -> {
                for (int i = 0; i < 2; i++) {
                    tone(b, i * 0.2, 0.25, midi(88), Wave.TRIANGLE, 0.35, new Env(0.002, 9, 0.03));
                    tone(b, i * 0.2, 0.25, midi(83), Wave.PULSE, 0.10, new Env(0.002, 9, 0.03));
                }
            });
            case ERROR -> one(0.32, b -> {
                tone(b, 0, 0.13, 220, Wave.SQUARE, 0.16, Env.NOTE);
                tone(b, 0.15, 0.16, 165, Wave.SQUARE, 0.16, Env.NOTE);
            });
        };
    }

    private static float[] crash(double gain) {
        return one(0.9, b -> {
            noise(b, 0, 0.9, 0.9 * gain, new Env(0.001, 4.5, 0.1), 0.45, 7);
            noise(b, 0, 0.12, 0.6 * gain, new Env(0.001, 25, 0.02), 1, 8);
            tone(b, 0, 0.4, 120, 35, Wave.SINE, 0.7 * gain, new Env(0.001, 6, 0.05));
        });
    }

    private static void arpeggio(float[] b, int[] notes, double step, double len, Wave wave, double gain) {
        for (int i = 0; i < notes.length; i++) {
            tone(b, i * step, len, midi(notes[i]), wave, gain, Env.NOTE);
        }
    }

    private static float[] one(double seconds, java.util.function.Consumer<float[]> draw) {
        float[] b = Synth.buffer(seconds);
        draw.accept(b);
        return b;
    }
}
