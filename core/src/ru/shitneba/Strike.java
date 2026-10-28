package ru.shitneba;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import ru.shitneba.gfx.Dyn;
import ru.shitneba.gfx.Mat;
import ru.shitneba.gfx.Node3;

/** Режим «Рой»: дрон-камикадзе прорывает зону ПВО. Порт strike.gd. */
public class Strike {
    // ---------- справочники ----------
    public static final class DroneDef {
        public final String key, name, desc; public final int price;
        public final float sp, boost, min, turn, hp, dmg, rad, bat; public final int flares, bombs; public final float sig; public final boolean wing;
        DroneDef(String key, String name, String desc, int price, float sp, float boost, float min, float turn, float hp, float dmg, float rad, float bat, int flares, int bombs, float sig, boolean wing) {
            this.key = key; this.name = name; this.desc = desc; this.price = price; this.sp = sp; this.boost = boost; this.min = min; this.turn = turn; this.hp = hp;
            this.dmg = dmg; this.rad = rad; this.bat = bat; this.flares = flares; this.bombs = bombs; this.sig = sig; this.wing = wing;
        }
    }

    public static final Map<String, DroneDef> DRONES = new LinkedHashMap<>();
    public static final String[][] UPS = {{"war", "Боевая часть", "+12% урона и +6% радиуса взрыва за уровень"}, {"bat", "Аккумулятор", "+15% времени полёта и +3% скорости ускорения"},
            {"hull", "Корпус", "+20% прочности за уровень"}, {"ecm", "Ловушки", "Меньше заметность, ловушки надёжнее; +1 ловушка на 2 и 4 уровне"}};
    public static final int[] UP_COST = {300, 700, 1300, 2200, 3500};
    static final int START_STOCK = 8;

    static {
        DRONES.put("osa", new DroneDef("osa", "Оса", "FPV-квадрокоптер. Быстрый и вёрткий, умеет зависать. Боевая часть небольшая.", 0, 34, 56, 6, 2.6f, 65, 190, 9, 75, 2, 0, 0.9f, false));
        DRONES.put("strizh", new DroneDef("strizh", "Стриж", "Крылатый барражирующий боеприпас. Не зависает, но быстрый и малозаметный. В пике бьёт сильнее.", 1800, 46, 72, 30, 1.5f, 65, 250, 11, 110, 2, 0, 0.8f, true));
        DRONES.put("taran", new DroneDef("taran", "Таран", "Тяжёлый бронированный гексакоптер. Медленный и заметный, но держит удар, а взрыв сносит всё вокруг.", 3200, 25, 37, 5, 1.7f, 180, 460, 15, 85, 3, 0, 1.4f, false));
        DRONES.put("shershen", new DroneDef("shershen", "Шершень", "Носитель: сбрасывает три бомбы, а потом сам идёт на таран. Три удара за один вылет.", 4500, 30, 46, 5, 2f, 85, 150, 9, 95, 2, 3, 1.1f, false));
    }

    static final class AAD { float hp, det, gun, rate, gdmg, spread, mcd, lock, turn, laser, ldps, flak, frate, fdmg, frad; int mis; }

    static final Map<String, AAD> AA = new LinkedHashMap<>();

    static AAD aa(float hp, float det, float turn) { AAD a = new AAD(); a.hp = hp; a.det = det; a.turn = turn; return a; }

    static {
        AAD p = aa(260, 430, 2.2f); p.gun = 300; p.rate = 9; p.gdmg = 4; p.spread = 0.022f; p.mis = 1; p.mcd = 6.5f; p.lock = 1.6f; AA.put("pantsir", p);
        AAD s = aa(200, 290, 3f); s.gun = 270; s.rate = 16; s.gdmg = 3; s.spread = 0.03f; AA.put("shilka", s);
        AAD t = aa(240, 540, 1.6f); t.mis = 2; t.mcd = 3.8f; t.lock = 1.3f; AA.put("tor", t);
        AAD l = aa(220, 360, 1.4f); l.laser = 320; l.ldps = 24; AA.put("luch", l);
        AAD g = aa(280, 420, 1.2f); g.flak = 440; g.frate = 1.3f; g.fdmg = 24; g.frad = 8; AA.put("grom", g);
    }

    static final String[][] TG = {{"depot", "Склад", "320", "300", ""}, {"radar", "РЛС", "200", "250", "а"}, {"hq", "Штаб", "480", "450", ""},
            {"ew", "Станция РЭБ", "220", "280", "а"}, {"fuel", "Цистерны", "160", "200", "ы"}};

    static String[] tg(String k) { for (String[] t : TG) if (t[0].equals(k)) return t; return TG[0]; }

    public static String tgName(String k) { return tg(k)[1]; }

    // ---------- объекты ----------
    public static final class Obj {
        public String kind; public Node3 g, spin, turret, pitch, radar; public final Vector3 pos = new Vector3();
        public float hp, max, r, hr, ry, yaw, pit = 0.3f, track, cd, mcd, burst, rest, heat;
        public boolean alive = true, unit, see, over, beam, known;
        public Array<Vector3> barrels, noses; int bi;
    }

    static final class Mis { Node3 node, flame; SmokeTrail trail; final Vector3 pos = new Vector3(), vel = new Vector3(), last = new Vector3(); float sp, life, age; Flare tgt; boolean atDrone; Obj src; }

    static final class Flare { final Vector3 pos = new Vector3(), vel = new Vector3(); float life; }

    static final class Bomb { Node3 g; final Vector3 pos = new Vector3(), vel = new Vector3(); }

    static final class Shell { final Vector3 pos = new Vector3(), prev = new Vector3(), o = new Vector3(), vel = new Vector3(); float life, dmg, rad; boolean flak; }

    public static final class Hunter { Node3 g, rot; public final Vector3 pos = new Vector3(), vel = new Vector3(); float ph; public boolean chase, alive = true; }

    public static final class DroneState {
        public Node3 g; Array<Node3> rot, bombs; public float r; public final Vector3 pos = new Vector3(), vel = new Vector3();
        public float yaw, pitch, roll, sp, hp, max, bat, bmax; public int flares, nb; public boolean alive, fall;
    }

    public static final class Stats { public float sp, boost, min, turn, hp, dmg, rad, bat, sig, decoy; public int flares, bombs; public boolean wing; }

    public static final class Feed { public String t; public Color c; public float life; }

    // ---------- состояние ----------
    final Game game;
    final World world;
    final PerspectiveCamera cam;
    public final Node3 root3 = new Node3();
    public boolean active, paused;
    public String st = "";
    public float T;
    public boolean bot;
    public int zone, score, stock, aaKills, tgKills, sorties, chain;
    public float chainT;
    public final Array<Obj> units = new Array<>(), targets = new Array<>();
    final Array<Shell> shells = new Array<>();
    final Array<Mis> mis = new Array<>();
    final Array<Flare> flares = new Array<>();
    final Array<Bomb> bombs = new Array<>();
    public final Array<Hunter> hunters = new Array<>();
    public DroneState D = new DroneState();
    public String DK = "osa";
    public Stats S = new Stats();
    public final Vector3 launchP = new Vector3();
    float launchYaw;
    float boomT, clearT;
    final Vector3 boomP = new Vector3();
    public int camMode;
    public float alert, lockWarn, laserWarn, jam;
    boolean radAlive;
    public String big = "", bigSub = "";
    public float bigT;
    public final Array<Feed> feed = new Array<>();
    public float inX, inY, mdX, mdY;
    public boolean inBoost, inBrake;
    public float shake, hitT, flareCd;
    public float fade;
    int fadeHold, zoneSorties0;
    public boolean resRec, resRankUp;
    public int resCoins, resXp;
    float lateOverT = -1, lateBlastT = -1;
    /** Игрок нажал «Завершить забег»: после взрыва — сразу итог, без нового рубежа. */
    boolean ending;
    final Vector3 lateBlastP = new Vector3();
    public GameUI ui;
    final Random R = new Random();
    public final Dyn tracers, beamDyn;
    final Vector3 camSmooth = new Vector3();

