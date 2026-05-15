package com.piranport.artillery.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.util.HashMap;
import java.util.Map;

/** 加载 data/piranport/artillery/cannons/ 下的火炮 JSON 配置 */
@EventBusSubscriber(modid = PiranPort.MOD_ID)
public class ArtilleryConfig extends SimplePreparableReloadListener<Map<String, ArtilleryCannonData>> {
    private static final Gson GSON = new GsonBuilder().create();
    private static final String PATH_PREFIX = "data/piranport/artillery/cannons/";

    private static final Map<String, ArtilleryCannonData> CANNON_DATA = new HashMap<>();

    @Override
    protected Map<String, ArtilleryCannonData> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<String, ArtilleryCannonData> result = new HashMap<>();
        for (Map.Entry<ResourceLocation, Resource> entry : manager.listResources("artillery/cannons",
                loc -> loc.getPath().endsWith(".json")).entrySet()) {
            ResourceLocation loc = entry.getKey();
            String name = loc.getPath().replaceFirst(".*artillery/cannons/", "").replace(".json", "");
            try (BufferedReader reader = entry.getValue().openAsReader()) {
                ArtilleryCannonData data = GSON.fromJson(reader, ArtilleryCannonData.class);
                if (data != null) {
                    result.put(name, data);
                }
            } catch (Exception e) {
                PiranPort.LOGGER.warn("Failed to load artillery config: {}", loc, e);
            }
        }
        return result;
    }

    @Override
    protected void apply(Map<String, ArtilleryCannonData> data, ResourceManager manager, ProfilerFiller profiler) {
        CANNON_DATA.clear();
        CANNON_DATA.putAll(data);
        PiranPort.LOGGER.info("Loaded {} artillery cannon config(s)", data.size());
    }

    public static ArtilleryCannonData get(String name) {
        return CANNON_DATA.getOrDefault(name, ArtilleryCannonData.DEFAULT);
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ArtilleryConfig());
    }
}
