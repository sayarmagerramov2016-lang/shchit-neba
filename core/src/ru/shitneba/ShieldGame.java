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
import com.badlogic.gdx.utils.ScreenUtils;

import ru.shitneba.gfx.Dyn;
import ru.shitneba.gfx.MB;
import ru.shitneba.gfx.Node3;
import ru.shitneba.gfx.Renderer;
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
    ru.shitneba.gfx.FpvFx fpv;
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
        MB.hq = G.SV.gfx >= 2 && !G.isMobile();
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
        duel = new Duel(game);
        duel.ui = ui; game.duel = duel; ui.duel = duel;
        fpv = new ru.shitneba.gfx.FpvFx();
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
        for (com.badlogic.gdx.utils.JsonValue t = G.D.get("DT").child; t != null; t = t.next, i++) {
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
        String tst = System.getenv("SN_TEST");
        if (tst != null && T > 4f && T < 4.2f) {
            if (tst.equals("crash")) throw new IllegalStateException("тестовый вылет чёрного ящика");
            if (tst.equals("freeze")) { BlackBox.mark("тест: зависание"); try { Thread.sleep(4000); } catch (InterruptedException ignored) { } T = 5f; }
        }
        float dt = Math.min(Gdx.graphics.getDeltaTime(), 0.05f);
        if (shotDir != null) dt = 1f / 30f;
        T += dt;
        if (shotDir == null) dynScale(Gdx.graphics.getDeltaTime());
        else if ("lowres".equals(System.getenv("SN_TEST"))) rend.scale = 0.6f;
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
        rend.render(roots, dyns);
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

    /** Проверка «сочности»: выстрелы, гильзы, взрывы, облёт босса, ночь и гроза. */
    public static final String[][] SEQ_FX = {
            {"menu", "3"}, {"endless", "1"}, {"fx_fire", "6"}, {"fx_boom", "0.17"}, {"fx_boom2", "0.6"}, {"fx_fires", "2.5"}, {"fx_heat", "0.6"},
            {"fx_boss", "1.3"}, {"fx_boss2", "1.2"}, {"fx_after", "2"}, {"fx_dusk", "1"}, {"fx_night", "1.5"}, {"fx_storm", "1.2"}, {"fx_bolt", "0.1"},
            {"fx_nboom", "0.15"}, {"end", "0"}};

    Vector3 ahead(float d, float up) {
        Vector3 p = new Vector3(game.camPos).mulAdd(game.aimDir.cpy().set(game.aimDir.x, 0, game.aimDir.z).nor(), d);
        p.y = world.height(p.x, p.z) + up;
        return p;
    }

    void shots(float dt) {
        shotT += dt;
        String[][] seq = SEQ;
        if (shotStep >= seq.length) return;
        String name = seq[shotStep][0];
        float wait = Float.parseFloat(seq[shotStep][1]);
        if (shotT < wait) return;
        save(shotDir + "/" + name + ".png");
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
            case "fx_bolt": world.debugBolt(cam.position); break;
            case "fx_nboom": game.explosion(ahead(45, 2f), 2f); break;
            default: break;
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
        for (int i = 4; i < px.length; i += 4) px[i - 1] = (byte) 255;
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
        draw.dispose();
        duel.stop();
        fpv.dispose();
        rend.dispose();
        if (G.sfx != null) G.sfx.dispose();
    }
}
