package ru.shitneba;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Array.ArrayIterator;
import java.util.Random;
import ru.shitneba.gfx.Dyn;
import ru.shitneba.gfx.MB;
import ru.shitneba.gfx.Mat;
import ru.shitneba.gfx.Mesh3;
import ru.shitneba.gfx.Node3;
import ru.shitneba.gfx.Tex;

public final class FX {
   static final Random R = new Random();
   public static Dyn dAdd;
   public static Dyn dSmoke;
   public static Dyn dDebris;
   static final Array<FX.P> parts = new Array(false, 2048);
   static final Array<FX.P> free = new Array(false, 2048);
   public static final Array<FX.Emitter> emitters = new Array();
   public static final Array<SmokeTrail> trails = new Array();
   public static final Array<Node3> flames = new Array();
   static int MAX = 2600;
   static Mat coreMat;
   static Mesh3 coreMesh;
   public static final int K_ADD = 0;
   public static final int K_SMOKE = 1;
   public static final int K_DEBRIS = 2;
   static final Vector3 tdir = new Vector3();
   static final FX.Cfg sphereCfg = new FX.Cfg();
   static final Color SPARK = new Color(1.0F, 0.9F, 0.5F, 1.0F);
   public static final Object ROOT_TAG = new Object();
   static final Vector3 tv = new Vector3();
   static final Vector3 right = new Vector3();
   static final Vector3 up = new Vector3();
   static final Matrix4 tm = new Matrix4();
   static final float[] col = new float[4];

   public static int count() {
      return parts.size;
   }

   public static void init() {
      Mat a = new Mat();
      a.unshaded = true;
      a.vcol = true;
      a.tex = Tex.soft(1.5F);
      a.blend = 2;
      a.depthWrite = false;
      a.cull = false;
      a.order = 5;
      dAdd = new Dyn(a);
      Mat s = new Mat();
      s.unshaded = true;
      s.vcol = true;
      s.tex = Tex.smoke();
      s.blend = 1;
      s.depthWrite = false;
      s.cull = false;
      s.distFadeMin = 1.5F;
      s.distFadeMax = 7.0F;
      s.order = 3;
      dSmoke = new Dyn(s);
      Mat d = Mat.std(new Color(0.12F, 0.12F, 0.12F, 1.0F));
      d.vcol = false;
      d.rough = 0.9F;
      dDebris = new Dyn(d);
      MAX = G.isMobile() ? 1600 : 2600;
      coreMat = new Mat();
      coreMat.unshaded = true;
      coreMat.vcol = false;
      coreMat.tex = Tex.flameCore();
      coreMat.blend = 2;
      coreMat.depthWrite = false;
      coreMat.cull = false;
      coreMat.fog = false;
      coreMat.order = 6;
      MB b = new MB();
      b.cyl(0.13F, 0.0F, 1.6F, 10, Color.WHITE);
      coreMesh = b.mesh();
   }

   static float rr(float a, float b) {
      return a + R.nextFloat() * (b - a);
   }

   static FX.P obtain() {
      return free.size > 0 ? (FX.P)free.pop() : new FX.P();
   }

   static void randDir(FX.Cfg c, Vector3 out) {
      if (c.spread >= 179.0F) {
         float u = rr(-1.0F, 1.0F);
         float th = R.nextFloat() * (float) (Math.PI * 2);
         float s = (float)Math.sqrt(1.0F - u * u);
         out.set(s * MathUtils.cos(th), u, s * MathUtils.sin(th));
      } else {
         float ang = c.spread * (float) (Math.PI / 180.0) * (float)Math.sqrt(R.nextFloat());
         float th = R.nextFloat() * (float) (Math.PI * 2);
         Vector3 d = c.dir;
         Vector3 t1 = Math.abs(d.y) < 0.9F ? new Vector3(0.0F, 1.0F, 0.0F).crs(d).nor() : new Vector3(1.0F, 0.0F, 0.0F).crs(d).nor();
         Vector3 t2 = new Vector3(d).crs(t1);
         float sa = MathUtils.sin(ang);
         float ca = MathUtils.cos(ang);
         out.set(d).scl(ca).mulAdd(t1, sa * MathUtils.cos(th)).mulAdd(t2, sa * MathUtils.sin(th)).nor();
      }
   }

