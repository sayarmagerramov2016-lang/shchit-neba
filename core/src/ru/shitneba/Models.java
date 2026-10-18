package ru.shitneba;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Array.ArrayIterator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import ru.shitneba.gfx.MB;
import ru.shitneba.gfx.Mat;
import ru.shitneba.gfx.Mesh3;
import ru.shitneba.gfx.Node3;
import ru.shitneba.gfx.Tex;

public final class Models {
   static final float PI = (float) Math.PI;
   static final Color DARK = c("23261f");
   static final Color TIRE = c("161616");
   static final Color STEEL = c("5a6064");
   static final Color GLASS = c("1b2a3a");
   static final Color WHITE = Color.WHITE;
   static final Color SHADE = c("bfbfbf");
   static final Color OLIVE_D = c("3b4030");
   static final Color GUN = c("34383b");
   static final Color RUBBER = c("1d1d1d");
   static final Color RIVET = c("6a6f68");
   static final Color MARK = c("e8e4d6");
   static final Color AMBER = c("ffb020");
   static final Color HAZ = c("e0b020");
   static final Color GREY = c("4f545b");
   static final Color DGREY = c("2c2f35");
   static final Color LGREY = c("c5c9d0");
   static final Color BD1 = c("2a2d33");
   static final Color BD2 = c("3a3e45");
   static final Color BD3 = c("4c525a");
   static final Color BDK = c("16181b");
   static final Color BGL = c("1d2a40");
   public static Mat MAT_VC;
   public static Mat MAT_GLOW;
   public static Mat MAT_ROTOR;
   public static Mat MAT_HULL;
   public static Mat MAT_METAL;
   public static Mat MAT_RUBBER;
   public static Mat MAT_GLASS;
   public static Mat MAT_LAMP;
   public static Mat MAT_PAINT;
   public static Mat MAT_MISSILE;
   public static Mat MAT_NOSE;
   static final Map<String, Mat> camoMats = new HashMap<>();
   static final float[] S2 = new float[]{-1.0F, 1.0F};
   static final Map<Character, String> SEG = new HashMap<>();
   static Mesh3 noseMesh;
   static final Map<String, Models.Vehicle> vehCache = new HashMap<>();
   static final Map<String, Models.EnemyDef> enemyCache = new HashMap<>();
   static final Map<String, Mesh3> legCache = new HashMap<>();
   static Mesh3 missile;

   static Color c(String h) {
      return G.hex(h);
   }

   static Vector3 V(float x, float y, float z) {
      return new Vector3(x, y, z);
   }

   static Mat hull(float rough, float metal, float seams, float dust) {
      Mat m = new Mat();
      m.hull = true;
      m.vcol = true;
      m.rough = rough;
      m.metal = metal;
      m.seams = seams;
      m.dust = dust;
      return m;
   }

   public static void initMaterials() {
      MAT_VC = Mat.vc();
      MAT_HULL = hull(0.45F, 0.25F, 0.5F, 0.0F);
      MAT_HULL.wear = 0.25F;
      MAT_HULL.panelScale = 0.3F;
      MAT_METAL = hull(0.32F, 0.75F, 0.0F, 0.3F);
      MAT_METAL.shadowRecv = true;
      MAT_RUBBER = hull(0.92F, 0.0F, 0.0F, 0.35F);
      MAT_RUBBER.shadowRecv = true;
      MAT_GLASS = Mat.std(new Color(0.05F, 0.08F, 0.11F, 1.0F));
      MAT_GLASS.rough = 0.05F;
      MAT_GLASS.metal = 0.9F;
      MAT_GLASS.spec = 1.2F;
      MAT_LAMP = Mat.vc();
      MAT_LAMP.emission = 0.9F;
      MAT_LAMP.rough = 0.1F;
      MAT_GLOW = Mat.glow();
      MAT_ROTOR = new Mat();
      MAT_ROTOR.unshaded = true;
      MAT_ROTOR.vcol = false;
      MAT_ROTOR.color.set(0.12F, 0.12F, 0.13F, 0.16F);
      MAT_ROTOR.blend = 1;
      MAT_ROTOR.cull = false;
      MAT_ROTOR.depthWrite = false;
      MAT_PAINT = hull(0.6F, 0.1F, 0.45F, 1.0F);
      MAT_PAINT.panelScale = 0.26F;
      MAT_PAINT.shadowRecv = true;
      MAT_MISSILE = hull(0.38F, 0.25F, 0.3F, 0.0F);
      MAT_NOSE = Mat.std(c("9fa39a"));
      MAT_NOSE.rough = 0.35F;
      MAT_NOSE.metal = 0.3F;
   }

   public static Mat camoMat(String id) {
      Mat m = camoMats.get(id);
      if (m != null) {
         return m;
      } else {
         m = hull(0.55F, 0.12F, 0.42F, 1.0F);
         m.panelScale = 0.26F;
         m.camo = Tex.camoTile(id);
         m.shadowRecv = true;
         camoMats.put(id, m);
         return m;
      }
   }

   static void rail(MB M, Vector3 a, Vector3 b) {
      M.seg(a, b, 0.025F, 0.025F, 6, STEEL);
   }

   static void rail(MB M, Vector3 a, Vector3 b, float r) {
      M.seg(a, b, r, r, 6, STEEL);
   }

   static void wheel(Models.Parts P, String node, float px, float py, float pz, float r, float w, float side) {
      MB R = P.g(node, "r");
      MB D = P.g(node, "d");
      MB M = P.g(node, "m");
      float rz = (float) (Math.PI / 2);
      R.cyl(r * 0.93F, r * 0.93F, w, 22, RUBBER, px, py, pz, 0.0F, 0.0F, rz);
      R.cyl(r * 0.86F, r * 0.86F, w + 0.05F, 22, c("232323"), px, py, pz, 0.0F, 0.0F, rz);
      D.cyl(r * 0.6F, r * 0.6F, w + 0.07F, 18, OLIVE_D, px, py, pz, 0.0F, 0.0F, rz);
      M.cyl(r * 0.22F, r * 0.3F, w + 0.17F, 10, STEEL, px + side * 0.03F, py, pz, 0.0F, 0.0F, rz);

      for (int i = 0; i < 8; i++) {
         float a = i * (float) (Math.PI * 2) / 8.0F;
         M.cyl(0.03F, 0.03F, w + 0.12F, 6, STEEL, px, py + MathUtils.cos(a) * r * 0.42F, pz + MathUtils.sin(a) * r * 0.42F, 0.0F, 0.0F, rz);
      }

      for (int i = 0; i < 20; i++) {
         float a = i * (float) (Math.PI * 2) / 20.0F;
         R.box(w * 0.92F, 0.09F, 0.13F, RUBBER, px, py + MathUtils.cos(a) * r * 0.95F, pz + MathUtils.sin(a) * r * 0.95F, a, 0.0F, 0.0F);
      }
   }

   static void fender(MB b, float zf, float zb, float x, float y, float w, Color col) {
      float hf = (zb - zf) / 2.0F;
      float[][] pts = new float[][]{
         {-hf - 0.3F, -0.28F},
         {-hf, 0.05F},
         {hf, 0.05F},
         {hf + 0.3F, -0.28F},
         {hf + 0.23F, -0.31F},
         {hf - 0.03F, -0.02F},
         {-hf + 0.03F, -0.02F},
         {-hf - 0.23F, -0.31F}
      };
      b.extrude(pts, w, col, x, y, (zf + zb) / 2.0F);
   }

   static void tracks2(Models.Parts P, float length, float x) {
      MB R = P.g("root", "r");
      MB D = P.g("root", "d");
      MB M = P.g("root", "m");
      float rz = (float) (Math.PI / 2);

      for (int si = 0; si < 2; si++) {
         float X = (si == 0 ? -1 : 1) * x;
         R.box(0.64F, 0.07F, length - 0.9F, RUBBER, X, 1.02F, 0.0F);
         R.box(0.64F, 0.07F, length - 1.6F, RUBBER, X, 0.04F, 0.0F);

         for (float zz : new float[]{-1.0F, 1.0F}) {
            R.cyl(0.47F, 0.47F, 0.64F, 16, RUBBER, X, 0.53F, zz * (length / 2.0F - 0.5F), 0.0F, 0.0F, rz);
         }

         for (int i = 0; i < 6; i++) {
            float z = -length / 2.0F + 1.05F + i * (length - 2.1F) / 5.0F;
            R.cyl(0.41F, 0.41F, 0.6F, 18, RUBBER, X, 0.46F, z, 0.0F, 0.0F, rz);
            D.cyl(0.33F, 0.33F, 0.64F, 16, OLIVE_D, X, 0.46F, z, 0.0F, 0.0F, rz);
            M.cyl(0.11F, 0.13F, 0.72F, 8, STEEL, X, 0.46F, z, 0.0F, 0.0F, rz);
         }

         M.cyl(0.44F, 0.44F, 0.5F, 12, GUN, X, 0.6F, -length / 2.0F + 0.45F, 0.0F, 0.0F, rz);

         for (int i = 0; i < 12; i++) {
            float a = i * (float) (Math.PI * 2) / 12.0F;
            M.box(0.48F, 0.1F, 0.12F, GUN, X, 0.6F + MathUtils.cos(a) * 0.46F, -length / 2.0F + 0.45F + MathUtils.sin(a) * 0.46F, a, 0.0F, 0.0F);
         }

         D.cyl(0.38F, 0.38F, 0.62F, 14, OLIVE_D, X, 0.6F, length / 2.0F - 0.45F, 0.0F, 0.0F, rz);
         int n = (int)((length - 0.9F) / 0.26F);

         for (int i = 0; i < n; i++) {
            float z = -length / 2.0F + 0.45F + i * 0.26F;
            M.box(0.66F, 0.05F, 0.1F, GUN, X, 1.07F, z);
            if (Math.abs(z) < length / 2.0F - 0.75F) {
               M.box(0.66F, 0.05F, 0.1F, GUN, X, 0.0F, z);
            }
         }

         for (int ix = 0; ix < 3; ix++) {
            D.cyl(0.09F, 0.09F, 0.4F, 8, OLIVE_D, X, 0.98F, -length / 2.0F + 1.7F + ix * (length - 3.4F) / 2.0F, 0.0F, 0.0F, rz);
         }
      }
   }

   static void pantsir(Models.Parts P, Array<Vector3> barrels, Array<Vector3> noses) {
      MB C = P.g("root", "c");
      MB D = P.g("root", "d");
      MB M = P.g("root", "m");
      MB Gl = P.g("root", "g");
      MB L = P.g("root", "l");
      MB R = P.g("root", "r");

      for (float s : S2) {
         D.box(0.24F, 0.34F, 9.8F, OLIVE_D, s * 0.55F, 1.0F, 0.0F);
      }

      for (float z : new float[]{-3.5F, -2.1F, 1.5F, 2.9F}) {
         M.cyl(0.1F, 0.1F, 2.1F, 8, GUN, 0.0F, 0.62F, z, 0.0F, 0.0F, (float) (Math.PI / 2));
         D.box(0.5F, 0.42F, 0.5F, OLIVE_D, 0.0F, 0.66F, z);

         for (float s : S2) {
            wheel(P, "root", s * 1.17F, 0.62F, z, 0.62F, 0.46F, s);
         }
      }

      M.box(0.14F, 0.14F, 6.2F, GUN, 0.0F, 0.76F, -0.3F);

      for (float s : S2) {
         fender(C, -4.2F, -1.4F, s * 1.17F, 1.4F, 0.64F, WHITE);
         fender(C, 0.8F, 3.6F, s * 1.17F, 1.4F, 0.64F, WHITE);
         R.box(0.55F, 0.5F, 0.03F, RUBBER, s * 1.17F, 0.95F, 3.75F);
      }

      float cz = -3.85F;
      C.extrude(
         new float[][]{{-1.05F, 0.0F}, {1.0F, 0.0F}, {1.06F, 0.95F}, {0.96F, 1.06F}, {0.78F, 1.92F}, {0.68F, 2.02F}, {-1.05F, 2.02F}},
         2.5F,
         WHITE,
         0.0F,
         1.3F,
         cz
      );

      for (float s : S2) {
         Gl.box(1.08F, 0.78F, 0.04F, GLASS, s * 0.58F, 2.79F, -4.745F, 0.206F, 0.0F, 0.0F);
         D.box(0.62F, 0.03F, 0.03F, DARK, s * 0.55F, 2.43F, -4.86F, 0.0F, 0.0F, s * 0.35F);
         Gl.box(0.04F, 0.6F, 0.92F, GLASS, s * 1.255F, 2.74F, -4.15F);
         D.box(0.03F, 1.75F, 0.03F, DARK, s * 1.255F, 2.25F, -3.55F);
         D.box(0.03F, 0.03F, 1.4F, DARK, s * 1.255F, 1.4F, -4.2F);
         M.box(0.05F, 0.05F, 0.22F, STEEL, s * 1.27F, 2.32F, -3.75F);
         L.box(0.36F, 0.22F, 0.05F, c("fff3d0"), s * 0.92F, 1.66F, -4.93F);
         D.box(0.46F, 0.3F, 0.04F, DARK, s * 0.92F, 1.66F, -4.91F);
         L.box(0.14F, 0.09F, 0.05F, c("ff9a2e"), s * 1.12F, 1.9F, -4.92F);
         rail(M, V(s * 1.25F, 2.9F, -4.5F), V(s * 1.62F, 2.95F, -4.6F));
         rail(M, V(s * 1.25F, 2.4F, -4.5F), V(s * 1.62F, 2.55F, -4.6F));
         D.box(0.08F, 0.42F, 0.26F, DARK, s * 1.66F, 2.72F, -4.6F);
         M.box(0.36F, 0.04F, 0.42F, STEEL, s * 1.3F, 1.0F, -4.1F);
         M.box(0.36F, 0.04F, 0.42F, STEEL, s * 1.3F, 0.62F, -4.1F);
         M.box(0.08F, 0.1F, 0.18F, GUN, s * 0.75F, 1.15F, -5.15F);
      }

      Gl.box(0.04F, 0.4F, 0.9F, GLASS, 0.0F, 2.75F, -2.82F);
      D.box(1.55F, 0.6F, 0.04F, c("1d1f1a"), 0.0F, 2.0F, -4.915F);

      for (int i = 0; i < 5; i++) {
         M.box(1.5F, 0.04F, 0.05F, GUN, 0.0F, 1.78F + i * 0.11F, -4.94F);
      }

      D.box(2.62F, 0.34F, 0.3F, OLIVE_D, 0.0F, 1.3F, -5.0F);
      C.box(2.3F, 0.05F, 0.34F, WHITE, 0.0F, 3.37F, -4.68F, -0.15F, 0.0F, 0.0F);
      C.box(0.9F, 0.08F, 0.7F, WHITE, 0.0F, 3.36F, -3.6F);
      M.cyl(0.012F, 0.02F, 1.4F, 4, DARK, 0.95F, 4.0F, -3.1F);
      M.cyl(0.08F, 0.08F, 1.6F, 8, GUN, -1.05F, 2.9F, -2.62F);
      D.box(0.22F, 0.9F, 0.06F, OLIVE_D, -1.05F, 2.8F, -2.5F);
      C.extrude(new float[][]{{-3.7F, 0.0F}, {3.6F, 0.0F}, {3.7F, 0.2F}, {3.7F, 1.45F}, {-3.4F, 1.45F}, {-3.7F, 1.1F}}, 2.65F, WHITE, 0.0F, 1.48F, 1.0F);
      D.box(2.2F, 0.32F, 7.2F, OLIVE_D, 0.0F, 1.32F, 1.0F);

      for (float s : S2) {
         D.box(0.03F, 0.55F, 1.25F, c("2a2d24"), s * 1.335F, 2.3F, -1.4F);

         for (int i = 0; i < 6; i++) {
            M.box(0.05F, 0.03F, 1.2F, GUN, s * 1.35F, 2.07F + i * 0.09F, -1.4F);
         }

         D.box(0.03F, 0.9F, 0.7F, c("31352a"), s * 1.33F, 2.2F, 2.6F);
         M.box(0.05F, 0.05F, 0.16F, STEEL, s * 1.35F, 2.25F, 2.4F);
         rail(M, V(s * 1.36F, 2.85F, -2.4F), V(s * 1.36F, 2.85F, 4.3F));

         for (float z : new float[]{-2.4F, 1.0F, 4.3F}) {
            rail(M, V(s * 1.33F, 2.62F, z), V(s * 1.36F, 2.85F, z));
         }

         for (float z : new float[]{-2.45F, 4.45F}) {
            M.cyl(0.08F, 0.08F, 0.7F, 8, STEEL, s * 1.42F, 1.15F, z);
            D.box(0.42F, 0.06F, 0.42F, OLIVE_D, s * 1.42F, 0.8F, z);
            D.box(0.26F, 0.42F, 0.26F, OLIVE_D, s * 1.42F, 1.62F, z);
         }

         L.box(0.05F, 0.08F, 0.12F, c("ff9a2e"), s * 1.34F, 1.6F, 0.0F);
         L.box(0.22F, 0.12F, 0.05F, c("d0201a"), s * 1.0F, 1.55F, 4.72F);
      }

      D.cyl(0.32F, 0.32F, 1.5F, 14, OLIVE_D, 1.05F, 1.0F, -0.35F, (float) (Math.PI / 2), 0.0F, 0.0F);

      for (float z : new float[]{-0.8F, 0.1F}) {
         M.cyl(0.335F, 0.335F, 0.05F, 14, GUN, 1.05F, 1.0F, z, (float) (Math.PI / 2), 0.0F, 0.0F);
      }

      C.box(0.55F, 0.6F, 1.3F, WHITE, -1.05F, 1.0F, -0.4F);

      for (float x : new float[]{-0.35F, 0.35F}) {
         rail(M, V(x, 0.9F, 4.72F), V(x, 2.9F, 4.72F));
      }

      for (int i = 0; i < 6; i++) {
         rail(M, V(-0.35F, 1.05F + i * 0.32F, 4.72F), V(0.35F, 1.05F + i * 0.32F, 4.72F), 0.02F);
      }

      M.cyl(0.012F, 0.02F, 2.6F, 4, DARK, 1.15F, 4.2F, 4.2F);
      M.cyl(0.012F, 0.02F, 2.2F, 4, DARK, -1.15F, 4.0F, 4.3F);
      MB TC = P.g("turret", "c");
      MB TD = P.g("turret", "d");
      MB TM = P.g("turret", "m");
      MB TG = P.g("turret", "g");
      TM.cyl(1.42F, 1.42F, 0.12F, 28, GUN, 0.0F, 0.06F, 0.0F);
      TC.cyl(1.32F, 1.42F, 0.3F, 28, WHITE, 0.0F, 0.25F, 0.0F);
      TC.extrude(new float[][]{{-1.55F, 0.0F}, {1.05F, 0.0F}, {1.3F, 0.3F}, {1.15F, 1.1F}, {0.9F, 1.22F}, {-1.55F, 1.22F}}, 2.2F, WHITE, 0.0F, 0.38F, 0.25F);
      TC.cyl(0.36F, 0.36F, 0.1F, 16, WHITE, -0.5F, 1.64F, 0.7F);
      TM.box(0.06F, 0.06F, 0.4F, STEEL, -0.5F, 1.7F, 0.7F);
      TC.box(0.3F, 0.22F, 0.35F, WHITE, 0.45F, 1.7F, -0.45F);
      TG.box(0.22F, 0.12F, 0.03F, GLASS, 0.45F, 1.72F, -0.64F);

      for (float s : S2) {
         TC.box(0.24F, 0.62F, 1.5F, WHITE, s * 1.2F, 0.95F, 0.75F);
         rail(TM, V(s * 0.95F, 1.7F, 1.6F), V(s * 0.95F, 1.7F, -0.2F));
      }

      TC.box(1.6F, 0.62F, 0.5F, WHITE, 0.0F, 0.82F, 1.98F);
      TD.box(1.3F, 0.42F, 0.04F, c("23261f"), 0.0F, 0.82F, 2.24F);

      for (int i = 0; i < 4; i++) {
         TM.box(1.25F, 0.03F, 0.05F, GUN, 0.0F, 0.67F + i * 0.1F, 2.26F);
      }

      MB RC = P.g("radar", "c");
      MB RD = P.g("radar", "d");
      MB RM = P.g("radar", "m");
      RM.cyl(0.13F, 0.17F, 0.75F, 12, GUN, 0.0F, 0.37F, 0.0F);
      float cA = MathUtils.cos(0.25F);
      float sA = MathUtils.sin(0.25F);
      RC.box(2.4F, 1.25F, 0.2F, WHITE, 0.0F, 1.2F, 0.0F, 0.25F, 0.0F, 0.0F);
      RD.box(2.22F, 1.08F, 0.04F, c("2b3036"), 0.0F, 1.2F + -sA * -0.11F, cA * -0.11F, 0.25F, 0.0F, 0.0F);

      for (int i = 0; i < 7; i++) {
         RM.box(0.025F, 1.04F, 0.03F, GUN, -0.93F + i * 0.31F, 1.2F + -sA * -0.13F, cA * -0.13F, 0.25F, 0.0F, 0.0F);
      }

      RC.box(0.5F, 0.35F, 0.3F, WHITE, 0.0F, 0.75F, 0.25F);
      MB PC = P.g("pitch", "c");
      MB PD = P.g("pitch", "d");
      MB PM = P.g("pitch", "m");
      MB PG = P.g("pitch", "g");
      PM.cyl(0.16F, 0.16F, 4.5F, 12, GUN, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, (float) (Math.PI / 2));
      float rx = (float) (Math.PI / 2);
      PC.cyl(0.72F, 0.72F, 0.42F, 28, WHITE, 0.0F, 0.32F, -0.75F, rx, 0.0F, 0.0F);
      PD.cyl(0.66F, 0.66F, 0.05F, 28, c("2a2f35"), 0.0F, 0.32F, -0.98F, rx, 0.0F, 0.0F);
      PM.cyl(0.12F, 0.12F, 0.08F, 12, GUN, 0.0F, 0.32F, -1.02F, rx, 0.0F, 0.0F);
      PC.box(0.55F, 0.42F, 0.62F, WHITE, 0.0F, -0.36F, -0.85F);
      PG.box(0.32F, 0.22F, 0.03F, GLASS, 0.0F, -0.34F, -1.17F);
      Array<Array<Vector3>> sideNoses = new Array();
      sideNoses.add(new Array());
      sideNoses.add(new Array());

      for (int si = 0; si < 2; si++) {
         float s = si == 0 ? -1.0F : 1.0F;
         float px = s * 1.45F;

         for (float row : new float[]{0.17F, -0.17F}) {
            for (float col : new float[]{-0.32F, 0.0F, 0.32F}) {
               float x = px + col;
               PC.cyl(0.15F, 0.15F, 3.3F, 14, WHITE, x, 0.12F + row, -0.55F, rx, 0.0F, 0.0F);
               PD.cyl(0.165F, 0.165F, 0.1F, 14, OLIVE_D, x, 0.12F + row, -2.15F, rx, 0.0F, 0.0F);
               PD.cyl(0.165F, 0.165F, 0.1F, 14, OLIVE_D, x, 0.12F + row, 0.98F, rx, 0.0F, 0.0F);
               PD.cyl(0.13F, 0.13F, 0.02F, 14, c("1a1a1a"), x, 0.12F + row, -2.2F, rx, 0.0F, 0.0F);
               ((Array)sideNoses.get(si)).add(V(x, 0.12F + row, -2.2F));
            }
         }

         for (float z : new float[]{-1.55F, 0.35F}) {
            PC.box(1.12F, 0.76F, 0.12F, WHITE, px, 0.12F, z);
         }

         PD.box(0.3F, 0.3F, 0.9F, OLIVE_D, s * 0.75F, 0.0F, 0.0F);
         float gx = s * 2.38F;
         PC.box(0.42F, 0.62F, 1.55F, WHITE, gx, -0.1F, -0.2F);
         PD.box(0.5F, 0.3F, 0.5F, OLIVE_D, s * 2.1F, -0.05F, 0.2F);
         PD.box(0.3F, 0.5F, 0.6F, OLIVE_D, gx, -0.55F, 0.3F);

         for (float dy : new float[]{-0.14F, 0.14F}) {
            float y = -0.1F + dy;
            PM.cyl(0.058F, 0.058F, 3.25F, 10, GUN, gx, y, -2.55F, rx, 0.0F, 0.0F);
            PM.cyl(0.095F, 0.095F, 0.95F, 12, GUN, gx, y, -1.35F, rx, 0.0F, 0.0F);
            PM.cyl(0.085F, 0.085F, 0.3F, 10, GUN, gx, y, -4.2F, rx, 0.0F, 0.0F);
            PD.box(0.2F, 0.025F, 0.08F, DARK, gx, y, -4.17F);
            barrels.add(V(gx, y, -4.4F));
         }
      }

      for (int i = 0; i < 6; i++) {
         noses.add((Vector3)((Array)sideNoses.get(0)).get(i));
         noses.add((Vector3)((Array)sideNoses.get(1)).get(i));
      }
   }