    public Strike(Game g, World w, PerspectiveCamera cam) {
        this.game = g; this.world = w; this.cam = cam;
        root3.tag = FX.ROOT_TAG;
        Mat tm = new Mat(); tm.unshaded = true; tm.vcol = true; tm.blend = Mat.ADD; tm.depthWrite = false; tm.cull = false; tm.fog = false; tm.order = 4;
        tracers = new Dyn(tm);
        Mat bm = new Mat(); bm.unshaded = true; bm.vcol = true; bm.blend = Mat.ADD; bm.depthWrite = false; bm.cull = false; bm.fog = false; bm.order = 4;
        beamDyn = new Dyn(bm);
    }

    float rr(float a, float b) { return a + R.nextFloat() * (b - a); }

    static float moveToward(float a, float b, float d) { return Math.abs(b - a) <= d ? b : a + Math.signum(b - a) * d; }

    static float angDiff(float from, float to) {
        float d = (to - from) % MathUtils.PI2;
        if (d > MathUtils.PI) d -= MathUtils.PI2;
        if (d < -MathUtils.PI) d += MathUtils.PI2;
        return d;
    }

    public static Stats calcStats(String k) {
        DroneDef b = DRONES.get(k);
        int w = up("war"), ba = up("bat"), h = up("hull"), e = up("ecm");
        Stats o = new Stats();
        o.sp = b.sp; o.min = b.min; o.turn = b.turn; o.wing = b.wing; o.bombs = b.bombs;
        o.dmg = b.dmg * (1 + 0.12f * w); o.rad = b.rad * (1 + 0.06f * w);
        o.bat = b.bat * (1 + 0.15f * ba); o.boost = b.boost * (1 + 0.03f * ba);
        o.hp = b.hp * (1 + 0.2f * h);
        o.flares = b.flares + (e >= 2 ? 1 : 0) + (e >= 4 ? 1 : 0);
        o.sig = b.sig * (1 - 0.05f * e); o.decoy = 0.7f + 0.05f * e;
        return o;
    }

    public static int up(String k) { Integer v = G.SV.swUp.get(k); return v == null ? 0 : v; }

    String vehSkin() {
        switch (world.themeName) {
            case "desert": return "desert";
            case "snow": return "winter";
            case "autumn": return "autumn";
            case "night": return "night";
            default: return "olive";
        }
    }

    static void noShadow(Node3 n) { n.forEach(k -> k.castShadow = false); }

    // ---------- старт/выход ----------
    public void start() {
        DK = G.SV.swOwn.contains(G.SV.swPick) ? G.SV.swPick : "osa";
        S = calcStats(DK);
        camMode = G.SV.swCam;
        game.clearBattle();
        game.mode = "strike"; game.state = "strike";
        if (game.vm != null) game.vm.root.visible = false;
        for (Models.Mog m : game.mogs) m.g.remove();
        game.mogs.clear();
        int n = G.D.get("THEMES").size;
        int ti = R.nextInt(n);
        while (G.D.get("THEMES").get(ti).getString("name").equals("night")) ti = R.nextInt(n);
        if (bot) ti = 0;
        world.build(ti, false, 0, 0, 0, "", true);
        world.beamOn = false;
        if (world.beam != null) world.beam.visible = false;
        for (Vector3 s : world.stacks) FX.chimney(s);
        root3.kids.clear();
        active = true; paused = false; fade = 0; fadeHold = 0;
        zone = 0; score = 0; stock = START_STOCK; aaKills = 0; tgKills = 0; sorties = 0; chain = 0; feed.clear();
        lateOverT = -1; lateBlastT = -1; ending = false;
        nextZone();
        zoneSorties0 = 0;
        launch();
    }

    public void exit() {
        clearZone();
        root3.kids.clear();
        active = false; paused = false; st = "";
        G.loop("buzz", 0, 1);
        game.mode = "endless"; game.state = "menu";
        if (game.vm != null) game.vm.root.visible = true;
    }

    void clearZone() {
        for (Obj o : units) o.g.remove();
        for (Obj o : targets) o.g.remove();
        for (Hunter h : hunters) h.g.remove();
        units.clear(); targets.clear(); hunters.clear();
        for (Mis m : mis) { m.node.remove(); m.trail.stop(); }
        mis.clear(); shells.clear(); flares.clear();
        for (Bomb b : bombs) b.g.remove();
        bombs.clear();
        FX.clear();
        for (Vector3 s : world.stacks) FX.chimney(s);
    }

    Vector3 freeSpot(Array<Vector3> placed, float rmin, float rmax, float gap) {
        for (int tries = 0; tries < 60; tries++) {
            float a = R.nextFloat() * MathUtils.PI2, r = rr(rmin, rmax);
            float x = MathUtils.sin(a) * r, z = MathUtils.cos(a) * r;
            if (Math.abs(x - World.ROAD_X) < 9) continue;
            boolean ok = true;
            for (Vector3 q : placed) if (q.dst(x, 0, z) < gap) { ok = false; break; }
            if (ok) return new Vector3(x, world.height(x, z), z);
        }
        float a2 = R.nextFloat() * MathUtils.PI2;
        float x = MathUtils.sin(a2) * rmax, z = MathUtils.cos(a2) * rmax;
        return new Vector3(x, world.height(x, z), z);
    }

