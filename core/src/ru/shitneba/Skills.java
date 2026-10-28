package ru.shitneba;

/**
 * Дерево навыков командира (3.10.0). Три ветки по 5 узлов: четыре обычных (до 3 уровней) и «ульта» (1 уровень, 3 очка).
 * Очки — за опыт званий: одно за каждые {@link #XP_PER_POINT} опыта, максимум столько, чтобы взять всё дерево.
 * В дуэли навыки не действуют (честный бой). Сохранение: блок cmd.sk.
 */
public final class Skills {
    public static final int XP_PER_POINT = 500;

    public static final class Node {
        public final String id, branch, name, fmt; public final int max, cost; public final float per;
        Node(String id, String branch, String name, String fmt, float per, int max, int cost) {
            this.id = id; this.branch = branch; this.name = name; this.fmt = fmt; this.per = per; this.max = max; this.cost = cost;
        }
        public boolean ult() { return max == 1 && cost > 1; }
        /** Описание для уровня lv (1..max); %d — суммарный процент/число. */
        public String desc(int lv) { return fmt.contains("%d") ? String.format(fmt, Math.round(per * Math.max(1, lv))) : fmt; }
    }

    public static final String[] BRANCHES = {"fire", "armor", "tact"};
    public static final String[] BRANCH_N = {"Огонь", "Броня", "Тактика"};

    public static final Node[] NODES = {
            new Node("f_dmg", "fire", "Калибр", "+%d%% урона пушки и луча", 5, 3, 1),
            new Node("f_rate", "fire", "Темп", "+%d%% скорострельности", 4, 3, 1),
            new Node("f_mis", "fire", "Боевая часть", "+%d%% урона ракет и разрывов, перезарядка быстрее", 6, 3, 1),
            new Node("f_crit", "fire", "Меткость", "%d%% шанс двойного урона", 5, 3, 1),
            new Node("f_ult", "fire", "Шквал", "Каждые 25 сбитых — 6 с огня без перегрева, темп ×1.5", 0, 1, 3),
            new Node("a_hp", "armor", "Бронелисты", "+%d%% прочности комплекса", 6, 3, 1),
            new Node("a_reg", "armor", "Ремрота", "+%d прочности после каждой волны", 5, 3, 1),
            new Node("a_cool", "armor", "Охлаждение", "+%d%% к остыванию стволов", 8, 3, 1),
            new Node("a_def", "armor", "Рикошет", "%d%% шанс не получить урон", 5, 3, 1),
            new Node("a_ult", "armor", "Второе дыхание", "Раз за бой при гибели комплекс встаёт с 30% прочности", 0, 1, 3),
            new Node("t_coin", "tact", "Снабжение", "+%d%% монет за сбитых и волны", 6, 3, 1),
            new Node("t_combo", "tact", "Кураж", "Серия держится дольше на %d с", 1, 3, 1),
            new Node("t_xp", "tact", "Штаб", "+%d%% опыта званий и пропуска", 8, 3, 1),
            new Node("t_luck", "tact", "Трофеи", "%d%% шанс получить ×5 монет за сбитого", 4, 3, 1),
            new Node("t_ult", "tact", "Наводчик", "Фантомы и невидимки видны на экране, прицел их подсвечивает", 0, 1, 3),
    };

    public static Node node(String id) { for (Node n : NODES) if (n.id.equals(id)) return n; return null; }

    /** Уровень узла (без учёта режима). */
    public static int lv(String id) { Integer v = G.SV.skl.get(id); return v == null ? 0 : v; }

    /** Уровень узла с учётом того, что в дуэли навыки выключены. */
    public static boolean off;
    public static int on(String id) { return off ? 0 : lv(id); }

    public static float k(String id) { Node n = node(id); return n == null ? 0 : on(id) * n.per / 100f; }

    public static int earned() {
        int all = 0; for (Node n : NODES) all += n.max * n.cost;
        return Math.min(all, G.SV.xp / XP_PER_POINT);
    }

    public static int spent() { int s = 0; for (Node n : NODES) s += lv(n.id) * n.cost; return s; }

    public static int free() { return Math.max(0, earned() - spent()); }

    /** Сколько опыта до следующего очка (0 — дерево уже можно взять целиком). */
    public static int xpToNext() {
        int all = 0; for (Node n : NODES) all += n.max * n.cost;
        if (earned() >= all) return 0;
        return XP_PER_POINT - G.SV.xp % XP_PER_POINT;
    }

    /** Предыдущий узел ветки (его нужно взять хотя бы на 1 уровень). */
    public static Node prev(Node n) {
        Node p = null;
        for (Node o : NODES) { if (o == n) return p; if (o.branch.equals(n.branch)) p = o; }
        return null;
    }

    public static boolean open(Node n) { Node p = prev(n); return p == null || lv(p.id) > 0; }

    public static boolean canBuy(Node n) { return open(n) && lv(n.id) < n.max && free() >= n.cost; }

    public static boolean buy(Node n) {
        if (!canBuy(n)) return false;
        G.SV.skl.put(n.id, lv(n.id) + 1);
        G.save();
        return true;
    }

    public static final int RESET_COST = 300;

    public static boolean reset() {
        if (spent() == 0 || G.SV.c < RESET_COST) return false;
        G.SV.c -= RESET_COST; G.SV.skl.clear(); G.save();
        return true;
    }

    public static int ults() { int n = 0; for (Node o : NODES) if (o.ult() && lv(o.id) > 0) n++; return n; }

    private Skills() { }
}
