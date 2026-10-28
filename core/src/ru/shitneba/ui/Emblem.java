package ru.shitneba.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;

import java.util.ArrayList;
import java.util.List;

import ru.shitneba.Bestiary;
import ru.shitneba.G;
import ru.shitneba.Skills;

/**
 * Эмблемы командира (3.10.0): значок игрока в меню, на экране итога и в онлайн-рекордах.
 * Все рисуются кодом и анимированы (блик, вращение, пульс, искры). Открываются за заслуги, выбор — в окне «Командир».
 */
public final class Emblem {
    public static final class Def {
        public final String id, name, req;
        Def(String id, String name, String req) { this.id = id; this.name = name; this.req = req; }
    }

    public static final Def[] ALL = {
            new Def("recruit", "Новобранец", "Есть у всех"),
            new Def("sergeant", "Сержант", "Звание «Сержант»"),
            new Def("officer", "Офицер", "Звание «Лейтенант»"),
            new Def("general", "Генерал", "Звание «Генерал»"),
            new Def("hunter", "Охотник на гигантов", "Сбей 25 боссов"),
            new Def("thousand", "Тысяча", "Сбей 1000 дронов"),
            new Def("untouch", "Неприкасаемый", "20 волн без урона"),
            new Def("daily", "Ветеран вызовов", "Пройди 7 вызовов дня"),
            new Def("swarm", "Повелитель роя", "10 рубежей в «Рое»"),
            new Def("scholar", "Знаток", "Открой весь бестиарий"),
            new Def("commander", "Командир", "Возьми все 3 ульты"),
            new Def("season", "Сезон", "30-й уровень пропуска"),
    };

    public static Def def(String id) { for (Def e : ALL) if (e.id.equals(id)) return e; return ALL[0]; }

    static float st(String k) { Float v = G.SV.stats.get(k); return v == null ? 0 : v; }

    /** Выполнено ли условие (без учёта уже открытых). */
    static boolean cond(String id) {
        switch (id) {
            case "recruit": return true;
            case "sergeant": return G.rankIndex() >= 3;
            case "officer": return G.rankIndex() >= 7;
            case "general": return G.rankIndex() >= 13;
            case "hunter": return st("bosses") >= 25;
            case "thousand": return st("kills") >= 1000;
            case "untouch": return st("nohit") >= 20;
            case "daily": return st("daily") >= 7;
            case "swarm": return G.SV.swBestz >= 10;
            case "scholar": return Bestiary.seenCount() >= Bestiary.ALL.length;
            case "commander": return Skills.ults() >= 3;
            case "season": return G.SV.bpGot.contains(30) || ru.shitneba.Pass.level() >= 30;
            default: return false;
        }
    }

    public static boolean own(String id) { return id.equals("recruit") || G.SV.emOwn.contains(id); }

    /** Проверить условия; вернуть только что открытые эмблемы. */
    public static List<Def> check() {
        List<Def> out = new ArrayList<>();
        for (Def e : ALL) if (!own(e.id) && cond(e.id)) { G.SV.emOwn.add(e.id); out.add(e); }
        if (!out.isEmpty()) G.save();
        return out;
    }

    public static String sel() { return own(G.SV.emSel) ? G.SV.emSel : "recruit"; }

    // ---------------- рисование ----------------
    static Color c(String h) { return Color.valueOf(h); }
    static final Color GOLD = c("ffd27a"), GOLD_D = c("8a6420"), OLIVE = c("56634a"), OLIVE_D = c("2e3527"), TEAL = c("5ff0c8"),
            RED = c("ff5a4a"), RED_D = c("5a1a14"), BLUE = c("7fd6ff"), BLUE_D = c("16314a"), VIO = c("c79bff"), VIO_D = c("2a1747"),
            ORANGE = c("ffb03a"), ORANGE_D = c("4a2a08"), DARK = c("0b1012"), STEEL = c("aeb8bd"), WHITE = Color.WHITE, GREY = c("4a5054");
    static final Color tmp = new Color();

    static float[] shield(float cx, float cy, float r) {
        return new float[]{cx - r * 0.8f, cy - r * 0.85f, cx + r * 0.8f, cy - r * 0.85f, cx + r * 0.8f, cy + r * 0.05f,
                cx + r * 0.45f, cy + r * 0.62f, cx, cy + r * 0.95f, cx - r * 0.45f, cy + r * 0.62f, cx - r * 0.8f, cy + r * 0.05f};
    }

