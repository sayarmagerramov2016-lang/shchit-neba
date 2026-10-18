package ru.shitneba.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import java.util.HashMap;
import java.util.Map;

public class Draw implements Disposable {
   public static final float VH = 540.0F;
   public final PolygonSpriteBatch b = new PolygonSpriteBatch(4000);
   public final OrthographicCamera cam = new OrthographicCamera();
   public float w = 960.0F;
   public float h = 540.0F;
   public float s = 1.0F;
   public boolean off;
   final Texture white;
   final float[] vb = new float[320];
   final short[] ib = new short[186];
   final GlyphLayout gl = new GlyphLayout();
   public static final int MAIN = 0;
   public static final int TITLE = 1;
   public static final int BOLD = 2;
   public static final int WIDE = 3;
   final FreeTypeFontGenerator[] gens = new FreeTypeFontGenerator[4];
   final Map<Integer, BitmapFont> fonts = new HashMap<>();
   static final String CHARS = "0123456789 !?.,:;-+×%/()«»—·…№'\"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyzАБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя";
   int nv;
   int ni;

   public Draw() {
      Pixmap p = new Pixmap(4, 4, Format.RGBA8888);
      p.setColor(Color.WHITE);
      p.fill();
      this.white = new Texture(p);
      p.dispose();
      this.gens[0] = new FreeTypeFontGenerator(Gdx.files.internal("Exo2-Medium.ttf"));
      this.gens[1] = new FreeTypeFontGenerator(Gdx.files.internal("RussoOne.ttf"));
      this.gens[2] = new FreeTypeFontGenerator(Gdx.files.internal("Exo2-Bold.ttf"));
      this.gens[3] = this.gens[1];
      this.resize();
   }

   public void resize() {
      int sw = Math.max(1, Gdx.graphics.getWidth());
      int sh = Math.max(1, Gdx.graphics.getHeight());
      this.s = sh / 540.0F;
      this.h = 540.0F;
      this.w = sw / this.s;
      if (this.w < 720.0F) {
         this.w = 720.0F;
         this.s = sw / this.w;
         this.h = sh / this.s;
      }

      this.cam.setToOrtho(true, this.w, this.h);
      this.cam.update();

      for (BitmapFont f : this.fonts.values()) {
         f.dispose();
      }

      this.fonts.clear();
   }

   public void begin() {
      this.b.setProjectionMatrix(this.cam.combined);
      this.b.begin();
      this.b.enableBlending();
      this.b.setBlendFunction(770, 771);
   }

   public void end() {
      this.b.end();
   }

   public BitmapFont font(int id, float size) {
      int px = Math.max(6, Math.round(size * this.s));
      int key = id * 1000 + px;
      BitmapFont f = this.fonts.get(key);
      if (f == null) {
         FreeTypeFontParameter p = new FreeTypeFontParameter();
         p.size = px;
         p.flip = true;
         p.incremental = true;
         p.characters = "0123456789 !?.,:;-+×%/()«»—·…№'\"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyzАБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя";
         p.minFilter = TextureFilter.Linear;
         p.magFilter = TextureFilter.Linear;
         p.kerning = true;
         if (id == 3) {
            p.spaceX = Math.round(2.0F * this.s);
         }

         f = this.gens[id].generateFont(p);
         f.setUseIntegerPositions(false);
         f.getData().setScale(1.0F / this.s);
         f.getData().markupEnabled = false;
         this.fonts.put(key, f);
      }

      return f;
   }

