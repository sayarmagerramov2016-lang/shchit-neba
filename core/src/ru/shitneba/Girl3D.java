package ru.shitneba;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.shitneba.gfx.MB;
import ru.shitneba.gfx.Mat;
import ru.shitneba.gfx.Node3;

/**
 * 3D-операторы (эскиз, в игру не подключено): чиби-девчонки из сфер и капсул,
 * лицо — картинка с глазами и ртом (assets/ops3d, рисует store/tools/make_ops_soft.py), натянутая на голову.
 * Рост ~1.25 единицы, ноги на y = 0, смотрит в −Z (как все модели игры).
 */
public final class Girl3D {
    public static final class Model {
        public Node3 root, head, armR, armL, face;
        public String id;
        public Mat faceMat;
        public void emotion(String e) { faceMat.tex = faceTex(id, e); }
    }

    static final class Look {
        String hair, style; String[] ex; String[][] o;
        Look(String hair, String style, String ex, String[] o0, String[] o1) { this.hair = hair; this.style = style; this.ex = ex.split(","); this.o = new String[][]{o0, o1}; }
        boolean has(String e) { return Arrays.asList(ex).contains(e); }
    }

    // o = top, acc, bot, legs, shoes, kind — те же цвета, что в make_ops_soft.py
    static final Map<String, Look> L = new HashMap<>();
    static {
        L.put("liza", new Look("8a5432", "pony", "wrench,cap", new String[]{"7a8a52", "f08a2a", "5d6b40", "4a4038", "5a3c26", "overall"}, new String[]{"24356a", "e8c25a", "1c2a56", "2a2a3e", "1e1e2a", "overall"}));
        L.put("katya", new Look("f4d27a", "long", "cup", new String[]{"efdcb4", "c8603e", "5a8a54", "f8f2ea", "8a5232", "sweater"}, new String[]{"f8c2d2", "ffffff", "d06a88", "ffffff", "d06a88", "sweater"}));
        L.put("sonya", new Look("525866", "bob", "scarf", new String[]{"687a4a", "9aaa66", "55613e", "424a30", "33332c", "camo"}, new String[]{"e6ecf0", "a6b4c0", "cad4dc", "b4c0ca", "64707a", "camo"}));
        L.put("anya", new Look("9cd8f4", "twin", "phones", new String[]{"2a3c6c", "59e0ff", "22325a", "24243a", "16162a", "uniform"}, new String[]{"f4f8fc", "2fb8e8", "64b0e0", "eef4fa", "3478b0", "uniform"}));
        L.put("masha", new Look("f49446", "short", "cat,bell", new String[]{"e04a42", "ffffff", "f8f4ee", "ffffff", "a83028", "sailor"}, new String[]{"2e2a34", "e8c25a", "423c48", "2e2a34", "e8c25a", "sailor"}));
        L.put("rin", new Look("ff5ac0", "side", "visor", new String[]{"24202e", "ff4fd8", "2e2a3a", "1a1822", "ff4fd8", "cyber"}, new String[]{"18222c", "3ff0ff", "1e2a38", "121a22", "3ff0ff", "cyber"}));
        L.put("yuki", new Look("f2f7ff", "long", "flake", new String[]{"c6e6ff", "ffffff", "96ccf6", "eef8ff", "5e9edc", "dress"}, new String[]{"2c4a86", "c6e6ff", "223a70", "dceafa", "c6e6ff", "dress"}));
        L.put("elka", new Look("44a856", "buns", "garland,starpin", new String[]{"36964a", "ffd23a", "2a7c40", "ea4444", "922626", "dress"}, new String[]{"e04444", "ffffff", "c23232", "ffffff", "36964a", "dress"}));
        L.put("akira", new Look("2c2834", "highpony", "katana", new String[]{"f8f4ee", "d0303f", "b82634", "2e2a34", "32262e", "hakama"}, new String[]{"342a3c", "a466ea", "221c2c", "1a1620", "a466ea", "hakama"}));
        L.put("sneg", new Look("eaf0fa", "braid", "kokoshnik", new String[]{"62a8ee", "ffffff", "5294de", "ffffff", "ffffff", "coat"}, new String[]{"ccd4e0", "ffbcd4", "b0bacb", "ffffff", "ffbcd4", "coat"}));
        L.put("valk", new Look("ffdc72", "long", "wings,halo,tiara", new String[]{"f8f4ea", "eabc36", "efe8d6", "f8f4ea", "eabc36", "armor"}, new String[]{"302840", "a676ff", "241e30", "302840", "a676ff", "armor"}));
    }

