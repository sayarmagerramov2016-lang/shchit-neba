package ru.shitneba.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ru.shitneba.G;
import ru.shitneba.Game;
import ru.shitneba.GameUI;
import ru.shitneba.gfx.Tex;

/** Интерфейс: меню-«тактический терминал», панели и боевой HUD. */
public class UI implements GameUI, InputProcessor {
    public interface Host {
        void applyGfx();
        void toMenuWorld();
        void toggleFullscreen();
        boolean isFullscreen();
        boolean bot();
    }

    static final HashMap<String, Color> CC = new HashMap<>();

    static Color C(String h) {
        Color c = CC.get(h);
        if (c == null) { c = G.hex(h); CC.put(h, c); }
        return c;
    }

    static final Color C_PANEL = new Color(0.03f, 0.045f, 0.05f, 0.94f), C_LINE = new Color(0.25f, 0.95f, 0.8f, 1f);
    static final Color C_ACC = C("ffb21a"), C_MUT = C("8fa59c"), C_TEAL = C("3ff0c8"), C_TXT = C("e6f1ec"), C_GREEN = C("3ddc97"),
            C_RANK = C("9fd0ff"), C_BUY = C("23d6a6"), C_BUYD = C("16b88c"), C_BUYH = C("4ff5c8"), C_RED = C("e8483c"), C_WARN = C("f0b53a"),
            C_BOSS = C("b07cff"), C_DARK = C("1c1200"), C_BUYT = C("03241b"), WHITE = Color.WHITE, BLACK = Color.BLACK;
    static final int NORMAL = 0, BIG = 1, BUY = 2;

    final Game g;
    final Draw d;
    final Host host;
    public String panel = "menu";
    public ru.shitneba.Strike strike;
    public ru.shitneba.Duel duel;
    float panelT, T;
    final Map<String, String> backTo = new HashMap<>();
    final Map<String, Float> panelH = new HashMap<>();

    // toast/banner/hint/comm
    String toastS; float toastT;
    String bannerS = "", bannerSub = ""; float bannerT; boolean bannerBoss;
    String hintS = ""; float hintT;
    final Array<String[]> commQ = new Array<>();
    String[] commCur; float commT;
    float overDelay = -1;
    Runnable pending; int pendingFrames;

    // ввод
    boolean down, dragged, tapReady, capture;
    int downPtr = -1;
    float downX, downY, curX, curY, lastY, tapX, tapY, mx = -1, my = -1;
    float scroll, maxScroll;
    final Map<Integer, String> touches = new HashMap<>();
    final Map<Integer, Vector2> lastTouch = new HashMap<>();
    boolean mouseFree;
    final boolean DESK;

    // раскладка панели
    float PX, PY, PW, PH, cx, cy, cw, viewTop, viewH;
    boolean clipOn;
    public int garageTab, shopVeh = -1;
    float hpShown = -1, hpGhost = -1;

    public UI(Game g, Draw d, Host host) {
        this.g = g; this.d = d; this.host = host;
        DESK = !G.isMobile();
        String[][] bt = {{"endless", "menu"}, {"garage", "menu"}, {"shop", "menu"}, {"settings", "menu"}, {"about", "menu"},
                {"quests", "menu"}, {"ach", "menu"}, {"records", "menu"}, {"campaign", "menu"}, {"chal", "menu"}, {"brief", "campaign"}, {"swarm", "menu"}, {"duel", "menu"}};
        for (String[] b : bt) backTo.put(b[0], b[1]);
    }

    // ================= GameUI =================
    @Override public void toast(String m) { toastS = m; toastT = 2.3f; }

    @Override public void banner(String t, boolean boss, String sub) { bannerS = t.toUpperCase(); bannerSub = sub; bannerT = 3f; bannerBoss = boss; }

    @Override public void hint(String t) { hintS = t; hintT = 5f; }

    @Override public void comm(String who, String text) {
        commQ.add(new String[]{who, text});
        if (commT <= 0) commNext();
    }

    void commNext() {
        if (commQ.size == 0) { commCur = null; commT = 0; return; }
        commCur = commQ.removeIndex(0);
        G.sfx("radio", 0.4f);
        commT = 1.8f + commCur[1].length() * 0.045f;
    }

    void clearComm() { commQ.clear(); commCur = null; commT = 0; }

    @Override public void startHud() { show(""); clearComm(); hpShown = -1; hpGhost = -1; }

    @Override public void over(boolean rec) { overRec = rec; overDelay = 0.5f; }

    @Override public void result(boolean win) { g.firing = false; touches.clear(); show("result"); }

    boolean overRec;

    // ================= панели =================
    public void show(String name) {
        if (!name.equals(panel)) { panelT = 0; scroll = 0; ru.shitneba.BlackBox.log("экран «" + name + "»"); }
        panel = name;
        tapReady = false;
        if (name.equals("menu")) g.ensureQuests();
    }

    boolean inBattle() { return panel.isEmpty() && (g.state.equals("run") || g.state.equals("dying")); }

    public void launch(final String m) {
        pending = () -> { show(""); g.startGame(m); };
        pendingFrames = 1;
    }

    void pause() {
        if (g.mode.equals("duel")) { g.firing = false; touches.clear(); joyId = -1; show("pause"); return; }
        if (!g.state.equals("run")) return;
        g.state = "pause"; g.firing = false; touches.clear(); show("pause");
    }

    void resume() { if (!g.mode.equals("duel")) g.state = "run"; show(""); }

    void toMenu() {
        clearComm();
        final boolean wasCamp = g.mode.equals("campaign") && g.CM != null;
        final boolean wasChal = wasCamp && "chal".equals(g.CM.getString("special", ""));
        g.quitBattle();
        pending = () -> {
            host.toMenuWorld();
            show(wasChal ? "chal" : (wasCamp ? "campaign" : "menu"));
        };
        pendingFrames = 1;
    }

    void back() {
        if (panel.equals("duel") && duel != null) duel.stop();
        if (panel.equals("shop") && g.state.equals("shop")) { g.state = "pause"; show("pause"); return; }
        if (panel.equals("settings") && g.state.equals("pause")) { show("pause"); return; }
        if (backTo.containsKey(panel)) show(backTo.get(panel));
    }

    public void androidBack() {
        if (strike != null && strike.active) {
            if (panel.equals("spause")) { strike.resume(); show(""); }
            else if (panel.equals("sover")) { if (strikeHost != null) strikeHost.exitStrike(false); }
            else strikePause();
            return;
        }
        if (panel.equals("dres")) { if (duel != null) duel.leave(); toMenu(); return; }
        if (panel.equals("duel")) { if (duel != null) duel.stop(); show("menu"); return; }
        if (inBattle()) { pause(); return; }
        if (panel.equals("pause")) { resume(); return; }
        if (panel.equals("over") || panel.equals("result")) { toMenu(); return; }
        if (panel.equals("cut")) { show("campaign"); return; }
        if (panel.equals("daily")) return;
        if (backTo.containsKey(panel) || panel.equals("shop") || panel.equals("settings")) { back(); return; }
        if (panel.equals("menu")) Gdx.app.exit();
    }

    String reportText;

    public void showReport(String text) { reportText = text; show("report"); }

    void pReport() {
        frame("report", 760, false, true, () -> {
            title("Чёрный ящик");
            label("В прошлый раз игра вылетела или зависла. Нажми «Скопировать» и пришли текст разработчику (или сделай скриншот).", 13, C_ACC, Draw.MAIN, 0);
            d.rect(cx, cy, cw, 1, C_TEAL, 0.3f);
            gap(6);
            label(reportText == null ? "" : reportText, 12, C_TXT);
            int r = btnRow(new String[]{"Скопировать", "Понятно"}, new int[]{NORMAL, BIG}, null, 46);
            if (r == 0) {
                try { Gdx.app.getClipboard().setContents(reportText); toast("Отчёт скопирован"); } catch (Throwable t) { toast("Не удалось скопировать"); }
            } else if (r == 1) { reportText = null; show("menu"); checkDaily(); }
        });
    }

    public void checkDaily() {
        if (G.SV.dailyLast.equals(G.today())) return;
        show("daily");
    }

    void openShopBattle() {
        if (!(g.state.equals("run") || g.state.equals("pause"))) return;
        g.state = "shop"; g.firing = false; touches.clear(); show("shop");
    }

    // ================= кадр =================
    public void update(float dt) {
        T += dt; panelT += dt;
        deskMouse();
        keyAim(dt);
        if (toastT > 0) toastT -= dt;
        if (commT > 0) { commT -= dt; if (commT <= 0) commNext(); }
        bannerT = Math.max(0, bannerT - dt); hintT = Math.max(0, hintT - dt);
        if (overDelay > 0) { overDelay -= dt; if (overDelay <= 0) { overDelay = -1; show("over"); } }
        if (duelResT > 0) { duelResT -= dt; if (duelResT <= 0) { g.firing = false; touches.clear(); show("dres"); } }
    }

    public void render() {
        d.begin();
        if (pending != null) {
            loading();
            d.end();
            if (pendingFrames-- <= 0) {
                Runnable r = pending; pending = null;
                ru.shitneba.BlackBox.hold = true;
                ru.shitneba.BlackBox.mark("загрузка");
                try { r.run(); } finally { ru.shitneba.BlackBox.tick(); ru.shitneba.BlackBox.hold = false; }
            }
            return;
        }
        boolean hud = panel.isEmpty() && (g.state.equals("run") || g.state.equals("dying") || g.state.equals("over") || g.state.equals("result"));
        if (strike != null && strike.active) { if (panel.isEmpty()) drawStrikeHud(); }
        else if (hud && bossRole()) drawBossHud();
        else if (hud) { if (g.killCamK() <= 0) drawHud(); drawKillBars(); }
        if (!panel.isEmpty()) drawPanel();
        drawToast();
        d.end();
        tapReady = false;
    }

    void loading() {
        d.rect(0, 0, d.w, d.h, C("05090a"));
        grid(0.06f);
        d.text("ЗАГРУЗКА…", d.w / 2, d.h / 2 + 10, 26, C_TEAL, Draw.WIDE, 0);
        float k = (T * 1.4f) % 1f;
        d.rect(d.w / 2 - 80, d.h / 2 + 30, 160, 3, C_TEAL, 0.15f);
        d.rect(d.w / 2 - 80 + 120 * k, d.h / 2 + 30, 40, 3, C_TEAL, 0.9f);
    }

    void drawPanel() {
        switch (panel) {
            case "menu": pMenu(); break;
            case "endless": pEndless(); break;
            case "garage": pGarage(); break;
            case "shop": pShop(); break;
            case "quests": pQuests(); break;
            case "ach": pAch(); break;
            case "records": pRecords(); break;
            case "settings": pSettings(); break;
            case "about": pAbout(); break;
            case "pause": pPause(); break;
            case "over": pOver(); break;
            case "daily": pDaily(); break;
            case "report": pReport(); break;
            case "campaign": pCampaign(); break;
            case "chal": pChal(); break;
            case "brief": pBrief(); break;
            case "cut": pCut(); break;
            case "result": pResult(); break;
            case "swarm": pSwarm(); break;
            case "spause": pSPause(); break;
            case "sover": pSOver(); break;
            case "duel": pDuel(); break;
            case "dres": pDRes(); break;
            default: break;
        }
        if (backTo.containsKey(panel) || panel.equals("shop") || (panel.equals("settings"))) backButton();
    }

    void backButton() {
        float x = 12, y = 12, w = 112, h = 40;
        if (button(x, y, w, h, "", NORMAL, true, 15)) back();
        if (!d.off) {
            float ax = x + 24, ay = y + h / 2;
            d.line(ax, ay, ax + 14, ay, 2, C_TXT, 1);
            d.line(ax, ay, ax + 6, ay - 6, 2, C_TXT, 1);
            d.line(ax, ay, ax + 6, ay + 6, 2, C_TXT, 1);
            d.text("Назад", x + 44, ay + 6, 15, C_TXT, Draw.TITLE, -1);
        }
    }

    // ---------- фон и коробки ----------
    void grid(float a) {
        Color gc = new Color(0.1f, 0.55f, 0.45f, 1);
        for (float x = 0; x < d.w; x += 32) d.rect(x, 0, 1, d.h, gc, a);
        for (float y = 0; y < d.h; y += 32) d.rect(0, y, d.w, 1, gc, a);
    }

    void dimBg() {
        d.rect(0, 0, d.w, d.h, new Color(0f, 0.012f, 0.014f, 1), 0.62f);
        grid(0.05f);
        float e = d.w * 0.18f;
        d.gradH(0, 0, e, d.h, BLACK, 0.35f, BLACK, 0);
        d.gradH(d.w - e, 0, e, d.h, BLACK, 0, BLACK, 0.35f);
    }

    void box(float x, float y, float w, float h, Color fill, float fa, Color border, float ba, float bw) {
        d.rect(x, y, w, h, fill, fa);
        if (ba > 0) d.frame(x, y, w, h, bw, border, ba);
    }

    void brackets(float x, float y, float w, float h) {
        float L = 16, o = 3, wd = 2;
        Color c = C_TEAL;
        float[][] cs = {{x - o, y - o, 1, 1}, {x + w + o, y - o, -1, 1}, {x - o, y + h + o, 1, -1}, {x + w + o, y + h + o, -1, -1}};
        for (float[] p : cs) {
            d.rect(Math.min(p[0], p[0] + p[2] * L), p[1] - (p[3] > 0 ? 0 : wd), L, wd, c, 0.85f);
            d.rect(p[0] - (p[2] > 0 ? 0 : wd), Math.min(p[1], p[1] + p[3] * L), wd, L, c, 0.85f);
        }
    }

    void sbox(float x, float y, float w, float h, Color fill, float fa, Color border, float ba, float left, float skew) {
        float o = skew * h;
        d.poly(new float[]{x + o, y, x + w, y, x + w - o, y + h, x, y + h}, fill, fa);
        if (ba > 0) {
            d.polyline(new float[]{x + o, y, x + w, y, x + w - o, y + h, x, y + h}, true, 1, border, ba);
            if (left > 1) d.line(x + left * 0.5f, y + h, x + o + left * 0.5f, y, left, border, ba);
        }
    }

    // ---------- ввод внутри панелей ----------
    static boolean in(float px, float py, float x, float y, float w, float h) { return px >= x && px <= x + w && py >= y && py <= y + h; }

    boolean inView(float py) { return !clipOn || (py >= viewTop && py <= viewTop + viewH); }

    boolean tap(float x, float y, float w, float h) {
        if (d.off || !tapReady) return false;
        if (!in(tapX, tapY, x, y, w, h) || !inView(tapY)) return false;
        tapReady = false;
        return true;
    }

    boolean pressing(float x, float y, float w, float h) { return !d.off && down && !dragged && in(curX, curY, x, y, w, h) && inView(curY); }

    boolean hover(float x, float y, float w, float h) { return DESK && !d.off && in(mx, my, x, y, w, h) && inView(my); }

    boolean button(float x, float y, float w, float h, String t, int style, boolean en, float fs) {
        if (!d.off) {
            boolean pr = en && pressing(x, y, w, h), hv = en && hover(x, y, w, h);
            Color tc;
            if (style == BIG) {
                Color f = pr ? C("e89400") : (hv ? C("ffc84d") : C_ACC);
                if (!en) f = C("6b5420");
                sbox(x, y, w, h, f, 1, WHITE, 0, 0, 0.16f);
                float o = 0.16f * h;
                d.quad(x + o, y, Color.toFloatBits(1, 1, 1, 0.22f), x + w, y, Color.toFloatBits(1, 1, 1, 0.22f),
                        x + w - o * 0.55f, y + h * 0.45f, Color.toFloatBits(1, 1, 1, 0f), x + o * 0.45f, y + h * 0.45f, Color.toFloatBits(1, 1, 1, 0f));
                tc = C_DARK;
            } else if (style == BUY) {
                if (en) sbox(x, y, w, h, pr ? C_BUYD : (hv ? C_BUYH : C_BUY), 1, C_TEAL, 0, 0, 0.12f);
                else sbox(x, y, w, h, WHITE, 0.05f, WHITE, 0.08f, 1, 0.12f);
                tc = en ? C_BUYT : new Color(0.55f, 0.62f, 0.6f, 0.7f);
            } else {
                if (en) sbox(x, y, w, h, C_TEAL, pr ? 0.26f : (hv ? 0.16f : 0.06f), C_TEAL, hv || pr ? 1f : 0.35f, 3, 0.16f);
                else sbox(x, y, w, h, WHITE, 0.025f, WHITE, 0.08f, 1, 0.16f);
                tc = en ? (hv ? WHITE : C_TXT) : new Color(0.55f, 0.62f, 0.6f, 0.55f);
            }
            if (!t.isEmpty()) {
                float size = fs > 0 ? fs : (style == BIG ? 18 : 15);
                int font = style == BIG ? Draw.WIDE : (style == BUY ? Draw.BOLD : Draw.MAIN);
                float tw = d.textW(t, size, font);
                if (tw > w - 16) size *= (w - 16) / tw;
                d.text(t, x + w / 2 + (pr ? 1 : 0), y + h / 2 + size * 0.36f + (pr ? 1 : 0), size, tc, font, 0);
            }
        }
        boolean c = en && tap(x, y, w, h);
        if (c) G.sfx("click");
        return c;
    }

    // ---------- раскладка ----------
    interface Body { void run(); }

    void frame(String name, float w, boolean left, boolean dim, Body body) {
        w = Math.min(w, d.w - 24);
        boolean hasBack = backTo.containsKey(name) || name.equals("shop") || name.equals("settings");
        float top = hasBack && (left || w > d.w - 300) ? 62 : 12;
        float maxH = d.h - top - 12;
        // замер
        boolean wasOff = d.off;
        d.off = true; clipOn = false;
        cx = 0; cy = 0; cw = w - 36; PW = w;
        body.run();
        float ch = cy - 8;
        d.off = wasOff;
        float innerH = Math.min(ch, maxH - 30);
        PW = w; PH = innerH + 30;
        PX = left ? Math.max(24, d.w * 0.04f) : (d.w - w) / 2;
        float k = Interp(Math.min(1, panelT / 0.22f));
        PY = Math.max(top, (d.h - PH) / 2) + (1 - k) * 14;
        if (dim) dimBg();
        // тень и фон
        for (int i = 1; i <= 4; i++) d.rect(PX - i * 3, PY - i * 3 + 6, PW + i * 6, PH + i * 6, BLACK, 0.07f);
        d.rect(PX, PY, PW, PH, C_PANEL, C_PANEL.a);
        d.grad(PX, PY, PW, 70, C_TEAL, 0.05f, C_TEAL, 0);
        d.frame(PX, PY, PW, PH, 1, C_LINE, 0.18f);
        d.rect(PX, PY, PW, 2, C_LINE, 0.3f);
        brackets(PX, PY, PW, PH);
        maxScroll = Math.max(0, ch - innerH);
        scroll = MathUtils.clamp(scroll, 0, maxScroll);
        viewTop = PY + 16; viewH = innerH;
        d.clip(PX + 2, viewTop - 2, PW - 4, viewH + 4);
        clipOn = true;
        cx = PX + 18; cy = viewTop - scroll; cw = PW - 36 - (maxScroll > 0 ? 8 : 0);
        body.run();
        d.unclip();
        clipOn = false;
        if (maxScroll > 0) {
            float tx = PX + PW - 10, th = viewH;
            d.rect(tx, viewTop, 5, th, C_TEAL, 0.08f);
            float gh = Math.max(24, th * innerH / ch), gy = viewTop + (th - gh) * (scroll / maxScroll);
            d.rect(tx, gy, 5, gh, C_TEAL, 0.45f);
        }
        panelH.put(name, PH);
    }

    static float Interp(float t) { return 1 - (1 - t) * (1 - t) * (1 - t); }

    void gap(float h) { cy += h; }