    void nextZone() {
        clearZone();
        zone += 1;
        Array<Vector3> placed = new Array<>();
        int nt = Math.min(5, 2 + zone / 2);
        Array<String> pool = new Array<>(new String[]{"depot", "fuel", "radar"});
        if (zone >= 2) pool.add("hq");
        if (zone >= 3) pool.add("ew");
        for (int i = 0; i < nt; i++) {
            String k = (i == 0 && zone % 3 == 0) ? "hq" : pool.get(R.nextInt(pool.size));
            Vector3 p = freeSpot(placed, 34, 120, 28);
            placed.add(new Vector3(p.x, 0, p.z));
            SModels.Target md = SModels.target(k, world.themeName);
            Node3 g = md.g;
            root3.add(g); g.pos.set(p); g.setEuler(0, R.nextFloat() * MathUtils.PI2, 0);
            float hpk = 1f + zone * 0.05f;
            Obj o = new Obj();
            o.kind = k; o.g = g; o.spin = md.spin; o.pos.set(p); o.hp = Float.parseFloat(tg(k)[2]) * hpk; o.max = o.hp; o.r = md.r; o.hr = md.hr; o.unit = false;
            targets.add(o);
        }
        int na = Math.min(10, 2 + (int) (zone * 0.7f));
        placed.add(new Vector3());
        Array<String> apool = new Array<>(new String[]{"shilka", "pantsir"});
        if (zone >= 2) apool.add("tor");
        if (zone >= 3) apool.add("grom");
        if (zone >= 4) apool.add("luch");
        String skin = vehSkin();
        for (int i = 0; i < na; i++) {
            String k = i < apool.size ? apool.get(i % apool.size) : apool.get(R.nextInt(apool.size));
            Vector3 p = i > 0 ? freeSpot(placed, 28, 175, 24) : new Vector3(0, world.height(0, 0), 0);
            if (i == 0) k = "pantsir";
            placed.add(new Vector3(p.x, 0, p.z));
            Models.Vehicle v = Models.vehicle(k, skin);
            noShadow(v.root);
            root3.add(v.root); v.root.pos.set(p);
            float ry = R.nextFloat() * MathUtils.PI2;
            v.root.setEuler(0, ry, 0);
            AAD A = AA.get(k);
            float hpk = 1f + zone * 0.04f;
            Obj u = new Obj();
            u.kind = k; u.g = v.root; u.turret = v.turret; u.pitch = v.pitch; u.radar = v.radar; u.barrels = v.barrels; u.noses = v.noses; u.pos.set(p); u.ry = ry;
            u.hp = A.hp * hpk; u.max = u.hp; u.r = 3.2f; u.hr = 2.4f; u.unit = true; u.yaw = R.nextFloat() * MathUtils.PI2; u.mcd = rr(1, 3);
            units.add(u);
        }
        int nh = MathUtils.clamp((int) ((zone - 2) * 0.7f + 0.5f), 0, 4);
        for (int i = 0; i < nh; i++) {
            Models.EnemyModel md = Models.enemy("quad", "harpy");
            root3.add(md.g);
            float a = (float) i / Math.max(1, nh) * MathUtils.PI2;
            Hunter h = new Hunter();
            h.g = md.g; h.rot = md.rot; h.ph = a;
            h.pos.set(MathUtils.sin(a) * 90, world.height(MathUtils.sin(a) * 90, MathUtils.cos(a) * 90) + 55, MathUtils.cos(a) * 90);
            md.g.pos.set(h.pos);
            hunters.add(h);
        }
        updateRad();
        float la = R.nextFloat() * MathUtils.PI2;
        float ld = zone == 1 ? 560f : Math.min(760f, 600f + zone * 20f);
        launchP.set(MathUtils.sin(la) * ld, 0, MathUtils.cos(la) * ld);
        launchP.y = world.height(launchP.x, launchP.z);
        launchYaw = MathUtils.atan2(launchP.x, launchP.z);
        big = "РУБЕЖ " + zone;
        bigSub = String.format("Целей: %d · ПВО: %d%s", nt, na, nh > 0 ? " · перехватчиков: " + nh : "");
        bigT = 3.2f;
        G.sfx("wave");
    }

    void updateRad() {
        radAlive = false;
        for (int i = 0; i < targets.size; i++) if (targets.get(i).alive && targets.get(i).kind.equals("radar")) radAlive = true;
    }

    void launch() {
        if (stock <= 0) return;
        stock -= 1; sorties += 1;
        if (D.g != null) D.g.remove();
        SModels.Drone md = SModels.drone(DK);
        root3.add(md.g);
        D = new DroneState();
        D.g = md.g; D.rot = md.rot; D.bombs = md.bombs; D.r = md.r;
        D.pos.set(launchP).add(0, 22, 0);
        D.yaw = launchYaw; D.pitch = -0.05f; D.sp = S.sp * 0.6f; D.hp = S.hp; D.max = S.hp; D.bat = S.bat; D.bmax = S.bat;
        D.flares = S.flares; D.nb = S.bombs; D.alive = true; D.fall = false;
        FX.puff(new Vector3(launchP).add(0, 1, 0), 3, new Color(0.5f, 0.45f, 0.4f, 1), 0.5f, 2, 1);
        st = "fly";
        feed("Дрон " + sorties + " в воздухе", C("3ff0c8"), 0);
        G.sfx("click");
    }

    static Color C(String h) { return G.hex(h); }

    public Vector3 fwd(Vector3 out) {
        return out.set(-MathUtils.sin(D.yaw) * MathUtils.cos(D.pitch), MathUtils.sin(D.pitch), -MathUtils.cos(D.yaw) * MathUtils.cos(D.pitch));
    }

    final Vector3 tA = new Vector3(), tB = new Vector3(), tC = new Vector3();

    void fly(float dt) {
        boolean wing = S.wing;
        float tx = inX, ty = inY;
        if (jam > 0.05f) { tx += MathUtils.sin(T * 3.1f) * jam * 0.35f; ty += MathUtils.cos(T * 2.3f) * jam * 0.25f; }
        float inv = G.SV.swInv ? -1 : 1;
        D.yaw -= tx * S.turn * dt + mdX;
        D.pitch = MathUtils.clamp(D.pitch + (ty * S.turn * 0.75f * dt + mdY) * inv, -1.45f, wing ? 0.8f : 1.1f);
        float mdx0 = mdX;
        mdX = 0; mdY = 0;
        float agl = D.pos.y - world.height(D.pos.x, D.pos.z);
        if (agl > 260) D.pitch = Math.min(D.pitch, 0);
        boolean boosting = inBoost && D.bat > 0 && !D.fall;
        float want = S.sp;
        if (boosting) want = S.boost;
        else if (inBrake && !wing) want = S.min;
        D.sp = moveToward(D.sp, want, (want > D.sp ? 28f : 36f) * dt);
        if (wing) D.sp = MathUtils.clamp(D.sp - MathUtils.sin(D.pitch) * 16f * dt, S.min, S.boost * 1.3f);
        D.bat -= dt * (boosting ? 2.6f : 1f);
        if (D.bat <= 0 && !D.fall) { D.bat = 0; D.fall = true; feed("Батарея разряжена!", C("ff5a47"), 0); G.sfx("alarm", 0.6f); }
        if (D.fall) { D.pitch = moveToward(D.pitch, -1.3f, dt * 0.7f); D.sp = moveToward(D.sp, 30, dt * 10); }
        Vector3 f = fwd(tA);
        D.vel.set(f).scl(D.sp);
        D.pos.mulAdd(D.vel, dt);
        D.roll = MathUtils.lerp(D.roll, -(tx + mdx0 * 20f) * (wing ? 0.75f : 0.45f), Math.min(1f, dt * 5f));
        float visP = wing ? D.pitch : (MathUtils.clamp(D.pitch, -1.2f, 0.7f) * 0.55f - 0.3f * D.sp / S.boost);
        D.g.pos.set(D.pos);
        D.g.setEuler(visP, D.yaw, D.roll);
        for (Node3 r : D.rot) r.rotateLocal(Vector3.Y, dt * 60f);
        G.loop("buzz", camMode == 0 ? 0.28f : 0.16f, 0.75f + D.sp / 60f + (boosting ? 0.15f : 0f));
        if (boosting && R.nextFloat() < 0.3f) FX.puff(tB.set(D.pos).mulAdd(f, -2f), 0.4f, new Color(0.6f, 0.6f, 0.6f, 1), 0.25f, 0.6f, 0);
        if (D.hp < D.max * 0.5f && R.nextFloat() < 0.3f) FX.puff(D.pos, 0.6f, new Color(0.15f, 0.15f, 0.15f, 1), 0.5f, 1.4f, 1);
        if (D.pos.y < world.height(D.pos.x, D.pos.z) + 0.5f) { detonate(D.pos, null, 1f); return; }
        for (int pass = 0; pass < 2; pass++) {
            Array<Obj> a = pass == 0 ? targets : units;
            for (int i = 0; i < a.size; i++) {
                Obj o = a.get(i);
                if (!o.alive) continue;
                if (tB.set(o.pos).add(0, o.hr, 0).dst(D.pos) < o.r + D.r) { detonate(D.pos, o, 1f); return; }
            }
        }
        jam = 0;
        for (int i = 0; i < targets.size; i++) { Obj t = targets.get(i); if (t.alive && t.kind.equals("ew")) jam = Math.max(jam, MathUtils.clamp(1f - t.pos.dst(D.pos) / 280f, 0, 1)); }
    }

