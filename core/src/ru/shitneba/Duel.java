package ru.shitneba;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.Array.ArrayIterator;
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

public class Duel {
   public static final int PORT = 47100;
   public static final int DISC_PORT = 47101;
   static final String MAGIC = "SKYDUEL1";
   public static final String[] VEHS = G.VEHS;
   public static Duel.Platform platform;
   static final byte M_LOBBY = 1;
   static final byte M_PICK = 2;
   static final byte M_START = 3;
   static final byte M_IN_BOSS = 4;
   static final byte M_IN_PVO = 5;
   static final byte M_SNAP = 6;
   static final byte M_END = 7;
   static final byte M_BYE = 8;
   final Game game;
   public GameUI ui;
   public volatile boolean isHost;
   public volatile boolean connected;
   public String myRole = "pvo";
   public String myPick = "";
   public String theirPick = "";
   public boolean theirReady;
   public boolean inBattle;
   public String hostName = "";
   public final Map<String, String[]> found = Collections.synchronizedMap(new LinkedHashMap<>());
   public String status = "";
   ServerSocket server;
   Socket sock;
   DataOutputStream out;
   final LinkedBlockingQueue<byte[]> sendQ = new LinkedBlockingQueue<>();
   final ConcurrentLinkedQueue<byte[]> recvQ = new ConcurrentLinkedQueue<>();
   final ConcurrentLinkedQueue<Runnable> mainQ = new ConcurrentLinkedQueue<>();
   volatile boolean running;
   DatagramSocket discSend;
   DatagramSocket discRecv;
   Thread discThread;
   float snapT;
   float inT;
   final List<Object[]> events = new ArrayList<>();
   final Map<String, Integer> lastCnt = new HashMap<>();
   final int[] lastAb = new int[4];
   final Map<Integer, Game.E> vis = new HashMap<>();
   final Array<Node3> rkVis = new Array();
   float prevHp = -1.0F;
   static String[] TYPES;
   public final int[] cnt = new int[3];
   final Quaternion q = new Quaternion();
   final Quaternion cur = new Quaternion();

   public boolean hosting() {
      return this.server != null;
   }

   public Duel(Game g) {
      this.game = g;
   }

   static String[] types() {
      if (TYPES == null) {
         List<String> l = new ArrayList<>();

         for (JsonValue v = G.D.get("DT").child; v != null; v = v.next) {
            l.add(v.name);
         }

         Collections.sort(l);
         TYPES = l.toArray(new String[0]);
      }

      return TYPES;
   }

   static int typeIdx(String t) {
      String[] ts = types();

      for (int i = 0; i < ts.length; i++) {
         if (ts[i].equals(t)) {
            return i;
         }
      }

      return 0;
   }

   public static String localIp() {
      String best = "";

      try {
         Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();

         while (en.hasMoreElements()) {
            NetworkInterface ni = en.nextElement();
            if (ni.isUp() && !ni.isLoopback()) {
               Enumeration<InetAddress> ad = ni.getInetAddresses();

               while (ad.hasMoreElements()) {
                  InetAddress a = ad.nextElement();
                  if (a instanceof Inet4Address) {
                     String s = a.getHostAddress();
                     if (!s.startsWith("127.") && !s.startsWith("169.254.")) {
                        if (s.startsWith("192.168.") || s.startsWith("10.") || s.startsWith("172.")) {
                           return s;
                        }

                        if (best.isEmpty()) {
                           best = s;
                        }
                     }
                  }
               }
            }
         }
      } catch (Throwable var6) {
      }

      return best.isEmpty() ? "127.0.0.1" : best;
   }

