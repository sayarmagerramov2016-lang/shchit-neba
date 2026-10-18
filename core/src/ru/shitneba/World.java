package ru.shitneba;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.Array.ArrayIterator;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import ru.shitneba.gfx.Dyn;
import ru.shitneba.gfx.MB;
import ru.shitneba.gfx.Mat;
import ru.shitneba.gfx.Mesh3;
import ru.shitneba.gfx.Node3;
import ru.shitneba.gfx.Renderer;
import ru.shitneba.gfx.Tex;

public class World {
   public static final float ROAD_X = -42.0F;
   static final Map<String, float[]> ATM = new HashMap<>();
   static final Map<String, String[]> GROUND = new HashMap<>();
   public final Node3 root = new Node3();
   Node3 content;
   final Array<Mesh3> meshes = new Array();
   final Renderer rend;
   public String themeName = "dusk";
   public boolean night;
   public boolean sea;
   public boolean dam;
   public boolean snow;
   int hf;
   public float rain;
   public float fog;
   public float wind;
   public boolean beamOn;
   float tAcc;
   float lightning;
   float flashT;
   final Random R = new Random();
   public Node3 beam;
   public Node3 stars;
   public Node3 moon;
   final Array<float[]> clouds = new Array();
   public final Array<Vector3> stacks = new Array();
   final Color cloudTint = new Color();
   public final Color smokeTint = new Color(0.84F, 0.83F, 0.81F, 1.0F);
   public final Dyn cloudDyn;
   public final Dyn rainDyn;
   Mat starMat;
   float[] precip;
   final Vector3 tv = new Vector3();
   final Vector3 rightV = new Vector3();
   final Vector3 upV = new Vector3();

   public World(Renderer r) {
      this.rend = r;
      Mat cm = new Mat();
      cm.unshaded = true;
      cm.vcol = true;
      cm.tex = Tex.cloud();
      cm.blend = 1;
      cm.depthWrite = false;
      cm.cull = false;
      cm.fog = false;
      cm.order = -10;
      this.cloudDyn = new Dyn(cm);
      Mat rm = new Mat();
      rm.unshaded = true;
      rm.vcol = true;
      rm.blend = 1;
      rm.depthWrite = false;
      rm.cull = false;
      this.rainDyn = new Dyn(rm);
   }

   float rr(float a, float b) {
      return a + this.R.nextFloat() * (b - a);
   }

   static Color c(String h) {
      return G.hex(h);
   }

   public float hNoise(float x, float z) {
      return (float)(
         Math.sin(x * 0.004) * Math.cos(z * 0.0035) * 22.0
            + Math.sin(x * 0.011 + 1.3) * Math.sin(z * 0.009 + 0.7) * 9.0
            + Math.sin(x * 0.031 + z * 0.027) * 2.5
            + Math.sin(x * 0.07 - z * 0.05) * 0.8
      );
   }

   public float height(float x, float z) {
      float d = (float)Math.sqrt(x * x + z * z);
      float f = MathUtils.clamp((d - 55.0F) / 320.0F, 0.0F, 1.0F);
      float h;
      if (this.hf == 1) {
         h = (float)((Math.sin(x * 0.012 + z * 0.004) * 6.0 + Math.sin(x * 0.005 - z * 0.009) * 9.0) * f);
      } else {
         h = this.hNoise(x, z) * f * f;
      }

      float rd = Math.abs(x - -42.0F);
      if (rd < 30.0F) {
         h *= 0.25F + 0.75F * MathUtils.clamp((rd - 8.0F) / 22.0F, 0.0F, 1.0F);
      }

      if (this.sea && x < -120.0F) {
         h = h * MathUtils.clamp(1.0F - (-120.0F - x) / 60.0F, 0.0F, 1.0F) - MathUtils.clamp((-120.0F - x) / 40.0F, 0.0F, 1.0F) * 14.0F;
      }

      if (this.dam && z < -150.0F && z > -520.0F && Math.abs(x) < 150.0F) {
         h = Math.min(h, -3.0F + Math.max(0.0F, Math.abs(x) - 110.0F) * 0.6F);
      }

      if (d > 1000.0F) {
         h += (d - 1000.0F) * 0.09F;
      }

      return h;
   }

   Node3 add(Mesh3 m, Mat mat) {
      this.meshes.add(m);
      Node3 n = new Node3(m, mat);
      this.content.add(n);
      return n;
   }

   static Color lerp(Color a, Color b, float t) {
      return a.cpy().lerp(b, t);
   }

