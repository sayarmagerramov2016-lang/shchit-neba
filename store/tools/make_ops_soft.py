#!/usr/bin/env python3
"""Рисует операторов (чиби аниме-девчонок) для «Щит неба 3D» — 3.7.1, мягкая рисовка с тенями.

Запуск: python3 store/tools/make_ops.py assets/ops
Стиль по примеру владельца: большая голова (~42% роста), маленькое тельце, цветной тонкий контур,
светотень (свет сверху слева), блики на волосах, тень от чёлки на лице, глянцевые глаза.
На каждую девчонку и наряд (0 — обычный, 1 — с 5-го уровня дружбы):
  <id>_<o>_body.png  — тело без лица и без указывающей руки (256×512), ноги стоят на FEET_Y
  <id>_<o>_arm.png   — указывающая рука, ось у плеча (ARM_PX, ARM_PY), крепится в SHOULDER
  <id>_<o>_por.png   — портрет 200×200
На девчонку 7 лиц <id>_face_<эмоция>.png (FACE_W×FACE_H, кладётся в FACE_X, FACE_Y):
norm, blink, shout, joy, panic, worry, cry.
Те же числа записаны в ui/OpUI.java — менять вместе.
"""
import math
import os
import sys

from PIL import Image, ImageDraw, ImageFilter, ImageChops

S = 4
W, H = 256, 512
FEET_Y = 478
FACE_X, FACE_Y, FACE_W, FACE_H = 48, 142, 160, 108
ARM_W, ARM_H, ARM_PX, ARM_PY = 104, 44, 14, 22
SHOULDER = (100, 264)
EMOS = ["norm", "blink", "shout", "joy", "panic", "worry", "cry"]

SKIN, SKIN_SH, SKIN_HL = "#ffe6d8", "#f4bba8", "#fff5ee"
LASH = "#3a2230"


# ---------------------------------------------------------------- цвет
def hx(c, a=255):
    if isinstance(c, tuple):
        return c if len(c) == 4 else c + (a,)
    c = c.lstrip("#")
    return (int(c[0:2], 16), int(c[2:4], 16), int(c[4:6], 16), a)


def mix(a, b, k):
    a, b = hx(a), hx(b)
    return "#%02x%02x%02x" % tuple(int(a[i] + (b[i] - a[i]) * k) for i in range(3))


def dk(c, k=0.3):
    return mix(c, "#2a1630", k)


def lt(c, k=0.3):
    return mix(c, "#ffffff", k)


def sh(c, k=0.22):
    """Цвет тени: темнее и чуть в фиолет — как в аниме."""
    return mix(mix(c, "#6a4a8a", k * 0.6), "#1a1020", k * 0.5)


# ---------------------------------------------------------------- маски
def P(pts):
    return [(x * S, y * S) for x, y in pts]


def B(x0, y0, x1, y1):
    return [x0 * S, y0 * S, x1 * S, y1 * S]


def new_mask(w=W, h=H):
    return Image.new("L", (w * S, h * S), 0)


def shift(m, dx, dy):
    out = Image.new("L", m.size, 0)
    out.paste(m, (int(dx * S), int(dy * S)))
    return out


def m_poly(pts, w=W, h=H):
    m = new_mask(w, h); ImageDraw.Draw(m).polygon(P(pts), fill=255); return m


def m_ell(x0, y0, x1, y1, w=W, h=H):
    m = new_mask(w, h); ImageDraw.Draw(m).ellipse(B(x0, y0, x1, y1), fill=255); return m


def m_rr(x0, y0, x1, y1, r, w=W, h=H):
    m = new_mask(w, h); ImageDraw.Draw(m).rounded_rectangle(B(x0, y0, x1, y1), radius=r * S, fill=255); return m


def m_line(pts, wd, w=W, h=H):
    m = new_mask(w, h)
    d = ImageDraw.Draw(m)
    d.line(P(pts), fill=255, width=int(wd * S), joint="curve")
    r = wd * S / 2
    for x, y in (pts[0], pts[-1]):
        d.ellipse([x * S - r, y * S - r, x * S + r, y * S + r], fill=255)
    return m


def union(*ms):
    out = ms[0]
    for m in ms[1:]:
        out = ImageChops.lighter(out, m)
    return out


def roundm(m, r=5):
    return m.filter(ImageFilter.GaussianBlur(r * S)).point(lambda v: 255 if v > 128 else 0)


def sub(a, b):
    return ImageChops.subtract(a, b)


def mul(a, b):
    return ImageChops.multiply(a, b)


def grow(m, px):
    k = int(px * S) * 2 + 1
    return m.filter(ImageFilter.MaxFilter(k if k < 26 else 25))


def vgrad(h0, h1, top, bot, w=W, h=H):
    """Вертикальный градиент маской: 255 на h0, 0 на h1."""
    g = Image.new("L", (1, h * S))
    for y in range(h * S):
        t = (y / S - h0) / max(1, (h1 - h0))
        g.putpixel((0, y), int(255 * min(1, max(0, (1 - t) * top + t * bot))))
    return g.resize((w * S, h * S))


