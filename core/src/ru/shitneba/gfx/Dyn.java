package ru.shitneba.gfx;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.FloatArray;

/** Динамическая геометрия, пересобираемая каждый кадр (частицы, трассеры, шлейфы). */
public class Dyn implements Disposable {
    public final Mat mat;
    public final FloatArray v = new FloatArray(4096);
    Mesh mesh;
    int cap;

    public Dyn(Mat mat) { this.mat = mat; }

    public int count() { return v.size / Mesh3.STRIDE; }

    public void clear() { v.clear(); }

    public void vert(float x, float y, float z, float nx, float ny, float nz, float r, float g, float b, float a, float u, float w) {
        v.add(x, y, z, nx); v.add(ny, nz, r, g); v.add(b, a, u, w);
    }

    /** Билборд-квад, обращённый к камере. right/up — оси камеры. Порядок CW. */
    public void billboard(Vector3 p, float size, Vector3 right, Vector3 up, float r, float g, float b, float a, float rot) {
        float hs = size * 0.5f;
        float c = (float) Math.cos(rot), s = (float) Math.sin(rot);
        float rx = (right.x * c + up.x * s) * hs, ry = (right.y * c + up.y * s) * hs, rz = (right.z * c + up.z * s) * hs;
        float ux = (-right.x * s + up.x * c) * hs, uy = (-right.y * s + up.y * c) * hs, uz = (-right.z * s + up.z * c) * hs;
        float ax = p.x - rx - ux, ay = p.y - ry - uy, az = p.z - rz - uz;
        float bx = p.x + rx - ux, by = p.y + ry - uy, bz = p.z + rz - uz;
        float cx = p.x + rx + ux, cy = p.y + ry + uy, cz = p.z + rz + uz;
        float dx = p.x - rx + ux, dy = p.y - ry + uy, dz = p.z - rz + uz;
        // CW при взгляде с камеры: a, d, c / a, c, b
        vert(ax, ay, az, 0, 1, 0, r, g, b, a, 0, 1); vert(dx, dy, dz, 0, 1, 0, r, g, b, a, 0, 0); vert(cx, cy, cz, 0, 1, 0, r, g, b, a, 1, 0);
        vert(ax, ay, az, 0, 1, 0, r, g, b, a, 0, 1); vert(cx, cy, cz, 0, 1, 0, r, g, b, a, 1, 0); vert(bx, by, bz, 0, 1, 0, r, g, b, a, 1, 1);
    }

    /** Добавить готовый меш-примитив, трансформированный матрицей (для трассеров/обломков). */
    public void prim(MB.Prim p, Matrix4 t, float r, float g, float b, float a) {
        float[] m = t.val, d = p.d;
        for (int i = 0; i < p.n; i++) {
            int o = i * 8;
            float x = d[o], y = d[o + 1], z = d[o + 2];
            float nx = d[o + 3], ny = d[o + 4], nz = d[o + 5];
            vert(m[Matrix4.M00] * x + m[Matrix4.M01] * y + m[Matrix4.M02] * z + m[Matrix4.M03],
                    m[Matrix4.M10] * x + m[Matrix4.M11] * y + m[Matrix4.M12] * z + m[Matrix4.M13],
                    m[Matrix4.M20] * x + m[Matrix4.M21] * y + m[Matrix4.M22] * z + m[Matrix4.M23],
                    m[Matrix4.M00] * nx + m[Matrix4.M01] * ny + m[Matrix4.M02] * nz,
                    m[Matrix4.M10] * nx + m[Matrix4.M11] * ny + m[Matrix4.M12] * nz,
                    m[Matrix4.M20] * nx + m[Matrix4.M21] * ny + m[Matrix4.M22] * nz,
                    r, g, b, a, d[o + 6], d[o + 7]);
        }
    }

    void flushTo(ShaderProgram p) {
        int n = count();
        if (n == 0) return;
        if (mesh == null || cap < n) {
            if (mesh != null) mesh.dispose();
            cap = Math.max(n + 256, cap * 2);
            mesh = new Mesh(false, cap, 0, Mesh3.ATTRS);
        }
        mesh.setVertices(v.items, 0, v.size);
        mesh.render(p, GL20.GL_TRIANGLES, 0, n);
    }

    @Override
    public void dispose() { if (mesh != null) mesh.dispose(); }

    // ---- общие примитивы для динамики ----
    public static final MB.Prim BOX = MB.boxPrim(1, 1, 1);
    public static final MB.Prim CYL8 = MB.cylPrim(1, 1, 1, 8, false);
}