   public static void emit(FX.Cfg c, float x, float y, float z, int amount) {
      if (G.isMobile() && amount > 3) {
         amount = (int)Math.ceil(amount * 0.7F);
      }

      for (int i = 0; i < amount; i++) {
         if (parts.size >= MAX) {
            return;
         }

         FX.P p = obtain();
         p.c = c;
         p.x = x;
         p.y = y;
         p.z = z;
         if (c.sphere > 0.0F) {
            randDir(sphereCfg, tdir);
            float r = c.sphere * (float)Math.cbrt(R.nextFloat());
            p.x = p.x + tdir.x * r;
            p.y = p.y + tdir.y * r;
            p.z = p.z + tdir.z * r;
         }

         randDir(c, tdir);
         float v = rr(c.vMin, c.vMax);
         p.vx = tdir.x * v;
         p.vy = tdir.y * v;
         p.vz = tdir.z * v;
         p.age = 0.0F;
         p.life = c.life;
         p.sc = rr(c.sMin, c.sMax);
         p.damp = rr(c.dMin, c.dMax);
         p.rot = R.nextFloat() * (float) (Math.PI * 2);
         p.rotV = c.angVel > 0.0F ? rr(-c.angVel, c.angVel) * (float) (Math.PI / 180.0) : 0.0F;
         p.ax = rr(-1.0F, 1.0F);
         p.ay = rr(-1.0F, 1.0F);
         p.az = rr(-1.0F, 1.0F);
         parts.add(p);
      }
   }

   static float[] grad(float... v) {
      return v;
   }

   public static void explosion(Vector3 pos, float s, boolean smoke) {
      float n = Math.min(2.2F, s);
      FX.Cfg flash = new FX.Cfg()
         .vel(0.0F, 0.0F)
         .grav(0.0F, 0.0F, 0.0F)
         .scale(16.0F * s, 16.0F * s)
         .curve(0.35F, 1.0F)
         .ramp(0.0F, 1.0F, 0.96F, 0.85F, 1.0F, 1.0F, 1.0F, 0.65F, 0.3F, 0.0F)
         .life(0.18F);
      emit(flash, pos.x, pos.y, pos.z, 1);
      FX.Cfg fire = new FX.Cfg()
         .vel(3.0F * s, 11.0F * s)
         .damp(5.0F, 9.0F)
         .grav(0.0F, 3.0F, 0.0F)
         .scale(3.0F * s, 6.0F * s)
         .curve(0.45F, 1.0F)
         .ramp(0.0F, 1.0F, 0.95F, 0.75F, 1.0F, 0.25F, 1.0F, 0.62F, 0.2F, 0.95F, 0.6F, 0.75F, 0.25F, 0.06F, 0.5F, 1.0F, 0.3F, 0.08F, 0.02F, 0.0F)
         .life(0.6F);
      emit(fire, pos.x, pos.y, pos.z, (int)(12.0F * n) + 5);
      float sq = (float)Math.sqrt(s);
      FX.Cfg spark = new FX.Cfg()
         .vel(30.0F * sq, 70.0F * sq)
         .damp(1.0F, 1.5F)
         .grav(0.0F, -18.0F, 0.0F)
         .scale(0.35F, 0.55F)
         .curve(1.0F, 0.3F)
         .ramp(0.0F, 1.0F, 0.9F, 0.6F, 1.0F, 1.0F, 1.0F, 0.35F, 0.1F, 0.0F)
         .life(0.9F);
      emit(spark, pos.x, pos.y, pos.z, (int)(14.0F * n) + 4);
      if (smoke) {
         float g = rr(0.08F, 0.15F);
         FX.Cfg sm = new FX.Cfg()
            .kind(1)
            .vel(1.5F * s, 5.0F * s)
            .damp(1.5F, 2.2F)
            .grav(0.0F, 2.0F, 0.0F)
            .scale(4.0F * s, 6.5F * s)
            .curve(0.5F, 2.6F)
            .ramp(
               0.0F,
               g * 0.8F,
               g * 0.7F,
               g * 0.6F,
               0.0F,
               0.08F,
               g,
               g * 0.92F,
               g * 0.85F,
               0.9F,
               0.5F,
               g + 0.12F,
               g + 0.11F,
               g + 0.1F,
               0.55F,
               1.0F,
               0.42F,
               0.41F,
               0.4F,
               0.0F
            )
            .life(4.5F);
         emit(sm, pos.x, pos.y, pos.z, (int)(8.0F * n) + 4);
      }

      if (s >= 1.0F) {
         FX.Cfg db = new FX.Cfg().kind(2).vel(8.0F, 22.0F).grav(0.0F, -20.0F, 0.0F).life(2.0F).dir(Vector3.Y, 70.0F);
         db.angVel = 400.0F;
         emit(db, pos.x, pos.y, pos.z, (int)(5.0F * n) + 2);
      }
   }

