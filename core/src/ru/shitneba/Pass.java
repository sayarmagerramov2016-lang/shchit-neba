package ru.shitneba;

import java.util.Calendar;

/**
 * Боевой пропуск без доната (3.9.0): 30 уровней, опыт за бой, награды забираются вручную.
 * Сбрасывается в начале каждого месяца. Сохранение: блок bp (month, xp, got).
 */
public final class Pass {
    public static final int LEVELS = 30, XP_LV = 100;

    /** Опыт пропуска за события боя. */
    public static final int XP_KILL = 1, XP_HEAVY = 3, XP_BOSS = 25, XP_WAVE = 10, XP_MISSION = 40, XP_DAILY = 150, XP_QUEST = 30, XP_STRIKE_ZONE = 30;

    public static final class Reward {
        public final String kind, id, name; public final int n;
        Reward(String kind, String id, String name, int n) { this.kind = kind; this.id = id; this.name = name; this.n = n; }
    }

    /** Награда уровня 1..30. */
    public static Reward reward(int lv) {
        switch (lv) {
            case 5: case 15: case 25: return new Reward("case", "army", "Армейский кейс", 0);
            case 10: return new Reward("tracer", "neon", "Трассеры «Неон»", 0);
            case 12: return new Reward("skin", "guard", "Окраска «Гвардия»", 0);
            case 20: return new Reward("case", "elite", "Элитный кейс", 0);
            case 30: return new Reward("skin", "aurora", "Окраска «Полярное сияние»", 1000);
            default: return new Reward("coins", "", "", 80 + lv * 10);
        }
    }

    static String monthKey() {
        Calendar c = Calendar.getInstance();
        return String.format("%04d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1);
    }

    /** Новый месяц — новый пропуск. */
    public static void check() {
        String m = monthKey();
        if (!m.equals(G.SV.bpMonth)) { G.SV.bpMonth = m; G.SV.bpXp = 0; G.SV.bpGot.clear(); }
    }

    public static int level() { check(); return Math.min(LEVELS, G.SV.bpXp / XP_LV); }

    /** Доля до следующего уровня. */
    public static float levelK() { return level() >= LEVELS ? 1 : (G.SV.bpXp % XP_LV) / (float) XP_LV; }

    /** Добавить опыт; возвращает новый уровень, если он вырос, иначе 0. */
    public static int add(int xp) {
        if (xp <= 0) return 0;
        int l0 = level();
        G.SV.bpXp = Math.min(LEVELS * XP_LV, G.SV.bpXp + xp);
        int l1 = level();
        return l1 > l0 ? l1 : 0;
    }

    public static boolean got(int lv) { check(); return G.SV.bpGot.contains(lv); }

    public static boolean canClaim(int lv) { return lv <= level() && !got(lv); }

    /** Сколько наград ждут — для значка в меню. */
    public static int ready() { int n = 0; for (int i = 1; i <= level(); i++) if (!got(i)) n++; return n; }

    /** Дней до сброса. */
    public static int daysLeft() {
        Calendar c = Calendar.getInstance();
        return c.getActualMaximum(Calendar.DAY_OF_MONTH) - c.get(Calendar.DAY_OF_MONTH) + 1;
    }

    /**
     * Забрать награду. Кейс не открывается здесь — его откроет окно операторов (возвращается Ops.Case).
     * Уже имеющиеся окраска/трассеры заменяются монетами.
     */
    public static Ops.Case claim(int lv) {
        if (!canClaim(lv)) return null;
        G.SV.bpGot.add(lv);
        Reward r = reward(lv);
        Ops.Case open = null;
        switch (r.kind) {
            case "case":
                for (Ops.Case c : Ops.CASES) if (c.id.equals(r.id)) open = c;
                break;
            case "skin": case "tracer":
                if (G.SV.skOwn.contains(r.id)) G.SV.c += 800; else G.SV.skOwn.add(r.id);
                G.SV.c += r.n;
                break;
            default: G.SV.c += r.n;
        }
        G.save();
        return open;
    }

    private Pass() { }
}