   static void shilka(Models.Parts P, Array<Vector3> barrels, Array<Vector3> noses) {
      MB C = P.g("root", "c");
      MB D = P.g("root", "d");
      MB M = P.g("root", "m");
      MB Gl = P.g("root", "g");
      MB L = P.g("root", "l");
      tracks2(P, 7.0F, 1.45F);
      C.extrude(new float[][]{{-3.3F, 0.0F}, {3.0F, 0.0F}, {3.55F, 0.45F}, {2.3F, 1.28F}, {-3.1F, 1.28F}, {-3.5F, 0.7F}}, 2.3F, WHITE, 0.0F, 0.9F, 0.0F);

      for (float s : S2) {
         C.extrude(new float[][]{{-3.2F, 0.0F}, {2.9F, 0.0F}, {2.3F, 0.32F}, {-3.0F, 0.36F}}, 0.7F, WHITE, s * 1.45F, 1.8F, 0.0F, 0.0F, 0.0F, -s * 0.42F);
      }

      for (float s : S2) {
         C.box(0.78F, 0.05F, 6.9F, WHITE, s * 1.47F, 1.12F, -0.1F);
         C.box(0.04F, 0.42F, 6.6F, WHITE, s * 1.84F, 0.92F, 0.0F);
         C.box(0.5F, 0.35F, 1.2F, WHITE, s * 1.45F, 1.32F, 2.2F);
         L.box(0.2F, 0.2F, 0.06F, c("fff3d0"), s * 0.8F, 1.65F, -3.42F);
         rail(M, V(s * 0.65F, 1.55F, -3.5F), V(s * 0.95F, 1.55F, -3.5F));
         rail(M, V(s * 0.65F, 1.8F, -3.45F), V(s * 0.95F, 1.8F, -3.45F));
         M.box(0.12F, 0.12F, 0.2F, GUN, s * 0.7F, 1.0F, -3.55F);
         L.box(0.14F, 0.1F, 0.05F, c("d0201a"), s * 0.95F, 1.8F, 3.42F);
      }

      D.box(1.6F, 0.06F, 1.3F, c("2a2d24"), 0.0F, 2.17F, 2.4F);

      for (int i = 0; i < 8; i++) {
         M.box(1.5F, 0.04F, 0.05F, GUN, 0.0F, 2.2F, 1.85F + i * 0.15F);
      }

      C.cyl(0.3F, 0.3F, 0.1F, 14, WHITE, -0.6F, 2.2F, -2.2F);
      Gl.box(0.4F, 0.1F, 0.04F, GLASS, -0.6F, 2.12F, -2.55F, -0.5F, 0.0F, 0.0F);
      D.box(1.0F, 0.25F, 0.5F, DARK, 0.8F, 2.25F, 2.8F);
      M.cyl(0.1F, 0.12F, 0.6F, 8, GUN, -1.0F, 2.4F, 3.0F);
      MB TC = P.g("turret", "c");
      MB TM = P.g("turret", "m");
      MB TG = P.g("turret", "g");
      TM.cyl(1.5F, 1.5F, 0.12F, 28, GUN, 0.0F, 0.06F, 0.0F);
      TC.cyl(1.5F, 1.6F, 0.4F, 28, WHITE, 0.0F, 0.25F, 0.0F);
      TC.extrude(new float[][]{{-1.9F, 0.0F}, {1.5F, 0.0F}, {2.0F, 0.5F}, {1.3F, 1.35F}, {-1.6F, 1.35F}, {-2.0F, 0.7F}}, 2.9F, WHITE, 0.0F, 0.45F, 0.1F);

      for (float s : S2) {
         TC.extrude(new float[][]{{-1.7F, 0.0F}, {1.4F, 0.0F}, {1.1F, 0.9F}, {-1.5F, 0.9F}}, 0.3F, WHITE, s * 1.5F, 0.95F, 0.1F, 0.0F, 0.0F, s * 0.3F);
      }

      TG.box(0.5F, 0.3F, 0.04F, GLASS, 0.75F, 1.35F, -1.66F, 0.6F, 0.0F, 0.0F);
      TG.box(0.5F, 0.3F, 0.04F, GLASS, -0.75F, 1.35F, -1.66F, 0.6F, 0.0F, 0.0F);
      TC.cyl(0.33F, 0.33F, 0.1F, 16, WHITE, -0.7F, 1.85F, 0.6F);
      TC.cyl(0.33F, 0.33F, 0.1F, 16, WHITE, 0.7F, 1.85F, 0.9F);

      for (float s : S2) {
         TC.box(0.3F, 0.7F, 1.7F, WHITE, s * 1.55F, 1.0F, 0.7F);
         rail(TM, V(s * 1.2F, 1.88F, 1.9F), V(s * 1.2F, 1.88F, -0.5F));
      }

      TC.box(2.2F, 0.8F, 0.5F, WHITE, 0.0F, 1.0F, 2.2F);
      MB RC = P.g("radar", "c");
      MB RD = P.g("radar", "d");
      MB RM = P.g("radar", "m");
      RM.cyl(0.1F, 0.1F, 0.6F, 8, GUN, 0.0F, 0.25F, 0.0F);
      RC.sph(0.85F, WHITE, 0.0F, 0.85F, 0.2F, 1.0F, 1.0F, 0.35F, 18);
      RD.cyl(0.08F, 0.08F, 0.7F, 8, DARK, 0.0F, 0.85F, -0.25F, (float) (Math.PI / 2), 0.0F, 0.0F);
      MB PC = P.g("pitch", "c");
      MB PM = P.g("pitch", "m");
      MB PD = P.g("pitch", "d");

      for (float x : new float[]{-0.95F, 0.95F}) {
         PC.box(0.5F, 0.75F, 1.1F, WHITE, x, 0.0F, 0.0F);

         for (float dy : new float[]{-0.18F, 0.18F}) {
            PM.cyl(0.055F, 0.065F, 2.8F, 10, GUN, x, dy, -1.9F, (float) (Math.PI / 2), 0.0F, 0.0F);
            PM.cyl(0.085F, 0.085F, 0.7F, 10, GUN, x, dy, -0.9F, (float) (Math.PI / 2), 0.0F, 0.0F);
            PM.cyl(0.075F, 0.05F, 0.3F, 10, GUN, x, dy, -3.35F, (float) (Math.PI / 2), 0.0F, 0.0F);
            barrels.add(V(x, dy, -3.55F));
         }

         PD.box(0.3F, 0.3F, 0.6F, OLIVE_D, x, -0.5F, 0.2F);
      }
   }

   static void tor(Models.Parts P, Array<Vector3> barrels, Array<Vector3> noses) {
      MB C = P.g("root", "c");
      MB D = P.g("root", "d");
      MB M = P.g("root", "m");
      MB L = P.g("root", "l");
      tracks2(P, 7.2F, 1.5F);
      C.extrude(new float[][]{{-3.4F, 0.0F}, {3.1F, 0.0F}, {3.65F, 0.5F}, {2.4F, 1.32F}, {-3.2F, 1.32F}, {-3.6F, 0.7F}}, 2.4F, WHITE, 0.0F, 0.9F, 0.0F);

      for (float s : S2) {
         C.extrude(new float[][]{{-3.3F, 0.0F}, {3.0F, 0.0F}, {2.4F, 0.32F}, {-3.1F, 0.36F}}, 0.72F, WHITE, s * 1.5F, 1.82F, 0.0F, 0.0F, 0.0F, -s * 0.42F);
      }

      for (float s : S2) {
         C.box(0.8F, 0.05F, 7.1F, WHITE, s * 1.52F, 1.12F, -0.1F);
         C.box(0.04F, 0.42F, 6.8F, WHITE, s * 1.9F, 0.92F, 0.0F);
         C.box(0.55F, 0.4F, 1.6F, WHITE, s * 1.5F, 1.35F, 2.0F);
         L.box(0.2F, 0.2F, 0.06F, c("fff3d0"), s * 0.85F, 1.7F, -3.52F);
         M.box(0.12F, 0.12F, 0.2F, GUN, s * 0.7F, 1.0F, -3.65F);
         L.box(0.14F, 0.1F, 0.05F, c("d0201a"), s * 1.0F, 1.85F, 3.52F);
      }

      D.box(1.8F, 0.06F, 1.2F, c("2a2d24"), 0.0F, 2.22F, 2.6F);

      for (int i = 0; i < 7; i++) {
         M.box(1.7F, 0.04F, 0.05F, GUN, 0.0F, 2.25F, 2.1F + i * 0.16F);
      }

      MB TC = P.g("turret", "c");
      MB TD = P.g("turret", "d");
      MB TM = P.g("turret", "m");
      MB TG = P.g("turret", "g");
      TM.cyl(1.55F, 1.55F, 0.12F, 28, GUN, 0.0F, 0.06F, 0.0F);
      TC.cyl(1.55F, 1.7F, 0.4F, 28, WHITE, 0.0F, 0.25F, 0.0F);
      TC.extrude(new float[][]{{-1.5F, 0.0F}, {1.4F, 0.0F}, {1.85F, 0.5F}, {1.3F, 1.0F}, {-1.3F, 1.0F}, {-1.65F, 0.5F}}, 2.9F, WHITE, 0.0F, 0.4F, 0.2F);
      TC.cyl(0.55F, 0.55F, 0.35F, 24, WHITE, 0.0F, 0.9F, -1.35F, (float) (Math.PI / 2), 0.0F, 0.0F);
      TD.cyl(0.5F, 0.5F, 0.04F, 24, c("2a2f35"), 0.0F, 0.9F, -1.53F, (float) (Math.PI / 2), 0.0F, 0.0F);
      TC.box(0.4F, 0.35F, 0.4F, WHITE, 0.95F, 1.4F, -0.9F);
      TG.box(0.28F, 0.16F, 0.03F, GLASS, 0.95F, 1.42F, -1.11F);

      for (float s : S2) {
         rail(TM, V(s * 1.2F, 1.42F, 1.5F), V(s * 1.2F, 1.42F, -0.4F));
      }

      MB RC = P.g("radar", "c");
      MB RD = P.g("radar", "d");
      MB RM = P.g("radar", "m");
      RM.cyl(0.12F, 0.12F, 0.7F, 10, GUN, 0.0F, 0.3F, 0.0F);
      RC.box(2.4F, 0.9F, 0.18F, WHITE, 0.0F, 0.95F, 0.0F);
      RD.box(2.2F, 0.72F, 0.04F, c("2a2e33"), 0.0F, 0.95F, -0.11F);

      for (int i = 0; i < 7; i++) {
         RM.box(0.025F, 0.7F, 0.03F, GUN, -0.93F + i * 0.31F, 0.95F, -0.13F);
      }

      MB PC = P.g("pitch", "c");
      MB PD = P.g("pitch", "d");
      MB PM = P.g("pitch", "m");
      PC.extrude(new float[][]{{-1.4F, -0.7F}, {1.6F, -0.7F}, {1.6F, 0.9F}, {-1.4F, 0.9F}}, 2.6F, WHITE);
      PD.box(2.62F, 0.05F, 3.0F, OLIVE_D, 0.0F, 0.0F, -0.1F);

      for (int r = 0; r < 2; r++) {
         for (int cc = 0; cc < 4; cc++) {
            float x = -0.9F + cc * 0.6F;
            float y = -0.32F + r * 0.62F;
            PD.cyl(0.24F, 0.24F, 0.08F, 16, OLIVE_D, x, y, -1.6F, (float) (Math.PI / 2), 0.0F, 0.0F);
            PD.cyl(0.2F, 0.2F, 0.06F, 14, c("1a1a1a"), x, y, -1.63F, (float) (Math.PI / 2), 0.0F, 0.0F);
            noses.add(V(x, y, -1.6F));
         }
      }

      for (float s : S2) {
         PM.cyl(0.14F, 0.14F, 0.4F, 10, GUN, s * 1.4F, 0.0F, 0.0F, 0.0F, 0.0F, (float) (Math.PI / 2));
      }
   }

