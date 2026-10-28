package ru.shitneba;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;

import java.util.Random;

import ru.shitneba.gfx.Dyn;
import ru.shitneba.gfx.MB;
import ru.shitneba.gfx.Mat;
import ru.shitneba.gfx.Mesh3;
import ru.shitneba.gfx.Node3;
import ru.shitneba.gfx.Renderer;
import ru.shitneba.gfx.Tex;

/** Частицы — эмуляция CPUParticles3D из fx.gd (взрывы, искры, дым, горелки, шлейфы). */
public final class FX {
    static final Random R = new Random();
    public static Dyn dAdd, dSmoke, dDebris, dRing, dCasing;
    /** Высота земли (для гильз, обломков и пожаров). */
    public interface HeightFn { float h(float x, float z); }
    public static HeightFn height;
    public static Renderer.Env env;
    /** Яркость дыма (ночью и в грозу дым темнее). */
    public static float smokeLum = 1f;
    static final Array<P> parts = new Array<>(false, 2048);
    static final Array<P> free = new Array<>(false, 2048);
    public static final Array<Emitter> emitters = new Array<>();
    public static final Array<SmokeTrail> trails = new Array<>();
    public static final Array<Node3> flames = new Array<>();
    static int MAX = 2600;
    static Mat coreMat;
    static Mesh3 coreMesh;

    public static final int K_ADD = 0, K_SMOKE = 1, K_DEBRIS = 2, K_RING = 3, K_CASING = 4;

    /** Шаблон эмиссии: параметры CPUParticles3D. */
    public static final class Cfg {
        public int kind = K_ADD;
        public float vMin, vMax, dMin, dMax, sMin = 1, sMax = 1, cA = 1, cB = 1, life = 1;
        public final Vector3 grav = new Vector3(), dir = new Vector3(0, 1, 0);
        public float spread = 180;
        public float[] ramp; // [off, r,g,b,a]*
        public float sphere = 0;
        public float angVel = 0;
        /** Плоское кольцо по земле вместо билборда. */
        public boolean flat;
        /** Отскок от земли (гильзы). */
        public boolean bounce;
        /** Шлейф за частицей (горящие обломки): шаблон и частота в секунду. */
        public Cfg trail; public float trailRate;
        /** Шанс оставить пожар, упав на землю. */
        public float groundFire;
        /** Размеры коробки для обломков/гильз. */
        public float bx = 0.35f, by = 0.2f, bz = 0.3f;

        public Cfg vel(float a, float b) { vMin = a; vMax = b; return this; }
        public Cfg damp(float a, float b) { dMin = a; dMax = b; return this; }
        public Cfg grav(float x, float y, float z) { grav.set(x, y, z); return this; }
        public Cfg scale(float a, float b) { sMin = a; sMax = b; return this; }
        public Cfg curve(float a, float b) { cA = a; cB = b; return this; }
        public Cfg ramp(float... r) { ramp = r; return this; }
        public Cfg life(float l) { life = l; return this; }
        public Cfg kind(int k) { kind = k; return this; }
        public Cfg dir(Vector3 d, float spr) { dir.set(d); spread = spr; return this; }
    }

    static final class P {
        float x, y, z, vx, vy, vz, age, life, sc, damp, rot, rotV, ax, ay, az, tacc;
        boolean landed;
        Cfg c;
    }

    public static int count() { return parts.size; }

    public static void init() {
        Mat a = new Mat(); a.unshaded = true; a.vcol = true; a.tex = Tex.soft(1.5f); a.blend = Mat.ADD; a.depthWrite = false; a.cull = false; a.order = 5;
        dAdd = new Dyn(a);
        Mat s = new Mat(); s.unshaded = true; s.vcol = true; s.tex = Tex.smoke(); s.blend = Mat.ALPHA; s.depthWrite = false; s.cull = false;
        s.distFadeMin = 1.5f; s.distFadeMax = 7f; s.order = 3;
        dSmoke = new Dyn(s);
        Mat d = Mat.std(new Color(0.12f, 0.12f, 0.12f, 1)); d.vcol = false; d.rough = 0.9f;
        dDebris = new Dyn(d);
        Mat rg = new Mat(); rg.unshaded = true; rg.vcol = true; rg.tex = Tex.ring(); rg.blend = Mat.ADD; rg.depthWrite = false; rg.cull = false; rg.order = 5;
        dRing = new Dyn(rg);
        Mat cs = Mat.std(new Color(0.86f, 0.66f, 0.3f, 1)); cs.vcol = false; cs.rough = 0.3f; cs.metal = 0.85f; cs.spec = 1f;
        dCasing = new Dyn(cs);
        MAX = G.isMobile() ? 1600 : 2600;
        coreMat = new Mat(); coreMat.unshaded = true; coreMat.vcol = false; coreMat.tex = Tex.flameCore(); coreMat.blend = Mat.ADD; coreMat.depthWrite = false; coreMat.cull = false; coreMat.fog = false; coreMat.order = 6;
        MB b = new MB();
        b.cyl(0.13f, 0f, 1.6f, 10, Color.WHITE);
        // UV v вдоль высоты: верх(0.13)=0 -> низ=1 (как у CylinderMesh)
        coreMesh = b.mesh();
    }

