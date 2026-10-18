package ru.shitneba.gfx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap.Blending;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonValue.ValueType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import ru.shitneba.G;

public final class Tex {
   static final Map<String, Texture> cache = new HashMap<>();
   static final Random R = new Random();

   static float randf() {
      return R.nextFloat();
   }

   static float rr(float a, float b) {
      return a + R.nextFloat() * (b - a);
   }

   public static Texture tex(Tex.Img im, boolean mip) {
      Pixmap pm = im.pixmap();
      Texture t = new Texture(pm, mip);
      t.setFilter(mip ? TextureFilter.MipMapLinearLinear : TextureFilter.Linear, TextureFilter.Linear);
      t.setWrap(TextureWrap.Repeat, TextureWrap.Repeat);
      return t;
   }

   static void noise(Tex.Img im, float amt) {
      for (int i = 0; i < im.w * im.h; i++) {
         float k = (randf() - 0.5F) * amt;
         im.p[i * 4] = im.p[i * 4] + k;
         im.p[i * 4 + 1] = im.p[i * 4 + 1] + k;
         im.p[i * 4 + 2] = im.p[i * 4 + 2] + k;
      }
   }

   public static void circle(Tex.Img im, float cx, float cy, float r, Color col) {
      for (int y = Math.max(0, (int)(cy - r)); y < Math.min(im.h, (int)(cy + r) + 1); y++) {
         for (int x = Math.max(0, (int)(cx - r)); x < Math.min(im.w, (int)(cx + r) + 1); x++) {
            if ((x - cx) * (x - cx) + (y - cy) * (y - cy) <= r * r) {
               if (col.a >= 1.0F) {
                  im.set(x, y, col);
               } else {
                  int i = (y * im.w + x) * 4;
                  im.set(
                     x,
                     y,
                     MathUtils.lerp(im.p[i], col.r, col.a),
                     MathUtils.lerp(im.p[i + 1], col.g, col.a),
                     MathUtils.lerp(im.p[i + 2], col.b, col.a),
                     MathUtils.lerp(im.p[i + 3], 1.0F, col.a)
                  );
               }
            }
         }
      }
   }

   static boolean inPoly(float px, float py, float[] xs, float[] ys) {
      boolean c = false;
      int i = 0;

      for (int j = xs.length - 1; i < xs.length; j = i++) {
         if (ys[i] > py != ys[j] > py && px < (xs[j] - xs[i]) * (py - ys[i]) / (ys[j] - ys[i]) + xs[i]) {
            c = !c;
         }
      }

      return c;
   }

   public static void poly(Tex.Img im, float[] xs, float[] ys, Color col) {
      float mnx = 1.0E9F;
      float mny = 1.0E9F;
      float mxx = -1.0E9F;
      float mxy = -1.0E9F;

      for (int i = 0; i < xs.length; i++) {
         mnx = Math.min(mnx, xs[i]);
         mny = Math.min(mny, ys[i]);
         mxx = Math.max(mxx, xs[i]);
         mxy = Math.max(mxy, ys[i]);
      }

      for (int y = Math.max(0, (int)mny); y < Math.min(im.h, (int)mxy + 1); y++) {
         for (int x = Math.max(0, (int)mnx); x < Math.min(im.w, (int)mxx + 1); x++) {
            if (inPoly(x + 0.5F, y + 0.5F, xs, ys)) {
               im.set(x, y, col);
            }
         }
      }
   }

   static void band(Tex.Img im, float x0, float y0, float x1, float y1, float w, Color col) {
      float dx = x1 - x0;
      float dy = y1 - y0;
      float l = (float)Math.sqrt(dx * dx + dy * dy);
      float nx = -dy / l * w / 2.0F;
      float ny = dx / l * w / 2.0F;
      poly(im, new float[]{x0 + nx, x1 + nx, x1 - nx, x0 - nx}, new float[]{y0 + ny, y1 + ny, y1 - ny, y0 - ny}, col);
   }

   static Color skCol(JsonValue cols, int i) {
      return G.hex(cols.getString(i));
   }

   static JsonValue skin(String id) {
      JsonValue sk = G.D.get("SK_VEH").get(id);
      return sk == null ? G.D.get("SK_VEH").get("olive") : sk;
   }

