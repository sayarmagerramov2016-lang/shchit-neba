package ru.shitneba.gfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer.FrameBufferBuilder;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Sort;
import java.util.Comparator;

public class Renderer implements Disposable {
   public final Shaders sh = new Shaders();
   public final Renderer.Env env = new Renderer.Env();
   public Camera cam;
   public boolean shadows = true;
   public final Vector3 shadowFocus = new Vector3();
   public float shadowRange = 70.0F;
   public float scale = 1.0F;
   public int shadowEvery = 1;
   int frame;
   public boolean dumpNext;
   public int statOpq;
   public int statTrn;
   public int statCast;
   public int statTris;
   FrameBuffer sceneFbo;
   Mesh blitQuad;
   ShaderProgram blitProg;
   final Mesh skyQuad;
   FrameBuffer shadowFbo;
   final OrthographicCamera sunCam = new OrthographicCamera();
   final Matrix4 svp = new Matrix4();
   final Matrix3 nmat = new Matrix3();
   final Vector3 tv = new Vector3();
   final Vector3 tv2 = new Vector3();
   final Matrix4 ivp = new Matrix4();
   final Matrix4 ident = new Matrix4();
   final Array<Renderer.Item> pool = new Array();
   final Array<Renderer.Item> opaque = new Array();
   final Array<Renderer.Item> trans = new Array();
   int poolUsed;
   final Array<Node3> casters = new Array();
   ShaderProgram curProg;
   Mat curMat;
   Texture panelTex;
   static final Comparator<Renderer.Item> OPQ = (a, b) -> Integer.compare(a.key, b.key);
   static final Comparator<Renderer.Item> TRN = (a, b) -> a.m.order != b.m.order ? Integer.compare(a.m.order, b.m.order) : Float.compare(b.d, a.d);
   boolean sceneFailed;

   public Renderer() {
      this.skyQuad = new Mesh(true, 4, 6, new VertexAttribute[]{new VertexAttribute(1, 3, "a_position")});
      this.skyQuad.setVertices(new float[]{-1.0F, -1.0F, 0.0F, 1.0F, -1.0F, 0.0F, 1.0F, 1.0F, 0.0F, -1.0F, 1.0F, 0.0F});
      this.skyQuad.setIndices(new short[]{0, 1, 2, 0, 2, 3});
   }

   Renderer.Item item() {
      if (this.poolUsed >= this.pool.size) {
         this.pool.add(new Renderer.Item());
      }

      return (Renderer.Item)this.pool.get(this.poolUsed++);
   }

   static Vector3 lin(Color c, Vector3 out) {
      return out.set((float)Math.pow(c.r, 2.2), (float)Math.pow(c.g, 2.2), (float)Math.pow(c.b, 2.2));
   }

   void setEnv(ShaderProgram p) {
      p.setUniformMatrix("u_vp", this.cam.combined);
      p.setUniformf("u_cam", this.cam.position);
      p.setUniformf("u_sunDir", this.env.sunDir);
      lin(this.env.sunCol, this.tv).scl(this.env.sunEnergy);
      p.setUniformf("u_sunCol", this.tv);
      lin(this.env.amb, this.tv).scl(this.env.ambEnergy * (1.0F + this.env.flash * 8.0F));
      p.setUniformf("u_amb", this.tv);
      lin(this.env.fogCol, this.tv);
      p.setUniformf("u_fogCol", this.tv);
      p.setUniformf("u_fogD", this.env.fogD);
      p.setUniformf("u_post", this.env.exposure, this.env.contrast, this.env.saturation);
   }

   void bindMat(Mat m) {
      int key = m.key();
      ShaderProgram p = this.sh.get(key);
      if (p != this.curProg) {
         p.bind();
         this.curProg = p;
         this.setEnv(p);
         if ((key & 64) != 0 && this.shadowFbo != null) {
            ((Texture)this.shadowFbo.getColorBufferTexture()).bind(3);
            p.setUniformi("u_shadow", 3);
            p.setUniformMatrix("u_svp", this.svp);
         }

         this.curMat = null;
      }

      if (m != this.curMat) {
         this.curMat = m;
         GL20 gl = Gdx.gl;
         p.setUniformf("u_color", m.color.r, m.color.g, m.color.b, m.color.a);
         p.setUniformf("u_uvs", m.uvx, m.uvy);
         p.setUniformf("u_m", m.rough, m.metal, m.spec, m.emission);
         p.setUniformf("u_h3", m.burnt, m.wear, 0.0F, m.alpha);
         if (m.distFadeMax > 0.0F) {
            p.setUniformf("u_fade", m.distFadeMin, m.distFadeMax);
         }

         if (m.tex != null) {
            m.tex.bind(0);
            p.setUniformi("u_tex", 0);
         }

         if (m.hull) {
            if (this.panelTex == null) {
               this.panelTex = Tex.panel();
            }

            this.panelTex.bind(2);
            p.setUniformi("u_panel", 2);
            if (m.camo != null) {
               m.camo.bind(1);
               p.setUniformi("u_camo", 1);
            }

            p.setUniformf("u_h1", m.camoScale, m.panelScale, m.seams, m.camo != null ? 1.0F : 0.0F);
            p.setUniformf("u_h2", m.rough, m.metal, m.dust, m.dustH);
         }

         gl.glActiveTexture(33984);
         if (m.cull) {
            gl.glEnable(2884);
         } else {
            gl.glDisable(2884);
         }

         gl.glDepthMask(m.depthWrite);
         if (m.depthTest) {
            gl.glEnable(2929);
         } else {
            gl.glDisable(2929);
         }

         if (m.blend == 0 && m.alpha >= 0.999F) {
            gl.glDisable(3042);
         } else {
            gl.glEnable(3042);
            if (m.blend == 2) {
               gl.glBlendFunc(1, 1);
            } else {
               gl.glBlendFunc(770, 771);
            }
         }
      }
   }

