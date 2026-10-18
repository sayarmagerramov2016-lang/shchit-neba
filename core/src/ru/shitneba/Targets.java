package ru.shitneba;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

import java.util.Random;

import ru.shitneba.gfx.MB;
import ru.shitneba.gfx.Mat;
import ru.shitneba.gfx.Node3;

/**
 * Цели режима «Рой» (3.3.0): склад, бензохранилище, РЛС, штаб, станция РЭБ.
 * Модели собираются из примитивов, но по образцу настоящих объектов:
 * арочный ангар из гофролиста, резервуары в обваловании, РЛС на прицепе с решётчатой антенной,
 * одноэтажное здание штаба с антеннами и спутниковой тарелкой, РЭБ на шасси трёхосного грузовика.
 * Вперёд у всех моделей — это −Z, земля — y = 0.
 */
public final class Targets {
    static Color c(String h) { return G.hex(h); }

    static Vector3 V(float x, float y, float z) { return new Vector3(x, y, z); }

    static final float PI = MathUtils.PI;
    static final float[] S2 = {-1f, 1f};

    // общая палитра
    static final Color CONC = c("8e8b82"), CONC_D = c("6f6c64"), DIRT = c("6d5f45"), DIRT_D = c("564a35");
    static final Color OLV = c("4d5a35"), OLV_D = c("3a4428"), OLV_L = c("66734a"), KHAKI = c("7a7556");
    static final Color STEEL = c("6b7075"), DSTEEL = c("3e4245"), RUST = c("7a4a2e"), TIRE = c("1c1c1c"), BLK = c("161616");
    static final Color WOOD = c("8a6a44"), WOOD_D = c("6b5132"), SAND = c("a8966a"), SAND_D = c("8c7a52");
    static final Color RED = c("b02a20"), WHITE = c("e6e4dc"), YEL = c("d8a820"), GLASS = c("1a2633");

    /** Набор построителей под разные материалы: корпус, металл, резина, стекло, огни. */
    static final class K {
        final MB b = new MB(), m = new MB(), r = new MB(), g = new MB(), l = new MB();
        K() { g.noAo = true; l.noAo = true; }

        Node3 node() {
            Node3 n = new Node3();
            put(n, b, Models.MAT_HULL, true);
            put(n, m, Models.MAT_METAL, true);
            put(n, r, Models.MAT_RUBBER, true);
            put(n, g, Models.MAT_GLASS, false);
            put(n, l, Models.MAT_LAMP, false);
            return n;
        }

        static void put(Node3 n, MB mb, Mat mat, boolean shadow) {
            if (mb.empty()) return;
            Node3 k = new Node3(mb.mesh(), mat);
            k.castShadow = shadow;
            n.add(k);
        }
    }

    // ================= общие детали =================

    /** Мешок с песком (капсула) — для брустверов. */
    static void bag(MB b, float x, float y, float z, float ry, Color col) {
        b.capsule(0.22f, 0.55f, col, x, y, z, 0, ry, PI / 2);
    }

    /** Бруствер из мешков по дуге. */
    static void bagWall(MB b, float cx, float cz, float R, float a0, float a1, int rows, Random rnd) {
        int n = Math.max(2, (int) (R * Math.abs(a1 - a0) / 0.95f));
        for (int row = 0; row < rows; row++) {
            float off = row % 2 == 0 ? 0 : 0.5f;
            for (int i = 0; i < n; i++) {
                float a = a0 + (a1 - a0) * (i + off) / n;
                if (row % 2 == 1 && i == n - 1) continue;
                Color col = rnd.nextBoolean() ? SAND : SAND_D;
                bag(b, cx + MathUtils.sin(a) * R, 0.2f + row * 0.36f, cz + MathUtils.cos(a) * R, a + PI / 2, col);
            }
        }
    }

    /** Ящик с крышкой, рёбрами и маркировкой. */
    static void crate(MB b, float x, float y, float z, float w, float h, float d, float ry, Color col) {
        b.box(w, h, d, col, x, y + h / 2, z, 0, ry, 0);
        Color dk = new Color(col).mul(0.78f, 0.78f, 0.78f, 1);
        float cs = MathUtils.cos(ry), sn = MathUtils.sin(ry);
        for (float s : S2) {
            float lx = s * (w / 2 - 0.06f);
            b.box(0.07f, h + 0.02f, d + 0.02f, dk, x + cs * lx, y + h / 2, z - sn * lx, 0, ry, 0);
        }
        b.box(w * 0.4f, h * 0.18f, 0.012f, c("d9d3b8"), x + sn * (d / 2 + 0.005f) * 0, y + h * 0.62f, z, 0, ry, 0);
    }

    /** Бочка 200 л с обручами. */
    static void barrel(MB b, MB m, float x, float y, float z, Color col, boolean lying, float ry) {
        float rx = lying ? PI / 2 : 0;
        b.cyl(0.29f, 0.29f, 0.88f, 16, col, x, y + (lying ? 0.29f : 0.44f), z, rx, ry, 0);
        for (float k : new float[]{-0.22f, 0.22f}) {
            float ox = lying ? MathUtils.sin(ry) * k : 0, oz = lying ? MathUtils.cos(ry) * k : 0, oy = lying ? 0.29f : 0.44f + k;
            m.cyl(0.305f, 0.305f, 0.03f, 16, DSTEEL, x + ox, y + oy, z + oz, rx, ry, 0);
        }
    }

    /** Поддон. */
    static void pallet(MB b, float x, float z, float ry) {
        for (int i = 0; i < 5; i++) {
            float o = -0.48f + i * 0.24f;
            b.box(0.12f, 0.03f, 1.2f, WOOD, x + MathUtils.cos(ry) * o, 0.135f, z - MathUtils.sin(ry) * o, 0, ry, 0);
        }
        for (float o : new float[]{-0.5f, 0, 0.5f}) b.box(1.0f, 0.1f, 0.1f, WOOD_D, x + MathUtils.sin(ry) * o, 0.06f, z + MathUtils.cos(ry) * o, 0, ry, 0);
    }

    /** Колесо грузовика: шина с протектором, диск, ступица. */
    static void truckWheel(K k, float x, float y, float z, float rad, float w, float side) {
        float rz = PI / 2;
        k.r.cyl(rad, rad, w, 22, TIRE, x, y, z, 0, 0, rz);
        for (int i = 0; i < 12; i++) {
            float a = i * MathUtils.PI2 / 12;
            k.r.box(w * 0.9f, 0.07f, 0.12f, TIRE, x, y + MathUtils.cos(a) * rad, z + MathUtils.sin(a) * rad, a, 0, 0);
        }
        k.b.cyl(rad * 0.55f, rad * 0.55f, w + 0.03f, 18, OLV_D, x + side * 0.0f, y, z, 0, 0, rz);
        k.m.cyl(rad * 0.2f, rad * 0.26f, w + 0.1f, 10, DSTEEL, x + side * 0.03f, y, z, 0, 0, rz);
        for (int i = 0; i < 8; i++) {
            float a = i * MathUtils.PI2 / 8;
            k.m.cyl(0.025f, 0.025f, w + 0.12f, 6, STEEL, x, y + MathUtils.cos(a) * rad * 0.37f, z + MathUtils.sin(a) * rad * 0.37f, 0, 0, rz);
        }
    }