    static float rr(float a, float b) { return a + R.nextFloat() * (b - a); }

    static P obtain() { return free.size > 0 ? free.pop() : new P(); }

    static final Vector3 tdir = new Vector3(), rdA = new Vector3(), rdB = new Vector3();

    static void randDir(Cfg c, Vector3 out) {
        if (c.spread >= 179f) {
            float u = rr(-1, 1), th = R.nextFloat() * MathUtils.PI2, s = (float) Math.sqrt(1 - u * u);
            out.set(s * MathUtils.cos(th), u, s * MathUtils.sin(th));
            return;
        }
        float ang = c.spread * MathUtils.degreesToRadians * (float) Math.sqrt(R.nextFloat());
        float th = R.nextFloat() * MathUtils.PI2;
        Vector3 d = c.dir;
        Vector3 t1 = (Math.abs(d.y) < 0.9f ? rdA.set(Vector3.Y) : rdA.set(Vector3.X)).crs(d).nor();
        Vector3 t2 = rdB.set(d).crs(t1);
        float sa = MathUtils.sin(ang), ca = MathUtils.cos(ang);
        out.set(d).scl(ca).mulAdd(t1, sa * MathUtils.cos(th)).mulAdd(t2, sa * MathUtils.sin(th)).nor();
    }

    public static void emit(Cfg c, float x, float y, float z, int amount) {
        if (G.isMobile() && amount > 3) amount = (int) Math.ceil(amount * 0.7f);
        for (int i = 0; i < amount; i++) {
            if (parts.size >= MAX) return;
            P p = obtain();
            p.c = c;
            p.x = x; p.y = y; p.z = z;
            if (c.sphere > 0) {
                randDir(sphereCfg, tdir);
                float r = c.sphere * (float) Math.cbrt(R.nextFloat());
                p.x += tdir.x * r; p.y += tdir.y * r; p.z += tdir.z * r;
            }
            randDir(c, tdir);
            float v = rr(c.vMin, c.vMax);
            p.vx = tdir.x * v; p.vy = tdir.y * v; p.vz = tdir.z * v;
            p.age = 0; p.life = c.life;
            p.sc = rr(c.sMin, c.sMax);
            p.damp = rr(c.dMin, c.dMax);
            p.rot = R.nextFloat() * MathUtils.PI2;
            p.rotV = c.angVel > 0 ? rr(-c.angVel, c.angVel) * MathUtils.degreesToRadians : 0;
            p.ax = rr(-1, 1); p.ay = rr(-1, 1); p.az = rr(-1, 1);
            p.tacc = 0; p.landed = false;
            parts.add(p);
        }
    }

    static final Cfg sphereCfg = new Cfg();

    // ---------- эффекты (fx.gd) ----------
    static final Cfg EMBER_SMOKE = new Cfg().kind(K_SMOKE).vel(0.2f, 0.6f).damp(0.5f, 1f).grav(0, 1.5f, 0).scale(0.9f, 1.4f).curve(0.6f, 2.4f)
            .ramp(0, 0.16f, 0.14f, 0.13f, 0.55f, 1, 0.35f, 0.33f, 0.32f, 0).life(1.6f);
    static final Cfg EMBER_FIRE = new Cfg().vel(0, 0.5f).grav(0, 1, 0).scale(1.1f, 1.6f).curve(1f, 0.2f)
            .ramp(0, 1, 0.75f, 0.3f, 1, 1, 1, 0.35f, 0.08f, 0).life(0.35f);

