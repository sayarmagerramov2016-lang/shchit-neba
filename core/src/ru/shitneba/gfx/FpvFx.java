package ru.shitneba.gfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;

public class FpvFx implements Disposable {
   Mesh quad;
   ShaderProgram prog;
   boolean failed;
   static final String VS = "attribute vec2 a_position;\nvarying vec2 v_uv;\nvoid main(){ v_uv = a_position * 0.5 + 0.5; gl_Position = vec4(a_position, 0.0, 1.0); }";
   static final String FS = "#ifdef GL_ES\n#ifdef GL_FRAGMENT_PRECISION_HIGH\nprecision highp float;\n#else\nprecision mediump float;\n#endif\n#endif\nvarying vec2 v_uv;\nuniform float u_noise; uniform float u_t; uniform float u_hit; uniform float u_fpv;\nfloat h(vec2 p) { return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453); }\nvoid main() {\n vec2 px = gl_FragCoord.xy;\n float n = h(floor(px / 2.0) + vec2(floor(u_t * 50.0) * 7.31, floor(u_t * 50.0) * 3.17));\n float band = h(vec2(floor(px.y / 4.0), floor(u_t * 20.0)));\n float line = step(1.0 - u_noise * 0.22, band);\n float roll = smoothstep(0.0, 0.04, abs(fract(1.0 - v_uv.y + u_t * 0.35) - 0.5));\n float scan = 0.5 + 0.5 * sin(px.y * 1.7);\n float vig = smoothstep(0.42, 0.98, length((v_uv - 0.5) * vec2(1.25, 1.0)));\n float st = u_noise * (0.35 + 0.65 * n) * (0.55 + line * 0.9);\n vec3 col = vec3(n) * st;\n float a = st * 0.85 + scan * 0.05 * u_fpv + vig * 0.6 * u_fpv + (1.0 - roll) * u_noise * 0.25 * u_fpv;\n col += vec3(u_hit, u_hit * 0.1, 0.0) * 0.6;\n gl_FragColor = vec4(col, clamp(a + u_hit * 0.35, 0.0, 0.92));\n}";

   public void draw(float noise, float t, float hit, float fpv) {
      if (!this.failed) {
         if (this.prog == null) {
            this.prog = new ShaderProgram(
               "attribute vec2 a_position;\nvarying vec2 v_uv;\nvoid main(){ v_uv = a_position * 0.5 + 0.5; gl_Position = vec4(a_position, 0.0, 1.0); }",
               "#ifdef GL_ES\n#ifdef GL_FRAGMENT_PRECISION_HIGH\nprecision highp float;\n#else\nprecision mediump float;\n#endif\n#endif\nvarying vec2 v_uv;\nuniform float u_noise; uniform float u_t; uniform float u_hit; uniform float u_fpv;\nfloat h(vec2 p) { return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453); }\nvoid main() {\n vec2 px = gl_FragCoord.xy;\n float n = h(floor(px / 2.0) + vec2(floor(u_t * 50.0) * 7.31, floor(u_t * 50.0) * 3.17));\n float band = h(vec2(floor(px.y / 4.0), floor(u_t * 20.0)));\n float line = step(1.0 - u_noise * 0.22, band);\n float roll = smoothstep(0.0, 0.04, abs(fract(1.0 - v_uv.y + u_t * 0.35) - 0.5));\n float scan = 0.5 + 0.5 * sin(px.y * 1.7);\n float vig = smoothstep(0.42, 0.98, length((v_uv - 0.5) * vec2(1.25, 1.0)));\n float st = u_noise * (0.35 + 0.65 * n) * (0.55 + line * 0.9);\n vec3 col = vec3(n) * st;\n float a = st * 0.85 + scan * 0.05 * u_fpv + vig * 0.6 * u_fpv + (1.0 - roll) * u_noise * 0.25 * u_fpv;\n col += vec3(u_hit, u_hit * 0.1, 0.0) * 0.6;\n gl_FragColor = vec4(col, clamp(a + u_hit * 0.35, 0.0, 0.92));\n}"
            );
            if (!this.prog.isCompiled()) {
               Gdx.app.error("fpv", this.prog.getLog());
               this.failed = true;
               return;
            }

            this.quad = new Mesh(true, 4, 6, new VertexAttribute[]{new VertexAttribute(1, 2, "a_position")});
            this.quad.setVertices(new float[]{-1.0F, -1.0F, 1.0F, -1.0F, 1.0F, 1.0F, -1.0F, 1.0F});
            this.quad.setIndices(new short[]{0, 1, 2, 0, 2, 3});
         }

         if (!(noise <= 0.001F) || !(fpv <= 0.0F) || !(hit <= 0.0F)) {
            GL20 gl = Gdx.gl;
            gl.glDisable(2929);
            gl.glDisable(2884);
            gl.glEnable(3042);
            gl.glBlendFunc(770, 771);
            this.prog.bind();
            this.prog.setUniformf("u_noise", noise);
            this.prog.setUniformf("u_t", t);
            this.prog.setUniformf("u_hit", hit);
            this.prog.setUniformf("u_fpv", fpv);
            this.quad.render(this.prog, 4);
            gl.glDisable(3042);
         }
      }
   }

   public void dispose() {
      if (this.prog != null) {
         this.prog.dispose();
      }

      if (this.quad != null) {
         this.quad.dispose();
      }
   }
}
