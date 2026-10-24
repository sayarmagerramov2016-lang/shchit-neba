"""Операторы «Щит неба 3D» в Blender (bpy): гладкие чиби-модели со скелетом.

Запуск: python3 store/tools/blender_ops.py <папка-вывода> [id,id,...]
Нужен модуль bpy (pip install bpy). Лица — картинки assets/ops3d/<id>_face_<эмоция>.png
(рисует store/tools/make_ops_soft.py). На выходе для каждой девчонки:
  <id>.glb       — модель со скелетом (кости root, spine, head, arm.R, arm.L)
  <id>_front.png, <id>_side.png, <id>_back.png — рендеры Cycles с аниме-контуром (Freestyle)
Оси Blender: Z вверх, вперёд −Y. Рост ~1.3.
Пока эскиз — в игру не подключено.
"""
import math
import os
import sys

import bpy  # noqa: E402 — bpy первым, он регистрирует bmesh и mathutils
import bmesh
from mathutils import Euler, Matrix, Vector

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
FACES = os.path.join(ROOT, "assets", "ops3d")

# hair, style, extras, [top, acc, bot, legs, shoes, kind] ×2 — как в make_ops_soft.py
GIRLS = {
    "liza": ("8a5432", "pony", "wrench,cap", ["7a8a52", "f08a2a", "5d6b40", "4a4038", "5a3c26", "overall"], ["24356a", "e8c25a", "1c2a56", "2a2a3e", "1e1e2a", "overall"]),
    "katya": ("f4d27a", "long", "cup", ["efdcb4", "c8603e", "5a8a54", "f8f2ea", "8a5232", "sweater"], ["f8c2d2", "ffffff", "d06a88", "ffffff", "d06a88", "sweater"]),
    "sonya": ("525866", "bob", "scarf", ["687a4a", "9aaa66", "55613e", "424a30", "33332c", "camo"], ["e6ecf0", "a6b4c0", "cad4dc", "b4c0ca", "64707a", "camo"]),
    "anya": ("9cd8f4", "twin", "phones", ["2a3c6c", "59e0ff", "22325a", "24243a", "16162a", "uniform"], ["f4f8fc", "2fb8e8", "64b0e0", "eef4fa", "3478b0", "uniform"]),
    "masha": ("f49446", "short", "cat,bell", ["e04a42", "ffffff", "f8f4ee", "ffffff", "a83028", "sailor"], ["2e2a34", "e8c25a", "423c48", "2e2a34", "e8c25a", "sailor"]),
    "rin": ("ff5ac0", "side", "visor", ["24202e", "ff4fd8", "2e2a3a", "1a1822", "ff4fd8", "cyber"], ["18222c", "3ff0ff", "1e2a38", "121a22", "3ff0ff", "cyber"]),
    "yuki": ("f2f7ff", "long", "flake", ["c6e6ff", "ffffff", "96ccf6", "eef8ff", "5e9edc", "dress"], ["2c4a86", "c6e6ff", "223a70", "dceafa", "c6e6ff", "dress"]),
    "elka": ("44a856", "buns", "garland,starpin", ["36964a", "ffd23a", "2a7c40", "ea4444", "922626", "dress"], ["e04444", "ffffff", "c23232", "ffffff", "36964a", "dress"]),
    "akira": ("2c2834", "highpony", "katana", ["f8f4ee", "d0303f", "b82634", "2e2a34", "32262e", "hakama"], ["342a3c", "a466ea", "221c2c", "1a1620", "a466ea", "hakama"]),
    "sneg": ("eaf0fa", "braid", "kokoshnik", ["62a8ee", "ffffff", "5294de", "ffffff", "ffffff", "coat"], ["ccd4e0", "ffbcd4", "b0bacb", "ffffff", "ffbcd4", "coat"]),
    "valk": ("ffdc72", "long", "wings,halo,tiara", ["f8f4ea", "eabc36", "efe8d6", "f8f4ea", "eabc36", "armor"], ["302840", "a676ff", "241e30", "302840", "a676ff", "armor"]),
}
SKIN = "ffe6d8"
LOW = False  # облегчённые модели для игры (меньше граней)
HR, HZ = 0.30, 0.95  # радиус и высота центра головы


# ---------------------------------------------------------------- материалы
def lin(h):
    h = h.lstrip("#")
    c = [int(h[i:i + 2], 16) / 255 for i in (0, 2, 4)]
    return [x / 12.92 if x <= 0.04045 else ((x + 0.055) / 1.055) ** 2.4 for x in c] + [1]


MATS = {}


