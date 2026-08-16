package com.piranport.dungeon.client;

import com.piranport.dungeon.data.ChapterData;
import com.piranport.dungeon.data.NodeData;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.key.DungeonKeyItem;
import com.piranport.dungeon.key.DungeonProgress;
import com.piranport.dungeon.menu.DungeonBookMenu;
import com.piranport.dungeon.network.ClientDungeonData;
import com.piranport.dungeon.network.SelectNodePayload;
import com.piranport.dungeon.network.SelectStagePayload;
import com.piranport.dungeon.network.ToggleReadyPayload;
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
import java.util.Set;

/**
 * Two-layer screen: stage selection → node map.
 */
public class DungeonBookScreen extends AbstractContainerScreen<DungeonBookMenu> {
    private enum ViewMode { STAGE_SELECT, NODE_MAP }
    private ViewMode mode = ViewMode.STAGE_SELECT;

    private int selectedChapterIndex = 0;
    private String selectedStageId = null;
    private StageData selectedStage = null;

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

        // ===== 章节页签 (含翻页：策划 §10.5 缺翻页) =====
        // Phase 27：简单翻页 — 当前每屏显示 3 个章节页签；超过则用 "<" ">" 按钮翻页
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
        // "<" ">" 翻页按钮
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
            // Phase 27：关卡列表滚动 (策划 §10.5/§12.2 缺滚动)
            List<String> stageIds = new ArrayList<>(chapter.stages());
            int visibleStages = 5;
            int stagePages = (stageIds.size() + visibleStages - 1) / visibleStages;
            int stagePage = 0;
            // 用 selectedStageId 推算当前页（保持体验连贯）
            int curIdx = selectedStageId != null ? stageIds.indexOf(selectedStageId) : 0;
            if (curIdx >= 0) stagePage = curIdx / visibleStages;
            int stageStart = stagePage * visibleStages;
            // 复制为 final 给 lambda 用
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
            // 滚动箭头
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

