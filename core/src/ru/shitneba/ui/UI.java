package ru.shitneba.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Input.TextInputListener;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.Array.ArrayIterator;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import ru.shitneba.BlackBox;
import ru.shitneba.Duel;
import ru.shitneba.G;
import ru.shitneba.Game;
import ru.shitneba.GameUI;
import ru.shitneba.Strike;
import ru.shitneba.gfx.Tex;

public class UI implements GameUI, InputProcessor {
   static final HashMap<String, Color> CC = new HashMap<>();
   static final Color C_PANEL = new Color(0.03F, 0.045F, 0.05F, 0.94F);
   static final Color C_LINE = new Color(0.25F, 0.95F, 0.8F, 1.0F);
   static final Color C_ACC = C("ffb21a");
   static final Color C_MUT = C("8fa59c");
   static final Color C_TEAL = C("3ff0c8");
   static final Color C_TXT = C("e6f1ec");
   static final Color C_GREEN = C("3ddc97");
   static final Color C_RANK = C("9fd0ff");
   static final Color C_BUY = C("23d6a6");
   static final Color C_BUYD = C("16b88c");
   static final Color C_BUYH = C("4ff5c8");
   static final Color C_RED = C("e8483c");
   static final Color C_WARN = C("f0b53a");
   static final Color C_BOSS = C("b07cff");
   static final Color C_DARK = C("1c1200");
   static final Color C_BUYT = C("03241b");
   static final Color WHITE = Color.WHITE;
   static final Color BLACK = Color.BLACK;
   static final int NORMAL = 0;
   static final int BIG = 1;
   static final int BUY = 2;
   final Game g;
   final Draw d;
   final UI.Host host;
   public String panel = "menu";
   public Strike strike;
   public Duel duel;
   float panelT;
   float T;
   final Map<String, String> backTo = new HashMap<>();
   final Map<String, Float> panelH = new HashMap<>();
   String toastS;
   float toastT;
   String bannerS = "";
   String bannerSub = "";
   float bannerT;
   boolean bannerBoss;
   String hintS = "";
   float hintT;
   final Array<String[]> commQ = new Array();
   String[] commCur;
   float commT;
   float overDelay = -1.0F;
   Runnable pending;
   int pendingFrames;
   boolean down;
   boolean dragged;
   boolean tapReady;
   boolean capture;
   int downPtr = -1;
   float downX;
   float downY;
   float curX;
   float curY;
   float lastY;
   float tapX;
   float tapY;
   float mx = -1.0F;
   float my = -1.0F;
   float scroll;
   float maxScroll;
   final Map<Integer, String> touches = new HashMap<>();
   final Map<Integer, Vector2> lastTouch = new HashMap<>();
   boolean mouseFree;
   final boolean DESK;
   float PX;
   float PY;
   float PW;
   float PH;
   float cx;
   float cy;
   float cw;
   float viewTop;
   float viewH;
   boolean clipOn;
   public int garageTab;
   public int shopVeh = -1;
   float hpShown = -1.0F;
   float hpGhost = -1.0F;
   boolean overRec;
   String reportText;
   JsonValue curMission;
   int cutCh;
   JsonValue cutThen;
   final HashMap<Integer, Boolean> chOpen = new HashMap<>();
   final Vector3 tv = new Vector3();
   final Vector3 tv2 = new Vector3();
   final Vector3 right = new Vector3();
   final Vector3 up = new Vector3();
   final Vector2 sp = new Vector2();
   final Vector2 sp2 = new Vector2();
   static final float[] PAUSE_R = new float[]{12.0F, 12.0F, 48.0F, 48.0F};
   static final float[] SHOP_R = new float[]{12.0F, 68.0F, 48.0F, 48.0F};
   public UI.StrikeHost strikeHost;
   static final Color C_OSD = new Color(0.92F, 1.0F, 0.96F, 1.0F);
   static final Color C_SRED = C("ff5a47");
   int joyId = -1;
   final Vector2 joyO = new Vector2();
   final Vector2 joyP = new Vector2();
   boolean mbBoost;
   final Array<Vector3> tmpArr = new Array();
   String duelView = "main";
   float duelResT;
   boolean dLost;
   boolean dWin;
   String dReason = "";
   String ipText = "";
   final HashMap<Integer, Float> altBtn = new HashMap<>();
   static final LinkedHashMap<String, String> EMPTY = new LinkedHashMap<>();

   static Color C(String h) {
      Color c = CC.get(h);
      if (c == null) {
         c = G.hex(h);
         CC.put(h, c);
      }

      return c;
   }

   public UI(Game g, Draw d, UI.Host host) {
      this.g = g;
      this.d = d;
      this.host = host;
      this.DESK = !G.isMobile();
      String[][] bt = new String[][]{
         {"endless", "menu"},
         {"garage", "menu"},
         {"shop", "menu"},
         {"settings", "menu"},
         {"about", "menu"},
         {"quests", "menu"},
         {"ach", "menu"},
         {"records", "menu"},
         {"campaign", "menu"},
         {"chal", "menu"},
         {"brief", "campaign"},
         {"swarm", "menu"},
         {"duel", "menu"}
      };

      for (String[] b : bt) {
         this.backTo.put(b[0], b[1]);
      }
   }

   @Override
   public void toast(String m) {
      this.toastS = m;
      this.toastT = 2.3F;
   }

   @Override
   public void banner(String t, boolean boss, String sub) {
      this.bannerS = t.toUpperCase();
      this.bannerSub = sub;
      this.bannerT = 3.0F;
      this.bannerBoss = boss;
   }

   @Override
   public void hint(String t) {
      this.hintS = t;
      this.hintT = 5.0F;
   }

   @Override
   public void comm(String who, String text) {
      this.commQ.add(new String[]{who, text});
      if (this.commT <= 0.0F) {
         this.commNext();
      }
   }

   void commNext() {
      if (this.commQ.size == 0) {
         this.commCur = null;
         this.commT = 0.0F;
      } else {
         this.commCur = (String[])this.commQ.removeIndex(0);
         G.sfx("radio", 0.4F);
         this.commT = 1.8F + this.commCur[1].length() * 0.045F;
      }
   }

   void clearComm() {
      this.commQ.clear();
      this.commCur = null;
      this.commT = 0.0F;
   }

   @Override
   public void startHud() {
      this.show("");
      this.clearComm();
      this.hpShown = -1.0F;
      this.hpGhost = -1.0F;
   }

   @Override
   public void over(boolean rec) {
      this.overRec = rec;
      this.overDelay = 0.5F;
   }

   @Override
   public void result(boolean win) {
      this.g.firing = false;
      this.touches.clear();
      this.show("result");
   }

   public void show(String name) {
      if (!name.equals(this.panel)) {
         this.panelT = 0.0F;
         this.scroll = 0.0F;
         BlackBox.log("экран «" + name + "»");
      }

      this.panel = name;
      this.tapReady = false;
      if (name.equals("menu")) {
         this.g.ensureQuests();
      }
   }

   boolean inBattle() {
      return this.panel.isEmpty() && (this.g.state.equals("run") || this.g.state.equals("dying"));
   }

   public void launch(String m) {
      this.pending = () -> {
         this.show("");
         this.g.startGame(m);
      };
      this.pendingFrames = 1;
   }

   void pause() {
      if (this.g.mode.equals("duel")) {
         this.g.firing = false;
         this.touches.clear();
         this.joyId = -1;
         this.show("pause");
      } else if (this.g.state.equals("run")) {
         this.g.state = "pause";
         this.g.firing = false;
         this.touches.clear();
         this.show("pause");
      }
   }

   void resume() {
      if (!this.g.mode.equals("duel")) {
         this.g.state = "run";
      }

      this.show("");
   }

   void toMenu() {
      this.clearComm();
      boolean wasCamp = this.g.mode.equals("campaign") && this.g.CM != null;
      boolean wasChal = wasCamp && "chal".equals(this.g.CM.getString("special", ""));
      this.g.quitBattle();
      this.pending = () -> {
         this.host.toMenuWorld();
         this.show(wasChal ? "chal" : (wasCamp ? "campaign" : "menu"));
      };
      this.pendingFrames = 1;
   }

   void back() {
      if (this.panel.equals("duel") && this.duel != null) {
         this.duel.stop();
      }

      if (this.panel.equals("shop") && this.g.state.equals("shop")) {
         this.g.state = "pause";
         this.show("pause");
      } else if (this.panel.equals("settings") && this.g.state.equals("pause")) {
         this.show("pause");
      } else {
         if (this.backTo.containsKey(this.panel)) {
            this.show(this.backTo.get(this.panel));
         }
      }
   }

   public void androidBack() {
      if (this.strike != null && this.strike.active) {
         if (this.panel.equals("spause")) {
            this.strike.resume();
            this.show("");
         } else if (this.panel.equals("sover")) {
            if (this.strikeHost != null) {
               this.strikeHost.exitStrike(false);
            }
         } else {
            this.strikePause();
         }
      } else if (this.panel.equals("dres")) {
         if (this.duel != null) {
            this.duel.leave();
         }

         this.toMenu();
      } else if (this.panel.equals("duel")) {
         if (this.duel != null) {
            this.duel.stop();
         }

         this.show("menu");
      } else if (this.inBattle()) {
         this.pause();
      } else if (this.panel.equals("pause")) {
         this.resume();
      } else if (this.panel.equals("over") || this.panel.equals("result")) {
         this.toMenu();
      } else if (this.panel.equals("cut")) {
         this.show("campaign");
      } else if (!this.panel.equals("daily")) {
         if (!this.backTo.containsKey(this.panel) && !this.panel.equals("shop") && !this.panel.equals("settings")) {
            if (this.panel.equals("menu")) {
               Gdx.app.exit();
            }
         } else {
            this.back();
         }
      }
   }

   public void showReport(String text) {
      this.reportText = text;
      this.show("report");
   }

   void pReport() {
      this.frame("report", 760.0F, false, true, () -> {
         this.title("Чёрный ящик");
         this.label("В прошлый раз игра вылетела или зависла. Нажми «Скопировать» и пришли текст разработчику (или сделай скриншот).", 13.0F, C_ACC, 0, 0);
         this.d.rect(this.cx, this.cy, this.cw, 1.0F, C_TEAL, 0.3F);
         this.gap(6.0F);
         this.label(this.reportText == null ? "" : this.reportText, 12.0F, C_TXT);
         int r = this.btnRow(new String[]{"Скопировать", "Понятно"}, new int[]{0, 1}, null, 46.0F);
         if (r == 0) {
            try {
               Gdx.app.getClipboard().setContents(this.reportText);
               this.toast("Отчёт скопирован");
            } catch (Throwable var3) {
               this.toast("Не удалось скопировать");
            }
         } else if (r == 1) {
            this.reportText = null;
            this.show("menu");
            this.checkDaily();
         }
      });
   }

   public void checkDaily() {
      if (!G.SV.dailyLast.equals(G.today())) {
         this.show("daily");
      }
   }

   void openShopBattle() {
      if (this.g.state.equals("run") || this.g.state.equals("pause")) {
         this.g.state = "shop";
         this.g.firing = false;
         this.touches.clear();
         this.show("shop");
      }
   }

   public void update(float dt) {
      this.T += dt;
      this.panelT += dt;
      this.deskMouse();
      this.keyAim(dt);
      if (this.toastT > 0.0F) {
         this.toastT -= dt;
      }

      if (this.commT > 0.0F) {
         this.commT -= dt;
         if (this.commT <= 0.0F) {
            this.commNext();
         }
      }

      this.bannerT = Math.max(0.0F, this.bannerT - dt);
      this.hintT = Math.max(0.0F, this.hintT - dt);
      if (this.overDelay > 0.0F) {
         this.overDelay -= dt;
         if (this.overDelay <= 0.0F) {
            this.overDelay = -1.0F;
            this.show("over");
         }
      }

      if (this.duelResT > 0.0F) {
         this.duelResT -= dt;
         if (this.duelResT <= 0.0F) {
            this.g.firing = false;
            this.touches.clear();
            this.show("dres");
         }
      }
   }

   public void render() {
      this.d.begin();
      if (this.pending != null) {
         this.loading();
         this.d.end();
         if (this.pendingFrames-- <= 0) {
            Runnable r = this.pending;
            this.pending = null;
            BlackBox.hold = true;
            BlackBox.mark("загрузка");

            try {
               r.run();
            } finally {
               BlackBox.tick();
               BlackBox.hold = false;
            }
         }
      } else {
         boolean hud = this.panel.isEmpty()
            && (this.g.state.equals("run") || this.g.state.equals("dying") || this.g.state.equals("over") || this.g.state.equals("result"));
         if (this.strike != null && this.strike.active) {
            if (this.panel.isEmpty()) {
               this.drawStrikeHud();
            }
         } else if (hud && this.bossRole()) {
            this.drawBossHud();
         } else if (hud) {
            this.drawHud();
         }

         if (!this.panel.isEmpty()) {
            this.drawPanel();
         }

         this.drawToast();
         this.d.end();
         this.tapReady = false;
      }
   }

   void loading() {
      this.d.rect(0.0F, 0.0F, this.d.w, this.d.h, C("05090a"));
      this.grid(0.06F);
      this.d.text("ЗАГРУЗКА…", this.d.w / 2.0F, this.d.h / 2.0F + 10.0F, 26.0F, C_TEAL, 3, 0);
      float k = this.T * 1.4F % 1.0F;
      this.d.rect(this.d.w / 2.0F - 80.0F, this.d.h / 2.0F + 30.0F, 160.0F, 3.0F, C_TEAL, 0.15F);
      this.d.rect(this.d.w / 2.0F - 80.0F + 120.0F * k, this.d.h / 2.0F + 30.0F, 40.0F, 3.0F, C_TEAL, 0.9F);
   }

   void drawPanel() {
      String var1 = this.panel;
      switch (var1) {
         case "menu":
            this.pMenu();
            break;
         case "endless":
            this.pEndless();
            break;
         case "garage":
            this.pGarage();
            break;
         case "shop":
            this.pShop();
            break;
         case "quests":
            this.pQuests();
            break;
         case "ach":
            this.pAch();
            break;
         case "records":
            this.pRecords();
            break;
         case "settings":
            this.pSettings();
            break;
         case "about":
            this.pAbout();
            break;
         case "pause":
            this.pPause();
            break;
         case "over":
            this.pOver();
            break;
         case "daily":
            this.pDaily();
            break;
         case "report":
            this.pReport();
            break;
         case "campaign":
            this.pCampaign();
            break;
         case "chal":
            this.pChal();
            break;
         case "brief":
            this.pBrief();
            break;
         case "cut":
            this.pCut();
            break;
         case "result":
            this.pResult();
            break;
         case "swarm":
            this.pSwarm();
            break;
         case "spause":
            this.pSPause();
            break;
         case "sover":
            this.pSOver();
            break;
         case "duel":
            this.pDuel();
            break;
         case "dres":
            this.pDRes();
      }

      if (this.backTo.containsKey(this.panel) || this.panel.equals("shop") || this.panel.equals("settings")) {
         this.backButton();
      }
   }

   void backButton() {
      float x = 12.0F;
      float y = 12.0F;
      float w = 112.0F;
      float h = 40.0F;
      if (this.button(x, y, w, h, "", 0, true, 15.0F)) {
         this.back();
      }

      if (!this.d.off) {
         float ax = x + 24.0F;
         float ay = y + h / 2.0F;
         this.d.line(ax, ay, ax + 14.0F, ay, 2.0F, C_TXT, 1.0F);
         this.d.line(ax, ay, ax + 6.0F, ay - 6.0F, 2.0F, C_TXT, 1.0F);
         this.d.line(ax, ay, ax + 6.0F, ay + 6.0F, 2.0F, C_TXT, 1.0F);
         this.d.text("Назад", x + 44.0F, ay + 6.0F, 15.0F, C_TXT, 1, -1);
      }
   }

   void grid(float a) {
      Color gc = new Color(0.1F, 0.55F, 0.45F, 1.0F);

      for (float x = 0.0F; x < this.d.w; x += 32.0F) {
         this.d.rect(x, 0.0F, 1.0F, this.d.h, gc, a);
      }

      for (float y = 0.0F; y < this.d.h; y += 32.0F) {
         this.d.rect(0.0F, y, this.d.w, 1.0F, gc, a);
      }
   }

   void dimBg() {
      this.d.rect(0.0F, 0.0F, this.d.w, this.d.h, new Color(0.0F, 0.012F, 0.014F, 1.0F), 0.62F);
      this.grid(0.05F);
      float e = this.d.w * 0.18F;
      this.d.gradH(0.0F, 0.0F, e, this.d.h, BLACK, 0.35F, BLACK, 0.0F);
      this.d.gradH(this.d.w - e, 0.0F, e, this.d.h, BLACK, 0.0F, BLACK, 0.35F);
   }

   void box(float x, float y, float w, float h, Color fill, float fa, Color border, float ba, float bw) {
      this.d.rect(x, y, w, h, fill, fa);
      if (ba > 0.0F) {
         this.d.frame(x, y, w, h, bw, border, ba);
      }
   }

   void brackets(float x, float y, float w, float h) {
      float L = 16.0F;
      float o = 3.0F;
      float wd = 2.0F;
      Color c = C_TEAL;
      float[][] cs = new float[][]{
         {x - o, y - o, 1.0F, 1.0F}, {x + w + o, y - o, -1.0F, 1.0F}, {x - o, y + h + o, 1.0F, -1.0F}, {x + w + o, y + h + o, -1.0F, -1.0F}
      };

      for (float[] p : cs) {
         this.d.rect(Math.min(p[0], p[0] + p[2] * L), p[1] - (p[3] > 0.0F ? 0.0F : wd), L, wd, c, 0.85F);
         this.d.rect(p[0] - (p[2] > 0.0F ? 0.0F : wd), Math.min(p[1], p[1] + p[3] * L), wd, L, c, 0.85F);
      }
   }

   void sbox(float x, float y, float w, float h, Color fill, float fa, Color border, float ba, float left, float skew) {
      float o = skew * h;
      this.d.poly(new float[]{x + o, y, x + w, y, x + w - o, y + h, x, y + h}, fill, fa);
      if (ba > 0.0F) {
         this.d.polyline(new float[]{x + o, y, x + w, y, x + w - o, y + h, x, y + h}, true, 1.0F, border, ba);
         if (left > 1.0F) {
            this.d.line(x + left * 0.5F, y + h, x + o + left * 0.5F, y, left, border, ba);
         }
      }
   }

   static boolean in(float px, float py, float x, float y, float w, float h) {
      return px >= x && px <= x + w && py >= y && py <= y + h;
   }

   boolean inView(float py) {
      return !this.clipOn || py >= this.viewTop && py <= this.viewTop + this.viewH;
   }

   boolean tap(float x, float y, float w, float h) {
      if (this.d.off || !this.tapReady) {
         return false;
      } else if (in(this.tapX, this.tapY, x, y, w, h) && this.inView(this.tapY)) {
         this.tapReady = false;
         return true;
      } else {
         return false;
      }
   }

   boolean pressing(float x, float y, float w, float h) {
      return !this.d.off && this.down && !this.dragged && in(this.curX, this.curY, x, y, w, h) && this.inView(this.curY);
   }

   boolean hover(float x, float y, float w, float h) {
      return this.DESK && !this.d.off && in(this.mx, this.my, x, y, w, h) && this.inView(this.my);
   }

   boolean button(float x, float y, float w, float h, String t, int style, boolean en, float fs) {
      if (!this.d.off) {
         boolean pr = en && this.pressing(x, y, w, h);
         boolean hv = en && this.hover(x, y, w, h);
         Color tc;
         if (style == 1) {
            Color f = pr ? C("e89400") : (hv ? C("ffc84d") : C_ACC);
            if (!en) {
               f = C("6b5420");
            }

            this.sbox(x, y, w, h, f, 1.0F, WHITE, 0.0F, 0.0F, 0.16F);
            float o = 0.16F * h;
            this.d
               .quad(
                  x + o,
                  y,
                  Color.toFloatBits(1.0F, 1.0F, 1.0F, 0.22F),
                  x + w,
                  y,
                  Color.toFloatBits(1.0F, 1.0F, 1.0F, 0.22F),
                  x + w - o * 0.55F,
                  y + h * 0.45F,
                  Color.toFloatBits(1.0F, 1.0F, 1.0F, 0.0F),
                  x + o * 0.45F,
                  y + h * 0.45F,
                  Color.toFloatBits(1.0F, 1.0F, 1.0F, 0.0F)
               );
            tc = C_DARK;
         } else if (style == 2) {
            if (en) {
               this.sbox(x, y, w, h, pr ? C_BUYD : (hv ? C_BUYH : C_BUY), 1.0F, C_TEAL, 0.0F, 0.0F, 0.12F);
            } else {
               this.sbox(x, y, w, h, WHITE, 0.05F, WHITE, 0.08F, 1.0F, 0.12F);
            }

            tc = en ? C_BUYT : new Color(0.55F, 0.62F, 0.6F, 0.7F);
         } else {
            if (en) {
               this.sbox(x, y, w, h, C_TEAL, pr ? 0.26F : (hv ? 0.16F : 0.06F), C_TEAL, !hv && !pr ? 0.35F : 1.0F, 3.0F, 0.16F);
            } else {
               this.sbox(x, y, w, h, WHITE, 0.025F, WHITE, 0.08F, 1.0F, 0.16F);
            }

            tc = en ? (hv ? WHITE : C_TXT) : new Color(0.55F, 0.62F, 0.6F, 0.55F);
         }

         if (!t.isEmpty()) {
            float size = fs > 0.0F ? fs : (style == 1 ? 18 : 15);
            int font = style == 1 ? 3 : (style == 2 ? 2 : 0);
            float tw = this.d.textW(t, size, font);
            if (tw > w - 16.0F) {
               size *= (w - 16.0F) / tw;
            }

            this.d.text(t, x + w / 2.0F + (pr ? 1 : 0), y + h / 2.0F + size * 0.36F + (pr ? 1 : 0), size, tc, font, 0);
         }
      }

      boolean c = en && this.tap(x, y, w, h);
      if (c) {
         G.sfx("click");
      }

      return c;
   }

   void frame(String name, float w, boolean left, boolean dim, UI.Body body) {
      w = Math.min(w, this.d.w - 24.0F);
      boolean hasBack = this.backTo.containsKey(name) || name.equals("shop") || name.equals("settings");
      float top = !hasBack || !left && !(w > this.d.w - 300.0F) ? 12.0F : 62.0F;
      float maxH = this.d.h - top - 12.0F;
      boolean wasOff = this.d.off;
      this.d.off = true;
      this.clipOn = false;
      this.cx = 0.0F;
      this.cy = 0.0F;
      this.cw = w - 36.0F;
      this.PW = w;
      body.run();
      float ch = this.cy - 8.0F;
      this.d.off = wasOff;
      float innerH = Math.min(ch, maxH - 30.0F);
      this.PW = w;
      this.PH = innerH + 30.0F;
      this.PX = left ? Math.max(24.0F, this.d.w * 0.04F) : (this.d.w - w) / 2.0F;
      float k = Interp(Math.min(1.0F, this.panelT / 0.22F));
      this.PY = Math.max(top, (this.d.h - this.PH) / 2.0F) + (1.0F - k) * 14.0F;
      if (dim) {
         this.dimBg();
      }

      for (int i = 1; i <= 4; i++) {
         this.d.rect(this.PX - i * 3, this.PY - i * 3 + 6.0F, this.PW + i * 6, this.PH + i * 6, BLACK, 0.07F);
      }

      this.d.rect(this.PX, this.PY, this.PW, this.PH, C_PANEL, C_PANEL.a);
      this.d.grad(this.PX, this.PY, this.PW, 70.0F, C_TEAL, 0.05F, C_TEAL, 0.0F);
      this.d.frame(this.PX, this.PY, this.PW, this.PH, 1.0F, C_LINE, 0.18F);
      this.d.rect(this.PX, this.PY, this.PW, 2.0F, C_LINE, 0.3F);
      this.brackets(this.PX, this.PY, this.PW, this.PH);
      this.maxScroll = Math.max(0.0F, ch - innerH);
      this.scroll = MathUtils.clamp(this.scroll, 0.0F, this.maxScroll);
      this.viewTop = this.PY + 16.0F;
      this.viewH = innerH;
      this.d.clip(this.PX + 2.0F, this.viewTop - 2.0F, this.PW - 4.0F, this.viewH + 4.0F);
      this.clipOn = true;
      this.cx = this.PX + 18.0F;
      this.cy = this.viewTop - this.scroll;
      this.cw = this.PW - 36.0F - (this.maxScroll > 0.0F ? 8 : 0);
      body.run();
      this.d.unclip();
      this.clipOn = false;
      if (this.maxScroll > 0.0F) {
         float tx = this.PX + this.PW - 10.0F;
         float th = this.viewH;
         this.d.rect(tx, this.viewTop, 5.0F, th, C_TEAL, 0.08F);
         float gh = Math.max(24.0F, th * innerH / ch);
         float gy = this.viewTop + (th - gh) * (this.scroll / this.maxScroll);
         this.d.rect(tx, gy, 5.0F, gh, C_TEAL, 0.45F);
      }

      this.panelH.put(name, this.PH);
   }

