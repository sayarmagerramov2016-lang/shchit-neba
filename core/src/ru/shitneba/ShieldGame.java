package ru.shitneba;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.Array.ArrayIterator;
import java.util.Locale;
import ru.shitneba.gfx.Dyn;
import ru.shitneba.gfx.FpvFx;
import ru.shitneba.gfx.MB;
import ru.shitneba.gfx.Node3;
import ru.shitneba.gfx.Renderer;
import ru.shitneba.ui.Draw;
import ru.shitneba.ui.UI;

public class ShieldGame extends ApplicationAdapter implements UI.Host, UI.StrikeHost {
   public static String shotDir;
   public static String forceVeh;
   Renderer rend;
   World world;
   Game game;
   UI ui;
   Draw draw;
   Strike strike;
   Duel duel;
   FpvFx fpv;
   PerspectiveCamera cam;
   final Array<Node3> roots = new Array();
   final Array<Dyn> dyns = new Array();
   float T;
   boolean botOn;
   int shotStep = 0;
   float shotT = 0.0F;
   float baseScale = 1.0F;
   float fpsAcc;
   int fpsN;
   Node3 warm;
   int warmFrames;
   int bbFrame;
   float botT;
   public static String[][] SEQ = new String[][]{
      {"menu", "3"},
      {"skin", "2"},
      {"daily", "1"},
      {"garage", "1"},
      {"garage1", "1"},
      {"shop", "1"},
      {"quests", "1"},
      {"ach", "1"},
      {"records", "1"},
      {"settings", "1"},
      {"endless", "1"},
      {"play1", "9"},
      {"play2", "14"},
      {"boss", "10"},
      {"pause", "1"},
      {"bshop", "1"},
      {"over", "5"},
      {"end", "0"}
   };
   public static final String[][] SEQ_CAMP = new String[][]{
      {"menu", "3"},
      {"campaign", "1"},
      {"cut", "1"},
      {"brief", "1"},
      {"chal", "1"},
      {"m12", "12"},
      {"m24", "12"},
      {"mwin", "4"},
      {"m14", "10"},
      {"mfail", "5"},
      {"end", "0"}
   };
   public static final String[][] SEQ_STRIKE = new String[][]{
      {"menu", "3"}, {"swarm", "1"}, {"s1", "6"}, {"s2", "8"}, {"s3", "10"}, {"spause", "1"}, {"s4", "3"}, {"sover", "4"}, {"end", "0"}
   };
   public static final String[][] SEQ_DUELHOST = new String[][]{{"menu", "3"}, {"duel", "1"}, {"dhost", "14"}, {"dh2", "6"}, {"dh3", "10"}, {"end", "0"}};
   public static final String[][] SEQ_DUELJOIN = new String[][]{{"menu", "5"}, {"djoin", "6"}, {"dj2", "4"}, {"dj3", "10"}, {"dj4", "8"}, {"end", "0"}};

