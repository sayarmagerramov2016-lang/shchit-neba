package ru.shitneba;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import ru.shitneba.gfx.MB;
import ru.shitneba.gfx.Mat;
import ru.shitneba.gfx.Mesh3;
import ru.shitneba.gfx.Node3;
import ru.shitneba.gfx.Tex;

/**
 * 3.4.0 — карты кампании. У каждой главы своя местность:
 * рельеф (shape), зоны без деревьев (free) и постройки (build).
 * Игрок всегда в центре (0,0), враги идут с −Z, дорога на x = World.ROAD_X.
 */
final class Maps {
    /** Карта по номеру главы (1..11). */
    static final String[] BY_CH = {"valley", "river", "hub", "airfield", "fields", "port", "taiga", "oil", "forge", "city", "tower"};

    static String forMission(String id) {
        try {
            int ch = Integer.parseInt(id.substring(0, id.indexOf('-')));
            return BY_CH[MathUtils.clamp(ch - 1, 0, BY_CH.length - 1)];
        } catch (Exception e) { return "valley"; }
    }

    /** Старая деревня (290, −460) остаётся только на этих картах. */
    static boolean village(String map) { return map.equals("valley") || map.equals("port"); }

    /** Где кончается дорога (по −Z). На аэродроме упирается в полосу. */
    static float roadEnd(String map) { return map.equals("airfield") ? -205f : -1100f; }

    // ---------- плотина на «Большой воде» ----------
    static final float DAM_X = 120f, DAM_Z = -240f, LAKE_Y = 14f, RIVER_Y = -1f;
    static final float[][] RIVER = {{120, -226}, {128, -160}, {168, -84}, {246, 6}, {372, 86}, {600, 158}, {900, 200}};

    static float riverDist(float x, float z) {
        float best = 1e9f;
        for (int i = 0; i + 1 < RIVER.length; i++) {
            float ax = RIVER[i][0], az = RIVER[i][1], bx = RIVER[i + 1][0], bz = RIVER[i + 1][1];
            float dx = bx - ax, dz = bz - az, t = MathUtils.clamp(((x - ax) * dx + (z - az) * dz) / (dx * dx + dz * dz), 0, 1);
            float px = ax + dx * t - x, pz = az + dz * t - z;
            best = Math.min(best, px * px + pz * pz);
        }
        return (float) Math.sqrt(best);
    }

    /** «Форма» водохранилища: <1 — вода, 1..1.15 — берег. */
    static float lakeE(float x, float z) {
        float ex = Math.abs(x - DAM_X) / 110f;
        float ez = z < -900 ? (-900 - z) / 100f : 0f;
        return (float) Math.sqrt(ex * ex + ez * ez);
    }

    // ---------- озеро в тайге ----------
    static final float ICE_X = 150f, ICE_Z = -275f, ICE_RX = 140f, ICE_RZ = 90f;

    static float iceE(float x, float z) {
        float dx = (x - ICE_X) / ICE_RX, dz = (z - ICE_Z) / ICE_RZ;
        return (float) Math.sqrt(dx * dx + dz * dz);
    }

    // ---------- рельеф ----------
    static float smooth(float t) { t = MathUtils.clamp(t, 0, 1); return t * t * (3 - 2 * t); }

    /** Выровнять прямоугольник до высоты y с плавным краем blend. */
    static float flat(float h, float x, float z, float x0, float z0, float x1, float z1, float y, float blend) {
        float dx = Math.max(0, Math.max(x0 - x, x - x1)), dz = Math.max(0, Math.max(z0 - z, z - z1));
        float d = (float) Math.sqrt(dx * dx + dz * dz);
        return MathUtils.lerp(h, y, smooth(1 - d / blend));
    }

    static float shape(String map, float x, float z, float h) {
        switch (map) {
            case "river": {
                // упоры плотины в берега
                for (float ax : new float[]{DAM_X - 115, DAM_X + 115}) {
                    float d = (float) Math.sqrt((x - ax) * (x - ax) + (z - DAM_Z + 4) * (z - DAM_Z + 4));
                    h = Math.max(h, 26f - 0.38f * d);
                }
                if (z < -249) {
                    float e = lakeE(x, z);
                    if (e < 1.6f) {
                        h += 12f * MathUtils.sin(MathUtils.PI * MathUtils.clamp((e - 1f) / 0.6f, 0, 1));
                        float basin = -3f + Math.max(0, e - 1f) * 120f;
                        h = Math.min(h, basin);
                        if (e > 1f && e < 1.35f) h = Math.max(h, Math.min(basin, LAKE_Y + 2));
                    }
                }
                float rd = riverDist(x, z);
                if (rd < 80 && z > -236) {
                    // широкое русло: мелкие детали сетка рельефа не «видит»
                    float bed = -2.6f + Math.max(0, rd - 26) * 0.3f;
                    h = Math.min(h, bed);
                    if (rd > 27 && rd < 60) h = Math.max(h, Math.min(bed, 0.8f));
                }
                return h;
            }
            case "hub": return flat(h, x, z, 40, -440, 430, -90, 3f, 50f);
            case "airfield":
                h = flat(h, x, z, -1150, -390, 1150, -210, 2f, 70f);
                return flat(h, x, z, 110, -220, 480, -40, 2f, 45f);
            case "fields": return h * 0.4f;
            case "port": return flat(h, x, z, -117, -830, -56, -90, 2.4f, 18f);
            case "taiga": {
                float e = iceE(x, z);
                if (e < 1.5f) h = Math.min(h, -1.4f + Math.max(0, e - 1f) * 40f);
                return flat(h, x, z, -250, -290, -100, -120, Math.max(-1, h * 0.3f), 30f);
            }
            case "oil": return flat(h, x, z, -310, -320, -150, -160, 1.5f, 40f);
            case "forge": h = flat(h, x, z, -1150, -240, 1150, -180, 1.5f, 40f); return flat(h, x, z, 100, -460, 280, -300, 1.5f, 40f);
            case "city": h = flat(h * 0.5f, x, z, 60, -520, 430, -110, 2f, 60f); return flat(h, x, z, -310, -410, -90, -150, 2f, 50f);
            case "tower": return flat(h, x, z, 40, -640, 200, -480, 4f, 60f);
            default: return h;
        }
    }

    // ---------- зоны без деревьев и камней ----------
    static final Array<float[]> keep = new Array<>();
    /** Поля «Поводка»: x0, z0, x1, z1, тип. */
    static final Array<float[]> fields = new Array<>();
    /** Дома «Химеры»: x, z, ry, длина, глубина, этажи. */
    static final Array<float[]> blocks = new Array<>();

    static float[] r(float x0, float z0, float x1, float z1) { return new float[]{x0, z0, x1, z1}; }

    static void prepare(String map) {
        keep.clear(); fields.clear(); blocks.clear(); cut.clear();
        switch (map) {
            case "river": cut.add(r(-15, -250, 290, -50)); break;
            case "port": cut.add(r(-140, -620, -50, -40)); break;
            case "taiga": cut.add(r(-10, -190, 320, -50)); break;
            case "tower": cut.add(r(-60, -500, 260, -80)); break;
            default: break;
        }
        Random R = new Random(map.hashCode());
        switch (map) {
            case "river": keep.add(r(-10, -1040, 250, -230)); break;
            case "hub": keep.add(r(30, -450, 440, -80)); break;
            case "airfield": keep.add(r(-1160, -410, 1160, -200)); keep.add(r(100, -220, 490, -30)); break;
            case "fields": {
                // лоскутное одеяло полей между дорогой и горизонтом
                for (float z = -60; z > -1000; z -= 0) {
                    float d = 90 + R.nextFloat() * 90;
                    for (float x = -760; x < 820; ) {
                        float w = 110 + R.nextFloat() * 120;
                        float x1 = Math.min(x + w, 820);
                        if (x1 > World.ROAD_X - 14 && x < World.ROAD_X + 14) { // дорога и лесополоса вдоль неё
                            if (World.ROAD_X - 14 - x > 40) fields.add(new float[]{x + 3, z - d + 3, World.ROAD_X - 14, z - 3, R.nextInt(5)});
                            x = World.ROAD_X + 14; continue;
                        }
                        if (R.nextFloat() < 0.85f) fields.add(new float[]{x + 3, z - d + 3, x1 - 3, z - 3, R.nextInt(5)});
                        x = x1 + (R.nextFloat() < 0.3f ? 12 : 0);
                    }
                    z -= d + (R.nextFloat() < 0.4f ? 14 : 0);
                }
                for (float[] f : fields) keep.add(f);
                keep.add(r(150, -380, 320, -210));
                break;
            }
            case "port": keep.add(r(-420, -850, -54, -80)); break;
            case "taiga": keep.add(r(-260, -300, -95, -110)); break;
            case "oil": keep.add(r(-320, -330, -140, -150)); keep.add(r(-700, -160, 700, -125)); break;
            case "forge": keep.add(r(-1150, -245, 1150, -175)); keep.add(r(90, -470, 290, -290)); keep.add(r(-390, -610, -180, -420)); break;
            case "city": {
                keep.add(r(55, -525, 435, -100));
                keep.add(r(-315, -415, -85, -145));
                // микрорайон: длинные девятиэтажки и башни-шестнадцатиэтажки
                float[][] spots = {
                        {140, -330, 0, 72, 12, 9}, {260, -320, 0, 60, 12, 9}, {380, -345, 0.05f, 84, 12, 9},
                        {520, -330, 0, 24, 24, 16}, {130, -470, MathUtils.PI / 2, 96, 12, 12}, {250, -470, 0, 24, 24, 16},
                        {350, -480, 0, 24, 24, 16}, {480, -460, 0, 72, 12, 9}, {600, -520, MathUtils.PI / 2, 84, 12, 12},
                        {180, -620, 0, 84, 13, 14}, {330, -640, 0, 72, 12, 9}, {470, -620, 0, 24, 24, 17},
                        {560, -720, 0, 96, 12, 12}, {260, -770, 0, 24, 24, 16}, {130, -760, 0, 60, 12, 9},
                        {-180, -340, 0, 60, 12, 5}, {-300, -350, 0, 60, 12, 5}, {-420, -360, 0, 48, 12, 5},
                        {-200, -470, 0, 24, 24, 12}, {-330, -500, MathUtils.PI / 2, 72, 12, 9}, {-440, -560, 0, 60, 12, 9}};
                for (float[] s : spots) blocks.add(new float[]{s[0] * 0.66f + (s[0] < 0 ? 20 : 5), s[1] * 0.62f + 5, s[2], s[3], s[4], s[5]});
                break;
            }
            case "tower": keep.add(r(30, -650, 210, -470)); keep.add(r(-360, -420, -110, -200)); break;
            default: break;
        }
    }

    /** Можно ли тут траву и камни (нет построек, воды, полей). */
    static boolean free(String map, float x, float z) {
        for (float[] k : keep) if (x > k[0] && x < k[2] && z > k[1] && z < k[3]) return false;
        if (map.equals("river") && riverDist(x, z) < 48) return false;
        if (map.equals("taiga") && iceE(x, z) < 1.15f) return false;
        return true;
    }

    /** Вырубки: лес не заслоняет главное на карте. */
    static final Array<float[]> cut = new Array<>();

