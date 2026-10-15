package com.piranport.dungeon.client;

import com.piranport.dungeon.data.ChapterData;
import com.piranport.dungeon.data.NodeData;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.key.DungeonKeyItem;
import com.piranport.dungeon.key.DungeonProgress;
import com.piranport.dungeon.menu.DungeonBookMenu;
import com.piranport.dungeon.network.ClientDungeonData;
import com.piranport.dungeon.network.SelectNodePayload;
import com.piranport.registry.ModDataComponents;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Two-layer screen: stage selection → node map.
 *
 * <p>整合版 §3.1 联机大厅与队长机制已作废（副本/10）：
 * <ul>
 *   <li>删除大厅成员列表渲染（lobby 已不存在）</li>
 *   <li>删除"切换准备"按钮（无准备机制）</li>
 *   <li>Sortie 按钮直接发 SelectNodePayload 进入 stage 的 startNode（不再有 SelectStagePayload 中转）</li>
 * </ul>
 * 阶段 3（P1-B）会把这个 screen 替换为 DungeonContinueScreen（"继续/从头开始"对话框）。
 *
 * <p>NODE_MAP 布局：左栏保留标题/指引/计时/返回按钮；右栏用 {@link DungeonNodeMapRenderer}
 * 画三态节点图（不再用 Button widget），点击由 {@link #mouseClicked} 的圆形命中检测接管。</p>
 */
public class DungeonBookScreen extends AbstractContainerScreen<DungeonBookMenu> {
    private enum ViewMode { STAGE_SELECT, NODE_MAP }
    private ViewMode mode = ViewMode.STAGE_SELECT;

    private int selectedChapterIndex = 0;
    private String selectedStageId = null;
    private StageData selectedStage = null;

    /** 节点图右栏的屏幕坐标（renderBg 时算出，mouseClicked 命中检测复用）。 */
    private Map<String, int[]> nodePositions = Map.of();

    // Cached data
    private final List<ChapterData> chapters;

    public DungeonBookScreen(DungeonBookMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 280;
        this.imageHeight = 200;
        this.inventoryLabelY = 999; // hide
        this.titleLabelY = 999;    // hide
        this.chapters = ClientDungeonData.getSortedChapters();
    }

    @Override
    protected void init() {
        super.init();
        rebuildButtons();
    }

    private void rebuildButtons() {
        clearWidgets();
        if (mode == ViewMode.STAGE_SELECT) {
            buildStageSelectButtons();
        } else {
            buildNodeMapButtons();
        }
    }

    private void buildStageSelectButtons() {
        int x = leftPos + 5;
        int y = topPos + 5;

        // 章节页签
        int chapterPageSize = 3;
        int chapterPages = (chapters.size() + chapterPageSize - 1) / chapterPageSize;
        int chapterPage = selectedChapterIndex / Math.max(1, chapterPageSize);
        if (chapterPages == 0) chapterPages = 1;
        if (chapterPage >= chapterPages) chapterPage = chapterPages - 1;
        int chapterStart = chapterPage * chapterPageSize;

        for (int i = chapterStart; i < Math.min(chapterStart + chapterPageSize, chapters.size()); i++) {
            final int ci = i;
            ChapterData ch = chapters.get(i);
            addRenderableWidget(Button.builder(
                    Component.literal(ch.displayName()),
                    btn -> { selectedChapterIndex = ci; rebuildButtons(); }
            ).bounds(x + (i - chapterStart) * 90, y, 85, 16).build());
        }
        if (chapterPages > 1) {
            int arrowY = y + 18;
            if (chapterPage > 0) {
                addRenderableWidget(Button.builder(
                        Component.literal("<"),
                        btn -> {
                            selectedChapterIndex = Math.max(0, selectedChapterIndex - chapterPageSize);
                            rebuildButtons();
                        }
                ).bounds(x, arrowY, 18, 14).build());
            }
            if (chapterPage < chapterPages - 1) {
                addRenderableWidget(Button.builder(
                        Component.literal(">"),
                        btn -> {
                            selectedChapterIndex = Math.min(chapters.size() - 1,
                                    selectedChapterIndex + chapterPageSize);
                            rebuildButtons();
                        }
                ).bounds(x + 18, arrowY, 18, 14).build());
            }
        }

        // Stage buttons for selected chapter
        if (selectedChapterIndex < chapters.size()) {
            ChapterData chapter = chapters.get(selectedChapterIndex);
            int sy = y + 22;
            List<String> stageIds = new ArrayList<>(chapter.stages());
            int visibleStages = 5;
            int stagePages = (stageIds.size() + visibleStages - 1) / visibleStages;
            int stagePage = 0;
            int curIdx = selectedStageId != null ? stageIds.indexOf(selectedStageId) : 0;
            if (curIdx >= 0) stagePage = curIdx / visibleStages;
            int stageStart = stagePage * visibleStages;
            final int sp = stagePage;
            final int sps = stagePages;

            for (int si = stageStart; si < Math.min(stageStart + visibleStages, stageIds.size()); si++) {
                String stageId = stageIds.get(si);
                StageData stage = ClientDungeonData.getStage(stageId);
                if (stage == null) continue;
                String label = stage.displayName();
                addRenderableWidget(Button.builder(
                        Component.literal(label),
                        btn -> {
                            selectedStageId = stageId;
                            selectedStage = stage;
                        }
                ).bounds(x + 10, sy, 200, 16).build());
                sy += 20;
            }
            if (sps > 1) {
                int arrowX = x + 10;
                if (sp > 0) {
                    addRenderableWidget(Button.builder(
                            Component.literal("∧"),
                            btn -> {
                                int newIdx = (sp - 1) * visibleStages;
                                if (newIdx >= 0 && newIdx < stageIds.size()) {
                                    String sid = stageIds.get(newIdx);
                                    selectedStageId = sid;
                                    selectedStage = ClientDungeonData.getStage(sid);
                                }
                                rebuildButtons();
                            }
                    ).bounds(arrowX, y + 22 - 14, 16, 12).build());
                }
                if (sp < sps - 1) {
                    addRenderableWidget(Button.builder(
                            Component.literal("∨"),
                            btn -> {
                                int newIdx = (sp + 1) * visibleStages;
                                if (newIdx < stageIds.size()) {
                                    String sid = stageIds.get(newIdx);
                                    selectedStageId = sid;
                                    selectedStage = ClientDungeonData.getStage(sid);
                                }
                                rebuildButtons();
                            }
                    ).bounds(arrowX + 18, y + 22 - 14, 16, 12).build());
                }
            }
        }

        // 整合版 §3.1：Sortie 按钮直接发 SelectNodePayload 进入 stage 的 startNode（不再有 SelectStagePayload 中转）
        addRenderableWidget(Button.builder(
                Component.translatable("gui.piranport.dungeon_book.sortie"),
                btn -> {
                    if (selectedStageId != null && selectedStage != null
                            && selectedStage.startNode() != null) {
                        PacketDistributor.sendToServer(new SelectNodePayload(
                                menu.getLecternPos(), menu.getKeySlot(), selectedStage.startNode()));
                        mode = ViewMode.NODE_MAP;
                        rebuildButtons();
                    }
                }
        ).bounds(leftPos + 70, topPos + imageHeight - 25, 60, 18).build());

        // Exit button
        addRenderableWidget(Button.builder(
                Component.translatable("gui.piranport.dungeon_book.exit"),
                btn -> onClose()
        ).bounds(leftPos + 140, topPos + imageHeight - 25, 60, 18).build());

        // 整合版 §3.1：删除"切换准备"按钮（联机大厅作废，无准备机制）
    }

    /** NODE_MAP 左栏只有返回按钮；节点不再是 widget，点击走 {@link #mouseClicked}。 */
    private void buildNodeMapButtons() {
        addRenderableWidget(Button.builder(
                Component.translatable("gui.piranport.dungeon_book.back"),
                btn -> { mode = ViewMode.STAGE_SELECT; rebuildButtons(); }
        ).bounds(leftPos + 140, topPos + imageHeight - 25, 60, 18).build());
    }

    private DungeonProgress getKeyProgress() {
        if (minecraft != null && minecraft.player != null) {
            ItemStack keyStack = minecraft.player.getInventory().getItem(menu.getKeySlot());
            if (keyStack.getItem() instanceof DungeonKeyItem) {
                return keyStack.getOrDefault(ModDataComponents.DUNGEON_PROGRESS.get(),
                        DungeonProgress.EMPTY);
            }
        }
        return DungeonProgress.EMPTY;
    }

    /** 节点图区域：右半栏，避开左栏文字与底部按钮。 */
    private int graphLeft()   { return leftPos + 110; }
    private int graphTop()    { return topPos + 46; }
    private int graphWidth()  { return imageWidth - 116; }
    private int graphHeight() { return imageHeight - 78; }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 节点图点击：自算圆形命中，命中可达且未通关节点即发 SelectNodePayload
        if (mode == ViewMode.NODE_MAP && selectedStage != null && button == 0) {
            String hit = DungeonNodeMapRenderer.hitTest(nodePositions, mouseX, mouseY);
            if (hit != null) {
                DungeonProgress progress = getKeyProgress();
                Set<String> cleared = progress.clearedNodes();
                Set<String> reachable = DungeonNodeMapRenderer.computeReachable(selectedStage, progress);
                if (reachable.contains(hit) && !cleared.contains(hit)) {
                    PacketDistributor.sendToServer(new SelectNodePayload(
                            menu.getLecternPos(), menu.getKeySlot(), hit));
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        // Dark background
        gfx.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xDD1A1A2E);
        // Border
        gfx.renderOutline(leftPos, topPos, imageWidth, imageHeight, 0xFF4A4A6A);

        if (mode == ViewMode.NODE_MAP && selectedStage != null) {
            // Title / info 放左栏
            gfx.drawString(font, selectedStage.displayName(),
                    leftPos + 5, topPos + 5, 0xFFFFD700, false);
            gfx.drawString(font, "引导: " + selectedStage.guidance(),
                    leftPos + 5, topPos + 24, 0xFFB8C7D9, false);
            gfx.drawString(font, "目标: " + selectedStage.objective(),
                    leftPos + 5, topPos + 36, 0xFFFFD080, false);

            // Timer display：钥匙里存的是服务端累计的关卡总用时（节点转换时同步），
            // 不用墙钟推算——墙上时钟在暂停或挂机时照走，显示会越飘越多。
            DungeonProgress progress = getKeyProgress();
            if (progress.timerStarted()) {
                long elapsed = progress.elapsedMillis();
                String timeStr = formatTime(elapsed);
                gfx.drawString(font, timeStr,
                        leftPos + imageWidth - font.width(timeStr) - 5, topPos + 5,
                        0xFFAAFFAA, false);
            }

            // 右栏节点图：渲染同时缓存坐标，mouseClicked 用同一份坐标做命中检测，
            // 两边坐标必然一致（不会出现看得见的点不中的偏移）。
            nodePositions = DungeonNodeMapRenderer.layout(selectedStage,
                    graphLeft(), graphTop(), graphWidth(), graphHeight());
            DungeonNodeMapRenderer.render(gfx, font, selectedStage, nodePositions,
                    progress, mouseX, mouseY);
            // 图例贴节点图区域底部、返回按钮上方一行
            DungeonNodeMapRenderer.renderLegend(gfx, font,
                    graphLeft(), topPos + imageHeight - 32);
        }
        // 整合版 §3.1：删除 STAGE_SELECT 视图中的 lobby 成员列表渲染
    }

    private String formatTime(long millis) {
        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long ms = millis % 1000;
        return String.format("%02d:%02d.%03d", minutes, seconds, ms);
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        // No default labels
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        super.render(gfx, mouseX, mouseY, partialTick);
        renderTooltip(gfx, mouseX, mouseY);
    }
}