   static void luch(Models.Parts P, Array<Vector3> barrels, Array<Vector3> noses) {
      MB C = P.g("root", "c");
      MB D = P.g("root", "d");
      MB M = P.g("root", "m");
      MB L = P.g("root", "l");
      tracks2(P, 7.0F, 1.5F);
      C.extrude(new float[][]{{-3.3F, 0.0F}, {3.0F, 0.0F}, {3.55F, 0.45F}, {2.3F, 1.22F}, {-3.1F, 1.22F}, {-3.5F, 0.65F}}, 2.5F, WHITE, 0.0F, 0.9F, 0.0F);

      for (float s : S2) {
         C.extrude(new float[][]{{-3.2F, 0.0F}, {2.9F, 0.0F}, {2.3F, 0.3F}, {-3.0F, 0.34F}}, 0.66F, WHITE, s * 1.52F, 1.75F, 0.0F, 0.0F, 0.0F, -s * 0.42F);
      }

      for (float s : S2) {
         C.box(0.75F, 0.05F, 6.9F, WHITE, s * 1.5F, 1.12F, -0.1F);
         C.box(0.6F, 0.55F, 1.4F, WHITE, s * 1.45F, 1.45F, 2.2F);
         L.box(0.2F, 0.18F, 0.06F, c("fff3d0"), s * 0.85F, 1.65F, -3.42F);
         L.box(0.14F, 0.1F, 0.05F, c("d0201a"), s * 1.0F, 1.8F, 3.42F);

         for (int i = 0; i < 4; i++) {
            M.box(0.05F, 0.42F, 1.2F, GUN, s * (1.25F + i * 0.12F), 1.5F, 2.2F);
         }
      }

      D.box(1.9F, 0.06F, 1.4F, c("2a2d24"), 0.0F, 2.12F, 2.5F);
      MB TC = P.g("turret", "c");
      MB TD = P.g("turret", "d");
      MB TM = P.g("turret", "m");
      MB TL = P.g("turret", "l");
      TM.cyl(1.5F, 1.5F, 0.12F, 28, GUN, 0.0F, 0.06F, 0.0F);
      TC.cyl(1.45F, 1.65F, 0.45F, 28, WHITE, 0.0F, 0.27F, 0.0F);
      TC.extrude(new float[][]{{-1.4F, 0.0F}, {1.3F, 0.0F}, {1.7F, 0.42F}, {1.1F, 0.85F}, {-1.2F, 0.85F}, {-1.5F, 0.45F}}, 2.4F, WHITE, 0.0F, 0.45F, 0.4F);

      for (float s : S2) {
         TD.cyl(0.32F, 0.32F, 1.6F, 16, c("3a4048"), s * 0.75F, 1.0F, 1.5F, (float) (Math.PI / 2), 0.0F, 0.0F);

         for (int i = 0; i < 3; i++) {
            TM.cyl(0.34F, 0.34F, 0.06F, 16, GUN, s * 0.75F, 1.0F, 0.9F + i * 0.6F, (float) (Math.PI / 2), 0.0F, 0.0F);
         }

         TL.box(0.08F, 0.08F, 0.5F, c("6ff2ff"), s * 0.75F, 1.34F, 1.5F);
      }

      MB RC = P.g("radar", "c");
      MB RM = P.g("radar", "m");
      MB RD = P.g("radar", "d");
      RM.cyl(0.1F, 0.1F, 0.6F, 10, GUN, 0.0F, 0.3F, 0.0F);
      RC.cyl(0.75F, 0.55F, 0.12F, 20, WHITE, 0.0F, 0.7F, 0.0F, 1.208305F, 0.0F, 0.0F);
      RD.cyl(0.6F, 0.6F, 0.02F, 20, c("2a2e33"), 0.0F, 0.72F, -0.07F, 1.208305F, 0.0F, 0.0F);
      MB PC = P.g("pitch", "c");
      MB PD = P.g("pitch", "d");
      MB PM = P.g("pitch", "m");
      MB PL = P.g("pitch", "l");
      PC.box(1.5F, 1.1F, 1.6F, WHITE, 0.0F, 0.0F, 0.3F);
      PD.cyl(0.55F, 0.62F, 2.6F, 24, c("454b52"), 0.0F, 0.0F, -1.4F, (float) (Math.PI / 2), 0.0F, 0.0F);

      for (int i = 0; i < 7; i++) {
         PM.cyl(0.78F, 0.78F, 0.07F, 24, GUN, 0.0F, 0.0F, -0.5F - i * 0.3F, (float) (Math.PI / 2), 0.0F, 0.0F);
      }

      PM.cyl(0.66F, 0.5F, 0.35F, 24, GUN, 0.0F, 0.0F, -2.8F, (float) (Math.PI / 2), 0.0F, 0.0F);
      PL.cyl(0.42F, 0.42F, 0.05F, 24, c("8ff6ff"), 0.0F, 0.0F, -2.98F, (float) (Math.PI / 2), 0.0F, 0.0F);
      PL.cyl(0.62F, 0.62F, 0.04F, 24, c("2fb8d8"), 0.0F, 0.0F, -2.66F, (float) (Math.PI / 2), 0.0F, 0.0F);

      for (float s : S2) {
         PC.box(0.25F, 0.7F, 1.3F, WHITE, s * 0.85F, 0.0F, 0.3F);
         PM.cyl(0.14F, 0.14F, 0.4F, 10, GUN, s * 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, (float) (Math.PI / 2));
      }

      barrels.add(V(0.0F, 0.0F, -3.05F));
   }

   static void grom(Models.Parts P, Array<Vector3> barrels, Array<Vector3> noses) {
      MB C = P.g("root", "c");
      MB D = P.g("root", "d");
      MB M = P.g("root", "m");
      MB L = P.g("root", "l");
      MB Gl = P.g("root", "g");

      for (float s : S2) {
         D.box(0.24F, 0.34F, 8.2F, OLIVE_D, s * 0.55F, 0.9F, 0.0F);
      }

      for (float z : new float[]{-2.7F, -0.5F, 2.3F}) {
         M.cyl(0.1F, 0.1F, 2.2F, 8, GUN, 0.0F, 0.66F, z, 0.0F, 0.0F, (float) (Math.PI / 2));

         for (float s : S2) {
            wheel(P, "root", s * 1.28F, 0.68F, z, 0.68F, 0.52F, s);
         }
      }

      C.extrude(new float[][]{{-4.2F, 0.0F}, {3.7F, 0.0F}, {4.5F, 0.55F}, {3.3F, 1.55F}, {-3.7F, 1.6F}, {-4.4F, 0.85F}}, 2.4F, WHITE, 0.0F, 0.95F, 0.0F);

      for (float s : S2) {
         C.extrude(new float[][]{{-4.0F, 0.0F}, {3.6F, 0.0F}, {3.9F, 0.35F}, {-3.8F, 0.4F}}, 0.55F, WHITE, s * 1.35F, 1.55F, 0.0F, 0.0F, 0.0F, -s * 0.35F);

         for (float z : new float[]{-2.7F, -0.5F, 2.3F}) {
            fender(C, z - 0.85F, z + 0.85F, s * 1.32F, 1.5F, 0.62F, WHITE);
         }
      }

      for (float x : new float[]{-0.55F, 0.55F}) {
         Gl.box(0.9F, 0.5F, 0.04F, GLASS, x, 2.05F, -3.83F, -0.72F, 0.0F, 0.0F);
      }

      D.box(2.2F, 0.08F, 0.1F, DARK, 0.0F, 2.36F, -3.6F, -0.72F, 0.0F, 0.0F);

      for (float s : S2) {
         Gl.box(0.03F, 0.4F, 0.8F, GLASS, s * 1.205F, 2.15F, -3.0F);
         L.box(0.14F, 0.1F, 0.05F, c("d0201a"), s * 0.95F, 1.9F, 4.18F);
      }

      MB TC = P.g("turret", "c");
      MB TD = P.g("turret", "d");
      MB TM = P.g("turret", "m");
      MB TG = P.g("turret", "g");
      TM.cyl(1.5F, 1.5F, 0.12F, 32, GUN, 0.0F, 0.06F, 0.0F);
      TC.cyl(1.45F, 1.6F, 0.3F, 32, WHITE, 0.0F, 0.2F, 0.1F);
      TC.extrude(new float[][]{{-1.7F, 0.0F}, {1.5F, 0.0F}, {2.1F, 0.5F}, {1.55F, 1.15F}, {-1.3F, 1.2F}, {-1.8F, 0.6F}}, 2.7F, WHITE, 0.0F, 0.32F, 0.3F);
      TC.box(0.55F, 0.5F, 0.75F, WHITE, -0.95F, 1.7F, 0.6F);
      TG.box(0.4F, 0.22F, 0.03F, GLASS, -0.95F, 1.75F, 0.22F);
      TD.cyl(0.3F, 0.3F, 0.9F, 16, c("2f342c"), 1.15F, 1.55F, 0.7F, 0.0F, 0.0F, (float) (Math.PI / 2));
      TD.box(1.0F, 0.5F, 1.2F, OLIVE_D, 0.0F, 0.95F, 2.05F);

      for (int i = 0; i < 5; i++) {
         TM.box(1.0F, 0.03F, 0.05F, GUN, 0.0F, 1.21F, 1.6F + i * 0.2F);
      }

      MB RC = P.g("radar", "c");
      MB RM = P.g("radar", "m");
      MB RD = P.g("radar", "d");
      RM.cyl(0.1F, 0.1F, 0.6F, 10, GUN, 0.0F, 0.3F, 0.0F);
      RC.box(1.6F, 0.6F, 0.14F, WHITE, 0.0F, 0.8F, 0.0F);
      RD.box(1.45F, 0.48F, 0.04F, c("2a2e33"), 0.0F, 0.8F, -0.09F);
      MB PC = P.g("pitch", "c");
      MB PD = P.g("pitch", "d");
      MB PM = P.g("pitch", "m");
      PC.cyl(0.62F, 0.62F, 1.9F, 24, WHITE, 0.0F, 0.0F, 0.1F, 0.0F, 0.0F, (float) (Math.PI / 2));
      PC.extrude(new float[][]{{-0.6F, -0.55F}, {0.7F, -0.45F}, {0.9F, 0.0F}, {0.7F, 0.45F}, {-0.6F, 0.55F}}, 1.5F, WHITE, 0.0F, 0.0F, -0.5F);

      for (float x : new float[]{-0.45F, 0.45F}) {
         PD.cyl(0.2F, 0.24F, 1.2F, 16, c("3a3f36"), x, 0.05F, -1.4F, (float) (Math.PI / 2), 0.0F, 0.0F);
         PM.cyl(0.11F, 0.11F, 3.6F, 16, GUN, x, 0.05F, -3.4F, (float) (Math.PI / 2), 0.0F, 0.0F);

         for (int k = 0; k < 4; k++) {
            PM.cyl(0.125F, 0.125F, 0.06F, 14, GUN, x, 0.05F, -2.2F - k * 0.6F, (float) (Math.PI / 2), 0.0F, 0.0F);
         }

         PM.cyl(0.17F, 0.17F, 0.5F, 16, GUN, x, 0.05F, -5.2F, (float) (Math.PI / 2), 0.0F, 0.0F);

         for (int k = 0; k < 3; k++) {
            PM.box(0.36F, 0.05F, 0.08F, c("1a1a1a"), x, 0.05F, -5.05F - k * 0.14F);
         }

         barrels.add(V(x, 0.05F, -5.5F));
      }
   }

   static void bolts(MB M, Vector3 a, Vector3 b, int n, float ax, float ay, float az, float r) {
      for (int i = 0; i < n; i++) {
         float k = (float)i / Math.max(1, n - 1);
         M.cyl(r, r, 0.04F, 6, RIVET, MathUtils.lerp(a.x, b.x, k), MathUtils.lerp(a.y, b.y, k), MathUtils.lerp(a.z, b.z, k), ax, ay, az);
      }
   }

   static void hatch(MB C, MB M, Vector3 p, float r) {
      C.cyl(r, r * 1.04F, 0.09F, 18, WHITE, p.x, p.y, p.z);
      M.cyl(r * 1.1F, r * 1.1F, 0.05F, 18, GUN, p.x, p.y - 0.03F, p.z);
      rail(M, V(p.x - r * 0.4F, p.y + 0.09F, p.z), V(p.x + r * 0.4F, p.y + 0.09F, p.z));
      M.box(0.1F, 0.07F, 0.14F, GUN, p.x, p.y + 0.03F, p.z + r * 0.95F);
   }

   static void grille(MB M, MB D, Vector3 c, float w, float d, int n) {
      D.box(w + 0.08F, 0.04F, d + 0.08F, DARK, c.x, c.y, c.z);

      for (int i = 0; i < n; i++) {
         M.box(w, 0.04F, 0.045F, GUN, c.x, c.y + 0.035F, c.z - d / 2.0F + (i + 0.5F) * d / n, 0.5F, 0.0F, 0.0F);
      }
   }

   static void smoke(MB M, MB D, Vector3 pos, float side, int n) {
      D.box(0.36F, 0.22F, 0.42F, OLIVE_D, pos.x, pos.y, pos.z);

      for (int i = 0; i < n; i++) {
         float px = pos.x + side * 0.02F;
         float py = pos.y + 0.14F + i % 2 * 0.05F;
         float pz = pos.z - 0.12F + i * 0.12F;
         M.cyl(0.055F, 0.055F, 0.34F, 8, GUN, px + side * 0.1F, py + 0.06F, pz - 0.08F, -0.7F, 0.0F, -side * 0.6F);
         M.cyl(0.035F, 0.035F, 0.35F, 6, c("111111"), px + side * 0.11F, py + 0.07F, pz - 0.09F, -0.7F, 0.0F, -side * 0.6F);
      }
   }

   static void antenna(MB M, Vector3 pos, float h, Vector3 tilt) {
      M.cyl(0.07F, 0.09F, 0.14F, 8, GUN, pos.x, pos.y, pos.z);
      M.cyl(0.045F, 0.045F, 0.22F, 6, c("222222"), pos.x, pos.y + 0.17F, pos.z);
      Vector3 top = V(pos.x, pos.y + h, pos.z).add(tilt);
      rail(M, V(pos.x, pos.y + 0.27F, pos.z), top, 0.014F);
      M.sph(0.03F, c("222222"), top);
   }

   static void toolbox(MB D, MB M, Vector3 p, Vector3 s) {
      D.box(s.x, s.y, s.z, OLIVE_D, p.x, p.y, p.z);
      D.box(s.x + 0.03F, 0.04F, s.z + 0.03F, c("343a2a"), p.x, p.y + s.y * 0.32F, p.z);

      for (float k : new float[]{-0.3F, 0.3F}) {
         M.box(0.06F, 0.08F, 0.04F, STEEL, p.x + s.x * k, p.y + s.y * 0.15F, p.z - s.z / 2.0F - 0.02F);
      }
   }

   static void jerry(MB D, MB M, Vector3 p) {
      D.box(0.16F, 0.46F, 0.34F, c("4a5236"), p.x, p.y, p.z);
      D.box(0.17F, 0.05F, 0.3F, c("3c4430"), p.x, p.y + 0.08F, p.z);
      M.box(0.06F, 0.06F, 0.12F, STEEL, p.x, p.y + 0.26F, p.z - 0.08F);
   }

   static void periscope(MB M, MB Gl, Vector3 p) {
      M.box(0.2F, 0.14F, 0.16F, GUN, p.x, p.y, p.z);
      Gl.box(0.16F, 0.08F, 0.02F, GLASS, p.x, p.y, p.z - 0.085F);
   }

   static void lampGuard(MB L, MB M, Vector3 p) {
      M.cyl(0.13F, 0.13F, 0.12F, 12, GUN, p.x, p.y, p.z + 0.05F, (float) (Math.PI / 2), 0.0F, 0.0F);
      L.cyl(0.1F, 0.1F, 0.03F, 12, c("fff3d0"), p.x, p.y, p.z - 0.02F, (float) (Math.PI / 2), 0.0F, 0.0F);

      for (float dx : new float[]{-0.07F, 0.0F, 0.07F}) {
         rail(M, V(p.x + dx, p.y + 0.14F, p.z - 0.06F), V(p.x + dx, p.y - 0.14F, p.z - 0.06F), 0.012F);
      }
   }

