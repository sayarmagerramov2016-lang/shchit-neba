package ru.shitneba.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import ru.shitneba.ShieldGame;

public class DesktopLauncher {
   public static void main(String[] args) {
      if (args.length > 0) {
         ShieldGame.shotDir = args[0];
      }

      if (args.length > 1 && !args[1].equals("-")) {
         ShieldGame.forceVeh = args[1];
      }

      if (args.length > 2 && args[2].equals("camp")) {
         ShieldGame.SEQ = ShieldGame.SEQ_CAMP;
      }

      if (args.length > 2 && args[2].equals("strike")) {
         ShieldGame.SEQ = ShieldGame.SEQ_STRIKE;
      }

      if (args.length > 2 && args[2].equals("duelhost")) {
         ShieldGame.SEQ = ShieldGame.SEQ_DUELHOST;
      }

      if (args.length > 2 && args[2].equals("dueljoin")) {
         ShieldGame.SEQ = ShieldGame.SEQ_DUELJOIN;
      }

      Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
      cfg.setTitle("Щит неба 3D");
      cfg.setWindowedMode(1280, 720);
      cfg.useVsync(true);
      cfg.setForegroundFPS(60);
      cfg.setBackBufferConfig(8, 8, 8, 8, 24, 0, 4);
      new Lwjgl3Application(new ShieldGame(), cfg);
   }
}
