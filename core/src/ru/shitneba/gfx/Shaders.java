package ru.shitneba.gfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.IntMap;

/** Uber-шейдер: освещение солнцем + амбиент, туман Godot, ACES-тонмаппинг, hull-текстуры. */
public class Shaders implements Disposable {
    final IntMap<ShaderProgram> progs = new IntMap<>();
    public ShaderProgram sky, depth;

    static final String HEAD = "#ifdef GL_ES\n#ifdef GL_FRAGMENT_PRECISION_HIGH\nprecision highp float;\n#else\nprecision mediump float;\n#endif\n#endif\n";

    static final String VS =
            "attribute vec3 a_position; attribute vec3 a_normal; attribute vec4 a_color; attribute vec2 a_texCoord0;\n" +
            "uniform mat4 u_vp; uniform mat4 u_world; uniform mat3 u_nmat; uniform vec2 u_uvs;\n" +
            "#ifdef SHADOW\nuniform mat4 u_svp; varying vec4 v_sh;\n#endif\n" +
            "varying vec3 v_wpos; varying vec3 v_n; varying vec4 v_col; varying vec2 v_uv; varying vec3 v_mp; varying vec3 v_mn;\n" +
            "void main(){ vec4 wp = u_world*vec4(a_position,1.0); v_wpos = wp.xyz; v_n = normalize(u_nmat*a_normal); v_col = a_color;\n" +
            " v_uv = a_texCoord0*u_uvs; v_mp = a_position; v_mn = a_normal;\n" +
            "#ifdef SHADOW\n v_sh = u_svp*wp;\n#endif\n" +
            " gl_Position = u_vp*wp; }\n";

    static final String TONE =
            "vec3 aces(vec3 c){ const float A=0.0245786; const float B=0.000090537; const float C=0.983729; const float D=0.432951; const float E=0.238081;\n" +
            " mat3 rrt = mat3(vec3(0.59719,0.07600,0.02840), vec3(0.35458,0.90834,0.13383), vec3(0.04823,0.01566,0.83777));\n" +
            " mat3 odt = mat3(vec3(1.60475,-0.10208,-0.00327), vec3(-0.53108,1.10813,-0.07276), vec3(-0.07367,-0.00605,1.07602));\n" +
            " c *= 1.8; c = rrt*c; c = (c*(c+A)-B)/(c*(C*c+D)+E); c = odt*c;\n" +
            " vec3 w = rrt*vec3(1.8); w = (w*(w+A)-B)/(w*(C*w+D)+E); w = odt*w; return c/w; }\n" +
            "uniform vec3 u_post; // exposure, contrast, saturation\n" +
            "vec3 finish(vec3 c){ c = aces(max(c,vec3(0.0))*u_post.x); c = clamp(c,0.0,1.0); c = pow(c, vec3(1.0/2.2));\n" +
            " c = mix(vec3(0.5), c, u_post.y); float l = dot(c, vec3(0.299,0.587,0.114)); c = mix(vec3(l), c, u_post.z); return clamp(c,0.0,1.0); }\n";

