package ru.shitneba;

import java.util.HashMap;
import java.util.Map;

import ru.shitneba.gfx.Node3;

/** Объекты миссий: радар, плотина ГЭС, подстанция, грузовики колонны (модели — в Targets и SModels). */
public final class Objs {
    public static final class Obj { public Node3 g, spin; }

    /** Шаблоны моделей: строятся один раз, дальше копируются (меши общие). */
    static final Map<String, Node3> tplCache = new HashMap<>();

    public static Obj make(String kind) {
        Obj o = new Obj();
        if (kind.equals("radar")) {
            SModels.Target t = SModels.target("radar", "dusk");
            o.g = t.g; o.spin = t.spin;
            return o;
        }
        // плотина, подстанция; всё остальное — грузовик колонны (три вида по очереди)
        if (!kind.equals("dam") && !kind.equals("sub")) kind = "truck";
        String key = kind.equals("truck") ? "truck" + (Targets.truckVar % 3) : kind;
        Node3 tpl = tplCache.get(key);
        if (tpl == null) {
            tpl = kind.equals("dam") ? Targets.dam() : kind.equals("sub") ? Targets.substation() : Targets.convoyTruck();
            tplCache.put(key, tpl);
        } else if (kind.equals("truck")) Targets.truckVar++;
        o.g = new Node3();
        o.g.add(tpl.cloneTree());
        return o;
    }

    private Objs() { }
}