def mat(h, rough=0.55, sheen=0.0, emit=0.0, metal=0.0):
    k = (h, rough, emit, metal)
    if k in MATS:
        return MATS[k]
    m = bpy.data.materials.new("m_" + h)
    m.use_nodes = True
    b = m.node_tree.nodes["Principled BSDF"]
    b.inputs["Base Color"].default_value = lin(h)
    b.inputs["Roughness"].default_value = rough
    b.inputs["Metallic"].default_value = metal
    if "Coat Weight" in b.inputs:
        b.inputs["Coat Weight"].default_value = 0.0
    if emit > 0:
        b.inputs["Emission Color"].default_value = lin(h)
        b.inputs["Emission Strength"].default_value = emit
    MATS[k] = m
    return m


def dk(h, k):
    c = [int(h[i:i + 2], 16) for i in (0, 2, 4)]
    return "%02x%02x%02x" % tuple(int(v * (1 - k)) for v in c)


def lt(h, k):
    c = [int(h[i:i + 2], 16) for i in (0, 2, 4)]
    return "%02x%02x%02x" % tuple(int(v + (255 - v) * k) for v in c)


# ---------------------------------------------------------------- примитивы
PARTS = []  # (объект, кость)


def finish(o, m, bone, smooth=True, sub=1):
    o.data.materials.append(m)
    if smooth:
        for p in o.data.polygons:
            p.use_smooth = True
    if sub:
        md = o.modifiers.new("sub", "SUBSURF")
        md.levels = sub
        md.render_levels = sub
    PARTS.append((o, bone))
    return o


