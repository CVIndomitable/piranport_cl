package com.piranport.dungeon.block;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import com.mojang.blaze3d.platform.NativeImage;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 把两个讲台模型真正烘成四边形，数一下顶点是否落在方块体积内。
 *
 * <p>前面的测试只到"原版反序列化器收不收"这一层。但用户是插上钥匙之后方块消失，
 * 这说明模型能解析、能烘焙出四边形，只是那些四边形的坐标把方块"撑没了"——
 * 原版的区块渲染对超出 0..16 的体积容忍度很低（背面剔除 + 光照计算都会跟着错）。
 *
 * <p>所以这里往下走一层：用 {@link FaceBakery#bakeQuad} 对每个 element 的每个面真烘一遍，
 * 断言顶点全部落在 [-0.1, 16.1] 的方盒里（留 0.1 给原版烘焙的浮点误差）。
 * 同时断言钥匙 element 不能和台面 element 完全共面，否则 z-fighting。
 *
 * <p>不碰注册表/资源管理器：{@link TextureAtlasSprite} 是接口，用一个空实现即可，
 * 本测试只关心几何，不关心贴图采样。
 */
class DungeonLecternQuadBakeTest {

    private static final String ASSETS = "/assets/piranport";

    private static final JsonDeserializationContext CONTEXT = new JsonDeserializationContext() {
        private final BlockElement.Deserializer elements = new BlockElement.Deserializer();
        private final BlockElementFace.Deserializer faces = new BlockElementFace.Deserializer();
        private final BlockFaceUV.Deserializer uvs = new BlockFaceUV.Deserializer();

        @Override
        public <T> T deserialize(JsonElement json, Type typeOfT) throws JsonParseException {
            Object result = null;
            if (typeOfT == BlockElement.class) {
                result = elements.deserialize(json, typeOfT, this);
            } else if (typeOfT == BlockElementFace.class) {
                result = faces.deserialize(json, typeOfT, this);
            } else if (typeOfT == BlockFaceUV.class) {
                result = uvs.deserialize(json, typeOfT, this);
            }
            @SuppressWarnings("unchecked")
            T typed = (T) result;
            return typed;
        }
    };

    /**
     * 一个真的 16×16 精灵，但用空白图构造。
     *
     * <p>不能用 {@code null} / 匿名类糊弄：{@link TextureAtlasSprite} 的构造函数会读
     * {@code contents.width()/height()} 算 UV，传 null 直接 NPE。所以老老实实造一张
     * 16×16 的 {@link NativeImage} 包成 SpriteContents —— 烘焙只关心几何与 UV 归一化，
     * 不关心像素内容，空白图足够。
     */
    private static TextureAtlasSprite stubSprite() {
        NativeImage image = new NativeImage(16, 16, false);
        SpriteContents contents = new SpriteContents(
                ResourceLocation.withDefaultNamespace("stub"),
                new FrameSize(16, 16), image, ResourceMetadata.EMPTY);
        return new StubSprite(contents);
    }

    /** 构造函数是 protected，只能靠继承拿到——测试类里开个最小的子类。 */
    private static final class StubSprite extends TextureAtlasSprite {
        StubSprite(SpriteContents contents) {
            super(InventoryMenu.BLOCK_ATLAS, contents, 16, 16, 0, 0);
        }
    }

    private static final ModelState IDENTITY = new ModelState() {
        @Override
        public Transformation getRotation() {
            return Transformation.identity();
        }

        @Override
        public boolean isUvLocked() {
            return false;
        }
    };

    @Test
    void everyBakedQuadStaysInsideTheBlockVolume() throws Exception {
        FaceBakery bakery = new FaceBakery();
        TextureAtlasSprite sprite = stubSprite();

        List<String> violations = new ArrayList<>();
        for (String name : new String[]{"dungeon_lectern", "dungeon_lectern_key"}) {
            BlockModel model = loadModel(name);
            int quadCount = 0;
            for (BlockElement element : model.getElements()) {
                for (Map.Entry<Direction, BlockElementFace> e : element.faces.entrySet()) {
                    BakedQuad quad = bakery.bakeQuad(
                            element.from, element.to, e.getValue(), sprite,
                            e.getKey(), IDENTITY, element.rotation, element.shade);
                    assertNotNull(quad, name + " 的 " + e.getKey() + " 面烘焙结果为 null");
                    quadCount++;
                    checkVertices(name, e.getKey().getName(), quad, violations);
                }
            }
            assertTrue(quadCount > 0, name + " 没烘出任何四边形，游戏里就是透明的");
        }

        assertTrue(violations.isEmpty(),
                "以下四边形顶点跑出了 0..16 方块体积，会让方块渲染异常（表现为透明）：\n"
                        + String.join("\n", violations));
    }

    /**
     * 顶点位置在 {@link BakedQuad#getVertices()} 里是 x,y,z 三连（后面跟 color/uv/…）。
     * 每 8 个 float 一个顶点，前 3 个是坐标。
     */
    private static void checkVertices(String model, String face, BakedQuad quad, List<String> violations) {
        int[] v = quad.getVertices();
        for (int i = 0; i < 4; i++) {
            float x = Float.intBitsToFloat(v[i * 8]);
            float y = Float.intBitsToFloat(v[i * 8 + 1]);
            float z = Float.intBitsToFloat(v[i * 8 + 2]);
            // 顶点是【归一化】的：原版 bakeQuad 内部把 0..16 除成了 0..1。
            // 容差取 0.01（即 0.16 像素），比原版烘焙的浮点误差大一两个数量级，
            // 又能抓住"角点翻出方块体积"这类真实越界。
            if (x < -0.01f || x > 1.01f || y < -0.01f || y > 1.01f || z < -0.01f || z > 1.01f) {
                violations.add(String.format(
                        "  %s 的 %s 面 顶点%d = (%.4f, %.4f, %.4f)（归一化坐标，应在 0..1）",
                        model, face, i, x, y, z));
            }
        }
    }

    private static BlockModel loadModel(String name) throws Exception {
        String path = ASSETS + "/models/block/" + name + ".json";
        try (InputStream in = DungeonLecternQuadBakeTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "找不到资源：" + path);
            JsonObject json = JsonParser.parseReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            return new BlockModel.Deserializer().deserialize(json, BlockModel.class, CONTEXT);
        }
    }
}
