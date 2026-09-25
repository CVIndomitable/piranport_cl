package com.piranport.client;

import com.piranport.PiranPort;
import com.piranport.aviation.AircraftDefinition;
import com.piranport.aviation.AircraftDefinitionService;
import com.piranport.component.AircraftInfo;
import com.piranport.entity.AircraftEntity;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Client-side mapping from an aircraft definition's visual id to the baked
 * model and texture used by {@link AircraftRenderer}.
 *
 * <p>The registry deliberately contains model kinds rather than model
 * instances. Models are baked by the renderer provider after layer
 * registration, while this class remains safe to query during client-side
 * entity rendering and unit tests.</p>
 */
public final class AircraftVisualRegistry {
    public static final String B25_VISUAL_ID = "b25";
    public static final String F4F_VISUAL_ID = "f4f";

    private static final ResourceLocation B25_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "textures/entity/b25.png");
    private static final ResourceLocation F4F_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "textures/entity/f4f.png");

    private static final AircraftVisual B25 =
            new AircraftVisual(B25_VISUAL_ID, ModelKind.B25, B25_TEXTURE);
    private static final AircraftVisual F4F =
            new AircraftVisual(F4F_VISUAL_ID, ModelKind.F4F, F4F_TEXTURE);

    /**
     * Aliases preserve the visual output of entities saved before visual IDs
     * were introduced. Any unregistered id intentionally resolves to F4F.
     */
    private static final Map<String, AircraftVisual> VISUALS = Map.of(
            B25_VISUAL_ID, B25,
            "level_bomber", B25,
            F4F_VISUAL_ID, F4F,
            "fighter", F4F);

    private AircraftVisualRegistry() {}

    /**
     * Resolves a data-defined visual id. Namespaced IDs from JSON are accepted
     * when they use this mod's namespace; unknown IDs use the safe F4F visual.
     */
    public static AircraftVisual resolve(String visualId) {
        return VISUALS.getOrDefault(normalize(visualId), F4F);
    }

    /** Resolves the visual declared by an immutable aircraft definition. */
    public static AircraftVisual resolve(AircraftDefinition definition) {
        return definition == null ? F4F : resolve(definition.visualId());
    }

    /**
     * Resolves an entity's visual definition on the client. Resource-backed
     * definitions may be unavailable in a client-only resource view, so the
     * old LEVEL_BOMBER type is retained as a compatibility fallback. When a
     * definition is present, its visual id always wins and unknown values
     * therefore fall back to F4F as required.
     */
    public static AircraftVisual resolve(AircraftEntity entity) {
        if (entity == null) return F4F;

        String definitionId = entity.getAircraftDefinitionId();
        AircraftDefinition definition = AircraftDefinitionService.find(definitionId);
        if (definition == null && definitionId != null && !definitionId.isBlank()) {
            String canonicalId = AircraftDefinitionService.canonicalResourceId(definitionId);
            if (!canonicalId.equals(definitionId)) {
                definition = AircraftDefinitionService.find(canonicalId);
            }
        }
        if (definition != null) return resolve(definition);

        // Old entities have no definition snapshot on the client. Preserve
        // the pre-refactor B25 rendering until those entities are replaced.
        return resolveLegacyType(entity.getAircraftType());
    }

    /** Returns the fallback visual used for legacy entities without a definition. */
    public static AircraftVisual resolveLegacyType(AircraftInfo.AircraftType aircraftType) {
        return aircraftType == AircraftInfo.AircraftType.LEVEL_BOMBER ? B25 : F4F;
    }

    public enum ModelKind {
        B25,
        F4F
    }

    public record AircraftVisual(String id, ModelKind modelKind, ResourceLocation texture) {
        public AircraftVisual {
            id = normalize(id);
            Objects.requireNonNull(modelKind, "modelKind");
            Objects.requireNonNull(texture, "texture");
        }
    }

    private static String normalize(String visualId) {
        if (visualId == null || visualId.isBlank()) return "";
        String value = visualId.trim().toLowerCase(Locale.ROOT);
        ResourceLocation parsed = ResourceLocation.tryParse(value);
        if (parsed != null && PiranPort.MOD_ID.equals(parsed.getNamespace())) {
            return parsed.getPath();
        }
        return value;
    }
}
