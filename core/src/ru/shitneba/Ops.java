package ru.shitneba;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Операторы — аниме-девчонки рядом с комплексом (перенесено из 2D-версии «Щит неба» 3.4.0).
 * Покупаются только за игровые монеты: в магазине или из кейсов. Одновременно выбрана одна.
 * Картинки — assets/ops (рисует store/tools/make_ops.py).
 */
public final class Ops {
    public static final String[] RAR_N = {"Обычная", "Редкая", "Эпическая", "Легендарная", "Мифическая"};
    public static final String[] RAR_C = {"b9c3cc", "4fa3ff", "b07cff", "ffb21a", "ff4a6a"};
    public static final int[] DUPE = {40, 100, 250, 600, 1500};
    /** Пороги опыта дружбы: уровень 1 — с нуля, 10 — с 3700. */
    public static final int[] LV_X = {0, 40, 120, 260, 480, 800, 1250, 1850, 2650, 3700};
    public static final int PITY = 40;

    public static final class Op {
        public final String id, name, abil, abDesc, desc;
        public final int rar, price; public final float cd; public final boolean ny;
        public final float pitch;
        Op(String id, String name, int rar, int price, String abil, float cd, String abDesc, String desc, boolean ny, float pitch) {
            this.id = id; this.name = name; this.rar = rar; this.price = price; this.abil = abil; this.cd = cd;
            this.abDesc = abDesc; this.desc = desc; this.ny = ny; this.pitch = pitch;
        }
    }

    public static final Op[] ALL = {
            new Op("liza", "Лиза", 0, 400, "Ремкомплект", 40, "Сразу чинит 20% брони", "Механик отряда. Гаечный ключ всегда при ней.", false, 1.0f),
            new Op("katya", "Катя", 0, 400, "Горячий чай", 30, "Пушка остывает полностью, +2 ракеты", "Термос с чаем решает любые проблемы.", false, 1.12f),
            new Op("sonya", "Соня", 0, 400, "Маскировка", 35, "Дымовая завеса на 5 с", "Тихая разведчица. Любит туман.", false, 0.9f),
            new Op("anya", "Аня", 1, 1000, "Радар", 30, "8 с: невидимые дроны видны, урон +30%", "Слышит дрон раньше, чем он взлетит.", false, 1.2f),
            new Op("masha", "Маша", 1, 1000, "Кошачья удача", 40, "8 с: монеты x2", "Приносит удачу. Мяу.", false, 1.3f),
            new Op("rin", "Рин", 2, 2500, "Перегрузка", 35, "6 с: скорострельность x1.6, пушка не греется", "Неоновый хакер. Разгоняет всё, что стреляет.", false, 1.25f),
            new Op("yuki", "Юки", 2, 2500, "Заморозка", 35, "Все дроны замедлены на 4 с", "Холодная, как зимнее небо.", false, 1.15f),
            new Op("elka", "Ёлка", 2, 3000, "Гирлянда", 30, "Молния цепью бьёт 6 ближайших целей", "Новогодняя. Светится, искрит, радуется.", true, 1.35f),
            new Op("akira", "Акира", 3, 6000, "Рывок самурая", 35, "5 с: урон x2, скорострельность x1.3", "Путь меча — путь ПВО.", false, 0.95f),
            new Op("sneg", "Снегурочка", 3, 7000, "Метель", 40, "Заморозка на 4 с и урон всем дронам на экране", "Новогодняя. Метель по её слову.", true, 1.1f),
            new Op("valk", "Валькирия", 4, 15000, "Ангел-хранитель", 45, "+25% брони и 3 с неуязвимости. Раз за бой сама спасает от смерти", "Мифическая. Небо под её крылом.", false, 1.05f),
    };

    public static final class Case {
        public final String id, name, col; public final int price; public final float[] odds; public final boolean ny;
        Case(String id, String name, String col, int price, float[] odds, boolean ny) { this.id = id; this.name = name; this.col = col; this.price = price; this.odds = odds; this.ny = ny; }
    }

    static final float[] ARMY = {55, 28, 12, 4.5f, 0.5f};
    public static final Case[] CASES = {
            new Case("free", "Подарок", "3ddc97", 0, ARMY, false),
            new Case("army", "Армейский", "8a9a5a", 300, ARMY, false),
            new Case("elite", "Элитный", "b07cff", 900, new float[]{0, 70, 22, 7, 1}, false),
            new Case("ny", "Новогодний", "4fa3ff", 700, new float[]{34, 40, 20, 6, 0}, true),
    };

    static final Random R = new Random();

    public static Op get(String id) {
        if (id == null) return null;
        for (Op o : ALL) if (o.id.equals(id)) return o;
        return null;
    }

    public static Op sel() { return get(G.SV.opSel); }

    public static boolean owned(String id) { return G.SV.opOwn.contains(id); }

    public static int xp(String id) { Integer v = G.SV.opXp.get(id); return v == null ? 0 : v; }

    public static int level(String id) {
        int x = xp(id), l = 1;
        for (int i = 0; i < LV_X.length; i++) if (x >= LV_X[i]) l = i + 1;
        return l;
    }

    /** Доля прогресса до следующего уровня. */
    public static float lvK(String id) {
        int l = level(id);
        if (l >= LV_X.length) return 1;
        return (xp(id) - LV_X[l - 1]) / (float) (LV_X[l] - LV_X[l - 1]);
    }

