package ru.shitneba.gfx;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.FloatArray;

public class Dyn implements Disposable {
   public final Mat mat;
   public final FloatArray v = new FloatArray(4096);
   Mesh mesh;
   int cap;
   public static final MB.Prim BOX = MB.boxPrim(1.0F, 1.0F, 1.0F);
   public static final MB.Prim CYL8 = MB.cylPrim(1.0F, 1.0F, 1.0F, 8, false);

   public Dyn(Mat mat) {
      this.mat = mat;
   }

   public int count() {
      return this.v.size / 12;
   }

   public void clear() {
      this.v.clear();
   }

   public void vert(float x, float y, float z, float nx, float ny, float nz, float r, float g, float b, float a, float u, float w) {
      this.v.add(x, y, z, nx);
      this.v.add(ny, nz, r, g);
      this.v.add(b, a, u, w);
   }

   public void billboard(Vector3 p, float size, Vector3 right, Vector3 up, float r, float g, float b, float a, float rot) {
      float hs = size * 0.5F;
      float c = (float)Math.cos(rot);
      float s = (float)Math.sin(rot);
      float rx = (right.x * c + up.x * s) * hs;
      float ry = (right.y * c + up.y * s) * hs;
      float rz = (right.z * c + up.z * s) * hs;
      float ux = (-right.x * s + up.x * c) * hs;
      float uy = (-right.y * s + up.y * c) * hs;
      float uz = (-right.z * s + up.z * c) * hs;
      float ax = p.x - rx - ux;
      float ay = p.y - ry - uy;
      float az = p.z - rz - uz;
      float bx = p.x + rx - ux;
      float by = p.y + ry - uy;
      float bz = p.z + rz - uz;
      float cx = p.x + rx + ux;
      float cy = p.y + ry + uy;
      float cz = p.z + rz + uz;
      float dx = p.x - rx + ux;
      float dy = p.y - ry + uy;
      float dz = p.z - rz + uz;
      this.vert(ax, ay, az, 0.0F, 1.0F, 0.0F, r, g, b, a, 0.0F, 1.0F);
      this.vert(dx, dy, dz, 0.0F, 1.0F, 0.0F, r, g, b, a, 0.0F, 0.0F);
      this.vert(cx, cy, cz, 0.0F, 1.0F, 0.0F, r, g, b, a, 1.0F, 0.0F);
      this.vert(ax, ay, az, 0.0F, 1.0F, 0.0F, r, g, b, a, 0.0F, 1.0F);
      this.vert(cx, cy, cz, 0.0F, 1.0F, 0.0F, r, g, b, a, 1.0F, 0.0F);
      this.vert(bx, by, bz, 0.0F, 1.0F, 0.0F, r, g, b, a, 1.0F, 1.0F);
   }

   public void prim(MB.Prim p, Matrix4 t, float r, float g, float b, float a) {
      float[] m = t.val;
      float[] d = p.d;

      for (int i = 0; i < p.n; i++) {
         int o = i * 8;
         float x = d[o];
         float y = d[o + 1];
         float z = d[o + 2];
         float nx = d[o + 3];
         float ny = d[o + 4];
         float nz = d[o + 5];
         this.vert(
            m[0] * x + m[4] * y + m[8] * z + m[12],
            m[1] * x + m[5] * y + m[9] * z + m[13],
            m[2] * x + m[6] * y + m[10] * z + m[14],
            m[0] * nx + m[4] * ny + m[8] * nz,
            m[1] * nx + m[5] * ny + m[9] * nz,
            m[2] * nx + m[6] * ny + m[10] * nz,
            r,
            g,
            b,
            a,
            d[o + 6],
            d[o + 7]
         );
      }
   }

   void flushTo(ShaderProgram p) {
      int n = this.count();
      if (n != 0) {
         if (this.mesh == null || this.cap < n) {
            if (this.mesh != null) {
               this.mesh.dispose();
            }

            this.cap = Math.max(n + 256, this.cap * 2);
            this.mesh = new Mesh(false, this.cap, 0, Mesh3.ATTRS);
         }

         this.mesh.setVertices(this.v.items, 0, this.v.size);
         this.mesh.render(p, 4, 0, n);
      }
   }

   public void dispose() {
      if (this.mesh != null) {
         this.mesh.dispose();
      }
   }
}
