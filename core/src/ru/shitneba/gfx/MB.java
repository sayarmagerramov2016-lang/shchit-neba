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

/**
 * Построитель мешей с вершинным цветом — порт mb.gd.
 * Все примитивы повторяют геометрию примитивов Godot (BoxMesh, CylinderMesh, SphereMesh...).
 * Передние грани — по часовой стрелке (как в Godot).
 */
public class MB {
    public static boolean hq = false;
    public boolean noAo = false;
    public final FloatArray v = new FloatArray(1024);

    /** Примитив: треугольники pos3 nrm3 uv2 (8 float на вершину). */
    public static final class Prim { public float[] d; public int n; }

    static final Map<String, Prim> cache = new HashMap<>();
    static final Vector3 ta = new Vector3(), tb = new Vector3(), tc = new Vector3(), tn = new Vector3();

    public int count() { return v.size / Mesh3.STRIDE; }
    public boolean empty() { return v.size == 0; }

    // ---------- построение примитивов ----------
    static final class PB {
        final FloatArray f = new FloatArray(256);

        /** треугольник с нормалями; порядок приводится к CW относительно суммарной нормали */
        void tri(float[] a, float[] b, float[] c) {
            float ex = b[0] - a[0], ey = b[1] - a[1], ez = b[2] - a[2];
            float fx = c[0] - a[0], fy = c[1] - a[1], fz = c[2] - a[2];
            float cx = ey * fz - ez * fy, cy = ez * fx - ex * fz, cz = ex * fy - ey * fx;
            float nx = a[3] + b[3] + c[3], ny = a[4] + b[4] + c[4], nz = a[5] + b[5] + c[5];
            if (cx * nx + cy * ny + cz * nz > 0) { float[] t = b; b = c; c = t; }
            put(a); put(b); put(c);
        }

        void put(float[] p) { f.add(p[0], p[1], p[2], p[3]); f.add(p[4], p[5], p[6], p[7]); }

        void quad(float[] a, float[] b, float[] c, float[] d) { tri(a, b, c); tri(a, c, d); }

        Prim done() { Prim p = new Prim(); p.d = f.toArray(); p.n = p.d.length / 8; return p; }
    }

    static float[] vtx(float x, float y, float z, float nx, float ny, float nz, float u, float w) {
        float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (l > 1e-6f) { nx /= l; ny /= l; nz /= l; }
        return new float[]{x, y, z, nx, ny, nz, u, w};
    }

    static Prim boxPrim(float w, float h, float d) {
        PB b = new PB();
        float hx = w / 2, hy = h / 2, hz = d / 2;
        for (int s = -1; s <= 1; s += 2) {
            b.quad(vtx(s * hx, -hy, -hz, s, 0, 0, 0, 0), vtx(s * hx, hy, -hz, s, 0, 0, 0, 1), vtx(s * hx, hy, hz, s, 0, 0, 1, 1), vtx(s * hx, -hy, hz, s, 0, 0, 1, 0));
            b.quad(vtx(-hx, s * hy, -hz, 0, s, 0, 0, 0), vtx(hx, s * hy, -hz, 0, s, 0, 1, 0), vtx(hx, s * hy, hz, 0, s, 0, 1, 1), vtx(-hx, s * hy, hz, 0, s, 0, 0, 1));
            b.quad(vtx(-hx, -hy, s * hz, 0, 0, s, 0, 0), vtx(hx, -hy, s * hz, 0, 0, s, 1, 0), vtx(hx, hy, s * hz, 0, 0, s, 1, 1), vtx(-hx, hy, s * hz, 0, 0, s, 0, 1));
        }
        return b.done();
    }

