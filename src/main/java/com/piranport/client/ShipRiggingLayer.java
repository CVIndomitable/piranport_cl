package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.piranport.client.gecko.RiggingModelRegistry;
import com.piranport.combat.TransformationManager;
import com.piranport.rigging.RiggingLoadout;
import com.piranport.rigging.RiggingMount;
import com.piranport.skin.ClientSkinData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix4f;

/**
 * 玩家舰装渲染层：程序化舰体轮廓 + 按挂点摆放的武器占位模型。
 *
 * <p><b>数据来源</b>：挂点分配由服务端算好放进 {@link com.piranport.registry.ModAttachmentTypes#RIGGING_LOADOUT}，
 * 所有看得见该玩家的客户端都只读这份快照 —— 远程玩家的背包客户端根本拿不到。
 * 只有本地玩家在快照到达之前（进服第一帧 / 单人模式）才用
 * {@link com.piranport.rigging.RiggingLoadoutResolver} 现场兜底。
 *
 * <p><b>Phase 范围</b>：只做视觉与数据，不改弹道起点、不隐藏手持武器（Phase 2/3 再接）。
 */
public class ShipRiggingLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private enum HullProfile {
        DEFAULT(RiggingLoadout.HULL_DEFAULT),
        CARRIER(RiggingLoadout.HULL_CARRIER),
        DESTROYER(RiggingLoadout.HULL_DESTROYER),
        CRUISER(RiggingLoadout.HULL_CRUISER),
        SUBMARINE(RiggingLoadout.HULL_SUBMARINE);

        private final int id;

        HullProfile(int id) {
            this.id = id;
        }

