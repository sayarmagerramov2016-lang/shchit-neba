package ru.shitneba.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import java.util.HashMap;
import java.util.Map;

import ru.shitneba.G;
import ru.shitneba.Game;
import ru.shitneba.Ops;

/**
 * Операторы в интерфейсе: окно «Операторы» (кейсы, рулетка, магазин и коллекция),
 * девчонка рядом с комплексом в бою, кнопка её умения.
 * Картинки — assets/ops (store/tools/make_ops.py): тело, лицо (7 эмоций), указывающая рука, портрет.
 */
public class OpUI {
    final UI u;
    final Draw d;
    final Map<String, TextureRegion> tx = new HashMap<>();
    // из make_ops.py
    static final float IW = 256, IH = 512, FACE_X = 56, FACE_Y = 118, FACE_W = 144, FACE_H = 104;
    static final float ARM_W = 128, ARM_H = 48, ARM_PX = 16, ARM_PY = 24, SH_X = 94, SH_Y = 232;

    public OpUI(UI u) { this.u = u; this.d = u.d; }

    TextureRegion t(String name) {
        TextureRegion r = tx.get(name);
        if (r == null) {
            Texture t;
            try {
                t = new Texture(Gdx.files.internal("ops/" + name + ".png"), true);
                t.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
            } catch (Exception e) {
                ru.shitneba.BlackBox.mark("нет картинки ops/" + name);
                t = d.white;
            }
            r = new TextureRegion(t);
            r.flip(false, true); // камера интерфейса смотрит осью Y вниз
            tx.put(name, r);
        }
        return r;
    }

    static Color rc(int rar) { return UI.C(Ops.RAR_C[rar]); }

    String outfit(Ops.Op o) { return o.id + "_" + (Ops.alt(o.id) ? 1 : 0); }

    /** Портрет в квадрате; dark — затемнить (ещё не открыта). */
    void portrait(Ops.Op o, float x, float y, float s, boolean dark) {
        if (d.off) return;
        TextureRegion r = t(outfit(o) + "_por");
        if (dark) d.b.setColor(0.05f, 0.05f, 0.08f, 0.9f); else d.b.setColor(Color.WHITE);
        d.b.draw(r, x, y, s, s);
        d.b.setColor(Color.WHITE);
    }

    // =========================================================
    //                    девчонка в бою
    // =========================================================
    String emo = "norm"; float emoT, blinkT = 3, jumpT, jumpDur = 0.45f, jumpH, panicT, bubT, bubCd, abilT, sayCd;
    String bubS = "";
    boolean lost;
    float armAng = -135;
    final Array<float[]> parts = new Array<>(); // x, y, vx, vy, life, max, type
    float partAcc;

    public void reset() {
        emo = "norm"; emoT = 0; jumpT = 0; panicT = 0; bubT = 0; bubCd = 0; abilT = 0; lost = false; parts.clear(); sayCd = 0;
    }

    void bubble(String s, Ops.Op o) {
        if (s == null) return;
        bubS = s; bubT = 1.7f + s.length() * 0.03f; bubCd = 2.5f;
        G.sfxP("chirp", 0.45f, o.pitch * (0.92f + MathUtils.random(0.16f)), 120);
    }

    void setEmo(String e, float t) { emo = e; emoT = t; }

    void jump(float dur, float h) { jumpT = dur; jumpDur = dur; jumpH = h; }

