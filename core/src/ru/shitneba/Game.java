package ru.shitneba;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import ru.shitneba.gfx.Dyn;
import ru.shitneba.gfx.MB;
import ru.shitneba.gfx.Mat;
import ru.shitneba.gfx.Mesh3;
import ru.shitneba.gfx.Node3;

/** Логика боя — порт game.gd (бесконечный режим). */
public class Game {
    public final Node3 root = new Node3();
    public World world;
    public PerspectiveCamera cam;
    public GameUI ui;
    public String state = "menu";
    public String mode = "endless";
    public String VEH = "pantsir";
    public Models.Vehicle vm;
    public final Map<String, Float> ST = new LinkedHashMap<>();
    public Run RUN;
    public JsonValue dif;
    public float difSpd = 1, difDmg = 1, difHp = 1, difCoin = 1;
    public int score, wave, kills, combo, runCoins;
    public float hp = 100, heat, comboT;
    public boolean overheated;
    public float aimYaw = 0, aimPitch = 0.2f;
    public boolean firing;
    float fireT;
    int barrelI;
    public int missiles = 4;
    public float reloadT, chanT;
    public float shake, hurtT;
    // ---------- сочность: отдача, тряска, облёт босса ----------
    public float recoil, quake;
    Vector3 pitchBase, rootBase;
    public float killCamT; static final float KILL_DUR = 2.7f;
    final Vector3 killFocus = new Vector3(); Node3 killNode; float killR, killA0;
    public String killName;
    float steamT; int stormLeft;
    int toSpawn; float spawnT, spawnInt = 1.3f; public float between; float deadT;
    public final Array<E> bosses = new Array<>();
    float missionTime;
    boolean waveHit;
    public final Map<String, Boolean> abUsed = new LinkedHashMap<>();
    public final Map<String, Float> abCd = new LinkedHashMap<>();
    public float smokeT, repairT, empT;
    int scoutQ; float scoutT; int squads; float squadT; boolean bossWave, bossSpawned; int bossAt, spawned;
    int nohitWaves;
    public final Array<Models.Mog> mogs = new Array<>();
    public int tut = -1; float tutT;
    int wxRoll; float burnT, wreckCam;
    public final Array<E> enemies = new Array<>();
    final Array<Bullet> bullets = new Array<>();
    final Array<Rocket> rockets = new Array<>();
    final Array<Ebul> ebul = new Array<>();
    public final Lock lock = new Lock();
    public final Vector3 aimDir = new Vector3(0, 0, -1), camPos = new Vector3(), aimPoint = new Vector3();
    public final Color trc = new Color(1, 0.85f, 0.45f, 1);
    public static final Vector3 AIM = new Vector3(0, 2.6f, 0.6f);
    public final Array<DmgNum> dmgNums = new Array<>();
    public float hitMark;
    final Array<Wreck> wrecks = new Array<>();
    public boolean nightRun;
    public int xpGot, rankUp = -1;
    int nextId = 1;
    float boltT;
    final Array<float[]> bolts = new Array<>();
    final Array<float[]> beams = new Array<>();
    final Array<Timer> timers = new Array<>();
    final Random R = new Random();
    public final Dyn tracers, beamDyn;
    public static final float BUL_SP = 440f;
    static final String[] GROUND_T = {"inf", "rpg", "mech", "mortar", "walker", "colossus"};
    public float T;
    // ---------- кампания ----------
    public JsonValue CM;
    public Mission obj;
    public float missionT, missionTime2;
    float timeSaid;
    JsonValue squadMix;
    public static final class Mission { public String name, kind; public float hp, max, r = 7; public final Vector3 aim = new Vector3(); public final Array<Objs.Obj> models = new Array<>(); }
    /** Итог миссии для экрана результата. */
    public boolean resWin; public boolean[] resOk; public int resGot, resReward; public String resReason = "";
    public JsonValue resM;

    public boolean hasMod(String m) {
        if (CM == null || !CM.has("mods")) return false;
        for (JsonValue v = CM.get("mods").child; v != null; v = v.next) if (v.asString().equals(m)) return true;
        return false;
    }

    public boolean isCamp() { return mode.equals("campaign") && CM != null; }

    static Mat FLASH_MAT, CHAR_MAT, SHIELD_MAT;

    public static final class Run {
        public int c;
        public Map<String, Integer> up;
        public Map<String, Boolean> ab;
    }

    public static final class Lock { public E t; public float k; public boolean beeped; }

    public static final class DmgNum { public E e; public final Vector3 p = new Vector3(); public float v, t; public String src; }

    static final class Bullet { final Vector3 pos = new Vector3(), prev = new Vector3(), o = new Vector3(), vel = new Vector3(); float life, dmg; boolean mog, flak; }

    static final class Rocket { Node3 node, flame; SmokeTrail trail; final Vector3 pos = new Vector3(), vel = new Vector3(); E tgt; float life, sp, age; }

    static final class Ebul { final Vector3 pos = new Vector3(), vel = new Vector3(); float life, dmg; String tgt; }

    static final class Wreck { Node3 g, rot; final Vector3 vel = new Vector3(), spin = new Vector3(); float t, s; boolean boss; }

    static final class Timer { float t; Runnable r; }

    public static final class E {
        public String type; public Models.EnemyModel md; public Node3 g;
        public final Vector3 pos = new Vector3(), vel = new Vector3();
        public float hp, max, r, t; public boolean boss, ground;
        public String phase = "in"; public float fire, spawnT = 6, side, sp, dmg; public String tgt = "base";
        public float data; public boolean inv; public int cloak; public float heal = 2; public int split;
        public final Array<E> nodes = new Array<>(); public float anim, alt;
        public FX.Emitter smoke, trail;
        public float ang, dir, distR; public boolean stealth; public float reveal;
        public E flock; public final Vector3 foff = new Vector3(); public E core; public float oa;
        public final Vector3 runV = new Vector3(); public boolean hasRunV; public int runI = -1; public float rt;
        public float cloakT = 6, reT = 22, shT = 6; public Node3 dome; public float flash; public boolean keep; public int id;
        public boolean pc, pcInv, net;
        public final Vector3 tgtPos = new Vector3(); public final com.badlogic.gdx.math.Quaternion tgtRot = new com.badlogic.gdx.math.Quaternion(); public boolean hasTgt;
    }

    public Game() {
        root.tag = FX.ROOT_TAG;
        Mat tm = new Mat(); tm.unshaded = true; tm.vcol = true; tm.blend = Mat.ADD; tm.depthWrite = false; tm.cull = false; tm.fog = false; tm.order = 4;
        tracers = new Dyn(tm);
        Mat bm = new Mat(); bm.unshaded = true; bm.vcol = true; bm.blend = Mat.ADD; bm.depthWrite = false; bm.cull = false; bm.order = 4;
        beamDyn = new Dyn(bm);
        ensureOverlays();
        for (String k : new String[]{"salvo", "emp"}) abUsed.put(k, false);
        for (String k : new String[]{"smoke", "repair"}) abCd.put(k, 0f);
    }

    static void ensureOverlays() {
        if (CHAR_MAT == null) { CHAR_MAT = new Mat(); CHAR_MAT.unshaded = true; CHAR_MAT.vcol = false; CHAR_MAT.color.set(0.05f, 0.04f, 0.03f, 0.55f); CHAR_MAT.blend = Mat.ALPHA; CHAR_MAT.depthWrite = false; }
        if (FLASH_MAT == null) { FLASH_MAT = Mat.unshadedAdd(new Color(1, 0.95f, 0.85f, 0.6f)); FLASH_MAT.cull = true; }
        if (SHIELD_MAT == null) { SHIELD_MAT = Mat.unshadedAdd(new Color(0.35f, 0.7f, 1, 0.22f)); }
    }

    float rr(float a, float b) { return a + R.nextFloat() * (b - a); }

    void later(float t, Runnable r) { Timer tm = new Timer(); tm.t = t; tm.r = r; timers.add(tm); }

    // ---------- кошелёк ----------
    public boolean runWallet() { return (mode.equals("endless") || mode.equals("duel")) && RUN != null; }

    // ---------- дуэль ----------
    public Duel duel;
    public boolean netView;
    public String duelRole = "";
    public float duelT, bossSpecT, bossCamPitch = 0.25f;
    public final float[] bossCd = new float[3];
    public static final class BossIn { public float mx, my, mz, cy; public int f1, f2, sp; }
    public final BossIn bossIn = new BossIn();
    public int netMis;
    public final int[] netAb = new int[4];
    public E bossE;
    public static final float DUEL_TIME = 180f;
    public static final String[] DUEL_BOSSES = {"boss", "gunship", "walker", "carrier", "ghost", "hydra", "chimera", "storm", "manta", "colossus"};
    public static final float CD_F1 = 4f, CD_F2 = 15f;
    static final Map<String, Float> DUEL_SPD = new LinkedHashMap<>(), SPEC_CD = new LinkedHashMap<>(), DUEL_HP = new LinkedHashMap<>();
    public static final Map<String, String> SPEC_SHORT = new LinkedHashMap<>(), SPEC_NAME = new LinkedHashMap<>();

    static {
        String[] k = {"storm", "manta", "colossus", "boss", "gunship", "walker", "carrier", "ghost", "hydra", "chimera"};
        float[] spd = {22, 50, 9, 30, 44, 10, 20, 36, 28, 24};
        float[] cd = {9, 11, 16, 16, 10, 12, 22, 14, 12, 30};
        float[] hp = {300, 220, 380, 260, 230, 340, 360, 210, 310, 330};
        String[] sh = {"МОЛНИЯ", "ПИКЕ", "КУПОЛ", "ЩИТ", "ФОРСАЖ", "ЗАЛП", "РОЙ+", "ТЕНЬ", "ВЕЕР", "УЗЛЫ"};
        String[] nm = {"Молния", "Пике", "Купол", "Щит", "Форсаж", "Артобстрел", "Большой рой", "Невидимость", "Веер ракет", "Узлы щита"};
        for (int i = 0; i < k.length; i++) { DUEL_SPD.put(k[i], spd[i]); SPEC_CD.put(k[i], cd[i]); DUEL_HP.put(k[i], hp[i]); SPEC_SHORT.put(k[i], sh[i]); SPEC_NAME.put(k[i], nm[i]); }
    }

    public static float specCd(String t) { Float v = SPEC_CD.get(t); return v == null ? 15f : v; }

    void push(Object... ev) { if (duel != null && mode.equals("duel")) duel.pushEvent(ev); }

    public void startDuel(String boss, String veh, int theme) {
        clearBattle();
        mode = "duel"; CM = null;
        VEH = veh;
        RUN = new Run();
        RUN.c = 0;
        RUN.up = G.zeroUp(); RUN.up.put("armor", 3); RUN.up.put("cool", 1);
        RUN.ab = new LinkedHashMap<>();
        for (String k : new String[]{"salvo", "emp", "smoke", "repair"}) RUN.ab.put(k, true);
        dif = G.D.get("DIFF").get("normal");
        difSpd = 1; difDmg = 0.6f; difHp = 1; difCoin = 1;
        nightRun = false;
        applyStats();
        setVehicleModel();
        JsonValue tr = G.D.get("SK_TR").get(G.SV.skTr);
        if (tr == null) tr = G.D.get("SK_TR").get("gold");
        trc.set(G.hex(tr.getString("col")));
        world.build(theme, false, 0, 0, 0, "", false);
        for (Vector3 s : world.stacks) FX.chimney(s);
        obj = null; setupMogs();
        score = 0; wave = 1; hp = st("maxHp"); heat = 0; overheated = false; kills = 0; combo = 0; runCoins = 0;
        missiles = (int) st("maxMis"); reloadT = 0; chanT = 0; firing = false;
        aimYaw = 0; aimPitch = 0.2f; toSpawn = 0; between = 0; bossWave = false; missionTime = 0;
        abUsed.put("salvo", false); abUsed.put("emp", false); abCd.put("smoke", 0f); abCd.put("repair", 0f);
        smokeT = 0; repairT = 0; empT = 0; scoutQ = 0; squads = 0; nohitWaves = 0;
        duelT = DUEL_TIME; bossCd[0] = 3; bossCd[1] = 6; bossCd[2] = 8; bossSpecT = 0; bossCamPitch = 0.25f;
        bossIn.mx = 0; bossIn.my = 0; bossIn.mz = 0; bossIn.cy = MathUtils.PI; bossIn.f1 = 0; bossIn.f2 = 0; bossIn.sp = 0;
        netMis = 0; for (int i = 0; i < 4; i++) netAb[i] = 0; tut = -1;
        bossE = null;
        if (!netView) {
            E b = spawnEnemy(boss, null, 0f, 600f);
            b.pc = true; Float h = DUEL_HP.get(boss); b.hp = h == null ? 180 : h; b.max = b.hp;
            if (boss.equals("ghost")) b.cloak = 0;
            bossE = b;
            bossIn.cy = MathUtils.atan2(b.pos.x, b.pos.z);
        }
        state = "run";
        syncNoses();
        if (ui != null) ui.banner("Дуэль", true, "Вы — " + (duelRole.equals("pvo") ? "ПВО: сбейте босса за 3 минуты" : "босс: разрушьте комплекс"));
        G.sfx("boss");
    }

    public E duelBoss() {
        if (bossE != null) return bossE;
        for (E e : bosses) return e;
        return null;
    }

    void duelRules(float dt) {
        duelT = Math.max(0, duelT - dt);
        for (int i = 0; i < 3; i++) bossCd[i] = Math.max(0, bossCd[i] - dt);
        bossSpecT = Math.max(0, bossSpecT - dt);
        if (bossE != null && bossSpecT <= 0 && bossE.pcInv) {
            bossE.inv = false; bossE.pcInv = false; bossE.cloak = 0;
            if (bossE.type.equals("colossus")) shieldVis(bossE, false);
            bossE.md.body.alpha = 1; bossE.md.body.overlay = null;
        }
        if (bossE != null && (bossE.hp <= 0 || !enemies.contains(bossE, true))) {
            if (duel != null) duel.finishHost("pvo", "Босс сбит!");
            return;
        }
        if (duelT <= 0 && duel != null) duel.finishHost("pvo", "Время вышло — оборона выстояла");
    }

    static Mat SHIELD_OVER;

    Vector3 pcBoss(final E e, float dt, Vector3 to) {
        BossIn b = bossIn;
        float cy = b.cy;
        float fx = -MathUtils.sin(cy), fz = -MathUtils.cos(cy), rx = MathUtils.cos(cy), rz = -MathUtils.sin(cy);
        Float sp0 = DUEL_SPD.get(e.type);
        float spd = (sp0 == null ? 28f : sp0) * ((e.type.equals("gunship") || e.type.equals("manta")) && bossSpecT > 0 ? 2.2f : 1f) * (empT > 0 ? 0.45f : 1f);
        Vector3 mv = new Vector3(b.mx, b.my, b.mz);
        if (mv.len() > 1) mv.nor();
        Vector3 want = new Vector3(fx * mv.z + rx * mv.x, 0, fz * mv.z + rz * mv.x).scl(spd);
        float gh = height(e.pos.x, e.pos.z);
        boolean walk = e.type.equals("walker") || e.type.equals("colossus");
        if (walk) {
            want.y = 0; e.anim += dt * want.len() * 0.15f;
            if (e.md.legs.size > 0) { float s = MathUtils.sin(e.anim) * 0.45f; e.md.legs.get(0).setEuler(s, 0, 0); e.md.legs.get(1).setEuler(-s, 0, 0); }
        } else {
            want.y = mv.y * spd * 0.7f;
            float alt = e.pos.y - gh;
            if (alt < 30) want.y = Math.max(want.y, (30 - alt) * 1.5f);
            if (alt > 280) want.y = Math.min(want.y, (280 - alt) * 1.5f);
        }
        float hp2 = (float) Math.sqrt(e.pos.x * e.pos.x + e.pos.z * e.pos.z);
        if (hp2 > 750) want.add(tA.set(-e.pos.x, 0, -e.pos.z).nor().scl((hp2 - 750) * 0.8f));
        if (hp2 < 45) want.add(tA.set(e.pos.x, 0, e.pos.z).nor().scl((45 - hp2) * 2f));
        e.vel.lerp(want, 1 - (float) Math.exp(-dt * 2));
        if (e.type.equals("walker")) { e.vel.y = 0; e.pos.y = gh + 8.4f * e.g.scl.y; }
        if (e.type.equals("colossus")) { e.vel.y = 0; e.pos.y = gh; }
        if (b.f1 > 0) {
            b.f1 = 0;
            if (bossCd[0] <= 0) {
                bossCd[0] = CD_F1 * ((e.type.equals("hydra") || e.type.equals("gunship")) ? 0.85f : 1f);
                int n = e.type.equals("gunship") ? 4 : 3;
                for (int k = 0; k < n; k++) later(k * 0.2f, () -> { if (e.hp > 0 && enemies.contains(e, true)) enemyShot(e, "erk", 66); });
                if (walk) enemyShot(e.pos.cpy().add(0, 12, 0), "base", "shell", 0);
                if (e.type.equals("manta")) for (int k = 0; k < 3; k++) later(k * 0.25f, () -> { if (e.hp > 0 && enemies.contains(e, true)) enemyShot(e, "shell", 0); });
            }
        }
        if (b.f2 > 0) {
            b.f2 = 0;
            if (bossCd[1] <= 0 && empT <= 0) { bossCd[1] = CD_F2; pcSwarm(e); }
        }
        if (b.sp > 0) {
            b.sp = 0;
            if (bossCd[2] <= 0) { bossCd[2] = specCd(e.type); pcSpecial(e); }
        }
        if (e.type.equals("core") || e.type.equals("boss")) e.g.rotateY(dt * 0.2f);
        return (mv.len() > 0.1f || walk) ? new Vector3(fx, 0, fz) : new Vector3(to.x, 0, to.z);
    }

