package ru.shitneba.gfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.Disposable;

import java.nio.ByteBuffer;

/**
 * «Лучший момент» (3.9.0): во время боя 10 раз в секунду снимается уменьшенный кадр сцены (без интерфейса)
 * в кольцевой буфер на 5 с. Когда случается что-то яркое (мультикилл, босс), через 2 с буфер «замораживается» —
 * получается ролик: 3 с до события и 2 с после. Лучший за бой показывается на экране итога и сохраняется гифкой.
 */
public class Replay implements Disposable {
    public static final int W = 256, H = 144, FPS = 10, PRE = 30, POST = 20, N = PRE + POST;

    final byte[][] ring = new byte[N][];
    int head, count;
    float acc;
    boolean failed, cpuMode;
    // заморозка
    boolean pending; int postLeft; float pendW;
    public float bestW;
    public byte[][] best; public int bestN;
    public String bestName = "";
    String pendName = "";

    FrameBuffer small; Texture grab; int gw, gh;
    ShaderProgram prog; Mesh quad;
    ByteBuffer buf, big;

    public void reset() { head = 0; count = 0; acc = 0; pending = false; best = null; bestN = 0; bestW = 0; bestName = ""; }

    /** Событие для ролика: вес (чем больше, тем ярче), название. */
    public void event(float w, String name) {
        if (failed) return;
        if (pending) { if (w > pendW) { pendW = w; pendName = name; } return; }
        if (w <= bestW) return;
        pending = true; pendW = w; pendName = name; postLeft = POST;
    }

    /** Вызывается раз в кадр после отрисовки сцены (до интерфейса). */
    public void tick(float dt, boolean active) {
        if (failed || !active) { acc = 0; return; }
        acc += dt;
        if (acc < 1f / FPS) return;
        acc = 0;
        try { grabFrame(); } catch (Throwable t) { failed = true; Gdx.app.error("replay", "отключено", t); return; }
        if (pending && --postLeft <= 0) freeze();
    }

    void freeze() {
        pending = false;
        int n = Math.min(count, N);
        byte[][] out = new byte[n][];
        for (int i = 0; i < n; i++) {
            int k = (head - n + i + N) % N;
            out[i] = ring[k];
            ring[k] = null; // буфер отдан ролику, кольцу заведём новый
        }
        count = 0;
        best = out; bestN = n; bestW = pendW; bestName = pendName;
    }

    void grabFrame() {
        byte[] dst = ring[head];
        if (dst == null) dst = ring[head] = new byte[W * H * 4];
        int bw = Gdx.graphics.getBackBufferWidth(), bh = Gdx.graphics.getBackBufferHeight();
        if (!cpuMode && !gpuGrab(bw, bh, dst)) cpuMode = true;
        if (cpuMode) cpuGrab(bw, bh, dst);
        head = (head + 1) % N;
        count = Math.min(N, count + 1);
    }