    public void event(String ev) {
        Game g = u.g;
        Ops.Op o = g.op;
        if (o == null) return;
        switch (ev) {
            case "hit":
                setEmo("panic", 0.9f); panicT = 0.6f; jump(0.25f, 6);
                if (bubCd <= 0 && MathUtils.random() < 0.4f) bubble(Ops.say("hit"), o);
                break;
            case "kill":
                if (!emo.equals("panic") || emoT <= 0) { setEmo("joy", 0.7f); jump(0.45f, 14); }
                burst(8);
                if (bubCd <= 0) {
                    if (Ops.level(o.id) >= 3 && MathUtils.random() < 0.1f) bubble(Ops.say("warm"), o);
                    else if (MathUtils.random() < 0.22f) bubble(Ops.say("kill"), o);
                }
                break;
            case "boss":
                setEmo("joy", 2.4f); jump(1.8f, 26); burst(30);
                bubble(Ops.say("boss"), o);
                break;
            case "wave":
                setEmo("shout", 1.1f); bubble(Ops.say("wave"), o);
                break;
            case "low":
                setEmo("worry", 1.5f); bubble(Ops.say("low"), o);
                break;
            case "lose":
                lost = true; bubble(Ops.say("lose"), o);
                break;
            case "abil":
                setEmo("shout", 1.2f); jump(0.4f, 12); abilT = 1.5f; burst(20);
                bubble(o.abil + "!", o);
                G.sfx("wave", 0.5f);
                break;
            case "save":
                setEmo("shout", 1.5f); jump(0.5f, 18); burst(40); abilT = 1.5f;
                bubble(Ops.say("save"), o);
                break;
            default: break;
        }
    }

    float gx, gy, gk; // левый верх девчонки и масштаб — для частиц и пузыря

    /** Девчонка справа от комплекса: дышит, качается, показывает рукой на прицел. */
    public void drawGirl(float W, float H, float dt) {
        Game g = u.g;
        Ops.Op o = g.op;
        if (o == null) return;
        float T = u.T;
        emoT -= dt; jumpT = Math.max(0, jumpT - dt); panicT = Math.max(0, panicT - dt); bubT -= dt; bubCd -= dt; abilT -= dt;
        blinkT -= dt;
        float gh = Math.min(H * 0.44f, 300), k = gh / IH;
        float cxm = W * 0.6f;
        float lift = 0;
        if (jumpT > 0) {
            float p = 1 - jumpT / jumpDur;
            int hops = jumpDur > 1 ? 3 : 1;
            lift = Math.abs(MathUtils.sin(p * MathUtils.PI * hops)) * jumpH * (1 - p * 0.3f);
        }
        float shx = panicT > 0 ? MathUtils.sin(T * 60) * 3 * (panicT / 0.6f) : 0;
        float breath = 1 + 0.012f * MathUtils.sin(T * 2.2f);
        float sway = 1.2f * MathUtils.sin(T * 1.1f);
        float x0 = cxm - IW * k / 2 + shx, y0 = H - 6 - IH * k * breath - lift;
        gx = x0; gy = y0; gk = k;
        // сияние за спиной у легендарок и мифика
        if (o.rar >= 3) {
            Color gc = rc(o.rar);
            float pr = 0.7f + 0.3f * MathUtils.sin(T * 2.5f);
            for (int i = 4; i >= 1; i--) d.circle(cxm, y0 + 190 * k, (70 + i * 26) * k, gc, 0.06f * pr);
        }
        // эмоция
        String e;
        if (emoT > 0) e = emo;
        else if (lost) e = "cry";
        else if (g.hp < g.st("maxHp") * 0.3f && g.hp > 0) e = "worry";
        else e = "norm";
        if (blinkT < 0) { if (blinkT < -0.12f) blinkT = 2.5f + MathUtils.random(2.5f); else if (e.equals("norm") || e.equals("worry")) e = "blink"; }
        String base = outfit(o);
        // тело (качается вокруг ног)
        float ox = IW * k / 2, oy = IH * k * breath;
        d.b.setColor(Color.WHITE);
        d.b.draw(t(base + "_body"), x0, y0, ox, oy, IW * k, IH * k * breath, 1, 1, sway);
        // лицо — с тем же поворотом вокруг той же точки
        float fx = x0 + FACE_X * k, fy = y0 + FACE_Y * k * breath;
        d.b.draw(t(o.id + "_face_" + e), fx, fy, ox - FACE_X * k, oy - FACE_Y * k * breath, FACE_W * k, FACE_H * k * breath, 1, 1, sway);
        // рука на прицел
        float sxp = x0 + SH_X * k, syp = y0 + SH_Y * k * breath;
        float tgt = -135;
        if (g.state.equals("run") && u.proj(g.aimPoint, u.sp2)) tgt = MathUtils.atan2(u.sp2.y - syp, u.sp2.x - sxp) * MathUtils.radDeg;
        if (lost) tgt = 110;
        tgt = lost ? tgt : MathUtils.clamp(tgt, -175, -15);
        armAng += (tgt - armAng) * Math.min(1, dt * 8);
        d.b.draw(t(base + "_arm"), sxp - ARM_PX * k, syp - ARM_PY * k, ARM_PX * k, ARM_PY * k, ARM_W * k, ARM_H * k, 1, 1, armAng);
        // частицы
        if (o.rar >= 2) {
            partAcc += dt * (o.rar >= 3 ? 7 : 4);
            while (partAcc > 1) { partAcc -= 1; spawn(cxm + MathUtils.random(-90, 90) * k, y0 + MathUtils.random(40, 480) * k, false); }
        }
        drawParts(dt, o);
        // пузырь с репликой
        if (bubT > 0) {
            float a = MathUtils.clamp(Math.min(bubT / 0.25f, 1), 0, 1);
            float fs = 14, tw = Math.min(d.textW(bubS, fs, Draw.BOLD), 240);
            float bw = tw + 22, bh = 30, bx = MathUtils.clamp(cxm - bw / 2 + 30 * k, 8, W - bw - 8), by = y0 + 20 * k - bh - 6;
            d.rect(bx, by, bw, bh, Color.WHITE, 0.95f * a);
            d.frame(bx, by, bw, bh, 2, rc(o.rar), a);
            d.tri(cxm + 6 * k, by + bh, cxm + 26 * k, by + bh, cxm + 4 * k, by + bh + 10, Color.WHITE, 0.95f * a);
            d.text(bubS, bx + bw / 2, by + bh / 2 + fs * 0.36f, fs, new Color(0.15f, 0.1f, 0.15f, a), Draw.BOLD, 0);
        }
        // название умения крупно
        if (abilT > 0) {
            float a = MathUtils.clamp(Math.min(abilT / 0.3f, (1.5f - abilT) / 0.15f), 0, 1);
            float sc = 1 + (1.5f - abilT) * 0.06f;
            String s = o.abil.toUpperCase();
            Color c = rc(o.rar);
            d.text(s, W / 2 + 2, H * 0.36f + 2, 36 * sc, new Color(0, 0, 0, 0.6f * a), Draw.WIDE, 0);
            d.text(s, W / 2, H * 0.36f, 36 * sc, new Color(c.r, c.g, c.b, a), Draw.WIDE, 0);
            d.text(o.name, W / 2, H * 0.36f + 26, 15, new Color(1, 1, 1, 0.9f * a), Draw.BOLD, 0);
        }
    }

