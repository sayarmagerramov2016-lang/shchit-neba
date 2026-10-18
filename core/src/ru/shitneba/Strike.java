package ru.shitneba;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Array.ArrayIterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import ru.shitneba.gfx.Dyn;
import ru.shitneba.gfx.Mat;
import ru.shitneba.gfx.Node3;

public class Strike {
   public static final Map<String, Strike.DroneDef> DRONES = new LinkedHashMap<>();
   public static final String[][] UPS = new String[][]{
      {"war", "Боевая часть", "+12% урона и +6% радиуса взрыва за уровень"},
      {"bat", "Аккумулятор", "+15% времени полёта и +3% скорости ускорения"},
      {"hull", "Корпус", "+20% прочности за уровень"},
      {"ecm", "Ловушки", "Меньше заметность, ловушки надёжнее; +1 ловушка на 2 и 4 уровне"}
   };
   public static final int[] UP_COST = new int[]{300, 700, 1300, 2200, 3500};
   static final int START_STOCK = 8;
   static final Map<String, Strike.AAD> AA = new LinkedHashMap<>();
   static final String[][] TG = new String[][]{
      {"depot", "Склад", "320", "300", ""},
      {"radar", "РЛС", "200", "250", "а"},
      {"hq", "Штаб", "480", "450", ""},
      {"ew", "Станция РЭБ", "220", "280", "а"},
      {"fuel", "Цистерны", "160", "200", "ы"}
   };
   final Game game;
   final World world;
   final PerspectiveCamera cam;
   public final Node3 root3 = new Node3();
   public boolean active;
   public boolean paused;
   public String st = "";
   public float T;
   public boolean bot;
   public int zone;
   public int score;
   public int stock;
   public int aaKills;
   public int tgKills;
   public int sorties;
   public int chain;
   public float chainT;
   public final Array<Strike.Obj> units = new Array();
   public final Array<Strike.Obj> targets = new Array();
   final Array<Strike.Shell> shells = new Array();
   final Array<Strike.Mis> mis = new Array();
   final Array<Strike.Flare> flares = new Array();
   final Array<Strike.Bomb> bombs = new Array();
   public final Array<Strike.Hunter> hunters = new Array();
   public Strike.DroneState D = new Strike.DroneState();
   public String DK = "osa";
   public Strike.Stats S = new Strike.Stats();
   public final Vector3 launchP = new Vector3();
   float launchYaw;
   float boomT;
   float clearT;
   final Vector3 boomP = new Vector3();
   public int camMode;
   public float alert;
   public float lockWarn;
   public float laserWarn;
   public float jam;
   boolean radAlive;
   public String big = "";
   public String bigSub = "";
   public float bigT;
   public final Array<Strike.Feed> feed = new Array();
   public float inX;
   public float inY;
   public float mdX;
   public float mdY;
   public boolean inBoost;
   public boolean inBrake;
   public float shake;
   public float hitT;
   public float flareCd;
   public float fade;
   int fadeHold;
   int zoneSorties0;
   public boolean resRec;
   public boolean resRankUp;
   public int resCoins;
   public int resXp;
   float lateOverT = -1.0F;
   float lateBlastT = -1.0F;
   final Vector3 lateBlastP = new Vector3();
   public GameUI ui;
   final Random R = new Random();
   public final Dyn tracers;
   public final Dyn beamDyn;
   final Vector3 camSmooth = new Vector3();
   final Vector3 tA = new Vector3();
   final Vector3 tB = new Vector3();
   final Vector3 tC = new Vector3();
   final Matrix4 tm = new Matrix4();
   float bt;

   static Strike.AAD aa(float hp, float det, float turn) {
      Strike.AAD a = new Strike.AAD();
      a.hp = hp;
      a.det = det;
      a.turn = turn;
      return a;
   }

   static String[] tg(String k) {
      for (String[] t : TG) {
         if (t[0].equals(k)) {
            return t;
         }
      }

      return TG[0];
   }

   public static String tgName(String k) {
      return tg(k)[1];
   }

   public Strike(Game g, World w, PerspectiveCamera cam) {
      this.game = g;
      this.world = w;
      this.cam = cam;
      this.root3.tag = FX.ROOT_TAG;
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
      bm.fog = false;
      bm.order = 4;
      this.beamDyn = new Dyn(bm);
   }

   float rr(float a, float b) {
      return a + this.R.nextFloat() * (b - a);
   }

   static float moveToward(float a, float b, float d) {
      return Math.abs(b - a) <= d ? b : a + Math.signum(b - a) * d;
   }

   static float angDiff(float from, float to) {
      float d = (to - from) % (float) (Math.PI * 2);
      if (d > (float) Math.PI) {
         d -= (float) (Math.PI * 2);
      }

      if (d < (float) -Math.PI) {
         d += (float) (Math.PI * 2);
      }

      return d;
   }

   public static Strike.Stats calcStats(String k) {
      G.Save s = G.SV;
      Strike.DroneDef b = DRONES.get(k);
      int w = up("war");
      int ba = up("bat");
      int h = up("hull");
      int e = up("ecm");
      Strike.Stats o = new Strike.Stats();
      o.sp = b.sp;
      o.min = b.min;
      o.turn = b.turn;
      o.wing = b.wing;
      o.bombs = b.bombs;
      o.dmg = b.dmg * (1.0F + 0.12F * w);
      o.rad = b.rad * (1.0F + 0.06F * w);
      o.bat = b.bat * (1.0F + 0.15F * ba);
      o.boost = b.boost * (1.0F + 0.03F * ba);
      o.hp = b.hp * (1.0F + 0.2F * h);
      o.flares = b.flares + (e >= 2 ? 1 : 0) + (e >= 4 ? 1 : 0);
      o.sig = b.sig * (1.0F - 0.05F * e);
      o.decoy = 0.7F + 0.05F * e;
      return o;
   }

   public static int up(String k) {
      Integer v = G.SV.swUp.get(k);
      return v == null ? 0 : v;
   }

   String vehSkin() {
      String var1 = this.world.themeName;
      switch (var1) {
         case "desert":
            return "desert";
         case "snow":
            return "winter";
         case "autumn":
            return "autumn";
         case "night":
            return "night";
         default:
            return "olive";
      }
   }

   static void noShadow(Node3 n) {
      n.forEach(k -> k.castShadow = false);
   }

   public void start() {
      this.DK = G.SV.swOwn.contains(G.SV.swPick) ? G.SV.swPick : "osa";
      this.S = calcStats(this.DK);
      this.camMode = G.SV.swCam;
      this.game.clearBattle();
      this.game.mode = "strike";
      this.game.state = "strike";
      if (this.game.vm != null) {
         this.game.vm.root.visible = false;
      }

      ArrayIterator n = this.game.mogs.iterator();

      while (n.hasNext()) {
         Models.Mog m = (Models.Mog)n.next();
         m.g.remove();
      }

      this.game.mogs.clear();
      int nx = G.D.get("THEMES").size;
      int ti = this.R.nextInt(nx);

      while (G.D.get("THEMES").get(ti).getString("name").equals("night")) {
         ti = this.R.nextInt(nx);
      }

      if (this.bot) {
         ti = 0;
      }

      this.world.build(ti, false, 0.0F, 0.0F, 0.0F, "", true);
      this.world.beamOn = false;
      if (this.world.beam != null) {
         this.world.beam.visible = false;
      }

      ArrayIterator var3 = this.world.stacks.iterator();

      while (var3.hasNext()) {
         Vector3 s = (Vector3)var3.next();
         FX.chimney(s);
      }

      this.root3.kids.clear();
      this.active = true;
      this.paused = false;
      this.fade = 0.0F;
      this.fadeHold = 0;
      this.zone = 0;
      this.score = 0;
      this.stock = 8;
      this.aaKills = 0;
      this.tgKills = 0;
      this.sorties = 0;
      this.chain = 0;
      this.feed.clear();
      this.lateOverT = -1.0F;
      this.lateBlastT = -1.0F;
      this.nextZone();
      this.zoneSorties0 = 0;
      this.launch();
   }

   public void exit() {
      this.clearZone();
      this.root3.kids.clear();
      this.active = false;
      this.paused = false;
      this.st = "";
      G.loop("buzz", 0.0F, 1.0F);
      this.game.mode = "endless";
      this.game.state = "menu";
      if (this.game.vm != null) {
         this.game.vm.root.visible = true;
      }
   }

   void clearZone() {
      ArrayIterator var1 = this.units.iterator();

      while (var1.hasNext()) {
         Strike.Obj o = (Strike.Obj)var1.next();
         o.g.remove();
      }

      var1 = this.targets.iterator();

      while (var1.hasNext()) {
         Strike.Obj o = (Strike.Obj)var1.next();
         o.g.remove();
      }

      var1 = this.hunters.iterator();

      while (var1.hasNext()) {
         Strike.Hunter h = (Strike.Hunter)var1.next();
         h.g.remove();
      }

      this.units.clear();
      this.targets.clear();
      this.hunters.clear();
      var1 = this.mis.iterator();

      while (var1.hasNext()) {
         Strike.Mis m = (Strike.Mis)var1.next();
         m.node.remove();
         m.trail.stop();
      }

      this.mis.clear();
      this.shells.clear();
      this.flares.clear();
      var1 = this.bombs.iterator();

      while (var1.hasNext()) {
         Strike.Bomb b = (Strike.Bomb)var1.next();
         b.g.remove();
      }

      this.bombs.clear();
      FX.clear();
      var1 = this.world.stacks.iterator();

      while (var1.hasNext()) {
         Vector3 s = (Vector3)var1.next();
         FX.chimney(s);
      }
   }