    /** Можно ли тут дерево. */
    static boolean treeOk(String map, float x, float z) {
        if (!free(map, x, z)) return false;
        for (float[] k : cut) if (x > k[0] && x < k[2] && z > k[1] && z < k[3]) return false;
        // на застроенных картах ближний лес вырублен — видно постройки
        if ((map.equals("hub") || map.equals("forge") || map.equals("city") || map.equals("airfield")) && z < 40 && x * x + z * z < 330 * 330 && x > World.ROAD_X + 10) return false;
        return true;
    }

    // =================================================================
    // постройки
    // =================================================================
    final World w;
    final String map;
    final Random R;
    final MB wall = new MB(), metal = new MB(), glow = new MB(), glass = new MB(), fac = new MB(), lit = new MB(), wat = new MB(), ice = new MB(), soil = new MB();
    final boolean night;

    Maps(World w) {
        this.w = w; this.map = w.map; this.night = w.night;
        R = new Random(map.hashCode() * 31L + 7);
    }

    static Color c(String h) { return G.hex(h); }

    float rr(float a, float b) { return a + R.nextFloat() * (b - a); }

    float gy(float x, float z) { return w.height(x, z); }

    Color vary(Color base, float amt) {
        float d = rr(-amt, amt);
        return new Color(MathUtils.clamp(base.r + d, 0, 1), MathUtils.clamp(base.g + d, 0, 1), MathUtils.clamp(base.b + d, 0, 1), 1);
    }

    static void vtx(MB b, float x, float y, float z, float nx, float ny, float nz, Color c, float u, float v) {
        b.v.addAll(x, y, z, nx, ny, nz, c.r, c.g, c.b, c.a, u, v);
    }

    /** Четырёхугольник a-b-c-d (против часовой со стороны лица), uv a(u0,v0)…c(u1,v1). */
    static void quad(MB b, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz,
                     Color col, float u0, float v0, float u1, float v1) {
        float ex = bx - ax, ey = by - ay, ez = bz - az, fx = dx - ax, fy = dy - ay, fz = dz - az;
        float nx = ey * fz - ez * fy, ny = ez * fx - ex * fz, nz = ex * fy - ey * fx;
        float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (l < 1e-8f) return;
        nx /= l; ny /= l; nz /= l;
        // в рендере лицевые грани — по часовой (glFrontFace CW), поэтому обход a-c-b
        vtx(b, ax, ay, az, nx, ny, nz, col, u0, v0); vtx(b, cx, cy, cz, nx, ny, nz, col, u1, v1); vtx(b, bx, by, bz, nx, ny, nz, col, u1, v0);
        vtx(b, ax, ay, az, nx, ny, nz, col, u0, v0); vtx(b, dx, dy, dz, nx, ny, nz, col, u0, v1); vtx(b, cx, cy, cz, nx, ny, nz, col, u1, v1);
    }

    /** Поворот локальной точки вокруг Y (как rotateRad у libGDX). */
    static float rx(float lx, float lz, float ry) { return lx * MathUtils.cos(ry) + lz * MathUtils.sin(ry); }

    static float rz(float lx, float lz, float ry) { return -lx * MathUtils.sin(ry) + lz * MathUtils.cos(ry); }

    void add(MB b, Mat m) { if (!b.empty()) w.add(b.mesh(), m); }

    void build() {
        MB.hq = false;
        switch (map) {
            case "river": river(); break;
            case "hub": hub(); break;
            case "airfield": airfield(); break;
            case "fields": fieldsMap(); break;
            case "port": port(); break;
            case "taiga": taiga(); break;
            case "oil": oil(); break;
            case "forge": forge(); break;
            case "city": city(); break;
            case "tower": tower(); break;
            default: return;
        }
        Mat wm = Mat.vc(); wm.rough = 0.9f; wm.shadowRecv = true;
        add(wall, wm);
        Mat mm = Mat.vc(); mm.rough = 0.5f; mm.metal = 0.35f; mm.spec = 0.8f;
        add(metal, mm);
        add(glass, Models.MAT_GLASS);
        Mat sm = Mat.vc(); sm.rough = 1f; sm.spec = 0.15f; sm.tex = Tex.grain("soil", 0.8f, 1600, 0.55f, 1.05f); sm.shadowRecv = true;
        add(soil, sm);
        if (!fac.empty()) {
            Mat fm = Mat.vc(); fm.tex = winTex(); fm.rough = 0.9f; fm.spec = 0.35f; fm.shadowRecv = true;
            add(fac, fm);
        }
        if (!lit.empty() && night) {
            Mat lm = new Mat(); lm.unshaded = true; lm.vcol = true; lm.tex = litTex(); lm.blend = Mat.ADD; lm.depthWrite = false; lm.order = 2;
            add(lit, lm);
        }
        if (!glow.empty()) add(glow, Models.MAT_GLOW);
        if (!wat.empty()) { Mat m = Mat.vc(); m.rough = 0.1f; m.metal = 0.2f; m.spec = 1.5f; add(wat, m); }
        if (!ice.empty()) { Mat m = Mat.vc(); m.rough = 0.25f; m.spec = 1.1f; m.tex = Tex.grain("ice", 0.92f, 500, 0.8f, 1.0f); add(ice, m); }
    }

    // ---------- общие детали ----------

    /** Вода: клетки сетки там, где земля ниже уровня (края прячутся под берегом). */
    void water(float x0, float z0, float x1, float z1, float y, float cell, Color col) {
        for (float x = x0; x < x1; x += cell) for (float z = z0; z < z1; z += cell) {
            float a = gy(x, z), b = gy(x + cell, z), c2 = gy(x + cell, z + cell), d = gy(x, z + cell), m = gy(x + cell / 2, z + cell / 2);
            if (Math.min(Math.min(a, b), Math.min(Math.min(c2, d), m)) >= y - 0.05f) continue;
            quad(wat, x, y, z + cell, x + cell, y, z + cell, x + cell, y, z, x, y, z, col, 0, 0, 1, 1);
        }
    }

    /** Простой дом с двускатной крышей. */
    void house(float x, float z, float ry, float wd, float dp, float h, Color wc, Color roof) {
        float y = gy(x, z);
        wall.box(wd, h, dp, wc, x, y + h / 2 - 0.4f, z, 0, ry, 0);
        float rh = dp * 0.38f;
        wall.extrude(new float[][]{{-dp / 2 - 0.5f, 0}, {dp / 2 + 0.5f, 0}, {0, rh}}, wd + 0.8f, roof, x, y + h - 0.4f, z, 0, ry, 0);
        // окна
        for (int s = -1; s <= 1; s += 2) for (int i = 0; i < Math.max(1, (int) (wd / 4)); i++) {
            float lx = -wd / 2 + (i + 0.5f) * wd / Math.max(1, (int) (wd / 4)), lz = s * (dp / 2 + 0.05f);
            Color wcol = night && R.nextFloat() < 0.6f ? c("ffcf7a") : c("2a3440");
            MB tgt = night && wcol.r > 0.5f ? glow : glass;
            tgt.box(1.1f, 1.2f, 0.12f, wcol, x + rx(lx, lz, ry), y + h * 0.55f, z + rz(lx, lz, ry), 0, ry, 0);
        }
    }

    /** Арочный ангар вдоль локальной оси Z (длина L, радиус Rr). */
    void arch(float x, float z, float ry, float Rr, float L, Color col, Color end, boolean doorOpen) {
        float y = gy(x, z) - 0.3f;
        int N = 12;
        for (int i = 0; i < N; i++) {
            float a0 = MathUtils.PI * i / N, a1 = MathUtils.PI * (i + 1) / N;
            float x0 = MathUtils.cos(a0) * Rr, y0 = MathUtils.sin(a0) * Rr * 0.8f, x1 = MathUtils.cos(a1) * Rr, y1 = MathUtils.sin(a1) * Rr * 0.8f;
            Color cc = i % 2 == 0 ? col : vary(col, 0.02f);
            // наружная поверхность: от +L/2 к −L/2
            quad(metal, x + rx(x1, L / 2, ry), y + y1, z + rz(x1, L / 2, ry), x + rx(x0, L / 2, ry), y + y0, z + rz(x0, L / 2, ry),
                    x + rx(x0, -L / 2, ry), y + y0, z + rz(x0, -L / 2, ry), x + rx(x1, -L / 2, ry), y + y1, z + rz(x1, -L / 2, ry), cc, 0, 0, 1, 1);
            // рёбра жёсткости
            if (i % 2 == 0) for (int k = -2; k <= 2; k++) {
                float lz = k * L / 5f;
                metal.seg(new Vector3(x + rx(x0 * 1.01f, lz, ry), y + y0 * 1.01f, z + rz(x0 * 1.01f, lz, ry)),
                        new Vector3(x + rx(x1 * 1.01f, lz, ry), y + y1 * 1.01f, z + rz(x1 * 1.01f, lz, ry)), 0.18f, 0.18f, 4, vary(col, 0.06f));
            }
        }
        // торцы: стенка-полукруг, ворота
        for (int s = -1; s <= 1; s += 2) {
            float lz = s * L / 2;
            for (int i = 0; i < N; i++) {
                float a0 = MathUtils.PI * i / N, a1 = MathUtils.PI * (i + 1) / N;
                float x0 = MathUtils.cos(a0) * Rr, y0 = MathUtils.sin(a0) * Rr * 0.8f, x1 = MathUtils.cos(a1) * Rr, y1 = MathUtils.sin(a1) * Rr * 0.8f;
                float cx0 = x + rx(0, lz, ry), cz0 = z + rz(0, lz, ry);
                if (s < 0) {
                    wall.v.addAll(cx0, y, cz0, 0, 0, 0, end.r, end.g, end.b, 1, 0, 0);
                    wall.v.addAll(x + rx(x0, lz, ry), y + y0, z + rz(x0, lz, ry), 0, 0, 0, end.r, end.g, end.b, 1, 0, 0);
                    wall.v.addAll(x + rx(x1, lz, ry), y + y1, z + rz(x1, lz, ry), 0, 0, 0, end.r, end.g, end.b, 1, 0, 0);
                } else {
                    wall.v.addAll(cx0, y, cz0, 0, 0, 0, end.r, end.g, end.b, 1, 0, 0);
                    wall.v.addAll(x + rx(x1, lz, ry), y + y1, z + rz(x1, lz, ry), 0, 0, 0, end.r, end.g, end.b, 1, 0, 0);
                    wall.v.addAll(x + rx(x0, lz, ry), y + y0, z + rz(x0, lz, ry), 0, 0, 0, end.r, end.g, end.b, 1, 0, 0);
                }
                float nx = MathUtils.sin(ry) * s, nz = MathUtils.cos(ry) * s;
                for (int q = wall.v.size - 36; q < wall.v.size; q += 12) { wall.v.items[q + 3] = nx; wall.v.items[q + 4] = 0; wall.v.items[q + 5] = nz; }
            }
            // ворота
            float dw = Rr * 1.1f, dh = Rr * 0.55f;
            float px = x + rx(0, lz + s * 0.15f, ry), pz = z + rz(0, lz + s * 0.15f, ry);
            wall.box(dw, dh, 0.3f, doorOpen && s > 0 ? c("15171a") : vary(c("6c7076"), 0.03f), px, y + dh / 2, pz, 0, ry, 0);
            for (int k = 1; k < 6; k++) metal.box(0.12f, dh, 0.4f, c("4a4e54"), px + rx(-dw / 2 + k * dw / 6, 0, ry), y + dh / 2, pz + rz(-dw / 2 + k * dw / 6, 0, ry), 0, ry, 0);
            if (night) glow.box(1.2f, 0.4f, 0.4f, c("fff0c8"), px, y + dh + 1.2f, pz, 0, ry, 0);
            else metal.box(1.2f, 0.4f, 0.4f, c("888a86"), px, y + dh + 1.2f, pz, 0, ry, 0);
        }
    }