class Pic:
    def __init__(self, w=W, h=H):
        self.w, self.h = w, h
        self.im = Image.new("RGBA", (w * S, h * S), (0, 0, 0, 0))

    def fill(self, mask, col, alpha=1.0):
        layer = Image.new("RGBA", self.im.size, hx(col))
        a = mask if alpha >= 1 else mask.point(lambda v: int(v * alpha))
        if len(hx(col)) == 4 and hx(col)[3] < 255:
            a = a.point(lambda v: int(v * hx(col)[3] / 255))
        layer.putalpha(a)
        self.im.alpha_composite(layer)

    def part(self, mask, base, sv=(9, 8), shade=0.24, hl=0.0, line=1.8, soft=1.2, lcol=None):
        """Деталь: заливка, тень с нижне-правой стороны, блик сверху-слева, цветной контур."""
        self.fill(mask, base)
        if shade > 0:
            sm = sub(mask, shift(mask, -sv[0], -sv[1]))
            if soft:
                sm = mul(sm.filter(ImageFilter.GaussianBlur(soft * S)), mask)
            self.fill(sm, sh(base, shade))
        if hl > 0:
            hm = sub(mask, shift(mask, sv[0] * 0.6, sv[1] * 0.6))
            hm = mul(hm.filter(ImageFilter.GaussianBlur(2 * S)), mask)
            self.fill(hm, lt(base, 0.45), hl)
        if line:
            edge = sub(grow(mask, line * 0.5), mask)
            inner = sub(mask, ImageChops.invert(grow(ImageChops.invert(mask), line * 0.5)))
            self.fill(union(edge, inner), lcol or mix(dk(base, 0.55), "#3a2030", 0.35))

    def line(self, pts, col, wd, alpha=1.0):
        self.fill(m_line(pts, wd, self.w, self.h), col, alpha)

    def done(self):
        return self.im.resize((self.w, self.h), Image.LANCZOS)