   public void prewarm() {
      float[][] fs = new float[][]{
         {0.0F, 11.0F},
         {0.0F, 12.0F},
         {0.0F, 13.0F},
         {0.0F, 14.0F},
         {0.0F, 15.0F},
         {0.0F, 16.0F},
         {1.0F, 11.0F},
         {1.0F, 12.0F},
         {1.0F, 13.0F},
         {1.0F, 14.0F},
         {1.0F, 16.0F},
         {1.0F, 18.0F},
         {1.0F, 19.0F},
         {1.0F, 20.0F},
         {1.0F, 22.0F},
         {1.0F, 24.0F},
         {1.0F, 34.0F},
         {2.0F, 12.0F},
         {2.0F, 13.0F},
         {2.0F, 14.0F},
         {2.0F, 24.0F}
      };

      for (float[] f : fs) {
         this.font((int)f[0], f[1]);
      }
   }

   public float textW(String t, float size, int font) {
      this.gl.setText(this.font(font, size), t);
      return this.gl.width;
   }

   public void text(String t, float x, float baseline, float size, Color c, int font, int align) {
      if (!this.off && t != null && !t.isEmpty()) {
         BitmapFont f = this.font(font, size);
         f.setColor(c);
         float y = baseline - f.getCapHeight();
         int al = align < 0 ? 8 : (align == 0 ? 1 : 16);
         f.draw(this.b, t, x, y, 0.0F, al, false);
      }
   }

   public void text(String t, float x, float baseline, float size, Color c) {
      this.text(t, x, baseline, size, c, 0, -1);
   }

   public float wrap(String t, float x, float top, float wd, float size, Color c, int font, int align) {
      BitmapFont f = this.font(font, size);
      int al = align < 0 ? 8 : (align == 0 ? 1 : 16);
      this.gl.setText(f, t, c, wd, al, true);
      float hh = this.gl.height + this.lineGap(f);
      if (!this.off) {
         float asc = f.getLineHeight() - f.getCapHeight();
         this.b.setColor(Color.WHITE);
         f.draw(this.b, this.gl, x, top + asc * 0.45F);
      }

      return hh;
   }

   float lineGap(BitmapFont f) {
      return (f.getLineHeight() - f.getCapHeight()) * 0.9F;
   }

   public float lineH(float size, int font) {
      return this.font(font, size).getLineHeight();
   }

   float pack(Color c, float a) {
      return Color.toFloatBits(c.r, c.g, c.b, c.a * a);
   }

   void v(float x, float y, float col) {
      int o = this.nv * 5;
      this.vb[o] = x;
      this.vb[o + 1] = y;
      this.vb[o + 2] = col;
      this.vb[o + 3] = 0.5F;
      this.vb[o + 4] = 0.5F;
      this.nv++;
   }

   void flushPoly() {
      if (this.nv > 0 && this.ni > 0) {
         this.b.draw(this.white, this.vb, 0, this.nv * 5, this.ib, 0, this.ni);
      }

      this.nv = 0;
      this.ni = 0;
   }

   public void rect(float x, float y, float ww, float hh, Color c) {
      this.rect(x, y, ww, hh, c, 1.0F);
   }

   public void rect(float x, float y, float ww, float hh, Color c, float a) {
      if (!this.off && !(ww <= 0.0F) && !(hh <= 0.0F)) {
         float f = this.pack(c, a);
         this.quad(x, y, f, x + ww, y, f, x + ww, y + hh, f, x, y + hh, f);
      }
   }

   public void grad(float x, float y, float ww, float hh, Color top, float ta, Color bot, float ba) {
      if (!this.off) {
         float t = this.pack(top, ta);
         float bo = this.pack(bot, ba);
         this.quad(x, y, t, x + ww, y, t, x + ww, y + hh, bo, x, y + hh, bo);
      }
   }

   public void gradH(float x, float y, float ww, float hh, Color l, float la, Color r, float ra) {
      if (!this.off) {
         float a = this.pack(l, la);
         float bb = this.pack(r, ra);
         this.quad(x, y, a, x + ww, y, bb, x + ww, y + hh, bb, x, y + hh, a);
      }
   }