    static float[] ngon(float cx, float cy, float r, int n, float rot) {
        float[] p = new float[n * 2];
        for (int i = 0; i < n; i++) { float a = rot + i * MathUtils.PI2 / n; p[i * 2] = cx + MathUtils.cos(a) * r; p[i * 2 + 1] = cy + MathUtils.sin(a) * r; }
        return p;
    }

    /** Блик: светлая полоса, раз в несколько секунд пробегает по значку. */
    static void shine(Draw d, float cx, float cy, float r, float t, float period) {
        float k = (t % period) / 0.9f;
        if (k > 1) return;
        float x = cx - r * 1.2f + k * r * 2.4f;
        d.quad(x - r * 0.12f, cy - r, d.pack(WHITE, 0f), x + r * 0.05f, cy - r, d.pack(WHITE, 0.35f),
                x - r * 0.25f, cy + r, d.pack(WHITE, 0.35f), x - r * 0.42f, cy + r, d.pack(WHITE, 0f));
    }

    static void chevrons(Draw d, float cx, float cy, float r, int n, Color col) {
        for (int i = 0; i < n; i++) {
            float y = cy - r * 0.18f + i * r * 0.28f - (n - 1) * r * 0.14f;
            d.line(cx - r * 0.42f, y, cx, y + r * 0.22f, r * 0.13f, col, 1);
            d.line(cx, y + r * 0.22f, cx + r * 0.42f, y, r * 0.13f, col, 1);
        }
    }

    static void sparks(Draw d, float cx, float cy, float r, float t, int n, Color col) {
        for (int i = 0; i < n; i++) {
            float a = t * (0.9f + i * 0.13f) + i * MathUtils.PI2 / n, rr = r * (1.02f + 0.06f * MathUtils.sin(t * 3 + i));
            float s = r * (0.05f + 0.03f * (0.5f + 0.5f * MathUtils.sin(t * 6 + i * 2)));
            d.circle(cx + MathUtils.cos(a) * rr, cy + MathUtils.sin(a) * rr, s, col, 0.9f);
        }
    }