    /** Взрыв: вспышка + свет, огненный шар, искры, ударная волна, горящие обломки, дым, пыль по земле. */
    public static void explosion(Vector3 pos, float s, boolean smoke) {
        float n = Math.min(2.2f, s);
        boolean mob = G.isMobile();
        float fs = Math.min(s, 2.2f);
        Cfg flash = new Cfg().vel(0, 0).grav(0, 0, 0).scale(16 * fs, 16 * fs).curve(0.35f, 1f).ramp(0, 1, 0.96f, 0.85f, 1, 1, 1, 0.65f, 0.3f, 0).life(0.18f);
        emit(flash, pos.x, pos.y, pos.z, 1);
        // свет вспышки подсвечивает округу (землю, машину, дроны)
        light(pos, 26f + 34f * n, 1f, 0.62f, 0.28f, (3.5f + 4f * n), 0.22f + 0.18f * n);
        Cfg fire = new Cfg().vel(3 * s, 11 * s).damp(5, 9).grav(0, 3, 0).scale(3 * s, 6 * s).curve(0.45f, 1f)
                .ramp(0, 1, 0.95f, 0.75f, 1, 0.25f, 1, 0.62f, 0.2f, 0.95f, 0.6f, 0.75f, 0.25f, 0.06f, 0.5f, 1, 0.3f, 0.08f, 0.02f, 0).life(0.6f);
        emit(fire, pos.x, pos.y, pos.z, (int) (12 * n) + 5);
        float sq = (float) Math.sqrt(s);
        Cfg spark = new Cfg().vel(30 * sq, 70 * sq).damp(1, 1.5f).grav(0, -18, 0).scale(0.35f, 0.55f).curve(1f, 0.3f)
                .ramp(0, 1, 0.9f, 0.6f, 1, 1, 1, 0.35f, 0.1f, 0).life(0.9f);
        emit(spark, pos.x, pos.y, pos.z, (int) (14 * n) + 4);
        // ударная волна: быстро расширяющееся кольцо
        if (s >= 0.6f) {
            Cfg ring = new Cfg().kind(K_RING).vel(0, 0).grav(0, 0, 0).scale(18 * s, 18 * s).curve(0.08f, 1f)
                    .ramp(0, 1, 0.92f, 0.8f, 0.9f, 0.4f, 1, 0.8f, 0.6f, 0.45f, 1, 0.8f, 0.6f, 0.4f, 0).life(0.32f + 0.08f * n);
            emit(ring, pos.x, pos.y, pos.z, 1);
        }
        if (smoke) {
            float g = rr(0.08f, 0.15f);
            Cfg sm = new Cfg().kind(K_SMOKE).vel(1.5f * s, 5 * s).damp(1.5f, 2.2f).grav(0, 2, 0).scale(4 * s, 6.5f * s).curve(0.5f, 2.6f)
                    .ramp(0, g * 0.8f, g * 0.7f, g * 0.6f, 0, 0.08f, g, g * 0.92f, g * 0.85f, 0.9f, 0.5f, g + 0.12f, g + 0.11f, g + 0.1f, 0.55f, 1, 0.42f, 0.41f, 0.4f, 0).life(4.5f);
            emit(sm, pos.x, pos.y, pos.z, (int) (8 * n) + 4);
        }
        if (s >= 0.9f) {
            // горящие обломки: тёмные куски с огнём и дымным хвостом, падают и могут поджечь землю
            Cfg db = new Cfg().kind(K_DEBRIS).vel(8, 22).grav(0, -20, 0).life(3f).dir(Vector3.Y, 70);
            db.angVel = 400; db.trail = EMBER_SMOKE; db.trailRate = mob ? 9 : 16; db.groundFire = 0.35f;
            emit(db, pos.x, pos.y, pos.z, (int) (5 * n) + 2);
            Cfg em = new Cfg().vel(10, 26).grav(0, -16, 0).damp(0.3f, 0.6f).scale(0.9f, 1.4f).curve(1f, 0.5f)
                    .ramp(0, 1, 0.85f, 0.45f, 1, 0.7f, 1, 0.45f, 0.1f, 1, 1, 0.6f, 0.15f, 0.02f, 0).life(1.8f).dir(Vector3.Y, 80);
            em.trail = EMBER_FIRE; em.trailRate = mob ? 14 : 26; em.groundFire = 0.15f;
            emit(em, pos.x, pos.y, pos.z, (int) (4 * n) + 2);
        }
        // взрыв у земли: пыль кольцом, плоская волна по земле, пожар
        if (height != null) {
            float gh = height.h(pos.x, pos.z), above = pos.y - gh;
            if (above < 3f + 2f * s) {
                Cfg gr = new Cfg().kind(K_RING).vel(0, 0).grav(0, 0, 0).scale(26 * s, 26 * s).curve(0.1f, 1f)
                        .ramp(0, 1, 0.8f, 0.55f, 0.7f, 1, 0.6f, 0.45f, 0.35f, 0).life(0.45f);
                gr.flat = true;
                emit(gr, pos.x, gh + 0.25f, pos.z, 1);
                Cfg dust = new Cfg().kind(K_SMOKE).dir(Vector3.Y, 82).vel(9 * sq, 20 * sq).damp(10, 14).grav(0, 0.6f, 0).scale(3 * s, 5 * s).curve(0.6f, 2.2f)
                        .ramp(0, 0.42f, 0.36f, 0.28f, 0.75f, 1, 0.5f, 0.45f, 0.38f, 0).life(2.6f);
                emit(dust, pos.x, gh + 0.8f, pos.z, (int) (10 * n) + 4);
                if (s >= 1f) groundFire(tv2.set(pos.x, gh, pos.z), MathUtils.clamp(s * 0.8f, 0.6f, 2.4f), rr(4f, 7f) + s);
            }
        }
    }