   static float Interp(float t) {
      return 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
   }

   void gap(float h) {
      this.cy += h;
   }

   void title(String t) {
      float size = 24.0F;
      this.d.text(t.toUpperCase(), this.cx + this.cw / 2.0F, this.cy + size, size, C_TXT, 3, 0);
      this.cy += size + 10.0F;
      float c = this.cx + this.cw / 2.0F;
      this.d.rect(c - 70.0F, this.cy, 140.0F, 2.0F, C_TEAL, 0.7F);
      this.d.rect(c - 110.0F, this.cy, 30.0F, 2.0F, C_TEAL, 0.3F);
      this.d.rect(c + 80.0F, this.cy, 30.0F, 2.0F, C_TEAL, 0.3F);
      this.cy += 12.0F;
   }

   void label(String t, float size, Color c, int font, int align) {
      float h = this.d.wrap(t, this.cx, this.cy, this.cw, size, c, font, align);
      this.cy += h + 6.0F;
   }

   void label(String t, float size, Color c) {
      this.label(t, size, c, 0, -1);
   }

   void section(String t) {
      this.gap(4.0F);
      this.d.rect(this.cx, this.cy + 3.0F, 3.0F, 14.0F, C_ACC, 1.0F);
      this.d.text(t, this.cx + 10.0F, this.cy + 16.0F, 17.0F, C_TXT, 1, -1);
      this.cy += 26.0F;
   }

   boolean btn(String t, float h, int style, boolean en) {
      boolean c = this.button(this.cx, this.cy, this.cw, h, t, style, en, 0.0F);
      this.cy += h + 8.0F;
      return c;
   }

   int btnRow(String[] ts, int[] styles, boolean[] en, float h) {
      int n = ts.length;
      float gp = 6.0F;
      float w = (this.cw - gp * (n - 1)) / n;
      int r = -1;

      for (int i = 0; i < n; i++) {
         if (this.button(this.cx + i * (w + gp), this.cy, w, h, ts[i], styles[i], en == null || en[i], 0.0F)) {
            r = i;
         }
      }

      this.cy += h + 8.0F;
      return r;
   }

   void bar(float x, float y, float w, float h, float k, Color c) {
      this.d.rect(x, y, w, h, WHITE, 0.08F);
      if (k > 0.0F) {
         this.d.rect(x, y, w * MathUtils.clamp(k, 0.0F, 1.0F), h, c, 1.0F);
      }
   }

   int tabs(String[] names, int sel) {
      int n = names.length;
      float gp = 4.0F;
      float w = (this.cw - gp * (n - 1)) / n;
      float h = 34.0F;

      for (int i = 0; i < n; i++) {
         float x = this.cx + i * (w + gp);
         boolean s = i == sel;
         if (!this.d.off) {
            this.d.rect(x, this.cy, w, h, s ? C_TEAL : WHITE, s ? 0.14F : 0.03F);
            this.d.rect(x, this.cy + h - 2.0F, w, 2.0F, s ? C_TEAL : WHITE, s ? 1.0F : 0.1F);
            this.d.text(names[i], x + w / 2.0F, this.cy + h / 2.0F + 5.5F, 15.0F, s ? WHITE : C_MUT, s ? 2 : 0, 0);
         }

         if (!s && this.tap(x, this.cy, w, h)) {
            sel = i;
            G.sfx("click");
            this.scroll = 0.0F;
         }
      }

      this.cy += h + 10.0F;
      return sel;
   }

   UI.Card card(String title, String desc, String btn, boolean en, Runnable act) {
      UI.Card c = new UI.Card();
      c.title = title;
      c.desc = desc;
      c.btn = btn;
      c.en = en;
      c.act = act;
      return c;
   }

   void cards(List<UI.Card> cs, int cols) {
      cols = Math.max(1, Math.min(cols, (int)(this.cw / 190.0F)));
      float gp = 8.0F;
      float w = (this.cw - gp * (cols - 1)) / cols;

      for (int i0 = 0; i0 < cs.size(); i0 += cols) {
         float rowH = 0.0F;
         boolean was = this.d.off;
         this.d.off = true;

         for (int i = i0; i < Math.min(cs.size(), i0 + cols); i++) {
            rowH = Math.max(rowH, this.cardH(cs.get(i), w));
         }

         this.d.off = was;

         for (int i = i0; i < Math.min(cs.size(), i0 + cols); i++) {
            this.drawCard(cs.get(i), this.cx + (i - i0) * (w + gp), this.cy, w, rowH);
         }

         this.cy += rowH + gp;
      }
   }

   float cardH(UI.Card c, float w) {
      float h = 12.0F;
      if (c.sw != null || c.swc != null) {
         h += 44.0F;
      }

      h += this.d.wrap(c.title, 0.0F, 0.0F, w - 24.0F, 15.0F, WHITE, 2, -1) + 2.0F;
      if (c.sub != null) {
         h += this.d.wrap(c.sub, 0.0F, 0.0F, w - 24.0F, 13.0F, WHITE, 2, -1) + 2.0F;
      }

      if (!c.desc.isEmpty()) {
         h += this.d.wrap(c.desc, 0.0F, 0.0F, w - 24.0F, 12.0F, WHITE, 0, -1) + 2.0F;
      }

      if (c.lv >= 0) {
         h += 14.0F;
      }

      return h + 54.0F;
   }

   void drawCard(UI.Card c, float x, float y, float w, float h) {
      if (!this.d.off) {
         this.box(x, y, w, h, c.sel ? C_ACC : WHITE, c.sel ? 0.07F : 0.05F, c.sel ? C_ACC : C_LINE, c.sel ? 1.0F : 0.22F, c.sel ? 2.0F : 1.0F);
         if (c.sel) {
            this.d.rect(x, y, 4.0F, h, C_ACC, 1.0F);
         }
      }

      float yy = y + 12.0F;
      if (c.sw != null) {
         this.d.tex(c.sw, x + 12.0F, yy, w - 24.0F, 40.0F, (w - 24.0F) / 64.0F, 0.625F);
         this.d.frame(x + 12.0F, yy, w - 24.0F, 40.0F, 1.0F, BLACK, 0.4F);
         yy += 44.0F;
      } else if (c.swc != null) {
         this.d.rect(x + 12.0F, yy + 10.0F, w - 24.0F, 20.0F, BLACK, 0.4F);
         this.d.gradH(x + 16.0F, yy + 17.0F, w - 32.0F, 6.0F, c.swc, 0.1F, c.swc, 1.0F);
         this.d.circle(x + w - 18.0F, yy + 20.0F, 5.0F, WHITE, 0.9F);
         yy += 44.0F;
      }

      yy += this.d.wrap(c.title, x + 12.0F, yy, w - 24.0F, 15.0F, C_TXT, 2, -1) + 2.0F;
      if (c.sub != null) {
         yy += this.d.wrap(c.sub, x + 12.0F, yy, w - 24.0F, 13.0F, c.subc != null ? c.subc : C_TEAL, 2, -1) + 2.0F;
      }

      if (!c.desc.isEmpty()) {
         yy += this.d.wrap(c.desc, x + 12.0F, yy, w - 24.0F, 12.0F, C_MUT, 0, -1) + 2.0F;
      }

      if (c.lv >= 0) {
         float pw = Math.min(16.0F, (w - 24.0F - (c.max - 1) * 3) / Math.max(1, c.max));

         for (int i = 0; i < c.max; i++) {
            this.d.rect(x + 12.0F + i * (pw + 3.0F), yy + 2.0F, pw, 6.0F, i < c.lv ? C_ACC : WHITE, i < c.lv ? 1.0F : 0.12F);
         }

         yy += 14.0F;
      }

      if (this.button(x + 12.0F, y + h - 12.0F - 34.0F, w - 24.0F, 34.0F, c.btn, 2, c.en, 14.0F) && c.act != null) {
         c.act.run();
      }
   }

   float rankK() {
      int ri = G.rankIndex();
      if (ri >= G.RANK_X.length - 1) {
         return 1.0F;
      } else {
         int a = G.RANK_X[ri];
         int b = G.RANK_X[ri + 1];
         return MathUtils.clamp((float)(G.xp() - a) / (b - a), 0.0F, 1.0F);
      }
   }

   String rankText() {
      int ri = G.rankIndex();
      return ri >= G.RANK_X.length - 1
         ? String.format("Звание: %s · опыт %d (высшее звание)", G.RANK_N[ri], G.xp())
         : String.format("Звание: %s · опыт %d / %d", G.RANK_N[ri], G.xp(), G.RANK_X[ri + 1]);
   }

   int questsReady() {
      int n = 0;

      for (G.Quest q : G.SV.qList) {
         if (q.done && !q.cl) {
            n++;
         }
      }

      return n;
   }

   void pMenu() {
      this.frame(
         "menu",
         440.0F,
         true,
         false,
         () -> {
            this.d.text("ОТРЯД «ЩИТ» · ТАКТИЧЕСКИЙ ТЕРМИНАЛ", this.cx, this.cy + 11.0F, 11.0F, C_TEAL, 3, -1);
            this.cy += 20.0F;
            this.d.text("ЩИТ НЕБА 3D", this.cx, this.cy + 38.0F, 40.0F, C_TXT, 3, -1);
            this.cy += 50.0F;
            this.d.text("Оборона долины Ясная · 6+", this.cx, this.cy + 13.0F, 13.0F, C_MUT);
            this.cy += 22.0F;
            this.d.text("Рекорд  " + G.SV.best, this.cx, this.cy + 14.0F, 14.0F, C_TXT);
            this.d.text("Монеты  " + G.SV.c, this.cx + this.cw, this.cy + 14.0F, 14.0F, C_ACC, 2, 1);
            this.cy += 22.0F;
            this.d.text(this.rankText(), this.cx, this.cy + 13.0F, 13.0F, C_RANK, 2, -1);
            this.cy += 20.0F;
            this.bar(this.cx, this.cy, this.cw, 6.0F, this.rankK(), C_RANK);
            this.cy += 14.0F;
            if (this.btn("Бесконечный бой", 52.0F, 1, true)) {
               this.show("endless");
            }

            if (this.btn(String.format("Кампания · глава %d   %d/%d", this.curChapter() + 1, this.allStars(), this.allMissions() * 3), 44.0F, 0, true)) {
               this.openCampaign();
            }

            if (!this.d.off) {
               this.d.star(this.cx + this.cw - 22.0F, this.cy - 8.0F - 22.0F, 7.0F, C_ACC, 1.0F);
            }

            int r2 = this.btnRow(new String[]{"", ""}, new int[]{0, 0}, null, 44.0F);
            if (!this.d.off) {
               float w2 = (this.cw - 6.0F) / 2.0F;
               this.d.text("НОВОЕ", this.cx + 22.0F, this.cy - 8.0F - 44.0F + 17.0F, 10.0F, C_ACC, 3, -1);
               this.d.text("Рой: дрон-камикадзе", this.cx + 22.0F, this.cy - 8.0F - 44.0F + 34.0F, 14.0F, C_TXT, 2, -1);
               this.d.text("ПО WI‑FI", this.cx + w2 + 6.0F + 22.0F, this.cy - 8.0F - 44.0F + 17.0F, 10.0F, C_TEAL, 3, -1);
               this.d.text("Дуэль: ПВО — босс", this.cx + w2 + 6.0F + 22.0F, this.cy - 8.0F - 44.0F + 34.0F, 14.0F, C_TXT, 2, -1);
            }

            if (r2 == 0) {
               this.show("swarm");
            } else if (r2 == 1) {
               this.openDuel();
            }

            int qn = this.questsReady();
            String[] names = new String[]{
               "Гараж", "Улучшения", qn > 0 ? "Задания (" + qn + ")" : "Задания", "Испытания", "Достижения", "Рекорды", "Настройки", "Об игре"
            };
            float gp = 6.0F;
            float w = (this.cw - gp * 3.0F) / 4.0F;

            for (int i = 0; i < names.length; i++) {
               float xx = this.cx + i % 4 * (w + gp);
               float yx = this.cy + i / 4 * (38.0F + gp);
               if (this.button(xx, yx, w, 38.0F, names[i], 0, true, 14.0F)) {
                  switch (i) {
                     case 0:
                        this.garageTab = 0;
                        this.show("garage");
                        break;
                     case 1:
                        this.shopVeh = -1;
                        this.backTo.put("shop", "menu");
                        this.show("shop");
                        break;
                     case 2:
                        this.show("quests");
                        break;
                     case 3:
                        this.show("chal");
                        break;
                     case 4:
                        this.show("ach");
                        break;
                     case 5:
                        this.show("records");
                        break;
                     case 6:
                        this.show("settings");
                        break;
                     default:
                        this.show("about");
                  }
               }

               if (i == 2 && qn > 0 && !this.d.off) {
                  this.d.circle(xx + w - 10.0F, yx + 9.0F, 4.0F, C_ACC, 0.6F + 0.4F * MathUtils.sin(this.T * 5.0F));
               }
            }

            this.cy += 2.0F * (38.0F + gp) + 4.0F;
            this.d.text("v3.1.2", this.cx + this.cw, this.cy + 4.0F, 11.0F, C_MUT, 0, 1);
            this.cy += 10.0F;
         }
      );
      if (!this.d.off) {
         String vn = G.vehName(G.SV.veh);
         float x = this.d.w - 24.0F;
         float y = this.d.h - 30.0F;
         JsonValue V = G.D.get("VEHICLES").get(G.SV.veh);
         this.d.text("ВЫБРАННЫЙ КОМПЛЕКС", x, y - 46.0F, 11.0F, C_TEAL, 3, 1);
         this.d.text(vn.toUpperCase(), x + 1.5F, y - 18.0F + 1.5F, 26.0F, BLACK, 3, 1);
         this.d.text(vn.toUpperCase(), x, y - 18.0F, 26.0F, C_TXT, 3, 1);
         float tw = Math.min(360.0F, this.d.textW(V.getString("desc"), 13.0F, 0));
         this.d.rect(x - tw, y - 13.0F, tw, 1.0F, C_TEAL, 0.4F);
         this.d.text(V.getString("desc"), x, y + 6.0F, 13.0F, C_MUT, 0, 1);
      }
   }

   void pEndless() {
      this.frame(
         "endless",
         460.0F,
         false,
         true,
         () -> {
            this.title("Бесконечный бой");
            this.label(
               "Улучшения из меню действуют с первой волны, а монеты за сбитых можно тратить на прокачку прямо в бою. 30% монет боя переходят в общий баланс.",
               13.0F,
               C_MUT,
               0,
               0
            );
            String[] keys = new String[]{"easy", "normal", "hard"};
            String[] nm = new String[3];
            int[] st = new int[3];

            for (int i = 0; i < 3; i++) {
               nm[i] = G.D.get("DIFF").get(keys[i]).getString("name");
               st[i] = G.SV.diff.equals(keys[i]) ? 1 : 0;
            }

            int r = this.btnRow(nm, st, null, 40.0F);
            if (r >= 0) {
               G.SV.diff = keys[r];
               G.save();
            }

            this.label(G.D.get("DIFF").get(G.SV.diff).getString("hint"), 12.0F, C_MUT, 0, 0);
            if (this.toggle("Ночной бой", "прожектор, монеты ×1.25", G.SV.night)) {
               G.SV.night = !G.SV.night;
               G.save();
            }

            this.d.text("Комплекс: " + G.vehName(G.SV.veh), this.cx, this.cy + 13.0F, 13.0F, C_TXT, 2, -1);
            this.d.text("сменить в гараже", this.cx + this.cw, this.cy + 13.0F, 12.0F, C_MUT, 0, 1);
            this.cy += 24.0F;
            if (this.btn("В бой", 50.0F, 1, true)) {
               this.launch("endless");
            }
         }
      );
   }

   boolean toggle(String t, String sub, boolean on) {
      float h = 40.0F;
      boolean c = this.button(this.cx, this.cy, this.cw, h, "", 0, true, 0.0F);
      if (!this.d.off) {
         this.d.text(t, this.cx + 18.0F, this.cy + (sub != null ? 18 : 25), 15.0F, C_TXT, 0, -1);
         if (sub != null) {
            this.d.text(sub, this.cx + 18.0F, this.cy + 33.0F, 11.0F, C_MUT, 0, -1);
         }

         float sx = this.cx + this.cw - 58.0F;
         float sy = this.cy + 11.0F;
         this.d.rect(sx, sy, 40.0F, 18.0F, on ? C_TEAL : WHITE, on ? 0.35F : 0.1F);
         this.d.rect(on ? sx + 22.0F : sx + 2.0F, sy + 2.0F, 16.0F, 14.0F, on ? C_TEAL : C_MUT, 1.0F);
      }

      this.cy += h + 8.0F;
      return c;
   }

   void pGarage() {
      float w = Math.min(580.0F, this.d.w * 0.56F);
      this.frame(
         "garage",
         w,
         true,
         false,
         () -> {
            this.title("Гараж");
            this.d.text("Монеты: " + G.SV.c, this.cx + this.cw / 2.0F, this.cy + 14.0F, 14.0F, C_ACC, 2, 0);
            this.cy += 24.0F;
            this.garageTab = this.tabs(new String[]{"Машины", "Окраска", "Трассеры"}, this.garageTab);
            List<UI.Card> cs = new ArrayList<>();
            if (this.garageTab == 0) {
               for (String k : G.VEHS) {
                  JsonValue V = G.D.get("VEHICLES").get(k);
                  boolean own = G.SV.vown.contains(k);
                  boolean sel = G.SV.veh.equals(k);
                  int cost = V.getInt("cost");
                  UI.Card c = this.card(
                     V.getString("name"),
                     V.getString("desc"),
                     own ? (sel ? "Выбрана" : "Выбрать") : "Купить · " + cost,
                     own && !sel || !own && G.SV.c >= cost,
                     () -> {
                        if (!G.SV.vown.contains(k)) {
                           G.SV.c -= cost;
                           G.SV.vown.add(k);
                           G.sfx("coin");
                        }

                        G.SV.veh = k;
                        G.save();
                        this.selectVeh(k);
                     }
                  );
                  c.sel = sel;
                  c.sub = vehRole(k);
                  c.subc = C_TEAL;
                  cs.add(c);
               }
            } else if (this.garageTab == 1) {
               for (JsonValue s = G.D.get("SK_VEH").child; s != null; s = s.next) {
                  String id = s.name;
                  if (!s.getBoolean("hidden", false) || G.SV.skOwn.contains(id)) {
                     boolean own = G.SV.skOwn.contains(id);
                     boolean sel = G.SV.skVeh.equals(id);
                     int cost = s.getInt("cost");
                     UI.Card c = this.card(
                        s.getString("name"),
                        "",
                        own ? (sel ? "Выбрана" : "Выбрать") : (cost == 0 ? "Получить" : "Купить · " + cost),
                        own && !sel || !own && G.SV.c >= cost,
                        () -> {
                           if (!G.SV.skOwn.contains(id)) {
                              G.SV.c -= cost;
                              G.SV.skOwn.add(id);
                              G.sfx("coin");
                           }

                           G.SV.skVeh = id;
                           G.save();
                           this.selectVeh(G.SV.veh);
                        }
                     );
                     c.sel = sel;
                     c.sw = Tex.camo(id);
                     cs.add(c);
                  }
               }
            } else {
               for (JsonValue sx = G.D.get("SK_TR").child; sx != null; sx = sx.next) {
                  String id = sx.name;
                  boolean own = G.SV.skOwn.contains(id);
                  boolean sel = G.SV.skTr.equals(id);
                  int cost = sx.getInt("cost");
                  UI.Card c = this.card(
                     sx.getString("name"), "", own ? (sel ? "Выбран" : "Выбрать") : "Купить · " + cost, own && !sel || !own && G.SV.c >= cost, () -> {
                        if (!G.SV.skOwn.contains(id)) {
                           G.SV.c -= cost;
                           G.SV.skOwn.add(id);
                           G.sfx("coin");
                        }

                        G.SV.skTr = id;
                        G.save();
                     }
                  );
                  c.sel = sel;
                  c.swc = G.hex(sx.getString("col"));
                  cs.add(c);
               }
            }

            this.cards(cs, this.garageTab == 0 ? 2 : 3);
         }
      );
   }

   static String vehRole(String k) {
      switch (k) {
         case "pantsir":
            return "Пушка + 4 ракеты";
         case "shilka":
            return "4 ствола · без ракет";
         case "tor":
            return "8 ракет · залповый пуск";
         case "luch":
            return "Лазер · без упреждения";
         default:
            return "Зенитные разрывы";
      }
   }

   void selectVeh(String k) {
      this.g.VEH = k;
      this.g.applyStats();
      this.g.setVehicleModel();
      this.g.missiles = (int)this.g.st("maxMis");
      this.g.syncNoses();
   }