   public String host(String role, String pick) {
      this.stop();

      try {
         this.server = new ServerSocket();
         this.server.setReuseAddress(true);
         this.server.bind(new InetSocketAddress(47100));
      } catch (IOException var9) {
         this.server = null;
         return "Не удалось открыть порт 47100";
      }

      this.isHost = true;
      this.myRole = role;
      this.myPick = pick;
      this.theirPick = "";
      this.theirReady = false;
      this.connected = false;
      this.running = true;
      String ip = localIp();
      this.hostName = "Игрок " + ip.substring(ip.lastIndexOf(46) + 1);
      ServerSocket srv = this.server;
      Thread t = new Thread(() -> {
         try {
            Socket s = srv.accept();
            this.attach(s);
            this.mainQ.add(() -> {
               this.connected = true;
               this.sendLobby();
               if (this.ui != null) {
                  this.ui.toast("Соперник подключился");
               }
            });
         } catch (IOException var3x) {
         }
      }, "duel-accept");
      t.setDaemon(true);
      t.start();

      try {
         this.discSend = new DatagramSocket();
         this.discSend.setBroadcast(true);
      } catch (IOException var8) {
         this.discSend = null;
      }

      DatagramSocket ds = this.discSend;
      Thread b = new Thread(() -> {
         while (this.running && this.isHost && ds != null) {
            if (!this.connected) {
               try {
                  byte[] msg = ("SKYDUEL1|" + this.hostName + "|" + this.myRole + "|" + this.myPick).getBytes(StandardCharsets.UTF_8);
                  ds.send(new DatagramPacket(msg, msg.length, InetAddress.getByName("255.255.255.255"), 47101));
               } catch (Throwable var3x) {
               }
            }

            try {
               Thread.sleep(1000L);
            } catch (InterruptedException var4x) {
               break;
            }
         }
      }, "duel-bcast");
      b.setDaemon(true);
      b.start();
      return "";
   }

   public String join(String ip0) {
      this.stopDiscovery();
      this.closeSocket();
      String ip = ip0.trim();
      this.isHost = false;
      this.connected = false;
      this.myPick = "";
      this.theirPick = "";
      this.running = true;
      Thread t = new Thread(() -> {
         try {
            Socket s = new Socket();
            s.connect(new InetSocketAddress(ip, 47100), 5000);
            this.attach(s);
            this.mainQ.add(() -> this.connected = true);
         } catch (IOException var3x) {
            this.mainQ.add(() -> {
               this.connected = false;
               if (this.ui != null) {
                  this.ui.toast("Не удалось подключиться");
               }
            });
         }
      }, "duel-connect");
      t.setDaemon(true);
      t.start();
      return "";
   }

   void attach(Socket s) throws IOException {
      s.setTcpNoDelay(true);
      this.sock = s;
      this.out = new DataOutputStream(s.getOutputStream());
      DataInputStream in = new DataInputStream(s.getInputStream());
      this.sendQ.clear();
      Thread r = new Thread(() -> {
         try {
            while (this.running) {
               int len = in.readInt();
               if (len >= 0 && len <= 4000000) {
                  byte[] b = new byte[len];
                  in.readFully(b);
                  this.recvQ.add(b);
                  continue;
               }
               break;
            }
         } catch (IOException var4x) {
         }

         this.mainQ.add(this::onDisconnected);
      }, "duel-read");
      r.setDaemon(true);
      r.start();
      DataOutputStream o = this.out;
      Thread w = new Thread(() -> {
         try {
            while (this.running) {
               byte[] b = this.sendQ.poll(500L, TimeUnit.MILLISECONDS);
               if (b != null) {
                  o.writeInt(b.length);
                  o.write(b);
                  o.flush();
               }
            }
         } catch (Throwable var3x) {
         }
      }, "duel-write");
      w.setDaemon(true);
      w.start();
   }

   void onDisconnected() {
      if (this.connected || this.inBattle) {
         this.connected = false;
         this.theirReady = false;
         this.theirPick = "";
         if (this.inBattle) {
            this.finish("", this.isHost ? "Соперник отключился" : "Хост отключился");
         } else if (this.ui != null) {
            this.ui.toast("Соперник отключился");
         }
      }
   }

   void closeSocket() {
      try {
         if (this.sock != null) {
            this.sock.close();
         }
      } catch (IOException var2) {
      }

      this.sock = null;
      this.out = null;
   }

   public void stop() {
      this.running = false;
      this.stopDiscovery();
      if (this.discSend != null) {
         this.discSend.close();
         this.discSend = null;
      }

      try {
         if (this.server != null) {
            this.server.close();
         }
      } catch (IOException var2) {
      }

      this.server = null;
      this.closeSocket();
      this.connected = false;
      this.inBattle = false;
      this.isHost = false;
      this.recvQ.clear();
      this.sendQ.clear();
      this.clearVis();
   }

