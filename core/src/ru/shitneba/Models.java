package ru.shitneba;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import ru.shitneba.gfx.MB;
import ru.shitneba.gfx.Mat;
import ru.shitneba.gfx.Mesh3;
import ru.shitneba.gfx.Node3;
import ru.shitneba.gfx.Tex;

/** Модели техники, врагов, ракеты, МОГ — порт models.gd. */
public final class Models {
    static final float PI = MathUtils.PI;

    static Color c(String h) { return G.hex(h); }

    static Vector3 V(float x, float y, float z) { return new Vector3(x, y, z); }

    static final Color DARK = c("23261f"), TIRE = c("161616"), STEEL = c("5a6064"), GLASS = c("1b2a3a"), WHITE = Color.WHITE, SHADE = c("bfbfbf");
    static final Color OLIVE_D = c("3b4030"), GUN = c("34383b"), RUBBER = c("1d1d1d");
    static final Color RIVET = c("6a6f68"), MARK = c("e8e4d6"), AMBER = c("ffb020"), HAZ = c("e0b020");
    static final Color GREY = c("4f545b"), DGREY = c("2c2f35"), LGREY = c("c5c9d0");
    static final Color BD1 = c("2a2d33"), BD2 = c("3a3e45"), BD3 = c("4c525a"), BDK = c("16181b"), BGL = c("1d2a40");

    public static Mat MAT_VC, MAT_GLOW, MAT_ROTOR, MAT_HULL, MAT_METAL, MAT_RUBBER, MAT_GLASS, MAT_LAMP, MAT_PAINT, MAT_MISSILE, MAT_NOSE;
    static final Map<String, Mat> camoMats = new HashMap<>();

    static Mat hull(float rough, float metal, float seams, float dust) {
        Mat m = new Mat(); m.hull = true; m.vcol = true;
        m.rough = rough; m.metal = metal; m.seams = seams; m.dust = dust;
        return m;
    }

    public static void initMaterials() {
        MAT_VC = Mat.vc();
        MAT_HULL = hull(0.45f, 0.25f, 0.5f, 0f); MAT_HULL.wear = 0.25f; MAT_HULL.panelScale = 0.3f;
        MAT_METAL = hull(0.32f, 0.75f, 0f, 0.3f); MAT_METAL.shadowRecv = true;
        MAT_RUBBER = hull(0.92f, 0f, 0f, 0.35f); MAT_RUBBER.shadowRecv = true;
        MAT_GLASS = Mat.std(new Color(0.05f, 0.08f, 0.11f, 1)); MAT_GLASS.rough = 0.05f; MAT_GLASS.metal = 0.9f; MAT_GLASS.spec = 1.2f;
        MAT_LAMP = Mat.vc(); MAT_LAMP.emission = 0.9f; MAT_LAMP.rough = 0.1f;
        MAT_GLOW = Mat.glow();
        MAT_ROTOR = new Mat(); MAT_ROTOR.unshaded = true; MAT_ROTOR.vcol = false; MAT_ROTOR.color.set(0.12f, 0.12f, 0.13f, 0.16f);
        MAT_ROTOR.blend = Mat.ALPHA; MAT_ROTOR.cull = false; MAT_ROTOR.depthWrite = false;
        MAT_PAINT = hull(0.6f, 0.1f, 0.45f, 1f); MAT_PAINT.panelScale = 0.26f; MAT_PAINT.shadowRecv = true;
        MAT_MISSILE = hull(0.38f, 0.25f, 0.3f, 0f);
        MAT_NOSE = Mat.std(c("9fa39a")); MAT_NOSE.rough = 0.35f; MAT_NOSE.metal = 0.3f;
    }

    static final Map<Mat, Mat> upCache = new HashMap<>();

    /** Тот же материал без пыли «снизу» (для башни и всего, что на ней: их высота считается не от земли). */
    static Mat upMat(Mat m) {
        Mat u = upCache.get(m);
        if (u != null) return u;
        u = m.cpy(); u.dust = 0f;
        upCache.put(m, u);
        return u;
    }

    public static Mat camoMat(String id) {
        Mat m = camoMats.get(id);
        if (m != null) return m;
        m = hull(0.55f, 0.12f, 0.42f, 1f);
        m.panelScale = 0.26f; m.camo = Tex.camoTile(id); m.shadowRecv = true;
        camoMats.put(id, m);
        return m;
    }

    // ---------- части ----------
    static final class Parts {
        final Map<String, Map<String, MB>> d = new LinkedHashMap<>();

        MB g(String node, String key) {
            Map<String, MB> m = d.get(node);
            if (m == null) { m = new LinkedHashMap<>(); d.put(node, m); }
            MB b = m.get(key);
            if (b == null) { b = new MB(); m.put(key, b); }
            return b;
        }
    }

    static void rail(MB M, Vector3 a, Vector3 b) { M.seg(a, b, 0.025f, 0.025f, 6, STEEL); }

    static void rail(MB M, Vector3 a, Vector3 b, float r) { M.seg(a, b, r, r, 6, STEEL); }

    static void wheel(Parts P, String node, float px, float py, float pz, float r, float w, float side) {
        MB R = P.g(node, "r"), D = P.g(node, "d"), M = P.g(node, "m");
        float rz = PI / 2;
        R.cyl(r * 0.93f, r * 0.93f, w, 22, RUBBER, px, py, pz, 0, 0, rz);
        R.cyl(r * 0.86f, r * 0.86f, w + 0.05f, 22, c("232323"), px, py, pz, 0, 0, rz);
        D.cyl(r * 0.6f, r * 0.6f, w + 0.07f, 18, OLIVE_D, px, py, pz, 0, 0, rz);
        M.cyl(r * 0.22f, r * 0.3f, w + 0.17f, 10, STEEL, px + side * 0.03f, py, pz, 0, 0, rz);
        for (int i = 0; i < 8; i++) {
            float a = i * MathUtils.PI2 / 8;
            M.cyl(0.03f, 0.03f, w + 0.12f, 6, STEEL, px, py + MathUtils.cos(a) * r * 0.42f, pz + MathUtils.sin(a) * r * 0.42f, 0, 0, rz);
        }
        for (int i = 0; i < 20; i++) {
            float a = i * MathUtils.PI2 / 20;
            R.box(w * 0.92f, 0.09f, 0.13f, RUBBER, px, py + MathUtils.cos(a) * r * 0.95f, pz + MathUtils.sin(a) * r * 0.95f, a, 0, 0);
        }
    }

    static void fender(MB b, float zf, float zb, float x, float y, float w, Color col) {
        float hf = (zb - zf) / 2f;
        float[][] pts = {{-hf - 0.3f, -0.28f}, {-hf, 0.05f}, {hf, 0.05f}, {hf + 0.3f, -0.28f}, {hf + 0.23f, -0.31f}, {hf - 0.03f, -0.02f}, {-hf + 0.03f, -0.02f}, {-hf - 0.23f, -0.31f}};
        b.extrude(pts, w, col, x, y, (zf + zb) / 2f);
    }

    static void tracks2(Parts P, float length, float x) {
        MB R = P.g("root", "r"), D = P.g("root", "d"), M = P.g("root", "m");
        float rz = PI / 2;
        for (int si = 0; si < 2; si++) {
            float X = (si == 0 ? -1 : 1) * x;
            R.box(0.64f, 0.07f, length - 0.9f, RUBBER, X, 1.02f, 0);
            R.box(0.64f, 0.07f, length - 1.6f, RUBBER, X, 0.04f, 0);
            for (float zz : new float[]{-1, 1}) R.cyl(0.47f, 0.47f, 0.64f, 16, RUBBER, X, 0.53f, zz * (length / 2 - 0.5f), 0, 0, rz);
            for (int i = 0; i < 6; i++) {
                float z = -length / 2 + 1.05f + i * (length - 2.1f) / 5;
                R.cyl(0.41f, 0.41f, 0.6f, 18, RUBBER, X, 0.46f, z, 0, 0, rz);
                D.cyl(0.33f, 0.33f, 0.64f, 16, OLIVE_D, X, 0.46f, z, 0, 0, rz);
                M.cyl(0.11f, 0.13f, 0.72f, 8, STEEL, X, 0.46f, z, 0, 0, rz);
            }
            M.cyl(0.44f, 0.44f, 0.5f, 12, GUN, X, 0.6f, -length / 2 + 0.45f, 0, 0, rz);
            for (int i = 0; i < 12; i++) {
                float a = i * MathUtils.PI2 / 12;
                M.box(0.48f, 0.1f, 0.12f, GUN, X, 0.6f + MathUtils.cos(a) * 0.46f, -length / 2 + 0.45f + MathUtils.sin(a) * 0.46f, a, 0, 0);
            }
            D.cyl(0.38f, 0.38f, 0.62f, 14, OLIVE_D, X, 0.6f, length / 2 - 0.45f, 0, 0, rz);
            int n = (int) ((length - 0.9f) / 0.26f);
            for (int i = 0; i < n; i++) {
                float z = -length / 2 + 0.45f + i * 0.26f;
                M.box(0.66f, 0.05f, 0.1f, GUN, X, 1.07f, z);
                if (Math.abs(z) < length / 2 - 0.75f) M.box(0.66f, 0.05f, 0.1f, GUN, X, 0f, z);
            }
            for (int i = 0; i < 3; i++) D.cyl(0.09f, 0.09f, 0.4f, 8, OLIVE_D, X, 0.98f, -length / 2 + 1.7f + i * (length - 3.4f) / 2, 0, 0, rz);
        }
    }

    static final float[] S2 = {-1f, 1f};

    /**
     * Корпус гусеничной машины (как у «Шилки» и «Тора»): нос с двумя бронелистами (верхний пологий, нижний крутой),
     * плоская крыша, наклонные борта надгусеничных ниш, полки над гусеницами с фальшбортом, корма с решёткой.
     * L — длина по гусенице, y0/y1 — низ и крыша корпуса, w — полуширина крыши.
     */
    static void hullT(MB C, MB D, MB M, float L, float y0, float y1, float w) {
        float h = y1 - y0, f = L / 2 + 0.15f, r = -L / 2 - 0.05f;
        // центральный корпус: профиль сбоку (вперёд — +), нос из двух листов
        C.extrude(new float[][]{{f - 0.35f, 0}, {r + 0.25f, 0}, {r, 0.3f}, {r, h - 0.08f}, {r + 0.12f, h}, {f - 1.6f, h}, {f, h * 0.42f}, {f + 0.02f, h * 0.3f}},
                w * 2f, WHITE, 0, y0, 0);
        // наклонные борта над гусеницами (от кромки крыши вниз-наружу к полке)
        float shelf = 1.12f;
        for (float s : S2) {
            float ang = 0.3f, hh = (y1 - shelf) / (float) Math.cos(ang), ww = (y1 - shelf) * (float) Math.tan(ang);
            C.extrude(new float[][]{{f - 1.62f, hh}, {f - 0.1f, hh * 0.38f}, {f - 0.1f, 0}, {r + 0.15f, 0}, {r + 0.15f, hh - 0.05f}, {r + 0.25f, hh}},
                    0.08f, WHITE, s * (w + ww), shelf, 0, 0, 0, s * ang);
            // ниша под бортом заполнена, чтобы сквозь щель не было видно неба
            C.box(ww + 0.05f, hh * 0.6f, L - 0.3f, WHITE, s * (w + ww / 2f), shelf + hh * 0.3f, -0.05f);
            // полка над гусеницей и фальшборт с крепёжными уголками
            C.box(0.8f, 0.05f, L + 0.15f, WHITE, s * (w + 0.38f), shelf, -0.05f);
            C.box(0.035f, 0.18f, L + 0.1f, WHITE, s * (w + 0.77f), shelf + 0.06f, -0.05f);
            for (int i = 0; i < 7; i++) M.box(0.05f, 0.05f, 0.08f, GUN, s * (w + 0.78f), shelf - 0.04f, -L / 2 + 0.4f + i * (L - 0.8f) / 6f);
            // передние брызговики
            C.extrude(new float[][]{{0, 0}, {0.55f, -0.35f}, {0.6f, -0.32f}, {0.06f, 0.04f}}, 0.66f, WHITE, s * (w + 0.38f), shelf, -f + 0.25f, 0, PI, 0);
        }
        // шов нижнего лобового листа и буксирные крюки
        D.box(w * 2f + 0.02f, 0.04f, 0.04f, DARK, 0, y0 + h * 0.42f, -f - 0.01f);
        for (float s : S2) {
            M.box(0.1f, 0.18f, 0.22f, GUN, s * (w - 0.35f), y0 + h * 0.22f, -f + 0.05f);
            M.torus(0.09f, 0.03f, GUN, s * (w - 0.35f), y0 + h * 0.08f, -f - 0.02f, 0, 0, PI / 2);
            M.box(0.1f, 0.18f, 0.22f, GUN, s * (w - 0.35f), y0 + 0.4f, -r + 0.02f);
        }
        // кромка крыши (сварной шов) по периметру
        for (float s : S2) M.box(0.04f, 0.03f, L - 1.4f, GUN, s * (w - 0.02f), y1 + 0.01f, 0.65f);
    }