   Vector3 freeSpot(Array<Vector3> placed, float rmin, float rmax, float gap) {
      for (int tries = 0; tries < 60; tries++) {
         float a = this.R.nextFloat() * (float) (Math.PI * 2);
         float r = this.rr(rmin, rmax);
         float x = MathUtils.sin(a) * r;
         float z = MathUtils.cos(a) * r;
         if (!(Math.abs(x - -42.0F) < 9.0F)) {
            boolean ok = true;
            ArrayIterator var11 = placed.iterator();

            while (var11.hasNext()) {
               Vector3 q = (Vector3)var11.next();
               if (q.dst(x, 0.0F, z) < gap) {
                  ok = false;
                  break;
               }
            }

            if (ok) {
               return new Vector3(x, this.world.height(x, z), z);
            }
         }
      }

      float a2 = this.R.nextFloat() * (float) (Math.PI * 2);
      float x = MathUtils.sin(a2) * rmax;
      float z = MathUtils.cos(a2) * rmax;
      return new Vector3(x, this.world.height(x, z), z);
   }

   void nextZone() {
      this.clearZone();
      this.zone++;
      Array<Vector3> placed = new Array();
      int nt = Math.min(5, 2 + this.zone / 2);
      Array<String> pool = new Array(new String[]{"depot", "fuel", "radar"});
      if (this.zone >= 2) {
         pool.add("hq");
      }

      if (this.zone >= 3) {
         pool.add("ew");
      }

      for (int i = 0; i < nt; i++) {
         String k = i == 0 && this.zone % 3 == 0 ? "hq" : (String)pool.get(this.R.nextInt(pool.size));
         Vector3 p = this.freeSpot(placed, 34.0F, 120.0F, 28.0F);
         placed.add(new Vector3(p.x, 0.0F, p.z));
         SModels.Target md = SModels.target(k, this.world.themeName);
         Node3 g = md.g;
         this.root3.add(g);
         g.pos.set(p);
         g.setEuler(0.0F, this.R.nextFloat() * (float) (Math.PI * 2), 0.0F);
         float hpk = 1.0F + this.zone * 0.05F;
         Strike.Obj o = new Strike.Obj();
         o.kind = k;
         o.g = g;
         o.spin = md.spin;
         o.pos.set(p);
         o.hp = Float.parseFloat(tg(k)[2]) * hpk;
         o.max = o.hp;
         o.r = md.r;
         o.hr = md.hr;
         o.unit = false;
         this.targets.add(o);
      }

      int na = Math.min(10, 2 + (int)(this.zone * 0.7F));
      placed.add(new Vector3());
      Array<String> apool = new Array(new String[]{"shilka", "pantsir"});
      if (this.zone >= 2) {
         apool.add("tor");
      }

      if (this.zone >= 3) {
         apool.add("grom");
      }

      if (this.zone >= 4) {
         apool.add("luch");
      }

      String skin = this.vehSkin();

      for (int i = 0; i < na; i++) {
         String k = i < apool.size ? (String)apool.get(i % apool.size) : (String)apool.get(this.R.nextInt(apool.size));
         Vector3 p = i > 0 ? this.freeSpot(placed, 28.0F, 175.0F, 24.0F) : new Vector3(0.0F, this.world.height(0.0F, 0.0F), 0.0F);
         if (i == 0) {
            k = "pantsir";
         }

         placed.add(new Vector3(p.x, 0.0F, p.z));
         Models.Vehicle v = Models.vehicle(k, skin);
         noShadow(v.root);
         this.root3.add(v.root);
         v.root.pos.set(p);
         float ry = this.R.nextFloat() * (float) (Math.PI * 2);
         v.root.setEuler(0.0F, ry, 0.0F);
         Strike.AAD A = AA.get(k);
         float hpk = 1.0F + this.zone * 0.04F;
         Strike.Obj u = new Strike.Obj();
         u.kind = k;
         u.g = v.root;
         u.turret = v.turret;
         u.pitch = v.pitch;
         u.radar = v.radar;
         u.barrels = v.barrels;
         u.noses = v.noses;
         u.pos.set(p);
         u.ry = ry;
         u.hp = A.hp * hpk;
         u.max = u.hp;
         u.r = 3.2F;
         u.hr = 2.4F;
         u.unit = true;
         u.yaw = this.R.nextFloat() * (float) (Math.PI * 2);
         u.mcd = this.rr(1.0F, 3.0F);
         this.units.add(u);
      }

      int nh = MathUtils.clamp((int)((this.zone - 2) * 0.7F + 0.5F), 0, 4);

      for (int i = 0; i < nh; i++) {
         Models.EnemyModel md = Models.enemy("quad", "harpy");
         this.root3.add(md.g);
         float a = (float)i / Math.max(1, nh) * (float) (Math.PI * 2);
         Strike.Hunter h = new Strike.Hunter();
         h.g = md.g;
         h.rot = md.rot;
         h.ph = a;
         h.pos.set(MathUtils.sin(a) * 90.0F, this.world.height(MathUtils.sin(a) * 90.0F, MathUtils.cos(a) * 90.0F) + 55.0F, MathUtils.cos(a) * 90.0F);
         md.g.pos.set(h.pos);
         this.hunters.add(h);
      }

      this.updateRad();
      float la = this.R.nextFloat() * (float) (Math.PI * 2);
      float ld = this.zone == 1 ? 560.0F : Math.min(760.0F, 600.0F + this.zone * 20.0F);
      this.launchP.set(MathUtils.sin(la) * ld, 0.0F, MathUtils.cos(la) * ld);
      this.launchP.y = this.world.height(this.launchP.x, this.launchP.z);
      this.launchYaw = MathUtils.atan2(this.launchP.x, this.launchP.z);
      this.big = "РУБЕЖ " + this.zone;
      this.bigSub = String.format("Целей: %d · ПВО: %d%s", nt, na, nh > 0 ? " · перехватчиков: " + nh : "");
      this.bigT = 3.2F;
      G.sfx("wave");
   }

   void updateRad() {
      this.radAlive = false;

      for (int i = 0; i < this.targets.size; i++) {
         if (((Strike.Obj)this.targets.get(i)).alive && ((Strike.Obj)this.targets.get(i)).kind.equals("radar")) {
            this.radAlive = true;
         }
      }
   }

   void launch() {
      if (this.stock > 0) {
         this.stock--;
         this.sorties++;
         if (this.D.g != null) {
            this.D.g.remove();
         }

         SModels.Drone md = SModels.drone(this.DK);
         this.root3.add(md.g);
         this.D = new Strike.DroneState();
         this.D.g = md.g;
         this.D.rot = md.rot;
         this.D.bombs = md.bombs;
         this.D.r = md.r;
         this.D.pos.set(this.launchP).add(0.0F, 22.0F, 0.0F);
         this.D.yaw = this.launchYaw;
         this.D.pitch = -0.05F;
         this.D.sp = this.S.sp * 0.6F;
         this.D.hp = this.S.hp;
         this.D.max = this.S.hp;
         this.D.bat = this.S.bat;
         this.D.bmax = this.S.bat;
         this.D.flares = this.S.flares;
         this.D.nb = this.S.bombs;
         this.D.alive = true;
         this.D.fall = false;
         FX.puff(new Vector3(this.launchP).add(0.0F, 1.0F, 0.0F), 3.0F, new Color(0.5F, 0.45F, 0.4F, 1.0F), 0.5F, 2.0F, 1.0F);
         this.st = "fly";
         this.feed("Дрон " + this.sorties + " в воздухе", C("3ff0c8"), 0);
         G.sfx("click");
      }
   }

   static Color C(String h) {
      return G.hex(h);
   }

   public Vector3 fwd(Vector3 out) {
      return out.set(
         -MathUtils.sin(this.D.yaw) * MathUtils.cos(this.D.pitch), MathUtils.sin(this.D.pitch), -MathUtils.cos(this.D.yaw) * MathUtils.cos(this.D.pitch)
      );
   }

