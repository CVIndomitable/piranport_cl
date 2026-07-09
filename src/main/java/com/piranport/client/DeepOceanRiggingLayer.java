package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.piranport.npc.deepocean.AbstractDeepOceanEntity;
import com.piranport.npc.deepocean.DeepOceanFlagshipEntity;
import com.piranport.npc.deepocean.DeepOceanLightCruiserEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;

/** Programmatic ship silhouettes for deep ocean units until dedicated models land. */
public class DeepOceanRiggingLayer extends RenderLayer<AbstractDeepOceanEntity, HumanoidModel<AbstractDeepOceanEntity>> {

    public DeepOceanRiggingLayer(RenderLayerParent<AbstractDeepOceanEntity, HumanoidModel<AbstractDeepOceanEntity>> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       AbstractDeepOceanEntity entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) return;

        DeepOceanRenderer.VisualProfile profile = DeepOceanRenderer.profileFor(entity);
        float launchPose = AircraftLaunchPoseClientState.intensity(entity.getId(), partialTick);
        poseStack.pushPose();
        getParentModel().body.translateAndRotate(poseStack);
        poseStack.translate(0.0, 0.30 + launchPose * 0.025, 0.32 - launchPose * 0.035);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.translucent());
        Matrix4f pose = poseStack.last().pose();

        switch (profile.hullKind()) {
            case SUPPLY -> renderSupply(consumer, pose, profile, packedLight);
            case DESTROYER -> renderDestroyer(consumer, pose, profile, packedLight);
            case CRUISER -> {
                if (entity instanceof DeepOceanLightCruiserEntity) {
                    renderLightCruiser(consumer, pose, profile, packedLight);
                } else {
                    renderCruiser(consumer, pose, profile, packedLight);
                }
            }
            case BATTLESHIP -> renderBattleship(consumer, pose, profile, packedLight);
            case CARRIER -> renderCarrier(consumer, pose, profile, launchPose, packedLight);
            case SUBMARINE -> renderSubmarine(consumer, pose, profile, packedLight);
            case FLAGSHIP -> renderFlagship(consumer, pose, profile, flagshipPhase(entity), ageInTicks, packedLight);
        }

        poseStack.popPose();
    }

    private static void renderSupply(VertexConsumer consumer, Matrix4f pose,
                                     DeepOceanRenderer.VisualProfile profile, int light) {
        renderHull(consumer, pose, -0.34f, 0.34f, -0.04f, 0.18f, profile, light);
        renderCuboid(consumer, pose, -0.18f, 0.18f, -0.10f, 0.04f, 0.34f, 0.18f,
                0.58f, 0.70f, 0.76f, 0.60f, light);
    }

    private static void renderDestroyer(VertexConsumer consumer, Matrix4f pose,
                                        DeepOceanRenderer.VisualProfile profile, int light) {
        renderHull(consumer, pose, -0.42f, 0.42f, -0.06f, 0.14f, profile, light);
        renderTurret(consumer, pose, -0.24f, 0.13f, profile, light);
        renderTurret(consumer, pose, 0.24f, 0.13f, profile, light);
    }

    private static void renderCruiser(VertexConsumer consumer, Matrix4f pose,
                                      DeepOceanRenderer.VisualProfile profile, int light) {
        renderHull(consumer, pose, -0.50f, 0.50f, -0.07f, 0.18f, profile, light);
        renderTurret(consumer, pose, -0.28f, 0.16f, profile, light);
        renderTurret(consumer, pose, 0.00f, 0.17f, profile, light);
        renderTurret(consumer, pose, 0.28f, 0.16f, profile, light);
    }

    private static void renderLightCruiser(VertexConsumer consumer, Matrix4f pose,
                                           DeepOceanRenderer.VisualProfile profile, int light) {
        renderHull(consumer, pose, -0.46f, 0.46f, -0.08f, 0.16f, profile, light);
        renderCuboid(consumer, pose, -0.18f, 0.10f, -0.12f, 0.18f, 0.30f, 0.18f,
                0.12f, 0.12f, 0.15f, 0.82f, light);

        renderLightCruiserShoulderRig(consumer, pose, -1.0f, profile, light);
        renderLightCruiserShoulderRig(consumer, pose, 1.0f, profile, light);
        renderLightCruiserSkirt(consumer, pose, profile, light);
        renderLightCruiserTailAnchor(consumer, pose, profile, light);

        renderTurret(consumer, pose, -0.24f, 0.17f, profile, light);
        renderTurret(consumer, pose, 0.24f, 0.17f, profile, light);
        renderLightCruiserGlow(consumer, pose, light);
    }

    private static void renderLightCruiserShoulderRig(VertexConsumer consumer, Matrix4f pose,
                                                      float side,
                                                      DeepOceanRenderer.VisualProfile profile, int light) {
        float inner = side * 0.34f;
        float outer = side * 0.70f;
        float minX = Math.min(inner, outer);
        float maxX = Math.max(inner, outer);
        renderCuboid(consumer, pose, minX, 0.18f, -0.08f, maxX, 0.48f, 0.22f,
                0.09f, 0.09f, 0.11f, 0.90f, light);
        renderCuboid(consumer, pose, minX + 0.04f, 0.24f, -0.15f, maxX - 0.05f, 0.36f, 0.02f,
                profile.r() * 0.72f, profile.g() * 0.66f, profile.b() * 0.82f, 0.78f, light);

        float gunBase = side * 0.58f;
        renderCuboid(consumer, pose, gunBase - 0.035f, 0.34f, -0.48f,
                gunBase + 0.035f, 0.38f, -0.10f, 0.72f, 0.70f, 0.78f, 0.86f, light);
        renderCuboid(consumer, pose, gunBase - 0.12f * side - 0.035f, 0.41f, -0.43f,
                gunBase - 0.12f * side + 0.035f, 0.45f, -0.08f, 0.72f, 0.70f, 0.78f, 0.78f, light);

        renderCuboid(consumer, pose, side * 0.46f - 0.02f, 0.42f, 0.10f,
                side * 0.46f + 0.02f, 0.66f, 0.18f, 0.92f, 0.20f, 0.74f, 0.78f, LightTexture.FULL_BRIGHT);
    }

    private static void renderLightCruiserSkirt(VertexConsumer consumer, Matrix4f pose,
                                                DeepOceanRenderer.VisualProfile profile, int light) {
        renderCuboid(consumer, pose, -0.62f, -0.24f, 0.04f, -0.30f, 0.06f, 0.26f,
                0.10f, 0.10f, 0.12f, 0.80f, light);
        renderCuboid(consumer, pose, 0.30f, -0.24f, 0.04f, 0.62f, 0.06f, 0.26f,
                0.10f, 0.10f, 0.12f, 0.80f, light);
        renderCuboid(consumer, pose, -0.40f, -0.28f, 0.22f, 0.40f, -0.08f, 0.36f,
                profile.r() * 0.45f, profile.g() * 0.44f, profile.b() * 0.54f, 0.70f, light);
        renderCuboid(consumer, pose, -0.54f, -0.06f, 0.25f, 0.54f, -0.02f, 0.33f,
                0.92f, 0.18f, 0.74f, 0.64f, LightTexture.FULL_BRIGHT);
    }

    private static void renderLightCruiserTailAnchor(VertexConsumer consumer, Matrix4f pose,
                                                     DeepOceanRenderer.VisualProfile profile, int light) {
        renderCuboid(consumer, pose, 0.46f, -0.05f, 0.28f, 0.58f, 0.02f, 0.48f,
                profile.r() * 0.44f, profile.g() * 0.42f, profile.b() * 0.52f, 0.78f, light);
        renderCuboid(consumer, pose, 0.56f, -0.14f, 0.46f, 0.66f, -0.06f, 0.66f,
                profile.r() * 0.44f, profile.g() * 0.42f, profile.b() * 0.52f, 0.74f, light);
        renderCuboid(consumer, pose, 0.64f, -0.24f, 0.62f, 0.74f, -0.16f, 0.82f,
                profile.r() * 0.44f, profile.g() * 0.42f, profile.b() * 0.52f, 0.70f, light);

        renderCuboid(consumer, pose, 0.70f, -0.36f, 0.78f, 0.80f, -0.12f, 0.86f,
                0.10f, 0.10f, 0.13f, 0.88f, light);
        renderCuboid(consumer, pose, 0.58f, -0.18f, 0.80f, 0.92f, -0.10f, 0.88f,
                0.10f, 0.10f, 0.13f, 0.88f, light);
        renderCuboid(consumer, pose, 0.56f, -0.42f, 0.80f, 0.70f, -0.32f, 0.88f,
                0.10f, 0.10f, 0.13f, 0.88f, light);
        renderCuboid(consumer, pose, 0.80f, -0.42f, 0.80f, 0.94f, -0.32f, 0.88f,
                0.10f, 0.10f, 0.13f, 0.88f, light);
        renderCuboid(consumer, pose, 0.66f, -0.25f, 0.74f, 0.84f, -0.21f, 0.92f,
                0.98f, 0.20f, 0.82f, 0.60f, LightTexture.FULL_BRIGHT);
    }

    private static void renderLightCruiserGlow(VertexConsumer consumer, Matrix4f pose, int light) {
        int glowLight = LightTexture.FULL_BRIGHT;
        renderCuboid(consumer, pose, -0.08f, 0.56f, -0.08f, 0.08f, 0.62f, 0.02f,
                1.00f, 0.26f, 0.86f, 0.78f, glowLight);
        renderCuboid(consumer, pose, -0.12f, 0.47f, -0.09f, 0.12f, 0.50f, -0.04f,
                1.00f, 0.26f, 0.86f, 0.68f, glowLight);
        renderCuboid(consumer, pose, -0.42f, 0.02f, -0.10f, -0.08f, 0.055f, -0.05f,
                1.00f, 0.20f, 0.80f, 0.70f, glowLight);
        renderCuboid(consumer, pose, 0.08f, 0.02f, -0.10f, 0.42f, 0.055f, -0.05f,
                1.00f, 0.20f, 0.80f, 0.70f, glowLight);
        renderCuboid(consumer, pose, -0.30f, -0.14f, 0.28f, -0.22f, -0.10f, 0.40f,
                1.00f, 0.20f, 0.80f, 0.62f, glowLight);
        renderCuboid(consumer, pose, 0.22f, -0.14f, 0.28f, 0.30f, -0.10f, 0.40f,
                1.00f, 0.20f, 0.80f, 0.62f, glowLight);
    }

    private static void renderBattleship(VertexConsumer consumer, Matrix4f pose,
                                         DeepOceanRenderer.VisualProfile profile, int light) {
        renderHull(consumer, pose, -0.62f, 0.62f, -0.08f, 0.20f, profile, light);
        renderTurret(consumer, pose, -0.34f, 0.18f, profile, light);
        renderTurret(consumer, pose, -0.08f, 0.19f, profile, light);
        renderTurret(consumer, pose, 0.20f, 0.19f, profile, light);
        renderCuboid(consumer, pose, 0.36f, 0.16f, -0.05f, 0.52f, 0.34f, 0.18f,
                profile.r(), profile.g(), profile.b(), profile.alpha(), light);
    }

    private static void renderCarrier(VertexConsumer consumer, Matrix4f pose,
                                      DeepOceanRenderer.VisualProfile profile, float launchPose, int light) {
        renderHull(consumer, pose, -0.58f, 0.58f, -0.08f, 0.18f, profile, light);
        renderCuboid(consumer, pose, -0.50f, 0.09f, -0.16f, 0.50f, 0.13f, 0.26f,
                0.18f, 0.22f, 0.24f, 0.82f, light);
        renderCuboid(consumer, pose, -0.035f, 0.135f, -0.14f, 0.035f, 0.155f, 0.24f,
                0.90f, 0.86f, 0.55f, 0.86f, light);
        renderCuboid(consumer, pose, 0.28f, 0.13f, -0.14f, 0.46f, 0.28f, 0.02f,
                profile.r(), profile.g(), profile.b(), 0.64f, light);
        if (launchPose > 0.02f) {
            renderCuboid(consumer, pose,
                    -0.46f - launchPose * 0.08f, 0.155f, 0.19f + launchPose * 0.06f,
                    0.46f + launchPose * 0.08f, 0.175f, 0.29f + launchPose * 0.10f,
                    0.78f, 0.92f, 1.00f, 0.42f * launchPose, light);
            renderCuboid(consumer, pose,
                    -0.58f - launchPose * 0.08f, 0.125f, 0.18f + launchPose * 0.04f,
                    -0.48f, 0.155f, 0.31f + launchPose * 0.08f,
                    profile.r(), profile.g(), profile.b(), 0.52f * launchPose, light);
            renderCuboid(consumer, pose,
                    0.48f, 0.125f, 0.18f + launchPose * 0.04f,
                    0.58f + launchPose * 0.08f, 0.155f, 0.31f + launchPose * 0.08f,
                    profile.r(), profile.g(), profile.b(), 0.52f * launchPose, light);
        }
    }

    private static void renderSubmarine(VertexConsumer consumer, Matrix4f pose,
                                        DeepOceanRenderer.VisualProfile profile, int light) {
        renderCuboid(consumer, pose, -0.48f, -0.03f, -0.06f, 0.48f, 0.10f, 0.14f,
                profile.r(), profile.g(), profile.b(), profile.alpha(), light);
        renderCuboid(consumer, pose, -0.08f, 0.10f, -0.03f, 0.08f, 0.25f, 0.09f,
                0.28f, 0.42f, 0.74f, 0.72f, light);
    }

    private static int flagshipPhase(AbstractDeepOceanEntity entity) {
        return entity instanceof DeepOceanFlagshipEntity flagship ? flagship.getPhase() : 1;
    }

    private static void renderFlagship(VertexConsumer consumer, Matrix4f pose,
                                       DeepOceanRenderer.VisualProfile profile, int phase,
                                       float ageInTicks, int light) {
        renderBattleship(consumer, pose, profile, light);
        renderCuboid(consumer, pose, -0.72f, 0.16f, 0.08f, -0.58f, 0.42f, 0.24f,
                profile.r() * 0.72f, profile.g() * 0.72f, profile.b(), profile.alpha(), light);
        renderCuboid(consumer, pose, 0.58f, 0.16f, 0.08f, 0.72f, 0.42f, 0.24f,
                profile.r() * 0.72f, profile.g() * 0.72f, profile.b(), profile.alpha(), light);
        renderCuboid(consumer, pose, -0.56f, 0.24f, -0.18f, -0.46f, 0.32f, 0.12f,
                0.78f, 0.76f, 0.88f, 0.78f, light);
        renderCuboid(consumer, pose, 0.46f, 0.24f, -0.18f, 0.56f, 0.32f, 0.12f,
                0.78f, 0.76f, 0.88f, 0.78f, light);
        renderCuboid(consumer, pose, -0.08f, 0.34f, -0.04f, 0.08f, 0.52f, 0.18f,
                0.74f, 0.52f, 0.96f, 0.84f, light);
        renderCuboid(consumer, pose, -0.26f, 0.49f, 0.02f, 0.26f, 0.54f, 0.08f,
                0.88f, 0.72f, 1.00f, 0.74f, light);
        if (phase >= 2) {
            renderFlagshipPhaseTwo(consumer, pose, ageInTicks, light);
        }
        if (phase >= 3) {
            renderFlagshipPhaseThree(consumer, pose, ageInTicks, light);
        }
    }

    private static void renderFlagshipPhaseTwo(VertexConsumer consumer, Matrix4f pose,
                                               float ageInTicks, int light) {
        float pulse = 0.55f + 0.25f * (float) Math.sin(ageInTicks * 0.12f);
        renderCuboid(consumer, pose, -0.38f, 0.30f, 0.20f, -0.28f, 0.68f, 0.28f,
                0.70f, 0.36f, 0.96f, 0.72f, light);
        renderCuboid(consumer, pose, 0.28f, 0.30f, 0.20f, 0.38f, 0.68f, 0.28f,
                0.70f, 0.36f, 0.96f, 0.72f, light);
        renderCuboid(consumer, pose, -0.18f, 0.55f, 0.16f, 0.18f, 0.62f, 0.34f,
                0.94f, 0.76f, 1.00f, pulse, light);
        renderCuboid(consumer, pose, -0.82f, 0.22f, 0.24f, -0.66f, 0.28f, 0.42f,
                0.42f, 0.72f, 1.00f, 0.42f, light);
        renderCuboid(consumer, pose, 0.66f, 0.22f, 0.24f, 0.82f, 0.28f, 0.42f,
                0.42f, 0.72f, 1.00f, 0.42f, light);
    }

    private static void renderFlagshipPhaseThree(VertexConsumer consumer, Matrix4f pose,
                                                 float ageInTicks, int light) {
        float pulse = 0.58f + 0.28f * (float) Math.sin(ageInTicks * 0.18f);
        renderCuboid(consumer, pose, -0.08f, 0.62f, 0.20f, 0.08f, 0.86f, 0.36f,
                0.95f, 0.48f, 1.00f, 0.82f, light);
        renderCuboid(consumer, pose, -0.62f, 0.42f, 0.20f, -0.48f, 0.62f, 0.35f,
                0.90f, 0.38f, 0.96f, 0.72f, light);
        renderCuboid(consumer, pose, 0.48f, 0.42f, 0.20f, 0.62f, 0.62f, 0.35f,
                0.90f, 0.38f, 0.96f, 0.72f, light);
        renderCuboid(consumer, pose, -0.92f, 0.08f, 0.18f, -0.78f, 0.44f, 0.30f,
                0.70f, 0.18f, 0.92f, 0.76f, light);
        renderCuboid(consumer, pose, 0.78f, 0.08f, 0.18f, 0.92f, 0.44f, 0.30f,
                0.70f, 0.18f, 0.92f, 0.76f, light);
        renderCuboid(consumer, pose, -0.72f, 0.02f, 0.34f, 0.72f, 0.055f, 0.46f,
                0.98f, 0.42f, 1.00f, pulse, light);
        renderCuboid(consumer, pose, -0.30f, 0.70f, 0.24f, 0.30f, 0.735f, 0.48f,
                0.98f, 0.42f, 1.00f, pulse * 0.82f, light);
    }

    private static void renderHull(VertexConsumer consumer, Matrix4f pose,
                                   float minX, float maxX, float minY, float maxY,
                                   DeepOceanRenderer.VisualProfile profile, int light) {
        renderCuboid(consumer, pose, minX, minY, -0.08f, maxX, maxY, 0.18f,
                profile.r(), profile.g(), profile.b(), profile.alpha(), light);
        renderCuboid(consumer, pose, minX - 0.08f, minY + 0.03f, -0.02f, minX + 0.06f, maxY + 0.04f, 0.22f,
                profile.r() * 0.72f, profile.g() * 0.72f, profile.b() * 0.78f, profile.alpha() * 0.9f, light);
        renderCuboid(consumer, pose, maxX - 0.06f, minY + 0.03f, -0.02f, maxX + 0.08f, maxY + 0.04f, 0.22f,
                profile.r() * 0.72f, profile.g() * 0.72f, profile.b() * 0.78f, profile.alpha() * 0.9f, light);
    }

    private static void renderTurret(VertexConsumer consumer, Matrix4f pose, float x,
                                     float y, DeepOceanRenderer.VisualProfile profile, int light) {
        renderCuboid(consumer, pose, x - 0.08f, y, -0.13f, x + 0.08f, y + 0.08f, 0.03f,
                profile.r() * 0.88f, profile.g() * 0.88f, profile.b() * 0.94f, profile.alpha(), light);
        renderCuboid(consumer, pose, x - 0.025f, y + 0.035f, -0.29f, x + 0.025f, y + 0.065f, -0.12f,
                0.72f, 0.76f, 0.82f, 0.78f, light);
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
