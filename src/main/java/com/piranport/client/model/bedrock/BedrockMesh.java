package com.piranport.client.model.bedrock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.Direction;
import org.joml.Quaternionf;

import java.util.Map;

/**
 * 把 Bedrock 几何画到 Java 实体渲染管线里。
 *
 * <p>坐标转换：Bedrock 与 Java 模型空间只差 Y 轴（Bedrock 脚底 y=0 向上，
 * Java 脚底 y=24 向下），所以顶点取 {@code y = -(yBedrock - bonePivot)}，
 * 根骨骼补 24 单位让 LivingEntityRenderer 的 -1.501 位移正好落在脚底。
 * X/Z 不翻转——YSM 模型与原版玩家模型一样面朝 -Z、左侧为 +X。</p>
 *
 * <p>面顶点顺序与 ModelPart.Cube 的箱式 UV 完全一致，逐面 UV 只替换矩形。</p>
 */
public final class BedrockMesh {
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
    /** YSM 模型的整体缩放（ysm.json properties.height_scale / width_scale）。 */
    public static final float YSM_SCALE = 0.7F;

    private final BedrockGeometry geometry;
    private final float scale;
    /** 每根骨骼相对父骨骼的位移（模型单位，已乘缩放）。 */
    private final float[] localOffsets;
    private final float[] bindRotations;

    public BedrockMesh(BedrockGeometry geometry, float scale) {
        this.geometry = geometry;
        this.scale = scale;
        int boneCount = geometry.bones.size();
        this.localOffsets = new float[boneCount * 3];
        this.bindRotations = new float[boneCount * 3];
        for (int i = 0; i < boneCount; i++) {
            BedrockGeometry.Bone bone = geometry.bones.get(i);
            BedrockGeometry.Bone parent = bone.parent >= 0 ? geometry.bones.get(bone.parent) : null;
            if (parent == null) {
                // 根骨骼：补 24 单位地面偏移
                localOffsets[i * 3] = bone.pivot[0] * scale;
                localOffsets[i * 3 + 1] = 24.0F - bone.pivot[1] * scale;
                localOffsets[i * 3 + 2] = bone.pivot[2] * scale;
            } else {
                localOffsets[i * 3] = (bone.pivot[0] - parent.pivot[0]) * scale;
                localOffsets[i * 3 + 1] = -(bone.pivot[1] - parent.pivot[1]) * scale;
                localOffsets[i * 3 + 2] = (bone.pivot[2] - parent.pivot[2]) * scale;
            }
            bindRotations[i * 3] = bone.rotation[0];
            bindRotations[i * 3 + 1] = bone.rotation[1];
            bindRotations[i * 3 + 2] = bone.rotation[2];
        }
    }

    public BedrockGeometry geometry() {
        return geometry;
    }

    public Map<String, Integer> boneIndex() {
        return geometry.indexByName;
    }

    /**
     * @param poses 每根骨骼 9 个分量：rotation xyz（度）、position xyz（Bedrock 格）、scale xyz
     */
    public void render(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color, float[] poses) {
        for (int root : geometry.roots) {
            renderBone(root, pose, buffer, light, overlay, color, poses);
        }
    }

    private void renderBone(int index, PoseStack pose, VertexConsumer buffer,
                            int light, int overlay, int color, float[] poses) {
        BedrockGeometry.Bone bone = geometry.bones.get(index);
        if (bone.cubes.isEmpty() && bone.children.isEmpty()) return;

        int base = index * 9;
        pose.pushPose();

        // 位移：骨骼自身绑定位置 + 动画位移；动画 y 需要按 Bedrock→Java 取反
        float px = localOffsets[index * 3] + poses[base + 3] * scale;
        float py = localOffsets[index * 3 + 1] - poses[base + 4] * scale;
        float pz = localOffsets[index * 3 + 2] + poses[base + 5] * scale;
        pose.translate(px / 16.0F, py / 16.0F, pz / 16.0F);

        // 旋转：绑定旋转 + 动画旋转（叠加），ZYX 欧拉序与 Blockbench/ModelPart 一致
        float rx = (bindRotations[index * 3] + poses[base]) * DEG_TO_RAD;
        float ry = (bindRotations[index * 3 + 1] + poses[base + 1]) * DEG_TO_RAD;
        float rz = (bindRotations[index * 3 + 2] + poses[base + 2]) * DEG_TO_RAD;
        if (rx != 0.0F || ry != 0.0F || rz != 0.0F) {
            pose.mulPose(new Quaternionf().rotationZYX(rz, ry, rx));
        }

        float sx = poses[base + 6];
        float sy = poses[base + 7];
        float sz = poses[base + 8];
        if (sx != 1.0F || sy != 1.0F || sz != 1.0F) {
            pose.scale(sx, sy, sz);
        }

        for (BedrockGeometry.Cube cube : bone.cubes) {
            renderCube(bone, cube, pose, buffer, light, overlay, color);
        }
        for (int child : bone.children) {
            renderBone(child, pose, buffer, light, overlay, color, poses);
        }
        pose.popPose();
    }

