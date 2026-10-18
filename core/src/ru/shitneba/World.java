package ru.shitneba;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.JsonValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import ru.shitneba.gfx.Dyn;
import ru.shitneba.gfx.MB;
import ru.shitneba.gfx.Mat;
import ru.shitneba.gfx.Mesh3;
import ru.shitneba.gfx.Node3;
import ru.shitneba.gfx.Renderer;
import ru.shitneba.gfx.Tex;

/** Мир: рельеф, небо, деревья, трава, горы, облака, погода — порт world.gd. */
public class World {
    public static final float ROAD_X = -42f;
    static final Map<String, float[]> ATM = new HashMap<>();
    static final Map<String, String[]> GROUND = new HashMap<>();

    static {
        ATM.put("dusk", new float[]{6, 2.6f, 0.006f, 13}); ATM.put("dawn", new float[]{4.5f, 2, 0.005f, 9}); ATM.put("snow", new float[]{2.2f, 1.2f, 0.004f, 22});
        ATM.put("desert", new float[]{9, 3.2f, 0.009f, 12}); ATM.put("coast", new float[]{5, 3, 0.005f, 5}); ATM.put("industry", new float[]{14, 4.2f, 0.012f, 4});
        ATM.put("night", new float[]{4, 2, 0.005f, 10}); ATM.put("autumn", new float[]{6, 2.6f, 0.006f, 12}); ATM.put("storm", new float[]{12, 3.8f, 0.011f, 18});
        // a, b, rock, mt, trees, grass
        GROUND.put("dusk", new String[]{"7d7a3c", "56622e", "8c6c46", "5d6584", "pine", "77803a"});
        GROUND.put("night", new String[]{"4b5a35", "34452a", "5a5048", "2f3a58", "pine", "4d5c34"});
        GROUND.put("dawn", new String[]{"86904a", "5f7034", "95785a", "6e5d82", "pine", "8a9a48"});
        GROUND.put("snow", new String[]{"eef3f8", "c9d4e2", "8a96a8", "93a6bd", "snowpine", ""});
        GROUND.put("desert", new String[]{"d9a86a", "bf8a52", "9a6a44", "a8683c", "cactus", "a08a50"});
        GROUND.put("coast", new String[]{"6f7a40", "4f5f30", "7d7468", "2b3d5e", "pine", "6f7a3a"});
        GROUND.put("industry", new String[]{"5a4a3c", "3d332b", "4a3e36", "3b2a2a", "dead", "5a5236"});
        GROUND.put("autumn", new String[]{"9a7a3a", "7a5a2a", "8c6c46", "6a4a3a", "autumn", "a88a42"});
        GROUND.put("storm", new String[]{"56603c", "3e472e", "5a5650", "3a4250", "pine", "5c6640"});
    }

    public final Node3 root = new Node3();
    Node3 content;
    final Array<Mesh3> meshes = new Array<>();
    final Renderer rend;
    public String themeName = "dusk";
    public boolean night, sea, dam, snow;
    int hf;
    public float rain, fog, wind;
    public boolean beamOn;
    float tAcc, lightning, flashT;
    final Random R = new Random();
    public Node3 beam, stars, moon;
    final Array<float[]> clouds = new Array<>();
    public final Array<Vector3> stacks = new Array<>();
    final Color cloudTint = new Color();
    public final Color smokeTint = new Color(0.84f, 0.83f, 0.81f, 1);
    public final Dyn cloudDyn, rainDyn;
    Mat starMat;
    float[] precip;

    public World(Renderer r) {
        rend = r;
        Mat cm = new Mat(); cm.unshaded = true; cm.vcol = true; cm.tex = Tex.cloud(); cm.blend = Mat.ALPHA; cm.depthWrite = false; cm.cull = false; cm.fog = false; cm.order = -10;
        cloudDyn = new Dyn(cm);
        Mat rm = new Mat(); rm.unshaded = true; rm.vcol = true; rm.blend = Mat.ALPHA; rm.depthWrite = false; rm.cull = false;
        rainDyn = new Dyn(rm);
    }

    float rr(float a, float b) { return a + R.nextFloat() * (b - a); }

    static Color c(String h) { return G.hex(h); }

    public float hNoise(float x, float z) {
        return (float) (Math.sin(x * 0.004) * Math.cos(z * 0.0035) * 22 + Math.sin(x * 0.011 + 1.3) * Math.sin(z * 0.009 + 0.7) * 9
                + Math.sin(x * 0.031 + z * 0.027) * 2.5 + Math.sin(x * 0.07 - z * 0.05) * 0.8);
    }

    public float height(float x, float z) {
        float d = (float) Math.sqrt(x * x + z * z);
        float f = MathUtils.clamp((d - 55f) / 320f, 0, 1);
        float h;
        if (hf == 1) h = (float) ((Math.sin(x * 0.012 + z * 0.004) * 6 + Math.sin(x * 0.005 - z * 0.009) * 9) * f);
        else h = hNoise(x, z) * f * f;
        float rd = Math.abs(x - ROAD_X);
        if (rd < 30) h *= 0.25f + 0.75f * MathUtils.clamp((rd - 8) / 22f, 0, 1);
        if (sea && x < -120) h = h * MathUtils.clamp(1 - (-120 - x) / 60f, 0, 1) - MathUtils.clamp((-120 - x) / 40f, 0, 1) * 14;
        if (dam && z < -150 && z > -520 && Math.abs(x) < 150) h = Math.min(h, -3 + Math.max(0, Math.abs(x) - 110) * 0.6f);
        if (d > 1000) h += (d - 1000) * 0.09f;
        return h;
    }

