package com.piranport.dungeon.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 整合版 §3.1 讲台入口"继续/从头开始"对话框。
 *
 * <p>显示 stage displayName + clearedNodes 数量。
 * 按钮：
 * <ul>
 *   <li>继续 — 发 {@link com.piranport.dungeon.network.ContinueFromCheckpointPayload}</li>
 *   <li>从头开始 — 发 {@link com.piranport.dungeon.network.RestartFromBeginningPayload}</li>
 * </ul>
 * </p>
 */
public class DungeonContinueScreen extends Screen {
    private final BlockPos lecternPos;
    private final String stageName;
    private final int clearedNodeCount;

    public DungeonContinueScreen(BlockPos lecternPos, String stageName, int clearedNodeCount) {
        super(Component.literal("副本入口"));
        this.lecternPos = lecternPos;
        this.stageName = stageName;
        this.clearedNodeCount = clearedNodeCount;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        // 标题文字
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

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        // 暗背景
        gfx.fill(0, 0, this.width, this.height, 0xCC1A1A2E);
        super.render(gfx, mouseX, mouseY, partialTick);

        // 标题
        int cx = this.width / 2;
        gfx.drawCenteredString(this.font, "§6副本入口", cx, this.height / 2 - 50, 0xFFFFD700);
        gfx.drawCenteredString(this.font, "§f" + stageName, cx, this.height / 2 - 30, 0xFFFFFFFF);
        gfx.drawCenteredString(this.font,
                "§7已通关节点: §f" + clearedNodeCount, cx, this.height / 2 - 10, 0xFFCCCCCC);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}