    static void pantsir(Parts P, Array<Vector3> barrels, Array<Vector3> noses) {
        MB C = P.g("root", "c"), D = P.g("root", "d"), M = P.g("root", "m"), Gl = P.g("root", "g"), L = P.g("root", "l"), R = P.g("root", "r");
        for (float s : S2) D.box(0.24f, 0.34f, 9.8f, OLIVE_D, s * 0.55f, 1.0f, 0);
        for (float z : new float[]{-3.5f, -2.1f, 1.5f, 2.9f}) {
            M.cyl(0.1f, 0.1f, 2.1f, 8, GUN, 0, 0.62f, z, 0, 0, PI / 2);
            D.box(0.5f, 0.42f, 0.5f, OLIVE_D, 0, 0.66f, z);
            for (float s : S2) wheel(P, "root", s * 1.17f, 0.62f, z, 0.62f, 0.46f, s);
        }
        M.box(0.14f, 0.14f, 6.2f, GUN, 0, 0.76f, -0.3f);
        for (float s : S2) {
            fender(C, -4.2f, -1.4f, s * 1.17f, 1.4f, 0.64f, WHITE);
            fender(C, 0.8f, 3.6f, s * 1.17f, 1.4f, 0.64f, WHITE);
            R.box(0.55f, 0.5f, 0.03f, RUBBER, s * 1.17f, 0.95f, 3.75f);
        }
        float cz = -3.85f;
        C.extrude(new float[][]{{-1.05f, 0}, {1.0f, 0}, {1.06f, 0.95f}, {0.96f, 1.06f}, {0.78f, 1.92f}, {0.68f, 2.02f}, {-1.05f, 2.02f}}, 2.5f, WHITE, 0, 1.3f, cz);
        for (float s : S2) {
            Gl.box(1.08f, 0.78f, 0.04f, GLASS, s * 0.58f, 2.79f, -4.745f, 0.206f, 0, 0);
            D.box(0.62f, 0.03f, 0.03f, DARK, s * 0.55f, 2.43f, -4.86f, 0, 0, s * 0.35f);
            Gl.box(0.04f, 0.6f, 0.92f, GLASS, s * 1.255f, 2.74f, -4.15f);
            D.box(0.03f, 1.75f, 0.03f, DARK, s * 1.255f, 2.25f, -3.55f);
            D.box(0.03f, 0.03f, 1.4f, DARK, s * 1.255f, 1.4f, -4.2f);
            M.box(0.05f, 0.05f, 0.22f, STEEL, s * 1.27f, 2.32f, -3.75f);
            L.box(0.36f, 0.22f, 0.05f, c("fff3d0"), s * 0.92f, 1.66f, -4.93f);
            D.box(0.46f, 0.3f, 0.04f, DARK, s * 0.92f, 1.66f, -4.91f);
            L.box(0.14f, 0.09f, 0.05f, c("ff9a2e"), s * 1.12f, 1.9f, -4.92f);
            rail(M, V(s * 1.25f, 2.9f, -4.5f), V(s * 1.62f, 2.95f, -4.6f));
            rail(M, V(s * 1.25f, 2.4f, -4.5f), V(s * 1.62f, 2.55f, -4.6f));
            D.box(0.08f, 0.42f, 0.26f, DARK, s * 1.66f, 2.72f, -4.6f);
            M.box(0.36f, 0.04f, 0.42f, STEEL, s * 1.3f, 1.0f, -4.1f); M.box(0.36f, 0.04f, 0.42f, STEEL, s * 1.3f, 0.62f, -4.1f);
            M.box(0.08f, 0.1f, 0.18f, GUN, s * 0.75f, 1.15f, -5.15f);
        }
        Gl.box(0.04f, 0.4f, 0.9f, GLASS, 0, 2.75f, -2.82f);
        D.box(1.55f, 0.6f, 0.04f, c("1d1f1a"), 0, 2.0f, -4.915f);
        for (int i = 0; i < 5; i++) M.box(1.5f, 0.04f, 0.05f, GUN, 0, 1.78f + i * 0.11f, -4.94f);
        D.box(2.62f, 0.34f, 0.3f, OLIVE_D, 0, 1.3f, -5.0f);
        C.box(2.3f, 0.05f, 0.34f, WHITE, 0, 3.37f, -4.68f, -0.15f, 0, 0);
        C.box(0.9f, 0.08f, 0.7f, WHITE, 0, 3.36f, -3.6f);
        M.cyl(0.012f, 0.02f, 1.4f, 4, DARK, 0.95f, 4.0f, -3.1f);
        M.cyl(0.08f, 0.08f, 1.6f, 8, GUN, -1.05f, 2.9f, -2.62f); D.box(0.22f, 0.9f, 0.06f, OLIVE_D, -1.05f, 2.8f, -2.5f);
        C.extrude(new float[][]{{-3.7f, 0}, {3.6f, 0}, {3.7f, 0.2f}, {3.7f, 1.45f}, {-3.4f, 1.45f}, {-3.7f, 1.1f}}, 2.65f, WHITE, 0, 1.48f, 1.0f);
        D.box(2.2f, 0.32f, 7.2f, OLIVE_D, 0, 1.32f, 1.0f);
        for (float s : S2) {
            D.box(0.03f, 0.55f, 1.25f, c("2a2d24"), s * 1.335f, 2.3f, -1.4f);
            for (int i = 0; i < 6; i++) M.box(0.05f, 0.03f, 1.2f, GUN, s * 1.35f, 2.07f + i * 0.09f, -1.4f);
            D.box(0.03f, 0.9f, 0.7f, c("31352a"), s * 1.33f, 2.2f, 2.6f);
            M.box(0.05f, 0.05f, 0.16f, STEEL, s * 1.35f, 2.25f, 2.4f);
            rail(M, V(s * 1.36f, 2.85f, -2.4f), V(s * 1.36f, 2.85f, 4.3f));
            for (float z : new float[]{-2.4f, 1.0f, 4.3f}) rail(M, V(s * 1.33f, 2.62f, z), V(s * 1.36f, 2.85f, z));
            for (float z : new float[]{-2.45f, 4.45f}) {
                M.cyl(0.08f, 0.08f, 0.7f, 8, STEEL, s * 1.42f, 1.15f, z);
                D.box(0.42f, 0.06f, 0.42f, OLIVE_D, s * 1.42f, 0.8f, z);
                D.box(0.26f, 0.42f, 0.26f, OLIVE_D, s * 1.42f, 1.62f, z);
            }
            L.box(0.05f, 0.08f, 0.12f, c("ff9a2e"), s * 1.34f, 1.6f, 0.0f);
            L.box(0.22f, 0.12f, 0.05f, c("d0201a"), s * 1.0f, 1.55f, 4.72f);
        }
        D.cyl(0.32f, 0.32f, 1.5f, 14, OLIVE_D, 1.05f, 1.0f, -0.35f, PI / 2, 0, 0);
        for (float z : new float[]{-0.8f, 0.1f}) M.cyl(0.335f, 0.335f, 0.05f, 14, GUN, 1.05f, 1.0f, z, PI / 2, 0, 0);
        C.box(0.55f, 0.6f, 1.3f, WHITE, -1.05f, 1.0f, -0.4f);
        for (float x : new float[]{-0.35f, 0.35f}) rail(M, V(x, 0.9f, 4.72f), V(x, 2.9f, 4.72f));
        for (int i = 0; i < 6; i++) rail(M, V(-0.35f, 1.05f + i * 0.32f, 4.72f), V(0.35f, 1.05f + i * 0.32f, 4.72f), 0.02f);
        M.cyl(0.012f, 0.02f, 2.6f, 4, DARK, 1.15f, 4.2f, 4.2f); M.cyl(0.012f, 0.02f, 2.2f, 4, DARK, -1.15f, 4.0f, 4.3f);
        MB TC = P.g("turret", "c"), TD = P.g("turret", "d"), TM = P.g("turret", "m"), TG = P.g("turret", "g");
        TM.cyl(1.42f, 1.42f, 0.12f, 28, GUN, 0, 0.06f, 0);
        TC.cyl(1.32f, 1.42f, 0.3f, 28, WHITE, 0, 0.25f, 0);
        TC.extrude(new float[][]{{-1.55f, 0}, {1.05f, 0}, {1.3f, 0.3f}, {1.15f, 1.1f}, {0.9f, 1.22f}, {-1.55f, 1.22f}}, 2.2f, WHITE, 0, 0.38f, 0.25f);
        TC.cyl(0.36f, 0.36f, 0.1f, 16, WHITE, -0.5f, 1.64f, 0.7f); TM.box(0.06f, 0.06f, 0.4f, STEEL, -0.5f, 1.7f, 0.7f);
        TC.box(0.3f, 0.22f, 0.35f, WHITE, 0.45f, 1.7f, -0.45f); TG.box(0.22f, 0.12f, 0.03f, GLASS, 0.45f, 1.72f, -0.64f);
        for (float s : S2) {
            TC.box(0.24f, 0.62f, 1.5f, WHITE, s * 1.2f, 0.95f, 0.75f);
            rail(TM, V(s * 0.95f, 1.7f, 1.6f), V(s * 0.95f, 1.7f, -0.2f));
        }
        TC.box(1.6f, 0.62f, 0.5f, WHITE, 0, 0.82f, 1.98f); TD.box(1.3f, 0.42f, 0.04f, c("23261f"), 0, 0.82f, 2.24f);
        for (int i = 0; i < 4; i++) TM.box(1.25f, 0.03f, 0.05f, GUN, 0, 0.67f + i * 0.1f, 2.26f);
        MB RC = P.g("radar", "c"), RD = P.g("radar", "d"), RM = P.g("radar", "m");
        RM.cyl(0.13f, 0.17f, 0.75f, 12, GUN, 0, 0.37f, 0);
        // tp(v) = Rx(0.25)*v + (0,1.2,0)
        float cA = MathUtils.cos(0.25f), sA = MathUtils.sin(0.25f);
        RC.box(2.4f, 1.25f, 0.2f, WHITE, 0, 1.2f, 0, 0.25f, 0, 0);
        RD.box(2.22f, 1.08f, 0.04f, c("2b3036"), 0, 1.2f + (-sA * -0.11f), cA * -0.11f, 0.25f, 0, 0);
        for (int i = 0; i < 7; i++) RM.box(0.025f, 1.04f, 0.03f, GUN, -0.93f + i * 0.31f, 1.2f + (-sA * -0.13f), cA * -0.13f, 0.25f, 0, 0);
        RC.box(0.5f, 0.35f, 0.3f, WHITE, 0, 0.75f, 0.25f);
        MB PC = P.g("pitch", "c"), PD = P.g("pitch", "d"), PM = P.g("pitch", "m"), PG = P.g("pitch", "g");
        PM.cyl(0.16f, 0.16f, 4.5f, 12, GUN, 0, 0, 0, 0, 0, PI / 2);
        float rx = PI / 2;
        PC.cyl(0.72f, 0.72f, 0.42f, 28, WHITE, 0, 0.32f, -0.75f, rx, 0, 0);
        PD.cyl(0.66f, 0.66f, 0.05f, 28, c("2a2f35"), 0, 0.32f, -0.98f, rx, 0, 0);
        PM.cyl(0.12f, 0.12f, 0.08f, 12, GUN, 0, 0.32f, -1.02f, rx, 0, 0);
        PC.box(0.55f, 0.42f, 0.62f, WHITE, 0, -0.36f, -0.85f); PG.box(0.32f, 0.22f, 0.03f, GLASS, 0, -0.34f, -1.17f);
        Array<Array<Vector3>> sideNoses = new Array<>();
        sideNoses.add(new Array<Vector3>()); sideNoses.add(new Array<Vector3>());
        for (int si = 0; si < 2; si++) {
            float s = si == 0 ? -1 : 1;
            float px = s * 1.45f;
            for (float row : new float[]{0.17f, -0.17f}) for (float col : new float[]{-0.32f, 0f, 0.32f}) {
                float x = px + col;
                PC.cyl(0.15f, 0.15f, 3.3f, 14, WHITE, x, 0.12f + row, -0.55f, rx, 0, 0);
                PD.cyl(0.165f, 0.165f, 0.1f, 14, OLIVE_D, x, 0.12f + row, -2.15f, rx, 0, 0);
                PD.cyl(0.165f, 0.165f, 0.1f, 14, OLIVE_D, x, 0.12f + row, 0.98f, rx, 0, 0);
                PD.cyl(0.13f, 0.13f, 0.02f, 14, c("1a1a1a"), x, 0.12f + row, -2.2f, rx, 0, 0);
                sideNoses.get(si).add(V(x, 0.12f + row, -2.2f));
            }
            for (float z : new float[]{-1.55f, 0.35f}) PC.box(1.12f, 0.76f, 0.12f, WHITE, px, 0.12f, z);
            PD.box(0.3f, 0.3f, 0.9f, OLIVE_D, s * 0.75f, 0, 0);
            float gx = s * 2.38f;
            PC.box(0.42f, 0.62f, 1.55f, WHITE, gx, -0.1f, -0.2f);
            PD.box(0.5f, 0.3f, 0.5f, OLIVE_D, s * 2.1f, -0.05f, 0.2f);
            PD.box(0.3f, 0.5f, 0.6f, OLIVE_D, gx, -0.55f, 0.3f);
            for (float dy : new float[]{-0.14f, 0.14f}) {
                float y = -0.1f + dy;
                PM.cyl(0.058f, 0.058f, 3.25f, 10, GUN, gx, y, -2.55f, rx, 0, 0);
                PM.cyl(0.095f, 0.095f, 0.95f, 12, GUN, gx, y, -1.35f, rx, 0, 0);
                PM.cyl(0.085f, 0.085f, 0.3f, 10, GUN, gx, y, -4.2f, rx, 0, 0);
                PD.box(0.2f, 0.025f, 0.08f, DARK, gx, y, -4.17f);
                barrels.add(V(gx, y, -4.4f));
            }
        }
        for (int i = 0; i < 6; i++) { noses.add(sideNoses.get(0).get(i)); noses.add(sideNoses.get(1).get(i)); }
    }

    static void shilka(Parts P, Array<Vector3> barrels, Array<Vector3> noses) {
        MB C = P.g("root", "c"), D = P.g("root", "d"), M = P.g("root", "m"), Gl = P.g("root", "g"), L = P.g("root", "l");
        tracks2(P, 7.0f, 1.45f);
        hullT(C, D, M, 7.0f, 0.75f, 2.18f, 1.08f);
        for (float s : S2) {
            C.box(0.5f, 0.35f, 1.2f, WHITE, s * 1.45f, 1.32f, 2.2f);
            L.box(0.2f, 0.2f, 0.06f, c("fff3d0"), s * 0.8f, 1.65f, -3.42f);
            rail(M, V(s * 0.65f, 1.55f, -3.5f), V(s * 0.95f, 1.55f, -3.5f)); rail(M, V(s * 0.65f, 1.8f, -3.45f), V(s * 0.95f, 1.8f, -3.45f));
            M.box(0.12f, 0.12f, 0.2f, GUN, s * 0.7f, 1.0f, -3.55f);
            L.box(0.14f, 0.1f, 0.05f, c("d0201a"), s * 0.95f, 1.8f, 3.42f);
        }
        D.box(1.6f, 0.06f, 1.3f, c("2a2d24"), 0, 2.17f, 2.4f);
        for (int i = 0; i < 8; i++) M.box(1.5f, 0.04f, 0.05f, GUN, 0, 2.2f, 1.85f + i * 0.15f);
        C.cyl(0.3f, 0.3f, 0.1f, 14, WHITE, -0.6f, 2.2f, -2.2f); Gl.box(0.4f, 0.1f, 0.04f, GLASS, -0.6f, 2.12f, -2.55f, -0.5f, 0, 0);
        D.box(1.0f, 0.25f, 0.5f, DARK, 0.8f, 2.25f, 2.8f); M.cyl(0.1f, 0.12f, 0.6f, 8, GUN, -1.0f, 2.4f, 3.0f);
        MB TC = P.g("turret", "c"), TM = P.g("turret", "m"), TG = P.g("turret", "g");
        TM.cyl(1.5f, 1.5f, 0.12f, 28, GUN, 0, 0.06f, 0);
        TC.cyl(1.5f, 1.6f, 0.4f, 28, WHITE, 0, 0.25f, 0);
        TC.extrude(new float[][]{{-1.9f, 0}, {1.5f, 0}, {2.0f, 0.5f}, {1.3f, 1.35f}, {-1.6f, 1.35f}, {-2.0f, 0.7f}}, 2.9f, WHITE, 0, 0.45f, 0.1f);
        for (float s : S2) TC.extrude(new float[][]{{-1.7f, 0}, {1.4f, 0}, {1.1f, 0.9f}, {-1.5f, 0.9f}}, 0.3f, WHITE, s * 1.5f, 0.95f, 0.1f, 0, 0, s * 0.3f);
        TG.box(0.5f, 0.3f, 0.04f, GLASS, 0.75f, 1.35f, -1.66f, 0.6f, 0, 0); TG.box(0.5f, 0.3f, 0.04f, GLASS, -0.75f, 1.35f, -1.66f, 0.6f, 0, 0);
        TC.cyl(0.33f, 0.33f, 0.1f, 16, WHITE, -0.7f, 1.85f, 0.6f); TC.cyl(0.33f, 0.33f, 0.1f, 16, WHITE, 0.7f, 1.85f, 0.9f);
        for (float s : S2) {
            TC.box(0.3f, 0.7f, 1.7f, WHITE, s * 1.55f, 1.0f, 0.7f);
            rail(TM, V(s * 1.2f, 1.88f, 1.9f), V(s * 1.2f, 1.88f, -0.5f));
        }
        TC.box(2.2f, 0.8f, 0.5f, WHITE, 0, 1.0f, 2.2f);
        MB RC = P.g("radar", "c"), RD = P.g("radar", "d"), RM = P.g("radar", "m");
        RM.cyl(0.1f, 0.1f, 0.6f, 8, GUN, 0, 0.25f, 0);
        RC.sph(0.85f, WHITE, 0, 0.85f, 0.2f, 1, 1, 0.35f, 18);
        RD.cyl(0.08f, 0.08f, 0.7f, 8, DARK, 0, 0.85f, -0.25f, PI / 2, 0, 0);
        MB PC = P.g("pitch", "c"), PM = P.g("pitch", "m"), PD = P.g("pitch", "d");
        for (float x : new float[]{-0.95f, 0.95f}) {
            PC.box(0.5f, 0.75f, 1.1f, WHITE, x, 0, 0);
            for (float dy : new float[]{-0.18f, 0.18f}) {
                PM.cyl(0.055f, 0.065f, 2.8f, 10, GUN, x, dy, -1.9f, PI / 2, 0, 0);
                PM.cyl(0.085f, 0.085f, 0.7f, 10, GUN, x, dy, -0.9f, PI / 2, 0, 0);
                PM.cyl(0.075f, 0.05f, 0.3f, 10, GUN, x, dy, -3.35f, PI / 2, 0, 0);
                barrels.add(V(x, dy, -3.55f));
            }
            PD.box(0.3f, 0.3f, 0.6f, OLIVE_D, x, -0.5f, 0.2f);
        }
    }

