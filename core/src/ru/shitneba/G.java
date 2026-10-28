package ru.shitneba;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Глобальные данные: data.json (D), сохранение (SV), звания, утилиты. Порт g.gd. */
public final class G {
    public static final String VER = "3.8.1";
    public static JsonValue D;
    public static Save SV;
    public static Sfx sfx;
    public static final String[] VEHS = {"pantsir", "shilka", "tor", "luch", "grom"};

    public static final class Quest {
        public String id; public int lv; public float n, r, p; public boolean done, cl;
    }

    public static final class Top {
        public int s, w; public String d = "normal", t = "", v = "pantsir";
    }

    public static final class Save {
        public int c, best, xp;
        public final Map<String, Map<String, Integer>> upv = new LinkedHashMap<>();
        public final List<String> vown = new ArrayList<>();
        public String veh = "pantsir";
        public final Map<String, Boolean> ab = new LinkedHashMap<>();
        public final List<String> skOwn = new ArrayList<>();
        public String skVeh = "olive", skTr = "gold";
        public final Map<String, Integer> camp = new LinkedHashMap<>();
        public final List<String> cuts = new ArrayList<>();
        public final Map<String, Float> stats = new LinkedHashMap<>();
        public final List<String> ach = new ArrayList<>();
        public final Map<String, Integer> chal = new LinkedHashMap<>();
        public String dailyLast = ""; public int dailyStreak;
        public String dmis = "";
        public String qDay = ""; public final List<Quest> qList = new ArrayList<>();
        public String diff = "normal";
        public int gfx = 2;
        public float sens = 1f, vol = 0.8f;
        public boolean vib = true, night = false, tut = false;
        public final List<Top> top = new ArrayList<>();
        // режим «Рой»
        public final List<String> swOwn = new ArrayList<>();
        public String swPick = "osa";
        public final Map<String, Integer> swUp = new LinkedHashMap<>();
        public int swBest, swBestz, swCam;
        public boolean swInv;
        // дуэль
        public String duelName = "";
        // операторы
        public final List<String> opOwn = new ArrayList<>();
        public String opSel = "", opFree = "";
        public final Map<String, Integer> opXp = new LinkedHashMap<>();
        public final List<String> opAlt = new ArrayList<>();
        public int opPity, opCases;
    }

    public static void init() {
        D = new JsonReader().parse(Gdx.files.internal("data.json"));
        loadSave();
    }

    public static Map<String, Integer> zeroUp() {
        Map<String, Integer> o = new LinkedHashMap<>();
        for (JsonValue u = D.get("UPG").child; u != null; u = u.next) o.put(u.name, 0);
        return o;
    }

    public static Save defSave() {
        Save s = new Save();
        for (String v : VEHS) s.upv.put(v, zeroUp());
        s.vown.add("pantsir");
        for (String k : new String[]{"salvo", "emp", "smoke", "repair"}) s.ab.put(k, false);
        s.skOwn.add("olive"); s.skOwn.add("gold");
        s.swOwn.add("osa");
        for (String k : new String[]{"war", "bat", "hull", "ecm"}) s.swUp.put(k, 0);
        if (Gdx.app != null && Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Android) s.gfx = 1;
        return s;
    }

    static FileHandle saveFile() { return Gdx.files.local("save3d.json"); }

