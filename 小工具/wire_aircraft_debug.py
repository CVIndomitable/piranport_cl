# -*- coding: utf-8 -*-
"""把舰载机模型接进 /ppd model_debug（渲染器 + 合法值名单）。

用法：
    python3 小工具/wire_aircraft_debug.py ju87c JU87C Ju87cModel ju87c
参数：飞机 id、Java 常量名、模型类名、调试渲染器里的字段名。
"""
import re
import sys
from pathlib import Path

RENDERER = Path("src/main/java/com/piranport/client/ModelDebugBlockEntityRenderer.java")
COMMANDS = Path("src/main/java/com/piranport/debug/PiranPortCommands.java")


def sub_once(text, pattern, repl, what):
    new, n = re.subn(pattern, repl, text, count=1)
    assert n == 1, f"anchor not found: {what}"
    return new


def wire(ident, const, class_name, var):
    tex = f"{const}_TEXTURE"

    # ---- 调试台渲染器 ----
    s = RENDERER.read_text()
    if f"{class_name}<Entity>" not in s:
        s = sub_once(s, r'(import com\.piranport\.client\.model\.TorpedoModel;)',
                     f'import com.piranport.client.model.{class_name};\n\\1', "import")
        s = sub_once(s, r'(    private static final ResourceLocation HEAVY_CRUISER_TEXTURE =)',
                     f'    private static final ResourceLocation {tex} =\n'
                     f'            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, '
                     f'"textures/entity/{ident}.png");\n\\1',
                     "texture constant")
        s = sub_once(s, r'(    private final DeepOceanHeavyCruiserModel heavyCruiser;)',
                     f'    private final {class_name}<Entity> {var};\n\\1', "field")
        s = sub_once(s, r'(        this\.heavyCruiser = new DeepOceanHeavyCruiserModel\()',
                     f'        this.{var} = new {class_name}<>('
                     f'ctx.getModelSet().bakeLayer({class_name}.LAYER_LOCATION));\n\\1',
                     "constructor")
        s = sub_once(s, r'(            case "heavy_cruiser" ->)',
                     f'            case "{ident}" -> {{ model = {var}; texture = {tex}; }}\n\\1',
                     "switch case")
        RENDERER.write_text(s)

    # ---- 指令建议 + 白名单 ----
    s = COMMANDS.read_text()
    if f'builder.suggest("{ident}");' not in s:
        s = sub_once(s, r'(\s+)builder\.suggest\("heavy_cruiser"\);',
                     lambda m: (f'{m.group(1)}builder.suggest("{ident}");\n'
                                f'{m.group(1)}builder.suggest("heavy_cruiser");'),
                     "command suggestion")
    m = re.search(r'(MODEL_DEBUG_TYPES = java\.util\.List\.of\(\n\s*)([^\n]*)(\n)', s)
    assert m, "MODEL_DEBUG_TYPES"
    if f'"{ident}"' not in m.group(2):
        s = s[:m.start(2)] + m.group(2).replace('"heavy_cruiser"',
                                               f'"{ident}", "heavy_cruiser"', 1) + s[m.end(2):]
    COMMANDS.write_text(s)
    print("debug-wired", ident, "->", class_name)


if __name__ == "__main__":
    wire(*sys.argv[1:5])
