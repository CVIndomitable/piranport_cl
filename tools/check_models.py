#!/usr/bin/env python3
"""方块/物品模型严格校验器 —— 按 Minecraft 模型编解码器的规则检查。

为什么需要这个：
`tools/render_block_model.py` 只做 `json.load` 然后读它认识的字段，
**未知字段会被静默忽略**。所以一个带 `_comment` 的模型能正常渲染预览，
进游戏却是紫黑方块（模型解析失败 → missing model 回退）。

典型踩坑：
    "elements": [{ "_comment": "立柱", "from": [...], ... }]
                ^^^^^^^^^^^^^^^^^ 未知道具 → 整个模型解析失败

Minecraft 用的是严格 JSON codec：多一个不认识的键，整份模型直接作废，
连报错都只体现在日志里，游戏里只看到紫黑格。

用法：
    python3 tools/check_models.py            # 校验全部模型
    python3 tools/check_models.py <文件...>  # 只校验指定文件
"""
import glob
import json
import os
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(REPO, "src/main/resources/assets")

# 允许的顶层键。render_type 是 NeoForge 扩展（原版没有），必须放行。
TOP_KEYS = {"parent", "ambientocclusion", "display", "textures", "elements",
            "gui_light", "overrides", "render_type", "variants", "multipart"}
ELEMENT_KEYS = {"from", "to", "rotation", "shade", "faces"}
FACE_KEYS = {"uv", "texture", "cullface", "rotation", "tintindex"}
DIRECTIONS = {"down", "up", "north", "south", "east", "west"}


def check(path):
    """返回错误列表。空列表 = 通过。"""
    errs = []
    try:
        with open(path, encoding="utf-8") as f:
            d = json.load(f)
    except json.JSONDecodeError as e:
        return [f"JSON 语法错误: {e}"]
    if not isinstance(d, dict):
        return ["顶层不是对象"]

    for k in d:
        if k not in TOP_KEYS:
            errs.append(f"顶层未知键 {k!r} —— MC 严格解析会因此丢弃整个模型")

    textures = d.get("textures") or {}
    if "textures" in d and not isinstance(textures, dict):
        errs.append("textures 不是对象")

    for i, e in enumerate(d.get("elements") or []):
        if not isinstance(e, dict):
            errs.append(f"元素{i} 不是对象")
            continue
        for k in e:
            if k not in ELEMENT_KEYS:
                errs.append(f"元素{i} 未知键 {k!r}")
        f_, t_ = e.get("from"), e.get("to")
        if not (isinstance(f_, list) and len(f_) == 3):
            errs.append(f"元素{i} from 必须是三元数组")
        if not (isinstance(t_, list) and len(t_) == 3):
            errs.append(f"元素{i} to 必须是三元数组")
        if isinstance(f_, list) and isinstance(t_, list) and len(f_) == 3 == len(t_):
            for j, ax in enumerate("xyz"):
                if f_[j] > t_[j]:
                    errs.append(f"元素{i} {ax} 轴 from({f_[j]}) > to({t_[j]})")
                for k, lbl in ((f_[j], "from"), (t_[j], "to")):
                    if k > 32 or k < -16:
                        errs.append(
                            f"元素{i} {lbl}.{ax} = {k} 超出 MC 硬边界 [-16, 32]"
                            f" —— 会导致整个模型解析失败（游戏里显示紫黑 missing model）")
        for fn, fv in (e.get("faces") or {}).items():
            if fn not in DIRECTIONS:
                errs.append(f"元素{i} 未知面 {fn!r}")
            if not isinstance(fv, dict):
                continue
            for k in fv:
                if k not in FACE_KEYS:
                    errs.append(f"元素{i}.{fn} 未知键 {k!r}")
            ref = fv.get("texture")
            if ref is None:
                errs.append(f"元素{i}.{fn} 缺少 texture")
            elif ref.startswith("#"):
                if ref[1:] not in textures:
                    errs.append(f"元素{i}.{fn} 引用未定义的贴图变量 {ref!r}")

    # 贴图变量指向的文件是否真实存在（只查本仓库命名空间）
    for var, ref in textures.items():
        if not isinstance(ref, str):
            continue
        seen = 0
        while ref.startswith("#") and seen < 10:
            ref = textures.get(ref[1:], ref)
            seen += 1
        if not ref.startswith("piranport:"):
            continue
        rel = ref.split(":", 1)[1]
        png = os.path.join(ASSETS, "piranport/textures", rel + ".png")
        if not os.path.exists(png):
            errs.append(f"贴图变量 {var!r} → {ref} 的文件不存在: {rel}.png")
    return errs


def main():
    if len(sys.argv) > 1:
        files = sys.argv[1:]
    else:
        files = []
        for pat in ("models/block/*.json", "models/item/*.json", "blockstates/*.json"):
            files += glob.glob(os.path.join(ASSETS, "piranport", pat))
    total = 0
    for f in sorted(files):
        errs = check(f)
        if errs:
            total += len(errs)
            print(f"\n✗ {os.path.relpath(f, REPO)}")
            for e in errs:
                print(f"    {e}")
    print(f"\n检查 {len(files)} 个文件，{total} 个问题")
    return 1 if total else 0


if __name__ == "__main__":
    sys.exit(main())
