package ru.shitneba;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

import java.util.HashMap;
import java.util.Map;

import ru.shitneba.gfx.MB;
import ru.shitneba.gfx.Mesh3;
import ru.shitneba.gfx.Node3;

/** Модели режима «Рой»: дроны-камикадзе, бомбы, наземные цели. Порт smodels.gd. */
public final class SModels {
    static Color c(String h) { return G.hex(h); }

    static final Color CF = c("1d2023"), CF2 = c("2b2f33"), MOT = c("3c4044"), BELL = c("c45a24"), OLV = c("4b5a32"), OLV2 = c("5d6b3c"),
            BAT = c("d9b21c"), LG = c("8e949b"), WH = c("d6d9dc");
    static final float PI = MathUtils.PI;

    static Mesh3 prop;

    static Mesh3 propMesh() {
        if (prop != null) return prop;
        MB b = new MB(); b.noAo = true;
        b.cyl(1f, 1f, 0.012f, 28, Color.WHITE);
        prop = b.mesh();
        return prop;
    }

    static Vector3 V(float x, float y, float z) { return new Vector3(x, y, z); }

    public static final class Drone { public Node3 g; public final Array<Node3> rot = new Array<>(), bombs = new Array<>(); public float r; public final Vector3 cam = new Vector3(); }

    static final class DroneTpl { Node3 g; float r, sc; Vector3 cam; }

    static final Map<String, DroneTpl> droneCache = new HashMap<>();

    public static Drone drone(String kind) {
        DroneTpl t = droneCache.get(kind);
        if (t == null) { t = buildDrone(kind); droneCache.put(kind, t); }
        Drone d = new Drone();
        d.g = t.g.cloneTree();
        d.r = t.r; d.cam.set(t.cam).scl(t.sc);
        for (Node3 k : d.g.kids) {
            if ("rot".equals(k.name)) d.rot.add(k);
            if ("bomb".equals(k.name)) d.bombs.add(k);
        }
        return d;
    }