   void fly(float dt) {
      boolean wing = this.S.wing;
      float tx = this.inX;
      float ty = this.inY;
      if (this.jam > 0.05F) {
         tx += MathUtils.sin(this.T * 3.1F) * this.jam * 0.35F;
         ty += MathUtils.cos(this.T * 2.3F) * this.jam * 0.25F;
      }

      float inv = G.SV.swInv ? -1.0F : 1.0F;
      this.D.yaw = this.D.yaw - (tx * this.S.turn * dt + this.mdX);
      this.D.pitch = MathUtils.clamp(this.D.pitch + (ty * this.S.turn * 0.75F * dt + this.mdY) * inv, -1.45F, wing ? 0.8F : 1.1F);
      float mdx0 = this.mdX;
      this.mdX = 0.0F;
      this.mdY = 0.0F;
      float agl = this.D.pos.y - this.world.height(this.D.pos.x, this.D.pos.z);
      if (agl > 260.0F) {
         this.D.pitch = Math.min(this.D.pitch, 0.0F);
      }

      boolean boosting = this.inBoost && this.D.bat > 0.0F && !this.D.fall;
      float want = this.S.sp;
      if (boosting) {
         want = this.S.boost;
      } else if (this.inBrake && !wing) {
         want = this.S.min;
      }

      this.D.sp = moveToward(this.D.sp, want, (want > this.D.sp ? 28.0F : 36.0F) * dt);
      if (wing) {
         this.D.sp = MathUtils.clamp(this.D.sp - MathUtils.sin(this.D.pitch) * 16.0F * dt, this.S.min, this.S.boost * 1.3F);
      }

      this.D.bat -= dt * (boosting ? 2.6F : 1.0F);
      if (this.D.bat <= 0.0F && !this.D.fall) {
         this.D.bat = 0.0F;
         this.D.fall = true;
         this.feed("Батарея разряжена!", C("ff5a47"), 0);
         G.sfx("alarm", 0.6F);
      }

      if (this.D.fall) {
         this.D.pitch = moveToward(this.D.pitch, -1.3F, dt * 0.7F);
         this.D.sp = moveToward(this.D.sp, 30.0F, dt * 10.0F);
      }

      Vector3 f = this.fwd(this.tA);
      this.D.vel.set(f).scl(this.D.sp);
      this.D.pos.mulAdd(this.D.vel, dt);
      this.D.roll = MathUtils.lerp(this.D.roll, -(tx + mdx0 * 20.0F) * (wing ? 0.75F : 0.45F), Math.min(1.0F, dt * 5.0F));
      float visP = wing ? this.D.pitch : MathUtils.clamp(this.D.pitch, -1.2F, 0.7F) * 0.55F - 0.3F * this.D.sp / this.S.boost;
      this.D.g.pos.set(this.D.pos);
      this.D.g.setEuler(visP, this.D.yaw, this.D.roll);
      ArrayIterator i = this.D.rot.iterator();

      while (i.hasNext()) {
         Node3 r = (Node3)i.next();
         r.rotateLocal(Vector3.Y, dt * 60.0F);
      }

      G.loop("buzz", this.camMode == 0 ? 0.28F : 0.16F, 0.75F + this.D.sp / 60.0F + (boosting ? 0.15F : 0.0F));
      if (boosting && this.R.nextFloat() < 0.3F) {
         FX.puff(this.tB.set(this.D.pos).mulAdd(f, -2.0F), 0.4F, new Color(0.6F, 0.6F, 0.6F, 1.0F), 0.25F, 0.6F, 0.0F);
      }

      if (this.D.hp < this.D.max * 0.5F && this.R.nextFloat() < 0.3F) {
         FX.puff(this.D.pos, 0.6F, new Color(0.15F, 0.15F, 0.15F, 1.0F), 0.5F, 1.4F, 1.0F);
      }

      if (this.D.pos.y < this.world.height(this.D.pos.x, this.D.pos.z) + 0.5F) {
         this.detonate(this.D.pos, null, 1.0F);
      } else {
         for (int pass = 0; pass < 2; pass++) {
            Array<Strike.Obj> a = pass == 0 ? this.targets : this.units;

            for (int ix = 0; ix < a.size; ix++) {
               Strike.Obj o = (Strike.Obj)a.get(ix);
               if (o.alive && this.tB.set(o.pos).add(0.0F, o.hr, 0.0F).dst(this.D.pos) < o.r + this.D.r) {
                  this.detonate(this.D.pos, o, 1.0F);
                  return;
               }
            }
         }

         this.jam = 0.0F;

         for (int ixx = 0; ixx < this.targets.size; ixx++) {
            Strike.Obj t = (Strike.Obj)this.targets.get(ixx);
            if (t.alive && t.kind.equals("ew")) {
               this.jam = Math.max(this.jam, MathUtils.clamp(1.0F - t.pos.dst(this.D.pos) / 280.0F, 0.0F, 1.0F));
            }
         }
      }
   }

   public void useFlares() {
      if (this.st.equals("fly") && this.D.alive && this.D.flares > 0 && !(this.flareCd > 0.0F)) {
         this.D.flares--;
         this.flareCd = 1.2F;
         G.sfx("emp", 0.4F);
         Vector3 f = this.fwd(new Vector3());
         Array<Strike.Flare> made = new Array();

         for (int i = 0; i < 4; i++) {
            Strike.Flare o = new Strike.Flare();
            o.vel.set(this.D.vel).scl(0.4F).add(this.rr(-9.0F, 9.0F), this.rr(-2.0F, 6.0F), this.rr(-9.0F, 9.0F)).mulAdd(f, -8.0F);
            o.pos.set(this.D.pos).mulAdd(f, -1.5F);
            o.life = 3.2F;
            this.flares.add(o);
            made.add(o);
         }

         ArrayIterator var5 = this.mis.iterator();

         while (var5.hasNext()) {
            Strike.Mis m = (Strike.Mis)var5.next();
            if (m.atDrone && m.pos.dst(this.D.pos) < 600.0F && this.R.nextFloat() < this.S.decoy) {
               m.atDrone = false;
               m.tgt = (Strike.Flare)made.get(this.R.nextInt(4));
            }
         }

         this.feed("Ловушки отстрелены", C("ffd27a"), 0);
      }
   }

   public void dropBomb() {
      if (this.st.equals("fly") && this.D.alive && this.D.nb > 0) {
         this.D.nb--;
         if (this.D.nb < this.D.bombs.size) {
            ((Node3)this.D.bombs.get(this.D.nb)).visible = false;
         }

         Strike.Bomb b = new Strike.Bomb();
         b.g = SModels.bombFree();
         this.root3.add(b.g);
         b.pos.set(this.D.pos).add(0.0F, -0.8F, 0.0F);
         b.vel.set(this.D.vel).scl(0.9F);
         b.g.pos.set(b.pos);
         this.bombs.add(b);
         G.sfx("click", 0.6F);
      }
   }

   public void detonateNow() {
      if (this.st.equals("fly") && this.D.alive) {
         this.detonate(this.D.pos, null, 1.0F);
      }
   }

   float diveBonus() {
      float k = 1.0F;
      if (this.S.wing) {
         k += MathUtils.clamp((this.D.sp - this.S.sp) / 60.0F, 0.0F, 0.4F);
         if (this.D.pitch < -0.6F) {
            k += 0.15F;
         }
      }

      return k;
   }

   void detonate(Vector3 p, Strike.Obj direct, float k) {
      if (this.D.alive) {
         float dmg = this.S.dmg * k * this.diveBonus();
         float rad = this.S.rad;
         FX.explosion(p, 1.4F + rad / 9.0F, true);
         G.sfx("boom");
         G.hap(60);
         this.shake = 1.0F;
         this.D.alive = false;
         if (this.D.g != null) {
            this.D.g.visible = false;
         }

         G.loop("buzz", 0.0F, 1.0F);
         this.st = "boom";
         this.boomT = 2.6F;
         this.boomP.set(p);
         this.blast(p, dmg, rad, direct);
      }
   }

   void blast(Vector3 p, float dmg, float rad, Strike.Obj direct) {
      for (int pass = 0; pass < 2; pass++) {
         Array<Strike.Obj> a = new Array(pass == 0 ? this.targets : this.units);
         ArrayIterator var7 = a.iterator();

         while (var7.hasNext()) {
            Strike.Obj o = (Strike.Obj)var7.next();
            if (o.alive) {
               float d = this.tB.set(o.pos).add(0.0F, o.hr, 0.0F).dst(p) - o.r;
               if (o == direct) {
                  this.hurt(o, dmg);
               } else if (d < rad) {
                  this.hurt(o, dmg * (1.0F - MathUtils.clamp(d / rad, 0.0F, 1.0F)) * 0.85F);
               }
            }
         }
      }

      for (int i = 0; i < this.hunters.size; i++) {
         Strike.Hunter h = (Strike.Hunter)this.hunters.get(i);
         if (h.alive && h.pos.dst(p) < rad * 1.6F) {
            this.killHunter(h);
         }
      }
   }

   void hurt(Strike.Obj o, float dmg) {
      if (o.alive && !(dmg <= 0.0F)) {
         o.hp -= dmg;
         FX.sparks(this.tC.set(o.pos).add(0.0F, o.hr, 0.0F), 8);
         if (o.hp <= 0.0F) {
            this.destroy(o);
         }
      }
   }