    static final Vector3 tv2 = new Vector3();

    /** Пыль кольцом по земле (выстрел «Грома», падение тяжёлого). */
    public static void groundDust(Vector3 at, float s) {
        Cfg gr = new Cfg().kind(K_RING).vel(0, 0).grav(0, 0, 0).scale(22 * s, 22 * s).curve(0.15f, 1f)
                .ramp(0, 0.55f, 0.45f, 0.33f, 0.5f, 1, 0.4f, 0.32f, 0.24f, 0).life(0.4f);
        gr.flat = true;
        emit(gr, at.x, at.y + 0.3f, at.z, 1);
    }

    // ---------- пожары на земле ----------
    static int fires;
    public static void groundFire(Vector3 at, float size, float dur) {
        if (fires >= (G.isMobile() ? 5 : 9)) return;
        Cfg f = new Cfg().kind(K_ADD).dir(Vector3.Y, 18).vel(1.5f, 4).grav(0, 2, 0).scale(1.4f * size, 2.4f * size).curve(1f, 0.25f)
                .ramp(0, 0.9f, 0.45f, 0.12f, 0.55f, 0.5f, 0.85f, 0.28f, 0.05f, 0.45f, 1, 0.5f, 0.08f, 0.02f, 0).life(0.7f);
        f.sphere = 0.8f * size;
        Cfg sm = new Cfg().kind(K_SMOKE).dir(Vector3.Y, 12).vel(2, 4).grav(0, 1.2f, 0).scale(2.4f * size, 3.6f * size).curve(0.6f, 3f)
                .ramp(0, 0.1f, 0.09f, 0.08f, 0.6f, 1, 0.32f, 0.31f, 0.3f, 0).life(3.5f);
        Emitter e1 = new Emitter(); e1.fixed.set(at).add(0, 0.4f, 0); e1.cfg = f; e1.rate = 14; e1.killAt = dur; e1.fire = true;
        Emitter e2 = new Emitter(); e2.fixed.set(at).add(0, 1.5f, 0); e2.cfg = sm; e2.rate = 5; e2.killAt = dur + 1f;
        emitters.add(e1); emitters.add(e2);
        fires++;
    }

    // ---------- свет ----------
    static final class Light { float x, y, z, r, cr, cg, cb, pow, life, age; }
    static final Array<Light> lights = new Array<>();

    /** Временный точечный свет: позиция, радиус, цвет, сила, время жизни. */
    public static void light(Vector3 p, float radius, float r, float g, float b, float pow, float life) {
        if (lights.size >= 24) lights.removeIndex(0);
        Light l = new Light();
        l.x = p.x; l.y = p.y; l.z = p.z; l.r = radius; l.cr = r; l.cg = g; l.cb = b; l.pow = pow; l.life = life;
        lights.add(l);
    }

    static final Light[] best = new Light[4];
    static final float[] bestW = new float[4];

    static void fillLights(Camera cam) {
        if (env == null) return;
        for (int i = 0; i < 4; i++) { best[i] = null; bestW[i] = 0; }
        for (Light l : lights) {
            float k = 1f - l.age / l.life;
            float w = l.pow * k * k * l.r / (40f + cam.position.dst(l.x, l.y, l.z));
            int at = -1;
            for (int i = 0; i < 4; i++) if (w > bestW[i]) { at = i; break; }
            if (at < 0) continue;
            for (int i = 3; i > at; i--) { best[i] = best[i - 1]; bestW[i] = bestW[i - 1]; }
            best[at] = l; bestW[at] = w;
        }
        int n = 0;
        for (int i = 0; i < 4; i++) {
            Light l = best[i];
            if (l == null) { env.plc[i * 3] = env.plc[i * 3 + 1] = env.plc[i * 3 + 2] = 0; env.pl[i * 4 + 3] = 1; continue; }
            float k = 1f - l.age / l.life; k = k * k * l.pow;
            env.pl[i * 4] = l.x; env.pl[i * 4 + 1] = l.y; env.pl[i * 4 + 2] = l.z; env.pl[i * 4 + 3] = l.r;
            env.plc[i * 3] = l.cr * k; env.plc[i * 3 + 1] = l.cg * k; env.plc[i * 3 + 2] = l.cb * k;
            n++;
        }
        env.plN = n;
    }

    // ---------- свечения на один кадр (раскалённые стволы) ----------
    static final FloatArray fglow = new FloatArray();

    public static void frameGlow(Vector3 p, float size, float r, float g, float b, float a) {
        fglow.add(p.x, p.y, p.z, size); fglow.add(r, g, b, a);
    }

    // ---------- гильзы ----------
    static Cfg casingS, casingL;

