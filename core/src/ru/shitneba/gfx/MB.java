package ru.shitneba.gfx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ShortArray;
import java.util.HashMap;
import java.util.Map;

public class MB {
   public static boolean hq = false;
   public boolean noAo = false;
   public final FloatArray v = new FloatArray(1024);
   static final Map<String, MB.Prim> cache = new HashMap<>();
   static final Vector3 ta = new Vector3();
   static final Vector3 tb = new Vector3();
   static final Vector3 tc = new Vector3();
   static final Vector3 tn = new Vector3();
   static final Matrix4 xm = new Matrix4();
   static final Matrix3 nm = new Matrix3();
   static final EarClippingTriangulator ECT = new EarClippingTriangulator();

   public int count() {
      return this.v.size / 12;
   }

   public boolean empty() {
      return this.v.size == 0;
   }

   static float[] vtx(float x, float y, float z, float nx, float ny, float nz, float u, float w) {
      float l = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
      if (l > 1.0E-6F) {
         nx /= l;
         ny /= l;
         nz /= l;
      }

      return new float[]{x, y, z, nx, ny, nz, u, w};
   }

   static MB.Prim boxPrim(float w, float h, float d) {
      MB.PB b = new MB.PB();
      float hx = w / 2.0F;
      float hy = h / 2.0F;
      float hz = d / 2.0F;

      for (int s = -1; s <= 1; s += 2) {
         b.quad(
            vtx(s * hx, -hy, -hz, s, 0.0F, 0.0F, 0.0F, 0.0F),
            vtx(s * hx, hy, -hz, s, 0.0F, 0.0F, 0.0F, 1.0F),
            vtx(s * hx, hy, hz, s, 0.0F, 0.0F, 1.0F, 1.0F),
            vtx(s * hx, -hy, hz, s, 0.0F, 0.0F, 1.0F, 0.0F)
         );
         b.quad(
            vtx(-hx, s * hy, -hz, 0.0F, s, 0.0F, 0.0F, 0.0F),
            vtx(hx, s * hy, -hz, 0.0F, s, 0.0F, 1.0F, 0.0F),
            vtx(hx, s * hy, hz, 0.0F, s, 0.0F, 1.0F, 1.0F),
            vtx(-hx, s * hy, hz, 0.0F, s, 0.0F, 0.0F, 1.0F)
         );
         b.quad(
            vtx(-hx, -hy, s * hz, 0.0F, 0.0F, s, 0.0F, 0.0F),
            vtx(hx, -hy, s * hz, 0.0F, 0.0F, s, 1.0F, 0.0F),
            vtx(hx, hy, s * hz, 0.0F, 0.0F, s, 1.0F, 1.0F),
            vtx(-hx, hy, s * hz, 0.0F, 0.0F, s, 0.0F, 1.0F)
         );
      }

      return b.done();
   }