   void destroy(Strike.Obj o) {
      o.alive = false;
      Vector3 c = new Vector3(o.pos).add(0.0F, o.hr, 0.0F);
      FX.explosion(c, 2.6F, true);
      G.sfx("boom", 0.9F);
      Node3 g = o.g;
      float ry = MathUtils.atan2(g.rot.val[8], g.rot.val[10]);
      g.setEuler(this.rr(-0.08F, 0.08F), ry, this.rr(-0.12F, 0.12F));
      g.scl.set(1.0F, 0.7F, 1.0F);
      Node3 a1 = new Node3();
      a1.pos.set(0.0F, o.hr + 1.0F, 0.0F);
      g.add(a1);
      FX.burner(a1, true, 2.2F);
      Node3 a2 = new Node3();
      a2.pos.set(0.0F, o.hr + 2.5F, 0.0F);
      g.add(a2);
      FX.burner(a2, false, 3.0F);
      this.chain = this.chainT > 0.0F ? this.chain + 1 : 1;
      this.chainT = 7.0F;
      float mul = 1.0F + 0.25F * (this.chain - 1);
      int pts;
      if (o.unit) {
         this.aaKills++;
         o.turret.setEuler(this.rr(-0.3F, 0.3F), o.yaw - o.ry, this.rr(-0.4F, 0.4F));
         pts = 180 + this.zone * 10;
         this.feed(G.vehName(o.kind) + " уничтожен", C("ff9a6a"), (int)(pts * mul));
         o.beam = false;
      } else {
         this.tgKills++;
         String[] t = tg(o.kind);
         pts = Integer.parseInt(t[3]) + this.zone * 20;
         this.feed(t[1] + " уничтожен" + t[4], C("ffd27a"), (int)(pts * mul));
         if (o.kind.equals("radar")) {
            if (o.spin != null) {
               o.spin.setEuler(0.0F, 0.0F, 1.0F);
            }

            this.updateRad();
            if (!this.radAlive) {
               this.feed("РЛС подавлены: ПВО видят хуже", C("3ff0c8"), 0);
            }
         }

         if (o.kind.equals("ew")) {
            this.feed("РЭБ подавлена: сигнал чистый", C("3ff0c8"), 0);
         }

         if (o.kind.equals("fuel")) {
            FX.explosion(new Vector3(c).add(3.0F, 2.0F, 0.0F), 3.2F, true);
            this.lateBlastT = 0.25F;
            this.lateBlastP.set(c);
         }
      }

      this.score += (int)(pts * mul);
      if (this.chain >= 2) {
         this.feed("Серия ×" + this.chain, C("3ff0c8"), 0);
      }

      G.sfx("coin", 0.5F);
      if (!o.unit && this.targetsLeft() == 0) {
         this.zoneClear();
      }
   }

   void zoneClear() {
      if (!this.st.equals("clear") && !this.st.equals("over")) {
         int bonus = 400 + this.zone * 150;
         int back = 0;
         if (this.D.alive) {
            back = 1;
            this.D.alive = false;
            if (this.D.g != null) {
               this.D.g.visible = false;
            }

            G.loop("buzz", 0.0F, 1.0F);
         }

         int add = 4 + back + (this.sorties - this.zoneSorties0 <= 1 ? 2 : 0);
         this.stock += add;
         this.score += bonus;
         this.big = "РУБЕЖ " + this.zone + " ПРОРВАН";
         this.bigSub = String.format("+%d очков · +%d дрон%s%s", bonus, add, add < 5 ? "а" : "ов", back > 0 ? " · дрон вернулся" : "");
         this.bigT = 3.5F;
         G.sfx("clear");
         this.st = "clear";
         this.clearT = 3.6F;
         Float z = G.SV.stats.get("swZone");
         if (z == null || z < this.zone) {
            G.SV.stats.put("swZone", (float)this.zone);
         }
      }
   }

   void dhurt(float dmg, String src) {
      if (this.D.alive && this.st.equals("fly")) {
         this.D.hp -= dmg;
         this.hitT = 0.35F;
         this.shake = Math.max(this.shake, 0.35F);
         G.sfx("tink", 0.6F, 60);
         G.hap(20);
         if (this.D.hp <= 0.0F) {
            this.feed("Дрон сбит: " + src, C("ff5a47"), 0);
            this.detonate(this.D.pos, null, 0.5F);
         }
      }
   }

   void feed(String t, Color col, int pts) {
      Strike.Feed f = new Strike.Feed();
      f.t = t + (pts > 0 ? "  +" + pts : "");
      f.c = col;
      f.life = 5.0F;
      this.feed.insert(0, f);
      if (this.feed.size > 6) {
         this.feed.pop();
      }
   }

   boolean los(Vector3 a, Vector3 b) {
      for (int i = 1; i < 9; i++) {
         float k = i / 9.0F;
         float x = MathUtils.lerp(a.x, b.x, k);
         float y = MathUtils.lerp(a.y, b.y, k);
         float z = MathUtils.lerp(a.z, b.z, k);
         if (y < this.world.height(x, z) - 0.5F) {
            return false;
         }
      }

      return true;
   }

   Vector3 lead(Vector3 from, float speed, Vector3 out) {
      float t = from.dst(this.D.pos) / speed;
      return out.set(this.D.pos).mulAdd(this.D.vel, t * 0.95F);
   }

   void updateUnits(float dt) {
      this.alert = Math.max(0.0F, this.alert - dt);
      this.lockWarn = Math.max(0.0F, this.lockWarn - dt);
      this.laserWarn = Math.max(0.0F, this.laserWarn - dt);
      float zk = 1.0F + this.zone * 0.03F;

      for (int ui0 = 0; ui0 < this.units.size; ui0++) {
         Strike.Obj u = (Strike.Obj)this.units.get(ui0);
         if (u.alive) {
            Strike.AAD A = AA.get(u.kind);
            Vector3 c = this.tC.set(u.pos).add(0.0F, 3.0F, 0.0F);
            boolean see = false;
            float d = 99999.0F;
            if (this.D.alive && this.st.equals("fly")) {
               d = c.dst(this.D.pos);
               float agl = this.D.pos.y - this.world.height(this.D.pos.x, this.D.pos.z);
               float low = MathUtils.clamp((agl - 8.0F) / 32.0F, 0.0F, 1.0F) * 0.68F + 0.32F;
               float det = A.det * zk * this.S.sig * low * (this.radAlive ? 1.25F : 1.0F) * (agl > 170.0F ? 1.3F : 1.0F);
               if (d < det || d < 45.0F) {
                  see = this.los(c, this.D.pos);
               }

               if (d < 320.0F) {
                  u.known = true;
               }
            }

            u.see = see;
            u.track = see ? Math.min(u.track + dt, 5.0F) : Math.max(u.track - dt * 0.8F, 0.0F);
            if (see) {
               this.alert = 0.6F;
               u.known = true;
            }

            if (u.track > 0.25F && this.D.alive) {
               Vector3 lp = A.gun > 0.0F ? this.lead(c, 400.0F, this.tB) : this.tB.set(this.D.pos);
               float vx = lp.x - c.x;
               float vy = lp.y - c.y;
               float vz = lp.z - c.z;
               float wy = MathUtils.atan2(-vx, -vz);
               float wp = MathUtils.atan2(vy, (float)Math.sqrt(vx * vx + vz * vz));
               u.yaw = u.yaw + MathUtils.clamp(angDiff(u.yaw, wy), -A.turn * dt, A.turn * dt);
               u.pit = MathUtils.clamp(moveToward(u.pit, wp, A.turn * 0.7F * dt), -0.05F, 1.3F);
            } else {
               u.yaw += dt * 0.35F;
               u.pit = moveToward(u.pit, 0.35F, dt * 0.3F);
            }

            u.turret.setEuler(0.0F, u.yaw - u.ry, 0.0F);
            u.pitch.setEuler(u.pit, 0.0F, 0.0F);
            u.radar.rotateY(dt * (u.track > 0.0F ? 5.0F : 2.0F));
            u.cd = Math.max(u.cd - dt, -0.15F);
            u.mcd -= dt;
            float aimErr = 1.0F;
            if (this.D.alive) {
               aimErr = Math.abs(angDiff(u.yaw, MathUtils.atan2(-(this.D.pos.x - c.x), -(this.D.pos.z - c.z))));
            }

            if (A.gun > 0.0F && u.track > 1.3F - Math.min(0.5F, this.zone * 0.05F) && d < A.gun && aimErr < 0.3F) {
               if (u.burst > 0.0F) {
                  u.burst -= dt;

                  while (u.cd <= 0.0F) {
                     u.cd = u.cd + 1.0F / A.rate;
                     this.unitShot(u, A, d);
                  }

                  if (u.burst <= 0.0F) {
                     u.rest = this.rr(0.4F, 0.9F);
                  }
               } else {
                  u.rest -= dt;
                  u.cd = Math.max(u.cd, 0.0F);
                  if (u.rest <= 0.0F) {
                     u.burst = this.rr(0.9F, 1.6F);
                  }
               }
            }

            if (A.mis > 0 && u.track > A.lock * 0.5F && d < A.det * zk) {
               this.lockWarn = 0.4F;
            }

            if (A.mis > 0 && u.track > A.lock && d < A.det * zk && u.mcd <= 0.0F) {
               int mine = 0;

               for (int k = 0; k < this.mis.size; k++) {
                  if (((Strike.Mis)this.mis.get(k)).src == u) {
                     mine++;
                  }
               }

               if (mine < (this.zone < 6 ? 1 : A.mis)) {
                  this.launchMissile(u);
                  u.mcd = A.mcd * Math.max(0.7F, 1.15F - this.zone * 0.03F);
               }
            }

            u.beam = false;
            if (A.laser > 0.0F) {
               if (u.over) {
                  u.heat -= dt / 2.2F;
                  if (u.heat < 0.2F) {
                     u.over = false;
                  }
               } else if (u.track > 1.1F && d < A.laser && aimErr < 0.35F) {
                  u.beam = true;
                  u.heat += dt / 3.2F;
                  this.laserWarn = 0.3F;
                  this.dhurt(A.ldps * Math.min(1.5F, 0.6F + this.zone * 0.08F) * dt, "лазер");
                  if (this.R.nextFloat() < 0.3F && this.D.alive) {
                     FX.sparks(this.D.pos, 2, new Color(1.0F, 0.4F, 0.3F, 1.0F));
                  }

                  if (u.heat >= 1.0F) {
                     u.over = true;
                  }
               } else {
                  u.heat = Math.max(0.0F, u.heat - dt / 3.0F);
               }
            }

            if (A.flak > 0.0F && u.track > 0.8F && d < A.flak && u.cd <= 0.0F && aimErr < 0.4F) {
               u.cd = 1.0F / A.frate;
               Vector3 from = u.barrels.size > 0 ? u.pitch.toGlobal((Vector3)u.barrels.get(0), new Vector3()) : new Vector3(u.pos).add(0.0F, 4.0F, 0.0F);
               Vector3 lp2 = this.lead(from, 300.0F, new Vector3()).add(this.rr(-4.0F, 4.0F), this.rr(-3.0F, 3.0F), this.rr(-4.0F, 4.0F));
               Strike.Shell s = new Strike.Shell();
               s.pos.set(from);
               s.prev.set(from);
               s.o.set(from);
               s.vel.set(lp2).sub(from).nor().scl(300.0F);
               s.life = from.dst(lp2) / 300.0F + this.rr(-0.06F, 0.06F);
               s.dmg = A.fdmg * Math.min(1.5F, 0.6F + this.zone * 0.08F);
               s.flak = true;
               s.rad = A.frad;
               this.shells.add(s);
               FX.glow(from, 4.0F, new Color(1.0F, 0.85F, 0.5F, 1.0F), 0.06F);
               G.sfx("flak", MathUtils.clamp(90.0F / Math.max(d, 1.0F), 0.08F, 0.6F), 80);
            }
         }
      }
   }