   public void build(int theme, boolean dark, float wRain, float wFog, float wWind, String obj, boolean noBeam) {
      if (this.content != null) {
         this.content.remove();
         ArrayIterator T = this.meshes.iterator();

         while (T.hasNext()) {
            Mesh3 m = (Mesh3)T.next();
            m.dispose();
         }

         this.meshes.clear();
      }

      this.content = new Node3();
      this.root.add(this.content);
      JsonValue T = G.D.get("THEMES").get(Math.min(theme, G.D.get("THEMES").size - 1));
      this.themeName = T.getString("name");
      String[] Gr = GROUND.containsKey(this.themeName) ? GROUND.get(this.themeName) : GROUND.get("dusk");
      this.night = dark || this.themeName.equals("night");
      this.hf = T.getBoolean("dunes", false) ? 1 : 0;
      this.sea = T.getBoolean("sea", false);
      this.dam = "dam".equals(obj);
      this.rain = wRain;
      this.fog = wFog;
      this.wind = wWind;
      this.snow = T.getBoolean("snow", false);
      this.stacks.clear();
      this.clouds.clear();
      int gfx = G.SV.gfx;
      float[] atm = ATM.containsKey(this.themeName) ? ATM.get(this.themeName) : ATM.get("dusk");
      JsonValue sunInfo = T.get("sun");
      float sx = sunInfo != null ? sunInfo.getFloat("x", 0.7F) : 0.7F;
      String sunCol = sunInfo != null ? sunInfo.getString("col", "255,220,190") : "255,220,190";
      float az = (sx - 0.5F) * 1.4F;
      float el = this.night ? 0.5F : atm[3] * (float) Math.PI / 180.0F;
      Renderer.Env e = this.rend.env;
      e.sunDir.set(-MathUtils.sin(az) * MathUtils.cos(el), MathUtils.sin(el), -MathUtils.cos(az) * MathUtils.cos(el)).nor();
      JsonValue sk = T.get("sky");
      float lift = this.night ? 0.0F : 0.18F;

      for (int i = 0; i < 4; i++) {
         e.sky[i].set(lerp(c(sk.getString(i)), c("bcd0ea"), lift * (0.4F + i * 0.2F)));
      }

      e.skySun.set(G.rgbList(sunCol));
      e.sunOn = this.night ? 0.0F : (this.rain > 0.0F ? 0.3F : 1.0F);
      if (this.night) {
         e.amb.set(c("33476e"));
      } else {
         e.amb.set(lerp(lerp(c(sk.getString(2)), c(sk.getString(3)), 0.5F), c("aab8cc"), 0.5F));
      }

      e.ambEnergy = this.night ? 0.6F : 0.95F;
      e.exposure = this.night ? 1.15F : (this.themeName.equals("snow") ? 0.85F : 1.0F);
      Color fogc;
      if (this.night) {
         fogc = c("0e1626");
      } else {
         fogc = lerp(G.rgbList(T.getString("fog")), c("8a96a8"), 0.45F);
         fogc.mul(0.82F, 0.82F, 0.82F, 1.0F);
      }

      if (this.rain > 0.0F) {
         fogc = lerp(fogc, c("5a6270"), 0.6F);
      }

      e.fogCol.set(fogc);
      e.fogD = (this.night ? 7.0E-4F : 4.2E-4F) * (1.0F + this.fog * 3.0F) * (this.rain > 0.0F ? 2.2F : 1.0F);
      e.sunCol.set(this.night ? c("8fa6d8") : lerp(G.rgbList(sunCol), Color.WHITE, 0.35F));
      e.sunEnergy = this.night ? 0.45F : (this.rain > 0.0F ? 0.65F : (this.themeName.equals("snow") ? 1.25F : 1.45F));
      this.rend.shadows = gfx >= 1;
      if (this.night) {
         this.smokeTint.set(0.4F, 0.42F, 0.48F, 1.0F);
      } else {
         this.smokeTint.set(0.84F, 0.83F, 0.81F, 1.0F).lerp(e.sunCol, 0.12F);
         if (this.rain > 0.0F) {
            this.smokeTint.mul(0.75F, 0.75F, 0.75F, 1.0F);
         }
      }

      SmokeTrail.tint.set(this.smokeTint);
      SmokeTrail.wind = this.wind * 0.05F;
      this.buildTerrain(Gr, gfx);
      MB rb = new MB();
      Color rcol = this.night ? c("3a3428") : (this.themeName.equals("snow") ? c("a9adb2") : c("6e5e48"));

      for (float z = 700.0F; z > -1100.0F; z -= 12.0F) {
         float y = Math.max(this.height(-46.0F, z), Math.max(this.height(-38.0F, z), this.height(-42.0F, z))) + 0.12F;
         rb.box(8.0F, 0.1F, 12.4F, rcol, -42.0F, y, z - 6.0F);
      }

      Mat roadMat = Mat.vc();
      roadMat.shadowRecv = true;
      this.add(rb.mesh(), roadMat);
      this.buildPost();
      if (!Gr[4].isEmpty()) {
         this.buildTrees(Gr, gfx);
      }

      if (!Gr[5].isEmpty() && gfx >= 1) {
         this.buildGrass(Gr, gfx);
      }

      this.buildRocks(Gr);
      this.buildMountains(T, Gr, gfx);
      this.buildExtras(T);
      this.buildClouds(T);
      this.stars = null;
      this.moon = null;
      if (this.night) {
         this.buildStars();
      }

      this.beamOn = this.night && !noBeam;
      Mat bm = new Mat();
      bm.unshaded = true;
      bm.vcol = true;
      bm.blend = 2;
      bm.depthWrite = false;
      bm.cull = false;
      bm.fog = false;
      MB bb = new MB();
      int segs = 20;

      for (int i = 0; i < segs; i++) {
         float a0 = (float)i / segs * (float) (Math.PI * 2);
         float a1 = (float)(i + 1) / segs * (float) (Math.PI * 2);
         Color c0 = new Color(0.18F, 0.17F, 0.15F, 1.0F);
         Color c1 = new Color(0.0F, 0.0F, 0.0F, 1.0F);
         float r0 = 0.4F;
         float r1 = 60.0F;
         float[] p = new float[]{
            MathUtils.cos(a0) * r0,
            MathUtils.sin(a0) * r0,
            0.0F,
            MathUtils.cos(a1) * r0,
            MathUtils.sin(a1) * r0,
            0.0F,
            MathUtils.cos(a1) * r1,
            MathUtils.sin(a1) * r1,
            -600.0F,
            MathUtils.cos(a0) * r1,
            MathUtils.sin(a0) * r1,
            -600.0F
         };
         float[][] cols = new float[][]{{c0.r, c0.g, c0.b}, {c0.r, c0.g, c0.b}, {c1.r, c1.g, c1.b}, {c1.r, c1.g, c1.b}};
         int[] idx = new int[]{0, 1, 2, 0, 2, 3};

         for (int k : idx) {
            bb.v.addAll(new float[]{p[k * 3], p[k * 3 + 1], p[k * 3 + 2], 0.0F, 1.0F, 0.0F, cols[k][0], cols[k][1], cols[k][2], 1.0F, 0.0F, 0.0F});
         }
      }

      this.beam = this.add(bb.mesh(), bm);
      this.beam.visible = this.beamOn;
      this.precip = null;
   }

