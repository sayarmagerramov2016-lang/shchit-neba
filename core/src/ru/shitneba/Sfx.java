package ru.shitneba;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/** Синтез звуков — точный порт формул из g.gd старой версии. */
public class Sfx implements Disposable {
    static final int RATE = 22050;
    final Map<String, Sound> snd = new HashMap<>();
    final Map<String, Long> last = new HashMap<>();
    final Random rnd = new Random(1);
    static final Map<String, Float> PITCH = new HashMap<>();

    static {
        PITCH.put("flak", 0.06f); PITCH.put("shot", 0.07f); PITCH.put("shot2", 0.07f); PITCH.put("boom", 0.12f);
        PITCH.put("boom2", 0.12f); PITCH.put("tink", 0.15f); PITCH.put("missile", 0.06f); PITCH.put("hit", 0.08f);
    }

    interface F1 { float f(float k); }
    interface F2 { float[] f(float k); }

    float randf() { return rnd.nextFloat(); }

    float[] synth(float dur, F1 fn, F2 ft) {
        int n = (int) (dur * RATE);
        float[] out = new float[n];
        float lp = 0; double ph = 0;
        for (int i = 0; i < n; i++) {
            float t = (float) i / RATE, k = t / dur;
            float cut = fn.f(k);
            float a = MathUtils.clamp(cut / RATE * 6.28f, 0, 1);
            lp += (randf() * 2f - 1f - lp) * a;
            float[] tv = ft.f(k);
            ph += tv[0] / RATE * MathUtils.PI2;
            out[i] = (lp * 1.6f + (float) Math.sin(ph) * tv[1]) * (float) Math.pow(1 - k, 1.6);
        }
        return out;
    }

    static float[] norm(float[] a, float peak) {
        float m = 0.0001f;
        for (float v : a) m = Math.max(m, Math.abs(v));
        for (int i = 0; i < a.length; i++) a[i] = a[i] / m * peak;
        return a;
    }

    float[] gun(float dur, float crackCut, float th0, float th1, float thv) {
        int n = (int) (dur * RATE);
        float[] out = new float[n];
        float lp = 0; double ph = 0;
        float ac = MathUtils.clamp(crackCut / RATE * 6.28f, 0, 1);
        for (int i = 0; i < n; i++) {
            float t = (float) i / RATE;
            float wn = randf() * 2f - 1f;
            lp += (wn - lp) * ac;
            float crack = (wn - lp) * (float) Math.exp(-t * 70.0);
            ph += MathUtils.lerp(th0, th1, Math.min(1f, t / dur * 1.5f)) / RATE * MathUtils.PI2;
            float thump = (float) Math.sin(ph) * (float) Math.exp(-t * 26.0) * thv;
            float body = lp * (float) Math.exp(-t * 22.0) * 1.4f;
            float click = t < 0.002f ? (1f - t / 0.002f) : 0f;
            out[i] = crack * 0.8f + thump + body + click * 0.6f;
        }
        return norm(out, 0.9f);
    }

    float[] boom(float dur, float cut0, float cut1, float sub0, float sub1, float crackle) {
        int n = (int) (dur * RATE);
        float[] out = new float[n];
        float l1 = 0, l2 = 0, cr = 0; double ph = 0;
        for (int i = 0; i < n; i++) {
            float t = (float) i / RATE, k = t / dur;
            float env = (float) (Math.exp(-t * 2.4) * (1.0 - Math.exp(-t * 500.0)));
            float a = MathUtils.clamp(MathUtils.lerp(cut0, cut1, (float) Math.sqrt(k)) / RATE * 6.28f, 0, 1);
            l1 += (randf() * 2f - 1f - l1) * a; l2 += (l1 - l2) * a;
            ph += MathUtils.lerp(sub0, sub1, k) / RATE * MathUtils.PI2;
            cr *= 0.92f;
            if (randf() < crackle * (1f - k) * 0.006f) cr = randf() * 2f - 1f;
            out[i] = l2 * 3f * env + (float) Math.sin(ph) * (float) Math.exp(-t * 3.2) * 0.8f + cr * 0.4f * (1f - k);
        }
        return norm(out, 0.95f);
    }

    /** Гром: треск разряда и долгий низкий раскат. */
    float[] thunder() {
        float dur = 3.4f;
        int n = (int) (dur * RATE);
        float[] out = new float[n];
        float l1 = 0, l2 = 0, l3 = 0, cr = 0;
        for (int i = 0; i < n; i++) {
            float t = (float) i / RATE, k = t / dur;
            float wn = randf() * 2f - 1f;
            l1 += (wn - l1) * 0.09f; l2 += (l1 - l2) * 0.035f; l3 += (l2 - l3) * 0.02f;
            float roll = 0.65f + 0.35f * (float) Math.sin(t * 5.3 + Math.sin(t * 2.1) * 2);
            float env = (float) ((1 - Math.exp(-t * 18.0)) * Math.exp(-t * 0.9));
            cr *= 0.9f;
            if (randf() < 0.02f * (float) Math.exp(-t * 6.0)) cr = randf() * 2f - 1f;
            out[i] = (l3 * 14f * roll + l1 * 0.5f * (float) Math.exp(-t * 3.0)) * env + cr * 0.6f * (1 - k);
        }
        return norm(out, 0.9f);
    }