   static void cable(MB M, Vector3 a, Vector3 b) {
      rail(M, a, b, 0.035F);
      M.cyl(0.09F, 0.09F, 0.05F, 10, GUN, a.x, a.y, a.z, 0.0F, 0.0F, (float) (Math.PI / 2));
      M.cyl(0.09F, 0.09F, 0.05F, 10, GUN, b.x, b.y, b.z, 0.0F, 0.0F, (float) (Math.PI / 2));

      for (float k : new float[]{0.25F, 0.5F, 0.75F}) {
         M.box(0.05F, 0.1F, 0.06F, STEEL, a.cpy().lerp(b, k));
      }
   }

   static void exhaust(MB M, Vector3 p) {
      M.cyl(0.11F, 0.12F, 0.5F, 10, c("2b2b2b"), p.x, p.y, p.z, (float) (Math.PI / 2), 0.0F, 0.0F);
      M.cyl(0.13F, 0.13F, 0.08F, 10, c("4a3a2e"), p.x, p.y, p.z + 0.25F, (float) (Math.PI / 2), 0.0F, 0.0F);
   }

   static void number(MB D, String txt, Vector3 pos, float side, float h) {
      float w = h * 0.55F;
      float t = h * 0.14F;

      for (int i = 0; i < txt.length(); i++) {
         float ox = pos.x;
         float oy = pos.y;
         float oz = pos.z - side * (i - (txt.length() - 1) / 2.0F) * (w + t * 1.6F);
         String sg = SEG.get(txt.charAt(i));
         if (sg != null) {
            for (int k = 0; k < sg.length(); k++) {
               char ch = sg.charAt(k);
               float qx;
               float qy;
               boolean hor;
               switch (ch) {
                  case 'a':
                     qx = 0.0F;
                     qy = h / 2.0F;
                     hor = true;
                     break;
                  case 'b':
                     qx = 1.0F;
                     qy = h / 4.0F;
                     hor = false;
                     break;
                  case 'c':
                  default:
                     qx = 1.0F;
                     qy = -h / 4.0F;
                     hor = false;
                     break;
                  case 'd':
                     qx = 0.0F;
                     qy = -h / 2.0F;
                     hor = true;
                     break;
                  case 'e':
                     qx = -1.0F;
                     qy = -h / 4.0F;
                     hor = false;
                     break;
                  case 'f':
                     qx = -1.0F;
                     qy = h / 4.0F;
                     hor = false;
                     break;
                  case 'g':
                     qx = 0.0F;
                     qy = 0.0F;
                     hor = true;
               }

               if (hor) {
                  D.box(0.012F, t, w, MARK, ox + side * 0.006F, oy + qy, oz);
               } else {
                  D.box(0.012F, h / 2.0F, t, MARK, ox + side * 0.006F, oy + qy, oz + side * -qx * w / 2.0F);
               }
            }
         }
      }
   }

   static void sensor(MB D, MB Gl, Vector3 p, float r) {
      D.sph(r, c("2c3029"), p.x, p.y, p.z, 1.0F, 1.0F, 1.0F, 12);
      Gl.cyl(r * 0.45F, r * 0.45F, 0.04F, 12, c("2a3a56"), p.x, p.y, p.z - r * 0.95F, (float) (Math.PI / 2), 0.0F, 0.0F);
      D.box(r * 1.6F, 0.06F, 0.08F, DARK, p.x, p.y - r * 0.2F, p.z - r * 0.7F);
   }

   static void reflectors(MB L, Vector3 p, Color col) {
      L.box(0.1F, 0.06F, 0.02F, col, p.x, p.y, p.z);
   }

   static void skirt(MB C, MB M, float x, float z0, float z1, float y, float side) {
      int n = 5;
      float L = (z1 - z0) / n;

      for (int i = 0; i < n; i++) {
         float zc = z0 + (i + 0.5F) * L;
         C.box(0.035F, 0.5F, L - 0.05F, WHITE, x, y, zc);
         bolts(M, V(x + side * 0.02F, y + 0.17F, zc - L * 0.35F), V(x + side * 0.02F, y + 0.17F, zc + L * 0.35F), 3, 0.0F, 0.0F, (float) (Math.PI / 2), 0.03F);
      }

      M.box(0.04F, 0.06F, z1 - z0, GUN, x, y + 0.26F, (z0 + z1) / 2.0F);
   }

   static void detail(String kind, Models.Parts P) {
      MB C = P.g("root", "c");
      MB D = P.g("root", "d");
      MB M = P.g("root", "m");
      MB L = P.g("root", "l");
      MB Gl = P.g("root", "g");
      MB TC = P.g("turret", "c");
      MB TD = P.g("turret", "d");
      MB TM = P.g("turret", "m");
      MB TG = P.g("turret", "g");
      switch (kind) {
         case "shilka":
            for (float s : S2) {
               skirt(C, M, s * 1.87F, -3.2F, 3.1F, 0.82F, s);
               toolbox(D, M, V(s * 1.45F, 1.32F, -0.9F), V(0.62F, 0.32F, 1.1F));
               toolbox(D, M, V(s * 1.45F, 1.3F, 0.6F), V(0.62F, 0.28F, 0.7F));
               cable(M, V(s * 1.875F, 1.25F, -2.6F), V(s * 1.875F, 1.25F, 1.6F));
               exhaust(M, V(s * 1.2F, 1.9F, 3.45F));
               jerry(D, M, V(s * 1.2F, 2.45F, 3.25F));
               reflectors(L, V(s * 1.5F, 1.45F, 3.43F), c("ff6a00"));
               lampGuard(L, M, V(s * 0.8F, 1.65F, -3.45F));
               number(D, "217", V(s * 1.89F, 1.25F, 0.2F), s, 0.3F);
               smoke(TM, TD, V(s * 1.6F, 1.5F, -0.9F), s, 3);
               antenna(TM, V(s * 1.0F, 1.82F, 2.2F), 2.4F, V(0.0F, 0.0F, 0.5F));
               TC.box(0.06F, 0.55F, 1.5F, WHITE, s * 1.73F, 1.0F, 0.7F);
               bolts(TM, V(s * 1.76F, 1.25F, 0.1F), V(s * 1.76F, 1.25F, 1.3F), 5, 0.0F, 0.0F, (float) (Math.PI / 2), 0.035F);
            }

            for (int i = 0; i < 3; i++) {
               periscope(M, Gl, V(-0.6F + i * 0.55F, 2.25F, -2.9F));
            }

            hatch(C, M, V(0.7F, 2.18F, -2.2F), 0.28F);
            grille(M, D, V(0.0F, 2.18F, 1.3F), 1.4F, 0.7F, 6);
            sensor(TD, TG, V(-1.2F, 1.95F, -0.4F), 0.22F);
            bolts(TM, V(-1.2F, 1.82F, -1.6F), V(1.2F, 1.82F, -1.6F), 9, 0.0F, 0.0F, 0.0F, 0.035F);
            break;
         case "tor":
            for (float s : S2) {
               skirt(C, M, s * 1.93F, -3.3F, 3.2F, 0.82F, s);
               toolbox(D, M, V(s * 1.5F, 1.32F, -1.2F), V(0.62F, 0.32F, 1.2F));
               cable(M, V(s * 1.935F, 1.25F, -2.8F), V(s * 1.935F, 1.25F, 1.8F));
               exhaust(M, V(s * 1.25F, 1.95F, 3.55F));
               jerry(D, M, V(s * 1.55F, 1.42F, 0.6F));
               lampGuard(L, M, V(s * 0.85F, 1.7F, -3.55F));
               reflectors(L, V(s * 1.5F, 1.5F, 3.53F), c("ff6a00"));
               number(D, "304", V(s * 1.96F, 1.2F, 0.0F), s, 0.3F);
               smoke(TM, TD, V(s * 1.55F, 1.05F, -1.1F), s, 4);
               antenna(TM, V(s * 1.15F, 1.37F, 1.6F), 2.6F, V(0.0F, 0.0F, 0.4F));
               bolts(TM, V(s * 1.46F, 0.75F, -1.2F), V(s * 1.46F, 0.75F, 1.4F), 7, 0.0F, 0.0F, (float) (Math.PI / 2), 0.035F);
            }

            for (int i = 0; i < 3; i++) {
               periscope(M, Gl, V(-0.6F + i * 0.55F, 2.25F, -3.0F));
            }

            hatch(C, M, V(-0.7F, 2.22F, -2.3F), 0.3F);
            grille(M, D, V(0.0F, 2.24F, 1.2F), 1.5F, 0.9F, 7);
            hatch(TC, TM, V(-0.5F, 1.37F, 0.6F), 0.3F);
            sensor(TD, TG, V(0.95F, 1.65F, -0.9F), 0.2F);
            break;
         case "luch":
            for (float s : S2) {
               skirt(C, M, s * 1.83F, -3.1F, 3.0F, 0.82F, s);
               toolbox(D, M, V(s * 1.5F, 1.32F, -1.0F), V(0.62F, 0.32F, 1.3F));
               cable(M, V(s * 1.835F, 1.25F, -2.6F), V(s * 1.835F, 1.25F, 1.4F));
               lampGuard(L, M, V(s * 0.85F, 1.65F, -3.45F));
               reflectors(L, V(s * 1.5F, 1.45F, 3.43F), c("ff6a00"));
               number(D, "51", V(s * 1.85F, 1.2F, 0.0F), s, 0.3F);
               smoke(TM, TD, V(s * 1.3F, 0.85F, -0.8F), s, 3);
               rail(TM, V(s * 0.75F, 1.0F, 0.7F), V(s * 0.6F, 1.25F, -0.3F), 0.05F);

               for (int k = 0; k < 4; k++) {
                  TD.box(0.04F, 0.1F, 0.22F, k % 2 == 0 ? HAZ : c("1a1a1a"), s * 1.31F, 0.75F, -0.9F + k * 0.22F);
               }

               antenna(TM, V(s * 1.0F, 1.25F, 1.9F), 2.2F, V(0.0F, 0.0F, 0.0F));
            }

            for (int i = 0; i < 3; i++) {
               periscope(M, Gl, V(-0.6F + i * 0.55F, 2.15F, -2.9F));
            }

            hatch(C, M, V(0.7F, 2.12F, -2.2F), 0.28F);
            grille(M, D, V(0.0F, 2.15F, 1.2F), 1.5F, 0.8F, 7);
            sensor(TD, TG, V(-0.9F, 1.55F, -0.7F), 0.22F);
            MB PD = P.g("pitch", "d");
            MB PL = P.g("pitch", "l");

            for (int k = 0; k < 6; k++) {
               PD.box(1.52F, 0.06F, 0.1F, k % 2 == 0 ? HAZ : c("1a1a1a"), 0.0F, 0.56F, -0.1F + k * 0.1F);
            }

            for (int i = 0; i < 8; i++) {
               float a = i * (float) (Math.PI * 2) / 8.0F;
               PL.box(0.05F, 0.05F, 0.05F, c("6ff2ff"), MathUtils.cos(a) * 0.5F, MathUtils.sin(a) * 0.5F, -2.95F);
            }
            break;
         case "grom":
            for (float s : S2) {
               toolbox(D, M, V(s * 0.85F, 2.7F, 2.2F), V(0.5F, 0.3F, 1.3F));
               cable(M, V(s * 1.5F, 1.75F, -1.8F), V(s * 1.5F, 1.75F, 2.8F));
               exhaust(M, V(s * 0.8F, 2.3F, 4.15F));
               lampGuard(L, M, V(s * 0.85F, 1.6F, -4.4F));
               reflectors(L, V(s * 1.1F, 1.3F, 4.18F), c("ff6a00"));
               number(D, "88", V(s * 1.66F, 1.75F, 0.8F), s, 0.34F);
               rail(M, V(s * 1.0F, 2.55F, -3.2F), V(s * 1.45F, 2.65F, -3.25F), 0.02F);
               D.box(0.06F, 0.3F, 0.2F, DARK, s * 1.5F, 2.6F, -3.25F);
               TD.box(0.38F, 0.6F, 1.4F, OLIVE_D, s * 1.62F, 0.75F, 0.6F);

               for (int k = 0; k < 3; k++) {
                  TM.box(0.4F, 0.04F, 0.04F, STEEL, s * 1.62F, 0.6F + k * 0.15F, -0.11F);
               }

               smoke(TM, TD, V(s * 1.5F, 1.2F, -1.1F), s, 3);
               antenna(TM, V(s * 1.2F, 1.52F, 1.8F), 2.5F, V(0.0F, 0.0F, 0.5F));
            }

            L.box(0.18F, 0.1F, 0.18F, AMBER, -0.6F, 2.6F, -2.8F);
            L.box(0.18F, 0.1F, 0.18F, AMBER, 0.6F, 2.6F, -2.8F);
            MB R = P.g("root", "r");
            R.cyl(0.62F, 0.62F, 0.42F, 22, RUBBER, 0.0F, 1.9F, 4.38F, (float) (Math.PI / 2), 0.0F, 0.0F);
            D.cyl(0.38F, 0.38F, 0.44F, 16, OLIVE_D, 0.0F, 1.9F, 4.38F, (float) (Math.PI / 2), 0.0F, 0.0F);
            hatch(C, M, V(-0.6F, 2.58F, -2.2F), 0.3F);
            hatch(C, M, V(0.6F, 2.58F, -2.2F), 0.3F);
            grille(M, D, V(0.0F, 2.57F, 3.0F), 1.6F, 1.0F, 8);

            for (int i = 0; i < 3; i++) {
               periscope(M, Gl, V(-0.6F + i * 0.6F, 2.62F, -2.65F));
            }

            sensor(TD, TG, V(-1.05F, 2.15F, 0.2F), 0.2F);
            break;
         case "pantsir":
            for (float s : S2) {
               number(D, "112", V(s * 1.31F, 2.3F, -3.2F), s, 0.28F);
               smoke(TM, TD, V(s * 1.55F, 0.9F, -0.9F), s, 3);
               reflectors(L, V(s * 1.2F, 1.3F, 4.7F), c("ff6a00"));
            }
      }
   }

   public static Models.Vehicle vehicle(String kind, String skin) {
      String key = kind + "|" + skin;
      Models.Vehicle t = vehCache.get(key);
      if (t == null) {
         t = buildVehicle(kind, skin);
         vehCache.put(key, t);
      }

      Models.Vehicle v = new Models.Vehicle();
      v.root = t.root.cloneTree();
      v.turret = v.root.find("Turret");
      v.pitch = v.root.find("Pitch");
      v.radar = v.root.find("Radar");
      v.barrels.addAll(t.barrels);
      v.noses.addAll(t.noses);
      ArrayIterator var5 = v.pitch.kids.iterator();

      while (var5.hasNext()) {
         Node3 k = (Node3)var5.next();
         if (k.mesh == noseMesh && noseMesh != null) {
            v.noseMeshes.add(k);
         }
      }

      return v;
   }

   static Models.Vehicle buildVehicle(String kind, String skin) {
      MB.hq = true;
      Mat cm = camoMat(skin);
      Models.Vehicle v = new Models.Vehicle();
      Node3 root = new Node3();
      Node3 turret = new Node3();
      Node3 pitch = new Node3();
      Node3 radar = new Node3();
      Map<String, Node3> nodes = new HashMap<>();
      nodes.put("root", root);
      nodes.put("turret", turret);
      nodes.put("pitch", pitch);
      nodes.put("radar", radar);
      Models.Parts P = new Models.Parts();
      float noseOff = -0.2F;
      float ty;
      float tz;
      switch (kind) {
         case "pantsir":
            pantsir(P, v.barrels, v.noses);
            ty = 3.0F;
            tz = 0.6F;
            radar.pos.set(0.0F, 1.6F, 0.85F);
            pitch.pos.set(0.0F, 0.95F, -0.55F);
            noseOff = 0.0F;
            break;
         case "shilka":
            shilka(P, v.barrels, v.noses);
            ty = 2.15F;
            tz = 0.3F;
            radar.pos.set(0.0F, 1.85F, 1.3F);
            pitch.pos.set(0.0F, 1.0F, -0.9F);
            break;
         case "grom":
            grom(P, v.barrels, v.noses);
            ty = 2.5F;
            tz = 0.4F;
            radar.pos.set(0.0F, 1.5F, 1.2F);
            pitch.pos.set(0.0F, 0.95F, -0.9F);
            break;
         case "luch":
            luch(P, v.barrels, v.noses);
            ty = 2.15F;
            tz = 0.3F;
            radar.pos.set(0.0F, 1.0F, 1.9F);
            pitch.pos.set(0.0F, 1.2F, -0.3F);
            break;
         default:
            tor(P, v.barrels, v.noses);
            ty = 2.2F;
            tz = 0.4F;
            radar.pos.set(0.0F, 1.35F, 1.4F);
            pitch.pos.set(0.0F, 1.35F, -0.2F);
      }

      detail(kind, P);
      Map<String, Mat> mats = new HashMap<>();
      mats.put("c", cm);
      mats.put("d", MAT_PAINT);
      mats.put("m", MAT_METAL);
      mats.put("r", MAT_RUBBER);
      mats.put("g", MAT_GLASS);
      mats.put("l", MAT_LAMP);

      for (Entry<String, Map<String, MB>> e : P.d.entrySet()) {
         for (Entry<String, MB> k : e.getValue().entrySet()) {
            MB b = k.getValue();
            String key = k.getKey();
            b.noAo = key.equals("l") || key.equals("g");
            if (!b.empty()) {
               Node3 mi = new Node3(b.mesh(), mats.get(key));
               mi.castShadow = key.equals("c") || key.equals("d") || key.equals("r") || key.equals("m");
               nodes.get(e.getKey()).add(mi);
            }
         }
      }

      turret.pos.set(0.0F, ty, tz);
      turret.name = "Turret";
      pitch.name = "Pitch";
      radar.name = "Radar";
      turret.add(radar);
      turret.add(pitch);
      root.add(turret);
      if (noseMesh == null) {
         MB nb = new MB();
         MB.hq = false;
         nb.cyl(0.0F, 0.14F, 0.36F, 12, Color.WHITE);
         noseMesh = nb.mesh();
         MB.hq = true;
      }

      ArrayIterator var23 = v.noses.iterator();

      while (var23.hasNext()) {
         Vector3 p = (Vector3)var23.next();
         Node3 m = new Node3(noseMesh, MAT_NOSE);
         m.pos.set(p.x, p.y, p.z + noseOff);
         m.setEuler((float) (-Math.PI / 2), 0.0F, 0.0F);
         pitch.add(m);
         v.noseMeshes.add(m);
      }

      MB.hq = false;
      v.root = root;
      v.turret = turret;
      v.pitch = pitch;
      v.radar = radar;
      return v;
   }