    Node3 add(Mesh3 m, Mat mat) {
        meshes.add(m);
        Node3 n = new Node3(m, mat);
        content.add(n);
        return n;
    }

    static Color lerp(Color a, Color b, float t) { return a.cpy().lerp(b, t); }

    public void build(int theme, boolean dark, float wRain, float wFog, float wWind, String obj, boolean noBeam) {
        if (content != null) { content.remove(); for (Mesh3 m : meshes) m.dispose(); meshes.clear(); }
        content = new Node3();
        root.add(content);
        JsonValue T = G.D.get("THEMES").get(Math.min(theme, G.D.get("THEMES").size - 1));
        themeName = T.getString("name");
        String[] Gr = GROUND.containsKey(themeName) ? GROUND.get(themeName) : GROUND.get("dusk");
        night = dark || themeName.equals("night");
        hf = T.getBoolean("dunes", false) ? 1 : 0;
        sea = T.getBoolean("sea", false);
        dam = "dam".equals(obj);
        rain = wRain; fog = wFog; wind = wWind;
        snow = T.getBoolean("snow", false);
        stacks.clear(); clouds.clear();
        int gfx = G.SV.gfx;
        float[] atm = ATM.containsKey(themeName) ? ATM.get(themeName) : ATM.get("dusk");
        JsonValue sunInfo = T.get("sun");
        float sx = sunInfo != null ? sunInfo.getFloat("x", 0.7f) : 0.7f;
        String sunCol = sunInfo != null ? sunInfo.getString("col", "255,220,190") : "255,220,190";
        float az = (sx - 0.5f) * 1.4f;
        float el = night ? 0.5f : atm[3] * MathUtils.PI / 180f;
        Renderer.Env e = rend.env;
        e.sunDir.set(-MathUtils.sin(az) * MathUtils.cos(el), MathUtils.sin(el), -MathUtils.cos(az) * MathUtils.cos(el)).nor();
        JsonValue sk = T.get("sky");
        float lift = night ? 0 : 0.18f;
        for (int i = 0; i < 4; i++) e.sky[i].set(lerp(c(sk.getString(i)), c("bcd0ea"), lift * (0.4f + i * 0.2f)));
        e.skySun.set(G.rgbList(sunCol));
        e.sunOn = night ? 0 : (rain > 0 ? 0.3f : 1f);
        if (night) e.amb.set(c("33476e"));
        else e.amb.set(lerp(lerp(c(sk.getString(2)), c(sk.getString(3)), 0.5f), c("aab8cc"), 0.5f));
        e.ambEnergy = night ? 0.6f : 0.95f;
        e.exposure = night ? 1.15f : (themeName.equals("snow") ? 0.85f : 1f);
        Color fogc;
        if (night) fogc = c("0e1626");
        else { fogc = lerp(G.rgbList(T.getString("fog")), c("8a96a8"), 0.45f); fogc.mul(0.82f, 0.82f, 0.82f, 1f); }
        if (rain > 0) fogc = lerp(fogc, c("5a6270"), 0.6f);
        e.fogCol.set(fogc);
        e.fogD = (night ? 0.0007f : 0.00042f) * (1 + fog * 3) * (rain > 0 ? 2.2f : 1f);
        e.sunCol.set(night ? c("8fa6d8") : lerp(G.rgbList(sunCol), Color.WHITE, 0.35f));
        e.sunEnergy = night ? 0.45f : (rain > 0 ? 0.65f : (themeName.equals("snow") ? 1.25f : 1.45f));
        rend.shadows = gfx >= 1;
        if (night) smokeTint.set(0.4f, 0.42f, 0.48f, 1);
        else { smokeTint.set(0.84f, 0.83f, 0.81f, 1).lerp(e.sunCol, 0.12f); if (rain > 0) smokeTint.mul(0.75f, 0.75f, 0.75f, 1); }
        SmokeTrail.tint.set(smokeTint);
        SmokeTrail.wind = wind * 0.05f;

        buildTerrain(Gr, gfx);
        // дорога
        MB rb = new MB();
        Color rcol = night ? c("3a3428") : (themeName.equals("snow") ? c("a9adb2") : c("6e5e48"));
        for (float z = 700f; z > -1100f; z -= 12f) {
            float y = Math.max(height(ROAD_X - 4, z), Math.max(height(ROAD_X + 4, z), height(ROAD_X, z))) + 0.12f;
            rb.box(8, 0.1f, 12.4f, rcol, ROAD_X, y, z - 6);
        }
        Mat roadMat = Mat.vc(); roadMat.shadowRecv = true;
        add(rb.mesh(), roadMat);
        buildPost();
        if (!Gr[4].isEmpty()) buildTrees(Gr, gfx);
        if (!Gr[5].isEmpty() && gfx >= 1) buildGrass(Gr, gfx);
        buildRocks(Gr);
        buildMountains(T, Gr, gfx);
        buildExtras(T);
        buildClouds(T);
        stars = null; moon = null;
        if (night) buildStars();
        beamOn = night && !noBeam;
        Mat bm = new Mat(); bm.unshaded = true; bm.vcol = true; bm.blend = Mat.ADD; bm.depthWrite = false; bm.cull = false; bm.fog = false;
        MB bb = new MB();
        // конус луча прожектора: вершина у источника, расширение к цели (вдоль -Z)
        int segs = 20;
        for (int i = 0; i < segs; i++) {
            float a0 = (float) i / segs * MathUtils.PI2, a1 = (float) (i + 1) / segs * MathUtils.PI2;
            Color c0 = new Color(0.18f, 0.17f, 0.15f, 1), c1 = new Color(0f, 0f, 0f, 1);
            // тонкий треугольный конус от 0 до -600
            float r0 = 0.4f, r1 = 60f;
            float[] p = {MathUtils.cos(a0) * r0, MathUtils.sin(a0) * r0, 0, MathUtils.cos(a1) * r0, MathUtils.sin(a1) * r0, 0,
                    MathUtils.cos(a1) * r1, MathUtils.sin(a1) * r1, -600, MathUtils.cos(a0) * r1, MathUtils.sin(a0) * r1, -600};
            float[][] cols = {{c0.r, c0.g, c0.b}, {c0.r, c0.g, c0.b}, {c1.r, c1.g, c1.b}, {c1.r, c1.g, c1.b}};
            int[] idx = {0, 1, 2, 0, 2, 3};
            for (int k : idx) bb.v.addAll(p[k * 3], p[k * 3 + 1], p[k * 3 + 2], 0, 1, 0, cols[k][0], cols[k][1], cols[k][2], 1, 0, 0);
        }
        beam = add(bb.mesh(), bm);
        beam.visible = beamOn;
        precip = null;
    }