    public void useFlares() {
        if (!st.equals("fly") || !D.alive || D.flares <= 0 || flareCd > 0) return;
        D.flares -= 1; flareCd = 1.2f;
        G.sfx("emp", 0.4f);
        Vector3 f = fwd(new Vector3());
        Array<Flare> made = new Array<>();
        for (int i = 0; i < 4; i++) {
            Flare o = new Flare();
            o.vel.set(D.vel).scl(0.4f).add(rr(-9, 9), rr(-2, 6), rr(-9, 9)).mulAdd(f, -8f);
            o.pos.set(D.pos).mulAdd(f, -1.5f);
            o.life = 3.2f;
            flares.add(o); made.add(o);
        }
        for (Mis m : mis) if (m.atDrone && m.pos.dst(D.pos) < 600 && R.nextFloat() < S.decoy) { m.atDrone = false; m.tgt = made.get(R.nextInt(4)); }
        feed("Ловушки отстрелены", C("ffd27a"), 0);
    }

    public void dropBomb() {
        if (!st.equals("fly") || !D.alive || D.nb <= 0) return;
        D.nb -= 1;
        if (D.nb < D.bombs.size) D.bombs.get(D.nb).visible = false;
        Bomb b = new Bomb();
        b.g = SModels.bombFree(); root3.add(b.g);
        b.pos.set(D.pos).add(0, -0.8f, 0); b.vel.set(D.vel).scl(0.9f);
        b.g.pos.set(b.pos);
        bombs.add(b);
        G.sfx("click", 0.6f);
    }

    public void detonateNow() { if (st.equals("fly") && D.alive) detonate(D.pos, null, 1f); }

    float diveBonus() {
        float k = 1;
        if (S.wing) {
            k += MathUtils.clamp((D.sp - S.sp) / 60f, 0, 0.4f);
            if (D.pitch < -0.6f) k += 0.15f;
        }
        return k;
    }

    void detonate(Vector3 p, Obj direct, float k) {
        if (!D.alive) return;
        float dmg = S.dmg * k * diveBonus(), rad = S.rad;
        FX.explosion(p, 1.4f + rad / 9f, true); G.sfx("boom"); G.hap(60);
        shake = 1;
        D.alive = false;
        if (D.g != null) D.g.visible = false;
        G.loop("buzz", 0, 1);
        st = "boom"; boomT = 2.6f; boomP.set(p);
        blast(p, dmg, rad, direct);
    }

    void blast(Vector3 p, float dmg, float rad, Obj direct) {
        for (int pass = 0; pass < 2; pass++) {
            Array<Obj> a = new Array<>(pass == 0 ? targets : units);
            for (Obj o : a) {
                if (!o.alive) continue;
                float d = tB.set(o.pos).add(0, o.hr, 0).dst(p) - o.r;
                if (o == direct) hurt(o, dmg);
                else if (d < rad) hurt(o, dmg * (1 - MathUtils.clamp(d / rad, 0, 1)) * 0.85f);
            }
        }
        for (int i = 0; i < hunters.size; i++) { Hunter h = hunters.get(i); if (h.alive && h.pos.dst(p) < rad * 1.6f) killHunter(h); }
    }

    void hurt(Obj o, float dmg) {
        if (!o.alive || dmg <= 0) return;
        o.hp -= dmg;
        FX.sparks(tC.set(o.pos).add(0, o.hr, 0), 8);
        if (o.hp <= 0) destroy(o);
    }

    void destroy(Obj o) {
        o.alive = false;
        Vector3 c = new Vector3(o.pos).add(0, o.hr, 0);
        FX.explosion(c, 2.6f, true); G.sfx("boom", 0.9f);
        Node3 g = o.g;
        float ry = MathUtils.atan2(g.rot.val[Matrix4.M02], g.rot.val[Matrix4.M22]);
        g.setEuler(rr(-0.08f, 0.08f), ry, rr(-0.12f, 0.12f));
        g.scl.set(1, 0.7f, 1);
        Node3 a1 = new Node3(); a1.pos.set(0, o.hr + 1f, 0); g.add(a1); FX.burner(a1, true, 2.2f);
        Node3 a2 = new Node3(); a2.pos.set(0, o.hr + 2.5f, 0); g.add(a2); FX.burner(a2, false, 3f);
        chain = chainT > 0 ? chain + 1 : 1;
        chainT = 7;
        float mul = 1 + 0.25f * (chain - 1);
        int pts;
        if (o.unit) {
            aaKills++;
            o.turret.setEuler(rr(-0.3f, 0.3f), o.yaw - o.ry, rr(-0.4f, 0.4f));
            pts = 180 + zone * 10;
            feed(G.vehName(o.kind) + " уничтожен", C("ff9a6a"), (int) (pts * mul));
            o.beam = false;
        } else {
            tgKills++;
            String[] t = tg(o.kind);
            pts = Integer.parseInt(t[3]) + zone * 20;
            feed(t[1] + " уничтожен" + t[4], C("ffd27a"), (int) (pts * mul));
            if (o.kind.equals("radar")) {
                if (o.spin != null) o.spin.setEuler(0, 0, 1);
                updateRad();
                if (!radAlive) feed("РЛС подавлены: ПВО видят хуже", C("3ff0c8"), 0);
            }
            if (o.kind.equals("ew")) feed("РЭБ подавлена: сигнал чистый", C("3ff0c8"), 0);
            if (o.kind.equals("fuel")) {
                FX.explosion(new Vector3(c).add(3, 2, 0), 3.2f, true);
                lateBlastT = 0.25f; lateBlastP.set(c);
            }
        }
        score += (int) (pts * mul);
        if (chain >= 2) feed("Серия ×" + chain, C("3ff0c8"), 0);
        G.sfx("coin", 0.5f);
        if (!o.unit) {
            if (targetsLeft() == 0) zoneClear();
        }
    }

    void zoneClear() {
        if (st.equals("clear") || st.equals("over") || ending) return;
        int bonus = 400 + zone * 150;
        int back = 0;
        if (D.alive) {
            back = 1; D.alive = false;
            if (D.g != null) D.g.visible = false;
            G.loop("buzz", 0, 1);
        }
        int add = 4 + back + (sorties - zoneSorties0 <= 1 ? 2 : 0);
        stock += add;
        score += bonus;
        big = "РУБЕЖ " + zone + " ПРОРВАН";
        bigSub = String.format("+%d очков · +%d дрон%s%s", bonus, add, add < 5 ? "а" : "ов", back > 0 ? " · дрон вернулся" : "");
        bigT = 3.5f;
        G.sfx("clear");
        st = "clear"; clearT = 3.6f;
        int pl = Pass.add(Pass.XP_STRIKE_ZONE);
        if (pl > 0) feed("Боевой пропуск: уровень " + pl, C("ffd27a"), 0);
        Float z = G.SV.stats.get("swZone");
        if (z == null || z < zone) G.SV.stats.put("swZone", (float) zone);
    }