   static boolean isBossType(String t) {
      return G.D.get("BOSS_INFO").has(t);
   }

   static void quadBody(MB b, MB gl, MB rot, Color A, int arms, Color body, float s) {
      b.box(0.95F * s, 0.38F * s, 1.05F * s, body);
      b.sph(0.38F * s, GREY, 0.0F, 0.18F * s, 0.0F, 1.1F, 0.5F, 1.3F);
      b.box(0.5F * s, 0.06F * s, 0.7F * s, A, 0.0F, 0.22F * s, 0.0F);

      for (int i = 0; i < arms; i++) {
         float a = (float)i / arms * (float) (Math.PI * 2) + (float) Math.PI / arms;
         float x = MathUtils.cos(a) * 1.35F * s;
         float z = MathUtils.sin(a) * 1.35F * s;
         b.box(1.35F * s, 0.09F * s, 0.14F * s, DGREY, x / 2.0F, 0.02F, z / 2.0F, 0.0F, -a, 0.0F);
         b.cyl(0.13F * s, 0.15F * s, 0.28F * s, 8, c("1b1b1b"), x, 0.1F * s, z);
         rot.disc(0.78F * s, Color.WHITE, x, 0.27F * s, z);
      }

      gl.sph(0.12F * s, new Color(1.0F, 0.2F, 0.15F, 1.0F), 0.0F, -0.05F, -0.55F * s, 1.0F, 1.0F, 1.0F, 6);
      gl.sph(0.12F * s, new Color(0.2F, 1.0F, 0.4F, 1.0F), 0.0F, -0.05F, 0.55F * s, 1.0F, 1.0F, 1.0F, 6);
   }

   static Models.EnemyDef def(String type, String acc) {
      String key = type + acc;
      Models.EnemyDef ed = enemyCache.get(key);
      if (ed != null) {
         return ed;
      } else {
         MB.hq = isBossType(type);
         Color A = acc.equals("manti") ? c("9b59b6") : c("c0392b");
         MB b = new MB();
         MB gl = new MB();
         MB rot = new MB();
         gl.noAo = true;
         rot.noAo = true;
         float r = 2.3F;
         float sc = 1.0F;
         switch (type) {
            case "quad":
            case "decoy":
               quadBody(b, gl, rot, A, 4, type.equals("decoy") ? c("6b7078") : DGREY, 1.0F);
               r = 2.3F;
               sc = 1.35F;
               break;
            case "mini":
               quadBody(b, gl, rot, A, 4, DGREY, 0.7F);
               r = 1.5F;
               sc = 1.2F;
               break;
            case "stealth":
               b.extrude(
                  new float[][]{{-1.5F, 0.0F}, {0.4F, 1.6F}, {0.9F, 0.0F}, {0.4F, -1.6F}},
                  0.28F,
                  c("1a1d24"),
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  0.0F,
                  (float) (Math.PI / 2)
               );
               b.sph(0.45F, c("252a33"), 0.0F, 0.12F, -0.1F, 1.0F, 0.45F, 1.6F);

               for (float x : S2) {
                  rot.disc(0.5F, Color.WHITE, x * 0.95F, 0.12F, 0.35F);
               }

               gl.sph(0.09F, new Color(0.55F, 0.4F, 1.0F, 1.0F), 0.0F, -0.05F, -1.2F, 1.0F, 1.0F, 1.0F, 6);
               r = 2.0F;
               sc = 1.5F;
               break;
            case "armor":
               b.sph(1.0F, c("4a5560"), 0.0F, 0.15F, 0.0F, 1.25F, 0.55F, 1.45F, 12);

               for (int i = 0; i < 6; i++) {
                  float a = i / 6.0F * (float) (Math.PI * 2);
                  float x = MathUtils.cos(a) * 1.55F;
                  float z = MathUtils.sin(a) * 1.55F;
                  b.box(1.5F, 0.14F, 0.22F, c("3a4047"), x / 2.0F, 0.05F, z / 2.0F, 0.0F, -a, 0.0F);
                  b.cyl(0.62F, 0.62F, 0.22F, 14, c("5b6670"), x, 0.12F, z);
                  rot.disc(0.5F, Color.WHITE, x, 0.26F, z);
               }

               for (int k = 0; k < 5; k++) {
                  b.box(0.5F, 0.08F, 0.32F, c("6d7a85"), -0.55F + k * 0.27F, 0.68F, -0.2F + k % 2 * 0.25F, 0.2F, 0.0F, 0.0F);
               }

               gl.sph(0.14F, new Color(1.0F, 0.55F, 0.1F, 1.0F), 0.0F, 0.0F, -1.35F, 1.0F, 1.0F, 1.0F, 6);
               r = 2.3F;
               sc = 1.6F;
               break;
            case "zig":
               quadBody(b, gl, rot, A, 3, c("3a3420"), 1.0F);
               b.box(0.6F, 0.07F, 0.6F, c("e0b030"), 0.0F, 0.24F, 0.0F);
               r = 2.2F;
               sc = 1.35F;
               break;
            case "split":
               quadBody(b, gl, rot, A, 4, DGREY, 1.25F);

               for (float x : new float[]{-0.45F, 0.0F, 0.45F}) {
                  b.box(0.32F, 0.3F, 0.32F, c("8a5a2a"), x, -0.5F, 0.0F);
               }

               r = 2.8F;
               sc = 1.4F;
               break;
            case "heavy":
               quadBody(b, gl, rot, A, 6, c("24272c"), 1.1F);
               b.box(0.7F, 0.45F, 1.3F, c("1c1c1c"), 0.0F, -0.55F, 0.0F);
               r = 4.3F;
               sc = 2.3F;
               break;
            case "relay":
               quadBody(b, gl, rot, A, 4, c("24302a"), 1.1F);
               b.cyl(0.04F, 0.04F, 1.6F, 6, c("999999"), 0.0F, 1.0F, 0.0F);
               gl.torus(0.5F, 0.06F, c("2ecc71"), 0.0F, 1.5F, 0.0F);
               r = 3.2F;
               sc = 1.6F;
               break;
            case "wing":
               b.wing(4.6F, 3.2F, 0.18F, LGREY);
               b.cyl(0.24F, 0.3F, 3.4F, 10, GREY, 0.0F, 0.15F, 0.0F, (float) (Math.PI / 2), 0.0F, 0.0F);
               b.sph(0.26F, c("111111"), 0.0F, 0.2F, -1.65F, 1.0F, 0.8F, 1.4F);

               for (float x : new float[]{-0.8F, 0.8F}) {
                  b.box(0.06F, 0.7F, 0.8F, GREY, x, 0.45F, 1.1F, 0.0F, 0.0F, x * 0.15F);
               }

               b.box(0.4F, 0.05F, 0.4F, A, 0.0F, 0.3F, 0.2F);
               rot.disc(0.7F, Color.WHITE, 0.0F, 0.15F, 1.75F, (float) (Math.PI / 2), 0.0F, 0.0F);
               gl.sph(0.12F, new Color(1.0F, 0.2F, 0.15F, 1.0F), 2.2F, 0.05F, 0.3F);
               gl.sph(0.12F, new Color(0.2F, 1.0F, 0.4F, 1.0F), -2.2F, 0.05F, 0.3F);
               r = 2.8F;
               sc = 1.35F;
               break;
            case "kami":
               b.cyl(0.28F, 0.32F, 3.6F, 10, c("8b8f96"), 0.0F, 0.0F, 0.0F, (float) (Math.PI / 2), 0.0F, 0.0F);
               b.cyl(0.0F, 0.28F, 0.8F, 10, A, 0.0F, 0.0F, -2.2F, (float) (-Math.PI / 2), 0.0F, 0.0F);
               b.wing(3.2F, 1.6F, 0.08F, c("6e737a"), 0.0F, 0.0F, 0.3F);
               b.box(0.05F, 0.6F, 0.6F, c("6e737a"), 0.0F, 0.3F, 1.5F);
               gl.sph(0.2F, new Color(1.0F, 0.55F, 0.2F, 1.0F), 0.0F, 0.0F, 2.1F);
               r = 2.4F;
               sc = 1.3F;
               break;
            case "scout":
               b.cyl(0.3F, 0.22F, 3.8F, 10, c("dadde2"), 0.0F, 0.0F, 0.0F, (float) (Math.PI / 2), 0.0F, 0.0F);
               b.box(7.5F, 0.1F, 0.7F, c("dadde2"), 0.0F, 0.1F, -0.2F);
               b.box(2.2F, 0.06F, 0.45F, c("dadde2"), 0.0F, 0.1F, 1.7F);
               b.box(0.06F, 0.6F, 0.5F, c("dadde2"), 0.0F, 0.4F, 1.7F);
               b.cyl(0.02F, 0.02F, 2.2F, 4, c("333333"), 0.0F, 1.0F, -0.4F);
               gl.sph(0.14F, new Color(1.0F, 0.9F, 0.2F, 1.0F), 0.0F, 2.1F, -0.4F);
               rot.disc(0.6F, Color.WHITE, 0.0F, 0.0F, 2.0F, (float) (Math.PI / 2), 0.0F, 0.0F);
               r = 3.2F;
               sc = 1.4F;
               break;
            case "erk":
               b.cyl(0.13F, 0.13F, 1.6F, 8, c("c9c9c9"), 0.0F, 0.0F, 0.0F, (float) (Math.PI / 2), 0.0F, 0.0F);
               b.cyl(0.0F, 0.13F, 0.4F, 8, c("d05030"), 0.0F, 0.0F, -1.0F, (float) (-Math.PI / 2), 0.0F, 0.0F);

               for (int i = 0; i < 4; i++) {
                  b.box(
                     0.02F,
                     0.3F,
                     0.3F,
                     c("777777"),
                     MathUtils.cos(i * (float) Math.PI / 2.0F) * 0.15F,
                     MathUtils.sin(i * (float) Math.PI / 2.0F) * 0.15F,
                     0.6F,
                     0.0F,
                     0.0F,
                     i * (float) Math.PI / 2.0F
                  );
               }

               gl.sph(0.16F, new Color(1.0F, 0.6F, 0.25F, 1.0F), 0.0F, 0.0F, 0.85F);
               r = 1.4F;
               sc = 1.5F;
               break;
            case "shell":
               b.sph(0.3F, c("333333"));
               gl.sph(0.2F, new Color(1.0F, 0.35F, 0.2F, 1.0F), 0.0F, 0.0F, 0.0F);
               r = 1.3F;
               sc = 1.5F;
               break;
            case "node":
               b.sph(1.4F, c("2b2f3a"), 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 4);
               gl.torus(1.6F, 0.12F, A);
               r = 2.4F;
               break;
            case "boss":
               b.cyl(3.0F, 3.4F, 1.4F, 6, c("2a2d33"));
               b.cyl(2.2F, 3.0F, 0.8F, 6, c("3a3e45"), 0.0F, 0.9F, 0.0F);

               for (int i = 0; i < 6; i++) {
                  float a = i / 6.0F * (float) (Math.PI * 2);
                  float x = MathUtils.cos(a) * 5.0F;
                  float z = MathUtils.sin(a) * 5.0F;
                  b.box(4.2F, 0.35F, 0.6F, c("24272c"), x / 2.0F, 0.2F, z / 2.0F, 0.0F, -a, 0.0F);
                  b.cyl(0.5F, 0.6F, 0.8F, 8, c("1a1a1a"), x, 0.4F, z);
                  rot.disc(2.3F, Color.WHITE, x, 0.85F, z);
               }

               for (float x : new float[]{-1.8F, 1.8F}) {
                  b.box(0.9F, 0.7F, 2.6F, c("3a2a2a"), x, -0.9F, 0.0F);
               }

               gl.sph(0.6F, new Color(1.0F, 0.15F, 0.1F, 1.0F), 0.0F, -0.7F, -3.0F);
               r = 7.0F;
               break;
            case "gunship":
               Color GW = c("30343a");
               Color GD = c("23262b");
               b.sph(2.2F, GW, 0.0F, 0.2F, -0.6F, 0.95F, 0.85F, 2.0F, 24);
               b.sph(1.6F, GW, 0.0F, -0.1F, -3.0F, 0.8F, 0.7F, 1.4F, 20);
               b.seg(V(0.0F, 0.4F, 2.8F), V(0.0F, 0.75F, 9.6F), 0.85F, 0.35F, 16, GW);
               b.extrude(new float[][]{{-0.2F, 0.0F}, {1.6F, 0.0F}, {2.4F, 2.4F}, {1.4F, 2.6F}, {0.4F, 0.9F}}, 0.16F, GD, 0.0F, 0.7F, 9.0F);
               b.box(2.6F, 0.14F, 0.9F, GD, 0.0F, 0.75F, 9.0F);
               b.extrude(new float[][]{{-1.1F, -0.15F}, {1.2F, -0.12F}, {1.3F, 0.12F}, {-1.1F, 0.15F}}, 6.6F, GD, 0.0F, -0.25F, 0.4F);
               b.sph(0.9F, GD, 0.0F, 1.75F, -0.2F, 1.3F, 0.6F, 2.2F, 18);
               b.cyl(0.25F, 0.25F, 1.0F, 12, c("222222"), 0.0F, 2.3F, 0.0F);
               b.cyl(0.55F, 0.55F, 0.3F, 16, GD, 0.0F, 2.75F, 0.0F);

               for (int k = 0; k < 4; k++) {
                  b.box(4.6F, 0.1F, 0.35F, GD, 0.0F, 2.85F, 0.0F, 0.0F, k * (float) Math.PI / 4.0F, 0.0F);
               }

               rot.disc(8.5F, Color.WHITE, 0.0F, 2.95F, 0.0F);
               rot.disc(1.4F, Color.WHITE, 0.3F, 1.9F, 9.9F, 0.0F, 0.0F, (float) (Math.PI / 2));
               gl.sph(0.4F, new Color(1.0F, 0.2F, 0.1F, 1.0F), 0.0F, -0.4F, -4.2F);
               r = 7.0F;
               break;
            case "walker":
               Color HW = c("3a3e44");
               Color HD = c("2a2d32");
               Color HL = c("4a4f56");
               b.sph(3.0F, HW, 0.0F, 0.4F, 0.2F, 1.15F, 0.72F, 1.25F, 24);
               b.extrude(new float[][]{{-1.0F, -1.5F}, {3.0F, -1.3F}, {3.9F, 0.0F}, {3.1F, 1.5F}, {-1.2F, 1.9F}}, 4.6F, HL, 0.0F, 0.2F, 0.0F);
               b.extrude(new float[][]{{-2.6F, -0.9F}, {1.8F, -0.9F}, {2.2F, 0.6F}, {-2.8F, 0.9F}}, 6.4F, HD, 0.0F, -0.6F, 0.4F);
               b.sph(1.0F, BGL, 0.0F, 0.95F, -3.55F, 1.7F, 0.38F, 0.55F, 20);
               gl.box(2.6F, 0.12F, 0.06F, new Color(1.0F, 0.25F, 0.12F, 1.0F), 0.0F, 0.95F, -3.95F);

               for (float x : new float[]{-2.6F, 2.6F}) {
                  b.cyl(1.15F, 1.15F, 1.2F, 20, HD, x, -1.4F, 0.2F, 0.0F, 0.0F, (float) (Math.PI / 2));
               }

               b.cyl(0.95F, 0.95F, 2.6F, 20, HL, 3.7F, 0.9F, -0.4F, (float) (Math.PI / 2), 0.0F, 0.0F);
               b.cyl(0.42F, 0.36F, 6.0F, 16, HD, 3.7F, 0.9F, -4.5F, (float) (Math.PI / 2), 0.0F, 0.0F);
               b.cyl(0.55F, 0.55F, 0.9F, 16, HD, 3.7F, 0.9F, -7.3F, (float) (Math.PI / 2), 0.0F, 0.0F);

               for (int k = 0; k < 3; k++) {
                  b.box(1.2F, 0.08F, 0.16F, BDK, 3.7F, 0.9F, -7.0F - k * 0.26F);
               }

               b.box(2.0F, 1.6F, 2.6F, HL, -3.7F, 1.0F, -0.2F);

               for (int i = 0; i < 9; i++) {
                  gl.sph(0.16F, new Color(1.0F, 0.4F, 0.15F, 1.0F), -4.3F + i % 3 * 0.6F, 0.5F + i / 3 * 0.5F, -1.55F, 1.0F, 1.0F, 1.0F, 6);
               }

               b.cyl(1.25F, 1.35F, 2.6F, 22, HD, 0.0F, 2.0F, 2.2F);

               for (int k = 0; k < 4; k++) {
                  b.torus(1.38F, 0.08F, HL, 0.0F, 1.0F + k * 0.6F, 2.2F);
               }

               r = 7.5F;
               sc = 2.6F;
               break;
            case "carrier":
               b.sph(9.0F, c("3a3530"), 0.0F, 0.0F, 0.0F, 1.0F, 0.75F, 2.2F, 14);
               b.box(5.0F, 2.2F, 14.0F, c("24211e"), 0.0F, -6.4F, 0.0F);

               for (int i = 0; i < 4; i++) {
                  gl.box(2.2F, 0.4F, 2.2F, new Color(0.9F, 0.55F, 0.15F, 1.0F), 0.0F, -7.6F, -5.0F + i * 3.4F);
               }

               for (float x : new float[]{-7.5F, 7.5F}) {
                  b.box(0.4F, 6.0F, 4.0F, c("2c2a27"), x, 0.0F, 16.0F);
                  b.cyl(1.2F, 1.2F, 3.0F, 10, c("1a1a1a"), x * 1.25F, 0.0F, 10.0F, (float) (Math.PI / 2), 0.0F, 0.0F);
                  rot.disc(3.0F, Color.WHITE, x * 1.25F, 0.0F, 11.6F, (float) (Math.PI / 2), 0.0F, 0.0F);
               }

               r = 16.0F;
               break;
            case "ghost":
               b.wing(36.0F, 16.0F, 0.9F, c("1b1d22"));
               b.sph(3.0F, c("22252b"), 0.0F, 0.6F, -1.0F, 1.2F, 0.5F, 2.2F);

               for (float x : new float[]{-3.0F, 3.0F}) {
                  b.box(1.6F, 0.4F, 1.2F, c("111111"), x, 0.4F, 5.5F);
               }

               gl.sph(0.4F, new Color(0.6F, 0.85F, 1.0F, 1.0F), 0.0F, 0.9F, -6.0F);
               r = 12.0F;
               break;
            case "core":
               b.sph(5.2F, c("1f2229"), 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 16);
               b.torus(7.0F, 0.35F, c("3a3f4a"));
               gl.torus(7.6F, 0.25F, A, 0.0F, 0.0F, 0.0F, 0.3F, 0.4F, 0.0F);
               gl.sph(0.8F, new Color(1.0F, 0.1F, 0.1F, 1.0F), 0.0F, 0.0F, -5.3F);
               r = 7.0F;
               break;
            case "hydra":
               b.wing(34.0F, 18.0F, 0.7F, c("26282e"));
               b.sph(3.4F, c("4b4f57"), 0.0F, 0.6F, 0.0F, 1.2F, 0.5F, 2.0F);

               for (float x : new float[]{-5.0F, 0.0F, 5.0F}) {
                  b.cyl(1.0F, 1.0F, 2.6F, 10, c("1c1c1c"), x, 0.4F, 7.4F, (float) (Math.PI / 2), 0.0F, 0.0F);
                  gl.sph(0.8F, new Color(0.75F, 0.45F, 1.0F, 1.0F), x, 0.4F, 8.6F);
               }

               for (float x : new float[]{-12.0F, -6.0F, 6.0F, 12.0F}) {
                  gl.sph(0.35F, new Color(1.0F, 0.2F, 0.2F, 1.0F), x, -0.2F, x * 0.1F);
               }

               r = 13.0F;
               break;
            case "chimera":
               b.wing(40.0F, 20.0F, 1.0F, c("241d30"));
               b.sph(5.0F, c("2f2640"), 0.0F, 1.2F, 1.0F, 1.0F, 0.8F, 1.2F, 16);
               gl.torus(6.3F, 0.3F, c("9b59b6"), 0.0F, 1.2F, 1.0F);

               for (float x : new float[]{-9.0F, -3.0F, 3.0F, 9.0F}) {
                  b.box(1.4F, 0.8F, 4.0F, c("15111c"), x, -0.4F, 4.0F);
               }

               gl.sph(0.9F, new Color(0.85F, 0.4F, 1.0F, 1.0F), 0.0F, 1.2F, -4.0F);
               r = 14.0F;
               break;
            case "storm":
               b.sph(8.0F, c("3a3f4a"), 0.0F, 0.0F, 0.0F, 1.0F, 0.32F, 1.0F, 18);
               b.sph(4.0F, c("2a2e36"), 0.0F, 1.6F, 0.0F, 1.0F, 0.55F, 1.0F, 14);
               b.torus(8.6F, 0.45F, c("23262c"));

               for (int i = 0; i < 3; i++) {
                  float a = i * (float) (Math.PI * 2) / 3.0F;
                  float px = MathUtils.cos(a) * 5.0F;
                  float pz = MathUtils.sin(a) * 5.0F;
                  float py = -1.2F;
                  b.cyl(0.35F, 0.6F, 3.2F, 10, c("4b5160"), px, py - 1.4F, pz);

                  for (int k = 0; k < 3; k++) {
                     b.torus(0.75F - k * 0.12F, 0.12F, c("8a929f"), px, py - 0.8F - k * 0.7F, pz);
                  }

                  gl.sph(0.55F, new Color(0.55F, 0.85F, 1.0F, 1.0F), px, py - 3.2F, pz, 1.0F, 1.0F, 1.0F, 8);
               }

               for (int i = 0; i < 8; i++) {
                  float a = i * (float) (Math.PI * 2) / 8.0F;
                  gl.sph(0.22F, new Color(0.5F, 0.9F, 1.0F, 1.0F), MathUtils.cos(a) * 8.0F, 0.4F, MathUtils.sin(a) * 8.0F, 1.0F, 1.0F, 1.0F, 6);
               }

               for (float x : S2) {
                  rot.disc(1.6F, Color.WHITE, x * 9.2F, 0.2F, 2.5F, (float) (Math.PI / 2), 0.0F, 0.0F);
               }

               r = 10.0F;
               sc = 1.6F;
               break;
            case "manta":
               b.wing(30.0F, 14.0F, 0.8F, c("2b3036"));
               b.sph(2.4F, c("3b424b"), 0.0F, 0.5F, -1.0F, 1.2F, 0.45F, 2.2F, 12);
               b.seg(V(0.0F, 0.2F, 5.0F), V(0.0F, 0.6F, 16.0F), 0.6F, 0.15F, 8, c("23272c"));

               for (float x : new float[]{-6.0F, 6.0F}) {
                  b.box(1.4F, 0.5F, 3.0F, c("1b1e22"), x, -0.2F, 2.0F);
               }

               for (float x : new float[]{-6.0F, 6.0F}) {
                  gl.box(1.0F, 0.2F, 0.3F, new Color(1.0F, 0.45F, 0.15F, 1.0F), x, -0.2F, 3.55F);
               }

               gl.sph(0.5F, new Color(1.0F, 0.3F, 0.2F, 1.0F), 0.0F, 0.2F, -5.6F);
               r = 10.0F;
               sc = 1.4F;
               break;
            case "colossus":
               tracksSimple(b, 9.0F, 2.6F, 2.0F);
               b.box(4.2F, 1.5F, 8.6F, c("34382f"), 0.0F, 1.6F, 0.0F);
               b.extrude(new float[][]{{-4.6F, 0.0F}, {3.4F, 0.0F}, {4.9F, 0.8F}, {4.1F, 1.5F}, {-4.6F, 1.5F}}, 6.4F, c("3c4038"), 0.0F, 2.3F, 0.0F);
               b.cyl(2.3F, 2.7F, 1.5F, 16, c("4a4f45"), 0.0F, 4.55F, 0.7F);
               b.extrude(new float[][]{{-1.2F, 0.0F}, {2.4F, 0.0F}, {3.0F, 0.6F}, {2.4F, 1.2F}, {-1.2F, 1.2F}}, 3.6F, c("454a40"), 0.0F, 4.4F, 0.4F);

               for (float x : new float[]{-0.9F, 0.9F}) {
                  b.cyl(0.35F, 0.42F, 7.5F, 10, c("1f211d"), x, 5.0F, -5.2F, (float) (Math.PI / 2), 0.0F, 0.0F);
                  b.cyl(0.5F, 0.5F, 0.9F, 10, c("1f211d"), x, 5.0F, -8.6F, (float) (Math.PI / 2), 0.0F, 0.0F);
               }

               for (float x : new float[]{-2.6F, 2.6F}) {
                  b.box(1.0F, 1.0F, 1.8F, c("4a2a2a"), x, 5.0F, 2.2F);
               }

               gl.sph(0.35F, new Color(1.0F, 0.2F, 0.15F, 1.0F), 0.0F, 5.7F, -2.6F);
               r = 6.0F;
               sc = 2.2F;
               break;
            case "inf":
            case "rpg":
               Color cA = c("3d4148");
               Color cB = c("2a2d33");
               Color cC = c("1c1e22");
               b.box(0.52F, 0.24F, 0.34F, cB, 0.0F, 1.36F, 0.0F);
               b.seg(V(0.0F, 1.45F, 0.0F), V(0.0F, 1.75F, 0.0F), 0.22F, 0.27F, 10, cA);
               b.sph(0.34F, cA, 0.0F, 1.92F, 0.0F, 1.0F, 0.85F, 0.66F, 12);
               b.box(0.56F, 0.46F, 0.12F, cB, 0.0F, 1.9F, -0.2F);
               b.box(0.5F, 0.1F, 0.13F, cC, 0.0F, 1.62F, -0.19F);

               for (float x : new float[]{-0.16F, 0.0F, 0.16F}) {
                  b.box(0.12F, 0.16F, 0.08F, cC, x, 1.72F, -0.27F);
               }

               b.box(0.44F, 0.56F, 0.26F, cB, 0.0F, 1.9F, 0.3F);
               b.cyl(0.012F, 0.012F, 0.8F, 4, cC, 0.15F, 2.5F, 0.38F);
               b.seg(V(0.0F, 2.15F, 0.0F), V(0.0F, 2.28F, 0.0F), 0.08F, 0.08F, 8, cC);
               b.sph(0.16F, c("2f3238"), 0.0F, 2.38F, 0.0F, 1.0F, 1.0F, 1.0F, 10);
               b.sph(0.2F, cB, 0.0F, 2.44F, 0.01F, 1.0F, 0.78F, 1.08F, 12);
               gl.box(0.26F, 0.05F, 0.03F, new Color(1.0F, 0.2F, 0.15F, 1.0F), 0.0F, 2.38F, -0.16F);

               for (float s : S2) {
                  Vector3 sh = V(s * 0.37F, 2.1F, 0.0F);
                  Vector3 el = V(s * 0.42F, 1.78F, -0.2F);
                  b.sph(0.12F, cB, sh.x, sh.y, sh.z, 1.0F, 1.0F, 1.0F, 8);
                  b.seg(sh, el, 0.085F, 0.075F, 8, cA);
                  Vector3 hand = type.equals("inf") ? V(s * 0.12F, 1.72F, -0.55F) : V(s * 0.3F, 2.0F, -0.35F);
                  if (type.equals("rpg") && s < 0.0F) {
                     hand = V(-0.2F, 1.85F, -0.4F);
                  }

                  b.seg(el, hand, 0.072F, 0.06F, 8, cA);
                  b.sph(0.065F, cC, hand.x, hand.y, hand.z, 1.0F, 1.0F, 1.0F, 6);
               }

               if (type.equals("inf")) {
                  b.box(0.07F, 0.14F, 0.75F, cC, 0.06F, 1.72F, -0.6F);
                  b.seg(V(0.06F, 1.75F, -0.95F), V(0.06F, 1.75F, -1.35F), 0.025F, 0.022F, 6, cC);
                  b.box(0.06F, 0.2F, 0.1F, cC, 0.06F, 1.58F, -0.62F, -0.25F, 0.0F, 0.0F);
                  b.box(0.06F, 0.12F, 0.3F, cC, 0.06F, 1.68F, -0.12F);
                  b.box(0.05F, 0.06F, 0.18F, cC, 0.06F, 1.82F, -0.55F);
               } else {
                  b.seg(V(0.3F, 2.12F, 0.55F), V(0.3F, 2.12F, -0.85F), 0.1F, 0.1F, 10, c("3a4232"));
                  b.seg(V(0.3F, 2.12F, -0.85F), V(0.3F, 2.12F, -1.25F), 0.13F, 0.04F, 10, c("6a3a28"));
                  b.box(0.08F, 0.18F, 0.1F, cC, 0.3F, 1.98F, -0.35F);
               }

               r = 1.4F;
               sc = 4.0F;
               break;
            case "mech":
               b.extrude(new float[][]{{-1.3F, -0.7F}, {1.3F, -0.7F}, {1.6F, 0.2F}, {1.0F, 1.0F}, {-1.3F, 1.0F}}, 2.6F, c("3a3d33"), 0.0F, 3.6F, 0.0F);

               for (float x : new float[]{-1.7F, 1.7F}) {
                  b.box(0.7F, 0.7F, 1.2F, c("2c2f28"), x, 3.8F, 0.0F);
                  b.cyl(0.18F, 0.2F, 1.8F, 8, c("1a1a1a"), x, 3.8F, -1.2F, (float) (Math.PI / 2), 0.0F, 0.0F);
               }

               gl.box(1.2F, 0.12F, 0.08F, new Color(1.0F, 0.2F, 0.2F, 1.0F), 0.0F, 4.0F, -1.62F);
               r = 2.6F;
               sc = 2.6F;
               break;
            case "mortar":
               tracksSimple(b, 2.6F, 0.8F, 0.6F);
               b.box(1.6F, 0.6F, 2.2F, c("3a3d33"), 0.0F, 0.9F, 0.0F);
               b.cyl(0.22F, 0.28F, 1.8F, 10, c("1a1a1a"), 0.0F, 1.7F, -0.3F, -0.6F, 0.0F, 0.0F);
               r = 1.8F;
               sc = 3.0F;
               break;
            default:
               quadBody(b, gl, rot, A, 4, DGREY, 1.0F);
         }

         bossDetail(type, b, gl, rot, A);
         ed = new Models.EnemyDef();
         ed.body = b.mesh();
         gl.noAo = true;
         if (!gl.empty()) {
            boolean h = MB.hq;
            MB.hq = false;
            ed.glow = gl.mesh();
            MB.hq = h;
         }

         if (!rot.empty()) {
            boolean h = MB.hq;
            MB.hq = false;
            ed.rot = rot.mesh();
            MB.hq = h;
         }

         ed.r = r;
         ed.sc = sc;
         MB.hq = false;
         enemyCache.put(key, ed);
         return ed;
      }
   }