    /** Гильза: вылетает вбок-вверх из казённика, звенит о землю. */
    public static void casing(Vector3 at, Vector3 side, boolean big) {
        Cfg c = big ? casingL : casingS;
        if (c == null) {
            c = new Cfg().kind(K_CASING).vel(0, 0).grav(0, -24, 0).life(big ? 2.2f : 1.6f);
            c.angVel = 900; c.bounce = true;
            if (big) { c.bx = 0.17f; c.by = 0.17f; c.bz = 0.62f; casingL = c; } else { c.bx = 0.09f; c.by = 0.09f; c.bz = 0.34f; casingS = c; }
        }
        if (parts.size >= MAX) return;
        P p = obtain();
        p.c = c; p.x = at.x; p.y = at.y; p.z = at.z;
        float v = rr(5f, 8f);
        p.vx = side.x * v + rr(-1, 1); p.vy = rr(3.5f, 6.5f); p.vz = side.z * v + rr(-1, 1);
        p.age = 0; p.life = c.life; p.sc = 1; p.damp = 0; p.rot = R.nextFloat() * MathUtils.PI2; p.rotV = rr(-c.angVel, c.angVel) * MathUtils.degreesToRadians;
        p.ax = rr(-1, 1); p.ay = rr(-1, 1); p.az = rr(-1, 1); p.tacc = 0; p.landed = false;
        parts.add(p);
    }

    public static void sparks(Vector3 pos, int n, Color col) {
        Cfg c = new Cfg().vel(10, 30).damp(2, 2).grav(0, -10, 0).scale(0.5f, 0.6f).curve(1f, 0.3f)
                .ramp(0, col.r, col.g, col.b, 1, 1, col.r, col.g * 0.5f, col.b * 0.3f, 0).life(0.4f);
        emit(c, pos.x, pos.y, pos.z, n);
    }

    static final Color SPARK = new Color(1, 0.9f, 0.5f, 1);

    public static void sparks(Vector3 pos, int n) { sparks(pos, n, SPARK); }

    public static void puff(Vector3 pos, float size, Color col, float alpha, float life, float vy) {
        Cfg c = new Cfg().kind(K_SMOKE).vel(0.5f, 1.2f).grav(0, vy, 0).damp(0.5f, 0.5f).scale(1.5f * size, 2.0f * size).curve(0.6f, 2.6f)
                .ramp(0, col.r, col.g, col.b, alpha, 1, col.r + 0.15f, col.g + 0.15f, col.b + 0.15f, 0).life(life);
        emit(c, pos.x, pos.y, pos.z, 2);
    }

    public static void glow(Vector3 pos, float size, Color col, float life) {
        Cfg c = new Cfg().vel(0, 0).grav(0, 0, 0).scale(size, size).curve(1, 1).ramp(0, col.r, col.g, col.b, 1, 1, col.r, col.g, col.b, 0).life(life);
        emit(c, pos.x, pos.y, pos.z, 1);
    }

    public static void launchBlast(Vector3 muzzle, Vector3 fwd) {
        Vector3 rear = new Vector3(muzzle).mulAdd(fwd, -3.4f);
        glow(muzzle, 7f, new Color(1, 0.9f, 0.7f, 1), 0.14f);
        glow(rear, 9f, new Color(1, 0.75f, 0.45f, 1), 0.18f);
        Vector3 back = new Vector3(fwd).scl(-1);
        Cfg jet = new Cfg().dir(back, 12).vel(25, 45).damp(30, 40).grav(0, 0, 0).scale(1.6f, 2.6f).curve(0.6f, 1.4f)
                .ramp(0, 1, 0.92f, 0.7f, 1, 1, 1, 0.5f, 0.2f, 0).life(0.25f);
        emit(jet, rear.x, rear.y, rear.z, 10);
        Color t = SmokeTrail.tint;
        Cfg b1 = new Cfg().kind(K_SMOKE).dir(back, 25).vel(2, 7).damp(2, 3).grav(0, 0.9f, 0).scale(3.2f, 4.6f).curve(0.45f, 2.6f)
                .ramp(0, t.r, t.g, t.b, 0, 0.05f, t.r, t.g, t.b, 0.5f, 0.5f, t.r * 0.95f, t.g * 0.95f, t.b * 0.95f, 0.25f, 1, t.r * 0.9f, t.g * 0.9f, t.b * 0.9f, 0).life(4f);
        emit(b1, rear.x, rear.y, rear.z, 9);
        Cfg b2 = new Cfg().kind(K_SMOKE).dir(fwd, 30).vel(3, 10).damp(3, 5).grav(0, 0.8f, 0).scale(1.6f, 2.6f).curve(0.5f, 3f)
                .ramp(0, t.r, t.g, t.b, 0, 0.05f, t.r, t.g, t.b, 0.5f, 1, t.r * 0.9f, t.g * 0.9f, t.b * 0.9f, 0).life(3f);
        emit(b2, muzzle.x, muzzle.y, muzzle.z, 6);
    }