   static JsonValue skCols(JsonValue sk) {
      JsonValue c = sk.get("cols");
      if (c != null) {
         return c;
      } else {
         JsonValue a = new JsonValue(ValueType.array);
         a.addChild(new JsonValue(sk.getString("hull")));
         a.addChild(new JsonValue(sk.getString("dark")));
         return a;
      }
   }

   public static Texture camo(String id) {
      String key = "camo_" + id;
      if (cache.containsKey(key)) {
         return cache.get(key);
      } else {
         JsonValue sk = skin(id);
         int S = 128;
         Tex.Img im = new Tex.Img(S, S, G.hex(sk.getString("hull")));
         JsonValue cols = skCols(sk);
         int nc = cols.size;
         String pat = sk.getString("pat", "");
         switch (pat) {
            case "blob3":
               for (int i = 0; i < 22; i++) {
                  Color col = skCol(cols, 1 + i % (nc - 1));
                  float cx = randf() * S;
                  float cy = randf() * S;
                  float r = 9.0F + randf() * 17.0F;

                  for (int k = 0; k < 5; k++) {
                     circle(im, cx + rr(-r, r), cy + rr(-r * 0.5F, r * 0.5F), r * rr(0.5F, 0.9F), col);
                  }
               }
               break;
            case "digital":
               for (int y = 0; y < S; y += 6) {
                  for (int x = 0; x < S; x += 6) {
                     if (randf() < 0.45F) {
                        im.fillRect(x, y, 6, 6, skCol(cols, 1 + R.nextInt(nc - 1)));
                     }
                  }
               }
               break;
            case "stripes":
               for (int i = -4; i < 12; i++) {
                  band(im, i * 17.0F, 0.0F, i * 17.0F + 30.0F, S, 7.0F, skCol(cols, 1));
               }
               break;
            case "splinter":
               for (int i = 0; i < 26; i++) {
                  float x = randf() * S;
                  float y = randf() * S;
                  poly(
                     im,
                     new float[]{x, x + rr(10.0F, 35.0F), x + rr(5.0F, 20.0F)},
                     new float[]{y, y + rr(-10.0F, 10.0F), y + rr(10.0F, 25.0F)},
                     skCol(cols, 1 + i % (nc - 1))
                  );
               }
               break;
            case "metal":
               for (int y = 0; y < S; y++) {
                  for (int x = 0; x < S; x++) {
                     float k = (float)(x + y) / (2 * S);
                     Color c = k < 0.5F ? skCol(cols, 0).cpy().lerp(skCol(cols, 1), k * 2.0F) : skCol(cols, 1).cpy().lerp(skCol(cols, 2), (k - 0.5F) * 2.0F);
                     im.set(x, y, c);
                  }
               }
         }

         noise(im, 0.07F);

         for (int y = S * 6 / 10; y < S; y++) {
            float k = (y - S * 0.6F) / (S * 0.4F) * 0.3F;

            for (int xx = 0; xx < S; xx++) {
               int i = (y * S + xx) * 4;
               im.set(xx, y, MathUtils.lerp(im.p[i], 0.47F, k), MathUtils.lerp(im.p[i + 1], 0.39F, k), MathUtils.lerp(im.p[i + 2], 0.27F, k), 1.0F);
            }
         }

         Texture t = tex(im, true);
         cache.put(key, t);
         return t;
      }
   }

