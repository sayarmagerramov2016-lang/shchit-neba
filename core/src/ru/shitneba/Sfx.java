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

public class Sfx implements Disposable {
   static final int RATE = 22050;
   final Map<String, Sound> snd = new HashMap<>();
   final Map<String, Long> last = new HashMap<>();
   final Random rnd = new Random(1L);
   static final Map<String, Float> PITCH = new HashMap<>();
   long loopId = -1L;

   float randf() {
      return this.rnd.nextFloat();
   }

   float[] synth(float dur, Sfx.F1 fn, Sfx.F2 ft) {
      int n = (int)(dur * 22050.0F);
      float[] out = new float[n];
      float lp = 0.0F;
      double ph = 0.0;

      for (int i = 0; i < n; i++) {
         float t = i / 22050.0F;
         float k = t / dur;
         float cut = fn.f(k);
         float a = MathUtils.clamp(cut / 22050.0F * 6.28F, 0.0F, 1.0F);
         lp += (this.randf() * 2.0F - 1.0F - lp) * a;
         float[] tv = ft.f(k);
         ph += tv[0] / 22050.0F * (float) (Math.PI * 2);
         out[i] = (lp * 1.6F + (float)Math.sin(ph) * tv[1]) * (float)Math.pow(1.0F - k, 1.6);
      }

      return out;
   }

   static float[] norm(float[] a, float peak) {
      float m = 1.0E-4F;

      for (float v : a) {
         m = Math.max(m, Math.abs(v));
      }

      for (int i = 0; i < a.length; i++) {
         a[i] = a[i] / m * peak;
      }

      return a;
   }

   float[] gun(float dur, float crackCut, float th0, float th1, float thv) {
      int n = (int)(dur * 22050.0F);
      float[] out = new float[n];
      float lp = 0.0F;
      double ph = 0.0;
      float ac = MathUtils.clamp(crackCut / 22050.0F * 6.28F, 0.0F, 1.0F);

      for (int i = 0; i < n; i++) {
         float t = i / 22050.0F;
         float wn = this.randf() * 2.0F - 1.0F;
         lp += (wn - lp) * ac;
         float crack = (wn - lp) * (float)Math.exp(-t * 70.0);
         ph += MathUtils.lerp(th0, th1, Math.min(1.0F, t / dur * 1.5F)) / 22050.0F * (float) (Math.PI * 2);
         float thump = (float)Math.sin(ph) * (float)Math.exp(-t * 26.0) * thv;
         float body = lp * (float)Math.exp(-t * 22.0) * 1.4F;
         float click = t < 0.002F ? 1.0F - t / 0.002F : 0.0F;
         out[i] = crack * 0.8F + thump + body + click * 0.6F;
      }

      return norm(out, 0.9F);
   }

   float[] boom(float dur, float cut0, float cut1, float sub0, float sub1, float crackle) {
      int n = (int)(dur * 22050.0F);
      float[] out = new float[n];
      float l1 = 0.0F;
      float l2 = 0.0F;
      float cr = 0.0F;
      double ph = 0.0;

      for (int i = 0; i < n; i++) {
         float t = i / 22050.0F;
         float k = t / dur;
         float env = (float)(Math.exp(-t * 2.4) * (1.0 - Math.exp(-t * 500.0)));
         float a = MathUtils.clamp(MathUtils.lerp(cut0, cut1, (float)Math.sqrt(k)) / 22050.0F * 6.28F, 0.0F, 1.0F);
         l1 += (this.randf() * 2.0F - 1.0F - l1) * a;
         l2 += (l1 - l2) * a;
         ph += MathUtils.lerp(sub0, sub1, k) / 22050.0F * (float) (Math.PI * 2);
         cr *= 0.92F;
         if (this.randf() < crackle * (1.0F - k) * 0.006F) {
            cr = this.randf() * 2.0F - 1.0F;
         }

         out[i] = l2 * 3.0F * env + (float)Math.sin(ph) * (float)Math.exp(-t * 3.2) * 0.8F + cr * 0.4F * (1.0F - k);
      }

      return norm(out, 0.95F);
   }

   float[] tink() {
      int n = 1764;
      float[] out = new float[n];

      for (int i = 0; i < n; i++) {
         float t = i / 22050.0F;
         double T = (Math.PI * 2) * t;
         out[i] = (float)(
            (Math.sin(T * 2900.0) * 0.5 + Math.sin(T * 4370.0) * 0.3 + Math.sin(T * 1830.0) * 0.25 + (this.randf() - 0.5) * 0.3 * Math.exp(-t * 200.0))
               * Math.exp(-t * 60.0)
         );
      }

      return norm(out, 0.8F);
   }

