package ru.shitneba.gfx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;

/** Материал: аналог StandardMaterial3D / hull-шейдера из старой версии. */
public class Mat {
    public static final int OPAQUE = 0, ALPHA = 1, ADD = 2;

    public int blend = OPAQUE;
    public boolean cull = true, depthWrite = true, depthTest = true, fog = true, unshaded = false;
    public boolean vcol = true, shadowRecv = false, scissor = false, hull = false;
    /** цвет в sRGB, умножается (albedo_color) */
    public final Color color = new Color(1, 1, 1, 1);
    public Texture tex;
    public float uvx = 1, uvy = 1;
    public float rough = 0.85f, metal = 0f, spec = 0.5f;
    public float emission = 0f;
    // hull
    public Texture camo;
    public float camoScale = 0.2f, panelScale = 0.45f, seams = 1f, dust = 1f, dustH = 1.7f, burnt = 0f, wear = 0.6f;
    /** множитель альфы (transparency) */
    public float alpha = 1f;
    public float distFadeMin = 0f, distFadeMax = 0f;
    public int order = 0;

    public Mat() { }

    public Mat cpy() {
        Mat m = new Mat();
        m.blend = blend; m.cull = cull; m.depthWrite = depthWrite; m.depthTest = depthTest; m.fog = fog; m.unshaded = unshaded;
        m.vcol = vcol; m.shadowRecv = shadowRecv; m.scissor = scissor; m.hull = hull;
        m.color.set(color); m.tex = tex; m.uvx = uvx; m.uvy = uvy; m.rough = rough; m.metal = metal; m.spec = spec; m.emission = emission;
        m.camo = camo; m.camoScale = camoScale; m.panelScale = panelScale; m.seams = seams; m.dust = dust; m.dustH = dustH; m.burnt = burnt; m.wear = wear;
        m.alpha = alpha; m.distFadeMin = distFadeMin; m.distFadeMax = distFadeMax; m.order = order;
        return m;
    }

    public boolean transparent() { return blend != OPAQUE || alpha < 0.999f; }

    public int key() {
        int k = 0;
        if (!unshaded) k |= 1;
        if (vcol) k |= 2;
        if (tex != null) k |= 4;
        if (hull) k |= 8;
        if (fog) k |= 16;
        if (scissor) k |= 32;
        if (shadowRecv) k |= 64;
        if (blend == ADD) k |= 128;
        if (distFadeMax > 0) k |= 256;
        return k;
    }

    // ---- фабрики ----
    public static Mat vc() { Mat m = new Mat(); m.rough = 0.85f; return m; }

    public static Mat glow() { Mat m = new Mat(); m.unshaded = true; return m; }

    public static Mat std(Color c) { Mat m = new Mat(); m.vcol = false; m.color.set(c); m.rough = 1f; return m; }

    public static Mat unshadedAdd(Color c) {
        Mat m = new Mat(); m.unshaded = true; m.vcol = false; m.blend = ADD; m.color.set(c); m.depthWrite = false; m.cull = false;
        return m;
    }
}
