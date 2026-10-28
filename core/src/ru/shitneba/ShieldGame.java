package ru.shitneba;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ScreenUtils;

import java.util.ArrayList;

import ru.shitneba.gfx.Dyn;
import ru.shitneba.gfx.FpvFx;
import ru.shitneba.gfx.Node3;
import ru.shitneba.gfx.Renderer;
import ru.shitneba.gfx.Replay;
import ru.shitneba.ui.Draw;
import ru.shitneba.ui.UI;

/** Точка входа: цикл кадра, камера, связка мира, боя и интерфейса. */
public class ShieldGame extends ApplicationAdapter implements UI.Host, UI.StrikeHost {
    /** Папка для автоматических снимков экрана (отладка на ПК). */
    public static String shotDir;
    /** Отладка: принудительно выбранная машина. */
    public static String forceVeh;
    /** Отладка: «фотостудия» всех моделей (аргумент models). */
    public static boolean studioMode;
    Studio studio;
    int stIdx, stView;

    Renderer rend;
    World world;
    Game game;
    UI ui;
    Draw draw;
    Strike strike;
    Duel duel;
    Replay replay;
    FpvFx fpv;
    PerspectiveCamera cam;
    final Array<Node3> roots = new Array<>();
    final Array<Dyn> dyns = new Array<>();
    float T;
    boolean botOn;
    int shotStep = 0;
    float shotT = 0;