   public static void sparks(Vector3 pos, int n, Color col) {
      FX.Cfg c = new FX.Cfg()
         .vel(10.0F, 30.0F)
         .damp(2.0F, 2.0F)
         .grav(0.0F, -10.0F, 0.0F)
         .scale(0.5F, 0.6F)
         .curve(1.0F, 0.3F)
         .ramp(0.0F, col.r, col.g, col.b, 1.0F, 1.0F, col.r, col.g * 0.5F, col.b * 0.3F, 0.0F)
         .life(0.4F);
      emit(c, pos.x, pos.y, pos.z, n);
   }

   public static void sparks(Vector3 pos, int n) {
      sparks(pos, n, SPARK);
   }

   public static void puff(Vector3 pos, float size, Color col, float alpha, float life, float vy) {
      FX.Cfg c = new FX.Cfg()
         .kind(1)
         .vel(0.5F, 1.2F)
         .grav(0.0F, vy, 0.0F)
         .damp(0.5F, 0.5F)
         .scale(1.5F * size, 2.0F * size)
         .curve(0.6F, 2.6F)
         .ramp(0.0F, col.r, col.g, col.b, alpha, 1.0F, col.r + 0.15F, col.g + 0.15F, col.b + 0.15F, 0.0F)
         .life(life);
      emit(c, pos.x, pos.y, pos.z, 2);
   }

   public static void glow(Vector3 pos, float size, Color col, float life) {
      FX.Cfg c = new FX.Cfg()
         .vel(0.0F, 0.0F)
         .grav(0.0F, 0.0F, 0.0F)
         .scale(size, size)
         .curve(1.0F, 1.0F)
         .ramp(0.0F, col.r, col.g, col.b, 1.0F, 1.0F, col.r, col.g, col.b, 0.0F)
         .life(life);
      emit(c, pos.x, pos.y, pos.z, 1);
   }