   void unitShot(Strike.Obj u, Strike.AAD A, float d) {
      u.bi = (u.bi + 1) % Math.max(1, u.barrels.size);
      Vector3 from = u.barrels.size > 0 ? u.pitch.toGlobal((Vector3)u.barrels.get(u.bi), new Vector3()) : new Vector3(u.pos).add(0.0F, 4.0F, 0.0F);
      Vector3 v = this.lead(from, 420.0F, new Vector3()).sub(from).nor();
      float sp = A.spread + this.D.sp / 56.0F * 0.012F;
      v.add(this.rr(-1.0F, 1.0F) * sp, this.rr(-1.0F, 1.0F) * sp, this.rr(-1.0F, 1.0F) * sp).nor().scl(420.0F);
      Strike.Shell s = new Strike.Shell();
      s.pos.set(from);
      s.prev.set(from);
      s.o.set(from);
      s.vel.set(v);
      s.life = 1.6F;
      s.dmg = A.gdmg * Math.min(1.5F, 0.4F + this.zone * 0.1F);
      this.shells.add(s);
      if (this.R.nextFloat() < 0.5F) {
         FX.glow(from, 2.4F, new Color(1.0F, 0.8F, 0.5F, 1.0F), 0.05F);
      }

      G.sfx(u.kind.equals("shilka") ? "shot2" : "shot", MathUtils.clamp(60.0F / Math.max(d, 1.0F), 0.05F, 0.5F), 45);
   }

   void launchMissile(Strike.Obj u) {
      Vector3 from = u.noses.size > 0
         ? u.pitch.toGlobal((Vector3)u.noses.get(this.R.nextInt(u.noses.size)), new Vector3())
         : new Vector3(u.pos).add(0.0F, 5.0F, 0.0F);
      float[] gm = u.pitch.global.val;
      Vector3 fw = new Vector3(-gm[8], -gm[9], -gm[10]).nor();
      Node3 node = new Node3();
      node.add(new Node3(Models.missileMesh(), Models.MAT_MISSILE));
      Node3 fl = FX.flame(1.78F);
      node.add(fl);
      SmokeTrail trl = new SmokeTrail(true);
      this.root3.add(node);
      node.pos.set(from);
      node.lookDir(fw, Math.abs(fw.y) < 0.98F ? Vector3.Y : Vector3.X);
      FX.launchBlast(from, fw);
      Strike.Mis m = new Strike.Mis();
      m.node = node;
      m.trail = trl;
      m.flame = fl;
      m.pos.set(from);
      m.vel.set(fw).scl(30.0F);
      m.sp = 30.0F;
      m.atDrone = true;
      m.life = 9.0F;
      m.last.set(this.D.pos);
      m.src = u;
      this.mis.add(m);
      G.sfx("missile", 0.7F);
      G.sfx("alarm", 0.5F, 400);
      this.feed("ПУСК РАКЕТЫ! Ловушки или укрытие", C("ff5a47"), 0);
   }

   void updateMissiles(float dt) {
      for (int i = this.mis.size - 1; i >= 0; i--) {
         Strike.Mis m = (Strike.Mis)this.mis.get(i);
         m.life -= dt;
         m.age += dt;
         m.sp = Math.min(125.0F, m.sp + (m.age < 0.3F ? 40.0F : 110.0F) * dt);
         Vector3 dir = this.tA.set(m.vel).nor();
         Vector3 aim = null;
         if (m.atDrone) {
            if (this.D.alive && this.st.equals("fly")) {
               if (m.age % 0.2F < dt && this.los(m.pos, this.D.pos)) {
                  m.last.set(this.D.pos).mulAdd(this.D.vel, m.pos.dst(this.D.pos) / Math.max(m.sp, 1.0F) * 0.8F);
               }

               aim = m.last;
            }
         } else if (m.tgt != null && m.tgt.life > 0.0F) {
            aim = m.tgt.pos;
         }

         if (aim != null && m.age > 0.25F) {
            Vector3 wd = this.tB.set(aim).sub(m.pos).nor();
            float ang = Game.angleBetween(dir, wd);
            float mt = 2.7F * dt;
            if (ang > 1.0E-4F) {
               Game.slerp(dir, wd, Math.min(1.0F, mt / ang));
            }
         }

         m.vel.set(dir).scl(m.sp);
         Vector3 prev = new Vector3(m.pos);
         m.pos.mulAdd(m.vel, dt);
         m.node.pos.set(m.pos);
         m.node.lookDir(dir, Math.abs(dir.y) < 0.98F ? Vector3.Y : Vector3.X);
         m.trail.emit(this.tC.set(m.pos).mulAdd(dir, -1.9F));
         float fk = this.rr(0.75F, 1.25F);
         m.flame.scl.set(fk, fk, fk * Math.min(1.0F, m.age * 6.0F + 0.3F));
         boolean boom = false;
         if (this.D.alive && this.st.equals("fly") && m.atDrone) {
            float abx = m.pos.x - prev.x;
            float aby = m.pos.y - prev.y;
            float abz = m.pos.z - prev.z;
            float l2 = Math.max(abx * abx + aby * aby + abz * abz, 1.0E-6F);
            float t = MathUtils.clamp(((this.D.pos.x - prev.x) * abx + (this.D.pos.y - prev.y) * aby + (this.D.pos.z - prev.z) * abz) / l2, 0.0F, 1.0F);
            float dd = this.D.pos.dst(prev.x + abx * t, prev.y + aby * t, prev.z + abz * t);
            if (dd < 4.5F) {
               boom = true;
               this.dhurt((24.0F + this.zone * 2.5F) * (1.0F - dd / 9.0F), "ракета");
            }
         }

         if (!m.atDrone && m.tgt != null && m.pos.dst(m.tgt.pos) < 5.0F) {
            boom = true;
         }

         if (m.life <= 0.0F || m.pos.y < this.world.height(m.pos.x, m.pos.z)) {
            boom = true;
         }

         if (boom) {
            FX.explosion(m.pos, 1.3F, true);
            if (this.D.alive && m.pos.dst(this.D.pos) < 30.0F) {
               G.sfx("boom", 0.6F);
            }

            m.trail.stop();
            m.node.remove();
            this.mis.removeIndex(i);
         }
      }
   }