def sph(loc, scl, m, bone="spine", seg=24, sub=1, rot=(0, 0, 0)):
    if LOW:
        seg = max(8, seg // 2) if max(scl) > 0.05 else 8
        sub = sub if max(scl) > 0.05 else 0
    bpy.ops.mesh.primitive_uv_sphere_add(segments=seg, ring_count=seg // 2, radius=1, location=loc, rotation=rot)
    o = bpy.context.object
    o.scale = scl
    return finish(o, m, bone, sub=sub)


def cone(loc, r1, r2, depth, m, bone="spine", rot=(0, 0, 0), scl=(1, 1, 1), vert=24, sub=1, cap=True):
    if LOW:
        vert = max(3, min(vert, 12))
    bpy.ops.mesh.primitive_cone_add(vertices=vert, radius1=r1, radius2=r2, depth=depth, location=loc, rotation=rot,
                                    end_fill_type="NGON" if cap else "NOTHING")
    o = bpy.context.object
    o.scale = scl
    return finish(o, m, bone, sub=sub)


def tor(loc, R, r, m, bone="spine", rot=(0, 0, 0)):
    bpy.ops.mesh.primitive_torus_add(major_radius=R, minor_radius=r, major_segments=20 if LOW else 40, minor_segments=5 if LOW else 10, location=loc, rotation=rot)
    o = bpy.context.object
    return finish(o, m, bone, sub=0)


def tube(pts, r, m, bone="spine", r_end=None):
    """Трубка по точкам (руки, ноги, хвосты): кривая с объёмом, радиус сужается к концу."""
    cu = bpy.data.curves.new("tube", "CURVE")
    cu.dimensions = "3D"
    cu.bevel_depth = r
    cu.bevel_resolution = 1 if LOW else 6
    cu.resolution_u = 4 if LOW else 12
    cu.use_fill_caps = True
    sp = cu.splines.new("NURBS")
    sp.points.add(len(pts) - 1)
    for i, p in enumerate(pts):
        k = i / max(1, len(pts) - 1)
        sp.points[i].co = (p[0], p[1], p[2], 1)
        if r_end is not None:
            sp.points[i].radius = 1 + (r_end / r - 1) * k
    sp.use_endpoint_u = True
    sp.order_u = min(4, len(pts))
    o = bpy.data.objects.new("tube", cu)
    bpy.context.collection.objects.link(o)
    bpy.context.view_layer.objects.active = o
    o.select_set(True)
    bpy.ops.object.convert(target="MESH")
    o = bpy.context.object
    return finish(o, m, bone, sub=0)


def strand(root, az, el, length, width, tilt, m, bend=0.6, flat=0.45, bone="head"):
    """Прядь: плоский конус от корня на голове вниз, с изгибом. az — вокруг головы (0 — спереди), el — высота корня."""
    bpy.ops.mesh.primitive_cone_add(vertices=6 if LOW else 10, radius1=0.004, radius2=width, depth=length, location=(0, 0, 0))
    o = bpy.context.object
    bm = bmesh.new()
    bm.from_mesh(o.data)
    for v in bm.verts:
        v.co.z -= length / 2  # корень в начале координат, кончик вниз
        v.co.y *= flat
        # изгиб от головы наружу
        t = -v.co.z / length
        v.co.y -= bend * length * t * t * 0.35
    bm.to_mesh(o.data)
    bm.free()
    o.rotation_euler = Euler((-tilt, 0, az), "XYZ")
    o.location = root
    return finish(o, m, bone, sub=1)


# ---------------------------------------------------------------- голова
def head_point(az, el, r=1.0):
    return Vector((math.sin(az) * math.cos(el) * HR * 1.04 * r, -math.cos(az) * math.cos(el) * HR * r, HZ + math.sin(el) * HR * 0.97 * r))


def face_decal(gid, emo="norm"):
    """Кусок сферы спереди головы с картинкой лица (UV — прямая проекция, как в игре)."""
    x0, x1 = (48 - 128) / 92, (208 - 128) / 92
    z0, z1 = -(142 - 156) / 90, -(250 - 156) / 90
    n = 12 if LOW else 24
    bm = bmesh.new()
    uv = bm.loops.layers.uv.new("UVMap")
    grid = []
    for j in range(n + 1):
        row = []
        for i in range(n + 1):
            u, w = i / n, j / n
            x, z = x0 + (x1 - x0) * u, z0 + (z1 - z0) * w
            q = 1 - x * x - z * z
            y = -math.sqrt(q) if q > 0.002 else -0.04
            r = HR * 1.008
            row.append((bm.verts.new((x * r * 1.04, y * r, HZ + z * r * 0.97)), u, 1 - w))
        grid.append(row)
    for j in range(n):
        for i in range(n):
            a, b, c, d = grid[j][i], grid[j][i + 1], grid[j + 1][i + 1], grid[j + 1][i]
            f = bm.faces.new((a[0], d[0], c[0], b[0]))
            for loop, src in zip(f.loops, (a, d, c, b)):
                loop[uv].uv = (src[1], src[2])
    me = bpy.data.meshes.new("face")
    bm.to_mesh(me)
    bm.free()
    o = bpy.data.objects.new("face", me)
    noline().objects.link(o)
    for p in me.polygons:
        p.use_smooth = True
    m = bpy.data.materials.new("face_" + gid)
    m.use_nodes = True
    nt = m.node_tree
    bsdf = nt.nodes["Principled BSDF"]
    bsdf.inputs["Roughness"].default_value = 0.5
    img = nt.nodes.new("ShaderNodeTexImage")
    img.image = bpy.data.images.load(os.path.join(FACES, f"{gid}_face_{emo}.png"))
    img.extension = "CLIP"
    nt.links.new(img.outputs["Color"], bsdf.inputs["Base Color"])
    nt.links.new(img.outputs["Alpha"], bsdf.inputs["Alpha"])
    m.blend_method = "BLEND" if hasattr(m, "blend_method") else None
    me.materials.append(m)
    PARTS.append((o, "head"))
    return o


def noline():
    c = bpy.data.collections.get("noline")
    if c is None:
        c = bpy.data.collections.new("noline")
        bpy.context.scene.collection.children.link(c)
    return c


def hair_cap(h):
    bpy.ops.mesh.primitive_uv_sphere_add(segments=24 if LOW else 40, ring_count=12 if LOW else 20, radius=1, location=(0, 0, HZ + 0.012))
    o = bpy.context.object
    bm = bmesh.new()
    bm.from_mesh(o.data)
    kill = [v for v in bm.verts if v.co.y < -0.22 and v.co.z < 0.3]
    bmesh.ops.delete(bm, geom=kill, context="VERTS")
    bm.to_mesh(o.data)
    bm.free()
    o.scale = (HR * 1.1, HR * 1.08, HR * 1.06)
    sol = o.modifiers.new("sol", "SOLIDIFY")
    sol.thickness = 0.06
    return finish(o, mat(h, 0.42), "head", sub=1 if LOW else 2)


def build_girl(gid, outfit=0):
    hair_h, style, ex, o0, o1 = GIRLS[gid]
    ex = ex.split(",")
    top, acc, bot, legs, shoes, kind = (o0 if outfit == 0 else o1)
    skin = mat(SKIN, 0.6)
    mt, ma, mb, ml, ms = mat(top), mat(acc, 0.4), mat(bot), mat(legs), mat(shoes, 0.35)
    mh, mhd = mat(hair_h, 0.42), mat(dk(hair_h, 0.12), 0.45)

    # ---- ноги и обувь
    for s in (-1, 1):
        sph((s * 0.068, -0.025, 0.045), (0.062, 0.09, 0.048), ms, "root")
        tube([(s * 0.068, 0, 0.07), (s * 0.068, 0, 0.2), (s * 0.068, 0, 0.33)], 0.044, ml, "root")
    # ---- низ
    if kind in ("overall", "uniform", "camo", "cyber"):
        cone((0, 0, 0.36), 0.15, 0.13, 0.14, mb)
    elif kind == "hakama":
        cone((0, 0, 0.27), 0.23, 0.13, 0.3, mb)
    elif kind == "coat":
        cone((0, 0, 0.29), 0.22, 0.14, 0.3, mt)
        tor((0, 0, 0.145), 0.21, 0.028, mat("ffffff", 0.9))
    else:
        cone((0, 0, 0.35), 0.24, 0.13, 0.18, mb)
    # ---- торс
    sph((0, 0, 0.5), (0.15, 0.13, 0.165), mt)
    if kind == "overall":
        sph((0, -0.004, 0.47), (0.138, 0.112, 0.125), mb)
    elif kind == "sailor":
        cone((0, 0, 0.62), 0.17, 0.12, 0.04, ma)
        sph((0, -0.125, 0.575), (0.04, 0.02, 0.025), mat("ea4242", 0.4))
    elif kind == "uniform":
        cone((0, -0.128, 0.53), 0.016, 0.01, 0.11, ma, rot=(0, 0, 0), scl=(1, 0.4, 1))
    elif kind == "cyber":
        tor((0, 0, 0.46), 0.145, 0.008, mat(acc, 0.3, emit=3))
        tor((0, 0, 0.56), 0.13, 0.008, mat(acc, 0.3, emit=3))
    elif kind == "hakama":
        cone((0, 0, 0.42), 0.158, 0.158, 0.04, ma)
        cone((0, -0.118, 0.57), 0.06, 0.005, 0.12, ma, rot=(0.25, 0, 0), scl=(1, 0.25, 1), vert=3)
    elif kind == "armor":
        sph((0, -0.03, 0.53), (0.125, 0.1, 0.11), ma)
        for s in (-1, 1):
            sph((s * 0.15, 0, 0.6), (0.06, 0.06, 0.045), ma)
    elif kind == "dress":
        cone((0, 0, 0.42), 0.155, 0.155, 0.03, ma)
    elif kind == "sweater":
        cone((0, 0, 0.58), 0.155, 0.15, 0.025, ma)
    elif kind == "coat":
        tor((0, 0, 0.63), 0.12, 0.032, mat("ffffff", 0.9))
        for z in (0.36, 0.45, 0.53):
            sph((0, -0.15 if z > 0.4 else -0.17, z), (0.016, 0.01, 0.016), mat("e8c25a" if acc == "ffffff" else acc, 0.3))
    if "scarf" in ex:
        tor((0, 0, 0.645), 0.075, 0.032, mat("5e7e9e" if top == "e6ecf0" else "b84e3c"))
    if "bell" in ex:
        sph((0, -0.13, 0.615), (0.022, 0.022, 0.022), mat("eac85a", 0.25, metal=0.8))
    if "garland" in ex:
        cs = ["ff4040", "ffd23a", "40a0ff", "ff60e0"]
        for i in range(7):
            a = -1 + 2 * i / 6
            sph((math.sin(a) * 0.142, -math.cos(a) * 0.122, 0.545 - (1 - a * a) * 0.04), (0.015,) * 3, mat(cs[i % 4], 0.3, emit=2))
    tube([(0, 0, 0.6), (0, 0, 0.7)], 0.04, skin)
    if "katana" in ex:
        tube([(-0.2, 0.17, 0.25), (0.22, 0.17, 0.85)], 0.016, mat("3a2a3a"))
        tor((0.11, 0.17, 0.69), 0.035, 0.012, mat("e8c25a", 0.3, metal=0.7), rot=(0, 0.6, 0))
    if "wings" in ex:
        wc = "ffffff" if outfit == 0 else "433460"
        for s in (-1, 1):
            for i in range(4):
                a = 0.5 + i * 0.28
                sph((s * (0.14 + math.cos(a) * 0.17), 0.15, 0.6 + math.sin(a) * 0.17), (0.07, 0.03, 0.21 - i * 0.025),
                    mat(lt(wc, 0.05 * i), 0.5), rot=(0, s * (0.6 - i * 0.35), 0))

    # ---- руки: правая показывает вверх-вперёд, левая согнута к груди
    sl = ma if kind == "armor" else mt
    sh = Vector((-0.15, 0, 0.6))
    # рука вытянута вбок-вперёд и чуть вверх, палец смотрит вперёд на цель (а не вверх у лица)
    elbow = sh + Vector((-0.11, -0.07, 0.03))
    hand = elbow + Vector((-0.09, -0.12, 0.05))
    tube([tuple(sh), tuple(elbow), tuple(hand)], 0.04, sl, "arm.R", r_end=0.034)
    fist = hand + Vector((-0.02, -0.03, 0.005))
    sph(tuple(fist), (0.042, 0.045, 0.038), skin, "arm.R")
    tube([tuple(fist + Vector((-0.012, -0.035, 0.008))), tuple(fist + Vector((-0.03, -0.085, 0.02)))], 0.012, skin, "arm.R")
    shl = Vector((0.15, 0, 0.6))
    tube([tuple(shl), (0.17, -0.04, 0.5), (0.06, -0.11, 0.47)], 0.04, sl, "arm.L", r_end=0.034)
    sph((0.04, -0.12, 0.47), (0.04, 0.038, 0.042), skin, "arm.L")
    if "cup" in ex:
        cone((0.03, -0.17, 0.48), 0.03, 0.034, 0.07, mat("ffffff", 0.3), "arm.L", sub=0)
    if "wrench" in ex:
        tube([(0.05, -0.13, 0.46), (0.08, -0.17, 0.3)], 0.012, mat("aab2ba", 0.3, metal=0.8), "arm.L")
        tor((0.085, -0.175, 0.28), 0.025, 0.009, mat("aab2ba", 0.3, metal=0.8), "arm.L", rot=(math.pi / 2, 0, 0))

    # ---- голова
    sph((0, 0, HZ), (HR * 1.04, HR, HR * 0.97), skin, "head", seg=40)
    for s in (-1, 1):  # щёчки и уши
        sph((s * HR * 1.02, 0.01, HZ - 0.03), (0.025, 0.035, 0.045), skin, "head")
    face_decal(gid)
    hair_cap(hair_h)
    # чёлка
    n = 9
    for i in range(n):
        az = -1.0 + 2.0 * i / (n - 1)
        long_ = 0.16 + (0.03 if i % 2 == 0 else 0) - abs(az) * 0.03
        if style == "side":
            long_ = 0.11 + (az + 1) * 0.06
        root = head_point(az, 0.82, 1.0)
        strand(tuple(root), az, 0.82, long_, 0.1, 0.42 + abs(az) * 0.15, mh if i % 3 else mat(lt(hair_h, 0.06), 0.42), bend=1.3, flat=0.32)
    # боковые пряди
    for s in (-1, 1):
        for k in range(2):
            az = s * (1.35 + k * 0.25)
            strand(tuple(head_point(az, 0.4, 1.03)), az, 0.4, 0.3 if style != "short" else 0.18, 0.07, 0.12, mh, bend=0.4)
    # затылок
    if style in ("long", "braid", "bob", "highpony", "buns", "short", "pony", "side", "twin"):
        cnt = 13
        ln = {"long": 0.62, "braid": 0.5, "bob": 0.33, "highpony": 0.4, "buns": 0.34, "short": 0.24, "pony": 0.3, "side": 0.3, "twin": 0.3}[style]
        for i in range(cnt):
            az = math.pi - 1.6 + 3.2 * i / (cnt - 1)
            strand(tuple(head_point(az, 0.55, 1.04)), az, 0.55, ln + (0.04 if i % 2 else 0), 0.1, 0.05, mhd if i % 2 else mh, bend=0.3)
    if style == "twin":
        for s in (-1, 1):
            tube([(s * 0.3, 0.06, HZ + 0.14), (s * 0.4, 0.08, HZ - 0.2), (s * 0.38, 0.1, HZ - 0.6)], 0.075, mh, "head", r_end=0.02)
            sph((s * 0.3, 0.05, HZ + 0.15), (0.04,) * 3, mat("ff6a8a", 0.4), "head")
    if style == "pony":
        tube([(0, 0.3, HZ + 0.05), (0.04, 0.42, HZ - 0.15), (0.05, 0.44, HZ - 0.48)], 0.08, mhd, "head", r_end=0.02)
    if style == "highpony":
        tube([(0, 0.24, HZ + 0.3), (0.03, 0.42, HZ + 0.1), (0.05, 0.44, HZ - 0.6)], 0.1, mhd, "head", r_end=0.02)
        sph((0, 0.22, HZ + 0.32), (0.05,) * 3, mat("d0303f", 0.4), "head")
    if style == "side":
        tube([(0.28, 0.12, HZ + 0.14), (0.38, 0.14, HZ - 0.15), (0.38, 0.15, HZ - 0.5)], 0.085, mh, "head", r_end=0.02)
        sph((0.28, 0.1, HZ + 0.15), (0.042,) * 3, mat("46f0ff", 0.3, emit=2), "head")
    if style == "buns":
        for s in (-1, 1):
            sph((s * 0.2, 0.06, HZ + 0.25), (0.11, 0.11, 0.11), mh, "head")
    if style == "braid":
        for i in range(7):
            sph((0.24 + i * 0.008, 0.12, HZ - 0.25 - i * 0.075), (0.052 - i * 0.003,) * 3, mh, "head")
        sph((0.3, 0.12, HZ - 0.8), (0.03, 0.03, 0.02), mat("62a8ee", 0.4), "head")
    # головные украшения
    if "cap" in ex:
        sph((0, 0.03, HZ + 0.16), (HR * 1.0, HR * 1.0, HR * 0.72), mt, "head")
        cone((0, -0.03, HZ + 0.15), HR * 1.08, HR * 1.04, 0.015, mat(dk(top, 0.15)), "head", scl=(1, 1.1, 1), sub=0)
        sph((0, -0.24, HZ + 0.28), (0.025,) * 3, ma, "head")
    if "phones" in ex:
        for i in range(10):
            a0 = math.pi * i / 10
            p0 = (math.cos(a0) * HR * 1.15, 0.02, HZ - 0.02 + math.sin(a0) * HR * 1.15)
            a1 = math.pi * (i + 1) / 10
            p1 = (math.cos(a1) * HR * 1.15, 0.02, HZ - 0.02 + math.sin(a1) * HR * 1.15)
            tube([p0, p1], 0.016, mat("2c2c3a", 0.4), "head")
        for s in (-1, 1):
            cone((s * HR * 1.12, 0, HZ - 0.03), 0.07, 0.07, 0.06, mat("2c2c3a", 0.4), "head", rot=(0, math.pi / 2, 0), sub=0)
            cone((s * HR * 1.14, 0, HZ - 0.03), 0.05, 0.05, 0.062, mat(acc, 0.3, emit=1.5), "head", rot=(0, math.pi / 2, 0), sub=0)
        tube([(-0.34, -0.02, HZ - 0.06), (-0.25, -0.18, HZ - 0.2), (-0.13, -0.27, HZ - 0.21)], 0.008, mat("2c2c3a"), "head")
        sph((-0.13, -0.275, HZ - 0.21), (0.02,) * 3, ma, "head")
    if "cat" in ex:
        for s in (-1, 1):
            cone((s * 0.17, 0.0, HZ + 0.3), 0.08, 0.004, 0.15, mh, "head", rot=(0, s * 0.35, 0), scl=(1, 0.5, 1), vert=4)
            cone((s * 0.168, -0.022, HZ + 0.29), 0.05, 0.003, 0.11, mat("ffc6cf"), "head", rot=(0, s * 0.35, 0), scl=(1, 0.25, 1), vert=4)
    if "visor" in ex:
        tor((0, 0, HZ + 0.11), HR * 1.13, 0.02, mat(acc, 0.3, emit=3), "head")
    if "flake" in ex:
        for i in range(3):
            cone((0.2, -0.2, HZ + 0.17), 0.007, 0.007, 0.1, mat("5ab0ff", 0.3, emit=1), "head", rot=(0, i * math.pi / 3, 0), sub=0)
    if "starpin" in ex:
        sph((0.2, -0.21, HZ + 0.18), (0.035, 0.018, 0.035), mat("ffd23a", 0.3, emit=1), "head")
    if "kokoshnik" in ex:
        sph((0, 0.06, HZ + 0.26), (HR * 1.05, HR * 0.3, HR * 1.15), mt, "head", rot=(0.2, 0, 0))
        sph((0, -0.16, HZ + 0.33), (0.03, 0.015, 0.03), ma, "head")
        for s in (-1, 1):
            sph((s * 0.11, -0.15, HZ + 0.28), (0.022, 0.012, 0.022), ma, "head")
    if "tiara" in ex:
        tor((0, 0.02, HZ + 0.22), HR * 0.95, 0.012, mat(acc, 0.3, metal=0.8), "head", rot=(-0.15, 0, 0))
        sph((0, -0.27, HZ + 0.27), (0.03, 0.018, 0.036), mat("ff6a8a", 0.2), "head")
    if "halo" in ex:
        tor((0, 0.04, HZ + 0.42), 0.16, 0.014, mat("fff2a0", 0.3, emit=6), "head", rot=(0.25, 0, 0))


# ---------------------------------------------------------------- скелет, сцена, рендер
def armature():
    arm = bpy.data.armatures.new("rig")
    ob = bpy.data.objects.new("rig", arm)
    bpy.context.collection.objects.link(ob)
    bpy.context.view_layer.objects.active = ob
    bpy.ops.object.mode_set(mode="EDIT")
    def bone(name, h, t, parent=None):
        b = arm.edit_bones.new(name)
        b.head, b.tail = h, t
        if parent:
            b.parent = arm.edit_bones[parent]
    bone("root", (0, 0, 0), (0, 0, 0.3))
    bone("spine", (0, 0, 0.3), (0, 0, 0.68), "root")
    bone("head", (0, 0, 0.68), (0, 0, 1.25), "spine")
    bone("arm.R", (-0.15, 0, 0.6), (-0.37, -0.22, 0.69), "spine")
    bone("arm.L", (0.15, 0, 0.6), (0.06, -0.11, 0.47), "spine")
    bpy.ops.object.mode_set(mode="OBJECT")
    for o, b in PARTS:
        mw = o.matrix_world.copy()
        o.parent = ob
        o.parent_type = "BONE"
        o.parent_bone = b
        o.matrix_world = mw
    return ob


def scene_setup(res=720):
    sc = bpy.context.scene
    sc.render.engine = "CYCLES"
    sc.cycles.device = "CPU"
    sc.cycles.samples = 48
    try:
        sc.cycles.use_denoising = True
        sc.cycles.denoiser = "OPENIMAGEDENOISE"
    except Exception:
        pass
    sc.render.resolution_x = res
    sc.render.resolution_y = int(res * 1.15)
    sc.render.film_transparent = False
    sc.view_settings.view_transform = "AgX" if "AgX" in [i.identifier for i in sc.view_settings.bl_rna.properties["view_transform"].enum_items] else "Filmic"
    sc.view_settings.look = "None"
    # аниме-контур
    sc.render.use_freestyle = True
    sc.render.line_thickness_mode = "ABSOLUTE"
    sc.render.line_thickness = 1.1
    ls = sc.view_layers[0].freestyle_settings.linesets
    lset = ls[0] if len(ls) else ls.new("outline")
    if lset.linestyle is None:
        lset.linestyle = bpy.data.linestyles.new("ink")
    lset.linestyle.color = (0.18, 0.1, 0.14)
    lset.select_by_visibility = True
    lset.select_by_collection = True
    lset.collection = noline()
    lset.collection_negation = "EXCLUSIVE"
    lset.select_crease = False
    lset.select_border = False
    # мир и пол
    w = bpy.data.worlds.new("w")
    sc.world = w
    w.use_nodes = True
    w.node_tree.nodes["Background"].inputs["Color"].default_value = (0.12, 0.14, 0.2, 1)
    w.node_tree.nodes["Background"].inputs["Strength"].default_value = 0.8
    bpy.ops.mesh.primitive_plane_add(size=60, location=(0, 0, 0))
    fl = bpy.context.object
    fl.data.materials.append(mat("39404a", 0.9))
    for c in fl.users_collection:
        c.objects.unlink(fl)
    noline().objects.link(fl)
    # свет: тёплый ключевой, холодный контровой
    def light(kind, loc, rot, energy, col, size=1.0):
        ld = bpy.data.lights.new(kind, kind)
        ld.energy = energy
        ld.color = col
        if kind == "AREA":
            ld.size = size
        o = bpy.data.objects.new(kind, ld)
        o.location = loc
        o.rotation_euler = rot
        bpy.context.collection.objects.link(o)
    light("SUN", (0, 0, 5), (math.radians(50), 0, math.radians(-35)), 4.5, (1, 0.93, 0.85))
    light("AREA", (1.2, 1.4, 1.6), (math.radians(-60), 0, math.radians(140)), 160, (0.55, 0.75, 1), 1.5)
    light("AREA", (-1.5, -1.8, 1.0), (math.radians(70), 0, math.radians(-40)), 70, (1, 0.95, 0.95), 2)
    cam = bpy.data.objects.new("cam", bpy.data.cameras.new("cam"))
    cam.data.lens = 70
    bpy.context.collection.objects.link(cam)
    sc.camera = cam
    return cam


def aim(cam, yaw, el=0.1, dist=2.9, tz=0.66):
    t = Vector((0, 0, tz))
    d = Vector((math.sin(yaw) * math.cos(el), -math.cos(yaw) * math.cos(el), math.sin(el)))
    cam.location = t + d * dist
    cam.rotation_euler = (t - cam.location).to_track_quat("-Z", "Y").to_euler()


def clear():
    bpy.ops.wm.read_factory_settings(use_empty=True)
    PARTS.clear()
    MATS.clear()


# ---------------------------------------------------------------- экспорт в игру
BONE_HEAD = {"root": (0, 0, 0), "spine": (0, 0, 0.3), "head": (0, 0, 0.68), "arm.R": (-0.15, 0, 0.6), "arm.L": (0.15, 0, 0.6)}


def g3(v):
    """Оси Blender (Z вверх, вперёд −Y) → оси игры (Y вверх, вперёд −Z), поворот без зеркала."""
    return (-v[0], v[2], v[1])


def srgb8(c):
    out = []
    for x in c[:3]:
        x = max(0.0, min(1.0, x))
        x = x * 12.92 if x <= 0.0031308 else 1.055 * x ** (1 / 2.4) - 0.055
        out.append(int(round(x * 255)))
    return out


def export_game(path):
    """Формат .ops (gzip, big-endian): «OPS1», число групп; группа: кость, вид (0 обычная, 1 светится, 2 лицо),
    число вершин и индексов; вершина: позиция int16×3 (/8192, относительно начала кости), нормаль int8×3,
    цвет sRGB u8×3, UV u16×2 (только у лица); индексы u16 (или u32, если вершин больше 65535)."""
    import gzip
    import struct
    groups = {}
    dg = bpy.context.evaluated_depsgraph_get()
    for o, bone in PARTS:
        oe = o.evaluated_get(dg)
        me = oe.to_mesh()
        me.calc_loop_triangles()
        mw = o.matrix_world
        nm = mw.to_3x3().inverted().transposed()
        kind = 2 if o.name.startswith("face") else 0
        mats = [s.material for s in o.material_slots]
        uvl = me.uv_layers.active.data if (kind == 2 and me.uv_layers.active) else None
        hx, hy, hz = BONE_HEAD[bone]
        for tri in me.loop_triangles:
            m = mats[tri.material_index] if mats else None
            col, k = (1, 1, 1, 1), kind
            if m is not None and kind != 2:
                b = m.node_tree.nodes.get("Principled BSDF")
                col = tuple(b.inputs["Base Color"].default_value)
                if b.inputs["Emission Strength"].default_value > 0:
                    k = 1
            g = groups.setdefault((bone, k), {"v": [], "map": {}, "i": []})
            cs = srgb8(col)
            for li in tri.loops:
                lp = me.loops[li]
                p = mw @ me.vertices[lp.vertex_index].co
                n = (nm @ (lp.normal if tri.use_smooth else tri.normal)).normalized()
                p = g3((p.x - hx, p.y - hy, p.z - hz))
                n = g3(n)
                uv = (0, 0)
                if uvl is not None:
                    u = uvl[li].uv
                    uv = (int(max(0, min(1, u.x)) * 65535), int(max(0, min(1, 1 - u.y)) * 65535))
                key = (round(p[0] * 8192), round(p[1] * 8192), round(p[2] * 8192),
                       round(n[0] * 127), round(n[1] * 127), round(n[2] * 127), cs[0], cs[1], cs[2], uv[0], uv[1])
                idx = g["map"].get(key)
                if idx is None:
                    idx = len(g["v"])
                    g["map"][key] = idx
                    g["v"].append(key)
                g["i"].append(idx)
        oe.to_mesh_clear()
    buf = bytearray(b"OPS1")
    buf += struct.pack(">H", len(groups))
    for (bone, k), g in groups.items():
        bn = bone.encode()
        buf += struct.pack(">B", len(bn)) + bn
        nv, ni = len(g["v"]), len(g["i"])
        buf += struct.pack(">BII", k, nv, ni)
        for v in g["v"]:
            buf += struct.pack(">hhhbbbBBB", *[max(-32767, min(32767, x)) for x in v[:3]], *v[3:9])
            if k == 2:
                buf += struct.pack(">HH", v[9], v[10])
        fmt = ">%d%s" % (ni, "H" if nv <= 65535 else "I")
        buf += struct.pack(fmt, *g["i"])
    with gzip.open(path, "wb", compresslevel=9) as f:
        f.write(bytes(buf))
    return sum(len(g["i"]) for g in groups.values()) // 3


def portrait(cam, path):
    sc = bpy.context.scene
    sc.render.resolution_x = 256
    sc.render.resolution_y = 256
    sc.render.film_transparent = True
    sc.render.line_thickness = 1.4
    for o in bpy.data.objects:
        if o.type == "MESH" and o.name.startswith("Plane"):
            o.hide_render = True
    cam.data.lens = 85
    aim(cam, -0.28, el=0.08, dist=2.3, tz=HZ - 0.06)
    sc.render.filepath = path
    bpy.ops.render.render(write_still=True)


def game_assets(out, ids):
    """Модели для игры (.ops) и портреты 200×200 для меню — оба наряда."""
    global LOW
    from PIL import Image
    os.makedirs(out, exist_ok=True)
    for gid in ids:
        for of in (0, 1):
            clear()
            LOW = True
            build_girl(gid, of)
            tris = export_game(os.path.join(out, f"{gid}_{of}.ops"))
            clear()
            LOW = False
            build_girl(gid, of)  # портрет — с полной детализацией
            cam = scene_setup(256)
            bpy.context.scene.cycles.samples = 32
            tmp = os.path.join(out, f"_por_{gid}_{of}.png")
            portrait(cam, tmp)
            Image.open(tmp).resize((200, 200), Image.LANCZOS).save(os.path.join(ROOT, "assets", "ops", f"{gid}_{of}_por.png"), optimize=True)
            os.remove(tmp)
            print("game", gid, of, tris, flush=True)


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else "/tmp/blender_ops"
    ids = sys.argv[2].split(",") if len(sys.argv) > 2 else list(GIRLS)
    if os.environ.get("GAME"):
        game_assets(out, ids)
        return
    outfit = int(os.environ.get("OUTFIT", "0"))
    res = int(os.environ.get("RES", "720"))
    os.makedirs(out, exist_ok=True)
    for gid in ids:
        clear()
        build_girl(gid, outfit)
        armature()
        bpy.ops.export_scene.gltf(filepath=os.path.join(out, f"{gid}.glb"), export_format="GLB", use_selection=False)
        cam = scene_setup(res)
        sc = bpy.context.scene
        for name, yaw in (("front", -0.35), ("side", -1.2), ("back", math.pi - 0.5)):
            if os.environ.get("ONLYFRONT") and name != "front":
                continue
            aim(cam, yaw)
            sc.render.filepath = os.path.join(out, f"{gid}_{name}.png")
            bpy.ops.render.render(write_still=True)
        print("done", gid, flush=True)


if __name__ == "__main__":
    main()
