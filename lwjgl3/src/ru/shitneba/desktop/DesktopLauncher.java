package ru.shitneba.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import ru.shitneba.ShieldGame;

public class DesktopLauncher {
    public static void main(String[] args) {
        if (args.length > 0) ShieldGame.shotDir = args[0];
        if (args.length > 1 && !args[1].equals("-")) ShieldGame.forceVeh = args[1];
        if (args.length > 2 && args[2].equals("camp")) ShieldGame.SEQ = ShieldGame.SEQ_CAMP;
        if (args.length > 2 && args[2].equals("objs")) ShieldGame.SEQ = ShieldGame.SEQ_OBJ;
        if (args.length > 2 && args[2].equals("models")) ShieldGame.studioMode = true;
        if (args.length > 2 && args[2].equals("maps")) ShieldGame.SEQ = ShieldGame.SEQ_MAPS;
        if (args.length > 2 && args[2].equals("store")) ShieldGame.SEQ = ShieldGame.SEQ_STORE;
        if (args.length > 2 && args[2].equals("fx")) ShieldGame.SEQ = ShieldGame.SEQ_FX;
        if (args.length > 2 && args[2].equals("strike")) ShieldGame.SEQ = ShieldGame.SEQ_STRIKE;
        if (args.length > 2 && args[2].equals("duelhost")) ShieldGame.SEQ = ShieldGame.SEQ_DUELHOST;
        if (args.length > 2 && args[2].equals("dueljoin")) ShieldGame.SEQ = ShieldGame.SEQ_DUELJOIN;
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setTitle("Щит неба 3D");
        int w = 1280, h = 720;
        try { if (System.getenv("SN_W") != null) { w = Integer.parseInt(System.getenv("SN_W")); h = Integer.parseInt(System.getenv("SN_H")); } } catch (Exception ignored) {}
        cfg.setWindowedMode(w, h);
        cfg.useVsync(true);
        cfg.setForegroundFPS(60);
        cfg.setBackBufferConfig(8, 8, 8, 8, 24, 0, 4);
        new Lwjgl3Application(new ShieldGame(), cfg);
    }
}