    void title(String t) {
        float size = 24;
        d.text(t.toUpperCase(), cx + cw / 2, cy + size, size, C_TXT, Draw.WIDE, 0);
        cy += size + 10;
        float c = cx + cw / 2;
        d.rect(c - 70, cy, 140, 2, C_TEAL, 0.7f);
        d.rect(c - 110, cy, 30, 2, C_TEAL, 0.3f);
        d.rect(c + 80, cy, 30, 2, C_TEAL, 0.3f);
        cy += 2 + 10;
    }

    void label(String t, float size, Color c, int font, int align) {
        float h = d.wrap(t, cx, cy, cw, size, c, font, align);
        cy += h + 6;
    }

    void label(String t, float size, Color c) { label(t, size, c, Draw.MAIN, -1); }

    void section(String t) {
        gap(4);
        d.rect(cx, cy + 3, 3, 14, C_ACC, 1);
        d.text(t, cx + 10, cy + 16, 17, C_TXT, Draw.TITLE, -1);
        cy += 26;
    }

    boolean btn(String t, float h, int style, boolean en) {
        boolean c = button(cx, cy, cw, h, t, style, en, 0);
        cy += h + 8;
        return c;
    }

    /** Ряд кнопок одинаковой ширины; возвращает индекс нажатой или -1. */
    int btnRow(String[] ts, int[] styles, boolean[] en, float h) {
        int n = ts.length; float gp = 6, w = (cw - gp * (n - 1)) / n;
        int r = -1;
        for (int i = 0; i < n; i++)
            if (button(cx + i * (w + gp), cy, w, h, ts[i], styles[i], en == null || en[i], 0)) r = i;
        cy += h + 8;
        return r;
    }

    void bar(float x, float y, float w, float h, float k, Color c) {
        d.rect(x, y, w, h, WHITE, 0.08f);
        if (k > 0) d.rect(x, y, w * MathUtils.clamp(k, 0, 1), h, c, 1);
    }

    int tabs(String[] names, int sel) {
        int n = names.length; float gp = 4, w = (cw - gp * (n - 1)) / n, h = 34;
        for (int i = 0; i < n; i++) {
            float x = cx + i * (w + gp);
            boolean s = i == sel;
            if (!d.off) {
                d.rect(x, cy, w, h, s ? C_TEAL : WHITE, s ? 0.14f : 0.03f);
                d.rect(x, cy + h - 2, w, 2, s ? C_TEAL : WHITE, s ? 1 : 0.1f);
                d.text(names[i], x + w / 2, cy + h / 2 + 5.5f, 15, s ? WHITE : C_MUT, s ? Draw.BOLD : Draw.MAIN, 0);
            }
            if (!s && tap(x, cy, w, h)) { sel = i; G.sfx("click"); scroll = 0; }
        }
        cy += h + 10;
        return sel;
    }

    // ---------- карточки ----------
    static final class Card {
        String title, desc = "", btn, sub; Color subc;
        boolean en, sel; Runnable act;
        Texture sw; Color swc;
        int lv = -1, max;
    }

    Card card(String title, String desc, String btn, boolean en, Runnable act) {
        Card c = new Card(); c.title = title; c.desc = desc; c.btn = btn; c.en = en; c.act = act; return c;
    }

    void cards(List<Card> cs, int cols) {
        cols = Math.max(1, Math.min(cols, (int) (cw / 190)));
        float gp = 8, w = (cw - gp * (cols - 1)) / cols;
        for (int i0 = 0; i0 < cs.size(); i0 += cols) {
            float rowH = 0;
            boolean was = d.off;
            d.off = true;
            for (int i = i0; i < Math.min(cs.size(), i0 + cols); i++) rowH = Math.max(rowH, cardH(cs.get(i), w));
            d.off = was;
            for (int i = i0; i < Math.min(cs.size(), i0 + cols); i++) drawCard(cs.get(i), cx + (i - i0) * (w + gp), cy, w, rowH);
            cy += rowH + gp;
        }
    }

    float cardH(Card c, float w) {
        float h = 12;
        if (c.sw != null || c.swc != null) h += 44;
        h += d.wrap(c.title, 0, 0, w - 24, 15, WHITE, Draw.BOLD, -1) + 2;
        if (c.sub != null) h += d.wrap(c.sub, 0, 0, w - 24, 13, WHITE, Draw.BOLD, -1) + 2;
        if (!c.desc.isEmpty()) h += d.wrap(c.desc, 0, 0, w - 24, 12, WHITE, Draw.MAIN, -1) + 2;
        if (c.lv >= 0) h += 14;
        h += 8 + 34 + 12;
        return h;
    }

    void drawCard(Card c, float x, float y, float w, float h) {
        if (!d.off) {
            box(x, y, w, h, c.sel ? C_ACC : WHITE, c.sel ? 0.07f : 0.05f, c.sel ? C_ACC : C_LINE, c.sel ? 1f : 0.22f, c.sel ? 2 : 1);
            if (c.sel) d.rect(x, y, 4, h, C_ACC, 1);
        }
        float yy = y + 12;
        if (c.sw != null) { d.tex(c.sw, x + 12, yy, w - 24, 40, (w - 24) / 64f, 40 / 64f); d.frame(x + 12, yy, w - 24, 40, 1, BLACK, 0.4f); yy += 44; }
        else if (c.swc != null) {
            d.rect(x + 12, yy + 10, w - 24, 20, BLACK, 0.4f);
            d.gradH(x + 16, yy + 17, w - 32, 6, c.swc, 0.1f, c.swc, 1);
            d.circle(x + w - 18, yy + 20, 5, WHITE, 0.9f);
            yy += 44;
        }
        yy += d.wrap(c.title, x + 12, yy, w - 24, 15, C_TXT, Draw.BOLD, -1) + 2;
        if (c.sub != null) yy += d.wrap(c.sub, x + 12, yy, w - 24, 13, c.subc != null ? c.subc : C_TEAL, Draw.BOLD, -1) + 2;
        if (!c.desc.isEmpty()) yy += d.wrap(c.desc, x + 12, yy, w - 24, 12, C_MUT, Draw.MAIN, -1) + 2;
        if (c.lv >= 0) {
            float pw = Math.min(16, (w - 24 - (c.max - 1) * 3) / Math.max(1, c.max));
            for (int i = 0; i < c.max; i++) d.rect(x + 12 + i * (pw + 3), yy + 2, pw, 6, i < c.lv ? C_ACC : WHITE, i < c.lv ? 1 : 0.12f);
            yy += 14;
        }
        if (button(x + 12, y + h - 12 - 34, w - 24, 34, c.btn, BUY, c.en, 14) && c.act != null) c.act.run();
    }

    // =========================================================
    //                         ПАНЕЛИ
    // =========================================================
    float rankK() {
        int ri = G.rankIndex();
        if (ri >= G.RANK_X.length - 1) return 1;
        int a = G.RANK_X[ri], b = G.RANK_X[ri + 1];
        return MathUtils.clamp((G.xp() - a) / (float) (b - a), 0, 1);
    }

    String rankText() {
        int ri = G.rankIndex();
        if (ri >= G.RANK_X.length - 1) return String.format("Звание: %s · опыт %d (высшее звание)", G.RANK_N[ri], G.xp());
        return String.format("Звание: %s · опыт %d / %d", G.RANK_N[ri], G.xp(), G.RANK_X[ri + 1]);
    }

    int questsReady() {
        int n = 0;
        for (G.Quest q : G.SV.qList) if (q.done && !q.cl) n++;
        return n;
    }

    void pMenu() {
        frame("menu", 440, true, false, () -> {
            d.text("ОТРЯД «ЩИТ» · ТАКТИЧЕСКИЙ ТЕРМИНАЛ", cx, cy + 11, 11, C_TEAL, Draw.WIDE, -1);
            cy += 20;
            d.text("ЩИТ НЕБА 3D", cx, cy + 38, 40, C_TXT, Draw.WIDE, -1);
            cy += 50;
            d.text("Оборона долины Ясная · 6+", cx, cy + 13, 13, C_MUT);
            cy += 22;
            d.text("Рекорд  " + G.SV.best, cx, cy + 14, 14, C_TXT);
            d.text("Монеты  " + G.SV.c, cx + cw, cy + 14, 14, C_ACC, Draw.BOLD, 1);
            cy += 22;
            d.text(rankText(), cx, cy + 13, 13, C_RANK, Draw.BOLD, -1);
            cy += 20;
            bar(cx, cy, cw, 6, rankK(), C_RANK);
            cy += 14;
            if (btn("Бесконечный бой", 52, BIG, true)) show("endless");
            if (btn(String.format("Кампания · глава %d   %d/%d", curChapter() + 1, allStars(), allMissions() * 3), 44, NORMAL, true)) openCampaign();
            if (!d.off) d.star(cx + cw - 22, cy - 8 - 22, 7, C_ACC, 1);
            int r2 = btnRow(new String[]{"", ""}, new int[]{NORMAL, NORMAL}, null, 44);
            if (!d.off) {
                float w2 = (cw - 6) / 2;
                d.text("НОВОЕ", cx + 22, cy - 8 - 44 + 17, 10, C_ACC, Draw.WIDE, -1);
                d.text("Рой: дрон-камикадзе", cx + 22, cy - 8 - 44 + 34, 14, C_TXT, Draw.BOLD, -1);
                d.text("ПО WI‑FI", cx + w2 + 6 + 22, cy - 8 - 44 + 17, 10, C_TEAL, Draw.WIDE, -1);
                d.text("Дуэль: ПВО — босс", cx + w2 + 6 + 22, cy - 8 - 44 + 34, 14, C_TXT, Draw.BOLD, -1);
            }
            if (r2 == 0) show("swarm");
            else if (r2 == 1) openDuel();
            int qn = questsReady();
            String[] names = {"Гараж", "Улучшения", qn > 0 ? "Задания (" + qn + ")" : "Задания", "Испытания", "Достижения", "Рекорды", "Настройки", "Об игре"};
            float gp = 6, w = (cw - gp * 3) / 4;
            for (int i = 0; i < names.length; i++) {
                float x = cx + (i % 4) * (w + gp), y = cy + (i / 4) * (38 + gp);
                if (button(x, y, w, 38, names[i], NORMAL, true, 14)) {
                    switch (i) {
                        case 0: garageTab = 0; show("garage"); break;
                        case 1: shopVeh = -1; backTo.put("shop", "menu"); show("shop"); break;
                        case 2: show("quests"); break;
                        case 3: show("chal"); break;
                        case 4: show("ach"); break;
                        case 5: show("records"); break;
                        case 6: show("settings"); break;
                        default: show("about"); break;
                    }
                }
                if (i == 2 && qn > 0 && !d.off) d.circle(x + w - 10, y + 9, 4, C_ACC, 0.6f + 0.4f * MathUtils.sin(T * 5));
            }
            cy += 2 * (38 + gp) + 4;
            d.text("v" + G.VER, cx + cw, cy + 4, 11, C_MUT, Draw.MAIN, 1);
            cy += 10;
        });
        // табличка машины справа
        if (!d.off) {
            String vn = G.vehName(G.SV.veh);
            float x = d.w - 24, y = d.h - 30;
            JsonValue V = G.D.get("VEHICLES").get(G.SV.veh);
            d.text("ВЫБРАННЫЙ КОМПЛЕКС", x, y - 46, 11, C_TEAL, Draw.WIDE, 1);
            d.text(vn.toUpperCase(), x + 1.5f, y - 18 + 1.5f, 26, BLACK, Draw.WIDE, 1);
            d.text(vn.toUpperCase(), x, y - 18, 26, C_TXT, Draw.WIDE, 1);
            float tw = Math.min(360, d.textW(V.getString("desc"), 13, Draw.MAIN));
            d.rect(x - tw, y - 13, tw, 1, C_TEAL, 0.4f);
            d.text(V.getString("desc"), x, y + 6, 13, C_MUT, Draw.MAIN, 1);
        }
    }

    void pEndless() {
        frame("endless", 460, false, true, () -> {
            title("Бесконечный бой");
            label("Улучшения из меню действуют с первой волны, а монеты за сбитых можно тратить на прокачку прямо в бою. 30% монет боя переходят в общий баланс.", 13, C_MUT, Draw.MAIN, 0);
            String[] keys = {"easy", "normal", "hard"};
            String[] nm = new String[3]; int[] st = new int[3];
            for (int i = 0; i < 3; i++) { nm[i] = G.D.get("DIFF").get(keys[i]).getString("name"); st[i] = G.SV.diff.equals(keys[i]) ? BIG : NORMAL; }
            int r = btnRow(nm, st, null, 40);
            if (r >= 0) { G.SV.diff = keys[r]; G.save(); }
            label(G.D.get("DIFF").get(G.SV.diff).getString("hint"), 12, C_MUT, Draw.MAIN, 0);
            if (toggle("Ночной бой", "прожектор, монеты ×1.25", G.SV.night)) { G.SV.night = !G.SV.night; G.save(); }
            d.text("Комплекс: " + G.vehName(G.SV.veh), cx, cy + 13, 13, C_TXT, Draw.BOLD, -1);
            d.text("сменить в гараже", cx + cw, cy + 13, 12, C_MUT, Draw.MAIN, 1);
            cy += 24;
            if (btn("В бой", 50, BIG, true)) launch("endless");
        });
    }

    boolean toggle(String t, String sub, boolean on) {
        float h = 40;
        boolean c = button(cx, cy, cw, h, "", NORMAL, true, 0);
        if (!d.off) {
            d.text(t, cx + 18, cy + (sub != null ? 18 : 25), 15, C_TXT, Draw.MAIN, -1);
            if (sub != null) d.text(sub, cx + 18, cy + 33, 11, C_MUT, Draw.MAIN, -1);
            float sx = cx + cw - 58, sy = cy + 11;
            d.rect(sx, sy, 40, 18, on ? C_TEAL : WHITE, on ? 0.35f : 0.1f);
            d.rect(on ? sx + 22 : sx + 2, sy + 2, 16, 14, on ? C_TEAL : C_MUT, 1);
        }
        cy += h + 8;
        return c;
    }

    void pGarage() {
        float w = Math.min(580, d.w * 0.56f);
        frame("garage", w, true, false, () -> {
            title("Гараж");
            d.text("Монеты: " + G.SV.c, cx + cw / 2, cy + 14, 14, C_ACC, Draw.BOLD, 0);
            cy += 24;
            garageTab = tabs(new String[]{"Машины", "Окраска", "Трассеры"}, garageTab);
            List<Card> cs = new ArrayList<>();
            if (garageTab == 0) {
                for (final String k : G.VEHS) {
                    JsonValue V = G.D.get("VEHICLES").get(k);
                    boolean own = G.SV.vown.contains(k), sel = G.SV.veh.equals(k);
                    final int cost = V.getInt("cost");
                    Card c = card(V.getString("name"), V.getString("desc"), own ? (sel ? "Выбрана" : "Выбрать") : "Купить · " + cost,
                            (own && !sel) || (!own && G.SV.c >= cost), () -> {
                                if (!G.SV.vown.contains(k)) { G.SV.c -= cost; G.SV.vown.add(k); G.sfx("coin"); }
                                G.SV.veh = k; G.save(); selectVeh(k);
                            });
                    c.sel = sel;
                    c.sub = vehRole(k);
                    c.subc = C_TEAL;
                    cs.add(c);
                }
            } else if (garageTab == 1) {
                for (JsonValue s = G.D.get("SK_VEH").child; s != null; s = s.next) {
                    final String id = s.name;
                    if (s.getBoolean("hidden", false) && !G.SV.skOwn.contains(id)) continue;
                    boolean own = G.SV.skOwn.contains(id), sel = G.SV.skVeh.equals(id);
                    final int cost = s.getInt("cost");
                    Card c = card(s.getString("name"), "", own ? (sel ? "Выбрана" : "Выбрать") : (cost == 0 ? "Получить" : "Купить · " + cost),
                            (own && !sel) || (!own && G.SV.c >= cost), () -> {
                                if (!G.SV.skOwn.contains(id)) { G.SV.c -= cost; G.SV.skOwn.add(id); G.sfx("coin"); }
                                G.SV.skVeh = id; G.save(); selectVeh(G.SV.veh);
                            });
                    c.sel = sel;
                    c.sw = Tex.camo(id);
                    cs.add(c);
                }
            } else {
                for (JsonValue s = G.D.get("SK_TR").child; s != null; s = s.next) {
                    final String id = s.name;
                    boolean own = G.SV.skOwn.contains(id), sel = G.SV.skTr.equals(id);
                    final int cost = s.getInt("cost");
                    Card c = card(s.getString("name"), "", own ? (sel ? "Выбран" : "Выбрать") : "Купить · " + cost,
                            (own && !sel) || (!own && G.SV.c >= cost), () -> {
                                if (!G.SV.skOwn.contains(id)) { G.SV.c -= cost; G.SV.skOwn.add(id); G.sfx("coin"); }
                                G.SV.skTr = id; G.save();
                            });
                    c.sel = sel;
                    c.swc = G.hex(s.getString("col"));
                    cs.add(c);
                }
            }
            cards(cs, garageTab == 0 ? 2 : 3);
        });
    }

    static String vehRole(String k) {
        switch (k) {
            case "pantsir": return "Пушка + 4 ракеты";
            case "shilka": return "4 ствола · без ракет";
            case "tor": return "8 ракет · залповый пуск";
            case "luch": return "Лазер · без упреждения";
            default: return "Зенитные разрывы";
        }
    }

    void selectVeh(String k) {
        g.VEH = k;
        g.applyStats();
        g.setVehicleModel();
        g.missiles = (int) g.st("maxMis");
        g.syncNoses();
    }

    void pShop() {
        final boolean inRun = g.state.equals("shop");
        frame("shop", 920, false, true, () -> {
            title("Улучшения");
            final String V;
            final Map<String, Integer> up;
            if (inRun) { V = g.VEH; up = g.UP(); }
            else {
                List<String> own = new ArrayList<>();
                for (String v : G.VEHS) if (G.SV.vown.contains(v)) own.add(v);
                if (shopVeh < 0 || shopVeh >= own.size()) shopVeh = Math.max(0, own.indexOf(G.SV.veh));
                if (own.size() > 1) {
                    String[] nm = new String[own.size()];
                    for (int i = 0; i < nm.length; i++) nm[i] = G.vehName(own.get(i));
                    shopVeh = tabs(nm, shopVeh);
                }
                V = own.get(shopVeh);
                up = G.SV.upv.get(V);
            }
            final boolean runW = inRun && g.runWallet();
            int coins = runW ? g.RUN.c : G.SV.c;
            d.text("Монеты: " + coins + (runW ? "  (монеты этого боя)" : "  (общий баланс)"), cx + cw / 2, cy + 14, 14, C_ACC, Draw.BOLD, 0);
            cy += 24;
            section("Улучшения · " + G.vehName(V));
            List<Card> cs = new ArrayList<>();
            for (JsonValue kv = G.D.get("UPG_SET").get(V).child; kv != null; kv = kv.next) {
                final String k = kv.asString();
                JsonValue u = G.D.get("UPG").get(k);
                Integer lvI = up.get(k);
                final int l = lvI == null ? 0 : lvI, mx = u.getInt("max");
                boolean isMax = l >= mx;
                final int cost = isMax ? 0 : u.get("cost").getInt(l);
                String val = u.get("val").getString(Math.min(l, u.get("val").size - 1)) + (isMax ? "" : "  »  " + u.get("val").getString(l + 1));
                Card c = card(u.getString("name"), "", isMax ? "Максимум" : "Улучшить · " + cost, !isMax && coins >= cost, () -> buyUp(V, up, k, cost, inRun));
                c.sub = val; c.subc = C_TXT; c.lv = l; c.max = mx;
                cs.add(c);
            }
            cards(cs, 4);
            section("Способности");
            cs.clear();
            final Map<String, Boolean> ab = runW ? g.RUN.ab : G.SV.ab;
            for (JsonValue a = G.D.get("ABIL").child; a != null; a = a.next) {
                final String k = a.name;
                boolean own = Boolean.TRUE.equals(ab.get(k));
                final int cost = a.getInt("cost");
                cs.add(card(a.getString("name"), a.getString("desc"), own ? "Куплено" : "Купить · " + cost, !own && coins >= cost, () -> {
                    if (runW) g.RUN.c -= cost; else G.SV.c -= cost;
                    ab.put(k, true); G.sfx("coin"); G.save();
                }));
            }
            cards(cs, 4);
            if (inRun && btn("Продолжить бой", 48, BIG, true)) resume();
        });
    }