    static Prim bevelBoxPrim(float w, float h, float d, float bv) {
        PB B = new PB();
        float hx = w / 2, hy = h / 2, hz = d / 2;
        float ex = hx - bv, ey = hy - bv, ez = hz - bv;
        float[] s2 = {-1, 1};
        for (float s : s2) {
            B.quad(vtx(s * hx, -ey, -ez, s, 0, 0, 0, 0), vtx(s * hx, ey, -ez, s, 0, 0, 0, 0), vtx(s * hx, ey, ez, s, 0, 0, 0, 0), vtx(s * hx, -ey, ez, s, 0, 0, 0, 0));
            B.quad(vtx(-ex, s * hy, -ez, 0, s, 0, 0, 0), vtx(ex, s * hy, -ez, 0, s, 0, 0, 0), vtx(ex, s * hy, ez, 0, s, 0, 0, 0), vtx(-ex, s * hy, ez, 0, s, 0, 0, 0));
            B.quad(vtx(-ex, -ey, s * hz, 0, 0, s, 0, 0), vtx(ex, -ey, s * hz, 0, 0, s, 0, 0), vtx(ex, ey, s * hz, 0, 0, s, 0, 0), vtx(-ex, ey, s * hz, 0, 0, s, 0, 0));
        }
        for (float sa : s2) for (float sb : s2) {
            B.quad(vtx(sa * hx, sb * ey, -ez, sa, sb, 0, 0, 0), vtx(sa * ex, sb * hy, -ez, sa, sb, 0, 0, 0), vtx(sa * ex, sb * hy, ez, sa, sb, 0, 0, 0), vtx(sa * hx, sb * ey, ez, sa, sb, 0, 0, 0));
            B.quad(vtx(sa * hx, -ey, sb * ez, sa, 0, sb, 0, 0), vtx(sa * ex, -ey, sb * hz, sa, 0, sb, 0, 0), vtx(sa * ex, ey, sb * hz, sa, 0, sb, 0, 0), vtx(sa * hx, ey, sb * ez, sa, 0, sb, 0, 0));
            B.quad(vtx(-ex, sa * hy, sb * ez, 0, sa, sb, 0, 0), vtx(-ex, sa * ey, sb * hz, 0, sa, sb, 0, 0), vtx(ex, sa * ey, sb * hz, 0, sa, sb, 0, 0), vtx(ex, sa * hy, sb * ez, 0, sa, sb, 0, 0));
        }
        for (float sx : s2) for (float sy : s2) for (float sz : s2)
            B.tri(vtx(sx * hx, sy * ey, sz * ez, sx, sy, sz, 0, 0), vtx(sx * ex, sy * hy, sz * ez, sx, sy, sz, 0, 0), vtx(sx * ex, sy * ey, sz * hz, sx, sy, sz, 0, 0));
        return B.done();
    }

    static Prim cylPrim(float rt, float rb, float h, int seg, boolean caps) {
        PB b = new PB();
        float hy = h / 2, ny = (rb - rt) / h;
        for (int i = 0; i < seg; i++) {
            float u0 = (float) i / seg, u1 = (float) (i + 1) / seg;
            float x0 = MathUtils.sin(u0 * MathUtils.PI2), z0 = MathUtils.cos(u0 * MathUtils.PI2);
            float x1 = MathUtils.sin(u1 * MathUtils.PI2), z1 = MathUtils.cos(u1 * MathUtils.PI2);
            float[] a = vtx(x0 * rt, hy, z0 * rt, x0, ny, z0, u0, 0);
            float[] bb = vtx(x1 * rt, hy, z1 * rt, x1, ny, z1, u1, 0);
            float[] c = vtx(x1 * rb, -hy, z1 * rb, x1, ny, z1, u1, 1);
            float[] d = vtx(x0 * rb, -hy, z0 * rb, x0, ny, z0, u0, 1);
            if (rt > 0) b.tri(a, bb, c);
            if (rb > 0) b.tri(a, c, d);
            if (caps) {
                if (rt > 0) b.tri(vtx(0, hy, 0, 0, 1, 0, 0.5f, 0.5f), vtx(x0 * rt, hy, z0 * rt, 0, 1, 0, 0, 0), vtx(x1 * rt, hy, z1 * rt, 0, 1, 0, 0, 0));
                if (rb > 0) b.tri(vtx(0, -hy, 0, 0, -1, 0, 0.5f, 0.5f), vtx(x0 * rb, -hy, z0 * rb, 0, -1, 0, 0, 0), vtx(x1 * rb, -hy, z1 * rb, 0, -1, 0, 0, 0));
            }
        }
        return b.done();
    }

