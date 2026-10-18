package ru.shitneba.android;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import ru.shitneba.ShieldGame;

public class AndroidLauncher extends AndroidApplication {
    private WifiManager.MulticastLock mlock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm != null) { mlock = wm.createMulticastLock("shitneba-duel"); mlock.setReferenceCounted(false); mlock.acquire(); }
        } catch (Throwable ignored) { }
        AndroidApplicationConfiguration cfg = new AndroidApplicationConfiguration();
        cfg.useImmersiveMode = true;
        cfg.useAccelerometer = false;
        cfg.useCompass = false;
        cfg.numSamples = 0;
        cfg.depth = 16;
        initialize(new ShieldGame(), cfg);
    }

    @Override
    protected void onDestroy() {
        try { if (mlock != null && mlock.isHeld()) mlock.release(); } catch (Throwable ignored) { }
        super.onDestroy();
    }
}