    /**
     * Трёхосный армейский грузовик (как КамАЗ-5350): рама, колёса, кабина.
     * Кабина спереди (−Z), задний край рамы — z1. Возвращает высоту верха рамы.
     */
    static float truck(K k, float zCab, float z1, Color body) {
        float frameY = 1.12f;
        float wr = 0.58f, ww = 0.42f;
        float[] axles = {zCab + 1.15f, z1 - 2.15f, z1 - 0.85f};
        // рама и мосты
        for (float s : S2) k.m.box(0.14f, 0.28f, z1 - zCab - 0.2f, DSTEEL, s * 0.48f, frameY - 0.14f, (zCab + z1) / 2 + 0.1f);
        for (float z : axles) {
            k.m.cyl(0.11f, 0.11f, 2.0f, 10, DSTEEL, 0, wr, z, 0, 0, PI / 2);
            k.m.sph(0.24f, DSTEEL, 0, wr, z, 1, 0.85f, 1.2f);
            for (float s : S2) truckWheel(k, s * 1.04f, wr, z, wr, ww, s);
        }
        // кабина
        float cz = zCab + 1.05f, cw = 2.4f, ch = 1.85f, cd = 1.95f, cy = frameY + 0.12f;
        k.b.box(cw, ch, cd, body, 0, cy + ch / 2, cz);
        k.b.box(cw - 0.1f, 0.12f, cd - 0.1f, new Color(body).mul(0.85f, 0.85f, 0.85f, 1), 0, cy + ch + 0.05f, cz);
        // лобовое стекло (2 половины), боковые окна
        for (float s : S2) {
            k.g.box(1.05f, 0.72f, 0.03f, GLASS, s * 0.56f, cy + ch - 0.52f, zCab + 0.06f, -0.06f, 0, 0);
            k.g.box(0.03f, 0.6f, 0.9f, GLASS, s * (cw / 2 + 0.005f), cy + ch - 0.55f, cz - 0.2f);
            // дверь: щель и ручка
            k.b.box(0.012f, ch - 0.35f, 0.025f, BLK, s * (cw / 2 + 0.006f), cy + ch / 2 - 0.05f, cz + 0.55f);
            k.m.box(0.04f, 0.04f, 0.2f, DSTEEL, s * (cw / 2 + 0.03f), cy + 0.95f, cz + 0.3f);
            // ступени
            k.m.box(0.35f, 0.05f, 0.5f, DSTEEL, s * (cw / 2 - 0.1f), cy - 0.15f, cz - 0.1f);
            k.m.box(0.35f, 0.05f, 0.5f, DSTEEL, s * (cw / 2 - 0.1f), cy - 0.55f, cz - 0.1f);
            // зеркала на кронштейнах
            k.m.seg(V(s * cw / 2, cy + ch - 0.3f, zCab + 0.25f), V(s * (cw / 2 + 0.38f), cy + ch - 0.15f, zCab + 0.05f), 0.02f, 0.02f, 6, DSTEEL);
            k.b.box(0.06f, 0.42f, 0.22f, BLK, s * (cw / 2 + 0.4f), cy + ch - 0.3f, zCab + 0.05f);
            // фары и подфарники в бампере
            k.l.box(0.28f, 0.18f, 0.04f, c("fff3d0"), s * 0.82f, frameY - 0.05f, zCab - 0.21f);
            k.l.box(0.12f, 0.08f, 0.04f, c("ffa020"), s * 1.08f, frameY - 0.05f, zCab - 0.21f);
            // крылья над передними колёсами
            k.b.extrude(new float[][]{{-0.75f, 0}, {0.75f, 0}, {0.75f, 0.06f}, {-0.75f, 0.06f}}, 0.48f, body, s * 1.04f, wr + 0.62f, axles[0], 0, PI / 2, 0);
            // брызговики задних мостов
            k.r.box(0.45f, 0.5f, 0.03f, BLK, s * 1.04f, wr + 0.05f, z1 - 0.2f);
        }
        // стойка между стёклами, козырёк, решётка, бампер
        k.b.box(0.08f, 0.75f, 0.06f, body, 0, cy + ch - 0.52f, zCab + 0.04f);
        k.b.box(cw, 0.08f, 0.3f, body, 0, cy + ch - 0.06f, zCab - 0.1f);
        k.b.box(1.4f, 0.55f, 0.05f, new Color(body).mul(0.6f, 0.6f, 0.6f, 1), 0, cy + 0.42f, zCab - 0.01f);
        for (int i = 0; i < 5; i++) k.m.box(1.32f, 0.04f, 0.04f, DSTEEL, 0, cy + 0.2f + i * 0.11f, zCab - 0.04f);
        k.m.box(cw + 0.1f, 0.26f, 0.22f, DSTEEL, 0, frameY - 0.05f, zCab - 0.08f);
        for (float s : S2) k.m.box(0.18f, 0.06f, 0.18f, DSTEEL, s * 0.6f, frameY - 0.22f, zCab - 0.2f);
        // габаритные огни на крыше
        for (int i = 0; i < 3; i++) k.l.box(0.12f, 0.06f, 0.06f, c("ffb020"), -0.35f + i * 0.35f, cy + ch + 0.14f, zCab + 0.15f);
        // за кабиной: топливный бак, ящик АКБ, запаска
        k.m.cyl(0.3f, 0.3f, 1.1f, 16, STEEL, -1.0f, frameY - 0.35f, zCab + 2.55f, PI / 2, 0, 0);
        k.b.box(0.6f, 0.45f, 0.7f, OLV_D, 1.0f, frameY - 0.3f, zCab + 2.55f);
        return frameY;
    }

    /** Кунг (кузов-фургон) на раме: коробка с рёбрами, дверью, лестницей, окнами, кондиционером. */
    static void kung(K k, float y0, float z0, float z1, float h, Color col) {
        float w = 2.5f, L = z1 - z0, zc = (z0 + z1) / 2;
        k.b.box(w, h, L, col, 0, y0 + h / 2, zc);
        Color rib = new Color(col).mul(0.82f, 0.82f, 0.82f, 1);
        int n = (int) (L / 0.9f);
        for (int i = 0; i <= n; i++) {
            float z = z0 + 0.05f + i * (L - 0.1f) / n;
            for (float s : S2) k.b.box(0.04f, h - 0.04f, 0.07f, rib, s * (w / 2 + 0.015f), y0 + h / 2, z);
        }
        for (float s : S2) {
            k.b.box(0.05f, 0.08f, L, rib, s * (w / 2 + 0.02f), y0 + h - 0.04f, zc);
            k.b.box(0.05f, 0.08f, L, rib, s * (w / 2 + 0.02f), y0 + 0.04f, zc);
            k.g.box(0.02f, 0.42f, 0.7f, GLASS, s * (w / 2 + 0.03f), y0 + h - 0.6f, z0 + 0.9f);
            k.b.box(0.04f, 0.52f, 0.8f, BLK, s * (w / 2 + 0.02f), y0 + h - 0.6f, z0 + 0.9f);
        }
        // дверь сзади, лестница
        k.b.box(0.9f, h - 0.3f, 0.05f, rib, 0.45f, y0 + (h - 0.3f) / 2 + 0.1f, z1 + 0.02f);
        k.m.box(0.04f, 0.04f, 0.18f, DSTEEL, 0.1f, y0 + h / 2, z1 + 0.1f);
        for (float s : S2) k.m.seg(V(0.45f + s * 0.25f, 0.2f, z1 + 0.55f), V(0.45f + s * 0.25f, y0, z1 + 0.1f), 0.025f, 0.025f, 6, DSTEEL);
        for (int i = 0; i < 4; i++) {
            float t = (i + 0.5f) / 4f;
            k.m.box(0.5f, 0.03f, 0.06f, DSTEEL, 0.45f, 0.2f + (y0 - 0.2f) * t, z1 + 0.55f - 0.45f * t);
        }
        // кондиционер на передней стенке
        k.b.box(1.0f, 0.55f, 0.35f, c("8a8a80"), -0.5f, y0 + h - 0.45f, z0 - 0.17f);
        k.m.cyl(0.18f, 0.18f, 0.02f, 16, DSTEEL, -0.5f, y0 + h - 0.45f, z0 - 0.35f, PI / 2, 0, 0);
    }

    /** Аутригер-домкрат (опора) — откинутая лапа с пятой. */
    static void jack(K k, float x, float y, float z, float side) {
        k.m.box(0.25f, 0.2f, 0.25f, DSTEEL, x, y, z);
        k.m.seg(V(x, y, z), V(x + side * 0.55f, 0.35f, z), 0.07f, 0.07f, 8, OLV_D);
        k.m.cyl(0.06f, 0.06f, 0.4f, 8, STEEL, x + side * 0.55f, 0.2f, z);
        k.m.cyl(0.24f, 0.24f, 0.05f, 14, DSTEEL, x + side * 0.55f, 0.025f, z);
    }

    // ================= 1. Склад =================