    void pcSwarm(E e) {
        Vector3 at = e.pos.cpy().add(0, -6, 0);
        switch (e.type) {
            case "carrier": for (int k = 0; k < 6; k++) { E m = spawnEnemy("mini", at.cpy().add(rr(-8, 8), 0, rr(-8, 8))); m.vel.set(0, -8, 0); } break;
            case "hydra": for (int k = 0; k < 2; k++) spawnEnemy("heavy", at.cpy().add(rr(-15, 15), 0, rr(-10, 10))); spawnEnemy("kami", at); break;
            case "chimera": for (int k = 0; k < 2; k++) spawnEnemy("kami", at.cpy().add(rr(-10, 10), 0, 0)); spawnEnemy("split", at); break;
            case "gunship": case "manta": for (int k = 0; k < 3; k++) spawnEnemy("wing", at.cpy().add(rr(-10, 10), 0, rr(-10, 10))); break;
            case "storm": for (int k = 0; k < 3; k++) spawnEnemy("zig", at.cpy().add(rr(-12, 12), 0, rr(-12, 12))); break;
            case "ghost": for (int k = 0; k < 2; k++) spawnEnemy("stealth", at.cpy().add(rr(-10, 10), 0, rr(-10, 10))); break;
            case "walker": case "colossus":
                for (int k = 0; k < 4; k++) spawnEnemy(k % 2 == 0 ? "inf" : "rpg", new Vector3(e.pos.x + rr(-15, 15), 0, e.pos.z + rr(-15, 15)));
                break;
            default: for (int k = 0; k < 4; k++) { E q = spawnEnemy("quad", at.cpy().add(rr(-12, 12), 0, rr(-8, 8))); q.vel.set(0, -4, 0); }
        }
        push("toast", "Босс выпустил рой!");
        if (ui != null && duelRole.equals("pvo")) ui.toast("Босс выпустил рой!");
    }

    void pcSpecial(final E e) {
        String nm = SPEC_NAME.containsKey(e.type) ? SPEC_NAME.get(e.type) : "";
        switch (e.type) {
            case "boss":
                bossSpecT = 3.5f; e.inv = true; e.pcInv = true;
                if (SHIELD_OVER == null) SHIELD_OVER = Mat.unshadedAdd(new Color(0.4f, 0.75f, 1, 0.35f));
                e.md.body.overlay = SHIELD_OVER;
                break;
            case "ghost": bossSpecT = 4.5f; e.inv = true; e.pcInv = true; e.cloak = 1; e.md.body.alpha = 0.1f; break;
            case "gunship": bossSpecT = 3f; break;
            case "manta": bossSpecT = 2.5f; break;
            case "storm": for (int k = 0; k < 3; k++) later(k * 0.45f, () -> { if (e.hp > 0 && enemies.contains(e, true)) lightning(e.pos.cpy().add(0, -6, 0), e); }); break;
            case "colossus": bossSpecT = 4f; e.inv = true; e.pcInv = true; shieldVis(e, true); break;
            case "walker": for (int k = 0; k < 5; k++) later(k * 0.35f, () -> { if (e.hp > 0 && enemies.contains(e, true)) enemyShot(e.pos.cpy().add(0, 12, 0), "base", "shell", 0); }); break;
            case "carrier": for (int k = 0; k < 12; k++) { E m = spawnEnemy("mini", e.pos.cpy().add(rr(-14, 14), -10, rr(-14, 14))); m.vel.set(0, -10, 0); } break;
            case "hydra": for (int k = 0; k < 8; k++) later(k * 0.1f, () -> { if (e.hp > 0 && enemies.contains(e, true)) enemyShot(e, "erk", 70); }); break;
            case "chimera": if (e.nodes.size == 0) spawnNodes(e, 4); break;
            default: break;
        }
        push("toast", "Босс: " + nm + "!");
        if (ui != null) ui.toast("Босс: " + nm + "!");
    }

    void bossCamera(float dt) {
        E be = duelBoss();
        if (be == null) return;
        float cy = bossIn.cy;
        Vector3 fw = new Vector3(-MathUtils.sin(cy) * MathUtils.cos(bossCamPitch), -MathUtils.sin(bossCamPitch), -MathUtils.cos(cy) * MathUtils.cos(bossCamPitch));
        float dist = MathUtils.clamp(be.r * 2.4f + 16f, 26f, 90f);
        Vector3 bp = be.g.pos;
        Vector3 cpos = new Vector3(bp).mulAdd(fw, -dist).add(0, be.r * 0.6f + 4f, 0);
        cpos.y = Math.max(cpos.y, height(cpos.x, cpos.z) + 3);
        if (shake > 0) { cpos.add(rr(-1, 1) * shake * 0.5f, rr(-1, 1) * shake * 0.5f, 0); shake = Math.max(0, shake - dt * 2.2f); }
        if (cam.position.dst(cpos) < 60) cam.position.lerp(cpos, Math.min(1f, dt * 8f)); else cam.position.set(cpos);
        cam.direction.set(bp).mulAdd(fw, 60).add(0, be.r * 0.3f, 0).sub(cam.position).nor();
        cam.up.set(Vector3.Y);
        cam.update();
        camPos.set(cam.position);
    }
    public int coins() { return runWallet() ? RUN.c : G.SV.c; }
    public void setCoins(int v) { if (runWallet()) RUN.c = v; else G.SV.c = v; }
    public Map<String, Integer> UP() {
        if (hasMod("noUpg")) return G.zeroUp();
        return runWallet() ? RUN.up : G.SV.upv.get(VEH);
    }
    public Map<String, Boolean> AB() { return runWallet() ? RUN.ab : G.SV.ab; }
    int u(String k) { Integer v = UP().get(k); return v == null ? 0 : v; }
    public float st(String k) { Float v = ST.get(k); return v == null ? 0 : v; }

    public void applyStats() {
        String V = VEH;
        ST.put("maxHp", (float) Math.round((100 + 20 * u("armor")) * (hasMod("glass") ? 0.5f : 1f)));
        ST.put("regen", 10f + 4 * u("armor"));
        ST.put("gun", (!(V.equals("tor") || V.equals("luch")) && !hasMod("missilesOnly")) ? 1f : 0f);
        ST.put("beam", V.equals("luch") && !hasMod("missilesOnly") ? 1f : 0f);
        ST.put("ldps", 9f * (1 + 0.25f * u("lens")));
        float[] capk = {1f, 1.2f, 1.43f, 1.71f, 2f};
        ST.put("lheat", 1f / (3.5f * capk[MathUtils.clamp(u("cap"), 0, 4)]));
        ST.put("chain", (float) u("chain"));
        ST.put("rate", (V.equals("shilka") ? 26f : (V.equals("grom") ? 4.2f : 13f)) * (1 + 0.15f * u("rate")));
        ST.put("flak", V.equals("grom") ? 1f : 0f);
        ST.put("fdmg", 2.4f * (1 + 0.3f * u("fpow")));
        ST.put("frad", 13f * (1 + 0.15f * u("frad")));
        ST.put("fprox", 6f + 2f * u("fuse"));
        ST.put("dmg", 1 + 0.5f * u("dmg"));
        ST.put("heatPer", (V.equals("shilka") ? 0.021f : (V.equals("grom") ? 0.085f : 0.042f)) * (1 - 0.12f * u("belt")));
        ST.put("cool", 0.3f * (1 + 0.25f * (u("cool") + u("lcool"))));
        ST.put("spread", (V.equals("shilka") ? 0.008f : 0.004f) * (1 - 0.2f * u("spread")));
        ST.put("track", 0.25f * u("track"));
        ST.put("maxMis", (float) (hasMod("gunOnly") ? 0 : (V.equals("tor") ? 8 + u("count") : (V.equals("pantsir") ? 4 + u("count") : (hasMod("missilesOnly") ? 4 : 0)))));
        ST.put("reload", (V.equals("tor") ? 2.2f : 3.5f) * (float) Math.pow(0.85, u("reload")));
        ST.put("misSpd", 1 + 0.2f * u("speed"));
        ST.put("warhead", 1 + 0.3f * u("warhead"));
        ST.put("chan", 0.42f * (float) Math.pow(0.85, u("channels")));
        ST.put("twin", 0.15f * u("twin"));
        ST.put("mog", (float) u("mog"));
    }

    public void setVehicleModel() {
        if (vm != null) {
            vm.root.remove();
            for (Object[] w : vm.wreck) ((Node3) w[0]).remove();
        }
        vm = Models.vehicle(VEH, G.SV.skVeh);
        vm.root.pos.y = 0.25f;
        pitchBase = vm.pitch.pos.cpy(); rootBase = vm.root.pos.cpy();
        root.add(vm.root);
        vm.root.updateGlobal(root.global);
    }

    public void syncNoses() {
        int n = vm.noseMeshes.size;
        for (int i = 0; i < n; i++) vm.noseMeshes.get(i).visible = i < Math.min(n, missiles) && st("maxMis") > 0 && !vm.broken;
    }

    public void explosion(Vector3 p, float s) { explosion(p, s, true); }

    public void explosion(Vector3 p, float s, boolean smoke) {
        push("boom", p.x, p.y, p.z, s);
        FX.explosion(p, s, smoke);
        float d = p.dst(cam.position);
        shake = Math.max(shake, MathUtils.clamp(1.4f * s - d / 120f, 0, 1.2f));
        G.sfx("boom", MathUtils.clamp(1.2f - d / 900f, 0.12f, 1f) * Math.min(1.4f, s), 30);
    }

    int lvl() { return isCamp() ? CM.getInt("lvl", 0) + wave : wave; }

    float spdMul() {
        float cm = 1f;
        if (CM != null) cm = CM.getFloat("spdMul", hasMod("fast") ? 1.5f : 1f);
        return difSpd * cm * (1 + Math.min(0.5f, lvl() * 0.012f));
    }

    public static boolean isBoss(String t) { return G.D.get("BOSS_INFO").has(t); }

    static boolean isGround(String t) { for (String g : GROUND_T) if (g.equals(t)) return true; return false; }

    public float height(float x, float z) { return world.height(x, z); }

    public E spawnEnemy(String type) { return spawnEnemy(type, null, Float.NaN, Float.NaN); }

    public E spawnEnemy(String type, Vector3 at) { return spawnEnemy(type, at, Float.NaN, Float.NaN); }

    public E spawnEnemy(String type, Vector3 at, float optAng, float optDist) {
        JsonValue D = G.D.get("DT").get(type);
        if (D == null) D = G.D.get("DT").get("quad");
        String acc = ((CM != null && CM.getInt("lvl", 0) >= 18) || wave >= 20) ? "manti" : "harpy";
        Models.EnemyModel md = Models.enemy(type, acc);
        boolean boss = isBoss(type), ground = isGround(type);
        float spread = mode.equals("endless") ? Math.min(MathUtils.PI * 0.95f, 0.6f + wave * 0.12f) : Math.min(MathUtils.PI * 0.9f, 0.7f + lvl() * 0.04f);
        float a = Float.isNaN(optAng) ? rr(-spread, spread) : optAng;
        float Dd = Float.isNaN(optDist) ? (boss ? 650f : (ground ? rr(380, 470) : rr(420, 560))) : optDist;
        Vector3 pos = new Vector3();
        if (at != null) pos.set(at);
        else {
            float x = -MathUtils.sin(a) * Dd, z = -MathUtils.cos(a) * Dd;
            float alt = 0;
            if (!ground) {
                if (type.equals("kami")) alt = rr(110, 150);
                else if (type.equals("wing")) alt = rr(60, 110);
                else if (boss) alt = type.equals("walker") ? 0 : (type.equals("carrier") ? 170 : 130);
                else if (type.equals("scout")) alt = rr(80, 120);
                else alt = rr(40, 90);
            }
            pos.set(x, height(x, z) + alt, z);
        }
        md.g.pos.set(pos);
        root.add(md.g);
        float hpMul = boss ? (CM != null ? CM.getFloat("bossMul", 1.8f) : 1.8f) * 1.6f * (1 + lvl() * 0.03f) : 1f * (1 + lvl() * 0.03f);
        float ehp = Math.max(1f, Math.round(D.getFloat("hp") * hpMul * difHp));
        JsonValue spA = D.get("sp");
        E e = new E();
        e.type = type; e.md = md; e.g = md.g; e.pos.set(pos); e.hp = ehp; e.max = ehp; e.r = md.r; e.t = R.nextFloat() * 10;
        e.boss = boss; e.ground = ground; e.fire = rr(2, 4); e.side = R.nextFloat() < 0.5f ? -1 : 1;
        e.sp = rr(spA.getFloat(0), spA.getFloat(1)) * 0.2f * spdMul();
        e.dmg = D.getFloat("dmg") * 0.6f * difDmg;
        e.alt = pos.y - height(pos.x, pos.z);
        if (obj != null && !boss && !ground && !(type.equals("scout") || type.equals("erk") || type.equals("shell") || type.equals("node") || type.equals("relay"))
                && R.nextFloat() < CM.getFloat("objShare", 0.5f)) e.tgt = "obj";
        if (type.equals("scout")) { e.ang = a; e.dir = R.nextFloat() < 0.5f ? -1 : 1; e.distR = rr(260, 330); }
        if (type.equals("stealth")) { e.stealth = true; e.reveal = 0; setBodyAlpha(e, 0.15f); }
        if (type.equals("erk")) e.trail = FX.trail(md.g, false);
        if (type.equals("core") || type.equals("chimera")) spawnNodes(e, type.equals("chimera") ? 5 : 4);
        e.id = nextId++;
        enemies.add(e);
        if (boss) bosses.add(e);
        return e;
    }

    void setBodyAlpha(E e, float a) {
        e.md.body.alpha = a;
        if (e.md.glow != null) e.md.glow.alpha = a;
        if (e.md.rot != null) e.md.rot.visible = a > 0.3f;
    }

    void spawnNodes(E core, int n) {
        for (int i = 0; i < n; i++) {
            E nd = spawnEnemy("node", core.pos);
            nd.core = core; nd.oa = (float) i / n * MathUtils.PI2;
            nd.hp = Math.round(9 * 1.6f * (1 + lvl() * 0.03f)); nd.max = nd.hp;
            core.nodes.add(nd);
        }
        core.inv = true;
    }

    public void removeEnemy(E e) {
        if (e.trail != null) { FX.detach(e.trail); e.trail = null; }
        if (!e.keep) e.g.remove();
        enemies.removeValue(e, true);
        bosses.removeValue(e, true);
    }

    public int addCoins(float n) {
        int k = Math.max(1, Math.round(n));
        setCoins(coins() + k); runCoins += k; qProg("coins", k, false);
        return k;
    }

    void dmgNum(E e, float dmg, String src) {
        float v = dmg * 10f;
        for (DmgNum n : dmgNums) {
            if (n.e == e && n.src.equals(src) && n.t > 0.5f) {
                n.v += v; n.t = 0.85f; n.p.set(visPos(e, tmpV)).add(0, e.r * 0.9f + 1f, 0); return;
            }
        }
        if (dmgNums.size >= 40) dmgNums.removeIndex(0);
        DmgNum d = new DmgNum(); d.e = e; d.p.set(visPos(e, tmpV)).add(0, e.r * 0.9f + 1f, 0); d.v = v; d.t = 0.85f; d.src = src;
        dmgNums.add(d);
    }

    final Vector3 tmpV = new Vector3(), tA = new Vector3(), tB = new Vector3(), tC = new Vector3();

