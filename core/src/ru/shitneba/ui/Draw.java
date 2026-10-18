package ru.shitneba.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;

import java.util.HashMap;
import java.util.Map;

/** 2D-рисование интерфейса в виртуальных координатах (высота 540, ось Y вниз). */
public class Draw implements Disposable {
    public static final float VH = 540f;
    public final PolygonSpriteBatch b = new PolygonSpriteBatch(4000);
    public final OrthographicCamera cam = new OrthographicCamera();
    public float w = 960, h = 540, s = 1;
    public boolean off; // режим измерения: ничего не рисуем
    final Texture white;
    final float[] vb = new float[5 * 64];
    final short[] ib = new short[3 * 62];
    final GlyphLayout gl = new GlyphLayout();
    public static final int MAIN = 0, TITLE = 1, BOLD = 2, WIDE = 3;
    final FreeTypeFontGenerator[] gens = new FreeTypeFontGenerator[4];
    final Map<Integer, BitmapFont> fonts = new HashMap<>();

    public Draw() {
        Pixmap p = new Pixmap(4, 4, Pixmap.Format.RGBA8888);
        p.setColor(Color.WHITE); p.fill();
        white = new Texture(p); p.dispose();
        gens[MAIN] = new FreeTypeFontGenerator(Gdx.files.internal("Exo2-Medium.ttf"));
        gens[TITLE] = new FreeTypeFontGenerator(Gdx.files.internal("RussoOne.ttf"));
        gens[BOLD] = new FreeTypeFontGenerator(Gdx.files.internal("Exo2-Bold.ttf"));
        gens[WIDE] = gens[TITLE];
        resize();
    }

    public void resize() {
        int sw = Math.max(1, Gdx.graphics.getWidth()), sh = Math.max(1, Gdx.graphics.getHeight());
        s = sh / VH;
        h = VH; w = sw / s;
        if (w < 720) { w = 720; s = sw / w; h = sh / s; }
        cam.setToOrtho(true, w, h);
        cam.update();
        for (BitmapFont f : fonts.values()) f.dispose();
        fonts.clear();
    }