    /** Арочный ангар из гофролиста на бетонной площадке, ящики, бочки, поддоны. */
    static void depot(K k, boolean snow, Random R) {
        MB b = k.b, m = k.m;
        float hw = 5.4f, hh = 4.3f, L = 13f;
        Color sheet = snow ? c("b8bcc0") : c("5c6152");
        Color sheetD = snow ? c("9da2a6") : c("4a4f43");
        // площадка и отмостка
        b.box(14.2f, 0.25f, 18.5f, CONC, 0, 0.125f, -1.2f);
        for (int i = 0; i < 6; i++) b.box(14.15f, 0.02f, 0.04f, CONC_D, 0, 0.255f, -10.2f + i * 3.4f);
        // гофролист: полосы-арки чередуются по тону — даёт «волну» профиля
        int ribs = 26;
        for (int i = 0; i < ribs; i++) {
            float z = -L / 2 + (i + 0.5f) * L / ribs;
            float dr = i % 2 == 0 ? 0.0f : 0.05f;
            float[][] arch = new float[17][];
            for (int j = 0; j < 17; j++) { float a = PI * j / 16f; arch[j] = new float[]{MathUtils.cos(a) * (hw + dr), MathUtils.sin(a) * (hh + dr)}; }
            b.extrude(arch, L / ribs + 0.01f, i % 2 == 0 ? sheet : sheetD, 0, 0.25f, z, 0, PI / 2, 0);
        }
        // торцы: каркас, раздвижные ворота с раскосами, калитка, вентрешётки
        for (float s : S2) {
            float zz = s * (L / 2 + 0.03f);
            float[][] end = new float[17][];
            for (int j = 0; j < 17; j++) { float a = PI * j / 16f; end[j] = new float[]{MathUtils.cos(a) * hw * 0.99f, MathUtils.sin(a) * hh * 0.99f}; }
            b.extrude(end, 0.08f, sheetD, 0, 0.25f, zz, 0, PI / 2, 0);
            b.box(0.25f, 0.2f, 0.4f, CONC_D, 0, 0.35f, zz);
        }
        float zf = -L / 2 - 0.1f;
        Color gate = c("3a4030");
        for (float s : S2) {
            float gx = s * 1.25f;
            b.box(2.4f, 3.5f, 0.1f, gate, gx, 2.0f, zf);
            m.box(2.4f, 0.1f, 0.06f, DSTEEL, gx, 3.7f, zf - 0.07f);
            m.box(2.4f, 0.1f, 0.06f, DSTEEL, gx, 0.32f, zf - 0.07f);
            m.box(2.4f, 0.1f, 0.06f, DSTEEL, gx, 2.0f, zf - 0.07f);
            for (float e : S2) m.box(0.1f, 3.5f, 0.06f, DSTEEL, gx + e * 1.15f, 2.0f, zf - 0.07f);
            m.box(0.08f, 3.2f, 0.05f, DSTEEL, gx, 2.0f, zf - 0.08f, 0, 0, s * 0.6f);
        }
        m.box(5.6f, 0.14f, 0.2f, DSTEEL, 0, 3.95f, zf - 0.05f); // направляющая ворот
        b.box(0.85f, 1.9f, 0.08f, gate, -3.6f, 1.2f, zf);
        m.box(0.06f, 0.06f, 0.14f, STEEL, -3.3f, 1.2f, zf - 0.08f);
        k.l.box(0.3f, 0.12f, 0.12f, c("fff0c0"), -3.6f, 2.4f, zf - 0.08f);
        for (float s : S2) {
            b.box(0.9f, 0.6f, 0.08f, BLK, s * 2.4f, 4.15f - 0.4f, zf + 0.01f);
            for (int i = 0; i < 4; i++) m.box(0.88f, 0.04f, 0.1f, DSTEEL, s * 2.4f, 3.55f + i * 0.13f, zf - 0.03f);
        }
        // водостоки вдоль низа арки
        for (float s : S2) m.cyl(0.08f, 0.08f, L, 10, STEEL, s * (hw + 0.1f), 0.5f, 0, PI / 2, 0, 0);
        // маскировочная сеть на части крыши (рваные лоскуты)
        Color[] net = snow ? new Color[]{c("e3e6e8"), c("c9ced2")} : new Color[]{c("4c5a32"), c("6b6a3e"), c("3e4a2a")};
        for (int i = 0; i < 22; i++) {
            float a = 0.35f + R.nextFloat() * (PI - 0.7f);
            float z = 0.5f + R.nextFloat() * 5.5f;
            b.box(1.6f + R.nextFloat(), 0.04f, 1.3f + R.nextFloat(), net[i % net.length],
                    MathUtils.cos(a) * (hw + 0.12f), 0.25f + MathUtils.sin(a) * (hh + 0.12f), z, 0, 0, a - PI / 2 + (R.nextFloat() - 0.5f) * 0.3f);
        }
        // перед воротами: ящики боеприпасов штабелями, поддоны, бочки
        Color[] cr = {c("4f5d33"), c("5a6638"), c("44502c")};
        for (int st = 0; st < 3; st++) {
            float bx = -4.6f + st * 1.5f, bz = -9.3f + (st % 2) * 0.4f;
            pallet(b, bx, bz, 0.05f * st);
            for (int lv = 0; lv < 3 - st % 2; lv++) for (int j = 0; j < 2; j++)
                crate(b, bx - 0.25f + j * 0.5f, 0.2f + lv * 0.42f, bz, 0.48f, 0.4f, 1.1f, 0.05f * st, cr[(lv + j + st) % 3]);
        }
        for (int i = 0; i < 6; i++) barrel(b, m, 3.2f + (i % 3) * 0.62f, 0.25f, -9.2f + (i / 3) * 0.62f, i % 2 == 0 ? c("2f4f63") : c("6b3626"), false, 0);
        barrel(b, m, 5.0f, 0.25f, -8.4f, c("2f4f63"), true, 0.8f);
        pallet(b, 1.4f, -10.0f, 0.3f);
        crate(b, 1.4f, 0.2f, -10.0f, 1.2f, 0.7f, 0.9f, 0.3f, c("6a5c3e"));
        // фонарный столб
        m.cyl(0.07f, 0.1f, 5.5f, 8, DSTEEL, 6.2f, 2.75f, -8.2f);
        m.seg(V(6.2f, 5.4f, -8.2f), V(5.4f, 5.6f, -8.2f), 0.04f, 0.04f, 6, DSTEEL);
        k.l.box(0.4f, 0.1f, 0.25f, c("fff0c0"), 5.3f, 5.5f, -8.2f);
    }

    // ================= 2. Бензохранилище =================

    /** Резервуар горизонтальный: цилиндр с днищами, опоры-ложементы, люк, лестница. */
    static void hTank(K k, float x, float z, float rad, float len, Color col, Random R) {
        MB b = k.b, m = k.m;
        float y = rad + 0.55f;
        b.cyl(rad, rad, len, 32, col, x, y, z, PI / 2, 0, 0);
        for (float s : S2) b.sph(rad, new Color(col).mul(0.95f, 0.95f, 0.95f, 1), x, y, z + s * len / 2, 1, 1, 0.28f);
        // сварные кольцевые швы и потёки ржавчины
        for (int i = 1; i < 4; i++) m.cyl(rad + 0.012f, rad + 0.012f, 0.04f, 32, new Color(col).mul(0.86f, 0.86f, 0.86f, 1), x, y, z - len / 2 + i * len / 4, PI / 2, 0, 0);
        for (int i = 0; i < 4; i++) {
            float a = 0.6f + R.nextFloat() * 1.0f, zz = z - len / 2 + 0.6f + R.nextFloat() * (len - 1.2f);
            b.box(0.12f, 0.9f, 0.02f, RUST, x + MathUtils.cos(a) * (rad + 0.01f), y + MathUtils.sin(a) * (rad + 0.01f) - 0.3f, zz, 0, PI / 2, a - PI / 2);
        }
        // опоры
        for (float s : S2) {
            float zz = z + s * len * 0.3f;
            b.box(rad * 1.6f, 0.55f, 0.6f, CONC, x, 0.275f, zz);
            m.torus(rad + 0.03f, 0.05f, DSTEEL, x, y, zz, PI / 2, 0, 0);
        }
        // горловина с люком и дыхательным клапаном
        m.cyl(0.38f, 0.38f, 0.35f, 18, STEEL, x, y + rad + 0.1f, z - len * 0.18f);
        m.cyl(0.42f, 0.42f, 0.06f, 18, DSTEEL, x, y + rad + 0.3f, z - len * 0.18f);
        m.cyl(0.07f, 0.07f, 0.5f, 8, STEEL, x + 0.3f, y + rad + 0.25f, z + len * 0.2f);
        m.cyl(0.14f, 0.1f, 0.18f, 10, DSTEEL, x + 0.3f, y + rad + 0.55f, z + len * 0.2f);
        // площадка обслуживания с перилами сверху
        float py = y + rad + 0.03f;
        m.box(1.0f, 0.05f, len * 0.5f, DSTEEL, x - 0.55f, py, z);
        for (float e : new float[]{-0.25f, 0.25f}) {
            float zz = z + e * len;
            m.cyl(0.025f, 0.025f, 1.0f, 6, YEL, x - 1.05f, py + 0.5f, zz);
        }
        m.cyl(0.025f, 0.025f, len * 0.5f, 6, YEL, x - 1.05f, py + 1.0f, z, PI / 2, 0, 0);
        m.cyl(0.025f, 0.025f, len * 0.5f, 6, YEL, x - 1.05f, py + 0.5f, z, PI / 2, 0, 0);
        // лестница на торце
        float lz = z - len / 2 - rad * 0.28f - 0.25f;
        for (float e : S2) m.seg(V(x - 0.6f + e * 0.25f, 0.25f, lz - 0.45f), V(x - 0.6f + e * 0.25f, py, lz + 0.1f), 0.025f, 0.025f, 6, DSTEEL);
        int st = (int) (py / 0.32f);
        for (int i = 1; i < st; i++) { float t = (float) i / st; m.box(0.5f, 0.03f, 0.05f, DSTEEL, x - 0.6f, 0.25f + (py - 0.25f) * t, lz - 0.45f + 0.55f * t); }
        // красная полоса «огнеопасно»
        b.cyl(rad + 0.006f, rad + 0.006f, 0.35f, 32, RED, x, y, z + len * 0.36f, PI / 2, 0, 0);
    }