    void dhurt(float dmg, String src) {
        if (!D.alive || !st.equals("fly")) return;
        D.hp -= dmg; hitT = 0.35f; shake = Math.max(shake, 0.35f);
        G.sfx("tink", 0.6f, 60); G.hap(20);
        if (D.hp <= 0) { feed("Дрон сбит: " + src, C("ff5a47"), 0); detonate(D.pos, null, 0.5f); }
    }

    void feed(String t, Color col, int pts) {
        Feed f = new Feed(); f.t = t + (pts > 0 ? "  +" + pts : ""); f.c = col; f.life = 5;
        feed.insert(0, f);
        if (feed.size > 6) feed.pop();
    }

    boolean los(Vector3 a, Vector3 b) {
        for (int i = 1; i < 9; i++) {
            float k = i / 9f;
            float x = MathUtils.lerp(a.x, b.x, k), y = MathUtils.lerp(a.y, b.y, k), z = MathUtils.lerp(a.z, b.z, k);
            if (y < world.height(x, z) - 0.5f) return false;
        }
        return true;
    }

    Vector3 lead(Vector3 from, float speed, Vector3 out) {
        float t = from.dst(D.pos) / speed;
        return out.set(D.pos).mulAdd(D.vel, t * 0.95f);
    }

    void updateUnits(float dt) {
        alert = Math.max(0, alert - dt); lockWarn = Math.max(0, lockWarn - dt); laserWarn = Math.max(0, laserWarn - dt);
        float zk = 1 + zone * 0.03f;
        for (int ui0 = 0; ui0 < units.size; ui0++) {
            Obj u = units.get(ui0);
            if (!u.alive) continue;
            AAD A = AA.get(u.kind);
            Vector3 c = tC.set(u.pos).add(0, 3, 0);
            boolean see = false; float d = 99999;
            if (D.alive && st.equals("fly")) {
                d = c.dst(D.pos);
                float agl = D.pos.y - world.height(D.pos.x, D.pos.z);
                float low = MathUtils.clamp((agl - 8f) / 32f, 0, 1) * 0.68f + 0.32f;
                float det = A.det * zk * S.sig * low * (radAlive ? 1.25f : 1f) * (agl > 170 ? 1.3f : 1f);
                if (d < det || d < 45) see = los(c, D.pos);
                if (d < 320) u.known = true;
            }
            u.see = see;
            u.track = see ? Math.min(u.track + dt, 5f) : Math.max(u.track - dt * 0.8f, 0);
            if (see) { alert = 0.6f; u.known = true; }
            if (u.track > 0.25f && D.alive) {
                Vector3 lp = A.gun > 0 ? lead(c, 400, tB) : tB.set(D.pos);
                float vx = lp.x - c.x, vy = lp.y - c.y, vz = lp.z - c.z;
                float wy = MathUtils.atan2(-vx, -vz), wp = MathUtils.atan2(vy, (float) Math.sqrt(vx * vx + vz * vz));
                u.yaw = u.yaw + MathUtils.clamp(angDiff(u.yaw, wy), -A.turn * dt, A.turn * dt);
                u.pit = MathUtils.clamp(moveToward(u.pit, wp, A.turn * 0.7f * dt), -0.05f, 1.3f);
            } else {
                u.yaw += dt * 0.35f; u.pit = moveToward(u.pit, 0.35f, dt * 0.3f);
            }
            u.turret.setEuler(0, u.yaw - u.ry, 0);
            u.pitch.setEuler(u.pit, 0, 0);
            u.radar.rotateY(dt * (u.track > 0 ? 5f : 2f));
            u.cd = Math.max(u.cd - dt, -0.15f); u.mcd -= dt;
            float aimErr = 1;
            if (D.alive) aimErr = Math.abs(angDiff(u.yaw, MathUtils.atan2(-(D.pos.x - c.x), -(D.pos.z - c.z))));
            if (A.gun > 0 && u.track > 1.3f - Math.min(0.5f, zone * 0.05f) && d < A.gun && aimErr < 0.3f) {
                if (u.burst > 0) {
                    u.burst -= dt;
                    while (u.cd <= 0) { u.cd += 1f / A.rate; unitShot(u, A, d); }
                    if (u.burst <= 0) u.rest = rr(0.4f, 0.9f);
                } else {
                    u.rest -= dt; u.cd = Math.max(u.cd, 0);
                    if (u.rest <= 0) u.burst = rr(0.9f, 1.6f);
                }
            }
            if (A.mis > 0 && u.track > A.lock * 0.5f && d < A.det * zk) lockWarn = 0.4f;
            if (A.mis > 0 && u.track > A.lock && d < A.det * zk && u.mcd <= 0) {
                int mine = 0;
                for (int k = 0; k < mis.size; k++) if (mis.get(k).src == u) mine++;
                if (mine < (zone < 6 ? 1 : A.mis)) { launchMissile(u); u.mcd = A.mcd * Math.max(0.7f, 1.15f - zone * 0.03f); }
            }
            u.beam = false;
            if (A.laser > 0) {
                if (u.over) { u.heat -= dt / 2.2f; if (u.heat < 0.2f) u.over = false; }
                else if (u.track > 1.1f && d < A.laser && aimErr < 0.35f) {
                    u.beam = true; u.heat += dt / 3.2f; laserWarn = 0.3f;
                    dhurt(A.ldps * Math.min(1.5f, 0.6f + zone * 0.08f) * dt, "лазер");
                    if (R.nextFloat() < 0.3f && D.alive) FX.sparks(D.pos, 2, new Color(1, 0.4f, 0.3f, 1));
                    if (u.heat >= 1) u.over = true;
                } else u.heat = Math.max(0, u.heat - dt / 3f);
            }
            if (A.flak > 0 && u.track > 0.8f && d < A.flak && u.cd <= 0 && aimErr < 0.4f) {
                u.cd = 1f / A.frate;
                Vector3 from = u.barrels.size > 0 ? u.pitch.toGlobal(u.barrels.get(0), new Vector3()) : new Vector3(u.pos).add(0, 4, 0);
                Vector3 lp2 = lead(from, 300, new Vector3()).add(rr(-4, 4), rr(-3, 3), rr(-4, 4));
                Shell s = new Shell();
                s.pos.set(from); s.prev.set(from); s.o.set(from); s.vel.set(lp2).sub(from).nor().scl(300);
                s.life = from.dst(lp2) / 300f + rr(-0.06f, 0.06f); s.dmg = A.fdmg * Math.min(1.5f, 0.6f + zone * 0.08f); s.flak = true; s.rad = A.frad;
                shells.add(s);
                FX.glow(from, 4, new Color(1, 0.85f, 0.5f, 1), 0.06f);
                G.sfx("flak", MathUtils.clamp(90f / Math.max(d, 1), 0.08f, 0.6f), 80);
            }
        }
    }