    void buildTerrain(String[] Gr, int gfx) {
        int AS = gfx >= 2 ? 160 : (gfx >= 1 ? 128 : 96);
        int RS = gfx >= 2 ? 64 : (gfx >= 1 ? 52 : 40);
        float RMAX = 2500f;
        Color cA = c(Gr[0]), cB = c(Gr[1]), cR = c(Gr[2]);
        Color cRoad = themeName.equals("snow") ? c("b7b9bb") : c("7a6a52");
        int nv = 1 + RS * AS;
        float[] px = new float[nv * 3], col = new float[nv * 3], uv = new float[nv * 2];
        Color cc = new Color();
        int k = 0;
        for (int j = 0; j <= RS; j++) {
            int cnt = j == 0 ? 1 : AS;
            for (int i = 0; i < cnt; i++) {
                float x, z;
                if (j == 0) { x = 0; z = 0; } else {
                    float r = RMAX * (float) Math.pow((float) j / RS, 1.9);
                    float a = (float) i / AS * MathUtils.PI2;
                    x = MathUtils.sin(a) * r; z = MathUtils.cos(a) * r;
                }
                float y = height(x, z);
                px[k * 3] = x; px[k * 3 + 1] = y; px[k * 3 + 2] = z;
                uv[k * 2] = x / 9f; uv[k * 2 + 1] = z / 9f;
                float n = (float) ((Math.sin(x * 0.021) * Math.sin(z * 0.017) + Math.sin(x * 0.06 + z * 0.05) * 0.4 + Math.sin(x * 0.13 - z * 0.11) * 0.25 + 1.65) / 3.3);
                cc.set(cA).lerp(cB, MathUtils.clamp(n, 0, 1));
                if (y > 20) cc.lerp(cR, MathUtils.clamp((y - 20) / 30f, 0, 0.8f));
                if (sea && x < -110) cc.lerp(c("c9b48a"), MathUtils.clamp((-110 - x) / 30f, 0, 1));
                if (Math.abs(x - ROAD_X) < 9) cc.lerp(cRoad, 0.6f);
                float m = night ? 0.65f : 1.18f;
                col[k * 3] = cc.r * m; col[k * 3 + 1] = cc.g * m; col[k * 3 + 2] = cc.b * m;
                k++;
            }
        }
        com.badlogic.gdx.utils.IntArray idx = new com.badlogic.gdx.utils.IntArray();
        for (int i = 0; i < AS; i++) idx.addAll(0, 1 + (i + 1) % AS, 1 + i);
        for (int j = 1; j < RS; j++) for (int i = 0; i < AS; i++) {
            int A = 1 + (j - 1) * AS + i, B = 1 + (j - 1) * AS + (i + 1) % AS, C = A + AS, D = B + AS;
            idx.addAll(A, B, C, B, D, C);
        }
        add(indexedMesh(px, col, uv, idx, true), terrainMat());
    }

    Mat terrainMat() {
        Mat m = Mat.vc(); m.tex = Tex.ground(themeName); m.rough = 1f; m.shadowRecv = true; m.spec = 0.2f;
        return m;
    }

    /** Индексированная сетка → неиндексированный меш с гладкими нормалями (generate_normals). up=true: нормали вверх. */
    Mesh3 indexedMesh(float[] p, float[] col, float[] uv, com.badlogic.gdx.utils.IntArray idx, boolean faceUp) {
        int nv = p.length / 3;
        float[] nrm = new float[nv * 3];
        for (int t = 0; t < idx.size; t += 3) {
            int a = idx.get(t), b = idx.get(t + 1), c2 = idx.get(t + 2);
            float ex = p[b * 3] - p[a * 3], ey = p[b * 3 + 1] - p[a * 3 + 1], ez = p[b * 3 + 2] - p[a * 3 + 2];
            float fx = p[c2 * 3] - p[a * 3], fy = p[c2 * 3 + 1] - p[a * 3 + 1], fz = p[c2 * 3 + 2] - p[a * 3 + 2];
            float nx = ey * fz - ez * fy, ny = ez * fx - ex * fz, nz = ex * fy - ey * fx;
            if (faceUp && ny < 0) { nx = -nx; ny = -ny; nz = -nz; }
            for (int v : new int[]{a, b, c2}) { nrm[v * 3] += nx; nrm[v * 3 + 1] += ny; nrm[v * 3 + 2] += nz; }
        }
        FloatArray out = new FloatArray(idx.size * Mesh3.STRIDE);
        for (int t = 0; t < idx.size; t += 3) {
            int a = idx.get(t), b = idx.get(t + 1), c2 = idx.get(t + 2);
            // ориентация: CW относительно нормали вверх
            float ex = p[b * 3] - p[a * 3], ez = p[b * 3 + 2] - p[a * 3 + 2];
            float fx = p[c2 * 3] - p[a * 3], fz = p[c2 * 3 + 2] - p[a * 3 + 2];
            float cy = ez * fx - ex * fz;
            int[] ord = cy > 0 ? new int[]{a, c2, b} : new int[]{a, b, c2};
            if (!faceUp) ord = new int[]{a, b, c2};
            for (int v : ord) {
                float nx = nrm[v * 3], ny = nrm[v * 3 + 1], nz = nrm[v * 3 + 2];
                float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (l > 0) { nx /= l; ny /= l; nz /= l; }
                out.addAll(p[v * 3], p[v * 3 + 1], p[v * 3 + 2], nx, ny, nz, col[v * 3], col[v * 3 + 1], col[v * 3 + 2], 1f, uv[v * 2], uv[v * 2 + 1]);
            }
        }
        return new Mesh3(out.items, out.size);
    }