   void pShop() {
      boolean inRun = this.g.state.equals("shop");
      this.frame(
         "shop",
         920.0F,
         false,
         true,
         () -> {
            this.title("Улучшения");
            String V;
            Map<String, Integer> up;
            if (inRun) {
               V = this.g.VEH;
               up = this.g.UP();
            } else {
               List<String> own = new ArrayList<>();

               for (String v : G.VEHS) {
                  if (G.SV.vown.contains(v)) {
                     own.add(v);
                  }
               }

               if (this.shopVeh < 0 || this.shopVeh >= own.size()) {
                  this.shopVeh = Math.max(0, own.indexOf(G.SV.veh));
               }

               if (own.size() > 1) {
                  String[] nm = new String[own.size()];

                  for (int i = 0; i < nm.length; i++) {
                     nm[i] = G.vehName(own.get(i));
                  }

                  this.shopVeh = this.tabs(nm, this.shopVeh);
               }

               V = own.get(this.shopVeh);
               up = G.SV.upv.get(V);
            }

            boolean runW = inRun && this.g.runWallet();
            int coins = runW ? this.g.RUN.c : G.SV.c;
            this.d
               .text("Монеты: " + coins + (runW ? "  (монеты этого боя)" : "  (общий баланс)"), this.cx + this.cw / 2.0F, this.cy + 14.0F, 14.0F, C_ACC, 2, 0);
            this.cy += 24.0F;
            this.section("Улучшения · " + G.vehName(V));
            List<UI.Card> cs = new ArrayList<>();

            for (JsonValue kv = G.D.get("UPG_SET").get(V).child; kv != null; kv = kv.next) {
               String k = kv.asString();
               JsonValue u = G.D.get("UPG").get(k);
               Integer lvI = up.get(k);
               int l = lvI == null ? 0 : lvI;
               int mx = u.getInt("max");
               boolean isMax = l >= mx;
               int cost = isMax ? 0 : u.get("cost").getInt(l);
               String val = u.get("val").getString(Math.min(l, u.get("val").size - 1)) + (isMax ? "" : "  »  " + u.get("val").getString(l + 1));
               UI.Card c = this.card(
                  u.getString("name"), "", isMax ? "Максимум" : "Улучшить · " + cost, !isMax && coins >= cost, () -> this.buyUp(V, up, k, cost, inRun)
               );
               c.sub = val;
               c.subc = C_TXT;
               c.lv = l;
               c.max = mx;
               cs.add(c);
            }

            this.cards(cs, 4);
            this.section("Способности");
            cs.clear();
            Map<String, Boolean> ab = runW ? this.g.RUN.ab : G.SV.ab;

            for (JsonValue a = G.D.get("ABIL").child; a != null; a = a.next) {
               String k = a.name;
               boolean own = Boolean.TRUE.equals(ab.get(k));
               int cost = a.getInt("cost");
               cs.add(this.card(a.getString("name"), a.getString("desc"), own ? "Куплено" : "Купить · " + cost, !own && coins >= cost, () -> {
                  if (runW) {
                     this.g.RUN.c -= cost;
                  } else {
                     G.SV.c -= cost;
                  }

                  ab.put(k, true);
                  G.sfx("coin");
                  G.save();
               }));
            }

            this.cards(cs, 4);
            if (inRun && this.btn("Продолжить бой", 48.0F, 1, true)) {
               this.resume();
            }
         }
      );
   }

   void buyUp(String V, Map<String, Integer> up, String k, int cost, boolean inRun) {
      if (inRun && this.g.runWallet()) {
         this.g.RUN.c -= cost;
      } else {
         G.SV.c -= cost;
      }

      Integer o = up.get(k);
      up.put(k, (o == null ? 0 : o) + 1);
      G.sfx("coin");
      G.save();
      if (inRun) {
         float om = this.g.st("maxHp");
         int oms = (int)this.g.st("maxMis");
         this.g.applyStats();
         this.g.hp = this.g.hp + (this.g.st("maxHp") - om);
         this.g.missiles = Math.min((int)this.g.st("maxMis"), this.g.missiles + Math.max(0, (int)this.g.st("maxMis") - oms));
         this.g.syncNoses();
         if (k.equals("mog")) {
            this.g.setupMogs();
            String[] msg = new String[]{"", "МОГ занял позицию рядом с постом", "МОГ усилен", "Второй МОГ занял позицию"};
            this.toast(msg[Math.min(3, up.get(k))]);
         }
      } else if (V.equals(this.g.VEH)) {
         this.g.applyStats();
      }
   }

   void pQuests() {
      this.frame(
         "quests",
         580.0F,
         false,
         true,
         () -> {
            this.title("Задания дня");
            Calendar now = Calendar.getInstance();
            int left = (23 - now.get(11)) * 60 + (59 - now.get(12));
            this.label(String.format("Новые задания через %d ч %02d мин · прогресс идёт в любом бою", left / 60, left % 60), 13.0F, C_MUT, 0, 0);

            for (G.Quest q : G.SV.qList) {
               float h = 66.0F;
               boolean ready = q.done && !q.cl;
               if (!this.d.off) {
                  this.box(
                     this.cx,
                     this.cy,
                     this.cw,
                     h,
                     ready ? C_ACC : WHITE,
                     ready ? 0.08F : 0.05F,
                     ready ? C_ACC : C_LINE,
                     ready ? 1.0F : 0.22F,
                     ready ? 2.0F : 1.0F
                  );
                  this.d.text(G.D.get("QDEF").get(q.id).get("t").getString(q.lv), this.cx + 14.0F, this.cy + 24.0F, 16.0F, C_TXT, 1, -1);
                  this.bar(this.cx + 14.0F, this.cy + 34.0F, this.cw - 190.0F, 8.0F, q.p / q.n, q.done ? C_GREEN : C_ACC);
                  this.d.text(String.format("%d / %d", (int)q.p, (int)q.n), this.cx + 14.0F, this.cy + 58.0F, 12.0F, C_MUT);
               }

               String bt = q.cl ? "Получено" : (q.done ? "Забрать +" + (int)q.r : "+" + (int)q.r);
               if (this.button(this.cx + this.cw - 164.0F, this.cy + 13.0F, 150.0F, 40.0F, bt, 2, ready, 0.0F)) {
                  q.cl = true;
                  G.SV.c = G.SV.c + (int)q.r;
                  G.sfx("coin");
                  G.save();
                  this.toast("+" + (int)q.r + " монет");
               }

               if (q.cl && !this.d.off) {
                  this.d.check(this.cx + this.cw - 150.0F, this.cy + 33.0F, 6.0F, 2.5F, C_GREEN, 1.0F);
               }

               this.cy += h + 8.0F;
            }
         }
      );
   }

   static boolean achHidden(String st) {
      return st.equals("coop") || st.equals("daily");
   }

   void pAch() {
      this.frame(
         "ach",
         920.0F,
         false,
         true,
         () -> {
            this.title("Достижения");
            List<JsonValue> list = new ArrayList<>();
            int got = 0;

            for (JsonValue a = G.D.get("ACH").child; a != null; a = a.next) {
               if (!achHidden(a.getString("st"))) {
                  list.add(a);
                  if (G.SV.ach.contains(a.getString("id"))) {
                     got++;
                  }
               }
            }

            this.label(String.format("Открыто %d из %d · награда начисляется сразу", got, list.size()), 13.0F, C_MUT, 0, 0);
            int cols = Math.max(1, Math.min(3, (int)(this.cw / 260.0F)));
            float gp = 8.0F;
            float w = (this.cw - gp * (cols - 1)) / cols;

            for (int i0 = 0; i0 < list.size(); i0 += cols) {
               float rowH = 0.0F;
               boolean was = this.d.off;
               this.d.off = true;

               for (int i = i0; i < Math.min(list.size(), i0 + cols); i++) {
                  rowH = Math.max(rowH, 64.0F + this.d.wrap(list.get(i).getString("desc"), 0.0F, 0.0F, w - 24.0F, 12.0F, WHITE, 0, -1));
               }

               this.d.off = was;

               for (int i = i0; i < Math.min(list.size(), i0 + cols); i++) {
                  JsonValue ax = list.get(i);
                  float x = this.cx + (i - i0) * (w + gp);
                  float y = this.cy;
                  boolean done = G.SV.ach.contains(ax.getString("id"));
                  float n = ax.getFloat("n");
                  float val = Math.min(n, this.g.statVal(ax.getString("st")));
                  if (!this.d.off) {
                     this.box(x, y, w, rowH, done ? C_ACC : WHITE, done ? 0.1F : 0.05F, done ? C_ACC : C_LINE, done ? 0.8F : 0.22F, 1.0F);
                     this.d.text(ax.getString("name"), x + 12.0F, y + 20.0F, 15.0F, C_TXT, 1, -1);
                     this.d.star(x + w - 18.0F, y + 14.0F, 8.0F, done ? C_ACC : WHITE, done ? 1.0F : 0.15F);
                  }

                  float hh = this.d.wrap(ax.getString("desc"), x + 12.0F, y + 28.0F, w - 24.0F, 12.0F, C_MUT, 0, -1);
                  if (!this.d.off) {
                     this.bar(x + 12.0F, y + 30.0F + hh, w - 24.0F, 6.0F, val / n, done ? C_GREEN : C_ACC);
                     this.d
                        .text(
                           done ? "Получено · +" + ax.getInt("r") : String.format("%d / %d · награда %d", (int)val, (int)n, ax.getInt("r")),
                           x + 12.0F,
                           y + 30.0F + hh + 22.0F,
                           12.0F,
                           done ? C_ACC : C_MUT
                        );
                  }
               }

               this.cy += rowH + gp;
            }
         }
      );
   }

   void pRecords() {
      this.frame(
         "records",
         660.0F,
         false,
         true,
         () -> {
            this.title("Рекорды");
            this.label(
               String.format(
                  "Всего сбито: %d   ·   Боссов: %d   ·   Лучшая волна: %d   ·   Ракет: %d",
                  (int)this.g.statVal("kills"),
                  (int)this.g.statVal("bosses"),
                  (int)this.g.statVal("bestWave"),
                  (int)this.g.statVal("missiles")
               ),
               13.0F,
               C_MUT,
               0,
               0
            );
            if (G.SV.top.isEmpty()) {
               this.gap(10.0F);
               this.label("Пока пусто — сыграй бесконечный бой, и лучший результат появится здесь.", 15.0F, C_TXT, 0, 0);
               this.gap(10.0F);
            } else {
               float[] col = new float[]{0.0F, 0.08F, 0.28F, 0.44F, 0.68F, 0.88F};
               String[] hd = new String[]{"#", "Очки", "Волна", "Машина", "Сложность", "Дата"};

               for (int i = 0; i < hd.length; i++) {
                  this.d.text(hd[i], this.cx + 8.0F + col[i] * this.cw, this.cy + 12.0F, 12.0F, C_MUT);
               }

               this.cy += 20.0F;

               for (int i = 0; i < G.SV.top.size(); i++) {
                  G.Top r = G.SV.top.get(i);
                  Color c = i == 0 ? C_ACC : C_TXT;
                  if (i % 2 == 0) {
                     this.d.rect(this.cx, this.cy, this.cw, 26.0F, WHITE, 0.03F);
                  }

                  if (i == 0) {
                     this.d.rect(this.cx, this.cy, 3.0F, 26.0F, C_ACC, 1.0F);
                  }

                  JsonValue dd = G.D.get("DIFF").get(r.d);
                  String dt = r.t.length() >= 10 ? r.t.substring(8, 10) + "." + r.t.substring(5, 7) : r.t;
                  String[] vals = new String[]{
                     String.valueOf(i + 1), String.valueOf(r.s), String.valueOf(r.w), G.vehName(r.v), dd == null ? "" : dd.getString("name"), dt
                  };

                  for (int k = 0; k < vals.length; k++) {
                     this.d.text(vals[k], this.cx + 8.0F + col[k] * this.cw, this.cy + 18.0F, 15.0F, c, i == 0 ? 1 : 0, -1);
                  }

                  this.cy += 28.0F;
               }

               this.gap(6.0F);
            }
         }
      );
   }

   float slider(String t, float v, float a, float b, String disp) {
      this.d.text(t, this.cx, this.cy + 14.0F, 14.0F, C_TXT);
      this.d.text(disp, this.cx + this.cw, this.cy + 14.0F, 13.0F, C_TEAL, 2, 1);
      this.cy += 22.0F;
      float x = this.cx + 8.0F;
      float w = this.cw - 16.0F;
      float y = this.cy + 10.0F;
      float k = (v - a) / (b - a);
      if (!this.d.off) {
         this.d.rect(x, y - 2.0F, w, 4.0F, C_TEAL, 0.14F);
         this.d.rect(x, y - 2.0F, w * k, 4.0F, C_TEAL, 1.0F);
         this.d.rect(x + w * k - 6.0F, y - 9.0F, 12.0F, 18.0F, C_TEAL, 1.0F);
         this.d.rect(x + w * k - 2.0F, y - 5.0F, 4.0F, 10.0F, C_PANEL, 0.6F);
         if (this.down && in(this.downX, this.downY, x - 10.0F, y - 14.0F, w + 20.0F, 28.0F) && this.inView(this.downY)) {
            this.capture = true;
            float nk = MathUtils.clamp((this.curX - x) / w, 0.0F, 1.0F);
            v = a + nk * (b - a);
         }
      }

      this.cy += 30.0F;
      return v;
   }

   void pSettings() {
      this.frame(
         "settings",
         500.0F,
         false,
         true,
         () -> {
            this.title("Настройки");
            float ns = this.slider("Чувствительность прицела", G.SV.sens, 0.4F, 2.0F, String.format("×%.2f", G.SV.sens));
            if (Math.abs(ns - G.SV.sens) > 1.0E-4F) {
               G.SV.sens = Math.round(ns * 20.0F) / 20.0F;
            }

            float nv = this.slider("Громкость", G.SV.vol, 0.0F, 1.0F, Math.round(G.SV.vol * 100.0F) + "%");
            if (Math.abs(nv - G.SV.vol) > 1.0E-4F) {
               G.SV.vol = Math.round(nv * 20.0F) / 20.0F;
            }

            this.d.text("Графика", this.cx, this.cy + 14.0F, 14.0F, C_TXT);
            this.cy += 22.0F;
            String[] q = new String[]{"Экономная", "Средняя", "Высокая"};
            int[] st = new int[3];

            for (int i = 0; i < 3; i++) {
               st[i] = G.SV.gfx == i ? 1 : 0;
            }

            int r = this.btnRow(q, st, null, 38.0F);
            if (r >= 0 && r != G.SV.gfx) {
               G.SV.gfx = r;
               G.save();
               this.host.applyGfx();
            }

            if (this.toggle("Вибрация", null, G.SV.vib)) {
               G.SV.vib = !G.SV.vib;
               G.save();
            }

            if (this.DESK && this.toggle("Полный экран", null, this.host.isFullscreen())) {
               this.host.toggleFullscreen();
            }

            if (this.DESK) {
               this.label(
                  "Мышь — наведение · ЛКМ или пробел — огонь · ПКМ или E — ракета · 1–4 — способности · B — улучшения · Esc — пауза · Tab — отпустить курсор · F11 — полный экран",
                  12.0F,
                  C_MUT
               );
            }

            if (this.btn("Обучение заново", 38.0F, 0, true)) {
               G.SV.tut = false;
               G.save();
               this.toast("Обучение покажется в следующем бою");
            }
         }
      );
      if (!this.down && this.capture) {
         this.capture = false;
         G.save();
      }
   }

   void pAbout() {
      this.frame("about", 640.0F, false, true, () -> {
         this.title("Об игре");
         this.label("Щит неба 3D · версия 3.1.2 · 6+", 16.0F, C_TXT, 1, -1);
         this.label("Аркада про противовоздушную оборону. Все организации, персонажи, искусственные интеллекты и боевые машины вымышлены.", 14.0F, C_TXT);
         this.label("Конфиденциальность: игра не собирает и не передаёт персональные данные. Прогресс хранится только на устройстве.", 14.0F, C_TXT);
         this.label("Движок libGDX (лицензия Apache 2.0). Шрифты Russo One и Exo 2 — SIL Open Font License 1.1.", 14.0F, C_MUT);
      });
   }

   void pPause() {
      this.frame(
         "pause",
         400.0F,
         false,
         true,
         () -> {
            this.title("Пауза");
            if (this.g.mode.equals("endless")) {
               this.d
                  .text(
                     String.format("Волна %d  ·  очки %d  ·  сбито %d", Math.max(1, this.g.wave), this.g.score, this.g.kills),
                     this.cx + this.cw / 2.0F,
                     this.cy + 13.0F,
                     13.0F,
                     C_MUT,
                     0,
                     0
                  );
               this.cy += 24.0F;
            }

            if (this.btn("Продолжить", 50.0F, 1, true)) {
               this.resume();
            }

            if (!this.g.mode.equals("duel") && this.btn("Улучшения", 42.0F, 0, true)) {
               this.openShopBattle();
            }

            if (this.btn("Настройки", 42.0F, 0, true)) {
               this.show("settings");
            }

            if (this.btn("Выйти из боя", 42.0F, 0, true)) {
               if (this.duel != null && this.g.mode.equals("duel")) {
                  this.duel.leave();
               }

               this.toMenu();
            }
         }
      );
   }

   void pOver() {
      this.frame(
         "over",
         480.0F,
         false,
         true,
         () -> {
            this.title("Комплекс подбит");
            if (this.overRec) {
               float k = 0.75F + 0.25F * MathUtils.sin(this.T * 4.0F);
               this.d.text("НОВЫЙ РЕКОРД!", this.cx + this.cw / 2.0F, this.cy + 18.0F, 20.0F, new Color(1.0F, 0.7F * k + 0.1F, 0.1F, 1.0F), 3, 0);
               this.cy += 30.0F;
            }

            String[][] cells = new String[][]{
               {"Очки", String.valueOf(this.g.score)},
               {"Волна", String.valueOf(this.g.wave)},
               {"Сбито", String.valueOf(this.g.kills)},
               {"В копилку", "+" + Math.round(this.g.runCoins * 0.3F)}
            };
            float gp = 6.0F;
            float w = (this.cw - gp * 3.0F) / 4.0F;

            for (int i = 0; i < 4; i++) {
               float x = this.cx + i * (w + gp);
               if (!this.d.off) {
                  this.box(x, this.cy, w, 58.0F, WHITE, 0.05F, C_LINE, 0.22F, 1.0F);
                  this.d.text(cells[i][0], x + w / 2.0F, this.cy + 18.0F, 12.0F, C_MUT, 0, 0);
                  this.d.text(cells[i][1], x + w / 2.0F, this.cy + 46.0F, 22.0F, i == 3 ? C_ACC : C_TXT, 1, 0);
               }
            }

            this.cy += 66.0F;
            if (this.g.xpGot > 0) {
               this.d.text("Опыт +" + this.g.xpGot, this.cx, this.cy + 13.0F, 13.0F, C_RANK, 2, -1);
               this.d.text(G.RANK_N[G.rankIndex()], this.cx + this.cw, this.cy + 13.0F, 13.0F, C_RANK, 0, 1);
               this.cy += 20.0F;
               this.bar(this.cx, this.cy, this.cw, 6.0F, this.rankK(), C_RANK);
               this.cy += 14.0F;
               if (this.g.rankUp >= 0) {
                  this.label(String.format("Новое звание: %s!  +%d монет", G.RANK_N[this.g.rankUp], 100 * this.g.rankUp), 18.0F, C_ACC, 1, 0);
               }
            }

            int r = this.btnRow(new String[]{"Ещё раз", "Меню"}, new int[]{1, 0}, null, 48.0F);
            if (r == 0) {
               this.g.rankUp = -1;
               this.clearComm();
               this.g.quitBattle();
               this.launch("endless");
            } else if (r == 1) {
               this.g.rankUp = -1;
               this.toMenu();
            }
         }
      );
   }

   int dailyDay() {
      int st = G.SV.dailyStreak;
      if (!G.SV.dailyLast.equals(G.today(-1))) {
         st = 0;
      }

      return st % G.D.get("DAILY").size + 1;
   }

   void pDaily() {
      this.frame("daily", 620.0F, false, true, () -> {
         this.title("Ежедневная награда");
         this.label("Заходи каждый день подряд — награда растёт. Пропустишь день — счёт начнётся заново.", 13.0F, C_MUT, 0, 0);
         int day = this.dailyDay();
         JsonValue D = G.D.get("DAILY");
         int n = D.size;
         float gp = 6.0F;
         float w = (this.cw - gp * (n - 1)) / n;
         float h = 84.0F;

         for (int i = 0; i < n; i++) {
            float x = this.cx + i * (w + gp);
            boolean cur = i + 1 == day;
            boolean past = i + 1 < day;
            if (!this.d.off) {
               float pulse = cur ? 0.1F + 0.05F * MathUtils.sin(this.T * 4.0F) : 0.04F;
               this.box(x, this.cy, w, h, cur ? C_ACC : WHITE, pulse, cur ? C_ACC : C_LINE, cur ? 1.0F : 0.22F, cur ? 2.0F : 1.0F);
               this.d.text("День " + (i + 1), x + w / 2.0F, this.cy + 26.0F, 12.0F, C_MUT, 0, 0);
               if (past) {
                  this.d.check(x + w / 2.0F, this.cy + 52.0F, 9.0F, 3.0F, C_GREEN, 1.0F);
               } else {
                  this.d.text("+" + D.getInt(i), x + w / 2.0F, this.cy + 60.0F, 18.0F, cur ? C_ACC : new Color(1.0F, 1.0F, 1.0F, 0.6F), 1, 0);
               }
            }
         }

         this.cy += h + 12.0F;
         int rw = D.getInt(day - 1);
         if (this.btn("Забрать +" + rw + " монет", 50.0F, 1, true)) {
            G.SV.dailyLast = G.today();
            G.SV.dailyStreak = day;
            G.SV.c += rw;
            G.addXp(10);
            G.save();
            G.sfx("clear");
            this.toast("+" + rw + " монет · день " + day);
            this.show("menu");
         }
      });
   }

   static JsonValue chapters() {
      return G.D.get("CHAPTERS");
   }

   static int campOf(String id) {
      Integer v = G.SV.camp.get(id);
      return v == null ? 0 : v;
   }

   int chStars(int ci) {
      int n = 0;

      for (JsonValue m = chapters().get(ci).get("missions").child; m != null; m = m.next) {
         n += campOf(m.getString("id"));
      }

      return n;
   }

   int allStars() {
      int n = 0;

      for (int i = 0; i < chapters().size; i++) {
         n += this.chStars(i);
      }

      return n;
   }

   int allMissions() {
      int n = 0;

      for (JsonValue c = chapters().child; c != null; c = c.next) {
         n += c.get("missions").size;
      }

      return n;
   }

   int curChapter() {
      for (int ci = 0; ci < chapters().size; ci++) {
         for (JsonValue m = chapters().get(ci).get("missions").child; m != null; m = m.next) {
            if (campOf(m.getString("id")) == 0) {
               return ci;
            }
         }
      }

      return chapters().size - 1;
   }

   int[] chapterOf(JsonValue m) {
      String id = m.getString("id");

      for (int ci = 0; ci < chapters().size; ci++) {
         JsonValue ms = chapters().get(ci).get("missions");

         for (int mi = 0; mi < ms.size; mi++) {
            if (ms.get(mi).getString("id").equals(id)) {
               return new int[]{ci, mi};
            }
         }
      }

      return new int[]{-1, -1};
   }

   boolean unlocked(int ci, int mi) {
      if (mi == 0) {
         if (ci == 0) {
            return true;
         } else {
            JsonValue pm = chapters().get(ci - 1).get("missions");
            return campOf(pm.get(pm.size - 1).getString("id")) > 0;
         }
      } else {
         return campOf(chapters().get(ci).get("missions").get(mi - 1).getString("id")) > 0;
      }
   }

   JsonValue nextMission(JsonValue m) {
      int[] p = this.chapterOf(m);
      if (p[0] < 0) {
         return null;
      } else {
         JsonValue ms = chapters().get(p[0]).get("missions");
         if (p[1] + 1 < ms.size) {
            return ms.get(p[1] + 1);
         } else {
            return p[0] + 1 < chapters().size ? chapters().get(p[0] + 1).get("missions").get(0) : null;
         }
      }
   }

   public JsonValue findMission(String id) {
      for (JsonValue c = chapters().child; c != null; c = c.next) {
         for (JsonValue m = c.get("missions").child; m != null; m = m.next) {
            if (m.getString("id").equals(id)) {
               return m;
            }
         }
      }

      for (JsonValue mx = G.D.get("CHALLENGES").child; mx != null; mx = mx.next) {
         if (mx.getString("id").equals(id)) {
            return mx;
         }
      }

      return null;
   }

   public void debugBrief(String id) {
      this.openBrief(this.findMission(id));
   }

   public void debugLaunch(String id) {
      this.launchMission(this.findMission(id));
   }

   public void debugDuelHost() {
      this.duelView = "host";
      this.hostWith("pvo", "pantsir");
      this.show("duel");
   }

   public void debugDuelJoin(String ip) {
      this.duelView = "join";
      this.show("duel");
      this.duel.join(ip);
   }

   public void debugCut(int ch) {
      this.cutCh = ch;
      this.cutThen = null;
      this.show("cut");
   }