   public void quad(float x1, float y1, float c1, float x2, float y2, float c2, float x3, float y3, float c3, float x4, float y4, float c4) {
      if (!this.off) {
         this.nv = 0;
         this.ni = 0;
         this.v(x1, y1, c1);
         this.v(x2, y2, c2);
         this.v(x3, y3, c3);
         this.v(x4, y4, c4);
         this.ib[0] = 0;
         this.ib[1] = 1;
         this.ib[2] = 2;
         this.ib[3] = 0;
         this.ib[4] = 2;
         this.ib[5] = 3;
         this.ni = 6;
         this.flushPoly();
      }
   }

   public void tri(float x1, float y1, float x2, float y2, float x3, float y3, Color c, float a) {
      if (!this.off) {
         float f = this.pack(c, a);
         this.nv = 0;
         this.ni = 0;
         this.v(x1, y1, f);
         this.v(x2, y2, f);
         this.v(x3, y3, f);
         this.ib[0] = 0;
         this.ib[1] = 1;
         this.ib[2] = 2;
         this.ni = 3;
         this.flushPoly();
      }
   }

   public void poly(float[] xy, Color c, float a) {
      if (!this.off) {
         int n = xy.length / 2;
         float f = this.pack(c, a);
         this.nv = 0;
         this.ni = 0;

         for (int i = 0; i < n; i++) {
            this.v(xy[i * 2], xy[i * 2 + 1], f);
         }

         for (int i = 1; i < n - 1; i++) {
            this.ib[this.ni++] = 0;
            this.ib[this.ni++] = (short)i;
            this.ib[this.ni++] = (short)(i + 1);
         }

         this.flushPoly();
      }
   }

   public void line(float x1, float y1, float x2, float y2, float wd, Color c, float a) {
      if (!this.off) {
         float dx = x2 - x1;
         float dy = y2 - y1;
         float l = (float)Math.sqrt(dx * dx + dy * dy);
         if (!(l < 1.0E-4F)) {
            float nx = -dy / l * wd * 0.5F;
            float ny = dx / l * wd * 0.5F;
            float f = this.pack(c, a);
            this.quad(x1 + nx, y1 + ny, f, x2 + nx, y2 + ny, f, x2 - nx, y2 - ny, f, x1 - nx, y1 - ny, f);
         }
      }
   }

   public void polyline(float[] xy, boolean closed, float wd, Color c, float a) {
      int n = xy.length / 2;

      for (int i = 0; i < n - 1; i++) {
         this.line(xy[i * 2], xy[i * 2 + 1], xy[i * 2 + 2], xy[i * 2 + 3], wd, c, a);
      }

      if (closed) {
         this.line(xy[n * 2 - 2], xy[n * 2 - 1], xy[0], xy[1], wd, c, a);
      }
   }

   public void circle(float cx, float cy, float r, Color c, float a) {
      if (!this.off) {
         int seg = MathUtils.clamp((int)(r * 0.8F) + 10, 12, 40);
         float f = this.pack(c, a);
         this.nv = 0;
         this.ni = 0;
         this.v(cx, cy, f);

         for (int i = 0; i <= seg; i++) {
            float an = i * (float) (Math.PI * 2) / seg;
            this.v(cx + MathUtils.cos(an) * r, cy + MathUtils.sin(an) * r, f);
         }

         for (int i = 1; i <= seg; i++) {
            this.ib[this.ni++] = 0;
            this.ib[this.ni++] = (short)i;
            this.ib[this.ni++] = (short)(i + 1);
         }

         this.flushPoly();
      }
   }

   public void arc(float cx, float cy, float r, float a0, float a1, float wd, Color c, float a) {
      if (!this.off) {
         int seg = MathUtils.clamp((int)(Math.abs(a1 - a0) * r * 0.25F) + 6, 6, 60);
         float f = this.pack(c, a);
         float ri = r - wd * 0.5F;
         float ro = r + wd * 0.5F;

         for (int i = 0; i < seg; i++) {
            float t0 = a0 + (a1 - a0) * i / seg;
            float t1 = a0 + (a1 - a0) * (i + 1) / seg;
            float c0 = MathUtils.cos(t0);
            float s0 = MathUtils.sin(t0);
            float c1 = MathUtils.cos(t1);
            float s1 = MathUtils.sin(t1);
            this.quad(cx + c0 * ri, cy + s0 * ri, f, cx + c0 * ro, cy + s0 * ro, f, cx + c1 * ro, cy + s1 * ro, f, cx + c1 * ri, cy + s1 * ri, f);
         }
      }
   }

