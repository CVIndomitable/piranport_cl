package com.piranport.client;

import com.piranport.PiranPort;
import com.piranport.aviation.AircraftVisualIds;
import com.piranport.entity.AircraftEntity;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Objects;

/**
 * 客户端视觉 ID → 模型层/贴图映射。
 *
 * <p>视觉 ID 的白名单在 {@link AircraftVisualIds}（服务端数据加载也用它做硬校验）。
 * 本类只负责映射，不再根据机种猜模型；未知 ID 会显式记录错误并使用占位视觉，
 * 数据加载阶段已经会先报错，因此正常运行不会走到这里。
 */
public final class AircraftVisualRegistry {
    private static final ResourceLocation B25_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "textures/entity/b25.png");
    private static final ResourceLocation F4F_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "textures/entity/f4f.png");
    private static final ResourceLocation BARRACUDA_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "textures/entity/barracuda.png");
    private static final ResourceLocation RYUSEI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "textures/entity/ryusei.png");
    private static final ResourceLocation TENGA_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "textures/entity/tenga.png");

    private static final ResourceLocation JU87C_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "textures/entity/ju87c.png");

    private static final ResourceLocation BTD_DESTROYER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "textures/entity/btd_destroyer.png");

    private static final ResourceLocation WYVERN_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "textures/entity/wyvern.png");

    private static final ResourceLocation SKYRAIDER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "textures/entity/skyraider.png");

    private static final ResourceLocation F4U7_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "textures/entity/f4u7.png");

    private static final ResourceLocation TYPE96_FIGHTER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "textures/entity/type96_fighter.png");

    private static final ResourceLocation F2A_BUFFALO_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "textures/entity/f2a_buffalo.png");

    private static final ResourceLocation REPPU_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "textures/entity/reppu.png");

    private static final ResourceLocation SEAFANG_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "textures/entity/seafang.png");

    private static final AircraftVisual B25 =
            new AircraftVisual(AircraftVisualIds.B25, ModelKind.B25, B25_TEXTURE);
    private static final AircraftVisual F4F =
            new AircraftVisual(AircraftVisualIds.F4F, ModelKind.F4F, F4F_TEXTURE);
    private static final AircraftVisual BARRACUDA =
            new AircraftVisual(AircraftVisualIds.BARRACUDA, ModelKind.BARRACUDA, BARRACUDA_TEXTURE);
    private static final AircraftVisual RYUSEI =
            new AircraftVisual(AircraftVisualIds.RYUSEI, ModelKind.RYUSEI, RYUSEI_TEXTURE);
    private static final AircraftVisual TENGA =
            new AircraftVisual(AircraftVisualIds.TENGA, ModelKind.TENGA, TENGA_TEXTURE);

    private static final AircraftVisual JU87C =
            new AircraftVisual(AircraftVisualIds.JU87C, ModelKind.JU87C, JU87C_TEXTURE);

    private static final AircraftVisual BTD_DESTROYER =
            new AircraftVisual(AircraftVisualIds.BTD_DESTROYER, ModelKind.BTD_DESTROYER, BTD_DESTROYER_TEXTURE);

    private static final AircraftVisual WYVERN =
            new AircraftVisual(AircraftVisualIds.WYVERN, ModelKind.WYVERN, WYVERN_TEXTURE);

    private static final AircraftVisual SKYRAIDER =
            new AircraftVisual(AircraftVisualIds.SKYRAIDER, ModelKind.SKYRAIDER, SKYRAIDER_TEXTURE);

    private static final AircraftVisual F4U7 =
            new AircraftVisual(AircraftVisualIds.F4U7, ModelKind.F4U7, F4U7_TEXTURE);

    private static final AircraftVisual TYPE96_FIGHTER =
            new AircraftVisual(AircraftVisualIds.TYPE96_FIGHTER, ModelKind.TYPE96_FIGHTER, TYPE96_FIGHTER_TEXTURE);

    private static final AircraftVisual F2A_BUFFALO =
            new AircraftVisual(AircraftVisualIds.F2A_BUFFALO, ModelKind.F2A_BUFFALO, F2A_BUFFALO_TEXTURE);

    private static final AircraftVisual REPPU =
            new AircraftVisual(AircraftVisualIds.REPPU, ModelKind.REPPU, REPPU_TEXTURE);

    private static final AircraftVisual SEAFANG =
            new AircraftVisual(AircraftVisualIds.SEAFANG, ModelKind.SEAFANG, SEAFANG_TEXTURE);

    // 美术占位：待替换为正式模型与贴图。
    private static final AircraftVisual PLACEHOLDER =
            new AircraftVisual(AircraftVisualIds.PLACEHOLDER, ModelKind.F4F, B25_TEXTURE);

    private static final Map<String, AircraftVisual> VISUALS = Map.ofEntries(
            Map.entry(AircraftVisualIds.B25, B25),
            Map.entry(AircraftVisualIds.LEVEL_BOMBER, B25),
            Map.entry(AircraftVisualIds.DIVE_BOMBER, B25),
            Map.entry(AircraftVisualIds.TORPEDO_BOMBER, B25),
            Map.entry(AircraftVisualIds.ASW, B25),
            Map.entry(AircraftVisualIds.F4F, F4F),
            Map.entry(AircraftVisualIds.SEAFANG, SEAFANG),
            Map.entry(AircraftVisualIds.REPPU, REPPU),
            Map.entry(AircraftVisualIds.F2A_BUFFALO, F2A_BUFFALO),
            Map.entry(AircraftVisualIds.TYPE96_FIGHTER, TYPE96_FIGHTER),
            Map.entry(AircraftVisualIds.F4U7, F4U7),
            Map.entry(AircraftVisualIds.SKYRAIDER, SKYRAIDER),
            Map.entry(AircraftVisualIds.WYVERN, WYVERN),
            Map.entry(AircraftVisualIds.BTD_DESTROYER, BTD_DESTROYER),
            Map.entry(AircraftVisualIds.JU87C, JU87C),
            Map.entry(AircraftVisualIds.BARRACUDA, BARRACUDA),
            Map.entry(AircraftVisualIds.RYUSEI, RYUSEI),
            Map.entry(AircraftVisualIds.TENGA, TENGA),
            Map.entry(AircraftVisualIds.FIGHTER, F4F),
            Map.entry(AircraftVisualIds.ROCKET_FIGHTER, F4F),
            Map.entry(AircraftVisualIds.RECON, F4F),
            Map.entry(AircraftVisualIds.PLACEHOLDER, PLACEHOLDER));

    private AircraftVisualRegistry() {}

    /** 解析视觉 ID；未知 ID 返回显式占位 F4F 并记录错误。 */
    public static AircraftVisual resolve(String visualId) {
        String normalized = AircraftVisualIds.normalize(visualId);
        AircraftVisual visual = VISUALS.get(normalized);
        if (visual != null) return visual;
        PiranPort.LOGGER.error("Unknown aircraft visual_id '{}'; using placeholder {}", visualId,
                AircraftVisualIds.F4F);
        return F4F;
    }

    public static boolean isRegistered(String visualId) {
        return VISUALS.containsKey(AircraftVisualIds.normalize(visualId));
    }

    /**
     * 实体渲染入口：只读同步过来的 visual_id，不依赖客户端是否加载了服务端数据包。
     */
    public static AircraftVisual resolve(AircraftEntity entity) {
        return entity == null ? F4F : resolve(entity.getVisualId());
    }

    public enum ModelKind {
        B25,
        F4F,
        BARRACUDA,
        RYUSEI,
        TENGA,
        JU87C,
        BTD_DESTROYER,
        WYVERN,
        SKYRAIDER,
        F4U7,
        TYPE96_FIGHTER,
        F2A_BUFFALO,
        REPPU,
        SEAFANG
    }

    public record AircraftVisual(String id, ModelKind modelKind, ResourceLocation texture) {
        public AircraftVisual {
            id = AircraftVisualIds.normalize(id);
            Objects.requireNonNull(modelKind, "modelKind");
            Objects.requireNonNull(texture, "texture");
        }
    }
}