   void buildTerrain(String[] Gr, int gfx) {
      int AS = gfx >= 2 ? 160 : (gfx >= 1 ? 128 : 96);
      int RS = gfx >= 2 ? 64 : (gfx >= 1 ? 52 : 40);
      float RMAX = 2500.0F;
      Color cA = c(Gr[0]);
      Color cB = c(Gr[1]);
      Color cR = c(Gr[2]);
      Color cRoad = this.themeName.equals("snow") ? c("b7b9bb") : c("7a6a52");
      int nv = 1 + RS * AS;
      float[] px = new float[nv * 3];
      float[] col = new float[nv * 3];
      float[] uv = new float[nv * 2];
      Color cc = new Color();
      int k = 0;

      for (int j = 0; j <= RS; j++) {
         int cnt = j == 0 ? 1 : AS;

         for (int i = 0; i < cnt; i++) {
            float x;
            float z;
            if (j == 0) {
               x = 0.0F;
               z = 0.0F;
            } else {
               float r = RMAX * (float)Math.pow((float)j / RS, 1.9);
               float a = (float)i / AS * (float) (Math.PI * 2);
               x = MathUtils.sin(a) * r;
               z = MathUtils.cos(a) * r;
            }

            float y = this.height(x, z);
            px[k * 3] = x;
            px[k * 3 + 1] = y;
            px[k * 3 + 2] = z;
            uv[k * 2] = x / 9.0F;
            uv[k * 2 + 1] = z / 9.0F;
            float n = (float)(
               (Math.sin(x * 0.021) * Math.sin(z * 0.017) + Math.sin(x * 0.06 + z * 0.05) * 0.4 + Math.sin(x * 0.13 - z * 0.11) * 0.25 + 1.65) / 3.3
            );
            cc.set(cA).lerp(cB, MathUtils.clamp(n, 0.0F, 1.0F));
            if (y > 20.0F) {
               cc.lerp(cR, MathUtils.clamp((y - 20.0F) / 30.0F, 0.0F, 0.8F));
            }

            if (this.sea && x < -110.0F) {
               cc.lerp(c("c9b48a"), MathUtils.clamp((-110.0F - x) / 30.0F, 0.0F, 1.0F));
            }

            if (Math.abs(x - -42.0F) < 9.0F) {
               cc.lerp(cRoad, 0.6F);
            }

            float m = this.night ? 0.65F : 1.18F;
            col[k * 3] = cc.r * m;
            col[k * 3 + 1] = cc.g * m;
            col[k * 3 + 2] = cc.b * m;
            k++;
         }
      }

      IntArray idx = new IntArray();

      for (int i = 0; i < AS; i++) {
         idx.addAll(new int[]{0, 1 + (i + 1) % AS, 1 + i});
      }

      for (int j = 1; j < RS; j++) {
         for (int i = 0; i < AS; i++) {
            int A = 1 + (j - 1) * AS + i;
            int B = 1 + (j - 1) * AS + (i + 1) % AS;
            int C = A + AS;
            int D = B + AS;
            idx.addAll(new int[]{A, B, C, B, D, C});
         }
      }

      this.add(this.indexedMesh(px, col, uv, idx, true), this.terrainMat());
   }

   Mat terrainMat() {
      Mat m = Mat.vc();
      m.tex = Tex.ground(this.themeName);
      m.rough = 1.0F;
      m.shadowRecv = true;
      m.spec = 0.2F;
      return m;
   }

   Mesh3 indexedMesh(float[] p, float[] col, float[] uv, IntArray idx, boolean faceUp) {
      int nv = p.length / 3;
      float[] nrm = new float[nv * 3];

      for (int t = 0; t < idx.size; t += 3) {
         int a = idx.get(t);
         int b = idx.get(t + 1);
         int c2 = idx.get(t + 2);
         float ex = p[b * 3] - p[a * 3];
         float ey = p[b * 3 + 1] - p[a * 3 + 1];
         float ez = p[b * 3 + 2] - p[a * 3 + 2];
         float fx = p[c2 * 3] - p[a * 3];
         float fy = p[c2 * 3 + 1] - p[a * 3 + 1];
         float fz = p[c2 * 3 + 2] - p[a * 3 + 2];
         float nx = ey * fz - ez * fy;
         float ny = ez * fx - ex * fz;
         float nz = ex * fy - ey * fx;
         if (faceUp && ny < 0.0F) {
            nx = -nx;
            ny = -ny;
            nz = -nz;
         }

         for (int v : new int[]{a, b, c2}) {
            nrm[v * 3] = nrm[v * 3] + nx;
            nrm[v * 3 + 1] = nrm[v * 3 + 1] + ny;
            nrm[v * 3 + 2] = nrm[v * 3 + 2] + nz;
         }
      }

      FloatArray out = new FloatArray(idx.size * 12);

      for (int t = 0; t < idx.size; t += 3) {
         int a = idx.get(t);
         int b = idx.get(t + 1);
         int c2 = idx.get(t + 2);
         float ex = p[b * 3] - p[a * 3];
         float ez = p[b * 3 + 2] - p[a * 3 + 2];
         float fx = p[c2 * 3] - p[a * 3];
         float fz = p[c2 * 3 + 2] - p[a * 3 + 2];
         float cy = ez * fx - ex * fz;
         int[] ord = cy > 0.0F ? new int[]{a, c2, b} : new int[]{a, b, c2};
         if (!faceUp) {
            ord = new int[]{a, b, c2};
         }

         for (int v : ord) {
            float nx = nrm[v * 3];
            float ny = nrm[v * 3 + 1];
            float nz = nrm[v * 3 + 2];
            float l = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (l > 0.0F) {
               nx /= l;
               ny /= l;
               nz /= l;
            }

            out.addAll(
               new float[]{p[v * 3], p[v * 3 + 1], p[v * 3 + 2], nx, ny, nz, col[v * 3], col[v * 3 + 1], col[v * 3 + 2], 1.0F, uv[v * 2], uv[v * 2 + 1]}
            );
         }
      }

      return new Mesh3(out.items, out.size);
   }

