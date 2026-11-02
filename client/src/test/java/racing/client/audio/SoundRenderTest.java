package racing.client.audio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoundRenderTest {

    private static double peak(float[] b) {
        double p = 0;
        for (float v : b) p = Math.max(p, Math.abs(v));
        return p;
    }

    @Test
    void everySfxIsAudibleShortAndNotClippingHard() {
        for (Sfx s : Sfx.values()) {
            float[] b = s.render();
            double seconds = b.length / Synth.RATE;
            assertTrue(seconds > 0.02 && seconds < 2.0, s + " dài " + seconds + " s");
            double p = peak(b);
            assertTrue(p > 0.05, s + " gần như im lặng, đỉnh " + p);
            assertTrue(p < 2.5, s + " quá to, đỉnh " + p);
        }
    }

    @Test
    void sfxRenderIsDeterministic() {
        assertArrayEquals(Sfx.CRASH.render(), Sfx.CRASH.render());
    }

    @Test
    void opponentCrashIsQuieterThanOwnCrash() {
        assertTrue(peak(Sfx.CRASH_FAR.render()) < peak(Sfx.CRASH.render()) * 0.6);
    }

    @Test
    void musicLoopsAreExactlyEightBarsAndAudibleThroughout() {
        for (Bgm m : Bgm.values()) {
            float[] b = m.render();
            assertEquals(Math.round(m.loopSeconds() * Synth.RATE), b.length, m.name());
            int quarter = b.length / 4;
            for (int q = 0; q < 4; q++) {
                float[] part = java.util.Arrays.copyOfRange(b, q * quarter, (q + 1) * quarter);
                assertTrue(peak(part) > 0.1, m + " im lặng ở phần " + q);
            }
        }
        assertTrue(Bgm.RACE.stepSeconds() < Bgm.MENU.stepSeconds(), "nhạc đua phải nhanh hơn nhạc sảnh");
    }

    @Test
    void pcmIsSixteenBitLittleEndianAndSoftClipped() {
        byte[] pcm = Synth.toPcm(new float[]{0f, 10f, -10f}, 1.0);
        assertEquals(6, pcm.length);
        short loud = (short) ((pcm[3] << 8) | (pcm[2] & 0xff));
        short quiet = (short) ((pcm[5] << 8) | (pcm[4] & 0xff));
        assertTrue(loud > 31000 && loud <= 32000, "dương bão hòa mềm: " + loud);
        assertTrue(quiet < -31000 && quiet >= -32000, "âm bão hòa mềm: " + quiet);
        assertEquals(0, pcm[0]);
        assertEquals(0, pcm[1]);
    }

    @Test
    void foldLoopWrapsTailToStart() {
        float[] mix = new float[(int) Synth.RATE + 3];
        mix[mix.length - 1] = 1f;   // mẫu cuối, vượt quá 1 s
        float[] loop = Synth.foldLoop(mix, 1.0);
        assertEquals((int) Synth.RATE, loop.length);
        assertEquals(1f, loop[2]);
    }
}