   void drawNode(Node3 n, Mat m) {
      this.bindMat(m);
      if (n.alpha < 0.999F) {
         this.curProg.setUniformf("u_h3", m.burnt, m.wear, 0.0F, m.alpha * n.alpha);
      }

      this.curProg.setUniformMatrix("u_world", n.global);
      this.nmat.set(n.global).inv().transpose();
      this.curProg.setUniformMatrix("u_nmat", this.nmat);
      n.mesh.mesh.render(this.curProg, 4, 0, n.mesh.count);
      if (n.alpha < 0.999F) {
         this.curMat = null;
      }
   }

   public void drawDyn(Dyn d) {
      if (d.count() != 0) {
         this.bindMat(d.mat);
         this.curProg.setUniformMatrix("u_world", this.ident);
         this.nmat.idt();
         this.curProg.setUniformMatrix("u_nmat", this.nmat);
         d.flushTo(this.curProg);
      }
   }

   void collect(Node3 n, boolean shadow) {
      if (n.visible) {
         if (n.mesh != null && n.mat != null && n.mesh.count > 0) {
            this.tv.set(n.mesh.center).mul(n.global);
            float s = Math.max(Math.abs(n.global.getScaleX()), Math.max(Math.abs(n.global.getScaleY()), Math.abs(n.global.getScaleZ())));
            if (this.cam.frustum.sphereInFrustum(this.tv, n.mesh.radius * s + 0.5F)) {
               Renderer.Item it = this.item();
               it.n = n;
               it.m = n.mat;
               it.key = n.mat.key() * 4 + n.mat.order;
               it.d = this.tv.dst2(this.cam.position);
               if (!n.mat.transparent() && !(n.alpha < 0.999F)) {
                  this.opaque.add(it);
               } else {
                  this.trans.add(it);
               }

               if (n.overlay != null) {
                  Renderer.Item o = this.item();
                  o.n = n;
                  o.m = n.overlay;
                  o.d = it.d - 0.01F;
                  this.trans.add(o);
               }
            }

            if (shadow && n.castShadow) {
               this.casters.add(n);
            }
         }

         for (int i = 0; i < n.kids.size; i++) {
            this.collect((Node3)n.kids.get(i), shadow);
         }
      }
   }