   void openCampaign() {
      if (!G.SV.cuts.contains("1")) {
         this.cutCh = 0;
         this.cutThen = null;
         this.show("cut");
      } else {
         this.show("campaign");
      }
   }

   void openMission(JsonValue m) {
      int[] p = this.chapterOf(m);
      if (p[0] >= 0 && !G.SV.cuts.contains(String.valueOf(p[0] + 1))) {
         this.cutCh = p[0];
         this.cutThen = m;
         this.show("cut");
      } else {
         this.openBrief(m);
      }
   }

   void openBrief(JsonValue m) {
      this.curMission = m;
      boolean chal = "chal".equals(m.getString("special", ""));
      this.backTo.put("brief", chal ? "chal" : "campaign");
      this.show("brief");
   }

   void launchMission(JsonValue m) {
      this.pending = () -> {
         this.show("");
         this.g.startGame("campaign", m);
      };
      this.pendingFrames = 1;
   }

   void stars(float x, float cyy, int n, int total, float r) {
      for (int i = 0; i < total; i++) {
         this.d.star(x + r + i * (r * 2.0F + 4.0F), cyy, r, i < n ? C_ACC : WHITE, i < n ? 1.0F : 0.14F);
      }
   }

   float dialogLine(String who, String text, float x, float y, float w) {
      JsonValue c = G.D.get("CH").get(who);
      String name = c != null ? c.getString("name") : who;
      String ava = c != null ? c.getString("ava") : "?";
      Color col = c != null ? C(c.getString("col")) : WHITE;
      boolean was = this.d.off;
      this.d.off = true;
      float th = this.d.wrap(text, 0.0F, 0.0F, w - 70.0F, 14.0F, WHITE, 0, -1);
      this.d.off = was;
      float h = Math.max(58.0F, 30.0F + th + 6.0F);
      if (!this.d.off) {
         this.box(x, y, w, h, WHITE, 0.05F, C_LINE, 0.22F, 1.0F);
         this.d.rect(x + 12.0F, y + 10.0F, 38.0F, 38.0F, col, 1.0F);
         this.d.text(ava, x + 31.0F, y + 36.0F, 18.0F, C("0b1220"), 1, 0);
         this.d.text(name, x + 62.0F, y + 22.0F, 12.0F, col, 2, -1);
      }

      this.d.wrap(text, x + 62.0F, y + 26.0F, w - 74.0F, 14.0F, C_TXT, 0, -1);
      return h;
   }

   static String typeName(JsonValue m) {
      String t = m.getString("type", "waves");
      if (t.equals("timer")) {
         return "Удержание · " + m.getInt("time", 0) + " с";
      } else if (t.equals("escort")) {
         return "Сопровождение · " + m.getInt("time", 0) + " с";
      } else {
         int n = m.has("waves") ? m.get("waves").size : 0;
         return n + (n == 1 ? " волна" : (n < 5 ? " волны" : " волн"));
      }
   }

   void pCampaign() {
      this.frame(
         "campaign",
         920.0F,
         false,
         true,
         () -> {
            this.title("Кампания");
            int all = this.allStars();
            int mx = this.allMissions() * 3;
            this.d.text(String.format("Звёзды: %d / %d", all, mx), this.cx + this.cw / 2.0F, this.cy + 13.0F, 13.0F, C_ACC, 2, 0);
            this.cy += 20.0F;
            this.bar(this.cx + this.cw * 0.3F, this.cy, this.cw * 0.4F, 4.0F, (float)all / mx, C_ACC);
            this.cy += 14.0F;
            int cur = this.curChapter();

            for (int ci = 0; ci < chapters().size; ci++) {
               JsonValue ch = chapters().get(ci);
               boolean open = this.unlocked(ci, 0);
               JsonValue ms = ch.get("missions");
               boolean expanded = open && (this.chOpen.containsKey(ci) ? this.chOpen.get(ci) : ci == cur);
               float hh = 40.0F;
               if (!this.d.off) {
                  this.box(this.cx, this.cy, this.cw, hh, open ? C_TEAL : WHITE, open ? (expanded ? 0.1F : 0.05F) : 0.02F, C_LINE, open ? 0.3F : 0.1F, 1.0F);
                  if (open) {
                     this.d.rect(this.cx, this.cy, 3.0F, hh, ci == cur ? C_ACC : C_TEAL, 1.0F);
                  }

                  this.d.text(ch.getString("name"), this.cx + 16.0F, this.cy + 26.0F, 17.0F, open ? C_TXT : new Color(1.0F, 1.0F, 1.0F, 0.35F), 1, -1);
                  if (open) {
                     int cs = this.chStars(ci);
                     this.d.text(cs + "/" + ms.size * 3, this.cx + this.cw - 46.0F, this.cy + 26.0F, 14.0F, C_ACC, 2, 1);
                     this.d.star(this.cx + this.cw - 32.0F, this.cy + 20.0F, 7.0F, C_ACC, 1.0F);
                     float ax = this.cx + this.cw - 14.0F;
                     float ay = this.cy + 20.0F;
                     if (expanded) {
                        this.d.line(ax - 5.0F, ay - 2.0F, ax, ay + 3.0F, 2.0F, C_MUT, 1.0F);
                        this.d.line(ax, ay + 3.0F, ax + 5.0F, ay - 2.0F, 2.0F, C_MUT, 1.0F);
                     } else {
                        this.d.line(ax - 2.0F, ay - 5.0F, ax + 3.0F, ay, 2.0F, C_MUT, 1.0F);
                        this.d.line(ax + 3.0F, ay, ax - 2.0F, ay + 5.0F, 2.0F, C_MUT, 1.0F);
                     }
                  } else {
                     this.d.text("откроется после главы " + ci, this.cx + this.cw - 14.0F, this.cy + 25.0F, 13.0F, C_MUT, 0, 1);
                  }
               }

               if (open && this.tap(this.cx, this.cy, this.cw, hh)) {
                  this.chOpen.put(ci, !expanded);
                  G.sfx("click");
               }

               this.cy += hh + 8.0F;
               if (expanded) {
                  int cols = Math.max(1, Math.min(3, (int)(this.cw / 250.0F)));
                  float gp = 8.0F;
                  float w = (this.cw - gp * (cols - 1)) / cols;

                  for (int i0 = 0; i0 < ms.size; i0 += cols) {
                     float rowH = 0.0F;
                     boolean was = this.d.off;
                     this.d.off = true;

                     for (int i = i0; i < Math.min(ms.size, i0 + cols); i++) {
                        rowH = Math.max(rowH, 112.0F + this.d.wrap(ms.get(i).getString("goal"), 0.0F, 0.0F, w - 24.0F, 12.0F, WHITE, 0, -1));
                     }

                     this.d.off = was;

                     for (int i = i0; i < Math.min(ms.size, i0 + cols); i++) {
                        JsonValue m = ms.get(i);
                        float x = this.cx + (i - i0) * (w + gp);
                        float y = this.cy;
                        boolean op = this.unlocked(ci, i);
                        int got = campOf(m.getString("id"));
                        boolean next = op && got == 0;
                        if (!this.d.off) {
                           this.box(
                              x,
                              y,
                              w,
                              rowH,
                              next ? C_ACC : WHITE,
                              op ? (next ? 0.06F : 0.06F) : 0.025F,
                              next ? C_ACC : C_LINE,
                              next ? 1.0F : 0.22F,
                              next ? 2.0F : 1.0F
                           );
                           this.d.text(m.getString("id") + "  ·  " + typeName(m), x + 12.0F, y + 20.0F, 12.0F, C_MUT);
                           this.stars(x + w - 12.0F - 60.0F, y + 15.0F, got, 3, 8.0F);
                           this.d.text(m.getString("title"), x + 12.0F, y + 44.0F, 16.0F, op ? C_TXT : new Color(1.0F, 1.0F, 1.0F, 0.4F), 1, -1);
                        }

                        this.d.wrap(m.getString("goal"), x + 12.0F, y + 52.0F, w - 24.0F, 12.0F, C_MUT, 0, -1);
                        if (this.button(
                           x + 12.0F, y + rowH - 12.0F - 34.0F, w - 24.0F, 34.0F, op ? (got == 0 ? "Начать" : "Переиграть") : "Закрыто", 2, op, 14.0F
                        )) {
                           this.openMission(m);
                        }
                     }

                     this.cy += rowH + gp;
                  }

                  this.gap(4.0F);
               }
            }
         }
      );
   }

   void pCut() {
      this.frame("cut", 660.0F, false, true, () -> {
         JsonValue cut = G.D.get("CUTS").get(String.valueOf(this.cutCh + 1));
         this.d.text("ГЛАВА " + (this.cutCh + 1), this.cx + this.cw / 2.0F, this.cy + 14.0F, 14.0F, C_ACC, 3, 0);
         this.cy += 22.0F;
         this.title(cut.getString("title"));

         for (JsonValue l = cut.get("lines").child; l != null; l = l.next) {
            this.cy = this.cy + (this.dialogLine(l.getString(0), l.getString(1), this.cx, this.cy, this.cw) + 8.0F);
         }

         if (this.btn("Далее", 48.0F, 1, true)) {
            String k = String.valueOf(this.cutCh + 1);
            if (!G.SV.cuts.contains(k)) {
               G.SV.cuts.add(k);
               G.save();
            }

            if (this.cutThen != null) {
               this.openBrief(this.cutThen);
            } else {
               this.show("campaign");
            }
         }
      });
   }

   void pBrief() {
      JsonValue m = this.curMission;
      if (m == null) {
         this.show("campaign");
      } else {
         this.frame(
            "brief",
            660.0F,
            false,
            true,
            () -> {
               boolean chal = "chal".equals(m.getString("special", ""));
               this.d.text(chal ? "ИСПЫТАНИЕ" : "МИССИЯ " + m.getString("id"), this.cx + this.cw / 2.0F, this.cy + 14.0F, 14.0F, C_ACC, 3, 0);
               this.cy += 22.0F;
               this.title(m.getString("title"));
               this.label("Задача: " + m.getString("goal"), 15.0F, C_TXT, 2, 0);
               this.d.text(typeName(m), this.cx + this.cw / 2.0F, this.cy + 11.0F, 12.0F, C_MUT, 0, 0);
               this.cy += 20.0F;
               if (m.has("brief")) {
                  for (JsonValue l = m.get("brief").child; l != null; l = l.next) {
                     this.cy = this.cy + (this.dialogLine(l.getString(0), l.getString(1), this.cx, this.cy, this.cw) + 8.0F);
                  }
               }

               if (m.has("mods")) {
                  for (JsonValue md = m.get("mods").child; md != null; md = md.next) {
                     JsonValue M = G.D.get("MODS").get(md.asString());
                     if (M != null) {
                        this.label("•  " + M.getString("name") + ": " + M.getString("desc"), 14.0F, C("ffb0a0"));
                     }
                  }
               }

               Integer pvI = chal ? G.SV.chal.get(m.getString("id")) : G.SV.camp.get(m.getString("id"));
               int prev = pvI == null ? 0 : pvI;
               this.section("Звёзды");
               JsonValue st = m.get("stars");

               for (int i = 0; i < st.size; i++) {
                  this.d.star(this.cx + 10.0F, this.cy + 9.0F, 8.0F, i < prev ? C_ACC : WHITE, i < prev ? 1.0F : 0.14F);
                  this.d.text(st.get(i).getString("t"), this.cx + 28.0F, this.cy + 14.0F, 14.0F, C_TXT);
                  this.cy += 24.0F;
               }

               this.gap(2.0F);
               int rw = m.getInt("reward");
               this.label(
                  String.format(
                     "Награда: %d монет%s",
                     prev == 0 ? rw : Math.round(rw * 0.25F),
                     prev == 0 ? "" : (chal ? " (повторное прохождение)" : " (за новые звёзды — бонус)")
                  ),
                  13.0F,
                  C_ACC,
                  2,
                  -1
               );
               if (this.btn("В бой", 50.0F, 1, true)) {
                  this.launchMission(m);
               }
            }
         );
      }
   }

   void pResult() {
      JsonValue m = this.g.resM;
      if (m == null) {
         this.show("menu");
      } else {
         this.frame(
            "result",
            560.0F,
            false,
            true,
            () -> {
               boolean chal = "chal".equals(m.getString("special", ""));
               boolean win = this.g.resWin;
               this.title(chal ? (win ? "Испытание пройдено" : "Испытание провалено") : (win ? "Миссия выполнена" : "Миссия провалена"));
               this.d
                  .text(
                     chal ? m.getString("title") : m.getString("id") + " · " + m.getString("title"),
                     this.cx + this.cw / 2.0F,
                     this.cy + 14.0F,
                     14.0F,
                     C_MUT,
                     0,
                     0
                  );
               this.cy += 24.0F;
               if (win) {
                  float k = Math.min(1.0F, this.panelT / 0.9F);

                  for (int i = 0; i < 3; i++) {
                     float appear = MathUtils.clamp(k * 3.0F - i, 0.0F, 1.0F);
                     boolean on = i < this.g.resGot;
                     float r = 22.0F * (on ? 0.6F + 0.4F * Interp(appear) : 1.0F);
                     this.d
                        .star(
                           this.cx + this.cw / 2.0F + (i - 1) * 56, this.cy + 24.0F, r, on && appear > 0.0F ? C_ACC : WHITE, on && appear > 0.0F ? 1.0F : 0.14F
                        );
                  }

                  this.cy += 56.0F;
                  JsonValue st = m.get("stars");

                  for (int i = 0; i < st.size; i++) {
                     boolean ok = this.g.resOk != null && i < this.g.resOk.length && this.g.resOk[i];
                     this.d.star(this.cx + 10.0F, this.cy + 9.0F, 8.0F, ok ? C_ACC : WHITE, ok ? 1.0F : 0.14F);
                     this.d.text(st.get(i).getString("t"), this.cx + 28.0F, this.cy + 14.0F, 14.0F, new Color(C_TXT.r, C_TXT.g, C_TXT.b, ok ? 1.0F : 0.5F));
                     this.cy += 24.0F;
                  }

                  this.gap(4.0F);
                  this.label(this.g.resReward > 0 ? "Награда: +" + this.g.resReward + " монет" : "Награда за миссию уже получена", 16.0F, C_ACC, 1, 0);
                  if (this.g.xpGot > 0) {
                     this.d.text("Опыт +" + this.g.xpGot, this.cx, this.cy + 13.0F, 13.0F, C_RANK, 2, -1);
                     this.d.text(G.RANK_N[G.rankIndex()], this.cx + this.cw, this.cy + 13.0F, 13.0F, C_RANK, 0, 1);
                     this.cy += 20.0F;
                     this.bar(this.cx, this.cy, this.cw, 6.0F, this.rankK(), C_RANK);
                     this.cy += 14.0F;
                     if (this.g.rankUp >= 0) {
                        this.label(String.format("Новое звание: %s!  +%d монет", G.RANK_N[this.g.rankUp], 100 * this.g.rankUp), 18.0F, C_ACC, 1, 0);
                     }
                  }

                  if (m.has("debrief")) {
                     for (JsonValue l = m.get("debrief").child; l != null; l = l.next) {
                        this.cy = this.cy + (this.dialogLine(l.getString(0), l.getString(1), this.cx, this.cy, this.cw) + 8.0F);
                     }
                  }

                  if (!chal) {
                     JsonValue nxt = this.nextMission(m);
                     if (nxt == null) {
                        this.label("Кампания пройдена! Небо над долиной чистое. Спасибо, командир!", 14.0F, C_TXT, 0, 0);
                     } else if (this.btn("Следующая: " + nxt.getString("title"), 48.0F, 1, true)) {
                        this.g.rankUp = -1;
                        this.clearComm();
                        this.g.quitBattle();
                        this.pending = () -> {
                           this.host.toMenuWorld();
                           this.openMission(nxt);
                        };
                        this.pendingFrames = 1;
                     }
                  }
               } else {
                  this.label(this.g.resReason, 16.0F, C("ff8a7a"), 2, 0);
                  this.label(
                     this.g.hasMod("noUpg")
                        ? "Здесь улучшения не работают — только точность и способности."
                        : "Совет: загляни в «Улучшения» — прокачка из меню действует и здесь.",
                     13.0F,
                     C_MUT,
                     0,
                     0
                  );
               }

               int r = this.btnRow(new String[]{"Повторить", chal ? "К испытаниям" : "К миссиям"}, new int[]{win ? 0 : 1, 0}, null, 44.0F);
               if (r == 0) {
                  this.g.rankUp = -1;
                  this.clearComm();
                  this.g.quitBattle();
                  this.launchMission(m);
               } else if (r == 1) {
                  this.g.rankUp = -1;
                  this.toMenu();
               }
            }
         );
      }
   }

   void pChal() {
      this.frame("chal", 920.0F, false, true, () -> {
         this.title("Испытания");
         JsonValue list = G.D.get("CHALLENGES");
         int done = 0;

         for (JsonValue c = list.child; c != null; c = c.next) {
            Integer v = G.SV.chal.get(c.getString("id"));
            if (v != null && v > 0) {
               done++;
            }
         }

         this.label(String.format("Особые бои с жёсткими условиями · пройдено %d из %d", done, list.size), 13.0F, C_MUT, 0, 0);
         int cols = Math.max(1, Math.min(3, (int)(this.cw / 260.0F)));
         float gp = 8.0F;
         float w = (this.cw - gp * (cols - 1)) / cols;

         for (int i0 = 0; i0 < list.size; i0 += cols) {
            float rowH = 0.0F;
            boolean was = this.d.off;
            this.d.off = true;

            for (int i = i0; i < Math.min(list.size, i0 + cols); i++) {
               JsonValue cx = list.get(i);
               float mh = 0.0F;
               if (cx.has("mods")) {
                  for (JsonValue md = cx.get("mods").child; md != null; md = md.next) {
                     mh += 16.0F;
                  }
               }

               rowH = Math.max(rowH, 116.0F + mh + this.d.wrap(cx.getString("goal"), 0.0F, 0.0F, w - 24.0F, 12.0F, WHITE, 0, -1));
            }

            this.d.off = was;

            for (int i = i0; i < Math.min(list.size, i0 + cols); i++) {
               JsonValue cx = list.get(i);
               Integer gv = G.SV.chal.get(cx.getString("id"));
               int got = gv == null ? 0 : gv;
               float x = this.cx + (i - i0) * (w + gp);
               float y = this.cy;
               if (!this.d.off) {
                  this.box(x, y, w, rowH, got > 0 ? C_GREEN : WHITE, got > 0 ? 0.05F : 0.06F, got > 0 ? C_GREEN : C_LINE, got > 0 ? 0.5F : 0.22F, 1.0F);
                  this.d.text(cx.getString("title"), x + 12.0F, y + 22.0F, 16.0F, C_TXT, 1, -1);
                  this.stars(x + w - 12.0F - 60.0F, y + 15.0F, got, 3, 8.0F);
               }

               float yy = y + 30.0F;
               yy += this.d.wrap(cx.getString("goal"), x + 12.0F, yy, w - 24.0F, 12.0F, C_MUT, 0, -1);
               if (cx.has("mods")) {
                  for (JsonValue md = cx.get("mods").child; md != null; md = md.next) {
                     JsonValue M = G.D.get("MODS").get(md.asString());
                     this.d.text("• " + (M != null ? M.getString("name") : md.asString()), x + 12.0F, yy + 12.0F, 12.0F, C("ffb0a0"));
                     yy += 16.0F;
                  }
               }

               int rw = cx.getInt("reward");
               this.d.text("Награда: " + (got == 0 ? rw : Math.round(rw * 0.25F)), x + 12.0F, yy + 14.0F, 12.0F, C_ACC, 2, -1);
               if (this.button(x + 12.0F, y + rowH - 12.0F - 34.0F, w - 24.0F, 34.0F, got == 0 ? "Начать" : "Переиграть", 2, true, 14.0F)) {
                  this.openBrief(cx);
               }
            }

            this.cy += rowH + gp;
         }
      });
   }

   void drawToast() {
      if (!(this.toastT <= 0.0F) && this.toastS != null) {
         float a = MathUtils.clamp(this.toastT / 0.25F, 0.0F, 1.0F) * MathUtils.clamp((2.3F - this.toastT) / 0.15F, 0.0F, 1.0F);
         float tw = this.d.textW(this.toastS, 14.0F, 1) + 32.0F;
         float x = (this.d.w - tw) / 2.0F;
         float y = 64.0F;
         this.d.rect(x, y, tw, 40.0F, new Color(0.04F, 0.07F, 0.12F, 1.0F), 0.92F * a);
         this.d.frame(x, y, tw, 40.0F, 1.0F, new Color(1.0F, 0.71F, 0.28F, 1.0F), 0.5F * a);
         this.d.rect(x, y, 3.0F, 40.0F, C_ACC, a);
         this.d.text(this.toastS, this.d.w / 2.0F, y + 25.0F, 14.0F, new Color(1.0F, 1.0F, 1.0F, a), 1, 0);
      }
   }

   boolean proj(Vector3 p, Vector2 out) {
      PerspectiveCamera cam = this.g.cam;
      this.tv.set(p).sub(cam.position);
      if (this.tv.dot(cam.direction) <= 0.05F) {
         return false;
      } else {
         this.tv.set(p);
         cam.project(this.tv);
         out.x = this.tv.x / this.d.s;
         out.y = (Gdx.graphics.getHeight() - this.tv.y) / this.d.s;
         return true;
      }
   }

   float sx() {
      return this.d.w;
   }

   float sy() {
      return this.d.h;
   }

   float[] fireC() {
      return new float[]{this.sx() - 22.0F - 59.0F, this.sy() - 22.0F - 59.0F};
   }

   float[] misC() {
      return new float[]{this.sx() - 22.0F - 128.0F - 42.0F, this.sy() - 40.0F - 42.0F};
   }

   float[] abRect(int i) {
      return new float[]{this.sx() - 26.0F - 100.0F, this.sy() - 150.0F - 40.0F - i * 44, 100.0F, 36.0F};
   }

   List<String> abilList() {
      List<String> out = new ArrayList<>();

      for (String k : new String[]{"salvo", "emp", "smoke", "repair"}) {
         if (Boolean.TRUE.equals(this.g.AB().get(k))) {
            out.add(k);
         }
      }

      return out;
   }

   boolean vehMis() {
      return this.g.VEH.equals("pantsir") && this.g.st("maxMis") > 0.0F;
   }

   void rrect(float x, float y, float w, float h, Color c, float a, boolean border) {
      this.d.rect(x, y, w, h, c, a);
      if (border) {
         this.d.frame(x, y, w, h, 1.0F, C_LINE, 0.22F);
      }
   }