    void spawn(float x, float y, boolean burst) {
        if (parts.size > 120) return;
        float sp = burst ? 140 : 20;
        float a = MathUtils.random(MathUtils.PI2);
        float life = burst ? MathUtils.random(0.6f, 1.2f) : MathUtils.random(1.2f, 2.4f);
        parts.add(new float[]{x, y, MathUtils.cos(a) * sp * MathUtils.random(0.4f, 1f), MathUtils.sin(a) * sp * MathUtils.random(0.4f, 1f) - (burst ? 40 : 10), life, life, MathUtils.random(1000)});
    }

    void burst(int n) {
        Ops.Op o = u.g.op;
        if (o == null || o.rar < 2 || gk == 0) return;
        for (int i = 0; i < n; i++) spawn(gx + (128 + MathUtils.random(-60, 60)) * gk, gy + MathUtils.random(60, 300) * gk, true);
    }

    void drawParts(float dt, Ops.Op o) {
        for (int i = parts.size - 1; i >= 0; i--) {
            float[] p = parts.get(i);
            p[4] -= dt;
            if (p[4] <= 0) { parts.removeIndex(i); continue; }
            p[0] += p[2] * dt; p[1] += p[3] * dt;
            p[2] *= 1 - dt * 1.5f; p[3] = p[3] * (1 - dt * 1.5f) + (o.id.equals("akira") || o.id.equals("yuki") || o.id.equals("sneg") ? 30 : -6) * dt;
            float a = MathUtils.clamp(p[4] / p[5] * 1.5f, 0, 1), x = p[0], y = p[1], ph = p[6];
            switch (o.id) {
                case "rin": {
                    Color c = ph % 2 < 1 ? UI.C("ff4fd8") : UI.C("3ff0ff");
                    d.circle(x, y, 2.2f, c, a); d.circle(x, y, 5, c, 0.25f * a);
                    break;
                }
                case "yuki": case "sneg":
                    for (int j = 0; j < 3; j++) {
                        float an = j * MathUtils.PI / 3 + u.T + ph;
                        d.line(x - MathUtils.cos(an) * 4, y - MathUtils.sin(an) * 4, x + MathUtils.cos(an) * 4, y + MathUtils.sin(an) * 4, 1.4f, Color.WHITE, a);
                    }
                    break;
                case "elka": {
                    String[] cs = {"ff4040", "ffd23a", "40a0ff", "ff60e0", "40ff80"};
                    float bl = 0.5f + 0.5f * MathUtils.sin(u.T * 8 + ph);
                    d.circle(x, y, 2.8f, UI.C(cs[(int) ph % 5]), a * (0.4f + 0.6f * bl));
                    break;
                }
                case "akira":
                    d.circle(x, y, 3.2f, UI.C("ffb0c8"), a); d.circle(x + 1.5f, y - 1, 2, UI.C("ffd8e4"), a);
                    break;
                case "valk":
                    d.star(x, y, 4, UI.C("ffd86a"), a);
                    break;
                default:
                    d.circle(x, y, 2.5f, rc(o.rar), a);
            }
        }
    }