    static void tor(Parts P, Array<Vector3> barrels, Array<Vector3> noses) {
        MB C = P.g("root", "c"), D = P.g("root", "d"), M = P.g("root", "m"), L = P.g("root", "l");
        tracks2(P, 7.2f, 1.5f);
        hullT(C, D, M, 7.2f, 0.75f, 2.22f, 1.12f);
        for (float s : S2) {
            C.box(0.55f, 0.4f, 1.6f, WHITE, s * 1.52f, 1.35f, 2.0f);
            L.box(0.2f, 0.2f, 0.06f, c("fff3d0"), s * 0.85f, 1.7f, -3.52f);
            M.box(0.12f, 0.12f, 0.2f, GUN, s * 0.7f, 1.0f, -3.65f);
            L.box(0.14f, 0.1f, 0.05f, c("d0201a"), s * 1.0f, 1.85f, 3.52f);
        }
        D.box(1.8f, 0.06f, 1.2f, c("2a2d24"), 0, 2.22f, 2.6f);
        for (int i = 0; i < 7; i++) M.box(1.7f, 0.04f, 0.05f, GUN, 0, 2.25f, 2.1f + i * 0.16f);
        // ---- башня как у «Тора»: высокая бронированная коробка, сверху сзади — поисковая РЛС ----
        MB TC = P.g("turret", "c"), TD = P.g("turret", "d"), TM = P.g("turret", "m"), TG = P.g("turret", "g"), TL = P.g("turret", "l");
        TM.cyl(1.55f, 1.55f, 0.12f, 28, GUN, 0, 0.06f, 0);
        TC.cyl(1.5f, 1.62f, 0.32f, 28, WHITE, 0, 0.26f, 0);
        // корпус башни: профиль сбоку (вперёд +), передний скос под качающуюся часть
        TC.extrude(new float[][]{{1.2f, 0}, {-1.95f, 0}, {-2.05f, 0.25f}, {-2.05f, 1.55f}, {-1.85f, 1.7f}, {0.55f, 1.7f}, {0.75f, 1.35f}, {1.45f, 0.6f}, {1.45f, 0.25f}},
                2.7f, WHITE, 0, 0.42f, 0.2f);
        // навесные бронекоробки по бортам, лючки, поручни
        for (float s : S2) {
            TC.box(0.32f, 0.95f, 2.3f, WHITE, s * 1.48f, 0.98f, 1.05f);
            TD.box(0.02f, 0.75f, 2.0f, c("2f3528"), s * 1.645f, 0.98f, 1.05f);
            for (int i = 0; i < 3; i++) TM.box(0.04f, 0.04f, 0.04f, RIVET, s * 1.65f, 1.35f, 0.3f + i * 0.75f);
            rail(TM, V(s * 1.15f, 2.2f, 1.9f), V(s * 1.15f, 2.2f, 0.3f));
            for (float z : new float[]{1.9f, 0.3f}) rail(TM, V(s * 1.15f, 2.12f, z), V(s * 1.15f, 2.2f, z));
            // блоки отстрела дымовых гранат
            TD.box(0.3f, 0.3f, 0.5f, OLIVE_D, s * 1.45f, 1.62f, -0.55f);
            for (int i = 0; i < 3; i++) TM.cyl(0.06f, 0.06f, 0.3f, 8, GUN, s * 1.55f, 1.7f, -0.7f + i * 0.15f, 0.6f, 0, -s * 0.8f);
        }
        // крыша: люки, командирская башенка с приборами, ТВ-оптика
        hatch(TC, TM, V(-0.6f, 2.15f, 1.3f), 0.32f);
        TC.cyl(0.42f, 0.46f, 0.25f, 18, WHITE, 0.55f, 2.25f, 1.1f);
        for (int i = 0; i < 4; i++) { float a = -0.9f + i * 0.6f; TG.box(0.14f, 0.07f, 0.02f, GLASS, 0.55f + MathUtils.sin(a) * 0.44f, 2.3f, 1.1f - MathUtils.cos(a) * 0.44f, 0, a, 0); }
        TC.box(0.5f, 0.42f, 0.5f, WHITE, -0.95f, 2.28f, 0.0f); TG.box(0.34f, 0.24f, 0.02f, GLASS, -0.95f, 2.3f, -0.26f);
        // задняя стенка: электроника, люк, фильтровентиляция
        TD.box(1.8f, 0.6f, 0.04f, c("2b3026"), 0, 1.25f, 2.27f);
        for (int i = 0; i < 6; i++) TM.box(1.7f, 0.03f, 0.05f, GUN, 0, 1.0f + i * 0.1f, 2.3f);
        TC.cyl(0.25f, 0.25f, 0.3f, 14, WHITE, 1.0f, 1.6f, 2.32f, PI / 2, 0, 0);
        // ниши для вертикальных пусковых модулей — крышки в два ряда (ракеты стартуют из качалки)
        for (int i = 0; i < 4; i++) for (float s : S2) {
            TD.box(0.36f, 0.04f, 0.36f, c("3d4434"), s * 0.4f, 2.13f, 0.3f + i * 0.42f);
            TM.box(0.3f, 0.03f, 0.04f, GUN, s * 0.4f, 2.16f, 0.3f + i * 0.42f);
        }
        TL.box(0.12f, 0.08f, 0.08f, c("ff3020"), 1.2f, 2.2f, 2.1f);
        // ---- поисковая РЛС: изогнутое (параболическое) зеркало на кронштейне, облучатель ----
        MB RC = P.g("radar", "c"), RD = P.g("radar", "d"), RM = P.g("radar", "m");
        RM.cyl(0.16f, 0.2f, 0.75f, 12, GUN, 0, 0.38f, 0);
        RM.box(0.9f, 0.18f, 0.4f, GUN, 0, 0.78f, 0);
        int segs = 9;
        float W = 2.8f, Hh = 1.0f;
        for (int i = 0; i < segs; i++) {
            float t = (i + 0.5f) / segs - 0.5f;
            float y = 1.3f + t * Hh * 1.05f, z = -0.15f * (t * t) * 4f + 0.12f;
            float ang = t * 0.6f;
            RC.box(W, Hh / segs + 0.012f, 0.08f, WHITE, 0, y, z, ang, 0, 0);
            RD.box(W - 0.15f, Hh / segs - 0.02f, 0.02f, c("2a2f35"), 0, y, z - 0.05f, ang, 0, 0);
        }
        for (float s : S2) RC.box(0.08f, Hh + 0.1f, 0.3f, WHITE, s * (W / 2 + 0.03f), 1.3f, 0.05f);
        for (float s : S2) RM.seg(V(s * 0.9f, 0.85f, 0f), V(s * 1.35f, 1.3f - Hh / 2f, 0.1f), 0.04f, 0.04f, 6, GUN);
        RM.seg(V(0, 1.3f, -0.2f), V(0, 1.3f, -0.85f), 0.025f, 0.025f, 6, GUN);
        RD.box(0.4f, 0.16f, 0.16f, c("2a2f35"), 0, 1.3f, -0.9f);
        // ---- качающаяся часть: антенна сопровождения (фазированная решётка) и два блока по 4 ракеты ----
        MB PC = P.g("pitch", "c"), PD = P.g("pitch", "d"), PM = P.g("pitch", "m"), PG = P.g("pitch", "g");
        for (float s : S2) PM.cyl(0.14f, 0.14f, 0.4f, 10, GUN, s * 1.45f, 0, 0, 0, 0, PI / 2);
        // центральный блок антенны: восьмигранная рамка, «соты» решётки, оптика снизу
        PC.box(1.5f, 1.5f, 0.9f, WHITE, 0, 0.15f, -0.35f);
        PC.cyl(0.78f, 0.82f, 0.18f, 8, WHITE, 0, 0.18f, -0.85f, PI / 2, PI / 8, 0);
        PD.cyl(0.66f, 0.66f, 0.03f, 8, c("2b3036"), 0, 0.18f, -0.95f, PI / 2, PI / 8, 0);
        for (int i = 0; i < 7; i++) for (int j = 0; j < 7; j++) {
            float x = -0.42f + i * 0.14f, y = -0.24f + j * 0.14f;
            if (x * x + (y - 0.18f) * (y - 0.18f) + 0.18f * 0.18f > 0.62f * 0.62f) continue;
            PM.box(0.1f, 0.1f, 0.02f, c("3a4048"), x, y, -0.975f);
        }
        PC.box(0.6f, 0.36f, 0.4f, WHITE, 0, -0.75f, -0.75f); PG.box(0.42f, 0.22f, 0.02f, GLASS, 0, -0.75f, -0.96f);
        // пусковые блоки по бокам: 2×2 квадратных контейнера с крышками
        for (float s : S2) {
            float px = s * 1.05f;
            PC.box(0.7f, 1.4f, 1.6f, WHITE, px, 0.0f, -0.2f);
            for (int r = 0; r < 2; r++) for (int cc = 0; cc < 2; cc++) {
                float x = px + (cc - 0.5f) * 0.32f, y = 0.32f - r * 0.62f;
                PD.box(0.28f, 0.28f, 0.06f, OLIVE_D, x, y, -1.02f);
                PD.box(0.22f, 0.22f, 0.03f, c("1a1a1a"), x, y, -1.05f);
                PM.box(0.3f, 0.03f, 0.05f, GUN, x, y + 0.15f, -1.03f);
                noses.add(V(x, y, -1.0f));
            }
            for (int i = 0; i < 3; i++) PM.box(0.04f, 1.42f, 0.04f, GUN, px + s * 0.36f, 0, -0.85f + i * 0.5f);
        }
    }

    static void luch(Parts P, Array<Vector3> barrels, Array<Vector3> noses) {
        MB C = P.g("root", "c"), D = P.g("root", "d"), M = P.g("root", "m"), L = P.g("root", "l");
        tracks2(P, 7.0f, 1.5f);
        hullT(C, D, M, 7.0f, 0.75f, 2.12f, 1.12f);
        for (float s : S2) {
            C.box(0.6f, 0.55f, 1.4f, WHITE, s * 1.52f, 1.45f, 2.2f);
            L.box(0.2f, 0.18f, 0.06f, c("fff3d0"), s * 0.85f, 1.65f, -3.42f);
            L.box(0.14f, 0.1f, 0.05f, c("d0201a"), s * 1.0f, 1.8f, 3.42f);
            for (int i = 0; i < 4; i++) M.box(0.05f, 0.42f, 1.2f, GUN, s * (1.25f + i * 0.12f), 1.5f, 2.2f);
        }
        D.box(1.9f, 0.06f, 1.4f, c("2a2d24"), 0, 2.12f, 2.5f);
        MB TC = P.g("turret", "c"), TD = P.g("turret", "d"), TM = P.g("turret", "m"), TL = P.g("turret", "l");
        TM.cyl(1.5f, 1.5f, 0.12f, 28, GUN, 0, 0.06f, 0);
        TC.cyl(1.45f, 1.65f, 0.45f, 28, WHITE, 0, 0.27f, 0);
        TC.extrude(new float[][]{{-1.4f, 0}, {1.3f, 0}, {1.7f, 0.42f}, {1.1f, 0.85f}, {-1.2f, 0.85f}, {-1.5f, 0.45f}}, 2.4f, WHITE, 0, 0.45f, 0.4f);
        for (float s : S2) {
            TD.cyl(0.32f, 0.32f, 1.6f, 16, c("3a4048"), s * 0.75f, 1.0f, 1.5f, PI / 2, 0, 0);
            for (int i = 0; i < 3; i++) TM.cyl(0.34f, 0.34f, 0.06f, 16, GUN, s * 0.75f, 1.0f, 0.9f + i * 0.6f, PI / 2, 0, 0);
            TL.box(0.08f, 0.08f, 0.5f, c("6ff2ff"), s * 0.75f, 1.34f, 1.5f);
        }
        MB RC = P.g("radar", "c"), RM = P.g("radar", "m"), RD = P.g("radar", "d");
        RM.cyl(0.1f, 0.1f, 0.6f, 10, GUN, 0, 0.3f, 0);
        RC.cyl(0.75f, 0.55f, 0.12f, 20, WHITE, 0, 0.7f, 0, PI / 2.6f, 0, 0);
        RD.cyl(0.6f, 0.6f, 0.02f, 20, c("2a2e33"), 0, 0.72f, -0.07f, PI / 2.6f, 0, 0);
        MB PC = P.g("pitch", "c"), PD = P.g("pitch", "d"), PM = P.g("pitch", "m"), PL = P.g("pitch", "l");
        PC.box(1.5f, 1.1f, 1.6f, WHITE, 0, 0, 0.3f);
        PD.cyl(0.55f, 0.62f, 2.6f, 24, c("454b52"), 0, 0, -1.4f, PI / 2, 0, 0);
        for (int i = 0; i < 7; i++) PM.cyl(0.78f, 0.78f, 0.07f, 24, GUN, 0, 0, -0.5f - i * 0.3f, PI / 2, 0, 0);
        PM.cyl(0.66f, 0.5f, 0.35f, 24, GUN, 0, 0, -2.8f, PI / 2, 0, 0);
        PL.cyl(0.42f, 0.42f, 0.05f, 24, c("8ff6ff"), 0, 0, -2.98f, PI / 2, 0, 0);
        PL.cyl(0.62f, 0.62f, 0.04f, 24, c("2fb8d8"), 0, 0, -2.66f, PI / 2, 0, 0);
        for (float s : S2) {
            PC.box(0.25f, 0.7f, 1.3f, WHITE, s * 0.85f, 0, 0.3f);
            PM.cyl(0.14f, 0.14f, 0.4f, 10, GUN, s * 1.0f, 0, 0, 0, 0, PI / 2);
        }
        barrels.add(V(0, 0, -3.05f));
    }

    static void grom(Parts P, Array<Vector3> barrels, Array<Vector3> noses) {
        MB C = P.g("root", "c"), D = P.g("root", "d"), M = P.g("root", "m"), L = P.g("root", "l"), Gl = P.g("root", "g");
        for (float s : S2) D.box(0.24f, 0.34f, 8.2f, OLIVE_D, s * 0.55f, 0.9f, 0);
        for (float z : new float[]{-2.7f, -0.5f, 2.3f}) {
            M.cyl(0.1f, 0.1f, 2.2f, 8, GUN, 0, 0.66f, z, 0, 0, PI / 2);
            for (float s : S2) wheel(P, "root", s * 1.28f, 0.68f, z, 0.68f, 0.52f, s);
        }
        C.extrude(new float[][]{{-4.2f, 0}, {3.7f, 0}, {4.5f, 0.55f}, {3.3f, 1.55f}, {-3.7f, 1.6f}, {-4.4f, 0.85f}}, 2.4f, WHITE, 0, 0.95f, 0);
        for (float s : S2) {
            C.extrude(new float[][]{{-4.0f, 0}, {3.6f, 0}, {3.9f, 0.35f}, {-3.8f, 0.4f}}, 0.55f, WHITE, s * 1.35f, 1.55f, 0, 0, 0, -s * 0.35f);
            for (float z : new float[]{-2.7f, -0.5f, 2.3f}) fender(C, z - 0.85f, z + 0.85f, s * 1.32f, 1.5f, 0.62f, WHITE);
        }
        for (float x : new float[]{-0.55f, 0.55f}) Gl.box(0.9f, 0.5f, 0.04f, GLASS, x, 2.05f, -3.83f, -0.72f, 0, 0);
        D.box(2.2f, 0.08f, 0.1f, DARK, 0, 2.36f, -3.6f, -0.72f, 0, 0);
        for (float s : S2) {
            Gl.box(0.03f, 0.4f, 0.8f, GLASS, s * 1.205f, 2.15f, -3.0f);
            L.box(0.14f, 0.1f, 0.05f, c("d0201a"), s * 0.95f, 1.9f, 4.18f);
        }
        MB TC = P.g("turret", "c"), TD = P.g("turret", "d"), TM = P.g("turret", "m"), TG = P.g("turret", "g");
        TM.cyl(1.5f, 1.5f, 0.12f, 32, GUN, 0, 0.06f, 0);
        TC.cyl(1.45f, 1.6f, 0.3f, 32, WHITE, 0, 0.2f, 0.1f);
        TC.extrude(new float[][]{{-1.7f, 0}, {1.5f, 0}, {2.1f, 0.5f}, {1.55f, 1.15f}, {-1.3f, 1.2f}, {-1.8f, 0.6f}}, 2.7f, WHITE, 0, 0.32f, 0.3f);
        TC.box(0.55f, 0.5f, 0.75f, WHITE, -0.95f, 1.7f, 0.6f); TG.box(0.4f, 0.22f, 0.03f, GLASS, -0.95f, 1.75f, 0.22f);
        TD.cyl(0.3f, 0.3f, 0.9f, 16, c("2f342c"), 1.15f, 1.55f, 0.7f, 0, 0, PI / 2);
        TD.box(1.0f, 0.5f, 1.2f, OLIVE_D, 0, 0.95f, 2.05f);
        for (int i = 0; i < 5; i++) TM.box(1.0f, 0.03f, 0.05f, GUN, 0, 1.21f, 1.6f + i * 0.2f);
        MB RC = P.g("radar", "c"), RM = P.g("radar", "m"), RD = P.g("radar", "d");
        RM.cyl(0.1f, 0.1f, 0.6f, 10, GUN, 0, 0.3f, 0);
        RC.box(1.6f, 0.6f, 0.14f, WHITE, 0, 0.8f, 0); RD.box(1.45f, 0.48f, 0.04f, c("2a2e33"), 0, 0.8f, -0.09f);
        MB PC = P.g("pitch", "c"), PD = P.g("pitch", "d"), PM = P.g("pitch", "m");
        PC.cyl(0.62f, 0.62f, 1.9f, 24, WHITE, 0, 0, 0.1f, 0, 0, PI / 2);
        PC.extrude(new float[][]{{-0.6f, -0.55f}, {0.7f, -0.45f}, {0.9f, 0f}, {0.7f, 0.45f}, {-0.6f, 0.55f}}, 1.5f, WHITE, 0, 0, -0.5f);
        for (float x : new float[]{-0.45f, 0.45f}) {
            PD.cyl(0.2f, 0.24f, 1.2f, 16, c("3a3f36"), x, 0.05f, -1.4f, PI / 2, 0, 0);
            PM.cyl(0.11f, 0.11f, 3.6f, 16, GUN, x, 0.05f, -3.4f, PI / 2, 0, 0);
            for (int k = 0; k < 4; k++) PM.cyl(0.125f, 0.125f, 0.06f, 14, GUN, x, 0.05f, -2.2f - k * 0.6f, PI / 2, 0, 0);
            PM.cyl(0.17f, 0.17f, 0.5f, 16, GUN, x, 0.05f, -5.2f, PI / 2, 0, 0);
            for (int k = 0; k < 3; k++) PM.box(0.36f, 0.05f, 0.08f, c("1a1a1a"), x, 0.05f, -5.05f - k * 0.14f);
            barrels.add(V(x, 0.05f, -5.5f));
        }
    }