    public void begin() {
        b.setProjectionMatrix(cam.combined);
        b.begin();
        b.enableBlending();
        b.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    public void end() { b.end(); }

    // ---------- шрифты ----------
    public BitmapFont font(int id, float size) {
        int px = Math.max(6, Math.round(size * s));
        int key = id * 1000 + px;
        BitmapFont f = fonts.get(key);
        if (f == null) {
            FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
            p.size = px; p.flip = true; p.incremental = true; p.characters = CHARS;
            p.minFilter = Texture.TextureFilter.Linear; p.magFilter = Texture.TextureFilter.Linear;
            p.kerning = true;
            if (id == WIDE) p.spaceX = Math.round(2 * s);
            f = gens[id].generateFont(p);
            f.setUseIntegerPositions(false);
            f.getData().setScale(1f / s);
            f.getData().markupEnabled = false;
            fonts.put(key, f);
        }
        return f;
    }

    static final String CHARS = "0123456789 !?.,:;-+×%/()«»—·…№'\"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
            + "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя";

    /** Заранее создать шрифты, которые нужны в бою. */
    public void prewarm() {
        float[][] fs = {{MAIN, 11}, {MAIN, 12}, {MAIN, 13}, {MAIN, 14}, {MAIN, 15}, {MAIN, 16}, {TITLE, 11}, {TITLE, 12}, {TITLE, 13}, {TITLE, 14},
                {TITLE, 16}, {TITLE, 18}, {TITLE, 19}, {TITLE, 20}, {TITLE, 22}, {TITLE, 24}, {TITLE, 34}, {BOLD, 12}, {BOLD, 13}, {BOLD, 14}, {BOLD, 24}};
        for (float[] f : fs) font((int) f[0], f[1]);
    }

    public float textW(String t, float size, int font) {
        gl.setText(font(font, size), t);
        return gl.width;
    }

    /** Текст по базовой линии (как draw_string в Godot). align: -1 лево, 0 центр, 1 право. */
    public void text(String t, float x, float baseline, float size, Color c, int font, int align) {
        if (off || t == null || t.isEmpty()) return;
        BitmapFont f = font(font, size);
        f.setColor(c);
        float y = baseline - f.getCapHeight();
        int al = align < 0 ? Align.left : (align == 0 ? Align.center : Align.right);
        f.draw(b, t, x, y, 0, al, false);
    }

    public void text(String t, float x, float baseline, float size, Color c) { text(t, x, baseline, size, c, MAIN, -1); }

    /** Текст с переносом в рамке шириной wd, top — верх строки. Возвращает высоту. */
    public float wrap(String t, float x, float top, float wd, float size, Color c, int font, int align) {
        BitmapFont f = font(font, size);
        int al = align < 0 ? Align.left : (align == 0 ? Align.center : Align.right);
        gl.setText(f, t, c, wd, al, true);
        float hh = gl.height + lineGap(f);
        if (!off) {
            float asc = f.getLineHeight() - f.getCapHeight();
            b.setColor(Color.WHITE);
            f.draw(b, gl, x, top + asc * 0.45f);
        }
        return hh;
    }

    float lineGap(BitmapFont f) { return (f.getLineHeight() - f.getCapHeight()) * 0.9f; }

    public float lineH(float size, int font) { return font(font, size).getLineHeight(); }

    // ---------- примитивы ----------
    float pack(Color c, float a) { return Color.toFloatBits(c.r, c.g, c.b, c.a * a); }

    int nv, ni;

    void v(float x, float y, float col) {
        int o = nv * 5;
        vb[o] = x; vb[o + 1] = y; vb[o + 2] = col; vb[o + 3] = 0.5f; vb[o + 4] = 0.5f;
        nv++;
    }

    void flushPoly() {
        if (nv > 0 && ni > 0) b.draw(white, vb, 0, nv * 5, ib, 0, ni);
        nv = 0; ni = 0;
    }

    public void rect(float x, float y, float ww, float hh, Color c) { rect(x, y, ww, hh, c, 1); }

    public void rect(float x, float y, float ww, float hh, Color c, float a) {
        if (off || ww <= 0 || hh <= 0) return;
        float f = pack(c, a);
        quad(x, y, f, x + ww, y, f, x + ww, y + hh, f, x, y + hh, f);
    }

    public void grad(float x, float y, float ww, float hh, Color top, float ta, Color bot, float ba) {
        if (off) return;
        float t = pack(top, ta), bo = pack(bot, ba);
        quad(x, y, t, x + ww, y, t, x + ww, y + hh, bo, x, y + hh, bo);
    }

    public void gradH(float x, float y, float ww, float hh, Color l, float la, Color r, float ra) {
        if (off) return;
        float a = pack(l, la), bb = pack(r, ra);
        quad(x, y, a, x + ww, y, bb, x + ww, y + hh, bb, x, y + hh, a);
    }

    public void quad(float x1, float y1, float c1, float x2, float y2, float c2, float x3, float y3, float c3, float x4, float y4, float c4) {
        if (off) return;
        nv = 0; ni = 0;
        v(x1, y1, c1); v(x2, y2, c2); v(x3, y3, c3); v(x4, y4, c4);
        ib[0] = 0; ib[1] = 1; ib[2] = 2; ib[3] = 0; ib[4] = 2; ib[5] = 3; ni = 6;
        flushPoly();
    }

    public void tri(float x1, float y1, float x2, float y2, float x3, float y3, Color c, float a) {
        if (off) return;
        float f = pack(c, a);
        nv = 0; ni = 0;
        v(x1, y1, f); v(x2, y2, f); v(x3, y3, f);
        ib[0] = 0; ib[1] = 1; ib[2] = 2; ni = 3;
        flushPoly();
    }

    /** Выпуклый многоугольник веером. */
    public void poly(float[] xy, Color c, float a) {
        if (off) return;
        int n = xy.length / 2;
        float f = pack(c, a);
        nv = 0; ni = 0;
        for (int i = 0; i < n; i++) v(xy[i * 2], xy[i * 2 + 1], f);
        for (int i = 1; i < n - 1; i++) { ib[ni++] = 0; ib[ni++] = (short) i; ib[ni++] = (short) (i + 1); }
        flushPoly();
    }

    public void line(float x1, float y1, float x2, float y2, float wd, Color c, float a) {
        if (off) return;
        float dx = x2 - x1, dy = y2 - y1, l = (float) Math.sqrt(dx * dx + dy * dy);
        if (l < 1e-4f) return;
        float nx = -dy / l * wd * 0.5f, ny = dx / l * wd * 0.5f;
        float f = pack(c, a);
        quad(x1 + nx, y1 + ny, f, x2 + nx, y2 + ny, f, x2 - nx, y2 - ny, f, x1 - nx, y1 - ny, f);
    }

    public void polyline(float[] xy, boolean closed, float wd, Color c, float a) {
        int n = xy.length / 2;
        for (int i = 0; i < n - 1; i++) line(xy[i * 2], xy[i * 2 + 1], xy[i * 2 + 2], xy[i * 2 + 3], wd, c, a);
        if (closed) line(xy[n * 2 - 2], xy[n * 2 - 1], xy[0], xy[1], wd, c, a);
    }

    public void circle(float cx, float cy, float r, Color c, float a) {
        if (off) return;
        int seg = MathUtils.clamp((int) (r * 0.8f) + 10, 12, 40);
        float f = pack(c, a);
        nv = 0; ni = 0;
        v(cx, cy, f);
        for (int i = 0; i <= seg; i++) {
            float an = i * MathUtils.PI2 / seg;
            v(cx + MathUtils.cos(an) * r, cy + MathUtils.sin(an) * r, f);
        }
        for (int i = 1; i <= seg; i++) { ib[ni++] = 0; ib[ni++] = (short) i; ib[ni++] = (short) (i + 1); }
        flushPoly();
    }

    public void arc(float cx, float cy, float r, float a0, float a1, float wd, Color c, float a) {
        if (off) return;
        int seg = MathUtils.clamp((int) (Math.abs(a1 - a0) * r * 0.25f) + 6, 6, 60);
        float f = pack(c, a);
        float ri = r - wd * 0.5f, ro = r + wd * 0.5f;
        for (int i = 0; i < seg; i++) {
            float t0 = a0 + (a1 - a0) * i / seg, t1 = a0 + (a1 - a0) * (i + 1) / seg;
            float c0 = MathUtils.cos(t0), s0 = MathUtils.sin(t0), c1 = MathUtils.cos(t1), s1 = MathUtils.sin(t1);
            quad(cx + c0 * ri, cy + s0 * ri, f, cx + c0 * ro, cy + s0 * ro, f, cx + c1 * ro, cy + s1 * ro, f, cx + c1 * ri, cy + s1 * ri, f);
        }
    }

    public void ring(float cx, float cy, float r, float wd, Color c, float a) { arc(cx, cy, r, 0, MathUtils.PI2, wd, c, a); }

    /** Прямоугольник-рамка. */
    public void frame(float x, float y, float ww, float hh, float wd, Color c, float a) {
        rect(x, y, ww, wd, c, a); rect(x, y + hh - wd, ww, wd, c, a);
        rect(x, y + wd, wd, hh - 2 * wd, c, a); rect(x + ww - wd, y + wd, wd, hh - 2 * wd, c, a);
    }

    public static float[] oct(float cx, float cy, float r) {
        float[] p = new float[16];
        for (int i = 0; i < 8; i++) {
            float an = MathUtils.PI / 8 + i * MathUtils.PI / 4;
            p[i * 2] = cx + MathUtils.cos(an) * r; p[i * 2 + 1] = cy + MathUtils.sin(an) * r;
        }
        return p;
    }

    public void star(float cx, float cy, float r, Color c, float a) {
        if (off) return;
        float f = pack(c, a);
        nv = 0; ni = 0;
        v(cx, cy, f);
        for (int i = 0; i <= 10; i++) {
            float an = -MathUtils.PI / 2 + i * MathUtils.PI / 5;
            float rr = (i % 2 == 0) ? r : r * 0.45f;
            v(cx + MathUtils.cos(an) * rr, cy + MathUtils.sin(an) * rr, f);
        }
        for (int i = 1; i <= 10; i++) { ib[ni++] = 0; ib[ni++] = (short) i; ib[ni++] = (short) (i + 1); }
        flushPoly();
    }

    public void check(float cx, float cy, float r, float wd, Color c, float a) {
        line(cx - r, cy, cx - r * 0.3f, cy + r * 0.7f, wd, c, a);
        line(cx - r * 0.3f - wd * 0.3f, cy + r * 0.7f + wd * 0.2f, cx + r, cy - r * 0.6f, wd, c, a);
    }

    /** Прямоугольная текстура (образец окраски). */
    public void tex(Texture t, float x, float y, float ww, float hh, float u2, float v2) {
        if (off) return;
        b.setColor(Color.WHITE);
        b.draw(t, x, y, ww, hh, 0, 0, u2, v2);
    }

    // ---------- обрезка ----------
    public void clip(float x, float y, float ww, float hh) {
        if (off) return;
        b.flush();
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        int sh = Gdx.graphics.getBackBufferHeight();
        float k = Gdx.graphics.getBackBufferWidth() / (float) Gdx.graphics.getWidth();
        int px = Math.round(x * s * k), py = Math.round(sh - (y + hh) * s * k);
        Gdx.gl.glScissor(px, py, Math.max(0, Math.round(ww * s * k)), Math.max(0, Math.round(hh * s * k)));
    }

    public void unclip() {
        if (off) return;
        b.flush();
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
    }

    @Override
    public void dispose() {
        b.dispose(); white.dispose();
        for (BitmapFont f : fonts.values()) f.dispose();
        gens[MAIN].dispose(); gens[TITLE].dispose(); gens[BOLD].dispose();
    }
}
