#!/usr/bin/env python3
"""离线方块模型预览器：不启动游戏就能看自定义模型 + 贴图长什么样。

按 Minecraft 的真实规则实现，不是近似：
- 面的四个顶点顺序来自 FaceInfo，UV 取值来自 BlockFaceUV（顶点 0..3 依次
  对应 uv 矩形的 左上/左下/右下/右上），`rotation` 也照 getShiftedIndex 处理。
- 面没有写 `uv` 时按 BlockElement.uvsByFace 反推默认 UV（照抄方块坐标），
  所以本预览器能直接渲染原版模型，也能验证"靠默认 UV 对齐"的模型。
- 按原版给六个面乘亮度系数（顶 1.0 / 南北 0.8 / 东西 0.6 / 底 0.5）。
- 动态贴图（配 .mcmeta 的竖向拼接图）只取第一帧 —— MC 的 SpriteContents
  只拿帧尺寸建 sprite，模型 UV 里的 [0,16] 永远映射到单帧，不会纵向拉伸。

投影用正交等轴：正交变换是线性的，平行四边形投影后仍是平行四边形，
所以每个面用一次 PIL 的仿射变换就能精确贴图，不需要拆三角形。

用法：
    python3 tools/render_block_model.py <模型json> <输出png> [yaw] [pitch] [缩放]
    # yaw=225 pitch=22 约等于物品栏里的方块预览角度

注意：这是"看形状和配色对不对"的工具，不模拟 AO、光照传播、区块剔除，
也不渲染 `cullface`（面一律画出来，方便发现被挡住的问题面）。
"""
import json
import math
import os
import sys

from PIL import Image, ImageDraw

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(REPO, "src/main/resources/assets")
# 可选：想拿原版模型/贴图做对照（比如量砖块比例、核对 UV 方向）时，
# 先把客户端 jar 里的 assets 解出来，再用环境变量指过去：
#   unzip -oq ~/.gradle/caches/neoformruntime/artifacts/minecraft_1.21.1_client.jar \
#         'assets/minecraft/*' -d /tmp/vanilla_ref
#   export PIRANPORT_VANILLA_ASSETS=/tmp/vanilla_ref/assets
VANILLA = os.environ.get("PIRANPORT_VANILLA_ASSETS", "")

MIN, MAX = 0, 1
# FaceInfo：每个方向的四个顶点按 0..3 的顺序，各轴取 MIN 还是 MAX
FACE_VERTS = {
    "down":  [(MIN, MIN, MAX), (MIN, MIN, MIN), (MAX, MIN, MIN), (MAX, MIN, MAX)],
    "up":    [(MIN, MAX, MIN), (MIN, MAX, MAX), (MAX, MAX, MAX), (MAX, MAX, MIN)],
    "north": [(MAX, MAX, MIN), (MAX, MIN, MIN), (MIN, MIN, MIN), (MIN, MAX, MIN)],
    "south": [(MIN, MAX, MAX), (MIN, MIN, MAX), (MAX, MIN, MAX), (MAX, MAX, MAX)],
    "west":  [(MIN, MAX, MIN), (MIN, MIN, MIN), (MIN, MIN, MAX), (MIN, MAX, MAX)],
    "east":  [(MAX, MAX, MAX), (MAX, MIN, MAX), (MAX, MIN, MIN), (MAX, MAX, MIN)],
}
# BlockFaceUV：顶点 i 的 u 取 uv[0] 或 uv[2]，v 取 uv[1] 或 uv[3]
UV_U = [0, 0, 2, 2]
UV_V = [1, 3, 3, 1]
# 原版各方块面亮度系数
SHADE = {"up": 1.0, "down": 0.5, "north": 0.8, "south": 0.8, "east": 0.6, "west": 0.6}


def load_model(path, depth=0):
    """读模型并合并父模型（贴图与 elements 都继承，子级覆盖父级）。"""
    with open(path) as f:
        model = json.load(f)
    parent = model.get("parent")
    if parent and depth < 10:
        ppath = resolve_model(parent)
        if ppath:
            base = load_model(ppath, depth + 1)
            merged = dict(base)
            merged["textures"] = {**base.get("textures", {}), **model.get("textures", {})}
            if "elements" in model:
                merged["elements"] = model["elements"]
            return merged
    return model


