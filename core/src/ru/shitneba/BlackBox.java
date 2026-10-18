package ru.shitneba;

import com.badlogic.gdx.Gdx;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * «Чёрный ящик»: записывает зависания (сторожевой поток) и вылеты (необработанные исключения)
 * в файлы freeze.txt / crash.txt. При следующем запуске игра показывает отчёт.
 */
public final class BlackBox {
    public static volatile long beat;
    public static volatile String place = "запуск";
    public static volatile String info = "";
    /** Не считать зависанием (загрузка, прогрев, сворачивание). */
    public static volatile boolean hold;
    static volatile boolean running;
    static Thread main, dog;
    static File dir;
    static final String[] ring = new String[40];
    static int ringPos;
    static final SimpleDateFormat TF = new SimpleDateFormat("HH:mm:ss", Locale.US);
    static final long FREEZE_MS = 2500;

    public static void start() {
        try {
            dir = Gdx.files.local("").file().getAbsoluteFile();
        } catch (Throwable t) { dir = new File("."); }
        main = Thread.currentThread();
        beat = System.currentTimeMillis();
        log("запуск · версия " + G.VER + " · " + Gdx.app.getType() + " · " + Gdx.graphics.getWidth() + "×" + Gdx.graphics.getHeight());
        final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((th, ex) -> {
            try { crash(th, ex); } catch (Throwable ignored) { }
            if (prev != null) prev.uncaughtException(th, ex);
        });
        running = true;
        dog = new Thread(BlackBox::watch, "blackbox-watchdog");
        dog.setDaemon(true);
        dog.start();
    }

    public static void stop() { running = false; }

    /** Отметка «где сейчас игра» — вызывать часто, это дёшево. */
    public static void mark(String p) { place = p; }

    /** Сердцебиение — раз за кадр. */
    public static void tick() { beat = System.currentTimeMillis(); }

    public static synchronized void log(String s) {
        ring[ringPos % ring.length] = TF.format(new Date()) + "  " + s;
        ringPos++;
        if (Gdx.app != null) Gdx.app.log("bb", s);
    }

    static synchronized String journal() {
        StringBuilder b = new StringBuilder();
        int n = Math.min(ringPos, ring.length);
        for (int i = ringPos - n; i < ringPos; i++) b.append(ring[i % ring.length]).append('\n');
        return b.toString();
    }

    static String header(String title) {
        return title + "\n"
                + "Время: " + new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.US).format(new Date()) + "\n"
                + "Версия: " + G.VER + "\n"
                + "Устройство: " + System.getProperty("os.name", "?") + " " + System.getProperty("os.version", "") + " · Java " + System.getProperty("java.version", "?") + "\n"
                + "Место: " + place + "\n"
                + "Состояние: " + info + "\n";
    }

    static String stack(StackTraceElement[] st, int max) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < Math.min(max, st.length); i++) b.append("  at ").append(st[i]).append('\n');
        return b.toString();
    }

    static void crash(Thread th, Throwable ex) {
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        String s = sw.toString();
        if (s.length() > 6000) s = s.substring(0, 6000) + "\n…";
        write("crash.txt", header("ВЫЛЕТ ИГРЫ") + "Поток: " + th.getName() + "\n\nОшибка:\n" + s + "\nЖурнал:\n" + journal(), false);
    }

    static void watch() {
        boolean reported = false;
        long since = 0;
        while (running) {
            try { Thread.sleep(400); } catch (InterruptedException e) { return; }
            if (hold) { beat = System.currentTimeMillis(); continue; }
            long age = System.currentTimeMillis() - beat;
            if (age > FREEZE_MS && !reported) {
                reported = true; since = beat;
                String st = main != null ? stack(main.getStackTrace(), 30) : "";
                write("freeze.txt", header("ЗАВИСАНИЕ") + "Нет кадров уже " + (age / 1000f) + " с\n\nГде стоит игра:\n" + st + "\nЖурнал:\n" + journal(), false);
            } else if (reported && beat != since) {
                reported = false;
                write("freeze.txt", "Ожила через " + String.format(Locale.US, "%.1f", (beat - since) / 1000f) + " с\n", true);
            }
        }
    }

    static synchronized void write(String name, String text, boolean append) {
        try (Writer w = new OutputStreamWriter(new FileOutputStream(new File(dir, name), append), StandardCharsets.UTF_8)) {
            w.write(text);
        } catch (Throwable ignored) { }
    }

    /** Отчёт с прошлого запуска (вылет или зависание), или null. Файлы удаляются. */
    public static String takeReport() {
        StringBuilder b = new StringBuilder();
        for (String n : new String[]{"crash.txt", "freeze.txt"}) {
            try {
                File f = new File(dir, n);
                if (!f.exists()) continue;
                java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
                try (java.io.FileInputStream in = new java.io.FileInputStream(f)) {
                    byte[] buf = new byte[4096]; int n2;
                    while ((n2 = in.read(buf)) > 0) bo.write(buf, 0, n2);
                }
                if (b.length() > 0) b.append("\n\n");
                b.append(new String(bo.toByteArray(), StandardCharsets.UTF_8).trim());
                f.delete();
            } catch (Throwable ignored) { }
        }
        return b.length() == 0 ? null : b.toString();
    }

    private BlackBox() { }
}
