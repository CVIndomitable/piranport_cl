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
    private final int kills;
    private final String ending;

    public DungeonResultScreen(String stageName, long timeMillis,
                                boolean isFirstClear, List<String> rewardNames) {
        this(stageName, timeMillis, isFirstClear, rewardNames, 0);
    }

    public DungeonResultScreen(String stageName, long timeMillis,
                               boolean isFirstClear, List<String> rewardNames, int kills) {
        super(Component.translatable("gui.piranport.dungeon_result.title"));
        this.stageName = stageName;
        this.timeMillis = timeMillis;
        this.isFirstClear = isFirstClear;
        this.rewardNames = rewardNames;
        this.kills = Math.max(0, kills);
        this.ending = "战区肃清，航路恢复，舰队返回港口。";
    }

    public DungeonResultScreen(String stageName, long timeMillis,
                               boolean isFirstClear, List<String> rewardNames,
                               int kills, String ending) {
        super(Component.translatable("gui.piranport.dungeon_result.title"));
        this.stageName = stageName;
        this.timeMillis = timeMillis;
        this.isFirstClear = isFirstClear;
        this.rewardNames = rewardNames;
        this.kills = Math.max(0, kills);
        this.ending = ending == null || ending.isBlank()
                ? "战区肃清，航路恢复，舰队返回港口。" : ending;
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

        // renderBackground() 可能带强模糊。先铺一块完全不透明的面板，再画控件和文字。
        gfx.fill(cx - 120, cy - 70, cx + 120, cy + 85, 0xFF1A1A2E);
        gfx.renderOutline(cx - 120, cy - 70, 240, 155, 0xFFFFD700);

        super.render(gfx, mouseX, mouseY, partialTick);

        // 所有文字放在最终 GUI pass（super.render 之后）：部分客户端会在控件层之前
        // 插入模糊/后处理，先画的文字会被一起虚化。与 TownScroll 屏幕保持一致。
        gfx.drawCenteredString(font,
                Component.translatable("gui.piranport.dungeon_result.clear"),
                cx, cy - 60, 0xFFFFD700);
        gfx.drawCenteredString(font, Component.literal(stageName), cx, cy - 45, 0xFFFFFFFF);

        gfx.drawCenteredString(font,
                Component.translatable("gui.piranport.dungeon_result.kills", kills),
                cx, cy - 25, 0xFFFFAA88);
        gfx.drawCenteredString(font, Component.literal(ending), cx, cy + 3, 0xFFD0D0FF);

        int ry = cy + 29;
        for (String reward : rewardNames) {
            gfx.drawCenteredString(font, Component.literal(reward), cx, ry, 0xFFCCCCCC);
            ry += 12;
        }
    }
}