    @Override
    public void create() {
        G.init();
        BlackBox.start();
        BlackBox.hold = true;
        BlackBox.mark("загрузка");
        G.sfx = new Sfx();
        Models.initMaterials();
        rend = new Renderer();
        FX.init();
        FX.env = rend.env;
        SmokeTrail.init();
        cam = new PerspectiveCamera(62, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.near = 0.3f; cam.far = 6000f;
        rend.cam = cam;
        world = new World(rend);
        game = new Game();
        game.world = world; game.cam = cam;
        FX.height = (x, z) -> world.height(x, z);
        draw = new Draw();
        ui = new UI(game, draw, this);
        game.ui = ui;
        roots.add(world.root); roots.add(game.root);
        strike = new Strike(game, world, cam);
        strike.ui = ui; ui.strike = strike; ui.strikeHost = this;
        roots.add(strike.root3);
        replay = new Replay();
        game.replay = replay; ui.replay = replay;
        duel = new Duel(game);
        duel.ui = ui; game.duel = duel; ui.duel = duel;
        fpv = new FpvFx();
        if (forceVeh != null) { G.SV.veh = forceVeh; for (String v : G.VEHS) if (!G.SV.vown.contains(v)) G.SV.vown.add(v); }
        game.VEH = G.SV.veh;
        applyGfxNoRebuild();
        world.build(0, false, 0, 0, 0, "", false);
        for (Vector3 s : world.stacks) FX.chimney(s);
        game.applyStats();
        game.setVehicleModel();
        game.missiles = (int) game.st("maxMis");
        game.syncNoses();
        game.ensureQuests();
        Gdx.input.setInputProcessor(ui);
        Gdx.input.setCatchKey(Input.Keys.BACK, true);
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        warmBuild();
        boolean showRep = shotDir == null || System.getenv("SN_REPORT") != null;
        String report = showRep ? BlackBox.takeReport() : null;
        if (report != null) ui.showReport(report);
        else if (shotDir == null) ui.checkDaily();
        if (shotDir != null) { botOn = true; G.SV.tut = true; }
        if (studioMode) {
            botOn = false;
            studio = new Studio();
            roots.removeValue(game.root, true); roots.removeValue(strike.root3, true);
            roots.add(studio.root);
            studio.show(0, world);
        }
        BlackBox.tick();
        BlackBox.hold = false;
    }

    float baseScale = 1f, fpsAcc; int fpsN;
    Node3 warm; int warmFrames;

    /** Прогрев: один кадр со всеми моделями, чтобы шейдеры скомпилировались до боя. */
    void warmBuild() {
        warm = new Node3();
        int i = 0;
        for (JsonValue t = G.D.get("DT").child; t != null; t = t.next, i++) {
            for (String acc : new String[]{"harpy", "manti"}) {
                try {
                    Models.EnemyModel m = Models.enemy(t.name, acc);
                    m.g.pos.set((i % 6) * 0.05f, (i / 6) * 0.05f, 0);
                    m.g.scl.set(0.004f, 0.004f, 0.004f);
                    warm.add(m.g);
                } catch (Throwable e) { Gdx.app.error("warm", t.name, e); }
            }
        }
        Node3 ms = new Node3(Models.missileMesh(), Models.MAT_MISSILE);
        ms.scl.set(0.01f, 0.01f, 0.01f);
        warm.add(ms);
        Node3 fl = FX.flame(1.78f); fl.scl.set(0.01f, 0.01f, 0.01f); warm.add(fl);
        Models.Mog mg = Models.mog(); mg.g.scl.set(0.01f, 0.01f, 0.01f); warm.add(mg.g);
        roots.add(warm);
        warmFrames = 3;
        // символы шрифтов
        draw.prewarm();
    }

    void warmStep() {
        if (warm == null) return;
        cam.update();
        warm.pos.set(cam.position).mulAdd(cam.direction, 2f);
        if (--warmFrames <= 0) { roots.removeValue(warm, true); warm = null; }
    }

    void applyGfxNoRebuild() {
        int q = G.SV.gfx;
        rend.shadows = q > 0;
        rend.shadowRange = q == 2 ? 90f : 70f;
        boolean mob = G.isMobile();
        rend.shadowEvery = mob && q < 2 ? 2 : 1;
        float[] lvl = {0.6f, 0.8f, 1f};
        baseScale = lvl[q];
        if (mob) {
            // ограничиваем число пикселей 3D-картинки на экранах высокой плотности
            float[] maxH = {540f, 720f, 1080f};
            int bh = Math.max(1, Gdx.graphics.getBackBufferHeight());
            baseScale = Math.min(baseScale, maxH[q] / bh);
        }
        baseScale = MathUtils.clamp(baseScale, 0.35f, 1f);
        String fs = System.getenv("SN_SCALE");
        if (fs != null) baseScale = Float.parseFloat(fs);
        rend.scale = baseScale;
    }

    void dynScale(float delta) {
        fpsAcc += delta; fpsN++;
        if (fpsAcc < 2f) return;
        float fps = fpsN / fpsAcc;
        fpsAcc = 0; fpsN = 0;
        float lo = Math.max(0.35f, baseScale * 0.6f);
        if (fps < 45 && rend.scale > lo) rend.scale = Math.max(lo, rend.scale - 0.1f);
        else if (fps > 57 && rend.scale < baseScale) rend.scale = Math.min(baseScale, rend.scale + 0.05f);
    }

    @Override
    public void applyGfx() {
        applyGfxNoRebuild();
        if (game.state.equals("menu")) toMenuWorld();
    }

    @Override
    public void toMenuWorld() {
        FX.clear();
        world.build(0, false, 0, 0, 0, "", false);
        for (Vector3 s : world.stacks) FX.chimney(s);
        game.VEH = G.SV.veh;
        game.applyStats();
        game.setVehicleModel();
        game.missiles = (int) game.st("maxMis");
        game.syncNoses();
    }

    @Override
    public void toggleFullscreen() {
        if (Gdx.graphics.isFullscreen()) Gdx.graphics.setWindowedMode(1280, 720);
        else Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
    }

    @Override public boolean isFullscreen() { return Gdx.graphics.isFullscreen(); }

    @Override public boolean bot() { return botOn; }

    @Override
    public void startStrike() {
        strike.bot = botOn;
        strike.start();
    }

    @Override
    public void exitStrike(boolean hangar) {
        strike.exit();
        ui.show("");
        toMenuWorld();
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        ui.show(hangar ? "swarm" : "menu");
    }

    @Override
    public void resize(int w, int h) {
        if (w <= 0 || h <= 0) return;
        cam.viewportWidth = w; cam.viewportHeight = h;
        cam.fieldOfView = w / (float) Math.max(1, h) < 1.5f ? 70f : 62f;
        cam.update();
        draw.resize();
        applyGfxNoRebuild();
    }

    @Override
    public void pause() {
        BlackBox.hold = true;
        BlackBox.log("свёрнута");
        ui.onPause();
        G.save();
    }

    @Override
    public void resume() {
        BlackBox.tick();
        BlackBox.hold = false;
        BlackBox.log("развёрнута");
    }

    /** Отладка: SN_TEST=crash | freeze | lowres. */
    static final String TEST = System.getenv("SN_TEST");

    int bbFrame;

    void bbInfo() {
        if (++bbFrame % 15 != 0) return;
        BlackBox.info = game.mode + " · " + game.state + " · панель «" + ui.panel + "» · волна " + game.wave + " · врагов " + game.enemies.size
                + " · частиц " + FX.count() + " · кадров/с " + Gdx.graphics.getFramesPerSecond() + " · масштаб " + String.format(java.util.Locale.US, "%.2f", rend.scale)
                + (strike.active ? " · рой" : "") + " · машина " + game.VEH;
    }

    @Override
    public void render() {
        BlackBox.tick();
        BlackBox.mark("кадр");
        bbInfo();
        String tst = TEST;
        if (tst != null && T > 4f && T < 4.2f) {
            if (tst.equals("crash")) throw new IllegalStateException("тестовый вылет чёрного ящика");
            if (tst.equals("freeze")) { BlackBox.mark("тест: зависание"); try { Thread.sleep(4000); } catch (InterruptedException ignored) { } T = 5f; }
        }
        float dt = Math.min(Gdx.graphics.getDeltaTime(), 0.05f);
        if (shotDir != null) dt = 1f / 30f;
        T += dt;
        if (shotDir == null) dynScale(Gdx.graphics.getDeltaTime());
        else if ("lowres".equals(TEST)) rend.scale = 0.6f;
        if (botOn && !strike.active) bot(dt);
        BlackBox.mark("дуэль: сеть");
        duel.update(dt);
        BlackBox.mark("интерфейс: обновление");
        ui.update(dt);
        BlackBox.mark("бой");
        // замедление времени (добивание босса): бой, мир и эффекты идут медленнее, камера и интерфейс — нет
        float gdt = strike.active ? dt : dt * game.timeScale();
        game.step(gdt);
        BlackBox.mark("рой / камера");
        if (strike.active) strike.update(dt);
        else if (studio != null) studio.aim(cam, stView);
        else game.updateCamera(dt);
        warmStep();
        BlackBox.mark("мир");
        world.update(gdt, cam);
        game.updateSpot();
        SmokeTrail.wind = world.wind;
        BlackBox.mark("эффекты");
        FX.update(gdt, cam);
        FX.fill(cam);
        game.fillDyn();
        // тени — вокруг точки перед камерой
        Vector3 f = rend.shadowFocus.set(cam.direction.x, 0, cam.direction.z).nor().scl(game.state.equals("menu") ? 0 : 22).add(cam.position.x, 0, cam.position.z);
        if (game.state.equals("menu")) f.set(0, 0, 0);
        dyns.clear();
        dyns.add(world.cloudDyn); dyns.add(FX.dDebris); dyns.add(FX.dCasing); dyns.add(SmokeTrail.dyn); dyns.add(FX.dSmoke);
        dyns.add(game.tracers); dyns.add(game.beamDyn); dyns.add(strike.tracers); dyns.add(strike.beamDyn); dyns.add(FX.dAdd); dyns.add(FX.dRing); dyns.add(world.boltDyn); dyns.add(world.rainDyn);
        if (strike.active) rend.shadowFocus.set(cam.position.x, 0, cam.position.z);
        BlackBox.mark("рендер кадра");
        if (studio == null) ui.op.world3D();
        rend.render(roots, dyns);
        BlackBox.mark("лучший момент: запись");
        boolean rec = studio == null && !strike.active && !game.mode.equals("duel") && (game.state.equals("run") || game.state.equals("dying"));
        replay.tick(Gdx.graphics.getDeltaTime(), rec);
        if (strike.active && ui.panel.isEmpty()) fpv.draw(strike.noise(), strike.T, strike.hitT, strike.camMode == 0 && strike.st.equals("fly") ? 1 : 0);
        BlackBox.mark("отрисовка интерфейса");
        if (studio == null) ui.render();
        BlackBox.mark("кадр");
        if (studio != null) studioShots(dt);
        else if (shotDir != null) shots(dt);
    }

    // ---------- автопилот для проверок ----------
    float botT;

    void bot(float dt) {
        if (!game.state.equals("run")) { game.firing = false; return; }
        botT += dt;
        Game.E best = null; float bd = 1e9f;
        for (Game.E e : game.enemies) {
            if (e.cloak != 0 || e.type.equals("shell") || e.type.equals("node")) continue;
            float dd = e.pos.dst2(game.camPos);
            if (dd < bd) { bd = dd; best = e; }
        }
        if (best == null) { game.firing = false; return; }
        Vector3 lp = game.leadPoint(best, new Vector3()).sub(game.camPos);
        float yaw = MathUtils.atan2(-lp.x, -lp.z);
        float pitch = MathUtils.clamp((float) Math.atan2(lp.y, Math.sqrt(lp.x * lp.x + lp.z * lp.z)), -0.06f, 1.2f);
        float dy = yaw - game.aimYaw;
        while (dy > MathUtils.PI) dy -= MathUtils.PI2;
        while (dy < -MathUtils.PI) dy += MathUtils.PI2;
        game.aimYaw += dy * Math.min(1, dt * 6);
        game.aimPitch += (pitch - game.aimPitch) * Math.min(1, dt * 6);
        game.firing = Math.abs(dy) < 0.08f && !game.overheated;
        if (game.lock.t != null && game.lock.k >= 1 && game.missiles > 0 && botT % 1.5f < dt) game.fireMissile("lock", false);
    }

    public static String[][] SEQ = {
            {"menu", "3"}, {"skin", "2"}, {"daily", "1"}, {"garage", "1"}, {"garage1", "1"}, {"shop", "1"}, {"quests", "1"}, {"ach", "1"},
            {"records", "1"}, {"settings", "1"}, {"endless", "1"}, {"play1", "9"}, {"play2", "14"}, {"boss", "10"}, {"pause", "1"},
            {"bshop", "1"}, {"over", "5"}, {"end", "0"}};
    public static final String[][] SEQ_CAMP = {
            {"menu", "3"}, {"campaign", "1"}, {"cut", "1"}, {"brief", "1"}, {"chal", "1"}, {"m12", "12"}, {"m24", "12"}, {"mwin", "4"}, {"m14", "10"},
            {"mfail", "5"}, {"end", "0"}};

    public static final String[][] SEQ_STRIKE = {
            {"menu", "3"}, {"swarm", "1"}, {"s1", "6"}, {"s2", "8"}, {"s3", "10"}, {"spause", "1"}, {"s4", "3"}, {"sover", "4"}, {"end", "0"}};
    public static final String[][] SEQ_DUELHOST = {{"menu", "3"}, {"duel", "1"}, {"dhost", "14"}, {"dh2", "6"}, {"dh3", "10"}, {"end", "0"}};
    public static final String[][] SEQ_DUELJOIN = {{"menu", "5"}, {"djoin", "6"}, {"dj2", "4"}, {"dj3", "10"}, {"dj4", "8"}, {"end", "0"}};

    /** Скриншоты для магазина (RuStore): бой на разных картах кампании. SN_STORE=6-2,9-1 — свой список. */
    public static String[][] SEQ_STORE = storeSeq();

    static String[][] storeSeq() {
        String env = System.getenv("SN_STORE");
        String[] ids = env != null ? env.split(",") : new String[]{"6-2", "9-1", "4-1", "7-1", "10-1", "2-1", "8-1"};
        ArrayList<String[]> out = new ArrayList<>();
        out.add(new String[]{"menu", "3"});
        for (String id : ids) out.add(new String[]{"st_" + id, "13"});
        out.add(new String[]{"end", "0"});
        return out.toArray(new String[0][]);
    }

    /** Проверка «сочности»: выстрелы, гильзы, взрывы, облёт босса, ночь и гроза. */
    public static final String[][] SEQ_FX = {
            {"menu", "3"}, {"endless", "1"}, {"fx_fire", "6"}, {"fx_boom", "0.17"}, {"fx_boom2", "0.6"}, {"fx_fires", "2.5"}, {"fx_heat", "0.6"},
            {"fx_boss", "1.3"}, {"fx_boss2", "1.2"}, {"fx_after", "2"}, {"fx_dusk", "1"}, {"fx_night", "1.5"}, {"fx_storm", "1.2"}, {"fx_bolt", "0.1"},
            {"fx_nboom", "0.15"}, {"end", "0"}};

    /** 3.7.0: операторы — окно, кейсы, рулетка, девчонка в бою, умения, спасение Валькирии. */
    public static final String[][] SEQ_OPS = {
            {"menu", "3"}, {"ops", "1.5"}, {"opshop", "1.5"}, {"opshop2", "1"}, {"oproll1", "1.6"}, {"oproll2", "6"},
            {"op_play", "9"}, {"op_abil", "0.35"}, {"op_abil2", "1.6"}, {"op_hit", "0.25"}, {"op_rin", "8"}, {"op_rinab", "0.5"},
            {"op_elka", "8"}, {"op_elkab", "0.12"}, {"op_valk", "8"}, {"op_low", "1.2"}, {"op_save", "0.4"}, {"op_die", "5"}, {"end", "0"}};

    /** 3.8.1: живая девчонка — все действия по очереди (крупные кадры вокруг неё). */
    public static final String[][] SEQ_OPSACT = {
            {"menu", "3"}, {"oa_start", "8"}, {"oa_wave", "1.2"}, {"oa_salute", "1.2"}, {"oa_pump", "1.2"}, {"oa_clap", "1.2"}, {"oa_cheer", "1.2"},
            {"oa_hop", "0.5"}, {"oa_duck", "1.2"}, {"oa_worry", "1.2"}, {"oa_spin", "0.6"}, {"oa_walk", "0.8"}, {"oa_run", "0.8"}, {"oa_sad", "1.5"}, {"end", "0"}};

    /** 3.9.0 (пункт 2): пропуск, вызов дня, рой сотни, невидимка, матка, мультикилл, лучший момент. */
    public static final String[][] SEQ_P2 = {
            {"menu", "3"}, {"p2_pass", "1.5"}, {"p2_chal", "1.5"}, {"p2_brief", "1"}, {"p2_play", "8"}, {"p2_swarm", "4"}, {"p2_swarm2", "3"},
            {"p2_new", "6"}, {"p2_multi", "0.35"}, {"p2_multi2", "4"}, {"p2_over", "4"}, {"p2_gif", "6"}, {"end", "0"}};

    /** Проверка объектов кампании: плотина, колонна, подстанция, радар — камера смотрит на объект. */
    public static final String[][] SEQ_OBJ = {
            {"menu", "3"}, {"o_dam", "3"}, {"o_convoy", "4"}, {"o_sub", "3"}, {"o_radar", "3"}, {"end", "0"}};

    /** 3.4.0: карты кампании — по миссии из каждой главы, 3 ракурса (прямо, влево, вправо). SN_MAPS=3-1,4-2 — только эти. */
    public static String[][] SEQ_MAPS = mapsSeq();
    float mapYaw;

    static String[][] mapsSeq() {
        String env = System.getenv("SN_MAPS");
        String[] ids = env != null ? env.split(",") : new String[]{"1-3", "2-2", "3-1", "4-1", "5-3", "6-2", "7-1", "8-1", "9-1", "10-1", "11-2"};
        ArrayList<String[]> out = new ArrayList<>();
        out.add(new String[]{"menu", "3"});
        for (String id : ids) for (int k = 0; k < 4; k++) out.add(new String[]{"m_" + id + "_" + k, k == 0 ? "3" : "1.5"});
        out.add(new String[]{"end", "0"});
        return out.toArray(new String[0][]);
    }

    /** Отладка: навести прицел на ближайшего врага этого типа. */
    void aimAt(String type) {
        Game.E best = null; float bd = 1e9f;
        for (Game.E e : game.enemies) if (e.type.equals(type) && e.pos.dst2(game.camPos) < bd) { bd = e.pos.dst2(game.camPos); best = e; }
        if (best == null) return;
        Vector3 d = new Vector3(best.pos).sub(game.camPos);
        game.aimYaw = MathUtils.atan2(-d.x, -d.z);
        game.aimPitch = MathUtils.clamp((float) Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)), -0.05f, 1.1f);
    }

    void lookObj() {
        if (game.obj == null) return;
        Vector3 d = new Vector3(game.obj.aim).sub(game.camPos);
        game.aimYaw = MathUtils.atan2(-d.x, -d.z);
        game.aimPitch = MathUtils.clamp((float) Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)), -0.05f, 0.6f);
    }

    Vector3 ahead(float d, float up) {
        Vector3 p = new Vector3(game.camPos).mulAdd(game.aimDir.cpy().set(game.aimDir.x, 0, game.aimDir.z).nor(), d);
        p.y = world.height(p.x, p.z) + up;
        return p;
    }

    void shots(float dt) {
        shotT += dt;
        String[][] seq = SEQ;
        if (seq == SEQ_OBJ && shotStep > 0 && game.obj != null) { botOn = false; game.firing = false; lookObj(); }
        if (seq == SEQ_MAPS && shotStep > 0) { botOn = false; game.firing = false; game.aimYaw = mapYaw; game.aimPitch = 0.07f; }
        if (shotStep >= seq.length) return;
        String name = seq[shotStep][0];
        float wait = Float.parseFloat(seq[shotStep][1]);
        if (shotT < wait) return;
        save(shotDir + "/" + name + ".png");
        if (ui.op.scrOk) Gdx.app.log("girl", name + " " + (int) (ui.op.scrX * draw.s) + " " + (int) (ui.op.scrFeet * draw.s) + " " + (int) (ui.op.scrH * draw.s));
        if (name.equals("menu") || name.equals("play1")) rend.dumpNext = true;
        Gdx.app.log("stat", name + " opq=" + rend.statOpq + " trn=" + rend.statTrn + " cast=" + rend.statCast + " tris=" + rend.statTris + " parts=" + FX.count() + " fps=" + Gdx.graphics.getFramesPerSecond());
        shotT = 0;
        shotStep++;
        if (shotStep >= seq.length) { Gdx.app.exit(); return; }
        String next = seq[shotStep][0];
        switch (next) {
            case "skin": G.SV.skVeh = "amethyst"; toMenuWorld(); ui.show("garage"); ui.garageTab = 1; break;
            case "daily": G.SV.dailyLast = G.today(-1); G.SV.dailyStreak = 2; ui.show("daily"); break;
            case "garage": ui.show("garage"); break;
            case "garage1": ui.garageTab = 1; break;
            case "shop": G.SV.c = 1200; ui.show("shop"); break;
            case "quests": game.ensureQuests(); G.SV.qList.get(0).p = G.SV.qList.get(0).n; G.SV.qList.get(0).done = true; ui.show("quests"); break;
            case "ach": ui.show("ach"); break;
            case "records": ui.show("records"); break;
            case "settings": ui.show("settings"); break;
            case "endless": ui.show("endless"); break;
            case "play1": G.SV.ab.put("salvo", true); G.SV.ab.put("smoke", true); ui.launch("endless"); break;
            case "boss": game.wave = 4; game.spawnEnemy("walker"); game.spawnEnemy("gunship"); break;
            case "pause": game.state = "pause"; ui.show("pause"); break;
            case "bshop": game.state = "shop"; ui.show("shop"); break;
            case "over": game.state = "run"; ui.show(""); forceDie(); break;
            case "campaign": G.SV.cuts.add("1"); G.SV.camp.put("1-1", 3); G.SV.camp.put("1-2", 2); ui.show("campaign"); break;
            case "cut": ui.debugCut(1); break;
            case "brief": ui.debugBrief("1-2"); break;
            case "chal": ui.show("chal"); break;
            case "m12": G.SV.cuts.add("2"); ui.debugLaunch("1-2"); break;
            case "m24": game.quitBattle(); ui.debugLaunch("2-4"); break;
            case "mwin": game.missionComplete(); break;
            case "m14": game.quitBattle(); ui.debugLaunch("1-4"); break;
            case "mfail": forceDie(); break;
            case "swarm": G.SV.c = 5000; ui.show("swarm"); break;
            case "s1": ui.show(""); startStrike(); break;
            case "s2": strike.camMode = 1; break;
            case "s3": strike.camMode = 0; break;
            case "spause": strike.pause(); ui.show("spause"); break;
            case "s4": strike.resume(); ui.show(""); break;
            case "sover": strike.endRun(); break;
            case "duel": ui.show("duel"); break;
            case "dhost": ui.debugDuelHost(); break;
            case "djoin": ui.debugDuelJoin("127.0.0.1"); break;
            case "dj2": duel.sendPick("gunship"); break;
            case "dh2": duel.begin(); break;
            case "ops": G.SV.c = 20000; G.SV.opOwn.add("liza"); G.SV.opOwn.add("akira"); G.SV.opOwn.add("rin"); G.SV.opSel = "akira";
                G.SV.opXp.put("akira", 900); G.SV.opXp.put("liza", 130); ui.show("ops"); break;
            case "opshop": ui.op.tab = 1; break;
            case "opshop2": G.SV.opAlt.add("akira"); break;
            case "oproll1": ui.op.tab = 0; ui.op.debugRoll(2); break;
            case "oproll2": break;
            case "op_play": G.SV.tut = true; G.SV.opSel = "akira"; ui.launch("endless"); break;
            case "op_abil": game.opCd = 0; game.useOp(); break;
            case "op_hit": game.hitTarget("base", 3f); break;
            case "op_rin": game.quitBattle(); G.SV.opSel = "rin"; ui.launch("endless"); break;
            case "op_rinab": game.opCd = 0; game.useOp(); break;
            case "op_elka": game.quitBattle(); G.SV.opOwn.add("elka"); G.SV.opSel = "elka"; ui.launch("endless"); break;
            case "op_elkab": game.opCd = 0; game.useOp(); break;
            case "op_valk": game.quitBattle(); G.SV.opOwn.add("valk"); G.SV.opSel = "valk"; G.SV.opXp.put("valk", 3800); G.SV.opAlt.add("valk"); ui.launch("endless"); break;
            case "op_low": game.hp = game.st("maxHp") * 0.2f; game.hitTarget("base", 1f); break;
            case "op_save": forceDie(); break;
            case "op_die": game.opInvT = 0; forceDie(); break;
            case "oa_start": G.SV.tut = true; G.SV.opOwn.add("akira"); G.SV.opSel = "akira"; G.SV.opXp.put("akira", 900); ui.launch("endless"); break;
            case "o_dam": G.SV.tut = true; ui.debugLaunch("2-1"); break;
            case "o_convoy": game.quitBattle(); ui.debugLaunch("2-4"); break;
            case "o_sub": game.quitBattle(); ui.debugLaunch("5-2"); break;
            case "o_radar": game.quitBattle(); ui.debugLaunch("4-2"); break;
            case "p2_pass": G.SV.bpXp = 1240; ui.show("pass"); break;
            case "p2_chal": ui.show("chal"); break;
            case "p2_brief": ui.debugBrief(Daily.id()); break;
            case "p2_play": G.SV.tut = true; ui.launch("endless"); break;
            case "p2_swarm": game.wave = 7; game.spawnSwarm(90); break;
            case "p2_swarm2": botOn = false; game.firing = false; aimAt("gnat"); break;
            case "p2_new": botOn = true; game.spawnEnemy("matka", ahead(160, 70)); game.spawnEnemy("phantom", ahead(200, 60)); for (int i = 0; i < 4; i++) game.spawnEnemy("quad", ahead(150, 50)); break;
            case "p2_multi": botOn = false; game.firing = false; aimAt("matka"); { int k = 0; for (Game.E e : new Array<>(game.enemies)) if (e.type.equals("quad") && k++ < 4) game.damageEnemy(e, 1e5f, null, "gun"); } break;
            case "p2_multi2": botOn = true; break;
            case "p2_over": game.state = "run"; ui.show(""); forceDie(); break;
            case "p2_gif": ui.debugSaveGif(); break;
            case "fx_fire": G.SV.night = System.getenv("SN_NIGHT") != null; G.SV.ab.put("salvo", true); ui.launch("endless"); break;
            case "fx_boom": botOn = false; game.firing = false; game.aimYaw = 0; game.aimPitch = 0.08f; game.explosion(ahead(55, 1.5f), 2f); break;
            case "fx_heat": game.heat = 0.97f; game.overheated = true; game.firing = false; game.aimPitch = 0.12f; break;
            case "fx_boss": { game.heat = 0; game.overheated = false; game.aimPitch = 0.25f;
                Game.E b = game.spawnEnemy("gunship", ahead(140, 60)); b.vel.set(0, 0, 0);
                for (int i = 0; i < 3; i++) game.step(1f / 30f);
                game.damageEnemy(b, 1e6f, b.pos.cpy(), "mis"); break; }
            case "fx_dusk": world.nightK = 0.45f; world.nightTgt = 0.45f; break;
            case "fx_night": world.nightK = 1f; world.nightTgt = 1f; break;
            case "fx_storm": world.stormK = 1f; world.stormTgt = 1f; world.wind = 30; break;
            case "fx_bolt": world.debugBolt(); break;
            case "fx_nboom": game.explosion(ahead(45, 2f), 2f); break;
            default:
                if (next.startsWith("oa_")) { if (next.equals("oa_sad")) game.opDebugLose(); ui.op.debugAct(next.substring(3)); break; }
                if (next.startsWith("st_")) {
                    G.SV.tut = true; if (shotStep > 1) game.quitBattle(); ui.debugLaunch(next.substring(3)); break;
                }
                if (next.startsWith("m_")) {
                    String[] p = next.split("_");
                    int k = Integer.parseInt(p[2]);
                    mapYaw = k == 0 || k == 3 ? 0f : (k == 1 ? 0.95f : -0.95f);
                    Game.debugCamUp = k == 3 ? 160f : 0f;
                    if (k == 0) { G.SV.tut = true; if (shotStep > 1) game.quitBattle(); ui.debugLaunch(p[1]); }
                }
                break;
        }
    }

    void studioShots(float dt) {
        shotT += dt;
        if (shotT < 0.5f) return;
        shotT = 0;
        Studio.Item it = studio.items.get(stIdx);
        save(shotDir + "/" + String.format("%02d", stIdx) + "_" + it.group + "_" + it.id + "_" + stView + ".png");
        if (stView == 0) {
            final int[] n = {0};
            studio.root.forEach(k -> { if (k.mesh != null) n[0] += k.mesh.count / 3; });
            Gdx.app.log("tris", it.id + " " + n[0]);
        }
        if (++stView > 2) {
            stView = 0;
            if (++stIdx >= studio.items.size) { Gdx.app.exit(); return; }
            studio.show(stIdx, world);
        }
    }

    void forceDie() { game.hp = 0.5f; game.hitTarget("base", 50f); }

    void save(String path) {
        int w = Gdx.graphics.getBackBufferWidth(), h = Gdx.graphics.getBackBufferHeight();
        byte[] px = ScreenUtils.getFrameBufferPixels(0, 0, w, h, true);
        for (int i = 3; i < px.length; i += 4) px[i] = (byte) 255; // непрозрачная альфа у каждого пикселя
        Pixmap p = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        BufferUtils.copy(px, 0, p.getPixels(), px.length);
        PixmapIO.writePNG(Gdx.files.absolute(path), p);
        p.dispose();
        Gdx.app.log("shot", path);
    }

    @Override
    public void dispose() {
        BlackBox.log("выход");
        BlackBox.stop();
        G.save();
        ui.kit.dispose();
        ui.op.dispose();
        draw.dispose();
        world.dispose();
        duel.stop();
        fpv.dispose();
        rend.dispose();
        replay.dispose();
        if (G.sfx != null) G.sfx.dispose();
    }
}
