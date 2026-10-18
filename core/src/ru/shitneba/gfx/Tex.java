package ru.shitneba.gfx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.MipMapGenerator;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.JsonValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import ru.shitneba.G;

/** Процедурные текстуры — порт tex.gd. */
public final class Tex {
    static final Map<String, Texture> cache = new HashMap<>();
    static final Random R = new Random();

    static float randf() { return R.nextFloat(); }
    static float rr(float a, float b) { return a + R.nextFloat() * (b - a); }

    /** Простое RGBA-изображение на float-массивах. */
    public static final class Img {
        public final int w, h; public final float[] p;
        public Img(int w, int h, Color c) {
            this.w = w; this.h = h; p = new float[w * h * 4];
            for (int i = 0; i < w * h; i++) { p[i * 4] = c.r; p[i * 4 + 1] = c.g; p[i * 4 + 2] = c.b; p[i * 4 + 3] = c.a; }
        }
        public void set(int x, int y, float r, float g, float b, float a) {
            if (x < 0 || y < 0 || x >= w || y >= h) return;
            int i = (y * w + x) * 4; p[i] = r; p[i + 1] = g; p[i + 2] = b; p[i + 3] = a;
        }
        public void set(int x, int y, Color c) { set(x, y, c.r, c.g, c.b, c.a); }
        public float get(int x, int y, int ch) { return p[(y * w + x) * 4 + ch]; }
        public void fillRect(int x0, int y0, int ww, int hh, Color c) {
            for (int y = Math.max(0, y0); y < Math.min(h, y0 + hh); y++)
                for (int x = Math.max(0, x0); x < Math.min(w, x0 + ww); x++) set(x, y, c);
        }
        public Pixmap pixmap() {
            Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
            pm.setBlending(Pixmap.Blending.None);
            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++) {
                    int i = (y * w + x) * 4;
                    pm.drawPixel(x, y, Color.rgba8888(MathUtils.clamp(p[i], 0, 1), MathUtils.clamp(p[i + 1], 0, 1), MathUtils.clamp(p[i + 2], 0, 1), MathUtils.clamp(p[i + 3], 0, 1)));
                }
            return pm;
        }
    }

    public static Texture tex(Img im, boolean mip) {
        Pixmap pm = im.pixmap();
        Texture t = new Texture(pm, mip);
        t.setFilter(mip ? Texture.TextureFilter.MipMapLinearLinear : Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        t.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        return t;
    }

    static void noise(Img im, float amt) {
        for (int i = 0; i < im.w * im.h; i++) {
            float k = (randf() - 0.5f) * amt;
            im.p[i * 4] += k; im.p[i * 4 + 1] += k; im.p[i * 4 + 2] += k;
        }
    }

    public static void circle(Img im, float cx, float cy, float r, Color col) {
        for (int y = Math.max(0, (int) (cy - r)); y < Math.min(im.h, (int) (cy + r) + 1); y++)
            for (int x = Math.max(0, (int) (cx - r)); x < Math.min(im.w, (int) (cx + r) + 1); x++)
                if ((x - cx) * (x - cx) + (y - cy) * (y - cy) <= r * r) {
                    if (col.a >= 1f) im.set(x, y, col);
                    else {
                        int i = (y * im.w + x) * 4;
                        im.set(x, y, MathUtils.lerp(im.p[i], col.r, col.a), MathUtils.lerp(im.p[i + 1], col.g, col.a), MathUtils.lerp(im.p[i + 2], col.b, col.a), MathUtils.lerp(im.p[i + 3], 1f, col.a));
                    }
                }
    }

    static boolean inPoly(float px, float py, float[] xs, float[] ys) {
        boolean c = false;
        for (int i = 0, j = xs.length - 1; i < xs.length; j = i++)
            if (((ys[i] > py) != (ys[j] > py)) && (px < (xs[j] - xs[i]) * (py - ys[i]) / (ys[j] - ys[i]) + xs[i])) c = !c;
        return c;
    }

    public static void poly(Img im, float[] xs, float[] ys, Color col) {
        float mnx = 1e9f, mny = 1e9f, mxx = -1e9f, mxy = -1e9f;
        for (int i = 0; i < xs.length; i++) { mnx = Math.min(mnx, xs[i]); mny = Math.min(mny, ys[i]); mxx = Math.max(mxx, xs[i]); mxy = Math.max(mxy, ys[i]); }
        for (int y = Math.max(0, (int) mny); y < Math.min(im.h, (int) mxy + 1); y++)
            for (int x = Math.max(0, (int) mnx); x < Math.min(im.w, (int) mxx + 1); x++)
                if (inPoly(x + 0.5f, y + 0.5f, xs, ys)) im.set(x, y, col);
    }

    static void band(Img im, float x0, float y0, float x1, float y1, float w, Color col) {
        float dx = x1 - x0, dy = y1 - y0, l = (float) Math.sqrt(dx * dx + dy * dy);
        float nx = -dy / l * w / 2, ny = dx / l * w / 2;
        poly(im, new float[]{x0 + nx, x1 + nx, x1 - nx, x0 - nx}, new float[]{y0 + ny, y1 + ny, y1 - ny, y0 - ny}, col);
    }

    static Color skCol(JsonValue cols, int i) { return G.hex(cols.getString(i)); }

    static JsonValue skin(String id) {
        JsonValue sk = G.D.get("SK_VEH").get(id);
        return sk == null ? G.D.get("SK_VEH").get("olive") : sk;
    }

    static JsonValue skCols(JsonValue sk) {
        JsonValue c = sk.get("cols");
        if (c != null) return c;
        JsonValue a = new JsonValue(JsonValue.ValueType.array);
        a.addChild(new JsonValue(sk.getString("hull")));
        a.addChild(new JsonValue(sk.getString("dark")));
        return a;
    }

    /** Образец окраски для карточки гаража. */
    public static Texture camo(String id) {
        String key = "camo_" + id;
        if (cache.containsKey(key)) return cache.get(key);
        JsonValue sk = skin(id);
        int S = 128;
        Img im = new Img(S, S, G.hex(sk.getString("hull")));
        JsonValue cols = skCols(sk);
        int nc = cols.size;
        String pat = sk.getString("pat", "");
        switch (pat) {
            case "blob3":
                for (int i = 0; i < 22; i++) {
                    Color col = skCol(cols, 1 + i % (nc - 1));
                    float cx = randf() * S, cy = randf() * S, r = 9 + randf() * 17;
                    for (int k = 0; k < 5; k++) circle(im, cx + rr(-r, r), cy + rr(-r * 0.5f, r * 0.5f), r * rr(0.5f, 0.9f), col);
                }
                break;
            case "digital":
                for (int y = 0; y < S; y += 6) for (int x = 0; x < S; x += 6)
                    if (randf() < 0.45f) im.fillRect(x, y, 6, 6, skCol(cols, 1 + R.nextInt(nc - 1)));
                break;
            case "stripes":
                for (int i = -4; i < 12; i++) band(im, i * 17f, 0, i * 17f + 30f, S, 7f, skCol(cols, 1));
                break;
            case "splinter":
                for (int i = 0; i < 26; i++) {
                    float x = randf() * S, y = randf() * S;
                    poly(im, new float[]{x, x + rr(10, 35), x + rr(5, 20)}, new float[]{y, y + rr(-10, 10), y + rr(10, 25)}, skCol(cols, 1 + i % (nc - 1)));
                }
                break;
            case "metal":
                for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
                    float k = (float) (x + y) / (2 * S);
                    Color c = k < 0.5f ? skCol(cols, 0).cpy().lerp(skCol(cols, 1), k * 2) : skCol(cols, 1).cpy().lerp(skCol(cols, 2), (k - 0.5f) * 2);
                    im.set(x, y, c);
                }
                break;
        }
        noise(im, 0.07f);
        for (int y = S * 6 / 10; y < S; y++) {
            float k = (y - S * 0.6f) / (S * 0.4f) * 0.3f;
            for (int x = 0; x < S; x++) {
                int i = (y * S + x) * 4;
                im.set(x, y, MathUtils.lerp(im.p[i], 0.47f, k), MathUtils.lerp(im.p[i + 1], 0.39f, k), MathUtils.lerp(im.p[i + 2], 0.27f, k), 1);
            }
        }
        Texture t = tex(im, true);
        cache.put(key, t);
        return t;
    }

    public static Texture ground(String name) {
        String key = "gr_" + name;
        if (cache.containsKey(key)) return cache.get(key);
        int S = 128;
        Img im = new Img(S, S, new Color(0.85f, 0.85f, 0.85f, 1));
        for (int i = 0; i < 40; i++) circle(im, randf() * S, randf() * S, rr(5, 20), new Color(rr(0.7f, 1), rr(0.7f, 1), rr(0.7f, 1), 0.35f));
        if (name.equals("desert")) {
            for (int i = 0; i < 14; i++) {
                float y0 = i * 9f;
                for (int x = 0; x < S; x++) im.set(x, ((int) (y0 + Math.sin(x * 0.1 + i) * 2.0) % S + S) % S, 0.6f, 0.6f, 0.6f, 1);
            }
        } else if (name.equals("snow")) {
            for (int i = 0; i < 500; i++) {
                if (randf() < 0.5f) im.set(R.nextInt(S), R.nextInt(S), 1, 1, 1, 1);
                else im.set(R.nextInt(S), R.nextInt(S), 0.75f, 0.78f, 0.85f, 1);
            }
        } else {
            for (int i = 0; i < 1300; i++) {
                int x = R.nextInt(S), y = R.nextInt(S);
                float l = rr(0.5f, 1);
                for (int k = 0; k < 3; k++) im.set((x + (randf() < 0.3f ? k : 0)) % S, (y - k + S) % S, l, l, l, 1);
            }
        }
        Texture t = tex(im, true);
        cache.put(key, t);
        return t;
    }

    public static Texture grain(String key, float base, int dots, float dl0, float dl1) {
        if (cache.containsKey(key)) return cache.get(key);
        int S = 64;
        Img im = new Img(S, S, new Color(base, base, base, 1));
        for (int i = 0; i < dots; i++) {
            float l = rr(dl0, dl1);
            im.set(R.nextInt(S), R.nextInt(S), l, l, l, 1);
            im.set(R.nextInt(S), R.nextInt(S), l * 0.9f, l * 0.9f, l * 0.9f, 1);
        }
        Texture t = tex(im, true);
        cache.put(key, t);
        return t;
    }

    public static Texture slab() {
        if (cache.containsKey("slab")) return cache.get("slab");
        int S = 128;
        Img im = new Img(S, S, new Color(0.78f, 0.78f, 0.78f, 1));
        noise(im, 0.1f);
        Color d = new Color(0.25f, 0.25f, 0.25f, 1);
        for (int i = 0; i < 5; i++) { im.fillRect(i * S / 4, 0, 1, S, d); im.fillRect(0, i * S / 4, S, 1, d); }
        Texture t = tex(im, true);
        cache.put("slab", t);
        return t;
    }

    public static Texture soft(float power) {
        String key = "soft" + power;
        if (cache.containsKey(key)) return cache.get(key);
        int S = 64;
        Img im = new Img(S, S, new Color(1, 1, 1, 0));
        for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
            float dx = x - S / 2f + 0.5f, dy = y - S / 2f + 0.5f;
            float r = (float) Math.sqrt(dx * dx + dy * dy) / (S / 2f);
            im.set(x, y, 1, 1, 1, (float) Math.pow(Math.max(0, 1 - r), power));
        }
        Texture t = tex(im, true);
        t.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
        cache.put(key, t);
        return t;
    }

    /** Кольцо ударной волны: яркий край, мягкий спад внутрь. */
    public static Texture ring() {
        if (cache.containsKey("ring")) return cache.get("ring");
        int S = 128;
        Img im = new Img(S, S, new Color(1, 1, 1, 0));
        for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
            float dx = x - S / 2f + 0.5f, dy = y - S / 2f + 0.5f;
            float r = (float) Math.sqrt(dx * dx + dy * dy) / (S / 2f);
            float a = r > 1f ? 0f : (r > 0.86f ? (1f - r) / 0.14f : (float) Math.pow(r / 0.86f, 5f));
            im.set(x, y, 1, 1, 1, MathUtils.clamp(a, 0, 1));
        }
        Texture t = tex(im, true);
        t.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
        cache.put("ring", t);
        return t;
    }

    public static Texture smoke() {
        if (cache.containsKey("smoke")) return cache.get("smoke");
        int S = 128;
        Noise fn = new Noise(11);
        Img im = new Img(S, S, new Color(1, 1, 1, 0));
        for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
            float dx = x - S / 2f + 0.5f, dy = y - S / 2f + 0.5f;
            float r = (float) Math.sqrt(dx * dx + dy * dy) / (S / 2f);
            float n = fn.fbm(x * 0.035f, y * 0.035f, 4) * 0.5f + 0.5f;
            float fall = MathUtils.clamp(1 - r, 0, 1);
            float a = MathUtils.clamp((float) Math.pow(fall, 1.3) * (0.35f + 1.1f * n) - 0.05f, 0, 1);
            float l = 0.8f + 0.2f * n;
            im.set(x, y, l, l, l, a);
        }
        Texture t = tex(im, true);
        t.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
        cache.put("smoke", t);
        return t;
    }

    public static Texture cloud() {
        if (cache.containsKey("cloud")) return cache.get("cloud");
        int W = 128, H = 64;
        Img im = new Img(W, H, new Color(1, 1, 1, 0));
        for (int k = 0; k < 22; k++) {
            float cx = rr(22, 106), cy = rr(28, 44) - Math.abs(cx - 64) * 0.1f, Rr = rr(9, 17);
            for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) {
                float r = (float) Math.sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy)) / Rr;
                if (r < 1f) {
                    float ca = im.get(x, y, 3);
                    float l = 1f - (y - cy + Rr) / (Rr * 2f) * 0.12f;
                    im.set(x, y, l, l, l, Math.min(1f, ca + (1f - r) * 0.5f));
                }
            }
        }
        Texture t = tex(im, true);
        t.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
        cache.put("cloud", t);
        return t;
    }

    public static Texture camoTile(String id) {
        String key = "camot_" + id;
        if (cache.containsKey(key)) return cache.get(key);
        JsonValue sk = skin(id);
        final int S = 256;
        final Img im = new Img(S, S, G.hex(sk.getString("hull")));
        JsonValue cols = skCols(sk);
        int nc = cols.size;
        String pat = sk.getString("pat", "");
        switch (pat) {
            case "blob3":
                for (int i = 0; i < 34; i++) {
                    Color col = skCol(cols, 1 + i % (nc - 1));
                    float cx = randf() * S, cy = randf() * S, r = 14 + randf() * 26;
                    for (int k = 0; k < 6; k++) {
                        float px = cx + rr(-r, r), py = cy + rr(-r * 0.5f, r * 0.5f), pr = r * rr(0.45f, 0.85f);
                        for (int ox = -S; ox <= S; ox += S) for (int oy = -S; oy <= S; oy += S) {
                            if (px + ox + pr < 0 || px + ox - pr > S || py + oy + pr < 0 || py + oy - pr > S) continue;
                            circle(im, px + ox, py + oy, pr, col);
                        }
                    }
                }
                break;
            case "digital":
                for (int y = 0; y < S; y += 8) for (int x = 0; x < S; x += 8)
                    if (randf() < 0.45f) im.fillRect(x, y, 8, 8, skCol(cols, 1 + R.nextInt(nc - 1)));
                break;
            case "stripes":
                for (int i = -8; i < 16; i++) band(im, i * 32f, 0, i * 32f + 64f, S, 12f, skCol(cols, 1));
                break;
            case "splinter":
                for (int i = 0; i < 44; i++) {
                    float x = randf() * (S - 40), y = randf() * (S - 30);
                    poly(im, new float[]{x, x + rr(14, 40), x + rr(6, 26)}, new float[]{y, y + rr(-12, 12), y + rr(12, 30)}, skCol(cols, 1 + i % (nc - 1)));
                }
                break;
            case "metal":
                for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
                    float k = 0.5f + 0.5f * (float) Math.sin((float) (x + y) / S * MathUtils.PI2);
                    im.set(x, y, skCol(cols, 0).cpy().lerp(skCol(cols, Math.min(2, nc - 1)), k));
                }
                break;
        }
        noise(im, 0.04f);
        Texture t = tex(im, true);
        cache.put(key, t);
        return t;
    }

    public static Texture panel() {
        if (cache.containsKey("panel")) return cache.get("panel");
        final int S = 256;
        Noise fn = new Noise(7);
        Img im = new Img(S, S, new Color(1, 0, 0, 1));
        // бесшовный шум: периодическая решётка 3 клетки на тайл
        for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
            float g = fn.periodicFbm(x / (float) S * 3f, y / (float) S * 3f, 3, 4) * 0.5f + 0.5f;
            im.set(x, y, 1f, MathUtils.clamp((g - 0.3f) * 1.8f, 0, 1), 0f, 1f);
        }
        java.util.ArrayList<int[]> rects = new java.util.ArrayList<>(), out = new java.util.ArrayList<>();
        rects.add(new int[]{0, 0, S, S});
        while (!rects.isEmpty()) {
            int[] r = rects.remove(rects.size() - 1);
            if ((r[2] > 120 || r[3] > 120 || randf() < 0.35f) && r[2] > 48 && r[3] > 48) {
                if (r[2] >= r[3]) {
                    int cut = (int) (r[2] * rr(0.3f, 0.7f));
                    rects.add(new int[]{r[0], r[1], cut, r[3]}); rects.add(new int[]{r[0] + cut, r[1], r[2] - cut, r[3]});
                } else {
                    int cut = (int) (r[3] * rr(0.3f, 0.7f));
                    rects.add(new int[]{r[0], r[1], r[2], cut}); rects.add(new int[]{r[0], r[1] + cut, r[2], r[3] - cut});
                }
            } else out.add(r);
        }
        for (int[] r : out) {
            int x0 = r[0], y0 = r[1];
            for (int x = x0; x < x0 + r[2]; x++) { seam(im, x, y0, 0.35f); seam(im, x, y0 + 1, 0.8f); }
            for (int y = y0; y < y0 + r[3]; y++) { seam(im, x0, y, 0.35f); seam(im, x0 + 1, y, 0.8f); }
            if (randf() < 0.5f) {
                for (int x = x0 + 4; x < x0 + r[2] - 2; x += 7) seam(im, x, y0 + 4, 0.55f);
                for (int y = y0 + 4; y < y0 + r[3] - 2; y += 7) seam(im, x0 + 4, y, 0.55f);
            }
        }
        for (int i = 0; i < 60; i++) {
            int x = R.nextInt(S), y = R.nextInt(S);
            float a = randf() * MathUtils.PI2;
            int L = 4 + R.nextInt(15);
            for (int k = 0; k < L; k++) {
                int px = Math.floorMod(x + (int) (Math.cos(a) * k), S), py = Math.floorMod(y + (int) (Math.sin(a) * k), S);
                im.p[(py * S + px) * 4 + 2] = 1f;
            }
        }
        Texture t = tex(im, true);
        cache.put("panel", t);
        return t;
    }

    static void seam(Img im, int x, int y, float v) {
        int S = im.w;
        x = Math.floorMod(x, S); y = Math.floorMod(y, S);
        int i = (y * S + x) * 4;
        im.p[i] = Math.min(im.p[i], v);
    }

    /** Текстура травы (из world.gd). */
    public static Texture grass() {
        if (cache.containsKey("grass")) return cache.get("grass");
        int S = 128;
        Img im = new Img(S, S, new Color(0.75f, 0.78f, 0.55f, 0f));
        for (int i = 0; i < 46; i++) {
            float x0 = rr(10, S - 10), hh = rr(S * 0.45f, S * 0.98f), bend = rr(-0.35f, 0.35f);
            float w0 = rr(1.6f, 2.6f), l = rr(0.7f, 1.0f);
            for (int k = 0; k < (int) hh; k++) {
                float t = k / hh, xc = x0 + bend * k * t, w = w0 * (1 - t * 0.85f);
                int py = S - 1 - k;
                float lum = l * (0.62f + 0.38f * t);
                for (int px = (int) Math.floor(xc - w - 1); px < (int) Math.ceil(xc + w + 1); px++) {
                    if (px < 0 || px >= S) continue;
                    float cov = MathUtils.clamp(w - Math.abs(px + 0.5f - xc) + 0.5f, 0, 1);
                    if (cov <= 0) continue;
                    float na = Math.max(im.get(px, py, 3), cov);
                    im.set(px, py, lum, lum * 1.02f, lum * 0.78f, na);
                }
            }
        }
        Texture t = tex(im, true);
        t.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
        cache.put("grass", t);
        return t;
    }

    /** Текстура дымного шлейфа (smoke_trail.gd). */
    public static Texture trail() {
        if (cache.containsKey("trail")) return cache.get("trail");
        int W = 32, H = 128;
        Noise fn = new Noise(3);
        Img im = new Img(W, H, new Color(1, 1, 1, 0));
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) {
            float u = (x + 0.5f) / W;
            float edge = (float) Math.pow(MathUtils.clamp(1 - Math.abs(u * 2 - 1), 0, 1), 1.2);
            float n = fn.periodicFbm(x / (float) W * 2f, y / (float) H * 8f, 0, 3) * 0.5f + 0.5f;
            float a = MathUtils.clamp(edge * (0.75f + 0.7f * n), 0, 1);
            float l = 0.86f + 0.14f * n;
            im.set(x, y, l, l, l, a);
        }
        Texture t = tex(im, true);
        cache.put("trail", t);
        return t;
    }

    /** Градиент ядра факела ракеты (fx.gd mat_core). */
    public static Texture flameCore() {
        if (cache.containsKey("fcore")) return cache.get("fcore");
        Img im = new Img(4, 64, Color.WHITE);
        for (int y = 0; y < 64; y++) {
            float k = y / 63f;
            for (int x = 0; x < 4; x++) im.set(x, y, 1, 0.95f - k * 0.35f, 0.8f - k * 0.6f, (float) Math.pow(1 - k, 1.6));
        }
        Texture t = tex(im, false);
        t.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
        cache.put("fcore", t);
        return t;
    }

    public static Texture white() {
        if (cache.containsKey("white")) return cache.get("white");
        Img im = new Img(4, 4, Color.WHITE);
        Texture t = tex(im, false);
        cache.put("white", t);
        return t;
    }

    public static void clear() {
        for (Texture t : cache.values()) t.dispose();
        cache.clear();
    }

    /** Градиентный шум с фрактальным суммированием и периодичной версией (замена FastNoiseLite). */
    public static final class Noise {
        final int[] perm = new int[512];
        final float[] gx = new float[256], gy = new float[256];

        public Noise(long seed) {
            Random r = new Random(seed);
            int[] p = new int[256];
            for (int i = 0; i < 256; i++) { p[i] = i; float a = r.nextFloat() * MathUtils.PI2; gx[i] = (float) Math.cos(a); gy[i] = (float) Math.sin(a); }
            for (int i = 255; i > 0; i--) { int j = r.nextInt(i + 1); int t = p[i]; p[i] = p[j]; p[j] = t; }
            for (int i = 0; i < 512; i++) perm[i] = p[i & 255];
        }

        static float fade(float t) { return t * t * t * (t * (t * 6 - 15) + 10); }

        float grad(int ix, int iy, float x, float y) {
            int h = perm[(perm[ix & 255] + iy) & 511] & 255;
            return gx[h] * (x - ix) + gy[h] * (y - iy);
        }

        public float noise(float x, float y) { return noiseP(x, y, 0); }

        /** period<=0 — без периода. */
        public float noiseP(float x, float y, int period) {
            int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y);
            float fx = fade(x - x0), fy = fade(y - y0);
            int ax = x0, bx = x0 + 1, ay = y0, by = y0 + 1;
            int wx0 = period > 0 ? Math.floorMod(ax, period) : ax, wx1 = period > 0 ? Math.floorMod(bx, period) : bx;
            int wy0 = period > 0 ? Math.floorMod(ay, period) : ay, wy1 = period > 0 ? Math.floorMod(by, period) : by;
            float n00 = gradW(wx0, wy0, x - ax, y - ay), n10 = gradW(wx1, wy0, x - bx, y - ay);
            float n01 = gradW(wx0, wy1, x - ax, y - by), n11 = gradW(wx1, wy1, x - bx, y - by);
            return MathUtils.lerp(MathUtils.lerp(n00, n10, fx), MathUtils.lerp(n01, n11, fx), fy) * 1.414f;
        }

        float gradW(int ix, int iy, float dx, float dy) {
            int h = perm[(perm[ix & 255] + (iy & 255)) & 511] & 255;
            return gx[h] * dx + gy[h] * dy;
        }

        public float fbm(float x, float y, int oct) {
            float s = 0, a = 1, f = 1, norm = 0;
            for (int i = 0; i < oct; i++) { s += noiseP(x * f, y * f, 0) * a; norm += a; a *= 0.5f; f *= 2f; }
            return s / norm;
        }

        public float periodicFbm(float x, float y, int period, int oct) {
            float s = 0, a = 1, norm = 0; int f = 1;
            for (int i = 0; i < oct; i++) { s += noiseP(x * f, y * f, period > 0 ? period * f : 0) * a; norm += a; a *= 0.5f; f *= 2; }
            return s / norm;
        }
    }

    private Tex() { }
}