   void drawHud() {
      Game G0 = this.g;
      float W = this.sx();
      float H = this.sy();
      this.rrect(PAUSE_R[0], PAUSE_R[1], PAUSE_R[2], PAUSE_R[3], C_PANEL, 0.94F, true);
      this.d.rect(29.0F, 27.0F, 5.0F, 18.0F, WHITE);
      this.d.rect(39.0F, 27.0F, 5.0F, 18.0F, WHITE);
      this.rrect(SHOP_R[0], SHOP_R[1], SHOP_R[2], SHOP_R[3], C_ACC, 1.0F, false);
      this.d.text("₽", 36.0F, 101.0F, 24.0F, C("24170a"), 2, 0);
      this.rrect(72.0F, 12.0F, 170.0F, 70.0F, C_PANEL, 0.94F, true);
      this.d.rect(72.0F, 16.0F, 3.0F, 62.0F, C_ACC);
      this.d.text(String.valueOf(G0.score), 84.0F, 37.0F, 24.0F, WHITE, 1, -1);
      this.d.text(this.hudWaveText(), 84.0F, 57.0F, 13.0F, C_MUT);
      this.d.text(G0.runWallet() ? "Монеты боя: " + G0.RUN.c : "+" + G0.runCoins, 84.0F, 75.0F, 13.0F, C_ACC, 2, -1);
      if (G0.combo >= 3 && G0.comboT > 0.0F) {
         float k = MathUtils.clamp(G0.comboT / 3.0F, 0.0F, 1.0F);
         this.d.text("Серия ×" + G0.combo, 77.0F, 107.0F, 20.0F, BLACK, 1, -1);
         this.d.text("Серия ×" + G0.combo, 76.0F, 106.0F, 20.0F, new Color(C_ACC.r, C_ACC.g, C_ACC.b, 0.5F + 0.5F * k), 1, -1);
      }

      float maxHp = G0.st("maxHp");
      float hpw = Math.min(340.0F, W * 0.36F);
      float hx = (W - hpw) / 2.0F;
      this.rrect(hx, 12.0F, hpw, G0.obj != null ? 72.0F : 44.0F, C_PANEL, 0.94F, true);
      this.d.text("Прочность комплекса", hx + 12.0F, 32.0F, 12.0F, C_MUT);
      this.d.text(String.format("%d/%d", (int)Math.ceil(G0.hp), (int)maxHp), hx + hpw - 12.0F, 32.0F, 12.0F, C_MUT, 0, 1);
      float hk = G0.hp / maxHp;
      if (this.hpShown < 0.0F) {
         this.hpShown = hk;
         this.hpGhost = hk;
      }

      this.hpShown = MathUtils.lerp(this.hpShown, hk, 0.3F);
      if (this.hpGhost < hk) {
         this.hpGhost = hk;
      } else {
         this.hpGhost = Math.max(hk, this.hpGhost - Gdx.graphics.getDeltaTime() * 0.25F);
      }

      Color hc = hk > 0.5F ? C_GREEN : (hk > 0.25F ? C_WARN : C_RED);
      float bx = hx + 12.0F;
      float bw = hpw - 24.0F;
      this.d.rect(bx, 39.0F, bw, 8.0F, WHITE, 0.08F);
      this.d.rect(bx, 39.0F, bw * MathUtils.clamp(this.hpGhost, 0.0F, 1.0F), 8.0F, WHITE, 0.45F);
      this.d.rect(bx, 39.0F, bw * MathUtils.clamp(this.hpShown, 0.0F, 1.0F), 8.0F, hc, 1.0F);
      if (G0.smokeT > 0.0F) {
         this.d.text("ДЫМ", hx + hpw / 2.0F, 32.0F, 11.0F, C_MUT, 2, 0);
      }

      if (G0.obj != null) {
         this.d.text(G0.obj.name, hx + 12.0F, 64.0F, 12.0F, C_MUT);
         this.d.text(Math.round(G0.obj.hp / G0.obj.max * 100.0F) + "%", hx + hpw - 12.0F, 64.0F, 12.0F, C_MUT, 0, 1);
         this.bar(bx, 70.0F, bw, 8.0F, G0.obj.hp / G0.obj.max, C("5aa8ff"));
      }

      float by = (G0.obj != null ? 84 : 56) + 12;

      for (ArrayIterator cxx = G0.bosses.iterator(); cxx.hasNext(); by += 34.0F) {
         Game.E b = (Game.E)cxx.next();
         float bww = Math.min(460.0F, W * 0.5F);
         String bn = G.D.get("BOSS_INFO").get(b.type).getString("name");
         this.d.text(bn, W / 2.0F + 1.0F, by + 13.0F, 13.0F, BLACK, 1, 0);
         this.d.text(bn, W / 2.0F, by + 12.0F, 13.0F, C("e6d2ff"), 1, 0);
         this.d.rect((W - bww) / 2.0F - 2.0F, by + 15.0F, bww + 4.0F, 14.0F, BLACK, 0.45F);
         this.bar((W - bww) / 2.0F, by + 17.0F, bww, 10.0F, b.hp / b.max, b.inv ? C("a4d0ff") : C_BOSS);
      }

      float cxx = W / 2.0F;
      float cyy = H / 2.0F;
      Color rc2 = new Color(0.85F, 1.0F, 0.95F, 0.9F);

      for (int sx = -1; sx <= 1; sx += 2) {
         for (int sy = -1; sy <= 1; sy += 2) {
            float ox = cxx + sx * 14;
            float oy = cyy + sy * 14;
            this.d.line(ox, oy, ox - sx * 7, oy, 2.0F, rc2, rc2.a);
            this.d.line(ox, oy, ox, oy - sy * 7, 2.0F, rc2, rc2.a);
         }
      }

      this.d.line(cxx, cyy - 20.0F, cxx, cyy - 27.0F, 1.5F, WHITE, 0.55F);
      this.d.line(cxx, cyy + 20.0F, cxx, cyy + 27.0F, 1.5F, WHITE, 0.55F);
      this.d.line(cxx - 20.0F, cyy, cxx - 27.0F, cyy, 1.5F, WHITE, 0.55F);
      this.d.line(cxx + 20.0F, cyy, cxx + 27.0F, cyy, 1.5F, WHITE, 0.55F);
      this.d.circle(cxx, cyy, 2.0F, C_TEAL, 1.0F);
      if (G0.hitMark > 0.0F) {
         float k2 = G0.hitMark / 0.13F;
         Color hcm = new Color(1.0F, 0.35F, 0.25F, 1.0F);

         for (int sx = -1; sx <= 1; sx += 2) {
            for (int sy = -1; sy <= 1; sy += 2) {
               this.d.line(cxx + sx * 7, cyy + sy * 7, cxx + sx * (13.0F + 3.0F * k2), cyy + sy * (13.0F + 3.0F * k2), 2.5F, hcm, k2);
            }
         }
      }

      ArrayIterator var42 = G0.dmgNums.iterator();

      while (var42.hasNext()) {
         Game.DmgNum n = (Game.DmgNum)var42.next();
         if (this.proj(n.p, this.sp)) {
            float a = MathUtils.clamp(n.t / 0.35F, 0.0F, 1.0F);
            boolean mis = n.src.equals("mis");
            Color col = mis ? new Color(1.0F, 0.62F, 0.2F, a) : (n.src.equals("gun") ? new Color(1.0F, 0.95F, 0.6F, a) : new Color(0.75F, 0.9F, 1.0F, a));
            float fs = mis ? 22.0F : (n.v < 100.0F ? 16 : 19);
            String s = String.valueOf(Math.round(n.v));
            this.d.text(s, this.sp.x + 1.5F, this.sp.y + 1.5F, fs, new Color(0.0F, 0.0F, 0.0F, a * 0.6F), 1, 0);
            this.d.text(s, this.sp.x, this.sp.y, fs, col, 1, 0);
         }
      }

      if (G0.st("gun") > 0.0F || G0.st("beam") > 0.0F) {
         this.bar(cxx - 35.0F, cyy + 32.0F, 70.0F, 4.0F, G0.heat, G0.overheated ? C("ff3b30") : C("ffb05a"));
         if (G0.overheated) {
            this.d.text("ПЕРЕГРЕВ", cxx, cyy + 50.0F, 11.0F, C("ff6a5e"), 1, 0);
         }
      }

      List<Game.E> list = new ArrayList<>();
      ArrayIterator var46 = G0.enemies.iterator();

      while (var46.hasNext()) {
         Game.E e = (Game.E)var46.next();
         if (!this.hiddenE(e) && !e.type.equals("node")) {
            list.add(e);
         }
      }

      Vector3 cp = G0.camPos;
      Collections.sort(list, (ax, b) -> Float.compare(ax.pos.dst2(cp), b.pos.dst2(cp)));
      int arrows = 0;
      int marks = 0;
      PerspectiveCamera cam = G0.cam;
      this.right.set(cam.direction).crs(cam.up).nor();
      this.up.set(this.right).crs(cam.direction).nor();

      for (Game.E e : list) {
         float dist = e.pos.dst(cp);
         Color col = this.typeCol(e);
         boolean on = this.proj(G0.visPos(e, this.tv2), this.sp) && this.sp.x > 8.0F && this.sp.x < W - 8.0F && this.sp.y > 8.0F && this.sp.y < H - 8.0F;
         boolean shot = e.type.equals("erk") || e.type.equals("shell");
         if (on) {
            if (e.type.equals("scout")) {
               this.bar(this.sp.x - 22.0F, this.sp.y - 26.0F, 44.0F, 5.0F, e.data, C("ffe04a"));
            }

            if (e != G0.lock.t && !(dist < 40.0F) && marks < 28 && !shot) {
               marks++;
               float rr = e.boss ? 13.0F : 9.0F;
               if (e.ground) {
                  this.d.frame(this.sp.x - rr, this.sp.y - rr, rr * 2.0F, rr * 2.0F, 2.0F, col, 1.0F);
               } else {
                  this.d.ring(this.sp.x, this.sp.y, rr, !e.boss && !e.type.equals("heavy") ? 2.0F : 3.0F, col, 1.0F);
               }
            }
         } else if (arrows < 10 && (!shot || !(dist > 120.0F))) {
            arrows++;
            this.tv.set(e.pos).sub(cam.position);
            float lx = this.tv.dot(this.right);
            float ly = this.tv.dot(this.up);
            float ang = MathUtils.atan2(-ly, lx);
            float m = Math.max(Math.abs(MathUtils.cos(ang)) / 0.45F, Math.abs(MathUtils.sin(ang)) / 0.41F);
            float ax = W / 2.0F + MathUtils.cos(ang) / m * W / 2.0F * 0.9F;
            float ay = H / 2.0F + MathUtils.sin(ang) / m * H / 2.0F * 0.9F;
            float fx = MathUtils.cos(ang);
            float fy = MathUtils.sin(ang);
            float sxx = -fy;
            this.d
               .poly(
                  new float[]{ax + fx * 11.0F, ay + fy * 11.0F, ax - fx * 11.0F + sxx * 11.0F, ay - fy * 11.0F + fx * 11.0F, ax - fx * 5.0F, ay - fy * 5.0F},
                  col,
                  1.0F
               );
            this.d
               .poly(
                  new float[]{ax + fx * 11.0F, ay + fy * 11.0F, ax - fx * 5.0F, ay - fy * 5.0F, ax - fx * 11.0F - sxx * 11.0F, ay - fy * 11.0F - fx * 11.0F},
                  col,
                  1.0F
               );
         }
      }

      if (G0.lock.t != null && G0.state.equals("run")) {
         Game.E lt = G0.lock.t;
         if (this.proj(G0.visPos(lt, this.tv2), this.sp)) {
            float dist = lt.pos.dst(cp);
            float s = MathUtils.clamp(lt.r * 900.0F / dist, 26.0F, 110.0F) * (1.6F - G0.lock.k * 0.6F);
            boolean locked = G0.lock.k >= 1.0F && G0.st("maxMis") > 0.0F;
            Color col = locked ? C("ff3b30") : new Color(1.0F, 0.86F, 0.47F, 0.9F);
            this.d.frame(this.sp.x - s / 2.0F, this.sp.y - s / 2.0F, s, s, 2.0F, col, col.a);
            this.d.text(Math.round(dist) + " м", this.sp.x + s / 2.0F + 6.0F, this.sp.y - s / 2.0F + 10.0F, 11.0F, C("ffe08a"));
            if (locked) {
               this.d.text("ЗАХВАТ", this.sp.x, this.sp.y - s / 2.0F - 6.0F, 11.0F, C("ff6a5e"), 1, 0);
            }

            if (G0.st("gun") > 0.0F && this.proj(G0.leadPoint(lt, this.tv2), this.sp2)) {
               this.d
                  .polyline(
                     new float[]{this.sp2.x, this.sp2.y - 8.0F, this.sp2.x + 8.0F, this.sp2.y, this.sp2.x, this.sp2.y + 8.0F, this.sp2.x - 8.0F, this.sp2.y},
                     true,
                     2.0F,
                     C("7cf5ff"),
                     1.0F
                  );
            }
         }
      }

      float radarR = Math.min(70.0F, H * 0.125F);
      float rx = W - 12.0F - radarR;
      float ry = 12.0F + radarR;
      this.d.circle(rx, ry, radarR, new Color(0.02F, 0.06F, 0.06F, 1.0F), 0.72F);
      this.d.polyline(Draw.oct(rx, ry, radarR + 5.0F), true, 1.5F, C_TEAL, 0.5F);

      for (float k : new float[]{0.33F, 0.66F, 1.0F}) {
         this.d.ring(rx, ry, (radarR - 2.0F) * k, 1.0F, new Color(0.47F, 0.86F, 0.78F, 1.0F), 0.35F);
      }

      this.d
         .poly(
            new float[]{
               rx,
               ry,
               rx + MathUtils.cos(-2.0707965F) * radarR,
               ry + MathUtils.sin(-2.0707965F) * radarR,
               rx,
               ry - radarR,
               rx + MathUtils.cos(-1.0707964F) * radarR,
               ry + MathUtils.sin(-1.0707964F) * radarR
            },
            new Color(0.47F, 0.94F, 0.78F, 1.0F),
            0.12F
         );
      float sw = this.T * 2.4F % (float) (Math.PI * 2);
      this.d.line(rx, ry, rx + MathUtils.cos(sw) * radarR, ry + MathUtils.sin(sw) * radarR, 1.0F, new Color(0.47F, 0.94F, 0.78F, 1.0F), 0.6F);
      float cyw = MathUtils.cos(G0.aimYaw);
      float syw = MathUtils.sin(G0.aimYaw);
      float scl = (radarR - 4.0F) / 750.0F;
      ArrayIterator var75 = G0.enemies.iterator();

      while (var75.hasNext()) {
         Game.E ex = (Game.E)var75.next();
         if (ex.cloak == 0) {
            float vx = (ex.pos.x * cyw - ex.pos.z * syw) * scl;
            float vy = (ex.pos.x * syw + ex.pos.z * cyw) * scl;
            float l = (float)Math.sqrt(vx * vx + vy * vy);
            if (l > radarR - 4.0F) {
               vx = vx / l * (radarR - 4.0F);
               vy = vy / l * (radarR - 4.0F);
            }

            Color c = ex.boss ? C("c08bff") : (ex.ground ? C("ff9a3c") : (!ex.type.equals("wing") && !ex.type.equals("kami") ? C("ff5a4a") : C("ffd24a")));
            this.d.circle(rx + vx, ry + vy, ex.boss ? 5.0F : (ex.type.equals("erk") ? 1.5F : 3.0F), c, 1.0F);
         }
      }

      this.d.rect(rx - 2.0F, ry - 2.0F, 4.0F, 4.0F, C("7cf5c0"));
      float[] fc = this.fireC();
      boolean tor = G0.VEH.equals("tor");
      this.roundBtn(fc[0], fc[1], 59.0F, tor ? C("2a5bc4") : C("d03a28"), tor ? C("4a85e6") : C("f0604a"), G0.firing, new Color(1.0F, 1.0F, 1.0F, 0.14F));
      if (tor) {
         this.d.text("ПУСК", fc[0], fc[1] + 2.0F, 18.0F, WHITE, 1, 0);
         this.pips(fc[0], fc[1] + 14.0F, G0.missiles, (int)G0.st("maxMis"));
      } else {
         this.d.text(G0.VEH.equals("luch") ? "ЛУЧ" : "ОГОНЬ", fc[0], fc[1] + 7.0F, 19.0F, WHITE, 1, 0);
      }

      if (this.DESK) {
         this.d.text(tor ? "ЛКМ / ПКМ" : "ЛКМ / пробел", fc[0], fc[1] + (tor ? 40 : 30), 11.0F, new Color(1.0F, 1.0F, 1.0F, 0.75F), 0, 0);
      }

      if (this.vehMis()) {
         float[] mc = this.misC();
         boolean ready = G0.lock.t != null && G0.lock.k >= 1.0F && G0.missiles > 0;
         float dk = G0.missiles <= 0 ? 0.5F : 0.0F;
         this.roundBtn(mc[0], mc[1], 42.0F, darker(C("2a5bc4"), dk), darker(C("4a85e6"), dk), false, ready ? C("ff3b30") : new Color(1.0F, 1.0F, 1.0F, 0.14F));
         this.d.text("РАКЕТА", mc[0], mc[1] + 1.0F, 13.0F, WHITE, 1, 0);
         if (this.DESK) {
            this.d.text("ПКМ", mc[0], mc[1] - 56.0F, 12.0F, new Color(1.0F, 1.0F, 1.0F, 0.75F), 0, 0);
         }

         this.pips(mc[0], mc[1] + 10.0F, G0.missiles, (int)G0.st("maxMis"));
         if (G0.missiles < G0.st("maxMis") && G0.reloadT > 0.0F) {
            float k = 1.0F - G0.reloadT / G0.st("reload");
            this.d.arc(mc[0], mc[1], 48.5F, (float) (-Math.PI / 2), (float) (-Math.PI / 2) + (float) (Math.PI * 2) * k, 3.0F, C_TEAL, 0.8F);
         }
      }

      List<String> ab = this.abilList();

      for (int i = 0; i < ab.size(); i++) {
         String k = ab.get(i);
         float[] r = this.abRect(i);
         boolean used = !k.equals("salvo") && !k.equals("emp") ? G0.abCd.get(k) > 0.0F : G0.abUsed.get(k);
         this.d.rect(r[0], r[1], r[2], r[3], C_PANEL, used ? 0.5F : 0.9F);
         this.d.frame(r[0], r[1], r[2], r[3], 1.0F, C_LINE, 0.22F);
         String nm = G.D.get("ABIL").get(k).getString("name").split(" ")[0].toUpperCase();
         this.d.text((this.DESK ? i + 1 + " · " : "") + nm, r[0] + r[2] / 2.0F, r[1] + 23.0F, 12.0F, new Color(1.0F, 1.0F, 1.0F, used ? 0.4F : 1.0F), 1, 0);
         float kf = 1.0F;
         if (k.equals("smoke")) {
            kf = 1.0F - G0.abCd.get("smoke") / 30.0F;
         } else if (k.equals("repair")) {
            kf = 1.0F - G0.abCd.get("repair") / 45.0F;
         } else if (used) {
            kf = 0.0F;
         }

         this.d.rect(r[0] + 6.0F, r[1] + r[3] - 4.0F, (r[2] - 12.0F) * kf, 3.0F, C_ACC);
      }

      if (this.DESK && !this.host.bot() && G0.state.equals("run") && !Gdx.input.isCursorCatched()) {
         this.d.text("Кликните по экрану, чтобы управлять мышью", W / 2.0F, H * 0.7F, 16.0F, new Color(1.0F, 1.0F, 1.0F, 0.85F), 1, 0);
      }

      if (G0.hurtT > 0.0F) {
         this.d.rect(0.0F, 0.0F, W, H, new Color(0.86F, 0.08F, 0.04F, 1.0F), MathUtils.clamp(G0.hurtT * 0.5F, 0.0F, 0.3F));
      }

      if (hk < 0.3F && G0.hp > 0.0F) {
         float a = (0.3F - hk) * (0.6F + 0.4F * MathUtils.sin(this.T * 6.0F));
         float ex = W * 0.12F;
         this.d.gradH(0.0F, 0.0F, ex, H, C_RED, a, C_RED, 0.0F);
         this.d.gradH(W - ex, 0.0F, ex, H, C_RED, 0.0F, C_RED, a);
      }

      if (G0.empT > 0.0F) {
         this.d.rect(0.0F, 0.0F, W, H, new Color(0.35F, 0.66F, 1.0F, 1.0F), 0.08F + MathUtils.sin(this.T * 1000.0F / 120.0F) * 0.04F);
      }

      if (this.bannerT > 0.0F) {
         float ba = MathUtils.clamp(Math.min(this.bannerT / 0.4F, (3.0F - this.bannerT) / 0.3F), 0.0F, 1.0F);
         float y = H * 0.22F;
         Color bc = this.bannerBoss ? C("d8b4ff") : WHITE;
         this.d.text(this.bannerS, W / 2.0F + 2.0F, y + 38.0F + 2.0F, 34.0F, new Color(0.0F, 0.0F, 0.0F, 0.6F * ba), 1, 0);
         this.d.text(this.bannerS, W / 2.0F, y + 38.0F, 34.0F, new Color(bc.r, bc.g, bc.b, ba), 1, 0);
         if (!this.bannerSub.isEmpty()) {
            this.d.text(this.bannerSub, W / 2.0F + 1.5F, y + 48.0F + 20.0F + 1.5F, 16.0F, new Color(0.0F, 0.0F, 0.0F, 0.6F * ba), 0, 0);
            this.d.text(this.bannerSub, W / 2.0F, y + 48.0F + 20.0F, 16.0F, new Color(1.0F, 0.886F, 0.66F, ba), 0, 0);
         }
      }

      if (this.hintT > 0.0F && !this.hintS.isEmpty()) {
         float a = MathUtils.clamp(this.hintT / 0.3F, 0.0F, 1.0F);
         float hw = W * 0.6F;
         this.d.wrap(this.hintS, W * 0.2F + 1.5F, H - 175.0F + 1.5F, hw, 15.0F, new Color(0.0F, 0.0F, 0.0F, 0.6F * a), 0, 0);
         this.d.wrap(this.hintS, W * 0.2F, H - 175.0F, hw, 15.0F, new Color(1.0F, 1.0F, 1.0F, a), 0, 0);
      }

      if (this.commCur != null) {
         this.drawComm();
      }
   }

   String hudWaveText() {
      Game G0 = this.g;
      if (!G0.mode.equals("endless") && G0.CM != null) {
         JsonValue C = G0.CM;
         String nm = "chal".equals(C.getString("special", "")) ? C.getString("title") : "Миссия " + C.getString("id");
         return C.getString("type").equals("waves")
            ? String.format("%s · волна %d/%d", nm, Math.max(1, G0.wave), C.get("waves").size)
            : String.format("%s · %d с", nm, Math.max(0, (int)Math.ceil(G0.missionT)));
      } else {
         return "Волна " + Math.max(1, G0.wave);
      }
   }

   void drawComm() {
      JsonValue c = G.D.get("CH").get(this.commCur[0]);
      String name = c != null ? c.getString("name") : this.commCur[0];
      String ava = c != null ? c.getString("ava") : "?";
      Color col = c != null ? G.hex(c.getString("col")) : WHITE;
      float x = 72.0F;
      float y = 96.0F;
      float tw = 300.0F;
      boolean was = this.d.off;
      this.d.off = true;
      float th = this.d.wrap(this.commCur[1], 0.0F, 0.0F, tw, 14.0F, WHITE, 0, -1);
      this.d.off = was;
      float h = Math.max(56.0F, 22.0F + th + 8.0F);
      float w = 58.0F + tw + 12.0F;
      this.d.rect(x, y, w, h, new Color(0.04F, 0.07F, 0.12F, 1.0F), 0.88F);
      this.d.frame(x, y, w, h, 1.0F, C_LINE, 0.22F);
      this.d.rect(x, y, 3.0F, h, col, 1.0F);
      this.d.rect(x + 12.0F, y + 10.0F, 36.0F, 36.0F, col, 1.0F);
      this.d.text(ava, x + 30.0F, y + 35.0F, 18.0F, C("0b1220"), 1, 0);
      this.d.text(name, x + 58.0F, y + 20.0F, 12.0F, col, 2, -1);
      this.d.wrap(this.commCur[1], x + 58.0F, y + 24.0F, tw, 14.0F, WHITE, 0, -1);
   }

   static Color darker(Color c, float k) {
      return new Color(c.r * (1.0F - k), c.g * (1.0F - k), c.b * (1.0F - k), c.a);
   }

   boolean hiddenE(Game.E e) {
      if (e.cloak != 0) {
         return true;
      } else {
         return !e.stealth ? false : e.reveal <= 0.0F && e.pos.dst(this.g.camPos) > 170.0F;
      }
   }