    // ---------- непрерывные эмиттеры ----------
    public static final class Emitter {
        public Node3 anchor;
        public final Vector3 off = new Vector3(), fixed = new Vector3();
        public Cfg cfg;
        public float rate, acc;
        public boolean emitting = true;
        public float killAt = -1;
        public float t;
        public Glow glowCfg;
        public boolean fire;
    }

    static final class Glow { float size; float r, g, b; }

    static boolean attached(Node3 n) {
        for (Node3 p = n; p != null; p = p.parent) if (p.tag == ROOT_TAG) return true;
        return false;
    }

    public static final Object ROOT_TAG = new Object();

    public static Emitter burner(Node3 anchor, boolean fire, float size) {
        Cfg c = new Cfg().kind(fire ? K_ADD : K_SMOKE).dir(Vector3.Y, 15).vel(fire ? 2 : 1.5f, fire ? 5 : 3).grav(0, fire ? -9.8f : 0.8f, 0)
                .scale(1.5f * size, (fire ? 2.4f : 2.2f) * size).life(fire ? 0.7f : 2.0f);
        // CPUParticles3D по умолчанию имеет гравитацию (0,-9.8,0)
        if (fire) c.curve(1f, 0.3f).ramp(0, 1, 0.65f, 0.2f, 1, 1, 0.9f, 0.2f, 0.05f, 0);
        else c.curve(0.7f, 3f).ramp(0, 0.12f, 0.12f, 0.12f, 0.6f, 1, 0.3f, 0.3f, 0.3f, 0);
        c.sphere = 0.6f * size;
        Emitter e = new Emitter();
        e.anchor = anchor; e.cfg = c; e.rate = (fire ? 24 : 16) / c.life;
        emitters.add(e);
        return e;
    }

    public static Emitter chimney(Vector3 pos) {
        Cfg c = new Cfg().kind(K_SMOKE).dir(new Vector3(0.3f, 1, 0).nor(), 10).vel(5, 7).grav(0, 0.3f, 0).scale(12, 14).curve(1, 3.5f)
                .ramp(0, 0.18f, 0.15f, 0.14f, 0.45f, 1, 0.4f, 0.35f, 0.33f, 0).life(9f);
        Emitter e = new Emitter();
        e.fixed.set(pos); e.cfg = c; e.rate = 40 / 9f;
        emitters.add(e);
        // предварительный прогрев
        for (int i = 0; i < 40; i++) {
            emit(c, pos.x, pos.y, pos.z, 1);
            P p = parts.peek();
            float a = R.nextFloat() * 9f;
            p.age = a; p.x += p.vx * a; p.y += p.vy * a; p.z += p.vz * a;
        }
        return e;
    }

    /** Дымный хвост + вспышка для ракет противника (FX.trail). */
    public static Emitter trail(Node3 anchor, boolean big) {
        Cfg s = new Cfg().kind(K_SMOKE).vel(0.2f, 0.8f).grav(0, 0.6f, 0).scale(big ? 1f : 0.6f, big ? 1.3f : 0.8f).curve(0.8f, big ? 5f : 3.5f)
                .ramp(0, 0.92f, 0.9f, 0.88f, 0, 0.08f, 0.92f, 0.9f, 0.88f, 0.5f, 1, 0.6f, 0.6f, 0.62f, 0).life(big ? 2.6f : 1.2f);
        Emitter e = new Emitter();
        e.anchor = anchor; e.cfg = s; e.rate = (big ? 70 : 30) / s.life;
        e.off.set(0, 0, 1.0f);
        Glow g = new Glow(); g.size = big ? 2.8f : 2.0f; g.r = 1; g.g = 0.75f; g.b = 0.35f;
        e.glowCfg = g;
        emitters.add(e);
        return e;
    }

    public static void detach(Emitter e) {
        if (e == null) return;
        if (e.anchor != null) { e.anchor.toGlobal(e.off, e.fixed); }
        e.anchor = null; e.emitting = false; e.glowCfg = null;
        e.killAt = e.t + 3f;
    }

    /** Факел ракеты: ядро-конус + свечение. */
    public static Node3 flame(float tailZ) {
        Node3 n = new Node3();
        n.pos.set(0, 0, tailZ);
        Node3 core = new Node3(coreMesh, coreMat);
        core.setEuler(-MathUtils.PI / 2, 0, 0);
        core.pos.set(0, 0, 0.8f);
        n.add(core);
        n.tag = "flame";
        flames.add(n);
        return n;
    }

    static final Vector3 tv = new Vector3(), right = new Vector3(), up = new Vector3();
    static final Matrix4 tm = new Matrix4();

