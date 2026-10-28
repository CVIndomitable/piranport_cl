#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""复查 models/**/*.json 的贴图引用是否完整。

检查两件事：
  1. 每个面的 "#xxx" 都能在本模型 + 父模型链合并后的 textures 里找到；
  2. 每个 piranport: 贴图路径都有对应的 PNG 文件。

只把「不是别的模型 parent」的模型当作烘焙入口（root）来检查：
custom/*.json 这类 Blockbench parent 模型本身不写 textures，
贴图键由引用它的 block/item 子模型提供；单独检查它们会误报。
"""
import json
import os
import sys

ASSETS = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                      "src/main/resources/assets/piranport")
MODELS = os.path.join(ASSETS, "models")

NS = "piranport"


def rel_to_path(model_id):
    """piranport:custom/foo -> <ASSETS>/models/custom/foo.json；其它命名空间跳过。"""
    ns, _, path = model_id.partition(":")
    if ns != NS:
        return None
    return os.path.join(MODELS, path + ".json")


def collect_refs(node, acc):
    if isinstance(node, dict):
        for k, v in node.items():
            if k == "texture" and isinstance(v, str) and v.startswith("#"):
                acc.add(v[1:])
            else:
                collect_refs(v, acc)
    elif isinstance(node, list):
        for v in node:
            collect_refs(v, acc)


def walk_chain(path, cache):
    """返回 (合并后的 refs, 合并后的 textures)。子模型优先于父模型。"""
    refs, textures = set(), {}
    seen = set()
    cur = path
    while cur and cur not in seen:
        seen.add(cur)
        if cur not in cache:
            with open(cur, encoding="utf-8") as f:
                cache[cur] = json.load(f)
        data = cache[cur]
        collect_refs(data.get("elements", []), refs)
        for k, v in (data.get("textures") or {}).items():
            textures.setdefault(k, v)
        parent = data.get("parent")
        cur = rel_to_path(parent) if parent else None
        if cur and not os.path.exists(cur):
            break
    return refs, textures


def main():
    cache = {}
    all_models = []
    for root, _, files in os.walk(MODELS):
        for name in files:
            if name.endswith(".json"):
                all_models.append(os.path.join(root, name))

    referenced_as_parent = set()
    for p in all_models:
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        cache[p] = data
        parent = data.get("parent")
        if parent:
            target = rel_to_path(parent)
            if target:
                referenced_as_parent.add(target)

    problems = []
    checked = 0
    for p in sorted(all_models):
        if p in referenced_as_parent:
            continue
        checked += 1
        refs, textures = walk_chain(p, cache)
        missing = sorted(r for r in refs if r not in textures)
        if missing:
            problems.append((p, "未定义引用", missing))
        # 只查真正被面引用的纹理；定义了却没用的条目缺图不影响渲染
        for key in sorted(refs):
            value = textures[key]
            ns, _, path = value.partition(":")
            if ns != NS:
                continue  # 原版或其它命名空间不查本仓库 PNG
            png = os.path.join(ASSETS, "textures", path + ".png")
            if not os.path.exists(png):
                problems.append((p, "缺 PNG(" + key + ")", value))

    for p, kind, detail in problems:
        print(f"{os.path.relpath(p, MODELS)} | {kind}: {detail}")
    print(f"检查入口模型 {checked} 个，问题 {len(problems)} 个")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
