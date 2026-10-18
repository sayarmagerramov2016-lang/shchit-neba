package ru.shitneba.gfx;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

/** Узел сцены (аналог Node3D/MeshInstance3D). Вращение — матрицей. */
public class Node3 {
    public String name = "";
    public final Vector3 pos = new Vector3();
    public final Vector3 scl = new Vector3(1, 1, 1);
    public final Matrix4 rot = new Matrix4();
    public final Matrix4 global = new Matrix4();
    public Node3 parent;
    public final Array<Node3> kids = new Array<>(4);
    public Mesh3 mesh;
    public Mat mat;
    public Mat overlay;
    public boolean visible = true, castShadow = false;
    /** доп. множитель альфы (transparency из Godot = 1 - alpha) */
    public float alpha = 1f;
    /** пользовательское поле (тег) */
    public Object tag;
    /** вызывается перед отрисовкой (для эмиттеров и т.п.) */
    public Runnable onUpdate;

    static final Matrix4 tmp = new Matrix4();
    static final Quaternion q = new Quaternion();

    public Node3() { }

    public Node3(Mesh3 mesh, Mat mat) { this.mesh = mesh; this.mat = mat; }

    /** Глубокая копия поддерева; меши и материалы общие. */
    public Node3 cloneTree() {
        Node3 n = new Node3(mesh, mat);
        n.name = name; n.pos.set(pos); n.scl.set(scl); n.rot.set(rot);
        n.visible = visible; n.castShadow = castShadow; n.alpha = alpha; n.overlay = overlay; n.tag = tag;
        for (int i = 0; i < kids.size; i++) n.add(kids.get(i).cloneTree());
        return n;
    }

    public Node3 find(String nm) {
        if (nm.equals(name)) return this;
        for (int i = 0; i < kids.size; i++) { Node3 r = kids.get(i).find(nm); if (r != null) return r; }
        return null;
    }

    public Node3 add(Node3 c) {
        if (c.parent != null) c.parent.kids.removeValue(c, true);
        c.parent = this; kids.add(c); return c;
    }

    public void remove() {
        if (parent != null) parent.kids.removeValue(this, true);
        parent = null;
    }

    /** Godot rotation (порядок YXZ). */
    public Node3 setEuler(float x, float y, float z) {
        rot.idt();
        if (y != 0) rot.rotateRad(Vector3.Y, y);
        if (x != 0) rot.rotateRad(Vector3.X, x);
        if (z != 0) rot.rotateRad(Vector3.Z, z);
        return this;
    }

    public void rotateY(float a) { tmp.setToRotationRad(Vector3.Y, a); rot.mulLeft(tmp); }

    /** вращение в локальной системе */
    public void rotateLocal(Vector3 axis, float a) { rot.rotateRad(axis, a); }

    /** -Z смотрит вдоль dir (как look_at в Godot). */
    public void lookDir(Vector3 dir, Vector3 up) { basisLook(rot, dir, up); }

    public static void basisLook(Matrix4 out, Vector3 dir, Vector3 up) {
        float dx = dir.x, dy = dir.y, dz = dir.z;
        float l = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (l < 1e-6f) { out.idt(); return; }
        float zx = -dx / l, zy = -dy / l, zz = -dz / l;
        float xx = up.y * zz - up.z * zy, xy = up.z * zx - up.x * zz, xz = up.x * zy - up.y * zx;
        float xl = (float) Math.sqrt(xx * xx + xy * xy + xz * xz);
        if (xl < 1e-6f) { xx = 1; xy = 0; xz = 0; xl = 1; }
        xx /= xl; xy /= xl; xz /= xl;
        float yx = zy * xz - zz * xy, yy = zz * xx - zx * xz, yz = zx * xy - zy * xx;
        float[] v = out.val;
        v[Matrix4.M00] = xx; v[Matrix4.M10] = xy; v[Matrix4.M20] = xz; v[Matrix4.M30] = 0;
        v[Matrix4.M01] = yx; v[Matrix4.M11] = yy; v[Matrix4.M21] = yz; v[Matrix4.M31] = 0;
        v[Matrix4.M02] = zx; v[Matrix4.M12] = zy; v[Matrix4.M22] = zz; v[Matrix4.M32] = 0;
        v[Matrix4.M03] = 0; v[Matrix4.M13] = 0; v[Matrix4.M23] = 0; v[Matrix4.M33] = 1;
    }

    public Matrix4 local(Matrix4 out) {
        out.set(rot);
        out.val[Matrix4.M03] = pos.x; out.val[Matrix4.M13] = pos.y; out.val[Matrix4.M23] = pos.z;
        if (scl.x != 1 || scl.y != 1 || scl.z != 1) out.scale(scl.x, scl.y, scl.z);
        return out;
    }

    public void updateGlobal(Matrix4 parentG) {
        if (parentG == null) local(global);
        else { local(tmp); global.set(parentG).mul(tmp); }
        for (int i = 0; i < kids.size; i++) kids.get(i).updateGlobal(global);
    }

    /** Глобальная позиция (после updateGlobal). */
    public Vector3 gpos(Vector3 out) { return global.getTranslation(out); }

    /** точка из локальных координат этого узла в мировые (по текущей global) */
    public Vector3 toGlobal(Vector3 local, Vector3 out) { return out.set(local).mul(global); }

    /** Пересчитать global цепочкой предков (если нужно вне кадра). */
    public void refresh() {
        Array<Node3> chain = new Array<>();
        for (Node3 n = this; n != null; n = n.parent) chain.add(n);
        Matrix4 m = null;
        for (int i = chain.size - 1; i >= 0; i--) {
            Node3 n = chain.get(i);
            if (m == null) n.local(n.global); else { n.local(tmp); n.global.set(m).mul(tmp); }
            m = n.global;
        }
    }

    public interface Visitor { void visit(Node3 n); }

    public void forEach(Visitor f) {
        f.visit(this);
        for (int i = 0; i < kids.size; i++) kids.get(i).forEach(f);
    }
}