   public static Texture ground(String name) {
      String key = "gr_" + name;
      if (cache.containsKey(key)) {
         return cache.get(key);
      } else {
         int S = 128;
         Tex.Img im = new Tex.Img(S, S, new Color(0.85F, 0.85F, 0.85F, 1.0F));

         for (int i = 0; i < 40; i++) {
            circle(im, randf() * S, randf() * S, rr(5.0F, 20.0F), new Color(rr(0.7F, 1.0F), rr(0.7F, 1.0F), rr(0.7F, 1.0F), 0.35F));
         }

         if (name.equals("desert")) {
            for (int i = 0; i < 14; i++) {
               float y0 = i * 9.0F;

               for (int x = 0; x < S; x++) {
                  im.set(x, ((int)(y0 + Math.sin(x * 0.1 + i) * 2.0) % S + S) % S, 0.6F, 0.6F, 0.6F, 1.0F);
               }
            }
         } else if (name.equals("snow")) {
            for (int i = 0; i < 500; i++) {
               if (randf() < 0.5F) {
                  im.set(R.nextInt(S), R.nextInt(S), 1.0F, 1.0F, 1.0F, 1.0F);
               } else {
                  im.set(R.nextInt(S), R.nextInt(S), 0.75F, 0.78F, 0.85F, 1.0F);
               }
            }
         } else {
            for (int ix = 0; ix < 1300; ix++) {
               int x = R.nextInt(S);
               int y = R.nextInt(S);
               float l = rr(0.5F, 1.0F);

               for (int k = 0; k < 3; k++) {
                  im.set((x + (randf() < 0.3F ? k : 0)) % S, (y - k + S) % S, l, l, l, 1.0F);
               }
            }
         }

         Texture t = tex(im, true);
         cache.put(key, t);
         return t;
      }
   }

   public static Texture grain(String key, float base, int dots, float dl0, float dl1) {
      if (cache.containsKey(key)) {
         return cache.get(key);
      } else {
         int S = 64;
         Tex.Img im = new Tex.Img(S, S, new Color(base, base, base, 1.0F));

         for (int i = 0; i < dots; i++) {
            float l = rr(dl0, dl1);
            im.set(R.nextInt(S), R.nextInt(S), l, l, l, 1.0F);
            im.set(R.nextInt(S), R.nextInt(S), l * 0.9F, l * 0.9F, l * 0.9F, 1.0F);
         }

         Texture t = tex(im, true);
         cache.put(key, t);
         return t;
      }
   }

   public static Texture slab() {
      if (cache.containsKey("slab")) {
         return cache.get("slab");
      } else {
         int S = 128;
         Tex.Img im = new Tex.Img(S, S, new Color(0.78F, 0.78F, 0.78F, 1.0F));
         noise(im, 0.1F);
         Color d = new Color(0.25F, 0.25F, 0.25F, 1.0F);

         for (int i = 0; i < 5; i++) {
            im.fillRect(i * S / 4, 0, 1, S, d);
            im.fillRect(0, i * S / 4, S, 1, d);
         }

         Texture t = tex(im, true);
         cache.put("slab", t);
         return t;
      }
   }

   public static Texture soft(float power) {
      String key = "soft" + power;
      if (cache.containsKey(key)) {
         return cache.get(key);
      } else {
         int S = 64;
         Tex.Img im = new Tex.Img(S, S, new Color(1.0F, 1.0F, 1.0F, 0.0F));

         for (int y = 0; y < S; y++) {
            for (int x = 0; x < S; x++) {
               float dx = x - S / 2.0F + 0.5F;
               float dy = y - S / 2.0F + 0.5F;
               float r = (float)Math.sqrt(dx * dx + dy * dy) / (S / 2.0F);
               im.set(x, y, 1.0F, 1.0F, 1.0F, (float)Math.pow(Math.max(0.0F, 1.0F - r), power));
            }
         }

         Texture t = tex(im, true);
         t.setWrap(TextureWrap.ClampToEdge, TextureWrap.ClampToEdge);
         cache.put(key, t);
         return t;
      }
   }

   public static Texture smoke() {
      if (cache.containsKey("smoke")) {
         return cache.get("smoke");
      } else {
         int S = 128;
         Tex.Noise fn = new Tex.Noise(11L);
         Tex.Img im = new Tex.Img(S, S, new Color(1.0F, 1.0F, 1.0F, 0.0F));

         for (int y = 0; y < S; y++) {
            for (int x = 0; x < S; x++) {
               float dx = x - S / 2.0F + 0.5F;
               float dy = y - S / 2.0F + 0.5F;
               float r = (float)Math.sqrt(dx * dx + dy * dy) / (S / 2.0F);
               float n = fn.fbm(x * 0.035F, y * 0.035F, 4) * 0.5F + 0.5F;
               float fall = MathUtils.clamp(1.0F - r, 0.0F, 1.0F);
               float a = MathUtils.clamp((float)Math.pow(fall, 1.3) * (0.35F + 1.1F * n) - 0.05F, 0.0F, 1.0F);
               float l = 0.8F + 0.2F * n;
               im.set(x, y, l, l, l, a);
            }
         }

         Texture t = tex(im, true);
         t.setWrap(TextureWrap.ClampToEdge, TextureWrap.ClampToEdge);
         cache.put("smoke", t);
         return t;
      }
   }

