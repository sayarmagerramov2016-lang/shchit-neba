package ru.shitneba.gfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Sort;

import java.util.Comparator;

/** Отрисовка сцены: небо, тени от солнца, непрозрачные/прозрачные меши, динамические батчи. */
public class Renderer implements Disposable {
    public final Shaders sh = new Shaders();
    public final Env env = new Env();
    public Camera cam;
    public boolean shadows = true;
    public final Vector3 shadowFocus = new Vector3();
    public float shadowRange = 70f;
    /** Масштаб 3D-картинки (1 = полное разрешение экрана). */
    public float scale = 1f;
    /** Обновлять карту теней раз в N кадров. */
    public int shadowEvery = 1;
    int frame;
    public boolean dumpNext;
    public int statOpq, statTrn, statCast, statTris;
    FrameBuffer sceneFbo;
    Mesh blitQuad;
    ShaderProgram blitProg;

    final Mesh skyQuad;
    FrameBuffer shadowFbo;
    final OrthographicCamera sunCam = new OrthographicCamera();
    final Matrix4 svp = new Matrix4();
    final Matrix3 nmat = new Matrix3();
    final Vector3 tv = new Vector3(), tv2 = new Vector3();
    final Matrix4 ivp = new Matrix4();
    final Matrix4 ident = new Matrix4();

    public static final class Env {
        public final Vector3 sunDir = new Vector3(0.3f, 0.5f, -0.8f).nor();
        public final Color sunCol = new Color(1, 1, 1, 1);
        public float sunEnergy = 1.45f;
        public final Color amb = new Color(0.5f, 0.5f, 0.6f, 1);
        public float ambEnergy = 0.95f;
        public final Color fogCol = new Color(0.6f, 0.6f, 0.7f, 1);
        public float fogD = 0.00042f;
        public float exposure = 1f, contrast = 1.07f, saturation = 1.08f;
        public final Color[] sky = {new Color(), new Color(), new Color(), new Color()};
        public final Color skySun = new Color(1, 0.8f, 0.7f, 1);
        public float sunOn = 1f, skyFog = 0.15f, flash = 0f;
        /** Точечные источники (вспышки взрывов): xyz+радиус, цвет*сила. */
        public final float[] pl = new float[16], plc = new float[12];
        public int plN = 0;
    }

    static final class Item {
        Node3 n; Mat m; float d; int key;
    }

    final Array<Item> pool = new Array<>(), opaque = new Array<>(), trans = new Array<>();
    int poolUsed;
    final Array<Node3> casters = new Array<>();