    /** Решётчатая мачта/опора: 4 ноги, пояса и раскосы. */
    void lattice(float x, float z, float h, float b0, float b1, float ry, Color col, float rad) {
        float y = gy(x, z);
        int lv = Math.max(3, (int) (h / 9));
        float[][] lx = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
        Vector3[] prev = null;
        for (int i = 0; i <= lv; i++) {
            float t = (float) i / lv, hb = MathUtils.lerp(b0, b1, t) / 2, yy = y + t * h;
            Vector3[] cur = new Vector3[4];
            for (int k = 0; k < 4; k++) cur[k] = new Vector3(x + rx(lx[k][0] * hb, lx[k][1] * hb, ry), yy, z + rz(lx[k][0] * hb, lx[k][1] * hb, ry));
            if (prev != null) for (int k = 0; k < 4; k++) {
                metal.seg(prev[k], cur[k], rad * 1.4f, rad * 1.4f, 4, col);
                metal.seg(prev[k], cur[(k + 1) % 4], rad * 0.8f, rad * 0.8f, 3, col);
                metal.seg(cur[k], cur[(k + 1) % 4], rad * 0.8f, rad * 0.8f, 3, col);
            }
            prev = cur;
        }
    }

    /** ЛЭП: опора с траверсами. Возвращает точки подвеса проводов. */
    Vector3[] pylon(float x, float z, float ry, float h) {
        Color col = c("7d8288");
        lattice(x, z, h, 7f, 1.6f, ry, col, 0.22f);
        float y = gy(x, z);
        Vector3[] pts = new Vector3[3];
        float[][] arms = {{-7.5f, h * 0.72f}, {7.5f, h * 0.72f}, {0, h * 0.92f}};
        for (int i = 0; i < 2; i++) {
            float s = i == 0 ? -1 : 1, yy = y + h * 0.72f;
            metal.seg(new Vector3(x + rx(s * 0.9f, 0, ry), yy, z + rz(s * 0.9f, 0, ry)), new Vector3(x + rx(s * 8.5f, 0, ry), yy, z + rz(s * 8.5f, 0, ry)), 0.25f, 0.18f, 4, col);
            metal.seg(new Vector3(x + rx(s * 0.8f, 0, ry), yy + 3.5f, z + rz(s * 0.8f, 0, ry)), new Vector3(x + rx(s * 8.5f, 0, ry), yy, z + rz(s * 8.5f, 0, ry)), 0.15f, 0.15f, 3, col);
        }
        metal.seg(new Vector3(x, y + h * 0.9f, z), new Vector3(x, y + h + 3, z), 0.2f, 0.1f, 4, col);
        for (int i = 0; i < 3; i++) {
            float ax = arms[i][0], ay = arms[i][1];
            float px = x + rx(ax == 0 ? 1.5f : ax, 0, ry), pz = z + rz(ax == 0 ? 1.5f : ax, 0, ry);
            metal.cyl(0.25f, 0.25f, 2.6f, 6, c("c9c3b4"), px, y + ay - 1.3f, pz);
            pts[i] = new Vector3(px, y + ay - 2.6f, pz);
        }
        return pts;
    }

    /** Провисающий провод между двумя точками. */
    void wire(Vector3 a, Vector3 b, float sag, float rad) {
        int n = 8;
        Vector3 p0 = new Vector3(a);
        Color col = c("2a2c2e");
        for (int i = 1; i <= n; i++) {
            float t = (float) i / n;
            Vector3 p1 = new Vector3(a).lerp(b, t);
            p1.y -= sag * 4 * t * (1 - t);
            metal.seg(p0, p1, rad, rad, 3, col);
            p0 = p1;
        }
    }

    /** Линия ЛЭП по точкам. */
    void powerLine(float[][] pts, float h) {
        Vector3[] prev = null;
        for (int i = 0; i < pts.length; i++) {
            float ry;
            if (i + 1 < pts.length) ry = MathUtils.atan2(pts[i + 1][0] - pts[i][0], pts[i + 1][1] - pts[i][1]) + MathUtils.PI / 2;
            else ry = MathUtils.atan2(pts[i][0] - pts[i - 1][0], pts[i][1] - pts[i - 1][1]) + MathUtils.PI / 2;
            Vector3[] cur = pylon(pts[i][0], pts[i][1], ry, h);
            if (prev != null) for (int k = 0; k < 3; k++) wire(prev[k], cur[k], 6f, 0.07f);
            prev = cur;
        }
    }

    /** Контейнер 6 или 12 м. */
    void container(float x, float y, float z, float ry, boolean big, Color col) {
        float L = big ? 12.2f : 6.1f;
        metal.box(2.44f, 2.6f, L, col, x, y + 1.3f, z, 0, ry, 0);
        Color d = new Color(col).mul(0.78f, 0.78f, 0.78f, 1);
        if (!G.isMobile()) for (int k = 0; k < (big ? 4 : 3); k++) {
            float lz = -L / 2 + 0.6f + k * (L - 1.2f) / (big ? 3 : 2);
            for (int s = -1; s <= 1; s += 2)
                metal.box(0.08f, 2.4f, 0.25f, d, x + rx(s * 1.24f, lz, ry), y + 1.3f, z + rz(s * 1.24f, lz, ry), 0, ry, 0);
        }
    }

    static final Color[] BOX = {G.hex("a8382a"), G.hex("2f5f8a"), G.hex("c08a2a"), G.hex("3f6f4a"), G.hex("7a7f86"), G.hex("8a4a2a"), G.hex("d0cfc8")};

    void stack(float x, float z, float ry, int rows, int cols, int lv) {
        float y = gy(x, z);
        for (int i = 0; i < rows; i++) for (int j = 0; j < cols; j++) {
            int hgt = 1 + R.nextInt(lv);
            for (int k = 0; k < hgt; k++) {
                float lx = i * 2.6f, lz = j * 12.6f;
                container(x + rx(lx, lz, ry), y + k * 2.62f, z + rz(lx, lz, ry), ry, true, BOX[R.nextInt(BOX.length)]);
            }
        }
    }

    /** Мачта освещения. */
    void lamp(float x, float z, float h) {
        float y = gy(x, z);
        metal.cyl(0.18f, 0.28f, h, 6, c("666a6e"), x, y + h / 2, z);
        metal.box(2.2f, 0.4f, 0.8f, c("444444"), x, y + h, z);
        if (night) glow.box(2f, 0.15f, 0.6f, c("fff2cc"), x, y + h - 0.25f, z);
    }

    /** Грузовик с прицепом-фурой (простой, для стоянок). */
    void truck(float x, float z, float ry, Color cab, Color trailer) {
        float y = gy(x, z);
        wall.box(2.5f, 2.8f, 2.4f, cab, x + rx(0, -6.3f, ry), y + 2.1f, z + rz(0, -6.3f, ry), 0, ry, 0);
        glass.box(2.3f, 1f, 0.1f, c("1a2633"), x + rx(0, -7.52f, ry), y + 2.8f, z + rz(0, -7.52f, ry), 0, ry, 0);
        wall.box(2.55f, 3f, 11f, trailer, x + rx(0, 0.8f, ry), y + 2.6f, z + rz(0, 0.8f, ry), 0, ry, 0);
        for (float lz : new float[]{-6.4f, -4.2f, 3.6f, 4.9f, 6.2f}) for (int s = -1; s <= 1; s += 2)
            metal.cyl(0.52f, 0.52f, 0.4f, 8, c("1c1c1c"), x + rx(s * 1.1f, lz, ry), y + 0.52f, z + rz(s * 1.1f, lz, ry), 0, ry, MathUtils.PI / 2);
    }

    /** Окна: днём тёмное стекло в панельной стене. 8×8 ячеек, 1 ячейка = 1 окно. */
    static final Map<String, Texture> TEX = new HashMap<>();

    static Texture winTex() {
        Texture t = TEX.get("win");
        if (t != null) return t;
        int C = 32, S = C * 8;
        Random rn = new Random(5);
        Tex.Img im = new Tex.Img(S, S, new Color(0.86f, 0.85f, 0.82f, 1));
        for (int i = 0; i < S * S; i++) { float k = (rn.nextFloat() - 0.5f) * 0.05f; im.p[i * 4] += k; im.p[i * 4 + 1] += k; im.p[i * 4 + 2] += k; }
        Color seam = new Color(0.62f, 0.61f, 0.58f, 1), frame = new Color(0.93f, 0.93f, 0.9f, 1);
        for (int cy = 0; cy < 8; cy++) for (int cx = 0; cx < 8; cx++) {
            int x0 = cx * C, y0 = cy * C;
            im.fillRect(x0, y0, C, 1, seam); im.fillRect(x0, y0, 1, C, seam);
            im.fillRect(x0 + 7, y0 + 7, 18, 17, frame);
            float g = 0.12f + rn.nextFloat() * 0.1f;
            Color gl = rn.nextFloat() < 0.25f ? new Color(0.55f + rn.nextFloat() * 0.2f, 0.5f, 0.42f, 1) : new Color(g, g + 0.03f, g + 0.07f, 1);
            im.fillRect(x0 + 8, y0 + 8, 16, 15, gl);
            im.fillRect(x0 + 15, y0 + 8, 2, 15, frame);
        }
        t = Tex.tex(im, true);
        TEX.put("win", t);
        return t;
    }

    /** Окна ночью: горящие окна на чёрном (рисуется сложением поверх стены). */
    static Texture litTex() {
        Texture t = TEX.get("lit");
        if (t != null) return t;
        int C = 32, S = C * 8;
        Random rn = new Random(9);
        Tex.Img im = new Tex.Img(S, S, new Color(0, 0, 0, 1));
        Color[] warm = {new Color(1f, 0.78f, 0.42f, 1), new Color(1f, 0.88f, 0.6f, 1), new Color(0.75f, 0.85f, 1f, 1), new Color(1f, 0.62f, 0.32f, 1)};
        for (int cy = 0; cy < 8; cy++) for (int cx = 0; cx < 8; cx++) {
            if (rn.nextFloat() > 0.38f) continue;
            Color k = warm[rn.nextInt(warm.length)].cpy().mul(0.6f + rn.nextFloat() * 0.4f);
            im.fillRect(cx * C + 8, cy * C + 8, 16, 15, k);
        }
        t = Tex.tex(im, true);
        TEX.put("lit", t);
        return t;
    }

