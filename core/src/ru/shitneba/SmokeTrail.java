package ru.shitneba;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

import ru.shitneba.gfx.Dyn;
import ru.shitneba.gfx.Mat;
import ru.shitneba.gfx.Tex;

/** Ленточный дымный след ракеты — порт smoke_trail.gd. */
public class SmokeTrail {
    public static final Color tint = new Color(0.86f, 0.85f, 0.83f, 1);
    public static float wind = 0f;
    public static Dyn dyn;

    final Array<float[]> pts = new Array<>(); // x,y,z,t,s
    float life = 3.4f, w0 = 0.7f, grow = 3.6f, alpha = 0.85f, spacing = 1.6f;
    boolean alive = true, hasHead;
    float T, totalS;
    final Vector3 head = new Vector3();

    public static void init() {
        Mat m = new Mat(); m.unshaded = true; m.vcol = true; m.tex = Tex.trail(); m.blend = Mat.ALPHA; m.cull = false; m.depthWrite = false; m.order = 2;
        dyn = new Dyn(m);
    }

    public SmokeTrail(boolean big) {
        if (!big) { life = 1.8f; w0 = 0.25f; grow = 1.6f; alpha = 0.5f; spacing = 1.2f; }
        FX.trails.add(this);
    }

    public void emit(Vector3 p) {
        head.set(p); hasHead = true;
        if (pts.size == 0) { pts.add(new float[]{p.x, p.y, p.z, T, totalS}); return; }
        float[] last = pts.peek();
        float d = p.dst(last[0], last[1], last[2]);
        if (d >= spacing) {
            int n = (int) (d / spacing);
            float t0 = last[3];
            for (int k = 1; k <= n; k++) {
                float f = (float) k / n;
                pts.add(new float[]{MathUtils.lerp(last[0], p.x, f), MathUtils.lerp(last[1], p.y, f), MathUtils.lerp(last[2], p.z, f), MathUtils.lerp(t0, T, f), totalS + d * f});
            }
            totalS += d;
            while (pts.size > 260) pts.removeIndex(0);
        }
    }

    public void stop() { alive = false; hasHead = false; }

    boolean update(float dt) {
        T += dt;
        while (pts.size > 0 && T - pts.get(0)[3] > life) pts.removeIndex(0);
        return alive || pts.size > 0;
    }

    final Vector3 tv = new Vector3(), dir = new Vector3(), side = new Vector3(), toCam = new Vector3();

    void fill(Camera cam) {
        int n = pts.size + (hasHead ? 1 : 0);
        if (n < 2) return;
        float[] px = new float[n * 3], wd = new float[n], al = new float[n], vv = new float[n];
        for (int i = 0; i < n; i++) {
            float x, y, z, t, s;
            if (i < pts.size) { float[] q = pts.get(i); x = q[0]; y = q[1]; z = q[2]; t = q[3]; s = q[4]; }
            else { x = head.x; y = head.y; z = head.z; t = T; float[] l = pts.peek(); s = totalS + head.dst(l[0], l[1], l[2]); }
            float age = T - t, k = age / life;
            px[i * 3] = x + wind * age * 0.6f; px[i * 3 + 1] = y + 0.7f * age; px[i * 3 + 2] = z;
            wd[i] = w0 + grow * (float) Math.sqrt(Math.max(0, age)) + age * 0.6f;
            float a = alpha * (float) Math.pow(MathUtils.clamp(1 - k, 0, 1), 1.4);
            float fromHead = n - 1 - i;
            a *= MathUtils.clamp(0.25f + fromHead * 0.35f, 0, 1);
            al[i] = a; vv[i] = s * 0.035f;
        }
        float r = tint.r, g = tint.g, b = tint.b;
        Vector3 cp = cam.position;
        float prevLx = 0, prevLy = 0, prevLz = 0, prevRx = 0, prevRy = 0, prevRz = 0, prevA = 0, prevV = 0;
        for (int i = 0; i < n; i++) {
            int ip = Math.max(0, i - 1), in = Math.min(n - 1, i + 1);
            dir.set(px[in * 3] - px[ip * 3], px[in * 3 + 1] - px[ip * 3 + 1], px[in * 3 + 2] - px[ip * 3 + 2]);
            if (dir.len2() < 1e-6f) dir.set(0, 1, 0);
            toCam.set(cp.x - px[i * 3], cp.y - px[i * 3 + 1], cp.z - px[i * 3 + 2]);
            side.set(dir).crs(toCam).nor().scl(wd[i] * 0.5f);
            float lx = px[i * 3] - side.x, ly = px[i * 3 + 1] - side.y, lz = px[i * 3 + 2] - side.z;
            float rx = px[i * 3] + side.x, ry = px[i * 3 + 1] + side.y, rz = px[i * 3 + 2] + side.z;
            if (i > 0) {
                Dyn d = dyn;
                d.vert(prevLx, prevLy, prevLz, 0, 1, 0, r, g, b, prevA, 0, prevV);
                d.vert(prevRx, prevRy, prevRz, 0, 1, 0, r, g, b, prevA, 1, prevV);
                d.vert(rx, ry, rz, 0, 1, 0, r, g, b, al[i], 1, vv[i]);
                d.vert(prevLx, prevLy, prevLz, 0, 1, 0, r, g, b, prevA, 0, prevV);
                d.vert(rx, ry, rz, 0, 1, 0, r, g, b, al[i], 1, vv[i]);
                d.vert(lx, ly, lz, 0, 1, 0, r, g, b, al[i], 0, vv[i]);
            }
            prevLx = lx; prevLy = ly; prevLz = lz; prevRx = rx; prevRy = ry; prevRz = rz; prevA = al[i]; prevV = vv[i];
        }
    }
}