    public Renderer() {
        skyQuad = new Mesh(true, 4, 6, new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"));
        skyQuad.setVertices(new float[]{-1, -1, 0, 1, -1, 0, 1, 1, 0, -1, 1, 0});
        skyQuad.setIndices(new short[]{0, 1, 2, 0, 2, 3});
    }

    Item item() {
        if (poolUsed >= pool.size) pool.add(new Item());
        return pool.get(poolUsed++);
    }

    static Vector3 lin(Color c, Vector3 out) {
        return out.set((float) Math.pow(c.r, 2.2), (float) Math.pow(c.g, 2.2), (float) Math.pow(c.b, 2.2));
    }

    // ---------- шейдерные параметры среды ----------
    void setEnv(ShaderProgram p) {
        p.setUniformMatrix("u_vp", cam.combined);
        p.setUniformf("u_cam", cam.position);
        p.setUniformf("u_sunDir", env.sunDir);
        lin(env.sunCol, tv).scl(env.sunEnergy);
        p.setUniformf("u_sunCol", tv);
        lin(env.amb, tv).scl(env.ambEnergy * (1f + env.flash * 8f));
        p.setUniformf("u_amb", tv);
        lin(env.fogCol, tv);
        p.setUniformf("u_fogCol", tv);
        p.setUniformf("u_fogD", env.fogD);
        p.setUniformf("u_post", env.exposure, env.contrast, env.saturation);
        p.setUniformf("u_plN", env.plN);
        if (env.plN > 0) { p.setUniform4fv("u_pl", env.pl, 0, 16); p.setUniform3fv("u_plc", env.plc, 0, 12); }
    }

    static final long T0 = System.nanoTime();

    /** Время анимированных окрасок (с), по кругу, чтобы не терять точность во float. */
    static float ANIM_T() { return ((System.nanoTime() - T0) / 1_000_000L % 600_000L) / 1000f; }

    ShaderProgram curProg;
    Mat curMat;
    Texture panelTex;

    void bindMat(Mat m) {
        int key = m.key();
        ShaderProgram p = sh.get(key);
        if (p != curProg) {
            p.bind();
            curProg = p;
            setEnv(p);
            if ((key & 64) != 0 && shadowFbo != null) {
                shadowFbo.getColorBufferTexture().bind(3);
                p.setUniformi("u_shadow", 3);
                p.setUniformMatrix("u_svp", svp);
            }
            curMat = null;
        }
        if (m.anim > 0 && m.hull) p.setUniformf("u_an", m.anim, ANIM_T(), 0, 0);
        if (m == curMat) return;
        curMat = m;
        if (m.anim > 0 && m.hull) p.setUniformf("u_anc", m.animCol.r, m.animCol.g, m.animCol.b);
        GL20 gl = Gdx.gl;
        p.setUniformf("u_color", m.color.r, m.color.g, m.color.b, m.color.a);
        p.setUniformf("u_uvs", m.uvx, m.uvy);
        p.setUniformf("u_m", m.rough, m.metal, m.spec, m.emission);
        p.setUniformf("u_h3", m.burnt, m.wear, 0, m.alpha);
        if (m.distFadeMax > 0) p.setUniformf("u_fade", m.distFadeMin, m.distFadeMax);
        if (m.tex != null) { m.tex.bind(0); p.setUniformi("u_tex", 0); }
        if (m.hull) {
            if (panelTex == null) panelTex = Tex.panel();
            panelTex.bind(2); p.setUniformi("u_panel", 2);
            if (m.camo != null) { m.camo.bind(1); p.setUniformi("u_camo", 1); }
            p.setUniformf("u_h1", m.camoScale, m.panelScale, m.seams, m.camo != null ? 1f : 0f);
            p.setUniformf("u_h2", m.rough, m.metal, m.dust, m.dustH);
        }
        gl.glActiveTexture(GL20.GL_TEXTURE0);
        if (m.cull) gl.glEnable(GL20.GL_CULL_FACE); else gl.glDisable(GL20.GL_CULL_FACE);
        gl.glDepthMask(m.depthWrite);
        if (m.depthTest) gl.glEnable(GL20.GL_DEPTH_TEST); else gl.glDisable(GL20.GL_DEPTH_TEST);
        if (m.blend == Mat.OPAQUE && m.alpha >= 0.999f) gl.glDisable(GL20.GL_BLEND);
        else {
            gl.glEnable(GL20.GL_BLEND);
            if (m.blend == Mat.ADD) gl.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE);
            else gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }
    }

    void drawNode(Node3 n, Mat m) {
        bindMat(m);
        if (n.alpha < 0.999f) curProg.setUniformf("u_h3", m.burnt, m.wear, 0, m.alpha * n.alpha);
        curProg.setUniformMatrix("u_world", n.global);
        nmat.set(n.global);
        if (Math.abs(nmat.det()) > 1e-12f) nmat.inv().transpose(); else nmat.idt(); // сплющенный узел — без вылета на обращении

        curProg.setUniformMatrix("u_nmat", nmat);
        n.mesh.mesh.render(curProg, GL20.GL_TRIANGLES, 0, n.mesh.count);
        if (n.alpha < 0.999f) curMat = null;
    }

    public void drawDyn(Dyn d) {
        if (d.count() == 0) return;
        bindMat(d.mat);
        curProg.setUniformMatrix("u_world", ident);
        nmat.idt();
        curProg.setUniformMatrix("u_nmat", nmat);
        d.flushTo(curProg);
    }

    void collect(Node3 n, boolean shadow) {
        if (!n.visible) return;
        if (n.mesh != null && n.mat != null && n.mesh.count > 0) {
            tv.set(n.mesh.center).mul(n.global);
            float s = Math.max(Math.abs(n.global.getScaleX()), Math.max(Math.abs(n.global.getScaleY()), Math.abs(n.global.getScaleZ())));
            if (cam.frustum.sphereInFrustum(tv, n.mesh.radius * s + 0.5f)) {
                Item it = item(); it.n = n; it.m = n.mat; it.key = n.mat.key() * 4 + n.mat.order;
                it.d = tv.dst2(cam.position);
                if (n.mat.transparent() || n.alpha < 0.999f) trans.add(it); else opaque.add(it);
                if (n.overlay != null) { Item o = item(); o.n = n; o.m = n.overlay; o.d = it.d - 0.01f; trans.add(o); }
            }
            if (shadow && n.castShadow) casters.add(n);
        }
        for (int i = 0; i < n.kids.size; i++) collect(n.kids.get(i), shadow);
    }