    void unitShot(Obj u, AAD A, float d) {
        u.bi = (u.bi + 1) % Math.max(1, u.barrels.size);
        Vector3 from = u.barrels.size > 0 ? u.pitch.toGlobal(u.barrels.get(u.bi), new Vector3()) : new Vector3(u.pos).add(0, 4, 0);
        Vector3 v = lead(from, 420, new Vector3()).sub(from).nor();
        float sp = A.spread + D.sp / 56f * 0.012f;
        v.add(rr(-1, 1) * sp, rr(-1, 1) * sp, rr(-1, 1) * sp).nor().scl(420);
        Shell s = new Shell();
        s.pos.set(from); s.prev.set(from); s.o.set(from); s.vel.set(v); s.life = 1.6f; s.dmg = A.gdmg * Math.min(1.5f, 0.4f + zone * 0.1f);
        shells.add(s);
        if (R.nextFloat() < 0.5f) FX.glow(from, 2.4f, new Color(1, 0.8f, 0.5f, 1), 0.05f);
        G.sfx(u.kind.equals("shilka") ? "shot2" : "shot", MathUtils.clamp(60f / Math.max(d, 1), 0.05f, 0.5f), 45);
    }

    void launchMissile(Obj u) {
        Vector3 from = u.noses.size > 0 ? u.pitch.toGlobal(u.noses.get(R.nextInt(u.noses.size)), new Vector3()) : new Vector3(u.pos).add(0, 5, 0);
        float[] gm = u.pitch.global.val;
        Vector3 fw = new Vector3(-gm[Matrix4.M02], -gm[Matrix4.M12], -gm[Matrix4.M22]).nor();
        Node3 node = new Node3();
        node.add(new Node3(Models.missileMesh(), Models.MAT_MISSILE));
        Node3 fl = FX.flame(1.78f); node.add(fl);
        SmokeTrail trl = new SmokeTrail(true);
        root3.add(node); node.pos.set(from);
        node.lookDir(fw, Math.abs(fw.y) < 0.98f ? Vector3.Y : Vector3.X);
        FX.launchBlast(from, fw);
        Mis m = new Mis();
        m.node = node; m.trail = trl; m.flame = fl; m.pos.set(from); m.vel.set(fw).scl(30); m.sp = 30; m.atDrone = true; m.life = 9; m.last.set(D.pos); m.src = u;
        mis.add(m);
        G.sfx("missile", 0.7f); G.sfx("alarm", 0.5f, 400);
        feed("ПУСК РАКЕТЫ! Ловушки или укрытие", C("ff5a47"), 0);
    }

    void updateMissiles(float dt) {
        for (int i = mis.size - 1; i >= 0; i--) {
            Mis m = mis.get(i);
            m.life -= dt; m.age += dt;
            m.sp = Math.min(125f, m.sp + (m.age < 0.3f ? 40f : 110f) * dt);
            Vector3 dir = tA.set(m.vel).nor();
            Vector3 aim = null;
            if (m.atDrone) {
                if (D.alive && st.equals("fly")) {
                    if ((m.age % 0.2f) < dt && los(m.pos, D.pos)) m.last.set(D.pos).mulAdd(D.vel, (m.pos.dst(D.pos) / Math.max(m.sp, 1)) * 0.8f);
                    aim = m.last;
                }
            } else if (m.tgt != null && m.tgt.life > 0) aim = m.tgt.pos;
            if (aim != null && m.age > 0.25f) {
                Vector3 wd = tB.set(aim).sub(m.pos).nor();
                float ang = Game.angleBetween(dir, wd), mt = 2.7f * dt;
                if (ang > 0.0001f) Game.slerp(dir, wd, Math.min(1f, mt / ang));
            }
            m.vel.set(dir).scl(m.sp);
            Vector3 prev = new Vector3(m.pos);
            m.pos.mulAdd(m.vel, dt);
            m.node.pos.set(m.pos);
            m.node.lookDir(dir, Math.abs(dir.y) < 0.98f ? Vector3.Y : Vector3.X);
            m.trail.emit(tC.set(m.pos).mulAdd(dir, -1.9f));
            float fk = rr(0.75f, 1.25f);
            m.flame.scl.set(fk, fk, fk * Math.min(1f, m.age * 6f + 0.3f));
            boolean boom = false;
            if (D.alive && st.equals("fly") && m.atDrone) {
                float abx = m.pos.x - prev.x, aby = m.pos.y - prev.y, abz = m.pos.z - prev.z;
                float l2 = Math.max(abx * abx + aby * aby + abz * abz, 1e-6f);
                float t = MathUtils.clamp(((D.pos.x - prev.x) * abx + (D.pos.y - prev.y) * aby + (D.pos.z - prev.z) * abz) / l2, 0, 1);
                float dd = D.pos.dst(prev.x + abx * t, prev.y + aby * t, prev.z + abz * t);
                if (dd < 4.5f) { boom = true; dhurt((24f + zone * 2.5f) * (1 - dd / 9f), "ракета"); }
            }
            if (!m.atDrone && m.tgt != null && m.pos.dst(m.tgt.pos) < 5) boom = true;
            if (m.life <= 0 || m.pos.y < world.height(m.pos.x, m.pos.z)) boom = true;
            if (boom) {
                FX.explosion(m.pos, 1.3f, true);
                if (D.alive && m.pos.dst(D.pos) < 30) G.sfx("boom", 0.6f);
                m.trail.stop(); m.node.remove(); mis.removeIndex(i);
            }
        }
    }

    final Matrix4 tm = new Matrix4();

    void updateShells(float dt) {
        tracers.clear();
        int n = 0;
        for (int i = shells.size - 1; i >= 0; i--) {
            Shell b = shells.get(i);
            b.prev.set(b.pos);
            b.vel.y -= 4f * dt;
            b.pos.mulAdd(b.vel, dt);
            b.life -= dt;
            boolean dead = false;
            if (b.flak) {
                boolean near = D.alive && st.equals("fly") && b.pos.dst(D.pos) < 5;
                if (b.life <= 0 || near) {
                    FX.explosion(b.pos, 0.55f, false);
                    FX.puff(b.pos, 2.2f, new Color(0.08f, 0.08f, 0.08f, 1), 0.75f, 2.6f, 0.4f);
                    if (D.alive && st.equals("fly")) {
                        float dd = b.pos.dst(D.pos);
                        if (dd < b.rad) dhurt(b.dmg * (1 - dd / b.rad * 0.7f), "зенитка");
                    }
                    shells.removeIndex(i); continue;
                }
            } else {
                if (D.alive && st.equals("fly") && Game.segHit(b.prev, b.pos, D.pos, D.r + 0.45f)) { dhurt(b.dmg, "пушка"); FX.sparks(D.pos, 4); dead = true; }
                if (b.life <= 0) dead = true;
            }
            if (!dead && b.pos.y < world.height(b.pos.x, b.pos.z)) dead = true;
            if (dead) { shells.removeIndex(i); continue; }
            if (n < 300) {
                float trav = b.pos.dst(b.o);
                float L = Math.min(b.flak ? 6f : 14f, trav);
                if (L > 0.5f) {
                    tA.set(b.vel).nor();
                    tB.set(b.pos).mulAdd(tA, -L * 0.5f);
                    Node3.basisLook(tm, tA, Math.abs(tA.y) < 0.98f ? Vector3.Y : Vector3.X);
                    tm.val[Matrix4.M03] = tB.x; tm.val[Matrix4.M13] = tB.y; tm.val[Matrix4.M23] = tB.z;
                    tm.scale(0.16f, 0.16f, L);
                    if (b.flak) tracers.prim(Dyn.BOX, tm, 1, 0.85f, 0.5f, 1);
                    else tracers.prim(Dyn.BOX, tm, 1, 0.55f, 0.35f, 1);
                    n++;
                }
            }
        }
    }

