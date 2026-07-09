package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;

/** Small immediate-mode shapes for lightweight entity silhouettes. */
public final class SimpleEntityRenderShapes {

    private SimpleEntityRenderShapes() {
    }

    public static VertexConsumer translucentConsumer(MultiBufferSource buffer) {
        return buffer.getBuffer(RenderType.translucent());
    }

    public static void renderCuboid(PoseStack poseStack, MultiBufferSource buffer, int light,
                                    float minX, float minY, float minZ,
                                    float maxX, float maxY, float maxZ,
                                    float r, float g, float b, float alpha) {
        renderCuboid(translucentConsumer(buffer), poseStack.last().pose(),
                minX, minY, minZ, maxX, maxY, maxZ, r, g, b, alpha, light);
    }

    public static void renderCuboid(VertexConsumer consumer, Matrix4f pose,
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