    void buildPost() {
        // плита
        MB sb = new MB();
        sb.add(MB.pBox(16, 0.25f, 20), Color.WHITE, new Matrix4().setToTranslation(0, 0.12f, 0.5f), 1f);
        // UV плиты: растянуть на всю плиту
        Mat slab = Mat.std(night ? c("3a3a38") : c("77746e")); slab.tex = Tex.slab(); slab.rough = 0.95f; slab.shadowRecv = true;
        slab.vcol = false;
        add(sb.mesh(), slab);
        MB b = new MB();
        Color sand = themeName.equals("snow") ? c("c8ccd0") : c("9c8a5f");
        for (int i = 0; i < 30; i++) {
            float a = -MathUtils.PI * 0.9f + (i / 29f) * MathUtils.PI * 1.8f, r = 11.5f;
            for (int k = 0; k < (i % 2 == 1 ? 2 : 3); k++) {
                float d = rr(-0.05f, 0.05f);
                Color sc = new Color(MathUtils.clamp(sand.r + d, 0, 1), MathUtils.clamp(sand.g + d, 0, 1), MathUtils.clamp(sand.b + d, 0, 1), 1);
                b.capsule(0.42f, 1.2f, sc, MathUtils.sin(a) * r, 0.42f + k * 0.62f, MathUtils.cos(a) * r + 1, 0, a, MathUtils.PI / 2);
            }
        }
        for (float[] p : new float[][]{{5.5f, 7.0f}, {6.7f, 6.1f}, {6.1f, 6.6f}}) b.box(1.3f, 0.95f, 1.1f, c("4f5a3a"), p[0], 0.6f, p[1]);
        b.box(1.2f, 0.9f, 1.0f, c("4f5a3a"), 6.1f, 1.5f, 6.6f);
        b.cyl(0f, 3.4f, 2.8f, 4, c("56603f"), -6.5f, 1.5f, 8.8f, 0, MathUtils.PI / 4, 0);
        b.cyl(0.12f, 0.18f, 10, 6, c("555555"), -13, 5, -9); b.box(2.4f, 0.1f, 0.1f, c("555555"), -13, 9.6f, -9);
        for (int i = 0; i < 36; i++) {
            float a = i / 36f * MathUtils.PI2, x = MathUtils.sin(a) * 30, zz = MathUtils.cos(a) * 30;
            if (Math.abs(x - ROAD_X) < 8) continue;
            b.cyl(0.07f, 0.09f, 1.6f, 5, c("5a4a3a"), x, height(x, zz) + 0.8f, zz);
        }
        for (float[] p : new float[][]{{14, -14}, {-16, 14}}) {
            b.cyl(0.15f, 0.2f, 9, 6, c("444444"), p[0], 4.5f, p[1]);
            b.box(1.6f, 0.8f, 0.6f, c("333333"), p[0], 9.2f, p[1]);
        }
        Mat pm = Mat.vc(); pm.shadowRecv = true;
        Node3 n = add(b.mesh(), pm);
        n.castShadow = true;
        if (night) {
            MB lb = new MB();
            for (float[] p : new float[][]{{14, -14}, {-16, 14}}) lb.box(1.3f, 0.5f, 0.1f, c("fff0c0"), p[0], 9.2f, p[1] - 0.32f);
            add(lb.mesh(), Models.MAT_GLOW);
        }
    }

    Mat std(Color col, Texture tex, boolean colors) {
        Mat m = new Mat();
        m.color.set(col);
        if (night) m.color.mul(0.6f, 0.6f, 0.6f, 1);
        m.color.a = 1;
        m.tex = tex; m.rough = 1f; m.vcol = colors; m.spec = 0.25f;
        return m;
    }

    static Color darkened(Color c, float k) { return new Color(c.r * (1 - k), c.g * (1 - k), c.b * (1 - k), c.a); }

    static Color lightened(Color c, float k) { return new Color(c.r + (1 - c.r) * k, c.g + (1 - c.g) * k, c.b + (1 - c.b) * k, c.a); }