    // ---------- кнопка умения ----------
    static final float[] BTN = {12, 124, 52, 52};

    public void drawButton() {
        Game g = u.g;
        Ops.Op o = g.op;
        if (o == null) return;
        float x = BTN[0], y = BTN[1], s = BTN[2];
        boolean ready = g.opCd <= 0;
        d.rect(x, y, s, s, UI.C_PANEL, 0.94f);
        portrait(o, x + 2, y + 2, s - 4, false);
        if (ready) {
            float p = 0.6f + 0.4f * MathUtils.sin(u.T * 5);
            d.frame(x - 2, y - 2, s + 4, s + 4, 3, UI.C("ffc83a"), p);
            d.frame(x - 5, y - 5, s + 10, s + 10, 2, UI.C("ffc83a"), 0.3f * p);
        } else {
            d.rect(x, y, s, s, Color.BLACK, 0.55f);
            float k = 1 - g.opCd / Math.max(1, g.opCdMax);
            d.arc(x + s / 2, y + s / 2, s * 0.42f, -MathUtils.PI / 2, -MathUtils.PI / 2 + MathUtils.PI2 * k, 3, UI.C_TEAL, 0.9f);
            d.text(String.valueOf((int) Math.ceil(g.opCd)), x + s / 2, y + s / 2 + 7, 19, Color.WHITE, Draw.TITLE, 0);
            d.frame(x, y, s, s, 1, UI.C_LINE, 0.3f);
        }
        if (u.DESK) d.text("Q", x + s - 4, y + s - 4, 11, Color.WHITE, Draw.BOLD, 1);
    }

    public boolean hitButton(float x, float y) {
        if (u.g.op == null) return false;
        return UI.in(x, y, BTN[0] - 4, BTN[1] - 4, BTN[2] + 8, BTN[3] + 8);
    }

    // =========================================================
    //                  окно «Операторы»
    // =========================================================
    public int tab;

    /** Для автотеста: открыть кейс по номеру. */
    public void debugRoll(int i) { startRoll(Ops.CASES[i]); }

    public void pOps() {
        u.frame("ops", 820, false, true, () -> {
            u.title("Операторы");
            Ops.Op s = Ops.sel();
            if (!d.off) {
                d.text("Монеты: " + G.SV.c, u.cx, u.cy + 15, 15, UI.C_ACC, Draw.BOLD, -1);
                String nm = s == null ? "нет" : s.name;
                float w0 = d.textW(nm, 15, Draw.BOLD);
                d.text(nm, u.cx + u.cw, u.cy + 15, 15, s == null ? UI.C_MUT : rc(s.rar), Draw.BOLD, 1);
                d.text("Оператор: ", u.cx + u.cw - w0 - 4, u.cy + 15, 15, UI.C_MUT, Draw.MAIN, 1);
            }
            u.gap(26);
            tab = u.tabs(new String[]{"Кейсы", "Магазин и коллекция"}, tab);
            if (tab == 0) cases(); else shop();
        });
    }