   void buildPost() {
      MB sb = new MB();
      sb.add(MB.pBox(16.0F, 0.25F, 20.0F), Color.WHITE, new Matrix4().setToTranslation(0.0F, 0.12F, 0.5F), 1.0F);
      Mat slab = Mat.std(this.night ? c("3a3a38") : c("77746e"));
      slab.tex = Tex.slab();
      slab.rough = 0.95F;
      slab.shadowRecv = true;
      slab.vcol = false;
      this.add(sb.mesh(), slab);
      MB b = new MB();
      Color sand = this.themeName.equals("snow") ? c("c8ccd0") : c("9c8a5f");

      for (int i = 0; i < 30; i++) {
         float a = (float) (-Math.PI * 9.0 / 10.0) + i / 29.0F * (float) Math.PI * 1.8F;
         float r = 11.5F;

         for (int k = 0; k < (i % 2 == 1 ? 2 : 3); k++) {
            float d = this.rr(-0.05F, 0.05F);
            Color sc = new Color(
               MathUtils.clamp(sand.r + d, 0.0F, 1.0F), MathUtils.clamp(sand.g + d, 0.0F, 1.0F), MathUtils.clamp(sand.b + d, 0.0F, 1.0F), 1.0F
            );
            b.capsule(0.42F, 1.2F, sc, MathUtils.sin(a) * r, 0.42F + k * 0.62F, MathUtils.cos(a) * r + 1.0F, 0.0F, a, (float) (Math.PI / 2));
         }
      }

      for (float[] p : new float[][]{{5.5F, 7.0F}, {6.7F, 6.1F}, {6.1F, 6.6F}}) {
         b.box(1.3F, 0.95F, 1.1F, c("4f5a3a"), p[0], 0.6F, p[1]);
      }

      b.box(1.2F, 0.9F, 1.0F, c("4f5a3a"), 6.1F, 1.5F, 6.6F);
      b.cyl(0.0F, 3.4F, 2.8F, 4, c("56603f"), -6.5F, 1.5F, 8.8F, 0.0F, (float) (Math.PI / 4), 0.0F);
      b.cyl(0.12F, 0.18F, 10.0F, 6, c("555555"), -13.0F, 5.0F, -9.0F);
      b.box(2.4F, 0.1F, 0.1F, c("555555"), -13.0F, 9.6F, -9.0F);

      for (int i = 0; i < 36; i++) {
         float a = i / 36.0F * (float) (Math.PI * 2);
         float x = MathUtils.sin(a) * 30.0F;
         float zz = MathUtils.cos(a) * 30.0F;
         if (!(Math.abs(x - -42.0F) < 8.0F)) {
            b.cyl(0.07F, 0.09F, 1.6F, 5, c("5a4a3a"), x, this.height(x, zz) + 0.8F, zz);
         }
      }

      for (float[] p : new float[][]{{14.0F, -14.0F}, {-16.0F, 14.0F}}) {
         b.cyl(0.15F, 0.2F, 9.0F, 6, c("444444"), p[0], 4.5F, p[1]);
         b.box(1.6F, 0.8F, 0.6F, c("333333"), p[0], 9.2F, p[1]);
      }

      Mat pm = Mat.vc();
      pm.shadowRecv = true;
      Node3 n = this.add(b.mesh(), pm);
      n.castShadow = true;
      if (this.night) {
         MB lb = new MB();

         for (float[] p : new float[][]{{14.0F, -14.0F}, {-16.0F, 14.0F}}) {
            lb.box(1.3F, 0.5F, 0.1F, c("fff0c0"), p[0], 9.2F, p[1] - 0.32F);
         }

         this.add(lb.mesh(), Models.MAT_GLOW);
      }
   }

   Mat std(Color col, Texture tex, boolean colors) {
      Mat m = new Mat();
      m.color.set(col);
      if (this.night) {
         m.color.mul(0.6F, 0.6F, 0.6F, 1.0F);
      }

      m.color.a = 1.0F;
      m.tex = tex;
      m.rough = 1.0F;
      m.vcol = colors;
      m.spec = 0.25F;
      return m;
   }

   static Color darkened(Color c, float k) {
      return new Color(c.r * (1.0F - k), c.g * (1.0F - k), c.b * (1.0F - k), c.a);
   }

   static Color lightened(Color c, float k) {
      return new Color(c.r + (1.0F - c.r) * k, c.g + (1.0F - c.g) * k, c.b + (1.0F - c.b) * k, c.a);
   }

   void buildTrees(String[] Gr, int gfx) {
      int N = gfx >= 2 ? 520 : (gfx >= 1 ? 380 : 220);
      if (G.isMobile()) {
         N = new int[]{180, 260, 340}[MathUtils.clamp(gfx, 0, 2)];
      }

      Texture needle = Tex.grain("needle", 0.72F, 900, 0.4F, 1.0F);
      String kind = Gr[4];
      MB crown = new MB();
      MB crown2 = new MB();
      MB trunk = new MB();
      MB.Prim pT = null;
      Mat mT = null;
      MB.Prim pC;
      MB.Prim pC2;
      Mat mC;
      Mat mC2;
      if (kind.equals("pine") || kind.equals("snowpine")) {
         pC = MB.pCyl(0.0F, 2.4F, 5.5F, 8, true);
         pC2 = MB.pCyl(0.0F, 1.7F, 4.2F, 8, true);
         mC = this.std(kind.equals("snowpine") ? c("3a5646") : c("34502e"), needle, true);
         mC2 = this.std(kind.equals("snowpine") ? c("e6edf4") : c("3d5c33"), needle, true);
         pT = MB.pCyl(0.25F, 0.35F, 2.0F, 5, false);
         mT = this.std(c("4a3426"), null, false);
      } else if (kind.equals("autumn")) {
         pC = MB.pSph(2.6F, 4.6F, 8, 4);
         pC2 = MB.pSph(1.8F, 3.2F, 7, 3);
         mC = this.std(Color.WHITE, needle, true);
         mC2 = this.std(Color.WHITE, needle, true);
         pT = MB.pCyl(0.22F, 0.34F, 3.0F, 5, false);
         mT = this.std(c("4a3426"), null, false);
      } else if (kind.equals("cactus")) {
         pC = MB.pCyl(0.35F, 0.4F, 4.0F, 6, true);
         pC2 = MB.pSph(1.4F, 2.8F, 5, 3);
         mC = this.std(c("5f7a3a"), null, true);
         mC2 = this.std(c(Gr[2]), null, true);
      } else {
         pC = MB.pCyl(0.12F, 0.35F, 6.0F, 5, true);
         pC2 = MB.pCyl(0.0F, 1.6F, 3.0F, 8, true);
         mC = this.std(c("2a221c"), null, true);
         mC2 = this.std(c("3a2e24"), null, true);
      }

      Matrix4 t = new Matrix4();
      Color[] leaves = new Color[]{c("d2721e"), c("c0401e"), c("e0a62a"), c("9a5a1a"), c("6f7a2a")};
      int k = 0;
      int guard = 0;

      while (k < N && guard < N * 10) {
         guard++;
         float a = this.R.nextFloat() * (float) (Math.PI * 2);
         float r = this.rr(45.0F, 1150.0F);
         float x = MathUtils.sin(a) * r;
         float z = MathUtils.cos(a) * r;
         if (!(Math.abs(x - -42.0F) < 14.0F)) {
            float y = this.height(x, z);
            if (!(y < -1.0F)) {
               float s = this.rr(0.7F, 1.5F);
               float sy = s * this.rr(0.85F, 1.3F);
               float ry = this.R.nextFloat() * (float) (Math.PI * 2);
               Color tc = darkened(Color.WHITE, this.rr(-0.15F, 0.25F));
               tc.a = 1.0F;
               if (kind.equals("pine") || kind.equals("snowpine")) {
                  crown.add(pC, tc, inst(t, x, y + 4.0F * sy, z, ry, s, sy));
                  crown2.add(pC2, kind.equals("pine") ? tc : Color.WHITE, inst(t, x, y + 6.6F * sy, z, ry, s, sy));
                  trunk.add(pT, Color.WHITE, inst(t, x, y + 1.0F * sy, z, ry, s, sy));
               } else if (kind.equals("autumn")) {
                  Color leaf = leaves[this.R.nextInt(5)];
                  crown.add(pC, darkened(leaf, this.rr(-0.1F, 0.2F)), inst(t, x, y + 4.4F * sy, z, ry, s, sy));
                  crown2.add(pC2, lightened(leaf, 0.08F), inst(t, x + 0.8F, y + 6.2F * sy, z - 0.5F, ry, s, sy));
                  trunk.add(pT, Color.WHITE, inst(t, x, y + 1.5F * sy, z, ry, s, sy));
               } else if (kind.equals("cactus")) {
                  crown.add(pC, tc, inst(t, x, y + 2.0F * sy, z, ry, s, sy));
                  crown2.add(pC2, tc, inst(t, x + 6.0F, y + 0.3F, z + 4.0F, ry, s, sy));
               } else {
                  crown.add(pC, tc, inst(t, x, y + 3.0F * sy, z, ry, s, sy));
                  crown2.add(pC2, tc, inst(t, x + 3.0F, y + 1.0F, z, ry, s * 0.8F, sy * 0.8F));
               }

               k++;
            }
         }
      }

      this.add(crown.mesh(), mC);
      this.add(crown2.mesh(), mC2);
      if (!trunk.empty() && mT != null) {
         this.add(trunk.mesh(), mT);
      }
   }

