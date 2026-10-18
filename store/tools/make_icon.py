# Иконка «Щит неба 3D»: рисуется в 2048 px и уменьшается (сглаживание).
import math, sys
from PIL import Image, ImageDraw, ImageFilter, ImageChops
S = 2048
def lerp(a, b, t): return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(len(a)))

def gradient(w, h, stops):
    im = Image.new('RGB', (w, h)); px = im.load()
    for y in range(h):
        t = y / (h - 1)
        for i in range(len(stops) - 1):
            if stops[i][0] <= t <= stops[i + 1][0]:
                k = (t - stops[i][0]) / (stops[i + 1][0] - stops[i][0]); c = lerp(stops[i][1], stops[i + 1][1], k); break
        for x in range(w): px[x, y] = c
    return im

def shield_poly(cx, top, w, h, n=80):
    """Классический щит: верх с лёгким прогибом, прямые бока, плавное сужение в остриё."""
    def bez(p0, p1, p2, p3, t):
        u = 1 - t
        return (u**3*p0[0]+3*u*u*t*p1[0]+3*u*t*t*p2[0]+t**3*p3[0], u**3*p0[1]+3*u*u*t*p1[1]+3*u*t*t*p2[1]+t**3*p3[1])
    L, R, B = cx - w/2, cx + w/2, top + h
    pts = []
    for i in range(n+1):                       # верх: от левого угла к правому, середина чуть ниже
        t = i/n; pts.append((L + w*t, top + w*0.06*math.sin(math.pi*t)))
    side = top + h*0.42
    for i in range(1, n+1):                    # правый бок вниз к острию
        t = i/n; pts.append(bez((R, top), (R, side), (R - w*0.02, B - h*0.22), (cx, B), t))
    for i in range(1, n):                      # левый бок вверх
        t = i/n; pts.append(bez((cx, B), (L + w*0.02, B - h*0.22), (L, side), (L, top), t))
    return pts