   Color typeCol(Game.E e) {
      if (e.boss) {
         return C("c08bff");
      } else {
         String var2 = e.type;
         switch (var2) {
            case "scout":
               return C("ffe04a");
            case "relay":
               return C("3ddc97");
            case "wing":
            case "kami":
               return C("ffd24a");
            case "armor":
               return C("9fd0ff");
            case "stealth":
               return C("b48cff");
            default:
               return e.ground ? C("ff9a3c") : C("ff5a4a");
         }
      }
   }

   void roundBtn(float cx, float cy, float r, Color base, Color top, boolean pressed, Color ring) {
      float k = pressed ? 0.93F : 1.0F;
      this.d.poly(Draw.oct(cx, cy, (r + 6.0F) * k), BLACK, 0.35F);
      this.d.poly(Draw.oct(cx, cy, r * k), darker(base, 0.25F), 1.0F);
      this.d.poly(Draw.oct(cx, cy, r * 0.88F * k), top, 0.92F);
      this.d.polyline(Draw.oct(cx, cy, (r + 4.0F) * k), true, 2.0F, ring.a > 0.2F ? ring : WHITE, ring.a > 0.2F ? ring.a : 0.35F);
      float[] hl = Draw.oct(cx, cy, r * 0.74F * k);
      this.d.polyline(new float[]{hl[8], hl[9], hl[10], hl[11], hl[12], hl[13]}, false, 2.0F, WHITE, 0.3F);
   }

   void pips(float cx, float cy, int n, int mx) {
      int cnt = Math.min(mx, 12);
      float w = cnt * 9;

      for (int i = 0; i < cnt; i++) {
         this.d.rect(cx - w / 2.0F + i * 9, cy, 6.0F, 11.0F, WHITE, i < n ? 1.0F : 0.25F);
      }
   }

   void deskMouse() {
      if (this.DESK && !this.host.bot()) {
         boolean want = this.inBattle() && this.g.state.equals("run") && !this.mouseFree;
         if (this.strike != null && this.strike.active) {
            want = this.strikeInput() && !this.mouseFree;
         }

         if (want != Gdx.input.isCursorCatched()) {
            Gdx.input.setCursorCatched(want);
         }
      }
   }

   void keyAim(float dt) {
      if (this.strike != null && this.strike.active) {
         if (this.strikeInput() && this.strike.st.equals("fly")) {
            this.strikeKeys();
         }
      } else if (this.inBattle()) {
         if (this.bossRole()) {
            this.bossKeys();
         } else {
            float kk = dt * 1.6F * G.SV.sens;
            if (Gdx.input.isKeyPressed(21) || Gdx.input.isKeyPressed(29)) {
               this.g.aimYaw += kk;
            }

            if (Gdx.input.isKeyPressed(22) || Gdx.input.isKeyPressed(32)) {
               this.g.aimYaw -= kk;
            }

            if (Gdx.input.isKeyPressed(19) || Gdx.input.isKeyPressed(51)) {
               this.g.aimPitch = MathUtils.clamp(this.g.aimPitch + kk, -0.06F, 1.2F);
            }

            if (Gdx.input.isKeyPressed(20) || Gdx.input.isKeyPressed(47)) {
               this.g.aimPitch = MathUtils.clamp(this.g.aimPitch - kk, -0.06F, 1.2F);
            }
         }
      }
   }

   boolean deskHudClick(float x, float y) {
      if (in(x, y, PAUSE_R[0], PAUSE_R[1], PAUSE_R[2], PAUSE_R[3])) {
         this.pause();
         return true;
      } else if (in(x, y, SHOP_R[0], SHOP_R[1], SHOP_R[2], SHOP_R[3])) {
         this.openShopBattle();
         return true;
      } else {
         List<String> ab = this.abilList();

         for (int i = 0; i < ab.size(); i++) {
            float[] r = this.abRect(i);
            if (in(x, y, r[0], r[1], r[2], r[3])) {
               this.g.useAbility(ab.get(i));
               return true;
            }
         }

         return false;
      }
   }

   void aimDelta(float dx, float dy, float k) {
      this.g.aimYaw -= dx * k;
      this.g.aimPitch = MathUtils.clamp(this.g.aimPitch - dy * k, -0.06F, 1.2F);
   }

   public boolean touchDown(int sx, int sy, int ptr, int button) {
      float x = sx / this.d.s;
      float y = sy / this.d.s;
      if (this.pending != null) {
         return true;
      } else if (this.strikeInput()) {
         return this.strikeTouchDown(x, y, ptr, button);
      } else if (this.inBattle() && this.bossRole()) {
         return this.bossTouchDown(x, y, ptr, button);
      } else if (this.inBattle()) {
         if (this.DESK) {
            if (!Gdx.input.isCursorCatched()) {
               if (button == 0 && !this.deskHudClick(x, y)) {
                  this.mouseFree = false;
               }
            } else if (button == 0) {
               this.g.firing = true;
            } else if (button == 1) {
               if (this.g.VEH.equals("tor")) {
                  this.g.firing = true;
               } else {
                  this.g.fireMissile("lock", false);
               }
            }

            return true;
         } else {
            String what = "aim";
            float[] fc = this.fireC();
            float[] mc = this.misC();
            if (Vector2.dst(x, y, fc[0], fc[1]) < 64.0F) {
               what = "fire";
               this.g.firing = true;
            } else if (this.vehMis() && Vector2.dst(x, y, mc[0], mc[1]) < 46.0F) {
               what = "mis";
               this.g.fireMissile("lock", false);
            } else if (in(x, y, PAUSE_R[0], PAUSE_R[1], PAUSE_R[2], PAUSE_R[3])) {
               what = "ui";
               this.pause();
            } else if (in(x, y, SHOP_R[0], SHOP_R[1], SHOP_R[2], SHOP_R[3])) {
               what = "ui";
               this.openShopBattle();
            } else {
               List<String> ab = this.abilList();

               for (int i = 0; i < ab.size(); i++) {
                  float[] r = this.abRect(i);
                  if (in(x, y, r[0], r[1], r[2], r[3])) {
                     what = "ab";
                     this.g.useAbility(ab.get(i));
                  }
               }
            }

            this.touches.put(ptr, what);
            this.lastTouch.put(ptr, new Vector2(x, y));
            return true;
         }
      } else if (this.down && ptr != this.downPtr) {
         return true;
      } else {
         this.down = true;
         this.downPtr = ptr;
         this.dragged = false;
         this.capture = false;
         this.downX = this.curX = x;
         this.downY = this.curY = y;
         this.lastY = y;
         this.mx = x;
         this.my = y;
         return true;
      }
   }

   public boolean touchDragged(int sx, int sy, int ptr) {
      float x = sx / this.d.s;
      float y = sy / this.d.s;
      if (this.strikeInput() || this.inBattle() && this.bossRole()) {
         if (this.DESK) {
            if (Gdx.input.isCursorCatched()) {
               this.mouseMoved(sx, sy);
            }

            return true;
         } else {
            return this.strikeInput() ? this.strikeDrag(x, y, ptr) : this.bossDrag(x, y, ptr);
         }
      } else if (this.inBattle()) {
         if (this.DESK) {
            if (Gdx.input.isCursorCatched()) {
               this.mouseMoved(sx, sy);
            }

            return true;
         } else {
            Vector2 lt = this.lastTouch.get(ptr);
            if ("aim".equals(this.touches.get(ptr)) && lt != null) {
               this.aimDelta(x - lt.x, y - lt.y, 0.0042F * G.SV.sens);
            }

            if (lt != null) {
               lt.set(x, y);
            }

            return true;
         }
      } else if (ptr == this.downPtr && this.down) {
         this.curX = x;
         this.curY = y;
         this.mx = x;
         this.my = y;
         if (!this.dragged && Vector2.dst(x, y, this.downX, this.downY) > 9.0F) {
            this.dragged = true;
         }

         if (this.dragged && !this.capture) {
            this.scroll = this.scroll - (y - this.lastY);
         }

         this.lastY = y;
         return true;
      } else {
         return true;
      }
   }

   public boolean touchUp(int sx, int sy, int ptr, int button) {
      float x = sx / this.d.s;
      float y = sy / this.d.s;
      if (this.strike != null && this.strike.active && (this.touches.containsKey(ptr) || this.DESK) && this.panel.isEmpty()) {
         return this.strikeUp(ptr, button);
      } else {
         if (this.bossRole() && (this.touches.containsKey(ptr) || this.DESK && this.inBattle())) {
            this.bossUp(ptr);
            if (this.inBattle()) {
               return true;
            }
         }

         if (this.inBattle() || this.touches.containsKey(ptr)) {
            if (this.DESK) {
               if (button == 0) {
                  this.g.firing = false;
               }

               if (button == 1 && this.g.VEH.equals("tor")) {
                  this.g.firing = false;
               }
            }

            if ("fire".equals(this.touches.get(ptr))) {
               this.g.firing = false;
            }

            this.touches.remove(ptr);
            this.lastTouch.remove(ptr);
            if (this.inBattle()) {
               return true;
            }
         }

         if (ptr != this.downPtr) {
            return true;
         } else {
            if (this.down && !this.dragged && !this.capture) {
               this.tapReady = true;
               this.tapX = x;
               this.tapY = y;
            }

            this.down = false;
            this.downPtr = -1;
            if (this.capture) {
               this.capture = false;
               G.save();
            }

            return true;
         }
      }
   }

   public boolean touchCancelled(int sx, int sy, int ptr, int button) {
      this.touches.remove(ptr);
      this.lastTouch.remove(ptr);
      if (ptr == this.downPtr) {
         this.down = false;
         this.downPtr = -1;
      }

      this.g.firing = false;
      return true;
   }

   public boolean mouseMoved(int sx, int sy) {
      if (this.DESK && Gdx.input.isCursorCatched() && this.strikeInput()) {
         float k = 0.0022F * G.SV.sens;
         this.strike.mdX = this.strike.mdX + Gdx.input.getDeltaX() / this.d.s * k;
         this.strike.mdY = this.strike.mdY - Gdx.input.getDeltaY() / this.d.s * k;
         return true;
      } else if (this.DESK && Gdx.input.isCursorCatched() && this.inBattle() && this.bossRole()) {
         float k = 0.0026F * G.SV.sens;
         this.g.bossIn.cy = this.g.bossIn.cy - Gdx.input.getDeltaX() / this.d.s * k;
         this.g.bossCamPitch = MathUtils.clamp(this.g.bossCamPitch + Gdx.input.getDeltaY() / this.d.s * k, -0.35F, 0.9F);
         return true;
      } else if (this.inBattle() && this.DESK && Gdx.input.isCursorCatched()) {
         this.aimDelta(Gdx.input.getDeltaX() / this.d.s, Gdx.input.getDeltaY() / this.d.s, 0.0026F * G.SV.sens);
         return true;
      } else {
         this.mx = sx / this.d.s;
         this.my = sy / this.d.s;
         return false;
      }
   }

   public boolean scrolled(float ax, float ay) {
      if (!this.panel.isEmpty()) {
         this.scroll += ay * 48.0F;
      }

      return true;
   }

   public boolean keyDown(int kc) {
      if (kc == 4) {
         this.androidBack();
         return true;
      } else if (kc == 141 && this.DESK) {
         this.host.toggleFullscreen();
         return true;
      } else {
         if (this.strike != null && this.strike.active) {
            if (this.panel.equals("spause") && (kc == 111 || kc == 44)) {
               this.strike.resume();
               this.show("");
               return true;
            }

            if (this.strikeInput()) {
               return this.strikeKey(kc);
            }
         }

         if (this.inBattle() && this.bossRole()) {
            return this.bossKey(kc);
         } else if (this.inBattle()) {
            if (kc == 62) {
               this.g.firing = true;
            } else if (kc == 33 || kc == 46) {
               this.g.fireMissile("lock", false);
            } else if (kc == 111 || kc == 44) {
               this.pause();
            } else if (kc == 30) {
               this.openShopBattle();
            } else if (kc == 61) {
               this.mouseFree = !this.mouseFree;
            } else {
               if (kc < 8 || kc > 11) {
                  return false;
               }

               List<String> ab = this.abilList();
               int i = kc - 8;
               if (i < ab.size()) {
                  this.g.useAbility(ab.get(i));
               }
            }

            return true;
         } else if (kc != 111) {
            return false;
         } else {
            if (this.panel.equals("pause")) {
               this.resume();
            } else {
               if (this.panel.equals("menu") || this.panel.equals("daily")) {
                  return false;
               }

               if (this.panel.equals("over") || this.panel.equals("result")) {
                  this.toMenu();
               } else if (this.panel.equals("cut")) {
                  this.show("campaign");
               } else {
                  this.back();
               }
            }

            return true;
         }
      }
   }

   public boolean keyUp(int kc) {
      if (kc == 62) {
         this.g.firing = false;
      }

      return false;
   }

   public boolean keyTyped(char c) {
      return false;
   }

   public void onPause() {
      this.g.firing = false;
      this.mouseFree = true;
      if (this.strike != null && this.strike.active) {
         this.strikePause();
      } else {
         if (this.g.state.equals("run") && this.panel.isEmpty()) {
            this.pause();
         }
      }
   }

   @Override
   public void strikeOver() {
      this.show("sover");
   }

   void pSwarm() {
      this.frame(
         "swarm",
         920.0F,
         false,
         true,
         () -> {
            this.title("Рой: дрон-камикадзе");
            this.label(
               "Учения «Обратная сторона»: ты — красная команда. Веди дрон через зону ПВО и уничтожь все цели рубежа. Лети низко — радары видят хуже, прячься за холмами, сбивай ракеты ловушками. На старте 8 дронов, за каждый прорванный рубеж — ещё 4–7. Кончились дроны — конец забега.",
               13.0F,
               C_MUT,
               0,
               0
            );
            this.d
               .text(
                  String.format("Рекорд: %d очков · лучший рубеж: %d · монеты: %d", G.SV.swBest, G.SV.swBestz, G.SV.c),
                  this.cx + this.cw / 2.0F,
                  this.cy + 14.0F,
                  14.0F,
                  C_ACC,
                  2,
                  0
               );
            this.cy += 24.0F;
            String pick = Strike.DRONES.containsKey(G.SV.swPick) ? G.SV.swPick : "osa";
            if (this.btn("Взлёт · " + Strike.DRONES.get(pick).name, 52.0F, 1, true)) {
               this.pending = () -> {
                  this.show("");
                  if (this.strikeHost != null) {
                     this.strikeHost.startStrike();
                  }
               };
               this.pendingFrames = 1;
            }

            this.section("Дроны");
            List<UI.Card> cs = new ArrayList<>();

            for (Strike.DroneDef dd : Strike.DRONES.values()) {
               boolean own = G.SV.swOwn.contains(dd.key);
               boolean sel = pick.equals(dd.key);
               UI.Card c = this.card(
                  dd.name, dd.desc, own ? (sel ? "Выбран" : "Выбрать") : "Купить · " + dd.price, own && !sel || !own && G.SV.c >= dd.price, () -> {
                     if (!G.SV.swOwn.contains(dd.key)) {
                        G.SV.c = G.SV.c - dd.price;
                        G.SV.swOwn.add(dd.key);
                        G.sfx("coin");
                     }

                     G.SV.swPick = dd.key;
                     G.save();
                  }
               );
               c.sel = sel;
               c.sub = String.format(
                  "Скорость %d · прочность %d · взрыв %d%s", (int)(dd.sp * 3.6F), (int)dd.hp, (int)dd.dmg, dd.bombs > 0 ? " · бомбы ×" + dd.bombs : ""
               );
               c.subc = C_TEAL;
               cs.add(c);
            }

            this.cards(cs, 4);
            this.section("Улучшения (для всех дронов)");
            cs.clear();

            for (String[] u : Strike.UPS) {
               int l = Strike.up(u[0]);
               boolean mx = l >= 5;
               int cost = mx ? 0 : Strike.UP_COST[l];
               UI.Card c = this.card(u[1], u[2], mx ? "Максимум" : "Улучшить · " + cost, !mx && G.SV.c >= cost, () -> {
                  G.SV.c -= cost;
                  G.SV.swUp.put(u[0], l + 1);
                  G.sfx("coin");
                  G.save();
               });
               c.lv = l;
               c.max = 5;
               cs.add(c);
            }

            this.cards(cs, 4);
            int r = this.btnRow(
               new String[]{"Камера: " + (G.SV.swCam == 0 ? "FPV" : "3-е лицо"), "Инверсия высоты: " + (G.SV.swInv ? "вкл" : "выкл")},
               new int[]{0, 0},
               null,
               40.0F
            );
            if (r == 0) {
               G.SV.swCam = 1 - G.SV.swCam;
               G.save();
            } else if (r == 1) {
               G.SV.swInv = !G.SV.swInv;
               G.save();
            }

            this.label(
               this.DESK
                  ? "ПК: мышь — курс, ЛКМ или Shift — ускорение, Ctrl — тормоз, ПКМ или пробел — ловушки, E — сброс бомбы, X — подрыв, C — камера, Esc — пауза."
                  : "Телефон: левый джойстик — руль, свайп справа — точное наведение, «Ускор» удерживать.",
               12.0F,
               C_MUT
            );
         }
      );
   }

   void pSPause() {
      Strike S = this.strike;
      this.frame("spause", 400.0F, false, true, () -> {
         this.title("Пауза");
         if (this.btn("Продолжить", 50.0F, 1, true)) {
            S.resume();
            this.show("");
         }

         if (this.btn("Камера: " + (S.camMode == 0 ? "FPV" : "3-е лицо"), 42.0F, 0, true)) {
            S.toggleCam();
         }

         if (this.btn("Завершить забег", 42.0F, 0, true)) {
            this.show("");
            S.endRun();
         }

         if (this.btn("Выйти в меню", 42.0F, 0, true) && this.strikeHost != null) {
            this.strikeHost.exitStrike(false);
         }
      });
   }

   void pSOver() {
      Strike S = this.strike;
      this.frame(
         "sover",
         500.0F,
         false,
         true,
         () -> {
            this.title("Рой рассеян");
            if (S.resRec) {
               this.d.text("НОВЫЙ РЕКОРД!", this.cx + this.cw / 2.0F, this.cy + 18.0F, 20.0F, C_ACC, 3, 0);
               this.cy += 30.0F;
            }

            String[][] cells = new String[][]{
               {"Очки", String.valueOf(S.score)},
               {"Рубеж", String.valueOf(S.zone)},
               {"Целей", String.valueOf(S.tgKills)},
               {"Машин ПВО", String.valueOf(S.aaKills)}
            };
            float gp = 6.0F;
            float w = (this.cw - gp * 3.0F) / 4.0F;

            for (int i = 0; i < 4; i++) {
               float x = this.cx + i * (w + gp);
               if (!this.d.off) {
                  this.box(x, this.cy, w, 58.0F, WHITE, 0.05F, C_LINE, 0.22F, 1.0F);
                  this.d.text(cells[i][0], x + w / 2.0F, this.cy + 18.0F, 12.0F, C_MUT, 0, 0);
                  this.d.text(cells[i][1], x + w / 2.0F, this.cy + 46.0F, 22.0F, C_TXT, 1, 0);
               }
            }

            this.cy += 66.0F;
            this.label(
               String.format("Вылетов: %d · монеты +%d · опыт +%d%s", S.sorties, S.resCoins, S.resXp, S.resRankUp ? " · новое звание!" : ""),
               14.0F,
               C_ACC,
               2,
               0
            );
            this.d.text(this.rankText(), this.cx, this.cy + 13.0F, 13.0F, C_RANK, 2, -1);
            this.cy += 20.0F;
            this.bar(this.cx, this.cy, this.cw, 6.0F, this.rankK(), C_RANK);
            this.cy += 14.0F;
            int r = this.btnRow(new String[]{"Ещё раз", "Ангар", "Меню"}, new int[]{1, 0, 0}, null, 48.0F);
            if (r == 0) {
               this.pending = () -> {
                  this.show("");
                  if (this.strikeHost != null) {
                     this.strikeHost.startStrike();
                  }
               };
               this.pendingFrames = 1;
            } else if (r == 1) {
               if (this.strikeHost != null) {
                  this.strikeHost.exitStrike(true);
               }
            } else if (r == 2 && this.strikeHost != null) {
               this.strikeHost.exitStrike(false);
            }
         }
      );
   }

   void osd(String t, float x, float y, float size, Color col, int align, boolean title) {
      int f = title ? 1 : 0;
      Color sh = new Color(0.0F, 0.0F, 0.0F, 0.65F * col.a);
      this.d.text(t, x + 1.2F, y + 1.2F, size, sh, f, align);
      this.d.text(t, x - 0.8F, y - 0.8F, size, sh, f, align);
      this.d.text(t, x, y, size, col, f, align);
   }

   float[] sBoost() {
      return new float[]{this.sx() - 84.0F, this.sy() - 84.0F};
   }

   float[] sFlare() {
      return new float[]{this.sx() - 200.0F, this.sy() - 62.0F};
   }

   float[] sDet() {
      return new float[]{this.sx() - 84.0F, this.sy() - 212.0F};
   }

   float[] sBrake() {
      return new float[]{this.sx() - 196.0F, this.sy() - 160.0F};
   }

   float[] sBomb() {
      return new float[]{this.sx() - 300.0F, this.sy() - 62.0F};
   }

   float[] sCam() {
      return new float[]{this.sx() - 222.0F, 14.0F, 70.0F, 40.0F};
   }

   void sBtn(float[] c, float r, String label, Color col, boolean on, String sub) {
      this.d.poly(Draw.oct(c[0], c[1], r), col, on ? 0.55F : 0.18F);
      this.d.polyline(Draw.oct(c[0], c[1], r), true, 2.0F, WHITE, on ? 0.6F : 0.2F);
      this.osd(label, c[0], c[1] + (sub.isEmpty() ? 5 : -1), r > 50.0F ? 14.0F : 12.0F, new Color(1.0F, 1.0F, 1.0F, on ? 1.0F : 0.45F), 0, true);
      if (!sub.isEmpty()) {
         this.osd(sub, c[0], c[1] + 15.0F, 12.0F, new Color(1.0F, 1.0F, 1.0F, on ? 0.8F : 0.4F), 0, true);
      }
   }

   void edgeArrow(Vector3 p, Color col, String label) {
      PerspectiveCamera cam = this.g.cam;
      float W = this.sx();
      float H = this.sy();
      this.tv.set(p).sub(cam.position);
      this.right.set(cam.direction).crs(cam.up).nor();
      float a = MathUtils.atan2(this.tv.dot(this.right), this.tv.dot(cam.direction));
      float r = Math.min(W, H) * 0.4F;
      float fx = MathUtils.sin(a);
      float fy = -MathUtils.cos(a);
      float sx2 = -fy;
      float ax = W / 2.0F + fx * r;
      float ay = H / 2.0F + fy * r;
      this.d
         .poly(
            new float[]{ax + fx * 14.0F, ay + fy * 14.0F, ax - fx * 9.0F + sx2 * 10.0F, ay - fy * 9.0F + fx * 10.0F, ax - fx * 3.0F, ay - fy * 3.0F}, col, 1.0F
         );
      this.d
         .poly(
            new float[]{ax + fx * 14.0F, ay + fy * 14.0F, ax - fx * 3.0F, ay - fy * 3.0F, ax - fx * 9.0F - sx2 * 10.0F, ay - fy * 9.0F - fx * 10.0F}, col, 1.0F
         );
      this.osd(label, ax - fx * 26.0F, ay - fy * 26.0F + 4.0F, 11.0F, col, 0, true);
   }

   boolean onScreen(Vector2 v) {
      return v.x >= 0.0F && v.x <= this.sx() && v.y >= 0.0F && v.y <= this.sy();
   }

