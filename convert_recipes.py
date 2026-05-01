#!/usr/bin/env python3
"""
将 1.21.1 格式的配方转换为 1.20.1 格式。
主要差异：
1. result 从 {"id": "...", "count": N} 改为 "..." 或 {"item": "...", "count": N}
2. ingredient 从 {"item": "..."} 保持不变（1.21.1 支持裸字符串，但 1.20.1 需要对象）
"""
import json
import os
from pathlib import Path

recipes_dir = Path("src/main/resources/data/piranport/recipes")

for recipe_file in recipes_dir.glob("*.json"):
    with open(recipe_file, 'r', encoding='utf-8') as f:
        data = json.load(f)

    # 转换 result 字段
    if "result" in data and isinstance(data["result"], dict):
        if "id" in data["result"]:
            # 1.21.1 格式: {"id": "...", "count": N}
            item_id = data["result"]["id"]
            count = data["result"].get("count", 1)

            if count == 1:
                # 单个物品：直接用字符串
                data["result"] = item_id
            else:
                # 多个物品：用 {"item": "...", "count": N}
                data["result"] = {"item": item_id, "count": count}

    # 写回文件
    with open(recipe_file, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write('\n')

print(f"转换完成，共处理 {len(list(recipes_dir.glob('*.json')))} 个配方文件")