   void updateShells(float dt) {
      this.tracers.clear();
      int n = 0;

      for (int i = this.shells.size - 1; i >= 0; i--) {
         Strike.Shell b = (Strike.Shell)this.shells.get(i);
         b.prev.set(b.pos);
         b.vel.y -= 4.0F * dt;
         b.pos.mulAdd(b.vel, dt);
         b.life -= dt;
         boolean dead = false;
         if (!b.flak) {
            if (this.D.alive && this.st.equals("fly") && Game.segHit(b.prev, b.pos, this.D.pos, this.D.r + 0.45F)) {
               this.dhurt(b.dmg, "пушка");
               FX.sparks(this.D.pos, 4);
               dead = true;
            }

            if (b.life <= 0.0F) {
               dead = true;
            }
         } else {
            boolean near = this.D.alive && this.st.equals("fly") && b.pos.dst(this.D.pos) < 5.0F;
            if (b.life <= 0.0F || near) {
               FX.explosion(b.pos, 0.55F, false);
               FX.puff(b.pos, 2.2F, new Color(0.08F, 0.08F, 0.08F, 1.0F), 0.75F, 2.6F, 0.4F);
               if (this.D.alive && this.st.equals("fly")) {
                  float dd = b.pos.dst(this.D.pos);
                  if (dd < b.rad) {
                     this.dhurt(b.dmg * (1.0F - dd / b.rad * 0.7F), "зенитка");
                  }
               }

               this.shells.removeIndex(i);
               continue;
            }
         }

         if (!dead && b.pos.y < this.world.height(b.pos.x, b.pos.z)) {
            dead = true;
         }

         if (dead) {
            this.shells.removeIndex(i);
         } else if (n < 300) {
            float trav = b.pos.dst(b.o);
            float L = Math.min(b.flak ? 6.0F : 14.0F, trav);
            if (L > 0.5F) {
               this.tA.set(b.vel).nor();
               this.tB.set(b.pos).mulAdd(this.tA, -L * 0.5F);
               Node3.basisLook(this.tm, this.tA, Math.abs(this.tA.y) < 0.98F ? Vector3.Y : Vector3.X);
               this.tm.val[12] = this.tB.x;
               this.tm.val[13] = this.tB.y;
               this.tm.val[14] = this.tB.z;
               this.tm.scale(0.16F, 0.16F, L);
               if (b.flak) {
                  this.tracers.prim(Dyn.BOX, this.tm, 1.0F, 0.85F, 0.5F, 1.0F);
               } else {
                  this.tracers.prim(Dyn.BOX, this.tm, 1.0F, 0.55F, 0.35F, 1.0F);
               }

               n++;
            }
         }
      }
   }

   void drawBeams() {
      this.beamDyn.clear();
      ArrayIterator var1 = this.units.iterator();

      while (var1.hasNext()) {
         Strike.Obj u = (Strike.Obj)var1.next();
         if (u.alive && u.beam && this.D.alive) {
            Vector3 a = u.pitch.toGlobal(this.tA.set(0.0F, 0.3F, -2.0F), new Vector3());
            Vector3 b = new Vector3(this.D.pos).add(this.rr(-0.3F, 0.3F), this.rr(-0.3F, 0.3F), this.rr(-0.3F, 0.3F));
            float w = this.rr(0.8F, 1.3F);
            this.cylBetween(a, b, 0.12F * w, 1.0F, 0.25F, 0.2F, 0.8F);
            this.cylBetween(a, b, 0.04F * w, 1.0F, 0.9F, 0.85F, 0.9F);
            if (this.R.nextFloat() < 0.5F) {
               FX.glow(a, 2.0F, new Color(1.0F, 0.3F, 0.2F, 1.0F), 0.05F);
            }
         }
      }
   }