    public void damageEnemy(E e, float dmg, Vector3 p, String src) {
        if (e.hp <= 0) return;
        if (e.inv) { if (p != null) FX.sparks(p, 3, new Color(0.6f, 0.8f, 1, 1)); return; }
        if (src.equals("gun") || src.equals("beam") || src.equals("mog") || src.equals("flak")) {
            if (e.type.equals("mech")) dmg *= src.equals("flak") ? 0.7f : 0.35f;
            else if (e.type.equals("walker") || e.type.equals("colossus")) dmg *= 0.5f;
            else if (e.type.equals("armor")) {
                dmg *= src.equals("beam") ? 0.6f : 0.2f;
                if (p != null && R.nextFloat() < 0.5f) FX.sparks(p, 4, new Color(0.6f, 0.85f, 1, 1));
            }
        }
        if (e.stealth) e.reveal = 2.5f;
        if (netView) {
            if (p != null) FX.sparks(p, 3);
            if (src.equals("gun")) { hitMark = 0.13f; G.sfx("tink", 0.22f, 45); }
            return;
        }
        e.hp -= dmg;
        if (p != null) FX.sparks(p, 3);
        if (!(e.type.equals("erk") || e.type.equals("shell"))) {
            dmgNum(e, dmg, src);
            e.md.body.overlay = FLASH_MAT; e.flash = 0.07f;
        }
        if (src.equals("gun")) { hitMark = 0.13f; G.sfx("tink", 0.22f, 45); }
        else if (src.equals("beam")) hitMark = 0.08f;
        if (e.hp < e.max * 0.5f && e.smoke == null && !(e.type.equals("erk") || e.type.equals("shell") || e.type.equals("node")) && !e.ground)
            e.smoke = FX.burner(e.g, false, e.boss ? 3f : 1f);
        if (e.hp <= 0) killEnemy(e);
    }

    void killEnemy(E e) {
        push("kill", (float) e.id);
        JsonValue D = G.D.get("DT").get(e.type);
        if (D == null) D = G.D.get("DT").get("quad");
        float sz = e.boss ? 4f : ((e.type.equals("heavy") || e.type.equals("mech")) ? 1.8f : ((e.type.equals("erk") || e.type.equals("shell") || e.type.equals("mini")) ? 0.6f : 1.1f));
        explosion(e.pos, sz);
        if (e.boss) {
            for (int k = 1; k < 4; k++) {
                final Vector3 bp = e.pos.cpy().add(rr(-10, 10), rr(-3, 3), rr(-6, 6));
                later(k * 0.22f, () -> explosion(bp, 2.5f));
            }
            statAdd("bosses", 1); qProg("boss", 1, false); statAdd("b_" + e.type, 1);
            if (ui != null) ui.toast(G.D.get("BOSS_INFO").get(e.type).getString("short") + " уничтожен!");
            shake = Math.max(shake, 1.2f); G.hap(80);
            FX.light(e.pos, 160f, 1f, 0.6f, 0.3f, 9f, 0.6f);
            G.sfx("boom2", 1f, 80);
            startKillCam(e);
        }
        if (e.ground) FX.puff(tmpV.set(e.pos).add(0, 1, 0), 3f, new Color(0.1f, 0.1f, 0.1f, 1), 0.6f, 5f, 3f);
        score += Math.round(D.getFloat("pts") * 10 * (1 + (float) Math.floor(combo / 5f) * 0.1f));
        addCoins(D.getFloat("coin") * difCoin * (mode.equals("endless") ? 2f : 0.5f)); G.sfx("coin", 0.5f, 60);
        combo += 1; comboT = 3f; kills += 1;
        statAdd("kills", 1); qProg("kill", 1, false);
        if (e.type.equals("heavy")) qProg("heavy", 1, false);
        if (e.type.equals("kami") || e.type.equals("relay") || e.type.equals("mini")) statAdd("k_" + e.type, 1);
        if (!VEH.equals("pantsir")) statAdd("v_" + VEH, 1);
        if (e.type.equals("split")) {
            for (int i = 0; i < 3; i++) {
                E m = spawnEnemy("mini", e.pos.cpy().add(rr(-3, 3), rr(-2, 2), rr(-3, 3)));
                m.vel.set(rr(-10, 10), rr(-4, 4), rr(-10, 10)); m.tgt = e.tgt;
            }
        }
        if (e.core != null) {
            E c = e.core;
            c.nodes.removeValue(e, true);
            if (c.nodes.size == 0) { c.inv = false; if (ui != null) ui.toast("Щит снят — бей по ядру!"); }
        }
        if (e.type.equals("core") || e.type.equals("chimera")) for (E n : new Array<>(e.nodes)) killEnemy(n);
        boolean noWreck = e.type.equals("erk") || e.type.equals("shell") || e.type.equals("node") || e.type.equals("core") || e.type.equals("chimera") || e.type.equals("mini");
        if (!e.ground && !noWreck) makeWreck(e, sz);
        else if (e.type.equals("mini") && R.nextFloat() < 0.5f) makeWreck(e, sz);
        removeEnemy(e);
    }

    /** Замедление и облёт камерой вокруг падающего босса. */
    void startKillCam(E e) {
        if (mode.equals("duel") || netView || !state.equals("run") || (vm != null && vm.broken)) return;
        killNode = e.g; killFocus.set(e.pos);
        killR = MathUtils.clamp(e.r * 3.4f + 20f, 28f, 90f);
        killA0 = MathUtils.atan2(camPos.x - e.pos.x, camPos.z - e.pos.z) - 0.5f;
        JsonValue bi = G.D.get("BOSS_INFO").get(e.type);
        killName = bi != null ? bi.getString("short", "Босс") : "Босс";
        killCamT = KILL_DUR;
        BlackBox.log("облёт босса: " + e.type);
    }

    /** Множитель скорости времени для боя (1 — обычная скорость). */
    public float timeScale() {
        if (killCamT <= 0) return 1f;
        float p = 1f - killCamT / KILL_DUR;
        if (p < 0.06f) return MathUtils.lerp(1f, 0.2f, p / 0.06f);
        if (p > 0.78f) return MathUtils.lerp(0.2f, 1f, (p - 0.78f) / 0.22f);
        return 0.2f;
    }

    /** Насколько сейчас включена кинорамка (0..1). */
    public float killCamK() {
        if (killCamT <= 0) return 0;
        float p = 1f - killCamT / KILL_DUR;
        return Math.min(MathUtils.clamp(p / 0.1f, 0, 1), MathUtils.clamp((1f - p) / 0.15f, 0, 1));
    }

    void makeWreck(E e, float sz) {
        if (wrecks.size >= 22) { Wreck old = wrecks.removeIndex(0); old.g.remove(); }
        e.keep = true;
        e.md.body.overlay = CHAR_MAT;
        FX.burner(e.g, true, MathUtils.clamp(sz, 0.8f, 3f));
        Wreck w = new Wreck();
        w.g = e.g; w.rot = e.md.rot;
        w.vel.set(e.vel).scl(0.5f).add(rr(-7, 7), rr(2, 9), rr(-7, 7));
        if (e.boss) w.vel.set(e.vel).scl(0.2f).add(rr(-3, 3), 2, rr(-3, 3));
        w.spin.set(rr(-4, 4), rr(-3, 3), rr(-5, 5)).scl(e.boss ? 0.25f : 1f);
        w.boss = e.boss; w.s = sz;
        wrecks.add(w);
    }

    void updateWrecks(float dt) {
        for (int i = wrecks.size - 1; i >= 0; i--) {
            Wreck w = wrecks.get(i);
            w.t += dt;
            w.vel.y -= (w.boss ? 11f : 24f) * dt;
            w.g.pos.mulAdd(w.vel, dt);
            w.g.rotateLocal(Vector3.X, w.spin.x * dt); w.g.rotateLocal(Vector3.Y, w.spin.y * dt); w.g.rotateLocal(Vector3.Z, w.spin.z * dt);
            if (w.rot != null) w.rot.rotateY(dt * Math.max(0, 9f - w.t * 4f));
            Vector3 p = w.g.pos;
            if (p.y <= height(p.x, p.z) + 0.6f || w.t > 9f) {
                explosion(p, w.boss ? 3.2f : MathUtils.clamp(w.s * 0.6f, 0.5f, 1.4f));
                FX.puff(tmpV.set(p).add(0, 1.5f, 0), w.boss ? 4f : 2f, new Color(0.22f, 0.19f, 0.15f, 1), 0.55f, 3f, 2f);
                if (w.boss) shake = Math.max(shake, 1.1f);
                w.g.remove(); wrecks.removeIndex(i);
            }
        }
    }

    void updateJuice(float dt) {
        hitMark = Math.max(0, hitMark - dt);
        if (boltT > 0) { boltT -= dt; if (boltT <= 0) bolts.clear(); }
        for (int i = dmgNums.size - 1; i >= 0; i--) {
            DmgNum n = dmgNums.get(i);
            n.t -= dt; n.p.y += dt * 9f;
            if (n.t <= 0) dmgNums.removeIndex(i);
        }
    }

    String chooseFrom(Map<String, Float> mix) {
        float s = 0;
        for (float v : mix.values()) s += v;
        float r = R.nextFloat() * s;
        for (Map.Entry<String, Float> e : mix.entrySet()) { r -= e.getValue(); if (r <= 0) return e.getKey(); }
        return mix.keySet().iterator().next();
    }

    void spawnFromMix(Map<String, Float> mix) {
        String t = chooseFrom(mix);
        if (t.equals("flock")) {
            E lead = spawnEnemy("mini", null, rr(-1, 1), Float.NaN);
            for (int i = 0; i < 7; i++) {
                E m = spawnEnemy("mini", lead.pos.cpy().add(rr(-6, 6), rr(-3, 3), rr(-6, 6)));
                m.flock = lead; m.foff.set(rr(-7, 7), rr(-3, 4), rr(-7, 7));
            }
            if (ui != null && R.nextFloat() < 0.5f) ui.comm("lisa", "Стая! Бей очередью в центр!");
            return;
        }
        if (t.equals("swarm")) {
            E base = spawnEnemy("mini", null, rr(-1, 1), Float.NaN);
            for (int i = 0; i < 4; i++) spawnEnemy("mini", base.pos.cpy().add(rr(-10, 10), rr(-5, 5), rr(-10, 10)));
            return;
        }
        spawnEnemy(t);
    }

    void spawnSquad(Map<String, Integer> mix) {
        float a = rr(-0.8f, 0.8f); int n = 0;
        for (Map.Entry<String, Integer> e : mix.entrySet())
            for (int i = 0; i < e.getValue(); i++) { spawnEnemy(e.getKey(), null, a + rr(-0.12f, 0.12f), rr(380, 450)); n++; }
        if (ui != null && R.nextFloat() < 0.6f) ui.comm("lisa", "Наземная группа, " + n + " целей! Опусти ствол!");
    }

    JsonValue campWaveCfg() {
        if (CM != null && CM.has("waves")) return CM.get("waves").get(Math.max(0, Math.min(wave - 1, CM.get("waves").size - 1)));
        return null;
    }

    String bossTypeForWave() {
        JsonValue c = campWaveCfg();
        if (c != null && c.has("boss")) {
            JsonValue b = c.get("boss");
            if (b.isString()) return b.asString();
            if (b.isBoolean() && b.asBoolean()) return "boss";
        }
        String[] rot = {"boss", "gunship", "walker", "carrier", "ghost", "hydra", "storm", "manta", "colossus"};
        return rot[Math.max(0, (wave / 5 - 1)) % rot.length];
    }

    Vector3 aimTarget(E e) { return tgtPos(e.tgt); }

    Vector3 tgtPos(String tgt) { return ("obj".equals(tgt) && obj != null && obj.hp > 0) ? obj.aim : AIM; }

    void face(E e, Vector3 dir) {
        if (dir.len2() < 0.01f) return;
        tA.set(dir).nor();
        e.g.lookDir(tA, Math.abs(tA.y) < 0.98f ? Vector3.Y : Vector3.X);
    }

    void enemyShot(Vector3 from, String tgt, String kind, float speed) {
        Vector3 Tt = tgtPos(tgt);
        if (kind.equals("erk")) {
            E r = spawnEnemy("erk", from);
            r.vel.set(Tt).sub(from).nor().scl(speed * 0.6f); r.sp = speed * 1.6f; r.tgt = tgt; r.hp = 1; r.max = 1; r.dmg = 8f * difDmg;
            return;
        }
        E s = spawnEnemy("shell", from);
        float dx = Tt.x - from.x + rr(-10, 10), dz = Tt.z - from.z + rr(-10, 10), dy = Tt.y - from.y, tt = rr(4f, 5.5f);
        s.vel.set(dx / tt, (dy + 0.5f * 18 * tt * tt) / tt, dz / tt); s.hp = 1; s.max = 1; s.dmg = 6f * difDmg; s.tgt = tgt;
    }

    void enemyShot(E src, String kind, float speed) { enemyShot(src.pos.cpy(), src.tgt, kind, speed); }

    boolean hidden(E e) {
        if (e.cloak != 0) return true;
        if (e.stealth) return e.reveal <= 0 && e.pos.dst(camPos) > 170f;
        return false;
    }

    public Vector3 visPos(E e, Vector3 out) {
        out.set(e.pos);
        if (e.ground && !e.type.equals("walker")) out.y += e.r * 0.8f;
        return out;
    }

    int liveNonShots() {
        int n = 0;
        for (E o : enemies) if (!(o.type.equals("shell") || o.type.equals("erk"))) n++;
        return n;
    }

