package ru.shitneba.gfx;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;

/** Неиндексированный меш треугольников: pos3 nrm3 col4 uv2. */
public class Mesh3 implements Disposable {
    public static final int STRIDE = 12;
    public static final VertexAttributes ATTRS = new VertexAttributes(
            new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
            new VertexAttribute(VertexAttributes.Usage.Normal, 3, "a_normal"),
            new VertexAttribute(VertexAttributes.Usage.ColorUnpacked, 4, "a_color"),
            new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord0"));

    public final Mesh mesh;
    public final int count;
    public final Vector3 center = new Vector3();
    public float radius;

    public Mesh3(float[] v, int floats) {
        count = floats / STRIDE;
        mesh = new Mesh(true, Math.max(1, count), 0, ATTRS);
        mesh.setVertices(v, 0, floats);
        float mnx = 1e9f, mny = 1e9f, mnz = 1e9f, mxx = -1e9f, mxy = -1e9f, mxz = -1e9f;
        for (int i = 0; i < floats; i += STRIDE) {
            mnx = Math.min(mnx, v[i]); mny = Math.min(mny, v[i + 1]); mnz = Math.min(mnz, v[i + 2]);
            mxx = Math.max(mxx, v[i]); mxy = Math.max(mxy, v[i + 1]); mxz = Math.max(mxz, v[i + 2]);
        }
        if (count == 0) { mnx = mny = mnz = mxx = mxy = mxz = 0; }
        center.set((mnx + mxx) / 2, (mny + mxy) / 2, (mnz + mxz) / 2);
        float r = 0;
        for (int i = 0; i < floats; i += STRIDE) r = Math.max(r, center.dst2(v[i], v[i + 1], v[i + 2]));
        radius = (float) Math.sqrt(r);
    }

    @Override
    public void dispose() { mesh.dispose(); }
}
