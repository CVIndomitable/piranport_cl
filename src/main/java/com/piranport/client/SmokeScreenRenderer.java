package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.piranport.block.entity.SmokeScreenBlockEntity;
import com.piranport.registry.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;

/**
 * 烟雾方块的自定义渲染器，根据玩家位置动态调整透明度。
 *
 * <p>设计要点：
 * <ul>
 * <li>BlockEntityRenderer 是单例，所有同类型 BE 共享一个实例</li>
 * <li>使用 per-tick 缓存避免重复检测玩家位置</li>
 * <li>检测范围覆盖玩家碰撞箱（1.8 格高）周围 3x2x3 区域</li>
 * </ul>
 */
public class SmokeScreenRenderer implements BlockEntityRenderer<SmokeScreenBlockEntity> {

    private static final float SMOKE_R = 0.7f;
    private static final float SMOKE_G = 0.7f;
    private static final float SMOKE_B = 0.7f;
    private static final float ALPHA_INSIDE = 0.25f;
    private static final float ALPHA_OUTSIDE = 1.0f;

    // per-tick 缓存（单例共享）：同一 tick 内所有 BE 共享一次检测结果，
    // 避免 18 个 BE 每帧各做 18 次方块查询（总计 324 次）
    private long lastCheckTick = -1;
    private boolean cachedInsideSmoke = false;

    public SmokeScreenRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(SmokeScreenBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Level level = be.getLevel();
        Player player = Minecraft.getInstance().player;
        if (level == null || player == null) return;

        long currentTick = level.getGameTime();
        if (currentTick != lastCheckTick) {
            cachedInsideSmoke = isPlayerInsideSmoke(level, player);
            lastCheckTick = currentTick;
        }

        float alpha = cachedInsideSmoke ? ALPHA_INSIDE : ALPHA_OUTSIDE;

        VertexConsumer consumer = buffer.getBuffer(RenderType.translucent());
        Matrix4f pose = poseStack.last().pose();

        renderCubeFaces(consumer, pose, SMOKE_R, SMOKE_G, SMOKE_B, alpha,
                packedLight, packedOverlay);
    }

    /**
     * 检测玩家是否在烟雾方块内部。
     * 检测范围：玩家脚部位置周围 3x2x3 = 18 格，覆盖玩家 0.6 宽 x 1.8 高碰撞箱。
     * 使用 betweenClosedStream + anyMatch 实现短路优化。
     */
    private boolean isPlayerInsideSmoke(Level level, Player player) {
        BlockPos playerPos = player.blockPosition();
        return BlockPos.betweenClosedStream(
                playerPos.offset(-1, 0, -1),
                playerPos.offset(1, 1, 1)
        ).anyMatch(pos -> level.getBlockState(pos).is(ModBlocks.SMOKE_SCREEN.get()));
    }

    /**
     * 渲染立方体的 6 个面。顶点顺序遵循逆时针绕序（从面外侧看）。
     */
    private void renderCubeFaces(VertexConsumer consumer, Matrix4f pose,
                                 float r, float g, float b, float alpha,
                                 int light, int overlay) {
        // 下面 (Y-)
        addQuad(consumer, pose,
                0, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1,
                r, g, b, alpha, 0, -1, 0, light, overlay);

        // 上面 (Y+)
        addQuad(consumer, pose,
                0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0,
                r, g, b, alpha, 0, 1, 0, light, overlay);

        // 南面 (Z+)
        addQuad(consumer, pose,
                0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1, 1,
                r, g, b, alpha, 0, 0, 1, light, overlay);

        // 北面 (Z-)
        addQuad(consumer, pose,
                0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0,
                r, g, b, alpha, 0, 0, -1, light, overlay);

        // 西面 (X-)
        addQuad(consumer, pose,
                0, 0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 0,
                r, g, b, alpha, -1, 0, 0, light, overlay);

        // 东面 (X+)
        addQuad(consumer, pose,
                1, 0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1,
                r, g, b, alpha, 1, 0, 0, light, overlay);
    }

    private static void addQuad(VertexConsumer consumer, Matrix4f pose,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float x3, float y3, float z3,
                                float x4, float y4, float z4,
                                float r, float g, float b, float alpha,
                                float nx, float ny, float nz,
                                int light, int overlay) {
        consumer.addVertex(pose, x1, y1, z1)
                .setColor(r, g, b, alpha)
                .setUv(0, 0)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);

        consumer.addVertex(pose, x2, y2, z2)
                .setColor(r, g, b, alpha)
                .setUv(1, 0)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);

        consumer.addVertex(pose, x3, y3, z3)
                .setColor(r, g, b, alpha)
                .setUv(1, 1)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);

        consumer.addVertex(pose, x4, y4, z4)
                .setColor(r, g, b, alpha)
                .setUv(0, 1)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
    }

    @Override
    public boolean shouldRenderOffScreen(SmokeScreenBlockEntity be) {
        return false;
    }

    @Override
    public int getViewDistance() {
        return 64;
    }
}