        // Sortie button
        addRenderableWidget(Button.builder(
                Component.translatable("gui.piranport.dungeon_book.sortie"),
                btn -> {
                    if (selectedStageId != null) {
                        PacketDistributor.sendToServer(new SelectStagePayload(
                                menu.getLecternPos(), menu.getKeySlot(), selectedStageId));
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

        // ===== Phase 27：策划 §10.2 切换准备按钮 (位于成员区右上) =====
        List<String> members = ClientDungeonData.getLobbyMembers();
        List<Boolean> readyStates = ClientDungeonData.getLobbyReadyStates();
        boolean myReady = false;
        if (minecraft != null && minecraft.player != null) {
            String myName = minecraft.player.getGameProfile().getName();
            int myIdx = members.indexOf(myName);
            if (myIdx >= 0 && myIdx < readyStates.size()) {
                myReady = readyStates.get(myIdx);
            }
        }
        addRenderableWidget(Button.builder(
                Component.translatable(myReady
                        ? "gui.piranport.dungeon_book.unready"
                        : "gui.piranport.dungeon_book.ready"),
                btn -> PacketDistributor.sendToServer(
                        new ToggleReadyPayload(menu.getLecternPos()))
        ).bounds(leftPos + imageWidth - 70, topPos + imageHeight - 108, 65, 16).build());
    }

    private void buildNodeMapButtons() {
        if (selectedStage == null) return;

        // Back button
        addRenderableWidget(Button.builder(
                Component.translatable("gui.piranport.dungeon_book.back"),
                btn -> { mode = ViewMode.STAGE_SELECT; rebuildButtons(); }
        ).bounds(leftPos + 140, topPos + imageHeight - 25, 60, 18).build());

        // Get current progress from key
        DungeonProgress progress = getKeyProgress();
        Set<String> cleared = progress.clearedNodes();

        // Determine reachable nodes
        Set<String> reachable;
        if (cleared.isEmpty()) {
            reachable = Set.of(selectedStage.startNode());
        } else {
            reachable = new java.util.HashSet<>();
            for (String clearedNode : cleared) {
                reachable.addAll(selectedStage.getReachableFrom(clearedNode));
            }
            reachable.removeAll(cleared);
        }

        // Node buttons
        for (var entry : selectedStage.nodes().entrySet()) {
            String nodeId = entry.getKey();
            NodeData node = entry.getValue();

            boolean isCleared = cleared.contains(nodeId);
            boolean isReachable = reachable.contains(nodeId);

            String prefix = isCleared ? "[✓] " : isReachable ? "[→] " : "[x] ";
            String typeLabel = switch (node.type()) {
                case BATTLE -> "战斗";
                case BOSS -> "★Boss";
                case RESOURCE -> "补给";
                case COST -> "漩涡";
            };

            Button nodeBtn = Button.builder(
                    Component.literal(prefix + nodeId + " " + typeLabel),
                    btn -> {
                        if (isReachable && !isCleared) {
                            PacketDistributor.sendToServer(new SelectNodePayload(
                                    menu.getLecternPos(), menu.getKeySlot(), nodeId));
                        }
                    }
            ).bounds(leftPos + scaleX(node.displayX()), topPos + scaleY(node.displayY()),
                    70, 16).build();

            nodeBtn.active = isReachable && !isCleared;
            addRenderableWidget(nodeBtn);
        }
    }

    private int scaleX(int displayX) {
        return (int) (displayX * imageWidth / 400.0) + 5;
    }

    private int scaleY(int displayY) {
        return (int) (displayY * (imageHeight - 50) / 400.0) + 20;
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

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        // Dark background
        gfx.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xDD1A1A2E);
        // Border
        gfx.renderOutline(leftPos, topPos, imageWidth, imageHeight, 0xFF4A4A6A);

        if (mode == ViewMode.NODE_MAP && selectedStage != null) {
            // Draw edges between nodes
            for (var edge : selectedStage.edges()) {
                NodeData from = selectedStage.nodes().get(edge.from());
                NodeData to = selectedStage.nodes().get(edge.to());
                if (from != null && to != null) {
                    int x1 = leftPos + scaleX(from.displayX()) + 35;
                    int y1 = topPos + scaleY(from.displayY()) + 8;
                    int x2 = leftPos + scaleX(to.displayX()) + 35;
                    int y2 = topPos + scaleY(to.displayY()) + 8;
                    drawLine(gfx, x1, y1, x2, y2, 0xFF6A6A8A);
                }
            }

            // Title
            gfx.drawString(font, selectedStage.displayName(),
                    leftPos + 5, topPos + 5, 0xFFFFD700, false);

            // Timer display
            DungeonProgress progress = getKeyProgress();
            if (progress.timerStarted()) {
                long elapsed = System.currentTimeMillis() - progress.startTimeMillis();
                String timeStr = formatTime(elapsed);
                gfx.drawString(font, timeStr,
                        leftPos + imageWidth - font.width(timeStr) - 5, topPos + 5,
                        0xFFAAFFAA, false);
            }
        } else if (mode == ViewMode.STAGE_SELECT) {
            // Phase 27：策划 §10.2 大厅成员列表 (策划 §10.5/§12.2 缺成员列表)
            List<String> members = ClientDungeonData.getLobbyMembers();
            List<Boolean> readyStates = ClientDungeonData.getLobbyReadyStates();
            String flagship = ClientDungeonData.getLobbyFlagshipName();
            gfx.drawString(font, Component.translatable("gui.piranport.dungeon_book.lobby_members").getString(),
                    leftPos + 5, topPos + imageHeight - 105, 0xFFFFD700, false);
            int memberY = topPos + imageHeight - 92;
            for (int i = 0; i < members.size() && i < 6; i++) {
                String name = members.get(i);
                boolean isFlagship = name.equals(flagship);
                boolean isReady = i < readyStates.size() && readyStates.get(i);
                String prefix = isFlagship ? "★ " : "  ";
                String readyMark = isReady ? " [✓]" : " [ ]";
                int color = isReady ? 0xFF7CFF7C : (isFlagship ? 0xFFFFE08A : 0xFFCCCCCC);
                gfx.drawString(font, prefix + name + readyMark,
                        leftPos + 5, memberY + i * 12, color, false);
            }
        }
    }

    private void drawLine(GuiGraphics gfx, int x1, int y1, int x2, int y2, int color) {
        // Simple Bresenham line using fill for each pixel
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;

        int maxIterations = 10000; // Safety limit for abnormal input
        int iterations = 0;
        while (true) {
            gfx.fill(x1, y1, x1 + 1, y1 + 1, color);
            if (x1 == x2 && y1 == y2) break;
            if (++iterations > maxIterations) break; // Prevent infinite loop
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x1 += sx; }
            if (e2 < dx) { err += dx; y1 += sy; }
        }
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
