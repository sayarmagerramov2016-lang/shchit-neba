package ru.shitneba;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Array.ArrayIterator;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import ru.shitneba.gfx.MB;
import ru.shitneba.gfx.Mesh3;
import ru.shitneba.gfx.Node3;

public final class SModels {
   static final Color CF = c("1d2023");
   static final Color CF2 = c("2b2f33");
   static final Color MOT = c("3c4044");
   static final Color BELL = c("c45a24");
   static final Color OLV = c("4b5a32");
   static final Color OLV2 = c("5d6b3c");
   static final Color BAT = c("d9b21c");
   static final Color LG = c("8e949b");
   static final Color WH = c("d6d9dc");
   static final float PI = (float) Math.PI;
   static Mesh3 prop;
   static final Map<String, SModels.DroneTpl> droneCache = new HashMap<>();
   static Mesh3 bomb;
   static final Map<String, SModels.Target> tgCache = new HashMap<>();

   static Color c(String h) {
      return G.hex(h);
   }

   static Mesh3 propMesh() {
      if (prop != null) {
         return prop;
      } else {
         MB b = new MB();
         b.noAo = true;
         b.cyl(1.0F, 1.0F, 0.012F, 28, Color.WHITE);
         prop = b.mesh();
         return prop;
      }
   }

   static Vector3 V(float x, float y, float z) {
      return new Vector3(x, y, z);
   }

   public static SModels.Drone drone(String kind) {
      SModels.DroneTpl t = droneCache.get(kind);
      if (t == null) {
         t = buildDrone(kind);
         droneCache.put(kind, t);
      }

      SModels.Drone d = new SModels.Drone();
      d.g = t.g.cloneTree();
      d.r = t.r;
      d.cam.set(t.cam).scl(t.sc);
      ArrayIterator var3 = d.g.kids.iterator();

      while (var3.hasNext()) {
         Node3 k = (Node3)var3.next();
         if ("rot".equals(k.name)) {
            d.rot.add(k);
         }

         if ("bomb".equals(k.name)) {
            d.bombs.add(k);
         }
      }

      return d;
   }

