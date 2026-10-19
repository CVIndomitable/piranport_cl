package com.piranport.aviation;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.piranport.PiranPort;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads immutable aircraft definitions from data/&lt;namespace&gt;/aircraft/*.json.
 *
 * <p>定义 ID = 物品注册名。文件 {@code data/piranport/aircraft/seafire.json}
 * 对应 {@code piranport:seafire}。加载使用 DFU Codec，不做任何旧格式兼容。
 */
@EventBusSubscriber(modid = PiranPort.MOD_ID)
public final class AircraftDefinitionReloadListener
        extends SimplePreparableReloadListener<Map<ResourceLocation, AircraftDefinition>> {

    @Override
    protected Map<ResourceLocation, AircraftDefinition> prepare(ResourceManager manager,
                                                                ProfilerFiller profiler) {
        Map<ResourceLocation, AircraftDefinition> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        for (Map.Entry<ResourceLocation, Resource> entry : manager.listResources("aircraft",
                location -> location.getPath().endsWith(".json")).entrySet()) {
            ResourceLocation resourceId = entry.getKey();
            ResourceLocation definitionId = definitionId(resourceId);
            try (BufferedReader reader = entry.getValue().openAsReader()) {
                JsonElement json = JsonParser.parseReader(reader);
                AircraftDefinition definition = AircraftDefinition.JsonDefinition.CODEC
                        .parse(JsonOps.INSTANCE, json)
                        .getOrThrow(message -> new IllegalArgumentException(
                                "invalid aircraft definition " + resourceId + ": " + message))
                        .withId(definitionId);
                if (result.put(definition.id(), definition) != null) {
                    errors.add("duplicate aircraft definition: " + definition.id()
                            + " (resource " + resourceId + ")");
                }
            } catch (Exception exception) {
                errors.add("failed to load " + resourceId + ": " + exception.getMessage());
            }
        }
        if (!errors.isEmpty()) {
            throw new IllegalStateException("aircraft definition load failed:\n - "
                    + String.join("\n - ", errors));
        }
        return result;
    }

    @Override
    protected void apply(Map<ResourceLocation, AircraftDefinition> definitions, ResourceManager manager,
                         ProfilerFiller profiler) {
        AircraftCatalog.rebuild(definitions);
        AircraftDefinitionService.replaceAll(definitions);
        PiranPort.LOGGER.info("Loaded {} aircraft definition(s)", definitions.size());
    }

    /** data/<ns>/aircraft/<path>.json → <ns>:<path>（装备 ID 与物品注册名一致）。 */
    static ResourceLocation definitionId(ResourceLocation resourceId) {
        String path = resourceId.getPath();
        String prefix = "aircraft/";
        if (!path.startsWith(prefix) || !path.endsWith(".json")) {
            throw new IllegalArgumentException("not an aircraft definition resource: " + resourceId);
        }
        String name = path.substring(prefix.length(), path.length() - ".json".length());
        return ResourceLocation.fromNamespaceAndPath(resourceId.getNamespace(), name);
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new AircraftDefinitionReloadListener());
    }
}