    void updateEnemies(float dt) {
        float slow = empT > 0 ? 0.35f : 1f;
        for (int i = enemies.size - 1; i >= 0; i--) {
            if (i >= enemies.size) continue;
            E e = enemies.get(i);
            e.t += dt;
            if (e.stealth) {
                e.reveal = Math.max(0, e.reveal - dt);
                boolean vis = !hidden(e);
                float tr = 1 - e.md.body.alpha;
                tr = MathUtils.lerp(tr, vis ? 0.25f : 0.86f, Math.min(1f, dt * 6f));
                setBodyAlpha(e, 1 - tr);
            }
            if (e.flock != null) {
                E ld = e.flock;
                if (ld.hp > 0 && enemies.contains(ld, true)) {
                    tA.set(e.foff).rotateRad(Vector3.Y, e.t * 0.8f).add(ld.pos);
                    e.pos.lerp(tA, Math.min(1f, dt * 2.5f));
                } else e.flock = null;
            }
            if (e.flash > 0) { e.flash -= dt; if (e.flash <= 0 && e.md.body.overlay == FLASH_MAT) e.md.body.overlay = null; }
            Vector3 Tt = aimTarget(e);
            Vector3 to = tB.set(Tt).sub(e.pos);
            float dist = to.len(), hd = (float) Math.sqrt(to.x * to.x + to.z * to.z);
            Vector3 want = tC.setZero();
            float sp = e.sp * slow;
            Vector3 faceDir = null;
            String ty = e.type;
            switch (ty) {
                case "quad": case "mini": case "decoy": case "zig": case "split": {
                    if (!e.phase.equals("dive")) {
                        want.set(to.x, 0, to.z).nor().scl(sp);
                        float ca = (ty.equals("mini") ? 28f : 38f) + MathUtils.sin(e.t) * 8;
                        want.y = (height(e.pos.x, e.pos.z) + ca - e.pos.y) * 0.6f;
                        tA.set(-to.z, 0, to.x).nor();
                        want.mulAdd(tA, ty.equals("zig") ? MathUtils.cos(e.t * 3.2f) * sp * 1.1f : MathUtils.cos(e.t * 1.7f) * 3f);
                        if (hd < ("obj".equals(e.tgt) ? 60f : 85f)) e.phase = "dive";
                    } else want.set(to).nor().scl(sp * 1.4f);
                    e.vel.lerp(want, 1 - (float) Math.exp(-dt * 1.8f));
                    e.g.setEuler(e.vel.z * 0.02f, e.t * 0.3f, -e.vel.x * 0.02f);
                    break;
                }
                case "wing": case "kami": {
                    float dd = ty.equals("kami") ? 200f : 130f;
                    if (!e.phase.equals("dive")) {
                        want.set(to.x, 0, to.z).nor().scl(sp * (ty.equals("kami") ? 1.25f : 1f));
                        want.y = (e.alt - (e.pos.y - height(e.pos.x, e.pos.z))) * 0.4f;
                        if (hd < dd) { e.phase = "dive"; if (ty.equals("kami")) G.sfx("alarm", 0.5f, 900); }
                    } else want.set(to).nor().scl(sp * (ty.equals("kami") ? 2f : 1.5f));
                    e.vel.lerp(want, 1 - (float) Math.exp(-dt * (ty.equals("kami") ? 2.2f : 1.2f)));
                    faceDir = e.vel;
                    break;
                }
                case "heavy": {
                    if (!e.phase.equals("dive")) {
                        want.set(to.x, 0, to.z).nor().scl(sp); want.y = (height(e.pos.x, e.pos.z) + 60 - e.pos.y) * 0.5f;
                        if (hd < 120) e.phase = "dive";
                    } else want.set(to).nor().scl(sp * 1.5f);
                    e.fire -= dt;
                    if (e.fire <= 0 && hd < 330 && hd > 140) { e.fire = rr(3.5f, 5); enemyShot(e, "erk", 55); }
                    e.vel.lerp(want, 1 - (float) Math.exp(-dt));
                    e.g.setEuler(e.vel.z * 0.03f, e.t * 0.2f, -e.vel.x * 0.03f);
                    break;
                }
                case "relay": {
                    tA.set(to.x, 0, to.z).nor().scl(toSpawn > 0 ? -200f : -120f).add(Tt);
                    want.set(tA.x - e.pos.x, 0, tA.z - e.pos.z).limit(Math.max(sp, 18f));
                    want.y = (height(e.pos.x, e.pos.z) + 75 - e.pos.y) * 0.5f;
                    e.vel.lerp(want, 1 - (float) Math.exp(-dt));
                    e.heal -= dt;
                    if (e.heal <= 0) {
                        e.heal = 1.5f;
                        for (E o : enemies)
                            if (o != e && !o.boss && o.hp < o.max && o.pos.dst(e.pos) < 70) {
                                o.hp = Math.min(o.max, o.hp + 1); FX.glow(o.pos, 3f, new Color(0.3f, 1, 0.5f, 1), 0.3f);
                            }
                    }
                    e.g.rotateY(dt * 0.5f);
                    break;
                }
                case "scout": {
                    e.ang += e.dir * dt * (sp / e.distR);
                    float tx = -MathUtils.sin(e.ang) * e.distR, tz = -MathUtils.cos(e.ang) * e.distR;
                    want.set(tx - e.pos.x, (height(tx, tz) + 100 - e.pos.y) * 0.5f, tz - e.pos.z).limit(sp * 1.4f);
                    e.vel.lerp(want, 1 - (float) Math.exp(-dt * 2));
                    faceDir = e.vel;
                    e.data += dt / 22f;
                    if (e.data >= 1 && state.equals("run")) missionFail("Разведчик передал координаты");
                    break;
                }
                case "erk": {
                    tA.set(e.vel).nor(); tC.set(to).nor();
                    float ang = angleBetween(tA, tC), mt = 2.2f * dt;
                    if (ang > 0.0001f) slerp(tA, tC, Math.min(1f, mt / ang));
                    float nsp = Math.min(sp, e.vel.len() + 90 * dt);
                    e.vel.set(tA).scl(nsp);
                    faceDir = e.vel;
                    break;
                }
                case "shell": {
                    e.vel.y -= 18 * dt;
                    if (e.pos.y < height(e.pos.x, e.pos.z) + 0.5f) {
                        explosion(e.pos, 0.9f);
                        if (e.pos.dst(aimTarget(e)) < 9) hitTarget(e.tgt, e.dmg);
                        removeEnemy(e); continue;
                    }
                    break;
                }
                case "node": {
                    E c = e.core;
                    if (c == null || c.hp <= 0) { removeEnemy(e); continue; }
                    e.oa += dt * 0.8f;
                    e.pos.set(c.pos).add(MathUtils.cos(e.oa) * 22, MathUtils.sin(e.oa * 2) * 4, MathUtils.sin(e.oa) * 22);
                    e.vel.set(c.vel);
                    e.g.rotateY(dt * 2);
                    e.g.pos.set(e.pos);
                    if (R.nextFloat() < 0.3f) FX.glow(tA.set(e.pos).lerp(c.pos, R.nextFloat()), 1.6f, new Color(0.5f, 0.75f, 1, 1), 0.1f);
                    continue;
                }
                case "inf": case "rpg": case "mech": case "mortar": {
                    int groundN = 0, allN = 0;
                    for (E o : enemies) { if (o.ground) groundN++; if (!(o.type.equals("erk") || o.type.equals("shell"))) allN++; }
                    boolean lastOnes = toSpawn <= 0 && groundN == allN;
                    float rng = lastOnes ? 120f : (ty.equals("inf") ? 130f : ty.equals("rpg") ? 220f : ty.equals("mech") ? 110f : 300f);
                    if (hd > rng) { want.set(to.x, 0, to.z).nor().scl(sp); e.anim += dt * sp * 0.9f; }
                    e.vel.lerp(want, 1 - (float) Math.exp(-dt * 3));
                    e.pos.y = height(e.pos.x, e.pos.z);
                    faceDir = tA.set(to.x, 0, to.z);
                    e.fire -= dt;
                    if (hd <= rng + 5 && e.fire <= 0) {
                        if (ty.equals("inf")) {
                            e.fire = rr(4, 6);
                            final E fe = e;
                            for (int k = 0; k < 3; k++) later(k * 0.11f, () -> { if (fe.hp > 0 && state.equals("run")) enemyTracer(fe); });
                        } else if (ty.equals("rpg")) { e.fire = rr(5, 7); enemyShot(e.pos.cpy().add(0, 8.5f, 0), e.tgt, "erk", 55); }
                        else if (ty.equals("mech")) { e.fire = 3f; enemyShot(e.pos.cpy().add(0, 10, 0), e.tgt, "erk", 70); }
                        else { e.fire = rr(8, 10); enemyShot(e.pos.cpy().add(0, 5, 0), e.tgt, "shell", 0); }
                    }
                    if (e.md.legs.size > 0) {
                        float s = MathUtils.sin(e.anim * 2.2f) * 0.5f;
                        e.md.legs.get(0).setEuler(s, 0, 0); e.md.legs.get(1).setEuler(-s, 0, 0);
                    }
                    break;
                }
                default:
                    if (e.pc) faceDir = pcBoss(e, dt, to);
                    else if (e.boss) faceDir = updateBoss(e, dt, to, hd, slow);
            }
            if (!e.ground && !ty.equals("walker")) {
                e.pos.mulAdd(e.vel, dt);
                float gh = height(e.pos.x, e.pos.z) + (ty.equals("erk") ? 0.5f : 3f);
                if (e.pos.y < gh && dist > 14 && !ty.equals("shell")) { e.pos.y = gh; e.vel.y = Math.abs(e.vel.y) * 0.5f; }
            } else e.pos.mulAdd(e.vel, dt);
            e.g.pos.set(e.pos);
            if (faceDir != null) face(e, faceDir);
            if (e.md.rot != null) e.md.rot.rotateY(dt * 0.5f);
            if (!e.boss && !e.ground && !ty.equals("shell")) {
                if (dist < (("obj".equals(e.tgt) && obj != null) ? obj.r : 4.5f)) {
                    hitTarget(e.tgt, e.dmg); explosion(e.pos, ty.equals("erk") ? 0.8f : 1.3f); removeEnemy(e); continue;
                }
            }
            if (e.pos.len2() > 2500f * 2500f) removeEnemy(e);
        }
    }

    static float angleBetween(Vector3 a, Vector3 b) {
        float d = MathUtils.clamp(a.dot(b) / Math.max(1e-6f, a.len() * b.len()), -1, 1);
        return (float) Math.acos(d);
    }

    /** a = slerp(a, b, t) для единичных векторов */
    static void slerp(Vector3 a, Vector3 b, float t) {
        float d = MathUtils.clamp(a.dot(b), -1, 1);
        float th = (float) Math.acos(d) * t;
        Vector3 rel = new Vector3(b).mulAdd(a, -d);
        if (rel.len2() < 1e-10f) return;
        rel.nor();
        a.scl(MathUtils.cos(th)).mulAdd(rel, MathUtils.sin(th)).nor();
    }

    Vector3 orbit(E e, Vector3 to, float hd, float sp, float gh, float R0, float alt, float k) {
        Vector3 w = new Vector3();
        if (hd > R0 + 40) w.set(to.x, 0, to.z).nor().scl(sp);
        else {
            w.set(-to.z, 0, to.x).nor().scl(sp * k * e.side);
            w.mulAdd(tA.set(to.x, 0, to.z).nor(), (hd - R0) * 0.05f);
        }
        w.y = (gh + alt - e.pos.y) * 0.4f;
        return w;
    }

    Vector3 updateBoss(final E e, float dt, Vector3 to, float hd, float slow) {
        float sp = e.sp * slow, gh = height(e.pos.x, e.pos.z);
        e.fire -= dt; e.spawnT -= dt;
        Vector3 want;
        boolean live = empT <= 0;
        switch (e.type) {
            case "boss":
                want = orbit(e, to, hd, sp, gh, 220f, 90f, 1f); e.vel.lerp(want, 1 - (float) Math.exp(-dt * 0.8f));
                if (e.fire <= 0 && hd < 450) {
                    e.fire = e.hp < e.max * 0.4f ? 2.6f : 4f;
                    for (int k = 0; k < 3; k++) later(k * 0.25f, () -> { if (e.hp > 0 && enemies.contains(e, true)) enemyShot(e, "erk", 60); });
                }
                if (e.spawnT <= 0 && live) {
                    e.spawnT = 8f;
                    for (int k = 0; k < 3; k++) { E q = spawnEnemy("quad", e.pos.cpy().add(rr(-12, 12), -5, rr(-8, 8))); q.vel.set(rr(-8, 8), -4, rr(-8, 8)); }
                }
                e.g.rotateY(dt * 0.2f);
                return null;
            case "gunship":
                want = orbit(e, to, hd, sp, gh, 190f, 60f, 1.6f); e.vel.lerp(want, 1 - (float) Math.exp(-dt * 1.2f));
                if (e.fire <= 0 && hd < 400) {
                    e.fire = 5f;
                    for (int k = 0; k < 4; k++) later(k * 0.22f, () -> { if (e.hp > 0 && enemies.contains(e, true)) enemyShot(e, "erk", 65); });
                }
                return new Vector3(to.x, 0, to.z);
            case "walker": {
                want = new Vector3();
                if (hd > 170) { want.set(to.x, 0, to.z).nor().scl(sp); e.anim += dt * 1.6f; }
                e.vel.lerp(want, 1 - (float) Math.exp(-dt * 2)); e.vel.y = 0;
                if (e.md.legs.size > 0) {
                    float s = MathUtils.sin(e.anim) * 0.45f; e.md.legs.get(0).setEuler(s, 0, 0); e.md.legs.get(1).setEuler(-s, 0, 0);
                }
                if (Math.abs(MathUtils.sin(e.anim)) > 0.98f && hd > 170) shake = Math.max(shake, 0.25f);
                if (e.fire <= 0 && hd < 420) {
                    e.fire = e.hp < e.max * 0.4f ? 2.2f : 3.2f;
                    e.g.updateGlobal(root.global);
                    Vector3 from = e.g.toGlobal(tA.set(0, 2.7f, -7.0f), new Vector3());
                    enemyShot(from, e.tgt, "erk", 75);
                }
                e.pos.y = gh + 8.4f * e.g.scl.y;
                return new Vector3(to.x, 0, to.z);
            }
            case "carrier":
                want = orbit(e, to, hd, sp, gh, 380f, 170f, 0.5f); e.vel.lerp(want, 1 - (float) Math.exp(-dt * 0.4f));
                if (e.spawnT <= 0 && live) {
                    e.spawnT = 5.5f;
                    Vector3 base = e.pos.cpy().add(0, -12, 0);
                    for (int k = 0; k < 5; k++) { E m = spawnEnemy("mini", base.cpy().add(rr(-6, 6), 0, rr(-10, 10))); m.vel.set(rr(-6, 6), -8, rr(-6, 6)); }
                    if (ui != null && R.nextFloat() < 0.4f) ui.comm("lisa", "«Улей» выпустил рой!");
                }
                if (e.fire <= 0 && hd < 500) { e.fire = 6f; enemyShot(e, "erk", 55); }
                return new Vector3(e.vel.x, 0, e.vel.z);
            case "ghost": {
                if (!e.hasRunV) { e.runV.set(-e.pos.x, 0, -e.pos.z).nor(); e.hasRunV = true; }
                want = new Vector3(e.runV).scl(sp * 1.4f); want.y = (gh + 120 - e.pos.y) * 0.4f;
                if (e.pos.dot(e.runV) > 520) e.runV.set(-e.pos.x + rr(-80, 80), 0, -e.pos.z + rr(-80, 80)).nor();
                e.vel.lerp(want, 1 - (float) Math.exp(-dt * 0.6f));
                if (hd < 60 && e.fire <= 0) { e.fire = 0.9f; enemyShot(e, "shell", 0); }
                e.cloakT -= dt;
                if (e.cloakT <= 0) {
                    e.cloak = e.cloak != 0 ? 0 : 1; e.cloakT = e.cloak != 0 ? 3f : 7f; e.inv = e.cloak != 0;
                    setBodyAlpha(e, e.cloak != 0 ? 0.12f : 1f);
                }
                return new Vector3(e.vel.x, 0, e.vel.z);
            }
            case "core":
                want = orbit(e, to, hd, sp, gh, 260f, 120f, 0.5f); e.vel.lerp(want, 1 - (float) Math.exp(-dt * 0.5f));
                e.g.rotateY(dt * 0.6f);
                if (e.fire <= 0 && hd < 480) {
                    e.fire = e.inv ? 3.2f : 2.2f;
                    for (int k = 0; k < 2; k++) later(k * 0.3f, () -> { if (e.hp > 0 && enemies.contains(e, true)) enemyShot(e, "erk", 60); });
                }
                if (e.nodes.size == 0 && !e.inv) {
                    e.reT -= dt;
                    if (e.reT <= 0) { e.reT = 22f; spawnNodes(e, 4); if (ui != null) ui.toast("Ядро восстановило щит!"); }
                }
                return null;
            case "hydra":
                want = orbit(e, to, hd, sp, gh, 230f, 110f, 1.1f); e.vel.lerp(want, 1 - (float) Math.exp(-dt * 0.6f));
                if (e.fire <= 0 && hd < 450) { e.fire = e.hp < e.max * 0.3f ? 1.5f : 2.4f; for (int k = 0; k < 3; k++) enemyShot(e, "erk", 60); }
                if (e.hp < e.max * (0.66f - e.split * 0.33f) && e.split < 2 && live) {
                    e.split += 1;
                    if (ui != null) ui.toast("«Гидра» сбрасывает рой!");
                    for (int k = 0; k < 3; k++) spawnEnemy("heavy", e.pos.cpy().add(rr(-20, 20), -6, rr(-10, 10)));
                    for (int k = 0; k < 2; k++) spawnEnemy("kami", e.pos.cpy().add(rr(-10, 10), 0, 0));
                }
                return new Vector3(e.vel.x, 0, e.vel.z);
            case "storm":
                want = orbit(e, to, hd, sp, gh, 280f, 150f, 0.6f); e.vel.lerp(want, 1 - (float) Math.exp(-dt * 0.5f));
                e.g.rotateY(dt * 0.3f);
                if (e.fire <= 0 && hd < 520) { e.fire = e.hp < e.max * 0.4f ? 3f : 4.5f; lightning(e.pos.cpy().add(0, -6, 0), e); }
                if (e.spawnT <= 0 && live) {
                    e.spawnT = 9f;
                    for (int k = 0; k < 3; k++) spawnEnemy("zig", e.pos.cpy().add(rr(-15, 15), -8, rr(-15, 15)));
                }
                return null;
            case "manta": {
                if (e.runI < 0) { e.runI = 0; e.rt = 0; }
                e.rt += dt;
                if (e.runI == 0) {
                    want = orbit(e, to, hd, sp, gh, 420f, 140f, 1.4f);
                    if (e.rt > 7f) { e.runI = 1; e.rt = 0; if (ui != null && R.nextFloat() < 0.6f) ui.comm("lisa", "«Скат» заходит в пике!"); }
                } else {
                    Vector3 d2 = new Vector3(to.x, (gh + 70 - e.pos.y) * 0.02f, to.z).nor();
                    if (e.runI == 1) {
                        want = d2.scl(sp * 3.2f);
                        if (hd < 70) { e.runI = 2; e.rt = 0; }
                        if (hd < 160 && e.fire <= 0) { e.fire = 0.35f; enemyShot(e, "shell", 0); }
                    } else {
                        want = new Vector3(e.vel).nor().scl(sp * 3f); want.y = 25;
                        if (e.rt > 4f) { e.runI = 0; e.rt = 0; }
                    }
                }
                e.vel.lerp(want, 1 - (float) Math.exp(-dt * 1.5f));
                return new Vector3(e.vel.x, 0, e.vel.z);
            }
            case "colossus": {
                want = new Vector3();
                if (hd > 210) want.set(to.x, 0, to.z).nor().scl(sp);
                e.vel.lerp(want, 1 - (float) Math.exp(-dt * 2)); e.vel.y = 0;
                e.pos.y = gh;
                e.shT -= dt;
                if (e.shT <= 0) {
                    e.inv = !e.inv; e.shT = e.inv ? 4f : 6f;
                    shieldVis(e, e.inv);
                    if (!e.inv && ui != null && R.nextFloat() < 0.5f) ui.comm("lisa", "Купол «Колосса» опущен — огонь!");
                }
                if (e.fire <= 0 && hd < 480) {
                    e.fire = e.hp < e.max * 0.4f ? 2.6f : 3.6f;
                    enemyShot(e.pos.cpy().add(0, 11, 0), e.tgt, "shell", 0);
                    enemyShot(e.pos.cpy().add(0, 11, 0), e.tgt, "erk", 70);
                }
                if (e.spawnT <= 0 && live) {
                    e.spawnT = 12f;
                    String[] ts = {"inf", "rpg", "mech"};
                    for (int k = 0; k < 3; k++) spawnEnemy(ts[k], new Vector3(e.pos.x + rr(-20, 20), 0, e.pos.z + rr(-20, 20)));
                }
                return new Vector3(to.x, 0, to.z);
            }
            case "chimera":
                want = orbit(e, to, hd, sp, gh, 260f, 130f, 0.7f); e.vel.lerp(want, 1 - (float) Math.exp(-dt * 0.5f));
                if (e.fire <= 0 && hd < 480) {
                    e.fire = e.hp < e.max * 0.3f ? 1.4f : 2.2f;
                    for (int k = 0; k < 3; k++) enemyShot(e, "erk", 62);
                    if (R.nextFloat() < 0.4f) enemyShot(e, "shell", 0);
                }
                if (e.spawnT <= 0 && live) {
                    e.spawnT = 6.5f;
                    for (int k = 0; k < 2; k++) spawnEnemy("kami", e.pos.cpy().add(rr(-10, 10), -4, 0));
                    spawnEnemy("split", e.pos.cpy().add(0, -8, 0));
                }
                if (e.nodes.size == 0 && !e.inv) { e.reT -= dt; if (e.reT <= 0) { e.reT = 25f; spawnNodes(e, 5); } }
                return new Vector3(e.vel.x, 0, e.vel.z);
        }
        return null;
    }