    void cases() {
        int cols = Math.max(1, Math.min(4, (int) (u.cw / 175)));
        float gp = 8, w = (u.cw - gp * (cols - 1)) / cols, h = 236;
        for (int i = 0; i < Ops.CASES.length; i++) {
            Ops.Case c = Ops.CASES[i];
            float x = u.cx + (i % cols) * (w + gp), y = u.cy + (i / cols) * (h + gp);
            caseCard(c, x, y, w, h);
        }
        u.cy += ((Ops.CASES.length + cols - 1) / cols) * (h + gp) + 4;
        int left = Math.max(1, Ops.PITY - G.SV.opPity);
        u.label(String.format("До гарантированной легендарки: %d · открыто кейсов: %d · собрано: %d из %d",
                left, G.SV.opCases, Ops.collected(), Ops.ALL.length), 13, UI.C_MUT, Draw.MAIN, 0);
        u.label("Повторка разбирается в монеты: обычная 40 · редкая 100 · эпическая 250 · легендарная 600 · мифическая 1500. Ёлка и Снегурочка — только в Новогоднем кейсе.", 12, UI.C_MUT, Draw.MAIN, 0);
    }

    void crate(float x, float y, float w, float h, Color c) {
        float bw = Math.min(w * 0.5f, 90), bh = bw * 0.62f, bx = x + (w - bw) / 2, by = y + (h - bh) / 2 + 6;
        d.rect(bx, by, bw, bh, c, 1);
        d.rect(bx, by, bw, bh * 0.3f, darker(c, 0.25f), 1);
        d.frame(bx, by, bw, bh, 2, darker(c, 0.5f), 1);
        d.rect(bx + bw / 2 - 5, by, 10, bh, darker(c, 0.4f), 1);
        d.rect(bx + bw / 2 - 7, by + bh * 0.3f - 4, 14, 10, UI.C("e8c25a"), 1);
        d.line(bx + 6, by + bh * 0.45f, bx + bw * 0.4f, by + bh * 0.45f, 1, Color.WHITE, 0.25f);
    }

    static Color darker(Color c, float k) { return new Color(c.r * (1 - k), c.g * (1 - k), c.b * (1 - k), 1); }

    void caseCard(Ops.Case c, float x, float y, float w, float h) {
        Color cc = UI.C(c.col);
        boolean free = c.id.equals("free");
        boolean can = free ? Ops.freeReady() : G.SV.c >= c.price;
        if (!d.off) {
            d.rect(x, y, w, h, cc, 0.1f);
            d.frame(x, y, w, h, free ? 2 : 1, cc, free ? 0.9f : 0.55f);
            d.text((free ? "ПОДАРОК" : c.name.toUpperCase()), x + w / 2, y + 22, 15, free ? UI.C("7dffb0") : Color.WHITE, Draw.WIDE, 0);
            if (free) d.text("бесплатно раз в сутки", x + w / 2, y + 38, 11, UI.C_MUT, Draw.MAIN, 0);
            crate(x, y + 30, w, 80, cc);
            float yy = y + 128;
            for (int r = 0; r < 5; r++) {
                if (c.odds[r] <= 0) continue;
                String pc = (c.odds[r] == (int) c.odds[r] ? String.valueOf((int) c.odds[r]) : String.valueOf(c.odds[r]).replace('.', ',')) + "%";
                d.text(Ops.RAR_N[r], x + 12, yy, 12, rc(r), Draw.MAIN, -1);
                d.text(pc, x + w - 12, yy, 12, rc(r), Draw.BOLD, 1);
                yy += 15;
            }
        }
        String bt = free ? (Ops.freeReady() ? "Открыть" : "Завтра") : "Открыть · " + c.price;
        if (u.button(x + 10, y + h - 44, w - 20, 34, bt, UI.BUY, can, 14)) startRoll(c);
    }