    void buildTrees(String[] Gr, int gfx) {
        int N = gfx >= 2 ? 520 : (gfx >= 1 ? 380 : 220);
        if (G.isMobile()) N = new int[]{180, 260, 340}[MathUtils.clamp(gfx, 0, 2)];
        Texture needle = Tex.grain("needle", 0.72f, 900, 0.4f, 1.0f);
        String kind = Gr[4];
        MB crown = new MB(), crown2 = new MB(), trunk = new MB();
        MB.Prim pC, pC2, pT = null;
        Mat mC, mC2, mT = null;
        if (kind.equals("pine") || kind.equals("snowpine")) {
            pC = MB.pCyl(0, 2.4f, 5.5f, 8, true); pC2 = MB.pCyl(0, 1.7f, 4.2f, 8, true);
            mC = std(kind.equals("snowpine") ? c("3a5646") : c("34502e"), needle, true);
            mC2 = std(kind.equals("snowpine") ? c("e6edf4") : c("3d5c33"), needle, true);
            pT = MB.pCyl(0.25f, 0.35f, 2, 5, false); mT = std(c("4a3426"), null, false);
        } else if (kind.equals("autumn")) {
            pC = MB.pSph(2.6f, 4.6f, 8, 4); pC2 = MB.pSph(1.8f, 3.2f, 7, 3);
            mC = std(Color.WHITE, needle, true); mC2 = std(Color.WHITE, needle, true);
            pT = MB.pCyl(0.22f, 0.34f, 3, 5, false); mT = std(c("4a3426"), null, false);
        } else if (kind.equals("cactus")) {
            pC = MB.pCyl(0.35f, 0.4f, 4, 6, true); pC2 = MB.pSph(1.4f, 2.8f, 5, 3);
            mC = std(c("5f7a3a"), null, true); mC2 = std(c(Gr[2]), null, true);
        } else {
            pC = MB.pCyl(0.12f, 0.35f, 6, 5, true); pC2 = MB.pCyl(0, 1.6f, 3.0f, 8, true);
            mC = std(c("2a221c"), null, true); mC2 = std(c("3a2e24"), null, true);
        }
        Matrix4 t = new Matrix4();
        Color[] leaves = {c("d2721e"), c("c0401e"), c("e0a62a"), c("9a5a1a"), c("6f7a2a")};
        int k = 0, guard = 0;
        while (k < N && guard < N * 10) {
            guard++;
            float a = R.nextFloat() * MathUtils.PI2, r = rr(45, 1150), x = MathUtils.sin(a) * r, z = MathUtils.cos(a) * r;
            if (Math.abs(x - ROAD_X) < 14) continue;
            float y = height(x, z);
            if (y < -1) continue;
            float s = rr(0.7f, 1.5f), sy = s * rr(0.85f, 1.3f), ry = R.nextFloat() * MathUtils.PI2;
            Color tc = darkened(Color.WHITE, rr(-0.15f, 0.25f)); tc.a = 1;
            if (kind.equals("pine") || kind.equals("snowpine")) {
                crown.add(pC, tc, inst(t, x, y + 4 * sy, z, ry, s, sy));
                crown2.add(pC2, kind.equals("pine") ? tc : Color.WHITE, inst(t, x, y + 6.6f * sy, z, ry, s, sy));
                trunk.add(pT, Color.WHITE, inst(t, x, y + 1 * sy, z, ry, s, sy));
            } else if (kind.equals("autumn")) {
                Color leaf = leaves[R.nextInt(5)];
                crown.add(pC, darkened(leaf, rr(-0.1f, 0.2f)), inst(t, x, y + 4.4f * sy, z, ry, s, sy));
                crown2.add(pC2, lightened(leaf, 0.08f), inst(t, x + 0.8f, y + 6.2f * sy, z - 0.5f, ry, s, sy));
                trunk.add(pT, Color.WHITE, inst(t, x, y + 1.5f * sy, z, ry, s, sy));
            } else if (kind.equals("cactus")) {
                crown.add(pC, tc, inst(t, x, y + 2 * sy, z, ry, s, sy));
                crown2.add(pC2, tc, inst(t, x + 6, y + 0.3f, z + 4, ry, s, sy));
            } else {
                crown.add(pC, tc, inst(t, x, y + 3 * sy, z, ry, s, sy));
                crown2.add(pC2, tc, inst(t, x + 3, y + 1, z, ry, s * 0.8f, sy * 0.8f));
            }
            k++;
        }
        add(crown.mesh(), mC);
        add(crown2.mesh(), mC2);
        if (!trunk.empty() && mT != null) add(trunk.mesh(), mT);
    }

    static Matrix4 inst(Matrix4 t, float x, float y, float z, float ry, float s, float sy) {
        return t.idt().translate(x, y, z).rotateRad(Vector3.Y, ry).scale(s, sy, s);
    }

    void buildGrass(String[] Gr, int gfx) {
        Texture tex = Tex.grass();
        MB b = new MB();
        MB.Prim q = MB.pQuad(1.5f, 1.0f);
        boolean mob = G.isMobile();
        int N = !mob ? (gfx >= 2 ? 2600 : 1400) : new int[]{500, 900, 1300}[MathUtils.clamp(gfx, 0, 2)];
        Matrix4 t = new Matrix4(), base = new Matrix4();
        Color col = new Color();
        for (int i = 0; i < N; i++) {
            float x, z;
            while (true) {
                float a = R.nextFloat() * MathUtils.PI2, r = 9 + (float) Math.pow(R.nextFloat(), 0.8) * 130;
                x = MathUtils.sin(a) * r; z = MathUtils.cos(a) * r;
                if (Math.abs(x - ROAD_X) >= 6 && !(Math.abs(x) < 9 && z > -10 && z < 11)) break;
            }
            float s = rr(0.7f, 1.5f);
            base.idt().translate(x, height(x, z) - 0.05f, z).rotateRad(Vector3.Y, R.nextFloat() * MathUtils.PI).scale(s, s * rr(0.7f, 1.2f), s);
            hsv(col, 0.17f + rr(-0.04f, 0.05f), 0.35f, rr(0.85f, 1.05f));
            for (int k = 0; k < 3; k++) {
                t.set(base).translate(0, 0.5f, 0).rotateRad(Vector3.Y, k * MathUtils.PI / 3);
                int start = b.v.size;
                b.add(q, col, t);
                for (int o = start; o < b.v.size; o += Mesh3.STRIDE) { b.v.items[o + 3] = 0; b.v.items[o + 4] = 1; b.v.items[o + 5] = 0; }
            }
        }
        Mat m = std(lightened(c(Gr[5]), 0.12f), tex, true);
        m.cull = false; m.scissor = true;
        add(b.mesh(), m);
    }

