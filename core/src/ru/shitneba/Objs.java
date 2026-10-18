package ru.shitneba;

import com.badlogic.gdx.graphics.Color;
import java.util.HashMap;
import java.util.Map;
import ru.shitneba.gfx.MB;
import ru.shitneba.gfx.Mesh3;
import ru.shitneba.gfx.Node3;

public final class Objs {
   static final Map<String, Mesh3[]> cache = new HashMap<>();

   static Color c(String h) {
      return G.hex(h);
   }

   public static Objs.Obj make(String kind) {
      Mesh3[] ms = cache.get(kind);
      if (ms == null) {
         ms = new Mesh3[2];
         MB b = new MB();
         float PI = (float) Math.PI;
         byte var5 = -1;
         switch (kind.hashCode()) {
            case 99216:
               if (kind.equals("dam")) {
                  var5 = 1;
               }
               break;
            case 114240:
               if (kind.equals("sub")) {
                  var5 = 2;
               }
               break;
            case 108270342:
               if (kind.equals("radar")) {
                  var5 = 0;
               }
         }

         label129: {
            float[] i;
            int x;
            int k;
            switch (var5) {
               case 0:
                  b.box(3.0F, 1.2F, 7.0F, c("4d5a3c"), 0.0F, 1.4F, 0.0F);

                  for (float z : new float[]{-2.5F, 2.5F}) {
                     for (float s : new float[]{-1.0F, 1.0F}) {
                        b.cyl(0.55F, 0.55F, 0.4F, 12, Models.TIRE, s * 1.3F, 0.55F, z, 0.0F, 0.0F, PI / 2.0F);
                     }
                  }

                  b.box(1.6F, 3.5F, 1.6F, c("5a6646"), 0.0F, 3.8F, 0.0F);
                  b.box(2.6F, 0.5F, 2.6F, c("3c4630"), 0.0F, 5.6F, 0.0F);
                  MB a = new MB();
                  a.box(9.0F, 2.6F, 0.3F, c("6c7656"));

                  for (int ix = 0; ix < 9; ix++) {
                     a.box(0.08F, 2.6F, 0.5F, c("3a3f33"), -4 + ix, 0.0F, -0.2F);
                  }

                  ms[1] = a.mesh();
                  break label129;
               case 1:
                  b.extrude(new float[][]{{-6.0F, 0.0F}, {6.0F, 0.0F}, {3.0F, 28.0F}, {-3.0F, 28.0F}}, 220.0F, c("8d8a82"), 0.0F, -4.0F, 0.0F);

                  for (int ix = 0; ix < 6; ix++) {
                     b.box(4.0F, 22.0F, 1.0F, c("6e6c66"), -50 + ix * 20, 8.0F, 5.4F);
                  }

                  for (int ix = 0; ix < 3; ix++) {
                     b.box(8.0F, 18.0F, 0.6F, c("bfd8e8"), -20 + ix * 20, 5.0F, 6.2F);
                  }

                  b.box(220.0F, 1.2F, 7.0F, c("5b5953"), 0.0F, 24.6F, 0.0F);
                  break label129;
               case 2:
                  for (int ix = 0; ix < 3; ix++) {
                     float xx = -8.0F + ix * 8;
                     b.box(4.0F, 4.0F, 3.0F, c("6e7570"), xx, 2.0F, 0.0F);

                     for (int kx = 0; kx < 3; kx++) {
                        b.cyl(0.25F, 0.25F, 2.0F, 8, c("c8b88a"), xx - 1.2F + kx * 1.2F, 5.0F, 0.0F);
                     }
                  }

                  for (float xx : new float[]{-16.0F, 16.0F}) {
                     b.box(0.5F, 18.0F, 0.5F, c("777777"), xx, 9.0F, 0.0F);
                     b.box(8.0F, 0.4F, 0.4F, c("777777"), xx, 16.0F, 0.0F);
                  }

                  b.box(30.0F, 0.3F, 14.0F, c("8a8a80"), 0.0F, 0.15F, 0.0F);
                  break label129;
               default:
                  b.box(2.4F, 0.4F, 8.0F, Models.DARK, 0.0F, 1.0F, 0.0F);
                  i = new float[]{-2.8F, 1.4F, 2.8F};
                  x = i.length;
                  k = 0;
            }

            while (k < x) {
               float z = i[k];

               for (float s : new float[]{-1.0F, 1.0F}) {
                  b.cyl(0.6F, 0.6F, 0.45F, 14, Models.TIRE, s * 1.2F, 0.6F, z, 0.0F, 0.0F, PI / 2.0F);
               }

               k++;
            }

            b.extrude(new float[][]{{-1.0F, 0.0F}, {1.1F, 0.0F}, {1.2F, 0.9F}, {0.9F, 1.7F}, {-1.0F, 1.7F}}, 2.5F, c("56634a"), 0.0F, 1.2F, -2.9F);
            b.box(2.3F, 0.6F, 0.05F, Models.GLASS, 0.0F, 2.5F, -3.95F);
            b.box(2.6F, 2.4F, 5.2F, c("6b7752"), 0.0F, 2.5F, 1.4F);
         }

         ms[0] = b.mesh();
         cache.put(kind, ms);
      }

      Objs.Obj o = new Objs.Obj();
      o.g = new Node3();
      Node3 body = new Node3(ms[0], Models.MAT_HULL);
      body.castShadow = true;
      o.g.add(body);
      if (ms[1] != null) {
         o.spin = new Node3(ms[1], Models.MAT_HULL);
         o.spin.castShadow = true;
         o.spin.pos.set(0.0F, 7.2F, 0.0F);
         o.g.add(o.spin);
      }

      return o;
   }

   private Objs() {
   }

   public static final class Obj {
      public Node3 g;
      public Node3 spin;
   }
}