    // ---- детализация ----
    static void bolts(MB M, Vector3 a, Vector3 b, int n, float ax, float ay, float az, float r) {
        for (int i = 0; i < n; i++) {
            float k = (float) i / (float) Math.max(1, n - 1);
            M.cyl(r, r, 0.04f, 6, RIVET, MathUtils.lerp(a.x, b.x, k), MathUtils.lerp(a.y, b.y, k), MathUtils.lerp(a.z, b.z, k), ax, ay, az);
        }
    }

    static void hatch(MB C, MB M, Vector3 p, float r) {
        C.cyl(r, r * 1.04f, 0.09f, 18, WHITE, p.x, p.y, p.z);
        M.cyl(r * 1.1f, r * 1.1f, 0.05f, 18, GUN, p.x, p.y - 0.03f, p.z);
        rail(M, V(p.x - r * 0.4f, p.y + 0.09f, p.z), V(p.x + r * 0.4f, p.y + 0.09f, p.z));
        M.box(0.1f, 0.07f, 0.14f, GUN, p.x, p.y + 0.03f, p.z + r * 0.95f);
    }

    static void grille(MB M, MB D, Vector3 c, float w, float d, int n) {
        D.box(w + 0.08f, 0.04f, d + 0.08f, DARK, c.x, c.y, c.z);
        for (int i = 0; i < n; i++) M.box(w, 0.04f, 0.045f, GUN, c.x, c.y + 0.035f, c.z - d / 2 + (i + 0.5f) * d / n, 0.5f, 0, 0);
    }

    static void smoke(MB M, MB D, Vector3 pos, float side, int n) {
        D.box(0.36f, 0.22f, 0.42f, OLIVE_D, pos.x, pos.y, pos.z);
        for (int i = 0; i < n; i++) {
            float px = pos.x + side * 0.02f, py = pos.y + 0.14f + (i % 2) * 0.05f, pz = pos.z - 0.12f + i * 0.12f;
            M.cyl(0.055f, 0.055f, 0.34f, 8, GUN, px + side * 0.1f, py + 0.06f, pz - 0.08f, -0.7f, 0, -side * 0.6f);
            M.cyl(0.035f, 0.035f, 0.35f, 6, c("111111"), px + side * 0.11f, py + 0.07f, pz - 0.09f, -0.7f, 0, -side * 0.6f);
        }
    }

    static void antenna(MB M, Vector3 pos, float h, Vector3 tilt) {
        M.cyl(0.07f, 0.09f, 0.14f, 8, GUN, pos.x, pos.y, pos.z);
        M.cyl(0.045f, 0.045f, 0.22f, 6, c("222222"), pos.x, pos.y + 0.17f, pos.z);
        Vector3 top = V(pos.x, pos.y + h, pos.z).add(tilt);
        rail(M, V(pos.x, pos.y + 0.27f, pos.z), top, 0.014f);
        M.sph(0.03f, c("222222"), top);
    }

    static void toolbox(MB D, MB M, Vector3 p, Vector3 s) {
        D.box(s.x, s.y, s.z, OLIVE_D, p.x, p.y, p.z);
        D.box(s.x + 0.03f, 0.04f, s.z + 0.03f, c("343a2a"), p.x, p.y + s.y * 0.32f, p.z);
        for (float k : new float[]{-0.3f, 0.3f}) M.box(0.06f, 0.08f, 0.04f, STEEL, p.x + s.x * k, p.y + s.y * 0.15f, p.z - s.z / 2 - 0.02f);
    }

    static void jerry(MB D, MB M, Vector3 p) {
        D.box(0.16f, 0.46f, 0.34f, c("4a5236"), p.x, p.y, p.z);
        D.box(0.17f, 0.05f, 0.3f, c("3c4430"), p.x, p.y + 0.08f, p.z);
        M.box(0.06f, 0.06f, 0.12f, STEEL, p.x, p.y + 0.26f, p.z - 0.08f);
    }

    static void periscope(MB M, MB Gl, Vector3 p) {
        M.box(0.2f, 0.14f, 0.16f, GUN, p.x, p.y, p.z);
        Gl.box(0.16f, 0.08f, 0.02f, GLASS, p.x, p.y, p.z - 0.085f);
    }

    static void lampGuard(MB L, MB M, Vector3 p) {
        M.cyl(0.13f, 0.13f, 0.12f, 12, GUN, p.x, p.y, p.z + 0.05f, PI / 2, 0, 0);
        L.cyl(0.1f, 0.1f, 0.03f, 12, c("fff3d0"), p.x, p.y, p.z - 0.02f, PI / 2, 0, 0);
        for (float dx : new float[]{-0.07f, 0f, 0.07f}) rail(M, V(p.x + dx, p.y + 0.14f, p.z - 0.06f), V(p.x + dx, p.y - 0.14f, p.z - 0.06f), 0.012f);
    }

    static void cable(MB M, Vector3 a, Vector3 b) {
        rail(M, a, b, 0.035f);
        M.cyl(0.09f, 0.09f, 0.05f, 10, GUN, a.x, a.y, a.z, 0, 0, PI / 2); M.cyl(0.09f, 0.09f, 0.05f, 10, GUN, b.x, b.y, b.z, 0, 0, PI / 2);
        for (float k : new float[]{0.25f, 0.5f, 0.75f}) M.box(0.05f, 0.1f, 0.06f, STEEL, a.cpy().lerp(b, k));
    }

    static void exhaust(MB M, Vector3 p) {
        M.cyl(0.11f, 0.12f, 0.5f, 10, c("2b2b2b"), p.x, p.y, p.z, PI / 2, 0, 0);
        M.cyl(0.13f, 0.13f, 0.08f, 10, c("4a3a2e"), p.x, p.y, p.z + 0.25f, PI / 2, 0, 0);
    }

    static final Map<Character, String> SEG = new HashMap<>();

    static {
        SEG.put('0', "abcdef"); SEG.put('1', "bc"); SEG.put('2', "abged"); SEG.put('3', "abgcd"); SEG.put('4', "fgbc");
        SEG.put('5', "afgcd"); SEG.put('6', "afgedc"); SEG.put('7', "abc"); SEG.put('8', "abcdefg"); SEG.put('9', "abcdfg");
    }

    static void number(MB D, String txt, Vector3 pos, float side, float h) {
        float w = h * 0.55f, t = h * 0.14f;
        for (int i = 0; i < txt.length(); i++) {
            float ox = pos.x, oy = pos.y, oz = pos.z - side * (i - (txt.length() - 1) / 2f) * (w + t * 1.6f);
            String sg = SEG.get(txt.charAt(i));
            if (sg == null) continue;
            for (int k = 0; k < sg.length(); k++) {
                char ch = sg.charAt(k);
                float qx, qy; boolean hor;
                switch (ch) {
                    case 'a': qx = 0; qy = h / 2; hor = true; break;
                    case 'g': qx = 0; qy = 0; hor = true; break;
                    case 'd': qx = 0; qy = -h / 2; hor = true; break;
                    case 'f': qx = -1; qy = h / 4; hor = false; break;
                    case 'b': qx = 1; qy = h / 4; hor = false; break;
                    case 'e': qx = -1; qy = -h / 4; hor = false; break;
                    default: qx = 1; qy = -h / 4; hor = false; break;
                }
                if (hor) D.box(0.012f, t, w, MARK, ox + side * 0.006f, oy + qy, oz);
                else D.box(0.012f, h / 2, t, MARK, ox + side * 0.006f, oy + qy, oz + side * -qx * w / 2);
            }
        }
    }

    static void sensor(MB D, MB Gl, Vector3 p, float r) {
        D.sph(r, c("2c3029"), p.x, p.y, p.z, 1, 1, 1, 12);
        Gl.cyl(r * 0.45f, r * 0.45f, 0.04f, 12, c("2a3a56"), p.x, p.y, p.z - r * 0.95f, PI / 2, 0, 0);
        D.box(r * 1.6f, 0.06f, 0.08f, DARK, p.x, p.y - r * 0.2f, p.z - r * 0.7f);
    }

    static void reflectors(MB L, Vector3 p, Color col) { L.box(0.1f, 0.06f, 0.02f, col, p.x, p.y, p.z); }

    static void skirt(MB C, MB M, float x, float z0, float z1, float y, float side) {
        int n = 5; float L = (z1 - z0) / n;
        for (int i = 0; i < n; i++) {
            float zc = z0 + (i + 0.5f) * L;
            C.box(0.035f, 0.5f, L - 0.05f, WHITE, x, y, zc);
            bolts(M, V(x + side * 0.02f, y + 0.17f, zc - L * 0.35f), V(x + side * 0.02f, y + 0.17f, zc + L * 0.35f), 3, 0, 0, PI / 2, 0.03f);
        }
        M.box(0.04f, 0.06f, z1 - z0, GUN, x, y + 0.26f, (z0 + z1) / 2);
    }

    static void detail(String kind, Parts P) {
        MB C = P.g("root", "c"), D = P.g("root", "d"), M = P.g("root", "m"), L = P.g("root", "l"), Gl = P.g("root", "g");
        MB TC = P.g("turret", "c"), TD = P.g("turret", "d"), TM = P.g("turret", "m"), TG = P.g("turret", "g");
        switch (kind) {
            case "shilka":
                for (float s : S2) {
                    skirt(C, M, s * 1.87f, -3.2f, 3.1f, 0.82f, s);
                    toolbox(D, M, V(s * 1.45f, 1.32f, -0.9f), V(0.62f, 0.32f, 1.1f));
                    toolbox(D, M, V(s * 1.45f, 1.3f, 0.6f), V(0.62f, 0.28f, 0.7f));
                    cable(M, V(s * 1.875f, 1.25f, -2.6f), V(s * 1.875f, 1.25f, 1.6f));
                    exhaust(M, V(s * 1.2f, 1.9f, 3.45f));
                    jerry(D, M, V(s * 1.2f, 2.45f, 3.25f));
                    reflectors(L, V(s * 1.5f, 1.45f, 3.43f), c("ff6a00"));
                    lampGuard(L, M, V(s * 0.8f, 1.65f, -3.45f));
                    number(D, "217", V(s * 1.89f, 1.25f, 0.2f), s, 0.3f);
                    smoke(TM, TD, V(s * 1.6f, 1.5f, -0.9f), s, 3);
                    antenna(TM, V(s * 1.0f, 1.82f, 2.2f), 2.4f, V(0, 0, 0.5f));
                    TC.box(0.06f, 0.55f, 1.5f, WHITE, s * 1.73f, 1.0f, 0.7f);
                    bolts(TM, V(s * 1.76f, 1.25f, 0.1f), V(s * 1.76f, 1.25f, 1.3f), 5, 0, 0, PI / 2, 0.035f);
                }
                for (int i = 0; i < 3; i++) periscope(M, Gl, V(-0.6f + i * 0.55f, 2.25f, -2.9f));
                hatch(C, M, V(0.7f, 2.18f, -2.2f), 0.28f);
                grille(M, D, V(0, 2.18f, 1.3f), 1.4f, 0.7f, 6);
                sensor(TD, TG, V(-1.2f, 1.95f, -0.4f), 0.22f);
                bolts(TM, V(-1.2f, 1.82f, -1.6f), V(1.2f, 1.82f, -1.6f), 9, 0, 0, 0, 0.035f);
                break;
            case "tor":
                for (float s : S2) {
                    skirt(C, M, s * 1.93f, -3.3f, 3.2f, 0.82f, s);
                    toolbox(D, M, V(s * 1.5f, 1.32f, -1.2f), V(0.62f, 0.32f, 1.2f));
                    cable(M, V(s * 1.935f, 1.25f, -2.8f), V(s * 1.935f, 1.25f, 1.8f));
                    exhaust(M, V(s * 1.25f, 1.95f, 3.55f));
                    jerry(D, M, V(s * 1.55f, 1.42f, 0.6f));
                    lampGuard(L, M, V(s * 0.85f, 1.7f, -3.55f));
                    reflectors(L, V(s * 1.5f, 1.5f, 3.53f), c("ff6a00"));
                    number(D, "304", V(s * 1.96f, 1.2f, 0f), s, 0.3f);
                    antenna(TM, V(s * 1.15f, 2.13f, 1.95f), 2.6f, V(0, 0, 0.4f));
                    bolts(TM, V(s * 1.46f, 0.75f, -1.2f), V(s * 1.46f, 0.75f, 1.4f), 7, 0, 0, PI / 2, 0.035f);
                }
                for (int i = 0; i < 3; i++) periscope(M, Gl, V(-0.6f + i * 0.55f, 2.25f, -3.0f));
                hatch(C, M, V(-0.7f, 2.22f, -2.3f), 0.3f);
                grille(M, D, V(0, 2.24f, 1.2f), 1.5f, 0.9f, 7);
                break;
            case "luch": {
                for (float s : S2) {
                    skirt(C, M, s * 1.83f, -3.1f, 3.0f, 0.82f, s);
                    toolbox(D, M, V(s * 1.5f, 1.32f, -1.0f), V(0.62f, 0.32f, 1.3f));
                    cable(M, V(s * 1.835f, 1.25f, -2.6f), V(s * 1.835f, 1.25f, 1.4f));
                    lampGuard(L, M, V(s * 0.85f, 1.65f, -3.45f));
                    reflectors(L, V(s * 1.5f, 1.45f, 3.43f), c("ff6a00"));
                    number(D, "51", V(s * 1.85f, 1.2f, 0f), s, 0.3f);
                    smoke(TM, TD, V(s * 1.3f, 0.85f, -0.8f), s, 3);
                    rail(TM, V(s * 0.75f, 1.0f, 0.7f), V(s * 0.6f, 1.25f, -0.3f), 0.05f);
                    for (int k = 0; k < 4; k++) TD.box(0.04f, 0.1f, 0.22f, k % 2 == 0 ? HAZ : c("1a1a1a"), s * 1.31f, 0.75f, -0.9f + k * 0.22f);
                    antenna(TM, V(s * 1.0f, 1.25f, 1.9f), 2.2f, V(0, 0, 0));
                }
                for (int i = 0; i < 3; i++) periscope(M, Gl, V(-0.6f + i * 0.55f, 2.15f, -2.9f));
                hatch(C, M, V(0.7f, 2.12f, -2.2f), 0.28f);
                grille(M, D, V(0, 2.15f, 1.2f), 1.5f, 0.8f, 7);
                sensor(TD, TG, V(-0.9f, 1.55f, -0.7f), 0.22f);
                MB PD = P.g("pitch", "d"), PL = P.g("pitch", "l");
                for (int k = 0; k < 6; k++) PD.box(1.52f, 0.06f, 0.1f, k % 2 == 0 ? HAZ : c("1a1a1a"), 0, 0.56f, -0.1f + k * 0.1f);
                for (int i = 0; i < 8; i++) {
                    float a = i * MathUtils.PI2 / 8;
                    PL.box(0.05f, 0.05f, 0.05f, c("6ff2ff"), MathUtils.cos(a) * 0.5f, MathUtils.sin(a) * 0.5f, -2.95f);
                }
                break;
            }
            case "grom": {
                for (float s : S2) {
                    toolbox(D, M, V(s * 0.85f, 2.7f, 2.2f), V(0.5f, 0.3f, 1.3f));
                    cable(M, V(s * 1.5f, 1.75f, -1.8f), V(s * 1.5f, 1.75f, 2.8f));
                    exhaust(M, V(s * 0.8f, 2.3f, 4.15f));
                    lampGuard(L, M, V(s * 0.85f, 1.6f, -4.4f));
                    reflectors(L, V(s * 1.1f, 1.3f, 4.18f), c("ff6a00"));
                    number(D, "88", V(s * 1.66f, 1.75f, 0.8f), s, 0.34f);
                    rail(M, V(s * 1.0f, 2.55f, -3.2f), V(s * 1.45f, 2.65f, -3.25f), 0.02f); D.box(0.06f, 0.3f, 0.2f, DARK, s * 1.5f, 2.6f, -3.25f);
                    TD.box(0.38f, 0.6f, 1.4f, OLIVE_D, s * 1.62f, 0.75f, 0.6f);
                    for (int k = 0; k < 3; k++) TM.box(0.4f, 0.04f, 0.04f, STEEL, s * 1.62f, 0.6f + k * 0.15f, -0.11f);
                    smoke(TM, TD, V(s * 1.5f, 1.2f, -1.1f), s, 3);
                    antenna(TM, V(s * 1.2f, 1.52f, 1.8f), 2.5f, V(0, 0, 0.5f));
                }
                L.box(0.18f, 0.1f, 0.18f, AMBER, -0.6f, 2.6f, -2.8f); L.box(0.18f, 0.1f, 0.18f, AMBER, 0.6f, 2.6f, -2.8f);
                MB R = P.g("root", "r");
                R.cyl(0.62f, 0.62f, 0.42f, 22, RUBBER, 0, 1.9f, 4.38f, PI / 2, 0, 0); D.cyl(0.38f, 0.38f, 0.44f, 16, OLIVE_D, 0, 1.9f, 4.38f, PI / 2, 0, 0);
                hatch(C, M, V(-0.6f, 2.58f, -2.2f), 0.3f); hatch(C, M, V(0.6f, 2.58f, -2.2f), 0.3f);
                grille(M, D, V(0, 2.57f, 3.0f), 1.6f, 1.0f, 8);
                for (int i = 0; i < 3; i++) periscope(M, Gl, V(-0.6f + i * 0.6f, 2.62f, -2.65f));
                sensor(TD, TG, V(-1.05f, 2.15f, 0.2f), 0.2f);
                break;
            }
            case "pantsir":
                for (float s : S2) {
                    number(D, "112", V(s * 1.31f, 2.3f, -3.2f), s, 0.28f);
                    smoke(TM, TD, V(s * 1.55f, 0.9f, -0.9f), s, 3);
                    reflectors(L, V(s * 1.2f, 1.3f, 4.7f), c("ff6a00"));
                }
                break;
        }
    }

