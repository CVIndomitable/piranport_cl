package com.piranport.combat.cannon.ammo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.piranport.PiranPort;
import com.piranport.combat.cannon.CannonAmmoRules;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.io.BufferedReader;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Loads optional ammo definitions from data/&lt;namespace&gt;/ammo/*.json. */
@EventBusSubscriber(modid = PiranPort.MOD_ID)
public final class AmmoDefinitionReloadListener
        extends SimplePreparableReloadListener<Map<ResourceLocation, AmmoDefinition>> {
    private static final Gson GSON = new GsonBuilder().create();

    @Override
    protected Map<ResourceLocation, AmmoDefinition> prepare(ResourceManager manager,
                                                              ProfilerFiller profiler) {
        Map<ResourceLocation, AmmoDefinition> result = new LinkedHashMap<>();
        AmmoDefinitionService.allInOrder().forEach(definition ->
                result.put(definition.itemId(), definition));
        for (Map.Entry<ResourceLocation, Resource> entry : manager.listResources("ammo",
                location -> location.getPath().endsWith(".json")).entrySet()) {
            ResourceLocation resourceId = entry.getKey();
            String path = resourceId.getPath();
            String itemPath = path.substring(0, path.length() - ".json".length());
            ResourceLocation itemId = ResourceLocation.fromNamespaceAndPath(
                    resourceId.getNamespace(), itemPath);
            try (BufferedReader reader = entry.getValue().openAsReader()) {
                JsonAmmoDefinition json = GSON.fromJson(reader, JsonAmmoDefinition.class);
                AmmoDefinition definition = json.toDefinition(itemId);
                result.put(definition.itemId(), definition);
            } catch (Exception exception) {
                PiranPort.LOGGER.error("Failed to load ammo definition {}", resourceId, exception);
            }
        }
        return result;
    }

    @Override
    protected void apply(Map<ResourceLocation, AmmoDefinition> definitions,
                         ResourceManager manager, ProfilerFiller profiler) {
        AmmoDefinitionService.replaceAll(definitions);
        PiranPort.LOGGER.info("Loaded {} cannon ammo definition(s)", definitions.size());
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new AmmoDefinitionReloadListener());
    }

    /** Gson DTO isolates resource parsing from the immutable domain record. */
    static record JsonAmmoDefinition(
            String behavior,
            String caliber_family,
            Float damage_multiplier,
            Float explosion_multiplier,
            Float armor_ignore,
            Boolean underwater_explosion,
            String impact_kind
    ) {
        AmmoDefinition toDefinition(ResourceLocation itemId) {
            AmmoBehavior resolvedBehavior = enumValue(behavior, AmmoBehavior.class, "behavior", itemId);
            CannonAmmoRules.CaliberFamily family = caliber_family == null || caliber_family.isBlank()
                    ? null
                    : enumValue(caliber_family, CannonAmmoRules.CaliberFamily.class,
                    "caliber_family", itemId);
            AmmoBehaviorStrategy strategy = AmmoBehaviorStrategy.require(resolvedBehavior);
            float damage = damage_multiplier != null ? damage_multiplier : 1.0F;
            float explosion = explosion_multiplier != null ? explosion_multiplier : 1.0F;
            float armor = armor_ignore != null ? armor_ignore
                    : (resolvedBehavior == AmmoBehavior.AP ? 0.5F : 0.0F);
            boolean underwater = underwater_explosion != null
                    ? underwater_explosion : strategy.explodesUnderwater();
            AmmoBehaviorStrategy.ImpactKind impact = impact_kind == null || impact_kind.isBlank()
                    ? strategy.impactKind()
                    : enumValue(impact_kind, AmmoBehaviorStrategy.ImpactKind.class,
                    "impact_kind", itemId);
            return new AmmoDefinition(itemId, resolvedBehavior,
                    java.util.Optional.ofNullable(family), damage, explosion, armor,
                    underwater, java.util.Optional.of(impact));
        }

        private static <E> E enumValue(String value, Class<E> type, String field,
                                       ResourceLocation itemId) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(field + " is required for " + itemId);
            }
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            for (E constant : type.getEnumConstants()) {
                if (((Enum<?>) constant).name().toLowerCase(Locale.ROOT).equals(normalized)) {
                    return constant;
                }
                if (constant instanceof CannonAmmoRules.CaliberFamily family
                        && family.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                    return constant;
                }
                if (constant instanceof AmmoBehaviorStrategy.ImpactKind impact
                        && impact.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                    return constant;
                }
            }
            throw new IllegalArgumentException("unknown " + field + " '" + value
                    + "' for " + itemId);
        }
    }
}
