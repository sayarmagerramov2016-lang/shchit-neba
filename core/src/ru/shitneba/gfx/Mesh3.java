package ru.shitneba.gfx;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Array.ArrayIterator;

public class Mesh3 implements Disposable {
   public static final int STRIDE = 12;
   public static final VertexAttributes ATTRS = new VertexAttributes(
      new VertexAttribute[]{
         new VertexAttribute(1, 3, "a_position"),
         new VertexAttribute(8, 3, "a_normal"),
         new VertexAttribute(2, 4, "a_color"),
         new VertexAttribute(16, 2, "a_texCoord0")
      }
   );
   static final Array<Mesh3> ALL = new Array();
   public final Mesh mesh;
   public final int count;
   public final Vector3 center = new Vector3();
   public float radius;

   public Mesh3(float[] v, int floats) {
      this.count = floats / 12;
      this.mesh = new Mesh(true, Math.max(1, this.count), 0, ATTRS);
      this.mesh.setVertices(v, 0, floats);
      float mnx = 1.0E9F;
      float mny = 1.0E9F;
      float mnz = 1.0E9F;
      float mxx = -1.0E9F;
      float mxy = -1.0E9F;
      float mxz = -1.0E9F;

      for (int i = 0; i < floats; i += 12) {
         mnx = Math.min(mnx, v[i]);
         mny = Math.min(mny, v[i + 1]);
         mnz = Math.min(mnz, v[i + 2]);
         mxx = Math.max(mxx, v[i]);
         mxy = Math.max(mxy, v[i + 1]);
         mxz = Math.max(mxz, v[i + 2]);
      }

      if (this.count == 0) {
         mxz = 0.0F;
         mxy = 0.0F;
         mxx = 0.0F;
         mnz = 0.0F;
         mny = 0.0F;
         mnx = 0.0F;
      }

      this.center.set((mnx + mxx) / 2.0F, (mny + mxy) / 2.0F, (mnz + mxz) / 2.0F);
      float r = 0.0F;

      for (int i = 0; i < floats; i += 12) {
         r = Math.max(r, this.center.dst2(v[i], v[i + 1], v[i + 2]));
      }

      this.radius = (float)Math.sqrt(r);
      ALL.add(this);
   }

   public void dispose() {
      this.mesh.dispose();
      ALL.removeValue(this, true);
   }

   public static void disposeAll() {
      ArrayIterator var0 = new Array(ALL).iterator();

      while (var0.hasNext()) {
         Mesh3 m = (Mesh3)var0.next();
         m.dispose();
      }
   }
}