    // ---------- рулетка ----------
    Ops.Case rollCase;
    Ops.Result res;
    Ops.Op[] strip;
    float rollT;
    static final float ROLL = 5.6f;
    int lastTick = -1;
    float stripJit;

    void startRoll(Ops.Case c) {
        Ops.Result r = Ops.open(c);
        if (r == null) { u.toast(c.id.equals("free") ? "Подарок уже открыт — завтра будет новый" : "Не хватает монет"); return; }
        rollCase = c; res = r; strip = Ops.strip(c, r.op); rollT = 0; lastTick = -1;
        stripJit = MathUtils.random(-0.35f, 0.35f);
        u.backTo.put("oproll", "ops");
        u.show("oproll");
    }

    static float ease(float t) { return 1 - (float) Math.pow(1 - t, 3.2); }

    public void pRoll() {
        float W = d.w, H = d.h;
        float dt = Math.min(0.05f, Gdx.graphics.getDeltaTime());
        if (!d.off) {
            d.rect(0, 0, W, H, UI.C("05090a"), 0.94f);
            u.grid(0.05f);
        }
        rollT += dt;
        boolean done = rollT >= ROLL;
        float it = Math.min(100, H * 0.24f), gp = 8, cell = it + gp;
        float pos = ease(Math.min(1, rollT / ROLL)) * (Ops.WIN + 0.5f + stripJit * 0.8f) * cell; // смещение ленты в пикселях
        if (done) pos = (Ops.WIN + 0.5f + stripJit * 0.8f) * cell;
        int idx = (int) (pos / cell);
        if (idx != lastTick && !done) { lastTick = idx; G.sfx("tick", 0.5f, 25); G.hap(8); }
        float my = H * 0.3f, mx = W / 2;
        if (!d.off) {
            d.text(rollCase.name.toUpperCase() + " КЕЙС", mx, my - 26, 20, UI.C(rollCase.col), Draw.WIDE, 0);
            d.rect(0, my - 6, W, it + 12, Color.BLACK, 0.5f);
            for (int i = 0; i < strip.length; i++) {
                float x = mx - pos + i * cell;
                if (x + it < -10 || x > W + 10) continue;
                Ops.Op o = strip[i];
                Color c = rc(o.rar);
                d.rect(x, my, it, it, c, 0.18f);
                portrait(o, x + 4, my + 4, it - 8, false);
                d.rect(x, my + it - 5, it, 5, c, 1);
                d.frame(x, my, it, it, 1, c, 0.6f);
            }
            d.gradH(0, my - 6, W * 0.25f, it + 12, Color.BLACK, 0.9f, Color.BLACK, 0);
            d.gradH(W * 0.75f, my - 6, W * 0.25f, it + 12, Color.BLACK, 0, Color.BLACK, 0.9f);
            d.tri(mx - 10, my - 16, mx + 10, my - 16, mx, my - 2, UI.C_ACC, 1);
            d.tri(mx - 10, my + it + 16, mx + 10, my + it + 16, mx, my + it + 2, UI.C_ACC, 1);
            d.rect(mx - 1, my - 4, 2, it + 8, UI.C_ACC, 0.9f);
        }
        if (!done) {
            if (u.button(mx - 80, my + it + 40, 160, 40, "Пропустить", UI.NORMAL, true, 15)) rollT = ROLL;
            return;
        }
        if (rollT - dt < ROLL) {
            boolean big = res.op.rar >= 3;
            G.sfx(big ? "boss" : "clear", big ? 0.9f : 0.6f);
            if (G.SV.vib && G.isMobile()) try { Gdx.input.vibrate(big ? 400 : 60); } catch (Throwable ignored) { }
        }
        // карточка выигрыша
        Ops.Op o = res.op;
        Color c = rc(o.rar);
        float cw = Math.min(420, W - 40), ch = Math.min(H - (my + it + 24) - 12, 240), cx = mx - cw / 2, cy = my + it + 20;
        float a = MathUtils.clamp((rollT - ROLL) / 0.25f, 0, 1);
        if (!d.off) {
            if (o.rar >= 3) for (int i = 3; i >= 1; i--) d.circle(mx, my + it / 2, it * (0.6f + i * 0.25f), c, 0.06f * a);
            d.rect(cx, cy, cw, ch, UI.C_PANEL, 0.96f * a);
            d.frame(cx, cy, cw, ch, 2, c, a);
            float ps = Math.min(ch - 70, 120);
            portrait(o, cx + 12, cy + 12, ps, false);
            float tx = cx + 24 + ps;
            d.text(o.name, tx, cy + 34, 24, Color.WHITE, Draw.TITLE, -1);
            d.rect(tx, cy + 44, d.textW(Ops.RAR_N[o.rar], 12, Draw.BOLD) + 14, 20, c, 0.9f);
            d.text(Ops.RAR_N[o.rar], tx + 7, cy + 59, 12, UI.C("0a0a0a"), Draw.BOLD, -1);
            d.text(res.isNew ? "Новый оператор!" : "Уже есть — разобрана на +" + res.dupe, tx, cy + 88, 15, res.isNew ? UI.C_GREEN : UI.C_ACC, Draw.BOLD, -1);
            if (res.pity) d.text("Сработал гарант", tx, cy + 106, 12, UI.C_ACC, Draw.MAIN, -1);
            d.wrap(o.abil + ": " + o.abDesc, tx, cy + 114, cx + cw - tx - 12, 12, UI.C_MUT, Draw.MAIN, -1);
        }
        float bw = (cw - 24 - 12) / 3, by = cy + ch - 46;
        boolean canMore = rollCase.id.equals("free") ? false : G.SV.c >= rollCase.price;
        if (u.button(cx + 12, by, bw, 36, rollCase.id.equals("free") ? "Ещё (300)" : "Ещё · " + rollCase.price, UI.BUY,
                rollCase.id.equals("free") ? G.SV.c >= 300 : canMore, 13)) {
            startRoll(rollCase.id.equals("free") ? Ops.CASES[1] : rollCase);
            return;
        }
        boolean isSel = o.id.equals(G.SV.opSel);
        if (u.button(cx + 18 + bw, by, bw, 36, isSel ? "Выбрана" : "Выбрать", UI.NORMAL, !isSel, 13)) { G.SV.opSel = o.id; G.save(); u.toast(o.name + " — твой оператор"); }
        if (u.button(cx + 24 + bw * 2, by, bw, 36, "Готово", UI.BIG, true, 14)) u.show("ops");
    }

