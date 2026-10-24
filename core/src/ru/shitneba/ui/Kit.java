package ru.shitneba.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;

import java.util.HashMap;
import java.util.Map;

/**
 * UI-кит главного меню (нарисован в SpriteCook, приглушён store/tools/tone_ui.py).
 * Картинки лежат в assets/ui/menu. Грузятся при первом обращении.
 * Камера интерфейса смотрит осью Y вниз, поэтому v=0 — верх картинки, как и в файле.
 */
public class Kit implements Disposable {
    final Draw d;
    final Map<String, Texture> tx = new HashMap<>();
    /** 9-slice в пикселях картинки: левый, верхний, правый, нижний. */
    static final Map<String, int[]> NS = new HashMap<>();
    static {
        NS.put("menu_button_blue", new int[]{32, 26, 95, 26});
        NS.put("menu_button_gold", new int[]{32, 26, 95, 26});
        NS.put("small_button_plate", new int[]{28, 25, 28, 30});
        NS.put("icon_button_plate", new int[]{30, 30, 30, 30});
        NS.put("radar_panel", new int[]{40, 40, 40, 40});
    }

    public Kit(Draw d) { this.d = d; }

    public Texture t(String name) {
        Texture t = tx.get(name);
        if (t == null) {
            try {
                t = new Texture(Gdx.files.internal("ui/menu/" + name + ".png"), true);
                t.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
            } catch (Exception e) {
                ru.shitneba.BlackBox.mark("нет картинки ui/menu/" + name);
                t = d.white;
            }
            tx.put(name, t);
        }
        return t;
    }

    /** Картинка целиком в прямоугольник. */
    public void img(String name, float x, float y, float w, float h, Color tint, float a) {
        if (d.off) return;
        Texture t = t(name);
        d.b.setColor(tint.r, tint.g, tint.b, a);
        d.b.draw(t, x, y, w, h, 0, 0, 1, 1);
        d.b.setColor(Color.WHITE);
    }

    public void img(String name, float x, float y, float w, float h) { img(name, x, y, w, h, Color.WHITE, 1); }

    /** Картинка вписана в коробку с сохранением пропорций, по центру. */
    public void fit(String name, float x, float y, float w, float h, float a) {
        Texture t = t(name);
        float k = Math.min(w / t.getWidth(), h / t.getHeight());
        float iw = t.getWidth() * k, ih = t.getHeight() * k;
        img(name, x + (w - iw) / 2, y + (h - ih) / 2, iw, ih, Color.WHITE, a);
    }

    /** Ширина картинки при заданной высоте. */
    public float wAt(String name, float h) { Texture t = t(name); return h * t.getWidth() / t.getHeight(); }

    /** Растягиваемая плашка: края масштабируются по высоте, середина тянется. */
    public void nine(String name, float x, float y, float w, float h, Color tint, float a) {
        if (d.off) return;
        Texture t = t(name);
        int[] n = NS.get(name);
        if (n == null) { img(name, x, y, w, h, tint, a); return; }
        float TW = t.getWidth(), TH = t.getHeight();
        float k = h / TH;
        float L = n[0] * k, T = n[1] * k, R = n[2] * k, B = n[3] * k;
        if (L + R > w) { float f = w / (L + R); L *= f; R *= f; }
        float[] xs = {x, x + L, x + w - R, x + w};
        float[] ys = {y, y + T, y + h - B, y + h};
        float[] us = {0, n[0] / TW, 1 - n[2] / TW, 1};
        float[] vs = {0, n[1] / TH, 1 - n[3] / TH, 1};
        d.b.setColor(tint.r, tint.g, tint.b, a);
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++) {
                float ww = xs[i + 1] - xs[i], hh = ys[j + 1] - ys[j];
                if (ww <= 0 || hh <= 0) continue;
                d.b.draw(t, xs[i], ys[j], ww, hh, us[i], vs[j], us[i + 1], vs[j + 1]);
            }
        d.b.setColor(Color.WHITE);
    }

    @Override public void dispose() {
        for (Texture t : tx.values()) if (t != d.white) t.dispose();
        tx.clear();
    }
}
