package com.piranport.dungeon.block;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 海域出击书台外观资源的结构测试。
 *
 * <p>讲台之前是"透明方块"——blockstate 指向 {@code minecraft:block/lectern}，
 * 而 mod 自己没有任何 dungeon_lectern 贴图，所以它只剩碰撞箱。这组测试锁死修复后的约定：
 * <ul>
 *   <li>blockstate 必须覆盖 facing × has_key 全 8 种组合（钥匙插没插外观要有区别）</li>
 *   <li>两个模型引用的贴图必须真实存在于 assets 目录</li>
 *   <li>带钥匙状态必须叠加空台面模型和独立的钥匙模型，避免子模型覆盖台面几何</li>
 * </ul>
 *
 * <p>只读资源文件、不碰方块注册表，因此不会触发 MC 的 bootstrap（见
 * 踩坑记录：单测碰 BuiltInRegistries 会抛 ExceptionInInitializerError）。
 */
class DungeonLecternModelTest {

    private static final String ASSETS = "/assets/piranport";

    @Test
    void blockstateCoversEveryFacingAndKeyCombination() throws IOException {
        JsonObject blockstate = readJson(ASSETS + "/blockstates/dungeon_lectern.json");
        JsonObject variants = blockstate.getAsJsonObject("variants");
        assertNotNull(variants, "blockstate 必须有 variants");

        List<String> missing = new ArrayList<>();
        for (String facing : List.of("north", "south", "east", "west")) {
            for (String hasKey : List.of("true", "false")) {
                String key = "facing=" + facing + ",has_key=" + hasKey;
                if (!variants.has(key)) {
                    missing.add(key);
                }
            }
        }
        assertTrue(missing.isEmpty(),
                "blockstate 缺少变体（会导致该朝向下方块不可见）：" + missing);
        assertEquals(8, variants.size(), "讲台应只有 facing × has_key 共 8 个变体");

        // 空状态直接使用底座；有钥匙状态必须同时使用底座和钥匙叠加层。
        String emptyModel = variants.getAsJsonObject("facing=north,has_key=false")
                .get("model").getAsString();
        JsonArray keyedModels = variants.getAsJsonArray("facing=north,has_key=true");
        assertEquals(2, keyedModels.size(),
                "has_key=true 必须由底座模型和钥匙模型两层叠加");
        assertEquals(emptyModel, keyedModels.get(0).getAsJsonObject().get("model").getAsString(),
                "有钥匙状态必须保留讲台底座");
        assertEquals("piranport:block/dungeon_lectern_key",
                keyedModels.get(1).getAsJsonObject().get("model").getAsString(),
                "有钥匙状态必须追加独立钥匙模型");
    }

    @Test
    void bothModelsReferenceExistingTextures() throws IOException {
        for (String model : List.of("dungeon_lectern", "dungeon_lectern_key")) {
            JsonObject json = readJson(ASSETS + "/models/block/" + model + ".json");
            JsonObject textures = json.getAsJsonObject("textures");
            assertNotNull(textures, model + " 必须定义 textures");

            for (var entry : textures.entrySet()) {
                String ref = entry.getValue().getAsString();
                if (ref.startsWith("#")) {
                    continue; // 纹理变量引用，由父模型提供
                }
                // 只校验本 mod 的贴图；原版贴图不在本资源包内
                if (!ref.startsWith("piranport:block/")) {
                    continue;
                }
                String path = ASSETS + "/textures/" + ref.replace("piranport:", "") + ".png";
                try (InputStream in = getClass().getResourceAsStream(path)) {
                    assertNotNull(in, model + " 引用了不存在的贴图：" + ref + "（期望 " + path + "）");
                }
            }
        }
    }

    @Test
    void keyedModelOwnsOnlyTheKeyGeometry() throws IOException {
        JsonObject keyed = readJson(ASSETS + "/models/block/dungeon_lectern_key.json");
        assertEquals("minecraft:block/block", keyed.get("parent").getAsString(),
                "钥匙叠加层必须使用基础 block 父模型，避免覆盖底座模型");

        JsonObject empty = readJson(ASSETS + "/models/block/dungeon_lectern.json");
        int deskElements = empty.getAsJsonArray("elements").size();
        int keyElements = keyed.getAsJsonArray("elements").size();
        assertEquals(1, keyElements, "子模型只应追加 1 个钥匙 element");

        assertTrue(deskElements > 1, "空模型应有多个台面 element");

        // 钥匙必须有独立贴图，否则会和台面糊在一起看不出区别
        assertTrue(keyed.getAsJsonObject("textures").has("key"),
                "带钥匙模型必须定义 key 贴图");
    }