   public void render(Array<Node3> roots, Array<Dyn> dyns) {
      GL20 gl = Gdx.gl;
      this.poolUsed = 0;
      this.opaque.clear();
      this.trans.clear();
      this.casters.clear();

      for (int i = 0; i < roots.size; i++) {
         ((Node3)roots.get(i)).updateGlobal(null);
      }

      for (int i = 0; i < roots.size; i++) {
         this.collect((Node3)roots.get(i), this.shadows);
      }

      this.frame++;
      this.statOpq = this.opaque.size;
      this.statTrn = this.trans.size;
      this.statCast = this.casters.size;
      this.statTris = 0;
      if (this.dumpNext) {
         this.dumpNext = false;

         for (int i = 0; i < this.opaque.size; i++) {
            Node3 n = ((Renderer.Item)this.opaque.get(i)).n;
            Gdx.app.log("node", n.mesh.count / 3 + " cast=" + n.castShadow + " key=" + ((Renderer.Item)this.opaque.get(i)).m.key() + " r=" + n.mesh.radius);
         }
      }

      for (int i = 0; i < this.opaque.size; i++) {
         this.statTris = this.statTris + ((Renderer.Item)this.opaque.get(i)).n.mesh.count / 3;
      }

      for (int i = 0; i < this.casters.size; i++) {
         this.statTris = this.statTris + ((Node3)this.casters.get(i)).mesh.count / 3;
      }

      if (this.shadows && (this.shadowFbo == null || this.frame % Math.max(1, this.shadowEvery) == 0)) {
         this.renderShadow();
      }

      int bw = Gdx.graphics.getBackBufferWidth();
      int bh = Gdx.graphics.getBackBufferHeight();
      boolean scaled = this.scale < 0.98F && this.ensureScene(Math.max(64, Math.round(bw * this.scale)), Math.max(64, Math.round(bh * this.scale)));
      if (scaled) {
         this.sceneFbo.begin();
         gl.glViewport(0, 0, this.sceneFbo.getWidth(), this.sceneFbo.getHeight());
      } else {
         gl.glViewport(0, 0, bw, bh);
      }

      gl.glClearColor(0.0F, 0.0F, 0.0F, 1.0F);
      gl.glDepthMask(true);
      gl.glClear(16640);
      gl.glFrontFace(2304);
      gl.glCullFace(1029);
      gl.glDepthFunc(515);
      this.drawSky();
      this.curProg = null;
      this.curMat = null;
      Sort.instance().sort(this.opaque, OPQ);

      for (int i = 0; i < this.opaque.size; i++) {
         Renderer.Item it = (Renderer.Item)this.opaque.get(i);
         this.drawNode(it.n, it.m);
      }

      if (dyns != null) {
         for (int i = 0; i < dyns.size; i++) {
            if (!((Dyn)dyns.get(i)).mat.transparent()) {
               this.drawDyn((Dyn)dyns.get(i));
            }
         }
      }

      Sort.instance().sort(this.trans, TRN);

      for (int ix = 0; ix < this.trans.size; ix++) {
         Renderer.Item it = (Renderer.Item)this.trans.get(ix);
         this.drawNode(it.n, it.m);
      }

      if (dyns != null) {
         for (int ix = 0; ix < dyns.size; ix++) {
            if (((Dyn)dyns.get(ix)).mat.transparent()) {
               this.drawDyn((Dyn)dyns.get(ix));
            }
         }
      }

      gl.glDepthMask(true);
      gl.glDisable(3042);
      gl.glDisable(2884);
      gl.glFrontFace(2305);
      gl.glDisable(2929);
      this.curProg = null;
      this.curMat = null;
      if (scaled) {
         this.sceneFbo.end();
         gl.glViewport(0, 0, bw, bh);
         this.blit();
      }
   }

   boolean ensureScene(int w, int h) {
      if (this.sceneFailed) {
         return false;
      } else if (this.sceneFbo != null && this.sceneFbo.getWidth() == w && this.sceneFbo.getHeight() == h) {
         return true;
      } else {
         if (this.sceneFbo != null) {
            this.sceneFbo.dispose();
            this.sceneFbo = null;
         }

         try {
            FrameBufferBuilder b = new FrameBufferBuilder(w, h);
            b.addBasicColorTextureAttachment(Format.RGB888);
            b.addDepthRenderBuffer(33190);
            this.sceneFbo = b.build();
         } catch (Throwable var6) {
            try {
               this.sceneFbo = new FrameBuffer(Format.RGB888, w, h, true);
            } catch (Throwable var5) {
               this.sceneFailed = true;
               this.sceneFbo = null;
               return false;
            }
         }

         ((Texture)this.sceneFbo.getColorBufferTexture()).setFilter(TextureFilter.Linear, TextureFilter.Linear);
         if (this.blitQuad == null) {
            this.blitQuad = new Mesh(true, 4, 6, new VertexAttribute[]{new VertexAttribute(1, 2, "a_position")});
            this.blitQuad.setVertices(new float[]{-1.0F, -1.0F, 1.0F, -1.0F, 1.0F, 1.0F, -1.0F, 1.0F});
            this.blitQuad.setIndices(new short[]{0, 1, 2, 0, 2, 3});
            String vs = "attribute vec2 a_position;\nvarying vec2 v_uv;\nvoid main(){ v_uv = a_position * 0.5 + 0.5; gl_Position = vec4(a_position, 0.0, 1.0); }";
            String fs = "#ifdef GL_ES\nprecision mediump float;\n#endif\nvarying vec2 v_uv;\nuniform sampler2D u_tex;\nvoid main(){ gl_FragColor = vec4(texture2D(u_tex, v_uv).rgb, 1.0); }";
            this.blitProg = new ShaderProgram(vs, fs);
            if (!this.blitProg.isCompiled()) {
               Gdx.app.error("blit", this.blitProg.getLog());
               this.sceneFailed = true;
               return false;
            }
         }

         return true;
      }
   }

