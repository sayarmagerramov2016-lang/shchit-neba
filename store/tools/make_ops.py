#!/usr/bin/env python3
"""Рисует операторов (аниме-девчонок) для «Щит неба 3D».

Запуск: python3 store/tools/make_ops.py assets/ops
На каждую девчонку и каждый наряд (0 — обычный, 1 — второй, с 5-го уровня дружбы):
  <id>_<o>_body.png  — всё тело без лица и без правой руки (256×512)
  <id>_<o>_arm.png   — указывающая рука, ось у плеча (ARM_PX, ARM_PY)
  <id>_<o>_por.png   — портрет для карточек, кейсов и кнопки умения (200×200)
и на каждую девчонку 7 лиц: <id>_face_<эмоция>.png — кусок FACE_W×FACE_H, кладётся в (FACE_X, FACE_Y).
Эмоции: norm, blink, shout, joy, panic, worry, cry.
Координаты в описании — в итоговых пикселях, рисуется с запасом S и уменьшается (сглаживание).
"""
import math
import os
import sys

from PIL import Image, ImageDraw, ImageFilter

S = 4
W, H = 256, 512
FACE_X, FACE_Y, FACE_W, FACE_H = 56, 118, 144, 104
ARM_W, ARM_H, ARM_PX, ARM_PY = 128, 48, 16, 24
SHOULDER = (94, 232)  # где у тела ось указывающей руки
EMOS = ["norm", "blink", "shout", "joy", "panic", "worry", "cry"]

SKIN, SKIN_D = "#ffe1cf", "#e9b49c"
LINE = "#3b2533"


def hx(c, a=255):
    c = c.lstrip("#")
    return (int(c[0:2], 16), int(c[2:4], 16), int(c[4:6], 16), a)


def mix(a, b, k):
    a, b = hx(a), hx(b)
    return "#%02x%02x%02x" % tuple(int(a[i] + (b[i] - a[i]) * k) for i in range(3))


def dk(c, k=0.3):
    return mix(c, "#1a1020", k)


def lt(c, k=0.3):
    return mix(c, "#ffffff", k)


def P(pts):
    return [(x * S, y * S) for x, y in pts]


def B(x0, y0, x1, y1):
    return [x0 * S, y0 * S, x1 * S, y1 * S]


class Pen:
    def __init__(self, im):
        self.im = im
        self.d = ImageDraw.Draw(im)

    def poly(self, pts, fill, line=LINE, w=2.2):
        self.d.polygon(P(pts), fill=hx(fill) if isinstance(fill, str) else fill,
                       outline=hx(line) if line else None, width=int(w * S) if line else 0)

    def ell(self, x0, y0, x1, y1, fill, line=LINE, w=2.2):
        self.d.ellipse(B(x0, y0, x1, y1), fill=hx(fill) if isinstance(fill, str) else fill,
                       outline=hx(line) if line else None, width=int(w * S) if line else 0)

    def rr(self, x0, y0, x1, y1, r, fill, line=LINE, w=2.2):
        self.d.rounded_rectangle(B(x0, y0, x1, y1), radius=r * S, fill=hx(fill) if isinstance(fill, str) else fill,
                                 outline=hx(line) if line else None, width=int(w * S) if line else 0)

    def ln(self, pts, col, w):
        self.d.line(P(pts), fill=hx(col) if isinstance(col, str) else col, width=int(w * S), joint="curve")
        r = w * S / 2
        for x, y in (pts[0], pts[-1]):
            self.d.ellipse([x * S - r, y * S - r, x * S + r, y * S + r], fill=hx(col) if isinstance(col, str) else col)

    def arc(self, x0, y0, x1, y1, a0, a1, col, w):
        self.d.arc(B(x0, y0, x1, y1), a0, a1, fill=hx(col), width=int(w * S))

    def chord(self, x0, y0, x1, y1, a0, a1, fill, line=LINE, w=2):
        self.d.chord(B(x0, y0, x1, y1), a0, a1, fill=hx(fill), outline=hx(line) if line else None, width=int(w * S) if line else 0)


def canvas(w, h):
    return Image.new("RGBA", (w * S, h * S), (0, 0, 0, 0))


def down(im, w, h):
    return im.resize((w, h), Image.LANCZOS)