    static DroneTpl buildDrone(String kind) {
        MB.hq = true;
        MB b = new MB(), m = new MB(), gl = new MB();
        gl.noAo = true;
        Array<float[]> props = new Array<>(); // x,y,z,scale,axisZ
        float r, sc;
        Vector3 cam;
        switch (kind) {
            case "osa": {
                b.box(0.36f, 0.04f, 0.5f, CF, 0, 0, 0); b.box(0.32f, 0.03f, 0.42f, CF2, 0, 0.15f, 0);
                for (float x : new float[]{-0.13f, 0.13f}) for (float z : new float[]{-0.17f, 0.17f}) m.cyl(0.012f, 0.012f, 0.15f, 6, LG, x, 0.075f, z);
                for (int i = 0; i < 4; i++) {
                    float a = PI / 4 + i * PI / 2;
                    float px = MathUtils.cos(a) * 0.46f, pz = MathUtils.sin(a) * 0.46f;
                    b.box(0.62f, 0.035f, 0.075f, CF, px * 0.5f, 0, pz * 0.5f, 0, -a, 0);
                    m.cyl(0.06f, 0.065f, 0.07f, 14, MOT, px, 0.05f, pz);
                    m.cyl(0.055f, 0.055f, 0.035f, 14, BELL, px, 0.1f, pz);
                    m.cyl(0.01f, 0.01f, 0.06f, 6, LG, px, 0.14f, pz);
                    props.add(new float[]{px, 0.15f, pz, 0.25f, 0});
                }
                b.box(0.16f, 0.11f, 0.3f, BAT, 0, 0.23f, 0.02f); b.box(0.17f, 0.012f, 0.31f, c("222222"), 0, 0.29f, 0.02f);
                b.box(0.03f, 0.125f, 0.32f, c("1a1a1a"), 0, 0.23f, 0.02f);
                b.box(0.1f, 0.09f, 0.08f, c("2a2a2a"), 0, 0.08f, -0.25f, -0.25f, 0, 0);
                m.cyl(0.03f, 0.035f, 0.05f, 12, c("111111"), 0, 0.09f, -0.3f, PI / 2 - 0.25f, 0, 0);
                gl.sph(0.022f, new Color(0.3f, 0.6f, 1f, 1), 0, 0.095f, -0.325f);
                b.cyl(0.07f, 0.07f, 0.5f, 16, OLV, 0, -0.1f, -0.02f, PI / 2, 0, 0);
                b.cyl(0f, 0.07f, 0.2f, 16, OLV2, 0, -0.1f, -0.37f, -PI / 2, 0, 0);
                m.cyl(0.012f, 0.012f, 0.08f, 6, LG, 0, -0.1f, -0.5f, PI / 2, 0, 0);
                b.cyl(0.075f, 0.075f, 0.03f, 16, c("2a2a2a"), 0, -0.1f, 0.1f, PI / 2, 0, 0);
                b.cyl(0.075f, 0.075f, 0.03f, 16, c("2a2a2a"), 0, -0.1f, -0.12f, PI / 2, 0, 0);
                for (float s : new float[]{-1, 1}) {
                    m.cyl(0.008f, 0.008f, 0.24f, 5, c("111111"), s * 0.08f, 0.25f, 0.27f, 0.6f, 0, s * 0.3f);
                    b.sph(0.02f, c("e04020"), s * 0.12f, 0.35f, 0.34f);
                }
                gl.sph(0.02f, new Color(1, 0.2f, 0.1f, 1), 0, 0.03f, 0.26f);
                r = 0.9f; sc = 2.2f; cam = V(0, 0.12f, -0.38f);
                break;
            }
            case "strizh": {
                float rx = PI / 2;
                b.cyl(0.11f, 0.11f, 1.2f, 18, LG, 0, 0, 0, rx, 0, 0);
                b.cyl(0f, 0.11f, 0.32f, 18, c("9aa0a6"), 0, 0, -0.76f, -PI / 2, 0, 0);
                b.cyl(0.11f, 0.05f, 0.25f, 18, c("7b8187"), 0, 0, 0.72f, rx, 0, 0);
                b.cyl(0.112f, 0.112f, 0.08f, 18, OLV, 0, 0, -0.42f, rx, 0, 0);
                b.cyl(0.112f, 0.112f, 0.04f, 18, c("c03020"), 0, 0, -0.32f, rx, 0, 0);
                gl.sph(0.05f, new Color(0.2f, 0.45f, 0.9f, 1), 0, -0.08f, -0.62f);
                b.sph(0.06f, c("1a2a3a"), 0, -0.07f, -0.6f, 1, 0.7f, 1.3f);
                for (float z : new float[]{-0.18f, 0.42f}) {
                    for (float a : new float[]{PI / 4, -PI / 4})
                        b.box(z < 0 ? 1.15f : 0.9f, 0.016f, z < 0 ? 0.2f : 0.17f, WH, 0, 0, z, 0, 0, a);
                    for (float a : new float[]{PI / 4, -PI / 4, 3 * PI / 4, -3 * PI / 4}) {
                        float k = z < 0 ? 0.55f : 0.43f;
                        b.box(0.03f, 0.03f, 0.06f, c("c03020"), MathUtils.cos(a) * k, MathUtils.sin(a) * k, z);
                    }
                }
                m.cyl(0.006f, 0.006f, 0.22f, 5, c("111111"), 0, 0.2f, 0.2f);
                m.cyl(0.04f, 0.04f, 0.08f, 10, MOT, 0, 0, 0.86f, rx, 0, 0);
                props.add(new float[]{0, 0, 0.92f, 0.24f, 1});
                gl.sph(0.025f, new Color(1, 0.2f, 0.1f, 1), 0, 0.12f, 0.6f);
                r = 1.1f; sc = 1.7f; cam = V(0, -0.1f, -0.75f);
                break;
            }
            case "taran": {
                b.cyl(0.3f, 0.34f, 0.16f, 8, c("3b4430"), 0, 0, 0);
                b.sph(0.26f, c("46503a"), 0, 0.08f, 0, 1, 0.45f, 1.15f);
                for (float s : new float[]{-1, 1}) b.box(0.04f, 0.16f, 0.42f, c("56603f"), s * 0.33f, 0.02f, 0, 0, 0, s * 0.2f);
                b.box(0.38f, 0.12f, 0.05f, c("56603f"), 0, 0.02f, -0.34f, 0.3f, 0, 0);
                for (int i = 0; i < 6; i++) {
                    float a = PI / 6 + i * PI / 3;
                    Vector3 p = V(MathUtils.cos(a) * 0.72f, 0.03f, MathUtils.sin(a) * 0.72f);
                    m.seg(V(MathUtils.cos(a) * 0.25f, 0.03f, MathUtils.sin(a) * 0.25f), p, 0.035f, 0.03f, 10, CF2);
                    m.cyl(0.075f, 0.08f, 0.1f, 14, MOT, p.x, p.y + 0.06f, p.z);
                    m.cyl(0.07f, 0.07f, 0.04f, 14, c("a03a20"), p.x, p.y + 0.12f, p.z);
                    props.add(new float[]{p.x, p.y + 0.17f, p.z, 0.32f, 0});
                }
                float rx = PI / 2;
                b.cyl(0.15f, 0.15f, 0.55f, 18, OLV, 0, -0.24f, -0.05f, rx, 0, 0);
                b.cyl(0.155f, 0.155f, 0.06f, 18, c("d07a20"), 0, -0.24f, -0.2f, rx, 0, 0);
                b.cyl(0.04f, 0.15f, 0.18f, 18, OLV2, 0, -0.24f, -0.41f, -PI / 2, 0, 0);
                m.cyl(0.015f, 0.015f, 0.12f, 6, LG, 0, -0.24f, -0.55f, rx, 0, 0);
                for (float s : new float[]{-1, 1}) {
                    m.seg(V(s * 0.18f, -0.05f, -0.15f), V(s * 0.3f, -0.42f, -0.2f), 0.015f, 0.015f, 6, CF2);
                    m.seg(V(s * 0.18f, -0.05f, 0.15f), V(s * 0.3f, -0.42f, 0.2f), 0.015f, 0.015f, 6, CF2);
                    m.seg(V(s * 0.3f, -0.42f, -0.32f), V(s * 0.3f, -0.42f, 0.32f), 0.018f, 0.018f, 6, CF2);
                }
                b.box(0.1f, 0.08f, 0.08f, c("222222"), 0, 0, -0.36f);
                gl.sph(0.025f, new Color(0.3f, 0.6f, 1f, 1), 0, 0, -0.41f);
                for (float s : new float[]{-1, 1}) gl.sph(0.025f, s < 0 ? new Color(1, 0.2f, 0.1f, 1) : new Color(0.2f, 1, 0.3f, 1), s * 0.3f, 0.05f, 0.25f);
                r = 1.5f; sc = 2.3f; cam = V(0, 0.02f, -0.45f);
                break;
            }
            default: {
                b.box(0.34f, 0.12f, 0.62f, c("33383d"), 0, 0, 0);
                b.box(0.28f, 0.06f, 0.5f, c("4a5056"), 0, 0.09f, 0);
                b.sph(0.12f, c("2a2e32"), 0, 0.02f, -0.33f, 1.2f, 0.8f, 1);
                gl.sph(0.03f, new Color(0.3f, 0.6f, 1f, 1), 0, 0, -0.44f);
                for (int i = 0; i < 4; i++) {
                    float a = PI / 4 + i * PI / 2;
                    Vector3 p = V(MathUtils.cos(a) * 0.62f, 0.02f, MathUtils.sin(a) * 0.62f);
                    m.seg(V(MathUtils.cos(a) * 0.15f, 0.02f, MathUtils.sin(a) * 0.15f), p, 0.03f, 0.026f, 10, CF);
                    m.cyl(0.06f, 0.06f, 0.24f, 14, MOT, p.x, p.y, p.z);
                    m.cyl(0.058f, 0.058f, 0.04f, 14, c("e0b020"), p.x, p.y + 0.1f, p.z);
                    props.add(new float[]{p.x, p.y + 0.15f, p.z, 0.3f, 0});
                    props.add(new float[]{p.x, p.y - 0.14f, p.z, 0.3f, 0});
                }
                b.box(0.32f, 0.03f, 0.4f, CF2, 0, -0.1f, 0);
                for (float x : new float[]{-0.12f, 0f, 0.12f}) m.box(0.015f, 0.06f, 0.38f, LG, x, -0.13f, 0);
                b.box(0.1f, 0.1f, 0.18f, BAT, 0, 0.16f, 0.12f);
                for (float s : new float[]{-1, 1}) m.cyl(0.007f, 0.007f, 0.2f, 5, c("111111"), s * 0.12f, 0.2f, 0.28f, 0.5f, 0, s * 0.3f);
                r = 1.2f; sc = 2.1f; cam = V(0, 0, -0.46f);
            }
        }
        Node3 g = new Node3();
        Node3 body = new Node3(b.mesh(), Models.MAT_HULL); body.castShadow = true; g.add(body);
        if (!m.empty()) { Node3 mm = new Node3(m.mesh(), Models.MAT_METAL); mm.castShadow = true; g.add(mm); }
        if (!gl.empty()) g.add(new Node3(gl.mesh(), Models.MAT_GLOW));
        for (float[] p : props) {
            Node3 n = new Node3(propMesh(), Models.MAT_ROTOR);
            n.name = "rot";
            n.pos.set(p[0], p[1], p[2]); n.scl.set(p[3], 1, p[3]);
            if (p[4] > 0) n.setEuler(PI / 2, 0, 0);
            g.add(n);
        }
        if (kind.equals("shershen")) {
            for (float x : new float[]{-0.12f, 0f, 0.12f}) {
                Node3 bm = new Node3(bombMesh(), Models.MAT_HULL);
                bm.name = "bomb";
                bm.pos.set(x, -0.26f, 0);
                g.add(bm);
            }
        }
        MB.hq = false;
        g.scl.set(sc, sc, sc);
        DroneTpl t = new DroneTpl();
        t.g = g; t.r = r; t.sc = sc; t.cam = cam;
        return t;
    }