    /** Склад ГСМ: два горизонтальных резервуара и вертикальный, обвалование, трубопровод с задвижками, насосная. */
    static void fuel(K k, boolean snow, Random R) {
        MB b = k.b, m = k.m;
        Color earth = snow ? c("dfe2e4") : DIRT, earthD = snow ? c("c5c9cc") : DIRT_D;
        // обвалование — земляной вал по периметру с проездом спереди
        float W = 15f, D = 14f;
        for (float s : S2) {
            b.extrude(new float[][]{{-1.4f, 0}, {1.4f, 0}, {0.45f, 1.1f}, {-0.45f, 1.1f}}, D, earth, s * W / 2, 0, 0, 0, PI / 2, 0);
        }
        b.extrude(new float[][]{{-1.4f, 0}, {1.4f, 0}, {0.45f, 1.1f}, {-0.45f, 1.1f}}, W + 2.8f, earthD, 0, 0, D / 2, 0, 0, 0);
        for (float s : S2) b.extrude(new float[][]{{-1.4f, 0}, {1.4f, 0}, {0.45f, 1.1f}, {-0.45f, 1.1f}}, 4.6f, earthD, s * 5.1f, 0, -D / 2, 0, 0, 0);
        // днище каре — бетон
        b.box(W - 1.4f, 0.12f, D - 1.4f, CONC_D, 0, 0.06f, 0);
        // резервуары
        Color tank = snow ? c("cfd2d0") : c("b9b8ad");
        hTank(k, -3.2f, 1.2f, 1.55f, 8.6f, tank, R);
        hTank(k, 0.9f, 1.2f, 1.55f, 8.6f, new Color(tank).mul(0.96f, 0.97f, 0.94f, 1), R);
        // вертикальный резервуар РВС с кольцевыми поясами и винтовой лестницей
        float vx = 5.1f, vz = -1.8f, vr = 2.0f, vh = 4.2f;
        b.cyl(vr, vr, vh, 36, tank, vx, vh / 2 + 0.12f, vz);
        b.cyl(vr * 0.98f, 0.35f, 0.55f, 36, new Color(tank).mul(0.92f, 0.92f, 0.92f, 1), vx, vh + 0.4f, vz);
        for (int i = 1; i < 4; i++) m.cyl(vr + 0.015f, vr + 0.015f, 0.05f, 36, new Color(tank).mul(0.85f, 0.85f, 0.85f, 1), vx, 0.12f + i * vh / 4, vz);
        b.cyl(vr + 0.008f, vr + 0.008f, 0.4f, 36, RED, vx, vh * 0.72f, vz);
        for (int i = 0; i < 14; i++) {
            float a = -PI * 0.2f + i * 0.13f, y = 0.4f + i * (vh - 0.3f) / 14f;
            m.box(0.55f, 0.04f, 0.22f, DSTEEL, vx + MathUtils.cos(a) * (vr + 0.3f), y, vz + MathUtils.sin(a) * (vr + 0.3f), 0, -a, 0);
            m.cyl(0.02f, 0.02f, 1.0f, 5, YEL, vx + MathUtils.cos(a) * (vr + 0.56f), y + 0.5f, vz + MathUtils.sin(a) * (vr + 0.56f));
        }
        for (int i = 0; i < 12; i++) {
            float a = i * MathUtils.PI2 / 12;
            m.cyl(0.02f, 0.02f, 0.9f, 5, YEL, vx + MathUtils.cos(a) * (vr - 0.1f), vh + 0.6f, vz + MathUtils.sin(a) * (vr - 0.1f));
        }
        m.torus(vr - 0.1f, 0.03f, YEL, vx, vh + 1.05f, vz, 0, 0, 0);
        // трубопровод вдоль переднего края с задвижками (штурвалы) и отводами к резервуарам
        float py = 0.7f, pz = -5.2f;
        m.cyl(0.13f, 0.13f, 11f, 12, c("5c5a50"), -0.6f, py, pz, 0, 0, PI / 2);
        for (float tx : new float[]{-3.2f, 0.9f}) {
            m.seg(V(tx, py, pz), V(tx, py, -3.1f), 0.1f, 0.1f, 10, c("5c5a50"));
            m.seg(V(tx, py, -3.1f), V(tx, 1.2f, -2.8f), 0.1f, 0.1f, 10, c("5c5a50"));
            m.torus(0.22f, 0.025f, RED, tx, py + 0.45f, pz + 0.6f, 0, 0, 0);
            m.cyl(0.03f, 0.03f, 0.4f, 6, DSTEEL, tx, py + 0.25f, pz + 0.6f);
            m.cyl(0.17f, 0.17f, 0.25f, 12, DSTEEL, tx, py, pz + 0.6f, PI / 2, 0, 0);
        }
        for (int i = 0; i < 6; i++) m.cyl(0.04f, 0.04f, py, 6, DSTEEL, -5.5f + i * 2f, py / 2, pz);
        // насосная: будка с дверью и двускатной крышей, раздаточная колонка
        float hx = -5.0f, hz = -5.6f;
        b.box(2.2f, 2.1f, 1.8f, c("7d7c70"), hx, 1.17f, hz);
        b.extrude(new float[][]{{-1.25f, 0}, {1.25f, 0}, {0, 0.6f}}, 2.1f, c("5b4a3a"), hx, 2.22f, hz, 0, PI / 2, 0);
        b.box(0.8f, 1.6f, 0.05f, c("4f5640"), hx + 0.3f, 0.92f, hz - 0.92f);
        k.l.box(0.2f, 0.1f, 0.1f, c("fff0c0"), hx + 0.3f, 1.95f, hz - 0.95f);
        b.box(0.5f, 1.3f, 0.4f, RED, -2.0f, 0.77f, -6.3f);
        k.r.seg(V(-2.0f, 1.1f, -6.5f), V(-1.6f, 0.2f, -6.9f), 0.04f, 0.04f, 6, BLK);
        // пожарный щит: красная доска, ведро-конус, огнетушитель, ящик с песком
        b.box(1.2f, 1.6f, 0.06f, RED, 3.8f, 0.92f, -6.2f);
        for (int i = 0; i < 2; i++) m.cyl(0.0f, 0.16f, 0.35f, 10, RED, 3.45f + i * 0.35f, 1.3f, -6.28f, PI, 0, 0);
        b.cyl(0.09f, 0.09f, 0.55f, 10, RED, 4.2f, 0.62f, -6.3f);
        b.box(1.0f, 0.5f, 0.6f, c("8a2a20"), 3.8f, 0.37f, -6.8f);
        // предупреждающие таблички на столбиках у въезда
        for (float s : S2) {
            m.cyl(0.04f, 0.04f, 1.4f, 6, DSTEEL, s * 2.5f, 0.82f, -7.6f);
            b.box(0.6f, 0.4f, 0.03f, YEL, s * 2.5f, 1.45f, -7.62f);
            b.box(0.5f, 0.08f, 0.032f, BLK, s * 2.5f, 1.45f, -7.63f);
        }
    }

    // ================= 3. РЛС =================

    /** Неподвижная часть РЛС: прицеп-фургон с опорами и мачтой-поворотным основанием; генератор рядом. */
    static void radarBase(K k, boolean snow) {
        MB b = k.b, m = k.m;
        Color body = snow ? c("b8bcb2") : OLV;
        // рама прицепа, колёса, дышло
        for (float s : S2) m.box(0.16f, 0.25f, 7.0f, DSTEEL, s * 0.75f, 1.0f, 0.2f);
        for (float z : new float[]{1.6f, 2.8f}) {
            m.cyl(0.09f, 0.09f, 2.0f, 8, DSTEEL, 0, 0.55f, z, 0, 0, PI / 2);
            for (float s : S2) truckWheel(k, s * 1.05f, 0.55f, z, 0.55f, 0.38f, s);
        }
        m.seg(V(-0.7f, 0.95f, -3.2f), V(0, 0.7f, -4.6f), 0.06f, 0.06f, 8, DSTEEL);
        m.seg(V(0.7f, 0.95f, -3.2f), V(0, 0.7f, -4.6f), 0.06f, 0.06f, 8, DSTEEL);
        m.torus(0.13f, 0.035f, DSTEEL, 0, 0.7f, -4.75f, 0, 0, 0);
        // аппаратная — кунг на прицепе
        kung(k, 1.12f, -3.0f, 1.2f, 2.0f, body);
        // опоры-домкраты по углам
        for (float s : S2) for (float z : new float[]{-2.9f, 3.3f}) jack(k, s * 0.9f, 1.0f, z, s);
        // поворотное основание и мачта на задней части
        b.box(2.3f, 0.35f, 2.2f, body, 0, 1.3f, 2.3f);
        m.cyl(0.95f, 1.05f, 0.35f, 28, DSTEEL, 0, 1.62f, 2.3f);
        m.cyl(0.7f, 0.85f, 1.2f, 24, body, 0, 2.38f, 2.3f);
        // кабели к генератору
        k.r.seg(V(-1.25f, 1.6f, -1.5f), V(-3.0f, 0.05f, -1.8f), 0.04f, 0.04f, 6, BLK);
        k.r.seg(V(-3.0f, 0.05f, -1.8f), V(-4.6f, 0.05f, -0.8f), 0.04f, 0.04f, 6, BLK);
        // дизель-генератор на салазках
        float gx = -5.2f, gz = -0.6f;
        m.box(1.4f, 0.12f, 2.4f, DSTEEL, gx, 0.06f, gz);
        b.box(1.2f, 1.1f, 2.1f, body, gx, 0.67f, gz);
        for (int i = 0; i < 6; i++) m.box(0.02f, 0.6f, 0.08f, DSTEEL, gx - 0.61f, 0.7f, gz - 0.6f + i * 0.24f);
        m.cyl(0.07f, 0.07f, 0.6f, 8, DSTEEL, gx + 0.3f, 1.5f, gz + 0.7f);
        m.cyl(0.09f, 0.07f, 0.1f, 8, BLK, gx + 0.3f, 1.83f, gz + 0.7f);
        // бруствер вокруг генератора
        Random R = new Random(7);
        bagWall(b, gx, gz, 1.9f, PI * 0.55f, PI * 1.95f, 2, R);
        // антенна связи на кунге
        m.cyl(0.02f, 0.035f, 3.0f, 6, DSTEEL, 0.9f, 4.6f, -2.6f);
    }