    void enemyTracer(E e) {
        Vector3 from = e.pos.cpy().add(0, 6.9f, 0);
        Ebul b = new Ebul();
        b.pos.set(from); b.vel.set(tgtPos(e.tgt)).sub(from).nor().scl(160f);
        b.vel.x += rr(-9, 9); b.vel.y += rr(-4, 4);
        b.life = 2f; b.dmg = 1f * difDmg; b.tgt = e.tgt;
        ebul.add(b);
        G.sfx("mg", 0.4f, 70);
    }

    void updateEbul(float dt) {
        for (int i = ebul.size - 1; i >= 0; i--) {
            Ebul b = ebul.get(i);
            b.pos.mulAdd(b.vel, dt); b.life -= dt;
            if (b.pos.dst(tgtPos(b.tgt)) < 4) { hitTarget(b.tgt, b.dmg); FX.sparks(b.pos, 4); ebul.removeIndex(i); }
            else if (b.life <= 0) ebul.removeIndex(i);
        }
    }

    void hitTarget(String tgt, float dmg) {
        if (!state.equals("run")) return;
        if ("obj".equals(tgt) && obj != null && obj.hp > 0) { damageObj(dmg); return; }
        if (smokeT > 0) dmg /= 3f;
        hp -= dmg; hurtT = 0.5f; shake = Math.max(shake, Math.min(1f, 0.3f + dmg / 20f)); G.hap(45); G.sfx("hit", 0.8f, 80); waveHit = true;
        FX.sparks(tmpV.set(rr(-1.4f, 1.4f), rr(1.6f, 3.6f), rr(-4, 4)), 10);
        if (hp <= 0) {
            hp = 0; state = "dying"; deadT = 3.2f;
            explosion(AIM.cpy().add(0, 1, 0), 3); breakVehicle();
            later(0.3f, () -> explosion(new Vector3(1, 2, -3), 2.2f));
            later(0.7f, () -> explosion(new Vector3(-1, 1.5f, 2.5f), 1.8f));
        }
    }

    void breakVehicle() {
        if (vm.broken) return;
        vm.broken = true; vm.wreck.clear();
        vm.root.forEach(n -> {
            if (n.mat == null) return;
            Mat m = n.mat.cpy();
            if (m.hull) m.burnt = 0.85f;
            else { m.color.mul(0.18f, 0.18f, 0.18f, 1); m.emission = 0; m.metal = 0; m.rough = 1; }
            n.mat = m;
        });
        fly(vm.turret, new Vector3(rr(-5, 5), rr(15, 20), rr(-5, 5)), new Vector3(rr(-2.5f, 2.5f), rr(-4, 4), rr(-2.5f, 2.5f)));
        fly(vm.radar, new Vector3(rr(-10, 10), rr(18, 24), rr(-10, 10)), new Vector3(rr(-6, 6), rr(-6, 6), rr(-6, 6)));
        for (Node3 m : vm.noseMeshes) m.visible = false;
        Node3 a1 = new Node3(); a1.pos.set(0, 2.4f, 0.5f); vm.root.add(a1); FX.burner(a1, true, 1.6f);
        Node3 a2 = new Node3(); a2.pos.set(0, 3.5f, 0); vm.root.add(a2); FX.burner(a2, false, 2.5f);
        burnT = 40f;
    }

    void fly(Node3 o, Vector3 v, Vector3 av) {
        root.updateGlobal(null);
        Matrix4 gm = new Matrix4(o.global);
        o.remove();
        root.add(o);
        gm.getTranslation(o.pos);
        o.rot.set(gm); o.rot.val[Matrix4.M03] = 0; o.rot.val[Matrix4.M13] = 0; o.rot.val[Matrix4.M23] = 0;
        vm.wreck.add(new Object[]{o, v, av, false});
    }

    void updateWreck(float dt) {
        if (vm == null || !vm.broken) return;
        for (Object[] w : vm.wreck) {
            if ((Boolean) w[3]) continue;
            Node3 o = (Node3) w[0]; Vector3 v = (Vector3) w[1], av = (Vector3) w[2];
            v.y -= 22 * dt;
            o.pos.mulAdd(v, dt);
            o.rotateLocal(Vector3.X, av.x * dt); o.rotateLocal(Vector3.Y, av.y * dt); o.rotateLocal(Vector3.Z, av.z * dt);
            float gy = Math.max(0.3f, height(o.pos.x, o.pos.z) + 0.4f);
            if (o.pos.y < gy) {
                o.pos.y = gy; v.scl(0.35f); v.y = Math.abs(v.y) * 0.4f; av.scl(0.4f);
                FX.explosion(o.pos, 0.5f, false);
                if (v.len() < 1.5f) { w[3] = true; FX.burner(o, true, 0.8f); }
            }
            if (R.nextFloat() < 0.5f) FX.puff(o.pos, 1f, new Color(0.1f, 0.1f, 0.1f, 1), 0.5f, 1.6f, 1f);
        }
    }

    Vector3 barrelWorld(int i, Vector3 out) { return vm.pitch.toGlobal(vm.barrels.get(i), out); }

    void fireBullet() {
        barrelI = (barrelI + 1) % vm.barrels.size;
        Vector3 from = barrelWorld(barrelI, new Vector3());
        Vector3 tgt = tA.set(aimPoint);
        if (st("track") > 0 && lock.t != null && lock.k > 0.3f) tgt.lerp(leadPoint(lock.t, tB), st("track") * 0.8f);
        Vector3 v = tC.set(tgt).sub(from).nor();
        float s = st("spread");
        v.add(rr(-1, 1) * s, rr(-1, 1) * s, rr(-1, 1) * s).nor().scl(BUL_SP);
        Bullet b = new Bullet();
        b.pos.set(from); b.prev.set(from); b.o.set(from); b.vel.set(v); b.life = 2.2f; b.dmg = st("dmg"); b.flak = st("flak") > 0;
        bullets.add(b);
        FX.glow(tmpV.set(from).mulAdd(v, 0.005f), VEH.equals("grom") ? 4.2f : 2.8f, new Color(1, 0.85f, 0.5f, 1), 0.06f);
        shotJuice(from, v);
        if (VEH.equals("grom")) {
            G.sfx("flak", 0.8f, 60); shake = Math.max(shake, 0.12f);
            FX.puff(tmpV.set(from).mulAdd(v, 0.01f), 1.2f, new Color(0.5f, 0.5f, 0.48f, 1), 0.4f, 0.8f, 1f);
        } else G.sfx(VEH.equals("shilka") ? "shot2" : "shot", 0.7f, 38);
    }

    final Vector3 jA = new Vector3(), jB = new Vector3();

    /** Отдача, вспышка-свет, гильзы, у «Грома» — тряска земли и пыль. */
    void shotJuice(Vector3 from, Vector3 vel) {
        boolean grom = VEH.equals("grom"), shilka = VEH.equals("shilka");
        recoil = Math.min(1f, recoil + (grom ? 0.8f : (shilka ? 0.16f : 0.3f)));
        FX.light(from, grom ? 26f : 15f, 1f, 0.72f, 0.38f, grom ? 2.6f : 1.3f, 0.07f);
        if (!G.isMobile() || R.nextFloat() < 0.6f) {
            float side = vm.barrels.get(barrelI).x >= 0 ? 1f : -1f;
            jA.set(MathUtils.cos(aimYaw) * side, 0, -MathUtils.sin(aimYaw) * side);
            jB.set(vel).nor();
            Vector3 breech = tmpV.set(from).mulAdd(jB, grom ? -2.6f : -1.7f);
            FX.casing(breech, jA, grom);
        }
        if (grom) {
            quake = Math.min(1f, quake + 0.55f);
            shake = Math.max(shake, 0.2f);
            G.hap(20);
            int n = G.isMobile() ? 2 : 3;
            for (int i = 0; i < n; i++) {
                float a = R.nextFloat() * MathUtils.PI2, r = rr(3.5f, 7f);
                float x = MathUtils.sin(a) * r, z = MathUtils.cos(a) * r;
                FX.puff(tmpV.set(x, height(x, z) + 0.4f, z), 1.6f, new Color(0.46f, 0.39f, 0.3f, 1), 0.45f, 1.8f, 1.2f);
            }
            if (R.nextFloat() < 0.5f) FX.groundDust(tmpV.set(0, height(0, 0), 0), 0.5f);
        }
    }

    /** Раскалённые стволы и пар при перегреве. */
    void heatJuice(float dt) {
        if (vm == null || vm.broken || vm.barrels.size == 0 || st("gun") <= 0 || VEH.equals("tor")) return;
        if (heat < 0.25f) { steamT = 0; return; }
        float hk = MathUtils.clamp((heat - 0.25f) / 0.75f, 0, 1);
        float[] g = vm.pitch.global.val;
        jB.set(g[Matrix4.M02], g[Matrix4.M12], g[Matrix4.M22]).nor(); // назад вдоль ствола
        float gl = VEH.equals("grom") ? 1.6f : 1f;
        for (int i = 0; i < vm.barrels.size; i++) {
            barrelWorld(i, jA);
            for (int j = 0; j < 3; j++) {
                tmpV.set(jA).mulAdd(jB, (0.3f + j * 0.55f) * gl);
                float a = hk * (0.95f - 0.25f * j) * (0.85f + 0.15f * MathUtils.sin(T * 30f + i));
                FX.frameGlow(tmpV, (0.42f + 0.1f * j) * gl, 0.9f, 0.12f + 0.25f * hk * hk, 0.02f, a * 0.55f);
            }
        }
        steamT -= dt;
        if (steamT <= 0 && (hk > 0.7f || overheated)) {
            steamT = overheated ? 0.09f : 0.22f;
            barrelWorld(R.nextInt(vm.barrels.size), jA);
            tmpV.set(jA).mulAdd(jB, rr(0.2f, 1.4f) * gl).add(0, 0.25f, 0);
            FX.puff(tmpV, 0.4f * gl, new Color(0.86f, 0.87f, 0.9f, 1), overheated ? 0.35f : 0.2f, 1.2f, 2.8f);
        }
    }

    static boolean segHit(Vector3 a, Vector3 b, Vector3 c, float r) {
        float abx = b.x - a.x, aby = b.y - a.y, abz = b.z - a.z;
        float l2 = Math.max(abx * abx + aby * aby + abz * abz, 1e-6f);
        float t = MathUtils.clamp(((c.x - a.x) * abx + (c.y - a.y) * aby + (c.z - a.z) * abz) / l2, 0, 1);
        float dx = a.x + abx * t - c.x, dy = a.y + aby * t - c.y, dz = a.z + abz * t - c.z;
        return dx * dx + dy * dy + dz * dz < r * r;
    }

    final Matrix4 tm = new Matrix4();

    void updateBullets(float dt) {
        float windx = world.wind * 0.04f;
        tracers.clear();
        int n = 0;
        for (int i = bullets.size - 1; i >= 0; i--) {
            Bullet b = bullets.get(i);
            b.prev.set(b.pos);
            b.vel.y -= 3.5f * dt; b.vel.x += windx * dt;
            b.pos.mulAdd(b.vel, dt);
            b.life -= dt;
            boolean dead = b.life <= 0;
            if (b.flak && !dead) {
                boolean boom = b.life < 0.12f;
                if (!boom) for (E e : enemies) {
                    if (e.type.equals("shell") || hidden(e)) continue;
                    if (segHit(b.prev, b.pos, visPos(e, tmpV), e.r + st("fprox"))) { boom = true; break; }
                }
                if (boom) { flakBurst(b.pos); bullets.removeIndex(i); continue; }
            }
            if (!dead) {
                for (int k = 0; k < enemies.size; k++) {
                    E e = enemies.get(k);
                    Vector3 c = tmpV.set(e.pos);
                    if (e.ground) c.y += e.r * 0.8f;
                    if (segHit(b.prev, b.pos, c, e.r)) { damageEnemy(e, b.dmg, b.pos, b.mog ? "mog" : "gun"); dead = true; break; }
                }
            }
            if (!dead && b.pos.y < height(b.pos.x, b.pos.z)) {
                dead = true;
                FX.puff(tmpV.set(b.pos).add(0, 0.5f, 0), 0.7f, new Color(0.45f, 0.38f, 0.28f, 1), 0.5f, 0.9f, 2f);
                for (E e : new Array<>(enemies)) if (e.ground && e.pos.dst(b.pos) < 3) damageEnemy(e, b.dmg * 0.5f, null, "gun");
            }
            if (dead) { bullets.removeIndex(i); continue; }
            if (n < 400) {
                float L = BUL_SP * 0.045f;
                float trav = b.pos.dst(b.o);
                float len = Math.min(L, trav);
                if (len > 0.5f) {
                    tA.set(b.vel).nor();
                    tB.set(b.pos).mulAdd(tA, -len * 0.5f);
                    Node3.basisLook(tm, tA, Math.abs(tA.y) < 0.98f ? Vector3.Y : Vector3.X);
                    tm.val[Matrix4.M03] = tB.x; tm.val[Matrix4.M13] = tB.y; tm.val[Matrix4.M23] = tB.z;
                    tm.scale(0.13f, 0.13f, len);
                    Color c = b.mog ? MOGC : trc;
                    tracers.prim(Dyn.BOX, tm, c.r, c.g, c.b, 1);
                    n++;
                }
            }
        }
    }

    static final Color MOGC = new Color(1, 0.7f, 0.45f, 1);

    void lightning(Vector3 from, E src) {
        Vector3 to = AIM.cpy().add(rr(-2, 2), 0, rr(-2, 2));
        bolt(from, to);
        push("bolt", from.x, from.y, from.z, to.x, to.y, to.z);
        hitTarget(src.tgt, 7f * difDmg);
        if (st("gun") > 0 || st("beam") > 0) heat = Math.min(1f, heat + 0.35f);
        G.sfx("emp", 0.7f, 200); shake = Math.max(shake, 0.4f);
    }

    void bolt(Vector3 a, Vector3 b) {
        bolts.clear();
        Vector3 prev = a.cpy();
        for (int i = 1; i <= 7; i++) {
            float k = i / 7f;
            Vector3 p = i == 7 ? b.cpy() : a.cpy().lerp(b, k).add(new Vector3(rr(-6, 6), rr(-4, 4), rr(-6, 6)).scl(1f - Math.abs(k - 0.5f) * 1.2f));
            bolts.add(new float[]{prev.x, prev.y, prev.z, p.x, p.y, p.z});
            prev = p;
        }
        boltT = 0.18f;
        FX.glow(b, 6f, new Color(0.6f, 0.85f, 1, 1), 0.2f); FX.sparks(b, 14, new Color(0.7f, 0.9f, 1, 1));
    }

    static Mesh3 domeMesh;

    void shieldVis(E e, boolean on) {
        if (e.dome == null) {
            if (domeMesh == null) { MB b = new MB(); b.add(MB.pSph(9, 12, 16, 8), Color.WHITE, new Matrix4()); domeMesh = b.mesh(); }
            Node3 m = new Node3(domeMesh, SHIELD_MAT);
            m.pos.set(0, 3.5f, 0);
            m.scl.set(1 / e.g.scl.x, 1 / e.g.scl.y, 1 / e.g.scl.z);
            e.g.add(m); e.dome = m;
        }
        e.dome.visible = on;
    }

    void beamPlace(int i, Vector3 a, Vector3 b, float w) {
        while (beams.size <= i) beams.add(new float[7]);
        float[] q = beams.get(i);
        q[0] = a.x; q[1] = a.y; q[2] = a.z; q[3] = b.x; q[4] = b.y; q[5] = b.z; q[6] = w;
    }

    int beamUsed;

