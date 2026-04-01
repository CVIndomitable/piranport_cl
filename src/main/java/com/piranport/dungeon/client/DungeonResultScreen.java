package com.piranport.dungeon.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Shown after dungeon completion. Displays time, rewards, first-clear status.
 */
public class DungeonResultScreen extends Screen {
    private final String stageName;
    private final long timeMillis;
    private final boolean isFirstClear;
    private final List<String> rewardNames;

    public DungeonResultScreen(String stageName, long timeMillis,
                                boolean isFirstClear, List<String> rewardNames) {
        super(Component.translatable("gui.piranport.dungeon_result.title"));
        this.stageName = stageName;
        this.timeMillis = timeMillis;
        this.isFirstClear = isFirstClear;
        this.rewardNames = rewardNames;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(
                Component.translatable("gui.piranport.dungeon_result.close"),
                btn -> onClose()
        ).bounds(width / 2 - 40, height / 2 + 60, 80, 20).build());
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        renderBackground(gfx, mouseX, mouseY, partialTick);
        int cx = width / 2;
        int cy = height / 2;

        // Panel
        gfx.fill(cx - 120, cy - 70, cx + 120, cy + 85, 0xDD1A1A2E);
        gfx.renderOutline(cx - 120, cy - 70, 240, 155, 0xFFFFD700);

        // Title
        gfx.drawCenteredString(font,
                Component.translatable("gui.piranport.dungeon_result.clear"),
                cx, cy - 60, 0xFFFFD700);

        // Stage name
        gfx.drawCenteredString(font, Component.literal(stageName), cx, cy - 45, 0xFFFFFFFF);

        // Time
        long totalSec = timeMillis / 1000;
        String timeStr = String.format("%02d:%02d.%03d", totalSec / 60, totalSec % 60, timeMillis % 1000);
        gfx.drawCenteredString(font,
                Component.translatable("gui.piranport.dungeon_result.time", timeStr),
                cx, cy - 25, 0xFFAAFFAA);

        // First clear
        if (isFirstClear) {
            gfx.drawCenteredString(font,
                    Component.translatable("gui.piranport.dungeon_result.first_clear"),
                    cx, cy - 10, 0xFFFFD700);
        }

        // Rewards
        int ry = cy + 5;
        for (String reward : rewardNames) {
            gfx.drawCenteredString(font, Component.literal(reward), cx, ry, 0xFFCCCCCC);
            ry += 12;
        }

        super.render(gfx, mouseX, mouseY, partialTick);
    }
}