   static Mesh3 leg(String kind) {
      Mesh3 m = legCache.get(kind);
      if (m != null) {
         return m;
      } else {
         MB.hq = kind.equals("walker");
         MB b = new MB();
         if (kind.equals("walker")) {
            Color LW = c("2e3237");
            Color LD = c("1c1e21");
            Color LL = c("444a51");
            b.sph(1.05F, LL, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 20);
            b.seg(V(0.0F, 0.0F, 0.0F), V(0.0F, -3.0F, -0.7F), 0.85F, 0.62F, 16, LW);
            b.extrude(new float[][]{{-0.5F, -2.6F}, {0.9F, -2.4F}, {1.1F, -0.4F}, {0.2F, 0.2F}, {-0.6F, -0.4F}}, 1.5F, LL);
            b.sph(0.82F, LD, 0.0F, -3.0F, -0.7F, 1.0F, 1.0F, 1.0F, 18);
            b.cyl(0.95F, 0.95F, 1.5F, 20, LL, 0.0F, -3.0F, -0.7F, 0.0F, 0.0F, (float) (Math.PI / 2));
            b.seg(V(0.0F, -3.0F, -0.7F), V(0.0F, -6.0F, 0.3F), 0.62F, 0.45F, 16, LW);
            b.extrude(new float[][]{{0.2F, -5.4F}, {1.25F, -5.2F}, {1.35F, -3.3F}, {0.6F, -2.9F}}, 1.3F, LL);

            for (float x : new float[]{-0.55F, 0.55F}) {
               b.seg(V(x, -0.6F, 0.75F), V(x, -2.7F, 0.2F), 0.14F, 0.14F, 8, LD);
               b.seg(V(x, -3.3F, 0.35F), V(x, -5.5F, 0.85F), 0.12F, 0.12F, 8, c("8a8f96"));
            }

            b.sph(0.5F, LD, 0.0F, -6.0F, 0.3F, 1.0F, 1.0F, 1.0F, 14);
            b.extrude(new float[][]{{-1.3F, -6.95F}, {1.7F, -6.95F}, {1.9F, -6.5F}, {0.6F, -6.1F}, {-1.1F, -6.3F}}, 2.2F, LW);

            for (float x : new float[]{-0.75F, 0.0F, 0.75F}) {
               b.seg(V(x, -6.7F, -1.5F), V(x * 1.25F, -6.92F, -2.7F), 0.26F, 0.05F, 10, LD);
            }

            b.seg(V(0.0F, -6.7F, 1.1F), V(0.0F, -6.92F, 2.1F), 0.24F, 0.05F, 10, LD);
         } else if (kind.equals("mech")) {
            b.box(0.6F, 2.8F, 0.7F, c("2c2f28"), 0.0F, -1.4F, 0.0F);
            b.box(1.0F, 0.3F, 1.4F, c("1f211c"), 0.0F, -2.85F, -0.2F);
         } else {
            b.sph(0.13F, c("2a2d33"), 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 8);
            b.seg(V(0.0F, 0.0F, 0.0F), V(0.0F, -0.6F, -0.04F), 0.12F, 0.095F, 8, c("3d4148"));
            b.box(0.2F, 0.2F, 0.08F, c("2a2d33"), 0.0F, -0.62F, -0.12F);
            b.seg(V(0.0F, -0.6F, -0.04F), V(0.0F, -1.12F, 0.02F), 0.09F, 0.075F, 8, c("3d4148"));
            b.box(0.18F, 0.14F, 0.34F, c("1c1e22"), 0.0F, -1.2F, -0.06F);
         }

         m = b.mesh();
         MB.hq = false;
         legCache.put(kind, m);
         return m;
      }
   }

   static void rivets(MB b, Vector3 a, Vector3 c2, int n, float r) {
      for (int i = 0; i < n; i++) {
         float k = (float)i / Math.max(1, n - 1);
         b.sph(r, c("5a6068"), MathUtils.lerp(a.x, c2.x, k), MathUtils.lerp(a.y, c2.y, k), MathUtils.lerp(a.z, c2.z, k), 1.0F, 1.0F, 1.0F, 4);
      }
   }