   static Matrix4 inst(Matrix4 t, float x, float y, float z, float ry, float s, float sy) {
      return t.idt().translate(x, y, z).rotateRad(Vector3.Y, ry).scale(s, sy, s);
   }

   void buildGrass(String[] Gr, int gfx) {
      Texture tex = Tex.grass();
      MB b = new MB();
      MB.Prim q = MB.pQuad(1.5F, 1.0F);
      boolean mob = G.isMobile();
      int N = !mob ? (gfx >= 2 ? 2600 : 1400) : new int[]{500, 900, 1300}[MathUtils.clamp(gfx, 0, 2)];
      Matrix4 t = new Matrix4();
      Matrix4 base = new Matrix4();
      Color col = new Color();

      for (int i = 0; i < N; i++) {
         float x;
         float z;
         do {
            float a = this.R.nextFloat() * (float) (Math.PI * 2);
            float r = 9.0F + (float)Math.pow(this.R.nextFloat(), 0.8) * 130.0F;
            x = MathUtils.sin(a) * r;
            z = MathUtils.cos(a) * r;
         } while (!(Math.abs(x - -42.0F) >= 6.0F) || Math.abs(x) < 9.0F && z > -10.0F && z < 11.0F);

         float s = this.rr(0.7F, 1.5F);
         base.idt().translate(x, this.height(x, z) - 0.05F, z).rotateRad(Vector3.Y, this.R.nextFloat() * (float) Math.PI).scale(s, s * this.rr(0.7F, 1.2F), s);
         hsv(col, 0.17F + this.rr(-0.04F, 0.05F), 0.35F, this.rr(0.85F, 1.05F));

         for (int k = 0; k < 3; k++) {
            t.set(base).translate(0.0F, 0.5F, 0.0F).rotateRad(Vector3.Y, k * (float) Math.PI / 3.0F);
            int start = b.v.size;
            b.add(q, col, t);

            for (int o = start; o < b.v.size; o += 12) {
               b.v.items[o + 3] = 0.0F;
               b.v.items[o + 4] = 1.0F;
               b.v.items[o + 5] = 0.0F;
            }
         }
      }

      Mat m = this.std(lightened(c(Gr[5]), 0.12F), tex, true);
      m.cull = false;
      m.scissor = true;
      this.add(b.mesh(), m);
   }

   static void hsv(Color out, float h, float s, float v) {
      h = (h % 1.0F + 1.0F) % 1.0F;
      int i = (int)(h * 6.0F);
      float f = h * 6.0F - i;
      float p = v * (1.0F - s);
      float q = v * (1.0F - f * s);
      float t = v * (1.0F - (1.0F - f) * s);
      switch (i % 6) {
         case 0:
            out.set(v, t, p, 1.0F);
            break;
         case 1:
            out.set(q, v, p, 1.0F);
            break;
         case 2:
            out.set(p, v, t, 1.0F);
            break;
         case 3:
            out.set(p, q, v, 1.0F);
            break;
         case 4:
            out.set(t, p, v, 1.0F);
            break;
         default:
            out.set(v, p, q, 1.0F);
      }
   }

   void buildRocks(String[] Gr) {
      MB b = new MB();
      MB.Prim sph = MB.pSph(1.0F, 2.0F, 6, 3);
      Matrix4 t = new Matrix4();

      for (int i = 0; i < 160; i++) {
         float a = this.R.nextFloat() * (float) (Math.PI * 2);
         float r = this.rr(35.0F, 700.0F);
         float x = MathUtils.sin(a) * r;
         float z = MathUtils.cos(a) * r;
         float s = this.rr(0.6F, 3.5F);
         t.idt()
            .translate(x, this.height(x, z) + s * 0.2F, z)
            .rotateRad(Vector3.Y, this.R.nextFloat() * 3.0F)
            .rotateRad(Vector3.X, this.R.nextFloat() * 3.0F)
            .scale(s, s * this.rr(0.5F, 0.9F), s);
         b.add(sph, Color.WHITE, t);
      }

      Mat m = this.std(c(Gr[2]), Tex.grain("rock", 0.75F, 700, 0.45F, 1.0F), false);
      this.add(b.mesh(), m);
   }