    void buyUp(String V, Map<String, Integer> up, String k, int cost, boolean inRun) {
        if (inRun && g.runWallet()) g.RUN.c -= cost; else G.SV.c -= cost;
        Integer o = up.get(k);
        up.put(k, (o == null ? 0 : o) + 1);
        G.sfx("coin"); G.save();
        if (inRun) {
            float om = g.st("maxHp"); int oms = (int) g.st("maxMis");
            g.applyStats();
            g.hp += g.st("maxHp") - om;
            g.missiles = Math.min((int) g.st("maxMis"), g.missiles + Math.max(0, (int) g.st("maxMis") - oms));
            g.syncNoses();
            if (k.equals("mog")) {
                g.setupMogs();
                String[] msg = {"", "МОГ занял позицию рядом с постом", "МОГ усилен", "Второй МОГ занял позицию"};
                toast(msg[Math.min(3, up.get(k))]);
            }
        } else if (V.equals(g.VEH)) { g.applyStats(); }
    }

    void pQuests() {
        frame("quests", 580, false, true, () -> {
            title("Задания дня");
            Calendar now = Calendar.getInstance();
            int left = (23 - now.get(Calendar.HOUR_OF_DAY)) * 60 + (59 - now.get(Calendar.MINUTE));
            label(String.format("Новые задания через %d ч %02d мин · прогресс идёт в любом бою", left / 60, left % 60), 13, C_MUT, Draw.MAIN, 0);
            for (final G.Quest q : G.SV.qList) {
                float h = 66;
                boolean ready = q.done && !q.cl;
                if (!d.off) {
                    box(cx, cy, cw, h, ready ? C_ACC : WHITE, ready ? 0.08f : 0.05f, ready ? C_ACC : C_LINE, ready ? 1 : 0.22f, ready ? 2 : 1);
                    d.text(G.D.get("QDEF").get(q.id).get("t").getString(q.lv), cx + 14, cy + 24, 16, C_TXT, Draw.TITLE, -1);
                    bar(cx + 14, cy + 34, cw - 190, 8, q.p / q.n, q.done ? C_GREEN : C_ACC);
                    d.text(String.format("%d / %d", (int) q.p, (int) q.n), cx + 14, cy + 58, 12, C_MUT);
                }
                String bt = q.cl ? "Получено" : (q.done ? "Забрать +" + (int) q.r : "+" + (int) q.r);
                if (button(cx + cw - 164, cy + 13, 150, 40, bt, BUY, ready, 0)) {
                    q.cl = true; G.SV.c += (int) q.r; G.sfx("coin"); G.save(); toast("+" + (int) q.r + " монет");
                }
                if (q.cl && !d.off) d.check(cx + cw - 150, cy + 33, 6, 2.5f, C_GREEN, 1);
                cy += h + 8;
            }
        });
    }

    static boolean achHidden(String st) { return st.equals("coop") || st.equals("daily"); }

    void pAch() {
        frame("ach", 920, false, true, () -> {
            title("Достижения");
            List<JsonValue> list = new ArrayList<>();
            int got = 0;
            for (JsonValue a = G.D.get("ACH").child; a != null; a = a.next) {
                if (achHidden(a.getString("st"))) continue;
                list.add(a);
                if (G.SV.ach.contains(a.getString("id"))) got++;
            }
            label(String.format("Открыто %d из %d · награда начисляется сразу", got, list.size()), 13, C_MUT, Draw.MAIN, 0);
            int cols = Math.max(1, Math.min(3, (int) (cw / 260)));
            float gp = 8, w = (cw - gp * (cols - 1)) / cols;
            for (int i0 = 0; i0 < list.size(); i0 += cols) {
                float rowH = 0;
                boolean was = d.off; d.off = true;
                for (int i = i0; i < Math.min(list.size(), i0 + cols); i++) rowH = Math.max(rowH, 64 + d.wrap(list.get(i).getString("desc"), 0, 0, w - 24, 12, WHITE, Draw.MAIN, -1));
                d.off = was;
                for (int i = i0; i < Math.min(list.size(), i0 + cols); i++) {
                    JsonValue a = list.get(i);
                    float x = cx + (i - i0) * (w + gp), y = cy;
                    boolean done = G.SV.ach.contains(a.getString("id"));
                    float n = a.getFloat("n"), val = Math.min(n, g.statVal(a.getString("st")));
                    if (!d.off) {
                        box(x, y, w, rowH, done ? C_ACC : WHITE, done ? 0.1f : 0.05f, done ? C_ACC : C_LINE, done ? 0.8f : 0.22f, 1);
                        d.text(a.getString("name"), x + 12, y + 20, 15, C_TXT, Draw.TITLE, -1);
                        d.star(x + w - 18, y + 14, 8, done ? C_ACC : WHITE, done ? 1 : 0.15f);
                    }
                    float hh = d.wrap(a.getString("desc"), x + 12, y + 28, w - 24, 12, C_MUT, Draw.MAIN, -1);
                    if (!d.off) {
                        bar(x + 12, y + 30 + hh, w - 24, 6, val / n, done ? C_GREEN : C_ACC);
                        d.text(done ? "Получено · +" + a.getInt("r") : String.format("%d / %d · награда %d", (int) val, (int) n, a.getInt("r")), x + 12, y + 30 + hh + 22, 12, done ? C_ACC : C_MUT);
                    }
                }
                cy += rowH + gp;
            }
        });
    }

    void pRecords() {
        frame("records", 660, false, true, () -> {
            title("Рекорды");
            label(String.format("Всего сбито: %d   ·   Боссов: %d   ·   Лучшая волна: %d   ·   Ракет: %d", (int) g.statVal("kills"), (int) g.statVal("bosses"),
                    (int) g.statVal("bestWave"), (int) g.statVal("missiles")), 13, C_MUT, Draw.MAIN, 0);
            if (G.SV.top.isEmpty()) {
                gap(10);
                label("Пока пусто — сыграй бесконечный бой, и лучший результат появится здесь.", 15, C_TXT, Draw.MAIN, 0);
                gap(10);
                return;
            }
            float[] col = {0, 0.08f, 0.28f, 0.44f, 0.68f, 0.88f};
            String[] hd = {"#", "Очки", "Волна", "Машина", "Сложность", "Дата"};
            for (int i = 0; i < hd.length; i++) d.text(hd[i], cx + 8 + col[i] * cw, cy + 12, 12, C_MUT);
            cy += 20;
            for (int i = 0; i < G.SV.top.size(); i++) {
                G.Top r = G.SV.top.get(i);
                Color c = i == 0 ? C_ACC : C_TXT;
                if (i % 2 == 0) d.rect(cx, cy, cw, 26, WHITE, 0.03f);
                if (i == 0) d.rect(cx, cy, 3, 26, C_ACC, 1);
                JsonValue dd = G.D.get("DIFF").get(r.d);
                String dt = r.t.length() >= 10 ? r.t.substring(8, 10) + "." + r.t.substring(5, 7) : r.t;
                String[] vals = {String.valueOf(i + 1), String.valueOf(r.s), String.valueOf(r.w), G.vehName(r.v), dd == null ? "" : dd.getString("name"), dt};
                for (int k = 0; k < vals.length; k++) d.text(vals[k], cx + 8 + col[k] * cw, cy + 18, 15, c, i == 0 ? Draw.TITLE : Draw.MAIN, -1);
                cy += 28;
            }
            gap(6);
        });
    }

    float slider(String t, float v, float a, float b, String disp) {
        d.text(t, cx, cy + 14, 14, C_TXT);
        d.text(disp, cx + cw, cy + 14, 13, C_TEAL, Draw.BOLD, 1);
        cy += 22;
        float x = cx + 8, w = cw - 16, y = cy + 10;
        float k = (v - a) / (b - a);
        if (!d.off) {
            d.rect(x, y - 2, w, 4, C_TEAL, 0.14f);
            d.rect(x, y - 2, w * k, 4, C_TEAL, 1);
            d.rect(x + w * k - 6, y - 9, 12, 18, C_TEAL, 1);
            d.rect(x + w * k - 2, y - 5, 4, 10, C_PANEL, 0.6f);
            if (down && in(downX, downY, x - 10, y - 14, w + 20, 28) && inView(downY)) {
                capture = true;
                float nk = MathUtils.clamp((curX - x) / w, 0, 1);
                v = a + nk * (b - a);
            }
        }
        cy += 30;
        return v;
    }

    void pSettings() {
        frame("settings", 500, false, true, () -> {
            title("Настройки");
            float ns = slider("Чувствительность прицела", G.SV.sens, 0.4f, 2f, String.format("×%.2f", G.SV.sens));
            if (Math.abs(ns - G.SV.sens) > 1e-4f) { G.SV.sens = Math.round(ns * 20) / 20f; }
            float nv = slider("Громкость", G.SV.vol, 0, 1, Math.round(G.SV.vol * 100) + "%");
            if (Math.abs(nv - G.SV.vol) > 1e-4f) G.SV.vol = Math.round(nv * 20) / 20f;
            d.text("Графика", cx, cy + 14, 14, C_TXT);
            cy += 22;
            String[] q = {"Экономная", "Средняя", "Высокая"};
            int[] st = new int[3];
            for (int i = 0; i < 3; i++) st[i] = G.SV.gfx == i ? BIG : NORMAL;
            int r = btnRow(q, st, null, 38);
            if (r >= 0 && r != G.SV.gfx) { G.SV.gfx = r; G.save(); host.applyGfx(); }
            if (toggle("Вибрация", null, G.SV.vib)) { G.SV.vib = !G.SV.vib; G.save(); }
            if (DESK && toggle("Полный экран", null, host.isFullscreen())) host.toggleFullscreen();
            if (DESK) label("Мышь — наведение · ЛКМ или пробел — огонь · ПКМ или E — ракета · 1–4 — способности · B — улучшения · Esc — пауза · Tab — отпустить курсор · F11 — полный экран", 12, C_MUT);
            if (btn("Обучение заново", 38, NORMAL, true)) { G.SV.tut = false; G.save(); toast("Обучение покажется в следующем бою"); }
        });
        if (!down && capture) { capture = false; G.save(); }
    }

    void pAbout() {
        frame("about", 640, false, true, () -> {
            title("Об игре");
            label("Щит неба 3D · версия " + G.VER + " · 6+", 16, C_TXT, Draw.TITLE, -1);
            label("Аркада про противовоздушную оборону. Все организации, персонажи, искусственные интеллекты и боевые машины вымышлены.", 14, C_TXT);
            label("Конфиденциальность: игра не собирает и не передаёт персональные данные. Прогресс хранится только на устройстве.", 14, C_TXT);
            label("Движок libGDX (лицензия Apache 2.0). Шрифты Russo One и Exo 2 — SIL Open Font License 1.1.", 14, C_MUT);
        });
    }

    void pPause() {
        frame("pause", 400, false, true, () -> {
            title("Пауза");
            if (g.mode.equals("endless")) {
                d.text(String.format("Волна %d  ·  очки %d  ·  сбито %d", Math.max(1, g.wave), g.score, g.kills), cx + cw / 2, cy + 13, 13, C_MUT, Draw.MAIN, 0);
                cy += 24;
            }
            if (btn("Продолжить", 50, BIG, true)) resume();
            if (!g.mode.equals("duel") && btn("Улучшения", 42, NORMAL, true)) openShopBattle();
            if (btn("Настройки", 42, NORMAL, true)) show("settings");
            if (btn("Выйти из боя", 42, NORMAL, true)) { if (duel != null && g.mode.equals("duel")) duel.leave(); toMenu(); }
        });
    }

    void pOver() {
        frame("over", 480, false, true, () -> {
            title("Комплекс подбит");
            if (overRec) {
                float k = 0.75f + 0.25f * MathUtils.sin(T * 4);
                d.text("НОВЫЙ РЕКОРД!", cx + cw / 2, cy + 18, 20, new Color(1, 0.7f * k + 0.1f, 0.1f, 1), Draw.WIDE, 0);
                cy += 30;
            }
            String[][] cells = {{"Очки", String.valueOf(g.score)}, {"Волна", String.valueOf(g.wave)}, {"Сбито", String.valueOf(g.kills)}, {"В копилку", "+" + Math.round(g.runCoins * 0.3f)}};
            float gp = 6, w = (cw - gp * 3) / 4;
            for (int i = 0; i < 4; i++) {
                float x = cx + i * (w + gp);
                if (!d.off) {
                    box(x, cy, w, 58, WHITE, 0.05f, C_LINE, 0.22f, 1);
                    d.text(cells[i][0], x + w / 2, cy + 18, 12, C_MUT, Draw.MAIN, 0);
                    d.text(cells[i][1], x + w / 2, cy + 46, 22, i == 3 ? C_ACC : C_TXT, Draw.TITLE, 0);
                }
            }
            cy += 66;
            if (g.xpGot > 0) {
                d.text("Опыт +" + g.xpGot, cx, cy + 13, 13, C_RANK, Draw.BOLD, -1);
                d.text(G.RANK_N[G.rankIndex()], cx + cw, cy + 13, 13, C_RANK, Draw.MAIN, 1);
                cy += 20;
                bar(cx, cy, cw, 6, rankK(), C_RANK);
                cy += 14;
                if (g.rankUp >= 0) {
                    label(String.format("Новое звание: %s!  +%d монет", G.RANK_N[g.rankUp], 100 * g.rankUp), 18, C_ACC, Draw.TITLE, 0);
                }
            }
            int r = btnRow(new String[]{"Ещё раз", "Меню"}, new int[]{BIG, NORMAL}, null, 48);
            if (r == 0) { g.rankUp = -1; clearComm(); g.quitBattle(); launch("endless"); }
            else if (r == 1) { g.rankUp = -1; toMenu(); }
        });
    }

    int dailyDay() {
        int st = G.SV.dailyStreak;
        if (!G.SV.dailyLast.equals(G.today(-1))) st = 0;
        return st % G.D.get("DAILY").size + 1;
    }

    void pDaily() {
        frame("daily", 620, false, true, () -> {
            title("Ежедневная награда");
            label("Заходи каждый день подряд — награда растёт. Пропустишь день — счёт начнётся заново.", 13, C_MUT, Draw.MAIN, 0);
            final int day = dailyDay();
            JsonValue D = G.D.get("DAILY");
            int n = D.size;
            float gp = 6, w = (cw - gp * (n - 1)) / n, h = 84;
            for (int i = 0; i < n; i++) {
                float x = cx + i * (w + gp);
                boolean cur = i + 1 == day, past = i + 1 < day;
                if (!d.off) {
                    float pulse = cur ? 0.1f + 0.05f * MathUtils.sin(T * 4) : 0.04f;
                    box(x, cy, w, h, cur ? C_ACC : WHITE, pulse, cur ? C_ACC : C_LINE, cur ? 1 : 0.22f, cur ? 2 : 1);
                    d.text("День " + (i + 1), x + w / 2, cy + 26, 12, C_MUT, Draw.MAIN, 0);
                    if (past) d.check(x + w / 2, cy + 52, 9, 3, C_GREEN, 1);
                    else d.text("+" + D.getInt(i), x + w / 2, cy + 60, 18, cur ? C_ACC : new Color(1, 1, 1, 0.6f), Draw.TITLE, 0);
                }
            }
            cy += h + 12;
            final int rw = D.getInt(day - 1);
            if (btn("Забрать +" + rw + " монет", 50, BIG, true)) {
                G.SV.dailyLast = G.today(); G.SV.dailyStreak = day; G.SV.c += rw; G.addXp(10); G.save(); G.sfx("clear");
                toast("+" + rw + " монет · день " + day);
                show("menu");
            }
        });
    }

    // =========================================================
    //                 КАМПАНИЯ И ИСПЫТАНИЯ
    // =========================================================
    JsonValue curMission;
    int cutCh;
    JsonValue cutThen;
    final HashMap<Integer, Boolean> chOpen = new HashMap<>();

    static JsonValue chapters() { return G.D.get("CHAPTERS"); }

    static int campOf(String id) { Integer v = G.SV.camp.get(id); return v == null ? 0 : v; }

    int chStars(int ci) {
        int n = 0;
        for (JsonValue m = chapters().get(ci).get("missions").child; m != null; m = m.next) n += campOf(m.getString("id"));
        return n;
    }

    int allStars() { int n = 0; for (int i = 0; i < chapters().size; i++) n += chStars(i); return n; }

    int allMissions() { int n = 0; for (JsonValue c = chapters().child; c != null; c = c.next) n += c.get("missions").size; return n; }

    int curChapter() {
        for (int ci = 0; ci < chapters().size; ci++)
            for (JsonValue m = chapters().get(ci).get("missions").child; m != null; m = m.next)
                if (campOf(m.getString("id")) == 0) return ci;
        return chapters().size - 1;
    }

    int[] chapterOf(JsonValue m) {
        String id = m.getString("id");
        for (int ci = 0; ci < chapters().size; ci++) {
            JsonValue ms = chapters().get(ci).get("missions");
            for (int mi = 0; mi < ms.size; mi++) if (ms.get(mi).getString("id").equals(id)) return new int[]{ci, mi};
        }
        return new int[]{-1, -1};
    }

    boolean unlocked(int ci, int mi) {
        if (mi == 0) {
            if (ci == 0) return true;
            JsonValue pm = chapters().get(ci - 1).get("missions");
            return campOf(pm.get(pm.size - 1).getString("id")) > 0;
        }
        return campOf(chapters().get(ci).get("missions").get(mi - 1).getString("id")) > 0;
    }

    JsonValue nextMission(JsonValue m) {
        int[] p = chapterOf(m);
        if (p[0] < 0) return null;
        JsonValue ms = chapters().get(p[0]).get("missions");
        if (p[1] + 1 < ms.size) return ms.get(p[1] + 1);
        if (p[0] + 1 < chapters().size) return chapters().get(p[0] + 1).get("missions").get(0);
        return null;
    }

    /** Отладка: найти миссию или испытание по id. */
    public JsonValue findMission(String id) {
        for (JsonValue c = chapters().child; c != null; c = c.next)
            for (JsonValue m = c.get("missions").child; m != null; m = m.next) if (m.getString("id").equals(id)) return m;
        for (JsonValue m = G.D.get("CHALLENGES").child; m != null; m = m.next) if (m.getString("id").equals(id)) return m;
        return null;
    }

    public void debugBrief(String id) { openBrief(findMission(id)); }

    public void debugLaunch(String id) { launchMission(findMission(id)); }

    public void debugDuelHost() { duelView = "host"; hostWith("pvo", "pantsir"); show("duel"); }

    public void debugDuelJoin(String ip) { duelView = "join"; show("duel"); duel.join(ip); }

    public void debugCut(int ch) { cutCh = ch; cutThen = null; show("cut"); }

    void openCampaign() {
        if (!G.SV.cuts.contains("1")) { cutCh = 0; cutThen = null; show("cut"); return; }
        show("campaign");
    }

    void openMission(JsonValue m) {
        int[] p = chapterOf(m);
        if (p[0] >= 0 && !G.SV.cuts.contains(String.valueOf(p[0] + 1))) { cutCh = p[0]; cutThen = m; show("cut"); return; }
        openBrief(m);
    }

    void openBrief(JsonValue m) {
        curMission = m;
        boolean chal = "chal".equals(m.getString("special", ""));
        backTo.put("brief", chal ? "chal" : "campaign");
        show("brief");
    }