   static MB.Prim bevelBoxPrim(float w, float h, float d, float bv) {
      MB.PB B = new MB.PB();
      float hx = w / 2.0F;
      float hy = h / 2.0F;
      float hz = d / 2.0F;
      float ex = hx - bv;
      float ey = hy - bv;
      float ez = hz - bv;
      float[] s2 = new float[]{-1.0F, 1.0F};

      for (float s : s2) {
         B.quad(
            vtx(s * hx, -ey, -ez, s, 0.0F, 0.0F, 0.0F, 0.0F),
            vtx(s * hx, ey, -ez, s, 0.0F, 0.0F, 0.0F, 0.0F),
            vtx(s * hx, ey, ez, s, 0.0F, 0.0F, 0.0F, 0.0F),
            vtx(s * hx, -ey, ez, s, 0.0F, 0.0F, 0.0F, 0.0F)
         );
         B.quad(
            vtx(-ex, s * hy, -ez, 0.0F, s, 0.0F, 0.0F, 0.0F),
            vtx(ex, s * hy, -ez, 0.0F, s, 0.0F, 0.0F, 0.0F),
            vtx(ex, s * hy, ez, 0.0F, s, 0.0F, 0.0F, 0.0F),
            vtx(-ex, s * hy, ez, 0.0F, s, 0.0F, 0.0F, 0.0F)
         );
         B.quad(
            vtx(-ex, -ey, s * hz, 0.0F, 0.0F, s, 0.0F, 0.0F),
            vtx(ex, -ey, s * hz, 0.0F, 0.0F, s, 0.0F, 0.0F),
            vtx(ex, ey, s * hz, 0.0F, 0.0F, s, 0.0F, 0.0F),
            vtx(-ex, ey, s * hz, 0.0F, 0.0F, s, 0.0F, 0.0F)
         );
      }

      for (float sa : s2) {
         for (float sb : s2) {
            B.quad(
               vtx(sa * hx, sb * ey, -ez, sa, sb, 0.0F, 0.0F, 0.0F),
               vtx(sa * ex, sb * hy, -ez, sa, sb, 0.0F, 0.0F, 0.0F),
               vtx(sa * ex, sb * hy, ez, sa, sb, 0.0F, 0.0F, 0.0F),
               vtx(sa * hx, sb * ey, ez, sa, sb, 0.0F, 0.0F, 0.0F)
            );
            B.quad(
               vtx(sa * hx, -ey, sb * ez, sa, 0.0F, sb, 0.0F, 0.0F),
               vtx(sa * ex, -ey, sb * hz, sa, 0.0F, sb, 0.0F, 0.0F),
               vtx(sa * ex, ey, sb * hz, sa, 0.0F, sb, 0.0F, 0.0F),
               vtx(sa * hx, ey, sb * ez, sa, 0.0F, sb, 0.0F, 0.0F)
            );
            B.quad(
               vtx(-ex, sa * hy, sb * ez, 0.0F, sa, sb, 0.0F, 0.0F),
               vtx(-ex, sa * ey, sb * hz, 0.0F, sa, sb, 0.0F, 0.0F),
               vtx(ex, sa * ey, sb * hz, 0.0F, sa, sb, 0.0F, 0.0F),
               vtx(ex, sa * hy, sb * ez, 0.0F, sa, sb, 0.0F, 0.0F)
            );
         }
      }

      for (float sx : s2) {
         for (float sy : s2) {
            for (float sz : s2) {
               B.tri(
                  vtx(sx * hx, sy * ey, sz * ez, sx, sy, sz, 0.0F, 0.0F),
                  vtx(sx * ex, sy * hy, sz * ez, sx, sy, sz, 0.0F, 0.0F),
                  vtx(sx * ex, sy * ey, sz * hz, sx, sy, sz, 0.0F, 0.0F)
               );
            }
         }
      }

      return B.done();
   }

   static MB.Prim cylPrim(float rt, float rb, float h, int seg, boolean caps) {
      MB.PB b = new MB.PB();
      float hy = h / 2.0F;
      float ny = (rb - rt) / h;

      for (int i = 0; i < seg; i++) {
         float u0 = (float)i / seg;
         float u1 = (float)(i + 1) / seg;
         float x0 = MathUtils.sin(u0 * (float) (Math.PI * 2));
         float z0 = MathUtils.cos(u0 * (float) (Math.PI * 2));
         float x1 = MathUtils.sin(u1 * (float) (Math.PI * 2));
         float z1 = MathUtils.cos(u1 * (float) (Math.PI * 2));
         float[] a = vtx(x0 * rt, hy, z0 * rt, x0, ny, z0, u0, 0.0F);
         float[] bb = vtx(x1 * rt, hy, z1 * rt, x1, ny, z1, u1, 0.0F);
         float[] c = vtx(x1 * rb, -hy, z1 * rb, x1, ny, z1, u1, 1.0F);
         float[] d = vtx(x0 * rb, -hy, z0 * rb, x0, ny, z0, u0, 1.0F);
         if (rt > 0.0F) {
            b.tri(a, bb, c);
         }

         if (rb > 0.0F) {
            b.tri(a, c, d);
         }

         if (caps) {
            if (rt > 0.0F) {
               b.tri(
                  vtx(0.0F, hy, 0.0F, 0.0F, 1.0F, 0.0F, 0.5F, 0.5F),
                  vtx(x0 * rt, hy, z0 * rt, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F),
                  vtx(x1 * rt, hy, z1 * rt, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F)
               );
            }

            if (rb > 0.0F) {
               b.tri(
                  vtx(0.0F, -hy, 0.0F, 0.0F, -1.0F, 0.0F, 0.5F, 0.5F),
                  vtx(x0 * rb, -hy, z0 * rb, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F),
                  vtx(x1 * rb, -hy, z1 * rb, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F)
               );
            }
         }
      }

      return b.done();
   }

