package com.piranport.dungeon.client;

import com.piranport.registry.ModItems;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 右上角滑下的"本节点已通关"横幅（《副本/19》2026-10-22 补充）。
 *
 * <p><b>为什么照抄 {@code AdvancementToast} 而不是自绘：</b>策划要求"和原版进度一样"。
 * 直接复用原版 {@code toast/advancement} 底板贴图、32 像素行高、5 秒停留、
 * 以及原版的 {@code ToastComponent} 进出场动画与音效，观感就是原版进度条；
 * 任务栏里也不会多出页面——本横幅没有确认按钮，玩家不需要任何操作。</p>
 *
 * <p>版式与原版一致：图标 16×16 在 (8,8)，文字从 x=30 起。单行副标题放在 y=18，
 * 多行时在 32 像素高度里垂直居中并逐行排开。</p>
 */
public class NodeClearedToast implements Toast {
    private static final ResourceLocation BACKGROUND_SPRITE =
            ResourceLocation.withDefaultNamespace("toast/advancement");
    /** 与原版进度横幅一致：5 秒（再乘玩家的通知显示时长倍率）。 */
    private static final int DISPLAY_TIME = 5000;
    /** 原版普通按进度的黄色标题。 */
    private static final int TITLE_COLOR = 0xFFFF00;
    /** 原版副标题宽度上限（125 像素），超出后自动换行。 */
    private static final int TEXT_WIDTH = 125;

    private final String stageName;
    private final String nodeName;
    private final boolean firstClear;

    public NodeClearedToast(String stageName, String nodeName, boolean firstClear) {
        this.stageName = stageName == null ? "" : stageName;
        this.nodeName = nodeName == null ? "" : nodeName;
        this.firstClear = firstClear;
    }

    @Override
    public Toast.Visibility render(GuiGraphics guiGraphics, ToastComponent toastComponent,
                                   long timeSinceLastVisible) {
        guiGraphics.blitSprite(BACKGROUND_SPRITE, 0, 0, this.width(), this.height());
        Font font = toastComponent.getMinecraft().font;

        List<FormattedCharSequence> title = font.split(
                Component.translatable("dungeon.piranport.node_cleared.toast.title"), TEXT_WIDTH);
        if (title.size() == 1) {
            guiGraphics.drawString(font, title.get(0), 30, 7, TITLE_COLOR, false);
        } else {
            // 标题被翻译撑成多行时，先让标题占满整块高度；副标题会自然落在下方。
            int y = 7;
            for (FormattedCharSequence line : title) {
                guiGraphics.drawString(font, line, 30, y, TITLE_COLOR, false);
                y += 9;
            }
        }

        List<FormattedCharSequence> subtitle = font.split(buildSubtitle(), TEXT_WIDTH);
        int textY = title.size() == 1
                ? 18
                : this.height() / 2 - subtitle.size() * 9 / 2;
        for (FormattedCharSequence line : subtitle) {
            guiGraphics.drawString(font, line, 30, textY, 0xFFFFFF, false);
            textY += 9;
        }

        // 副本地牢里最认得出的物品就是副本钥匙，直接当图标用，不必另画新贴图。
        guiGraphics.renderFakeItem(new ItemStack(ModItems.DUNGEON_KEY.get()), 8, 8);

        return (double) timeSinceLastVisible
                >= DISPLAY_TIME * toastComponent.getNotificationDisplayTimeMultiplier()
                ? Toast.Visibility.HIDE
                : Toast.Visibility.SHOW;
    }

    private Component buildSubtitle() {
        String base = this.nodeName.isBlank() ? this.stageName
                : this.stageName.isBlank() ? this.nodeName
                : this.stageName + " · " + this.nodeName;
        Component subtitle = Component.literal(base);
        if (this.firstClear) {
            subtitle = subtitle.copy().append(" · ").append(
                    Component.translatable("dungeon.piranport.node_cleared.toast.first_clear"));
        }
        return subtitle;
    }
}