    private void renderCube(BedrockGeometry.Bone bone, BedrockGeometry.Cube cube, PoseStack pose,
                            VertexConsumer buffer, int light, int overlay, int color) {
        boolean rotated = cube.isRotated();
        if (rotated) {
            pose.pushPose();
            // cube pivot 相对骨骼 pivot；旋转后再平移回来，相当于绕 cube pivot 旋转
            float dx = (cube.pivot[0] - bone.pivot[0]) * scale / 16.0F;
            float dy = -(cube.pivot[1] - bone.pivot[1]) * scale / 16.0F;
            float dz = (cube.pivot[2] - bone.pivot[2]) * scale / 16.0F;
            pose.translate(dx, dy, dz);
            pose.mulPose(new Quaternionf().rotationZYX(
                    cube.rotation[2] * DEG_TO_RAD,
                    cube.rotation[1] * DEG_TO_RAD,
                    cube.rotation[0] * DEG_TO_RAD));
            pose.translate(-dx, -dy, -dz);
        }

        for (Map.Entry<Direction, BedrockGeometry.FaceUv> entry : cube.faces.entrySet()) {
            BedrockGeometry.FaceUv uv = entry.getValue();
            if (!uv.isVisible()) continue;
            emitFace(bone, cube, entry.getKey(), uv, pose, buffer, light, overlay, color);
        }

        if (rotated) pose.popPose();
    }

    private void emitFace(BedrockGeometry.Bone bone, BedrockGeometry.Cube cube, Direction direction,
                          BedrockGeometry.FaceUv uv, PoseStack pose,
                          VertexConsumer buffer, int light, int overlay, int color) {
        float ox = cube.origin[0];
        float oy = cube.origin[1];
        float oz = cube.origin[2];
        float x0 = Math.min(ox, ox + cube.size[0]);
        float x1 = Math.max(ox, ox + cube.size[0]);
        float y0 = Math.min(oy, oy + cube.size[1]);
        float y1 = Math.max(oy, oy + cube.size[1]);
        float z0 = Math.min(oz, oz + cube.size[2]);
        float z1 = Math.max(oz, oz + cube.size[2]);

        float[][] corners = switch (direction) {
            case NORTH -> new float[][]{
                    {x1, y1, z0}, {x0, y1, z0}, {x0, y0, z0}, {x1, y0, z0}};
            case SOUTH -> new float[][]{
                    {x0, y1, z1}, {x1, y1, z1}, {x1, y0, z1}, {x0, y0, z1}};
            case EAST -> new float[][]{
                    {x1, y1, z1}, {x1, y1, z0}, {x1, y0, z0}, {x1, y0, z1}};
            case WEST -> new float[][]{
                    {x0, y1, z0}, {x0, y1, z1}, {x0, y0, z1}, {x0, y0, z0}};
            case UP -> new float[][]{
                    {x1, y1, z1}, {x0, y1, z1}, {x0, y1, z0}, {x1, y1, z0}};
            case DOWN -> new float[][]{
                    {x1, y0, z0}, {x0, y0, z0}, {x0, y0, z1}, {x1, y0, z1}};
        };

        float u1 = uv.u1() / geometry.textureWidth;
        float v1 = uv.v1() / geometry.textureHeight;
        float u2 = uv.u2() / geometry.textureWidth;
        float v2 = uv.v2() / geometry.textureHeight;
        float[] us = {u2, u1, u1, u2};
        float[] vs = {v1, v1, v2, v2};

        float nx = direction.getStepX();
        float ny = -direction.getStepY();
        float nz = direction.getStepZ();

        PoseStack.Pose last = pose.last();
        float sx = scale / 16.0F;
        for (int i = 0; i < 4; i++) {
            float[] corner = corners[i];
            buffer.addVertex(last,
                            (corner[0] - bone.pivot[0]) * sx,
                            -(corner[1] - bone.pivot[1]) * sx,
                            (corner[2] - bone.pivot[2]) * sx)
                    .setColor(color)
                    .setUv(us[i], vs[i])
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal(last, nx, ny, nz);
        }
    }
}