   void drawStrikeHud() {
      Strike S = this.strike;
      Strike.DroneState D = S.D;
      float W = this.sx();
      float H = this.sy();
      Color AMB = C_ACC;
      Color TEAL = C_TEAL;
      boolean fpv = S.camMode == 0 && S.st.equals("fly");
      Strike.Obj near = null;
      float nd = 1.0E9F;
      ArrayIterator cxx = S.targets.iterator();

      while (cxx.hasNext()) {
         Strike.Obj t = (Strike.Obj)cxx.next();
         if (t.alive && D.alive && t.pos.dst(D.pos) < nd) {
            nd = t.pos.dst(D.pos);
            near = t;
         }
      }

      cxx = S.targets.iterator();

      while (cxx.hasNext()) {
         Strike.Obj t = (Strike.Obj)cxx.next();
         if (t.alive) {
            this.tv2.set(t.pos).add(0.0F, t.hr + 4.0F, 0.0F);
            float dist = D.alive ? t.pos.dst(D.pos) : 0.0F;
            if (this.proj(this.tv2, this.sp) && this.onScreen(this.sp)) {
               float r = 11.0F;
               this.d
                  .polyline(
                     new float[]{this.sp.x, this.sp.y - r, this.sp.x + r, this.sp.y, this.sp.x, this.sp.y + r, this.sp.x - r, this.sp.y}, true, 2.0F, AMB, 1.0F
                  );
               String nm = Strike.tgName(t.kind);
               if (t == near) {
                  this.osd(nm + " · " + (int)dist + " м", this.sp.x, this.sp.y - 16.0F, 13.0F, AMB, 0, true);
               } else if (!D.alive || dist < 200.0F) {
                  this.osd(nm, this.sp.x, this.sp.y - 16.0F, 11.0F, new Color(AMB.r, AMB.g, AMB.b, 0.8F), 0, true);
               }

               float k = t.hp / Math.max(1.0F, t.max);
               if (k < 1.0F) {
                  this.d.rect(this.sp.x - 20.0F, this.sp.y + 15.0F, 40.0F, 4.0F, BLACK, 0.5F);
                  this.d.rect(this.sp.x - 20.0F, this.sp.y + 15.0F, 40.0F * k, 4.0F, AMB, 1.0F);
               }
            }
         }
      }

      cxx = S.units.iterator();

      while (cxx.hasNext()) {
         Strike.Obj u = (Strike.Obj)cxx.next();
         if (u.alive && u.known && this.proj(this.tv2.set(u.pos).add(0.0F, 6.5F, 0.0F), this.sp) && this.onScreen(this.sp)) {
            Color col = u.track > 0.3F ? C_SRED : new Color(1.0F, 0.55F, 0.45F, 0.75F);
            this.d
               .polyline(
                  new float[]{this.sp.x, this.sp.y - 9.0F, this.sp.x + 9.0F, this.sp.y + 7.0F, this.sp.x - 9.0F, this.sp.y + 7.0F}, true, 2.0F, col, col.a
               );
            if (D.alive && (u.track > 0.3F || u.pos.dst(D.pos) < 200.0F)) {
               this.osd(G.vehName(u.kind), this.sp.x, this.sp.y - 13.0F, 11.0F, col, 0, true);
            }
         }
      }

      cxx = S.hunters.iterator();

      while (cxx.hasNext()) {
         Strike.Hunter h = (Strike.Hunter)cxx.next();
         if (h.alive && this.proj(h.pos, this.sp) && this.onScreen(this.sp)) {
            this.d.ring(this.sp.x, this.sp.y, 8.0F, 2.0F, h.chase ? C_SRED : new Color(1.0F, 0.6F, 0.5F, 1.0F), h.chase ? 1.0F : 0.6F);
         }
      }

      if (S.st.equals("fly") && D.alive) {
         cxx = S.missilePositions(this.tmpArr).iterator();

         while (cxx.hasNext()) {
            Vector3 m = (Vector3)cxx.next();
            if (this.proj(m, this.sp) && this.onScreen(this.sp)) {
               this.d.ring(this.sp.x, this.sp.y, 12.0F, 2.5F, C_SRED, 1.0F);
               this.d.line(this.sp.x - 16.0F, this.sp.y, this.sp.x + 16.0F, this.sp.y, 2.0F, C_SRED, 1.0F);
            } else {
               this.edgeArrow(m, C_SRED, "РАКЕТА " + (int)m.dst(D.pos) + " м");
            }
         }

         if (near != null && (!this.proj(this.tv2.set(near.pos).add(0.0F, near.hr, 0.0F), this.sp) || !this.onScreen(this.sp))) {
            this.edgeArrow(near.pos, AMB, Strike.tgName(near.kind) + " " + (int)nd + " м");
         }
      }

      float cxxx = W / 2.0F;
      float cyy = H / 2.0F;
      if (S.st.equals("fly") && D.alive) {
         if (fpv) {
            float hy = cyy + D.pitch * H * 0.55F;
            float rl = D.roll;
            float dx = MathUtils.cos(rl);
            float dy = MathUtils.sin(rl);
            this.d.line(cxxx - dx * 160.0F, hy - dy * 160.0F, cxxx - dx * 60.0F, hy - dy * 60.0F, 2.0F, WHITE, 0.55F);
            this.d.line(cxxx + dx * 60.0F, hy + dy * 60.0F, cxxx + dx * 160.0F, hy + dy * 160.0F, 2.0F, WHITE, 0.55F);
            this.d.line(cxxx - 14.0F, cyy, cxxx - 5.0F, cyy, 2.0F, C_OSD, 1.0F);
            this.d.line(cxxx + 5.0F, cyy, cxxx + 14.0F, cyy, 2.0F, C_OSD, 1.0F);
            this.d.line(cxxx, cyy + 5.0F, cxxx, cyy + 12.0F, 2.0F, C_OSD, 1.0F);
            this.d.circle(cxxx, cyy, 2.0F, C_OSD, 1.0F);
            this.osd("REC", W - 330.0F, 34.0F, 13.0F, C_SRED, -1, true);
            this.d.circle(W - 330.0F + 36.0F, 29.0F, 4.0F, C_SRED, MathUtils.sin(this.T * 6.0F) > 0.0F ? 1.0F : 0.3F);
         } else if (this.proj(S.fwd(this.tv2).scl(60.0F).add(D.pos), this.sp)) {
            this.d.ring(this.sp.x, this.sp.y, 9.0F, 2.0F, C_OSD, 1.0F);
            this.d.circle(this.sp.x, this.sp.y, 1.5F, C_OSD, 1.0F);
         }

         float agl = D.pos.y - this.g.world.height(D.pos.x, D.pos.z);
         boolean low = agl < 14.0F;
         float bk = D.bat / Math.max(1.0F, D.bmax);
         float y0 = H - 26.0F;
         this.osd("ВЫС " + (int)agl + " м", cxxx - 150.0F, y0, 15.0F, low ? TEAL : C_OSD, 0, true);
         this.osd((int)(D.sp * 3.6F) + " км/ч", cxxx, y0, 15.0F, C_OSD, 0, true);
         this.osd("АКБ " + (int)(bk * 100.0F) + "%", cxxx + 150.0F, y0, 15.0F, bk < 0.25F ? C_SRED : C_OSD, 0, true);
         float bw = 220.0F;
         this.d.rect(cxxx - bw / 2.0F, y0 - 34.0F, bw, 6.0F, BLACK, 0.45F);
         this.d.rect(cxxx - bw / 2.0F, y0 - 34.0F, bw * D.hp / Math.max(1.0F, D.max), 6.0F, D.hp > D.max * 0.35F ? TEAL : C_SRED, 1.0F);
         this.osd("КОРПУС", cxxx - bw / 2.0F - 8.0F, y0 - 27.0F, 11.0F, C_OSD, 1, true);
         if (low) {
            this.osd("НИЗКО: РЛС ВИДЯТ ХУЖЕ", cxxx, y0 - 46.0F, 12.0F, TEAL, 0, true);
         }

         float sig = 1.0F - MathUtils.clamp(D.pos.dst(S.launchP) / 1600.0F + S.jam * 0.7F, 0.0F, 0.9F);

         for (int i = 0; i < 5; i++) {
            this.d.rect(W - 340.0F + i * 7, 54 - i * 3, 5.0F, 6 + i * 3, i / 5.0F < sig ? C_OSD : WHITE, i / 5.0F < sig ? 1.0F : 0.2F);
         }

         float wy = 120.0F;
         int nmis = S.missilesAtMe();
         if (S.lockWarn > 0.0F && this.T % 0.4F < 0.25F) {
            this.osd("ОБЛУЧЕНИЕ РАДАРОМ", cxxx, wy, 18.0F, C_SRED, 0, true);
            wy += 24.0F;
         } else if (S.alert > 0.0F) {
            this.osd("ОБНАРУЖЕН", cxxx, wy, 17.0F, AMB, 0, true);
            wy += 24.0F;
         }

         if (nmis > 0 && this.T % 0.3F < 0.18F) {
            this.osd("РАКЕТА! ×" + nmis, cxxx, wy, 20.0F, C_SRED, 0, true);
            wy += 26.0F;
         }

         if (S.laserWarn > 0.0F) {
            this.osd("ЛАЗЕР ЖЖЁТ КОРПУС", cxxx, wy, 17.0F, C_SRED, 0, true);
            wy += 24.0F;
         }

         if (S.jam > 0.15F) {
            this.osd("ПОМЕХИ РЭБ", cxxx, wy, 16.0F, C_RANK, 0, true);
            wy += 22.0F;
         }

         if (D.fall) {
            this.osd("АКБ РАЗРЯЖЕНА — ПАДАЕМ", cxxx, wy, 18.0F, C_SRED, 0, true);
         }

         if (nmis > 0 && this.T % 0.5F < 0.02F) {
            G.sfx("lock", 0.35F, 200);
         }
      }

      this.d.rect(PAUSE_R[0], PAUSE_R[1], PAUSE_R[2], PAUSE_R[3], BLACK, 0.45F);
      this.d.rect(29.0F, 27.0F, 5.0F, 18.0F, WHITE);
      this.d.rect(39.0F, 27.0F, 5.0F, 18.0F, WHITE);
      this.osd("РУБЕЖ " + S.zone, 74.0F, 30.0F, 17.0F, AMB, -1, true);
      this.osd(String.valueOf(S.score), 74.0F, 52.0F, 18.0F, C_OSD, -1, true);
      this.osd(String.format("Целей: %d · дронов в запасе: %d", S.targetsLeft(), S.stock), 74.0F, 72.0F, 12.0F, C_OSD, -1, false);

      for (int i = 0; i < Math.min(S.stock, 20); i++) {
         this.d.rect(74 + i * 12, 80.0F, 8.0F, 8.0F, TEAL, 1.0F);
      }

      if (S.chain >= 2 && S.chainT > 0.0F) {
         this.osd("Серия ×" + S.chain, 74.0F, 108.0F, 15.0F, TEAL, -1, true);
      }

      float[] cr = this.sCam();
      this.d.rect(cr[0], cr[1], cr[2], cr[3], BLACK, 0.45F);
      this.d.frame(cr[0], cr[1], cr[2], cr[3], 1.5F, WHITE, 0.5F);
      this.osd(S.camMode == 0 ? "FPV" : "3-Е ЛИЦО", cr[0] + cr[2] / 2.0F, cr[1] + cr[3] / 2.0F + 5.0F, 12.0F, C_OSD, 0, true);
      this.minimap(W - 80.0F, 82.0F, 64.0F);
      float fy = 166.0F;

      for (ArrayIterator var40 = S.feed.iterator(); var40.hasNext(); fy += 18.0F) {
         Strike.Feed f = (Strike.Feed)var40.next();
         float a = MathUtils.clamp(f.life, 0.0F, 1.0F);
         this.osd(f.t, W - 14.0F, fy, 13.0F, new Color(f.c.r, f.c.g, f.c.b, a), 1, false);
      }

      if (S.bigT > 0.0F) {
         float a = MathUtils.clamp(Math.min(S.bigT / 0.4F, (3.5F - S.bigT) / 0.3F), 0.0F, 1.0F);
         this.osd(S.big, cxxx, H * 0.3F, 34.0F, new Color(AMB.r, AMB.g, AMB.b, a), 0, true);
         this.osd(S.bigSub, cxxx, H * 0.3F + 30.0F, 15.0F, new Color(1.0F, 1.0F, 1.0F, a), 0, false);
      }

      if (S.st.equals("boom")) {
         this.osd(S.D.alive ? "" : (S.stock > 0 ? "ДРОН ПОТЕРЯН · СИГНАЛА НЕТ" : "ДРОНЫ ЗАКОНЧИЛИСЬ"), cxxx, H * 0.5F, 20.0F, C_OSD, 0, true);
      }

      if (S.fade > 0.0F) {
         this.d.rect(0.0F, 0.0F, W, H, new Color(0.01F, 0.02F, 0.025F, 1.0F), S.fade);
         if (S.fade > 0.6F && S.st.equals("clear")) {
            this.osd("ПЕРЕБРОСКА К РУБЕЖУ " + (S.zone + 1) + "…", cxxx, cyy + 8.0F, 22.0F, new Color(TEAL.r, TEAL.g, TEAL.b, S.fade), 0, true);
         }
      }

      if (S.st.equals("fly") && D.alive) {
         boolean touch = !this.DESK || this.joyId >= 0 || this.touches.size() > 0;
         if (touch) {
            this.sBtn(this.sBoost(), 58.0F, "УСКОР", C("c45a24"), D.bat > 0.0F, "");
            this.sBtn(this.sFlare(), 40.0F, "ЛОВУШКИ", C("2a5bc4"), D.flares > 0 && S.flareCd <= 0.0F, "×" + D.flares);
            this.sBtn(this.sDet(), 36.0F, "ПОДРЫВ", C("a02020"), true, "");
            if (!S.S.wing) {
               this.sBtn(this.sBrake(), 36.0F, "ТОРМОЗ", C("2f6e63"), true, "");
            }

            if (S.S.bombs > 0) {
               this.sBtn(this.sBomb(), 40.0F, "СБРОС", C("7a6a20"), D.nb > 0, "×" + D.nb);
            }

            float jx = this.joyId >= 0 ? this.joyO.x : 130.0F;
            float jy = this.joyId >= 0 ? this.joyO.y : H - 130.0F;
            this.d.ring(jx, jy, 75.0F, 2.0F, WHITE, 0.25F);
            this.d.circle(this.joyId >= 0 ? this.joyP.x : jx, this.joyId >= 0 ? this.joyP.y : jy, 28.0F, TEAL, 0.4F);
         } else {
            this.osd("Ловушки ×" + D.flares + (S.S.bombs > 0 ? "   Бомбы ×" + D.nb : ""), W - 20.0F, H - 60.0F, 14.0F, C_OSD, 1, true);
            this.osd(
               "Мышь — курс · WASD — руль · ЛКМ/Shift — ускор · Ctrl — тормоз · ПКМ/Пробел — ловушки · E — сброс · X — подрыв · C — камера",
               cxxx,
               H - 6.0F,
               11.0F,
               new Color(1.0F, 1.0F, 1.0F, 0.55F),
               0,
               false
            );
            if (!Gdx.input.isCursorCatched() && !this.host.bot()) {
               this.osd("Кликните по экрану, чтобы управлять мышью", cxxx, H * 0.62F, 16.0F, C_OSD, 0, true);
            }
         }
      }
   }

   void minimap(float cx0, float cy0, float R) {
      Strike S = this.strike;
      this.d.circle(cx0, cy0, R, new Color(0.0F, 0.05F, 0.05F, 1.0F), 0.55F);
      this.d.ring(cx0, cy0, R, 1.5F, C_TEAL, 0.6F);
      Vector3 me = S.D.pos;
      float yaw = S.D.yaw;
      float scale = R / 420.0F;
      float cs = MathUtils.cos(yaw);
      float sn = MathUtils.sin(yaw);
      ArrayIterator var10 = S.units.iterator();

      while (var10.hasNext()) {
         Strike.Obj u = (Strike.Obj)var10.next();
         if (u.alive) {
            float[] m = mapPt(u.pos, me, cs, sn, scale, R, cx0, cy0);
            float det = S.detRange(u) * scale;
            if (Vector2.dst(m[0], m[1], cx0, cy0) + det < R) {
               this.d.ring(m[0], m[1], det, 1.0F, new Color(1.0F, 0.3F, 0.2F, 1.0F), 0.18F);
            }

            this.d.rect(m[0] - 3.0F, m[1] - 3.0F, 6.0F, 6.0F, u.track > 0.3F ? C_SRED : new Color(1.0F, 0.5F, 0.4F, 1.0F), u.track > 0.3F ? 1.0F : 0.8F);
         }
      }

      var10 = S.targets.iterator();

      while (var10.hasNext()) {
         Strike.Obj t = (Strike.Obj)var10.next();
         if (t.alive) {
            float[] m = mapPt(t.pos, me, cs, sn, scale, R, cx0, cy0);
            this.d.poly(new float[]{m[0], m[1] - 5.0F, m[0] + 5.0F, m[1], m[0], m[1] + 5.0F, m[0] - 5.0F, m[1]}, C_ACC, 1.0F);
         }
      }

      var10 = S.hunters.iterator();

      while (var10.hasNext()) {
         Strike.Hunter h = (Strike.Hunter)var10.next();
         if (h.alive) {
            float[] m = mapPt(h.pos, me, cs, sn, scale, R, cx0, cy0);
            this.d.circle(m[0], m[1], 2.5F, C_SRED, 1.0F);
         }
      }

      var10 = S.allMissiles(this.tmpArr).iterator();

      while (var10.hasNext()) {
         Vector3 p = (Vector3)var10.next();
         float[] m = mapPt(p, me, cs, sn, scale, R, cx0, cy0);
         this.d.circle(m[0], m[1], 2.0F, WHITE, 1.0F);
      }

      this.d.poly(new float[]{cx0, cy0 - 7.0F, cx0 + 5.0F, cy0 + 5.0F, cx0 - 5.0F, cy0 + 5.0F}, C_TEAL, 1.0F);
   }

   static float[] mapPt(Vector3 p, Vector3 me, float cs, float sn, float scale, float R, float cx0, float cy0) {
      float dx = p.x - me.x;
      float dz = p.z - me.z;
      float x = (dx * cs - dz * sn) * scale;
      float z = (dx * sn + dz * cs) * scale;
      float l = (float)Math.sqrt(x * x + z * z);
      if (l > R - 4.0F) {
         x = x / l * (R - 4.0F);
         z = z / l * (R - 4.0F);
      }

      return new float[]{cx0 + x, cy0 + z};
   }

   boolean strikeInput() {
      return this.strike != null
         && this.strike.active
         && !this.strike.paused
         && this.panel.isEmpty()
         && (this.strike.st.equals("fly") || this.strike.st.equals("boom") || this.strike.st.equals("clear"));
   }

   void strikePause() {
      if (this.strike != null && this.strike.active && !this.strike.paused) {
         this.strike.pause();
         this.touches.clear();
         this.joyId = -1;
         this.mbBoost = false;
         this.show("spause");
      }
   }

   void strikeKeys() {
      Strike S = this.strike;
      float x = 0.0F;
      float y = 0.0F;
      if (Gdx.input.isKeyPressed(29) || Gdx.input.isKeyPressed(21)) {
         x--;
      }

      if (Gdx.input.isKeyPressed(32) || Gdx.input.isKeyPressed(22)) {
         x++;
      }

      if (Gdx.input.isKeyPressed(51) || Gdx.input.isKeyPressed(19)) {
         y++;
      }

      if (Gdx.input.isKeyPressed(47) || Gdx.input.isKeyPressed(20)) {
         y--;
      }

      if (this.joyId < 0 && !S.bot) {
         S.inX = x;
         S.inY = y;
      }

      boolean tb = false;
      boolean tbr = false;

      for (String w : this.touches.values()) {
         if (w.equals("boost")) {
            tb = true;
         }

         if (w.equals("brake")) {
            tbr = true;
         }
      }

      if (!S.bot) {
         S.inBoost = Gdx.input.isKeyPressed(59) || Gdx.input.isKeyPressed(60) || this.mbBoost || tb;
         S.inBrake = Gdx.input.isKeyPressed(129) || Gdx.input.isKeyPressed(130) || tbr;
      }
   }

   boolean strikeTouchDown(float x, float y, int ptr, int button) {
      Strike S = this.strike;
      if (this.DESK) {
         if (!Gdx.input.isCursorCatched()) {
            if (button == 0) {
               if (in(x, y, PAUSE_R[0], PAUSE_R[1], PAUSE_R[2], PAUSE_R[3])) {
                  this.strikePause();
               } else {
                  float[] c = this.sCam();
                  if (in(x, y, c[0], c[1], c[2], c[3])) {
                     S.toggleCam();
                  } else {
                     this.mouseFree = false;
                  }
               }
            }
         } else if (button == 0) {
            this.mbBoost = true;
         } else if (button == 1) {
            S.useFlares();
         } else if (button == 2) {
            S.dropBomb();
         }

         return true;
      } else {
         String w = "look";
         if (dst(x, y, this.sBoost()) < 64.0F) {
            w = "boost";
         } else if (dst(x, y, this.sFlare()) < 46.0F) {
            w = "ui";
            S.useFlares();
         } else if (dst(x, y, this.sDet()) < 42.0F) {
            w = "ui";
            S.detonateNow();
         } else if (!S.S.wing && dst(x, y, this.sBrake()) < 42.0F) {
            w = "brake";
         } else if (S.S.bombs > 0 && dst(x, y, this.sBomb()) < 44.0F) {
            w = "ui";
            S.dropBomb();
         } else {
            if (in(x, y, PAUSE_R[0], PAUSE_R[1], PAUSE_R[2], PAUSE_R[3])) {
               w = "ui";
               this.strikePause();
               return true;
            }

            float[] c = this.sCam();
            if (in(x, y, c[0] - 8.0F, c[1] - 8.0F, c[2] + 16.0F, c[3] + 16.0F)) {
               w = "ui";
               S.toggleCam();
            } else if (x < this.sx() * 0.45F && this.joyId < 0) {
               w = "joy";
               this.joyId = ptr;
               this.joyO.set(x, y);
               this.joyP.set(x, y);
            }
         }

         this.touches.put(ptr, w);
         this.lastTouch.put(ptr, new Vector2(x, y));
         return true;
      }
   }

   static float dst(float x, float y, float[] c) {
      return Vector2.dst(x, y, c[0], c[1]);
   }

   boolean strikeDrag(float x, float y, int ptr) {
      Strike S = this.strike;
      String w = this.touches.get(ptr);
      Vector2 lt = this.lastTouch.get(ptr);
      if ("joy".equals(w)) {
         this.joyP.set(x, y);
         float vx = (this.joyP.x - this.joyO.x) / 75.0F;
         float vy = (this.joyP.y - this.joyO.y) / 75.0F;
         float l = (float)Math.sqrt(vx * vx + vy * vy);
         if (l > 1.0F) {
            vx /= l;
            vy /= l;
         }

         S.inX = vx;
         S.inY = -vy;
      } else if ("look".equals(w) && lt != null) {
         float k = 0.0032F * G.SV.sens;
         S.mdX = S.mdX + (x - lt.x) * k;
         S.mdY = S.mdY - (y - lt.y) * k;
      }

      if (lt != null) {
         lt.set(x, y);
      }

      return true;
   }

   boolean strikeUp(int ptr, int button) {
      if (this.DESK && button == 0) {
         this.mbBoost = false;
      }

      if (ptr == this.joyId) {
         this.joyId = -1;
         this.strike.inX = 0.0F;
         this.strike.inY = 0.0F;
      }

      this.touches.remove(ptr);
      this.lastTouch.remove(ptr);
      return true;
   }

