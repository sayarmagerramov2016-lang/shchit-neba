package ru.shitneba.gfx;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

public class Node3 {
   public String name = "";
   public final Vector3 pos = new Vector3();
   public final Vector3 scl = new Vector3(1.0F, 1.0F, 1.0F);
   public final Matrix4 rot = new Matrix4();
   public final Matrix4 global = new Matrix4();
   public Node3 parent;
   public final Array<Node3> kids = new Array(4);
   public Mesh3 mesh;
   public Mat mat;
   public Mat overlay;
   public boolean visible = true;
   public boolean castShadow = false;
   public float alpha = 1.0F;
   public Object tag;
   public Runnable onUpdate;
   static final Matrix4 tmp = new Matrix4();
   static final Quaternion q = new Quaternion();

   public Node3() {
   }

   public Node3(Mesh3 mesh, Mat mat) {
      this.mesh = mesh;
      this.mat = mat;
   }

   public Node3 cloneTree() {
      Node3 n = new Node3(this.mesh, this.mat);
      n.name = this.name;
      n.pos.set(this.pos);
      n.scl.set(this.scl);
      n.rot.set(this.rot);
      n.visible = this.visible;
      n.castShadow = this.castShadow;
      n.alpha = this.alpha;
      n.overlay = this.overlay;
      n.tag = this.tag;

      for (int i = 0; i < this.kids.size; i++) {
         n.add(((Node3)this.kids.get(i)).cloneTree());
      }

      return n;
   }

   public Node3 find(String nm) {
      if (nm.equals(this.name)) {
         return this;
      } else {
         for (int i = 0; i < this.kids.size; i++) {
            Node3 r = ((Node3)this.kids.get(i)).find(nm);
            if (r != null) {
               return r;
            }
         }

         return null;
      }
   }

   public Node3 add(Node3 c) {
      if (c.parent != null) {
         c.parent.kids.removeValue(c, true);
      }

      c.parent = this;
      this.kids.add(c);
      return c;
   }

   public void remove() {
      if (this.parent != null) {
         this.parent.kids.removeValue(this, true);
      }

      this.parent = null;
   }

   public Node3 setEuler(float x, float y, float z) {
      this.rot.idt();
      if (y != 0.0F) {
         this.rot.rotateRad(Vector3.Y, y);
      }

      if (x != 0.0F) {
         this.rot.rotateRad(Vector3.X, x);
      }

      if (z != 0.0F) {
         this.rot.rotateRad(Vector3.Z, z);
      }

      return this;
   }

   public void rotateY(float a) {
      tmp.setToRotationRad(Vector3.Y, a);
      this.rot.mulLeft(tmp);
   }

   public void rotateLocal(Vector3 axis, float a) {
      this.rot.rotateRad(axis, a);
   }

   public void lookDir(Vector3 dir, Vector3 up) {
      basisLook(this.rot, dir, up);
   }

   public static void basisLook(Matrix4 out, Vector3 dir, Vector3 up) {
      float dx = dir.x;
      float dy = dir.y;
      float dz = dir.z;
      float l = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
      if (l < 1.0E-6F) {
         out.idt();
      } else {
         float zx = -dx / l;
         float zy = -dy / l;
         float zz = -dz / l;
         float xx = up.y * zz - up.z * zy;
         float xy = up.z * zx - up.x * zz;
         float xz = up.x * zy - up.y * zx;
         float xl = (float)Math.sqrt(xx * xx + xy * xy + xz * xz);
         if (xl < 1.0E-6F) {
            xx = 1.0F;
            xy = 0.0F;
            xz = 0.0F;
            xl = 1.0F;
         }

         xx /= xl;
         xy /= xl;
         xz /= xl;
         float yx = zy * xz - zz * xy;
         float yy = zz * xx - zx * xz;
         float yz = zx * xy - zy * xx;
         float[] v = out.val;
         v[0] = xx;
         v[1] = xy;
         v[2] = xz;
         v[3] = 0.0F;
         v[4] = yx;
         v[5] = yy;
         v[6] = yz;
         v[7] = 0.0F;
         v[8] = zx;
         v[9] = zy;
         v[10] = zz;
         v[11] = 0.0F;
         v[12] = 0.0F;
         v[13] = 0.0F;
         v[14] = 0.0F;
         v[15] = 1.0F;
      }
   }

   public Matrix4 local(Matrix4 out) {
      out.set(this.rot);
      out.val[12] = this.pos.x;
      out.val[13] = this.pos.y;
      out.val[14] = this.pos.z;
      if (this.scl.x != 1.0F || this.scl.y != 1.0F || this.scl.z != 1.0F) {
         out.scale(this.scl.x, this.scl.y, this.scl.z);
      }

      return out;
   }

   public void updateGlobal(Matrix4 parentG) {
      if (parentG == null) {
         this.local(this.global);
      } else {
         this.local(tmp);
         this.global.set(parentG).mul(tmp);
      }

      for (int i = 0; i < this.kids.size; i++) {
         ((Node3)this.kids.get(i)).updateGlobal(this.global);
      }
   }

   public Vector3 gpos(Vector3 out) {
      return this.global.getTranslation(out);
   }

   public Vector3 toGlobal(Vector3 local, Vector3 out) {
      return out.set(local).mul(this.global);
   }

   public void refresh() {
      Array<Node3> chain = new Array();

      for (Node3 n = this; n != null; n = n.parent) {
         chain.add(n);
      }

      Matrix4 m = null;

      for (int i = chain.size - 1; i >= 0; i--) {
         Node3 n = (Node3)chain.get(i);
         if (m == null) {
            n.local(n.global);
         } else {
            n.local(tmp);
            n.global.set(m).mul(tmp);
         }

         m = n.global;
      }
   }

   public void forEach(Node3.Visitor f) {
      f.visit(this);

      for (int i = 0; i < this.kids.size; i++) {
         ((Node3)this.kids.get(i)).forEach(f);
      }
   }

   public interface Visitor {
      void visit(Node3 var1);
   }
}
