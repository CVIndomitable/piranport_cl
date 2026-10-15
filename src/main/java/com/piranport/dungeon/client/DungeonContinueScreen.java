package com.piranport.dungeon.client;

import com.piranport.dungeon.block.DungeonLecternBlockEntity;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.key.DungeonKeyItem;
import com.piranport.dungeon.key.DungeonProgress;
import com.piranport.dungeon.network.ClientDungeonData;
import com.piranport.dungeon.network.SelectNodePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.Set;

/**
 * 整合版 §3.1 讲台入口"继续/从头开始"对话框。
 *
 * <p>布局：左栏标题 + 继续/从头开始/取消按钮；右栏节点图
 * （{@link DungeonNodeMapRenderer}，与 DungeonBookScreen 同款三态配色）。
 * 节点状态读自讲台 BE 同步下来的钥匙 component——服务端每次进度变化都会经
 * syncKey 写钥匙并由 BE 推 update tag，客户端只在渲染时读，不周期重读。</p>
 *
 * <p>点击可达且未通关的节点会直接发 {@link SelectNodePayload} 进本
 * （等价于在书台节点图上点节点）。</p>
 */
public class DungeonContinueScreen extends Screen {
    private final BlockPos lecternPos;
    private final String stageName;
    private final int clearedNodeCount;

    /** 右栏节点图的屏幕坐标缓存（render 时计算，mouseClicked 复用）。 */
    private Map<String, int[]> nodePositions = Map.of();

    public DungeonContinueScreen(BlockPos lecternPos, String stageName, int clearedNodeCount) {
        super(Component.literal("副本入口"));
        this.lecternPos = lecternPos;
        this.stageName = stageName;
        this.clearedNodeCount = clearedNodeCount;
    }

    /** 左栏宽度；右栏留给节点图。 */
    private int leftColWidth() { return Math.min(240, this.width / 3); }

    @Override
    protected void init() {
        int cx = leftColWidth() / 2;
        int cy = this.height / 2;

        // 继续按钮
        addRenderableWidget(Button.builder(
                Component.literal("继续（最新记录点）"),
                btn -> {
                    PacketDistributor.sendToServer(
                            new com.piranport.dungeon.network.ContinueFromCheckpointPayload(lecternPos));
                    onClose();
                }
        ).bounds(cx - 100, cy + 20, 200, 20).build());

        // 从头开始按钮
        addRenderableWidget(Button.builder(
                Component.literal("从头开始"),
                btn -> {
                    PacketDistributor.sendToServer(
                            new com.piranport.dungeon.network.RestartFromBeginningPayload(lecternPos));
                    onClose();
                }
        ).bounds(cx - 100, cy + 50, 200, 20).build());

        // 取消按钮
        addRenderableWidget(Button.builder(
                Component.literal("取消"),
                btn -> onClose()
        ).bounds(cx - 100, cy + 80, 200, 20).build());
    }

    /** 从客户端讲台 BE 上同步下来的钥匙里取进度；讲台 BE 的 KeyStack 随 update tag 下发。 */
    private DungeonProgress progress() {
        return DungeonNodeMapRenderer.progressFromLectern(lecternPos);
    }

    /** 从钥匙上的 stageId 查关卡数据（客户端注册表已同步）。 */
    private StageData stage() {
        if (minecraft == null || minecraft.level == null) return null;
        if (!(minecraft.level.getBlockEntity(lecternPos)
                instanceof DungeonLecternBlockEntity be)) return null;
        var stack = be.getKeyStack();
        if (!(stack.getItem() instanceof DungeonKeyItem)) return null;
        // 客户端拿不到服务端 DungeonRegistry，走已同步的 ClientDungeonData
        String stageId = DungeonKeyItem.getStageId(stack);
        StageData s = ClientDungeonData.getStage(stageId);
        if (s == null) {
            // chapter_* 钥匙：resolveStageId 依赖服务端注册表，这里用章节首关卡回退
            String resolved = resolveStageClientSide(stageId);
            if (resolved != null) s = ClientDungeonData.getStage(resolved);
        }
        return s;
    }

    /** 客户端侧解析 chapter_* → 首关卡 ID（对应服务端 DungeonKeyItem.resolveStageId）。 */
    private String resolveStageClientSide(String id) {
        if (id == null || !id.startsWith("chapter_")) return null;
        for (var ch : ClientDungeonData.getSortedChapters()) {
            if (id.equals(ch.chapterId()) && !ch.stages().isEmpty()) {
                return ch.stages().get(0);
            }
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 节点图点击：命中可达未通关节点 → 直接进本（keySlot 走讲台钥匙槽位约定 -1，
        // 服务端 DungeonEntryService 以讲台为权威，keySlot 不参与校验）
        if (button == 0) {
            String hit = DungeonNodeMapRenderer.hitTest(nodePositions, mouseX, mouseY);
            if (hit != null) {
                StageData st = stage();
                DungeonProgress progress = progress();
                if (st != null) {
                    Set<String> reachable = DungeonNodeMapRenderer.computeReachable(st, progress);
                    if (reachable.contains(hit) && !progress.clearedNodes().contains(hit)) {
                        PacketDistributor.sendToServer(
                                new SelectNodePayload(lecternPos, -1, hit));
                        onClose();
                    }
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        // 暗背景
        gfx.fill(0, 0, this.width, this.height, 0xCC1A1A2E);
        super.render(gfx, mouseX, mouseY, partialTick);

        // 左栏标题与信息
        int cx = leftColWidth() / 2;
        gfx.drawCenteredString(this.font, "§6副本入口", cx, this.height / 2 - 50, 0xFFFFD700);
        gfx.drawCenteredString(this.font, "§f" + stageName, cx, this.height / 2 - 30, 0xFFFFFFFF);
        gfx.drawCenteredString(this.font,
                "§7已通关节点: §f" + clearedNodeCount, cx, this.height / 2 - 10, 0xFFCCCCCC);

        // 右栏节点图
        StageData st = stage();
        if (st != null) {
            int gx = leftColWidth() + 10;
            int gy = 20;
            int gw = this.width - gx - 10;
            int gh = this.height - 40;
            nodePositions = DungeonNodeMapRenderer.layout(st, gx, gy, gw, gh);
            DungeonNodeMapRenderer.render(gfx, this.font, st, nodePositions,
                    progress(), mouseX, mouseY);
            // 图例贴右栏底部
            DungeonNodeMapRenderer.renderLegend(gfx, this.font, gx, this.height - 20);
        } else {
            nodePositions = Map.of();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