   public static void launchBlast(Vector3 muzzle, Vector3 fwd) {
      Vector3 rear = new Vector3(muzzle).mulAdd(fwd, -3.4F);
      glow(muzzle, 7.0F, new Color(1.0F, 0.9F, 0.7F, 1.0F), 0.14F);
      glow(rear, 9.0F, new Color(1.0F, 0.75F, 0.45F, 1.0F), 0.18F);
      Vector3 back = new Vector3(fwd).scl(-1.0F);
      FX.Cfg jet = new FX.Cfg()
         .dir(back, 12.0F)
         .vel(25.0F, 45.0F)
         .damp(30.0F, 40.0F)
         .grav(0.0F, 0.0F, 0.0F)
         .scale(1.6F, 2.6F)
         .curve(0.6F, 1.4F)
         .ramp(0.0F, 1.0F, 0.92F, 0.7F, 1.0F, 1.0F, 1.0F, 0.5F, 0.2F, 0.0F)
         .life(0.25F);
      emit(jet, rear.x, rear.y, rear.z, 10);
      Color t = SmokeTrail.tint;
      FX.Cfg b1 = new FX.Cfg()
         .kind(1)
         .dir(back, 25.0F)
         .vel(2.0F, 7.0F)
         .damp(2.0F, 3.0F)
         .grav(0.0F, 0.9F, 0.0F)
         .scale(3.2F, 4.6F)
         .curve(0.45F, 2.6F)
         .ramp(
            0.0F,
            t.r,
            t.g,
            t.b,
            0.0F,
            0.05F,
            t.r,
            t.g,
            t.b,
            0.5F,
            0.5F,
            t.r * 0.95F,
            t.g * 0.95F,
            t.b * 0.95F,
            0.25F,
            1.0F,
            t.r * 0.9F,
            t.g * 0.9F,
            t.b * 0.9F,
            0.0F
         )
         .life(4.0F);
      emit(b1, rear.x, rear.y, rear.z, 9);
      FX.Cfg b2 = new FX.Cfg()
         .kind(1)
         .dir(fwd, 30.0F)
         .vel(3.0F, 10.0F)
         .damp(3.0F, 5.0F)
         .grav(0.0F, 0.8F, 0.0F)
         .scale(1.6F, 2.6F)
         .curve(0.5F, 3.0F)
         .ramp(0.0F, t.r, t.g, t.b, 0.0F, 0.05F, t.r, t.g, t.b, 0.5F, 1.0F, t.r * 0.9F, t.g * 0.9F, t.b * 0.9F, 0.0F)
         .life(3.0F);
      emit(b2, muzzle.x, muzzle.y, muzzle.z, 6);
   }

   static boolean attached(Node3 n) {
      for (Node3 p = n; p != null; p = p.parent) {
         if (p.tag == ROOT_TAG) {
            return true;
         }
      }

      return false;
   }

   public static FX.Emitter burner(Node3 anchor, boolean fire, float size) {
      FX.Cfg c = new FX.Cfg()
         .kind(fire ? 0 : 1)
         .dir(Vector3.Y, 15.0F)
         .vel(fire ? 2.0F : 1.5F, fire ? 5.0F : 3.0F)
         .grav(0.0F, fire ? -9.8F : 0.8F, 0.0F)
         .scale(1.5F * size, (fire ? 2.4F : 2.2F) * size)
         .life(fire ? 0.7F : 2.0F);
      if (fire) {
         c.curve(1.0F, 0.3F).ramp(0.0F, 1.0F, 0.65F, 0.2F, 1.0F, 1.0F, 0.9F, 0.2F, 0.05F, 0.0F);
      } else {
         c.curve(0.7F, 3.0F).ramp(0.0F, 0.12F, 0.12F, 0.12F, 0.6F, 1.0F, 0.3F, 0.3F, 0.3F, 0.0F);
      }

      c.sphere = 0.6F * size;
      FX.Emitter e = new FX.Emitter();
      e.anchor = anchor;
      e.cfg = c;
      e.rate = (fire ? 24 : 16) / c.life;
      emitters.add(e);
      return e;
   }