   void blit() {
      GL20 gl = Gdx.gl;
      gl.glDisable(2929);
      gl.glDisable(3042);
      gl.glDisable(2884);
      this.blitProg.bind();
      ((Texture)this.sceneFbo.getColorBufferTexture()).bind(0);
      this.blitProg.setUniformi("u_tex", 0);
      this.blitQuad.render(this.blitProg, 4);
   }

   void drawSky() {
      GL20 gl = Gdx.gl;
      gl.glDisable(2929);
      gl.glDepthMask(false);
      gl.glDisable(2884);
      gl.glDisable(3042);
      ShaderProgram p = this.sh.sky;
      p.bind();
      this.ivp.set(this.cam.combined).inv();
      p.setUniformMatrix("u_ivp", this.ivp);
      p.setUniformf("u_cam", this.cam.position);

      for (int i = 0; i < 4; i++) {
         lin(this.env.sky[i], this.tv);
         p.setUniformf("u_c" + i, this.tv);
      }

      p.setUniformf("u_sunDir", this.env.sunDir);
      lin(this.env.skySun, this.tv);
      p.setUniformf("u_sunCol", this.tv);
      p.setUniformf("u_sunOn", this.env.sunOn);
      lin(this.env.fogCol, this.tv);
      p.setUniformf("u_fogCol", this.tv);
      p.setUniformf("u_skyFog", this.env.skyFog);
      p.setUniformf("u_flash", this.env.flash * 3.0F);
      p.setUniformf("u_post", this.env.exposure, this.env.contrast, this.env.saturation);
      this.skyQuad.render(p, 4);
      gl.glDepthMask(true);
      gl.glEnable(2929);
   }

   void renderShadow() {
      if (this.shadowFbo == null) {
         try {
            this.shadowFbo = new FrameBuffer(Format.RGBA8888, 1024, 1024, true);
            ((Texture)this.shadowFbo.getColorBufferTexture()).setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
         } catch (Throwable var6) {
            this.shadows = false;
            return;
         }
      }

      float R = this.shadowRange;
      this.sunCam.viewportWidth = R * 2.0F;
      this.sunCam.viewportHeight = R * 2.0F;
      this.sunCam.near = 1.0F;
      this.sunCam.far = 400.0F;
      this.sunCam.position.set(this.shadowFocus).mulAdd(this.env.sunDir, 200.0F);
      this.sunCam.direction.set(this.env.sunDir).scl(-1.0F);
      this.sunCam.up.set(Math.abs(this.env.sunDir.y) > 0.95F ? Vector3.Z : Vector3.Y);
      this.sunCam.update();
      this.svp.set(this.sunCam.combined);
      this.shadowFbo.begin();
      GL20 gl = Gdx.gl;
      gl.glClearColor(1.0F, 1.0F, 1.0F, 1.0F);
      gl.glClear(16640);
      gl.glEnable(2929);
      gl.glDepthMask(true);
      gl.glDisable(3042);
      gl.glDisable(2884);
      ShaderProgram p = this.sh.depth;
      p.bind();
      p.setUniformMatrix("u_vp", this.svp);

      for (int i = 0; i < this.casters.size; i++) {
         Node3 n = (Node3)this.casters.get(i);
         p.setUniformMatrix("u_world", n.global);
         n.mesh.mesh.render(p, 4, 0, n.mesh.count);
      }

      this.shadowFbo.end();
   }

   public void dispose() {
      this.sh.dispose();
      this.skyQuad.dispose();
      if (this.shadowFbo != null) {
         this.shadowFbo.dispose();
      }

      if (this.sceneFbo != null) {
         this.sceneFbo.dispose();
      }

      if (this.blitQuad != null) {
         this.blitQuad.dispose();
      }

      if (this.blitProg != null) {
         this.blitProg.dispose();
      }
   }

   public static final class Env {
      public final Vector3 sunDir = new Vector3(0.3F, 0.5F, -0.8F).nor();
      public final Color sunCol = new Color(1.0F, 1.0F, 1.0F, 1.0F);
      public float sunEnergy = 1.45F;
      public final Color amb = new Color(0.5F, 0.5F, 0.6F, 1.0F);
      public float ambEnergy = 0.95F;
      public final Color fogCol = new Color(0.6F, 0.6F, 0.7F, 1.0F);
      public float fogD = 4.2E-4F;
      public float exposure = 1.0F;
      public float contrast = 1.07F;
      public float saturation = 1.08F;
      public final Color[] sky = new Color[]{new Color(), new Color(), new Color(), new Color()};
      public final Color skySun = new Color(1.0F, 0.8F, 0.7F, 1.0F);
      public float sunOn = 1.0F;
      public float skyFog = 0.15F;
      public float flash = 0.0F;
   }

   static final class Item {
      Node3 n;
      Mat m;
      float d;
      int key;
   }
}
