package ru.shitneba;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Matrix4;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import ru.shitneba.gfx.MB;
import ru.shitneba.gfx.Mat;
import ru.shitneba.gfx.Mesh3;
import ru.shitneba.gfx.Node3;

/**
 * 3D-операторы: модели из Blender (store/tools/blender_ops.py, режим GAME=1) → assets/ops3d/&lt;id&gt;_&lt;наряд&gt;.ops.
 * Детали жёстко привязаны к костям: root → spine → head, arm.R, arm.L — двигаем узлы и получаем анимацию.
 * Лицо — картинка с глазами и ртом (assets/ops3d/&lt;id&gt;_face_&lt;эмоция&gt;.png), эмоция меняется заменой текстуры.
 * Рост ~1.27, ноги на y = 0, смотрит в −Z (как все модели игры).
 */
public final class Girl3D {
    public static final class Model {
        public Node3 root, spine, head, armR, armL, shadow;
        public String id;
        public Mat faceMat;
        public void emotion(String e) { faceMat.tex = faceTex(id, e); }
    }

    public static final List<String> IDS = Arrays.asList("liza", "katya", "sonya", "anya", "masha", "rin", "yuki", "elka", "akira", "sneg", "valk");
    /** Высота макушки — для подгонки камеры. */
    public static final float TOP = 1.27f;

    static final Map<String, Texture> TEX = new HashMap<>();
    static Mat BODY_MAT, GLOW_MAT, SHADOW_MAT;

    public static Texture faceTex(String id, String e) {
        String k = id + "_" + e;
        Texture t = TEX.get(k);
        if (t == null) {
            t = new Texture(Gdx.files.internal("ops3d/" + id + "_face_" + e + ".png"), true);
            t.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
            TEX.put(k, t);
        }
        return t;
    }

    static void mats() {
        if (BODY_MAT != null) return;
        BODY_MAT = Mat.vc(); BODY_MAT.rough = 0.6f; BODY_MAT.spec = 0.4f; BODY_MAT.dust = 0f;
        GLOW_MAT = Mat.vc(); GLOW_MAT.unshaded = true; GLOW_MAT.dust = 0f;
        SHADOW_MAT = Mat.std(new Color(0, 0, 0, 1)); SHADOW_MAT.unshaded = true; SHADOW_MAT.blend = Mat.ALPHA; SHADOW_MAT.alpha = 0.35f;
        SHADOW_MAT.depthWrite = false; SHADOW_MAT.fog = false; SHADOW_MAT.cull = false;
    }

    static final float[] LIN = new float[256];
    static { for (int i = 0; i < 256; i++) LIN[i] = (float) Math.pow(i / 255f, 2.2); }

    public static Model build(String id, int outfit) {
        mats();
        Model m = new Model();
        m.id = id;
        m.root = new Node3();
        m.spine = new Node3(); m.spine.pos.set(0, 0.3f, 0); m.root.add(m.spine);
        m.head = new Node3(); m.head.pos.set(0, 0.38f, 0); m.spine.add(m.head);
        // кости рук в осях игры: Blender (∓0.15, 0, 0.6) → (±0.15, 0.6, 0), относительно spine
        m.armR = new Node3(); m.armR.pos.set(0.15f, 0.3f, 0); m.spine.add(m.armR);
        m.armL = new Node3(); m.armL.pos.set(-0.15f, 0.3f, 0); m.spine.add(m.armL);
        Map<String, Node3> bones = new HashMap<>();
        bones.put("root", m.root); bones.put("spine", m.spine); bones.put("head", m.head); bones.put("arm.R", m.armR); bones.put("arm.L", m.armL);
        Mat fm = new Mat(); fm.tex = faceTex(id, "norm"); fm.blend = Mat.ALPHA; fm.depthWrite = false; fm.rough = 0.8f; fm.order = 3; fm.dust = 0f;
        m.faceMat = fm;
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(
                Gdx.files.internal("ops3d/" + id + "_" + outfit + ".ops").read()), 1 << 16))) {
            byte[] magic = new byte[4];
            in.readFully(magic);
            int ng = in.readUnsignedShort();
            for (int gi = 0; gi < ng; gi++) {
                byte[] nb = new byte[in.readUnsignedByte()];
                in.readFully(nb);
                String bone = new String(nb, "UTF-8");
                int kind = in.readUnsignedByte();
                int nv = in.readInt(), ni = in.readInt();
                float[] vv = new float[nv * 12];
                for (int i = 0; i < nv; i++) {
                    int o = i * 12;
                    vv[o] = in.readShort() / 8192f; vv[o + 1] = in.readShort() / 8192f; vv[o + 2] = in.readShort() / 8192f;
                    vv[o + 3] = in.readByte() / 127f; vv[o + 4] = in.readByte() / 127f; vv[o + 5] = in.readByte() / 127f;
                    vv[o + 6] = LIN[in.readUnsignedByte()]; vv[o + 7] = LIN[in.readUnsignedByte()]; vv[o + 8] = LIN[in.readUnsignedByte()]; vv[o + 9] = 1;
                    if (kind == 2) { vv[o + 10] = in.readUnsignedShort() / 65535f; vv[o + 11] = in.readUnsignedShort() / 65535f; }
                }
                float[] out = new float[ni * 12];
                boolean wide = nv > 65535;
                for (int t = 0; t < ni; t += 3) {
                    int a = wide ? in.readInt() : in.readUnsignedShort();
                    int b = wide ? in.readInt() : in.readUnsignedShort();
                    int c = wide ? in.readInt() : in.readUnsignedShort();
                    // в Blender лицевая сторона против часовой, в игре — по часовой
                    System.arraycopy(vv, a * 12, out, t * 12, 12);
                    System.arraycopy(vv, c * 12, out, (t + 1) * 12, 12);
                    System.arraycopy(vv, b * 12, out, (t + 2) * 12, 12);
                }
                Node3 n = new Node3(new Mesh3(out, out.length), kind == 2 ? fm : (kind == 1 ? GLOW_MAT : BODY_MAT));
                n.castShadow = kind != 2;
                Node3 parent = bones.get(bone);
                (parent != null ? parent : m.root).add(n);
            }
        } catch (Exception e) {
            BlackBox.log("не загрузилась 3D-девчонка " + id + ": " + e);
        }
        MB b = new MB();
        b.add(MB.pSph(1, 2, 20, 4), Color.WHITE, new Matrix4().scale(0.26f, 0.004f, 0.2f));
        m.shadow = new Node3(b.mesh(), SHADOW_MAT);
        m.shadow.pos.y = 0.005f;
        m.root.add(m.shadow);
        return m;
    }

    private Girl3D() { }
}