def resolve_model(ref):
    ns, _, name = ref.partition(":")
    if not name:
        ns, name = "minecraft", ns
    for root in filter(None, (ASSETS, VANILLA)):
        p = os.path.join(root, ns, "models", name + ".json")
        if os.path.exists(p):
            return p
    return None


def resolve_texture(ref, textures):
    """顺着 #alias 解析到实际贴图文件，并裁掉动态贴图的非首帧。"""
    seen = 0
    while ref.startswith("#") and seen < 10:
        ref = textures.get(ref[1:], ref)
        seen += 1
    ns, _, name = ref.partition(":")
    if not name:
        ns, name = "minecraft", ns
    for root in filter(None, (ASSETS, VANILLA)):
        p = os.path.join(root, ns, "textures", name + ".png")
        if os.path.exists(p):
            img = Image.open(p).convert("RGB")
            if os.path.exists(p + ".mcmeta") and img.height > 16:
                img = img.crop((0, 0, 16, 16))   # 只保留第一帧
            return img
    raise FileNotFoundError(ref)


def default_uv(dname, f, t):
    """BlockElement.uvsByFace：面没写 uv 时 MC 自己推的默认值。"""
    x1, y1, z1, x2, y2, z2 = f[0], f[1], f[2], t[0], t[1], t[2]
    if dname == "down":
        return [x1, 16.0 - z2, x2, 16.0 - z1]
    if dname == "up":
        return [x1, z1, x2, z2]
    if dname == "north":
        return [16.0 - x2, 16.0 - y2, 16.0 - x1, 16.0 - y1]
    if dname == "south":
        return [x1, 16.0 - y2, x2, 16.0 - y1]
    if dname == "west":
        return [z1, 16.0 - y2, z2, 16.0 - y1]
    return [16.0 - z2, 16.0 - y2, 16.0 - z1, 16.0 - y1]   # east


def rotation_uvs(uv, rotation):
    """BlockFaceUV.getU/getV 里的 rotation 处理。"""
    if not rotation:
        return uv
    shift = (rotation // 90) % 4
    return [uv[(i + shift) % 4] for i in range(4)]


def collect_faces(model):
    textures = model.get("textures", {})
    faces = []
    for elem in model.get("elements", []):
        f, t = elem["from"], elem["to"]
        for dname, face in elem.get("faces", {}).items():
            img = resolve_texture(face["texture"], textures)
            uv = rotation_uvs(list(face.get("uv") or default_uv(dname, f, t)),
                              face.get("rotation", 0))
            pts, uvs = [], []
            for i, (ax, ay, az) in enumerate(FACE_VERTS[dname]):
                pts.append((t[0] if ax else f[0], t[1] if ay else f[1], t[2] if az else f[2]))
                uvs.append((uv[UV_U[i]], uv[UV_V[i]]))
            faces.append({"dir": dname, "pts": pts, "uvs": uvs,
                          "img": img, "shade": SHADE[dname]})
    return faces


def cross(a, b):
    return (a[1] * b[2] - a[2] * b[1],
            a[2] * b[0] - a[0] * b[2],
            a[0] * b[1] - a[1] * b[0])


def camera(yaw, pitch):
    """返回相机的 (右, 上, 前) 正交基。前指向从相机看进场景的方向。

    yaw 是绕 Y 轴的水平角，pitch 是俯角；yaw=0 时相机在 +Z 看向 -Z（即看到
    方块的南面），要正面看 north 面（模型正面）得用 yaw=180。
    """
    y, p = math.radians(yaw), math.radians(pitch)
    fwd = (-math.sin(y) * math.cos(p), -math.sin(p), -math.cos(y) * math.cos(p))
    r = cross(fwd, (0, 1, 0))
    n = math.hypot(*r) or 1
    right = (r[0] / n, r[1] / n, r[2] / n)
    return right, cross(right, fwd), fwd


def dot(a, b):
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]