    public static void loadSave() {
        SV = defSave();
        try {
            FileHandle f = saveFile();
            if (!f.exists()) return;
            JsonValue d = new JsonReader().parse(f.readString("UTF-8"));
            Save s = SV;
            s.c = d.getInt("c", 0); s.best = d.getInt("best", 0); s.xp = d.getInt("xp", 0);
            JsonValue upv = d.get("upv");
            if (upv != null) for (JsonValue v = upv.child; v != null; v = v.next) {
                Map<String, Integer> m = s.upv.containsKey(v.name) ? s.upv.get(v.name) : zeroUp();
                for (JsonValue u = v.child; u != null; u = u.next) m.put(u.name, u.asInt());
                s.upv.put(v.name, m);
            }
            readList(d.get("vown"), s.vown, true);
            s.veh = d.getString("veh", s.veh);
            JsonValue ab = d.get("ab");
            if (ab != null) for (JsonValue v = ab.child; v != null; v = v.next) s.ab.put(v.name, v.asBoolean());
            JsonValue sk = d.get("sk");
            if (sk != null) {
                readList(sk.get("own"), s.skOwn, true);
                s.skVeh = sk.getString("veh", s.skVeh); s.skTr = sk.getString("tr", s.skTr);
            }
            readIntMap(d.get("camp"), s.camp);
            readList(d.get("cuts"), s.cuts, true);
            JsonValue st = d.get("stats");
            if (st != null) for (JsonValue v = st.child; v != null; v = v.next) s.stats.put(v.name, v.asFloat());
            readList(d.get("ach"), s.ach, true);
            readIntMap(d.get("chal"), s.chal);
            JsonValue dl = d.get("daily");
            if (dl != null) { s.dailyLast = dl.getString("last", ""); s.dailyStreak = dl.getInt("streak", 0); }
            s.dmis = d.getString("dmis", "");
            JsonValue q = d.get("q");
            if (q != null) {
                s.qDay = q.getString("day", "");
                JsonValue l = q.get("list");
                if (l != null) for (JsonValue v = l.child; v != null; v = v.next) {
                    Quest qq = new Quest();
                    qq.id = v.getString("id"); qq.lv = v.getInt("lv"); qq.n = v.getFloat("n"); qq.r = v.getFloat("r");
                    qq.p = v.getFloat("p"); qq.done = v.getBoolean("done"); qq.cl = v.getBoolean("cl");
                    s.qList.add(qq);
                }
            }
            s.diff = d.getString("diff", s.diff); s.gfx = d.getInt("gfx", s.gfx);
            s.sens = d.getFloat("sens", s.sens); s.vol = d.getFloat("vol", s.vol);
            s.vib = d.getBoolean("vib", s.vib); s.night = d.getBoolean("night", s.night); s.tut = d.getBoolean("tut", s.tut);
            JsonValue top = d.get("top");
            if (top != null) for (JsonValue v = top.child; v != null; v = v.next) {
                Top t = new Top(); t.s = v.getInt("s"); t.w = v.getInt("w"); t.d = v.getString("d", "normal");
                t.t = v.getString("t", ""); t.v = v.getString("v", "pantsir"); s.top.add(t);
            }
            for (String v : VEHS) {
                if (!s.upv.containsKey(v)) s.upv.put(v, zeroUp());
                for (JsonValue u = D.get("UPG").child; u != null; u = u.next)
                    if (!s.upv.get(v).containsKey(u.name)) s.upv.get(v).put(u.name, 0);
            }
            if (!s.vown.contains(s.veh)) s.veh = "pantsir";
            JsonValue sw = d.get("sw");
            if (sw != null) {
                readList(sw.get("own"), s.swOwn, true);
                if (!s.swOwn.contains("osa")) s.swOwn.add(0, "osa");
                s.swPick = sw.getString("pick", "osa");
                JsonValue u = sw.get("up");
                if (u != null) for (JsonValue v = u.child; v != null; v = v.next) s.swUp.put(v.name, v.asInt());
                s.swBest = sw.getInt("best", 0); s.swBestz = sw.getInt("bestz", 0); s.swCam = sw.getInt("cam", 0); s.swInv = sw.getBoolean("inv", false);
            }
            s.duelName = d.getString("duelName", "");
            JsonValue op = d.get("op");
            if (op != null) {
                readList(op.get("own"), s.opOwn, true);
                readList(op.get("alt"), s.opAlt, true);
                readIntMap(op.get("xp"), s.opXp);
                s.opSel = op.getString("sel", ""); s.opFree = op.getString("free", "");
                s.opPity = op.getInt("pity", 0); s.opCases = op.getInt("cases", 0);
                if (!s.opOwn.contains(s.opSel)) s.opSel = "";
            }
        } catch (Throwable t) {
            Gdx.app.error("save", "bad save", t);
            SV = defSave();
        }
    }

    static void readList(JsonValue v, List<String> out, boolean clear) {
        if (v == null) return;
        if (clear) out.clear();
        for (JsonValue c = v.child; c != null; c = c.next) if (!out.contains(c.asString())) out.add(c.asString());
    }

    static void readIntMap(JsonValue v, Map<String, Integer> out) {
        if (v == null) return;
        for (JsonValue c = v.child; c != null; c = c.next) out.put(c.name, c.asInt());
    }