    // ---------- магазин и коллекция ----------
    void shop() {
        int cols = Math.max(1, Math.min(4, (int) (u.cw / 180)));
        float gp = 8, w = (u.cw - gp * (cols - 1)) / cols, h = 318;
        int n = Ops.ALL.length + 1;
        for (int i = 0; i < n; i++) {
            float x = u.cx + (i % cols) * (w + gp), y = u.cy + (i / cols) * (h + gp);
            if (!u.inView(y) && !u.inView(y + h) && !d.off) continue;
            if (i == 0) noneCard(x, y, w, h); else opCard(Ops.ALL[i - 1], x, y, w, h);
        }
        u.cy += ((n + cols - 1) / cols) * (h + gp);
    }

    void noneCard(float x, float y, float w, float h) {
        boolean sel = G.SV.opSel.isEmpty();
        if (!d.off) {
            d.rect(x, y, w, h, Color.WHITE, 0.04f);
            d.frame(x, y, w, h, sel ? 2 : 1, sel ? UI.C_ACC : UI.C_LINE, sel ? 1 : 0.25f);
            d.circle(x + w / 2, y + 90, 44, Color.WHITE, 0.06f);
            d.line(x + w / 2 - 22, y + 68, x + w / 2 + 22, y + 112, 3, UI.C_MUT, 0.6f);
            d.text("Без оператора", x + w / 2, y + 170, 15, UI.C_TXT, Draw.BOLD, 0);
            d.wrap("Снять текущую девчонку. Бой без умения и без бонусов.", x + 12, y + 182, w - 24, 12, UI.C_MUT, Draw.MAIN, 0);
        }
        if (u.button(x + 10, y + h - 44, w - 20, 34, sel ? "Выбрано" : "Выбрать", UI.BUY, !sel, 14)) { G.SV.opSel = ""; G.save(); }
    }