    public static void update(float dt, Camera cam) {
        // эмиттеры
        for (int i = emitters.size - 1; i >= 0; i--) {
            Emitter e = emitters.get(i);
            e.t += dt;
            if (e.killAt > 0 && e.t > e.killAt) { if (e.fire) fires = Math.max(0, fires - 1); emitters.removeIndex(i); continue; }
            if (e.anchor != null && !attached(e.anchor)) { emitters.removeIndex(i); continue; }
            if (!e.emitting) continue;
            if (e.anchor != null) e.anchor.toGlobal(e.off, tv); else tv.set(e.fixed);
            e.acc += dt * e.rate;
            int n = (int) e.acc;
            e.acc -= n;
            for (int k = 0; k < n; k++) emit1(e.cfg, tv);
            if (e.fire && R.nextFloat() < dt * 12f) {
                float fade = e.killAt > 0 ? MathUtils.clamp((e.killAt - e.t) / 1.5f, 0, 1) : 1;
                light(tv, 18f, 1f, 0.5f, 0.18f, rr(1.2f, 2.2f) * fade, 0.12f);
            }
        }
        for (int i = lights.size - 1; i >= 0; i--) { Light l = lights.get(i); l.age += dt; if (l.age >= l.life) lights.removeIndex(i); }
        // частицы
        for (int i = parts.size - 1; i >= 0; i--) {
            P p = parts.get(i);
            p.age += dt;
            if (p.age >= p.life) { parts.removeIndex(i); free.add(p); continue; }
            Cfg c = p.c;
            if (p.damp > 0) {
                float sp = (float) Math.sqrt(p.vx * p.vx + p.vy * p.vy + p.vz * p.vz);
                if (sp > 0) {
                    float ns = Math.max(0, sp - p.damp * dt);
                    float k = ns / sp; p.vx *= k; p.vy *= k; p.vz *= k;
                }
            }
            if (p.landed) continue;
            p.vx += c.grav.x * dt; p.vy += c.grav.y * dt; p.vz += c.grav.z * dt;
            p.x += p.vx * dt; p.y += p.vy * dt; p.z += p.vz * dt;
            p.rot += p.rotV * dt;
            if (c.trail != null) {
                p.tacc += dt * c.trailRate * (1f - p.age / p.life);
                while (p.tacc >= 1f) { p.tacc -= 1f; tv.set(p.x, p.y, p.z); emit1(c.trail, tv); }
            }
            if ((c.bounce || c.groundFire > 0 || c.kind == K_DEBRIS) && height != null && p.vy < 0) {
                float gh = height.h(p.x, p.z);
                if (p.y < gh + 0.05f) {
                    p.y = gh + 0.05f;
                    if (c.bounce && p.vy < -2.5f) { p.vy = -p.vy * 0.32f; p.vx *= 0.55f; p.vz *= 0.55f; p.rotV *= 0.5f; }
                    else if (c.bounce) { p.vx = p.vy = p.vz = 0; p.rotV = 0; p.landed = true; }
                    else {
                        if (c.groundFire > 0 && R.nextFloat() < c.groundFire) groundFire(tv.set(p.x, gh, p.z), rr(0.45f, 0.8f), rr(3f, 6f));
                        if (c.kind == K_DEBRIS) { p.vx = p.vy = p.vz = 0; p.rotV = 0; p.landed = true; p.life = Math.min(p.life, p.age + 4f); }
                        else { p.age = p.life; }
                    }
                }
            }
        }
        for (int i = trails.size - 1; i >= 0; i--) if (!trails.get(i).update(dt)) trails.removeIndex(i);
    }

    static void emit1(Cfg c, Vector3 at) {
        // на телефоне непрерывные эмиттеры редеют на 30%
        if (G.isMobile() && R.nextFloat() < 0.3f) return;
        if (parts.size >= MAX) return;
        P p = obtain();
        p.c = c; p.x = at.x; p.y = at.y; p.z = at.z;
        if (c.sphere > 0) {
            randDir(sphereCfg, tdir);
            float r = c.sphere * (float) Math.cbrt(R.nextFloat());
            p.x += tdir.x * r; p.y += tdir.y * r; p.z += tdir.z * r;
        }
        randDir(c, tdir);
        float v = rr(c.vMin, c.vMax);
        p.vx = tdir.x * v; p.vy = tdir.y * v; p.vz = tdir.z * v;
        p.age = 0; p.life = c.life; p.sc = rr(c.sMin, c.sMax); p.damp = rr(c.dMin, c.dMax);
        p.rot = R.nextFloat() * MathUtils.PI2; p.rotV = 0;
        p.tacc = 0; p.landed = false; // частица могла прийти из пула «приземлившейся» гильзой или обломком
        parts.add(p);
    }