    void launchMission(final JsonValue m) {
        pending = () -> { show(""); g.startGame("campaign", m); };
        pendingFrames = 1;
    }

    void stars(float x, float cyy, int n, int total, float r) {
        for (int i = 0; i < total; i++) d.star(x + r + i * (r * 2 + 4), cyy, r, i < n ? C_ACC : WHITE, i < n ? 1 : 0.14f);
    }

    float dialogLine(String who, String text, float x, float y, float w) {
        JsonValue c = G.D.get("CH").get(who);
        String name = c != null ? c.getString("name") : who, ava = c != null ? c.getString("ava") : "?";
        Color col = c != null ? C(c.getString("col")) : WHITE;
        boolean was = d.off; d.off = true;
        float th = d.wrap(text, 0, 0, w - 70, 14, WHITE, Draw.MAIN, -1);
        d.off = was;
        float h = Math.max(58, 30 + th + 6);
        if (!d.off) {
            box(x, y, w, h, WHITE, 0.05f, C_LINE, 0.22f, 1);
            d.rect(x + 12, y + 10, 38, 38, col, 1);
            d.text(ava, x + 31, y + 36, 18, C("0b1220"), Draw.TITLE, 0);
            d.text(name, x + 62, y + 22, 12, col, Draw.BOLD, -1);
        }
        d.wrap(text, x + 62, y + 26, w - 74, 14, C_TXT, Draw.MAIN, -1);
        return h;
    }

    static String typeName(JsonValue m) {
        String t = m.getString("type", "waves");
        if (t.equals("timer")) return "Удержание · " + m.getInt("time", 0) + " с";
        if (t.equals("escort")) return "Сопровождение · " + m.getInt("time", 0) + " с";
        int n = m.has("waves") ? m.get("waves").size : 0;
        return n + (n == 1 ? " волна" : (n < 5 ? " волны" : " волн"));
    }

    void pCampaign() {
        frame("campaign", 920, false, true, () -> {
            title("Кампания");
            int all = allStars(), mx = allMissions() * 3;
            d.text(String.format("Звёзды: %d / %d", all, mx), cx + cw / 2, cy + 13, 13, C_ACC, Draw.BOLD, 0);
            cy += 20;
            bar(cx + cw * 0.3f, cy, cw * 0.4f, 4, all / (float) mx, C_ACC);
            cy += 14;
            int cur = curChapter();
            for (int ci = 0; ci < chapters().size; ci++) {
                JsonValue ch = chapters().get(ci);
                boolean open = unlocked(ci, 0);
                JsonValue ms = ch.get("missions");
                boolean expanded = open && (chOpen.containsKey(ci) ? chOpen.get(ci) : ci == cur);
                float hh = 40;
                if (!d.off) {
                    box(cx, cy, cw, hh, open ? C_TEAL : WHITE, open ? (expanded ? 0.1f : 0.05f) : 0.02f, C_LINE, open ? 0.3f : 0.1f, 1);
                    if (open) d.rect(cx, cy, 3, hh, ci == cur ? C_ACC : C_TEAL, 1);
                    d.text(ch.getString("name"), cx + 16, cy + 26, 17, open ? C_TXT : new Color(1, 1, 1, 0.35f), Draw.TITLE, -1);
                    if (open) {
                        int cs = chStars(ci);
                        d.text(cs + "/" + ms.size * 3, cx + cw - 46, cy + 26, 14, C_ACC, Draw.BOLD, 1);
                        d.star(cx + cw - 32, cy + 20, 7, C_ACC, 1);
                        float ax = cx + cw - 14, ay = cy + 20;
                        if (expanded) { d.line(ax - 5, ay - 2, ax, ay + 3, 2, C_MUT, 1); d.line(ax, ay + 3, ax + 5, ay - 2, 2, C_MUT, 1); }
                        else { d.line(ax - 2, ay - 5, ax + 3, ay, 2, C_MUT, 1); d.line(ax + 3, ay, ax - 2, ay + 5, 2, C_MUT, 1); }
                    } else d.text("откроется после главы " + ci, cx + cw - 14, cy + 25, 13, C_MUT, Draw.MAIN, 1);
                }
                if (open && tap(cx, cy, cw, hh)) { chOpen.put(ci, !expanded); G.sfx("click"); }
                cy += hh + 8;
                if (!expanded) continue;
                int cols = Math.max(1, Math.min(3, (int) (cw / 250)));
                float gp = 8, w = (cw - gp * (cols - 1)) / cols;
                for (int i0 = 0; i0 < ms.size; i0 += cols) {
                    float rowH = 0;
                    boolean was = d.off; d.off = true;
                    for (int i = i0; i < Math.min(ms.size, i0 + cols); i++) rowH = Math.max(rowH, 112 + d.wrap(ms.get(i).getString("goal"), 0, 0, w - 24, 12, WHITE, Draw.MAIN, -1));
                    d.off = was;
                    for (int i = i0; i < Math.min(ms.size, i0 + cols); i++) {
                        final JsonValue m = ms.get(i);
                        float x = cx + (i - i0) * (w + gp), y = cy;
                        boolean op = unlocked(ci, i);
                        int got = campOf(m.getString("id"));
                        boolean next = op && got == 0;
                        if (!d.off) {
                            box(x, y, w, rowH, next ? C_ACC : WHITE, op ? (next ? 0.06f : 0.06f) : 0.025f, next ? C_ACC : C_LINE, next ? 1 : 0.22f, next ? 2 : 1);
                            d.text(m.getString("id") + "  ·  " + typeName(m), x + 12, y + 20, 12, C_MUT);
                            stars(x + w - 12 - 3 * 20, y + 15, got, 3, 8);
                            d.text(m.getString("title"), x + 12, y + 44, 16, op ? C_TXT : new Color(1, 1, 1, 0.4f), Draw.TITLE, -1);
                        }
                        d.wrap(m.getString("goal"), x + 12, y + 52, w - 24, 12, C_MUT, Draw.MAIN, -1);
                        if (button(x + 12, y + rowH - 12 - 34, w - 24, 34, op ? (got == 0 ? "Начать" : "Переиграть") : "Закрыто", BUY, op, 14)) openMission(m);
                    }
                    cy += rowH + gp;
                }
                gap(4);
            }
        });
    }

    void pCut() {
        frame("cut", 660, false, true, () -> {
            JsonValue cut = G.D.get("CUTS").get(String.valueOf(cutCh + 1));
            d.text("ГЛАВА " + (cutCh + 1), cx + cw / 2, cy + 14, 14, C_ACC, Draw.WIDE, 0);
            cy += 22;
            title(cut.getString("title"));
            for (JsonValue l = cut.get("lines").child; l != null; l = l.next) cy += dialogLine(l.getString(0), l.getString(1), cx, cy, cw) + 8;
            if (btn("Далее", 48, BIG, true)) {
                String k = String.valueOf(cutCh + 1);
                if (!G.SV.cuts.contains(k)) { G.SV.cuts.add(k); G.save(); }
                if (cutThen != null) openBrief(cutThen); else show("campaign");
            }
        });
    }

    void pBrief() {
        final JsonValue m = curMission;
        if (m == null) { show("campaign"); return; }
        frame("brief", 660, false, true, () -> {
            boolean chal = "chal".equals(m.getString("special", ""));
            d.text(chal ? "ИСПЫТАНИЕ" : "МИССИЯ " + m.getString("id"), cx + cw / 2, cy + 14, 14, C_ACC, Draw.WIDE, 0);
            cy += 22;
            title(m.getString("title"));
            label("Задача: " + m.getString("goal"), 15, C_TXT, Draw.BOLD, 0);
            d.text(typeName(m), cx + cw / 2, cy + 11, 12, C_MUT, Draw.MAIN, 0);
            cy += 20;
            if (m.has("brief")) for (JsonValue l = m.get("brief").child; l != null; l = l.next) cy += dialogLine(l.getString(0), l.getString(1), cx, cy, cw) + 8;
            if (m.has("mods")) for (JsonValue md = m.get("mods").child; md != null; md = md.next) {
                JsonValue M = G.D.get("MODS").get(md.asString());
                if (M != null) label("•  " + M.getString("name") + ": " + M.getString("desc"), 14, C("ffb0a0"));
            }
            Integer pvI = chal ? G.SV.chal.get(m.getString("id")) : G.SV.camp.get(m.getString("id"));
            int prev = pvI == null ? 0 : pvI;
            section("Звёзды");
            JsonValue st = m.get("stars");
            for (int i = 0; i < st.size; i++) {
                d.star(cx + 10, cy + 9, 8, i < prev ? C_ACC : WHITE, i < prev ? 1 : 0.14f);
                d.text(st.get(i).getString("t"), cx + 28, cy + 14, 14, C_TXT);
                cy += 24;
            }
            gap(2);
            int rw = m.getInt("reward");
            label(String.format("Награда: %d монет%s", prev == 0 ? rw : Math.round(rw * 0.25f), prev == 0 ? "" : (chal ? " (повторное прохождение)" : " (за новые звёзды — бонус)")), 13, C_ACC, Draw.BOLD, -1);
            if (btn("В бой", 50, BIG, true)) launchMission(m);
        });
    }

    void pResult() {
        final JsonValue m = g.resM;
        if (m == null) { show("menu"); return; }
        frame("result", 560, false, true, () -> {
            boolean chal = "chal".equals(m.getString("special", ""));
            boolean win = g.resWin;
            title(chal ? (win ? "Испытание пройдено" : "Испытание провалено") : (win ? "Миссия выполнена" : "Миссия провалена"));
            d.text(chal ? m.getString("title") : m.getString("id") + " · " + m.getString("title"), cx + cw / 2, cy + 14, 14, C_MUT, Draw.MAIN, 0);
            cy += 24;
            if (win) {
                float k = Math.min(1, panelT / 0.9f);
                for (int i = 0; i < 3; i++) {
                    float appear = MathUtils.clamp((k * 3 - i), 0, 1);
                    boolean on = i < g.resGot;
                    float r = 22 * (on ? (0.6f + 0.4f * Interp(appear)) : 1);
                    d.star(cx + cw / 2 + (i - 1) * 56, cy + 24, r, on && appear > 0 ? C_ACC : WHITE, on && appear > 0 ? 1 : 0.14f);
                }
                cy += 56;
                JsonValue st = m.get("stars");
                for (int i = 0; i < st.size; i++) {
                    boolean ok = g.resOk != null && i < g.resOk.length && g.resOk[i];
                    d.star(cx + 10, cy + 9, 8, ok ? C_ACC : WHITE, ok ? 1 : 0.14f);
                    d.text(st.get(i).getString("t"), cx + 28, cy + 14, 14, new Color(C_TXT.r, C_TXT.g, C_TXT.b, ok ? 1 : 0.5f));
                    cy += 24;
                }
                gap(4);
                label(g.resReward > 0 ? "Награда: +" + g.resReward + " монет" : "Награда за миссию уже получена", 16, C_ACC, Draw.TITLE, 0);
                if (g.xpGot > 0) {
                    d.text("Опыт +" + g.xpGot, cx, cy + 13, 13, C_RANK, Draw.BOLD, -1);
                    d.text(G.RANK_N[G.rankIndex()], cx + cw, cy + 13, 13, C_RANK, Draw.MAIN, 1);
                    cy += 20; bar(cx, cy, cw, 6, rankK(), C_RANK); cy += 14;
                    if (g.rankUp >= 0) label(String.format("Новое звание: %s!  +%d монет", G.RANK_N[g.rankUp], 100 * g.rankUp), 18, C_ACC, Draw.TITLE, 0);
                }
                if (m.has("debrief")) for (JsonValue l = m.get("debrief").child; l != null; l = l.next) cy += dialogLine(l.getString(0), l.getString(1), cx, cy, cw) + 8;
                if (!chal) {
                    final JsonValue nxt = nextMission(m);
                    if (nxt == null) label("Кампания пройдена! Небо над долиной чистое. Спасибо, командир!", 14, C_TXT, Draw.MAIN, 0);
                    else if (btn("Следующая: " + nxt.getString("title"), 48, BIG, true)) {
                        g.rankUp = -1; clearComm(); g.quitBattle();
                        pending = () -> { host.toMenuWorld(); openMission(nxt); };
                        pendingFrames = 1;
                    }
                }
            } else {
                label(g.resReason, 16, C("ff8a7a"), Draw.BOLD, 0);
                label(g.hasMod("noUpg") ? "Здесь улучшения не работают — только точность и способности." : "Совет: загляни в «Улучшения» — прокачка из меню действует и здесь.", 13, C_MUT, Draw.MAIN, 0);
            }
            int r = btnRow(new String[]{"Повторить", chal ? "К испытаниям" : "К миссиям"}, new int[]{win ? NORMAL : BIG, NORMAL}, null, 44);
            if (r == 0) { g.rankUp = -1; clearComm(); g.quitBattle(); launchMission(m); }
            else if (r == 1) { g.rankUp = -1; toMenu(); }
        });
    }

    void pChal() {
        frame("chal", 920, false, true, () -> {
            title("Испытания");
            JsonValue list = G.D.get("CHALLENGES");
            int done = 0;
            for (JsonValue c = list.child; c != null; c = c.next) { Integer v = G.SV.chal.get(c.getString("id")); if (v != null && v > 0) done++; }
            label(String.format("Особые бои с жёсткими условиями · пройдено %d из %d", done, list.size), 13, C_MUT, Draw.MAIN, 0);
            int cols = Math.max(1, Math.min(3, (int) (cw / 260)));
            float gp = 8, w = (cw - gp * (cols - 1)) / cols;
            for (int i0 = 0; i0 < list.size; i0 += cols) {
                float rowH = 0;
                boolean was = d.off; d.off = true;
                for (int i = i0; i < Math.min(list.size, i0 + cols); i++) {
                    JsonValue c = list.get(i);
                    float mh = 0;
                    if (c.has("mods")) for (JsonValue md = c.get("mods").child; md != null; md = md.next) mh += 16;
                    rowH = Math.max(rowH, 116 + mh + d.wrap(c.getString("goal"), 0, 0, w - 24, 12, WHITE, Draw.MAIN, -1));
                }
                d.off = was;
                for (int i = i0; i < Math.min(list.size, i0 + cols); i++) {
                    final JsonValue c = list.get(i);
                    Integer gv = G.SV.chal.get(c.getString("id"));
                    int got = gv == null ? 0 : gv;
                    float x = cx + (i - i0) * (w + gp), y = cy;
                    if (!d.off) {
                        box(x, y, w, rowH, got > 0 ? C_GREEN : WHITE, got > 0 ? 0.05f : 0.06f, got > 0 ? C_GREEN : C_LINE, got > 0 ? 0.5f : 0.22f, 1);
                        d.text(c.getString("title"), x + 12, y + 22, 16, C_TXT, Draw.TITLE, -1);
                        stars(x + w - 12 - 3 * 20, y + 15, got, 3, 8);
                    }
                    float yy = y + 30;
                    yy += d.wrap(c.getString("goal"), x + 12, yy, w - 24, 12, C_MUT, Draw.MAIN, -1);
                    if (c.has("mods")) for (JsonValue md = c.get("mods").child; md != null; md = md.next) {
                        JsonValue M = G.D.get("MODS").get(md.asString());
                        d.text("• " + (M != null ? M.getString("name") : md.asString()), x + 12, yy + 12, 12, C("ffb0a0"));
                        yy += 16;
                    }
                    int rw = c.getInt("reward");
                    d.text("Награда: " + (got == 0 ? rw : Math.round(rw * 0.25f)), x + 12, yy + 14, 12, C_ACC, Draw.BOLD, -1);
                    if (button(x + 12, y + rowH - 12 - 34, w - 24, 34, got == 0 ? "Начать" : "Переиграть", BUY, true, 14)) openBrief(c);
                }
                cy += rowH + gp;
            }
        });
    }

    void drawToast() {
        if (toastT <= 0 || toastS == null) return;
        float a = MathUtils.clamp(toastT / 0.25f, 0, 1) * MathUtils.clamp((2.3f - toastT) / 0.15f, 0, 1);
        float tw = d.textW(toastS, 14, Draw.TITLE) + 32;
        float x = (d.w - tw) / 2, y = 64;
        d.rect(x, y, tw, 40, new Color(0.04f, 0.07f, 0.12f, 1), 0.92f * a);
        d.frame(x, y, tw, 40, 1, new Color(1, 0.71f, 0.28f, 1), 0.5f * a);
        d.rect(x, y, 3, 40, C_ACC, a);
        d.text(toastS, d.w / 2, y + 25, 14, new Color(1, 1, 1, a), Draw.TITLE, 0);
    }

    // =========================================================
    //                          HUD
    // =========================================================
    final Vector3 tv = new Vector3(), tv2 = new Vector3(), right = new Vector3(), up = new Vector3();
    final Vector2 sp = new Vector2(), sp2 = new Vector2();

    boolean proj(Vector3 p, Vector2 out) {
        PerspectiveCamera cam = g.cam;
        tv.set(p).sub(cam.position);
        if (tv.dot(cam.direction) <= 0.05f) return false;
        tv.set(p);
        cam.project(tv);
        out.x = tv.x / d.s;
        out.y = (Gdx.graphics.getHeight() - tv.y) / d.s;
        return true;
    }

    float sx() { return d.w; }

    float sy() { return d.h; }

    float[] fireC() { return new float[]{sx() - 22 - 59, sy() - 22 - 59}; }

    float[] misC() { return new float[]{sx() - 22 - 128 - 42, sy() - 40 - 42}; }

    float[] abRect(int i) { return new float[]{sx() - 26 - 100, sy() - 150 - 40 - i * 44, 100, 36}; }

    static final float[] PAUSE_R = {12, 12, 48, 48}, SHOP_R = {12, 68, 48, 48};

    List<String> abilList() {
        List<String> out = new ArrayList<>();
        for (String k : new String[]{"salvo", "emp", "smoke", "repair"}) if (Boolean.TRUE.equals(g.AB().get(k))) out.add(k);
        return out;
    }

    boolean vehMis() { return g.VEH.equals("pantsir") && g.st("maxMis") > 0; }

    void rrect(float x, float y, float w, float h, Color c, float a, boolean border) {
        d.rect(x, y, w, h, c, a);
        if (border) d.frame(x, y, w, h, 1, C_LINE, 0.22f);
    }

    /** Кинорамка при облёте добитого босса. */
    void drawKillBars() {
        float k = g.killCamK();
        if (k <= 0) return;
        float W = sx(), H = sy(), bh = H * 0.1f * k;
        d.rect(0, 0, W, bh, BLACK, 0.92f);
        d.rect(0, H - bh, W, bh, BLACK, 0.92f);
        if (k > 0.6f && g.killName != null) d.text(g.killName.toUpperCase() + " СБИТ", W / 2f, H - bh / 2f + 9, 24, C_ACC, Draw.TITLE, 0);
    }

