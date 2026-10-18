package ru.shitneba;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.Array.ArrayIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import ru.shitneba.gfx.Dyn;
import ru.shitneba.gfx.MB;
import ru.shitneba.gfx.Mat;
import ru.shitneba.gfx.Mesh3;
import ru.shitneba.gfx.Node3;

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
   public Game.Run RUN;
   public JsonValue dif;
   public float difSpd = 1.0F;
   public float difDmg = 1.0F;
   public float difHp = 1.0F;
   public float difCoin = 1.0F;
   public int score;
   public int wave;
   public int kills;
   public int combo;
   public int runCoins;
   public float hp = 100.0F;
   public float heat;
   public float comboT;
   public boolean overheated;
   public float aimYaw = 0.0F;
   public float aimPitch = 0.2F;
   public boolean firing;
   float fireT;
   int barrelI;
   public int missiles = 4;
   public float reloadT;
   public float chanT;
   public float shake;
   public float hurtT;
   int toSpawn;
   float spawnT;
   float spawnInt = 1.3F;
   public float between;
   float deadT;
   public final Array<Game.E> bosses = new Array();
   float missionTime;
   boolean waveHit;
   public final Map<String, Boolean> abUsed = new LinkedHashMap<>();
   public final Map<String, Float> abCd = new LinkedHashMap<>();
   public float smokeT;
   public float repairT;
   public float empT;
   int scoutQ;
   float scoutT;
   int squads;
   float squadT;
   boolean bossWave;
   boolean bossSpawned;
   int bossAt;
   int spawned;
   int nohitWaves;
   public final Array<Models.Mog> mogs = new Array();
   public int tut = -1;
   float tutT;
   int wxRoll;
   float burnT;
   float wreckCam;
   public final Array<Game.E> enemies = new Array();
   final Array<Game.Bullet> bullets = new Array();
   final Array<Game.Rocket> rockets = new Array();
   final Array<Game.Ebul> ebul = new Array();
   public final Game.Lock lock = new Game.Lock();
   public final Vector3 aimDir = new Vector3(0.0F, 0.0F, -1.0F);
   public final Vector3 camPos = new Vector3();
   public final Vector3 aimPoint = new Vector3();
   public final Color trc = new Color(1.0F, 0.85F, 0.45F, 1.0F);
   public static final Vector3 AIM = new Vector3(0.0F, 2.6F, 0.6F);
   public final Array<Game.DmgNum> dmgNums = new Array();
   public float hitMark;
   final Array<Game.Wreck> wrecks = new Array();
   public boolean nightRun;
   public int xpGot;
   public int rankUp = -1;
   int nextId = 1;
   float boltT;
   final Array<float[]> bolts = new Array();
   final Array<float[]> beams = new Array();
   final Array<Game.Timer> timers = new Array();
   final Random R = new Random();
   public final Dyn tracers;
   public final Dyn beamDyn;
   public static final float BUL_SP = 440.0F;
   static final String[] GROUND_T = new String[]{"inf", "rpg", "mech", "mortar", "walker", "colossus"};
   public float T;
   public JsonValue CM;
   public Game.Mission obj;
   public float missionT;
   public float missionTime2;
   float timeSaid;
   JsonValue squadMix;
   public boolean resWin;
   public boolean[] resOk;
   public int resGot;
   public int resReward;
   public String resReason = "";
   public JsonValue resM;
   static Mat FLASH_MAT;
   static Mat CHAR_MAT;
   static Mat SHIELD_MAT;
   public Duel duel;
   public boolean netView;
   public String duelRole = "";
   public float duelT;
   public float bossSpecT;
   public float bossCamPitch = 0.25F;
   public final float[] bossCd = new float[3];
   public final Game.BossIn bossIn = new Game.BossIn();
   public int netMis;
   public final int[] netAb = new int[4];
   public Game.E bossE;
   public static final float DUEL_TIME = 180.0F;
   public static final String[] DUEL_BOSSES = new String[]{"boss", "gunship", "walker", "carrier", "ghost", "hydra", "chimera", "storm", "manta", "colossus"};
   public static final float CD_F1 = 4.0F;
   public static final float CD_F2 = 15.0F;
   static final Map<String, Float> DUEL_SPD = new LinkedHashMap<>();
   static final Map<String, Float> SPEC_CD = new LinkedHashMap<>();
   static final Map<String, Float> DUEL_HP = new LinkedHashMap<>();
   public static final Map<String, String> SPEC_SHORT = new LinkedHashMap<>();
   public static final Map<String, String> SPEC_NAME = new LinkedHashMap<>();
   static Mat SHIELD_OVER;
   final Vector3 tmpV = new Vector3();
   final Vector3 tA = new Vector3();
   final Vector3 tB = new Vector3();
   final Vector3 tC = new Vector3();
   final Matrix4 tm = new Matrix4();
   static final Color MOGC;
   static Mesh3 domeMesh;
   int beamUsed;
   static final String[] TUT;
   static final String[] TUT_PC;

   public boolean hasMod(String m) {
      if (this.CM != null && this.CM.has("mods")) {
         for (JsonValue v = this.CM.get("mods").child; v != null; v = v.next) {
            if (v.asString().equals(m)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public boolean isCamp() {
      return this.mode.equals("campaign") && this.CM != null;
   }

   public Game() {
      this.root.tag = FX.ROOT_TAG;
      Mat tm = new Mat();
      tm.unshaded = true;
      tm.vcol = true;
      tm.blend = 2;
      tm.depthWrite = false;
      tm.cull = false;
      tm.fog = false;
      tm.order = 4;
      this.tracers = new Dyn(tm);
      Mat bm = new Mat();
      bm.unshaded = true;
      bm.vcol = true;
      bm.blend = 2;
      bm.depthWrite = false;
      bm.cull = false;
      bm.order = 4;
      this.beamDyn = new Dyn(bm);
      ensureOverlays();

      for (String k : new String[]{"salvo", "emp"}) {
         this.abUsed.put(k, false);
      }

      for (String k : new String[]{"smoke", "repair"}) {
         this.abCd.put(k, 0.0F);
      }
   }

   static void ensureOverlays() {
      if (CHAR_MAT == null) {
         CHAR_MAT = new Mat();
         CHAR_MAT.unshaded = true;
         CHAR_MAT.vcol = false;
         CHAR_MAT.color.set(0.05F, 0.04F, 0.03F, 0.55F);
         CHAR_MAT.blend = 1;
         CHAR_MAT.depthWrite = false;
      }

      if (FLASH_MAT == null) {
         FLASH_MAT = Mat.unshadedAdd(new Color(1.0F, 0.95F, 0.85F, 0.6F));
         FLASH_MAT.cull = true;
      }

      if (SHIELD_MAT == null) {
         SHIELD_MAT = Mat.unshadedAdd(new Color(0.35F, 0.7F, 1.0F, 0.22F));
      }
   }

   float rr(float a, float b) {
      return a + this.R.nextFloat() * (b - a);
   }

   void later(float t, Runnable r) {
      Game.Timer tm = new Game.Timer();
      tm.t = t;
      tm.r = r;
      this.timers.add(tm);
   }

   public boolean runWallet() {
      return (this.mode.equals("endless") || this.mode.equals("duel")) && this.RUN != null;
   }

   public static float specCd(String t) {
      Float v = SPEC_CD.get(t);
      return v == null ? 15.0F : v;
   }

   void push(Object... ev) {
      if (this.duel != null && this.mode.equals("duel")) {
         this.duel.pushEvent(ev);
      }
   }

   public void startDuel(String boss, String veh, int theme) {
      this.clearBattle();
      this.mode = "duel";
      this.CM = null;
      this.VEH = veh;
      this.RUN = new Game.Run();
      this.RUN.c = 0;
      this.RUN.up = G.zeroUp();
      this.RUN.up.put("armor", 3);
      this.RUN.up.put("cool", 1);
      this.RUN.ab = new LinkedHashMap<>();

      for (String k : new String[]{"salvo", "emp", "smoke", "repair"}) {
         this.RUN.ab.put(k, true);
      }

      this.dif = G.D.get("DIFF").get("normal");
      this.difSpd = 1.0F;
      this.difDmg = 0.6F;
      this.difHp = 1.0F;
      this.difCoin = 1.0F;
      this.nightRun = false;
      this.applyStats();
      this.setVehicleModel();
      JsonValue tr = G.D.get("SK_TR").get(G.SV.skTr);
      if (tr == null) {
         tr = G.D.get("SK_TR").get("gold");
      }

      this.trc.set(G.hex(tr.getString("col")));
      this.world.build(theme, false, 0.0F, 0.0F, 0.0F, "", false);
      ArrayIterator var9 = this.world.stacks.iterator();

      while (var9.hasNext()) {
         Vector3 s = (Vector3)var9.next();
         FX.chimney(s);
      }

      this.obj = null;
      this.setupMogs();
      this.score = 0;
      this.wave = 1;
      this.hp = this.st("maxHp");
      this.heat = 0.0F;
      this.overheated = false;
      this.kills = 0;
      this.combo = 0;
      this.runCoins = 0;
      this.missiles = (int)this.st("maxMis");
      this.reloadT = 0.0F;
      this.chanT = 0.0F;
      this.firing = false;
      this.aimYaw = 0.0F;
      this.aimPitch = 0.2F;
      this.toSpawn = 0;
      this.between = 0.0F;
      this.bossWave = false;
      this.missionTime = 0.0F;
      this.abUsed.put("salvo", false);
      this.abUsed.put("emp", false);
      this.abCd.put("smoke", 0.0F);
      this.abCd.put("repair", 0.0F);
      this.smokeT = 0.0F;
      this.repairT = 0.0F;
      this.empT = 0.0F;
      this.scoutQ = 0;
      this.squads = 0;
      this.nohitWaves = 0;
      this.duelT = 180.0F;
      this.bossCd[0] = 3.0F;
      this.bossCd[1] = 6.0F;
      this.bossCd[2] = 8.0F;
      this.bossSpecT = 0.0F;
      this.bossCamPitch = 0.25F;
      this.bossIn.mx = 0.0F;
      this.bossIn.my = 0.0F;
      this.bossIn.mz = 0.0F;
      this.bossIn.cy = (float) Math.PI;
      this.bossIn.f1 = 0;
      this.bossIn.f2 = 0;
      this.bossIn.sp = 0;
      this.netMis = 0;

      for (int i = 0; i < 4; i++) {
         this.netAb[i] = 0;
      }

      this.tut = -1;
      this.bossE = null;
      if (!this.netView) {
         Game.E b = this.spawnEnemy(boss, null, 0.0F, 600.0F);
         b.pc = true;
         Float h = DUEL_HP.get(boss);
         b.hp = h == null ? 180.0F : h;
         b.max = b.hp;
         if (boss.equals("ghost")) {
            b.cloak = 0;
         }

         this.bossE = b;
         this.bossIn.cy = MathUtils.atan2(b.pos.x, b.pos.z);
      }

      this.state = "run";
      this.syncNoses();
      if (this.ui != null) {
         this.ui.banner("Дуэль", true, "Вы — " + (this.duelRole.equals("pvo") ? "ПВО: сбейте босса за 3 минуты" : "босс: разрушьте комплекс"));
      }

      G.sfx("boss");
   }

   public Game.E duelBoss() {
      if (this.bossE != null) {
         return this.bossE;
      } else {
         ArrayIterator var1 = this.bosses.iterator();
         return var1.hasNext() ? (Game.E)var1.next() : null;
      }
   }

   void duelRules(float dt) {
      this.duelT = Math.max(0.0F, this.duelT - dt);

      for (int i = 0; i < 3; i++) {
         this.bossCd[i] = Math.max(0.0F, this.bossCd[i] - dt);
      }

      this.bossSpecT = Math.max(0.0F, this.bossSpecT - dt);
      if (this.bossE != null && this.bossSpecT <= 0.0F && this.bossE.pcInv) {
         this.bossE.inv = false;
         this.bossE.pcInv = false;
         this.bossE.cloak = 0;
         if (this.bossE.type.equals("colossus")) {
            this.shieldVis(this.bossE, false);
         }

         this.bossE.md.body.alpha = 1.0F;
         this.bossE.md.body.overlay = null;
      }

      if (this.bossE == null || !(this.bossE.hp <= 0.0F) && this.enemies.contains(this.bossE, true)) {
         if (this.duelT <= 0.0F && this.duel != null) {
            this.duel.finishHost("pvo", "Время вышло — оборона выстояла");
         }
      } else {
         if (this.duel != null) {
            this.duel.finishHost("pvo", "Босс сбит!");
         }
      }
   }

   Vector3 pcBoss(Game.E e, float dt, Vector3 to) {
      Game.BossIn b = this.bossIn;
      float cy = b.cy;
      float fx = -MathUtils.sin(cy);
      float fz = -MathUtils.cos(cy);
      float rx = MathUtils.cos(cy);
      float rz = -MathUtils.sin(cy);
      Float sp0 = DUEL_SPD.get(e.type);
      float spd = (sp0 == null ? 28.0F : sp0)
         * ((e.type.equals("gunship") || e.type.equals("manta")) && this.bossSpecT > 0.0F ? 2.2F : 1.0F)
         * (this.empT > 0.0F ? 0.45F : 1.0F);
      Vector3 mv = new Vector3(b.mx, b.my, b.mz);
      if (mv.len() > 1.0F) {
         mv.nor();
      }

      Vector3 want = new Vector3(fx * mv.z + rx * mv.x, 0.0F, fz * mv.z + rz * mv.x).scl(spd);
      float gh = this.height(e.pos.x, e.pos.z);
      boolean walk = e.type.equals("walker") || e.type.equals("colossus");
      if (walk) {
         want.y = 0.0F;
         e.anim = e.anim + dt * want.len() * 0.15F;
         if (e.md.legs.size > 0) {
            float s = MathUtils.sin(e.anim) * 0.45F;
            ((Node3)e.md.legs.get(0)).setEuler(s, 0.0F, 0.0F);
            ((Node3)e.md.legs.get(1)).setEuler(-s, 0.0F, 0.0F);
         }
      } else {
         want.y = mv.y * spd * 0.7F;
         float alt = e.pos.y - gh;
         if (alt < 30.0F) {
            want.y = Math.max(want.y, (30.0F - alt) * 1.5F);
         }

         if (alt > 280.0F) {
            want.y = Math.min(want.y, (280.0F - alt) * 1.5F);
         }
      }

      float hp2 = (float)Math.sqrt(e.pos.x * e.pos.x + e.pos.z * e.pos.z);
      if (hp2 > 750.0F) {
         want.add(this.tA.set(-e.pos.x, 0.0F, -e.pos.z).nor().scl((hp2 - 750.0F) * 0.8F));
      }

      if (hp2 < 45.0F) {
         want.add(this.tA.set(e.pos.x, 0.0F, e.pos.z).nor().scl((45.0F - hp2) * 2.0F));
      }

      e.vel.lerp(want, 1.0F - (float)Math.exp(-dt * 2.0F));
      if (e.type.equals("walker")) {
         e.vel.y = 0.0F;
         e.pos.y = gh + 8.4F * e.g.scl.y;
      }

      if (e.type.equals("colossus")) {
         e.vel.y = 0.0F;
         e.pos.y = gh;
      }

      if (b.f1 > 0) {
         b.f1 = 0;
         if (this.bossCd[0] <= 0.0F) {
            this.bossCd[0] = 4.0F * (!e.type.equals("hydra") && !e.type.equals("gunship") ? 1.0F : 0.85F);
            int n = e.type.equals("gunship") ? 4 : 3;

            for (int k = 0; k < n; k++) {
               this.later(k * 0.2F, () -> {
                  if (e.hp > 0.0F && this.enemies.contains(e, true)) {
                     this.enemyShot(e, "erk", 66.0F);
                  }
               });
            }

            if (walk) {
               this.enemyShot(e.pos.cpy().add(0.0F, 12.0F, 0.0F), "base", "shell", 0.0F);
            }

            if (e.type.equals("manta")) {
               for (int k = 0; k < 3; k++) {
                  this.later(k * 0.25F, () -> {
                     if (e.hp > 0.0F && this.enemies.contains(e, true)) {
                        this.enemyShot(e, "shell", 0.0F);
                     }
                  });
               }
            }
         }
      }

      if (b.f2 > 0) {
         b.f2 = 0;
         if (this.bossCd[1] <= 0.0F && this.empT <= 0.0F) {
            this.bossCd[1] = 15.0F;
            this.pcSwarm(e);
         }
      }

      if (b.sp > 0) {
         b.sp = 0;
         if (this.bossCd[2] <= 0.0F) {
            this.bossCd[2] = specCd(e.type);
            this.pcSpecial(e);
         }
      }

      if (e.type.equals("core") || e.type.equals("boss")) {
         e.g.rotateY(dt * 0.2F);
      }

      return !(mv.len() > 0.1F) && !walk ? new Vector3(to.x, 0.0F, to.z) : new Vector3(fx, 0.0F, fz);
   }

   void pcSwarm(Game.E e) {
      Vector3 at = e.pos.cpy().add(0.0F, -6.0F, 0.0F);
      String var3 = e.type;
      switch (var3) {
         case "carrier":
            for (int k = 0; k < 6; k++) {
               Game.E m = this.spawnEnemy("mini", at.cpy().add(this.rr(-8.0F, 8.0F), 0.0F, this.rr(-8.0F, 8.0F)));
               m.vel.set(0.0F, -8.0F, 0.0F);
            }
            break;
         case "hydra":
            for (int k = 0; k < 2; k++) {
               this.spawnEnemy("heavy", at.cpy().add(this.rr(-15.0F, 15.0F), 0.0F, this.rr(-10.0F, 10.0F)));
            }

            this.spawnEnemy("kami", at);
            break;
         case "chimera":
            for (int k = 0; k < 2; k++) {
               this.spawnEnemy("kami", at.cpy().add(this.rr(-10.0F, 10.0F), 0.0F, 0.0F));
            }

            this.spawnEnemy("split", at);
            break;
         case "gunship":
         case "manta":
            for (int k = 0; k < 3; k++) {
               this.spawnEnemy("wing", at.cpy().add(this.rr(-10.0F, 10.0F), 0.0F, this.rr(-10.0F, 10.0F)));
            }
            break;
         case "storm":
            for (int k = 0; k < 3; k++) {
               this.spawnEnemy("zig", at.cpy().add(this.rr(-12.0F, 12.0F), 0.0F, this.rr(-12.0F, 12.0F)));
            }
            break;
         case "ghost":
            for (int k = 0; k < 2; k++) {
               this.spawnEnemy("stealth", at.cpy().add(this.rr(-10.0F, 10.0F), 0.0F, this.rr(-10.0F, 10.0F)));
            }
            break;
         case "walker":
         case "colossus":
            for (int k = 0; k < 4; k++) {
               this.spawnEnemy(k % 2 == 0 ? "inf" : "rpg", new Vector3(e.pos.x + this.rr(-15.0F, 15.0F), 0.0F, e.pos.z + this.rr(-15.0F, 15.0F)));
            }
            break;
         default:
            for (int k = 0; k < 4; k++) {
               Game.E q = this.spawnEnemy("quad", at.cpy().add(this.rr(-12.0F, 12.0F), 0.0F, this.rr(-8.0F, 8.0F)));
               q.vel.set(0.0F, -4.0F, 0.0F);
            }
      }

      this.push("toast", "Босс выпустил рой!");
      if (this.ui != null && this.duelRole.equals("pvo")) {
         this.ui.toast("Босс выпустил рой!");
      }
   }

   void pcSpecial(Game.E e) {
      String nm = SPEC_NAME.containsKey(e.type) ? SPEC_NAME.get(e.type) : "";
      String var3 = e.type;
      switch (var3) {
         case "boss":
            this.bossSpecT = 3.5F;
            e.inv = true;
            e.pcInv = true;
            if (SHIELD_OVER == null) {
               SHIELD_OVER = Mat.unshadedAdd(new Color(0.4F, 0.75F, 1.0F, 0.35F));
            }

            e.md.body.overlay = SHIELD_OVER;
            break;
         case "ghost":
            this.bossSpecT = 4.5F;
            e.inv = true;
            e.pcInv = true;
            e.cloak = 1;
            e.md.body.alpha = 0.1F;
            break;
         case "gunship":
            this.bossSpecT = 3.0F;
            break;
         case "manta":
            this.bossSpecT = 2.5F;
            break;
         case "storm":
            for (int k = 0; k < 3; k++) {
               this.later(k * 0.45F, () -> {
                  if (e.hp > 0.0F && this.enemies.contains(e, true)) {
                     this.lightning(e.pos.cpy().add(0.0F, -6.0F, 0.0F), e);
                  }
               });
            }
            break;
         case "colossus":
            this.bossSpecT = 4.0F;
            e.inv = true;
            e.pcInv = true;
            this.shieldVis(e, true);
            break;
         case "walker":
            for (int k = 0; k < 5; k++) {
               this.later(k * 0.35F, () -> {
                  if (e.hp > 0.0F && this.enemies.contains(e, true)) {
                     this.enemyShot(e.pos.cpy().add(0.0F, 12.0F, 0.0F), "base", "shell", 0.0F);
                  }
               });
            }
            break;
         case "carrier":
            for (int k = 0; k < 12; k++) {
               Game.E m = this.spawnEnemy("mini", e.pos.cpy().add(this.rr(-14.0F, 14.0F), -10.0F, this.rr(-14.0F, 14.0F)));
               m.vel.set(0.0F, -10.0F, 0.0F);
            }
            break;
         case "hydra":
            for (int k = 0; k < 8; k++) {
               this.later(k * 0.1F, () -> {
                  if (e.hp > 0.0F && this.enemies.contains(e, true)) {
                     this.enemyShot(e, "erk", 70.0F);
                  }
               });
            }
            break;
         case "chimera":
            if (e.nodes.size == 0) {
               this.spawnNodes(e, 4);
            }
      }

      this.push("toast", "Босс: " + nm + "!");
      if (this.ui != null) {
         this.ui.toast("Босс: " + nm + "!");
      }
   }

   void bossCamera(float dt) {
      Game.E be = this.duelBoss();
      if (be != null) {
         float cy = this.bossIn.cy;
         Vector3 fw = new Vector3(
            -MathUtils.sin(cy) * MathUtils.cos(this.bossCamPitch), -MathUtils.sin(this.bossCamPitch), -MathUtils.cos(cy) * MathUtils.cos(this.bossCamPitch)
         );
         float dist = MathUtils.clamp(be.r * 2.4F + 16.0F, 26.0F, 90.0F);
         Vector3 bp = be.g.pos;
         Vector3 cpos = new Vector3(bp).mulAdd(fw, -dist).add(0.0F, be.r * 0.6F + 4.0F, 0.0F);
         cpos.y = Math.max(cpos.y, this.height(cpos.x, cpos.z) + 3.0F);
         if (this.shake > 0.0F) {
            cpos.add(this.rr(-1.0F, 1.0F) * this.shake * 0.5F, this.rr(-1.0F, 1.0F) * this.shake * 0.5F, 0.0F);
            this.shake = Math.max(0.0F, this.shake - dt * 2.2F);
         }

         if (this.cam.position.dst(cpos) < 60.0F) {
            this.cam.position.lerp(cpos, Math.min(1.0F, dt * 8.0F));
         } else {
            this.cam.position.set(cpos);
         }

         this.cam.direction.set(bp).mulAdd(fw, 60.0F).add(0.0F, be.r * 0.3F, 0.0F).sub(this.cam.position).nor();
         this.cam.up.set(Vector3.Y);
         this.cam.update();
         this.camPos.set(this.cam.position);
      }
   }

   public int coins() {
      return this.runWallet() ? this.RUN.c : G.SV.c;
   }

   public void setCoins(int v) {
      if (this.runWallet()) {
         this.RUN.c = v;
      } else {
         G.SV.c = v;
      }
   }

   public Map<String, Integer> UP() {
      if (this.hasMod("noUpg")) {
         return G.zeroUp();
      } else {
         return this.runWallet() ? this.RUN.up : G.SV.upv.get(this.VEH);
      }
   }

   public Map<String, Boolean> AB() {
      return this.runWallet() ? this.RUN.ab : G.SV.ab;
   }

   int u(String k) {
      Integer v = this.UP().get(k);
      return v == null ? 0 : v;
   }

   public float st(String k) {
      Float v = this.ST.get(k);
      return v == null ? 0.0F : v;
   }

   public void applyStats() {
      String V = this.VEH;
      this.ST.put("maxHp", (float)Math.round((100 + 20 * this.u("armor")) * (this.hasMod("glass") ? 0.5F : 1.0F)));
      this.ST.put("regen", 10.0F + 4 * this.u("armor"));
      this.ST.put("gun", !V.equals("tor") && !V.equals("luch") && !this.hasMod("missilesOnly") ? 1.0F : 0.0F);
      this.ST.put("beam", V.equals("luch") && !this.hasMod("missilesOnly") ? 1.0F : 0.0F);
      this.ST.put("ldps", 9.0F * (1.0F + 0.25F * this.u("lens")));
      float[] capk = new float[]{1.0F, 1.2F, 1.43F, 1.71F, 2.0F};
      this.ST.put("lheat", 1.0F / (3.5F * capk[MathUtils.clamp(this.u("cap"), 0, 4)]));
      this.ST.put("chain", (float)this.u("chain"));
      this.ST.put("rate", (V.equals("shilka") ? 26.0F : (V.equals("grom") ? 4.2F : 13.0F)) * (1.0F + 0.15F * this.u("rate")));
      this.ST.put("flak", V.equals("grom") ? 1.0F : 0.0F);
      this.ST.put("fdmg", 2.4F * (1.0F + 0.3F * this.u("fpow")));
      this.ST.put("frad", 13.0F * (1.0F + 0.15F * this.u("frad")));
      this.ST.put("fprox", 6.0F + 2.0F * this.u("fuse"));
      this.ST.put("dmg", 1.0F + 0.5F * this.u("dmg"));
      this.ST.put("heatPer", (V.equals("shilka") ? 0.021F : (V.equals("grom") ? 0.085F : 0.042F)) * (1.0F - 0.12F * this.u("belt")));
      this.ST.put("cool", 0.3F * (1.0F + 0.25F * (this.u("cool") + this.u("lcool"))));
      this.ST.put("spread", (V.equals("shilka") ? 0.008F : 0.004F) * (1.0F - 0.2F * this.u("spread")));
      this.ST.put("track", 0.25F * this.u("track"));
      this.ST
         .put(
            "maxMis",
            (float)(
               this.hasMod("gunOnly")
                  ? 0
                  : (V.equals("tor") ? 8 + this.u("count") : (V.equals("pantsir") ? 4 + this.u("count") : (this.hasMod("missilesOnly") ? 4 : 0)))
            )
         );
      this.ST.put("reload", (V.equals("tor") ? 2.2F : 3.5F) * (float)Math.pow(0.85, this.u("reload")));
      this.ST.put("misSpd", 1.0F + 0.2F * this.u("speed"));
      this.ST.put("warhead", 1.0F + 0.3F * this.u("warhead"));
      this.ST.put("chan", 0.42F * (float)Math.pow(0.85, this.u("channels")));
      this.ST.put("twin", 0.15F * this.u("twin"));
      this.ST.put("mog", (float)this.u("mog"));
   }

   public void setVehicleModel() {
      if (this.vm != null) {
         this.vm.root.remove();
         ArrayIterator var1 = this.vm.wreck.iterator();

         while (var1.hasNext()) {
            Object[] w = (Object[])var1.next();
            ((Node3)w[0]).remove();
         }
      }

      this.vm = Models.vehicle(this.VEH, G.SV.skVeh);
      this.vm.root.pos.y = 0.25F;
      this.root.add(this.vm.root);
      this.vm.root.updateGlobal(this.root.global);
   }

   public void syncNoses() {
      int n = this.vm.noseMeshes.size;

      for (int i = 0; i < n; i++) {
         ((Node3)this.vm.noseMeshes.get(i)).visible = i < Math.min(n, this.missiles) && this.st("maxMis") > 0.0F && !this.vm.broken;
      }
   }

   public void explosion(Vector3 p, float s) {
      this.explosion(p, s, true);
   }

   public void explosion(Vector3 p, float s, boolean smoke) {
      this.push("boom", p.x, p.y, p.z, s);
      FX.explosion(p, s, smoke);
      float d = p.dst(this.cam.position);
      this.shake = Math.max(this.shake, MathUtils.clamp(1.4F * s - d / 120.0F, 0.0F, 1.2F));
      G.sfx("boom", MathUtils.clamp(1.2F - d / 900.0F, 0.12F, 1.0F) * Math.min(1.4F, s), 30);
   }

   int lvl() {
      return this.isCamp() ? this.CM.getInt("lvl", 0) + this.wave : this.wave;
   }

   float spdMul() {
      float cm = 1.0F;
      if (this.CM != null) {
         cm = this.CM.getFloat("spdMul", this.hasMod("fast") ? 1.5F : 1.0F);
      }

      return this.difSpd * cm * (1.0F + Math.min(0.5F, this.lvl() * 0.012F));
   }

   public static boolean isBoss(String t) {
      return G.D.get("BOSS_INFO").has(t);
   }

   static boolean isGround(String t) {
      for (String g : GROUND_T) {
         if (g.equals(t)) {
            return true;
         }
      }

      return false;
   }

   public float height(float x, float z) {
      return this.world.height(x, z);
   }

   public Game.E spawnEnemy(String type) {
      return this.spawnEnemy(type, null, Float.NaN, Float.NaN);
   }

   public Game.E spawnEnemy(String type, Vector3 at) {
      return this.spawnEnemy(type, at, Float.NaN, Float.NaN);
   }

   public Game.E spawnEnemy(String type, Vector3 at, float optAng, float optDist) {
      JsonValue D = G.D.get("DT").get(type);
      if (D == null) {
         D = G.D.get("DT").get("quad");
      }

      String acc = (this.CM == null || this.CM.getInt("lvl", 0) < 18) && this.wave < 20 ? "harpy" : "manti";
      Models.EnemyModel md = Models.enemy(type, acc);
      boolean boss = isBoss(type);
      boolean ground = isGround(type);
      float spread = this.mode.equals("endless")
         ? Math.min(2.984513F, 0.6F + this.wave * 0.12F)
         : Math.min((float) (Math.PI * 9.0 / 10.0), 0.7F + this.lvl() * 0.04F);
      float a = Float.isNaN(optAng) ? this.rr(-spread, spread) : optAng;
      float Dd = Float.isNaN(optDist) ? (boss ? 650.0F : (ground ? this.rr(380.0F, 470.0F) : this.rr(420.0F, 560.0F))) : optDist;
      Vector3 pos = new Vector3();
      if (at != null) {
         pos.set(at);
      } else {
         float x = -MathUtils.sin(a) * Dd;
         float z = -MathUtils.cos(a) * Dd;
         float alt = 0.0F;
         if (!ground) {
            if (type.equals("kami")) {
               alt = this.rr(110.0F, 150.0F);
            } else if (type.equals("wing")) {
               alt = this.rr(60.0F, 110.0F);
            } else if (boss) {
               alt = type.equals("walker") ? 0.0F : (type.equals("carrier") ? 170 : 130);
            } else if (type.equals("scout")) {
               alt = this.rr(80.0F, 120.0F);
            } else {
               alt = this.rr(40.0F, 90.0F);
            }
         }

         pos.set(x, this.height(x, z) + alt, z);
      }

      md.g.pos.set(pos);
      this.root.add(md.g);
      float hpMul = boss
         ? (this.CM != null ? this.CM.getFloat("bossMul", 1.8F) : 1.8F) * 1.6F * (1.0F + this.lvl() * 0.03F)
         : 1.0F * (1.0F + this.lvl() * 0.03F);
      float ehp = Math.max(1.0F, (float)Math.round(D.getFloat("hp") * hpMul * this.difHp));
      JsonValue spA = D.get("sp");
      Game.E e = new Game.E();
      e.type = type;
      e.md = md;
      e.g = md.g;
      e.pos.set(pos);
      e.hp = ehp;
      e.max = ehp;
      e.r = md.r;
      e.t = this.R.nextFloat() * 10.0F;
      e.boss = boss;
      e.ground = ground;
      e.fire = this.rr(2.0F, 4.0F);
      e.side = this.R.nextFloat() < 0.5F ? -1.0F : 1.0F;
      e.sp = this.rr(spA.getFloat(0), spA.getFloat(1)) * 0.2F * this.spdMul();
      e.dmg = D.getFloat("dmg") * 0.6F * this.difDmg;
      e.alt = pos.y - this.height(pos.x, pos.z);
      if (this.obj != null
         && !boss
         && !ground
         && !type.equals("scout")
         && !type.equals("erk")
         && !type.equals("shell")
         && !type.equals("node")
         && !type.equals("relay")
         && this.R.nextFloat() < this.CM.getFloat("objShare", 0.5F)) {
         e.tgt = "obj";
      }

      if (type.equals("scout")) {
         e.ang = a;
         e.dir = this.R.nextFloat() < 0.5F ? -1.0F : 1.0F;
         e.distR = this.rr(260.0F, 330.0F);
      }

      if (type.equals("stealth")) {
         e.stealth = true;
         e.reveal = 0.0F;
         this.setBodyAlpha(e, 0.15F);
      }

      if (type.equals("erk")) {
         e.trail = FX.trail(md.g, false);
      }

      if (type.equals("core") || type.equals("chimera")) {
         this.spawnNodes(e, type.equals("chimera") ? 5 : 4);
      }

      e.id = this.nextId++;
      this.enemies.add(e);
      if (boss) {
         this.bosses.add(e);
      }

      return e;
   }

   void setBodyAlpha(Game.E e, float a) {
      e.md.body.alpha = a;
      if (e.md.glow != null) {
         e.md.glow.alpha = a;
      }

      if (e.md.rot != null) {
         e.md.rot.visible = a > 0.3F;
      }
   }

   void spawnNodes(Game.E core, int n) {
      for (int i = 0; i < n; i++) {
         Game.E nd = this.spawnEnemy("node", core.pos);
         nd.core = core;
         nd.oa = (float)i / n * (float) (Math.PI * 2);
         nd.hp = Math.round(14.400001F * (1.0F + this.lvl() * 0.03F));
         nd.max = nd.hp;
         core.nodes.add(nd);
      }

      core.inv = true;
   }

   public void removeEnemy(Game.E e) {
      if (e.trail != null) {
         FX.detach(e.trail);
         e.trail = null;
      }

      if (!e.keep) {
         e.g.remove();
      }

      this.enemies.removeValue(e, true);
      this.bosses.removeValue(e, true);
   }

   public int addCoins(float n) {
      int k = Math.max(1, Math.round(n));
      this.setCoins(this.coins() + k);
      this.runCoins += k;
      this.qProg("coins", k, false);
      return k;
   }

   void dmgNum(Game.E e, float dmg, String src) {
      float v = dmg * 10.0F;
      ArrayIterator d = this.dmgNums.iterator();

      while (d.hasNext()) {
         Game.DmgNum n = (Game.DmgNum)d.next();
         if (n.e == e && n.src.equals(src) && n.t > 0.5F) {
            n.v += v;
            n.t = 0.85F;
            n.p.set(this.visPos(e, this.tmpV)).add(0.0F, e.r * 0.9F + 1.0F, 0.0F);
            return;
         }
      }

      if (this.dmgNums.size >= 40) {
         this.dmgNums.removeIndex(0);
      }

      Game.DmgNum dx = new Game.DmgNum();
      dx.e = e;
      dx.p.set(this.visPos(e, this.tmpV)).add(0.0F, e.r * 0.9F + 1.0F, 0.0F);
      dx.v = v;
      dx.t = 0.85F;
      dx.src = src;
      this.dmgNums.add(dx);
   }

   public void damageEnemy(Game.E e, float dmg, Vector3 p, String src) {
      if (!(e.hp <= 0.0F)) {
         if (e.inv) {
            if (p != null) {
               FX.sparks(p, 3, new Color(0.6F, 0.8F, 1.0F, 1.0F));
            }
         } else {
            if (src.equals("gun") || src.equals("beam") || src.equals("mog") || src.equals("flak")) {
               if (e.type.equals("mech")) {
                  dmg *= src.equals("flak") ? 0.7F : 0.35F;
               } else if (e.type.equals("walker") || e.type.equals("colossus")) {
                  dmg *= 0.5F;
               } else if (e.type.equals("armor")) {
                  dmg *= src.equals("beam") ? 0.6F : 0.2F;
                  if (p != null && this.R.nextFloat() < 0.5F) {
                     FX.sparks(p, 4, new Color(0.6F, 0.85F, 1.0F, 1.0F));
                  }
               }
            }

            if (e.stealth) {
               e.reveal = 2.5F;
            }

            if (this.netView) {
               if (p != null) {
                  FX.sparks(p, 3);
               }

               if (src.equals("gun")) {
                  this.hitMark = 0.13F;
                  G.sfx("tink", 0.22F, 45);
               }
            } else {
               e.hp -= dmg;
               if (p != null) {
                  FX.sparks(p, 3);
               }

               if (!e.type.equals("erk") && !e.type.equals("shell")) {
                  this.dmgNum(e, dmg, src);
                  e.md.body.overlay = FLASH_MAT;
                  e.flash = 0.07F;
               }

               if (src.equals("gun")) {
                  this.hitMark = 0.13F;
                  G.sfx("tink", 0.22F, 45);
               } else if (src.equals("beam")) {
                  this.hitMark = 0.08F;
               }

               if (e.hp < e.max * 0.5F && e.smoke == null && !e.type.equals("erk") && !e.type.equals("shell") && !e.type.equals("node") && !e.ground) {
                  e.smoke = FX.burner(e.g, false, e.boss ? 3.0F : 1.0F);
               }

               if (e.hp <= 0.0F) {
                  this.killEnemy(e);
               }
            }
         }
      }
   }

   void killEnemy(Game.E e) {
      this.push("kill", (float)e.id);
      JsonValue D = G.D.get("DT").get(e.type);
      if (D == null) {
         D = G.D.get("DT").get("quad");
      }

      float sz = e.boss
         ? 4.0F
         : (
            !e.type.equals("heavy") && !e.type.equals("mech")
               ? (!e.type.equals("erk") && !e.type.equals("shell") && !e.type.equals("mini") ? 1.1F : 0.6F)
               : 1.8F
         );
      this.explosion(e.pos, sz);
      if (e.boss) {
         for (int k = 1; k < 4; k++) {
            Vector3 bp = e.pos.cpy().add(this.rr(-10.0F, 10.0F), this.rr(-3.0F, 3.0F), this.rr(-6.0F, 6.0F));
            this.later(k * 0.22F, () -> this.explosion(bp, 2.5F));
         }

         this.statAdd("bosses", 1.0F);
         this.qProg("boss", 1.0F, false);
         this.statAdd("b_" + e.type, 1.0F);
         if (this.ui != null) {
            this.ui.toast(G.D.get("BOSS_INFO").get(e.type).getString("short") + " уничтожен!");
         }

         this.shake = Math.max(this.shake, 1.2F);
         G.hap(80);
      }

      if (e.ground) {
         FX.puff(this.tmpV.set(e.pos).add(0.0F, 1.0F, 0.0F), 3.0F, new Color(0.1F, 0.1F, 0.1F, 1.0F), 0.6F, 5.0F, 3.0F);
      }

      this.score = this.score + Math.round(D.getFloat("pts") * 10.0F * (1.0F + (float)Math.floor(this.combo / 5.0F) * 0.1F));
      this.addCoins(D.getFloat("coin") * this.difCoin * (this.mode.equals("endless") ? 2.0F : 0.5F));
      G.sfx("coin", 0.5F, 60);
      this.combo++;
      this.comboT = 3.0F;
      this.kills++;
      this.statAdd("kills", 1.0F);
      this.qProg("kill", 1.0F, false);
      if (e.type.equals("heavy")) {
         this.qProg("heavy", 1.0F, false);
      }

      if (e.type.equals("kami") || e.type.equals("relay") || e.type.equals("mini")) {
         this.statAdd("k_" + e.type, 1.0F);
      }

      if (!this.VEH.equals("pantsir")) {
         this.statAdd("v_" + this.VEH, 1.0F);
      }

      if (e.type.equals("split")) {
         for (int i = 0; i < 3; i++) {
            Game.E m = this.spawnEnemy("mini", e.pos.cpy().add(this.rr(-3.0F, 3.0F), this.rr(-2.0F, 2.0F), this.rr(-3.0F, 3.0F)));
            m.vel.set(this.rr(-10.0F, 10.0F), this.rr(-4.0F, 4.0F), this.rr(-10.0F, 10.0F));
            m.tgt = e.tgt;
         }
      }

      if (e.core != null) {
         Game.E c = e.core;
         c.nodes.removeValue(e, true);
         if (c.nodes.size == 0) {
            c.inv = false;
            if (this.ui != null) {
               this.ui.toast("Щит снят — бей по ядру!");
            }
         }
      }

      if (e.type.equals("core") || e.type.equals("chimera")) {
         ArrayIterator var8 = new Array(e.nodes).iterator();

         while (var8.hasNext()) {
            Game.E n = (Game.E)var8.next();
            this.killEnemy(n);
         }
      }

      boolean noWreck = e.type.equals("erk")
         || e.type.equals("shell")
         || e.type.equals("node")
         || e.type.equals("core")
         || e.type.equals("chimera")
         || e.type.equals("mini");
      if (!e.ground && !noWreck) {
         this.makeWreck(e, sz);
      } else if (e.type.equals("mini") && this.R.nextFloat() < 0.5F) {
         this.makeWreck(e, sz);
      }

      this.removeEnemy(e);
   }

   void makeWreck(Game.E e, float sz) {
      if (this.wrecks.size >= 22) {
         Game.Wreck old = (Game.Wreck)this.wrecks.removeIndex(0);
         old.g.remove();
      }

      e.keep = true;
      e.md.body.overlay = CHAR_MAT;
      FX.burner(e.g, true, MathUtils.clamp(sz, 0.8F, 3.0F));
      Game.Wreck w = new Game.Wreck();
      w.g = e.g;
      w.rot = e.md.rot;
      w.vel.set(e.vel).scl(0.5F).add(this.rr(-7.0F, 7.0F), this.rr(2.0F, 9.0F), this.rr(-7.0F, 7.0F));
      if (e.boss) {
         w.vel.set(e.vel).scl(0.2F).add(this.rr(-3.0F, 3.0F), 2.0F, this.rr(-3.0F, 3.0F));
      }

      w.spin.set(this.rr(-4.0F, 4.0F), this.rr(-3.0F, 3.0F), this.rr(-5.0F, 5.0F)).scl(e.boss ? 0.25F : 1.0F);
      w.boss = e.boss;
      w.s = sz;
      this.wrecks.add(w);
   }

   void updateWrecks(float dt) {
      for (int i = this.wrecks.size - 1; i >= 0; i--) {
         Game.Wreck w = (Game.Wreck)this.wrecks.get(i);
         w.t += dt;
         w.vel.y = w.vel.y - (w.boss ? 11.0F : 24.0F) * dt;
         w.g.pos.mulAdd(w.vel, dt);
         w.g.rotateLocal(Vector3.X, w.spin.x * dt);
         w.g.rotateLocal(Vector3.Y, w.spin.y * dt);
         w.g.rotateLocal(Vector3.Z, w.spin.z * dt);
         if (w.rot != null) {
            w.rot.rotateY(dt * Math.max(0.0F, 9.0F - w.t * 4.0F));
         }

         Vector3 p = w.g.pos;
         if (p.y <= this.height(p.x, p.z) + 0.6F || w.t > 9.0F) {
            this.explosion(p, w.boss ? 3.2F : MathUtils.clamp(w.s * 0.6F, 0.5F, 1.4F));
            FX.puff(this.tmpV.set(p).add(0.0F, 1.5F, 0.0F), w.boss ? 4.0F : 2.0F, new Color(0.22F, 0.19F, 0.15F, 1.0F), 0.55F, 3.0F, 2.0F);
            if (w.boss) {
               this.shake = Math.max(this.shake, 1.1F);
            }

            w.g.remove();
            this.wrecks.removeIndex(i);
         }
      }
   }

   void updateJuice(float dt) {
      this.hitMark = Math.max(0.0F, this.hitMark - dt);
      if (this.boltT > 0.0F) {
         this.boltT -= dt;
         if (this.boltT <= 0.0F) {
            this.bolts.clear();
         }
      }

      for (int i = this.dmgNums.size - 1; i >= 0; i--) {
         Game.DmgNum n = (Game.DmgNum)this.dmgNums.get(i);
         n.t -= dt;
         n.p.y += dt * 9.0F;
         if (n.t <= 0.0F) {
            this.dmgNums.removeIndex(i);
         }
      }
   }

   String chooseFrom(Map<String, Float> mix) {
      float s = 0.0F;

      for (float v : mix.values()) {
         s += v;
      }

      float r = this.R.nextFloat() * s;

      for (Entry<String, Float> e : mix.entrySet()) {
         r -= e.getValue();
         if (r <= 0.0F) {
            return e.getKey();
         }
      }

      return mix.keySet().iterator().next();
   }

   void spawnFromMix(Map<String, Float> mix) {
      String t = this.chooseFrom(mix);
      if (t.equals("flock")) {
         Game.E lead = this.spawnEnemy("mini", null, this.rr(-1.0F, 1.0F), Float.NaN);

         for (int i = 0; i < 7; i++) {
            Game.E m = this.spawnEnemy("mini", lead.pos.cpy().add(this.rr(-6.0F, 6.0F), this.rr(-3.0F, 3.0F), this.rr(-6.0F, 6.0F)));
            m.flock = lead;
            m.foff.set(this.rr(-7.0F, 7.0F), this.rr(-3.0F, 4.0F), this.rr(-7.0F, 7.0F));
         }

         if (this.ui != null && this.R.nextFloat() < 0.5F) {
            this.ui.comm("lisa", "Стая! Бей очередью в центр!");
         }
      } else if (!t.equals("swarm")) {
         this.spawnEnemy(t);
      } else {
         Game.E base = this.spawnEnemy("mini", null, this.rr(-1.0F, 1.0F), Float.NaN);

         for (int i = 0; i < 4; i++) {
            this.spawnEnemy("mini", base.pos.cpy().add(this.rr(-10.0F, 10.0F), this.rr(-5.0F, 5.0F), this.rr(-10.0F, 10.0F)));
         }
      }
   }

   void spawnSquad(Map<String, Integer> mix) {
      float a = this.rr(-0.8F, 0.8F);
      int n = 0;

      for (Entry<String, Integer> e : mix.entrySet()) {
         for (int i = 0; i < e.getValue(); i++) {
            this.spawnEnemy(e.getKey(), null, a + this.rr(-0.12F, 0.12F), this.rr(380.0F, 450.0F));
            n++;
         }
      }

      if (this.ui != null && this.R.nextFloat() < 0.6F) {
         this.ui.comm("lisa", "Наземная группа, " + n + " целей! Опусти ствол!");
      }
   }

   JsonValue campWaveCfg() {
      return this.CM != null && this.CM.has("waves") ? this.CM.get("waves").get(Math.max(0, Math.min(this.wave - 1, this.CM.get("waves").size - 1))) : null;
   }

   String bossTypeForWave() {
      JsonValue c = this.campWaveCfg();
      if (c != null && c.has("boss")) {
         JsonValue b = c.get("boss");
         if (b.isString()) {
            return b.asString();
         }

         if (b.isBoolean() && b.asBoolean()) {
            return "boss";
         }
      }

      String[] rot = new String[]{"boss", "gunship", "walker", "carrier", "ghost", "hydra", "storm", "manta", "colossus"};
      return rot[Math.max(0, this.wave / 5 - 1) % rot.length];
   }

   Vector3 aimTarget(Game.E e) {
      return this.tgtPos(e.tgt);
   }

   Vector3 tgtPos(String tgt) {
      return "obj".equals(tgt) && this.obj != null && this.obj.hp > 0.0F ? this.obj.aim : AIM;
   }

   void face(Game.E e, Vector3 dir) {
      if (!(dir.len2() < 0.01F)) {
         this.tA.set(dir).nor();
         e.g.lookDir(this.tA, Math.abs(this.tA.y) < 0.98F ? Vector3.Y : Vector3.X);
      }
   }

   void enemyShot(Vector3 from, String tgt, String kind, float speed) {
      Vector3 Tt = this.tgtPos(tgt);
      if (kind.equals("erk")) {
         Game.E r = this.spawnEnemy("erk", from);
         r.vel.set(Tt).sub(from).nor().scl(speed * 0.6F);
         r.sp = speed * 1.6F;
         r.tgt = tgt;
         r.hp = 1.0F;
         r.max = 1.0F;
         r.dmg = 8.0F * this.difDmg;
      } else {
         Game.E s = this.spawnEnemy("shell", from);
         float dx = Tt.x - from.x + this.rr(-10.0F, 10.0F);
         float dz = Tt.z - from.z + this.rr(-10.0F, 10.0F);
         float dy = Tt.y - from.y;
         float tt = this.rr(4.0F, 5.5F);
         s.vel.set(dx / tt, (dy + 9.0F * tt * tt) / tt, dz / tt);
         s.hp = 1.0F;
         s.max = 1.0F;
         s.dmg = 6.0F * this.difDmg;
         s.tgt = tgt;
      }
   }

   void enemyShot(Game.E src, String kind, float speed) {
      this.enemyShot(src.pos.cpy(), src.tgt, kind, speed);
   }

   boolean hidden(Game.E e) {
      if (e.cloak != 0) {
         return true;
      } else {
         return !e.stealth ? false : e.reveal <= 0.0F && e.pos.dst(this.camPos) > 170.0F;
      }
   }

   public Vector3 visPos(Game.E e, Vector3 out) {
      out.set(e.pos);
      if (e.ground && !e.type.equals("walker")) {
         out.y = out.y + e.r * 0.8F;
      }

      return out;
   }

   int liveNonShots() {
      int n = 0;
      ArrayIterator var2 = this.enemies.iterator();

      while (var2.hasNext()) {
         Game.E o = (Game.E)var2.next();
         if (!o.type.equals("shell") && !o.type.equals("erk")) {
            n++;
         }
      }

      return n;
   }

   void updateEnemies(float dt) {
      float slow = this.empT > 0.0F ? 0.35F : 1.0F;

      for (int i = this.enemies.size - 1; i >= 0; i--) {
         if (i < this.enemies.size) {
            Game.E e = (Game.E)this.enemies.get(i);
            e.t += dt;
            if (e.stealth) {
               e.reveal = Math.max(0.0F, e.reveal - dt);
               boolean vis = !this.hidden(e);
               float tr = 1.0F - e.md.body.alpha;
               tr = MathUtils.lerp(tr, vis ? 0.25F : 0.86F, Math.min(1.0F, dt * 6.0F));
               this.setBodyAlpha(e, 1.0F - tr);
            }

            if (e.flock != null) {
               Game.E ld = e.flock;
               if (ld.hp > 0.0F && this.enemies.contains(ld, true)) {
                  this.tA.set(e.foff).rotateRad(Vector3.Y, e.t * 0.8F).add(ld.pos);
                  e.pos.lerp(this.tA, Math.min(1.0F, dt * 2.5F));
               } else {
                  e.flock = null;
               }
            }

            if (e.flash > 0.0F) {
               e.flash -= dt;
               if (e.flash <= 0.0F && e.md.body.overlay == FLASH_MAT) {
                  e.md.body.overlay = null;
               }
            }

            Vector3 Tt = this.aimTarget(e);
            Vector3 to = this.tB.set(Tt).sub(e.pos);
            float dist = to.len();
            float hd = (float)Math.sqrt(to.x * to.x + to.z * to.z);
            Vector3 want = this.tC.setZero();
            float sp = e.sp * slow;
            Vector3 faceDir = null;
            String ty = e.type;
            switch (ty) {
               case "quad":
               case "mini":
               case "decoy":
               case "zig":
               case "split":
                  if (!e.phase.equals("dive")) {
                     want.set(to.x, 0.0F, to.z).nor().scl(sp);
                     float ca = (ty.equals("mini") ? 28.0F : 38.0F) + MathUtils.sin(e.t) * 8.0F;
                     want.y = (this.height(e.pos.x, e.pos.z) + ca - e.pos.y) * 0.6F;
                     this.tA.set(-to.z, 0.0F, to.x).nor();
                     want.mulAdd(this.tA, ty.equals("zig") ? MathUtils.cos(e.t * 3.2F) * sp * 1.1F : MathUtils.cos(e.t * 1.7F) * 3.0F);
                     if (hd < ("obj".equals(e.tgt) ? 60.0F : 85.0F)) {
                        e.phase = "dive";
                     }
                  } else {
                     want.set(to).nor().scl(sp * 1.4F);
                  }

                  e.vel.lerp(want, 1.0F - (float)Math.exp(-dt * 1.8F));
                  e.g.setEuler(e.vel.z * 0.02F, e.t * 0.3F, -e.vel.x * 0.02F);
                  break;
               case "wing":
               case "kami":
                  float dd = ty.equals("kami") ? 200.0F : 130.0F;
                  if (!e.phase.equals("dive")) {
                     want.set(to.x, 0.0F, to.z).nor().scl(sp * (ty.equals("kami") ? 1.25F : 1.0F));
                     want.y = (e.alt - (e.pos.y - this.height(e.pos.x, e.pos.z))) * 0.4F;
                     if (hd < dd) {
                        e.phase = "dive";
                        if (ty.equals("kami")) {
                           G.sfx("alarm", 0.5F, 900);
                        }
                     }
                  } else {
                     want.set(to).nor().scl(sp * (ty.equals("kami") ? 2.0F : 1.5F));
                  }

                  e.vel.lerp(want, 1.0F - (float)Math.exp(-dt * (ty.equals("kami") ? 2.2F : 1.2F)));
                  faceDir = e.vel;
                  break;
               case "heavy":
                  if (!e.phase.equals("dive")) {
                     want.set(to.x, 0.0F, to.z).nor().scl(sp);
                     want.y = (this.height(e.pos.x, e.pos.z) + 60.0F - e.pos.y) * 0.5F;
                     if (hd < 120.0F) {
                        e.phase = "dive";
                     }
                  } else {
                     want.set(to).nor().scl(sp * 1.5F);
                  }

                  e.fire -= dt;
                  if (e.fire <= 0.0F && hd < 330.0F && hd > 140.0F) {
                     e.fire = this.rr(3.5F, 5.0F);
                     this.enemyShot(e, "erk", 55.0F);
                  }

                  e.vel.lerp(want, 1.0F - (float)Math.exp(-dt));
                  e.g.setEuler(e.vel.z * 0.03F, e.t * 0.2F, -e.vel.x * 0.03F);
                  break;
               case "relay":
                  this.tA.set(to.x, 0.0F, to.z).nor().scl(this.toSpawn > 0 ? -200.0F : -120.0F).add(Tt);
                  want.set(this.tA.x - e.pos.x, 0.0F, this.tA.z - e.pos.z).limit(Math.max(sp, 18.0F));
                  want.y = (this.height(e.pos.x, e.pos.z) + 75.0F - e.pos.y) * 0.5F;
                  e.vel.lerp(want, 1.0F - (float)Math.exp(-dt));
                  e.heal -= dt;
                  if (e.heal <= 0.0F) {
                     e.heal = 1.5F;
                     ArrayIterator var28 = this.enemies.iterator();

                     while (var28.hasNext()) {
                        Game.E ox = (Game.E)var28.next();
                        if (ox != e && !ox.boss && ox.hp < ox.max && ox.pos.dst(e.pos) < 70.0F) {
                           ox.hp = Math.min(ox.max, ox.hp + 1.0F);
                           FX.glow(ox.pos, 3.0F, new Color(0.3F, 1.0F, 0.5F, 1.0F), 0.3F);
                        }
                     }
                  }

                  e.g.rotateY(dt * 0.5F);
                  break;
               case "scout":
                  e.ang = e.ang + e.dir * dt * (sp / e.distR);
                  float tx = -MathUtils.sin(e.ang) * e.distR;
                  float tz = -MathUtils.cos(e.ang) * e.distR;
                  want.set(tx - e.pos.x, (this.height(tx, tz) + 100.0F - e.pos.y) * 0.5F, tz - e.pos.z).limit(sp * 1.4F);
                  e.vel.lerp(want, 1.0F - (float)Math.exp(-dt * 2.0F));
                  faceDir = e.vel;
                  e.data += dt / 22.0F;
                  if (e.data >= 1.0F && this.state.equals("run")) {
                     this.missionFail("Разведчик передал координаты");
                  }
                  break;
               case "erk":
                  this.tA.set(e.vel).nor();
                  this.tC.set(to).nor();
                  float ang = angleBetween(this.tA, this.tC);
                  float mt = 2.2F * dt;
                  if (ang > 1.0E-4F) {
                     slerp(this.tA, this.tC, Math.min(1.0F, mt / ang));
                  }

                  float nsp = Math.min(sp, e.vel.len() + 90.0F * dt);
                  e.vel.set(this.tA).scl(nsp);
                  faceDir = e.vel;
                  break;
               case "shell":
                  e.vel.y -= 18.0F * dt;
                  if (e.pos.y < this.height(e.pos.x, e.pos.z) + 0.5F) {
                     this.explosion(e.pos, 0.9F);
                     if (e.pos.dst(this.aimTarget(e)) < 9.0F) {
                        this.hitTarget(e.tgt, e.dmg);
                     }

                     this.removeEnemy(e);
                     continue;
                  }
                  break;
               case "node":
                  Game.E c = e.core;
                  if (c != null && !(c.hp <= 0.0F)) {
                     e.oa += dt * 0.8F;
                     e.pos.set(c.pos).add(MathUtils.cos(e.oa) * 22.0F, MathUtils.sin(e.oa * 2.0F) * 4.0F, MathUtils.sin(e.oa) * 22.0F);
                     e.vel.set(c.vel);
                     e.g.rotateY(dt * 2.0F);
                     e.g.pos.set(e.pos);
                     if (this.R.nextFloat() < 0.3F) {
                        FX.glow(this.tA.set(e.pos).lerp(c.pos, this.R.nextFloat()), 1.6F, new Color(0.5F, 0.75F, 1.0F, 1.0F), 0.1F);
                     }
                     continue;
                  }

                  this.removeEnemy(e);
                  continue;
               case "inf":
               case "rpg":
               case "mech":
               case "mortar":
                  int groundN = 0;
                  int allN = 0;
                  ArrayIterator lastOnes = this.enemies.iterator();

                  while (lastOnes.hasNext()) {
                     Game.E o = (Game.E)lastOnes.next();
                     if (o.ground) {
                        groundN++;
                     }

                     if (!o.type.equals("erk") && !o.type.equals("shell")) {
                        allN++;
                     }
                  }

                  boolean lastOnesx = this.toSpawn <= 0 && groundN == allN;
                  float rng = lastOnesx ? 120.0F : (ty.equals("inf") ? 130.0F : (ty.equals("rpg") ? 220.0F : (ty.equals("mech") ? 110.0F : 300.0F)));
                  if (hd > rng) {
                     want.set(to.x, 0.0F, to.z).nor().scl(sp);
                     e.anim += dt * sp * 0.9F;
                  }

                  e.vel.lerp(want, 1.0F - (float)Math.exp(-dt * 3.0F));
                  e.pos.y = this.height(e.pos.x, e.pos.z);
                  faceDir = this.tA.set(to.x, 0.0F, to.z);
                  e.fire -= dt;
                  if (hd <= rng + 5.0F && e.fire <= 0.0F) {
                     if (ty.equals("inf")) {
                        e.fire = this.rr(4.0F, 6.0F);
                        Game.E fe = e;

                        for (int k = 0; k < 3; k++) {
                           this.later(k * 0.11F, () -> {
                              if (fe.hp > 0.0F && this.state.equals("run")) {
                                 this.enemyTracer(fe);
                              }
                           });
                        }
                     } else if (ty.equals("rpg")) {
                        e.fire = this.rr(5.0F, 7.0F);
                        this.enemyShot(e.pos.cpy().add(0.0F, 8.5F, 0.0F), e.tgt, "erk", 55.0F);
                     } else if (ty.equals("mech")) {
                        e.fire = 3.0F;
                        this.enemyShot(e.pos.cpy().add(0.0F, 10.0F, 0.0F), e.tgt, "erk", 70.0F);
                     } else {
                        e.fire = this.rr(8.0F, 10.0F);
                        this.enemyShot(e.pos.cpy().add(0.0F, 5.0F, 0.0F), e.tgt, "shell", 0.0F);
                     }
                  }

                  if (e.md.legs.size > 0) {
                     float s = MathUtils.sin(e.anim * 2.2F) * 0.5F;
                     ((Node3)e.md.legs.get(0)).setEuler(s, 0.0F, 0.0F);
                     ((Node3)e.md.legs.get(1)).setEuler(-s, 0.0F, 0.0F);
                  }
                  break;
               default:
                  if (e.pc) {
                     faceDir = this.pcBoss(e, dt, to);
                  } else if (e.boss) {
                     faceDir = this.updateBoss(e, dt, to, hd, slow);
                  }
            }

            if (!e.ground && !ty.equals("walker")) {
               e.pos.mulAdd(e.vel, dt);
               float gh = this.height(e.pos.x, e.pos.z) + (ty.equals("erk") ? 0.5F : 3.0F);
               if (e.pos.y < gh && dist > 14.0F && !ty.equals("shell")) {
                  e.pos.y = gh;
                  e.vel.y = Math.abs(e.vel.y) * 0.5F;
               }
            } else {
               e.pos.mulAdd(e.vel, dt);
            }

            e.g.pos.set(e.pos);
            if (faceDir != null) {
               this.face(e, faceDir);
            }

            if (e.md.rot != null) {
               e.md.rot.rotateY(dt * 0.5F);
            }

            if (!e.boss && !e.ground && !ty.equals("shell") && dist < ("obj".equals(e.tgt) && this.obj != null ? this.obj.r : 4.5F)) {
               this.hitTarget(e.tgt, e.dmg);
               this.explosion(e.pos, ty.equals("erk") ? 0.8F : 1.3F);
               this.removeEnemy(e);
            } else if (e.pos.len2() > 6250000.0F) {
               this.removeEnemy(e);
            }
         }
      }
   }

   static float angleBetween(Vector3 a, Vector3 b) {
      float d = MathUtils.clamp(a.dot(b) / Math.max(1.0E-6F, a.len() * b.len()), -1.0F, 1.0F);
      return (float)Math.acos(d);
   }

   static void slerp(Vector3 a, Vector3 b, float t) {
      float d = MathUtils.clamp(a.dot(b), -1.0F, 1.0F);
      float th = (float)Math.acos(d) * t;
      Vector3 rel = new Vector3(b).mulAdd(a, -d);
      if (!(rel.len2() < 1.0E-10F)) {
         rel.nor();
         a.scl(MathUtils.cos(th)).mulAdd(rel, MathUtils.sin(th)).nor();
      }
   }

   Vector3 orbit(Game.E e, Vector3 to, float hd, float sp, float gh, float R0, float alt, float k) {
      Vector3 w = new Vector3();
      if (hd > R0 + 40.0F) {
         w.set(to.x, 0.0F, to.z).nor().scl(sp);
      } else {
         w.set(-to.z, 0.0F, to.x).nor().scl(sp * k * e.side);
         w.mulAdd(this.tA.set(to.x, 0.0F, to.z).nor(), (hd - R0) * 0.05F);
      }

      w.y = (gh + alt - e.pos.y) * 0.4F;
      return w;
   }

   Vector3 updateBoss(Game.E e, float dt, Vector3 to, float hd, float slow) {
      float sp = e.sp * slow;
      float gh = this.height(e.pos.x, e.pos.z);
      e.fire -= dt;
      e.spawnT -= dt;
      boolean live = this.empT <= 0.0F;
      String var10 = e.type;
      switch (var10) {
         case "boss":
            Vector3 wantxxxxxxxx = this.orbit(e, to, hd, sp, gh, 220.0F, 90.0F, 1.0F);
            e.vel.lerp(wantxxxxxxxx, 1.0F - (float)Math.exp(-dt * 0.8F));
            if (e.fire <= 0.0F && hd < 450.0F) {
               e.fire = e.hp < e.max * 0.4F ? 2.6F : 4.0F;

               for (int k = 0; k < 3; k++) {
                  this.later(k * 0.25F, () -> {
                     if (e.hp > 0.0F && this.enemies.contains(e, true)) {
                        this.enemyShot(e, "erk", 60.0F);
                     }
                  });
               }
            }

            if (e.spawnT <= 0.0F && live) {
               e.spawnT = 8.0F;

               for (int k = 0; k < 3; k++) {
                  Game.E q = this.spawnEnemy("quad", e.pos.cpy().add(this.rr(-12.0F, 12.0F), -5.0F, this.rr(-8.0F, 8.0F)));
                  q.vel.set(this.rr(-8.0F, 8.0F), -4.0F, this.rr(-8.0F, 8.0F));
               }
            }

            e.g.rotateY(dt * 0.2F);
            return null;
         case "gunship":
            Vector3 want2 = this.orbit(e, to, hd, sp, gh, 190.0F, 60.0F, 1.6F);
            e.vel.lerp(want2, 1.0F - (float)Math.exp(-dt * 1.2F));
            if (e.fire <= 0.0F && hd < 400.0F) {
               e.fire = 5.0F;

               for (int k = 0; k < 4; k++) {
                  this.later(k * 0.22F, () -> {
                     if (e.hp > 0.0F && this.enemies.contains(e, true)) {
                        this.enemyShot(e, "erk", 65.0F);
                     }
                  });
               }
            }

            return new Vector3(to.x, 0.0F, to.z);
         case "walker":
            Vector3 wantx = new Vector3();
            if (hd > 170.0F) {
               wantx.set(to.x, 0.0F, to.z).nor().scl(sp);
               e.anim += dt * 1.6F;
            }

            e.vel.lerp(wantx, 1.0F - (float)Math.exp(-dt * 2.0F));
            e.vel.y = 0.0F;
            if (e.md.legs.size > 0) {
               float s = MathUtils.sin(e.anim) * 0.45F;
               ((Node3)e.md.legs.get(0)).setEuler(s, 0.0F, 0.0F);
               ((Node3)e.md.legs.get(1)).setEuler(-s, 0.0F, 0.0F);
            }

            if (Math.abs(MathUtils.sin(e.anim)) > 0.98F && hd > 170.0F) {
               this.shake = Math.max(this.shake, 0.25F);
            }

            if (e.fire <= 0.0F && hd < 420.0F) {
               e.fire = e.hp < e.max * 0.4F ? 2.2F : 3.2F;
               e.g.updateGlobal(this.root.global);
               Vector3 from = e.g.toGlobal(this.tA.set(0.0F, 2.7F, -7.0F), new Vector3());
               this.enemyShot(from, e.tgt, "erk", 75.0F);
            }

            e.pos.y = gh + 8.4F * e.g.scl.y;
            return new Vector3(to.x, 0.0F, to.z);
         case "carrier":
            Vector3 wantxxxxxxx = this.orbit(e, to, hd, sp, gh, 380.0F, 170.0F, 0.5F);
            e.vel.lerp(wantxxxxxxx, 1.0F - (float)Math.exp(-dt * 0.4F));
            if (e.spawnT <= 0.0F && live) {
               e.spawnT = 5.5F;
               Vector3 base = e.pos.cpy().add(0.0F, -12.0F, 0.0F);

               for (int k = 0; k < 5; k++) {
                  Game.E m = this.spawnEnemy("mini", base.cpy().add(this.rr(-6.0F, 6.0F), 0.0F, this.rr(-10.0F, 10.0F)));
                  m.vel.set(this.rr(-6.0F, 6.0F), -8.0F, this.rr(-6.0F, 6.0F));
               }

               if (this.ui != null && this.R.nextFloat() < 0.4F) {
                  this.ui.comm("lisa", "«Улей» выпустил рой!");
               }
            }

            if (e.fire <= 0.0F && hd < 500.0F) {
               e.fire = 6.0F;
               this.enemyShot(e, "erk", 55.0F);
            }

            return new Vector3(e.vel.x, 0.0F, e.vel.z);
         case "ghost":
            if (!e.hasRunV) {
               e.runV.set(-e.pos.x, 0.0F, -e.pos.z).nor();
               e.hasRunV = true;
            }

            Vector3 want = new Vector3(e.runV).scl(sp * 1.4F);
            want.y = (gh + 120.0F - e.pos.y) * 0.4F;
            if (e.pos.dot(e.runV) > 520.0F) {
               e.runV.set(-e.pos.x + this.rr(-80.0F, 80.0F), 0.0F, -e.pos.z + this.rr(-80.0F, 80.0F)).nor();
            }

            e.vel.lerp(want, 1.0F - (float)Math.exp(-dt * 0.6F));
            if (hd < 60.0F && e.fire <= 0.0F) {
               e.fire = 0.9F;
               this.enemyShot(e, "shell", 0.0F);
            }

            e.cloakT -= dt;
            if (e.cloakT <= 0.0F) {
               e.cloak = e.cloak != 0 ? 0 : 1;
               e.cloakT = e.cloak != 0 ? 3.0F : 7.0F;
               e.inv = e.cloak != 0;
               this.setBodyAlpha(e, e.cloak != 0 ? 0.12F : 1.0F);
            }

            return new Vector3(e.vel.x, 0.0F, e.vel.z);
         case "core":
            Vector3 wantxxxxxx = this.orbit(e, to, hd, sp, gh, 260.0F, 120.0F, 0.5F);
            e.vel.lerp(wantxxxxxx, 1.0F - (float)Math.exp(-dt * 0.5F));
            e.g.rotateY(dt * 0.6F);
            if (e.fire <= 0.0F && hd < 480.0F) {
               e.fire = e.inv ? 3.2F : 2.2F;

               for (int k = 0; k < 2; k++) {
                  this.later(k * 0.3F, () -> {
                     if (e.hp > 0.0F && this.enemies.contains(e, true)) {
                        this.enemyShot(e, "erk", 60.0F);
                     }
                  });
               }
            }

            if (e.nodes.size == 0 && !e.inv) {
               e.reT -= dt;
               if (e.reT <= 0.0F) {
                  e.reT = 22.0F;
                  this.spawnNodes(e, 4);
                  if (this.ui != null) {
                     this.ui.toast("Ядро восстановило щит!");
                  }
               }
            }

            return null;
         case "hydra":
            Vector3 wantxxxxx = this.orbit(e, to, hd, sp, gh, 230.0F, 110.0F, 1.1F);
            e.vel.lerp(wantxxxxx, 1.0F - (float)Math.exp(-dt * 0.6F));
            if (e.fire <= 0.0F && hd < 450.0F) {
               e.fire = e.hp < e.max * 0.3F ? 1.5F : 2.4F;

               for (int k = 0; k < 3; k++) {
                  this.enemyShot(e, "erk", 60.0F);
               }
            }

            if (e.hp < e.max * (0.66F - e.split * 0.33F) && e.split < 2 && live) {
               e.split++;
               if (this.ui != null) {
                  this.ui.toast("«Гидра» сбрасывает рой!");
               }

               for (int k = 0; k < 3; k++) {
                  this.spawnEnemy("heavy", e.pos.cpy().add(this.rr(-20.0F, 20.0F), -6.0F, this.rr(-10.0F, 10.0F)));
               }

               for (int k = 0; k < 2; k++) {
                  this.spawnEnemy("kami", e.pos.cpy().add(this.rr(-10.0F, 10.0F), 0.0F, 0.0F));
               }
            }

            return new Vector3(e.vel.x, 0.0F, e.vel.z);
         case "storm":
            Vector3 wantxxxx = this.orbit(e, to, hd, sp, gh, 280.0F, 150.0F, 0.6F);
            e.vel.lerp(wantxxxx, 1.0F - (float)Math.exp(-dt * 0.5F));
            e.g.rotateY(dt * 0.3F);
            if (e.fire <= 0.0F && hd < 520.0F) {
               e.fire = e.hp < e.max * 0.4F ? 3.0F : 4.5F;
               this.lightning(e.pos.cpy().add(0.0F, -6.0F, 0.0F), e);
            }

            if (e.spawnT <= 0.0F && live) {
               e.spawnT = 9.0F;

               for (int k = 0; k < 3; k++) {
                  this.spawnEnemy("zig", e.pos.cpy().add(this.rr(-15.0F, 15.0F), -8.0F, this.rr(-15.0F, 15.0F)));
               }
            }

            return null;
         case "manta":
            if (e.runI < 0) {
               e.runI = 0;
               e.rt = 0.0F;
            }

            e.rt += dt;
            Vector3 wantM;
            if (e.runI == 0) {
               wantM = this.orbit(e, to, hd, sp, gh, 420.0F, 140.0F, 1.4F);
               if (e.rt > 7.0F) {
                  e.runI = 1;
                  e.rt = 0.0F;
                  if (this.ui != null && this.R.nextFloat() < 0.6F) {
                     this.ui.comm("lisa", "«Скат» заходит в пике!");
                  }
               }
            } else {
               Vector3 d2 = new Vector3(to.x, (gh + 70.0F - e.pos.y) * 0.02F, to.z).nor();
               if (e.runI == 1) {
                  wantM = d2.scl(sp * 3.2F);
                  if (hd < 70.0F) {
                     e.runI = 2;
                     e.rt = 0.0F;
                  }

                  if (hd < 160.0F && e.fire <= 0.0F) {
                     e.fire = 0.35F;
                     this.enemyShot(e, "shell", 0.0F);
                  }
               } else {
                  wantM = new Vector3(e.vel).nor().scl(sp * 3.0F);
                  wantM.y = 25.0F;
                  if (e.rt > 4.0F) {
                     e.runI = 0;
                     e.rt = 0.0F;
                  }
               }
            }

            e.vel.lerp(wantM, 1.0F - (float)Math.exp(-dt * 1.5F));
            return new Vector3(e.vel.x, 0.0F, e.vel.z);
         case "colossus":
            Vector3 wantxxx = new Vector3();
            if (hd > 210.0F) {
               wantxxx.set(to.x, 0.0F, to.z).nor().scl(sp);
            }

            e.vel.lerp(wantxxx, 1.0F - (float)Math.exp(-dt * 2.0F));
            e.vel.y = 0.0F;
            e.pos.y = gh;
            e.shT -= dt;
            if (e.shT <= 0.0F) {
               e.inv = !e.inv;
               e.shT = e.inv ? 4.0F : 6.0F;
               this.shieldVis(e, e.inv);
               if (!e.inv && this.ui != null && this.R.nextFloat() < 0.5F) {
                  this.ui.comm("lisa", "Купол «Колосса» опущен — огонь!");
               }
            }

            if (e.fire <= 0.0F && hd < 480.0F) {
               e.fire = e.hp < e.max * 0.4F ? 2.6F : 3.6F;
               this.enemyShot(e.pos.cpy().add(0.0F, 11.0F, 0.0F), e.tgt, "shell", 0.0F);
               this.enemyShot(e.pos.cpy().add(0.0F, 11.0F, 0.0F), e.tgt, "erk", 70.0F);
            }

            if (e.spawnT <= 0.0F && live) {
               e.spawnT = 12.0F;
               String[] ts = new String[]{"inf", "rpg", "mech"};

               for (int k = 0; k < 3; k++) {
                  this.spawnEnemy(ts[k], new Vector3(e.pos.x + this.rr(-20.0F, 20.0F), 0.0F, e.pos.z + this.rr(-20.0F, 20.0F)));
               }
            }

            return new Vector3(to.x, 0.0F, to.z);
         case "chimera":
            Vector3 wantxx = this.orbit(e, to, hd, sp, gh, 260.0F, 130.0F, 0.7F);
            e.vel.lerp(wantxx, 1.0F - (float)Math.exp(-dt * 0.5F));
            if (e.fire <= 0.0F && hd < 480.0F) {
               e.fire = e.hp < e.max * 0.3F ? 1.4F : 2.2F;

               for (int k = 0; k < 3; k++) {
                  this.enemyShot(e, "erk", 62.0F);
               }

               if (this.R.nextFloat() < 0.4F) {
                  this.enemyShot(e, "shell", 0.0F);
               }
            }

            if (e.spawnT <= 0.0F && live) {
               e.spawnT = 6.5F;

               for (int k = 0; k < 2; k++) {
                  this.spawnEnemy("kami", e.pos.cpy().add(this.rr(-10.0F, 10.0F), -4.0F, 0.0F));
               }

               this.spawnEnemy("split", e.pos.cpy().add(0.0F, -8.0F, 0.0F));
            }

            if (e.nodes.size == 0 && !e.inv) {
               e.reT -= dt;
               if (e.reT <= 0.0F) {
                  e.reT = 25.0F;
                  this.spawnNodes(e, 5);
               }
            }

            return new Vector3(e.vel.x, 0.0F, e.vel.z);
         default:
            return null;
      }
   }

   void enemyTracer(Game.E e) {
      Vector3 from = e.pos.cpy().add(0.0F, 6.9F, 0.0F);
      Game.Ebul b = new Game.Ebul();
      b.pos.set(from);
      b.vel.set(this.tgtPos(e.tgt)).sub(from).nor().scl(160.0F);
      b.vel.x = b.vel.x + this.rr(-9.0F, 9.0F);
      b.vel.y = b.vel.y + this.rr(-4.0F, 4.0F);
      b.life = 2.0F;
      b.dmg = 1.0F * this.difDmg;
      b.tgt = e.tgt;
      this.ebul.add(b);
      G.sfx("mg", 0.4F, 70);
   }

   void updateEbul(float dt) {
      for (int i = this.ebul.size - 1; i >= 0; i--) {
         Game.Ebul b = (Game.Ebul)this.ebul.get(i);
         b.pos.mulAdd(b.vel, dt);
         b.life -= dt;
         if (b.pos.dst(this.tgtPos(b.tgt)) < 4.0F) {
            this.hitTarget(b.tgt, b.dmg);
            FX.sparks(b.pos, 4);
            this.ebul.removeIndex(i);
         } else if (b.life <= 0.0F) {
            this.ebul.removeIndex(i);
         }
      }
   }

   void hitTarget(String tgt, float dmg) {
      if (this.state.equals("run")) {
         if ("obj".equals(tgt) && this.obj != null && this.obj.hp > 0.0F) {
            this.damageObj(dmg);
         } else {
            if (this.smokeT > 0.0F) {
               dmg /= 3.0F;
            }

            this.hp -= dmg;
            this.hurtT = 0.5F;
            this.shake = Math.max(this.shake, Math.min(1.0F, 0.3F + dmg / 20.0F));
            G.hap(45);
            G.sfx("hit", 0.8F, 80);
            this.waveHit = true;
            FX.sparks(this.tmpV.set(this.rr(-1.4F, 1.4F), this.rr(1.6F, 3.6F), this.rr(-4.0F, 4.0F)), 10);
            if (this.hp <= 0.0F) {
               this.hp = 0.0F;
               this.state = "dying";
               this.deadT = 3.2F;
               this.explosion(AIM.cpy().add(0.0F, 1.0F, 0.0F), 3.0F);
               this.breakVehicle();
               this.later(0.3F, () -> this.explosion(new Vector3(1.0F, 2.0F, -3.0F), 2.2F));
               this.later(0.7F, () -> this.explosion(new Vector3(-1.0F, 1.5F, 2.5F), 1.8F));
            }
         }
      }
   }

   void breakVehicle() {
      if (!this.vm.broken) {
         this.vm.broken = true;
         this.vm.wreck.clear();
         this.vm.root.forEach(n -> {
            if (n.mat != null) {
               Mat m = n.mat.cpy();
               if (m.hull) {
                  m.burnt = 0.85F;
               } else {
                  m.color.mul(0.18F, 0.18F, 0.18F, 1.0F);
                  m.emission = 0.0F;
                  m.metal = 0.0F;
                  m.rough = 1.0F;
               }

               n.mat = m;
            }
         });
         this.fly(
            this.vm.turret,
            new Vector3(this.rr(-5.0F, 5.0F), this.rr(15.0F, 20.0F), this.rr(-5.0F, 5.0F)),
            new Vector3(this.rr(-2.5F, 2.5F), this.rr(-4.0F, 4.0F), this.rr(-2.5F, 2.5F))
         );
         this.fly(
            this.vm.radar,
            new Vector3(this.rr(-10.0F, 10.0F), this.rr(18.0F, 24.0F), this.rr(-10.0F, 10.0F)),
            new Vector3(this.rr(-6.0F, 6.0F), this.rr(-6.0F, 6.0F), this.rr(-6.0F, 6.0F))
         );
         ArrayIterator a1 = this.vm.noseMeshes.iterator();

         while (a1.hasNext()) {
            Node3 m = (Node3)a1.next();
            m.visible = false;
         }

         Node3 a1x = new Node3();
         a1x.pos.set(0.0F, 2.4F, 0.5F);
         this.vm.root.add(a1x);
         FX.burner(a1x, true, 1.6F);
         Node3 a2 = new Node3();
         a2.pos.set(0.0F, 3.5F, 0.0F);
         this.vm.root.add(a2);
         FX.burner(a2, false, 2.5F);
         this.burnT = 40.0F;
      }
   }

   void fly(Node3 o, Vector3 v, Vector3 av) {
      this.root.updateGlobal(null);
      Matrix4 gm = new Matrix4(o.global);
      o.remove();
      this.root.add(o);
      gm.getTranslation(o.pos);
      o.rot.set(gm);
      o.rot.val[12] = 0.0F;
      o.rot.val[13] = 0.0F;
      o.rot.val[14] = 0.0F;
      this.vm.wreck.add(new Object[]{o, v, av, false});
   }

   void updateWreck(float dt) {
      if (this.vm != null && this.vm.broken) {
         ArrayIterator var2 = this.vm.wreck.iterator();

         while (var2.hasNext()) {
            Object[] w = (Object[])var2.next();
            if (!(Boolean)w[3]) {
               Node3 o = (Node3)w[0];
               Vector3 v = (Vector3)w[1];
               Vector3 av = (Vector3)w[2];
               v.y -= 22.0F * dt;
               o.pos.mulAdd(v, dt);
               o.rotateLocal(Vector3.X, av.x * dt);
               o.rotateLocal(Vector3.Y, av.y * dt);
               o.rotateLocal(Vector3.Z, av.z * dt);
               float gy = Math.max(0.3F, this.height(o.pos.x, o.pos.z) + 0.4F);
               if (o.pos.y < gy) {
                  o.pos.y = gy;
                  v.scl(0.35F);
                  v.y = Math.abs(v.y) * 0.4F;
                  av.scl(0.4F);
                  FX.explosion(o.pos, 0.5F, false);
                  if (v.len() < 1.5F) {
                     w[3] = true;
                     FX.burner(o, true, 0.8F);
                  }
               }

               if (this.R.nextFloat() < 0.5F) {
                  FX.puff(o.pos, 1.0F, new Color(0.1F, 0.1F, 0.1F, 1.0F), 0.5F, 1.6F, 1.0F);
               }
            }
         }
      }
   }

   Vector3 barrelWorld(int i, Vector3 out) {
      return this.vm.pitch.toGlobal((Vector3)this.vm.barrels.get(i), out);
   }

   void fireBullet() {
      this.barrelI = (this.barrelI + 1) % this.vm.barrels.size;
      Vector3 from = this.barrelWorld(this.barrelI, new Vector3());
      Vector3 tgt = this.tA.set(this.aimPoint);
      if (this.st("track") > 0.0F && this.lock.t != null && this.lock.k > 0.3F) {
         tgt.lerp(this.leadPoint(this.lock.t, this.tB), this.st("track") * 0.8F);
      }

      Vector3 v = this.tC.set(tgt).sub(from).nor();
      float s = this.st("spread");
      v.add(this.rr(-1.0F, 1.0F) * s, this.rr(-1.0F, 1.0F) * s, this.rr(-1.0F, 1.0F) * s).nor().scl(440.0F);
      Game.Bullet b = new Game.Bullet();
      b.pos.set(from);
      b.prev.set(from);
      b.o.set(from);
      b.vel.set(v);
      b.life = 2.2F;
      b.dmg = this.st("dmg");
      b.flak = this.st("flak") > 0.0F;
      this.bullets.add(b);
      FX.glow(this.tmpV.set(from).mulAdd(v, 0.005F), this.VEH.equals("grom") ? 4.2F : 2.8F, new Color(1.0F, 0.85F, 0.5F, 1.0F), 0.06F);
      if (this.VEH.equals("grom")) {
         G.sfx("flak", 0.8F, 60);
         this.shake = Math.max(this.shake, 0.12F);
         FX.puff(this.tmpV.set(from).mulAdd(v, 0.01F), 1.2F, new Color(0.5F, 0.5F, 0.48F, 1.0F), 0.4F, 0.8F, 1.0F);
      } else {
         G.sfx(this.VEH.equals("shilka") ? "shot2" : "shot", 0.7F, 38);
      }
   }

   static boolean segHit(Vector3 a, Vector3 b, Vector3 c, float r) {
      float abx = b.x - a.x;
      float aby = b.y - a.y;
      float abz = b.z - a.z;
      float l2 = Math.max(abx * abx + aby * aby + abz * abz, 1.0E-6F);
      float t = MathUtils.clamp(((c.x - a.x) * abx + (c.y - a.y) * aby + (c.z - a.z) * abz) / l2, 0.0F, 1.0F);
      float dx = a.x + abx * t - c.x;
      float dy = a.y + aby * t - c.y;
      float dz = a.z + abz * t - c.z;
      return dx * dx + dy * dy + dz * dz < r * r;
   }

   void updateBullets(float dt) {
      float windx = this.world.wind * 0.04F;
      this.tracers.clear();
      int n = 0;

      for (int i = this.bullets.size - 1; i >= 0; i--) {
         Game.Bullet b = (Game.Bullet)this.bullets.get(i);
         b.prev.set(b.pos);
         b.vel.y -= 3.5F * dt;
         b.vel.x += windx * dt;
         b.pos.mulAdd(b.vel, dt);
         b.life -= dt;
         boolean dead = b.life <= 0.0F;
         if (b.flak && !dead) {
            boolean boom = b.life < 0.12F;
            if (!boom) {
               ArrayIterator trav = this.enemies.iterator();

               while (trav.hasNext()) {
                  Game.E e = (Game.E)trav.next();
                  if (!e.type.equals("shell") && !this.hidden(e) && segHit(b.prev, b.pos, this.visPos(e, this.tmpV), e.r + this.st("fprox"))) {
                     boom = true;
                     break;
                  }
               }
            }

            if (boom) {
               this.flakBurst(b.pos);
               this.bullets.removeIndex(i);
               continue;
            }
         }

         if (!dead) {
            for (int k = 0; k < this.enemies.size; k++) {
               Game.E e = (Game.E)this.enemies.get(k);
               Vector3 c = this.tmpV.set(e.pos);
               if (e.ground) {
                  c.y = c.y + e.r * 0.8F;
               }

               if (segHit(b.prev, b.pos, c, e.r)) {
                  this.damageEnemy(e, b.dmg, b.pos, b.mog ? "mog" : "gun");
                  dead = true;
                  break;
               }
            }
         }

         if (!dead && b.pos.y < this.height(b.pos.x, b.pos.z)) {
            dead = true;
            FX.puff(this.tmpV.set(b.pos).add(0.0F, 0.5F, 0.0F), 0.7F, new Color(0.45F, 0.38F, 0.28F, 1.0F), 0.5F, 0.9F, 2.0F);
            ArrayIterator var12 = new Array(this.enemies).iterator();

            while (var12.hasNext()) {
               Game.E ex = (Game.E)var12.next();
               if (ex.ground && ex.pos.dst(b.pos) < 3.0F) {
                  this.damageEnemy(ex, b.dmg * 0.5F, null, "gun");
               }
            }
         }

         if (dead) {
            this.bullets.removeIndex(i);
         } else if (n < 400) {
            float L = 19.800001F;
            float trav = b.pos.dst(b.o);
            float len = Math.min(L, trav);
            if (len > 0.5F) {
               this.tA.set(b.vel).nor();
               this.tB.set(b.pos).mulAdd(this.tA, -len * 0.5F);
               Node3.basisLook(this.tm, this.tA, Math.abs(this.tA.y) < 0.98F ? Vector3.Y : Vector3.X);
               this.tm.val[12] = this.tB.x;
               this.tm.val[13] = this.tB.y;
               this.tm.val[14] = this.tB.z;
               this.tm.scale(0.13F, 0.13F, len);
               Color cx = b.mog ? MOGC : this.trc;
               this.tracers.prim(Dyn.BOX, this.tm, cx.r, cx.g, cx.b, 1.0F);
               n++;
            }
         }
      }
   }

   void lightning(Vector3 from, Game.E src) {
      Vector3 to = AIM.cpy().add(this.rr(-2.0F, 2.0F), 0.0F, this.rr(-2.0F, 2.0F));
      this.bolt(from, to);
      this.push("bolt", from.x, from.y, from.z, to.x, to.y, to.z);
      this.hitTarget(src.tgt, 7.0F * this.difDmg);
      if (this.st("gun") > 0.0F || this.st("beam") > 0.0F) {
         this.heat = Math.min(1.0F, this.heat + 0.35F);
      }

      G.sfx("emp", 0.7F, 200);
      this.shake = Math.max(this.shake, 0.4F);
   }

   void bolt(Vector3 a, Vector3 b) {
      this.bolts.clear();
      Vector3 prev = a.cpy();

      for (int i = 1; i <= 7; i++) {
         float k = i / 7.0F;
         Vector3 p = i == 7
            ? b.cpy()
            : a.cpy().lerp(b, k).add(new Vector3(this.rr(-6.0F, 6.0F), this.rr(-4.0F, 4.0F), this.rr(-6.0F, 6.0F)).scl(1.0F - Math.abs(k - 0.5F) * 1.2F));
         this.bolts.add(new float[]{prev.x, prev.y, prev.z, p.x, p.y, p.z});
         prev = p;
      }

      this.boltT = 0.18F;
      FX.glow(b, 6.0F, new Color(0.6F, 0.85F, 1.0F, 1.0F), 0.2F);
      FX.sparks(b, 14, new Color(0.7F, 0.9F, 1.0F, 1.0F));
   }

   void shieldVis(Game.E e, boolean on) {
      if (e.dome == null) {
         if (domeMesh == null) {
            MB b = new MB();
            b.add(MB.pSph(9.0F, 12.0F, 16, 8), Color.WHITE, new Matrix4());
            domeMesh = b.mesh();
         }

         Node3 m = new Node3(domeMesh, SHIELD_MAT);
         m.pos.set(0.0F, 3.5F, 0.0F);
         m.scl.set(1.0F / e.g.scl.x, 1.0F / e.g.scl.y, 1.0F / e.g.scl.z);
         e.g.add(m);
         e.dome = m;
      }

      e.dome.visible = on;
   }

   void beamPlace(int i, Vector3 a, Vector3 b, float w) {
      while (this.beams.size <= i) {
         this.beams.add(new float[7]);
      }

      float[] q = (float[])this.beams.get(i);
      q[0] = a.x;
      q[1] = a.y;
      q[2] = a.z;
      q[3] = b.x;
      q[4] = b.y;
      q[5] = b.z;
      q[6] = w;
   }

   void updateBeam(float dt) {
      this.beamUsed = 0;
      boolean on = this.firing && this.st("beam") > 0.0F && !this.overheated && this.state.equals("run");
      if (on) {
         this.heat = this.heat + this.st("lheat") * dt;
         if (this.heat >= 1.0F) {
            this.heat = 1.0F;
            this.overheated = true;
            G.sfx("alarm", 0.6F, 900);
         } else {
            Vector3 from = this.barrelWorld(0, new Vector3());
            Vector3 dir = new Vector3(this.aimPoint).sub(from).nor();
            Game.E best = null;
            float bt = 1600.0F;
            ArrayIterator end = this.enemies.iterator();

            while (end.hasNext()) {
               Game.E e = (Game.E)end.next();
               if (e.cloak == 0 && !e.type.equals("shell")) {
                  Vector3 c = this.visPos(e, this.tmpV);
                  float t = this.tA.set(c).sub(from).dot(dir);
                  if (!(t < 0.0F) && !(t > bt) && this.tB.set(from).mulAdd(dir, t).dst(c) < e.r * 0.9F + 0.8F) {
                     best = e;
                     bt = t;
                  }
               }
            }

            Vector3 endx = best != null ? new Vector3(from).mulAdd(dir, bt) : new Vector3(this.aimPoint);
            this.beamPlace(0, from, endx, 1.0F);
            FX.glow(this.tA.set(from).mulAdd(dir, 0.4F), 1.6F, new Color(0.5F, 0.95F, 1.0F, 1.0F), 0.05F);
            G.sfx("laser", 0.4F, 95);
            int used = 1;
            if (best != null) {
               FX.glow(endx, 2.4F, new Color(0.6F, 0.95F, 1.0F, 1.0F), 0.06F);
               this.damageEnemy(best, this.st("ldps") * dt, this.R.nextFloat() < 0.25F ? endx : null, "beam");
               Game.E cur = best;
               Array<Game.E> hit = new Array();
               hit.add(best);

               for (int k = 0; k < (int)this.st("chain"); k++) {
                  Game.E nb = null;
                  float nd = 75.0F;
                  ArrayIterator var14 = this.enemies.iterator();

                  while (var14.hasNext()) {
                     Game.E e = (Game.E)var14.next();
                     if (!hit.contains(e, true) && !this.hidden(e) && !e.type.equals("shell") && !e.type.equals("node")) {
                        float d = this.visPos(e, this.tA).dst(this.visPos(cur, this.tB));
                        if (d < nd) {
                           nd = d;
                           nb = e;
                        }
                     }
                  }

                  if (nb == null) {
                     break;
                  }

                  this.beamPlace(used, this.visPos(cur, new Vector3()), this.visPos(nb, new Vector3()), 0.6F);
                  used++;
                  this.damageEnemy(nb, this.st("ldps") * dt * 0.6F, null, "beam");
                  hit.add(nb);
                  cur = nb;
               }
            } else if (this.R.nextFloat() < 0.3F && this.aimPoint.y < this.height(this.aimPoint.x, this.aimPoint.z) + 2.0F) {
               FX.puff(this.tA.set(endx).add(0.0F, 0.5F, 0.0F), 0.8F, new Color(0.35F, 0.3F, 0.25F, 1.0F), 0.5F, 0.8F, 2.0F);
            }

            this.beamUsed = used;
         }
      }
   }

   void fillBeams() {
      this.beamDyn.clear();
      long ms = System.currentTimeMillis();

      for (int i = 0; i < this.beamUsed; i++) {
         float[] q = (float[])this.beams.get(i);

         for (int k = 0; k < 2; k++) {
            float ww = q[6] * (1.0F + 0.18F * MathUtils.sin((float)ms * 0.06F + k * 2.0F)) * (k == 0 ? 0.09F : 0.42F);
            this.cylBetween(
               this.beamDyn, q[0], q[1], q[2], q[3], q[4], q[5], ww, k == 0 ? new float[]{0.9F, 1.0F, 1.0F, 0.95F} : new float[]{0.2F, 0.85F, 1.0F, 0.32F}
            );
         }
      }

      if (this.boltT > 0.0F) {
         ArrayIterator var7 = this.bolts.iterator();

         while (var7.hasNext()) {
            float[] q = (float[])var7.next();
            this.cylBetween(this.beamDyn, q[0], q[1], q[2], q[3], q[4], q[5], 0.18F, new float[]{0.75F, 0.92F, 1.0F, 0.95F});
         }
      }
   }

   void cylBetween(Dyn d, float ax, float ay, float az, float bx, float by, float bz, float r, float[] c) {
      this.tA.set(bx - ax, by - ay, bz - az);
      float L = this.tA.len();
      if (!(L < 0.1F)) {
         this.tA.scl(1.0F / L);
         Vector3 ref = Math.abs(this.tA.y) < 0.95F ? Vector3.Y : Vector3.X;
         this.tB.set(this.tA).crs(ref).nor();
         this.tC.set(this.tB).crs(this.tA).nor();
         float[] m = this.tm.idt().val;
         m[0] = this.tB.x * r;
         m[1] = this.tB.y * r;
         m[2] = this.tB.z * r;
         m[4] = this.tA.x * L;
         m[5] = this.tA.y * L;
         m[6] = this.tA.z * L;
         m[8] = this.tC.x * r;
         m[9] = this.tC.y * r;
         m[10] = this.tC.z * r;
         m[12] = (ax + bx) / 2.0F;
         m[13] = (ay + by) / 2.0F;
         m[14] = (az + bz) / 2.0F;
         d.prim(Dyn.CYL8, this.tm, c[0], c[1], c[2], c[3]);
      }
   }

   void flakBurst(Vector3 p) {
      this.push("boom", p.x, p.y, p.z, 0.7F);
      FX.explosion(p, 0.7F, true);
      FX.puff(p, 3.5F, new Color(0.12F, 0.12F, 0.12F, 1.0F), 0.65F, 2.2F, 0.4F);
      G.sfx("boom", 0.35F, 50);
      this.hitMark = 0.13F;
      ArrayIterator var2 = new Array(this.enemies).iterator();

      while (var2.hasNext()) {
         Game.E e = (Game.E)var2.next();
         if (this.enemies.contains(e, true)) {
            float d = this.visPos(e, this.tmpV).dst(p) - e.r * 0.5F;
            if (d < this.st("frad")) {
               this.damageEnemy(e, this.st("fdmg") * (1.0F - MathUtils.clamp(d / this.st("frad"), 0.0F, 1.0F) * 0.6F), null, "flak");
            }
         }
      }
   }

   public Vector3 leadPoint(Game.E e, Vector3 out) {
      Vector3 c = this.visPos(e, new Vector3());
      float t = c.dst(this.camPos) / 440.0F;
      out.set(c).mulAdd(e.vel, t);
      t = out.dst(this.camPos) / 440.0F;
      out.set(c).mulAdd(e.vel, t);
      out.y += 1.75F * t * t;
      return out;
   }

   public boolean fireMissile(Object target, boolean free) {
      if (!this.state.equals("run")) {
         return false;
      } else if (this.netView) {
         if (this.duelRole.equals("pvo") && !this.VEH.equals("tor") && this.missiles > 0) {
            this.netMis++;
         }

         return false;
      } else if (free || this.missiles > 0 && !(this.st("maxMis") <= 0.0F)) {
         if (!free) {
            this.missiles--;
            if (this.reloadT <= 0.0F) {
               this.reloadT = this.st("reload");
            }
         }

         int nn = this.vm.noses.size;
         int idx = nn > 0 ? (!free && this.missiles < nn ? this.missiles : this.R.nextInt(Math.max(1, nn))) : 0;
         this.vm.root.updateGlobal(this.root.global);
         Vector3 from = nn > 0
            ? this.vm.pitch.toGlobal((Vector3)this.vm.noses.get(idx), new Vector3())
            : this.vm.turret.toGlobal(new Vector3(0.0F, 2.0F, 0.0F), new Vector3());
         Node3 node = new Node3();
         node.add(new Node3(Models.missileMesh(), Models.MAT_MISSILE));
         Node3 fl = FX.flame(1.78F);
         node.add(fl);
         SmokeTrail tr = new SmokeTrail(true);
         this.root.add(node);
         node.pos.set(from);
         Vector3 fwd = new Vector3();
         if (nn > 0) {
            float[] g = this.vm.pitch.global.val;
            fwd.set(-g[8], -g[9], -g[10]).nor();
         } else {
            fwd.set(this.aimDir);
         }

         node.lookDir(fwd, Math.abs(fwd.y) < 0.98F ? Vector3.Y : Vector3.X);
         FX.launchBlast(from, fwd);
         this.shake = Math.max(this.shake, 0.18F);
         Game.E tgt;
         if (target instanceof String) {
            tgt = this.lock.t != null && this.lock.k >= 1.0F ? this.lock.t : null;
         } else {
            tgt = (Game.E)target;
         }

         if (tgt != null) {
            ArrayIterator r = this.enemies.iterator();

            while (r.hasNext()) {
               Game.E o = (Game.E)r.next();
               if (o.type.equals("decoy") && o != tgt && o.pos.dst(tgt.pos) < 55.0F && this.R.nextFloat() < 0.5F) {
                  tgt = o;
                  break;
               }
            }
         }

         Game.Rocket r = new Game.Rocket();
         r.node = node;
         r.trail = tr;
         r.flame = fl;
         r.pos.set(from);
         r.vel.set(fwd).scl(32.0F).add(0.0F, 3.0F, 0.0F);
         r.tgt = tgt;
         r.life = 7.0F;
         r.sp = 32.0F;
         this.rockets.add(r);
         G.sfx("missile", 0.8F, 50);
         G.hap(15);
         this.statAdd("missiles", 1.0F);
         this.qProg("missile", 1.0F, false);
         this.syncNoses();
         return true;
      } else {
         return false;
      }
   }

   void updateRockets(float dt) {
      for (int i = this.rockets.size - 1; i >= 0; i--) {
         Game.Rocket r = (Game.Rocket)this.rockets.get(i);
         r.life -= dt;
         r.age += dt;
         float ms = this.st("misSpd");
         r.sp = Math.min(200.0F * ms, r.sp + (r.age < 0.12F ? 60.0F : 190.0F) * ms * dt);
         if (r.tgt != null && (r.tgt.hp <= 0.0F || !this.enemies.contains(r.tgt, true) || r.tgt.cloak != 0)) {
            r.tgt = null;
         }

         Vector3 dir = this.tA.set(r.vel).nor();
         if (r.tgt != null) {
            float d = r.tgt.pos.dst(r.pos);
            float lead = d / Math.max(r.sp, 1.0F);
            Vector3 wd = this.tB.set(r.tgt.pos).mulAdd(r.tgt.vel, lead).sub(r.pos).nor();
            float ang = angleBetween(dir, wd);
            float mt = 3.4F * dt;
            if (ang > 1.0E-4F) {
               slerp(dir, wd, Math.min(1.0F, mt / ang));
            }
         }

         r.vel.set(dir).scl(r.sp);
         Vector3 prev = this.tC.set(r.pos);
         Vector3 prevC = prev.cpy();
         r.pos.mulAdd(r.vel, dt);
         r.node.pos.set(r.pos);
         r.node.lookDir(dir, Math.abs(dir.y) < 0.98F ? Vector3.Y : Vector3.X);
         r.trail.emit(this.tmpV.set(r.pos).mulAdd(dir, -1.9F));
         float fk = this.rr(0.75F, 1.25F);
         r.flame.scl.set(fk * 0.9F + 0.1F, fk * 0.9F + 0.1F, fk * Math.min(1.0F, r.age * 6.0F + 0.3F));
         Game.E hit = null;
         ArrayIterator var18 = this.enemies.iterator();

         while (var18.hasNext()) {
            Game.E e = (Game.E)var18.next();
            if (!e.inv && segHit(prevC, r.pos, e.pos, e.r + 1.5F)) {
               hit = e;
               break;
            }
         }

         if (hit != null || r.life <= 0.0F || r.pos.y < this.height(r.pos.x, r.pos.z)) {
            this.explosion(r.pos, 1.6F);
            float dmg = 14.0F * this.st("warhead");
            ArrayIterator var20 = new Array(this.enemies).iterator();

            while (var20.hasNext()) {
               Game.E e = (Game.E)var20.next();
               float dd = e.pos.dst(r.pos);
               if (dd < 14.0F + e.r) {
                  this.damageEnemy(e, e == hit ? dmg : (float)Math.ceil(dmg * 0.55F * (1.0F - dd / (14.0F + e.r))), e.pos.cpy(), "mis");
               }
            }

            r.trail.stop();
            r.node.remove();
            this.rockets.removeIndex(i);
         }
      }
   }

   void updateLock(float dt) {
      Game.E best = null;
      float bestA = this.VEH.equals("tor") ? 0.32F : 0.2F;
      ArrayIterator var4 = this.enemies.iterator();

      while (var4.hasNext()) {
         Game.E e = (Game.E)var4.next();
         if (!this.hidden(e) && !e.type.equals("shell")) {
            Vector3 to = this.visPos(e, this.tA).sub(this.camPos);
            float d = to.len();
            if (!(d > 1200.0F)) {
               float a = angleBetween(this.aimDir, to) - (e.boss ? 0.04F : 0.0F);
               if (a < bestA) {
                  bestA = a;
                  best = e;
               }
            }
         }
      }

      if (best != this.lock.t) {
         this.lock.t = best;
         this.lock.k = 0.0F;
         this.lock.beeped = false;
      }

      if (this.lock.t != null) {
         this.lock.k = Math.min(1.0F, this.lock.k + dt / (this.VEH.equals("tor") ? 0.3F : 0.55F));
         if (this.lock.k >= 1.0F && !this.lock.beeped) {
            this.lock.beeped = true;
            if (this.st("maxMis") > 0.0F) {
               G.sfx("lock", 0.5F);
            }
         }
      }
   }

   public void setupMogs() {
      ArrayIterator lv = this.mogs.iterator();

      while (lv.hasNext()) {
         Models.Mog m = (Models.Mog)lv.next();
         m.g.remove();
      }

      this.mogs.clear();
      int lvx = (int)this.st("mog");
      if (lvx > 0) {
         float[][] spots = new float[][]{{13.0F, -9.0F}, {-15.0F, -7.0F}};

         for (int i = 0; i < (lvx >= 3 ? 2 : 1); i++) {
            Models.Mog m = Models.mog();
            m.g.pos.set(spots[i][0], this.height(spots[i][0], spots[i][1]), spots[i][1]);
            float ry = this.rr(-0.3F, 0.3F);
            m.g.setEuler(0.0F, ry, 0.0F);
            m.g.tag = ry;
            this.root.add(m.g);
            m.t = 0.0F;
            m.tgt = null;
            m.dmg = lvx >= 2 ? 0.9F : 0.6F;
            this.mogs.add(m);
         }
      }
   }

   void updateMogs(float dt) {
      ArrayIterator var2 = this.mogs.iterator();

      while (var2.hasNext()) {
         Models.Mog m = (Models.Mog)var2.next();
         m.t -= dt;
         m.g.updateGlobal(this.root.global);
         Vector3 gp = m.gun.gpos(new Vector3());
         Game.E t = (Game.E)m.tgt;
         if (t == null || t.hp <= 0.0F || !this.enemies.contains(t, true) || t.pos.dst(gp) > 320.0F) {
            m.tgt = null;
            t = null;
            float bd = 320.0F;
            ArrayIterator lp = this.enemies.iterator();

            while (lp.hasNext()) {
               Game.E e = (Game.E)lp.next();
               if (e.cloak == 0 && !e.boss) {
                  float d = e.pos.dst(gp);
                  if (d < bd) {
                     bd = d;
                     t = e;
                  }
               }
            }

            m.tgt = t;
         }

         if (t != null) {
            float tt = t.pos.dst(gp) / 440.0F;
            Vector3 lp = new Vector3(t.pos).mulAdd(t.vel, tt);
            m.gun.setEuler(0.0F, MathUtils.atan2(-(lp.x - gp.x), -(lp.z - gp.z)) - (Float)m.g.tag, 0.0F);
            if (m.t <= 0.0F) {
               m.t = 0.14285715F;
               Vector3 v = new Vector3(lp).sub(gp).nor().add(this.rr(-0.012F, 0.012F), this.rr(-0.012F, 0.012F), 0.0F).nor().scl(440.0F);
               Vector3 from = new Vector3(gp).add(0.0F, 0.55F, 0.0F);
               Game.Bullet b = new Game.Bullet();
               b.pos.set(from);
               b.prev.set(from);
               b.o.set(from);
               b.vel.set(v);
               b.life = 1.6F;
               b.dmg = m.dmg;
               b.mog = true;
               this.bullets.add(b);
               if (this.R.nextFloat() < 0.5F) {
                  G.sfx("mg", 0.3F, 70);
               }
            }
         }
      }
   }

   public void useAbility(String k) {
      if (this.state.equals("run") && Boolean.TRUE.equals(this.AB().get(k))) {
         if (this.netView) {
            if (this.duelRole.equals("pvo")) {
               int i = Arrays.asList("salvo", "emp", "smoke", "repair").indexOf(k);
               if (i >= 0) {
                  this.netAb[i]++;
               }
            }
         } else {
            if (k.equals("salvo")) {
               if (this.abUsed.get("salvo")) {
                  return;
               }

               this.abUsed.put("salvo", true);
               List<Game.E> ts = new ArrayList<>();
               ArrayIterator a = this.enemies.iterator();

               while (a.hasNext()) {
                  Game.E e = (Game.E)a.next();
                  if (!this.hidden(e) && !e.type.equals("shell") && !e.type.equals("node")) {
                     ts.add(e);
                  }
               }

               Collections.sort(ts, (ax, b) -> Float.compare(ax.pos.len2(), b.pos.len2()));

               for (int i = 0; i < 6; i++) {
                  Game.E tg = ts.size() > 0 ? ts.get(i % ts.size()) : null;
                  this.later(i * 0.12F, () -> this.fireMissile(tg, true));
               }

               if (this.ui != null) {
                  this.ui.toast("Залп!");
               }
            } else if (k.equals("emp")) {
               if (this.abUsed.get("emp")) {
                  return;
               }

               this.abUsed.put("emp", true);
               this.empT = 5.0F;
               G.sfx("emp");
               G.hap(40);

               for (int i = 0; i < 8; i++) {
                  FX.glow(
                     new Vector3(MathUtils.cos(i * (float) Math.PI / 4.0F) * 30.0F, 4.0F, MathUtils.sin(i * (float) Math.PI / 4.0F) * 30.0F),
                     30.0F,
                     new Color(0.5F, 0.8F, 1.0F, 1.0F),
                     0.8F
                  );
               }

               if (this.ui != null) {
                  this.ui.toast("Помехи: рой замедлен");
               }
            } else if (k.equals("smoke")) {
               if (this.abCd.get("smoke") > 0.0F) {
                  return;
               }

               this.abCd.put("smoke", 30.0F);
               this.smokeT = 6.0F;

               for (int i = 0; i < 18; i++) {
                  float a = this.R.nextFloat() * (float) (Math.PI * 2);
                  float r = this.rr(9.0F, 15.0F);
                  FX.puff(
                     new Vector3(MathUtils.sin(a) * r, this.rr(0.5F, 2.0F), MathUtils.cos(a) * r),
                     5.0F,
                     new Color(0.75F, 0.76F, 0.74F, 1.0F),
                     0.45F,
                     6.0F,
                     0.4F
                  );
               }
            } else if (k.equals("repair")) {
               if (this.abCd.get("repair") > 0.0F) {
                  return;
               }

               this.abCd.put("repair", 45.0F);
               this.repairT = 4.0F;
               if (this.ui != null) {
                  this.ui.toast("Ремонт…");
               }
            }

            G.sfx("click");
         }
      }
   }

   public void clearBattle() {
      ArrayIterator var1 = new Array(this.enemies).iterator();

      while (var1.hasNext()) {
         Game.E e = (Game.E)var1.next();
         e.keep = false;
         this.removeEnemy(e);
      }

      var1 = this.wrecks.iterator();

      while (var1.hasNext()) {
         Game.Wreck w = (Game.Wreck)var1.next();
         w.g.remove();
      }

      this.wrecks.clear();
      this.dmgNums.clear();
      this.hitMark = 0.0F;
      this.beamUsed = 0;
      var1 = this.rockets.iterator();

      while (var1.hasNext()) {
         Game.Rocket r = (Game.Rocket)var1.next();
         r.node.remove();
         r.trail.stop();
      }

      this.rockets.clear();
      this.bullets.clear();
      this.ebul.clear();
      this.bosses.clear();
      this.timers.clear();
      this.bolts.clear();
      this.lock.t = null;
      if (this.obj != null) {
         var1 = this.obj.models.iterator();

         while (var1.hasNext()) {
            Objs.Obj m = (Objs.Obj)var1.next();
            m.g.remove();
         }
      }

      this.obj = null;
      this.tracers.clear();
      FX.clear();
      var1 = this.world.stacks.iterator();

      while (var1.hasNext()) {
         Vector3 s = (Vector3)var1.next();
         FX.chimney(s);
      }
   }

   public void startGame(String m) {
      this.startGame(m, null);
   }

   public void startGame(String m, JsonValue def) {
      BlackBox.log("старт боя: " + m + (def != null ? " " + def.getString("id", "") : "") + " · " + G.SV.veh);
      this.clearBattle();
      this.mode = m;
      this.CM = m.equals("campaign") ? def : null;
      this.VEH = G.SV.veh;
      if (this.hasMod("missilesOnly") && (this.VEH.equals("shilka") || this.VEH.equals("luch") || this.VEH.equals("grom"))) {
         this.VEH = "pantsir";
      }

      this.RUN = null;
      if (m.equals("endless")) {
         this.RUN = new Game.Run();
         this.RUN.c = 60;
         this.RUN.up = new LinkedHashMap<>(G.SV.upv.get(this.VEH));
         this.RUN.ab = new LinkedHashMap<>();

         for (String k : new String[]{"salvo", "emp", "smoke", "repair"}) {
            this.RUN.ab.put(k, Boolean.TRUE.equals(G.SV.ab.get(k)));
         }
      }

      this.dif = G.D.get("DIFF").get(m.equals("endless") ? G.SV.diff : "normal");
      if (this.dif == null) {
         this.dif = G.D.get("DIFF").get("normal");
      }

      this.difSpd = this.dif.getFloat("spd");
      this.difDmg = this.dif.getFloat("dmg");
      this.difHp = this.dif.getFloat("hp");
      this.difCoin = this.dif.getFloat("coin");
      this.nightRun = m.equals("endless") && G.SV.night;
      if (this.nightRun) {
         this.difCoin *= 1.25F;
      }

      this.applyStats();
      this.setVehicleModel();
      JsonValue tr = G.D.get("SK_TR").get(G.SV.skTr);
      if (tr == null) {
         tr = G.D.get("SK_TR").get("gold");
      }

      this.trc.set(G.hex(tr.getString("col")));
      if (this.CM != null) {
         JsonValue wx = this.CM.get("weather");
         this.world
            .build(
               this.CM.getInt("theme", 0),
               this.CM.getBoolean("dark", false),
               wx != null ? wx.getFloat("rain", 0.0F) : 0.0F,
               wx != null ? wx.getFloat("fog", 0.0F) : 0.0F,
               wx != null ? wx.getFloat("wind", 0.0F) : 0.0F,
               this.CM.has("obj") ? this.CM.get("obj").getString("kind", "") : "",
               this.CM.getBoolean("noBeam", false)
            );
      } else {
         this.world.build(0, this.nightRun, 0.0F, 0.0F, 0.0F, "", false);
      }

      ArrayIterator var9 = this.world.stacks.iterator();

      while (var9.hasNext()) {
         Vector3 s = (Vector3)var9.next();
         FX.chimney(s);
      }

      this.obj = this.makeObj();
      this.setupMogs();
      this.score = 0;
      this.wave = 0;
      this.hp = this.st("maxHp");
      this.heat = 0.0F;
      this.overheated = false;
      this.kills = 0;
      this.combo = 0;
      this.runCoins = 0;
      this.missiles = (int)this.st("maxMis");
      this.reloadT = 0.0F;
      this.chanT = 0.0F;
      this.firing = false;
      this.aimYaw = 0.0F;
      this.aimPitch = 0.2F;
      this.toSpawn = 0;
      this.between = 1.2F;
      this.bossWave = false;
      this.bossSpawned = false;
      this.missionTime = 0.0F;
      this.missionT = this.CM != null ? this.CM.getFloat("time", 0.0F) : 0.0F;
      this.timeSaid = 0.0F;
      this.squadMix = null;
      this.resReason = "";
      this.abUsed.put("salvo", false);
      this.abUsed.put("emp", false);
      this.abCd.put("smoke", 0.0F);
      this.abCd.put("repair", 0.0F);
      this.smokeT = 0.0F;
      this.repairT = 0.0F;
      this.empT = 0.0F;
      this.scoutQ = 0;
      this.scoutT = 0.0F;
      this.squads = 0;
      this.squadT = 0.0F;
      this.nohitWaves = 0;
      this.wxRoll = 0;
      this.burnT = 0.0F;
      this.syncNoses();
      this.state = "run";
      if (this.ui != null) {
         this.ui.startHud();
      }

      if (this.CM != null && (this.CM.getString("type").equals("timer") || this.CM.getString("type").equals("escort"))) {
         this.between = 0.0F;
         this.wave = 1;
         this.spawnT = 1.5F;
         this.squadT = this.CM.has("squadEvery") ? 8.0F : 0.0F;
         if (this.ui != null) {
            this.ui.banner(this.CM.getString("title"), false, this.CM.getString("goal"));
         }

         G.sfx("wave");
      }

      this.ensureQuests();
      if (!G.SV.tut && (m.equals("endless") || this.CM != null && "1-1".equals(this.CM.getString("id", "")))) {
         this.tut = 0;
         this.tutT = 1.5F;
      }

      G.hap(25);
   }

   static Map<String, Float> mixOf(JsonValue m) {
      Map<String, Float> out = new LinkedHashMap<>();
      if (m == null) {
         out.put("quad", 1.0F);
         return out;
      } else {
         for (JsonValue v = m.child; v != null; v = v.next) {
            out.put(v.name, v.asFloat());
         }

         return out;
      }
   }

   static Map<String, Integer> squadOf(JsonValue m) {
      Map<String, Integer> out = new LinkedHashMap<>();

      for (JsonValue v = m.child; v != null; v = v.next) {
         out.put(v.name, v.asInt());
      }

      return out;
   }

   void startWave() {
      this.wave++;
      this.waveHit = false;
      this.abUsed.put("salvo", false);
      this.abUsed.put("emp", false);
      if (!this.mode.equals("campaign")) {
         this.squadMix = null;
         this.bossWave = this.wave % 5 == 0;
         this.bossSpawned = false;
         this.squads = this.wave >= 8 ? 1 + (this.wave - 8) / 2 : 0;
         this.squadT = 3.5F;
         if (this.wave == 8 && this.ui != null) {
            this.ui.toast("Наземные роботы! Опускай ствол ниже");
         }

         this.toSpawn = Math.round(this.bossWave ? 4 + this.wave : 4.0F + this.wave * 2.4F);
         this.spawned = 0;
         this.bossAt = (int)Math.ceil(this.toSpawn / 2.0);
         this.spawnInt = Math.max(0.35F, 1.5F - this.wave * 0.09F);
         this.spawnT = 0.4F;
         if (this.ui != null) {
            this.ui.banner("Волна " + this.wave, this.bossWave, this.bossWave ? G.D.get("BOSS_INFO").get(this.bossTypeForWave()).getString("name") : "");
         }

         G.sfx(this.bossWave ? "boss" : "wave");
         this.qProg("wave", this.wave, true);
         this.statMax("bestWave", this.wave);
      } else {
         JsonValue c = this.campWaveCfg();
         JsonValue bv = c.get("boss");
         this.bossWave = bv != null && (bv.isString() || bv.isBoolean() && bv.asBoolean());
         this.bossSpawned = false;
         this.squads = c.getInt("squads", 0);
         this.squadT = 3.5F;
         this.squadMix = c.get("squadMix");
         this.toSpawn = c.getInt("n");
         this.spawned = 0;
         this.bossAt = (int)Math.ceil(this.toSpawn / 2.0);
         this.spawnInt = c.getFloat("int", 1.3F);
         this.spawnT = 0.5F;
         this.scoutQ = c.getInt("scouts", 0);
         this.scoutT = 2.0F;
         String bt = this.bossWave ? this.bossTypeForWave() : "";
         if (this.ui != null) {
            String sub = "";
            if (this.bossWave) {
               sub = G.D.get("BOSS_INFO").get(bt).getString("name");
               if (c.has("boss2")) {
                  sub = sub + " + " + G.D.get("BOSS_INFO").get(c.getString("boss2")).getString("short");
               }
            }

            this.ui.banner(String.format("Волна %d/%d", this.wave, this.CM.get("waves").size), this.bossWave, sub);
         }

         G.sfx(this.bossWave ? "boss" : "wave");
         if (c.has("say") && this.ui != null) {
            for (JsonValue l = c.get("say").child; l != null; l = l.next) {
               this.ui.comm(l.getString(0), l.getString(1));
            }
         }
      }
   }

   Map<String, Float> endlessMix() {
      int w = this.wave;
      Map<String, Float> m = new LinkedHashMap<>();
      m.put("quad", 3.0F);
      m.put("wing", 1.0F + w * 0.2F);
      if (w >= 3) {
         m.put("zig", 2.0F);
         m.put("heavy", 1.0F + w * 0.1F);
      }

      if (w >= 4) {
         m.put("swarm", 1.2F);
      }

      if (w >= 5) {
         m.put("decoy", 1.0F);
      }

      if (w >= 6) {
         m.put("kami", 1.2F);
      }

      if (w >= 7) {
         m.put("split", 1.0F);
      }

      if (w >= 9) {
         m.put("relay", 0.5F);
      }

      if (w >= 6) {
         m.put("flock", 0.6F);
      }

      if (w >= 8) {
         m.put("stealth", 0.8F);
      }

      if (w >= 10) {
         m.put("armor", 0.7F);
      }

      return m;
   }

   void updateWaves(float dt) {
      this.missionTime += dt;
      if (this.CM == null || !this.CM.getString("type").equals("timer") && !this.CM.getString("type").equals("escort")) {
         if (this.between > 0.0F) {
            this.between -= dt;
            if (this.between <= 0.0F) {
               this.startWave();
            }
         } else {
            if (this.toSpawn > 0) {
               this.spawnT -= dt;
               if (this.spawnT <= 0.0F) {
                  this.spawnT = this.spawnInt * this.rr(0.7F, 1.3F);
                  this.toSpawn--;
                  this.spawned++;
                  if (this.bossWave && !this.bossSpawned && this.spawned >= this.bossAt) {
                     this.bossSpawned = true;
                     this.spawnEnemy(this.bossTypeForWave());
                     JsonValue c = this.campWaveCfg();
                     if (c != null && c.has("boss2")) {
                        this.spawnEnemy(c.getString("boss2"), null, this.rr(-0.6F, 0.6F), Float.NaN);
                     }

                     if (c != null && c.has("boss3")) {
                        this.spawnEnemy(c.getString("boss3"), null, this.rr(-0.9F, 0.9F), Float.NaN);
                     }
                  } else {
                     this.spawnFromMix(this.mode.equals("campaign") ? mixOf(this.campWaveCfg().get("mix")) : this.endlessMix());
                  }
               }
            }

            if (this.squads > 0) {
               this.squadT -= dt;
               if (this.squadT <= 0.0F) {
                  this.squads--;
                  this.squadT = 9.0F;
                  Map<String, Integer> mix = new LinkedHashMap<>();
                  if (this.squadMix != null) {
                     mix = squadOf(this.squadMix);
                  } else if (this.wave >= 12) {
                     mix.put("inf", 3);
                     mix.put("rpg", 2);
                     mix.put("mech", 1);
                  } else {
                     mix.put("inf", 4);
                     mix.put("rpg", 1);
                  }

                  this.spawnSquad(mix);
               }
            }

            if (this.scoutQ > 0) {
               this.scoutT -= dt;
               if (this.scoutT <= 0.0F) {
                  this.scoutQ--;
                  this.scoutT = 6.0F;
                  this.spawnEnemy("scout", null, this.rr(-0.4F, 0.4F), 600.0F);
                  if (this.ui != null) {
                     this.ui.comm("lisa", "Разведчик! Сбей его, пока он не передал данные!");
                  }
               }
            }

            int left = this.liveNonShots();
            if (this.toSpawn <= 0 && this.squads <= 0 && this.scoutQ <= 0 && left == 0) {
               if (!this.waveHit) {
                  this.statAdd("nohit", 1.0F);
                  this.nohitWaves++;
                  this.qProg("nohit", this.nohitWaves, true);
               } else {
                  this.nohitWaves = 0;
               }

               if (this.mode.equals("campaign") && this.wave >= this.CM.get("waves").size) {
                  this.missionComplete();
                  return;
               }

               this.between = 3.5F;
               this.hp = Math.min(this.st("maxHp"), this.hp + this.st("regen"));
               int bonus = 0;
               if (this.mode.equals("endless")) {
                  bonus = this.addCoins((15 + 6 * this.wave) * this.difCoin * (!this.waveHit ? 1.5F : 1.0F));
               }

               if (this.ui != null) {
                  this.ui
                     .banner(
                        "Волна отбита",
                        false,
                        bonus > 0
                           ? String.format("Прочность +%d · награда +%d монет%s", (int)this.st("regen"), bonus, !this.waveHit ? " (без урона ×1.5)" : "")
                           : "Прочность +" + (int)this.st("regen")
                     );
               }

               if (this.mode.equals("endless") && this.ui != null) {
                  this.ui.toast("Монеты боя: " + this.RUN.c + " — жми ₽ для улучшений");
               }

               this.checkAch();
            }
         }
      } else {
         this.missionT -= dt;
         if (this.CM.has("timeSay") && this.ui != null) {
            for (JsonValue ts = this.CM.get("timeSay").child; ts != null; ts = ts.next) {
               if (this.missionTime >= ts.getFloat(0) && this.timeSaid < ts.getFloat(0)) {
                  this.timeSaid = ts.getFloat(0);
                  this.ui.comm(ts.getString(1), ts.getString(2));
               }
            }
         }

         this.spawnT -= dt;
         float k = 1.0F - this.missionT / this.CM.getFloat("time");
         if (this.spawnT <= 0.0F && this.missionT > 3.0F) {
            this.spawnFromMix(mixOf(this.CM.get("mix")));
            this.spawnT = Math.max(0.7F, 2.2F - k * 0.9F) * (this.CM.getBoolean("swarm", false) ? 0.85F : 1.0F);
         }

         if (this.CM.has("squadEvery")) {
            this.squadT -= dt;
            if (this.squadT <= 0.0F) {
               this.squadT = this.CM.getFloat("squadEvery");
               Map<String, Integer> sm;
               if (this.CM.has("squadMix")) {
                  sm = squadOf(this.CM.get("squadMix"));
               } else {
                  sm = new LinkedHashMap<>();
                  sm.put("inf", 3);
                  sm.put("rpg", 1);
               }

               this.spawnSquad(sm);
            }
         }

         if (this.missionT <= 0.0F) {
            this.missionComplete();
         }
      }
   }

   Game.Mission makeObj() {
      if (this.CM != null && this.CM.has("obj")) {
         JsonValue o = this.CM.get("obj");
         String kind = o.getString("kind", "radar");
         Game.Mission ms = new Game.Mission();
         ms.name = o.getString("name");
         ms.hp = o.getFloat("hp") * 1.4F;
         ms.max = ms.hp;
         ms.kind = kind;
         if (kind.equals("convoy")) {
            for (int i = 0; i < 3; i++) {
               Objs.Obj m = Objs.make("truck");
               this.root.add(m.g);
               ms.models.add(m);
            }

            ms.r = 9.0F;
            this.updateObj(0.0F);
         } else {
            Objs.Obj m = Objs.make(kind);
            this.root.add(m.g);
            ms.models.add(m);
            if (kind.equals("radar")) {
               m.g.pos.set(-26.0F, this.height(-26.0F, -22.0F), -22.0F);
               ms.aim.set(-26.0F, this.height(-26.0F, -22.0F) + 6.0F, -22.0F);
               ms.r = 8.0F;
            } else if (kind.equals("dam")) {
               m.g.pos.set(0.0F, 0.0F, -240.0F);
               ms.aim.set(0.0F, 18.0F, -236.0F);
               ms.r = 22.0F;
            } else if (kind.equals("sub")) {
               m.g.pos.set(34.0F, this.height(34.0F, -40.0F), -40.0F);
               ms.aim.set(34.0F, this.height(34.0F, -40.0F) + 4.0F, -40.0F);
               ms.r = 14.0F;
            }
         }

         return ms;
      } else {
         return null;
      }
   }

   void updateObj(float dt) {
      if (this.obj != null) {
         if (this.obj.kind.equals("convoy")) {
            float p = MathUtils.clamp(this.missionTime / this.CM.getFloat("time", 80.0F), 0.0F, 1.0F);

            for (int i = 0; i < this.obj.models.size; i++) {
               Objs.Obj m = (Objs.Obj)this.obj.models.get(i);
               float z = 520.0F - p * 920.0F + i * 15;
               float y = this.height(-42.0F, z);
               m.g.pos.set(-42.0F + i % 2 * 1.2F - 0.6F, y + MathUtils.sin(this.missionTime * 6.0F + i) * 0.04F, z);
            }

            this.obj.aim.set(((Objs.Obj)this.obj.models.get(1)).g.pos).add(0.0F, 2.5F, 0.0F);
         }

         ArrayIterator var7 = this.obj.models.iterator();

         while (var7.hasNext()) {
            Objs.Obj m = (Objs.Obj)var7.next();
            if (m.spin != null) {
               m.spin.rotateY(dt * 1.2F);
            }
         }

         if (this.obj.hp < this.obj.max * 0.5F && this.R.nextFloat() < 0.1F) {
            FX.puff(
               this.tmpV.set(this.obj.aim).add(this.rr(-3.0F, 3.0F), 0.0F, this.rr(-3.0F, 3.0F)), 3.0F, new Color(0.12F, 0.12F, 0.12F, 1.0F), 0.5F, 3.0F, 3.0F
            );
         }
      }
   }

   void damageObj(float n) {
      if (this.obj != null && !(this.obj.hp <= 0.0F)) {
         this.obj.hp -= n;
         this.hurtT = 0.25F;
         G.sfx("hit", 0.6F, 80);
         if (this.ui != null && this.R.nextFloat() < 0.15F) {
            this.ui.comm("lisa", this.obj.name + " под ударом!");
         }

         if (this.obj.hp <= 0.0F) {
            this.obj.hp = 0.0F;
            this.explosion(this.obj.aim, 3.0F);
            String nm = this.obj.name;
            this.later(0.6F, () -> this.missionFail("Объект «" + nm + "» уничтожен"));
         }
      }
   }

   float statOf(String k) {
      if (k.equals("hp")) {
         return Math.max(0.0F, this.hp) / this.st("maxHp");
      } else if (k.equals("obj")) {
         return this.obj != null ? Math.max(0.0F, this.obj.hp) / this.obj.max : 1.0F;
      } else {
         return k.equals("time") ? this.missionTime : 0.0F;
      }
   }

   boolean starOk(JsonValue st) {
      if (!st.has("k")) {
         return true;
      } else {
         float v = this.statOf(st.getString("k"));
         return st.getString("op").equals(">=") ? v >= st.getFloat("v") : v <= st.getFloat("v");
      }
   }

   void missionComplete() {
      BlackBox.log("миссия выполнена");
      if (this.state.equals("run")) {
         this.state = "result";
         this.firing = false;
         JsonValue stars = this.CM.get("stars");
         boolean[] ok = new boolean[stars.size];
         int got = 0;

         for (int i = 0; i < stars.size; i++) {
            ok[i] = this.starOk(stars.get(i));
            if (ok[i]) {
               got++;
            }
         }

         String id = this.CM.getString("id");
         Integer pvI = G.SV.camp.get(id);
         int prev = pvI == null ? 0 : pvI;
         String special = this.CM.getString("special", "");
         int rw = this.CM.getInt("reward");
         int reward;
         if (special.equals("chal")) {
            Integer pc = G.SV.chal.get(id);
            int pv = pc == null ? 0 : pc;
            reward = pv > 0 ? Math.round(rw * 0.25F) : rw;
            if (pv == 0) {
               this.statAdd("chal", 1.0F);
            }

            G.SV.chal.put(id, Math.max(pv, got));
         } else {
            reward = (prev == 0 ? rw : 0) + Math.max(0, got - Math.max(prev, 1)) * Math.round(rw * 0.25F);
            G.SV.camp.put(id, Math.max(prev, got));
         }

         G.SV.c += reward;
         if (this.CM.getBoolean("final", false) && !G.SV.skOwn.contains("aurum")) {
            G.SV.skOwn.add("aurum");
         }

         if (this.CM.has("skin") && !G.SV.skOwn.contains(this.CM.getString("skin"))) {
            G.SV.skOwn.add(this.CM.getString("skin"));
         }

         this.xpGot = Math.max(5, Math.round(this.score / 150.0F)) + got * 5;
         this.rankUp = G.addXp(this.xpGot);
         this.checkAch();
         G.save();
         G.sfx("clear");
         G.hap(40);
         this.resWin = true;
         this.resOk = ok;
         this.resGot = got;
         this.resReward = reward;
         this.resReason = "";
         this.resM = this.CM;
         JsonValue cm = this.CM;
         this.later(0.9F, () -> {
            if (this.state.equals("result") && this.CM == cm && this.ui != null) {
               this.ui.result(true);
            }
         });
      }
   }

   void missionFail(String reason) {
      BlackBox.log("миссия провалена: " + reason);
      if (this.state.equals("run") || this.state.equals("dying")) {
         if (this.mode.equals("campaign")) {
            this.state = "result";
            this.firing = false;
            G.save();
            G.sfx("fail");
            G.hap(60);
            this.resWin = false;
            this.resOk = null;
            this.resGot = 0;
            this.resReward = 0;
            this.resReason = reason;
            this.resM = this.CM;
            JsonValue cm = this.CM;
            this.later(1.1F, () -> {
               if (this.state.equals("result") && this.CM == cm && this.ui != null) {
                  this.ui.result(false);
               }
            });
         }
      }
   }

   void gameOver() {
      BlackBox.log("бой окончен · волна " + this.wave + " · очки " + this.score);
      if (this.mode.equals("duel")) {
         if (this.duel != null && !this.netView) {
            this.duel.finishHost("boss", "Комплекс ПВО уничтожен!");
         }
      } else if (this.mode.equals("campaign")) {
         this.state = "run";
         this.missionFail("Комплекс уничтожен");
      } else {
         this.state = "over";
         boolean rec = this.score > G.SV.best;
         if (rec) {
            G.SV.best = this.score;
         }

         if (this.score > 0) {
            G.Top t = new G.Top();
            t.s = this.score;
            t.w = this.wave;
            t.d = G.SV.diff;
            t.t = G.today();
            t.v = this.VEH;
            G.SV.top.add(t);
            Collections.sort(G.SV.top, (a, b) -> Integer.compare(b.s, a.s));

            while (G.SV.top.size() > 10) {
               G.SV.top.remove(G.SV.top.size() - 1);
            }
         }

         G.SV.c = G.SV.c + Math.round(this.runCoins * 0.3F);
         this.xpGot = this.score > 0 ? Math.max(1, Math.round(this.score / 100.0F)) : 0;
         this.rankUp = G.addXp(this.xpGot);
         this.checkAch();
         G.save();
         if (this.ui != null) {
            this.ui.over(rec);
         }
      }
   }

   public void quitBattle() {
      this.clearBattle();
      ArrayIterator var1 = this.mogs.iterator();

      while (var1.hasNext()) {
         Models.Mog m = (Models.Mog)var1.next();
         m.g.remove();
      }

      this.mogs.clear();
      this.state = "menu";
      this.RUN = null;
      this.CM = null;
      this.mode = "endless";
      this.netView = false;
      this.duelRole = "";
      this.bossE = null;
      G.save();
   }

   public void statAdd(String k, float v) {
      Float o = G.SV.stats.get(k);
      G.SV.stats.put(k, (o == null ? 0.0F : o) + v);
   }

   void statMax(String k, float v) {
      Float o = G.SV.stats.get(k);
      if (o == null || v > o) {
         G.SV.stats.put(k, v);
      }
   }

   public float statVal(String k) {
      if (!k.equals("campDone") && !k.equals("campStars")) {
         Float v = G.SV.stats.get(k);
         return v == null ? 0.0F : v;
      } else {
         float n = 0.0F;

         for (JsonValue ch = G.D.get("CHAPTERS").child; ch != null; ch = ch.next) {
            for (JsonValue m = ch.get("missions").child; m != null; m = m.next) {
               Integer s = G.SV.camp.get(m.getString("id"));
               int sv = s == null ? 0 : s;
               n += k.equals("campDone") ? (sv > 0 ? 1 : 0) : sv;
            }
         }

         return n;
      }
   }

   public void checkAch() {
      for (JsonValue a = G.D.get("ACH").child; a != null; a = a.next) {
         String id = a.getString("id");
         if (!G.SV.ach.contains(id) && !a.getString("st").equals("coop") && this.statVal(a.getString("st")) >= a.getFloat("n")) {
            G.SV.ach.add(id);
            G.SV.c = G.SV.c + a.getInt("r");
            if (this.ui != null) {
               this.ui.toast("Достижение: " + a.getString("name") + " (+" + a.getInt("r") + ")");
            }
         }
      }
   }

   public void ensureQuests() {
      String d = G.today();
      if (!G.SV.qDay.equals(d) || G.SV.qList.size() <= 0) {
         Random rng = new Random(("q" + d).hashCode());
         List<String> keys = new ArrayList<>();

         for (JsonValue q = G.D.get("QDEF").child; q != null; q = q.next) {
            keys.add(q.name);
         }

         List<G.Quest> list = new ArrayList<>();

         while (list.size() < 3) {
            String k = keys.get(rng.nextInt(keys.size()));
            boolean dup = false;

            for (G.Quest q : list) {
               if (q.id.equals(k)) {
                  dup = true;
               }
            }

            if (!dup) {
               JsonValue qd = G.D.get("QDEF").get(k);
               int lv = rng.nextInt(qd.get("n").size);
               G.Quest qx = new G.Quest();
               qx.id = k;
               qx.lv = lv;
               qx.n = qd.get("n").getFloat(lv);
               qx.r = qd.get("r").getFloat(lv);
               list.add(qx);
            }
         }

         G.SV.qDay = d;
         G.SV.qList.clear();
         G.SV.qList.addAll(list);
         G.save();
      }
   }

   public void qProg(String id, float v, boolean isMax) {
      for (G.Quest q : G.SV.qList) {
         if (q.id.equals(id) && !q.done) {
            q.p = isMax ? Math.max(q.p, v) : q.p + v;
            if (q.p >= q.n) {
               q.p = q.n;
               q.done = true;
               if (this.ui != null) {
                  this.ui.toast("Задание выполнено: " + G.D.get("QDEF").get(id).get("t").getString(q.lv));
               }
            }
         }
      }
   }

   public void updateCamera(float dt) {
      if (this.state.equals("menu")) {
         float a2 = 0.75F + MathUtils.sin(this.T * 0.08F) * 0.45F;
         this.cam.position.set(MathUtils.sin(a2) * 21.0F, 6.8F + MathUtils.sin(this.T * 0.3F) * 0.6F, MathUtils.cos(a2) * 21.0F - 2.0F);
         this.cam.direction.set(0.0F, 4.0F, 0.0F).sub(this.cam.position).nor();
         this.cam.up.set(Vector3.Y);
         this.cam.direction.rotateRad(Vector3.Y, 0.42F);
         this.cam.update();
      } else if (!this.mode.equals("duel") || !this.duelRole.equals("boss") || this.duelBoss() == null || this.vm != null && this.vm.broken) {
         if (this.vm != null && this.vm.broken) {
            this.wreckCam += dt;
            float a = this.aimYaw + 0.6F + this.wreckCam * 0.25F;
            float r = 17.0F - Math.min(5.0F, this.wreckCam * 1.5F);
            this.cam.position.set(MathUtils.sin(a) * r, 7.0F + MathUtils.sin(this.wreckCam * 0.5F), MathUtils.cos(a) * r);
            this.cam.direction.set(0.0F, 2.2F, 0.0F).sub(this.cam.position).nor();
            this.cam.up.set(Vector3.Y);
            this.cam.update();
         } else {
            this.wreckCam = 0.0F;
            float p = this.aimPitch;
            this.aimDir.set(-MathUtils.sin(this.aimYaw) * MathUtils.cos(p), MathUtils.sin(p), -MathUtils.cos(this.aimYaw) * MathUtils.cos(p));
            Vector3 tp = this.vm.turret.gpos(new Vector3());
            this.camPos
               .set(tp)
               .add(
                  MathUtils.sin(this.aimYaw) * 13.5F * MathUtils.cos(p * 0.85F),
                  4.6F - 2.2F * MathUtils.sin(p),
                  MathUtils.cos(this.aimYaw) * 13.5F * MathUtils.cos(p * 0.85F)
               )
               .add(MathUtils.cos(this.aimYaw) * 2.8F, 0.0F, -MathUtils.sin(this.aimYaw) * 2.8F);
            this.camPos.y = Math.max(this.camPos.y, 2.2F);
            Vector3 cp = new Vector3(this.camPos);
            if (this.shake > 0.0F) {
               this.tA.set(this.rr(-1.0F, 1.0F), this.rr(-1.0F, 1.0F), 0.0F).scl(this.shake * 0.35F);
               cp.add(this.tA);
               this.shake = Math.max(0.0F, this.shake - dt * 2.2F);
            }

            this.aimPoint.set(this.camPos).mulAdd(this.aimDir, 450.0F);
            this.cam.position.set(cp);
            this.cam.direction.set(this.aimDir);
            this.cam.up.set(Vector3.Y);
            this.cam.update();
         }
      } else {
         float p0 = this.aimPitch;
         this.aimDir.set(-MathUtils.sin(this.aimYaw) * MathUtils.cos(p0), MathUtils.sin(p0), -MathUtils.cos(this.aimYaw) * MathUtils.cos(p0));
         this.aimPoint.set(AIM).mulAdd(this.aimDir, 450.0F);
         this.bossCamera(dt);
      }
   }

   public void step(float dt) {
      this.T += dt;
      this.root.updateGlobal(null);
      if (this.state.equals("over")) {
         this.updateWreck(dt);
      } else if (!this.state.equals("pause") && !this.state.equals("shop")) {
         if (this.state.equals("run") || this.state.equals("dying") || this.state.equals("result")) {
            for (int i = this.timers.size - 1; i >= 0; i--) {
               Game.Timer tm = (Game.Timer)this.timers.get(i);
               tm.t -= dt;
               if (tm.t <= 0.0F) {
                  this.timers.removeIndex(i);
                  tm.r.run();
               }
            }

            if (!this.state.equals("run")) {
               this.beamUsed = 0;
            } else {
               this.fireT -= dt;
               boolean gunOn = this.st("gun") > 0.0F && !this.VEH.equals("tor");
               if (this.firing && gunOn && !this.overheated) {
                  while (this.fireT <= 0.0F) {
                     this.fireT = this.fireT + 1.0F / this.st("rate");
                     this.fireBullet();
                     this.heat = this.heat + this.st("heatPer");
                  }

                  if (this.heat >= 1.0F) {
                     this.heat = 1.0F;
                     this.overheated = true;
                     G.sfx("alarm", 0.6F, 900);
                  }
               } else {
                  this.fireT = Math.max(this.fireT, 0.0F);
               }

               if (this.VEH.equals("tor")) {
                  this.chanT -= dt;
                  if (this.firing && this.chanT <= 0.0F && this.missiles > 0) {
                     Game.E t = this.lock.t != null && this.lock.k >= 0.5F ? this.lock.t : null;
                     if (this.fireMissile(t, false)) {
                        this.chanT = this.st("chan");
                        if (this.R.nextFloat() < this.st("twin") && this.missiles > 0) {
                           Game.E t2 = null;
                           ArrayIterator var5 = this.enemies.iterator();

                           while (var5.hasNext()) {
                              Game.E e = (Game.E)var5.next();
                              if (e != t && e.cloak == 0 && angleBetween(this.aimDir, this.tA.set(e.pos).sub(this.camPos)) < 0.5F) {
                                 t2 = e;
                                 break;
                              }
                           }

                           this.fireMissile(t2 != null ? t2 : t, false);
                        }
                     }
                  }
               }

               if (this.VEH.equals("luch")) {
                  this.updateBeam(dt);
               } else {
                  this.beamUsed = 0;
               }

               this.heat = Math.max(0.0F, this.heat - dt * this.st("cool") * (this.overheated ? 1.5F : 1.0F));
               if (this.overheated && this.heat < 0.3F) {
                  this.overheated = false;
               }

               if (this.missiles < this.st("maxMis") && !this.netView) {
                  this.reloadT -= dt;
                  if (this.reloadT <= 0.0F) {
                     this.missiles++;
                     this.reloadT = this.missiles < this.st("maxMis") ? this.st("reload") : 0.0F;
                     this.syncNoses();
                  }
               }

               if (this.comboT > 0.0F) {
                  this.comboT -= dt;
                  if (this.comboT <= 0.0F) {
                     this.combo = 0;
                  }
               }

               this.empT = Math.max(0.0F, this.empT - dt);
               this.smokeT = Math.max(0.0F, this.smokeT - dt);
               this.abCd.put("smoke", Math.max(0.0F, this.abCd.get("smoke") - dt));
               this.abCd.put("repair", Math.max(0.0F, this.abCd.get("repair") - dt));
               if (this.repairT > 0.0F && !this.netView) {
                  this.repairT -= dt;
                  this.hp = Math.min(this.st("maxHp"), this.hp + this.st("maxHp") / 3.0F / 4.0F * dt);
               }

               if (this.mode.equals("duel")) {
                  if (!this.netView) {
                     this.duelRules(dt);
                  }
               } else {
                  this.updateWaves(dt);
               }

               this.updateTut(dt);
               if (this.mode.equals("endless") && this.wave >= 6 && this.between > 0.0F && this.wxRoll != this.wave) {
                  this.wxRoll = this.wave;
                  if (this.R.nextFloat() < 0.3F) {
                     this.world.rain = this.R.nextFloat() < 0.6F ? 1.0F : 0.0F;
                     this.world.wind = this.world.rain > 0.0F ? this.rr(-60.0F, 60.0F) : 0.0F;
                  }
               }
            }

            if (!this.netView) {
               this.updateEnemies(dt);
            }

            this.updateWrecks(dt);
            this.updateJuice(dt);
            if (!this.netView) {
               this.updateEbul(dt);
               this.updateRockets(dt);
            }

            this.updateMogs(dt);
            this.updateObj(dt);
            this.updateLock(dt);
            if (this.state.equals("dying")) {
               this.deadT -= dt;
               if (this.deadT <= 0.0F) {
                  this.gameOver();
               }
            }

            float mh = this.st("maxHp");
            if (this.hp < mh * 0.65F && this.hp > mh * 0.35F && this.R.nextFloat() < 0.12F) {
               FX.puff(this.tmpV.set(this.rr(-1.0F, 1.0F), 3.2F, this.rr(0.0F, 3.0F)), 1.0F, new Color(0.35F, 0.35F, 0.35F, 1.0F), 0.45F, 2.0F, 2.5F);
            }

            if (this.hp < mh * 0.35F && this.hp > 0.0F && this.R.nextFloat() < 0.25F) {
               FX.puff(this.tmpV.set(this.rr(-1.0F, 1.0F), 3.5F, this.rr(0.0F, 3.0F)), 1.5F, new Color(0.12F, 0.12F, 0.12F, 1.0F), 0.6F, 2.5F, 3.0F);
               if (this.R.nextFloat() < 0.4F) {
                  FX.glow(this.tmpV.set(this.rr(-1.0F, 1.0F), 3.2F, this.rr(0.0F, 3.0F)), 1.5F, new Color(1.0F, 0.5F, 0.15F, 1.0F), 0.4F);
               }
            }
         }

         this.updateBullets(dt);
         this.updateWreck(dt);
         if (this.vm != null && !this.vm.broken) {
            this.vm.turret.setEuler(0.0F, this.state.equals("menu") ? MathUtils.sin(this.T * 0.4F) * 0.6F : this.aimYaw, 0.0F);
            this.vm.pitch.setEuler(this.state.equals("menu") ? 0.35F + MathUtils.sin(this.T * 0.5F) * 0.1F : this.aimPitch, 0.0F, 0.0F);
            this.vm.radar.rotateY(dt * 2.5F);
         }

         this.hurtT = Math.max(0.0F, this.hurtT - dt);
      }
   }

   public void updateSpot() {
      if (this.world.beam != null && this.world.beamOn) {
         Vector3 src = this.vm != null && !this.vm.broken
            ? this.vm.turret.toGlobal(new Vector3(0.0F, 1.6F, -1.2F), new Vector3())
            : new Vector3(0.0F, 4.0F, 0.0F);
         Vector3 tgt = this.state.equals("menu") ? new Vector3(-60.0F, 80.0F, -300.0F) : new Vector3(this.aimPoint);
         this.world.beam.pos.set(src);
         this.world.beam.lookDir(tgt.sub(src), Vector3.Y);
      }
   }

   void updateTut(float dt) {
      if (this.tut >= 0) {
         this.tutT -= dt;
         if (this.tutT <= 0.0F) {
            if (this.tut >= TUT.length) {
               this.tut = -1;
               G.SV.tut = true;
               G.save();
               return;
            }

            String msg = G.isMobile() ? TUT[this.tut] : TUT_PC[this.tut];
            if (!G.isMobile() && this.VEH.equals("tor") && this.tut == 1) {
               msg = "Держите левую кнопку мыши — «Молот» пускает ракеты по целям в прицеле";
            }

            if (G.isMobile() && this.VEH.equals("tor") && this.tut == 1) {
               msg = "Держите «Пуск» — «Молот» пускает ракеты по целям в прицеле";
            }

            if (this.VEH.equals("luch") && this.tut == 1) {
               msg = "Держите «Огонь» — «Луч» жжёт цель в прицеле. Следите за нагревом!";
            }

            if (!this.VEH.equals("pantsir") && this.tut == 2) {
               this.tut++;
               return;
            }

            if (this.ui != null) {
               this.ui.hint(msg);
            }

            this.tut++;
            this.tutT = 5.5F;
         }
      }
   }

   public void fillDyn() {
      this.fillBeams();
   }

   static {
      String[] k = new String[]{"storm", "manta", "colossus", "boss", "gunship", "walker", "carrier", "ghost", "hydra", "chimera"};
      float[] spd = new float[]{22.0F, 50.0F, 9.0F, 30.0F, 44.0F, 10.0F, 20.0F, 36.0F, 28.0F, 24.0F};
      float[] cd = new float[]{9.0F, 11.0F, 16.0F, 16.0F, 10.0F, 12.0F, 22.0F, 14.0F, 12.0F, 30.0F};
      float[] hp = new float[]{300.0F, 220.0F, 380.0F, 260.0F, 230.0F, 340.0F, 360.0F, 210.0F, 310.0F, 330.0F};
      String[] sh = new String[]{"МОЛНИЯ", "ПИКЕ", "КУПОЛ", "ЩИТ", "ФОРСАЖ", "ЗАЛП", "РОЙ+", "ТЕНЬ", "ВЕЕР", "УЗЛЫ"};
      String[] nm = new String[]{"Молния", "Пике", "Купол", "Щит", "Форсаж", "Артобстрел", "Большой рой", "Невидимость", "Веер ракет", "Узлы щита"};

      for (int i = 0; i < k.length; i++) {
         DUEL_SPD.put(k[i], spd[i]);
         SPEC_CD.put(k[i], cd[i]);
         DUEL_HP.put(k[i], hp[i]);
         SPEC_SHORT.put(k[i], sh[i]);
         SPEC_NAME.put(k[i], nm[i]);
      }

      MOGC = new Color(1.0F, 0.7F, 0.45F, 1.0F);
      TUT = new String[]{
         "Ведите пальцем по экрану — башня и прицел поворачиваются",
         "Держите кнопку «Огонь» — пушка стреляет туда, где прицел",
         "Наведите рамку на цель до надписи «Захват» и нажмите «Ракета»",
         "Красные стрелки по краям показывают врагов за экраном. Удачи!"
      };
      TUT_PC = new String[]{
         "Двигайте мышью — башня и прицел поворачиваются (кликните по экрану, если курсор виден)",
         "Держите левую кнопку мыши или пробел — пушка стреляет туда, где прицел",
         "Наведите рамку на цель до надписи «Захват» и нажмите правую кнопку мыши",
         "Красные стрелки по краям — враги за экраном. 1–4 — способности, B — улучшения. Удачи!"
      };
   }

   public static final class BossIn {
      public float mx;
      public float my;
      public float mz;
      public float cy;
      public int f1;
      public int f2;
      public int sp;
   }

   static final class Bullet {
      final Vector3 pos = new Vector3();
      final Vector3 prev = new Vector3();
      final Vector3 o = new Vector3();
      final Vector3 vel = new Vector3();
      float life;
      float dmg;
      boolean mog;
      boolean flak;
   }

   public static final class DmgNum {
      public Game.E e;
      public final Vector3 p = new Vector3();
      public float v;
      public float t;
      public String src;
   }

   public static final class E {
      public String type;
      public Models.EnemyModel md;
      public Node3 g;
      public final Vector3 pos = new Vector3();
      public final Vector3 vel = new Vector3();
      public float hp;
      public float max;
      public float r;
      public float t;
      public boolean boss;
      public boolean ground;
      public String phase = "in";
      public float fire;
      public float spawnT = 6.0F;
      public float side;
      public float sp;
      public float dmg;
      public String tgt = "base";
      public float data;
      public boolean inv;
      public int cloak;
      public float heal = 2.0F;
      public int split;
      public final Array<Game.E> nodes = new Array();
      public float anim;
      public float alt;
      public FX.Emitter smoke;
      public FX.Emitter trail;
      public float ang;
      public float dir;
      public float distR;
      public boolean stealth;
      public float reveal;
      public Game.E flock;
      public final Vector3 foff = new Vector3();
      public Game.E core;
      public float oa;
      public final Vector3 runV = new Vector3();
      public boolean hasRunV;
      public int runI = -1;
      public float rt;
      public float cloakT = 6.0F;
      public float reT = 22.0F;
      public float shT = 6.0F;
      public Node3 dome;
      public float flash;
      public boolean keep;
      public int id;
      public boolean pc;
      public boolean pcInv;
      public boolean net;
      public final Vector3 tgtPos = new Vector3();
      public final Quaternion tgtRot = new Quaternion();
      public boolean hasTgt;
   }

   static final class Ebul {
      final Vector3 pos = new Vector3();
      final Vector3 vel = new Vector3();
      float life;
      float dmg;
      String tgt;
   }

   public static final class Lock {
      public Game.E t;
      public float k;
      public boolean beeped;
   }

   public static final class Mission {
      public String name;
      public String kind;
      public float hp;
      public float max;
      public float r = 7.0F;
      public final Vector3 aim = new Vector3();
      public final Array<Objs.Obj> models = new Array();
   }

   static final class Rocket {
      Node3 node;
      Node3 flame;
      SmokeTrail trail;
      final Vector3 pos = new Vector3();
      final Vector3 vel = new Vector3();
      Game.E tgt;
      float life;
      float sp;
      float age;
   }

   public static final class Run {
      public int c;
      public Map<String, Integer> up;
      public Map<String, Boolean> ab;
   }

   static final class Timer {
      float t;
      Runnable r;
   }

   static final class Wreck {
      Node3 g;
      Node3 rot;
      final Vector3 vel = new Vector3();
      final Vector3 spin = new Vector3();
      float t;
      float s;
      boolean boss;
   }
}