    static final String FS =
            "uniform vec4 u_color; uniform sampler2D u_tex; uniform sampler2D u_camo; uniform sampler2D u_panel; uniform sampler2D u_shadow;\n" +
            "uniform vec3 u_cam; uniform vec3 u_sunDir; uniform vec3 u_sunCol; uniform vec3 u_amb; uniform vec3 u_fogCol; uniform float u_fogD;\n" +
            "uniform vec4 u_h1; uniform vec4 u_h2; uniform vec4 u_h3; uniform vec4 u_m; uniform vec2 u_fade;\n" +
            "uniform vec4 u_pl[4]; uniform vec3 u_plc[4]; uniform float u_plN;\n" +
            "varying vec3 v_wpos; varying vec3 v_n; varying vec4 v_col; varying vec2 v_uv; varying vec3 v_mp; varying vec3 v_mn;\n" +
            "#ifdef SHADOW\nvarying vec4 v_sh;\n" +
            "float unpack(vec4 c){ return dot(c, vec4(1.0, 1.0/255.0, 1.0/65025.0, 1.0/16581375.0)); }\n" +
            "float shadowK(){ vec3 p = v_sh.xyz/v_sh.w*0.5+0.5; if(p.x<0.0||p.x>1.0||p.y<0.0||p.y>1.0||p.z>1.0) return 1.0;\n" +
            " float s=0.0; float o=1.0/1024.0; float z=p.z-0.0025;\n" +
            " s += step(z, unpack(texture2D(u_shadow, p.xy+vec2(-o,-o))));\n" +
            " s += step(z, unpack(texture2D(u_shadow, p.xy+vec2(o,-o))));\n" +
            " s += step(z, unpack(texture2D(u_shadow, p.xy+vec2(-o,o))));\n" +
            " s += step(z, unpack(texture2D(u_shadow, p.xy+vec2(o,o))));\n" +
            " return s*0.25; }\n#endif\n" +
            "#ifdef HULL\nvec4 tri3(sampler2D t, vec3 p, vec3 w){ return texture2D(t,p.zy)*w.x + texture2D(t,p.xz)*w.y + texture2D(t,p.xy)*w.z; }\n#endif\n" +
            TONE +
            "void main(){\n" +
            " vec4 base = u_color;\n" +
            "#ifdef TEX\n vec4 t = texture2D(u_tex, v_uv); base *= vec4(t.rgb, t.a);\n#endif\n" +
            " vec3 col = pow(base.rgb, vec3(2.2)); float alpha = base.a;\n" +
            "#ifdef VCOL\n col *= v_col.rgb; alpha *= v_col.a;\n#endif\n" +
            "#ifdef SCISSOR\n if(alpha < 0.45) discard; alpha = 1.0;\n#endif\n" +
            " float rough = u_m.x; float metal = u_m.y;\n" +
            "#ifdef HULL\n vec3 w = pow(abs(v_mn), vec3(4.0)); w /= (w.x+w.y+w.z);\n" +
            " if (u_h1.w > 0.5) col *= pow(tri3(u_camo, v_mp*u_h1.x, w).rgb, vec3(2.2));\n" +
            " vec4 pn = tri3(u_panel, v_mp*u_h1.y, w); float g = pn.g;\n" +
            " col *= mix(1.0, mix(0.55,1.0,pn.r), u_h1.z); col *= 0.9 + 0.2*g;\n" +
            " col = mix(col, col*1.35+0.08, pn.b*u_h3.y*0.6);\n" +
            " float low = clamp(1.0 - v_mp.y/u_h2.w, 0.0, 1.0)*u_h2.z; vec3 dirt = vec3(0.36,0.31,0.24);\n" +
            " col = mix(col, dirt, clamp(low*(0.35+0.5*g) + g*0.12*u_h2.z, 0.0, 0.85));\n" +
            " float top = clamp(v_mn.y, 0.0, 1.0)*u_h2.z; col = mix(col, dirt*1.15, top*g*0.25);\n" +
            " col = mix(col, vec3(0.045,0.04,0.035)+g*0.03, u_h3.x);\n" +
            " rough = clamp(u_h2.x + g*0.25 + low*0.25 - pn.b*u_h3.y*0.2 + u_h3.x, 0.05, 1.0);\n" +
            " metal = clamp(u_h2.y + pn.b*u_h3.y*0.5, 0.0, 1.0)*(1.0-u_h3.x);\n#endif\n" +
            " vec3 lit;\n" +
            "#ifdef LIT\n vec3 n = normalize(v_n); if(!gl_FrontFacing) n = -n;\n" +
            " float ndl = max(dot(n, u_sunDir), 0.0); float sh = 1.0;\n" +
            "#ifdef SHADOW\n sh = shadowK();\n#endif\n" +
            " vec3 V = normalize(u_cam - v_wpos); vec3 H = normalize(V + u_sunDir);\n" +
            " float sp = exp2(10.0*(1.0-rough)+1.0); float spec = pow(max(dot(n,H),0.0), sp)*(sp+8.0)/25.0*u_m.z;\n" +
            " vec3 diff = col*(1.0-metal); vec3 F0 = mix(vec3(0.04), col, metal);\n" +
            " float hemi = 0.75 + 0.25*n.y;\n" +
            " lit = diff*(u_amb*hemi + u_sunCol*ndl*sh) + F0*spec*u_sunCol*ndl*sh*(1.0-rough*0.7) + col*metal*u_amb*0.55;\n" +
            " if (u_plN > 0.5) { for (int i = 0; i < 4; i++) { vec3 L = u_pl[i].xyz - v_wpos; float dl = length(L);\n" +
            "  float at = clamp(1.0 - dl/u_pl[i].w, 0.0, 1.0); at *= at;\n" +
            "  lit += (diff + F0*0.5)*u_plc[i]*at*(0.25 + 0.75*max(dot(n, L/max(dl,0.001)), 0.0)); } }\n" +
            "#else\n lit = col;\n#endif\n" +
            " lit += col*u_m.w;\n" +
            "#ifdef FOG\n float fd = length(u_cam - v_wpos); float f = 1.0 - exp(-u_fogD*fd);\n" +
            "#ifdef ADD\n lit *= (1.0-f);\n#else\n lit = mix(lit, u_fogCol, f);\n#endif\n#endif\n" +
            "#ifdef DFADE\n alpha *= clamp((length(u_cam - v_wpos)-u_fade.x)/max(0.01,u_fade.y-u_fade.x), 0.0, 1.0);\n#endif\n" +
            " alpha *= u_h3.w;\n" +
            "#ifdef ADD\n gl_FragColor = vec4(finish(lit)*alpha, alpha);\n#else\n gl_FragColor = vec4(finish(lit), alpha);\n#endif\n" +
            "}\n";