    static Prim sphPrim(float r, float height, int radial, int rings) {
        PB b = new PB();
        int rows = rings + 1;
        float hh = height / 2;
        for (int j = 0; j < rows; j++) {
            float v0 = (float) j / rows, v1 = (float) (j + 1) / rows;
            float w0 = MathUtils.sin(MathUtils.PI * v0), w1 = MathUtils.sin(MathUtils.PI * v1);
            float y0 = MathUtils.cos(MathUtils.PI * v0), y1 = MathUtils.cos(MathUtils.PI * v1);
            for (int i = 0; i < radial; i++) {
                float u0 = (float) i / radial, u1 = (float) (i + 1) / radial;
                float sx0 = MathUtils.sin(u0 * MathUtils.PI2), cz0 = MathUtils.cos(u0 * MathUtils.PI2);
                float sx1 = MathUtils.sin(u1 * MathUtils.PI2), cz1 = MathUtils.cos(u1 * MathUtils.PI2);
                float[] a = vtx(sx0 * r * w0, y0 * hh, cz0 * r * w0, sx0 * w0, y0, cz0 * w0, u0, v0);
                float[] bb = vtx(sx1 * r * w0, y0 * hh, cz1 * r * w0, sx1 * w0, y0, cz1 * w0, u1, v0);
                float[] c = vtx(sx1 * r * w1, y1 * hh, cz1 * r * w1, sx1 * w1, y1, cz1 * w1, u1, v1);
                float[] d = vtx(sx0 * r * w1, y1 * hh, cz0 * r * w1, sx0 * w1, y1, cz0 * w1, u0, v1);
                if (j > 0) b.tri(a, bb, c);
                if (j < rows - 1) b.tri(a, c, d);
            }
        }
        return b.done();
    }

    static Prim torusPrim(float R, float t, int rs, int ts) {
        PB b = new PB();
        for (int i = 0; i < rs; i++) for (int j = 0; j < ts; j++) {
            float[][] q = new float[4][];
            int[][] ij = {{i, j}, {i + 1, j}, {i + 1, j + 1}, {i, j + 1}};
            for (int k = 0; k < 4; k++) {
                float a = (float) ij[k][0] / rs * MathUtils.PI2, bb = (float) ij[k][1] / ts * MathUtils.PI2;
                float ca = MathUtils.cos(a), sa = MathUtils.sin(a), cb = MathUtils.cos(bb), sb = MathUtils.sin(bb);
                q[k] = vtx(ca * (R + cb * t), sb * t, sa * (R + cb * t), ca * cb, sb, sa * cb, 0, 0);
            }
            b.quad(q[0], q[1], q[2], q[3]);
        }
        return b.done();
    }

    static Prim capsulePrim(float r, float len, int radial) {
        PB b = new PB();
        int lat = 3;
        float half = len / 2;
        // кольца: верхняя полусфера, цилиндр, нижняя полусфера
        int rows = lat * 2 + 1;
        float[][] ys = new float[rows + 1][2]; // y, w (радиус-множитель), ny
        float[] nys = new float[rows + 1];
        int k = 0;
        for (int j = 0; j <= lat; j++) { float a = MathUtils.HALF_PI * (1 - (float) j / lat); ys[k][0] = half + MathUtils.sin(a) * r; ys[k][1] = MathUtils.cos(a); nys[k] = MathUtils.sin(a); k++; }
        for (int j = 0; j <= lat; j++) { float a = MathUtils.HALF_PI * ((float) j / lat); ys[k][0] = -half - MathUtils.sin(a) * r; ys[k][1] = MathUtils.cos(a); nys[k] = -MathUtils.sin(a); k++; }
        for (int j = 0; j < k - 1; j++) for (int i = 0; i < radial; i++) {
            float u0 = (float) i / radial * MathUtils.PI2, u1 = (float) (i + 1) / radial * MathUtils.PI2;
            float s0 = MathUtils.sin(u0), c0 = MathUtils.cos(u0), s1 = MathUtils.sin(u1), c1 = MathUtils.cos(u1);
            float[] a = vtx(s0 * r * ys[j][1], ys[j][0], c0 * r * ys[j][1], s0 * ys[j][1], nys[j], c0 * ys[j][1], 0, 0);
            float[] bb = vtx(s1 * r * ys[j][1], ys[j][0], c1 * r * ys[j][1], s1 * ys[j][1], nys[j], c1 * ys[j][1], 0, 0);
            float[] c = vtx(s1 * r * ys[j + 1][1], ys[j + 1][0], c1 * r * ys[j + 1][1], s1 * ys[j + 1][1], nys[j + 1], c1 * ys[j + 1][1], 0, 0);
            float[] d = vtx(s0 * r * ys[j + 1][1], ys[j + 1][0], c0 * r * ys[j + 1][1], s0 * ys[j + 1][1], nys[j + 1], c0 * ys[j + 1][1], 0, 0);
            if (ys[j][1] > 1e-4f) b.tri(a, bb, c);
            if (ys[j + 1][1] > 1e-4f) b.tri(a, c, d);
        }
        return b.done();
    }