def render(model_path, out_path, yaw=35.0, pitch=30.0, scale=22, bg=(24, 26, 30)):
    render_faces(collect_faces(load_model(model_path)), out_path, yaw, pitch, scale, bg)


def render_faces(faces, out_path, yaw=35.0, pitch=30.0, scale=22, bg=(24, 26, 30)):
    """画一组面。face 里的 pts 是方块坐标，想预览"叠起来"的方块时可以把
    上面那层的 pts 整体加 16 再一起传进来（UV 已经算好，不受影响）。"""
    right, up, fwd = camera(yaw, pitch)

    for fc in faces:
        centroid = [sum(p[i] for p in fc["pts"]) / 4 for i in range(3)]
        fc["depth"] = dot(centroid, fwd)
        fc["sc"] = [(dot(p, right), -dot(p, up)) for p in fc["pts"]]
    faces.sort(key=lambda f: -f["depth"])          # 画家算法：远的先画

    xs = [p[0] for f in faces for p in f["sc"]]
    ys = [p[1] for f in faces for p in f["sc"]]
    pad = 4
    ox, oy = pad - min(xs) * scale, pad - min(ys) * scale
    size = (int((max(xs) - min(xs)) * scale + pad * 2),
            int((max(ys) - min(ys)) * scale + pad * 2))
    canvas = Image.new("RGB", size, bg)

    for fc in faces:
        dst = [(p[0] * scale + ox, p[1] * scale + oy) for p in fc["sc"]]
        d1 = (dst[1][0] - dst[0][0], dst[1][1] - dst[0][1])
        d2 = (dst[3][0] - dst[0][0], dst[3][1] - dst[0][1])
        s1 = (fc["uvs"][1][0] - fc["uvs"][0][0], fc["uvs"][1][1] - fc["uvs"][0][1])
        s2 = (fc["uvs"][3][0] - fc["uvs"][0][0], fc["uvs"][3][1] - fc["uvs"][0][1])
        det = d1[0] * d2[1] - d1[1] * d2[0]
        if abs(det) < 1e-9:                       # 正对镜头的面投影成一条线
            continue
        a = (s1[0] * d2[1] - s2[0] * d1[1]) / det
        b = (s2[0] * d1[0] - s1[0] * d2[0]) / det
        d = (s1[1] * d2[1] - s2[1] * d1[1]) / det
        e = (s2[1] * d1[0] - s1[1] * d2[0]) / det
        # uv 是 0..16 的贴图坐标，换算成像素
        tw, th = fc["img"].size
        sx, sy = tw / 16.0, th / 16.0
        a, b, d, e = a * sx, b * sx, d * sy, e * sy
        c = fc["uvs"][0][0] * sx - a * dst[0][0] - b * dst[0][1]
        f_ = fc["uvs"][0][1] * sy - d * dst[0][0] - e * dst[0][1]

        layer = Image.new("RGBA", size, (0, 0, 0, 0))
        layer.paste(fc["img"].convert("RGBA").transform(
            size, Image.AFFINE, (a, b, c, d, e, f_), resample=Image.NEAREST), (0, 0))
        if fc["shade"] != 1.0:
            ch = layer.split()
            layer = Image.merge("RGBA", [c_.point(lambda v, s=fc["shade"]: int(v * s))
                                         for c_ in ch[:3]] + [ch[3]])
        mask = Image.new("L", size, 0)
        ImageDraw.Draw(mask).polygon(dst, fill=255)
        canvas.paste(layer, (0, 0), mask)

    canvas.save(out_path)
    print("wrote", out_path, canvas.size)


if __name__ == "__main__":
    if len(sys.argv) < 3:
        print(__doc__)
        sys.exit(1)
    a = sys.argv[1:]
    render(a[0], a[1],
           float(a[2]) if len(a) > 2 else 35.0,
           float(a[3]) if len(a) > 3 else 30.0,
           int(a[4]) if len(a) > 4 else 22)
