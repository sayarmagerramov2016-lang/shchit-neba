package ru.shitneba.gfx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;

public class Mat {
   public static final int OPAQUE = 0;
   public static final int ALPHA = 1;
   public static final int ADD = 2;
   public int blend = 0;
   public boolean cull = true;
   public boolean depthWrite = true;
   public boolean depthTest = true;
   public boolean fog = true;
   public boolean unshaded = false;
   public boolean vcol = true;
   public boolean shadowRecv = false;
   public boolean scissor = false;
   public boolean hull = false;
   public final Color color = new Color(1.0F, 1.0F, 1.0F, 1.0F);
   public Texture tex;
   public float uvx = 1.0F;
   public float uvy = 1.0F;
   public float rough = 0.85F;
   public float metal = 0.0F;
   public float spec = 0.5F;
   public float emission = 0.0F;
   public Texture camo;
   public float camoScale = 0.2F;
   public float panelScale = 0.45F;
   public float seams = 1.0F;
   public float dust = 1.0F;
   public float dustH = 1.7F;
   public float burnt = 0.0F;
   public float wear = 0.6F;
   public float alpha = 1.0F;
   public float distFadeMin = 0.0F;
   public float distFadeMax = 0.0F;
   public int order = 0;

   public Mat cpy() {
      Mat m = new Mat();
      m.blend = this.blend;
      m.cull = this.cull;
      m.depthWrite = this.depthWrite;
      m.depthTest = this.depthTest;
      m.fog = this.fog;
      m.unshaded = this.unshaded;
      m.vcol = this.vcol;
      m.shadowRecv = this.shadowRecv;
      m.scissor = this.scissor;
      m.hull = this.hull;
      m.color.set(this.color);
      m.tex = this.tex;
      m.uvx = this.uvx;
      m.uvy = this.uvy;
      m.rough = this.rough;
      m.metal = this.metal;
      m.spec = this.spec;
      m.emission = this.emission;
      m.camo = this.camo;
      m.camoScale = this.camoScale;
      m.panelScale = this.panelScale;
      m.seams = this.seams;
      m.dust = this.dust;
      m.dustH = this.dustH;
      m.burnt = this.burnt;
      m.wear = this.wear;
      m.alpha = this.alpha;
      m.distFadeMin = this.distFadeMin;
      m.distFadeMax = this.distFadeMax;
      m.order = this.order;
      return m;
   }

   public boolean transparent() {
      return this.blend != 0 || this.alpha < 0.999F;
   }

   public int key() {
      int k = 0;
      if (!this.unshaded) {
         k |= 1;
      }

      if (this.vcol) {
         k |= 2;
      }

      if (this.tex != null) {
         k |= 4;
      }

      if (this.hull) {
         k |= 8;
      }

      if (this.fog) {
         k |= 16;
      }

      if (this.scissor) {
         k |= 32;
      }

      if (this.shadowRecv) {
         k |= 64;
      }

      if (this.blend == 2) {
         k |= 128;
      }

      if (this.distFadeMax > 0.0F) {
         k |= 256;
      }

      return k;
   }

   public static Mat vc() {
      Mat m = new Mat();
      m.rough = 0.85F;
      return m;
   }

   public static Mat glow() {
      Mat m = new Mat();
      m.unshaded = true;
      return m;
   }

   public static Mat std(Color c) {
      Mat m = new Mat();
      m.vcol = false;
      m.color.set(c);
      m.rough = 1.0F;
      return m;
   }

   public static Mat unshadedAdd(Color c) {
      Mat m = new Mat();
      m.unshaded = true;
      m.vcol = false;
      m.blend = 2;
      m.color.set(c);
      m.depthWrite = false;
      m.cull = false;
      return m;
   }
}