    /** Модель боевой машины. */
    public static final class Vehicle {
        public Node3 root, turret, pitch, radar;
        public final Array<Vector3> barrels = new Array<>(), noses = new Array<>();
        public final Array<Node3> noseMeshes = new Array<>();
        public boolean broken;
        public final Array<Object[]> wreck = new Array<>();
    }

    static Mesh3 noseMesh;

    static final Map<String, Vehicle> vehCache = new HashMap<>();

    /** Экземпляр машины: геометрия строится один раз на пару «машина+окраска». */
    public static Vehicle vehicle(String kind, String skin) {
        String key = kind + "|" + skin;
        Vehicle t = vehCache.get(key);
        if (t == null) { t = buildVehicle(kind, skin); vehCache.put(key, t); }
        Vehicle v = new Vehicle();
        v.root = t.root.cloneTree();
        v.turret = v.root.find("Turret"); v.pitch = v.root.find("Pitch"); v.radar = v.root.find("Radar");
        v.barrels.addAll(t.barrels); v.noses.addAll(t.noses);
        for (Node3 k : v.pitch.kids) if (k.mesh == noseMesh && noseMesh != null) v.noseMeshes.add(k);
        return v;
    }

    static Vehicle buildVehicle(String kind, String skin) {
        MB.hq = true;
        Mat cm = camoMat(skin);
        Vehicle v = new Vehicle();
        Node3 root = new Node3(), turret = new Node3(), pitch = new Node3(), radar = new Node3();
        Map<String, Node3> nodes = new HashMap<>();
        nodes.put("root", root); nodes.put("turret", turret); nodes.put("pitch", pitch); nodes.put("radar", radar);
        Parts P = new Parts();
        float ty, tz, noseOff = -0.2f;
        switch (kind) {
            case "pantsir": pantsir(P, v.barrels, v.noses); ty = 3.0f; tz = 0.6f; radar.pos.set(0, 1.6f, 0.85f); pitch.pos.set(0, 0.95f, -0.55f); noseOff = 0; break;
            case "shilka": shilka(P, v.barrels, v.noses); ty = 2.15f; tz = 0.3f; radar.pos.set(0, 1.85f, 1.3f); pitch.pos.set(0, 1.0f, -0.9f); break;
            case "grom": grom(P, v.barrels, v.noses); ty = 2.5f; tz = 0.4f; radar.pos.set(0, 1.5f, 1.2f); pitch.pos.set(0, 0.95f, -0.9f); break;
            case "luch": luch(P, v.barrels, v.noses); ty = 2.15f; tz = 0.3f; radar.pos.set(0, 1.0f, 1.9f); pitch.pos.set(0, 1.2f, -0.3f); break;
            default: tor(P, v.barrels, v.noses); ty = 2.2f; tz = 0.4f; radar.pos.set(0, 1.35f, 1.4f); pitch.pos.set(0, 1.35f, -0.2f); break;
        }
        detail(kind, P);
        Map<String, Mat> mats = new HashMap<>();
        mats.put("c", cm); mats.put("d", MAT_PAINT); mats.put("m", MAT_METAL); mats.put("r", MAT_RUBBER); mats.put("g", MAT_GLASS); mats.put("l", MAT_LAMP);
        // у башни, качалки и радара высота считается от их собственного основания, а не от земли —
        // с обычной «пылью снизу» они целиком серели; им — материалы с лёгкой пылью
        Map<String, Mat> upMats = new HashMap<>(mats);
        upMats.put("c", upMat(cm)); upMats.put("d", upMat(MAT_PAINT)); upMats.put("m", upMat(MAT_METAL));
        for (Map.Entry<String, Map<String, MB>> e : P.d.entrySet()) {
            Map<String, Mat> mm = e.getKey().equals("root") ? mats : upMats;
            for (Map.Entry<String, MB> k : e.getValue().entrySet()) {
                MB b = k.getValue();
                String key = k.getKey();
                b.noAo = key.equals("l") || key.equals("g");
                if (b.empty()) continue;
                Node3 mi = new Node3(b.mesh(), mm.get(key));
                mi.castShadow = key.equals("c") || key.equals("d") || key.equals("r") || key.equals("m");
                nodes.get(e.getKey()).add(mi);
            }
        }
        turret.pos.set(0, ty, tz);
        turret.name = "Turret"; pitch.name = "Pitch"; radar.name = "Radar";
        turret.add(radar); turret.add(pitch); root.add(turret);
        if (noseMesh == null) {
            MB nb = new MB();
            MB.hq = false;
            nb.cyl(0f, 0.14f, 0.36f, 12, Color.WHITE);
            noseMesh = nb.mesh();
            MB.hq = true;
        }
        for (Vector3 p : v.noses) {
            Node3 m = new Node3(noseMesh, MAT_NOSE);
            m.pos.set(p.x, p.y, p.z + noseOff);
            m.setEuler(-PI / 2, 0, 0);
            pitch.add(m);
            v.noseMeshes.add(m);
        }
        MB.hq = false;
        v.root = root; v.turret = turret; v.pitch = pitch; v.radar = radar;
        return v;
    }

    // ---------- враги ----------
    public static final class EnemyDef { public Mesh3 body, glow, rot; public float r, sc; }

    public static final class EnemyModel {
        public Node3 g, body, glow, rot;
        public float r;
        public final Array<Node3> legs = new Array<>();
    }

    static final Map<String, EnemyDef> enemyCache = new HashMap<>();
    static final Map<String, Mesh3> legCache = new HashMap<>();

    static boolean isBossType(String t) { return G.D.get("BOSS_INFO").has(t); }

    static void quadBody(MB b, MB gl, MB rot, Color A, int arms, Color body, float s) {
        b.box(0.95f * s, 0.38f * s, 1.05f * s, body);
        b.sph(0.38f * s, GREY, 0, 0.18f * s, 0, 1.1f, 0.5f, 1.3f);
        b.box(0.5f * s, 0.06f * s, 0.7f * s, A, 0, 0.22f * s, 0);
        for (int i = 0; i < arms; i++) {
            float a = (float) i / arms * MathUtils.PI2 + PI / arms;
            float x = MathUtils.cos(a) * 1.35f * s, z = MathUtils.sin(a) * 1.35f * s;
            b.box(1.35f * s, 0.09f * s, 0.14f * s, DGREY, x / 2, 0.02f, z / 2, 0, -a, 0);
            b.cyl(0.13f * s, 0.15f * s, 0.28f * s, 8, c("1b1b1b"), x, 0.1f * s, z);
            rot.disc(0.78f * s, Color.WHITE, x, 0.27f * s, z);
        }
        gl.sph(0.12f * s, new Color(1, 0.2f, 0.15f, 1), 0, -0.05f, -0.55f * s, 1, 1, 1, 6);
        gl.sph(0.12f * s, new Color(0.2f, 1, 0.4f, 1), 0, -0.05f, 0.55f * s, 1, 1, 1, 6);
    }