   public static FX.Emitter chimney(Vector3 pos) {
      FX.Cfg c = new FX.Cfg()
         .kind(1)
         .dir(new Vector3(0.3F, 1.0F, 0.0F).nor(), 10.0F)
         .vel(5.0F, 7.0F)
         .grav(0.0F, 0.3F, 0.0F)
         .scale(12.0F, 14.0F)
         .curve(1.0F, 3.5F)
         .ramp(0.0F, 0.18F, 0.15F, 0.14F, 0.45F, 1.0F, 0.4F, 0.35F, 0.33F, 0.0F)
         .life(9.0F);
      FX.Emitter e = new FX.Emitter();
      e.fixed.set(pos);
      e.cfg = c;
      e.rate = 4.4444447F;
      emitters.add(e);

      for (int i = 0; i < 40; i++) {
         emit(c, pos.x, pos.y, pos.z, 1);
         FX.P p = (FX.P)parts.peek();
         float a = R.nextFloat() * 9.0F;
         p.age = a;
         p.x = p.x + p.vx * a;
         p.y = p.y + p.vy * a;
         p.z = p.z + p.vz * a;
      }

      return e;
   }

   public static FX.Emitter trail(Node3 anchor, boolean big) {
      FX.Cfg s = new FX.Cfg()
         .kind(1)
         .vel(0.2F, 0.8F)
         .grav(0.0F, 0.6F, 0.0F)
         .scale(big ? 1.0F : 0.6F, big ? 1.3F : 0.8F)
         .curve(0.8F, big ? 5.0F : 3.5F)
         .ramp(0.0F, 0.92F, 0.9F, 0.88F, 0.0F, 0.08F, 0.92F, 0.9F, 0.88F, 0.5F, 1.0F, 0.6F, 0.6F, 0.62F, 0.0F)
         .life(big ? 2.6F : 1.2F);
      FX.Emitter e = new FX.Emitter();
      e.anchor = anchor;
      e.cfg = s;
      e.rate = (big ? 70 : 30) / s.life;
      e.off.set(0.0F, 0.0F, 1.0F);
      FX.Glow g = new FX.Glow();
      g.size = big ? 2.8F : 2.0F;
      g.r = 1.0F;
      g.g = 0.75F;
      g.b = 0.35F;
      e.glowCfg = g;
      emitters.add(e);
      return e;
   }

   public static void detach(FX.Emitter e) {
      if (e != null) {
         if (e.anchor != null) {
            e.anchor.toGlobal(e.off, e.fixed);
         }

         e.anchor = null;
         e.detached = true;
         e.emitting = false;
         e.glowCfg = null;
         e.killAt = e.t + 3.0F;
      }
   }

   public static void kill(FX.Emitter e) {
      if (e != null) {
         e.dead = true;
      }
   }

   public static Node3 flame(float tailZ) {
      Node3 n = new Node3();
      n.pos.set(0.0F, 0.0F, tailZ);
      Node3 core = new Node3(coreMesh, coreMat);
      core.setEuler((float) (-Math.PI / 2), 0.0F, 0.0F);
      core.pos.set(0.0F, 0.0F, 0.8F);
      n.add(core);
      n.tag = "flame";
      flames.add(n);
      return n;
   }

   public static void update(float dt, Camera cam) {
      for (int i = emitters.size - 1; i >= 0; i--) {
         FX.Emitter e = (FX.Emitter)emitters.get(i);
         e.t += dt;
         if (!e.dead && (!(e.killAt > 0.0F) || !(e.t > e.killAt))) {
            if (e.anchor != null && !attached(e.anchor)) {
               emitters.removeIndex(i);
            } else if (e.emitting) {
               if (e.anchor != null) {
                  e.anchor.toGlobal(e.off, tv);
               } else {
                  tv.set(e.fixed);
               }

               e.acc = e.acc + dt * e.rate;
               int n = (int)e.acc;
               e.acc -= n;

               for (int k = 0; k < n; k++) {
                  emit1(e.cfg, tv);
               }
            }
         } else {
            emitters.removeIndex(i);
         }
      }

      for (int ix = parts.size - 1; ix >= 0; ix--) {
         FX.P p = (FX.P)parts.get(ix);
         p.age += dt;
         if (p.age >= p.life) {
            parts.removeIndex(ix);
            free.add(p);
         } else {
            FX.Cfg c = p.c;
            if (p.damp > 0.0F) {
               float sp = (float)Math.sqrt(p.vx * p.vx + p.vy * p.vy + p.vz * p.vz);
               if (sp > 0.0F) {
                  float ns = Math.max(0.0F, sp - p.damp * dt);
                  float k = ns / sp;
                  p.vx *= k;
                  p.vy *= k;
                  p.vz *= k;
               }
            }

            p.vx = p.vx + c.grav.x * dt;
            p.vy = p.vy + c.grav.y * dt;
            p.vz = p.vz + c.grav.z * dt;
            p.x = p.x + p.vx * dt;
            p.y = p.y + p.vy * dt;
            p.z = p.z + p.vz * dt;
            p.rot = p.rot + p.rotV * dt;
         }
      }

      for (int ixx = trails.size - 1; ixx >= 0; ixx--) {
         if (!((SmokeTrail)trails.get(ixx)).update(dt)) {
            trails.removeIndex(ixx);
         }
      }
   }