    static void rampAt(float[] r, float t, float[] out) {
        if (r == null) { out[0] = out[1] = out[2] = out[3] = 1; return; }
        int n = r.length / 5;
        if (t <= r[0]) { System.arraycopy(r, 1, out, 0, 4); return; }
        for (int i = 0; i < n - 1; i++) {
            float o0 = r[i * 5], o1 = r[(i + 1) * 5];
            if (t <= o1) {
                float k = (t - o0) / Math.max(1e-6f, o1 - o0);
                for (int j = 0; j < 4; j++) out[j] = MathUtils.lerp(r[i * 5 + 1 + j], r[(i + 1) * 5 + 1 + j], k);
                return;
            }
        }
        System.arraycopy(r, (n - 1) * 5 + 1, out, 0, 4);
    }

    static final float[] col = new float[4];

    /** Заполнить динамические буферы частиц для кадра. */
    public static void fill(Camera cam) {
        dAdd.clear(); dSmoke.clear(); dDebris.clear(); dRing.clear(); dCasing.clear();
        fillLights(cam);
        right.set(cam.direction).crs(cam.up).nor();
        up.set(right).crs(cam.direction).nor();
        for (int i = 0; i < parts.size; i++) {
            P p = parts.get(i);
            Cfg c = p.c;
            float t = p.age / p.life;
            if (c.kind == K_DEBRIS || c.kind == K_CASING) {
                tm.idt().translate(p.x, p.y, p.z).rotateRad(tv.set(p.ax, p.ay, p.az).nor(), p.rot).scale(c.bx, c.by, c.bz);
                (c.kind == K_CASING ? dCasing : dDebris).prim(Dyn.BOX, tm, 1, 1, 1, 1);
                continue;
            }
            rampAt(c.ramp, t, col);
            float sz = p.sc * MathUtils.lerp(c.cA, c.cB, t);
            tv.set(p.x, p.y, p.z);
            if (c.kind == K_RING) {
                if (c.flat) flatQuad(dRing, tv, sz, col[0], col[1], col[2], col[3]);
                else dRing.billboard(tv, sz, right, up, col[0], col[1], col[2], col[3], 0);
                continue;
            }
            if (c.kind == K_SMOKE) dSmoke.billboard(tv, sz, right, up, col[0] * smokeLum, col[1] * smokeLum, col[2] * smokeLum, col[3], p.rot);
            else dAdd.billboard(tv, sz, right, up, col[0], col[1], col[2], col[3], 0);
        }
        // свечения горелок-хвостов
        for (int i = 0; i < emitters.size; i++) {
            Emitter e = emitters.get(i);
            if (e.glowCfg == null || e.anchor == null || !e.emitting) continue;
            e.anchor.toGlobal(e.off, tv);
            dAdd.billboard(tv, e.glowCfg.size, right, up, e.glowCfg.r, e.glowCfg.g, e.glowCfg.b, 0.8f, 0);
        }
        // свечения факелов ракет
        for (int i = flames.size - 1; i >= 0; i--) {
            Node3 f = flames.get(i);
            if (!attached(f)) { flames.removeIndex(i); continue; }
            f.toGlobal(tv.set(0, 0, 0.3f), tv);
            float k = f.scl.x;
            dAdd.billboard(tv, 2.6f * k, right, up, 1f, 0.72f * 0.85f, 0.42f * 0.85f, 0.85f, 0);
        }
        for (int i = 0; i < fglow.size; i += 8) {
            float[] g = fglow.items;
            tv.set(g[i], g[i + 1], g[i + 2]);
            dAdd.billboard(tv, g[i + 3], right, up, g[i + 4], g[i + 5], g[i + 6], g[i + 7], 0);
        }
        fglow.clear();
        SmokeTrail.dyn.clear();
        for (int i = 0; i < trails.size; i++) trails.get(i).fill(cam);
    }

    static void flatQuad(Dyn d, Vector3 p, float size, float r, float g, float b, float a) {
        float h = size * 0.5f;
        float ax = p.x - h, az = p.z - h, bx = p.x + h, bz = p.z + h, y = p.y;
        d.vert(ax, y, az, 0, 1, 0, r, g, b, a, 0, 0); d.vert(bx, y, az, 0, 1, 0, r, g, b, a, 1, 0); d.vert(bx, y, bz, 0, 1, 0, r, g, b, a, 1, 1);
        d.vert(ax, y, az, 0, 1, 0, r, g, b, a, 0, 0); d.vert(bx, y, bz, 0, 1, 0, r, g, b, a, 1, 1); d.vert(ax, y, bz, 0, 1, 0, r, g, b, a, 0, 1);
    }

    public static void clear() {
        for (P p : parts) free.add(p);
        parts.clear();
        emitters.clear();
        trails.clear();
        flames.clear();
        lights.clear();
        fglow.clear();
        fires = 0;
        if (env != null) env.plN = 0;
    }

    private FX() { }
}