    void drawHud() {
        Game G0 = g;
        float W = sx(), H = sy();
        // пауза и магазин
        rrect(PAUSE_R[0], PAUSE_R[1], PAUSE_R[2], PAUSE_R[3], C_PANEL, 0.94f, true);
        d.rect(29, 27, 5, 18, WHITE); d.rect(39, 27, 5, 18, WHITE);
        rrect(SHOP_R[0], SHOP_R[1], SHOP_R[2], SHOP_R[3], C_ACC, 1, false);
        d.text("₽", 36, 101, 24, C("24170a"), Draw.BOLD, 0);
        // очки
        rrect(72, 12, 170, 70, C_PANEL, 0.94f, true);
        d.rect(72, 16, 3, 62, C_ACC);
        d.text(String.valueOf(G0.score), 84, 14 + 23, 24, WHITE, Draw.TITLE, -1);
        d.text(hudWaveText(), 84, 44 + 13, 13, C_MUT);
        d.text(G0.runWallet() ? "Монеты боя: " + G0.RUN.c : "+" + G0.runCoins, 84, 62 + 13, 13, C_ACC, Draw.BOLD, -1);
        if (G0.combo >= 3 && G0.comboT > 0) {
            float k = MathUtils.clamp(G0.comboT / 3f, 0, 1);
            d.text("Серия ×" + G0.combo, 76 + 1, 86 + 21, 20, BLACK, Draw.TITLE, -1);
            d.text("Серия ×" + G0.combo, 76, 86 + 20, 20, new Color(C_ACC.r, C_ACC.g, C_ACC.b, 0.5f + 0.5f * k), Draw.TITLE, -1);
        }
        // прочность
        float maxHp = G0.st("maxHp");
        float hpw = Math.min(340, W * 0.36f);
        float hx = (W - hpw) / 2;
        rrect(hx, 12, hpw, G0.obj != null ? 72 : 44, C_PANEL, 0.94f, true);
        d.text("Прочность комплекса", hx + 12, 12 + 20, 12, C_MUT);
        d.text(String.format("%d/%d", (int) Math.ceil(G0.hp), (int) maxHp), hx + hpw - 12, 12 + 20, 12, C_MUT, Draw.MAIN, 1);
        float hk = G0.hp / maxHp;
        if (hpShown < 0) { hpShown = hk; hpGhost = hk; }
        hpShown = MathUtils.lerp(hpShown, hk, 0.3f);
        if (hpGhost < hk) hpGhost = hk; else hpGhost = Math.max(hk, hpGhost - Gdx.graphics.getDeltaTime() * 0.25f);
        Color hc = hk > 0.5f ? C_GREEN : (hk > 0.25f ? C_WARN : C_RED);
        float bx = hx + 12, bw = hpw - 24;
        d.rect(bx, 39, bw, 8, WHITE, 0.08f);
        d.rect(bx, 39, bw * MathUtils.clamp(hpGhost, 0, 1), 8, WHITE, 0.45f);
        d.rect(bx, 39, bw * MathUtils.clamp(hpShown, 0, 1), 8, hc, 1);
        if (G0.smokeT > 0) d.text("ДЫМ", hx + hpw / 2, 12 + 20, 11, C_MUT, Draw.BOLD, 0);
        if (G0.obj != null) {
            d.text(G0.obj.name, hx + 12, 12 + 52, 12, C_MUT);
            d.text(Math.round(G0.obj.hp / G0.obj.max * 100) + "%", hx + hpw - 12, 12 + 52, 12, C_MUT, Draw.MAIN, 1);
            bar(bx, 12 + 58, bw, 8, G0.obj.hp / G0.obj.max, C("5aa8ff"));
        }
        float by = (G0.obj != null ? 84 : 56) + 12;
        for (Game.E b : G0.bosses) {
            float bww = Math.min(460, W * 0.5f);
            String bn = G.D.get("BOSS_INFO").get(b.type).getString("name");
            d.text(bn, W / 2 + 1, by + 13, 13, BLACK, Draw.TITLE, 0);
            d.text(bn, W / 2, by + 12, 13, C("e6d2ff"), Draw.TITLE, 0);
            d.rect((W - bww) / 2 - 2, by + 15, bww + 4, 14, BLACK, 0.45f);
            bar((W - bww) / 2, by + 17, bww, 10, b.hp / b.max, b.inv ? C("a4d0ff") : C_BOSS);
            by += 34;
        }
        // прицел
        float cxx = W / 2, cyy = H / 2;
        Color rc2 = new Color(0.85f, 1, 0.95f, 0.9f);
        for (int sx = -1; sx <= 1; sx += 2) for (int sy = -1; sy <= 1; sy += 2) {
            float ox = cxx + sx * 14, oy = cyy + sy * 14;
            d.line(ox, oy, ox - sx * 7, oy, 2, rc2, rc2.a);
            d.line(ox, oy, ox, oy - sy * 7, 2, rc2, rc2.a);
        }
        d.line(cxx, cyy - 20, cxx, cyy - 27, 1.5f, WHITE, 0.55f); d.line(cxx, cyy + 20, cxx, cyy + 27, 1.5f, WHITE, 0.55f);
        d.line(cxx - 20, cyy, cxx - 27, cyy, 1.5f, WHITE, 0.55f); d.line(cxx + 20, cyy, cxx + 27, cyy, 1.5f, WHITE, 0.55f);
        d.circle(cxx, cyy, 2, C_TEAL, 1);
        if (G0.hitMark > 0) {
            float k2 = G0.hitMark / 0.13f;
            Color hcm = new Color(1, 0.35f, 0.25f, 1);
            for (int sx = -1; sx <= 1; sx += 2) for (int sy = -1; sy <= 1; sy += 2)
                d.line(cxx + sx * 7, cyy + sy * 7, cxx + sx * (13 + 3 * k2), cyy + sy * (13 + 3 * k2), 2.5f, hcm, k2);
        }
        // цифры урона
        for (Game.DmgNum n : G0.dmgNums) {
            if (!proj(n.p, sp)) continue;
            float a = MathUtils.clamp(n.t / 0.35f, 0, 1);
            boolean mis = n.src.equals("mis");
            Color col = mis ? new Color(1, 0.62f, 0.2f, a) : (n.src.equals("gun") ? new Color(1, 0.95f, 0.6f, a) : new Color(0.75f, 0.9f, 1, a));
            float fs = mis ? 22 : (n.v < 100 ? 16 : 19);
            String s = String.valueOf(Math.round(n.v));
            d.text(s, sp.x + 1.5f, sp.y + 1.5f, fs, new Color(0, 0, 0, a * 0.6f), Draw.TITLE, 0);
            d.text(s, sp.x, sp.y, fs, col, Draw.TITLE, 0);
        }
        if (G0.st("gun") > 0 || G0.st("beam") > 0) {
            bar(cxx - 35, cyy + 32, 70, 4, G0.heat, G0.overheated ? C("ff3b30") : C("ffb05a"));
            if (G0.overheated) d.text("ПЕРЕГРЕВ", cxx, cyy + 50, 11, C("ff6a5e"), Draw.TITLE, 0);
        }
        // метки врагов
        List<Game.E> list = new ArrayList<>();
        for (Game.E e : G0.enemies) if (!hiddenE(e) && !e.type.equals("node")) list.add(e);
        final Vector3 cp = G0.camPos;
        Collections.sort(list, (a, b) -> Float.compare(a.pos.dst2(cp), b.pos.dst2(cp)));
        int arrows = 0, marks = 0;
        PerspectiveCamera cam = G0.cam;
        right.set(cam.direction).crs(cam.up).nor();
        up.set(right).crs(cam.direction).nor();
        for (Game.E e : list) {
            float dist = e.pos.dst(cp);
            Color col = typeCol(e);
            boolean on = proj(G0.visPos(e, tv2), sp) && sp.x > 8 && sp.x < W - 8 && sp.y > 8 && sp.y < H - 8;
            boolean shot = e.type.equals("erk") || e.type.equals("shell");
            if (on) {
                if (e.type.equals("scout")) bar(sp.x - 22, sp.y - 26, 44, 5, e.data, C("ffe04a"));
                if (e == G0.lock.t || dist < 40 || marks >= 28 || shot) continue;
                marks++;
                float rr = e.boss ? 13 : 9;
                if (e.ground) d.frame(sp.x - rr, sp.y - rr, rr * 2, rr * 2, 2, col, 1);
                else d.ring(sp.x, sp.y, rr, (e.boss || e.type.equals("heavy")) ? 3 : 2, col, 1);
            } else {
                if (arrows >= 10 || (shot && dist > 120)) continue;
                arrows++;
                tv.set(e.pos).sub(cam.position);
                float lx = tv.dot(right), ly = tv.dot(up);
                float ang = MathUtils.atan2(-ly, lx);
                float m = Math.max(Math.abs(MathUtils.cos(ang)) / 0.45f, Math.abs(MathUtils.sin(ang)) / 0.41f);
                float ax = W / 2 + MathUtils.cos(ang) / m * W / 2 * 0.9f, ay = H / 2 + MathUtils.sin(ang) / m * H / 2 * 0.9f;
                float fx = MathUtils.cos(ang), fy = MathUtils.sin(ang), sxx = -fy, syy = fx;
                d.poly(new float[]{ax + fx * 11, ay + fy * 11, ax - fx * 11 + sxx * 11, ay - fy * 11 + syy * 11, ax - fx * 5, ay - fy * 5}, col, 1);
                d.poly(new float[]{ax + fx * 11, ay + fy * 11, ax - fx * 5, ay - fy * 5, ax - fx * 11 - sxx * 11, ay - fy * 11 - syy * 11}, col, 1);
            }
        }
        // захват
        if (G0.lock.t != null && G0.state.equals("run")) {
            Game.E lt = G0.lock.t;
            if (proj(G0.visPos(lt, tv2), sp)) {
                float dist = lt.pos.dst(cp);
                float s = MathUtils.clamp(lt.r * 900f / dist, 26, 110) * (1.6f - G0.lock.k * 0.6f);
                boolean locked = G0.lock.k >= 1 && G0.st("maxMis") > 0;
                Color col = locked ? C("ff3b30") : new Color(1, 0.86f, 0.47f, 0.9f);
                d.frame(sp.x - s / 2, sp.y - s / 2, s, s, 2, col, col.a);
                d.text(Math.round(dist) + " м", sp.x + s / 2 + 6, sp.y - s / 2 + 10, 11, C("ffe08a"));
                if (locked) d.text("ЗАХВАТ", sp.x, sp.y - s / 2 - 6, 11, C("ff6a5e"), Draw.TITLE, 0);
                if (G0.st("gun") > 0 && proj(G0.leadPoint(lt, tv2), sp2)) {
                    d.polyline(new float[]{sp2.x, sp2.y - 8, sp2.x + 8, sp2.y, sp2.x, sp2.y + 8, sp2.x - 8, sp2.y}, true, 2, C("7cf5ff"), 1);
                }
            }
        }
        // радар
        float radarR = Math.min(70, H * 0.125f);
        float rx = W - 12 - radarR, ry = 12 + radarR;
        d.circle(rx, ry, radarR, new Color(0.02f, 0.06f, 0.06f, 1), 0.72f);
        d.polyline(Draw.oct(rx, ry, radarR + 5), true, 1.5f, C_TEAL, 0.5f);
        for (float k : new float[]{0.33f, 0.66f, 1f}) d.ring(rx, ry, (radarR - 2) * k, 1, new Color(0.47f, 0.86f, 0.78f, 1), 0.35f);
        d.poly(new float[]{rx, ry, rx + MathUtils.cos(-MathUtils.PI / 2 - 0.5f) * radarR, ry + MathUtils.sin(-MathUtils.PI / 2 - 0.5f) * radarR, rx, ry - radarR,
                rx + MathUtils.cos(-MathUtils.PI / 2 + 0.5f) * radarR, ry + MathUtils.sin(-MathUtils.PI / 2 + 0.5f) * radarR}, new Color(0.47f, 0.94f, 0.78f, 1), 0.12f);
        float sw = (T * 2.4f) % MathUtils.PI2;
        d.line(rx, ry, rx + MathUtils.cos(sw) * radarR, ry + MathUtils.sin(sw) * radarR, 1, new Color(0.47f, 0.94f, 0.78f, 1), 0.6f);
        float cyw = MathUtils.cos(G0.aimYaw), syw = MathUtils.sin(G0.aimYaw), scl = (radarR - 4) / 750f;
        for (Game.E e : G0.enemies) {
            if (e.cloak != 0) continue;
            float vx = (e.pos.x * cyw - e.pos.z * syw) * scl, vy = (e.pos.x * syw + e.pos.z * cyw) * scl;
            float l = (float) Math.sqrt(vx * vx + vy * vy);
            if (l > radarR - 4) { vx = vx / l * (radarR - 4); vy = vy / l * (radarR - 4); }
            Color c = e.boss ? C("c08bff") : (e.ground ? C("ff9a3c") : ((e.type.equals("wing") || e.type.equals("kami")) ? C("ffd24a") : C("ff5a4a")));
            d.circle(rx + vx, ry + vy, e.boss ? 5 : (e.type.equals("erk") ? 1.5f : 3), c, 1);
        }
        d.rect(rx - 2, ry - 2, 4, 4, C("7cf5c0"));
        // кнопки
        float[] fc = fireC();
        boolean tor = G0.VEH.equals("tor");
        roundBtn(fc[0], fc[1], 59, tor ? C("2a5bc4") : C("d03a28"), tor ? C("4a85e6") : C("f0604a"), G0.firing, new Color(1, 1, 1, 0.14f));
        if (tor) {
            d.text("ПУСК", fc[0], fc[1] + 2, 18, WHITE, Draw.TITLE, 0);
            pips(fc[0], fc[1] + 14, G0.missiles, (int) G0.st("maxMis"));
        } else d.text(G0.VEH.equals("luch") ? "ЛУЧ" : "ОГОНЬ", fc[0], fc[1] + 7, 19, WHITE, Draw.TITLE, 0);
        if (DESK) d.text(tor ? "ЛКМ / ПКМ" : "ЛКМ / пробел", fc[0], fc[1] + (tor ? 40 : 30), 11, new Color(1, 1, 1, 0.75f), Draw.MAIN, 0);
        if (vehMis()) {
            float[] mc = misC();
            boolean ready = G0.lock.t != null && G0.lock.k >= 1 && G0.missiles > 0;
            float dk = G0.missiles <= 0 ? 0.5f : 0f;
            roundBtn(mc[0], mc[1], 42, darker(C("2a5bc4"), dk), darker(C("4a85e6"), dk), false, ready ? C("ff3b30") : new Color(1, 1, 1, 0.14f));
            d.text("РАКЕТА", mc[0], mc[1] + 1, 13, WHITE, Draw.TITLE, 0);
            if (DESK) d.text("ПКМ", mc[0], mc[1] - 56, 12, new Color(1, 1, 1, 0.75f), Draw.MAIN, 0);
            pips(mc[0], mc[1] + 10, G0.missiles, (int) G0.st("maxMis"));
            if (G0.missiles < G0.st("maxMis") && G0.reloadT > 0) {
                float k = 1 - G0.reloadT / G0.st("reload");
                d.arc(mc[0], mc[1], 48.5f, -MathUtils.PI / 2, -MathUtils.PI / 2 + MathUtils.PI2 * k, 3, C_TEAL, 0.8f);
            }
        }
        List<String> ab = abilList();
        for (int i = 0; i < ab.size(); i++) {
            String k = ab.get(i);
            float[] r = abRect(i);
            boolean used = (k.equals("salvo") || k.equals("emp")) ? G0.abUsed.get(k) : G0.abCd.get(k) > 0;
            d.rect(r[0], r[1], r[2], r[3], C_PANEL, used ? 0.5f : 0.9f);
            d.frame(r[0], r[1], r[2], r[3], 1, C_LINE, 0.22f);
            String nm = G.D.get("ABIL").get(k).getString("name").split(" ")[0].toUpperCase();
            d.text((DESK ? (i + 1) + " · " : "") + nm, r[0] + r[2] / 2, r[1] + 23, 12, new Color(1, 1, 1, used ? 0.4f : 1), Draw.TITLE, 0);
            float kf = 1;
            if (k.equals("smoke")) kf = 1 - G0.abCd.get("smoke") / 30f;
            else if (k.equals("repair")) kf = 1 - G0.abCd.get("repair") / 45f;
            else if (used) kf = 0;
            d.rect(r[0] + 6, r[1] + r[3] - 4, (r[2] - 12) * kf, 3, C_ACC);
        }
        if (DESK && !host.bot() && G0.state.equals("run") && !Gdx.input.isCursorCatched())
            d.text("Кликните по экрану, чтобы управлять мышью", W / 2, H * 0.7f, 16, new Color(1, 1, 1, 0.85f), Draw.TITLE, 0);
        if (G0.hurtT > 0) d.rect(0, 0, W, H, new Color(0.86f, 0.08f, 0.04f, 1), MathUtils.clamp(G0.hurtT * 0.5f, 0, 0.3f));
        if (hk < 0.3f && G0.hp > 0) {
            float a = (0.3f - hk) * (0.6f + 0.4f * MathUtils.sin(T * 6));
            float e = W * 0.12f;
            d.gradH(0, 0, e, H, C_RED, a, C_RED, 0); d.gradH(W - e, 0, e, H, C_RED, 0, C_RED, a);
        }
        if (G0.empT > 0) d.rect(0, 0, W, H, new Color(0.35f, 0.66f, 1, 1), 0.08f + MathUtils.sin(T * 1000 / 120f) * 0.04f);
        // баннер, подсказка, рация
        if (bannerT > 0) {
            float ba = MathUtils.clamp(Math.min(bannerT / 0.4f, (3f - bannerT) / 0.3f), 0, 1);
            float y = H * 0.22f;
            Color bc = bannerBoss ? C("d8b4ff") : WHITE;
            d.text(bannerS, W / 2 + 2, y + 38 + 2, 34, new Color(0, 0, 0, 0.6f * ba), Draw.TITLE, 0);
            d.text(bannerS, W / 2, y + 38, 34, new Color(bc.r, bc.g, bc.b, ba), Draw.TITLE, 0);
            if (!bannerSub.isEmpty()) {
                d.text(bannerSub, W / 2 + 1.5f, y + 48 + 20 + 1.5f, 16, new Color(0, 0, 0, 0.6f * ba), Draw.MAIN, 0);
                d.text(bannerSub, W / 2, y + 48 + 20, 16, new Color(1, 0.886f, 0.66f, ba), Draw.MAIN, 0);
            }
        }
        if (hintT > 0 && !hintS.isEmpty()) {
            float a = MathUtils.clamp(hintT / 0.3f, 0, 1);
            float hw = W * 0.6f;
            d.wrap(hintS, W * 0.2f + 1.5f, H - 175 + 1.5f, hw, 15, new Color(0, 0, 0, 0.6f * a), Draw.MAIN, 0);
            d.wrap(hintS, W * 0.2f, H - 175, hw, 15, new Color(1, 1, 1, a), Draw.MAIN, 0);
        }
        if (commCur != null) drawComm();
    }

    String hudWaveText() {
        Game G0 = g;
        if (G0.mode.equals("endless") || G0.CM == null) return "Волна " + Math.max(1, G0.wave);
        JsonValue C = G0.CM;
        String nm = "chal".equals(C.getString("special", "")) ? C.getString("title") : "Миссия " + C.getString("id");
        if (C.getString("type").equals("waves")) return String.format("%s · волна %d/%d", nm, Math.max(1, G0.wave), C.get("waves").size);
        return String.format("%s · %d с", nm, Math.max(0, (int) Math.ceil(G0.missionT)));
    }

    void drawComm() {
        JsonValue c = G.D.get("CH").get(commCur[0]);
        String name = c != null ? c.getString("name") : commCur[0], ava = c != null ? c.getString("ava") : "?";
        Color col = c != null ? G.hex(c.getString("col")) : WHITE;
        float x = 72, y = 96, tw = 300;
        boolean was = d.off; d.off = true;
        float th = d.wrap(commCur[1], 0, 0, tw, 14, WHITE, Draw.MAIN, -1);
        d.off = was;
        float h = Math.max(56, 22 + th + 8), w = 12 + 36 + 10 + tw + 12;
        d.rect(x, y, w, h, new Color(0.04f, 0.07f, 0.12f, 1), 0.88f);
        d.frame(x, y, w, h, 1, C_LINE, 0.22f);
        d.rect(x, y, 3, h, col, 1);
        d.rect(x + 12, y + 10, 36, 36, col, 1);
        d.text(ava, x + 30, y + 35, 18, C("0b1220"), Draw.TITLE, 0);
        d.text(name, x + 58, y + 20, 12, col, Draw.BOLD, -1);
        d.wrap(commCur[1], x + 58, y + 24, tw, 14, WHITE, Draw.MAIN, -1);
    }

    static Color darker(Color c, float k) { return new Color(c.r * (1 - k), c.g * (1 - k), c.b * (1 - k), c.a); }