    static EnemyDef def(String type, String acc) {
        String key = type + acc;
        EnemyDef ed = enemyCache.get(key);
        if (ed != null) return ed;
        MB.hq = isBossType(type);
        Color A = acc.equals("manti") ? c("9b59b6") : c("c0392b");
        MB b = new MB(), gl = new MB(), rot = new MB();
        gl.noAo = true; rot.noAo = true;
        float r = 2.3f, sc = 1f;
        switch (type) {
            case "quad": case "decoy": quadBody(b, gl, rot, A, 4, type.equals("decoy") ? c("6b7078") : DGREY, 1f); r = 2.3f; sc = 1.35f; break;
            case "mini": quadBody(b, gl, rot, A, 4, DGREY, 0.7f); r = 1.5f; sc = 1.2f; break;
            case "stealth":
                b.extrude(new float[][]{{-1.5f, 0}, {0.4f, 1.6f}, {0.9f, 0}, {0.4f, -1.6f}}, 0.28f, c("1a1d24"), 0, 0, 0, 0, 0, PI / 2);
                b.sph(0.45f, c("252a33"), 0, 0.12f, -0.1f, 1f, 0.45f, 1.6f);
                for (float x : S2) rot.disc(0.5f, Color.WHITE, x * 0.95f, 0.12f, 0.35f);
                gl.sph(0.09f, new Color(0.55f, 0.4f, 1, 1), 0, -0.05f, -1.2f, 1, 1, 1, 6);
                r = 2.0f; sc = 1.5f; break;
            case "armor":
                b.sph(1.0f, c("4a5560"), 0, 0.15f, 0, 1.25f, 0.55f, 1.45f, 12);
                for (int i = 0; i < 6; i++) {
                    float a = i / 6f * MathUtils.PI2, x = MathUtils.cos(a) * 1.55f, z = MathUtils.sin(a) * 1.55f;
                    b.box(1.5f, 0.14f, 0.22f, c("3a4047"), x / 2, 0.05f, z / 2, 0, -a, 0);
                    b.cyl(0.62f, 0.62f, 0.22f, 14, c("5b6670"), x, 0.12f, z);
                    rot.disc(0.5f, Color.WHITE, x, 0.26f, z);
                }
                for (int k = 0; k < 5; k++) b.box(0.5f, 0.08f, 0.32f, c("6d7a85"), -0.55f + k * 0.27f, 0.68f, -0.2f + (k % 2) * 0.25f, 0.2f, 0, 0);
                gl.sph(0.14f, new Color(1, 0.55f, 0.1f, 1), 0, 0, -1.35f, 1, 1, 1, 6);
                r = 2.3f; sc = 1.6f; break;
            case "zig": quadBody(b, gl, rot, A, 3, c("3a3420"), 1f); b.box(0.6f, 0.07f, 0.6f, c("e0b030"), 0, 0.24f, 0); r = 2.2f; sc = 1.35f; break;
            case "split":
                quadBody(b, gl, rot, A, 4, DGREY, 1.25f);
                for (float x : new float[]{-0.45f, 0f, 0.45f}) b.box(0.32f, 0.3f, 0.32f, c("8a5a2a"), x, -0.5f, 0);
                r = 2.8f; sc = 1.4f; break;
            case "heavy": quadBody(b, gl, rot, A, 6, c("24272c"), 1.1f); b.box(0.7f, 0.45f, 1.3f, c("1c1c1c"), 0, -0.55f, 0); r = 4.3f; sc = 2.3f; break;
            case "relay":
                quadBody(b, gl, rot, A, 4, c("24302a"), 1.1f); b.cyl(0.04f, 0.04f, 1.6f, 6, c("999999"), 0, 1.0f, 0); gl.torus(0.5f, 0.06f, c("2ecc71"), 0, 1.5f, 0);
                r = 3.2f; sc = 1.6f; break;
            case "wing":
                b.wing(4.6f, 3.2f, 0.18f, LGREY);
                b.cyl(0.24f, 0.3f, 3.4f, 10, GREY, 0, 0.15f, 0, PI / 2, 0, 0);
                b.sph(0.26f, c("111111"), 0, 0.2f, -1.65f, 1, 0.8f, 1.4f);
                for (float x : new float[]{-0.8f, 0.8f}) b.box(0.06f, 0.7f, 0.8f, GREY, x, 0.45f, 1.1f, 0, 0, x * 0.15f);
                b.box(0.4f, 0.05f, 0.4f, A, 0, 0.3f, 0.2f);
                rot.disc(0.7f, Color.WHITE, 0, 0.15f, 1.75f, PI / 2, 0, 0);
                gl.sph(0.12f, new Color(1, 0.2f, 0.15f, 1), 2.2f, 0.05f, 0.3f); gl.sph(0.12f, new Color(0.2f, 1, 0.4f, 1), -2.2f, 0.05f, 0.3f);
                r = 2.8f; sc = 1.35f; break;
            case "kami":
                b.cyl(0.28f, 0.32f, 3.6f, 10, c("8b8f96"), 0, 0, 0, PI / 2, 0, 0);
                b.cyl(0f, 0.28f, 0.8f, 10, A, 0, 0, -2.2f, -PI / 2, 0, 0);
                b.wing(3.2f, 1.6f, 0.08f, c("6e737a"), 0, 0, 0.3f);
                b.box(0.05f, 0.6f, 0.6f, c("6e737a"), 0, 0.3f, 1.5f);
                gl.sph(0.2f, new Color(1, 0.55f, 0.2f, 1), 0, 0, 2.1f);
                r = 2.4f; sc = 1.3f; break;
            case "scout":
                b.cyl(0.3f, 0.22f, 3.8f, 10, c("dadde2"), 0, 0, 0, PI / 2, 0, 0);
                b.box(7.5f, 0.1f, 0.7f, c("dadde2"), 0, 0.1f, -0.2f); b.box(2.2f, 0.06f, 0.45f, c("dadde2"), 0, 0.1f, 1.7f); b.box(0.06f, 0.6f, 0.5f, c("dadde2"), 0, 0.4f, 1.7f);
                b.cyl(0.02f, 0.02f, 2.2f, 4, c("333333"), 0, 1.0f, -0.4f); gl.sph(0.14f, new Color(1, 0.9f, 0.2f, 1), 0, 2.1f, -0.4f);
                rot.disc(0.6f, Color.WHITE, 0, 0, 2.0f, PI / 2, 0, 0);
                r = 3.2f; sc = 1.4f; break;
            case "erk":
                b.cyl(0.13f, 0.13f, 1.6f, 8, c("c9c9c9"), 0, 0, 0, PI / 2, 0, 0);
                b.cyl(0f, 0.13f, 0.4f, 8, c("d05030"), 0, 0, -1.0f, -PI / 2, 0, 0);
                for (int i = 0; i < 4; i++) b.box(0.02f, 0.3f, 0.3f, c("777777"), MathUtils.cos(i * PI / 2) * 0.15f, MathUtils.sin(i * PI / 2) * 0.15f, 0.6f, 0, 0, i * PI / 2);
                gl.sph(0.16f, new Color(1, 0.6f, 0.25f, 1), 0, 0, 0.85f);
                r = 1.4f; sc = 1.5f; break;
            case "shell": b.sph(0.3f, c("333333")); gl.sph(0.2f, new Color(1, 0.35f, 0.2f, 1), 0, 0, 0); r = 1.3f; sc = 1.5f; break;
            case "node": b.sph(1.4f, c("2b2f3a"), 0, 0, 0, 1, 1, 1, 4); gl.torus(1.6f, 0.12f, A); r = 2.4f; break;
            case "boss":
                b.cyl(3.0f, 3.4f, 1.4f, 6, c("2a2d33")); b.cyl(2.2f, 3.0f, 0.8f, 6, c("3a3e45"), 0, 0.9f, 0);
                for (int i = 0; i < 6; i++) {
                    float a = i / 6f * MathUtils.PI2, x = MathUtils.cos(a) * 5f, z = MathUtils.sin(a) * 5f;
                    b.box(4.2f, 0.35f, 0.6f, c("24272c"), x / 2, 0.2f, z / 2, 0, -a, 0); b.cyl(0.5f, 0.6f, 0.8f, 8, c("1a1a1a"), x, 0.4f, z);
                    rot.disc(2.3f, Color.WHITE, x, 0.85f, z);
                }
                for (float x : new float[]{-1.8f, 1.8f}) b.box(0.9f, 0.7f, 2.6f, c("3a2a2a"), x, -0.9f, 0);
                gl.sph(0.6f, new Color(1, 0.15f, 0.1f, 1), 0, -0.7f, -3.0f);
                r = 7.0f; break;
            case "gunship": {
                Color GW = c("30343a"), GD = c("23262b");
                b.sph(2.2f, GW, 0, 0.2f, -0.6f, 0.95f, 0.85f, 2.0f, 24);
                b.sph(1.6f, GW, 0, -0.1f, -3.0f, 0.8f, 0.7f, 1.4f, 20);
                b.seg(V(0, 0.4f, 2.8f), V(0, 0.75f, 9.6f), 0.85f, 0.35f, 16, GW);
                b.extrude(new float[][]{{-0.2f, 0}, {1.6f, 0}, {2.4f, 2.4f}, {1.4f, 2.6f}, {0.4f, 0.9f}}, 0.16f, GD, 0, 0.7f, 9.0f);
                b.box(2.6f, 0.14f, 0.9f, GD, 0, 0.75f, 9.0f);
                b.extrude(new float[][]{{-1.1f, -0.15f}, {1.2f, -0.12f}, {1.3f, 0.12f}, {-1.1f, 0.15f}}, 6.6f, GD, 0, -0.25f, 0.4f);
                b.sph(0.9f, GD, 0, 1.75f, -0.2f, 1.3f, 0.6f, 2.2f, 18);
                b.cyl(0.25f, 0.25f, 1.0f, 12, c("222222"), 0, 2.3f, 0);
                b.cyl(0.55f, 0.55f, 0.3f, 16, GD, 0, 2.75f, 0);
                for (int k = 0; k < 4; k++) b.box(4.6f, 0.1f, 0.35f, GD, 0, 2.85f, 0, 0, k * PI / 4, 0);
                rot.disc(8.5f, Color.WHITE, 0, 2.95f, 0); rot.disc(1.4f, Color.WHITE, 0.3f, 1.9f, 9.9f, 0, 0, PI / 2);
                gl.sph(0.4f, new Color(1, 0.2f, 0.1f, 1), 0, -0.4f, -4.2f);
                r = 7.0f; break;
            }
            case "walker": {
                Color HW = c("3a3e44"), HD = c("2a2d32"), HL = c("4a4f56");
                b.sph(3.0f, HW, 0, 0.4f, 0.2f, 1.15f, 0.72f, 1.25f, 24);
                b.extrude(new float[][]{{-1.0f, -1.5f}, {3.0f, -1.3f}, {3.9f, 0}, {3.1f, 1.5f}, {-1.2f, 1.9f}}, 4.6f, HL, 0, 0.2f, 0);
                b.extrude(new float[][]{{-2.6f, -0.9f}, {1.8f, -0.9f}, {2.2f, 0.6f}, {-2.8f, 0.9f}}, 6.4f, HD, 0, -0.6f, 0.4f);
                b.sph(1.0f, BGL, 0, 0.95f, -3.55f, 1.7f, 0.38f, 0.55f, 20);
                gl.box(2.6f, 0.12f, 0.06f, new Color(1, 0.25f, 0.12f, 1), 0, 0.95f, -3.95f);
                for (float x : new float[]{-2.6f, 2.6f}) b.cyl(1.15f, 1.15f, 1.2f, 20, HD, x, -1.4f, 0.2f, 0, 0, PI / 2);
                b.cyl(0.95f, 0.95f, 2.6f, 20, HL, 3.7f, 0.9f, -0.4f, PI / 2, 0, 0);
                b.cyl(0.42f, 0.36f, 6.0f, 16, HD, 3.7f, 0.9f, -4.5f, PI / 2, 0, 0);
                b.cyl(0.55f, 0.55f, 0.9f, 16, HD, 3.7f, 0.9f, -7.3f, PI / 2, 0, 0);
                for (int k = 0; k < 3; k++) b.box(1.2f, 0.08f, 0.16f, BDK, 3.7f, 0.9f, -7.0f - k * 0.26f);
                b.box(2.0f, 1.6f, 2.6f, HL, -3.7f, 1.0f, -0.2f);
                for (int i = 0; i < 9; i++) gl.sph(0.16f, new Color(1, 0.4f, 0.15f, 1), -4.3f + (i % 3) * 0.6f, 0.5f + (i / 3) * 0.5f, -1.55f, 1, 1, 1, 6);
                b.cyl(1.25f, 1.35f, 2.6f, 22, HD, 0, 2.0f, 2.2f);
                for (int k = 0; k < 4; k++) b.torus(1.38f, 0.08f, HL, 0, 1.0f + k * 0.6f, 2.2f);
                r = 7.5f; sc = 2.6f; break;
            }
            case "carrier":
                b.sph(9, c("3a3530"), 0, 0, 0, 1, 0.75f, 2.2f, 14);
                b.box(5, 2.2f, 14, c("24211e"), 0, -6.4f, 0);
                for (int i = 0; i < 4; i++) gl.box(2.2f, 0.4f, 2.2f, new Color(0.9f, 0.55f, 0.15f, 1), 0, -7.6f, -5 + i * 3.4f);
                for (float x : new float[]{-7.5f, 7.5f}) {
                    b.box(0.4f, 6, 4, c("2c2a27"), x, 0, 16); b.cyl(1.2f, 1.2f, 3, 10, c("1a1a1a"), x * 1.25f, 0, 10, PI / 2, 0, 0);
                    rot.disc(3.0f, Color.WHITE, x * 1.25f, 0, 11.6f, PI / 2, 0, 0);
                }
                r = 16.0f; break;
            case "ghost":
                b.wing(36, 16, 0.9f, c("1b1d22")); b.sph(3, c("22252b"), 0, 0.6f, -1, 1.2f, 0.5f, 2.2f);
                for (float x : new float[]{-3, 3}) b.box(1.6f, 0.4f, 1.2f, c("111111"), x, 0.4f, 5.5f);
                gl.sph(0.4f, new Color(0.6f, 0.85f, 1, 1), 0, 0.9f, -6);
                r = 12.0f; break;
            case "core":
                b.sph(5.2f, c("1f2229"), 0, 0, 0, 1, 1, 1, 16);
                b.torus(7.0f, 0.35f, c("3a3f4a")); gl.torus(7.6f, 0.25f, A, 0, 0, 0, 0.3f, 0.4f, 0);
                gl.sph(0.8f, new Color(1, 0.1f, 0.1f, 1), 0, 0, -5.3f);
                r = 7.0f; break;
            case "hydra":
                b.wing(34, 18, 0.7f, c("26282e")); b.sph(3.4f, c("4b4f57"), 0, 0.6f, 0, 1.2f, 0.5f, 2.0f);
                for (float x : new float[]{-5, 0, 5}) {
                    b.cyl(1.0f, 1.0f, 2.6f, 10, c("1c1c1c"), x, 0.4f, 7.4f, PI / 2, 0, 0); gl.sph(0.8f, new Color(0.75f, 0.45f, 1, 1), x, 0.4f, 8.6f);
                }
                for (float x : new float[]{-12, -6, 6, 12}) gl.sph(0.35f, new Color(1, 0.2f, 0.2f, 1), x, -0.2f, x * 0.1f);
                r = 13.0f; break;
            case "chimera":
                b.wing(40, 20, 1.0f, c("241d30")); b.sph(5, c("2f2640"), 0, 1.2f, 1, 1, 0.8f, 1.2f, 16);
                gl.torus(6.3f, 0.3f, c("9b59b6"), 0, 1.2f, 1);
                for (float x : new float[]{-9, -3, 3, 9}) b.box(1.4f, 0.8f, 4, c("15111c"), x, -0.4f, 4);
                gl.sph(0.9f, new Color(0.85f, 0.4f, 1, 1), 0, 1.2f, -4);
                r = 14.0f; break;
            case "storm":
                b.sph(8.0f, c("3a3f4a"), 0, 0, 0, 1, 0.32f, 1, 18);
                b.sph(4.0f, c("2a2e36"), 0, 1.6f, 0, 1, 0.55f, 1, 14);
                b.torus(8.6f, 0.45f, c("23262c"));
                for (int i = 0; i < 3; i++) {
                    float a = i * MathUtils.PI2 / 3f, px = MathUtils.cos(a) * 5f, pz = MathUtils.sin(a) * 5f, py = -1.2f;
                    b.cyl(0.35f, 0.6f, 3.2f, 10, c("4b5160"), px, py - 1.4f, pz);
                    for (int k = 0; k < 3; k++) b.torus(0.75f - k * 0.12f, 0.12f, c("8a929f"), px, py - 0.8f - k * 0.7f, pz);
                    gl.sph(0.55f, new Color(0.55f, 0.85f, 1, 1), px, py - 3.2f, pz, 1, 1, 1, 8);
                }
                for (int i = 0; i < 8; i++) {
                    float a = i * MathUtils.PI2 / 8f;
                    gl.sph(0.22f, new Color(0.5f, 0.9f, 1, 1), MathUtils.cos(a) * 8f, 0.4f, MathUtils.sin(a) * 8f, 1, 1, 1, 6);
                }
                for (float x : S2) rot.disc(1.6f, Color.WHITE, x * 9.2f, 0.2f, 2.5f, PI / 2, 0, 0);
                r = 10.0f; sc = 1.6f; break;
            case "manta":
                b.wing(30, 14, 0.8f, c("2b3036"));
                b.sph(2.4f, c("3b424b"), 0, 0.5f, -1.0f, 1.2f, 0.45f, 2.2f, 12);
                b.seg(V(0, 0.2f, 5.0f), V(0, 0.6f, 16.0f), 0.6f, 0.15f, 8, c("23272c"));
                for (float x : new float[]{-6, 6}) b.box(1.4f, 0.5f, 3.0f, c("1b1e22"), x, -0.2f, 2.0f);
                for (float x : new float[]{-6, 6}) gl.box(1.0f, 0.2f, 0.3f, new Color(1, 0.45f, 0.15f, 1), x, -0.2f, 3.55f);
                gl.sph(0.5f, new Color(1, 0.3f, 0.2f, 1), 0, 0.2f, -5.6f);
                r = 10.0f; sc = 1.4f; break;
            case "colossus":
                tracksSimple(b, 9.0f, 2.6f, 2.0f);
                b.box(4.2f, 1.5f, 8.6f, c("34382f"), 0, 1.6f, 0);
                b.extrude(new float[][]{{-4.6f, 0}, {3.4f, 0}, {4.9f, 0.8f}, {4.1f, 1.5f}, {-4.6f, 1.5f}}, 6.4f, c("3c4038"), 0, 2.3f, 0);
                b.cyl(2.3f, 2.7f, 1.5f, 16, c("4a4f45"), 0, 4.55f, 0.7f);
                b.extrude(new float[][]{{-1.2f, 0}, {2.4f, 0}, {3.0f, 0.6f}, {2.4f, 1.2f}, {-1.2f, 1.2f}}, 3.6f, c("454a40"), 0, 4.4f, 0.4f);
                for (float x : new float[]{-0.9f, 0.9f}) {
                    b.cyl(0.35f, 0.42f, 7.5f, 10, c("1f211d"), x, 5.0f, -5.2f, PI / 2, 0, 0);
                    b.cyl(0.5f, 0.5f, 0.9f, 10, c("1f211d"), x, 5.0f, -8.6f, PI / 2, 0, 0);
                }
                for (float x : new float[]{-2.6f, 2.6f}) b.box(1.0f, 1.0f, 1.8f, c("4a2a2a"), x, 5.0f, 2.2f);
                gl.sph(0.35f, new Color(1, 0.2f, 0.15f, 1), 0, 5.7f, -2.6f);
                r = 6.0f; sc = 2.2f; break;
            case "inf": case "rpg": {
                Color cA = c("3d4148"), cB = c("2a2d33"), cC = c("1c1e22");
                b.box(0.52f, 0.24f, 0.34f, cB, 0, 1.36f, 0);
                b.seg(V(0, 1.45f, 0), V(0, 1.75f, 0), 0.22f, 0.27f, 10, cA);
                b.sph(0.34f, cA, 0, 1.92f, 0, 1.0f, 0.85f, 0.66f, 12);
                b.box(0.56f, 0.46f, 0.12f, cB, 0, 1.9f, -0.2f); b.box(0.5f, 0.1f, 0.13f, cC, 0, 1.62f, -0.19f);
                for (float x : new float[]{-0.16f, 0f, 0.16f}) b.box(0.12f, 0.16f, 0.08f, cC, x, 1.72f, -0.27f);
                b.box(0.44f, 0.56f, 0.26f, cB, 0, 1.9f, 0.3f); b.cyl(0.012f, 0.012f, 0.8f, 4, cC, 0.15f, 2.5f, 0.38f);
                b.seg(V(0, 2.15f, 0), V(0, 2.28f, 0), 0.08f, 0.08f, 8, cC);
                b.sph(0.16f, c("2f3238"), 0, 2.38f, 0, 1, 1, 1, 10);
                b.sph(0.2f, cB, 0, 2.44f, 0.01f, 1.0f, 0.78f, 1.08f, 12);
                gl.box(0.26f, 0.05f, 0.03f, new Color(1, 0.2f, 0.15f, 1), 0, 2.38f, -0.16f);
                for (float s : S2) {
                    Vector3 sh = V(s * 0.37f, 2.1f, 0), el = V(s * 0.42f, 1.78f, -0.2f);
                    b.sph(0.12f, cB, sh.x, sh.y, sh.z, 1, 1, 1, 8);
                    b.seg(sh, el, 0.085f, 0.075f, 8, cA);
                    Vector3 hand = type.equals("inf") ? V(s * 0.12f, 1.72f, -0.55f) : V(s * 0.3f, 2.0f, -0.35f);
                    if (type.equals("rpg") && s < 0) hand = V(-0.2f, 1.85f, -0.4f);
                    b.seg(el, hand, 0.072f, 0.06f, 8, cA);
                    b.sph(0.065f, cC, hand.x, hand.y, hand.z, 1, 1, 1, 6);
                }
                if (type.equals("inf")) {
                    b.box(0.07f, 0.14f, 0.75f, cC, 0.06f, 1.72f, -0.6f);
                    b.seg(V(0.06f, 1.75f, -0.95f), V(0.06f, 1.75f, -1.35f), 0.025f, 0.022f, 6, cC);
                    b.box(0.06f, 0.2f, 0.1f, cC, 0.06f, 1.58f, -0.62f, -0.25f, 0, 0);
                    b.box(0.06f, 0.12f, 0.3f, cC, 0.06f, 1.68f, -0.12f);
                    b.box(0.05f, 0.06f, 0.18f, cC, 0.06f, 1.82f, -0.55f);
                } else {
                    b.seg(V(0.3f, 2.12f, 0.55f), V(0.3f, 2.12f, -0.85f), 0.1f, 0.1f, 10, c("3a4232"));
                    b.seg(V(0.3f, 2.12f, -0.85f), V(0.3f, 2.12f, -1.25f), 0.13f, 0.04f, 10, c("6a3a28"));
                    b.box(0.08f, 0.18f, 0.1f, cC, 0.3f, 1.98f, -0.35f);
                }
                r = 1.4f; sc = 4.0f; break;
            }
            case "mech":
                b.extrude(new float[][]{{-1.3f, -0.7f}, {1.3f, -0.7f}, {1.6f, 0.2f}, {1.0f, 1.0f}, {-1.3f, 1.0f}}, 2.6f, c("3a3d33"), 0, 3.6f, 0);
                for (float x : new float[]{-1.7f, 1.7f}) {
                    b.box(0.7f, 0.7f, 1.2f, c("2c2f28"), x, 3.8f, 0);
                    b.cyl(0.18f, 0.2f, 1.8f, 8, c("1a1a1a"), x, 3.8f, -1.2f, PI / 2, 0, 0);
                }
                gl.box(1.2f, 0.12f, 0.08f, new Color(1, 0.2f, 0.2f, 1), 0, 4.0f, -1.62f);
                r = 2.6f; sc = 2.6f; break;
            case "mortar":
                tracksSimple(b, 2.6f, 0.8f, 0.6f); b.box(1.6f, 0.6f, 2.2f, c("3a3d33"), 0, 0.9f, 0);
                b.cyl(0.22f, 0.28f, 1.8f, 10, c("1a1a1a"), 0, 1.7f, -0.3f, -0.6f, 0, 0);
                r = 1.8f; sc = 3.0f; break;
            default:
                quadBody(b, gl, rot, A, 4, DGREY, 1f);
        }
        bossDetail(type, b, gl, rot, A);
        ed = new EnemyDef();
        ed.body = b.mesh();
        gl.noAo = true;
        if (!gl.empty()) { boolean h = MB.hq; MB.hq = false; ed.glow = gl.mesh(); MB.hq = h; }
        if (!rot.empty()) { boolean h = MB.hq; MB.hq = false; ed.rot = rot.mesh(); MB.hq = h; }
        ed.r = r; ed.sc = sc;
        MB.hq = false;
        enemyCache.put(key, ed);
        return ed;
    }

