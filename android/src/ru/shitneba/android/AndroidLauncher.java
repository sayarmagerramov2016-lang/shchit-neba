package ru.shitneba.android;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import ru.shitneba.Duel;
import ru.shitneba.ShieldGame;

public class AndroidLauncher extends AndroidApplication {
    WifiManager.MulticastLock lock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Duel.platform = on -> {
            try {
                if (lock == null) {
                    WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    if (wm == null) return;
                    lock = wm.createMulticastLock("shitneba-duel");
                    lock.setReferenceCounted(false);
                }
                if (on) lock.acquire(); else if (lock.isHeld()) lock.release();
            } catch (Throwable ignored) { }
        };
        AndroidApplicationConfiguration cfg = new AndroidApplicationConfiguration();
        cfg.useImmersiveMode = true;
        cfg.useAccelerometer = false;
        cfg.useCompass = false;
        cfg.numSamples = 0;
        cfg.depth = 24;
        initialize(new ShieldGame(), cfg);
    }

    @Override
    protected void onDestroy() {
        try { if (lock != null && lock.isHeld()) lock.release(); } catch (Throwable ignored) { }
        super.onDestroy();
    }
}
