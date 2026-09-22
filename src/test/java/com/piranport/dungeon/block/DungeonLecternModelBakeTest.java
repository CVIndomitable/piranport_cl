package com.piranport.dungeon.block;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.BlockModel;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 用原版反序列化器真解析两个讲台模型。
 *
 * <p>前面的 DungeonLecternModelTest 只查 JSON 结构，抓不到"原版拒绝这个模型"的坑
 * （rotation 必须是 22.5 整数倍就是典型）。这里直接调
 * {@link BlockModel.Deserializer}，跑的就是模型加载时的同一条路径。
 *
 * <p>{@code BlockModel.Deserializer#getElements} 内部会拿 context 去反序列化每个 element，
 * 所以必须喂一个真实的 {@link JsonDeserializationContext}——传 null 会 NPE，而 NPE 会被
 * 误报成"模型非法"。这里实现一个最小 context，按目标类型分发给原版的 element 反序列化器。
 *
 * <p>不碰方块注册表，因此不会触发 MC bootstrap。
 */
class DungeonLecternModelBakeTest {

    private static final String ASSETS = "/assets/piranport";

    /**
     * 最小反序列化上下文：只支持原版 {@link BlockElement} / {@link BlockElementFace}
     * ——这两个是 {@code BlockModel.Deserializer} 解析 elements 时会向下分发的类型。
     * 其余类型本测试用不到，返回 null。
     */
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

    @Test
    void bothModelsSurviveVanillaDeserializer() throws IOException {
        for (String name : new String[]{"dungeon_lectern", "dungeon_lectern_key"}) {
            JsonObject json = readJson(ASSETS + "/models/block/" + name + ".json");

            BlockModel model;
            try {
                model = new BlockModel.Deserializer().deserialize(
                        json, BlockModel.class, CONTEXT);
            } catch (Throwable t) {
                throw new AssertionError(
                        name + " 被原版反序列化器拒绝，游戏里会表现为方块透明："
                                + t.getClass().getSimpleName() + ": " + t.getMessage(), t);
            }
            assertNotNull(model, name + " 解析结果不应为 null");
            assertTrue(model.getElements().size() > 0, name + " 应解析出至少 1 个 element");
        }
    }

    private JsonObject readJson(String resourcePath) throws IOException {
        try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
            assertNotNull(in, "找不到资源：" + resourcePath);
            return JsonParser.parseReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