    /** Вращающаяся часть РЛС (как у РЛС «Небо»): решётчатая ферма, ряды волновых каналов-«ёлок», облучатель. */
    static void radarSpin(K k) {
        MB m = k.m, b = k.b;
        float W = 8.6f, H = 2.6f;
        // вертикальная стойка от основания (основание узла на y=0, стойка вниз до мачты)
        m.cyl(0.32f, 0.38f, 3.5f, 16, OLV_D, 0, -1.9f, 0);
        // поворотная голова
        b.box(1.2f, 0.7f, 1.0f, OLV, 0, -0.1f, 0.35f);
        // ферма: верх/низ пояса и раскосы
        for (float yy : new float[]{-H / 2, H / 2}) m.cyl(0.07f, 0.07f, W, 8, DSTEEL, 0, yy + 0.4f, -0.25f, 0, 0, PI / 2);
        int n = 12;
        for (int i = 0; i <= n; i++) {
            float x = -W / 2 + i * W / n;
            m.cyl(0.04f, 0.04f, H, 6, DSTEEL, x, 0.4f, -0.25f);
            if (i < n) m.seg(V(x, -H / 2 + 0.4f, -0.25f), V(x + W / n, H / 2 + 0.4f, -0.25f), 0.03f, 0.03f, 5, DSTEEL);
        }
        // подкосы к голове
        for (float s : S2) m.seg(V(0, -0.2f, 0.3f), V(s * W * 0.38f, -H / 2 + 0.4f, -0.25f), 0.05f, 0.05f, 6, DSTEEL);
        // антенные «ёлки»: 3 ряда по 12 волновых каналов, у каждого — директоры
        for (int row = 0; row < 3; row++) {
            float yy = -0.45f + row * 0.85f + 0.4f;
            for (int i = 0; i < 12; i++) {
                float x = -W / 2 + 0.36f + i * (W - 0.72f) / 11f;
                m.cyl(0.018f, 0.018f, 1.1f, 5, STEEL, x, yy, -0.8f, PI / 2, 0, 0);
                for (int d = 0; d < 4; d++) m.cyl(0.01f, 0.01f, 0.55f - d * 0.05f, 4, c("b4b6b0"), x, yy, -0.4f - d * 0.27f, 0, 0, PI / 2);
            }
        }
        // отражатель (сетка) за излучателями
        for (int i = 0; i < 9; i++) m.cyl(0.012f, 0.012f, W, 4, c("8a8d88"), 0, -H / 2 + 0.5f + i * (H - 0.2f) / 8, -0.18f, 0, 0, PI / 2);
        // проблесковый огонь на вершине
        k.l.box(0.14f, 0.14f, 0.14f, c("ff3020"), 0, H / 2 + 0.55f, -0.25f);
    }

    // ================= 4. Штаб =================

    /** Одноэтажное здание штаба: плоская кровля с парапетом, окна с рамами, крыльцо, антенны, тарелка, мешки. */
    static void hq(K k, boolean snow, Random R) {
        MB b = k.b, m = k.m;
        Color wall = snow ? c("c6c2b8") : c("a59c86"), wallD = new Color(wall).mul(0.85f, 0.85f, 0.85f, 1);
        Color plinth = c("6e6a60"), roof = c("4a4a46");
        float W = 11f, D = 7.5f, H = 3.4f;
        // цоколь, стены, швы панелей
        b.box(W + 0.3f, 0.45f, D + 0.3f, plinth, 0, 0.225f, 0);
        b.box(W, H, D, wall, 0, 0.45f + H / 2, 0);
        for (int i = 1; i < 6; i++) for (float s : S2) b.box(0.03f, H, 0.02f, wallD, -W / 2 + i * W / 6, 0.45f + H / 2, s * (D / 2 + 0.005f));
        b.box(W + 0.02f, 0.03f, D + 0.02f, wallD, 0, 0.45f + H * 0.5f, 0);
        // кровля с парапетом, вентшахты, лючок
        float ry = 0.45f + H;
        b.box(W + 0.2f, 0.18f, D + 0.2f, roof, 0, ry + 0.09f, 0);
        for (float s : S2) {
            b.box(W + 0.2f, 0.45f, 0.18f, wallD, 0, ry + 0.4f, s * (D / 2 + 0.01f));
            b.box(0.18f, 0.45f, D + 0.2f, wallD, s * (W / 2 + 0.01f), ry + 0.4f, 0);
        }
        b.box(0.8f, 0.7f, 0.8f, wallD, 3.4f, ry + 0.5f, 1.8f);
        m.box(0.9f, 0.06f, 0.9f, DSTEEL, 3.4f, ry + 0.88f, 1.8f);
        b.box(0.6f, 0.5f, 0.6f, wallD, -4.2f, ry + 0.4f, -2.1f);
        // окна по фасадам (рамы, стекло, отливы); часть заложена мешками
        for (int i = 0; i < 5; i++) {
            float x = -W / 2 + 1.2f + i * (W - 2.4f) / 4;
            if (i == 2) continue;
            for (float s : S2) {
                float z = s * (D / 2 + 0.02f);
                boolean bagged = (i + (s > 0 ? 1 : 0)) % 3 == 0;
                b.box(1.25f, 1.35f, 0.05f, c("d8d3c4"), x, 2.35f, z);
                if (bagged) {
                    for (int r = 0; r < 3; r++) for (int j = 0; j < 2; j++) bag(b, x - 0.27f + j * 0.54f + (r % 2) * 0.1f, 1.85f + r * 0.36f, z + s * 0.1f, 0, r % 2 == 0 ? SAND : SAND_D);
                } else {
                    k.g.box(1.1f, 1.2f, 0.06f, GLASS, x, 2.35f, z);
                    b.box(0.06f, 1.2f, 0.07f, c("d8d3c4"), x, 2.35f, z);
                    b.box(1.1f, 0.06f, 0.07f, c("d8d3c4"), x, 2.65f, z);
                    // крест из скотча
                    b.box(1.5f, 0.05f, 0.075f, c("c8b88a"), x, 2.35f, z, 0, 0, 0.83f);
                    b.box(1.5f, 0.05f, 0.075f, c("c8b88a"), x, 2.35f, z, 0, 0, -0.83f);
                }
                m.box(1.35f, 0.05f, 0.18f, DSTEEL, x, 1.65f, z + s * 0.06f);
            }
        }
        for (float s : S2) {
            k.g.box(0.06f, 1.2f, 1.1f, GLASS, s * (W / 2 + 0.02f), 2.35f, -1.4f);
            b.box(0.05f, 1.35f, 1.25f, c("d8d3c4"), s * (W / 2 + 0.01f), 2.35f, -1.4f);
        }
        // крыльцо: ступени, площадка, козырёк на столбах, дверь, фонарь
        float zf = -D / 2;
        for (int i = 0; i < 3; i++) b.box(2.6f - i * 0.2f, 0.15f, 1.6f - i * 0.35f, CONC, 0, 0.075f + i * 0.15f, zf - 0.9f + i * 0.17f);
        b.box(2.8f, 0.12f, 1.8f, roof, 0, 3.15f, zf - 0.85f);
        for (float s : S2) m.cyl(0.06f, 0.06f, 2.7f, 8, DSTEEL, s * 1.25f, 1.8f, zf - 1.6f);
        b.box(1.3f, 2.2f, 0.06f, c("5a4430"), 0, 1.55f, zf - 0.02f);
        m.box(0.04f, 0.04f, 0.14f, STEEL, 0.45f, 1.5f, zf - 0.08f);
        k.l.box(0.3f, 0.15f, 0.15f, c("fff0c0"), 0, 2.95f, zf - 0.1f);
        b.box(0.5f, 0.32f, 0.03f, c("2a4a8a"), 0.95f, 2.4f, zf - 0.03f); // табличка
        // бруствер у входа
        bagWall(b, 0, zf - 1.0f, 3.0f, PI * 0.72f, PI * 1.28f, 3, R);
        // антенны: две мачты с растяжками и спутниковая тарелка на крыше
        for (float s : S2) {
            float ax = s * 4.4f, az = 2.4f;
            m.cyl(0.05f, 0.09f, 7f, 8, DSTEEL, ax, ry + 3.6f, az);
            for (int k2 = 0; k2 < 3; k2++) m.box(1.1f, 0.04f, 0.04f, DSTEEL, ax, ry + 4.6f + k2 * 1.6f, az, 0, k2 * 0.9f, 0);
            for (int g = 0; g < 3; g++) {
                float a = g * MathUtils.PI2 / 3 + 0.4f;
                m.seg(V(ax, ry + 6.4f, az), V(ax + MathUtils.cos(a) * 2.2f, ry + 0.2f, az + MathUtils.sin(a) * 1.5f), 0.008f, 0.008f, 4, c("9a9a92"));
            }
            k.l.box(0.16f, 0.16f, 0.16f, c("ff2a1a"), ax, ry + 7.2f, az);
        }
        float dx = -2.2f, dz = 1.6f;
        m.cyl(0.08f, 0.1f, 1.0f, 8, DSTEEL, dx, ry + 0.6f, dz);
        b.sph(1.15f, c("dedcd4"), dx, ry + 1.5f, dz, 1, 0.35f, 1, 20);
        m.seg(V(dx, ry + 1.45f, dz), V(dx - 0.15f, ry + 2.5f, dz - 0.75f), 0.025f, 0.025f, 6, DSTEEL);
        m.box(0.14f, 0.18f, 0.14f, DSTEEL, dx - 0.15f, ry + 2.55f, dz - 0.78f);
        // УАЗ-«буханка» у торца: кузов, окна, колёса
        float ux = 7.6f, uz = -0.5f;
        Color uaz = snow ? c("9aa094") : c("56603e");
        b.box(1.9f, 1.7f, 4.4f, uaz, ux, 1.25f, uz);
        b.box(1.8f, 0.35f, 0.5f, uaz, ux, 0.55f, uz - 2.35f);
        for (float s : S2) {
            k.g.box(0.03f, 0.5f, 3.3f, GLASS, ux + s * 0.96f, 1.75f, uz + 0.2f);
            truckWheel(k, ux + s * 0.85f, 0.42f, uz - 1.35f, 0.42f, 0.28f, s);
            truckWheel(k, ux + s * 0.85f, 0.42f, uz + 1.35f, 0.42f, 0.28f, s);
        }
        k.g.box(1.6f, 0.55f, 0.03f, GLASS, ux, 1.8f, uz - 2.21f);
        k.l.box(0.2f, 0.2f, 0.04f, c("fff3d0"), ux - 0.6f, 1.0f, uz - 2.25f);
        k.l.box(0.2f, 0.2f, 0.04f, c("fff3d0"), ux + 0.6f, 1.0f, uz - 2.25f);
        // дизель-генератор и бочки за зданием
        b.box(1.0f, 0.9f, 1.6f, OLV, -6.4f, 0.45f, 2.4f);
        m.cyl(0.05f, 0.05f, 0.5f, 6, DSTEEL, -6.2f, 1.1f, 2.9f);
        k.r.seg(V(-5.9f, 0.6f, 2.4f), V(-5.5f, 1.2f, 2.4f), 0.035f, 0.035f, 6, BLK);
        for (int i = 0; i < 3; i++) barrel(b, m, -6.4f + (i - 1) * 0.65f, 0, 3.8f, c("2f4f63"), false, 0);
        // маскировочная сеть над торцом на шестах
        Color[] net = snow ? new Color[]{c("e3e6e8"), c("c9ced2")} : new Color[]{c("4c5a32"), c("6b6a3e"), c("3e4a2a")};
        for (float s : S2) m.cyl(0.05f, 0.05f, 2.6f, 6, WOOD_D, -7.4f, 1.3f, s * 2.6f);
        for (int i = 0; i < 9; i++) b.box(1.4f + R.nextFloat() * 0.6f, 0.04f, 1.2f + R.nextFloat() * 0.6f, net[i % net.length],
                -6.4f + (R.nextFloat() - 0.5f) * 2.0f, 2.55f + R.nextFloat() * 0.1f, (R.nextFloat() - 0.5f) * 5.0f, 0, R.nextFloat(), (R.nextFloat() - 0.5f) * 0.2f);
    }