    void drawBeams() {
        beamDyn.clear();
        for (Obj u : units) {
            if (!(u.alive && u.beam && D.alive)) continue;
            Vector3 a = u.pitch.toGlobal(tA.set(0, 0.3f, -2f), new Vector3());
            Vector3 b = new Vector3(D.pos).add(rr(-0.3f, 0.3f), rr(-0.3f, 0.3f), rr(-0.3f, 0.3f));
            float w = rr(0.8f, 1.3f);
            cylBetween(a, b, 0.12f * w, 1, 0.25f, 0.2f, 0.8f);
            cylBetween(a, b, 0.04f * w, 1, 0.9f, 0.85f, 0.9f);
            if (R.nextFloat() < 0.5f) FX.glow(a, 2, new Color(1, 0.3f, 0.2f, 1), 0.05f);
        }
    }

    void cylBetween(Vector3 a, Vector3 b, float r, float cr, float cg, float cb, float ca) {
        tA.set(b).sub(a);
        float L = tA.len();
        if (L < 0.1f) return;
        tA.scl(1f / L);
        Vector3 ref = Math.abs(tA.y) < 0.95f ? Vector3.Y : Vector3.X;
        tB.set(tA).crs(ref).nor(); tC.set(tB).crs(tA).nor();
        float[] m = tm.idt().val;
        m[Matrix4.M00] = tB.x * r; m[Matrix4.M10] = tB.y * r; m[Matrix4.M20] = tB.z * r;
        m[Matrix4.M01] = tA.x * L; m[Matrix4.M11] = tA.y * L; m[Matrix4.M21] = tA.z * L;
        m[Matrix4.M02] = tC.x * r; m[Matrix4.M12] = tC.y * r; m[Matrix4.M22] = tC.z * r;
        m[Matrix4.M03] = (a.x + b.x) / 2; m[Matrix4.M13] = (a.y + b.y) / 2; m[Matrix4.M23] = (a.z + b.z) / 2;
        beamDyn.prim(Dyn.CYL8, tm, cr, cg, cb, ca);
    }

    void updateHunters(float dt) {
        for (Hunter h : hunters) {
            if (!h.alive) continue;
            Vector3 hp = h.pos;
            Vector3 want = new Vector3();
            h.chase = D.alive && st.equals("fly") && (alert > 0 || hp.dst(D.pos) < 160) && hp.dst(D.pos) < 520;
            float sp;
            if (h.chase) { want.set(D.pos).mulAdd(D.vel, 0.4f).sub(hp).nor(); sp = Math.min(52f, 42f + zone); }
            else {
                h.ph += dt * 0.25f;
                float cx = MathUtils.sin(h.ph) * 90, cz = MathUtils.cos(h.ph) * 90;
                want.set(cx, world.height(cx, cz) + 55, cz).sub(hp).nor(); sp = 22;
            }
            Vector3 cur = h.vel.len() > 0.1f ? new Vector3(h.vel).nor() : new Vector3(want);
            float ang = Game.angleBetween(cur, want);
            if (ang > 0.0001f) Game.slerp(cur, want, Math.min(1f, 2.4f * dt / ang));
            h.vel.set(cur).scl(sp);
            hp.mulAdd(h.vel, dt);
            float gy = world.height(hp.x, hp.z);
            if (hp.y < gy + 5) hp.y = gy + 5;
            h.g.pos.set(hp);
            float ry = (float) Math.sqrt(cur.x * cur.x + cur.z * cur.z) > 0.1f ? MathUtils.atan2(-cur.x, -cur.z) : 0;
            h.g.setEuler(h.chase ? -0.25f : -0.1f, ry, 0);
            if (h.rot != null) h.rot.rotateY(dt * 40);
            if (D.alive && st.equals("fly") && hp.dst(D.pos) < 3.4f + D.r) { killHunter(h); dhurt(32f + zone, "перехватчик"); }
        }
    }

    void killHunter(Hunter h) {
        h.alive = false;
        FX.explosion(h.pos, 1f, true);
        h.g.visible = false;
        score += 60;
        feed("Перехватчик уничтожен", C("ff9a6a"), 60);
    }

    void updateMisc(float dt) {
        for (int i = flares.size - 1; i >= 0; i--) {
            Flare f = flares.get(i);
            f.life -= dt; f.vel.y -= 9f * dt; f.vel.scl(1 - dt * 0.8f);
            f.pos.mulAdd(f.vel, dt);
            if ((T * 30f + i) % 2f < 1f) FX.glow(f.pos, 2.6f, new Color(1, 0.9f, 0.6f, 1), 0.07f);
            if (R.nextFloat() < 0.25f) FX.puff(f.pos, 0.6f, new Color(0.85f, 0.85f, 0.85f, 1), 0.35f, 1.2f, 0.5f);
            if (f.life <= 0) flares.removeIndex(i);
        }
        for (int i = bombs.size - 1; i >= 0; i--) {
            Bomb b = bombs.get(i);
            b.vel.y -= 14f * dt; b.vel.scl(1 - dt * 0.15f);
            b.pos.mulAdd(b.vel, dt);
            b.g.pos.set(b.pos);
            if (b.vel.len() > 0.5f) { tA.set(b.vel).nor(); b.g.lookDir(tA, Math.abs(tA.y) < 0.98f ? Vector3.Y : Vector3.X); }
            Obj hit = null;
            for (int pass = 0; pass < 2; pass++) {
                Array<Obj> arr = pass == 0 ? targets : units;
                for (int k = 0; k < arr.size; k++) { Obj o = arr.get(k); if (o.alive && tB.set(o.pos).add(0, o.hr, 0).dst(b.pos) < o.r) hit = o; }
            }
            if (hit != null || b.pos.y < world.height(b.pos.x, b.pos.z) + 0.2f) {
                FX.explosion(b.pos, 1.5f, true); G.sfx("boom", 0.8f);
                blast(new Vector3(b.pos), 140f * (1 + 0.12f * up("war")), 9, hit);
                b.g.remove(); bombs.removeIndex(i);
            }
        }
        for (Obj t : targets) if (t.alive && t.spin != null) t.spin.rotateY(dt * 1.5f);
        for (Feed f : feed) f.life -= dt;
        while (feed.size > 0 && feed.peek().life <= 0) feed.pop();
    }

    // ---------- кадр ----------
    public void update(float dt) {
        if (!active) return;
        if (paused) return;
        T += dt;
        root3.updateGlobal(null);
        bigT = Math.max(0, bigT - dt); hitT = Math.max(0, hitT - dt); flareCd = Math.max(0, flareCd - dt);
        if (chainT > 0) { chainT -= dt; if (chainT <= 0) chain = 0; }
        if (lateBlastT > 0) { lateBlastT -= dt; if (lateBlastT <= 0) blast(lateBlastP, 220, 20, null); }
        if (lateOverT > 0) { lateOverT -= dt; if (lateOverT <= 0 && st.equals("over") && ui != null) ui.strikeOver(); }
        if (bot) bot(dt);
        if (st.equals("fly") && D.alive) fly(dt);
        updateUnits(dt);
        drawBeams();
        updateMissiles(dt);
        updateShells(dt);
        updateHunters(dt);
        updateMisc(dt);
        if (st.equals("boom")) {
            boomT -= dt;
            if (boomT <= 0) {
                if (ending || (targetsLeft() > 0 && stock <= 0)) gameOver();
                else if (targetsLeft() > 0) launch();
            }
        } else if (st.equals("clear")) {
            clearT -= dt;
            fade = MathUtils.clamp(1f - (clearT - 0.1f) / 0.7f, 0, 1);
            if (clearT <= 0 && fadeHold < 2) fadeHold++;
            else if (clearT <= 0) {
                fadeHold = 0;
                nextZone(); zoneSorties0 = sorties;
                launch();
                fade = 1;
            }
        }
        if (!st.equals("clear") && fade > 0) fade = Math.max(0, fade - dt * 2);
        camera(dt);
    }