    static final String SKY_VS = "attribute vec3 a_position; varying vec2 v_p; void main(){ v_p = a_position.xy; gl_Position = vec4(a_position.xy, 0.9999, 1.0); }\n";

    static final String SKY_FS =
            "uniform mat4 u_ivp; uniform vec3 u_cam; uniform vec3 u_c0; uniform vec3 u_c1; uniform vec3 u_c2; uniform vec3 u_c3;\n" +
            "uniform vec3 u_sunDir; uniform vec3 u_sunCol; uniform float u_sunOn; uniform vec3 u_fogCol; uniform float u_skyFog; uniform float u_flash;\n" +
            "varying vec2 v_p;\n" + TONE +
            "void main(){ vec4 a = u_ivp*vec4(v_p, 1.0, 1.0); vec3 d = normalize(a.xyz/a.w - u_cam);\n" +
            " float y = d.y; vec3 col;\n" +
            " if (y > 0.45) col = mix(u_c1, u_c0, smoothstep(0.45,1.0,y));\n" +
            " else if (y > 0.12) col = mix(u_c2, u_c1, smoothstep(0.12,0.45,y));\n" +
            " else if (y > 0.0) col = mix(u_c3, u_c2, smoothstep(0.0,0.12,y));\n" +
            " else col = u_c3*(1.0+y*2.0);\n" +
            " float sd = max(dot(d, normalize(u_sunDir)), 0.0);\n" +
            " col += u_sunCol*u_sunOn*(pow(sd,6.0)*0.35 + pow(sd,60.0)*0.5 + smoothstep(0.9993,0.9996,sd)*4.0);\n" +
            " col = mix(col, u_fogCol, u_skyFog); col *= 1.0 + u_flash;\n" +
            " gl_FragColor = vec4(finish(col), 1.0); }\n";

    static final String DEPTH_VS = "attribute vec3 a_position; uniform mat4 u_vp; uniform mat4 u_world; varying float v_z;\n" +
            "void main(){ vec4 p = u_vp*u_world*vec4(a_position,1.0); v_z = p.z/p.w*0.5+0.5; gl_Position = p; }\n";
    static final String DEPTH_FS = "varying float v_z;\n" +
            "vec4 pack(float d){ vec4 e = vec4(1.0,255.0,65025.0,16581375.0)*d; e = fract(e); e -= e.yzww*vec4(1.0/255.0,1.0/255.0,1.0/255.0,0.0); return e; }\n" +
            "void main(){ gl_FragColor = pack(clamp(v_z,0.0,0.99999)); }\n";

    public Shaders() {
        ShaderProgram.pedantic = false;
        sky = compile(SKY_VS, SKY_FS, "");
        depth = compile(DEPTH_VS, DEPTH_FS, "");
    }

    static ShaderProgram compile(String vs, String fs, String defs) {
        ShaderProgram p = new ShaderProgram(HEAD + defs + vs, HEAD + defs + fs);
        if (!p.isCompiled()) {
            Gdx.app.error("shader", p.getLog());
            throw new RuntimeException("Shader: " + p.getLog());
        }
        return p;
    }

    public ShaderProgram get(int key) {
        ShaderProgram p = progs.get(key);
        if (p != null) return p;
        StringBuilder d = new StringBuilder();
        if ((key & 1) != 0) d.append("#define LIT\n");
        if ((key & 2) != 0) d.append("#define VCOL\n");
        if ((key & 4) != 0) d.append("#define TEX\n");
        if ((key & 8) != 0) d.append("#define HULL\n");
        if ((key & 16) != 0) d.append("#define FOG\n");
        if ((key & 32) != 0) d.append("#define SCISSOR\n");
        if ((key & 64) != 0) d.append("#define SHADOW\n");
        if ((key & 128) != 0) d.append("#define ADD\n");
        if ((key & 256) != 0) d.append("#define DFADE\n");
        p = compile(VS, FS, d.toString());
        progs.put(key, p);
        return p;
    }

    @Override
    public void dispose() {
        for (ShaderProgram p : progs.values()) p.dispose();
        progs.clear();
        sky.dispose(); depth.dispose();
    }
}