    static final Comparator<Item> OPQ = (a, b) -> Integer.compare(a.key, b.key);
    static final Comparator<Item> TRN = (a, b) -> {
        if (a.m.order != b.m.order) return Integer.compare(a.m.order, b.m.order);
        return Float.compare(b.d, a.d);
    };

    /** Полный кадр сцены (без UI). */
    public void render(Array<Node3> roots, Array<Dyn> dyns) {
        GL20 gl = Gdx.gl;
        poolUsed = 0; opaque.clear(); trans.clear(); casters.clear();
        for (int i = 0; i < roots.size; i++) roots.get(i).updateGlobal(null);
        for (int i = 0; i < roots.size; i++) collect(roots.get(i), shadows);
        frame++;
        statOpq = opaque.size; statTrn = trans.size; statCast = casters.size;
        statTris = 0;
        if (dumpNext) { dumpNext = false; for (int i = 0; i < opaque.size; i++) { Node3 n = opaque.get(i).n; Gdx.app.log("node", (n.mesh.count / 3) + " cast=" + n.castShadow + " key=" + opaque.get(i).m.key() + " r=" + n.mesh.radius); } }
        for (int i = 0; i < opaque.size; i++) statTris += opaque.get(i).n.mesh.count / 3;
        for (int i = 0; i < casters.size; i++) statTris += casters.get(i).mesh.count / 3;
        if (shadows && (shadowFbo == null || frame % Math.max(1, shadowEvery) == 0)) renderShadow();

        int bw = Gdx.graphics.getBackBufferWidth(), bh = Gdx.graphics.getBackBufferHeight();
        boolean scaled = scale < 0.98f && ensureScene(Math.max(64, Math.round(bw * scale)), Math.max(64, Math.round(bh * scale)));
        if (scaled) {
            sceneFbo.begin();
            gl.glViewport(0, 0, sceneFbo.getWidth(), sceneFbo.getHeight());
        } else gl.glViewport(0, 0, bw, bh);
        gl.glClearColor(0, 0, 0, 1);
        gl.glDepthMask(true);
        gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        gl.glFrontFace(GL20.GL_CW);
        gl.glCullFace(GL20.GL_BACK);
        gl.glDepthFunc(GL20.GL_LEQUAL);
        drawSky();

        curProg = null; curMat = null;
        Sort.instance().sort(opaque, OPQ);
        for (int i = 0; i < opaque.size; i++) { Item it = opaque.get(i); drawNode(it.n, it.m); }
        if (dyns != null) for (int i = 0; i < dyns.size; i++) if (!dyns.get(i).mat.transparent()) drawDyn(dyns.get(i));
        Sort.instance().sort(trans, TRN);
        for (int i = 0; i < trans.size; i++) { Item it = trans.get(i); drawNode(it.n, it.m); }
        if (dyns != null) for (int i = 0; i < dyns.size; i++) if (dyns.get(i).mat.transparent()) drawDyn(dyns.get(i));
        gl.glDepthMask(true);
        gl.glDisable(GL20.GL_BLEND);
        gl.glDisable(GL20.GL_CULL_FACE);
        gl.glFrontFace(GL20.GL_CCW);
        gl.glDisable(GL20.GL_DEPTH_TEST);
        curProg = null; curMat = null;
        if (scaled) {
            sceneFbo.end();
            gl.glViewport(0, 0, bw, bh);
            blit();
        }
    }

    boolean sceneFailed;

