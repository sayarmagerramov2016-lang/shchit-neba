package ru.shitneba;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;

import ru.shitneba.gfx.Node3;

/**
 * Отладка (только ПК): «фотостудия» — по очереди ставит каждую модель игры в меню-мир
 * и снимает её с двух сторон. Запуск: третий аргумент {@code models}.
 * Необязательный SN_ONLY=tor,quad — снять только эти модели.
 */
public final class Studio {
    public static final class Item {
        public final String group, id, title;
        Item(String group, String id, String title) { this.group = group; this.id = id; this.title = title; }
    }

    public final Array<Item> items = new Array<>();
    public final Node3 root = new Node3();
    final Vector3 center = new Vector3();
    float radius = 5f;
    Node3 spin;
    /** Точка съёмки: зданиям нужно чистое место в стороне от подиума меню. */
    float ox, oz;

    public Studio() {
        JsonValue D = G.D;
        String only = System.getenv("SN_ONLY");
        Array<Item> all = new Array<>();
        for (String v : G.VEHS) all.add(new Item("veh", v, D.get("VEHICLES").get(v).getString("name", v)));
        String[][] tg = {{"depot", "Склад"}, {"fuel", "Бензохранилище"}, {"radar", "Радар"}, {"hq", "Штаб"}, {"ew", "Станция РЭБ"}};
        for (String[] k : tg) all.add(new Item("target", k[0], k[1]));
        String[][] ob = {{"truck", "Грузовик колонны"}, {"truck", "Грузовик колонны"}, {"truck", "Грузовик колонны"}, {"sub", "Подстанция"}, {"dam", "Плотина"}};
        for (String[] k : ob) all.add(new Item("obj", k[0], k[1]));
        for (Item it : all) {
            if (only == null) { items.add(it); continue; }
            for (String o : only.split(",")) if (o.trim().equals(it.id)) items.add(it);
        }
    }

    /** Поставить модель №i в начало координат. */
    public void show(int i, World world) {
        while (root.kids.size > 0) root.kids.peek().remove();
        Item it = items.get(i);
        Node3 g;
        float lift = 0;
        spin = null;
        switch (it.group) {
            case "veh": {
                Models.Vehicle v = Models.vehicle(it.id, "olive");
                v.turret.setEuler(0, 0.35f, 0);
                v.pitch.setEuler(0.3f, 0, 0);
                g = v.root;
                lift = 0.25f;
                break;
            }
            case "enemy": {
                Models.EnemyModel m = Models.enemy(it.id, "harpy");
                g = m.g;
                lift = Game.isGround(it.id) ? 0 : -1; // -1 = подвесить над землёй
                break;
            }
            case "drone": g = SModels.drone(it.id).g; lift = -1; break;
            case "target": g = SModels.target(it.id, world.themeName).g; break;
            case "obj": g = Objs.make(it.id).g; break;
            default:
                if (it.id.equals("missile")) {
                    g = new Node3(Models.missileMesh(), Models.MAT_MISSILE);
                    g.castShadow = true;
                    lift = -1;
                } else g = Models.mog().g;
                break;
        }
        root.add(g);
        boolean big = it.group.equals("target") || it.group.equals("obj");
        ox = big ? 0 : 0; oz = big ? -70 : 0;
        String sp = System.getenv("SN_SPOT");
        if (big && sp != null) { String[] a = sp.split(","); ox = Float.parseFloat(a[0]); oz = Float.parseFloat(a[1]); }
        g.pos.set(ox, 0, oz);
        root.updateGlobal(null);
        measure(g);
        float ground = world.height(ox, oz);
        if (lift < 0) g.pos.y = ground + Math.max(2f, radius * 0.9f) - center.y;
        else g.pos.y = ground + lift;
        root.updateGlobal(null);
        measure(g);
        g.castShadow = true;
        g.forEach(n -> n.castShadow = n.mesh != null);
    }

    void measure(Node3 g) {
        final float[] bb = {1e9f, 1e9f, 1e9f, -1e9f, -1e9f, -1e9f};
        final Vector3 c = new Vector3(), s = new Vector3();
        g.forEach(n -> {
            if (n.mesh == null || !n.visible || n.mesh.count == 0) return;
            n.toGlobal(n.mesh.center, c);
            n.global.getScale(s);
            float r = n.mesh.radius * Math.max(s.x, Math.max(s.y, s.z));
            bb[0] = Math.min(bb[0], c.x - r); bb[1] = Math.min(bb[1], c.y - r); bb[2] = Math.min(bb[2], c.z - r);
            bb[3] = Math.max(bb[3], c.x + r); bb[4] = Math.max(bb[4], c.y + r); bb[5] = Math.max(bb[5], c.z + r);
        });
        if (bb[0] > bb[3]) { center.setZero(); radius = 3; return; }
        center.set((bb[0] + bb[3]) / 2, (bb[1] + bb[4]) / 2, (bb[2] + bb[5]) / 2);
        radius = 0.5f * (float) Math.sqrt((bb[3] - bb[0]) * (bb[3] - bb[0]) + (bb[4] - bb[1]) * (bb[4] - bb[1]) + (bb[5] - bb[2]) * (bb[5] - bb[2]));
        radius = Math.max(radius * 0.8f, 0.5f);
    }

    /** Ракурс: 0 — спереди-слева, 1 — сзади-справа, 2 — сбоку крупно. */
    public void aim(PerspectiveCamera cam, int view) {
        float yaw = view == 0 ? -0.75f : (view == 1 ? MathUtils.PI - 0.75f : -MathUtils.PI / 2);
        float d = radius / MathUtils.sin(cam.fieldOfView * MathUtils.degreesToRadians * 0.5f) * (view == 2 ? 0.5f : 0.62f);
        float el = view == 2 ? 0.1f : 0.32f;
        // вперёд у моделей — это -Z
        cam.position.set(center.x + MathUtils.sin(yaw) * MathUtils.cos(el) * d, center.y + MathUtils.sin(el) * d, center.z - MathUtils.cos(yaw) * MathUtils.cos(el) * d);
        cam.direction.set(center).sub(cam.position).nor();
        cam.up.set(Vector3.Y);
        cam.near = Math.max(0.05f, d * 0.02f);
        cam.update();
    }
}
