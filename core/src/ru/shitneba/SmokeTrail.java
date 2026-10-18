package ru.shitneba;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import ru.shitneba.gfx.Dyn;
import ru.shitneba.gfx.Mat;
import ru.shitneba.gfx.Tex;

public class SmokeTrail {
   public static final Color tint = new Color(0.86F, 0.85F, 0.83F, 1.0F);
   public static float wind = 0.0F;
   public static Dyn dyn;
   final Array<float[]> pts = new Array();
   float life = 3.4F;
   float w0 = 0.7F;
   float grow = 3.6F;
   float alpha = 0.85F;
   float spacing = 1.6F;
   boolean alive = true;
   boolean hasHead;
   float T;
   float totalS;
   final Vector3 head = new Vector3();
   final Vector3 tv = new Vector3();
   final Vector3 dir = new Vector3();
   final Vector3 side = new Vector3();
   final Vector3 toCam = new Vector3();

   public static void init() {
      Mat m = new Mat();
      m.unshaded = true;
      m.vcol = true;
      m.tex = Tex.trail();
      m.blend = 1;
      m.cull = false;
      m.depthWrite = false;
      m.order = 2;
      dyn = new Dyn(m);
   }

   public SmokeTrail(boolean big) {
      if (!big) {
         this.life = 1.8F;
         this.w0 = 0.25F;
         this.grow = 1.6F;
         this.alpha = 0.5F;
         this.spacing = 1.2F;
      }

      FX.trails.add(this);
   }

   public void emit(Vector3 p) {
      this.head.set(p);
      this.hasHead = true;
      if (this.pts.size == 0) {
         this.pts.add(new float[]{p.x, p.y, p.z, this.T, this.totalS});
      } else {
         float[] last = (float[])this.pts.peek();
         float d = p.dst(last[0], last[1], last[2]);
         if (d >= this.spacing) {
            int n = (int)(d / this.spacing);
            float t0 = last[3];

            for (int k = 1; k <= n; k++) {
               float f = (float)k / n;
               this.pts
                  .add(
                     new float[]{
                        MathUtils.lerp(last[0], p.x, f),
                        MathUtils.lerp(last[1], p.y, f),
                        MathUtils.lerp(last[2], p.z, f),
                        MathUtils.lerp(t0, this.T, f),
                        this.totalS + d * f
                     }
                  );
            }

            this.totalS += d;

            while (this.pts.size > 260) {
               this.pts.removeIndex(0);
            }
         }
      }
   }

   public void stop() {
      this.alive = false;
      this.hasHead = false;
   }

   boolean update(float dt) {
      this.T += dt;

      while (this.pts.size > 0 && this.T - ((float[])this.pts.get(0))[3] > this.life) {
         this.pts.removeIndex(0);
      }

      return this.alive || this.pts.size > 0;
   }

   void fill(Camera cam) {
      int n = this.pts.size + (this.hasHead ? 1 : 0);
      if (n >= 2) {
         float[] px = new float[n * 3];
         float[] wd = new float[n];
         float[] al = new float[n];
         float[] vv = new float[n];

         for (int i = 0; i < n; i++) {
            float x;
            float y;
            float z;
            float t;
            float s;
            if (i < this.pts.size) {
               float[] q = (float[])this.pts.get(i);
               x = q[0];
               y = q[1];
               z = q[2];
               t = q[3];
               s = q[4];
            } else {
               x = this.head.x;
               y = this.head.y;
               z = this.head.z;
               t = this.T;
               float[] l = (float[])this.pts.peek();
               s = this.totalS + this.head.dst(l[0], l[1], l[2]);
            }

            float age = this.T - t;
            float k = age / this.life;
            px[i * 3] = x + wind * age * 0.6F;
            px[i * 3 + 1] = y + 0.7F * age;
            px[i * 3 + 2] = z;
            wd[i] = this.w0 + this.grow * (float)Math.sqrt(Math.max(0.0F, age)) + age * 0.6F;
            float a = this.alpha * (float)Math.pow(MathUtils.clamp(1.0F - k, 0.0F, 1.0F), 1.4);
            float fromHead = n - 1 - i;
            a *= MathUtils.clamp(0.25F + fromHead * 0.35F, 0.0F, 1.0F);
            al[i] = a;
            vv[i] = s * 0.035F;
         }

         float r = tint.r;
         float g = tint.g;
         float b = tint.b;
         Vector3 cp = cam.position;
         float prevLx = 0.0F;
         float prevLy = 0.0F;
         float prevLz = 0.0F;
         float prevRx = 0.0F;
         float prevRy = 0.0F;
         float prevRz = 0.0F;
         float prevA = 0.0F;
         float prevV = 0.0F;

         for (int i = 0; i < n; i++) {
            int ip = Math.max(0, i - 1);
            int in = Math.min(n - 1, i + 1);
            this.dir.set(px[in * 3] - px[ip * 3], px[in * 3 + 1] - px[ip * 3 + 1], px[in * 3 + 2] - px[ip * 3 + 2]);
            if (this.dir.len2() < 1.0E-6F) {
               this.dir.set(0.0F, 1.0F, 0.0F);
            }

            this.toCam.set(cp.x - px[i * 3], cp.y - px[i * 3 + 1], cp.z - px[i * 3 + 2]);
            this.side.set(this.dir).crs(this.toCam).nor().scl(wd[i] * 0.5F);
            float lx = px[i * 3] - this.side.x;
            float ly = px[i * 3 + 1] - this.side.y;
            float lz = px[i * 3 + 2] - this.side.z;
            float rx = px[i * 3] + this.side.x;
            float ry = px[i * 3 + 1] + this.side.y;
            float rz = px[i * 3 + 2] + this.side.z;
            if (i > 0) {
               Dyn d = dyn;
               d.vert(prevLx, prevLy, prevLz, 0.0F, 1.0F, 0.0F, r, g, b, prevA, 0.0F, prevV);
               d.vert(prevRx, prevRy, prevRz, 0.0F, 1.0F, 0.0F, r, g, b, prevA, 1.0F, prevV);
               d.vert(rx, ry, rz, 0.0F, 1.0F, 0.0F, r, g, b, al[i], 1.0F, vv[i]);
               d.vert(prevLx, prevLy, prevLz, 0.0F, 1.0F, 0.0F, r, g, b, prevA, 0.0F, prevV);
               d.vert(rx, ry, rz, 0.0F, 1.0F, 0.0F, r, g, b, al[i], 1.0F, vv[i]);
               d.vert(lx, ly, lz, 0.0F, 1.0F, 0.0F, r, g, b, al[i], 0.0F, vv[i]);
            }

            prevLx = lx;
            prevLy = ly;
            prevLz = lz;
            prevRx = rx;
            prevRy = ry;
            prevRz = rz;
            prevA = al[i];
            prevV = vv[i];
         }
      }
   }
}
