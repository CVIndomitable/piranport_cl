package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.piranport.client.model.UnicornModel;
import com.piranport.client.model.KitchenGoddessModel;
import com.piranport.npc.shipgirl.ShipGirlEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;

/**
 * Skin-aware ship rigging for ship girl NPCs until final authored models are available.
 */
public class ShipGirlRiggingLayer extends RenderLayer<ShipGirlEntity, PlayerModel<ShipGirlEntity>> {
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

    public ShipGirlRiggingLayer(RenderLayerParent<ShipGirlEntity, PlayerModel<ShipGirlEntity>> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       ShipGirlEntity entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) {
            return;
        }

        int skinId = entity.getSkinVariant();
        if (skinId == UnicornModel.SKIN_ID
                || skinId == KitchenGoddessModel.SKIN_ID
                || FubukiShipGirlRiggingLayer.supports(skinId)) {
            return;
        }
        int rapport = entity.getRapport();
        HullProfile profile = resolveHullProfile(skinId);
        float[] accent = accentColor(skinId);
        float pulse = rapport >= 80 ? 0.5f + 0.5f * (float) Math.sin(ageInTicks * 0.16f) : 0.0f;

        poseStack.pushPose();
        getParentModel().body.translateAndRotate(poseStack);
        poseStack.translate(0.0, 0.28 + Math.sin(ageInTicks * 0.08f) * 0.01, 0.34);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.translucent());
        Matrix4f pose = poseStack.last().pose();

        renderBackplate(consumer, pose, packedLight);
        switch (profile) {
            case CARRIER -> renderCarrier(consumer, pose, accent, rapport, packedLight);
            case DESTROYER -> renderDestroyer(consumer, pose, accent, rapport, packedLight);
            case CRUISER -> renderCruiser(consumer, pose, accent, rapport, packedLight);
            case SUBMARINE -> renderSubmarine(consumer, pose, accent, rapport, packedLight);
            case DEFAULT -> renderDefault(consumer, pose, accent, rapport, packedLight);
        }
        renderSkinMotif(consumer, pose, resolveMotif(skinId), accent, packedLight);
        renderRapportMarks(consumer, pose, accent, rapport, pulse, packedLight);

        poseStack.popPose();
    }

    private static HullProfile resolveHullProfile(int skinId) {
        return switch (skinId) {
            case 4, 18, 20 -> HullProfile.CARRIER;
            case 5, 6, 7, 8, 9, 13, 14, 15, 21, 22 -> HullProfile.DESTROYER;
            case 10, 11, 12, 16, 19 -> HullProfile.CRUISER;
            case 17 -> HullProfile.SUBMARINE;
            default -> HullProfile.DEFAULT;
        };
    }

    private static float[] accentColor(int skinId) {
        return switch (skinId) {
            case 4, 8, 9, 10, 11, 13 -> new float[] { 0.72f, 0.58f, 0.42f };
            case 5, 6, 7, 14, 15 -> new float[] { 0.42f, 0.68f, 0.76f };
            case 12, 16, 17 -> new float[] { 0.58f, 0.50f, 0.78f };
            case 18, 19, 20, 21, 22 -> new float[] { 0.62f, 0.72f, 0.90f };
            default -> new float[] { 0.48f, 0.62f, 0.72f };
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
                                        float[] accent, int light) {
        switch (motif) {
            case J_BOW -> renderBowMotif(consumer, pose, accent, light);
            case C_TALISMAN -> renderTalismanMotif(consumer, pose, accent, light);
            case G_ARRAY -> renderArrayMotif(consumer, pose, accent, light);
            case I_CATAPULT -> renderCatapultMotif(consumer, pose, accent, light);
            case E_LONGBOW -> renderLongbowMotif(consumer, pose, accent, light);
            case F_RAPIER -> renderRapierMotif(consumer, pose, accent, light);
            case U_FUSILIER -> renderFusilierMotif(consumer, pose, accent, light);
            case NONE -> {
            }
        }
    }

    private static void renderBackplate(VertexConsumer consumer, Matrix4f pose, int light) {
        renderCuboid(consumer, pose,
                -0.26f, -0.04f, -0.04f, 0.26f, 0.14f, 0.10f,
                0.12f, 0.14f, 0.17f, 0.70f, light);
        renderCuboid(consumer, pose,
                -0.05f, 0.12f, -0.02f, 0.05f, 0.40f, 0.08f,
                0.20f, 0.22f, 0.25f, 0.64f, light);
    }

    private static void renderBowMotif(VertexConsumer consumer, Matrix4f pose, float[] accent, int light) {
        renderCuboid(consumer, pose,
                -0.55f, 0.17f, 0.18f, -0.46f, 0.50f, 0.23f,
                0.94f, 0.84f, 0.62f, 0.72f, light);
        renderCuboid(consumer, pose,
                0.46f, 0.17f, 0.18f, 0.55f, 0.50f, 0.23f,
                0.94f, 0.84f, 0.62f, 0.72f, light);
        renderCuboid(consumer, pose,
                -0.43f, 0.32f, 0.20f, 0.43f, 0.36f, 0.23f,
                accent[0], accent[1], accent[2], 0.64f, light);
    }

    private static void renderTalismanMotif(VertexConsumer consumer, Matrix4f pose, float[] accent, int light) {
        for (int i = 0; i < 4; i++) {
            float x = -0.30f + i * 0.20f;
            renderCuboid(consumer, pose,
                    x, 0.17f, 0.18f, x + 0.07f, 0.38f, 0.23f,
                    0.86f, 0.94f, 1.00f, 0.52f, light);
            renderCuboid(consumer, pose,
                    x + 0.012f, 0.23f, 0.232f, x + 0.058f, 0.26f, 0.26f,
                    accent[0], accent[1], accent[2], 0.72f, light);
        }
    }

    private static void renderArrayMotif(VertexConsumer consumer, Matrix4f pose, float[] accent, int light) {
        for (int i = 0; i < 3; i++) {
            float y = 0.17f + i * 0.085f;
            renderCuboid(consumer, pose,
                    -0.36f, y, 0.18f, 0.36f, y + 0.03f, 0.23f,
                    accent[0] * 0.85f, accent[1] * 0.85f, accent[2], 0.62f, light);
        }
        renderCuboid(consumer, pose,
                -0.04f, 0.13f, 0.17f, 0.04f, 0.48f, 0.22f,
                0.82f, 0.80f, 0.96f, 0.62f, light);
    }

    private static void renderCatapultMotif(VertexConsumer consumer, Matrix4f pose, float[] accent, int light) {
        renderCuboid(consumer, pose,
                -0.60f, 0.145f, 0.20f, -0.10f, 0.19f, 0.25f,
                0.78f, 0.82f, 0.86f, 0.66f, light);
        renderCuboid(consumer, pose,
                0.10f, 0.145f, 0.20f, 0.60f, 0.19f, 0.25f,
                0.78f, 0.82f, 0.86f, 0.66f, light);
        renderCuboid(consumer, pose,
                -0.10f, 0.12f, 0.225f, 0.10f, 0.22f, 0.29f,
                accent[0], accent[1], accent[2], 0.68f, light);
    }

    private static void renderLongbowMotif(VertexConsumer consumer, Matrix4f pose, float[] accent, int light) {
        renderCuboid(consumer, pose,
                -0.62f, 0.10f, 0.18f, -0.54f, 0.54f, 0.23f,
                0.88f, 0.90f, 0.95f, 0.68f, light);
        renderCuboid(consumer, pose,
                -0.54f, 0.51f, 0.20f, 0.36f, 0.55f, 0.23f,
                accent[0], accent[1], accent[2], 0.64f, light);
        renderCuboid(consumer, pose,
                -0.54f, 0.10f, 0.20f, 0.36f, 0.14f, 0.23f,
                accent[0], accent[1], accent[2], 0.64f, light);
    }

    private static void renderRapierMotif(VertexConsumer consumer, Matrix4f pose, float[] accent, int light) {
        renderCuboid(consumer, pose,
                0.42f, 0.10f, 0.17f, 0.48f, 0.58f, 0.22f,
                0.92f, 0.92f, 0.98f, 0.74f, light);
        renderCuboid(consumer, pose,
                0.31f, 0.18f, 0.18f, 0.58f, 0.23f, 0.24f,
                accent[0], accent[1], accent[2], 0.64f, light);
    }

    private static void renderFusilierMotif(VertexConsumer consumer, Matrix4f pose, float[] accent, int light) {
        renderCuboid(consumer, pose,
                -0.54f, 0.18f, 0.18f, -0.28f, 0.27f, 0.28f,
                0.18f, 0.20f, 0.22f, 0.76f, light);
        renderCuboid(consumer, pose,
                0.28f, 0.18f, 0.18f, 0.54f, 0.27f, 0.28f,
                0.18f, 0.20f, 0.22f, 0.76f, light);
        renderCuboid(consumer, pose,
                -0.25f, 0.21f, 0.22f, 0.25f, 0.25f, 0.27f,
                accent[0], accent[1], accent[2], 0.62f, light);
    }

    private static void renderDefault(VertexConsumer consumer, Matrix4f pose, float[] accent,
                                      int rapport, int light) {
        renderSideHull(consumer, pose, -0.50f, -0.34f, accent, 0.66f, light);
        renderSideHull(consumer, pose, 0.34f, 0.50f, accent, 0.66f, light);
        renderTurret(consumer, pose, -0.18f, 0.11f, accent, light);
        renderTurret(consumer, pose, 0.18f, 0.11f, accent, light);
        if (rapport >= 50) {
            renderSignalMast(consumer, pose, accent, light);
        }
    }

    private static void renderCarrier(VertexConsumer consumer, Matrix4f pose, float[] accent,
                                      int rapport, int light) {
        renderCuboid(consumer, pose,
                -0.70f, -0.03f, -0.08f, 0.70f, 0.07f, 0.18f,
                accent[0] * 0.68f, accent[1] * 0.68f, accent[2] * 0.68f, 0.70f, light);
        renderCuboid(consumer, pose,
                -0.58f, 0.075f, -0.15f, 0.58f, 0.115f, 0.27f,
                0.25f, 0.28f, 0.30f, 0.82f, light);
        renderCuboid(consumer, pose,
                -0.03f, 0.125f, -0.13f, 0.03f, 0.145f, 0.25f,
                0.90f, 0.84f, 0.58f, 0.82f, light);
        renderCuboid(consumer, pose,
                0.32f, 0.12f, -0.12f, 0.50f, 0.28f, 0.06f,
                accent[0], accent[1], accent[2], 0.70f, light);
        if (rapport >= 50) {
            renderCuboid(consumer, pose,
                    -0.52f, 0.15f, 0.06f, -0.40f, 0.22f, 0.13f,
                    0.76f, 0.84f, 0.94f, 0.72f, light);
        }
    }

    private static void renderDestroyer(VertexConsumer consumer, Matrix4f pose, float[] accent,
                                        int rapport, int light) {
        renderSideHull(consumer, pose, -0.68f, -0.46f, accent, 0.72f, light);
        renderSideHull(consumer, pose, 0.46f, 0.68f, accent, 0.72f, light);
        renderTurret(consumer, pose, -0.30f, 0.12f, accent, light);
        renderTurret(consumer, pose, 0.30f, 0.12f, accent, light);
        renderTorpedoRack(consumer, pose, -0.18f, accent, light);
        renderTorpedoRack(consumer, pose, 0.18f, accent, light);
        if (rapport >= 50) {
            renderSignalMast(consumer, pose, accent, light);
        }
    }

    private static void renderCruiser(VertexConsumer consumer, Matrix4f pose, float[] accent,
                                      int rapport, int light) {
        renderSideHull(consumer, pose, -0.78f, -0.52f, accent, 0.75f, light);
        renderSideHull(consumer, pose, 0.52f, 0.78f, accent, 0.75f, light);
        renderTurret(consumer, pose, -0.34f, 0.13f, accent, light);
        renderTurret(consumer, pose, 0.0f, 0.15f, accent, light);
        renderTurret(consumer, pose, 0.34f, 0.13f, accent, light);
        renderCuboid(consumer, pose,
                -0.10f, 0.08f, -0.02f, 0.10f, 0.26f, 0.12f,
                0.22f, 0.24f, 0.28f, 0.78f, light);
        if (rapport >= 50) {
            renderSignalMast(consumer, pose, accent, light);
        }
    }

    private static void renderSubmarine(VertexConsumer consumer, Matrix4f pose, float[] accent,
                                        int rapport, int light) {
        renderCuboid(consumer, pose,
                -0.62f, -0.05f, -0.05f, 0.62f, 0.05f, 0.12f,
                accent[0] * 0.66f, accent[1] * 0.66f, accent[2] * 0.66f, 0.76f, light);
        renderCuboid(consumer, pose,
                -0.12f, 0.045f, -0.02f, 0.12f, 0.20f, 0.08f,
                accent[0], accent[1], accent[2], 0.76f, light);
        renderCuboid(consumer, pose,
                -0.025f, 0.18f, -0.005f, 0.025f, 0.32f, 0.04f,
                0.82f, 0.86f, 0.90f, 0.70f, light);
        if (rapport >= 50) {
            renderCuboid(consumer, pose,
                    -0.50f, 0.03f, 0.13f, 0.50f, 0.06f, 0.17f,
                    0.70f, 0.90f, 1.00f, 0.54f, light);
        }
    }

    private static void renderSideHull(VertexConsumer consumer, Matrix4f pose,
                                       float minX, float maxX, float[] accent, float alpha, int light) {
        renderCuboid(consumer, pose,
                minX, -0.02f, -0.07f, maxX, 0.13f, 0.19f,
                accent[0], accent[1], accent[2], alpha, light);
        renderCuboid(consumer, pose,
                minX + 0.02f, 0.12f, 0.00f, maxX - 0.02f, 0.21f, 0.13f,
                0.18f, 0.20f, 0.23f, alpha * 0.95f, light);
    }

    private static void renderTurret(VertexConsumer consumer, Matrix4f pose, float x,
                                     float y, float[] accent, int light) {
        renderCuboid(consumer, pose,
                x - 0.08f, y, -0.14f, x + 0.08f, y + 0.08f, 0.02f,
                accent[0] * 0.85f, accent[1] * 0.85f, accent[2] * 0.90f, 0.78f, light);
        renderCuboid(consumer, pose,
                x - 0.024f, y + 0.035f, -0.30f, x + 0.024f, y + 0.062f, -0.13f,
                0.78f, 0.80f, 0.84f, 0.74f, light);
    }

    private static void renderTorpedoRack(VertexConsumer consumer, Matrix4f pose, float x,
                                          float[] accent, int light) {
        for (int i = 0; i < 3; i++) {
            float y = 0.015f + i * 0.045f;
            renderCuboid(consumer, pose,
                    x - 0.055f, y, 0.16f, x + 0.055f, y + 0.025f, 0.30f,
                    accent[0] * 0.76f, accent[1] * 0.76f, accent[2] * 0.82f, 0.70f, light);
        }
    }

    private static void renderSignalMast(VertexConsumer consumer, Matrix4f pose, float[] accent, int light) {
        renderCuboid(consumer, pose,
                -0.018f, 0.26f, 0.00f, 0.018f, 0.58f, 0.04f,
                0.82f, 0.84f, 0.86f, 0.76f, light);
        renderCuboid(consumer, pose,
                0.018f, 0.46f, 0.00f, 0.18f, 0.54f, 0.035f,
                accent[0], accent[1], accent[2], 0.78f, light);
    }

    private static void renderRapportMarks(VertexConsumer consumer, Matrix4f pose, float[] accent,
                                           int rapport, float pulse, int light) {
        if (rapport < 20) {
            return;
        }
        renderCuboid(consumer, pose,
                -0.20f, -0.055f, 0.105f, 0.20f, -0.025f, 0.145f,
                accent[0] * 1.1f, accent[1] * 1.1f, accent[2] * 1.1f, 0.72f, light);
        if (rapport >= 80) {
            float alpha = 0.46f + 0.22f * pulse;
            renderCuboid(consumer, pose,
                    -0.30f, 0.16f, 0.12f, 0.30f, 0.20f, 0.18f,
                    0.92f, 0.88f, 0.62f, alpha, light);
        }
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