    /** Панельный дом: стены с окнами (uv: 1 = одно окно), крыша, подъезды, ночью горят окна. */
    void panel(float x, float z, float ry, float L, float D, int floors) {
        float y = gy(x, z) - 0.5f, fh = 2.9f, H = floors * fh + 0.8f;
        Color wc = vary(R.nextFloat() < 0.5f ? c("cfcabd") : c("bfc4c6"), 0.03f);
        if (night) wc.mul(0.55f, 0.55f, 0.6f, 1);
        int cl = Math.max(2, Math.round(L / 3.3f)), cd = Math.max(2, Math.round(D / 3.3f));
        float u0 = R.nextInt(8), v0 = R.nextInt(8);
        float hx = L / 2, hz = D / 2;
        // 4 стены: передняя (+z), правая (+x), задняя (−z), левая (−x)
        float[][] fc = {{-hx, hz, hx, hz}, {hx, hz, hx, -hz}, {hx, -hz, -hx, -hz}, {-hx, -hz, -hx, hz}};
        for (int f = 0; f < 4; f++) {
            float ax = x + rx(fc[f][0], fc[f][1], ry), az = z + rz(fc[f][0], fc[f][1], ry), bx = x + rx(fc[f][2], fc[f][3], ry), bz = z + rz(fc[f][2], fc[f][3], ry);
            int cols = f % 2 == 0 ? cl : cd;
            float yb = y + 0.8f;
            // цоколь
            quad(wall, ax, y - 2, az, bx, y - 2, bz, bx, yb, bz, ax, yb, az, c("8a8780"), 0, 0, 1, 1);
            // текстура 8×8 окон: 1 окно = 1/8 uv
            quad(fac, ax, yb, az, bx, yb, bz, bx, y + H, bz, ax, y + H, az, wc, u0 / 8f, (v0 + floors) / 8f, (u0 + cols) / 8f, v0 / 8f);
            float nx = MathUtils.sin(ry + f * MathUtils.PI / 2) * 0.1f, nz = MathUtils.cos(ry + f * MathUtils.PI / 2) * 0.1f;
            quad(lit, ax + nx, yb, az + nz, bx + nx, yb, bz + nz, bx + nx, y + H, bz + nz, ax + nx, y + H, az + nz, Color.WHITE, u0 / 8f, (v0 + floors) / 8f, (u0 + cols) / 8f, v0 / 8f);
        }
        // крыша, парапет, машинные будки, антенны
        wall.box(L + 0.4f, 0.6f, D + 0.4f, c("6a6a66"), x, y + H + 0.3f, z, 0, ry, 0);
        int ent = Math.max(1, Math.round(L / 24f));
        for (int i = 0; i < ent; i++) {
            float lx = -hx + (i + 0.5f) * L / ent;
            wall.box(3.2f, 2.6f, 3.4f, c("8f8c84"), x + rx(lx, 0, ry), y + H + 1.6f, z + rz(lx, 0, ry), 0, ry, 0);
            // подъезд с козырьком
            wall.box(3.4f, 0.25f, 1.8f, c("7a7770"), x + rx(lx, hz + 0.9f, ry), y + 3.1f, z + rz(lx, hz + 0.9f, ry), 0, ry, 0);
            wall.box(1.6f, 2.2f, 0.2f, c("4a3a2e"), x + rx(lx, hz + 0.05f, ry), y + 1.9f, z + rz(lx, hz + 0.05f, ry), 0, ry, 0);
            if (night) glow.box(0.6f, 0.25f, 0.25f, c("fff0c8"), x + rx(lx, hz + 0.3f, ry), y + 2.85f, z + rz(lx, hz + 0.3f, ry), 0, ry, 0);
            if (R.nextFloat() < 0.6f) metal.seg(new Vector3(x + rx(lx + 1, -2, ry), y + H + 0.6f, z + rz(lx + 1, -2, ry)), new Vector3(x + rx(lx + 1, -2, ry), y + H + 5, z + rz(lx + 1, -2, ry)), 0.05f, 0.05f, 3, c("555555"));
        }
    }

    /** Самолёт на стоянке (транспортник с высоким крылом). */
    void plane(float x, float z, float ry, float s) {
        float y = gy(x, z);
        Color b = c("8f969c"), d = c("5f666c");
        // фюзеляж вдоль локальной Z
        Vector3 nose = new Vector3(x + rx(0, -15 * s, ry), y + 3.2f * s, z + rz(0, -15 * s, ry)), tail = new Vector3(x + rx(0, 14 * s, ry), y + 4.4f * s, z + rz(0, 14 * s, ry));
        Vector3 mid0 = new Vector3(x + rx(0, -10 * s, ry), y + 3.2f * s, z + rz(0, -10 * s, ry)), mid1 = new Vector3(x + rx(0, 8 * s, ry), y + 3.4f * s, z + rz(0, 8 * s, ry));
        metal.seg(mid0, mid1, 2.1f * s, 2.1f * s, 10, b);
        metal.seg(nose, mid0, 0.4f * s, 2.1f * s, 10, b);
        metal.seg(mid1, tail, 2.1f * s, 0.5f * s, 10, b);
        glass.box(1.6f * s, 0.6f * s, 0.8f * s, c("1a2633"), x + rx(0, -13.2f * s, ry), y + 4.1f * s, z + rz(0, -13.2f * s, ry), 0.5f, ry, 0);
        // крыло, двигатели, хвост
        metal.box(40 * s, 0.5f * s, 4.2f * s, b, x + rx(0, -2 * s, ry), y + 5.3f * s, z + rz(0, -2 * s, ry), 0, ry, 0);
        for (float lx : new float[]{-12, -6.5f, 6.5f, 12}) {
            metal.cyl(0.9f * s, 0.9f * s, 4.5f * s, 8, d, x + rx(lx * s, -3.5f * s, ry), y + 4.4f * s, z + rz(lx * s, -3.5f * s, ry), MathUtils.PI / 2, ry, 0);
            metal.box(0.2f * s, 2.8f * s, 0.2f * s, c("2a2a2a"), x + rx(lx * s, -6f * s, ry), y + 4.4f * s, z + rz(lx * s, -6f * s, ry), 0, ry, 0);
        }
        metal.box(0.5f * s, 6.5f * s, 4f * s, b, x + rx(0, 13 * s, ry), y + 7.5f * s, z + rz(0, 13 * s, ry), 0, ry, 0);
        metal.box(13 * s, 0.4f * s, 2.8f * s, b, x + rx(0, 14.2f * s, ry), y + 10.4f * s, z + rz(0, 14.2f * s, ry), 0, ry, 0);
        for (float lz : new float[]{-11, 0, 2}) for (int k = (lz == -11 ? 0 : -1); k <= (lz == -11 ? 0 : 1); k += 2) {
            float lx = k * 2.2f * s;
            metal.cyl(0.6f * s, 0.6f * s, 0.5f * s, 8, c("1c1c1c"), x + rx(lx, lz * s, ry), y + 0.6f * s, z + rz(lx, lz * s, ry), 0, ry, MathUtils.PI / 2);
            if (lz != -11 || k == 0) metal.box(0.25f, 1.6f * s, 0.25f, c("444444"), x + rx(lx, lz * s, ry), y + 1.5f * s, z + rz(lx, lz * s, ry), 0, ry, 0);
            if (lz == -11) break;
        }
    }

    // =================================================================
    // карты
    // =================================================================

    /** Глава 2 «Большая вода»: водохранилище, плотина ГЭС, река ниже плотины, ЛЭП от ГЭС. */
    void river() {
        Color wc = night ? c("0b1a2a") : c("27526e");
        water(-20, -1040, 260, -246, LAKE_Y, 16, wc);
        // река: клетки вдоль русла
        for (float x = 60; x < 950; x += 12) for (float z = -232; z < 260; z += 12) {
            if (riverDist(x + 6, z + 6) > 38) continue;
            float a = gy(x, z), b = gy(x + 12, z), c2 = gy(x + 12, z + 12), d = gy(x, z + 12);
            if (Math.min(Math.min(a, b), Math.min(c2, d)) >= RIVER_Y - 0.05f) continue;
            quad(wat, x, RIVER_Y, z + 12, x + 12, RIVER_Y, z + 12, x + 12, RIVER_Y, z, x, RIVER_Y, z, wc, 0, 0, 1, 1);
        }
        if (!w.dam) { // в миссии 2-1 плотину ставит сама миссия (объект)
            Node3 dam = Targets.dam();
            dam.pos.set(DAM_X, 0, DAM_Z);
            w.content.add(dam);
        }
        // пена у водосброса
        for (int i = 0; i < 18; i++) wall.sph(rr(2, 4), c("e8eef2"), DAM_X + rr(-12, 12), RIVER_Y + 0.3f, DAM_Z + rr(14, 34), 1, 0.25f, 1);
        // ЛЭП от машзала вправо за горизонт
        powerLine(new float[][]{{DAM_X + 70, -205}, {DAM_X + 200, -230}, {DAM_X + 340, -260}, {DAM_X + 480, -290}, {DAM_X + 620, -320}, {DAM_X + 760, -350}}, 30);
        // мост через реку и лодочная станция
        float bx = 300, bz = 60;
        float ry = MathUtils.atan2(372 - 246, 86 - 6) + MathUtils.PI / 2;
        float by = 4f;
        wall.box(110, 1.2f, 9, c("8f8b82"), bx, by, bz, 0, ry, 0);
        for (int s = -1; s <= 1; s += 2) metal.box(110, 0.9f, 0.2f, c("6f7378"), bx + rx(0, s * 4.4f, ry), by + 1.05f, bz + rz(0, s * 4.4f, ry), 0, ry, 0);
        for (float lx : new float[]{-45, -22, 0, 22, 45}) wall.box(2, by + 4, 6, c("7f7b72"), bx + rx(lx, 0, ry), by / 2 - 2, bz + rz(lx, 0, ry), 0, ry, 0);
        for (float lx = -50; lx <= 50; lx += 10) for (int s = -1; s <= 1; s += 2) metal.box(0.15f, 1f, 0.15f, c("6f7378"), bx + rx(lx, s * 4.4f, ry), by + 1.1f, bz + rz(lx, s * 4.4f, ry));
        // посёлок энергетиков слева от дороги
        Color[] walls = {c("c9b48a"), c("b9a48a"), c("d8cfc0"), c("a89a8a")};
        for (int i = 0; i < 16; i++) {
            float hx = rr(-260, -110), hz = rr(-300, -140);
            if (Math.abs(hx - World.ROAD_X) < 20) continue;
            house(hx, hz, Math.round(rr(0, 3)) * MathUtils.PI / 2, rr(7, 11), rr(7, 9), rr(4, 6), walls[R.nextInt(4)], c("7a3b2e"));
        }
    }