        static HullProfile fromId(int id) {
            for (HullProfile profile : values()) {
                if (profile.id == id) return profile;
            }
            return DEFAULT;
        }
    }

    private enum RiggingMotif {
        J_BOW,
        C_TALISMAN,
        G_ARRAY,
        I_CATAPULT,
        E_LONGBOW,
        F_RAPIER,
        U_FUSILIER,
        NONE
    }

    public ShipRiggingLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible()) return;
        if (!RiggingClientState.isRenderedAsTransformed(player)) return;

        RiggingLoadout loadout = RiggingClientState.resolveLoadout(player);
        int skinId = AircraftLaunchPoseClientState.skinId(
                player.getId(), ClientSkinData.getActiveSkin(player.getUUID()));
        float launchPose = AircraftLaunchPoseClientState.intensity(player.getId(), partialTick);

        renderHull(poseStack, bufferSource, packedLight, player, loadout, skinId, launchPose);
        for (RiggingMount mount : loadout.mounts()) {
            // 已有 GeckoLib 模型的装备由 RiggingAttachedLayer 绘制，这里不能再画一份物品模型。
            if (RiggingModelRegistry.isRegistered(mount.stack())) continue;
            renderMount(poseStack, bufferSource, packedLight, player, mount, launchPose);
        }
    }

    private void renderHull(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                            AbstractClientPlayer player, RiggingLoadout loadout, int skinId, float launchPose) {
        poseStack.pushPose();
        this.getParentModel().body.translateAndRotate(poseStack);
        poseStack.translate(0.0, 0.28 + launchPose * 0.035, 0.34 - launchPose * 0.035);
        poseStack.mulPose(Axis.XP.rotationDegrees(-3.0f * launchPose));

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.translucent());
        Matrix4f pose = poseStack.last().pose();
        // 旧 hull/motif 的坐标是「格」；新增的烟囱/舷侧/甲板按玩家模型骨骼习惯用
        // 1/16 格为单位书写。两者不能共用同一矩阵，否则新结构会被放大约 16 倍。
        Matrix4f pose16 = new Matrix4f(pose).scale(1.0f / 16.0f, 1.0f / 16.0f, 1.0f / 16.0f);

        HullProfile profile = HullProfile.fromId(loadout.hullProfile());
        float[] accent = accentColor(skinId);

        renderCuboid(consumer, pose,
                -0.55f, -0.08f, -0.10f, 0.55f, 0.07f, 0.16f,
                0.10f, 0.12f, 0.15f, 0.82f, packedLight);

        // 通用上层建筑：烟囱/桅杆永远在躯干后上方；左舷结构常驻。
        // 右舷在有飞机时整侧换飞行甲板，否则画常规舷侧结构。
        renderBackSuperstructure(consumer, pose16, accent, launchPose, packedLight);
        renderSideSponson(consumer, pose16, accent, 1.0f, launchPose, packedLight);
        if (loadout.hasAircraft()) {
            renderFlightDeck(consumer, pose16, accent, launchPose, packedLight);
        } else {
            renderSideSponson(consumer, pose16, accent, -1.0f, launchPose, packedLight);
        }

        switch (profile) {
            case CARRIER -> renderCarrierHull(consumer, pose, accent, packedLight);
            case DESTROYER -> renderDestroyerHull(consumer, pose, accent, packedLight);
            case CRUISER -> renderCruiserHull(consumer, pose, accent, packedLight);
            case SUBMARINE -> renderSubmarineHull(consumer, pose, accent, packedLight);
            case DEFAULT -> renderDefaultHull(consumer, pose, accent, loadout.hasAircraft(), packedLight);
        }
        renderSkinMotif(consumer, pose, resolveMotif(skinId, resolveNationality(player)),
                accent, launchPose, packedLight);
        renderLaunchPulse(consumer, pose, accent, launchPose, packedLight);

        poseStack.popPose();
    }

    /**
     * 从客户端玩家变身核心读取国籍字段（决策 §航空/22）。
     * 客户端不持有 ShipType 实例的字段（仅在 ShipCoreItem 上），直接通过
     * {@code TransformationManager.findTransformedCore} → {@code ShipCoreItem.getShipType().nationality}。
     * 失败 / 非变身状态 / null 核心 → 返回 null（由调用方走 skinId fallback）。
     */
    private static String resolveNationality(AbstractClientPlayer player) {
        if (player == null) return null;
        try {
            var core = TransformationManager.findTransformedCore(player);
            if (core == null || core.isEmpty()) return null;
            if (core.getItem() instanceof com.piranport.item.ShipCoreItem sci) {
                return sci.getShipType().nationality;
            }
        } catch (Exception ignored) {
            // 客户端可能在异次元（非变身态）调用，安全降级
        }
        return null;
    }

    private static float[] accentColor(int skinId) {
        return switch (skinId) {
            case 4, 8, 9, 10, 11, 13 -> new float[] { 0.70f, 0.55f, 0.42f };
            case 5, 6, 7, 14, 15 -> new float[] { 0.42f, 0.62f, 0.72f };
            case 12, 16, 17 -> new float[] { 0.55f, 0.46f, 0.70f };
            case 18, 19, 20, 21, 22 -> new float[] { 0.58f, 0.68f, 0.88f };
            default -> new float[] { 0.38f, 0.55f, 0.68f };
        };
    }

    private static RiggingMotif resolveMotif(int skinId, String nationality) {
        if (nationality != null) {
            return switch (nationality) {
                case "J" -> RiggingMotif.J_BOW;
                case "E" -> RiggingMotif.E_LONGBOW;
                case "U" -> RiggingMotif.U_FUSILIER;
                default -> resolveMotifBySkinId(skinId);
            };
        }
        return resolveMotifBySkinId(skinId);
    }

    private static RiggingMotif resolveMotifBySkinId(int skinId) {
        return switch (skinId) {
            case 4, 8, 9, 11, 13 -> RiggingMotif.J_BOW;
            case 5, 6, 7, 14, 15 -> RiggingMotif.C_TALISMAN;
            case 12, 17 -> RiggingMotif.G_ARRAY;
            case 10, 16 -> RiggingMotif.I_CATAPULT;
            case 18, 22 -> RiggingMotif.E_LONGBOW;
            case 19 -> RiggingMotif.F_RAPIER;
            case 20, 21 -> RiggingMotif.U_FUSILIER;
            default -> RiggingMotif.NONE;
        };
    }

    private static void renderSkinMotif(VertexConsumer consumer, Matrix4f pose, RiggingMotif motif,
                                        float[] accent, float launchPose, int packedLight) {
        switch (motif) {
            case J_BOW -> renderBowMotif(consumer, pose, accent, launchPose, packedLight);
            case C_TALISMAN -> renderTalismanMotif(consumer, pose, accent, launchPose, packedLight);
            case G_ARRAY -> renderArrayMotif(consumer, pose, accent, launchPose, packedLight);
            case I_CATAPULT -> renderCatapultMotif(consumer, pose, accent, launchPose, packedLight);
            case E_LONGBOW -> renderLongbowMotif(consumer, pose, accent, launchPose, packedLight);
            case F_RAPIER -> renderRapierMotif(consumer, pose, accent, launchPose, packedLight);
            case U_FUSILIER -> renderFusilierMotif(consumer, pose, accent, launchPose, packedLight);
            case NONE -> {
            }
        }
    }

    private static void renderBowMotif(VertexConsumer consumer, Matrix4f pose, float[] accent,
                                       float launchPose, int light) {
        renderCuboid(consumer, pose,
                -0.62f, 0.18f, 0.20f, -0.50f, 0.56f, 0.25f,
                0.94f, 0.84f, 0.62f, 0.78f, light);
        renderCuboid(consumer, pose,
                0.50f, 0.18f, 0.20f, 0.62f, 0.56f, 0.25f,
                0.94f, 0.84f, 0.62f, 0.78f, light);
        renderCuboid(consumer, pose,
                -0.50f, 0.35f - launchPose * 0.055f, 0.215f + launchPose * 0.055f,
                0.50f, 0.39f - launchPose * 0.055f, 0.245f + launchPose * 0.055f,
                accent[0], accent[1], accent[2], 0.68f, light);
        if (launchPose > 0.02f) {
            renderCuboid(consumer, pose,
                    -0.08f, 0.35f - launchPose * 0.050f, 0.245f + launchPose * 0.055f,
                    0.08f, 0.39f - launchPose * 0.050f, 0.37f + launchPose * 0.080f,
                    1.00f, 0.94f, 0.70f, 0.44f * launchPose, light);
        }
    }

    private static void renderTalismanMotif(VertexConsumer consumer, Matrix4f pose, float[] accent,
                                            float launchPose, int light) {
        for (int i = 0; i < 4; i++) {
            float x = -0.33f + i * 0.22f;
            float lift = launchPose * (0.025f + i * 0.012f);
            renderCuboid(consumer, pose,
                    x, 0.18f + lift, 0.20f, x + 0.08f, 0.42f + lift, 0.245f,
                    0.86f, 0.94f, 1.00f, 0.56f, light);
            renderCuboid(consumer, pose,
                    x + 0.015f, 0.24f + lift, 0.246f, x + 0.065f, 0.27f + lift, 0.275f,
                    accent[0], accent[1], accent[2], 0.78f, light);
        }
    }

    private static void renderArrayMotif(VertexConsumer consumer, Matrix4f pose, float[] accent,
                                         float launchPose, int light) {
        for (int i = 0; i < 3; i++) {
            float y = 0.18f + i * 0.10f;
            renderCuboid(consumer, pose,
                    -0.42f - launchPose * 0.03f, y, 0.20f,
                    0.42f + launchPose * 0.03f, y + 0.035f, 0.245f + launchPose * 0.025f,
                    accent[0] * 0.85f, accent[1] * 0.85f, accent[2], 0.68f, light);
        }
        renderCuboid(consumer, pose,
                -0.05f, 0.13f, 0.18f, 0.05f, 0.55f, 0.24f,
                0.82f, 0.80f, 0.96f, 0.66f, light);
    }

    private static void renderCatapultMotif(VertexConsumer consumer, Matrix4f pose, float[] accent,
                                            float launchPose, int light) {
        renderCuboid(consumer, pose,
                -0.70f - launchPose * 0.10f, 0.15f, 0.22f,
                -0.10f, 0.20f, 0.27f + launchPose * 0.045f,
                0.78f, 0.82f, 0.86f, 0.72f, light);
        renderCuboid(consumer, pose,
                0.10f, 0.15f, 0.22f,
                0.70f + launchPose * 0.10f, 0.20f, 0.27f + launchPose * 0.045f,
                0.78f, 0.82f, 0.86f, 0.72f, light);
        renderCuboid(consumer, pose,
                -0.12f, 0.12f + launchPose * 0.015f, 0.245f + launchPose * 0.075f,
                0.12f, 0.24f + launchPose * 0.015f, 0.31f + launchPose * 0.075f,
                accent[0], accent[1], accent[2], 0.74f, light);
    }

    private static void renderLongbowMotif(VertexConsumer consumer, Matrix4f pose, float[] accent,
                                           float launchPose, int light) {
        renderCuboid(consumer, pose,
                -0.72f, 0.10f, 0.20f, -0.62f, 0.60f, 0.25f,
                0.88f, 0.90f, 0.95f, 0.74f, light);
        renderCuboid(consumer, pose,
                -0.62f, 0.57f, 0.215f, 0.42f, 0.61f, 0.245f,
                accent[0], accent[1], accent[2], 0.70f, light);
        renderCuboid(consumer, pose,
                -0.62f, 0.09f, 0.215f, 0.42f, 0.13f, 0.245f,
                accent[0], accent[1], accent[2], 0.70f, light);
        if (launchPose > 0.02f) {
            renderCuboid(consumer, pose,
                    -0.62f, 0.32f - launchPose * 0.040f, 0.245f + launchPose * 0.055f,
                    0.36f, 0.36f - launchPose * 0.040f, 0.285f + launchPose * 0.070f,
                    1.00f, 0.96f, 0.78f, 0.42f * launchPose, light);
        }
    }

    private static void renderRapierMotif(VertexConsumer consumer, Matrix4f pose, float[] accent,
                                          float launchPose, int light) {
        renderCuboid(consumer, pose,
                0.46f + launchPose * 0.060f, 0.10f + launchPose * 0.035f, 0.18f,
                0.52f + launchPose * 0.060f, 0.66f + launchPose * 0.035f, 0.23f + launchPose * 0.045f,
                0.92f, 0.92f, 0.98f, 0.80f, light);
        renderCuboid(consumer, pose,
                0.34f + launchPose * 0.050f, 0.18f + launchPose * 0.025f, 0.19f,
                0.64f + launchPose * 0.050f, 0.24f + launchPose * 0.025f, 0.25f + launchPose * 0.045f,
                accent[0], accent[1], accent[2], 0.70f, light);
    }

    private static void renderFusilierMotif(VertexConsumer consumer, Matrix4f pose, float[] accent,
                                            float launchPose, int light) {
        renderCuboid(consumer, pose,
                -0.62f, 0.18f, 0.19f - launchPose * 0.035f,
                -0.30f, 0.28f, 0.30f - launchPose * 0.035f,
                0.18f, 0.20f, 0.22f, 0.82f, light);
        renderCuboid(consumer, pose,
                0.30f, 0.18f, 0.19f - launchPose * 0.035f,
                0.62f, 0.28f, 0.30f - launchPose * 0.035f,
                0.18f, 0.20f, 0.22f, 0.82f, light);
        renderCuboid(consumer, pose,
                -0.28f, 0.21f, 0.23f, 0.28f, 0.25f, 0.28f,
                accent[0], accent[1], accent[2], 0.68f, light);
    }

    private static void renderLaunchPulse(VertexConsumer consumer, Matrix4f pose, float[] accent,
                                          float launchPose, int light) {
        if (launchPose <= 0.02f) {
            return;
        }
        renderCuboid(consumer, pose,
                -0.48f - launchPose * 0.10f, 0.135f, 0.28f + launchPose * 0.08f,
                0.48f + launchPose * 0.10f, 0.150f, 0.33f + launchPose * 0.12f,
                accent[0], accent[1], accent[2], 0.34f * launchPose, light);
    }

    /**
     * 后背烟囱与桅杆：所有舰种都有，位于躯干后方（模型 -z 方向，+y 朝下）。
     * 放飞时随舰体一起轻微后仰，和 hull 的偏移方向一致。
     */
    private static void renderBackSuperstructure(VertexConsumer consumer, Matrix4f pose, float[] accent,
                                                 float launchPose, int light) {
        float z = -launchPose * 0.03f;
        // 烟囱主体（上窄下宽）
        renderCuboid(consumer, pose,
                -1.5f, -4.6f, -5.0f + z, 1.5f, 1.4f, -2.8f + z,
                0.16f, 0.17f, 0.20f, 0.88f, light);
        renderCuboid(consumer, pose,
                -1.1f, -5.3f, -4.6f + z, 1.1f, -4.5f, -3.2f + z,
                0.10f, 0.11f, 0.13f, 0.90f, light);
        // 烟囱根部的配色环
        renderCuboid(consumer, pose,
                -1.55f, 0.9f, -5.05f + z, 1.55f, 1.5f, -2.75f + z,
                accent[0], accent[1], accent[2], 0.72f, light);
        // 桅杆 + 横桁
        renderCuboid(consumer, pose,
                -0.2f, -12.5f, -4.3f + z, 0.2f, -4.6f, -3.7f + z,
                0.78f, 0.80f, 0.84f, 0.80f, light);
        renderCuboid(consumer, pose,
                -2.6f, -9.2f, -4.2f + z, 2.6f, -8.8f, -3.8f + z,
                0.78f, 0.80f, 0.84f, 0.76f, light);
        renderCuboid(consumer, pose,
                -0.35f, -13.0f, -4.1f + z, 0.35f, -12.5f, -3.9f + z,
                accent[0], accent[1], accent[2], 0.70f, light);
    }

    /**
     * 舷侧结构：一块竖向装甲板 + 上下两个炮位平台。
     * {@code side} = +1 为左舷（模型 +x），-1 为右舷。挂点高度与
     * {@link #mountPose} 中 SIDE_LEFT/right 的 y 对齐。
     */
    private static void renderSideSponson(VertexConsumer consumer, Matrix4f pose, float[] accent,
                                          float side, float launchPose, int light) {
        float inner = Math.min(side * 2.5f, side * 3.1f);
        float outer = Math.max(side * 2.5f, side * 3.1f);
        // 竖板
        renderCuboid(consumer, pose, inner, 0.6f, -1.5f, outer, 5.4f, 1.4f,
                0.14f, 0.15f, 0.18f, 0.86f, light);
        // 上炮位平台（y 小 = 高）
        renderCuboid(consumer, pose,
                Math.min(side * 3.1f, side * 4.9f), 0.6f, -0.9f,
                Math.max(side * 3.1f, side * 4.9f), 1.2f, 1.3f,
                0.20f, 0.21f, 0.25f, 0.88f, light);
        // 下炮位平台
        renderCuboid(consumer, pose,
                Math.min(side * 3.1f, side * 4.9f), 4.0f, -1.5f,
                Math.max(side * 3.1f, side * 4.9f), 4.6f, 0.7f,
                0.20f, 0.21f, 0.25f, 0.88f, light);
        // 舷侧配色条
        renderCuboid(consumer, pose,
                Math.min(side * 3.1f, side * 3.25f), 1.9f - launchPose * 0.05f, -1.4f,
                Math.max(side * 3.1f, side * 3.25f), 2.1f - launchPose * 0.05f, 1.3f,
                accent[0], accent[1], accent[2], 0.68f, light);
        // 连接躯干的支撑
        renderCuboid(consumer, pose,
                Math.min(side * 2.2f, side * 2.5f), 0.4f, -0.4f,
                Math.max(side * 2.2f, side * 2.5f), 5.6f, 0.4f,
                0.12f, 0.13f, 0.16f, 0.84f, light);
    }

    /**
     * 右舷飞行甲板：只在快捷栏里有飞机时出现（届时右舷炮位取消）。
     * 甲板向模型 -x 方向伸出，两个飞机位在 z 轴前后各一。
     */
    private static void renderFlightDeck(VertexConsumer consumer, Matrix4f pose, float[] accent,
                                         float launchPose, int light) {
        float lift = launchPose * 0.02f;
        // 甲板面
        renderCuboid(consumer, pose,
                -9.0f, 0.8f - lift, -4.6f, -2.4f, 1.2f - lift, 4.6f,
                0.26f, 0.29f, 0.31f, 0.84f, light);
        // 舷边配色条
        renderCuboid(consumer, pose,
                -9.05f, 0.75f - lift, -4.6f, -8.6f, 1.25f - lift, 4.6f,
                accent[0], accent[1], accent[2], 0.74f, light);
        // 跑道中线
        renderCuboid(consumer, pose,
                -8.4f, 0.74f - lift, -0.15f, -3.0f, 0.82f - lift, 0.15f,
                0.92f, 0.86f, 0.62f, 0.86f, light);
        // 两条弹射轨道
        for (float z : new float[] { -2.2f, 2.2f }) {
            renderCuboid(consumer, pose,
                    -8.4f, 0.74f - lift, z - 0.1f, -3.4f, 0.82f - lift, z + 0.1f,
                    0.62f, 0.64f, 0.68f, 0.82f, light);
        }
        // 与躯干连接的支撑臂
        renderCuboid(consumer, pose, -3.0f, 0.6f, -0.8f, -2.3f, 1.6f, 0.8f,
                0.12f, 0.13f, 0.16f, 0.86f, light);
    }

    private static void renderDefaultHull(VertexConsumer consumer, Matrix4f pose, float[] accent,
                                          boolean hasAircraft, int packedLight) {
        renderCuboid(consumer, pose,
                -0.68f, -0.02f, -0.05f, -0.48f, 0.12f, 0.21f,
                accent[0], accent[1], accent[2], 0.68f, packedLight);
        renderCuboid(consumer, pose,
                0.48f, -0.02f, -0.05f, 0.68f, 0.12f, 0.21f,
                accent[0], accent[1], accent[2], 0.68f, packedLight);
        if (hasAircraft) {
            renderCarrierDeck(consumer, pose, packedLight);
        }
    }

    private static void renderCarrierHull(VertexConsumer consumer, Matrix4f pose, float[] accent, int packedLight) {
        renderCuboid(consumer, pose,
                -0.76f, -0.03f, -0.08f, 0.76f, 0.08f, 0.21f,
                accent[0] * 0.72f, accent[1] * 0.72f, accent[2] * 0.72f, 0.76f, packedLight);
        renderCarrierDeck(consumer, pose, packedLight);
        renderCuboid(consumer, pose,
                0.35f, 0.12f, 0.02f, 0.56f, 0.24f, 0.16f,
                accent[0], accent[1], accent[2], 0.74f, packedLight);
    }

    private static void renderCarrierDeck(VertexConsumer consumer, Matrix4f pose, int packedLight) {
        renderCuboid(consumer, pose,
                -0.54f, 0.08f, -0.15f, 0.54f, 0.105f, 0.28f,
                0.26f, 0.29f, 0.31f, 0.84f, packedLight);
        renderCuboid(consumer, pose,
                -0.04f, 0.112f, -0.13f, 0.04f, 0.13f, 0.26f,
                0.92f, 0.86f, 0.62f, 0.88f, packedLight);
    }

    private static void renderDestroyerHull(VertexConsumer consumer, Matrix4f pose, float[] accent, int packedLight) {
        renderCuboid(consumer, pose,
                -0.72f, -0.01f, -0.06f, -0.50f, 0.12f, 0.20f,
                accent[0], accent[1], accent[2], 0.70f, packedLight);
        renderCuboid(consumer, pose,
                0.50f, -0.01f, -0.06f, 0.72f, 0.12f, 0.20f,
                accent[0], accent[1], accent[2], 0.70f, packedLight);
        renderCuboid(consumer, pose,
                -0.33f, 0.08f, -0.03f, -0.20f, 0.18f, 0.11f,
                0.18f, 0.20f, 0.22f, 0.82f, packedLight);
        renderCuboid(consumer, pose,
                0.20f, 0.08f, -0.03f, 0.33f, 0.18f, 0.11f,
                0.18f, 0.20f, 0.22f, 0.82f, packedLight);
    }

    private static void renderCruiserHull(VertexConsumer consumer, Matrix4f pose, float[] accent, int packedLight) {
        renderCuboid(consumer, pose,
                -0.82f, -0.03f, -0.07f, -0.55f, 0.14f, 0.22f,
                accent[0], accent[1], accent[2], 0.72f, packedLight);
        renderCuboid(consumer, pose,
                0.55f, -0.03f, -0.07f, 0.82f, 0.14f, 0.22f,
                accent[0], accent[1], accent[2], 0.72f, packedLight);
        renderCuboid(consumer, pose,
                -0.09f, 0.07f, -0.03f, 0.09f, 0.25f, 0.13f,
                0.20f, 0.22f, 0.25f, 0.86f, packedLight);
        renderCuboid(consumer, pose,
                -0.035f, 0.24f, -0.01f, 0.035f, 0.39f, 0.06f,
                accent[0], accent[1], accent[2], 0.76f, packedLight);
    }

    private static void renderSubmarineHull(VertexConsumer consumer, Matrix4f pose, float[] accent, int packedLight) {
        renderCuboid(consumer, pose,
                -0.64f, -0.05f, -0.05f, 0.64f, 0.05f, 0.12f,
                accent[0] * 0.65f, accent[1] * 0.65f, accent[2] * 0.65f, 0.76f, packedLight);
        renderCuboid(consumer, pose,
                -0.10f, 0.045f, -0.02f, 0.10f, 0.20f, 0.08f,
                accent[0], accent[1], accent[2], 0.78f, packedLight);
        renderCuboid(consumer, pose,
                -0.02f, 0.18f, -0.005f, 0.02f, 0.32f, 0.04f,
                0.78f, 0.82f, 0.86f, 0.72f, packedLight);
    }

    /**
     * 按挂点摆一个物品模型。
     *
     * <p>挂点参考系统一由 {@link RiggingSocketTransforms} 提供，和 GeckoLib 层共用，
     * 避免两套坐标漂移。
     */
    private void renderMount(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                             AbstractClientPlayer player, RiggingMount mount, float launchPose) {
        poseStack.pushPose();
        RiggingSocketTransforms.applyBase(poseStack, this.getParentModel(), mount.socket(), launchPose);

        RiggingSocketTransforms.MountPose pose =
                RiggingSocketTransforms.mountPose(mount.socket(), launchPose);
        RiggingSocketTransforms.applyMountFrame(poseStack, pose);
        poseStack.scale(pose.scale(), pose.scale(), pose.scale());

        Minecraft.getInstance().getItemRenderer().renderStatic(
                mount.stack(), ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY,
                poseStack, bufferSource, player.level(), player.getId() * 31 + mount.socket().ordinal());
        poseStack.popPose();
    }

    private static void renderCuboid(VertexConsumer consumer, Matrix4f pose,
                                     float minX, float minY, float minZ,
                                     float maxX, float maxY, float maxZ,
                                     float r, float g, float b, float alpha,
                                     int light) {
        addQuad(consumer, pose, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ,
                r, g, b, alpha, 0, -1, 0, light);
        addQuad(consumer, pose, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ,
                r, g, b, alpha, 0, 1, 0, light);
        addQuad(consumer, pose, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ,
                r, g, b, alpha, 0, 0, 1, light);
        addQuad(consumer, pose, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ,
                r, g, b, alpha, 0, 0, -1, light);
        addQuad(consumer, pose, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ,
                r, g, b, alpha, -1, 0, 0, light);
        addQuad(consumer, pose, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ,
                r, g, b, alpha, 1, 0, 0, light);
    }

    private static void addQuad(VertexConsumer consumer, Matrix4f pose,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float x3, float y3, float z3,
                                float x4, float y4, float z4,
                                float r, float g, float b, float alpha,
                                float nx, float ny, float nz,
                                int light) {
        consumer.addVertex(pose, x1, y1, z1)
                .setColor(r, g, b, alpha)
                .setUv(0, 0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(nx, ny, nz);
        consumer.addVertex(pose, x2, y2, z2)
                .setColor(r, g, b, alpha)
                .setUv(1, 0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(nx, ny, nz);
        consumer.addVertex(pose, x3, y3, z3)
                .setColor(r, g, b, alpha)
                .setUv(1, 1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(nx, ny, nz);
        consumer.addVertex(pose, x4, y4, z4)
                .setColor(r, g, b, alpha)
                .setUv(0, 1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(nx, ny, nz);
    }
}