def build(part="full"):
    # фон: закатное небо как в игре
    bg = gradient(S, S, [(0, (22, 18, 54)), (0.45, (74, 40, 104)), (0.75, (196, 92, 88)), (1, (247, 164, 92))])
    d = ImageDraw.Draw(bg)
    # звёзды вверху
    import random; r = random.Random(7)
    for _ in range(70):
        x, y = r.randrange(S), r.randrange(int(S * 0.4)); s = r.choice([3, 4, 5, 7])
        d.ellipse([x - s, y - s, x + s, y + s], fill=(255, 245, 230))
    # горы-силуэт внизу
    hills = [(0, S)]
    for i in range(0, S + 1, 16):
        hills.append((i, S * 0.86 - 70 * math.sin(i / 260) - 40 * math.sin(i / 97 + 1)))
    hills.append((S, S)); d.polygon(hills, fill=(40, 22, 52))

    if part == 'bg': return bg
    sky_only = bg.copy()
    # щит
    cx, top, w, h = S / 2, S * 0.13, S * 0.70, S * 0.80
    mask = Image.new('L', (S, S), 0); ImageDraw.Draw(mask).polygon(shield_poly(cx, top, w, h), fill=255)
    # тень щита
    sh = mask.filter(ImageFilter.GaussianBlur(40)); shadow = Image.new('RGB', (S, S), (8, 4, 18))
    off = Image.new('L', (S, S), 0); off.paste(sh, (0, 30)); bg = Image.composite(shadow, bg, off.point(lambda v: v * 0.8))
    # золотая окантовка
    rim = gradient(S, S, [(0, (255, 226, 120)), (0.5, (246, 166, 35)), (1, (176, 92, 20))])
    bg.paste(rim, (0, 0), mask)
    inner = Image.new('L', (S, S), 0); ImageDraw.Draw(inner).polygon(shield_poly(cx, top + 70, w - 150, h - 150), fill=255)
    # внутри щита — ночное небо
    sky = gradient(S, S, [(0, (14, 22, 56)), (0.55, (34, 46, 104)), (1, (70, 58, 130))])
    bg.paste(sky, (0, 0), inner)
    d = ImageDraw.Draw(bg)

    # вспышка взрыва (дрон)
    gx, gy = S * 0.61, S * 0.355
    glow = Image.new('RGBA', (S, S), (0, 0, 0, 0)); gd = ImageDraw.Draw(glow)
    for rr, c in [(330, (255, 120, 30, 70)), (240, (255, 150, 40, 120)), (170, (255, 190, 70, 190)), (110, (255, 230, 140, 255)), (60, (255, 255, 235, 255))]:
        gd.ellipse([gx - rr, gy - rr, gx + rr, gy + rr], fill=c)
    glow = glow.filter(ImageFilter.GaussianBlur(26))
    # лучи вспышки
    rays = Image.new('RGBA', (S, S), (0, 0, 0, 0)); rd = ImageDraw.Draw(rays)
    for k in range(10):
        a = k * math.pi / 5 + 0.2; L = 380 if k % 2 == 0 else 250
        p1 = (gx + math.cos(a) * L, gy + math.sin(a) * L)
        p2 = (gx + math.cos(a + 0.09) * 70, gy + math.sin(a + 0.09) * 70); p3 = (gx + math.cos(a - 0.09) * 70, gy + math.sin(a - 0.09) * 70)
        rd.polygon([p1, p2, p3], fill=(255, 220, 130, 200))
    rays = rays.filter(ImageFilter.GaussianBlur(6))
    # обломки дрона: силуэт квадрокоптера, разорванный на части
    frag = Image.new('RGBA', (S, S), (0, 0, 0, 0)); fd = ImageDraw.Draw(frag)
    dk = (24, 16, 34, 255)
    def rotor(x, y, a):
        fd.ellipse([x - 70, y - 18, x + 70, y + 18], fill=dk); fd.ellipse([x - 16, y - 16, x + 16, y + 16], fill=dk)
    # левое крыло дрона отлетает
    fd.polygon([(gx - 310, gy - 120), (gx - 160, gy - 60), (gx - 175, gy - 25), (gx - 320, gy - 85)], fill=dk); rotor(gx - 340, gy - 135, 0)
    fd.polygon([(gx + 170, gy + 40), (gx + 320, gy + 110), (gx + 305, gy + 145), (gx + 160, gy + 75)], fill=dk); rotor(gx + 340, gy + 140, 0)
    for (x, y, rr) in [(gx + 230, gy - 230, 22), (gx - 230, gy + 190, 18), (gx + 60, gy - 300, 14), (gx - 90, gy + 290, 12)]:
        fd.polygon([(x - rr, y), (x, y - rr * 1.4), (x + rr, y + rr * 0.3), (x - rr * 0.2, y + rr)], fill=dk)

    clip = inner
    for L in (glow, rays, frag):
        a = ImageChops.multiply(L.split()[3], clip); L.putalpha(a); bg.paste(L, (0, 0), L)

    # зенитный комплекс-силуэт внизу щита (крупно), стволы смотрят на взрыв, от среза — дымный след ракеты
    veh = Image.new('RGBA', (S, S), (0, 0, 0, 0)); vd = ImageDraw.Draw(veh); c = (14, 10, 24, 255)
    by = S * 0.79
    vd.polygon([(S*0.16, by-40), (S*0.21, by-130), (S*0.62, by-130), (S*0.66, by-60), (S*0.66, by+20), (S*0.16, by+20)], fill=c)   # корпус
    vd.rounded_rectangle([S*0.15, by-10, S*0.67, by+95], 48, fill=c)                                                           # гусеница
    vd.polygon([(S*0.30, by-130), (S*0.33, by-260), (S*0.50, by-260), (S*0.53, by-130)], fill=c)                                # башня
    px0, py0 = S*0.47, by-215
    ang = math.atan2(gy - py0, gx - px0)
    nx, ny = -math.sin(ang), math.cos(ang)
    tips = []
    for o in (-30, 30):
        x0, y0 = px0 + nx*o, py0 + ny*o; x1, y1 = x0 + math.cos(ang)*300, y0 + math.sin(ang)*300
        vd.line([(x0, y0), (x1, y1)], fill=c, width=30); tips.append((x1, y1))
    vd.rectangle([S*0.335, by-360, S*0.352, by-255], fill=c)                                                                   # мачта
    vd.polygon([(S*0.285, by-395), (S*0.405, by-370), (S*0.40, by-340), (S*0.28, by-365)], fill=c)                             # антенна РЛС
    tx, ty = tips[1]
    trail = Image.new('RGBA', (S, S), (0, 0, 0, 0)); td = ImageDraw.Draw(trail)
    for i in range(70):
        t = i/69; x = tx + (gx - tx)*t; y = ty + (gy - ty)*t
        rr = 18 + 38*(1 - t); a = int(60 + 170*t)
        td.ellipse([x-rr, y-rr, x+rr, y+rr], fill=(240, 236, 250, a))
    trail = trail.filter(ImageFilter.GaussianBlur(12))
    for L in (trail, veh):
        L.putalpha(ImageChops.multiply(L.split()[3], clip)); bg.paste(L, (0, 0), L)
    # вспышка дульного пламени у второго ствола
    mf = Image.new('RGBA', (S, S), (0, 0, 0, 0)); md = ImageDraw.Draw(mf)
    x, y = tips[0]
    for rr, col in [(70, (255, 150, 40, 140)), (40, (255, 220, 120, 230))]:
        md.ellipse([x-rr, y-rr, x+rr, y+rr], fill=col)
    mf = mf.filter(ImageFilter.GaussianBlur(10)); bg.paste(mf, (0, 0), mf)

    # блик по верху окантовки
    hl = Image.new('RGBA', (S, S), (255, 255, 255, 0)); hd = ImageDraw.Draw(hl)
    hd.polygon(shield_poly(cx, top + 8, w - 30, h * 0.18), fill=(255, 255, 255, 45))
    hl.putalpha(ImageChops.multiply(hl.split()[3], ImageChops.subtract(mask, inner))); bg.paste(hl, (0, 0), hl)
    if part == 'fg':
        # только щит (с тенью) на прозрачном фоне — передний слой адаптивной иконки Android
        a = ImageChops.lighter(mask, off.point(lambda v: v * 0.8))
        fg = bg.convert('RGBA'); fg.putalpha(a); return fg
    return bg