   public void ring(float cx, float cy, float r, float wd, Color c, float a) {
      this.arc(cx, cy, r, 0.0F, (float) (Math.PI * 2), wd, c, a);
   }

   public void frame(float x, float y, float ww, float hh, float wd, Color c, float a) {
      this.rect(x, y, ww, wd, c, a);
      this.rect(x, y + hh - wd, ww, wd, c, a);
      this.rect(x, y + wd, wd, hh - 2.0F * wd, c, a);
      this.rect(x + ww - wd, y + wd, wd, hh - 2.0F * wd, c, a);
   }

   public static float[] oct(float cx, float cy, float r) {
      float[] p = new float[16];

      for (int i = 0; i < 8; i++) {
         float an = (float) (Math.PI / 8) + i * (float) Math.PI / 4.0F;
         p[i * 2] = cx + MathUtils.cos(an) * r;
         p[i * 2 + 1] = cy + MathUtils.sin(an) * r;
      }

      return p;
   }

   public void star(float cx, float cy, float r, Color c, float a) {
      if (!this.off) {
         float f = this.pack(c, a);
         this.nv = 0;
         this.ni = 0;
         this.v(cx, cy, f);

         for (int i = 0; i <= 10; i++) {
            float an = (float) (-Math.PI / 2) + i * (float) Math.PI / 5.0F;
            float rr = i % 2 == 0 ? r : r * 0.45F;
            this.v(cx + MathUtils.cos(an) * rr, cy + MathUtils.sin(an) * rr, f);
         }

         for (int i = 1; i <= 10; i++) {
            this.ib[this.ni++] = 0;
            this.ib[this.ni++] = (short)i;
            this.ib[this.ni++] = (short)(i + 1);
         }

         this.flushPoly();
      }
   }

   public void check(float cx, float cy, float r, float wd, Color c, float a) {
      this.line(cx - r, cy, cx - r * 0.3F, cy + r * 0.7F, wd, c, a);
      this.line(cx - r * 0.3F - wd * 0.3F, cy + r * 0.7F + wd * 0.2F, cx + r, cy - r * 0.6F, wd, c, a);
   }

   public void tex(Texture t, float x, float y, float ww, float hh, float u2, float v2) {
      if (!this.off) {
         this.b.setColor(Color.WHITE);
         this.b.draw(t, x, y, ww, hh, 0.0F, 0.0F, u2, v2);
      }
   }

   public void clip(float x, float y, float ww, float hh) {
      if (!this.off) {
         this.b.flush();
         Gdx.gl.glEnable(3089);
         int sh = Gdx.graphics.getBackBufferHeight();
         float k = (float)Gdx.graphics.getBackBufferWidth() / Gdx.graphics.getWidth();
         int px = Math.round(x * this.s * k);
         int py = Math.round(sh - (y + hh) * this.s * k);
         Gdx.gl.glScissor(px, py, Math.max(0, Math.round(ww * this.s * k)), Math.max(0, Math.round(hh * this.s * k)));
      }
   }

   public void unclip() {
      if (!this.off) {
         this.b.flush();
         Gdx.gl.glDisable(3089);
      }
   }

   public void dispose() {
      this.b.dispose();
      this.white.dispose();

      for (BitmapFont f : this.fonts.values()) {
         f.dispose();
      }

      this.gens[0].dispose();
      this.gens[1].dispose();
      this.gens[2].dispose();
   }
}