   public void startDiscovery() {
      this.stopDiscovery();
      this.found.clear();
      if (platform != null) {
         platform.multicast(true);
      }

      try {
         this.discRecv = new DatagramSocket(null);
         this.discRecv.setReuseAddress(true);
         this.discRecv.setBroadcast(true);
         this.discRecv.bind(new InetSocketAddress(47101));
         this.discRecv.setSoTimeout(500);
      } catch (IOException var2) {
         this.discRecv = null;
         return;
      }

      DatagramSocket ds = this.discRecv;
      this.discThread = new Thread(() -> {
         byte[] buf = new byte[512];

         while (!ds.isClosed()) {
            try {
               DatagramPacket p = new DatagramPacket(buf, buf.length);
               ds.receive(p);
               String pk = new String(p.getData(), 0, p.getLength(), StandardCharsets.UTF_8);
               String[] parts = pk.split("\\|");
               if (parts.length >= 4 && parts[0].equals("SKYDUEL1")) {
                  String ip = p.getAddress().getHostAddress();
                  this.found.put(ip, new String[]{parts[1], parts[2], parts[3], String.valueOf(System.currentTimeMillis())});
               }
            } catch (IOException var8) {
            }

            long now = System.currentTimeMillis();
            synchronized (this.found) {
               Iterator<String[]> it = this.found.values().iterator();

               while (it.hasNext()) {
                  if (now - Long.parseLong(it.next()[3]) > 4000L) {
                     it.remove();
                  }
               }
            }
         }
      }, "duel-disc");
      this.discThread.setDaemon(true);
      this.discThread.start();
   }

   public void stopDiscovery() {
      if (this.discRecv != null) {
         this.discRecv.close();
         this.discRecv = null;
      }

      if (platform != null) {
         platform.multicast(false);
      }
   }