    void updateBeam(float dt) {
        beamUsed = 0;
        boolean on = firing && st("beam") > 0 && !overheated && state.equals("run");
        if (!on) return;
        heat += st("lheat") * dt;
        if (heat >= 1) { heat = 1; overheated = true; G.sfx("alarm", 0.6f, 900); return; }
        Vector3 from = barrelWorld(0, new Vector3());
        Vector3 dir = new Vector3(aimPoint).sub(from).nor();
        E best = null; float bt = 1600f;
        for (E e : enemies) {
            if (e.cloak != 0 || e.type.equals("shell")) continue;
            Vector3 c = visPos(e, tmpV);
            float t = tA.set(c).sub(from).dot(dir);
            if (t < 0 || t > bt) continue;
            if (tB.set(from).mulAdd(dir, t).dst(c) < e.r * 0.9f + 0.8f) { best = e; bt = t; }
        }
        Vector3 end = best != null ? new Vector3(from).mulAdd(dir, bt) : new Vector3(aimPoint);
        beamPlace(0, from, end, 1f);
        FX.glow(tA.set(from).mulAdd(dir, 0.4f), 1.6f, new Color(0.5f, 0.95f, 1, 1), 0.05f);
        G.sfx("laser", 0.4f, 95);
        int used = 1;
        if (best != null) {
            FX.glow(end, 2.4f, new Color(0.6f, 0.95f, 1, 1), 0.06f);
            damageEnemy(best, st("ldps") * dt, R.nextFloat() < 0.25f ? end : null, "beam");
            E cur = best; Array<E> hit = new Array<>(); hit.add(best);
            for (int k = 0; k < (int) st("chain"); k++) {
                E nb = null; float nd = 75f;
                for (E e : enemies) {
                    if (hit.contains(e, true) || hidden(e) || e.type.equals("shell") || e.type.equals("node")) continue;
                    float d = visPos(e, tA).dst(visPos(cur, tB));
                    if (d < nd) { nd = d; nb = e; }
                }
                if (nb == null) break;
                beamPlace(used, visPos(cur, new Vector3()), visPos(nb, new Vector3()), 0.6f); used++;
                damageEnemy(nb, st("ldps") * dt * 0.6f, null, "beam");
                hit.add(nb); cur = nb;
            }
        } else if (R.nextFloat() < 0.3f && aimPoint.y < height(aimPoint.x, aimPoint.z) + 2f) {
            FX.puff(tA.set(end).add(0, 0.5f, 0), 0.8f, new Color(0.35f, 0.3f, 0.25f, 1), 0.5f, 0.8f, 2f);
        }
        beamUsed = used;
    }

    void fillBeams() {
        beamDyn.clear();
        long ms = System.currentTimeMillis();
        for (int i = 0; i < beamUsed; i++) {
            float[] q = beams.get(i);
            for (int k = 0; k < 2; k++) {
                float ww = q[6] * (1f + 0.18f * MathUtils.sin(ms * 0.06f + k * 2f)) * (k == 0 ? 0.09f : 0.42f);
                cylBetween(beamDyn, q[0], q[1], q[2], q[3], q[4], q[5], ww, k == 0 ? new float[]{0.9f, 1, 1, 0.95f} : new float[]{0.2f, 0.85f, 1, 0.32f});
            }
        }
        if (boltT > 0) for (float[] q : bolts) cylBetween(beamDyn, q[0], q[1], q[2], q[3], q[4], q[5], 0.18f, new float[]{0.75f, 0.92f, 1, 0.95f});
    }

    void cylBetween(Dyn d, float ax, float ay, float az, float bx, float by, float bz, float r, float[] c) {
        tA.set(bx - ax, by - ay, bz - az);
        float L = tA.len();
        if (L < 0.1f) return;
        tA.scl(1f / L);
        // ось Y цилиндра вдоль tA
        Vector3 ref = Math.abs(tA.y) < 0.95f ? Vector3.Y : Vector3.X;
        tB.set(tA).crs(ref).nor(); // x
        tC.set(tB).crs(tA).nor(); // z
        float[] m = tm.idt().val;
        m[Matrix4.M00] = tB.x * r; m[Matrix4.M10] = tB.y * r; m[Matrix4.M20] = tB.z * r;
        m[Matrix4.M01] = tA.x * L; m[Matrix4.M11] = tA.y * L; m[Matrix4.M21] = tA.z * L;
        m[Matrix4.M02] = tC.x * r; m[Matrix4.M12] = tC.y * r; m[Matrix4.M22] = tC.z * r;
        m[Matrix4.M03] = (ax + bx) / 2; m[Matrix4.M13] = (ay + by) / 2; m[Matrix4.M23] = (az + bz) / 2;
        d.prim(Dyn.CYL8, tm, c[0], c[1], c[2], c[3]);
    }

    void flakBurst(Vector3 p) {
        push("boom", p.x, p.y, p.z, 0.7f);
        FX.explosion(p, 0.7f, true);
        FX.puff(p, 3.5f, new Color(0.12f, 0.12f, 0.12f, 1), 0.65f, 2.2f, 0.4f);
        G.sfx("boom", 0.35f, 50);
        hitMark = 0.13f;
        for (E e : new Array<>(enemies)) {
            if (!enemies.contains(e, true)) continue;
            float d = visPos(e, tmpV).dst(p) - e.r * 0.5f;
            if (d < st("frad")) damageEnemy(e, st("fdmg") * (1f - MathUtils.clamp(d / st("frad"), 0, 1) * 0.6f), null, "flak");
        }
    }

    public Vector3 leadPoint(E e, Vector3 out) {
        Vector3 c = visPos(e, new Vector3());
        float t = c.dst(camPos) / BUL_SP;
        out.set(c).mulAdd(e.vel, t);
        t = out.dst(camPos) / BUL_SP;
        out.set(c).mulAdd(e.vel, t); out.y += 0.5f * 3.5f * t * t;
        return out;
    }

    public boolean fireMissile(Object target, boolean free) {
        if (!state.equals("run")) return false;
        if (netView) {
            if (duelRole.equals("pvo") && !VEH.equals("tor") && missiles > 0) netMis++;
            return false;
        }
        if (!free && (missiles <= 0 || st("maxMis") <= 0)) return false;
        if (!free) { missiles -= 1; if (reloadT <= 0) reloadT = st("reload"); }
        int nn = vm.noses.size;
        int idx = nn > 0 ? ((!free && missiles < nn) ? missiles : R.nextInt(Math.max(1, nn))) : 0;
        vm.root.updateGlobal(root.global);
        Vector3 from = nn > 0 ? vm.pitch.toGlobal(vm.noses.get(idx), new Vector3()) : vm.turret.toGlobal(new Vector3(0, 2, 0), new Vector3());
        Node3 node = new Node3();
        node.add(new Node3(Models.missileMesh(), Models.MAT_MISSILE));
        Node3 fl = FX.flame(1.78f); node.add(fl);
        SmokeTrail tr = new SmokeTrail(true);
        root.add(node); node.pos.set(from);
        Vector3 fwd = new Vector3();
        if (nn > 0) { float[] g = vm.pitch.global.val; fwd.set(-g[Matrix4.M02], -g[Matrix4.M12], -g[Matrix4.M22]).nor(); }
        else fwd.set(aimDir);
        node.lookDir(fwd, Math.abs(fwd.y) < 0.98f ? Vector3.Y : Vector3.X);
        FX.launchBlast(from, fwd);
        FX.light(from, 34f, 1f, 0.7f, 0.4f, 3f, 0.3f);
        recoil = Math.min(1f, recoil + 0.35f);
        shake = Math.max(shake, 0.18f);
        E tgt;
        if (target instanceof String) tgt = (lock.t != null && lock.k >= 1) ? lock.t : null;
        else tgt = (E) target;
        if (tgt != null) for (E o : enemies) if (o.type.equals("decoy") && o != tgt && o.pos.dst(tgt.pos) < 55 && R.nextFloat() < 0.5f) { tgt = o; break; }
        Rocket r = new Rocket();
        r.node = node; r.trail = tr; r.flame = fl; r.pos.set(from); r.vel.set(fwd).scl(32).add(0, 3, 0); r.tgt = tgt; r.life = 7f; r.sp = 32f;
        rockets.add(r);
        G.sfx("missile", 0.8f, 50); G.hap(15); statAdd("missiles", 1); qProg("missile", 1, false);
        syncNoses();
        return true;
    }

    void updateRockets(float dt) {
        for (int i = rockets.size - 1; i >= 0; i--) {
            Rocket r = rockets.get(i);
            r.life -= dt; r.age += dt;
            float ms = st("misSpd");
            r.sp = Math.min(200 * ms, r.sp + (r.age < 0.12f ? 60f : 190f) * ms * dt);
            if (r.tgt != null && (r.tgt.hp <= 0 || !enemies.contains(r.tgt, true) || r.tgt.cloak != 0)) r.tgt = null;
            Vector3 dir = tA.set(r.vel).nor();
            if (r.tgt != null) {
                float d = r.tgt.pos.dst(r.pos), lead = d / Math.max(r.sp, 1f);
                Vector3 wd = tB.set(r.tgt.pos).mulAdd(r.tgt.vel, lead).sub(r.pos).nor();
                float ang = angleBetween(dir, wd), mt = 3.4f * dt;
                if (ang > 0.0001f) slerp(dir, wd, Math.min(1f, mt / ang));
            }
            r.vel.set(dir).scl(r.sp);
            Vector3 prev = tC.set(r.pos);
            Vector3 prevC = prev.cpy();
            r.pos.mulAdd(r.vel, dt);
            r.node.pos.set(r.pos);
            r.node.lookDir(dir, Math.abs(dir.y) < 0.98f ? Vector3.Y : Vector3.X);
            r.trail.emit(tmpV.set(r.pos).mulAdd(dir, -1.9f));
            float fk = rr(0.75f, 1.25f);
            r.flame.scl.set(fk * 0.9f + 0.1f, fk * 0.9f + 0.1f, fk * Math.min(1f, r.age * 6f + 0.3f));
            E hit = null;
            for (E e : enemies) if (!e.inv && segHit(prevC, r.pos, e.pos, e.r + 1.5f)) { hit = e; break; }
            if (hit != null || r.life <= 0 || r.pos.y < height(r.pos.x, r.pos.z)) {
                explosion(r.pos, 1.6f);
                float dmg = 14 * st("warhead");
                for (E e : new Array<>(enemies)) {
                    float dd = e.pos.dst(r.pos);
                    if (dd < 14 + e.r) damageEnemy(e, e == hit ? dmg : (float) Math.ceil(dmg * 0.55f * (1 - dd / (14 + e.r))), e.pos.cpy(), "mis");
                }
                r.trail.stop(); r.node.remove(); rockets.removeIndex(i);
            }
        }
    }

    void updateLock(float dt) {
        E best = null;
        float bestA = VEH.equals("tor") ? 0.32f : 0.2f;
        for (E e : enemies) {
            if (hidden(e) || e.type.equals("shell")) continue;
            Vector3 to = visPos(e, tA).sub(camPos);
            float d = to.len();
            if (d > 1200) continue;
            float a = angleBetween(aimDir, to) - (e.boss ? 0.04f : 0f);
            if (a < bestA) { bestA = a; best = e; }
        }
        if (best != lock.t) { lock.t = best; lock.k = 0; lock.beeped = false; }
        if (lock.t != null) {
            lock.k = Math.min(1f, lock.k + dt / (VEH.equals("tor") ? 0.3f : 0.55f));
            if (lock.k >= 1 && !lock.beeped) { lock.beeped = true; if (st("maxMis") > 0) G.sfx("lock", 0.5f); }
        }
    }

    public void setupMogs() {
        for (Models.Mog m : mogs) m.g.remove();
        mogs.clear();
        int lv = (int) st("mog");
        if (lv <= 0) return;
        float[][] spots = {{13f, -9f}, {-15f, -7f}};
        for (int i = 0; i < (lv >= 3 ? 2 : 1); i++) {
            Models.Mog m = Models.mog();
            m.g.pos.set(spots[i][0], height(spots[i][0], spots[i][1]), spots[i][1]);
            float ry = rr(-0.3f, 0.3f);
            m.g.setEuler(0, ry, 0); m.g.tag = ry;
            root.add(m.g);
            m.t = 0; m.tgt = null; m.dmg = lv >= 2 ? 0.9f : 0.6f;
            mogs.add(m);
        }
    }

    void updateMogs(float dt) {
        for (Models.Mog m : mogs) {
            m.t -= dt;
            m.g.updateGlobal(root.global);
            Vector3 gp = m.gun.gpos(new Vector3());
            E t = (E) m.tgt;
            if (t == null || t.hp <= 0 || !enemies.contains(t, true) || t.pos.dst(gp) > 320) {
                m.tgt = null; t = null; float bd = 320f;
                for (E e : enemies) {
                    if (e.cloak != 0 || e.boss) continue;
                    float d = e.pos.dst(gp);
                    if (d < bd) { bd = d; t = e; }
                }
                m.tgt = t;
            }
            if (t == null) continue;
            float tt = t.pos.dst(gp) / BUL_SP;
            Vector3 lp = new Vector3(t.pos).mulAdd(t.vel, tt);
            m.gun.setEuler(0, MathUtils.atan2(-(lp.x - gp.x), -(lp.z - gp.z)) - (Float) m.g.tag, 0);
            if (m.t <= 0) {
                m.t = 1f / 7f;
                Vector3 v = new Vector3(lp).sub(gp).nor().add(rr(-0.012f, 0.012f), rr(-0.012f, 0.012f), 0).nor().scl(BUL_SP);
                Vector3 from = new Vector3(gp).add(0, 0.55f, 0);
                Bullet b = new Bullet();
                b.pos.set(from); b.prev.set(from); b.o.set(from); b.vel.set(v); b.life = 1.6f; b.dmg = m.dmg; b.mog = true;
                bullets.add(b);
                if (R.nextFloat() < 0.5f) G.sfx("mg", 0.3f, 70);
            }
        }
    }

    public void useAbility(String k) {
        if (!state.equals("run") || !Boolean.TRUE.equals(AB().get(k))) return;
        if (netView) {
            if (duelRole.equals("pvo")) { int i = java.util.Arrays.asList("salvo", "emp", "smoke", "repair").indexOf(k); if (i >= 0) netAb[i]++; }
            return;
        }
        if (k.equals("salvo")) {
            if (abUsed.get("salvo")) return;
            abUsed.put("salvo", true);
            List<E> ts = new ArrayList<>();
            for (E e : enemies) if (!hidden(e) && !(e.type.equals("shell") || e.type.equals("node"))) ts.add(e);
            Collections.sort(ts, (a, b) -> Float.compare(a.pos.len2(), b.pos.len2()));
            for (int i = 0; i < 6; i++) {
                final E tg = ts.size() > 0 ? ts.get(i % ts.size()) : null;
                later(i * 0.12f, () -> fireMissile(tg, true));
            }
            if (ui != null) ui.toast("Залп!");
        } else if (k.equals("emp")) {
            if (abUsed.get("emp")) return;
            abUsed.put("emp", true); empT = 5f; G.sfx("emp"); G.hap(40);
            for (int i = 0; i < 8; i++) FX.glow(new Vector3(MathUtils.cos(i * MathUtils.PI / 4) * 30, 4, MathUtils.sin(i * MathUtils.PI / 4) * 30), 30f, new Color(0.5f, 0.8f, 1, 1), 0.8f);
            if (ui != null) ui.toast("Помехи: рой замедлен");
        } else if (k.equals("smoke")) {
            if (abCd.get("smoke") > 0) return;
            abCd.put("smoke", 30f); smokeT = 6f;
            for (int i = 0; i < 18; i++) {
                float a = R.nextFloat() * MathUtils.PI2, r = rr(9, 15);
                FX.puff(new Vector3(MathUtils.sin(a) * r, rr(0.5f, 2), MathUtils.cos(a) * r), 5f, new Color(0.75f, 0.76f, 0.74f, 1), 0.45f, 6f, 0.4f);
            }
        } else if (k.equals("repair")) {
            if (abCd.get("repair") > 0) return;
            abCd.put("repair", 45f); repairT = 4f;
            if (ui != null) ui.toast("Ремонт…");
        }
        G.sfx("click");
    }

    public void clearBattle() {
        for (E e : new Array<>(enemies)) { e.keep = false; removeEnemy(e); }
        for (Wreck w : wrecks) w.g.remove();
        wrecks.clear(); dmgNums.clear(); hitMark = 0; beamUsed = 0;
        for (Rocket r : rockets) { r.node.remove(); r.trail.stop(); }
        rockets.clear(); bullets.clear(); ebul.clear(); bosses.clear(); timers.clear(); bolts.clear();
        lock.t = null; killCamT = 0; killNode = null; recoil = 0; quake = 0; stormLeft = 0;
        if (obj != null) for (Objs.Obj m : obj.models) m.g.remove();
        obj = null;
        tracers.clear();
        FX.clear();
        for (Vector3 s : world.stacks) FX.chimney(s);
    }

    public void startGame(String m) { startGame(m, null); }