    // ================= 5. Станция РЭБ =================

    /** Станция РЭБ на шасси трёхосного грузовика: кунг, радиопрозрачный купол, телескопическая мачта с панелями. */
    static void ew(K k, boolean snow) {
        MB b = k.b, m = k.m;
        Color body = snow ? c("b8bcb2") : OLV;
        float fy = truck(k, -4.4f, 4.3f, body);
        kung(k, fy + 0.1f, -1.85f, 4.3f, 2.25f, body);
        float top = fy + 0.1f + 2.25f;
        // купол-обтекатель на постаменте
        m.cyl(0.85f, 0.95f, 0.3f, 24, DSTEEL, 0, top + 0.15f, -0.6f);
        b.sph(1.0f, c("d6d4c8"), 0, top + 0.55f, -0.6f, 1, 0.9f, 1, 20);
        // телескопическая мачта (секции сужаются) и решётка панельных антенн
        float mx = 0.4f, mz = 2.8f;
        float[] r = {0.2f, 0.16f, 0.12f, 0.09f};
        float y = top;
        for (int i = 0; i < 4; i++) {
            m.cyl(r[i], r[i], 1.2f, 12, i % 2 == 0 ? OLV_D : DSTEEL, mx, y + 0.6f, mz);
            m.cyl(r[i] + 0.03f, r[i] + 0.03f, 0.08f, 12, DSTEEL, mx, y + 1.18f, mz);
            y += 1.1f;
        }
        for (int i = 0; i < 4; i++) {
            float a = i * PI / 2 + 0.3f;
            float px = mx + MathUtils.sin(a) * 0.45f, pz = mz + MathUtils.cos(a) * 0.45f;
            m.seg(V(mx, y - 0.1f, mz), V(px, y - 0.1f, pz), 0.03f, 0.03f, 5, DSTEEL);
            b.box(0.55f, 0.95f, 0.08f, c("c9c7bb"), px, y - 0.2f, pz, 0, a, 0);
        }
        // штыревые антенны на крыше кунга (пучок разной высоты), ограждение
        float[][] whips = {{-0.9f, 0.3f, 2.8f}, {-0.9f, 1.3f, 3.6f}, {-0.4f, 3.6f, 2.2f}, {0.9f, 0.6f, 3.0f}, {1.0f, 1.6f, 2.4f}, {-1.0f, 2.4f, 1.8f}};
        for (float[] w : whips) {
            m.cyl(0.05f, 0.05f, 0.15f, 8, DSTEEL, w[0], top + 0.08f, w[1]);
            m.cyl(0.012f, 0.025f, w[2], 5, c("3a3a36"), w[0], top + 0.15f + w[2] / 2, w[1]);
        }
        for (float s : S2) m.cyl(0.025f, 0.025f, 6.0f, 6, YEL, s * 1.18f, top + 0.5f, 1.25f, PI / 2, 0, 0);
        for (float s : S2) for (int i = 0; i < 5; i++) m.cyl(0.02f, 0.02f, 0.5f, 5, YEL, s * 1.18f, top + 0.25f, -1.7f + i * 1.45f);
        // аутригеры
        for (float s : S2) for (float z : new float[]{-1.6f, 3.9f}) jack(k, s * 1.1f, 0.85f, z, s);
        // ящики ЗИП по бортам рамы, запасное колесо за кабиной
        for (float s : S2) {
            b.box(0.5f, 0.45f, 1.4f, OLV_D, s * 1.0f, fy - 0.35f, 0.6f);
            m.box(0.04f, 0.04f, 0.12f, STEEL, s * 1.26f, fy - 0.3f, 0.6f);
        }
        truckWheel(k, 0.0f, fy + 0.62f, -2.2f, 0.56f, 0.4f, 1);
        // проблесковый маяк и прожектор
        k.l.box(0.15f, 0.15f, 0.15f, c("ff8a10"), 0, top + 1.6f, -0.6f);
        m.box(0.3f, 0.25f, 0.25f, DSTEEL, -1.0f, top + 0.2f, -1.4f);
        k.l.box(0.22f, 0.18f, 0.03f, c("fff3d0"), -1.0f, top + 0.2f, -1.53f);
    }


    // ================= объекты кампании (3.3.1) =================

    /** Тентованный кузов: борта, дуги и брезент. */
    static void tentBody(K k, float y0, float z0, float z1, Color tarp) {
        float w = 2.45f, L = z1 - z0, zc = (z0 + z1) / 2;
        Color side = c("4a5532");
        k.b.box(w, 0.08f, L, DSTEEL, 0, y0, zc);
        for (float s : S2) k.b.box(0.06f, 0.6f, L, side, s * (w / 2 - 0.03f), y0 + 0.34f, zc);
        k.b.box(w, 0.6f, 0.06f, side, 0, y0 + 0.34f, z1 - 0.03f);
        // брезент: прямые бока и крыша со скосами
        k.b.extrude(new float[][]{{-w / 2, 0}, {w / 2, 0}, {w / 2, 1.35f}, {w / 2 - 0.25f, 1.6f}, {-w / 2 + 0.25f, 1.6f}, {-w / 2, 1.35f}}, L - 0.1f, tarp, 0, y0 + 0.62f, zc, 0, PI / 2, 0);
        Color dk = new Color(tarp).mul(0.8f, 0.8f, 0.8f, 1);
        int n = (int) (L / 1.1f);
        for (int i = 0; i <= n; i++) {
            float z = z0 + 0.1f + i * (L - 0.2f) / n;
            k.b.extrude(new float[][]{{-w / 2 - 0.02f, 1.3f}, {-w / 2 - 0.02f, 1.36f}, {-w / 2 + 0.25f, 1.63f}, {w / 2 - 0.25f, 1.63f}, {w / 2 + 0.02f, 1.36f}, {w / 2 + 0.02f, 1.3f}, {w / 2 - 0.24f, 1.57f}, {-w / 2 + 0.24f, 1.57f}},
                    0.06f, dk, 0, y0 + 0.62f, z, 0, PI / 2, 0);
        }
        for (float s : S2) for (int i = 0; i < 6; i++) k.m.box(0.03f, 0.18f, 0.03f, BLK, s * (w / 2 + 0.01f), y0 + 0.7f, z0 + 0.3f + i * (L - 0.6f) / 5);
        k.b.box(w - 0.1f, 1.3f, 0.03f, dk, 0, y0 + 1.3f, z1 - 0.01f);
    }