   static void pod(MB b, MB gl, float px, float py, float pz, int n, float r, float L) {
      b.cyl(r, r, L, 12, BD1, px, py, pz, (float) (Math.PI / 2), 0.0F, 0.0F);
      b.cyl(r * 1.05F, r * 1.05F, 0.15F, 12, BDK, px, py, pz - L / 2.0F, (float) (Math.PI / 2), 0.0F, 0.0F);

      for (int i = 0; i < n; i++) {
         float a = i * (float) (Math.PI * 2) / n;
         gl.sph(
            r * 0.18F,
            new Color(1.0F, 0.45F, 0.2F, 1.0F),
            px + MathUtils.cos(a) * r * 0.55F,
            py + MathUtils.sin(a) * r * 0.55F,
            pz - L / 2.0F - 0.05F,
            1.0F,
            1.0F,
            1.0F,
            4
         );
      }
   }

   static void nozzle(MB b, MB gl, float px, float py, float pz, float r, Color glow) {
      b.cyl(r, r * 0.8F, r * 1.4F, 12, BDK, px, py, pz, (float) (Math.PI / 2), 0.0F, 0.0F);
      gl.cyl(r * 0.65F, r * 0.65F, 0.1F, 12, glow, px, py, pz + r * 0.72F, (float) (Math.PI / 2), 0.0F, 0.0F);
   }

   static void navl(MB gl, float x, float y, float z, boolean red) {
      gl.sph(0.28F, red ? new Color(1.0F, 0.15F, 0.1F, 1.0F) : new Color(0.2F, 1.0F, 0.4F, 1.0F), x, y, z, 1.0F, 1.0F, 1.0F, 6);
   }

   static void bossDetail(String type, MB b, MB gl, MB rot, Color A) {
      Color glowO = new Color(1.0F, 0.55F, 0.2F, 1.0F);
      switch (type) {
         case "boss":
            for (int i = 0; i < 6; i++) {
               float a = i / 6.0F * (float) (Math.PI * 2);
               float x = MathUtils.cos(a) * 5.0F;
               float z = MathUtils.sin(a) * 5.0F;
               b.torus(2.45F, 0.14F, BD2, x, 0.85F, z);

               for (int k = 0; k < 3; k++) {
                  b.box(0.08F, 0.5F, 0.08F, BD1, x + MathUtils.cos(a + k * 2.1F) * 2.4F, 0.6F, z + MathUtils.sin(a + k * 2.1F) * 2.4F);
               }

               navl(gl, x + MathUtils.cos(a) * 2.5F, 1.1F, z + MathUtils.sin(a) * 2.5F, i % 2 == 0);

               for (int k = 0; k < 5; k++) {
                  b.box(0.42F, 0.04F, 0.12F, k % 2 == 0 ? A : BDK, x * (0.25F + k * 0.1F), 0.39F, z * (0.25F + k * 0.1F), 0.0F, -a, 0.0F);
               }
            }

            for (int i = 0; i < 6; i++) {
               float a = (i + 0.5F) / 6.0F * (float) (Math.PI * 2);
               b.box(1.6F, 0.12F, 1.0F, BD3, MathUtils.cos(a) * 2.2F, 1.32F, MathUtils.sin(a) * 2.2F, 0.25F, -a, 0.0F);
               rivets(b, V(MathUtils.cos(a) * 1.7F, 1.42F, MathUtils.sin(a) * 1.7F), V(MathUtils.cos(a) * 2.7F, 1.25F, MathUtils.sin(a) * 2.7F), 3, 0.07F);
            }

            b.cyl(1.0F, 0.8F, 0.6F, 12, BD1, 0.0F, 1.6F, 0.0F);
            b.cyl(0.08F, 0.08F, 2.5F, 6, c("777777"), 0.6F, 2.6F, 0.4F);
            gl.sph(0.18F, new Color(1.0F, 0.2F, 0.15F, 1.0F), 0.6F, 3.9F, 0.4F);
            b.sph(1.1F, BD2, 0.0F, -1.5F, -0.6F, 1.0F, 1.0F, 1.0F, 12);

            for (float x : new float[]{-0.35F, 0.35F}) {
               b.cyl(0.14F, 0.14F, 2.4F, 8, BDK, x, -1.6F, -2.0F, (float) (Math.PI / 2), 0.0F, 0.0F);
            }

            gl.cyl(0.3F, 0.3F, 0.05F, 10, new Color(1.0F, 0.2F, 0.15F, 1.0F), 0.0F, -1.3F, -1.65F, (float) (Math.PI / 2), 0.0F, 0.0F);

            for (float x : new float[]{-2.2F, 2.2F}) {
               b.box(0.18F, 0.18F, 5.0F, BDK, x, -2.0F, 0.0F);

               for (float z : new float[]{-1.5F, 1.5F}) {
                  b.box(0.14F, 1.0F, 0.14F, BD1, x, -1.5F, z);
               }
            }
            break;
         case "gunship":
            b.sph(1.1F, BGL, 0.0F, 0.95F, -2.6F, 0.75F, 0.6F, 1.3F, 18);
            b.sph(1.0F, BGL, 0.0F, 1.2F, -1.2F, 0.75F, 0.6F, 1.1F, 18);
            b.box(1.5F, 0.06F, 0.08F, BDK, 0.0F, 1.62F, -1.9F);
            b.sph(0.6F, BD2, 0.0F, -1.0F, -3.7F, 1.0F, 1.0F, 1.0F, 16);

            for (float x : new float[]{-0.15F, 0.15F}) {
               b.cyl(0.08F, 0.08F, 1.8F, 10, BDK, x, -1.05F, -4.7F, (float) (Math.PI / 2), 0.0F, 0.0F);
            }

            for (float x : new float[]{-3.0F, 3.0F}) {
               pod(b, gl, x, -0.6F, 0.3F, 7, 0.55F, 2.0F);
               pod(b, gl, x * 0.68F, -0.55F, 0.6F, 4, 0.32F, 1.6F);
               b.box(0.2F, 0.5F, 0.3F, BD1, x, -0.25F, 0.3F);
            }

            for (float x : new float[]{-0.95F, 0.95F}) {
               b.sph(0.7F, BD2, x, 1.5F, 0.6F, 0.9F, 0.85F, 2.1F, 16);
               b.cyl(0.4F, 0.4F, 0.12F, 14, BDK, x, 1.5F, -0.85F, (float) (Math.PI / 2), 0.0F, 0.0F);
               nozzle(b, gl, x, 1.5F, 2.1F, 0.34F, glowO);
               b.seg(V(x * 1.3F, -1.7F, -2.4F), V(x * 1.3F, -1.7F, 1.8F), 0.08F, 0.08F, 8, BDK);

               for (float z : new float[]{-1.6F, 1.2F}) {
                  b.seg(V(x * 0.9F, -0.9F, z), V(x * 1.3F, -1.7F, z), 0.06F, 0.06F, 6, BD1);
               }
            }

            navl(gl, -3.1F, -0.15F, 0.6F, true);
            navl(gl, 3.1F, -0.15F, 0.6F, false);
            gl.sph(0.2F, Color.WHITE, 0.0F, 3.3F, 10.6F);

            for (int k = 0; k < 6; k++) {
               b.box(0.04F, 0.5F, 0.06F, k % 2 == 0 ? A : BDK, -1.95F, 0.3F, -1.2F + k * 0.4F);
            }
            break;
         case "walker":
            rivets(b, V(-2.0F, 1.75F, -2.6F), V(2.0F, 1.75F, -2.6F), 9, 0.08F);
            rivets(b, V(-2.2F, -1.25F, -3.0F), V(2.2F, -1.25F, -3.0F), 9, 0.08F);

            for (float x : new float[]{-0.8F, 0.8F}) {
               nozzle(b, gl, x, 1.4F, 3.7F, 0.42F, glowO);
            }

            b.cyl(0.06F, 0.06F, 3.0F, 6, c("777777"), 1.2F, 4.6F, 1.8F);
            gl.sph(0.16F, new Color(1.0F, 0.2F, 0.15F, 1.0F), 1.2F, 6.1F, 1.8F);
            b.sph(0.45F, BD3, -1.4F, 2.1F, -1.8F, 1.0F, 1.0F, 1.0F, 14);
            gl.sph(0.15F, new Color(1.0F, 0.2F, 0.1F, 1.0F), -1.4F, 2.1F, -2.25F);

            for (float k : S2) {
               gl.sph(0.18F, new Color(1.0F, 0.6F, 0.2F, 1.0F), k * 1.8F, 0.35F, -3.62F);
            }
            break;
         case "carrier":
            for (int i = 0; i < 7; i++) {
               float z = -14.0F + i * 4.7F;
               float k = (float)Math.sqrt(Math.max(0.0, 1.0 - Math.pow(z / 19.8, 2.0))) * 1.01F;

               for (int j = 0; j < 28; j++) {
                  float t0 = j * (float) (Math.PI * 2) / 28.0F;
                  float t1 = (j + 1) * (float) (Math.PI * 2) / 28.0F;
                  b.seg(
                     V(MathUtils.cos(t0) * 9.0F * k, MathUtils.sin(t0) * 6.75F * k, z),
                     V(MathUtils.cos(t1) * 9.0F * k, MathUtils.sin(t1) * 6.75F * k, z),
                     0.16F,
                     0.16F,
                     4,
                     BD1
                  );
               }
            }

            for (float a : new float[]{0.0F, (float) (Math.PI / 2), (float) Math.PI, (float) (Math.PI * 3.0 / 2.0)}) {
               b.box(0.3F, 4.5F, 5.0F, c("2c2a27"), MathUtils.sin(a) * 4.0F, MathUtils.cos(a) * 4.0F, 18.5F, 0.0F, 0.0F, -a);
            }

            for (int i = 0; i < 8; i++) {
               gl.box(0.5F, 0.4F, 0.05F, new Color(1.0F, 0.85F, 0.45F, 1.0F), -2.52F, -6.0F, -5.5F + i * 1.6F, 0.0F, (float) (Math.PI / 2), 0.0F);
               gl.box(0.5F, 0.4F, 0.05F, new Color(1.0F, 0.85F, 0.45F, 1.0F), 2.52F, -6.0F, -5.5F + i * 1.6F, 0.0F, (float) (Math.PI / 2), 0.0F);
            }

            for (int i = 0; i < 4; i++) {
               b.box(2.4F, 0.06F, 2.4F, BDK, 0.0F, -7.52F, -5.0F + i * 3.4F);
            }

            for (float x : new float[]{-1.6F, 1.6F}) {
               gl.sph(0.5F, new Color(1.0F, 1.0F, 0.85F, 1.0F), x, -7.3F, -7.2F);
            }

            b.box(1.4F, 1.2F, 2.0F, BD2, 0.0F, -5.0F, -7.6F);
            b.sph(0.8F, BGL, 0.0F, -5.2F, -8.5F, 1.2F, 0.8F, 0.8F);

            for (int i = 0; i < 3; i++) {
               b.cyl(0.06F, 0.06F, 3.0F, 6, c("777777"), -2 + i * 2, 7.2F, -4 + i * 3);
               navl(gl, -2 + i * 2, 8.7F, -4 + i * 3, i == 1);
            }

            navl(gl, -9.1F, 0.0F, 0.0F, true);
            navl(gl, 9.1F, 0.0F, 0.0F, false);
            break;
         case "ghost":
            b.sph(1.4F, BGL, 0.0F, 1.3F, -3.4F, 1.0F, 0.45F, 1.6F, 12);

            for (float x : new float[]{-3.0F, 3.0F}) {
               b.sph(1.6F, c("15171b"), x, 0.55F, 1.5F, 0.9F, 0.45F, 2.6F, 10);
               b.box(1.5F, 0.35F, 0.3F, BDK, x, 0.75F, -2.0F);
               gl.box(1.3F, 0.18F, 0.08F, new Color(0.55F, 0.75F, 1.0F, 1.0F), x, 0.45F, 6.15F);
            }

            for (float k : S2) {
               for (int i = 0; i < 5; i++) {
                  gl.sph(0.12F, new Color(0.5F, 0.8F, 1.0F, 1.0F), k * (3.5F + i * 3.0F), 0.4F, -1.6F + i * 1.35F, 1.0F, 1.0F, 1.0F, 4);
               }

               b.box(12.0F, 0.04F, 0.08F, BDK, k * 8.0F, 0.47F, 0.5F, 0.0F, k * 0.42F, 0.0F);
               b.box(10.0F, 0.04F, 0.08F, BDK, k * 7.0F, 0.47F, 2.8F, 0.0F, k * 0.42F, 0.0F);
            }

            b.box(3.0F, 0.05F, 4.0F, c("101215"), 0.0F, -0.47F, 0.5F);
            break;
         case "core":
            for (int i = 0; i < 12; i++) {
               float a = i / 12.0F * (float) (Math.PI * 2);

               for (float yy : new float[]{-2.6F, 0.0F, 2.6F}) {
                  float rr = (float)Math.sqrt(Math.max(0.1F, 27.0F - yy * yy));
                  b.box(
                     1.8F,
                     1.5F,
                     0.25F,
                     (i + (int)yy) % 2 == 0 ? BD2 : BD1,
                     MathUtils.cos(a) * rr,
                     yy,
                     MathUtils.sin(a) * rr,
                     0.0F,
                     -a + (float) (Math.PI / 2),
                     0.0F
                  );
               }

               gl.box(0.12F, 1.0F, 0.1F, A, MathUtils.cos(a + 0.26F) * 5.25F, 1.3F, MathUtils.sin(a + 0.26F) * 5.25F, 0.0F, -a, 0.0F);
            }

            for (Vector3 vv : new Vector3[]{V(0.0F, 1.0F, 0.0F), V(0.0F, -1.0F, 0.0F), V(0.7F, 0.5F, 0.5F), V(-0.6F, -0.5F, 0.6F)}) {
               Vector3 d = vv.nor();
               b.seg(d.cpy().scl(5.0F), d.cpy().scl(8.0F), 0.25F, 0.05F, 6, BD3);
               gl.sph(0.2F, A, d.x * 8.1F, d.y * 8.1F, d.z * 8.1F, 1.0F, 1.0F, 1.0F, 4);
            }

            for (int i = 0; i < 8; i++) {
               float a = i / 8.0F * (float) (Math.PI * 2);
               b.box(1.2F, 0.5F, 0.7F, BD3, MathUtils.cos(a) * 7.0F, 0.0F, MathUtils.sin(a) * 7.0F, 0.0F, -a, 0.0F);
            }
            break;
         case "hydra":
            b.sph(1.6F, BGL, 0.0F, 1.3F, -2.6F, 1.0F, 0.5F, 1.5F, 12);

            for (float k : S2) {
               for (int i = 0; i < 3; i++) {
                  float x = k * (5.5F + i * 3.2F);

                  for (int j = 0; j < 2; j++) {
                     b.cyl(0.18F, 0.18F, 2.4F, 8, c("c9c9c9"), x + j * 0.5F - 0.25F, -0.65F, 0.2F + i * 0.9F, (float) (Math.PI / 2), 0.0F, 0.0F);
                  }

                  b.box(1.1F, 0.2F, 2.6F, BD1, x, -0.4F, 0.2F + i * 0.9F);
               }

               b.box(0.18F, 2.4F, 2.6F, BD2, k * 7.5F, 1.2F, 6.0F, 0.0F, 0.0F, k * 0.3F);
               b.box(10.0F, 0.04F, 0.08F, BDK, k * 7.0F, 0.37F, 1.5F, 0.0F, k * 0.5F, 0.0F);
            }

            for (float x : new float[]{-5.0F, 0.0F, 5.0F}) {
               b.torus(1.05F, 0.12F, BD3, x, 0.4F, 6.2F, (float) (Math.PI / 2), 0.0F, 0.0F);
            }
            break;
         case "chimera":
            for (float k : S2) {
               b.seg(V(k * 2.5F, 0.2F, -6.0F), V(k * 4.0F, -0.4F, -11.0F), 0.7F, 0.15F, 8, BD1);
               b.seg(V(k * 4.0F, -0.4F, -11.0F), V(k * 2.8F, -0.8F, -13.5F), 0.2F, 0.03F, 6, c("8a7aa0"));

               for (int i = 0; i < 6; i++) {
                  gl.box(
                     4.0F,
                     0.08F,
                     0.12F,
                     new Color(0.75F, 0.35F, 1.0F, 1.0F),
                     k * (4.0F + i * 2.5F),
                     0.52F,
                     -1.0F + i * 1.6F,
                     0.0F,
                     k * (0.6F - i * 0.05F),
                     0.0F
                  );
               }

               for (float x : new float[]{3.0F, 9.0F}) {
                  nozzle(b, gl, k * x, -0.4F, 6.4F, 0.55F, new Color(0.85F, 0.45F, 1.0F, 1.0F));
               }
            }

            for (int i = 0; i < 7; i++) {
               b.seg(V(0.0F, 4.8F - i * 0.3F, -2.0F + i * 1.4F), V(0.0F, 6.6F - i * 0.35F, -1.6F + i * 1.4F), 0.35F, 0.03F, 6, c("4a3a60"));
            }

            for (int i = 0; i < 3; i++) {
               gl.sph(0.35F, new Color(1.0F, 0.3F, 0.9F, 1.0F), -0.9F + i * 0.9F, 2.0F, -4.6F, 1.0F, 1.0F, 1.0F, 6);
            }
            break;
         case "storm":
            b.box(3.0F, 1.2F, 5.0F, BD1, 0.0F, -3.0F, 0.0F);

            for (int i = 0; i < 5; i++) {
               gl.box(0.4F, 0.3F, 0.05F, new Color(0.7F, 0.9F, 1.0F, 1.0F), -1.52F, -3.0F, -1.6F + i * 0.8F, 0.0F, (float) (Math.PI / 2), 0.0F);
            }

            for (int i = 0; i < 6; i++) {
               float a = i * (float) (Math.PI * 2) / 6.0F + 0.5F;
               b.cyl(0.06F, 0.12F, 3.4F, 6, c("8a929f"), MathUtils.cos(a) * 3.0F, 3.6F, MathUtils.sin(a) * 3.0F);
               gl.sph(0.2F, new Color(0.6F, 0.9F, 1.0F, 1.0F), MathUtils.cos(a) * 3.0F, 5.3F, MathUtils.sin(a) * 3.0F, 1.0F, 1.0F, 1.0F, 4);
            }

            for (int i = 0; i < 16; i++) {
               b.box(
                  1.4F,
                  0.5F,
                  0.3F,
                  BD2,
                  MathUtils.cos(i * (float) (Math.PI * 2) / 16.0F) * 8.6F,
                  0.0F,
                  MathUtils.sin(i * (float) (Math.PI * 2) / 16.0F) * 8.6F,
                  0.0F,
                  -i * (float) (Math.PI * 2) / 16.0F + (float) (Math.PI / 2),
                  0.0F
               );
            }
            break;
         case "manta":
            b.sph(1.2F, BGL, 0.0F, 0.9F, -2.6F, 1.0F, 0.5F, 1.6F, 12);
            b.box(2.6F, 0.06F, 4.0F, BDK, 0.0F, -0.42F, 0.5F);

            for (int i = 0; i < 3; i++) {
               b.box(2.4F, 0.04F, 0.06F, c("5a5f66"), 0.0F, -0.46F, -1.0F + i * 1.2F);
            }

            for (float k : S2) {
               b.box(0.15F, 1.8F, 2.2F, BD2, k * 3.5F, 0.9F, 4.0F, 0.0F, 0.0F, k * 0.25F);
               b.box(1.2F, 0.5F, 1.4F, BDK, k * 2.0F, 0.6F, -0.8F);

               for (int i = 0; i < 6; i++) {
                  gl.box(1.6F, 0.06F, 0.1F, new Color(1.0F, 0.45F, 0.15F, 1.0F), k * (2.5F + i * 2.0F), 0.0F, -2.0F + i * 1.1F, 0.0F, k * 0.55F, 0.0F);
               }
            }
            break;
         case "colossus":
            for (float k : S2) {
               for (int i = 0; i < 5; i++) {
                  b.box(0.08F, 0.95F, 1.7F, BD2, k * 3.25F, 1.75F, -3.6F + i * 1.8F);
               }

               rivets(b, V(k * 3.3F, 2.1F, -4.2F), V(k * 3.3F, 2.1F, 4.2F), 11, 0.07F);

               for (int i = 0; i < 4; i++) {
                  b.box(0.12F, 0.12F, 0.7F, BDK, k * 3.25F, 3.05F, -1.5F + i * 0.75F);
               }

               b.cyl(0.25F, 0.28F, 0.9F, 10, c("2b2b2b"), k * 2.2F, 4.1F, 4.2F);
               gl.cyl(0.18F, 0.18F, 0.05F, 8, new Color(1.0F, 0.5F, 0.2F, 1.0F), k * 2.2F, 4.56F, 4.2F);

               for (int i = 0; i < 3; i++) {
                  b.cyl(0.1F, 0.1F, 0.5F, 6, BDK, k * 2.3F, 5.4F + i * 0.05F, -0.8F + i * 0.25F, -0.6F, 0.0F, -k * 0.6F);
               }
            }

            b.cyl(0.5F, 0.5F, 0.18F, 12, BD3, 0.9F, 5.8F, 1.2F);
            b.cyl(0.07F, 0.07F, 1.4F, 6, BDK, 0.9F, 6.1F, 0.4F, (float) (Math.PI / 2), 0.0F, 0.0F);

            for (int i = 0; i < 3; i++) {
               b.box(0.3F, 0.2F, 0.2F, BDK, -0.8F + i * 0.4F, 3.85F, -4.1F);
            }

            gl.box(1.6F, 0.1F, 0.06F, new Color(0.4F, 0.8F, 1.0F, 1.0F), 0.0F, 3.2F, -4.75F);
            b.cyl(0.07F, 0.07F, 3.5F, 6, c("777777"), -1.6F, 7.3F, 1.8F);
            gl.sph(0.15F, new Color(1.0F, 0.2F, 0.15F, 1.0F), -1.6F, 9.1F, 1.8F);
      }
   }