   static MB.Prim sphPrim(float r, float height, int radial, int rings) {
      MB.PB b = new MB.PB();
      int rows = rings + 1;
      float hh = height / 2.0F;

      for (int j = 0; j < rows; j++) {
         float v0 = (float)j / rows;
         float v1 = (float)(j + 1) / rows;
         float w0 = MathUtils.sin((float) Math.PI * v0);
         float w1 = MathUtils.sin((float) Math.PI * v1);
         float y0 = MathUtils.cos((float) Math.PI * v0);
         float y1 = MathUtils.cos((float) Math.PI * v1);

         for (int i = 0; i < radial; i++) {
            float u0 = (float)i / radial;
            float u1 = (float)(i + 1) / radial;
            float sx0 = MathUtils.sin(u0 * (float) (Math.PI * 2));
            float cz0 = MathUtils.cos(u0 * (float) (Math.PI * 2));
            float sx1 = MathUtils.sin(u1 * (float) (Math.PI * 2));
            float cz1 = MathUtils.cos(u1 * (float) (Math.PI * 2));
            float[] a = vtx(sx0 * r * w0, y0 * hh, cz0 * r * w0, sx0 * w0, y0, cz0 * w0, u0, v0);
            float[] bb = vtx(sx1 * r * w0, y0 * hh, cz1 * r * w0, sx1 * w0, y0, cz1 * w0, u1, v0);
            float[] c = vtx(sx1 * r * w1, y1 * hh, cz1 * r * w1, sx1 * w1, y1, cz1 * w1, u1, v1);
            float[] d = vtx(sx0 * r * w1, y1 * hh, cz0 * r * w1, sx0 * w1, y1, cz0 * w1, u0, v1);
            if (j > 0) {
               b.tri(a, bb, c);
            }

            if (j < rows - 1) {
               b.tri(a, c, d);
            }
         }
      }

      return b.done();
   }

   static MB.Prim torusPrim(float R, float t, int rs, int ts) {
      MB.PB b = new MB.PB();

      for (int i = 0; i < rs; i++) {
         for (int j = 0; j < ts; j++) {
            float[][] q = new float[4][];
            int[][] ij = new int[][]{{i, j}, {i + 1, j}, {i + 1, j + 1}, {i, j + 1}};

            for (int k = 0; k < 4; k++) {
               float a = (float)ij[k][0] / rs * (float) (Math.PI * 2);
               float bb = (float)ij[k][1] / ts * (float) (Math.PI * 2);
               float ca = MathUtils.cos(a);
               float sa = MathUtils.sin(a);
               float cb = MathUtils.cos(bb);
               float sb = MathUtils.sin(bb);
               q[k] = vtx(ca * (R + cb * t), sb * t, sa * (R + cb * t), ca * cb, sb, sa * cb, 0.0F, 0.0F);
            }

            b.quad(q[0], q[1], q[2], q[3]);
         }
      }

      return b.done();
   }