   void send(byte type, Duel.W body) {
      if (this.out != null) {
         try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream(256);
            DataOutputStream o = new DataOutputStream(bo);
            o.writeByte(type);
            if (body != null) {
               body.w(o);
            }

            o.flush();
            if (this.sendQ.size() < 200) {
               this.sendQ.add(bo.toByteArray());
            }
         } catch (IOException var5) {
         }
      }
   }

   void sendLobby() {
      this.send((byte)1, o -> {
         o.writeUTF(this.myRole);
         o.writeUTF(this.myPick);
      });
   }

   public void hostPickChanged() {
      if (this.isHost && this.connected) {
         this.sendLobby();
      }
   }

   public void sendPick(String pick) {
      this.myPick = pick;
      if (!this.isHost && this.connected) {
         this.send((byte)2, o -> o.writeUTF(pick));
      }
   }

   public boolean canStart() {
      return this.isHost && this.connected && this.theirReady && !this.theirPick.isEmpty();
   }

   public void begin() {
      if (this.canStart()) {
         String boss = this.myRole.equals("boss") ? this.myPick : this.theirPick;
         String veh = this.myRole.equals("pvo") ? this.myPick : this.theirPick;
         int theme = MathUtils.random(G.D.get("THEMES").size - 1);
         this.send((byte)3, o -> {
            o.writeUTF(boss);
            o.writeUTF(veh);
            o.writeInt(theme);
            o.writeUTF(this.myRole);
         });
         this.startBattle(boss, veh, theme, this.myRole);
      }
   }

   void startBattle(String boss, String veh, int theme, String hostRole) {
      this.inBattle = true;
      this.events.clear();
      this.prevHp = -1.0F;
      this.lastCnt.clear();

      for (int i = 0; i < 4; i++) {
         this.lastAb[i] = 0;
      }

      this.clearVis();
      String role = this.isHost ? hostRole : (hostRole.equals("pvo") ? "boss" : "pvo");
      this.myRole = role;
      this.game.netView = !this.isHost;
      this.game.duelRole = role;
      this.game.startDuel(boss, veh, theme);
      if (this.ui != null) {
         this.ui.startHud();
      }
   }

   public void pushEvent(Object[] e) {
      if (this.isHost && this.inBattle && this.events.size() < 40) {
         this.events.add(e);
      }
   }

   public void finishHost(String winner, String reason) {
      if (this.isHost && this.inBattle) {
         this.send((byte)7, o -> {
            o.writeUTF(winner);
            o.writeUTF(reason);
         });
         this.finish(winner, reason);
      }
   }

   void finish(String winner, String reason) {
      if (this.inBattle) {
         this.inBattle = false;
         this.game.firing = false;
         this.game.state = "result";
         if (this.ui != null) {
            this.ui.duelResult(winner.isEmpty(), winner.equals(this.myRole), reason);
         }
      }
   }

   public void leave() {
      this.stop();
      this.game.netView = false;
      this.game.duelRole = "";
   }

   public void update(float dt) {
      Runnable r;
      while ((r = this.mainQ.poll()) != null) {
         r.run();
      }

      byte[] m;
      while ((m = this.recvQ.poll()) != null) {
         try {
            this.handle(new DataInputStream(new ByteArrayInputStream(m)));
         } catch (Throwable var5) {
            Gdx.app.error("duel", "bad msg", var5);
         }
      }

      if (this.inBattle && this.out != null) {
         if (this.isHost) {
            this.snapT -= dt;
            if (this.snapT <= 0.0F) {
               this.snapT = 0.05F;
               this.sendSnap();
            }
         } else {
            this.inT -= dt;
            if (this.inT <= 0.0F) {
               this.inT = 0.033333335F;
               this.sendInput();
            }

            this.updateVis(dt);
         }
      }
   }

   void handle(DataInputStream in) throws IOException {
      byte t = in.readByte();
      switch (t) {
         case 1: {
            String hr = in.readUTF();
            String hp = in.readUTF();
            this.myRole = hr.equals("pvo") ? "boss" : "pvo";
            this.theirPick = hp;
            boolean bad = this.myPick.isEmpty()
               || this.myRole.equals("boss") && !contains(Game.DUEL_BOSSES, this.myPick)
               || this.myRole.equals("pvo") && !contains(VEHS, this.myPick);
            if (bad) {
               this.myPick = this.myRole.equals("boss") ? Game.DUEL_BOSSES[1] : VEHS[0];
            }
            break;
         }
         case 2:
            if (this.isHost) {
               this.theirPick = in.readUTF();
               this.theirReady = true;
            }
            break;
         case 3: {
            String boss = in.readUTF();
            String veh = in.readUTF();
            int th = in.readInt();
            String hr = in.readUTF();
            this.startBattle(boss, veh, th, hr);
            break;
         }
         case 4:
            if (!this.isHost || !this.inBattle) {
               return;
            }

            Game.BossIn b = this.game.bossIn;
            b.mx = in.readFloat();
            b.my = in.readFloat();
            b.mz = in.readFloat();
            b.cy = in.readFloat();
            int[] n = new int[]{in.readInt(), in.readInt(), in.readInt()};
            String[] ks = new String[]{"f1", "f2", "sp"};

            for (int i = 0; i < 3; i++) {
               int last = this.lastCnt.containsKey(ks[i]) ? this.lastCnt.get(ks[i]) : 0;
               if (n[i] > last) {
                  if (i == 0) {
                     b.f1 = b.f1 + (n[i] - last);
                  } else if (i == 1) {
                     b.f2 = b.f2 + (n[i] - last);
                  } else {
                     b.sp = b.sp + (n[i] - last);
                  }

                  this.lastCnt.put(ks[i], n[i]);
               }
            }
            break;
         case 5:
            if (!this.isHost || !this.inBattle) {
               return;
            }

            this.game.aimYaw = in.readFloat();
            this.game.aimPitch = in.readFloat();
            this.game.firing = in.readBoolean();
            int n2 = in.readInt();
            int last = this.lastCnt.containsKey("mis") ? this.lastCnt.get("mis") : 0;
            if (n2 > last) {
               for (int ix = 0; ix < n2 - last; ix++) {
                  this.game.fireMissile("lock", false);
               }

               this.lastCnt.put("mis", n2);
            }

            String[] ks2 = new String[]{"salvo", "emp", "smoke", "repair"};

            for (int ix = 0; ix < 4; ix++) {
               int a = in.readInt();
               if (a > this.lastAb[ix]) {
                  this.lastAb[ix] = a;
                  this.game.useAbility(ks2[ix]);
               }
            }
            break;
         case 6:
            if (!this.isHost && this.inBattle) {
               this.readSnap(in);
            }
            break;
         case 7:
            String w = in.readUTF();
            String rs = in.readUTF();
            this.finish(w, rs);
      }
   }

   static boolean contains(String[] a, String v) {
      for (String s : a) {
         if (s.equals(v)) {
            return true;
         }
      }

      return false;
   }

   void sendInput() {
      if (this.myRole.equals("boss")) {
         Game.BossIn b = this.game.bossIn;
         int f1 = this.cnt[0];
         int f2 = this.cnt[1];
         int sp = this.cnt[2];
         this.send((byte)4, o -> {
            o.writeFloat(b.mx);
            o.writeFloat(b.my);
            o.writeFloat(b.mz);
            o.writeFloat(b.cy);
            o.writeInt(f1);
            o.writeInt(f2);
            o.writeInt(sp);
         });
      } else {
         this.send((byte)5, o -> {
            o.writeFloat(this.game.aimYaw);
            o.writeFloat(this.game.aimPitch);
            o.writeBoolean(this.game.firing);
            o.writeInt(this.game.netMis);

            for (int i = 0; i < 4; i++) {
               o.writeInt(this.game.netAb[i]);
            }
         });
      }
   }

   public void bossPress(int i) {
      if (this.isHost) {
         if (i == 0) {
            this.game.bossIn.f1++;
         } else if (i == 1) {
            this.game.bossIn.f2++;
         } else {
            this.game.bossIn.sp++;
         }
      } else {
         this.cnt[i]++;
      }

      G.sfx("click", 0.3F);
   }

   void sendSnap() {
      Game g = this.game;
      List<Object[]> ev = new ArrayList<>(this.events);
      this.events.clear();
      this.send((byte)6, o -> {
         o.writeFloat(g.duelT);
         o.writeFloat(g.hp);
         o.writeFloat(g.st("maxHp"));
         o.writeInt(g.missiles);
         o.writeInt((int)g.st("maxMis"));
         o.writeFloat(g.aimYaw);
         o.writeFloat(g.aimPitch);
         o.writeBoolean(g.firing);
         o.writeBoolean(g.overheated);
         o.writeFloat(g.heat);

         for (int i = 0; i < 3; i++) {
            o.writeFloat(g.bossCd[i]);
         }

         o.writeFloat(g.bossSpecT);
         o.writeBoolean(g.abUsed.get("salvo"));
         o.writeBoolean(g.abUsed.get("emp"));
         o.writeFloat(g.abCd.get("smoke"));
         o.writeFloat(g.abCd.get("repair"));
         o.writeFloat(g.empT);
         o.writeFloat(g.smokeT);
         o.writeShort(g.enemies.size);
         ArrayIterator var7 = g.enemies.iterator();

         while (var7.hasNext()) {
            Game.E e = (Game.E)var7.next();
            o.writeInt(e.id);
            o.writeShort(typeIdx(e.type));
            o.writeFloat(e.pos.x);
            o.writeFloat(e.pos.y);
            o.writeFloat(e.pos.z);
            e.g.rot.getRotation(this.q, true);
            o.writeFloat(this.q.x);
            o.writeFloat(this.q.y);
            o.writeFloat(this.q.z);
            o.writeFloat(this.q.w);
            int fl = (e.cloak != 0 ? 1 : 0) | (e.inv ? 2 : 0) | (g.hidden(e) ? 4 : 0);
            o.writeByte(fl);
            o.writeFloat(e.hp / Math.max(1.0F, e.max));
         }

         o.writeShort(g.rockets.size);
         var7 = g.rockets.iterator();

         while (var7.hasNext()) {
            Game.Rocket r = (Game.Rocket)var7.next();
            o.writeFloat(r.pos.x);
            o.writeFloat(r.pos.y);
            o.writeFloat(r.pos.z);
            o.writeFloat(r.vel.x);
            o.writeFloat(r.vel.y);
            o.writeFloat(r.vel.z);
         }

         o.writeShort(ev.size());

         for (Object[] e : ev) {
            o.writeUTF((String)e[0]);
            o.writeByte(e.length - 1);

            for (int i = 1; i < e.length; i++) {
               if (e[i] instanceof String) {
                  o.writeByte(1);
                  o.writeUTF((String)e[i]);
               } else {
                  o.writeByte(0);
                  o.writeFloat(((Number)e[i]).floatValue());
               }
            }
         }
      });
   }

   void readSnap(DataInputStream in) throws IOException {
      Game g = this.game;
      g.duelT = in.readFloat();
      float nhp = in.readFloat();
      if (this.prevHp >= 0.0F && nhp < this.prevHp - 0.01F) {
         g.hurtT = 0.5F;
         g.shake = Math.max(g.shake, Math.min(1.0F, 0.3F + (this.prevHp - nhp) / 20.0F));
         G.sfx("hit", 0.8F, 80);
         G.hap(45);
      }

      this.prevHp = nhp;
      g.hp = nhp;
      g.ST.put("maxHp", in.readFloat());
      g.missiles = in.readInt();
      g.ST.put("maxMis", (float)in.readInt());
      float ay = in.readFloat();
      float ap = in.readFloat();
      boolean fi = in.readBoolean();
      boolean oh = in.readBoolean();
      float heat = in.readFloat();

      for (int i = 0; i < 3; i++) {
         g.bossCd[i] = in.readFloat();
      }

      g.bossSpecT = in.readFloat();
      g.abUsed.put("salvo", in.readBoolean());
      g.abUsed.put("emp", in.readBoolean());
      g.abCd.put("smoke", in.readFloat());
      g.abCd.put("repair", in.readFloat());
      g.empT = in.readFloat();
      g.smokeT = in.readFloat();
      if (this.myRole.equals("boss")) {
         g.aimYaw = g.aimYaw + Strike.angDiff(g.aimYaw, ay) * 0.6F;
         g.aimPitch = ap;
         g.firing = fi;
         g.overheated = oh;
         g.heat = heat;
      }

      g.syncNoses();
      int ne = in.readShort();
      Set<Integer> seen = new HashSet<>();
      String[] ty = types();

      for (int j = 0; j < ne; j++) {
         int id = in.readInt();
         int ti = in.readShort();
         float x = in.readFloat();
         float y = in.readFloat();
         float z = in.readFloat();
         float qx = in.readFloat();
         float qy = in.readFloat();
         float qz = in.readFloat();
         float qw = in.readFloat();
         int fl = in.readByte();
         float hk = in.readFloat();
         seen.add(id);
         Game.E e = this.vis.get(id);
         if (e == null) {
            e = this.makeVis(id, ty[Math.max(0, Math.min(ty.length - 1, ti))], new Vector3(x, y, z));
         }

         e.vel.set(x, y, z).sub(e.tgtPos).scl(20.0F);
         if (!e.hasTgt) {
            e.vel.setZero();
         }

         e.tgtPos.set(x, y, z);
         e.hasTgt = true;
         e.tgtRot.set(qx, qy, qz, qw);
         e.cloak = (fl & 4) != 0 ? 1 : 0;
         e.inv = (fl & 2) != 0;
         e.hp = hk;
         e.max = 1.0F;
         e.md.body.alpha = (fl & 1) != 0 ? 0.14F : ((fl & 4) != 0 && this.myRole.equals("pvo") ? 0.4F : 1.0F);
      }

      for (Integer idx : new ArrayList<>(this.vis.keySet())) {
         if (!seen.contains(idx)) {
            this.dropVis(idx);
         }
      }

      int nrk = in.readShort();

      while (this.rkVis.size < nrk) {
         Node3 m = new Node3(Models.missileMesh(), Models.MAT_MISSILE);
         g.root.add(m);
         this.rkVis.add(m);
      }

      for (int i = 0; i < this.rkVis.size; i++) {
         Node3 m = (Node3)this.rkVis.get(i);
         if (i < nrk) {
            Vector3 p = new Vector3(in.readFloat(), in.readFloat(), in.readFloat());
            Vector3 v = new Vector3(in.readFloat(), in.readFloat(), in.readFloat());
            m.visible = true;
            m.pos.set(p);
            if (v.len() > 1.0F) {
               v.nor();
               m.lookDir(v, Math.abs(v.y) < 0.98F ? Vector3.Y : Vector3.X);
            }

            if (MathUtils.random() < 0.6F) {
               FX.puff(p, 0.9F, new Color(0.8F, 0.8F, 0.78F, 1.0F), 0.45F, 1.2F, 0.5F);
            }
         } else {
            m.visible = false;
         }
      }

      int nev = in.readShort();

      for (int ix = 0; ix < nev; ix++) {
         String k = in.readUTF();
         int n = in.readByte();
         Object[] a = new Object[n];

         for (int j = 0; j < n; j++) {
            a[j] = in.readByte() == 1 ? in.readUTF() : in.readFloat();
         }

         this.playEvent(k, a);
      }
   }

   float f(Object o) {
      return o instanceof Float ? (Float)o : 0.0F;
   }

   void playEvent(String k, Object[] a) {
      switch (k) {
         case "boom":
            Vector3 p = new Vector3(this.f(a[0]), this.f(a[1]), this.f(a[2]));
            float s = this.f(a[3]);
            FX.explosion(p, s, true);
            G.sfx("boom", MathUtils.clamp(1.2F - p.dst(this.game.cam.position) / 900.0F, 0.12F, 1.0F) * Math.min(1.4F, s), 30);
            break;
         case "kill":
            Game.E e = this.vis.get((int)this.f(a[0]));
            if (e != null) {
               e.hp = 0.0F;
            }
            break;
         case "bolt":
            this.game.bolt(new Vector3(this.f(a[0]), this.f(a[1]), this.f(a[2])), new Vector3(this.f(a[3]), this.f(a[4]), this.f(a[5])));
            G.sfx("emp", 0.6F, 200);
            break;
         case "toast":
            if (this.ui != null && a.length > 0 && a[0] instanceof String) {
               this.ui.toast((String)a[0]);
            }
      }
   }

   Game.E makeVis(int id, String type, Vector3 p) {
      Models.EnemyModel md = Models.enemy(type, "harpy");
      this.game.root.add(md.g);
      md.g.pos.set(p);
      Game.E e = new Game.E();
      e.id = id;
      e.type = type;
      e.md = md;
      e.g = md.g;
      e.pos.set(p);
      e.tgtPos.set(p);
      e.hp = 1.0F;
      e.max = 1.0F;
      e.r = md.r;
      e.boss = Game.isBoss(type);
      e.ground = Game.isGround(type);
      e.net = true;
      if (type.equals("erk")) {
         e.trail = FX.trail(md.g, false);
      }

      this.vis.put(id, e);
      this.game.enemies.add(e);
      if (e.boss) {
         this.game.bosses.add(e);
      }

      return e;
   }

   void dropVis(int id) {
      Game.E e = this.vis.remove(id);
      if (e != null) {
         this.game.enemies.removeValue(e, true);
         this.game.bosses.removeValue(e, true);
         if (e.trail != null) {
            FX.detach(e.trail);
            e.trail = null;
         }

         boolean shot = e.type.equals("erk") || e.type.equals("shell") || e.type.equals("node");
         if (!e.ground && !shot && e.hp <= 0.05F) {
            this.game.explosion(e.pos, e.boss ? 4.0F : 1.1F);
            this.game.makeWreck(e, e.boss ? 4.0F : 1.1F);
         } else {
            e.g.remove();
         }
      }
   }

   public void clearVis() {
      for (Game.E e : this.vis.values()) {
         if (e.trail != null) {
            FX.detach(e.trail);
         }

         e.g.remove();
         this.game.enemies.removeValue(e, true);
         this.game.bosses.removeValue(e, true);
      }

      this.vis.clear();
      ArrayIterator var3 = this.rkVis.iterator();

      while (var3.hasNext()) {
         Node3 m = (Node3)var3.next();
         m.remove();
      }

      this.rkVis.clear();
   }

   void updateVis(float dt) {
      for (Game.E e : this.vis.values()) {
         e.t += dt;
         e.pos.lerp(e.tgtPos, Math.min(1.0F, dt * 14.0F));
         e.g.pos.set(e.pos);
         e.g.rot.getRotation(this.cur, true);
         this.cur.slerp(e.tgtRot, Math.min(1.0F, dt * 10.0F));
         e.g.rot.set(this.cur);
         if (e.md.rot != null) {
            e.md.rot.rotateY(dt * 9.0F);
         }
      }
   }

   public interface Platform {
      void multicast(boolean var1);
   }

   interface W {
      void w(DataOutputStream var1) throws IOException;
   }
}
