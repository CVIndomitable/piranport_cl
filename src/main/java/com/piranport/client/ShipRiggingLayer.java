package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.piranport.artillery.ArtilleryItem;
import com.piranport.combat.TransformationManager;
import com.piranport.item.AircraftItem;
import com.piranport.item.DepthChargeLauncherItem;
import com.piranport.item.MissileLauncherItem;
import com.piranport.item.TorpedoLauncherItem;
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
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Ship rigging layer that renders a skin-aware hull silhouette and reuses
 * existing weapon item models for hardpoints until final authored models exist.
 */
public class ShipRiggingLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final int MAX_SIDE_RIGGING = 3;

    private enum HullProfile {
        CARRIER,
        DESTROYER,
        CRUISER,
        SUBMARINE,
        DEFAULT
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
        if (!isRenderedAsTransformed(player)) return;

        List<ItemStack> rigging = collectRigging(player);
        float launchPose = AircraftLaunchPoseClientState.intensity(player.getId(), partialTick);
        renderHull(poseStack, bufferSource, packedLight, player, containsAircraft(rigging), launchPose);

        for (int i = 0; i < rigging.size(); i++) {
            renderStack(poseStack, bufferSource, packedLight, player, rigging.get(i), i, launchPose);
        }
    }

    private static List<ItemStack> collectRigging(AbstractClientPlayer player) {
        if (!isRenderedAsTransformed(player)) return List.of();

        ItemStack cannon = ItemStack.EMPTY;
        ItemStack aircraft = ItemStack.EMPTY;
        ItemStack torpedo = ItemStack.EMPTY;
        ItemStack missile = ItemStack.EMPTY;
        ItemStack depth = ItemStack.EMPTY;

        boolean localPlayer = player == Minecraft.getInstance().player;
        if (localPlayer) {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = player.getInventory().items.get(i);
                if (stack.isEmpty()) continue;
                if (cannon.isEmpty() && stack.getItem() instanceof ArtilleryItem) cannon = one(stack);
                else if (aircraft.isEmpty() && stack.getItem() instanceof AircraftItem) aircraft = one(stack);
                else if (torpedo.isEmpty() && stack.getItem() instanceof TorpedoLauncherItem) torpedo = one(stack);
                else if (missile.isEmpty() && stack.getItem() instanceof MissileLauncherItem) missile = one(stack);
                else if (depth.isEmpty() && stack.getItem() instanceof DepthChargeLauncherItem) depth = one(stack);
            }
        }

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        if (cannon.isEmpty() && mainHand.getItem() instanceof ArtilleryItem) cannon = one(mainHand);
        if (cannon.isEmpty() && offHand.getItem() instanceof ArtilleryItem) cannon = one(offHand);
        if (aircraft.isEmpty() && mainHand.getItem() instanceof AircraftItem) aircraft = one(mainHand);
        if (aircraft.isEmpty() && offHand.getItem() instanceof AircraftItem) aircraft = one(offHand);
        if (torpedo.isEmpty() && mainHand.getItem() instanceof TorpedoLauncherItem) torpedo = one(mainHand);
        if (torpedo.isEmpty() && offHand.getItem() instanceof TorpedoLauncherItem) torpedo = one(offHand);
        if (missile.isEmpty() && mainHand.getItem() instanceof MissileLauncherItem) missile = one(mainHand);
        if (missile.isEmpty() && offHand.getItem() instanceof MissileLauncherItem) missile = one(offHand);
        if (depth.isEmpty() && mainHand.getItem() instanceof DepthChargeLauncherItem) depth = one(mainHand);
        if (depth.isEmpty() && offHand.getItem() instanceof DepthChargeLauncherItem) depth = one(offHand);

        List<ItemStack> result = new ArrayList<>();
        if (!cannon.isEmpty()) {
            result.add(cannon);
        } else if (!aircraft.isEmpty()) {
            result.add(aircraft);
        }
        addIfPresent(result, torpedo);
        addIfPresent(result, missile);
        addIfPresent(result, depth);
        if (result.size() <= MAX_SIDE_RIGGING && !aircraft.isEmpty() && !sameItem(result.get(0), aircraft)) {
            result.add(aircraft);
        }
        return result.size() > MAX_SIDE_RIGGING + 1 ? result.subList(0, MAX_SIDE_RIGGING + 1) : result;
    }

    private static boolean isRenderedAsTransformed(AbstractClientPlayer player) {
        boolean localPlayer = player == Minecraft.getInstance().player;
        return localPlayer
                ? TransformationManager.isPlayerTransformed(player)
                : ClientSkinData.getActiveSkin(player.getUUID()) > 0;
    }

    private void renderHull(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                            AbstractClientPlayer player, boolean hasAircraft, float launchPose) {
        poseStack.pushPose();
        this.getParentModel().body.translateAndRotate(poseStack);
        poseStack.translate(0.0, 0.28 + launchPose * 0.035, 0.34 - launchPose * 0.035);
        poseStack.mulPose(Axis.XP.rotationDegrees(-3.0f * launchPose));

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.translucent());
        Matrix4f pose = poseStack.last().pose();

        int skinId = AircraftLaunchPoseClientState.skinId(
                player.getId(), ClientSkinData.getActiveSkin(player.getUUID()));
        HullProfile profile = resolveHullProfile(skinId, hasAircraft);
        float[] accent = accentColor(skinId);

        renderCuboid(consumer, pose,
                -0.55f, -0.08f, -0.10f, 0.55f, 0.07f, 0.16f,
                0.10f, 0.12f, 0.15f, 0.82f, packedLight);

        switch (profile) {
            case CARRIER -> renderCarrierHull(consumer, pose, accent, packedLight);
            case DESTROYER -> renderDestroyerHull(consumer, pose, accent, packedLight);
            case CRUISER -> renderCruiserHull(consumer, pose, accent, packedLight);
            case SUBMARINE -> renderSubmarineHull(consumer, pose, accent, packedLight);
            case DEFAULT -> renderDefaultHull(consumer, pose, accent, hasAircraft, packedLight);
        }
        renderSkinMotif(consumer, pose, resolveMotif(skinId), accent, launchPose, packedLight);
        renderLaunchPulse(consumer, pose, accent, launchPose, packedLight);

        poseStack.popPose();
    }

    private static HullProfile resolveHullProfile(int skinId, boolean hasAircraft) {
        return switch (skinId) {
            case 4, 18, 20 -> HullProfile.CARRIER;
            case 5, 6, 7, 8, 9, 13, 14, 15, 21, 22 -> HullProfile.DESTROYER;
            case 10, 11, 12, 16, 19 -> HullProfile.CRUISER;
            case 17 -> HullProfile.SUBMARINE;
            default -> hasAircraft ? HullProfile.CARRIER : HullProfile.DEFAULT;
        };
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

    private static RiggingMotif resolveMotif(int skinId) {
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

    private void renderStack(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                             AbstractClientPlayer player, ItemStack stack, int index, float launchPose) {
        poseStack.pushPose();
        this.getParentModel().body.translateAndRotate(poseStack);

        HardpointPose pose = resolveHardpoint(
                AircraftLaunchPoseClientState.skinId(
                        player.getId(), ClientSkinData.getActiveSkin(player.getUUID())),
                index, launchPose);

        poseStack.translate(pose.x, pose.y, pose.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(pose.yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pose.pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pose.roll));
        poseStack.scale(pose.scale, pose.scale, pose.scale);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack, ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY,
                poseStack, bufferSource, player.level(), player.getId() * 31 + index);
        poseStack.popPose();
    }

    /** Per-skin weapon hardpoint pose. Index 0 = main weapon (front center). */
    private static HardpointPose resolveHardpoint(int skinId, int index, float launchPose) {
        // Skin-specific side gun layouts for carriers (deck-mounted turrets).
        if (index >= 1) {
            return switch (skinId) {
                // 航母: 两侧弹药/挂载点 (deck-side hardpoints)
                case 4, 18, 20 -> carrierSideHardpoint(index, launchPose);
                // 驱逐舰: 鱼雷/防空两侧
                case 5, 6, 7, 8, 9, 13, 14, 15, 21, 22 -> destroyerSideHardpoint(index, launchPose);
                // 巡洋舰: 中型炮外侧
                case 10, 11, 12, 16, 19 -> cruiserSideHardpoint(index, launchPose);
                // 潜艇: 单侧
                case 17 -> submarineSideHardpoint(index, launchPose);
                default -> defaultSideHardpoint(index, launchPose);
            };
        }
        // Main weapon (index 0): centered on bow, varies by hull profile.
        return mainHardpoint(skinId, launchPose);
    }

    private static HardpointPose mainHardpoint(int skinId, float launchPose) {
        // 航母主位放飞行甲板前缘；潜艇稍低；其他前置
        float y = 0.20f + launchPose * 0.025f;
        float z = 0.32f - launchPose * 0.070f;
        float pitch = -7.0f * launchPose;
        float scale = 0.62f;
        float roll = 90.0f;
        float yaw = 180.0f;
        if (skinId == 17) { // 潜艇
            y = 0.16f + launchPose * 0.020f;
            z = 0.28f - launchPose * 0.060f;
            scale = 0.58f;
        } else if (skinId == 4 || skinId == 18 || skinId == 20) { // 航母
            y = 0.32f + launchPose * 0.020f;
            z = 0.30f - launchPose * 0.050f;
        }
        return new HardpointPose(0.0f, y, z, yaw, pitch, roll, scale);
    }

    private static HardpointPose carrierSideHardpoint(int index, float launchPose) {
        // 航母: 甲板两侧，左右交替
        float side = index % 2 == 1 ? -1.0f : 1.0f;
        float row = (index + 1) / 2;
        float x = side * (0.46f + launchPose * 0.025f);
        float y = 0.22f + row * 0.08f;
        float z = 0.22f - launchPose * 0.030f;
        float yaw = 180.0f + side * 12.0f;
        float pitch = -4.0f * launchPose;
        float roll = side * 70.0f;
        float scale = 0.40f;
        return new HardpointPose(x, y, z, yaw, pitch, roll, scale);
    }

    private static HardpointPose destroyerSideHardpoint(int index, float launchPose) {
        // 驱逐舰: 鱼雷/防空两侧稍低
        float side = index % 2 == 1 ? -1.0f : 1.0f;
        float row = (index + 1) / 2;
        float x = side * (0.50f + launchPose * 0.025f);
        float y = 0.12f + row * 0.10f;
        float z = 0.22f - launchPose * 0.035f;
        float yaw = 180.0f + side * 22.0f;
        float pitch = -4.0f * launchPose;
        float roll = side * 60.0f;
        float scale = 0.40f;
        return new HardpointPose(x, y, z, yaw, pitch, roll, scale);
    }

    private static HardpointPose cruiserSideHardpoint(int index, float launchPose) {
        // 巡洋舰: 主炮塔外侧
        float side = index % 2 == 1 ? -1.0f : 1.0f;
        float row = (index + 1) / 2;
        float x = side * (0.56f + launchPose * 0.025f);
        float y = 0.16f + row * 0.10f;
        float z = 0.24f - launchPose * 0.035f;
        float yaw = 180.0f + side * 18.0f;
        float pitch = -4.0f * launchPose;
        float roll = side * 55.0f;
        float scale = 0.44f;
        return new HardpointPose(x, y, z, yaw, pitch, roll, scale);
    }

    private static HardpointPose submarineSideHardpoint(int index, float launchPose) {
        // 潜艇: 鱼雷管发射口在舷侧靠后
        float side = index % 2 == 1 ? -1.0f : 1.0f;
        float row = (index + 1) / 2;
        float x = side * (0.42f + launchPose * 0.020f);
        float y = 0.08f + row * 0.06f;
        float z = 0.18f - launchPose * 0.025f;
        float yaw = 180.0f + side * 30.0f;
        float pitch = -3.0f * launchPose;
        float roll = side * 75.0f;
        float scale = 0.36f;
        return new HardpointPose(x, y, z, yaw, pitch, roll, scale);
    }

    private static HardpointPose defaultSideHardpoint(int index, float launchPose) {
        float side = index % 2 == 1 ? -1.0f : 1.0f;
        float row = (index + 1) / 2;
        float x = side * (0.42f + launchPose * 0.025f);
        float y = 0.12f + row * 0.10f;
        float z = 0.24f - launchPose * 0.035f;
        float yaw = 180.0f + side * 18.0f;
        float pitch = -4.0f * launchPose;
        float roll = side * 58.0f;
        float scale = 0.42f;
        return new HardpointPose(x, y, z, yaw, pitch, roll, scale);
    }

    private record HardpointPose(float x, float y, float z, float yaw, float pitch, float roll, float scale) {}

    private static ItemStack one(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    private static void addIfPresent(List<ItemStack> result, ItemStack stack) {
        if (!stack.isEmpty()) {
            result.add(stack);
        }
    }

    private static boolean sameItem(ItemStack a, ItemStack b) {
        return !a.isEmpty() && !b.isEmpty() && a.getItem() == b.getItem();
    }

    private static boolean containsAircraft(List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (stack.getItem() instanceof AircraftItem) {
                return true;
            }
        }
        return false;
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