    public static void save() {
        try {
            StringWriter sw = new StringWriter();
            JsonWriter w = new JsonWriter(sw);
            w.setOutputType(JsonWriter.OutputType.json);
            Save s = SV;
            w.object();
            w.set("c", s.c).set("best", s.best).set("xp", s.xp);
            w.object("upv");
            for (Map.Entry<String, Map<String, Integer>> e : s.upv.entrySet()) {
                w.object(e.getKey());
                for (Map.Entry<String, Integer> u : e.getValue().entrySet()) w.set(u.getKey(), u.getValue());
                w.pop();
            }
            w.pop();
            w.array("vown"); for (String v : s.vown) w.value(v); w.pop();
            w.set("veh", s.veh);
            w.object("ab"); for (Map.Entry<String, Boolean> e : s.ab.entrySet()) w.set(e.getKey(), e.getValue()); w.pop();
            w.object("sk");
            w.array("own"); for (String v : s.skOwn) w.value(v); w.pop();
            w.set("veh", s.skVeh).set("tr", s.skTr);
            w.pop();
            w.object("camp"); for (Map.Entry<String, Integer> e : s.camp.entrySet()) w.set(e.getKey(), e.getValue()); w.pop();
            w.array("cuts"); for (String v : s.cuts) w.value(v); w.pop();
            w.object("stats"); for (Map.Entry<String, Float> e : s.stats.entrySet()) w.set(e.getKey(), e.getValue()); w.pop();
            w.array("ach"); for (String v : s.ach) w.value(v); w.pop();
            w.object("chal"); for (Map.Entry<String, Integer> e : s.chal.entrySet()) w.set(e.getKey(), e.getValue()); w.pop();
            w.object("daily").set("last", s.dailyLast).set("streak", s.dailyStreak).pop();
            w.set("dmis", s.dmis);
            w.object("q").set("day", s.qDay);
            w.array("list");
            for (Quest q : s.qList) {
                w.object().set("id", q.id).set("lv", q.lv).set("n", q.n).set("r", q.r).set("p", q.p).set("done", q.done).set("cl", q.cl).pop();
            }
            w.pop().pop();
            w.set("diff", s.diff).set("gfx", s.gfx).set("sens", s.sens).set("vol", s.vol).set("vib", s.vib).set("night", s.night).set("tut", s.tut);
            w.object("sw");
            w.array("own"); for (String v : s.swOwn) w.value(v); w.pop();
            w.set("pick", s.swPick);
            w.object("up"); for (Map.Entry<String, Integer> e : s.swUp.entrySet()) w.set(e.getKey(), e.getValue()); w.pop();
            w.set("best", s.swBest).set("bestz", s.swBestz).set("cam", s.swCam).set("inv", s.swInv);
            w.pop();
            w.set("duelName", s.duelName);
            w.object("op");
            w.array("own"); for (String v : s.opOwn) w.value(v); w.pop();
            w.array("alt"); for (String v : s.opAlt) w.value(v); w.pop();
            w.object("xp"); for (Map.Entry<String, Integer> e : s.opXp.entrySet()) w.set(e.getKey(), e.getValue()); w.pop();
            w.set("sel", s.opSel).set("free", s.opFree).set("pity", s.opPity).set("cases", s.opCases);
            w.pop();
            w.array("top");
            for (Top t : s.top) w.object().set("s", t.s).set("w", t.w).set("d", t.d).set("t", t.t).set("v", t.v).pop();
            w.pop();
            w.pop();
            w.close();
            saveFile().writeString(sw.toString(), false, "UTF-8");
        } catch (Throwable t) {
            Gdx.app.error("save", "save failed", t);
        }
    }

    // ---------- звания ----------
    public static final String[] RANK_N = {"Рядовой", "Ефрейтор", "Младший сержант", "Сержант", "Старший сержант", "Старшина",
            "Прапорщик", "Лейтенант", "Старший лейтенант", "Капитан", "Майор", "Подполковник", "Полковник", "Генерал"};
    public static final int[] RANK_X = {0, 40, 120, 250, 450, 700, 1050, 1500, 2100, 2900, 4000, 5500, 7500, 10000};

    public static int xp() { return SV.xp; }

    public static int rankIndex() { return rankIndex(xp()); }

    public static int rankIndex(int x) {
        int r = 0;
        for (int i = 0; i < RANK_X.length; i++) if (x >= RANK_X[i]) r = i;
        return r;
    }

    public static int addXp(int n) {
        if (n <= 0) return -1;
        int before = rankIndex();
        SV.xp = xp() + n;
        int after = rankIndex();
        if (after > before) {
            for (int i = before + 1; i <= after; i++) SV.c += 100 * i;
            return after;
        }
        return -1;
    }

    public static String today(int off) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, off);
        return String.format("%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    public static String today() { return today(0); }

    private static long hapT;

    public static void hap(int ms) {
        if (!SV.vib || Gdx.app.getType() != com.badlogic.gdx.Application.ApplicationType.Android) return;
        long now = System.currentTimeMillis();
        if (now - hapT < 150) return;
        hapT = now;
        try { Gdx.input.vibrate(ms); } catch (Throwable ignored) { }
    }

    public static void loop(String name, float vol, float pitch) { if (sfx != null) sfx.loop(name, vol, pitch); }

    public static void sfx(String name) { sfx(name, 1f, 30); }
    public static void sfx(String name, float vol) { sfx(name, vol, 30); }
    public static void sfx(String name, float vol, int gapMs) { if (sfx != null) sfx.play(name, vol, gapMs); }
    public static void sfxP(String name, float vol, float pitch, int gapMs) { if (sfx != null) sfx.playP(name, vol, pitch, gapMs); }

    // ---------- утилиты данных ----------
    public static JsonValue d(String... path) {
        JsonValue v = D;
        for (String p : path) { if (v == null) return null; v = v.get(p); }
        return v;
    }

    public static String str(JsonValue v, String k, String def) { return v == null ? def : v.getString(k, def); }

    public static Color hex(String s) {
        if (s.startsWith("#")) s = s.substring(1);
        if (s.length() == 6) s = s + "ff";
        return Color.valueOf(s);
    }

    public static Color rgbList(String csv) {
        String[] p = csv.split(",");
        return new Color(Float.parseFloat(p[0].trim()) / 255f, Float.parseFloat(p[1].trim()) / 255f, Float.parseFloat(p[2].trim()) / 255f, 1f);
    }

    public static boolean isMobile() {
        return Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Android || Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.iOS;
    }

    public static String vehName(String k) {
        JsonValue v = D.get("VEHICLES").get(k);
        return v == null ? k : v.getString("name");
    }

    private G() { }
}