   float[] whoosh(float dur) {
      int n = (int)(dur * 22050.0F);
      float[] out = new float[n];
      float l1 = 0.0F;
      float l2 = 0.0F;
      float r = 0.0F;

      for (int i = 0; i < n; i++) {
         float t = i / 22050.0F;
         float k = t / dur;
         float wn = this.randf() * 2.0F - 1.0F;
         float a1 = MathUtils.clamp(MathUtils.lerp(700.0F, 3200.0F, k) / 22050.0F * 6.28F, 0.0F, 1.0F);
         float a2 = MathUtils.clamp(MathUtils.lerp(250.0F, 1200.0F, k) / 22050.0F * 6.28F, 0.0F, 1.0F);
         l1 += (wn - l1) * a1;
         l2 += (wn - l2) * a2;
         r += (wn - r) * 0.02F;
         float env = Math.min(1.0F, t / 0.04F) * (float)Math.pow(1.0F - k, 1.3);
         out[i] = ((l1 - l2) * 1.8F + r * 2.5F * (float)Math.exp(-t * 2.0)) * env + (this.randf() - 0.5F) * (float)Math.exp(-t * 40.0);
      }

      return norm(out, 0.85F);
   }

   float[] laser() {
      int n = 2866;
      float[] out = new float[n];
      float p1 = 0.0F;
      float p2 = 0.0F;

      for (int i = 0; i < n; i++) {
         float t = i / 22050.0F;
         p1 = (p1 + 0.008163265F) % 1.0F;
         p2 = (p2 + 0.016417233F) % 1.0F;
         float saw = (p1 * 2.0F - 1.0F) * 0.5F + (p2 * 2.0F - 1.0F) * 0.35F;
         float env = Math.min(1.0F, t / 0.01F) * Math.min(1.0F, (0.13F - t) / 0.02F);
         out[i] = (saw + (float)Math.sin(9110.619F * t) * 0.18F + (this.randf() - 0.5F) * 0.25F) * env;
      }

      return norm(out, 0.6F);
   }

   float[] buzz() {
      int n = 11025;
      float[] out = new float[n];
      float lp = 0.0F;

      for (int i = 0; i < n; i++) {
         float t = i / 22050.0F;
         float saw = t * 180.0F % 1.0F * 2.0F - 1.0F;
         float saw2 = t * 362.0F % 1.0F * 2.0F - 1.0F;
         lp += (this.randf() * 2.0F - 1.0F - lp) * 0.2F;
         out[i] = saw * 0.35F + saw2 * 0.2F + (float)Math.sin(565.4867F * t) * 0.3F + lp * 0.25F;
      }

      return norm(out, 0.5F);
   }

   public void loop(String name, float vol, float pitch) {
      Sound s = this.snd.get(name);
      if (s != null) {
         float master = G.SV == null ? 0.8F : G.SV.vol;
         if (!(vol <= 0.0F) && !(master <= 0.0F)) {
            float v = Math.min(1.0F, vol * master);
            pitch = MathUtils.clamp(pitch, 0.5F, 2.0F);
            if (this.loopId < 0L) {
               this.loopId = s.loop(v, pitch, 0.0F);
            } else {
               s.setVolume(this.loopId, v);
               s.setPitch(this.loopId, pitch);
            }
         } else {
            if (this.loopId >= 0L) {
               s.stop(this.loopId);
               this.loopId = -1L;
            }
         }
      }
   }

