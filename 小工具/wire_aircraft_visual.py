# -*- coding: utf-8 -*-
"""把一个新的舰载机 visual_id 接进服务端白名单、客户端注册表、渲染器和层定义。

批量机型每架都要改 4 个共享文件 + 飞机定义，手改容易漏；这里集中处理并断言
每个锚点只命中一次。用法：

    python3 小工具/wire_aircraft_visual.py ju87c JU87C Ju87cModel ju87cModel

参数：飞机定义 id、Java 常量名、模型类名、渲染器字段名（贴图默认与 id 同名）。
"""
import json
import re
import sys
from pathlib import Path

VISUAL_IDS = Path("src/main/java/com/piranport/aviation/AircraftVisualIds.java")
REGISTRY = Path("src/main/java/com/piranport/client/AircraftVisualRegistry.java")
RENDERER = Path("src/main/java/com/piranport/client/AircraftRenderer.java")
CLIENT = Path("src/main/java/com/piranport/client/ClientModEvents.java")
DATA = Path("src/main/resources/data/piranport/aircraft")


def sub_once(text, pattern, repl, what):
    new, n = re.subn(pattern, repl, text, count=1)
    assert n == 1, f"anchor not found: {what}"
    return new


def wire(ident, const, class_name, model_var, texture=None):
    texture = texture or ident

    # ---- 服务端白名单 ----
    s = VISUAL_IDS.read_text()
    if f'String {const} =' not in s:
        s = sub_once(s, r'(    public static final String FIGHTER = "fighter";)',
                     f'    /** {ident} 专属模型。 */\n'
                     f'    public static final String {const} = "{ident}";\n\\1',
                     "AircraftVisualIds constant")
        s = sub_once(s, r'(KNOWN = Set\.of\(\s*\n\s*B25, F4F, )', rf'\1{const}, ', "KNOWN set")
        VISUAL_IDS.write_text(s)

    # ---- 客户端注册表 ----
    s = REGISTRY.read_text()
    if f"{const}_TEXTURE" not in s:
        s = sub_once(s, r'(    private static final AircraftVisual B25 =)',
                     f'    private static final ResourceLocation {const}_TEXTURE =\n'
                     f'            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, '
                     f'"textures/entity/{texture}.png");\n\n\\1',
                     "texture constant")
        s = sub_once(s, r'(    // 美术占位：待替换为正式模型与贴图。)',
                     f'    private static final AircraftVisual {const} =\n'
                     f'            new AircraftVisual(AircraftVisualIds.{const}, ModelKind.{const}, '
                     f'{const}_TEXTURE);\n\n\\1',
                     "visual constant")
        s = sub_once(s, r'(            Map\.entry\(AircraftVisualIds\.F4F, F4F\),)',
                     rf'\1\n            Map.entry(AircraftVisualIds.{const}, {const}),',
                     "map entry")
        m = re.search(r'(public enum ModelKind \{\n)((?:\s+\w+,\n)*)(\s+)(\w+)\n    \}', s)
        assert m, "ModelKind enum"
        s = s[:m.start()] + (m.group(1) + m.group(2) + m.group(3) + m.group(4) + ",\n"
                             + m.group(3) + const + "\n    }") + s[m.end():]
        REGISTRY.write_text(s)

    # ---- 渲染器 ----
    s = RENDERER.read_text()
    if f"{class_name}<AircraftEntity>" not in s:
        s = sub_once(s, r'(import com\.piranport\.client\.model\.[^\n]+;)',
                     f'\\1\nimport com.piranport.client.model.{class_name};', "renderer import")
        s = sub_once(s, r'(    public AircraftRenderer\()',
                     f'    private final {class_name}<AircraftEntity> {model_var};\n\n\\1',
                     "renderer field")
        m = re.search(r'(        this\.\w+Model = new [^\n]+;\n)(?!        this\.\w+Model)', s)
        assert m, "renderer constructor assignment anchor"
        s = s[:m.end()] + (f'        this.{model_var} = new {class_name}<>('
                           f'ctx.bakeLayer({class_name}.LAYER_LOCATION));\n') + s[m.end():]
        m = re.search(r'(        model = switch \(visual\.modelKind\(\)\) \{\n(?:.*\n)*?)(        \};)',
                      s)
        assert m, "renderer switch"
        s = s[:m.start(2)] + f'            case {const} -> {model_var};\n' + s[m.start(2):]
        RENDERER.write_text(s)

    # ---- 层定义 ----
    s = CLIENT.read_text()
    if f"{class_name}.LAYER_LOCATION" not in s:
        matches = list(re.finditer(
            r'        event\.registerLayerDefinition\(com\.piranport\.client\.model\.\w+\.LAYER_LOCATION,\n'
            r'                com\.piranport\.client\.model\.\w+::createBodyLayer\);\n', s))
        assert matches, "layer registration anchor"
        pos = matches[-1].end()
        s = s[:pos] + (f'        event.registerLayerDefinition(com.piranport.client.model.'
                       f'{class_name}.LAYER_LOCATION,\n'
                       f'                com.piranport.client.model.{class_name}::createBodyLayer);\n') + s[pos:]
        CLIENT.write_text(s)

    # ---- 飞机定义 ----
    p = DATA / f"{ident}.json"
    d = json.loads(p.read_text())
    d["visual_id"] = ident
    p.write_text(json.dumps(d, ensure_ascii=False, indent=2) + "\n")
    print("wired", ident, "->", const, class_name)


if __name__ == "__main__":
    wire(*sys.argv[1:5], *(sys.argv[5:6] or []))
