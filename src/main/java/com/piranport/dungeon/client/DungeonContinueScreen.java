package com.piranport.dungeon.client;

import com.piranport.dungeon.block.DungeonLecternBlockEntity;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.key.DungeonKeyItem;
import com.piranport.dungeon.key.DungeonProgress;
import com.piranport.dungeon.network.ClientDungeonData;
import com.piranport.dungeon.network.SelectNodePayload;
import com.piranport.dungeon.network.SelectStagePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 整合版 §3.1 讲台入口"继续/从头开始"对话框。
 *
 * <p>布局：左栏顶部是章节钥匙可切换的本章关卡列表（普通钥匙不显示）；左栏中部标题 +
 * 继续/从头开始/取消按钮；右栏节点图（{@link DungeonNodeMapRenderer}）。
 * 关卡名、节点图都从讲台 BE 读取，换关后随 BE 同步自动刷新。</p>
 *
 * <p>点击可达且未通关的节点会直接发 {@link SelectNodePayload} 进本
 * （等价于在书台节点图上点节点）。点击本章其他关会发 {@link SelectStagePayload} 请求换关。</p>
 */
public class DungeonContinueScreen extends Screen {
    private static final int STAGE_ROW_TOP = 28;
    private static final int STAGE_ROW_HEIGHT = 18;

    private final BlockPos lecternPos;

    /** 右栏节点图的屏幕坐标缓存（render 时计算，mouseClicked 复用）。 */
    private Map<String, int[]> nodePositions = Map.of();

    /** 章节钥匙通关后没有绑定实例，直接进本按钮必须置灰，只能选关。 */
    private Button continueButton;
    private Button restartButton;

    public DungeonContinueScreen(BlockPos lecternPos) {
        super(Component.literal("副本入口"));
        this.lecternPos = lecternPos;
    }

    /** 左栏宽度；右栏留给节点图。 */
    private int leftColWidth() { return Math.min(240, this.width / 3); }