   void buildMountains(JsonValue T, String[] Gr, int gfx) {
      int SEG = gfx >= 1 ? 180 : 120;
      int RAD = 16;
      float r0 = 1250.0F;
      float r1 = 2700.0F;
      Color cM = c(Gr[3]);
      Color cS = this.night ? c("8c96aa") : c("f2f5fa");
      Color cL = c(Gr[1]);
      float ph = this.R.nextFloat() * 100.0F;
      int nv = (SEG + 1) * (RAD + 1);
      float[] p = new float[nv * 3];
      float[] col = new float[nv * 3];
      float[] uv = new float[nv * 2];
      Color cc = new Color();
      int k = 0;

      for (int i = 0; i <= SEG; i++) {
         float a = (float)i / SEG * (float) (Math.PI * 2);

         for (int j = 0; j <= RAD; j++) {
            float t = (float)j / RAD;
            float r = r0 + (r1 - r0) * t;
            float x = MathUtils.sin(a) * r;
            float z = MathUtils.cos(a) * r;
            float ridge = 0.0F;
            float amp = 1.0F;
            float f = 1.0F;

            for (int o = 0; o < 4; o++) {
               ridge = (float)(ridge + (1.0 - Math.abs(Math.sin(a * 7.0F * f + ph + o * 1.7F) * Math.cos(a * 3.3F * f + o))) * amp);
               amp *= 0.5F;
               f *= 2.1F;
            }

            float envv = MathUtils.sin(t * (float) Math.PI) * (0.35F + 0.65F * t);
            float y = ridge * 360.0F * envv + this.height(x * 0.5F, z * 0.5F) * 0.5F - 20.0F;
            if (this.sea && MathUtils.sin(a) > 0.3F && MathUtils.cos(a) < 0.7F) {
               y = Math.min(y, -6.0F);
            }

            cc.set(cL).lerp(cM, MathUtils.clamp(y / 120.0F, 0.0F, 1.0F));
            float sl = this.themeName.equals("snow") ? 90.0F : 220.0F;
            if (y > sl) {
               cc.lerp(cS, MathUtils.clamp((y - sl) / 60.0F, 0.0F, 1.0F));
            }

            if (this.night) {
               cc.mul(0.6F, 0.6F, 0.6F, 1.0F);
            }

            p[k * 3] = x;
            p[k * 3 + 1] = y;
            p[k * 3 + 2] = z;
            col[k * 3] = cc.r;
            col[k * 3 + 1] = cc.g;
            col[k * 3 + 2] = cc.b;
            uv[k * 2] = a * 40.0F;
            uv[k * 2 + 1] = t * 12.0F;
            k++;
         }
      }

      IntArray idx = new IntArray();

      for (int i = 0; i < SEG; i++) {
         for (int j = 0; j < RAD; j++) {
            int A = i * (RAD + 1) + j;
            int B = A + RAD + 1;
            idx.addAll(new int[]{A, A + 1, B, B, A + 1, B + 1});
         }
      }

      Mat m = Mat.vc();
      m.tex = Tex.grain("rock", 0.75F, 700, 0.45F, 1.0F);
      m.rough = 1.0F;
      m.cull = false;
      m.spec = 0.2F;
      this.add(this.indexedMesh(p, col, uv, idx, true), m);
   }

   void buildExtras(JsonValue T) {
      if (this.themeName.equals("dusk")
         || this.themeName.equals("night")
         || this.themeName.equals("dawn")
         || this.themeName.equals("coast")
         || this.themeName.equals("snow")) {
         MB b = new MB();
         MB wl = new MB();
         float cx = 290.0F;
         float cz = -460.0F;

         for (int i = 0; i < 44; i++) {
            float x = cx + this.rr(-110.0F, 110.0F);
            float z = cz + this.rr(-90.0F, 90.0F);
            float w = this.rr(7.0F, 12.0F);
            float h = this.rr(5.0F, 9.0F);
            float d = this.rr(7.0F, 10.0F);
            float y = this.height(x, z);
            float ry = Math.round(this.R.nextFloat() * 3.0F) * (float) Math.PI / 2.0F;
            b.box(w, h, d, this.themeName.equals("snow") ? c("a89a8a") : c("b9a48a"), x, y + h / 2.0F - 0.5F, z, 0.0F, ry, 0.0F);
            b.cyl(
               0.0F,
               Math.max(w, d) * 0.75F,
               h * 0.45F,
               4,
               this.themeName.equals("snow") ? c("eef2f8") : c("7a3b2e"),
               x,
               y + h + h * 0.2F - 0.5F,
               z,
               0.0F,
               ry + (float) (Math.PI / 4),
               0.0F
            );
            if (this.night && this.R.nextFloat() < 0.7F) {
               wl.box(1.2F, 1.1F, 0.2F, c("ffd27a"), x, y + h * 0.5F, z + d / 2.0F * 1.01F);
            }
         }

         this.add(b.mesh(), Models.MAT_VC);
         if (!wl.empty()) {
            this.add(wl.mesh(), Models.MAT_GLOW);
         }
      }

      if (T.getBoolean("factory", false) || this.themeName.equals("industry")) {
         MB b = new MB();
         MB gl = new MB();

         for (int ix = 0; ix < 14; ix++) {
            float x = this.rr(-500.0F, 500.0F);
            float z = this.rr(-900.0F, -600.0F);
            float w = this.rr(30.0F, 70.0F);
            float h = this.rr(18.0F, 45.0F);
            float d = this.rr(30.0F, 60.0F);
            b.box(w, h, d, c("3a3030"), x, this.height(x, z) + h / 2.0F, z);
            gl.box(w * 0.8F, 1.2F, 0.5F, c("ff8a3a"), x, this.height(x, z) + h * 0.6F, z + d / 2.0F + 0.3F);
         }

         for (int ix = 0; ix < 7; ix++) {
            float x = -360 + ix * 120 + this.rr(-30.0F, 30.0F);
            float z = this.rr(-820.0F, -700.0F);
            float h = this.rr(70.0F, 120.0F);
            float y = this.height(x, z);
            b.cyl(5.0F, 7.0F, h, 10, c("4a3a36"), x, y + h / 2.0F, z);
            b.cyl(5.3F, 5.3F, 3.0F, 10, c("8a2a20"), x, y + h - 6.0F, z);
            this.stacks.add(new Vector3(x, y + h, z));
         }

         this.add(b.mesh(), Models.MAT_VC);
         this.add(gl.mesh(), Models.MAT_GLOW);
      }

      if (this.themeName.equals("desert")) {
         MB b = new MB();

         for (int ix = 0; ix < 8; ix++) {
            float x = 220.0F + ix % 4 * 34;
            float z = -380.0F - (float)Math.floor(ix / 4.0F) * 40.0F;
            float y = this.height(x, z);
            b.cyl(13.0F, 13.0F, 14.0F, 20, c("d8d2c4"), x, y + 7.0F, z);
            b.cyl(13.3F, 13.3F, 1.0F, 20, c("b02a20"), x, y + 10.0F, z);
         }

         this.add(b.mesh(), Models.MAT_VC);
      }

      if (this.sea || this.dam) {
         MB w = new MB();
         float sx = this.sea ? 4000.0F : 300.0F;
         float sz = this.sea ? 6000.0F : 380.0F;
         Matrix4 t = new Matrix4().setToTranslation(this.sea ? -2150.0F : 0.0F, this.sea ? -3.0F : 14.0F, this.sea ? -500.0F : -440.0F);
         int n = 24;

         for (int ix = 0; ix < n; ix++) {
            for (int j = 0; j < n; j++) {
               float x0 = -sx / 2.0F + sx * ix / n;
               float x1 = -sx / 2.0F + sx * (ix + 1) / n;
               float z0 = -sz / 2.0F + sz * j / n;
               float z1 = -sz / 2.0F + sz * (j + 1) / n;
               w.add(
                  MB.pBox(x1 - x0, 0.01F, z1 - z0), this.night ? c("0b1a2a") : c("1d4a6a"), new Matrix4(t).translate((x0 + x1) / 2.0F, 0.0F, (z0 + z1) / 2.0F)
               );
            }
         }

         Mat wm = Mat.vc();
         wm.rough = 0.1F;
         wm.metal = 0.2F;
         wm.spec = 1.5F;
         this.add(w.mesh(), wm);
      }

      if (this.sea) {
         MB b = new MB();
         float x = -230.0F;
         float z = -360.0F;
         float y = Math.max(0.0F, this.height(x, z));
         b.cyl(4.0F, 6.0F, 34.0F, 14, c("e8e2d6"), x, y + 17.0F, z);

         for (int ix = 0; ix < 3; ix++) {
            b.cyl(4.2F + ix * 0.6F, 4.2F + ix * 0.6F, 3.0F, 14, c("b02a20"), x, y + 6.0F + ix * 10, z);
         }

         b.cyl(3.2F, 3.2F, 5.0F, 10, c("333333"), x, y + 36.0F, z);
         this.add(b.mesh(), Models.MAT_VC);
      }
   }