    static Mesh3 bomb;

    static Mesh3 bombMesh() {
        if (bomb != null) return bomb;
        MB b = new MB();
        float rx = PI / 2;
        b.cyl(0.045f, 0.045f, 0.22f, 12, OLV, 0, 0, 0, rx, 0, 0);
        b.sph(0.045f, OLV2, 0, 0, -0.11f, 1, 1, 1.4f);
        b.cyl(0.045f, 0.02f, 0.08f, 12, c("333333"), 0, 0, 0.15f, rx, 0, 0);
        for (int i = 0; i < 4; i++) {
            float a = i * PI / 2;
            b.box(0.005f, 0.08f, 0.07f, c("333333"), MathUtils.cos(a) * 0.04f, MathUtils.sin(a) * 0.04f, 0.17f, 0, 0, a);
        }
        bomb = b.mesh();
        return bomb;
    }

    public static Node3 bombFree() {
        Node3 n = new Node3();
        Node3 m = new Node3(bombMesh(), Models.MAT_HULL);
        n.add(m);
        n.scl.set(2.1f, 2.1f, 2.1f);
        return n;
    }

    public static final class Target { public Node3 g, spin; public float r, hr; }

    static final Map<String, Target> tgCache = new HashMap<>();

    public static Target target(String kind, String theme) {
        String key = kind + theme;
        Target c = tgCache.get(key);
        if (c == null) {
            Object[] o = Targets.build(kind, theme);
            c = new Target(); c.g = (Node3) o[0]; c.r = (Float) o[2]; c.hr = (Float) o[3];
            if (o[1] != null) ((Node3) o[1]).name = "spin";
            tgCache.put(key, c);
        }
        Target t = new Target(); t.g = c.g.cloneTree(); t.r = c.r; t.hr = c.hr;
        t.spin = t.g.find("spin");
        return t;
    }

    private SModels() { }
}
