package ru.shitneba;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import ru.shitneba.gfx.Node3;

/** Дуэль по Wi‑Fi: ПВО против босса. Порт duel.gd на TCP + UDP-поиск. */
public class Duel {
    public static final int PORT = 47100, DISC_PORT = 47101;
    static final String MAGIC = "SKYDUEL1";
    public static final String[] VEHS = G.VEHS;

    /** Платформенные функции (на Android — блокировка multicast для приёма широковещательных пакетов). */
    public interface Platform { void multicast(boolean on); }

    public static Platform platform;

    static final byte M_LOBBY = 1, M_PICK = 2, M_START = 3, M_IN_BOSS = 4, M_IN_PVO = 5, M_SNAP = 6, M_END = 7;

    final Game game;
    public GameUI ui;
    public volatile boolean isHost, connected;
    public String myRole = "pvo", myPick = "", theirPick = "";
    public boolean theirReady, inBattle;
    public String hostName = "";
    public final Map<String, String[]> found = Collections.synchronizedMap(new LinkedHashMap<>()); // ip -> name, role, pick, time

    ServerSocket server;
    public boolean hosting() { return server != null; }
    Socket sock;
    DataOutputStream out;
    final LinkedBlockingQueue<byte[]> sendQ = new LinkedBlockingQueue<>();
    final ConcurrentLinkedQueue<byte[]> recvQ = new ConcurrentLinkedQueue<>();
    final ConcurrentLinkedQueue<Runnable> mainQ = new ConcurrentLinkedQueue<>();
    volatile boolean running;
    DatagramSocket discSend, discRecv;
    Thread discThread;
    float snapT, inT;
    final List<Object[]> events = new ArrayList<>();
    final Map<String, Integer> lastCnt = new HashMap<>();
    final int[] lastAb = new int[4];
    final Map<Integer, Game.E> vis = new HashMap<>();
    final Array<Node3> rkVis = new Array<>();
    float prevHp = -1;
    static String[] TYPES;

    public Duel(Game g) { this.game = g; }

    static String[] types() {
        if (TYPES == null) {
            List<String> l = new ArrayList<>();
            for (JsonValue v = G.D.get("DT").child; v != null; v = v.next) l.add(v.name);
            Collections.sort(l);
            TYPES = l.toArray(new String[0]);
        }
        return TYPES;
    }

    static int typeIdx(String t) { String[] ts = types(); for (int i = 0; i < ts.length; i++) if (ts[i].equals(t)) return i; return 0; }

