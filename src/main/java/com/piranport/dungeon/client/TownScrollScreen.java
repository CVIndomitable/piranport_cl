package com.piranport.dungeon.client;

import com.piranport.dungeon.network.TownScrollUsePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Confirmation screen for using the town scroll.
 */
public class TownScrollScreen extends Screen {

    public TownScrollScreen() {
        super(Component.translatable("gui.piranport.town_scroll.title"));
    }

    @Override
    protected void init() {
        super.init();
        int cx = width / 2;
        int cy = height / 2;

        addRenderableWidget(Button.builder(
                Component.translatable("gui.piranport.town_scroll.confirm"),
                btn -> {
                    PacketDistributor.sendToServer(new TownScrollUsePayload());
                    onClose();
                }
        ).bounds(cx - 55, cy + 10, 50, 20).build());

        addRenderableWidget(Button.builder(
                Component.translatable("gui.piranport.town_scroll.cancel"),
                btn -> onClose()
        ).bounds(cx + 5, cy + 10, 50, 20).build());
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        renderBackground(gfx, mouseX, mouseY, partialTick);
        int cx = width / 2;
        int cy = height / 2;

        // Draw a fully opaque dialog after the dim/blurred background so its text
        // stays readable on clients that apply a strong background blur.
        gfx.fill(cx - 100, cy - 45, cx + 100, cy + 50, 0xFF1A1A1A);
        gfx.renderOutline(cx - 100, cy - 45, 200, 95, 0xFFFFD700);

        super.render(gfx, mouseX, mouseY, partialTick);

        // Draw text in the same final GUI pass as the buttons. Some clients apply
        // their blur/post-processing pass between the panel and widget layers.
        gfx.drawCenteredString(font, title, cx, cy - 36, 0xFFFFD700);
        gfx.drawCenteredString(font,
                Component.translatable("gui.piranport.town_scroll.question"),
                cx, cy - 18, 0xFFFFFFFF);
    }
}