    static final Color SKIN = c("ffe6d8");
    static final Map<String, Texture> TEX = new HashMap<>();
    static Mat BODY_MAT, GLOW_MAT;
    static final Matrix4 M = new Matrix4();

    /** Цвет вершин шейдер считает линейным — переводим из sRGB. */
    static Color c(String h) {
        Color x = G.hex(h);
        return new Color((float) Math.pow(x.r, 2.2), (float) Math.pow(x.g, 2.2), (float) Math.pow(x.b, 2.2), 1);
    }

    static Color dk(Color a, float k) { return new Color(a.r * (1 - k), a.g * (1 - k), a.b * (1 - k), 1); }

    static Color lt(Color a, float k) { return new Color(a.r + (1 - a.r) * k, a.g + (1 - a.g) * k, a.b + (1 - a.b) * k, 1); }

    static Texture faceTex(String id, String e) {
        String k = id + "_" + e;
        Texture t = TEX.get(k);
        if (t == null) {
            t = new Texture(Gdx.files.internal("ops3d/" + id + "_face_" + e + ".png"), true);
            t.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
            TEX.put(k, t);
        }
        return t;
    }

    // голова: радиус и центр
    static final float HR = 0.30f, HY = 0.93f;

    public static Model build(String id, int outfit) {
        if (BODY_MAT == null) {
            BODY_MAT = Mat.vc(); BODY_MAT.rough = 0.75f; BODY_MAT.spec = 0.35f; BODY_MAT.dust = 0f;
            GLOW_MAT = Mat.vc(); GLOW_MAT.unshaded = true; GLOW_MAT.dust = 0f;
        }
        boolean hq = MB.hq; MB.hq = true;
        Look lk = L.get(id);
        String[] o = lk.o[outfit];
        Color top = c(o[0]), acc = c(o[1]), bot = c(o[2]), legs = c(o[3]), shoes = c(o[4]);
        String kind = o[5];
        Color hair = c(lk.hair), hairD = dk(hair, 0.18f);
        Model m = new Model();
        m.id = id;
        m.root = new Node3();

        // ---------- тело ----------
        MB b = new MB();
        for (int s = -1; s <= 1; s += 2) {
            b.sph(0.062f, shoes, s * 0.068f, 0.045f, -0.02f, 1f, 0.75f, 1.45f, 12);
            b.cyl(0.042f, 0.046f, 0.24f, 12, legs, s * 0.068f, 0.2f, 0);
        }
        switch (kind) {
            case "overall": case "uniform": case "camo": case "cyber":
                b.cyl(0.13f, 0.15f, 0.14f, 16, bot, 0, 0.36f, 0);
                break;
            case "hakama":
                b.cyl(0.13f, 0.22f, 0.3f, 18, bot, 0, 0.27f, 0);
                break;
            case "coat":
                b.cyl(0.14f, 0.22f, 0.3f, 18, top, 0, 0.28f, 0);
                b.torus(0.2f, 0.025f, c("ffffff"), 0, 0.135f, 0);
                break;
            default:
                b.cyl(0.13f, 0.23f, 0.18f, 18, bot, 0, 0.35f, 0);
        }
        b.sph(0.15f, top, 0, 0.5f, 0, 1f, 1.1f, 0.85f, 14);
        switch (kind) {
            case "overall":
                b.sph(0.151f, bot, 0, 0.47f, -0.002f, 0.92f, 0.75f, 0.85f, 14);
                b.box(0.025f, 0.12f, 0.02f, bot, -0.06f, 0.58f, -0.11f, 0.15f, 0, 0);
                b.box(0.025f, 0.12f, 0.02f, bot, 0.06f, 0.58f, -0.11f, 0.15f, 0, 0);
                break;
            case "sailor":
                b.cyl(0.13f, 0.17f, 0.04f, 16, acc, 0, 0.62f, 0);
                b.sph(0.03f, c("ea4242"), 0, 0.57f, -0.13f, 1.3f, 0.8f, 0.6f, 8);
                break;
            case "uniform":
                b.box(0.025f, 0.1f, 0.01f, acc, 0, 0.53f, -0.128f);
                break;
            case "cyber":
                b.torus(0.14f, 0.008f, acc, 0, 0.47f, 0, 0, 0, 0);
                b.torus(0.12f, 0.008f, acc, 0, 0.56f, 0, 0, 0, 0);
                break;
            case "hakama":
                b.box(0.09f, 0.1f, 0.01f, acc, 0, 0.56f, -0.125f, 0, 0, 0);
                b.cyl(0.155f, 0.155f, 0.04f, 16, acc, 0, 0.42f, 0);
                break;
            case "armor":
                b.sph(0.13f, acc, 0, 0.53f, -0.025f, 0.95f, 0.85f, 0.75f, 12);
                b.sph(0.06f, acc, -0.15f, 0.6f, 0, 1, 0.8f, 1, 10);
                b.sph(0.06f, acc, 0.15f, 0.6f, 0, 1, 0.8f, 1, 10);
                break;
            case "dress":
                b.cyl(0.152f, 0.152f, 0.03f, 16, acc, 0, 0.42f, 0);
                break;
            case "sweater":
                b.cyl(0.152f, 0.152f, 0.025f, 16, acc, 0, 0.58f, 0);
                break;
            case "coat":
                b.box(0.03f, 0.3f, 0.01f, c("ffffff"), 0, 0.42f, -0.13f, 0.12f, 0, 0);
                b.torus(0.12f, 0.03f, c("ffffff"), 0, 0.63f, 0);
                break;
            default: break;
        }
        if (lk.has("scarf")) b.torus(0.075f, 0.03f, c(o[0].equals("e6ecf0") ? "5e7e9e" : "b84e3c"), 0, 0.64f, 0);
        if (lk.has("bell")) b.sph(0.022f, c("eac85a"), 0, 0.61f, -0.12f);
        if (lk.has("garland")) for (int i = 0; i < 7; i++) {
            float a = MathUtils.lerp(-1f, 1f, i / 6f);
            String[] cs = {"ff4040", "ffd23a", "40a0ff", "ff60e0"};
            b.sph(0.014f, c(cs[i % 4]), MathUtils.sin(a) * 0.135f, 0.54f - (1 - a * a) * 0.04f, -MathUtils.cos(a) * 0.115f);
        }
        b.cyl(0.04f, 0.042f, 0.06f, 10, SKIN, 0, 0.64f, 0);
        if (lk.has("katana")) {
            b.seg(new Vector3(-0.2f, 0.25f, 0.16f), new Vector3(0.22f, 0.85f, 0.17f), 0.016f, 0.016f, 8, c("3a2a3a"));
            b.seg(new Vector3(0.1f, 0.67f, 0.17f), new Vector3(0.13f, 0.71f, 0.17f), 0.035f, 0.035f, 10, c("e8c25a"));
        }
        Node3 body = new Node3(b.mesh(), BODY_MAT);
        m.root.add(body);
        if (lk.has("wings")) {
            MB w = new MB();
            Color wc = outfit == 0 ? c("ffffff") : c("433460");
            for (int s = -1; s <= 1; s += 2) {
                for (int i = 0; i < 4; i++) {
                    float a = 0.5f + i * 0.28f;
                    w.sph(0.17f - i * 0.02f, i == 0 ? wc : lt(wc, 0.05f * i), s * (0.14f + MathUtils.cos(a) * 0.17f), 0.6f + MathUtils.sin(a) * 0.17f, 0.14f,
                            0.4f, 1.3f, 0.18f, 12);
                }
            }
            m.root.add(new Node3(w.mesh(), BODY_MAT));
        }

        // ---------- руки ----------
        Color sl = kind.equals("armor") ? acc : top;
        m.armR = new Node3(); m.armR.pos.set(-0.15f, 0.6f, 0);
        MB ar = new MB();
        ar.capsule(0.04f, 0.13f, sl, -0.085f, 0, 0, 0, 0, MathUtils.PI / 2);
        ar.sph(0.042f, SKIN, -0.19f, 0, 0, 1, 0.95f, 1, 12);
        ar.capsule(0.013f, 0.05f, SKIN, -0.245f, 0.005f, 0, 0, 0, MathUtils.PI / 2);
        m.armR.add(new Node3(ar.mesh(), BODY_MAT));
        m.armR.setEuler(0, 0.5f, -0.9f); // по умолчанию показывает вверх-вперёд
        m.root.add(m.armR);
        m.armL = new Node3(); m.armL.pos.set(0.15f, 0.6f, 0);
        MB al = new MB();
        al.capsule(0.04f, 0.12f, sl, 0.02f, -0.07f, -0.02f, 0.4f, 0, 0.35f);
        al.sph(0.042f, SKIN, -0.02f, -0.15f, -0.09f, 1, 0.95f, 1, 12);
        if (lk.has("cup")) al.cyl(0.035f, 0.03f, 0.07f, 12, c("ffffff"), -0.02f, -0.12f, -0.14f);
        if (lk.has("wrench")) {
            al.seg(new Vector3(-0.02f, -0.15f, -0.12f), new Vector3(0.03f, -0.32f, -0.16f), 0.012f, 0.012f, 8, c("aab2ba"));
            al.torus(0.025f, 0.009f, c("aab2ba"), 0.035f, -0.34f, -0.165f, MathUtils.PI / 2, 0, 0);
        }
        m.armL.add(new Node3(al.mesh(), BODY_MAT));
        m.root.add(m.armL);

        // ---------- голова ----------
        m.head = new Node3(); m.head.pos.set(0, HY, 0);
        MB hd = new MB();
        hd.sph(HR, SKIN, 0, 0, 0, 1.04f, 0.97f, 1f, 22);
        for (int s = -1; s <= 1; s += 2) hd.sph(0.05f, SKIN, s * HR * 1.02f, -0.03f, 0.01f, 0.5f, 0.9f, 0.7f, 8);
        m.head.add(new Node3(hd.mesh(), BODY_MAT));
        // лицо
        Mat fm = new Mat(); fm.vcol = false; fm.tex = faceTex(id, "norm"); fm.blend = Mat.ALPHA; fm.depthWrite = false; fm.rough = 0.9f; fm.order = 3; fm.dust = 0f;
        m.faceMat = fm;
        // края картинки лица (make_ops_soft: FACE 48,142,160,108; центр головы 128,156; радиусы 92×90)
        float fx0 = (48 - 128) / 92f, fx1 = (208 - 128) / 92f, fy0 = -(142 - 156) / 90f, fy1 = -(250 - 156) / 90f;
        Node3 face = new Node3(); face.scl.set(1.04f, 0.97f, 1f);
        MB fb = new MB();
        fb.add(MB.pFace(HR * 1.006f, fx0, fx1, fy0, fy1, 14), Color.WHITE, M.idt());
        face.mesh = fb.mesh(); face.mat = fm;
        m.head.add(face);
        m.face = face;
        // волосы
        MB hb = new MB();
        hb.add(MB.pHairCap(26, 14, 0.25f, 0.28f), hair, MB.xf(M, 0, 0.012f, 0.005f, 0, 0, 0, HR * 1.1f, HR * 1.06f, HR * 1.09f));
        // чёлка — пряди
        for (int i = 0; i < 9; i++) {
            float a = MathUtils.lerp(-0.95f, 0.95f, i / 8f);
            float len = 0.15f + ((i % 2 == 0) ? 0.03f : -0.01f);
            float x = MathUtils.sin(a) * HR * 1.02f, z = -MathUtils.cos(a) * HR * 1.04f;
            M.idt().translate(x, HR * 0.35f, z).rotateRad(Vector3.Y, -a).rotateRad(Vector3.X, -0.42f).scale(1f, 1f, 0.45f);
            hb.add(MB.pCyl(0.055f, 0.004f, len, 10, true), i % 2 == 0 ? hair : lt(hair, 0.05f), M.translate(0, -len / 2, 0));
        }
        // боковые пряди
        for (int s = -1; s <= 1; s += 2) {
            M.idt().translate(s * HR * 0.95f, -0.06f, -0.1f).rotateRad(Vector3.Z, s * 0.08f).scale(0.7f, 1f, 0.6f);
            hb.add(MB.pCyl(0.06f, 0.03f, 0.3f, 10, true), hair, M);
        }
        String st = lk.style;
        switch (st) {
            case "long": case "braid":
                hb.sph(HR * 1.02f, hairD, 0, -0.2f, 0.1f, 1.08f, 1.2f, 0.55f, 18);
                break;
            case "twin":
                for (int s = -1; s <= 1; s += 2) {
                    hb.seg(new Vector3(s * 0.31f, 0.12f, 0.06f), new Vector3(s * 0.4f, -0.55f, 0.1f), 0.075f, 0.045f, 12, hair);
                    hb.sph(0.076f, hair, s * 0.31f, 0.12f, 0.06f);
                    hb.sph(0.035f, c("ff6a8a"), s * 0.3f, 0.15f, 0.04f);
                }
                break;
            case "pony":
                hb.seg(new Vector3(0, 0.05f, 0.31f), new Vector3(0.05f, -0.45f, 0.42f), 0.08f, 0.03f, 12, hairD);
                break;
            case "highpony":
                hb.seg(new Vector3(0, 0.3f, 0.22f), new Vector3(0.05f, -0.6f, 0.42f), 0.1f, 0.03f, 12, hairD);
                hb.sph(0.045f, c("d0303f"), 0, 0.32f, 0.2f);
                hb.sph(HR * 1.02f, hairD, 0, -0.08f, 0.08f, 1.06f, 0.9f, 0.6f, 16);
                break;
            case "side":
                hb.seg(new Vector3(0.28f, 0.12f, 0.12f), new Vector3(0.38f, -0.5f, 0.14f), 0.08f, 0.035f, 12, hair);
                hb.sph(0.04f, c("46f0ff"), 0.28f, 0.14f, 0.1f);
                break;
            case "bob":
                hb.sph(HR * 1.02f, hairD, 0, -0.06f, 0.1f, 1.12f, 0.8f, 0.75f, 18);
                break;
            case "buns":
                hb.sph(0.11f, hair, -0.2f, 0.25f, 0.06f); hb.sph(0.11f, hair, 0.2f, 0.25f, 0.06f);
                hb.sph(HR * 1.02f, hairD, 0, -0.1f, 0.06f, 1.06f, 0.9f, 0.7f, 16);
                break;
            default: break;
        }
        if (st.equals("braid")) for (int i = 0; i < 6; i++) hb.sph(0.05f - i * 0.003f, hair, 0.24f + i * 0.01f, -0.25f - i * 0.075f, 0.12f);
        // головные украшения
        if (lk.has("cap")) {
            hb.add(MB.pSph(1, 2, 20, 10), top, MB.xf(M, 0, 0.08f, 0, 0, 0, 0, HR * 1.13f, HR * 0.9f, HR * 1.13f));
            hb.cyl(HR * 1.2f, HR * 1.2f, 0.02f, 20, dk(top, 0.15f), 0, 0.06f, -0.05f);
            hb.sph(0.03f, acc, 0, 0.3f, -0.23f);
        }
        if (lk.has("phones")) {
            for (int i = 0; i < 8; i++) { // дуга наушников поверх головы, от уха до уха
                float a0 = MathUtils.PI * i / 8f, a1 = MathUtils.PI * (i + 1) / 8f;
                hb.seg(new Vector3(MathUtils.cos(a0) * HR * 1.14f, -0.02f + MathUtils.sin(a0) * HR * 1.14f, 0.02f),
                        new Vector3(MathUtils.cos(a1) * HR * 1.14f, -0.02f + MathUtils.sin(a1) * HR * 1.14f, 0.02f), 0.016f, 0.016f, 6, c("2c2c3a"));
            }
            for (int s = -1; s <= 1; s += 2) {
                hb.cyl(0.07f, 0.07f, 0.06f, 16, c("2c2c3a"), s * HR * 1.12f, -0.02f, 0, 0, 0, MathUtils.PI / 2);
                hb.cyl(0.05f, 0.05f, 0.062f, 16, acc, s * HR * 1.13f, -0.02f, 0, 0, 0, MathUtils.PI / 2);
            }
            hb.seg(new Vector3(-0.33f, -0.05f, 0), new Vector3(-0.15f, -0.2f, -0.24f), 0.008f, 0.008f, 6, c("2c2c3a"));
            hb.sph(0.02f, acc, -0.15f, -0.2f, -0.25f);
        }
        if (lk.has("cat")) for (int s = -1; s <= 1; s += 2) {
            M.idt().translate(s * 0.17f, 0.28f, 0.02f).rotateRad(Vector3.Z, -s * 0.35f).scale(1, 1, 0.5f);
            hb.add(MB.pCyl(0.002f, 0.08f, 0.14f, 4, true), hair, M);
            M.idt().translate(s * 0.168f, 0.27f, -0.02f).rotateRad(Vector3.Z, -s * 0.35f).scale(1, 1, 0.2f);
            hb.add(MB.pCyl(0.002f, 0.05f, 0.1f, 4, true), c("ffc6cf"), M);
        }
        if (lk.has("visor")) hb.add(MB.pCyl(1, 1, 1, 24, false), acc, MB.xf(M, 0, 0.11f, 0, 0, 0, 0, HR * 1.13f, 0.03f, HR * 1.13f));
        if (lk.has("flake")) for (int i = 0; i < 3; i++) hb.box(0.09f, 0.012f, 0.012f, c("5ab0ff"), 0.2f, 0.17f, -0.19f, 0, 0, i * MathUtils.PI / 3);
        if (lk.has("starpin")) hb.sph(0.035f, c("ffd23a"), 0.2f, 0.18f, -0.2f, 1, 1, 0.5f, 6);
        if (lk.has("kokoshnik")) {
            M.idt().translate(0, 0.25f, -0.06f).rotateRad(Vector3.X, -0.2f).scale(1.05f, 1.15f, 0.3f);
            hb.add(MB.pSph(HR, 2 * HR, 20, 10), top, M);
            hb.sph(0.03f, acc, 0, 0.33f, -0.16f); hb.sph(0.022f, acc, -0.11f, 0.28f, -0.15f); hb.sph(0.022f, acc, 0.11f, 0.28f, -0.15f);
        }
        if (lk.has("tiara")) {
            hb.torus(HR * 0.95f, 0.012f, acc, 0, 0.22f, 0.02f, -0.15f, 0, 0);
            hb.sph(0.03f, c("ff6a8a"), 0, 0.27f, -0.27f, 1, 1.2f, 0.6f, 8);
        }
        m.head.add(new Node3(hb.mesh(), BODY_MAT));
        if (lk.has("halo")) {
            MB hl = new MB();
            hl.torus(0.16f, 0.014f, c("fff2a0"), 0, 0.42f, 0.04f, 0.25f, 0, 0);
            m.head.add(new Node3(hl.mesh(), GLOW_MAT));
        }
        m.root.add(m.head);
        MB.hq = hq;
        m.root.forEach(n -> n.castShadow = n.mesh != null && n.mat != m.faceMat);
        return m;
    }

    public static final List<String> IDS = Arrays.asList("liza", "katya", "sonya", "anya", "masha", "rin", "yuki", "elka", "akira", "sneg", "valk");

    private Girl3D() { }
}