    static Mesh3 leg(String kind) {
        Mesh3 m = legCache.get(kind);
        if (m != null) return m;
        MB.hq = kind.equals("walker");
        MB b = new MB();
        if (kind.equals("walker")) {
            Color LW = c("2e3237"), LD = c("1c1e21"), LL = c("444a51");
            b.sph(1.05f, LL, 0, 0, 0, 1, 1, 1, 20);
            b.seg(V(0, 0, 0), V(0, -3.0f, -0.7f), 0.85f, 0.62f, 16, LW);
            b.extrude(new float[][]{{-0.5f, -2.6f}, {0.9f, -2.4f}, {1.1f, -0.4f}, {0.2f, 0.2f}, {-0.6f, -0.4f}}, 1.5f, LL);
            b.sph(0.82f, LD, 0, -3.0f, -0.7f, 1, 1, 1, 18);
            b.cyl(0.95f, 0.95f, 1.5f, 20, LL, 0, -3.0f, -0.7f, 0, 0, PI / 2);
            b.seg(V(0, -3.0f, -0.7f), V(0, -6.0f, 0.3f), 0.62f, 0.45f, 16, LW);
            b.extrude(new float[][]{{0.2f, -5.4f}, {1.25f, -5.2f}, {1.35f, -3.3f}, {0.6f, -2.9f}}, 1.3f, LL);
            for (float x : new float[]{-0.55f, 0.55f}) {
                b.seg(V(x, -0.6f, 0.75f), V(x, -2.7f, 0.2f), 0.14f, 0.14f, 8, LD);
                b.seg(V(x, -3.3f, 0.35f), V(x, -5.5f, 0.85f), 0.12f, 0.12f, 8, c("8a8f96"));
            }
            b.sph(0.5f, LD, 0, -6.0f, 0.3f, 1, 1, 1, 14);
            b.extrude(new float[][]{{-1.3f, -6.95f}, {1.7f, -6.95f}, {1.9f, -6.5f}, {0.6f, -6.1f}, {-1.1f, -6.3f}}, 2.2f, LW);
            for (float x : new float[]{-0.75f, 0f, 0.75f}) b.seg(V(x, -6.7f, -1.5f), V(x * 1.25f, -6.92f, -2.7f), 0.26f, 0.05f, 10, LD);
            b.seg(V(0, -6.7f, 1.1f), V(0, -6.92f, 2.1f), 0.24f, 0.05f, 10, LD);
        } else if (kind.equals("mech")) {
            b.box(0.6f, 2.8f, 0.7f, c("2c2f28"), 0, -1.4f, 0); b.box(1.0f, 0.3f, 1.4f, c("1f211c"), 0, -2.85f, -0.2f);
        } else {
            b.sph(0.13f, c("2a2d33"), 0, 0, 0, 1, 1, 1, 8);
            b.seg(V(0, 0, 0), V(0, -0.6f, -0.04f), 0.12f, 0.095f, 8, c("3d4148"));
            b.box(0.2f, 0.2f, 0.08f, c("2a2d33"), 0, -0.62f, -0.12f);
            b.seg(V(0, -0.6f, -0.04f), V(0, -1.12f, 0.02f), 0.09f, 0.075f, 8, c("3d4148"));
            b.box(0.18f, 0.14f, 0.34f, c("1c1e22"), 0, -1.2f, -0.06f);
        }
        m = b.mesh();
        MB.hq = false;
        legCache.put(kind, m);
        return m;
    }

    static void rivets(MB b, Vector3 a, Vector3 c2, int n, float r) {
        for (int i = 0; i < n; i++) {
            float k = (float) i / (float) Math.max(1, n - 1);
            b.sph(r, c("5a6068"), MathUtils.lerp(a.x, c2.x, k), MathUtils.lerp(a.y, c2.y, k), MathUtils.lerp(a.z, c2.z, k), 1, 1, 1, 4);
        }
    }

    static void pod(MB b, MB gl, float px, float py, float pz, int n, float r, float L) {
        b.cyl(r, r, L, 12, BD1, px, py, pz, PI / 2, 0, 0);
        b.cyl(r * 1.05f, r * 1.05f, 0.15f, 12, BDK, px, py, pz - L / 2, PI / 2, 0, 0);
        for (int i = 0; i < n; i++) {
            float a = i * MathUtils.PI2 / n;
            gl.sph(r * 0.18f, new Color(1, 0.45f, 0.2f, 1), px + MathUtils.cos(a) * r * 0.55f, py + MathUtils.sin(a) * r * 0.55f, pz - L / 2 - 0.05f, 1, 1, 1, 4);
        }
    }

    static void nozzle(MB b, MB gl, float px, float py, float pz, float r, Color glow) {
        b.cyl(r, r * 0.8f, r * 1.4f, 12, BDK, px, py, pz, PI / 2, 0, 0);
        gl.cyl(r * 0.65f, r * 0.65f, 0.1f, 12, glow, px, py, pz + r * 0.72f, PI / 2, 0, 0);
    }

    static void navl(MB gl, float x, float y, float z, boolean red) {
        gl.sph(0.28f, red ? new Color(1, 0.15f, 0.1f, 1) : new Color(0.2f, 1, 0.4f, 1), x, y, z, 1, 1, 1, 6);
    }

    static void bossDetail(String type, MB b, MB gl, MB rot, Color A) {
        Color glowO = new Color(1, 0.55f, 0.2f, 1);
        switch (type) {
            case "boss":
                for (int i = 0; i < 6; i++) {
                    float a = i / 6f * MathUtils.PI2, x = MathUtils.cos(a) * 5f, z = MathUtils.sin(a) * 5f;
                    b.torus(2.45f, 0.14f, BD2, x, 0.85f, z);
                    for (int k = 0; k < 3; k++) b.box(0.08f, 0.5f, 0.08f, BD1, x + MathUtils.cos(a + k * 2.1f) * 2.4f, 0.6f, z + MathUtils.sin(a + k * 2.1f) * 2.4f);
                    navl(gl, x + MathUtils.cos(a) * 2.5f, 1.1f, z + MathUtils.sin(a) * 2.5f, i % 2 == 0);
                    for (int k = 0; k < 5; k++) b.box(0.42f, 0.04f, 0.12f, k % 2 == 0 ? A : BDK, x * (0.25f + k * 0.1f), 0.39f, z * (0.25f + k * 0.1f), 0, -a, 0);
                }
                for (int i = 0; i < 6; i++) {
                    float a = (i + 0.5f) / 6f * MathUtils.PI2;
                    b.box(1.6f, 0.12f, 1.0f, BD3, MathUtils.cos(a) * 2.2f, 1.32f, MathUtils.sin(a) * 2.2f, 0.25f, -a, 0);
                    rivets(b, V(MathUtils.cos(a) * 1.7f, 1.42f, MathUtils.sin(a) * 1.7f), V(MathUtils.cos(a) * 2.7f, 1.25f, MathUtils.sin(a) * 2.7f), 3, 0.07f);
                }
                b.cyl(1.0f, 0.8f, 0.6f, 12, BD1, 0, 1.6f, 0); b.cyl(0.08f, 0.08f, 2.5f, 6, c("777777"), 0.6f, 2.6f, 0.4f); gl.sph(0.18f, new Color(1, 0.2f, 0.15f, 1), 0.6f, 3.9f, 0.4f);
                b.sph(1.1f, BD2, 0, -1.5f, -0.6f, 1, 1, 1, 12);
                for (float x : new float[]{-0.35f, 0.35f}) b.cyl(0.14f, 0.14f, 2.4f, 8, BDK, x, -1.6f, -2.0f, PI / 2, 0, 0);
                gl.cyl(0.3f, 0.3f, 0.05f, 10, new Color(1, 0.2f, 0.15f, 1), 0, -1.3f, -1.65f, PI / 2, 0, 0);
                for (float x : new float[]{-2.2f, 2.2f}) {
                    b.box(0.18f, 0.18f, 5.0f, BDK, x, -2.0f, 0);
                    for (float z : new float[]{-1.5f, 1.5f}) b.box(0.14f, 1.0f, 0.14f, BD1, x, -1.5f, z);
                }
                break;
            case "gunship":
                b.sph(1.1f, BGL, 0, 0.95f, -2.6f, 0.75f, 0.6f, 1.3f, 18);
                b.sph(1.0f, BGL, 0, 1.2f, -1.2f, 0.75f, 0.6f, 1.1f, 18);
                b.box(1.5f, 0.06f, 0.08f, BDK, 0, 1.62f, -1.9f);
                b.sph(0.6f, BD2, 0, -1.0f, -3.7f, 1, 1, 1, 16);
                for (float x : new float[]{-0.15f, 0.15f}) b.cyl(0.08f, 0.08f, 1.8f, 10, BDK, x, -1.05f, -4.7f, PI / 2, 0, 0);
                for (float x : new float[]{-3.0f, 3.0f}) {
                    pod(b, gl, x, -0.6f, 0.3f, 7, 0.55f, 2.0f);
                    pod(b, gl, x * 0.68f, -0.55f, 0.6f, 4, 0.32f, 1.6f);
                    b.box(0.2f, 0.5f, 0.3f, BD1, x, -0.25f, 0.3f);
                }
                for (float x : new float[]{-0.95f, 0.95f}) {
                    b.sph(0.7f, BD2, x, 1.5f, 0.6f, 0.9f, 0.85f, 2.1f, 16);
                    b.cyl(0.4f, 0.4f, 0.12f, 14, BDK, x, 1.5f, -0.85f, PI / 2, 0, 0);
                    nozzle(b, gl, x, 1.5f, 2.1f, 0.34f, glowO);
                    b.seg(V(x * 1.3f, -1.7f, -2.4f), V(x * 1.3f, -1.7f, 1.8f), 0.08f, 0.08f, 8, BDK);
                    for (float z : new float[]{-1.6f, 1.2f}) b.seg(V(x * 0.9f, -0.9f, z), V(x * 1.3f, -1.7f, z), 0.06f, 0.06f, 6, BD1);
                }
                navl(gl, -3.1f, -0.15f, 0.6f, true); navl(gl, 3.1f, -0.15f, 0.6f, false); gl.sph(0.2f, Color.WHITE, 0, 3.3f, 10.6f);
                for (int k = 0; k < 6; k++) b.box(0.04f, 0.5f, 0.06f, k % 2 == 0 ? A : BDK, -1.95f, 0.3f, -1.2f + k * 0.4f);
                break;
            case "walker":
                rivets(b, V(-2.0f, 1.75f, -2.6f), V(2.0f, 1.75f, -2.6f), 9, 0.08f);
                rivets(b, V(-2.2f, -1.25f, -3.0f), V(2.2f, -1.25f, -3.0f), 9, 0.08f);
                for (float x : new float[]{-0.8f, 0.8f}) nozzle(b, gl, x, 1.4f, 3.7f, 0.42f, glowO);
                b.cyl(0.06f, 0.06f, 3.0f, 6, c("777777"), 1.2f, 4.6f, 1.8f); gl.sph(0.16f, new Color(1, 0.2f, 0.15f, 1), 1.2f, 6.1f, 1.8f);
                b.sph(0.45f, BD3, -1.4f, 2.1f, -1.8f, 1, 1, 1, 14); gl.sph(0.15f, new Color(1, 0.2f, 0.1f, 1), -1.4f, 2.1f, -2.25f);
                for (float k : S2) gl.sph(0.18f, new Color(1, 0.6f, 0.2f, 1), k * 1.8f, 0.35f, -3.62f);
                break;
            case "carrier":
                for (int i = 0; i < 7; i++) {
                    float z = -14f + i * 4.7f;
                    float k = (float) Math.sqrt(Math.max(0, 1 - Math.pow(z / 19.8, 2))) * 1.01f;
                    for (int j = 0; j < 28; j++) {
                        float t0 = j * MathUtils.PI2 / 28f, t1 = (j + 1) * MathUtils.PI2 / 28f;
                        b.seg(V(MathUtils.cos(t0) * 9f * k, MathUtils.sin(t0) * 6.75f * k, z), V(MathUtils.cos(t1) * 9f * k, MathUtils.sin(t1) * 6.75f * k, z), 0.16f, 0.16f, 4, BD1);
                    }
                }
                for (float a : new float[]{0, PI / 2, PI, PI * 1.5f}) b.box(0.3f, 4.5f, 5.0f, c("2c2a27"), MathUtils.sin(a) * 4f, MathUtils.cos(a) * 4f, 18.5f, 0, 0, -a);
                for (int i = 0; i < 8; i++) {
                    gl.box(0.5f, 0.4f, 0.05f, new Color(1, 0.85f, 0.45f, 1), -2.52f, -6.0f, -5.5f + i * 1.6f, 0, PI / 2, 0);
                    gl.box(0.5f, 0.4f, 0.05f, new Color(1, 0.85f, 0.45f, 1), 2.52f, -6.0f, -5.5f + i * 1.6f, 0, PI / 2, 0);
                }
                for (int i = 0; i < 4; i++) b.box(2.4f, 0.06f, 2.4f, BDK, 0, -7.52f, -5 + i * 3.4f);
                for (float x : new float[]{-1.6f, 1.6f}) gl.sph(0.5f, new Color(1, 1, 0.85f, 1), x, -7.3f, -7.2f);
                b.box(1.4f, 1.2f, 2.0f, BD2, 0, -5.0f, -7.6f); b.sph(0.8f, BGL, 0, -5.2f, -8.5f, 1.2f, 0.8f, 0.8f);
                for (int i = 0; i < 3; i++) { b.cyl(0.06f, 0.06f, 3.0f, 6, c("777777"), -2 + i * 2, 7.2f, -4 + i * 3); navl(gl, -2 + i * 2, 8.7f, -4 + i * 3, i == 1); }
                navl(gl, -9.1f, 0, 0, true); navl(gl, 9.1f, 0, 0, false);
                break;
            case "ghost":
                b.sph(1.4f, BGL, 0, 1.3f, -3.4f, 1.0f, 0.45f, 1.6f, 12);
                for (float x : new float[]{-3, 3}) {
                    b.sph(1.6f, c("15171b"), x, 0.55f, 1.5f, 0.9f, 0.45f, 2.6f, 10);
                    b.box(1.5f, 0.35f, 0.3f, BDK, x, 0.75f, -2.0f);
                    gl.box(1.3f, 0.18f, 0.08f, new Color(0.55f, 0.75f, 1, 1), x, 0.45f, 6.15f);
                }
                for (float k : S2) {
                    for (int i = 0; i < 5; i++) gl.sph(0.12f, new Color(0.5f, 0.8f, 1, 1), k * (3.5f + i * 3.0f), 0.4f, -1.6f + i * 1.35f, 1, 1, 1, 4);
                    b.box(12.0f, 0.04f, 0.08f, BDK, k * 8.0f, 0.47f, 0.5f, 0, k * 0.42f, 0);
                    b.box(10.0f, 0.04f, 0.08f, BDK, k * 7.0f, 0.47f, 2.8f, 0, k * 0.42f, 0);
                }
                b.box(3.0f, 0.05f, 4.0f, c("101215"), 0, -0.47f, 0.5f);
                break;
            case "core":
                for (int i = 0; i < 12; i++) {
                    float a = i / 12f * MathUtils.PI2;
                    for (float yy : new float[]{-2.6f, 0f, 2.6f}) {
                        float rr = (float) Math.sqrt(Math.max(0.1f, 27f - yy * yy));
                        b.box(1.8f, 1.5f, 0.25f, ((i + (int) yy) % 2 == 0) ? BD2 : BD1, MathUtils.cos(a) * rr, yy, MathUtils.sin(a) * rr, 0, -a + PI / 2, 0);
                    }
                    gl.box(0.12f, 1.0f, 0.1f, A, MathUtils.cos(a + 0.26f) * 5.25f, 1.3f, MathUtils.sin(a + 0.26f) * 5.25f, 0, -a, 0);
                }
                for (Vector3 vv : new Vector3[]{V(0, 1, 0), V(0, -1, 0), V(0.7f, 0.5f, 0.5f), V(-0.6f, -0.5f, 0.6f)}) {
                    Vector3 d = vv.nor();
                    b.seg(d.cpy().scl(5f), d.cpy().scl(8f), 0.25f, 0.05f, 6, BD3);
                    gl.sph(0.2f, A, d.x * 8.1f, d.y * 8.1f, d.z * 8.1f, 1, 1, 1, 4);
                }
                for (int i = 0; i < 8; i++) {
                    float a = i / 8f * MathUtils.PI2;
                    b.box(1.2f, 0.5f, 0.7f, BD3, MathUtils.cos(a) * 7f, 0, MathUtils.sin(a) * 7f, 0, -a, 0);
                }
                break;
            case "hydra":
                b.sph(1.6f, BGL, 0, 1.3f, -2.6f, 1.0f, 0.5f, 1.5f, 12);
                for (float k : S2) {
                    for (int i = 0; i < 3; i++) {
                        float x = k * (5.5f + i * 3.2f);
                        for (int j = 0; j < 2; j++) b.cyl(0.18f, 0.18f, 2.4f, 8, c("c9c9c9"), x + j * 0.5f - 0.25f, -0.65f, 0.2f + i * 0.9f, PI / 2, 0, 0);
                        b.box(1.1f, 0.2f, 2.6f, BD1, x, -0.4f, 0.2f + i * 0.9f);
                    }
                    b.box(0.18f, 2.4f, 2.6f, BD2, k * 7.5f, 1.2f, 6.0f, 0, 0, k * 0.3f);
                    b.box(10.0f, 0.04f, 0.08f, BDK, k * 7.0f, 0.37f, 1.5f, 0, k * 0.5f, 0);
                }
                for (float x : new float[]{-5, 0, 5}) b.torus(1.05f, 0.12f, BD3, x, 0.4f, 6.2f, PI / 2, 0, 0);
                break;
            case "chimera":
                for (float k : S2) {
                    b.seg(V(k * 2.5f, 0.2f, -6.0f), V(k * 4.0f, -0.4f, -11.0f), 0.7f, 0.15f, 8, BD1);
                    b.seg(V(k * 4.0f, -0.4f, -11.0f), V(k * 2.8f, -0.8f, -13.5f), 0.2f, 0.03f, 6, c("8a7aa0"));
                    for (int i = 0; i < 6; i++) gl.box(4.0f, 0.08f, 0.12f, new Color(0.75f, 0.35f, 1, 1), k * (4.0f + i * 2.5f), 0.52f, -1.0f + i * 1.6f, 0, k * (0.6f - i * 0.05f), 0);
                    for (float x : new float[]{3, 9}) nozzle(b, gl, k * x, -0.4f, 6.4f, 0.55f, new Color(0.85f, 0.45f, 1, 1));
                }
                for (int i = 0; i < 7; i++) b.seg(V(0, 4.8f - i * 0.3f, -2.0f + i * 1.4f), V(0, 6.6f - i * 0.35f, -1.6f + i * 1.4f), 0.35f, 0.03f, 6, c("4a3a60"));
                for (int i = 0; i < 3; i++) gl.sph(0.35f, new Color(1, 0.3f, 0.9f, 1), -0.9f + i * 0.9f, 2.0f, -4.6f, 1, 1, 1, 6);
                break;
            case "storm":
                b.box(3.0f, 1.2f, 5.0f, BD1, 0, -3.0f, 0);
                for (int i = 0; i < 5; i++) gl.box(0.4f, 0.3f, 0.05f, new Color(0.7f, 0.9f, 1, 1), -1.52f, -3.0f, -1.6f + i * 0.8f, 0, PI / 2, 0);
                for (int i = 0; i < 6; i++) {
                    float a = i * MathUtils.PI2 / 6f + 0.5f;
                    b.cyl(0.06f, 0.12f, 3.4f, 6, c("8a929f"), MathUtils.cos(a) * 3f, 3.6f, MathUtils.sin(a) * 3f);
                    gl.sph(0.2f, new Color(0.6f, 0.9f, 1, 1), MathUtils.cos(a) * 3f, 5.3f, MathUtils.sin(a) * 3f, 1, 1, 1, 4);
                }
                for (int i = 0; i < 16; i++) b.box(1.4f, 0.5f, 0.3f, BD2, MathUtils.cos(i * MathUtils.PI2 / 16) * 8.6f, 0, MathUtils.sin(i * MathUtils.PI2 / 16) * 8.6f, 0, -i * MathUtils.PI2 / 16 + PI / 2, 0);
                break;
            case "manta":
                b.sph(1.2f, BGL, 0, 0.9f, -2.6f, 1.0f, 0.5f, 1.6f, 12);
                b.box(2.6f, 0.06f, 4.0f, BDK, 0, -0.42f, 0.5f);
                for (int i = 0; i < 3; i++) b.box(2.4f, 0.04f, 0.06f, c("5a5f66"), 0, -0.46f, -1.0f + i * 1.2f);
                for (float k : S2) {
                    b.box(0.15f, 1.8f, 2.2f, BD2, k * 3.5f, 0.9f, 4.0f, 0, 0, k * 0.25f);
                    b.box(1.2f, 0.5f, 1.4f, BDK, k * 2.0f, 0.6f, -0.8f);
                    for (int i = 0; i < 6; i++) gl.box(1.6f, 0.06f, 0.1f, new Color(1, 0.45f, 0.15f, 1), k * (2.5f + i * 2.0f), 0, -2.0f + i * 1.1f, 0, k * 0.55f, 0);
                }
                break;
            case "colossus":
                for (float k : S2) {
                    for (int i = 0; i < 5; i++) b.box(0.08f, 0.95f, 1.7f, BD2, k * 3.25f, 1.75f, -3.6f + i * 1.8f);
                    rivets(b, V(k * 3.3f, 2.1f, -4.2f), V(k * 3.3f, 2.1f, 4.2f), 11, 0.07f);
                    for (int i = 0; i < 4; i++) b.box(0.12f, 0.12f, 0.7f, BDK, k * 3.25f, 3.05f, -1.5f + i * 0.75f);
                    b.cyl(0.25f, 0.28f, 0.9f, 10, c("2b2b2b"), k * 2.2f, 4.1f, 4.2f); gl.cyl(0.18f, 0.18f, 0.05f, 8, new Color(1, 0.5f, 0.2f, 1), k * 2.2f, 4.56f, 4.2f);
                    for (int i = 0; i < 3; i++) b.cyl(0.1f, 0.1f, 0.5f, 6, BDK, k * 2.3f, 5.4f + i * 0.05f, -0.8f + i * 0.25f, -0.6f, 0, -k * 0.6f);
                }
                b.cyl(0.5f, 0.5f, 0.18f, 12, BD3, 0.9f, 5.8f, 1.2f); b.cyl(0.07f, 0.07f, 1.4f, 6, BDK, 0.9f, 6.1f, 0.4f, PI / 2, 0, 0);
                for (int i = 0; i < 3; i++) b.box(0.3f, 0.2f, 0.2f, BDK, -0.8f + i * 0.4f, 3.85f, -4.1f);
                gl.box(1.6f, 0.1f, 0.06f, new Color(0.4f, 0.8f, 1, 1), 0, 3.2f, -4.75f);
                b.cyl(0.07f, 0.07f, 3.5f, 6, c("777777"), -1.6f, 7.3f, 1.8f); gl.sph(0.15f, new Color(1, 0.2f, 0.15f, 1), -1.6f, 9.1f, 1.8f);
                break;
        }
    }

