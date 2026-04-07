package com.piranport.npc.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.piranport.PiranPort;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads deep ocean NPC configuration from data/piranport/npc/deep_ocean/ JSON files.
 */
public class DeepOceanDataLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();

    private static final Map<String, DeepOceanData> REGISTRY = new HashMap<>();

    public DeepOceanDataLoader() {
        super(GSON, "npc");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources,
                         ResourceManager resourceManager, ProfilerFiller profiler) {
        REGISTRY.clear();

        for (var entry : resources.entrySet()) {
            ResourceLocation id = entry.getKey();
            String path = id.getPath();

            // Only process deep_ocean/ subdirectory
            if (!path.startsWith("deep_ocean/")) continue;

            try {
                JsonObject json = entry.getValue().getAsJsonObject();
                DeepOceanData data = parseDeepOceanData(json);
                REGISTRY.put(data.entityId(), data);
            } catch (Exception e) {
                PiranPort.LOGGER.warn("Failed to parse deep ocean NPC data {}: {}", id, e.getMessage());
            }
        }

        PiranPort.LOGGER.info("Loaded {} deep ocean NPC configurations", REGISTRY.size());
    }

    private DeepOceanData parseDeepOceanData(JsonObject json) {
        String entityId = json.get("entity_id").getAsString();
        double health = json.has("health") ? json.get("health").getAsDouble() : 50;
        double armor = json.has("armor") ? json.get("armor").getAsDouble() : 8;
        double speed = json.has("speed") ? json.get("speed").getAsDouble() : 0.25;
        double detectionRange = json.has("detection_range") ? json.get("detection_range").getAsDouble() : 32;
        double orbitDistance = json.has("orbit_distance") ? json.get("orbit_distance").getAsDouble() : 16;

        List<String> weapons = new ArrayList<>();
        if (json.has("weapons")) {
            JsonArray arr = json.getAsJsonArray("weapons");
            for (JsonElement e : arr) {
                weapons.add(e.getAsString());
            }
        }

        String lootTable = json.has("loot_table") ? json.get("loot_table").getAsString() : "";
        int trackingMin = json.has("tracking_interval_min") ? json.get("tracking_interval_min").getAsInt() : 3;
        int trackingMax = json.has("tracking_interval_max") ? json.get("tracking_interval_max").getAsInt() : 6;
        int fireInterval = json.has("fire_interval") ? json.get("fire_interval").getAsInt() : 80;
        float shellDamage = json.has("shell_damage") ? json.get("shell_damage").getAsFloat() : 5.0f;
        float explosionPower = json.has("explosion_power") ? json.get("explosion_power").getAsFloat() : 1.5f;

        return new DeepOceanData(entityId, health, armor, speed, detectionRange, orbitDistance,
                List.copyOf(weapons), lootTable, trackingMin, trackingMax, fireInterval,
                shellDamage, explosionPower);
    }

    /**
     * Get data for a specific entity type.
     */
    public static DeepOceanData getData(String entityId) {
        return REGISTRY.getOrDefault(entityId, DeepOceanData.defaults(entityId));
    }

    /**
     * Get all loaded data (for debug).
     */
    public static Map<String, DeepOceanData> getAll() {
        return Collections.unmodifiableMap(REGISTRY);
    }
}
