package com.piranport.client.model.bedrock;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/** 从资源包读取 Bedrock 几何与动画。 */
public final class BedrockGeometryLoader {
    private BedrockGeometryLoader() {}

    public static BedrockGeometry loadGeometry(ResourceManager resourceManager, ResourceLocation location)
            throws IOException {
        Resource resource = resourceManager.getResourceOrThrow(location);
        try (Reader reader = resource.openAsReader()) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            return BedrockGeometry.parse(root);
        }
    }

    public static Map<String, BedrockAnimation> loadAnimations(ResourceManager resourceManager, ResourceLocation location)
            throws IOException {
        Resource resource = resourceManager.getResourceOrThrow(location);
        try (Reader reader = resource.openAsReader()) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject animations = root.getAsJsonObject("animations");
            if (animations == null) return Map.of();
            Map<String, BedrockAnimation> result = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : animations.entrySet()) {
                if (!entry.getValue().isJsonObject()) continue;
                result.put(entry.getKey(), BedrockAnimation.parse(entry.getKey(), entry.getValue().getAsJsonObject()));
            }
            return result;
        }
    }
}