    static void hsv(Color out, float h, float s, float v) {
        h = (h % 1 + 1) % 1;
        int i = (int) (h * 6); float f = h * 6 - i, p = v * (1 - s), q = v * (1 - f * s), t = v * (1 - (1 - f) * s);
        switch (i % 6) {
            case 0: out.set(v, t, p, 1); break;
            case 1: out.set(q, v, p, 1); break;
            case 2: out.set(p, v, t, 1); break;
            case 3: out.set(p, q, v, 1); break;
            case 4: out.set(t, p, v, 1); break;
            default: out.set(v, p, q, 1); break;
        }
    }

    void buildRocks(String[] Gr) {
        MB b = new MB();
        MB.Prim sph = MB.pSph(1, 2, 6, 3);
        Matrix4 t = new Matrix4();
        for (int i = 0; i < 160; i++) {
            float a = R.nextFloat() * MathUtils.PI2, r = rr(35, 700), x = MathUtils.sin(a) * r, z = MathUtils.cos(a) * r, s = rr(0.6f, 3.5f);
            t.idt().translate(x, height(x, z) + s * 0.2f, z).rotateRad(Vector3.Y, R.nextFloat() * 3).rotateRad(Vector3.X, R.nextFloat() * 3).scale(s, s * rr(0.5f, 0.9f), s);
            b.add(sph, Color.WHITE, t);
        }
        Mat m = std(c(Gr[2]), Tex.grain("rock", 0.75f, 700, 0.45f, 1.0f), false);
        add(b.mesh(), m);
    }

    void buildMountains(JsonValue T, String[] Gr, int gfx) {
        int SEG = gfx >= 1 ? 180 : 120, RAD = 16;
        float r0 = 1250f, r1 = 2700f;
        Color cM = c(Gr[3]), cS = night ? c("8c96aa") : c("f2f5fa"), cL = c(Gr[1]);
        float ph = R.nextFloat() * 100;
        int nv = (SEG + 1) * (RAD + 1);
        float[] p = new float[nv * 3], col = new float[nv * 3], uv = new float[nv * 2];
        Color cc = new Color();
        int k = 0;
        for (int i = 0; i <= SEG; i++) {
            float a = (float) i / SEG * MathUtils.PI2;
            for (int j = 0; j <= RAD; j++) {
                float t = (float) j / RAD, r = r0 + (r1 - r0) * t, x = MathUtils.sin(a) * r, z = MathUtils.cos(a) * r;
                float ridge = 0, amp = 1, f = 1;
                for (int o = 0; o < 4; o++) {
                    ridge += (1 - Math.abs(Math.sin(a * 7 * f + ph + o * 1.7f) * Math.cos(a * 3.3f * f + o))) * amp;
                    amp *= 0.5f; f *= 2.1f;
                }
                float envv = MathUtils.sin(t * MathUtils.PI) * (0.35f + 0.65f * t);
                float y = ridge * 360 * envv + height(x * 0.5f, z * 0.5f) * 0.5f - 20;
                if (sea && MathUtils.sin(a) > 0.3f && MathUtils.cos(a) < 0.7f) y = Math.min(y, -6);
                cc.set(cL).lerp(cM, MathUtils.clamp(y / 120f, 0, 1));
                float sl = themeName.equals("snow") ? 90f : 220f;
                if (y > sl) cc.lerp(cS, MathUtils.clamp((y - sl) / 60f, 0, 1));
                if (night) cc.mul(0.6f, 0.6f, 0.6f, 1);
                p[k * 3] = x; p[k * 3 + 1] = y; p[k * 3 + 2] = z;
                col[k * 3] = cc.r; col[k * 3 + 1] = cc.g; col[k * 3 + 2] = cc.b;
                uv[k * 2] = a * 40; uv[k * 2 + 1] = t * 12;
                k++;
            }
        }
        com.badlogic.gdx.utils.IntArray idx = new com.badlogic.gdx.utils.IntArray();
        for (int i = 0; i < SEG; i++) for (int j = 0; j < RAD; j++) {
            int A = i * (RAD + 1) + j, B = A + RAD + 1;
            idx.addAll(A, A + 1, B, B, A + 1, B + 1);
        }
        Mat m = Mat.vc(); m.tex = Tex.grain("rock", 0.75f, 700, 0.45f, 1.0f); m.rough = 1; m.cull = false; m.spec = 0.2f;
        add(indexedMesh(p, col, uv, idx, true), m);
    }

