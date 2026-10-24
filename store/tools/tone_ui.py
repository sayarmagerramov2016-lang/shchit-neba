"""Гасит мультяшность UI-кита: латунь вместо глянцевого золота, приглушённые цвета,
срезанные блики, матовая зернистость. Альфа не трогается."""
import sys, os, numpy as np
from PIL import Image

def rgb2hsv(a):
    r, g, b = a[..., 0], a[..., 1], a[..., 2]
    mx = a.max(-1); mn = a.min(-1); d = mx - mn
    h = np.zeros_like(mx)
    m = d > 1e-6
    rm = m & (mx == r); gm = m & (mx == g) & ~rm; bm = m & ~rm & ~gm
    h[rm] = ((g - b)[rm] / d[rm]) % 6
    h[gm] = (b - r)[gm] / d[gm] + 2
    h[bm] = (r - g)[bm] / d[bm] + 4
    h = h / 6.0
    s = np.where(mx > 1e-6, d / np.maximum(mx, 1e-6), 0)
    return h, s, mx

def hsv2rgb(h, s, v):
    i = np.floor(h * 6).astype(int) % 6
    f = h * 6 - np.floor(h * 6)
    p = v * (1 - s); q = v * (1 - f * s); t = v * (1 - (1 - f) * s)
    out = np.zeros(h.shape + (3,))
    for k, (R, G, B) in enumerate([(v, t, p), (q, v, p), (p, v, t), (p, q, v), (t, p, v), (v, p, q)]):
        mk = i == k
        out[mk, 0] = R[mk]; out[mk, 1] = G[mk]; out[mk, 2] = B[mk]
    return out

def process(src, dst, seed):
    im = Image.open(src).convert("RGBA")
    a = np.asarray(im).astype(np.float64) / 255.0
    rgb, al = a[..., :3], a[..., 3]
    h, s, v = rgb2hsv(rgb)
    deg = h * 360

    gold = (deg >= 25) & (deg <= 60)
    blue = (deg >= 170) & (deg <= 250)
    red = (deg < 15) | (deg > 340)

    # латунь: золото чуть к оливке, меньше насыщенность, темнее
    h = np.where(gold, (h * 360 + 3) / 360, h)
    s = np.where(gold, s * 0.62, s)
    v = np.where(gold, (v ** 1.25) * 0.86, v)
    # холодные экраны радара/плашек и голубые иконки — тише и темнее
    s = np.where(blue, s * 0.55, s)
    v = np.where(blue & (v > 0.5), v * 0.82, v)
    # белые блестящие детали (шестерня) — в сталь
    v = np.where((s < 0.15) & (v > 0.6), 0.6 + (v - 0.6) * 0.35, v)
    # красный бейдж оставить заметным, но без леденцового блеска
    s = np.where(red, s * 0.80, s)
    v = np.where(red, v * 0.82, v)
    # остальное — общий приглушённый тон
    s = np.where(~(gold | blue | red), s * 0.6, s)

    # срезать глянцевые блики (мягкое сжатие верха)
    knee = 0.55
    v = np.where(v > knee, knee + (v - knee) * 0.45, v)
    # блики с малой насыщенностью (белые «зайчики») тоже гасим
    s = np.where((v > 0.5) & (s < 0.25), s, s)

    out = hsv2rgb(h % 1.0, np.clip(s, 0, 1), np.clip(v, 0, 1))
    # лёгкий холодный графитовый оттенок
    out = out * np.array([0.97, 0.99, 1.0])
    # матовое зерно металла
    rng = np.random.default_rng(seed)
    out += rng.normal(0, 0.012, out.shape[:2])[..., None]
    out = np.clip(out, 0, 1)

    res = np.concatenate([out, al[..., None]], -1)
    Image.fromarray((res * 255 + 0.5).astype(np.uint8), "RGBA").save(dst, optimize=True)

if __name__ == "__main__":
    src_dir, dst_dir = sys.argv[1], sys.argv[2]
    os.makedirs(dst_dir, exist_ok=True)
    for k, f in enumerate(sorted(os.listdir(src_dir))):
        if f.endswith(".png"):
            process(os.path.join(src_dir, f), os.path.join(dst_dir, f), k)
            print(f)