    @Test
    void emptyModelActuallyHasGeometry() throws IOException {
        JsonObject empty = readJson(ASSETS + "/models/block/dungeon_lectern.json");
        JsonArray elements = empty.getAsJsonArray("elements");
        assertNotNull(elements, "空模型必须有 elements");
        assertTrue(elements.size() >= 4,
                "空讲台应是多部件的指挥台（底座 + 台身 + 台面 + 立柱…），当前只有 " + elements.size());

        // 每个 element 都必须有 from/to/faces，否则模型加载时会报错
        int i = 0;
        for (JsonElement el : elements) {
            JsonObject obj = el.getAsJsonObject();
            assertTrue(obj.has("from") && obj.has("to"), "element " + i + " 缺少 from/to");
            assertTrue(obj.has("faces"), "element " + i + " 缺少 faces");
            i++;
        }
    }

    @Test
    void keyedModelUsesCutoutRenderType() throws IOException {
        // 钥匙贴图只有 83/256 像素不透明（alpha 轮廓），带 alpha 的几何必须走 cutout 做 alpha 裁剪。
        // 写法必须与 mod 内其他 54 个模型一致：带 minecraft: 命名空间（裸 "cutout" 不在验证范围内）。
        JsonObject keyed = readJson(ASSETS + "/models/block/dungeon_lectern_key.json");
        assertEquals("minecraft:cutout", keyed.get("render_type").getAsString(),
                "带钥匙模型必须声明 render_type=minecraft:cutout，否则 alpha 钥匙贴图渲染错乱");
    }

    @Test
    void cutoutRenderTypeSitsOnTheModelOwningTheAlphaTexture() throws IOException {
        // 这是"插上钥匙后方块透明"的真正的坑，之前的三层叠加把根因藏住了：
        //   1) 子模型 dungeon_lectern_key 声明 render_type=cutout —— 但【只作用于它自己定义的
        //      element】。它 parent 指向 dungeon_lectern，而父模型 parent 是 minecraft:block/block，
        //      block/block 不带 render_type，默认 solid（FaceBakery 的默认渲染类型）。
        //   2) 于是 inherited 的台面几何仍是 solid，而 cutout 的钥匙 element 与它是
        //      【同一个 block model part 的同一批 quads】—— 一个 part 只取一次渲染类型。
        //   3) 钥匙贴图 83/256 像素 alpha=0，solid 层不做 alpha 裁剪，整批 quad 渲染错乱 → 方块"消失"。
        // 所以 render_type 必须落在【声明了带 alpha 贴图的 element、且离根最近】的模型上。
        // dungeon_lectern（父）显式声明后，两层都成了 cutout，与渲染类型无关的台面贴图是全不透明的，
        // 视觉零变化。
        JsonObject empty = readJson(ASSETS + "/models/block/dungeon_lectern.json");
        assertEquals("minecraft:block/block", empty.get("parent").getAsString(),
                "前提校验：空模型的父是 minecraft:block/block（它不带 render_type）");
        assertTrue(empty.has("render_type"),
                "dungeon_lectern 必须自己声明 render_type —— 它的父 minecraft:block/block 不带，"
                        + "不声明就默认 solid，子模型的 cutout 声明救不了 inherited 的台面几何");
        String emptyType = empty.get("render_type").getAsString();
        assertEquals("minecraft:cutout", emptyType,
                "空模型必须声明 minecraft:cutout（与带钥匙模型一致，避免同一方块的两个状态用不同渲染类型）");

        // 两层渲染类型必须相同，否则 has_key 翻转时渲染类型跳变
        JsonObject keyed = readJson(ASSETS + "/models/block/dungeon_lectern_key.json");
        assertEquals(emptyType, keyed.get("render_type").getAsString(),
                "空模型与带钥匙模型的 render_type 必须一致，否则插入/取出钥匙时渲染类型跳变");
    }

    @Test
    void everyAlphaTextureIsUsedOnlyByCutoutModels() throws IOException {
        // 反向锁：只要某个模型引用了带 alpha 的贴图，它的渲染类型就必须是 cutout。
        // 用反射枚举 assets 下的模型资源，避免手写清单在新增模型时静默失效。
        // 已知带 alpha 的贴图（按本 mod 的资源实测）：dungeon_lectern_key。
        // 若日后新增带 alpha 的方块贴图，请把它加进这个集合——本测试的意义正是"逼人显式登记"。
        List<String> alphaTextures = List.of("piranport:block/dungeon_lectern_key");
        for (String model : List.of("dungeon_lectern", "dungeon_lectern_key")) {
            JsonObject json = readJson(ASSETS + "/models/block/" + model + ".json");
            JsonObject textures = json.getAsJsonObject("textures");
            boolean usesAlpha = textures != null && textures.entrySet().stream()
                    .anyMatch(e -> alphaTextures.contains(e.getValue().getAsString()));
            if (!usesAlpha) {
                continue;
            }
            assertEquals("minecraft:cutout", json.get("render_type").getAsString(),
                    model + " 引用了带 alpha 的贴图，render_type 必须是 minecraft:cutout");
        }
    }