def legacy(im, size):
    """Иконка для старых Android: скруглённый квадрат."""
    im = im.resize((size * 4, size * 4), Image.LANCZOS).convert('RGBA')
    m = Image.new('L', im.size, 0); ImageDraw.Draw(m).rounded_rectangle([0, 0, im.size[0] - 1, im.size[1] - 1], int(size * 4 * 0.18), fill=255)
    im.putalpha(m); return im.resize((size, size), Image.LANCZOS)

if __name__ == '__main__':
    out = sys.argv[1]
    im = build()
    im.resize((512, 512), Image.LANCZOS).save(out + '/icon_512.png')
    im.resize((1024, 1024), Image.LANCZOS).save(out + '/icon_1024.png')
    if len(sys.argv) > 2:   # папка android/res
        res = sys.argv[2]
        import os
        bgi, fgi = build('bg'), build('fg')
        for dname, px in [('mdpi', 48), ('hdpi', 72), ('xhdpi', 96), ('xxhdpi', 144), ('xxxhdpi', 192)]:
            d = res + '/drawable-' + dname; os.makedirs(d, exist_ok=True)
            md = res + '/mipmap-' + dname; os.makedirs(md, exist_ok=True); legacy(im, px).save(md + '/ic_launcher.png')
            ad = px * 108 // 48     # слой адаптивной иконки — 108dp, щит занимает ~64dp (в безопасной зоне 66dp)
            bgi.resize((ad, ad), Image.LANCZOS).save(d + '/ic_launcher_bg.png')
            k = 0.62; inner = int(ad * k); f = fgi.resize((inner, inner), Image.LANCZOS)
            canvas = Image.new('RGBA', (ad, ad), (0, 0, 0, 0)); canvas.paste(f, ((ad - inner) // 2, (ad - inner) // 2 + int(ad * 0.01)), f)
            canvas.save(d + '/ic_launcher_fg.png')