   public Sfx() {
      try {
         this.put("laser", this.laser());
         this.put("flak", this.gun(0.32F, 900.0F, 70.0F, 34.0F, 1.3F));
         this.put("shot", this.gun(0.17F, 1800.0F, 110.0F, 48.0F, 0.9F));
         this.put("shot2", this.gun(0.11F, 2600.0F, 150.0F, 70.0F, 0.5F));
         this.put("boom", this.boom(1.8F, 1400.0F, 70.0F, 62.0F, 30.0F, 1.0F));
         this.put("boom2", this.boom(1.5F, 1900.0F, 90.0F, 75.0F, 34.0F, 1.6F));
         this.put("tink", this.tink());
         this.put("missile", this.whoosh(1.2F));
         this.put("hit", this.synth(0.35F, k -> MathUtils.lerp(1400.0F, 100.0F, k), k -> new float[]{MathUtils.lerp(120.0F, 40.0F, k), 0.4F}));
         this.put("lock", this.synth(0.14F, k -> 10.0F, k -> new float[]{k < 0.5F ? 1800.0F : 2200.0F, 0.35F}));
         this.put("click", this.synth(0.05F, k -> 10.0F, k -> new float[]{MathUtils.lerp(820.0F, 600.0F, k), 0.4F}));
         this.put("coin", this.synth(0.09F, k -> 10.0F, k -> new float[]{MathUtils.lerp(1300.0F, 1900.0F, k), 0.25F}));
         this.put("wave", this.synth(0.5F, k -> 10.0F, k -> new float[]{k < 0.4F ? 440.0F : 660.0F, 0.4F}));
         this.put("boss", this.synth(1.2F, k -> 200.0F, k -> new float[]{MathUtils.lerp(110.0F, 70.0F, k), 0.6F}));
         this.put("alarm", this.synth(0.6F, k -> 10.0F, k -> new float[]{MathUtils.lerp(900.0F, 600.0F, k * 2.0F % 1.0F), 0.35F}));
         this.put("emp", this.synth(1.2F, k -> MathUtils.lerp(3000.0F, 200.0F, k), k -> new float[]{MathUtils.lerp(1200.0F, 60.0F, k), 0.4F}));
         this.put("mg", this.synth(0.05F, k -> 2500.0F, k -> new float[]{0.0F, 0.0F}));
         this.put("radio", this.synth(0.12F, k -> 2200.0F, k -> new float[]{0.0F, 0.0F}));
         float[] cl = new float[]{523.0F, 659.0F, 784.0F, 1046.0F};
         this.put("clear", this.synth(0.8F, k -> 10.0F, k -> new float[]{cl[Math.min(3, (int)(k * 4.0F))], 0.35F}));
         this.put("buzz", this.buzz());
         float[] fl = new float[]{392.0F, 330.0F, 262.0F};
         this.put("fail", this.synth(0.9F, k -> 10.0F, k -> new float[]{fl[Math.min(2, (int)(k * 3.0F))], 0.35F}));
      } catch (Throwable var3) {
         Gdx.app.error("Sfx", "audio disabled", var3);
      }
   }

   void put(String name, float[] s) {
      FileHandle fh = Gdx.files.local("sfx3/" + name + ".wav");
      fh.writeBytes(wav(s), false);
      this.snd.put(name, Gdx.audio.newSound(fh));
   }

   static byte[] wav(float[] s) {
      int dataLen = s.length * 2;
      ByteBuffer bb = ByteBuffer.allocate(44 + dataLen).order(ByteOrder.LITTLE_ENDIAN);
      bb.put("RIFF".getBytes()).putInt(36 + dataLen).put("WAVE".getBytes());
      bb.put("fmt ".getBytes()).putInt(16).putShort((short)1).putShort((short)1).putInt(22050).putInt(44100).putShort((short)2).putShort((short)16);
      bb.put("data".getBytes()).putInt(dataLen);

      for (float v : s) {
         bb.putShort((short)(MathUtils.clamp(v, -1.0F, 1.0F) * 32000.0F));
      }

      return bb.array();
   }

   public void play(String name, float vol, int gapMs) {
      if (name.equals("boom") && this.rnd.nextFloat() < 0.5F) {
         name = "boom2";
      }

      float master = G.SV == null ? 0.8F : G.SV.vol;
      if (!(master <= 0.0F)) {
         Sound s = this.snd.get(name);
         if (s != null) {
            long now = System.currentTimeMillis();
            Long lt = this.last.get(name);
            if (lt == null || now - lt >= gapMs) {
               this.last.put(name, now);
               Float pv = PITCH.get(name);
               float pitch = 1.0F + (pv == null ? 0.0F : (this.rnd.nextFloat() * 2.0F - 1.0F) * pv);
               float v = Math.min(1.0F, Math.max(0.001F, vol) * master);
               if (v > 0.003F) {
                  s.play(v, pitch, 0.0F);
               }
            }
         }
      }
   }

   public void dispose() {
      for (Sound s : this.snd.values()) {
         s.dispose();
      }

      this.snd.clear();
   }

   static {
      PITCH.put("flak", 0.06F);
      PITCH.put("shot", 0.07F);
      PITCH.put("shot2", 0.07F);
      PITCH.put("boom", 0.12F);
      PITCH.put("boom2", 0.12F);
      PITCH.put("tink", 0.15F);
      PITCH.put("missile", 0.06F);
      PITCH.put("hit", 0.08F);
   }

   interface F1 {
      float f(float var1);
   }

   interface F2 {
      float[] f(float var1);
   }
}