    float[] tink() {
        int n = (int) (0.08f * RATE);
        float[] out = new float[n];
        for (int i = 0; i < n; i++) {
            float t = (float) i / RATE;
            double T = Math.PI * 2 * t;
            out[i] = (float) ((Math.sin(T * 2900) * 0.5 + Math.sin(T * 4370) * 0.3 + Math.sin(T * 1830) * 0.25 + (randf() - 0.5) * 0.3 * Math.exp(-t * 200.0)) * Math.exp(-t * 60.0));
        }
        return norm(out, 0.8f);
    }

    float[] whoosh(float dur) {
        int n = (int) (dur * RATE);
        float[] out = new float[n];
        float l1 = 0, l2 = 0, r = 0;
        for (int i = 0; i < n; i++) {
            float t = (float) i / RATE, k = t / dur;
            float wn = randf() * 2f - 1f;
            float a1 = MathUtils.clamp(MathUtils.lerp(700, 3200, k) / RATE * 6.28f, 0, 1);
            float a2 = MathUtils.clamp(MathUtils.lerp(250, 1200, k) / RATE * 6.28f, 0, 1);
            l1 += (wn - l1) * a1; l2 += (wn - l2) * a2;
            r += (wn - r) * 0.02f;
            float env = Math.min(1f, t / 0.04f) * (float) Math.pow(1 - k, 1.3);
            out[i] = ((l1 - l2) * 1.8f + r * 2.5f * (float) Math.exp(-t * 2.0)) * env + ((randf() - 0.5f) * (float) Math.exp(-t * 40.0));
        }
        return norm(out, 0.85f);
    }

    float[] laser() {
        int n = (int) (0.13f * RATE);
        float[] out = new float[n];
        float p1 = 0, p2 = 0;
        for (int i = 0; i < n; i++) {
            float t = (float) i / RATE;
            p1 = (p1 + 180f / RATE) % 1f; p2 = (p2 + 362f / RATE) % 1f;
            float saw = (p1 * 2 - 1) * 0.5f + (p2 * 2 - 1) * 0.35f;
            float env = Math.min(1f, t / 0.01f) * Math.min(1f, (0.13f - t) / 0.02f);
            out[i] = (saw + (float) Math.sin(MathUtils.PI2 * 1450 * t) * 0.18f + (randf() - 0.5f) * 0.25f) * env;
        }
        return norm(out, 0.6f);
    }

    float[] buzz() {
        int n = (int) (RATE * 0.5f);
        float[] out = new float[n];
        float lp = 0;
        for (int i = 0; i < n; i++) {
            float t = (float) i / RATE;
            float saw = ((t * 180f) % 1f) * 2f - 1f, saw2 = ((t * 362f) % 1f) * 2f - 1f;
            lp += (randf() * 2f - 1f - lp) * 0.2f;
            out[i] = saw * 0.35f + saw2 * 0.2f + (float) Math.sin(MathUtils.PI2 * 90f * t) * 0.3f + lp * 0.25f;
        }
        return norm(out, 0.5f);
    }

    long loopId = -1;

    /** Зацикленный звук (жужжание дрона). vol<=0 — остановить. */
    public void loop(String name, float vol, float pitch) {
        Sound s = snd.get(name);
        if (s == null) return;
        float master = G.SV == null ? 0.8f : G.SV.vol;
        if (vol <= 0 || master <= 0) { if (loopId >= 0) { s.stop(loopId); loopId = -1; } return; }
        float v = Math.min(1f, vol * master);
        pitch = MathUtils.clamp(pitch, 0.5f, 2f);
        if (loopId < 0) loopId = s.loop(v, pitch, 0);
        else { s.setVolume(loopId, v); s.setPitch(loopId, pitch); }
    }

