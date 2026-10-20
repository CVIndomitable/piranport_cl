package com.piranport.dungeon.client;

import com.mojang.blaze3d.vertex.PoseStack;
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
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * 副本出口传送门的实体渲染器。
 *
 * <p><b>这里刻意不再画门（方案 A）。</b>出口的可见主体是
 * {@code PortalStructureHelper} 摆出的 4×5×4 {@code dungeon_portal} 方块门框；
 * 实体本身只承担两件事：走进判定（{@code DungeonPortalEntity.tick}）和
 * 门头的标签。此前这里还额外画了一个自转的紫色小门，叠在方块门框里，看起来像同一位置有两扇门。</p>
 *
 * <p>标签按本地玩家自己的快照着色：条件满足绿色、未满足红色；没有快照（尚未走近）时条件灰色、不表态。
 * 隐藏门不画标签，让玩家只看到确实可走的路线。</p>
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

    public DungeonPortalRenderer(EntityRendererProvider.Context context) {
        super(context);
        // 实体是不可见的触发体，不投影子，否则开口地面上会出现一块莫名的圆影。
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(DungeonPortalEntity entity, float entityYaw, float partialTick,
                        PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
        renderLabel(entity, poseStack, buffer);
    }

    /** 旧存档的单门 target 为空，不显示标签。 */
    private void renderLabel(DungeonPortalEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
        String target = entity.getTargetNodeId();
        if (target.isBlank()) return;
        List<Row> rows = buildRows(entity, target);
        if (rows.isEmpty()) return;

        Font font = Minecraft.getInstance().font;
        poseStack.pushPose();
        poseStack.translate(0.0, 1.25, 0.0);
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