    void gameOver() {
        st = "over";
        resRec = score > G.SV.swBest;
        G.SV.swBest = Math.max(G.SV.swBest, score); G.SV.swBestz = Math.max(G.SV.swBestz, zone);
        resCoins = score / 8;
        G.SV.c += resCoins;
        resXp = score / 50 + zone * 4;
        int old = G.rankIndex();
        G.addXp(resXp);
        resRankUp = G.rankIndex() > old;
        game.statAdd("swTg", tgKills); game.statAdd("swAA", aaKills); game.statAdd("swRuns", 1);
        game.checkAch();
        G.save();
        G.loop("buzz", 0, 1);
        G.sfx("fail");
        lateOverT = 1.2f;
    }

    public void endRun() {
        paused = false; stock = 0; ending = true;
        if (D.alive && st.equals("fly")) detonate(D.pos, null, 1f);
        else if (!st.equals("over")) gameOver();
    }

    public void pause() {
        if (paused) return;
        paused = true; inBoost = false; inX = 0; inY = 0;
        G.loop("buzz", 0, 1);
    }

    public void resume() { paused = false; }

    public void toggleCam() {
        camMode = 1 - camMode;
        G.SV.swCam = camMode; G.save();
        G.sfx("click");
    }

    void camera(float dt) {
        Vector3 sh = new Vector3();
        if (shake > 0) { sh.set(rr(-1, 1), rr(-1, 1), rr(-1, 1)).scl(shake * 0.4f); shake = Math.max(0, shake - dt * 2.5f); }
        cam.up.set(Vector3.Y);
        if (st.equals("fly") && D.alive) {
            Vector3 f = fwd(new Vector3());
            if (camMode == 0) {
                cam.fieldOfView = 92;
                tm.idt().rotateRad(Vector3.Y, D.yaw).rotateRad(Vector3.X, D.pitch).rotateRad(Vector3.Z, D.roll);
                Vector3 off = new Vector3(0, 0.15f, -D.r * 0.9f).rot(tm);
                cam.position.set(D.pos).add(off).mulAdd(sh, 0.3f);
                cam.direction.set(0, 0, -1).rot(tm);
                cam.up.set(0, 1, 0).rot(tm);
                D.g.visible = false;
            } else {
                cam.fieldOfView = 72;
                D.g.visible = true;
                float dist = 7f + D.r * 3.5f;
                Vector3 hf = (float) Math.sqrt(f.x * f.x + f.z * f.z) > 0.05f ? new Vector3(f.x, 0, f.z).nor() : new Vector3(-MathUtils.sin(D.yaw), 0, -MathUtils.cos(D.yaw));
                Vector3 want = new Vector3(D.pos).mulAdd(hf, -dist).add(0, 2.2f + D.r - f.y * 3f, 0);
                want.y = Math.max(want.y, world.height(want.x, want.z) + 1.5f);
                if (camSmooth.dst(want) > 40) camSmooth.set(want); else camSmooth.lerp(want, Math.min(1f, dt * 9f));
                cam.position.set(camSmooth).add(sh);
                cam.direction.set(D.pos).mulAdd(f, 18).sub(cam.position).nor();
            }
        } else {
            cam.fieldOfView = 70;
            Vector3 c = !st.equals("clear") ? new Vector3(boomP) : new Vector3(0, world.height(0, 0) + 10, 0);
            float a = T * 0.35f, r = !st.equals("clear") ? 26 : 120;
            Vector3 p = new Vector3(c).add(MathUtils.sin(a) * r, !st.equals("clear") ? 10 : 55, MathUtils.cos(a) * r);
            p.y = Math.max(p.y, world.height(p.x, p.z) + 3);
            cam.position.set(p).add(sh);
            cam.direction.set(c).sub(cam.position).nor();
            camSmooth.set(cam.position);
        }
        cam.update();
    }

    /** Сила помех FPV-картинки. */
    public float noise() {
        float nz = 0;
        if (st.equals("fly") && D.alive) {
            nz = MathUtils.clamp(D.pos.dst(launchP) / 1600f, 0, 0.3f) + jam * 0.75f + (1 - D.hp / Math.max(1, D.max)) * 0.25f + hitT * 1.2f;
            if (D.fall) nz += 0.3f;
        } else if (st.equals("boom") && boomT > 2.2f) nz = 1;
        if (camMode == 1) nz *= 0.35f;
        return MathUtils.clamp(nz, 0, 1);
    }

    public int targetsLeft() { int n = 0; for (int i = 0; i < targets.size; i++) if (targets.get(i).alive) n++; return n; }

    public int missilesAtMe() { int n = 0; for (Mis m : mis) if (m.atDrone) n++; return n; }

    public Array<Vector3> missilePositions(Array<Vector3> out) { out.clear(); for (Mis m : mis) if (m.atDrone) out.add(m.pos); return out; }

    public Array<Vector3> allMissiles(Array<Vector3> out) { out.clear(); for (Mis m : mis) out.add(m.pos); return out; }

    public float detRange(Obj u) { return AA.get(u.kind).det * (1 + zone * 0.03f); }

    // ---------- автопилот (проверки) ----------
    float bt;

    void bot(float dt) {
        bt += dt;
        if (!st.equals("fly") || !D.alive) return;
        Obj best = null; float bd = 1e9f;
        for (Obj t : targets) if (t.alive) { float d = t.pos.dst(D.pos); if (d < bd) { bd = d; best = t; } }
        if (best == null) return;
        Vector3 tp = new Vector3(best.pos).add(0, best.hr, 0);
        float flat = (float) Math.hypot(tp.x - D.pos.x, tp.z - D.pos.z);
        Vector3 aim = new Vector3(tp);
        if (flat > 140) aim.set(tp.x, Math.max(world.height(D.pos.x, D.pos.z), world.height(tp.x, tp.z)) + 10f, tp.z);
        Vector3 v = aim.sub(D.pos);
        float wy = MathUtils.atan2(-v.x, -v.z);
        float wp = MathUtils.atan2(v.y, (float) Math.hypot(v.x, v.z));
        if (flat > 140) {
            Vector3 ahead = fwd(new Vector3()).scl(40).add(D.pos);
            if (world.height(ahead.x, ahead.z) + 10 > D.pos.y) wp = 0.5f;
        }
        inX = MathUtils.clamp(-angDiff(D.yaw, wy) * 2.5f, -1, 1);
        mdY = MathUtils.clamp((wp - D.pitch) * 4f, -1, 1) * S.turn * 0.75f * dt * (G.SV.swInv ? -1 : 1);
        inY = 0;
        inBoost = flat > 220 && D.bat > S.bat * 0.3f;
        boolean threat = false;
        for (int i = 0; i < mis.size; i++) if (mis.get(i).atDrone && mis.get(i).pos.dst(D.pos) < 220) threat = true;
        if (threat) useFlares();
        if (D.nb > 0 && flat < 30 && D.pos.y - tp.y < 60) dropBomb();
    }
}