    public Sfx() {
        try {
            put("laser", laser());
            put("flak", gun(0.32f, 900, 70, 34, 1.3f));
            put("shot", gun(0.17f, 1800, 110, 48, 0.9f));
            put("shot2", gun(0.11f, 2600, 150, 70, 0.5f));
            put("boom", boom(1.8f, 1400, 70, 62, 30, 1.0f));
            put("boom2", boom(1.5f, 1900, 90, 75, 34, 1.6f));
            put("thunder", thunder());
            put("tink", tink());
            put("missile", whoosh(1.2f));
            put("hit", synth(0.35f, k -> MathUtils.lerp(1400, 100, k), k -> new float[]{MathUtils.lerp(120, 40, k), 0.4f}));
            put("lock", synth(0.14f, k -> 10f, k -> new float[]{k < 0.5f ? 1800 : 2200, 0.35f}));
            put("click", synth(0.05f, k -> 10f, k -> new float[]{MathUtils.lerp(820, 600, k), 0.4f}));
            put("coin", synth(0.09f, k -> 10f, k -> new float[]{MathUtils.lerp(1300, 1900, k), 0.25f}));
            put("wave", synth(0.5f, k -> 10f, k -> new float[]{k < 0.4f ? 440 : 660, 0.4f}));
            put("boss", synth(1.2f, k -> 200f, k -> new float[]{MathUtils.lerp(110, 70, k), 0.6f}));
            put("alarm", synth(0.6f, k -> 10f, k -> new float[]{MathUtils.lerp(900, 600, (k * 2f) % 1f), 0.35f}));
            put("emp", synth(1.2f, k -> MathUtils.lerp(3000, 200, k), k -> new float[]{MathUtils.lerp(1200, 60, k), 0.4f}));
            put("mg", synth(0.05f, k -> 2500f, k -> new float[]{0, 0}));
            put("radio", synth(0.12f, k -> 2200f, k -> new float[]{0, 0}));
            final float[] cl = {523, 659, 784, 1046};
            put("clear", synth(0.8f, k -> 10f, k -> new float[]{cl[Math.min(3, (int) (k * 4))], 0.35f}));
            put("buzz", buzz());
            // голосок оператора: два коротких тона вверх-вниз (высота задаётся при проигрывании)
            put("chirp", synth(0.16f, k -> 10f, k -> new float[]{k < 0.45f ? MathUtils.lerp(700, 980, k / 0.45f) : MathUtils.lerp(1050, 760, (k - 0.45f) / 0.55f), 0.38f}));
            put("tick", synth(0.03f, k -> 10f, k -> new float[]{1500, 0.3f}));
            final float[] fl = {392, 330, 262};
            put("fail", synth(0.9f, k -> 10f, k -> new float[]{fl[Math.min(2, (int) (k * 3))], 0.35f}));
        } catch (Throwable t) {
            Gdx.app.error("Sfx", "audio disabled", t);
        }
    }

    void put(String name, float[] s) {
        FileHandle fh = Gdx.files.local("sfx3/" + name + ".wav");
        fh.writeBytes(wav(s), false);
        snd.put(name, Gdx.audio.newSound(fh));
    }

    static byte[] wav(float[] s) {
        int dataLen = s.length * 2;
        ByteBuffer bb = ByteBuffer.allocate(44 + dataLen).order(ByteOrder.LITTLE_ENDIAN);
        bb.put("RIFF".getBytes()).putInt(36 + dataLen).put("WAVE".getBytes());
        bb.put("fmt ".getBytes()).putInt(16).putShort((short) 1).putShort((short) 1).putInt(RATE).putInt(RATE * 2).putShort((short) 2).putShort((short) 16);
        bb.put("data".getBytes()).putInt(dataLen);
        for (float v : s) bb.putShort((short) (MathUtils.clamp(v, -1f, 1f) * 32000));
        return bb.array();
    }

    public void play(String name, float vol, int gapMs) {
        if (name.equals("boom") && rnd.nextFloat() < 0.5f) name = "boom2";
        float master = G.SV == null ? 0.8f : G.SV.vol;
        if (master <= 0) return;
        Sound s = snd.get(name);
        if (s == null) return;
        long now = System.currentTimeMillis();
        Long lt = last.get(name);
        if (lt != null && now - lt < gapMs) return;
        last.put(name, now);
        Float pv = PITCH.get(name);
        float pitch = 1f + (pv == null ? 0 : (rnd.nextFloat() * 2 - 1) * pv);
        float v = Math.min(1f, Math.max(0.001f, vol) * master);
        if (v > 0.003f) s.play(v, pitch, 0);
    }

    /** Проиграть с заданной высотой (голоса операторов). */
    public void playP(String name, float vol, float pitch, int gapMs) {
        float master = G.SV == null ? 0.8f : G.SV.vol;
        Sound s = snd.get(name);
        if (s == null || master <= 0) return;
        long now = System.currentTimeMillis();
        Long lt = last.get(name);
        if (lt != null && now - lt < gapMs) return;
        last.put(name, now);
        s.play(Math.min(1f, vol * master), MathUtils.clamp(pitch, 0.5f, 2f), 0);
    }

    @Override
    public void dispose() {
        for (Sound s : snd.values()) s.dispose();
        snd.clear();
    }
}