def curve(p0, p1, p2, n=12):
    out = []
    for i in range(n + 1):
        t = i / n
        out.append(((1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t * t * p2[0],
                    (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t * t * p2[1]))
    return out


def star_pts(cx, cy, r, r2, n=5, rot=-90):
    pts = []
    for i in range(n * 2):
        a = math.radians(rot + i * 180 / n)
        rr_ = r if i % 2 == 0 else r2
        pts.append((cx + math.cos(a) * rr_, cy + math.sin(a) * rr_))
    return pts


# ---------------------------------------------------------------- девчонки
# hair, eye, style, top, top2 (акцент), bottom, legs, shoes, extra
GIRLS = {
    "liza": dict(hair="#7a4a2c", eye="#3f8f5a", style="pony", extra=["wrench", "cap"],
                 o=[dict(top="#6b7a4a", acc="#f08a2a", bot="#56633c", legs="#3a3a3a", shoes="#4a3424", kind="overall"),
                    dict(top="#1f2d55", acc="#e8c25a", bot="#18244a", legs="#2a2a3a", shoes="#1a1a24", kind="overall")]),
    "katya": dict(hair="#f2cf6e", eye="#4f8ad8", style="long", extra=["cup"],
                  o=[dict(top="#e8d2a6", acc="#c0563c", bot="#4e7a4a", legs="#f4efe8", shoes="#7a4a2c", kind="sweater"),
                     dict(top="#f4b6c8", acc="#ffffff", bot="#c65a7a", legs="#ffffff", shoes="#c65a7a", kind="sweater")]),
    "sonya": dict(hair="#4a4e58", eye="#7a6ad0", style="bob", extra=["scarf"],
                  o=[dict(top="#5d6b42", acc="#8a9a5a", bot="#4b5636", legs="#38402a", shoes="#2c2c26", kind="camo"),
                     dict(top="#dfe6ea", acc="#9aa8b4", bot="#c4ced6", legs="#a8b4be", shoes="#5a6670", kind="camo")]),
    "anya": dict(hair="#8fd0f0", eye="#2f6fd8", style="twin", extra=["phones"],
                 o=[dict(top="#24345e", acc="#59e0ff", bot="#1c2a4c", legs="#1a1a2a", shoes="#121220", kind="uniform"),
                    dict(top="#f2f6fa", acc="#2fb8e8", bot="#5aa8d8", legs="#e8eef4", shoes="#2f6fa8", kind="uniform")]),
    "masha": dict(hair="#f0873a", eye="#e0b030", style="short", extra=["cat", "bell"],
                  o=[dict(top="#d8403a", acc="#ffffff", bot="#f4f0ea", legs="#ffffff", shoes="#9a2a24", kind="sailor"),
                     dict(top="#26222a", acc="#e8c25a", bot="#3a343e", legs="#26222a", shoes="#e8c25a", kind="sailor")]),
    "rin": dict(hair="#ff4fb8", eye="#40f0ff", style="side", extra=["visor"],
                o=[dict(top="#1a1622", acc="#ff4fd8", bot="#24202e", legs="#121018", shoes="#ff4fd8", kind="cyber"),
                   dict(top="#101a22", acc="#3ff0ff", bot="#16222e", legs="#0c1218", shoes="#3ff0ff", kind="cyber")]),
    "yuki": dict(hair="#f0f6ff", eye="#5ab0ff", style="long", extra=["flake"],
                 o=[dict(top="#bfe4ff", acc="#ffffff", bot="#8fc8f4", legs="#eaf6ff", shoes="#5a9ad8", kind="dress"),
                    dict(top="#24407a", acc="#bfe4ff", bot="#1c3264", legs="#d8e8f8", shoes="#bfe4ff", kind="dress")]),
    "elka": dict(hair="#3a9a4a", eye="#e04040", style="buns", extra=["garland", "starpin"],
                 o=[dict(top="#2f8a40", acc="#ffd23a", bot="#24703a", legs="#e83a3a", shoes="#8a2020", kind="dress"),
                    dict(top="#d83a3a", acc="#ffffff", bot="#b82a2a", legs="#ffffff", shoes="#2f8a40", kind="dress")]),
    "akira": dict(hair="#22202a", eye="#d03030", style="highpony", extra=["katana"],
                  o=[dict(top="#f6f2ea", acc="#c8283a", bot="#b0202e", legs="#26222a", shoes="#2a2026", kind="hakama"),
                     dict(top="#2a2230", acc="#9a5ae0", bot="#1a1622", legs="#141018", shoes="#9a5ae0", kind="hakama")]),
    "sneg": dict(hair="#e6eef8", eye="#3a8ae0", style="braid", extra=["kokoshnik"],
                 o=[dict(top="#5aa0e8", acc="#ffffff", bot="#4a8ad8", legs="#ffffff", shoes="#ffffff", kind="coat"),
                    dict(top="#c8d0dc", acc="#ffb8d0", bot="#aab4c4", legs="#ffffff", shoes="#ffb8d0", kind="coat")]),
    "valk": dict(hair="#ffd86a", eye="#4a90ff", style="long", extra=["wings", "halo", "tiara"],
                 o=[dict(top="#f6f2e6", acc="#e8b830", bot="#ece4d0", legs="#f6f2e6", shoes="#e8b830", kind="armor"),
                    dict(top="#2a2238", acc="#a070ff", bot="#1e1828", legs="#2a2238", shoes="#a070ff", kind="armor")]),
}


# ---------------------------------------------------------------- волосы
def hair_back(p, g):
    h, sty = g["hair"], g["style"]
    hd = dk(h, 0.25)
    if sty in ("long", "braid"):
        p.poly([(52, 120), (204, 120), (214, 250), (200, 336), (128, 352), (56, 336), (42, 250)], hd)
    elif sty == "twin":
        for sx in (1, -1):
            cx = 128 + sx * 98
            p.poly([(cx - 20 * sx, 100), (cx + 14 * sx, 120), (cx + 22 * sx, 220), (cx + 10 * sx, 330),
                    (cx - 6 * sx, 360), (cx - 18 * sx, 300), (cx - 22 * sx, 200)], h)
            p.ell(cx - 12, 96, cx + 12, 120, g.get("tie", "#ff5a7a"))
    elif sty == "pony":
        p.poly([(150, 90), (214, 120), (232, 220), (214, 300), (196, 250), (186, 160)], hd)
    elif sty == "highpony":
        p.poly([(160, 40), (220, 70), (240, 180), (236, 300), (214, 360), (200, 260), (190, 120)], hd)
        p.poly([(52, 120), (204, 120), (210, 240), (128, 250), (46, 240)], hd)
    elif sty == "side":
        p.poly([(170, 100), (226, 140), (238, 250), (220, 330), (204, 240), (188, 150)], hd)
        p.ell(196, 104, 222, 130, "#40f0ff" if "visor" in g["extra"] else "#ff5a7a")
    elif sty == "bob":
        p.poly([(44, 120), (212, 120), (222, 210), (196, 232), (60, 232), (34, 210)], hd)
    elif sty == "short":
        p.poly([(48, 120), (208, 120), (214, 196), (42, 196)], hd)
    elif sty == "buns":
        p.poly([(52, 120), (204, 120), (210, 230), (46, 230)], hd)
    if sty == "braid":
        for i in range(6):
            y = 230 + i * 26
            p.ell(196 - i * 2, y, 226 - i * 2, y + 30, h)
        p.ell(198, 380, 222, 396, "#5aa0e8")


def hair_front(p, g):
    h = g["hair"]
    hl = lt(h, 0.35)
    sty = g["style"]
    # макушка и чёлка — один многоугольник (без прямой линии через лицо)
    xs = [216, 194, 180, 160, 146, 128, 110, 96, 76, 62, 40]
    ys = [126, 140, 114, 142, 116, 136, 116, 142, 114, 140, 126]
    if sty == "side":
        ys = [116, 124, 106, 130, 110, 140, 118, 160, 124, 166, 126]
    pts = []
    for i in range(25):
        a = math.radians(180 + i * 7.5)
        pts.append((128 + math.cos(a) * 90, 128 + math.sin(a) * 88))
    pts += list(zip(xs, ys))
    p.poly(pts, h, w=2.4)
    # боковые пряди
    if sty not in ("short",):
        p.poly([(40, 112), (58, 120), (60, 200), (48, 226), (36, 196)], h, w=2.2)
        p.poly([(216, 112), (198, 120), (196, 200), (208, 226), (220, 196)], h, w=2.2)
    else:
        p.poly([(40, 112), (58, 120), (58, 176), (42, 186)], h, w=2.2)
        p.poly([(216, 112), (198, 120), (198, 176), (214, 186)], h, w=2.2)
    # блик
    p.arc(70, 66, 186, 140, 200, 250, hl, 4)
    p.arc(74, 70, 182, 136, 290, 330, hl, 3)
    if sty == "buns":
        for cx in (62, 194):
            p.ell(cx - 26, 30, cx + 26, 82, h, w=2.4)
            p.arc(cx - 16, 40, cx + 16, 72, 200, 300, hl, 3)
    if sty == "highpony":
        p.ell(148, 36, 176, 58, "#c8283a")


# ---------------------------------------------------------------- тело
def body(p, g, o):
    top, acc, bot, legs, shoes, kind = o["top"], o["acc"], o["bot"], o["legs"], o["shoes"], o["kind"]
    # ноги
    p.rr(104, 340, 124, 470, 8, legs)
    p.rr(132, 340, 152, 470, 8, legs)
    if kind in ("dress", "sweater", "sailor", "coat") and legs.lower() not in ("#ffffff", "#f4efe8"):
        pass
    # обувь
    p.rr(98, 458, 126, 490, 10, shoes)
    p.rr(130, 458, 158, 490, 10, shoes)
    p.ln([(104, 482), (122, 482)], lt(shoes, 0.3), 2)
    p.ln([(134, 482), (152, 482)], lt(shoes, 0.3), 2)
    # низ
    if kind in ("overall", "uniform", "camo", "cyber"):
        p.poly([(92, 298), (164, 298), (168, 366), (130, 368), (128, 344), (126, 368), (88, 366)], bot)
    elif kind == "hakama":
        p.poly([(90, 296), (166, 296), (186, 420), (128, 424), (70, 420)], bot)
        p.ln([(128, 312), (128, 420)], dk(bot, 0.3), 2)
        p.ln([(108, 312), (100, 418)], dk(bot, 0.3), 1.6)
        p.ln([(148, 312), (156, 418)], dk(bot, 0.3), 1.6)
    elif kind == "coat":
        p.poly([(88, 290), (168, 290), (190, 412), (66, 412)], top)
        p.rr(62, 400, 194, 420, 9, "#ffffff")
    else:
        p.poly([(90, 296), (166, 296), (186, 370), (70, 370)], bot)
        for x in range(80, 180, 18):
            p.ln([(x + 8, 302), (x + 4, 366)], dk(bot, 0.2), 1.4)
    # торс
    p.rr(88, 218, 168, 312, 18, top)
    if kind == "overall":
        p.rr(102, 250, 154, 312, 6, bot)
        p.ln([(104, 226), (106, 252)], bot, 6)
        p.ln([(152, 226), (150, 252)], bot, 6)
        p.ell(103, 248, 111, 256, acc, w=1.4)
        p.ell(145, 248, 153, 256, acc, w=1.4)
        p.rr(116, 268, 140, 286, 3, dk(bot, 0.15), w=1.6)
    elif kind == "sweater":
        for y in (250, 268, 286):
            p.ln([(94, y), (162, y)], dk(top, 0.12), 2)
        p.ln([(100, 236), (156, 236)], acc, 4)
    elif kind == "camo":
        for (x, y, r) in ((104, 240, 9), (146, 262, 11), (118, 288, 8), (154, 234, 7), (98, 282, 7)):
            p.ell(x - r, y - r * 0.7, x + r, y + r * 0.7, o["acc"], line=None)
        p.ln([(128, 222), (128, 310)], dk(top, 0.3), 1.6)
    elif kind == "uniform":
        p.poly([(110, 220), (128, 250), (146, 220)], "#ffffff", w=1.6)
        p.poly([(124, 238), (132, 238), (134, 270), (128, 278), (122, 270)], acc, w=1.4)
        p.rr(96, 252, 112, 262, 2, acc, w=1.2)
        p.ln([(92, 296), (164, 296)], dk(top, 0.3), 4)
    elif kind == "sailor":
        p.poly([(94, 222), (162, 222), (150, 250), (128, 270), (106, 250)], acc, w=1.8)
        p.poly([(118, 252), (138, 252), (146, 274), (128, 266), (110, 274)], "#e83a3a" if top != "#d8403a" else "#2a4a9a", w=1.4)
    elif kind == "cyber":
        p.ln([(96, 230), (110, 300)], acc, 2.4)
        p.ln([(160, 230), (146, 300)], acc, 2.4)
        p.ln([(110, 262), (146, 262)], acc, 2)
        p.ell(122, 278, 134, 290, acc, w=1.4)
        p.rr(84, 214, 172, 232, 8, dk(top, -0.2) if False else lt(top, 0.1))
    elif kind == "dress":
        p.poly([(110, 220), (128, 238), (146, 220)], acc, w=1.4)
        p.ln([(92, 296), (164, 296)], acc, 4)
        p.ell(122, 252, 134, 264, acc, w=1.2)
    elif kind == "hakama":
        p.poly([(104, 218), (128, 270), (152, 218)], acc, w=1.6)
        p.poly([(110, 218), (128, 256), (146, 218)], top, w=1.2)
        p.rr(90, 290, 166, 304, 3, acc)
    elif kind == "coat":
        p.rr(84, 214, 172, 234, 10, "#ffffff")
        p.ln([(128, 234), (128, 410)], "#ffffff", 7)
        for y in (262, 300, 340):
            p.ell(121, y - 5, 135, y + 9, o["acc"] if o["acc"] != "#ffffff" else "#e8c25a", w=1.2)
    elif kind == "armor":
        p.poly([(94, 222), (162, 222), (156, 270), (128, 286), (100, 270)], acc, w=1.8)
        p.poly([(102, 228), (154, 228), (150, 264), (128, 276), (106, 264)], top, w=1.4)
        p.poly(star_pts(128, 250, 9, 4), acc, w=1)
        p.rr(80, 214, 106, 236, 10, acc)
        p.rr(150, 214, 176, 236, 10, acc)
        p.poly([(90, 296), (166, 296), (186, 380), (70, 380)], bot)
        p.ln([(90, 300), (166, 300)], acc, 5)
    # шея
    p.rr(118, 196, 138, 224, 6, SKIN, w=1.8)
    p.ln([(120, 216), (136, 216)], SKIN_D, 2)
    # левая рука (с нашей стороны справа), висит
    sl = top if kind not in ("overall",) else top
    p.ln([(166, 232), (180, 278), (182, 316)], LINE, 22)
    p.ln([(166, 232), (180, 278), (182, 316)], sl, 18)
    p.ell(172, 308, 194, 330, SKIN, w=2)


def head(p, g):
    # уши
    p.ell(42, 140, 62, 176, SKIN, w=2)
    p.ell(194, 140, 214, 176, SKIN, w=2)
    # лицо — целый овал, верх закроют волосы
    p.ell(46, 66, 210, 226, SKIN, w=2.4)


def extras_back(p, g, o):
    ex = g["extra"]
    if "wings" in ex:
        wc = "#ffffff" if o["top"] != "#2a2238" else "#3a2a50"
        for sx in (1, -1):
            pts = [(128 + sx * 30, 236)]
            for i in range(6):
                a = math.radians(-60 + i * 22)
                r = 116 - i * 6
                pts.append((128 + sx * (30 + math.cos(a) * r), 236 + math.sin(a) * r))
            pts.append((128 + sx * 40, 300))
            p.poly(pts, wc, line=dk(o["acc"], 0.2), w=2)
            for i in range(4):
                a = math.radians(-40 + i * 24)
                p.ln([(128 + sx * 44, 250), (128 + sx * (44 + math.cos(a) * 70), 250 + math.sin(a) * 70)], lt(o["acc"], 0.4), 2)
    if "katana" in ex:
        p.ln([(60, 360), (214, 150)], LINE, 10)
        p.ln([(60, 360), (214, 150)], "#3a2a3a", 7)
        p.ln([(176, 202), (214, 150)], "#d8dde4", 6)
        p.ln([(168, 194), (184, 210)], "#e8c25a", 6)


def extras_front(p, g, o):
    ex = g["extra"]
    acc = o["acc"]
    if "cap" in ex:
        p.chord(52, 34, 204, 150, 180, 360, o["top"], w=2.4)
        p.poly([(52, 92), (204, 92), (226, 104), (40, 104)], dk(o["top"], 0.15), w=2)
        p.ell(118, 54, 138, 74, acc, w=1.4)
    if "phones" in ex:
        p.arc(36, 40, 220, 220, 190, 350, "#2a2a36", 9)
        p.rr(30, 128, 52, 176, 8, "#2a2a36")
        p.rr(204, 128, 226, 176, 8, "#2a2a36")
        p.ell(34, 140, 48, 164, acc, line=None)
        p.ln([(44, 170), (60, 196), (86, 200)], "#2a2a36", 3)
        p.ell(82, 194, 94, 206, acc, w=1.2)
    if "cat" in ex:
        for sx in (1, -1):
            cx = 128 + sx * 58
            p.poly([(cx - 26 * sx, 74), (cx + 10 * sx, 18), (cx + 26 * sx, 80)], g["hair"], w=2.4)
            p.poly([(cx - 12 * sx, 70), (cx + 8 * sx, 34), (cx + 16 * sx, 74)], "#ffc0c8", line=None)
    if "bell" in ex:
        p.ell(120, 218, 136, 234, "#e8c25a", w=1.6)
        p.ln([(122, 228), (134, 228)], LINE, 1.2)
    if "visor" in ex:
        p.rr(54, 102, 202, 116, 6, acc, w=1.6)
        p.ln([(62, 108), (194, 108)], "#ffffff", 1.4)
    if "flake" in ex or "starpin" in ex:
        cx, cy = 184, 104
        if "starpin" in ex:
            p.poly(star_pts(cx, cy - 4, 15, 6), "#ffd23a", w=1.6)
        else:
            for i in range(6):
                a = math.radians(i * 60)
                p.ln([(cx, cy), (cx + math.cos(a) * 15, cy + math.sin(a) * 15)], "#5ab0ff", 3)
            p.ell(cx - 4, cy - 4, cx + 4, cy + 4, "#ffffff", line=None)
    if "garland" in ex:
        pts = curve((90, 244), (128, 284), (166, 244), 10)
        p.ln(pts, "#2a5a30", 2)
        cols = ["#ff4040", "#ffd23a", "#40a0ff", "#ff60e0"]
        for i, (x, y) in enumerate(pts[1:-1:2]):
            p.ell(x - 4, y - 2, x + 4, y + 6, cols[i % 4], w=1)
    if "kokoshnik" in ex:
        p.poly([(54, 96), (70, 50), (100, 22), (128, 12), (156, 22), (186, 50), (202, 96), (128, 84)], o["top"], w=2.4)
        p.poly([(76, 88), (88, 52), (128, 30), (168, 52), (180, 88), (128, 80)], o["acc"], line=None)
        for x, y in ((128, 44), (100, 60), (156, 60)):
            p.ell(x - 6, y - 6, x + 6, y + 6, "#5aa0e8" if o["acc"] == "#ffffff" else "#ffffff", w=1.2)
        p.poly(star_pts(128, 52, 10, 4, 6), "#ffffff", line=None)
    if "tiara" in ex:
        p.poly([(80, 76), (104, 60), (128, 40), (152, 60), (176, 76), (128, 70)], o["acc"], w=1.8)
        p.ell(122, 52, 134, 64, "#ff6a8a" if o["acc"] == "#e8b830" else "#ffd86a", w=1)
    if "halo" in ex:
        p.d.ellipse(B(84, 14, 172, 36), outline=hx("#fff2a0"), width=int(5 * S))
    if "scarf" in ex:
        p.rr(96, 200, 160, 224, 10, "#b04a3a" if o["top"] != "#dfe6ea" else "#5a7a9a")
        p.poly([(140, 214), (156, 214), (162, 262), (146, 258)], "#b04a3a" if o["top"] != "#dfe6ea" else "#5a7a9a")
    if "wrench" in ex:
        p.ln([(184, 316), (204, 362)], LINE, 9)
        p.ln([(184, 316), (204, 362)], "#a8b0b8", 6)
        p.ell(196, 352, 216, 376, "#a8b0b8", w=2)
        p.ell(202, 360, 210, 368, "#3a3a3a", line=None)
    if "cup" in ex:
        p.rr(178, 300, 202, 330, 4, "#ffffff", w=2)
        p.arc(196, 306, 210, 324, 270, 90, LINE, 2)
        for i, x in enumerate((184, 194)):
            p.arc(x - 4, 280 - i * 3, x + 6, 298, 100, 260, "#ffffff", 2)


def draw_body(gid, oi):
    g = GIRLS[gid]
    o = g["o"][oi]
    im = canvas(W, H)
    p = Pen(im)
    extras_back(p, g, o)
    hair_back(p, g)
    body(p, g, o)
    head(p, g)
    hair_front(p, g)
    extras_front(p, g, o)
    return down(im, W, H)


def draw_arm(gid, oi):
    g = GIRLS[gid]
    o = g["o"][oi]
    im = canvas(ARM_W, ARM_H)
    p = Pen(im)
    sl = o["top"]
    if o["kind"] == "armor":
        sl = o["acc"]
    y = ARM_PY
    p.ln([(ARM_PX, y), (ARM_PX + 78, y)], LINE, 22)
    p.ln([(ARM_PX, y), (ARM_PX + 78, y)], sl, 18)
    p.ln([(ARM_PX + 62, y - 9), (ARM_PX + 62, y + 9)], lt(sl, 0.25) if o["kind"] != "cyber" else o["acc"], 4)
    # кисть с указательным пальцем
    p.ell(ARM_PX + 76, y - 11, ARM_PX + 98, y + 11, SKIN, w=2)
    p.ln([(ARM_PX + 92, y - 4), (ARM_PX + 110, y - 5)], LINE, 8.5)
    p.ln([(ARM_PX + 92, y - 4), (ARM_PX + 110, y - 5)], SKIN, 5.5)
    return down(im, ARM_W, ARM_H)


def eye(p, cx, cy, col, mode, side):
    # mode: open, wide, closed, happy, tight
    if mode == "closed":
        p.arc(cx - 15, cy - 6, cx + 15, cy + 14, 200, 340, LINE, 3.2)
        return
    if mode == "happy":
        p.arc(cx - 15, cy - 4, cx + 15, cy + 22, 200, 340, LINE, 3.6)
        return
    if mode == "tight":
        p.ln([(cx - 14, cy - 4), (cx + 2, cy + 4), (cx - 12, cy + 10)] if side < 0 else
             [(cx + 14, cy - 4), (cx - 2, cy + 4), (cx + 12, cy + 10)], LINE, 3.4)
        return
    hw, hh = (16, 21) if mode == "open" else (17, 23)
    p.ell(cx - hw, cy - hh, cx + hw, cy + hh, "#ffffff", w=2)
    ir = 12 if mode == "open" else 8
    p.ell(cx - ir, cy - hh + 4, cx + ir, cy + hh - 1, col, line=None)
    p.ell(cx - ir + 2, cy - hh + 4, cx + ir - 2, cy + 2, dk(col, 0.35), line=None)
    pr = 5 if mode == "open" else 3
    p.ell(cx - pr, cy - 2, cx + pr, cy + 8, "#1a1020", line=None)
    p.ell(cx + 2, cy - 12, cx + 9, cy - 5, "#ffffff", line=None)
    p.ell(cx - 7, cy + 7, cx - 3, cy + 11, (255, 255, 255, 200), line=None)
    # верхнее веко
    p.arc(cx - hw - 2, cy - hh - 1, cx + hw + 2, cy + hh, 195, 345, LINE, 4.2)
    p.ln([(cx + side * (hw + 1), cy - 8), (cx + side * (hw + 6), cy - 13)], LINE, 3)


def draw_face(gid, emo):
    g = GIRLS[gid]
    im = canvas(FACE_W, FACE_H)
    p = Pen(im)
    ox, oy = FACE_X, FACE_Y

    def X(x):
        return x - ox

    def Y(y):
        return y - oy

    lx, rx, ey = X(96), X(160), Y(160)
    col = g["eye"]
    # румянец
    blush = (255, 120, 140, 110 if emo in ("joy", "shout", "cry", "norm", "blink") else 60)
    p.ell(lx - 20, ey + 18, lx + 4, ey + 30, blush, line=None)
    p.ell(rx - 4, ey + 18, rx + 20, ey + 30, blush, line=None)
    mx, my = X(128), Y(196)
    if emo == "norm":
        eye(p, lx, ey, col, "open", -1); eye(p, rx, ey, col, "open", 1)
        p.arc(lx - 14, ey - 34, lx + 14, ey - 18, 200, 330, dk(g["hair"], 0.3), 2.6)
        p.arc(rx - 14, ey - 34, rx + 14, ey - 18, 210, 340, dk(g["hair"], 0.3), 2.6)
        p.arc(mx - 9, my - 8, mx + 9, my + 4, 20, 160, LINE, 2.4)
    elif emo == "blink":
        eye(p, lx, ey, col, "closed", -1); eye(p, rx, ey, col, "closed", 1)
        p.arc(lx - 14, ey - 32, lx + 14, ey - 16, 200, 330, dk(g["hair"], 0.3), 2.6)
        p.arc(rx - 14, ey - 32, rx + 14, ey - 16, 210, 340, dk(g["hair"], 0.3), 2.6)
        p.arc(mx - 9, my - 8, mx + 9, my + 4, 20, 160, LINE, 2.4)
    elif emo == "shout":
        eye(p, lx, ey, col, "open", -1); eye(p, rx, ey, col, "open", 1)
        p.ln([(lx - 14, ey - 30), (lx + 12, ey - 22)], dk(g["hair"], 0.35), 3.4)
        p.ln([(rx + 14, ey - 30), (rx - 12, ey - 22)], dk(g["hair"], 0.35), 3.4)
        p.ell(mx - 12, my - 9, mx + 12, my + 14, "#7a1a2a", w=2.2)
        p.ell(mx - 7, my + 4, mx + 7, my + 12, "#ff7a8a", line=None)
    elif emo == "joy":
        eye(p, lx, ey, col, "happy", -1); eye(p, rx, ey, col, "happy", 1)
        p.chord(mx - 14, my - 14, mx + 14, my + 14, 0, 180, "#7a1a2a", w=2.2)
        p.chord(mx - 8, my + 2, mx + 8, my + 13, 0, 180, "#ff7a8a", line=None)
        for x, y in ((lx - 26, ey - 22), (rx + 26, ey - 22)):
            p.poly(star_pts(x, y, 7, 3, 4), "#ffe060", line=None)
    elif emo == "panic":
        eye(p, lx, ey, col, "wide", -1); eye(p, rx, ey, col, "wide", 1)
        p.ln([(lx - 12, ey - 26), (lx + 12, ey - 34)], dk(g["hair"], 0.35), 3)
        p.ln([(rx + 12, ey - 26), (rx - 12, ey - 34)], dk(g["hair"], 0.35), 3)
        pts = [(mx - 13, my + 2), (mx - 7, my - 3), (mx - 1, my + 3), (mx + 5, my - 3), (mx + 11, my + 2)]
        p.ln(pts, LINE, 2.6)
        sx, sy = X(204), Y(130)
        p.poly([(sx, sy - 14), (sx + 8, sy + 2), (sx, sy + 8), (sx - 8, sy + 2)], (140, 200, 255, 230), line="#4a7ac8", w=1.4)
    elif emo == "worry":
        eye(p, lx, ey, col, "open", -1); eye(p, rx, ey, col, "open", 1)
        p.ln([(lx - 14, ey - 24), (lx + 12, ey - 32)], dk(g["hair"], 0.35), 3)
        p.ln([(rx + 14, ey - 24), (rx - 12, ey - 32)], dk(g["hair"], 0.35), 3)
        p.arc(mx - 9, my - 2, mx + 9, my + 10, 200, 340, LINE, 2.4)
    elif emo == "cry":
        eye(p, lx, ey, col, "tight", -1); eye(p, rx, ey, col, "tight", 1)
        p.ln([(lx - 14, ey - 22), (lx + 12, ey - 30)], dk(g["hair"], 0.35), 3)
        p.ln([(rx + 14, ey - 22), (rx - 12, ey - 30)], dk(g["hair"], 0.35), 3)
        p.chord(mx - 12, my - 4, mx + 12, my + 16, 180, 360, "#7a1a2a", w=2.2)
        tc = (120, 190, 255, 220)
        for x in (lx - 4, rx + 4):
            p.ln([(x, ey + 12), (x - 2, ey + 30), (x + 1, ey + 46)], tc, 6)
    return down(im, FACE_W, FACE_H)


def portrait(body_im, face_im, arm_im):
    full = body_im.copy()
    full.alpha_composite(face_im, (FACE_X, FACE_Y))
    crop = full.crop((18, 4, 238, 264)).resize((200, 236), Image.LANCZOS)
    out = Image.new("RGBA", (200, 200), (0, 0, 0, 0))
    out.alpha_composite(crop.crop((0, 0, 200, 200)), (0, 0))
    return out


def full_preview(body_im, face_im, arm_im, ang=-140):
    full = Image.new("RGBA", (W + 40, H), (0, 0, 0, 0))
    full.alpha_composite(body_im, (40, 0))
    full.alpha_composite(face_im, (FACE_X + 40, FACE_Y))
    big = Image.new("RGBA", (300, 300), (0, 0, 0, 0))
    big.alpha_composite(arm_im, (150 - ARM_PX, 150 - ARM_PY))
    big = big.rotate(-ang, center=(150, 150), resample=Image.BICUBIC)
    full.alpha_composite(big, (SHOULDER[0] + 40 - 150, SHOULDER[1] - 150))
    return full


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else "assets/ops"
    os.makedirs(out, exist_ok=True)
    sheet = []
    for gid in GIRLS:
        faces = {e: draw_face(gid, e) for e in EMOS}
        for e, im in faces.items():
            im.save(os.path.join(out, f"{gid}_face_{e}.png"), optimize=True)
        for oi in range(2):
            b = draw_body(gid, oi)
            a = draw_arm(gid, oi)
            b.save(os.path.join(out, f"{gid}_{oi}_body.png"), optimize=True)
            a.save(os.path.join(out, f"{gid}_{oi}_arm.png"), optimize=True)
            portrait(b, faces["norm"], a).save(os.path.join(out, f"{gid}_{oi}_por.png"), optimize=True)
            sheet.append(full_preview(b, faces["norm"] if oi == 0 else faces["joy"], a))
    # контрольный лист
    cols = 11
    sw, sh = W + 40, H
    big = Image.new("RGBA", (cols * sw, 2 * sh), (40, 46, 52, 255))
    for i, im in enumerate(sheet):
        gi, oi = divmod(i, 2)
        big.alpha_composite(im, (gi * sw, oi * sh))
    big.convert("RGB").save(os.path.join(os.path.dirname(out.rstrip("/")) or ".", "..", "store", "ops_sheet.png") if False else "/tmp/ops_sheet.png")
    em = Image.new("RGBA", (FACE_W * 7, FACE_H * 2), (230, 225, 220, 255))
    for i, e in enumerate(EMOS):
        em.alpha_composite(draw_face("liza", e), (i * FACE_W, 0))
        em.alpha_composite(draw_face("valk", e), (i * FACE_W, FACE_H))
    em.convert("RGB").save("/tmp/ops_faces.png")


if __name__ == "__main__":
    main()