    public static float coinBonus(String id) { return (level(id) - 1) * 0.03f; }

    public static float cdOf(Op o) { return o.cd * (1 - (level(o.id) - 1) * 0.03f); }

    public static boolean alt(String id) { return G.SV.opAlt.contains(id) && level(id) >= 5; }

    /** Опыт дружбы выбранной девчонке. Возвращает новый уровень, если он вырос, иначе 0. */
    public static int addXp(int n) {
        Op o = sel();
        if (o == null || n <= 0) return 0;
        int l0 = level(o.id);
        G.SV.opXp.put(o.id, xp(o.id) + n);
        int l1 = level(o.id);
        return l1 > l0 ? l1 : 0;
    }

    public static boolean freeReady() { return !G.today().equals(G.SV.opFree); }

    /** Пул девчонок редкости r для кейса. */
    static List<Op> pool(Case c, int r) {
        List<Op> out = new ArrayList<>();
        for (Op o : ALL) {
            if (o.rar != r) continue;
            if (c.ny) {
                if (r == 3 && !o.id.equals("sneg")) continue;
                if (r == 4) continue;
            } else if (o.ny) continue;
            out.add(o);
        }
        return out;
    }

    public static int rollRarity(Case c) {
        float s = 0;
        for (float v : c.odds) s += v;
        float x = R.nextFloat() * s;
        for (int i = 0; i < c.odds.length; i++) { x -= c.odds[i]; if (x < 0 && c.odds[i] > 0) return i; }
        return 0;
    }

    public static final class Result { public Op op; public boolean isNew, pity; public int dupe; }

    /** Открыть кейс: списать монеты, выбрать выигрыш, сразу сохранить (до прокрутки). null — не хватает монет. */
    public static Result open(Case c) { return open(c, false); }

    /** gift = кейс-подарок (награда боевого пропуска): без списания монет и без суточного лимита. */
    public static Result open(Case c, boolean gift) {
        if (!gift && c.id.equals("free")) {
            if (!freeReady()) return null;
            G.SV.opFree = G.today();
        } else if (!gift) {
            if (G.SV.c < c.price) return null;
            G.SV.c -= c.price;
        }
        int r = rollRarity(c);
        Result res = new Result();
        G.SV.opPity++;
        if (r < 3 && G.SV.opPity >= PITY) { r = 3; res.pity = true; }
        if (r >= 3) G.SV.opPity = 0;
        List<Op> p = pool(c, r);
        while (p.isEmpty() && r > 0) p = pool(c, --r);
        Op o = p.get(R.nextInt(p.size()));
        res.op = o;
        G.SV.opCases++;
        if (owned(o.id)) {
            res.dupe = DUPE[o.rar];
            G.SV.c += res.dupe;
        } else {
            res.isNew = true;
            G.SV.opOwn.add(o.id);
        }
        G.save();
        return res;
    }

    /** Лента рулетки: 44 портрета, выигрыш на WIN, сразу за ним часто стоит легендарка или мифик. */
    public static final int STRIP = 44, WIN = 38;

    public static Op[] strip(Case c, Op win) {
        Op[] s = new Op[STRIP];
        for (int i = 0; i < STRIP; i++) {
            List<Op> p = pool(c, rollRarity(c));
            if (p.isEmpty()) p = pool(c, 0);
            if (p.isEmpty()) p = pool(c, 1);
            s[i] = p.get(R.nextInt(p.size()));
        }
        s[WIN] = win;
        if (win.rar < 3 && R.nextFloat() < 0.7f) {
            List<Op> hi = pool(c, R.nextFloat() < 0.35f && !c.ny ? 4 : 3);
            if (hi.isEmpty()) hi = pool(c, 3);
            if (!hi.isEmpty()) s[WIN + 1] = hi.get(R.nextInt(hi.size()));
        }
        return s;
    }

    public static int collected() {
        int n = 0;
        for (Op o : ALL) if (owned(o.id)) n++;
        return n;
    }

    // ---------- реплики ----------
    static final String[][] SAY = {
            {"hit", "Нас подбили!", "Ай! Держимся!", "Попали по нам!", "Броня гудит!"},
            {"kill", "Сбит!", "Есть!", "Минус один!", "Готов!"},
            {"boss", "Босс готов!", "Мы его сделали!", "Ура-а-а!"},
            {"wave", "Новая волна!", "Летят!", "Внимание, цели!"},
            {"low", "Броня на исходе!", "Осторожно, командир!", "Нам плохо…"},
            {"lose", "Нет…", "Мы не смогли…"},
            {"warm", "Командир, ты лучший!", "С тобой не страшно", "Отличный выстрел, командир!", "Горжусь тобой!"},
            {"save", "Я тебя не отдам!", "Вставай, командир!"},
            {"clear", "Небо чистое!", "Отбились!", "Так держать!", "Ещё волну? Давай!"},
            {"idle", "Смотрю в оба!", "Тихо… слишком тихо", "Командир, не спим!", "Радар чистый пока", "Где же они?", "Я готова!"},
    };

    public static String say(String ev) {
        for (String[] s : SAY) if (s[0].equals(ev)) return s[1 + R.nextInt(s.length - 1)];
        return null;
    }

    private Ops() { }
}