   static void tracksSimple(MB b, float length, float x, float h) {
      for (float s : S2) {
         b.box(0.62F, h, length, c("1d1d1b"), s * x, h / 2.0F + 0.05F, 0.0F);

         for (int i = 0; i < 6; i++) {
            float z = -length / 2.0F + 0.7F + i * (length - 1.4F) / 5.0F;
            b.cyl(0.42F, 0.42F, 0.66F, 12, c("34362f"), s * x, 0.5F, z, 0.0F, 0.0F, (float) (Math.PI / 2));
            b.cyl(0.18F, 0.18F, 0.7F, 8, STEEL, s * x, 0.5F, z, 0.0F, 0.0F, (float) (Math.PI / 2));
         }

         for (int i = 0; i < 18; i++) {
            b.box(0.66F, 0.06F, 0.18F, c("2c2c28"), s * x, 0.04F, -length / 2.0F + 0.3F + i * (length - 0.6F) / 17.0F);
         }
      }
   }

   public static Models.EnemyModel enemy(String type, String acc) {
      Models.EnemyDef d = def(type, acc);
      Models.EnemyModel em = new Models.EnemyModel();
      Node3 g = new Node3();
      em.body = g.add(new Node3(d.body, MAT_HULL));
      if (d.glow != null) {
         em.glow = g.add(new Node3(d.glow, MAT_GLOW));
      }

      if (d.rot != null) {
         em.rot = g.add(new Node3(d.rot, MAT_ROTOR));
      }

      if (type.equals("inf") || type.equals("rpg") || type.equals("mech") || type.equals("walker")) {
         String lk = type.equals("walker") ? "walker" : (type.equals("mech") ? "mech" : "inf");
         float hip = type.equals("walker") ? -1.4F : (type.equals("mech") ? 3.0F : 1.3F);
         float hx = type.equals("walker") ? 2.6F : (type.equals("mech") ? 1.0F : 0.25F);

         for (float s : S2) {
            Node3 l = new Node3(leg(lk), MAT_HULL);
            l.pos.set(s * hx, hip, 0.0F);
            g.add(l);
            em.legs.add(l);
         }
      }

      g.scl.set(d.sc, d.sc, d.sc);
      em.g = g;
      em.r = d.r * d.sc;
      return em;
   }

   public static Mesh3 missileMesh() {
      if (missile != null) {
         return missile;
      } else {
         MB b = new MB();
         float rx = (float) (Math.PI / 2);
         b.cyl(0.17F, 0.17F, 1.5F, 16, c("8b8f80"), 0.0F, 0.0F, 0.85F, rx, 0.0F, 0.0F);
         b.cyl(0.14F, 0.17F, 0.2F, 16, c("3a3a36"), 0.0F, 0.0F, 1.68F, rx, 0.0F, 0.0F);
         b.cyl(0.175F, 0.175F, 0.06F, 16, c("2a2a28"), 0.0F, 0.0F, 0.12F, rx, 0.0F, 0.0F);

         for (int i = 0; i < 4; i++) {
            float a = i * (float) Math.PI / 2.0F + (float) (Math.PI / 4);
            b.box(0.025F, 0.42F, 0.5F, c("7d8173"), MathUtils.cos(a) * 0.32F, MathUtils.sin(a) * 0.32F, 1.35F, 0.0F, 0.0F, a - (float) (Math.PI / 2));
         }

         b.cyl(0.09F, 0.09F, 1.55F, 14, c("d8d9d2"), 0.0F, 0.0F, -0.65F, rx, 0.0F, 0.0F);
         b.cyl(0.0F, 0.09F, 0.55F, 14, c("c4c6bd"), 0.0F, 0.0F, -1.7F, (float) (-Math.PI / 2), 0.0F, 0.0F);
         b.cyl(0.092F, 0.092F, 0.05F, 14, c("1e1e1e"), 0.0F, 0.0F, -1.1F, rx, 0.0F, 0.0F);

         for (int i = 0; i < 4; i++) {
            float a = i * (float) Math.PI / 2.0F;
            b.box(0.015F, 0.2F, 0.18F, c("b8bab2"), MathUtils.cos(a) * 0.16F, MathUtils.sin(a) * 0.16F, -1.15F, 0.0F, 0.0F, a - (float) (Math.PI / 2));
            b.box(0.015F, 0.16F, 0.3F, c("b8bab2"), MathUtils.cos(a) * 0.15F, MathUtils.sin(a) * 0.15F, 0.0F, 0.0F, 0.0F, a - (float) (Math.PI / 2));
         }

         missile = b.mesh();
         return missile;
      }
   }

   public static Models.Mog mog() {
      Node3 g = new Node3();
      Node3 gun = new Node3();
      Models.Parts P = new Models.Parts();
      MB C = P.g("root", "c");
      MB D = P.g("root", "d");
      MB M = P.g("root", "m");
      MB Gl = P.g("root", "g");
      MB L = P.g("root", "l");
      MB R = P.g("root", "r");
      D.box(1.5F, 0.35F, 5.0F, OLIVE_D, 0.0F, 0.62F, 0.0F);

      for (float z : new float[]{-1.75F, 1.75F}) {
         M.cyl(0.08F, 0.08F, 1.9F, 8, GUN, 0.0F, 0.55F, z, 0.0F, 0.0F, (float) (Math.PI / 2));

         for (float s : S2) {
            wheel(P, "root", s * 1.03F, 0.55F, z, 0.55F, 0.42F, s);
         }
      }

      C.extrude(
         new float[][]{{-2.8F, 0.0F}, {2.6F, 0.0F}, {2.85F, 0.35F}, {2.75F, 0.85F}, {1.3F, 1.0F}, {0.9F, 1.75F}, {-2.75F, 1.8F}, {-2.85F, 1.0F}},
         2.3F,
         WHITE,
         0.0F,
         0.75F,
         0.0F
      );

      for (float s : S2) {
         fender(C, -2.45F, -1.05F, s * 1.06F, 1.3F, 0.5F, WHITE);
         fender(C, 1.05F, 2.45F, s * 1.06F, 1.3F, 0.5F, WHITE);
         Gl.box(0.95F, 0.85F, 0.04F, GLASS, s * 0.55F, 2.125F, -1.13F, 0.49F, 0.0F, 0.0F);

         for (float z : new float[]{-0.4F, 0.6F}) {
            Gl.box(0.04F, 0.42F, 0.8F, GLASS, s * 1.155F, 2.12F, z);
         }

         D.box(0.03F, 1.0F, 0.03F, DARK, s * 1.153F, 1.7F, 0.1F);
         D.box(0.03F, 1.0F, 0.03F, DARK, s * 1.153F, 1.7F, 1.1F);
         M.box(0.05F, 0.05F, 0.2F, STEEL, s * 1.17F, 1.85F, -0.2F);
         M.box(0.05F, 0.05F, 0.2F, STEEL, s * 1.17F, 1.85F, 0.8F);
         L.box(0.24F, 0.16F, 0.05F, c("fff3d0"), s * 0.78F, 1.4F, -2.83F);
         L.box(0.16F, 0.1F, 0.05F, c("d0201a"), s * 0.9F, 1.6F, 2.86F);
         rail(M, V(s * 1.15F, 2.2F, -0.95F), V(s * 1.45F, 2.25F, -1.05F));
         D.box(0.06F, 0.3F, 0.2F, DARK, s * 1.48F, 2.15F, -1.05F);
         M.box(0.1F, 0.12F, 0.2F, GUN, s * 0.6F, 0.85F, -2.95F);
      }

      D.box(1.4F, 0.4F, 0.04F, c("1d1f1a"), 0.0F, 1.35F, -2.86F);

      for (int i = 0; i < 4; i++) {
         M.box(1.35F, 0.035F, 0.05F, GUN, 0.0F, 1.21F + i * 0.09F, -2.88F);
      }

      D.box(2.35F, 0.3F, 0.3F, OLIVE_D, 0.0F, 0.9F, -2.95F);
      M.cyl(0.14F, 0.14F, 0.7F, 10, GUN, 0.0F, 0.95F, -3.05F, 0.0F, 0.0F, (float) (Math.PI / 2));
      R.cyl(0.5F, 0.5F, 0.36F, 20, RUBBER, 0.0F, 1.75F, 3.0F, (float) (Math.PI / 2), 0.0F, 0.0F);
      D.cyl(0.3F, 0.3F, 0.38F, 16, OLIVE_D, 0.0F, 1.75F, 3.0F, (float) (Math.PI / 2), 0.0F, 0.0F);
      M.cyl(0.52F, 0.52F, 0.1F, 20, GUN, 0.0F, 2.6F, 0.3F);

      for (float x : new float[]{-0.95F, 0.95F}) {
         rail(M, V(x, 2.62F, 0.9F), V(x, 2.62F, 2.6F));
      }

      M.cyl(0.012F, 0.02F, 1.6F, 4, DARK, -0.9F, 3.35F, 2.5F);
      MB GC = P.g("gun", "c");
      MB GD = P.g("gun", "d");
      MB GM = P.g("gun", "m");
      GM.cyl(0.06F, 0.06F, 0.45F, 8, GUN, 0.0F, 0.22F, 0.0F);
      GC.box(1.2F, 0.55F, 0.06F, WHITE, 0.0F, 0.55F, -0.45F);

      for (float s : S2) {
         GC.box(0.45F, 0.55F, 0.06F, WHITE, s * 0.75F, 0.55F, -0.32F, 0.0F, s * 0.6F, 0.0F);
      }

      GM.box(0.16F, 0.2F, 0.75F, GUN, 0.0F, 0.55F, -0.05F);
      GM.seg(V(0.0F, 0.56F, -0.42F), V(0.0F, 0.56F, -1.5F), 0.035F, 0.03F, 8, GUN);
      GM.seg(V(0.0F, 0.56F, -1.45F), V(0.0F, 0.56F, -1.62F), 0.05F, 0.05F, 8, GUN);
      GD.box(0.2F, 0.2F, 0.3F, OLIVE_D, -0.22F, 0.5F, 0.0F);
      GM.box(0.04F, 0.12F, 0.06F, GUN, 0.1F, 0.5F, 0.33F);
      GM.box(0.04F, 0.12F, 0.06F, GUN, -0.1F, 0.5F, 0.33F);
      Color uni = c("4b5238");
      GD.seg(V(0.0F, 0.0F, 0.55F), V(0.0F, 0.55F, 0.55F), 0.2F, 0.24F, 10, uni);
      GD.sph(0.15F, c("c49a7a"), 0.0F, 0.74F, 0.55F, 1.0F, 1.0F, 1.0F, 10);
      GD.sph(0.18F, uni.cpy().mul(0.8F, 0.8F, 0.8F, 1.0F), 0.0F, 0.8F, 0.56F, 1.0F, 0.75F, 1.05F, 10);

      for (float s : S2) {
         GD.seg(V(s * 0.2F, 0.48F, 0.55F), V(s * 0.1F, 0.5F, 0.33F), 0.06F, 0.05F, 6, uni);
      }

      Map<String, Mat> mats = new HashMap<>();
      mats.put("c", camoMat("olive"));
      mats.put("d", MAT_PAINT);
      mats.put("m", MAT_METAL);
      mats.put("r", MAT_RUBBER);
      mats.put("g", MAT_GLASS);
      mats.put("l", MAT_LAMP);
      Map<String, Node3> nodes = new HashMap<>();
      nodes.put("root", g);
      nodes.put("gun", gun);

      for (Entry<String, Map<String, MB>> e : P.d.entrySet()) {
         for (Entry<String, MB> k : e.getValue().entrySet()) {
            MB b = k.getValue();
            if (!b.empty()) {
               b.noAo = k.getKey().equals("l") || k.getKey().equals("g");
               Node3 mi = new Node3(b.mesh(), mats.get(k.getKey()));
               mi.castShadow = !b.noAo;
               nodes.get(e.getKey()).add(mi);
            }
         }
      }

      gun.pos.set(0.0F, 2.62F, 0.3F);
      g.add(gun);
      Models.Mog m = new Models.Mog();
      m.g = g;
      m.gun = gun;
      return m;
   }

   private Models() {
   }

   static {
      SEG.put('0', "abcdef");
      SEG.put('1', "bc");
      SEG.put('2', "abged");
      SEG.put('3', "abgcd");
      SEG.put('4', "fgbc");
      SEG.put('5', "afgcd");
      SEG.put('6', "afgedc");
      SEG.put('7', "abc");
      SEG.put('8', "abcdefg");
      SEG.put('9', "abcdfg");
   }

   public static final class EnemyDef {
      public Mesh3 body;
      public Mesh3 glow;
      public Mesh3 rot;
      public float r;
      public float sc;
   }

   public static final class EnemyModel {
      public Node3 g;
      public Node3 body;
      public Node3 glow;
      public Node3 rot;
      public float r;
      public final Array<Node3> legs = new Array();
   }

   public static final class Mog {
      public Node3 g;
      public Node3 gun;
      public float t;
      public Object tgt;
      public float dmg;
   }

   static final class Parts {
      final Map<String, Map<String, MB>> d = new LinkedHashMap<>();

      MB g(String node, String key) {
         Map<String, MB> m = this.d.get(node);
         if (m == null) {
            m = new LinkedHashMap<>();
            this.d.put(node, m);
         }

         MB b = m.get(key);
         if (b == null) {
            b = new MB();
            m.put(key, b);
         }

         return b;
      }
   }

   public static final class Vehicle {
      public Node3 root;
      public Node3 turret;
      public Node3 pitch;
      public Node3 radar;
      public final Array<Vector3> barrels = new Array();
      public final Array<Vector3> noses = new Array();
      public final Array<Node3> noseMeshes = new Array();
      public boolean broken;
      public final Array<Object[]> wreck = new Array();
   }
}