    static void tracksSimple(MB b, float length, float x, float h) {
        for (float s : S2) {
            b.box(0.62f, h, length, c("1d1d1b"), s * x, h / 2 + 0.05f, 0);
            for (int i = 0; i < 6; i++) {
                float z = -length / 2 + 0.7f + i * (length - 1.4f) / 5;
                b.cyl(0.42f, 0.42f, 0.66f, 12, c("34362f"), s * x, 0.5f, z, 0, 0, PI / 2);
                b.cyl(0.18f, 0.18f, 0.7f, 8, STEEL, s * x, 0.5f, z, 0, 0, PI / 2);
            }
            for (int i = 0; i < 18; i++) b.box(0.66f, 0.06f, 0.18f, c("2c2c28"), s * x, 0.04f, -length / 2 + 0.3f + i * (length - 0.6f) / 17);
        }
    }

    public static EnemyModel enemy(String type, String acc) {
        EnemyDef d = def(type, acc);
        EnemyModel em = new EnemyModel();
        Node3 g = new Node3();
        em.body = g.add(new Node3(d.body, MAT_HULL));
        if (d.glow != null) em.glow = g.add(new Node3(d.glow, MAT_GLOW));
        if (d.rot != null) em.rot = g.add(new Node3(d.rot, MAT_ROTOR));
        if (type.equals("inf") || type.equals("rpg") || type.equals("mech") || type.equals("walker")) {
            String lk = type.equals("walker") ? "walker" : (type.equals("mech") ? "mech" : "inf");
            float hip = type.equals("walker") ? -1.4f : (type.equals("mech") ? 3.0f : 1.3f);
            float hx = type.equals("walker") ? 2.6f : (type.equals("mech") ? 1.0f : 0.25f);
            for (float s : S2) {
                Node3 l = new Node3(leg(lk), MAT_HULL);
                l.pos.set(s * hx, hip, 0);
                g.add(l);
                em.legs.add(l);
            }
        }
        g.scl.set(d.sc, d.sc, d.sc);
        em.g = g;
        em.r = d.r * d.sc;
        return em;
    }

    static Mesh3 missile;

    public static Mesh3 missileMesh() {
        if (missile != null) return missile;
        MB b = new MB();
        float rx = PI / 2;
        b.cyl(0.17f, 0.17f, 1.5f, 16, c("8b8f80"), 0, 0, 0.85f, rx, 0, 0);
        b.cyl(0.14f, 0.17f, 0.2f, 16, c("3a3a36"), 0, 0, 1.68f, rx, 0, 0);
        b.cyl(0.175f, 0.175f, 0.06f, 16, c("2a2a28"), 0, 0, 0.12f, rx, 0, 0);
        for (int i = 0; i < 4; i++) {
            float a = i * PI / 2 + PI / 4;
            b.box(0.025f, 0.42f, 0.5f, c("7d8173"), MathUtils.cos(a) * 0.32f, MathUtils.sin(a) * 0.32f, 1.35f, 0, 0, a - PI / 2);
        }
        b.cyl(0.09f, 0.09f, 1.55f, 14, c("d8d9d2"), 0, 0, -0.65f, rx, 0, 0);
        b.cyl(0f, 0.09f, 0.55f, 14, c("c4c6bd"), 0, 0, -1.7f, -PI / 2, 0, 0);
        b.cyl(0.092f, 0.092f, 0.05f, 14, c("1e1e1e"), 0, 0, -1.1f, rx, 0, 0);
        for (int i = 0; i < 4; i++) {
            float a = i * PI / 2;
            b.box(0.015f, 0.2f, 0.18f, c("b8bab2"), MathUtils.cos(a) * 0.16f, MathUtils.sin(a) * 0.16f, -1.15f, 0, 0, a - PI / 2);
            b.box(0.015f, 0.16f, 0.3f, c("b8bab2"), MathUtils.cos(a) * 0.15f, MathUtils.sin(a) * 0.15f, 0f, 0, 0, a - PI / 2);
        }
        missile = b.mesh();
        return missile;
    }

    /** МОГ — мобильная огневая группа. */
    public static final class Mog { public Node3 g, gun; public float t; public Object tgt; public float dmg; }

    public static Mog mog() {
        Node3 g = new Node3(), gun = new Node3();
        Parts P = new Parts();
        MB C = P.g("root", "c"), D = P.g("root", "d"), M = P.g("root", "m"), Gl = P.g("root", "g"), L = P.g("root", "l"), R = P.g("root", "r");
        D.box(1.5f, 0.35f, 5.0f, OLIVE_D, 0, 0.62f, 0);
        for (float z : new float[]{-1.75f, 1.75f}) {
            M.cyl(0.08f, 0.08f, 1.9f, 8, GUN, 0, 0.55f, z, 0, 0, PI / 2);
            for (float s : S2) wheel(P, "root", s * 1.03f, 0.55f, z, 0.55f, 0.42f, s);
        }
        C.extrude(new float[][]{{-2.8f, 0}, {2.6f, 0}, {2.85f, 0.35f}, {2.75f, 0.85f}, {1.3f, 1.0f}, {0.9f, 1.75f}, {-2.75f, 1.8f}, {-2.85f, 1.0f}}, 2.3f, WHITE, 0, 0.75f, 0);
        for (float s : S2) {
            fender(C, -2.45f, -1.05f, s * 1.06f, 1.3f, 0.5f, WHITE);
            fender(C, 1.05f, 2.45f, s * 1.06f, 1.3f, 0.5f, WHITE);
            Gl.box(0.95f, 0.85f, 0.04f, GLASS, s * 0.55f, 2.125f, -1.13f, 0.49f, 0, 0);
            for (float z : new float[]{-0.4f, 0.6f}) Gl.box(0.04f, 0.42f, 0.8f, GLASS, s * 1.155f, 2.12f, z);
            D.box(0.03f, 1.0f, 0.03f, DARK, s * 1.153f, 1.7f, 0.1f); D.box(0.03f, 1.0f, 0.03f, DARK, s * 1.153f, 1.7f, 1.1f);
            M.box(0.05f, 0.05f, 0.2f, STEEL, s * 1.17f, 1.85f, -0.2f); M.box(0.05f, 0.05f, 0.2f, STEEL, s * 1.17f, 1.85f, 0.8f);
            L.box(0.24f, 0.16f, 0.05f, c("fff3d0"), s * 0.78f, 1.4f, -2.83f);
            L.box(0.16f, 0.1f, 0.05f, c("d0201a"), s * 0.9f, 1.6f, 2.86f);
            rail(M, V(s * 1.15f, 2.2f, -0.95f), V(s * 1.45f, 2.25f, -1.05f)); D.box(0.06f, 0.3f, 0.2f, DARK, s * 1.48f, 2.15f, -1.05f);
            M.box(0.1f, 0.12f, 0.2f, GUN, s * 0.6f, 0.85f, -2.95f);
        }
        D.box(1.4f, 0.4f, 0.04f, c("1d1f1a"), 0, 1.35f, -2.86f);
        for (int i = 0; i < 4; i++) M.box(1.35f, 0.035f, 0.05f, GUN, 0, 1.21f + i * 0.09f, -2.88f);
        D.box(2.35f, 0.3f, 0.3f, OLIVE_D, 0, 0.9f, -2.95f); M.cyl(0.14f, 0.14f, 0.7f, 10, GUN, 0, 0.95f, -3.05f, 0, 0, PI / 2);
        R.cyl(0.5f, 0.5f, 0.36f, 20, RUBBER, 0, 1.75f, 3.0f, PI / 2, 0, 0); D.cyl(0.3f, 0.3f, 0.38f, 16, OLIVE_D, 0, 1.75f, 3.0f, PI / 2, 0, 0);
        M.cyl(0.52f, 0.52f, 0.1f, 20, GUN, 0, 2.6f, 0.3f);
        for (float x : new float[]{-0.95f, 0.95f}) rail(M, V(x, 2.62f, 0.9f), V(x, 2.62f, 2.6f));
        M.cyl(0.012f, 0.02f, 1.6f, 4, DARK, -0.9f, 3.35f, 2.5f);
        MB GC = P.g("gun", "c"), GD = P.g("gun", "d"), GM = P.g("gun", "m");
        GM.cyl(0.06f, 0.06f, 0.45f, 8, GUN, 0, 0.22f, 0);
        GC.box(1.2f, 0.55f, 0.06f, WHITE, 0, 0.55f, -0.45f);
        for (float s : S2) GC.box(0.45f, 0.55f, 0.06f, WHITE, s * 0.75f, 0.55f, -0.32f, 0, s * 0.6f, 0);
        GM.box(0.16f, 0.2f, 0.75f, GUN, 0, 0.55f, -0.05f);
        GM.seg(V(0, 0.56f, -0.42f), V(0, 0.56f, -1.5f), 0.035f, 0.03f, 8, GUN);
        GM.seg(V(0, 0.56f, -1.45f), V(0, 0.56f, -1.62f), 0.05f, 0.05f, 8, GUN);
        GD.box(0.2f, 0.2f, 0.3f, OLIVE_D, -0.22f, 0.5f, 0f);
        GM.box(0.04f, 0.12f, 0.06f, GUN, 0.1f, 0.5f, 0.33f); GM.box(0.04f, 0.12f, 0.06f, GUN, -0.1f, 0.5f, 0.33f);
        Color uni = c("4b5238");
        GD.seg(V(0, 0, 0.55f), V(0, 0.55f, 0.55f), 0.2f, 0.24f, 10, uni);
        GD.sph(0.15f, c("c49a7a"), 0, 0.74f, 0.55f, 1, 1, 1, 10);
        GD.sph(0.18f, uni.cpy().mul(0.8f, 0.8f, 0.8f, 1), 0, 0.8f, 0.56f, 1, 0.75f, 1.05f, 10);
        for (float s : S2) GD.seg(V(s * 0.2f, 0.48f, 0.55f), V(s * 0.1f, 0.5f, 0.33f), 0.06f, 0.05f, 6, uni);
        Map<String, Mat> mats = new HashMap<>();
        mats.put("c", camoMat("olive")); mats.put("d", MAT_PAINT); mats.put("m", MAT_METAL); mats.put("r", MAT_RUBBER); mats.put("g", MAT_GLASS); mats.put("l", MAT_LAMP);
        Map<String, Node3> nodes = new HashMap<>();
        nodes.put("root", g); nodes.put("gun", gun);
        for (Map.Entry<String, Map<String, MB>> e : P.d.entrySet())
            for (Map.Entry<String, MB> k : e.getValue().entrySet()) {
                MB b = k.getValue();
                if (b.empty()) continue;
                b.noAo = k.getKey().equals("l") || k.getKey().equals("g");
                Node3 mi = new Node3(b.mesh(), mats.get(k.getKey()));
                mi.castShadow = !b.noAo;
                nodes.get(e.getKey()).add(mi);
            }
        gun.pos.set(0, 2.62f, 0.3f);
        g.add(gun);
        Mog m = new Mog();
        m.g = g; m.gun = gun;
        return m;
    }

    private Models() { }
}