    public static String localIp() {
        String best = "";
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface ni = en.nextElement();
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (Enumeration<InetAddress> ad = ni.getInetAddresses(); ad.hasMoreElements(); ) {
                    InetAddress a = ad.nextElement();
                    if (!(a instanceof Inet4Address)) continue;
                    String s = a.getHostAddress();
                    if (s.startsWith("127.") || s.startsWith("169.254.")) continue;
                    if (s.startsWith("192.168.") || s.startsWith("10.") || s.startsWith("172.")) return s;
                    if (best.isEmpty()) best = s;
                }
            }
        } catch (Throwable ignored) { }
        return best.isEmpty() ? "127.0.0.1" : best;
    }

    // ---------- соединение ----------
    public String host(String role, String pick) {
        stop();
        try {
            server = new ServerSocket();
            server.setReuseAddress(true);
            server.bind(new InetSocketAddress(PORT));
        } catch (IOException e) {
            server = null;
            return "Не удалось открыть порт " + PORT;
        }
        isHost = true; myRole = role; myPick = pick; theirPick = ""; theirReady = false; connected = false; running = true;
        String ip = localIp();
        hostName = "Игрок " + ip.substring(ip.lastIndexOf('.') + 1);
        final ServerSocket srv = server;
        Thread t = new Thread(() -> {
            try {
                Socket s = srv.accept();
                attach(s);
                mainQ.add(() -> {
                    connected = true;
                    sendLobby();
                    if (ui != null) ui.toast("Соперник подключился");
                });
            } catch (IOException ignored) { }
        }, "duel-accept");
        t.setDaemon(true); t.start();
        // широковещательные объявления
        try {
            discSend = new DatagramSocket();
            discSend.setBroadcast(true);
        } catch (IOException e) { discSend = null; }
        final DatagramSocket ds = discSend;
        Thread b = new Thread(() -> {
            while (running && isHost && ds != null) {
                if (!connected) {
                    try {
                        byte[] msg = (MAGIC + "|" + hostName + "|" + myRole + "|" + myPick).getBytes(StandardCharsets.UTF_8);
                        ds.send(new DatagramPacket(msg, msg.length, InetAddress.getByName("255.255.255.255"), DISC_PORT));
                    } catch (Throwable ignored) { }
                }
                try { Thread.sleep(1000); } catch (InterruptedException e) { break; }
            }
        }, "duel-bcast");
        b.setDaemon(true); b.start();
        return "";
    }

    public String join(final String ip0) {
        stopDiscovery();
        closeSocket();
        final String ip = ip0.trim();
        isHost = false; connected = false; myPick = ""; theirPick = ""; running = true;
        Thread t = new Thread(() -> {
            try {
                Socket s = new Socket();
                s.connect(new InetSocketAddress(ip, PORT), 5000);
                attach(s);
                mainQ.add(() -> { connected = true; });
            } catch (IOException e) {
                mainQ.add(() -> { connected = false; if (ui != null) ui.toast("Не удалось подключиться"); });
            }
        }, "duel-connect");
        t.setDaemon(true); t.start();
        return "";
    }

    void attach(Socket s) throws IOException {
        s.setTcpNoDelay(true);
        sock = s;
        out = new DataOutputStream(s.getOutputStream());
        final DataInputStream in = new DataInputStream(s.getInputStream());
        sendQ.clear();
        Thread r = new Thread(() -> {
            try {
                while (running) {
                    int len = in.readInt();
                    if (len < 0 || len > 4_000_000) break;
                    byte[] b = new byte[len];
                    in.readFully(b);
                    recvQ.add(b);
                }
            } catch (IOException ignored) { }
            mainQ.add(this::onDisconnected);
        }, "duel-read");
        r.setDaemon(true); r.start();
        final DataOutputStream o = out;
        Thread w = new Thread(() -> {
            try {
                while (running) {
                    byte[] b = sendQ.poll(500, TimeUnit.MILLISECONDS);
                    if (b == null) continue;
                    o.writeInt(b.length); o.write(b); o.flush();
                }
            } catch (Throwable ignored) { }
        }, "duel-write");
        w.setDaemon(true); w.start();
    }

    void onDisconnected() {
        if (!connected && !inBattle) return;
        connected = false; theirReady = false; theirPick = "";
        if (inBattle) finish("", isHost ? "Соперник отключился" : "Хост отключился");
        else if (ui != null) ui.toast("Соперник отключился");
    }

    void closeSocket() {
        try { if (sock != null) sock.close(); } catch (IOException ignored) { }
        sock = null; out = null;
    }

    public void stop() {
        running = false;
        stopDiscovery();
        if (discSend != null) { discSend.close(); discSend = null; }
        try { if (server != null) server.close(); } catch (IOException ignored) { }
        server = null;
        closeSocket();
        connected = false; inBattle = false; isHost = false;
        recvQ.clear(); sendQ.clear();
        clearVis();
    }

    public void startDiscovery() {
        stopDiscovery(); found.clear();
        if (platform != null) platform.multicast(true);
        try {
            discRecv = new DatagramSocket(null);
            discRecv.setReuseAddress(true);
            discRecv.setBroadcast(true);
            discRecv.bind(new InetSocketAddress(DISC_PORT));
            discRecv.setSoTimeout(500);
        } catch (IOException e) { discRecv = null; return; }
        final DatagramSocket ds = discRecv;
        discThread = new Thread(() -> {
            byte[] buf = new byte[512];
            while (!ds.isClosed()) {
                try {
                    DatagramPacket p = new DatagramPacket(buf, buf.length);
                    ds.receive(p);
                    String pk = new String(p.getData(), 0, p.getLength(), StandardCharsets.UTF_8);
                    String[] parts = pk.split("\\|");
                    if (parts.length >= 4 && parts[0].equals(MAGIC)) {
                        String ip = p.getAddress().getHostAddress();
                        found.put(ip, new String[]{parts[1], parts[2], parts[3], String.valueOf(System.currentTimeMillis())});
                    }
                } catch (IOException ignored) { }
                long now = System.currentTimeMillis();
                synchronized (found) {
                    for (Iterator<String[]> it = found.values().iterator(); it.hasNext(); ) if (now - Long.parseLong(it.next()[3]) > 4000) it.remove();
                }
            }
        }, "duel-disc");
        discThread.setDaemon(true); discThread.start();
    }

    public void stopDiscovery() {
        if (discRecv != null) { discRecv.close(); discRecv = null; }
        if (platform != null) platform.multicast(false);
    }

    // ---------- сообщения ----------
    interface W { void w(DataOutputStream o) throws IOException; }

    void send(byte type, W body) {
        if (out == null) return;
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream(256);
            DataOutputStream o = new DataOutputStream(bo);
            o.writeByte(type);
            if (body != null) body.w(o);
            o.flush();
            if (sendQ.size() < 200) sendQ.add(bo.toByteArray());
        } catch (IOException ignored) { }
    }

    void sendLobby() { send(M_LOBBY, o -> { o.writeUTF(myRole); o.writeUTF(myPick); }); }

    public void hostPickChanged() { if (isHost && connected) sendLobby(); }

    public void sendPick(String pick) {
        myPick = pick;
        if (!isHost && connected) send(M_PICK, o -> o.writeUTF(pick));
    }

    public boolean canStart() { return isHost && connected && theirReady && !theirPick.isEmpty(); }

    public void begin() {
        if (!canStart()) return;
        final String boss = myRole.equals("boss") ? myPick : theirPick;
        final String veh = myRole.equals("pvo") ? myPick : theirPick;
        final int theme = MathUtils.random(G.D.get("THEMES").size - 1);
        send(M_START, o -> { o.writeUTF(boss); o.writeUTF(veh); o.writeInt(theme); o.writeUTF(myRole); });
        startBattle(boss, veh, theme, myRole);
    }

    void startBattle(String boss, String veh, int theme, String hostRole) {
        inBattle = true; events.clear(); prevHp = -1;
        lastCnt.clear(); for (int i = 0; i < 4; i++) lastAb[i] = 0;
        clearVis();
        String role = isHost ? hostRole : (hostRole.equals("pvo") ? "boss" : "pvo");
        myRole = role;
        game.netView = !isHost;
        game.duelRole = role;
        game.startDuel(boss, veh, theme);
        if (ui != null) ui.startHud();
    }

    public void pushEvent(Object[] e) { if (isHost && inBattle && events.size() < 40) events.add(e); }

    public void finishHost(final String winner, final String reason) {
        if (!isHost || !inBattle) return;
        send(M_END, o -> { o.writeUTF(winner); o.writeUTF(reason); });
        finish(winner, reason);
    }

    void finish(String winner, String reason) {
        if (!inBattle) return;
        inBattle = false;
        game.firing = false; game.state = "result";
        if (ui != null) ui.duelResult(winner.isEmpty(), winner.equals(myRole), reason);
    }

    public void leave() {
        stop();
        game.netView = false; game.duelRole = "";
    }

    // ---------- цикл ----------
    public void update(float dt) {
        Runnable r;
        while ((r = mainQ.poll()) != null) r.run();
        byte[] m;
        while ((m = recvQ.poll()) != null) {
            try { handle(new DataInputStream(new ByteArrayInputStream(m))); }
            catch (Throwable t) { Gdx.app.error("duel", "bad msg", t); }
        }
        if (!inBattle || out == null) return;
        if (isHost) {
            snapT -= dt;
            if (snapT <= 0) { snapT = 0.05f; sendSnap(); }
        } else {
            inT -= dt;
            if (inT <= 0) { inT = 1f / 30f; sendInput(); }
            updateVis(dt);
        }
    }

    void handle(DataInputStream in) throws IOException {
        byte t = in.readByte();
        switch (t) {
            case M_LOBBY: {
                String hr = in.readUTF(), hp = in.readUTF();
                myRole = hr.equals("pvo") ? "boss" : "pvo";
                theirPick = hp;
                boolean bad = myPick.isEmpty() || (myRole.equals("boss") && !contains(Game.DUEL_BOSSES, myPick)) || (myRole.equals("pvo") && !contains(VEHS, myPick));
                if (bad) myPick = myRole.equals("boss") ? Game.DUEL_BOSSES[1] : VEHS[0];
                break;
            }
            case M_PICK: if (isHost) { theirPick = in.readUTF(); theirReady = true; } break;
            case M_START: { String boss = in.readUTF(), veh = in.readUTF(); int th = in.readInt(); String hr = in.readUTF(); startBattle(boss, veh, th, hr); break; }
            case M_IN_BOSS: {
                if (!isHost || !inBattle) return;
                Game.BossIn b = game.bossIn;
                b.mx = in.readFloat(); b.my = in.readFloat(); b.mz = in.readFloat(); b.cy = in.readFloat();
                int[] n = {in.readInt(), in.readInt(), in.readInt()};
                String[] ks = {"f1", "f2", "sp"};
                for (int i = 0; i < 3; i++) {
                    int last = lastCnt.containsKey(ks[i]) ? lastCnt.get(ks[i]) : 0;
                    if (n[i] > last) {
                        if (i == 0) b.f1 += n[i] - last; else if (i == 1) b.f2 += n[i] - last; else b.sp += n[i] - last;
                        lastCnt.put(ks[i], n[i]);
                    }
                }
                break;
            }
            case M_IN_PVO: {
                if (!isHost || !inBattle) return;
                game.aimYaw = in.readFloat(); game.aimPitch = in.readFloat(); game.firing = in.readBoolean();
                int n = in.readInt();
                int last = lastCnt.containsKey("mis") ? lastCnt.get("mis") : 0;
                if (n > last) { for (int i = 0; i < n - last; i++) game.fireMissile("lock", false); lastCnt.put("mis", n); }
                String[] ks = {"salvo", "emp", "smoke", "repair"};
                for (int i = 0; i < 4; i++) { int a = in.readInt(); if (a > lastAb[i]) { lastAb[i] = a; game.useAbility(ks[i]); } }
                break;
            }
            case M_SNAP: if (!isHost && inBattle) readSnap(in); break;
            case M_END: { String w = in.readUTF(), rs = in.readUTF(); finish(w, rs); break; }
            default: break;
        }
    }

    static boolean contains(String[] a, String v) { for (String s : a) if (s.equals(v)) return true; return false; }

    void sendInput() {
        if (myRole.equals("boss")) {
            final Game.BossIn b = game.bossIn;
            final int f1 = cnt[0], f2 = cnt[1], sp = cnt[2];
            send(M_IN_BOSS, o -> { o.writeFloat(b.mx); o.writeFloat(b.my); o.writeFloat(b.mz); o.writeFloat(b.cy); o.writeInt(f1); o.writeInt(f2); o.writeInt(sp); });
        } else {
            send(M_IN_PVO, o -> {
                o.writeFloat(game.aimYaw); o.writeFloat(game.aimPitch); o.writeBoolean(game.firing); o.writeInt(game.netMis);
                for (int i = 0; i < 4; i++) o.writeInt(game.netAb[i]);
            });
        }
    }

    /** Счётчики нажатий босса на клиенте (f1, f2, особое). */
    public final int[] cnt = new int[3];

    public void bossPress(int i) {
        if (isHost) { if (i == 0) game.bossIn.f1++; else if (i == 1) game.bossIn.f2++; else game.bossIn.sp++; }
        else cnt[i]++;
        G.sfx("click", 0.3f);
    }

    final Quaternion q = new Quaternion();

    void sendSnap() {
        final Game g = game;
        final List<Object[]> ev = new ArrayList<>(events);
        events.clear();
        send(M_SNAP, o -> {
            o.writeFloat(g.duelT); o.writeFloat(g.hp); o.writeFloat(g.st("maxHp")); o.writeInt(g.missiles); o.writeInt((int) g.st("maxMis"));
            o.writeFloat(g.aimYaw); o.writeFloat(g.aimPitch); o.writeBoolean(g.firing); o.writeBoolean(g.overheated); o.writeFloat(g.heat);
            for (int i = 0; i < 3; i++) o.writeFloat(g.bossCd[i]);
            o.writeFloat(g.bossSpecT);
            o.writeBoolean(g.abUsed.get("salvo")); o.writeBoolean(g.abUsed.get("emp")); o.writeFloat(g.abCd.get("smoke")); o.writeFloat(g.abCd.get("repair"));
            o.writeFloat(g.empT); o.writeFloat(g.smokeT);
            o.writeShort(g.enemies.size);
            for (Game.E e : g.enemies) {
                o.writeInt(e.id); o.writeShort(typeIdx(e.type));
                o.writeFloat(e.pos.x); o.writeFloat(e.pos.y); o.writeFloat(e.pos.z);
                e.g.rot.getRotation(q, true);
                o.writeFloat(q.x); o.writeFloat(q.y); o.writeFloat(q.z); o.writeFloat(q.w);
                int fl = (e.cloak != 0 ? 1 : 0) | (e.inv ? 2 : 0) | (g.hidden(e) ? 4 : 0);
                o.writeByte(fl); o.writeFloat(e.hp / Math.max(1f, e.max));
            }
            o.writeShort(g.rockets.size);
            for (Game.Rocket r : g.rockets) { o.writeFloat(r.pos.x); o.writeFloat(r.pos.y); o.writeFloat(r.pos.z); o.writeFloat(r.vel.x); o.writeFloat(r.vel.y); o.writeFloat(r.vel.z); }
            o.writeShort(ev.size());
            for (Object[] e : ev) {
                o.writeUTF((String) e[0]);
                o.writeByte(e.length - 1);
                for (int i = 1; i < e.length; i++) {
                    if (e[i] instanceof String) { o.writeByte(1); o.writeUTF((String) e[i]); }
                    else { o.writeByte(0); o.writeFloat(((Number) e[i]).floatValue()); }
                }
            }
        });
    }

    void readSnap(DataInputStream in) throws IOException {
        Game g = game;
        g.duelT = in.readFloat();
        float nhp = in.readFloat();
        if (prevHp >= 0 && nhp < prevHp - 0.01f) {
            g.hurtT = 0.5f; g.shake = Math.max(g.shake, Math.min(1f, 0.3f + (prevHp - nhp) / 20f)); G.sfx("hit", 0.8f, 80); G.hap(45);
        }
        prevHp = nhp; g.hp = nhp; g.ST.put("maxHp", in.readFloat());
        g.missiles = in.readInt(); g.ST.put("maxMis", (float) in.readInt());
        float ay = in.readFloat(), ap = in.readFloat(); boolean fi = in.readBoolean(), oh = in.readBoolean(); float heat = in.readFloat();
        for (int i = 0; i < 3; i++) g.bossCd[i] = in.readFloat();
        g.bossSpecT = in.readFloat();
        g.abUsed.put("salvo", in.readBoolean()); g.abUsed.put("emp", in.readBoolean()); g.abCd.put("smoke", in.readFloat()); g.abCd.put("repair", in.readFloat());
        g.empT = in.readFloat(); g.smokeT = in.readFloat();
        if (myRole.equals("boss")) {
            g.aimYaw = g.aimYaw + Strike.angDiff(g.aimYaw, ay) * 0.6f; g.aimPitch = ap; g.firing = fi;
            g.overheated = oh; g.heat = heat;
        }
        g.syncNoses();
        int ne = in.readShort();
        Set<Integer> seen = new HashSet<>();
        String[] ty = types();
        for (int j = 0; j < ne; j++) {
            int id = in.readInt(); int ti = in.readShort();
            float x = in.readFloat(), y = in.readFloat(), z = in.readFloat();
            float qx = in.readFloat(), qy = in.readFloat(), qz = in.readFloat(), qw = in.readFloat();
            int fl = in.readByte(); float hk = in.readFloat();
            seen.add(id);
            Game.E e = vis.get(id);
            if (e == null) e = makeVis(id, ty[Math.max(0, Math.min(ty.length - 1, ti))], new Vector3(x, y, z));
            e.vel.set(x, y, z).sub(e.tgtPos).scl(1f / 0.05f);
            if (!e.hasTgt) e.vel.setZero();
            e.tgtPos.set(x, y, z); e.hasTgt = true;
            e.tgtRot.set(qx, qy, qz, qw);
            e.cloak = (fl & 4) != 0 ? 1 : 0; e.inv = (fl & 2) != 0;
            e.hp = hk; e.max = 1;
            e.md.body.alpha = (fl & 1) != 0 ? 0.14f : ((fl & 4) != 0 && myRole.equals("pvo") ? 0.4f : 1f);
        }
        for (Integer id : new ArrayList<>(vis.keySet())) if (!seen.contains(id)) dropVis(id);
        int nrk = in.readShort();
        while (rkVis.size < nrk) { Node3 m = new Node3(Models.missileMesh(), Models.MAT_MISSILE); g.root.add(m); rkVis.add(m); }
        for (int i = 0; i < rkVis.size; i++) {
            Node3 m = rkVis.get(i);
            if (i < nrk) {
                Vector3 p = new Vector3(in.readFloat(), in.readFloat(), in.readFloat());
                Vector3 v = new Vector3(in.readFloat(), in.readFloat(), in.readFloat());
                m.visible = true; m.pos.set(p);
                if (v.len() > 1) { v.nor(); m.lookDir(v, Math.abs(v.y) < 0.98f ? Vector3.Y : Vector3.X); }
                if (MathUtils.random() < 0.6f) FX.puff(p, 0.9f, TRAIL_C, 0.45f, 1.2f, 0.5f);
            } else m.visible = false;
        }
        int nev = in.readShort();
        for (int i = 0; i < nev; i++) {
            String k = in.readUTF();
            int n = in.readByte();
            Object[] a = new Object[n];
            for (int j = 0; j < n; j++) a[j] = in.readByte() == 1 ? in.readUTF() : (Object) in.readFloat();
            playEvent(k, a);
        }
    }

    static final Color TRAIL_C = new Color(0.8f, 0.8f, 0.78f, 1);

    float f(Object o) { return o instanceof Float ? (Float) o : 0f; }

    void playEvent(String k, Object[] a) {
        switch (k) {
            case "boom": {
                Vector3 p = new Vector3(f(a[0]), f(a[1]), f(a[2]));
                float s = f(a[3]);
                FX.explosion(p, s, true);
                G.sfx("boom", MathUtils.clamp(1.2f - p.dst(game.cam.position) / 900f, 0.12f, 1f) * Math.min(1.4f, s), 30);
                break;
            }
            case "kill": { Game.E e = vis.get((int) f(a[0])); if (e != null) e.hp = 0; break; }
            case "bolt": game.bolt(new Vector3(f(a[0]), f(a[1]), f(a[2])), new Vector3(f(a[3]), f(a[4]), f(a[5]))); G.sfx("emp", 0.6f, 200); break;
            case "toast": if (ui != null && a.length > 0 && a[0] instanceof String) ui.toast((String) a[0]); break;
            default: break;
        }
    }

    Game.E makeVis(int id, String type, Vector3 p) {
        Models.EnemyModel md = Models.enemy(type, "harpy");
        game.root.add(md.g); md.g.pos.set(p);
        Game.E e = new Game.E();
        e.id = id; e.type = type; e.md = md; e.g = md.g; e.pos.set(p); e.tgtPos.set(p); e.hp = 1; e.max = 1; e.r = md.r;
        e.boss = Game.isBoss(type); e.ground = Game.isGround(type); e.net = true;
        if (type.equals("erk")) e.trail = FX.trail(md.g, false);
        vis.put(id, e); game.enemies.add(e);
        if (e.boss) game.bosses.add(e);
        return e;
    }

    void dropVis(int id) {
        Game.E e = vis.remove(id);
        if (e == null) return;
        game.enemies.removeValue(e, true); game.bosses.removeValue(e, true);
        if (e.trail != null) { FX.detach(e.trail); e.trail = null; }
        boolean shot = e.type.equals("erk") || e.type.equals("shell") || e.type.equals("node");
        if (!e.ground && !shot && e.hp <= 0.05f) { game.explosion(e.pos, e.boss ? 4f : 1.1f); game.makeWreck(e, e.boss ? 4f : 1.1f); }
        else e.g.remove();
    }

    public void clearVis() {
        for (Game.E e : vis.values()) {
            if (e.trail != null) FX.detach(e.trail);
            e.g.remove();
            game.enemies.removeValue(e, true); game.bosses.removeValue(e, true);
        }
        vis.clear();
        for (Node3 m : rkVis) m.remove();
        rkVis.clear();
    }

    final Quaternion cur = new Quaternion();

    void updateVis(float dt) {
        for (Game.E e : vis.values()) {
            e.t += dt;
            e.pos.lerp(e.tgtPos, Math.min(1f, dt * 14f));
            e.g.pos.set(e.pos);
            e.g.rot.getRotation(cur, true);
            cur.slerp(e.tgtRot, Math.min(1f, dt * 10f));
            e.g.rot.set(cur);
            if (e.md.rot != null) e.md.rot.rotateY(dt * 9f);
        }
    }
}