   static void emit1(FX.Cfg c, Vector3 at) {
      if (!G.isMobile() || !(R.nextFloat() < 0.3F)) {
         boolean m = G.isMobile();
         if (parts.size < MAX) {
            FX.P p = obtain();
            p.c = c;
            p.x = at.x;
            p.y = at.y;
            p.z = at.z;
            if (c.sphere > 0.0F) {
               randDir(sphereCfg, tdir);
               float r = c.sphere * (float)Math.cbrt(R.nextFloat());
               p.x = p.x + tdir.x * r;
               p.y = p.y + tdir.y * r;
               p.z = p.z + tdir.z * r;
            }

            randDir(c, tdir);
            float v = rr(c.vMin, c.vMax);
            p.vx = tdir.x * v;
            p.vy = tdir.y * v;
            p.vz = tdir.z * v;
            p.age = 0.0F;
            p.life = c.life;
            p.sc = rr(c.sMin, c.sMax);
            p.damp = rr(c.dMin, c.dMax);
            p.rot = R.nextFloat() * (float) (Math.PI * 2);
            p.rotV = 0.0F;
            parts.add(p);
         }
      }
   }

   static void rampAt(float[] r, float t, float[] out) {
      if (r == null) {
         out[0] = out[1] = out[2] = out[3] = 1.0F;
      } else {
         int n = r.length / 5;
         if (t <= r[0]) {
            System.arraycopy(r, 1, out, 0, 4);
         } else {
            for (int i = 0; i < n - 1; i++) {
               float o0 = r[i * 5];
               float o1 = r[(i + 1) * 5];
               if (t <= o1) {
                  float k = (t - o0) / Math.max(1.0E-6F, o1 - o0);

                  for (int j = 0; j < 4; j++) {
                     out[j] = MathUtils.lerp(r[i * 5 + 1 + j], r[(i + 1) * 5 + 1 + j], k);
                  }

                  return;
               }
            }

            System.arraycopy(r, (n - 1) * 5 + 1, out, 0, 4);
         }
      }
   }

