package ru.shitneba;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

/**
 * Вызов дня (3.9.0): одна миссия на сутки, одинаковая у всех игроков (зерно — дата).
 * Машина, карта, погода, ограничения и волны выбираются случайно; награда — раз в день, за серию дней — больше.
 * Строится как обычная миссия кампании (special = "daily"), поэтому идёт тем же движком.
 */
public final class Daily {
    static JsonValue cache; static String cacheDay = "";

    static final String[] MOD_POOL = {"fast", "glass", "noUpg", "gunOnly", "missilesOnly"};
    static final String[] BOSSES = {"gunship", "walker", "carrier", "ghost", "hydra", "storm", "manta", "boss"};

    public static String id() { return "daily-" + G.today(); }

    /** Пройден ли вызов сегодня (награда уже взята). */
    public static boolean doneToday() { return G.today().equals(G.SV.dcDay); }

    /** Серия дней подряд с учётом сегодняшнего. */
    public static int streak() {
        if (doneToday() || G.today(-1).equals(G.SV.dcDay)) return G.SV.dcStreak;
        return 0;
    }

    public static int reward() { return 300 + Math.min(6, streak()) * 50; }

    public static JsonValue today() {
        String day = G.today();
        if (cache != null && day.equals(cacheDay)) return cache;
        Random r = new Random(("sky-daily-" + day).hashCode());
        String veh = G.VEHS[r.nextInt(G.VEHS.length)];
        List<String> mods = new ArrayList<>();
        int nm = r.nextInt(3); // 0–2 ограничения
        while (mods.size() < nm) {
            String m = MOD_POOL[r.nextInt(MOD_POOL.length)];
            if (mods.contains(m)) continue;
            if (m.equals("missilesOnly") && !(veh.equals("pantsir") || veh.equals("tor"))) continue;
            if (m.equals("gunOnly") && (veh.equals("tor") || mods.contains("missilesOnly"))) continue;
            if (m.equals("missilesOnly") && mods.contains("gunOnly")) continue;
            mods.add(m);
        }
        int theme = r.nextInt(9);
        boolean dark = theme == 1 || r.nextFloat() < 0.2f;
        boolean rain = r.nextFloat() < 0.25f;
        String map = Maps.BY_CH[r.nextInt(Maps.BY_CH.length)];
        String boss = BOSSES[r.nextInt(BOSSES.length)];
        int lvl = 10 + r.nextInt(8);
        StringBuilder w = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            int n = 9 + i * 3;
            StringBuilder mix = new StringBuilder("\"quad\":3,\"wing\":2");
            if (i >= 1) mix.append(",\"zig\":2,\"heavy\":1");
            if (i >= 2) mix.append(",\"phantom\":1,\"kami\":1");
            if (i >= 3) mix.append(",\"matka\":0.35,\"gnats\":0.5");
            if (i >= 4) mix.append(",\"split\":1,\"stealth\":1");
            if (w.length() > 0) w.append(',');
            w.append("{\"n\":").append(n).append(",\"int\":").append(Math.max(0.8f, 1.35f - i * 0.1f)).append(",\"mix\":{").append(mix).append('}');
            if (i == 5) w.append(",\"boss\":\"").append(boss).append('"');
            w.append('}');
        }
        StringBuilder ms = new StringBuilder();
        for (String m : mods) { if (ms.length() > 0) ms.append(','); ms.append('"').append(m).append('"'); }
        Calendar c = Calendar.getInstance();
        String title = String.format("Вызов дня · %02d.%02d", c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1);
        String json = "{\"id\":\"" + id() + "\",\"title\":\"" + title + "\",\"goal\":\"Отбей 6 волн и босса на " + G.vehName(veh) + "\","
                + "\"type\":\"waves\",\"special\":\"daily\",\"veh\":\"" + veh + "\",\"theme\":" + theme + ",\"dark\":" + dark + ",\"map\":\"" + map + "\","
                + (rain ? "\"weather\":{\"rain\":1,\"wind\":" + (r.nextInt(80) - 40) + "}," : "")
                + "\"lvl\":" + lvl + ",\"reward\":300,\"mods\":[" + ms + "],\"waves\":[" + w + "],"
                + "\"stars\":[{\"t\":\"Выполнить задание\"},{\"t\":\"Комплекс цел на 50%+\",\"k\":\"hp\",\"op\":\">=\",\"v\":0.5},{\"t\":\"Комплекс цел на 80%+\",\"k\":\"hp\",\"op\":\">=\",\"v\":0.8}],"
                + "\"brief\":[[\"gromov\",\"Сегодняшняя задача одна на всех расчётов. Посмотрим, кто справится.\"]],\"debrief\":[]}";
        cache = new JsonReader().parse(json);
        cacheDay = day;
        return cache;
    }

    /** Отметить прохождение; возвращает награду в монетах (0 — сегодня уже получена). */
    static int complete() {
        if (doneToday()) return 0;
        G.SV.dcStreak = G.today(-1).equals(G.SV.dcDay) ? G.SV.dcStreak + 1 : 1;
        G.SV.dcDay = G.today();
        return reward();
    }

    private Daily() { }
}