    void opCard(Ops.Op o, float x, float y, float w, float h) {
        boolean own = Ops.owned(o.id), sel = o.id.equals(G.SV.opSel);
        Color c = rc(o.rar);
        int lv = Ops.level(o.id);
        if (!d.off) {
            d.rect(x, y, w, h, c, own ? 0.07f : 0.03f);
            d.frame(x, y, w, h, sel ? 3 : 1.5f, c, own ? 0.9f : 0.4f);
            float ps = Math.min(w - 24, 110);
            portrait(o, x + (w - ps) / 2, y + 8, ps, !own);
            float yy = y + 8 + ps + 18;
            d.text(o.name, x + 12, yy, 16, own ? Color.WHITE : UI.C_MUT, Draw.TITLE, -1);
            float bx = x + 12;
            yy += 6;
            float rw = d.textW(Ops.RAR_N[o.rar], 11, Draw.BOLD) + 10;
            d.rect(bx, yy, rw, 16, c, 0.9f);
            d.text(Ops.RAR_N[o.rar], bx + 5, yy + 12, 11, UI.C("0a0a0a"), Draw.BOLD, -1);
            if (o.ny) {
                float nx = bx + rw + 4, nw = d.textW("Новый год", 11, Draw.BOLD) + 10;
                d.rect(nx, yy, nw, 16, UI.C("8fd0ff"), 0.9f);
                d.text("Новый год", nx + 5, yy + 12, 11, UI.C("0a1a2a"), Draw.BOLD, -1);
            }
            yy += 22;
            yy += d.wrap(o.abil + " · " + Math.round(Ops.cdOf(o)) + " с", x + 12, yy, w - 24, 12, UI.C_ACC, Draw.BOLD, -1) + 1;
            yy += d.wrap(o.abDesc, x + 12, yy, w - 24, 11, UI.C_MUT, Draw.MAIN, -1) + 3;
            if (own) {
                d.text(String.format("Дружба %d ур. · монеты +%d%%", lv, Math.round(Ops.coinBonus(o.id) * 100)), x + 12, yy + 10, 11, UI.C_TXT, Draw.MAIN, -1);
                u.bar(x + 12, yy + 14, w - 24, 4, Ops.lvK(o.id), c);
            }
        }
        float by = y + h - 44;
        if (own) {
            float hw = (w - 26) / 2;
            if (lv >= 5) {
                boolean alt = G.SV.opAlt.contains(o.id);
                if (u.button(x + 10, by - 40, w - 20, 32, alt ? "Наряд 2 из 2" : "Наряд 1 из 2", UI.NORMAL, true, 12)) {
                    if (alt) G.SV.opAlt.remove(o.id); else G.SV.opAlt.add(o.id);
                    G.save();
                }
            } else if (!d.off) d.text("Второй наряд — с 5 ур.", x + w / 2, by - 18, 11, UI.C_MUT, Draw.MAIN, 0);
            if (u.button(x + 10, by, w - 20, 34, sel ? "Выбрана" : "Выбрать", UI.BUY, !sel, 14)) { G.SV.opSel = o.id; G.save(); G.sfxP("chirp", 0.5f, o.pitch, 100); }
        } else {
            if (u.button(x + 10, by, w - 20, 34, "Купить · " + o.price, UI.BUY, G.SV.c >= o.price, 14)) {
                G.SV.c -= o.price; G.SV.opOwn.add(o.id); G.SV.opSel = o.id; G.save();
                u.toast(o.name + " в отряде!");
                G.sfx("clear", 0.6f);
            }
        }
    }

    /** Есть что открыть бесплатно — для значка в меню. */
    public static boolean hasFree() { return Ops.freeReady(); }

    public void dispose() {
        for (TextureRegion r : tx.values()) if (r.getTexture() != d.white) r.getTexture().dispose();
        tx.clear();
    }

    static Vector2 tmp = new Vector2();
}