    /** Нарисовать эмблему в круге радиуса r. locked — серый силуэт. */
    public static void draw(Draw d, String id, float cx, float cy, float r, float t, boolean locked) {
        if (d.off) return;
        if (locked) {
            d.poly(ngon(cx, cy, r * 0.92f, 6, MathUtils.PI / 6), GREY, 0.35f);
            d.polyline(ngon(cx, cy, r * 0.92f, 6, MathUtils.PI / 6), true, 1.5f, GREY, 0.8f);
            // замок
            d.rect(cx - r * 0.3f, cy - r * 0.02f, r * 0.6f, r * 0.45f, STEEL, 0.7f);
            d.arc(cx, cy - r * 0.05f, r * 0.2f, MathUtils.PI, MathUtils.PI2, r * 0.09f, STEEL, 0.7f);
            return;
        }
        float p = 0.5f + 0.5f * MathUtils.sin(t * 2.6f);
        switch (id) {
            case "sergeant": {
                float[] s = shield(cx, cy, r);
                d.poly(s, OLIVE_D, 1); d.polyline(s, true, r * 0.07f, GOLD_D, 1);
                chevrons(d, cx, cy + r * 0.05f, r, 3, GOLD);
                shine(d, cx, cy, r * 0.85f, t, 3.2f);
                break;
            }
            case "officer": {
                float[] s = shield(cx, cy, r);
                d.poly(s, BLUE_D, 1); d.polyline(s, true, r * 0.07f, STEEL, 1);
                for (int i = 0; i < 12; i++) {
                    float a0 = t * 0.6f + i * MathUtils.PI2 / 12;
                    d.arc(cx, cy - r * 0.05f, r * 0.5f, a0, a0 + 0.3f, r * 0.05f, STEEL, 0.7f);
                }
                d.star(cx, cy - r * 0.05f, r * 0.36f, GOLD, 1);
                shine(d, cx, cy, r * 0.85f, t, 3.6f);
                break;
            }
            case "general": {
                for (int i = 0; i < 16; i++) {
                    float a = t * 0.35f + i * MathUtils.PI2 / 16, l = r * (i % 2 == 0 ? 1.0f : 0.8f);
                    d.line(cx, cy, cx + MathUtils.cos(a) * l, cy + MathUtils.sin(a) * l, r * 0.07f, GOLD, 0.35f + 0.2f * p);
                }
                d.circle(cx, cy, r * 0.62f, GOLD_D, 1);
                d.ring(cx, cy, r * 0.62f, r * 0.06f, GOLD, 1);
                d.star(cx, cy, r * 0.5f, tmp.set(GOLD).lerp(WHITE, 0.3f * p), 1);
                shine(d, cx, cy, r * 0.6f, t, 2.6f);
                break;
            }
            case "hunter": {
                d.circle(cx, cy, r * 0.9f, RED_D, 1);
                d.ring(cx, cy, r * 0.9f, r * 0.07f, RED, 1);
                float a = t * 0.8f;
                for (int i = 0; i < 4; i++) {
                    float b = a + i * MathUtils.PI / 2, c0 = MathUtils.cos(b), s0 = MathUtils.sin(b);
                    d.line(cx + c0 * r * 0.25f, cy + s0 * r * 0.25f, cx + c0 * r * 0.75f, cy + s0 * r * 0.75f, r * 0.08f, WHITE, 0.9f);
                }
                d.ring(cx, cy, r * (0.42f + 0.06f * p), r * 0.06f, RED, 0.9f);
                d.circle(cx, cy, r * 0.09f, RED, 1);
                break;
            }
            case "thousand": {
                float[] o = Draw.oct(cx, cy, r * 0.9f);
                d.poly(o, DARK, 1); d.polyline(o, true, r * 0.07f, TEAL, 1);
                // крылья
                for (int sd = -1; sd <= 1; sd += 2) for (int i = 0; i < 3; i++) {
                    float y = cy - r * 0.05f + i * r * 0.14f;
                    d.line(cx + sd * r * 0.12f, y, cx + sd * r * (0.7f - i * 0.12f), y - r * (0.25f - i * 0.05f), r * 0.08f, TEAL, 0.9f);
                }
                d.text("1000", cx, cy + r * 0.62f, r * 0.34f, WHITE, Draw.BOLD, 0);
                sparks(d, cx, cy, r * 0.85f, t, 5, TEAL);
                break;
            }
            case "untouch": {
                float[] s = shield(cx, cy, r);
                d.poly(s, BLUE_D, 1);
                d.polyline(s, true, r * (0.06f + 0.04f * p), BLUE, 0.6f + 0.4f * p);
                d.ring(cx, cy - r * 0.05f, r * (0.34f + 0.05f * p), r * 0.05f, BLUE, 0.5f + 0.5f * p);
                d.circle(cx, cy - r * 0.05f, r * 0.2f, WHITE, 0.9f);
                shine(d, cx, cy, r * 0.85f, t, 2.4f);
                break;
            }
            case "daily": {
                for (int i = 0; i < 12; i++) {
                    float a = -t * 0.5f + i * MathUtils.PI2 / 12;
                    d.tri(cx + MathUtils.cos(a - 0.12f) * r * 0.62f, cy + MathUtils.sin(a - 0.12f) * r * 0.62f,
                            cx + MathUtils.cos(a + 0.12f) * r * 0.62f, cy + MathUtils.sin(a + 0.12f) * r * 0.62f,
                            cx + MathUtils.cos(a) * r * 0.98f, cy + MathUtils.sin(a) * r * 0.98f, ORANGE, 0.85f);
                }
                d.circle(cx, cy, r * 0.62f, ORANGE_D, 1);
                d.ring(cx, cy, r * 0.62f, r * 0.06f, ORANGE, 1);
                d.text("7", cx, cy + r * 0.24f, r * 0.7f, tmp.set(ORANGE).lerp(WHITE, 0.4f * p), Draw.BOLD, 0);
                break;
            }
            case "swarm": {
                float[] h = ngon(cx, cy, r * 0.92f, 6, MathUtils.PI / 6);
                d.poly(h, DARK, 1); d.polyline(h, true, r * 0.07f, ORANGE, 1);
                // дрон: крестовина с винтами, винты мерцают
                float a = MathUtils.PI / 4;
                for (int i = 0; i < 4; i++) {
                    float b = a + i * MathUtils.PI / 2, x = cx + MathUtils.cos(b) * r * 0.42f, y = cy + MathUtils.sin(b) * r * 0.42f;
                    d.line(cx, cy, x, y, r * 0.09f, STEEL, 1);
                    d.ring(x, y, r * 0.17f, r * 0.04f, ORANGE, 0.4f + 0.6f * (0.5f + 0.5f * MathUtils.sin(t * 20 + i * 1.7f)));
                }
                d.circle(cx, cy, r * 0.15f, ORANGE, 1);
                sparks(d, cx, cy, r * 0.8f, t * 1.6f, 7, ORANGE);
                break;
            }
            case "scholar": {
                d.circle(cx, cy, r * 0.9f, c("06201c"), 1);
                for (float k : new float[]{0.3f, 0.6f, 0.9f}) d.ring(cx, cy, r * k, r * 0.03f, TEAL, 0.45f);
                float a = t * 1.8f;
                d.tri(cx, cy, cx + MathUtils.cos(a) * r * 0.9f, cy + MathUtils.sin(a) * r * 0.9f,
                        cx + MathUtils.cos(a - 0.6f) * r * 0.9f, cy + MathUtils.sin(a - 0.6f) * r * 0.9f, TEAL, 0.3f);
                d.line(cx, cy, cx + MathUtils.cos(a) * r * 0.9f, cy + MathUtils.sin(a) * r * 0.9f, r * 0.05f, TEAL, 1);
                for (int i = 0; i < 5; i++) {
                    float b = i * 1.9f + 0.4f, rr = r * (0.3f + 0.12f * i);
                    float ang = ((a - b) % MathUtils.PI2 + MathUtils.PI2) % MathUtils.PI2;
                    d.circle(cx + MathUtils.cos(b) * rr, cy + MathUtils.sin(b) * rr, r * 0.07f, RED, Math.max(0.15f, 1 - ang / 2.5f));
                }
                break;
            }
            case "commander": {
                d.circle(cx, cy, r * 0.92f, VIO_D, 1);
                for (int k = 0; k < 3; k++) {
                    float a0 = t * (0.7f + k * 0.4f) * (k % 2 == 0 ? 1 : -1);
                    float rr = r * (0.88f - k * 0.13f);
                    for (int i = 0; i < 3; i++) d.arc(cx, cy, rr, a0 + i * MathUtils.PI2 / 3, a0 + i * MathUtils.PI2 / 3 + 1.4f, r * 0.05f, VIO, 0.9f - k * 0.2f);
                }
                d.star(cx, cy, r * 0.4f, tmp.set(VIO).lerp(WHITE, 0.4f + 0.3f * p), 1);
                break;
            }
            case "season": {
                float[] h = ngon(cx, cy, r * 0.92f, 6, MathUtils.PI / 6);
                d.poly(h, DARK, 1);
                // переливающаяся рамка «полярное сияние»
                for (int i = 0; i < 6; i++) {
                    float k = 0.5f + 0.5f * MathUtils.sin(t * 1.4f + i * 1.05f);
                    tmp.set(0.1f, 1f, 0.55f, 1).lerp(0.6f, 0.3f, 1f, 1, k);
                    d.line(h[i * 2], h[i * 2 + 1], h[(i + 1) % 6 * 2], h[(i + 1) % 6 * 2 + 1], r * 0.1f, tmp, 1);
                }
                tmp.set(0.1f, 1f, 0.55f, 1).lerp(0.6f, 0.3f, 1f, 1, p);
                d.text("30", cx, cy + r * 0.22f, r * 0.62f, tmp, Draw.BOLD, 0);
                shine(d, cx, cy, r * 0.85f, t, 3f);
                break;
            }
            default: { // recruit
                float[] h = ngon(cx, cy, r * 0.92f, 6, MathUtils.PI / 6);
                d.poly(h, OLIVE_D, 1); d.polyline(h, true, r * 0.07f, OLIVE, 1);
                chevrons(d, cx, cy + r * 0.05f, r, 1, STEEL);
                shine(d, cx, cy, r * 0.85f, t, 4.5f);
            }
        }
    }

    private Emblem() { }
}
