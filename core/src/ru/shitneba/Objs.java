package ru.shitneba;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;

import ru.shitneba.gfx.MB;
import ru.shitneba.gfx.Mesh3;
import ru.shitneba.gfx.Node3;

/** Объекты миссий: радар, плотина, подстанция, грузовик колонны. Порт objs.gd. */
public final class Objs {
    public static final class Obj { public Node3 g, spin; }

    static Color c(String h) { return G.hex(h); }

    static final java.util.Map<String, Mesh3[]> cache = new java.util.HashMap<>();
    static final java.util.Map<String, Node3> tplCache = new java.util.HashMap<>();

    public static Obj make(String kind) {
        if (kind.equals("radar")) {
            SModels.Target t = SModels.target("radar", "dusk");
            Obj o = new Obj(); o.g = t.g; o.spin = t.spin;
            return o;
        }
        // 3.3.1: плотина, подстанция и грузовики колонны — новые модели из Targets
        if (kind.equals("dam") || kind.equals("sub") || kind.equals("truck")) {
            String key = kind.equals("truck") ? "truck" + (Targets.truckVar % 3) : kind;
            Node3 tpl = tplCache.get(key);
            if (tpl == null) {
                tpl = kind.equals("dam") ? Targets.dam() : kind.equals("sub") ? Targets.substation() : Targets.convoyTruck();
                tplCache.put(key, tpl);
            } else if (kind.equals("truck")) Targets.truckVar++;
            Obj o = new Obj(); o.g = new Node3(); o.g.add(tpl.cloneTree());
            return o;
        }
        Mesh3[] ms = cache.get(kind);
        if (ms == null) {
            ms = new Mesh3[2];
            MB b = new MB();
            float PI = MathUtils.PI;
            switch (kind) {
                case "radar": {
                    b.box(3, 1.2f, 7, c("4d5a3c"), 0, 1.4f, 0);
                    for (float z : new float[]{-2.5f, 2.5f}) for (float s : new float[]{-1, 1}) b.cyl(0.55f, 0.55f, 0.4f, 12, Models.TIRE, s * 1.3f, 0.55f, z, 0, 0, PI / 2);
                    b.box(1.6f, 3.5f, 1.6f, c("5a6646"), 0, 3.8f, 0);
                    b.box(2.6f, 0.5f, 2.6f, c("3c4630"), 0, 5.6f, 0);
                    MB a = new MB();
                    a.box(9, 2.6f, 0.3f, c("6c7656"));
                    for (int i = 0; i < 9; i++) a.box(0.08f, 2.6f, 0.5f, c("3a3f33"), -4 + i, 0, -0.2f);
                    ms[1] = a.mesh();
                    break;
                }
                case "dam": {
                    b.extrude(new float[][]{{-6, 0}, {6, 0}, {3, 28}, {-3, 28}}, 220, c("8d8a82"), 0, -4, 0);
                    for (int i = 0; i < 6; i++) b.box(4, 22, 1, c("6e6c66"), -50 + i * 20, 8, 5.4f);
                    for (int i = 0; i < 3; i++) b.box(8, 18, 0.6f, c("bfd8e8"), -20 + i * 20, 5, 6.2f);
                    b.box(220, 1.2f, 7, c("5b5953"), 0, 24.6f, 0);
                    break;
                }
                case "sub": {
                    for (int i = 0; i < 3; i++) {
                        float x = -8f + i * 8;
                        b.box(4, 4, 3, c("6e7570"), x, 2, 0);
                        for (int k = 0; k < 3; k++) b.cyl(0.25f, 0.25f, 2, 8, c("c8b88a"), x - 1.2f + k * 1.2f, 5, 0);
                    }
                    for (float x : new float[]{-16f, 16f}) { b.box(0.5f, 18, 0.5f, c("777777"), x, 9, 0); b.box(8, 0.4f, 0.4f, c("777777"), x, 16, 0); }
                    b.box(30, 0.3f, 14, c("8a8a80"), 0, 0.15f, 0);
                    break;
                }
                default: { // truck
                    b.box(2.4f, 0.4f, 8, Models.DARK, 0, 1.0f, 0);
                    for (float z : new float[]{-2.8f, 1.4f, 2.8f}) for (float s : new float[]{-1, 1}) b.cyl(0.6f, 0.6f, 0.45f, 14, Models.TIRE, s * 1.2f, 0.6f, z, 0, 0, PI / 2);
                    b.extrude(new float[][]{{-1, 0}, {1.1f, 0}, {1.2f, 0.9f}, {0.9f, 1.7f}, {-1, 1.7f}}, 2.5f, c("56634a"), 0, 1.2f, -2.9f);
                    b.box(2.3f, 0.6f, 0.05f, Models.GLASS, 0, 2.5f, -3.95f);
                    b.box(2.6f, 2.4f, 5.2f, c("6b7752"), 0, 2.5f, 1.4f);
                }
            }
            ms[0] = b.mesh();
            cache.put(kind, ms);
        }
        Obj o = new Obj();
        o.g = new Node3();
        Node3 body = new Node3(ms[0], Models.MAT_HULL);
        body.castShadow = true;
        o.g.add(body);
        if (ms[1] != null) {
            o.spin = new Node3(ms[1], Models.MAT_HULL);
            o.spin.castShadow = true;
            o.spin.pos.set(0, 7.2f, 0);
            o.g.add(o.spin);
        }
        return o;
    }

    private Objs() { }
}