    /** Кадр с экрана → текстура → уменьшение на видеокарте → чтение 256×144. */
    boolean gpuGrab(int bw, int bh, byte[] dst) {
        GL20 gl = Gdx.gl;
        if (prog == null) {
            String vs = "attribute vec2 a_position;\nvarying vec2 v_uv;\nvoid main(){ v_uv = a_position * 0.5 + 0.5; gl_Position = vec4(a_position, 0.0, 1.0); }";
            String fs = "#ifdef GL_ES\nprecision mediump float;\n#endif\nvarying vec2 v_uv;\nuniform sampler2D u_tex;\nuniform vec2 u_px;\n"
                    + "void main(){ vec3 c = texture2D(u_tex, v_uv + u_px * vec2(-0.25,-0.25)).rgb + texture2D(u_tex, v_uv + u_px * vec2(0.25,-0.25)).rgb"
                    + " + texture2D(u_tex, v_uv + u_px * vec2(-0.25,0.25)).rgb + texture2D(u_tex, v_uv + u_px * vec2(0.25,0.25)).rgb; gl_FragColor = vec4(c * 0.25, 1.0); }";
            prog = new ShaderProgram(vs, fs);
            if (!prog.isCompiled()) { Gdx.app.error("replay", prog.getLog()); return false; }
            quad = new Mesh(true, 4, 6, new VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position"));
            quad.setVertices(new float[]{-1, -1, 1, -1, 1, 1, -1, 1});
            quad.setIndices(new short[]{0, 1, 2, 0, 2, 3});
            small = new FrameBuffer(Pixmap.Format.RGBA8888, W, H, false);
            buf = BufferUtils.newByteBuffer(W * H * 4);
        }
        if (grab == null || gw != bw || gh != bh) {
            if (grab != null) grab.dispose();
            grab = new Texture(bw, bh, Pixmap.Format.RGB888);
            grab.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            gw = bw; gh = bh;
        }
        while (gl.glGetError() != GL20.GL_NO_ERROR) { /* сбросить старые ошибки */ }
        grab.bind(0);
        gl.glCopyTexSubImage2D(GL20.GL_TEXTURE_2D, 0, 0, 0, 0, 0, bw, bh);
        if (gl.glGetError() != GL20.GL_NO_ERROR) return false;
        small.begin();
        gl.glViewport(0, 0, W, H);
        gl.glDisable(GL20.GL_BLEND); gl.glDisable(GL20.GL_DEPTH_TEST); gl.glDisable(GL20.GL_CULL_FACE);
        prog.bind();
        prog.setUniformi("u_tex", 0);
        prog.setUniformf("u_px", 1f / W, 1f / H);
        quad.render(prog, GL20.GL_TRIANGLES);
        buf.clear();
        gl.glReadPixels(0, 0, W, H, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, buf);
        small.end();
        gl.glViewport(0, 0, bw, bh);
        buf.get(dst, 0, W * H * 4);
        return true;
    }

    /** Запасной путь (если копирование экрана не поддерживается): читаем экран целиком и уменьшаем сами. */
    void cpuGrab(int bw, int bh, byte[] dst) {
        if (big == null || big.capacity() < bw * bh * 4) big = BufferUtils.newByteBuffer(bw * bh * 4);
        big.clear();
        Gdx.gl.glPixelStorei(GL20.GL_PACK_ALIGNMENT, 1);
        Gdx.gl.glReadPixels(0, 0, bw, bh, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, big);
        for (int y = 0; y < H; y++) {
            int sy = (int) ((y + 0.5f) * bh / H);
            for (int x = 0; x < W; x++) {
                int sx = (int) ((x + 0.5f) * bw / W), s = (sy * bw + sx) * 4, d = (y * W + x) * 4;
                dst[d] = big.get(s); dst[d + 1] = big.get(s + 1); dst[d + 2] = big.get(s + 2); dst[d + 3] = (byte) 255;
            }
        }
    }

    // ---------- показ ролика в интерфейсе ----------
    Texture view; Pixmap viewPx; int viewFrame = -1;

    /** Текстура кадра ролика для времени t (по кругу). null — ролика нет. */
    public Texture frame(float t) {
        if (best == null || bestN == 0) return null;
        int f = (int) (t * FPS) % bestN;
        if (viewPx == null) { viewPx = new Pixmap(W, H, Pixmap.Format.RGBA8888); view = new Texture(W, H, Pixmap.Format.RGBA8888); view.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear); }
        if (f != viewFrame) {
            ByteBuffer p = viewPx.getPixels();
            byte[] src = best[f];
            // в Pixmap строки сверху вниз, в кадре — снизу вверх
            for (int y = 0; y < H; y++) { p.position(y * W * 4); p.put(src, (H - 1 - y) * W * 4, W * 4); }
            p.position(0);
            view.draw(viewPx, 0, 0);
            viewFrame = f;
        }
        return view;
    }

    /** Собрать GIF (долго — вызывать не в кадре). */
    public byte[] gif() { return best == null ? null : GifEnc.encode(best, bestN, W, H, 100 / FPS); }

    @Override
    public void dispose() {
        if (small != null) small.dispose();
        if (grab != null) grab.dispose();
        if (prog != null) prog.dispose();
        if (quad != null) quad.dispose();
        if (view != null) view.dispose();
        if (viewPx != null) viewPx.dispose();
    }
}
