package com.piranport.aviation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.piranport.PiranPort;
import com.piranport.component.AircraftInfo;
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

/** Loads immutable aircraft definitions from data/&lt;namespace&gt;/aircraft/*.json. */
@EventBusSubscriber(modid = PiranPort.MOD_ID)
public final class AircraftDefinitionReloadListener
        extends SimplePreparableReloadListener<Map<String, AircraftDefinition>> {
    private static final Gson GSON = new GsonBuilder().create();

    @Override
    protected Map<String, AircraftDefinition> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<String, AircraftDefinition> result = new HashMap<>();
        for (Map.Entry<ResourceLocation, Resource> entry : manager.listResources("aircraft",
                location -> location.getPath().endsWith(".json")).entrySet()) {
            ResourceLocation resourceId = entry.getKey();
            String definitionId = AircraftDefinitionService.canonicalResourceId(resourceId.toString()
                    .substring(0, resourceId.toString().length() - ".json".length()));
            try (BufferedReader reader = entry.getValue().openAsReader()) {
                JsonAircraftDefinition json = GSON.fromJson(reader, JsonAircraftDefinition.class);
                AircraftDefinition definition = json.toDefinition(definitionId);
                if (result.put(definition.id(), definition) != null) {
                    PiranPort.LOGGER.warn("Duplicate aircraft definition {} from resource {}",
                            definition.id(), resourceId);
                }
            } catch (Exception exception) {
                PiranPort.LOGGER.error("Failed to load aircraft definition {}", resourceId, exception);
            }
        }
        return result;
    }

    @Override
    protected void apply(Map<String, AircraftDefinition> definitions, ResourceManager manager,
                         ProfilerFiller profiler) {
        AircraftDefinitionService.replaceAll(definitions);
        PiranPort.LOGGER.info("Loaded {} aircraft definition(s)", definitions.size());
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new AircraftDefinitionReloadListener());
    }

    /** Gson DTO keeps resource parsing independent from the immutable domain record. */
    static record JsonAircraftDefinition(
            String aircraft_class,
            String attack_profile,
            String payload_type,
            String visual_id,
            int fuel_capacity,
            int ammo_capacity,
            float panel_damage,
            float panel_speed,
            int weight,
            String bombing_mode
    ) {
        AircraftDefinition toDefinition(String id) {
            return new AircraftDefinition(
                    id,
                    enumValue(aircraft_class, AircraftInfo.AircraftType.class, "aircraft_class", id),
                    enumValue(attack_profile, AircraftDefinition.AttackProfile.class, "attack_profile", id),
                    enumValue(payload_type, AircraftDefinition.PayloadType.class, "payload_type", id),
                    visual_id,
                    fuel_capacity,
                    ammo_capacity,
                    panel_damage,
                    panel_speed,
                    weight,
                    enumValue(bombing_mode, AircraftInfo.BombingMode.class, "bombing_mode", id));
        }

        private static <E> E enumValue(String value, Class<E> type, String field, String id) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(field + " is required for " + id);
            }
            String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
            for (E constant : type.getEnumConstants()) {
                String name = ((Enum<?>) constant).name().toLowerCase(java.util.Locale.ROOT);
                if (name.equals(normalized)) return constant;
                if (constant instanceof AircraftInfo.AircraftType aircraft
                        && aircraft.getSerializedName().equals(normalized)) return constant;
                if (constant instanceof AircraftInfo.BombingMode bombing
                        && bombing.getSerializedName().equals(normalized)) return constant;
                if (constant instanceof AircraftDefinition.AttackProfile profile
                        && profile.id().equals(normalized)) return constant;
                if (constant instanceof AircraftDefinition.PayloadType payload
                        && (payload.registryName().equals(normalized)
                        || payload.registryName().substring(payload.registryName().indexOf(':') + 1)
                        .equals(normalized))) return constant;
            }
            throw new IllegalArgumentException("unknown " + field + " '" + value + "' for " + id);
        }
    }
}