   void cylBetween(Vector3 a, Vector3 b, float r, float cr, float cg, float cb, float ca) {
      this.tA.set(b).sub(a);
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
         m[12] = (a.x + b.x) / 2.0F;
         m[13] = (a.y + b.y) / 2.0F;
         m[14] = (a.z + b.z) / 2.0F;
         this.beamDyn.prim(Dyn.CYL8, this.tm, cr, cg, cb, ca);
      }
   }

   void updateHunters(float dt) {
      ArrayIterator var2 = this.hunters.iterator();

      while (var2.hasNext()) {
         Strike.Hunter h = (Strike.Hunter)var2.next();
         if (h.alive) {
            Vector3 hp = h.pos;
            Vector3 want = new Vector3();
            h.chase = this.D.alive && this.st.equals("fly") && (this.alert > 0.0F || hp.dst(this.D.pos) < 160.0F) && hp.dst(this.D.pos) < 520.0F;
            float sp;
            if (h.chase) {
               want.set(this.D.pos).mulAdd(this.D.vel, 0.4F).sub(hp).nor();
               sp = Math.min(52.0F, 42.0F + this.zone);
            } else {
               h.ph += dt * 0.25F;
               float cx = MathUtils.sin(h.ph) * 90.0F;
               float cz = MathUtils.cos(h.ph) * 90.0F;
               want.set(cx, this.world.height(cx, cz) + 55.0F, cz).sub(hp).nor();
               sp = 22.0F;
            }

            Vector3 cur = h.vel.len() > 0.1F ? new Vector3(h.vel).nor() : new Vector3(want);
            float ang = Game.angleBetween(cur, want);
            if (ang > 1.0E-4F) {
               Game.slerp(cur, want, Math.min(1.0F, 2.4F * dt / ang));
            }

            h.vel.set(cur).scl(sp);
            hp.mulAdd(h.vel, dt);
            float gy = this.world.height(hp.x, hp.z);
            if (hp.y < gy + 5.0F) {
               hp.y = gy + 5.0F;
            }

            h.g.pos.set(hp);
            float ry = (float)Math.sqrt(cur.x * cur.x + cur.z * cur.z) > 0.1F ? MathUtils.atan2(-cur.x, -cur.z) : 0.0F;
            h.g.setEuler(h.chase ? -0.25F : -0.1F, ry, 0.0F);
            if (h.rot != null) {
               h.rot.rotateY(dt * 40.0F);
            }

            if (this.D.alive && this.st.equals("fly") && hp.dst(this.D.pos) < 3.4F + this.D.r) {
               this.killHunter(h);
               this.dhurt(32.0F + this.zone, "перехватчик");
            }
         }
      }
   }

   void killHunter(Strike.Hunter h) {
      h.alive = false;
      FX.explosion(h.pos, 1.0F, true);
      h.g.visible = false;
      this.score += 60;
      this.feed("Перехватчик уничтожен", C("ff9a6a"), 60);
   }

   void updateMisc(float dt) {
      for (int i = this.flares.size - 1; i >= 0; i--) {
         Strike.Flare f = (Strike.Flare)this.flares.get(i);
         f.life -= dt;
         f.vel.y -= 9.0F * dt;
         f.vel.scl(1.0F - dt * 0.8F);
         f.pos.mulAdd(f.vel, dt);
         if ((this.T * 30.0F + i) % 2.0F < 1.0F) {
            FX.glow(f.pos, 2.6F, new Color(1.0F, 0.9F, 0.6F, 1.0F), 0.07F);
         }

         if (this.R.nextFloat() < 0.25F) {
            FX.puff(f.pos, 0.6F, new Color(0.85F, 0.85F, 0.85F, 1.0F), 0.35F, 1.2F, 0.5F);
         }

         if (f.life <= 0.0F) {
            this.flares.removeIndex(i);
         }
      }

      for (int i = this.bombs.size - 1; i >= 0; i--) {
         Strike.Bomb b = (Strike.Bomb)this.bombs.get(i);
         b.vel.y -= 14.0F * dt;
         b.vel.scl(1.0F - dt * 0.15F);
         b.pos.mulAdd(b.vel, dt);
         b.g.pos.set(b.pos);
         if (b.vel.len() > 0.5F) {
            this.tA.set(b.vel).nor();
            b.g.lookDir(this.tA, Math.abs(this.tA.y) < 0.98F ? Vector3.Y : Vector3.X);
         }

         Strike.Obj hit = null;

         for (int pass = 0; pass < 2; pass++) {
            Array<Strike.Obj> arr = pass == 0 ? this.targets : this.units;

            for (int k = 0; k < arr.size; k++) {
               Strike.Obj o = (Strike.Obj)arr.get(k);
               if (o.alive && this.tB.set(o.pos).add(0.0F, o.hr, 0.0F).dst(b.pos) < o.r) {
                  hit = o;
               }
            }
         }

         if (hit != null || b.pos.y < this.world.height(b.pos.x, b.pos.z) + 0.2F) {
            FX.explosion(b.pos, 1.5F, true);
            G.sfx("boom", 0.8F);
            this.blast(new Vector3(b.pos), 140.0F * (1.0F + 0.12F * up("war")), 9.0F, hit);
            b.g.remove();
            this.bombs.removeIndex(i);
         }
      }

      ArrayIterator var10 = this.targets.iterator();

      while (var10.hasNext()) {
         Strike.Obj t = (Strike.Obj)var10.next();
         if (t.alive && t.spin != null) {
            t.spin.rotateY(dt * 1.5F);
         }
      }

      var10 = this.feed.iterator();

      while (var10.hasNext()) {
         Strike.Feed fx = (Strike.Feed)var10.next();
         fx.life -= dt;
      }

      while (this.feed.size > 0 && ((Strike.Feed)this.feed.peek()).life <= 0.0F) {
         this.feed.pop();
      }
   }

   public void update(float dt) {
      if (this.active) {
         if (!this.paused) {
            this.T += dt;
            this.root3.updateGlobal(null);
            this.bigT = Math.max(0.0F, this.bigT - dt);
            this.hitT = Math.max(0.0F, this.hitT - dt);
            this.flareCd = Math.max(0.0F, this.flareCd - dt);
            if (this.chainT > 0.0F) {
               this.chainT -= dt;
               if (this.chainT <= 0.0F) {
                  this.chain = 0;
               }
            }

            if (this.lateBlastT > 0.0F) {
               this.lateBlastT -= dt;
               if (this.lateBlastT <= 0.0F) {
                  this.blast(this.lateBlastP, 220.0F, 20.0F, null);
               }
            }

            if (this.lateOverT > 0.0F) {
               this.lateOverT -= dt;
               if (this.lateOverT <= 0.0F && this.st.equals("over") && this.ui != null) {
                  this.ui.strikeOver();
               }
            }

            if (this.bot) {
               this.bot(dt);
            }

            if (this.st.equals("fly") && this.D.alive) {
               this.fly(dt);
            }

            this.updateUnits(dt);
            this.drawBeams();
            this.updateMissiles(dt);
            this.updateShells(dt);
            this.updateHunters(dt);
            this.updateMisc(dt);
            if (this.st.equals("boom")) {
               this.boomT -= dt;
               if (this.boomT <= 0.0F) {
                  int left = this.targetsLeft();
                  if (left != 0) {
                     if (this.stock > 0) {
                        this.launch();
                     } else {
                        this.gameOver();
                     }
                  }
               }
            } else if (this.st.equals("clear")) {
               this.clearT -= dt;
               this.fade = MathUtils.clamp(1.0F - (this.clearT - 0.1F) / 0.7F, 0.0F, 1.0F);
               if (this.clearT <= 0.0F && this.fadeHold < 2) {
                  this.fadeHold++;
               } else if (this.clearT <= 0.0F) {
                  this.fadeHold = 0;
                  this.nextZone();
                  this.zoneSorties0 = this.sorties;
                  this.launch();
                  this.fade = 1.0F;
               }
            }

            if (!this.st.equals("clear") && this.fade > 0.0F) {
               this.fade = Math.max(0.0F, this.fade - dt * 2.0F);
            }

            this.camera(dt);
         }
      }
   }

   void gameOver() {
      this.st = "over";
      this.resRec = this.score > G.SV.swBest;
      G.SV.swBest = Math.max(G.SV.swBest, this.score);
      G.SV.swBestz = Math.max(G.SV.swBestz, this.zone);
      this.resCoins = this.score / 8;
      G.SV.c = G.SV.c + this.resCoins;
      this.resXp = this.score / 50 + this.zone * 4;
      int old = G.rankIndex();
      G.addXp(this.resXp);
      this.resRankUp = G.rankIndex() > old;
      this.game.statAdd("swTg", this.tgKills);
      this.game.statAdd("swAA", this.aaKills);
      this.game.statAdd("swRuns", 1.0F);
      this.game.checkAch();
      G.save();
      G.loop("buzz", 0.0F, 1.0F);
      G.sfx("fail");
      this.lateOverT = 1.2F;
   }

   public void endRun() {
      this.paused = false;
      this.stock = 0;
      if (this.D.alive && this.st.equals("fly")) {
         this.detonate(this.D.pos, null, 1.0F);
      } else if (!this.st.equals("over")) {
         this.gameOver();
      }
   }

   public void pause() {
      if (!this.paused) {
         this.paused = true;
         this.inBoost = false;
         this.inX = 0.0F;
         this.inY = 0.0F;
         G.loop("buzz", 0.0F, 1.0F);
      }
   }

   public void resume() {
      this.paused = false;
   }

   public void toggleCam() {
      this.camMode = 1 - this.camMode;
      G.SV.swCam = this.camMode;
      G.save();
      G.sfx("click");
   }

   void camera(float dt) {
      Vector3 sh = new Vector3();
      if (this.shake > 0.0F) {
         sh.set(this.rr(-1.0F, 1.0F), this.rr(-1.0F, 1.0F), this.rr(-1.0F, 1.0F)).scl(this.shake * 0.4F);
         this.shake = Math.max(0.0F, this.shake - dt * 2.5F);
      }

      this.cam.up.set(Vector3.Y);
      if (this.st.equals("fly") && this.D.alive) {
         Vector3 f = this.fwd(new Vector3());
         if (this.camMode == 0) {
            this.cam.fieldOfView = 92.0F;
            this.tm.idt().rotateRad(Vector3.Y, this.D.yaw).rotateRad(Vector3.X, this.D.pitch).rotateRad(Vector3.Z, this.D.roll);
            Vector3 off = new Vector3(0.0F, 0.15F, -this.D.r * 0.9F).rot(this.tm);
            this.cam.position.set(this.D.pos).add(off).mulAdd(sh, 0.3F);
            this.cam.direction.set(0.0F, 0.0F, -1.0F).rot(this.tm);
            this.cam.up.set(0.0F, 1.0F, 0.0F).rot(this.tm);
            this.D.g.visible = false;
         } else {
            this.cam.fieldOfView = 72.0F;
            this.D.g.visible = true;
            float dist = 7.0F + this.D.r * 3.5F;
            Vector3 hf = (float)Math.sqrt(f.x * f.x + f.z * f.z) > 0.05F
               ? new Vector3(f.x, 0.0F, f.z).nor()
               : new Vector3(-MathUtils.sin(this.D.yaw), 0.0F, -MathUtils.cos(this.D.yaw));
            Vector3 want = new Vector3(this.D.pos).mulAdd(hf, -dist).add(0.0F, 2.2F + this.D.r - f.y * 3.0F, 0.0F);
            want.y = Math.max(want.y, this.world.height(want.x, want.z) + 1.5F);
            if (this.camSmooth.dst(want) > 40.0F) {
               this.camSmooth.set(want);
            } else {
               this.camSmooth.lerp(want, Math.min(1.0F, dt * 9.0F));
            }

            this.cam.position.set(this.camSmooth).add(sh);
            this.cam.direction.set(this.D.pos).mulAdd(f, 18.0F).sub(this.cam.position).nor();
         }
      } else {
         this.cam.fieldOfView = 70.0F;
         Vector3 c = !this.st.equals("clear") ? new Vector3(this.boomP) : new Vector3(0.0F, this.world.height(0.0F, 0.0F) + 10.0F, 0.0F);
         float a = this.T * 0.35F;
         float r = !this.st.equals("clear") ? 26.0F : 120.0F;
         Vector3 p = new Vector3(c).add(MathUtils.sin(a) * r, !this.st.equals("clear") ? 10.0F : 55.0F, MathUtils.cos(a) * r);
         p.y = Math.max(p.y, this.world.height(p.x, p.z) + 3.0F);
         this.cam.position.set(p).add(sh);
         this.cam.direction.set(c).sub(this.cam.position).nor();
         this.camSmooth.set(this.cam.position);
      }

      this.cam.update();
   }

   public float noise() {
      float nz = 0.0F;
      if (this.st.equals("fly") && this.D.alive) {
         nz = MathUtils.clamp(this.D.pos.dst(this.launchP) / 1600.0F, 0.0F, 0.3F)
            + this.jam * 0.75F
            + (1.0F - this.D.hp / Math.max(1.0F, this.D.max)) * 0.25F
            + this.hitT * 1.2F;
         if (this.D.fall) {
            nz += 0.3F;
         }
      } else if (this.st.equals("boom") && this.boomT > 2.2F) {
         nz = 1.0F;
      }

      if (this.camMode == 1) {
         nz *= 0.35F;
      }

      return MathUtils.clamp(nz, 0.0F, 1.0F);
   }

   public int targetsLeft() {
      int n = 0;

      for (int i = 0; i < this.targets.size; i++) {
         if (((Strike.Obj)this.targets.get(i)).alive) {
            n++;
         }
      }

      return n;
   }

   public int missilesAtMe() {
      int n = 0;
      ArrayIterator var2 = this.mis.iterator();

      while (var2.hasNext()) {
         Strike.Mis m = (Strike.Mis)var2.next();
         if (m.atDrone) {
            n++;
         }
      }

      return n;
   }

   public Array<Vector3> missilePositions(Array<Vector3> out) {
      out.clear();
      ArrayIterator var2 = this.mis.iterator();

      while (var2.hasNext()) {
         Strike.Mis m = (Strike.Mis)var2.next();
         if (m.atDrone) {
            out.add(m.pos);
         }
      }

      return out;
   }

   public Array<Vector3> allMissiles(Array<Vector3> out) {
      out.clear();
      ArrayIterator var2 = this.mis.iterator();

      while (var2.hasNext()) {
         Strike.Mis m = (Strike.Mis)var2.next();
         out.add(m.pos);
      }

      return out;
   }

   public float detRange(Strike.Obj u) {
      float var10002 = this.zone;
      return AA.get(u.kind).det * (1.0F + var10002 * 0.03F);
   }

   void bot(float dt) {
      this.bt += dt;
      if (this.st.equals("fly") && this.D.alive) {
         Strike.Obj best = null;
         float bd = 1.0E9F;
         ArrayIterator tp = this.targets.iterator();

         while (tp.hasNext()) {
            Strike.Obj t = (Strike.Obj)tp.next();
            if (t.alive) {
               float d = t.pos.dst(this.D.pos);
               if (d < bd) {
                  bd = d;
                  best = t;
               }
            }
         }

         if (best != null) {
            Vector3 tpx = new Vector3(best.pos).add(0.0F, best.hr, 0.0F);
            float flat = (float)Math.hypot(tpx.x - this.D.pos.x, tpx.z - this.D.pos.z);
            Vector3 aim = new Vector3(tpx);
            if (flat > 140.0F) {
               aim.set(tpx.x, Math.max(this.world.height(this.D.pos.x, this.D.pos.z), this.world.height(tpx.x, tpx.z)) + 10.0F, tpx.z);
            }

            Vector3 v = aim.sub(this.D.pos);
            float wy = MathUtils.atan2(-v.x, -v.z);
            float wp = MathUtils.atan2(v.y, (float)Math.hypot(v.x, v.z));
            if (flat > 140.0F) {
               Vector3 ahead = this.fwd(new Vector3()).scl(40.0F).add(this.D.pos);
               if (this.world.height(ahead.x, ahead.z) + 10.0F > this.D.pos.y) {
                  wp = 0.5F;
               }
            }

            this.inX = MathUtils.clamp(-angDiff(this.D.yaw, wy) * 2.5F, -1.0F, 1.0F);
            this.mdY = MathUtils.clamp((wp - this.D.pitch) * 4.0F, -1.0F, 1.0F) * this.S.turn * 0.75F * dt * (G.SV.swInv ? -1 : 1);
            this.inY = 0.0F;
            this.inBoost = flat > 220.0F && this.D.bat > this.S.bat * 0.3F;
            boolean threat = false;

            for (int i = 0; i < this.mis.size; i++) {
               if (((Strike.Mis)this.mis.get(i)).atDrone && ((Strike.Mis)this.mis.get(i)).pos.dst(this.D.pos) < 220.0F) {
                  threat = true;
               }
            }

            if (threat) {
               this.useFlares();
            }

            if (this.D.nb > 0 && flat < 30.0F && this.D.pos.y - tpx.y < 60.0F) {
               this.dropBomb();
            }
         }
      }
   }

   static {
      DRONES.put(
         "osa",
         new Strike.DroneDef(
            "osa",
            "Оса",
            "FPV-квадрокоптер. Быстрый и вёрткий, умеет зависать. Боевая часть небольшая.",
            0,
            34.0F,
            56.0F,
            6.0F,
            2.6F,
            65.0F,
            190.0F,
            9.0F,
            75.0F,
            2,
            0,
            0.9F,
            false
         )
      );
      DRONES.put(
         "strizh",
         new Strike.DroneDef(
            "strizh",
            "Стриж",
            "Крылатый барражирующий боеприпас. Не зависает, но быстрый и малозаметный. В пике бьёт сильнее.",
            1800,
            46.0F,
            72.0F,
            30.0F,
            1.5F,
            65.0F,
            250.0F,
            11.0F,
            110.0F,
            2,
            0,
            0.8F,
            true
         )
      );
      DRONES.put(
         "taran",
         new Strike.DroneDef(
            "taran",
            "Таран",
            "Тяжёлый бронированный гексакоптер. Медленный и заметный, но держит удар, а взрыв сносит всё вокруг.",
            3200,
            25.0F,
            37.0F,
            5.0F,
            1.7F,
            180.0F,
            460.0F,
            15.0F,
            85.0F,
            3,
            0,
            1.4F,
            false
         )
      );
      DRONES.put(
         "shershen",
         new Strike.DroneDef(
            "shershen",
            "Шершень",
            "Носитель: сбрасывает три бомбы, а потом сам идёт на таран. Три удара за один вылет.",
            4500,
            30.0F,
            46.0F,
            5.0F,
            2.0F,
            85.0F,
            150.0F,
            9.0F,
            95.0F,
            2,
            3,
            1.1F,
            false
         )
      );
      Strike.AAD p = aa(260.0F, 430.0F, 2.2F);
      p.gun = 300.0F;
      p.rate = 9.0F;
      p.gdmg = 4.0F;
      p.spread = 0.022F;
      p.mis = 1;
      p.mcd = 6.5F;
      p.lock = 1.6F;
      AA.put("pantsir", p);
      Strike.AAD s = aa(200.0F, 290.0F, 3.0F);
      s.gun = 270.0F;
      s.rate = 16.0F;
      s.gdmg = 3.0F;
      s.spread = 0.03F;
      AA.put("shilka", s);
      Strike.AAD t = aa(240.0F, 540.0F, 1.6F);
      t.mis = 2;
      t.mcd = 3.8F;
      t.lock = 1.3F;
      AA.put("tor", t);
      Strike.AAD l = aa(220.0F, 360.0F, 1.4F);
      l.laser = 320.0F;
      l.ldps = 24.0F;
      AA.put("luch", l);
      Strike.AAD g = aa(280.0F, 420.0F, 1.2F);
      g.flak = 440.0F;
      g.frate = 1.3F;
      g.fdmg = 24.0F;
      g.frad = 8.0F;
      AA.put("grom", g);
   }

   static final class AAD {
      float hp;
      float det;
      float gun;
      float rate;
      float gdmg;
      float spread;
      float mcd;
      float lock;
      float turn;
      float laser;
      float ldps;
      float flak;
      float frate;
      float fdmg;
      float frad;
      int mis;
   }

   static final class Bomb {
      Node3 g;
      final Vector3 pos = new Vector3();
      final Vector3 vel = new Vector3();
   }

   public static final class DroneDef {
      public final String key;
      public final String name;
      public final String desc;
      public final int price;
      public final float sp;
      public final float boost;
      public final float min;
      public final float turn;
      public final float hp;
      public final float dmg;
      public final float rad;
      public final float bat;
      public final int flares;
      public final int bombs;
      public final float sig;
      public final boolean wing;

      DroneDef(
         String key,
         String name,
         String desc,
         int price,
         float sp,
         float boost,
         float min,
         float turn,
         float hp,
         float dmg,
         float rad,
         float bat,
         int flares,
         int bombs,
         float sig,
         boolean wing
      ) {
         this.key = key;
         this.name = name;
         this.desc = desc;
         this.price = price;
         this.sp = sp;
         this.boost = boost;
         this.min = min;
         this.turn = turn;
         this.hp = hp;
         this.dmg = dmg;
         this.rad = rad;
         this.bat = bat;
         this.flares = flares;
         this.bombs = bombs;
         this.sig = sig;
         this.wing = wing;
      }
   }

   public static final class DroneState {
      public Node3 g;
      Array<Node3> rot;
      Array<Node3> bombs;
      public float r;
      public final Vector3 pos = new Vector3();
      public final Vector3 vel = new Vector3();
      public float yaw;
      public float pitch;
      public float roll;
      public float sp;
      public float hp;
      public float max;
      public float bat;
      public float bmax;
      public int flares;
      public int nb;
      public boolean alive;
      public boolean fall;
   }

   public static final class Feed {
      public String t;
      public Color c;
      public float life;
   }

   static final class Flare {
      final Vector3 pos = new Vector3();
      final Vector3 vel = new Vector3();
      float life;
   }

   public static final class Hunter {
      Node3 g;
      Node3 rot;
      public final Vector3 pos = new Vector3();
      public final Vector3 vel = new Vector3();
      float ph;
      public boolean chase;
      public boolean alive = true;
   }

   static final class Mis {
      Node3 node;
      Node3 flame;
      SmokeTrail trail;
      final Vector3 pos = new Vector3();
      final Vector3 vel = new Vector3();
      final Vector3 last = new Vector3();
      float sp;
      float life;
      float age;
      Strike.Flare tgt;
      boolean atDrone;
      Strike.Obj src;
   }

   public static final class Obj {
      public String kind;
      public Node3 g;
      public Node3 spin;
      public Node3 turret;
      public Node3 pitch;
      public Node3 radar;
      public final Vector3 pos = new Vector3();
      public float hp;
      public float max;
      public float r;
      public float hr;
      public float ry;
      public float yaw;
      public float pit = 0.3F;
      public float track;
      public float cd;
      public float mcd;
      public float burst;
      public float rest;
      public float heat;
      public boolean alive = true;
      public boolean unit;
      public boolean see;
      public boolean over;
      public boolean beam;
      public boolean known;
      public Array<Vector3> barrels;
      public Array<Vector3> noses;
      int bi;
   }

   static final class Shell {
      final Vector3 pos = new Vector3();
      final Vector3 prev = new Vector3();
      final Vector3 o = new Vector3();
      final Vector3 vel = new Vector3();
      float life;
      float dmg;
      float rad;
      boolean flak;
   }

   public static final class Stats {
      public float sp;
      public float boost;
      public float min;
      public float turn;
      public float hp;
      public float dmg;
      public float rad;
      public float bat;
      public float sig;
      public float decoy;
      public int flares;
      public int bombs;
      public boolean wing;
   }
}