    boolean hiddenE(Game.E e) {
        if (e.cloak != 0) return true;
        if (e.stealth) return e.reveal <= 0 && e.pos.dst(g.camPos) > 170f;
        return false;
    }

    Color typeCol(Game.E e) {
        if (e.boss) return C("c08bff");
        switch (e.type) {
            case "scout": return C("ffe04a");
            case "relay": return C("3ddc97");
            case "wing": case "kami": return C("ffd24a");
            case "armor": return C("9fd0ff");
            case "stealth": return C("b48cff");
            default: return e.ground ? C("ff9a3c") : C("ff5a4a");
        }
    }

    void roundBtn(float cx, float cy, float r, Color base, Color top, boolean pressed, Color ring) {
        float k = pressed ? 0.93f : 1f;
        d.poly(Draw.oct(cx, cy, (r + 6) * k), BLACK, 0.35f);
        d.poly(Draw.oct(cx, cy, r * k), darker(base, 0.25f), 1);
        d.poly(Draw.oct(cx, cy, r * 0.88f * k), top, 0.92f);
        d.polyline(Draw.oct(cx, cy, (r + 4) * k), true, 2, ring.a > 0.2f ? ring : WHITE, ring.a > 0.2f ? ring.a : 0.35f);
        float[] hl = Draw.oct(cx, cy, r * 0.74f * k);
        d.polyline(new float[]{hl[8], hl[9], hl[10], hl[11], hl[12], hl[13]}, false, 2, WHITE, 0.3f);
    }

    void pips(float cx, float cy, int n, int mx) {
        int cnt = Math.min(mx, 12);
        float w = cnt * 9;
        for (int i = 0; i < cnt; i++) d.rect(cx - w / 2 + i * 9, cy, 6, 11, WHITE, i < n ? 1 : 0.25f);
    }

    // =========================================================
    //                          ВВОД
    // =========================================================
    void deskMouse() {
        if (!DESK || host.bot()) return;
        boolean want = inBattle() && g.state.equals("run") && !mouseFree;
        if (strike != null && strike.active) want = strikeInput() && !mouseFree;
        if (want != Gdx.input.isCursorCatched()) Gdx.input.setCursorCatched(want);
    }