    public void startGame(String m, JsonValue def) {
        BlackBox.log("старт боя: " + m + (def != null ? " " + def.getString("id", "") : "") + " · " + G.SV.veh);
        clearBattle();
        mode = m;
        CM = m.equals("campaign") ? def : null;
        VEH = G.SV.veh;
        if (hasMod("missilesOnly") && (VEH.equals("shilka") || VEH.equals("luch") || VEH.equals("grom"))) VEH = "pantsir";
        RUN = null;
        if (m.equals("endless")) {
            RUN = new Run();
            RUN.c = 60;
            RUN.up = new LinkedHashMap<>(G.SV.upv.get(VEH));
            RUN.ab = new LinkedHashMap<>();
            for (String k : new String[]{"salvo", "emp", "smoke", "repair"}) RUN.ab.put(k, Boolean.TRUE.equals(G.SV.ab.get(k)));
        }
        dif = G.D.get("DIFF").get(m.equals("endless") ? G.SV.diff : "normal");
        if (dif == null) dif = G.D.get("DIFF").get("normal");
        difSpd = dif.getFloat("spd"); difDmg = dif.getFloat("dmg"); difHp = dif.getFloat("hp"); difCoin = dif.getFloat("coin");
        nightRun = m.equals("endless") && G.SV.night;
        if (nightRun) difCoin *= 1.25f;
        applyStats();
        setVehicleModel();
        JsonValue tr = G.D.get("SK_TR").get(G.SV.skTr);
        if (tr == null) tr = G.D.get("SK_TR").get("gold");
        trc.set(G.hex(tr.getString("col")));
        if (CM != null) {
            JsonValue wx = CM.get("weather");
            world.build(CM.getInt("theme", 0), CM.getBoolean("dark", false), wx != null ? wx.getFloat("rain", 0) : 0, wx != null ? wx.getFloat("fog", 0) : 0,
                    wx != null ? wx.getFloat("wind", 0) : 0, CM.has("obj") ? CM.get("obj").getString("kind", "") : "", CM.getBoolean("noBeam", false));
        } else {
            world.build(0, nightRun, 0, 0, 0, "", false);
            if (m.equals("endless")) world.enableDynamic();
        }
        for (Vector3 s : world.stacks) FX.chimney(s);
        obj = makeObj();
        setupMogs();
        score = 0; wave = 0; hp = st("maxHp"); heat = 0; overheated = false; kills = 0; combo = 0; runCoins = 0;
        missiles = (int) st("maxMis"); reloadT = 0; chanT = 0; firing = false;
        aimYaw = 0; aimPitch = 0.2f; toSpawn = 0; between = 1.2f; bossWave = false; bossSpawned = false; missionTime = 0;
        missionT = CM != null ? CM.getFloat("time", 0) : 0; timeSaid = 0; squadMix = null; resReason = "";
        abUsed.put("salvo", false); abUsed.put("emp", false); abCd.put("smoke", 0f); abCd.put("repair", 0f);
        smokeT = 0; repairT = 0; empT = 0; scoutQ = 0; scoutT = 0; squads = 0; squadT = 0; nohitWaves = 0; wxRoll = 0; burnT = 0;
        syncNoses();
        state = "run";
        if (ui != null) ui.startHud();
        if (CM != null && (CM.getString("type").equals("timer") || CM.getString("type").equals("escort"))) {
            between = 0; wave = 1; spawnT = 1.5f; squadT = CM.has("squadEvery") ? 8f : 0f;
            if (ui != null) ui.banner(CM.getString("title"), false, CM.getString("goal"));
            G.sfx("wave");
        }
        ensureQuests();
        if (!G.SV.tut && (m.equals("endless") || (CM != null && "1-1".equals(CM.getString("id", ""))))) { tut = 0; tutT = 1.5f; }
        G.hap(25);
    }

    static Map<String, Float> mixOf(JsonValue m) {
        Map<String, Float> out = new LinkedHashMap<>();
        if (m == null) { out.put("quad", 1f); return out; }
        for (JsonValue v = m.child; v != null; v = v.next) out.put(v.name, v.asFloat());
        return out;
    }