def curve(p0, p1, p2, n=14):
    return [((1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t * t * p2[0],
             (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t * t * p2[1]) for t in [i / n for i in range(n + 1)]]


def star_pts(cx, cy, r, r2, n=5, rot=-90):
    pts = []
    for i in range(n * 2):
        a = math.radians(rot + i * 180 / n)
        rr_ = r if i % 2 == 0 else r2
        pts.append((cx + math.cos(a) * rr_, cy + math.sin(a) * rr_))
    return pts


# ---------------------------------------------------------------- девчонки
GIRLS = {
    "liza": dict(hair="#8a5432", eye="#3f9a5e", style="pony", extra=["wrench", "cap"],
                 o=[dict(top="#7a8a52", acc="#f08a2a", bot="#5d6b40", legs="#4a4038", shoes="#5a3c26", kind="overall"),
                    dict(top="#24356a", acc="#e8c25a", bot="#1c2a56", legs="#2a2a3e", shoes="#1e1e2a", kind="overall")]),
    "katya": dict(hair="#f4d27a", eye="#4f8ad8", style="long", extra=["cup"],
                  o=[dict(top="#efdcb4", acc="#c8603e", bot="#5a8a54", legs="#f8f2ea", shoes="#8a5232", kind="sweater"),
                     dict(top="#f8c2d2", acc="#ffffff", bot="#d06a88", legs="#ffffff", shoes="#d06a88", kind="sweater")]),
    "sonya": dict(hair="#525866", eye="#8a76e0", style="bob", extra=["scarf"],
                  o=[dict(top="#687a4a", acc="#9aaa66", bot="#55613e", legs="#424a30", shoes="#33332c", kind="camo"),
                     dict(top="#e6ecf0", acc="#a6b4c0", bot="#cad4dc", legs="#b4c0ca", shoes="#64707a", kind="camo")]),
    "anya": dict(hair="#9cd8f4", eye="#3478e0", style="twin", extra=["phones"],
                 o=[dict(top="#2a3c6c", acc="#59e0ff", bot="#22325a", legs="#24243a", shoes="#16162a", kind="uniform"),
                    dict(top="#f4f8fc", acc="#2fb8e8", bot="#64b0e0", legs="#eef4fa", shoes="#3478b0", kind="uniform")]),
    "masha": dict(hair="#f49446", eye="#e6b432", style="short", extra=["cat", "bell"],
                  o=[dict(top="#e04a42", acc="#ffffff", bot="#f8f4ee", legs="#ffffff", shoes="#a83028", kind="sailor"),
                     dict(top="#2e2a34", acc="#e8c25a", bot="#423c48", legs="#2e2a34", shoes="#e8c25a", kind="sailor")]),
    "rin": dict(hair="#ff5ac0", eye="#46f0ff", style="side", extra=["visor"],
                o=[dict(top="#24202e", acc="#ff4fd8", bot="#2e2a3a", legs="#1a1822", shoes="#ff4fd8", kind="cyber"),
                   dict(top="#18222c", acc="#3ff0ff", bot="#1e2a38", legs="#121a22", shoes="#3ff0ff", kind="cyber")]),
    "yuki": dict(hair="#f2f7ff", eye="#5ab0ff", style="long", extra=["flake"],
                 o=[dict(top="#c6e6ff", acc="#ffffff", bot="#96ccf6", legs="#eef8ff", shoes="#5e9edc", kind="dress"),
                    dict(top="#2c4a86", acc="#c6e6ff", bot="#223a70", legs="#dceafa", shoes="#c6e6ff", kind="dress")]),
    "elka": dict(hair="#44a856", eye="#e44646", style="buns", extra=["garland", "starpin"],
                 o=[dict(top="#36964a", acc="#ffd23a", bot="#2a7c40", legs="#ea4444", shoes="#922626", kind="dress"),
                    dict(top="#e04444", acc="#ffffff", bot="#c23232", legs="#ffffff", shoes="#36964a", kind="dress")]),
    "akira": dict(hair="#2c2834", eye="#da3a3a", style="highpony", extra=["katana"],
                  o=[dict(top="#f8f4ee", acc="#d0303f", bot="#b82634", legs="#2e2a34", shoes="#32262e", kind="hakama"),
                     dict(top="#342a3c", acc="#a466ea", bot="#221c2c", legs="#1a1620", shoes="#a466ea", kind="hakama")]),
    "sneg": dict(hair="#eaf0fa", eye="#3a8ae0", style="braid", extra=["kokoshnik"],
                 o=[dict(top="#62a8ee", acc="#ffffff", bot="#5294de", legs="#ffffff", shoes="#ffffff", kind="coat"),
                    dict(top="#ccd4e0", acc="#ffbcd4", bot="#b0bacb", legs="#ffffff", shoes="#ffbcd4", kind="coat")]),
    "valk": dict(hair="#ffdc72", eye="#4a92ff", style="long", extra=["wings", "halo", "tiara"],
                 o=[dict(top="#f8f4ea", acc="#eabc36", bot="#efe8d6", legs="#f8f4ea", shoes="#eabc36", kind="armor"),
                    dict(top="#302840", acc="#a676ff", bot="#241e30", legs="#302840", shoes="#a676ff", kind="armor")]),
}

HCX, HCY, HRX, HRY = 128, 156, 92, 90  # голова


# ---------------------------------------------------------------- волосы
def hair_back_mask(g):
    sty = g["style"]
    ms = []
    if sty in ("long", "braid"):
        ms.append(m_poly([(40, 150), (216, 150), (226, 280), (214, 352), (128, 366), (42, 352), (30, 280)]))
    elif sty == "twin":
        for sx in (1, -1):
            cx = 128 + sx * 104
            ms.append(m_poly([(cx - 24 * sx, 112), (cx + 12 * sx, 128), (cx + 24 * sx, 230), (cx + 14 * sx, 340),
                              (cx - 2 * sx, 384), (cx - 18 * sx, 330), (cx - 26 * sx, 222)]))
    elif sty == "pony":
        ms.append(m_poly([(150, 100), (222, 138), (240, 240), (224, 326), (204, 270), (190, 172)]))
    elif sty == "highpony":
        ms.append(m_poly([(160, 56), (226, 86), (246, 200), (240, 320), (218, 382), (204, 280), (192, 132)]))
        ms.append(m_poly([(40, 150), (216, 150), (220, 262), (128, 272), (36, 262)]))
    elif sty == "side":
        ms.append(m_poly([(170, 112), (232, 152), (244, 266), (226, 348), (208, 262), (190, 166)]))
    elif sty == "bob":
        ms.append(m_poly([(34, 150), (222, 150), (232, 238), (204, 262), (52, 262), (24, 238)]))
    elif sty == "short":
        ms.append(m_poly([(38, 150), (218, 150), (224, 226), (32, 226)]))
    elif sty == "buns":
        ms.append(m_poly([(40, 150), (216, 150), (222, 254), (34, 254)]))
    return roundm(union(*ms), 6) if ms else new_mask()


def hair_front_mask(g):
    sty = g["style"]
    crown = []
    for i in range(25):
        a = math.radians(180 + i * 7.5)
        crown.append((HCX + math.cos(a) * (HRX + 3), HCY - 4 + math.sin(a) * (HRY + 2)))
    crown += [(HCX + HRX + 3, 128), (HCX - HRX - 3, 128)]
    ms = [m_poly(crown)]
    tx = [46, 66, 86, 106, 128, 150, 170, 190, 210]
    ty = [170, 154, 176, 152, 172, 152, 176, 154, 170]
    if sty == "side":
        ty = [190, 182, 178, 166, 158, 146, 140, 134, 150]
    for x, y in zip(tx, ty):
        bend = 6 if x < 128 else -6
        pts = curve((x - 16, 96), (x - 12 + bend, y - 26), (x, y), 10) + curve((x, y), (x + 10 + bend, y - 30), (x + 16, 96), 10)
        ms.append(m_poly(pts))
    m = union(*ms)
    if sty == "short":
        m = union(m, m_poly([(34, 140), (56, 150), (56, 208), (40, 220)]), m_poly([(222, 140), (200, 150), (200, 208), (216, 220)]))
    else:
        m = union(m, m_poly([(34, 140), (56, 150), (60, 236), (48, 266), (30, 230)]), m_poly([(222, 140), (200, 150), (196, 236), (208, 266), (226, 230)]))
    m = roundm(m, 1.5)
    if sty == "buns":
        m = union(m, m_ell(36, 44, 92, 98), m_ell(164, 44, 220, 98))
    return m


def draw_hair_back(p, g):
    h = g["hair"]
    m = hair_back_mask(g)
    p.part(m, dk(h, 0.18), sv=(10, 10), shade=0.3)
    if g["style"] == "twin":
        for sx in (1, -1):
            cx = 128 + sx * 104
            p.part(m_ell(cx - 12, 108, cx + 12, 132), "#ff6a8a", sv=(3, 3), hl=0.5)
    if g["style"] == "side":
        p.part(m_ell(198, 116, 224, 142), "#46f0ff" if "visor" in g["extra"] else "#ff6a8a", sv=(3, 3), hl=0.5)
    if g["style"] == "braid":
        for i in range(6):
            y = 252 + i * 24
            p.part(m_ell(198 - i * 2, y, 228 - i * 2, y + 30), h, sv=(4, 4), shade=0.3, hl=0.4)
        p.part(m_ell(200, 398, 224, 414), "#62a8ee", sv=(2, 2))


def draw_hair_front(p, g):
    h = g["hair"]
    m = hair_front_mask(g)
    p.fill(m, h)
    # тень снизу пряди и затемнение к низу
    p.fill(mul(m, vgrad(150, 260, 0, 1)), sh(h, 0.22), 0.6)
    sm = sub(m, shift(m, -6, -10))
    p.fill(mul(sm.filter(ImageFilter.GaussianBlur(1.5 * S)), m), sh(h, 0.3))
    # пряди
    for x0, y1 in ((56, 150), (76, 160), (96, 156), (117, 154), (139, 154), (160, 156), (180, 160), (200, 150)):
        p.fill(mul(m_line(curve((x0 + (6 if x0 < 128 else -6), 92), (x0, 128), (x0 + (3 if x0 < 128 else -3), y1)), 1.8), m), sh(h, 0.3), 0.75)
    # «ангельское кольцо» — блик на макушке
    ring = new_mask()
    ImageDraw.Draw(ring).arc(B(62, 86, 194, 150), 200, 340, fill=255, width=int(7 * S))
    p.fill(mul(ring.filter(ImageFilter.GaussianBlur(1.5 * S)), m), lt(h, 0.6), 0.85)
    for cx, cy in ((92, 104), (160, 102)):
        p.fill(mul(m_ell(cx - 7, cy - 3, cx + 7, cy + 3), m), "#ffffff", 0.7)
    edge = sub(grow(m, 0.9), m)
    p.fill(edge, mix(dk(h, 0.55), "#3a2030", 0.35))
    if g["style"] == "highpony":
        p.part(m_ell(150, 52, 178, 74), "#d0303f", sv=(3, 3), hl=0.5)
    return m


# ---------------------------------------------------------------- тело
def body(p, g, o):
    top, acc, bot, legs, shoes, kind = o["top"], o["acc"], o["bot"], o["legs"], o["shoes"], o["kind"]
    # ноги
    for x0 in (106, 132):
        p.part(m_rr(x0, 372, x0 + 18, 458, 8), legs, sv=(5, 0), shade=0.22)
    # обувь
    for x0 in (100, 130):
        p.part(m_rr(x0, 448, x0 + 26, FEET_Y, 11), shoes, sv=(4, 5), shade=0.3, hl=0.5)
        p.line([(x0 + 6, 470), (x0 + 20, 470)], lt(shoes, 0.25), 1.6, 0.8)
    # низ
    if kind in ("overall", "uniform", "camo", "cyber"):
        p.part(m_poly([(96, 322), (160, 322), (164, 382), (130, 384), (128, 364), (126, 384), (92, 382)]), bot, sv=(8, 6))
    elif kind == "hakama":
        p.part(m_poly([(94, 320), (162, 320), (180, 436), (128, 440), (76, 436)]), bot, sv=(10, 6))
        for x0, x1 in ((128, 128), (110, 102), (146, 154)):
            p.line([(x0, 336), (x1, 434)], sh(bot, 0.3), 1.6, 0.8)
    elif kind == "coat":
        p.part(m_poly([(92, 314), (164, 314), (184, 428), (72, 428)]), top, sv=(10, 6))
        p.part(m_rr(68, 418, 188, 436, 9), "#ffffff", sv=(3, 4), shade=0.15)
    elif kind == "armor":
        p.part(m_poly([(94, 320), (162, 320), (182, 394), (74, 394)]), bot, sv=(10, 6))
        p.line([(94, 324), (162, 324)], acc, 5)
    else:
        p.part(m_poly([(94, 320), (162, 320), (182, 388), (74, 388)]), bot, sv=(10, 6))
        for x in range(86, 176, 16):
            p.line([(x + 6, 326), (x + 2, 384)], sh(bot, 0.25), 1.4, 0.7)
    # торс (пухлый)
    tm = m_rr(94, 252, 162, 336, 20)
    p.part(tm, top, sv=(10, 6), hl=0.3)
    if kind == "overall":
        p.part(m_rr(106, 282, 150, 336, 7), bot, sv=(6, 4))
        p.line([(108, 256), (110, 284)], bot, 6); p.line([(148, 256), (146, 284)], bot, 6)
        p.part(m_ell(106, 280, 114, 288), acc, sv=(1, 1), line=1)
        p.part(m_ell(142, 280, 150, 288), acc, sv=(1, 1), line=1)
        p.part(m_rr(118, 298, 138, 314, 3), sh(bot, 0.15), sv=(2, 2), line=1.2)
    elif kind == "sweater":
        for y in (284, 302, 320):
            p.line([(100, y), (156, y)], sh(top, 0.12), 2, 0.8)
        p.line([(104, 266), (152, 266)], acc, 4)
    elif kind == "camo":
        for (x, y, r) in ((108, 272, 9), (148, 292, 11), (120, 316, 8), (152, 266, 7), (102, 310, 7)):
            p.fill(mul(m_ell(x - r, y - r * 0.7, x + r, y + r * 0.7), tm), acc)
        p.line([(128, 256), (128, 334)], sh(top, 0.3), 1.6)
    elif kind == "uniform":
        p.part(m_poly([(112, 254), (128, 282), (144, 254)]), "#ffffff", sv=(2, 2), line=1.2)
        p.part(m_poly([(124, 270), (132, 270), (134, 298), (128, 306), (122, 298)]), acc, sv=(2, 2), line=1.2)
        p.part(m_rr(100, 286, 114, 294, 2), acc, sv=(1, 1), line=1)
    elif kind == "sailor":
        p.part(m_poly([(98, 254), (158, 254), (148, 282), (128, 300), (108, 282)]), acc, sv=(4, 4), line=1.4)
        p.part(m_poly([(118, 284), (138, 284), (146, 306), (128, 298), (110, 306)]), "#ea4242" if top != "#e04a42" else "#2a4a9a", sv=(2, 2), hl=0.4, line=1.2)
    elif kind == "cyber":
        p.line([(100, 262), (112, 330)], acc, 2.4); p.line([(156, 262), (144, 330)], acc, 2.4)
        p.line([(112, 296), (144, 296)], acc, 2)
        p.part(m_ell(122, 308, 134, 320), acc, sv=(1, 1), hl=0.6, line=1)
        p.part(m_rr(90, 248, 166, 266, 9), lt(top, 0.12), sv=(3, 4))
    elif kind == "dress":
        p.part(m_poly([(112, 252), (128, 270), (144, 252)]), acc, sv=(2, 2), line=1.2)
        p.line([(96, 322), (160, 322)], acc, 4)
        p.part(m_ell(122, 284, 134, 296), acc, sv=(1, 1), hl=0.5, line=1)
    elif kind == "hakama":
        p.part(m_poly([(106, 252), (128, 302), (150, 252)]), acc, sv=(3, 3), line=1.4)
        p.part(m_poly([(112, 252), (128, 288), (144, 252)]), top, sv=(2, 2), line=1)
        p.part(m_rr(94, 314, 162, 328, 3), acc, sv=(2, 3))
    elif kind == "coat":
        p.part(m_rr(88, 246, 168, 266, 10), "#ffffff", sv=(3, 4), shade=0.12)
        p.line([(128, 266), (128, 420)], "#ffffff", 7)
        for y in (290, 326, 364):
            p.part(m_ell(122, y - 5, 134, y + 7), acc if acc != "#ffffff" else "#e8c25a", sv=(2, 2), hl=0.5, line=1)
    elif kind == "armor":
        p.part(m_poly([(98, 256), (158, 256), (152, 298), (128, 312), (104, 298)]), acc, sv=(4, 4), hl=0.6, line=1.4)
        p.part(m_poly([(106, 262), (150, 262), (146, 294), (128, 304), (110, 294)]), top, sv=(3, 3), line=1)
        p.part(m_poly(star_pts(128, 280, 9, 4)), acc, sv=(1, 1), hl=0.6, line=1)
        p.part(m_rr(84, 246, 108, 268, 10), acc, sv=(3, 3), hl=0.6)
        p.part(m_rr(148, 246, 172, 268, 10), acc, sv=(3, 3), hl=0.6)
    # шея
    p.part(m_rr(118, 230, 138, 258, 6), SKIN, sv=(3, 3), shade=0.1, line=1.2)
    p.fill(m_rr(118, 240, 138, 254, 4), SKIN_SH, 0.8)
    # левая рука (у зрителя справа) — согнута, кисть у груди как у примера
    sl = top if kind != "armor" else acc
    arm = m_line([(162, 266), (176, 300), (162, 318)], 16)
    p.part(arm, sl, sv=(5, 4), hl=0.3)
    p.part(m_ell(146, 306, 168, 328), SKIN, sv=(3, 3), shade=0.14, line=1.3)


def head(p, g):
    face = m_ell(HCX - 86, HCY - 74, HCX + 86, HCY + 90)
    p.part(face, SKIN, sv=(6, 8), shade=0.12, line=1.6, lcol="#c08a7a")
    # мягкий градиент румянца на щеках снизу
    p.fill(mul(face, vgrad(190, 246, 0, 1)), "#ffd2c4", 0.45)
    # тень от чёлки на лбу
    hm = hair_front_mask(g)
    sm = mul(shift(hm, 2, 9), face).filter(ImageFilter.GaussianBlur(1.6 * S))
    p.fill(mul(sm, face), SKIN_SH, 0.75)
    return face


def extras_back(p, g, o):
    ex = g["extra"]
    if "wings" in ex:
        wc = "#ffffff" if o["top"] != "#302840" else "#433460"
        for sx in (1, -1):
            pts = [(128 + sx * 26, 262)]
            for i in range(6):
                a = math.radians(-64 + i * 23)
                r = 112 - i * 6
                pts.append((128 + sx * (26 + math.cos(a) * r), 262 + math.sin(a) * r))
            pts.append((128 + sx * 36, 322))
            p.part(m_poly(pts), wc, sv=(-8 * sx if sx < 0 else 8, 8), shade=0.25, hl=0.4, line=1.6, lcol=dk(o["acc"], 0.25))
            for i in range(4):
                a = math.radians(-44 + i * 24)
                p.line([(128 + sx * 40, 276), (128 + sx * (40 + math.cos(a) * 64), 276 + math.sin(a) * 64)], lt(o["acc"], 0.4), 1.6, 0.8)
    if "katana" in ex:
        p.part(m_line([(58, 380), (214, 172)], 8), "#3a2a3a", sv=(2, 2), line=1.4)
        p.part(m_line([(178, 222), (214, 172)], 5), "#dfe4ea", sv=(1, 1), hl=0.6, line=1)
        p.part(m_line([(170, 214), (186, 230)], 6), "#e8c25a", sv=(1, 1), line=1)


def extras_front(p, g, o):
    ex = g["extra"]
    acc = o["acc"]
    if "cap" in ex:
        cm = new_mask(); ImageDraw.Draw(cm).chord(B(48, 48, 208, 172), 180, 360, fill=255)
        p.part(cm, o["top"], sv=(8, 6), hl=0.4)
        p.part(m_poly([(48, 108), (208, 108), (230, 120), (36, 120)]), sh(o["top"], 0.1), sv=(3, 3))
        p.part(m_ell(118, 70, 138, 90), acc, sv=(2, 2), hl=0.6, line=1)
    if "phones" in ex:
        bm = new_mask(); ImageDraw.Draw(bm).arc(B(32, 52, 224, 240), 190, 350, fill=255, width=int(8 * S))
        p.part(bm, "#2c2c3a", sv=(2, 2), line=1)
        p.part(m_rr(24, 146, 48, 196, 9), "#2c2c3a", sv=(3, 3), hl=0.4)
        p.part(m_rr(208, 146, 232, 196, 9), "#2c2c3a", sv=(3, 3), hl=0.4)
        p.fill(m_ell(28, 158, 44, 184), acc)
        p.line([(40, 190), (58, 214), (80, 220)], "#2c2c3a", 3)
        p.part(m_ell(76, 214, 88, 226), acc, sv=(1, 1), line=1)
    if "cat" in ex:
        for sx in (1, -1):
            cx = 128 + sx * 60
            p.part(m_poly([(cx - 26 * sx, 92), (cx + 10 * sx, 30), (cx + 28 * sx, 98)]), g["hair"], sv=(4, 4), hl=0.3)
            p.fill(m_poly([(cx - 12 * sx, 88), (cx + 8 * sx, 46), (cx + 18 * sx, 92)]), "#ffc6cf")
    if "bell" in ex:
        p.part(m_ell(120, 250, 136, 266), "#eac85a", sv=(2, 2), hl=0.7, line=1.2)
    if "visor" in ex:
        p.part(m_rr(50, 118, 206, 134, 7), acc, sv=(2, 3), hl=0.7, line=1.2)
        p.line([(60, 124), (196, 124)], "#ffffff", 1.4, 0.8)
    if "flake" in ex or "starpin" in ex:
        cx, cy = 188, 118
        if "starpin" in ex:
            p.part(m_poly(star_pts(cx, cy - 4, 15, 6)), "#ffd23a", sv=(2, 2), hl=0.6, line=1.3)
        else:
            for i in range(6):
                a = math.radians(i * 60)
                p.line([(cx, cy), (cx + math.cos(a) * 15, cy + math.sin(a) * 15)], "#5ab0ff", 3)
            p.fill(m_ell(cx - 4, cy - 4, cx + 4, cy + 4), "#ffffff")
    if "garland" in ex:
        pts = curve((96, 276), (128, 314), (160, 276), 10)
        p.line(pts, "#2a5a30", 2)
        cols = ["#ff4040", "#ffd23a", "#40a0ff", "#ff60e0"]
        for i, (x, y) in enumerate(pts[1:-1:2]):
            p.part(m_ell(x - 4, y - 2, x + 4, y + 6), cols[i % 4], sv=(1, 1), hl=0.7, line=0.8)
    if "kokoshnik" in ex:
        km = m_poly([(50, 112), (66, 66), (98, 36), (128, 26), (158, 36), (190, 66), (206, 112), (128, 100)])
        p.part(km, o["top"], sv=(6, 6), hl=0.4)
        p.fill(m_poly([(74, 104), (86, 68), (128, 46), (170, 68), (182, 104), (128, 96)]), acc)
        for x, y in ((128, 60), (100, 76), (156, 76)):
            p.part(m_ell(x - 6, y - 6, x + 6, y + 6), "#62a8ee" if acc == "#ffffff" else "#ffffff", sv=(1, 1), hl=0.7, line=1)
    if "tiara" in ex:
        p.part(m_poly([(78, 96), (104, 78), (128, 56), (152, 78), (178, 96), (128, 88)]), acc, sv=(2, 2), hl=0.7, line=1.3)
        p.part(m_ell(122, 70, 134, 82), "#ff6a8a" if acc == "#eabc36" else "#ffdc72", sv=(1, 1), hl=0.7, line=0.8)
    if "halo" in ex:
        hm = new_mask(); ImageDraw.Draw(hm).ellipse(B(82, 28, 174, 52), outline=255, width=int(5 * S))
        p.fill(hm.filter(ImageFilter.GaussianBlur(2.5 * S)), "#fff6b0", 0.8)
        p.fill(hm, "#fff2a0")
    if "scarf" in ex:
        sc = "#b84e3c" if o["top"] != "#e6ecf0" else "#5e7e9e"
        p.part(m_rr(98, 230, 158, 256, 11), sc, sv=(4, 4), hl=0.3)
        p.part(m_poly([(140, 246), (156, 246), (162, 292), (146, 288)]), sc, sv=(3, 3))
    if "wrench" in ex:
        p.part(m_line([(156, 322), (184, 366)], 6), "#aab2ba", sv=(2, 2), hl=0.6, line=1.2)
        p.part(m_ell(176, 356, 196, 378), "#aab2ba", sv=(2, 2), hl=0.6, line=1.2)
        p.fill(m_ell(182, 362, 190, 370), "#3a3a3a")
    if "cup" in ex:
        p.part(m_rr(150, 300, 172, 328, 4), "#ffffff", sv=(3, 3), shade=0.15, line=1.4)
        p.line([(168, 308), (176, 312), (176, 320), (170, 322)], "#c0b0b0", 2)
        for i, x in enumerate((156, 166)):
            p.line(curve((x, 296), (x - 5, 286 - i * 2), (x + 1, 276)), "#ffffff", 2, 0.7)


HEAD_EX = ("cap", "phones", "cat", "visor", "flake", "starpin", "kokoshnik", "tiara", "halo")
HK, BK, NECK, NY = 1.15, 0.82, 250, 290  # голова крупнее, тельце короче; шея уходит на NY


def place(im, k, out):
    """Масштаб вокруг шеи (128, NECK) и перенос шеи на (128, NY)."""
    w, h = im.size
    sc = im.resize((int(w * k), int(h * k)), Image.LANCZOS)
    out.alpha_composite(sc, (int(round(128 - 128 * k)), int(round(NY - NECK * k))))


def draw_body(gid, oi):
    g = GIRLS[gid]
    o = g["o"][oi]
    hb = Pic(); draw_hair_back(hb, g)
    bd = Pic(); extras_back(bd, g, o); body(bd, g, o)
    gb = dict(g); gb["extra"] = [e for e in g["extra"] if e not in HEAD_EX]
    extras_front(bd, gb, o)
    hd = Pic(); head(hd, g); draw_hair_front(hd, g)
    gh = dict(g); gh["extra"] = [e for e in g["extra"] if e in HEAD_EX]
    extras_front(hd, gh, o)
    out = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    place(hb.done(), HK, out)
    place(bd.done(), BK, out)
    place(hd.done(), HK, out)
    return out


def draw_arm(gid, oi):
    g = GIRLS[gid]
    o = g["o"][oi]
    p = Pic(ARM_W, ARM_H)
    sl = o["acc"] if o["kind"] == "armor" else o["top"]
    y = ARM_PY
    p.part(m_line([(ARM_PX, y), (ARM_PX + 54, y)], 16, ARM_W, ARM_H), sl, sv=(0, 5), hl=0.35)
    p.part(m_line([(ARM_PX + 44, y - 7), (ARM_PX + 44, y + 7)], 3, ARM_W, ARM_H), lt(sl, 0.25) if o["kind"] != "cyber" else o["acc"], line=0)
    p.part(m_ell(ARM_PX + 52, y - 10, ARM_PX + 72, y + 10, ARM_W, ARM_H), SKIN, sv=(2, 3), shade=0.15, line=1.3, lcol="#c08a7a")
    p.part(m_line([(ARM_PX + 66, y - 4), (ARM_PX + 84, y - 5)], 5.5, ARM_W, ARM_H), SKIN, sv=(1, 2), shade=0.15, line=1.2, lcol="#c08a7a")
    return p.done()


# ---------------------------------------------------------------- лицо
def eye(p, cx, cy, col, mode, side):
    if mode == "closed":
        p.line(curve((cx - 15, cy + 2), (cx, cy + 10), (cx + 15, cy + 2)), LASH, 3.2)
        return
    if mode == "happy":
        p.line(curve((cx - 15, cy + 8), (cx, cy - 8), (cx + 15, cy + 8)), LASH, 3.6)
        return
    if mode == "tight":
        pts = [(cx - 14, cy - 6), (cx + 4, cy + 3), (cx - 12, cy + 10)] if side < 0 else [(cx + 14, cy - 6), (cx - 4, cy + 3), (cx + 12, cy + 10)]
        p.line(pts, LASH, 3.4)
        return
    hw, hh = (20, 26) if mode == "open" else (20, 27)
    w_, h_ = p.w, p.h
    white = m_ell(cx - hw, cy - hh, cx + hw, cy + hh, w_, h_)
    p.fill(white, "#ffffff")
    p.fill(mul(white, vgrad(cy - hh, cy - hh + 12, 1, 0, w_, h_)), "#d8d0f0", 0.9)  # тень века на белке
    ir = 15 if mode == "open" else 9
    iris = mul(m_ell(cx - ir, cy - hh + 2, cx + ir, cy + hh, w_, h_), white)
    p.fill(iris, dk(col, 0.25))
    p.fill(mul(iris, vgrad(cy - hh, cy + hh, 0, 1, w_, h_)), lt(col, 0.35))
    p.fill(mul(m_ell(cx - ir + 3, cy + 4, cx + ir - 3, cy + hh - 2, w_, h_), iris).filter(ImageFilter.GaussianBlur(1.5 * S)), lt(col, 0.6), 0.8)
    pr = 6 if mode == "open" else 3
    p.fill(mul(m_ell(cx - pr, cy - 4, cx + pr, cy + 8, w_, h_), iris), dk(col, 0.7))
    # блики
    p.fill(m_ell(cx + 1, cy - 15, cx + 11, cy - 4, w_, h_), "#ffffff")
    p.fill(m_ell(cx - 9, cy + 6, cx - 4, cy + 11, w_, h_), "#ffffff", 0.85)
    p.fill(m_ell(cx + 6, cy + 9, cx + 9, cy + 12, w_, h_), "#ffffff", 0.6)
    # ресницы
    lm = Image.new("L", white.size, 0)
    ImageDraw.Draw(lm).arc(B(cx - hw - 2, cy - hh - 1, cx + hw + 2, cy + hh), 200, 340, fill=255, width=int(4.5 * S))
    p.fill(lm, LASH)
    p.line([(cx + side * (hw - 1), cy - 12), (cx + side * (hw + 6), cy - 16)], LASH, 3)
    p.line(curve((cx - 9, cy + hh - 1), (cx, cy + hh + 1), (cx + 9, cy + hh - 1)), "#a8707a", 1.4, 0.8)


def draw_face(gid, emo):
    g = GIRLS[gid]
    p = Pic(FACE_W, FACE_H)

    def X(x):
        return x - FACE_X

    def Y(y):
        return y - FACE_Y

    lx, rx, ey = X(92), X(164), Y(192)
    col = g["eye"]
    bh = dk(g["hair"], 0.35)
    # румянец мягкий
    bl = 0.55 if emo in ("joy", "shout", "cry", "norm", "blink") else 0.3
    for bx in (lx - 6, rx + 6):
        p.fill(m_ell(bx - 16, ey + 20, bx + 16, ey + 34, FACE_W, FACE_H).filter(ImageFilter.GaussianBlur(3 * S)), "#ff8c9c", bl)
        for i in range(3):
            p.line([(bx - 8 + i * 6, ey + 24), (bx - 11 + i * 6, ey + 30)], "#ff8c9c", 1.2, bl)
    mx, my = X(128), Y(230)
    def brows(dy_in, dy_out):
        pass  # брови прячутся под чёлкой
    if emo == "norm":
        eye(p, lx, ey, col, "open", -1); eye(p, rx, ey, col, "open", 1)
        brows(-2, 2)
        p.line(curve((mx - 7, my - 2), (mx, my + 4), (mx + 7, my - 2)), "#8a3a4a", 2.2)
    elif emo == "blink":
        eye(p, lx, ey, col, "closed", -1); eye(p, rx, ey, col, "closed", 1)
        brows(0, 2)
        p.line(curve((mx - 7, my - 2), (mx, my + 4), (mx + 7, my - 2)), "#8a3a4a", 2.2)
    elif emo == "shout":
        eye(p, lx, ey, col, "open", -1); eye(p, rx, ey, col, "open", 1)
        brows(8, -2)
        p.part(m_ell(mx - 12, my - 8, mx + 12, my + 14, FACE_W, FACE_H), "#8a2234", sv=(0, 0), shade=0, line=1.6, lcol="#5a1a24")
        p.fill(m_ell(mx - 7, my + 4, mx + 7, my + 12, FACE_W, FACE_H), "#ff8494")
    elif emo == "joy":
        eye(p, lx, ey, col, "happy", -1); eye(p, rx, ey, col, "happy", 1)
        brows(-4, 0)
        m = Image.new("L", (FACE_W * S, FACE_H * S), 0)
        ImageDraw.Draw(m).chord(B(mx - 13, my - 13, mx + 13, my + 13), 0, 180, fill=255)
        p.part(m, "#8a2234", sv=(0, 0), shade=0, line=1.6, lcol="#5a1a24")
        p.fill(mul(m_ell(mx - 8, my + 2, mx + 8, my + 14, FACE_W, FACE_H), m), "#ff8494")
        for x, y in ((lx - 26, ey - 22), (rx + 26, ey - 22)):
            p.fill(m_poly(star_pts(x, y, 7, 3, 4), FACE_W, FACE_H), "#ffe060")
    elif emo == "panic":
        eye(p, lx, ey, col, "wide", -1); eye(p, rx, ey, col, "wide", 1)
        brows(-10, 2)
        p.line([(mx - 12, my + 2), (mx - 6, my - 3), (mx, my + 3), (mx + 6, my - 3), (mx + 12, my + 2)], "#8a3a4a", 2.4)
        sx, sy = X(206), Y(150)
        p.part(m_poly([(sx, sy - 14), (sx + 8, sy + 2), (sx, sy + 9), (sx - 8, sy + 2)], FACE_W, FACE_H), "#b8dcff", sv=(2, 2), hl=0.8, line=1.2, lcol="#5a8ad0")
    elif emo == "worry":
        eye(p, lx, ey, col, "open", -1); eye(p, rx, ey, col, "open", 1)
        brows(-8, 4)
        p.line(curve((mx - 8, my + 4), (mx, my - 2), (mx + 8, my + 4)), "#8a3a4a", 2.2)
    elif emo == "cry":
        eye(p, lx, ey, col, "tight", -1); eye(p, rx, ey, col, "tight", 1)
        brows(-8, 4)
        m = Image.new("L", (FACE_W * S, FACE_H * S), 0)
        ImageDraw.Draw(m).chord(B(mx - 12, my - 4, mx + 12, my + 16), 180, 360, fill=255)
        p.part(m, "#8a2234", sv=(0, 0), shade=0, line=1.6, lcol="#5a1a24")
        for x in (lx - 2, rx + 2):
            p.line([(x, ey + 10), (x - 2, ey + 28), (x + 1, ey + 44)], (130, 196, 255, 230), 6)
            p.line([(x - 1, ey + 12), (x - 2, ey + 26)], "#ffffff", 1.6, 0.8)
    return p.done()


# ---------------------------------------------------------------- сборка
def tface(im):
    return im.resize((int(FACE_W * HK), int(FACE_H * HK)), Image.LANCZOS)


FX2, FY2 = int(round(128 + (FACE_X - 128) * HK)), int(round(NY + (FACE_Y - NECK) * HK))
SH2 = (128 + (SHOULDER[0] - 128) * BK, NY + (SHOULDER[1] - NECK) * BK)


def tarm(im):
    return im.resize((int(ARM_W * BK), int(ARM_H * BK)), Image.LANCZOS)


def full_preview(body_im, face_im, arm_im, ang=-140):
    full = Image.new("RGBA", (W + 40, H), (0, 0, 0, 0))
    full.alpha_composite(body_im, (40, 0))
    full.alpha_composite(face_im, (FX2 + 40, FY2))
    big = Image.new("RGBA", (300, 300), (0, 0, 0, 0))
    big.alpha_composite(arm_im, (150 - int(ARM_PX * BK), 150 - int(ARM_PY * BK)))
    big = big.rotate(-ang, center=(150, 150), resample=Image.BICUBIC)
    full.alpha_composite(big, (int(SH2[0]) + 40 - 150, int(SH2[1]) - 150))
    return full


def portrait(body_im, face_im):
    full = body_im.copy()
    full.alpha_composite(face_im, (FX2, FY2))
    crop = full.crop((10, 26, 246, 262)).resize((200, 200), Image.LANCZOS)
    return crop


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else "assets/ops"
    os.makedirs(out, exist_ok=True)
    only = os.environ.get("OPS_ONLY")
    ids = only.split(",") if only else list(GIRLS)
    sheet = []
    for gid in ids:
        faces = {e: tface(draw_face(gid, e)) for e in EMOS}
        for e, im in faces.items():
            im.save(os.path.join(out, f"{gid}_face_{e}.png"), optimize=True)
        for oi in range(2):
            b = draw_body(gid, oi)
            a = tarm(draw_arm(gid, oi))
            b.save(os.path.join(out, f"{gid}_{oi}_body.png"), optimize=True)
            a.save(os.path.join(out, f"{gid}_{oi}_arm.png"), optimize=True)
            portrait(b, faces["norm"]).save(os.path.join(out, f"{gid}_{oi}_por.png"), optimize=True)
            sheet.append(full_preview(b, faces["norm"] if oi == 0 else faces["joy"], a))
        print(gid, flush=True)
    sw = W + 40
    big = Image.new("RGBA", (len(ids) * sw, 2 * H), (52, 58, 66, 255))
    for i, im in enumerate(sheet):
        gi, oi = divmod(i, 2)
        big.alpha_composite(im, (gi * sw, oi * H))
    big.convert("RGB").save("/tmp/ops_sheet.png")
    em = Image.new("RGBA", (FACE_W * 7, FACE_H), (240, 232, 226, 255))
    for i, e in enumerate(EMOS):
        em.alpha_composite(draw_face(ids[0], e), (i * FACE_W, 0))
    em.convert("RGB").save("/tmp/ops_faces.png")


if __name__ == "__main__":
    main()