   void buildClouds(JsonValue T) {
      int n = this.rain > 0.0F ? 60 : 40;
      if (this.night) {
         this.cloudTint.set(c("2a3550"));
      } else if (this.rain > 0.0F) {
         this.cloudTint.set(c("8a8f98"));
      } else {
         this.cloudTint.set(G.rgbList(T.getString("cloudHi", "255,240,230")).lerp(Color.WHITE, 0.6F));
      }

      for (int i = 0; i < n; i++) {
         float a = this.R.nextFloat() * (float) (Math.PI * 2);
         float r = this.rr(250.0F, 2000.0F);
         float w = this.rr(170.0F, 420.0F);
         float y = this.rain > 0.0F ? this.rr(160.0F, 280.0F) : this.rr(230.0F, 560.0F);
         float al = (this.rain > 0.0F ? 0.9F : this.rr(0.5F, 0.85F)) * (this.night ? 0.7F : 1.0F);
         this.clouds.add(new float[]{MathUtils.sin(a) * r, y, MathUtils.cos(a) * r, w, w * 0.45F, al});
      }
   }

   void buildStars() {
      MB b = new MB();

      for (int i = 0; i < 900; i++) {
         float u = this.R.nextFloat() * (float) (Math.PI * 2);
         float v = this.rr(0.05F, 1.0F);
         float r = 2600.0F;
         float q = (float)Math.sqrt(1.0F - v * v);
         float k = this.rr(0.5F, 1.0F);
         b.box(2.5F, 2.5F, 2.5F, new Color(k, k, k, 1.0F), MathUtils.cos(u) * q * r, v * r, MathUtils.sin(u) * q * r);
      }

      Mat sm = Mat.glow();
      sm.fog = false;
      sm.order = -20;
      this.stars = this.add(b.mesh(), sm);
      MB mb = new MB();
      mb.sph(60.0F, c("dfe8ff"), 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 16);
      Mat mm = Mat.glow();
      mm.fog = false;
      mm.order = -20;
      this.moon = this.add(mb.mesh(), mm);
   }

