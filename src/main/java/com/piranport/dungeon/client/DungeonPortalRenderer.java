package com.piranport.dungeon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.piranport.dungeon.entity.DungeonPortalEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * 副本出口传送门的实体渲染器。
 *
 * <p><b>这里刻意不再画门（方案 A）。</b>出口的可见主体是
 * {@code PortalStructureHelper} 摆出的 4×5×4 {@code dungeon_portal} 方块门框；
 * 实体本身只承担两件事：走进判定（{@code DungeonPortalEntity.tick}）和
 * 多后继门头顶的"→ 目标节点"文字。此前这里还额外画了一个自转的紫色小门，
 * 叠在方块门框里，看起来像同一位置有两扇门。</p>
 *
 * <p>环境粒子仍由 {@link DungeonPortalEntity#tick()} 在客户端发出，不在这里绘制。</p>
 */
public class DungeonPortalRenderer extends EntityRenderer<DungeonPortalEntity> {

    public DungeonPortalRenderer(EntityRendererProvider.Context context) {
        super(context);
        // 实体是不可见的触发体，不投影子，否则开口地面上会出现一块莫名的圆影。
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(DungeonPortalEntity entity, float entityYaw, float partialTick,
                        PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
        renderTargetLabel(entity, poseStack, buffer, packedLight);
    }

    /**
     * 多扇门时在门上方标出去向。旧存档的单门 target 为空，不显示。
     * 使用面向摄像机的 billboard 文本，远处也能分辨每扇门通向哪个节点。
     */
    private void renderTargetLabel(DungeonPortalEntity entity, PoseStack poseStack,
                                   MultiBufferSource buffer, int packedLight) {
        String target = entity.getTargetNodeId();
        if (target == null || target.isBlank()) return;
        Component text = Component.literal("→ " + target);
        Font font = Minecraft.getInstance().font;
        poseStack.pushPose();
        poseStack.translate(0.0, 1.25, 0.0);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(-0.025f, -0.025f, 0.025f);
        // 门内文字用满亮，夜间/水下也能看清每扇门的去向。
        font.drawInBatch(text, -font.width(text) / 2.0f, 0.0f, 0xFFFFFFFF, false,
                poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, 0xF000F0);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(DungeonPortalEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/entity/end_portal.png");
    }
}