    /** Цистерна-бензовоз: эллиптический бак, переливные отсеки, лестница, «ОГНЕОПАСНО». */
    static void tankBody(K k, float y0, float z0, float z1) {
        float L = z1 - z0, zc = (z0 + z1) / 2;
        Color tc = c("8d9474");
        k.m.box(1.4f, 0.2f, L, DSTEEL, 0, y0 + 0.05f, zc);
        k.b.cyl(1.1f, 1.1f, L - 0.4f, 28, tc, 0, y0 + 1.1f, zc, PI / 2, 0, 0);
        for (float s : S2) k.b.sph(1.1f, new Color(tc).mul(0.94f, 0.94f, 0.94f, 1), 0, y0 + 1.1f, zc + s * (L / 2 - 0.2f), 1, 1, 0.25f);
        for (int i = 0; i < 3; i++) {
            float z = z0 + 1.0f + i * (L - 2f) / 2f;
            k.m.cyl(0.28f, 0.28f, 0.18f, 14, STEEL, 0, y0 + 2.25f, z);
            k.m.cyl(1.13f, 1.13f, 0.05f, 28, DSTEEL, 0, y0 + 1.1f, z, PI / 2, 0, 0);
        }
        k.m.box(0.5f, 0.04f, L - 0.6f, DSTEEL, 0.55f, y0 + 2.22f, zc);
        for (float s : S2) {
            k.b.box(0.02f, 0.3f, 1.4f, RED, s * 1.1f, y0 + 1.1f, zc);
            k.b.box(0.025f, 0.08f, 1.2f, WHITE, s * 1.11f, y0 + 1.1f, zc);
            k.m.box(0.4f, 0.4f, L - 0.6f, OLV_D, s * 0.95f, y0 + 0.3f, zc);
        }
        for (float e : new float[]{-0.25f, 0.25f}) k.m.seg(V(e, 0.5f, z1 + 0.15f), V(e, y0 + 2.2f, z1 - 0.2f), 0.025f, 0.025f, 6, DSTEEL);
        for (int i = 0; i < 6; i++) k.m.box(0.5f, 0.03f, 0.05f, DSTEEL, 0, 0.6f + i * (y0 + 1.5f) / 6f, z1 + 0.12f - i * 0.05f);
        k.m.cyl(0.08f, 0.08f, 0.6f, 8, DSTEEL, 0.6f, y0 - 0.1f, z1 - 0.1f, PI / 2, 0, 0);
    }

    static int truckVar;

    /** Грузовик колонны: по очереди тентованный, бензовоз, с кунгом. */
    public static Node3 convoyTruck() {
        boolean hq0 = MB.hq; MB.hq = false;
        K k = new K();
        int v = truckVar++ % 3;
        Color body = c("4f5a36");
        float fy = truck(k, -4.0f, 4.0f, body);
        if (v == 0) tentBody(k, fy + 0.15f, -1.6f, 4.0f, c("5d6640"));
        else if (v == 1) tankBody(k, fy + 0.1f, -1.6f, 4.0f);
        else kung(k, fy + 0.15f, -1.55f, 4.0f, 2.1f, body);
        // запасное колесо за кабиной, номер-полоса на двери
        truckWheel(k, 0.6f, fy + 0.62f, -2.15f, 0.56f, 0.38f, 1);
        Node3 n = k.node();
        MB.hq = hq0;
        return n;
    }

    /** Бетонная гравитационная плотина 220 м: секции, водосброс с затворами и струями, ГЭС у подножия, кран на гребне. */
    public static Node3 dam() {
        boolean hq0 = MB.hq; MB.hq = false;
        K k = new K();
        MB b = k.b, m = k.m;
        Color con = c("9a968c"), conD = c("7f7b72"), conL = c("aaa69b");
        float Wd = 220f, base = -4f, H = 28f;
        // тело: верховая грань почти вертикальная (−Z, к воде), низовая — откос к игроку (+Z)
        float[][] prof = {{6, 0}, {-12, 0}, {-3.5f, H - 3}, {-3.5f, H}, {4, H}, {5, H - 3}};
        b.extrude(prof, Wd, con, 0, base, 0);
        // секционные швы на низовой грани (каждые 15 м), полосы потёков
        for (int i = -7; i <= 7; i++) {
            float x = i * 15f;
            b.box(0.25f, H * 1.04f, 0.3f, conD, x, base + H / 2, 4f + (H / 2) * 0.0f + 0.0f, -0.3f, 0, 0);
        }
        for (int i = 0; i < 18; i++) {
            float x = -105 + i * 12.3f;
            b.box(1.2f + (i % 3) * 0.5f, H * 0.7f, 0.1f, i % 2 == 0 ? c("85827a") : c("8e8a80"), x, base + H * 0.45f, 3.6f + 0.0f, -0.31f, 0, 0);
        }
        // водосброс в центре: 3 пролёта между быками, сегментные затворы, вода по откосу
        float sw = 30f;
        for (int i = 0; i <= 3; i++) {
            float x = -sw / 2 + i * sw / 3;
            b.box(2.2f, 8f, 11f, conL, x, base + H + 2f, 0.5f);
            b.box(2.6f, 0.8f, 11.4f, conD, x, base + H + 6.2f, 0.5f);
        }
        for (int i = 0; i < 3; i++) {
            float x = -sw / 2 + (i + 0.5f) * sw / 3;
            m.box(sw / 3 - 2.2f, 5f, 0.4f, c("4a5560"), x, base + H + 2.2f, -2.5f);
            for (int j = 0; j < 4; j++) m.box(sw / 3 - 2.2f, 0.2f, 0.25f, DSTEEL, x, base + H + 0.6f + j * 1.25f, -2.75f);
            // струя по откосу и пена внизу
            k.l.box(sw / 3 - 2.6f, 0.15f, 27f, c("9fc8e0"), x, base + H * 0.5f - 0.6f, 8.2f, -1.08f, 0, 0);
            k.l.box(sw / 3 - 2.6f, 1.2f, 6f, c("e6f2f8"), x, base + 0.6f, 16f);
        }
        // служебный мост над водосбросом и машинные будки затворов
        b.box(sw + 2.4f, 0.6f, 4f, conD, 0, base + H + 6.7f, 0.5f);
        for (int i = 0; i < 3; i++) {
            float x = -sw / 2 + (i + 0.5f) * sw / 3;
            b.box(4f, 2.6f, 3f, c("b9b2a0"), x, base + H + 8.3f, 0.5f);
            b.extrude(new float[][]{{-1.7f, 0}, {1.7f, 0}, {0, 0.8f}}, 4.2f, c("6a4a3a"), x, base + H + 9.6f, 0.5f);
            k.g.box(1.2f, 0.7f, 0.05f, GLASS, x, base + H + 8.6f, 2.03f);
        }
        // гребень: дорога, парапеты-ограждения, фонари
        b.box(Wd, 0.2f, 6.6f, c("4c4c48"), 0, base + H + 0.1f, 0.25f);
        for (float zz : new float[]{-3.4f, 3.9f}) {
            b.box(Wd, 0.9f, 0.3f, conL, 0, base + H + 0.55f, zz);
            for (int i = -9; i <= 9; i++) if (Math.abs(i * 11f) > sw / 2 + 3) {
                m.cyl(0.12f, 0.15f, 6f, 8, DSTEEL, i * 11f, base + H + 3.5f, zz);
                m.seg(V(i * 11f, base + H + 6.4f, zz), V(i * 11f, base + H + 6.6f, zz + (zz > 0 ? -1.2f : 1.2f)), 0.08f, 0.08f, 6, DSTEEL);
                k.l.box(0.6f, 0.2f, 0.4f, c("fff0c0"), i * 11f, base + H + 6.5f, zz + (zz > 0 ? -1.3f : 1.3f));
            }
        }
        // козловой кран на гребне
        float cx = -48f;
        for (float e : new float[]{-6f, 6f}) for (float zz : new float[]{-2.8f, 3.3f}) m.box(0.6f, 12f, 0.6f, YEL, cx + e, base + H + 6.2f, zz);
        for (float zz : new float[]{-2.8f, 3.3f}) m.box(14f, 1.2f, 0.8f, YEL, cx, base + H + 12.4f, zz);
        m.box(3f, 2f, 7f, c("c89a20"), cx + 2f, base + H + 11.6f, 0.25f);
        for (int i = 0; i < 6; i++) m.seg(V(cx - 6f + i * 2.4f, base + H + 0.6f, -2.8f), V(cx - 4.8f + i * 2.4f, base + H + 11.8f, -2.8f), 0.12f, 0.12f, 5, YEL);
        // ГЭС у подножия справа: машзал с окнами, трансформаторы, отводящий канал
        float px = 55f, pz = 14f;
        b.box(36f, 13f, 12f, c("b5ad9a"), px, base + 6.5f, pz);
        b.box(36.6f, 1f, 12.6f, conD, px, base + 13.5f, pz);
        for (int i = 0; i < 8; i++) k.g.box(2.6f, 6f, 0.1f, GLASS, px - 15.5f + i * 4.4f, base + 7.5f, pz + 6.02f);
        for (int i = 0; i < 9; i++) b.box(0.5f, 12f, 0.3f, c("a39b88"), px - 17.6f + i * 4.4f, base + 6.5f, pz + 6.1f);
        b.box(30f, 1.5f, 0.2f, c("2a4a8a"), px, base + 11.8f, pz + 6.1f);
        for (int i = 0; i < 3; i++) {
            float tx = px - 12f + i * 12f, tz = pz + 11f;
            b.box(4f, 3.5f, 3f, c("6a7060"), tx, base + 1.75f + 1f, tz);
            for (int f = 0; f < 6; f++) for (float e : S2) m.box(0.1f, 2.6f, 2.2f, DSTEEL, tx + e * (2.1f + f * 0.0f), base + 2.6f, tz - 1.2f + f * 0.45f);
            for (int u = 0; u < 3; u++) m.cyl(0.18f, 0.25f, 2.2f, 8, c("8a5a3a"), tx - 1.2f + u * 1.2f, base + 5.6f, tz);
            m.cyl(0.6f, 0.6f, 3f, 14, c("6a7060"), tx, base + 5.2f, tz + 1.2f, 0, 0, PI / 2);
        }
        k.l.box(30f, 0.6f, 14f, c("2f6a8a"), px, base + 0.4f, pz + 16f);
        Node3 n = k.node();
        MB.hq = hq0;
        return n;
    }