    void keyAim(float dt) {
        if (strike != null && strike.active) { if (strikeInput() && strike.st.equals("fly")) strikeKeys(); return; }
        if (!inBattle()) return;
        if (bossRole()) { bossKeys(); return; }
        float kk = dt * 1.6f * G.SV.sens;
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) g.aimYaw += kk;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) g.aimYaw -= kk;
        if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) g.aimPitch = MathUtils.clamp(g.aimPitch + kk, -0.06f, 1.2f);
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) g.aimPitch = MathUtils.clamp(g.aimPitch - kk, -0.06f, 1.2f);
    }

    boolean deskHudClick(float x, float y) {
        if (in(x, y, PAUSE_R[0], PAUSE_R[1], PAUSE_R[2], PAUSE_R[3])) { pause(); return true; }
        if (in(x, y, SHOP_R[0], SHOP_R[1], SHOP_R[2], SHOP_R[3])) { openShopBattle(); return true; }
        List<String> ab = abilList();
        for (int i = 0; i < ab.size(); i++) {
            float[] r = abRect(i);
            if (in(x, y, r[0], r[1], r[2], r[3])) { g.useAbility(ab.get(i)); return true; }
        }
        return false;
    }

    void aimDelta(float dx, float dy, float k) {
        g.aimYaw -= dx * k;
        g.aimPitch = MathUtils.clamp(g.aimPitch - dy * k, -0.06f, 1.2f);
    }

    @Override
    public boolean touchDown(int sx, int sy, int ptr, int button) {
        float x = sx / d.s, y = sy / d.s;
        if (pending != null) return true;
        if (strikeInput()) return strikeTouchDown(x, y, ptr, button);
        if (inBattle() && bossRole()) return bossTouchDown(x, y, ptr, button);
        if (inBattle()) {
            if (DESK) {
                if (!Gdx.input.isCursorCatched()) {
                    if (button == Input.Buttons.LEFT && !deskHudClick(x, y)) mouseFree = false;
                } else if (button == Input.Buttons.LEFT) g.firing = true;
                else if (button == Input.Buttons.RIGHT) {
                    if (g.VEH.equals("tor")) g.firing = true; else g.fireMissile("lock", false);
                }
                return true;
            }
            String what = "aim";
            float[] fc = fireC(), mc = misC();
            if (Vector2.dst(x, y, fc[0], fc[1]) < 64) { what = "fire"; g.firing = true; }
            else if (vehMis() && Vector2.dst(x, y, mc[0], mc[1]) < 46) { what = "mis"; g.fireMissile("lock", false); }
            else if (in(x, y, PAUSE_R[0], PAUSE_R[1], PAUSE_R[2], PAUSE_R[3])) { what = "ui"; pause(); }
            else if (in(x, y, SHOP_R[0], SHOP_R[1], SHOP_R[2], SHOP_R[3])) { what = "ui"; openShopBattle(); }
            else {
                List<String> ab = abilList();
                for (int i = 0; i < ab.size(); i++) {
                    float[] r = abRect(i);
                    if (in(x, y, r[0], r[1], r[2], r[3])) { what = "ab"; g.useAbility(ab.get(i)); }
                }
            }
            touches.put(ptr, what);
            lastTouch.put(ptr, new Vector2(x, y));
            return true;
        }
        if (down && ptr != downPtr) return true;
        down = true; downPtr = ptr; dragged = false; capture = false;
        downX = curX = x; downY = curY = y; lastY = y;
        mx = x; my = y;
        return true;
    }

    @Override
    public boolean touchDragged(int sx, int sy, int ptr) {
        float x = sx / d.s, y = sy / d.s;
        if (strikeInput() || (inBattle() && bossRole())) {
            if (DESK) { if (Gdx.input.isCursorCatched()) mouseMoved(sx, sy); return true; }
            return strikeInput() ? strikeDrag(x, y, ptr) : bossDrag(x, y, ptr);
        }
        if (inBattle()) {
            if (DESK) { if (Gdx.input.isCursorCatched()) mouseMoved(sx, sy); return true; }
            Vector2 lt = lastTouch.get(ptr);
            if ("aim".equals(touches.get(ptr)) && lt != null) aimDelta(x - lt.x, y - lt.y, 0.0042f * G.SV.sens);
            if (lt != null) lt.set(x, y);
            return true;
        }
        if (ptr != downPtr || !down) return true;
        curX = x; curY = y; mx = x; my = y;
        if (!dragged && Vector2.dst(x, y, downX, downY) > 9) dragged = true;
        if (dragged && !capture) scroll -= (y - lastY);
        lastY = y;
        return true;
    }

    @Override
    public boolean touchUp(int sx, int sy, int ptr, int button) {
        float x = sx / d.s, y = sy / d.s;
        if (strike != null && strike.active && (touches.containsKey(ptr) || DESK) && panel.isEmpty()) return strikeUp(ptr, button);
        if (bossRole() && (touches.containsKey(ptr) || (DESK && inBattle()))) { bossUp(ptr); if (inBattle()) return true; }
        if (inBattle() || touches.containsKey(ptr)) {
            if (DESK) {
                if (button == Input.Buttons.LEFT) g.firing = false;
                if (button == Input.Buttons.RIGHT && g.VEH.equals("tor")) g.firing = false;
            }
            if ("fire".equals(touches.get(ptr))) g.firing = false;
            touches.remove(ptr); lastTouch.remove(ptr);
            if (inBattle()) return true;
        }
        if (ptr != downPtr) return true;
        if (down && !dragged && !capture) { tapReady = true; tapX = x; tapY = y; }
        down = false; downPtr = -1;
        if (capture) { capture = false; G.save(); }
        return true;
    }

    @Override
    public boolean touchCancelled(int sx, int sy, int ptr, int button) {
        touches.remove(ptr); lastTouch.remove(ptr);
        if (ptr == downPtr) { down = false; downPtr = -1; }
        g.firing = false;
        return true;
    }

    @Override
    public boolean mouseMoved(int sx, int sy) {
        if (DESK && Gdx.input.isCursorCatched() && strikeInput()) {
            float k = 0.0022f * G.SV.sens;
            strike.mdX += Gdx.input.getDeltaX() / d.s * k; strike.mdY -= Gdx.input.getDeltaY() / d.s * k;
            return true;
        }
        if (DESK && Gdx.input.isCursorCatched() && inBattle() && bossRole()) {
            float k = 0.0026f * G.SV.sens;
            g.bossIn.cy -= Gdx.input.getDeltaX() / d.s * k;
            g.bossCamPitch = MathUtils.clamp(g.bossCamPitch + Gdx.input.getDeltaY() / d.s * k, -0.35f, 0.9f);
            return true;
        }
        if (inBattle() && DESK && Gdx.input.isCursorCatched()) {
            aimDelta(Gdx.input.getDeltaX() / d.s, Gdx.input.getDeltaY() / d.s, 0.0026f * G.SV.sens);
            return true;
        }
        mx = sx / d.s; my = sy / d.s;
        return false;
    }

    @Override
    public boolean scrolled(float ax, float ay) {
        if (!panel.isEmpty()) scroll += ay * 48;
        return true;
    }

    @Override
    public boolean keyDown(int kc) {
        if (kc == Input.Keys.BACK) { androidBack(); return true; }
        if (kc == Input.Keys.F11 && DESK) { host.toggleFullscreen(); return true; }
        if (strike != null && strike.active) {
            if (panel.equals("spause") && (kc == Input.Keys.ESCAPE || kc == Input.Keys.P)) { strike.resume(); show(""); return true; }
            if (strikeInput()) return strikeKey(kc);
        }
        if (inBattle() && bossRole()) return bossKey(kc);
        if (inBattle()) {
            if (kc == Input.Keys.SPACE) g.firing = true;
            else if (kc == Input.Keys.E || kc == Input.Keys.R) g.fireMissile("lock", false);
            else if (kc == Input.Keys.ESCAPE || kc == Input.Keys.P) pause();
            else if (kc == Input.Keys.B) openShopBattle();
            else if (kc == Input.Keys.TAB) mouseFree = !mouseFree;
            else if (kc >= Input.Keys.NUM_1 && kc <= Input.Keys.NUM_4) {
                List<String> ab = abilList();
                int i = kc - Input.Keys.NUM_1;
                if (i < ab.size()) g.useAbility(ab.get(i));
            } else return false;
            return true;
        }
        if (kc == Input.Keys.ESCAPE) {
            if (panel.equals("pause")) resume();
            else if (panel.equals("menu") || panel.equals("daily")) return false;
            else if (panel.equals("over") || panel.equals("result")) toMenu();
            else if (panel.equals("cut")) show("campaign");
            else back();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyUp(int kc) {
        if (kc == Input.Keys.SPACE) g.firing = false;
        return false;
    }

    @Override public boolean keyTyped(char c) { return false; }

    /** Потеря фокуса окна / сворачивание. */
    public void onPause() {
        g.firing = false;
        mouseFree = true;
        if (strike != null && strike.active) { strikePause(); return; }
        if (g.state.equals("run") && panel.isEmpty()) pause();
    }

    // =========================================================
    //                      РЕЖИМ «РОЙ»
    // =========================================================
    public interface StrikeHost { void startStrike(); void exitStrike(boolean hangar); }

    public StrikeHost strikeHost;

    @Override public void strikeOver() { show("sover"); }

    void pSwarm() {
        frame("swarm", 920, false, true, () -> {
            title("Рой: дрон-камикадзе");
            label("Учения «Обратная сторона»: ты — красная команда. Веди дрон через зону ПВО и уничтожь все цели рубежа. Лети низко — радары видят хуже, прячься за холмами, сбивай ракеты ловушками. На старте 8 дронов, за каждый прорванный рубеж — ещё 4–7. Кончились дроны — конец забега.", 13, C_MUT, Draw.MAIN, 0);
            d.text(String.format("Рекорд: %d очков · лучший рубеж: %d · монеты: %d", G.SV.swBest, G.SV.swBestz, G.SV.c), cx + cw / 2, cy + 14, 14, C_ACC, Draw.BOLD, 0);
            cy += 24;
            String pick = ru.shitneba.Strike.DRONES.containsKey(G.SV.swPick) ? G.SV.swPick : "osa";
            if (btn("Взлёт · " + ru.shitneba.Strike.DRONES.get(pick).name, 52, BIG, true)) {
                pending = () -> { show(""); if (strikeHost != null) strikeHost.startStrike(); };
                pendingFrames = 1;
            }
            section("Дроны");
            List<Card> cs = new ArrayList<>();
            for (final ru.shitneba.Strike.DroneDef dd : ru.shitneba.Strike.DRONES.values()) {
                boolean own = G.SV.swOwn.contains(dd.key), sel = pick.equals(dd.key);
                Card c = card(dd.name, dd.desc, own ? (sel ? "Выбран" : "Выбрать") : "Купить · " + dd.price, (own && !sel) || (!own && G.SV.c >= dd.price), () -> {
                    if (!G.SV.swOwn.contains(dd.key)) { G.SV.c -= dd.price; G.SV.swOwn.add(dd.key); G.sfx("coin"); }
                    G.SV.swPick = dd.key; G.save();
                });
                c.sel = sel;
                c.sub = String.format("Скорость %d · прочность %d · взрыв %d%s", (int) (dd.sp * 3.6f), (int) dd.hp, (int) dd.dmg, dd.bombs > 0 ? " · бомбы ×" + dd.bombs : "");
                c.subc = C_TEAL;
                cs.add(c);
            }
            cards(cs, 4);
            section("Улучшения (для всех дронов)");
            cs.clear();
            for (final String[] u : ru.shitneba.Strike.UPS) {
                final int l = ru.shitneba.Strike.up(u[0]);
                boolean mx = l >= 5;
                final int cost = mx ? 0 : ru.shitneba.Strike.UP_COST[l];
                Card c = card(u[1], u[2], mx ? "Максимум" : "Улучшить · " + cost, !mx && G.SV.c >= cost, () -> {
                    G.SV.c -= cost; G.SV.swUp.put(u[0], l + 1); G.sfx("coin"); G.save();
                });
                c.lv = l; c.max = 5;
                cs.add(c);
            }
            cards(cs, 4);
            int r = btnRow(new String[]{"Камера: " + (G.SV.swCam == 0 ? "FPV" : "3-е лицо"), "Инверсия высоты: " + (G.SV.swInv ? "вкл" : "выкл")}, new int[]{NORMAL, NORMAL}, null, 40);
            if (r == 0) { G.SV.swCam = 1 - G.SV.swCam; G.save(); }
            else if (r == 1) { G.SV.swInv = !G.SV.swInv; G.save(); }
            label(DESK ? "ПК: мышь — курс, ЛКМ или Shift — ускорение, Ctrl — тормоз, ПКМ или пробел — ловушки, E — сброс бомбы, X — подрыв, C — камера, Esc — пауза."
                    : "Телефон: левый джойстик — руль, свайп справа — точное наведение, «Ускор» удерживать.", 12, C_MUT);
        });
    }

    void pSPause() {
        final ru.shitneba.Strike S = strike;
        frame("spause", 400, false, true, () -> {
            title("Пауза");
            if (btn("Продолжить", 50, BIG, true)) { S.resume(); show(""); }
            if (btn("Камера: " + (S.camMode == 0 ? "FPV" : "3-е лицо"), 42, NORMAL, true)) S.toggleCam();
            if (btn("Завершить забег", 42, NORMAL, true)) { show(""); S.endRun(); }
            if (btn("Выйти в меню", 42, NORMAL, true)) { if (strikeHost != null) strikeHost.exitStrike(false); }
        });
    }

    void pSOver() {
        final ru.shitneba.Strike S = strike;
        frame("sover", 500, false, true, () -> {
            title("Рой рассеян");
            if (S.resRec) { d.text("НОВЫЙ РЕКОРД!", cx + cw / 2, cy + 18, 20, C_ACC, Draw.WIDE, 0); cy += 30; }
            String[][] cells = {{"Очки", String.valueOf(S.score)}, {"Рубеж", String.valueOf(S.zone)}, {"Целей", String.valueOf(S.tgKills)}, {"Машин ПВО", String.valueOf(S.aaKills)}};
            float gp = 6, w = (cw - gp * 3) / 4;
            for (int i = 0; i < 4; i++) {
                float x = cx + i * (w + gp);
                if (!d.off) {
                    box(x, cy, w, 58, WHITE, 0.05f, C_LINE, 0.22f, 1);
                    d.text(cells[i][0], x + w / 2, cy + 18, 12, C_MUT, Draw.MAIN, 0);
                    d.text(cells[i][1], x + w / 2, cy + 46, 22, C_TXT, Draw.TITLE, 0);
                }
            }
            cy += 66;
            label(String.format("Вылетов: %d · монеты +%d · опыт +%d%s", S.sorties, S.resCoins, S.resXp, S.resRankUp ? " · новое звание!" : ""), 14, C_ACC, Draw.BOLD, 0);
            d.text(rankText(), cx, cy + 13, 13, C_RANK, Draw.BOLD, -1);
            cy += 20; bar(cx, cy, cw, 6, rankK(), C_RANK); cy += 14;
            int r = btnRow(new String[]{"Ещё раз", "Ангар", "Меню"}, new int[]{BIG, NORMAL, NORMAL}, null, 48);
            if (r == 0) { pending = () -> { show(""); if (strikeHost != null) strikeHost.startStrike(); }; pendingFrames = 1; }
            else if (r == 1) { if (strikeHost != null) strikeHost.exitStrike(true); }
            else if (r == 2) { if (strikeHost != null) strikeHost.exitStrike(false); }
        });
    }

    static final Color C_OSD = new Color(0.92f, 1, 0.96f, 1), C_SRED = C("ff5a47");

    void osd(String t, float x, float y, float size, Color col, int align, boolean title) {
        int f = title ? Draw.TITLE : Draw.MAIN;
        Color sh = new Color(0, 0, 0, 0.65f * col.a);
        d.text(t, x + 1.2f, y + 1.2f, size, sh, f, align);
        d.text(t, x - 0.8f, y - 0.8f, size, sh, f, align);
        d.text(t, x, y, size, col, f, align);
    }

    float[] sBoost() { return new float[]{sx() - 84, sy() - 84}; }
    float[] sFlare() { return new float[]{sx() - 200, sy() - 62}; }
    float[] sDet() { return new float[]{sx() - 84, sy() - 212}; }
    float[] sBrake() { return new float[]{sx() - 196, sy() - 160}; }
    float[] sBomb() { return new float[]{sx() - 300, sy() - 62}; }
    float[] sCam() { return new float[]{sx() - 222, 14, 70, 40}; }

    int joyId = -1;
    final Vector2 joyO = new Vector2(), joyP = new Vector2();
    boolean mbBoost;

    void sBtn(float[] c, float r, String label, Color col, boolean on, String sub) {
        d.poly(Draw.oct(c[0], c[1], r), col, on ? 0.55f : 0.18f);
        d.polyline(Draw.oct(c[0], c[1], r), true, 2, WHITE, on ? 0.6f : 0.2f);
        osd(label, c[0], c[1] + (sub.isEmpty() ? 5 : -1), r > 50 ? 14 : 12, new Color(1, 1, 1, on ? 1 : 0.45f), 0, true);
        if (!sub.isEmpty()) osd(sub, c[0], c[1] + 15, 12, new Color(1, 1, 1, on ? 0.8f : 0.4f), 0, true);
    }

    void edgeArrow(Vector3 p, Color col, String label) {
        PerspectiveCamera cam = g.cam;
        float W = sx(), H = sy();
        tv.set(p).sub(cam.position);
        right.set(cam.direction).crs(cam.up).nor();
        float a = MathUtils.atan2(tv.dot(right), tv.dot(cam.direction));
        float r = Math.min(W, H) * 0.4f;
        float fx = MathUtils.sin(a), fy = -MathUtils.cos(a), sx2 = -fy, sy2 = fx;
        float ax = W / 2 + fx * r, ay = H / 2 + fy * r;
        d.poly(new float[]{ax + fx * 14, ay + fy * 14, ax - fx * 9 + sx2 * 10, ay - fy * 9 + sy2 * 10, ax - fx * 3, ay - fy * 3}, col, 1);
        d.poly(new float[]{ax + fx * 14, ay + fy * 14, ax - fx * 3, ay - fy * 3, ax - fx * 9 - sx2 * 10, ay - fy * 9 - sy2 * 10}, col, 1);
        osd(label, ax - fx * 26, ay - fy * 26 + 4, 11, col, 0, true);
    }

    boolean onScreen(Vector2 v) { return v.x >= 0 && v.x <= sx() && v.y >= 0 && v.y <= sy(); }

    final Array<Vector3> tmpArr = new Array<>();

    void drawStrikeHud() {
        ru.shitneba.Strike S = strike;
        ru.shitneba.Strike.DroneState D = S.D;
        float W = sx(), H = sy();
        Color AMB = C_ACC, TEAL = C_TEAL;
        boolean fpv = S.camMode == 0 && S.st.equals("fly");
        ru.shitneba.Strike.Obj near = null; float nd = 1e9f;
        for (ru.shitneba.Strike.Obj t : S.targets) if (t.alive && D.alive && t.pos.dst(D.pos) < nd) { nd = t.pos.dst(D.pos); near = t; }
        for (ru.shitneba.Strike.Obj t : S.targets) {
            if (!t.alive) continue;
            tv2.set(t.pos).add(0, t.hr + 4, 0);
            float dist = D.alive ? t.pos.dst(D.pos) : 0;
            if (proj(tv2, sp) && onScreen(sp)) {
                float r = 11;
                d.polyline(new float[]{sp.x, sp.y - r, sp.x + r, sp.y, sp.x, sp.y + r, sp.x - r, sp.y}, true, 2, AMB, 1);
                String nm = ru.shitneba.Strike.tgName(t.kind);
                if (t == near) osd(nm + " · " + (int) dist + " м", sp.x, sp.y - 16, 13, AMB, 0, true);
                else if (!D.alive || dist < 200) osd(nm, sp.x, sp.y - 16, 11, new Color(AMB.r, AMB.g, AMB.b, 0.8f), 0, true);
                float k = t.hp / Math.max(1, t.max);
                if (k < 1) { d.rect(sp.x - 20, sp.y + 15, 40, 4, BLACK, 0.5f); d.rect(sp.x - 20, sp.y + 15, 40 * k, 4, AMB, 1); }
            }
        }
        for (ru.shitneba.Strike.Obj u : S.units) {
            if (!u.alive || !u.known) continue;
            if (!proj(tv2.set(u.pos).add(0, 6.5f, 0), sp) || !onScreen(sp)) continue;
            Color col = u.track > 0.3f ? C_SRED : new Color(1, 0.55f, 0.45f, 0.75f);
            d.polyline(new float[]{sp.x, sp.y - 9, sp.x + 9, sp.y + 7, sp.x - 9, sp.y + 7}, true, 2, col, col.a);
            if (D.alive && (u.track > 0.3f || u.pos.dst(D.pos) < 200)) osd(G.vehName(u.kind), sp.x, sp.y - 13, 11, col, 0, true);
        }
        for (ru.shitneba.Strike.Hunter h : S.hunters) {
            if (!h.alive) continue;
            if (proj(h.pos, sp) && onScreen(sp)) d.ring(sp.x, sp.y, 8, 2, h.chase ? C_SRED : new Color(1, 0.6f, 0.5f, 1), h.chase ? 1 : 0.6f);
        }
        if (S.st.equals("fly") && D.alive) {
            for (Vector3 m : S.missilePositions(tmpArr)) {
                if (proj(m, sp) && onScreen(sp)) { d.ring(sp.x, sp.y, 12, 2.5f, C_SRED, 1); d.line(sp.x - 16, sp.y, sp.x + 16, sp.y, 2, C_SRED, 1); }
                else edgeArrow(m, C_SRED, "РАКЕТА " + (int) m.dst(D.pos) + " м");
            }
            if (near != null) {
                if (!proj(tv2.set(near.pos).add(0, near.hr, 0), sp) || !onScreen(sp)) edgeArrow(near.pos, AMB, ru.shitneba.Strike.tgName(near.kind) + " " + (int) nd + " м");
            }
        }
        float cxx = W / 2, cyy = H / 2;
        if (S.st.equals("fly") && D.alive) {
            if (fpv) {
                float hy = cyy + D.pitch * H * 0.55f;
                float rl = D.roll, dx = MathUtils.cos(rl), dy = MathUtils.sin(rl);
                d.line(cxx - dx * 160, hy - dy * 160, cxx - dx * 60, hy - dy * 60, 2, WHITE, 0.55f);
                d.line(cxx + dx * 60, hy + dy * 60, cxx + dx * 160, hy + dy * 160, 2, WHITE, 0.55f);
                d.line(cxx - 14, cyy, cxx - 5, cyy, 2, C_OSD, 1); d.line(cxx + 5, cyy, cxx + 14, cyy, 2, C_OSD, 1);
                d.line(cxx, cyy + 5, cxx, cyy + 12, 2, C_OSD, 1);
                d.circle(cxx, cyy, 2, C_OSD, 1);
                osd("REC", W - 330, 34, 13, C_SRED, -1, true);
                d.circle(W - 330 + 36, 29, 4, C_SRED, MathUtils.sin(T * 6) > 0 ? 1 : 0.3f);
            } else if (proj(S.fwd(tv2).scl(60).add(D.pos), sp)) {
                d.ring(sp.x, sp.y, 9, 2, C_OSD, 1); d.circle(sp.x, sp.y, 1.5f, C_OSD, 1);
            }
            float agl = D.pos.y - g.world.height(D.pos.x, D.pos.z);
            boolean low = agl < 14;
            float bk = D.bat / Math.max(1, D.bmax);
            float y0 = H - 26;
            osd("ВЫС " + (int) agl + " м", cxx - 150, y0, 15, low ? TEAL : C_OSD, 0, true);
            osd((int) (D.sp * 3.6f) + " км/ч", cxx, y0, 15, C_OSD, 0, true);
            osd("АКБ " + (int) (bk * 100) + "%", cxx + 150, y0, 15, bk < 0.25f ? C_SRED : C_OSD, 0, true);
            float bw = 220;
            d.rect(cxx - bw / 2, y0 - 34, bw, 6, BLACK, 0.45f);
            d.rect(cxx - bw / 2, y0 - 34, bw * D.hp / Math.max(1, D.max), 6, D.hp > D.max * 0.35f ? TEAL : C_SRED, 1);
            osd("КОРПУС", cxx - bw / 2 - 8, y0 - 27, 11, C_OSD, 1, true);
            if (low) osd("НИЗКО: РЛС ВИДЯТ ХУЖЕ", cxx, y0 - 46, 12, TEAL, 0, true);
            float sig = 1 - MathUtils.clamp(D.pos.dst(S.launchP) / 1600f + S.jam * 0.7f, 0, 0.9f);
            for (int i = 0; i < 5; i++) d.rect(W - 340 + i * 7, 54 - i * 3, 5, 6 + i * 3, i / 5f < sig ? C_OSD : WHITE, i / 5f < sig ? 1 : 0.2f);
            float wy = 120;
            int nmis = S.missilesAtMe();
            if (S.lockWarn > 0 && (T % 0.4f) < 0.25f) { osd("ОБЛУЧЕНИЕ РАДАРОМ", cxx, wy, 18, C_SRED, 0, true); wy += 24; }
            else if (S.alert > 0) { osd("ОБНАРУЖЕН", cxx, wy, 17, AMB, 0, true); wy += 24; }
            if (nmis > 0 && (T % 0.3f) < 0.18f) { osd("РАКЕТА! ×" + nmis, cxx, wy, 20, C_SRED, 0, true); wy += 26; }
            if (S.laserWarn > 0) { osd("ЛАЗЕР ЖЖЁТ КОРПУС", cxx, wy, 17, C_SRED, 0, true); wy += 24; }
            if (S.jam > 0.15f) { osd("ПОМЕХИ РЭБ", cxx, wy, 16, C_RANK, 0, true); wy += 22; }
            if (D.fall) osd("АКБ РАЗРЯЖЕНА — ПАДАЕМ", cxx, wy, 18, C_SRED, 0, true);
            if (nmis > 0 && (T % 0.5f) < 0.02f) G.sfx("lock", 0.35f, 200);
        }
        d.rect(PAUSE_R[0], PAUSE_R[1], PAUSE_R[2], PAUSE_R[3], BLACK, 0.45f);
        d.rect(29, 27, 5, 18, WHITE); d.rect(39, 27, 5, 18, WHITE);
        osd("РУБЕЖ " + S.zone, 74, 30, 17, AMB, -1, true);
        osd(String.valueOf(S.score), 74, 52, 18, C_OSD, -1, true);
        osd(String.format("Целей: %d · дронов в запасе: %d", S.targetsLeft(), S.stock), 74, 72, 12, C_OSD, -1, false);
        for (int i = 0; i < Math.min(S.stock, 20); i++) d.rect(74 + i * 12, 80, 8, 8, TEAL, 1);
        if (S.chain >= 2 && S.chainT > 0) osd("Серия ×" + S.chain, 74, 108, 15, TEAL, -1, true);
        float[] cr = sCam();
        d.rect(cr[0], cr[1], cr[2], cr[3], BLACK, 0.45f); d.frame(cr[0], cr[1], cr[2], cr[3], 1.5f, WHITE, 0.5f);
        osd(S.camMode == 0 ? "FPV" : "3-Е ЛИЦО", cr[0] + cr[2] / 2, cr[1] + cr[3] / 2 + 5, 12, C_OSD, 0, true);
        minimap(W - 80, 82, 64);
        float fy = 166;
        for (ru.shitneba.Strike.Feed f : S.feed) {
            float a = MathUtils.clamp(f.life, 0, 1);
            osd(f.t, W - 14, fy, 13, new Color(f.c.r, f.c.g, f.c.b, a), 1, false);
            fy += 18;
        }
        if (S.bigT > 0) {
            float a = MathUtils.clamp(Math.min(S.bigT / 0.4f, (3.5f - S.bigT) / 0.3f), 0, 1);
            osd(S.big, cxx, H * 0.3f, 34, new Color(AMB.r, AMB.g, AMB.b, a), 0, true);
            osd(S.bigSub, cxx, H * 0.3f + 30, 15, new Color(1, 1, 1, a), 0, false);
        }
        if (S.st.equals("boom"))
            osd(S.D.alive ? "" : (S.stock > 0 ? "ДРОН ПОТЕРЯН · СИГНАЛА НЕТ" : "ДРОНЫ ЗАКОНЧИЛИСЬ"), cxx, H * 0.5f, 20, C_OSD, 0, true);
        if (S.fade > 0) {
            d.rect(0, 0, W, H, new Color(0.01f, 0.02f, 0.025f, 1), S.fade);
            if (S.fade > 0.6f && S.st.equals("clear")) osd("ПЕРЕБРОСКА К РУБЕЖУ " + (S.zone + 1) + "…", cxx, cyy + 8, 22, new Color(TEAL.r, TEAL.g, TEAL.b, S.fade), 0, true);
        }
        if (S.st.equals("fly") && D.alive) {
            boolean touch = !DESK || joyId >= 0 || touches.size() > 0;
            if (touch) {
                sBtn(sBoost(), 58, "УСКОР", C("c45a24"), D.bat > 0, "");
                sBtn(sFlare(), 40, "ЛОВУШКИ", C("2a5bc4"), D.flares > 0 && S.flareCd <= 0, "×" + D.flares);
                sBtn(sDet(), 36, "ПОДРЫВ", C("a02020"), true, "");
                if (!S.S.wing) sBtn(sBrake(), 36, "ТОРМОЗ", C("2f6e63"), true, "");
                if (S.S.bombs > 0) sBtn(sBomb(), 40, "СБРОС", C("7a6a20"), D.nb > 0, "×" + D.nb);
                float jx = joyId >= 0 ? joyO.x : 130, jy = joyId >= 0 ? joyO.y : H - 130;
                d.ring(jx, jy, 75, 2, WHITE, 0.25f);
                d.circle(joyId >= 0 ? joyP.x : jx, joyId >= 0 ? joyP.y : jy, 28, TEAL, 0.4f);
            } else {
                osd("Ловушки ×" + D.flares + (S.S.bombs > 0 ? "   Бомбы ×" + D.nb : ""), W - 20, H - 60, 14, C_OSD, 1, true);
                osd("Мышь — курс · WASD — руль · ЛКМ/Shift — ускор · Ctrl — тормоз · ПКМ/Пробел — ловушки · E — сброс · X — подрыв · C — камера", cxx, H - 6, 11, new Color(1, 1, 1, 0.55f), 0, false);
                if (!Gdx.input.isCursorCatched() && !host.bot()) osd("Кликните по экрану, чтобы управлять мышью", cxx, H * 0.62f, 16, C_OSD, 0, true);
            }
        }
    }

    void minimap(float cx0, float cy0, float R) {
        ru.shitneba.Strike S = strike;
        d.circle(cx0, cy0, R, new Color(0, 0.05f, 0.05f, 1), 0.55f);
        d.ring(cx0, cy0, R, 1.5f, C_TEAL, 0.6f);
        Vector3 me = S.D.pos;
        float yaw = S.D.yaw, scale = R / 420f;
        float cs = MathUtils.cos(yaw), sn = MathUtils.sin(yaw);
        for (ru.shitneba.Strike.Obj u : S.units) {
            if (!u.alive) continue;
            float[] m = mapPt(u.pos, me, cs, sn, scale, R, cx0, cy0);
            float det = S.detRange(u) * scale;
            if (Vector2.dst(m[0], m[1], cx0, cy0) + det < R) d.ring(m[0], m[1], det, 1, new Color(1, 0.3f, 0.2f, 1), 0.18f);
            d.rect(m[0] - 3, m[1] - 3, 6, 6, u.track > 0.3f ? C_SRED : new Color(1, 0.5f, 0.4f, 1), u.track > 0.3f ? 1 : 0.8f);
        }
        for (ru.shitneba.Strike.Obj t : S.targets) if (t.alive) {
            float[] m = mapPt(t.pos, me, cs, sn, scale, R, cx0, cy0);
            d.poly(new float[]{m[0], m[1] - 5, m[0] + 5, m[1], m[0], m[1] + 5, m[0] - 5, m[1]}, C_ACC, 1);
        }
        for (ru.shitneba.Strike.Hunter h : S.hunters) if (h.alive) { float[] m = mapPt(h.pos, me, cs, sn, scale, R, cx0, cy0); d.circle(m[0], m[1], 2.5f, C_SRED, 1); }
        for (Vector3 p : S.allMissiles(tmpArr)) { float[] m = mapPt(p, me, cs, sn, scale, R, cx0, cy0); d.circle(m[0], m[1], 2, WHITE, 1); }
        d.poly(new float[]{cx0, cy0 - 7, cx0 + 5, cy0 + 5, cx0 - 5, cy0 + 5}, C_TEAL, 1);
    }

    static float[] mapPt(Vector3 p, Vector3 me, float cs, float sn, float scale, float R, float cx0, float cy0) {
        float dx = p.x - me.x, dz = p.z - me.z;
        float x = (dx * cs - dz * sn) * scale, z = (dx * sn + dz * cs) * scale;
        float l = (float) Math.sqrt(x * x + z * z);
        if (l > R - 4) { x = x / l * (R - 4); z = z / l * (R - 4); }
        return new float[]{cx0 + x, cy0 + z};
    }

    boolean strikeInput() { return strike != null && strike.active && !strike.paused && panel.isEmpty() && (strike.st.equals("fly") || strike.st.equals("boom") || strike.st.equals("clear")); }

    void strikePause() {
        if (strike == null || !strike.active || strike.paused) return;
        strike.pause(); touches.clear(); joyId = -1; mbBoost = false;
        show("spause");
    }

    void strikeKeys() {
        ru.shitneba.Strike S = strike;
        float x = 0, y = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) x -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) x += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) y += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) y -= 1;
        if (joyId < 0 && !S.bot) { S.inX = x; S.inY = y; }
        boolean tb = false, tbr = false;
        for (String w : touches.values()) { if (w.equals("boost")) tb = true; if (w.equals("brake")) tbr = true; }
        if (!S.bot) {
            S.inBoost = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT) || mbBoost || tb;
            S.inBrake = Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT) || tbr;
        }
    }

    boolean strikeTouchDown(float x, float y, int ptr, int button) {
        ru.shitneba.Strike S = strike;
        if (DESK) {
            if (!Gdx.input.isCursorCatched()) {
                if (button == Input.Buttons.LEFT) {
                    if (in(x, y, PAUSE_R[0], PAUSE_R[1], PAUSE_R[2], PAUSE_R[3])) strikePause();
                    else { float[] c = sCam(); if (in(x, y, c[0], c[1], c[2], c[3])) S.toggleCam(); else mouseFree = false; }
                }
            } else if (button == Input.Buttons.LEFT) mbBoost = true;
            else if (button == Input.Buttons.RIGHT) S.useFlares();
            else if (button == Input.Buttons.MIDDLE) S.dropBomb();
            return true;
        }
        String w = "look";
        if (dst(x, y, sBoost()) < 64) w = "boost";
        else if (dst(x, y, sFlare()) < 46) { w = "ui"; S.useFlares(); }
        else if (dst(x, y, sDet()) < 42) { w = "ui"; S.detonateNow(); }
        else if (!S.S.wing && dst(x, y, sBrake()) < 42) w = "brake";
        else if (S.S.bombs > 0 && dst(x, y, sBomb()) < 44) { w = "ui"; S.dropBomb(); }
        else if (in(x, y, PAUSE_R[0], PAUSE_R[1], PAUSE_R[2], PAUSE_R[3])) { w = "ui"; strikePause(); return true; }
        else { float[] c = sCam(); if (in(x, y, c[0] - 8, c[1] - 8, c[2] + 16, c[3] + 16)) { w = "ui"; S.toggleCam(); }
               else if (x < sx() * 0.45f && joyId < 0) { w = "joy"; joyId = ptr; joyO.set(x, y); joyP.set(x, y); } }
        touches.put(ptr, w);
        lastTouch.put(ptr, new Vector2(x, y));
        return true;
    }

    static float dst(float x, float y, float[] c) { return Vector2.dst(x, y, c[0], c[1]); }

    boolean strikeDrag(float x, float y, int ptr) {
        ru.shitneba.Strike S = strike;
        String w = touches.get(ptr);
        Vector2 lt = lastTouch.get(ptr);
        if ("joy".equals(w)) {
            joyP.set(x, y);
            float vx = (joyP.x - joyO.x) / 75f, vy = (joyP.y - joyO.y) / 75f;
            float l = (float) Math.sqrt(vx * vx + vy * vy);
            if (l > 1) { vx /= l; vy /= l; }
            S.inX = vx; S.inY = -vy;
        } else if ("look".equals(w) && lt != null) {
            float k = 0.0032f * G.SV.sens;
            S.mdX += (x - lt.x) * k; S.mdY -= (y - lt.y) * k;
        }
        if (lt != null) lt.set(x, y);
        return true;
    }

    boolean strikeUp(int ptr, int button) {
        if (DESK && button == Input.Buttons.LEFT) mbBoost = false;
        if (ptr == joyId) { joyId = -1; strike.inX = 0; strike.inY = 0; }
        touches.remove(ptr); lastTouch.remove(ptr);
        return true;
    }

    boolean strikeKey(int kc) {
        ru.shitneba.Strike S = strike;
        switch (kc) {
            case Input.Keys.SPACE: case Input.Keys.F: S.useFlares(); return true;
            case Input.Keys.E: case Input.Keys.B: S.dropBomb(); return true;
            case Input.Keys.X: S.detonateNow(); return true;
            case Input.Keys.C: case Input.Keys.V: S.toggleCam(); return true;
            case Input.Keys.TAB: mouseFree = !mouseFree; return true;
            case Input.Keys.ESCAPE: case Input.Keys.P: strikePause(); return true;
            default: return false;
        }
    }

    // =========================================================
    //                         ДУЭЛЬ
    // =========================================================
    String duelView = "main";
    float duelResT;
    boolean dLost, dWin; String dReason = "";
    String ipText = "";

    boolean bossRole() { return g.mode.equals("duel") && g.duelRole.equals("boss"); }

    void openDuel() {
        duelView = "main";
        if (duel != null) duel.stop();
        show("duel");
    }

    static String pickName(String role, String k) {
        if (k == null || k.isEmpty()) return "—";
        if (role.equals("boss")) { JsonValue b = G.D.get("BOSS_INFO").get(k); return b != null ? b.getString("short") : k; }
        return G.vehName(k);
    }

    String pickGrid(String role, String cur) {
        String[] list = role.equals("boss") ? ru.shitneba.Game.DUEL_BOSSES : ru.shitneba.Duel.VEHS;
        int cols = Math.max(2, Math.min(5, (int) (cw / 120)));
        float gp = 6, w = (cw - gp * (cols - 1)) / cols;
        String res = null;
        for (int i = 0; i < list.length; i++) {
            float x = cx + (i % cols) * (w + gp), y = cy + (i / cols) * (38 + gp);
            if (button(x, y, w, 38, pickName(role, list[i]), list[i].equals(cur) ? BUY : NORMAL, true, 13)) res = list[i];
        }
        cy += ((list.length + cols - 1) / cols) * (38 + gp) + 4;
        if (role.equals("boss") && cur != null && !cur.isEmpty())
            label("Особое умение: " + ru.shitneba.Game.SPEC_NAME.get(cur), 13, C_TEAL);
        return res;
    }

    void hostWith(String role, String pick) {
        ru.shitneba.Duel D = duel;
        if (!D.hosting() || !D.isHost) {
            String err = D.host(role, pick);
            if (!err.isEmpty()) { toast(err); duelView = "main"; }
        } else { D.myRole = role; D.myPick = pick; D.theirReady = false; D.hostPickChanged(); }
    }

    void pDuel() {
        final ru.shitneba.Duel D = duel;
        frame("duel", 660, false, true, () -> {
            title("Дуэль по Wi‑Fi");
            if (duelView.equals("main")) {
                label("Один игрок — расчёт ПВО, другой — босс. Оба устройства должны быть в одной Wi‑Fi сети. Босс побеждает, если разрушит комплекс. ПВО — если собьёт босса или продержится 3 минуты.", 14, C_MUT, Draw.MAIN, 0);
                if (btn("Создать игру", 50, BIG, true)) { duelView = "host"; hostWith("pvo", "pantsir"); }
                if (btn("Найти игру", 46, NORMAL, true)) { duelView = "join"; D.stop(); D.startDiscovery(); }
            } else if (duelView.equals("host")) {
                label("Ваш адрес в сети: " + ru.shitneba.Duel.localIp(), 15, C_TEAL, Draw.TITLE, 0);
                section("Ваша роль");
                int r = btnRow(new String[]{"Я — ПВО", "Я — босс"}, new int[]{D.myRole.equals("pvo") ? BIG : NORMAL, D.myRole.equals("boss") ? BIG : NORMAL}, null, 42);
                if (r == 0 && !D.myRole.equals("pvo")) hostWith("pvo", "pantsir");
                if (r == 1 && !D.myRole.equals("boss")) hostWith("boss", "gunship");
                section("Выбор: " + (D.myRole.equals("boss") ? "ваш босс" : "ваша машина"));
                String k = pickGrid(D.myRole, D.myPick);
                if (k != null) { D.myPick = k; D.hostPickChanged(); }
                String st;
                if (!D.connected) st = "Ждём соперника… Пусть он нажмёт «Найти игру» на своём устройстве.";
                else if (!D.theirReady) st = "Соперник подключился и выбирает " + (D.myRole.equals("boss") ? "машину" : "босса") + "…";
                else st = "Соперник готов: " + pickName(D.myRole.equals("boss") ? "pvo" : "boss", D.theirPick);
                label(st, 14, D.canStart() ? C_ACC : C_MUT, Draw.MAIN, 0);
                if (btn("Начать бой", 50, BIG, D.canStart())) D.begin();
                if (btn("Отмена", 38, NORMAL, true)) openDuel();
            } else {
                if (!D.connected) {
                    section("Игры в сети");
                    java.util.List<java.util.Map.Entry<String, String[]>> list;
                    synchronized (D.found) { list = new ArrayList<>(D.found.entrySet()); }
                    if (list.isEmpty()) label("Ищем… Если игра не находится, введите адрес, который показан у создателя игры.", 13, C_MUT);
                    for (java.util.Map.Entry<String, String[]> e : list) {
                        String[] f = e.getValue();
                        String roleTxt = f[1].equals("pvo") ? "он — ПВО, вы — босс" : "он — босс, вы — ПВО";
                        if (btn(f[0] + " · " + roleTxt + " · " + e.getKey(), 42, BUY, true)) { D.join(e.getKey()); toast("Подключаемся к " + e.getKey() + "…"); }
                    }
                    float w = cw - 170;
                    if (!d.off) {
                        box(cx, cy, w, 40, WHITE, 0.05f, C_LINE, 0.4f, 1);
                        d.text(ipText.isEmpty() ? "192.168.1.5" : ipText, cx + 12, cy + 26, 15, ipText.isEmpty() ? C_MUT : C_TXT);
                    }
                    if (tap(cx, cy, w, 40)) {
                        Gdx.input.getTextInput(new Input.TextInputListener() {
                            @Override public void input(String text) { ipText = text.trim(); }
                            @Override public void canceled() { }
                        }, "Адрес создателя игры", ipText, "192.168.1.5");
                    }
                    if (button(cx + w + 8, cy, 162, 40, "Подключиться", NORMAL, true, 0)) {
                        if (ipText.isEmpty()) toast("Введите адрес");
                        else { D.join(ipText); toast("Подключаемся к " + ipText + "…"); }
                    }
                    cy += 48;
                } else {
                    label("Подключено! Ваша роль: " + (D.myRole.equals("boss") ? "БОСС" : "ПВО"), 16, C_TEAL, Draw.TITLE, 0);
                    if (!D.theirPick.isEmpty()) label("Соперник выбрал: " + pickName(D.myRole.equals("pvo") ? "boss" : "pvo", D.theirPick), 14, C_TXT);
                    section("Выбор: " + (D.myRole.equals("boss") ? "ваш босс" : "ваша машина"));
                    String k = pickGrid(D.myRole, D.myPick);
                    if (k != null) D.myPick = k;
                    if (btn("Готов", 50, BIG, true)) { D.sendPick(D.myPick); toast("Ждём, пока создатель начнёт бой"); }
                }
                if (btn("Отмена", 38, NORMAL, true)) openDuel();
            }
        });
    }

    @Override public void duelResult(boolean lostLink, boolean win, String reason) {
        dLost = lostLink; dWin = win; dReason = reason;
        if (!lostLink && win) { G.addXp(25); G.SV.c += 60; g.statAdd("duelw", 1); g.checkAch(); G.save(); }
        G.sfx(win ? "clear" : "fail");
        overDelay = -1;
        duelResT = 0.8f;
    }

    void pDRes() {
        frame("dres", 480, false, true, () -> {
            String t = dLost ? "Бой прерван" : (dWin ? "Победа!" : "Поражение");
            d.text(t.toUpperCase(), cx + cw / 2, cy + 30, 30, dLost ? C_MUT : (dWin ? C_ACC : C_SRED), Draw.WIDE, 0);
            cy += 46;
            label(dReason, 16, C_TXT, Draw.MAIN, 0);
            if (!dLost) label(dWin ? "+60 монет · +25 опыта" : "Попробуйте ещё раз!", 14, C_TEAL, Draw.BOLD, 0);
            boolean rem = duel != null && duel.connected;
            int r = rem ? btnRow(new String[]{"Реванш", "В меню"}, new int[]{BIG, NORMAL}, null, 46) : (btn("В меню", 46, NORMAL, true) ? 1 : -1);
            if (rem && r == 0) rematch();
            else if (r == 1) { if (duel != null) duel.leave(); toMenu(); }
        });
    }

    void rematch() {
        clearComm(); g.quitBattle(); duel.clearVis();
        g.netView = false; g.duelRole = "";
        pending = () -> {
            host.toMenuWorld();
            duel.theirReady = false;
            duelView = duel.isHost ? "host" : "join";
            if (duel.isHost) duel.hostPickChanged();
            show("duel");
        };
        pendingFrames = 1;
    }

    float[] b1C() { return fireC(); }
    float[] b2C() { return misC(); }
    float[] b3C() { return new float[]{sx() - 81, sy() - 81 - 112}; }
    float[] upC() { return new float[]{sx() - 300, sy() - 150}; }
    float[] dnC() { return new float[]{sx() - 300, sy() - 72}; }
    final HashMap<Integer, Float> altBtn = new HashMap<>();

    void bossPress(int i) { if (duel != null) duel.bossPress(i); }

    void cdBtn(float[] c, float r, String label, float cd, float cdmax, Color col) {
        boolean ready = cd <= 0;
        roundBtn(c[0], c[1], r, ready ? col : darker(col, 0.55f), ready ? new Color(Math.min(1, col.r * 1.15f + 0.05f), Math.min(1, col.g * 1.15f + 0.05f), Math.min(1, col.b * 1.15f + 0.05f), 1) : darker(col, 0.45f), false, ready ? new Color(1, 1, 1, 0.5f) : new Color(1, 1, 1, 0.12f));
        d.text(label, c[0], c[1] + (ready ? 5 : -2), r > 50 ? 15 : 12, WHITE, Draw.TITLE, 0);
        if (!ready) {
            d.text(String.format("%.1f", cd), c[0], c[1] + 16, 13, new Color(1, 1, 1, 0.8f), Draw.TITLE, 0);
            d.arc(c[0], c[1], r + 2, -MathUtils.PI / 2, -MathUtils.PI / 2 + MathUtils.PI2 * (1 - cd / Math.max(0.01f, cdmax)), 3, C_ACC, 1);
        }
    }

    static String mmss(float t) { return String.format("%d:%02d", (int) t / 60, (int) t % 60); }

    void drawBossHud() {
        Game G0 = g;
        float W = sx(), H = sy();
        Game.E be = G0.duelBoss();
        rrect(72, 12, 120, 48, C_PANEL, 0.94f, true);
        d.text(mmss(G0.duelT), 132, 46, 26, G0.duelT > 30 ? C_ACC : C_SRED, Draw.TITLE, 0);
        float bw = Math.min(460, W * 0.5f);
        rrect((W - bw) / 2 - 14, 10, bw + 28, 74, C_PANEL, 0.94f, true);
        if (be != null) {
            d.text("ВАШ БОСС: " + G.D.get("BOSS_INFO").get(be.type).getString("name"), W / 2, 26, 13, C("e6d2ff"), Draw.TITLE, 0);
            bar((W - bw) / 2, 32, bw, 10, be.hp / Math.max(0.001f, be.max), be.inv ? C("a4d0ff") : C_BOSS);
        }
        d.text("КОМПЛЕКС ПВО", W / 2, 62, 12, C_MUT, Draw.TITLE, 0);
        bar((W - bw) / 2, 68, bw, 8, G0.hp / Math.max(1, G0.st("maxHp")), C_SRED);
        float dist = be != null ? (float) Math.hypot(be.pos.x, be.pos.z) : 0;
        if (proj(tv2.set(0, 3, 0), sp) && onScreen(sp)) {
            for (int sx2 = -1; sx2 <= 1; sx2 += 2) for (int sy2 = -1; sy2 <= 1; sy2 += 2) {
                float ox = sp.x + sx2 * 16, oy = sp.y + sy2 * 16;
                d.line(ox, oy, ox - sx2 * 8, oy, 2, C_SRED, 1); d.line(ox, oy, ox, oy - sy2 * 8, 2, C_SRED, 1);
            }
            d.text("ЦЕЛЬ · " + (int) dist + " м", sp.x, sp.y - 24, 13, C("ffb0a6"), Draw.TITLE, 0);
        } else edgeArrow(new Vector3(0, 3, 0), C_SRED, "комплекс " + (int) dist + " м");
        d.circle(W / 2, H / 2, 2, WHITE, 0.6f);
        String tp = be != null ? be.type : "boss";
        cdBtn(b1C(), 59, "РАКЕТЫ", G0.bossCd[0], Game.CD_F1, C("d03a28"));
        cdBtn(b2C(), 42, "РОЙ", G0.empT <= 0 ? G0.bossCd[1] : 9.9f, Game.CD_F2, C("2a5bc4"));
        cdBtn(b3C(), 40, Game.SPEC_SHORT.containsKey(tp) ? Game.SPEC_SHORT.get(tp) : "ОСОБОЕ", G0.bossCd[2], Game.specCd(tp), C("7a3cc4"));
        if (G0.bossSpecT > 0) d.text(String.format("%s: %.1f с", Game.SPEC_NAME.get(tp), G0.bossSpecT), W / 2, H * 0.78f, 16, C_TEAL, Draw.TITLE, 0);
        if (G0.empT > 0) d.text("ПОМЕХИ! Вы замедлены, рой недоступен", W / 2, H * 0.72f, 16, C_RANK, Draw.TITLE, 0);
        if (DESK) {
            float[] a = b1C(), b = b2C(), c = b3C();
            d.text("ЛКМ/1", a[0], a[1] + 32, 11, new Color(1, 1, 1, 0.75f), Draw.MAIN, 0);
            d.text("ПКМ/2", b[0], b[1] - 50, 11, new Color(1, 1, 1, 0.75f), Draw.MAIN, 0);
            d.text("Q/3", c[0], c[1] - 48, 11, new Color(1, 1, 1, 0.75f), Draw.MAIN, 0);
            d.text("WASD — полёт · Пробел / C — выше / ниже · мышь — обзор · Esc — пауза", W / 2, H - 14, 12, new Color(1, 1, 1, 0.6f), Draw.MAIN, 0);
            if (!Gdx.input.isCursorCatched() && !host.bot()) d.text("Кликните по экрану, чтобы управлять мышью", W / 2, H * 0.62f, 16, new Color(1, 1, 1, 0.85f), Draw.TITLE, 0);
        }
        if (!DESK || joyId >= 0) {
            if (!tp.equals("walker")) {
                cdBtn(upC(), 30, "ВВЕРХ", 0, 1, C("2f6e63"));
                cdBtn(dnC(), 30, "ВНИЗ", 0, 1, C("2f6e63"));
            }
            float jx = joyId >= 0 ? joyO.x : 120, jy = joyId >= 0 ? joyO.y : H - 120;
            d.ring(jx, jy, 70, 2, WHITE, 0.25f);
            d.circle(joyId >= 0 ? joyP.x : jx, joyId >= 0 ? joyP.y : jy, 26, C_TEAL, 0.45f);
        }
        rrect(PAUSE_R[0], PAUSE_R[1], PAUSE_R[2], PAUSE_R[3], C_PANEL, 0.94f, true);
        d.rect(29, 27, 5, 18, WHITE); d.rect(39, 27, 5, 18, WHITE);
        if (G0.hurtT > 0) d.rect(0, 0, W, H, new Color(0.86f, 0.08f, 0.04f, 1), MathUtils.clamp(G0.hurtT * 0.3f, 0, 0.18f));
        if (bannerT > 0) {
            float ba = MathUtils.clamp(Math.min(bannerT / 0.4f, (3f - bannerT) / 0.3f), 0, 1);
            d.text(bannerS, W / 2, H * 0.3f + 38, 34, new Color(0.85f, 0.7f, 1, ba), Draw.TITLE, 0);
            if (!bannerSub.isEmpty()) d.text(bannerSub, W / 2, H * 0.3f + 68, 16, new Color(1, 0.886f, 0.66f, ba), Draw.MAIN, 0);
        }
    }

    void bossKeys() {
        Game.BossIn b = g.bossIn;
        if (!DESK || host.bot()) return;
        float x = 0, z = 0, y = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) z += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) z -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) x += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) x -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) y += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.C) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) y -= 1;
        if (joyId < 0) { b.mx = x; b.mz = z; }
        if (altBtn.isEmpty()) b.my = y;
    }

    boolean bossTouchDown(float x, float y, int ptr, int button) {
        Game.BossIn b = g.bossIn;
        if (DESK) {
            if (!Gdx.input.isCursorCatched()) {
                if (button == Input.Buttons.LEFT) { if (in(x, y, PAUSE_R[0], PAUSE_R[1], PAUSE_R[2], PAUSE_R[3])) pause(); else mouseFree = false; }
            } else if (button == Input.Buttons.LEFT) bossPress(0);
            else if (button == Input.Buttons.RIGHT) bossPress(1);
            return true;
        }
        String w;
        if (dst(x, y, b1C()) < 64) { bossPress(0); w = "ui"; }
        else if (dst(x, y, b2C()) < 48) { bossPress(1); w = "ui"; }
        else if (dst(x, y, b3C()) < 46) { bossPress(2); w = "ui"; }
        else if (dst(x, y, upC()) < 36) { altBtn.put(ptr, 1f); b.my = 1; w = "ui"; }
        else if (dst(x, y, dnC()) < 36) { altBtn.put(ptr, -1f); b.my = -1; w = "ui"; }
        else if (in(x, y, PAUSE_R[0], PAUSE_R[1], PAUSE_R[2], PAUSE_R[3])) { pause(); w = "ui"; }
        else if (x < sx() * 0.45f && joyId < 0) { joyId = ptr; joyO.set(x, y); joyP.set(x, y); w = "joy"; }
        else w = "cam";
        touches.put(ptr, w);
        lastTouch.put(ptr, new Vector2(x, y));
        return true;
    }

    boolean bossDrag(float x, float y, int ptr) {
        Game.BossIn b = g.bossIn;
        String w = touches.get(ptr);
        Vector2 lt = lastTouch.get(ptr);
        if ("joy".equals(w)) {
            joyP.set(x, y);
            float vx = (joyP.x - joyO.x) / 70f, vy = (joyP.y - joyO.y) / 70f;
            float l = (float) Math.sqrt(vx * vx + vy * vy);
            if (l > 1) { vx /= l; vy /= l; }
            b.mx = vx; b.mz = -vy;
        } else if ("cam".equals(w) && lt != null) {
            float k = 0.0042f * G.SV.sens;
            b.cy -= (x - lt.x) * k;
            g.bossCamPitch = MathUtils.clamp(g.bossCamPitch + (y - lt.y) * k, -0.35f, 0.9f);
        }
        if (lt != null) lt.set(x, y);
        return true;
    }

    boolean bossUp(int ptr) {
        Game.BossIn b = g.bossIn;
        if (ptr == joyId) { joyId = -1; b.mx = 0; b.mz = 0; }
        if (altBtn.remove(ptr) != null) b.my = 0;
        touches.remove(ptr); lastTouch.remove(ptr);
        return true;
    }

    boolean bossKey(int kc) {
        switch (kc) {
            case Input.Keys.Q: case Input.Keys.E: case Input.Keys.F: case Input.Keys.NUM_3: bossPress(2); return true;
            case Input.Keys.NUM_1: bossPress(0); return true;
            case Input.Keys.NUM_2: bossPress(1); return true;
            case Input.Keys.ESCAPE: case Input.Keys.P: pause(); return true;
            case Input.Keys.TAB: mouseFree = !mouseFree; return true;
            default: return false;
        }
    }

    static final LinkedHashMap<String, String> EMPTY = new LinkedHashMap<>();
}