    void buildExtras(JsonValue T) {
        if (themeName.equals("dusk") || themeName.equals("night") || themeName.equals("dawn") || themeName.equals("coast") || themeName.equals("snow")) {
            MB b = new MB(), wl = new MB();
            float cx = 290f, cz = -460f;
            for (int i = 0; i < 44; i++) {
                float x = cx + rr(-110, 110), z = cz + rr(-90, 90), w = rr(7, 12), h = rr(5, 9), d = rr(7, 10), y = height(x, z);
                float ry = Math.round(R.nextFloat() * 3) * MathUtils.PI / 2;
                b.box(w, h, d, themeName.equals("snow") ? c("a89a8a") : c("b9a48a"), x, y + h / 2 - 0.5f, z, 0, ry, 0);
                b.cyl(0, Math.max(w, d) * 0.75f, h * 0.45f, 4, themeName.equals("snow") ? c("eef2f8") : c("7a3b2e"), x, y + h + h * 0.2f - 0.5f, z, 0, ry + MathUtils.PI / 4, 0);
                if (night && R.nextFloat() < 0.7f) wl.box(1.2f, 1.1f, 0.2f, c("ffd27a"), x, y + h * 0.5f, z + d / 2 * 1.01f);
            }
            add(b.mesh(), Models.MAT_VC);
            if (!wl.empty()) add(wl.mesh(), Models.MAT_GLOW);
        }
        if (T.getBoolean("factory", false) || themeName.equals("industry")) {
            MB b = new MB(), gl = new MB();
            for (int i = 0; i < 14; i++) {
                float x = rr(-500, 500), z = rr(-900, -600), w = rr(30, 70), h = rr(18, 45), d = rr(30, 60);
                b.box(w, h, d, c("3a3030"), x, height(x, z) + h / 2, z);
                gl.box(w * 0.8f, 1.2f, 0.5f, c("ff8a3a"), x, height(x, z) + h * 0.6f, z + d / 2 + 0.3f);
            }
            for (int i = 0; i < 7; i++) {
                float x = -360 + i * 120 + rr(-30, 30), z = rr(-820, -700), h = rr(70, 120), y = height(x, z);
                b.cyl(5, 7, h, 10, c("4a3a36"), x, y + h / 2, z); b.cyl(5.3f, 5.3f, 3, 10, c("8a2a20"), x, y + h - 6, z);
                stacks.add(new Vector3(x, y + h, z));
            }
            add(b.mesh(), Models.MAT_VC); add(gl.mesh(), Models.MAT_GLOW);
        }
        if (themeName.equals("desert")) {
            MB b = new MB();
            for (int i = 0; i < 8; i++) {
                float x = 220f + (i % 4) * 34, z = -380f - (float) Math.floor(i / 4f) * 40, y = height(x, z);
                b.cyl(13, 13, 14, 20, c("d8d2c4"), x, y + 7, z); b.cyl(13.3f, 13.3f, 1, 20, c("b02a20"), x, y + 10, z);
            }
            add(b.mesh(), Models.MAT_VC);
        }
        if (sea || dam) {
            MB w = new MB();
            float sx = sea ? 4000 : 300, sz = sea ? 6000 : 380;
            Matrix4 t = new Matrix4().setToTranslation(sea ? -2150 : 0, sea ? -3 : 14, sea ? -500 : -440);
            // плоскость воды из сетки (чтобы туман по вершинам работал корректно)
            int n = 24;
            for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) {
                float x0 = -sx / 2 + sx * i / n, x1 = -sx / 2 + sx * (i + 1) / n, z0 = -sz / 2 + sz * j / n, z1 = -sz / 2 + sz * (j + 1) / n;
                w.add(MB.pBox(x1 - x0, 0.01f, z1 - z0), night ? c("0b1a2a") : c("1d4a6a"), new Matrix4(t).translate((x0 + x1) / 2, 0, (z0 + z1) / 2));
            }
            Mat wm = Mat.vc(); wm.rough = 0.1f; wm.metal = 0.2f; wm.spec = 1.5f;
            add(w.mesh(), wm);
        }
        if (sea) {
            MB b = new MB();
            float x = -230f, z = -360f, y = Math.max(0, height(x, z));
            b.cyl(4, 6, 34, 14, c("e8e2d6"), x, y + 17, z);
            for (int i = 0; i < 3; i++) b.cyl(4.2f + i * 0.6f, 4.2f + i * 0.6f, 3, 14, c("b02a20"), x, y + 6 + i * 10, z);
            b.cyl(3.2f, 3.2f, 5, 10, c("333333"), x, y + 36, z);
            add(b.mesh(), Models.MAT_VC);
        }
    }

    void buildClouds(JsonValue T) {
        int n = rain > 0 ? 60 : 40;
        if (night) cloudTint.set(c("2a3550"));
        else if (rain > 0) cloudTint.set(c("8a8f98"));
        else cloudTint.set(G.rgbList(T.getString("cloudHi", "255,240,230")).lerp(Color.WHITE, 0.6f));
        for (int i = 0; i < n; i++) {
            float a = R.nextFloat() * MathUtils.PI2, r = rr(250, 2000), w = rr(170, 420);
            float y = rain > 0 ? rr(160, 280) : rr(230, 560);
            float al = (rain > 0 ? 0.9f : rr(0.5f, 0.85f)) * (night ? 0.7f : 1f);
            clouds.add(new float[]{MathUtils.sin(a) * r, y, MathUtils.cos(a) * r, w, w * 0.45f, al});
        }
    }

    void buildStars() {
        MB b = new MB();
        for (int i = 0; i < 900; i++) {
            float u = R.nextFloat() * MathUtils.PI2, v = rr(0.05f, 1f), r = 2600f;
            float q = (float) Math.sqrt(1 - v * v);
            float k = rr(0.5f, 1f);
            b.box(2.5f, 2.5f, 2.5f, new Color(k, k, k, 1), MathUtils.cos(u) * q * r, v * r, MathUtils.sin(u) * q * r);
        }
        Mat sm = Mat.glow(); sm.fog = false; sm.order = -20;
        stars = add(b.mesh(), sm);
        MB mb = new MB();
        mb.sph(60, c("dfe8ff"), 0, 0, 0, 1, 1, 1, 16);
        Mat mm = Mat.glow(); mm.fog = false; mm.order = -20;
        moon = add(mb.mesh(), mm);
    }

    final Vector3 tv = new Vector3(), rightV = new Vector3(), upV = new Vector3();

    public void update(float dt, Camera cam) {
        tAcc += dt;
        for (float[] c2 : clouds) {
            c2[0] += dt * (2 + wind * 0.03f);
            if (c2[0] > 2100) c2[0] = -2100;
            if (c2[0] < -2100) c2[0] = 2100;
        }
        Vector3 cp = cam.position;
        if (stars != null) stars.pos.set(cp.x, 0, cp.z);
        if (moon != null) moon.pos.set(cp.x, 0, cp.z).add(tv.set(0.5f, 0.45f, -1).nor().scl(2500));
        if (rain > 0) {
            lightning -= dt;
            if (lightning <= 0) { lightning = rr(5, 12); flashT = 0.25f; }
        }
        if (flashT > 0) { flashT -= dt; rend.env.flash = Math.max(0, flashT) * 1.0f; } else rend.env.flash = 0;
        // облака
        cloudDyn.clear();
        Vector3 right = rightV.set(cam.direction).crs(cam.up).nor();
        Vector3 up = upV.set(right).crs(cam.direction).nor();
        float cr = cloudTint.r, cg = cloudTint.g, cb = cloudTint.b;
        // vcol у облаков — линейный цвет; переводим из sRGB
        cr = (float) Math.pow(cr, 2.2); cg = (float) Math.pow(cg, 2.2); cb = (float) Math.pow(cb, 2.2);
        for (float[] c2 : clouds) {
            float x = c2[0], y = c2[1], z = c2[2], hw = c2[3] / 2, hh = c2[4] / 2, a = c2[5];
            float ax = x - right.x * hw - up.x * hh, ay = y - right.y * hw - up.y * hh, az = z - right.z * hw - up.z * hh;
            float bx = x + right.x * hw - up.x * hh, by = y + right.y * hw - up.y * hh, bz = z + right.z * hw - up.z * hh;
            float cx = x + right.x * hw + up.x * hh, cy = y + right.y * hw + up.y * hh, cz = z + right.z * hw + up.z * hh;
            float dx = x - right.x * hw + up.x * hh, dy = y - right.y * hw + up.y * hh, dz = z - right.z * hw + up.z * hh;
            cloudDyn.vert(ax, ay, az, 0, 1, 0, cr, cg, cb, a, 0, 1); cloudDyn.vert(dx, dy, dz, 0, 1, 0, cr, cg, cb, a, 0, 0); cloudDyn.vert(cx, cy, cz, 0, 1, 0, cr, cg, cb, a, 1, 0);
            cloudDyn.vert(ax, ay, az, 0, 1, 0, cr, cg, cb, a, 0, 1); cloudDyn.vert(cx, cy, cz, 0, 1, 0, cr, cg, cb, a, 1, 0); cloudDyn.vert(bx, by, bz, 0, 1, 0, cr, cg, cb, a, 1, 1);
        }
        // осадки
        rainDyn.clear();
        if (rain > 0 || snow) {
            int n = rain > 0 ? 700 : 900;
            if (precip == null || precip.length != n * 3) {
                precip = new float[n * 3];
                for (int i = 0; i < n; i++) { precip[i * 3] = rr(-60, 60); precip[i * 3 + 1] = rr(-30, 32); precip[i * 3 + 2] = rr(-60, 60); }
            }
            float sp = rain > 0 ? 50 : 3.5f;
            for (int i = 0; i < n; i++) {
                precip[i * 3 + 1] -= sp * dt;
                if (snow) { precip[i * 3] += MathUtils.sin(tAcc + i) * dt * 0.8f; }
                precip[i * 3] += wind * 0.02f * dt;
                if (precip[i * 3 + 1] < -30) { precip[i * 3 + 1] += 62; precip[i * 3] = rr(-60, 60); precip[i * 3 + 2] = rr(-60, 60); }
                float x = cp.x + precip[i * 3], y = cp.y + precip[i * 3 + 1], z = cp.z + precip[i * 3 + 2];
                if (rain > 0) {
                    float w = 0.03f, h = 1.3f;
                    float rx = right.x * w, rz = right.z * w;
                    rainDyn.vert(x - rx, y, z - rz, 0, 1, 0, 0.45f, 0.5f, 0.6f, 0.45f, 0, 0);
                    rainDyn.vert(x - rx, y + h, z - rz, 0, 1, 0, 0.45f, 0.5f, 0.6f, 0.45f, 0, 0);
                    rainDyn.vert(x + rx, y + h, z + rz, 0, 1, 0, 0.45f, 0.5f, 0.6f, 0.45f, 0, 0);
                    rainDyn.vert(x - rx, y, z - rz, 0, 1, 0, 0.45f, 0.5f, 0.6f, 0.45f, 0, 0);
                    rainDyn.vert(x + rx, y + h, z + rz, 0, 1, 0, 0.45f, 0.5f, 0.6f, 0.45f, 0, 0);
                    rainDyn.vert(x + rx, y, z + rz, 0, 1, 0, 0.45f, 0.5f, 0.6f, 0.45f, 0, 0);
                } else {
                    rainDyn.billboard(tv.set(x, y, z), 0.18f, right, up, 1, 1, 1, 0.9f, 0);
                }
            }
        }
    }

    public void dispose() { for (Mesh3 m : meshes) m.dispose(); meshes.clear(); cloudDyn.dispose(); rainDyn.dispose(); }
}