   static SModels.DroneTpl buildDrone(String kind) {
      MB.hq = true;
      MB b = new MB();
      MB m = new MB();
      MB gl = new MB();
      gl.noAo = true;
      Array<float[]> props = new Array();
      float r;
      float sc;
      Vector3 cam;
      switch (kind) {
         case "osa":
            b.box(0.36F, 0.04F, 0.5F, CF, 0.0F, 0.0F, 0.0F);
            b.box(0.32F, 0.03F, 0.42F, CF2, 0.0F, 0.15F, 0.0F);

            for (float x : new float[]{-0.13F, 0.13F}) {
               for (float z : new float[]{-0.17F, 0.17F}) {
                  m.cyl(0.012F, 0.012F, 0.15F, 6, LG, x, 0.075F, z);
               }
            }

            for (int i = 0; i < 4; i++) {
               float a = (float) (Math.PI / 4) + i * (float) Math.PI / 2.0F;
               float px = MathUtils.cos(a) * 0.46F;
               float pz = MathUtils.sin(a) * 0.46F;
               b.box(0.62F, 0.035F, 0.075F, CF, px * 0.5F, 0.0F, pz * 0.5F, 0.0F, -a, 0.0F);
               m.cyl(0.06F, 0.065F, 0.07F, 14, MOT, px, 0.05F, pz);
               m.cyl(0.055F, 0.055F, 0.035F, 14, BELL, px, 0.1F, pz);
               m.cyl(0.01F, 0.01F, 0.06F, 6, LG, px, 0.14F, pz);
               props.add(new float[]{px, 0.15F, pz, 0.25F, 0.0F});
            }

            b.box(0.16F, 0.11F, 0.3F, BAT, 0.0F, 0.23F, 0.02F);
            b.box(0.17F, 0.012F, 0.31F, c("222222"), 0.0F, 0.29F, 0.02F);
            b.box(0.03F, 0.125F, 0.32F, c("1a1a1a"), 0.0F, 0.23F, 0.02F);
            b.box(0.1F, 0.09F, 0.08F, c("2a2a2a"), 0.0F, 0.08F, -0.25F, -0.25F, 0.0F, 0.0F);
            m.cyl(0.03F, 0.035F, 0.05F, 12, c("111111"), 0.0F, 0.09F, -0.3F, 1.3207964F, 0.0F, 0.0F);
            gl.sph(0.022F, new Color(0.3F, 0.6F, 1.0F, 1.0F), 0.0F, 0.095F, -0.325F);
            b.cyl(0.07F, 0.07F, 0.5F, 16, OLV, 0.0F, -0.1F, -0.02F, (float) (Math.PI / 2), 0.0F, 0.0F);
            b.cyl(0.0F, 0.07F, 0.2F, 16, OLV2, 0.0F, -0.1F, -0.37F, (float) (-Math.PI / 2), 0.0F, 0.0F);
            m.cyl(0.012F, 0.012F, 0.08F, 6, LG, 0.0F, -0.1F, -0.5F, (float) (Math.PI / 2), 0.0F, 0.0F);
            b.cyl(0.075F, 0.075F, 0.03F, 16, c("2a2a2a"), 0.0F, -0.1F, 0.1F, (float) (Math.PI / 2), 0.0F, 0.0F);
            b.cyl(0.075F, 0.075F, 0.03F, 16, c("2a2a2a"), 0.0F, -0.1F, -0.12F, (float) (Math.PI / 2), 0.0F, 0.0F);

            for (float s : new float[]{-1.0F, 1.0F}) {
               m.cyl(0.008F, 0.008F, 0.24F, 5, c("111111"), s * 0.08F, 0.25F, 0.27F, 0.6F, 0.0F, s * 0.3F);
               b.sph(0.02F, c("e04020"), s * 0.12F, 0.35F, 0.34F);
            }

            gl.sph(0.02F, new Color(1.0F, 0.2F, 0.1F, 1.0F), 0.0F, 0.03F, 0.26F);
            r = 0.9F;
            sc = 2.2F;
            cam = V(0.0F, 0.12F, -0.38F);
            break;
         case "strizh":
            float rx = (float) (Math.PI / 2);
            b.cyl(0.11F, 0.11F, 1.2F, 18, LG, 0.0F, 0.0F, 0.0F, rx, 0.0F, 0.0F);
            b.cyl(0.0F, 0.11F, 0.32F, 18, c("9aa0a6"), 0.0F, 0.0F, -0.76F, (float) (-Math.PI / 2), 0.0F, 0.0F);
            b.cyl(0.11F, 0.05F, 0.25F, 18, c("7b8187"), 0.0F, 0.0F, 0.72F, rx, 0.0F, 0.0F);
            b.cyl(0.112F, 0.112F, 0.08F, 18, OLV, 0.0F, 0.0F, -0.42F, rx, 0.0F, 0.0F);
            b.cyl(0.112F, 0.112F, 0.04F, 18, c("c03020"), 0.0F, 0.0F, -0.32F, rx, 0.0F, 0.0F);
            gl.sph(0.05F, new Color(0.2F, 0.45F, 0.9F, 1.0F), 0.0F, -0.08F, -0.62F);
            b.sph(0.06F, c("1a2a3a"), 0.0F, -0.07F, -0.6F, 1.0F, 0.7F, 1.3F);

            for (float z : new float[]{-0.18F, 0.42F}) {
               for (float a : new float[]{(float) (Math.PI / 4), (float) (-Math.PI / 4)}) {
                  b.box(z < 0.0F ? 1.15F : 0.9F, 0.016F, z < 0.0F ? 0.2F : 0.17F, WH, 0.0F, 0.0F, z, 0.0F, 0.0F, a);
               }

               for (float a : new float[]{(float) (Math.PI / 4), (float) (-Math.PI / 4), (float) (Math.PI * 3.0 / 4.0), (float) (-Math.PI * 3.0 / 4.0)}) {
                  float k = z < 0.0F ? 0.55F : 0.43F;
                  b.box(0.03F, 0.03F, 0.06F, c("c03020"), MathUtils.cos(a) * k, MathUtils.sin(a) * k, z);
               }
            }

            m.cyl(0.006F, 0.006F, 0.22F, 5, c("111111"), 0.0F, 0.2F, 0.2F);
            m.cyl(0.04F, 0.04F, 0.08F, 10, MOT, 0.0F, 0.0F, 0.86F, rx, 0.0F, 0.0F);
            props.add(new float[]{0.0F, 0.0F, 0.92F, 0.24F, 1.0F});
            gl.sph(0.025F, new Color(1.0F, 0.2F, 0.1F, 1.0F), 0.0F, 0.12F, 0.6F);
            r = 1.1F;
            sc = 1.7F;
            cam = V(0.0F, -0.1F, -0.75F);
            break;
         case "taran":
            b.cyl(0.3F, 0.34F, 0.16F, 8, c("3b4430"), 0.0F, 0.0F, 0.0F);
            b.sph(0.26F, c("46503a"), 0.0F, 0.08F, 0.0F, 1.0F, 0.45F, 1.15F);

            for (float s : new float[]{-1.0F, 1.0F}) {
               b.box(0.04F, 0.16F, 0.42F, c("56603f"), s * 0.33F, 0.02F, 0.0F, 0.0F, 0.0F, s * 0.2F);
            }

            b.box(0.38F, 0.12F, 0.05F, c("56603f"), 0.0F, 0.02F, -0.34F, 0.3F, 0.0F, 0.0F);

            for (int i = 0; i < 6; i++) {
               float a = (float) (Math.PI / 6) + i * (float) Math.PI / 3.0F;
               Vector3 p = V(MathUtils.cos(a) * 0.72F, 0.03F, MathUtils.sin(a) * 0.72F);
               m.seg(V(MathUtils.cos(a) * 0.25F, 0.03F, MathUtils.sin(a) * 0.25F), p, 0.035F, 0.03F, 10, CF2);
               m.cyl(0.075F, 0.08F, 0.1F, 14, MOT, p.x, p.y + 0.06F, p.z);
               m.cyl(0.07F, 0.07F, 0.04F, 14, c("a03a20"), p.x, p.y + 0.12F, p.z);
               props.add(new float[]{p.x, p.y + 0.17F, p.z, 0.32F, 0.0F});
            }

            float rx2 = (float) (Math.PI / 2);
            b.cyl(0.15F, 0.15F, 0.55F, 18, OLV, 0.0F, -0.24F, -0.05F, rx2, 0.0F, 0.0F);
            b.cyl(0.155F, 0.155F, 0.06F, 18, c("d07a20"), 0.0F, -0.24F, -0.2F, rx2, 0.0F, 0.0F);
            b.cyl(0.04F, 0.15F, 0.18F, 18, OLV2, 0.0F, -0.24F, -0.41F, (float) (-Math.PI / 2), 0.0F, 0.0F);
            m.cyl(0.015F, 0.015F, 0.12F, 6, LG, 0.0F, -0.24F, -0.55F, rx2, 0.0F, 0.0F);

            for (float s : new float[]{-1.0F, 1.0F}) {
               m.seg(V(s * 0.18F, -0.05F, -0.15F), V(s * 0.3F, -0.42F, -0.2F), 0.015F, 0.015F, 6, CF2);
               m.seg(V(s * 0.18F, -0.05F, 0.15F), V(s * 0.3F, -0.42F, 0.2F), 0.015F, 0.015F, 6, CF2);
               m.seg(V(s * 0.3F, -0.42F, -0.32F), V(s * 0.3F, -0.42F, 0.32F), 0.018F, 0.018F, 6, CF2);
            }

            b.box(0.1F, 0.08F, 0.08F, c("222222"), 0.0F, 0.0F, -0.36F);
            gl.sph(0.025F, new Color(0.3F, 0.6F, 1.0F, 1.0F), 0.0F, 0.0F, -0.41F);

            for (float s : new float[]{-1.0F, 1.0F}) {
               gl.sph(0.025F, s < 0.0F ? new Color(1.0F, 0.2F, 0.1F, 1.0F) : new Color(0.2F, 1.0F, 0.3F, 1.0F), s * 0.3F, 0.05F, 0.25F);
            }

            r = 1.5F;
            sc = 2.3F;
            cam = V(0.0F, 0.02F, -0.45F);
            break;
         default:
            b.box(0.34F, 0.12F, 0.62F, c("33383d"), 0.0F, 0.0F, 0.0F);
            b.box(0.28F, 0.06F, 0.5F, c("4a5056"), 0.0F, 0.09F, 0.0F);
            b.sph(0.12F, c("2a2e32"), 0.0F, 0.02F, -0.33F, 1.2F, 0.8F, 1.0F);
            gl.sph(0.03F, new Color(0.3F, 0.6F, 1.0F, 1.0F), 0.0F, 0.0F, -0.44F);

            for (int i = 0; i < 4; i++) {
               float a = (float) (Math.PI / 4) + i * (float) Math.PI / 2.0F;
               Vector3 p = V(MathUtils.cos(a) * 0.62F, 0.02F, MathUtils.sin(a) * 0.62F);
               m.seg(V(MathUtils.cos(a) * 0.15F, 0.02F, MathUtils.sin(a) * 0.15F), p, 0.03F, 0.026F, 10, CF);
               m.cyl(0.06F, 0.06F, 0.24F, 14, MOT, p.x, p.y, p.z);
               m.cyl(0.058F, 0.058F, 0.04F, 14, c("e0b020"), p.x, p.y + 0.1F, p.z);
               props.add(new float[]{p.x, p.y + 0.15F, p.z, 0.3F, 0.0F});
               props.add(new float[]{p.x, p.y - 0.14F, p.z, 0.3F, 0.0F});
            }

            b.box(0.32F, 0.03F, 0.4F, CF2, 0.0F, -0.1F, 0.0F);

            for (float x : new float[]{-0.12F, 0.0F, 0.12F}) {
               m.box(0.015F, 0.06F, 0.38F, LG, x, -0.13F, 0.0F);
            }

            b.box(0.1F, 0.1F, 0.18F, BAT, 0.0F, 0.16F, 0.12F);

            for (float s : new float[]{-1.0F, 1.0F}) {
               m.cyl(0.007F, 0.007F, 0.2F, 5, c("111111"), s * 0.12F, 0.2F, 0.28F, 0.5F, 0.0F, s * 0.3F);
            }

            r = 1.2F;
            sc = 2.1F;
            cam = V(0.0F, 0.0F, -0.46F);
      }

      Node3 g = new Node3();
      Node3 body = new Node3(b.mesh(), Models.MAT_HULL);
      body.castShadow = true;
      g.add(body);
      if (!m.empty()) {
         Node3 mm = new Node3(m.mesh(), Models.MAT_METAL);
         mm.castShadow = true;
         g.add(mm);
      }

      if (!gl.empty()) {
         g.add(new Node3(gl.mesh(), Models.MAT_GLOW));
      }

      ArrayIterator var31 = props.iterator();

      while (var31.hasNext()) {
         float[] p = (float[])var31.next();
         Node3 n = new Node3(propMesh(), Models.MAT_ROTOR);
         n.name = "rot";
         n.pos.set(p[0], p[1], p[2]);
         n.scl.set(p[3], 1.0F, p[3]);
         if (p[4] > 0.0F) {
            n.setEuler((float) (Math.PI / 2), 0.0F, 0.0F);
         }

         g.add(n);
      }

      if (kind.equals("shershen")) {
         for (float x : new float[]{-0.12F, 0.0F, 0.12F}) {
            Node3 bm = new Node3(bombMesh(), Models.MAT_HULL);
            bm.name = "bomb";
            bm.pos.set(x, -0.26F, 0.0F);
            g.add(bm);
         }
      }

      MB.hq = false;
      g.scl.set(sc, sc, sc);
      SModels.DroneTpl t = new SModels.DroneTpl();
      t.g = g;
      t.r = r;
      t.sc = sc;
      t.cam = cam;
      return t;
   }