    /** Подстанция: гравийная площадка, ограда, 3 трансформатора с радиаторами и вводами, порталы с шинами, ОПУ. */
    public static Node3 substation() {
        boolean hq0 = MB.hq; MB.hq = false;
        K k = new K();
        MB b = k.b, m = k.m;
        Random R = new Random(91);
        Color gal = c("8c9296"), porc = c("6e4a34"), tr = c("6b7562");
        b.box(34f, 0.2f, 18f, c("8a877e"), 0, 0.1f, 0);
        // ограда: столбы и сетка, ворота спереди
        for (int i = 0; i <= 17; i++) for (float s : S2) {
            float x = -17f + i * 2f;
            m.cyl(0.06f, 0.06f, 2.4f, 6, gal, x, 1.2f, s * 9f);
        }
        for (int i = 0; i <= 9; i++) for (float s : S2) m.cyl(0.06f, 0.06f, 2.4f, 6, gal, s * 17f, 1.2f, -9f + i * 2f);
        // сетка-рабица: частые горизонтальные проволоки и колючка поверху (сквозь неё видно оборудование)
        for (float s : S2) {
            for (int i = 0; i < 9; i++) {
                float y = 0.15f + i * 0.27f;
                m.box(34f, 0.025f, 0.025f, gal, 0, y, s * 9f);
                m.box(0.025f, 0.025f, 18f, gal, s * 17f, y, 0);
            }
            for (int i = 0; i < 3; i++) {
                m.box(34f, 0.02f, 0.02f, c("5a5e60"), 0, 2.45f + i * 0.12f, s * (9f + i * 0.1f));
                m.box(0.02f, 0.02f, 18f, c("5a5e60"), s * (17f + i * 0.1f), 2.45f + i * 0.12f, 0);
            }
        }
        for (int i = 0; i < 4; i++) { b.box(0.7f, 0.5f, 0.03f, YEL, -12f + i * 8f, 1.6f, 9.05f); b.box(0.4f, 0.08f, 0.032f, BLK, -12f + i * 8f, 1.6f, 9.06f); }
        // трансформаторы
        for (int i = 0; i < 3; i++) {
            float x = -9f + i * 9f, z = 2.5f;
            b.box(5.6f, 0.5f, 3.6f, CONC_D, x, 0.45f, z);
            b.box(3.6f, 3.0f, 2.6f, tr, x, 2.2f, z);
            b.box(3.8f, 0.2f, 2.8f, new Color(tr).mul(0.85f, 0.85f, 0.85f, 1), x, 3.75f, z);
            for (float s : S2) for (int f = 0; f < 7; f++) m.box(0.06f, 2.4f, 0.9f, DSTEEL, x + s * (1.9f + f * 0.0f) + s * f * 0.0f, 2.1f, z - 1.35f + f * 0.45f);
            for (float s : S2) b.box(0.6f, 2.5f, 2.9f, new Color(tr).mul(0.9f, 0.9f, 0.9f, 1), x + s * 2.25f, 2.1f, z);
            // расширительный бак, газовое реле, вводы ВН из «тарелочек»
            b.cyl(0.45f, 0.45f, 2.6f, 14, tr, x, 4.6f, z + 1.0f, 0, 0, PI / 2);
            m.seg(V(x, 3.85f, z + 0.6f), V(x, 4.4f, z + 1.0f), 0.06f, 0.06f, 6, DSTEEL);
            for (int u = 0; u < 3; u++) {
                float bx = x - 1.1f + u * 1.1f;
                for (int d = 0; d < 8; d++) m.cyl(0.24f, 0.24f, 0.08f, 12, porc, bx, 4.1f + d * 0.22f, z - 0.4f);
                m.cyl(0.12f, 0.12f, 1.9f, 8, porc, bx, 4.9f, z - 0.4f);
                m.cyl(0.08f, 0.08f, 0.3f, 6, gal, bx, 5.95f, z - 0.4f);
            }
            b.box(1.2f, 0.05f, 2.2f, c("2a2620"), x + 2.0f, 0.22f, z + 0.3f); // пятно масла
        }
        // порталы: решётчатые стойки и траверса, гирлянды изоляторов, провода к вводам
        for (float z : new float[]{-4.5f}) {
            for (float x : new float[]{-15f, 15f}) {
                for (float e : S2) for (float f : S2) m.seg(V(x + e * 0.6f, 0.2f, z + f * 0.6f), V(x + e * 0.25f, 11f, z + f * 0.25f), 0.07f, 0.07f, 5, gal);
                for (int h = 1; h < 8; h++) m.box(1.0f - h * 0.08f, 0.06f, 0.06f, gal, x, h * 1.4f, z + 0.5f - h * 0.04f);
            }
            m.box(31f, 0.6f, 0.6f, gal, 0, 11f, z);
            for (int i = 0; i < 12; i++) m.seg(V(-14.5f + i * 2.6f, 10.7f, z), V(-13.2f + i * 2.6f, 11.3f, z), 0.04f, 0.04f, 4, gal);
            for (int i = 0; i < 3; i++) for (int u = 0; u < 3; u++) {
                float x = -9f + i * 9f - 1.1f + u * 1.1f;
                for (int d = 0; d < 6; d++) m.cyl(0.16f, 0.16f, 0.06f, 10, porc, x, 10.6f - d * 0.18f, z);
                m.seg(V(x, 9.5f, z), V(x, 6.0f, 2.1f), 0.03f, 0.03f, 4, BLK);
                m.seg(V(x, 9.5f, z), V(-9f + i * 9f + 2.5f * (u - 1), 13.6f, -20f), 0.03f, 0.03f, 4, BLK);
            }
        }
        // опоры уходящих ЛЭП за оградой: стойка с траверсой и гирляндами
        for (int i = 0; i < 3; i++) {
            float x = -9f + i * 9f, z = -20f;
            for (float e : S2) m.seg(V(x + e * 1.4f, 0, z), V(x + e * 0.5f, 14f, z), 0.12f, 0.08f, 6, gal);
            m.box(6.5f, 0.35f, 0.35f, gal, x, 14f, z);
            for (int h = 1; h < 6; h++) m.box(2.6f - h * 0.3f, 0.06f, 0.06f, gal, x, h * 2.3f, z);
            for (int u = 0; u < 3; u++) for (int d = 0; d < 4; d++) m.cyl(0.14f, 0.14f, 0.06f, 8, porc, x + 2.5f * (u - 1), 13.8f - d * 0.16f, z);
        }
        // разъединители на опорах
        for (int i = 0; i < 6; i++) {
            float x = -12f + i * 4.8f, z = -7f;
            b.box(0.6f, 2.2f, 0.6f, CONC, x, 1.1f, z);
            for (int d = 0; d < 5; d++) m.cyl(0.18f, 0.18f, 0.07f, 10, porc, x, 2.4f + d * 0.18f, z);
            m.box(1.4f, 0.08f, 0.1f, gal, x, 3.4f, z, 0, 0, 0.3f);
        }
        // ОПУ — пункт управления с дверью и окнами
        float ox = 12.5f, oz = 6.4f;
        b.box(6f, 3.2f, 3.6f, c("b9b29c"), ox, 1.8f, oz);
        b.box(6.4f, 0.25f, 4f, c("4a4a46"), ox, 3.5f, oz);
        b.box(0.9f, 2.0f, 0.05f, c("4f5640"), ox - 1.8f, 1.2f, oz - 1.82f);
        for (int i = 0; i < 2; i++) k.g.box(1.0f, 0.8f, 0.05f, GLASS, ox + 0.2f + i * 1.6f, 2.2f, oz - 1.82f);
        k.l.box(0.25f, 0.12f, 0.12f, c("fff0c0"), ox - 1.8f, 2.4f, oz - 1.86f);
        // кабельные лотки
        for (int i = 0; i < 3; i++) b.box(0.6f, 0.15f, 3.6f, CONC_D, -9f + i * 9f, 0.25f, 5.6f);
        b.box(22f, 0.15f, 0.6f, CONC_D, 0.5f, 0.25f, 7.4f);
        // мешки у ОПУ
        bagWall(b, ox, oz - 3f, 2.4f, PI * 0.7f, PI * 1.3f, 2, R);
        Node3 n = k.node();
        MB.hq = hq0;
        return n;
    }

    // ================= сборка =================

    /** Возвращает {узел, вращающаяся часть или null, r, hr}. */
    static Object[] build(String kind, String theme) {
        boolean snow = "snow".equals(theme);
        Random R = new Random(kind.hashCode() * 31L + 7);
        boolean hq0 = MB.hq;
        // без скруглённых рёбер и с базовым числом сегментов: с высоты дрона разницы не видно, а треугольников вдвое меньше
        MB.hq = false;
        K k = new K();
        Node3 spin = null;
        float r, hr;
        switch (kind) {
            case "depot": depot(k, snow, R); r = 8; hr = 2.5f; break;
            case "hq": hq(k, snow, R); r = 7.5f; hr = 2f; break;
            case "ew": ew(k, snow); r = 5; hr = 2.5f; break;
            case "radar": {
                radarBase(k, snow);
                K s = new K();
                radarSpin(s);
                spin = s.node();
                spin.pos.set(0, 5.3f, 2.3f);
                r = 4.5f; hr = 3f;
                break;
            }
            default: fuel(k, snow, R); r = 8; hr = 2.4f; break;
        }
        Node3 g = k.node();
        if (spin != null) g.add(spin);
        MB.hq = hq0;
        return new Object[]{g, spin, r, hr};
    }

    private Targets() { }
}