    /** Глава 3 «Ангары „Гарпии“»: логистический хаб — ангары, контейнеры, площадки для дронов. */
    void hub() {
        float y0 = 3f;
        // бетонные плиты двора
        for (float x = 40; x < 430; x += 25) for (float z = -440; z < -90; z += 25)
            wall.box(24.6f, 0.4f, 24.6f, vary(c("8a8880"), 0.03f), x + 12.5f, y0 + 0.1f, z + 12.5f);
        // разметка
        for (float x = 60; x < 420; x += 12) wall.box(6, 0.42f, 0.4f, c("d8c040"), x, y0 + 0.12f, -262);
        // ряд арочных ангаров в глубине
        for (int i = 0; i < 4; i++) arch(95 + i * 95, -385, 0, 20, 70, vary(c("9aa0a4"), 0.04f), c("8c9298"), i == 1);
        // склад-коробка с воротами к игроку
        wall.box(100, 16, 50, c("b8b4aa"), 360, y0 + 8, -170);
        wall.box(102, 1, 52, c("5a5c5e"), 360, y0 + 16.5f, -170);
        for (int i = 0; i < 5; i++) metal.box(12, 9, 0.4f, c("4e6a8a"), 320 + i * 20, y0 + 4.5f, -144.7f);
        for (int i = 0; i < 5; i++) metal.box(13, 0.4f, 3, c("6a6c6e"), 320 + i * 20, y0 + 9.5f, -143.5f);
        // фирменная стела «Гарпии» — оранжевая (без надписей)
        metal.box(2, 28, 2, c("555a5e"), 55, y0 + 14, -100);
        metal.box(9, 6, 1, c("e0742a"), 55, y0 + 25, -100);
        metal.box(9.2f, 0.6f, 1.2f, c("2a2c2e"), 55, y0 + 22, -100);
        if (night) glow.box(8.6f, 5.4f, 1.1f, c("ff8a3a"), 55, y0 + 25, -100);
        // контейнерные штабеля
        stack(60, -230, 0, 6, 3, 3);
        stack(90, -235, 0, 5, 3, 4);
        stack(130, -200, MathUtils.PI / 2, 4, 2, 2);
        // площадки дронов 3×3: круг + «Н»
        for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) {
            float px = 190 + i * 34, pz = -120 - j * 34;
            wall.cyl(13, 13, 0.3f, 20, c("3a3c40"), px, y0 + 0.4f, pz);
            wall.cyl(12.2f, 12.2f, 0.32f, 20, c("e07a2a"), px, y0 + 0.42f, pz);
            wall.cyl(11, 11, 0.34f, 20, c("3a3c40"), px, y0 + 0.44f, pz);
            wall.box(1.4f, 0.36f, 9, c("e8e6e0"), px - 3, y0 + 0.47f, pz);
            wall.box(1.4f, 0.36f, 9, c("e8e6e0"), px + 3, y0 + 0.47f, pz);
            wall.box(6, 0.36f, 1.4f, c("e8e6e0"), px, y0 + 0.47f, pz);
            if (night) for (int k = 0; k < 8; k++) glow.box(0.5f, 0.5f, 0.5f, c("ffb060"), px + MathUtils.sin(k * 0.785f) * 13.5f, y0 + 0.6f, pz + MathUtils.cos(k * 0.785f) * 13.5f);
        }
        // грузовые дроны «Гарпии» на двух площадках
        for (float[] d : new float[][]{{190, -120}, {258, -154}}) cargoDrone(d[0], y0 + 0.5f, d[1]);
        // диспетчерская с антеннами
        wall.box(18, 7, 12, c("d0ccc0"), 250, y0 + 3.5f, -300);
        glass.box(18.2f, 2, 12.2f, c("1a2633"), 250, y0 + 5, -300);
        metal.cyl(0.3f, 0.3f, 18, 6, c("777777"), 256, y0 + 16, -304);
        metal.sph(2.2f, c("e6e4dc"), 244, y0 + 9.5f, -302);
        // забор по периметру
        float[][] fence = {{40, -90}, {430, -90}, {430, -440}, {40, -440}, {40, -90}};
        for (int i = 0; i + 1 < fence.length; i++) {
            float ax = fence[i][0], az = fence[i][1], bx = fence[i + 1][0], bz = fence[i + 1][1], L = (float) Math.hypot(bx - ax, bz - az);
            float ry = MathUtils.atan2(bx - ax, bz - az);
            for (float t = 0; t < L; t += 6) {
                float px = ax + (bx - ax) * t / L, pz = az + (bz - az) * t / L;
                metal.box(0.15f, 2.6f, 0.15f, c("5a5e62"), px, gy(px, pz) + 1.3f, pz);
            }
            float mx = (ax + bx) / 2, mz = (az + bz) / 2;
            metal.box(0.05f, 2.2f, L, c("7d8286"), mx, y0 + 1.3f, mz, 0, ry, 0);
        }
        for (float x = 70; x < 420; x += 80) { lamp(x, -270, 16); lamp(x + 40, -330, 16); }
        // фуры у склада
        for (int i = 0; i < 4; i++) truck(320 + i * 22, -122, MathUtils.PI, BOX[R.nextInt(BOX.length)], c("d8d6d0"));
    }

    /** Большой грузовой дрон-октокоптер «Гарпии» (стоит на площадке). */
    void cargoDrone(float x, float y, float z) {
        Color body = c("e0742a"), dk = c("2a2c2e");
        metal.box(3.2f, 1.4f, 3.2f, body, x, y + 2.2f, z);
        metal.box(2.6f, 1.6f, 2.6f, dk, x, y + 0.9f, z);
        for (int k = 0; k < 8; k++) {
            float a = k * MathUtils.PI / 4, ex = x + MathUtils.sin(a) * 5, ez = z + MathUtils.cos(a) * 5;
            metal.seg(new Vector3(x, y + 2.4f, z), new Vector3(ex, y + 2.6f, ez), 0.14f, 0.14f, 4, dk);
            metal.cyl(0.35f, 0.35f, 0.6f, 6, dk, ex, y + 2.9f, ez);
            metal.cyl(1.8f, 1.8f, 0.05f, 12, c("3a3c40"), ex, y + 3.25f, ez);
        }
        for (int s = -1; s <= 1; s += 2) metal.box(0.15f, 0.15f, 4, dk, x + s * 1.4f, y + 0.1f, z);
    }

    /** Глава 4 «Чужой почерк»: аэродром — полоса, рулёжка, перрон, ангары, вышка, самолёты. */
    void airfield() {
        float y0 = 2f, zr = -300;
        Color asph = c("3c3d3f"), mark = c("e6e4dc");
        wall.box(2200, 0.4f, 60, asph, 0, y0 + 0.05f, zr);
        wall.box(2200, 0.3f, 90, c("6a6a5a"), 0, y0 - 0.02f, zr);
        for (float x = -1050; x < 1050; x += 60) wall.box(30, 0.42f, 1, mark, x, y0 + 0.08f, zr);
        for (int s = -1; s <= 1; s += 2) {
            wall.box(2200, 0.42f, 0.8f, mark, 0, y0 + 0.08f, zr + s * 28.5f);
            for (int k = 0; k < 8; k++) wall.box(40, 0.42f, 2, mark, s * 1050, y0 + 0.08f, zr - 21 + k * 6);
            wall.box(3, 0.42f, 56, mark, s * 1010, y0 + 0.08f, zr);
        }
        // огни полосы
        for (float x = -1080; x <= 1080; x += 60) for (int s = -1; s <= 1; s += 2) {
            metal.box(0.5f, 0.6f, 0.5f, c("cccccc"), x, y0 + 0.3f, zr + s * 31);
            if (night) glow.box(0.6f, 0.5f, 0.6f, s > 0 ? c("ffffff") : c("ffe7a0"), x, y0 + 0.65f, zr + s * 31);
        }
        // рулёжка и перрон
        wall.box(700, 0.4f, 22, c("47484a"), 120, y0 + 0.04f, -228);
        for (float x : new float[]{-200, 160, 440}) wall.box(22, 0.4f, 50, c("47484a"), x, y0 + 0.04f, -250);
        for (float x = -220; x < 470; x += 8) wall.box(4, 0.42f, 0.4f, c("e0c040"), x, y0 + 0.07f, -228);
        for (float x = 120; x < 480; x += 30) for (float z = -215; z < -95; z += 30) wall.box(29.6f, 0.45f, 29.6f, vary(c("8a8880"), 0.025f), x + 15, y0 + 0.05f, z + 15);
        // ангары и самолёты
        for (int i = 0; i < 3; i++) arch(200 + i * 110, -68, MathUtils.PI, 24, 50, vary(c("7f8a8c"), 0.03f), c("6f787a"), i == 1);
        plane(250, -160, MathUtils.PI / 2, 1f);
        plane(390, -150, MathUtils.PI / 2 + 0.2f, 0.9f);
        plane(-420, -300, MathUtils.PI / 2, 0.85f);
        // командно-диспетчерский пункт
        float tx = 75, tz = -150, ty = gy(tx, tz);
        wall.box(14, 8, 10, c("d4d0c6"), tx + 12, ty + 4, tz);
        wall.box(6, 26, 6, c("d4d0c6"), tx, ty + 13, tz);
        wall.cyl(5.5f, 4.2f, 1, 8, c("8a8a86"), tx, ty + 26.5f, tz);
        glass.cyl(5f, 5f, 3.6f, 8, c("1a2633"), tx, ty + 28.8f, tz);
        wall.cyl(5.8f, 5.8f, 0.8f, 8, c("8a8a86"), tx, ty + 31, tz);
        metal.cyl(0.15f, 0.15f, 8, 4, c("777777"), tx + 2, ty + 35, tz);
        if (night) { glow.box(0.6f, 0.6f, 0.6f, c("ff3020"), tx + 2, ty + 39, tz); glow.cyl(4.95f, 4.95f, 3f, 8, c("3a5a6a"), tx, ty + 28.8f, tz); }
        // обзорная РЛС под куполом
        float rx0 = -160, rz0 = -190, ry0 = gy(rx0, rz0);
        lattice(rx0, rz0, 16, 6, 4, 0, c("8a8e90"), 0.25f);
        metal.sph(5.5f, c("e8e6e0"), rx0, ry0 + 21, rz0);
        // ветроуказатель
        metal.cyl(0.1f, 0.12f, 8, 4, c("777777"), 520, gy(520, -240) + 4, -240);
        metal.cyl(0.35f, 0.7f, 3.4f, 8, c("e0602a"), 521.6f, gy(520, -240) + 7.6f, -240, 0, 0, MathUtils.PI / 2 - 0.3f);
        // забор вдоль поля
        for (float x = -1100; x < 1100; x += 8) metal.box(0.12f, 2.2f, 0.12f, c("5a5e62"), x, gy(x, -405) + 1.1f, -405);
        metal.box(2200, 1.9f, 0.05f, c("7d8286"), 0, y0 + 1.1f, -405);
        for (float x = 140; x < 480; x += 70) lamp(x, -100, 20);
    }

    /** Глава 5 «Поводок»: поля, лесополосы, ЛЭП, элеватор. */
    void fieldsMap() {
        Color[] fc = {c("d8b850"), c("5f8a32"), c("5a4030"), c("8aa83a"), c("e0c048")};
        if (w.themeName.equals("night")) for (int i = 0; i < fc.length; i++) fc[i] = new Color(fc[i]).mul(0.7f, 0.7f, 0.75f, 1);
        float cell = 14f;
        for (float[] f : fields) {
            int type = (int) f[4];
            boolean alongX = R.nextBoolean();
            Color base = vary(fc[type], 0.03f);
            for (float x = f[0]; x < f[2] - 0.1f; x += cell) for (float z = f[1]; z < f[3] - 0.1f; z += cell) {
                float x1 = Math.min(x + cell, f[2]), z1 = Math.min(z + cell, f[3]);
                int row = (int) ((alongX ? z : x) / cell);
                Color cc = row % 2 == 0 ? base : new Color(base).mul(0.9f, 0.9f, 0.9f, 1);
                float u = alongX ? x / 6f : z / 6f;
                soil.v.addAll(x, gy(x, z1) + 0.35f, z1, 0, 1, 0, cc.r, cc.g, cc.b, 1, u, 0);
                soil.v.addAll(x1, gy(x1, z) + 0.35f, z, 0, 1, 0, cc.r, cc.g, cc.b, 1, u + 1, 1);
                soil.v.addAll(x1, gy(x1, z1) + 0.35f, z1, 0, 1, 0, cc.r, cc.g, cc.b, 1, u + 1, 0);
                soil.v.addAll(x, gy(x, z1) + 0.35f, z1, 0, 1, 0, cc.r, cc.g, cc.b, 1, u, 0);
                soil.v.addAll(x, gy(x, z) + 0.35f, z, 0, 1, 0, cc.r, cc.g, cc.b, 1, u, 1);
                soil.v.addAll(x1, gy(x1, z) + 0.35f, z, 0, 1, 0, cc.r, cc.g, cc.b, 1, u + 1, 1);
            }
            // рулоны сена на жёлтых полях
            if (type == 0 && R.nextFloat() < 0.7f) for (int k = 0; k < 10; k++) {
                float hx = rr(f[0] + 5, f[2] - 5), hz = rr(f[1] + 5, f[3] - 5);
                wall.cyl(0.9f, 0.9f, 1.4f, 10, c("c9a85a"), hx, gy(hx, hz) + 1.2f, hz, MathUtils.PI / 2, rr(0, 3), 0);
            }
        }
        // элеватор: ряд бетонных силосов и башня
        float ex = 230, ez = -320, ey = gy(ex, ez);
        for (int i = 0; i < 6; i++) for (int j = 0; j < 2; j++)
            wall.cyl(4.2f, 4.2f, 34, 14, c("d6d2c8"), ex - 25 + i * 8.6f, ey + 17, ez + j * 8.6f);
        wall.box(52, 3, 18, c("c4c0b4"), ex - 3.5f, ey + 35.5f, ez + 4.3f);
        wall.box(10, 50, 10, c("cfcabe"), ex + 30, ey + 25, ez + 4);
        wall.box(11, 6, 11, c("8a8a86"), ex + 30, ey + 53, ez + 4);
        metal.box(30, 2, 2.2f, c("8a8e90"), ex + 15, ey + 45, ez + 4, 0, 0, 0.12f);
        // фермы-коровники
        for (int i = 0; i < 3; i++) {
            float fx = 170 + i * 30, fz = -260;
            wall.box(14, 5, 60, c("b8ac98"), fx, gy(fx, fz) + 2.5f, fz);
            wall.extrude(new float[][]{{-7.5f, 0}, {7.5f, 0}, {0, 4}}, 61, c("6e6a66"), fx, gy(fx, fz) + 5, fz, 0, MathUtils.PI / 2, 0);
        }
        // ЛЭП через поля
        powerLine(new float[][]{{-900, -120}, {-650, -170}, {-400, -220}, {-150, -270}, {80, -400}, {330, -470}, {580, -540}, {850, -610}}, 34);
        powerLine(new float[][]{{-700, -900}, {-500, -760}, {-300, -620}, {-120, -480}, {40, -330}, {160, -170}}, 26);
    }

    /** Глава 6 «Соль и сталь»: порт — причальная стенка, пирсы, портальные краны, контейнеры, сухогруз. */
    void port() {
        float y0 = 2.4f;
        // причальная стенка вдоль моря
        wall.box(16, 12, 740, c("8a877e"), -118, y0 - 6, -460);
        wall.box(16.2f, 0.6f, 740, c("6a6862"), -118, y0 + 0.2f, -460);
        for (float z = -820; z < -100; z += 12) metal.cyl(0.4f, 0.5f, 0.8f, 6, c("2a2a2a"), -125, y0 + 0.6f, z);
        for (float z = -820; z < -100; z += 18) metal.cyl(0.9f, 0.9f, 1.4f, 10, c("1c1c1c"), -126.5f, y0 - 0.6f, z, MathUtils.PI / 2, 0, 0);
        // пирсы в море
        for (float pz : new float[]{-180, -300, -520}) {
            wall.box(150, 1.6f, 18, c("8a877e"), -200, y0 - 0.6f, pz);
            for (float x = -270; x < -125; x += 12) for (int s = -1; s <= 1; s += 2) wall.cyl(0.6f, 0.6f, 12, 6, c("5a5650"), x, y0 - 7, pz + s * 7);
            for (float x = -270; x < -125; x += 25) lamp(x, pz + 8, 12);
        }
        // портальные краны на рельсах
        for (int i = 0; i < 4; i++) {
            float cx = -112, cz = -130 - i * 110, cy = y0;
            Color cc = i % 2 == 0 ? c("c84a2a") : c("d8a830");
            for (int s = -1; s <= 1; s += 2) for (int t = -1; t <= 1; t += 2)
                metal.seg(new Vector3(cx + s * 5, cy, cz + t * 5), new Vector3(cx + s * 3, cy + 22, cz + t * 3), 0.6f, 0.5f, 6, cc);
            metal.box(10, 3, 10, cc, cx, cy + 23.5f, cz);
            metal.box(7, 5, 6, cc, cx + 2, cy + 27.5f, cz);
            glass.box(2.5f, 2, 6.2f, c("1a2633"), cx - 2, cy + 26.5f, cz);
            float ang = rr(-0.6f, 0.6f);
            Vector3 a = new Vector3(cx - 2, cy + 27, cz), b = new Vector3(cx - 2 - MathUtils.cos(ang) * 40, cy + 40, cz + MathUtils.sin(ang) * 40);
            metal.seg(a, b, 0.9f, 0.5f, 4, cc);
            metal.seg(new Vector3(cx + 2, cy + 33, cz), b, 0.12f, 0.12f, 3, c("333333"));
            metal.seg(new Vector3(b.x, b.y, b.z), new Vector3(b.x, cy + 8, b.z), 0.08f, 0.08f, 3, c("333333"));
            metal.box(1.4f, 1, 1.4f, c("d8a830"), b.x, cy + 7.5f, b.z);
            if (night) glow.box(0.6f, 0.6f, 0.6f, c("ff3020"), b.x, b.y + 0.6f, b.z);
        }
        // контейнерный терминал
        stack(-95, -200, 0, 5, 4, 3);
        stack(-92, -280, 0, 4, 5, 4);
        stack(-95, -420, 0, 5, 4, 3);
        // сухогруз у пирса
        float sx = -205, sz = -335, sy = -3f;
        float[][] hull = {{-8, 0}, {8, 0}, {11, 9}, {-11, 9}};
        metal.extrude(hull, 130, c("2a3a4e"), sx, sy - 2, sz, 0, 0, 0);
        metal.extrude(new float[][]{{-11, 7.2f}, {11, 7.2f}, {11.05f, 9.05f}, {-11.05f, 9.05f}}, 130.2f, c("a83224"), sx, sy - 2, sz, 0, 0, 0);
        metal.cyl(0.1f, 11, 18, 4, c("2a3a4e"), sx - 65, sy + 2.5f, sz, 0, MathUtils.PI / 4, MathUtils.PI / 2);
        wall.box(14, 12, 20, c("e8e6e0"), sx + 52, sy + 13, sz);
        glass.box(14.2f, 1.6f, 20.2f, c("1a2633"), sx + 52, sy + 17, sz);
        metal.cyl(1.8f, 1.8f, 8, 10, c("2a2a2a"), sx + 56, sy + 22, sz);
        for (int i = 0; i < 7; i++) for (int k = 0; k < 2 + R.nextInt(2); k++)
            for (int j = -3; j <= 3; j++) container(sx - 42 + i * 13, sy + 7 + k * 2.62f, sz + j * 2.5f, MathUtils.PI / 2, true, BOX[R.nextInt(BOX.length)]);
        // склады порта за стенкой
        for (int i = 0; i < 3; i++) arch(-82, -540 - i * 60, MathUtils.PI / 2, 14, 46, vary(c("a6a8aa"), 0.03f), c("8c9298"), false);
        // маяк уже есть в мире (берег моря)
    }

    /** Глава 7 «Белое безмолвие»: тайга, замёрзшее озеро с рыбацкими будками, избы, ретранслятор. */
    void taiga() {
        // лёд
        int N = 48;
        Color ic = night ? c("2a4058") : c("5f8fb0"), ic2 = night ? c("24384e") : c("7aa6c4");
        for (int i = 0; i < N; i++) {
            float a0 = MathUtils.PI2 * i / N, a1 = MathUtils.PI2 * (i + 1) / N;
            for (int ring = 0; ring < 4; ring++) {
                float r0 = Math.max(0.002f, 1.08f * ring / 4), r1 = 1.08f * (ring + 1) / 4;
                Color cc = (i + ring) % 3 == 0 ? ic2 : ic;
                float y = -0.7f;
                quad(ice, ICE_X + MathUtils.sin(a0) * ICE_RX * r0, y, ICE_Z + MathUtils.cos(a0) * ICE_RZ * r0,
                        ICE_X + MathUtils.sin(a0) * ICE_RX * r1, y, ICE_Z + MathUtils.cos(a0) * ICE_RZ * r1,
                        ICE_X + MathUtils.sin(a1) * ICE_RX * r1, y, ICE_Z + MathUtils.cos(a1) * ICE_RZ * r1,
                        ICE_X + MathUtils.sin(a1) * ICE_RX * r0, y, ICE_Z + MathUtils.cos(a1) * ICE_RZ * r0, cc,
                        MathUtils.sin(a0) * r0 * 30, MathUtils.cos(a0) * r0 * 20, MathUtils.sin(a1) * r1 * 30, MathUtils.cos(a1) * r1 * 20);
            }
        }
        // проруби и будки рыбаков
        Color[] hut = {c("b0302a"), c("2f5f9a"), c("3f7a3a"), c("d8a830"), c("6a4a32")};
        for (int i = 0; i < 10; i++) {
            float a = rr(0, MathUtils.PI2), r = rr(0.15f, 0.75f), x = ICE_X + MathUtils.sin(a) * ICE_RX * r, z = ICE_Z + MathUtils.cos(a) * ICE_RZ * r;
            float ry = rr(0, 3);
            wall.box(3, 2.6f, 2.6f, hut[R.nextInt(hut.length)], x, 0.6f, z, 0, ry, 0);
            wall.extrude(new float[][]{{-1.6f, 0}, {1.6f, 0}, {0, 0.9f}}, 3.3f, c("e8eef4"), x, 1.9f, z, 0, ry, 0);
            wall.disc(1.1f, c("12202e"), x + 3.5f, -0.66f, z + 1);
            // рыбак
            wall.capsule(0.35f, 0.9f, c("2a2a2a"), x + 3.5f, 0.3f, z - 0.4f, 0, 0, 0);
        }
        // торосы и снежные надувы по краю озера
        for (int i = 0; i < 26; i++) {
            float a = rr(0, MathUtils.PI2), r = rr(0.92f, 1.05f);
            wall.sph(rr(2, 5), c("f2f6fa"), ICE_X + MathUtils.sin(a) * ICE_RX * r, -0.6f, ICE_Z + MathUtils.cos(a) * ICE_RZ * r, rr(1, 2.5f), 0.35f, 1);
        }
        // избы с заснеженными крышами
        for (int i = 0; i < 14; i++) {
            float hx = rr(-245, -110), hz = rr(-285, -130);
            if (Math.abs(hx - World.ROAD_X) < 16) continue;
            float ry = Math.round(rr(0, 3)) * MathUtils.PI / 2;
            house(hx, hz, ry, rr(7, 10), rr(6, 8), rr(3.5f, 4.5f), vary(c("6a4a32"), 0.04f), c("eef2f8"));
            float y = gy(hx, hz);
            wall.box(0.8f, 2.4f, 0.8f, c("7a6a5a"), hx + rx(2, 1, ry), y + 6.2f, hz + rz(2, 1, ry));
            // поленница
            wall.box(3, 1.2f, 0.9f, c("8a6a44"), hx + rx(0, 5, ry), y + 0.6f, hz + rz(0, 5, ry), 0, ry, 0);
        }
        // ретранслятор на сопке (с растяжками)
        float mx = -300, mz = -520, my = gy(mx, mz);
        lattice(mx, mz, 70, 4, 2, 0.3f, c("c8ccd0"), 0.25f);
        for (int k = 0; k < 4; k++) for (float hh : new float[]{30, 60}) {
            float a = k * MathUtils.PI / 2 + 0.3f;
            metal.seg(new Vector3(mx, my + hh, mz), new Vector3(mx + MathUtils.sin(a) * hh * 0.8f, gy(mx + MathUtils.sin(a) * hh * 0.8f, mz + MathUtils.cos(a) * hh * 0.8f), mz + MathUtils.cos(a) * hh * 0.8f), 0.06f, 0.06f, 3, c("3a3a3a"));
        }
        for (int i = 0; i < 3; i++) metal.cyl(1.2f, 1.2f, 0.4f, 10, c("dddddd"), mx + 1.5f, my + 50 + i * 6, mz, MathUtils.PI / 2, 0, 0);
        if (night) for (float hh : new float[]{35, 70}) glow.box(0.8f, 0.8f, 0.8f, c("ff3020"), mx, my + hh + 0.5f, mz);
        // старая ЛЭП вдоль дороги на деревянных опорах
        Vector3[] prev = null;
        for (float z = -100; z > -1000; z -= 60) {
            float x = World.ROAD_X - 12, y = gy(x, z);
            wall.cyl(0.18f, 0.22f, 10, 5, c("5a4a3a"), x, y + 5, z);
            wall.box(3, 0.25f, 0.25f, c("5a4a3a"), x, y + 9.3f, z);
            Vector3[] cur = {new Vector3(x - 1.3f, y + 9.6f, z), new Vector3(x + 1.3f, y + 9.6f, z)};
            if (prev != null) for (int k = 0; k < 2; k++) wire(prev[k], cur[k], 1.5f, 0.04f);
            prev = cur;
        }
    }

    /** Глава 8 «Мираж»: нефтепромысел — станки-качалки, трубопровод, факел, глинобитный посёлок. */
    void oil() {
        // качалки: станина и стойка неподвижны, балансир с головой и противовесом качается (World.jacks)
        float S = 1.7f;
        float[][] js = {{-90, -190}, {70, -220}, {150, -320}, {-200, -380}, {300, -390}, {-30, -450}, {220, -520}, {-330, -540}, {380, -250}, {-420, -280}, {20, -110}};
        Color bc = c("3a3c3e"), hc = c("d8a830");
        MB bm = new MB();
        bm.box(0.7f * S, 0.7f * S, 8.2f * S, bc, 0, 0, -0.6f * S);
        bm.box(1.3f * S, 3.4f * S, 1.3f * S, hc, 0, -0.8f * S, -4.6f * S);
        bm.box(1.4f * S, 0.5f * S, 1.4f * S, hc, 0, 0.9f * S, -4.6f * S);
        bm.box(1.7f * S, 2.4f * S, 1.7f * S, c("8a2a20"), 0, -1.6f * S, 3.2f * S);
        bm.cyl(0.35f * S, 0.35f * S, 1.2f * S, 8, c("5a5a5a"), 0, 0, 0, 0, 0, MathUtils.PI / 2);
        Mesh3 beam = bm.mesh();
        Mat jm = Mat.vc(); jm.rough = 0.5f; jm.metal = 0.35f; jm.spec = 0.8f;
        for (float[] j : js) {
            float x = j[0], z = j[1], y = gy(x, z), ry = rr(0, 3);
            wall.box(3 * S, 0.6f, 9.5f * S, c("8a877e"), x, y + 0.3f, z, 0, ry, 0);
            for (int s = -1; s <= 1; s += 2) for (int t = -1; t <= 1; t += 2)
                metal.seg(new Vector3(x + rx(s * 1.2f * S, t * 1.2f * S, ry), y + 0.6f, z + rz(s * 1.2f * S, t * 1.2f * S, ry)),
                        new Vector3(x + rx(s * 0.3f * S, 0, ry), y + 6.6f * S, z + rz(s * 0.3f * S, 0, ry)), 0.12f * S, 0.12f * S, 4, bc);
            // редуктор с кривошипом и устье скважины
            metal.box(1.8f * S, 1.6f * S, 2 * S, c("6a6c6e"), x + rx(0, 3.4f * S, ry), y + 0.8f * S, z + rz(0, 3.4f * S, ry), 0, ry, 0);
            metal.cyl(0.25f * S, 0.25f * S, 1.6f * S, 6, c("5a5a5a"), x + rx(0, -4.6f * S, ry), y + 0.8f * S, z + rz(0, -4.6f * S, ry));
            metal.seg(new Vector3(x + rx(0, -4.6f * S, ry), y + 1.5f * S, z + rz(0, -4.6f * S, ry)), new Vector3(x + rx(0, -4.6f * S, ry), y + 4.4f * S, z + rz(0, -4.6f * S, ry)), 0.06f * S, 0.06f * S, 3, c("cfcfcf"));
            Node3 n = new Node3(beam, jm);
            n.castShadow = false;
            n.pos.set(x, y + 6.8f * S, z);
            n.tag = new float[]{ry, rr(0, 6.28f), rr(1.0f, 1.5f)};
            n.rot.idt().rotateRad(Vector3.Y, ry);
            w.content.add(n);
            w.jacks.add(n);
        }
        // трубопровод на опорах через всю карту
        float pz = -142;
        for (float x = -700; x < 700; x += 14) {
            float y = gy(x, pz);
            metal.box(0.3f, 1.6f, 1.6f, c("6a6c6e"), x, y + 0.8f, pz);
        }
        for (float x = -700; x < 700; x += 40) {
            float y0 = gy(x, pz) + 1.8f, y1 = gy(x + 40, pz) + 1.8f;
            metal.seg(new Vector3(x, y0, pz), new Vector3(x + 40, y1, pz), 0.6f, 0.6f, 8, c("b0a890"));
            metal.seg(new Vector3(x, y0 - 0.2f, pz + 2), new Vector3(x + 40, y1 - 0.2f, pz + 2), 0.35f, 0.35f, 6, c("8a8a86"));
        }
        // факел попутного газа
        float fx = 270, fz = -430, fy = gy(fx, fz);
        metal.cyl(0.6f, 0.9f, 30, 8, c("8a8e90"), fx, fy + 15, fz);
        for (int s = 0; s < 3; s++) metal.seg(new Vector3(fx, fy + 22, fz), new Vector3(fx + MathUtils.sin(s * 2.09f) * 12, gy(fx, fz), fz + MathUtils.cos(s * 2.09f) * 12), 0.05f, 0.05f, 3, c("3a3a3a"));
        Mat fm = Mat.unshadedAdd(c("ff9a3a")); fm.tex = Tex.soft(1.4f);
        MB fl = new MB();
        fl.sph(3.2f, Color.WHITE, fx, fy + 33.5f, fz, 0.8f, 1.8f, 0.8f);
        fl.sph(1.8f, Color.WHITE, fx, fy + 32, fz, 1, 1.4f, 1);
        w.add(fl.mesh(), fm);
        // глинобитный посёлок
        Color[] ad = {c("c8a070"), c("b89060"), c("d0aa78"), c("a88458")};
        for (int i = 0; i < 26; i++) {
            float hx = rr(-305, -150), hz = rr(-315, -165);
            float hw = rr(6, 11), hd = rr(6, 10), hh = rr(3.2f, 5), ry = Math.round(rr(0, 3)) * MathUtils.PI / 2, y = gy(hx, hz);
            Color cc = ad[R.nextInt(ad.length)];
            wall.box(hw, hh, hd, cc, hx, y + hh / 2 - 0.3f, hz, 0, ry, 0);
            wall.box(hw + 0.3f, 0.5f, hd + 0.3f, new Color(cc).mul(0.9f, 0.9f, 0.9f, 1), hx, y + hh, hz, 0, ry, 0);
            glass.box(1, 1.1f, 0.1f, c("2a1e14"), hx + rx(-hw / 4, hd / 2 + 0.03f, ry), y + hh * 0.55f, hz + rz(-hw / 4, hd / 2 + 0.03f, ry), 0, ry, 0);
            glass.box(1.2f, 2.1f, 0.1f, c("4a3424"), hx + rx(hw / 4, hd / 2 + 0.03f, ry), y + 1.05f, hz + rz(hw / 4, hd / 2 + 0.03f, ry), 0, ry, 0);
            if (R.nextFloat() < 0.4f) wall.cyl(1.2f, 1.2f, 0.4f, 8, c("e8e6e0"), hx, y + hh + 0.6f, hz);
            if (night && R.nextFloat() < 0.5f) glow.box(0.9f, 1f, 0.12f, c("ffb860"), hx + rx(-hw / 4, hd / 2 + 0.06f, ry), y + hh * 0.55f, hz + rz(-hw / 4, hd / 2 + 0.06f, ry), 0, ry, 0);
        }
        // минарет-водонапорка посёлка
        metal.cyl(1.6f, 2, 20, 10, c("c8a070"), -230, gy(-230, -240) + 10, -240);
        metal.cyl(3, 3, 3, 10, c("d0aa78"), -230, gy(-230, -240) + 21, -240);
    }

    /** Глава 9 «Кузница»: металлургический завод — домна, градирни, ж/д с вагонами, кран. */
    void forge() {
        // железная дорога вдоль фронта
        float rz0 = -210;
        wall.box(2300, 0.8f, 7, c("5a524a"), 0, gy(0, rz0) + 0.2f, rz0);
        for (int s = -1; s <= 1; s += 2) metal.box(2300, 0.25f, 0.18f, c("7a7672"), 0, gy(0, rz0) + 0.75f, rz0 + s * 0.76f);
        for (float x = -1100; x < 1100; x += 4f) wall.box(0.3f, 0.2f, 2.8f, c("3a302a"), x, gy(x, rz0) + 0.62f, rz0);
        // состав: хопперы и полувагоны
        float x = -180;
        for (int i = 0; i < 14; i++) {
            boolean hop = i < 7;
            Color cc = hop ? c("6a4a32") : c("3a3c40");
            float L = 14;
            float y = gy(x, rz0) + 0.8f;
            if (hop) {
                metal.box(3.1f, 3.4f, L - 1.2f, cc, x, y + 2.9f, rz0, 0, MathUtils.PI / 2, 0);
                metal.cyl(0, 1.6f, 1.4f, 4, cc, x - 3, y + 0.9f, rz0, MathUtils.PI, MathUtils.PI / 4, 0);
                metal.cyl(0, 1.6f, 1.4f, 4, cc, x + 3, y + 0.9f, rz0, MathUtils.PI, MathUtils.PI / 4, 0);
            } else {
                metal.box(3.1f, 2.2f, L - 1.2f, cc, x, y + 2.3f, rz0, 0, MathUtils.PI / 2, 0);
                wall.box(2.8f, 0.6f, L - 1.6f, c("1a1a1a"), x, y + 3.2f, rz0, 0, MathUtils.PI / 2, 0);
            }
            for (float lx : new float[]{-4.5f, 4.5f}) for (int s = -1; s <= 1; s += 2)
                metal.cyl(0.45f, 0.45f, 0.25f, 8, c("2a2a2a"), x + lx, y + 0.45f, rz0 + s * 0.76f, MathUtils.PI / 2, 0, 0);
            x += L;
        }
        // тепловоз
        float lx0 = x + 2, ly = gy(lx0, rz0) + 0.8f;
        metal.box(3.2f, 3.6f, 17, c("2f6a3a"), lx0 + 8, ly + 2.8f, rz0, 0, MathUtils.PI / 2, 0);
        metal.box(3.25f, 0.6f, 17.1f, c("d8a830"), lx0 + 8, ly + 1.8f, rz0, 0, MathUtils.PI / 2, 0);
        glass.box(0.1f, 1.2f, 2.6f, c("1a2633"), lx0 + 16.55f, ly + 3.8f, rz0);
        // домна с воздухонагревателями
        float bx = 190, bz = -380, by = gy(bx, bz);
        wall.cyl(7, 10, 50, 14, c("4a3e36"), bx, by + 25, bz);
        wall.cyl(4, 7, 10, 14, c("3a302a"), bx, by + 55, bz);
        lattice(bx, bz, 62, 24, 14, 0, c("5a4e46"), 0.4f);
        for (int i = 0; i < 4; i++) {
            float sx = bx - 40 + i * 13, sz = bz + 28;
            wall.cyl(5, 5, 42, 14, c("6a5a50"), sx, by + 21, sz);
            wall.sph(5, c("6a5a50"), sx, by + 42, sz, 1, 0.5f, 1);
        }
        metal.seg(new Vector3(bx - 40, by + 38, bz + 28), new Vector3(bx, by + 40, bz), 1.6f, 1.6f, 8, c("6a5e56"));
        metal.seg(new Vector3(bx, by + 58, bz), new Vector3(bx + 30, by + 66, bz - 20), 1.4f, 1.4f, 8, c("5a4e46"));
        metal.seg(new Vector3(bx + 30, by + 66, bz - 20), new Vector3(bx + 30, by, bz - 20), 1.4f, 1.4f, 8, c("5a4e46"));
        // наклонная конвейерная галерея
        metal.seg(new Vector3(bx - 70, by + 2, bz - 40), new Vector3(bx - 5, by + 52, bz), 1.8f, 1.8f, 4, c("8a7a6a"));
        if (night) for (int i = 0; i < 6; i++) glow.box(2, 1.2f, 0.3f, c("ff7a2a"), bx - 8 + i * 3, by + 4, bz + 10.2f);
        // градирни (гиперболоид из колец)
        for (float[] t : new float[][]{{-330, -470}, {-240, -560}}) {
            float tx = t[0], tz = t[1], ty = gy(tx, tz);
            float[] rs = {38, 33, 28, 25, 24, 25, 27};
            for (int i = 0; i + 1 < rs.length; i++) wall.cyl(rs[i + 1], rs[i], 16, 24, vary(c("a8a49c"), 0.02f), tx, ty + 8 + i * 16, tz);
            for (int k = 0; k < 12; k++) wall.box(1.6f, 6, 1.6f, c("7a766e"), tx + MathUtils.sin(k * 0.52f) * 37, ty + 2, tz + MathUtils.cos(k * 0.52f) * 37);
            w.stacks.add(new Vector3(tx, ty + 104, tz));
        }
        // мостовой кран над складом
        float kx = 30, kz = -320, ky = gy(kx, kz);
        for (int s = -1; s <= 1; s += 2) for (int t = -1; t <= 1; t += 2) metal.box(1.4f, 22, 1.4f, c("d8a830"), kx + s * 30, ky + 11, kz + t * 14);
        for (int t = -1; t <= 1; t += 2) metal.box(62, 2, 1.6f, c("d8a830"), kx, ky + 22, kz + t * 14);
        metal.box(2.2f, 2.4f, 30, c("c08a20"), kx + 6, ky + 23.5f, kz);
        for (int i = 0; i < 6; i++) wall.box(6, rr(2, 5), 6, c("5a4a40"), kx - 22 + i * 8, ky + 1.5f, kz + rr(-6, 6));
    }

    /** Глава 10 «Химера»: окраина города — панельки, гаражи, фонари вдоль дороги. */
    void city() {
        for (float[] b : blocks) panel(b[0], b[1], b[2], b[3], b[4], (int) b[5]);
        // гаражный кооператив
        for (int row = 0; row < 2; row++) for (int i = 0; i < 22; i++) {
            float gx = 110 + i * 4.2f, gz = -112 - row * 14, y = gy(gx, gz);
            wall.box(4, 2.8f, 6.5f, vary(c("8a8478"), 0.03f), gx, y + 1.2f, gz);
            metal.box(2.8f, 2.1f, 0.1f, BOX[R.nextInt(BOX.length)].cpy().mul(0.8f, 0.8f, 0.8f, 1), gx, y + 1.05f, gz + (row == 0 ? 3.27f : -3.27f));
        }
        // фонари вдоль дороги
        for (float z = 40; z > -900; z -= 36) {
            float x = World.ROAD_X + 6, y = gy(x, z);
            metal.cyl(0.12f, 0.16f, 9, 6, c("5a5e62"), x, y + 4.5f, z);
            metal.box(2.2f, 0.12f, 0.12f, c("5a5e62"), x - 1.1f, y + 9, z);
            metal.box(0.8f, 0.25f, 0.4f, c("3a3a3a"), x - 2.1f, y + 8.9f, z);
            if (night) glow.box(0.7f, 0.12f, 0.35f, c("ffcf7a"), x - 2.1f, y + 8.75f, z);
        }
        // котельная с трубой
        float kx = -200, kz = -440, ky = gy(kx, kz);
        wall.box(26, 12, 18, c("9a5a46"), kx, ky + 6, kz);
        wall.cyl(2.4f, 3.6f, 60, 12, c("a8a49c"), kx + 18, ky + 30, kz);
        for (int i = 0; i < 4; i++) wall.cyl(2.5f + i * 0.2f, 2.5f + i * 0.2f, 3, 12, c("b02a20"), kx + 18, ky + 42 + i * 5, kz);
        w.stacks.add(new Vector3(kx + 18, ky + 60, kz));
        // вышка сотовой связи
        lattice(30, -190, 42, 4, 1.4f, 0.4f, c("9a9ea2"), 0.2f);
        for (int k = 0; k < 3; k++) metal.box(0.5f, 2.6f, 0.4f, c("e6e4dc"), 30 + MathUtils.sin(k * 2.09f) * 1.2f, gy(30, -190) + 40, -190 + MathUtils.cos(k * 2.09f) * 1.2f);
        if (night) glow.box(0.7f, 0.7f, 0.7f, c("ff3020"), 30, gy(30, -190) + 43, -190);
    }

    /** Глава 11 «Последний сигнал»: огромная телебашня, мачты с растяжками, заброшенный посёлок. */
    void tower() {
        float tx = 120, tz = -560, ty = gy(tx, tz);
        Color con = c("c8c4ba");
        // опоры-ноги
        for (int k = 0; k < 6; k++) {
            float a = k * MathUtils.PI / 3;
            wall.seg(new Vector3(tx + MathUtils.sin(a) * 26, ty - 2, tz + MathUtils.cos(a) * 26), new Vector3(tx + MathUtils.sin(a) * 6, ty + 60, tz + MathUtils.cos(a) * 6), 2.4f, 4f, 6, con);
        }
        // ствол
        wall.cyl(4.5f, 9, 120, 16, con, tx, ty + 60, tz);
        wall.cyl(3, 4.5f, 120, 16, con, tx, ty + 180, tz);
        // смотровая «тарелка»
        wall.cyl(14, 6, 8, 20, c("9a9890"), tx, ty + 238, tz);
        glass.cyl(14.2f, 14.2f, 6, 20, c("1a2633"), tx, ty + 245, tz);
        wall.cyl(8, 15, 5, 20, c("9a9890"), tx, ty + 250, tz);
        wall.cyl(2.6f, 3, 30, 12, con, tx, ty + 267, tz);
        // антенна
        metal.cyl(1.6f, 2.4f, 40, 8, c("d84a2a"), tx, ty + 302, tz);
        metal.cyl(0.6f, 1.6f, 40, 8, c("e8e6e0"), tx, ty + 342, tz);
        for (int i = 0; i < 5; i++) wall.cyl(i % 2 == 0 ? 3.2f : 3.1f, i % 2 == 0 ? 3.2f : 3.1f, 8, 12, i % 2 == 0 ? c("d84a2a") : c("e8e6e0"), tx, ty + 286 + i * 8, tz);
        if (night || w.rain > 0) for (float hh : new float[]{120, 200, 252, 362}) for (int k = 0; k < 4; k++)
            glow.box(1.4f, 1.4f, 1.4f, c("ff3020"), tx + MathUtils.sin(k * 1.57f) * (hh > 300 ? 1.5f : hh > 230 ? 15 : 7), ty + hh, tz + MathUtils.cos(k * 1.57f) * (hh > 300 ? 1.5f : hh > 230 ? 15 : 7));
        // технический корпус у подножия
        wall.box(60, 14, 30, c("b4b0a6"), tx - 60, ty + 7, tz + 20);
        glass.box(60.2f, 3, 30.2f, c("1a2633"), tx - 60, ty + 9, tz + 20);
        // мачты с растяжками
        for (float[] m : new float[][]{{-260, -450, 90}, {360, -380, 80}, {-480, -700, 110}}) {
            float mx = m[0], mz = m[1], hh = m[2], my = gy(mx, mz);
            lattice(mx, mz, hh, 3, 2, 0.2f, c("c84a2a"), 0.22f);
            for (int k = 0; k < 3; k++) for (float f : new float[]{0.4f, 0.75f, 1f}) {
                float a = k * 2.09f + 0.4f, d = hh * 0.7f;
                float gx = mx + MathUtils.sin(a) * d, gz = mz + MathUtils.cos(a) * d;
                metal.seg(new Vector3(mx, my + hh * f, mz), new Vector3(gx, gy(gx, gz) + 0.5f, gz), 0.07f, 0.07f, 3, c("3a3a3a"));
            }
            if (night || w.rain > 0) glow.box(1, 1, 1, c("ff3020"), mx, my + hh + 0.6f, mz);
        }
        // заброшенный посёлок: пятиэтажки и дома без света
        float[][] bl = {{-160, -240, 0, 60, 12, 5}, {-270, -260, 0.1f, 48, 12, 5}, {-200, -360, MathUtils.PI / 2, 60, 12, 5}, {-320, -380, 0, 36, 12, 4}};
        boolean wasNight = night;
        for (float[] b : bl) panel(b[0], b[1], b[2], b[3], b[4], (int) b[5]);
        if (wasNight) lit.v.clear();
    }

}