   static MB.Prim capsulePrim(float r, float len, int radial) {
      MB.PB b = new MB.PB();
      int lat = 3;
      float half = len / 2.0F;
      int rows = lat * 2 + 1;
      float[][] ys = new float[rows + 1][2];
      float[] nys = new float[rows + 1];
      int k = 0;

      for (int j = 0; j <= lat; j++) {
         float a = (float) (Math.PI / 2) * (1.0F - (float)j / lat);
         ys[k][0] = half + MathUtils.sin(a) * r;
         ys[k][1] = MathUtils.cos(a);
         nys[k] = MathUtils.sin(a);
         k++;
      }

      for (int j = 0; j <= lat; j++) {
         float a = (float) (Math.PI / 2) * ((float)j / lat);
         ys[k][0] = -half - MathUtils.sin(a) * r;
         ys[k][1] = MathUtils.cos(a);
         nys[k] = -MathUtils.sin(a);
         k++;
      }

      for (int j = 0; j < k - 1; j++) {
         for (int i = 0; i < radial; i++) {
            float u0 = (float)i / radial * (float) (Math.PI * 2);
            float u1 = (float)(i + 1) / radial * (float) (Math.PI * 2);
            float s0 = MathUtils.sin(u0);
            float c0 = MathUtils.cos(u0);
            float s1 = MathUtils.sin(u1);
            float c1 = MathUtils.cos(u1);
            float[] a = vtx(s0 * r * ys[j][1], ys[j][0], c0 * r * ys[j][1], s0 * ys[j][1], nys[j], c0 * ys[j][1], 0.0F, 0.0F);
            float[] bb = vtx(s1 * r * ys[j][1], ys[j][0], c1 * r * ys[j][1], s1 * ys[j][1], nys[j], c1 * ys[j][1], 0.0F, 0.0F);
            float[] c = vtx(s1 * r * ys[j + 1][1], ys[j + 1][0], c1 * r * ys[j + 1][1], s1 * ys[j + 1][1], nys[j + 1], c1 * ys[j + 1][1], 0.0F, 0.0F);
            float[] d = vtx(s0 * r * ys[j + 1][1], ys[j + 1][0], c0 * r * ys[j + 1][1], s0 * ys[j + 1][1], nys[j + 1], c0 * ys[j + 1][1], 0.0F, 0.0F);
            if (ys[j][1] > 1.0E-4F) {
               b.tri(a, bb, c);
            }

            if (ys[j + 1][1] > 1.0E-4F) {
               b.tri(a, c, d);
            }
         }
      }

      return b.done();
   }

   static MB.Prim quadPrim(float w, float h) {
      MB.PB b = new MB.PB();
      b.quad(
         vtx(-w / 2.0F, -h / 2.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 1.0F),
         vtx(w / 2.0F, -h / 2.0F, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F),
         vtx(w / 2.0F, h / 2.0F, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F),
         vtx(-w / 2.0F, h / 2.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F)
      );
      return b.done();
   }

   static MB.Prim prim(String key, MB.PrimMaker mk) {
      MB.Prim p = cache.get(key);
      if (p == null) {
         p = mk.make();
         cache.put(key, p);
      }

      return p;
   }

   public static Matrix4 xf(Matrix4 out, float px, float py, float pz, float rx, float ry, float rz, float sx, float sy, float sz) {
      out.idt().translate(px, py, pz);
      if (rx != 0.0F) {
         out.rotateRad(Vector3.X, rx);
      }

      if (ry != 0.0F) {
         out.rotateRad(Vector3.Y, ry);
      }

      if (rz != 0.0F) {
         out.rotateRad(Vector3.Z, rz);
      }

      if (sx != 1.0F || sy != 1.0F || sz != 1.0F) {
         out.scale(sx, sy, sz);
      }

      return out;
   }