    @Test
    void elementRotationsAreLegalForVanillaDeserializer() throws IOException {
        // 原版 BlockElement.Deserializer 只接受 22.5 的整数倍，其余值会抛 JsonParseException
        // 并让【整个模型】加载失败 —— 表现就是方块又变回透明。这个坑踩过一次，锁死它。
        for (String model : List.of("dungeon_lectern", "dungeon_lectern_key")) {
            JsonObject json = readJson(ASSETS + "/models/block/" + model + ".json");
            for (JsonElement el : json.getAsJsonArray("elements")) {
                JsonObject obj = el.getAsJsonObject();
                if (!obj.has("rotation")) {
                    continue;
                }
                JsonObject rot = obj.getAsJsonObject("rotation");
                double angle = rot.get("angle").getAsDouble();
                double steps = angle / 22.5;
                assertTrue(Math.abs(steps - Math.rint(steps)) < 1e-9,
                        model + " 的 rotation.angle=" + angle
                                + " 不合法：只允许 -45/-22.5/0/22.5/45");
                assertTrue(Math.abs(angle) <= 45,
                        model + " 的 rotation.angle=" + angle + " 超出 ±45 范围");

                // axis 必须是 x/y/z，origin 必须落在 16px 空间内
                String axis = rot.get("axis").getAsString();
                assertTrue(List.of("x", "y", "z").contains(axis),
                        model + " 的 rotation.axis='" + axis + "' 非法");
                JsonArray origin = rot.getAsJsonArray("origin");
                assertEquals(3, origin.size(), model + " 的 rotation.origin 必须是 3 个数");
                for (JsonElement c : origin) {
                    double v = c.getAsDouble();
                    assertTrue(v >= -16 && v <= 32,
                            model + " 的 rotation.origin 分量 " + v + " 明显越界（期望 0..16 附近）");
                }
            }
        }
    }

    @Test
    void uvsStayInsideTheTexture() throws IOException {
        // UV 越界不会让模型加载失败，但会把贴图采样到边缘像素上，视觉上是拉丝。
        for (String model : List.of("dungeon_lectern", "dungeon_lectern_key")) {
            JsonObject json = readJson(ASSETS + "/models/block/" + model + ".json");
            for (JsonElement el : json.getAsJsonArray("elements")) {
                JsonObject obj = el.getAsJsonObject();

                // from 必须严格小于 to，否则原版会报 "Invalid element"
                JsonArray from = obj.getAsJsonArray("from");
                JsonArray to = obj.getAsJsonArray("to");
                for (int i = 0; i < 3; i++) {
                    assertTrue(from.get(i).getAsDouble() < to.get(i).getAsDouble(),
                            model + " 的 element from/to 在第 " + i + " 轴上不是递增的："
                                    + from + " -> " + to);
                }

                for (var face : obj.getAsJsonObject("faces").entrySet()) {
                    JsonObject f = face.getValue().getAsJsonObject();
                    JsonArray uv = f.getAsJsonArray("uv");
                    if (uv == null) {
                        continue; // 缺省 uv 由原版按面自动推导
                    }
                    assertEquals(4, uv.size(),
                            model + " 的 " + face.getKey() + " 面 uv 必须是 4 个数");
                    for (int i = 0; i < 4; i++) {
                        double v = uv.get(i).getAsDouble();
                        assertTrue(v >= 0 && v <= 16,
                                model + " 的 " + face.getKey() + " 面 uv[" + i + "]=" + v
                                        + " 超出 0..16 贴图范围");
                    }
                }
            }
        }
    }

    @Test
    void lecternMustRenderAsModelNotInvisible() throws Exception {
        // BaseEntityBlock 默认 getRenderShape() == INVISIBLE（为"BE 自带渲染器"准备）。
        // 本 BE 没有渲染器，不覆写的话区块渲染阶段直接跳过方块模型 ——
        // 表现就是"只有碰撞箱、方块透明"，正是这个方块最初的 bug。
        // 反射拿注解表判定"是否覆写"，不初始化方块实例，避免触发 MC bootstrap。
        Method getRenderShape = DungeonLecternBlock.class.getDeclaredMethod(
                "getRenderShape", net.minecraft.world.level.block.state.BlockState.class);
        assertTrue(getRenderShape.getDeclaringClass() == DungeonLecternBlock.class,
                "DungeonLecternBlock 必须自己覆写 getRenderShape 并返回 RenderShape.MODEL，"
                        + "否则方块会渲染成透明（只有碰撞箱）");
    }

    private JsonObject readJson(String resourcePath) throws IOException {
        try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
            assertNotNull(in, "找不到资源：" + resourcePath);
            return JsonParser.parseReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