   static Mesh3 bombMesh() {
      if (bomb != null) {
         return bomb;
      } else {
         MB b = new MB();
         float rx = (float) (Math.PI / 2);
         b.cyl(0.045F, 0.045F, 0.22F, 12, OLV, 0.0F, 0.0F, 0.0F, rx, 0.0F, 0.0F);
         b.sph(0.045F, OLV2, 0.0F, 0.0F, -0.11F, 1.0F, 1.0F, 1.4F);
         b.cyl(0.045F, 0.02F, 0.08F, 12, c("333333"), 0.0F, 0.0F, 0.15F, rx, 0.0F, 0.0F);

         for (int i = 0; i < 4; i++) {
            float a = i * (float) Math.PI / 2.0F;
            b.box(0.005F, 0.08F, 0.07F, c("333333"), MathUtils.cos(a) * 0.04F, MathUtils.sin(a) * 0.04F, 0.17F, 0.0F, 0.0F, a);
         }

         bomb = b.mesh();
         return bomb;
      }
   }

   public static Node3 bombFree() {
      Node3 n = new Node3();
      Node3 m = new Node3(bombMesh(), Models.MAT_HULL);
      n.add(m);
      n.scl.set(2.1F, 2.1F, 2.1F);
      return n;
   }

   public static SModels.Target target(String kind, String theme) {
      if (kind.equals("radar")) {
         Objs.Obj o = Objs.make("radar");
         SModels.Target t = new SModels.Target();
         t.g = o.g;
         t.spin = o.spin;
         t.r = 4.5F;
         t.hr = 3.0F;
         return t;
      } else {
         String key = kind + theme;
         SModels.Target c = tgCache.get(key);
         if (c == null) {
            MB.hq = true;
            MB b = new MB();
            MB gl = new MB();
            gl.noAo = true;
            boolean snow = theme.equals("snow");
            Color green = c("4c5a3a");
            Random R = new Random(kind.hashCode());
            byte body = -1;
            switch (kind.hashCode()) {
               case 3250:
                  if (kind.equals("ew")) {
                     body = 2;
                  }
                  break;
               case 3337:
                  if (kind.equals("hq")) {
                     body = 1;
                  }
                  break;
               case 95472180:
                  if (kind.equals("depot")) {
                     body = 0;
                  }
            }

            float r;
            float hr;
            label187: {
               int i;
               switch (body) {
                  case 0:
                     float[][] arch = new float[13][];

                     for (int ix = 0; ix < 13; ix++) {
                        float a = (float) Math.PI * ix / 12.0F;
                        arch[ix] = new float[]{MathUtils.cos(a) * 5.5F, MathUtils.sin(a) * 4.2F};
                     }

                     b.extrude(arch, 14.0F, !snow ? c("6b7058") : c("c9ccd0"), 0.0F, 0.6F, 0.0F, 0.0F, (float) (Math.PI / 2), 0.0F);
                     b.box(11.2F, 0.6F, 14.2F, c("5a5a52"), 0.0F, 0.3F, 0.0F);
                     b.box(4.2F, 3.4F, 0.2F, c("3d4030"), 0.0F, 2.3F, -7.05F);

                     for (int ix = 0; ix < 6; ix++) {
                        b.box(0.12F, 4.0F, 14.05F, c("55594a"), -4.6F + ix * 1.84F, 2.4F, 0.0F);
                     }

                     for (int ix = 0; ix < 7; ix++) {
                        float px = -9.0F + R.nextFloat() * 2.5F;
                        float pz = -6.0F + R.nextFloat() * 12.0F;
                        float h = 0.9F + ix % 3 * 0.9F;
                        b.box(1.6F, 0.9F, 1.1F, c("5d6a3a"), px, h - 0.45F, pz, 0.0F, R.nextFloat() * 0.4F, 0.0F);
                     }

                     for (int ix = 0; ix < 8; ix++) {
                        b.cyl(0.4F, 0.4F, 1.2F, 12, ix % 2 == 1 ? c("3d5a6a") : c("6a3a2a"), 7.2F + ix % 2 * 0.9F, 0.6F, -3.0F + ix / 2 * 0.9F);
                     }

                     r = 8.0F;
                     hr = 2.5F;
                     break label187;
                  case 1:
                     b.extrude(
                        new float[][]{{-6.0F, 0.0F}, {6.0F, 0.0F}, {4.5F, 3.2F}, {-4.5F, 3.2F}}, 11.0F, !snow ? c("7d7b72") : c("b8bcc0"), 0.0F, 0.0F, 0.0F
                     );
                     b.box(8.5F, 0.5F, 6.0F, c("6a685f"), 0.0F, 3.4F, 0.0F);
                     b.box(2.4F, 2.2F, 0.3F, c("2a2c28"), 0.0F, 1.1F, -5.6F);
                     b.box(4.0F, 2.6F, 2.4F, c("6a685f"), 0.0F, 1.3F, -6.8F);

                     for (float s : new float[]{-1.0F, 1.0F}) {
                        b.cyl(0.12F, 0.2F, 14.0F, 8, c("777777"), s * 3.2F, 7.5F, 1.5F);

                        for (int k = 0; k < 3; k++) {
                           b.box(1.4F, 0.06F, 0.06F, c("777777"), s * 3.2F, 9 + k * 2, 1.5F);
                        }

                        gl.sph(0.18F, new Color(1.0F, 0.15F, 0.1F, 1.0F), s * 3.2F, 14.6F, 1.5F);
                     }

                     b.cyl(0.9F, 0.9F, 0.5F, 18, c("555555"), -2.0F, 3.9F, 1.8F);
                     b.sph(1.2F, WH, -2.0F, 4.6F, 1.8F, 1.0F, 0.4F, 1.0F);

                     for (int ix = 0; ix < 14; ix++) {
                        float a = (float) (Math.PI / 4) + ix / 13.0F * (float) Math.PI * 1.5F;
                        b.capsule(0.4F, 1.1F, c("9c8a5f"), MathUtils.sin(a) * 8.5F, 0.4F, MathUtils.cos(a) * 8.5F, 0.0F, a, (float) (Math.PI / 2));
                     }

                     r = 7.5F;
                     hr = 2.0F;
                     break label187;
                  case 2:
                     b.box(2.6F, 0.5F, 8.5F, c("2a2c26"), 0.0F, 1.1F, 0.0F);

                     for (float z : new float[]{-3.0F, -1.6F, 2.2F, 3.4F}) {
                        for (float s : new float[]{-1.0F, 1.0F}) {
                           b.cyl(0.6F, 0.6F, 0.45F, 16, c("1a1a1a"), s * 1.25F, 0.6F, z, 0.0F, 0.0F, (float) (Math.PI / 2));
                        }
                     }

                     b.extrude(new float[][]{{-1.0F, 0.0F}, {1.1F, 0.0F}, {1.2F, 0.9F}, {0.9F, 1.8F}, {-1.0F, 1.8F}}, 2.6F, green, 0.0F, 1.3F, -3.2F);
                     b.box(2.5F, 0.6F, 0.05F, c("1b2a3a"), 0.0F, 2.6F, -4.25F);
                     b.box(2.6F, 2.6F, 5.0F, green, 0.0F, 2.7F, 1.3F);
                     b.sph(1.2F, WH, 0.0F, 4.3F, 0.2F, 1.0F, 0.8F, 1.0F);

                     for (int ix = 0; ix < 6; ix++) {
                        float px = -0.9F + ix % 3 * 0.9F;
                        float pz = 2.2F + ix / 3 * 1.1F;
                        b.cyl(0.03F, 0.05F, 3.2F + ix % 2, 6, c("888888"), px, 5.6F, pz);
                        b.box(0.5F, 0.04F, 0.04F, c("888888"), px, 6.8F, pz, 0.0F, ix * 0.6F, 0.0F);
                     }

                     gl.sph(0.14F, new Color(1.0F, 0.6F, 0.1F, 1.0F), 0.0F, 5.3F, 0.2F);
                     r = 5.0F;
                     hr = 2.5F;
                     break label187;
                  default:
                     i = 0;
               }

               while (i < 3) {
                  float x = -5.0F + i * 5.0F;
                  b.cyl(1.7F, 1.7F, 9.0F, 22, i != 1 ? c("a8a69a") : c("8a9070"), x, 2.4F, 0.0F, (float) (Math.PI / 2), 0.0F, 0.0F);

                  for (float z : new float[]{-4.6F, 4.6F}) {
                     b.sph(1.7F, c("9a988c"), x, 2.4F, z, 1.0F, 1.0F, 0.35F);
                  }

                  for (float z : new float[]{-2.8F, 2.8F}) {
                     b.box(3.2F, 0.9F, 0.6F, c("555550"), x, 0.45F, z);
                  }

                  b.box(0.08F, 0.5F, 9.0F, c("c03020"), x + 1.2F, 2.4F, 0.0F);
                  i++;
               }

               b.cyl(0.18F, 0.18F, 12.0F, 8, c("777777"), 0.0F, 0.5F, -6.0F, 0.0F, 0.0F, (float) (Math.PI / 2));
               r = 8.0F;
               hr = 2.4F;
            }

            Node3 g0 = new Node3();
            Node3 body2 = new Node3(b.mesh(), Models.MAT_HULL);
            body2.castShadow = true;
            g0.add(body2);
            if (!gl.empty()) {
               g0.add(new Node3(gl.mesh(), Models.MAT_GLOW));
            }

            MB.hq = false;
            c = new SModels.Target();
            c.g = g0;
            c.r = r;
            c.hr = hr;
            tgCache.put(key, c);
         }

         SModels.Target t = new SModels.Target();
         t.g = c.g.cloneTree();
         t.r = c.r;
         t.hr = c.hr;
         return t;
      }
   }

   private SModels() {
   }

   public static final class Drone {
      public Node3 g;
      public final Array<Node3> rot = new Array();
      public final Array<Node3> bombs = new Array();
      public float r;
      public final Vector3 cam = new Vector3();
   }

   static final class DroneTpl {
      Node3 g;
      float r;
      float sc;
      Vector3 cam;
   }

   public static final class Target {
      public Node3 g;
      public Node3 spin;
      public float r;
      public float hr;
   }
}