    static Map<String, Integer> squadOf(JsonValue m) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (JsonValue v = m.child; v != null; v = v.next) out.put(v.name, v.asInt());
        return out;
    }

    void startWave() {
        wave += 1; waveHit = false; abUsed.put("salvo", false); abUsed.put("emp", false);
        if (mode.equals("campaign")) {
            JsonValue c = campWaveCfg();
            JsonValue bv = c.get("boss");
            bossWave = bv != null && (bv.isString() || (bv.isBoolean() && bv.asBoolean())); bossSpawned = false;
            squads = c.getInt("squads", 0); squadT = 3.5f; squadMix = c.get("squadMix");
            toSpawn = c.getInt("n"); spawned = 0; bossAt = (int) Math.ceil(toSpawn / 2.0); spawnInt = c.getFloat("int", 1.3f); spawnT = 0.5f;
            scoutQ = c.getInt("scouts", 0); scoutT = 2f;
            String bt = bossWave ? bossTypeForWave() : "";
            if (ui != null) {
                String sub = "";
                if (bossWave) {
                    sub = G.D.get("BOSS_INFO").get(bt).getString("name");
                    if (c.has("boss2")) sub += " + " + G.D.get("BOSS_INFO").get(c.getString("boss2")).getString("short");
                }
                ui.banner(String.format("Волна %d/%d", wave, CM.get("waves").size), bossWave, sub);
            }
            G.sfx(bossWave ? "boss" : "wave");
            if (c.has("say") && ui != null) for (JsonValue l = c.get("say").child; l != null; l = l.next) ui.comm(l.getString(0), l.getString(1));
            return;
        }
        squadMix = null;
        bossWave = wave % 5 == 0; bossSpawned = false;
        squads = wave >= 8 ? 1 + (wave - 8) / 2 : 0; squadT = 3.5f;
        if (wave == 8 && ui != null) ui.toast("Наземные роботы! Опускай ствол ниже");
        toSpawn = Math.round(bossWave ? 4 + wave : 4 + wave * 2.4f); spawned = 0; bossAt = (int) Math.ceil(toSpawn / 2.0);
        spawnInt = Math.max(0.35f, 1.5f - wave * 0.09f); spawnT = 0.4f;
        if (ui != null) ui.banner("Волна " + wave, bossWave, bossWave ? G.D.get("BOSS_INFO").get(bossTypeForWave()).getString("name") : "");
        G.sfx(bossWave ? "boss" : "wave");
        qProg("wave", wave, true); statMax("bestWave", wave);
    }

    Map<String, Float> endlessMix() {
        int w = wave;
        Map<String, Float> m = new LinkedHashMap<>();
        m.put("quad", 3f); m.put("wing", 1 + w * 0.2f);
        if (w >= 3) { m.put("zig", 2f); m.put("heavy", 1 + w * 0.1f); }
        if (w >= 4) m.put("swarm", 1.2f);
        if (w >= 5) m.put("decoy", 1f);
        if (w >= 6) m.put("kami", 1.2f);
        if (w >= 7) m.put("split", 1f);
        if (w >= 9) m.put("relay", 0.5f);
        if (w >= 6) m.put("flock", 0.6f);
        if (w >= 8) m.put("stealth", 0.8f);
        if (w >= 10) m.put("armor", 0.7f);
        return m;
    }

    void updateWaves(float dt) {
        missionTime += dt;
        if (CM != null && (CM.getString("type").equals("timer") || CM.getString("type").equals("escort"))) {
            missionT -= dt;
            if (CM.has("timeSay") && ui != null)
                for (JsonValue ts = CM.get("timeSay").child; ts != null; ts = ts.next)
                    if (missionTime >= ts.getFloat(0) && timeSaid < ts.getFloat(0)) { timeSaid = ts.getFloat(0); ui.comm(ts.getString(1), ts.getString(2)); }
            spawnT -= dt;
            float k = 1 - missionT / CM.getFloat("time");
            if (spawnT <= 0 && missionT > 3) {
                spawnFromMix(mixOf(CM.get("mix")));
                spawnT = Math.max(0.7f, 2.2f - k * 0.9f) * (CM.getBoolean("swarm", false) ? 0.85f : 1f);
            }
            if (CM.has("squadEvery")) {
                squadT -= dt;
                if (squadT <= 0) {
                    squadT = CM.getFloat("squadEvery");
                    Map<String, Integer> sm;
                    if (CM.has("squadMix")) sm = squadOf(CM.get("squadMix"));
                    else { sm = new LinkedHashMap<>(); sm.put("inf", 3); sm.put("rpg", 1); }
                    spawnSquad(sm);
                }
            }
            if (missionT <= 0) missionComplete();
            return;
        }
        if (between > 0) { between -= dt; if (between <= 0) startWave(); return; }
        if (toSpawn > 0) {
            spawnT -= dt;
            if (spawnT <= 0) {
                spawnT = spawnInt * rr(0.7f, 1.3f);
                toSpawn -= 1; spawned += 1;
                if (bossWave && !bossSpawned && spawned >= bossAt) {
                    bossSpawned = true; spawnEnemy(bossTypeForWave());
                    JsonValue c = campWaveCfg();
                    if (c != null && c.has("boss2")) spawnEnemy(c.getString("boss2"), null, rr(-0.6f, 0.6f), Float.NaN);
                    if (c != null && c.has("boss3")) spawnEnemy(c.getString("boss3"), null, rr(-0.9f, 0.9f), Float.NaN);
                }
                else spawnFromMix(mode.equals("campaign") ? mixOf(campWaveCfg().get("mix")) : endlessMix());
            }
        }
        if (squads > 0) {
            squadT -= dt;
            if (squadT <= 0) {
                squads -= 1; squadT = 9f;
                Map<String, Integer> mix = new LinkedHashMap<>();
                if (squadMix != null) mix = squadOf(squadMix);
                else if (wave >= 12) { mix.put("inf", 3); mix.put("rpg", 2); mix.put("mech", 1); } else { mix.put("inf", 4); mix.put("rpg", 1); }
                spawnSquad(mix);
            }
        }
        if (scoutQ > 0) {
            scoutT -= dt;
            if (scoutT <= 0) {
                scoutQ -= 1; scoutT = 6f;
                spawnEnemy("scout", null, rr(-0.4f, 0.4f), 600f);
                if (ui != null) ui.comm("lisa", "Разведчик! Сбей его, пока он не передал данные!");
            }
        }
        int left = liveNonShots();
        if (toSpawn <= 0 && squads <= 0 && scoutQ <= 0 && left == 0) {
            if (!waveHit) { statAdd("nohit", 1); nohitWaves += 1; qProg("nohit", nohitWaves, true); } else nohitWaves = 0;
            if (mode.equals("campaign") && wave >= CM.get("waves").size) { missionComplete(); return; }
            between = 3.5f; hp = Math.min(st("maxHp"), hp + st("regen"));
            int bonus = 0;
            if (mode.equals("endless")) bonus = addCoins((15 + 6 * wave) * difCoin * (!waveHit ? 1.5f : 1f));
            if (ui != null) ui.banner("Волна отбита", false, bonus > 0 ? String.format("Прочность +%d · награда +%d монет%s", (int) st("regen"), bonus, !waveHit ? " (без урона ×1.5)" : "") : "Прочность +" + (int) st("regen"));
            if (mode.equals("endless") && ui != null) ui.toast("Монеты боя: " + RUN.c + " — жми ₽ для улучшений");
            checkAch();
        }
    }

    // ---------- объект миссии ----------
    Mission makeObj() {
        if (CM == null || !CM.has("obj")) return null;
        JsonValue o = CM.get("obj");
        String kind = o.getString("kind", "radar");
        Mission ms = new Mission();
        ms.name = o.getString("name"); ms.hp = o.getFloat("hp") * 1.4f; ms.max = ms.hp; ms.kind = kind;
        if (kind.equals("convoy")) {
            for (int i = 0; i < 3; i++) { Objs.Obj m = Objs.make("truck"); root.add(m.g); ms.models.add(m); }
            ms.r = 9;
            updateObj(0);
        } else {
            Objs.Obj m = Objs.make(kind); root.add(m.g); ms.models.add(m);
            if (kind.equals("radar")) { m.g.pos.set(-26, height(-26, -22), -22); ms.aim.set(-26, height(-26, -22) + 6, -22); ms.r = 8; }
            else if (kind.equals("dam")) { m.g.pos.set(0, 0, -240); ms.aim.set(0, 18, -236); ms.r = 22; }
            else if (kind.equals("sub")) { m.g.pos.set(34, height(34, -40), -40); ms.aim.set(34, height(34, -40) + 4, -40); ms.r = 14; }
        }
        return ms;
    }

    void updateObj(float dt) {
        if (obj == null) return;
        if (obj.kind.equals("convoy")) {
            float p = MathUtils.clamp(missionTime / CM.getFloat("time", 80), 0, 1);
            for (int i = 0; i < obj.models.size; i++) {
                Objs.Obj m = obj.models.get(i);
                float z = 520 - p * 920 + i * 15, y = height(World.ROAD_X, z);
                m.g.pos.set(World.ROAD_X + (i % 2) * 1.2f - 0.6f, y + MathUtils.sin(missionTime * 6 + i) * 0.04f, z);
            }
            obj.aim.set(obj.models.get(1).g.pos).add(0, 2.5f, 0);
        }
        for (Objs.Obj m : obj.models) if (m.spin != null) m.spin.rotateY(dt * 1.2f);
        if (obj.hp < obj.max * 0.5f && R.nextFloat() < 0.1f)
            FX.puff(tmpV.set(obj.aim).add(rr(-3, 3), 0, rr(-3, 3)), 3f, new Color(0.12f, 0.12f, 0.12f, 1), 0.5f, 3f, 3f);
    }

    void damageObj(float n) {
        if (obj == null || obj.hp <= 0) return;
        obj.hp -= n; hurtT = 0.25f; G.sfx("hit", 0.6f, 80);
        if (ui != null && R.nextFloat() < 0.15f) ui.comm("lisa", obj.name + " под ударом!");
        if (obj.hp <= 0) {
            obj.hp = 0; explosion(obj.aim, 3);
            final String nm = obj.name;
            later(0.6f, () -> missionFail("Объект «" + nm + "» уничтожен"));
        }
    }

    // ---------- итог миссии ----------
    float statOf(String k) {
        if (k.equals("hp")) return Math.max(0, hp) / st("maxHp");
        if (k.equals("obj")) return obj != null ? Math.max(0, obj.hp) / obj.max : 1f;
        if (k.equals("time")) return missionTime;
        return 0;
    }

    boolean starOk(JsonValue st) {
        if (!st.has("k")) return true;
        float v = statOf(st.getString("k"));
        return st.getString("op").equals(">=") ? v >= st.getFloat("v") : v <= st.getFloat("v");
    }

    void missionComplete() {
        BlackBox.log("миссия выполнена");
        if (!state.equals("run")) return;
        state = "result"; firing = false;
        JsonValue stars = CM.get("stars");
        boolean[] ok = new boolean[stars.size];
        int got = 0;
        for (int i = 0; i < stars.size; i++) { ok[i] = starOk(stars.get(i)); if (ok[i]) got++; }
        String id = CM.getString("id");
        Integer pvI = G.SV.camp.get(id);
        int prev = pvI == null ? 0 : pvI;
        int reward;
        String special = CM.getString("special", "");
        int rw = CM.getInt("reward");
        if (special.equals("chal")) {
            Integer pc = G.SV.chal.get(id);
            int pv = pc == null ? 0 : pc;
            reward = pv > 0 ? Math.round(rw * 0.25f) : rw;
            if (pv == 0) statAdd("chal", 1);
            G.SV.chal.put(id, Math.max(pv, got));
        } else {
            reward = (prev == 0 ? rw : 0) + Math.max(0, got - Math.max(prev, 1)) * Math.round(rw * 0.25f);
            G.SV.camp.put(id, Math.max(prev, got));
        }
        G.SV.c += reward;
        if (CM.getBoolean("final", false) && !G.SV.skOwn.contains("aurum")) G.SV.skOwn.add("aurum");
        if (CM.has("skin") && !G.SV.skOwn.contains(CM.getString("skin"))) G.SV.skOwn.add(CM.getString("skin"));
        xpGot = Math.max(5, Math.round(score / 150f)) + got * 5;
        rankUp = G.addXp(xpGot);
        checkAch(); G.save(); G.sfx("clear"); G.hap(40);
        resWin = true; resOk = ok; resGot = got; resReward = reward; resReason = ""; resM = CM;
        final JsonValue cm = CM;
        later(0.9f, () -> { if (state.equals("result") && CM == cm && ui != null) ui.result(true); });
    }

    void missionFail(String reason) {
        BlackBox.log("миссия провалена: " + reason);
        if (!(state.equals("run") || state.equals("dying"))) return;
        if (!mode.equals("campaign")) return;
        state = "result"; firing = false; G.save(); G.sfx("fail"); G.hap(60);
        resWin = false; resOk = null; resGot = 0; resReward = 0; resReason = reason; resM = CM;
        final JsonValue cm = CM;
        later(1.1f, () -> { if (state.equals("result") && CM == cm && ui != null) ui.result(false); });
    }

    void gameOver() {
        BlackBox.log("бой окончен · волна " + wave + " · очки " + score);
        if (mode.equals("duel")) { if (duel != null && !netView) duel.finishHost("boss", "Комплекс ПВО уничтожен!"); return; }
        if (mode.equals("campaign")) { state = "run"; missionFail("Комплекс уничтожен"); return; }
        state = "over";
        boolean rec = score > G.SV.best;
        if (rec) G.SV.best = score;
        if (score > 0) {
            G.Top t = new G.Top(); t.s = score; t.w = wave; t.d = G.SV.diff; t.t = G.today(); t.v = VEH;
            G.SV.top.add(t);
            Collections.sort(G.SV.top, (a, b) -> Integer.compare(b.s, a.s));
            while (G.SV.top.size() > 10) G.SV.top.remove(G.SV.top.size() - 1);
        }
        G.SV.c += Math.round(runCoins * 0.3f);
        xpGot = score > 0 ? Math.max(1, Math.round(score / 100f)) : 0;
        rankUp = G.addXp(xpGot);
        checkAch(); G.save();
        if (ui != null) ui.over(rec);
    }

    public void quitBattle() {
        clearBattle();
        for (Models.Mog m : mogs) m.g.remove();
        mogs.clear(); state = "menu"; RUN = null; CM = null; mode = "endless"; netView = false; duelRole = ""; bossE = null; G.save();
    }

    public void statAdd(String k, float v) { Float o = G.SV.stats.get(k); G.SV.stats.put(k, (o == null ? 0 : o) + v); }

    void statMax(String k, float v) { Float o = G.SV.stats.get(k); if (o == null || v > o) G.SV.stats.put(k, v); }

    public float statVal(String k) {
        if (k.equals("campDone") || k.equals("campStars")) {
            float n = 0;
            for (JsonValue ch = G.D.get("CHAPTERS").child; ch != null; ch = ch.next)
                for (JsonValue m = ch.get("missions").child; m != null; m = m.next) {
                    Integer s = G.SV.camp.get(m.getString("id"));
                    int sv = s == null ? 0 : s;
                    n += k.equals("campDone") ? (sv > 0 ? 1 : 0) : sv;
                }
            return n;
        }
        Float v = G.SV.stats.get(k);
        return v == null ? 0 : v;
    }

    public void checkAch() {
        for (JsonValue a = G.D.get("ACH").child; a != null; a = a.next) {
            String id = a.getString("id");
            if (!G.SV.ach.contains(id) && !a.getString("st").equals("coop") && statVal(a.getString("st")) >= a.getFloat("n")) {
                G.SV.ach.add(id); G.SV.c += a.getInt("r");
                if (ui != null) ui.toast("Достижение: " + a.getString("name") + " (+" + a.getInt("r") + ")");
            }
        }
    }

    public void ensureQuests() {
        String d = G.today();
        if (G.SV.qDay.equals(d) && G.SV.qList.size() > 0) return;
        Random rng = new Random(("q" + d).hashCode());
        List<String> keys = new ArrayList<>();
        for (JsonValue q = G.D.get("QDEF").child; q != null; q = q.next) keys.add(q.name);
        List<G.Quest> list = new ArrayList<>();
        while (list.size() < 3) {
            String k = keys.get(rng.nextInt(keys.size()));
            boolean dup = false;
            for (G.Quest q : list) if (q.id.equals(k)) dup = true;
            if (dup) continue;
            JsonValue qd = G.D.get("QDEF").get(k);
            int lv = rng.nextInt(qd.get("n").size);
            G.Quest q = new G.Quest(); q.id = k; q.lv = lv; q.n = qd.get("n").getFloat(lv); q.r = qd.get("r").getFloat(lv);
            list.add(q);
        }
        G.SV.qDay = d; G.SV.qList.clear(); G.SV.qList.addAll(list);
        G.save();
    }

    public void qProg(String id, float v, boolean isMax) {
        for (G.Quest q : G.SV.qList) {
            if (q.id.equals(id) && !q.done) {
                q.p = isMax ? Math.max(q.p, v) : q.p + v;
                if (q.p >= q.n) {
                    q.p = q.n; q.done = true;
                    if (ui != null) ui.toast("Задание выполнено: " + G.D.get("QDEF").get(id).get("t").getString(q.lv));
                }
            }
        }
    }

    // ---------- камера ----------
    public void updateCamera(float dt) {
        if (state.equals("menu")) {
            float a2 = 0.75f + MathUtils.sin(T * 0.08f) * 0.45f;
            cam.position.set(MathUtils.sin(a2) * 21, 6.8f + MathUtils.sin(T * 0.3f) * 0.6f, MathUtils.cos(a2) * 21 - 2);
            cam.direction.set(0, 4, 0).sub(cam.position).nor();
            cam.up.set(Vector3.Y);
            cam.direction.rotateRad(Vector3.Y, 0.42f);
            cam.update();
            return;
        }
        if (mode.equals("duel") && duelRole.equals("boss") && duelBoss() != null && !(vm != null && vm.broken)) {
            float p0 = aimPitch;
            aimDir.set(-MathUtils.sin(aimYaw) * MathUtils.cos(p0), MathUtils.sin(p0), -MathUtils.cos(aimYaw) * MathUtils.cos(p0));
            aimPoint.set(AIM).mulAdd(aimDir, 450);
            bossCamera(dt);
            return;
        }
        if (vm != null && vm.broken) {
            wreckCam += dt;
            float a = aimYaw + 0.6f + wreckCam * 0.25f, r = 17 - Math.min(5f, wreckCam * 1.5f);
            cam.position.set(MathUtils.sin(a) * r, 7 + MathUtils.sin(wreckCam * 0.5f), MathUtils.cos(a) * r);
            cam.direction.set(0, 2.2f, 0).sub(cam.position).nor();
            cam.up.set(Vector3.Y);
            cam.update();
            return;
        }
        wreckCam = 0;
        float p = aimPitch;
        aimDir.set(-MathUtils.sin(aimYaw) * MathUtils.cos(p), MathUtils.sin(p), -MathUtils.cos(aimYaw) * MathUtils.cos(p));
        Vector3 tp = vm.turret.gpos(new Vector3());
        camPos.set(tp).add(MathUtils.sin(aimYaw) * 13.5f * MathUtils.cos(p * 0.85f), 4.6f - 2.2f * MathUtils.sin(p), MathUtils.cos(aimYaw) * 13.5f * MathUtils.cos(p * 0.85f))
                .add(MathUtils.cos(aimYaw) * 2.8f, 0, -MathUtils.sin(aimYaw) * 2.8f);
        camPos.y = Math.max(camPos.y, 2.2f);
        Vector3 cp = new Vector3(camPos);
        if (shake > 0) {
            tA.set(rr(-1, 1), rr(-1, 1), 0).scl(shake * 0.35f);
            cp.add(tA);
            shake = Math.max(0, shake - dt * 2.2f);
        }
        aimPoint.set(camPos).mulAdd(aimDir, 450);
        // отдача: камеру толкает назад и чуть подбрасывает прицел; «Гром» трясёт землю
        boolean grom = VEH.equals("grom");
        cp.mulAdd(aimDir, -recoil * recoil * (grom ? 0.6f : 0.22f));
        if (quake > 0) cp.y += MathUtils.sin(T * 61f) * quake * 0.22f + MathUtils.sin(T * 37f) * quake * 0.12f;
        cam.position.set(cp);
        cam.direction.set(aimDir).add(0, recoil * recoil * (grom ? 0.03f : 0.01f), 0).nor();
        if (killCamT > 0) {
            killCamT -= dt;
            float pr = 1f - Math.max(0, killCamT) / KILL_DUR;
            float k = Math.min(smooth(MathUtils.clamp(pr / 0.14f, 0, 1)), smooth(MathUtils.clamp((1f - pr) / 0.2f, 0, 1)));
            Vector3 f = (killNode != null && killNode.parent != null) ? killNode.gpos(jA) : jA.set(killFocus);
            killFocus.set(f);
            float a = killA0 + pr * 1.4f;
            jB.set(f).add(MathUtils.sin(a) * killR, killR * 0.3f + 3f, MathUtils.cos(a) * killR);
            jB.y = Math.max(jB.y, height(jB.x, jB.z) + 3f);
            cam.position.lerp(jB, k);
            tmpV.set(f).sub(cam.position).nor();
            cam.direction.lerp(tmpV, k).nor();
        }
        cam.up.set(Vector3.Y);
        cam.update();
    }

    static float smooth(float x) { return x * x * (3 - 2 * x); }

    // ---------- шаг ----------
    public void step(float dt) {
        T += dt;
        root.updateGlobal(null);
        if (state.equals("over")) { updateWreck(dt); return; }
        if (state.equals("pause") || state.equals("shop")) return;
        if (state.equals("run") || state.equals("dying") || state.equals("result")) {
            for (int i = timers.size - 1; i >= 0; i--) {
                Timer tm = timers.get(i);
                tm.t -= dt;
                if (tm.t <= 0) { timers.removeIndex(i); tm.r.run(); }
            }
            if (state.equals("run")) {
                fireT -= dt;
                boolean gunOn = st("gun") > 0 && !VEH.equals("tor");
                if (firing && gunOn && !overheated) {
                    while (fireT <= 0) { fireT += 1f / st("rate"); fireBullet(); heat += st("heatPer"); }
                    if (heat >= 1) { heat = 1; overheated = true; G.sfx("alarm", 0.6f, 900); }
                } else fireT = Math.max(fireT, 0);
                if (VEH.equals("tor")) {
                    chanT -= dt;
                    if (firing && chanT <= 0 && missiles > 0) {
                        E t = (lock.t != null && lock.k >= 0.5f) ? lock.t : null;
                        if (fireMissile(t, false)) {
                            chanT = st("chan");
                            if (R.nextFloat() < st("twin") && missiles > 0) {
                                E t2 = null;
                                for (E e : enemies) if (e != t && e.cloak == 0 && angleBetween(aimDir, tA.set(e.pos).sub(camPos)) < 0.5f) { t2 = e; break; }
                                fireMissile(t2 != null ? t2 : t, false);
                            }
                        }
                    }
                }
                if (VEH.equals("luch")) updateBeam(dt); else beamUsed = 0;
                heat = Math.max(0, heat - dt * st("cool") * (overheated ? 1.5f : 1f));
                if (overheated && heat < 0.3f) overheated = false;
                if (missiles < st("maxMis") && !netView) {
                    reloadT -= dt;
                    if (reloadT <= 0) { missiles += 1; reloadT = missiles < st("maxMis") ? st("reload") : 0; syncNoses(); }
                }
                if (comboT > 0) { comboT -= dt; if (comboT <= 0) combo = 0; }
                empT = Math.max(0, empT - dt); smokeT = Math.max(0, smokeT - dt);
                abCd.put("smoke", Math.max(0, abCd.get("smoke") - dt)); abCd.put("repair", Math.max(0, abCd.get("repair") - dt));
                if (repairT > 0 && !netView) { repairT -= dt; hp = Math.min(st("maxHp"), hp + st("maxHp") / 3f / 4f * dt); }
                if (mode.equals("duel")) { if (!netView) duelRules(dt); }
                else updateWaves(dt);
                updateTut(dt);
                if (mode.equals("endless") && world.dynSky) {
                    // закат постепенно переходит в ночь: с 6-й волны темнеет, к 11-й — ночь
                    world.nightTgt = MathUtils.clamp((wave - 5) / 6f, 0, 1);
                    if (wave >= 4 && between > 0 && wxRoll != wave) {
                        wxRoll = wave;
                        if (stormLeft > 0) { stormLeft--; if (stormLeft == 0) { world.stormTgt = 0; if (ui != null) ui.toast("Гроза уходит"); } }
                        else if (R.nextFloat() < 0.3f) {
                            stormLeft = 2; world.stormTgt = 1;
                            if (ui != null) ui.toast("Надвигается гроза — молнии подсвечивают цели");
                        }
                    }
                } else if (mode.equals("endless") && wave >= 6 && between > 0 && wxRoll != wave) {
                    wxRoll = wave;
                    if (R.nextFloat() < 0.3f) {
                        world.rain = R.nextFloat() < 0.6f ? 1 : 0;
                        world.wind = world.rain > 0 ? rr(-60, 60) : 0;
                    }
                }
            } else beamUsed = 0;
            if (!netView) updateEnemies(dt);
            updateWrecks(dt);
            updateJuice(dt);
            if (!netView) { updateEbul(dt); updateRockets(dt); }
            updateMogs(dt);
            updateObj(dt);
            updateLock(dt);
            if (state.equals("dying")) { deadT -= dt; if (deadT <= 0) gameOver(); }
            float mh = st("maxHp");
            if (hp < mh * 0.65f && hp > mh * 0.35f && R.nextFloat() < 0.12f)
                FX.puff(tmpV.set(rr(-1, 1), 3.2f, rr(0, 3)), 1f, new Color(0.35f, 0.35f, 0.35f, 1), 0.45f, 2f, 2.5f);
            if (hp < mh * 0.35f && hp > 0 && R.nextFloat() < 0.25f) {
                FX.puff(tmpV.set(rr(-1, 1), 3.5f, rr(0, 3)), 1.5f, new Color(0.12f, 0.12f, 0.12f, 1), 0.6f, 2.5f, 3f);
                if (R.nextFloat() < 0.4f) FX.glow(tmpV.set(rr(-1, 1), 3.2f, rr(0, 3)), 1.5f, new Color(1, 0.5f, 0.15f, 1), 0.4f);
            }
        }
        updateBullets(dt);
        updateWreck(dt);
        recoil = Math.max(0, recoil - dt * 7f); quake = Math.max(0, quake - dt * 3f);
        if (state.equals("run")) heatJuice(dt);
        if (vm != null && !vm.broken && pitchBase != null) {
            boolean grom = VEH.equals("grom");
            float rk = recoil * recoil;
            vm.pitch.pos.set(pitchBase).add(0, 0, rk * (grom ? 0.32f : 0.12f));
            vm.root.pos.set(rootBase);
            if (grom) vm.root.pos.add(MathUtils.sin(aimYaw) * rk * 0.12f + rr(-1, 1) * quake * 0.03f, quake * 0.025f * rr(-1, 1), MathUtils.cos(aimYaw) * rk * 0.12f + rr(-1, 1) * quake * 0.03f);
        }
        if (vm != null && !vm.broken) {
            vm.turret.setEuler(0, state.equals("menu") ? MathUtils.sin(T * 0.4f) * 0.6f : aimYaw, 0);
            vm.pitch.setEuler(state.equals("menu") ? 0.35f + MathUtils.sin(T * 0.5f) * 0.1f : aimPitch, 0, 0);
            vm.radar.rotateY(dt * 2.5f);
        }
        hurtT = Math.max(0, hurtT - dt);
    }

    /** Луч прожектора (ночь). */
    public void updateSpot() {
        if (world.beam == null || !world.beamOn) return;
        Vector3 src = (vm != null && !vm.broken) ? vm.turret.toGlobal(new Vector3(0, 1.6f, -1.2f), new Vector3()) : new Vector3(0, 4, 0);
        Vector3 tgt = state.equals("menu") ? new Vector3(-60, 80, -300) : new Vector3(aimPoint);
        world.beam.pos.set(src);
        world.beam.lookDir(tgt.sub(src), Vector3.Y);
    }

    static final String[] TUT = {"Ведите пальцем по экрану — башня и прицел поворачиваются", "Держите кнопку «Огонь» — пушка стреляет туда, где прицел",
            "Наведите рамку на цель до надписи «Захват» и нажмите «Ракета»", "Красные стрелки по краям показывают врагов за экраном. Удачи!"};
    static final String[] TUT_PC = {"Двигайте мышью — башня и прицел поворачиваются (кликните по экрану, если курсор виден)",
            "Держите левую кнопку мыши или пробел — пушка стреляет туда, где прицел", "Наведите рамку на цель до надписи «Захват» и нажмите правую кнопку мыши",
            "Красные стрелки по краям — враги за экраном. 1–4 — способности, B — улучшения. Удачи!"};

    void updateTut(float dt) {
        if (tut < 0) return;
        tutT -= dt;
        if (tutT <= 0) {
            if (tut >= TUT.length) { tut = -1; G.SV.tut = true; G.save(); return; }
            String msg = G.isMobile() ? TUT[tut] : TUT_PC[tut];
            if (!G.isMobile() && VEH.equals("tor") && tut == 1) msg = "Держите левую кнопку мыши — «Молот» пускает ракеты по целям в прицеле";
            if (G.isMobile() && VEH.equals("tor") && tut == 1) msg = "Держите «Пуск» — «Молот» пускает ракеты по целям в прицеле";
            if (VEH.equals("luch") && tut == 1) msg = "Держите «Огонь» — «Луч» жжёт цель в прицеле. Следите за нагревом!";
            if (!VEH.equals("pantsir") && tut == 2) { tut += 1; return; }
            if (ui != null) ui.hint(msg);
            tut += 1; tutT = 5.5f;
        }
    }

    /** Заполнить динамику (трассеры, лучи) перед отрисовкой. */
    public void fillDyn() { fillBeams(); }
}
