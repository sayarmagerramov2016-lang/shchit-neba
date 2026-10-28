package ru.shitneba.android;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import ru.shitneba.Duel;
import ru.shitneba.G;
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
        // гифки «лучшего момента» — в «Картинки/ShchitNeba3D» (Android 10+ без разрешений), на старых — в папку игры
        G.platform = (data, name, mime) -> {
            if (Build.VERSION.SDK_INT >= 29) {
                ContentResolver cr = getContentResolver();
                ContentValues v = new ContentValues();
                v.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                v.put(MediaStore.MediaColumns.MIME_TYPE, mime);
                v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ShchitNeba3D");
                Uri u = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
                if (u == null) throw new IllegalStateException("MediaStore не дал место");
                try (OutputStream o = cr.openOutputStream(u)) { o.write(data); }
                return "Галерея → Картинки/ShchitNeba3D";
            }
            File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (dir == null) dir = getFilesDir();
            File f = new File(dir, name);
            try (FileOutputStream o = new FileOutputStream(f)) { o.write(data); }
            return f.getAbsolutePath();
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