   public static void fill(Camera cam) {
      dAdd.clear();
      dSmoke.clear();
      dDebris.clear();
      right.set(cam.direction).crs(cam.up).nor();
      up.set(right).crs(cam.direction).nor();

      for (int i = 0; i < parts.size; i++) {
         FX.P p = (FX.P)parts.get(i);
         FX.Cfg c = p.c;
         float t = p.age / p.life;
         if (c.kind == 2) {
            tm.idt().translate(p.x, p.y, p.z).rotateRad(tv.set(p.ax, p.ay, p.az).nor(), p.rot).scale(0.35F, 0.2F, 0.3F);
            dDebris.prim(Dyn.BOX, tm, 1.0F, 1.0F, 1.0F, 1.0F);
         } else {
            rampAt(c.ramp, t, col);
            float sz = p.sc * MathUtils.lerp(c.cA, c.cB, t);
            tv.set(p.x, p.y, p.z);
            (c.kind == 1 ? dSmoke : dAdd).billboard(tv, sz, right, up, col[0], col[1], col[2], col[3], c.kind == 1 ? p.rot : 0.0F);
         }
      }

      for (int ix = 0; ix < emitters.size; ix++) {
         FX.Emitter e = (FX.Emitter)emitters.get(ix);
         if (e.glowCfg != null && e.anchor != null && e.emitting) {
            e.anchor.toGlobal(e.off, tv);
            dAdd.billboard(tv, e.glowCfg.size, right, up, e.glowCfg.r, e.glowCfg.g, e.glowCfg.b, 0.8F, 0.0F);
         }
      }

      for (int ixx = flames.size - 1; ixx >= 0; ixx--) {
         Node3 f = (Node3)flames.get(ixx);
         if (!attached(f)) {
            flames.removeIndex(ixx);
         } else {
            f.toGlobal(tv.set(0.0F, 0.0F, 0.3F), tv);
            float k = f.scl.x;
            dAdd.billboard(tv, 2.6F * k, right, up, 1.0F, 0.61200005F, 0.357F, 0.85F, 0.0F);
         }
      }

      SmokeTrail.dyn.clear();

      for (int ixxx = 0; ixxx < trails.size; ixxx++) {
         ((SmokeTrail)trails.get(ixxx)).fill(cam);
      }
   }

   public static void clear() {
      ArrayIterator var0 = parts.iterator();

      while (var0.hasNext()) {
         FX.P p = (FX.P)var0.next();
         free.add(p);
      }

      parts.clear();
      emitters.clear();
      trails.clear();
      flames.clear();
   }

   private FX() {
   }

   public static final class Cfg {
      public int kind = 0;
      public float vMin;
      public float vMax;
      public float dMin;
      public float dMax;
      public float sMin = 1.0F;
      public float sMax = 1.0F;
      public float cA = 1.0F;
      public float cB = 1.0F;
      public float life = 1.0F;
      public final Vector3 grav = new Vector3();
      public final Vector3 dir = new Vector3(0.0F, 1.0F, 0.0F);
      public float spread = 180.0F;
      public float[] ramp;
      public float sphere = 0.0F;
      public float angVel = 0.0F;

      public FX.Cfg vel(float a, float b) {
         this.vMin = a;
         this.vMax = b;
         return this;
      }

      public FX.Cfg damp(float a, float b) {
         this.dMin = a;
         this.dMax = b;
         return this;
      }

      public FX.Cfg grav(float x, float y, float z) {
         this.grav.set(x, y, z);
         return this;
      }

      public FX.Cfg scale(float a, float b) {
         this.sMin = a;
         this.sMax = b;
         return this;
      }

      public FX.Cfg curve(float a, float b) {
         this.cA = a;
         this.cB = b;
         return this;
      }

      public FX.Cfg ramp(float... r) {
         this.ramp = r;
         return this;
      }

      public FX.Cfg life(float l) {
         this.life = l;
         return this;
      }

      public FX.Cfg kind(int k) {
         this.kind = k;
         return this;
      }

      public FX.Cfg dir(Vector3 d, float spr) {
         this.dir.set(d);
         this.spread = spr;
         return this;
      }
   }

   public static final class Emitter {
      public Node3 anchor;
      public final Vector3 off = new Vector3();
      public final Vector3 fixed = new Vector3();
      public FX.Cfg cfg;
      public float rate;
      public float acc;
      public boolean emitting = true;
      public boolean dead = false;
      public boolean detached = false;
      public float killAt = -1.0F;
      public float t;
      public FX.Glow glowCfg;
   }

   static final class Glow {
      float size;
      float r;
      float g;
      float b;
   }

   static final class P {
      float x;
      float y;
      float z;
      float vx;
      float vy;
      float vz;
      float age;
      float life;
      float sc;
      float damp;
      float rot;
      float rotV;
      float ax;
      float ay;
      float az;
      FX.Cfg c;
   }
}
