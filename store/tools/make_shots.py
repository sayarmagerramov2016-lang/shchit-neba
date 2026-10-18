# Скриншоты для RuStore: кадр игры + подпись сверху. 1920×1080 (16:9), JPG < 3 МБ.
# python3 make_shots.py <выход> <файл1.png> "<подпись1>" <файл2.png> "<подпись2>" ...
import sys, os
from PIL import Image, ImageDraw, ImageFont, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
FONT = os.path.join(HERE, '..', '..', 'assets', 'RussoOne.ttf')
W, H = 1920, 1080
BAND = 150            # высота полосы с подписью

def compose(src, text):
    shot = Image.open(src).convert('RGB').resize((W, H), Image.LANCZOS)
    # фон — тот же кадр, размытый и затемнённый
    bg = shot.filter(ImageFilter.GaussianBlur(30))
    bg = Image.blend(bg, Image.new('RGB', (W, H), (14, 10, 30)), 0.55)
    s = (H - BAND - 24) / H
    sw, sh = int(W * s), int(H * s)
    small = shot.resize((sw, sh), Image.LANCZOS)
    x, y = (W - sw) // 2, BAND
    # тень и золотая рамка вокруг кадра
    sd = Image.new('L', (W, H), 0); ImageDraw.Draw(sd).rectangle([x - 6, y - 6 + 14, x + sw + 6, y + sh + 6 + 14], fill=200)
    bg.paste((0, 0, 0), (0, 0), sd.filter(ImageFilter.GaussianBlur(18)))
    d = ImageDraw.Draw(bg)
    d.rectangle([x - 5, y - 5, x + sw + 4, y + sh + 4], fill=(246, 176, 48))
    bg.paste(small, (x, y))
    f = ImageFont.truetype(FONT, 64)
    while d.textlength(text, font=f) > W - 160: f = ImageFont.truetype(FONT, f.size - 2)
    tw = d.textlength(text, font=f)
    ty = (BAND - f.size) // 2 - 6
    d.text(((W - tw) / 2 + 3, ty + 4), text, font=f, fill=(0, 0, 0))
    d.text(((W - tw) / 2, ty), text, font=f, fill=(255, 255, 255))
    return bg

if __name__ == '__main__':
    out = sys.argv[1]; os.makedirs(out, exist_ok=True)
    args = sys.argv[2:]
    for i in range(0, len(args), 2):
        im = compose(args[i], args[i + 1])
        p = os.path.join(out, 'screen_%02d.jpg' % (i // 2 + 1))
        im.save(p, quality=92); print(p, os.path.getsize(p) // 1024, 'KB')