   public static Texture cloud() {
      if (cache.containsKey("cloud")) {
         return cache.get("cloud");
      } else {
         int W = 128;
         int H = 64;
         Tex.Img im = new Tex.Img(W, H, new Color(1.0F, 1.0F, 1.0F, 0.0F));

         for (int k = 0; k < 22; k++) {
            float cx = rr(22.0F, 106.0F);
            float cy = rr(28.0F, 44.0F) - Math.abs(cx - 64.0F) * 0.1F;
            float Rr = rr(9.0F, 17.0F);

            for (int y = 0; y < H; y++) {
               for (int x = 0; x < W; x++) {
                  float r = (float)Math.sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy)) / Rr;
                  if (r < 1.0F) {
                     float ca = im.get(x, y, 3);
                     float l = 1.0F - (y - cy + Rr) / (Rr * 2.0F) * 0.12F;
                     im.set(x, y, l, l, l, Math.min(1.0F, ca + (1.0F - r) * 0.5F));
                  }
               }
            }
         }

         Texture t = tex(im, true);
         t.setWrap(TextureWrap.ClampToEdge, TextureWrap.ClampToEdge);
         cache.put("cloud", t);
         return t;
      }
   }

   public static Texture camoTile(String id) {
      String key = "camot_" + id;
      if (cache.containsKey(key)) {
         return cache.get(key);
      } else {
         JsonValue sk = skin(id);
         int S = 256;
         Tex.Img im = new Tex.Img(256, 256, G.hex(sk.getString("hull")));
         JsonValue cols = skCols(sk);
         int nc = cols.size;
         String pat = sk.getString("pat", "");
         switch (pat) {
            case "blob3":
               for (int i = 0; i < 34; i++) {
                  Color col = skCol(cols, 1 + i % (nc - 1));
                  float cx = randf() * 256.0F;
                  float cy = randf() * 256.0F;
                  float r = 14.0F + randf() * 26.0F;

                  for (int k = 0; k < 6; k++) {
                     float px = cx + rr(-r, r);
                     float py = cy + rr(-r * 0.5F, r * 0.5F);
                     float pr = r * rr(0.45F, 0.85F);

                     for (int ox = -256; ox <= 256; ox += 256) {
                        for (int oy = -256; oy <= 256; oy += 256) {
                           if (!(px + ox + pr < 0.0F) && !(px + ox - pr > 256.0F) && !(py + oy + pr < 0.0F) && !(py + oy - pr > 256.0F)) {
                              circle(im, px + ox, py + oy, pr, col);
                           }
                        }
                     }
                  }
               }
               break;
            case "digital":
               for (int y = 0; y < 256; y += 8) {
                  for (int x = 0; x < 256; x += 8) {
                     if (randf() < 0.45F) {
                        im.fillRect(x, y, 8, 8, skCol(cols, 1 + R.nextInt(nc - 1)));
                     }
                  }
               }
               break;
            case "stripes":
               for (int i = -8; i < 16; i++) {
                  band(im, i * 32.0F, 0.0F, i * 32.0F + 64.0F, 256.0F, 12.0F, skCol(cols, 1));
               }
               break;
            case "splinter":
               for (int i = 0; i < 44; i++) {
                  float x = randf() * 216.0F;
                  float y = randf() * 226.0F;
                  poly(
                     im,
                     new float[]{x, x + rr(14.0F, 40.0F), x + rr(6.0F, 26.0F)},
                     new float[]{y, y + rr(-12.0F, 12.0F), y + rr(12.0F, 30.0F)},
                     skCol(cols, 1 + i % (nc - 1))
                  );
               }
               break;
            case "metal":
               for (int y = 0; y < 256; y++) {
                  for (int x = 0; x < 256; x++) {
                     float k = 0.5F + 0.5F * (float)Math.sin((x + y) / 256.0F * (float) (Math.PI * 2));
                     im.set(x, y, skCol(cols, 0).cpy().lerp(skCol(cols, Math.min(2, nc - 1)), k));
                  }
               }
         }

         noise(im, 0.04F);
         Texture t = tex(im, true);
         cache.put(key, t);
         return t;
      }
   }

   public static Texture panel() {
      if (cache.containsKey("panel")) {
         return cache.get("panel");
      } else {
         int S = 256;
         Tex.Noise fn = new Tex.Noise(7L);
         Tex.Img im = new Tex.Img(256, 256, new Color(1.0F, 0.0F, 0.0F, 1.0F));

         for (int y = 0; y < 256; y++) {
            for (int x = 0; x < 256; x++) {
               float g = fn.periodicFbm(x / 256.0F * 3.0F, y / 256.0F * 3.0F, 3, 4) * 0.5F + 0.5F;
               im.set(x, y, 1.0F, MathUtils.clamp((g - 0.3F) * 1.8F, 0.0F, 1.0F), 0.0F, 1.0F);
            }
         }

         ArrayList<int[]> rects = new ArrayList<>();
         ArrayList<int[]> out = new ArrayList<>();
         rects.add(new int[]{0, 0, 256, 256});

         while (!rects.isEmpty()) {
            int[] r = rects.remove(rects.size() - 1);
            if ((r[2] > 120 || r[3] > 120 || randf() < 0.35F) && r[2] > 48 && r[3] > 48) {
               if (r[2] >= r[3]) {
                  int cut = (int)(r[2] * rr(0.3F, 0.7F));
                  rects.add(new int[]{r[0], r[1], cut, r[3]});
                  rects.add(new int[]{r[0] + cut, r[1], r[2] - cut, r[3]});
               } else {
                  int cut = (int)(r[3] * rr(0.3F, 0.7F));
                  rects.add(new int[]{r[0], r[1], r[2], cut});
                  rects.add(new int[]{r[0], r[1] + cut, r[2], r[3] - cut});
               }
            } else {
               out.add(r);
            }
         }

         for (int[] r : out) {
            int x0 = r[0];
            int y0 = r[1];

            for (int x = x0; x < x0 + r[2]; x++) {
               seam(im, x, y0, 0.35F);
               seam(im, x, y0 + 1, 0.8F);
            }

            for (int y = y0; y < y0 + r[3]; y++) {
               seam(im, x0, y, 0.35F);
               seam(im, x0 + 1, y, 0.8F);
            }

            if (randf() < 0.5F) {
               for (int x = x0 + 4; x < x0 + r[2] - 2; x += 7) {
                  seam(im, x, y0 + 4, 0.55F);
               }

               for (int y = y0 + 4; y < y0 + r[3] - 2; y += 7) {
                  seam(im, x0 + 4, y, 0.55F);
               }
            }
         }

         for (int i = 0; i < 60; i++) {
            int x = R.nextInt(256);
            int y = R.nextInt(256);
            float a = randf() * (float) (Math.PI * 2);
            int L = 4 + R.nextInt(15);

            for (int k = 0; k < L; k++) {
               int px = Math.floorMod(x + (int)(Math.cos(a) * k), 256);
               int py = Math.floorMod(y + (int)(Math.sin(a) * k), 256);
               im.p[(py * 256 + px) * 4 + 2] = 1.0F;
            }
         }

         Texture t = tex(im, true);
         cache.put("panel", t);
         return t;
      }
   }

   static void seam(Tex.Img im, int x, int y, float v) {
      int S = im.w;
      x = Math.floorMod(x, S);
      y = Math.floorMod(y, S);
      int i = (y * S + x) * 4;
      im.p[i] = Math.min(im.p[i], v);
   }

   public static Texture grass() {
      if (cache.containsKey("grass")) {
         return cache.get("grass");
      } else {
         int S = 128;
         Tex.Img im = new Tex.Img(S, S, new Color(0.75F, 0.78F, 0.55F, 0.0F));

         for (int i = 0; i < 46; i++) {
            float x0 = rr(10.0F, S - 10);
            float hh = rr(S * 0.45F, S * 0.98F);
            float bend = rr(-0.35F, 0.35F);
            float w0 = rr(1.6F, 2.6F);
            float l = rr(0.7F, 1.0F);

            for (int k = 0; k < (int)hh; k++) {
               float t = k / hh;
               float xc = x0 + bend * k * t;
               float w = w0 * (1.0F - t * 0.85F);
               int py = S - 1 - k;
               float lum = l * (0.62F + 0.38F * t);

               for (int px = (int)Math.floor(xc - w - 1.0F); px < (int)Math.ceil(xc + w + 1.0F); px++) {
                  if (px >= 0 && px < S) {
                     float cov = MathUtils.clamp(w - Math.abs(px + 0.5F - xc) + 0.5F, 0.0F, 1.0F);
                     if (!(cov <= 0.0F)) {
                        float na = Math.max(im.get(px, py, 3), cov);
                        im.set(px, py, lum, lum * 1.02F, lum * 0.78F, na);
                     }
                  }
               }
            }
         }

         Texture t = tex(im, true);
         t.setWrap(TextureWrap.ClampToEdge, TextureWrap.ClampToEdge);
         cache.put("grass", t);
         return t;
      }
   }

   public static Texture trail() {
      if (cache.containsKey("trail")) {
         return cache.get("trail");
      } else {
         int W = 32;
         int H = 128;
         Tex.Noise fn = new Tex.Noise(3L);
         Tex.Img im = new Tex.Img(W, H, new Color(1.0F, 1.0F, 1.0F, 0.0F));

         for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
               float u = (x + 0.5F) / W;
               float edge = (float)Math.pow(MathUtils.clamp(1.0F - Math.abs(u * 2.0F - 1.0F), 0.0F, 1.0F), 1.2);
               float n = fn.periodicFbm((float)x / W * 2.0F, (float)y / H * 8.0F, 0, 3) * 0.5F + 0.5F;
               float a = MathUtils.clamp(edge * (0.75F + 0.7F * n), 0.0F, 1.0F);
               float l = 0.86F + 0.14F * n;
               im.set(x, y, l, l, l, a);
            }
         }

         Texture t = tex(im, true);
         cache.put("trail", t);
         return t;
      }
   }

   public static Texture flameCore() {
      if (cache.containsKey("fcore")) {
         return cache.get("fcore");
      } else {
         Tex.Img im = new Tex.Img(4, 64, Color.WHITE);

         for (int y = 0; y < 64; y++) {
            float k = y / 63.0F;

            for (int x = 0; x < 4; x++) {
               im.set(x, y, 1.0F, 0.95F - k * 0.35F, 0.8F - k * 0.6F, (float)Math.pow(1.0F - k, 1.6));
            }
         }

         Texture t = tex(im, false);
         t.setWrap(TextureWrap.ClampToEdge, TextureWrap.ClampToEdge);
         cache.put("fcore", t);
         return t;
      }
   }

   public static Texture white() {
      if (cache.containsKey("white")) {
         return cache.get("white");
      } else {
         Tex.Img im = new Tex.Img(4, 4, Color.WHITE);
         Texture t = tex(im, false);
         cache.put("white", t);
         return t;
      }
   }

   public static void clear() {
      for (Texture t : cache.values()) {
         t.dispose();
      }

      cache.clear();
   }

   private Tex() {
   }

   public static final class Img {
      public final int w;
      public final int h;
      public final float[] p;

      public Img(int w, int h, Color c) {
         this.w = w;
         this.h = h;
         this.p = new float[w * h * 4];

         for (int i = 0; i < w * h; i++) {
            this.p[i * 4] = c.r;
            this.p[i * 4 + 1] = c.g;
            this.p[i * 4 + 2] = c.b;
            this.p[i * 4 + 3] = c.a;
         }
      }

      public void set(int x, int y, float r, float g, float b, float a) {
         if (x >= 0 && y >= 0 && x < this.w && y < this.h) {
            int i = (y * this.w + x) * 4;
            this.p[i] = r;
            this.p[i + 1] = g;
            this.p[i + 2] = b;
            this.p[i + 3] = a;
         }
      }

      public void set(int x, int y, Color c) {
         this.set(x, y, c.r, c.g, c.b, c.a);
      }

      public float get(int x, int y, int ch) {
         return this.p[(y * this.w + x) * 4 + ch];
      }

      public void fillRect(int x0, int y0, int ww, int hh, Color c) {
         for (int y = Math.max(0, y0); y < Math.min(this.h, y0 + hh); y++) {
            for (int x = Math.max(0, x0); x < Math.min(this.w, x0 + ww); x++) {
               this.set(x, y, c);
            }
         }
      }

      public Pixmap pixmap() {
         Pixmap pm = new Pixmap(this.w, this.h, Format.RGBA8888);
         pm.setBlending(Blending.None);

         for (int y = 0; y < this.h; y++) {
            for (int x = 0; x < this.w; x++) {
               int i = (y * this.w + x) * 4;
               pm.drawPixel(
                  x,
                  y,
                  Color.rgba8888(
                     MathUtils.clamp(this.p[i], 0.0F, 1.0F),
                     MathUtils.clamp(this.p[i + 1], 0.0F, 1.0F),
                     MathUtils.clamp(this.p[i + 2], 0.0F, 1.0F),
                     MathUtils.clamp(this.p[i + 3], 0.0F, 1.0F)
                  )
               );
            }
         }

         return pm;
      }
   }

   public static final class Noise {
      final int[] perm = new int[512];
      final float[] gx = new float[256];
      final float[] gy = new float[256];

      public Noise(long seed) {
         Random r = new Random(seed);
         int[] p = new int[256];

         for (int i = 0; i < 256; i++) {
            p[i] = i;
            float a = r.nextFloat() * (float) (Math.PI * 2);
            this.gx[i] = (float)Math.cos(a);
            this.gy[i] = (float)Math.sin(a);
         }

         for (int i = 255; i > 0; i--) {
            int j = r.nextInt(i + 1);
            int t = p[i];
            p[i] = p[j];
            p[j] = t;
         }

         for (int i = 0; i < 512; i++) {
            this.perm[i] = p[i & 0xFF];
         }
      }

      static float fade(float t) {
         return t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
      }

      float grad(int ix, int iy, float x, float y) {
         int h = this.perm[this.perm[ix & 0xFF] + iy & 511] & 0xFF;
         return this.gx[h] * (x - ix) + this.gy[h] * (y - iy);
      }

      public float noise(float x, float y) {
         return this.noiseP(x, y, 0);
      }

      public float noiseP(float x, float y, int period) {
         int x0 = (int)Math.floor(x);
         int y0 = (int)Math.floor(y);
         float fx = fade(x - x0);
         float fy = fade(y - y0);
         int bx = x0 + 1;
         int by = y0 + 1;
         int wx0 = period > 0 ? Math.floorMod(x0, period) : x0;
         int wx1 = period > 0 ? Math.floorMod(bx, period) : bx;
         int wy0 = period > 0 ? Math.floorMod(y0, period) : y0;
         int wy1 = period > 0 ? Math.floorMod(by, period) : by;
         float n00 = this.gradW(wx0, wy0, x - x0, y - y0);
         float n10 = this.gradW(wx1, wy0, x - bx, y - y0);
         float n01 = this.gradW(wx0, wy1, x - x0, y - by);
         float n11 = this.gradW(wx1, wy1, x - bx, y - by);
         return MathUtils.lerp(MathUtils.lerp(n00, n10, fx), MathUtils.lerp(n01, n11, fx), fy) * 1.414F;
      }

      float gradW(int ix, int iy, float dx, float dy) {
         int h = this.perm[this.perm[ix & 0xFF] + (iy & 0xFF) & 511] & 0xFF;
         return this.gx[h] * dx + this.gy[h] * dy;
      }

      public float fbm(float x, float y, int oct) {
         float s = 0.0F;
         float a = 1.0F;
         float f = 1.0F;
         float norm = 0.0F;

         for (int i = 0; i < oct; i++) {
            s += this.noiseP(x * f, y * f, 0) * a;
            norm += a;
            a *= 0.5F;
            f *= 2.0F;
         }

         return s / norm;
      }

      public float periodicFbm(float x, float y, int period, int oct) {
         float s = 0.0F;
         float a = 1.0F;
         float norm = 0.0F;
         int f = 1;

         for (int i = 0; i < oct; i++) {
            s += this.noiseP(x * f, y * f, period > 0 ? period * f : 0) * a;
            norm += a;
            a *= 0.5F;
            f *= 2;
         }

         return s / norm;
      }
   }
}