   public MB add(MB.Prim a, Color col, Matrix4 t, float uvk) {
      nm.set(t).inv().transpose();
      float[] m = t.val;
      float[] n3 = nm.val;
      float[] d = a.d;
      this.v.ensureCapacity(a.n * 12);

      for (int i = 0; i < a.n; i++) {
         int o = i * 8;
         float x = d[o];
         float y = d[o + 1];
         float z = d[o + 2];
         float nx = d[o + 3];
         float ny = d[o + 4];
         float nz = d[o + 5];
         float wx = m[0] * x + m[4] * y + m[8] * z + m[12];
         float wy = m[1] * x + m[5] * y + m[9] * z + m[13];
         float wz = m[2] * x + m[6] * y + m[10] * z + m[14];
         float tx = n3[0] * nx + n3[3] * ny + n3[6] * nz;
         float ty = n3[1] * nx + n3[4] * ny + n3[7] * nz;
         float tz = n3[2] * nx + n3[5] * ny + n3[8] * nz;
         float l = (float)Math.sqrt(tx * tx + ty * ty + tz * tz);
         if (l > 1.0E-8F) {
            tx /= l;
            ty /= l;
            tz /= l;
         }

         this.v.add(wx, wy, wz, tx);
         this.v.add(ty, tz, col.r, col.g);
         this.v.add(col.b, col.a, d[o + 6] * uvk, d[o + 7] * uvk);
      }

      return this;
   }

   public MB add(MB.Prim a, Color col, Matrix4 t) {
      return this.add(a, col, t, 1.0F);
   }

