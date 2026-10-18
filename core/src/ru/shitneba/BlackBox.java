package ru.shitneba;

import com.badlogic.gdx.Gdx;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class BlackBox {
   public static volatile long beat;
   public static volatile String place = "запуск";
   public static volatile String info = "";
   public static volatile boolean hold;
   static volatile boolean running;
   static Thread main;
   static Thread dog;
   static File dir;
   static final String[] ring = new String[40];
   static int ringPos;
   static final SimpleDateFormat TF = new SimpleDateFormat("HH:mm:ss", Locale.US);
   static final long FREEZE_MS = 2500L;

   public static void start() {
      try {
         dir = Gdx.files.local("").file().getAbsoluteFile();
      } catch (Throwable var1) {
         dir = new File(".");
      }

      main = Thread.currentThread();
      beat = System.currentTimeMillis();
      log("запуск · версия 3.1.2 · " + Gdx.app.getType() + " · " + Gdx.graphics.getWidth() + "×" + Gdx.graphics.getHeight());
      UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
      Thread.setDefaultUncaughtExceptionHandler((th, ex) -> {
         try {
            crash(th, ex);
         } catch (Throwable var4) {
         }

         if (prev != null) {
            prev.uncaughtException(th, ex);
         }
      });
      running = true;
      dog = new Thread(BlackBox::watch, "blackbox-watchdog");
      dog.setDaemon(true);
      dog.start();
   }

   public static void stop() {
      running = false;
   }

   public static void mark(String p) {
      place = p;
   }

   public static void tick() {
      beat = System.currentTimeMillis();
   }

   public static synchronized void log(String s) {
      ring[ringPos % ring.length] = TF.format(new Date()) + "  " + s;
      ringPos++;
      if (Gdx.app != null) {
         Gdx.app.log("bb", s);
      }
   }

   static synchronized String journal() {
      StringBuilder b = new StringBuilder();
      int n = Math.min(ringPos, ring.length);

      for (int i = ringPos - n; i < ringPos; i++) {
         b.append(ring[i % ring.length]).append('\n');
      }

      return b.toString();
   }

   static String header(String title) {
      return title
         + "\nВремя: "
         + new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.US).format(new Date())
         + "\nВерсия: 3.1.2\nУстройство: "
         + System.getProperty("os.name", "?")
         + " "
         + System.getProperty("os.version", "")
         + " · Java "
         + System.getProperty("java.version", "?")
         + "\nМесто: "
         + place
         + "\nСостояние: "
         + info
         + "\n";
   }

   static String stack(StackTraceElement[] st, int max) {
      StringBuilder b = new StringBuilder();

      for (int i = 0; i < Math.min(max, st.length); i++) {
         b.append("  at ").append(st[i]).append('\n');
      }

      return b.toString();
   }

   static void crash(Thread th, Throwable ex) {
      StringWriter sw = new StringWriter();
      ex.printStackTrace(new PrintWriter(sw));
      String s = sw.toString();
      if (s.length() > 6000) {
         s = s.substring(0, 6000) + "\n…";
      }

      write("crash.txt", header("ВЫЛЕТ ИГРЫ") + "Поток: " + th.getName() + "\n\nОшибка:\n" + s + "\nЖурнал:\n" + journal(), false);
   }

   static void watch() {
      boolean reported = false;
      long since = 0L;

      while (running) {
         try {
            Thread.sleep(400L);
         } catch (InterruptedException var6) {
            return;
         }

         if (hold) {
            beat = System.currentTimeMillis();
         } else {
            long age = System.currentTimeMillis() - beat;
            if (age > 2500L && !reported) {
               reported = true;
               since = beat;
               String st = main != null ? stack(main.getStackTrace(), 30) : "";
               write(
                  "freeze.txt",
                  header("ЗАВИСАНИЕ") + "Нет кадров уже " + (float)age / 1000.0F + " с\n\nГде стоит игра:\n" + st + "\nЖурнал:\n" + journal(),
                  false
               );
            } else if (reported && beat != since) {
               reported = false;
               write("freeze.txt", "Ожила через " + String.format(Locale.US, "%.1f", (float)(beat - since) / 1000.0F) + " с\n", true);
            }
         }
      }
   }

   static synchronized void write(String name, String text, boolean append) {
      try (Writer w = new OutputStreamWriter(new FileOutputStream(new File(dir, name), append), StandardCharsets.UTF_8)) {
         w.write(text);
      } catch (Throwable var8) {
      }
   }

   public static String takeReport() {
      StringBuilder b = new StringBuilder();

      for (String n : new String[]{"crash.txt", "freeze.txt"}) {
         try {
            File f = new File(dir, n);
            if (f.exists()) {
               ByteArrayOutputStream bo = new ByteArrayOutputStream();
               FileInputStream in = new FileInputStream(f);

               try {
                  byte[] buf = new byte[4096];

                  int n2;
                  while ((n2 = in.read(buf)) > 0) {
                     bo.write(buf, 0, n2);
                  }
               } catch (Throwable var11) {
                  try {
                     in.close();
                  } catch (Throwable var10) {
                     var11.addSuppressed(var10);
                  }

                  throw var11;
               }

               in.close();
               if (b.length() > 0) {
                  b.append("\n\n");
               }

               b.append(new String(bo.toByteArray(), StandardCharsets.UTF_8).trim());
               f.delete();
            }
         } catch (Throwable var12) {
         }
      }

      return b.length() == 0 ? null : b.toString();
   }

   private BlackBox() {
   }
}