   public void update(float dt, Camera cam) {
      this.tAcc += dt;
      ArrayIterator cp = this.clouds.iterator();

      while (cp.hasNext()) {
         float[] c2 = (float[])cp.next();
         c2[0] += dt * (2.0F + this.wind * 0.03F);
         if (c2[0] > 2100.0F) {
            c2[0] = -2100.0F;
         }

         if (c2[0] < -2100.0F) {
            c2[0] = 2100.0F;
         }
      }

      Vector3 cpx = cam.position;
      if (this.stars != null) {
         this.stars.pos.set(cpx.x, 0.0F, cpx.z);
      }

      if (this.moon != null) {
         this.moon.pos.set(cpx.x, 0.0F, cpx.z).add(this.tv.set(0.5F, 0.45F, -1.0F).nor().scl(2500.0F));
      }

      if (this.rain > 0.0F) {
         this.lightning -= dt;
         if (this.lightning <= 0.0F) {
            this.lightning = this.rr(5.0F, 12.0F);
            this.flashT = 0.25F;
         }
      }

      if (this.flashT > 0.0F) {
         this.flashT -= dt;
         this.rend.env.flash = Math.max(0.0F, this.flashT) * 1.0F;
      } else {
         this.rend.env.flash = 0.0F;
      }

      this.cloudDyn.clear();
      Vector3 right = this.rightV.set(cam.direction).crs(cam.up).nor();
      Vector3 up = this.upV.set(right).crs(cam.direction).nor();
      float cr = this.cloudTint.r;
      float cg = this.cloudTint.g;
      float cb = this.cloudTint.b;
      cr = (float)Math.pow(cr, 2.2);
      cg = (float)Math.pow(cg, 2.2);
      cb = (float)Math.pow(cb, 2.2);
      ArrayIterator n = this.clouds.iterator();

      while (n.hasNext()) {
         float[] c2x = (float[])n.next();
         float x = c2x[0];
         float y = c2x[1];
         float z = c2x[2];
         float hw = c2x[3] / 2.0F;
         float hh = c2x[4] / 2.0F;
         float a = c2x[5];
         float ax = x - right.x * hw - up.x * hh;
         float ay = y - right.y * hw - up.y * hh;
         float az = z - right.z * hw - up.z * hh;
         float bx = x + right.x * hw - up.x * hh;
         float by = y + right.y * hw - up.y * hh;
         float bz = z + right.z * hw - up.z * hh;
         float cx = x + right.x * hw + up.x * hh;
         float cy = y + right.y * hw + up.y * hh;
         float cz = z + right.z * hw + up.z * hh;
         float dx = x - right.x * hw + up.x * hh;
         float dy = y - right.y * hw + up.y * hh;
         float dz = z - right.z * hw + up.z * hh;
         this.cloudDyn.vert(ax, ay, az, 0.0F, 1.0F, 0.0F, cr, cg, cb, a, 0.0F, 1.0F);
         this.cloudDyn.vert(dx, dy, dz, 0.0F, 1.0F, 0.0F, cr, cg, cb, a, 0.0F, 0.0F);
         this.cloudDyn.vert(cx, cy, cz, 0.0F, 1.0F, 0.0F, cr, cg, cb, a, 1.0F, 0.0F);
         this.cloudDyn.vert(ax, ay, az, 0.0F, 1.0F, 0.0F, cr, cg, cb, a, 0.0F, 1.0F);
         this.cloudDyn.vert(cx, cy, cz, 0.0F, 1.0F, 0.0F, cr, cg, cb, a, 1.0F, 0.0F);
         this.cloudDyn.vert(bx, by, bz, 0.0F, 1.0F, 0.0F, cr, cg, cb, a, 1.0F, 1.0F);
      }

      this.rainDyn.clear();
      if (this.rain > 0.0F || this.snow) {
         int nx = this.rain > 0.0F ? 700 : 900;
         if (this.precip == null || this.precip.length != nx * 3) {
            this.precip = new float[nx * 3];

            for (int i = 0; i < nx; i++) {
               this.precip[i * 3] = this.rr(-60.0F, 60.0F);
               this.precip[i * 3 + 1] = this.rr(-30.0F, 32.0F);
               this.precip[i * 3 + 2] = this.rr(-60.0F, 60.0F);
            }
         }

         float sp = this.rain > 0.0F ? 50.0F : 3.5F;

         for (int i = 0; i < nx; i++) {
            this.precip[i * 3 + 1] = this.precip[i * 3 + 1] - sp * dt;
            if (this.snow) {
               this.precip[i * 3] = this.precip[i * 3] + MathUtils.sin(this.tAcc + i) * dt * 0.8F;
            }

            this.precip[i * 3] = this.precip[i * 3] + this.wind * 0.02F * dt;
            if (this.precip[i * 3 + 1] < -30.0F) {
               this.precip[i * 3 + 1] = this.precip[i * 3 + 1] + 62.0F;
               this.precip[i * 3] = this.rr(-60.0F, 60.0F);
               this.precip[i * 3 + 2] = this.rr(-60.0F, 60.0F);
            }

            float x = cpx.x + this.precip[i * 3];
            float y = cpx.y + this.precip[i * 3 + 1];
            float z = cpx.z + this.precip[i * 3 + 2];
            if (this.rain > 0.0F) {
               float w = 0.03F;
               float h = 1.3F;
               float rx = right.x * w;
               float rz = right.z * w;
               this.rainDyn.vert(x - rx, y, z - rz, 0.0F, 1.0F, 0.0F, 0.45F, 0.5F, 0.6F, 0.45F, 0.0F, 0.0F);
               this.rainDyn.vert(x - rx, y + h, z - rz, 0.0F, 1.0F, 0.0F, 0.45F, 0.5F, 0.6F, 0.45F, 0.0F, 0.0F);
               this.rainDyn.vert(x + rx, y + h, z + rz, 0.0F, 1.0F, 0.0F, 0.45F, 0.5F, 0.6F, 0.45F, 0.0F, 0.0F);
               this.rainDyn.vert(x - rx, y, z - rz, 0.0F, 1.0F, 0.0F, 0.45F, 0.5F, 0.6F, 0.45F, 0.0F, 0.0F);
               this.rainDyn.vert(x + rx, y + h, z + rz, 0.0F, 1.0F, 0.0F, 0.45F, 0.5F, 0.6F, 0.45F, 0.0F, 0.0F);
               this.rainDyn.vert(x + rx, y, z + rz, 0.0F, 1.0F, 0.0F, 0.45F, 0.5F, 0.6F, 0.45F, 0.0F, 0.0F);
            } else {
               this.rainDyn.billboard(this.tv.set(x, y, z), 0.18F, right, up, 1.0F, 1.0F, 1.0F, 0.9F, 0.0F);
            }
         }
      }
   }

   public void dispose() {
      ArrayIterator var1 = this.meshes.iterator();

      while (var1.hasNext()) {
         Mesh3 m = (Mesh3)var1.next();
         m.dispose();
      }

      this.meshes.clear();
      this.cloudDyn.dispose();
      this.rainDyn.dispose();
   }

   static {
      ATM.put("dusk", new float[]{6.0F, 2.6F, 0.006F, 13.0F});
      ATM.put("dawn", new float[]{4.5F, 2.0F, 0.005F, 9.0F});
      ATM.put("snow", new float[]{2.2F, 1.2F, 0.004F, 22.0F});
      ATM.put("desert", new float[]{9.0F, 3.2F, 0.009F, 12.0F});
      ATM.put("coast", new float[]{5.0F, 3.0F, 0.005F, 5.0F});
      ATM.put("industry", new float[]{14.0F, 4.2F, 0.012F, 4.0F});
      ATM.put("night", new float[]{4.0F, 2.0F, 0.005F, 10.0F});
      ATM.put("autumn", new float[]{6.0F, 2.6F, 0.006F, 12.0F});
      ATM.put("storm", new float[]{12.0F, 3.8F, 0.011F, 18.0F});
      GROUND.put("dusk", new String[]{"7d7a3c", "56622e", "8c6c46", "5d6584", "pine", "77803a"});
      GROUND.put("night", new String[]{"4b5a35", "34452a", "5a5048", "2f3a58", "pine", "4d5c34"});
      GROUND.put("dawn", new String[]{"86904a", "5f7034", "95785a", "6e5d82", "pine", "8a9a48"});
      GROUND.put("snow", new String[]{"eef3f8", "c9d4e2", "8a96a8", "93a6bd", "snowpine", ""});
      GROUND.put("desert", new String[]{"d9a86a", "bf8a52", "9a6a44", "a8683c", "cactus", "a08a50"});
      GROUND.put("coast", new String[]{"6f7a40", "4f5f30", "7d7468", "2b3d5e", "pine", "6f7a3a"});
      GROUND.put("industry", new String[]{"5a4a3c", "3d332b", "4a3e36", "3b2a2a", "dead", "5a5236"});
      GROUND.put("autumn", new String[]{"9a7a3a", "7a5a2a", "8c6c46", "6a4a3a", "autumn", "a88a42"});
      GROUND.put("storm", new String[]{"56603c", "3e472e", "5a5650", "3a4250", "pine", "5c6640"});
   }
}