   public void create() {
      G.init();
      BlackBox.start();
      BlackBox.hold = true;
      BlackBox.mark("загрузка");
      G.sfx = new Sfx();
      MB.hq = G.SV.gfx >= 2 && !G.isMobile();
      Models.initMaterials();
      this.rend = new Renderer();
      FX.init();
      SmokeTrail.init();
      this.cam = new PerspectiveCamera(62.0F, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
      this.cam.near = 0.3F;
      this.cam.far = 6000.0F;
      this.rend.cam = this.cam;
      this.world = new World(this.rend);
      this.game = new Game();
      this.game.world = this.world;
      this.game.cam = this.cam;
      this.draw = new Draw();
      this.ui = new UI(this.game, this.draw, this);
      this.game.ui = this.ui;
      this.roots.add(this.world.root);
      this.roots.add(this.game.root);
      this.strike = new Strike(this.game, this.world, this.cam);
      this.strike.ui = this.ui;
      this.ui.strike = this.strike;
      this.ui.strikeHost = this;
      this.roots.add(this.strike.root3);
      this.duel = new Duel(this.game);
      this.duel.ui = this.ui;
      this.game.duel = this.duel;
      this.ui.duel = this.duel;
      this.fpv = new FpvFx();
      if (forceVeh != null) {
         G.SV.veh = forceVeh;

         for (String v : G.VEHS) {
            if (!G.SV.vown.contains(v)) {
               G.SV.vown.add(v);
            }
         }
      }

      this.game.VEH = G.SV.veh;
      this.applyGfxNoRebuild();
      this.world.build(0, false, 0.0F, 0.0F, 0.0F, "", false);
      ArrayIterator var5 = this.world.stacks.iterator();

      while (var5.hasNext()) {
         Vector3 s = (Vector3)var5.next();
         FX.chimney(s);
      }

      this.game.applyStats();
      this.game.setVehicleModel();
      this.game.missiles = (int)this.game.st("maxMis");
      this.game.syncNoses();
      this.game.ensureQuests();
      Gdx.input.setInputProcessor(this.ui);
      Gdx.input.setCatchKey(4, true);
      this.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
      this.warmBuild();
      boolean showRep = shotDir == null || System.getenv("SN_REPORT") != null;
      String report = showRep ? BlackBox.takeReport() : null;
      if (report != null) {
         this.ui.showReport(report);
      } else if (shotDir == null) {
         this.ui.checkDaily();
      }

      if (shotDir != null) {
         this.botOn = true;
         G.SV.tut = true;
      }

      BlackBox.tick();
      BlackBox.hold = false;
   }

   void warmBuild() {
      this.warm = new Node3();
      int i = 0;

      for (JsonValue t = G.D.get("DT").child; t != null; i++) {
         for (String acc : new String[]{"harpy", "manti"}) {
            try {
               Models.EnemyModel m = Models.enemy(t.name, acc);
               m.g.pos.set(i % 6 * 0.05F, i / 6 * 0.05F, 0.0F);
               m.g.scl.set(0.004F, 0.004F, 0.004F);
               this.warm.add(m.g);
            } catch (Throwable var8) {
               Gdx.app.error("warm", t.name, var8);
            }
         }

         t = t.next;
      }

      Node3 ms = new Node3(Models.missileMesh(), Models.MAT_MISSILE);
      ms.scl.set(0.01F, 0.01F, 0.01F);
      this.warm.add(ms);
      Node3 fl = FX.flame(1.78F);
      fl.scl.set(0.01F, 0.01F, 0.01F);
      this.warm.add(fl);
      Models.Mog mg = Models.mog();
      mg.g.scl.set(0.01F, 0.01F, 0.01F);
      this.warm.add(mg.g);
      this.roots.add(this.warm);
      this.warmFrames = 3;
      this.draw.prewarm();
   }

   void warmStep() {
      if (this.warm != null) {
         this.cam.update();
         this.warm.pos.set(this.cam.position).mulAdd(this.cam.direction, 2.0F);
         if (--this.warmFrames <= 0) {
            this.roots.removeValue(this.warm, true);
            this.warm = null;
         }
      }
   }

   void applyGfxNoRebuild() {
      int q = G.SV.gfx;
      this.rend.shadows = q > 0;
      this.rend.shadowRange = q == 2 ? 90.0F : 70.0F;
      boolean mob = G.isMobile();
      this.rend.shadowEvery = mob && q < 2 ? 2 : 1;
      float[] lvl = new float[]{0.6F, 0.8F, 1.0F};
      this.baseScale = lvl[q];
      if (mob) {
         float[] maxH = new float[]{540.0F, 720.0F, 1080.0F};
         int bh = Math.max(1, Gdx.graphics.getBackBufferHeight());
         this.baseScale = Math.min(this.baseScale, maxH[q] / bh);
      }

      this.baseScale = MathUtils.clamp(this.baseScale, 0.35F, 1.0F);
      String fs = System.getenv("SN_SCALE");
      if (fs != null) {
         this.baseScale = Float.parseFloat(fs);
      }

      this.rend.scale = this.baseScale;
   }

   void dynScale(float delta) {
      this.fpsAcc += delta;
      this.fpsN++;
      if (!(this.fpsAcc < 2.0F)) {
         float fps = this.fpsN / this.fpsAcc;
         this.fpsAcc = 0.0F;
         this.fpsN = 0;
         float lo = Math.max(0.35F, this.baseScale * 0.6F);
         if (fps < 45.0F && this.rend.scale > lo) {
            this.rend.scale = Math.max(lo, this.rend.scale - 0.1F);
         } else if (fps > 57.0F && this.rend.scale < this.baseScale) {
            this.rend.scale = Math.min(this.baseScale, this.rend.scale + 0.05F);
         }
      }
   }

   @Override
   public void applyGfx() {
      this.applyGfxNoRebuild();
      if (this.game.state.equals("menu")) {
         this.toMenuWorld();
      }
   }

   @Override
   public void toMenuWorld() {
      FX.clear();
      this.world.build(0, false, 0.0F, 0.0F, 0.0F, "", false);
      ArrayIterator var1 = this.world.stacks.iterator();

      while (var1.hasNext()) {
         Vector3 s = (Vector3)var1.next();
         FX.chimney(s);
      }

      this.game.VEH = G.SV.veh;
      this.game.applyStats();
      this.game.setVehicleModel();
      this.game.missiles = (int)this.game.st("maxMis");
      this.game.syncNoses();
   }

   @Override
   public void toggleFullscreen() {
      if (Gdx.graphics.isFullscreen()) {
         Gdx.graphics.setWindowedMode(1280, 720);
      } else {
         Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
      }
   }

   @Override
   public boolean isFullscreen() {
      return Gdx.graphics.isFullscreen();
   }

   @Override
   public boolean bot() {
      return this.botOn;
   }

   @Override
   public void startStrike() {
      this.strike.bot = this.botOn;
      this.strike.start();
   }

   @Override
   public void exitStrike(boolean hangar) {
      this.strike.exit();
      this.ui.show("");
      this.toMenuWorld();
      this.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
      this.ui.show(hangar ? "swarm" : "menu");
   }

   public void resize(int w, int h) {
      if (w > 0 && h > 0) {
         this.cam.viewportWidth = w;
         this.cam.viewportHeight = h;
         this.cam.fieldOfView = (float)w / Math.max(1, h) < 1.5F ? 70.0F : 62.0F;
         this.cam.update();
         this.draw.resize();
         this.applyGfxNoRebuild();
      }
   }

   public void pause() {
      BlackBox.hold = true;
      BlackBox.log("свёрнута");
      this.ui.onPause();
      G.save();
   }

   public void resume() {
      BlackBox.tick();
      BlackBox.hold = false;
      BlackBox.log("развёрнута");
   }

   void bbInfo() {
      if (++this.bbFrame % 15 == 0) {
         BlackBox.info = this.game.mode
            + " · "
            + this.game.state
            + " · панель «"
            + this.ui.panel
            + "» · волна "
            + this.game.wave
            + " · врагов "
            + this.game.enemies.size
            + " · частиц "
            + FX.count()
            + " · кадров/с "
            + Gdx.graphics.getFramesPerSecond()
            + " · масштаб "
            + String.format(Locale.US, "%.2f", this.rend.scale)
            + (this.strike.active ? " · рой" : "")
            + " · машина "
            + this.game.VEH;
      }
   }

   public void render() {
      BlackBox.tick();
      BlackBox.mark("кадр");
      this.bbInfo();
      String tst = System.getenv("SN_TEST");
      if (tst != null && this.T > 4.0F && this.T < 4.2F) {
         if (tst.equals("crash")) {
            throw new IllegalStateException("тестовый вылет чёрного ящика");
         }

         if (tst.equals("freeze")) {
            BlackBox.mark("тест: зависание");

            try {
               Thread.sleep(4000L);
            } catch (InterruptedException var4) {
            }

            this.T = 5.0F;
         }
      }

      float dt = Math.min(Gdx.graphics.getDeltaTime(), 0.05F);
      if (shotDir != null) {
         dt = 0.033333335F;
      }

      this.T += dt;
      if (shotDir == null) {
         this.dynScale(Gdx.graphics.getDeltaTime());
      } else if ("lowres".equals(System.getenv("SN_TEST"))) {
         this.rend.scale = 0.6F;
      }

      if (this.botOn && !this.strike.active) {
         this.bot(dt);
      }

      BlackBox.mark("дуэль: сеть");
      this.duel.update(dt);
      BlackBox.mark("интерфейс: обновление");
      this.ui.update(dt);
      BlackBox.mark("бой");
      this.game.step(dt);
      BlackBox.mark("рой / камера");
      if (this.strike.active) {
         this.strike.update(dt);
      } else {
         this.game.updateCamera(dt);
      }

      this.warmStep();
      BlackBox.mark("мир");
      this.world.update(dt, this.cam);
      this.game.updateSpot();
      SmokeTrail.wind = this.world.wind;
      BlackBox.mark("эффекты");
      FX.update(dt, this.cam);
      FX.fill(this.cam);
      this.game.fillDyn();
      Vector3 f = this.rend
         .shadowFocus
         .set(this.cam.direction.x, 0.0F, this.cam.direction.z)
         .nor()
         .scl(this.game.state.equals("menu") ? 0.0F : 22.0F)
         .add(this.cam.position.x, 0.0F, this.cam.position.z);
      if (this.game.state.equals("menu")) {
         f.set(0.0F, 0.0F, 0.0F);
      }

      this.dyns.clear();
      this.dyns.add(this.world.cloudDyn);
      this.dyns.add(FX.dDebris);
      this.dyns.add(SmokeTrail.dyn);
      this.dyns.add(FX.dSmoke);
      this.dyns.add(this.game.tracers);
      this.dyns.add(this.game.beamDyn);
      this.dyns.add(this.strike.tracers);
      this.dyns.add(this.strike.beamDyn);
      this.dyns.add(FX.dAdd);
      this.dyns.add(this.world.rainDyn);
      if (this.strike.active) {
         this.rend.shadowFocus.set(this.cam.position.x, 0.0F, this.cam.position.z);
      }

      BlackBox.mark("рендер кадра");
      this.rend.render(this.roots, this.dyns);
      if (this.strike.active && this.ui.panel.isEmpty()) {
         this.fpv.draw(this.strike.noise(), this.strike.T, this.strike.hitT, this.strike.camMode == 0 && this.strike.st.equals("fly") ? 1.0F : 0.0F);
      }

      BlackBox.mark("отрисовка интерфейса");
      this.ui.render();
      BlackBox.mark("кадр");
      if (shotDir != null) {
         this.shots(dt);
      }
   }

   void bot(float dt) {
      if (!this.game.state.equals("run")) {
         this.game.firing = false;
      } else {
         this.botT += dt;
         Game.E best = null;
         float bd = 1.0E9F;
         ArrayIterator lp = this.game.enemies.iterator();

         while (lp.hasNext()) {
            Game.E e = (Game.E)lp.next();
            if (e.cloak == 0 && !e.type.equals("shell") && !e.type.equals("node")) {
               float dd = e.pos.dst2(this.game.camPos);
               if (dd < bd) {
                  bd = dd;
                  best = e;
               }
            }
         }

         if (best == null) {
            this.game.firing = false;
         } else {
            Vector3 lpx = this.game.leadPoint(best, new Vector3()).sub(this.game.camPos);
            float yaw = MathUtils.atan2(-lpx.x, -lpx.z);
            float pitch = MathUtils.clamp((float)Math.atan2(lpx.y, Math.sqrt(lpx.x * lpx.x + lpx.z * lpx.z)), -0.06F, 1.2F);
            float dy = yaw - this.game.aimYaw;

            while (dy > (float) Math.PI) {
               dy -= (float) (Math.PI * 2);
            }

            while (dy < (float) -Math.PI) {
               dy += (float) (Math.PI * 2);
            }

            this.game.aimYaw = this.game.aimYaw + dy * Math.min(1.0F, dt * 6.0F);
            this.game.aimPitch = this.game.aimPitch + (pitch - this.game.aimPitch) * Math.min(1.0F, dt * 6.0F);
            this.game.firing = Math.abs(dy) < 0.08F && !this.game.overheated;
            if (this.game.lock.t != null && this.game.lock.k >= 1.0F && this.game.missiles > 0 && this.botT % 1.5F < dt) {
               this.game.fireMissile("lock", false);
            }
         }
      }
   }

   void shots(float dt) {
      this.shotT += dt;
      String[][] seq = SEQ;
      if (this.shotStep < seq.length) {
         String name = seq[this.shotStep][0];
         float wait = Float.parseFloat(seq[this.shotStep][1]);
         if (!(this.shotT < wait)) {
            this.save(shotDir + "/" + name + ".png");
            if (name.equals("menu") || name.equals("play1")) {
               this.rend.dumpNext = true;
            }

            Gdx.app
               .log(
                  "stat",
                  name
                     + " opq="
                     + this.rend.statOpq
                     + " trn="
                     + this.rend.statTrn
                     + " cast="
                     + this.rend.statCast
                     + " tris="
                     + this.rend.statTris
                     + " parts="
                     + FX.count()
                     + " fps="
                     + Gdx.graphics.getFramesPerSecond()
               );
            this.shotT = 0.0F;
            this.shotStep++;
            if (this.shotStep >= seq.length) {
               Gdx.app.exit();
            } else {
               String next = seq[this.shotStep][0];
               switch (next) {
                  case "skin":
                     G.SV.skVeh = "amethyst";
                     this.toMenuWorld();
                     this.ui.show("garage");
                     this.ui.garageTab = 1;
                     break;
                  case "daily":
                     G.SV.dailyLast = G.today(-1);
                     G.SV.dailyStreak = 2;
                     this.ui.show("daily");
                     break;
                  case "garage":
                     this.ui.show("garage");
                     break;
                  case "garage1":
                     this.ui.garageTab = 1;
                     break;
                  case "shop":
                     G.SV.c = 1200;
                     this.ui.show("shop");
                     break;
                  case "quests":
                     this.game.ensureQuests();
                     G.SV.qList.get(0).p = G.SV.qList.get(0).n;
                     G.SV.qList.get(0).done = true;
                     this.ui.show("quests");
                     break;
                  case "ach":
                     this.ui.show("ach");
                     break;
                  case "records":
                     this.ui.show("records");
                     break;
                  case "settings":
                     this.ui.show("settings");
                     break;
                  case "endless":
                     this.ui.show("endless");
                     break;
                  case "play1":
                     G.SV.ab.put("salvo", true);
                     G.SV.ab.put("smoke", true);
                     this.ui.launch("endless");
                     break;
                  case "boss":
                     this.game.wave = 4;
                     this.game.spawnEnemy("walker");
                     this.game.spawnEnemy("gunship");
                     break;
                  case "pause":
                     this.game.state = "pause";
                     this.ui.show("pause");
                     break;
                  case "bshop":
                     this.game.state = "shop";
                     this.ui.show("shop");
                     break;
                  case "over":
                     this.game.state = "run";
                     this.ui.show("");
                     this.forceDie();
                     break;
                  case "campaign":
                     G.SV.cuts.add("1");
                     G.SV.camp.put("1-1", 3);
                     G.SV.camp.put("1-2", 2);
                     this.ui.show("campaign");
                     break;
                  case "cut":
                     this.ui.debugCut(1);
                     break;
                  case "brief":
                     this.ui.debugBrief("1-2");
                     break;
                  case "chal":
                     this.ui.show("chal");
                     break;
                  case "m12":
                     G.SV.cuts.add("2");
                     this.ui.debugLaunch("1-2");
                     break;
                  case "m24":
                     this.game.quitBattle();
                     this.ui.debugLaunch("2-4");
                     break;
                  case "mwin":
                     this.game.missionComplete();
                     break;
                  case "m14":
                     this.game.quitBattle();
                     this.ui.debugLaunch("1-4");
                     break;
                  case "mfail":
                     this.forceDie();
                     break;
                  case "swarm":
                     G.SV.c = 5000;
                     this.ui.show("swarm");
                     break;
                  case "s1":
                     this.ui.show("");
                     this.startStrike();
                     break;
                  case "s2":
                     this.strike.camMode = 1;
                     break;
                  case "s3":
                     this.strike.camMode = 0;
                     break;
                  case "spause":
                     this.strike.pause();
                     this.ui.show("spause");
                     break;
                  case "s4":
                     this.strike.resume();
                     this.ui.show("");
                     break;
                  case "sover":
                     this.strike.endRun();
                     break;
                  case "duel":
                     this.ui.show("duel");
                     break;
                  case "dhost":
                     this.ui.debugDuelHost();
                     break;
                  case "djoin":
                     this.ui.debugDuelJoin("127.0.0.1");
                     break;
                  case "dj2":
                     this.duel.sendPick("gunship");
                     break;
                  case "dh2":
                     this.duel.begin();
               }
            }
         }
      }
   }

   void forceDie() {
      this.game.hp = 0.5F;
      this.game.hitTarget("base", 50.0F);
   }

   void save(String path) {
      int w = Gdx.graphics.getBackBufferWidth();
      int h = Gdx.graphics.getBackBufferHeight();
      byte[] px = ScreenUtils.getFrameBufferPixels(0, 0, w, h, true);

      for (int i = 4; i < px.length; i += 4) {
         px[i - 1] = -1;
      }

      Pixmap p = new Pixmap(w, h, Format.RGBA8888);
      BufferUtils.copy(px, 0, p.getPixels(), px.length);
      PixmapIO.writePNG(Gdx.files.absolute(path), p);
      p.dispose();
      Gdx.app.log("shot", path);
   }

   public void dispose() {
      BlackBox.log("выход");
      BlackBox.stop();
      G.save();
      this.draw.dispose();
      this.duel.stop();
      this.fpv.dispose();
      this.rend.dispose();
      if (G.sfx != null) {
         G.sfx.dispose();
      }
   }
}