    static Prim quadPrim(float w, float h) {
        PB b = new PB();
        b.quad(vtx(-w / 2, -h / 2, 0, 0, 0, 1, 0, 1), vtx(w / 2, -h / 2, 0, 0, 0, 1, 1, 1), vtx(w / 2, h / 2, 0, 0, 0, 1, 1, 0), vtx(-w / 2, h / 2, 0, 0, 0, 1, 0, 0));
        return b.done();
    }

    static Prim prim(String key, PrimMaker mk) {
        Prim p = cache.get(key);
        if (p == null) { p = mk.make(); cache.put(key, p); }
        return p;
    }

    interface PrimMaker { Prim make(); }

    // ---------- трансформ ----------
    static final Matrix4 xm = new Matrix4();
    static final Matrix3 nm = new Matrix3();

    /** Basis.from_euler(rot, XYZ) * scale */
    public static Matrix4 xf(Matrix4 out, float px, float py, float pz, float rx, float ry, float rz, float sx, float sy, float sz) {
        out.idt().translate(px, py, pz);
        if (rx != 0) out.rotateRad(Vector3.X, rx);
        if (ry != 0) out.rotateRad(Vector3.Y, ry);
        if (rz != 0) out.rotateRad(Vector3.Z, rz);
        if (sx != 1 || sy != 1 || sz != 1) out.scale(sx, sy, sz);
        return out;
    }