   public MB box(float w, float h, float d, Color col) {
      return this.box(w, h, d, col, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
   }

   public MB box(float w, float h, float d, Color col, float px, float py, float pz) {
      return this.box(w, h, d, col, px, py, pz, 0.0F, 0.0F, 0.0F);
   }

   public MB box(float w, float h, float d, Color col, float px, float py, float pz, float rx, float ry, float rz) {
      float mn = Math.min(w, Math.min(h, d));
      MB.Prim p;
      if (hq && mn > 0.035F) {
         float bv = MathUtils.clamp(mn * 0.16F, 0.006F, 0.09F);
         p = prim("bb" + w + "_" + h + "_" + d, () -> bevelBoxPrim(w, h, d, bv));
      } else {
         p = prim("b" + w + "_" + h + "_" + d, () -> boxPrim(w, h, d));
      }

      return this.add(p, col, xf(xm, px, py, pz, rx, ry, rz, 1.0F, 1.0F, 1.0F));
   }

   public MB box(float w, float h, float d, Color col, Vector3 p) {
      return this.box(w, h, d, col, p.x, p.y, p.z, 0.0F, 0.0F, 0.0F);
   }

   public MB box(float w, float h, float d, Color col, Vector3 p, Vector3 r) {
      return this.box(w, h, d, col, p.x, p.y, p.z, r.x, r.y, r.z);
   }

   public MB cyl(float rt, float rb, float h, int seg, Color col) {
      return this.cyl(rt, rb, h, seg, col, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
   }

   public MB cyl(float rt, float rb, float h, int seg, Color col, float px, float py, float pz) {
      return this.cyl(rt, rb, h, seg, col, px, py, pz, 0.0F, 0.0F, 0.0F);
   }

   public MB cyl(float rt, float rb, float h, int seg, Color col, float px, float py, float pz, float rx, float ry, float rz) {
      if (hq) {
         seg = MathUtils.clamp((int)(Math.max(rt, rb) * 46.0F), seg, 40);
      }

      int s = seg;
      MB.Prim p = prim("c" + rt + "_" + rb + "_" + h + "_" + seg, () -> cylPrim(rt, rb, h, s, true));
      return this.add(p, col, xf(xm, px, py, pz, rx, ry, rz, 1.0F, 1.0F, 1.0F));
   }

   public MB cyl(float rt, float rb, float h, int seg, Color col, Vector3 p) {
      return this.cyl(rt, rb, h, seg, col, p.x, p.y, p.z, 0.0F, 0.0F, 0.0F);
   }

   public MB cyl(float rt, float rb, float h, int seg, Color col, Vector3 p, Vector3 r) {
      return this.cyl(rt, rb, h, seg, col, p.x, p.y, p.z, r.x, r.y, r.z);
   }

   public MB sph(float r, Color col) {
      return this.sph(r, col, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 8);
   }

   public MB sph(float r, Color col, float px, float py, float pz) {
      return this.sph(r, col, px, py, pz, 1.0F, 1.0F, 1.0F, 8);
   }

   public MB sph(float r, Color col, float px, float py, float pz, float sx, float sy, float sz) {
      return this.sph(r, col, px, py, pz, sx, sy, sz, 8);
   }

   public MB sph(float r, Color col, float px, float py, float pz, float sx, float sy, float sz, int seg) {
      if (hq) {
         seg = MathUtils.clamp((int)(r * Math.max(sx, Math.max(sy, sz)) * 22.0F), seg, 32);
      }

      int s = seg;
      MB.Prim p = prim("s" + r + "_" + seg, () -> sphPrim(r, r * 2.0F, s, Math.max(3, s / 2)));
      return this.add(p, col, xf(xm, px, py, pz, 0.0F, 0.0F, 0.0F, sx, sy, sz));
   }

   public MB sph(float r, Color col, Vector3 p) {
      return this.sph(r, col, p.x, p.y, p.z, 1.0F, 1.0F, 1.0F, 8);
   }

   public MB sph(float r, Color col, Vector3 p, Vector3 s, int seg) {
      return this.sph(r, col, p.x, p.y, p.z, s.x, s.y, s.z, seg);
   }

   public MB torus(float R, float tube, Color col) {
      return this.torus(R, tube, col, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
   }

   public MB torus(float R, float tube, Color col, float px, float py, float pz) {
      return this.torus(R, tube, col, px, py, pz, 0.0F, 0.0F, 0.0F);
   }

   public MB torus(float R, float tube, Color col, float px, float py, float pz, float rx, float ry, float rz) {
      int rs = hq ? 40 : 24;
      int ts = hq ? 10 : 6;
      MB.Prim p = prim("t" + R + "_" + tube + "_" + rs, () -> torusPrim(R, tube, rs, ts));
      return this.add(p, col, xf(xm, px, py, pz, rx, ry, rz, 1.0F, 1.0F, 1.0F));
   }

   public MB capsule(float r, float len, Color col, float px, float py, float pz, float rx, float ry, float rz) {
      MB.Prim p = prim("p" + r + "_" + len, () -> capsulePrim(r, len, 6));
      return this.add(p, col, xf(xm, px, py, pz, rx, ry, rz, 1.0F, 1.0F, 1.0F));
   }

   public MB disc(float r, Color col, float px, float py, float pz) {
      return this.cyl(r, r, 0.02F, 16, col, px, py, pz, 0.0F, 0.0F, 0.0F);
   }

   public MB disc(float r, Color col, float px, float py, float pz, float rx, float ry, float rz) {
      return this.cyl(r, r, 0.02F, 16, col, px, py, pz, rx, ry, rz);
   }

   public MB quad(float w, float h, Color col, Matrix4 t) {
      return this.add(prim("q" + w + "_" + h, () -> quadPrim(w, h)), col, t);
   }

   public MB seg(Vector3 a, Vector3 b, float ra, float rb, int segs, Color col) {
      float dx = b.x - a.x;
      float dy = b.y - a.y;
      float dz = b.z - a.z;
      float L = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
      if (L < 1.0E-4F) {
         return this;
      } else {
         float yx = dx / L;
         float yy = dy / L;
         float yz = dz / L;
         float rx = 0.0F;
         float ry = 1.0F;
         float rz = 0.0F;
         if (Math.abs(yy) >= 0.95F) {
            rx = 1.0F;
            ry = 0.0F;
         }

         float xx = ry * yz - rz * yy;
         float xy = rz * yx - rx * yz;
         float xz = rx * yy - ry * yx;
         float xl = (float)Math.sqrt(xx * xx + xy * xy + xz * xz);
         xx /= xl;
         xy /= xl;
         xz /= xl;
         float zx = xy * yz - xz * yy;
         float zy = xz * yx - xx * yz;
         float zz = xx * yy - xy * yx;
         Matrix4 t = xm.idt();
         float[] m = t.val;
         m[0] = xx;
         m[1] = xy;
         m[2] = xz;
         m[4] = yx * L;
         m[5] = yy * L;
         m[6] = yz * L;
         m[8] = zx;
         m[9] = zy;
         m[10] = zz;
         m[12] = (a.x + b.x) / 2.0F;
         m[13] = (a.y + b.y) / 2.0F;
         m[14] = (a.z + b.z) / 2.0F;
         MB.Prim p = prim("c" + rb + "_" + ra + "_1.0_" + segs, () -> cylPrim(rb, ra, 1.0F, segs, true));
         return this.add(p, col, t);
      }
   }

   public MB extrude(float[][] pts, float depth, Color col, float px, float py, float pz, float rx, float ry, float rz) {
      MB.Prim p = extrudePrim(pts, depth, 0.3F);
      return this.add(p, col, xf(xm, px, py, pz, rx, ry, rz, 1.0F, 1.0F, 1.0F));
   }

   public MB extrude(float[][] pts, float depth, Color col, float px, float py, float pz) {
      return this.extrude(pts, depth, col, px, py, pz, 0.0F, 0.0F, 0.0F);
   }

   public MB extrude(float[][] pts, float depth, Color col) {
      return this.extrude(pts, depth, col, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
   }

   static MB.Prim extrudePrim(float[][] pts, float depth, float uvk) {
      int n = pts.length;
      float[] poly = new float[n * 2];

      for (int i = 0; i < n; i++) {
         poly[i * 2] = pts[i][0];
         poly[i * 2 + 1] = pts[i][1];
      }

      ShortArray tri = ECT.computeTriangles(poly);
      float hx = depth / 2.0F;
      MB.PB b = new MB.PB();

      for (int side = -1; side <= 1; side += 2) {
         for (int i = 0; i < tri.size; i += 3) {
            float[][] q = new float[3][];

            for (int k = 0; k < 3; k++) {
               int id = tri.get(i + k);
               q[k] = vtx(hx * side, poly[id * 2 + 1], -poly[id * 2], side, 0.0F, 0.0F, poly[id * 2] * uvk, poly[id * 2 + 1] * uvk);
            }

            b.tri(q[0], q[1], q[2]);
         }
      }

      float area = 0.0F;

      for (int i = 0; i < n; i++) {
         area += poly[i * 2] * poly[(i + 1) % n * 2 + 1] - poly[(i + 1) % n * 2] * poly[i * 2 + 1];
      }

      float sg = area > 0.0F ? 1.0F : -1.0F;

      for (int i = 0; i < n; i++) {
         float ax = poly[i * 2];
         float ay = poly[i * 2 + 1];
         float bx = poly[(i + 1) % n * 2];
         float by = poly[(i + 1) % n * 2 + 1];
         float ex = bx - ax;
         float ey = by - ay;
         float l = (float)Math.sqrt(ex * ex + ey * ey);
         if (!(l < 1.0E-6F)) {
            float n2x = ey / l * sg;
            float n2y = -ex / l * sg;
            float nnx = 0.0F;
            float nnz = -n2x;
            float L = l * uvk;
            b.quad(
               vtx(-hx, ay, -ax, nnx, n2y, nnz, 0.0F, 0.0F),
               vtx(-hx, by, -bx, nnx, n2y, nnz, L, 0.0F),
               vtx(hx, by, -bx, nnx, n2y, nnz, L, depth * uvk),
               vtx(hx, ay, -ax, nnx, n2y, nnz, 0.0F, depth * uvk)
            );
         }
      }

      return b.done();
   }

   public MB wing(float span, float length, float th, Color col) {
      return this.wing(span, length, th, col, 0.0F, 0.0F, 0.0F);
   }

   public MB wing(float span, float length, float th, Color col, float px, float py, float pz) {
      float[][] pts = new float[][]{
         {length * 0.5F, 0.0F},
         {-length * 0.32F, span / 2.0F},
         {-length * 0.5F, span * 0.42F},
         {-length * 0.28F, 0.0F},
         {-length * 0.5F, -span * 0.42F},
         {-length * 0.32F, -span / 2.0F}
      };
      MB.Prim p = extrudePrim(pts, th, 0.3F);
      return this.add(p, col, xf(xm, px, py, pz, 0.0F, 0.0F, (float) (Math.PI / 2), 1.0F, 1.0F, 1.0F));
   }

   public void bakeAo(float strength) {
      if (this.v.size != 0) {
         float y0 = 1.0E9F;
         float y1 = -1.0E9F;
         float[] a = this.v.items;

         for (int i = 0; i < this.v.size; i += 12) {
            y0 = Math.min(y0, a[i + 1]);
            y1 = Math.max(y1, a[i + 1]);
         }

         float hgt = Math.max(0.01F, y1 - y0);

         for (int i = 0; i < this.v.size; i += 12) {
            float ny = a[i + 4];
            float t = MathUtils.clamp((ny + 1.0F) / 1.7F, 0.0F, 1.0F);
            float sm = t * t * (3.0F - 2.0F * t);
            float face = MathUtils.lerp(0.66F, 1.08F, sm);
            float vert = MathUtils.lerp(0.8F, 1.04F, MathUtils.clamp((a[i + 1] - y0) / hgt, 0.0F, 1.0F));
            float k = MathUtils.lerp(1.0F, face * vert, strength);
            a[i + 6] = a[i + 6] * k;
            a[i + 7] = a[i + 7] * k;
            a[i + 8] = a[i + 8] * k;
         }
      }
   }

   public Mesh3 mesh() {
      if (hq && !this.noAo) {
         this.bakeAo(1.0F);
      }

      return new Mesh3(this.v.items, this.v.size);
   }

   public MB append(MB o) {
      this.v.addAll(o.v);
      return this;
   }

   public static void clearCache() {
      cache.clear();
   }

   public static MB.Prim pBox(float w, float h, float d) {
      return prim("b" + w + "_" + h + "_" + d, () -> boxPrim(w, h, d));
   }

   public static MB.Prim pCyl(float rt, float rb, float h, int seg, boolean caps) {
      return prim("c" + rt + "_" + rb + "_" + h + "_" + seg + (caps ? "" : "nc"), () -> cylPrim(rt, rb, h, seg, caps));
   }

   public static MB.Prim pSph(float r, float h, int radial, int rings) {
      return prim("S" + r + "_" + h + "_" + radial + "_" + rings, () -> sphPrim(r, h, radial, rings));
   }

   public static MB.Prim pQuad(float w, float h) {
      return prim("q" + w + "_" + h, () -> quadPrim(w, h));
   }

   static final class PB {
      final FloatArray f = new FloatArray(256);

      void tri(float[] a, float[] b, float[] c) {
         float ex = b[0] - a[0];
         float ey = b[1] - a[1];
         float ez = b[2] - a[2];
         float fx = c[0] - a[0];
         float fy = c[1] - a[1];
         float fz = c[2] - a[2];
         float cx = ey * fz - ez * fy;
         float cy = ez * fx - ex * fz;
         float cz = ex * fy - ey * fx;
         float nx = a[3] + b[3] + c[3];
         float ny = a[4] + b[4] + c[4];
         float nz = a[5] + b[5] + c[5];
         if (cx * nx + cy * ny + cz * nz > 0.0F) {
            float[] t = b;
            b = c;
            c = t;
         }

         this.put(a);
         this.put(b);
         this.put(c);
      }

      void put(float[] p) {
         this.f.add(p[0], p[1], p[2], p[3]);
         this.f.add(p[4], p[5], p[6], p[7]);
      }

      void quad(float[] a, float[] b, float[] c, float[] d) {
         this.tri(a, b, c);
         this.tri(a, c, d);
      }

      MB.Prim done() {
         MB.Prim p = new MB.Prim();
         p.d = this.f.toArray();
         p.n = p.d.length / 8;
         return p;
      }
   }

   public static final class Prim {
      public float[] d;
      public int n;
   }

   interface PrimMaker {
      MB.Prim make();
   }
}