    @Override
    protected void init() {
        int cx = leftColWidth() / 2;
        int cy = this.height / 2;

        // 继续按钮
        continueButton = addRenderableWidget(Button.builder(
                Component.literal("继续（最新记录点）"),
                btn -> {
                    PacketDistributor.sendToServer(
                            new com.piranport.dungeon.network.ContinueFromCheckpointPayload(lecternPos));
                    onClose();
                }
        ).bounds(cx - 100, cy + 20, 200, 20).build());

        // 从头开始按钮
        restartButton = addRenderableWidget(Button.builder(
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

        updateEntryButtons();
    }

    /**
     * 直接进本按钮只在钥匙绑定了实例时可用。
     *
     * <p>章节钥匙通关后实例被解绑、书台进入「等待选关」；此时继续/从头开始都会被
     * 服务端拒绝，界面必须同步置灰，避免玩家点出一个静默失败。</p>
     */
    private void updateEntryButtons() {
        ItemStack stack = keyStack();
        boolean bound = stack.getItem() instanceof DungeonKeyItem
                && DungeonKeyItem.getInstanceId(stack) != null;
        if (continueButton != null) continueButton.active = bound;
        if (restartButton != null) restartButton.active = bound;
    }

    /** 钥匙上记录的本章已通关关卡；旧版通用钥匙为空集。 */
    private Set<String> clearedStages() {
        return DungeonKeyItem.getClearedStages(keyStack());
    }

    private boolean hasBoundInstance() {
        ItemStack stack = keyStack();
        return stack.getItem() instanceof DungeonKeyItem
                && DungeonKeyItem.getInstanceId(stack) != null;
    }

    /** 从客户端讲台 BE 上同步下来的钥匙里取进度；讲台 BE 的 KeyStack 随 update tag 下发。 */
    private DungeonProgress progress() {
        return DungeonNodeMapRenderer.progressFromLectern(lecternPos);
    }

    private ItemStack keyStack() {
        if (minecraft == null || minecraft.level == null) return ItemStack.EMPTY;
        if (!(minecraft.level.getBlockEntity(lecternPos) instanceof DungeonLecternBlockEntity be)) return ItemStack.EMPTY;
        return be.getKeyStack();
    }

    /** 从钥匙上的 stageId 查关卡数据（客户端注册表已同步）。 */
    private StageData stage() {
        ItemStack stack = keyStack();
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

    /** 章节钥匙才有换关列表；普通（旧版）钥匙只绑定一关。 */
    private List<String> chapterStageIds() {
        ItemStack stack = keyStack();
        if (!(stack.getItem() instanceof DungeonKeyItem key && key.isChapterKey())) return List.of();
        StageData st = stage();
        if (st == null) return List.of();
        for (var ch : ClientDungeonData.getSortedChapters()) {
            if (ch.chapterId().equals(st.chapter())) return ch.stages();
        }
        return List.of();
    }

    private int stageRowX() { return 10; }
    private int stageRowWidth() { return leftColWidth() - 20; }
    private int stageRowY(int index) { return STAGE_ROW_TOP + index * STAGE_ROW_HEIGHT; }

    private int stageRowAt(double mouseX, double mouseY, int count) {
        int x = stageRowX();
        if (mouseX < x || mouseX >= x + stageRowWidth()) return -1;
        for (int i = 0; i < count; i++) {
            int y = stageRowY(i);
            if (mouseY >= y && mouseY < y + STAGE_ROW_HEIGHT) return i;
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            List<String> stageIds = chapterStageIds();
            int row = stageRowAt(mouseX, mouseY, stageIds.size());
            if (row >= 0) {
                StageData active = stage();
                String target = stageIds.get(row);
                // 已通关的关卡不可再选（重复游玩需要复制新钥匙），点击直接吞掉。
                if (clearedStages().contains(target)) return true;
                if (active == null || !target.equals(active.stageId())) {
                    PacketDistributor.sendToServer(new SelectStagePayload(lecternPos, target));
                }
                return true;
            }
        }
        // 节点图点击：命中可达未通关节点 → 直接进本（keySlot 走讲台钥匙槽位约定 -1，
        // 服务端 DungeonEntryService 以讲台为权威，keySlot 不参与校验）
        if (button == 0) {
            String hit = DungeonNodeMapRenderer.hitTest(nodePositions, mouseX, mouseY);
            if (hit != null) {
                // 等待选关（无实例）时节点图不可进本。
                if (!hasBoundInstance()) return true;
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
        // 换关后钥匙组件会随书台同步刷新，按钮可用性每帧重算。
        updateEntryButtons();

        // 暗背景
        gfx.fill(0, 0, this.width, this.height, 0xCC1A1A2E);
        super.render(gfx, mouseX, mouseY, partialTick);

        StageData st = stage();
        int cx = leftColWidth() / 2;

        renderChapterStageList(gfx, mouseX, mouseY, st);

        // 左栏标题与信息
        gfx.drawCenteredString(this.font, "§6副本入口", cx, this.height / 2 - 50, 0xFFFFD700);
        gfx.drawCenteredString(this.font, "§f" + (st == null ? "" : st.displayName()),
                cx, this.height / 2 - 30, 0xFFFFFFFF);
        List<String> chapterStages = chapterStageIds();
        if (!chapterStages.isEmpty()) {
            long cleared = chapterStages.stream().filter(clearedStages()::contains).count();
            gfx.drawCenteredString(this.font,
                    "§7本章通关: §f" + cleared + "/" + chapterStages.size(),
                    cx, this.height / 2 - 10, 0xFFCCCCCC);
        } else {
            gfx.drawCenteredString(this.font,
                    "§7已通关节点: §f" + progress().clearedNodes().size(), cx, this.height / 2 - 10, 0xFFCCCCCC);
        }

        // 右栏节点图
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

    private void renderChapterStageList(GuiGraphics gfx, int mouseX, int mouseY, StageData active) {
        List<String> stageIds = chapterStageIds();
        Set<String> cleared = clearedStages();
        for (int i = 0; i < stageIds.size(); i++) {
            String id = stageIds.get(i);
            StageData s = ClientDungeonData.getStage(id);
            String label = s == null ? id : s.displayName();
            boolean current = active != null && id.equals(active.stageId());
            boolean stageCleared = cleared.contains(id);
            int x = stageRowX();
            int y = stageRowY(i);
            boolean hover = !stageCleared && stageRowAt(mouseX, mouseY, stageIds.size()) == i;
            // 已通关关卡置灰且不响应 hover，暗示不可再选。
            int bg = stageCleared ? 0x55101010
                    : current ? 0xAA3A5A8A : hover ? 0x88444466 : 0x66222233;
            gfx.fill(x, y, x + stageRowWidth(), y + STAGE_ROW_HEIGHT - 2, bg);
            String prefix = stageCleared ? "§8✔ " : current ? "§e▶ " : "§7";
            String suffix = stageCleared ? " §8（已通关）" : "";
            gfx.drawString(this.font, prefix + label + suffix, x + 4, y + 4, 0xFFFFFFFF, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
