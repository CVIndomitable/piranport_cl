package com.piranport.dungeon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.piranport.dungeon.data.BranchRule;
import com.piranport.dungeon.data.NodeData;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.entity.DungeonPortalEntity;
import com.piranport.dungeon.event.BranchDoorText;
import com.piranport.dungeon.event.BranchDoors;
import com.piranport.dungeon.network.ClientDungeonData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * 副本出口传送门的实体渲染器：画一片漂浮在水面上的门面 + 门头的去向标签。
 *
 * <p><b>门体（《副本/26》2026-10-22 补充）：</b>出口是"一片式"——没有门框、没有平台、
 * 没有光柱。门面用原版末地门的渲染类型（{@code RenderType.endPortal()}，末地天空 +
 * 末地传送门的双贴图），紫黑漩涡的观感和下界门同源，且不需要我们额外准备贴图。</p>
 *
 * <p><b>为什么门面只绕 Y 轴转向镜头：</b>没有门框时，固定朝向的平面从侧面看会退化成
 * 一条线，玩家会以为门消失了。只绕 Y 轴转向镜头能把门面始终立在玩家眼前（保持竖直），
 * 又不会像完全公告板那样跟着俯仰歪倒；代价是不给实体加一个"朝向"同步字段。</p>
 *
 * <p>标签按本地玩家自己的快照着色：条件满足绿色、未满足红色；没有快照（尚未走近）时
 * 条件灰色、不表态。隐藏门不画标签，让玩家只看到确实可走的路线。</p>
 *
 * <p>环境粒子仍由 {@link DungeonPortalEntity#tick()} 在客户端发出，不在这里绘制。</p>
 */
public class DungeonPortalRenderer extends EntityRenderer<DungeonPortalEntity> {

    private static final int COLOR_TITLE = 0xFFFFFFFF;
    private static final int COLOR_DIM = 0xFF888888;
    private static final int COLOR_OBJECTIVE = 0xFFCCCCCC;
    private static final int COLOR_NEUTRAL = 0xFFAAAAAA;
    private static final int COLOR_MET = 0xFF55FF55;
    private static final int COLOR_UNMET = 0xFFFF5555;
    /** 标签行距（字体像素）；渲染缩放 0.025 下约为 0.25 格，门宽 4 格内可放下数行。 */
    private static final int LINE_PIXELS = 10;
    /** 门面半宽（格）：整片 2 格宽，与水上方的一格门面比例一致。 */
    private static final float SHEET_HALF_WIDTH = 1.0F;
    /** 门面高度（格）：3 格，下沿贴着水面（实体原点就在水面高度）。 */
    private static final float SHEET_HEIGHT = 3.0F;

    public DungeonPortalRenderer(EntityRendererProvider.Context context) {
        super(context);
        // 一片薄门面投圆影会很突兀，水面上的门不投影子。
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(DungeonPortalEntity entity, float entityYaw, float partialTick,
                        PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
        renderSheet(poseStack, buffer);
        renderLabel(entity, poseStack, buffer);
    }

    /**
     * 画一片竖直门面。
     *
     * <p>顶点顺序按原版 {@code TheEndPortalRenderer} 的正面绕序（左下→右下→右上→左上），
     * 因为 {@code RenderType.endPortal()} 用的是默认的背面剔除。</p>
     */
    private void renderSheet(PoseStack poseStack, MultiBufferSource buffer) {
        float yaw = Minecraft.getInstance().gameRenderer.getMainCamera().getYRot();
        poseStack.pushPose();
        // 让门面的 +Z 法线指向镜头：镜头视线为 (-sinYaw, 0, cosYaw)，
        // 绕 Y 轴转 (180 - yaw) 度后平面法线正好与之相反。
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        VertexConsumer consumer = buffer.getBuffer(RenderType.endPortal());
        Matrix4f pose = poseStack.last().pose();
        consumer.addVertex(pose, -SHEET_HALF_WIDTH, 0.0F, 0.0F);
        consumer.addVertex(pose, SHEET_HALF_WIDTH, 0.0F, 0.0F);
        consumer.addVertex(pose, SHEET_HALF_WIDTH, SHEET_HEIGHT, 0.0F);
        consumer.addVertex(pose, -SHEET_HALF_WIDTH, SHEET_HEIGHT, 0.0F);
        poseStack.popPose();
    }

    /** 旧存档的单门 target 为空，不显示标签。 */
    private void renderLabel(DungeonPortalEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
        String target = entity.getTargetNodeId();
        if (target.isBlank()) return;
        List<Row> rows = buildRows(entity, target);
        if (rows.isEmpty()) return;

        Font font = Minecraft.getInstance().font;
        poseStack.pushPose();
        // 标签放在门面正上方：门面高 3 格，再抬 0.4 格，离门顶留一点空隙。
        poseStack.translate(0.0, SHEET_HEIGHT + 0.4, 0.0);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(-0.025f, -0.025f, 0.025f);
        var pose = poseStack.last().pose();
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            // 门内文字用满亮，夜间/水下也能看清每扇门的去向。
            font.drawInBatch(row.text(), -font.width(row.text()) / 2.0f, i * LINE_PIXELS, row.color(),
                    false, pose, buffer, Font.DisplayMode.NORMAL, 0, 0xF000F0);
        }
        poseStack.popPose();
    }

    private static List<Row> buildRows(DungeonPortalEntity entity, String target) {
        StageData stage = ClientDungeonData.getStage(entity.getStageId());
        NodeData targetNode = stage == null ? null : stage.nodes().get(target);
        if (targetNode == null) {
            return List.of(new Row(Component.literal("→ " + target), COLOR_TITLE));
        }

        BranchDoors.Door door = BranchDoorClientState.find(
                entity.getInstanceKey(), entity.getSourceNodeId(), target);
        BranchDoors.State state = door == null ? null : door.state();
        if (state == BranchDoors.State.HIDDEN) return List.of();

        List<Row> rows = new ArrayList<>();
        boolean cleared = state == BranchDoors.State.DARK && door.unmetMask() == 0;
        rows.add(new Row(Component.literal("→ " + targetNode.label()),
                state == null || state == BranchDoors.State.LIT ? COLOR_TITLE : COLOR_DIM));

        if (state == BranchDoors.State.PASSED) {
            rows.add(new Row(Component.translatable("dungeon.piranport.branch.passed"), COLOR_DIM));
            return rows;
        }
        if (cleared) {
            rows.add(new Row(Component.translatable("dungeon.piranport.branch.cleared"), COLOR_DIM));
            return rows;
        }

        String objective = entity.getObjectiveKey();
        if (!objective.isBlank()) {
            rows.add(new Row(Component.translatable(objective), COLOR_OBJECTIVE));
        }

        BranchRule rule = ruleFor(stage, entity.getSourceNodeId(), door, target);
        if (rule != null) {
            for (BranchDoorText.Line line : BranchDoorText.conditions(rule)) {
                int color;
                if (state == null) color = COLOR_NEUTRAL;
                else if ((door.unmetMask() & line.bit()) != 0) color = COLOR_UNMET;
                else color = COLOR_MET;
                rows.add(new Row(line.text(), color));
            }
        }
        return rows;
    }

    /** 门对应的展示规则：优先用服务端定格的 ruleIndex，无快照时取第一条指向该目标的规则。 */
    private static BranchRule ruleFor(StageData stage, String sourceNodeId, BranchDoors.Door door, String target) {
        NodeData source = stage.nodes().get(sourceNodeId);
        if (source == null) return null;
        List<BranchRule> rules = source.branches();
        if (door != null && door.ruleIndex() >= 0 && door.ruleIndex() < rules.size()) {
            return rules.get(door.ruleIndex());
        }
        for (BranchRule rule : rules) {
            if (rule.to().equals(target)) return rule;
        }
        return null;
    }

    @Override
    public ResourceLocation getTextureLocation(DungeonPortalEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/entity/end_portal.png");
    }

    private record Row(Component text, int color) {}
}