    boolean ensureScene(int w, int h) {
        if (sceneFailed) return false;
        if (sceneFbo != null && sceneFbo.getWidth() == w && sceneFbo.getHeight() == h) return true;
        if (sceneFbo != null) { sceneFbo.dispose(); sceneFbo = null; }
        try {
            GLFrameBuffer.FrameBufferBuilder b = new GLFrameBuffer.FrameBufferBuilder(w, h);
            b.addBasicColorTextureAttachment(Pixmap.Format.RGB888);
            b.addDepthRenderBuffer(0x81A6); // GL_DEPTH_COMPONENT24(_OES)
            sceneFbo = b.build();
        } catch (Throwable t) {
            try { sceneFbo = new FrameBuffer(Pixmap.Format.RGB888, w, h, true); }
            catch (Throwable t2) { sceneFailed = true; sceneFbo = null; return false; }
        }
        sceneFbo.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        if (blitQuad == null) {
            blitQuad = new Mesh(true, 4, 6, new VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position"));
            blitQuad.setVertices(new float[]{-1, -1, 1, -1, 1, 1, -1, 1});
            blitQuad.setIndices(new short[]{0, 1, 2, 0, 2, 3});
            String vs = "attribute vec2 a_position;\nvarying vec2 v_uv;\nvoid main(){ v_uv = a_position * 0.5 + 0.5; gl_Position = vec4(a_position, 0.0, 1.0); }";
            String fs = "#ifdef GL_ES\nprecision mediump float;\n#endif\nvarying vec2 v_uv;\nuniform sampler2D u_tex;\nvoid main(){ gl_FragColor = vec4(texture2D(u_tex, v_uv).rgb, 1.0); }";
            blitProg = new ShaderProgram(vs, fs);
            if (!blitProg.isCompiled()) { Gdx.app.error("blit", blitProg.getLog()); sceneFailed = true; return false; }
        }
        return true;
    }

    void blit() {
        GL20 gl = Gdx.gl;
        gl.glDisable(GL20.GL_DEPTH_TEST);
        gl.glDisable(GL20.GL_BLEND);
        gl.glDisable(GL20.GL_CULL_FACE);
        blitProg.bind();
        sceneFbo.getColorBufferTexture().bind(0);
        blitProg.setUniformi("u_tex", 0);
        blitQuad.render(blitProg, GL20.GL_TRIANGLES);
    }

    void drawSky() {
        GL20 gl = Gdx.gl;
        gl.glDisable(GL20.GL_DEPTH_TEST);
        gl.glDepthMask(false);
        gl.glDisable(GL20.GL_CULL_FACE);
        gl.glDisable(GL20.GL_BLEND);
        ShaderProgram p = sh.sky;
        p.bind();
        ivp.set(cam.combined).inv();
        p.setUniformMatrix("u_ivp", ivp);
        p.setUniformf("u_cam", cam.position);
        for (int i = 0; i < 4; i++) { lin(env.sky[i], tv); p.setUniformf("u_c" + i, tv); }
        p.setUniformf("u_sunDir", env.sunDir);
        lin(env.skySun, tv);
        p.setUniformf("u_sunCol", tv);
        p.setUniformf("u_sunOn", env.sunOn);
        lin(env.fogCol, tv);
        p.setUniformf("u_fogCol", tv);
        p.setUniformf("u_skyFog", env.skyFog);
        p.setUniformf("u_flash", env.flash * 3f);
        p.setUniformf("u_post", env.exposure, env.contrast, env.saturation);
        skyQuad.render(p, GL20.GL_TRIANGLES);
        gl.glDepthMask(true);
        gl.glEnable(GL20.GL_DEPTH_TEST);
    }

    void renderShadow() {
        if (shadowFbo == null) {
            try {
                shadowFbo = new FrameBuffer(Pixmap.Format.RGBA8888, 1024, 1024, true);
                shadowFbo.getColorBufferTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            } catch (Throwable t) {
                shadows = false;
                return;
            }
        }
        float R = shadowRange;
        sunCam.viewportWidth = R * 2; sunCam.viewportHeight = R * 2;
        sunCam.near = 1f; sunCam.far = 400f;
        sunCam.position.set(shadowFocus).mulAdd(env.sunDir, 200f);
        sunCam.direction.set(env.sunDir).scl(-1);
        sunCam.up.set(Math.abs(env.sunDir.y) > 0.95f ? Vector3.Z : Vector3.Y);
        sunCam.update();
        svp.set(sunCam.combined);
        shadowFbo.begin();
        GL20 gl = Gdx.gl;
        gl.glClearColor(1, 1, 1, 1);
        gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        gl.glEnable(GL20.GL_DEPTH_TEST);
        gl.glDepthMask(true);
        gl.glDisable(GL20.GL_BLEND);
        gl.glDisable(GL20.GL_CULL_FACE);
        ShaderProgram p = sh.depth;
        p.bind();
        p.setUniformMatrix("u_vp", svp);
        for (int i = 0; i < casters.size; i++) {
            Node3 n = casters.get(i);
            p.setUniformMatrix("u_world", n.global);
            n.mesh.mesh.render(p, GL20.GL_TRIANGLES, 0, n.mesh.count);
        }
        shadowFbo.end();
    }

    @Override
    public void dispose() {
        sh.dispose(); skyQuad.dispose();
        if (shadowFbo != null) shadowFbo.dispose();
        if (sceneFbo != null) sceneFbo.dispose();
        if (blitQuad != null) blitQuad.dispose();
        if (blitProg != null) blitProg.dispose();
        if (panelTex != null) panelTex.dispose();
    }
}
