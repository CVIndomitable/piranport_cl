package com.piranport.client.model.bedrock;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 平海 YSM 模型资产回归：几何/动画能解析，采样结果在合理范围。 */
class BedrockModelAssetTest {
    private static JsonObject read(String path) throws Exception {
        try (Reader reader = new InputStreamReader(
                BedrockModelAssetTest.class.getResourceAsStream(path), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    @Test
    void geometryMatchesSourceAsset() throws Exception {
        BedrockGeometry geometry = BedrockGeometry.parse(read("/assets/piranport/geo/pinghai.geo.json"));
        assertEquals(53, geometry.bones.size());
        assertEquals(141, geometry.bones.stream().mapToInt(bone -> bone.cubes.size()).sum());
        assertEquals(128.0F, geometry.textureWidth);
        assertEquals(128.0F, geometry.textureHeight);
        assertNotNull(geometry.indexByName.get("Head"));
        assertNotNull(geometry.indexByName.get("LeftLeg"));
        assertFalse(geometry.roots.isEmpty());
    }

    @Test
    void animationsParseAndSample() throws Exception {
        BedrockGeometry geometry = BedrockGeometry.parse(read("/assets/piranport/geo/pinghai.geo.json"));
        JsonObject animationsJson = read("/assets/piranport/animations/pinghai.animation.json")
                .getAsJsonObject("animations");
        BedrockAnimation idle = BedrockAnimation.parse("idle",
                animationsJson.getAsJsonObject("idle"));
        BedrockAnimation walk = BedrockAnimation.parse("walk",
                animationsJson.getAsJsonObject("walk"));

        assertEquals(3.0F, idle.length, 1.0E-4F);
        assertTrue(idle.loop);
        assertEquals(1.0F, walk.length, 1.0E-4F);

        float[] poses = new float[geometry.bones.size() * 9];
        Arrays.fill(poses, 0.0F);
        for (int i = 6; i < poses.length; i += 9) {
            poses[i] = 1.0F;
            poses[i + 1] = 1.0F;
            poses[i + 2] = 1.0F;
        }
        BedrockMolang.Context context = new BedrockMolang.Context();
        context.animTime = 0.25F;
        walk.sample(0.25F, context, poses, geometry.indexByName);

        int leftLeg = geometry.indexByName.get("LeftLeg") * 9;
        assertTrue(Math.abs(poses[leftLeg]) > 0.01F, "行走动画应该让左腿产生 X 轴旋转");
        // 关键帧量级不能被 Molang 求值器放大成异常值
        for (float value : poses) {
            assertTrue(Float.isFinite(value));
            assertTrue(Math.abs(value) < 720.0F);
        }
    }
}