   boolean strikeKey(int kc) {
      Strike S = this.strike;
      switch (kc) {
         case 30:
         case 33:
            S.dropBomb();
            return true;
         case 31:
         case 50:
            S.toggleCam();
            return true;
         case 34:
         case 62:
            S.useFlares();
            return true;
         case 44:
         case 111:
            this.strikePause();
            return true;
         case 52:
            S.detonateNow();
            return true;
         case 61:
            this.mouseFree = !this.mouseFree;
            return true;
         default:
            return false;
      }
   }

   boolean bossRole() {
      return this.g.mode.equals("duel") && this.g.duelRole.equals("boss");
   }

   void openDuel() {
      this.duelView = "main";
      if (this.duel != null) {
         this.duel.stop();
      }

      this.show("duel");
   }

   static String pickName(String role, String k) {
      if (k == null || k.isEmpty()) {
         return "—";
      } else if (role.equals("boss")) {
         JsonValue b = G.D.get("BOSS_INFO").get(k);
         return b != null ? b.getString("short") : k;
      } else {
         return G.vehName(k);
      }
   }

   String pickGrid(String role, String cur) {
      String[] list = role.equals("boss") ? Game.DUEL_BOSSES : Duel.VEHS;
      int cols = Math.max(2, Math.min(5, (int)(this.cw / 120.0F)));
      float gp = 6.0F;
      float w = (this.cw - gp * (cols - 1)) / cols;
      String res = null;

      for (int i = 0; i < list.length; i++) {
         float x = this.cx + i % cols * (w + gp);
         float y = this.cy + i / cols * (38.0F + gp);
         if (this.button(x, y, w, 38.0F, pickName(role, list[i]), list[i].equals(cur) ? 2 : 0, true, 13.0F)) {
            res = list[i];
         }
      }

      this.cy += (list.length + cols - 1) / cols * (38.0F + gp) + 4.0F;
      if (role.equals("boss") && cur != null && !cur.isEmpty()) {
         this.label("Особое умение: " + Game.SPEC_NAME.get(cur), 13.0F, C_TEAL);
      }

      return res;
   }

   void hostWith(String role, String pick) {
      Duel D = this.duel;
      if (D.hosting() && D.isHost) {
         D.myRole = role;
         D.myPick = pick;
         D.theirReady = false;
         D.hostPickChanged();
      } else {
         String err = D.host(role, pick);
         if (!err.isEmpty()) {
            this.toast(err);
            this.duelView = "main";
         }
      }
   }

   void pDuel() {
      Duel D = this.duel;
      this.frame(
         "duel",
         660.0F,
         false,
         true,
         () -> {
            this.title("Дуэль по Wi‑Fi");
            if (this.duelView.equals("main")) {
               this.label(
                  "Один игрок — расчёт ПВО, другой — босс. Оба устройства должны быть в одной Wi‑Fi сети. Босс побеждает, если разрушит комплекс. ПВО — если собьёт босса или продержится 3 минуты.",
                  14.0F,
                  C_MUT,
                  0,
                  0
               );
               if (this.btn("Создать игру", 50.0F, 1, true)) {
                  this.duelView = "host";
                  this.hostWith("pvo", "pantsir");
               }

               if (this.btn("Найти игру", 46.0F, 0, true)) {
                  this.duelView = "join";
                  D.stop();
                  D.startDiscovery();
               }
            } else if (this.duelView.equals("host")) {
               this.label("Ваш адрес в сети: " + Duel.localIp(), 15.0F, C_TEAL, 1, 0);
               this.section("Ваша роль");
               int r = this.btnRow(new String[]{"Я — ПВО", "Я — босс"}, new int[]{D.myRole.equals("pvo") ? 1 : 0, D.myRole.equals("boss") ? 1 : 0}, null, 42.0F);
               if (r == 0 && !D.myRole.equals("pvo")) {
                  this.hostWith("pvo", "pantsir");
               }

               if (r == 1 && !D.myRole.equals("boss")) {
                  this.hostWith("boss", "gunship");
               }

               this.section("Выбор: " + (D.myRole.equals("boss") ? "ваш босс" : "ваша машина"));
               String k = this.pickGrid(D.myRole, D.myPick);
               if (k != null) {
                  D.myPick = k;
                  D.hostPickChanged();
               }

               String st;
               if (!D.connected) {
                  st = "Ждём соперника… Пусть он нажмёт «Найти игру» на своём устройстве.";
               } else if (!D.theirReady) {
                  st = "Соперник подключился и выбирает " + (D.myRole.equals("boss") ? "машину" : "босса") + "…";
               } else {
                  st = "Соперник готов: " + pickName(D.myRole.equals("boss") ? "pvo" : "boss", D.theirPick);
               }

               this.label(st, 14.0F, D.canStart() ? C_ACC : C_MUT, 0, 0);
               if (this.btn("Начать бой", 50.0F, 1, D.canStart())) {
                  D.begin();
               }

               if (this.btn("Отмена", 38.0F, 0, true)) {
                  this.openDuel();
               }
            } else {
               if (D.connected) {
                  this.label("Подключено! Ваша роль: " + (D.myRole.equals("boss") ? "БОСС" : "ПВО"), 16.0F, C_TEAL, 1, 0);
                  if (!D.theirPick.isEmpty()) {
                     this.label("Соперник выбрал: " + pickName(D.myRole.equals("pvo") ? "boss" : "pvo", D.theirPick), 14.0F, C_TXT);
                  }

                  this.section("Выбор: " + (D.myRole.equals("boss") ? "ваш босс" : "ваша машина"));
                  String kx = this.pickGrid(D.myRole, D.myPick);
                  if (kx != null) {
                     D.myPick = kx;
                  }

                  if (this.btn("Готов", 50.0F, 1, true)) {
                     D.sendPick(D.myPick);
                     this.toast("Ждём, пока создатель начнёт бой");
                  }
               } else {
                  this.section("Игры в сети");
                  List<Entry<String, String[]>> list;
                  synchronized (D.found) {
                     list = new ArrayList<>(D.found.entrySet());
                  }

                  if (list.isEmpty()) {
                     this.label("Ищем… Если игра не находится, введите адрес, который показан у создателя игры.", 13.0F, C_MUT);
                  }

                  for (Entry<String, String[]> e : list) {
                     String[] f = e.getValue();
                     String roleTxt = f[1].equals("pvo") ? "он — ПВО, вы — босс" : "он — босс, вы — ПВО";
                     if (this.btn(f[0] + " · " + roleTxt + " · " + e.getKey(), 42.0F, 2, true)) {
                        D.join(e.getKey());
                        this.toast("Подключаемся к " + e.getKey() + "…");
                     }
                  }

                  float w = this.cw - 170.0F;
                  if (!this.d.off) {
                     this.box(this.cx, this.cy, w, 40.0F, WHITE, 0.05F, C_LINE, 0.4F, 1.0F);
                     this.d
                        .text(
                           this.ipText.isEmpty() ? "192.168.1.5" : this.ipText, this.cx + 12.0F, this.cy + 26.0F, 15.0F, this.ipText.isEmpty() ? C_MUT : C_TXT
                        );
                  }

                  if (this.tap(this.cx, this.cy, w, 40.0F)) {
                     Gdx.input.getTextInput(new TextInputListener() {
                        public void input(String text) {
                           UI.this.ipText = text.trim();
                        }

                        public void canceled() {
                        }
                     }, "Адрес создателя игры", this.ipText, "192.168.1.5");
                  }

                  if (this.button(this.cx + w + 8.0F, this.cy, 162.0F, 40.0F, "Подключиться", 0, true, 0.0F)) {
                     if (this.ipText.isEmpty()) {
                        this.toast("Введите адрес");
                     } else {
                        D.join(this.ipText);
                        this.toast("Подключаемся к " + this.ipText + "…");
                     }
                  }

                  this.cy += 48.0F;
               }

               if (this.btn("Отмена", 38.0F, 0, true)) {
                  this.openDuel();
               }
            }
         }
      );
   }

   @Override
   public void duelResult(boolean lostLink, boolean win, String reason) {
      this.dLost = lostLink;
      this.dWin = win;
      this.dReason = reason;
      if (!lostLink && win) {
         G.addXp(25);
         G.SV.c += 60;
         this.g.statAdd("duelw", 1.0F);
         this.g.checkAch();
         G.save();
      }

      G.sfx(win ? "clear" : "fail");
      this.overDelay = -1.0F;
      this.duelResT = 0.8F;
   }

   void pDRes() {
      this.frame("dres", 480.0F, false, true, () -> {
         String t = this.dLost ? "Бой прерван" : (this.dWin ? "Победа!" : "Поражение");
         this.d.text(t.toUpperCase(), this.cx + this.cw / 2.0F, this.cy + 30.0F, 30.0F, this.dLost ? C_MUT : (this.dWin ? C_ACC : C_SRED), 3, 0);
         this.cy += 46.0F;
         this.label(this.dReason, 16.0F, C_TXT, 0, 0);
         if (!this.dLost) {
            this.label(this.dWin ? "+60 монет · +25 опыта" : "Попробуйте ещё раз!", 14.0F, C_TEAL, 2, 0);
         }

         boolean rem = this.duel != null && this.duel.connected;
         int r = rem ? this.btnRow(new String[]{"Реванш", "В меню"}, new int[]{1, 0}, null, 46.0F) : (this.btn("В меню", 46.0F, 0, true) ? 1 : -1);
         if (rem && r == 0) {
            this.rematch();
         } else if (r == 1) {
            if (this.duel != null) {
               this.duel.leave();
            }

            this.toMenu();
         }
      });
   }

   void rematch() {
      this.clearComm();
      this.g.quitBattle();
      this.duel.clearVis();
      this.g.netView = false;
      this.g.duelRole = "";
      this.pending = () -> {
         this.host.toMenuWorld();
         this.duel.theirReady = false;
         this.duelView = this.duel.isHost ? "host" : "join";
         if (this.duel.isHost) {
            this.duel.hostPickChanged();
         }

         this.show("duel");
      };
      this.pendingFrames = 1;
   }

   float[] b1C() {
      return this.fireC();
   }

   float[] b2C() {
      return this.misC();
   }

   float[] b3C() {
      return new float[]{this.sx() - 81.0F, this.sy() - 81.0F - 112.0F};
   }

   float[] upC() {
      return new float[]{this.sx() - 300.0F, this.sy() - 150.0F};
   }

   float[] dnC() {
      return new float[]{this.sx() - 300.0F, this.sy() - 72.0F};
   }

   void bossPress(int i) {
      if (this.duel != null) {
         this.duel.bossPress(i);
      }
   }

   void cdBtn(float[] c, float r, String label, float cd, float cdmax, Color col) {
      boolean ready = cd <= 0.0F;
      this.roundBtn(
         c[0],
         c[1],
         r,
         ready ? col : darker(col, 0.55F),
         ready
            ? new Color(Math.min(1.0F, col.r * 1.15F + 0.05F), Math.min(1.0F, col.g * 1.15F + 0.05F), Math.min(1.0F, col.b * 1.15F + 0.05F), 1.0F)
            : darker(col, 0.45F),
         false,
         ready ? new Color(1.0F, 1.0F, 1.0F, 0.5F) : new Color(1.0F, 1.0F, 1.0F, 0.12F)
      );
      this.d.text(label, c[0], c[1] + (ready ? 5 : -2), r > 50.0F ? 15.0F : 12.0F, WHITE, 1, 0);
      if (!ready) {
         this.d.text(String.format("%.1f", cd), c[0], c[1] + 16.0F, 13.0F, new Color(1.0F, 1.0F, 1.0F, 0.8F), 1, 0);
         this.d
            .arc(
               c[0],
               c[1],
               r + 2.0F,
               (float) (-Math.PI / 2),
               (float) (-Math.PI / 2) + (float) (Math.PI * 2) * (1.0F - cd / Math.max(0.01F, cdmax)),
               3.0F,
               C_ACC,
               1.0F
            );
      }
   }

   static String mmss(float t) {
      return String.format("%d:%02d", (int)t / 60, (int)t % 60);
   }

   void drawBossHud() {
      Game G0 = this.g;
      float W = this.sx();
      float H = this.sy();
      Game.E be = G0.duelBoss();
      this.rrect(72.0F, 12.0F, 120.0F, 48.0F, C_PANEL, 0.94F, true);
      this.d.text(mmss(G0.duelT), 132.0F, 46.0F, 26.0F, G0.duelT > 30.0F ? C_ACC : C_SRED, 1, 0);
      float bw = Math.min(460.0F, W * 0.5F);
      this.rrect((W - bw) / 2.0F - 14.0F, 10.0F, bw + 28.0F, 74.0F, C_PANEL, 0.94F, true);
      if (be != null) {
         this.d.text("ВАШ БОСС: " + G.D.get("BOSS_INFO").get(be.type).getString("name"), W / 2.0F, 26.0F, 13.0F, C("e6d2ff"), 1, 0);
         this.bar((W - bw) / 2.0F, 32.0F, bw, 10.0F, be.hp / Math.max(0.001F, be.max), be.inv ? C("a4d0ff") : C_BOSS);
      }

      this.d.text("КОМПЛЕКС ПВО", W / 2.0F, 62.0F, 12.0F, C_MUT, 1, 0);
      this.bar((W - bw) / 2.0F, 68.0F, bw, 8.0F, G0.hp / Math.max(1.0F, G0.st("maxHp")), C_SRED);
      float dist = be != null ? (float)Math.hypot(be.pos.x, be.pos.z) : 0.0F;
      if (this.proj(this.tv2.set(0.0F, 3.0F, 0.0F), this.sp) && this.onScreen(this.sp)) {
         for (int sx2 = -1; sx2 <= 1; sx2 += 2) {
            for (int sy2 = -1; sy2 <= 1; sy2 += 2) {
               float ox = this.sp.x + sx2 * 16;
               float oy = this.sp.y + sy2 * 16;
               this.d.line(ox, oy, ox - sx2 * 8, oy, 2.0F, C_SRED, 1.0F);
               this.d.line(ox, oy, ox, oy - sy2 * 8, 2.0F, C_SRED, 1.0F);
            }
         }

         this.d.text("ЦЕЛЬ · " + (int)dist + " м", this.sp.x, this.sp.y - 24.0F, 13.0F, C("ffb0a6"), 1, 0);
      } else {
         this.edgeArrow(new Vector3(0.0F, 3.0F, 0.0F), C_SRED, "комплекс " + (int)dist + " м");
      }

      this.d.circle(W / 2.0F, H / 2.0F, 2.0F, WHITE, 0.6F);
      String tp = be != null ? be.type : "boss";
      this.cdBtn(this.b1C(), 59.0F, "РАКЕТЫ", G0.bossCd[0], 4.0F, C("d03a28"));
      this.cdBtn(this.b2C(), 42.0F, "РОЙ", G0.empT <= 0.0F ? G0.bossCd[1] : 9.9F, 15.0F, C("2a5bc4"));
      this.cdBtn(this.b3C(), 40.0F, Game.SPEC_SHORT.containsKey(tp) ? Game.SPEC_SHORT.get(tp) : "ОСОБОЕ", G0.bossCd[2], Game.specCd(tp), C("7a3cc4"));
      if (G0.bossSpecT > 0.0F) {
         this.d.text(String.format("%s: %.1f с", Game.SPEC_NAME.get(tp), G0.bossSpecT), W / 2.0F, H * 0.78F, 16.0F, C_TEAL, 1, 0);
      }

      if (G0.empT > 0.0F) {
         this.d.text("ПОМЕХИ! Вы замедлены, рой недоступен", W / 2.0F, H * 0.72F, 16.0F, C_RANK, 1, 0);
      }

      if (this.DESK) {
         float[] a = this.b1C();
         float[] b = this.b2C();
         float[] c = this.b3C();
         this.d.text("ЛКМ/1", a[0], a[1] + 32.0F, 11.0F, new Color(1.0F, 1.0F, 1.0F, 0.75F), 0, 0);
         this.d.text("ПКМ/2", b[0], b[1] - 50.0F, 11.0F, new Color(1.0F, 1.0F, 1.0F, 0.75F), 0, 0);
         this.d.text("Q/3", c[0], c[1] - 48.0F, 11.0F, new Color(1.0F, 1.0F, 1.0F, 0.75F), 0, 0);
         this.d
            .text("WASD — полёт · Пробел / C — выше / ниже · мышь — обзор · Esc — пауза", W / 2.0F, H - 14.0F, 12.0F, new Color(1.0F, 1.0F, 1.0F, 0.6F), 0, 0);
         if (!Gdx.input.isCursorCatched() && !this.host.bot()) {
            this.d.text("Кликните по экрану, чтобы управлять мышью", W / 2.0F, H * 0.62F, 16.0F, new Color(1.0F, 1.0F, 1.0F, 0.85F), 1, 0);
         }
      }

      if (!this.DESK || this.joyId >= 0) {
         if (!tp.equals("walker")) {
            this.cdBtn(this.upC(), 30.0F, "ВВЕРХ", 0.0F, 1.0F, C("2f6e63"));
            this.cdBtn(this.dnC(), 30.0F, "ВНИЗ", 0.0F, 1.0F, C("2f6e63"));
         }

         float jx = this.joyId >= 0 ? this.joyO.x : 120.0F;
         float jy = this.joyId >= 0 ? this.joyO.y : H - 120.0F;
         this.d.ring(jx, jy, 70.0F, 2.0F, WHITE, 0.25F);
         this.d.circle(this.joyId >= 0 ? this.joyP.x : jx, this.joyId >= 0 ? this.joyP.y : jy, 26.0F, C_TEAL, 0.45F);
      }

      this.rrect(PAUSE_R[0], PAUSE_R[1], PAUSE_R[2], PAUSE_R[3], C_PANEL, 0.94F, true);
      this.d.rect(29.0F, 27.0F, 5.0F, 18.0F, WHITE);
      this.d.rect(39.0F, 27.0F, 5.0F, 18.0F, WHITE);
      if (G0.hurtT > 0.0F) {
         this.d.rect(0.0F, 0.0F, W, H, new Color(0.86F, 0.08F, 0.04F, 1.0F), MathUtils.clamp(G0.hurtT * 0.3F, 0.0F, 0.18F));
      }

      if (this.bannerT > 0.0F) {
         float ba = MathUtils.clamp(Math.min(this.bannerT / 0.4F, (3.0F - this.bannerT) / 0.3F), 0.0F, 1.0F);
         this.d.text(this.bannerS, W / 2.0F, H * 0.3F + 38.0F, 34.0F, new Color(0.85F, 0.7F, 1.0F, ba), 1, 0);
         if (!this.bannerSub.isEmpty()) {
            this.d.text(this.bannerSub, W / 2.0F, H * 0.3F + 68.0F, 16.0F, new Color(1.0F, 0.886F, 0.66F, ba), 0, 0);
         }
      }
   }

   void bossKeys() {
      Game.BossIn b = this.g.bossIn;
      if (this.DESK && !this.host.bot()) {
         float x = 0.0F;
         float z = 0.0F;
         float y = 0.0F;
         if (Gdx.input.isKeyPressed(51) || Gdx.input.isKeyPressed(19)) {
            z++;
         }

         if (Gdx.input.isKeyPressed(47) || Gdx.input.isKeyPressed(20)) {
            z--;
         }

         if (Gdx.input.isKeyPressed(32) || Gdx.input.isKeyPressed(22)) {
            x++;
         }

         if (Gdx.input.isKeyPressed(29) || Gdx.input.isKeyPressed(21)) {
            x--;
         }

         if (Gdx.input.isKeyPressed(62)) {
            y++;
         }

         if (Gdx.input.isKeyPressed(31) || Gdx.input.isKeyPressed(129) || Gdx.input.isKeyPressed(59)) {
            y--;
         }

         if (this.joyId < 0) {
            b.mx = x;
            b.mz = z;
         }

         if (this.altBtn.isEmpty()) {
            b.my = y;
         }
      }
   }

   boolean bossTouchDown(float x, float y, int ptr, int button) {
      Game.BossIn b = this.g.bossIn;
      if (this.DESK) {
         if (!Gdx.input.isCursorCatched()) {
            if (button == 0) {
               if (in(x, y, PAUSE_R[0], PAUSE_R[1], PAUSE_R[2], PAUSE_R[3])) {
                  this.pause();
               } else {
                  this.mouseFree = false;
               }
            }
         } else if (button == 0) {
            this.bossPress(0);
         } else if (button == 1) {
            this.bossPress(1);
         }

         return true;
      } else {
         String w;
         if (dst(x, y, this.b1C()) < 64.0F) {
            this.bossPress(0);
            w = "ui";
         } else if (dst(x, y, this.b2C()) < 48.0F) {
            this.bossPress(1);
            w = "ui";
         } else if (dst(x, y, this.b3C()) < 46.0F) {
            this.bossPress(2);
            w = "ui";
         } else if (dst(x, y, this.upC()) < 36.0F) {
            this.altBtn.put(ptr, 1.0F);
            b.my = 1.0F;
            w = "ui";
         } else if (dst(x, y, this.dnC()) < 36.0F) {
            this.altBtn.put(ptr, -1.0F);
            b.my = -1.0F;
            w = "ui";
         } else if (in(x, y, PAUSE_R[0], PAUSE_R[1], PAUSE_R[2], PAUSE_R[3])) {
            this.pause();
            w = "ui";
         } else if (x < this.sx() * 0.45F && this.joyId < 0) {
            this.joyId = ptr;
            this.joyO.set(x, y);
            this.joyP.set(x, y);
            w = "joy";
         } else {
            w = "cam";
         }

         this.touches.put(ptr, w);
         this.lastTouch.put(ptr, new Vector2(x, y));
         return true;
      }
   }

   boolean bossDrag(float x, float y, int ptr) {
      Game.BossIn b = this.g.bossIn;
      String w = this.touches.get(ptr);
      Vector2 lt = this.lastTouch.get(ptr);
      if ("joy".equals(w)) {
         this.joyP.set(x, y);
         float vx = (this.joyP.x - this.joyO.x) / 70.0F;
         float vy = (this.joyP.y - this.joyO.y) / 70.0F;
         float l = (float)Math.sqrt(vx * vx + vy * vy);
         if (l > 1.0F) {
            vx /= l;
            vy /= l;
         }

         b.mx = vx;
         b.mz = -vy;
      } else if ("cam".equals(w) && lt != null) {
         float k = 0.0042F * G.SV.sens;
         b.cy = b.cy - (x - lt.x) * k;
         this.g.bossCamPitch = MathUtils.clamp(this.g.bossCamPitch + (y - lt.y) * k, -0.35F, 0.9F);
      }

      if (lt != null) {
         lt.set(x, y);
      }

      return true;
   }

   boolean bossUp(int ptr) {
      Game.BossIn b = this.g.bossIn;
      if (ptr == this.joyId) {
         this.joyId = -1;
         b.mx = 0.0F;
         b.mz = 0.0F;
      }

      if (this.altBtn.remove(ptr) != null) {
         b.my = 0.0F;
      }

      this.touches.remove(ptr);
      this.lastTouch.remove(ptr);
      return true;
   }

   boolean bossKey(int kc) {
      switch (kc) {
         case 8:
            this.bossPress(0);
            return true;
         case 9:
            this.bossPress(1);
            return true;
         case 10:
         case 33:
         case 34:
         case 45:
            this.bossPress(2);
            return true;
         case 44:
         case 111:
            this.pause();
            return true;
         case 61:
            this.mouseFree = !this.mouseFree;
            return true;
         default:
            return false;
      }
   }

   interface Body {
      void run();
   }

   static final class Card {
      String title;
      String desc = "";
      String btn;
      String sub;
      Color subc;
      boolean en;
      boolean sel;
      Runnable act;
      Texture sw;
      Color swc;
      int lv = -1;
      int max;
   }

   public interface Host {
      void applyGfx();

      void toMenuWorld();

      void toggleFullscreen();

      boolean isFullscreen();

      boolean bot();
   }

   public interface StrikeHost {
      void startStrike();

      void exitStrike(boolean var1);
   }
}