    public MB add(Prim a, Color col, Matrix4 t, float uvk) {
        nm.set(t).inv().transpose();
        float[] m = t.val, n3 = nm.val;
        float[] d = a.d;
        v.ensureCapacity(a.n * Mesh3.STRIDE);
        for (int i = 0; i < a.n; i++) {
            int o = i * 8;
            float x = d[o], y = d[o + 1], z = d[o + 2];
            float nx = d[o + 3], ny = d[o + 4], nz = d[o + 5];
            float wx = m[Matrix4.M00] * x + m[Matrix4.M01] * y + m[Matrix4.M02] * z + m[Matrix4.M03];
            float wy = m[Matrix4.M10] * x + m[Matrix4.M11] * y + m[Matrix4.M12] * z + m[Matrix4.M13];
            float wz = m[Matrix4.M20] * x + m[Matrix4.M21] * y + m[Matrix4.M22] * z + m[Matrix4.M23];
            float tx = n3[Matrix3.M00] * nx + n3[Matrix3.M01] * ny + n3[Matrix3.M02] * nz;
            float ty = n3[Matrix3.M10] * nx + n3[Matrix3.M11] * ny + n3[Matrix3.M12] * nz;
            float tz = n3[Matrix3.M20] * nx + n3[Matrix3.M21] * ny + n3[Matrix3.M22] * nz;
            float l = (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
            if (l > 1e-8f) { tx /= l; ty /= l; tz /= l; }
            v.add(wx, wy, wz, tx); v.add(ty, tz, col.r, col.g); v.add(col.b, col.a, d[o + 6] * uvk, d[o + 7] * uvk);
        }
        return this;
    }

    public MB add(Prim a, Color col, Matrix4 t) { return add(a, col, t, 1f); }

    // ---------- API как в mb.gd ----------
    public MB box(float w, float h, float d, Color col) { return box(w, h, d, col, 0, 0, 0, 0, 0, 0); }

    public MB box(float w, float h, float d, Color col, float px, float py, float pz) { return box(w, h, d, col, px, py, pz, 0, 0, 0); }

    public MB box(float w, float h, float d, Color col, float px, float py, float pz, float rx, float ry, float rz) {
        float mn = Math.min(w, Math.min(h, d));
        Prim p;
        if (hq && mn > 0.035f) {
            final float bv = MathUtils.clamp(mn * 0.16f, 0.006f, 0.09f);
            final float W = w, H = h, Dd = d;
            p = prim("bb" + w + "_" + h + "_" + d, () -> bevelBoxPrim(W, H, Dd, bv));
        } else {
            final float W = w, H = h, Dd = d;
            p = prim("b" + w + "_" + h + "_" + d, () -> boxPrim(W, H, Dd));
        }
        return add(p, col, xf(xm, px, py, pz, rx, ry, rz, 1, 1, 1));
    }

    public MB box(float w, float h, float d, Color col, Vector3 p) { return box(w, h, d, col, p.x, p.y, p.z, 0, 0, 0); }

    public MB box(float w, float h, float d, Color col, Vector3 p, Vector3 r) { return box(w, h, d, col, p.x, p.y, p.z, r.x, r.y, r.z); }

    public MB cyl(float rt, float rb, float h, int seg, Color col) { return cyl(rt, rb, h, seg, col, 0, 0, 0, 0, 0, 0); }

    public MB cyl(float rt, float rb, float h, int seg, Color col, float px, float py, float pz) { return cyl(rt, rb, h, seg, col, px, py, pz, 0, 0, 0); }

    public MB cyl(float rt, float rb, float h, int seg, Color col, float px, float py, float pz, float rx, float ry, float rz) {
        if (hq) seg = MathUtils.clamp((int) (Math.max(rt, rb) * 46f), seg, 40);
        final float a = rt, b = rb, hh = h; final int s = seg;
        Prim p = prim("c" + rt + "_" + rb + "_" + h + "_" + seg, () -> cylPrim(a, b, hh, s, true));
        return add(p, col, xf(xm, px, py, pz, rx, ry, rz, 1, 1, 1));
    }

    public MB cyl(float rt, float rb, float h, int seg, Color col, Vector3 p) { return cyl(rt, rb, h, seg, col, p.x, p.y, p.z, 0, 0, 0); }

    public MB cyl(float rt, float rb, float h, int seg, Color col, Vector3 p, Vector3 r) { return cyl(rt, rb, h, seg, col, p.x, p.y, p.z, r.x, r.y, r.z); }

    public MB sph(float r, Color col) { return sph(r, col, 0, 0, 0, 1, 1, 1, 8); }

    public MB sph(float r, Color col, float px, float py, float pz) { return sph(r, col, px, py, pz, 1, 1, 1, 8); }

    public MB sph(float r, Color col, float px, float py, float pz, float sx, float sy, float sz) { return sph(r, col, px, py, pz, sx, sy, sz, 8); }

    public MB sph(float r, Color col, float px, float py, float pz, float sx, float sy, float sz, int seg) {
        if (hq) seg = MathUtils.clamp((int) (r * Math.max(sx, Math.max(sy, sz)) * 22f), seg, 32);
        final float rr = r; final int s = seg;
        Prim p = prim("s" + r + "_" + seg, () -> sphPrim(rr, rr * 2f, s, Math.max(3, s / 2)));
        return add(p, col, xf(xm, px, py, pz, 0, 0, 0, sx, sy, sz));
    }

    public MB sph(float r, Color col, Vector3 p) { return sph(r, col, p.x, p.y, p.z, 1, 1, 1, 8); }

    public MB sph(float r, Color col, Vector3 p, Vector3 s, int seg) { return sph(r, col, p.x, p.y, p.z, s.x, s.y, s.z, seg); }

    public MB torus(float R, float tube, Color col) { return torus(R, tube, col, 0, 0, 0, 0, 0, 0); }

    public MB torus(float R, float tube, Color col, float px, float py, float pz) { return torus(R, tube, col, px, py, pz, 0, 0, 0); }

    public MB torus(float R, float tube, Color col, float px, float py, float pz, float rx, float ry, float rz) {
        final int rs = hq ? 40 : 24, ts = hq ? 10 : 6;
        final float a = R, b = tube;
        Prim p = prim("t" + R + "_" + tube + "_" + rs, () -> torusPrim(a, b, rs, ts));
        return add(p, col, xf(xm, px, py, pz, rx, ry, rz, 1, 1, 1));
    }

    public MB capsule(float r, float len, Color col, float px, float py, float pz, float rx, float ry, float rz) {
        final float a = r, b = len;
        Prim p = prim("p" + r + "_" + len, () -> capsulePrim(a, b, 6));
        return add(p, col, xf(xm, px, py, pz, rx, ry, rz, 1, 1, 1));
    }

    public MB disc(float r, Color col, float px, float py, float pz) { return cyl(r, r, 0.02f, 16, col, px, py, pz, 0, 0, 0); }

    public MB disc(float r, Color col, float px, float py, float pz, float rx, float ry, float rz) { return cyl(r, r, 0.02f, 16, col, px, py, pz, rx, ry, rz); }

    public MB quad(float w, float h, Color col, Matrix4 t) {
        final float a = w, b = h;
        return add(prim("q" + w + "_" + h, () -> quadPrim(a, b)), col, t);
    }

    /** Цилиндр от a до b (seg в mb.gd). */
    public MB seg(Vector3 a, Vector3 b, float ra, float rb, int segs, Color col) {
        float dx = b.x - a.x, dy = b.y - a.y, dz = b.z - a.z;
        float L = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (L < 0.0001f) return this;
        float yx = dx / L, yy = dy / L, yz = dz / L;
        float rx = 0, ry = 1, rz = 0;
        if (Math.abs(yy) >= 0.95f) { rx = 1; ry = 0; }
        // x = ref × y
        float xx = ry * yz - rz * yy, xy = rz * yx - rx * yz, xz = rx * yy - ry * yx;
        float xl = (float) Math.sqrt(xx * xx + xy * xy + xz * xz);
        xx /= xl; xy /= xl; xz /= xl;
        // z = x × y
        float zx = xy * yz - xz * yy, zy = xz * yx - xx * yz, zz = xx * yy - xy * yx;
        Matrix4 t = xm.idt();
        float[] m = t.val;
        m[Matrix4.M00] = xx; m[Matrix4.M10] = xy; m[Matrix4.M20] = xz;
        m[Matrix4.M01] = yx * L; m[Matrix4.M11] = yy * L; m[Matrix4.M21] = yz * L;
        m[Matrix4.M02] = zx; m[Matrix4.M12] = zy; m[Matrix4.M22] = zz;
        m[Matrix4.M03] = (a.x + b.x) / 2; m[Matrix4.M13] = (a.y + b.y) / 2; m[Matrix4.M23] = (a.z + b.z) / 2;
        final float ta = ra, tbb = rb; final int s = segs;
        Prim p = prim("c" + rb + "_" + ra + "_1.0_" + segs, () -> cylPrim(tbb, ta, 1f, s, true));
        return add(p, col, t);
    }

    /** Выдавливание многоугольника (точки [x,y]) вдоль X. */
    public MB extrude(float[][] pts, float depth, Color col, float px, float py, float pz, float rx, float ry, float rz) {
        Prim p = extrudePrim(pts, depth, 0.3f);
        return add(p, col, xf(xm, px, py, pz, rx, ry, rz, 1, 1, 1));
    }

    public MB extrude(float[][] pts, float depth, Color col, float px, float py, float pz) { return extrude(pts, depth, col, px, py, pz, 0, 0, 0); }

    public MB extrude(float[][] pts, float depth, Color col) { return extrude(pts, depth, col, 0, 0, 0, 0, 0, 0); }

    static final EarClippingTriangulator ECT = new EarClippingTriangulator();

    static Prim extrudePrim(float[][] pts, float depth, float uvk) {
        int n = pts.length;
        float[] poly = new float[n * 2];
        for (int i = 0; i < n; i++) { poly[i * 2] = pts[i][0]; poly[i * 2 + 1] = pts[i][1]; }
        ShortArray tri = ECT.computeTriangles(poly);
        float hx = depth / 2;
        PB b = new PB();
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < tri.size; i += 3) {
                float[][] q = new float[3][];
                for (int k = 0; k < 3; k++) {
                    int id = tri.get(i + k);
                    q[k] = vtx(hx * side, poly[id * 2 + 1], -poly[id * 2], side, 0, 0, poly[id * 2] * uvk, poly[id * 2 + 1] * uvk);
                }
                b.tri(q[0], q[1], q[2]);
            }
        }
        float area = 0;
        for (int i = 0; i < n; i++) area += poly[i * 2] * poly[((i + 1) % n) * 2 + 1] - poly[((i + 1) % n) * 2] * poly[i * 2 + 1];
        float sg = area > 0 ? 1 : -1;
        for (int i = 0; i < n; i++) {
            float ax = poly[i * 2], ay = poly[i * 2 + 1], bx = poly[((i + 1) % n) * 2], by = poly[((i + 1) % n) * 2 + 1];
            float ex = bx - ax, ey = by - ay;
            float l = (float) Math.sqrt(ex * ex + ey * ey);
            if (l < 1e-6f) continue;
            float n2x = ey / l * sg, n2y = -ex / l * sg;
            float nnx = 0, nny = n2y, nnz = -n2x;
            float L = l * uvk;
            b.quad(vtx(-hx, ay, -ax, nnx, nny, nnz, 0, 0), vtx(-hx, by, -bx, nnx, nny, nnz, L, 0),
                    vtx(hx, by, -bx, nnx, nny, nnz, L, depth * uvk), vtx(hx, ay, -ax, nnx, nny, nnz, 0, depth * uvk));
        }
        return b.done();
    }

    public MB wing(float span, float length, float th, Color col) { return wing(span, length, th, col, 0, 0, 0); }

    public MB wing(float span, float length, float th, Color col, float px, float py, float pz) {
        float[][] pts = {{length * 0.5f, 0}, {-length * 0.32f, span / 2}, {-length * 0.5f, span * 0.42f}, {-length * 0.28f, 0},
                {-length * 0.5f, -span * 0.42f}, {-length * 0.32f, -span / 2}};
        Prim p = extrudePrim(pts, th, 0.3f);
        return add(p, col, xf(xm, px, py, pz, 0, 0, MathUtils.HALF_PI, 1, 1, 1));
    }

    /** запекание псевдо-AO (bake_ao) */
    public void bakeAo(float strength) {
        if (v.size == 0) return;
        float y0 = 1e9f, y1 = -1e9f;
        float[] a = v.items;
        for (int i = 0; i < v.size; i += Mesh3.STRIDE) { y0 = Math.min(y0, a[i + 1]); y1 = Math.max(y1, a[i + 1]); }
        float hgt = Math.max(0.01f, y1 - y0);
        for (int i = 0; i < v.size; i += Mesh3.STRIDE) {
            float ny = a[i + 4];
            float t = MathUtils.clamp((ny + 1f) / 1.7f, 0, 1);
            float sm = t * t * (3 - 2 * t);
            float face = MathUtils.lerp(0.66f, 1.08f, sm);
            float vert = MathUtils.lerp(0.8f, 1.04f, MathUtils.clamp((a[i + 1] - y0) / hgt, 0, 1));
            float k = MathUtils.lerp(1f, face * vert, strength);
            a[i + 6] *= k; a[i + 7] *= k; a[i + 8] *= k;
        }
    }

    public Mesh3 mesh() {
        if (hq && !noAo) bakeAo(1f);
        return new Mesh3(v.items, v.size);
    }

    public MB append(MB o) { v.addAll(o.v); return this; }

    public static void clearCache() { cache.clear(); }

    // публичные примитивы (для «мультимешей», запекаемых в один меш)
    public static Prim pBox(float w, float h, float d) { final float a = w, b = h, c = d; return prim("b" + w + "_" + h + "_" + d, () -> boxPrim(a, b, c)); }
    public static Prim pCyl(float rt, float rb, float h, int seg, boolean caps) { final float a = rt, b = rb, c = h; final int s = seg; final boolean cp = caps; return prim("c" + rt + "_" + rb + "_" + h + "_" + seg + (caps ? "" : "nc"), () -> cylPrim(a, b, c, s, cp)); }
    public static Prim pSph(float r, float h, int radial, int rings) { final float a = r, b = h; final int s = radial, q = rings; return prim("S" + r + "_" + h + "_" + radial + "_" + rings, () -> sphPrim(a, b, s, q)); }
    public static Prim pQuad(float w, float h) { final float a = w, b = h; return prim("q" + w + "_" + h, () -> quadPrim(a, b)); }
}
